package manual;

import java.io.*;
import java.util.*;

import com.ning.tr13.KeyValueReader;
import com.ning.tr13.TrieLookups;
import com.ning.tr13.impl.vint.SimpleVIntTrieBuilder;
import com.ning.tr13.impl.vint.VIntValueReader;
import com.ning.tr13.lookup.VIntTrieLookup;

/**
 * Simple micro-benchmark for comparing memory usage and performance of VInt-valued trie
 * against HashMap (String key, Integer value).
 */
public class MeasureVIntPerfVsMap
{
	private final File _file;
	
	public MeasureVIntPerfVsMap(File f)
	{
		_file = f;
	}

	public void testMemUsageAndSpeed() throws Exception
	{
		long initialMem = 0L;
		for (int i = 0; i < 3; ++i) {
			initialMem = memUsage();
			System.out.println("Initial memory usage: "+(initialMem>>10)+"k");
		}
		System.out.println("Loading map...");
		HashMap<String,Integer> map = loadMap();
		System.out.println("Map: "+(map.size() >> 10)+"k entries");
		long mapMem = 0L;
		for (int i = 0; i < 3; ++i) {
			mapMem = memUsage();
			System.out.println("Map mem usage: "+((mapMem - initialMem)>>10)+"k");
		}
		System.out.println("Loading trie...");
		byte[] rawTrie = loadRawTrie();
		System.out.println("Loaded trie, "+(rawTrie.length>>10)+"k bytes");
		long trieMem = 0L;
		for (int i = 0; i < 3; ++i) {
			trieMem = memUsage();
			System.out.println("Trie mem usage: "+((trieMem - mapMem)>>10)+"k");
		}

		// and finally, load entries for testing
		String[] keys = new String[map.size()];
		int i = 0;
		for (String str : map.keySet()) {
			// re-construct to avoid favoring Map with direct equality check
			keys[i++] = new String(str);
		}
		// note: since we are using bare HashMap, order might be sort of random already
		// but let's not count on it
		System.out.println("Shuffling keys");
        Random r = new Random(7);
        int len = keys.length;
        for (i = 0; i < len; ++i) {
            int ix = r.nextInt(len);
            if (ix != i) {
                String str = keys[i];
                keys[i] = keys[ix];
                keys[ix] = str;
            }
        }
        // kind of tricky to balance; tries use byte[], map String... so:
        byte[][] keys2 = new byte[len][];
		System.out.println("Generating byte[] keys");
        for (i = 0; i < len; ++i) {
        	keys2[i] = keys[i].getBytes("UTF-8");
        }
        testSpeed(keys, map, keys2, TrieLookups.constructByteArrayVIntTrie(rawTrie));
	}

	private void testSpeed(String[] keys, HashMap<String,Integer> map,
			byte[][] keys2, VIntTrieLookup trie)
	    throws Exception
	{
		System.out.println("Starting speed test!!!");

		int round = 0;
		
		while (true) {
			long start = System.currentTimeMillis();
			String desc;
			long result;
			if ((++round & 1) == 0) {
				result = testMap(keys, map);
				desc = "HashMap";
			} else {
				result = testTrie(keys2, trie);
				desc = "Trie";
			}
			long time = System.currentTimeMillis() - start;
			System.out.println("Method '"+desc+"': "+time+" msecs (result: "+Long.toHexString(result)+")");
			Thread.sleep(100L);
		}
	}

	private long testMap(String[] keys, HashMap<String,Integer> map)
	{
		long total = 0;
		for (String key : keys) {
			Integer I = map.get(key);
			total += I.intValue();
		}
		return total;
	}

	private long testTrie(byte[][] keys, VIntTrieLookup trie)
	{
		long total = 0;
		for (byte[] key : keys) {
			total += trie.getValue(key);
		}
		return total;
	}
	
	private long memUsage() throws Exception
	{
		Thread.sleep(50L);
		System.gc();
		Thread.sleep(50L);
		Runtime rt = Runtime.getRuntime();
		return rt.totalMemory() - rt.freeMemory();
	}
	
	private HashMap<String,Integer> loadMap() throws Exception
	{
        VIntValueReader kr = new VIntValueReader(_file);
        final HashMap<String,Integer> result = new HashMap<String,Integer>();
        
        kr.readAll(new KeyValueReader.ValueCallback<Long>() {
            @Override
            public void handleEntry(byte[] key, Long value) {
            	// platform-dependant is ok; only have ascii chars:
            	result.put(new String(key), Integer.valueOf(value.intValue()));
            }
        });
        return result;
	}
	
	private byte[] loadRawTrie() throws IOException
	{
	    SimpleVIntTrieBuilder b = new SimpleVIntTrieBuilder(new VIntValueReader(_file));
	    // To re-order or not? Reordering can increase speed slightly (5-10%)
	    b.setReorderEntries(true);
	    return b.build().serialize();
	}
	
	public static void main(String[] args) throws Exception
	{
		if (args.length != 1) {
			System.err.println("Usage: java .... [input-file]");
			System.exit(1);
		}
		MeasureVIntPerfVsMap test = new MeasureVIntPerfVsMap(new File(args[0]));
		test.testMemUsageAndSpeed();
	}
}
