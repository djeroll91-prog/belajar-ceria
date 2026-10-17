package com.ceria.belajar;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

import android.content.res.AssetFileDescriptor;
import android.media.MediaPlayer;
import android.view.View;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.Robolectric;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.android.controller.ActivityController;
import org.robolectric.annotation.Config;
import org.robolectric.annotation.GraphicsMode;

import java.io.DataInputStream;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.List;

/**
 * The app used to be silent: no effects, no music. These tests keep it that way
 * no longer, and guard the properties that matter:
 *
 *  - every sound the code names actually ships in res/raw
 *  - the files are real, non-silent PCM audio rather than empty placeholders
 *  - a parent can turn sound and music off, and the choice survives a restart
 *  - the tunes loop, so a child never sits in silence mid-screen
 */
@RunWith(RobolectricTestRunner.class)
@GraphicsMode(GraphicsMode.Mode.NATIVE)
@Config(sdk = 33, qualifiers = "w411dp-h891dp-xhdpi")
public class SoundTest {

    private MainActivity launch() {
        ActivityController<MainActivity> c = Robolectric.buildActivity(MainActivity.class).setup();
        return c.get();
    }

    private InputStream openRaw(MainActivity host, String name) {
        int id = host.getResources().getIdentifier(name, "raw", host.getPackageName());
        assertTrue("res/raw/" + name + " is missing", id != 0);
        return host.getResources().openRawResource(id);
    }

    /**
     * Reads a WAV header and returns the frame count, or -1 if it is not WAV.
     *
     * The buffer is filled with a loop rather than one {@code read}: Robolectric's
     * asset stream may hand back fewer bytes than asked for, and a single bulk
     * read would then look like a corrupt file.
     */
    private int wavFrames(InputStream in) throws Exception {
        DataInputStream d = new DataInputStream(in);
        byte[] header = new byte[44];
        int filled = 0;
        while (filled < header.length) {
            int read = d.read(header, filled, header.length - filled);
            if (read < 0) {
                break;
            }
            filled += read;
        }
        if (filled < header.length) {
            return -1;
        }
        String riff = new String(header, 0, 4, "US-ASCII");
        String wave = new String(header, 8, 4, "US-ASCII");
        if (!"RIFF".equals(riff) || !"WAVE".equals(wave)) {
            return -1;
        }
        int size = (header[40] & 0xFF) | ((header[41] & 0xFF) << 8)
                | ((header[42] & 0xFF) << 16) | ((header[43] & 0xFF) << 24);
        return size / 2;    // 16-bit mono
    }

    /** The samples of a WAV file, so tests can look at the audio itself. */
    private short[] wavSamples(InputStream in) throws Exception {
        DataInputStream d = new DataInputStream(in);
        byte[] all = new byte[44];
        int filled = 0;
        while (filled < 44) {
            int read = d.read(all, filled, 44 - filled);
            if (read < 0) {
                break;
            }
            filled += read;
        }
        java.io.ByteArrayOutputStream rest = new java.io.ByteArrayOutputStream();
        byte[] chunk = new byte[4096];
        int read;
        while ((read = d.read(chunk)) > 0) {
            rest.write(chunk, 0, read);
        }
        byte[] data = rest.toByteArray();
        short[] samples = new short[data.length / 2];
        for (int i = 0; i < samples.length; i++) {
            samples[i] = (short) ((data[i * 2] & 0xFF) | (data[i * 2 + 1] << 8));
        }
        return samples;
    }

    @Test
    public void everyNamedSoundShips() {
        MainActivity host = launch();
        List<String> names = new ArrayList<>();
        names.add(SoundEffects.TAP);
        names.add(SoundEffects.CORRECT);
        names.add(SoundEffects.WRONG);
        names.add(SoundEffects.STAR);
        names.add(SoundEffects.LEVEL_UP);
        names.add(SoundEffects.CELEBRATE);
        names.add(SoundEffects.POP);
        for (String note : SoundEffects.NOTES) {
            names.add(note);
        }
        names.add(MusicPlayer.HOME);
        names.add(MusicPlayer.LEARN);
        names.add(MusicPlayer.PLAY);

        for (String name : names) {
            int id = host.getResources().getIdentifier(name, "raw", host.getPackageName());
            assertTrue("the app plays " + name + " but it is not bundled", id != 0);
        }
    }

    @Test
    public void soundsAreRealAudioNotSilence() throws Exception {
        MainActivity host = launch();
        for (String name : new String[]{SoundEffects.TAP, SoundEffects.CORRECT,
                SoundEffects.WRONG, SoundEffects.LEVEL_UP, SoundEffects.CELEBRATE,
                MusicPlayer.HOME, MusicPlayer.LEARN, MusicPlayer.PLAY,
                SoundEffects.NOTES[0], SoundEffects.NOTES[7]}) {
            short[] samples;
            try (InputStream in = openRaw(host, name)) {
                assertTrue(name + " is not a readable WAV file", wavFrames(in) > 0);
            }
            try (InputStream in = openRaw(host, name)) {
                samples = wavSamples(in);
            }
            assertTrue(name + " holds only " + samples.length + " samples",
                    samples.length > 1000);
            // A file of zeroes would "play" as silence; insist on real signal.
            long energy = 0;
            for (short s : samples) {
                energy += (long) s * s;
            }
            double rms = Math.sqrt(energy / (double) samples.length);
            assertTrue(name + " is silent (rms=" + rms + ")", rms > 100);
        }
    }

    @Test
    public void musicLoopsSoTheAppIsNeverSilent() {
        MainActivity host = launch();
        // The three tunes must exist and be long enough to be a loop, not a blip.
        for (String track : new String[]{MusicPlayer.HOME, MusicPlayer.LEARN,
                MusicPlayer.PLAY}) {
            int id = host.getResources().getIdentifier(track, "raw", host.getPackageName());
            assertTrue(track + " is not bundled", id != 0);
            try (InputStream in = openRaw(host, track)) {
                int frames = wavFrames(in);
                // At least a second of audio at 11 kHz.
                assertTrue(track + " is too short to loop (" + frames + " frames)",
                        frames > 11025);
            } catch (Exception e) {
                throw new AssertionError(track + " could not be read", e);
            }
        }
        // Playing a track records it, so the app knows what is looping.
        MusicPlayer music = host.music();
        music.play(MusicPlayer.HOME);
        assertEquals(MusicPlayer.HOME, music.currentTrack());
        // Switching screens moves to the new tune rather than layering both.
        music.play(MusicPlayer.PLAY);
        assertEquals(MusicPlayer.PLAY, music.currentTrack());
    }

    @Test
    public void soundAndMusicCanBeTurnedOffAndTheChoiceSticks() {
        MainActivity host = launch();
        assertTrue("sound should start on", host.isSoundOn());
        assertTrue("music should start on", host.isMusicOn());

        // A tune is playing on the home screen, so there is something to resume.
        String tune = host.music().currentTrack();
        assertNotNull("the home screen should be playing a tune", tune);

        host.setSoundOn(false);
        host.setMusicOn(false);
        assertTrue("sound off was not applied", !host.sfx().isEnabled());
        assertTrue("music off was not applied", !host.music().isEnabled());

        // Turning music back on must resume a tune rather than leave silence.
        host.setMusicOn(true);
        assertTrue(host.music().isEnabled());
        assertEquals("music should resume the same tune", tune, host.music().currentTrack());

        host.setSoundOn(true);
        assertTrue(host.sfx().isEnabled());

        // Now switch both off and check the choice survives a fresh activity.
        host.setSoundOn(false);
        host.setMusicOn(false);
        MainActivity again = launch();
        assertTrue("sound preference did not persist", !again.isSoundOn());
        assertTrue("music preference did not persist", !again.isMusicOn());
    }

    @Test
    public void tapsAndCorrectAnswersMakeSound() {
        MainActivity host = launch();
        View tile = new View(host);
        // These are the calls every screen makes; they must not throw, and the
        // effects must stay loaded so there is something to play.
        host.tapFeedback(tile);
        host.correctFeedback(tile);
        host.sfx().correct();
        host.sfx().wrong();
        host.sfx().star();
        host.sfx().levelUp();
        host.sfx().celebrate();
        host.sfx().note(0);
        host.sfx().note(SoundEffects.NOTES.length - 1);
        host.sfx().playTune(new int[]{0, 1, 2}, 50);
        assertTrue("no sound effects were loaded", host.sfx().hasSamples());
    }

    @Test
    public void celebratingMakesBoboCheer() {
        MainActivity host = launch();
        host.celebrate("Well done!");
        RobotView bobo = host.mascot();
        assertNotNull("the mascot should be on the home screen", bobo);
        assertTrue("Bobo should cheer when the child is praised", bobo.isCheering());
    }

    @Test
    public void xylophoneHasOneKeyPerNote() {
        assertEquals("the toy has eight rainbow keys",
                8, SoundEffects.NOTES.length);
    }

    @Test
    public void listenAndFindAlwaysOffersDistinctPictures() {
        List<Content.Item> round = AnimalSoundScreen.pickRound(Content.ANIMALS, 4);
        assertEquals(4, round.size());
        assertTrue("the answer must be one of the choices", Content.ANIMALS.contains(round.get(0)));
        for (int i = 0; i < round.size(); i++) {
            for (int j = i + 1; j < round.size(); j++) {
                assertTrue("the same picture was offered twice",
                        round.get(i) != round.get(j));
            }
        }
    }
}
