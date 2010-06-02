package com.ning.tr13.lookup;

import java.io.IOException;

/**
 * Helper class for reading header section of the trie input
 */
public class TrieHeader
{
    public final static int HEADER_LENGTH = 16;

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
    private final static byte[] HEADER_TEMPLATE = new byte[] {
        // 5 bytes chosen to be human readable for easy eyeballing
        'T', 'R', '1', '3', '\n',
        // then version number "1.0" in hex; plus 0x80 bit set to force file type as 'binary' (in unix)
        (byte) (0x80 + 0x10),
        // then two spare bytes for future expansion
        0x0, 0x0
    };  

    protected final long _payloadLength;
    
    protected TrieHeader(long len)
    {
        _payloadLength = len;
    }
    
    public static TrieHeader read(byte[] buffer, int offset) throws IOException
    {
        for (int i = 0 ; i < 8; ++i) {
            if (buffer[offset+i] != HEADER_TEMPLATE[i]) {
                throw new IOException("Malformed input: no valid trie header found (first 8 bytes wrong)");
            }
        }
        long len = buffer[8];
        for (int i = 9; i < 16; ++i) {
            len = (len << 8) | (buffer[i] & 0xFF);
        }
        return new TrieHeader(len);
    }
    
    public static int fillHeaderInfo(byte[] buffer, long len)
    {
        System.arraycopy(TrieHeader.HEADER_TEMPLATE, 0, buffer, 0, 8);
        for (int i = 15; i >= 8; --i) {
            buffer[i] = (byte) len;
            len >>= 8;
        }
        return 16;
    }

    public long getPayloadLength() { return _payloadLength; }
}
