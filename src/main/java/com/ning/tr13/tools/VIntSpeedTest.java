package com.ning.tr13.tools;

import java.io.*;
import java.nio.ByteBuffer;
import java.util.*;
import java.util.concurrent.atomic.AtomicInteger;

import com.ning.tr13.*;
import com.ning.tr13.impl.vint.*;

public class VIntSpeedTest
{
    /**
     * We'll sample key set, take and use every Nth entry...
     */
    public final static int KEY_SAMPLING_RATIO = 39;
    
    private final KeyEntry[] entries;
    
    public VIntSpeedTest(KeyEntry[] entries)
    {
        this.entries = entries;
    }

    public long test(TrieLookup<Long> lookup)
    {
        long total = 0L;
        for (int i = 0, len = entries.length; i < len; ++i) {
            KeyEntry entry = entries[i];
            Long value = lookup.findValue(entry.rawKey);
            if (value == null || value.longValue() != entry.value) {
                throw new IllegalStateException("Problem with "+lookup+", entry #"+i+", value "
                        +value+"; expected "+entry.value);
            }
            total += value;
        }
        return total;
    }
    
    protected static KeyEntry[] loadKeys(File f, final int sampleRatio)
        throws IOException
    {
        VIntValueReader kr = new VIntValueReader(f);
        final ArrayList<KeyEntry> entries = new ArrayList<KeyEntry>();
        final AtomicInteger total = new AtomicInteger(0);
        
        kr.readAll(new KeyValueReader.ValueCallback<Long>() {
            @Override
            public void handleEntry(byte[] key, Long value) {
                int count = total.addAndGet(1);
                if ((count % sampleRatio) == 0) {
                    entries.add(new KeyEntry(key, value));
                }
            }
        });
        System.out.println("Read "+(total.get()>>10)+"k entries, sampled "+(entries.size()>>10)+"k.");
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
        VIntValueReader kr = new VIntValueReader(f);
        SimpleVIntTrieBuilder b = new SimpleVIntTrieBuilder(kr);
        // To re-order or not? Reordering increases speed by ~10%:
        final boolean REORDER = true;
        System.out.println("Reorder entries: "+REORDER);
        b.setReorderEntries(REORDER);
        byte[] rawTrie = b.build().serialize();
        b = null; // just ensure we can GC interemediate stuff
        TrieLookup<Long> arrayBased = new ByteArrayVIntTrieLookup(rawTrie);
        ByteBuffer buffer = ByteBuffer.allocateDirect((int) rawTrie.length);
        System.out.println("ByteBuffer: is-direct? "+buffer.isDirect());
        buffer.put(rawTrie);

        TrieLookup<Long> bufferBased = new ByteBufferVIntTrieLookup(buffer, rawTrie.length);
        VIntSpeedTest test = new VIntSpeedTest(entries);
        for (int i = 0; true; ++i) {
            long start = System.currentTimeMillis();
            TrieLookup<Long> trie;
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
