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
    private LensPagerView pagerView;
    private OcrScanner ocrScanner;
    private Handler handler = new Handler(Looper.getMainLooper());

    public LensSession(Context context) {
        super(context);
        ocrScanner = new OcrScanner();
    }

    @Override
    public View onCreateContentView() {
        pagerView = new LensPagerView(getContext());
        pagerView.setOnDismissListener(() -> finish());
        return pagerView;
    }

    @Override
    public void onShow(Bundle args, int showFlags) {
        super.onShow(args, showFlags);
        if (pagerView != null) {
            pagerView.showLoading();
        }
    }

    @Override
    public void onHandleScreenshot(Bitmap screenshot) {
        Log.d(TAG, "onHandleScreenshot received: " +
            (screenshot != null ? screenshot.getWidth() + "x" + screenshot.getHeight() : "null"));

        if (screenshot != null) {
            processScreenshot(screenshot);
        } else {
            handler.post(() -> {
                if (pagerView != null) {
                    pagerView.showError("No screenshot available");
                }
            });
        }
    }

    private void processScreenshot(Bitmap screenshot) {
        if (screenshot == null) {
            handler.post(() -> {
                if (pagerView != null) {
                    pagerView.showError("Screenshot failed");
                }
            });
            return;
        }

        // OCR coordinates stay in raw screenshot-bitmap space; the views own
        // the bitmap->view transform, so the frozen screenshot and the boxes
        // always align regardless of keyboard / status-bar state.
        ocrScanner.scanBitmap(screenshot, new OcrScanner.OcrCallback() {
            @Override
            public void onOcrComplete(List<TextRegion> regions) {
                if (regions != null && !regions.isEmpty()) {
                    new Thread(() -> {
                        ChineseTextEnricher enricher = new ChineseTextEnricher(getContext());
                        enricher.enrichRegions(regions);

                        handler.post(() -> {
                            if (pagerView != null) {
                                pagerView.setScreenshot(screenshot);
                                pagerView.displayTextRegions(regions);
                            }
                        });
                    }).start();
                } else {
                    handler.post(() -> {
                        if (pagerView != null) {
                            pagerView.setScreenshot(screenshot);
                            pagerView.displayTextRegions(
                                    regions != null ? regions : new ArrayList<>());
                        }
                    });
                }
                // Bitmap is retained for the overlay background; recycled on
                // session hide/destroy via pagerView.recycleScreenshot().
            }

            @Override
            public void onOcrError(Exception e) {
                handler.post(() -> {
                    if (pagerView != null) {
                        pagerView.showError("OCR failed: " + e.getMessage());
                    }
                });
            }
        });
    }

    @Override
    public void onHide() {
        super.onHide();
        handler.removeCallbacksAndMessages(null);
        if (pagerView != null) {
            pagerView.recycleScreenshot();
        }
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        handler.removeCallbacksAndMessages(null);
        if (pagerView != null) {
            pagerView.recycleScreenshot();
        }
        if (ocrScanner != null) {
            ocrScanner.close();
        }
    }
}
