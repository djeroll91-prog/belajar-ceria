package com.ceria.belajar;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.LinearGradient;
import android.graphics.Paint;
import android.graphics.Path;
import android.graphics.RadialGradient;
import android.graphics.Shader;
import android.os.SystemClock;
import android.util.AttributeSet;
import android.view.View;

import java.util.Random;

/**
 * A soft, painted backdrop for every screen: warm sky, a low sun, slow clouds
 * and rolling green hills.
 *
 * It is drawn rather than bundled as an image so it scales from a small phone to
 * a tablet without artefacts, and so the clouds can drift slowly enough to feel
 * calm rather than busy. Animation only runs while the view is attached, so it
 * costs nothing once the screen is closed.
 */
public class StorySceneView extends View {

    private final Paint paint = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Path hillPath = new Path();

    private static final int CLOUD_COUNT = 5;
    private final float[] cloudX = new float[CLOUD_COUNT];
    private final float[] cloudY = new float[CLOUD_COUNT];
    private final float[] cloudScale = new float[CLOUD_COUNT];
    private final float[] cloudSpeed = new float[CLOUD_COUNT];

    private long startTime;
    private boolean built;

    public StorySceneView(Context context) {
        this(context, null);
    }

    public StorySceneView(Context context, AttributeSet attrs) {
        super(context, attrs);
        setWillNotDraw(false);
        Random random = new Random(7);      // fixed seed: same calm sky every launch
        for (int i = 0; i < CLOUD_COUNT; i++) {
            cloudX[i] = random.nextFloat();
            cloudY[i] = 0.05f + random.nextFloat() * 0.34f;
            cloudScale[i] = 0.6f + random.nextFloat() * 0.8f;
            cloudSpeed[i] = 0.004f + random.nextFloat() * 0.010f;   // screens per second
        }
    }

    @Override
    protected void onAttachedToWindow() {
        super.onAttachedToWindow();
        startTime = SystemClock.uptimeMillis();
        // A slow repaint is enough for drifting clouds and far cheaper than a
        // frame-by-frame animator.
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
            buildSky(w, h);
            built = true;
        }

        canvas.drawRect(0, 0, w, h, paint);

        drawSun(canvas, w, h);

        float elapsed = (SystemClock.uptimeMillis() - startTime) / 1000f;
        for (int i = 0; i < CLOUD_COUNT; i++) {
            float x = (cloudX[i] + elapsed * cloudSpeed[i]) % 1.4f - 0.2f;
            drawCloud(canvas, x * w, cloudY[i] * h, cloudScale[i] * w * 0.22f, h);
        }

        drawHills(canvas, w, h);

        postInvalidateOnAnimation();
    }

    private void buildSky(int w, int h) {
        paint.setShader(new LinearGradient(0, 0, 0, h,
                new int[]{Util.SKY_TOP, Util.SKY_MID, Util.SKY_WARM},
                new float[]{0f, 0.55f, 1f}, Shader.TileMode.CLAMP));
    }

    /** A soft low sun; its halo is what makes the sky feel warm rather than flat. */
    private void drawSun(Canvas canvas, int w, int h) {
        float sx = w * 0.78f;
        float sy = h * 0.16f;
        float halo = Math.min(w, h) * 0.55f;
        paint.setShader(new RadialGradient(sx, sy, halo,
                new int[]{0x99FFF3B0, 0x44FFE9A0, 0x00FFF0B8},
                new float[]{0f, 0.45f, 1f}, Shader.TileMode.CLAMP));
        canvas.drawCircle(sx, sy, halo, paint);
        paint.setShader(null);
        paint.setColor(Util.SUN_GLOW);
        canvas.drawCircle(sx, sy, Math.min(w, h) * 0.075f, paint);
    }

    private void drawCloud(Canvas canvas, float cx, float cy, float radius, int h) {
        int glow = (int) (radius * 1.9f);
        paint.setShader(new RadialGradient(cx, cy, glow,
                new int[]{Util.CLOUD, 0x66FFFFFF, 0x00FFFFFF},
                new float[]{0f, 0.55f, 1f}, Shader.TileMode.CLAMP));
        canvas.drawCircle(cx, cy, glow, paint);

        // A few overlapping blobs give the cloud a hand-painted edge.
        paint.setShader(null);
        paint.setColor(Util.CLOUD);
        canvas.drawCircle(cx, cy, radius * 0.62f, paint);
        canvas.drawCircle(cx - radius * 0.62f, cy + radius * 0.14f, radius * 0.44f, paint);
        canvas.drawCircle(cx + radius * 0.60f, cy + radius * 0.18f, radius * 0.40f, paint);
        canvas.drawCircle(cx + radius * 0.12f, cy - radius * 0.34f, radius * 0.42f, paint);
    }

    private void drawHills(Canvas canvas, int w, int h) {
        // Distant hills, then a nearer meadow so the scene has depth.
        drawHill(canvas, w, h, h * 0.68f, h * 0.16f, Util.MEADOW);
        drawHill(canvas, w, h, h * 0.82f, h * 0.14f, Util.MEADOW_DEEP);
    }

    private void drawHill(Canvas canvas, int w, int h, float baseY, float crest, int color) {
        hillPath.reset();
        hillPath.moveTo(-w * 0.1f, h);
        hillPath.lineTo(-w * 0.1f, baseY);
        // Two gentle arcs read as soft rolling ground rather than a sine wave.
        hillPath.quadTo(w * 0.22f, baseY - crest, w * 0.5f, baseY - crest * 0.35f);
        hillPath.quadTo(w * 0.78f, baseY + crest * 0.22f, w * 1.1f, baseY - crest * 0.5f);
        hillPath.lineTo(w * 1.1f, h);
        hillPath.close();
        paint.setShader(null);
        paint.setColor(color);
        canvas.drawPath(hillPath, paint);
    }
}
