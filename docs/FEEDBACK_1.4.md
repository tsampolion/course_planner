# Feedback on 1.3.0: checklist

Tracks the fixes for the feedback from testing 1.3.0 in Android Studio. Each item is ticked when it is done, with where it was done. Shipped in 1.4.0.

## A. Printing

- [x] **A1. Choose what to print.** Print or save PDF opens a dialog listing the year's classes as checkboxes (all ticked by default); Print stays off until at least one is ticked. *`ui/YearScreens.kt` → `PrintDialog`.*
- [x] **A2. Margins.** The same dialog offers Narrow, Normal or Wide margins (8, 15 and 25 mm; Normal is the default), applied as CSS `@page` margins in the generated page. *`ui/PrintSheet.kt` → `PrintMargin`, `html()`.* Check on a device that the PDF shows the margin, since WebView applies `@page`.

## B. Rotation

- [x] **B1. Year screen layout.** In portrait the four buttons stack one above the other at full width; in landscape they sit side by side in one row (capped at 960 dp). *`ui/YearScreens.kt` → `YearScreen`, reading `LocalConfiguration.orientation`.*
- [x] Checked other screens for the same two-column grid: only the Year screen had one.

## C. Schedules

- [x] **C1. All months / By month.** Chips above the schedule. By month shows a scrollable tab row (one tab per month) over a swipeable pager. It opens on the current month, or the first month with a lesson still to come; the choice is saved in Settings (`scheduleByMonth`). *`ui/GroupScreen.kt`, `model/Settings.kt`, `data/SettingsStore.kt`.*
- [x] **C2. Done lessons.**
  - [x] Each lesson row has a ✓ that toggles that lesson on its own. *`LessonLine`, `PacerViewModel.toggleLesson`.*
  - [x] Long-press a row: "Done up to here" and "Not done from here" (also offered as accessibility actions). *`LessonLine`, `markDoneUpTo`, `unmarkDoneFrom`.*
  - [x] Done lessons stay in the schedule, dimmed, with the pages they covered, numbered together with the rest.
  - [x] "Mark next lesson done" stays, and now ticks the first lesson that isn't done; the summary shows the next lesson and the done count.
  - [x] Pacing: a done lesson's date leaves `lessonsFor` and its pages leave `remainingPages`; earlier lessons that aren't done keep their place and the remaining pages re-spread. *`engine/Pacer.kt`.*
  - [x] Parity: ticking lessons in order, one at a time or "up to here", gives the same schedule as the old "Mark next lesson done" (`markingDoneInOrderMatchesTheOldRule`, `markingDoneWithATestMatchesTheOldRule`).
  - [x] "Reset all positions" also clears done lessons; removing a book drops it from done lessons; the Tests screen still counts a done lesson as a lesson day.
  - [x] Saved file version 3 stores each group's done lessons; version 1 and 2 files still load.

## Release

- [x] Unit tests: four new engine tests (in-order parity with and without a test day, out-of-order re-spread and untick, unmark from a date).
- [x] Version 1.4.0, `versionCode` 7, CHANGELOG entry, docs (README, CLAUDE.md, BRIEF).
- [ ] Pull request, CI green.
