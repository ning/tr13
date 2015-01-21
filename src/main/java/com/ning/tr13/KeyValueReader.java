package com.ning.tr13;

import java.io.*;

import com.ning.tr13.util.UTF8Codec;

/**
 * Class that defines abstraction used for reading entries from
 * a stream source (like file), to be used for building a trie
 * structure. Entry input is assumed to be encoded in UTF-8,
 * and use a simple character separator
 * so that beginning of the line (before separator) is the key, and
 * remained of the line (after separator) value.
 *<p>
 * About the only additional feature is that empty lines are skipped;
 * and lines that start with '#' are taken to be comments.
 *<p>
 * Currently customization can be done by sub-classing; should
 * refactor to allow bit cleaner extensibility.
 * 
 * @param <T> Type of values source provides
 * 
 * @author tatu
 */
public abstract class KeyValueReader<T> extends KeyValueSource<T>
{
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
        _reader = new BufferedReader(new InputStreamReader(in, UTF8Codec.UTF8));
        _separatorChar = sepChar;
    }

    public void setCloseWhenDone(boolean b) {
        _closeWhenDone = b;
    }
    
    public void close() throws IOException {
        _reader.close();
    }
    
    public void readAll(ValueCallback<T> handler) throws IOException
    {
        String line;
        UTF8Codec codec = new UTF8Codec();
        
        while ((line = _reader.readLine()) != null) {
            ++_lineNumber;
            line = line.trim();
            if (line.length() == 0 || line.startsWith("#")) continue;
            int ix = line.indexOf(_separatorChar);
            if (ix > 0) {
                // !!! TODO: optimize
                String id = line.substring(0, ix);
                byte[] key = codec.encodeNonReentrant(id);
                T value = toValue(line.substring(ix+1).trim());
                handler.handleEntry(key, value);
            }
        }
        if (_closeWhenDone) {
            close();
        }
    }

    /**
     * Helper method subclasses implement to convert from String to value
     * type.
     */
    public abstract T toValue(String value) throws IOException;
    
    public int getLineNumber() { return _lineNumber; }
}
