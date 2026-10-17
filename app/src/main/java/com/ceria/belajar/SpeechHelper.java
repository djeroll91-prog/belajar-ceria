package com.ceria.belajar;

import android.app.Activity;
import android.content.ActivityNotFoundException;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Build;
import android.speech.RecognizerIntent;
import android.speech.tts.TextToSpeech;
import android.speech.tts.UtteranceProgressListener;
import android.speech.tts.Voice;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;

/**
 * Wraps text-to-speech and speech recognition so the rest of the app only has to
 * call speak() / listen() and react to the callbacks.
 *
 * The engine installed on a phone decides which languages it can speak. Google
 * Speech Services usually has Malay, but the voice data is often a separate
 * download. The old code fell back to Indonesian when Malay was missing, which
 * read "epal" as "e-pal"; a child learning to read would unlearn the correct
 * sound. Instead we pick the best Malay voice we can find, and when there is
 * none we tell the UI so it can ask the parent to install it.
 */
public class SpeechHelper {

    public interface SpeechListener {
        void onSpeechReady();

        void onSpeechResult(ArrayList<String> matches);

        void onSpeechError(String message);
    }

    /** Lets the UI react to engine readiness and to a missing Malay voice. */
    public interface EngineListener {
        void onEngineState(boolean ready, boolean malayVoiceFound);
    }

    public static final int REQUEST_SPEAK = 4021;

    private static final String PREFS = "belajar_ceria_voice";
    private static final String KEY_VOICE_MS = "voice_ms";
    private static final String KEY_VOICE_EN = "voice_en";
    private static final String KEY_ONLINE = "allow_online";
    private static final String KEY_PITCH = "pitch";
    private static final String KEY_RATE = "rate";

    private final Activity activity;
    private final SharedPreferences prefs;
    private TextToSpeech tts;
    private boolean ttsReady;
    private boolean malayVoiceFound;
    private SpeechListener activeListener;
    private EngineListener engineListener;
    /** Runs after every utterance, e.g. to stop the mascot's mouth. */
    private Runnable globalDone;
    /** Completion callbacks for individual utterances, keyed by utterance id. */
    private final Map<String, Runnable> utteranceDone = new HashMap<>();
    /** The voice picked for a language, so a song does not rescan them per word. */
    private final Map<Content.Lang, Voice> chosenVoice = new HashMap<>();

    public SpeechHelper(Activity activity) {
        this.activity = activity;
        this.prefs = activity.getSharedPreferences(PREFS, Context.MODE_PRIVATE);
        tts = new TextToSpeech(activity, status -> {
            ttsReady = status == TextToSpeech.SUCCESS;
            if (ttsReady) {
                applySavedVoice(Content.Lang.MS);
                applySavedVoice(Content.Lang.EN);
                installProgressListener();
            }
            notifyEngineState();
        });
    }

    /**
     * Reports the end of each utterance. A caller that speaks a whole song needs
     * to know when one word has finished before starting the next, otherwise the
     * words are cut off or flushed before they are audible.
     */
    private void installProgressListener() {
        if (tts == null) {
            return;
        }
        tts.setOnUtteranceProgressListener(new UtteranceProgressListener() {
            @Override
            public void onStart(String utteranceId) {
            }

            @Override
            public void onDone(String utteranceId) {
                finishUtterance(utteranceId);
            }

            @Override
            public void onError(String utteranceId) {
                // An engine that errors must not stall a caller waiting on us.
                finishUtterance(utteranceId);
            }

            private void finishUtterance(String utteranceId) {
                Runnable specific = utteranceDone.remove(utteranceId);
                if (specific != null) {
                    activity.runOnUiThread(specific);
                }
                if (globalDone != null) {
                    activity.runOnUiThread(globalDone);
                }
            }
        });
    }

    public boolean isTtsReady() {
        return ttsReady;
    }

    /** True when at least one installed voice can speak Bahasa Melayu. */
    public boolean hasMalayVoice() {
        return ttsReady && bestVoice(Content.Lang.MS) != null;
    }

    public boolean isMalayVoiceFound() {
        return malayVoiceFound;
    }

    public void setEngineListener(EngineListener listener) {
        this.engineListener = listener;
        notifyEngineState();
    }

    private void notifyEngineState() {
        if (engineListener != null) {
            engineListener.onEngineState(ttsReady, hasMalayVoice());
        }
    }

    // ------------------------------------------------------------------
    // Voice selection
    // ------------------------------------------------------------------

    /**
     * Scores a candidate voice for Bahasa Melayu. Higher is better, -1 means the
     * voice cannot be used. Kept free of framework types so it can be unit
     * tested: the caller passes the fields of a {@link Voice}.
     *
     * @param localeTag    voice locale, e.g. "ms-MY" or "en_US"
     * @param quality      {@link Voice#getQuality()} or a stand-in value
     * @param latency      {@link Voice#getLatency()} or a stand-in value
     * @param needsNetwork true when the voice only works online
     * @param allowNetwork whether online voices may be used
     */
    public static int scoreMalayVoice(String localeTag, int quality, int latency,
                                      boolean needsNetwork, boolean allowNetwork) {
        if (localeTag == null) {
            return -1;
        }
        String tag = localeTag.toLowerCase(Locale.ROOT).replace('_', '-');
        if (!tag.equals("ms") && !tag.startsWith("ms-")) {
            return -1;
        }
        if (needsNetwork && !allowNetwork) {
            return -1;
        }
        int score = quality * 100 - latency * 10;
        if (tag.equals("ms-my")) {
            score += 50;
        }
        if (!needsNetwork) {
            score += 5;
        }
        return score;
    }

    private Voice bestVoice(Content.Lang lang) {
        if (tts == null || !ttsReady) {
            return null;
        }
        Voice best = null;
        int bestScore = -1;
        try {
            Set<Voice> voices = tts.getVoices();
            if (voices == null) {
                return null;
            }
            for (Voice v : voices) {
                String tag = v.getLocale() == null ? null : v.getLocale().toLanguageTag();
                int score;
                if (lang == Content.Lang.MS) {
                    score = scoreMalayVoice(tag, v.getQuality(), v.getLatency(),
                            v.isNetworkConnectionRequired(), allowOnline());
                } else {
                    if (tag == null) {
                        continue;
                    }
                    String t = tag.toLowerCase(Locale.ROOT).replace('_', '-');
                    if (!t.startsWith("en")) {
                        continue;
                    }
                    if (v.isNetworkConnectionRequired() && !allowOnline()) {
                        continue;
                    }
                    score = v.getQuality() * 100 - v.getLatency() * 10;
                }
                if (score > bestScore) {
                    bestScore = score;
                    best = v;
                }
            }
        } catch (RuntimeException e) {
            return null;
        }
        return best;
    }

    /** Names of every installed voice that can speak Bahasa Melayu, best first. */
    public List<String> malayVoiceNames() {
        List<String> out = new ArrayList<>();
        if (tts == null || !ttsReady) {
            return out;
        }
        try {
            Set<Voice> voices = tts.getVoices();
            if (voices == null) {
                return out;
            }
            List<Voice> malay = new ArrayList<>();
            for (Voice v : voices) {
                String tag = v.getLocale() == null ? null : v.getLocale().toLanguageTag();
                if (scoreMalayVoice(tag, v.getQuality(), v.getLatency(),
                        v.isNetworkConnectionRequired(), allowOnline()) >= 0) {
                    malay.add(v);
                }
            }
            malay.sort((a, b) -> Integer.compare(scoreMalayVoice(
                            b.getLocale().toLanguageTag(), b.getQuality(), b.getLatency(),
                            b.isNetworkConnectionRequired(), allowOnline()),
                    scoreMalayVoice(a.getLocale().toLanguageTag(), a.getQuality(), a.getLatency(),
                            a.isNetworkConnectionRequired(), allowOnline())));
            for (Voice v : malay) {
                out.add(v.getName());
            }
        } catch (RuntimeException ignored) {
            // A broken engine should never crash the app.
        }
        return out;
    }

    public String activeVoiceName(Content.Lang lang) {
        return prefs.getString(lang == Content.Lang.MS ? KEY_VOICE_MS : KEY_VOICE_EN, null);
    }

    public void setVoiceByName(Content.Lang lang, String name) {
        prefs.edit().putString(lang == Content.Lang.MS ? KEY_VOICE_MS : KEY_VOICE_EN, name).apply();
        chosenVoice.remove(lang);
        applySavedVoice(lang);
        notifyEngineState();
    }

    private void applySavedVoice(Content.Lang lang) {
        if (tts == null || !ttsReady) {
            return;
        }
        Voice chosen = voiceFor(lang);
        if (chosen != null) {
            try {
                tts.setVoice(chosen);
            } catch (RuntimeException ignored) {
            }
        }
    }

    /** Online voices are usually higher quality and sound more natural. */
    public boolean allowOnline() {
        return prefs.getBoolean(KEY_ONLINE, true);
    }

    public void setAllowOnline(boolean value) {
        prefs.edit().putBoolean(KEY_ONLINE, value).apply();
        chosenVoice.clear();
        applySavedVoice(Content.Lang.MS);
        applySavedVoice(Content.Lang.EN);
        notifyEngineState();
    }

    public float pitch() {
        return prefs.getFloat(KEY_PITCH, 1.0f);
    }

    public void setPitch(float value) {
        prefs.edit().putFloat(KEY_PITCH, value).apply();
    }

    public float rate() {
        return prefs.getFloat(KEY_RATE, 0.92f);
    }

    public void setRate(float value) {
        prefs.edit().putFloat(KEY_RATE, value).apply();
    }

    /** Opens the system screen where a parent can install or pick TTS voices. */
    public void openVoiceInstall() {
        Intent install = new Intent("android.speech.tts.engine.INSTALL_TTS_DATA");
        install.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
        try {
            activity.startActivity(install);
            return;
        } catch (ActivityNotFoundException ignored) {
        }
        try {
            activity.startActivity(new Intent("com.android.settings.TTS_SETTINGS"));
        } catch (ActivityNotFoundException ignored) {
        }
    }

    // ------------------------------------------------------------------
    // Speaking
    // ------------------------------------------------------------------

    public void setOnDone(final Runnable action) {
        globalDone = action;
    }

    public void speak(String text, Content.Lang lang) {
        speakAt(text, lang, rate(), null);
    }

    public void speak(String text, Content.Lang lang, float speechRate) {
        speakAt(text, lang, speechRate, null);
    }

    /**
     * Speaks and runs {@code done} when the utterance finishes, so a caller can
     * chain phrases (a song, a story) without a fixed timer that either clips a
     * long word or leaves a gap after a short one.
     */
    public void speakThen(String text, Content.Lang lang, float speechRate, Runnable done) {
        speakAt(text, lang, speechRate, done);
    }

    /** Silences anything the engine is currently saying. */
    public void stop() {
        utteranceDone.clear();
        if (tts != null) {
            tts.stop();
        }
    }

    public void speakSlowly(String text, Content.Lang lang) {
        speakAt(text, lang, 0.6f, null);
    }

    private void speakAt(String text, Content.Lang lang, float speechRate, Runnable done) {
        if (tts == null || !ttsReady || text == null || text.isEmpty()) {
            // Nothing will be spoken, so a caller waiting on us must not hang.
            if (done != null) {
                activity.runOnUiThread(done);
            }
            return;
        }
        if (lang == Content.Lang.MS) {
            Voice voice = voiceFor(Content.Lang.MS);
            malayVoiceFound = voice != null;
            if (voice != null) {
                try {
                    tts.setVoice(voice);
                } catch (RuntimeException ignored) {
                }
            }
            // Never substitute Indonesian: it reads Malay words with the wrong
            // vowels. Ask for Malay and let the UI offer to install the voice.
            tts.setLanguage(new Locale("ms", "MY"));
            notifyEngineState();
        } else {
            Voice voice = voiceFor(Content.Lang.EN);
            if (voice != null) {
                try {
                    tts.setVoice(voice);
                } catch (RuntimeException ignored) {
                }
            }
            tts.setLanguage(Locale.US);
        }
        tts.setPitch(pitch());
        tts.setSpeechRate(speechRate);
        String id = "ceria-" + System.nanoTime();
        if (done != null) {
            utteranceDone.put(id, done);
        }
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
            tts.speak(text, TextToSpeech.QUEUE_FLUSH, null, id);
        } else {
            tts.speak(text, TextToSpeech.QUEUE_FLUSH, null);
            // Pre-Lollipop cannot tag an utterance, so the completion callback is
            // unusable there; let the caller continue rather than wait forever.
            if (done != null) {
                utteranceDone.remove(id);
                activity.runOnUiThread(done);
            }
        }
    }

    /**
     * The voice to use for a language, remembered after the first lookup.
     *
     * Scanning every installed voice on each word is wasted work on the speech
     * thread, and in a song it adds enough delay that words overlap or get
     * flushed before they are heard.
     */
    private Voice voiceFor(Content.Lang lang) {
        if (chosenVoice.containsKey(lang)) {
            return chosenVoice.get(lang);
        }
        String saved = activeVoiceName(lang);
        if (saved != null) {
            try {
                Set<Voice> voices = tts == null ? null : tts.getVoices();
                if (voices != null) {
                    for (Voice v : voices) {
                        if (saved.equals(v.getName())) {
                            chosenVoice.put(lang, v);
                            return v;
                        }
                    }
                }
            } catch (RuntimeException ignored) {
            }
        }
        Voice best = bestVoice(lang);
        chosenVoice.put(lang, best);
        return best;
    }

    /** Starts the microphone. The result arrives in onActivityResult of the activity. */
    public void listen(SpeechListener listener, Content.Lang lang) {
        this.activeListener = listener;
        Intent intent = new Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH);
        intent.putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL,
                RecognizerIntent.LANGUAGE_MODEL_FREE_FORM);
        intent.putExtra(RecognizerIntent.EXTRA_MAX_RESULTS, 5);
        intent.putExtra(RecognizerIntent.EXTRA_LANGUAGE,
                lang == Content.Lang.MS ? "ms-MY" : "en-US");
        intent.putExtra(RecognizerIntent.EXTRA_PROMPT, "\uD83C\uDFA4 Sila sebut...");
        try {
            activity.startActivityForResult(intent, REQUEST_SPEAK);
        } catch (ActivityNotFoundException e) {
            if (listener != null) {
                listener.onSpeechError("Alat ini tiada pengecam suara / No speech recognizer on this device");
            }
        }
    }

    public void handleResult(int requestCode, int resultCode, Intent data) {
        if (requestCode != REQUEST_SPEAK || activeListener == null) {
            return;
        }
        SpeechListener listener = activeListener;
        activeListener = null;
        if (resultCode == Activity.RESULT_OK && data != null) {
            ArrayList<String> matches =
                    data.getStringArrayListExtra(RecognizerIntent.EXTRA_RESULTS);
            listener.onSpeechResult(matches != null ? matches : new ArrayList<>());
        } else {
            listener.onSpeechError("Tiada suara dikesan / No speech detected");
        }
    }

    public static String normalize(String value) {
        if (value == null) {
            return "";
        }
        return value.toLowerCase(Locale.ROOT)
                .replaceAll("[^a-z0-9\u00C0-\u024F ]", "")
                .trim();
    }

    /** Loose comparison so children are praised for close-enough pronunciation. */
    public static boolean matches(String target, String spoken) {
        String t = normalize(target);
        String s = normalize(spoken);
        if (t.isEmpty() || s.isEmpty()) {
            return false;
        }
        if (s.contains(t) || t.contains(s)) {
            return true;
        }
        if (s.equals(t)) {
            return true;
        }
        int distance = editDistance(t, s);
        int tolerance = Math.max(1, t.length() / 3);
        return distance <= tolerance;
    }

    /**
     * Strict comparison for letter names, where "close enough" is not good
     * enough. Saying a letter name letter by letter ("K-H-A") is a different
     * thing from saying the name ("kha"), and a child who spells it out has not
     * learned the sound. The loose matcher would accept that, so this one is used
     * wherever the exact sound is the whole point of the exercise.
     */
    public static boolean matchesStrict(String target, String spoken) {
        String t = normalize(target);
        String s = normalize(spoken);
        if (t.isEmpty() || s.isEmpty()) {
            return false;
        }
        // Spelling it out must be rejected before the equality shortcut below:
        // normalize() drops the separators, so "k-h-a" becomes "kha" and would
        // otherwise look exactly like the name.
        if (spelledOut(spoken, t)) {
            return false;
        }
        if (s.equals(t)) {
            return true;
        }
        // The name may arrive inside a longer phrase, e.g. "ini kha".
        for (String token : s.split(" ")) {
            if (token.equals(t)) {
                return true;
            }
        }
        return false;
    }

    /**
     * True when the text looks like a name read out letter by letter, e.g.
     * "k h a" or "k-h-a" for "kha". The recognizer hands back single letters
     * separated by spaces or dashes when someone spells instead of speaks.
     */
    static boolean spelledOut(String spoken, String target) {
        if (spoken == null) {
            return false;
        }
        String flat = spoken.toLowerCase(Locale.ROOT).trim()
                .replaceAll("[-_.]", " ")
                .replaceAll("\\s+", " ")
                .trim();
        String[] tokens = flat.split(" ");
        if (tokens.length < 2) {
            return false;
        }
        StringBuilder joined = new StringBuilder();
        for (String token : tokens) {
            if (token.length() != 1) {
                return false;
            }
            joined.append(token);
        }
        // Only counts as spelling when the letters actually spell the name.
        return joined.toString().equals(target);
    }

    private static int editDistance(String a, String b) {
        int[] prev = new int[b.length() + 1];
        int[] curr = new int[b.length() + 1];
        for (int j = 0; j <= b.length(); j++) {
            prev[j] = j;
        }
        for (int i = 1; i <= a.length(); i++) {
            curr[0] = i;
            for (int j = 1; j <= b.length(); j++) {
                int cost = a.charAt(i - 1) == b.charAt(j - 1) ? 0 : 1;
                curr[j] = Math.min(Math.min(curr[j - 1] + 1, prev[j] + 1), prev[j - 1] + cost);
            }
            int[] swap = prev;
            prev = curr;
            curr = swap;
        }
        return prev[b.length()];
    }

    public void shutdown() {
        if (tts != null) {
            tts.stop();
            tts.shutdown();
            tts = null;
        }
    }

    public static boolean hasSpeechRecognition(Context context) {
        return context.getPackageManager()
                .queryIntentActivities(new Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH), 0)
                .size() > 0;
    }
}
