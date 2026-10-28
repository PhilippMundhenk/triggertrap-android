# Triggertrap — look and feel

One surface, the Android app. The branding is the original Triggertrap one and is kept on
purpose (the app continues the product people know). Code references:
`app/src/main/res/values/colors.xml`, `styles.xml`, `app/src/main/assets/fonts/`.

> Community continuation of the open-sourced app; Triggertrap Ltd no longer exists.

## 1. Name

| Where | Name |
|---|---|
| Product (titles, docs) | **Triggertrap** |
| Package / application id | `com.triggertrap` (unchanged so updates install over the old app) |
| Repository | `PhilippMundhenk/triggertrap-android` |

## 2. Colours

| Token | Value | Used for |
|---|---|---|
| `tt_red` | `#E2231A` | toolbar, primary button, running state, accents |
| `tt_dark_red` | `#C1241C` | status bar, pressed states, button border |
| `tt_very_dark_red` | `#AD211A` | drawer icon tiles (darker per row) |
| `tt_black` | `#313131` | content background behind screens, navigation bar |
| `tt_white` | `#FDFDFD` | text on red, dialog background |
| `tt_vlight_grey` / `tt_light_grey` | `#F6F6F6` / `#EFEFEF` | screen background, drawer headers |
| `tt_medium_grey` | `#DBDBDB` | button outer ring, dividers |
| `tt_dark_grey` | `#838383` | secondary text |

Light theme only (the original had none for dark mode; the red bar and light screens stay).

## 3. Typography

Roboto Light and Thin (bundled in `assets/fonts`) for titles and the big numbers, Roboto
Regular for body text, Android clock mono for the timers. Section headers in the drawer are
14 sp bold on light grey.

## 4. Components

Red toolbar with the menu icon and the mode name · drawer with grouped modes (red icon tile,
title, one-line hint) · the big red round button (`OngoingButton`, animated rings while
running) · time inputs with the in-app dial pad · circular countdown (`CircleTimerView`) ·
bottom status bar for a mode running on another screen (name + Stop) · bottom warning strip
("Set your volume to maximum") · notification with the running mode and a Stop action.

## 5. Icon

The original launcher icon (`drawable-*/ic_launcher.png`) and notification icon.

## 6. Tone

Short, friendly, as in the original strings ("Tap the big red button to make the magic
happen"). New strings follow that voice: short sentences, no exclamation marks except where
the original had them.
