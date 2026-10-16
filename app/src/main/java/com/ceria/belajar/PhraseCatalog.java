package com.ceria.belajar;

import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;

/**
 * Every phrase the app can say out loud, in a stable order.
 *
 * The same list drives three things: the parent recording studio, the file names
 * of bundled human clips in res/raw, and the CSV that a voice artist reads from.
 * A phrase's key must stay identical between all three, so it is derived only
 * from the text itself (see {@link #key}).
 */
public final class PhraseCatalog {

    /** One phrase to say, in one language. */
    public static class Phrase {
        public final Content.Lang lang;
        public final String text;
        public final String group;

        public Phrase(Content.Lang lang, String text, String group) {
            this.lang = lang;
            this.text = text;
            this.group = group;
        }

        /** e.g. "ms_epal", the base of clip_ms_epal.mp3 and voice/ms_epal.m4a */
        public String key() {
            return PhraseCatalog.key(lang, text);
        }

        @Override
        public String toString() {
            return key() + "  " + text;
        }

        @Override
        public boolean equals(Object o) {
            return o instanceof Phrase && ((Phrase) o).key().equals(key());
        }

        @Override
        public int hashCode() {
            return key().hashCode();
        }
    }

    private PhraseCatalog() {
    }

    /**
     * Turns a phrase into a safe file name: lower case, letters and digits kept,
     * everything else becomes a single underscore. Stable across runs, so a
     * recording stays attached to the same phrase after an app update.
     */
    public static String key(Content.Lang lang, String text) {
        String prefix = lang == Content.Lang.MS ? "ms_" : "en_";
        if (text == null) {
            return prefix + "empty";
        }
        String flat = text.toLowerCase(Locale.ROOT).trim()
                .replaceAll("[^a-z0-9]+", "_")
                .replaceAll("^_+|_+$", "");
        if (flat.isEmpty()) {
            flat = "empty";
        }
        return prefix + flat;
    }

    /** All phrases for one language, de-duplicated, in a stable order. */
    public static List<Phrase> forLang(Content.Lang lang) {
        LinkedHashSet<Phrase> unique = new LinkedHashSet<>();
        addItems(unique, lang, "letters", Content.LETTERS, true);
        addItems(unique, lang, "numbers", Content.NUMBERS, false);
        addItems(unique, lang, "words", Content.WORDS, false);
        addSentences(unique, lang);
        addSwatches(unique, lang);
        addItems(unique, lang, "body", Content.BODY, false);
        addItems(unique, lang, "animals", Content.ANIMALS, false);
        addItems(unique, lang, "plants", Content.PLANTS, false);
        addItems(unique, lang, "fruits", Content.FRUITS, false);
        addItems(unique, lang, "living", Content.LIVING, false);
        addItems(unique, lang, "jawi", Content.JAWI, false);
        addItems(unique, lang, "arabic", Content.ARABIC, false);
        return new ArrayList<>(unique);
    }

    private static void addItems(LinkedHashSet<Phrase> out, Content.Lang lang, String group,
                                 List<Content.Item> items, boolean includeLetterName) {
        for (Content.Item item : items) {
            if (Content.isScriptItem(item)) {
                // Jawi and Arabic are read by their letter name, e.g. "ba".
                out.add(new Phrase(lang, item.name(lang), group));
                continue;
            }
            if (Content.isColorItem(item)) {
                out.add(new Phrase(lang, item.name(lang), group));
                continue;
            }
            if (includeLetterName) {
                out.add(new Phrase(lang, Content.letterGlyph(item).toUpperCase(Locale.ROOT), group));
            }
            out.add(new Phrase(lang, item.spokenText(lang), group));
        }
    }

    private static void addSentences(LinkedHashSet<Phrase> out, Content.Lang lang) {
        for (Content.Sentence sentence : Content.SENTENCES) {
            out.add(new Phrase(lang, sentence.text(lang), "sentences"));
        }
    }

    private static void addSwatches(LinkedHashSet<Phrase> out, Content.Lang lang) {
        for (Content.Swatch swatch : Content.SWATCHES) {
            out.add(new Phrase(lang, swatch.name(lang), "colours"));
        }
    }

    /** Short phrases the app says beyond the learning content, e.g. encouragement. */
    public static List<Phrase> praise(Content.Lang lang) {
        List<Phrase> out = new ArrayList<>();
        if (lang == Content.Lang.MS) {
            out.add(new Phrase(lang, "Bagus sekali!", "praise"));
            out.add(new Phrase(lang, "Hebat!", "praise"));
            out.add(new Phrase(lang, "Cuba lagi ya", "praise"));
            out.add(new Phrase(lang, "Hai kawan! Jom belajar sama-sama!", "praise"));
        } else {
            out.add(new Phrase(lang, "Great job!", "praise"));
            out.add(new Phrase(lang, "Well done!", "praise"));
            out.add(new Phrase(lang, "Try again", "praise"));
            out.add(new Phrase(lang, "Hi friend! Let's learn together!", "praise"));
        }
        return out;
    }
}
