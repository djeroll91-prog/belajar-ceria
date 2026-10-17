package com.ceria.belajar;

import android.graphics.Color;
import android.graphics.Typeface;
import android.view.Gravity;
import android.view.View;
import android.view.ViewGroup;
import android.widget.LinearLayout;
import android.widget.TextView;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Random;

/**
 * Sorting game: two labelled baskets, and a row of pictures that have to be
 * dropped into the right one. Tap a picture, then tap the basket it belongs in.
 *
 * Grouping things that share a property is a core thinking skill, and it is the
 * same mental move as telling a letter from a number.
 */
public class SortGame extends BaseScreen {

    private static final int ROUNDS = 6;

    private final Random random = new Random();
    private int round;
    private int score;
    private int placed;
    private int totalInRound;
    private String leftKey;
    private String rightKey;
    private String leftLabel;
    private String rightLabel;
    private Content.Item held;
    private TextView heldView;
    private LinearLayout tray;
    private LinearLayout leftBasket;
    private LinearLayout rightBasket;
    private TextView scoreView;
    private TextView hintView;
    private boolean finished;

    public SortGame(MainActivity host) {
        super(host);
    }

    @Override
    protected String titleText() {
        return t("Sorting Baskets \uD83E\uDDFA", "Bakul Susun \uD83E\uDDFA");
    }

    @Override
    protected View buildBody() {
        LinearLayout column = vertical();
        column.setGravity(Gravity.CENTER_HORIZONTAL);

        scoreView = Util.text(host, "", 17, 0xFF4A148C);
        column.addView(scoreView, matchWrap());

        hintView = Util.text(host, "", 16, 0xFF00695C);
        hintView.setPadding(0, dp(4), 0, dp(8));
        column.addView(hintView, matchWrap());

        tray = new LinearLayout(host);
        tray.setOrientation(LinearLayout.HORIZONTAL);
        tray.setGravity(Gravity.CENTER);
        tray.setPadding(dp(6), dp(10), dp(6), dp(10));
        tray.setBackground(Util.roundedStroke(0xFFFFF8E1, dp(20), 0x66FFB300, dp(3)));
        column.addView(tray, matchWrap());

        column.addView(spacer(14));

        LinearLayout baskets = new LinearLayout(host);
        baskets.setOrientation(LinearLayout.HORIZONTAL);
        baskets.setGravity(Gravity.CENTER);
        leftBasket = basket(0xFF8FBF7F);
        rightBasket = basket(0xFF7FB4D9);
        LinearLayout.LayoutParams blp = new LinearLayout.LayoutParams(0,
                ViewGroup.LayoutParams.WRAP_CONTENT, 1f);
        blp.setMargins(dp(6), 0, dp(6), 0);
        baskets.addView(leftBasket, blp);
        baskets.addView(rightBasket, blp);
        column.addView(baskets, matchWrap());

        startGame();
        return column;
    }

    private LinearLayout basket(int color) {
        LinearLayout b = new LinearLayout(host);
        b.setOrientation(LinearLayout.VERTICAL);
        b.setGravity(Gravity.CENTER);
        b.setPadding(dp(8), dp(16), dp(8), dp(16));
        b.setMinimumHeight(dp(150));
        b.setBackground(Util.roundedStroke(color, dp(22), 0xCCFFFFFF, dp(3)));
        b.setElevation(dp(5));
        b.setClickable(true);
        return b;
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
        placed = 0;
        held = null;
        heldView = null;
        scoreView.setText(t("Round ", "Pusingan ") + round + " / " + ROUNDS
                + "   \u2B50 " + score);

        // Two groups that a young child can tell apart at a glance.
        boolean animalsAndPlants = random.nextBoolean();
        List<Content.Item> leftItems;
        List<Content.Item> rightItems;
        if (animalsAndPlants) {
            leftKey = "animal";
            rightKey = "plant";
            leftLabel = t("Animals", "Haiwan");
            rightLabel = t("Plants", "Tumbuhan");
            leftItems = new ArrayList<>(Content.ANIMALS);
            rightItems = new ArrayList<>(Content.PLANTS);
        } else {
            leftKey = "animal";
            rightKey = "fruit";
            leftLabel = t("Animals", "Haiwan");
            rightLabel = t("Fruits", "Buah");
            leftItems = new ArrayList<>(Content.ANIMALS);
            rightItems = new ArrayList<>(Content.FRUITS);
        }
        Collections.shuffle(leftItems, random);
        Collections.shuffle(rightItems, random);

        int each = 3;
        List<Content.Item> items = new ArrayList<>();
        items.addAll(leftItems.subList(0, each));
        items.addAll(rightItems.subList(0, each));
        Collections.shuffle(items, random);
        totalInRound = items.size();

        fillBasket(leftBasket, "\uD83E\uDDFA " + leftLabel);
        fillBasket(rightBasket, "\uD83E\uDDFA " + rightLabel);
        leftBasket.setOnClickListener(v -> drop("left", v));
        rightBasket.setOnClickListener(v -> drop("right", v));

        tray.removeAllViews();
        for (Content.Item item : items) {
            TextView tile = Util.text(host, item.symbol, 30, Color.BLACK);
            tile.setBackground(Util.roundedStroke(0xFFFFFFFF, dp(16), 0xFFB39DDB, dp(2)));
            tile.setClickable(true);
            LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(dp(58), dp(58));
            lp.setMargins(dp(4), 0, dp(4), 0);
            tile.setLayoutParams(lp);
            tile.setOnClickListener(v -> hold(item, tile));
            tray.addView(tile);
        }
        hintView.setText(t("Tap a picture, then tap its basket",
                "Tekan gambar, kemudian tekan bakulnya"));
    }

    private void fillBasket(LinearLayout basket, String title) {
        basket.removeAllViews();
        TextView label = Util.text(host, title, 15, Color.WHITE);
        label.setTypeface(null, Typeface.BOLD);
        basket.addView(label);
    }

    private void hold(Content.Item item, TextView tile) {
        if (finished || tile.getParent() != tray) {
            return;
        }
        held = item;
        heldView = tile;
        host.sfx().tap();
        host.pop(tile);
        hintView.setText(item.symbol + " " + t("goes where?", "pergi mana?"));
    }

    private void drop(String side, View basket) {
        if (finished || held == null) {
            return;
        }
        boolean correctSide = side.equals(belongs(held));
        if (!correctSide) {
            host.progress().resetStreak();
            hintView.setText(held.symbol + t(" is not a ", " bukan ")
                    + (side.equals("left") ? leftLabel.toLowerCase() : rightLabel.toLowerCase())
                    + t(". Try the other basket!", ". Cuba bakul lagi satu!"));
            host.wrongFeedback(basket);
            return;
        }

        tray.removeView(heldView);
        TextView placedTile = Util.text(host, held.symbol, 22, Color.BLACK);
        LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(dp(38), dp(38));
        lp.setMargins(dp(2), dp(6), dp(2), 0);
        placedTile.setLayoutParams(lp);
        ((LinearLayout) basket).addView(placedTile);
        host.pop(placedTile);

        held = null;
        heldView = null;
        placed++;
        score += 10;
        host.progress().addPoints(10);
        host.progress().addCorrect();
        host.progress().onCorrectAnswer();
        host.progress().earnedBadges();
        host.updateHeader();
        scoreView.setText(t("Round ", "Pusingan ") + round + " / " + ROUNDS
                + "   \u2B50 " + score);
        hintView.setText(t("Correct! ", "Betul! ") + placed + " / " + totalInRound);

        if (placed >= totalInRound) {
            host.progress().addStar();
            host.progress().earnedBadges();
            host.updateHeader();
            host.celebrate(t("Sorted it all!", "Semua tersusun!"));
            tray.postDelayed(this::nextRound, 950);
        }
    }

    /** Which basket an item belongs in, for the current grouping. */
    private String belongs(Content.Item item) {
        // The left basket is always the animal basket in both groupings.
        return Content.ANIMALS.contains(item) ? "left" : "right";
    }

    private void finish() {
        finished = true;
        tray.removeAllViews();
        scoreView.setText(t("You scored ", "Markah kamu ") + score + " \u2B50");
        hintView.setText(t("Sorting star! \uD83C\uDFC6", "Bintang susun! \uD83C\uDFC6"));
        host.celebrate(t("Well organised!", "Tersusun rapi!"));
        tray.addView(bigButton("\uD83D\uDD01", t("Play again", "Main lagi"),
                0xFF7FB4D9, this::startGame), wrap());
    }
}
