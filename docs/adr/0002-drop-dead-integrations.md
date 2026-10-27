# 0002. Drop Pebble, analytics, Play Services and the shop links

Date: 2026-10-27 · Status: accepted

## Context

The app integrated a Pebble watch app (installed from a Pebble app store URL), Mixpanel and
Flurry analytics, Google Play Services for location, a rate-us flow to the Play Store, an
email-us dialog and links to the Triggertrap shop for cables. The Pebble store, the Play
Store listing, the analytics projects and the shop no longer exist; Play Services forces a
dependency on Google devices for a feature the platform provides.

## Decision

Remove them. Distance lapse uses the platform `LocationManager`; the cable selector keeps the
camera-to-cable table and shows the cable code instead of a buy button.

## Consequences

- No network traffic except the Wi-Fi master / slave feature; no third-party SDKs.
- The drawer loses the Pebble entry; the `OnGoingAction.PEBBLE` constant stays so the
  notification text array indices are unchanged.
- Anyone with a Pebble (the 2025 revival) can bring the mode back from git history.
