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
import java.util.Random;

/**
 * Pronunciation practice. A random target is shown, the child taps the
 * microphone and says it. Correct attempts earn stars and points.
 */
public class SpeakScreen extends BaseScreen {

    private final Random random = new Random();
    private Content.Item target;
    private TextView targetView;
    private TextView translationView;
    private TextView statusView;
    private TextView scoreView;
    private ProgressBar progressBar;

    public SpeakScreen(MainActivity host) {
        super(host);
    }

    @Override
    protected String titleText() {
        return t("Say & Win \uD83C\uDFA4", "Sebut & Menang \uD83C\uDFA4");
    }

    @Override
    protected View buildBody() {
        LinearLayout column = vertical();
        column.setGravity(Gravity.CENTER_HORIZONTAL);

        scoreView = Util.text(host, "", 18, 0xFF4A148C);
        column.addView(scoreView, matchWrap());

        progressBar = new ProgressBar(host, null, android.R.attr.progressBarStyleHorizontal);
        progressBar.setMax(100);
        column.addView(progressBar, new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, dp(14)));

        LinearLayout card = new LinearLayout(host);
        card.setOrientation(LinearLayout.VERTICAL);
        card.setGravity(Gravity.CENTER);
        card.setPadding(dp(16), dp(24), dp(16), dp(24));
        card.setBackground(Util.roundedStroke(Color.WHITE, dp(26), 0xFFFF4081, dp(4)));
        card.setElevation(dp(6));

        targetView = Util.text(host, "", 54, 0xFFAD1457);
        targetView.setTypeface(Typeface.DEFAULT_BOLD);
        card.addView(targetView);

        translationView = Util.text(host, "", 22, 0xFF546E7A);
        card.addView(translationView);

        LinearLayout.LayoutParams cardLp = new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, 0, 1f);
        cardLp.setMargins(0, dp(12), 0, dp(12));
        column.addView(card, cardLp);

        statusView = Util.text(host, t("Tap the big microphone and say the word!",
                "Tekan mikrofon besar dan sebut perkataan itu!"), 17, 0xFF6A1B9A);
        statusView.setPadding(0, 0, 0, dp(10));
        column.addView(statusView, matchWrap());

        TextView mic = Util.text(host, "\uD83C\uDFA4", 60, Color.WHITE);
        mic.setBackground(Util.gradient(0xFFFF4081, 0xFFFF9100, dp(80)));
        mic.setElevation(dp(8));
        mic.setOnClickListener(v -> listen());
        LinearLayout.LayoutParams micLp = new LinearLayout.LayoutParams(dp(130), dp(130));
        micLp.setMargins(0, 0, 0, dp(12));
        column.addView(mic, micLp);

        column.addView(host.buildHeaderButton(t("\uD83D\uDD01 Perkataan baharu",
                "\uD83D\uDD01 Perkataan baharu"), 0xFF7C4DFF, this::newTarget), wrap());

        newTarget();
        return column;
    }

    private void newTarget() {
        List<Content.Item> pool = new ArrayList<>();
        pool.addAll(Content.LETTERS);
        pool.addAll(Content.WORDS);
        target = pool.get(random.nextInt(pool.size()));
        boolean letter = Content.LETTERS.contains(target);
        targetView.setText(letter ? Content.letterGlyph(target) : target.symbol);
        translationView.setText(target.name(lang));
        updateScore();
    }

    private void updateScore() {
        scoreView.setText("\u2B50 " + host.progress().stars() + "   \uD83C\uDFC6 "
                + host.progress().points() + "   \uD83D\uDD25 " + host.progress().streak());
        progressBar.setProgress(host.progress().levelProgress());
    }

    private void listen() {
        boolean letter = Content.LETTERS.contains(target);
        final String expected = letter ? Content.letterGlyph(target) : target.symbol;
        statusView.setText(t("Listening... \uD83D\uDC42", "Sedang mendengar... \uD83D\uDC42"));
        host.speech().listen(new SpeechHelper.SpeechListener() {
            @Override
            public void onSpeechReady() {
            }

            @Override
            public void onSpeechResult(ArrayList<String> matches) {
                boolean ok = false;
                String heard = matches.isEmpty() ? "" : matches.get(0);
                for (String m : matches) {
                    if (SpeechHelper.matches(expected, m)
                            || SpeechHelper.matches(target.name(lang), m)) {
                        ok = true;
                        break;
                    }
                }
                if (ok) {
                    host.progress().addPoints(15);
                    host.progress().addStar();
                    host.progress().addSpoken();
                    host.progress().addCorrect();
                    host.progress().onCorrectAnswer();
                    host.progress().earnedBadges();
                    host.updateHeader();
                    host.celebrate(t("Perfect! ", "Tepat sekali! ") + expected + " \u2B50 +15");
                    statusView.setText("\uD83C\uDF89 " + heard);
                    newTarget();
                } else {
                    host.progress().resetStreak();
                    statusView.setText(t("I heard \"", "Saya dengar \"") + heard
                            + t("\". Try again!", "\". Cuba lagi!"));
                    host.mouthSay(expected);
                }
                updateScore();
            }

            @Override
            public void onSpeechError(String message) {
                statusView.setText("\uD83D\uDE14 " + message);
            }
        }, lang);
    }
}
