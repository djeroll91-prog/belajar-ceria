package com.ceria.belajar;

import android.animation.ValueAnimator;
import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.RectF;
import android.view.View;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;

/** Falling confetti and floating stars shown when a child gets something right. */
public class CelebrationView extends View {

    private static final String[] EMOJIS = {"\u2B50", "\uD83C\uDF89", "\uD83C\uDF88", "\uD83E\uDD47",
            "\uD83C\uDF1F", "\uD83D\uDC4F", "\uD83C\uDF81", "\uD83C\uDF08"};
    private static final int[] COLORS = {
            0xFFFF5252, 0xFFFFD740, 0xFF69F0AE, 0xFF40C4FF, 0xFFE040FB, 0xFFFFAB40};

    private static class Piece {
        float x, y, vy, vx, size, rot, rotSpeed, gravity;
        int color;
        String emoji;
        boolean isEmoji;
    }

    private final List<Piece> pieces = new ArrayList<>();
    private final Paint paint = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Paint textPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Random random = new Random();
    private final RectF rect = new RectF();
    private ValueAnimator animator;
    private float progress;

    public CelebrationView(Context context) {
        super(context);
        textPaint.setTextAlign(Paint.Align.CENTER);
        setLayerType(View.LAYER_TYPE_HARDWARE, null);
    }

    /** Fires a burst of confetti, then removes itself from the parent. */
    public void burst() {
        pieces.clear();
        int count = 42;
        float w = getWidth() == 0 ? getResources().getDisplayMetrics().widthPixels : getWidth();
        for (int i = 0; i < count; i++) {
            Piece p = new Piece();
            p.x = random.nextFloat() * w;
            p.y = -random.nextFloat() * 300f - 40f;
            p.vy = 300f + random.nextFloat() * 500f;
            p.vx = (random.nextFloat() - 0.5f) * 200f;
            p.size = 26f + random.nextFloat() * 34f;
            p.rot = random.nextFloat() * 360f;
            p.rotSpeed = (random.nextFloat() - 0.5f) * 480f;
            p.gravity = 260f + random.nextFloat() * 240f;
            p.isEmoji = random.nextBoolean();
            p.emoji = EMOJIS[random.nextInt(EMOJIS.length)];
            p.color = COLORS[random.nextInt(COLORS.length)];
            pieces.add(p);
        }
        progress = 0f;
        if (animator != null) {
            animator.cancel();
        }
        animator = ValueAnimator.ofFloat(0f, 1f);
        animator.setDuration(2600);
        animator.addUpdateListener(a -> {
            progress = (float) a.getAnimatedValue();
            invalidate();
        });
        animator.addListener(new android.animation.AnimatorListenerAdapter() {
            @Override
            public void onAnimationEnd(android.animation.Animator animation) {
                android.view.ViewParent parent = getParent();
                if (parent instanceof android.view.ViewGroup) {
                    ((android.view.ViewGroup) parent).removeView(CelebrationView.this);
                }
            }
        });
        animator.start();
    }

    @Override
    protected void onDraw(Canvas canvas) {
        super.onDraw(canvas);
        float alpha = progress > 0.75f ? (1f - progress) / 0.25f : 1f;
        for (Piece p : pieces) {
            float t = progress * 2.6f;
            float y = p.y + p.vy * t + 0.5f * p.gravity * t * t;
            float x = p.x + p.vx * t;
            if (y > getHeight() + 80f) {
                continue;
            }
            canvas.save();
            canvas.translate(x, y);
            canvas.rotate(p.rot + p.rotSpeed * t);
            if (p.isEmoji) {
                textPaint.setTextSize(p.size);
                textPaint.setAlpha((int) (255 * alpha));
                canvas.drawText(p.emoji, 0, 0, textPaint);
            } else {
                paint.setColor(p.color);
                paint.setAlpha((int) (255 * alpha));
                rect.set(-p.size / 2f, -p.size / 4f, p.size / 2f, p.size / 4f);
                canvas.drawRoundRect(rect, p.size / 5f, p.size / 5f, paint);
            }
            canvas.restore();
        }
    }
}
