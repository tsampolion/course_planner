# Plan | Stats split of the group screen (2.6.0): implementation plan

**Spec:** `docs/superpowers/specs/2026-10-20-custom-plans-roadmap-design.md`, PR1. No engine or schema change.

- [x] `SegmentedToggle` in `ui/DashboardComponents.kt`: one ink pill that slides between choices (§5 chips, `Motion.settle`), tabs for TalkBack, and a segment haptic. It is reused by custom plans' "My plan | Proposed" in 2.7.0.
- [x] `sideways(forward, enabled)` in `ui/Motion.kt`: a shared-axis slide and fade for sibling views (`Motion.content`), which snaps when animations are off.
- [x] `ui/GroupStats.kt`:
  - `GroupStats` holds the tiles, verdict, Details, Lessons by month and Books, moved from `SummaryCard`;
  - `StatusChip` shows the verdict in one line and opens Stats.
- [x] `ui/GroupScreen.kt`:
  - Plan | Stats toggle above an `AnimatedContent`;
  - Plan: `NextLessonTile`, `StatusChip`, the day strip behind "Term weeks" and the tips behind "Tips", then the schedule;
  - the plan's `LazyListState` is hoisted so it keeps its scroll position.
- [x] Strings in `strings.xml`; `NoHardcodedTextTest` knows the new lazy keys and animation labels.
- [x] 2.6.0 / versionCode 27, CHANGELOG, README, BRIEF.
- [x] `./gradlew testDebugUnitTest lint assembleRelease`.
