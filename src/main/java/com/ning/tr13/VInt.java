package com.ning.tr13;

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
        // note: 1 bit is 'stolen' from each bit, to use as continuation
        bitsForFirstByte--;        
        int mask = 0xFF >> (8 - bitsForFirstByte);
        result[offset++] = (byte) (((int) value) & mask);
        value  = (value >>> bitsForFirstByte);
        while (value != 0L) {
            result[offset++] = (byte) value;
            value >>= 7;
        }
        return offset;
    }
}
