## Why

Bean `specgetty-mobile-em4u`, milestone `specgetty-mobile-mryq`.

`BRIEFING.md`: on phones the outline and the card are two navigation levels; on
wide screens they sit side by side, Material 3 adaptive list-detail. The spec
screen and the delta screen both have an outline and a card, and both currently
only do the phone half.

One thing has to be decided rather than inherited from the layout: what back
means. On a phone, back from a card returns to the outline, which is what the
spec screens already do. Side by side there is no level to return from, because
the outline never went away, so back leaves the screen. Getting that wrong gives
a tablet a back press that appears to do nothing.

The other is what a wide screen shows before anything is selected. Leaving half
the screen blank is the obvious behaviour and the wrong one, so the detail pane
says what it is for.

## What Changes

- `ui/ListDetail`: a composable taking a list slot and a detail slot, deciding
  between two levels and two panes from the window's width class, and reporting
  which it chose so a screen can answer back correctly.
- `ui/screen/SpecScreen` and `ui/screen/DeltaScreen` use it.

## Capabilities

### New Capabilities

- `adaptive-layout`: how an outline and its card are arranged, and what back
  means in each arrangement.

## Impact

- New dependency: `androidx.compose.material3.adaptive`, Apache-2.0, already in
  the version catalog. `docs/fdroid.md` gains a row.
- The decision is made from the window size class rather than from the device,
  so a phone in a freeform window and a folded tablet both get the right answer.
