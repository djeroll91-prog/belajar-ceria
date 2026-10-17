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
 * Word building game: the child sees a picture, hears the word, then taps the
 * letters in order to spell it.
 *
 * It only uses short single words from the learning content, and reads the word
 * out loud on every round, so a child who cannot read the clue yet can still
 * play by listening.
 */
public class SpellGame extends BaseScreen {

    private static final int TOTAL_ROUNDS = 8;
    private static final int MAX_LETTERS = 6;

    private final Random random = new Random();
    private final List<Content.Item> pool = new ArrayList<>();

    private int round;
    private int score;
    private String answer;
    private int filled;
    private LinearLayout slots;
    private GridLayout tiles;
    private TextView promptView;
    private TextView scoreView;

    public SpellGame(MainActivity host) {
        super(host);
    }

    @Override
    protected String titleText() {
        return t("Spell the Word \uD83D\uDD24", "Susun Huruf \uD83D\uDD24");
    }

    @Override
    protected View buildBody() {
        collectWords();

        LinearLayout column = vertical();
        column.setGravity(Gravity.CENTER_HORIZONTAL);

        scoreView = Util.text(host, "", 17, Util.INK);
        scoreView.setTypeface(null, Typeface.BOLD);
        column.addView(scoreView, matchWrap());

        promptView = Util.text(host, "", 60, Color.BLACK);
        promptView.setPadding(dp(10), dp(14), dp(10), dp(14));
        promptView.setBackground(Util.roundedStroke(Color.WHITE, dp(26), Util.MEADOW_DEEP, dp(4)));
        promptView.setElevation(dp(3));
        LinearLayout.LayoutParams promptLp = new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, 0, 1f);
        promptLp.setMargins(0, dp(10), 0, dp(10));
        column.addView(promptView, promptLp);
        promptView.setOnClickListener(v -> speakAnswer());

        slots = horizontal();
        column.addView(slots, matchWrap());

        LinearLayout.LayoutParams gap = matchWrap();
        gap.setMargins(0, dp(12), 0, dp(12));
        column.addView(new View(host), gap);

        tiles = new GridLayout(host);
        tiles.setColumnCount(6);
        column.addView(tiles, matchWrap());

        host.progress().registerGamePlayed();
        nextRound();
        return column;
    }

    /** Words this game can ask the child to spell, in the current language. */
    List<String> spellableWords() {
        collectWords();
        List<String> out = new ArrayList<>();
        for (Content.Item item : pool) {
            out.add(item.name(lang).toLowerCase(java.util.Locale.ROOT));
        }
        return out;
    }

    /** Short, spellable words from every content group the app already teaches. */
    private void collectWords() {
        if (!pool.isEmpty()) {
            return;
        }
        addFrom(Content.WORDS);
        addFrom(Content.FRUITS);
        addFrom(Content.ANIMALS);
        addFrom(Content.BODY);
        addFrom(Content.PLANTS);
    }

    private void addFrom(List<Content.Item> items) {
        for (Content.Item item : items) {
            String word = item.name(lang).trim();
            if (word.matches("[A-Za-z]{3," + MAX_LETTERS + "}")) {
                pool.add(item);
            }
        }
    }

    private void nextRound() {
        if (round >= TOTAL_ROUNDS) {
            finish();
            return;
        }
        if (pool.isEmpty()) {
            finish();
            return;
        }
        round++;
        filled = 0;
        Content.Item item = pool.get(random.nextInt(pool.size()));
        answer = item.name(lang).toLowerCase(java.util.Locale.ROOT);

        scoreView.setText(t("Round ", "Pusingan ") + round + " / " + TOTAL_ROUNDS
                + "   \u2B50 " + score);
        promptView.setText(item.symbol);
        speakAnswer();

        buildSlots();
        buildTiles();
    }

    private void buildSlots() {
        slots.removeAllViews();
        for (int i = 0; i < answer.length(); i++) {
            TextView slot = Util.text(host, "_", 30, Util.INK);
            slot.setTypeface(null, Typeface.BOLD);
            slot.setPadding(dp(2), dp(4), dp(2), dp(4));
            LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(0,
                    ViewGroup.LayoutParams.WRAP_CONTENT, 1f);
            lp.setMargins(dp(3), 0, dp(3), 0);
            slots.addView(slot, lp);
        }
    }

    private void buildTiles() {
        // A few extra letters would let a child guess. Keep only the real letters,
        // shuffled, so every tap either advances the word or is clearly wrong.
        List<Character> letters = new ArrayList<>();
        for (char c : answer.toCharArray()) {
            letters.add(c);
        }
        Collections.shuffle(letters);

        tiles.removeAllViews();
        for (char letter : letters) {
            tiles.addView(letterTile(letter), tileParams());
        }
    }

    private GridLayout.LayoutParams tileParams() {
        GridLayout.LayoutParams lp = new GridLayout.LayoutParams();
        lp.width = 0;
        lp.height = dp(60);
        lp.columnSpec = GridLayout.spec(GridLayout.UNDEFINED, 1f);
        lp.setMargins(dp(4), dp(4), dp(4), dp(4));
        return lp;
    }

    private View letterTile(final char letter) {
        final TextView tile = Util.text(host, String.valueOf(letter).toUpperCase(), 28, Color.WHITE);
        tile.setTypeface(null, Typeface.BOLD);
        tile.setBackground(Util.rounded(Util.storyColor(letter + round), dp(18)));
        tile.setElevation(dp(4));
        tile.setOnClickListener(v -> tap(letter, tile));
        return tile;
    }

    private void tap(char letter, View tile) {
        char expected = answer.charAt(filled);
        if (Character.toLowerCase(letter) == expected) {
            fillSlot(filled, expected);
            filled++;
            tile.setEnabled(false);
            tile.animate().alpha(0.25f).setDuration(150).start();
            if (filled == answer.length()) {
                winRound();
            }
            return;
        }

        host.progress().resetStreak();
        host.wrongFeedback(tile);
        host.mouthSay(t("Try the next letter!", "Cuba huruf seterusnya!"));
    }

    private void fillSlot(int index, char letter) {
        View child = slots.getChildAt(index);
        if (child instanceof TextView) {
            TextView slot = (TextView) child;
            slot.setText(String.valueOf(letter).toUpperCase());
            slot.setTextColor(Util.MEADOW_DEEP);
            slot.setScaleX(1.4f);
            slot.animate().scaleX(1f).setDuration(180).start();
        }
    }

    private void winRound() {
        score += 10;
        host.progress().addPoints(10);
        host.progress().addStar();
        host.progress().addCorrect();
        host.progress().onCorrectAnswer();
        host.progress().earnedBadges();
        host.updateHeader();
        scoreView.setText(t("Round ", "Pusingan ") + round + " / " + TOTAL_ROUNDS
                + "   \u2B50 " + score);
        promptView.animate().scaleX(1.15f).scaleY(1.15f).setDuration(150)
                .withEndAction(() -> promptView.animate().scaleX(1f).scaleY(1f)
                        .setDuration(150).start())
                .start();
        host.celebrate(t("You spelled it!", "Pandai mengeja!"));
        promptView.postDelayed(this::nextRound, 1100);
    }

    private void speakAnswer() {
        if (answer != null) {
            host.mouthSay(answer);
        }
    }

    private void finish() {
        scoreView.setText(t("You scored ", "Markah kamu ") + score + " \u2B50");
        promptView.setText("\uD83C\uDFC6");
        host.celebrate(t("Spelling star!", "Bintang mengeja!"));
        slots.removeAllViews();
        tiles.removeAllViews();
        tiles.setColumnCount(1);
        tiles.addView(bigButton("\uD83D\uDD01", t("Play again", "Main lagi"),
                0xFF8FBF7F, this::restart), new GridLayout.LayoutParams());
    }

    private void restart() {
        round = 0;
        score = 0;
        tiles.setColumnCount(6);
        nextRound();
    }
}
