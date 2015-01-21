package com.ning.tr13.impl.bytes;

import java.io.*;

import com.ning.tr13.KeyValueSource;
import com.ning.tr13.build.ClosedTrieNodeFactory;
import com.ning.tr13.build.OpenTrieNode;
import com.ning.tr13.build.SimpleTrieBuilder;

public class SimpleBytesTrieBuilder
    extends SimpleTrieBuilder<byte[]>
{
    private final static BytesNodeFactory nodeFactory = new BytesNodeFactory();

    public SimpleBytesTrieBuilder(KeyValueSource<byte[]> r) {
        this(r, false);
    }
    
    public SimpleBytesTrieBuilder(KeyValueSource<byte[]> r, boolean diagnostics) {
        super(r, diagnostics);
    }

    @Override
    public ClosedTrieNodeFactory<byte[]> closedTrieNodeFactory() {
        return nodeFactory;
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
        System.out.printf("Build complete: %d lines read, result file length is %s",
                b._linesRead, desc(outputFile.length()));
    }

    private static String desc(long count) {
        if (count < 2000) return String.valueOf(count);
        if (count < 2000000) {
          return String.format("%.1fkB", count / 1000.0);
        }
        return String.format("%.1fMB", count / 1000000.0);
    }
}
