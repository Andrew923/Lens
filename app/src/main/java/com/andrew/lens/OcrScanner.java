package com.andrew.lens;

import android.graphics.Bitmap;
import android.graphics.Rect;
import android.util.Log;

import com.google.mlkit.vision.common.InputImage;
import com.google.mlkit.vision.text.Text;
import com.google.mlkit.vision.text.TextRecognition;
import com.google.mlkit.vision.text.TextRecognizer;
import com.google.mlkit.vision.text.chinese.ChineseTextRecognizerOptions;

import java.util.ArrayList;
import java.util.List;

public class OcrScanner {
    private static final String TAG = "OcrScanner";

    public interface OcrCallback {
        void onOcrComplete(List<TextRegion> regions);
        void onOcrError(Exception e);
    }

    private final TextRecognizer chineseRecognizer;

    // Minimum confidence threshold (0.0 - 1.0)
    private static final float MIN_CONFIDENCE = 0.5f;
    // Minimum text length to accept
    private static final int MIN_TEXT_LENGTH = 1;
    // Minimum bounding box area (pixels)
    private static final int MIN_BOX_AREA = 100;

    public OcrScanner() {
        chineseRecognizer = TextRecognition.getClient(
                new ChineseTextRecognizerOptions.Builder().build());
    }

    /**
     * Scan a bitmap for text using OCR.
     *
     * Returned {@link TextRegion} bounds are in raw screenshot-bitmap pixel
     * space (origin = top-left of the captured bitmap). Mapping into view
     * space is the responsibility of the view layer.
     *
     * @param bitmap The screenshot to scan
     * @param callback Callback for results
     */
    public void scanBitmap(Bitmap bitmap, OcrCallback callback) {
        if (bitmap == null) {
            callback.onOcrComplete(new ArrayList<>());
            return;
        }

        InputImage image = InputImage.fromBitmap(bitmap, 0);

        chineseRecognizer.process(image)
                .addOnSuccessListener(text -> {
                    List<TextRegion> regions = convertToTextRegions(text);
                    callback.onOcrComplete(regions);
                })
                .addOnFailureListener(e -> {
                    callback.onOcrError(e);
                });
    }

    private List<TextRegion> convertToTextRegions(Text text) {
        List<TextRegion> regions = new ArrayList<>();

        for (Text.TextBlock block : text.getTextBlocks()) {
            for (Text.Line line : block.getLines()) {
                // Check confidence at element level (more granular)
                // Note: Chinese recognizer may not always provide confidence scores
                boolean hasConfidentElement = false;
                Float lineConfidence = line.getConfidence();
                if (lineConfidence == null || lineConfidence >= MIN_CONFIDENCE) {
                    hasConfidentElement = true;
                }

                if (!hasConfidentElement) {
                    continue;
                }

                String lineText = line.getText();
                Rect bounds = line.getBoundingBox();

                // Filter out invalid results
                if (bounds == null || lineText == null) {
                    continue;
                }

                lineText = lineText.trim();

                // Filter by text length
                if (lineText.length() < MIN_TEXT_LENGTH) {
                    continue;
                }

                // Filter by bounding box area
                int area = bounds.width() * bounds.height();
                if (area < MIN_BOX_AREA) {
                    continue;
                }

                // Filter out likely false positives:
                // - Single punctuation marks
                // - Only whitespace
                // - Common OCR artifacts
                if (isLikelyFalsePositive(lineText)) {
                    continue;
                }

                Log.d(TAG, "OCR: '" + lineText.substring(0, Math.min(20, lineText.length())) + "' y=" + bounds.top);

                TextRegion region = new TextRegion(lineText, new Rect(bounds), "OCR");
                regions.add(region);
            }
        }

        return regions;
    }

    private boolean isLikelyFalsePositive(String text) {
        if (text.isEmpty()) {
            return true;
        }

        // Single character that's just punctuation or symbol
        if (text.length() == 1) {
            char c = text.charAt(0);
            if (!Character.isLetterOrDigit(c) && !ChineseTextDetector.isChineseCharacter(c)) {
                return true;
            }
        }

        // Only punctuation/symbols
        boolean hasLetterOrDigit = false;
        for (int i = 0; i < text.length(); i++) {
            char c = text.charAt(i);
            if (Character.isLetterOrDigit(c) || ChineseTextDetector.isChineseCharacter(c)) {
                hasLetterOrDigit = true;
                break;
            }
        }
        if (!hasLetterOrDigit) {
            return true;
        }

        return false;
    }

    /**
     * Release resources when done
     */
    public void close() {
        chineseRecognizer.close();
    }
}
