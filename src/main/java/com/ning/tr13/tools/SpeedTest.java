package com.ning.tr13.tools;

import java.io.*;
import java.nio.ByteBuffer;
import java.util.*;

import com.ning.tr13.*;
import com.ning.tr13.build.*;
import com.ning.tr13.read.*;
import com.ning.tr13.util.UTF8Codec;

public class SpeedTest
{
    /**
     * We'll sample key set, take and use every Nth entry...
     */
    public final static int KEY_SAMPLING_RATIO = 39;
    
    private final KeyEntry[] entries;
    
    public SpeedTest(KeyEntry[] entries)
    {
        this.entries = entries;
    }

    public long test(TrieLookup lookup)
    {
        long total = 0L;
        for (int i = 0, len = entries.length; i < len; ++i) {
            KeyEntry entry = entries[i];
            long value = lookup.getValue(entry.rawKey);
            if (value != entry.value) {
                throw new IllegalStateException("Problem with "+lookup+", entry #"+i+", value "
                        +value+"; expected "+entry.value);
            }
            total += value;
        }
        return total;
    }
    
    protected static KeyEntry[] loadKeys(File f, int sampleRatio)
        throws IOException
    {
        KeyValueReader kr = new KeyValueReader(f);
        ArrayList<KeyEntry> entries = new ArrayList<KeyEntry>();
        String str;
        int total = 0;
        int count = sampleRatio;
        while ((str = kr.nextEntry()) != null) {
            ++total;
            if (--count < 1) {
                count = sampleRatio;
                entries.add(new KeyEntry(UTF8Codec.toUTF8(str), kr.getValue()));
            }
        }
        System.out.println("Read "+(total>>10)+"k entries, sampled "+(entries.size()>>10)+"k.");
        return entries.toArray(new KeyEntry[entries.size()]);
    }

    protected static KeyEntry[] shuffleEntries(KeyEntry[] entries) throws IOException
    {
        int len = entries.length;
        Random r = new Random(len);
        for (int i = 0; i < len; ++i) {
            int ix = r.nextInt(len);
            if (ix != i) {
                KeyEntry e = entries[i];
                entries[i] = entries[ix];
                entries[ix] = e;
            }
        }
        return entries;
    }
    
    public static void main(String[] args) throws Exception
    {
        if (args.length != 1) {
            System.err.println("Usage: test file");
            System.exit(1);
        }
        File f = new File(args[0]);
        // First: read just keys
        System.out.println("Loading keys...");
        KeyEntry[] entries = shuffleEntries(loadKeys(f, KEY_SAMPLING_RATIO));
        // Then build tries
        System.out.println("Building raw trie data...");
        KeyValueReader kr = new KeyValueReader(f);
        TrieNode root = new SimpleTrieBuilder(kr).build();     
        byte[] rawTrie = root.serialize();
        root = null; // need to be GCed for bigger tries..
        TrieLookup arrayBased = new ByteArrayTrie(rawTrie);
        ByteBuffer buffer = ByteBuffer.allocateDirect((int) rawTrie.length);
        System.out.println("ByteBuffer: is-direct? "+buffer.isDirect());
        buffer.put(rawTrie);

        TrieLookup bufferBased = new ByteBufferTrie(buffer, rawTrie.length);
        SpeedTest test = new SpeedTest(entries);
        for (int i = 0; true; ++i) {
            long start = System.currentTimeMillis();
            TrieLookup trie;
            switch (i % 2) {
            case 1:
                trie = bufferBased;
                break;
            default:
                trie = arrayBased;
            }
            long result = test.test(trie);
            long time = System.currentTimeMillis() - start;
            System.out.println("Took "+time+" msecs for "+trie.getClass()+" (result "
                    +Long.toHexString(result)+")");
            Thread.sleep(100L);
        }
    }

    private final static class KeyEntry {
        public final byte[] rawKey;
        public final long value;
        
        public KeyEntry(byte[] key, long value) {
            rawKey = key;
            this.value = value;
        }
    }
}
