# Purpose

Tr13 is a library for constructing and using read-only compact (memory-efficient) in-memory [trie](http://en.wikipedia.org/wiki/Trie) data structures, using raw (byte-sequences, `byte[]`) keys and values. Raw values can be automatically
converted to/from basic JDK types such as `java.lang.String`. Resulting `trie`s are "raw", in that they are stored as raw byte sequences (`ByteBuffer`, off-heap or in-heap, or `byte[]`), meaning that garbage-collection overhead should be minimal as the main data area is either off-heap (for native or mmap'ed `ByteBuffer`s), or a single `byte[]`.

Main benefits for using such tries is both relatively low memory usage (typically 20 - 35% less than raw input size) and low GC impact.

## Development

Mailing list: http://groups.google.com/group/ning-tr13-users

## Usage

Tries need to be built in lexicographic order, so pre-sorting may be needed.
For that, you may want to check out [Java Merge-sort](https://github.com/cowtowncoder/java-merge-sort) project.
Key and value types 

Building is done in two steps:

1. Construct raw trie structure in streaming fashion, either into a memory buffer (like `ByteArrayOutputStream`), or a File.
2. Construct actual `trie` from serialized raw sequence (`File`, `byte[]`).

Two-phase processing is needed since the actual result size is not known in advance.

## Sample

This [tr13 from LevelDB](https://gist.github.com/cowtowncoder/4e0b2308b1f660b8b855) gist shows simple usage.

## Documentation

Check out [https://github.com/ning/tr13/wiki]
