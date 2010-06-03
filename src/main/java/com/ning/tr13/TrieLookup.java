package com.ning.tr13;

import java.util.NoSuchElementException;

/**
 * Class used for reading persistent trie structure, and accessing values it
 * has. Since there are multiple backend implementations, this is an abstract
 * class with API and factory methods for creating specialized instances.
 * 
 * @author tatu
 */
public abstract class TrieLookup
{
    protected TrieLookup() { }
    
    /*
    /********************************************************** 
    /* Public API
    /********************************************************** 
     */

    public abstract long getValue(byte[] key) throws NoSuchElementException;

    public abstract long getValue(byte[] key, long defaultValue);

    public abstract Long findValue(byte[] key);

    /*
    /********************************************************** 
    /* Internal methods
    /********************************************************** 
     */

    protected static String _printKey(byte[] buffer, int offset, int len)
    {
        StringBuilder sb = new StringBuilder(20);
        sb.append("0x");
        // Print first and last 4 bytes
        if (len <= 8) {
            
        } else {
            for (int i = 0; i < 4; ++i) {
            }
        }
        return sb.toString();
    }
}
