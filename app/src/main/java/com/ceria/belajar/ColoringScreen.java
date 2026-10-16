package com.ceria.belajar;

import android.graphics.Color;
import android.graphics.Typeface;
import android.view.Gravity;
import android.view.View;
import android.view.ViewGroup;
import android.widget.HorizontalScrollView;
import android.widget.LinearLayout;
import android.widget.TextView;

import java.util.ArrayList;

/** Easy colouring activity that teaches colour words in both languages. */
public class ColoringScreen extends BaseScreen {

    private ColorPageView page;
    private TextView nameView;
    private TextView hintView;
    private int coloredCount;

    public ColoringScreen(MainActivity host) {
        super(host);
    }

    @Override
    protected String titleText() {
        return t("Colouring Fun \uD83C\uDFA8", "Jom Mewarna \uD83C\uDFA8");
    }

    @Override
    protected View buildBody() {
        LinearLayout column = vertical();
        column.setGravity(Gravity.CENTER_HORIZONTAL);

        nameView = Util.text(host, "", 20, 0xFF4A148C);
        nameView.setTypeface(null, Typeface.BOLD);
        column.addView(nameView, matchWrap());

        page = new ColorPageView(host);
        page.setOnColorListener((en, ms, fill) -> {
            String colour = lang == Content.Lang.MS ? ms : en;
            nameView.setText("\uD83D\uDD8C\uFE0F " + colour);
            coloredCount++;
            host.progress().addPoints(2);
            host.progress().addCorrect();
            host.progress().earnedBadges();
            host.updateHeader();
            host.voice().speak(lang, colour);
            if (coloredCount % 6 == 0) {
                host.progress().addStar();
                host.updateHeader();
                host.celebrate(t("Beautiful colouring! \u2B50",
                        "Cantiknya! \u2B50"));
                hintView.setText(t("You earned a star!", "Kamu dapat satu bintang!"));
            }
        });
        LinearLayout.LayoutParams pageLp = new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, 0, 1f);
        pageLp.setMargins(0, dp(10), 0, dp(10));
        column.addView(page, pageLp);

        hintView = Util.text(host, t("Pick a colour, then tap the picture!",
                "Pilih warna, kemudian tekan gambar!"), 15, 0xFF6A1B9A);
        hintView.setPadding(0, 0, 0, dp(8));
        column.addView(hintView, matchWrap());

        HorizontalScrollView scroller = new HorizontalScrollView(host);
        scroller.setHorizontalScrollBarEnabled(false);
        LinearLayout palette = new LinearLayout(host);
        palette.setOrientation(LinearLayout.HORIZONTAL);
        palette.setGravity(Gravity.CENTER_VERTICAL);
        scroller.addView(palette, new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT));

        for (final Content.Swatch swatch : Content.SWATCHES) {
            TextView dot = Util.text(host, "", 1, Color.WHITE);
            LinearLayout.LayoutParams dotLp = new LinearLayout.LayoutParams(dp(52), dp(52));
            dotLp.setMargins(dp(4), dp(4), dp(4), dp(4));
            dot.setLayoutParams(dotLp);
            dot.setBackground(Util.roundedStroke(swatch.color, dp(26),
                    swatch.color == 0xFFFFFFFF ? 0xFF9E9E9E : 0x66000000, dp(2)));
            dot.setOnClickListener(v -> {
                page.setActiveColor(swatch.color);
                nameView.setText("\uD83D\uDD8C\uFE0F " + swatch.name(lang));
                host.voice().speak(lang, swatch.name(lang));
            });
            palette.addView(dot);
        }
        column.addView(scroller, matchWrap());

        LinearLayout actions = horizontal();
        actions.addView(button("\uD83D\uDD01", t("Clear", "Padam"),
                0xFF5C6BC0, () -> page.clearAll()));
        actions.addView(button("\uD83D\uDDAC", t("New picture", "Gambar baharu"),
                0xFFEC407A, () -> {
                    page.nextPicture();
                    updatePictureName();
                }));
        column.addView(actions, wrap());

        updatePictureName();
        return column;
    }

    private void updatePictureName() {
        String name = page.pictureName();
        nameView.setText("\uD83D\uDDAC " + t(name, name));
    }

    private TextView button(String emoji, String label, int color, Runnable action) {
        TextView b = Util.text(host, emoji + " " + label, 15, Color.WHITE);
        b.setBackground(Util.rounded(color, dp(18)));
        b.setPadding(dp(16), dp(10), dp(16), dp(10));
        LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT);
        lp.setMargins(dp(6), dp(10), dp(6), 0);
        b.setLayoutParams(lp);
        b.setTypeface(null, Typeface.BOLD);
        b.setOnClickListener(v -> action.run());
        return b;
    }
}
