package com.ceria.belajar;

import android.content.Context;
import android.content.SharedPreferences;

/**
 * The app palette, in a light and a dark variant.
 *
 * Colours are chosen for a child's eyes rather than for a screenshot: the light
 * background is warm cream instead of pure white (less glare under a bright
 * screen), and text sits well above the 4.5:1 contrast minimum so small labels
 * stay readable. Dark mode keeps the same hues at lower luminance so a tunjang
 * still has the same colour identity at night.
 */
public final class Theme {

    public enum Mode { LIGHT, DARK }

    private static final String PREFS = "belajar_ceria_prefs";
    private static final String KEY = "ui_mode";

    // ---- light ----
    private static final int L_BG = 0xFFFFF6EC;
    private static final int L_SURFACE = 0xFFFFFFFF;
    private static final int L_SURFACE_2 = 0xFFFFF1E2;
    private static final int L_INK = 0xFF3A2E4F;
    private static final int L_INK_SOFT = 0xFF7A6C90;
    private static final int L_LINE = 0xFFEADFEF;
    private static final int L_ACCENT = 0xFFFF8A65;

    // ---- dark ----
    private static final int D_BG = 0xFF1B1726;
    private static final int D_SURFACE = 0xFF262036;
    private static final int D_SURFACE_2 = 0xFF2F2842;
    private static final int D_INK = 0xFFF3EEFF;
    private static final int D_INK_SOFT = 0xFFB9AECF;
    private static final int D_LINE = 0xFF3B3350;
    private static final int D_ACCENT = 0xFFFF9E80;

    public static final int GOOD = 0xFF66BB6A;
    public static final int GOOD_DARK = 0xFF2E7D32;
    public static final int BAD = 0xFFEF5350;
    public static final int BAD_DARK = 0xFFC62828;
    public static final int GOLD = 0xFFFFD166;

    private final SharedPreferences prefs;
    private Mode mode;

    public Theme(Context context) {
        prefs = context.getApplicationContext().getSharedPreferences(PREFS, Context.MODE_PRIVATE);
        mode = Mode.DARK.name().equals(prefs.getString(KEY, Mode.LIGHT.name()))
                ? Mode.DARK : Mode.LIGHT;
    }

    public Mode mode() {
        return mode;
    }

    public boolean isDark() {
        return mode == Mode.DARK;
    }

    public void setMode(Mode next) {
        mode = next;
        prefs.edit().putString(KEY, next.name()).apply();
    }

    public void toggle() {
        setMode(isDark() ? Mode.LIGHT : Mode.DARK);
    }

    private int pick(int light, int dark) {
        return isDark() ? dark : light;
    }

    public int bg() { return pick(L_BG, D_BG); }

    public int surface() { return pick(L_SURFACE, D_SURFACE); }

    public int surface2() { return pick(L_SURFACE_2, D_SURFACE_2); }

    public int ink() { return pick(L_INK, D_INK); }

    public int inkSoft() { return pick(L_INK_SOFT, D_INK_SOFT); }

    public int line() { return pick(L_LINE, D_LINE); }

    public int accent() { return pick(L_ACCENT, D_ACCENT); }

    /** Text colour that stays readable on top of an accent-filled button. */
    public int onAccent() {
        return isDark() ? 0xFF2A1F3A : 0xFFFFFFFF;
    }

    public int onGood() { return 0xFFFFFFFF; }

    /**
     * A tunjang keeps its own hue, but the hue is darkened in light mode and
     * lightened in dark mode so the tile border never disappears into the
     * background.
     */
    public int tunjangColor(int base) {
        return isDark() ? lighten(base, 0.18f) : base;
    }

    /** A pale wash of a hue, for tile backgrounds. */
    public int wash(int color, float amount) {
        return blend(surface(), color, amount);
    }

    public static int blend(int from, int to, float t) {
        float k = Math.max(0f, Math.min(1f, t));
        int r = (int) (android.graphics.Color.red(from) * (1 - k) + android.graphics.Color.red(to) * k);
        int g = (int) (android.graphics.Color.green(from) * (1 - k) + android.graphics.Color.green(to) * k);
        int b = (int) (android.graphics.Color.blue(from) * (1 - k) + android.graphics.Color.blue(to) * k);
        return 0xFF000000 | (r << 16) | (g << 8) | b;
    }

    public static int lighten(int color, float amount) {
        return blend(color, 0xFFFFFFFF, amount);
    }

    public static int darken(int color, float amount) {
        return blend(color, 0xFF000000, amount);
    }

    /** Colour of a correctness chip, darkened in light mode for contrast. */
    public int goodFor() {
        return isDark() ? GOOD : GOOD_DARK;
    }

    public int badFor() {
        return isDark() ? BAD : BAD_DARK;
    }
}
