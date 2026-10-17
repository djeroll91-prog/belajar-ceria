package com.ceria.belajar;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;

/**
 * Picks what a child should practise next.
 *
 * This is the "ulangan subjek yang lemah" part of the app: instead of a random
 * quiz, the weakest items come back first. An item is weak when it has been
 * answered wrongly, when its Leitner box is low, or both.
 */
public final class Review {

    private Review() {
    }

    /** One item worth practising, with the reason it was chosen. */
    public static class Candidate {
        public final Content.Unit unit;
        public final Content.Item item;
        public final String key;
        public final int right;
        public final int wrong;
        public final int box;

        Candidate(Content.Unit unit, Content.Item item, String key,
                  int right, int wrong, int box) {
            this.unit = unit;
            this.item = item;
            this.key = key;
            this.right = right;
            this.wrong = wrong;
            this.box = box;
        }

        public int accuracy() {
            int total = right + wrong;
            return total == 0 ? 0 : right * 100 / total;
        }

        public float weakness() {
            return wrong * 3f + (Progress.MAX_BOX - box) - right * 0.5f;
        }
    }

    /** A unit that needs attention, for the home screen shortcut. */
    public static class WeakUnit {
        public final String tunjangId;
        public final Content.Unit unit;
        public final int accuracy;
        public final int wrong;

        WeakUnit(String tunjangId, Content.Unit unit, int accuracy, int wrong) {
            this.tunjangId = tunjangId;
            this.unit = unit;
            this.accuracy = accuracy;
            this.wrong = wrong;
        }
    }

    /** The weakest items the child has met, worst first. */
    public static List<Candidate> queue(Progress progress, int limit) {
        List<Candidate> out = new ArrayList<>();
        for (Progress.Seen seen : progress.seenItems()) {
            out.add(new Candidate(seen.unit, seen.item, seen.key,
                    seen.right, seen.wrong, seen.box));
        }
        Collections.sort(out, new Comparator<Candidate>() {
            @Override
            public int compare(Candidate a, Candidate b) {
                return Float.compare(b.weakness(), a.weakness());
            }
        });
        if (out.size() > limit) {
            return new ArrayList<>(out.subList(0, limit));
        }
        return out;
    }

    /**
     * Units the child is getting wrong often enough to be worth flagging. A unit
     * needs at least a few answers before it can be judged, so a single early
     * mistake does not label a whole subject as weak.
     */
    public static List<WeakUnit> weakUnits(Progress progress, int limit) {
        List<WeakUnit> out = new ArrayList<>();
        for (Content.Tunjang tunjang : Content.TUNJANG) {
            for (Content.Unit unit : tunjang.units) {
                int right = 0, wrong = 0, seen = 0;
                for (Content.Item item : unit.items) {
                    String key = Progress.keyOf(item);
                    int r = progress.rightOf(unit.id, key);
                    int w = progress.wrongOf(unit.id, key);
                    if (r == 0 && w == 0) {
                        continue;
                    }
                    seen++;
                    right += r;
                    wrong += w;
                }
                if (seen < 3) {
                    continue;
                }
                int total = right + wrong;
                int accuracy = total == 0 ? 0 : right * 100 / total;
                if (accuracy < 80) {
                    out.add(new WeakUnit(tunjang.id, unit, accuracy, wrong));
                }
            }
        }
        Collections.sort(out, new Comparator<WeakUnit>() {
            @Override
            public int compare(WeakUnit a, WeakUnit b) {
                return Integer.compare(a.accuracy, b.accuracy);
            }
        });
        if (out.size() > limit) {
            return new ArrayList<>(out.subList(0, limit));
        }
        return out;
    }

    /** How many of a unit's items the child has met at all. */
    public static int seenIn(Progress progress, Content.Unit unit) {
        int seen = 0;
        for (Content.Item item : unit.items) {
            if (progress.hasSeen(unit.id, Progress.keyOf(item))) {
                seen++;
            }
        }
        return seen;
    }
}
