package com.ning.tr13.impl.vint;

import java.io.*;

import com.ning.tr13.KeyValueSource;
import com.ning.tr13.build.ClosedTrieNodeFactory;
import com.ning.tr13.build.OpenTrieNode;
import com.ning.tr13.build.SimpleTrieBuilder;

public class SimpleVIntTrieBuilder
    extends SimpleTrieBuilder<Long>
{
    private final static VIntNodeFactory nodeFactory = new VIntNodeFactory();
    
    public SimpleVIntTrieBuilder(KeyValueSource<Long> r) {
        this(r, false);
    }
    
    public SimpleVIntTrieBuilder(KeyValueSource<Long> r, boolean diagnostics) {
        super(r, diagnostics);
    }

    @Override
    public ClosedTrieNodeFactory<Long> closedTrieNodeFactory() {
        return nodeFactory;
    }
    
    @Override
    protected OpenTrieNode<Long> constructOpenNode(byte b, Long value) {
        return new OpenTrieNode<Long>(b, value);
    }

    public static void main(String[] args) throws Exception
    {
        if (args.length != 2) {
            System.err.println("USAGE: java ... [input-file] [output-file]");
            System.exit(2);
        }
        VIntValueReader r = new VIntValueReader(new FileInputStream(args[0]));
        SimpleVIntTrieBuilder b = new SimpleVIntTrieBuilder(r, true);
        File outputFile = new File(args[1]);
        OutputStream out = new FileOutputStream(outputFile);
        b.buildAndWrite(out, true);
        r.close();
        out.close();
        System.out.println("Build complete: "+b._linesRead+" lines read, result file length is "+(outputFile.length()>>10)+" kB");
    }
}
