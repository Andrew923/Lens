package com.andrew.lens;

import android.content.ClipData;
import android.content.ClipboardManager;
import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Matrix;
import android.graphics.Paint;
import android.graphics.Color;
import android.view.Gravity;
import android.view.MotionEvent;
import android.widget.FrameLayout;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.TextView;
import android.widget.Toast;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;

/**
 * Screen 2 of the Lens pager: the copy view.
 *
 * Recognized text is concatenated in reading order into a single selectable
 * {@link TextView}, so Android's native selection handles + floating
 * Copy/Share/Select-All toolbar work across the whole passage (native
 * selection cannot span multiple TextViews, hence one TextView). The frozen
 * screenshot is drawn dimmed behind the text for context. A guaranteed
 * "Copy all" button is provided as a fallback if the floating toolbar does
 * not render inside the assist window.
 */
public class CopyScreenView extends FrameLayout {

    private static final int BACKDROP_DIM = 0xCC000000; // heavy dim; text is the focus

    private Bitmap screenshot;
    private final Matrix bmpToView = new Matrix();
    private float scale = 1f, dx = 0f, dy = 0f;

    private final Paint bitmapPaint = new Paint(Paint.FILTER_BITMAP_FLAG);
    private final Paint dimPaint = new Paint();

    private TextView selectableText;

    public CopyScreenView(Context context) {
        super(context);
        init();
    }

    private void init() {
        dimPaint.setColor(BACKDROP_DIM);
        dimPaint.setStyle(Paint.Style.FILL);
        setWillNotDraw(false);

        LinearLayout column = new LinearLayout(getContext());
        column.setOrientation(LinearLayout.VERTICAL);

        TextView copyAll = new TextView(getContext());
        copyAll.setText("Copy all");
        copyAll.setTextColor(Color.WHITE);
        copyAll.setTextSize(15);
        copyAll.setGravity(Gravity.CENTER);
        copyAll.setPadding(24, 16, 24, 16);
        copyAll.setBackgroundColor(0xFF2196F3);
        copyAll.setOnClickListener(v -> copyAll());
        LinearLayout.LayoutParams capParams = new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT);
        capParams.bottomMargin = 8;
        column.addView(copyAll, capParams);

        ScrollView scroll = new ScrollView(getContext());
        scroll.setLayoutParams(new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT, 0, 1f));

        selectableText = new TextView(getContext());
        selectableText.setTextColor(Color.WHITE);
        selectableText.setTextSize(18);
        selectableText.setLineSpacing(10f, 1f);
        selectableText.setPadding(32, 24, 32, 48);
        selectableText.setTextIsSelectable(true);
        selectableText.setText("Scanning...", TextView.BufferType.SPANNABLE);
        // Keep ViewPager2 from stealing the horizontal selection drag.
        selectableText.setOnTouchListener((v, ev) -> {
            if (ev.getActionMasked() == MotionEvent.ACTION_DOWN) {
                getParent().requestDisallowInterceptTouchEvent(true);
            }
            return false; // let the TextView handle selection
        });
        scroll.addView(selectableText);

        column.addView(scroll);

        addView(column, new LayoutParams(
                LayoutParams.MATCH_PARENT, LayoutParams.MATCH_PARENT));
    }

    /** Set the frozen screenshot drawn (heavily dimmed) behind the text. */
    public void setScreenshot(Bitmap bitmap) {
        this.screenshot = bitmap;
        recomputeTransform();
        invalidate();
    }

    public void showLoading() {
        if (selectableText != null) {
            selectableText.setText("Scanning...", TextView.BufferType.SPANNABLE);
        }
    }

    public void displayTextRegions(List<TextRegion> regions) {
        if (regions == null || regions.isEmpty()) {
            selectableText.setText("No text found", TextView.BufferType.SPANNABLE);
            return;
        }

        List<TextRegion> ordered = new ArrayList<>(regions);
        Collections.sort(ordered, new Comparator<TextRegion>() {
            @Override
            public int compare(TextRegion a, TextRegion b) {
                int dt = Integer.compare(a.bounds.top, b.bounds.top);
                if (dt != 0) return dt;
                return Integer.compare(a.bounds.left, b.bounds.left);
            }
        });

        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < ordered.size(); i++) {
            if (i > 0) sb.append('\n');
            sb.append(ordered.get(i).text);
        }
        selectableText.setText(sb.toString(), TextView.BufferType.SPANNABLE);
    }

    private void copyAll() {
        CharSequence text = selectableText.getText();
        if (text == null || text.length() == 0) return;
        ClipboardManager clipboard = (ClipboardManager)
                getContext().getSystemService(Context.CLIPBOARD_SERVICE);
        clipboard.setPrimaryClip(ClipData.newPlainText("Lens Text", text));
        Toast.makeText(getContext(), "Copied all text", Toast.LENGTH_SHORT).show();
    }

    @Override
    protected void onSizeChanged(int w, int h, int oldw, int oldh) {
        super.onSizeChanged(w, h, oldw, oldh);
        recomputeTransform();
    }

    private void recomputeTransform() {
        if (screenshot == null || screenshot.isRecycled()
                || getWidth() == 0 || getHeight() == 0) {
            return;
        }
        float bmpW = screenshot.getWidth();
        float bmpH = screenshot.getHeight();
        scale = Math.min(getWidth() / bmpW, getHeight() / bmpH);
        dx = (getWidth() - bmpW * scale) / 2f;
        dy = 0f;
        bmpToView.reset();
        bmpToView.setScale(scale, scale);
        bmpToView.postTranslate(dx, dy);
    }

    @Override
    protected void onDraw(Canvas canvas) {
        super.onDraw(canvas);
        if (screenshot != null && !screenshot.isRecycled()) {
            canvas.drawBitmap(screenshot, bmpToView, bitmapPaint);
        }
        canvas.drawRect(0, 0, getWidth(), getHeight(), dimPaint);
    }
}
