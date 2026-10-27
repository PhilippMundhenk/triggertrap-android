# 0004. Extract the pure logic into a JVM `core` module with a 95 % gate

Date: 2026-10-27 · Status: accepted

## Context

The arithmetic that decides when the shutter fires (sequence generation, the TimeWarp bezier
and its overlap correction, HDR brackets, the waveform with its Samsung NX silence, the sun
times, the ND table) was spread over fragments, the service and the audio class, untestable
without an emulator. The jsuntimes jar had no source and no Maven artifact.

## Decision

A pure Kotlin/JVM module `core` (no Android imports) holds that logic; the app classes
delegate to it (`PulseGenerator`, `CubicBezierInterpolator`, `AudioBeeper`,
`NdCalculatorFragment`, `SunriseSunsetFragment`). JUnit tests and a JaCoCo line gate of 95 %
run on every build.

## Consequences

- The behaviour that matters for the hardware is pinned by tests (waveform block layout,
  timing constants, sequence totals).
- One deliberate behaviour change was made while porting: the pause after a single pulse was
  `Long.MAX_VALUE * 441 / 5` (an overflow) in the original, which kept the writer streaming
  silence; `core.PulseWaveform` passes FOREVER through and the writer idles.
- New logic goes into `core` first; the app only adapts it to views.
