package com.ning.tr13;

import com.ning.tr13.util.VInt;

public class VIntTest
    extends junit.framework.TestCase
{
    public void testLengthCalculation() throws Exception
    {
        // note: 8 means 8 available data bits; but one bit is needed for indicating where it's last byte
        assertEquals(1, VInt.lengthForUnsigned(0x1, 8));
        assertEquals(1, VInt.lengthForUnsigned(0x7F, 8));
        assertEquals(2, VInt.lengthForUnsigned(0x80, 8));
        assertEquals(2, VInt.lengthForUnsigned(0x3FFF, 8));
        assertEquals(3, VInt.lengthForUnsigned(0x4000, 8));

        // If we had 'pure' VInts (7 bits for each byte), max positive int would only take 9 bytes
        assertEquals(9, VInt.lengthForUnsigned(Long.MAX_VALUE, 8));
        // and since we claim to pass unsigned, -1 is actually 64 bits of payload, so:
        assertEquals(10, VInt.lengthForUnsigned(-1L, 8));
    }

    public void testRoundTrip() throws Exception
    {
        // test with different number of bits available in first byte
        _testRoundTrip(8);
        _testRoundTrip(7);
        _testRoundTrip(6);
        _testRoundTrip(5);
        _testRoundTrip(4);
    }

    /*
    /**********************************************************
    /* Helper methods
    /**********************************************************
     */
    
    private void _testRoundTrip(int bitsInFirstByte) throws Exception
    {
        // first, small values
        _assertRoundtrip(0L, bitsInFirstByte);
        _assertRoundtrip(1L, bitsInFirstByte);
        _assertRoundtrip(127L, bitsInFirstByte);
        _assertRoundtrip(128L, bitsInFirstByte);
        _assertRoundtrip(-1L, bitsInFirstByte);
        _assertRoundtrip(-127L, bitsInFirstByte);
        _assertRoundtrip(-255L, bitsInFirstByte);

        // then, single bit values (0x2,0x4, ... 0x80000000)
        for (int bit = 1; bit < 64; ++bit) {
            long value = 1L << bit;
            _assertRoundtrip(value, bitsInFirstByte);
        }
        // and 'all ones'
        for (long value = 0x3; value != -1L; value = (value << 1) + 1) {
            _assertRoundtrip(value, bitsInFirstByte);
        }
    }

    private void _assertRoundtrip(long value, int bitsForFirstByte)
    {
        // up to 10 bytes for encoded
        byte[] buffer = new byte[11];
        long[] resultLong = new long[1];
        int ptr = VInt.unsignedToBytes(value, bitsForFirstByte, buffer, 1);
        // ensure that first N bits are clear
        if (bitsForFirstByte < 8) {
            int mask = (1 << bitsForFirstByte) - 1;
            assertEquals(0, buffer[1] & ~mask);
        }
        // and that length is what expected length would look for
        int len = ptr - 1;
        assertEquals(len, VInt.lengthForUnsigned(value, bitsForFirstByte));
        int resultOffset = VInt.bytesToUnsigned(bitsForFirstByte, buffer, 1, resultLong);
        assertEquals(ptr, resultOffset);
        assertEquals(value, resultLong[0]);
    }
}
