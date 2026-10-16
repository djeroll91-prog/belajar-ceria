package com.ceria.belajar;

import java.util.ArrayList;
import java.util.List;

/** All learning content for the app, in English and Bahasa Melayu. */
public final class Content {

    public enum Lang { EN, MS }

    /** One learnable thing: a big symbol/emoji plus its name in both languages. */
    public static class Item {
        public final String symbol;
        public final String en;
        public final String ms;
        /** For reading practice this holds the word itself (e.g. "cat"); else null. */
        public final String word;

        public Item(String symbol, String en, String ms) {
            this(symbol, en, ms, null);
        }

        public Item(String symbol, String en, String ms, String word) {
            this.symbol = symbol;
            this.en = en;
            this.ms = ms;
            this.word = word;
        }

        public String name(Lang lang) {
            return lang == Lang.MS ? ms : en;
        }

        public String spokenText(Lang lang) {
            return word != null ? word : name(lang);
        }
    }

    public static class Sentence {
        public final String en;
        public final String ms;

        public Sentence(String en, String ms) {
            this.en = en;
            this.ms = ms;
        }

        public String text(Lang lang) {
            return lang == Lang.MS ? ms : en;
        }
    }

    private Content() {
    }

    public static final List<Item> LETTERS = new ArrayList<>();
    public static final List<Item> NUMBERS = new ArrayList<>();
    public static final List<Item> WORDS = new ArrayList<>();
    public static final List<Sentence> SENTENCES = new ArrayList<>();

    static {
        // ---- Alphabet ----
        add(LETTERS, "A", "Apple", "Apel", "\uD83C\uDF4E");
        add(LETTERS, "B", "Ball", "Bola", "\u26BD");
        add(LETTERS, "C", "Cat", "Kucing", "\uD83D\uDC31");
        add(LETTERS, "D", "Duck", "Itik", "\uD83E\uDD86");
        add(LETTERS, "E", "Elephant", "Gajah", "\uD83D\uDC18");
        add(LETTERS, "F", "Fish", "Ikan", "\uD83D\uDC1F");
        add(LETTERS, "G", "Grapes", "Anggur", "\uD83C\uDF47");
        add(LETTERS, "H", "House", "Rumah", "\uD83C\uDFE0");
        add(LETTERS, "I", "Ice cream", "Ais krim", "\uD83C\uDF66");
        add(LETTERS, "J", "Juice", "Jus", "\uD83E\uDDC3");
        add(LETTERS, "K", "Kite", "Layang-layang", "\uD83E\uDE81");
        add(LETTERS, "L", "Lion", "Singa", "\uD83E\uDD81");
        add(LETTERS, "M", "Moon", "Bulan", "\uD83C\uDF19");
        add(LETTERS, "N", "Nose", "Hidung", "\uD83D\uDC43");
        add(LETTERS, "O", "Orange", "Oren", "\uD83C\uDF4A");
        add(LETTERS, "P", "Panda", "Panda", "\uD83D\uDC3C");
        add(LETTERS, "Q", "Queen", "Ratu", "\uD83D\uDC51");
        add(LETTERS, "R", "Rabbit", "Arnab", "\uD83D\uDC30");
        add(LETTERS, "S", "Sun", "Matahari", "\u2600\uFE0F");
        add(LETTERS, "T", "Tiger", "Harimau", "\uD83D\uDC2F");
        add(LETTERS, "U", "Umbrella", "Payung", "\u2602\uFE0F");
        add(LETTERS, "V", "Van", "Van", "\uD83D\uDE90");
        add(LETTERS, "W", "Whale", "Ikan paus", "\uD83D\uDC33");
        add(LETTERS, "X", "Xylophone", "Xilofon", "\uD83C\uDFBC");
        add(LETTERS, "Y", "Yacht", "Kapal layar", "\u26F5");
        add(LETTERS, "Z", "Zebra", "Zebra", "\uD83E\uDD93");

        // ---- Numbers 1..20 ----
        String[] enNum = {"one", "two", "three", "four", "five", "six", "seven", "eight",
                "nine", "ten", "eleven", "twelve", "thirteen", "fourteen", "fifteen",
                "sixteen", "seventeen", "eighteen", "nineteen", "twenty"};
        String[] msNum = {"satu", "dua", "tiga", "empat", "lima", "enam", "tujuh", "lapan",
                "sembilan", "sepuluh", "sebelas", "dua belas", "tiga belas", "empat belas",
                "lima belas", "enam belas", "tujuh belas", "lapan belas", "sembilan belas",
                "dua puluh"};
        String[] numSymbols = {"\uD83C\uDF4E", "\uD83C\uDF4A", "\uD83C\uDF53", "\uD83C\uDF4C",
                "\uD83C\uDF49", "\uD83C\uDF51", "\uD83C\uDF4D", "\u2B50", "\uD83C\uDF88",
                "\uD83C\uDF38", "\uD83D\uDC1D", "\uD83C\uDF3B", "\uD83C\uDF40", "\uD83C\uDF6A",
                "\uD83C\uDF69", "\uD83C\uDF6D", "\uD83C\uDF81", "\uD83C\uDF2C\uFE0F", "\uD83D\uDC23",
                "\uD83C\uDF1F"};
        for (int i = 0; i < 20; i++) {
            NUMBERS.add(new Item(String.valueOf(i + 1), enNum[i], msNum[i], numSymbols[i]));
        }

        // ---- Simple words to read ----
        List<Item> enWords = new ArrayList<>();
        addWord(enWords, "cat", "cat", "kucing", "\uD83D\uDC08");
        addWord(enWords, "dog", "dog", "anjing", "\uD83D\uDC15");
        addWord(enWords, "sun", "sun", "matahari", "\u2600\uFE0F");
        addWord(enWords, "hat", "hat", "topi", "\uD83D\uDC52");
        addWord(enWords, "bus", "bus", "bas", "\uD83D\uDE8C");
        addWord(enWords, "cup", "cup", "cawan", "\uD83E\uDD5B");
        addWord(enWords, "pen", "pen", "pen", "\uD83D\uDD8A\uFE0F");
        addWord(enWords, "bed", "bed", "katil", "\uD83D\uDECF\uFE0F");
        addWord(enWords, "star", "star", "bintang", "\u2B50");
        addWord(enWords, "moon", "moon", "bulan", "\uD83C\uDF19");
        addWord(enWords, "fish", "fish", "ikan", "\uD83D\uDC1F");
        addWord(enWords, "bird", "bird", "burung", "\uD83D\uDC26");
        WORDS.addAll(enWords);

        List<Item> msWords = new ArrayList<>();
        addWord(msWords, "ibu", "mother", "ibu", "\uD83D\uDC69");
        addWord(msWords, "ayah", "father", "ayah", "\uD83D\uDC68");
        addWord(msWords, "kaki", "leg", "kaki", "\uD83E\uDDB6");
        addWord(msWords, "baju", "shirt", "baju", "\uD83D\uDC55");
        addWord(msWords, "buku", "book", "buku", "\uD83D\uDCD5");
        addWord(msWords, "meja", "table", "meja", "\uD83E\uDE91");
        addWord(msWords, "susu", "milk", "susu", "\uD83E\uDD5B");
        addWord(msWords, "bola", "ball", "bola", "\u26BD");
        addWord(msWords, "mata", "eye", "mata", "\uD83D\uDC41");
        addWord(msWords, "nasi", "rice", "nasi", "\uD83C\uDF5A");
        addWord(msWords, "rumah", "house", "rumah", "\uD83C\uDFE0");
        addWord(msWords, "bunga", "flower", "bunga", "\uD83C\uDF38");
        WORDS.addAll(msWords);

        // ---- Simple sentences ----
        addSentence("The cat is big.", "Kucing itu besar.");
        addSentence("I like to play.", "Saya suka bermain.");
        addSentence("The sun is hot.", "Matahari sangat panas.");
        addSentence("My ball is red.", "Bola saya berwarna merah.");
        addSentence("I can read.", "Saya boleh membaca.");
        addSentence("The fish can swim.", "Ikan boleh berenang.");
        addSentence("I love my mother.", "Saya sayang ibu saya.");
        addSentence("This is my book.", "Ini buku saya.");
    }

    // ---- Colours (name + the actual swatch colour used by the colouring game) ----
    public static class Swatch {
        public final int color;
        public final String en;
        public final String ms;

        public Swatch(int color, String en, String ms) {
            this.color = color;
            this.en = en;
            this.ms = ms;
        }

        public String name(Lang lang) {
            return lang == Lang.MS ? ms : en;
        }
    }

    public static final List<Swatch> SWATCHES = new ArrayList<>();
    public static final List<Item> COLOR_ITEMS = new ArrayList<>();

    static {
        SWATCHES.add(new Swatch(0xFFFF5252, "red", "merah"));
        SWATCHES.add(new Swatch(0xFFFF9800, "orange", "jingga"));
        SWATCHES.add(new Swatch(0xFFFFEB3B, "yellow", "kuning"));
        SWATCHES.add(new Swatch(0xFF66BB6A, "green", "hijau"));
        SWATCHES.add(new Swatch(0xFF42A5F5, "blue", "biru"));
        SWATCHES.add(new Swatch(0xFFAB47BC, "purple", "ungu"));
        SWATCHES.add(new Swatch(0xFFFF80AB, "pink", "merah jambu"));
        SWATCHES.add(new Swatch(0xFF8D6E63, "brown", "coklat"));
        SWATCHES.add(new Swatch(0xFF90A4AE, "grey", "kelabu"));
        SWATCHES.add(new Swatch(0xFF212121, "black", "hitam"));
        SWATCHES.add(new Swatch(0xFFFFFFFF, "white", "putih"));
        for (Swatch s : SWATCHES) {
            COLOR_ITEMS.add(new Item(String.format("#%06X", 0xFFFFFF & s.color), s.en, s.ms));
        }
    }

    /** Colour items are stored as "#RRGGBB" so the same flashcards/quiz can render them. */
    public static boolean isColorItem(Item item) {
        return item != null && item.symbol != null && item.symbol.length() == 7
                && item.symbol.charAt(0) == '#';
    }

    public static int colorOf(Item item) {
        try {
            return (int) Long.parseLong(item.symbol.substring(1), 16) | 0xFF000000;
        } catch (NumberFormatException e) {
            return 0xFF000000;
        }
    }

    /** Jawi/Arabic items are shown with the script glyph big and no picture. */
    public static boolean isScriptItem(Item item) {
        if (item == null || item.symbol == null || item.symbol.isEmpty()) {
            return false;
        }
        char c = item.symbol.charAt(0);
        return c >= 0x0600 && c <= 0x06FF;
    }

    public static final List<Item> BODY = new ArrayList<>();
    public static final List<Item> ANIMALS = new ArrayList<>();
    public static final List<Item> PLANTS = new ArrayList<>();
    public static final List<Item> FRUITS = new ArrayList<>();
    public static final List<Item> LIVING = new ArrayList<>();
    public static final List<Item> JAWI = new ArrayList<>();
    public static final List<Item> ARABIC = new ArrayList<>();

    static {
        // ---- Body parts / Anggota badan ----
        add(BODY, "\uD83D\uDC41", "eye", "mata");
        add(BODY, "\uD83D\uDC42", "ear", "telinga");
        add(BODY, "\uD83D\uDC43", "nose", "hidung");
        add(BODY, "\uD83D\uDC44", "mouth", "mulut");
        add(BODY, "\uD83E\uDD32", "hand", "tangan");
        add(BODY, "\uD83E\uDDB6", "leg", "kaki");
        add(BODY, "\uD83E\uDDB7", "tooth", "gigi");
        add(BODY, "\uD83D\uDC45", "tongue", "lidah");
        add(BODY, "\uD83D\uDC87", "hair", "rambut");
        add(BODY, "\uD83E\uDDE0", "brain", "otak");
        add(BODY, "\uD83E\uDEC0", "heart", "jantung");
        add(BODY, "\uD83E\uDDB4", "bone", "tulang");

        // ---- Animals / Haiwan ----
        add(ANIMALS, "\uD83D\uDC31", "cat", "kucing");
        add(ANIMALS, "\uD83D\uDC36", "dog", "anjing");
        add(ANIMALS, "\uD83D\uDC18", "elephant", "gajah");
        add(ANIMALS, "\uD83E\uDD81", "lion", "singa");
        add(ANIMALS, "\uD83D\uDC2F", "tiger", "harimau");
        add(ANIMALS, "\uD83D\uDC2E", "cow", "lembu");
        add(ANIMALS, "\uD83D\uDC37", "pig", "babi");
        add(ANIMALS, "\uD83D\uDC11", "sheep", "kambing biri-biri");
        add(ANIMALS, "\uD83D\uDC30", "rabbit", "arnab");
        add(ANIMALS, "\uD83D\uDC38", "frog", "katak");
        add(ANIMALS, "\uD83D\uDC35", "monkey", "monyet");
        add(ANIMALS, "\uD83D\uDC14", "chicken", "ayam");
        add(ANIMALS, "\uD83E\uDD86", "duck", "itik");
        add(ANIMALS, "\uD83D\uDC1F", "fish", "ikan");
        add(ANIMALS, "\uD83D\uDC26", "bird", "burung");
        add(ANIMALS, "\uD83D\uDC0E", "horse", "kuda");
        add(ANIMALS, "\uD83D\uDC22", "turtle", "kura-kura");
        add(ANIMALS, "\uD83D\uDC1D", "bee", "lebah");
        add(ANIMALS, "\uD83E\uDD8B", "butterfly", "kupu-kupu");
        add(ANIMALS, "\uD83D\uDC0D", "snake", "ular");

        // ---- Plants / Tumbuh-tumbuhan ----
        add(PLANTS, "\uD83C\uDF33", "tree", "pokok");
        add(PLANTS, "\uD83C\uDF38", "flower", "bunga");
        add(PLANTS, "\uD83C\uDF31", "seedling", "anak pokok");
        add(PLANTS, "\uD83C\uDF43", "leaves", "daun");
        add(PLANTS, "\uD83C\uDF35", "cactus", "kaktus");
        add(PLANTS, "\uD83C\uDF37", "tulip", "tulip");
        add(PLANTS, "\uD83C\uDF3B", "sunflower", "bunga matahari");
        add(PLANTS, "\uD83C\uDF3F", "herb", "herba");
        add(PLANTS, "\uD83C\uDF40", "ear of rice", "padi");
        add(PLANTS, "\uD83C\uDF34", "palm tree", "pokok kelapa");

        // ---- Fruits / Buah-buahan ----
        add(FRUITS, "\uD83C\uDF4E", "apple", "epal");
        add(FRUITS, "\uD83C\uDF4C", "banana", "pisang");
        add(FRUITS, "\uD83C\uDF47", "grapes", "anggur");
        add(FRUITS, "\uD83C\uDF4A", "orange", "oren");
        add(FRUITS, "\uD83C\uDF53", "strawberry", "strawberi");
        add(FRUITS, "\uD83C\uDF49", "watermelon", "tembikai");
        add(FRUITS, "\uD83C\uDF4D", "pineapple", "nanas");
        add(FRUITS, "\uD83E\uDD6D", "mango", "mangga");
        add(FRUITS, "\uD83C\uDF51", "peach", "pic");
        add(FRUITS, "\uD83C\uDF52", "cherry", "ceri");
        add(FRUITS, "\uD83C\uDF50", "pear", "pir");
        add(FRUITS, "\uD83E\uDD5D", "kiwi", "kiwi");

        // ---- Living things / Benda hidup (mix of people, animals, plants) ----
        add(LIVING, "\uD83D\uDC66", "boy", "budak lelaki");
        add(LIVING, "\uD83D\uDC67", "girl", "budak perempuan");
        add(LIVING, "\uD83D\uDC31", "cat", "kucing");
        add(LIVING, "\uD83D\uDC36", "dog", "anjing");
        add(LIVING, "\uD83C\uDF33", "tree", "pokok");
        add(LIVING, "\uD83C\uDF38", "flower", "bunga");
        add(LIVING, "\uD83D\uDC1F", "fish", "ikan");
        add(LIVING, "\uD83E\uDD8B", "butterfly", "kupu-kupu");
        add(LIVING, "\uD83D\uDC26", "bird", "burung");
        add(LIVING, "\uD83C\uDF31", "plant", "tumbuhan");

        // ---- Jawi letters (Arabic script for Malay) ----
        addJawi("\u0627", "alif");
        addJawi("\u0628", "ba");
        addJawi("\u062A", "ta");
        addJawi("\u062B", "tha");
        addJawi("\u062C", "jim");
        addJawi("\u0686", "ca");
        addJawi("\u062D", "ha");
        addJawi("\u062E", "kha");
        addJawi("\u062F", "dal");
        addJawi("\u0630", "dzal");
        addJawi("\u0631", "ra");
        addJawi("\u0632", "zai");
        addJawi("\u0633", "sin");
        addJawi("\u0634", "syin");
        addJawi("\u0635", "sad");
        addJawi("\u0636", "dhad");
        addJawi("\u0637", "tha");
        addJawi("\u0638", "zha");
        addJawi("\u0639", "ain");
        addJawi("\u063A", "ghain");
        addJawi("\u0641", "fa");
        addJawi("\u06A4", "pa");
        addJawi("\u0642", "qaf");
        addJawi("\u0643", "kaf");
        addJawi("\u06AC", "ga");
        addJawi("\u0644", "lam");
        addJawi("\u0645", "mim");
        addJawi("\u0646", "nun");
        addJawi("\u0648", "wau");
        addJawi("\u06CF", "va");
        addJawi("\u0647", "ha");
        addJawi("\u064A", "ya");
        addJawi("\u06BD", "nya");

        // ---- Arabic letters ----
        addArabic("\u0627", "alif");
        addArabic("\u0628", "ba");
        addArabic("\u062A", "ta");
        addArabic("\u062B", "tha");
        addArabic("\u062C", "jim");
        addArabic("\u062D", "ha");
        addArabic("\u062E", "kha");
        addArabic("\u062F", "dal");
        addArabic("\u0630", "dzal");
        addArabic("\u0631", "ra");
        addArabic("\u0632", "zai");
        addArabic("\u0633", "sin");
        addArabic("\u0634", "syin");
        addArabic("\u0635", "sad");
        addArabic("\u0636", "dhad");
        addArabic("\u0637", "tha");
        addArabic("\u0638", "zha");
        addArabic("\u0639", "ain");
        addArabic("\u063A", "ghain");
        addArabic("\u0641", "fa");
        addArabic("\u0642", "qaf");
        addArabic("\u0643", "kaf");
        addArabic("\u0644", "lam");
        addArabic("\u0645", "mim");
        addArabic("\u0646", "nun");
        addArabic("\u0647", "ha");
        addArabic("\u0648", "wau");
        addArabic("\u064A", "ya");
    }

    private static void addWord(List<Item> list, String word, String en, String ms, String emoji) {
        list.add(new Item(emoji, en, ms, word));
    }

    private static void addJawi(String glyph, String name) {
        JAWI.add(new Item(glyph, name, name));
    }

    private static void addArabic(String glyph, String name) {
        ARABIC.add(new Item(glyph, name, name));
    }

    private static void add(List<Item> list, String symbol, String en, String ms) {
        list.add(new Item(symbol, en, ms));
    }

    private static void add(List<Item> list, String letter, String en, String ms, String emoji) {
        list.add(new Item(letter + "|" + emoji, en, ms));
    }

    private static void addSentence(String en, String ms) {
        SENTENCES.add(new Sentence(en, ms));
    }

    public static String letterGlyph(Item item) {
        int bar = item.symbol.indexOf('|');
        return bar < 0 ? item.symbol : item.symbol.substring(0, bar);
    }

    public static String letterEmoji(Item item) {
        int bar = item.symbol.indexOf('|');
        return bar < 0 ? "\u2B50" : item.symbol.substring(bar + 1);
    }
}
