package com.andrew.lens;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class PinyinConverter {
    // Vowels with tone marks for tones 1-4 (index 0-3), neutral tone uses base vowel
    private static final String[] A_TONES = {"ā", "á", "ǎ", "à", "a"};
    private static final String[] E_TONES = {"ē", "é", "ě", "è", "e"};
    private static final String[] I_TONES = {"ī", "í", "ǐ", "ì", "i"};
    private static final String[] O_TONES = {"ō", "ó", "ǒ", "ò", "o"};
    private static final String[] U_TONES = {"ū", "ú", "ǔ", "ù", "u"};
    private static final String[] V_TONES = {"ǖ", "ǘ", "ǚ", "ǜ", "ü"};

    private static final Pattern SYLLABLE_PATTERN = Pattern.compile(
            "([a-zA-ZüÜ]+)(\\d)?", Pattern.CASE_INSENSITIVE);

    /**
     * Convert numbered pinyin to tone marks.
     * Example: "ni3 hao3" -> "nǐ hǎo"
     */
    public static String toToneMarks(String numbered) {
        if (numbered == null || numbered.isEmpty()) {
            return numbered;
        }

        StringBuilder result = new StringBuilder();
        Matcher matcher = SYLLABLE_PATTERN.matcher(numbered.toLowerCase());
        int lastEnd = 0;

        while (matcher.find()) {
            // Append any text between matches (spaces, punctuation)
            if (matcher.start() > lastEnd) {
                result.append(numbered.substring(lastEnd, matcher.start()));
            }

            String syllable = matcher.group(1);
            String toneStr = matcher.group(2);
            int tone = (toneStr != null) ? Integer.parseInt(toneStr) : 5;

            result.append(applyToneMark(syllable, tone));
            lastEnd = matcher.end();
        }

        // Append any remaining text
        if (lastEnd < numbered.length()) {
            result.append(numbered.substring(lastEnd));
        }

        return result.toString();
    }

    private static String applyToneMark(String syllable, int tone) {
        if (tone < 1 || tone > 5) {
            tone = 5; // neutral
        }
        int toneIndex = tone - 1;

        // Handle ü written as v or u:
        syllable = syllable.replace("v", "ü").replace("V", "Ü");

        // Find the vowel to apply tone mark to
        // Rules:
        // 1. If there's an 'a' or 'e', it takes the tone mark
        // 2. If there's 'ou', the 'o' takes it
        // 3. Otherwise, the second vowel takes it

        char[] chars = syllable.toCharArray();
        int vowelIndex = -1;

        // Rule 1: 'a' or 'e' takes the mark
        for (int i = 0; i < chars.length; i++) {
            char c = Character.toLowerCase(chars[i]);
            if (c == 'a' || c == 'e') {
                vowelIndex = i;
                break;
            }
        }

        // Rule 2: 'ou' - 'o' takes the mark
        if (vowelIndex == -1) {
            int ouIndex = syllable.toLowerCase().indexOf("ou");
            if (ouIndex != -1) {
                vowelIndex = ouIndex;
            }
        }

        // Rule 3: Second vowel takes it (for iu, ui, etc.)
        if (vowelIndex == -1) {
            int vowelCount = 0;
            for (int i = 0; i < chars.length; i++) {
                char c = Character.toLowerCase(chars[i]);
                if (isVowel(c)) {
                    vowelCount++;
                    if (vowelCount == 2 || vowelIndex == -1) {
                        vowelIndex = i;
                    }
                    if (vowelCount == 2) break;
                }
            }
        }

        if (vowelIndex == -1) {
            return syllable; // No vowel found
        }

        // Apply the tone mark
        StringBuilder result = new StringBuilder();
        for (int i = 0; i < chars.length; i++) {
            if (i == vowelIndex) {
                result.append(getTonedVowel(chars[i], toneIndex));
            } else {
                result.append(chars[i]);
            }
        }

        return result.toString();
    }

    private static boolean isVowel(char c) {
        return "aeiouü".indexOf(Character.toLowerCase(c)) != -1;
    }

    private static String getTonedVowel(char vowel, int toneIndex) {
        switch (Character.toLowerCase(vowel)) {
            case 'a': return A_TONES[toneIndex];
            case 'e': return E_TONES[toneIndex];
            case 'i': return I_TONES[toneIndex];
            case 'o': return O_TONES[toneIndex];
            case 'u': return U_TONES[toneIndex];
            case 'ü': return V_TONES[toneIndex];
            default: return String.valueOf(vowel);
        }
    }
}
