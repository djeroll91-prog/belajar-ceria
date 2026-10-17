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

/** Memory match: find the pairs of pictures and their names. */
public class MemoryGame extends BaseScreen {

    private final Handler handler = new Handler(Looper.getMainLooper());
    private final List<Content.Item> deck = new ArrayList<>();
    private final List<View> cards = new ArrayList<>();
    private final List<Integer> flipped = new ArrayList<>();
    private final List<Integer> matched = new ArrayList<>();
    private int moves;
    private TextView statusView;
    private GridLayout grid;

    public MemoryGame(MainActivity host) {
        super(host);
    }

    @Override
    protected String titleText() {
        return t("Memory Match \uD83E\uDDE9", "Padanan Memori \uD83E\uDDE9");
    }

    @Override
    protected View buildBody() {
        LinearLayout column = vertical();
        column.setGravity(Gravity.CENTER_HORIZONTAL);

        statusView = Util.text(host, "", 18, 0xFF4A148C);
        column.addView(statusView, matchWrap());

        grid = new GridLayout(host);
        grid.setColumnCount(4);
        column.addView(grid, matchWrap());

        column.addView(host.buildHeaderButton(t("\uD83D\uDD01 Main semula", "\uD83D\uDD01 Main semula"),
                0xFF5C6BC0, this::startGame), wrap());
        startGame();
        return column;
    }

    private void startGame() {
        host.progress().registerGamePlayed();
        deck.clear();
        List<Content.Item> pool = new ArrayList<>();
        pool.addAll(Content.WORDS.subList(0, 6));
        pool.addAll(Content.NUMBERS.subList(0, 2));
        Collections.shuffle(pool);
        for (int i = 0; i < 6 && i < pool.size(); i++) {
            deck.add(pool.get(i));
            deck.add(pool.get(i));
        }
        Collections.shuffle(deck);
        flipped.clear();
        matched.clear();
        moves = 0;
        buildGrid();
        updateStatus();
    }

    private void buildGrid() {
        grid.removeAllViews();
        cards.clear();
        for (int i = 0; i < deck.size(); i++) {
            final int idx = i;
            LinearLayout card = new LinearLayout(host);
            card.setOrientation(LinearLayout.VERTICAL);
            card.setGravity(Gravity.CENTER);
            card.setBackground(Util.rounded(0xFF7C4DFF, dp(16)));
            card.setElevation(dp(4));

            TextView face = Util.text(host, "?", 34, Color.WHITE);
            card.addView(face);

            card.setOnClickListener(v -> onCardClicked(idx, card, face));

            GridLayout.LayoutParams lp = new GridLayout.LayoutParams();
            lp.width = 0;
            lp.height = dp(84);
            lp.columnSpec = GridLayout.spec(GridLayout.UNDEFINED, 1f);
            lp.setMargins(dp(5), dp(5), dp(5), dp(5));
            grid.addView(card, lp);
            cards.add(card);
        }
    }

    private void onCardClicked(int index, LinearLayout card, TextView face) {
        if (flipped.contains(index) || matched.contains(index) || flipped.size() >= 2) {
            return;
        }
        reveal(card, face, index);
        flipped.add(index);
        if (flipped.size() == 2) {
            moves++;
            updateStatus();
            int a = flipped.get(0);
            int b = flipped.get(1);
            if (deck.get(a) == deck.get(b)) {
                matched.add(a);
                matched.add(b);
                host.progress().addPoints(5);
                host.progress().addCorrect();
                host.progress().onCorrectAnswer();
                host.progress().earnedBadges();
                host.updateHeader();
                flipped.clear();
                Content.Item item = deck.get(a);
                host.celebrate(t("Match! ", "Padan! ") + item.name(lang) + " \u2B50");
                if (matched.size() == deck.size()) {
                    host.progress().addStar();
                    host.updateHeader();
                    statusView.setText(t("You did it in ", "Kamu siap dalam ") + moves
                            + t(" moves! \uD83C\uDF89", " langkah! \uD83C\uDF89"));
                }
            } else {
                host.progress().resetStreak();
                host.sfx().wrong();
                handler.postDelayed(() -> {
                    hide(cards.get(a), (TextView) ((LinearLayout) cards.get(a)).getChildAt(0));
                    hide(cards.get(b), (TextView) ((LinearLayout) cards.get(b)).getChildAt(0));
                    flipped.clear();
                }, 750);
            }
        }
    }

    private void reveal(View card, TextView face, int index) {
        Content.Item item = deck.get(index);
        boolean number = Content.NUMBERS.contains(item);
        String top = Content.LETTERS.contains(item) ? Content.letterEmoji(item) : item.symbol;
        if (number) {
            top = item.symbol;
        }
        face.setText(top + "\n" + item.name(lang));
        face.setTextSize(16);
        card.setBackground(Util.rounded(0xFFFFB300, dp(16)));
        host.sfx().tap();
        card.setScaleX(0.9f);
        card.setScaleY(0.9f);
        card.animate().scaleX(1f).scaleY(1f).setDuration(180).start();
    }

    private void hide(View card, TextView face) {
        face.setText("?");
        face.setTextSize(34);
        card.setBackground(Util.rounded(0xFF7C4DFF, dp(16)));
    }

    private void updateStatus() {
        statusView.setText(t("Moves: ", "Langkah: ") + moves + "   "
                + t("Pairs: ", "Pasangan: ") + (matched.size() / 2) + "/" + (deck.size() / 2));
    }
}
