package com.ceria.belajar;

import android.app.Activity;
import android.content.ActivityNotFoundException;
import android.content.Context;
import android.content.Intent;
import android.os.Build;
import android.speech.RecognizerIntent;
import android.speech.tts.TextToSpeech;
import android.speech.tts.UtteranceProgressListener;

import java.util.ArrayList;
import java.util.Locale;

/**
 * Wraps text-to-speech and speech recognition so the rest of the app only has to
 * call speak() / listen() and react to the callbacks.
 */
public class SpeechHelper {

    public interface SpeechListener {
        void onSpeechReady();

        void onSpeechResult(ArrayList<String> matches);

        void onSpeechError(String message);
    }

    public static final int REQUEST_SPEAK = 4021;

    private final Activity activity;
    private TextToSpeech tts;
    private boolean ttsReady;
    private SpeechListener activeListener;

    public SpeechHelper(Activity activity) {
        this.activity = activity;
        tts = new TextToSpeech(activity, status -> {
            ttsReady = status == TextToSpeech.SUCCESS;
            if (ttsReady) {
                tts.setPitch(1.15f);
                tts.setSpeechRate(0.85f);
            }
        });
    }

    public boolean isTtsReady() {
        return ttsReady;
    }

    public void setOnDone(final Runnable action) {
        if (tts == null) {
            return;
        }
        tts.setOnUtteranceProgressListener(new UtteranceProgressListener() {
            @Override
            public void onStart(String utteranceId) {
            }

            @Override
            public void onDone(String utteranceId) {
                if (action != null) {
                    activity.runOnUiThread(action);
                }
            }

            @Override
            public void onError(String utteranceId) {
            }
        });
    }

    public void speak(String text, Content.Lang lang) {
        if (tts == null || !ttsReady) {
            return;
        }
        Locale locale = lang == Content.Lang.MS
                ? new Locale("ms", "MY")
                : Locale.US;
        int available = tts.isLanguageAvailable(locale);
        if (available == TextToSpeech.LANG_MISSING_DATA
                || available == TextToSpeech.LANG_NOT_SUPPORTED) {
            locale = new Locale("id", "ID");
            if (tts.isLanguageAvailable(locale) < TextToSpeech.LANG_AVAILABLE) {
                locale = Locale.US;
            }
        }
        tts.setLanguage(locale);
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
            tts.speak(text, TextToSpeech.QUEUE_FLUSH, null, "ceria-" + System.nanoTime());
        } else {
            tts.speak(text, TextToSpeech.QUEUE_FLUSH, null);
        }
    }

    public void speakSlowly(String text, Content.Lang lang) {
        if (tts == null || !ttsReady) {
            return;
        }
        tts.setSpeechRate(0.55f);
        speak(text, lang);
        tts.setSpeechRate(0.85f);
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
