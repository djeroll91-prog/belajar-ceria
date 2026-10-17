package com.ceria.belajar;

import android.graphics.drawable.GradientDrawable;
import android.view.Gravity;
import android.view.View;
import android.view.ViewGroup;
import android.widget.FrameLayout;
import android.widget.GridLayout;
import android.widget.LinearLayout;
import android.widget.TextView;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Random;

/**
 * A quiz over one unit, or over a review queue of weak items.
 *
 * Every answer is written back through {@link Progress#recordAnswer}, which is
 * what makes the review queue improve as the child plays: items answered wrongly
 * drop to box 0 and come back sooner.
 */
public class QuizScreen extends BaseScreen {

    /** One question: a prompt to say, and four things to choose between. */
    static class Question {
        final Content.Unit unit;
        final Content.Item answer;
        final String prompt;
        final List<Content.Item> options;

        Question(Content.Unit unit, Content.Item answer, String prompt, List<Content.Item> options) {
            this.unit = unit;
            this.answer = answer;
            this.prompt = prompt;
            this.options = options;
        }
    }

    private final List<Question> questions = new ArrayList<>();
    private final String heading;
    private int index;
    private int score;
    private boolean locked;
    private FrameLayout stage;
    private LinearLayout progressBar;
    private TextView counter;

    /** A quiz over a unit's own items. */
    public QuizScreen(MainActivity host, Content.Unit unit) {
        super(host);
        this.heading = unit.emoji + " " + unit.title(lang);
        build(host, unit, Math.min(8, unit.items.size()));
    }

    /** A quiz over the items a child keeps getting wrong. */
    public QuizScreen(MainActivity host, List<Review.Candidate> queue) {
        super(host);
        this.heading = t("Ulang kaji", "Review");
        Random random = new Random();
        for (Review.Candidate c : queue) {
            questions.add(questionFor(c.unit, c.item, random));
        }
    }

    private void build(MainActivity host, Content.Unit unit, int count) {
        Random random = new Random();
        List<Content.Item> pool = new ArrayList<>(unit.items);
        Collections.shuffle(pool, random);
        for (int i = 0; i < count && i < pool.size(); i++) {
            questions.add(questionFor(unit, pool.get(i), random));
        }
    }

    /**
     * Builds one question. The three wrong answers are drawn from the same unit,
     * so the child has to tell similar things apart rather than spotting the odd
     * one out.
     */
    private Question questionFor(Content.Unit unit, Content.Item answer, Random random) {
        List<Content.Item> distractors = new ArrayList<>();
        for (Content.Item item : unit.items) {
            if (item != answer) {
                distractors.add(item);
            }
        }
        Collections.shuffle(distractors, random);
        List<Content.Item> options = new ArrayList<>();
        options.add(answer);
        for (int i = 0; i < 3 && i < distractors.size(); i++) {
            options.add(distractors.get(i));
        }
        Collections.shuffle(options, random);
        String label = Content.spokenOf(answer, unit.kind, lang);
        return new Question(unit, answer, t("Yang mana ", "Which one is ") + label + "?", options);
    }

    @Override
    protected String titleText() {
        return heading;
    }

    @Override
    protected View buildBody() {
        LinearLayout column = vertical();

        if (questions.isEmpty()) {
            column.addView(Util.text(host, t("Tiada soalan", "No questions"), 16, theme.inkSoft()));
            return scrollWrap(column);
        }

        LinearLayout top = new LinearLayout(host);
        top.setOrientation(LinearLayout.HORIZONTAL);
        top.setGravity(Gravity.CENTER_VERTICAL);

        TextView prompt = Util.text(host, questions.get(0).prompt, 17, theme.ink());
        prompt.setTypeface(null, android.graphics.Typeface.BOLD);
        prompt.setGravity(Gravity.START);
        top.addView(prompt, new LinearLayout.LayoutParams(0,
                ViewGroup.LayoutParams.WRAP_CONTENT, 1f));
        promptView = prompt;

        counter = Util.text(host, "", 13, theme.inkSoft());
        top.addView(counter);
        column.addView(top, matchWrap());

        progressBar = new LinearLayout(host);
        progressBar.setOrientation(LinearLayout.HORIZONTAL);
        progressBar.setBackground(Util.rounded(theme.surface2(), dp(6)));
        LinearLayout.LayoutParams barLp = new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, dp(12));
        barLp.setMargins(0, dp(10), 0, dp(10));
        column.addView(progressBar, barLp);

        stage = new FrameLayout(host);
        column.addView(stage, new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, 0, 1f));

        showQuestion();
        return column;
    }

    private TextView promptView;

    private void showQuestion() {
        if (index >= questions.size()) {
            showResult();
            return;
        }
        locked = false;
        Question q = questions.get(index);
        promptView.setText(q.prompt);
        counter.setText((index + 1) + " / " + questions.size());

        progressBar.removeAllViews();
        if (index > 0) {
            View fill = new View(host);
            fill.setBackground(Util.rounded(Theme.GOOD, dp(6)));
            progressBar.addView(fill, new LinearLayout.LayoutParams(0, dp(12),
                    index / (float) questions.size()));
            progressBar.addView(new View(host), new LinearLayout.LayoutParams(0, dp(12),
                    (questions.size() - index) / (float) questions.size()));
        }

        stage.removeAllViews();
        LinearLayout column = new LinearLayout(host);
        column.setOrientation(LinearLayout.VERTICAL);

        TextView listen = Util.text(host, "\uD83D\uDD0A " + t("Dengar", "Listen"), 17, theme.onAccent());
        listen.setTypeface(null, android.graphics.Typeface.BOLD);
        listen.setGravity(Gravity.CENTER);
        listen.setBackground(Util.rounded(theme.accent(), dp(20)));
        listen.setElevation(dp(3));
        listen.setPadding(0, dp(14), 0, dp(14));
        listen.setOnClickListener(v -> {
            host.sfx().tap();
            say(q);
        });
        column.addView(listen, new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT));

        GridLayout grid = new GridLayout(host);
        grid.setColumnCount(2);
        LinearLayout.LayoutParams gridLp = new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, 0, 1f);
        gridLp.setMargins(0, dp(12), 0, 0);
        column.addView(grid, gridLp);

        for (Content.Item option : q.options) {
            grid.addView(optionTile(q, option), optionParams());
        }

        stage.addView(column, new FrameLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT));
        say(q);
    }

    private GridLayout.LayoutParams optionParams() {
        GridLayout.LayoutParams lp = new GridLayout.LayoutParams();
        lp.width = 0;
        lp.height = 0;
        lp.rowSpec = GridLayout.spec(GridLayout.UNDEFINED, 1f);
        lp.columnSpec = GridLayout.spec(GridLayout.UNDEFINED, 1f);
        lp.setMargins(dp(5), dp(5), dp(5), dp(5));
        return lp;
    }

    private View optionTile(Question q, Content.Item option) {
        LinearLayout tile = new LinearLayout(host);
        tile.setOrientation(LinearLayout.VERTICAL);
        tile.setGravity(Gravity.CENTER);
        tile.setPadding(dp(8), dp(12), dp(8), dp(12));
        tile.setBackground(Util.roundedStroke(theme.surface(), dp(22), theme.line(), dp(3)));
        tile.setElevation(dp(3));
        tile.setClickable(true);

        if (Content.isColorItem(option)) {
            View swatch = new View(host);
            GradientDrawable d = new GradientDrawable();
            d.setShape(GradientDrawable.OVAL);
            d.setColor(Content.colorOf(option));
            d.setStroke(dp(4), 0x33000000);
            swatch.setBackground(d);
            tile.addView(swatch, new LinearLayout.LayoutParams(dp(72), dp(72)));
        } else {
            TextView glyph = Util.text(host, Content.displayOf(option, lang), 40, theme.ink());
            glyph.setTypeface(null, android.graphics.Typeface.BOLD);
            tile.addView(glyph);
        }

        // A letter shows only the letter. Printing "House" beside "H" would both
        // give the pairing away and teach the wrong sound, since the quiz asks for
        // the letter's name ("ei"), not the picture's.
        if (!Content.isLetterItem(option)) {
            TextView name = Util.text(host, option.name(lang), 15, theme.ink());
            name.setTypeface(null, android.graphics.Typeface.BOLD);
            tile.addView(name);
        }

        // Tags let the result step find and highlight the right answer again.
        tile.setTag(option);
        tile.setOnClickListener(v -> answer(q, option, tile));
        return tile;
    }

    private void answer(Question q, Content.Item chosen, View tile) {
        if (locked) {
            return;
        }
        locked = true;
        boolean correct = chosen == q.answer;
        host.progress().recordAnswer(q.unit.id, Progress.keyOf(q.answer), correct);

        if (correct) {
            score++;
            host.correctFeedback(tile);
            tile.setBackground(Util.rounded(theme.goodFor(), dp(22)));
            host.mouthSay(praise());
            host.voice().speakThen(lang, praise(), 1f, this::advance);
        } else {
            host.wrongFeedback(tile);
            tile.setBackground(Util.rounded(theme.badFor(), dp(22)));
            // The right answer is shown rather than only marked wrong, so a wrong
            // tap still teaches the pairing.
            revealAnswer();
            host.mouthSay(t("Cuba lagi ya", "Try again"));
            host.voice().speakThen(lang, t("Cuba lagi ya", "Try again"), 1f, this::advance);
        }
    }

    private void revealAnswer() {
        GridLayout grid = null;
        for (int i = 0; i < stage.getChildCount(); i++) {
            View child = stage.getChildAt(i);
            if (child instanceof LinearLayout) {
                for (int j = 0; j < ((LinearLayout) child).getChildCount(); j++) {
                    View inner = ((LinearLayout) child).getChildAt(j);
                    if (inner instanceof GridLayout) {
                        grid = (GridLayout) inner;
                    }
                }
            }
        }
        if (grid == null) {
            return;
        }
        Question q = questions.get(index);
        for (int i = 0; i < grid.getChildCount(); i++) {
            View tile = grid.getChildAt(i);
            if (tile.getTag() == q.answer) {
                tile.setBackground(Util.rounded(theme.goodFor(), dp(22)));
            }
        }
    }

    private void advance() {
        index++;
        showQuestion();
    }

    private void say(Question q) {
        host.voice().speak(lang, Content.spokenOf(q.answer, q.unit.kind, lang));
    }

    private String praise() {
        return t("Bagus sekali!", "Great job!");
    }

    private void showResult() {
        stage.removeAllViews();
        progressBar.removeAllViews();
        promptView.setText(t("Siap!", "Done!"));
        counter.setText(score + " / " + questions.size());

        LinearLayout column = new LinearLayout(host);
        column.setOrientation(LinearLayout.VERTICAL);
        column.setGravity(Gravity.CENTER);

        int percent = score * 100 / Math.max(1, questions.size());
        int stars = percent >= 90 ? 3 : percent >= 70 ? 2 : percent >= 40 ? 1 : 0;

        TextView big = Util.text(host, stars == 3 ? "\uD83C\uDFC6"
                : stars == 2 ? "\uD83C\uDF89" : "\uD83D\uDCAA", 64, theme.ink());
        column.addView(big);

        TextView result = Util.text(host, score + " / " + questions.size(), 34, theme.ink());
        result.setTypeface(null, android.graphics.Typeface.BOLD);
        result.setPadding(0, dp(6), 0, dp(4));
        column.addView(result);

        StringBuilder starRow = new StringBuilder();
        for (int i = 0; i < stars; i++) {
            starRow.append("\u2B50");
        }
        column.addView(Util.text(host, starRow.length() == 0 ? "\u2014" : starRow.toString(),
                24, Theme.GOLD));

        LinearLayout row = new LinearLayout(host);
        row.setOrientation(LinearLayout.HORIZONTAL);
        row.setGravity(Gravity.CENTER);
        row.setPadding(0, dp(18), 0, 0);

        row.addView(bigButton(t("Main lagi", "Play again"), theme.accent(), this::restart));
        row.addView(bigButton(t("Utama", "Home"), theme.surface2(), host::showHome));
        column.addView(row);

        stage.addView(column, new FrameLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT));

        if (stars >= 2) {
            host.celebrate(host.praise());
        }
        host.mouthSay(praise());
        host.voice().speak(lang, praise());
    }

    private View bigButton(String label, int color, Runnable action) {
        TextView b = Util.text(host, label, 17, theme.onAccent());
        b.setTypeface(null, android.graphics.Typeface.BOLD);
        b.setGravity(Gravity.CENTER);
        b.setBackground(Util.rounded(color, dp(20)));
        b.setElevation(dp(3));
        b.setPadding(dp(20), dp(15), dp(20), dp(15));
        b.setOnClickListener(v -> {
            host.tapFeedback(v);
            v.postDelayed(action, 90);
        });
        LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT);
        lp.setMargins(dp(6), 0, dp(6), 0);
        b.setLayoutParams(lp);
        return b;
    }

    private void restart() {
        index = 0;
        score = 0;
        showQuestion();
    }
}
