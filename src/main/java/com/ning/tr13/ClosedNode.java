package com.ning.tr13;

public abstract class ClosedNode
{
    public final static int TYPE_LEAF_SIMPLE = 0;
    public final static int TYPE_LEAF_WITH_PREFIX = 1;
    public final static int TYPE_BRANCH_SIMPLE = 2;    
    public final static int TYPE_BRANCH_WITH_VALUE = 3;

    private static int total = 0;    
    
    protected ClosedNode() {
        if ((++total & 0xFFFFF) == 0) {
            System.out.println("Created "+(total >> 10)+"k closed nodes");
        }
    }
    
    public abstract long length();
    public abstract int typeBits();
    public abstract boolean isLeaf();

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
    
    // // // Sub-classes

    protected final static class SimpleLeaf
        extends ClosedNode
    {
        protected final byte _nextByte;
        protected final long _value;
        
        protected SimpleLeaf(byte b, long value) {
            _nextByte = b;
            _value = value;
        }

        public long length() {
            return VInt.lengthForUnsigned(_value, 6);
        }

        public int typeBits() { return TYPE_LEAF_SIMPLE; }
        public boolean isLeaf() { return true; }
}

    protected static class SimpleBranch
        extends ClosedNode
    {
        protected final byte _nextByte;
        protected final ClosedNode[] _children;
        
        protected SimpleBranch(byte b, ClosedNode[] kids) {
            _nextByte = b;
            _children = kids;
        }
    
        public long length()
        {
            // first one is VInt for total length; but that itself needs to be calculated
            long childLen = 0L;
            for (ClosedNode n : _children) {
                childLen += n.length();
            } 
            // and otherwise it really is just that length and child contents
            return VInt.lengthForUnsigned(childLen, 6) + childLen;            
        }

        public int typeBits() { return TYPE_BRANCH_SIMPLE; }
        public boolean isLeaf() { return false; }
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
    }
}
