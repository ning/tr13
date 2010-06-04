package com.ning.tr13.impl.vint;

import java.io.IOException;
import java.io.OutputStream;

import com.ning.tr13.build.ClosedTrieNode;
import com.ning.tr13.build.ClosedTrieNodeFactory;
import com.ning.tr13.util.VInt;

public class VIntNodeFactory
    extends ClosedTrieNodeFactory<Long>
{
    @Override
    public ClosedTrieNode<Long> serialized(ClosedTrieNode<Long> node) {
        return new SerializedNode<Long>(node.nextByte(), node.serialize());
    }

    @Override
    public ClosedTrieNode<Long> simpleBranch(byte b, ClosedTrieNode<Long>[] kids) {
        return new SimpleBranch<Long>(b, kids);
    }

    @Override
    public ClosedTrieNode<Long> simpleLeaf(byte b, Long value) {
        return new SimpleLeaf(b, value.longValue());
    }

    @Override
    public ClosedTrieNode<Long> suffixLeaf(byte b, ClosedTrieNode<Long> node)
    {
        if (node instanceof SimpleLeaf) { // convert into suffix one
            SimpleLeaf leaf = (SimpleLeaf) node;
            return new SuffixLeaf(b, leaf.value(), new byte[] { leaf.nextByte() });
        }
        // already suffixed one
        SuffixLeaf leaf = (SuffixLeaf) node;
        byte[] oldBytes = leaf._suffix;
        byte[] newBytes = new byte[1 + oldBytes.length];
        System.arraycopy(oldBytes, 0, newBytes, 1, oldBytes.length);
        newBytes[0] = leaf.nextByte();
        return new SuffixLeaf(b, leaf.value(), newBytes);
    }

    @Override
    public ClosedTrieNode<Long> valueBranch(byte b, ClosedTrieNode<Long>[] kids, Long value) {
        return new BranchWithValue(b, kids, value.longValue());
    }

    /*
    /**********************************************************
    /* Concrete node implementations
    /**********************************************************
     */

    /**
     * Simple leaf means a leaf with no additional suffix (single-byte/char
     * step). Serialization only has VInt itself; byte that leads to node
     * is retained here to keep branch object simpler.
     */
    public final static class SimpleLeaf
        extends Leaf<Long>
    {
        protected final long _value;

        protected SimpleLeaf(byte b, long v)
        {
            super(b);
            _value = v;
        }

        public long value() { return _value; }
        
        public long length() {
            return VInt.lengthForUnsigned(_value, FIRST_BYTE_BITS_FOR_LEAVES);
        }

        public int typeBits() { return TYPE_LEAF_SIMPLE; }
        public boolean isLeaf() { return true; }

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

    public final static class SuffixLeaf
        extends Leaf<Long>
    {
        protected final long _value;

        protected final byte[] _suffix;
        
        protected SuffixLeaf(byte b, long value, byte[] suffix)
        {
            super(b);
            _value = value;
            _suffix = suffix;
        }

        public long value() { return _value; }
        
        public long length() {
            int len = _suffix.length;
            return VInt.lengthForUnsigned(_value, FIRST_BYTE_BITS_FOR_LEAVES)
                + VInt.lengthForUnsigned(len, 8)
                + len;
        }
    
        public int typeBits() { return TYPE_LEAF_WITH_SUFFIX; }
    
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
    
    protected static class BranchWithValue
        extends SimpleBranch<Long>
    {
        protected final long _value;
        
        protected BranchWithValue(byte b, ClosedTrieNode<Long>[] kids, long value)
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
            int len = VInt.unsignedToBytes(_value, FIRST_BYTE_BITS_FOR_BRANCHES, tmpBuf, 0);
            _addTypeBits(tmpBuf, 0);
            out.write(tmpBuf, 0, len);
            // then length indicator for contents
            long contentLen = lengthOfContent();
            int ptr = VInt.unsignedToBytes(contentLen, 8, tmpBuf, 0);
            out.write(tmpBuf, 0, ptr);
            // then children
            for (ClosedTrieNode<Long> n : _children) {
                out.write(n.nextByte());
                n.serializeTo(out, tmpBuf);
            }
        }
    }

}
