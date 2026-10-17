package com.ceria.belajar;

import java.util.HashMap;
import java.util.LinkedHashSet;
import java.util.Locale;
import java.util.Map;
import java.util.Set;

/**
 * Fixes text that a speech engine would say the wrong way. Spelling it out
 * phonetically is the only lever we have over an engine we do not control, but
 * it applies to synthesised speech only: a human recording is played as-is.
 */
public final class SpokenText {

    private static final Map<String, String> EN = new HashMap<>();
    private static final Map<String, String> MS = new HashMap<>();

    /**
     * Letter names that must never reach the engine on their own.
     *
     * A speech engine treats a lone short token as an initialism and reads it
     * letter by letter, so "kha" comes out as "K-H-A". That is exactly the
     * pronunciation a child must not copy. Wrapping the name in a short phrase
     * gives the engine enough context to read it as one syllable, which is the
     * documented workaround for engines whose SSML support is unreliable.
     */
    private static final Set<String> LETTER_NAMES = new LinkedHashSet<>();

    static {
        // Letter names: a lone "A" can be read as an article, "X" as "eks".
        EN.put("a", "ay");
        EN.put("x", "eks");

        // Gently slow, syllable-friendly spelling for the youngest readers.
        EN.put("elephant", "el-e-fant");
        EN.put("umbrella", "um-brel-la");
        EN.put("butterfly", "but-ter-fly");
        EN.put("strawberry", "straw-ber-ry");
        EN.put("watermelon", "wa-ter-me-lon");
        EN.put("xylophone", "zy-lo-phone");

        MS.put("epal", "ee-pal");
        MS.put("een", "een");

        for (Content.Item item : Content.JAWI) {
            LETTER_NAMES.add(item.ms.toLowerCase(Locale.ROOT));
        }
        for (Content.Item item : Content.ARABIC) {
            LETTER_NAMES.add(item.ms.toLowerCase(Locale.ROOT));
        }
    }

    private SpokenText() {
    }

    /** The form to hand to a speech engine. Never used for a human recording. */
    public static String forEngine(Content.Lang lang, String text) {
        if (text == null) {
            return null;
        }
        String key = text.toLowerCase(Locale.ROOT).trim();
        Map<String, String> map = lang == Content.Lang.MS ? MS : EN;
        String fixed = map.get(key);
        if (fixed != null) {
            return fixed;
        }
        if (LETTER_NAMES.contains(key)) {
            return carrier(lang, text.trim());
        }
        return text;
    }

    /** True when the engine would need a carrier phrase for this text. */
    public static boolean needsCarrier(String text) {
        return text != null && LETTER_NAMES.contains(text.toLowerCase(Locale.ROOT).trim());
    }

    /**
     * A letter name inside a short spoken frame. "Ini kha" is a natural thing for
     * a teacher to say while pointing at a letter, and it keeps the engine from
     * spelling the syllable out.
     */
    static String carrier(Content.Lang lang, String name) {
        return (lang == Content.Lang.MS ? "Ini " : "This is ") + name;
    }
}
