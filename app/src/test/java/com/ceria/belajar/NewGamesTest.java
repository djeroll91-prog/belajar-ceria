package com.ceria.belajar;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

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

import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Plays the newer games for real: opens the screen, finds the control a child
 * would press, presses it, and checks the game reacts. A screen that merely
 * renders is not proof that the game works, so these tests drive the actual
 * click handlers rather than asserting on layout alone.
 */
@RunWith(RobolectricTestRunner.class)
@Config(sdk = 33, qualifiers = "w411dp-h891dp-xhdpi")
public class NewGamesTest {

    private MainActivity launch() {
        ActivityController<MainActivity> c = Robolectric.buildActivity(MainActivity.class).setup();
        return c.get();
    }

    private void layout(View v, int w, int h) {
        v.measure(View.MeasureSpec.makeMeasureSpec(w, View.MeasureSpec.EXACTLY),
                View.MeasureSpec.makeMeasureSpec(h, View.MeasureSpec.EXACTLY));
        v.layout(0, 0, w, h);
    }

    private void walk(View v, List<View> out) {
        out.add(v);
        if (v instanceof ViewGroup) {
            ViewGroup g = (ViewGroup) v;
            for (int i = 0; i < g.getChildCount(); i++) {
                walk(g.getChildAt(i), out);
            }
        }
    }

    private List<View> allViews(View root) {
        List<View> out = new ArrayList<>();
        walk(root, out);
        return out;
    }

    private String textOf(View v) {
        if (v instanceof TextView) {
            CharSequence t = ((TextView) v).getText();
            return t == null ? "" : t.toString();
        }
        return "";
    }

    /** The feedback line at the bottom of a game, where results are announced. */
    private String allText(View root) {
        StringBuilder sb = new StringBuilder();
        for (View v : allViews(root)) {
            sb.append(textOf(v)).append('\n');
        }
        return sb.toString();
    }

    @Test
    public void mathGameAcceptsTheCorrectSumAndRefusesAWrongOne() {
        MainActivity host = launch();
        MathGame game = new MathGame(host);
        host.open(game);
        View decor = host.getWindow().getDecorView();
        layout(decor, 822, 1782);

        Pattern sum = Pattern.compile("(\\d+)\\s*([+\\u2212])\\s*(\\d+)\\s*=");
        Matcher m = sum.matcher(allText(decor));
        assertTrue("the game must show a sum to solve", m.find());
        int a = Integer.parseInt(m.group(1));
        int b = Integer.parseInt(m.group(3));
        int answer = "+".equals(m.group(2)) ? a + b : a - b;

        View correctTile = null;
        View wrongTile = null;
        for (View v : allViews(decor)) {
            if (!v.isClickable()) {
                continue;
            }
            String label = textOf(v);
            if (!label.matches("\\d+")) {
                continue;
            }
            if (Integer.parseInt(label) == answer) {
                correctTile = v;
            } else if (wrongTile == null) {
                wrongTile = v;
            }
        }
        assertNotNull("there must be a tile holding the right answer", correctTile);
        assertNotNull("there must be a wrong tile to try", wrongTile);

        // A wrong tile is rejected and does not end the round.
        wrongTile.performClick();
        assertTrue("a wrong answer should not be praised: " + allText(decor),
                !allText(decor).contains("Correct!"));

        correctTile.performClick();
        assertTrue("the right answer must be praised: " + allText(decor),
                allText(decor).contains("Correct!") || allText(decor).contains("Betul!"));
    }

    @Test
    public void patternGameOnlyAcceptsThePieceThatFitsThePattern() {
        MainActivity host = launch();
        PatternGame game = new PatternGame(host);
        host.open(game);
        View decor = host.getWindow().getDecorView();
        layout(decor, 822, 1782);

        View gap = decor.findViewWithTag("gap");
        assertNotNull("the pattern must contain one empty slot", gap);
        ViewGroup row = (ViewGroup) gap.getParent();

        List<String> shown = new ArrayList<>();
        for (int i = 0; i < row.getChildCount(); i++) {
            shown.add(textOf(((ViewGroup) row.getChildAt(i)).getChildAt(0)));
        }
        assertEquals("the pattern shows five slots", 5, shown.size());
        assertEquals("the last slot is the empty one", "?", shown.get(4));

        // The pattern repeats every `distinct` pictures; work out which picture
        // belongs in slot 5 from the ones already on screen.
        int distinct = shown.get(0).equals(shown.get(2)) ? 2 : 3;
        String expected = shown.get(4 % distinct);

        List<View> options = new ArrayList<>();
        for (View v : allViews(decor)) {
            if (v.isClickable() && !isInside(v, row) && shown.contains(textOf(v))
                    && !textOf(v).isEmpty()) {
                options.add(v);
            }
        }
        assertEquals("one tile per picture in the pattern", distinct, options.size());

        View correct = null;
        View wrong = null;
        for (View v : options) {
            if (textOf(v).equals(expected)) {
                correct = v;
            } else {
                wrong = v;
            }
        }
        assertNotNull("the missing piece must be offered", correct);
        assertNotNull(wrong);

        wrong.performClick();
        assertTrue("a piece that breaks the pattern must be refused: " + allText(decor),
                !allText(decor).contains("Yes! That is right"));

        correct.performClick();
        assertTrue("the missing piece must be accepted: " + allText(decor),
                allText(decor).contains("Yes! That is right")
                        || allText(decor).contains("Ya! Betul"));
    }

    private boolean isInside(View child, ViewGroup ancestor) {
        View parent = child;
        while (parent != null) {
            if (parent == ancestor) {
                return true;
            }
            parent = parent.getParent() instanceof View ? (View) parent.getParent() : null;
        }
        return false;
    }

    @Test
    public void sortGameSendsAnAnimalToTheAnimalBasket() {
        MainActivity host = launch();
        SortGame game = new SortGame(host);
        host.open(game);
        View decor = host.getWindow().getDecorView();
        layout(decor, 822, 1782);

        // The baskets are the clickable groups that carry a basket label.
        List<View> baskets = new ArrayList<>();
        for (View v : allViews(decor)) {
            if (!v.isClickable() || !(v instanceof LinearLayout)) {
                continue;
            }
            LinearLayout group = (LinearLayout) v;
            if (group.getChildCount() > 0
                    && textOf(group.getChildAt(0)).contains("\uD83E\uDDFA")) {
                baskets.add(v);
            }
        }
        assertEquals("two baskets to sort into", 2, baskets.size());

        View animalBasket = textOf(((LinearLayout) baskets.get(0)).getChildAt(0))
                .contains("Animals") || textOf(((LinearLayout) baskets.get(0)).getChildAt(0))
                .contains("Haiwan") ? baskets.get(0) : baskets.get(1);

        // Pick an animal from the tray and drop it into the animal basket.
        View animalTile = null;
        for (View v : allViews(decor)) {
            if (v.isClickable() && Content.ANIMALS.stream()
                    .anyMatch(item -> item.symbol.equals(textOf(v)))) {
                animalTile = v;
                break;
            }
        }
        assertNotNull("the tray must hold at least one animal", animalTile);

        animalTile.performClick();
        animalBasket.performClick();
        assertTrue("an animal belongs in the animal basket: " + allText(decor),
                allText(decor).contains("Correct!") || allText(decor).contains("Betul!"));
    }

    @Test
    public void traceGamePraisesOnlyAfterTheOutlineIsCovered() {
        MainActivity host = launch();
        TraceGame game = new TraceGame(host, TraceGame.Mode.LETTER);
        host.open(game);
        View decor = host.getWindow().getDecorView();
        layout(decor, 822, 1782);

        // A single tap covers almost none of the letter, so it must not pass.
        View pad = null;
        for (View v : allViews(decor)) {
            if (v.getClass().getSimpleName().equals("TracePad")) {
                pad = v;
            }
        }
        assertNotNull("the tracing surface must exist", pad);
        assertTrue("the pad needs a real size to trace in",
                pad.getWidth() > 100 && pad.getHeight() > 100);

        long down = android.os.SystemClock.uptimeMillis();
        android.view.MotionEvent tap = android.view.MotionEvent.obtain(
                down, down, android.view.MotionEvent.ACTION_DOWN, 5f, 5f, 0);
        pad.dispatchTouchEvent(tap);
        android.view.MotionEvent up = android.view.MotionEvent.obtain(
                down, down, android.view.MotionEvent.ACTION_UP, 5f, 5f, 0);
        pad.dispatchTouchEvent(up);

        assertTrue("one stray tap is not a traced letter: " + allText(decor),
                !allText(decor).contains("Beautiful writing"));
    }
}
