package com.ning.tr13;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.ByteBuffer;

import com.ning.tr13.lookup.ByteArrayTrie;
import com.ning.tr13.lookup.ByteBufferTrie;
import com.ning.tr13.lookup.TrieHeader;
import com.ning.tr13.util.InputUtil;

/**
 * Factory class for instantiating {@link TrieLookup} instances
 * of all types.
 */
public class TrieLookups
{
    private TrieLookups() { }
    
    /**
     * Factory method that will read Trie from given file, and construct
     * an instance that uses direct byte buffer for storing and accessing
     * raw trie data during lookups.
     */
    public static TrieLookup read(File f) throws IOException
    {
        return readByteBufferBased(f, new DirectByteBufferAllocator());
    }

    /*
    /********************************************************** 
    /* Factory methods, "raw"
    /********************************************************** 
     */

    public static TrieLookup constructByteArrayBased(byte[] raw)
    {
        return new ByteArrayTrie(raw);
    }

    public static TrieLookup constructByteBufferBased(byte[] raw)
    {
        return new ByteArrayTrie(raw);
    }

    public static TrieLookup constructByteBufferBased(byte[] raw,
            ByteBufferAllocator a)
    {
        return new ByteArrayTrie(raw);
    }
    
    /*
    /********************************************************** 
    /* Factory methods, from files etc
    /********************************************************** 
     */

    public static TrieLookup readByteArrayBased(File f) throws IOException
    {
        FileInputStream fis = new FileInputStream(f);
        TrieLookup trie = readByteArrayBased(fis);
        fis.close();
        return trie;
    }

    public static TrieLookup readByteArrayBased(InputStream in) throws IOException
    {
        TrieHeader header = _readHeader(in, true);
        int len = (int) header.getPayloadLength();
        byte[] buffer = new byte[len];
        InputUtil.readFully(in, buffer, 0, len);
        return new ByteArrayTrie(buffer);
    }

    /**
     * Note: defaults to using {@link ByteBufferAllocator} that
     * allocates direct (native, non-Java) byte buffers to hold
     * raw trie data
     */
    public static TrieLookup readByteBufferBased(File f)
        throws IOException
    {
        return readByteBufferBased(f, new DirectByteBufferAllocator());
    }
    
    public static TrieLookup readByteBufferBased(File f, ByteBufferAllocator a)
        throws IOException
    {
        FileInputStream fis = new FileInputStream(f);
        TrieLookup trie = readByteBufferBased(fis, a);
        fis.close();
        return trie;
    }

    /**
     * Note: defaults to using {@link ByteBufferAllocator} that
     * allocates direct (native, non-Java) byte buffers to hold
     * raw trie data
     */
    public static TrieLookup readByteBufferBased(InputStream in)
        throws IOException
    {
        return readByteBufferBased(in, new DirectByteBufferAllocator());
    }

    public static TrieLookup readByteBufferBased(InputStream in, ByteBufferAllocator a)
        throws IOException
    {
        TrieHeader header = _readHeader(in, true);
        int len = (int) header.getPayloadLength();
        ByteBuffer bb = a.allocate(len);
        byte[] buffer = new byte[16000];
        while (len > 0) {
            int count = in.read(buffer, 0, Math.min(len, buffer.length));
            if (count < 0) {
                throw new IOException("Unexpected end-of-stream: still needed to read "+len+" bytes");
            }
            bb.put(buffer, 0, count);
            len -= count;
        }
        return new ByteBufferTrie(bb, len);
    }

    /*
    /********************************************************** 
    /* Internal methods
    /********************************************************** 
     */
    
    protected static TrieHeader _readHeader(InputStream in, boolean twoGigMax) throws IOException
    {
        byte[] buffer = new byte[TrieHeader.HEADER_LENGTH];
        InputUtil.readFully(in, buffer, 0, TrieHeader.HEADER_LENGTH);   
        TrieHeader h = TrieHeader.read(buffer, 0);
        if (twoGigMax) {
            if (h.getPayloadLength() > Integer.MAX_VALUE) {
                throw new IllegalArgumentException("Trie over 2 gigs in size: max size 2 gigs");
            }
        }
        return h;
    }  
    
    /*
    /********************************************************** 
    /* Helper classes
    /********************************************************** 
     */

    /**
     * Interface for helper object that allocates
     * {@link ByteBuffer}s; defined to allow allocating
     * direct and non-direct byte buffers.
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
