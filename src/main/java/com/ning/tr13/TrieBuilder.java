package com.ning.tr13;

import java.io.*;

/**
 * Class used for building binary trie structure from input source and
 * writing it to an output target; input and output are typically
 * files.
 * 
 * @author tatu
 */
public class TrieBuilder
{
    protected final KeyValueReader _reader;
    
    public TrieBuilder(KeyValueReader r)
    {
        _reader = r;
    }

    public ClosedNode build() throws IOException
    {
        OpenNode root = new OpenNode((byte) 0, null);
        String idStr;

        int count = 0;
        int skipped = 0;
        String prev = "";

        main_loop:
        while ((idStr = _reader.nextEntry()) != null) {
            long value = _reader.getValue();
            byte[] id = idStr.getBytes("UTF-8");

            // !!! Remove when not needed
            // sanity checks for dups:
            if (prev.equals(idStr)) {
                ++skipped;
                continue main_loop;
            }
            prev = idStr;
            
            OpenNode curr = root;
            int i = 0;
            // first, skip out common ancestry
            while (true) {
                OpenNode next = curr._currentChild;
                if (next == null || next._nodeByte != id[i]) break;
                if (++i >= id.length) { // sanity check, could skip
                    // appears that we have some of this problem
                    /*
                    throw new IllegalArgumentException("Malformed input, line "
                            +_reader.getLineNumber()+": id '"+idStr+"' not properly ordered");
                            */
                    ++skipped;
                    continue main_loop;
                }
                curr = next;
            }
            // then attach to where we diverge
            for (int last = id.length-1; i <= last; ++i) {
                OpenNode next = new OpenNode(id[i],
                        (i == last) ? Long.valueOf(value) : null);
                curr.addNode(next);
                curr = next;
            }

            if ((++count & 0xFFFFF) == 0) {
                System.out.println("Building: "+(count>>10)+"k lines processed, "
                        +(skipped>>10)+"k skipped");
            }
        }
        
        return root.close();
    }

    public static void main(String[] args) throws Exception
    {
        if (args.length != 1) {
            System.err.println("USAGE: java ... [input-file]");
            System.exit(1);
        }
        KeyValueReader r = new KeyValueReader(new FileInputStream(args[0]));
        TrieBuilder b = new TrieBuilder(r);
        ClosedNode n = b.build();
        long len = n.length();
        System.out.println("Built: expected length is "+len+" bytes");
        r.close();
    }
}
