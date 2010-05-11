package com.ning.tr13;

public abstract class ClosedNode
{
    public final static int TYPE_LEAF_SIMPLE = 0;
    public final static int TYPE_LEAF_WITH_PREFIX = 1;
    public final static int TYPE_BRANCH_SIMPLE = 2;    
    public final static int TYPE_BRANCH_WITH_VALUE = 3;

    //private static int totalCreated = 0;    

    /**
     * Byte that parent node (branch) will use to branch into this node.
     */
    protected final byte _nextByte;
    
    protected ClosedNode(byte nb)
    {
        _nextByte = nb;
        //if ((++totalCreated & 0x3FFFFFF) == 0) System.out.println("Created "+(totalCreated >> 10)+"k closed nodes");
    }

    public final byte nextByte() { return _nextByte; }
    
    public abstract long length();
    public abstract int typeBits();
    public abstract boolean isLeaf();

    public abstract int serialize(byte[] result, int offset);
    public abstract byte[] serialize();

    // // // Factory methods

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
    
    // // // Sub-classes

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
    }
    
    /**
     * Simple leaf means a leaf with no additional suffix (single-byte/char
     * step). Serialization only has VInt itself; byte that leads to node
     * is retained here to keep branch object simpler.
     */
    protected final static class SimpleLeaf
        extends ClosedNode
    {
        protected final long _value;
        
        protected SimpleLeaf(byte b, long value)
        {
            super(b);
            _value = value;
        }

        public long length() {
            return VInt.lengthForUnsigned(_value, 6);
        }

        public int typeBits() { return TYPE_LEAF_SIMPLE; }
        public boolean isLeaf() { return true; }
    
        public byte[] serialize() {
            byte[] result = new byte[VInt.lengthForUnsigned(_value, 6)];
            VInt.unsignedToBytes(_value, 6, result, 0);
            return result;
        }
        public int serialize(byte[] result, int offset) {
            offset += VInt.unsignedToBytes(_value, 6, result, offset);
            return offset;
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
            long contentLen = length();
            byte[] result = new byte[(int) contentLen];
            // First: serialize length indicator
            int offset = VInt.unsignedToBytes(contentLen, 6, result, 0);
            for (ClosedNode n : _children) {
                result[offset++] = n.nextByte();
                offset = n.serialize(result, offset);                
            }
            return result;
        }

        public int serialize(byte[] result, int offset)
        {
            long contentLen = length();
            // First: serialize length indicator
            offset = VInt.unsignedToBytes(contentLen, 6, result, offset);
            for (ClosedNode n : _children) {
                result[offset++] = n.nextByte();                
                offset = n.serialize(result, offset);
            }
            return offset;
        }
        
        /**
         * Helper method that calculates length of all contained data (children,
         * branching bytes).
         */
        private long lengthOfContent()
        {
            // one byte per child for branching:
            long len = (long) _children.length;
            // and then child serializations:
            for (ClosedNode n : _children) {
                len += n.length();
            }
            return len;
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
            // Then value for this node
            offset = VInt.unsignedToBytes(_value, 8, result, offset);
            // then contents
            for (ClosedNode n : _children) {
                result[offset++] = n.nextByte();
                offset = n.serialize(result, offset);              
            }
            return result;
        }

        public int serialize(byte[] result, int offset)
        {
            long contentLen = length();
            // First: serialize length indicator
            offset = VInt.unsignedToBytes(contentLen, 6, result, offset);
            // Then value for this node
            offset = VInt.unsignedToBytes(_value, 8, result, offset);
            for (ClosedNode n : _children) {
                result[offset++] = n.nextByte();                
                offset = n.serialize(result, offset);
            }
            return offset;
        }
    }
}
