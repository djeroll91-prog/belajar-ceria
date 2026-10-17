package com.ceria.belajar;

import android.view.Gravity;
import android.view.View;
import android.view.ViewGroup;
import android.widget.LinearLayout;
import android.widget.TextView;

import java.util.List;

/**
 * The review screen: what to practise, and why.
 *
 * This is the part of the app that answers "what should my child do now?".
 * Items the child gets wrong most often are listed first, and the weak units are
 * offered as whole-subject practice. Nothing here is random.
 */
public class ReviewScreen extends BaseScreen {

    public ReviewScreen(MainActivity host) {
        super(host);
    }

    @Override
    protected String titleText() {
        return t("Smart Review \uD83E\uDDE0", "Ulang Kaji Pintar \uD83E\uDDE0");
    }

    @Override
    protected View buildBody() {
        LinearLayout column = vertical();

        TextView sub = Util.text(host, t("Item yang selalu salah muncul lebih kerap",
                "Items you get wrong come back more often"), 13, theme.inkSoft());
        sub.setGravity(Gravity.START);
        sub.setPadding(dp(4), 0, 0, dp(12));
        column.addView(sub, matchWrap());

        List<Review.Candidate> queue = Review.queue(host.progress(), 12);
        List<Review.WeakUnit> weak = Review.weakUnits(host.progress(), 5);

        if (queue.isEmpty() && weak.isEmpty()) {
            column.addView(emptyCard(), matchWrap());
            return scrollWrap(column);
        }

        if (!weak.isEmpty()) {
            column.addView(sectionTitle(t("Subjek yang lemah", "Weak subjects"), weak.size()),
                    matchWrap());
            for (Review.WeakUnit w : weak) {
                column.addView(weakRow(w), matchWrap());
            }
        }

        if (!queue.isEmpty()) {
            column.addView(sectionTitle(t("Item untuk diulang", "Items to review"), queue.size()),
                    matchWrap());

            TextView start = Util.text(host, "\u25B6\uFE0F " + t("Mula ulang kaji", "Start review"),
                    18, theme.onAccent());
            start.setTypeface(null, android.graphics.Typeface.BOLD);
            start.setGravity(Gravity.CENTER);
            start.setBackground(Util.rounded(theme.accent(), dp(20)));
            start.setElevation(dp(3));
            start.setPadding(0, dp(16), 0, dp(16));
            start.setOnClickListener(v -> {
                host.tapFeedback(v);
                v.postDelayed(() -> host.open(new QuizScreen(host, queue)), 90);
            });
            LinearLayout.LayoutParams startLp = new LinearLayout.LayoutParams(
                    ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
            startLp.setMargins(dp(4), 0, dp(4), dp(12));
            column.addView(start, startLp);

            for (Review.Candidate c : queue) {
                column.addView(itemRow(c), matchWrap());
            }
        }
        return scrollWrap(column);
    }

    private View emptyCard() {
        LinearLayout card = new LinearLayout(host);
        card.setOrientation(LinearLayout.VERTICAL);
        card.setGravity(Gravity.CENTER);
        card.setPadding(dp(20), dp(30), dp(20), dp(30));
        card.setBackground(Util.roundedStroke(theme.surface(), dp(26), theme.line(), dp(3)));
        card.setElevation(dp(3));

        card.addView(Util.text(host, "\uD83C\uDF31", 56, theme.ink()));

        TextView head = Util.text(host, t("Belum ada apa nak diulang",
                "Nothing to review yet"), 19, theme.ink());
        head.setTypeface(null, android.graphics.Typeface.BOLD);
        head.setPadding(0, dp(10), 0, dp(4));
        card.addView(head);

        card.addView(Util.text(host, t("Cuba belajar dan kuiz dahulu",
                "Learn and try a quiz first"), 14, theme.inkSoft()));

        TextView go = Util.text(host, t("Mula belajar", "Start learning"), 17, theme.onAccent());
        go.setTypeface(null, android.graphics.Typeface.BOLD);
        go.setGravity(Gravity.CENTER);
        go.setBackground(Util.rounded(theme.accent(), dp(20)));
        go.setPadding(dp(24), dp(14), dp(24), dp(14));
        go.setOnClickListener(v -> host.open(new TunjangScreen(host, Content.TUNJANG.get(0))));
        LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT);
        lp.setMargins(0, dp(18), 0, 0);
        card.addView(go, lp);
        return card;
    }

    private View weakRow(Review.WeakUnit w) {
        LinearLayout row = new LinearLayout(host);
        row.setOrientation(LinearLayout.HORIZONTAL);
        row.setGravity(Gravity.CENTER_VERTICAL);
        row.setPadding(dp(14), dp(12), dp(14), dp(12));
        row.setBackground(Util.roundedStroke(theme.surface(), dp(18), theme.line(), dp(2)));
        row.setElevation(dp(2));

        View dot = new View(host);
        dot.setBackground(Util.rounded(w.accuracy < 50 ? theme.badFor() : Theme.GOLD, dp(7)));
        LinearLayout.LayoutParams dotLp = new LinearLayout.LayoutParams(dp(14), dp(14));
        dotLp.setMargins(0, 0, dp(12), 0);
        row.addView(dot, dotLp);

        LinearLayout texts = new LinearLayout(host);
        texts.setOrientation(LinearLayout.VERTICAL);
        TextView name = Util.text(host, w.unit.title(lang), 16, theme.ink());
        name.setTypeface(null, android.graphics.Typeface.BOLD);
        name.setGravity(Gravity.START);
        texts.addView(name);
        TextView why = Util.text(host, w.accuracy + "% \u00B7 " + w.wrong + " "
                + t("salah", "wrong"), 12, theme.inkSoft());
        why.setGravity(Gravity.START);
        texts.addView(why);
        row.addView(texts, new LinearLayout.LayoutParams(0,
                ViewGroup.LayoutParams.WRAP_CONTENT, 1f));

        TextView practise = Util.text(host, t("Latih", "Practise"), 14, theme.onAccent());
        practise.setTypeface(null, android.graphics.Typeface.BOLD);
        practise.setGravity(Gravity.CENTER);
        practise.setBackground(Util.rounded(theme.accent(), dp(16)));
        practise.setPadding(dp(16), dp(10), dp(16), dp(10));
        practise.setOnClickListener(v -> {
            host.tapFeedback(v);
            v.postDelayed(() -> host.open(new QuizScreen(host, w.unit)), 90);
        });
        row.addView(practise);

        LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
        lp.setMargins(0, dp(4), 0, dp(4));
        row.setLayoutParams(lp);
        return row;
    }

    private View itemRow(Review.Candidate c) {
        LinearLayout row = new LinearLayout(host);
        row.setOrientation(LinearLayout.HORIZONTAL);
        row.setGravity(Gravity.CENTER_VERTICAL);
        row.setPadding(dp(14), dp(10), dp(14), dp(10));
        row.setBackground(Util.roundedStroke(theme.surface(), dp(18), theme.line(), dp(2)));

        View dot = new View(host);
        int acc = c.accuracy();
        dot.setBackground(Util.rounded(acc < 50 ? theme.badFor() : acc < 80 ? Theme.GOLD
                : theme.goodFor(), dp(7)));
        LinearLayout.LayoutParams dotLp = new LinearLayout.LayoutParams(dp(14), dp(14));
        dotLp.setMargins(0, 0, dp(12), 0);
        row.addView(dot, dotLp);

        LinearLayout texts = new LinearLayout(host);
        texts.setOrientation(LinearLayout.VERTICAL);
        TextView name = Util.text(host, c.item.name(lang), 16, theme.ink());
        name.setTypeface(null, android.graphics.Typeface.BOLD);
        name.setGravity(Gravity.START);
        texts.addView(name);
        TextView why = Util.text(host, acc + "% \u00B7 " + t("kotak", "box") + " "
                + c.box + "/" + Progress.MAX_BOX, 12, theme.inkSoft());
        why.setGravity(Gravity.START);
        texts.addView(why);
        row.addView(texts, new LinearLayout.LayoutParams(0,
                ViewGroup.LayoutParams.WRAP_CONTENT, 1f));

        TextView hear = Util.text(host, "\uD83D\uDD0A", 18, theme.ink());
        hear.setGravity(Gravity.CENTER);
        hear.setBackground(Util.roundedStroke(theme.surface2(), dp(16), theme.line(), dp(2)));
        hear.setPadding(dp(12), dp(8), dp(12), dp(8));
        hear.setOnClickListener(v -> {
            host.sfx().tap();
            host.voice().speak(lang, Content.spokenOf(c.item, c.unit.kind, lang));
        });
        row.addView(hear);

        LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
        lp.setMargins(0, dp(4), 0, dp(4));
        row.setLayoutParams(lp);
        return row;
    }

    private View sectionTitle(String text, int count) {
        LinearLayout row = new LinearLayout(host);
        row.setOrientation(LinearLayout.HORIZONTAL);
        row.setGravity(Gravity.CENTER_VERTICAL);
        row.setPadding(dp(4), dp(18), 0, dp(8));

        TextView label = Util.text(host, text, 17, theme.ink());
        label.setTypeface(null, android.graphics.Typeface.BOLD);
        row.addView(label);

        TextView badge = Util.text(host, String.valueOf(count), 12, theme.inkSoft());
        badge.setBackground(Util.rounded(theme.surface2(), dp(10)));
        badge.setPadding(dp(8), dp(3), dp(8), dp(3));
        LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT);
        lp.setMargins(dp(8), 0, 0, 0);
        row.addView(badge, lp);
        return row;
    }
}
