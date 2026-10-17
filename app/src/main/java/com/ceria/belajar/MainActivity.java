package com.ceria.belajar;

import android.animation.ObjectAnimator;
import android.app.Activity;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.graphics.Color;
import android.os.Bundle;
import android.view.Gravity;
import android.view.View;
import android.view.ViewGroup;
import android.view.WindowManager;
import android.widget.FrameLayout;
import android.widget.GridLayout;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.TextView;

/**
 * Home of the app. It owns the shared services (speech, progress, language) and
 * swaps the different learning screens into its content frame.
 */
public class MainActivity extends Activity {

    private SpeechHelper speech;
    private VoicePack voice;
    private SoundEffects sfx;
    private MusicPlayer music;
    private Progress progress;
    private Theme theme;
    private FrameLayout rootFrame;
    private FrameLayout content;
    private LinearLayout header;
    private TextView homeButton;
    private TextView pointsPill;
    private TextView langPill;
    private TextView themePill;
    private TextView soundPill;
    private TextView musicPill;
    private TextView greeting;
    private RobotView mascot;
    private CelebrationView celebration;
    private Screen currentScreen;
    private final android.os.Handler ui = new android.os.Handler(android.os.Looper.getMainLooper());
    private Runnable unduck;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        getWindow().addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);
        progress = new Progress(this);
        theme = new Theme(this);
        speech = new SpeechHelper(this);
        voice = new VoicePack(this, speech);
        sfx = new SoundEffects(this);
        music = new MusicPlayer(this);
        sfx.setEnabled(progress.isSoundOn());
        music.setEnabled(progress.isMusicOn());
        speech.setOnDone(() -> {
            if (mascot != null) {
                mascot.setTalking(false);
            }
            unduckMusic();
        });
        buildChrome();
        showHome();
    }

    public SoundEffects sfx() {
        return sfx;
    }

    public MusicPlayer music() {
        return music;
    }

    /** Sound effects and music, so callers can stop them before speaking. */
    public void setSoundOn(boolean on) {
        progress.setSoundOn(on);
        sfx.setEnabled(on);
        updateHeader();
    }

    public void setMusicOn(boolean on) {
        progress.setMusicOn(on);
        music.setEnabled(on);
        // Turning music back on should restore the tune for wherever we are,
        // rather than leaving the screen silent until the next navigation.
        if (on) {
            startMusic();
        }
        updateHeader();
    }

    public boolean isSoundOn() {
        return progress.isSoundOn();
    }

    public boolean isMusicOn() {
        return progress.isMusicOn();
    }

    public Progress progress() {
        return progress;
    }

    /** A short line of praise in the current language, for the robot and voice. */
    public String praise() {
        String[] ms = {"Bagus sekali!", "Hebat!", "Pandai!", "Syabas!", "Betul!"};
        String[] en = {"Great job!", "Well done!", "Brilliant!", "You did it!", "Correct!"};
        String[] list = lang() == Content.Lang.MS ? ms : en;
        return list[(int) (System.currentTimeMillis() % list.length)];
    }

    /** Rebuilds the home screen in place, after a setting changed. */
    public void rebuildHome() {
        showHome();
    }

    /** The tunjang a unit belongs to, for colouring its screens. */
    public String tunjangIdOf(Content.Unit unit) {
        return Content.tunjangIdOf(unit);
    }

    public FrameLayout rootFrame() {
        return rootFrame;
    }

    public SpeechHelper speech() {
        return speech;
    }

    /** Prefers human recordings, falls back to the speech engine. */
    public VoicePack voice() {
        return voice;
    }

    /** The mascot on the home screen, or null when another screen is open. */
    public RobotView mascot() {
        return mascot;
    }

    /** Call after new recordings are added so the new clips are picked up. */
    public void refreshVoicePack() {
        voice.index();
    }

    public Content.Lang lang() {
        return progress.getLang();
    }

    // ------------------------------------------------------------------
    // Shared chrome: bright background, playful header and content frame.
    // ------------------------------------------------------------------

    private void buildChrome() {
        rootFrame = new FrameLayout(this);
        rootFrame.addView(new StorySceneView(this), new FrameLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT));

        LinearLayout column = new LinearLayout(this);
        column.setOrientation(LinearLayout.VERTICAL);
        rootFrame.addView(column, new FrameLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT));

        header = new LinearLayout(this);
        header.setOrientation(LinearLayout.HORIZONTAL);
        header.setGravity(Gravity.CENTER_VERTICAL);
        header.setPadding(Util.dp(this, 12), Util.dp(this, 10),
                Util.dp(this, 12), Util.dp(this, 10));
        column.addView(header, new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT));

        homeButton = Util.text(this, "\uD83C\uDFE0", 20, theme.ink());
        homeButton.setGravity(Gravity.CENTER);
        homeButton.setBackground(Util.roundedStroke(theme.surface(), Util.dp(this, 16),
                theme.line(), Util.dp(this, 2)));
        homeButton.setPadding(Util.dp(this, 12), Util.dp(this, 8),
                Util.dp(this, 12), Util.dp(this, 8));
        homeButton.setOnClickListener(v -> {
            sfx.tap();
            showHome();
        });
        header.addView(homeButton, pillParams(0, 0, 6, 0));

        pointsPill = Util.text(this, "", 16, theme.onAccent());
        pointsPill.setTypeface(null, android.graphics.Typeface.BOLD);
        pointsPill.setGravity(Gravity.CENTER);
        pointsPill.setBackground(Util.rounded(theme.accent(), Util.dp(this, 16)));
        pointsPill.setPadding(Util.dp(this, 14), Util.dp(this, 9),
                Util.dp(this, 14), Util.dp(this, 9));
        LinearLayout.LayoutParams pointsLp = new LinearLayout.LayoutParams(
                0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f);
        pointsLp.setMargins(Util.dp(this, 4), 0, Util.dp(this, 6), 0);
        header.addView(pointsPill, pointsLp);

        langPill = Util.text(this, "", 16, theme.ink());
        langPill.setTypeface(null, android.graphics.Typeface.BOLD);
        langPill.setGravity(Gravity.CENTER);
        langPill.setBackground(Util.roundedStroke(theme.surface(), Util.dp(this, 16),
                theme.line(), Util.dp(this, 2)));
        langPill.setPadding(Util.dp(this, 12), Util.dp(this, 9),
                Util.dp(this, 12), Util.dp(this, 9));
        langPill.setOnClickListener(v -> toggleLang());
        header.addView(langPill, pillParams(0, 0, 6, 0));

        themePill = Util.text(this, "", 18, theme.ink());
        themePill.setGravity(Gravity.CENTER);
        themePill.setBackground(Util.roundedStroke(theme.surface(), Util.dp(this, 16),
                theme.line(), Util.dp(this, 2)));
        themePill.setPadding(Util.dp(this, 12), Util.dp(this, 9),
                Util.dp(this, 12), Util.dp(this, 9));
        themePill.setOnClickListener(v -> {
            theme.toggle();
            rebuildForTheme();
        });
        header.addView(themePill, pillParams(0, 0, 6, 0));

        soundPill = Util.text(this, "", 18, theme.ink());
        soundPill.setGravity(Gravity.CENTER);
        soundPill.setBackground(Util.roundedStroke(theme.surface(), Util.dp(this, 16),
                theme.line(), Util.dp(this, 2)));
        soundPill.setPadding(Util.dp(this, 12), Util.dp(this, 9),
                Util.dp(this, 12), Util.dp(this, 9));
        soundPill.setOnClickListener(v -> {
            boolean on = !isSoundOn();
            setSoundOn(on);
            if (on) {
                sfx.tap();
            }
            updateHeader();
        });
        header.addView(soundPill, pillParams(0, 0, 6, 0));

        // Music keeps its own switch. A parent who finds the tune distracting
        // should not have to give up the tap and reward sounds as well.
        musicPill = Util.text(this, "", 18, theme.ink());
        musicPill.setGravity(Gravity.CENTER);
        musicPill.setBackground(Util.roundedStroke(theme.surface(), Util.dp(this, 16),
                theme.line(), Util.dp(this, 2)));
        musicPill.setPadding(Util.dp(this, 12), Util.dp(this, 9),
                Util.dp(this, 12), Util.dp(this, 9));
        musicPill.setOnClickListener(v -> {
            setMusicOn(!isMusicOn());
            updateHeader();
        });
        header.addView(musicPill, pillParams(0, 0, 6, 0));

        content = new FrameLayout(this);
        column.addView(content, new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, 0, 1f));

        setContentView(rootFrame);
        updateHeader();
    }

    private LinearLayout.LayoutParams pillParams(int left, int top, int right, int bottom) {
        LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT);
        lp.setMargins(Util.dp(this, left), Util.dp(this, top),
                Util.dp(this, right), Util.dp(this, bottom));
        return lp;
    }

    /**
     * Rebuilds the visible screen after the theme changes. Views are built in
     * code rather than inflated, so a new palette has to be drawn again rather
     * than restyled in place.
     */
    public void rebuildForTheme() {
        sfx.tap();
        Screen keep = currentScreen;
        buildChrome();
        if (keep == null) {
            showHome();
        } else {
            open(keep);
        }
    }

    public Theme theme() {
        return theme;
    }

    public void updateHeader() {
        if (pointsPill != null) {
            pointsPill.setText("\u2B50 " + progress.stars() + "   " + progress.points()
                    + " " + (lang() == Content.Lang.MS ? "markah" : "pts"));
        }
        if (langPill != null) {
            langPill.setText(lang() == Content.Lang.MS ? "\uD83C\uDDF2\uD83C\uDDFE BM" : "\uD83C\uDDEC\uD83C\uDDE7 EN");
        }
        if (themePill != null) {
            themePill.setText(theme.isDark() ? "\u2600\uFE0F" : "\uD83C\uDF19");
        }
        if (soundPill != null) {
            boolean on = isSoundOn();
            soundPill.setText(on ? "\uD83D\uDD0A" : "\uD83D\uDD07");
            soundPill.setAlpha(on ? 1f : 0.55f);
        }
        if (musicPill != null) {
            boolean on = isMusicOn();
            musicPill.setText(on ? "\uD83C\uDFB5" : "\uD83D\uDE36");
            musicPill.setAlpha(on ? 1f : 0.55f);
        }
    }

    // ------------------------------------------------------------------
    // Sound
    // ------------------------------------------------------------------

    /**
     * Switches the background tune to the one that suits what is on screen.
     *
     * Games and the recording studio get no tune at all: a looping melody under
     * a child who is trying to think, listen to a prompt, or record their voice
     * is noise, not atmosphere. The home screen and the reading screens keep a
     * gentle tune so the app never feels dead.
     */
    private void startMusic() {
        music.play(trackFor(currentScreen));
    }

    /** The tune that belongs on a given screen; null screen means the home page. */
    private String trackFor(Screen screen) {
        if (screen == null) {
            return MusicPlayer.HOME;
        }
        String name = screen.getClass().getSimpleName();
        if (name.endsWith("Game") || QUIET_SCREENS.contains(name)) {
            return MusicPlayer.SILENT;
        }
        return MusicPlayer.LEARN;
    }

    /**
     * Screens that need quiet: anything with a microphone, a music toy, or a
     * task the child must concentrate on. A tune here competes with the very
     * thing the screen is for.
     */
    private static final java.util.Set<String> QUIET_SCREENS = new java.util.HashSet<>(
            java.util.Arrays.asList(
                    "SpeakScreen", "ColoringScreen", "KaraokeScreen", "XylophoneScreen",
                    "AnimalSoundScreen", "RecordingStudioScreen", "VoiceScreen",
                    "CreditsScreen", "RewardsScreen", "QuizScreen"));

    /** Drops the music while a phrase is spoken, then brings it back. */
    private void duckMusic() {
        music.duck();
        if (unduck != null) {
            ui.removeCallbacks(unduck);
        }
        unduck = () -> music.unduck();
        ui.postDelayed(unduck, 4000);
    }

    private void unduckMusic() {
        if (unduck != null) {
            ui.removeCallbacks(unduck);
            unduck = null;
        }
        music.unduck();
    }

    // ------------------------------------------------------------------
    // Feedback
    // ------------------------------------------------------------------

    /** A tap anywhere in the app: a soft blip plus a little squish. */
    public void tapFeedback(View view) {
        sfx.tap();
        if (view != null) {
            ObjectAnimator.ofFloat(view, "scaleX", 1f, 0.92f, 1.04f, 1f)
                    .setDuration(200).start();
            ObjectAnimator.ofFloat(view, "scaleY", 1f, 0.92f, 1.04f, 1f)
                    .setDuration(200).start();
        }
    }

    /** A correct answer: sparkle sound and a happy wobble. */
    public void correctFeedback(View view) {
        sfx.correct();
        if (view != null) {
            view.animate().rotation(-4f).setDuration(90)
                    .withEndAction(() -> view.animate().rotation(4f).setDuration(90)
                            .withEndAction(() -> view.animate().rotation(0f)
                                    .setDuration(90).start()).start())
                    .start();
        }
    }

    /** A wrong answer: a soft buzz and a gentle shake, never a harsh sound. */
    public void wrongFeedback(View view) {
        sfx.wrong();
        if (view != null) {
            shake(view, 8);
        }
    }

    /** A quick side-to-side wobble that says "not that one" without words. */
    public void shake(View view, int distanceDp) {
        if (view == null) {
            return;
        }
        float d = Util.dp(this, distanceDp);
        view.animate().translationX(d).setDuration(70)
                .withEndAction(() -> view.animate().translationX(-d).setDuration(70)
                        .withEndAction(() -> view.animate().translationX(0)
                                .setDuration(70).start()).start())
                .start();
    }

    /** A happy pop: the view springs past its size and settles back. */
    public void pop(View view) {
        if (view == null) {
            return;
        }
        view.animate().scaleX(1.18f).scaleY(1.18f).setDuration(140)
                .withEndAction(() -> view.animate().scaleX(1f).scaleY(1f)
                        .setDuration(160).start())
                .start();
    }

    /** A small hop used for correct answers, so praise is visible as well as heard. */
    public void hop(View view) {
        if (view == null) {
            return;
        }
        view.animate().translationY(-Util.dp(this, 14)).setDuration(150)
                .withEndAction(() -> view.animate().translationY(0).setDuration(180).start())
                .start();
    }

    private void toggleLang() {
        Content.Lang next = lang() == Content.Lang.MS ? Content.Lang.EN : Content.Lang.MS;
        progress.setLang(next);
        updateHeader();
        if (mascot != null) {
            mouthSay(next == Content.Lang.MS ? "Bahasa Melayu!" : "English!");
        }
        showHome();
    }

    // ------------------------------------------------------------------
    // Home screen
    // ------------------------------------------------------------------

    public void showHome() {
        if (currentScreen instanceof RecordingStudioScreen) {
            ((RecordingStudioScreen) currentScreen).onLeave();
        }
        currentScreen = null;
        content.removeAllViews();

        ScrollView scroll = new ScrollView(this);
        scroll.setFillViewport(true);
        LinearLayout column = new LinearLayout(this);
        column.setOrientation(LinearLayout.VERTICAL);
        column.setPadding(Util.dp(this, 14), Util.dp(this, 14), Util.dp(this, 14), Util.dp(this, 28));
        scroll.addView(column, new FrameLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT));
        content.addView(scroll, new FrameLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT));

        column.addView(welcomeCard(), wrap());

        // Anything the child keeps getting wrong is offered again before the
        // rest of the menu, so practice comes before browsing.
        java.util.List<Review.WeakUnit> weak = Review.weakUnits(progress, 3);
        if (!weak.isEmpty()) {
            column.addView(sectionTitle(tr("Perlu diulang", "Needs practice"), weak.size()), wrap());
            LinearLayout row = new LinearLayout(this);
            row.setOrientation(LinearLayout.HORIZONTAL);
            for (Review.WeakUnit w : weak) {
                Content.Tunjang owner = Content.tunjangById(w.tunjangId);
                row.addView(navTile(w.unit.emoji, w.unit.title(lang()),
                        Math.round(w.accuracy * 100) + "%",
                        owner == null ? theme.accent() : theme.tunjangColor(owner.color),
                        () -> open(new UnitScreen(this, w.unit))), weightParams());
            }
            column.addView(row, wrap());
        }

        column.addView(sectionTitle(tr("Enam tunjang", "The six tunjang"), Content.TUNJANG.size()), wrap());
        for (Content.Tunjang tunjang : Content.TUNJANG) {
            column.addView(tunjangRow(tunjang), wrap());
        }

        column.addView(sectionTitle(tr("Aktiviti", "Activities"), 0), wrap());
        GridLayout activities = new GridLayout(this);
        int cols = getResources().getConfiguration().screenWidthDp >= 600 ? 4 : 3;
        activities.setColumnCount(cols);
        column.addView(activities, wrap());
        addTile(activities, "\uD83E\uDDE0", tr("Ulang kaji", "Review"), theme.accent(),
                () -> open(new ReviewScreen(this)));
        addTile(activities, "\uD83C\uDFC6", tr("Ganjaran", "Rewards"), Theme.GOLD,
                () -> open(new RewardsScreen(this)));
        addTile(activities, "\uD83C\uDFAE", tr("Permainan", "Games"), 0xFF7ED957,
                () -> open(new GamesScreen(this)));
        addTile(activities, "\uD83D\uDCC5", tr("Tema Mingguan", "Weekly Themes"), 0xFF8FD3F4,
                () -> open(new ThemesScreen(this)));
        addTile(activities, "\uD83D\uDD0A", tr("Suara Bacaan", "Reading Voice"), 0xFF8C9CC9,
                () -> open(new VoiceScreen(this)));
        addTile(activities, "\uD83C\uDF99\uFE0F", tr("Studio Rakaman", "Recording Studio"), 0xFF7FC7C0,
                () -> open(new RecordingStudioScreen(this)));
        addTile(activities, "\u2699\uFE0F", tr("Tetapan", "Settings"), 0xFFB9AECF,
                () -> open(new SettingsScreen(this)));

        startMusic();
    }

    private LinearLayout.LayoutParams wrap() {
        return new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
    }

    private LinearLayout.LayoutParams weightParams() {
        LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(0,
                ViewGroup.LayoutParams.WRAP_CONTENT, 1f);
        lp.setMargins(Util.dp(this, 5), Util.dp(this, 5), Util.dp(this, 5), Util.dp(this, 5));
        return lp;
    }

    /** A short welcome strip: the child's name for the app, and the robot. */
    private View welcomeCard() {
        LinearLayout card = new LinearLayout(this);
        card.setOrientation(LinearLayout.HORIZONTAL);
        card.setGravity(Gravity.CENTER_VERTICAL);
        card.setPadding(Util.dp(this, 16), Util.dp(this, 14),
                Util.dp(this, 16), Util.dp(this, 14));
        card.setBackground(Util.roundedStroke(theme.surface(), Util.dp(this, 26),
                theme.line(), Util.dp(this, 2)));
        card.setElevation(Util.dp(this, 3));

        mascot = new RobotView(this);
        mascot.setBodyColor(Util.storyColor(progress.points() / 50));
        mascot.setOnClickListener(v -> {
            mascot.setTalking(true);
            mouthSay(tr("Hai kawan! Jom belajar sama-sama!", "Hi friend! Let's learn together!"));
        });
        LinearLayout.LayoutParams robotLp = new LinearLayout.LayoutParams(
                Util.dp(this, 104), Util.dp(this, 104));
        card.addView(mascot, robotLp);

        LinearLayout texts = new LinearLayout(this);
        texts.setOrientation(LinearLayout.VERTICAL);
        texts.setPadding(Util.dp(this, 14), 0, 0, 0);

        TextView head = Util.text(this, tr("Selamat datang ke Belajar Ceria!",
                "Welcome to Belajar Ceria!"), 20, theme.ink());
        head.setTypeface(null, android.graphics.Typeface.BOLD);
        texts.addView(head);

        greeting = Util.text(this, tr("Namaku Bobo! Tekan aku ya \uD83D\uDE0A",
                "I am Bobo! Tap me \uD83D\uDE0A"), 15, theme.inkSoft());
        greeting.setPadding(0, Util.dp(this, 4), 0, 0);
        texts.addView(greeting);

        TextView sub = Util.text(this, tr("Prasekolah mengikut KSPK \u2014 6 tunjang",
                "Preschool following KSPK \u2014 6 tunjang"), 13, theme.inkSoft());
        sub.setPadding(0, Util.dp(this, 2), 0, 0);
        texts.addView(sub);

        card.addView(texts, new LinearLayout.LayoutParams(0,
                ViewGroup.LayoutParams.WRAP_CONTENT, 1f));
        return card;
    }

    private View sectionTitle(String text, int count) {
        LinearLayout row = new LinearLayout(this);
        row.setOrientation(LinearLayout.HORIZONTAL);
        row.setGravity(Gravity.CENTER_VERTICAL);
        row.setPadding(Util.dp(this, 4), Util.dp(this, 18), 0, Util.dp(this, 8));

        TextView label = Util.text(this, text, 19, theme.ink());
        label.setTypeface(null, android.graphics.Typeface.BOLD);
        row.addView(label);

        if (count > 0) {
            TextView badge = Util.text(this, String.valueOf(count), 12, theme.inkSoft());
            badge.setBackground(Util.rounded(theme.surface2(), Util.dp(this, 10)));
            badge.setPadding(Util.dp(this, 8), Util.dp(this, 3),
                    Util.dp(this, 8), Util.dp(this, 3));
            LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(
                    ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT);
            lp.setMargins(Util.dp(this, 8), 0, 0, 0);
            row.addView(badge, lp);
        }
        return row;
    }

    /** One tunjang as a full-width row: colour strip, name, and how much is done. */
    private View tunjangRow(Content.Tunjang tunjang) {
        int color = theme.tunjangColor(tunjang.color);

        LinearLayout row = new LinearLayout(this);
        row.setOrientation(LinearLayout.HORIZONTAL);
        row.setGravity(Gravity.CENTER_VERTICAL);
        row.setPadding(Util.dp(this, 14), Util.dp(this, 14),
                Util.dp(this, 14), Util.dp(this, 14));
        row.setBackground(Util.roundedStroke(theme.wash(color, 0.10f), Util.dp(this, 22),
                color, Util.dp(this, 3)));
        row.setElevation(Util.dp(this, 3));
        row.setClickable(true);

        TextView emoji = Util.text(this, tunjang.emoji, 30, theme.ink());
        emoji.setGravity(Gravity.CENTER);
        row.addView(emoji, new LinearLayout.LayoutParams(Util.dp(this, 54),
                ViewGroup.LayoutParams.WRAP_CONTENT));

        LinearLayout texts = new LinearLayout(this);
        texts.setOrientation(LinearLayout.VERTICAL);
        texts.setPadding(Util.dp(this, 6), 0, 0, 0);

        TextView name = Util.text(this, tunjang.title(lang()), 18, theme.ink());
        name.setTypeface(null, android.graphics.Typeface.BOLD);
        texts.addView(name);

        int done = 0, total = 0;
        for (Content.Unit unit : tunjang.units) {
            for (Content.Item item : unit.items) {
                total++;
                if (progress.boxOf(unit.id, Progress.keyOf(item)) >= 2) {
                    done++;
                }
            }
        }
        String sub = tunjang.units.size() + " " + tr("unit", "units");
        if (total > 0) {
            sub += " \u00B7 " + done + "/" + total + " " + tr("dikuasai", "mastered");
        }
        TextView meta = Util.text(this, sub, 13, theme.inkSoft());
        meta.setPadding(0, Util.dp(this, 3), 0, 0);
        texts.addView(meta);

        if (total > 0) {
            LinearLayout bar = new LinearLayout(this);
            bar.setOrientation(LinearLayout.HORIZONTAL);
            bar.setBackground(Util.rounded(theme.surface2(), Util.dp(this, 5)));
            LinearLayout.LayoutParams barLp = new LinearLayout.LayoutParams(
                    ViewGroup.LayoutParams.MATCH_PARENT, Util.dp(this, 8));
            barLp.setMargins(0, Util.dp(this, 7), 0, 0);
            texts.addView(bar, barLp);
            if (done > 0) {
                View fill = new View(this);
                fill.setBackground(Util.rounded(Theme.GOOD, Util.dp(this, 5)));
                bar.addView(fill, new LinearLayout.LayoutParams(0, Util.dp(this, 8), done / (float) total));
                bar.addView(new View(this), new LinearLayout.LayoutParams(0, Util.dp(this, 8),
                        (total - done) / (float) total));
            }
        }

        row.addView(texts, new LinearLayout.LayoutParams(0,
                ViewGroup.LayoutParams.WRAP_CONTENT, 1f));

        TextView arrow = Util.text(this, "\u203A", 26, color);
        row.addView(arrow);

        row.setOnClickListener(v -> {
            clickBee(v);
            v.postDelayed(() -> open(new TunjangScreen(this, tunjang)), 90);
        });
        return row;
    }

    private String tr(String ms, String en) {
        return lang() == Content.Lang.MS ? ms : en;
    }

    private void addTile(GridLayout grid, String emoji, String label, int color, Runnable action) {
        grid.addView(navTile(emoji, label, null, color, action), tileParams(grid));
    }

    private GridLayout.LayoutParams tileParams(GridLayout grid) {
        GridLayout.LayoutParams lp = new GridLayout.LayoutParams();
        lp.width = 0;
        lp.height = Util.dp(this, 112);
        lp.columnSpec = GridLayout.spec(GridLayout.UNDEFINED, 1f);
        lp.setMargins(Util.dp(this, 5), Util.dp(this, 5), Util.dp(this, 5), Util.dp(this, 5));
        return lp;
    }

    /** A tappable card: soft tint of its own colour, with the label underneath. */
    private View navTile(String emoji, String label, String sub, int color, Runnable action) {
        LinearLayout tile = new LinearLayout(this);
        tile.setOrientation(LinearLayout.VERTICAL);
        tile.setGravity(Gravity.CENTER);
        tile.setPadding(Util.dp(this, 8), Util.dp(this, 12),
                Util.dp(this, 8), Util.dp(this, 12));
        tile.setBackground(Util.roundedStroke(theme.wash(color, 0.14f), Util.dp(this, 22),
                color, Util.dp(this, 3)));
        tile.setElevation(Util.dp(this, 3));
        tile.setClickable(true);
        tile.setOnClickListener(v -> {
            clickBee(v);
            v.postDelayed(action, 90);
        });

        TextView icon = Util.text(this, emoji, 30, theme.ink());
        icon.setGravity(Gravity.CENTER);
        tile.addView(icon);

        TextView text = Util.text(this, label, 14, theme.ink());
        text.setTypeface(null, android.graphics.Typeface.BOLD);
        text.setGravity(Gravity.CENTER);
        tile.addView(text);

        if (sub != null) {
            TextView subText = Util.text(this, sub, 12, theme.inkSoft());
            subText.setGravity(Gravity.CENTER);
            tile.addView(subText);
        }
        return tile;
    }

    private void clickBee(View v) {
        tapFeedback(v);
    }

    /** Replaces the content frame with a screen view. */
    public void open(Screen screen) {
        if (currentScreen instanceof RecordingStudioScreen) {
            ((RecordingStudioScreen) currentScreen).onLeave();
        }
        currentScreen = screen;
        content.removeAllViews();
        content.addView(screen.build(), new FrameLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT));
        startMusic();
    }

    public View buildHeaderButton(String label, int color, Runnable action) {
        TextView b = Util.text(this, label, 15, Color.WHITE);
        b.setPadding(Util.dp(this, 14), Util.dp(this, 8), Util.dp(this, 14), Util.dp(this, 8));
        b.setBackground(Util.rounded(color, Util.dp(this, 18)));
        b.setOnClickListener(v -> action.run());
        return b;
    }

    // ------------------------------------------------------------------
    // Rewards / feedback
    // ------------------------------------------------------------------

    /** Shows confetti and a praising bubble. Points and stars are added by callers. */
    public void celebrate(String message) {
        if (celebration == null) {
            celebration = new CelebrationView(this);
            rootFrame.addView(celebration, new FrameLayout.LayoutParams(
                    ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT));
        }
        celebration.post(() -> celebration.burst());
        sfx.celebrate();
        if (mascot != null) {
            mascot.cheer();
            mouthSay(message);
        }
    }

    public void mouthSay(String message) {
        if (mascot == null) {
            return;
        }
        mascot.setTalking(true);
        duckMusic();
        voice.speak(lang(), message);
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        speech.handleResult(requestCode, resultCode, data);
        if (requestCode == RecordingStudioScreen.REQUEST_ZIP
                && currentScreen instanceof RecordingStudioScreen) {
            if (resultCode == RESULT_OK && data != null && data.getData() != null) {
                ((RecordingStudioScreen) currentScreen).importZip(data.getData());
            }
            return;
        }
        if (currentScreen instanceof RecordingStudioScreen) {
            ((RecordingStudioScreen) currentScreen).onLeave();
        }
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, String[] permissions, int[] results) {
        super.onRequestPermissionsResult(requestCode, permissions, results);
        if (currentScreen instanceof RecordingStudioScreen && results.length > 0) {
            ((RecordingStudioScreen) currentScreen)
                    .onPermissionResult(results[0] == PackageManager.PERMISSION_GRANTED);
        }
    }

    @Override
    protected void onPause() {
        super.onPause();
        music.pause();
    }

    @Override
    protected void onResume() {
        super.onResume();
        music.resume();
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        ui.removeCallbacksAndMessages(null);
        if (music != null) {
            music.shutdown();
        }
        if (sfx != null) {
            sfx.release();
        }
        if (voice != null) {
            voice.shutdown();
        }
        if (speech != null) {
            speech.shutdown();
        }
    }

    /** Every screen implements this so the host can mount it into the frame. */
    public interface Screen {
        View build();
    }
}
