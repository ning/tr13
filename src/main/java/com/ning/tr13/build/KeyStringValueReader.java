package com.ning.tr13.build;

import java.io.*;

import com.ning.tr13.KeyValueReader;

/**
 * Simple convenience implementation for String-valued tries, to help
 * building.
 */
public class KeyStringValueReader extends KeyValueReader<String>
{
    public KeyStringValueReader(File f) throws IOException {
        super(f);
    }
    
    public KeyStringValueReader(InputStream in) throws IOException {
        super(in);
    }
    
    public KeyStringValueReader(InputStream in, char sepChar) throws IOException {
        super(in, sepChar);
    }

    @Override
    public String toValue(String value) {
        return value;
    }

}
