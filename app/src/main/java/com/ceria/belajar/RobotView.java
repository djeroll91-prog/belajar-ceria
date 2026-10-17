package com.ceria.belajar;

import android.animation.ValueAnimator;
import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.Path;
import android.graphics.RectF;
import android.util.AttributeSet;
import android.view.View;
import android.view.animation.LinearInterpolator;

/**
 * "Bobo" the friendly robot mascot. Purely canvas-drawn so it needs no image
 * assets and scales crisply on every screen size. It bobs, blinks and its mouth
 * moves while it talks, which keeps young children engaged.
 */
public class RobotView extends View {

    private final Paint paint = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final RectF oval = new RectF();
    private final Path path = new Path();

    private float bob;          // 0..1 vertical bobbing phase
    private float blink;        // 0 = open, 1 = fully closed
    private boolean talking;
    private float mouth;        // 0..1 mouth open amount
    private int bodyColor = 0xFF7FB4D9;

    private ValueAnimator bobAnim;
    private ValueAnimator blinkAnim;
    private ValueAnimator cheerAnim;
    private float cheer;        // 0 = idle, 1 = arms up and smiling wide
    private boolean happy;

    public RobotView(Context context) {
        this(context, null);
    }

    public RobotView(Context context, AttributeSet attrs) {
        super(context, attrs);
        bodyColor = Util.storyColor(0);
        startAnimations();
    }

    public void setBodyColor(int color) {
        bodyColor = color;
        invalidate();
    }

    public void setTalking(boolean value) {
        talking = value;
        invalidate();
    }

    /**
     * A short celebration: Bobo jumps and throws his arms up. Used when a child
     * answers correctly, so praise is something they can see as well as hear.
     */
    public void cheer() {
        if (cheerAnim != null) {
            cheerAnim.cancel();
        }
        cheerAnim = ValueAnimator.ofFloat(0f, 1f, 0.85f, 1f, 0f);
        cheerAnim.setDuration(900);
        cheerAnim.setInterpolator(new LinearInterpolator());
        cheerAnim.addUpdateListener(a -> {
            cheer = (float) a.getAnimatedValue();
            happy = cheer > 0.4f;
            invalidate();
        });
        cheerAnim.start();
    }

    /** True while the celebration animation is running. */
    public boolean isCheering() {
        return cheerAnim != null && cheerAnim.isRunning();
    }

    private void startAnimations() {
        bobAnim = ValueAnimator.ofFloat(0f, 1f);
        bobAnim.setDuration(1400);
        bobAnim.setRepeatCount(ValueAnimator.INFINITE);
        bobAnim.setRepeatMode(ValueAnimator.REVERSE);
        bobAnim.setInterpolator(new LinearInterpolator());
        bobAnim.addUpdateListener(a -> {
            bob = (float) a.getAnimatedValue();
            if (talking) {
                mouth = 1f - mouth;
            }
            invalidate();
        });
        bobAnim.start();

        // Blink every few seconds for a lively, friendly face.
        blinkAnim = ValueAnimator.ofFloat(0f, 1f, 0f);
        blinkAnim.setDuration(320);
        blinkAnim.setStartDelay(2600);
        blinkAnim.setRepeatCount(ValueAnimator.INFINITE);
        blinkAnim.addUpdateListener(a -> {
            blink = (float) a.getAnimatedValue();
            invalidate();
        });
        blinkAnim.start();
    }

    @Override
    protected void onDraw(Canvas canvas) {
        super.onDraw(canvas);
        float w = getWidth();
        float h = getHeight();
        float cx = w / 2f;
        float unit = Math.min(w, h) / 100f;
        canvas.translate(0, (bob - 0.5f) * unit * 4f - cheer * unit * 10f);

        int darkBody = Util.darker(bodyColor, 0.75f);
        int lightBody = Color.WHITE;

        // ---- Antenna ----
        paint.setColor(darkBody);
        paint.setStrokeWidth(unit * 3.4f);
        paint.setStrokeCap(Paint.Cap.ROUND);
        canvas.drawLine(cx, h * 0.28f, cx, h * 0.14f, paint);
        paint.setColor(0xFFFF4081);
        canvas.drawCircle(cx, h * 0.11f, unit * 5.5f, paint);
        paint.setColor(0x55FFFFFF);
        canvas.drawCircle(cx - unit * 1.6f, h * 0.11f - unit * 1.6f, unit * 1.8f, paint);

        // ---- Arms ----
        paint.setColor(bodyColor);
        float armWave = (float) Math.sin((bob * Math.PI)) * unit * 5f;
        // Arms rise towards the head while cheering.
        float armLift = cheer * unit * 22f;
        oval.set(cx - w * 0.46f, h * 0.60f - armWave - armLift,
                cx - w * 0.30f, h * 0.76f - armWave - armLift);
        canvas.drawRoundRect(oval, unit * 6f, unit * 6f, paint);
        oval.set(cx + w * 0.30f, h * 0.60f + armWave - armLift,
                cx + w * 0.46f, h * 0.76f + armWave - armLift);
        canvas.drawRoundRect(oval, unit * 6f, unit * 6f, paint);

        // ---- Head ----
        paint.setColor(lightBody);
        oval.set(cx - w * 0.30f, h * 0.24f, cx + w * 0.30f, h * 0.59f);
        canvas.drawRoundRect(oval, unit * 12f, unit * 12f, paint);
        paint.setStyle(Paint.Style.STROKE);
        paint.setStrokeWidth(unit * 2.2f);
        paint.setColor(bodyColor);
        canvas.drawRoundRect(oval, unit * 12f, unit * 12f, paint);
        paint.setStyle(Paint.Style.FILL);

        // ---- Eyes ----
        float eyeY = h * 0.39f;
        float eyeDx = w * 0.13f;
        float eyeR = unit * 7.5f;
        float lid = blink;
        for (int s = -1; s <= 1; s += 2) {
            float ex = cx + s * eyeDx;
            paint.setColor(0xFF0D1B2A);
            if (lid > 0.85f) {
                paint.setStrokeWidth(unit * 2.4f);
                paint.setStrokeCap(Paint.Cap.ROUND);
                canvas.drawLine(ex - eyeR, eyeY, ex + eyeR, eyeY, paint);
            } else {
                oval.set(ex - eyeR, eyeY - eyeR * (1f - lid), ex + eyeR, eyeY + eyeR * (1f - lid));
                canvas.drawOval(oval, paint);
                paint.setColor(Color.WHITE);
                canvas.drawCircle(ex - eyeR * 0.3f, eyeY - eyeR * 0.3f, eyeR * 0.32f, paint);
            }
        }

        // ---- Cheeks ----
        paint.setColor(0xFFFF8A80);
        canvas.drawCircle(cx - w * 0.21f, h * 0.47f, unit * 4.2f, paint);
        canvas.drawCircle(cx + w * 0.21f, h * 0.47f, unit * 4.2f, paint);

        // ---- Mouth ----
        float mouthW = w * 0.20f;
        if (happy) {
            // A wide open smile: an arc with the corners turned up.
            paint.setColor(0xFF0D1B2A);
            paint.setStyle(Paint.Style.STROKE);
            paint.setStrokeWidth(unit * 2.6f);
            paint.setStrokeCap(Paint.Cap.ROUND);
            RectF smile = new RectF(cx - mouthW, h * 0.455f,
                    cx + mouthW, h * 0.545f);
            canvas.drawArc(smile, 20f, 140f, false, paint);
            paint.setStyle(Paint.Style.FILL);
        } else {
            float mouthH = unit * (2f + (talking ? mouth * 5f : 1.5f));
            paint.setColor(0xFF0D1B2A);
            oval.set(cx - mouthW, h * 0.485f, cx + mouthW, h * 0.485f + mouthH);
            canvas.drawRoundRect(oval, unit * 4f, unit * 4f, paint);
        }

        // ---- Body ----
        paint.setColor(bodyColor);
        oval.set(cx - w * 0.24f, h * 0.61f, cx + w * 0.24f, h * 0.92f);
        path.reset();
        path.addRoundRect(oval, unit * 10f, unit * 10f, Path.Direction.CW);
        canvas.drawPath(path, paint);

        // Belly screen with a little heart
        paint.setColor(0xFFFFFDE7);
        oval.set(cx - w * 0.13f, h * 0.68f, cx + w * 0.13f, h * 0.84f);
        canvas.drawRoundRect(oval, unit * 5f, unit * 5f, paint);
        drawHeart(canvas, cx, h * 0.755f, unit * 6.5f, 0xFFFF4081);

        // ---- Feet ----
        paint.setColor(darkBody);
        oval.set(cx - w * 0.20f, h * 0.90f, cx - w * 0.04f, h * 0.97f);
        canvas.drawRoundRect(oval, unit * 4f, unit * 4f, paint);
        oval.set(cx + w * 0.04f, h * 0.90f, cx + w * 0.20f, h * 0.97f);
        canvas.drawRoundRect(oval, unit * 4f, unit * 4f, paint);
    }

    private void drawHeart(Canvas canvas, float cx, float cy, float size, int color) {
        paint.setColor(color);
        Path heart = new Path();
        float half = size / 2f;
        heart.moveTo(cx, cy + half);
        heart.cubicTo(cx - size * 1.1f, cy - half * 0.3f, cx - half, cy - size, cx, cy - half * 0.5f);
        heart.cubicTo(cx + half, cy - size, cx + size * 1.1f, cy - half * 0.3f, cx, cy + half);
        heart.close();
        canvas.drawPath(heart, paint);
    }

    @Override
    protected void onDetachedFromWindow() {
        super.onDetachedFromWindow();
        if (bobAnim != null) {
            bobAnim.cancel();
        }
        if (blinkAnim != null) {
            blinkAnim.cancel();
        }
    }
}
