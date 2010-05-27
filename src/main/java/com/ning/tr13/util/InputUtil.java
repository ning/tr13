package com.ning.tr13.util;

import java.io.IOException;
import java.io.InputStream;

public class InputUtil
{
    public static void readFully(InputStream in, byte[] buffer) throws IOException
    {
        readFully(in, buffer, 0, buffer.length);
    }

    public static void readFully(InputStream in, byte[] buffer, int offset, int len) throws IOException
    {
        final int end = offset+len;
        while (offset < end) {
            int count = in.read(buffer, offset, end - offset);
            if (count < 1) {
                throw new IllegalArgumentException("Could not read "+buffer.length+" bytes; only got "+offset);
            }
            offset += count;
        }
    }
}
