package com.ceria.belajar;

import android.animation.ObjectAnimator;
import android.graphics.Color;
import android.graphics.drawable.GradientDrawable;
import android.os.Handler;
import android.os.Looper;
import android.view.Gravity;
import android.view.View;
import android.view.ViewGroup;
import android.widget.LinearLayout;
import android.widget.TextView;

/**
 * Sing-along karaoke. The alphabet (or a number line, or a simple song) is laid
 * out in cards; while it plays, the current card grows and glows so a child who
 * cannot read yet can follow along by watching where the music is.
 *
 * The voice is the phone's own engine singing one item at a time, so this works
 * with the same speech pack as the rest of the app and needs no new recordings.
 */
public class KaraokeScreen extends BaseScreen {

    private final Handler ui = new Handler(Looper.getMainLooper());
    private LinearLayout strip;
    private TextView status;
    private java.util.List<Content.Item> queue = new java.util.ArrayList<>();
    private int step;
    private Runnable ticker;

    public KaraokeScreen(MainActivity host) {
        super(host);
    }

    @Override
    protected String titleText() {
        return t("\uD83C\uDFB5 Sing Along", "\uD83C\uDFB5 Mari Menyanyi");
    }

    @Override
    protected View buildBody() {
        LinearLayout column = vertical();
        column.setGravity(Gravity.CENTER_HORIZONTAL);

        status = Util.text(host, t("Pick a song!", "Pilih lagu!"), 18, 0xFF4A148C);
        status.setPadding(0, dp(6), 0, dp(12));
        column.addView(status);

        strip = new LinearLayout(host);
        strip.setOrientation(LinearLayout.HORIZONTAL);
        strip.setGravity(Gravity.CENTER);
        strip.setPadding(dp(4), dp(4), dp(4), dp(4));
        column.addView(strip, matchWrap());

        column.addView(spacer(16));

        LinearLayout row1 = horizontal();
        row1.addView(bigButton("\uD83D\uDD24", t("ABC Song", "Lagu ABC"), 0xFFE8877A,
                () -> sing(t("Now sing the ABC!", "Jom nyanyi ABC!"),
                        Content.LETTERS, 1)));
        row1.addView(space(10));
        row1.addView(bigButton("\uD83D\uDD22", t("1 to 10", "1 hingga 10"), 0xFF7FB4D9,
                () -> sing(t("Now count to ten!", "Jom kira sampai sepuluh!"),
                        Content.NUMBERS, 10)));
        column.addView(row1, matchWrap());

        column.addView(spacer(10));

        LinearLayout row2 = horizontal();
        row2.addView(bigButton("\u23F9", t("Stop", "Berhenti"), 0xFF78909C,
                this::stopSinging));
        column.addView(row2, matchWrap());

        return scrollWrap(column);
    }

    /** Builds a row of cards and starts walking through them. */
    private void sing(String message, java.util.List<Content.Item> items, int count) {
        stopSinging();
        status.setText(message);
        strip.removeAllViews();
        int shown = Math.min(count, items.size());
        queue = new java.util.ArrayList<>(items.subList(0, shown));
        for (int i = 0; i < shown; i++) {
            strip.addView(card(items.get(i), i), cardParams());
        }
        step = 0;
        ticker = this::advance;
        ui.post(ticker);
    }

    private View card(Content.Item item, int index) {
        LinearLayout card = new LinearLayout(host);
        card.setOrientation(LinearLayout.VERTICAL);
        card.setGravity(Gravity.CENTER);
        card.setPadding(dp(2), dp(10), dp(2), dp(10));
        card.setTag(index);
        GradientDrawable bg = new GradientDrawable(
                GradientDrawable.Orientation.TOP_BOTTOM,
                new int[]{0xFFFFFFFF, 0xFFF3E5F5});
        bg.setCornerRadius(dp(14));
        bg.setStroke(dp(2), 0xFFCE93D8);
        card.setBackground(bg);

        TextView icon = Util.text(host, item.symbol, 26, Color.WHITE);
        card.addView(icon);
        TextView label = Util.text(host, item.name(lang), 13, 0xFF4A148C);
        label.setTypeface(null, android.graphics.Typeface.BOLD);
        card.addView(label);
        return card;
    }

    /** Speaks one item, highlights its card, then schedules the next. */
    private void advance() {
        if (strip == null || !strip.isAttachedToWindow()) {
            return;     // the child left the screen; stop singing
        }
        if (step >= strip.getChildCount()) {
            finish();
            return;
        }
        View card = strip.getChildAt(step);
        highlight(card);
        Content.Item item = itemAt(step);
        if (item != null) {
            host.music().duck();
            host.voice().speak(lang, item.spokenText(lang));
        }
        step++;
        ui.postDelayed(this::advance, 1200);
    }

    private Content.Item itemAt(int index) {
        return index >= 0 && index < queue.size() ? queue.get(index) : null;
    }

    private void highlight(View card) {
        for (int i = 0; i < strip.getChildCount(); i++) {
            View child = strip.getChildAt(i);
            boolean active = child == card;
            child.animate().scaleX(active ? 1.18f : 1f).scaleY(active ? 1.18f : 1f)
                    .alpha(active ? 1f : 0.55f).setDuration(220).start();
        }
        ObjectAnimator.ofFloat(card, "translationY", 0f, -dp(8), 0f)
                .setDuration(420).start();
    }

    private void finish() {
        status.setText(t("Hooray! You sang it all!", "Hebat! Kamu sudah nyanyi semua!"));
        host.progress().addStars(1);
        host.progress().addPoints(10);
        host.updateHeader();
        host.celebrate(t("What a lovely voice!", "Suara yang merdu!"));
        resetCards();
    }

    private void resetCards() {
        if (strip == null) {
            return;
        }
        for (int i = 0; i < strip.getChildCount(); i++) {
            strip.getChildAt(i).animate().scaleX(1f).scaleY(1f).alpha(1f)
                    .setDuration(200).start();
        }
    }

    private void stopSinging() {
        ui.removeCallbacksAndMessages(null);
        resetCards();
    }

    private View space(int widthDp) {
        View v = new View(host);
        v.setLayoutParams(new LinearLayout.LayoutParams(dp(widthDp), 1));
        return v;
    }

    private LinearLayout.LayoutParams cardParams() {
        LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(0, dp(96), 1f);
        lp.setMargins(dp(2), dp(4), dp(2), dp(4));
        return lp;
    }
}
