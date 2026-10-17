package com.ceria.belajar;

import android.graphics.Typeface;
import android.view.Gravity;
import android.view.View;
import android.widget.LinearLayout;
import android.widget.TextView;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;

/**
 * Say the Jawi letter name out loud. This is the one place where the loose
 * matcher is not good enough: a child who spells the name ("K-H-A") instead of
 * saying it ("kha") has learned the wrong thing, and the game must not praise
 * that. {@link SpeechHelper#matchesStrict} is used for exactly this reason.
 *
 * The robot says the name first, so the child hears the target before trying.
 */
public class JawiSpeakGame extends BaseScreen {

    private static final int ROUNDS = 8;

    private final Random random = new Random();
    private int round;
    private int score;
    private Content.Item target;
    private TextView glyphView;
    private TextView promptView;
    private TextView heardView;
    private TextView scoreView;
    private TextView hintView;
    private LinearLayout actions;
    private boolean listening;
    private boolean finished;

    public JawiSpeakGame(MainActivity host) {
        super(host);
    }

    @Override
    protected String titleText() {
        return t("Say the Jawi Letter \uD83D\uDDE3\uFE0F", "Sebut Huruf Jawi \uD83D\uDDE3\uFE0F");
    }

    @Override
    protected View buildBody() {
        LinearLayout column = vertical();
        column.setGravity(Gravity.CENTER_HORIZONTAL);

        scoreView = Util.text(host, "", 17, 0xFF4A148C);
        column.addView(scoreView, matchWrap());

        promptView = Util.text(host, t("Say this letter's name", "Sebut nama huruf ini"),
                16, 0xFF00695C);
        promptView.setPadding(0, dp(4), 0, dp(6));
        column.addView(promptView, matchWrap());

        glyphView = Util.text(host, "", 96, 0xFF4A148C);
        glyphView.setTypeface(Typeface.DEFAULT_BOLD);
        glyphView.setPadding(dp(10), dp(18), dp(10), dp(18));
        glyphView.setBackground(Util.roundedStroke(0xFFFFFFFF, dp(24), 0xFFB39DDB, dp(3)));
        glyphView.setElevation(dp(5));
        column.addView(glyphView, matchWrap());

        heardView = Util.text(host, "", 16, 0xFF4A3F5C);
        heardView.setPadding(0, dp(12), 0, dp(6));
        column.addView(heardView, matchWrap());

        hintView = Util.text(host, "", 15, 0xFFAD1457);
        hintView.setPadding(0, 0, 0, dp(8));
        column.addView(hintView, matchWrap());

        actions = new LinearLayout(host);
        actions.setOrientation(LinearLayout.HORIZONTAL);
        actions.setGravity(Gravity.CENTER);
        actions.addView(bigButton("\uD83D\uDD0A", t("Hear it", "Dengar"),
                0xFFFF9100, this::speakTarget), wrap());
        actions.addView(bigButton("\uD83C\uDFA4", t("Say it", "Sebut"),
                0xFF66BB6A, this::listen), wrap());
        column.addView(actions, wrap());

        startGame();
        return column;
    }

    private void startGame() {
        round = 0;
        score = 0;
        finished = false;
        host.progress().registerGamePlayed();
        nextRound();
    }

    private void nextRound() {
        if (round >= ROUNDS) {
            finish();
            return;
        }
        round++;
        scoreView.setText(t("Round ", "Pusingan ") + round + " / " + ROUNDS
                + "   \u2B50 " + score);
        heardView.setText("");
        hintView.setText("");

        target = Content.JAWI.get(random.nextInt(Content.JAWI.size()));
        glyphView.setText(target.symbol);
        glyphView.setScaleX(0.6f);
        glyphView.setScaleY(0.6f);
        glyphView.animate().scaleX(1f).scaleY(1f).setDuration(320).start();
        promptView.setText(t("Say this letter's name", "Sebut nama huruf ini"));

        // The child hears the correct name before being asked for it.
        speakTarget();
    }

    private void speakTarget() {
        host.voice().speak(lang, target.name(lang));
    }

    private void listen() {
        if (listening || finished) {
            return;
        }
        listening = true;
        hintView.setText(t("Listening...", "Sedang mendengar..."));
        host.speech().listen(new SpeechHelper.SpeechListener() {
            @Override
            public void onSpeechReady() {
            }

            @Override
            public void onSpeechResult(ArrayList<String> matches) {
                listening = false;
                handleAttempt(matches);
            }

            @Override
            public void onSpeechError(String message) {
                listening = false;
                hintView.setText("\uD83D\uDE14 " + message);
            }
        }, Content.Lang.MS);
    }

    /** The name the child must produce, e.g. "kha". */
    private String expected() {
        return target.ms;
    }

    private void handleAttempt(List<String> matches) {
        if (matches == null || matches.isEmpty()) {
            hintView.setText(t("I did not hear anything. Try again!",
                    "Saya tidak dengar apa-apa. Cuba lagi!"));
            return;
        }
        String heard = matches.get(0);
        heardView.setText(t("I heard: ", "Saya dengar: ") + heard);

        // Spelling the name out is rejected even though it contains the name.
        if (SpeechHelper.spelledOut(heard, expected())) {
            host.progress().onWrongAnswer();
            host.progress().resetStreak();
            hintView.setText(t("Say it as one sound, not letter by letter: ",
                    "Sebut sekali gus, jangan eja satu-satu: ") + expected());
            host.sfx().wrong();
            host.shake(glyphView, 8);
            host.mouthSay(expected());
            return;
        }

        boolean ok = false;
        for (String match : matches) {
            if (SpeechHelper.matchesStrict(expected(), match)) {
                ok = true;
                break;
            }
        }

        if (ok) {
            score += 15;
            host.progress().addPoints(15);
            host.progress().addStar();
            host.progress().addSpoken();
            host.progress().addCorrect();
            host.progress().onCorrectAnswer();
            host.progress().earnedBadges();
            host.updateHeader();
            scoreView.setText(t("Round ", "Pusingan ") + round + " / " + ROUNDS
                    + "   \u2B50 " + score);
            hintView.setText(t("Correct! ", "Betul! ") + expected() + " \u2B50");
            host.pop(glyphView);
            host.celebrate(t("You said it right!", "Sebutan kamu betul!"));
            actions.postDelayed(this::nextRound, 1400);
        } else {
            host.progress().onWrongAnswer();
            host.progress().resetStreak();
            hintView.setText(t("Not quite. Listen and try again: ",
                    "Belum tepat. Dengar dan cuba lagi: ") + expected());
            host.sfx().wrong();
            host.shake(glyphView, 8);
            host.mouthSay(expected());
        }
    }

    private void finish() {
        finished = true;
        glyphView.setText("\uD83C\uDFC6");
        heardView.setText("");
        hintView.setText(t("You scored ", "Markah kamu ") + score + " \u2B50");
        promptView.setText(t("Jawi speaker! ", "Pandai sebut Jawi! "));
        host.celebrate(t("Excellent pronunciation!", "Sebutan yang hebat!"));
        actions.removeAllViews();
        actions.addView(bigButton("\uD83D\uDD01", t("Play again", "Main lagi"),
                0xFF7C4DFF, this::startGame), wrap());
    }
}
