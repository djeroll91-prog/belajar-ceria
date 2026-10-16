package com.ceria.belajar;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import android.speech.tts.TextToSpeech;
import android.speech.tts.Voice;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.annotation.Config;

/**
 * The phone's engine decides which voices exist, so the picking logic is a pure
 * function and tested directly. The important rule: never pick a non-Malay voice
 * for Bahasa Melayu, because Indonesian reads "epal" as "e-pal".
 */
@RunWith(RobolectricTestRunner.class)
@Config(sdk = 33)
public class VoiceSelectionTest {

    private static final int VERY_HIGH = Voice.QUALITY_VERY_HIGH;
    private static final int HIGH = Voice.QUALITY_HIGH;
    private static final int NORMAL = Voice.QUALITY_NORMAL;
    private static final int VERY_LOW = Voice.QUALITY_VERY_LOW;
    private static final int LAT_VERY_LOW = Voice.LATENCY_VERY_LOW;
    private static final int LAT_HIGH = Voice.LATENCY_HIGH;

    @Test
    public void malayMalaysiaIsAccepted() {
        assertTrue(SpeechHelper.scoreMalayVoice("ms-MY", NORMAL, LAT_VERY_LOW, false, true) > 0);
    }

    @Test
    public void bareMalayLocaleIsAccepted() {
        assertTrue(SpeechHelper.scoreMalayVoice("ms", NORMAL, LAT_VERY_LOW, false, true) > 0);
    }

    @Test
    public void indonesianIsRejected() {
        assertEquals(-1, SpeechHelper.scoreMalayVoice("id-ID", VERY_HIGH, LAT_VERY_LOW, false, true));
    }

    @Test
    public void englishIsRejected() {
        assertEquals(-1, SpeechHelper.scoreMalayVoice("en-US", VERY_HIGH, LAT_VERY_LOW, false, true));
        assertEquals(-1, SpeechHelper.scoreMalayVoice("en_GB", VERY_HIGH, LAT_VERY_LOW, false, true));
    }

    @Test
    public void malayMalaysiaBeatsGenericMalay() {
        int my = SpeechHelper.scoreMalayVoice("ms-MY", HIGH, LAT_VERY_LOW, false, true);
        int generic = SpeechHelper.scoreMalayVoice("ms", HIGH, LAT_VERY_LOW, false, true);
        assertTrue("ms-MY should outrank bare ms, got " + my + " vs " + generic, my > generic);
    }

    @Test
    public void higherQualityWins() {
        int high = SpeechHelper.scoreMalayVoice("ms-MY", VERY_HIGH, LAT_VERY_LOW, false, true);
        int low = SpeechHelper.scoreMalayVoice("ms-MY", NORMAL, LAT_VERY_LOW, false, true);
        assertTrue(high > low);
    }

    @Test
    public void offlineVoiceBeatsOnlineVoiceAtSameQuality() {
        int offline = SpeechHelper.scoreMalayVoice("ms-MY", HIGH, LAT_VERY_LOW, false, true);
        int online = SpeechHelper.scoreMalayVoice("ms-MY", HIGH, LAT_VERY_LOW, true, true);
        assertTrue(offline > online);
    }

    @Test
    public void onlineVoiceRejectedWhenOnlineIsOff() {
        assertEquals(-1, SpeechHelper.scoreMalayVoice("ms-MY", HIGH, LAT_VERY_LOW, true, false));
        // An offline voice is still fine.
        assertTrue(SpeechHelper.scoreMalayVoice("ms-MY", HIGH, LAT_VERY_LOW, false, false) > 0);
    }
}
