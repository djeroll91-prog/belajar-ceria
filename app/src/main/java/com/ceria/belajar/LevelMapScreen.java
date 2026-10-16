package com.ceria.belajar;

import android.graphics.Color;
import android.graphics.Typeface;
import android.view.Gravity;
import android.view.View;
import android.view.ViewGroup;
import android.widget.LinearLayout;
import android.widget.ProgressBar;
import android.widget.TextView;

import java.util.ArrayList;
import java.util.List;

/**
 * Learning path. Shows every level as a step on a colourful trail with a clear
 * progress trend. Locked levels cannot be opened until the previous one is
 * passed, so a child always finishes what they started.
 */
public class LevelMapScreen extends BaseScreen {

    public LevelMapScreen(MainActivity host) {
        super(host);
    }

    @Override
    protected String titleText() {
        return t("Learning Path \uD83D\uDDFA\uFE0F", "Laluan Belajar \uD83D\uDDFA\uFE0F");
    }

    @Override
    protected View buildBody() {
        Progress p = host.progress();
        LinearLayout column = vertical();
        column.setGravity(Gravity.CENTER_HORIZONTAL);

        int passed = 0;
        for (Curriculum.Level l : Curriculum.LEVELS) {
            if (p.isLevelPassed(l.id, l.passScore)) {
                passed++;
            }
        }
        int percent = passed * 100 / Curriculum.total();

        LinearLayout trend = new LinearLayout(host);
        trend.setOrientation(LinearLayout.VERTICAL);
        trend.setPadding(dp(16), dp(16), dp(16), dp(16));
        trend.setBackground(Util.gradient(0xFF7C4DFF, 0xFF40C4FF, dp(22)));
        trend.setElevation(dp(5));

        TextView heading = Util.text(host, "\uD83D\uDCC8 " + t("Progress Trend", "Trend Kemajuan"),
                20, Color.WHITE);
        heading.setTypeface(null, Typeface.BOLD);
        heading.setGravity(Gravity.START);
        trend.addView(heading);

        TextView summary = Util.text(host, passed + " / " + Curriculum.total()
                + t(" levels passed", " tahap lulus") + "   " + percent + "%", 16, Color.WHITE);
        summary.setGravity(Gravity.START);
        trend.addView(summary);

        ProgressBar bar = new ProgressBar(host, null, android.R.attr.progressBarStyleHorizontal);
        bar.setMax(100);
        bar.setProgress(percent);
        LinearLayout.LayoutParams barLp = new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, dp(16));
        barLp.setMargins(0, dp(8), 0, dp(4));
        trend.addView(bar, barLp);

        TextView levelLine = Util.text(host, "\uD83C\uDF96\uFE0F " + t("Level ", "Tahap ")
                + p.level() + t(" (overall)", " (keseluruhan)"), 14, Color.WHITE);
        levelLine.setGravity(Gravity.START);
        trend.addView(levelLine);

        LinearLayout.LayoutParams trendLp = new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
        trendLp.setMargins(0, 0, 0, dp(14));
        column.addView(trend, trendLp);

        for (Curriculum.Level level : Curriculum.LEVELS) {
            column.addView(levelRow(level), rowParams());
        }

        LinearLayout.LayoutParams btnLp = wrap();
        btnLp.setMargins(0, dp(16), 0, 0);
        column.addView(host.buildHeaderButton("\uD83C\uDFA8 " + t("Colouring Game",
                "Permainan Mewarna"), 0xFFFF4081,
                () -> host.open(new ColoringScreen(host))), btnLp);
        btnLp = wrap();
        btnLp.setMargins(0, dp(10), 0, 0);
        column.addView(host.buildHeaderButton("\uD83C\uDFC6 " + t("My Rewards", "Ganjaran Saya"),
                0xFFFFB300, () -> host.open(new RewardsScreen(host))), btnLp);

        return scrollWrap(column);
    }

    private LinearLayout.LayoutParams rowParams() {
        LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
        lp.setMargins(0, dp(6), 0, dp(6));
        return lp;
    }

    private View levelRow(final Curriculum.Level level) {
        Progress p = host.progress();
        boolean unlocked = p.isUnlocked(level.id);
        boolean passed = p.isLevelPassed(level.id, level.passScore);
        int best = p.levelBest(level.id);

        LinearLayout row = new LinearLayout(host);
        row.setOrientation(LinearLayout.HORIZONTAL);
        row.setGravity(Gravity.CENTER_VERTICAL);
        row.setPadding(dp(14), dp(14), dp(14), dp(14));
        int bg = passed ? 0xFF66BB6A : unlocked ? 0xFFFFFFFF : 0xFFBDBDBD;
        row.setBackground(Util.roundedStroke(bg, dp(20), passed ? 0xFF2E7D32 : 0xFFDDDDDD, dp(2)));
        row.setElevation(dp(unlocked ? 4 : 1));

        TextView badge = Util.text(host, unlocked ? level.emoji : "\uD83D\uDD12", 30,
                unlocked ? Color.WHITE : 0xFF757575);
        badge.setBackground(Util.rounded(unlocked ? level.color : 0xFFE0E0E0, dp(16)));
        badge.setPadding(dp(14), dp(8), dp(14), dp(8));
        row.addView(badge);

        LinearLayout info = new LinearLayout(host);
        info.setOrientation(LinearLayout.VERTICAL);
        info.setPadding(dp(12), 0, dp(6), 0);

        TextView title = Util.text(host, level.id + ". " + level.title(lang), 17,
                unlocked ? 0xFF2A1B54 : 0xFF757575);
        title.setTypeface(null, Typeface.BOLD);
        title.setGravity(Gravity.START);
        info.addView(title);

        String sub;
        if (passed) {
            sub = "\u2705 " + t("Passed - best ", "Lulus - terbaik ") + best + "/" + level.passScore;
        } else if (unlocked) {
            sub = "\uD83C\uDFAF " + t("Pass with ", "Lulus dengan ") + level.passScore
                    + t(" marks (best ", " markah (terbaik ") + best + ")";
        } else {
            sub = "\uD83D\uDD12 " + t("Pass level ", "Lulus tahap ") + (level.id - 1)
                    + t(" to unlock", " untuk buka");
        }
        TextView subView = Util.text(host, sub, 13, 0xFF546E7A);
        subView.setGravity(Gravity.START);
        info.addView(subView);

        row.addView(info, new LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f));

        if (unlocked) {
            LinearLayout buttons = new LinearLayout(host);
            buttons.setOrientation(LinearLayout.HORIZONTAL);

            TextView learn = Util.text(host, "\uD83D\uDCD6", 18, Color.WHITE);
            learn.setBackground(Util.rounded(0xFF29B6F6, dp(14)));
            learn.setPadding(dp(11), dp(8), dp(11), dp(8));
            learn.setOnClickListener(v -> openLevel(level));
            buttons.addView(learn);

            TextView quiz = Util.text(host, "\uD83C\uDFAF", 18, Color.WHITE);
            quiz.setBackground(Util.rounded(passed ? 0xFFFF9100 : 0xFF7C4DFF, dp(14)));
            quiz.setPadding(dp(11), dp(8), dp(11), dp(8));
            LinearLayout.LayoutParams qlp = new LinearLayout.LayoutParams(
                    ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT);
            qlp.setMargins(dp(6), 0, 0, 0);
            quiz.setLayoutParams(qlp);
            quiz.setOnClickListener(v -> host.open(new QuizGame(host, level)));
            buttons.addView(quiz);

            row.addView(buttons);
        } else {
            row.setOnClickListener(v -> host.mouthSay(t("Finish the level before this one first!",
                    "Siapkan tahap sebelum ini dulu ya!")));
        }
        return row;
    }

    private void openLevel(Curriculum.Level level) {
        List<Content.Item> items = new ArrayList<>(level.items);
        LearnScreen.Mode mode = level.id == 1 ? LearnScreen.Mode.LETTER
                : level.id == 2 ? LearnScreen.Mode.NUMBER : LearnScreen.Mode.WORD;
        host.open(new LearnScreen(host, items, mode)
                .withTitle(level.emoji + " " + level.en, level.emoji + " " + level.ms));
    }
}
