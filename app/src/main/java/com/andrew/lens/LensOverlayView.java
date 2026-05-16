package com.andrew.lens;

import android.content.ClipData;
import android.content.ClipboardManager;
import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.Rect;
import android.graphics.RectF;
import android.view.Gravity;
import android.view.MotionEvent;
import android.widget.FrameLayout;
import android.widget.TextView;
import android.widget.Toast;

import java.util.ArrayList;
import java.util.List;

public class LensOverlayView extends FrameLayout {

    private static final int OVERLAY_COLOR = 0x80000000; // Semi-transparent black
    private static final int TEXT_BOX_COLOR = 0x4000FF00; // Semi-transparent green
    private static final int TEXT_BOX_SELECTED_COLOR = 0x8000FF00; // More opaque green
    private static final int TEXT_BOX_STROKE_COLOR = 0xFF00FF00; // Solid green stroke
    private static final int CHINESE_BOX_COLOR = 0x40FFAA00; // Semi-transparent orange for Chinese
    private static final int CHINESE_BOX_STROKE_COLOR = 0xFFFFAA00; // Orange stroke for Chinese
    private static final int OCR_BOX_COLOR = 0x4000AAFF; // Semi-transparent cyan for OCR
    private static final int OCR_BOX_STROKE_COLOR = 0xFF00AAFF; // Cyan stroke for OCR

    private List<TextRegion> textRegions = new ArrayList<>();
    private Paint boxPaint;
    private Paint strokePaint;
    private Paint selectedPaint;
    private Paint chineseBoxPaint;
    private Paint chineseStrokePaint;
    private Paint ocrBoxPaint;
    private Paint ocrStrokePaint;
    private Paint pinyinPaint;
    private Paint pinyinBackgroundPaint;
    private TextView statusText;
    private OnDismissListener dismissListener;
    private DefinitionPopupView definitionPopup;

    private static final int PINYIN_MARGIN = 4;

    public interface OnDismissListener {
        void onDismiss();
    }

    public LensOverlayView(Context context) {
        super(context);
        init();
    }

    private void init() {
        // Set semi-transparent background
        setBackgroundColor(OVERLAY_COLOR);

        // Initialize paints for non-Chinese text
        boxPaint = new Paint();
        boxPaint.setColor(TEXT_BOX_COLOR);
        boxPaint.setStyle(Paint.Style.FILL);

        strokePaint = new Paint();
        strokePaint.setColor(TEXT_BOX_STROKE_COLOR);
        strokePaint.setStyle(Paint.Style.STROKE);
        strokePaint.setStrokeWidth(3f);

        selectedPaint = new Paint();
        selectedPaint.setColor(TEXT_BOX_SELECTED_COLOR);
        selectedPaint.setStyle(Paint.Style.FILL);

        // Initialize paints for Chinese text (orange to distinguish)
        chineseBoxPaint = new Paint();
        chineseBoxPaint.setColor(CHINESE_BOX_COLOR);
        chineseBoxPaint.setStyle(Paint.Style.FILL);

        chineseStrokePaint = new Paint();
        chineseStrokePaint.setColor(CHINESE_BOX_STROKE_COLOR);
        chineseStrokePaint.setStyle(Paint.Style.STROKE);
        chineseStrokePaint.setStrokeWidth(3f);

        // Initialize paints for OCR text (cyan to distinguish)
        ocrBoxPaint = new Paint();
        ocrBoxPaint.setColor(OCR_BOX_COLOR);
        ocrBoxPaint.setStyle(Paint.Style.FILL);

        ocrStrokePaint = new Paint();
        ocrStrokePaint.setColor(OCR_BOX_STROKE_COLOR);
        ocrStrokePaint.setStyle(Paint.Style.STROKE);
        ocrStrokePaint.setStrokeWidth(3f);

        // Initialize paints for pinyin
        pinyinPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
        pinyinPaint.setColor(Color.WHITE);
        pinyinPaint.setTextAlign(Paint.Align.CENTER);

        pinyinBackgroundPaint = new Paint();
        pinyinBackgroundPaint.setColor(0xDD333333);
        pinyinBackgroundPaint.setStyle(Paint.Style.FILL);

        // Add status indicator at top
        statusText = new TextView(getContext());
        statusText.setText("LENS ACTIVE");
        statusText.setTextColor(Color.WHITE);
        statusText.setTextSize(18);
        statusText.setPadding(32, 16, 32, 16);
        statusText.setBackgroundColor(0xCC000000);

        LayoutParams statusParams = new LayoutParams(
                LayoutParams.WRAP_CONTENT,
                LayoutParams.WRAP_CONTENT
        );
        statusParams.gravity = Gravity.TOP | Gravity.CENTER_HORIZONTAL;
        statusParams.topMargin = 100;
        addView(statusText, statusParams);

        // Add definition popup (initially hidden)
        definitionPopup = new DefinitionPopupView(getContext());
        definitionPopup.setVisibility(GONE);
        definitionPopup.setOnDismissListener(() -> {
            // Deselect all when popup closes
            for (TextRegion r : textRegions) {
                r.isSelected = false;
            }
            invalidate();
        });
        LayoutParams popupParams = new LayoutParams(
                LayoutParams.WRAP_CONTENT,
                LayoutParams.WRAP_CONTENT
        );
        addView(definitionPopup, popupParams);

        // Enable drawing
        setWillNotDraw(false);
    }

    public void setOnDismissListener(OnDismissListener listener) {
        this.dismissListener = listener;
    }

    public void showLoading() {
        statusText.setText("LENS - Scanning...");
        this.textRegions = new ArrayList<>();
        invalidate();
    }

    public void showError(String message) {
        statusText.setText("LENS - " + message);
        this.textRegions = new ArrayList<>();
        invalidate();
    }

    public void displayTextRegions(List<TextRegion> regions) {
        if (regions == null || regions.isEmpty()) {
            statusText.setText("LENS - No text found");
            this.textRegions = new ArrayList<>();
            invalidate();
            return;
        }

        this.textRegions = regions;

        // Count Chinese regions
        int chineseCount = 0;
        for (TextRegion r : regions) {
            if (r.containsChinese) chineseCount++;
        }

        if (chineseCount > 0) {
            statusText.setText("LENS - " + regions.size() + " regions (" + chineseCount + " Chinese)");
        } else {
            statusText.setText("LENS - " + regions.size() + " text regions");
        }
        invalidate();
    }

    @Override
    protected void onDraw(Canvas canvas) {
        super.onDraw(canvas);

        for (TextRegion region : textRegions) {
            RectF rectF = new RectF(region.bounds);

            if (region.containsChinese) {
                // Draw Chinese text box (orange)
                if (region.isSelected) {
                    canvas.drawRoundRect(rectF, 8, 8, selectedPaint);
                } else {
                    canvas.drawRoundRect(rectF, 8, 8, chineseBoxPaint);
                }
                canvas.drawRoundRect(rectF, 8, 8, chineseStrokePaint);

                // Draw pinyin annotations above Chinese text
                drawPinyinAnnotations(canvas, region);
            } else {
                // Draw regular text box (green)
                if (region.isSelected) {
                    canvas.drawRoundRect(rectF, 8, 8, selectedPaint);
                } else {
                    canvas.drawRoundRect(rectF, 8, 8, boxPaint);
                }
                canvas.drawRoundRect(rectF, 8, 8, strokePaint);
            }
        }
    }

    private void drawPinyinAnnotations(Canvas canvas, TextRegion region) {
        if (region.segmentedWords == null || region.characterBounds == null) {
            return;
        }

        // Calculate pinyin size based on text height
        float pinyinSize = calculatePinyinSize(region);
        pinyinPaint.setTextSize(pinyinSize);

        int charIndex = 0;
        for (SegmentedWord word : region.segmentedWords) {
            if (word.entry != null && word.text.length() > 0) {
                // Get bounds for this word
                Rect wordBounds = CharacterBoundsCalculator.getWordBounds(
                        region.characterBounds,
                        charIndex,
                        charIndex + word.text.length()
                );

                if (wordBounds != null) {
                    String pinyin = word.entry.pinyinDisplay;

                    // Calculate position (centered above word)
                    float centerX = wordBounds.centerX();
                    float y = wordBounds.top - PINYIN_MARGIN;

                    // Draw background for readability
                    float pinyinWidth = pinyinPaint.measureText(pinyin);
                    float bgLeft = centerX - pinyinWidth / 2 - 4;
                    float bgRight = centerX + pinyinWidth / 2 + 4;
                    float bgTop = y - pinyinSize - 2;
                    float bgBottom = y + 4;

                    canvas.drawRoundRect(bgLeft, bgTop, bgRight, bgBottom,
                            4, 4, pinyinBackgroundPaint);

                    // Draw pinyin text
                    canvas.drawText(pinyin, centerX, y, pinyinPaint);
                }
            }

            charIndex += word.text.length();
        }
    }

    private float calculatePinyinSize(TextRegion region) {
        // Pinyin should be about 40% of character height, with min/max limits
        float charHeight = region.bounds.height();
        return Math.max(14f, Math.min(charHeight * 0.4f, 28f));
    }

    @Override
    public boolean onTouchEvent(MotionEvent event) {
        if (event.getAction() == MotionEvent.ACTION_UP) {
            float x = event.getX();
            float y = event.getY();

            // If popup is showing and tap is outside it, dismiss popup
            if (definitionPopup.isShowing()) {
                definitionPopup.dismiss();
                return true;
            }

            // Check if tap is on any text region
            TextRegion tappedRegion = null;
            for (TextRegion region : textRegions) {
                if (region.bounds.contains((int) x, (int) y)) {
                    tappedRegion = region;
                    break;
                }
            }

            if (tappedRegion != null) {
                handleTextRegionTap(tappedRegion, (int) x, (int) y);
            } else {
                // Tap outside text regions - dismiss
                if (dismissListener != null) {
                    dismissListener.onDismiss();
                }
            }

            return true;
        }
        return true;
    }

    private void handleTextRegionTap(TextRegion region, int tapX, int tapY) {
        if (region.containsChinese) {
            handleChineseTextTap(region, tapX, tapY);
        } else {
            handleRegularTextTap(region);
        }
    }

    private void handleChineseTextTap(TextRegion region, int tapX, int tapY) {
        // Find which word was tapped based on character bounds
        SegmentedWord tappedWord = findTappedWord(region, tapX);

        if (tappedWord != null && tappedWord.entry != null) {
            // Show definition popup for this word
            showDefinitionPopup(tappedWord.entry, tapX, region.bounds.top);

            // Select this region
            for (TextRegion r : textRegions) {
                r.isSelected = false;
            }
            region.isSelected = true;
            invalidate();
        } else {
            // No definition available - fall back to copy behavior
            handleRegularTextTap(region);
        }
    }

    private SegmentedWord findTappedWord(TextRegion region, int tapX) {
        if (region.segmentedWords == null || region.characterBounds == null) {
            return null;
        }

        int charIndex = 0;
        for (SegmentedWord word : region.segmentedWords) {
            Rect wordBounds = CharacterBoundsCalculator.getWordBounds(
                    region.characterBounds,
                    charIndex,
                    charIndex + word.text.length()
            );

            if (wordBounds != null && tapX >= wordBounds.left && tapX <= wordBounds.right) {
                return word;
            }

            charIndex += word.text.length();
        }

        // If no specific word found, return first word with definition
        for (SegmentedWord word : region.segmentedWords) {
            if (word.entry != null) {
                return word;
            }
        }

        return null;
    }

    private void showDefinitionPopup(DictionaryEntry entry, int x, int y) {
        definitionPopup.showEntry(entry, x, y);
    }

    private void handleRegularTextTap(TextRegion region) {
        if (region.isSelected) {
            // Second tap - copy to clipboard
            copyToClipboard(region.text);
            region.isSelected = false;
            Toast.makeText(getContext(), "Copied: " + truncateText(region.text, 50),
                    Toast.LENGTH_SHORT).show();
        } else {
            // First tap - select
            // Deselect others
            for (TextRegion r : textRegions) {
                r.isSelected = false;
            }
            region.isSelected = true;
            Toast.makeText(getContext(), "Tap again to copy",
                    Toast.LENGTH_SHORT).show();
        }
        invalidate();
    }

    private void copyToClipboard(String text) {
        ClipboardManager clipboard = (ClipboardManager)
                getContext().getSystemService(Context.CLIPBOARD_SERVICE);
        ClipData clip = ClipData.newPlainText("Lens Selection", text);
        clipboard.setPrimaryClip(clip);
    }

    private String truncateText(String text, int maxLen) {
        if (text.length() <= maxLen) return text;
        return text.substring(0, maxLen) + "...";
    }
}
