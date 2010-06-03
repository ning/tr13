package com.ning.tr13.impl.vint;

import com.ning.tr13.build.ValueEncoder;
import com.ning.tr13.util.VInt;

public final class VIntValueEncoder
    extends ValueEncoder<Long>
{
    @Override
    public int byteLength(Long value, int firstByteBits) {
        return VInt.lengthForUnsigned(value.longValue(), firstByteBits);
    }

    @Override
    public int serialize(Long value, int firstByteBits, byte[] result, int offset) {
        return VInt.unsignedToBytes(value.longValue(), firstByteBits, result, offset);
    }

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
}
