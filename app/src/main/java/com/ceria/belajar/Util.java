package com.ceria.belajar;

import android.content.Context;
import android.graphics.Color;
import android.graphics.drawable.GradientDrawable;
import android.util.TypedValue;
import android.view.Gravity;
import android.view.View;
import android.widget.TextView;

import java.util.Random;

/** Small helpers for spacing, colours and rounded shapes used all over the app. */
public final class Util {

    public static final int[] FUN_COLORS = {
            0xFFFF6D6D, 0xFFFFB300, 0xFFFFD600, 0xFF66BB6A,
            0xFF29B6F6, 0xFF7C4DFF, 0xFFFF4081, 0xFF26C6DA,
            0xFFFF9100, 0xFF9CCC65, 0xFFAB47BC, 0xFF5C6BC0
    };

    private static final Random RANDOM = new Random();

    private Util() {
    }

    public static int dp(Context c, float value) {
        return Math.round(TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP, value,
                c.getResources().getDisplayMetrics()));
    }

    public static int funColor(int index) {
        return FUN_COLORS[Math.abs(index) % FUN_COLORS.length];
    }

    public static int randomFunColor() {
        return FUN_COLORS[RANDOM.nextInt(FUN_COLORS.length)];
    }

    public static GradientDrawable rounded(int fill, float radiusPx) {
        GradientDrawable d = new GradientDrawable();
        d.setShape(GradientDrawable.RECTANGLE);
        d.setColor(fill);
        d.setCornerRadius(radiusPx);
        return d;
    }

    public static GradientDrawable roundedStroke(int fill, float radiusPx, int strokeColor, int strokeWidth) {
        GradientDrawable d = rounded(fill, radiusPx);
        d.setStroke(strokeWidth, strokeColor);
        return d;
    }

    public static GradientDrawable gradient(int from, int to, float radiusPx) {
        GradientDrawable d = new GradientDrawable(GradientDrawable.Orientation.TL_BR,
                new int[]{from, to});
        d.setShape(GradientDrawable.RECTANGLE);
        d.setCornerRadius(radiusPx);
        return d;
    }

    /** Big, chunky card style used for tiles and buttons. */
    public static void card(View v, int fill, float radiusPx) {
        v.setBackground(rounded(fill, radiusPx));
        v.setElevation(dp(v.getContext(), 4));
    }

    public static TextView text(Context c, String value, float sizeSp, int color) {
        TextView t = new TextView(c);
        t.setText(value);
        t.setTextSize(sizeSp);
        t.setTextColor(color);
        t.setGravity(Gravity.CENTER);
        return t;
    }

    public static int darker(int color, float factor) {
        float[] hsv = new float[3];
        Color.colorToHSV(color, hsv);
        hsv[2] = Math.max(0f, hsv[2] * factor);
        return Color.HSVToColor(hsv);
    }
}
