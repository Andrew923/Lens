package com.andrew.lens;

import android.content.Context;
import android.util.Log;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class DictionaryLoader {
    private static final String TAG = "DictionaryLoader";
    private static DictionaryLoader instance;

    private ChineseTrie trie;
    private boolean isLoaded = false;
    private boolean isLoading = false;

    // CC-CEDICT format: Traditional Simplified [pinyin] /def1/def2/
    private static final Pattern CEDICT_PATTERN = Pattern.compile(
            "^(\\S+)\\s+(\\S+)\\s+\\[([^\\]]+)\\]\\s+/(.+)/$");

    private DictionaryLoader() {
        trie = new ChineseTrie();
    }

    public static synchronized DictionaryLoader getInstance() {
        if (instance == null) {
            instance = new DictionaryLoader();
        }
        return instance;
    }

    public ChineseTrie getTrie() {
        return trie;
    }

    public boolean isLoaded() {
        return isLoaded;
    }

    /**
     * Load dictionary from assets. Call from background thread.
     */
    public synchronized void ensureLoaded(Context context) {
        if (isLoaded || isLoading) {
            return;
        }

        isLoading = true;
        long startTime = System.currentTimeMillis();

        try {
            InputStream is = context.getAssets().open("cedict.txt");
            BufferedReader reader = new BufferedReader(new InputStreamReader(is, "UTF-8"));

            String line;
            int count = 0;
            while ((line = reader.readLine()) != null) {
                // Skip comments and empty lines
                if (line.startsWith("#") || line.trim().isEmpty()) {
                    continue;
                }

                DictionaryEntry entry = parseLine(line);
                if (entry != null) {
                    trie.insert(entry);
                    count++;
                }
            }

            reader.close();
            is.close();

            isLoaded = true;
            long elapsed = System.currentTimeMillis() - startTime;
            Log.i(TAG, "Loaded " + count + " dictionary entries in " + elapsed + "ms");

        } catch (IOException e) {
            Log.e(TAG, "Failed to load dictionary", e);
        } finally {
            isLoading = false;
        }
    }

    private DictionaryEntry parseLine(String line) {
        Matcher matcher = CEDICT_PATTERN.matcher(line);
        if (!matcher.matches()) {
            return null;
        }

        String traditional = matcher.group(1);
        String simplified = matcher.group(2);
        String pinyin = matcher.group(3);
        String definitionsStr = matcher.group(4);

        // Split definitions by /
        String[] definitions = definitionsStr.split("/");

        return new DictionaryEntry(simplified, traditional, pinyin, definitions);
    }

    /**
     * Look up a word or character
     */
    public DictionaryEntry lookup(String text) {
        if (!isLoaded) {
            return null;
        }
        return trie.findExact(text);
    }

    /**
     * Find longest match starting at position
     */
    public DictionaryEntry findLongestMatch(String text, int startPos) {
        if (!isLoaded) {
            return null;
        }
        return trie.findLongestMatch(text, startPos);
    }
}
