package com.andrew.lens;

import android.content.Context;
import android.graphics.Color;
import android.graphics.Typeface;
import android.util.DisplayMetrics;
import android.view.Gravity;
import android.view.View;
import android.widget.FrameLayout;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.TextView;

public class DefinitionPopupView extends FrameLayout {
    private TextView characterText;
    private TextView traditionalText;
    private TextView pinyinText;
    private TextView definitionText;
    private DictionaryEntry currentEntry;
    private OnDismissListener dismissListener;

    public interface OnDismissListener {
        void onDismiss();
    }

    public DefinitionPopupView(Context context) {
        super(context);
        init();
    }

    private void init() {
        setBackgroundResource(R.drawable.popup_background);
        setElevation(16f);
        setPadding(32, 24, 32, 24);

        LinearLayout container = new LinearLayout(getContext());
        container.setOrientation(LinearLayout.VERTICAL);

        // Header row: Character + Traditional (if different)
        LinearLayout headerRow = new LinearLayout(getContext());
        headerRow.setOrientation(LinearLayout.HORIZONTAL);
        headerRow.setGravity(Gravity.CENTER_VERTICAL);

        characterText = new TextView(getContext());
        characterText.setTextSize(36);
        characterText.setTextColor(Color.BLACK);
        characterText.setTypeface(Typeface.DEFAULT_BOLD);
        headerRow.addView(characterText);

        traditionalText = new TextView(getContext());
        traditionalText.setTextSize(20);
        traditionalText.setTextColor(0xFF666666);
        traditionalText.setPadding(16, 0, 0, 0);
        headerRow.addView(traditionalText);

        // Spacer
        View spacer = new View(getContext());
        LinearLayout.LayoutParams spacerParams = new LinearLayout.LayoutParams(0, 0, 1f);
        headerRow.addView(spacer, spacerParams);

        // Close button (X)
        TextView closeButton = new TextView(getContext());
        closeButton.setText("X");
        closeButton.setTextSize(18);
        closeButton.setTextColor(0xFF888888);
        closeButton.setPadding(16, 0, 0, 0);
        closeButton.setOnClickListener(v -> dismiss());
        headerRow.addView(closeButton);

        container.addView(headerRow);

        // Pinyin
        pinyinText = new TextView(getContext());
        pinyinText.setTextSize(20);
        pinyinText.setTextColor(0xFF4CAF50);
        pinyinText.setPadding(0, 8, 0, 16);
        container.addView(pinyinText);

        // Divider
        View divider = new View(getContext());
        divider.setBackgroundColor(0xFFDDDDDD);
        LinearLayout.LayoutParams dividerParams = new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT, 1);
        dividerParams.bottomMargin = 16;
        container.addView(divider, dividerParams);

        // Definitions in scrollview
        ScrollView scrollView = new ScrollView(getContext());
        scrollView.setLayoutParams(new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT));

        definitionText = new TextView(getContext());
        definitionText.setTextSize(16);
        definitionText.setTextColor(Color.BLACK);
        definitionText.setLineSpacing(8f, 1f);
        scrollView.addView(definitionText);

        container.addView(scrollView);

        addView(container);
    }

    public void setOnDismissListener(OnDismissListener listener) {
        this.dismissListener = listener;
    }

    public void showEntry(DictionaryEntry entry, int targetX, int targetY) {
        this.currentEntry = entry;

        boolean useTraditional = Prefs.useTraditional(getContext());
        String primary = useTraditional ? entry.traditional : entry.simplified;
        String secondary = useTraditional ? entry.simplified : entry.traditional;
        characterText.setText(primary);

        // Show the other form in parentheses when it differs
        if (!secondary.equals(primary)) {
            traditionalText.setText("(" + secondary + ")");
            traditionalText.setVisibility(VISIBLE);
        } else {
            traditionalText.setVisibility(GONE);
        }

        pinyinText.setText(Prefs.pinyinToneMarks(getContext())
                ? entry.pinyinDisplay : entry.pinyin);

        // Format definitions with bullets
        StringBuilder defText = new StringBuilder();
        for (int i = 0; i < entry.definitions.length; i++) {
            if (i > 0) defText.append("\n");
            defText.append("• ").append(entry.definitions[i]);
        }
        definitionText.setText(defText.toString());

        // Position popup
        positionNearTarget(targetX, targetY);
        setVisibility(VISIBLE);
    }

    private void positionNearTarget(int targetX, int targetY) {
        // Measure the popup
        measure(MeasureSpec.makeMeasureSpec(0, MeasureSpec.UNSPECIFIED),
                MeasureSpec.makeMeasureSpec(0, MeasureSpec.UNSPECIFIED));

        int popupWidth = getMeasuredWidth();
        int popupHeight = getMeasuredHeight();

        DisplayMetrics dm = getResources().getDisplayMetrics();
        int screenWidth = dm.widthPixels;
        int screenHeight = dm.heightPixels;

        // Center horizontally on target, but keep on screen
        int x = targetX - popupWidth / 2;
        x = Math.max(16, Math.min(x, screenWidth - popupWidth - 16));

        // Prefer above the target
        int y = targetY - popupHeight - 32;
        if (y < 100) {
            // Too close to top, show below
            y = targetY + 32;
        }

        // Keep on screen vertically
        y = Math.max(100, Math.min(y, screenHeight - popupHeight - 100));

        setTranslationX(x);
        setTranslationY(y);
    }

    public void dismiss() {
        setVisibility(GONE);
        if (dismissListener != null) {
            dismissListener.onDismiss();
        }
    }

    public boolean isShowing() {
        return getVisibility() == VISIBLE;
    }
}
