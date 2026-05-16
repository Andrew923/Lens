package com.andrew.lens;

import android.content.Context;
import android.graphics.Bitmap;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.service.voice.VoiceInteractionSession;
import android.service.voice.VoiceInteractionSessionService;
import android.util.Log;
import android.view.View;

import java.util.ArrayList;
import java.util.List;

public class LensSessionService extends VoiceInteractionSessionService {
    @Override
    public VoiceInteractionSession onNewSession(Bundle args) {
        return new LensSession(this);
    }
}

class LensSession extends VoiceInteractionSession {
    private static final String TAG = "LensSession";
    private LensOverlayView overlayView;
    private OcrScanner ocrScanner;
    private Handler handler = new Handler(Looper.getMainLooper());
    private int yOffset = 0;

    public LensSession(Context context) {
        super(context);
        ocrScanner = new OcrScanner();
    }

    @Override
    public View onCreateContentView() {
        overlayView = new LensOverlayView(getContext());
        overlayView.setOnDismissListener(() -> finish());
        return overlayView;
    }

    @Override
    public void onShow(Bundle args, int showFlags) {
        super.onShow(args, showFlags);

        // Show loading state
        if (overlayView != null) {
            overlayView.showLoading();
        }
    }

    @Override
    public void onHandleScreenshot(Bitmap screenshot) {
        Log.d(TAG, "onHandleScreenshot received: " +
            (screenshot != null ? screenshot.getWidth() + "x" + screenshot.getHeight() : "null"));

        if (screenshot != null) {
            // Get status bar height from system resources
            int statusBarHeight = 0;
            int resourceId = getContext().getResources().getIdentifier("status_bar_height", "dimen", "android");
            if (resourceId > 0) {
                statusBarHeight = getContext().getResources().getDimensionPixelSize(resourceId);
            }
            yOffset = statusBarHeight;
            Log.d(TAG, "Status bar height: " + yOffset);

            processScreenshot(screenshot);
        } else {
            handler.post(() -> {
                if (overlayView != null) {
                    overlayView.showError("No screenshot available");
                }
            });
        }
    }

    private void processScreenshot(Bitmap screenshot) {
        if (screenshot == null) {
            handler.post(() -> {
                if (overlayView != null) {
                    overlayView.showError("Screenshot failed");
                }
            });
            return;
        }

        // Pass negative offset if overlay starts below screen top
        // OCR coordinates are in screenshot space, overlay is in screen space
        // If overlay starts at y=100, we need to ADD 100 to OCR y coords (or pass -100 to subtract internally)
        int adjustedOffset = -yOffset;
        Log.d(TAG, "Using OCR offset: " + adjustedOffset);

        ocrScanner.scanBitmap(screenshot, adjustedOffset, new OcrScanner.OcrCallback() {
            @Override
            public void onOcrComplete(List<TextRegion> regions) {
                // Enrich Chinese text with pinyin/definitions
                if (regions != null && !regions.isEmpty()) {
                    new Thread(() -> {
                        ChineseTextEnricher enricher = new ChineseTextEnricher(getContext());
                        enricher.enrichRegions(regions);

                        handler.post(() -> {
                            if (overlayView != null) {
                                overlayView.displayTextRegions(regions);
                            }
                        });
                    }).start();
                } else {
                    handler.post(() -> {
                        if (overlayView != null) {
                            overlayView.displayTextRegions(regions != null ? regions : new ArrayList<>());
                        }
                    });
                }

                // Recycle bitmap after OCR is done
                screenshot.recycle();
            }

            @Override
            public void onOcrError(Exception e) {
                screenshot.recycle();
                handler.post(() -> {
                    if (overlayView != null) {
                        overlayView.showError("OCR failed: " + e.getMessage());
                    }
                });
            }
        });
    }

    @Override
    public void onHide() {
        super.onHide();
        handler.removeCallbacksAndMessages(null);
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        handler.removeCallbacksAndMessages(null);
        if (ocrScanner != null) {
            ocrScanner.close();
        }
    }
}
