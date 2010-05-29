package com.ning.tr13;

import java.io.*;

/**
 * Class that defines abstraction used for reading trie entries
 * (from a file or other resource).
 *<p>
 * Currently customization can be done by sub-classing; should
 * refactor to allow bit cleaner extensibility.
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

    protected boolean _closeWhenDone = false;
    
    public KeyValueReader(InputStream in) throws IOException
    {
        this(in, DEFAULT_SEPARATOR_CHAR);
    }
    
    public KeyValueReader(InputStream in, char sepChar) throws IOException
    {
        _reader = new BufferedReader(new InputStreamReader(in, "UTF-8"));
        _separatorChar = sepChar;
    }

    public void setCloseWhenDone(boolean b) {
        _closeWhenDone = b;
    }
    
    public void close() throws IOException {
        _reader.close();
    }
    
    public int getLineNumber() { return _lineNumber; }

    public long getValue() { return _value; }

    /**
     * @return Key of next entry, if found; null if none
     * @throws IOException
     */
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
        if (_closeWhenDone) {
            close();
        }
        return null;
    }
}
