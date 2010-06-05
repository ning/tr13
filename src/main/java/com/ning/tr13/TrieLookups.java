package com.ning.tr13;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.ByteBuffer;

import com.ning.tr13.impl.bytes.ByteArrayBytesTrieLookup;
import com.ning.tr13.impl.bytes.ByteBufferBytesTrieLookup;
import com.ning.tr13.impl.vint.ByteArrayVIntTrieLookup;
import com.ning.tr13.impl.vint.ByteBufferVIntTrieLookup;
import com.ning.tr13.lookup.BytesTrieLookup;
import com.ning.tr13.lookup.TrieHeader;
import com.ning.tr13.lookup.VIntTrieLookup;
import com.ning.tr13.util.InputUtil;

/**
 * Factory class for instantiating {@link TrieLookup} instances
 * of all types.
 */
public class TrieLookups
{
    private TrieLookups() { }
    
    /**
     * Factory method that will read VInt (~= Long) valued Trie from given file
     * and construct a lookup instance that uses direct byte buffer for storing
     * and accessing raw trie data during lookups.
     */
    public static VIntTrieLookup readVIntTrie(File f) throws IOException
    {
        return readByteBufferVIntTrie(f, new DirectByteBufferAllocator());
    }

    /**
     * Factory method that will read byte[] valued Trie from given file
     * and construct a lookup instance that uses direct byte buffer for storing
     * and accessing raw trie data during lookups.
     */
    /*
    public static BytesTrieLookup readBytesTrie(File f) throws IOException
    {
        return readByteBufferBased(f, new DirectByteBufferAllocator());
    }
    */
    
    /*
    /********************************************************** 
    /* Factory methods, "raw"; used when raw trie data structure
    /* is already in memory
    /********************************************************** 
     */

    /**
     * Method for constructing variable int (VInt) valued tries, using
     * raw byte array as is for lookup.
     */
    public static VIntTrieLookup constructByteArrayVIntTrie(byte[] raw)
    {
        return new ByteArrayVIntTrieLookup(raw);
    }

    /**
     * Method for constructing variable int (VInt) valued tries,
     * by copying give byte array contents into a (direct) byte buffer
     * used for lookups.
     */
    public static VIntTrieLookup constructByteBufferVIntTrie(byte[] raw)
    {
        return constructByteBufferVIntTrie(raw, new DirectByteBufferAllocator());
    }

    /**
     * Method for constructing variable int (VInt) valued tries,
     * by copying give byte array contents into a (direct) byte buffer
     * used for lookups.
     */
    public static VIntTrieLookup constructByteBufferVIntTrie(byte[] raw,
            ByteBufferAllocator a)
    {
    	ByteBuffer bb = _arrayToBuffer(raw, a);
        return new ByteBufferVIntTrieLookup(bb, raw.length);
    }
    
    /**
     * Method for constructing byte[] ("bytes") valued tries, using
     * raw byte array as is for lookup.
     */
    public static BytesTrieLookup constructByteArrayBytesTrie(byte[] raw)
    {
        return new ByteArrayBytesTrieLookup(raw);
    }

    /**
     * Method for constructing byte[] ("bytes") valued tries, using
     * by copying give byte array contents into a (direct) byte buffer
     * used for lookups.
     */
    public static BytesTrieLookup constructByteBufferBytesTrie(byte[] raw)
    {
        return constructByteBufferBytesTrie(raw, new DirectByteBufferAllocator());
    }

    /**
     * Method for constructing byte[] ("bytes") valued tries, using
     * by copying give byte array contents into a (direct) byte buffer
     * used for lookups.
     */
    public static BytesTrieLookup constructByteBufferBytesTrie(byte[] raw,
            ByteBufferAllocator a)
    {
    	ByteBuffer bb = _arrayToBuffer(raw, a);
        return new ByteBufferBytesTrieLookup(bb, raw.length);
    }

    /*
    /********************************************************** 
    /* Factory methods, from files etc, for VInt-valued tries
    /********************************************************** 
     */

    public static VIntTrieLookup readByteArrayVIntTrie(File f) throws IOException
    {
        FileInputStream fis = new FileInputStream(f);
        VIntTrieLookup trie = readByteArrayVIntTrie(fis);
        fis.close();
        return trie;
    }

    public static VIntTrieLookup readByteArrayVIntTrie(InputStream in) throws IOException
    {
        TrieHeader header = _readHeader(in, true);
        int len = (int) header.getPayloadLength();
        byte[] buffer = new byte[len];
        InputUtil.readFully(in, buffer, 0, len);
        return new ByteArrayVIntTrieLookup(buffer);
    }

    /**
     * Note: defaults to using {@link ByteBufferAllocator} that
     * allocates direct (native, non-Java) byte buffers to hold
     * raw trie data
     */
    public static VIntTrieLookup readByteBufferVIntTrie(File f)
        throws IOException
    {
        return readByteBufferVIntTrie(f, new DirectByteBufferAllocator());
    }
    
    public static VIntTrieLookup readByteBufferVIntTrie(File f, ByteBufferAllocator a)
        throws IOException
    {
        FileInputStream fis = new FileInputStream(f);
        VIntTrieLookup trie = readByteBufferVIntTrie(fis, a);
        fis.close();
        return trie;
    }

    /**
     * Note: defaults to using {@link ByteBufferAllocator} that
     * allocates direct (native, non-Java) byte buffers to hold
     * raw trie data
     */
    public static VIntTrieLookup readByteBufferVIntTrie(InputStream in)
        throws IOException
    {
        return readByteBufferVIntTrie(in, new DirectByteBufferAllocator());
    }

    public static VIntTrieLookup readByteBufferVIntTrie(InputStream in, ByteBufferAllocator a)
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
        return new ByteBufferVIntTrieLookup(bb, len);
    }

    /*
    /********************************************************** 
    /* Factory methods, from files etc, for byte[]-valued tries
    /********************************************************** 
     */

    public static BytesTrieLookup readByteArrayBytesTrie(File f) throws IOException
    {
        FileInputStream fis = new FileInputStream(f);
        BytesTrieLookup trie = readByteArrayBytesTrie(fis);
        fis.close();
        return trie;
    }

    public static BytesTrieLookup readByteArrayBytesTrie(InputStream in) throws IOException
    {
        TrieHeader header = _readHeader(in, true);
        int len = (int) header.getPayloadLength();
        byte[] buffer = new byte[len];
        InputUtil.readFully(in, buffer, 0, len);
        return new ByteArrayBytesTrieLookup(buffer);
    }

    /**
     * Note: defaults to using {@link ByteBufferAllocator} that
     * allocates direct (native, non-Java) byte buffers to hold
     * raw trie data
     */
    public static BytesTrieLookup readByteBufferBytesTrie(File f)
        throws IOException
    {
        return readByteBufferBytesTrie(f, new DirectByteBufferAllocator());
    }
    
    public static BytesTrieLookup readByteBufferBytesTrie(File f, ByteBufferAllocator a)
        throws IOException
    {
        FileInputStream fis = new FileInputStream(f);
        BytesTrieLookup trie = readByteBufferBytesTrie(fis, a);
        fis.close();
        return trie;
    }

    /**
     * Note: defaults to using {@link ByteBufferAllocator} that
     * allocates direct (native, non-Java) byte buffers to hold
     * raw trie data
     */
    public static BytesTrieLookup readByteBufferBytesTrie(InputStream in)
        throws IOException
    {
        return readByteBufferBytesTrie(in, new DirectByteBufferAllocator());
    }

    public static BytesTrieLookup readByteBufferBytesTrie(InputStream in, ByteBufferAllocator a)
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
        return new ByteBufferBytesTrieLookup(bb, len);
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

    protected static ByteBuffer _arrayToBuffer(byte[] data, ByteBufferAllocator allocator)
    {
    	ByteBuffer bb = allocator.allocate(data.length);
    	bb.put(data);
    	return bb;
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

    /**
     * Concrete {@link ByteBufferAllocator} instance that allocates
     * direct byte buffers.
     */
    public static class DirectByteBufferAllocator
        implements ByteBufferAllocator
    {
        public ByteBuffer allocate(int size)
        {
            return ByteBuffer.allocateDirect(size);
        }
    }
    
}
