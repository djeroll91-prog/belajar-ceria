package com.ceria.belajar;

import android.graphics.Color;
import android.graphics.Typeface;
import android.view.Gravity;
import android.view.View;
import android.view.ViewGroup;
import android.widget.FrameLayout;
import android.widget.LinearLayout;
import android.widget.TextView;

import java.util.ArrayList;
import java.util.List;

/**
 * Flash-card learner used for letters, numbers and words. The child sees one big
 * card, hears it spoken, and can tap the microphone to try saying it.
 */
public class LearnScreen extends BaseScreen {

    public enum Mode { LETTER, NUMBER, WORD }

    private final List<Content.Item> items;
    private final Mode mode;
    private int index;
    private String titleEn;
    private String titleMs;

    private TextView emojiView;
    private TextView glyphView;
    private TextView nameView;
    private TextView translateView;
    private TextView counterView;
    private TextView hintView;
    private FrameLayout card;

    public LearnScreen(MainActivity host, List<Content.Item> items, Mode mode) {
        super(host);
        this.items = new ArrayList<>(items);
        this.mode = mode;
    }

    /** Optional custom screen title, used for the extra topic categories. */
    public LearnScreen withTitle(String en, String ms) {
        this.titleEn = en;
        this.titleMs = ms;
        return this;
    }

    @Override
    protected String titleText() {
        if (titleEn != null) {
            return t(titleEn, titleMs);
        }
        switch (mode) {
            case LETTER:
                return t("Letters ABC \uD83D\uDD24", "Huruf ABC \uD83D\uDD24");
            case NUMBER:
                return t("Numbers 123 \uD83D\uDD22", "Nombor 123 \uD83D\uDD22");
            default:
                return t("Words \uD83D\uDCD6", "Perkataan \uD83D\uDCD6");
        }
    }

    @Override
    protected View buildBody() {
        LinearLayout column = vertical();
        column.setGravity(Gravity.CENTER_HORIZONTAL);

        counterView = Util.text(host, "", 16, 0xFF4A148C);
        column.addView(counterView, matchWrap());

        card = new FrameLayout(host);
        card.setBackground(Util.roundedStroke(Color.WHITE, dp(28), 0xFFFFD600, dp(4)));
        card.setElevation(dp(6));
        LinearLayout cardContent = new LinearLayout(host);
        cardContent.setOrientation(LinearLayout.VERTICAL);
        cardContent.setGravity(Gravity.CENTER);
        cardContent.setPadding(dp(12), dp(18), dp(12), dp(18));
        card.addView(cardContent, new FrameLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT));

        glyphView = Util.text(host, "", 92, 0xFF311B92);
        glyphView.setTypeface(Typeface.DEFAULT_BOLD);
        cardContent.addView(glyphView);

        emojiView = Util.text(host, "", mode == Mode.LETTER ? 54 : 72, Color.BLACK);
        cardContent.addView(emojiView);

        nameView = Util.text(host, "", 34, 0xFFD81B60);
        nameView.setTypeface(Typeface.DEFAULT_BOLD);
        cardContent.addView(nameView);

        translateView = Util.text(host, "", 20, 0xFF546E7A);
        cardContent.addView(translateView);

        LinearLayout.LayoutParams cardLp = new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, 0, 1f);
        cardLp.setMargins(0, dp(10), 0, dp(10));
        column.addView(card, cardLp);

        hintView = Util.text(host, startingHint(), 15, 0xFF6A1B9A);
        hintView.setPadding(0, 0, 0, dp(8));
        column.addView(hintView, matchWrap());

        column.addView(buildControls(), matchWrap());

        render();
        return column;
    }

    private String startingHint() {
        return t("Tap the card to hear it, or press \uD83C\uDFA4 to say it!",
                "Tekan kad untuk dengar, atau tekan \uD83C\uDFA4 untuk sebut!");
    }

    private View buildControls() {
        LinearLayout controls = new LinearLayout(host);
        controls.setOrientation(LinearLayout.HORIZONTAL);
        controls.setGravity(Gravity.CENTER);

        controls.addView(roundButton("\u2B05\uFE0F", 0xFF5C6BC0, this::previous), lp(dp(58), dp(58)));
        controls.addView(pad());
        controls.addView(roundButton("\uD83D\uDD0A", 0xFFFF9100, this::speakCurrent), lp(dp(66), dp(66)));
        controls.addView(pad());
        controls.addView(roundButton("\uD83D\uDC22", 0xFF26A69A, this::speakSlow), lp(dp(58), dp(58)));
        controls.addView(pad());
        controls.addView(roundButton("\uD83C\uDFA4", 0xFFFF4081, this::startListening), lp(dp(66), dp(66)));
        controls.addView(pad());
        controls.addView(roundButton("\u27A1\uFE0F", 0xFF5C6BC0, this::next), lp(dp(58), dp(58)));

        return controls;
    }

    private View pad() {
        View v = new View(host);
        v.setLayoutParams(new LinearLayout.LayoutParams(dp(8), 1));
        return v;
    }

    private TextView roundButton(String emoji, int color, Runnable action) {
        TextView b = Util.text(host, emoji, 26, Color.WHITE);
        b.setBackground(Util.rounded(color, dp(40)));
        b.setElevation(dp(4));
        b.setOnClickListener(v -> {
            v.animate().scaleX(0.88f).scaleY(0.88f).setDuration(80)
                    .withEndAction(() -> {
                        v.animate().scaleX(1f).scaleY(1f).setDuration(80).start();
                        action.run();
                    }).start();
        });
        return b;
    }

    // ------------------------------------------------------------------

    private Content.Item current() {
        return items.get(index);
    }

    /** The exact text we want the child to say out loud. */
    private String targetUtterance() {
        Content.Item item = current();
        if (Content.isColorItem(item) || Content.isScriptItem(item)) {
            return item.name(lang);
        }
        switch (mode) {
            case LETTER:
                return Content.letterGlyph(item);
            case NUMBER:
                // Speech recognition handles "satu" far better than the digit "1".
                return item.name(lang);
            default:
                return item.word != null ? item.word : item.name(lang);
        }
    }

    private String displayName() {
        Content.Item item = current();
        if (mode == Mode.WORD && item.word != null) {
            return item.word;
        }
        return item.name(lang);
    }

    private void render() {
        Content.Item item = current();
        LinearLayout.LayoutParams emojiLp = new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT);
        emojiLp.gravity = Gravity.CENTER_HORIZONTAL;

        if (Content.isColorItem(item) || Content.isScriptItem(item)) {
            glyphView.setVisibility(View.GONE);
            nameView.setTextSize(38);
            if (Content.isColorItem(item)) {
                int swatch = Content.colorOf(item);
                emojiView.setText("");
                emojiView.setBackground(Util.roundedStroke(swatch, dp(30), 0x66000000, dp(2)));
                emojiLp.width = dp(150);
                emojiLp.height = dp(150);
                emojiView.setLayoutParams(emojiLp);
                emojiView.setTextSize(1);
            } else {
                // Jawi / Arabic glyph shown large, right-to-left.
                emojiView.setBackground(null);
                emojiView.setText(item.symbol);
                emojiView.setTextSize(110);
                emojiView.setLayoutParams(emojiLp);
            }
            nameView.setText(displayName());
            translateView.setText(displayName());
        } else {
            emojiView.setBackground(null);
            emojiView.setLayoutParams(emojiLp);
            nameView.setTextSize(34);
            switch (mode) {
                case LETTER:
                    glyphView.setVisibility(View.VISIBLE);
                    glyphView.setText(Content.letterGlyph(item));
                    emojiView.setText(Content.letterEmoji(item));
                    break;
                case NUMBER:
                    glyphView.setVisibility(View.VISIBLE);
                    glyphView.setText(item.symbol);
                    emojiView.setText(repeatEmoji(item, item.symbol));
                    break;
                default:
                    glyphView.setVisibility(View.GONE);
                    if (item.word != null) {
                        emojiView.setText(item.word);
                        emojiView.setTextSize(64);
                        emojiView.setTypeface(Typeface.DEFAULT_BOLD);
                        emojiView.setTextColor(0xFF311B92);
                        translateView.setText("\uD83D\uDCF8 " + item.symbol);
                    } else {
                        emojiView.setText(item.symbol);
                        emojiView.setTextSize(72);
                        emojiView.setTypeface(Typeface.DEFAULT);
                        emojiView.setTextColor(Color.BLACK);
                        translateView.setText(item.name(
                                lang == Content.Lang.MS ? Content.Lang.EN : Content.Lang.MS));
                    }
                    break;
            }
            nameView.setText(displayName());
            if (item.word == null) {
                translateView.setText(item.name(lang == Content.Lang.MS ? Content.Lang.EN : Content.Lang.MS));
            }
        }
        counterView.setText((index + 1) + " / " + items.size() + "   " + starRow());
    }

    /** For numbers, show the count as small repeated emojis so children can count. */
    private String repeatEmoji(Content.Item item, String ignored) {
        int n;
        try {
            n = Integer.parseInt(item.symbol);
        } catch (NumberFormatException e) {
            return "";
        }
        String[] pool = {"\uD83C\uDF4E", "\uD83C\uDF4A", "\uD83C\uDF53", "\uD83C\uDF4C"};
        String e = pool[Math.min(n, pool.length - 1)];
        if (n > 10) {
            return e + e + e + e + e + e + " +" + (n - 6);
        }
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < n; i++) {
            sb.append(e).append(' ');
        }
        return sb.toString();
    }

    private String starRow() {
        int stars = host.progress().stars();
        int shown = Math.min(stars, 5);
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < shown; i++) {
            sb.append("\u2B50");
        }
        return sb.toString();
    }

    private void next() {
        index = (index + 1) % items.size();
        render();
        speakCurrent();
    }

    private void previous() {
        index = (index - 1 + items.size()) % items.size();
        render();
        speakCurrent();
    }

    private void speakCurrent() {
        host.voice().speak(lang, displayName());
    }

    private void speakSlow() {
        host.voice().speakSlowly(lang, displayName());
    }

    private void startListening() {
        host.speech().listen(new SpeechHelper.SpeechListener() {
            @Override
            public void onSpeechReady() {
            }

            @Override
            public void onSpeechResult(ArrayList<String> matches) {
                String best = matches.isEmpty() ? "" : matches.get(0);
                boolean ok = false;
                for (String m : matches) {
                    if (SpeechHelper.matches(targetUtterance(), m)
                            || SpeechHelper.matches(displayName(), m)) {
                        ok = true;
                        break;
                    }
                }
                // A script letter name must be said as one sound. Spelling it out
                // ("K-H-A") is not the letter's name and must not be praised.
                if (ok && Content.isScriptItem(current())) {
                    boolean named = false;
                    for (String m : matches) {
                        if (SpeechHelper.matchesStrict(displayName(), m)) {
                            named = true;
                            break;
                        }
                    }
                    ok = named;
                }
                if (ok) {
                    host.progress().addPoints(10);
                    host.progress().addCorrect();
                    host.progress().addStar();
                    host.progress().addSpoken();
                    host.progress().onCorrectAnswer();
                    host.progress().earnedBadges();
                    host.updateHeader();
                    host.celebrate(t("Great job! ", "Bagus sekali! ") + displayName()
                            + " \u2B50 +10");
                    hintView.setText(t("Correct! ", "Betul! ") + best);
                    host.pop(emojiView);
                    next();
                } else {
                    host.progress().onWrongAnswer();
                    host.progress().resetStreak();
                    hintView.setText(t("Almost! I heard: ", "Hampir betul! Saya dengar: ")
                            + (best.isEmpty() ? "..." : best) + " \uD83D\uDC42");
                    host.sfx().wrong();
                    host.mouthSay(t("Try again, you can do it!", "Cuba lagi, kamu boleh!"));
                }
            }

            @Override
            public void onSpeechError(String message) {
                hintView.setText("\uD83D\uDE14 " + message);
            }
        }, lang);
    }
}
