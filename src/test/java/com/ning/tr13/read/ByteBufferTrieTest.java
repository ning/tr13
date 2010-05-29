package com.ning.tr13.read;

import java.io.*;
import java.nio.ByteBuffer;
import java.util.*;

import com.ning.tr13.*;
import com.ning.tr13.build.SimpleTrieBuilder;

public class ByteBufferTrieTest
    extends junit.framework.TestCase
{
    public void testSimple() throws Exception
    {
        Map<String,Number> entries = new LinkedHashMap<String,Number>();
        entries.put("ab", 10);
        entries.put("abc", 20);
        entries.put("abe", 3);
        entries.put("afgh", 4);
        entries.put("foo", 5);

        ByteArrayOutputStream out = new ByteArrayOutputStream();
        new SimpleTrieBuilder(new MapReader(entries)).buildAndWrite(out, false);
        byte[] raw = out.toByteArray();
        ByteBufferTrie trie = new ByteBufferTrie(ByteBuffer.wrap(raw), raw.length);
        for (Map.Entry<String,Number> entry : entries.entrySet()) {
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
