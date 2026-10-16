package com.ceria.belajar;

import java.util.ArrayList;
import java.util.List;

/**
 * Structured curriculum. Each level teaches a small batch of content and ends
 * with a quiz. A level stays locked until the previous one is passed, so a child
 * cannot skip ahead without first showing they learned the material.
 */
public final class Curriculum {

    public static class Level {
        public final int id;
        public final String en;
        public final String ms;
        public final String emoji;
        public final int color;
        public final int passScore;   // marks needed to unlock the next level
        public final List<Content.Item> items = new ArrayList<>();

        Level(int id, String emoji, String en, String ms, int color, int passScore) {
            this.id = id;
            this.emoji = emoji;
            this.en = en;
            this.ms = ms;
            this.color = color;
            this.passScore = passScore;
        }

        public String title(Content.Lang lang) {
            return lang == Content.Lang.MS ? ms : en;
        }
    }

    public static final List<Level> LEVELS = new ArrayList<>();

    private Curriculum() {
    }

    private static Level level(int id, String emoji, String en, String ms, int color,
                               int passScore, List<Content.Item> source, int from, int to) {
        Level l = new Level(id, emoji, en, ms, color, passScore);
        for (int i = from; i < to && i < source.size(); i++) {
            l.items.add(source.get(i));
        }
        return l;
    }

    private static Level levelAll(int id, String emoji, String en, String ms, int color,
                                  int passScore, List<Content.Item> source, int max) {
        return level(id, emoji, en, ms, color, passScore, source, 0,
                Math.min(max, source.size()));
    }

    static {
        LEVELS.add(level(1, "\uD83D\uDD24", "Letters A - F", "Huruf A - F", 0xFFFF5252,
                60, Content.LETTERS, 0, 6));
        LEVELS.add(levelAll(2, "\uD83D\uDD22", "Numbers 1 - 5", "Nombor 1 - 5", 0xFF29B6F6,
                60, Content.NUMBERS, 5));
        LEVELS.add(levelAll(3, "\uD83C\uDFA8", "Colours", "Warna", 0xFFFFB300, 60,
                Content.COLOR_ITEMS, 6));
        LEVELS.add(levelAll(4, "\uD83D\uDC41", "My Body", "Anggota Badan", 0xFFFF4081, 70,
                Content.BODY, 6));
        LEVELS.add(levelAll(5, "\uD83D\uDC3E", "Animals", "Haiwan", 0xFF66BB6A, 70,
                Content.ANIMALS, 6));
        LEVELS.add(levelAll(6, "\uD83C\uDF4E", "Fruits", "Buah-buahan", 0xFFEC407A, 70,
                Content.FRUITS, 6));
        LEVELS.add(levelAll(7, "\uD83C\uDF31", "Plants", "Tumbuh-tumbuhan", 0xFF26A69A, 70,
                Content.PLANTS, 6));
        LEVELS.add(levelAll(8, "\uD83D\uDCD6", "Words", "Perkataan", 0xFFAB47BC, 80,
                Content.WORDS, 8));
        LEVELS.add(levelAll(9, "\uD83C\uDF89", "Living Things", "Benda Hidup", 0xFF5C6BC0, 80,
                Content.LIVING, 8));
        LEVELS.add(levelAll(10, "\u0645", "Jawi Letters", "Huruf Jawi", 0xFF00897B, 80,
                Content.JAWI, 8));
        LEVELS.add(levelAll(11, "\u0639", "Arabic Letters", "Huruf Arab", 0xFF6D4C41, 80,
                Content.ARABIC, 8));
    }

    public static int total() {
        return LEVELS.size();
    }

    public static Level byId(int id) {
        for (Level l : LEVELS) {
            if (l.id == id) {
                return l;
            }
        }
        return LEVELS.get(0);
    }
}
