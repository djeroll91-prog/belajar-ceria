package com.ceria.belajar;

import android.view.Gravity;
import android.view.View;
import android.view.ViewGroup;
import android.widget.LinearLayout;
import android.widget.TextView;

/**
 * Settings, aimed at the parent or teacher rather than the child: language,
 * theme, sound, what the app follows, and how to clear progress.
 */
public class SettingsScreen extends BaseScreen {

    public SettingsScreen(MainActivity host) {
        super(host);
    }

    @Override
    protected String titleText() {
        return t("Settings \u2699\uFE0F", "Tetapan \u2699\uFE0F");
    }

    @Override
    protected View buildBody() {
        LinearLayout column = vertical();

        TextView sub = Util.text(host, t("Untuk ibu bapa dan guru",
                "For parents and teachers"), 13, theme.inkSoft());
        sub.setGravity(Gravity.START);
        sub.setPadding(dp(4), 0, 0, dp(12));
        column.addView(sub, matchWrap());

        column.addView(section(t("Bahasa", "Language")), matchWrap());
        LinearLayout langRow = new LinearLayout(host);
        langRow.setOrientation(LinearLayout.HORIZONTAL);
        langRow.addView(choice("Bahasa Melayu", lang == Content.Lang.MS, () -> {
            host.progress().setLang(Content.Lang.MS);
            host.rebuildHome();
        }), weight());
        langRow.addView(choice("English", lang == Content.Lang.EN, () -> {
            host.progress().setLang(Content.Lang.EN);
            host.rebuildHome();
        }), weight());
        column.addView(langRow, matchWrap());

        column.addView(section(t("Tema", "Theme")), matchWrap());
        LinearLayout themeRow = new LinearLayout(host);
        themeRow.setOrientation(LinearLayout.HORIZONTAL);
        themeRow.addView(choice("\u2600\uFE0F " + t("Cerah", "Light"), !theme.isDark(), () -> {
            theme.setMode(Theme.Mode.LIGHT);
            host.rebuildForTheme();
        }), weight());
        themeRow.addView(choice("\uD83C\uDF19 " + t("Gelap", "Dark"), theme.isDark(), () -> {
            theme.setMode(Theme.Mode.DARK);
            host.rebuildForTheme();
        }), weight());
        column.addView(themeRow, matchWrap());

        column.addView(section(t("Bunyi", "Sound")), matchWrap());
        LinearLayout soundRow = new LinearLayout(host);
        soundRow.setOrientation(LinearLayout.HORIZONTAL);
        soundRow.addView(choice(host.isSoundOn()
                ? "\uD83D\uDD0A " + t("Hidup", "On") : "\uD83D\uDD07 " + t("Mati", "Off"),
                host.isSoundOn(), () -> {
                    host.setSoundOn(!host.isSoundOn());
                    host.rebuildHome();
                }), weight());
        soundRow.addView(choice("\uD83C\uDFB5 " + t("Muzik", "Music"), host.isMusicOn(), () -> {
            host.setMusicOn(!host.isMusicOn());
            host.rebuildHome();
        }), weight());
        column.addView(soundRow, matchWrap());

        LinearLayout testRow = new LinearLayout(host);
        testRow.setOrientation(LinearLayout.HORIZONTAL);
        testRow.addView(choice(t("Uji suara", "Test voice"), false, () ->
                host.voice().speak(lang, t("Hai kawan! Jom belajar.", "Hi friend! Let's learn."))),
                weight());
        column.addView(testRow, matchWrap());

        column.addView(section(t("Kurikulum", "Curriculum")), matchWrap());
        column.addView(infoCard(), matchWrap());

        column.addView(section(t("Set semula", "Reset")), matchWrap());
        LinearLayout resetRow = new LinearLayout(host);
        resetRow.setOrientation(LinearLayout.HORIZONTAL);
        resetRow.addView(choice(t("Set semula penguasaan", "Reset mastery"), false, () -> {
            host.progress().resetMastery();
            host.showHome();
        }), weight());
        resetRow.addView(choice(t("Padam semua", "Erase everything"), false, () -> {
            host.progress().resetAll();
            host.showHome();
        }), weight());
        column.addView(resetRow, matchWrap());

        return scrollWrap(column);
    }

    private View section(String text) {
        TextView label = Util.text(host, text, 17, theme.ink());
        label.setTypeface(null, android.graphics.Typeface.BOLD);
        label.setGravity(Gravity.START);
        label.setPadding(dp(4), dp(18), 0, dp(8));
        return label;
    }

    private LinearLayout.LayoutParams weight() {
        LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(0,
                ViewGroup.LayoutParams.WRAP_CONTENT, 1f);
        lp.setMargins(dp(5), 0, dp(5), 0);
        return lp;
    }

    /** A pill that shows whether an option is the current one. */
    private View choice(String label, boolean selected, Runnable action) {
        TextView b = Util.text(host, label, 15, selected ? theme.onAccent() : theme.ink());
        b.setTypeface(null, android.graphics.Typeface.BOLD);
        b.setGravity(Gravity.CENTER);
        b.setBackground(selected ? Util.rounded(theme.accent(), dp(18))
                : Util.roundedStroke(theme.surface(), dp(18), theme.line(), dp(2)));
        b.setElevation(selected ? dp(3) : dp(1));
        b.setPadding(dp(14), dp(14), dp(14), dp(14));
        b.setClickable(true);
        b.setOnClickListener(v -> {
            host.tapFeedback(v);
            action.run();
        });
        return b;
    }

    private View infoCard() {
        LinearLayout card = new LinearLayout(host);
        card.setOrientation(LinearLayout.VERTICAL);
        card.setPadding(dp(16), dp(16), dp(16), dp(16));
        card.setBackground(Util.roundedStroke(theme.surface(), dp(20), theme.line(), dp(2)));

        TextView title = Util.text(host, Content.CURRICULUM_NAME, 15, theme.ink());
        title.setTypeface(null, android.graphics.Typeface.BOLD);
        title.setGravity(Gravity.START);
        card.addView(title);

        int units = 0, items = 0;
        for (Content.Tunjang t : Content.TUNJANG) {
            units += t.units.size();
            for (Content.Unit u : t.units) {
                items += u.items.size();
            }
        }

        card.addView(line(t("Umur", "Ages") + ": " + Content.AGES));
        card.addView(line(Content.TUNJANG.size() + " " + t("tunjang", "tunjang")
                + " \u00B7 " + units + " " + t("unit", "units")
                + " \u00B7 " + items + " " + t("item", "items")));
        card.addView(line(Content.THEMES.size() + " " + t("tema mingguan", "weekly themes")));
        card.addView(line(Content.CURRICULUM_NOTE));
        return card;
    }

    private View line(String text) {
        TextView t = Util.text(host, text, 12, theme.inkSoft());
        t.setGravity(Gravity.START);
        t.setPadding(0, dp(4), 0, 0);
        return t;
    }
}
