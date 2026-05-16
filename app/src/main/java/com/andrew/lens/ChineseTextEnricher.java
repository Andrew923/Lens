package com.andrew.lens;

import android.content.Context;
import android.graphics.Rect;
import java.util.List;

public class ChineseTextEnricher {
    private final DictionaryLoader dictLoader;
    private final ChineseSegmenter segmenter;

    public ChineseTextEnricher(Context context) {
        this.dictLoader = DictionaryLoader.getInstance();
        this.dictLoader.ensureLoaded(context);
        this.segmenter = new ChineseSegmenter(dictLoader);
    }

    /**
     * Enrich text regions with Chinese segmentation data.
     * Call from background thread.
     */
    public void enrichRegions(List<TextRegion> regions) {
        if (regions == null) {
            return;
        }

        for (TextRegion region : regions) {
            if (region.containsChinese) {
                enrichRegion(region);
            }
        }
    }

    private void enrichRegion(TextRegion region) {
        // Segment the text into words
        List<SegmentedWord> words = segmenter.segment(region.text);

        // Calculate per-character bounds
        List<Rect> charBounds = CharacterBoundsCalculator.estimateCharacterBounds(
                region.text, region.bounds);

        // Set the enriched data
        region.setChineseData(words, charBounds);
    }

    /**
     * Check if dictionary is loaded
     */
    public boolean isDictionaryLoaded() {
        return dictLoader.isLoaded();
    }
}
