package com.andrew.lens;

public class SegmentedWord {
    public final String text;
    public final int startIndex;
    public final DictionaryEntry entry; // null if not found in dictionary

    public SegmentedWord(String text, int startIndex, DictionaryEntry entry) {
        this.text = text;
        this.startIndex = startIndex;
        this.entry = entry;
    }

    public int getEndIndex() {
        return startIndex + text.length();
    }

    public boolean hasDefinition() {
        return entry != null;
    }
}
