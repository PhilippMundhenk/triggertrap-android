# 0001. Migrate the legacy app in place instead of rewriting it

Date: 2026-10-27 · Status: accepted

## Context

The 2015 code base is 22 000 lines of Java: one activity, one service, 25 fragments, custom
views and three vendored UI libraries, on the platform `Fragment` API and the support library.
It no longer built. A rewrite would have given a clean, fully testable app; it would also have
meant re-implementing 20 modes and their UI from scratch and losing the look people know.

## Decision

Keep the code and migrate it: AndroidX, AppCompat, SDK 35, runtime permissions, typed
foreground service, scripted import migration plus targeted hand edits. Extract only the pure
logic (sequences, curves, sun times, ND, waveform) into a new `core` module.

## Consequences

- The app works again with the original behaviour and look after one focused pass.
- The app module remains legacy-shaped: a 2 000-line activity, listener interfaces,
  Java. Its coverage comes from an Espresso suite rather than unit tests; the gate is lower
  than 95 % and documented in DEVELOPMENT.md.
- Revisit when a mode needs substantial new work: port that mode to a small Kotlin
  fragment + core logic rather than extending the Java.
