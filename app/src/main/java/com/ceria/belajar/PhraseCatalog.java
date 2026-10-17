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
        for (Content.Unit unit : Content.UNITS) {
            addUnit(unique, lang, unit);
        }
        addSwatches(unique, lang);
        return new ArrayList<>(unique);
    }

    /**
     * Every phrase a unit can say. Letters are special: the child must hear the
     * letter name ("ei"), and the picture beside it ("apel") is a separate phrase
     * so the flashcards and the ABC song can both be served from one list.
     */
    private static void addUnit(LinkedHashSet<Phrase> out, Content.Lang lang, Content.Unit unit) {
        if ("sentence".equals(unit.kind)) {
            for (Content.Sentence s : unit.sentences) {
                out.add(new Phrase(lang, s.text(lang), unit.id));
            }
            return;
        }
        for (Content.Item item : unit.items) {
            if (Content.isScriptItem(item)) {
                // Jawi and Arabic are read by their letter name, e.g. "ba".
                out.add(new Phrase(lang, item.name(lang), unit.id));
                continue;
            }
            if (Content.isColorItem(item)) {
                out.add(new Phrase(lang, item.name(lang), unit.id));
                continue;
            }
            if ("letter".equals(unit.kind)) {
                out.add(new Phrase(lang, Content.letterGlyph(item).toUpperCase(Locale.ROOT), unit.id));
                out.add(new Phrase(lang, item.name(lang), unit.id));
                continue;
            }
            out.add(new Phrase(lang, item.spokenText(lang), unit.id));
            if (!item.name(lang).equals(item.spokenText(lang))) {
                out.add(new Phrase(lang, item.name(lang), unit.id));
            }
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
