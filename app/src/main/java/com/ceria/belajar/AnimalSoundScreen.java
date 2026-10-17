package com.ceria.belajar;

import android.graphics.Color;
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
 * Listen and find. Bobo says an animal name in the chosen language and the child
 * taps the matching picture.
 *
 * This is a listening exercise rather than a reading one, so it works for a
 * child who cannot read yet: the prompt is spoken, and the answers are pictures.
 * Tapping a picture freely (outside a round) simply names it, so exploring is
 * always rewarded.
 */
public class AnimalSoundScreen extends BaseScreen {

    private static final int ROUND_SIZE = 4;

    private final Handler ui = new Handler(Looper.getMainLooper());
    private final List<Content.Item> pool = new ArrayList<>();

    private GridLayout grid;
    private TextView prompt;
    private TextView scoreView;
    private Content.Item target;
    private int found;
    private int rounds;

    public AnimalSoundScreen(MainActivity host) {
        super(host);
    }

    @Override
    protected String titleText() {
        return t("\uD83D\uDC15 Listen & Find", "\uD83D\uDC15 Dengar & Cari");
    }

    @Override
    protected View buildBody() {
        LinearLayout column = vertical();
        column.setGravity(Gravity.CENTER_HORIZONTAL);

        prompt = Util.text(host, "", 19, 0xFF4A148C);
        prompt.setGravity(Gravity.CENTER);
        prompt.setPadding(dp(4), dp(6), dp(4), dp(6));
        column.addView(prompt, matchWrap());

        scoreView = Util.text(host, "", 16, 0xFF00695C);
        scoreView.setPadding(0, 0, 0, dp(10));
        column.addView(scoreView);

        grid = new GridLayout(host);
        grid.setColumnCount(2);
        column.addView(grid, matchWrap());

        column.addView(spacer(14));
        column.addView(bigButton("\uD83D\uDD0A", t("Say it again", "Sebut lagi"),
                0xFF7C4DFF, this::askAgain), matchWrap());

        pool.addAll(Content.ANIMALS);
        startRound();
        return scrollWrap(column);
    }

    private void startRound() {
        if (grid == null || !grid.isAttachedToWindow()) {
            return;     // the child has already left this screen
        }
        List<Content.Item> choices = pickRound(pool, ROUND_SIZE);
        target = choices.get(0);

        grid.removeAllViews();
        for (Content.Item item : choices) {
            grid.addView(card(item), cardParams());
        }
        askAgain();
    }

    private void askAgain() {
        if (target == null) {
            return;
        }
        prompt.setText(t("Which one is the ", "Yang mana ") + target.name(lang) + "?");
        host.mouthSay(target.name(lang));
    }

    private View card(Content.Item item) {
        LinearLayout card = new LinearLayout(host);
        card.setOrientation(LinearLayout.VERTICAL);
        card.setGravity(Gravity.CENTER);
        card.setPadding(dp(8), dp(14), dp(8), dp(14));
        card.setBackground(Util.roundedStroke(0xFF8FBF7F, dp(22), 0xCCFFFFFF, dp(3)));
        card.setElevation(dp(5));
        card.setClickable(true);

        TextView icon = Util.text(host, item.symbol, 46, Color.WHITE);
        card.addView(icon);
        TextView label = Util.text(host, item.name(lang), 15, Color.WHITE);
        label.setTypeface(null, android.graphics.Typeface.BOLD);
        card.addView(label);

        card.setOnClickListener(v -> {
            if (item == target) {
                found++;
                rounds++;
                host.progress().addPoints(10);
                host.correctFeedback(v);
                host.celebrate(t("You found it! ", "Jumpa! ") + item.name(lang));
                host.updateHeader();
                updateScore();
                ui.postDelayed(this::startRound, 1400);
            } else {
                host.sfx().wrong();
                host.mouthSay(t("That is the ", "Itu ") + item.name(lang)
                        + t(". Listen again!", ". Dengar lagi!"));
                wiggle(v);
            }
        });
        return card;
    }

    private void wiggle(View v) {
        v.animate().translationX(dp(8)).setDuration(70)
                .withEndAction(() -> v.animate().translationX(-dp(8)).setDuration(70)
                        .withEndAction(() -> v.animate().translationX(0).setDuration(70)
                                .start()).start())
                .start();
    }

    /** Picks the pictures for one round; the first is the answer to find. */
    static List<Content.Item> pickRound(List<Content.Item> pool, int size) {
        List<Content.Item> choices = new ArrayList<>(pool);
        Collections.shuffle(choices);
        return new ArrayList<>(choices.subList(0, Math.min(size, choices.size())));
    }

    private void updateScore() {
        scoreView.setText("\u2B50 " + found + " / " + rounds);
    }

    private GridLayout.LayoutParams cardParams() {
        GridLayout.LayoutParams lp = new GridLayout.LayoutParams();
        lp.width = 0;
        lp.height = dp(150);
        lp.columnSpec = GridLayout.spec(GridLayout.UNDEFINED, 1f);
        lp.setMargins(dp(6), dp(6), dp(6), dp(6));
        return lp;
    }

    @Override
    protected void finalize() throws Throwable {
        ui.removeCallbacksAndMessages(null);
        super.finalize();
    }
}
