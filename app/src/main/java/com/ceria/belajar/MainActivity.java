package com.ceria.belajar;

import android.animation.ObjectAnimator;
import android.app.Activity;
import android.content.Intent;
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
    private Progress progress;
    private FrameLayout rootFrame;
    private FrameLayout content;
    private LinearLayout header;
    private TextView pointsPill;
    private TextView langPill;
    private TextView greeting;
    private RobotView mascot;
    private CelebrationView celebration;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        getWindow().addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);
        progress = new Progress(this);
        speech = new SpeechHelper(this);
        speech.setOnDone(() -> {
            if (mascot != null) {
                mascot.setTalking(false);
            }
        });
        buildChrome();
        showHome();
    }

    public Progress progress() {
        return progress;
    }

    public SpeechHelper speech() {
        return speech;
    }

    public Content.Lang lang() {
        return progress.getLang();
    }

    // ------------------------------------------------------------------
    // Shared chrome: bright background, playful header and content frame.
    // ------------------------------------------------------------------

    private void buildChrome() {
        rootFrame = new FrameLayout(this);
        rootFrame.setBackground(Util.gradient(0xFFFFF176, 0xFF80DEEA, 0));

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
        mascot.setBodyColor(Util.funColor(progress.points() / 50));
        mascot.setOnClickListener(v -> {
            mascot.setTalking(true);
            speech.speak(lang() == Content.Lang.MS
                    ? "Hai kawan! Jom belajar sama-sama!"
                    : "Hi friend! Let's learn together!", lang());
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

        addTile(grid, "\uD83D\uDDFA\uFE0F", tr("Laluan Belajar", "Learning Path"), 0xFF7C4DFF,
                () -> open(new LevelMapScreen(this)));
        addTile(grid, "\uD83D\uDD24", tr("Huruf ABC", "ABC Letters"), 0xFFFF5252,
                () -> open(new LearnScreen(this, Content.LETTERS, LearnScreen.Mode.LETTER)));
        addTile(grid, "\uD83D\uDD22", tr("Nombor 123", "Numbers 123"), 0xFF29B6F6,
                () -> open(new LearnScreen(this, Content.NUMBERS, LearnScreen.Mode.NUMBER)));
        addTile(grid, "\uD83D\uDCD6", tr("Perkataan", "Words"), 0xFF66BB6A,
                () -> open(new LearnScreen(this, Content.WORDS, LearnScreen.Mode.WORD)));
        addTile(grid, "\uD83D\uDCDD", tr("Ayat", "Sentences"), 0xFFAB47BC,
                () -> open(new SentenceScreen(this)));
        addTile(grid, "\uD83C\uDFA8", tr("Warna", "Colours"), 0xFFFFB300,
                () -> open(new LearnScreen(this, Content.COLOR_ITEMS, LearnScreen.Mode.WORD)
                        .withTitle("\uD83C\uDFA8 Colours", "\uD83C\uDFA8 Warna")));
        addTile(grid, "\uD83D\uDC41", tr("Tubuh Badan", "My Body"), 0xFFFF4081,
                () -> open(new LearnScreen(this, Content.BODY, LearnScreen.Mode.WORD)
                        .withTitle("\uD83D\uDC41 My Body", "\uD83D\uDC41 Tubuh Badan")));
        addTile(grid, "\uD83D\uDC3E", tr("Haiwan", "Animals"), 0xFF66BB6A,
                () -> open(new LearnScreen(this, Content.ANIMALS, LearnScreen.Mode.WORD)
                        .withTitle("\uD83D\uDC3E Animals", "\uD83D\uDC3E Haiwan")));
        addTile(grid, "\uD83C\uDF31", tr("Tumbuhan", "Plants"), 0xFF26A69A,
                () -> open(new LearnScreen(this, Content.PLANTS, LearnScreen.Mode.WORD)
                        .withTitle("\uD83C\uDF31 Plants", "\uD83C\uDF31 Tumbuh-tumbuhan")));
        addTile(grid, "\uD83C\uDF4E", tr("Buah-buahan", "Fruits"), 0xFFEC407A,
                () -> open(new LearnScreen(this, Content.FRUITS, LearnScreen.Mode.WORD)
                        .withTitle("\uD83C\uDF4E Fruits", "\uD83C\uDF4E Buah-buahan")));
        addTile(grid, "\uD83C\uDF89", tr("Benda Hidup", "Living Things"), 0xFF5C6BC0,
                () -> open(new LearnScreen(this, Content.LIVING, LearnScreen.Mode.WORD)
                        .withTitle("\uD83C\uDF89 Living Things", "\uD83C\uDF89 Benda Hidup")));
        addTile(grid, "\u0645", tr("Huruf Jawi", "Jawi Letters"), 0xFF00897B,
                () -> open(new LearnScreen(this, Content.JAWI, LearnScreen.Mode.WORD)
                        .withTitle("\u0645 Jawi", "\u0645 Jawi")));
        addTile(grid, "\u0639", tr("Huruf Arab", "Arabic Letters"), 0xFF6D4C41,
                () -> open(new LearnScreen(this, Content.ARABIC, LearnScreen.Mode.WORD)
                        .withTitle("\u0639 Arabic", "\u0639 Arab")));
        addTile(grid, "\uD83C\uDFA4", tr("Sebut & Menang", "Say & Win"), 0xFFFF9100,
                () -> open(new SpeakScreen(this)));
        addTile(grid, "\uD83C\uDFAF", tr("Kuiz", "Quiz"), 0xFFEC407A,
                () -> open(new QuizGame(this)));
        addTile(grid, "\uD83D\uDD22", tr("Kira Objek", "Count"), 0xFF26A69A,
                () -> open(new CountGame(this)));
        addTile(grid, "\uD83E\uDDE9", tr("Memori", "Memory"), 0xFF5C6BC0,
                () -> open(new MemoryGame(this)));
        addTile(grid, "\uD83C\uDFA8", tr("Mewarna", "Colouring"), 0xFFFF4081,
                () -> open(new ColoringScreen(this)));
        addTile(grid, "\uD83C\uDFC6", tr("Ganjaran", "Rewards"), 0xFFFFB300,
                () -> open(new RewardsScreen(this)));
    }

    private String tr(String ms, String en) {
        return lang() == Content.Lang.MS ? ms : en;
    }

    private void addTile(GridLayout grid, String emoji, String label, int color, Runnable action) {
        LinearLayout tile = new LinearLayout(this);
        tile.setOrientation(LinearLayout.VERTICAL);
        tile.setGravity(Gravity.CENTER);
        tile.setPadding(Util.dp(this, 8), Util.dp(this, 16), Util.dp(this, 8), Util.dp(this, 16));
        tile.setBackground(Util.rounded(color, Util.dp(this, 22)));
        tile.setElevation(Util.dp(this, 5));
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
        ObjectAnimator.ofFloat(v, "scaleX", 1f, 0.9f, 1.05f, 1f).setDuration(220).start();
        ObjectAnimator.ofFloat(v, "scaleY", 1f, 0.9f, 1.05f, 1f).setDuration(220).start();
    }

    /** Replaces the content frame with a screen view. */
    public void open(Screen screen) {
        content.removeAllViews();
        content.addView(screen.build(), new FrameLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT));
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
        if (mascot != null) {
            mouthSay(message);
        }
    }

    public void mouthSay(String message) {
        if (mascot == null) {
            return;
        }
        mascot.setTalking(true);
        speech.speak(message, lang());
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        speech.handleResult(requestCode, resultCode, data);
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        if (speech != null) {
            speech.shutdown();
        }
    }

    /** Every screen implements this so the host can mount it into the frame. */
    public interface Screen {
        View build();
    }
}
