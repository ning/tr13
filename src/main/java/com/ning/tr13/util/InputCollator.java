package com.ning.tr13.util;

import java.io.*;
import java.util.*;

/**
 * Simple helper class that can be used to convert a file that has
 * 'String [SEPARATOR-CHAR] String' TEST_ENTRIES into one with
 * 'String [SEPARATOR-CHAR] minimal-int-id' TEST_ENTRIES.
 *<p>
 * TODO: need to be able to access underlying mapping so that actual
 * original mappings can be recovered.
 * 
 * @author tatu
 */
public class InputCollator
{
    protected final char _separatorChar;
    
    public InputCollator(char sepChar)
    {
        _separatorChar = sepChar;
    }
    
    public void doIt(String filename) throws Exception
    {
        BufferedReader r = new BufferedReader(new InputStreamReader(new FileInputStream(filename), "UTF-8"));
        String line;
        String prevIdStr = "";
        int linenr = 0;
        int skipped = 0;

        final HashMap<String,Integer> ids = new HashMap<String,Integer>();
        int nextId = 0;
        
        while ((line = r.readLine()) != null) {
            if ((++linenr & 0xFFFFF) == 0) {
                System.err.println("Processed "+(linenr>>10)+"K lines, skipped "+(skipped>>10)+"K; last id: "+nextId);
            }
            int ix = line.indexOf(_separatorChar);
            if (ix < 1) {
                System.err.println("WARN: invalid line "+linenr+", skip.");
                continue;
            }
            String key = line.substring(0, ix);
            if (key.equals(prevIdStr)) {
                ++skipped;
                continue;
            }
            prevIdStr = key;
            String value = line.substring(ix+1);
            Integer I = ids.get(value);
            if (I == null) {
                I = Integer.valueOf(++nextId);
                ids.put(value, I);
            }
            System.out.print(key);
            System.out.print(_separatorChar);
            System.out.println(I.intValue());
        }
        System.err.println("DONE: read "+linenr+" lines, skipped "+skipped+"; "+nextId+" ids");
    }
    
    public static void main(String[] args) throws Exception
    {
        if (args.length != 1) {
            System.err.println("Usage: java ... [input file]");
            System.exit(1);
        }
        new InputCollator('|').doIt(args[0]);
    }
}
