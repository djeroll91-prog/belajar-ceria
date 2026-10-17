package com.ceria.belajar;

import android.content.Context;
import android.media.AudioAttributes;
import android.media.MediaPlayer;

import java.io.IOException;

/**
 * Loops a cheerful background tune while a screen is open.
 *
 * The tunes are short and seamless, so they loop rather than run for minutes.
 * Only one tune plays at a time: switching screens fades the old one out and
 * starts the new one, which is why {@link #play} takes a track name rather than
 * letting callers start several.
 *
 * Music is deliberately gentle and low: the spoken word must stay in front, so
 * the track sits well below speech level and ducks while the robot talks.
 */
public class MusicPlayer {

    public static final String HOME = "music_home";
    public static final String LEARN = "music_learn";
    public static final String PLAY = "music_play";

    private final Context context;
    private MediaPlayer player;
    /** The tune that should be playing; kept even while playback is torn down. */
    private String current;
    private boolean enabled = true;
    private float volume = 0.34f;

    public MusicPlayer(Context context) {
        this.context = context.getApplicationContext();
    }

    /** Turns music on or off. Turning it off stops whatever is playing. */
    public void setEnabled(boolean on) {
        if (this.enabled == on) {
            return;
        }
        this.enabled = on;
        if (on) {
            // Resume the tune the child was listening to, if there was one.
            String track = current;
            current = null;
            play(track);
        } else {
            // Keep the track name so it can be resumed later.
            release();
        }
    }

    public boolean isEnabled() {
        return enabled;
    }

    /** Starts a loop, or keeps the current one if it is already playing. */
    public void play(String track) {
        if (track == null) {
            return;
        }
        // Remember the request even when music is off, so switching music back
        // on resumes the right tune rather than silence.
        if (!enabled) {
            current = track;
            return;
        }
        if (track.equals(current) && player != null) {
            if (!player.isPlaying()) {
                safeStart();
            }
            return;
        }
        release();
        current = track;
        int res = context.getResources().getIdentifier(track, "raw", context.getPackageName());
        if (res == 0) {
            return;
        }
        try {
            player = new MediaPlayer();
            player.setAudioAttributes(new AudioAttributes.Builder()
                    .setUsage(AudioAttributes.USAGE_MEDIA)
                    .setContentType(AudioAttributes.CONTENT_TYPE_MUSIC)
                    .build());
            android.content.res.AssetFileDescriptor fd =
                    context.getResources().openRawResourceFd(res);
            if (fd == null) {
                release();
                return;
            }
            player.setDataSource(fd.getFileDescriptor(), fd.getStartOffset(), fd.getLength());
            fd.close();
            player.setLooping(true);
            player.setVolume(volume, volume);
            player.prepare();
            player.start();
        } catch (IOException | RuntimeException e) {
            // A device that cannot play this file should not leave the app
            // believing it is silent by choice; the track name stays recorded.
            release();
        }
    }

    private void safeStart() {
        try {
            player.start();
        } catch (RuntimeException e) {
            release();
        }
    }

    /**
     * Ducks the music while a phrase is spoken, so words are never buried under
     * the tune. Call {@link #unduck} when the phrase finishes.
     */
    public void duck() {
        if (player != null) {
            try {
                player.setVolume(volume * 0.25f, volume * 0.25f);
            } catch (RuntimeException ignored) {
            }
        }
    }

    public void unduck() {
        if (player != null) {
            try {
                player.setVolume(volume, volume);
            } catch (RuntimeException ignored) {
            }
        }
    }

    public void pause() {
        if (player != null) {
            try {
                player.pause();
            } catch (RuntimeException ignored) {
            }
        }
    }

    public void resume() {
        if (enabled && player != null) {
            safeStart();
        }
    }

    /** Stops and forgets the current track. */
    public void stop() {
        release();
        current = null;
    }

    private void release() {
        if (player != null) {
            try {
                player.stop();
            } catch (IllegalStateException ignored) {
            }
            player.release();
            player = null;
        }
    }

    public String currentTrack() {
        return current;
    }

    public void shutdown() {
        stop();
    }
}
