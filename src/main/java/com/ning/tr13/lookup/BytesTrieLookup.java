package com.ning.tr13.lookup;

import com.ning.tr13.*;

/**
 * Type-specific extension of {@link TrieLookup} that allows both extended and more
 * efficient access in cases where type is known a priori.
 * 
 * @author tatu
 */
public abstract class BytesTrieLookup
    extends TrieLookup<byte[]>
{
    protected BytesTrieLookup() { }

    @Override
    public abstract byte[] findValue(byte[] key);
}
