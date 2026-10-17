package com.ceria.belajar;

import android.view.Gravity;
import android.view.View;
import android.view.ViewGroup;
import android.widget.GridLayout;
import android.widget.LinearLayout;
import android.widget.TextView;

/**
 * The games hub. Each game names the skill it practises, so a parent can pick
 * deliberately rather than guessing from an icon.
 */
public class GamesScreen extends BaseScreen {

    public GamesScreen(MainActivity host) {
        super(host);
    }

    @Override
    protected String titleText() {
        return t("Games \uD83C\uDFAE", "Permainan \uD83C\uDFAE");
    }

    @Override
    protected View buildBody() {
        LinearLayout column = vertical();

        TextView sub = Util.text(host, t("Latihan untuk merangsang otak",
                "Brain-training practice"), 13, theme.inkSoft());
        sub.setGravity(Gravity.START);
        sub.setPadding(dp(4), 0, 0, dp(12));
        column.addView(sub, matchWrap());

        GridLayout grid = new GridLayout(host);
        grid.setColumnCount(host.getResources().getConfiguration().screenWidthDp >= 600 ? 4 : 2);
        column.addView(grid, matchWrap());

        add(grid, "\uD83E\uDDE0", t("Ulang kaji", "Review"),
                t("Item yang lemah", "Weak items"), theme.accent(),
                () -> host.open(new ReviewScreen(host)));
        add(grid, "\uD83D\uDD0A", t("Dengar & Pilih", "Listen & Choose"),
                t("Latih telinga", "Ear training"), 0xFF8FD3F4,
                () -> host.open(new QuizScreen(host, unitById("bm-huruf"))));
        add(grid, "\uD83C\uDFAF", t("Kuiz Cepat", "Quick Quiz"),
                t("Nombor 1-10", "Numbers 1-10"), 0xFF7FB4D9,
                () -> host.open(new QuizScreen(host, unitById("ma-nombor"))));
        add(grid, "\uD83E\uDDE9", t("Bina Perkataan", "Build a Word"),
                t("Suku kata", "Syllables"), 0xFF7ED957,
                () -> host.open(new SpellGame(host, unitById("bm-suku-kata"))));
        add(grid, "\uD83D\uDD0D", t("Cari Pasangan", "Matching Pairs"),
                t("Ingatan", "Memory"), Theme.GOLD,
                () -> host.open(new MemoryGame(host, unitById("sa-buah"))));
        add(grid, "\uD83E\uDEE7", t("Letup Gelembung", "Bubble Pop"),
                t("Kenal huruf", "Letter spotting"), 0xFF5FA8D3,
                () -> host.open(new BubblePopGame(host)));
        add(grid, "\uD83E\uDDFA", t("Bakul Susun", "Sorting Baskets"),
                t("Haiwan atau tumbuhan", "Animal or plant"), 0xFF66BB6A,
                () -> host.open(new SortGame(host)));
        add(grid, "\uD83E\uDDE9", t("Apa Seterusnya", "What Comes Next"),
                t("Corak", "Patterns"), 0xFFB48FC9,
                () -> host.open(new PatternGame(host)));
        add(grid, "\uD83D\uDC22", t("Kira Objek", "Count"),
                t("Nombor dan kiraan", "Numbers and counting"), 0xFF26C6DA,
                () -> host.open(new CountGame(host)));
        add(grid, "\uD83D\uDD0D", t("Cari Yang Berbeza", "Odd One Out"),
                t("Perhatian", "Attention"), 0xFFF09B5B,
                () -> host.open(new OddOneOutGame(host)));
        add(grid, "\uD83C\uDFB9", t("Xylophone", "Xylophone"),
                t("Bunyi dan irama", "Sound and rhythm"), 0xFF8FBF7F,
                () -> host.open(new XylophoneScreen(host)));
        add(grid, "\uD83D\uDC15", t("Suara Haiwan", "Animal Sounds"),
                t("Dengar dan teka", "Listen and guess"), 0xFFF2B457,
                () -> host.open(new AnimalSoundScreen(host)));
        return scrollWrap(column);
    }

    private Content.Unit unitById(String id) {
        return Content.unitById(id);
    }

    private void add(GridLayout grid, String emoji, String label, String sub, int color,
                     Runnable action) {
        LinearLayout tile = new LinearLayout(host);
        tile.setOrientation(LinearLayout.VERTICAL);
        tile.setGravity(Gravity.CENTER);
        tile.setPadding(dp(10), dp(14), dp(10), dp(14));
        tile.setBackground(Util.roundedStroke(theme.wash(color, 0.14f), dp(22), color, dp(3)));
        tile.setElevation(dp(3));
        tile.setClickable(true);

        TextView icon = Util.text(host, emoji, 30, theme.ink());
        icon.setGravity(Gravity.CENTER);
        tile.addView(icon);

        TextView name = Util.text(host, label, 14, theme.ink());
        name.setTypeface(null, android.graphics.Typeface.BOLD);
        name.setGravity(Gravity.CENTER);
        tile.addView(name);

        TextView meta = Util.text(host, sub, 12, theme.inkSoft());
        meta.setGravity(Gravity.CENTER);
        tile.addView(meta);

        tile.setOnClickListener(v -> {
            host.tapFeedback(v);
            v.postDelayed(action, 90);
        });

        GridLayout.LayoutParams lp = new GridLayout.LayoutParams();
        lp.width = 0;
        lp.height = dp(128);
        lp.columnSpec = GridLayout.spec(GridLayout.UNDEFINED, 1f);
        lp.setMargins(dp(5), dp(5), dp(5), dp(5));
        grid.addView(tile, lp);
    }
}
