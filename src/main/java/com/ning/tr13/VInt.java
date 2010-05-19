package com.ning.tr13;

/**
 * Helper class for dealing with Variable-length integers (VInts).
 * Encoding uses big-endian (most-significant byte first) ordering;
 * number of bits in the first byte can vary to allow using couple of
 * leading bits for type marking.
 */
public class VInt
{
    /**
     * 
     * @param value Value to check
     * @param bitsForFirstByte Number of bits in the first value byte that can be
     *  used for value itself: 8 for "pure" VInts, less for types that use leading
     *  bits for other purposes
     * @return
     */
    public static int lengthForUnsigned(long value, int bitsForFirstByte)
    {
        // note: 1 bit is 'stolen' from each bit, to use as continuation
        value >>>= (bitsForFirstByte - 1);
        int bytes = 1;
        while (value != 0L) {
            value >>= 7;
            ++bytes;
        }
        return bytes;
    }

    /**
     * Method for converting given unsigned value (meaning that no sign bit is assumed)
     * 
     * @return Offset after serializing VInt
     */
    public static int unsignedToBytes(long value, int bitsForFirstByte, byte[] result, int offset)
    {
        // N bits fit in the first byte; but MSB is the 'last byte' marker bit:
        --bitsForFirstByte;
        int mask = (1 << bitsForFirstByte) - 1;
        // Does value fit in that one byte?
        // so to reserve room for it
        if ((value & mask) == value) { // yup; single byte value:
            int i = (int) value;
            result[offset] = (byte) ((i & mask) | (1 << bitsForFirstByte));
            return offset+1;
        }
        // Ok: one more special case; if 4 bytes are enough, can do more efficient handling:
        if ((value >> (21 + bitsForFirstByte)) == 0L) {
            return _unsignedIntToBytes((int) value, result, offset, bitsForFirstByte, mask);
        }
        // If not, can quickly determine 4 LSBs        
        int i = (int) value;
        byte b0 = (byte) ((i & 0x7F) | 0x80);
        i >>= 7;
        byte b1 = (byte) (i & 0x7F);
        i >>= 7;
        byte b2 = (byte) (i & 0x7F);
        i >>= 7;
        byte b3 = (byte) (i & 0x7F);
        value >>>= 28;

        if (value <= mask) {
            return _appendBytes(result, offset, (byte) value, b3, b2, b1, b0);
        }
        byte b4 = (byte) (value & 0x7F);

        // Ok: 5 bytes (== 35 bits) done; can safely revert back to int now
        i = (int) (value >> 7);
        if (i <= mask) {
            result[offset++] = (byte) i;
            return _appendBytes(result, offset, b4, b3, b2, b1, b0);
        }
        byte b5 = (byte) (i & 0x7F);
        i >>= 7;
        if (i <= mask) { // 7 bytes
            result[offset++] = (byte) i;
            result[offset++] = b5;
            return _appendBytes(result, offset, b4, b3, b2, b1, b0);
        }
        byte b6 = (byte) (i & 0x7F);
        i >>= 7;
        if (i <= mask) { // 8 bytes
            result[offset++] = (byte) i;
            result[offset++] = b6;
            result[offset++] = b5;
            return _appendBytes(result, offset, b4, b3, b2, b1, b0);
        }
        byte b7 = (byte) (i & 0x7F);
        i >>= 7;
        if (i <= mask) { // 9 bytes
            result[offset++] = (byte) i;
            result[offset++] = b7;
            result[offset++] = b6;
            result[offset++] = b5;
            return _appendBytes(result, offset, b4, b3, b2, b1, b0);
        }
        // unlikely, but possible; up to 10 bytes; 9 with 7 bits, one with N <= 7 bits
        byte b8 = (byte) (i & 0x7F);
        i >>= 7;
        offset = _appendBytes(result, offset, (byte) i, b8, b7, b6, b5);
        return _appendBytes(result, offset, b4, b3, b2, b1, b0);
    }

    /**
     * Reverse of {@link #unsignedToBytes}
     * 
     * @param bitsForFirstByte Number of (least-significant) data bits in the first byte
     * @param buffer Buffer that contains bytes (caller has to ensure all data bytes are
     *   included)
     * @param offset Offset of the first data byte in buffer
     * 
     * @result Offset of the first byte following decoded long
     */
    public static int bytesToUnsigned(int bitsForFirstByte, byte[] buffer, int offset,
            long[] resultBuffer)
    {
        // Ok: first byte has N bits, of which MSB indicates if it's the last byte to use:
        int value = buffer[offset++] & ((1 << bitsForFirstByte) - 1);
        bitsForFirstByte--;
        int marker = (1 << bitsForFirstByte);
        if ((value & marker) != 0) { // if we have MSB set, it means 'last byte'
            value ^= marker;
            resultBuffer[0] = value;
            return offset;
        }
        value <<= 7;
        // Otherwise we'll just get N LSB
        int b = buffer[offset++];
        if (b < 0) { // 2 bytes
            resultBuffer[0] = value | (b & 0x7F);
            return offset;
        }
        value = (value | b) << 7;
        b = buffer[offset++];
        if (b < 0) { // 3 bytes
            resultBuffer[0] = value | (b & 0x7F);
            return offset;
        }
        value = (value | b) << 7;
        b = buffer[offset++];
        if (b < 0) { // 4 bytes
            resultBuffer[0] = value | (b & 0x7F);
            return offset;
        }
        value = (value | b);
        // At this point we must 'upgrade' to long; can just loop
        long l = (long) value;
        while (true) {
            l <<= 7;
            b = buffer[offset++];
            if (b < 0) { // last byte
                resultBuffer[0] = l | (b & 0x7F);
                return offset;
            }
            l |= b;
        }
    }

    /*
    /**********************************************************
    /* Helper methods
    /**********************************************************
     */

    /**
     * Helper method called when VInt fits within 4 bytes
     */
    private static int _unsignedIntToBytes(int value, byte[] result, int offset,
            int bitsForFirstByte, int maskForFirstByte)
    {
        // Ok: we can figure out 3 more bytes just from lower int:
        byte b0 = (byte) ((value & 0x7F) | 0x80);
        value >>= 7;
        if (value <= maskForFirstByte) {
            result[offset++] = (byte) value;
            result[offset++] = b0;
            return offset;
        }
        byte b1 = (byte) (value & 0x7F);
        value >>= 7;
        if (value <= maskForFirstByte) {
            result[offset++] = (byte) value;
            result[offset++] = b1;
            result[offset++] = b0;
            return offset;
        }
        result[offset++] = (byte) (value >> 7);
        result[offset++] = (byte) (value & 0x7F);
        result[offset++] = b1;
        result[offset++] = b0;
        return offset;
    }

    private static int _appendBytes(byte[] buffer, int ptr,
            byte b0, byte b1, byte b2, byte b3, byte b4)
    {
        buffer[ptr++] = b0;
        buffer[ptr++] = b1;
        buffer[ptr++] = b2;
        buffer[ptr++] = b3;
        buffer[ptr++] = b4;
        return ptr;
    }
    
    
}
