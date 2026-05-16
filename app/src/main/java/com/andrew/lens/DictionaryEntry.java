package com.andrew.lens;

public class DictionaryEntry {
    public final String simplified;
    public final String traditional;
    public final String pinyin;           // With tone numbers: "ni3 hao3"
    public final String pinyinDisplay;    // With tone marks: "nǐ hǎo"
    public final String[] definitions;
    public final int charLength;

    public DictionaryEntry(String simplified, String traditional,
                           String pinyin, String[] definitions) {
        this.simplified = simplified;
        this.traditional = traditional;
        this.pinyin = pinyin;
        this.pinyinDisplay = PinyinConverter.toToneMarks(pinyin);
        this.definitions = definitions;
        this.charLength = simplified.length();
    }
}
