package com.ceria.belajar;

import android.graphics.Color;
import android.graphics.Typeface;
import android.view.Gravity;
import android.view.View;
import android.view.ViewGroup;
import android.widget.GridLayout;
import android.widget.LinearLayout;
import android.widget.TextView;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Random;

/** Counting game: show N pictures and let the child pick the right number. */
public class CountGame extends BaseScreen {

    private static final String[] FRUITS = {"\uD83C\uDF4E", "\uD83C\uDF4A", "\uD83C\uDF53",
            "\uD83C\uDF4C", "\uD83C\uDF49", "\uD83C\uDF51", "\uD83C\uDF47", "\uD83C\uDF4B"};
    private static final String[] EN = {"one", "two", "three", "four", "five", "six", "seven",
            "eight", "nine", "ten"};
    private static final String[] MS = {"satu", "dua", "tiga", "empat", "lima", "enam", "tujuh",
            "lapan", "sembilan", "sepuluh"};

    private final Random random = new Random();
    private int round;
    private int score;
    private final int totalRounds = 8;
    private int answer;
    private TextView objectsView;
    private TextView progressView;
    private TextView feedbackView;
    private GridLayout choices;

    public CountGame(MainActivity host) {
        super(host);
    }

    @Override
    protected String titleText() {
        return t("Count the Objects \uD83D\uDD22", "Kira Objek \uD83D\uDD22");
    }

    @Override
    protected View buildBody() {
        LinearLayout column = vertical();
        column.setGravity(Gravity.CENTER_HORIZONTAL);

        progressView = Util.text(host, "", 17, 0xFF4A148C);
        column.addView(progressView, matchWrap());

        objectsView = Util.text(host, "", 40, Color.BLACK);
        objectsView.setPadding(dp(10), dp(20), dp(10), dp(20));
        objectsView.setBackground(Util.roundedStroke(Color.WHITE, dp(24), 0xFF66BB6A, dp(4)));
        LinearLayout.LayoutParams objLp = new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, 0, 1f);
        objLp.setMargins(0, dp(10), 0, dp(10));
        column.addView(objectsView, objLp);

        choices = new GridLayout(host);
        choices.setColumnCount(5);
        column.addView(choices, matchWrap());

        feedbackView = Util.text(host, t("How many do you see?", "Berapa banyak yang kamu nampak?"),
                18, 0xFFAD1457);
        feedbackView.setPadding(0, dp(14), 0, 0);
        column.addView(feedbackView, matchWrap());

        host.progress().registerGamePlayed();
        nextRound();
        return column;
    }

    private void nextRound() {
        if (round >= totalRounds) {
            finish();
            return;
        }
        round++;
        progressView.setText(t("Round ", "Pusingan ") + round + " / " + totalRounds
                + "   \u2B50 " + score);

        answer = 1 + random.nextInt(9);
        String fruit = FRUITS[random.nextInt(FRUITS.length)];
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < answer; i++) {
            sb.append(fruit).append(' ');
        }
        objectsView.setText(sb.toString().trim());
        feedbackView.setText(t("How many do you see?", "Berapa banyak yang kamu nampak?"));
        host.mouthSay(t("How many do you see?", "Berapa banyak yang kamu nampak?"));

        List<Integer> options = new ArrayList<>();
        options.add(answer);
        while (options.size() < 5) {
            int candidate = 1 + random.nextInt(10);
            if (!options.contains(candidate)) {
                options.add(candidate);
            }
        }
        Collections.shuffle(options);

        choices.removeAllViews();
        for (int n : options) {
            choices.addView(numberTile(n), tileParams());
        }
    }

    private GridLayout.LayoutParams tileParams() {
        GridLayout.LayoutParams lp = new GridLayout.LayoutParams();
        lp.width = 0;
        lp.height = dp(64);
        lp.columnSpec = GridLayout.spec(GridLayout.UNDEFINED, 1f);
        lp.setMargins(dp(5), dp(5), dp(5), dp(5));
        return lp;
    }

    private View numberTile(final int n) {
        TextView tile = Util.text(host, String.valueOf(n), 26, Color.WHITE);
        tile.setTypeface(null, Typeface.BOLD);
        tile.setBackground(Util.rounded(Util.funColor(n), dp(18)));
        tile.setElevation(dp(4));
        tile.setOnClickListener(v -> choose(n, tile));
        return tile;
    }

    private void choose(int n, View tile) {
        if (n == answer) {
            score += 10;
            host.progress().addPoints(10);
            host.progress().addStar();
            host.progress().addCorrect();
            host.progress().onCorrectAnswer();
            host.progress().earnedBadges();
            host.updateHeader();
            feedbackView.setText(t("Yes! ", "Ya! ") + answer + " - "
                    + (lang == Content.Lang.MS ? MS[answer - 1] : EN[answer - 1]) + " \u2B50");
            host.celebrate(t("Super counting!", "Pandai mengira!"));
            tile.animate().scaleX(1.15f).scaleY(1.15f).setDuration(140)
                    .withEndAction(() -> tile.animate().scaleX(1f).scaleY(1f).setDuration(140).start())
                    .start();
            choices.postDelayed(this::nextRound, 950);
        } else {
            host.progress().resetStreak();
            feedbackView.setText(t("Count again slowly! \uD83D\uDD0E", "Kira semula perlahan-lahan! \uD83D\uDD0E"));
            host.mouthSay(t("Let's count together!", "Jom kira bersama!"));
        }
    }

    private void finish() {
        progressView.setText(t("You scored ", "Markah kamu ") + score + " \u2B50");
        feedbackView.setText(t("Counting champion! \uD83C\uDFC6", "Juara mengira! \uD83C\uDFC6"));
        host.celebrate(t("Well done!", "Syabas!"));
        choices.removeAllViews();
        choices.addView(bigButton("\uD83D\uDD01", t("Play again", "Main lagi"),
                0xFF26A69A, this::restart), tileParams());
    }

    private void restart() {
        round = 0;
        score = 0;
        nextRound();
    }
}
