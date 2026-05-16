package com.andrew.lens;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.Color;
import android.view.Gravity;
import android.widget.LinearLayout;
import android.widget.TextView;

import androidx.viewpager2.widget.ViewPager2;

import java.util.List;

/**
 * Root content view for the Lens assist session: a header (page label, dot
 * indicator, close affordance) over a horizontally swipeable {@link ViewPager2}
 * with two screens — Translate (page 0) and Copy (page 1).
 *
 * Owns the single frozen screenshot bitmap and forwards it plus OCR results to
 * both screens. The bitmap is recycled exactly once via {@link #recycleScreenshot()}.
 */
public class LensPagerView extends LinearLayout {

    public interface OnDismissListener {
        void onDismiss();
    }

    private static final int DOT_ON = 0xFFFFFFFF;
    private static final int DOT_OFF = 0x66FFFFFF;

    private final TranslateScreenView translateScreen;
    private final CopyScreenView copyScreen;
    private final ViewPager2 pager;
    private final TextView label;
    private final TextView dot0;
    private final TextView dot1;

    private Bitmap screenshot;
    private boolean recycled;
    private OnDismissListener dismissListener;

    public LensPagerView(Context context) {
        super(context);
        setOrientation(VERTICAL);
        setBackgroundColor(Color.BLACK);

        // --- Header ---
        LinearLayout header = new LinearLayout(context);
        header.setOrientation(HORIZONTAL);
        header.setGravity(Gravity.CENTER_VERTICAL);
        header.setBackgroundColor(0xFF000000);
        header.setPadding(32, 16, 32, 16);

        label = new TextView(context);
        label.setText("Translate");
        label.setTextColor(Color.WHITE);
        label.setTextSize(18);
        header.addView(label, new LinearLayout.LayoutParams(0,
                LinearLayout.LayoutParams.WRAP_CONTENT, 1f));

        dot0 = makeDot(context, true);
        dot1 = makeDot(context, false);
        header.addView(dot0);
        header.addView(dot1);

        TextView close = new TextView(context);
        close.setText("  ✕"); // ✕
        close.setTextColor(Color.WHITE);
        close.setTextSize(20);
        close.setPadding(32, 0, 0, 0);
        close.setOnClickListener(v -> {
            if (dismissListener != null) dismissListener.onDismiss();
        });
        header.addView(close);

        addView(header, new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT));

        // --- Pager ---
        translateScreen = new TranslateScreenView(context);
        copyScreen = new CopyScreenView(context);

        pager = new ViewPager2(context);
        pager.setAdapter(new LensPagerAdapter(translateScreen, copyScreen));
        pager.setOffscreenPageLimit(1); // keep both screens alive / stateful
        pager.registerOnPageChangeCallback(new ViewPager2.OnPageChangeCallback() {
            @Override
            public void onPageSelected(int position) {
                updateHeader(position);
            }
        });
        addView(pager, new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT, 0, 1f));

        if (Prefs.defaultScreenCopy(context)) {
            pager.setCurrentItem(1, false);
            updateHeader(1);
        }
    }

    private TextView makeDot(Context context, boolean on) {
        TextView dot = new TextView(context);
        dot.setText("●"); // ●
        dot.setTextSize(10);
        dot.setTextColor(on ? DOT_ON : DOT_OFF);
        dot.setPadding(6, 0, 6, 0);
        return dot;
    }

    private void updateHeader(int position) {
        label.setText(position == 0 ? "Translate" : "Copy");
        dot0.setTextColor(position == 0 ? DOT_ON : DOT_OFF);
        dot1.setTextColor(position == 1 ? DOT_ON : DOT_OFF);
    }

    public void setOnDismissListener(OnDismissListener listener) {
        this.dismissListener = listener;
    }

    public void setScreenshot(Bitmap bitmap) {
        this.screenshot = bitmap;
        this.recycled = false;
        translateScreen.setScreenshot(bitmap);
        copyScreen.setScreenshot(bitmap);
    }

    public void showLoading() {
        translateScreen.showLoading();
        copyScreen.showLoading();
    }

    public void showError(String message) {
        translateScreen.showError(message);
        copyScreen.displayTextRegions(null);
    }

    public void displayTextRegions(List<TextRegion> regions) {
        translateScreen.displayTextRegions(regions);
        copyScreen.displayTextRegions(regions);
    }

    /** Idempotently recycle the shared screenshot bitmap. */
    public void recycleScreenshot() {
        if (recycled) return;
        recycled = true;
        translateScreen.setScreenshot(null);
        copyScreen.setScreenshot(null);
        if (screenshot != null && !screenshot.isRecycled()) {
            screenshot.recycle();
        }
        screenshot = null;
    }
}
