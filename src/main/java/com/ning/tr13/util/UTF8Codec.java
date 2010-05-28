package com.ning.tr13.util;

/**
 * Helper class for doing efficient UTF-8 to/from Java strings conversion.
 * Reason we don't want use JDK provided one is that of performance -- it works fine for longer
 * Strings, but can not recycle its buffers leading to inefficient handling of short
 * Strings.
 *<p>
 * TODO: for now we will indeed just use JDK; should get code from Jackson or Woodstox for
 * fast UTF-8 codec.
 */
public class UTF8Codec
{
    public static byte[] toUTF8(String str) {
        // @TODO: actually DIY... we just fake it, for now
        try {
            return str.getBytes("UTF-8");
        } catch (java.io.UnsupportedEncodingException e) {
            throw new IllegalStateException(e);
        }
    }

    public static String fromUTF8(byte[] bytes)
    {
        // @TODO: actually DIY... we just fake it, for now
        try {
            return new String(bytes, "UTF-8");
        } catch (java.io.UnsupportedEncodingException e) {
            throw new IllegalStateException(e);
        }
    }
}
