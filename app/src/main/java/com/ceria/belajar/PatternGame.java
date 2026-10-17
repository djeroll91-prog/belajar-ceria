package com.ceria.belajar;

import android.graphics.Color;
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
 * "What comes next?" A repeating pattern of pictures is shown with one gap, and
 * the child picks the picture that finishes it.
 *
 * Patterns are the earliest form of algebra a child meets, and they need no
 * reading at all, so this works for the youngest children who are still learning
 * their letters.
 */
public class PatternGame extends BaseScreen {

    private static final int TOTAL_ROUNDS = 8;
    private static final int GAP = -1;

    private final Random random = new Random();
    private int round;
    private int score;
    private int answerIndex;
    /** The picture that belongs in the gap, so the answer can be checked. */
    private Content.Item answerItem;
    private int[] pattern;
    private GridLayout options;
    private LinearLayout patternRow;
    private TextView scoreView;
    private TextView hintView;
    private boolean finished;

    public PatternGame(MainActivity host) {
        super(host);
    }

    @Override
    protected String titleText() {
        return t("What Comes Next? \uD83E\uDDE9", "Apa Seterusnya? \uD83E\uDDE9");
    }

    @Override
    protected View buildBody() {
        LinearLayout column = vertical();
        column.setGravity(Gravity.CENTER_HORIZONTAL);

        scoreView = Util.text(host, "", 17, 0xFF4A148C);
        scoreView.setPadding(0, dp(4), 0, dp(8));
        column.addView(scoreView);

        hintView = Util.text(host, t("Look at the pattern!", "Perhatikan corak ini!"),
                17, 0xFF00695C);
        hintView.setPadding(0, dp(4), 0, dp(10));
        column.addView(hintView);

        patternRow = new LinearLayout(host);
        patternRow.setOrientation(LinearLayout.HORIZONTAL);
        patternRow.setGravity(Gravity.CENTER);
        patternRow.setPadding(dp(6), dp(12), dp(6), dp(12));
        patternRow.setBackground(Util.roundedStroke(0xFFFFF8E1, dp(22), 0x66FFB300, dp(3)));
        column.addView(patternRow, matchWrap());

        column.addView(spacer(16));

        options = new GridLayout(host);
        options.setColumnCount(3);
        options.setUseDefaultMargins(true);
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
        if (round >= TOTAL_ROUNDS) {
            finish();
            return;
        }
        round++;
        scoreView.setText(t("Round ", "Pusingan ") + round + " / " + TOTAL_ROUNDS
                + "   \u2B50 " + score);

        List<Content.Item> pool = pool();
        Collections.shuffle(pool, random);
        // Two pictures make a simple A-B pattern, three make A-B-C. Older rounds
        // get the harder one.
        int distinct = round <= 3 ? 2 : 3;
        distinct = Math.min(distinct, pool.size());
        List<Content.Item> shapes = new ArrayList<>(pool.subList(0, distinct));

        int length = 5;
        pattern = new int[length];
        for (int i = 0; i < length; i++) {
            pattern[i] = i % distinct;
        }
        answerIndex = pattern[length - 1];
        pattern[length - 1] = GAP;

        renderPattern(shapes);
        renderOptions(shapes);
        hintView.setText(t("Which one finishes the pattern?",
                "Yang mana menyambung corak ini?"));
    }

    /** Pictures a pattern can be built from: things a child recognises by sight. */
    private List<Content.Item> pool() {
        List<Content.Item> pool = new ArrayList<>();
        pool.addAll(Content.ANIMALS);
        pool.addAll(Content.FRUITS);
        pool.addAll(Content.PLANTS);
        return pool;
    }

    private void renderPattern(List<Content.Item> shapes) {
        patternRow.removeAllViews();
        for (int value : pattern) {
            LinearLayout cell = new LinearLayout(host);
            cell.setOrientation(LinearLayout.VERTICAL);
            cell.setGravity(Gravity.CENTER);
            LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(dp(56), dp(56));
            lp.setMargins(dp(3), 0, dp(3), 0);
            cell.setLayoutParams(lp);

            if (value == GAP) {
                cell.setBackground(Util.roundedStroke(0xFFFFFFFF, dp(16), 0xFF7C4DFF, dp(3)));
                cell.addView(Util.text(host, "?", 30, 0xFF7C4DFF));
                cell.setTag("gap");
            } else {
                cell.addView(Util.text(host, shapes.get(value).symbol, 30, Color.BLACK));
            }
            patternRow.addView(cell);
        }
    }

    private void renderOptions(List<Content.Item> shapes) {
        options.removeAllViews();
        answerItem = shapes.get(answerIndex);
        // Distractors come from the pattern's own pictures, so the choice is
        // about the order and not about spotting a picture that never appeared.
        List<Content.Item> choices = new ArrayList<>();
        choices.add(answerItem);
        List<Content.Item> others = new ArrayList<>(shapes);
        Collections.shuffle(others, random);
        for (Content.Item candidate : others) {
            if (choices.size() >= Math.min(3, shapes.size())) {
                break;
            }
            if (!candidate.symbol.equals(answerItem.symbol)) {
                choices.add(candidate);
            }
        }
        Collections.shuffle(choices, random);

        for (Content.Item choice : choices) {
            final Content.Item item = choice;
            TextView tile = Util.text(host, item.symbol, 40, Color.BLACK);
            tile.setBackground(Util.roundedStroke(0xFFFFFFFF, dp(20), 0xFFB39DDB, dp(3)));
            tile.setClickable(true);
            tile.setElevation(dp(4));
            GridLayout.LayoutParams lp = new GridLayout.LayoutParams();
            lp.width = dp(88);
            lp.height = dp(88);
            lp.setMargins(dp(6), dp(6), dp(6), dp(6));
            tile.setLayoutParams(lp);
            tile.setOnClickListener(v -> choose(item, tile));
            options.addView(tile);
        }
    }

    private void choose(Content.Item picked, View tile) {
        if (finished) {
            return;
        }
        if (answerItem == null || !picked.symbol.equals(answerItem.symbol)) {
            host.progress().resetStreak();
            hintView.setText(t("Look again closely! \uD83D\uDC40", "Perhatikan lagi! \uD83D\uDC40"));
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
        scoreView.setText(t("Round ", "Pusingan ") + round + " / " + TOTAL_ROUNDS
                + "   \u2B50 " + score);
        hintView.setText(t("Yes! That is right \u2B50", "Ya! Betul \u2B50"));
        host.pop(tile);
        fillGap();
        host.celebrate(t("Pattern master!", "Pandai corak!"));
        options.postDelayed(this::nextRound, 1000);
    }

    /** Reveals the answer in the gap so the child sees the finished pattern. */
    private void fillGap() {
        View gap = patternRow.findViewWithTag("gap");
        if (gap instanceof LinearLayout && answerItem != null) {
            LinearLayout cell = (LinearLayout) gap;
            cell.removeAllViews();
            cell.setTag(null);
            cell.setBackground(Util.rounded(0xFFE8F5E9, dp(16)));
            cell.addView(Util.text(host, answerItem.symbol, 30, Color.BLACK));
            host.hop(cell);
        }
    }

    private void finish() {
        finished = true;
        options.removeAllViews();
        scoreView.setText(t("You scored ", "Markah kamu ") + score + " \u2B50");
        hintView.setText(t("Pattern champion! \uD83C\uDFC6", "Juara corak! \uD83C\uDFC6"));
        host.celebrate(t("Brilliant thinking!", "Hebat berfikir!"));
        options.addView(bigButton("\uD83D\uDD01", t("Play again", "Main lagi"),
                0xFF7C4DFF, this::startGame), wrap());
    }
}
