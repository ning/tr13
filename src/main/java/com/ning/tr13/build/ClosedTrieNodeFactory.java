package com.ning.tr13.build;

/**
 * Interface for node factories for specific value type.
 *
 * @param <T>
 */
public abstract class ClosedTrieNodeFactory<T>
{
    public abstract ClosedTrieNode<T> simpleLeaf(byte b, T value);
    public abstract ClosedTrieNode<T> simpleBranch(byte b, ClosedTrieNode<T>[] kids);
    public abstract ClosedTrieNode<T> valueBranch(byte b, ClosedTrieNode<T>[] kids, T value);
    public abstract ClosedTrieNode<T> serialized(ClosedTrieNode<T> node);
    public abstract ClosedTrieNode<T> suffixLeaf(byte b, ClosedTrieNode<T> node);
}
