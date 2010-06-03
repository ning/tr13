package com.ning.tr13.build;

/**
 * Interface for value encoders used during building compact trie
 * representation, to convert from value to matching byte sequence.
 *
 * @param <T> Value type to be serialized
 */
public abstract class ValueEncoder<T>
{
    public abstract int byteLength(T value, int firstByteBits);
    
    public abstract int serialize(T value, int firstByteBits, byte[] result, int offset);
}
