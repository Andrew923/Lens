package com.andrew.lens;

public class ChineseTextDetector {

    /**
     * Check if a character is a Chinese character (CJK Unified Ideographs)
     */
    public static boolean isChineseCharacter(char c) {
        return (c >= 0x4E00 && c <= 0x9FFF)    // CJK Unified Ideographs
            || (c >= 0x3400 && c <= 0x4DBF)    // CJK Extension A
            || (c >= 0xF900 && c <= 0xFAFF)    // CJK Compatibility Ideographs
            || (c >= 0x2E80 && c <= 0x2EFF)    // CJK Radicals Supplement
            || (c >= 0x3000 && c <= 0x303F);   // CJK Symbols and Punctuation
    }

    /**
     * Check if a string contains any Chinese characters
     */
    public static boolean containsChinese(String text) {
        if (text == null || text.isEmpty()) {
            return false;
        }
        for (int i = 0; i < text.length(); i++) {
            if (isChineseCharacter(text.charAt(i))) {
                return true;
            }
        }
        return false;
    }

    /**
     * Count the number of Chinese characters in a string
     */
    public static int countChineseCharacters(String text) {
        if (text == null || text.isEmpty()) {
            return 0;
        }
        int count = 0;
        for (int i = 0; i < text.length(); i++) {
            if (isChineseCharacter(text.charAt(i))) {
                count++;
            }
        }
        return count;
    }

    /**
     * Extract only Chinese characters from a string
     */
    public static String extractChinese(String text) {
        if (text == null || text.isEmpty()) {
            return "";
        }
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < text.length(); i++) {
            char c = text.charAt(i);
            if (isChineseCharacter(c)) {
                sb.append(c);
            }
        }
        return sb.toString();
    }
}
