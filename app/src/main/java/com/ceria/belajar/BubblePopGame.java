package com.ceria.belajar;

import android.animation.ObjectAnimator;
import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.RadialGradient;
import android.graphics.Shader;
import android.view.Gravity;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup;
import android.widget.FrameLayout;
import android.widget.LinearLayout;
import android.widget.TextView;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;

/**
 * Floating bubbles drift up the screen, each holding a letter or a number. The
 * child taps the bubbles that match the target the robot asks for.
 *
 * A moving target trains quick recognition of a shape, which is the skill behind
 * fluent reading: the child has to spot "B" among other letters fast, not sound
 * out every symbol from scratch.
 */
public class BubblePopGame extends BaseScreen {

    private static final int TOTAL_ROUNDS = 6;
    private static final int BUBBLES = 6;

    private final Random random = new Random();
    private int round;
    private int score;
    private int popped;
    private Content.Item target;
    private BubbleField field;
    private TextView promptView;
    private TextView scoreView;
    private TextView hintView;
    private boolean finished;

    public BubblePopGame(MainActivity host) {
        super(host);
    }

    @Override
    protected String titleText() {
        return t("Bubble Pop \uD83E\uDEE7", "Letup Gelembung \uD83E\uDEE7");
    }

    @Override
    protected View buildBody() {
        LinearLayout column = vertical();
        column.setGravity(Gravity.CENTER_HORIZONTAL);

        scoreView = Util.text(host, "", 17, 0xFF4A148C);
        column.addView(scoreView, matchWrap());

        promptView = Util.text(host, "", 24, 0xFF00695C);
        promptView.setTypeface(null, android.graphics.Typeface.BOLD);
        promptView.setPadding(0, dp(6), 0, dp(6));
        column.addView(promptView, matchWrap());

        hintView = Util.text(host, "", 16, 0xFFAD1457);
        column.addView(hintView, matchWrap());

        field = new BubbleField(host);
        LinearLayout.LayoutParams fieldLp = new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, 0, 1f);
        fieldLp.setMargins(0, dp(8), 0, 0);
        column.addView(field, fieldLp);

        startGame();
        return column;
    }

    private void startGame() {
        round = 0;
        score = 0;
        finished = false;
        host.progress().registerGamePlayed();
        nextRound();
    }

    private void nextRound() {
        if (round >= TOTAL_ROUNDS) {
            finish();
            return;
        }
        round++;
        popped = 0;
        scoreView.setText(t("Round ", "Pusingan ") + round + " / " + TOTAL_ROUNDS
                + "   \u2B50 " + score);

        List<Content.Item> pool = new ArrayList<>(Content.LETTERS);
        pool.addAll(Content.NUMBERS.subList(0, 10));
        target = pool.get(random.nextInt(pool.size()));

        promptView.setText(t("Pop every ", "Letupkan semua ") + glyph(target) + "!");
        hintView.setText(t("Tap the matching bubbles", "Tekan gelembung yang sama"));
        host.voice().speak(lang, speaksAs(target));

        field.setBubbles(makeBubbles(pool));
    }

    /** Letters are asked for by their character; numbers by their name. */
    private String speaksAs(Content.Item item) {
        if (Content.LETTERS.contains(item)) {
            return Content.letterGlyph(item);
        }
        return item.spokenText(lang);
    }

    /** The single character drawn inside a bubble: the letter or the digit. */
    private String glyph(Content.Item item) {
        if (Content.LETTERS.contains(item)) {
            return Content.letterGlyph(item);
        }
        return item.symbol;
    }

    private List<Bubble> makeBubbles(List<Content.Item> pool) {
        List<Bubble> out = new ArrayList<>();
        // Two bubbles carry the target, so "pop every one" is actually possible,
        // and the rest are near misses that have to be told apart from it.
        out.add(newBubble(target));
        out.add(newBubble(target));
        int guard = 0;
        while (out.size() < BUBBLES && guard++ < 200) {
            Content.Item candidate = pool.get(random.nextInt(pool.size()));
            if (!glyph(candidate).equalsIgnoreCase(glyph(target))) {
                out.add(newBubble(candidate));
            }
        }
        java.util.Collections.shuffle(out, random);
        return out;
    }

    private Bubble newBubble(Content.Item item) {
        Bubble b = new Bubble();
        b.item = item;
        b.label = glyph(item);
        b.radiusDp = 34 + random.nextInt(10);
        b.x = 0.12f + random.nextFloat() * 0.76f;
        b.y = random.nextFloat();
        b.speed = 0.16f + random.nextFloat() * 0.14f;
        b.phase = random.nextFloat() * 6.28f;
        b.color = Util.FUN_COLORS[random.nextInt(Util.FUN_COLORS.length)];
        return b;
    }

    private void onBubbleTapped(Bubble b) {
        if (finished) {
            return;
        }
        if (!b.label.equalsIgnoreCase(glyph(target))) {
            host.progress().resetStreak();
            hintView.setText(t("That is ", "Itu ") + b.label + t(". Look for ", ". Cari ")
                    + glyph(target) + "!");
            host.sfx().wrong();
            b.wobble = 1f;
            field.invalidate();
            return;
        }

        score += 5;
        popped++;
        host.progress().addPoints(5);
        host.progress().addCorrect();
        host.progress().onCorrectAnswer();
        host.progress().earnedBadges();
        host.updateHeader();
        host.sfx().pop();
        scoreView.setText(t("Round ", "Pusingan ") + round + " / " + TOTAL_ROUNDS
                + "   \u2B50 " + score);
        hintView.setText(t("Pop! ", "Pop! ") + popped + t(" found", " dijumpai"));

        if (popped >= 2) {
            host.progress().addStar();
            host.progress().earnedBadges();
            host.updateHeader();
            host.celebrate(t("Sharp eyes!", "Mata yang tajam!"));
            field.postDelayed(this::nextRound, 900);
        }
    }

    private void finish() {
        finished = true;
        field.clear();
        promptView.setText(t("You scored ", "Markah kamu ") + score + " \u2B50");
        hintView.setText(t("Bubble champion! \uD83C\uDFC6", "Juara gelembung! \uD83C\uDFC6"));
        host.celebrate(t("Fantastic!", "Hebat sekali!"));
        field.setFinishButton(() -> startGame());
    }

    /** One drifting bubble. */
    private static class Bubble {
        Content.Item item;
        String label;
        float x;
        float y;
        float speed;
        float phase;
        float wobble;
        int radiusDp;
        int color;
    }

    /** Draws the bubbles and drives their motion. */
    private class BubbleField extends FrameLayout {
        private final Paint paint = new Paint(Paint.ANTI_ALIAS_FLAG);
        private final Paint textPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
        private final List<Bubble> bubbles = new ArrayList<>();
        private ObjectAnimator animator;
        private Runnable finishAction;
        private float phaseTick;
        private final TextView replay = Util.text(host, "\uD83D\uDD01 "
                + t("Play again", "Main lagi"), 17, Color.WHITE);

        BubbleField(Context context) {
            super(context);
            textPaint.setColor(Color.WHITE);
            textPaint.setTextAlign(Paint.Align.CENTER);
            textPaint.setFakeBoldText(true);
            textPaint.setShadowLayer(dp(2), 0, dp(1), 0x66000000);

            replay.setBackground(Util.rounded(0xFF7C4DFF, dp(18)));
            replay.setPadding(dp(18), dp(10), dp(18), dp(10));
            replay.setVisibility(GONE);
            replay.setOnClickListener(v -> {
                host.tapFeedback(v);
                if (finishAction != null) {
                    finishAction.run();
                }
            });
            FrameLayout.LayoutParams lp = new FrameLayout.LayoutParams(
                    ViewGroup.LayoutParams.WRAP_CONTENT,
                    ViewGroup.LayoutParams.WRAP_CONTENT);
            lp.gravity = Gravity.CENTER;
            addView(replay, lp);
        }

        void setBubbles(List<Bubble> next) {
            bubbles.clear();
            bubbles.addAll(next);
            start();
        }

        void setFinishButton(Runnable action) {
            finishAction = action;
            replay.setVisibility(VISIBLE);
        }

        void clear() {
            bubbles.clear();
            stop();
            invalidate();
        }

        private void start() {
            stop();
            animator = ObjectAnimator.ofFloat(this, "phaseTick", 0f, 1f);
            animator.setDuration(1400);
            animator.setRepeatCount(ObjectAnimator.INFINITE);
            animator.addUpdateListener(a -> invalidate());
            animator.start();
        }

        private void stop() {
            if (animator != null) {
                animator.cancel();
                animator = null;
            }
        }

        /** Only used as an animation clock: the setter repaints the bubbles. */
        public void setPhaseTick(float value) {
            phaseTick = value;
        }

        public float getPhaseTick() {
            return phaseTick;
        }

        @Override
        protected void onDetachedFromWindow() {
            stop();
            super.onDetachedFromWindow();
        }

        @Override
        protected void dispatchDraw(Canvas canvas) {
            long now = android.os.SystemClock.uptimeMillis();
            for (Bubble b : bubbles) {
                float radius = Util.dp(getContext(), b.radiusDp);
                float cx = b.x * getWidth();
                // Drift up, wrap around at the top, sway gently side to side.
                float travel = (now / 1000f) * b.speed;
                float ny = (b.y + travel) % 1f;
                float cy = getHeight() - ny * getHeight();
                cx += (float) Math.sin(now / 620f + b.phase) * Util.dp(getContext(), 12);

                if (b.wobble > 0f) {
                    cx += (float) Math.sin(now / 40f) * Util.dp(getContext(), 6) * b.wobble;
                    b.wobble = Math.max(0f, b.wobble - 0.05f);
                }

                paint.setShader(new RadialGradient(cx - radius * 0.3f, cy - radius * 0.35f,
                        radius * 1.25f,
                        new int[]{0xFFFFFFFF, b.color, Util.darker(b.color, 0.75f)},
                        new float[]{0f, 0.45f, 1f}, Shader.TileMode.CLAMP));
                canvas.drawCircle(cx, cy, radius, paint);
                paint.setShader(null);

                textPaint.setTextSize(radius * 0.95f);
                canvas.drawText(b.label, cx, cy + radius * 0.34f, textPaint);
            }
            super.dispatchDraw(canvas);
        }

        @Override
        public boolean onTouchEvent(MotionEvent event) {
            if (event.getAction() != MotionEvent.ACTION_DOWN) {
                return true;
            }
            for (Bubble b : bubbles) {
                float radius = Util.dp(getContext(), b.radiusDp);
                float cx = b.x * getWidth();
                long now = android.os.SystemClock.uptimeMillis();
                float travel = (now / 1000f) * b.speed;
                float ny = (b.y + travel) % 1f;
                float cy = getHeight() - ny * getHeight();
                cx += (float) Math.sin(now / 620f + b.phase) * Util.dp(getContext(), 12);
                float dx = event.getX() - cx;
                float dy = event.getY() - cy;
                if (dx * dx + dy * dy <= radius * radius * 1.4f) {
                    onBubbleTapped(b);
                    return true;
                }
            }
            return true;
        }
    }
}
