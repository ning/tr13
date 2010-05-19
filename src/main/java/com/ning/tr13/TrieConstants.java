package com.ning.tr13;

public class TrieConstants
{
    /**
     * We will use simple 16-byte header. First 8 bytes contain signature, version
     * and config bits, and second 8 bytes are 64-bit length of the payload that
     * follows.
     *<p>
     * First 5 bytes are fixed (so first 4 bytes can be
     * used as 'magic cookie' for file type detection; 6th byte contains version number,
     * and 2 remaining bytes are reserved for use as bitfields for variations in file
     * format.
     */
    protected final static byte[] HEADER_TEMPLATE = new byte[] {
        // 5 bytes chosen to be human readable for easy eyeballing
        'T', 'R', '1', '3', '\n',
        // then version number "1.0" in hex; plus 0x80 bit set to force file type as 'binary' (in unix)
        (byte) (0x80 + 0x10),
        // then two spare bytes for future expansion
        0x0, 0x0
    };  

    // // // Type  bits
    
    public final static int TYPE_LEAF_SIMPLE = 0;
    public final static int TYPE_LEAF_WITH_SUFFIX = 1;
    public final static int TYPE_BRANCH_SIMPLE = 2;    
    public final static int TYPE_BRANCH_WITH_VALUE = 3;

    // // // And settings for 'bit stealing' for type indicator
    
    public final static int FIRST_BYTE_BITS_FOR_BRANCHES = 6;

    public final static int FIRST_BYTE_BITS_FOR_LEAVES = 6;

    
}
