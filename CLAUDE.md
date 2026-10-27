# Claude Code notes

Commands and rules that are not derivable from the code. What the project is: README.md.
Design and decisions: docs/DESIGN.md, docs/adr/. Build details: docs/DEVELOPMENT.md.

## Commands

```sh
export JAVA_HOME="/c/Program Files/Android/Android Studio/jbr"   # JDK 21 on this machine; bytecode targets 17
./gradlew :core:check                                             # unit tests + 95 % gate
./gradlew :app:lintDebug :app:assembleDebug
./gradlew :app:connectedDebugAndroidTest :app:jacocoAndroidTestReport :app:jacocoAndroidTestCoverageVerification
```

`local.properties` needs `sdk.dir=C:/Users/<user>/AppData/Local/Android/Sdk` (forward slashes;
backslashes are escapes in a properties file).

## Rules

- `core` stays pure JVM (no Android imports) and at or above 95 % line coverage.
- Every mode change needs a matching Espresso test in `app/src/androidTest`.
- Never run a second Gradle build while `connectedDebugAndroidTest` runs.
- The original Triggertrap branding (name, colours, icons, strings) is kept on purpose.
- Work is tracked in GitHub issues; reference them in commit subjects, close with a summary.

## Local quirks

- The emulator on this machine is a Play Store image (API 37); permission prompts appear as
  normal system dialogs and `uiautomator dump` sometimes returns a stale file: delete it first.
- The round mode buttons react in `onTouchEvent`; tests dispatch MotionEvents to the view
  (`TestSupport.tapButton`), injected touches become long presses on slow emulators.
