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
        assertTrue("the reading word list must not be empty", Content.WORDS.size() >= 10);
        assertTrue(Content.BODY.size() >= 10);
        assertTrue(Content.ANIMALS.size() >= 15);
        assertTrue(Content.PLANTS.size() >= 8);
        assertTrue(Content.FRUITS.size() >= 10);
        assertTrue(Content.LIVING.size() >= 8);
        assertTrue(Content.JAWI.size() >= 30);
        assertTrue(Content.ARABIC.size() >= 20);
        assertTrue(Content.SENTENCES.size() >= 10);
        assertEquals(Content.SWATCHES.size(), Content.COLOR_ITEMS.size());
    }

    @Test
    public void everyUnitIsWellFormed() {
        assertTrue("there should be six tunjang", Content.TUNJANG.size() == 6);
        assertTrue("there should be many units", Content.UNITS.size() >= 40);

        java.util.Set<String> ids = new java.util.HashSet<>();
        for (Content.Tunjang tunjang : Content.TUNJANG) {
            assertFalse("tunjang needs an id", tunjang.id.isEmpty());
            assertFalse("tunjang needs a name", tunjang.en.isEmpty() || tunjang.ms.isEmpty());
            assertTrue("tunjang needs units", !tunjang.units.isEmpty());
            for (Content.Unit unit : tunjang.units) {
                assertTrue("unit id must be unique: " + unit.id, ids.add(unit.id));
                assertFalse("unit needs a name", unit.en.isEmpty() || unit.ms.isEmpty());
                assertFalse("unit needs an emoji", unit.emoji.isEmpty());
                assertTrue("unit needs content: " + unit.id,
                        !unit.items.isEmpty() || !unit.sentences.isEmpty());
            }
        }
    }

    @Test
    public void everyItemHasBothLanguages() {
        for (Content.Unit unit : Content.UNITS) {
            for (Content.Item item : unit.items) {
                assertFalse("item needs English in " + unit.id,
                        item.name(Content.Lang.EN).isEmpty());
                assertFalse("item needs Malay in " + unit.id,
                        item.name(Content.Lang.MS).isEmpty());
            }
        }
    }

    @Test
    public void lettersAreStoredWithTheirPictureAndSaidByName() {
        Content.Unit letters = Content.unitById("bm-huruf");
        assertTrue("the ABC unit must exist", letters != null);
        assertEquals(26, letters.items.size());

        for (Content.Item item : letters.items) {
            String glyph = Content.letterGlyph(item);
            assertEquals(1, glyph.length());
            // The picture must not leak into the spoken form: a child learning
            // "A" must hear "ei", not "apel".
            String spoken = Content.spokenOf(item, "letter", Content.Lang.MS);
            assertFalse("letter " + glyph + " must not be spoken as its picture",
                    spoken.equals(item.ms));
            assertTrue("letter " + glyph + " must still show a picture",
                    item.symbol.indexOf('|') > 0);
        }
    }

    @Test
    public void letterGlyphNeverLeaksTheStoredPipe() {
        for (Content.Item item : Content.LETTERS) {
            String shown = Content.displayOf(item, Content.Lang.MS);
            assertFalse("a raw symbol leaked: " + shown, shown.contains("|"));
        }
    }

    @Test
    public void malayReadingIsTaughtThroughSyllables() {
        Content.Unit syllables = Content.unitById("bm-suku-kata");
        assertTrue("the syllable unit must exist", syllables != null);
        assertEquals(70, syllables.items.size());
        assertEquals("ba", syllables.items.get(0).symbol);

        Content.Unit wordSyllables = Content.unitById("bm-suku-kata-kata");
        assertTrue("the syllable-to-word unit must exist", wordSyllables != null);
        for (Content.Item item : wordSyllables.items) {
            assertTrue("a syllable word needs a readable word: " + item.symbol,
                    item.word != null && item.word.matches("[a-z]+"));
        }
    }

    @Test
    public void wordItemsCarryReadableWord() {
        for (Content.Unit unit : Content.UNITS) {
            if (!"word".equals(unit.kind)) {
                continue;
            }
            for (Content.Item item : unit.items) {
                assertTrue("word item in " + unit.id + " needs a readable word",
                        item.word != null && item.word.matches("[a-z]+"));
            }
        }
    }

    @Test
    public void scriptsAreMarkedAsScriptAndColoursAsColours() {
        Content.Unit jawi = Content.unitById("bm-jawi");
        assertTrue("the Jawi unit must exist", jawi != null);
        for (Content.Item item : jawi.items) {
            assertTrue("Jawi must be marked as script", Content.isScriptItem(item));
        }

        Content.Unit colours = Content.unitById("fiz-warna");
        assertTrue("the colour unit must exist", colours != null);
        for (Content.Item item : colours.items) {
            assertTrue("a colour must be a colour item", Content.isColorItem(item));
        }
    }

    @Test
    public void theYearHasFortyTwoThemes() {
        assertEquals(42, Content.THEMES.size());
        assertEquals(1, Content.THEMES.get(0).week);
        assertEquals(42, Content.THEMES.get(41).week);
        for (Content.Theme theme : Content.THEMES) {
            assertFalse(theme.en.isEmpty());
            assertFalse(theme.ms.isEmpty());
        }
    }

    @Test
    public void everyUnitBelongsToATunjang() {
        for (Content.Unit unit : Content.UNITS) {
            String owner = Content.tunjangIdOf(unit);
            assertTrue("unit " + unit.id + " has no tunjang", owner != null);
            assertTrue("unit " + unit.id + " is not reachable by id",
                    Content.unitById(unit.id) != null);
        }
    }

    @Test
    public void displayOfNeverShowsAColourHexString() {
        Content.Unit colours = Content.unitById("fiz-warna");
        for (Content.Item item : colours.items) {
            String shown = Content.displayOf(item, Content.Lang.MS);
            assertFalse("a hex colour leaked to the UI: " + shown, shown.startsWith("#"));
        }
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
