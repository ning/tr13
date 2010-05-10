package com.ning.tr13;

public class VInt
{
    /**
     * 
     * @param value
     * @param bitsForFirstByte Number of bits in the first value byte that can be
     *  used for value itself: 8 for "pure" VInts, less for types that use leading
     *  bits for other purposes
     * @return
     */
    public static int lengthForUnsigned(long value, int bitsForFirstByte)
    {
        value >>>= bitsForFirstByte;
        if (value == 0L) {
            return 1;
        }
        int bytes = 1;
        do {
            value >>= 8;
            ++bytes;
        } while (value != 0);
        return bytes;
    }
}
