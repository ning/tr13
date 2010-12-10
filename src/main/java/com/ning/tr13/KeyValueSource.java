package com.ning.tr13;

import java.io.IOException;

/**
 * Basic interface used for gathering key/value data to build a Tr13 instance.
 * 
 * @author tatu
 *
 * @param <T> Type of values source provides
 */
public abstract class KeyValueSource<T>
{
    public interface ValueCallback<V>
    {
        public void handleEntry(byte[] key, V value);
    }

    public abstract void readAll(ValueCallback<T> handler) throws IOException;

    /**
     * Method that can be used to get information about current line number within
     * source, if one available; should return -1 if no such information available.
     */
    public abstract int getLineNumber();
}
