package com.ning.tr13;

import java.io.*;

import com.ning.tr13.util.UTF8Codec;

/**
 * Class that defines abstraction used for reading trie TEST_ENTRIES
 * (from a file or other resource).
 *<p>
 * Currently customization can be done by sub-classing; should
 * refactor to allow bit cleaner extensibility.
 * 
 * @author tatu
 */
public abstract class KeyValueReader<T>
{
    public interface ValueCallback<V>
    {
        public void handleEntry(byte[] key, V value);
    }
    
    public final static char DEFAULT_SEPARATOR_CHAR = '|';
    
    protected final BufferedReader _reader;

    protected final char _separatorChar;
    
    protected int _lineNumber;

    protected boolean _closeWhenDone = false;

    public KeyValueReader(File f) throws IOException
    {
        this(new FileInputStream(f));
        _closeWhenDone = true;
    }
    
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

    public void readAll(ValueCallback<T> handler) throws IOException
    {
        String line;
        
        while ((line = _reader.readLine()) != null) {
            ++_lineNumber;
            line = line.trim();
            if (line.length() == 0 || line.startsWith("#")) continue;
            int ix = line.indexOf(_separatorChar);
            if (ix > 0) {
                // !!! TODO: optimize
                String id = line.substring(0, ix);
                byte[] key = UTF8Codec.toUTF8(id);
                parseAndHandle(handler, key, line.substring(ix+1).trim());
            }
        }
        if (_closeWhenDone) {
            close();
        }
    }

    protected abstract void parseAndHandle(ValueCallback<T> handler, byte[] key, String value)
        throws IOException;
}
