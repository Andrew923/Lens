package com.andrew.lens;

import android.app.Activity;
import android.graphics.Color;
import android.os.Bundle;
import android.view.Gravity;
import android.view.View;
import android.widget.CompoundButton;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.Switch;
import android.widget.TextView;

public class MainActivity extends Activity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        ScrollView scroll = new ScrollView(this);

        LinearLayout layout = new LinearLayout(this);
        layout.setOrientation(LinearLayout.VERTICAL);
        layout.setPadding(48, 48, 48, 48);
        scroll.addView(layout);

        TextView titleText = new TextView(this);
        titleText.setText("Lens");
        titleText.setTextSize(32);
        titleText.setGravity(Gravity.CENTER);
        layout.addView(titleText);

        TextView statusText = new TextView(this);
        statusText.setText("Ready\n\nHold the power button to activate");
        statusText.setTextSize(16);
        statusText.setTextColor(0xFF00AA00);
        statusText.setGravity(Gravity.CENTER);
        statusText.setPadding(0, 32, 0, 40);
        layout.addView(statusText);

        TextView settingsHeader = new TextView(this);
        settingsHeader.setText("Settings");
        settingsHeader.setTextSize(20);
        settingsHeader.setPadding(0, 0, 0, 8);
        layout.addView(settingsHeader);

        View divider = new View(this);
        divider.setBackgroundColor(0xFFCCCCCC);
        LinearLayout.LayoutParams dp = new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT, 2);
        dp.bottomMargin = 16;
        layout.addView(divider, dp);

        addToggle(layout,
                "Pinyin with tone marks",
                "On: nǐ hǎo   ·   Off: ni3 hao3",
                Prefs.pinyinToneMarks(this),
                (b, v) -> Prefs.setPinyinToneMarks(this, v));

        addToggle(layout,
                "Show traditional characters",
                "Display the traditional form as the primary character",
                Prefs.useTraditional(this),
                (b, v) -> Prefs.setUseTraditional(this, v));

        addToggle(layout,
                "Open on Copy screen",
                "Start the overlay on Copy instead of Translate",
                Prefs.defaultScreenCopy(this),
                (b, v) -> Prefs.setDefaultScreenCopy(this, v));

        addToggle(layout,
                "Dim screenshot behind boxes",
                "Darken the frozen screenshot for annotation contrast",
                Prefs.dimScreenshot(this),
                (b, v) -> Prefs.setDimScreenshot(this, v));

        setContentView(scroll);
    }

    private void addToggle(LinearLayout parent, String title, String subtitle,
                           boolean initial, CompoundButton.OnCheckedChangeListener listener) {
        LinearLayout row = new LinearLayout(this);
        row.setOrientation(LinearLayout.HORIZONTAL);
        row.setGravity(Gravity.CENTER_VERTICAL);
        row.setPadding(0, 20, 0, 20);

        LinearLayout textCol = new LinearLayout(this);
        textCol.setOrientation(LinearLayout.VERTICAL);

        TextView titleView = new TextView(this);
        titleView.setText(title);
        titleView.setTextSize(17);
        titleView.setTextColor(Color.BLACK);
        textCol.addView(titleView);

        TextView subView = new TextView(this);
        subView.setText(subtitle);
        subView.setTextSize(13);
        subView.setTextColor(0xFF888888);
        subView.setPadding(0, 4, 0, 0);
        textCol.addView(subView);

        row.addView(textCol, new LinearLayout.LayoutParams(
                0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f));

        Switch toggle = new Switch(this);
        toggle.setChecked(initial);
        toggle.setOnCheckedChangeListener(listener);
        row.addView(toggle);

        parent.addView(row, new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT));
    }
}
