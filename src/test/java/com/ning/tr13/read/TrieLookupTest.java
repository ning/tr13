package com.ning.tr13.read;

import java.io.*;
import java.nio.ByteBuffer;
import java.util.*;

import com.ning.tr13.*;
import com.ning.tr13.build.SimpleTrieBuilder;
import com.ning.tr13.lookup.ByteArrayTrie;
import com.ning.tr13.lookup.ByteBufferTrie;

public class TrieLookupTest
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

    public void testSimpleWiteByteBuffer() throws Exception
    {
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        new SimpleTrieBuilder(new MapReader(TEST_ENTRIES)).buildAndWrite(out, false);
        byte[] raw = out.toByteArray();
        _testSimple(new ByteBufferTrie(ByteBuffer.wrap(raw), raw.length));
    }

    public void testSimpleWiteByteArray() throws Exception
    {
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        new SimpleTrieBuilder(new MapReader(TEST_ENTRIES)).buildAndWrite(out, false);
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
        extends KeyValueReader
    {
        final Iterator<Map.Entry<String,Number>> _entryIt;
        Map.Entry<String,Number> _current;
        
        public MapReader(Map<String,Number> entries) throws IOException        
        {
            super(new ByteArrayInputStream(new byte[0]));
            _entryIt = entries.entrySet().iterator();
        }

        public long getValue() { return _current.getValue().longValue(); }
        
        public String nextEntry() throws IOException
        {
            if (_entryIt.hasNext()) {
                _current = _entryIt.next();
                return _current.getKey();
            }
            return null;
        }
    }
}
