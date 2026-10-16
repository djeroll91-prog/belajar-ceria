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

/**
 * "Which one is different?" game.
 *
 * Three pictures belong to one group and one belongs to another. Spotting the
 * odd one out is an early reasoning skill, and because the pictures are already
 * taught elsewhere in the app it doubles as vocabulary revision.
 */
public class OddOneOutGame extends BaseScreen {

    private static final int TOTAL_ROUNDS = 8;

    private final Random random = new Random();
    private final List<List<Content.Item>> groups = new ArrayList<>();

    private int round;
    private int score;
    private TextView scoreView;
    private TextView hintView;
    private GridLayout board;

    public OddOneOutGame(MainActivity host) {
        super(host);
    }

    @Override
    protected String titleText() {
        return t("Odd One Out \uD83D\uDD0D", "Cari Yang Berbeza \uD83D\uDD0D");
    }

    @Override
    protected View buildBody() {
        collectGroups();

        LinearLayout column = vertical();
        column.setGravity(Gravity.CENTER_HORIZONTAL);

        scoreView = Util.text(host, "", 17, Util.INK);
        scoreView.setTypeface(null, Typeface.BOLD);
        column.addView(scoreView, matchWrap());

        hintView = Util.text(host, t("Which one is different?",
                "Yang mana berbeza?"), 18, Util.INK_SOFT);
        hintView.setPadding(0, dp(8), 0, dp(14));
        column.addView(hintView, matchWrap());

        board = new GridLayout(host);
        board.setColumnCount(2);
        LinearLayout.LayoutParams boardLp = new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, 0, 1f);
        column.addView(board, boardLp);

        host.progress().registerGamePlayed();
        nextRound();
        return column;
    }

    /** How many distinct content groups this game can draw an odd one out from. */
    int groupCount() {
        collectGroups();
        return groups.size();
    }

    private void collectGroups() {
        if (!groups.isEmpty()) {
            return;
        }
        // Only groups with enough members to fill three slots.
        addGroup(Content.ANIMALS);
        addGroup(Content.FRUITS);
        addGroup(Content.BODY);
        addGroup(Content.PLANTS);
        addGroup(Content.WORDS);
    }

    private void addGroup(List<Content.Item> items) {
        List<Content.Item> usable = new ArrayList<>();
        for (Content.Item item : items) {
            if (item.symbol != null && !Content.isScriptItem(item)
                    && !Content.isColorItem(item)) {
                usable.add(item);
            }
        }
        if (usable.size() >= 3) {
            groups.add(usable);
        }
    }

    private void nextRound() {
        if (round >= TOTAL_ROUNDS || groups.size() < 2) {
            finish();
            return;
        }
        round++;
        scoreView.setText(t("Round ", "Pusingan ") + round + " / " + TOTAL_ROUNDS
                + "   \u2B50 " + score);

        List<List<Content.Item>> shuffled = new ArrayList<>(groups);
        Collections.shuffle(shuffled);
        List<Content.Item> majority = shuffled.get(0);
        List<Content.Item> minority = shuffled.get(1);

        List<Content.Item> picked = new ArrayList<>();
        List<Content.Item> pool = new ArrayList<>(majority);
        Collections.shuffle(pool);
        picked.add(pool.get(0));
        picked.add(pool.get(1));
        picked.add(pool.get(2));

        Content.Item oddItem = minority.get(random.nextInt(minority.size()));
        picked.add(oddItem);
        Collections.shuffle(picked);

        board.removeAllViews();
        for (Content.Item item : picked) {
            board.addView(card(item, item == oddItem), cardParams());
        }

        host.mouthSay(t("Which one is different?", "Yang mana berbeza?"));
    }

    private GridLayout.LayoutParams cardParams() {
        GridLayout.LayoutParams lp = new GridLayout.LayoutParams();
        lp.width = 0;
        lp.height = 0;
        lp.rowSpec = GridLayout.spec(GridLayout.UNDEFINED, 1f);
        lp.columnSpec = GridLayout.spec(GridLayout.UNDEFINED, 1f);
        lp.setMargins(dp(6), dp(6), dp(6), dp(6));
        return lp;
    }

    private View card(final Content.Item item, final boolean isOdd) {
        LinearLayout card = new LinearLayout(host);
        card.setOrientation(LinearLayout.VERTICAL);
        card.setGravity(Gravity.CENTER);
        card.setPadding(dp(8), dp(10), dp(8), dp(10));
        card.setBackground(Util.roundedStroke(Color.WHITE, dp(22), Util.MEADOW, dp(4)));
        card.setElevation(dp(5));

        TextView glyph = Util.text(host, item.symbol, 48, Color.BLACK);
        card.addView(glyph);
        TextView name = Util.text(host, item.name(lang), 14, Util.INK_SOFT);
        card.addView(name);

        card.setOnClickListener(v -> choose(item, name.getText().toString(), isOdd, card, glyph));
        return card;
    }

    private void choose(Content.Item item, String spoken, boolean isOdd,
                        View card, TextView glyph) {
        if (!isOdd) {
            host.progress().resetStreak();
            hintView.setText(t("Look again closely! \uD83D\uDC40", "Perhatikan lagi! \uD83D\uDC40"));
            card.animate().translationX(dp(8)).setDuration(60)
                    .withEndAction(() -> card.animate().translationX(0).setDuration(60).start())
                    .start();
            return;
        }

        score += 10;
        host.progress().addPoints(10);
        host.progress().addStar();
        host.progress().addCorrect();
        host.progress().onCorrectAnswer();
        host.progress().earnedBadges();
        host.updateHeader();
        scoreView.setText(t("Round ", "Pusingan ") + round + " / " + TOTAL_ROUNDS
                + "   \u2B50 " + score);
        hintView.setText(t("Spot on! That is ", "Betul! Ini ") + spoken + " \u2B50");
        host.celebrate(t("Sharp eyes!", "Mata yang tajam!"));
        glyph.animate().scaleX(1.3f).scaleY(1.3f).setDuration(160)
                .withEndAction(() -> glyph.animate().scaleX(1f).scaleY(1f)
                        .setDuration(160).start())
                .start();
        board.postDelayed(this::nextRound, 1000);
    }

    private void finish() {
        scoreView.setText(t("You scored ", "Markah kamu ") + score + " \u2B50");
        hintView.setText(t("Detective of the day! \uD83C\uDFC6", "Detektif hari ini! \uD83C\uDFC6"));
        host.celebrate(t("Brilliant!", "Hebat!"));
        board.removeAllViews();
        board.setColumnCount(1);
        board.addView(bigButton("\uD83D\uDD01", t("Play again", "Main lagi"),
                0xFF9A8FCF, this::restart), new GridLayout.LayoutParams());
    }

    private void restart() {
        round = 0;
        score = 0;
        board.setColumnCount(2);
        nextRound();
    }
}
