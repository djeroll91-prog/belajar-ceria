package com.ceria.belajar;

import android.graphics.Color;
import android.view.Gravity;
import android.view.View;
import android.view.ViewGroup;
import android.widget.FrameLayout;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.TextView;

/**
 * Common skeleton for every activity screen: a friendly toolbar with a big back
 * button, a title, and a body area that subclasses fill in.
 */
public abstract class BaseScreen implements MainActivity.Screen {

    protected final MainActivity host;
    protected final Content.Lang lang;
    protected FrameLayout body;

    protected BaseScreen(MainActivity host) {
        this.host = host;
        this.lang = host.lang();
    }

    protected String t(String en, String ms) {
        return lang == Content.Lang.MS ? ms : en;
    }

    protected int dp(float value) {
        return Util.dp(host, value);
    }

    @Override
    public View build() {
        LinearLayout root = new LinearLayout(host);
        root.setOrientation(LinearLayout.VERTICAL);
        root.setBackground(Util.gradient(0xFFFFF9C4, 0xFFB3E5FC, 0));

        LinearLayout bar = new LinearLayout(host);
        bar.setOrientation(LinearLayout.HORIZONTAL);
        bar.setGravity(Gravity.CENTER_VERTICAL);
        bar.setPadding(dp(10), dp(8), dp(10), dp(8));
        bar.setBackgroundColor(0x33FFFFFF);

        TextView back = Util.text(host, "\u2B05\uFE0F", 22, Color.WHITE);
        back.setBackground(Util.rounded(0xFF7C4DFF, dp(18)));
        back.setPadding(dp(14), dp(4), dp(14), dp(4));
        back.setOnClickListener(v -> host.showHome());
        bar.addView(back);

        TextView title = Util.text(host, titleText(), 19, 0xFF4A148C);
        title.setTypeface(null, android.graphics.Typeface.BOLD);
        LinearLayout.LayoutParams titleLp = new LinearLayout.LayoutParams(
                0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f);
        titleLp.setMargins(dp(12), 0, 0, 0);
        bar.addView(title, titleLp);

        root.addView(bar, new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT));

        body = new FrameLayout(host);
        root.addView(body, new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, 0, 1f));

        View built = buildBody();
        body.addView(built, new FrameLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT));
        return root;
    }

    protected abstract String titleText();

    protected abstract View buildBody();

    protected ScrollView scrollWrap(View inner) {
        ScrollView scroll = new ScrollView(host);
        scroll.setFillViewport(true);
        scroll.addView(inner, new FrameLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT));
        return scroll;
    }

    protected LinearLayout vertical() {
        LinearLayout l = new LinearLayout(host);
        l.setOrientation(LinearLayout.VERTICAL);
        l.setPadding(dp(14), dp(14), dp(14), dp(24));
        return l;
    }

    protected LinearLayout horizontal() {
        LinearLayout l = new LinearLayout(host);
        l.setOrientation(LinearLayout.HORIZONTAL);
        l.setGravity(Gravity.CENTER);
        return l;
    }

    protected LinearLayout.LayoutParams lp(int w, int h) {
        return new LinearLayout.LayoutParams(w, h);
    }

    protected LinearLayout.LayoutParams weighted(int h) {
        return new LinearLayout.LayoutParams(0, h, 1f);
    }

    protected LinearLayout.LayoutParams wrap() {
        return new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT);
    }

    protected LinearLayout.LayoutParams matchWrap() {
        return new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
    }

    /** A big colourful button with an emoji and a label. */
    protected LinearLayout bigButton(String emoji, String label, int color, Runnable action) {
        LinearLayout b = new LinearLayout(host);
        b.setOrientation(LinearLayout.VERTICAL);
        b.setGravity(Gravity.CENTER);
        b.setPadding(dp(10), dp(14), dp(10), dp(14));
        b.setBackground(Util.rounded(color, dp(20)));
        b.setElevation(dp(4));
        b.addView(Util.text(host, emoji, 28, Color.WHITE));
        TextView t = Util.text(host, label, 15, Color.WHITE);
        t.setTypeface(null, android.graphics.Typeface.BOLD);
        b.addView(t);
        b.setOnClickListener(v -> {
            v.animate().scaleX(0.92f).scaleY(0.92f).setDuration(90)
                    .withEndAction(() -> {
                        v.animate().scaleX(1f).scaleY(1f).setDuration(90).start();
                        action.run();
                    }).start();
        });
        return b;
    }

    protected View spacer(int heightDp) {
        View v = new View(host);
        v.setLayoutParams(new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, dp(heightDp)));
        return v;
    }
}
