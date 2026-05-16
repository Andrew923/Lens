package com.andrew.lens;

import java.util.ArrayList;
import java.util.List;

public class ChineseSegmenter {
    private final DictionaryLoader dictLoader;

    public ChineseSegmenter(DictionaryLoader dictLoader) {
        this.dictLoader = dictLoader;
    }

    /**
     * Segment Chinese text into words using maximum forward matching.
     * Non-Chinese characters are grouped together.
     */
    public List<SegmentedWord> segment(String text) {
        List<SegmentedWord> words = new ArrayList<>();
        if (text == null || text.isEmpty()) {
            return words;
        }

        int pos = 0;
        while (pos < text.length()) {
            char c = text.charAt(pos);

            if (ChineseTextDetector.isChineseCharacter(c)) {
                // Try to find longest matching Chinese word
                DictionaryEntry entry = dictLoader.findLongestMatch(text, pos);

                if (entry != null) {
                    // Found word in dictionary
                    words.add(new SegmentedWord(
                            entry.simplified,
                            pos,
                            entry
                    ));
                    pos += entry.charLength;
                } else {
                    // Single character fallback - try to look it up
                    String singleChar = String.valueOf(c);
                    DictionaryEntry singleEntry = dictLoader.lookup(singleChar);
                    words.add(new SegmentedWord(singleChar, pos, singleEntry));
                    pos++;
                }
            } else {
                // Non-Chinese character - group consecutive non-Chinese
                int start = pos;
                while (pos < text.length() && !ChineseTextDetector.isChineseCharacter(text.charAt(pos))) {
                    pos++;
                }
                String nonChinese = text.substring(start, pos);
                words.add(new SegmentedWord(nonChinese, start, null));
            }
        }

        return words;
    }
}
