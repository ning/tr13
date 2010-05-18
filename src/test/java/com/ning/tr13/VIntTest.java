package com.ning.tr13;

public class VIntTest
    extends junit.framework.TestCase
{
    public void testDummy() throws Exception
    {
        // note: 8 means 8 available data bits; but one bit is needed for indicating where it's last byte
        assertEquals(1, VInt.lengthForUnsigned(0x1, 8));
        assertEquals(1, VInt.lengthForUnsigned(0x7F, 8));
        assertEquals(2, VInt.lengthForUnsigned(0x80, 8));
        assertEquals(2, VInt.lengthForUnsigned(0x3FFF, 8));
        assertEquals(3, VInt.lengthForUnsigned(0x4000, 8));
    }
}
