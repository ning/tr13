package com.ning.tr13.impl.bytes;

import java.io.*;

import com.ning.tr13.KeyValueReader;
import com.ning.tr13.util.UTF8Codec;

public class BytesValueReader
    extends KeyValueReader<byte[]>
{
    public BytesValueReader(File f) throws IOException
    {
        super(f);
    }
    
    public BytesValueReader(InputStream in) throws IOException
    {
        super(in);
    }

    public BytesValueReader(InputStream in, char sepChar) throws IOException
    {
        super(in, sepChar);
    }
    
    @Override
    protected void parseAndHandle(KeyValueReader.ValueCallback<byte[]> handler, byte[] key, String value)
        throws IOException
    {
        byte[] bytes = UTF8Codec.toUTF8(value);
        handler.handleEntry(key, bytes);
    }
}
