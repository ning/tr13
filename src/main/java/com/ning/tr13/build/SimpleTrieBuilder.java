package com.ning.tr13.build;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStream;

import com.ning.tr13.KeyValueReader;
import com.ning.tr13.TrieBuilder;
import com.ning.tr13.TrieNode;
import com.ning.tr13.read.TrieHeader;
import com.ning.tr13.util.UTF8Codec;

/**
 * Straight-forward builder implementation that reads data using
 * given {@link KeyValueReader}, and writes results into an
 * output stream (typical File).
 */
public class SimpleTrieBuilder
    extends TrieBuilder
{
    protected final KeyValueReader _reader;

    /**
     * Flag to enable crude diagnostics to STDOUT
     */
    protected final boolean _diagnostics;

    protected int _linesRead;

    public SimpleTrieBuilder(KeyValueReader r) {
        this(r, false);
    }
    
    public SimpleTrieBuilder(KeyValueReader r, boolean diagnostics) {
        _reader = r;
        _diagnostics = diagnostics;
    }

    @Override
    public void buildAndWrite(OutputStream out) throws IOException
    {
        /* note: temp buffer has to be just big enough to contain basic branch header,
         * or leaf node.
         */
        byte[] tmpBuffer = new byte[ClosedNode.MINIMUM_TEMP_BUFFER_LENGTH];
        // currently it is constant; in future will need to add bitflags etc:
        TrieNode root = build();
        System.out.println("Payload length: "+root.length());
        // first things first: tr13 header:
        int headerLen = TrieHeader.fillHeaderInfo(tmpBuffer, root.length());
        out.write(tmpBuffer, 0, headerLen);
        // and then serialize the trie payload
        root.serializeTo(out, tmpBuffer);
        System.out.println("Completed.");
        
        out.flush();
    }
    
    @Override
    public TrieNode build() throws IOException
    {
        OpenNode root = new OpenNode((byte) 0, null);
        String idStr;

        int count = 0;

        while ((idStr = _reader.nextEntry()) != null) {
            long value = _reader.getValue();
            // !!! TODO: parse as bytes to speed up processing
            byte[] id = UTF8Codec.toUTF8(idStr);
            
            OpenNode curr = root;
            int i = 0;
            // first, skip out common ancestry
            while (true) {
                OpenNode next = curr.getCurrentChild();
                if (next == null || next.getNodeByte() != id[i]) break;
                if (++i >= id.length) { // sanity check, could skip, but better safe than sorry
                    throw new IllegalArgumentException("Malformed input, line "
                            +_reader.getLineNumber()+": id '"+idStr+"' not properly ordered");
                }
                curr = next;
            }
            // then attach to where we diverge
            for (int last = id.length-1; i <= last; ++i) {
                OpenNode next = new OpenNode(id[i],
                        (i == last) ? Long.valueOf(value) : null);
                curr.addNode(next);
                curr = next;
            }

            if ((++count & 0xFFFFF) == 0) {
                if (_diagnostics) {
                    System.out.println("Building: "+(count>>10)+"k lines processed");
                }
            }
        }
        _linesRead = count;        
        return root.close();
    }

    public static void main(String[] args) throws Exception
    {
        if (args.length != 2) {
            System.err.println("USAGE: java ... [input-file] [output-file]");
            System.exit(2);
        }
        KeyValueReader r = new KeyValueReader(new FileInputStream(args[0]));
        SimpleTrieBuilder b = new SimpleTrieBuilder(r, true);
        File outputFile = new File(args[1]);
        OutputStream out = new FileOutputStream(outputFile);
        b.buildAndWrite(out);
        out.close();
        System.out.println("Build complete: "+b._linesRead+" lines read, result file length is "+(outputFile.length()>>10)+" kB");
    }
}
