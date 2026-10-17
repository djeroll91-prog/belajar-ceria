package com.ceria.belajar;

import android.graphics.drawable.GradientDrawable;
import android.view.Gravity;
import android.view.View;
import android.view.ViewGroup;
import android.widget.FrameLayout;
import android.widget.LinearLayout;
import android.widget.TextView;

/**
 * Flashcards for one unit.
 *
 * The card is the whole screen so the glyph is as large as the device allows,
 * which is what makes a letter recognisable to a four-year-old. Tapping the card
 * repeats the word; the arrows move on. Every card the child sees is recorded as
 * a correct exposure, so merely reading a unit already builds its mastery record.
 */
public class LearnScreen extends BaseScreen {

    private final Content.Unit unit;
    private int index;
    private boolean auto = true;
    private TextView counter;
    private TextView autoButton;
    private FrameLayout card;

    public LearnScreen(MainActivity host, Content.Unit unit) {
        super(host);
        this.unit = unit;
    }

    @Override
    protected String titleText() {
        return unit.emoji + " " + unit.title(lang);
    }

    @Override
    protected View buildBody() {
        LinearLayout column = vertical();
        if (unit.items.isEmpty()) {
            column.addView(Util.text(host, t("Tiada kandungan", "No content"), 16, theme.inkSoft()));
            return scrollWrap(column);
        }

        card = new FrameLayout(host);
        card.setBackground(Util.roundedStroke(theme.surface(), dp(30), theme.line(), dp(3)));
        card.setElevation(dp(4));
        column.addView(card, new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, 0, 1f));

        counter = Util.text(host, "", 14, theme.inkSoft());
        counter.setPadding(0, dp(10), 0, dp(6));
        column.addView(counter, matchWrap());

        column.addView(controls(), matchWrap());
        show();
        speak();
        return column;
    }

    private View controls() {
        LinearLayout row = new LinearLayout(host);
        row.setOrientation(LinearLayout.HORIZONTAL);
        row.setGravity(Gravity.CENTER);

        row.addView(round("\u2B05", () -> {
            index = (index - 1 + unit.items.size()) % unit.items.size();
            show();
            speak();
        }), square(dp(60)));

        LinearLayout speakBtn = new LinearLayout(host);
        speakBtn.setOrientation(LinearLayout.HORIZONTAL);
        speakBtn.setGravity(Gravity.CENTER);
        speakBtn.setBackground(Util.rounded(theme.accent(), dp(22)));
        speakBtn.setElevation(dp(3));
        speakBtn.setClickable(true);
        speakBtn.addView(Util.text(host, "\uD83D\uDD0A", 24, theme.onAccent()));
        TextView speakLabel = Util.text(host, t("Sebut", "Say"), 16, theme.onAccent());
        speakLabel.setTypeface(null, android.graphics.Typeface.BOLD);
        LinearLayout.LayoutParams labelLp = new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT);
        labelLp.setMargins(dp(8), 0, dp(4), 0);
        speakBtn.addView(speakLabel, labelLp);
        speakBtn.setOnClickListener(v -> {
            host.tapFeedback(v);
            speak();
        });
        LinearLayout.LayoutParams speakLp = new LinearLayout.LayoutParams(0, dp(60), 1f);
        speakLp.setMargins(dp(10), 0, dp(10), 0);
        row.addView(speakBtn, speakLp);

        autoButton = round(auto ? "\u23F8" : "\u25B6\uFE0F", () -> {
            auto = !auto;
            autoButton.setText(auto ? "\u23F8" : "\u25B6\uFE0F");
        });
        row.addView(autoButton, square(dp(60)));

        row.addView(round("\u27A1", () -> {
            index = (index + 1) % unit.items.size();
            show();
            speak();
        }), square(dp(60)));
        return row;
    }

    private LinearLayout.LayoutParams square(int px) {
        LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(px, px);
        lp.setMargins(dp(5), 0, dp(5), 0);
        return lp;
    }

    private TextView round(String glyph, Runnable action) {
        TextView b = Util.text(host, glyph, 22, theme.ink());
        b.setGravity(Gravity.CENTER);
        b.setBackground(Util.roundedStroke(theme.surface(), dp(20), theme.line(), dp(2)));
        b.setElevation(dp(2));
        b.setClickable(true);
        b.setOnClickListener(v -> {
            host.tapFeedback(v);
            action.run();
        });
        return b;
    }

    private void show() {
        Content.Item item = unit.items.get(index);
        card.removeAllViews();

        LinearLayout stack = new LinearLayout(host);
        stack.setOrientation(LinearLayout.VERTICAL);
        stack.setGravity(Gravity.CENTER);

        if (Content.isColorItem(item)) {
            View swatch = new View(host);
            GradientDrawable d = new GradientDrawable();
            d.setShape(GradientDrawable.OVAL);
            d.setColor(Content.colorOf(item));
            d.setStroke(dp(5), 0x33000000);
            swatch.setBackground(d);
            LinearLayout.LayoutParams swLp = new LinearLayout.LayoutParams(dp(170), dp(170));
            swLp.setMargins(0, dp(10), 0, dp(14));
            stack.addView(swatch, swLp);
        } else {
            TextView glyph = Util.text(host, Content.displayOf(item, lang), 96, theme.ink());
            glyph.setTypeface(null, android.graphics.Typeface.BOLD);
            glyph.setPadding(0, dp(10), 0, dp(6));
            stack.addView(glyph);
        }

        TextView name = Util.text(host, item.name(lang), 30, theme.ink());
        name.setTypeface(null, android.graphics.Typeface.BOLD);
        stack.addView(name);

        String spoken = Content.spokenOf(item, unit.kind, lang);
        if (!spoken.equals(item.name(lang))) {
            TextView word = Util.text(host, spoken, 18, theme.inkSoft());
            word.setPadding(0, dp(6), 0, 0);
            stack.addView(word);
        }

        TextView hint = Util.text(host, t("Tekan kad untuk dengar", "Tap the card to hear"),
                13, theme.inkSoft());
        hint.setPadding(0, dp(16), 0, 0);
        stack.addView(hint);

        card.addView(stack, new FrameLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT));
        card.setOnClickListener(v -> speak());

        counter.setText((index + 1) + " / " + unit.items.size());

        // Reading a card counts as meeting the item, so a child who only browses
        // still builds up a picture of what they know.
        host.progress().recordAnswer(unit.id, Progress.keyOf(item), true);
    }

    private void speak() {
        if (unit.items.isEmpty()) {
            return;
        }
        host.voice().speak(lang, spokenName());
    }

    /** The exact text the voice is given for the card on screen. */
    public String spokenName() {
        if (unit.items.isEmpty()) {
            return "";
        }
        return Content.spokenOf(unit.items.get(index), unit.kind, lang);
    }

    /** The item currently on screen. */
    public Content.Item currentItem() {
        return unit.items.isEmpty() ? null : unit.items.get(index);
    }

    /** Advances one card, for tests that walk a whole unit. */
    public void nextForTest() {
        if (!unit.items.isEmpty()) {
            index = (index + 1) % unit.items.size();
        }
    }
}
