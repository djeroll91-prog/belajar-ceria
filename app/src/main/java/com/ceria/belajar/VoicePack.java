package com.ceria.belajar;

import android.content.Context;
import android.media.AudioAttributes;
import android.media.MediaPlayer;
import android.speech.tts.TextToSpeech;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * Plays the best available source for a phrase: a human recording if one exists,
 * otherwise the phone's speech engine. Recordings may be bundled in res/raw
 * (clip_&lt;key&gt;.mp3 or .ogg; the extension does not matter to the lookup) or added
 * on the device by a parent.
 *
 * Recordings are played in preference to synthesis because a child learning to
 * read should hear real, natural speech; synthesised speech is the fallback that
 * keeps every phrase covered before a pack is recorded.
 */
public class VoicePack {

    /** Where a phrase is coming from, so the UI can show a badge. */
    public enum Source { BUNDLED, RECORDED, SYNTHESIZED }

    private final Context context;
    private final SpeechHelper speech;
    private final Map<String, Integer> bundled = new HashMap<>();
    private final Set<String> recorded = new HashSet<>();
    private MediaPlayer player;

    public VoicePack(Context context, SpeechHelper speech) {
        this.context = context.getApplicationContext();
        this.speech = speech;
        index();
    }

    /** Re-reads what is on the device; call after a recording or download. */
    public void index() {
        bundled.clear();
        for (Content.Lang lang : Content.Lang.values()) {
            for (PhraseCatalog.Phrase phrase : PhraseCatalog.forLang(lang)) {
                int id = context.getResources().getIdentifier(
                        "clip_" + phrase.key(), "raw", context.getPackageName());
                if (id != 0) {
                    bundled.put(phrase.key(), id);
                }
            }
        }
        recorded.clear();
        for (Content.Lang lang : Content.Lang.values()) {
            for (PhraseCatalog.Phrase phrase : PhraseCatalog.forLang(lang)) {
                if (new File(userDir(), phrase.key() + ".m4a").exists()) {
                    recorded.add(phrase.key());
                }
            }
        }
        markManifest();
    }

    /**
     * A downloaded pack ships a manifest listing every clip it contains, so a
     * key is marked available even if Android's own file naming does not match.
     */
    private void markManifest() {
        for (Content.Lang lang : Content.Lang.values()) {
            File manifest = new File(userDir(), "manifest_" + lang.name().toLowerCase() + ".txt");
            if (!manifest.exists()) {
                continue;
            }
            try (java.io.BufferedReader reader = new java.io.BufferedReader(
                    new java.io.InputStreamReader(new FileInputStream(manifest)))) {
                String line;
                while ((line = reader.readLine()) != null) {
                    String key = line.trim();
                    if (!key.isEmpty()) {
                        recorded.add(key);
                    }
                }
            } catch (IOException ignored) {
            }
        }
    }

    public File userDir() {
        File dir = new File(context.getFilesDir(), "voice");
        if (!dir.exists() && !dir.mkdirs()) {
            // Fall back to the files dir itself; recording will report the error.
            return context.getFilesDir();
        }
        return dir;
    }

    public File fileFor(Content.Lang lang, String text) {
        return new File(userDir(), PhraseCatalog.key(lang, text) + ".m4a");
    }

    public Source sourceOf(Content.Lang lang, String text) {
        String key = PhraseCatalog.key(lang, text);
        if (recorded.contains(key)) {
            return Source.RECORDED;
        }
        if (bundled.containsKey(key)) {
            return Source.BUNDLED;
        }
        return Source.SYNTHESIZED;
    }

    /** How much of a language is covered by real recordings. */
    public int[] coverage(Content.Lang lang) {
        List<PhraseCatalog.Phrase> phrases = PhraseCatalog.forLang(lang);
        int done = 0;
        for (PhraseCatalog.Phrase phrase : phrases) {
            if (sourceOf(lang, phrase.text) != Source.SYNTHESIZED) {
                done++;
            }
        }
        return new int[]{done, phrases.size()};
    }

    /**
     * Speaks a phrase. Human recordings win; otherwise the engine reads a
     * phonetically corrected form.
     */
    public void speak(Content.Lang lang, String text, float rate) {
        if (text == null || text.isEmpty()) {
            return;
        }
        stop();
        String key = PhraseCatalog.key(lang, text);

        File userFile = new File(userDir(), key + ".m4a");
        if (userFile.exists() && play(userFile)) {
            return;
        }
        Integer bundledId = bundled.get(key);
        if (bundledId != null && playBundled(bundledId)) {
            return;
        }
        speech.speak(SpokenText.forEngine(lang, text), lang, rate);
    }

    private boolean playBundled(int resId) {
        try {
            player = openPlayer();
            android.content.res.AssetFileDescriptor fd =
                    context.getResources().openRawResourceFd(resId);
            if (fd == null) {
                return false;
            }
            player.setDataSource(fd.getFileDescriptor(), fd.getStartOffset(), fd.getLength());
            fd.close();
            player.start();
            return true;
        } catch (IOException | RuntimeException e) {
            releasePlayer();
            return false;
        }
    }

    private boolean play(File file) {
        try (FileInputStream in = new FileInputStream(file)) {
            player = openPlayer();
            player.setDataSource(in.getFD());
            player.start();
            return true;
        } catch (IOException | RuntimeException e) {
            releasePlayer();
            return false;
        }
    }

    private MediaPlayer openPlayer() {
        MediaPlayer mp = new MediaPlayer();
        mp.setAudioAttributes(new AudioAttributes.Builder()
                .setUsage(AudioAttributes.USAGE_ASSISTANCE_ACCESSIBILITY)
                .setContentType(AudioAttributes.CONTENT_TYPE_SPEECH)
                .build());
        return mp;
    }

    /** Stops any clip that is playing, freeing the player. */
    public void stop() {
        releasePlayer();
        speech.stop();
    }

    private void releasePlayer() {
        if (player != null) {
            try {
                player.stop();
            } catch (IllegalStateException ignored) {
            }
            player.release();
            player = null;
        }
    }

    public void shutdown() {
        releasePlayer();
    }

    /** True when a read-only bundled pack exists; the app ships with none. */
    public boolean hasAnyBundled() {
        return !bundled.isEmpty();
    }

    /** Raw resource id for a bundled clip, or 0. Used by the parent studio. */
    public int bundledId(Content.Lang lang, String text) {
        Integer id = bundled.get(PhraseCatalog.key(lang, text));
        return id == null ? 0 : id;
    }

    /** Simpler entry point for the rest of the app. */
    public void speak(Content.Lang lang, String text) {
        speak(lang, text, speech.rate());
    }

    public void speakSlowly(Content.Lang lang, String text) {
        speak(lang, text, 0.6f);
    }

    /** The engine, for callers that need voice settings. */
    public SpeechHelper engine() {
        return speech;
    }

    /** Used by tests and by the studio to decode a clip without playing it. */
    public InputStream openUserClip(Content.Lang lang, String text) throws IOException {
        return new FileInputStream(fileFor(lang, text));
    }
}
