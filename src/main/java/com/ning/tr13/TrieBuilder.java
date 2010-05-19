package com.ning.tr13;

import java.io.*;

import com.ning.tr13.build.ClosedNode;
import com.ning.tr13.build.OpenNode;

/**
 * Class used for building binary trie structure from input source and
 * writing it to an output target; input and output are typically
 * files.
 * 
 * @author tatu
 */
public class TrieBuilder
{
    /**
     * We will use simple 8-byte header. First 5 bytes are fixed (so first 4 bytes can be
     * used as 'magic cookie' for file type detection; 6th byte contains version number,
     * and 2 remaining bytes are reserved for use as bitfields for variations in file
     * format.
     */
    private final static byte[] HEADER_TEMPLATE = new byte[] {
        // 5 bytes chosen to be human readable for easy eyeballing
        'T', 'R', '1', '3', '\n',
        // then version number "1.0" in hex; plus 0x80 bit set to force file type as 'binary' (in unix)
        (byte) (0x80 + 0x10),
        // then two spare bytes for future expansion
        0x0, 0x0
    };
    
    protected final KeyValueReader _reader;

    /**
     * Flag to enable crude diagnostics to STDOUT
     */
    protected final boolean _diagnostics;

    protected int _linesRead;
    
    public TrieBuilder(KeyValueReader r, boolean diagnostics)
    {
        _reader = r;
        _diagnostics = diagnostics;
    }

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
    public void buildAndWrite(OutputStream out) throws IOException
    {
        /* note: temp buffer has to be just big enough to contain basic branch header,
         * or leaf node.
         */
        byte[] tmpBuffer = new byte[ClosedNode.MINIMUM_TEMP_BUFFER_LENGTH];
        // first things first: tr13 header:
        System.arraycopy(HEADER_TEMPLATE, 0, tmpBuffer, 0, 8);
        out.write(tmpBuffer, 0, 8);
        // currently it is constant; in future will need to add bitflags etc:
        build().serializeTo(out, tmpBuffer);
        out.flush();
    }
    
    /**
     * Main build method that will construct full InputCollator and return it as
     * a {@link ClosedNode} instance, which can be simply serialized
     * to an {@link OutputStream}
     * 
     * @return Root node
     * @throws IOException
     */
    public ClosedNode build() throws IOException
    {
        OpenNode root = new OpenNode((byte) 0, null);
        String idStr;

        int count = 0;

        while ((idStr = _reader.nextEntry()) != null) {
            long value = _reader.getValue();
            // !!! TODO: parse as bytes to speed up processing
            byte[] id = idStr.getBytes("UTF-8");
            
            OpenNode curr = root;
            int i = 0;
            // first, skip out common ancestry
            while (true) {
                OpenNode next = curr.getCurrentChild();
                if (next == null || next.getNodeByte() != id[i]) break;
                if (++i >= id.length) { // sanity check, could skip
                    // appears that we have some of this problem
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
        TrieBuilder b = new TrieBuilder(r, true);
        File outputFile = new File(args[1]);
        OutputStream out = new FileOutputStream(outputFile);
        b.buildAndWrite(out);
        out.close();
        System.out.println("Build complete: "+b._linesRead+" lines read, result file length is "+(outputFile.length()>>10)+" kB");
    }
}
