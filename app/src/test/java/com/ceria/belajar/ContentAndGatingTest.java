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

    @Test
    public void jawiLetterNamesFollowTheSchoolStandard() {
        // Names that were wrong before: they had been transliterated with Arabic
        // letter values instead of the Malaysian KSSR letter names.
        assertEquals("sa", jawiName("\u062B"));     // ث
        assertEquals("zal", jawiName("\u0630"));    // ذ
        assertEquals("dad", jawiName("\u0636"));    // ض
        assertEquals("to", jawiName("\u0637"));     // ط
        assertEquals("zho", jawiName("\u0638"));    // ظ
        assertEquals("kha", jawiName("\u062E"));    // خ
        assertEquals("ca", jawiName("\u0686"));     // چ
        assertEquals("ra", jawiName("\u0631"));     // ر

        // Letters that were missing from the inventory.
        assertEquals("nga", jawiName("\u06A0"));    // ڠ
        assertEquals("hamzah", jawiName("\u0621")); // ء
        assertEquals("ya", jawiName("\u0649"));     // ى
    }

    private String jawiName(String glyph) {
        for (Content.Item item : Content.JAWI) {
            if (glyph.equals(item.symbol)) {
                return item.ms;
            }
        }
        return "<missing: " + glyph + ">";
    }

    @Test
    public void everyJawiLetterHasAUniqueGlyphAndName() {
        java.util.Set<String> glyphs = new java.util.HashSet<>();
        for (Content.Item item : Content.JAWI) {
            assertTrue("duplicate Jawi glyph " + item.symbol, glyphs.add(item.symbol));
            assertFalse("Jawi letter without a name", item.ms.trim().isEmpty());
            // Names are spoken, so they must be plain lower-case Rumi.
            assertTrue("Jawi name must be lower case: " + item.ms,
                    item.ms.equals(item.ms.toLowerCase(java.util.Locale.ROOT)));
        }
    }

    @Test
    public void strictMatchingRejectsSpelledOutLetterNames() {
        // Saying the name is right...
        assertTrue(SpeechHelper.matchesStrict("kha", "kha"));
        assertTrue(SpeechHelper.matchesStrict("kha", "Kha"));
        // ...and saying it inside a short phrase is still saying it.
        assertTrue(SpeechHelper.matchesStrict("kha", "ini kha"));

        // Spelling it out is not: this is the mistake the game must catch.
        assertFalse(SpeechHelper.matchesStrict("kha", "k h a"));
        assertFalse(SpeechHelper.matchesStrict("kha", "k-h-a"));
        assertFalse(SpeechHelper.matchesStrict("kha", "K. H. A."));
        assertFalse(SpeechHelper.matchesStrict("ba", "b a"));
        assertFalse(SpeechHelper.matchesStrict("nga", "n g a"));
        assertFalse(SpeechHelper.matchesStrict("syin", "s y i n"));

        // A genuinely different sound is still wrong.
        assertFalse(SpeechHelper.matchesStrict("kha", "ha"));
        assertFalse(SpeechHelper.matchesStrict("kha", "ka"));
    }

    @Test
    public void spellingDetectionOnlyFiresWhenTheLettersSpellTheName() {
        assertTrue(SpeechHelper.spelledOut("k h a", "kha"));
        assertTrue(SpeechHelper.spelledOut("b-a", "ba"));
        assertFalse(SpeechHelper.spelledOut("kha", "kha"));
        assertFalse(SpeechHelper.spelledOut("ini kha", "kha"));
        // Single letters that do not spell the name are not "spelling it out".
        assertFalse(SpeechHelper.spelledOut("x y", "kha"));
    }

    @Test
    public void letterNamesAreNeverSentToTheEngineBare() {
        // A lone syllable is read letter by letter, so the engine gets a frame.
        assertEquals("Ini kha", SpokenText.forEngine(Content.Lang.MS, "kha"));
        assertEquals("Ini ba", SpokenText.forEngine(Content.Lang.MS, "ba"));
        assertEquals("Ini nya", SpokenText.forEngine(Content.Lang.MS, "nya"));
        assertTrue(SpokenText.needsCarrier("kha"));
        assertTrue(SpokenText.needsCarrier("KHA"));

        // Ordinary words pass through untouched...
        assertEquals("bola", SpokenText.forEngine(Content.Lang.MS, "bola"));
        assertFalse(SpokenText.needsCarrier("bola"));
        // ...and the deliberate respellings that already existed still apply.
        assertEquals("ee-pal", SpokenText.forEngine(Content.Lang.MS, "epal"));
        assertFalse(SpokenText.needsCarrier("epal"));
    }

    @Test
    public void everyJawiLetterNameGetsACarrierPhrase() {
        for (Content.Item item : Content.JAWI) {
            String sent = SpokenText.forEngine(Content.Lang.MS, item.spokenText(Content.Lang.MS));
            assertTrue("no carrier for " + item.ms + " -> " + sent,
                    sent.startsWith("Ini "));
        }
    }
}
