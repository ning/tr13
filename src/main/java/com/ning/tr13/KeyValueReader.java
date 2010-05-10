package com.ning.tr13;

import java.io.*;

/**
 * Class that defines abstraction used for reading trie entries
 * (from a file or other resource).
 * 
 * @author tatu
 */
public class KeyValueReader
{
    public final static char DEFAULT_SEPARATOR_CHAR = '|';
    
    protected final BufferedReader _reader;

    protected final char _separatorChar;
    
    protected int _lineNumber;
    
    protected long _value;
    
    public KeyValueReader(InputStream in) throws IOException
    {
        this(in, DEFAULT_SEPARATOR_CHAR);
    }
    
    public KeyValueReader(InputStream in, char sepChar) throws IOException
    {
        _reader = new BufferedReader(new InputStreamReader(in, "UTF-8"));
        _separatorChar = sepChar;
    }

    public void close() throws IOException {
        _reader.close();
    }
    
    public int getLineNumber() { return _lineNumber; }

    public long getValue() { return _value; }
    
    public String nextEntry() throws IOException
    {
        String line;
        
        while ((line = _reader.readLine()) != null) {
            ++_lineNumber;
            line = line.trim();
            if (line.length() == 0 || line.startsWith("#")) continue;
            int ix = line.indexOf(_separatorChar);
            if (ix > 0) {
                String id = line.substring(0, ix);
                try {
                    _value = Long.parseLong(line.substring(ix+1).trim());
                    return id;
                } catch (NumberFormatException e) {
                    ; // will error out below
                }
            }
            throw new IOException("Invalid line #"+_lineNumber+": '"+line+"'");
        }
        _value = 0L;
        return null;
    }
}
