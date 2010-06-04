package com.ning.tr13.impl.bytes;

import java.io.IOException;
import java.io.OutputStream;

import com.ning.tr13.build.ClosedTrieNode;
import com.ning.tr13.build.ClosedTrieNodeFactory;
import com.ning.tr13.build.ClosedTrieNode.Leaf;
import com.ning.tr13.build.ClosedTrieNode.SerializedNode;
import com.ning.tr13.build.ClosedTrieNode.SimpleBranch;
import com.ning.tr13.util.VInt;

public class BytesNodeFactory
    extends ClosedTrieNodeFactory<byte[]>
{
    @Override
    public ClosedTrieNode<byte[]> serialized(ClosedTrieNode<byte[]> node) {
        return new SerializedNode<byte[]>(node.nextByte(), node.serialize());
    }

    @Override
    public ClosedTrieNode<byte[]> simpleBranch(byte b, ClosedTrieNode<byte[]>[] kids) {
        return new SimpleBranch<byte[]>(b, kids);
    }

    @Override
    public ClosedTrieNode<byte[]> simpleLeaf(byte b, byte[] value) {
        return new SimpleLeaf(b, value);
    }

    @Override
    public ClosedTrieNode<byte[]> suffixLeaf(byte b, ClosedTrieNode<byte[]> node)
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
    public ClosedTrieNode<byte[]> valueBranch(byte b, ClosedTrieNode<byte[]>[] kids, byte[] value) {
        return new BranchWithValue(b, kids, value);
    }

    protected static int copyBytes(byte[] src, byte[] dst, int dstOffset)
    {
        int len = src.length;
        System.arraycopy(src, 0, dst, dstOffset, len);
        return dstOffset + len;
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
        extends Leaf<byte[]>
    {
        protected final byte[] _value;

        protected SimpleLeaf(byte b, byte[] v)
        {
            super(b);
            _value = v;
        }

        public byte[] value() { return _value; }
        
        public long length() {
            int dataLen = _value.length;
            return VInt.lengthForUnsigned(dataLen, FIRST_BYTE_BITS_FOR_LEAVES) + dataLen;
        }

        public int typeBits() { return TYPE_LEAF_SIMPLE; }
        public boolean isLeaf() { return true; }

        public int serialize(byte[] result, int offset)
        {
            int origOffset = offset;
            offset = VInt.unsignedToBytes(_value.length, FIRST_BYTE_BITS_FOR_LEAVES, result, 0);
            _addTypeBits(result, origOffset);
            return copyBytes(_value, result, offset);
        }

        public void serializeTo(OutputStream out, byte[] tmpBuf) throws IOException
        {
            // due to unlimited length of value, can't just delegate to serialize...
            int len = VInt.unsignedToBytes(_value.length, FIRST_BYTE_BITS_FOR_LEAVES, tmpBuf, 0);
            _addTypeBits(tmpBuf, 0);
            out.write(tmpBuf, 0, len);
            out.write(_value);
        }
    }

    public final static class SuffixLeaf
        extends Leaf<byte[]>
    {
        protected final byte[] _value;

        protected final byte[] _suffix;
        
        protected SuffixLeaf(byte b, byte[] value, byte[] suffix)
        {
            super(b);
            _value = value;
            _suffix = suffix;
        }

        public byte[] value() { return _value; }
        
        public long length() {
            int valueLen = _value.length;
            int suffixLen = _suffix.length;
            return VInt.lengthForUnsigned(valueLen, FIRST_BYTE_BITS_FOR_LEAVES)
                + valueLen
                + VInt.lengthForUnsigned(suffixLen, 8)
                + suffixLen;
        }
    
        public int typeBits() { return TYPE_LEAF_WITH_SUFFIX; } 
    
        public int serialize(byte[] result, int offset)
        {
            int origOffset = offset;
            int valueLen = _value.length;
            offset = VInt.unsignedToBytes(valueLen, FIRST_BYTE_BITS_FOR_LEAVES, result, offset);
            _addTypeBits(result, origOffset);
            offset = copyBytes(_value, result, offset);
            int suffixLen = _suffix.length;
            offset = VInt.unsignedToBytes(suffixLen, 8, result, offset);
            return copyBytes(_suffix, result, offset);
        }

        public void serializeTo(OutputStream out, byte[] tmpBuf) throws IOException
        {
            int len = VInt.unsignedToBytes(_value.length, FIRST_BYTE_BITS_FOR_LEAVES, tmpBuf, 0);
            _addTypeBits(tmpBuf, 0);
            out.write(tmpBuf, 0, len);
            out.write(_value);
            out.write(tmpBuf, 0, VInt.unsignedToBytes(_suffix.length, 8, tmpBuf, 0));
            out.write(_suffix);
        }
    }
    
    protected static class BranchWithValue
        extends SimpleBranch<byte[]>
    {
        protected final byte[] _value;
        
        protected BranchWithValue(byte b, ClosedTrieNode<byte[]>[] kids, byte[] value)
        {
            super(b, kids);
            _value = value;
        }
    
        @Override
        public long length()
        {
            // note: slightly different from super, since we start with value!
            long contentLen = lengthOfContent();
            int valueLen = _value.length;
            return VInt.lengthForUnsigned(valueLen, FIRST_BYTE_BITS_FOR_BRANCHES) + valueLen
                + VInt.lengthForUnsigned(contentLen, 8) + contentLen;
        }
        
        @Override
        public int typeBits() { return TYPE_BRANCH_WITH_VALUE; }
    
        @Override
        public byte[] serialize()
        {
            long contentLen = lengthOfContent();
            final int valueLen = _value.length;
            long totalLen = VInt.lengthForUnsigned(valueLen, FIRST_BYTE_BITS_FOR_BRANCHES) + valueLen
                    + VInt.lengthForUnsigned(contentLen, 8) + contentLen;
            byte[] result = new byte[(int) totalLen];
            // First: serialize value for this node:
            int offset = VInt.unsignedToBytes(valueLen, FIRST_BYTE_BITS_FOR_BRANCHES, result, 0);
            _addTypeBits(result, 0);
            offset = copyBytes(_value, result, offset);
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
            final int valueLen = _value.length;
            int origOffset = offset;
            offset = VInt.unsignedToBytes(valueLen, FIRST_BYTE_BITS_FOR_BRANCHES, result, offset);
            _addTypeBits(result, origOffset);
            offset = copyBytes(_value, result, offset);
            // Then content length indicator
            long contentLen = lengthOfContent();
            offset = VInt.unsignedToBytes(contentLen, 8, result, offset);
            // and contents
            offset = serializeChildren(result, offset);
            // sanity check (optional)
            if ((origOffset + length()) != offset) throw new IllegalStateException("Internal error: ValueBranch expected length wrong");
            return offset;
        }
    
        @Override
        public void serializeTo(OutputStream out, byte[] tmpBuf) throws IOException
        {
            // First: serialize value for this node:
            final int valueLen = _value.length;
            out.write(tmpBuf, 0, VInt.unsignedToBytes(valueLen, FIRST_BYTE_BITS_FOR_BRANCHES, tmpBuf, 0));
            _addTypeBits(tmpBuf, 0);
            out.write(_value);
            // then length indicator for contents
            long contentLen = lengthOfContent();
            int ptr = VInt.unsignedToBytes(contentLen, 8, tmpBuf, 0);
            out.write(tmpBuf, 0, ptr);
            // then children
            for (ClosedTrieNode<byte[]> n : _children) {
                out.write(n.nextByte());
                n.serializeTo(out, tmpBuf);
            }
        }
    }

}
