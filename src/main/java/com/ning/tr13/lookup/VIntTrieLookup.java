package com.ning.tr13.lookup;

import java.util.NoSuchElementException;

import com.ning.tr13.*;

/**
 * Type-specific extension of {@link TrieLookup} that allows both extended and more
 * efficient access in cases where type is known by caller.
 * 
 * @author tatu
 */
public abstract class VIntTrieLookup
    extends TrieLookup<Long>
{
    protected VIntTrieLookup() { }

    @Override
    public abstract Long findValue(byte[] key);

    /*
    /********************************************************** 
    /* Type-specific extension for more optimal access
    /********************************************************** 
     */
    
    /**
     * Accessor that will try to find entry with given key and return
     * value associated with it; but if none found, throws
     * {@link NoSuchElementException}.
     */
    public abstract long getValue(byte[] key) throws NoSuchElementException;

    /**
     * Accessor that will try to find entry with given key; but if one is not
     * found, returns specified default value
     */
    public abstract long getValue(byte[] key, long defaultValue);
}
