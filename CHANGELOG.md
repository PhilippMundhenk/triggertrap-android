# Changelog

All notable changes to this project are documented here. The format follows
[Keep a Changelog](https://keepachangelog.com/en/1.1.0/) and the project uses
[Semantic Versioning](https://semver.org/).

## [Unreleased]

## [4.0.0] - 2026-10-27

The 2015 app (3.0.1) brought back to life on current Android. Same look, same modes, same
dongle; the parts that depended on companies and services that no longer exist are gone.

### Added
- `core` module: the pulse sequences, the TimeWarp curve, sunrise/sunset, the ND calculator
  and the trigger waveform as pure Kotlin with unit tests and a 95 % coverage gate. (#4)
- Espresso end-to-end suite driving every mode against the real service and audio output,
  with a coverage gate. (#5)
- GitHub Actions: tests, lint, APKs, emulator suite, release on `v*` tags; dependabot;
  secret scan. (#6)
- Documentation: end-user README, docs/, policies, ADRs. (#7)
- Setting *Keep audio stream open*: silence between pulses so a Bluetooth receiver keeps
  the link up; notes on USB-C adapters and Bluetooth in docs/APP.md. (#11)

### Changed
- Gradle 8.14 / AGP 8.7, AndroidX, compileSdk and targetSdk 35, minSdk 26 (Android 8.0). (#1)
- Runtime permissions for microphone, location and notifications; a typed foreground
  service with a notification channel and a Stop action keeps sequences running in the
  background on Android 13-15; edge-to-edge layout on Android 15. (#3)
- Distance lapse uses the platform location manager instead of Google Play Services. (#2)
- The cable selector names the cable a camera needs instead of linking to the shop. (#2)
- The audio writer no longer streams silence forever after a single pulse (an overflow in
  the original pause handling).
- The countdown ring redraws 10 times a second instead of on every frame and the running
  timers update the screen every 100 ms instead of every 5 ms (less battery, same shutter
  timing, which comes from the audio clock).

### Fixed
- Crash when a list preference (pulse length, sensor delays, units) was missing or empty;
  the default from the settings definition is used instead.
- Crash when the Wi-Fi master was switched off without ever having been on
  (`MasterServer.close()` on an unstarted server).

### Removed
- Pebble watch mode, Mixpanel and Flurry analytics, the rate-us / email-us / Play Store
  prompts, Google Play Services. (#2)

## [3.0.1] - 2015-09-01

The last release by Triggertrap Ltd, published as open source in 2017.
