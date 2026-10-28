#!/usr/bin/env sh
# Runs the Espresso suite with the coverage gate on the CI emulator and always leaves
# diagnostics in app/build/emulator-diagnostics (screenshots, window focus, logcat).
# reactivecircus/android-emulator-runner runs every line of its `script:` in a fresh shell,
# so the "after" diagnostics have to live in one script that swallows the Gradle exit code.
set -u
out=app/build/emulator-diagnostics
mkdir -p "$out"

# dismiss the keyguard / any "System UI isn't responding" dialog: Espresso otherwise
# fails with RootViewWithoutFocusException before the first test runs
adb shell wm dismiss-keyguard || true
adb shell input keyevent 82 || true
adb shell input keyevent 3 || true
# no ANR / crash dialogs: a hung Pixel Launcher otherwise keeps the window focus for the whole run
adb shell settings put global hide_error_dialogs 1 || true
adb logcat -c || true
adb exec-out screencap -p > "$out/before.png" || true
adb shell dumpsys window | grep -E "mCurrentFocus|mFocusedApp" > "$out/focus-before.txt" || true

./gradlew :app:connectedDebugAndroidTest :app:jacocoAndroidTestReport :app:jacocoAndroidTestCoverageVerification --no-daemon
rc=$?

adb exec-out screencap -p > "$out/after.png" || true
adb shell dumpsys window | grep -E "mCurrentFocus|mFocusedApp" > "$out/focus-after.txt" || true
adb shell dumpsys window windows > "$out/windows-after.txt" || true
adb logcat -d > "$out/logcat.txt" || true
echo "gradle exit code $rc; diagnostics in $out"
exit $rc
