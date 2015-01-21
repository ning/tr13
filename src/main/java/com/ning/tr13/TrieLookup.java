package com.ning.tr13;

/**
 * API for accessing entries of a trie.
 *
 * @param <V> Value type of the underlying Trie
 */
public abstract class TrieLookup<V>
{
    /**
     * Class used for reading persistent trie structure, and accessing values it
     * has. Since there are multiple backend implementations, this is an abstract
     * class with API and factory methods for creating specialized instances.
     */
    protected TrieLookup() { }
    
    /*
    /********************************************************** 
    /* Public API
    /********************************************************** 
     */

    /**
     * Main lookup method usable with all value types
     */
    public abstract V findValue(byte[] key);

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
