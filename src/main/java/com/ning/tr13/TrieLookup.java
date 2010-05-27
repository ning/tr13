package com.ning.tr13;

import java.io.*;
import java.nio.ByteBuffer;
import java.util.NoSuchElementException;

import com.ning.tr13.read.ByteBufferTrie;
import com.ning.tr13.read.TrieHeader;
import com.ning.tr13.util.InputUtil;

/**
 * Class used for reading persistent trie structure, and accessing values it
 * has. Since there are multiple backend implementations, this is an abstract
 * class with API and factory methods for creating specialized instances.
 * 
 * @author tatu
 */
public abstract class TrieLookup
{
    protected TrieLookup() { }
    
    /*
    /********************************************************** 
    /* Factory methods
    /********************************************************** 
     */

    public static TrieLookup createFromFile(File f) throws IOException
    {
        return createByteBufferBased(f, new DirectByteBufferAllocator());
    }

    public static TrieLookup createByteArrayBased(File f)
    {
        long len = f.length();
        // note: ByteBuffer only good up to 2 gigs...
        if (len > Integer.MAX_VALUE) {
            // TODO: segmented version
            throw new IllegalArgumentException("File '"+f.getAbsolutePath()+"' over 2 gigs in size: ByteBuffer max size 2 gigs");
        }
        // !!! TBI
        return null;
    }
    
    public static TrieLookup createByteBufferBased(File f, ByteBufferAllocator a)
        throws IOException
    {
        FileInputStream fis = new FileInputStream(f);        
        TrieLookup trie = createByteBufferBased(fis, f.length(), a);
        fis.close();
        return trie;
    }

    public static TrieLookup createByteBufferBased(InputStream in, long inputLength, ByteBufferAllocator a)
        throws IOException
    {
        // note: ByteBuffer only good up to 2 gigs...
        if (inputLength > Integer.MAX_VALUE) {
            // TODO: segmented version
            throw new IllegalArgumentException("Input is over 2 gigs in size: ByteBuffer max size 2 gigs");
        }
        byte[] buffer = new byte[16000];
        InputUtil.readFully(in, buffer, 0, TrieHeader.HEADER_LENGTH);   
        TrieHeader h = TrieHeader.read(buffer, 0);
        int len = (int) h.getPayloadLength();
        ByteBuffer bb = a.allocate(len);
        
        while (len > 0) {
            int count = in.read(buffer, 0, Math.min(len, buffer.length));
            if (count < 0) {
                throw new IOException("Unexpected end-of-stream: still needed to read "+len+" bytes");
            }
            bb.put(buffer, 0, count);
            len -= count;
        }
        return new ByteBufferTrie(bb, (int) h.getPayloadLength());
    }
    
    /*
    /********************************************************** 
    /* Public API
    /********************************************************** 
     */

    public abstract long getValue(byte[] key) throws NoSuchElementException;

    public abstract long getValue(byte[] key, long defaultValue);

    public abstract Long findValue(byte[] key);


    /*
    /********************************************************** 
    /* Internal methods
    /********************************************************** 
     */

    protected static String _printKey(byte[] buffer, int offset, int len)
    {
        StringBuilder sb = new StringBuilder(20);
        sb.append("0x");
        // Print first and last 4 bytes
        if (len <= 8) {
            
        } else {
            for (int i = 0; i < 4; ++i) {
            }
        }
        return sb.toString();
    }
    
    /*
    /********************************************************** 
    /* Helper classes
    /********************************************************** 
     */

    public interface ByteBufferAllocator
    {
        public ByteBuffer allocate(int size);
    }

    public static class DirectByteBufferAllocator
        implements ByteBufferAllocator
    {
        public ByteBuffer allocate(int size)
        {
            return ByteBuffer.allocateDirect(size);
        }
    }
}
