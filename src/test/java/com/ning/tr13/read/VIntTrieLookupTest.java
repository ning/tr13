package com.ning.tr13.read;

import java.io.*;
import java.nio.ByteBuffer;
import java.util.*;

import com.ning.tr13.*;
import com.ning.tr13.impl.vint.ByteArrayVIntTrieLookup;
import com.ning.tr13.impl.vint.ByteBufferVIntTrieLookup;
import com.ning.tr13.impl.vint.SimpleVIntTrieBuilder;
import com.ning.tr13.util.UTF8Codec;

public class VIntTrieLookupTest
    extends junit.framework.TestCase
{
    final static Map<String,Number> TEST_ENTRIES = new LinkedHashMap<String,Number>();
    static {
        /*
        TEST_ENTRIES.put("ab", 10);
        TEST_ENTRIES.put("abc", 20);
        TEST_ENTRIES.put("abe", 3);
        TEST_ENTRIES.put("afgh", 4);
        TEST_ENTRIES.put("foo", 5);
        */

        TEST_ENTRIES.put("ab", 1);
        TEST_ENTRIES.put("abc", 2);
        TEST_ENTRIES.put("abe", 3);
        TEST_ENTRIES.put("afgh", 4);
        TEST_ENTRIES.put("foo", 5);
        TEST_ENTRIES.put("foobar", 6);
        TEST_ENTRIES.put("fx", 7);
    }

    public VIntTrieLookupTest() { }
    
    public void testSimpleUsingByteBuffer() throws Exception
    {
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        new SimpleVIntTrieBuilder(new MapReader(TEST_ENTRIES)).buildAndWrite(out, false);
        byte[] raw = out.toByteArray();
        _testSimple(new ByteBufferVIntTrieLookup(ByteBuffer.wrap(raw), raw.length));
    }

    public void testSimpleUsingByteArray() throws Exception
    {
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        new SimpleVIntTrieBuilder(new MapReader(TEST_ENTRIES)).buildAndWrite(out, false);
        byte[] raw = out.toByteArray();
        _testSimple(new ByteArrayVIntTrieLookup(raw));
    }

    /*
    /**********************************************************
    /* Helper methods
    /**********************************************************
     */

    private void _testSimple(TrieLookup<Long> trie) throws Exception
    {
        for (Map.Entry<String,Number> entry : TEST_ENTRIES.entrySet()) {
            Long value = entry.getValue().longValue();
            assertEquals(value, trie.findValue(entry.getKey().getBytes("UTF-8")));
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
        extends KeyValueSource<Long>
    {
        final Map<String,Number> _entries;
 
        protected int _lineNr;
        
        public MapReader(Map<String,Number> entries) {
            _entries = entries;
        }
        
        @Override
        public void readAll(ValueCallback<Long> handler) throws IOException
        {
            for (Map.Entry<String,Number> en : _entries.entrySet()) {
                ++_lineNr;
                handler.handleEntry(UTF8Codec.toUTF8(en.getKey()), en.getValue().longValue());
            }
        }

        @Override public int getLineNumber() { return _lineNr; }
    }
}
