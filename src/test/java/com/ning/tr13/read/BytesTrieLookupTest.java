package com.ning.tr13.read;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.util.LinkedHashMap;
import java.util.Map;

import static org.junit.Assert.*;

import com.ning.tr13.KeyValueReader;
import com.ning.tr13.TrieLookup;
import com.ning.tr13.impl.bytes.ByteArrayBytesTrieLookup;
import com.ning.tr13.impl.bytes.ByteBufferBytesTrieLookup;
import com.ning.tr13.impl.bytes.SimpleBytesTrieBuilder;
import com.ning.tr13.util.UTF8Codec;

public class BytesTrieLookupTest
    extends junit.framework.TestCase
{
    final static Map<String,String> TEST_ENTRIES = new LinkedHashMap<String,String>();
    static {
        TEST_ENTRIES.put("ab", "foob");
        TEST_ENTRIES.put("abc", "f");
        TEST_ENTRIES.put("abe", "xxxxx");
        TEST_ENTRIES.put("afgh", "-12535");
        TEST_ENTRIES.put("foo", "");
        TEST_ENTRIES.put("foobar", "ouch");
        TEST_ENTRIES.put("fx", "special");
    }

    public void testSimpleUsingByteBuffer() throws Exception
    {
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        new SimpleBytesTrieBuilder(new MapReader(TEST_ENTRIES)).buildAndWrite(out, false);
        byte[] raw = out.toByteArray();
/*        
for (int i = 0; i < raw.length; ++i) {
    System.out.println("#"+i+" -> 0x"+Integer.toHexString(0xFF & raw[i])+" / '"+((char) raw[i])+"'");
}
   */     
        _testSimple(new ByteBufferBytesTrieLookup(ByteBuffer.wrap(raw), raw.length));
    }

    public void testSimpleUsingByteArray() throws Exception
    {
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        new SimpleBytesTrieBuilder(new MapReader(TEST_ENTRIES)).buildAndWrite(out, false);
        byte[] raw = out.toByteArray();
        _testSimple(new ByteArrayBytesTrieLookup(raw));
    }

    /*
    /**********************************************************
    /* Helper methods
    /**********************************************************
     */

    private void _testSimple(TrieLookup<byte[]> trie) throws Exception
    {
        int ix = 0;
        for (Map.Entry<String,String> entry : TEST_ENTRIES.entrySet()) {
            ++ix;
            byte[] expValue = UTF8Codec.toUTF8(entry.getValue());
            String desc = "Entry "+ix+"/"+TEST_ENTRIES.size();
            byte[] actual = trie.findValue(entry.getKey().getBytes("UTF-8"));
            assertNotNull(desc+" not found", actual);
            assertArrayEquals(desc, expValue, actual);
        }
        // and then others we shouldn't get
        assertNull(trie.findValue("fo".getBytes("UTF-8")));
        assertNull(trie.findValue("foob".getBytes("UTF-8")));
        assertNull(trie.findValue("xuz".getBytes("UTF-8")));
        assertNull(trie.findValue("".getBytes("UTF-8")));
        assertNull(trie.findValue("a".getBytes("UTF-8")));
    }

    /*
    /**********************************************************
    /* Helper classes
    /**********************************************************
     */

    private static class MapReader
        extends KeyValueReader<byte[]>
    {
        final Map<String,String> _entries;
        
        public MapReader(Map<String,String> entries) throws IOException        
        {
            super(new ByteArrayInputStream(new byte[0]));
            _entries = entries;
        }

        @Override
        protected void parseAndHandle(KeyValueReader.ValueCallback<byte[]> handler, byte[] key, String value)
        { }
        
        @Override
        public void readAll(ValueCallback<byte[]> handler) throws IOException
        {
            for (Map.Entry<String,String> en : _entries.entrySet()) {
                handler.handleEntry(UTF8Codec.toUTF8(en.getKey()),
                        UTF8Codec.toUTF8(en.getValue()));
            }
        }
    }
}
