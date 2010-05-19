package com.ning.tr13;

import java.io.*;

/**
 * Definition of interface for classes that can build trie structure.
 * 
 * @author tatu
 */
public abstract class TrieBuilder
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
