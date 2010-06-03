package com.ning.tr13.impl.bytes;

import java.io.*;

import com.ning.tr13.KeyValueReader;
import com.ning.tr13.build.ClosedTrieNodeFactory;
import com.ning.tr13.build.OpenTrieNode;
import com.ning.tr13.build.SimpleTrieBuilder;

public class SimpleBytesTrieBuilder
    extends SimpleTrieBuilder<byte[]>
{
//    private final static VIntNodeFactory nodeFactory = new VIntNodeFactory();

    public SimpleBytesTrieBuilder(KeyValueReader<byte[]> r) {
        this(r, false);
    }
    
    public SimpleBytesTrieBuilder(KeyValueReader<byte[]> r, boolean diagnostics) {
        super(r, diagnostics);
    }

    @Override
    public ClosedTrieNodeFactory<byte[]> closedTrieNodeFactory() {
        // !!! TBI
        return null;
    }

    @Override
    protected OpenTrieNode<byte[]> constructOpenNode(byte b, byte[] value) {
        return new OpenTrieNode<byte[]>(b, value);
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
