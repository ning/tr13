package com.ning.tr13.impl.bytes;

import com.ning.tr13.TrieConstants;
import com.ning.tr13.lookup.BytesTrieLookup;
import com.ning.tr13.util.VInt;

public class ByteArrayBytesTrieLookup
    extends BytesTrieLookup
{
    /**
     * Buffer that contains raw trie data.
     */
    protected final byte[] _byteArray;

    public ByteArrayBytesTrieLookup(byte[] raw) {
        _byteArray = raw;
    }

    /*
    /**********************************************************
    /* TrieLookup impl
    /**********************************************************
     */

    @Override
    public byte[] findValue(byte[] key)
    {
        Path result = _findValue(new Path(key), 0);
        if (result != null) {
            return result.value();
        }
        return null;
    }

    /*
    /**********************************************************
    /* Internal methods
    /**********************************************************
     */

    private Path _findValue(Path path, int ptr)
    {
        final long[] longHolder = new long[1];
        
        main_loop:
        while (true) {
            int type = (_byteArray[ptr] >> 6) & 0x03;
            if (type == TrieConstants.TYPE_LEAF_SIMPLE) {
                // Only matches if we are at the end
                if (path.endOfKey()) {
                    ptr = VInt.bytesToUnsigned(TrieConstants.FIRST_BYTE_BITS_FOR_LEAVES,
                            _byteArray, ptr, longHolder);
                    final int valueLength = (int) longHolder[0];
                    path.setValue(_byteArray, ptr, valueLength);
                    return path;
                }
                return null;
            }
            if (type == TrieConstants.TYPE_LEAF_WITH_SUFFIX) {
                // First we get value, as with regular leaves
                ptr = VInt.bytesToUnsigned(TrieConstants.FIRST_BYTE_BITS_FOR_LEAVES,
                        _byteArray, ptr, longHolder);
                final int valueLength = (int) longHolder[0];
                path.setValue(_byteArray, ptr, valueLength);
                ptr += valueLength;
                // Then length of suffix verification
                ptr = VInt.bytesToUnsigned(8, _byteArray, ptr, longHolder);
                int suffixLen = (int) longHolder[0];
                if (path.matchKeySuffix(_byteArray, ptr, suffixLen)) {
                    return path;
                }
                return null;
            }
            // nope: a branch
            if (type == TrieConstants.TYPE_BRANCH_SIMPLE) {
                // first things first: if key ended, can't match:
                if (path.endOfKey()) {
                    return null;
                }
                // simple branches: first get total length of children; then children
                ptr = VInt.bytesToUnsigned(TrieConstants.FIRST_BYTE_BITS_FOR_BRANCHES,
                        _byteArray, ptr, longHolder);
            } else { // branch with value
                // ok: first thing; does this branch itself match?
                ptr = VInt.bytesToUnsigned(TrieConstants.FIRST_BYTE_BITS_FOR_BRANCHES,
                        _byteArray, ptr, longHolder);
                int valueLength = (int) longHolder[0];
                if (path.endOfKey()) {
                    path.setValue(_byteArray, ptr, valueLength);
                    return path;                
                }
                ptr += valueLength;
                ptr = VInt.bytesToUnsigned(8, _byteArray, ptr, longHolder);
            }
            // either way, now know content length; and can loop
            int end = ptr + (int) longHolder[0];
            child_loop:
            do {
                byte b = _byteArray[ptr++];
                if (!path.matchNextKeyByte(b)) {
                    ptr = _skipEntry(path, ptr, longHolder);
                    continue child_loop;
                }
                // match: handle entry
                continue main_loop;
            } while (ptr < end);
            // no match?
            return null;
        }
    }

    private int _skipEntry(Path path, int ptr, long[] longHolder)
    {
        int type = (_byteArray[ptr] >> 6) & 0x03;
        if (type == TrieConstants.TYPE_LEAF_SIMPLE) { // simple; value length, bytes
            ptr = VInt.bytesToUnsigned(TrieConstants.FIRST_BYTE_BITS_FOR_LEAVES,
                    _byteArray, ptr, longHolder);
        } else if (type == TrieConstants.TYPE_LEAF_WITH_SUFFIX) {
            // First we get value, as with regular leaves, skip:
            ptr = VInt.bytesToUnsigned(TrieConstants.FIRST_BYTE_BITS_FOR_LEAVES,
                    _byteArray, ptr, longHolder);
            ptr += (int) longHolder[0];
            // Then length of suffix
            ptr = VInt.bytesToUnsigned(8, _byteArray, ptr, longHolder);
        } else if (type == TrieConstants.TYPE_BRANCH_SIMPLE) {
            // simple branches: first get total length of children; then children
            ptr = VInt.bytesToUnsigned(TrieConstants.FIRST_BYTE_BITS_FOR_BRANCHES,
                    _byteArray, ptr, longHolder);
        } else {
            // branch with value
            // first value, then length of contents (children) to skip
            ptr = VInt.bytesToUnsigned(TrieConstants.FIRST_BYTE_BITS_FOR_BRANCHES,
                    _byteArray, ptr, longHolder);
            ptr += (int) longHolder[0];
            ptr = VInt.bytesToUnsigned(8, _byteArray, ptr, longHolder);
        }
        return ptr + (int) longHolder[0];
    }
    
    /*
    /**********************************************************
    /* Helper classes
    /**********************************************************
     */

    /**
     * Helper class that encapsulates traversal state
     */
    private static class Path
    {
        private final byte[] key;
        private int keyOffset;
        
        private byte[] valueBuffer;
        private int valueOffset;
        private int valueLength;
        
        public Path(byte[] key)
        {
            this.key = key;
            keyOffset = 0;
        }

        public void setValue(byte[] buffer, int offset, int length)
        {
            valueBuffer = buffer;
            valueOffset = offset;
            valueLength = length;
        }
        
        public byte[] value() {
            byte[] result = new byte[valueLength];
            System.arraycopy(valueBuffer, valueOffset, result, 0, valueLength);
            return result;
        }
        
        public boolean endOfKey() {
            return (keyOffset == key.length);
        }
        
        public int remainingKeyLength() {
            return (key.length - keyOffset);
        }

        public boolean matchKeySuffix(byte[] byteArray, int offset, int len)
        {
            if (len != remainingKeyLength()) return false;
            for (int i = 0; i < len; ++i) {
                if (byteArray[offset++] != key[keyOffset++]) {
                    return false;
                }
            }
            return true;
        }

        public boolean matchNextKeyByte(byte b)
        {
            if (key[keyOffset] == b) {
                ++keyOffset;
                return true;
            }
            return false;
        }
    }
}