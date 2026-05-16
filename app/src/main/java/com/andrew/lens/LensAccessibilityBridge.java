package com.andrew.lens;

import android.os.Handler;
import android.os.Looper;

import java.util.List;

public class LensAccessibilityBridge {

    private static LensAccessibilityBridge instance;
    private TextRegionsCallback currentCallback;
    private final Handler mainHandler = new Handler(Looper.getMainLooper());

    public interface TextRegionsCallback {
        void onTextRegionsReceived(List<TextRegion> regions);
    }

    private LensAccessibilityBridge() {}

    public static synchronized LensAccessibilityBridge getInstance() {
        if (instance == null) {
            instance = new LensAccessibilityBridge();
        }
        return instance;
    }

    /**
     * Called from LensSession to request text regions
     */
    public void requestTextRegions(TextRegionsCallback callback) {
        this.currentCallback = callback;

        // Check if AccessibilityService is running
        LensAccessibilityService service = LensAccessibilityService.getInstance();
        if (service != null) {
            // Run on background thread to avoid blocking UI
            new Thread(() -> {
                List<TextRegion> regions = service.scanScreenForText();

                // Enrich Chinese text regions with pinyin and definitions
                ChineseTextEnricher enricher = new ChineseTextEnricher(service);
                enricher.enrichRegions(regions);

                // Deliver results on main thread
                mainHandler.post(() -> {
                    if (currentCallback != null) {
                        currentCallback.onTextRegionsReceived(regions);
                    }
                });
            }).start();
        } else {
            // AccessibilityService not running - notify user
            mainHandler.post(() -> {
                if (currentCallback != null) {
                    currentCallback.onTextRegionsReceived(null);
                }
            });
        }
    }

    public void clearCallback() {
        this.currentCallback = null;
    }

    /**
     * Check if AccessibilityService is enabled
     */
    public boolean isAccessibilityServiceEnabled() {
        return LensAccessibilityService.getInstance() != null;
    }
}
