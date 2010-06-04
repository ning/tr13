package com.ning.tr13.read;

import java.io.*;
import java.nio.ByteBuffer;
import java.util.*;

import com.ning.tr13.*;
import com.ning.tr13.impl.vint.SimpleVIntTrieBuilder;
import com.ning.tr13.lookup.ByteArrayTrie;
import com.ning.tr13.lookup.ByteBufferTrie;
import com.ning.tr13.util.UTF8Codec;

public class VIntTrieLookupTest
    extends junit.framework.TestCase
{
    final static Map<String,Number> TEST_ENTRIES = new LinkedHashMap<String,Number>();
    static {
        TEST_ENTRIES.put("ab", 10);
        TEST_ENTRIES.put("abc", 20);
        TEST_ENTRIES.put("abe", 3);
        TEST_ENTRIES.put("afgh", 4);
        TEST_ENTRIES.put("foo", 5);
    }

    public void testSimpleUsingByteBuffer() throws Exception
    {
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        new SimpleVIntTrieBuilder(new MapReader(TEST_ENTRIES)).buildAndWrite(out, false);
        byte[] raw = out.toByteArray();
        _testSimple(new ByteBufferTrie(ByteBuffer.wrap(raw), raw.length));
    }

    public void testSimpleUsingByteArray() throws Exception
    {
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        new SimpleVIntTrieBuilder(new MapReader(TEST_ENTRIES)).buildAndWrite(out, false);
        byte[] raw = out.toByteArray();
        _testSimple(new ByteArrayTrie(raw));
    }

    /*
    /**********************************************************
    /* Helper methods
    /**********************************************************
     */

    private void _testSimple(TrieLookup trie) throws Exception
    {
        for (Map.Entry<String,Number> entry : TEST_ENTRIES.entrySet()) {
            long value = entry.getValue().longValue();
            assertEquals(value, trie.getValue(entry.getKey().getBytes("UTF-8")));
        }
        // and then others we shouldn't get
        assertNull(trie.findValue("fo".getBytes("UTF-8")));
        assertEquals(-1L, trie.getValue("fo".getBytes("UTF-8"), -1L));
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
        extends KeyValueReader<Long>
    {
        final Map<String,Number> _entries;
        
        public MapReader(Map<String,Number> entries) throws IOException        
        {
            super(new ByteArrayInputStream(new byte[0]));
            _entries = entries;
        }

        @Override
        protected void parseAndHandle(KeyValueReader.ValueCallback<Long> handler, byte[] key, String value)
        { }
        
        @Override
        public void readAll(ValueCallback<Long> handler) throws IOException
        {
            for (Map.Entry<String,Number> en : _entries.entrySet()) {
                handler.handleEntry(UTF8Codec.toUTF8(en.getKey()), en.getValue().longValue());
            }
        }
    }
}
