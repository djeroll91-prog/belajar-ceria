package com.ceria.belajar;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.LinearGradient;
import android.graphics.Paint;
import android.graphics.RadialGradient;
import android.graphics.Shader;
import android.os.SystemClock;
import android.util.AttributeSet;
import android.view.View;

/**
 * The calm backdrop behind every screen.
 *
 * It is a soft vertical gradient with a few very large, very faint drifting
 * blobs. The shapes are deliberately close to the background colour: a child
 * needs the content to stand out, not the wallpaper, and a busy background is
 * what makes letters hard to pick out. Everything is drawn from the active
 * {@link Theme}, so dark mode is a real change rather than a dimmed copy.
 *
 * Drawing rather than bundling an image means it scales from a small phone to a
 * tablet without artefacts. Animation only runs while the view is attached.
 */
public class StorySceneView extends View {

    private static final int BLOB_COUNT = 4;
    private static final long FRAME_MS = 66;      // ~15 fps: a calm drift is enough

    private final Paint paint = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final float[] blobX = new float[BLOB_COUNT];
    private final float[] blobY = new float[BLOB_COUNT];
    private final float[] blobScale = new float[BLOB_COUNT];
    private final float[] blobSpeed = new float[BLOB_COUNT];

    private final Theme theme;
    private long startTime;
    private boolean built;

    public StorySceneView(Context context) {
        this(context, null);
    }

    public StorySceneView(Context context, AttributeSet attrs) {
        super(context, attrs);
        setWillNotDraw(false);
        theme = context instanceof MainActivity ? ((MainActivity) context).theme() : new Theme(context);

        // Fixed positions: the same calm backdrop every launch, so the app feels
        // familiar rather than random.
        float[][] seed = {
                {0.15f, 0.12f, 1.35f, 0.0040f},
                {0.78f, 0.22f, 1.10f, 0.0026f},
                {0.35f, 0.62f, 1.55f, 0.0033f},
                {0.88f, 0.78f, 1.20f, 0.0020f},
        };
        for (int i = 0; i < BLOB_COUNT; i++) {
            blobX[i] = seed[i][0];
            blobY[i] = seed[i][1];
            blobScale[i] = seed[i][2];
            blobSpeed[i] = seed[i][3];
        }
    }

    @Override
    protected void onAttachedToWindow() {
        super.onAttachedToWindow();
        startTime = SystemClock.uptimeMillis();
        postInvalidateOnAnimation();
    }

    @Override
    protected void onDraw(Canvas canvas) {
        super.onDraw(canvas);
        int w = getWidth();
        int h = getHeight();
        if (w == 0 || h == 0) {
            return;
        }
        if (!built) {
            buildGradient(h);
            built = true;
        }

        canvas.drawRect(0, 0, w, h, paint);
        drawBlobs(canvas, w, h);
    }

    private void buildGradient(int h) {
        int top = theme.isDark()
                ? Theme.lighten(theme.bg(), 0.06f)
                : Theme.lighten(theme.bg(), 0.35f);
        paint.setShader(new LinearGradient(0, 0, 0, h,
                new int[]{top, theme.bg(), theme.surface2()},
                new float[]{0f, 0.62f, 1f}, Shader.TileMode.CLAMP));
    }

    private void drawBlobs(Canvas canvas, int w, int h) {
        float elapsed = (SystemClock.uptimeMillis() - startTime) / 1000f;
        int base = theme.accent();
        float opacity = theme.isDark() ? 0.10f : 0.07f;

        for (int i = 0; i < BLOB_COUNT; i++) {
            float x = (blobX[i] + elapsed * blobSpeed[i]) % 1.3f - 0.15f;
            float radius = Math.min(w, h) * 0.42f * blobScale[i];
            int tint = i % 2 == 0 ? base : theme.line();
            paint.setShader(new RadialGradient(x * w, blobY[i] * h, radius,
                    new int[]{withAlpha(tint, opacity), withAlpha(tint, 0f)},
                    new float[]{0f, 1f}, Shader.TileMode.CLAMP));
            canvas.drawCircle(x * w, blobY[i] * h, radius, paint);
        }
        paint.setShader(null);

        if (isAttachedToWindow()) {
            postInvalidateDelayed(FRAME_MS);
        }
    }

    private static int withAlpha(int color, float alpha) {
        int a = Math.round(255 * Math.max(0f, Math.min(1f, alpha)));
        return (a << 24) | (color & 0x00FFFFFF);
    }
}
