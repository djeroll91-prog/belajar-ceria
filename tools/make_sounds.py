#!/usr/bin/env python3
"""Generates the app's sound effects and background tunes.

Everything is synthesised from sine and triangle waves with the standard
library only, so the repository needs no audio toolchain and the output is
reproducible: running this twice produces byte-identical files.

Two families are written into app/src/main/res/raw:

  sfx_*    short effects, played through SoundPool (tap, right, wrong, ...)
  music_*  short seamless loops, played through MediaPlayer while a screen is up

The tunes are original, built from the C major pentatonic scale so they sound
like nursery rhymes without borrowing anyone's melody.
"""

import math
import struct
import wave
from pathlib import Path

ROOT = Path(__file__).resolve().parent.parent
OUT = ROOT / "app" / "src" / "main" / "res" / "raw"

SFX_RATE = 22050
MUSIC_RATE = 11025

# Equal temperament from A4 = 440 Hz.
NOTES = {}
for _i, _name in enumerate(
        ["C", "C#", "D", "D#", "E", "F", "F#", "G", "G#", "A", "A#", "B"]):
    for _octave in range(3, 7):
        _semitones = _i + (_octave - 4) * 12 - 9
        NOTES[f"{_name}{_octave}"] = 440.0 * (2 ** (_semitones / 12.0))
NOTES["-"] = 0.0     # rest


def envelope(index, total, attack=0.02, release=0.35):
    """Soft attack and release so notes never click."""
    attack_samples = max(1, int(total * attack))
    release_samples = max(1, int(total * release))
    if index < attack_samples:
        return index / attack_samples
    if index > total - release_samples:
        return max(0.0, (total - index) / release_samples)
    return 1.0


def tone(freq, seconds, rate, harmonics=(1.0, 0.25, 0.12), gain=0.42, vibrato=0.0):
    """A single note as a list of floats in [-1, 1]."""
    total = int(seconds * rate)
    out = []
    for i in range(total):
        if freq <= 0:
            out.append(0.0)
            continue
        wobble = 1.0
        if vibrato:
            wobble += vibrato * math.sin(2 * math.pi * 5.5 * i / rate)
        value = 0.0
        for h, amp in enumerate(harmonics, start=1):
            value += amp * math.sin(2 * math.pi * freq * h * wobble * i / rate)
        value /= sum(harmonics)
        out.append(value * gain * envelope(i, total))
    return out


def mix(*tracks):
    length = max(len(t) for t in tracks)
    out = [0.0] * length
    for track in tracks:
        for i, value in enumerate(track):
            out[i] += value
    return out


def sequence(notes, rate, beat, harmonics=(1.0, 0.25, 0.12), gain=0.42):
    """notes is a list of (name, beats). Rests are '-'."""
    out = []
    for name, beats in notes:
        out.extend(tone(NOTES[name], beats * beat, rate,
                        harmonics=harmonics, gain=gain))
    return out


def normalise(samples, peak=0.85):
    highest = max(abs(s) for s in samples) or 1.0
    scale = peak / highest
    return [s * scale for s in samples]


def write_wav(path, samples, rate):
    frames = bytearray()
    for value in samples:
        clamped = max(-1.0, min(1.0, value))
        frames += struct.pack("<h", int(clamped * 32767))
    with wave.open(str(path), "wb") as fh:
        fh.setnchannels(1)
        fh.setsampwidth(2)
        fh.setframerate(rate)
        fh.writeframes(bytes(frames))
    return len(frames)


def bell(freqs, rate, note=0.16, gain=0.4):
    """A short arpeggio, the sound of a job well done."""
    out = []
    for i, freq in enumerate(freqs):
        part = tone(freq, note * (1 + 0.35 * (len(freqs) - i)), rate,
                    harmonics=(1.0, 0.4, 0.2, 0.1), gain=gain)
        out.extend(part)
    return out


def buzz(rate):
    """A gentle 'not that one' wobble; low and short so it never frightens."""
    total = int(0.30 * rate)
    out = []
    for i in range(total):
        t = i / rate
        freq = 196.0 * (1.0 - 0.25 * (i / total))
        value = math.sin(2 * math.pi * freq * t) + 0.3 * math.sin(2 * math.pi * freq * 2 * t)
        out.append(value / 1.3 * 0.34 * envelope(i, total, 0.02, 0.5))
    return out


def build_effects():
    return {
        # A soft wooden blip for every tap.
        "sfx_tap": normalise(tone(NOTES["E5"], 0.09, SFX_RATE,
                                  harmonics=(1.0, 0.35, 0.15), gain=0.5), 0.5),
        # Rising two-note sparkle: "yes!"
        "sfx_correct": normalise(bell([NOTES["C5"], NOTES["E5"], NOTES["G5"]],
                                      SFX_RATE), 0.7),
        "sfx_wrong": normalise(buzz(SFX_RATE), 0.6),
        # A longer fanfare for finishing a level.
        "sfx_levelup": normalise(bell([NOTES["C5"], NOTES["E5"], NOTES["G5"],
                                       NOTES["C6"], NOTES["E6"]], SFX_RATE,
                                      note=0.18), 0.75),
        # Collecting a star.
        "sfx_star": normalise(bell([NOTES["G5"], NOTES["C6"]], SFX_RATE,
                                   note=0.12, gain=0.38), 0.65),
        "sfx_celebrate": normalise(bell([NOTES["C5"], NOTES["E5"], NOTES["G5"],
                                         NOTES["C6"], NOTES["G5"], NOTES["C6"]],
                                        SFX_RATE, note=0.14), 0.75),
        # Popping a bubble: a quick upward blip.
        "sfx_pop": normalise(tone(NOTES["A5"], 0.10, SFX_RATE,
                                  harmonics=(1.0, 0.5, 0.25), gain=0.45), 0.55),
    }


# The toy xylophone: eight rainbow keys, one octave of C major.
XYLOPHONE_KEYS = ["C5", "D5", "E5", "F5", "G5", "A5", "B5", "C6"]


def build_notes():
    """One warm, bell-like sample per xylophone key."""
    out = {}
    for name in XYLOPHONE_KEYS:
        samples = tone(NOTES[name], 0.75, SFX_RATE,
                       harmonics=(1.0, 0.5, 0.28, 0.14, 0.07), gain=0.5)
        out[f"note_{name.lower()}"] = normalise(samples, 0.62)
    return out


def build_tunes():
    """Seamless loops: each ends on its tonic and is a whole number of bars."""
    beat = 0.30

    home = sequence([
        ("C5", 1), ("E5", 1), ("G5", 1), ("E5", 1),
        ("F5", 1), ("E5", 1), ("D5", 2),
        ("C5", 1), ("D5", 1), ("E5", 1), ("G5", 1),
        ("A5", 2), ("G5", 2),
    ], MUSIC_RATE, beat, gain=0.34)
    home_bass = sequence([
        ("C3", 4), ("G3", 4), ("F3", 4), ("C3", 4),
    ], MUSIC_RATE, beat, harmonics=(1.0, 0.5), gain=0.22)

    learn = sequence([
        ("C5", 2), ("D5", 2), ("E5", 2), ("C5", 2),
        ("E5", 2), ("F5", 2), ("G5", 4),
        ("G5", 2), ("E5", 2), ("D5", 2), ("C5", 4),
    ], MUSIC_RATE, beat, harmonics=(1.0, 0.2), gain=0.3)
    learn_bass = sequence([
        ("C3", 8), ("G3", 8),
    ], MUSIC_RATE, beat, harmonics=(1.0, 0.5), gain=0.18)

    play = sequence([
        ("E5", 1), ("G5", 1), ("A5", 1), ("G5", 1),
        ("E5", 1), ("D5", 1), ("E5", 2),
        ("G5", 1), ("A5", 1), ("C6", 1), ("A5", 1),
        ("G5", 2), ("C5", 2),
    ], MUSIC_RATE, beat, gain=0.32)
    play_bass = sequence([
        ("C3", 4), ("G3", 4), ("A3", 4), ("C3", 4),
    ], MUSIC_RATE, beat, harmonics=(1.0, 0.5), gain=0.2)

    return {
        "music_home": mix(home, home_bass),
        "music_learn": mix(learn, learn_bass),
        "music_play": mix(play, play_bass),
    }


def main():
    OUT.mkdir(parents=True, exist_ok=True)
    written = []

    for name, samples in build_effects().items():
        path = OUT / f"{name}.wav"
        frames = write_wav(path, samples, SFX_RATE)
        written.append((name, frames, SFX_RATE))

    for name, samples in build_notes().items():
        path = OUT / f"{name}.wav"
        frames = write_wav(path, samples, SFX_RATE)
        written.append((name, frames, SFX_RATE))

    for name, samples in build_tunes().items():
        path = OUT / f"{name}.wav"
        frames = write_wav(path, normalise(samples, 0.7), MUSIC_RATE)
        written.append((name, frames, MUSIC_RATE))

    total = 0
    for name, frames, rate in sorted(written):
        seconds = frames / (2 * rate)
        total += frames
        print(f"{name:18} {seconds:5.2f}s  {frames // 1024:5d} KB")
    print(f"{len(written)} files, {total / 1024:.0f} KB of audio in {OUT.relative_to(ROOT)}")


if __name__ == "__main__":
    main()
