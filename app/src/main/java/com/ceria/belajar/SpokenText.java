package com.ceria.belajar;

import java.util.HashMap;
import java.util.Locale;
import java.util.Map;

/**
 * Fixes text that a speech engine would say the wrong way. Spelling it out
 * phonetically is the only lever we have over an engine we do not control, but
 * it applies to synthesised speech only: a human recording is played as-is.
 */
public final class SpokenText {

    private static final Map<String, String> EN = new HashMap<>();
    private static final Map<String, String> MS = new HashMap<>();

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
        return fixed != null ? fixed : text;
    }
}
