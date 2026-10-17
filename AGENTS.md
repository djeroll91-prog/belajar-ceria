# Belajar Ceria - Agent Notes

Android literacy app for kindergarten children. No third-party runtime
dependencies: everything (UI, mascot, confetti, colouring, icons) is drawn with
the Android framework so the APK stays small and offline-friendly.

## Build

The container can be reset, which wipes `/opt` and the system JDK. Everything
needed is kept under `/workspace` so it survives:

- `ANDROID_HOME=/workspace/android-sdk` (platform 34, build-tools 34.0.0)
- `JAVA_HOME=/workspace/tools/jdk-21.0.12.1+1` (portable JDK, if the system one
  is gone run `apt-get install -y openjdk-21-jdk-headless`, or re-download from
  the Adoptium API)
- `PATH=$PATH:/workspace/tools/gradle-8.9/bin`

```bash
export ANDROID_HOME=/workspace/android-sdk
export JAVA_HOME=/workspace/tools/jdk-21.0.12.1+1
export PATH=$JAVA_HOME/bin:$ANDROID_HOME/cmdline-tools/latest/bin:$ANDROID_HOME/platform-tools:/workspace/tools/gradle-8.9/bin:$PATH

gradle :app:assembleDebug          # debug APK
gradle :app:assembleRelease        # release APK, signed with keystore/belajar.jks
gradle :app:testDebugUnitTest      # unit + Robolectric UI smoke tests
```

Release output: `app/build/outputs/apk/release/app-release.apk`.
Signing config lives in `app/build.gradle`; keystore password is `ceriabelajar`.

Long Gradle runs must go to the background with a log file; the terminal soft
timeout kills foreground commands that take more than a few minutes.

## Architecture

- `MainActivity` - host activity. Owns `SpeechHelper`, `Progress`, the header and
  the content frame. Screens implement `MainActivity.Screen` and are mounted with
  `host.open(...)`.
- `BaseScreen` - toolbar + body skeleton shared by all screens.
- `Content` - all bilingual (EN/MS) content, generated from `data/kspk.json` by
  `tools/build_content.py`. Edit the JSON, never `Content.java`: the next
  regeneration overwrites it. The shape is six KSPK *tunjang*, each holding
  *units*, each holding *items* (and, for sentence units, *sentences*). Also
  defines `Swatch` for colours and the helpers `isColorItem`, `isScriptItem`,
  `isLetterItem`, `glyphOf`, `displayOf`, `spokenOf`, `tunjangIdOf`.
- Content helpers belong in the generator. `displayOf`/`spokenOf`/`tunjangIdOf`
  and the `CURRICULUM_*` constants are emitted by `tools/build_content.py`, so a
  hand edit to `Content.java` survives exactly until the next regeneration.
- `Progress` - SharedPreferences store: points, stars, badges, streak, language,
  and a Leitner box per item (`recordAnswer`, `boxOf`, `masteredIn`). Weak-unit
  detection feeds the review screen. Also holds the theme mode.
- `Theme` - the colour palette, light and dark. Screens read colours from here
  rather than hard-coding them, so dark mode works everywhere.
- `Review` - decides what a child should practise: wrong items first, then weak
  units. Nothing in it is random.
- Screens: `MainActivity` home (tunjang grid + activities), `TunjangScreen`
  (units of one tunjang), `UnitScreen` (learn/quiz/build/match for one unit),
  `LearnScreen` (flashcards), `QuizScreen` (a unit quiz, or a review queue),
  `ReviewScreen`, `GamesScreen`, `ThemesScreen`, `SettingsScreen`.
- Custom views: `RobotView` (animated mascot "Bobo"), `CelebrationView`
  (confetti), `ColorPageView` (colouring canvas), `StorySceneView` (flat themed
  backdrop shared by every screen, dark-mode aware).
- Mini games: `MemoryGame` (matching pairs), `CountGame`, `SpellGame` (build a
  word from syllables in Malay, letters in English), `OddOneOutGame`, and the
  brain-training set: `PatternGame` (finish a repeating pattern; distractors come
  from the pattern's own pictures, never from a wider pool, or the answer can be
  guessed by spotting the odd picture out), `BubblePopGame` (tap the drifting
  bubbles that match the target; draws in `dispatchDraw` of a `FrameLayout` and
  drives motion from an `ObjectAnimator` clock), `SortGame` (tap a picture, then
  tap its basket), `MathGame` (first sums, always drawn with countable pictures),
  `TraceGame` (trace a letter/number/Jawi glyph; the guide path is sampled with
  `PathMeasure` and praise is only given once 55% of the outline has been
  covered), `JawiSpeakGame` (say the Jawi letter name).
- Malay reading is taught through syllables (`bm-suku-kata`), not letters: a
  child who knows "ba" and "ju" can read "baju" without the alphabet names. That
  is why `SpellGame` chunks Malay by two and English by one.
- Letter data gotcha: a letter's `Item.symbol` is stored as `"A|🍎"` (glyph, then
  a pipe, then the picture emoji). Never draw `item.symbol` directly. Use
  `Content.glyphOf(item)` for the glyph and `Content.letterEmoji(item)` for the
  picture, or the tile shows the pipe and the emoji.
- Identify letters by shape, not by list membership. `Content.isLetterItem` checks
  the `"A|🍎"` shape, and `Progress.keyOf` uses it. The generated lists
  (`LETTERS`, `NUMBERS`, ...) now share the very same `Item` objects as the units,
  via the generator's `lastOf(unit)` helper, but a shape test cannot be broken by
  a future change to how the lists are built. A tile that prints the picture's
  name beside a letter gives the answer away and teaches the wrong sound, so
  letter quiz tiles show the glyph only.
- Background music is chosen per screen by `MainActivity.trackFor`. Screens whose
  name ends in `Game`, plus everything in `QUIET_SCREENS` (microphone, karaoke,
  recording studio, rewards, `QuizScreen`), get `MusicPlayer.SILENT` and stop the
  loop, because a tune under a child who is thinking or listening is noise.
  `QuizScreen` is in there by name because it does not end in `Game` but is one.
  `MusicPlayer.SILENT`
  is a real state, not a missing file, so `play` must release the player for it.
- Music and sound effects have separate toggles in the header. Do not fold them
  back into one switch: a parent who finds the tune distracting should not have to
  lose the tap and reward sounds.
- The karaoke song is driven by the audio, not a timer. `VoicePack.speakThen` runs
  a callback when the clip or the utterance actually ends, and `KaraokeScreen`
  waits for it before showing the next card. A fixed delay cannot work here: the
  number clips run 1.4-1.5 s while the old tick was 1.2 s, so every word was
  clipped, and a synthesised letter has no fixed length at all. A stall timeout
  (4 s) is kept purely as a safety net for an engine that never reports back, and
  a token retires the card so the timeout and the real completion cannot both
  advance the song. `KaraokeScreen.setSpeakerForTest` exists because the shadow
  player finishes instantly in tests, which would otherwise hide this.
- Shared feedback animations live on `MainActivity`: `pop`, `hop`, `shake` and
  `wrongFeedback`. New games should call these instead of rolling their own
  `animate()` chains, so praise and correction feel the same everywhere.

## Jawi and Arabic pronunciation

Getting this right is a product requirement, not a detail.

- Letter names in `Content.JAWI` follow the Malaysian KSSR/DBP names (`kha`, not
  an Arabic transliteration; `nga`, `hamzah` and `ya` are in the inventory).
  `ContentAndGatingTest.jawiLetterNamesFollowTheSchoolStandard` pins them.
- A speech engine reads a lone short token as an initialism, so a bare `kha`
  comes out as "K-H-A". `SpokenText.forEngine` wraps every Jawi/Arabic letter name
  in a carrier phrase (`"Ini kha"`) so the engine reads it as one syllable.
- Speech recognition is the other half: a child who spells the name out must not
  be praised. `SpeechHelper.spelledOut` detects that (single-letter tokens that
  join up to spell the name) and `SpeechHelper.matchesStrict` rejects it. Use
  `matchesStrict` for any script letter; use the looser `matches` for words.
- The rejection must happen *before* the equality check in `matchesStrict`:
  `normalize()` strips separators, so `k-h-a` would otherwise equal `kha`.

## Product rules that must not regress

1. Content follows the KSPK. `data/kspk.json` is the single source, generated into
   `Content.java` and `web/data.js`. A unit carries a KSPK code only where the
   official document was read directly; a null code means the unit is placed by
   tunjang and theme and must not be presented as a standard. Run
   `python3 tools/build_content.py --check` before committing: it fails if either
   generated file is out of date.
2. Progress is mastery-based, not level-gated. `Progress` keeps a Leitner box per
   item; `Review` turns the wrong ones into the next thing to practise. There are
   no locked screens: a child may open any tunjang at any time.
3. Feedback is always encouraging. Wrong answers lower the box and invite another
   attempt but never remove points or stars.
4. Everything is bilingual. New content must provide English and Bahasa Melayu
   strings, and route spoken text through `Content.spokenOf` so letters are said
   by name ("ei") and everything else by its own spoken form.
5. Jawi letter names are never accepted when spelled out. See the section above;
   `SpeechHelper.matchesStrict` is mandatory for script letters.

## Icons

Launcher icons are generated by `tools/make_icons.py` (pure Python, no Pillow).
It draws a cute chibi robot mascot and writes every density into
`app/src/main/res/mipmap-*`. It also writes a big preview to
`/tmp/apkshare/icon-preview.png` for a quick look. Run it after changing any
colour or proportion, then rebuild the APK.

```bash
python3 tools/make_icons.py
```

Android 8+ also uses the adaptive icon in
`app/src/main/res/mipmap-anydpi-v26/` with `drawable/ic_launcher_foreground.xml`
and `drawable/ic_launcher_background.xml`.

## Release

Version lives in `app/build.gradle` (`versionCode` / `versionName`). Bump both
for each new APK. The generated release APK is signed with `keystore/belajar.jks`
(store/key password `ceriabelajar`, alias `belajar`).

## Voice: human recordings with a synthesis fallback

Every phrase the app says has a stable key from `PhraseCatalog.key`, e.g. `ms_epal`.
The same key is used in three places, so never rename one after recording:

1. `res/raw/clip_<key>.<ext>` — clips bundled into the APK
2. `filesDir/voice/<key>.m4a` — clips added on the device by a parent
3. `docs/voice-script-<lang>.csv` — the script a voice artist reads from

`VoicePack` plays a bundled clip, then a device clip, then falls back to the
speech engine, which reads `SpokenText.forEngine` (phonetic fixes) rather than the
raw text. Recordings are preferred over synthesis because a child learning to
read should hear natural speech.

Letter names are in `SpokenText` for both languages, because an engine handed a
lone "B" may spell it out or read it in the wrong language. Malaysia, Brunei and
Singapore follow the English letter names (A = "ei", B = "bi", C = "si"), not the
Indonesian Dutch ones ("be", "ce"), so the Malay entries follow DBP's *Pedoman
Umum Ejaan Bahasa Melayu*. In `LearnScreen` letter mode the voice is given the
glyph via `spokenName()`, not the picture's name, so the card says "A" and not
"Apple".

Generate the scripts and bundle a finished pack:

```bash
python3 tools/voice_pack.py list        # writes docs/voice-script-{ms,en}.csv
python3 tools/voice_pack.py bundle DIR  # DIR holds <key>.m4a or <key>.ogg
python3 tools/voice_pack.py check       # coverage report
```

The generator compiles `Content` and `PhraseCatalog` and runs them, so the CSV,
the APK resource names and the recordings cannot drift apart. `PhraseCatalogTest`
pins the key rules and checks that every bundled clip is named after a real
phrase and starts with real audio bytes.

A parent can also record everything from the app itself, under the
**Studio Rakaman** tile: record, preview, delete, and export/import a zip of all
clips. That path needs no computer and no rebuild.

### Free human recordings from Wikimedia Commons

`tools/fetch_commons_voice.py` downloads real human pronunciations instead of
using a robot voice. Malay clips are `Ms-MY-<word>.ogg` and English ones are
`En-us-<word>.ogg`. Coverage is partial, so untranslated phrases keep the engine.

```bash
python3 tools/fetch_commons_voice.py              # report coverage
python3 tools/fetch_commons_voice.py --download   # fetch + write the credit list
./tools/refetch_loop.sh 40                        # keep retrying throttled clips
```

Commons throttles bursts of downloads with HTTP 429, so fetching is resumable:
each pass skips clips already on disk and merges the credit list rather than
rewriting it. Run it in the background over several passes.

The clips are **CC BY-SA 4.0** and require attribution, so the credit list is
bundled at `app/src/main/assets/voice-credits.csv` and shown on the
**Voice Credits** screen. Never bundle Commons clips without that screen: doing
so would breach the licence.

## Testing

`app/src/test` holds JUnit data tests and Robolectric UI smoke tests.

**Screens must be rendered, not just built.** A screen can build without
throwing and still show nothing: `showHome()` once returned a view it never
attached to the frame, so the app opened to a blank screen and the old smoke
test still passed. `HomeLayoutTest` now measures and draws the home screen and
every other screen, then asserts the drawing has more than 20 distinct colours
and at least two clickable views. Add any new screen to those lists.

**Attribution is tested, not assumed.** `everyBundledClipHasACreditEntry` fails
if a clip exists in `res/raw` without a matching row in `voice-credits.csv`, so
a new clip cannot ship uncredited. The fetch script rewrites the CSV after every
single clip for the same reason: an interrupted pass must never leave an
unattributed clip on disk.

`HomeLayoutTest.dumpHomeTree` prints the measured view tree with sizes and text;
run it when a layout looks wrong. A container that measures to `0x0` or a
`FrameLayout` with no children is the usual cause.

Robolectric needs `@GraphicsMode(NATIVE)` to draw real pixels. Colour counts
come from `Bitmap.getPixel`, so a fully transparent or blank screen fails.

