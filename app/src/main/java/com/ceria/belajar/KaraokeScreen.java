package com.ceria.belajar;

import android.animation.ObjectAnimator;
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
    private android.widget.HorizontalScrollView scroll;
    private TextView status;
    private java.util.List<Content.Item> queue = new java.util.ArrayList<>();
    private int step;
    /**
     * Identifies the card whose audio is in flight. Both the audio-completion
     * callback and the stall timeout call {@link #finishCard}, and only the first
     * of them may move the song on; without this the song would skip a letter
     * whenever the timeout and the clip finished close together.
     */
    private long cardToken;
    /** The posted stall timeout, kept so it can be cancelled. */
    private Runnable pendingTimeout;
    /**
     * Test seam. A unit test cannot hear audio, and the shadow player reports
     * completion instantly, so a test that wants to prove the song waits for one
     * letter before starting the next must be able to hold the callback back.
     */
    interface Speaker {
        void speak(String word, Runnable done);
    }

    private Speaker speaker;

    void setSpeakerForTest(Speaker speaker) {
        this.speaker = speaker;
    }

    /**
     * A safety net under the audio completion callback. If a device never reports
     * that a clip ended (a stuck engine, a clip that fails silently) the song must
     * still move on rather than freeze on one letter. Set well above the longest
     * clip so it only fires when something has actually gone wrong.
     */
    private static final long STALL_TIMEOUT_MS = 4000;
    private long stallTimeoutMs = STALL_TIMEOUT_MS;

    void setStallTimeoutForTest(long ms) {
        this.stallTimeoutMs = ms;
    }

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

        // The whole alphabet is 26 cards, far wider than any phone. A plain row
        // would squeeze them all to a sliver, so the strip scrolls sideways and
        // the current card is scrolled into view as the song moves along.
        scroll = new android.widget.HorizontalScrollView(host);
        scroll.setHorizontalScrollBarEnabled(false);
        scroll.addView(strip, new android.widget.FrameLayout.LayoutParams(
                ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT));
        column.addView(scroll, matchWrap());

        column.addView(spacer(16));

        LinearLayout row1 = horizontal();
        row1.addView(bigButton("\uD83D\uDD24", t("ABC Song", "Lagu ABC"), 0xFFE8877A,
                () -> sing(t("Now sing the ABC!", "Jom nyanyi ABC!"),
                        Content.LETTERS, Content.LETTERS.size())));
        row1.addView(space(10));
        row1.addView(bigButton("\uD83D\uDD22", t("1 to 10", "1 hingga 10"), 0xFF7FB4D9,
                () -> sing(t("Now count to ten!", "Jom kira sampai sepuluh!"),
                        Content.NUMBERS, 10)));
        column.addView(row1, matchWrap());

        column.addView(spacer(10));

        LinearLayout row2 = horizontal();
        row2.addView(bigButton("\u23F9", t("Stop", "Berhenti"), 0xFF78909C,
                () -> {
                    stopSinging();
                    host.voice().stop();
                    status.setText(t("Pick a song!", "Pilih lagu!"));
                }));
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
        ui.post(this::advance);
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

        // In the ABC song the card shows the letter itself; showing the picture
        // emoji would put the alphabet behind a row of unrelated objects.
        String face = Content.LETTERS.contains(item) ? Content.letterGlyph(item) : item.symbol;
        TextView icon = Util.text(host, face, 30, 0xFF4A148C);
        icon.setTypeface(null, android.graphics.Typeface.BOLD);
        card.addView(icon);
        TextView label = Util.text(host, item.name(lang), 12, 0xFF6A1B9A);
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
        scrollTo(card);
        Content.Item item = itemAt(step);
        step++;
        if (item == null) {
            ui.post(this::advance);
            return;
        }
        host.music().duck();
        // Letters are sung by name, so the ABC song says "A, B, C" rather
        // than naming the picture that happens to start with the letter.
        String word = Content.LETTERS.contains(item)
                ? Content.letterGlyph(item)
                : item.spokenText(lang);
        // The next letter starts only when this one has finished playing, so a
        // 1.5 s clip is never cut off at a fixed 1.2 s and a slow engine is never
        // flushed before it makes a sound. The timeout is only a safety net.
        long token = ++cardToken;
        Runnable onDone = () -> finishCard(token);
        if (speaker != null) {
            speaker.speak(word, onDone);
        } else {
            host.voice().speakThen(lang, word, host.speech().rate(), onDone);
        }
        pendingTimeout = () -> finishCard(token);
        ui.postDelayed(pendingTimeout, stallTimeoutMs);
    }

    /**
     * Called by whichever comes first, the audio ending or the stall timeout.
     * The token makes the second caller a no-op, so a letter is never skipped.
     */
    private void finishCard(long token) {
        if (token != cardToken) {
            return;
        }
        cardToken++;    // retire this card so the other signal does nothing
        ui.removeCallbacks(pendingTimeout);
        pendingTimeout = null;
        ui.post(this::advance);
    }

    /** Keeps the card being sung visible as the song moves along the strip. */
    private void scrollTo(View card) {
        if (scroll == null) {
            return;
        }
        scroll.post(() -> {
            int target = card.getLeft() - (scroll.getWidth() - card.getWidth()) / 2;
            scroll.smoothScrollTo(Math.max(0, target), 0);
        });
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
        // Retire any audio callback still in flight: it must not restart the song
        // after the child has stopped it or left the screen.
        cardToken++;
        pendingTimeout = null;
        resetCards();
    }

    private View space(int widthDp) {
        View v = new View(host);
        v.setLayoutParams(new LinearLayout.LayoutParams(dp(widthDp), 1));
        return v;
    }

    private LinearLayout.LayoutParams cardParams() {
        // A fixed width, not a weight: 26 weighted cards would each be about
        // 15dp wide, so the alphabet would be unreadable. Fixed cards keep their
        // size and the strip scrolls instead.
        LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(dp(72), dp(96));
        lp.setMargins(dp(3), dp(4), dp(3), dp(4));
        return lp;
    }
}
