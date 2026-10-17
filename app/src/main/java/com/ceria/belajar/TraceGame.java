package com.ceria.belajar;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.DashPathEffect;
import android.graphics.Paint;
import android.graphics.Path;
import android.graphics.PathMeasure;
import android.view.Gravity;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup;
import android.widget.LinearLayout;
import android.widget.TextView;

import java.util.ArrayList;
import java.util.List;

/**
 * Trace the shape of a letter or a number with a finger.
 *
 * Writing is how the shape of a letter becomes automatic, and it is the step
 * between recognising "B" and being able to produce it. The guide is drawn from
 * the real glyph outline, and the app measures how much of that outline the
 * child actually covered before it says well done, so the praise means something.
 */
public class TraceGame extends BaseScreen {

    public enum Mode { LETTER, NUMBER, JAWI }

    private final Mode mode;
    private int index;
    private int score;
    private TracePad pad;
    private TextView titleView;
    private TextView scoreView;
    private TextView hintView;
    private TextView replay;

    public TraceGame(MainActivity host) {
        this(host, Mode.LETTER);
    }

    public TraceGame(MainActivity host, Mode mode) {
        super(host);
        this.mode = mode;
    }

    @Override
    protected String titleText() {
        switch (mode) {
            case NUMBER:
                return t("Trace Numbers \u270F\uFE0F", "Tulis Nombor \u270F\uFE0F");
            case JAWI:
                return t("Trace Jawi \u270F\uFE0F", "Tulis Jawi \u270F\uFE0F");
            default:
                return t("Trace Letters \u270F\uFE0F", "Tulis Huruf \u270F\uFE0F");
        }
    }

    @Override
    protected View buildBody() {
        LinearLayout column = vertical();
        column.setGravity(Gravity.CENTER_HORIZONTAL);

        scoreView = Util.text(host, "", 17, 0xFF4A148C);
        column.addView(scoreView, matchWrap());

        titleView = Util.text(host, "", 20, 0xFF00695C);
        titleView.setPadding(0, dp(2), 0, dp(4));
        column.addView(titleView, matchWrap());

        hintView = Util.text(host, t("Follow the dots with your finger",
                "Ikut titik-titik dengan jari"), 15, 0xFFAD1457);
        column.addView(hintView, matchWrap());

        pad = new TracePad(host);
        LinearLayout.LayoutParams padLp = new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, 0, 1f);
        padLp.setMargins(0, dp(8), 0, dp(8));
        column.addView(pad, padLp);

        LinearLayout actions = horizontal();
        actions.addView(bigButton("\uD83D\uDD01", t("Clear", "Padam"),
                0xFF5C6BC0, () -> pad.clear()), wrap());
        actions.addView(bigButton("\u27A1\uFE0F", t("Next", "Seterusnya"),
                0xFF66BB6A, this::next), wrap());
        column.addView(actions, wrap());

        replay = Util.text(host, "", 16, 0xFF4A148C);
        column.addView(replay, matchWrap());

        host.progress().registerGamePlayed();
        show();
        return column;
    }

    private List<Content.Item> items() {
        switch (mode) {
            case NUMBER:
                return Content.NUMBERS;
            case JAWI:
                return Content.JAWI;
            default:
                return Content.LETTERS;
        }
    }

    /** The character being traced: a letter glyph, a digit, or a Jawi letter. */
    private String glyph() {
        Content.Item item = items().get(index % items().size());
        if (mode == Mode.LETTER) {
            return Content.letterGlyph(item);
        }
        if (mode == Mode.JAWI) {
            return item.symbol;
        }
        return item.symbol;
    }

    private void show() {
        String glyph = glyph();
        pad.setGlyph(glyph);
        titleView.setText(t("Trace: ", "Tulis: ") + glyph);
        scoreView.setText(t("Points ", "Markah ") + score + " \u2B50");
        hintView.setText(t("Follow the dots with your finger",
                "Ikut titik-titik dengan jari"));
        if (replay != null) {
            replay.setText("");
        }
        host.voice().speak(lang, mode == Mode.JAWI
                ? items().get(index % items().size()).spokenText(lang)
                : glyph);
    }

    private void next() {
        index++;
        pad.clear();
        show();
    }

    /** Called by the pad once enough of the outline has been covered. */
    private void onTraced() {
        score += 15;
        host.progress().addPoints(15);
        host.progress().addStar();
        host.progress().addCorrect();
        host.progress().onCorrectAnswer();
        host.progress().earnedBadges();
        host.updateHeader();
        scoreView.setText(t("Points ", "Markah ") + score + " \u2B50");
        hintView.setText(t("Beautiful writing! \u2B50", "Cantik tulisan kamu! \u2B50"));
        if (replay != null) {
            replay.setText(t("Tap Next for another letter", "Tekan Seterusnya untuk huruf lain"));
        }
        host.celebrate(t("Well written!", "Pandai menulis!"));
        pad.postDelayed(this::next, 1200);
    }

    /** The drawing surface: shows the glyph guide and records the finger path. */
    private class TracePad extends View {
        private final Paint guidePaint = new Paint(Paint.ANTI_ALIAS_FLAG);
        private final Paint dotPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
        private final Paint inkPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
        private final Path glyphPath = new Path();
        private final List<float[]> samples = new ArrayList<>();
        private final List<float[]> ink = new ArrayList<>();
        private String glyph = "A";
        private boolean solved;
        private float startX = -1;
        private float startY = -1;

        TracePad(Context context) {
            super(context);
            setBackground(Util.roundedStroke(0xFFFFFFFF, dp(24), 0xFFB39DDB, dp(3)));

            guidePaint.setStyle(Paint.Style.STROKE);
            guidePaint.setStrokeWidth(dp(3));
            guidePaint.setColor(0x553F51B5);
            guidePaint.setPathEffect(new DashPathEffect(new float[]{dp(9), dp(9)}, 0));

            dotPaint.setStyle(Paint.Style.FILL);
            dotPaint.setColor(0xFF43A047);

            inkPaint.setStyle(Paint.Style.STROKE);
            inkPaint.setStrokeCap(Paint.Cap.ROUND);
            inkPaint.setStrokeJoin(Paint.Join.ROUND);
            inkPaint.setStrokeWidth(dp(16));
            inkPaint.setColor(0xCCFF6D6D);
        }

        void setGlyph(String value) {
            glyph = value;
            solved = false;
            clear();
            buildGuide();
        }

        void clear() {
            ink.clear();
            startX = -1;
            startY = -1;
            invalidate();
        }

        /** Samples the real outline of the glyph so coverage can be measured. */
        private void buildGuide() {
            samples.clear();
            glyphPath.reset();
            if (getWidth() == 0 || getHeight() == 0) {
                return;
            }
            float size = Math.min(getWidth(), getHeight()) * 0.68f;
            Paint textPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
            textPaint.setTextSize(size);
            textPaint.setTypeface(android.graphics.Typeface.DEFAULT_BOLD);
            textPaint.getTextPath(glyph, 0, glyph.length(), 0, 0, glyphPath);

            android.graphics.RectF bounds = new android.graphics.RectF();
            glyphPath.computeBounds(bounds, true);
            if (bounds.isEmpty()) {
                return;
            }
            // Centre the outline in the pad.
            float dx = getWidth() / 2f - bounds.centerX();
            float dy = getHeight() / 2f - bounds.centerY();
            glyphPath.offset(dx, dy);

            PathMeasure measure = new PathMeasure(glyphPath, false);
            float length = measure.getLength();
            if (length <= 0f) {
                return;
            }
            int steps = 140;
            float[] pos = new float[2];
            for (int i = 0; i < steps; i++) {
                if (measure.getPosTan(length * i / (steps - 1f), pos, null)) {
                    samples.add(new float[]{pos[0], pos[1]});
                }
            }
            if (!samples.isEmpty()) {
                startX = samples.get(0)[0];
                startY = samples.get(0)[1];
            }
        }

        @Override
        protected void onSizeChanged(int w, int h, int oldw, int oldh) {
            super.onSizeChanged(w, h, oldw, oldh);
            buildGuide();
        }

        @Override
        protected void onDraw(Canvas canvas) {
            super.onDraw(canvas);
            canvas.drawPath(glyphPath, guidePaint);

            if (startX >= 0) {
                canvas.drawCircle(startX, startY, dp(10), dotPaint);
            }
            for (int i = 1; i < ink.size(); i++) {
                float[] a = ink.get(i - 1);
                float[] b = ink.get(i);
                canvas.drawLine(a[0], a[1], b[0], b[1], inkPaint);
            }
        }

        @Override
        public boolean onTouchEvent(MotionEvent event) {
            switch (event.getActionMasked()) {
                case MotionEvent.ACTION_DOWN:
                    ink.clear();
                    ink.add(new float[]{event.getX(), event.getY()});
                    invalidate();
                    return true;
                case MotionEvent.ACTION_MOVE:
                    ink.add(new float[]{event.getX(), event.getY()});
                    invalidate();
                    return true;
                case MotionEvent.ACTION_UP:
                    ink.add(new float[]{event.getX(), event.getY()});
                    invalidate();
                    checkCoverage();
                    return true;
                default:
                    return super.onTouchEvent(event);
            }
        }

        /** Praise only when most of the outline has actually been drawn over. */
        private void checkCoverage() {
            if (solved || samples.isEmpty() || ink.size() < 8) {
                return;
            }
            float tolerance = dp(30);
            float toleranceSq = tolerance * tolerance;
            int covered = 0;
            for (float[] sample : samples) {
                for (float[] point : ink) {
                    float dx = sample[0] - point[0];
                    float dy = sample[1] - point[1];
                    if (dx * dx + dy * dy <= toleranceSq) {
                        covered++;
                        break;
                    }
                }
            }
            if (covered >= samples.size() * 0.55f) {
                solved = true;
                onTraced();
            }
        }
    }
}
