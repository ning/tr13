package com.ning.tr13.impl.bytes;

import java.io.*;
import java.util.concurrent.atomic.AtomicInteger;

import com.ning.tr13.KeyValueReader;
import com.ning.tr13.build.OpenTrieNode;
import com.ning.tr13.build.SimpleTrieBuilder;
import com.ning.tr13.build.TrieNode;
import com.ning.tr13.util.UTF8Codec;

public class SimpleBytesTrieBuilder
    extends SimpleTrieBuilder<byte[]>
{
    public SimpleBytesTrieBuilder(KeyValueReader<byte[]> r) {
        this(r, false);
    }
    
    public SimpleBytesTrieBuilder(KeyValueReader<byte[]> r, boolean diagnostics) {
        super(r, diagnostics);
    }

    @Override
    public TrieNode build() throws IOException
    {
        final OpenTrieNode root = new OpenTrieNode((byte) 0, null);
        final boolean diag = _diagnostics;
        final AtomicInteger count = new AtomicInteger(0);

        _reader.readAll(new KeyValueReader.ValueCallback<byte[]>() {
            @Override
            public void handleEntry(byte[] id, byte[] value) {
                OpenTrieNode curr = root;
                int i = 0;
                // first, skip out common ancestry
                while (true) {
                    OpenTrieNode next = curr.getCurrentChild();
                    if (next == null || next.getNodeByte() != id[i]) break;
                    if (++i >= id.length) { // sanity check, could skip, but better safe than sorry
                        throw new IllegalArgumentException("Malformed input, line "
                                +_reader.getLineNumber()+": id '"+UTF8Codec.fromUTF8(id)+"' not properly ordered");
                    }
                    curr = next;
                }
                // then attach to where we diverge
                for (int last = id.length-1; i <= last; ++i) {
                    OpenTrieNode next = null;
                    /*
                    OpenTrieNode next = new OpenTrieNode(id[i],
                            (i == last) ? value : null);
                            */
                    if (true) throw new Error();
                    curr.addNode(next, _reorderEntries);
                    curr = next;
                }
                int c = count.addAndGet(1);
                if (diag && (c & 0xFFFFF) == 0) {
                    System.out.println("Building: "+(count.get()>>10)+"k lines processed");
                }
            }
        });
        _linesRead = count.get();    
        return root.close(_reorderEntries);
    }

    public static void main(String[] args) throws Exception
    {
        if (args.length != 2) {
            System.err.println("USAGE: java ... [input-file] [output-file]");
            System.exit(2);
        }
        BytesValueReader r = new BytesValueReader(new FileInputStream(args[0]));
        SimpleBytesTrieBuilder b = new SimpleBytesTrieBuilder(r, true);
        File outputFile = new File(args[1]);
        OutputStream out = new FileOutputStream(outputFile);
        b.buildAndWrite(out, true);
        out.close();
        System.out.println("Build complete: "+b._linesRead+" lines read, result file length is "+(outputFile.length()>>10)+" kB");
    }
}
