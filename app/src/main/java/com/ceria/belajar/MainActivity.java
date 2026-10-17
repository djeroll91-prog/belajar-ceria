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
    private FrameLayout rootFrame;
    private FrameLayout content;
    private LinearLayout header;
    private TextView pointsPill;
    private TextView langPill;
    private TextView soundPill;
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

        // A faint white wash keeps dark text readable over the painted sky.
        View scrim = new View(this);
        scrim.setBackgroundColor(0x1AFFFFFF);
        rootFrame.addView(scrim, new FrameLayout.LayoutParams(
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
        header.setBackgroundColor(0x33FFFFFF);
        column.addView(header, new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT));

        TextView title = Util.text(this, "\uD83C\uDF1F Belajar Ceria", 20, 0xFF4A148C);
        title.setTypeface(null, android.graphics.Typeface.BOLD);
        LinearLayout.LayoutParams titleLp = new LinearLayout.LayoutParams(
                0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f);
        title.setGravity(Gravity.CENTER_VERTICAL);
        header.addView(title, titleLp);

        pointsPill = Util.text(this, "", 15, Color.WHITE);
        pointsPill.setPadding(Util.dp(this, 12), Util.dp(this, 6),
                Util.dp(this, 12), Util.dp(this, 6));
        LinearLayout.LayoutParams pillLp = new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT);
        pillLp.setMargins(4, 0, 6, 0);
        pointsPill.setBackground(Util.rounded(0xFFFF6D00, Util.dp(this, 20)));
        header.addView(pointsPill, pillLp);

        langPill = Util.text(this, "", 15, 0xFF4A148C);
        langPill.setPadding(Util.dp(this, 12), Util.dp(this, 6),
                Util.dp(this, 12), Util.dp(this, 6));
        langPill.setBackground(Util.rounded(0xFFFFFFFF, Util.dp(this, 20)));
        langPill.setOnClickListener(v -> toggleLang());
        header.addView(langPill, new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT));

        soundPill = Util.text(this, "", 18, 0xFF4A148C);
        soundPill.setPadding(Util.dp(this, 12), Util.dp(this, 6),
                Util.dp(this, 12), Util.dp(this, 6));
        soundPill.setBackground(Util.rounded(0xFFFFFFFF, Util.dp(this, 20)));
        soundPill.setOnClickListener(v -> {
            boolean on = !(isSoundOn() && isMusicOn());
            setSoundOn(on);
            setMusicOn(on);
            if (on) {
                sfx.tap();
                startMusic();
            }
            updateHeader();
        });
        LinearLayout.LayoutParams soundLp = new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT);
        soundLp.setMargins(4, 0, 6, 0);
        header.addView(soundPill, soundLp);

        TextView voiceButton = Util.text(this, "\uD83D\uDD0A", 18, 0xFF4A148C);
        voiceButton.setPadding(Util.dp(this, 12), Util.dp(this, 6),
                Util.dp(this, 12), Util.dp(this, 6));
        voiceButton.setBackground(Util.rounded(0xFFFFFFFF, Util.dp(this, 20)));
        voiceButton.setOnClickListener(v -> open(new VoiceScreen(this)));
        header.addView(voiceButton, new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT));

        content = new FrameLayout(this);
        column.addView(content, new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, 0, 1f));

        setContentView(rootFrame);
        updateHeader();
    }

    public void updateHeader() {
        if (pointsPill != null) {
            pointsPill.setText("\u2B50 " + progress.stars() + "   " + progress.points()
                    + " " + (lang() == Content.Lang.MS ? "markah" : "pts"));
        }
        if (langPill != null) {
            langPill.setText(lang() == Content.Lang.MS
                    ? "\uD83C\uDDF2\uD83C\uDDFE BM"
                    : "\uD83C\uDDEC\uD83C\uDDE7 EN");
        }
        if (soundPill != null) {
            boolean on = isSoundOn() && isMusicOn();
            soundPill.setText(on ? "\uD83D\uDD0A" : "\uD83D\uDD07");
            soundPill.setAlpha(on ? 1f : 0.55f);
        }
    }

    // ------------------------------------------------------------------
    // Sound
    // ------------------------------------------------------------------

    /**
     * Switches the background tune to the one that suits what is on screen, so
     * the app never sits in silence and each activity has its own mood.
     */
    private void startMusic() {
        String track = MusicPlayer.HOME;
        if (currentScreen != null) {
            String name = currentScreen.getClass().getSimpleName();
            if (name.contains("Game") || name.contains("Speak") || name.contains("Coloring")) {
                track = MusicPlayer.PLAY;
            } else {
                track = MusicPlayer.LEARN;
            }
        }
        music.play(track);
    }

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
        column.setPadding(Util.dp(this, 14), Util.dp(this, 14), Util.dp(this, 14), Util.dp(this, 24));
        scroll.addView(column, new FrameLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT));
        content.addView(scroll, new FrameLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT));

        mascot = new RobotView(this);
        mascot.setBodyColor(Util.storyColor(progress.points() / 50));
        mascot.setOnClickListener(v -> {
            mascot.setTalking(true);
            voice.speak(lang(), lang() == Content.Lang.MS
                    ? "Hai kawan! Jom belajar sama-sama!"
                    : "Hi friend! Let's learn together!");
        });
        LinearLayout.LayoutParams robotLp = new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, Util.dp(this, 190));
        robotLp.setMargins(0, Util.dp(this, 4), 0, Util.dp(this, 6));
        column.addView(mascot, robotLp);

        greeting = Util.text(this, lang() == Content.Lang.MS
                ? "Namaku Bobo! Tekan aku ya \uD83D\uDE0A"
                : "I am Bobo! Tap me \uD83D\uDE0A", 17, 0xFF4A148C);
        greeting.setPadding(0, 0, 0, Util.dp(this, 12));
        column.addView(greeting);

        GridLayout grid = new GridLayout(this);
        int columns = getResources().getConfiguration().screenWidthDp >= 600 ? 3 : 2;
        grid.setColumnCount(columns);
        column.addView(grid, new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT));

        addTile(grid, "\uD83D\uDDFA\uFE0F", tr("Laluan Belajar", "Learning Path"), 0xFF9A8FCF,
                () -> open(new LevelMapScreen(this)));
        addTile(grid, "\uD83D\uDD24", tr("Huruf ABC", "ABC Letters"), 0xFFE8877A,
                () -> open(new LearnScreen(this, Content.LETTERS, LearnScreen.Mode.LETTER)));
        addTile(grid, "\uD83D\uDD22", tr("Nombor 123", "Numbers 123"), 0xFF7FB4D9,
                () -> open(new LearnScreen(this, Content.NUMBERS, LearnScreen.Mode.NUMBER)));
        addTile(grid, "\uD83D\uDCD6", tr("Perkataan", "Words"), 0xFF8FBF7F,
                () -> open(new LearnScreen(this, Content.WORDS, LearnScreen.Mode.WORD)));
        addTile(grid, "\uD83D\uDCDD", tr("Ayat", "Sentences"), 0xFFB48FC9,
                () -> open(new SentenceScreen(this)));
        addTile(grid, "\uD83C\uDFA8", tr("Warna", "Colours"), 0xFFF2B457,
                () -> open(new LearnScreen(this, Content.COLOR_ITEMS, LearnScreen.Mode.WORD)
                        .withTitle("\uD83C\uDFA8 Colours", "\uD83C\uDFA8 Warna")));
        addTile(grid, "\uD83D\uDC41", tr("Tubuh Badan", "My Body"), 0xFFE39BB5,
                () -> open(new LearnScreen(this, Content.BODY, LearnScreen.Mode.WORD)
                        .withTitle("\uD83D\uDC41 My Body", "\uD83D\uDC41 Tubuh Badan")));
        addTile(grid, "\uD83D\uDC3E", tr("Haiwan", "Animals"), 0xFF8FBF7F,
                () -> open(new LearnScreen(this, Content.ANIMALS, LearnScreen.Mode.WORD)
                        .withTitle("\uD83D\uDC3E Animals", "\uD83D\uDC3E Haiwan")));
        addTile(grid, "\uD83C\uDF31", tr("Tumbuhan", "Plants"), 0xFF7FC7C0,
                () -> open(new LearnScreen(this, Content.PLANTS, LearnScreen.Mode.WORD)
                        .withTitle("\uD83C\uDF31 Plants", "\uD83C\uDF31 Tumbuh-tumbuhan")));
        addTile(grid, "\uD83C\uDF4E", tr("Buah-buahan", "Fruits"), 0xFFE39BB5,
                () -> open(new LearnScreen(this, Content.FRUITS, LearnScreen.Mode.WORD)
                        .withTitle("\uD83C\uDF4E Fruits", "\uD83C\uDF4E Buah-buahan")));
        addTile(grid, "\uD83C\uDF89", tr("Benda Hidup", "Living Things"), 0xFF8C9CC9,
                () -> open(new LearnScreen(this, Content.LIVING, LearnScreen.Mode.WORD)
                        .withTitle("\uD83C\uDF89 Living Things", "\uD83C\uDF89 Benda Hidup")));
        addTile(grid, "\u0645", tr("Huruf Jawi", "Jawi Letters"), 0xFF7FC7C0,
                () -> open(new LearnScreen(this, Content.JAWI, LearnScreen.Mode.WORD)
                        .withTitle("\u0645 Jawi", "\u0645 Jawi")));
        addTile(grid, "\u0639", tr("Huruf Arab", "Arabic Letters"), 0xFFC9A227,
                () -> open(new LearnScreen(this, Content.ARABIC, LearnScreen.Mode.WORD)
                        .withTitle("\u0639 Arabic", "\u0639 Arab")));
        addTile(grid, "\uD83C\uDFA4", tr("Sebut & Menang", "Say & Win"), 0xFFF09B5B,
                () -> open(new SpeakScreen(this)));
        addTile(grid, "\uD83C\uDFAF", tr("Kuiz", "Quiz"), 0xFFE8877A,
                () -> open(new QuizGame(this)));
        addTile(grid, "\uD83D\uDD24", tr("Susun Huruf", "Spell the Word"), 0xFF9A8FCF,
                () -> open(new SpellGame(this)));
        addTile(grid, "\uD83D\uDD0D", tr("Cari Yang Berbeza", "Odd One Out"), 0xFF7FC7C0,
                () -> open(new OddOneOutGame(this)));
        addTile(grid, "\uD83D\uDC22", tr("Kira Objek", "Count"), 0xFF7FC7C0,
                () -> open(new CountGame(this)));
        addTile(grid, "\uD83E\uDDE9", tr("Apa Seterusnya?", "What Comes Next?"), 0xFF9CCC65,
                () -> open(new PatternGame(this)));
        addTile(grid, "\uD83E\uDEE7", tr("Letup Gelembung", "Bubble Pop"), 0xFF26C6DA,
                () -> open(new BubblePopGame(this)));
        addTile(grid, "\uD83E\uDDFA", tr("Bakul Susun", "Sorting Baskets"), 0xFF8FBF7F,
                () -> open(new SortGame(this)));
        addTile(grid, "\u2795", tr("Tambah & Tolak", "Add & Take Away"), 0xFF7FB4D9,
                () -> open(new MathGame(this)));
        addTile(grid, "\u270F\uFE0F", tr("Tulis Huruf", "Trace Letters"), 0xFFE8877A,
                () -> open(new TraceGame(this, TraceGame.Mode.LETTER)));
        addTile(grid, "\u270F\uFE0F", tr("Tulis Nombor", "Trace Numbers"), 0xFF7FB4D9,
                () -> open(new TraceGame(this, TraceGame.Mode.NUMBER)));
        addTile(grid, "\u270F\uFE0F", tr("Tulis Jawi", "Trace Jawi"), 0xFF7FC7C0,
                () -> open(new TraceGame(this, TraceGame.Mode.JAWI)));
        addTile(grid, "\uD83D\uDDE3\uFE0F", tr("Sebut Jawi", "Say Jawi"), 0xFFB48FC9,
                () -> open(new JawiSpeakGame(this)));
        addTile(grid, "\uD83E\uDDE9", tr("Memori", "Memory"), 0xFF8C9CC9,
                () -> open(new MemoryGame(this)));
        addTile(grid, "\uD83C\uDFA8", tr("Mewarna", "Colouring"), 0xFFE39BB5,
                () -> open(new ColoringScreen(this)));
        addTile(grid, "\uD83C\uDFC6", tr("Ganjaran", "Rewards"), 0xFFF2B457,
                () -> open(new RewardsScreen(this)));
        addTile(grid, "\uD83D\uDD0A", tr("Suara Bacaan", "Reading Voice"), 0xFF8C9CC9,
                () -> open(new VoiceScreen(this)));
        addTile(grid, "\uD83C\uDF99\uFE0F", tr("Studio Rakaman", "Recording Studio"), 0xFF7FC7C0,
                () -> open(new RecordingStudioScreen(this)));
        addTile(grid, "\uD83C\uDFA4", tr("Kredit Suara", "Voice Credits"), 0xFFC9A227,
                () -> open(new CreditsScreen(this)));
        addTile(grid, "\uD83C\uDFB9", tr("Xylophone", "Xylophone"), 0xFF8FBF7F,
                () -> open(new XylophoneScreen(this)));
        addTile(grid, "\uD83D\uDC15", tr("Suara Haiwan", "Animal Sounds"), 0xFFF2B457,
                () -> open(new AnimalSoundScreen(this)));
        addTile(grid, "\uD83C\uDFB5", tr("Karaoke ABC", "ABC Karaoke"), 0xFFE8877A,
                () -> open(new KaraokeScreen(this)));
        startMusic();
    }

    private String tr(String ms, String en) {
        return lang() == Content.Lang.MS ? ms : en;
    }

    private void addTile(GridLayout grid, String emoji, String label, int color, Runnable action) {
        LinearLayout tile = new LinearLayout(this);
        tile.setOrientation(LinearLayout.VERTICAL);
        tile.setGravity(Gravity.CENTER);
        tile.setPadding(Util.dp(this, 8), Util.dp(this, 16), Util.dp(this, 8), Util.dp(this, 16));
        // A soft white rim gives the tiles a painted-card feel against the sky.
        tile.setBackground(Util.roundedStroke(color, Util.dp(this, 24),
                0xCCFFFFFF, Util.dp(this, 3)));
        tile.setElevation(Util.dp(this, 6));
        tile.setClickable(true);
        tile.setOnClickListener(v -> {
            clickBee(v);
            v.postDelayed(action, 90);
        });

        TextView icon = Util.text(this, emoji, 40, Color.WHITE);
        tile.addView(icon);
        TextView text = Util.text(this, label, 15, Color.WHITE);
        text.setTypeface(null, android.graphics.Typeface.BOLD);
        tile.addView(text);

        GridLayout.LayoutParams lp = new GridLayout.LayoutParams();
        lp.width = 0;
        lp.height = Util.dp(this, 116);
        lp.columnSpec = GridLayout.spec(GridLayout.UNDEFINED, 1f);
        lp.setMargins(Util.dp(this, 6), Util.dp(this, 6), Util.dp(this, 6), Util.dp(this, 6));
        grid.addView(tile, lp);
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
