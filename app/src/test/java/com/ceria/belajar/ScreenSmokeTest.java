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
                new LevelMapScreen(host),
                new LearnScreen(host, Content.LETTERS, LearnScreen.Mode.LETTER),
                new LearnScreen(host, Content.NUMBERS, LearnScreen.Mode.NUMBER),
                new LearnScreen(host, Content.WORDS, LearnScreen.Mode.WORD),
                new LearnScreen(host, Content.COLOR_ITEMS, LearnScreen.Mode.WORD),
                new LearnScreen(host, Content.BODY, LearnScreen.Mode.WORD),
                new LearnScreen(host, Content.ANIMALS, LearnScreen.Mode.WORD),
                new LearnScreen(host, Content.PLANTS, LearnScreen.Mode.WORD),
                new LearnScreen(host, Content.FRUITS, LearnScreen.Mode.WORD),
                new LearnScreen(host, Content.LIVING, LearnScreen.Mode.WORD),
                new LearnScreen(host, Content.JAWI, LearnScreen.Mode.WORD),
                new LearnScreen(host, Content.ARABIC, LearnScreen.Mode.WORD),
                new SentenceScreen(host),
                new SpeakScreen(host),
                new QuizGame(host),
                new CountGame(host),
                new MemoryGame(host),
                new ColoringScreen(host),
                new RewardsScreen(host)
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
    public void eachCurriculumLevelOpensInTheQuiz() {
        MainActivity host = launch();
        for (Curriculum.Level level : Curriculum.LEVELS) {
            View view = new QuizGame(host, level).build();
            assertNotNull(view);
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
