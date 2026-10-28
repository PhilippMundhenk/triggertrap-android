# 0005. Keep the original Triggertrap branding and package name

Date: 2026-10-27 · Status: accepted

## Context

The house convention for new projects is a product name plus a code name and an independence
disclaimer. This is a continuation of an existing, well-known app whose company is gone. The
open-source licence (MIT, amended) asks that the software is not distributed under the
Triggertrap trademark and that references to it are removed, while allowing "compatible with
Triggertrap".

## Decision

Keep the name, colours, icons, strings and the `com.triggertrap` application id for this
personal continuation: it is what owners of the dongle look for, and keeping the id lets the
new build install over the old one. The licence file is kept unchanged and the README says
plainly that Triggertrap Ltd no longer exists and is not behind this.

## Consequences

- No rename work; updates install over the 2015 app.
- Anyone who wants to publish this build more widely (an app store listing) should rename it
  and strip the branding first, as the licence asks; a search-and-replace of `app_name`,
  the icons and the `com.triggertrap` id is the extent of it.
