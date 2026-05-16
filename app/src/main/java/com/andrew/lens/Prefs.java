package com.andrew.lens;

import android.content.Context;
import android.content.SharedPreferences;

/**
 * Thin wrapper over SharedPreferences for user-configurable Lens options.
 * All reads have sensible defaults so callers can use them without a setup step.
 */
public final class Prefs {
    private static final String FILE = "lens_prefs";

    private static final String KEY_PINYIN_TONE_MARKS = "pinyin_tone_marks";
    private static final String KEY_USE_TRADITIONAL = "use_traditional";
    private static final String KEY_DEFAULT_COPY = "default_screen_copy";
    private static final String KEY_DIM_SCREENSHOT = "dim_screenshot";

    private Prefs() {}

    private static SharedPreferences sp(Context c) {
        return c.getApplicationContext()
                .getSharedPreferences(FILE, Context.MODE_PRIVATE);
    }

    /** true = pinyin shown with tone marks (nǐ hǎo); false = numbered (ni3 hao3). */
    public static boolean pinyinToneMarks(Context c) {
        return sp(c).getBoolean(KEY_PINYIN_TONE_MARKS, true);
    }

    public static void setPinyinToneMarks(Context c, boolean v) {
        sp(c).edit().putBoolean(KEY_PINYIN_TONE_MARKS, v).apply();
    }

    /** true = show the traditional form as the primary character. */
    public static boolean useTraditional(Context c) {
        return sp(c).getBoolean(KEY_USE_TRADITIONAL, false);
    }

    public static void setUseTraditional(Context c, boolean v) {
        sp(c).edit().putBoolean(KEY_USE_TRADITIONAL, v).apply();
    }

    /** true = open the pager on the Copy screen instead of Translate. */
    public static boolean defaultScreenCopy(Context c) {
        return sp(c).getBoolean(KEY_DEFAULT_COPY, false);
    }

    public static void setDefaultScreenCopy(Context c, boolean v) {
        sp(c).edit().putBoolean(KEY_DEFAULT_COPY, v).apply();
    }

    /** true = dim the frozen screenshot behind the translate boxes. */
    public static boolean dimScreenshot(Context c) {
        return sp(c).getBoolean(KEY_DIM_SCREENSHOT, true);
    }

    public static void setDimScreenshot(Context c, boolean v) {
        sp(c).edit().putBoolean(KEY_DIM_SCREENSHOT, v).apply();
    }
}
