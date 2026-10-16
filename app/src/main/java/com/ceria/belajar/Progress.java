package com.ceria.belajar;

import android.content.Context;
import android.content.SharedPreferences;

import java.util.ArrayList;
import java.util.List;

/** Stores points, stars, badges and the saved language. */
public class Progress {

    public static class Badge {
        public final String id;
        public final String emoji;
        public final String en;
        public final String ms;
        public final int target;

        public Badge(String id, String emoji, String en, String ms, int target) {
            this.id = id;
            this.emoji = emoji;
            this.en = en;
            this.ms = ms;
            this.target = target;
        }

        public String title(Content.Lang lang) {
            return lang == Content.Lang.MS ? ms : en;
        }
    }

    private static final String PREFS = "belajar_ceria_prefs";
    private static final String KEY_LANG = "lang";
    private static final String KEY_POINTS = "points";
    private static final String KEY_STARS = "stars";
    private static final String KEY_CORRECT = "correct";
    private static final String KEY_SPOKEN = "spoken";
    private static final String KEY_STREAK = "streak";
    private static final String KEY_BEST_STREAK = "best_streak";
    private static final String KEY_GAMES = "games_played";

    private final SharedPreferences prefs;

    public Progress(Context context) {
        prefs = context.getApplicationContext().getSharedPreferences(PREFS, Context.MODE_PRIVATE);
    }

    public Content.Lang getLang() {
        return Content.Lang.MS.name().equals(prefs.getString(KEY_LANG, Content.Lang.MS.name()))
                ? Content.Lang.MS : Content.Lang.EN;
    }

    public void setLang(Content.Lang lang) {
        prefs.edit().putString(KEY_LANG, lang.name()).apply();
    }

    public int points() {
        return prefs.getInt(KEY_POINTS, 0);
    }

    public int stars() {
        return prefs.getInt(KEY_STARS, 0);
    }

    public int correct() {
        return prefs.getInt(KEY_CORRECT, 0);
    }

    public int spoken() {
        return prefs.getInt(KEY_SPOKEN, 0);
    }

    public int streak() {
        return prefs.getInt(KEY_STREAK, 0);
    }

    public int bestStreak() {
        return prefs.getInt(KEY_BEST_STREAK, 0);
    }

    public int gamesPlayed() {
        return prefs.getInt(KEY_GAMES, 0);
    }

    public int level() {
        return 1 + points() / 100;
    }

    /** Points within the current level (0..99). */
    public int levelProgress() {
        return points() % 100;
    }

    public void addPoints(int amount) {
        prefs.edit().putInt(KEY_POINTS, points() + amount).apply();
    }

    public void addStar() {
        prefs.edit().putInt(KEY_STARS, stars() + 1).apply();
    }

    public void addStars(int count) {
        prefs.edit().putInt(KEY_STARS, stars() + count).apply();
    }

    public void addCorrect() {
        prefs.edit().putInt(KEY_CORRECT, correct() + 1).apply();
    }

    public void addSpoken() {
        prefs.edit().putInt(KEY_SPOKEN, spoken() + 1).apply();
    }

    public void registerGamePlayed() {
        prefs.edit().putInt(KEY_GAMES, gamesPlayed() + 1).apply();
    }

    public void onCorrectAnswer() {
        int streak = streak() + 1;
        int best = Math.max(bestStreak(), streak);
        prefs.edit()
                .putInt(KEY_STREAK, streak)
                .putInt(KEY_BEST_STREAK, best)
                .apply();
    }

    public void onWrongAnswer() {
        prefs.edit().putInt(KEY_STREAK, 0).apply();
    }

    public boolean hasBadge(Badge badge) {
        return prefs.getBoolean("badge_" + badge.id, false);
    }

    public void unlockBadge(Badge badge) {
        prefs.edit().putBoolean("badge_" + badge.id, true).apply();
    }

    // ------------------------------------------------------------------
    // Level progression. A level only opens once the child has passed the
    // previous one, so nobody can skip ahead without learning.
    // ------------------------------------------------------------------

    public int levelBest(int levelId) {
        return prefs.getInt("level_best_" + levelId, 0);
    }

    /** Records a level attempt and returns true when the score is the new best. */
    public boolean recordLevelScore(int levelId, int score) {
        if (score > levelBest(levelId)) {
            prefs.edit().putInt("level_best_" + levelId, score).apply();
            return true;
        }
        return false;
    }

    public boolean isLevelPassed(int levelId, int required) {
        return levelBest(levelId) >= required;
    }

    public int highestUnlocked() {
        return prefs.getInt("highest_unlocked", 1);
    }

    public void unlockLevel(int levelId) {
        if (levelId > highestUnlocked()) {
            prefs.edit().putInt("highest_unlocked", levelId).apply();
        }
    }

    public boolean isUnlocked(int levelId) {
        return levelId <= highestUnlocked();
    }

    public int levelsPassed(List<Integer> levelIds, List<Integer> required) {
        int passed = 0;
        for (int i = 0; i < levelIds.size() && i < required.size(); i++) {
            if (isLevelPassed(levelIds.get(i), required.get(i))) {
                passed++;
            }
        }
        return passed;
    }

    /** Simple mastery percentage across all levels, used for the trend display. */
    public int masteryPercent(int totalLevels) {
        if (totalLevels <= 0) {
            return 0;
        }
        return Math.min(100, (highestUnlocked() - 1) * 100 / totalLevels);
    }

    public void resetStreak() {
        prefs.edit().putInt(KEY_STREAK, 0).apply();
    }

    public static final List<Badge> BADGES = new ArrayList<>();

    static {
        BADGES.add(new Badge("first_step", "\uD83C\uDF1F", "First Step", "Langkah Pertama", 1));
        BADGES.add(new Badge("ten_correct", "\uD83C\uDFAF", "Sharp Shooter", "Tepat 10", 10));
        BADGES.add(new Badge("three_in_row", "\uD83D\uDD25", "On Fire", "Panas 3x", 3));
        BADGES.add(new Badge("star_collector", "\u2B50", "Star Collector", "Pengumpul Bintang", 10));
        BADGES.add(new Badge("chatterbox", "\uD83C\uDFA4", "Chatterbox", "Suka Bercakap", 5));
        BADGES.add(new Badge("gamer", "\uD83C\uDFAE", "Little Gamer", "Pemain Cilik", 3));
        BADGES.add(new Badge("champion", "\uD83C\uDFC6", "Champion", "Juara", 50));
        BADGES.add(new Badge("superstar", "\uD83D\uDC51", "Superstar", "Bintang Terang", 30));
    }

    /** Badges that the child has earned based on the current counters. */
    public List<Badge> earnedBadges() {
        List<Badge> newly = new ArrayList<>();
        for (Badge b : BADGES) {
            if (hasBadge(b)) {
                continue;
            }
            boolean earned = false;
            switch (b.id) {
                case "first_step":
                    earned = correct() >= 1;
                    break;
                case "ten_correct":
                    earned = correct() >= 10;
                    break;
                case "three_in_row":
                    earned = bestStreak() >= 3;
                    break;
                case "star_collector":
                    earned = stars() >= 10;
                    break;
                case "chatterbox":
                    earned = spoken() >= 5;
                    break;
                case "gamer":
                    earned = gamesPlayed() >= 3;
                    break;
                case "champion":
                    earned = correct() >= 50;
                    break;
                case "superstar":
                    earned = stars() >= 30;
                    break;
                default:
                    break;
            }
            if (earned) {
                unlockBadge(b);
                newly.add(b);
            }
        }
        return newly;
    }

    public void resetAll() {
        prefs.edit().clear().apply();
    }
}
