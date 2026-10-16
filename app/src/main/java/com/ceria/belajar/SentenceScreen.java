package com.ceria.belajar;

import android.graphics.Color;
import android.graphics.Typeface;
import android.view.Gravity;
import android.view.View;
import android.view.ViewGroup;
import android.widget.LinearLayout;
import android.widget.TextView;

import java.util.ArrayList;

/** Reads simple sentences aloud and lets the child repeat them. */
public class SentenceScreen extends BaseScreen {

    private int index;
    private TextView sentenceView;
    private TextView translationView;
    private TextView hintView;
    private TextView emojiView;

    private final String[] emojis = {"\uD83D\uDC08", "\u26BD", "\u2600\uFE0F", "\uD83D\uDD34",
            "\uD83D\uDCD6", "\uD83D\uDC1F", "\uD83D\uDC69", "\uD83D\uDCDA"};

    public SentenceScreen(MainActivity host) {
        super(host);
    }

    @Override
    protected String titleText() {
        return t("Sentences \uD83D\uDCDD", "Ayat \uD83D\uDCDD");
    }

    @Override
    protected View buildBody() {
        LinearLayout column = vertical();
        column.setGravity(Gravity.CENTER_HORIZONTAL);

        LinearLayout card = new LinearLayout(host);
        card.setOrientation(LinearLayout.VERTICAL);
        card.setGravity(Gravity.CENTER);
        card.setPadding(dp(18), dp(24), dp(18), dp(24));
        card.setBackground(Util.roundedStroke(Color.WHITE, dp(26), 0xFF40C4FF, dp(4)));
        card.setElevation(dp(6));

        emojiView = Util.text(host, "", 56, Color.BLACK);
        card.addView(emojiView);

        sentenceView = Util.text(host, "", 32, 0xFF00695C);
        sentenceView.setTypeface(Typeface.DEFAULT_BOLD);
        sentenceView.setPadding(0, dp(10), 0, dp(10));
        card.addView(sentenceView);

        translationView = Util.text(host, "", 20, 0xFF546E7A);
        card.addView(translationView);

        LinearLayout.LayoutParams cardLp = new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, 0, 1f);
        cardLp.setMargins(0, dp(10), 0, dp(10));
        column.addView(card, cardLp);

        hintView = Util.text(host, t("Listen and repeat after Bobo!",
                "Dengar dan ulang selepas Bobo!"), 15, 0xFF6A1B9A);
        hintView.setPadding(0, 0, 0, dp(8));
        column.addView(hintView, matchWrap());

        LinearLayout controls = horizontal();
        controls.addView(button("\u2B05\uFE0F", 0xFF5C6BC0, this::previous));
        controls.addView(gap());
        controls.addView(button("\uD83D\uDD0A", 0xFFFF9100, () ->
                host.speech().speak(Content.SENTENCES.get(index).text(lang), lang)));
        controls.addView(gap());
        controls.addView(button("\uD83D\uDC22", 0xFF26A69A, () ->
                host.speech().speakSlowly(Content.SENTENCES.get(index).text(lang), lang)));
        controls.addView(gap());
        controls.addView(button("\uD83C\uDFA4", 0xFFFF4081, this::listen));
        controls.addView(gap());
        controls.addView(button("\u27A1\uFE0F", 0xFF5C6BC0, this::next));
        column.addView(controls, matchWrap());

        render();
        return column;
    }

    private View gap() {
        View v = new View(host);
        v.setLayoutParams(new LinearLayout.LayoutParams(dp(8), 1));
        return v;
    }

    private TextView button(String emoji, int color, Runnable action) {
        TextView b = Util.text(host, emoji, 26, Color.WHITE);
        b.setBackground(Util.rounded(color, dp(40)));
        b.setElevation(dp(4));
        b.setPadding(0, dp(12), 0, dp(12));
        LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(dp(62), dp(62));
        b.setLayoutParams(lp);
        b.setOnClickListener(v -> action.run());
        return b;
    }

    private void render() {
        Content.Sentence s = Content.SENTENCES.get(index);
        sentenceView.setText(s.text(lang));
        translationView.setText(s.text(lang == Content.Lang.MS ? Content.Lang.EN : Content.Lang.MS));
        emojiView.setText(emojis[index % emojis.length]);
    }

    private void next() {
        index = (index + 1) % Content.SENTENCES.size();
        render();
        host.speech().speak(Content.SENTENCES.get(index).text(lang), lang);
    }

    private void previous() {
        index = (index - 1 + Content.SENTENCES.size()) % Content.SENTENCES.size();
        render();
        host.speech().speak(Content.SENTENCES.get(index).text(lang), lang);
    }

    private void listen() {
        final String target = Content.SENTENCES.get(index).text(lang);
        host.speech().listen(new SpeechHelper.SpeechListener() {
            @Override
            public void onSpeechReady() {
            }

            @Override
            public void onSpeechResult(ArrayList<String> matches) {
                int good = 0;
                int total = target.split("\\s+").length;
                for (String m : matches) {
                    for (String word : target.split("\\s+")) {
                        if (SpeechHelper.matches(word, m)) {
                            good = total;
                            break;
                        }
                    }
                }
                if (good > 0) {
                    host.progress().addPoints(20);
                    host.progress().addStar();
                    host.progress().addSpoken();
                    host.progress().addCorrect();
                    host.progress().onCorrectAnswer();
                    host.progress().earnedBadges();
                    host.updateHeader();
                    host.celebrate(t("Excellent reading! \u2B50 +20",
                            "Bacaan hebat! \u2B50 +20"));
                    hintView.setText("\uD83C\uDF89 " + (matches.isEmpty() ? "" : matches.get(0)));
                } else {
                    host.progress().resetStreak();
                    hintView.setText(t("Let's read it together again!",
                            "Jom baca sekali lagi!"));
                    host.mouthSay(target);
                }
            }

            @Override
            public void onSpeechError(String message) {
                hintView.setText("\uD83D\uDE14 " + message);
            }
        }, lang);
    }
}
