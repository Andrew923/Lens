package com.andrew.lens;

import java.util.HashMap;
import java.util.Map;

public class ChineseTrie {
    private final TrieNode root = new TrieNode();

    public static class TrieNode {
        final Map<Character, TrieNode> children = new HashMap<>();
        DictionaryEntry entry; // null if not end of word
    }

    /**
     * Insert a dictionary entry into the trie
     */
    public void insert(DictionaryEntry entry) {
        TrieNode node = root;
        for (char c : entry.simplified.toCharArray()) {
            node = node.children.computeIfAbsent(c, k -> new TrieNode());
        }
        // If multiple entries for same word, keep the first (most common)
        if (node.entry == null) {
            node.entry = entry;
        }
    }

    /**
     * Find the longest matching word starting at position in text.
     * Returns null if no match found.
     */
    public DictionaryEntry findLongestMatch(String text, int startPos) {
        TrieNode node = root;
        DictionaryEntry lastMatch = null;

        for (int i = startPos; i < text.length(); i++) {
            char c = text.charAt(i);
            TrieNode child = node.children.get(c);
            if (child == null) {
                break;
            }
            node = child;
            if (node.entry != null) {
                lastMatch = node.entry;
            }
        }
        return lastMatch;
    }

    /**
     * Find exact match for a word.
     */
    public DictionaryEntry findExact(String word) {
        TrieNode node = root;
        for (char c : word.toCharArray()) {
            TrieNode child = node.children.get(c);
            if (child == null) {
                return null;
            }
            node = child;
        }
        return node.entry;
    }

    /**
     * Get the number of entries in the trie
     */
    public int size() {
        return countEntries(root);
    }

    private int countEntries(TrieNode node) {
        int count = node.entry != null ? 1 : 0;
        for (TrieNode child : node.children.values()) {
            count += countEntries(child);
        }
        return count;
    }
}
