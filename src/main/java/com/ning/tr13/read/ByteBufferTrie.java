package com.ning.tr13.read;

import java.nio.ByteBuffer;
import java.util.NoSuchElementException;

import com.ning.tr13.TrieConstants;
import com.ning.tr13.TrieLookup;
import com.ning.tr13.util.VInt;

public class ByteBufferTrie extends TrieLookup
{
    /**
     * Buffer that contains raw trie data.
     */
    protected final ByteBuffer _byteBuffer;

    /**
     * Number of bytes in {@link #_byteBuffer}
     */
    protected final int _size;

    public ByteBufferTrie(ByteBuffer bb, int size) {
        _byteBuffer = bb;
        _size = size;
    }

    /*
    /**********************************************************
    /* Trie API impl
    /**********************************************************
     */

    public long getValue(byte[] key) throws NoSuchElementException {
        Path result = _findValue(new Path(key), 0);
        if (result != null) {
            return result.value();
        }
        throw new NoSuchElementException("No value for key "+_printKey(key, 0, key.length));
    }

    public long getValue(byte[] key, long defaultValue) {
        // !!! TBI
        return defaultValue;
    }

    public Long findValue(byte[] key) {
        // !!! TBI
        return null;
    }

    /*
    /**********************************************************
    /* Internal methods
    /**********************************************************
     */

    private Path _findValue(Path path, int ptr)
    {
        final ByteBuffer bb = _byteBuffer;

        main_loop:

        while (true) {
            int type = (bb.get(ptr) >> 6) & 0x03;
            if (type == TrieConstants.TYPE_LEAF_SIMPLE) {
                // Only matches if we are at the end
                if (path.endOfKey()) {
                    VInt.bytesToUnsigned(TrieConstants.FIRST_BYTE_BITS_FOR_LEAVES,
                            bb, ptr, path.longHolder);
                    path.setValue(path.longHolder[0]);
                    return path;
                }
                return null;
            }
            if (type == TrieConstants.TYPE_LEAF_WITH_SUFFIX) {
                // First we get value, as with regular leaves
                ptr = VInt.bytesToUnsigned(TrieConstants.FIRST_BYTE_BITS_FOR_LEAVES,
                        bb, ptr, path.longHolder);
                path.setValue(path.longHolder[0]);
                // Then length of suffix
                ptr = VInt.bytesToUnsigned(8, bb, ptr, path.longHolder);
                int suffixLen = (int) path.longHolder[0];
                if (path.matchKeySuffix(bb, ptr, suffixLen)) {
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
                        bb, ptr, path.longHolder);
            } else { // branch with value
                // ok: first thing; does this branch itself match?
                ptr = VInt.bytesToUnsigned(TrieConstants.FIRST_BYTE_BITS_FOR_BRANCHES,
                        bb, ptr, path.longHolder);
                if (path.endOfKey()) {
                    path.setValue(path.longHolder[0]);
                    return path;                
                }
                ptr = VInt.bytesToUnsigned(8, bb, ptr, path.longHolder);
            }
            // either way, now know content length; and can loop
            int end = ptr + (int) path.longHolder[0];
            do {
                byte b = bb.get(ptr++);
                if (!path.matchNextKeyByte(b)) {
                    ptr = _skipEntry(path, ptr);
                    continue;
                }
                return path;
            } while (ptr < end);
        }
    }

    private int _skipEntry(Path path, int ptr)
    {
        final ByteBuffer bb = _byteBuffer;
        int type = (bb.get(ptr) >> 6) & 0x03;
        if (type == TrieConstants.TYPE_LEAF_SIMPLE) {
            ptr = VInt.bytesToUnsigned(TrieConstants.FIRST_BYTE_BITS_FOR_LEAVES,
                    bb, ptr, path.longHolder);
        } else if (type == TrieConstants.TYPE_LEAF_WITH_SUFFIX) {
            // First we get value, as with regular leaves
            ptr = VInt.bytesToUnsigned(TrieConstants.FIRST_BYTE_BITS_FOR_LEAVES,
                    bb, ptr, path.longHolder);
            // Then length of suffix
            ptr = VInt.bytesToUnsigned(8, bb, ptr, path.longHolder);
            int suffixLen = (int) path.longHolder[0];
            ptr += suffixLen;
        } else if (type == TrieConstants.TYPE_BRANCH_SIMPLE) {
            // simple branches: first get total length of children; then children
            ptr = VInt.bytesToUnsigned(TrieConstants.FIRST_BYTE_BITS_FOR_BRANCHES,
                    bb, ptr, path.longHolder);
            ptr += (int) path.longHolder[0];
        } else { // branch with value
            // first value, then length of contents (children) to skip
            ptr = VInt.bytesToUnsigned(TrieConstants.FIRST_BYTE_BITS_FOR_BRANCHES,
                    bb, ptr, path.longHolder);
            ptr = VInt.bytesToUnsigned(8, bb, ptr, path.longHolder);
            ptr += (int) path.longHolder[0];
        }
        return ptr;
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
        public final long[] longHolder = new long[1];

        private final byte[] key;
        private int keyOffset;
        
        private long value;
        
        public Path(byte[] key)
        {
            this.key = key;
            keyOffset = 0;
        }

        public void setValue(long value) {
            this.value = value;
        }
        
        public long value() { return value; }
        
        public boolean endOfKey() {
            return (keyOffset == key.length);
        }
        
        public int remainingKeyLength() {
            return (key.length - keyOffset);
        }

        public boolean matchKeySuffix(ByteBuffer bb, int offset, int len)
        {
            if (len != remainingKeyLength()) return false;
            for (int i = 0; i < len; ++i) {
                if (bb.get(offset++) != key[keyOffset++]) {
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
