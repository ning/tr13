package com.ning.tr13.tools;

import java.io.BufferedOutputStream;
import java.io.FileInputStream;
import java.io.IOException;

import com.ning.tr13.KeyValueReader;

public class VIntTrieDumper
    extends TrieDumper
{
    protected VIntTrieDumper(char valueSeparator) {
        super(valueSeparator);
    }

    public static void main(String[] args) throws IOException
    {
        if (args.length != 1) {
            System.err.println("USAGE: java ... [trie-file]");
            System.exit(1);
        }      
        FileInputStream in = new FileInputStream(args[0]);
        BufferedOutputStream out = new BufferedOutputStream(System.out);
        new VIntTrieDumper(KeyValueReader.DEFAULT_SEPARATOR_CHAR).dump(in, out); 
        in.close();
        out.flush();
        System.out.flush();
    }
}
