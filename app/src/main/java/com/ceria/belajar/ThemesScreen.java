package com.ceria.belajar;

import android.view.Gravity;
import android.view.View;
import android.view.ViewGroup;
import android.widget.LinearLayout;
import android.widget.TextView;

import java.util.Calendar;

/**
 * The 42 weekly themes from the KSPK/tadika year, with the current week picked
 * out. A tadika teacher works to one theme a week, so this is what makes the app
 * line up with what the child is doing in class.
 */
public class ThemesScreen extends BaseScreen {

    public ThemesScreen(MainActivity host) {
        super(host);
    }

    @Override
    protected String titleText() {
        return t("Weekly Themes \uD83D\uDCC5", "Tema Mingguan \uD83D\uDCC5");
    }

    @Override
    protected View buildBody() {
        LinearLayout column = vertical();

        int current = currentWeek();
        TextView sub = Util.text(host, t("Minggu ini: ", "This week: ") + current,
                13, theme.inkSoft());
        sub.setGravity(Gravity.START);
        sub.setPadding(dp(4), 0, 0, dp(12));
        column.addView(sub, matchWrap());

        for (Content.Theme week : Content.THEMES) {
            column.addView(themeRow(week, week.week == current), matchWrap());
        }
        return scrollWrap(column);
    }

    /**
     * Week number in the same 1..42 range the theme list uses. Clamped rather
     * than wrapped, because a theme list is a year's plan and week 43 is really
     * week 42's leftovers.
     */
    private int currentWeek() {
        Calendar now = Calendar.getInstance();
        int week = now.get(Calendar.WEEK_OF_YEAR);
        return Math.max(1, Math.min(Content.THEMES.size(), week));
    }

    private View themeRow(Content.Theme week, boolean isCurrent) {
        LinearLayout row = new LinearLayout(host);
        row.setOrientation(LinearLayout.HORIZONTAL);
        row.setGravity(Gravity.CENTER_VERTICAL);
        row.setPadding(dp(14), dp(12), dp(14), dp(12));

        int color = isCurrent ? theme.accent() : theme.line();
        row.setBackground(Util.roundedStroke(
                isCurrent ? theme.wash(theme.accent(), 0.14f) : theme.surface(),
                dp(18), color, dp(isCurrent ? 3 : 2)));
        if (isCurrent) {
            row.setElevation(dp(3));
        }

        TextView number = Util.text(host, String.valueOf(week.week), 15,
                isCurrent ? theme.onAccent() : theme.inkSoft());
        number.setTypeface(null, android.graphics.Typeface.BOLD);
        number.setGravity(Gravity.CENTER);
        number.setBackground(Util.rounded(isCurrent ? theme.accent() : theme.surface2(), dp(14)));
        LinearLayout.LayoutParams numLp = new LinearLayout.LayoutParams(dp(38), dp(38));
        numLp.setMargins(0, 0, dp(12), 0);
        row.addView(number, numLp);

        LinearLayout texts = new LinearLayout(host);
        texts.setOrientation(LinearLayout.VERTICAL);

        TextView name = Util.text(host, week.title(lang), 16, theme.ink());
        name.setTypeface(null, android.graphics.Typeface.BOLD);
        name.setGravity(Gravity.START);
        texts.addView(name);

        if (week.note != null) {
            TextView note = Util.text(host, week.note, 12, theme.inkSoft());
            note.setGravity(Gravity.START);
            texts.addView(note);
        }
        row.addView(texts, new LinearLayout.LayoutParams(0,
                ViewGroup.LayoutParams.WRAP_CONTENT, 1f));

        if (isCurrent) {
            TextView now = Util.text(host, t("SEKARANG", "NOW"), 10, theme.accent());
            now.setTypeface(null, android.graphics.Typeface.BOLD);
            row.addView(now);
        }

        LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
        lp.setMargins(0, dp(4), 0, dp(4));
        row.setLayoutParams(lp);
        return row;
    }
}
