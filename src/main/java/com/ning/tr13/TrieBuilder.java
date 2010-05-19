package com.ning.tr13;

import java.io.*;

/**
 * Definition of interface for classes that can build trie structure.
 * 
 * @author tatu
 */
public abstract class TrieBuilder
{
    /**
     * We will use simple 16-byte header. First 8 bytes contain signature, version
     * and config bits, and second 8 bytes are 64-bit length of the payload that
     * follows.
     *<p>
     * First 5 bytes are fixed (so first 4 bytes can be
     * used as 'magic cookie' for file type detection; 6th byte contains version number,
     * and 2 remaining bytes are reserved for use as bitfields for variations in file
     * format.
     */
    protected final static byte[] HEADER_TEMPLATE = new byte[] {
        // 5 bytes chosen to be human readable for easy eyeballing
        'T', 'R', '1', '3', '\n',
        // then version number "1.0" in hex; plus 0x80 bit set to force file type as 'binary' (in unix)
        (byte) (0x80 + 0x10),
        // then two spare bytes for future expansion
        0x0, 0x0
    };  
 
    /**
     * Method that will build the trie structure, read from input source, and
     * write it to given output stream.
     *<p>
     * NOTE: method will NOT close the output stream; caller has to do that
     * (it will be flushed) however.
     * 
     * @param out
     * @throws IOException
     */
    public abstract void buildAndWrite(OutputStream out) throws IOException;

    /**
     * Main build method that will construct full InputCollator and return it as
     * a {@link ClosedNode} instance, which can be simply serialized
     * to an {@link OutputStream}
     * 
     * @return Root node
     * @throws IOException
     */
    public abstract TrieNode build() throws IOException;
}
