package com.ning.tr13.build;

import java.io.IOException;
import java.io.OutputStream;

import com.ning.tr13.TrieNode;
import com.ning.tr13.util.VInt;

/**
 * This class represents in-memory nodes that are ready to be serialized.
 * 
 * @author tatu
 */
public abstract class ClosedNode
    implements TrieNode
{
    /**
     * This constants is used as safe minimum size for temporary
     * buffer to pass to {@link #writeTo} method. It is actually
     * set to quite a bit higher than strict minimum, just to give
     * some room for expansion in case structure changes.
     */
    public final static int MINIMUM_TEMP_BUFFER_LENGTH = 64;
    
    // // // Type  bits
    
    public final static int TYPE_LEAF_SIMPLE = 0;
    public final static int TYPE_LEAF_WITH_SUFFIX = 1;
    public final static int TYPE_BRANCH_SIMPLE = 2;    
    public final static int TYPE_BRANCH_WITH_VALUE = 3;

    /**
     * Byte that parent node (branch) will use to branch into this node.
     */
    protected final byte _nextByte;
    
    protected ClosedNode(byte nb)
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

    public static SimpleBranch simpleBranch(byte b, ClosedNode[] kids) {
        return new SimpleBranch(b, kids);
    }

    public static BranchWithValue valueBranch(byte b, ClosedNode[] kids, long value) {
        return new BranchWithValue(b, kids, value);
    }

    public static SerializedNode serialized(ClosedNode node) {
        return new SerializedNode(node._nextByte, node.serialize());
    }

    public static Leaf suffixLeaf(byte b, ClosedNode node)
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
        extends ClosedNode
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
    
        public byte[] serialize() { return _data; }
        public int serialize(byte[] result, int offset) {
            int len = _data.length;
            System.arraycopy(_data, 0, result, offset, len);
            return offset+len;
        }

        public void serializeTo(OutputStream out, byte[] tmpBuf) throws IOException
        {
            out.write(_data);
        }
    }

    protected abstract static class Leaf
        extends ClosedNode
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
            return VInt.lengthForUnsigned(_value, 6);
        }

        public int typeBits() { return TYPE_LEAF_SIMPLE; }
        public boolean isLeaf() { return true; }
    
        public byte[] serialize() {
            byte[] result = new byte[VInt.lengthForUnsigned(_value, 6)];
            VInt.unsignedToBytes(_value, 6, result, 0);
            _addTypeBits(result, 0);
            return result;
        }
        public int serialize(byte[] result, int offset)
        {
            int resultOffset = VInt.unsignedToBytes(_value, 6, result, offset);
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

        static int count = 0;
        
        protected SuffixLeaf(byte b, long value, byte[] suffix)
        {
            super(b, value);
            _suffix = suffix;
        }
    
        public long length() {
            int len = _suffix.length;
            return VInt.lengthForUnsigned(_value, 6)
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
            offset = VInt.unsignedToBytes(_value, 6, result, offset);
            _addTypeBits(result, origOffset);
            int len = _suffix.length;
            offset = VInt.unsignedToBytes(len, 8, result, 0);
            System.arraycopy(_suffix, 0, result, offset, len);
            offset += len;
            return offset;
        }

        public void serializeTo(OutputStream out, byte[] tmpBuf) throws IOException
        {
            // Note: caller must ensure buffer is big enough
            out.write(tmpBuf, 0, serialize(tmpBuf, 0));
        }
    }
    
    /**
     * Simple branch just means that branch node does not have associated
     * value. Serialization contains leading VInt for total length of
     * all contained nodes, and sequence of serialization for nodes.
     */
    protected static class SimpleBranch
        extends ClosedNode
    {
        protected final ClosedNode[] _children;
        
        protected SimpleBranch(byte b, ClosedNode[] kids) {
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
            byte[] result = new byte[(int) (contentLen + VInt.lengthForUnsigned(contentLen, 6))];
            // First: serialize length indicator
            int offset = VInt.unsignedToBytes(contentLen, 6, result, 0);
            _addTypeBits(result, 0);
            offset = serializeChildren(result, offset);
            return result;
        }

        public int serialize(byte[] result, int offset)
        {
            long contentLen = lengthOfContent();
            // First: serialize length indicator
            int origOffset = offset;
            offset = VInt.unsignedToBytes(contentLen, 6, result, offset);
            _addTypeBits(result, origOffset);
            offset = serializeChildren(result, offset);
            return offset;
        }

        public void serializeTo(OutputStream out, byte[] tmpBuf) throws IOException
        {
            long contentLen = lengthOfContent();
            // first simple length indicator
            int ptr = VInt.unsignedToBytes(contentLen, 6, tmpBuf, 0);
            _addTypeBits(tmpBuf, 0);
            out.write(tmpBuf, 0, ptr);
            // then children
            for (ClosedNode n : _children) {
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
            for (ClosedNode n : _children) {
                len += n.length();
            }
            return len;
        }

        protected int serializeChildren(byte[] result, int offset)
        {
            for (ClosedNode n : _children) {
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
        
        protected BranchWithValue(byte b, ClosedNode[] kids, long value)
        {
            super(b, kids);
            _value = value;
        }
    
        public long length()
        {
            // same as with simple branch, plus value; value has all bits to use
            return super.length() + VInt.lengthForUnsigned(_value, 8);
        }

        public int typeBits() { return TYPE_BRANCH_WITH_VALUE; }

        public byte[] serialize()
        {
            long contentLen = length();
            byte[] result = new byte[(int) contentLen];
            // First: serialize length indicator
            int offset = VInt.unsignedToBytes(contentLen, 6, result, 0);
            _addTypeBits(result, 0);
            // Then value for this node
            offset = VInt.unsignedToBytes(_value, 8, result, offset);
            // then contents
            offset = serializeChildren(result, offset);
            return result;
        }

        public int serialize(byte[] result, int offset)
        {
            long contentLen = length();
            // First: serialize length indicator
            int origOffset = offset;
            offset = VInt.unsignedToBytes(contentLen, 6, result, offset);
            _addTypeBits(result, origOffset);
            // Then value for this node
            offset = VInt.unsignedToBytes(_value, 8, result, offset);
            offset = serializeChildren(result, offset);
            return offset;
        }

        public void serializeTo(OutputStream out, byte[] tmpBuf) throws IOException
        {
            long contentLen = lengthOfContent();
            // first simple length indicator
            int ptr = VInt.unsignedToBytes(contentLen, 6, tmpBuf, 0);
            _addTypeBits(tmpBuf, 0);
            out.write(tmpBuf, 0, ptr);
            // Then value for this node
            out.write(tmpBuf, 0, VInt.unsignedToBytes(_value, 8, tmpBuf, 0));
            // then children
            for (ClosedNode n : _children) {
                n.serializeTo(out, tmpBuf);
            }
        }
    }
}
