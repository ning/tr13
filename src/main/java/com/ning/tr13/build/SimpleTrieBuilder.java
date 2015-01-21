package com.ning.tr13.build;

import java.io.*;
import java.util.concurrent.atomic.AtomicInteger;

import com.ning.tr13.KeyValueReader;
import com.ning.tr13.KeyValueSource;
import com.ning.tr13.TrieBuilder;
import com.ning.tr13.lookup.TrieHeader;
import com.ning.tr13.util.UTF8Codec;

/**
 * Straight-forward builder implementation that reads data using
 * given {@link KeyValueReader}, and writes results into an
 * output stream (typical File).
 */
public abstract class SimpleTrieBuilder<T>
    extends TrieBuilder<T>
{
    protected final KeyValueSource<T> _source;

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
    
    public SimpleTrieBuilder(KeyValueSource<T> r) {
        this(r, false);
    }
    
    public SimpleTrieBuilder(KeyValueSource<T> r, boolean diagnostics) {
        _source = r;
        _diagnostics = diagnostics;
    }

    public SimpleTrieBuilder<T> setReorderEntries(boolean b) {
        _reorderEntries = b;
        return this;
    }

    protected abstract ClosedTrieNodeFactory<T> closedTrieNodeFactory();
    protected abstract OpenTrieNode<T> constructOpenNode(byte b, T value);
    
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
        TrieNode<T> root = build();
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
    public TrieNode<T> build() throws IOException
    {
        final OpenTrieNode<T> root = constructOpenNode((byte) 0, null);
        final boolean diag = _diagnostics;
        final AtomicInteger count = new AtomicInteger(0);
        final ClosedTrieNodeFactory<T> nodeFactory = closedTrieNodeFactory();

        _source.readAll(new KeyValueReader.ValueCallback<T>() {
            @Override
            public void handleEntry(byte[] id, T value) {
                OpenTrieNode<T> curr = root;
                int i = 0;
                // first, skip out common ancestry
                while (true) {
                    OpenTrieNode<T> next = curr.getCurrentChild();
                    if (next == null || next.getNodeByte() != id[i]) break;
                    if (++i >= id.length) { // sanity check, could skip, but better safe than sorry
                        throw new IllegalArgumentException("Malformed input, line "
                                +_source.getLineNumber()+": id '"+UTF8Codec.decodeFromUTF8(id)+"' not properly ordered");
                    }
                    curr = next;
                }
                // then attach to where we diverge
                for (int last = id.length-1; i <= last; ++i) {
                    OpenTrieNode<T> next = constructOpenNode(id[i], (i == last) ? value : null);
                    curr.addNode(nodeFactory, next, _reorderEntries);
                    curr = next;
                }
                int c = count.addAndGet(1);
                if (diag && (c & 0xFFFFF) == 0) {
                    System.out.println("Building: "+(count.get()>>10)+"k lines processed");
                }
            }
        });
        _linesRead = count.get();    
        return root.close(nodeFactory, _reorderEntries);
    }
}
