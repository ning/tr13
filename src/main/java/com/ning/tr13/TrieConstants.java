package com.ning.tr13;

public class TrieConstants
{

    // // // Type  bits
    
    public final static int TYPE_LEAF_SIMPLE = 0;
    public final static int TYPE_LEAF_WITH_SUFFIX = 1;
    public final static int TYPE_BRANCH_SIMPLE = 2;    
    public final static int TYPE_BRANCH_WITH_VALUE = 3;

    // // // And settings for 'bit stealing' for type indicator
    
    public final static int FIRST_BYTE_BITS_FOR_BRANCHES = 6;

    public final static int FIRST_BYTE_BITS_FOR_LEAVES = 6;

    
}
