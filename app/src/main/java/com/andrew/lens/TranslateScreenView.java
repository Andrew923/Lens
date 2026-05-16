package com.andrew.lens;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Matrix;
import android.graphics.Paint;
import android.graphics.Rect;
import android.graphics.RectF;
import android.view.Gravity;
import android.view.MotionEvent;
import android.widget.FrameLayout;
import android.widget.TextView;

import java.util.ArrayList;
import java.util.List;

/**
 * Screen 1 of the Lens pager: the frozen-screenshot translation view.
 *
 * Draws the captured screenshot as a static background and paints OCR boxes
 * + pinyin on top of it. All OCR coordinates are in raw screenshot-bitmap
 * pixel space; this view owns the single bitmap-&gt;view transform (a
 * letterboxed fit), so boxes align 1:1 with the frozen image regardless of
 * keyboard / IME / status-bar state on the live screen.
 *
 * Tapping a Chinese word shows a {@link DefinitionPopupView}.
 */
public class TranslateScreenView extends FrameLayout {

    private static final int DIM_COLOR = 0x66000000; // light dim over screenshot for contrast
    private static final int TEXT_BOX_SELECTED_COLOR = 0x8000FF00;
    private static final int CHINESE_BOX_COLOR = 0x40FFAA00;
    private static final int CHINESE_BOX_STROKE_COLOR = 0xFFFFAA00;
    private static final int OCR_BOX_COLOR = 0x4000AAFF;
    private static final int OCR_BOX_STROKE_COLOR = 0xFF00AAFF;

    private static final int PINYIN_MARGIN = 4;
    private static final int TAP_SLOP = 24; // px of movement still treated as a tap

    private List<TextRegion> textRegions = new ArrayList<>();
    private Bitmap screenshot;

    // bitmap -> view transform (letterbox fit), recomputed on size/bitmap change
    private final Matrix bmpToView = new Matrix();
    private float scale = 1f;
    private float dx = 0f;
    private float dy = 0f;

    private Paint bitmapPaint;
    private Paint dimPaint;
    private Paint selectedPaint;
    private Paint chineseBoxPaint;
    private Paint chineseStrokePaint;
    private Paint ocrBoxPaint;
    private Paint ocrStrokePaint;
    private Paint pinyinPaint;
    private Paint pinyinBackgroundPaint;

    private TextView statusText;
    private DefinitionPopupView definitionPopup;

    private boolean pinyinToneMarks = true;
    private boolean dimEnabled = true;

    private float downX, downY;

    public TranslateScreenView(Context context) {
        super(context);
        init();
    }

    private void init() {
        bitmapPaint = new Paint(Paint.FILTER_BITMAP_FLAG);

        dimPaint = new Paint();
        dimPaint.setColor(DIM_COLOR);
        dimPaint.setStyle(Paint.Style.FILL);

        selectedPaint = new Paint();
        selectedPaint.setColor(TEXT_BOX_SELECTED_COLOR);
        selectedPaint.setStyle(Paint.Style.FILL);

        chineseBoxPaint = new Paint();
        chineseBoxPaint.setColor(CHINESE_BOX_COLOR);
        chineseBoxPaint.setStyle(Paint.Style.FILL);

        chineseStrokePaint = new Paint();
        chineseStrokePaint.setColor(CHINESE_BOX_STROKE_COLOR);
        chineseStrokePaint.setStyle(Paint.Style.STROKE);
        chineseStrokePaint.setStrokeWidth(3f);

        ocrBoxPaint = new Paint();
        ocrBoxPaint.setColor(OCR_BOX_COLOR);
        ocrBoxPaint.setStyle(Paint.Style.FILL);

        ocrStrokePaint = new Paint();
        ocrStrokePaint.setColor(OCR_BOX_STROKE_COLOR);
        ocrStrokePaint.setStyle(Paint.Style.STROKE);
        ocrStrokePaint.setStrokeWidth(3f);

        pinyinPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
        pinyinPaint.setColor(Color.WHITE);
        pinyinPaint.setTextAlign(Paint.Align.CENTER);

        pinyinBackgroundPaint = new Paint();
        pinyinBackgroundPaint.setColor(0xDD333333);
        pinyinBackgroundPaint.setStyle(Paint.Style.FILL);

        statusText = new TextView(getContext());
        statusText.setText("LENS - Scanning...");
        statusText.setTextColor(Color.WHITE);
        statusText.setTextSize(14);
        statusText.setPadding(24, 8, 24, 8);
        statusText.setBackgroundColor(0xCC000000);
        LayoutParams statusParams = new LayoutParams(
                LayoutParams.WRAP_CONTENT, LayoutParams.WRAP_CONTENT);
        statusParams.gravity = Gravity.TOP | Gravity.CENTER_HORIZONTAL;
        statusParams.topMargin = 8;
        addView(statusText, statusParams);

        definitionPopup = new DefinitionPopupView(getContext());
        definitionPopup.setVisibility(GONE);
        definitionPopup.setOnDismissListener(() -> {
            for (TextRegion r : textRegions) {
                r.isSelected = false;
            }
            invalidate();
        });
        addView(definitionPopup, new LayoutParams(
                LayoutParams.WRAP_CONTENT, LayoutParams.WRAP_CONTENT));

        pinyinToneMarks = Prefs.pinyinToneMarks(getContext());
        dimEnabled = Prefs.dimScreenshot(getContext());

        setWillNotDraw(false);
    }

    /** Set the frozen screenshot to draw as the background. */
    public void setScreenshot(Bitmap bitmap) {
        this.screenshot = bitmap;
        recomputeTransform();
        invalidate();
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
        pinyinToneMarks = Prefs.pinyinToneMarks(getContext());
        dimEnabled = Prefs.dimScreenshot(getContext());

        if (regions == null || regions.isEmpty()) {
            statusText.setText("LENS - No text found");
            this.textRegions = new ArrayList<>();
            invalidate();
            return;
        }

        this.textRegions = regions;

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
    protected void onSizeChanged(int w, int h, int oldw, int oldh) {
        super.onSizeChanged(w, h, oldw, oldh);
        recomputeTransform();
    }

    /** Compute the aspect-preserving (letterbox) bitmap -&gt; view transform. */
    private void recomputeTransform() {
        if (screenshot == null || screenshot.isRecycled()
                || getWidth() == 0 || getHeight() == 0) {
            return;
        }
        float bmpW = screenshot.getWidth();
        float bmpH = screenshot.getHeight();
        scale = Math.min(getWidth() / bmpW, getHeight() / bmpH);
        float drawW = bmpW * scale;
        dx = (getWidth() - drawW) / 2f;
        dy = 0f; // top-align so reading order matches
        bmpToView.reset();
        bmpToView.setScale(scale, scale);
        bmpToView.postTranslate(dx, dy);
    }

    private RectF mapToView(Rect bmpRect) {
        RectF r = new RectF(bmpRect);
        bmpToView.mapRect(r);
        return r;
    }

    @Override
    protected void onDraw(Canvas canvas) {
        super.onDraw(canvas);

        if (screenshot != null && !screenshot.isRecycled()) {
            canvas.drawBitmap(screenshot, bmpToView, bitmapPaint);
            // Dim only the rendered image area for annotation contrast.
            if (dimEnabled) {
                canvas.drawRect(dx, dy,
                        dx + screenshot.getWidth() * scale,
                        dy + screenshot.getHeight() * scale,
                        dimPaint);
            }
        }

        for (TextRegion region : textRegions) {
            RectF rectF = mapToView(region.bounds);

            if (region.containsChinese) {
                if (region.isSelected) {
                    canvas.drawRoundRect(rectF, 8, 8, selectedPaint);
                } else {
                    canvas.drawRoundRect(rectF, 8, 8, chineseBoxPaint);
                }
                canvas.drawRoundRect(rectF, 8, 8, chineseStrokePaint);
                drawPinyinAnnotations(canvas, region);
            } else {
                if (region.isSelected) {
                    canvas.drawRoundRect(rectF, 8, 8, selectedPaint);
                } else {
                    canvas.drawRoundRect(rectF, 8, 8, ocrBoxPaint);
                }
                canvas.drawRoundRect(rectF, 8, 8, ocrStrokePaint);
            }
        }
    }

    private void drawPinyinAnnotations(Canvas canvas, TextRegion region) {
        if (region.segmentedWords == null || region.characterBounds == null) {
            return;
        }

        float pinyinSize = calculatePinyinSize(region) * scale;
        pinyinPaint.setTextSize(pinyinSize);

        int charIndex = 0;
        for (SegmentedWord word : region.segmentedWords) {
            if (word.entry != null && word.text.length() > 0) {
                Rect wordBounds = CharacterBoundsCalculator.getWordBounds(
                        region.characterBounds,
                        charIndex,
                        charIndex + word.text.length());

                if (wordBounds != null) {
                    RectF vw = mapToView(wordBounds);
                    String pinyin = pinyinToneMarks
                            ? word.entry.pinyinDisplay : word.entry.pinyin;

                    float centerX = vw.centerX();
                    float y = vw.top - PINYIN_MARGIN;

                    float pinyinWidth = pinyinPaint.measureText(pinyin);
                    float bgLeft = centerX - pinyinWidth / 2 - 4;
                    float bgRight = centerX + pinyinWidth / 2 + 4;
                    float bgTop = y - pinyinSize - 2;
                    float bgBottom = y + 4;

                    canvas.drawRoundRect(bgLeft, bgTop, bgRight, bgBottom,
                            4, 4, pinyinBackgroundPaint);
                    canvas.drawText(pinyin, centerX, y, pinyinPaint);
                }
            }
            charIndex += word.text.length();
        }
    }

    private float calculatePinyinSize(TextRegion region) {
        // Pinyin ~40% of character height (in bitmap space; scaled by caller).
        float charHeight = region.bounds.height();
        return Math.max(14f, Math.min(charHeight * 0.4f, 28f));
    }

    @Override
    public boolean onTouchEvent(MotionEvent event) {
        switch (event.getAction()) {
            case MotionEvent.ACTION_DOWN:
                downX = event.getX();
                downY = event.getY();
                return true;
            case MotionEvent.ACTION_UP:
                float x = event.getX();
                float y = event.getY();
                // Treat as a tap only if the pointer barely moved; otherwise
                // it was a swipe that ViewPager2 handles (or a cancelled drag).
                if (Math.abs(x - downX) > TAP_SLOP || Math.abs(y - downY) > TAP_SLOP) {
                    return true;
                }
                handleTap(x, y);
                return true;
            default:
                return true;
        }
    }

    private void handleTap(float viewX, float viewY) {
        if (definitionPopup.isShowing()) {
            definitionPopup.dismiss();
            return;
        }

        // Convert the touch point into bitmap space for hit-testing.
        if (scale == 0f) return;
        int bmpX = Math.round((viewX - dx) / scale);
        int bmpY = Math.round((viewY - dy) / scale);

        TextRegion tappedRegion = null;
        for (TextRegion region : textRegions) {
            if (region.bounds.contains(bmpX, bmpY)) {
                tappedRegion = region;
                break;
            }
        }

        if (tappedRegion != null && tappedRegion.containsChinese) {
            handleChineseTextTap(tappedRegion, bmpX);
        }
        // Tapping empty space no longer dismisses; use the header ✕ to close.
    }

    private void handleChineseTextTap(TextRegion region, int bmpX) {
        SegmentedWord tappedWord = findTappedWord(region, bmpX);
        if (tappedWord != null && tappedWord.entry != null) {
            // Position popup in view space: centered over the tapped word,
            // anchored above the region.
            RectF regionView = mapToView(region.bounds);
            int popupX = Math.round((bmpX * scale) + dx);
            showDefinitionPopup(tappedWord.entry, popupX, Math.round(regionView.top));

            for (TextRegion r : textRegions) {
                r.isSelected = false;
            }
            region.isSelected = true;
            invalidate();
        }
    }

    private SegmentedWord findTappedWord(TextRegion region, int bmpX) {
        if (region.segmentedWords == null || region.characterBounds == null) {
            return null;
        }

        int charIndex = 0;
        for (SegmentedWord word : region.segmentedWords) {
            Rect wordBounds = CharacterBoundsCalculator.getWordBounds(
                    region.characterBounds,
                    charIndex,
                    charIndex + word.text.length());

            if (wordBounds != null && bmpX >= wordBounds.left && bmpX <= wordBounds.right) {
                return word;
            }
            charIndex += word.text.length();
        }

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
}
