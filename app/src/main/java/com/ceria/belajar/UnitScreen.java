package com.ceria.belajar;

import android.view.Gravity;
import android.view.View;
import android.view.ViewGroup;
import android.widget.GridLayout;
import android.widget.LinearLayout;
import android.widget.TextView;

/**
 * Everything a child can do with one unit: read it, be quizzed on it, build its
 * words, or match its pictures. The content list at the bottom is tappable, so
 * an item can be heard on its own before the games start.
 */
public class UnitScreen extends BaseScreen {

    private final Content.Unit unit;
    private final Content.Tunjang tunjang;

    public UnitScreen(MainActivity host, Content.Unit unit) {
        super(host);
        this.unit = unit;
        this.tunjang = Content.tunjangById(Content.tunjangIdOf(unit));
    }

    @Override
    protected String titleText() {
        return unit.emoji + " " + unit.title(lang);
    }

    @Override
    protected View buildBody() {
        LinearLayout column = vertical();
        int color = tunjang == null ? theme.accent() : theme.tunjangColor(tunjang.color);

        String note = unit.code != null
                ? "KSPK " + unit.code + (unit.note != null ? " \u2014 " + unit.note : "")
                : unit.note;
        if (note != null) {
            TextView meta = Util.text(host, note, 13, theme.inkSoft());
            meta.setGravity(Gravity.START);
            meta.setPadding(dp(4), 0, 0, dp(12));
            column.addView(meta, matchWrap());
        }

        column.addView(actionRow(color), matchWrap());

        int mastered = host.progress().masteredIn(unit);
        column.addView(sectionTitle(t("Kandungan", "Contents"), unit.items.size(), mastered),
                matchWrap());

        GridLayout grid = new GridLayout(host);
        grid.setColumnCount(host.getResources().getConfiguration().screenWidthDp >= 600 ? 5 : 3);
        column.addView(grid, matchWrap());

        for (Content.Item item : unit.items) {
            grid.addView(itemTile(item), itemParams());
        }
        return scrollWrap(column);
    }

    private View actionRow(int color) {
        LinearLayout row = new LinearLayout(host);
        row.setOrientation(LinearLayout.HORIZONTAL);

        row.addView(action("\uD83D\uDCD6", t("Belajar", "Learn"), color,
                () -> host.open(new LearnScreen(host, unit))), weight());
        row.addView(action("\uD83C\uDFAF", t("Kuiz", "Quiz"), color,
                () -> host.open(new QuizScreen(host, unit))), weight());

        if (supportsBuild()) {
            row.addView(action("\uD83E\uDDE9", t("Bina", "Build"), color,
                    () -> host.open(new SpellGame(host, unit))), weight());
        } else if (supportsMatch()) {
            row.addView(action("\uD83D\uDD0D", t("Padan", "Match"), color,
                    () -> host.open(new MemoryGame(host, unit))), weight());
        }
        return row;
    }

    private boolean supportsBuild() {
        return "word".equals(unit.kind) || "text".equals(unit.kind);
    }

    private boolean supportsMatch() {
        return "emoji".equals(unit.kind) || "color".equals(unit.kind)
                || "number".equals(unit.kind) || "letter".equals(unit.kind);
    }

    private View action(String emoji, String label, int color, Runnable action) {
        LinearLayout b = new LinearLayout(host);
        b.setOrientation(LinearLayout.VERTICAL);
        b.setGravity(Gravity.CENTER);
        b.setPadding(dp(8), dp(12), dp(8), dp(12));
        b.setBackground(Util.rounded(color, dp(20)));
        b.setElevation(dp(3));
        b.setClickable(true);

        b.addView(Util.text(host, emoji, 26, theme.onAccent()));

        TextView text = Util.text(host, label, 14, theme.onAccent());
        text.setTypeface(null, android.graphics.Typeface.BOLD);
        b.addView(text);

        b.setOnClickListener(v -> {
            host.tapFeedback(v);
            v.postDelayed(action, 90);
        });
        return b;
    }

    private LinearLayout.LayoutParams weight() {
        LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(0,
                ViewGroup.LayoutParams.WRAP_CONTENT, 1f);
        lp.setMargins(dp(5), dp(5), dp(5), dp(5));
        return lp;
    }

    private View sectionTitle(String text, int count, int mastered) {
        LinearLayout row = new LinearLayout(host);
        row.setOrientation(LinearLayout.HORIZONTAL);
        row.setGravity(Gravity.CENTER_VERTICAL);
        row.setPadding(dp(4), dp(18), 0, dp(8));

        TextView label = Util.text(host, text, 17, theme.ink());
        label.setTypeface(null, android.graphics.Typeface.BOLD);
        row.addView(label);

        TextView badge = Util.text(host, mastered + "/" + count + " "
                + t("dikuasai", "mastered"), 12, theme.inkSoft());
        badge.setBackground(Util.rounded(theme.surface2(), dp(10)));
        badge.setPadding(dp(8), dp(3), dp(8), dp(3));
        LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT);
        lp.setMargins(dp(8), 0, 0, 0);
        row.addView(badge, lp);
        return row;
    }

    private GridLayout.LayoutParams itemParams() {
        GridLayout.LayoutParams lp = new GridLayout.LayoutParams();
        lp.width = 0;
        lp.height = dp(112);
        lp.columnSpec = GridLayout.spec(GridLayout.UNDEFINED, 1f);
        lp.setMargins(dp(5), dp(5), dp(5), dp(5));
        return lp;
    }

    private View itemTile(Content.Item item) {
        LinearLayout tile = new LinearLayout(host);
        tile.setOrientation(LinearLayout.VERTICAL);
        tile.setGravity(Gravity.CENTER);
        tile.setPadding(dp(6), dp(10), dp(6), dp(10));
        tile.setBackground(Util.roundedStroke(theme.surface(), dp(20), theme.line(), dp(2)));
        tile.setElevation(dp(2));
        tile.setClickable(true);

        TextView glyph = Util.text(host, Content.displayOf(item, lang), 30, theme.ink());
        glyph.setGravity(Gravity.CENTER);
        tile.addView(glyph);

        TextView name = Util.text(host, item.name(lang), 13, theme.ink());
        name.setTypeface(null, android.graphics.Typeface.BOLD);
        name.setGravity(Gravity.CENTER);
        tile.addView(name);

        // A mastered item gets a quiet tick, so progress is visible in place.
        if (host.progress().boxOf(unit.id, Progress.keyOf(item)) >= 2) {
            TextView tick = Util.text(host, "\u2705", 12, theme.inkSoft());
            tick.setGravity(Gravity.CENTER);
            tile.addView(tick);
        }

        tile.setOnClickListener(v -> {
            host.sfx().tap();
            host.pop(v);
            host.voice().speak(lang, Content.spokenOf(item, unit.kind, lang));
        });
        return tile;
    }
}
