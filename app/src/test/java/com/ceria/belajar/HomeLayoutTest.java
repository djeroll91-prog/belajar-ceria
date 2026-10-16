package com.ceria.belajar;

import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.assertEquals;

import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Color;
import android.view.View;
import android.view.ViewGroup;
import android.widget.LinearLayout;
import android.widget.TextView;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.Robolectric;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.android.controller.ActivityController;
import org.robolectric.annotation.Config;
import org.robolectric.annotation.GraphicsMode;

import java.io.FileOutputStream;
import java.util.ArrayList;
import java.util.List;

/**
 * Renders the home screen at a phone size so the tiles can be checked for a real
 * on-screen size, and writes a PNG for a visual look. A tile that measures to
 * zero would be invisible to a child even though the code "works".
 */
@RunWith(RobolectricTestRunner.class)
@GraphicsMode(GraphicsMode.Mode.NATIVE)
@Config(sdk = 33, qualifiers = "w411dp-h891dp-xhdpi")
public class HomeLayoutTest {

    private MainActivity launch() {
        ActivityController<MainActivity> c = Robolectric.buildActivity(MainActivity.class).setup();
        return c.get();
    }

    private void layout(View v, int w, int h) {
        v.measure(View.MeasureSpec.makeMeasureSpec(w, View.MeasureSpec.EXACTLY),
                View.MeasureSpec.makeMeasureSpec(h, View.MeasureSpec.EXACTLY));
        v.layout(0, 0, w, h);
    }

    /** Every view under the root, in depth-first order. */
    private List<View> allViews(View root) {
        List<View> out = new ArrayList<>();
        walk(root, out, 0);
        return out;
    }

    private void walk(View v, List<View> out, int depth) {
        out.add(v);
        if (v instanceof ViewGroup) {
            ViewGroup g = (ViewGroup) v;
            for (int i = 0; i < g.getChildCount(); i++) {
                walk(g.getChildAt(i), out, depth + 1);
            }
        }
    }

    /** Only the views a finger can actually press. */
    private List<View> clickables(View root) {
        List<View> out = new ArrayList<>();
        for (View v : allViews(root)) {
            if (v.isClickable()) {
                out.add(v);
            }
        }
        return out;
    }

    @Test
    public void everyScreenRendersVisibleContent() throws Exception {
        MainActivity host = launch();
        MainActivity.Screen[] screens = new MainActivity.Screen[]{
                new LevelMapScreen(host),
                new LearnScreen(host, Content.LETTERS, LearnScreen.Mode.LETTER),
                new LearnScreen(host, Content.NUMBERS, LearnScreen.Mode.NUMBER),
                new LearnScreen(host, Content.WORDS, LearnScreen.Mode.WORD),
                new LearnScreen(host, Content.JAWI, LearnScreen.Mode.WORD),
                new SentenceScreen(host),
                new SpeakScreen(host),
                new QuizGame(host),
                new SpellGame(host),
                new OddOneOutGame(host),
                new CountGame(host),
                new MemoryGame(host),
                new ColoringScreen(host),
                new RewardsScreen(host),
                new VoiceScreen(host),
                new RecordingStudioScreen(host),
                new CreditsScreen(host)
        };

        for (MainActivity.Screen screen : screens) {
            String name = screen.getClass().getSimpleName();
            host.open(screen);
            View decor = host.getWindow().getDecorView();
            layout(decor, 822, 1782);

            Bitmap bmp = Bitmap.createBitmap(822, 1782, Bitmap.Config.ARGB_8888);
            Canvas canvas = new Canvas(bmp);
            canvas.drawColor(Color.WHITE);
            decor.draw(canvas);

            java.util.HashSet<Integer> colors = new java.util.HashSet<>();
            for (int y = 0; y < bmp.getHeight(); y += 9) {
                for (int x = 0; x < bmp.getWidth(); x += 9) {
                    colors.add(bmp.getPixel(x, y));
                }
            }
            assertTrue(name + " renders blank, distinct colours=" + colors.size(),
                    colors.size() > 20);

            int clickable = clickables(decor).size();
            assertTrue(name + " has nothing to press, clickable=" + clickable, clickable >= 2);
        }
    }

    @Test
    public void dumpHomeTree() {
        MainActivity a = launch();
        View decor = a.getWindow().getDecorView();
        layout(decor, 411 * 2, 891 * 2);

        StringBuilder sb = new StringBuilder();
        for (View v : allViews(decor)) {
            String text = "";
            if (v instanceof TextView) {
                CharSequence t = ((TextView) v).getText();
                text = t == null ? "" : t.toString();
            }
            sb.append(v.getClass().getSimpleName())
                    .append(" ").append(v.getMeasuredWidth()).append("x").append(v.getMeasuredHeight())
                    .append(v.isClickable() ? " [clickable]" : "")
                    .append(text.isEmpty() ? "" : " \"" + text + "\"")
                    .append("\n");
        }
        System.out.println("=== HOME TREE ===");
        System.out.println(sb);
        System.out.println("=== END TREE ===");
    }

    @Test
    public void homeTilesHaveRealSize() {
        MainActivity a = launch();
        View decor = a.getWindow().getDecorView();
        int w = 411 * 2, h = 891 * 2; // xhdpi pixels
        layout(decor, w, h);

        List<View> clickable = clickables(decor);
        assertTrue("home should have clickable tiles", clickable.size() >= 15);

        int visible = 0;
        for (View v : clickable) {
            if (v.getMeasuredWidth() > 0 && v.getMeasuredHeight() > 0) {
                visible++;
            }
        }
        assertTrue("most clickable tiles should have size, visible=" + visible
                + " of " + clickable.size(), visible >= clickable.size() - 2);
    }

    @Test
    public void homeTilesHaveLabels() {
        MainActivity a = launch();
        View decor = a.getWindow().getDecorView();
        layout(decor, 411 * 2, 891 * 2);

        int labels = 0;
        for (View v : allViews(decor)) {
            if (v instanceof TextView) {
                CharSequence t = ((TextView) v).getText();
                if (t != null && t.length() > 0) {
                    labels++;
                }
            }
        }
        assertTrue("home should show text labels, found=" + labels, labels >= 10);
    }

    @Test
    public void homeScreenRendersNonBlankImage() throws Exception {
        MainActivity a = launch();
        View decor = a.getWindow().getDecorView();
        layout(decor, 411 * 2, 891 * 2);

        Bitmap bmp = Bitmap.createBitmap(411 * 2, 891 * 2, Bitmap.Config.ARGB_8888);
        Canvas canvas = new Canvas(bmp);
        canvas.drawColor(Color.WHITE);
        decor.draw(canvas);

        // Count distinct colours. A blank screen has almost none.
        java.util.HashSet<Integer> colors = new java.util.HashSet<>();
        for (int y = 0; y < bmp.getHeight(); y += 7) {
            for (int x = 0; x < bmp.getWidth(); x += 7) {
                colors.add(bmp.getPixel(x, y));
            }
        }
        FileOutputStream out = new FileOutputStream("/tmp/home.png");
        bmp.compress(Bitmap.CompressFormat.PNG, 100, out);
        out.close();

        assertTrue("home screen looks blank, distinct colours=" + colors.size(),
                colors.size() > 20);
    }

    @Test
    public void newGameBoardsHaveRealTiles() {
        MainActivity host = launch();
        for (int i = 0; i < 2; i++) {
            MainActivity.Screen screen = i == 0 ? new SpellGame(host) : new OddOneOutGame(host);
            host.open(screen);
            View decor = host.getWindow().getDecorView();
            layout(decor, 822, 1782);

            int sized = 0, labelled = 0;
            for (View v : allViews(decor)) {
                if (v.isClickable() && v.getMeasuredWidth() > 40 && v.getMeasuredHeight() > 40) {
                    sized++;
                }
                if (v instanceof TextView) {
                    CharSequence t = ((TextView) v).getText();
                    if (t != null && t.length() > 0) {
                        labelled++;
                    }
                }
            }
            String name = screen.getClass().getSimpleName();
            assertTrue(name + " needs tappable tiles, found=" + sized, sized >= 4);
            assertTrue(name + " needs on-screen text, found=" + labelled, labelled >= 3);
        }
    }

    @Test
    public void malayLanguagePillShowsMalaysiaFlag() {
        MainActivity a = launch();
        a.progress().setLang(Content.Lang.MS);
        a.updateHeader();
        TextView pill = findLangPill(a);
        assertNotNull(pill);
        String text = pill.getText().toString();
        // Regional indicator "M" (U+1F1F2) then "Y" (U+1F1FE) = Malaysia.
        assertTrue("expected Malaysia flag for Bahasa Melayu but saw: " + text,
                text.contains("\uD83C\uDDF2\uD83C\uDDFE"));
        assertTrue("expected BM label, saw: " + text, text.contains("BM"));
    }

    private TextView findLangPill(MainActivity a) {
        for (View v : allViews(a.getWindow().getDecorView())) {
            if (v instanceof TextView) {
                CharSequence t = ((TextView) v).getText();
                if (t != null && (t.toString().contains("BM") || t.toString().contains("EN"))) {
                    return (TextView) v;
                }
            }
        }
        return null;
    }
}
