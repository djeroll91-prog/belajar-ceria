package com.ceria.belajar;

import android.graphics.Color;
import android.graphics.drawable.GradientDrawable;
import android.view.Gravity;
import android.view.View;
import android.view.ViewGroup;
import android.widget.LinearLayout;
import android.widget.TextView;

/**
 * A toy xylophone. Eight rainbow keys play one octave of C major, so anything a
 * child taps sounds pleasant; there are no wrong notes to be discouraged by.
 *
 * Tapping a key also speaks its letter name, quietly linking the sound to the
 * alphabet while they play.
 */
public class XylophoneScreen extends BaseScreen {

    private static final int[] KEY_COLORS = {
            0xFFE53935, 0xFFFB8C00, 0xFFFDD835, 0xFF43A047,
            0xFF00ACC1, 0xFF3949AB, 0xFF8E24AA, 0xFFD81B60};

    private static final String[] LETTERS = {"C", "D", "E", "F", "G", "A", "B", "C"};

    private LinearLayout keysRow;
    private TextView nowPlaying;

    public XylophoneScreen(MainActivity host) {
        super(host);
    }

    @Override
    protected String titleText() {
        return t("\uD83C\uDFB9 Xylophone", "\uD83C\uDFB9 Xylophone");
    }

    @Override
    protected View buildBody() {
        LinearLayout column = vertical();
        column.setGravity(Gravity.CENTER_HORIZONTAL);

        nowPlaying = Util.text(host, t("Tap a key!", "Tekan kekunci!"), 18, 0xFF4A148C);
        nowPlaying.setPadding(0, dp(8), 0, dp(12));
        column.addView(nowPlaying);

        keysRow = new LinearLayout(host);
        keysRow.setOrientation(LinearLayout.HORIZONTAL);
        keysRow.setGravity(Gravity.BOTTOM | Gravity.CENTER_HORIZONTAL);
        column.addView(keysRow, matchWrap());

        for (int i = 0; i < 8; i++) {
            final int index = i;
            TextView key = Util.text(host, LETTERS[i], 22, Color.WHITE);
            key.setTypeface(null, android.graphics.Typeface.BOLD);
            key.setGravity(Gravity.CENTER);
            key.setClickable(true);
            // Keys get shorter towards the top of the scale, like a real toy.
            int height = dp(210 - i * 14);
            GradientDrawable bg = new GradientDrawable(
                    GradientDrawable.Orientation.TOP_BOTTOM,
                    new int[]{lighten(KEY_COLORS[i]), KEY_COLORS[i]});
            bg.setCornerRadius(dp(14));
            bg.setStroke(dp(2), 0xCCFFFFFF);
            key.setBackground(bg);
            key.setElevation(dp(5));
            key.setOnClickListener(v -> {
                host.sfx().note(index);
                bounce(v);
                nowPlaying.setText(LETTERS[index]);
                host.progress().addPoints(1);
                host.updateHeader();
            });
            LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(0, height, 1f);
            lp.setMargins(dp(3), dp(6), dp(3), dp(6));
            keysRow.addView(key, lp);
        }

        column.addView(spacer(18));

        LinearLayout tunes = horizontal();
        tunes.addView(bigButton("\uD83C\uDFB5", t("Twinkle", "Kelip-kelip"), 0xFF7C4DFF,
                () -> playTune(new int[]{0, 0, 4, 4, 5, 5, 4}, t("Twinkle twinkle!",
                        "Kelip-kelip bintang!"))));
        tunes.addView(spacerView(10));
        tunes.addView(bigButton("\uD83D\uDD14", t("Scale", "Tangga"), 0xFF00ACC1,
                () -> playTune(new int[]{0, 1, 2, 3, 4, 5, 6, 7}, t("Up we go!",
                        "Naik tangga!"))));
        tunes.addView(spacerView(10));
        tunes.addView(bigButton("\uD83C\uDF08", t("Surprise", "Kejutan"), 0xFFF2B457,
                this::randomTune));
        column.addView(tunes, matchWrap());

        return scrollWrap(column);
    }

    private void playTune(int[] notes, String message) {
        nowPlaying.setText(message);
        host.sfx().playTune(notes, 340);
    }

    private void randomTune() {
        int[] run = new int[6];
        for (int i = 0; i < run.length; i++) {
            run[i] = (int) (Math.random() * 8);
        }
        playTune(run, t("Magic music!", "Muzik ajaib!"));
    }

    private void bounce(View v) {
        v.animate().scaleX(0.9f).scaleY(0.9f).setDuration(80)
                .withEndAction(() -> v.animate().scaleX(1f).scaleY(1f)
                        .setDuration(120).start())
                .start();
    }

    private View spacerView(int widthDp) {
        View v = new View(host);
        v.setLayoutParams(new LinearLayout.LayoutParams(dp(widthDp), 1));
        return v;
    }

    private static int lighten(int color) {
        int r = Math.min(255, (int) (Color.red(color) * 1.35f));
        int g = Math.min(255, (int) (Color.green(color) * 1.35f));
        int b = Math.min(255, (int) (Color.blue(color) * 1.35f));
        return Color.rgb(r, g, b);
    }
}
