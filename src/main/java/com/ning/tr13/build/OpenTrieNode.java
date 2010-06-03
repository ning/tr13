package com.ning.tr13.build;

import java.util.*;

/**
 * Class that represents currently open node in tree: open meaning that
 * new child TEST_ENTRIES can be appended.
 * 
 * @param <T> Value type of nodes
 */
public class OpenTrieNode<T>
{
    /**
     * For memory usage limitation, we will pre-serialize branches that are
     * not "too big"; basically we get most compaction by serializing
     * medium-sized branches eagerly
     */
    private final static long MAX_SERIALIZED = 64000L;
    
    /**
     * Value node has, if any
     */
    protected T _nodeValue;

    /**
     * Byte that leads to this node from parent branch.
     */
    protected byte _nodeByte;
    
    /**
     * Child nodes that have been completed so far, if any
     */
    protected ArrayList<ClosedTrieNode<T>> _closedChildren;

    /**
     * Currently open child node, if any.
     */
    protected OpenTrieNode<T> _currentChild;
    
    public OpenTrieNode(byte b, T value)
    {
        _nodeByte = b;
        _nodeValue = value;
    }

    public byte getNodeByte() { return _nodeByte; }
    public OpenTrieNode<T> getCurrentChild() { return _currentChild; }
    
    /**
     * Main mutation method used to close currently open child node
     * (if any), and optional start a new open child node.
     */
    public void addNode(ClosedTrieNodeFactory<T> nodeFactory, OpenTrieNode<T> n, boolean canReorder)
    {
        if (_currentChild != null) {
            if (_closedChildren == null) {
                _closedChildren = new ArrayList<ClosedTrieNode<T>>(2);
            }
            _closedChildren.add(_currentChild.close(nodeFactory, canReorder));
        }
        _currentChild = n;
    }

    /**
     * Method called once branch or leaf that this node represents is
     * complete and no child nodes will be added.
     * 
     * @return Closed node that represents this node once it is not open
     *   to changes
     */
    @SuppressWarnings("unchecked")
    public ClosedTrieNode<T> close(ClosedTrieNodeFactory<T> nodeFactory, boolean canReorder)
    {
        // first: is this a leaf?
        if (_currentChild == null) { // yes
            return nodeFactory.simpleLeaf(_nodeByte, _nodeValue);
        }
        // or only has a leaf as child?
        ClosedTrieNode<T> lastKid = _currentChild.close(nodeFactory, canReorder);
        ClosedTrieNode<T>[] closedKids;
        if (_closedChildren == null) {
            if (lastKid.isLeaf() && (_nodeValue == null)) {
                // single child which is leaf -> suffix leaf
                return nodeFactory.suffixLeaf(_nodeByte, lastKid);
            }
            closedKids = new ClosedTrieNode[] { lastKid };
        } else {
            closedKids = new ClosedTrieNode[_closedChildren.size()+1];
            _closedChildren.toArray(closedKids);
            closedKids[_closedChildren.size()] = lastKid;
            if (canReorder) {
                optimizeChildOrder(closedKids);
            }
        }
        // ok, branch. Value?
        ClosedTrieNode<T> branch;
        if (_nodeValue != null) {
            branch = nodeFactory.valueBranch(_nodeByte, closedKids, _nodeValue);
        } else {
            branch = nodeFactory.simpleBranch(_nodeByte, closedKids);

        }
        // one more thing: big enough to need serialization? (leaves we need not bother with)
        if (branch.length() < MAX_SERIALIZED) {
            branch = nodeFactory.serialized(branch);
        }
        return branch;
    }

    /**
     * Helper method that will try to reorder kids so that the biggest child
     * entries are ordered before smaller ones: the idea is that this should
     * reduce lookups for most likely target nodes (assuming uniform access
     * pattern)
     */
    private void optimizeChildOrder(ClosedTrieNode<T>[] kids)
    {
        // natural ordering works for ClosedTrieNode
        Arrays.sort(kids);
    }
}
