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
        // Letter names, spelled the way the engine must say them.
        //
        // Malaysia, Brunei and Singapore follow the English letter names
        // (A = "ei", B = "bi", C = "si"), unlike Indonesia which uses the Dutch
        // ones ("be", "ce"). A Malay voice handed a lone "B" may read it as the
        // English "bee" or spell it out, so the name is written phonetically.
        // Source: Dewan Bahasa dan Pustaka, Pedoman Umum Ejaan Bahasa Melayu.
        EN.put("a", "ay");
        EN.put("b", "bee");
        EN.put("c", "see");
        EN.put("d", "dee");
        EN.put("e", "ee");
        EN.put("f", "ef");
        EN.put("g", "jee");
        EN.put("h", "aitch");
        EN.put("i", "eye");
        EN.put("j", "jay");
        EN.put("k", "kay");
        EN.put("l", "el");
        EN.put("m", "em");
        EN.put("n", "en");
        EN.put("o", "oh");
        EN.put("p", "pee");
        EN.put("q", "cue");
        EN.put("r", "ar");
        EN.put("s", "ess");
        EN.put("t", "tee");
        EN.put("u", "you");
        EN.put("v", "vee");
        EN.put("w", "double you");
        EN.put("x", "eks");
        EN.put("y", "why");
        EN.put("z", "zed");

        MS.put("a", "ei");
        MS.put("b", "bi");
        MS.put("c", "si");
        MS.put("d", "di");
        MS.put("e", "i");
        MS.put("f", "ef");
        MS.put("g", "ji");
        MS.put("h", "hec");
        MS.put("i", "ai");
        MS.put("j", "je");
        MS.put("k", "ke");
        MS.put("l", "el");
        MS.put("m", "em");
        MS.put("n", "en");
        MS.put("o", "o");
        MS.put("p", "pi");
        MS.put("q", "kiu");
        MS.put("r", "ar");
        MS.put("s", "es");
        MS.put("t", "ti");
        MS.put("u", "yu");
        MS.put("v", "vi");
        MS.put("w", "dabliu");
        MS.put("x", "eks");
        MS.put("y", "wai");
        MS.put("z", "zet");

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
