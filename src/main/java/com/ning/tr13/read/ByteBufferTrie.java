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
        byte b = bb.get(ptr);

        int type = (b >> 6) & 0x03;
        switch (type) {
        case TrieConstants.TYPE_LEAF_SIMPLE:
            // Only matches if we are at the end
            if (path.endOfKey()) {
                VInt.bytesToUnsigned(TrieConstants.FIRST_BYTE_BITS_FOR_LEAVES,
                        bb, ptr, path.longHolder);
                path.setValue(path.longHolder[0]);
                return path;
            }
            return null;
            
        case TrieConstants.TYPE_LEAF_WITH_SUFFIX:
            
            
        case TrieConstants.TYPE_BRANCH_SIMPLE:    
        case TrieConstants.TYPE_BRANCH_WITH_VALUE:
        }

        return null;
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
        private final int keyEnd;
        private int keyOffset;
        
        private long value;
        
        public Path(byte[] key)
        {
            this.key = key;
            this.keyEnd = key.length-1;
            keyOffset = 0;
        }

        public void setValue(long value) {
            this.value = value;
        }
        
        public long value() { return value; }
        
        public boolean endOfKey() {
            return (keyOffset == keyEnd);
        }
    }
}
