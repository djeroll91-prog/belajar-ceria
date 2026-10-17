package com.ceria.belajar;

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
 * Word building: the child hears a word, then taps its syllables (Malay) or its
 * letters (English) in the right order.
 *
 * Malay is built from syllables because that is how it is taught in tadika: a
 * child who knows "ba" and "ju" can read "baju" without knowing the alphabet
 * names. English is built from letters, since English syllables are irregular.
 */
public class SpellGame extends BaseScreen {

    private static final int ROUNDS = 6;

    private final Content.Unit unit;
    private final Random random = new Random();
    private final List<Content.Item> pool = new ArrayList<>();

    private int round;
    private int score;
    private String answer;
    private int filled;
    private LinearLayout slots;
    private GridLayout tiles;
    private TextView promptView;

    /** Word building over a unit's own words. */
    public SpellGame(MainActivity host, Content.Unit unit) {
        super(host);
        this.unit = unit;
        for (Content.Item item : unit.items) {
            if (wordOf(item) != null) {
                pool.add(item);
            }
        }
    }

    /** Word building across every unit that has real words, for the games hub. */
    public SpellGame(MainActivity host) {
        super(host);
        this.unit = null;
        for (Content.Unit u : Content.UNITS) {
            for (Content.Item item : u.items) {
                if (wordOf(item) != null) {
                    pool.add(item);
                }
            }
        }
    }

    /** Every word this game can ask for, for tests and for a sanity check. */
    public java.util.List<String> spellableWords() {
        java.util.List<String> out = new ArrayList<>();
        for (Content.Item item : pool) {
            String word = wordOf(item);
            if (word != null) {
                out.add(word);
            }
        }
        return out;
    }

    private static String wordOf(Content.Item item) {
        String word = item.word;
        if (word == null || !word.matches("[A-Za-z]{2,8}")) {
            return null;
        }
        return word.toLowerCase();
    }

    private Content.Unit unitOf(Content.Item item) {
        if (unit != null) {
            return unit;
        }
        for (Content.Unit u : Content.UNITS) {
            if (u.items.contains(item)) {
                return u;
            }
        }
        return null;
    }

    @Override
    protected String titleText() {
        return t("Build the Word \uD83E\uDDE9", "Bina Perkataan \uD83E\uDDE9");
    }

    @Override
    protected View buildBody() {
        LinearLayout column = vertical();
        column.setGravity(Gravity.CENTER_HORIZONTAL);

        promptView = Util.text(host, "", 22, theme.ink());
        promptView.setTypeface(null, android.graphics.Typeface.BOLD);
        promptView.setPadding(0, dp(8), 0, dp(10));
        column.addView(promptView, matchWrap());

        slots = new LinearLayout(host);
        slots.setOrientation(LinearLayout.HORIZONTAL);
        slots.setGravity(Gravity.CENTER);
        column.addView(slots, matchWrap());

        tiles = new GridLayout(host);
        tiles.setColumnCount(4);
        LinearLayout.LayoutParams gridLp = new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, 0, 1f);
        gridLp.setMargins(0, dp(14), 0, 0);
        column.addView(tiles, gridLp);

        column.addView(bottomRow(), matchWrap());

        if (pool.isEmpty()) {
            promptView.setText(t("Tiada perkataan", "No words here"));
            return scrollWrap(column);
        }
        startRound();
        return column;
    }

    private View bottomRow() {
        LinearLayout row = new LinearLayout(host);
        row.setOrientation(LinearLayout.HORIZONTAL);
        row.setGravity(Gravity.CENTER);

        TextView hear = Util.text(host, "\uD83D\uDD0A " + t("Dengar", "Listen"), 16, theme.onAccent());
        hear.setTypeface(null, android.graphics.Typeface.BOLD);
        hear.setGravity(Gravity.CENTER);
        hear.setBackground(Util.rounded(theme.accent(), dp(20)));
        hear.setElevation(dp(3));
        hear.setPadding(dp(18), dp(13), dp(18), dp(13));
        hear.setOnClickListener(v -> {
            host.sfx().tap();
            host.voice().speak(lang, answer);
        });

        TextView skip = Util.text(host, t("Langkau", "Skip"), 16, theme.ink());
        skip.setTypeface(null, android.graphics.Typeface.BOLD);
        skip.setGravity(Gravity.CENTER);
        skip.setBackground(Util.roundedStroke(theme.surface(), dp(20), theme.line(), dp(2)));
        skip.setPadding(dp(18), dp(13), dp(18), dp(13));
        skip.setOnClickListener(v -> {
            host.sfx().tap();
            nextRound();
        });

        LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT);
        lp.setMargins(dp(6), 0, dp(6), 0);
        row.addView(hear, lp);
        LinearLayout.LayoutParams lp2 = new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT);
        lp2.setMargins(dp(6), 0, dp(6), 0);
        row.addView(skip, lp2);
        return row;
    }

    /** Malay is chunked into syllables; English into single letters. */
    private List<String> chunk(String word) {
        List<String> out = new ArrayList<>();
        if (lang == Content.Lang.EN || word.length() <= 2) {
            for (char c : word.toCharArray()) {
                out.add(String.valueOf(c));
            }
            return out;
        }
        for (int i = 0; i < word.length(); i += 2) {
            out.add(word.substring(i, Math.min(word.length(), i + 2)));
        }
        return out;
    }

    private void startRound() {
        if (round >= ROUNDS || pool.isEmpty()) {
            showResult();
            return;
        }
        Content.Item item = pool.get(random.nextInt(pool.size()));
        answer = wordOf(item);
        filled = 0;

        List<String> parts = chunk(answer);
        promptView.setText(Content.displayOf(item, lang) + "  "
                + t("Bina: ", "Build: ") + answer);

        slots.removeAllViews();
        for (int i = 0; i < parts.size(); i++) {
            TextView slot = Util.text(host, "", 26, theme.ink());
            slot.setTypeface(null, android.graphics.Typeface.BOLD);
            slot.setGravity(Gravity.CENTER);
            slot.setBackground(Util.roundedStroke(theme.surface2(), dp(16), theme.line(), dp(3)));
            LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(dp(76), dp(64));
            lp.setMargins(dp(5), 0, dp(5), 0);
            slots.addView(slot, lp);
        }

        List<String> shuffled = new ArrayList<>(parts);
        Collections.shuffle(shuffled, random);
        tiles.removeAllViews();
        for (String part : shuffled) {
            tiles.addView(letterTile(part, parts), tileParams());
        }

        host.voice().speak(lang, answer);
    }

    private GridLayout.LayoutParams tileParams() {
        GridLayout.LayoutParams lp = new GridLayout.LayoutParams();
        lp.width = 0;
        lp.height = dp(68);
        lp.columnSpec = GridLayout.spec(GridLayout.UNDEFINED, 1f);
        lp.setMargins(dp(5), dp(5), dp(5), dp(5));
        return lp;
    }

    private View letterTile(String part, List<String> parts) {
        TextView tile = Util.text(host, part, 24, theme.ink());
        tile.setTypeface(null, android.graphics.Typeface.BOLD);
        tile.setGravity(Gravity.CENTER);
        tile.setBackground(Util.roundedStroke(theme.surface(), dp(18), theme.line(), dp(3)));
        tile.setElevation(dp(3));
        tile.setClickable(true);

        tile.setOnClickListener(v -> {
            if (tile.getAlpha() < 1f || filled >= parts.size()) {
                return;
            }
            String want = parts.get(filled);
            Content.Unit owner = unitOf(pool.get(0));
            if (part.equals(want)) {
                TextView slot = (TextView) slots.getChildAt(filled);
                slot.setText(part);
                slot.setBackground(Util.rounded(theme.accent(), dp(16)));
                slot.setTextColor(theme.onAccent());
                filled++;
                tile.setAlpha(0.35f);
                tile.setClickable(false);
                host.sfx().tap();
                if (filled == parts.size()) {
                    score++;
                    host.progress().recordAnswer(owner == null ? "spell" : owner.id, answer, true);
                    host.correctFeedback(slots);
                    host.mouthSay(t("Bagus sekali!", "Great job!"));
                    host.voice().speakThen(lang, t("Bagus sekali!", "Great job!"), 1f,
                            this::nextRound);
                }
            } else {
                host.progress().recordAnswer(owner == null ? "spell" : owner.id, answer, false);
                host.wrongFeedback(tile);
                host.shake(tile, 7);
            }
        });
        return tile;
    }

    private void nextRound() {
        round++;
        startRound();
    }

    private void showResult() {
        LinearLayout column = vertical();
        column.setGravity(Gravity.CENTER);
        column.addView(Util.text(host, "\uD83E\uDDF1", 60, theme.ink()));
        TextView result = Util.text(host, score + " / " + ROUNDS, 32, theme.ink());
        result.setTypeface(null, android.graphics.Typeface.BOLD);
        column.addView(result);

        TextView home = Util.text(host, t("Utama", "Home"), 17, theme.onAccent());
        home.setTypeface(null, android.graphics.Typeface.BOLD);
        home.setGravity(Gravity.CENTER);
        home.setBackground(Util.rounded(theme.accent(), dp(20)));
        home.setPadding(dp(24), dp(15), dp(24), dp(15));
        home.setOnClickListener(v -> host.showHome());
        LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT);
        lp.setMargins(0, dp(16), 0, 0);
        column.addView(home, lp);

        if (score >= ROUNDS - 1) {
            host.celebrate(host.praise());
        }
        body.removeAllViews();
        body.addView(column, new android.widget.FrameLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT));
    }
}
