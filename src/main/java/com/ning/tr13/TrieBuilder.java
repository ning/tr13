package com.ning.tr13;

import java.io.*;

import com.ning.tr13.build.TrieNode;

/**
 * Definition of interface for classes that can build trie structure.
 * 
 * @author tatu
 */
public abstract class TrieBuilder<T>
    extends TrieConstants
{
 
    /**
     * Method that will build the trie structure, read from input source, and
     * write it to given output stream.
     *<p>
     * NOTE: method will NOT close the output stream; caller has to do that
     * (it will be flushed) however.
     * 
     * @param out
     * @param writeHeader Whether to write trie header before actual data;
     *  this is needed when loading tries back from a File using
     *  read methods, but not when directly constructing trie instances.
     *  
     * @throws IOException
     */
    public abstract void buildAndWrite(OutputStream out, boolean writeHeader)
        throws IOException;

    /**
     * Main build method that will construct full InputCollator and return it as
     * a {@link TrieNode} instance, which can be simply serialized
     * to an {@link OutputStream}
     * 
     * @return Root node
     * @throws IOException
     */
    public abstract TrieNode<T> build() throws IOException;
}
