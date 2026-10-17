package com.ceria.belajar;

import android.graphics.Color;
import android.graphics.Typeface;
import android.view.Gravity;
import android.view.View;
import android.view.ViewGroup;
import android.widget.GridLayout;
import android.widget.LinearLayout;
import android.widget.ProgressBar;
import android.widget.TextView;

import java.util.List;

/** Reward centre: points, level, star album and badges that motivate the child. */
public class RewardsScreen extends BaseScreen {

    public RewardsScreen(MainActivity host) {
        super(host);
    }

    @Override
    protected String titleText() {
        return t("My Rewards \uD83C\uDFC6", "Ganjaran Saya \uD83C\uDFC6");
    }

    @Override
    protected View buildBody() {
        Progress p = host.progress();
        LinearLayout column = vertical();
        column.setGravity(Gravity.CENTER_HORIZONTAL);

        LinearLayout hero = new LinearLayout(host);
        hero.setOrientation(LinearLayout.VERTICAL);
        hero.setGravity(Gravity.CENTER);
        hero.setPadding(dp(16), dp(18), dp(16), dp(18));
        hero.setBackground(Util.gradient(0xFFFFB300, 0xFFFF7043, dp(24)));
        hero.setElevation(dp(6));

        TextView level = Util.text(host, "\uD83C\uDF96\uFE0F " + t("Level ", "Tahap ") + p.level(),
                26, Color.WHITE);
        level.setTypeface(null, Typeface.BOLD);
        level.setScaleX(0.7f);
        level.setScaleY(0.7f);
        level.animate().scaleX(1f).scaleY(1f).setDuration(500).start();
        hero.addView(level);

        TextView pts = Util.text(host, p.points() + t(" points", " markah"), 18, Color.WHITE);
        hero.addView(pts);

        ProgressBar bar = new ProgressBar(host, null, android.R.attr.progressBarStyleHorizontal);
        bar.setMax(100);
        bar.setProgress(p.levelProgress());
        LinearLayout.LayoutParams barLp = new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, dp(16));
        barLp.setMargins(0, dp(10), 0, dp(4));
        hero.addView(bar, barLp);

        TextView nextHint = Util.text(host, (100 - p.levelProgress())
                + t(" more points to level up!", " markah lagi untuk naik tahap!"), 14, Color.WHITE);
        hero.addView(nextHint);

        LinearLayout.LayoutParams heroLp = new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
        heroLp.setMargins(0, 0, 0, dp(14));
        column.addView(hero, heroLp);

        LinearLayout stats = new LinearLayout(host);
        stats.setOrientation(LinearLayout.HORIZONTAL);
        stats.setGravity(Gravity.CENTER);
        stats.addView(statCard("\u2B50", p.stars() + "", t("Stars", "Bintang"), 0xFFFFC107));
        stats.addView(statCard("\u2705", p.correct() + "", t("Correct", "Betul"), 0xFF66BB6A));
        stats.addView(statCard("\uD83C\uDFA4", p.spoken() + "", t("Spoken", "Sebut"), 0xFFFF4081));
        stats.addView(statCard("\uD83D\uDD25", p.bestStreak() + "", t("Best streak", "Rentetan"), 0xFF29B6F6));
        column.addView(stats, matchWrap());

        TextView badgeTitle = Util.text(host, t("Badge Collection", "Koleksi Lencana"), 20,
                0xFF4A148C);
        badgeTitle.setTypeface(null, Typeface.BOLD);
        badgeTitle.setPadding(0, dp(18), 0, dp(8));
        badgeTitle.setGravity(Gravity.START);
        column.addView(badgeTitle, matchWrap());

        GridLayout badgeGrid = new GridLayout(host);
        badgeGrid.setColumnCount(4);
        List<Progress.Badge> earned = Progress.BADGES;
        for (Progress.Badge badge : earned) {
            badgeGrid.addView(badgeTile(badge, p.hasBadge(badge)), badgeParams());
        }
        column.addView(badgeGrid, matchWrap());

        LinearLayout.LayoutParams btnLp = wrap();
        btnLp.setMargins(0, dp(18), 0, 0);
        column.addView(host.buildHeaderButton("\uD83C\uDF89 " + t("Keep learning!",
                "Terus belajar!"), 0xFF7C4DFF, () -> host.showHome()), btnLp);

        return scrollWrap(column);
    }

    private View statCard(String emoji, String value, String label, int color) {
        LinearLayout c = new LinearLayout(host);
        c.setOrientation(LinearLayout.VERTICAL);
        c.setGravity(Gravity.CENTER);
        c.setPadding(dp(6), dp(10), dp(6), dp(10));
        c.setBackground(Util.rounded(color, dp(18)));
        c.addView(Util.text(host, emoji, 22, Color.WHITE));
        TextView v = Util.text(host, value, 20, Color.WHITE);
        v.setTypeface(null, Typeface.BOLD);
        c.addView(v);
        c.addView(Util.text(host, label, 12, Color.WHITE));
        LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(0, dp(96), 1f);
        lp.setMargins(dp(4), 0, dp(4), 0);
        c.setLayoutParams(lp);
        return c;
    }

    private GridLayout.LayoutParams badgeParams() {
        GridLayout.LayoutParams lp = new GridLayout.LayoutParams();
        lp.width = 0;
        lp.height = dp(96);
        lp.columnSpec = GridLayout.spec(GridLayout.UNDEFINED, 1f);
        lp.setMargins(dp(5), dp(5), dp(5), dp(5));
        return lp;
    }

    private View badgeTile(Progress.Badge badge, boolean unlocked) {
        LinearLayout c = new LinearLayout(host);
        c.setOrientation(LinearLayout.VERTICAL);
        c.setGravity(Gravity.CENTER);
        c.setPadding(dp(4), dp(8), dp(4), dp(8));
        c.setBackground(Util.rounded(unlocked ? 0xFFFFD600 : 0xFFE0E0E0, dp(16)));
        c.setElevation(dp(3));
        c.addView(Util.text(host, unlocked ? badge.emoji : "\uD83D\uDD12", 30,
                unlocked ? Color.WHITE : 0xFF9E9E9E));
        TextView t = Util.text(host, badge.title(lang), 11, unlocked ? 0xFF4A148C : 0xFF757575);
        t.setTypeface(null, Typeface.BOLD);
        c.addView(t);
        return c;
    }
}
