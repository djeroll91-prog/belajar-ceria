package com.ceria.belajar;

import android.graphics.Color;
import android.graphics.Typeface;
import android.view.Gravity;
import android.view.View;
import android.widget.GridLayout;
import android.widget.LinearLayout;
import android.widget.TextView;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Random;

/**
 * First sums, always shown with pictures. "2 apples and 1 apple" is drawn as
 * fruit the child can count, so the number sentence means something before it is
 * ever memorised.
 *
 * Answers are picked from tiles rather than typed, which keeps the game playable
 * for a child who cannot write numbers yet.
 */
public class MathGame extends BaseScreen {

    private static final int ROUNDS = 8;

    private final Random random = new Random();
    private int round;
    private int score;
    private int answer;
    private Content.Item object;
    private GridLayout options;
    private TextView sumView;
    private TextView objectsView;
    private TextView scoreView;
    private TextView hintView;
    private boolean finished;

    public MathGame(MainActivity host) {
        super(host);
    }

    @Override
    protected String titleText() {
        return t("Add and Take Away \u2795", "Tambah dan Tolak \u2795");
    }

    @Override
    protected View buildBody() {
        LinearLayout column = vertical();
        column.setGravity(Gravity.CENTER_HORIZONTAL);

        scoreView = Util.text(host, "", 17, 0xFF4A148C);
        column.addView(scoreView, matchWrap());

        sumView = Util.text(host, "", 34, 0xFF00695C);
        sumView.setTypeface(null, Typeface.BOLD);
        sumView.setPadding(0, dp(10), 0, dp(6));
        column.addView(sumView, matchWrap());

        objectsView = Util.text(host, "", 22, 0xFF4A3F5C);
        objectsView.setPadding(0, dp(2), 0, dp(10));
        column.addView(objectsView, matchWrap());

        hintView = Util.text(host, "", 16, 0xFFAD1457);
        hintView.setPadding(0, 0, 0, dp(10));
        column.addView(hintView, matchWrap());

        options = new GridLayout(host);
        options.setColumnCount(3);
        column.addView(options, matchWrap());

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
        if (round >= ROUNDS) {
            finish();
            return;
        }
        round++;
        scoreView.setText(t("Round ", "Pusingan ") + round + " / " + ROUNDS
                + "   \u2B50 " + score);
        hintView.setText("");

        List<Content.Item> pool = new ArrayList<>();
        pool.addAll(Content.FRUITS);
        pool.addAll(Content.ANIMALS);
        object = pool.get(random.nextInt(pool.size()));

        boolean subtract = round > 3 && random.nextBoolean();
        int a;
        int b;
        if (subtract) {
            a = 2 + random.nextInt(7);
            b = 1 + random.nextInt(Math.max(1, a - 1));
            answer = a - b;
        } else {
            a = 1 + random.nextInt(6);
            b = 1 + random.nextInt(6);
            answer = a + b;
        }

        sumView.setText(a + (subtract ? " \u2212 " : " + ") + b + " = ?");
        objectsView.setText(pictures(a, b, subtract));
        host.voice().speak(lang, (subtract
                ? t("Take away. ", "Tolak. ")
                : t("Add. ", "Tambah. "))
                + a + (subtract ? t(" take away ", " tolak ") : t(" and ", " tambah ")) + b);

        renderOptions();
    }

    /**
     * Draws the sum as countable pictures. For a subtraction the taken-away ones
     * are shown faded and struck through, so the child sees what is left.
     */
    private String pictures(int a, int b, boolean subtract) {
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < a; i++) {
            sb.append(object.symbol).append(' ');
        }
        if (subtract) {
            sb.append("  \u2796  ");
            for (int i = 0; i < b; i++) {
                sb.append(object.symbol).append(' ');
            }
        } else {
            sb.append("  \u2795  ");
            for (int i = 0; i < b; i++) {
                sb.append(object.symbol).append(' ');
            }
        }
        return sb.toString().trim();
    }

    private void renderOptions() {
        options.removeAllViews();
        List<Integer> choices = new ArrayList<>();
        choices.add(answer);
        int guard = 0;
        while (choices.size() < 3 && guard++ < 60) {
            int candidate = Math.max(0, answer + random.nextInt(7) - 3);
            if (!choices.contains(candidate)) {
                choices.add(candidate);
            }
        }
        Collections.shuffle(choices, random);

        for (final int value : choices) {
            TextView tile = Util.text(host, String.valueOf(value), 30, Color.WHITE);
            tile.setBackground(Util.roundedStroke(Util.randomFunColor(), dp(18),
                    0xCCFFFFFF, dp(2)));
            tile.setClickable(true);
            tile.setElevation(dp(4));
            GridLayout.LayoutParams lp = new GridLayout.LayoutParams();
            lp.width = dp(78);
            lp.height = dp(78);
            lp.setMargins(dp(6), dp(6), dp(6), dp(6));
            tile.setLayoutParams(lp);
            tile.setOnClickListener(v -> choose(value, tile));
            options.addView(tile);
        }
    }

    private void choose(int value, View tile) {
        if (finished) {
            return;
        }
        if (value != answer) {
            host.progress().resetStreak();
            hintView.setText(t("Count the pictures again!", "Kira gambar sekali lagi!"));
            host.wrongFeedback(tile);
            return;
        }

        score += 10;
        host.progress().addPoints(10);
        host.progress().addStar();
        host.progress().addCorrect();
        host.progress().onCorrectAnswer();
        host.progress().earnedBadges();
        host.updateHeader();
        scoreView.setText(t("Round ", "Pusingan ") + round + " / " + ROUNDS
                + "   \u2B50 " + score);
        hintView.setText(t("Correct! ", "Betul! ") + answer + " \u2B50");
        host.pop(tile);
        host.celebrate(t("Pandai kira!", "Pandai kira!"));
        options.postDelayed(this::nextRound, 950);
    }

    private void finish() {
        finished = true;
        options.removeAllViews();
        sumView.setText(t("You scored ", "Markah kamu ") + score + " \u2B50");
        objectsView.setText("");
        hintView.setText(t("Maths champion! \uD83C\uDFC6", "Juara matematik! \uD83C\uDFC6"));
        host.celebrate(t("Superb counting!", "Hebat mengira!"));
        options.addView(bigButton("\uD83D\uDD01", t("Play again", "Main lagi"),
                0xFF26C6DA, this::startGame), wrap());
    }
}
