package com.andrew.lens;

import android.app.Activity;
import android.os.Bundle;
import android.view.Gravity;
import android.widget.LinearLayout;
import android.widget.TextView;

public class MainActivity extends Activity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        LinearLayout layout = new LinearLayout(this);
        layout.setOrientation(LinearLayout.VERTICAL);
        layout.setGravity(Gravity.CENTER);
        layout.setPadding(48, 48, 48, 48);

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
        statusText.setPadding(0, 48, 0, 48);
        layout.addView(statusText);

        setContentView(layout);
    }
}
