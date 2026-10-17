package com.ceria.belajar;

import android.graphics.Color;
import android.graphics.Typeface;
import android.view.Gravity;
import android.view.View;
import android.view.ViewGroup;
import android.widget.GridLayout;
import android.widget.LinearLayout;
import android.widget.ProgressBar;
import android.widget.TextView;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Random;

/**
 * Level quiz. The child must reach the level's pass score before the next level
 * unlocks, which stops them from skipping ahead without learning.
 */
public class QuizGame extends BaseScreen {

    private final Random random = new Random();
    private final Curriculum.Level level;
    private int round;
    private int score;
    private final int totalRounds;
    private Content.Item answer;
    private TextView promptView;
    private TextView progressView;
    private TextView feedbackView;
    private ProgressBar scoreBar;
    private GridLayout options;
    private final List<Content.Item> pool = new ArrayList<>();
    private boolean finished;

    public QuizGame(MainActivity host) {
        this(host, null);
    }

    public QuizGame(MainActivity host, Curriculum.Level level) {
        super(host);
        this.level = level;
        this.totalRounds = level == null ? 8 : Math.max(6, level.items.size() + 4);
        if (level != null) {
            pool.addAll(level.items);
        } else {
            pool.addAll(Content.LETTERS);
            pool.addAll(Content.NUMBERS.subList(0, 10));
            pool.addAll(Content.WORDS);
        }
    }

    @Override
    protected String titleText() {
        return level == null
                ? t("Quiz \uD83C\uDFAF", "Kuiz \uD83C\uDFAF")
                : level.emoji + " " + level.title(lang);
    }

    @Override
    protected View buildBody() {
        LinearLayout column = vertical();
        column.setGravity(Gravity.CENTER_HORIZONTAL);

        if (level != null) {
            TextView pass = Util.text(host, t("Pass score: ", "Markah lulus: ")
                    + level.passScore + " \uD83C\uDFC1", 15, 0xFF4A148C);
            column.addView(pass, matchWrap());
        }

        progressView = Util.text(host, "", 17, 0xFF4A148C);
        progressView.setPadding(0, dp(6), 0, dp(6));
        column.addView(progressView, matchWrap());

        scoreBar = new ProgressBar(host, null, android.R.attr.progressBarStyleHorizontal);
        scoreBar.setMax(level == null ? totalRounds * 10 : level.passScore);
        column.addView(scoreBar, new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, dp(14)));

        promptView = Util.text(host, "", 26, 0xFF00695C);
        promptView.setTypeface(Typeface.DEFAULT_BOLD);
        promptView.setPadding(0, dp(12), 0, dp(12));
        column.addView(promptView, matchWrap());

        options = new GridLayout(host);
        options.setColumnCount(2);
        column.addView(options, matchWrap());

        feedbackView = Util.text(host, "", 18, 0xFFAD1457);
        feedbackView.setPadding(0, dp(14), 0, 0);
        column.addView(feedbackView, matchWrap());

        startGame();
        return column;
    }

    private void startGame() {
        if (pool.isEmpty()) {
            pool.addAll(Content.COLOR_ITEMS);
        }
        round = 0;
        score = 0;
        finished = false;
        host.progress().registerGamePlayed();
        nextRound();
    }

    private void nextRound() {
        if (round >= totalRounds) {
            finish();
            return;
        }
        round++;
        progressView.setText(t("Question ", "Soalan ") + round + " / " + totalRounds
                + "   \u2B50 " + score);
        scoreBar.setProgress(level == null ? Math.min(score, totalRounds * 10)
                : Math.min(score, level.passScore));

        List<Content.Item> choices = new ArrayList<>();
        answer = pool.get(random.nextInt(pool.size()));
        choices.add(answer);
        while (choices.size() < Math.min(4, pool.size())) {
            Content.Item candidate = pool.get(random.nextInt(pool.size()));
            if (!choices.contains(candidate)) {
                choices.add(candidate);
            }
        }
        Collections.shuffle(choices);

        String prompt;
        if (Content.isColorItem(answer)) {
            prompt = t("Which colour is \"", "Warna apa \"") + answer.name(lang) + "\"?";
        } else if (Content.isScriptItem(answer)) {
            prompt = t("Which letter sounds like ", "Huruf mana berbunyi ") + answer.name(lang) + "?";
        } else {
            prompt = t("Which one is ", "Yang mana ") + answer.name(lang) + "?";
        }
        promptView.setText(prompt);
        feedbackView.setText("");
        host.mouthSay(prompt);

        options.removeAllViews();
        for (Content.Item item : choices) {
            options.addView(optionTile(item), tileParams());
        }
    }

    private GridLayout.LayoutParams tileParams() {
        GridLayout.LayoutParams lp = new GridLayout.LayoutParams();
        lp.width = 0;
        lp.height = dp(110);
        lp.columnSpec = GridLayout.spec(GridLayout.UNDEFINED, 1f);
        lp.setMargins(dp(6), dp(6), dp(6), dp(6));
        return lp;
    }

    private View optionTile(final Content.Item item) {
        LinearLayout tile = new LinearLayout(host);
        tile.setOrientation(LinearLayout.VERTICAL);
        tile.setGravity(Gravity.CENTER);
        tile.setPadding(dp(6), dp(8), dp(6), dp(8));
        int color = Util.funColor(pool.indexOf(item) * 3 + round);
        tile.setBackground(Util.rounded(color, dp(20)));
        tile.setElevation(dp(4));

        TextView icon = Util.text(host, "", 40, Color.WHITE);
        if (Content.isColorItem(item)) {
            int swatch = Content.colorOf(item);
            icon.setText("");
            LinearLayout.LayoutParams ilp = new LinearLayout.LayoutParams(dp(52), dp(52));
            ilp.gravity = Gravity.CENTER_HORIZONTAL;
            icon.setLayoutParams(ilp);
            icon.setBackground(Util.roundedStroke(swatch, dp(26), 0x66000000, dp(2)));
        } else if (Content.isScriptItem(item)) {
            icon.setText(item.symbol);
            icon.setTextSize(44);
        } else {
            // Letters carry their picture inside the symbol ("A|🍎"), so the
            // raw value would draw "A|🍎" instead of just "A".
            icon.setText(Content.glyphOf(item));
        }
        tile.addView(icon);

        TextView label = Util.text(host, item.name(lang), 15, Color.WHITE);
        label.setTypeface(null, Typeface.BOLD);
        tile.addView(label);

        tile.setOnClickListener(v -> choose(item, tile));
        return tile;
    }

    private void choose(Content.Item chosen, View tile) {
        if (finished) {
            return;
        }
        if (chosen == answer) {
            score += 10;
            host.progress().addPoints(10);
            host.progress().addStar();
            host.progress().addCorrect();
            host.progress().onCorrectAnswer();
            host.progress().earnedBadges();
            host.updateHeader();
            feedbackView.setText(t("Correct! \u2B50 +10", "Betul! \u2B50 +10"));
            host.celebrate(t("Yes! Well done!", "Ya! Bagus!"));
            tile.animate().scaleX(1.12f).scaleY(1.12f).setDuration(140)
                    .withEndAction(() -> tile.animate().scaleX(1f).scaleY(1f).setDuration(140).start())
                    .start();
            options.postDelayed(this::nextRound, 900);
        } else {
            host.progress().resetStreak();
            feedbackView.setText(t("Oops, try again! \uD83D\uDE4B", "Alamak, cuba lagi! \uD83D\uDE4B"));
            host.wrongFeedback(tile);
            host.mouthSay(t("Not that one, try again!", "Bukan itu, cuba lagi!"));
        }
    }

    private void finish() {
        finished = true;
        options.removeAllViews();
        if (level == null) {
            promptView.setText(t("You scored ", "Markah kamu ") + score + " \u2B50");
            feedbackView.setText(t("Great playing! \uD83C\uDF89", "Hebat! \uD83C\uDF89"));
            host.celebrate(t("Amazing!", "Hebat!"));
            options.addView(bigButton("\uD83D\uDD01", t("Play again", "Main lagi"),
                    0xFF7C4DFF, this::startGame), tileParams());
            return;
        }

        boolean passed = score >= level.passScore;
        host.progress().recordLevelScore(level.id, score);
        if (passed) {
            host.progress().unlockLevel(level.id + 1);
            host.progress().addStars(3);
            host.updateHeader();
            promptView.setText("\uD83C\uDF89 " + t("Level passed!", "Tahap lulus!"));
            feedbackView.setText(t("Score ", "Markah ") + score + " / " + level.passScore
                    + "\n" + t("The next level is now open!", "Tahap seterusnya telah dibuka!"));
            host.celebrate(t("Fantastic! Level up!", "Hebat! Naik tahap!"));
            if (level.id < Curriculum.total()) {
                final Curriculum.Level next = Curriculum.byId(level.id + 1);
                options.addView(bigButton("\u27A1\uFE0F", t("Next: ", "Seterusnya: ")
                        + next.title(lang), 0xFF66BB6A,
                        () -> host.open(new QuizGame(host, next))), tileParams());
            } else {
                options.addView(bigButton("\uD83C\uDFC6", t("All done!", "Semua siap!"),
                        0xFF7C4DFF, () -> host.open(new RewardsScreen(host))), tileParams());
            }
            options.addView(bigButton("\uD83D\uDD01", t("Try for a higher score",
                    "Cuba markah lebih tinggi"), 0xFFFF9100, this::startGame), tileParams());
        } else {
            promptView.setText("\uD83D\uDCAA " + t("Nearly there!", "Hampir berjaya!"));
            feedbackView.setText(t("You scored ", "Markah kamu ") + score + " / " + level.passScore
                    + "\n" + t("You need ", "Kamu perlu ") + level.passScore
                    + t(" to unlock the next level.", " untuk buka tahap seterusnya."));
            host.mouthSay(t("Let's practise and try again. You can do it!",
                    "Jom ulang belajar dan cuba lagi. Kamu boleh!"));
            options.addView(bigButton("\uD83D\uDCD6", t("Learn first", "Belajar dulu"),
                    0xFF29B6F6, () -> host.open(new LearnScreen(host, level.items,
                            LearnScreen.Mode.WORD))), tileParams());
            options.addView(bigButton("\uD83D\uDD01", t("Try again", "Cuba lagi"),
                    0xFFFF9100, this::startGame), tileParams());
        }
    }
}
