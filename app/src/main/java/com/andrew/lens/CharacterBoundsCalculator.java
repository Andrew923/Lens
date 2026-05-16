package com.andrew.lens;

import android.graphics.Rect;
import java.util.ArrayList;
import java.util.List;

public class CharacterBoundsCalculator {

    /**
     * Estimate per-character bounds by dividing text bounds evenly.
     * This works reasonably well for Chinese text which uses monospace characters.
     */
    public static List<Rect> estimateCharacterBounds(String text, Rect textBounds) {
        List<Rect> bounds = new ArrayList<>();

        if (text == null || text.isEmpty() || textBounds == null) {
            return bounds;
        }

        int charCount = text.length();
        if (charCount == 0) {
            return bounds;
        }

        float charWidth = (float) textBounds.width() / charCount;

        for (int i = 0; i < charCount; i++) {
            int left = textBounds.left + (int)(i * charWidth);
            int right = textBounds.left + (int)((i + 1) * charWidth);
            bounds.add(new Rect(left, textBounds.top, right, textBounds.bottom));
        }

        return bounds;
    }

    /**
     * Get bounds for a range of characters
     */
    public static Rect getWordBounds(List<Rect> charBounds, int startIndex, int endIndex) {
        if (charBounds == null || charBounds.isEmpty()) {
            return null;
        }

        startIndex = Math.max(0, startIndex);
        endIndex = Math.min(charBounds.size(), endIndex);

        if (startIndex >= endIndex) {
            return null;
        }

        Rect first = charBounds.get(startIndex);
        Rect last = charBounds.get(endIndex - 1);

        return new Rect(first.left, first.top, last.right, last.bottom);
    }
}
