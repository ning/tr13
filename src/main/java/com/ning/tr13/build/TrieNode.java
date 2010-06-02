package com.ning.tr13.build;

import java.io.IOException;
import java.io.OutputStream;

/**
 * Interface to build-time node instances; mostly needed to express API
 * for writing trie (specified by root node) into result file.
 * 
 * @author tatu
 */
public interface TrieNode
{
    /**
     * @return Total length of this node, including all of its children
     */
    public long length();

    /**
     * Serialization method that will serialize contents into given
     * output stream, possibly using given buffer (to avoid additional
     * memory allocations).
     * Note that result does not include Trie header
     * that is generally need to read the trie back.
     */
    public void serializeTo(OutputStream out, byte[] tmpBuf) throws IOException;

    /**
     * Serialization method that will serialize contents into a byte buffer
     * with exact size needed to hold the contents.
     * Note that result does not include Trie header
     * that is generally need to read the trie back.
     */
    public byte[] serialize();
}
