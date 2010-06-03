package com.ning.tr13.lookup;

import java.io.IOException;

/**
 * Helper class for reading header section of the trie input
 */
public class TrieHeader
{
    public final static int HEADER_LENGTH = 16;

    public final static int TYPE_OFFSET = 6;
    
    public enum ValueType {
        /**
         * Values are Variable-length INTegers
         */
        VINT(1),
        
        /**
         * Values are simple byte arrays
         */
        BYTE_ARRAY(2)
        ;

        private int _type;
        
        private ValueType(int type) {
            _type = type;
        }

        public static ValueType valueOf(int raw) {
            for (ValueType t : values()) {
                if (t.rawType() == raw) {
                    return t;
                }
            }
            return null;
        }
        
        public int rawType() { return _type; }
    }
    
    /**
     * We will use simple 16-byte header. First 8 bytes contain signature, version
     * and config bits, and second 8 bytes are 64-bit length of the payload that
     * follows.
     *<p>
     * First 5 bytes are fixed (so first 4 bytes can be
     * used as 'magic cookie' for file type detection; 6th byte contains version number (in
     * lower nibble; upper nibble reserved for now)
     * and last remaining bytes is reserved for use as bitfield for variations in file
     * format.
     */
    private final static byte[] HEADER_TEMPLATE = new byte[] {
        // 5 bytes chosen to be human readable for easy eyeballing
        'T', 'R', '1', '3', '\n',
        // then version number "1.0" in hex; plus 0x80 bit set to force file type as 'binary' (in unix)
        (byte) (0x80 + 0x10),
        // Then content type; 0x01 for VInts, 0x02 for byte[]
        (byte) 0,
        // then one spare byte for future expansion
        0x0
    };  

    protected final ValueType _type;
    
    protected final long _payloadLength;
    
    protected TrieHeader(ValueType type, long len)
    {
        _type = type;
        _payloadLength = len;
    }
    
    public static TrieHeader read(byte[] buffer, int offset) throws IOException
    {
        for (int i = 0 ; i < TYPE_OFFSET; ++i) {
            if (buffer[offset+i] != HEADER_TEMPLATE[i]) {
                throw new IOException("Malformed input: no valid trie header found (first 6 bytes wrong)");
            }
        }
        int rawType = buffer[offset+TYPE_OFFSET] & 0x0F;
        ValueType type = ValueType.valueOf(rawType);
        if (type == null) {
            throw new IOException("Malformed input: unrecognized type: "+rawType);
        }
        long len = buffer[8];
        for (int i = 9; i < 16; ++i) {
            len = (len << 8) | (buffer[i] & 0xFF);
        }
        return new TrieHeader(type, len);
    }
    
    public static int fillHeaderInfo(byte[] buffer, ValueType type, long len)
    {
        System.arraycopy(TrieHeader.HEADER_TEMPLATE, 0, buffer, 0, 8);
        for (int i = 15; i >= 8; --i) {
            buffer[i] = (byte) len;
            len >>= 8;
        }
        return 16;
    }

    public ValueType getValueType() { return _type; }
    public long getPayloadLength() { return _payloadLength; }
}
