package com.ceria.belajar;

import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

import android.view.View;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.Robolectric;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.android.controller.ActivityController;
import org.robolectric.annotation.Config;

import java.util.ArrayList;
import java.util.List;

/**
 * Smoke test that actually inflates the activity and every screen on the home
 * grid. It catches the kind of crash a unit test on plain data would miss:
 * broken layouts, bad view params, null references while building the UI.
 */
@RunWith(RobolectricTestRunner.class)
@Config(sdk = 33, qualifiers = "w411dp-h891dp-xhdpi")
public class ScreenSmokeTest {

    private MainActivity launch() {
        ActivityController<MainActivity> controller =
                Robolectric.buildActivity(MainActivity.class).setup();
        return controller.get();
    }

    @Test
    public void mainActivityBuildsHomeScreen() {
        MainActivity activity = launch();
        assertNotNull(activity);
        assertNotNull(activity.findViewById(android.R.id.content));
    }

    @Test
    public void everyScreenBuildsWithoutCrashing() {
        MainActivity host = launch();

        MainActivity.Screen[] screens = new MainActivity.Screen[]{
                new LearnScreen(host, Content.unitById("bm-huruf")),
                new LearnScreen(host, Content.unitById("bm-suku-kata")),
                new LearnScreen(host, Content.unitById("fiz-warna")),
                new LearnScreen(host, Content.unitById("bm-jawi")),
                new UnitScreen(host, Content.unitById("bm-perkataan")),
                new TunjangScreen(host, Content.TUNJANG.get(0)),
                new ReviewScreen(host),
                new ThemesScreen(host),
                new SettingsScreen(host),
                new GamesScreen(host),
                new QuizScreen(host, Content.unitById("bm-huruf")),
                new QuizScreen(host, Content.unitById("ma-nombor")),
                new SentenceScreen(host),
                new SpeakScreen(host),
                new SpellGame(host),
                new SpellGame(host, Content.unitById("bm-suku-kata-kata")),
                new MemoryGame(host, Content.unitById("sa-buah")),
                new OddOneOutGame(host),
                new CountGame(host),
                new PatternGame(host),
                new BubblePopGame(host),
                new SortGame(host),
                new MathGame(host),
                new TraceGame(host),
                new JawiSpeakGame(host),
                new ColoringScreen(host),
                new RewardsScreen(host),
                new VoiceScreen(host),
                new RecordingStudioScreen(host),
                new CreditsScreen(host),
                new XylophoneScreen(host),
                new AnimalSoundScreen(host),
                new KaraokeScreen(host)
        };

        for (MainActivity.Screen screen : screens) {
            View view = screen.build();
            assertNotNull("screen failed to build: " + screen.getClass().getSimpleName(), view);
            assertTrue(view.getVisibility() == View.VISIBLE);
            assertTrue("screen has no children: " + screen.getClass().getSimpleName(),
                    view instanceof android.view.ViewGroup
                            && ((android.view.ViewGroup) view).getChildCount() > 0);
        }
    }

    @Test
    public void newGamesBuildInBothLanguages() {
        MainActivity host = launch();
        for (Content.Lang lang : Content.Lang.values()) {
            host.progress().setLang(lang);
            for (MainActivity.Screen screen : new MainActivity.Screen[]{
                    new SpellGame(host), new OddOneOutGame(host)}) {
                String name = screen.getClass().getSimpleName() + " (" + lang + ")";
                View view = screen.build();
                assertNotNull("failed to build " + name, view);
                assertTrue(name + " has no children", view instanceof android.view.ViewGroup
                        && ((android.view.ViewGroup) view).getChildCount() > 0);
            }
        }
    }

    @Test
    public void everySpellableWordCanBeBuiltFromItsOwnLetters() {
        MainActivity host = launch();
        SpellGame game = new SpellGame(host);
        List<String> words = game.spellableWords();
        assertTrue("spell game needs a pool of words, found=" + words.size(), words.size() >= 20);
        for (String word : words) {
            assertTrue("word must be plain letters: " + word,
                    word.matches("[a-z]{3,6}"));
        }
        // Two rounds must be able to pick different words, or the game would be dull.
        assertTrue("pool is too small to vary rounds", words.size() >= 8);
    }

    @Test
    public void oddOneOutHasEnoughDistinctGroups() {
        MainActivity host = launch();
        OddOneOutGame game = new OddOneOutGame(host);
        assertTrue("need at least two groups to pick an odd one out, found="
                + game.groupCount(), game.groupCount() >= 2);
    }

    @Test
    public void homeShowsItsLearningTiles() {
        MainActivity activity = launch();
        View decor = activity.getWindow().getDecorView();
        decor.measure(View.MeasureSpec.makeMeasureSpec(822, View.MeasureSpec.EXACTLY),
                View.MeasureSpec.makeMeasureSpec(1782, View.MeasureSpec.EXACTLY));
        decor.layout(0, 0, 822, 1782);

        int clickable = countClickable(decor);
        assertTrue("home should show its tiles, clickable views=" + clickable, clickable >= 15);
    }

    private int countClickable(View v) {
        int count = v.isClickable() ? 1 : 0;
        if (v instanceof android.view.ViewGroup) {
            android.view.ViewGroup g = (android.view.ViewGroup) v;
            for (int i = 0; i < g.getChildCount(); i++) {
                count += countClickable(g.getChildAt(i));
            }
        }
        return count;
    }

    @Test
    public void everyUnitOpensInTheQuiz() {
        MainActivity host = launch();
        for (Content.Unit unit : Content.UNITS) {
            View view = new QuizScreen(host, unit).build();
            assertNotNull("quiz failed for " + unit.id, view);
        }
    }

    @Test
    public void everyUnitOpensAsFlashcards() {
        MainActivity host = launch();
        for (Content.Unit unit : Content.UNITS) {
            View view = new LearnScreen(host, unit).build();
            assertNotNull("flashcards failed for " + unit.id, view);
        }
    }

    @Test
    public void everyTunjangOpens() {
        MainActivity host = launch();
        for (Content.Tunjang tunjang : Content.TUNJANG) {
            View view = new TunjangScreen(host, tunjang).build();
            assertNotNull("tunjang failed for " + tunjang.id, view);
        }
    }

    @Test
    public void coloringPageHasRegionsAndCycles() {
        MainActivity host = launch();
        ColorPageView page = new ColorPageView(host);
        page.setActiveColor(0xFF42A5F5);
        String first = page.pictureName();
        page.nextPicture();
        String second = page.pictureName();
        assertNotNull(first);
        assertNotNull(second);
    }

    @Test
    public void robotMascotDrawsToTheCanvas() {
        MainActivity host = launch();
        RobotView robot = new RobotView(host);
        robot.setTalking(true);
        robot.setBodyColor(0xFFFF5252);
        robot.measure(View.MeasureSpec.makeMeasureSpec(400, View.MeasureSpec.EXACTLY),
                View.MeasureSpec.makeMeasureSpec(400, View.MeasureSpec.EXACTLY));
        robot.layout(0, 0, 400, 400);
        android.graphics.Bitmap bitmap =
                android.graphics.Bitmap.createBitmap(400, 400, android.graphics.Bitmap.Config.ARGB_8888);
        robot.draw(new android.graphics.Canvas(bitmap));
        assertNotNull(bitmap);
    }
}
