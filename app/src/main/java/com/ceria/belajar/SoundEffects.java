package com.ceria.belajar;

import android.content.Context;
import android.media.AudioAttributes;
import android.media.SoundPool;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

/**
 * Short sound effects for taps and feedback.
 *
 * SoundPool is used rather than MediaPlayer because these clips are tiny and
 * overlap: a child can tap three tiles in a second, and each tap must not cut
 * off the previous one. Loading is asynchronous, so a sound requested before it
 * is ready is simply dropped rather than played late.
 *
 * Every call is guarded: a device that cannot open an audio stream should still
 * run the app silently rather than crash.
 */
public class SoundEffects {

    public static final String TAP = "sfx_tap";
    public static final String CORRECT = "sfx_correct";
    public static final String WRONG = "sfx_wrong";
    public static final String STAR = "sfx_star";
    public static final String LEVEL_UP = "sfx_levelup";
    public static final String CELEBRATE = "sfx_celebrate";
    public static final String POP = "sfx_pop";

    /** One octave of C major, the keys of the toy xylophone. */
    public static final String[] NOTES = {
            "note_c5", "note_d5", "note_e5", "note_f5",
            "note_g5", "note_a5", "note_b5", "note_c6"};

    private static final String[] NAMES = {
            TAP, CORRECT, WRONG, STAR, LEVEL_UP, CELEBRATE, POP,
            NOTES[0], NOTES[1], NOTES[2], NOTES[3],
            NOTES[4], NOTES[5], NOTES[6], NOTES[7]};

    private final Context context;
    private final Map<String, Integer> ids = new HashMap<>();
    private final Set<Integer> ready = new HashSet<>();
    private SoundPool pool;
    private android.os.Handler handler;
    private boolean enabled = true;
    private float volume = 0.9f;

    public SoundEffects(Context context) {
        this.context = context.getApplicationContext();
        try {
            pool = new SoundPool.Builder()
                    .setMaxStreams(4)
                    .setAudioAttributes(new AudioAttributes.Builder()
                            .setUsage(AudioAttributes.USAGE_GAME)
                            .setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION)
                            .build())
                    .build();
            pool.setOnLoadCompleteListener((soundPool, sampleId, status) -> {
                if (status == 0) {
                    ready.add(sampleId);
                }
            });
            for (String name : NAMES) {
                int res = context.getResources()
                        .getIdentifier(name, "raw", context.getPackageName());
                if (res != 0) {
                    ids.put(name, pool.load(context, res, 1));
                }
            }
        } catch (RuntimeException e) {
            pool = null;    // silent device is better than a crash
        }
    }

    /** Turns effects on or off without discarding the loaded samples. */
    public void setEnabled(boolean on) {
        this.enabled = on;
    }

    public boolean isEnabled() {
        return enabled;
    }

    public void play(String name) {
        if (!enabled || pool == null) {
            return;
        }
        Integer id = ids.get(name);
        if (id == null || !ready.contains(id)) {
            return;
        }
        try {
            pool.play(id, volume, volume, 1, 0, 1f);
        } catch (RuntimeException ignored) {
        }
    }

    public void tap() {
        play(TAP);
    }

    public void correct() {
        play(CORRECT);
    }

    public void wrong() {
        play(WRONG);
    }

    public void star() {
        play(STAR);
    }

    /** A short bubble-pop used when something is collected or cleared. */
    public void pop() {
        play(POP);
    }

    public void levelUp() {
        play(LEVEL_UP);
    }

    public void celebrate() {
        play(CELEBRATE);
    }

    /** Plays one xylophone key, 0-based from the lowest note. */
    public void note(int index) {
        if (index >= 0 && index < NOTES.length) {
            play(NOTES[index]);
        }
    }

    /** Plays a short run of notes, e.g. a nursery-rhyme phrase. */
    public void playTune(int[] noteIndices, long stepMillis) {
        for (int i = 0; i < noteIndices.length; i++) {
            final int note = noteIndices[i];
            if (handler == null) {
                handler = new android.os.Handler(android.os.Looper.getMainLooper());
            }
            handler.postDelayed(() -> note(note), i * stepMillis);
        }
    }

    /** True once at least one sample has finished loading; used by tests. */
    public boolean hasSamples() {
        return !ids.isEmpty();
    }

    public void release() {
        if (pool != null) {
            try {
                pool.release();
            } catch (RuntimeException ignored) {
            }
            pool = null;
        }
        ids.clear();
        ready.clear();
    }
}
