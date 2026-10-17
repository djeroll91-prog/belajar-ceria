#!/usr/bin/env python3
"""Author data/kspk.json, the single source of truth for all learning content.

This script exists to generate the first version of that file from compact
tables (the alphabet, the syllable grid) rather than to be a second source of
truth. After it runs, data/kspk.json is maintained by hand and this script is
no longer needed; it is kept so the structure is documented and reproducible.

Content follows the Malaysian preschool curriculum (KSPK, Semakan 2017) and its
six tunjang. Standard codes are recorded only where the official document was
actually checked; `code` is null elsewhere rather than inventing a reference.
"""
import json
from pathlib import Path

ROOT = Path(__file__).resolve().parent.parent
OUT = ROOT / "data/kspk.json"


def it(symbol, en, ms, kind="emoji", word=None, spoken_en=None, spoken_ms=None):
    return {"symbol": symbol, "kind": kind, "en": en, "ms": ms,
            "word": word, "spokenEn": spoken_en, "spokenMs": spoken_ms}


def unit(uid, emoji, en, ms, kind, items, code=None, note=None):
    return {"id": uid, "emoji": emoji, "en": en, "ms": ms, "kind": kind,
            "code": code, "note": note, "items": items}


# ----------------------------------------------------------------------
# Tunjang 1: Komunikasi - Bahasa Melayu
# ----------------------------------------------------------------------

LETTER_PICTURES = [
    ("A", "Apple", "Apel", "\U0001F34E"), ("B", "Ball", "Bola", "\u26BD"),
    ("C", "Cat", "Kucing", "\U0001F431"), ("D", "Duck", "Itik", "\U0001F986"),
    ("E", "Elephant", "Gajah", "\U0001F418"), ("F", "Fish", "Ikan", "\U0001F41F"),
    ("G", "Grapes", "Anggur", "\U0001F347"), ("H", "House", "Rumah", "\U0001F3E0"),
    ("I", "Ice cream", "Ais krim", "\U0001F366"), ("J", "Juice", "Jus", "\U0001F9C3"),
    ("K", "Kite", "Layang-layang", "\U0001FA81"), ("L", "Lion", "Singa", "\U0001F981"),
    ("M", "Moon", "Bulan", "\U0001F319"), ("N", "Nose", "Hidung", "\U0001F443"),
    ("O", "Orange", "Oren", "\U0001F34A"), ("P", "Panda", "Panda", "\U0001F43C"),
    ("Q", "Queen", "Ratu", "\U0001F451"), ("R", "Rabbit", "Arnab", "\U0001F430"),
    ("S", "Sun", "Matahari", "\u2600\uFE0F"), ("T", "Tiger", "Harimau", "\U0001F42F"),
    ("U", "Umbrella", "Payung", "\u2602\uFE0F"), ("V", "Van", "Van", "\U0001F690"),
    ("W", "Whale", "Ikan paus", "\U0001F433"), ("X", "Xylophone", "Xilofon", "\U0001F3BC"),
    ("Y", "Yacht", "Kapal layar", "\u26F5"), ("Z", "Zebra", "Zebra", "\U0001F993"),
]

# Malaysia follows the English letter names (DBP), not the Indonesian ones.
LETTER_NAMES_MS = {
    "A": "ei", "B": "bi", "C": "si", "D": "di", "E": "i", "F": "ef", "G": "ji",
    "H": "eic", "I": "ai", "J": "jei", "K": "kei", "L": "el", "M": "em", "N": "en",
    "O": "o", "P": "pi", "Q": "kiu", "R": "ar", "S": "es", "T": "ti", "U": "yu",
    "V": "vi", "W": "dabliu", "X": "eks", "Y": "wai", "Z": "zet",
}
LETTER_NAMES_EN = {
    "A": "ay", "B": "bee", "C": "see", "D": "dee", "E": "ee", "F": "eff", "G": "gee",
    "H": "aitch", "I": "eye", "J": "jay", "K": "kay", "L": "ell", "M": "em", "N": "en",
    "O": "oh", "P": "pee", "Q": "cue", "R": "ar", "S": "ess", "T": "tee", "U": "you",
    "V": "vee", "W": "double you", "X": "ex", "Y": "why", "Z": "zed",
}

letters = []
for glyph, en, ms, emoji in LETTER_PICTURES:
    letters.append(it(f"{glyph}|{emoji}", en, ms, kind="letter",
                      spoken_en=LETTER_NAMES_EN[glyph], spoken_ms=LETTER_NAMES_MS[glyph]))

VOWELS = [("A", "a", "a"), ("E", "e", "e"), ("I", "i", "i"), ("O", "o", "o"), ("U", "u", "u")]
vowels = [it(g, en, ms, kind="text") for g, en, ms in VOWELS]

CONSONANTS = ["B", "C", "D", "F", "G", "H", "J", "K", "L", "M", "N", "P", "R", "S", "T", "V", "W", "Y", "Z"]
consonants = [it(c, c, c, kind="text") for c in CONSONANTS]

# The syllable grid is how reading is actually taught in Malaysian preschools:
# a consonant joined to each vowel (kaedah KV), then two syllables make a word.
SYLLABLE_CONSONANTS = ["b", "c", "d", "g", "h", "j", "k", "l", "m", "n", "p", "r", "s", "t"]
SYLLABLE_VOWELS = ["a", "i", "u", "e", "o"]
syllables = []
for cons in SYLLABLE_CONSONANTS:
    for vow in SYLLABLE_VOWELS:
        syllables.append(it(f"{cons}{vow}", f"{cons}{vow}", f"{cons}{vow}", kind="text"))

KV_WORDS = [
    ("baba", "baba", "baba", "\U0001F466"), ("baju", "shirt", "baju", "\U0001F455"),
    ("bola", "ball", "bola", "\u26BD"), ("buku", "book", "buku", "\U0001F4D5"),
    ("batu", "stone", "batu", "\U0001FAA8"), ("kaki", "leg", "kaki", "\U0001F9B6"),
    ("kuku", "nail", "kuku", "\U0001F485"), ("mata", "eye", "mata", "\U0001F441"),
    ("meja", "table", "meja", "\U0001FA91"), ("nasi", "rice", "nasi", "\U0001F35A"),
    ("susu", "milk", "susu", "\U0001F95B"), ("roti", "bread", "roti", "\U0001F35E"),
    ("topi", "hat", "topi", "\U0001F452"), ("sapu", "broom", "sapu", "\U0001F9F9"),
    ("gigi", "tooth", "gigi", "\U0001F9B7"), ("jari", "finger", "jari", "\u261D"),
    ("duri", "thorn", "duri", "\U0001F339"), ("kuda", "horse", "kuda", "\U0001F40E"),
    ("lima", "five", "lima", "\u270B"), ("dua", "two", "dua", "\u270C"),
    ("satu", "one", "satu", "\u261D"), ("tiga", "three", "tiga", "\U0001F44C"),
    ("rambut", "hair", "rambut", "\U0001F487"), ("tangan", "hand", "tangan", "\U0001F932"),
]
kv_words = [it(emoji, en, ms, kind="word", word=word) for word, en, ms, emoji in KV_WORDS]

NUMBERS_EN = ["one", "two", "three", "four", "five", "six", "seven", "eight", "nine", "ten",
              "eleven", "twelve", "thirteen", "fourteen", "fifteen", "sixteen", "seventeen",
              "eighteen", "nineteen", "twenty"]
NUMBERS_MS = ["satu", "dua", "tiga", "empat", "lima", "enam", "tujuh", "lapan", "sembilan",
              "sepuluh", "sebelas", "dua belas", "tiga belas", "empat belas", "lima belas",
              "enam belas", "tujuh belas", "lapan belas", "sembilan belas", "dua puluh"]
NUMBER_EMOJI = ["\U0001F34E", "\U0001F34A", "\U0001F353", "\U0001F34C", "\U0001F349", "\U0001F351",
                "\U0001F34D", "\u2B50", "\U0001F388", "\U0001F338", "\U0001F41D", "\U0001F33B",
                "\U0001F340", "\U0001F36A", "\U0001F369", "\U0001F36D", "\U0001F381",
                "\U0001F32C\uFE0F", "\U0001F423", "\U0001F31F"]
numbers = [it(str(i + 1), NUMBERS_EN[i], NUMBERS_MS[i], kind="number", word=NUMBER_EMOJI[i])
           for i in range(20)]

READ_WORDS = [
    ("cat", "cat", "kucing", "\U0001F408"), ("dog", "dog", "anjing", "\U0001F415"),
    ("sun", "sun", "matahari", "\u2600\uFE0F"), ("hat", "hat", "topi", "\U0001F452"),
    ("bus", "bus", "bas", "\U0001F68C"), ("cup", "cup", "cawan", "\U0001F95B"),
    ("pen", "pen", "pen", "\U0001F58A\uFE0F"), ("bed", "bed", "katil", "\U0001F6CF\uFE0F"),
    ("star", "star", "bintang", "\u2B50"), ("moon", "moon", "bulan", "\U0001F319"),
    ("fish", "fish", "ikan", "\U0001F41F"), ("bird", "bird", "burung", "\U0001F426"),
]
en_words = [it(emoji, en, ms, kind="word", word=word) for word, en, ms, emoji in READ_WORDS]

MS_WORDS = [
    ("ibu", "mother", "ibu", "\U0001F469"), ("ayah", "father", "ayah", "\U0001F468"),
    ("kaki", "leg", "kaki", "\U0001F9B6"), ("baju", "shirt", "baju", "\U0001F455"),
    ("buku", "book", "buku", "\U0001F4D5"), ("meja", "table", "meja", "\U0001FA91"),
    ("susu", "milk", "susu", "\U0001F95B"), ("bola", "ball", "bola", "\u26BD"),
    ("mata", "eye", "mata", "\U0001F441"), ("nasi", "rice", "nasi", "\U0001F35A"),
    ("rumah", "house", "rumah", "\U0001F3E0"), ("bunga", "flower", "bunga", "\U0001F338"),
]
ms_words = [it(emoji, en, ms, kind="word", word=word) for word, en, ms, emoji in MS_WORDS]

SENTENCES = [
    ("The cat is big.", "Kucing itu besar."),
    ("I like to play.", "Saya suka bermain."),
    ("The sun is hot.", "Matahari sangat panas."),
    ("My ball is red.", "Bola saya berwarna merah."),
    ("I can read.", "Saya boleh membaca."),
    ("The fish can swim.", "Ikan boleh berenang."),
    ("I love my mother.", "Saya sayang ibu saya."),
    ("This is my book.", "Ini buku saya."),
    ("Saya suka makan nasi.", "Saya suka makan nasi."),
    ("Ibu saya masak di dapur.", "Ibu saya masak di dapur."),
    ("Ayah pergi bekerja.", "Ayah pergi bekerja."),
    ("Kami bermain di taman.", "Kami bermain di taman."),
    ("Ini rumah saya.", "Ini rumah saya."),
    ("Saya ada dua tangan.", "Saya ada dua tangan."),
    ("Terima kasih, cikgu.", "Terima kasih, cikgu."),
    ("Selamat pagi, kawan.", "Selamat pagi, kawan."),
]

# ----------------------------------------------------------------------
# Tunjang 5: Matematik Awal - shapes, sizes, patterns, money, time
# ----------------------------------------------------------------------

SHAPES = [
    ("\U0001F535", "circle", "bulat"), ("\U0001F7E6", "square", "segi empat sama"),
    ("\U0001F53A", "triangle", "segi tiga"), ("\U0001F7E8", "rectangle", "segi empat tepat"),
    ("\u2B50", "star", "bintang"), ("\U0001F49A", "heart", "hati"),
    ("\U0001F7E0", "oval", "bujur"), ("\U0001F4A0", "diamond", "diamond"),
]
shapes = [it(s, en, ms) for s, en, ms in SHAPES]

SIZES = [
    ("\U0001F418", "big", "besar"), ("\U0001F42D", "small", "kecil"),
    ("\U0001F4CF", "long", "panjang"), ("\U0001F4D0", "short", "pendek"),
    ("\U0001F4E6", "heavy", "berat"), ("\U0001F388", "light", "ringan"),
]
sizes = [it(s, en, ms) for s, en, ms in SIZES]

PATTERN_ITEMS = [
    ("\U0001F534", "red", "merah"), ("\U0001F535", "blue", "biru"),
    ("\U0001F7E1", "yellow", "kuning"), ("\U0001F7E2", "green", "hijau"),
]
patterns = [it(s, en, ms) for s, en, ms in PATTERN_ITEMS]

MONEY = [
    ("5", "five sen", "lima sen"), ("10", "ten sen", "sepuluh sen"),
    ("20", "twenty sen", "dua puluh sen"), ("50", "fifty sen", "lima puluh sen"),
    ("100", "one ringgit", "satu ringgit"),
]
money = [it(s, en, ms, kind="number") for s, en, ms in MONEY]

TIME = [
    ("\U0001F305", "morning", "pagi"), ("\U0001F31E", "afternoon", "tengah hari"),
    ("\U0001F307", "evening", "petang"), ("\U0001F31C", "night", "malam"),
    ("\U0001F4C5", "today", "hari ini"), ("\u23F0", "clock", "jam"),
    ("\U0001F4C6", "day", "hari"),
]
time_items = [it(s, en, ms) for s, en, ms in TIME]

# ----------------------------------------------------------------------
# Tunjang 5: Sains Awal
# ----------------------------------------------------------------------

SENSES = [
    ("\U0001F441", "see", "lihat", "eye", "mata"), ("\U0001F442", "hear", "dengar", "ear", "telinga"),
    ("\U0001F443", "smell", "hidu", "nose", "hidung"), ("\U0001F445", "taste", "rasa", "tongue", "lidah"),
    ("\U0001F932", "touch", "sentuh", "hand", "tangan"),
]
senses = [it(s, en, ms, word=w) for s, en, ms, w, _ in SENSES]

WEATHER = [
    ("\u2600\uFE0F", "sunny", "cerah"), ("\U0001F327\uFE0F", "rainy", "hujan"),
    ("\u2601\uFE0F", "cloudy", "berawan"), ("\U0001F32C\uFE0F", "windy", "berangin"),
    ("\u26C8\uFE0F", "storm", "ribut"), ("\U0001F308", "rainbow", "pelangi"),
]
weather = [it(s, en, ms) for s, en, ms in WEATHER]

WATER = [
    ("\U0001F4A7", "water", "air"), ("\U0001F9CA", "ice", "ais"),
    ("\u2601\uFE0F", "cloud", "awan"), ("\U0001F30A", "wave", "ombak"),
]
water_items = [it(s, en, ms) for s, en, ms in WATER]

LIGHT = [
    ("\U0001F4A1", "bright", "terang"), ("\U0001F311", "dark", "gelap"),
    ("\U0001F56F\uFE0F", "candle", "lilin"), ("\U0001F50C", "electricity", "elektrik"),
]
light = [it(s, en, ms) for s, en, ms in LIGHT]

BODY = [
    ("\U0001F441", "eye", "mata"), ("\U0001F442", "ear", "telinga"),
    ("\U0001F443", "nose", "hidung"), ("\U0001F444", "mouth", "mulut"),
    ("\U0001F932", "hand", "tangan"), ("\U0001F9B6", "leg", "kaki"),
    ("\U0001F9B7", "tooth", "gigi"), ("\U0001F445", "tongue", "lidah"),
    ("\U0001F487", "hair", "rambut"), ("\U0001F9E0", "brain", "otak"),
    ("\U0001FAC0", "heart", "jantung"), ("\U0001F9B4", "bone", "tulang"),
]
body = [it(s, en, ms) for s, en, ms in BODY]

ANIMALS = [
    ("\U0001F431", "cat", "kucing"), ("\U0001F415", "dog", "anjing"),
    ("\U0001F418", "elephant", "gajah"), ("\U0001F981", "lion", "singa"),
    ("\U0001F42F", "tiger", "harimau"), ("\U0001F42E", "cow", "lembu"),
    ("\U0001F411", "sheep", "kambing biri-biri"), ("\U0001F430", "rabbit", "arnab"),
    ("\U0001F438", "frog", "katak"), ("\U0001F435", "monkey", "monyet"),
    ("\U0001F414", "chicken", "ayam"), ("\U0001F986", "duck", "itik"),
    ("\U0001F41F", "fish", "ikan"), ("\U0001F426", "bird", "burung"),
    ("\U0001F40E", "horse", "kuda"), ("\U0001F422", "turtle", "kura-kura"),
    ("\U0001F41D", "bee", "lebah"), ("\U0001F98B", "butterfly", "kupu-kupu"),
    ("\U0001F40D", "snake", "ular"), ("\U0001F418", "deer", "rusa"),
]
animals = [it(s, en, ms) for s, en, ms in ANIMALS]

PLANTS = [
    ("\U0001F333", "tree", "pokok"), ("\U0001F338", "flower", "bunga"),
    ("\U0001F331", "seedling", "anak pokok"), ("\U0001F343", "leaves", "daun"),
    ("\U0001F335", "cactus", "kaktus"), ("\U0001F337", "tulip", "tulip"),
    ("\U0001F33B", "sunflower", "bunga matahari"), ("\U0001F33F", "herb", "herba"),
    ("\U0001F340", "ear of rice", "padi"), ("\U0001F334", "palm tree", "pokok kelapa"),
]
plants = [it(s, en, ms) for s, en, ms in PLANTS]

FRUITS = [
    ("\U0001F34E", "apple", "epal"), ("\U0001F34C", "banana", "pisang"),
    ("\U0001F347", "grapes", "anggur"), ("\U0001F34A", "orange", "oren"),
    ("\U0001F353", "strawberry", "strawberi"), ("\U0001F349", "watermelon", "tembikai"),
    ("\U0001F34D", "pineapple", "nanas"), ("\U0001F96D", "mango", "mangga"),
    ("\U0001F351", "peach", "pic"), ("\U0001F352", "cherry", "ceri"),
    ("\U0001F350", "pear", "pir"), ("\U0001F95D", "kiwi", "kiwi"),
]
fruits = [it(s, en, ms) for s, en, ms in FRUITS]

LIVING = [
    ("\U0001F466", "boy", "budak lelaki"), ("\U0001F467", "girl", "budak perempuan"),
    ("\U0001F431", "cat", "kucing"), ("\U0001F415", "dog", "anjing"),
    ("\U0001F333", "tree", "pokok"), ("\U0001F338", "flower", "bunga"),
    ("\U0001F41F", "fish", "ikan"), ("\U0001F98B", "butterfly", "kupu-kupu"),
    ("\U0001F426", "bird", "burung"), ("\U0001F331", "plant", "tumbuhan"),
]
living = [it(s, en, ms) for s, en, ms in LIVING]

# ----------------------------------------------------------------------
# Tunjang 3: Kemanusiaan - family, friends, school, home, jobs, country
# ----------------------------------------------------------------------

FAMILY = [
    ("\U0001F468", "father", "ayah"), ("\U0001F469", "mother", "ibu"),
    ("\U0001F466", "brother", "abang"), ("\U0001F467", "sister", "kakak"),
    ("\U0001F476", "baby", "adik bayi"), ("\U0001F474", "grandfather", "atuk"),
    ("\U0001F475", "grandmother", "nenek"), ("\U0001F46A", "family", "keluarga"),
]
family = [it(s, en, ms) for s, en, ms in FAMILY]

FRIENDS = [
    ("\U0001F91D", "friend", "kawan"), ("\U0001F44B", "greeting", "salam"),
    ("\U0001F917", "hug", "peluk"), ("\U0001F604", "smile", "senyum"),
    ("\U0001F3E1", "neighbour", "jiran"),
]
friends = [it(s, en, ms) for s, en, ms in FRIENDS]

SCHOOL = [
    ("\U0001F3EB", "school", "sekolah"), ("\U0001F9D1\u200D\U0001F3EB", "teacher", "cikgu"),
    ("\U0001F4DA", "books", "buku"), ("\u270F\uFE0F", "pencil", "pensel"),
    ("\U0001F392", "bag", "beg"), ("\U0001F3AF", "classroom", "kelas"),
    ("\U0001F3C0", "playground", "taman permainan"), ("\U0001F514", "bell", "loceng"),
]
school = [it(s, en, ms) for s, en, ms in SCHOOL]

HOUSE = [
    ("\U0001F3E0", "house", "rumah"), ("\U0001F6AA", "door", "pintu"),
    ("\U0001FA9F", "window", "tingkap"), ("\U0001F6CF\uFE0F", "bed", "katil"),
    ("\U0001FA91", "table", "meja"), ("\U0001FA91", "chair", "kerusi"),
    ("\U0001F6BF", "bathroom", "bilik air"), ("\U0001F373", "kitchen", "dapur"),
]
house = [it(s, en, ms) for s, en, ms in HOUSE]

JOBS = [
    ("\U0001F46E", "police officer", "polis"), ("\U0001F692", "firefighter", "bomba"),
    ("\U0001F469\u200D\u2695\uFE0F", "doctor", "doktor"), ("\U0001F469\u200D\U0001F3EB", "teacher", "cikgu"),
    ("\U0001F468\u200D\U0001F33E", "farmer", "petani"), ("\U0001F468\u200D\U0001F373", "cook", "tukang masak"),
    ("\U0001F468\u200D\U0001F527", "mechanic", "mekanik"), ("\U0001F469\u200D\U0001F4BC", "office worker", "pekerja pejabat"),
    ("\U0001F468\u200D\u2708\uFE0F", "pilot", "juruterbang"), ("\U0001F469\u200D\U0001F3A4", "singer", "penyanyi"),
]
jobs = [it(s, en, ms) for s, en, ms in JOBS]

COUNTRY = [
    ("\U0001F1F2\U0001F1FE", "Malaysia", "Malaysia"), ("\U0001F3F3\uFE0F", "flag", "bendera"),
    ("\U0001F451", "king", "raja"), ("\U0001F3DB\uFE0F", "mosque", "masjid"),
    ("\U0001F3EF", "temple", "tokong"), ("\U0001F3E5", "hospital", "hospital"),
]
country = [it(s, en, ms) for s, en, ms in COUNTRY]

VEHICLES = [
    ("\U0001F697", "car", "kereta"), ("\U0001F68C", "bus", "bas"),
    ("\U0001F692", "fire engine", "kereta bomba"), ("\U0001F691", "ambulance", "ambulans"),
    ("\U0001F6B2", "bicycle", "basikal"), ("\U0001F6B5", "motorcycle", "motosikal"),
    ("\u2708\uFE0F", "aeroplane", "kapal terbang"), ("\U0001F6A2", "ship", "kapal"),
    ("\U0001F686", "train", "kereta api"),
]
vehicles = [it(s, en, ms) for s, en, ms in VEHICLES]

# ----------------------------------------------------------------------
# Tunjang 6: Keterampilan Diri / sosioemosi, and Tunjang 2: nilai
# ----------------------------------------------------------------------

EMOTIONS = [
    ("\U0001F604", "happy", "gembira"), ("\U0001F622", "sad", "sedih"),
    ("\U0001F620", "angry", "marah"), ("\U0001F628", "afraid", "takut"),
    ("\U0001F634", "tired", "letih"), ("\U0001F914", "curious", "ingin tahu"),
    ("\U0001F970", "proud", "bangga"), ("\U0001F60C", "calm", "tenang"),
]
emotions = [it(s, en, ms) for s, en, ms in EMOTIONS]

HYGIENE = [
    ("\U0001F9FC", "wash hands", "basuh tangan"), ("\U0001FAA5", "brush teeth", "gosok gigi"),
    ("\U0001F6C1", "bath", "mandi"), ("\U0001F4A4", "sleep", "tidur"),
    ("\U0001F374", "eat", "makan"), ("\U0001F9F4", "comb hair", "sikat rambut"),
]
hygiene = [it(s, en, ms) for s, en, ms in HYGIENE]

SAFETY = [
    ("\U0001F6AB", "do not touch", "jangan sentuh"), ("\U0001F697", "hold hands", "pegang tangan"),
    ("\U0001F6B6", "walk carefully", "jalan berhati-hati"), ("\U0001F525", "hot", "panas"),
    ("\u26A0\uFE0F", "dangerous", "bahaya"), ("\U0001F6A6", "traffic light", "lampu isyarat"),
]
safety = [it(s, en, ms) for s, en, ms in SAFETY]

MANNERS = [
    ("\U0001F64F", "thank you", "terima kasih"), ("\U0001F647", "sorry", "minta maaf"),
    ("\U0001F44B", "good morning", "selamat pagi"), ("\U0001F91D", "please", "tolong"),
    ("\U0001F44D", "yes", "ya"), ("\U0001F645", "excuse me", "maafkan saya"),
]
manners = [it(s, en, ms) for s, en, ms in MANNERS]

VALUES = [
    ("\u2764\uFE0F", "love", "sayang"), ("\U0001F91D", "help", "tolong-menolong"),
    ("\U0001F4AA", "hardworking", "rajin"), ("\U0001F91D", "honest", "jujur"),
    ("\U0001F64F", "respect", "hormat"), ("\U0001F91D", "share", "berkongsi"),
    ("\U0001F9F9", "clean", "bersih"), ("\u23F0", "patient", "sabar"),
]
values = [it(s, en, ms) for s, en, ms in VALUES]

INSTRUMENTS = [
    ("\U0001F3B9", "piano", "piano"), ("\U0001F3B8", "guitar", "gitar"),
    ("\U0001F941", "drum", "dram"), ("\U0001F3BA", "trumpet", "trompet"),
    ("\U0001F3BB", "violin", "biola"), ("\U0001F3B5", "flute", "seruling"),
]
instruments = [it(s, en, ms) for s, en, ms in INSTRUMENTS]

TECHNOLOGY = [
    ("\U0001F4BB", "computer", "komputer"), ("\U0001F4F1", "phone", "telefon"),
    ("\U0001F4FA", "television", "televisyen"), ("\U0001F5B1\uFE0F", "mouse", "tetikus"),
    ("\u2328\uFE0F", "keyboard", "papan kekunci"), ("\U0001F4F7", "camera", "kamera"),
]
technology = [it(s, en, ms) for s, en, ms in TECHNOLOGY]

# Colours keep the exact hex symbols the colouring game already uses.
SWATCHES = [
    (0xFFFF5252, "red", "merah"), (0xFFFF9800, "orange", "jingga"),
    (0xFFFFEB3B, "yellow", "kuning"), (0xFF66BB6A, "green", "hijau"),
    (0xFF42A5F5, "blue", "biru"), (0xFFAB47BC, "purple", "ungu"),
    (0xFFFF80AB, "pink", "merah jambu"), (0xFF8D6E63, "brown", "coklat"),
    (0xFF90A4AE, "grey", "kelabu"), (0xFF212121, "black", "hitam"),
    (0xFFFFFFFF, "white", "putih"),
]
colours = [it("#%06X" % (c & 0xFFFFFF), en, ms, kind="color")
           for c, en, ms in SWATCHES]

# Jawi and Arabic keep their exact glyphs and names so the spoken forms match.
JAWI_GLYPHS = ["\u0627", "\u0628", "\u062A", "\u062B", "\u062C", "\u0686", "\u062D", "\u062E",
               "\u062F", "\u0630", "\u0631", "\u0632", "\u0633", "\u0634", "\u0635", "\u0636",
               "\u0637", "\u0638", "\u0639", "\u063A", "\u06A0", "\u0641", "\u06A4", "\u0642",
               "\u0643", "\u06AC", "\u0644", "\u0645", "\u0646", "\u0648", "\u06CF", "\u0647",
               "\u064A", "\u06BD", "\u0621", "\u0649"]
JAWI_NAMES = ["alif", "ba", "ta", "sa", "jim", "ca", "ha", "kha", "dal", "zal", "ra", "zai",
              "sin", "syin", "sad", "dad", "to", "zho", "ain", "ghain", "nga", "fa", "pa",
              "qaf", "kaf", "ga", "lam", "mim", "nun", "wau", "va", "ha", "ya", "nya",
              "hamzah", "ya"]
jawi = [it(g, n, n, kind="script") for g, n in zip(JAWI_GLYPHS, JAWI_NAMES)]

ARABIC_GLYPHS = ["\u0627", "\u0628", "\u062A", "\u062B", "\u062C", "\u062D", "\u062E", "\u062F",
                 "\u0630", "\u0631", "\u0632", "\u0633", "\u0634", "\u0635", "\u0636", "\u0637",
                 "\u0638", "\u0639", "\u063A", "\u0641", "\u0642", "\u0643", "\u0644", "\u0645",
                 "\u0646", "\u0647", "\u0648", "\u064A"]
ARABIC_NAMES = ["alif", "ba", "ta", "tha", "jim", "ha", "kha", "dal", "dzal", "ra", "zai",
                "sin", "syin", "sad", "dhad", "tha", "zha", "ain", "ghain", "fa", "qaf",
                "kaf", "lam", "mim", "nun", "ha", "wau", "ya"]
arabic = [it(g, n, n, kind="script") for g, n in zip(ARABIC_GLYPHS, ARABIC_NAMES)]

# ----------------------------------------------------------------------
# The six tunjang, with units. Codes appear only where the KSPK document
# itself was read; everything else carries a note saying it needs checking
# against the official DSKP.
# ----------------------------------------------------------------------

TUNJANG = [
    {
        "id": "komunikasi", "emoji": "\U0001F5E3\uFE0F", "en": "Communication",
        "ms": "Komunikasi", "color": "#E8877A",
        "units": [
            unit("bm-huruf", "\U0001F524", "Letters A-Z", "Huruf A-Z", "letter", letters,
                 code="BM 3.1", note="Recognising and naming letters"),
            unit("bm-vokal", "\U0001F170\uFE0F", "Vowels", "Huruf Vokal", "text", vowels,
                 note="a e i o u"),
            unit("bm-konsonan", "\U0001F520", "Consonants", "Huruf Konsonan", "text", consonants),
            unit("bm-suku-kata", "\U0001F9F1", "Syllables", "Suku Kata", "text", syllables,
                 note="Kaedah KV: consonant + vowel, the way reading is taught"),
            unit("bm-suku-kata-kata", "\U0001F4D6", "Build Words", "Bina Perkataan", "word", kv_words,
                 note="Two syllables joined: ba + ju = baju"),
            unit("bm-perkataan", "\U0001F4D5", "Words", "Perkataan", "word", ms_words),
            unit("bm-ayat", "\U0001F4DD", "Sentences", "Ayat", "sentence", SENTENCES),
            unit("bi-letters", "\U0001F524", "English Letters", "Huruf Inggeris", "letter", letters,
                 code="BI 2.1.1", note="Print carries meaning; letters have names"),
            unit("bi-cvc", "\U0001F4D8", "Reading Words", "Baca Perkataan", "word", en_words,
                 code="BI 3.2", note="Sounding out simple words, e.g. c-u-p"),
            unit("bi-sentences", "\U0001F4D7", "English Sentences", "Ayat Inggeris", "sentence", SENTENCES),
            unit("bm-jawi", "\u0645", "Jawi Letters", "Huruf Jawi", "script", jawi,
                 note="Jawi is part of Bahasa Melayu; names follow the KSSR/DBP reading"),
        ],
    },
    {
        "id": "kerohanian", "emoji": "\U0001F64F", "en": "Values and Manners",
        "ms": "Kerohanian, Sikap dan Nilai", "color": "#9A8FCF",
        "units": [
            unit("nilai-adab", "\U0001F64F", "Polite Words", "Ucapan Bertatasusila", "emoji", manners,
                 code="BM 1.2.3", note="Responding politely to greetings and instructions"),
            unit("nilai-murni", "\u2764\uFE0F", "Good Values", "Nilai Murni", "emoji", values),
            unit("nilai-arab", "\u0639", "Arabic Letters", "Huruf Arab", "script", arabic),
        ],
    },
    {
        "id": "kemanusiaan", "emoji": "\U0001F46A", "en": "People Around Me",
        "ms": "Kemanusiaan", "color": "#E39BB5",
        "units": [
            unit("hum-keluarga", "\U0001F46A", "My Family", "Keluarga Saya", "emoji", family),
            unit("hum-kawan", "\U0001F91D", "My Friends", "Kawan Saya", "emoji", friends),
            unit("hum-sekolah", "\U0001F3EB", "My School", "Sekolah Saya", "emoji", school),
            unit("hum-rumah", "\U0001F3E0", "My House", "Rumah Saya", "emoji", house),
            unit("hum-pekerjaan", "\U0001F477", "Jobs", "Pekerjaan", "emoji", jobs),
            unit("hum-negara", "\U0001F1F2\U0001F1FE", "My Country", "Negara Saya", "emoji", country),
        ],
    },
    {
        "id": "fizikal", "emoji": "\U0001F3A8", "en": "Body, Art and Music",
        "ms": "Perkembangan Fizikal dan Estetika", "color": "#F2B457",
        "units": [
            unit("fiz-badan", "\U0001F441", "My Body", "Anggota Badan", "emoji", body),
            unit("fiz-warna", "\U0001F3A8", "Colours", "Warna", "color", colours),
            unit("fiz-muzik", "\U0001F3B5", "Music", "Muzik", "emoji", instruments),
        ],
    },
    {
        "id": "sains", "emoji": "\U0001F52C", "en": "Science and Maths",
        "ms": "Sains dan Teknologi", "color": "#7FB4D9",
        "units": [
            unit("ma-nombor", "\U0001F522", "Numbers 1-20", "Nombor 1-20", "number", numbers,
                 code="MA 2.1.1", note="Stating numbers 1-10 in sequence"),
            unit("ma-bentuk", "\U0001F535", "Shapes", "Bentuk", "emoji", shapes),
            unit("ma-saiz", "\U0001F4CF", "Size", "Saiz", "emoji", sizes),
            unit("ma-pola", "\U0001F9E9", "Patterns", "Pola", "emoji", patterns),
            unit("ma-wang", "\U0001F4B0", "Money", "Wang", "number", money),
            unit("ma-masa", "\u23F0", "Time", "Masa", "emoji", time_items),
            unit("sa-deria", "\U0001F590\uFE0F", "My Senses", "Deria Saya", "emoji", senses),
            unit("sa-haiwan", "\U0001F43E", "Animals", "Haiwan", "emoji", animals),
            unit("sa-tumbuhan", "\U0001F331", "Plants", "Tumbuh-tumbuhan", "emoji", plants),
            unit("sa-buah", "\U0001F34E", "Fruits", "Buah-buahan", "emoji", fruits),
            unit("sa-cuaca", "\U0001F327\uFE0F", "Weather", "Cuaca", "emoji", weather),
            unit("sa-air", "\U0001F4A7", "Water", "Air", "emoji", water_items),
            unit("sa-terang", "\U0001F4A1", "Light and Dark", "Terang dan Gelap", "emoji", light),
            unit("sa-hidup", "\U0001F338", "Living Things", "Benda Hidup", "emoji", living),
            unit("ict-asas", "\U0001F4BB", "Using a Computer", "Penggunaan ICT", "emoji", technology),
        ],
    },
    {
        "id": "keterampilan", "emoji": "\U0001F31F", "en": "Self and Safety",
        "ms": "Keterampilan Diri", "color": "#7FC7C0",
        "units": [
            unit("dir-emosi", "\U0001F604", "My Feelings", "Emosi Saya", "emoji", emotions),
            unit("dir-bersih", "\U0001F9FC", "Staying Clean", "Kebersihan Diri", "emoji", hygiene),
            unit("dir-selamat", "\u26A0\uFE0F", "Staying Safe", "Keselamatan Diri", "emoji", safety),
            unit("dir-kenderaan", "\U0001F697", "Vehicles", "Kenderaan", "emoji", vehicles),
        ],
    },
]

# Weekly themes, the way a Malaysian preschool year is actually organised.
THEMES = [
    (1, "Orientation", "Orientasi", ""),
    (2, "Myself", "Diri Saya", ""),
    (3, "My Body", "Anggota Badan", ""),
    (4, "My Family", "Keluarga Saya", ""),
    (5, "My Country", "Negara Saya", "Chinese New Year"),
    (6, "My School", "Sekolah Saya", ""),
    (7, "My Friends", "Kawan Saya", ""),
    (8, "My Neighbours", "Jiran Saya", ""),
    (9, "Keeping Clean", "Kebersihan Diri", ""),
    (10, "Staying Safe", "Keselamatan Diri", ""),
    (11, "My House", "Rumah Saya", ""),
    (12, "My Feelings", "Emosi Diri", ""),
    (13, "Food", "Makanan", ""),
    (14, "Water", "Air", ""),
    (15, "Sand", "Pasir", ""),
    (16, "Clothes", "Pakaian", ""),
    (17, "Tools", "Peralatan", ""),
    (18, "Land Vehicles", "Kenderaan Darat", ""),
    (19, "Air Vehicles", "Kenderaan Udara", ""),
    (20, "Water Vehicles", "Kenderaan Air", ""),
    (21, "Public Places", "Kemudahan Awam", ""),
    (22, "Pets", "Haiwan Jinak", ""),
    (23, "Wild Animals", "Haiwan Liar", ""),
    (24, "Sea Animals", "Haiwan Air", ""),
    (25, "Insects", "Serangga", ""),
    (26, "Plants", "Tumbuh-tumbuhan", ""),
    (27, "Vegetables", "Sayur-sayuran", ""),
    (28, "Fruits", "Buah-buahan", ""),
    (29, "Flowers", "Bunga", ""),
    (30, "Caring for Nature", "Sayangi Alam Sekitar", ""),
    (31, "My Country", "Negara Saya", "Independence Day"),
    (32, "Hari Raya", "Hari Raya Aidilfitri", ""),
    (33, "Weather", "Cuaca", ""),
    (34, "Uniformed Jobs", "Pekerjaan Beruniform", ""),
    (35, "Other Jobs", "Pekerjaan Tidak Beruniform", ""),
    (36, "Building", "Pembinaan", ""),
    (37, "Traditional Songs", "Lagu dan Muzik Tradisional", ""),
    (38, "Craft", "Seni Kraf", ""),
    (39, "Deepavali", "Deepavali", ""),
    (40, "Traditional Games", "Permainan Tradisional", ""),
    (41, "Ready for Year One", "Persediaan ke Tahun Satu", "Graduation"),
    (42, "Transition", "Transisi ke Tahun Satu", ""),
]

DOC = {
    "meta": {
        "app": "Belajar Ceria",
        "curriculum": "KSPK (Semakan 2017), Kementerian Pendidikan Malaysia",
        "ages": "4+ and 5+",
        "languages": ["ms", "en"],
        "note": ("Codes are recorded only where the KSPK document was read directly. "
                 "A null code means the unit is placed by tunjang and theme, and should be "
                 "checked against the official DSKP before being presented as a standard."),
    },
    "tunjang": TUNJANG,
    "themes": [{"week": w, "en": en, "ms": ms, "note": note} for w, en, ms, note in THEMES],
}


def main():
    OUT.parent.mkdir(parents=True, exist_ok=True)
    # Sentences travel as objects so the generator has one shape to handle.
    for tunjang in TUNJANG:
        for u in tunjang["units"]:
            if u["kind"] == "sentence":
                u["items"] = [{"en": en, "ms": ms} for en, ms in u["items"]]
    OUT.write_text(json.dumps(DOC, ensure_ascii=False, indent=1) + "\n", encoding="utf-8")
    total_units = sum(len(t["units"]) for t in TUNJANG)
    total_items = sum(len(u["items"]) for t in TUNJANG for u in t["units"])
    print(f"wrote {OUT.relative_to(ROOT)}")
    print(f"tunjang: {len(TUNJANG)}  units: {total_units}  items: {total_items}  themes: {len(THEMES)}")


if __name__ == "__main__":
    main()
