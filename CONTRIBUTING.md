# Contributing

Bug reports, fixes and small focused features are welcome. Work is tracked in
[GitHub issues](https://github.com/PhilippMundhenk/triggertrap-android/issues); open one before
a larger change.

## Setup and checks

See [docs/DEVELOPMENT.md](docs/DEVELOPMENT.md): JDK 17+, the Android SDK (platform 35), an
emulator or device for the UI suite.

```sh
./gradlew :core:check                     # unit tests + 95 % coverage gate
./gradlew :app:lintDebug :app:assembleDebug
./gradlew :app:connectedDebugAndroidTest :app:jacocoAndroidTestReport :app:jacocoAndroidTestCoverageVerification   # emulator
```

`core` must stay at or above 95 % line coverage; the app module has its own gate (see
DEVELOPMENT.md for the current value). Add tests with the code, not after.

## Pull requests

Branch off `master`, open a PR; CI runs the tests, the gates, lint and the emulator suite.
Commit subjects reference the issue (`... (#12)`), bodies say what and why.

## Releases

`git tag vX.Y.Z && git push --tags` builds the APKs and a GitHub release with them attached.
Add the notable changes to `CHANGELOG.md` first.
