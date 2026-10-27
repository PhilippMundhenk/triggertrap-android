# Design notes

Status date: 2026-10-27.

## Goal

Keep the Triggertrap Android app usable on current Android, without the company behind it.
The modes, the look and the signal to the dongle stay as they were; everything that depended
on services that are gone is removed; the code is testable and built by CI.

## How the app works

```
MainActivity (AppCompat, drawer with the modes)      TriggertrapService (bound; foreground when the app is hidden)
  one fragment per mode  ──listener interfaces──▶    sequence player: [pulse, pause, ...] arrays, CountDownTimer for the UI
  status bar for a mode running elsewhere             OutputDispatcher ─▶ PulseHandler ─▶ AudioBeeper (AudioTrack)
  dial pad for the time inputs                                                          └▶ WifiBeeper (slaves)
                                                      MicVolumeMonitor (sound sensor), TTLocationService (distance lapse)
                                                      ZeroConfJmdns + MasterServer / SlaveSocket (Wi-Fi master / slave)
core (pure Kotlin): PulseSequences, CubicBezier, SunTimes, NdCalculator, TimeFormat, PulseWaveform
```

* A mode is a `long[]` sequence of pulse / pause durations; the fragments build it with
  `PulseGenerator` (which delegates to `core.PulseSequences`) and hand it to the service.
* The service plays the sequence through `AudioBeeper`: a writer thread fills stereo blocks
  from `core.PulseWaveform` and writes them to an `AudioTrack`. The waveform counts the pulse
  and the pause down in samples and calls back when they end; the service then plays the next
  pair. Timing therefore comes from the audio clock, not from timers.
* The activity binds the service and sets itself as listener; while the activity is stopped
  the service runs in the foreground (notification with Stop) and updates the notification
  text instead of the UI.
* Settings live in SharedPreferences behind the `TTApp` singleton.

## What changed in 4.0 (and why)

| Area | Before | Now |
|---|---|---|
| Build | Gradle 2.4, AGP 1.2, support-v13 jar, compileSdk 19 | Gradle 8.14, AGP 8.7, AndroidX, SDK 35, Kotlin DSL |
| Modules | mainActivity + 4 library modules + Eclipse copy | `app` (libraries vendored in) + `core` |
| Action bar | window decor (overlaps the content on Android 15) | Toolbar in the layout, insets applied by hand |
| Permissions | install time | runtime, asked by the screen that needs them |
| Service | `startForeground` without channel or type | channel, typed (mediaPlayback / microphone / location), Stop action |
| Location | Google Play Services LocationClient | platform `LocationManager` |
| Sun times | jsuntimes jar | `core.SunTimes` (NOAA) |
| Audio | deprecated `AudioTrack` constructor, writer streaming silence forever after a pulse | builder API, writer idles between pulses |
| Removed | Pebble, Mixpanel, Flurry, Play Services, rating and shop prompts | - |

## Open questions

* Real hardware: the audio path changed (builder API, idle writer); the signal itself is
  byte-identical, but the first run with a dongle is still to be done ([#8](https://github.com/PhilippMundhenk/triggertrap-android/issues/8)).
* Phones without a headphone jack: USB-C adapters with a DAC should work; untested.
* Wi-Fi master / slave is exercised only on a single emulator (register, discover, no second device).

## Build and test notes

See [DEVELOPMENT.md](DEVELOPMENT.md).
