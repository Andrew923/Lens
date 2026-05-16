package com.andrew.lens;

import android.accessibilityservice.AccessibilityService;
import android.graphics.Rect;
import android.view.accessibility.AccessibilityEvent;
import android.view.accessibility.AccessibilityNodeInfo;

import java.util.ArrayList;
import java.util.List;

public class LensAccessibilityService extends AccessibilityService {

    private static LensAccessibilityService instance;

    @Override
    public void onServiceConnected() {
        super.onServiceConnected();
        instance = this;
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        instance = null;
    }

    public static LensAccessibilityService getInstance() {
        return instance;
    }

    @Override
    public void onAccessibilityEvent(AccessibilityEvent event) {
        // We use on-demand scanning, not event-based
    }

    @Override
    public void onInterrupt() {
        // Handle interruption
    }

    /**
     * Scans the current screen and returns all text regions
     */
    public List<TextRegion> scanScreenForText() {
        List<TextRegion> regions = new ArrayList<>();
        AccessibilityNodeInfo rootNode = getRootInActiveWindow();

        if (rootNode != null) {
            traverseNode(rootNode, regions);
            rootNode.recycle();
        }

        return regions;
    }

    private void traverseNode(AccessibilityNodeInfo node, List<TextRegion> regions) {
        if (node == null) return;

        // Check if this node has text
        CharSequence text = node.getText();
        if (text != null && text.length() > 0) {
            Rect bounds = new Rect();
            node.getBoundsInScreen(bounds);

            // Only add if bounds are valid and visible
            if (bounds.width() > 0 && bounds.height() > 0) {
                TextRegion region = new TextRegion(
                        text.toString(),
                        bounds,
                        node.getClassName() != null ? node.getClassName().toString() : ""
                );
                regions.add(region);
            }
        }

        // Also check content description for images with alt text
        CharSequence contentDesc = node.getContentDescription();
        if (contentDesc != null && contentDesc.length() > 0 &&
                (text == null || text.length() == 0)) {
            Rect bounds = new Rect();
            node.getBoundsInScreen(bounds);

            if (bounds.width() > 0 && bounds.height() > 0) {
                TextRegion region = new TextRegion(
                        contentDesc.toString(),
                        bounds,
                        "ContentDescription"
                );
                regions.add(region);
            }
        }

        // Recursively traverse children
        for (int i = 0; i < node.getChildCount(); i++) {
            AccessibilityNodeInfo child = node.getChild(i);
            if (child != null) {
                traverseNode(child, regions);
                child.recycle();
            }
        }
    }
}
