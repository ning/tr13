package com.ning.tr13.build;

import java.io.IOException;
import java.io.OutputStream;

import com.ning.tr13.TrieConstants;
import com.ning.tr13.util.VInt;

/**
 * This class represents in-memory nodes that are ready to be serialized.
 * 
 * @author tatu
 */
public abstract class ClosedTrieNode
    extends TrieConstants
    implements TrieNode, Comparable<ClosedTrieNode>
{
    /**
     * This constants is used as safe minimum size for temporary
     * buffer to pass to {@link #writeTo} method. It is actually
     * set to quite a bit higher than strict minimum, just to give
     * some room for expansion in case structure changes.
     */
    public final static int MINIMUM_TEMP_BUFFER_LENGTH = 64;    

    /**
     * Byte that parent node (branch) will use to branch into this node.
     */
    protected final byte _nextByte;
    
    protected ClosedTrieNode(byte nb)
    {
        _nextByte = nb;
    }

    /*
    /***********************************************************
    /* Public API, TrieNode
    /***********************************************************
     */

    public abstract long length();
    
    public abstract void serializeTo(OutputStream out, byte[] tmpBuf) throws IOException;
    
    /*
    /***********************************************************
    /* Public API, other
    /***********************************************************
     */
    
    public final byte nextByte() { return _nextByte; }

    public abstract int typeBits();
    public abstract boolean isLeaf();

    public abstract int serialize(byte[] result, int offset);
    public abstract byte[] serialize();
    
    /*
    /**********************************************************
    /* Factory methods
    /**********************************************************
     */

    public static SimpleLeaf simpleLeaf(byte b, long v) {
        return new SimpleLeaf(b, v);
    }

    public static SimpleBranch simpleBranch(byte b, ClosedTrieNode[] kids) {
        return new SimpleBranch(b, kids);
    }

    public static BranchWithValue valueBranch(byte b, ClosedTrieNode[] kids, long value) {
        return new BranchWithValue(b, kids, value);
    }

    public static SerializedNode serialized(ClosedTrieNode node) {
        return new SerializedNode(node._nextByte, node.serialize());
    }

    public static Leaf suffixLeaf(byte b, ClosedTrieNode node)
    {
        Leaf leaf = (Leaf) node;
        if (leaf instanceof SimpleLeaf) { // convert into suffix one
            return new SuffixLeaf(b, leaf.value(), new byte[] { leaf.nextByte() });
        }
        // already suffixed one
        SuffixLeaf old = (SuffixLeaf) leaf;
        byte[] oldBytes = old._suffix;
        byte[] newBytes = new byte[1 + oldBytes.length];
        System.arraycopy(oldBytes, 0, newBytes, 1, oldBytes.length);
        newBytes[0] = leaf.nextByte();
        return new SuffixLeaf(b, leaf.value(), newBytes);
    }

    /*
    /**********************************************************
    /* Other methods
    /**********************************************************
     */
    
    protected void _addTypeBits(byte[] buffer, int offset)
    {
        int i = buffer[offset];
        buffer[offset] = (byte) (i | (typeBits() << 6));
    }

    @Override
    public int compareTo(ClosedTrieNode o)
    {
        // sort bigger children before shorter ones
        long diff = this.length() - o.length();
        if (diff < 0L) {
            return 1;
        }
        if (diff > 0L) {
            return -1;
        }
        return 0;
    }
    
    /*
    /**********************************************************
    /* Sub-classes
    /**********************************************************
     */

    /**
     * SerializedNode is a full binary serialization of a leaf or branch node;
     * used since it is the most compact representation for most nodes (esp.
     * branch nodes)
     */
    protected final static class SerializedNode
        extends ClosedTrieNode
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

    protected abstract static class Leaf
        extends ClosedTrieNode
    {
        protected final long _value;

        protected Leaf(byte b, long v) {
            super(b);
            _value = v;
        }

        public boolean isLeaf() { return true; }

        public long value() { return _value; }
    }
    
    /**
     * Simple leaf means a leaf with no additional suffix (single-byte/char
     * step). Serialization only has VInt itself; byte that leads to node
     * is retained here to keep branch object simpler.
     */
    protected final static class SimpleLeaf
        extends Leaf
    {
        protected SimpleLeaf(byte b, long v)
        {
            super(b, v);
        }
        
        public long length() {
            return VInt.lengthForUnsigned(_value, FIRST_BYTE_BITS_FOR_LEAVES);
        }

        public int typeBits() { return TYPE_LEAF_SIMPLE; }
        public boolean isLeaf() { return true; }
    
        public byte[] serialize() {
            byte[] result = new byte[VInt.lengthForUnsigned(_value, 6)];
            VInt.unsignedToBytes(_value, FIRST_BYTE_BITS_FOR_LEAVES, result, 0);
            _addTypeBits(result, 0);
            return result;
        }
        public int serialize(byte[] result, int offset)
        {
            int resultOffset = VInt.unsignedToBytes(_value, FIRST_BYTE_BITS_FOR_LEAVES, result, offset);
            _addTypeBits(result, offset);
            return resultOffset;
        }

        public void serializeTo(OutputStream out, byte[] tmpBuf) throws IOException
        {
            // Note: caller must ensure buffer is big enough
            out.write(tmpBuf, 0, serialize(tmpBuf, 0));
        }
    }

    protected final static class SuffixLeaf
        extends Leaf
    {
        protected final byte[] _suffix;
        
        protected SuffixLeaf(byte b, long value, byte[] suffix)
        {
            super(b, value);
            _suffix = suffix;
        }
    
        public long length() {
            int len = _suffix.length;
            return VInt.lengthForUnsigned(_value, FIRST_BYTE_BITS_FOR_LEAVES)
                + VInt.lengthForUnsigned(len, 8)
                + len;
        }
    
        public int typeBits() { return TYPE_LEAF_WITH_SUFFIX; }
    
        public byte[] serialize()
        {
            byte[] result = new byte[(int) length()];
            serialize(result, 0);
            return result;
        }

        public int serialize(byte[] result, int offset)
        {
            int origOffset = offset;
            offset = VInt.unsignedToBytes(_value, FIRST_BYTE_BITS_FOR_LEAVES, result, offset);
            _addTypeBits(result, origOffset);
            int len = _suffix.length;
            offset = VInt.unsignedToBytes(len, 8, result, offset);
            System.arraycopy(_suffix, 0, result, offset, len);
            offset += len;
            return offset;
        }

        public void serializeTo(OutputStream out, byte[] tmpBuf) throws IOException
        {
            int offset = VInt.unsignedToBytes(_value, FIRST_BYTE_BITS_FOR_LEAVES, tmpBuf, 0);
            _addTypeBits(tmpBuf, 0);
            int suffixLen = _suffix.length;
            offset = VInt.unsignedToBytes(suffixLen, 8, tmpBuf, offset);
            out.write(tmpBuf, 0, offset);
            out.write(_suffix, 0, suffixLen);
        }
    }
    
    /**
     * Simple branch just means that branch node does not have associated
     * value. Serialization contains leading VInt for total length of
     * all contained nodes, and sequence of serialization for nodes.
     */
    protected static class SimpleBranch
        extends ClosedTrieNode
    {
        protected final ClosedTrieNode[] _children;
        
        protected SimpleBranch(byte b, ClosedTrieNode[] kids) {
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
            for (ClosedTrieNode n : _children) {
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
            for (ClosedTrieNode n : _children) {
                len += n.length();
            }
            return len;
        }

        protected int serializeChildren(byte[] result, int offset)
        {
            for (ClosedTrieNode n : _children) {
                result[offset++] = n.nextByte();
                offset = n.serialize(result, offset);                
            }
            return offset;
        }
    }

    protected static class BranchWithValue
        extends SimpleBranch
    {
        protected final long _value;
        
        protected BranchWithValue(byte b, ClosedTrieNode[] kids, long value)
        {
            super(b, kids);
            _value = value;
        }

        @Override
        public long length()
        {
            // note: slightly different from super, since we start with value!
            long len = lengthOfContent();
            return len + VInt.lengthForUnsigned(_value, FIRST_BYTE_BITS_FOR_BRANCHES)
                + VInt.lengthForUnsigned(len, 8);
        }
        
        @Override
        public int typeBits() { return TYPE_BRANCH_WITH_VALUE; }

        @Override
        public byte[] serialize()
        {
            long contentLen = lengthOfContent();
            long totalLen = contentLen
                    + VInt.lengthForUnsigned(_value, FIRST_BYTE_BITS_FOR_BRANCHES)
                    + VInt.lengthForUnsigned(contentLen, 8);
            byte[] result = new byte[(int) totalLen];
            // First: serialize value for this node:
            int offset = VInt.unsignedToBytes(_value, FIRST_BYTE_BITS_FOR_BRANCHES, result, 0);
            _addTypeBits(result, 0);
            // then length of content (children)
            offset = VInt.unsignedToBytes(contentLen, 8, result, offset);
            // and then contents
            offset = serializeChildren(result, offset);
            return result;
        }

        @Override
        public int serialize(byte[] result, int offset)
        {
            // First: serialize value for this node:
            int origOffset = offset;
            offset = VInt.unsignedToBytes(_value, FIRST_BYTE_BITS_FOR_BRANCHES, result, offset);
            _addTypeBits(result, origOffset);
            // Then length indicator
            long contentLen = lengthOfContent();
            offset = VInt.unsignedToBytes(contentLen, 8, result, offset);
            // and then contents
            offset = serializeChildren(result, offset);
            if ((origOffset + length()) != offset) throw new IllegalStateException("Internal error: ValueBranch expected length wrong");
            return offset;
        }

        @Override
        public void serializeTo(OutputStream out, byte[] tmpBuf) throws IOException
        {
            // First: serialize value for this node:
            out.write(tmpBuf, 0, VInt.unsignedToBytes(_value, FIRST_BYTE_BITS_FOR_BRANCHES, tmpBuf, 0));
            _addTypeBits(tmpBuf, 0);
            // then length indicator for contents
            long contentLen = lengthOfContent();
            int ptr = VInt.unsignedToBytes(contentLen, 8, tmpBuf, 0);
            out.write(tmpBuf, 0, ptr);
            // then children
            for (ClosedTrieNode n : _children) {
                out.write(n.nextByte());
                n.serializeTo(out, tmpBuf);
            }
        }
    }
}
