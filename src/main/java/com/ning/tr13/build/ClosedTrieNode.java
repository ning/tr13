package com.ning.tr13.build;

import java.io.IOException;
import java.io.OutputStream;

import com.ning.tr13.TrieConstants;

/**
 * This class represents in-memory nodes that are ready to be serialized.
 * 
 * @author tatu
 */
public abstract class ClosedTrieNode<T>
    extends TrieConstants
    implements TrieNode<T>, Comparable<ClosedTrieNode<T>>
{
    /**
     * This constants is used as safe minimum size for temporary
     * buffer to pass to {@link #serializeTo} method. It is actually
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

    @Override
    public abstract long length();

    @Override
    public byte[] serialize()
    {
        byte[] result = new byte[(int) length()];
        serialize(result, 0);
        return result;
    }

    @Override
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
    public int compareTo(ClosedTrieNode<T> o)
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
}
