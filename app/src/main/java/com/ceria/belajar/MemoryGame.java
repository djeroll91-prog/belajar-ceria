package com.ceria.belajar;

import android.os.Handler;
import android.os.Looper;
import android.view.Gravity;
import android.view.View;
import android.view.ViewGroup;
import android.widget.GridLayout;
import android.widget.LinearLayout;
import android.widget.TextView;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * Matching pairs: turn over two cards and find the picture that goes with the
 * picture. The gentlest memory workout, and the one a four-year-old can win.
 */
public class MemoryGame extends BaseScreen {

    private static final int PAIRS = 6;

    private final Content.Unit unit;
    private final Handler handler = new Handler(Looper.getMainLooper());
    private final List<Content.Item> deck = new ArrayList<>();
    private final List<View> cards = new ArrayList<>();
    private final List<Integer> flipped = new ArrayList<>();
    private final List<Integer> matched = new ArrayList<>();
    private int moves;
    private TextView statusView;
    private GridLayout grid;

    /** Pairs drawn from one unit's own items. */
    public MemoryGame(MainActivity host, Content.Unit unit) {
        super(host);
        this.unit = unit;
        List<Content.Item> pool = new ArrayList<>(unit.items);
        Collections.shuffle(pool);
        for (int i = 0; i < Math.min(PAIRS, pool.size()); i++) {
            deck.add(pool.get(i));
            deck.add(pool.get(i));
        }
        Collections.shuffle(deck);
    }

    /** Pairs drawn from across the whole app, for the games hub. */
    public MemoryGame(MainActivity host) {
        super(host);
        this.unit = null;
        List<Content.Item> pool = new ArrayList<>();
        for (Content.Unit u : Content.UNITS) {
            if ("emoji".equals(u.kind) || "color".equals(u.kind)) {
                pool.addAll(u.items);
            }
        }
        Collections.shuffle(pool);
        for (int i = 0; i < Math.min(PAIRS, pool.size()); i++) {
            deck.add(pool.get(i));
            deck.add(pool.get(i));
        }
        Collections.shuffle(deck);
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
        return t("Matching Pairs \uD83D\uDD0D", "Cari Pasangan \uD83D\uDD0D");
    }

    @Override
    protected View buildBody() {
        LinearLayout column = vertical();
        column.setGravity(Gravity.CENTER_HORIZONTAL);

        statusView = Util.text(host, "", 17, theme.ink());
        statusView.setTypeface(null, android.graphics.Typeface.BOLD);
        statusView.setPadding(0, 0, 0, dp(10));
        column.addView(statusView, matchWrap());

        grid = new GridLayout(host);
        grid.setColumnCount(4);
        column.addView(grid, new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, 0, 1f));

        for (int i = 0; i < deck.size(); i++) {
            cards.add(card(i));
        }
        for (View card : cards) {
            grid.addView(card, cardParams());
        }
        updateStatus();
        return column;
    }

    private GridLayout.LayoutParams cardParams() {
        GridLayout.LayoutParams lp = new GridLayout.LayoutParams();
        lp.width = 0;
        lp.height = 0;
        lp.rowSpec = GridLayout.spec(GridLayout.UNDEFINED, 1f);
        lp.columnSpec = GridLayout.spec(GridLayout.UNDEFINED, 1f);
        lp.setMargins(dp(5), dp(5), dp(5), dp(5));
        return lp;
    }

    private View card(final int index) {
        TextView card = Util.text(host, "?", 34, theme.inkSoft());
        card.setTypeface(null, android.graphics.Typeface.BOLD);
        card.setGravity(Gravity.CENTER);
        card.setBackground(Util.roundedStroke(theme.surface(), dp(20), theme.line(), dp(3)));
        card.setElevation(dp(3));
        card.setClickable(true);
        card.setOnClickListener(v -> flip(index, card));
        return card;
    }

    private void flip(int index, TextView card) {
        if (flipped.contains(index) || matched.contains(index) || flipped.size() >= 2) {
            return;
        }
        Content.Item item = deck.get(index);
        Content.Unit owner = unitOf(item);
        card.setText(Content.displayOf(item, lang));
        card.setTextSize(34);
        card.setTextColor(theme.ink());
        flipped.add(index);
        host.sfx().tap();
        if (owner != null) {
            host.voice().speak(lang, Content.spokenOf(item, owner.kind, lang));
        }

        if (flipped.size() == 2) {
            moves++;
            int a = flipped.get(0);
            int b = flipped.get(1);
            boolean same = deck.get(a) == deck.get(b);
            Content.Item first = deck.get(a);
            Content.Unit firstUnit = unitOf(first);
            if (firstUnit != null) {
                host.progress().recordAnswer(firstUnit.id, Progress.keyOf(first), same);
            }
            if (same) {
                matched.add(a);
                matched.add(b);
                flipped.clear();
                host.correctFeedback(cards.get(a));
                host.correctFeedback(cards.get(b));
                cards.get(a).setBackground(Util.rounded(theme.wash(theme.goodFor(), 0.5f),
                        dp(20)));
                cards.get(b).setBackground(Util.rounded(theme.wash(theme.goodFor(), 0.5f),
                        dp(20)));
                updateStatus();
                if (matched.size() == deck.size()) {
                    host.celebrate(host.praise());
                    host.mouthSay(t("Hebat! Semua pasangan jumpa!", "Brilliant! All pairs found!"));
                }
            } else {
                host.wrongFeedback(cards.get(b));
                final int fa = a, fb = b;
                handler.postDelayed(() -> {
                    resetCard(fa);
                    resetCard(fb);
                    flipped.clear();
                    updateStatus();
                }, 900);
            }
        }
    }

    private void resetCard(int index) {
        TextView card = (TextView) cards.get(index);
        card.setText("?");
        card.setTextColor(theme.inkSoft());
        card.setBackground(Util.roundedStroke(theme.surface(), dp(20), theme.line(), dp(3)));
    }

    private void updateStatus() {
        statusView.setText(t("Langkah: ", "Moves: ") + moves
                + "   \u00B7   " + t("Jumpa: ", "Found: ") + (matched.size() / 2) + "/" + PAIRS);
    }

}
