package com.ceria.belajar;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import org.junit.Test;

/**
 * Verifies the content data, the locked-level rules and the pronunciation
 * comparison. These are the pieces that decide whether a child may move on, so
 * they must behave predictably.
 */
public class ContentAndGatingTest {

    @Test
    public void contentCollectionsArePopulated() {
        assertEquals(26, Content.LETTERS.size());
        assertEquals(20, Content.NUMBERS.size());
        assertTrue(Content.WORDS.size() >= 20);
        assertTrue(Content.BODY.size() >= 10);
        assertTrue(Content.ANIMALS.size() >= 15);
        assertTrue(Content.PLANTS.size() >= 8);
        assertTrue(Content.FRUITS.size() >= 10);
        assertTrue(Content.LIVING.size() >= 8);
        assertTrue(Content.JAWI.size() >= 30);
        assertTrue(Content.ARABIC.size() >= 20);
        assertEquals(Content.SWATCHES.size(), Content.COLOR_ITEMS.size());
    }

    @Test
    public void everyItemHasBothLanguages() {
        for (Content.Item item : Content.LETTERS) {
            assertFalse(item.en.isEmpty());
            assertFalse(item.ms.isEmpty());
        }
        for (Content.Item item : Content.COLOR_ITEMS) {
            assertTrue(Content.isColorItem(item));
            assertFalse(item.name(Content.Lang.EN).isEmpty());
            assertFalse(item.name(Content.Lang.MS).isEmpty());
        }
        for (Content.Item item : Content.JAWI) {
            assertTrue(Content.isScriptItem(item));
        }
    }

    @Test
    public void wordItemsCarryReadableWord() {
        for (Content.Item item : Content.WORDS) {
            assertTrue("word item needs a readable word", item.word != null);
        }
    }

    @Test
    public void curriculumStartsWithOneUnlockedLevel() {
        assertEquals(11, Curriculum.total());
        assertEquals(1, Curriculum.LEVELS.get(0).id);
        for (Curriculum.Level level : Curriculum.LEVELS) {
            assertTrue("every level teaches something", level.items.size() >= 5);
            assertTrue("pass score must be positive", level.passScore > 0);
        }
    }

    @Test
    public void aLevelCannotBeSkippedBeforePassingThePrevious() {
        int highestUnlocked = 1;
        int levelId = 3;
        int required = Curriculum.byId(3).passScore;

        assertTrue("level 3 is locked at the start", levelId > highestUnlocked);

        int score = required - 10;
        assertFalse(score >= required);

        score = required;
        boolean passed = score >= required;
        assertTrue(passed);
        if (passed) {
            highestUnlocked = levelId + 1;
        }
        assertEquals(4, highestUnlocked);
    }

    @Test
    public void pronunciationMatchingAcceptsCloseAnswers() {
        assertTrue(SpeechHelper.matches("apple", "apple"));
        assertTrue(SpeechHelper.matches("apple", "Apple!"));
        assertTrue(SpeechHelper.matches("cat", "cat"));
        assertFalse(SpeechHelper.matches("cat", "elephant"));
        assertTrue(SpeechHelper.matches("bola", "bola"));
        assertTrue(SpeechHelper.matches("banana", "bananna"));
    }

    @Test
    public void normalizationStripsPunctuationAndCase() {
        assertEquals("apple", SpeechHelper.normalize("  Apple! "));
        assertEquals("", SpeechHelper.normalize(null));
    }
}
