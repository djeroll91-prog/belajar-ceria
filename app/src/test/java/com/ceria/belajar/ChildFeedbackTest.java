package com.ceria.belajar;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.Robolectric;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.android.controller.ActivityController;
import org.robolectric.annotation.Config;
import org.robolectric.annotation.GraphicsMode;

import java.util.ArrayList;
import java.util.List;

/**
 * Three things a parent reported, each pinned here so they cannot come back:
 *
 *  1. background music kept looping during games and made them confusing
 *  2. the ABC quiz showed the raw "A|🍎" symbol instead of the letter
 *  3. the letter screen said the picture's name ("Apple") instead of the letter
 */
@RunWith(RobolectricTestRunner.class)
@GraphicsMode(GraphicsMode.Mode.NATIVE)
@Config(sdk = 33, qualifiers = "w411dp-h891dp-xhdpi")
public class ChildFeedbackTest {

    private MainActivity launch() {
        ActivityController<MainActivity> c = Robolectric.buildActivity(MainActivity.class).setup();
        return c.get();
    }

    private void walk(View v, List<TextView> out) {
        if (v instanceof TextView) {
            out.add((TextView) v);
        }
        if (v instanceof ViewGroup) {
            ViewGroup g = (ViewGroup) v;
            for (int i = 0; i < g.getChildCount(); i++) {
                walk(g.getChildAt(i), out);
            }
        }
    }

    private String textsOf(View root) {
        List<TextView> texts = new ArrayList<>();
        walk(root, texts);
        StringBuilder sb = new StringBuilder();
        for (TextView t : texts) {
            if (t.getText() != null) {
                sb.append(t.getText()).append('\n');
            }
        }
        return sb.toString();
    }

    // ------------------------------------------------------------------
    // 1. Music must not loop under a game
    // ------------------------------------------------------------------

    @Test
    public void gamesAreSilentSoChildrenCanThink() {
        MainActivity host = launch();
        MainActivity.Screen[] games = {
                new QuizGame(host), new SpellGame(host), new OddOneOutGame(host),
                new CountGame(host), new MemoryGame(host), new PatternGame(host),
                new BubblePopGame(host), new SortGame(host), new MathGame(host),
                new TraceGame(host), new JawiSpeakGame(host)};
        for (MainActivity.Screen game : games) {
            host.open(game);
            assertEquals(game.getClass().getSimpleName() + " should have no background music",
                    MusicPlayer.SILENT, host.music().currentTrack());
        }
    }

    @Test
    public void learningScreensStillHaveATune() {
        MainActivity host = launch();
        host.open(new LearnScreen(host, Content.LETTERS, LearnScreen.Mode.LETTER));
        assertNotEquals("a learning screen should not be silent",
                MusicPlayer.SILENT, host.music().currentTrack());
        host.showHome();
        assertEquals("home should have its own tune", MusicPlayer.HOME,
                host.music().currentTrack());
    }

    @Test
    public void musicAndSoundEffectsHaveSeparateSwitches() {
        MainActivity host = launch();
        // Turning the tune off must not take the tap and reward sounds with it.
        host.setMusicOn(false);
        assertFalse(host.isMusicOn());
        assertTrue("sound effects must survive the music being switched off",
                host.isSoundOn());
        assertTrue("switching music off must stop playback", !host.music().isEnabled());

        host.setMusicOn(true);
        assertTrue(host.isMusicOn());
        assertTrue(host.music().isEnabled());
    }

    // ------------------------------------------------------------------
    // 2. Letters must render as a single glyph, never "A|🍎"
    // ------------------------------------------------------------------

    @Test
    public void glyphOfUnwrapsLetterSymbols() {
        Content.Item a = Content.LETTERS.get(0);
        assertEquals("A", Content.glyphOf(a));
        // The raw symbol still carries the picture for the picture slot.
        assertTrue(a.symbol.contains("|"));
        assertEquals(Content.letterEmoji(a), a.symbol.substring(a.symbol.indexOf('|') + 1));
    }

    @Test
    public void quizNeverPrintsAPipeInAnOption() {
        MainActivity host = launch();
        // The default quiz draws from LETTERS, NUMBERS and WORDS, which is where
        // the raw "A|🍎" symbol used to leak onto the tiles.
        for (int i = 0; i < 12; i++) {
            QuizGame quiz = new QuizGame(host);
            String shown = textsOf(quiz.build());
            assertFalse("a letter tile leaked its raw symbol: " + shown,
                    shown.contains("|"));
        }
    }

    @Test
    public void karaokeShowsLettersNotPictures() {
        MainActivity host = launch();
        KaraokeScreen screen = new KaraokeScreen(host);
        View root = screen.build();
        String shown = textsOf(root);
        assertFalse("karaoke leaked a raw letter symbol", shown.contains("|"));
    }

    // ------------------------------------------------------------------
    // 3. The letter screen must say the letter, not the picture's name
    // ------------------------------------------------------------------

    @Test
    public void letterNamesAreSpelledTheMalaysianWay() {
        // Malaysia follows the English letter names (DBP: A = "ei", B = "bi"),
        // not the Indonesian Dutch ones ("be", "ce").
        assertEquals("ei", SpokenText.forEngine(Content.Lang.MS, "A"));
        assertEquals("bi", SpokenText.forEngine(Content.Lang.MS, "B"));
        assertEquals("si", SpokenText.forEngine(Content.Lang.MS, "C"));
        assertEquals("di", SpokenText.forEngine(Content.Lang.MS, "D"));
        assertEquals("ji", SpokenText.forEngine(Content.Lang.MS, "G"));
        assertEquals("dabliu", SpokenText.forEngine(Content.Lang.MS, "W"));
        assertEquals("zet", SpokenText.forEngine(Content.Lang.MS, "Z"));
    }

    @Test
    public void everyLetterHasAPronunciationInBothLanguages() {
        // Malay "O" is the one letter whose name is the letter itself; every
        // other name is a distinct word, so it is listed here rather than
        // weakening the rule for all 26.
        java.util.Set<String> nameIsTheLetter = new java.util.HashSet<>(
                java.util.Arrays.asList("MS O"));

        for (char c = 'A'; c <= 'Z'; c++) {
            String letter = String.valueOf(c);
            for (Content.Lang lang : Content.Lang.values()) {
                String label = lang + " " + letter;
                String spoken = SpokenText.forEngine(lang, letter);
                assertNotNull(label, spoken);
                // A bare single letter is read as an initialism or in the wrong
                // language by some engines, so the spoken form must be a real
                // lowercase word the engine will say as one sound.
                assertTrue("pronunciation for " + label + " is not a word: " + spoken,
                        spoken.matches("[a-z ]+"));
                if (!nameIsTheLetter.contains(label)) {
                    assertFalse("no pronunciation mapped for " + label,
                            spoken.equalsIgnoreCase(letter));
                }
            }
        }
    }

    @Test
    public void abcSongCoversTheWholeAlphabet() {
        MainActivity host = launch();
        KaraokeScreen screen = new KaraokeScreen(host);
        // The shadow player reports completion instantly, so the song would race
        // through all 26 cards before anything could be inspected. Hold each
        // callback back so the strip can be checked while the song is still going.
        java.util.List<Runnable> held = new ArrayList<>();
        screen.setSpeakerForTest((word, done) -> held.add(done));
        screen.setStallTimeoutForTest(60_000);

        View root = screen.build();
        host.setContentView(root);
        View abc = findClickableWithText(root, "ABC");
        assertNotNull("the ABC song button is missing", abc);
        abc.performClick();
        org.robolectric.shadows.ShadowLooper.idleMainLooper();

        // Let each letter finish in turn, exactly as the audio would.
        for (int i = 0; i < Content.LETTERS.size(); i++) {
            assertTrue("the song stopped after " + i + " letters",
                    held.size() > i);
            held.get(i).run();
            org.robolectric.shadows.ShadowLooper.idleMainLooper();
        }

        String shown = textsOf(root);
        for (char c = 'A'; c <= 'Z'; c++) {
            assertTrue("the ABC song never reached " + c, shown.contains(String.valueOf(c)));
        }
    }

    @Test
    public void abcSongWaitsForEachLetterToFinish() {
        MainActivity host = launch();
        KaraokeScreen screen = new KaraokeScreen(host);
        java.util.List<String> spoken = new ArrayList<>();
        java.util.List<Runnable> held = new ArrayList<>();
        screen.setSpeakerForTest((word, done) -> {
            spoken.add(word);
            held.add(done);
        });
        screen.setStallTimeoutForTest(60_000);

        View root = screen.build();
        host.setContentView(root);
        findClickableWithText(root, "ABC").performClick();
        org.robolectric.shadows.ShadowLooper.idleMainLooper();

        // The old code advanced on a fixed 1.2 s timer, so it asked for every
        // letter at once and clipped the ones longer than the tick. Only one
        // letter may be in flight until its audio reports that it has ended.
        assertEquals("the song raced ahead of the audio", 1, spoken.size());
        assertEquals("the first letter must be spoken by name", "A", spoken.get(0));

        held.get(0).run();
        org.robolectric.shadows.ShadowLooper.idleMainLooper();
        assertEquals("the second letter did not start after the first ended",
                2, spoken.size());
        assertEquals("B", spoken.get(1));
    }

    @Test
    public void abcSongNeverStallsWhenAudioNeverReportsBack() {
        MainActivity host = launch();
        KaraokeScreen screen = new KaraokeScreen(host);
        // A device whose engine never calls back must not freeze the song: the
        // stall timeout has to carry it on.
        screen.setSpeakerForTest((word, done) -> { /* never finishes */ });
        screen.setStallTimeoutForTest(50);

        View root = screen.build();
        host.setContentView(root);
        findClickableWithText(root, "ABC").performClick();
        org.robolectric.shadows.ShadowLooper.runUiThreadTasksIncludingDelayedTasks();

        // Every letter was reached even though no audio ever completed.
        String shown = textsOf(root);
        for (char c = 'A'; c <= 'Z'; c++) {
            assertTrue("the song stalled before " + c, shown.contains(String.valueOf(c)));
        }
    }

    @Test
    public void stoppingTheSongDoesNotRestartIt() {
        MainActivity host = launch();
        KaraokeScreen screen = new KaraokeScreen(host);
        java.util.List<Runnable> held = new ArrayList<>();
        screen.setSpeakerForTest((word, done) -> held.add(done));
        screen.setStallTimeoutForTest(60_000);

        View root = screen.build();
        host.setContentView(root);
        findClickableWithText(root, "ABC").performClick();
        org.robolectric.shadows.ShadowLooper.idleMainLooper();
        assertEquals(1, held.size());

        // Press Stop, then let the in-flight audio report completion. A retired
        // card must be ignored, so the song stays stopped.
        View stop = findClickableWithText(root, "Stop");
        if (stop == null) {
            stop = findClickableWithText(root, "Berhenti");
        }
        assertNotNull("the Stop button is missing", stop);
        stop.performClick();
        // The button runs its action after a short press animation, so let that
        // queued work run before pretending the audio reports back.
        org.robolectric.shadows.ShadowLooper.runUiThreadTasksIncludingDelayedTasks();

        // Now let the in-flight audio report completion. A retired card must be
        // ignored, so the song stays stopped.
        held.get(0).run();
        org.robolectric.shadows.ShadowLooper.runUiThreadTasksIncludingDelayedTasks();
        assertEquals("the song restarted after Stop was pressed", 1, held.size());
    }

    @Test
    public void everySongPhraseHasAudioThatEnds() {
        // The karaoke now chains phrases on their completion callback, so a phrase
        // with no bundled clip and no working engine would leave the child in
        // silence. Letters are the known gap: they rely on the engine, which is
        // why the song also has a stall timeout. This pins the clips that do ship.
        MainActivity host = launch();
        for (int i = 0; i < 10; i++) {
            Content.Item number = Content.NUMBERS.get(i);
            int id = host.getResources().getIdentifier(
                    "clip_" + PhraseCatalog.key(Content.Lang.MS, number.spokenText(Content.Lang.MS)),
                    "raw", host.getPackageName());
            assertTrue("the number song has no clip for " + number.spokenText(Content.Lang.MS),
                    id != 0);
        }
    }

    @Test
    public void abcScreenSaysTheLetterNotThePictureName() {
        MainActivity host = launch();
        LearnScreen screen = new LearnScreen(host, Content.LETTERS, LearnScreen.Mode.LETTER);
        screen.build();
        // Walk the whole alphabet: the voice must be told "A", "B", "C", ...
        // never "Apple", "Ball", "Cat".
        for (Content.Item item : Content.LETTERS) {
            String spoken = screen.spokenName();
            assertEquals("letter mode must speak the glyph, not the word",
                    Content.letterGlyph(item), spoken);
            assertFalse("the picture's name leaked into the voice",
                    spoken.equals(item.name(Content.Lang.MS))
                            || spoken.equals(item.name(Content.Lang.EN)));
            screen.nextForTest();
        }
    }

    @Test
    public void wordScreenStillSaysTheWholeWord() {
        MainActivity host = launch();
        LearnScreen screen = new LearnScreen(host, Content.WORDS, LearnScreen.Mode.WORD);
        screen.build();
        // Reading practice is the opposite case: there the word itself is right.
        for (Content.Item item : Content.WORDS) {
            assertEquals(item.word, screen.spokenName());
            screen.nextForTest();
        }
    }

    @Test
    public void numberScreenSaysTheNumberAsAWord() {
        MainActivity host = launch();
        LearnScreen screen = new LearnScreen(host, Content.NUMBERS, LearnScreen.Mode.NUMBER);
        screen.build();
        // "satu" is recognised far better than the digit "1", and the emoji in
        // symbol must never reach the voice.
        String first = screen.spokenName();
        assertEquals(Content.NUMBERS.get(0).name(Content.Lang.MS), first);
        assertFalse("an emoji reached the voice", first.matches(".*[\\uD800-\\uDFFF].*"));
    }

    /** Finds the nearest clickable ancestor of a TextView containing some text. */
    private View findClickableWithText(View root, String needle) {
        List<TextView> texts = new ArrayList<>();
        walk(root, texts);
        for (TextView t : texts) {
            String cs = t.getText() == null ? "" : t.getText().toString();
            if (cs.contains(needle)) {
                View v = t;
                while (v != null && !v.isClickable()) {
                    v = v.getParent() instanceof View ? (View) v.getParent() : null;
                }
                if (v != null) {
                    return v;
                }
            }
        }
        return null;
    }
}
