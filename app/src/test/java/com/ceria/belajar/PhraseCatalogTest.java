package com.ceria.belajar;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotEquals;
import static org.junit.Assert.assertNotNull;
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

    /**
     * A bundled clip must be named clip_&lt;key&gt;.&lt;ext&gt; for VoicePack to find it.
     * A typo here is silent: the app simply keeps using the robot voice, so the
     * naming is checked rather than trusted.
     */
    @Test
    public void bundledClipsAreNamedAfterKnownPhrases() {
        java.io.File raw = findRawDir();
        java.io.File[] files = raw == null ? null
                : raw.listFiles((dir, name) -> name.startsWith("clip_"));
        if (files == null || files.length == 0) {
            return;     // no pack bundled yet; nothing to check
        }
        Set<String> known = new HashSet<>();
        for (Content.Lang lang : Content.Lang.values()) {
            for (PhraseCatalog.Phrase phrase : PhraseCatalog.forLang(lang)) {
                known.add(phrase.key());
            }
        }
        for (java.io.File file : files) {
            String name = file.getName();
            int dot = name.lastIndexOf('.');
            assertTrue("clip has no extension: " + name, dot > 0);
            String key = name.substring("clip_".length(), dot);
            assertTrue("clip does not match any phrase: " + name, known.contains(key));
        }
    }

    /** Clips are served to MediaPlayer as-is, so the bytes must be real audio. */
    @Test
    public void bundledClipsAreRealAudio() throws Exception {
        java.io.File raw = findRawDir();
        java.io.File[] files = raw == null ? null
                : raw.listFiles((dir, name) -> name.startsWith("clip_"));
        if (files == null || files.length == 0) {
            return;
        }
        byte[] header = new byte[4];
        for (java.io.File file : files) {
            try (java.io.FileInputStream in = new java.io.FileInputStream(file)) {
                assertEquals("short clip: " + file.getName(), 4, in.read(header));
            }
            String magic = new String(header, "ISO-8859-1");
            assertTrue("not Ogg or MP4 audio: " + file.getName() + " (" + magic + ")",
                    magic.equals("OggS") || magic.startsWith("\u0000\u0000\u0000"));
        }
    }

    /**
     * Every bundled Commons clip must carry attribution, because the licence is
     * CC BY-SA. A clip with no credit row would make the app breach the licence
     * silently, so the two lists are checked against each other.
     */
    @Test
    public void everyBundledClipHasACreditEntry() throws Exception {
        java.io.File raw = findRawDir();
        java.io.File[] files = raw == null ? null
                : raw.listFiles((dir, name) -> name.startsWith("clip_"));
        if (files == null || files.length == 0) {
            return;     // no pack bundled yet; nothing to credit
        }

        java.io.File credits = null;
        for (String candidate : new String[]{"app/src/main/assets/voice-credits.csv",
                "src/main/assets/voice-credits.csv", "../app/src/main/assets/voice-credits.csv"}) {
            java.io.File f = new java.io.File(candidate);
            if (f.isFile()) {
                credits = f;
                break;
            }
        }
        assertNotNull("clips are bundled but there is no credit file", credits);

        Set<String> credited = new HashSet<>();
        try (java.io.BufferedReader in = new java.io.BufferedReader(
                new java.io.InputStreamReader(new java.io.FileInputStream(credits), "UTF-8"))) {
            String line = in.readLine();     // header
            while ((line = in.readLine()) != null) {
                if (line.trim().isEmpty()) {
                    continue;
                }
                String[] parts = line.split(",", -1);
                if (parts.length > 0) {
                    credited.add(parts[0].trim());
                }
            }
        }

        for (java.io.File file : files) {
            String name = file.getName();
            String key = name.substring("clip_".length(), name.lastIndexOf('.'));
            assertTrue("bundled clip has no attribution row: " + name, credited.contains(key));
        }
    }

    private java.io.File findRawDir() {
        String[] candidates = {
                "app/src/main/res/raw", "src/main/res/raw", "../app/src/main/res/raw"};
        for (String candidate : candidates) {
            java.io.File dir = new java.io.File(candidate);
            if (dir.isDirectory()) {
                return dir;
            }
        }
        return null;
    }
}
