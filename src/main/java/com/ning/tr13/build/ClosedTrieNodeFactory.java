package com.ning.tr13.build;

import java.io.IOException;
import java.io.OutputStream;

import com.ning.tr13.util.VInt;

/**
 * Interface for node factories for specific value type.
 *
 * @param <T>
 */
public abstract class ClosedTrieNodeFactory<T>
{
    public abstract ClosedTrieNode<T> simpleLeaf(byte b, T value);
    public abstract ClosedTrieNode<T> simpleBranch(byte b, ClosedTrieNode<T>[] kids);
    public abstract ClosedTrieNode<T> valueBranch(byte b, ClosedTrieNode<T>[] kids, T value);
    public abstract ClosedTrieNode<T> serialized(ClosedTrieNode<T> node);
    public abstract ClosedTrieNode<T> suffixLeaf(byte b, ClosedTrieNode<T> node);

    /*
    /**********************************************************
    /* Shared concrete implementations
    /**********************************************************
     */

    /**
     * SerializedNode is a full binary serialization of a leaf or branch node;
     * used since it is the most compact representation for most nodes (esp.
     * branch nodes)
     */
    public final static class SerializedNode<T>
        extends ClosedTrieNode<T>
    {
        protected final byte[] _data;
        
        public SerializedNode(byte nb, byte[] data) {
            super(nb);
            _data = data;
        }
        
        // doesn't matter but...
        @Override
        public boolean isLeaf() { return false; }

        @Override
        public long length() { return _data.length; }

        @Override
        public int typeBits() { return 0; }
    
        @Override
        public byte[] serialize() { return _data; }

        public int serialize(byte[] result, int offset) {
            int len = _data.length;
            System.arraycopy(_data, 0, result, offset, len);
            return offset+len;
        }

        @Override
        public void serializeTo(OutputStream out, byte[] tmpBuf) throws IOException
        {
            out.write(_data);
        }
    }

    public abstract static class Leaf<T>
        extends ClosedTrieNode<T>
    {
        protected Leaf(byte b) {
            super(b);
        }
    
        public boolean isLeaf() { return true; }
    }
    
    /**
     * Simple branch just means that branch node does not have associated
     * value. Serialization contains leading VInt for total length of
     * all contained nodes, and sequence of serialization for nodes.
     */
    public static class SimpleBranch<T>
        extends ClosedTrieNode<T>
    {
        protected final ClosedTrieNode<T>[] _children;
        
        public SimpleBranch(byte b, ClosedTrieNode<T>[] kids) {
            super(b);
            _children = kids;
        }
    
        public long length()
        {
            // first one is VInt for total length; but that itself needs to be calculated
            long len = lengthOfContent();
            // and otherwise it really is just that length and child contents
            return VInt.lengthForUnsigned(len, 6) + len; 
        }

        public int typeBits() { return TYPE_BRANCH_SIMPLE; }
        public boolean isLeaf() { return false; }

        public byte[] serialize()
        {
            long contentLen = lengthOfContent();
            byte[] result = new byte[(int) (contentLen + VInt.lengthForUnsigned(contentLen, FIRST_BYTE_BITS_FOR_BRANCHES))];
            // First: serialize length indicator
            int offset = VInt.unsignedToBytes(contentLen, FIRST_BYTE_BITS_FOR_BRANCHES, result, 0);
            _addTypeBits(result, 0);
            offset = serializeChildren(result, offset);
            return result;
        }

        public int serialize(byte[] result, int offset)
        {
            long contentLen = lengthOfContent();
            // First: serialize length indicator
            int origOffset = offset;
            offset = VInt.unsignedToBytes(contentLen, FIRST_BYTE_BITS_FOR_BRANCHES, result, offset);
            _addTypeBits(result, origOffset);
            offset = serializeChildren(result, offset);
            return offset;
        }

        public void serializeTo(OutputStream out, byte[] tmpBuf) throws IOException
        {
            long contentLen = lengthOfContent();
            // first simple length indicator
            int ptr = VInt.unsignedToBytes(contentLen, FIRST_BYTE_BITS_FOR_BRANCHES, tmpBuf, 0);
            _addTypeBits(tmpBuf, 0);
            out.write(tmpBuf, 0, ptr);
            // then children
            for (ClosedTrieNode<T> n : _children) {
                out.write(n.nextByte());
                n.serializeTo(out, tmpBuf);
            }
        }
        
        /**
         * Helper method that calculates length of all contained data (children,
         * branching bytes).
         */
        protected long lengthOfContent()
        {
            // one byte per child for branching:
            long len = (long) _children.length;
            // and then child serializations:
            for (ClosedTrieNode<T> n : _children) {
                len += n.length();
            }
            return len;
        }

        protected int serializeChildren(byte[] result, int offset)
        {
            for (ClosedTrieNode<T> n : _children) {
                result[offset++] = n.nextByte();
                offset = n.serialize(result, offset);                
            }
            return offset;
        }
    }
}
