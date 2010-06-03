package com.ning.tr13.build;

import java.io.*;

import com.ning.tr13.KeyValueReader;
import com.ning.tr13.TrieBuilder;
import com.ning.tr13.lookup.TrieHeader;

/**
 * Straight-forward builder implementation that reads data using
 * given {@link KeyValueReader}, and writes results into an
 * output stream (typical File).
 */
public abstract class SimpleTrieBuilder<T>
    extends TrieBuilder
{
    protected final KeyValueReader<T> _reader;

    /**
     * Flag to enable crude diagnostics to STDOUT
     */
    protected final boolean _diagnostics;

    protected int _linesRead;

    /**
     * Whether builder is allowed to reorder entries for branch
     * nodes. If true, builder can (and will) reorder entries so that
     * biggest entries come first; this since it should improve average
     * access time. If false, entries will be written in exact order
     * they have been added in.
     */
    protected boolean _reorderEntries;
    
    public SimpleTrieBuilder(KeyValueReader<T> r) {
        this(r, false);
    }
    
    public SimpleTrieBuilder(KeyValueReader<T> r, boolean diagnostics) {
        _reader = r;
        _diagnostics = diagnostics;
    }

    public SimpleTrieBuilder<T> setReorderEntries(boolean b) {
        _reorderEntries = b;
        return this;
    }
    
    /**
     * Method for building trie in-memory structure, and writing it out
     * using given output stream.
     * 
     * @param out Output stream to write trie structure to
     */
    @Override
    public void buildAndWrite(OutputStream out, boolean writeHeader)
        throws IOException
    {
        // first, build trie
        TrieNode root = build();
        byte[] tmpBuffer = new byte[ClosedTrieNode.MINIMUM_TEMP_BUFFER_LENGTH];
        // then write header, if requested
        if (writeHeader) {
            // currently it is constant; in future will need to add bitflags etc:
            System.out.println("Payload length: "+root.length());
            // first things first: tr13 header:
            int headerLen = TrieHeader.fillHeaderInfo(tmpBuffer, TrieHeader.ValueType.VINT, root.length());
            out.write(tmpBuffer, 0, headerLen);
        }
        // and then serialize the trie payload
        root.serializeTo(out, tmpBuffer);
        out.flush();
    }
    
    @Override
    public abstract TrieNode build() throws IOException;
}
