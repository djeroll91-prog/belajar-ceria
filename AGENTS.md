# Belajar Ceria - Agent Notes

Android literacy app for kindergarten children. No third-party runtime
dependencies: everything (UI, mascot, confetti, colouring, icons) is drawn with
the Android framework so the APK stays small and offline-friendly.

## Build

The Android SDK, JDK 21 and Gradle 8.9 are installed in this environment:

- `ANDROID_HOME=/opt/android-sdk` (platform 34, build-tools 34.0.0)
- `JAVA_HOME=/usr/lib/jvm/java-21-openjdk-amd64`
- `GRADLE_HOME=/opt/gradle/gradle-8.9`

```bash
export ANDROID_HOME=/opt/android-sdk
export JAVA_HOME=/usr/lib/jvm/java-21-openjdk-amd64
export PATH=$PATH:$ANDROID_HOME/cmdline-tools/latest/bin:$ANDROID_HOME/platform-tools:/opt/gradle/gradle-8.9/bin

gradle :app:assembleDebug          # debug APK
gradle :app:assembleRelease        # release APK, signed with keystore/belajar.jks
gradle :app:testDebugUnitTest      # unit + Robolectric UI smoke tests
```

Release output: `app/build/outputs/apk/release/app-release.apk`.
Signing config lives in `app/build.gradle`; keystore password is `ceriabelajar`.

## Architecture

- `MainActivity` - host activity. Owns `SpeechHelper`, `Progress`, the header and
  the content frame. Screens implement `MainActivity.Screen` and are mounted with
  `host.open(...)`.
- `BaseScreen` - toolbar + body skeleton shared by all screens.
- `Content` - all bilingual (EN/MS) data, plus Jawi and Arabic letters. Also
  defines `Swatch` for colours and helpers `isColorItem`, `isScriptItem`.
- `Curriculum` - the ordered level list. Each level has a `passScore`.
- `Progress` - SharedPreferences store: points, stars, badges, streak, language
  and the per-level best score / unlock state.
- Custom views: `RobotView` (animated mascot "Bobo"), `CelebrationView`
  (confetti), `ColorPageView` (colouring canvas).

## Product rules that must not regress

1. Levels are gated. `Progress.isUnlocked` / `highestUnlocked` only advance when
   the quiz score reaches the level's `passScore`. Children must not skip ahead.
2. Feedback is always encouraging. Wrong answers reset the streak but never
   remove points or stars, and always invite another attempt.
3. Everything is bilingual. New content must provide English and Bahasa Melayu
   strings, and route spoken text through `SpeechHelper` so TTS picks the locale.

## Testing

`app/src/test` holds JUnit data tests and Robolectric UI smoke tests. The smoke
test inflates the activity and every screen; add new screens there so layout
regressions are caught without a device.
