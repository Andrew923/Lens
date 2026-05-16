package com.andrew.lens;

import android.graphics.Rect;
import java.util.List;

public class TextRegion {
    public final String text;
    public final Rect bounds;
    public final String nodeType;
    public boolean isSelected;

    // Chinese support fields
    public final boolean containsChinese;
    public List<SegmentedWord> segmentedWords;
    public List<Rect> characterBounds;

    public TextRegion(String text, Rect bounds, String nodeType) {
        this.text = text;
        this.bounds = bounds;
        this.nodeType = nodeType;
        this.isSelected = false;
        this.containsChinese = ChineseTextDetector.containsChinese(text);
        this.segmentedWords = null;
        this.characterBounds = null;
    }

    /**
     * Set enriched Chinese data
     */
    public void setChineseData(List<SegmentedWord> words, List<Rect> charBounds) {
        this.segmentedWords = words;
        this.characterBounds = charBounds;
    }
}
