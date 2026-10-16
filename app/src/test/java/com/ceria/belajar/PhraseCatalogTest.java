package com.ceria.belajar;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotEquals;
import static org.junit.Assert.assertTrue;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.annotation.Config;

import java.util.HashSet;
import java.util.List;
import java.util.Set;

/**
 * The phrase list is the contract between three places: the parent studio file
 * names, the bundled clip names in res/raw, and the CSV a voice artist reads.
 * If a key changes, recordings silently stop matching, so these tests pin it.
 */
@RunWith(RobolectricTestRunner.class)
@Config(sdk = 33)
public class PhraseCatalogTest {

    @Test
    public void keysAreReadableFileNames() {
        for (Content.Lang lang : Content.Lang.values()) {
            for (PhraseCatalog.Phrase phrase : PhraseCatalog.forLang(lang)) {
                String key = phrase.key();
                assertTrue("bad key: " + key, key.matches("[a-z0-9_]+"));
                assertFalse("leading underscore: " + key, key.startsWith("_"));
                assertFalse("double underscore: " + key, key.contains("__"));
                assertTrue("wrong prefix: " + key,
                        key.startsWith(lang == Content.Lang.MS ? "ms_" : "en_"));
            }
        }
    }

    @Test
    public void keysAreUniqueWithinALanguage() {
        for (Content.Lang lang : Content.Lang.values()) {
            Set<String> seen = new HashSet<>();
            for (PhraseCatalog.Phrase phrase : PhraseCatalog.forLang(lang)) {
                assertTrue("duplicate key " + phrase.key() + " for " + phrase.text,
                        seen.add(phrase.key()));
            }
        }
    }

    @Test
    public void numberItemsAreSpokenAsWordsNotEmoji() {
        List<PhraseCatalog.Phrase> ms = PhraseCatalog.forLang(Content.Lang.MS);
        assertHas(ms, "ms_satu");
        assertHas(ms, "ms_dua");
        assertHas(ms, "ms_dua_puluh");
        List<PhraseCatalog.Phrase> en = PhraseCatalog.forLang(Content.Lang.EN);
        assertHas(en, "en_one");
        assertHas(en, "en_twenty");

        for (PhraseCatalog.Phrase phrase : ms) {
            assertFalse("emoji leaked into a phrase: " + phrase.text,
                    phrase.text.matches(".*[\\uD800-\\uDFFF].*"));
        }
    }

    @Test
    public void letterNamesAreIncluded() {
        for (Content.Lang lang : Content.Lang.values()) {
            List<PhraseCatalog.Phrase> phrases = PhraseCatalog.forLang(lang);
            String prefix = lang == Content.Lang.MS ? "ms_" : "en_";
            for (char c = 'a'; c <= 'z'; c++) {
                assertHas(phrases, prefix + c);
            }
        }
    }

    @Test
    public void malayAndEnglishMayShareSpellingButNotKeys() {
        // "panda" and "van" are spelled the same in both languages.
        String ms = PhraseCatalog.key(Content.Lang.MS, "Panda");
        String en = PhraseCatalog.key(Content.Lang.EN, "panda");
        assertNotEquals(ms, en);
        assertEquals("ms_panda", ms);
        assertEquals("en_panda", en);
    }

    @Test
    public void malayEpalIsKeyedAndRecoverable() {
        assertEquals("ms_epal", PhraseCatalog.key(Content.Lang.MS, "epal"));
        assertEquals("ms_ais_krim", PhraseCatalog.key(Content.Lang.MS, "Ais krim"));
    }

    @Test
    public void everyLanguageHasAUsefulNumberOfPhrases() {
        assertTrue(PhraseCatalog.forLang(Content.Lang.MS).size() >= 180);
        assertTrue(PhraseCatalog.forLang(Content.Lang.EN).size() >= 180);
    }

    private void assertHas(List<PhraseCatalog.Phrase> phrases, String key) {
        for (PhraseCatalog.Phrase phrase : phrases) {
            if (phrase.key().equals(key)) {
                return;
            }
        }
        throw new AssertionError("missing phrase " + key);
    }
}
