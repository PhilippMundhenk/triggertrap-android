# Development

```
core/    pure Kotlin/JVM, no dependencies: pulse sequences, the TimeWarp bezier, sun times (NOAA),
         the ND calculator, time formatting, the trigger waveform
app/     the Android app (Java, 2015 code base migrated to AndroidX): MainActivity + drawer, one
         fragment per mode, TriggertrapService (sequence player, foreground service), audio output,
         sound sensor, location, Wi-Fi master / slave; vendored UI libraries under antistatic.*,
         eu.inmite.*, com.viewpagerindicator.*; Espresso suite in src/androidTest
docs/    APP.md (modes), DESIGN.md (concept, changes, open questions), BRANDING.md, adr/, screenshots
```

Requires JDK 17+ to run Gradle (bytecode targets 17), the Android SDK with platform 35
(`local.properties`: `sdk.dir=C:/Users/<you>/AppData/Local/Android/Sdk`, forward slashes) and an
emulator or device (API 26+) for the UI suite. Work is tracked in
[GitHub issues](https://github.com/PhilippMundhenk/triggertrap-android/issues).

## Build & test

* `./gradlew :core:check` — unit tests (sequences, bezier incl. overlap thinning, sun times for
  four cities and the polar cases, ND table, waveform block layout and timing) and the
  **95 % line-coverage gate** (JaCoCo); `check` fails below it.
* `./gradlew :app:lintDebug :app:assembleDebug` — lint (errors fail the build) and the APK.
* `./gradlew :app:connectedDebugAndroidTest :app:jacocoAndroidTestReport
  :app:jacocoAndroidTestCoverageVerification` — Espresso end to end on a device / emulator:
  every drawer screen, settings, all six cable modes, timelapse in the background with the
  notification and its Stop action, star trail / bramping / HDR / HDR timelapse / TimeWarp to
  completion, sound sensor arm / disarm, distance lapse fed through the service, solar and ND
  calculators, Wi-Fi slave watch and master registration. Then the coverage gate. Report:
  `app/build/reports/jacoco/jacocoAndroidTestReport/html`.
* `./gradlew :app:assembleRelease` — release APK; signed with `app/release.keystore` + the
  `KEYSTORE_PASSWORD` / `KEY_ALIAS` / `KEY_PASSWORD` environment when present, debug-signed otherwise.

Measured on 2026-10-28: core 99.6 % lines (gate 95 %); app 63.8 % lines (gate 60 %, in
`app/build.gradle.kts`, `jacocoAndroidTestCoverageVerification`). The app gate is below the
house target of 95 % on purpose: the module is the 2015 code base and is covered end to end
through the UI only; [issue #9](https://github.com/PhilippMundhenk/triggertrap-android/issues/9)
lists what is uncovered and how to raise it.

### Test notes

* The round mode buttons (`OngoingButton`) react in `onTouchEvent`; the tests dispatch
  `MotionEvent`s to the view (`TestSupport.tapButton`, `pressDown`, `release`) because injected
  touches become long presses on slow emulators.
* While a mode runs the service ticks the UI every `UI_TICK_MS` (40 ms). Do not lower it:
  Espresso treats a main thread with a message due within 15 ms as busy and would hang.
* `TestSupport.reset()` clears both preference files and the `TTApp` singleton (reflection)
  and stops whatever the service is doing; every test launches the activity fresh.
* Modes that need time are configured through `TTApp` setters before the launch (1 s timelapse,
  2 star-trail exposures of 300 ms, ...), the screens read them in `onCreate`.
* Distance lapse: the emulator has no movement, so the test calls the service's location
  callback directly after starting the mode through the UI.

## CI and releases

`.github/workflows/ci.yml`: core tests with the gate, lint, debug + release APK (artifacts on
every run), the Espresso suite with its gate on an API 34 emulator. `secret-scan.yml`: gitleaks
on PRs and weekly. `dependabot.yml`: Gradle and Actions, weekly, minor/patch grouped.

* Tags `vX.Y.Z`: a GitHub release with `triggertrap-<tag>.apk` (release) and
  `triggertrap-<tag>-debug.apk` attached (`git tag v4.0.0 && git push --tags`).
* Release signing needs the repository secrets `ANDROID_KEYSTORE_B64`,
  `ANDROID_KEYSTORE_PASSWORD`, `ANDROID_KEY_ALIAS`, `ANDROID_KEY_PASSWORD`; without them the
  release APK is debug-signed (installable; a later signed build will not update over it).

## Status / known gaps

* Verified on an emulator only; the first run with a Triggertrap Mobile dongle and a camera is
  [issue #8](https://github.com/PhilippMundhenk/triggertrap-android/issues/8). The signal is
  byte-identical to the original (pinned by `PulseWaveformTest`), the audio path around it is new.
* Wi-Fi master / slave is exercised on one device (register, watch); two-device trigger
  relaying is untested.
* The app module keeps the 2015 structure (one large activity and service, listener
  interfaces, Java); its coverage comes from the Espresso suite only. Raising it towards 95 %
  means porting screens to small Kotlin fragments on top of `core`.
* Landscape layouts of the original are kept but not exercised by the tests.
