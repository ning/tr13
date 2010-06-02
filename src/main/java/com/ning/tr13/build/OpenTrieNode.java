package com.ning.tr13.build;

import java.util.*;


/**
 * Class that represents currently open node in tree: open meaning that
 * new child TEST_ENTRIES can be appended.
 */
public class OpenTrieNode
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
    protected long _nodeValue;

    /**
     * Byte that leads to this node from parent branch.
     */
    protected byte _nodeByte;

    protected boolean _hasValue;
    
    /**
     * Child nodes that have been completed so far, if any
     */
    protected ArrayList<ClosedTrieNode> _closedChildren;

    /**
     * Currently open child node, if any.
     */
    protected OpenTrieNode _currentChild;
    
    public OpenTrieNode(byte b, Long value)
    {
        _nodeByte = b;
        if (value == null) {
            _nodeValue = 0L;
            _hasValue = false;
        } else {
            _nodeValue = value.longValue();
            _hasValue = true;
        }
    }

    public byte getNodeByte() { return _nodeByte; }
    public OpenTrieNode getCurrentChild() { return _currentChild; }
    
    /**
     * Main mutation method used to close currently open child node
     * (if any), and optional start a new open child node.
     */
    public void addNode(OpenTrieNode n, boolean canReorder)
    {
        if (_currentChild != null) {
            if (_closedChildren == null) {
                _closedChildren = new ArrayList<ClosedTrieNode>(2);
            }
            _closedChildren.add(_currentChild.close(canReorder));
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
    public ClosedTrieNode close(boolean canReorder)
    {
        // first: is this a leaf?
        if (_currentChild == null) { // yes
            return ClosedTrieNode.simpleLeaf(_nodeByte, _nodeValue);
        }
        // or only has a leaf as child?
        ClosedTrieNode lastKid = _currentChild.close(canReorder);
        ClosedTrieNode[] closedKids;
        if (_closedChildren == null) {
            if (lastKid.isLeaf() && !_hasValue) {
                // single child which is leaf -> suffix leaf
                return ClosedTrieNode.suffixLeaf(_nodeByte, lastKid);
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
        ClosedTrieNode branch;
        if (_hasValue) {
            branch = ClosedTrieNode.valueBranch(_nodeByte, closedKids, _nodeValue);
        } else {
            branch = ClosedTrieNode.simpleBranch(_nodeByte, closedKids);

        }
        // one more thing: big enough to need serialization? (leaves we need not bother with)
        if (branch.length() < MAX_SERIALIZED) {
            branch = ClosedTrieNode.serialized(branch);
        }
        return branch;
    }

    /**
     * Helper method that will try to reorder kids so that the biggest child
     * entries are ordered before smaller ones: the idea is that this should
     * reduce lookups for most likely target nodes (assuming uniform access
     * pattern)
     */
    private void optimizeChildOrder(ClosedTrieNode[] kids)
    {
        // natural ordering works for ClosedTrieNode
        Arrays.sort(kids);
    }
}
