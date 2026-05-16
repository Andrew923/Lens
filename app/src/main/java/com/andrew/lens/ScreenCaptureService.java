package com.andrew.lens;

import android.app.Activity;
import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.Service;
import android.content.Context;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.PixelFormat;
import android.graphics.Point;
import android.hardware.display.DisplayManager;
import android.hardware.display.VirtualDisplay;
import android.media.Image;
import android.media.ImageReader;
import android.media.projection.MediaProjection;
import android.media.projection.MediaProjectionManager;
import android.os.Handler;
import android.os.IBinder;
import android.os.Looper;
import android.util.DisplayMetrics;
import android.util.Log;
import android.view.Display;
import android.view.WindowManager;

import java.nio.ByteBuffer;

public class ScreenCaptureService extends Service {
    private static final String TAG = "ScreenCaptureService";
    private static final String CHANNEL_ID = "lens_capture_channel";
    private static final int NOTIFICATION_ID = 1001;

    private static ScreenCaptureService instance;
    private MediaProjection mediaProjection;
    private ImageReader imageReader;
    private VirtualDisplay virtualDisplay;
    private int screenWidth;
    private int screenHeight;
    private int screenDensity;
    private Handler handler = new Handler(Looper.getMainLooper());
    private boolean isProjectionStopped = false;

    private MediaProjection.Callback projectionCallback = new MediaProjection.Callback() {
        @Override
        public void onStop() {
            Log.d(TAG, "MediaProjection stopped");
            isProjectionStopped = true;
            cleanup();
        }
    };

    public static ScreenCaptureService getInstance() {
        return instance;
    }

    @Override
    public void onCreate() {
        super.onCreate();
        instance = this;
        createNotificationChannel();

        // Get screen metrics using modern API
        WindowManager wm = (WindowManager) getSystemService(Context.WINDOW_SERVICE);
        Display display = wm.getDefaultDisplay();
        DisplayMetrics metrics = new DisplayMetrics();
        display.getRealMetrics(metrics);
        screenWidth = metrics.widthPixels;
        screenHeight = metrics.heightPixels;
        screenDensity = metrics.densityDpi;

        Log.d(TAG, "Screen size: " + screenWidth + "x" + screenHeight + " @ " + screenDensity + "dpi");
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        Log.d(TAG, "onStartCommand called");

        // Start foreground immediately (required on Android 14+)
        Notification notification = new Notification.Builder(this, CHANNEL_ID)
                .setContentTitle("Lens Screen Capture")
                .setContentText("Ready to capture screen for text recognition")
                .setSmallIcon(android.R.drawable.ic_menu_camera)
                .setOngoing(true)
                .build();

        startForeground(NOTIFICATION_ID, notification);

        if (intent == null) {
            Log.e(TAG, "Null intent received");
            return START_STICKY;
        }

        int resultCode = intent.getIntExtra("resultCode", 0);
        Intent data = intent.getParcelableExtra("data");

        if (resultCode != Activity.RESULT_OK || data == null) {
            Log.e(TAG, "Invalid resultCode or data: resultCode=" + resultCode + " (RESULT_OK=" + Activity.RESULT_OK + "), data=" + data);
            return START_STICKY;
        }

        try {
            // Create MediaProjection
            MediaProjectionManager projectionManager =
                    (MediaProjectionManager) getSystemService(Context.MEDIA_PROJECTION_SERVICE);
            mediaProjection = projectionManager.getMediaProjection(resultCode, data);

            if (mediaProjection != null) {
                Log.d(TAG, "MediaProjection created successfully");
                isProjectionStopped = false;
                mediaProjection.registerCallback(projectionCallback, handler);
                setupImageReader();
            } else {
                Log.e(TAG, "Failed to create MediaProjection");
            }
        } catch (Exception e) {
            Log.e(TAG, "Error creating MediaProjection", e);
        }

        return START_STICKY;
    }

    private void setupImageReader() {
        try {
            imageReader = ImageReader.newInstance(
                    screenWidth, screenHeight,
                    PixelFormat.RGBA_8888, 2);

            virtualDisplay = mediaProjection.createVirtualDisplay(
                    "LensCapture",
                    screenWidth, screenHeight, screenDensity,
                    DisplayManager.VIRTUAL_DISPLAY_FLAG_AUTO_MIRROR,
                    imageReader.getSurface(),
                    null, handler);

            Log.d(TAG, "ImageReader and VirtualDisplay setup complete");
        } catch (Exception e) {
            Log.e(TAG, "Error setting up ImageReader", e);
        }
    }

    public interface CaptureCallback {
        void onCaptureComplete(Bitmap bitmap);
        void onCaptureFailed(String error);
    }

    public void captureScreen(CaptureCallback callback) {
        if (isProjectionStopped) {
            Log.e(TAG, "MediaProjection was stopped");
            callback.onCaptureFailed("Permission expired. Please grant again in app.");
            return;
        }
        if (imageReader == null || mediaProjection == null) {
            Log.e(TAG, "Not ready: imageReader=" + imageReader + ", mediaProjection=" + mediaProjection);
            callback.onCaptureFailed("Screen capture not initialized. Grant permission in app.");
            return;
        }

        // Small delay to ensure the virtual display has updated
        handler.postDelayed(() -> {
            try {
                Image image = imageReader.acquireLatestImage();
                if (image == null) {
                    Log.e(TAG, "acquireLatestImage returned null");
                    callback.onCaptureFailed("Failed to capture image");
                    return;
                }

                Bitmap bitmap = imageToBitmap(image);
                image.close();

                if (bitmap != null) {
                    Log.d(TAG, "Capture successful: " + bitmap.getWidth() + "x" + bitmap.getHeight());
                    callback.onCaptureComplete(bitmap);
                } else {
                    callback.onCaptureFailed("Failed to convert image to bitmap");
                }
            } catch (Exception e) {
                Log.e(TAG, "Capture error", e);
                callback.onCaptureFailed("Capture error: " + e.getMessage());
            }
        }, 100);
    }

    private Bitmap imageToBitmap(Image image) {
        Image.Plane[] planes = image.getPlanes();
        ByteBuffer buffer = planes[0].getBuffer();
        int pixelStride = planes[0].getPixelStride();
        int rowStride = planes[0].getRowStride();
        int rowPadding = rowStride - pixelStride * screenWidth;

        Bitmap bitmap = Bitmap.createBitmap(
                screenWidth + rowPadding / pixelStride,
                screenHeight,
                Bitmap.Config.ARGB_8888);
        bitmap.copyPixelsFromBuffer(buffer);

        // Crop to actual screen size (remove padding)
        if (rowPadding > 0) {
            Bitmap cropped = Bitmap.createBitmap(bitmap, 0, 0, screenWidth, screenHeight);
            bitmap.recycle();
            return cropped;
        }

        return bitmap;
    }

    public boolean isReady() {
        return mediaProjection != null && imageReader != null && !isProjectionStopped;
    }

    private void cleanup() {
        if (virtualDisplay != null) {
            virtualDisplay.release();
            virtualDisplay = null;
        }
        if (imageReader != null) {
            imageReader.close();
            imageReader = null;
        }
    }

    private void createNotificationChannel() {
        NotificationChannel channel = new NotificationChannel(
                CHANNEL_ID,
                "Screen Capture",
                NotificationManager.IMPORTANCE_LOW);
        channel.setDescription("Required for screen capture functionality");

        NotificationManager manager = getSystemService(NotificationManager.class);
        manager.createNotificationChannel(channel);
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        Log.d(TAG, "onDestroy called");
        instance = null;

        cleanup();

        if (mediaProjection != null) {
            try {
                mediaProjection.unregisterCallback(projectionCallback);
                mediaProjection.stop();
            } catch (Exception e) {
                Log.e(TAG, "Error stopping MediaProjection", e);
            }
            mediaProjection = null;
        }
    }

    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }
}
