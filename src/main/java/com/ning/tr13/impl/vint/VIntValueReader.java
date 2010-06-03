package com.ning.tr13.impl.vint;

import java.io.*;

import com.ning.tr13.KeyValueReader;

public class VIntValueReader
    extends KeyValueReader<Long>
{
    public VIntValueReader(File f) throws IOException
    {
        super(f);
    }
    
    public VIntValueReader(InputStream in) throws IOException
    {
        super(in);
    }

    public VIntValueReader(InputStream in, char sepChar) throws IOException
    {
        super(in, sepChar);
    }
    
    @Override
    protected void parseAndHandle(KeyValueReader.ValueCallback<Long> handler, byte[] key, String value)
        throws IOException
    {
        try {
            Long l = new Long(value);
            handler.handleEntry(key, l);
        } catch (NumberFormatException e) {
            throw new IOException("Invalid line #"+getLineNumber()+", unrecognized number '"+value+"'");
        }
    }
}
