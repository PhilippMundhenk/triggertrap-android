# 0003. Vendor the three UI libraries into the app module

Date: 2026-10-27 · Status: accepted

## Context

The build had three library modules copied from GitHub in 2013: android-spinnerwheel
(`antistatic.spinnerwheel`), StyledDialogs (`eu.inmite.android.lib.dialogs`) and
ViewPagerIndicator (`com.viewpagerindicator`). None is maintained or published for AndroidX;
each needed the same import migration.

## Decision

Move their sources and resources into `app/src/main` under their original packages, pointing
their `R` references at `com.triggertrap.R`. Unused parts stay (small) rather than being
pruned on day one.

## Consequences

- One Android module, one coverage gate, one lint run; no library build files to maintain.
- Their licences (Apache 2.0) still apply to those packages; the headers are kept.
- Unused indicators (circle, title, tab) could be deleted later.
