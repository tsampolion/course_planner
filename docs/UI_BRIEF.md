# Task: Restructure Coursebook Pacer's UI into a home screen, focused screens, and a settings screen

> Historical (1.2.0). The look and the screen patterns now follow `docs/EDUSUITE_STYLE_GUIDE.md`; where this brief disagrees with the guide, the guide wins.

## Context
Coursebook Pacer (this repo) currently has a single scaffold in `ui/PacerApp.kt`: a top bar
with a plan dropdown and overflow menu, then a `ScrollableTabRow` with a "Setup" tab plus one
tab per group. `ui/SetupScreen.kt` is one long `LazyColumn` with six sections (Term, Groups,
Days off, Tests & worksheets, Books, Homework already assigned). `ui/GroupScreen.kt` shows a
group's summary, verdict, positions, month bars and lesson-by-lesson schedule. This works,
but Setup is a long scroll, the tab row gets crowded as groups grow, and there's no place
for app-level preferences.

Goal: a clearer, more navigable app. Teachers should see at a glance how every group is
doing, and reach any part of a plan in one or two taps.

Read `CLAUDE.md` and `docs/BRIEF.md` first. Their rules are binding.

## What to build
1. **Home screen** (new start destination) for the active plan:
   - Plan name with the plan switcher, plus quick access to new, rename and delete.
   - One card per group: name, lessons left, the verdict (on track, lessons to spare, or
     shortfall), and the next lesson's date and pages. Tapping a card opens that group.
   - The `problems()` list, shown prominently whenever it isn't empty, with each problem
     linking to the screen that fixes it.
   - Entry points to the plan-editing screens below, and to Settings.
   - A useful empty or first-run state (for example, no term dates or no groups yet).
2. **Plan-editing screens.** Split Setup into focused screens wherever that helps. A likely
   split: Term, Groups, Days off, Tests & worksheets, Books, Homework. Merge any that are
   too thin to stand alone, and justify your choices.
3. **Group detail screen**: today's `GroupScreen` content, with copy, share and
   "Mark next lesson done" kept.
4. **Settings screen**, holding only app-level preferences that actually affect the app.
   Candidates: theme (system, light or dark), defaults for new plans (lesson length and
   sustainable pace), print or PDF, and an About section (version, licence). Don't add
   settings that do nothing.

## Constraints (non-negotiable)
- **No new dependencies.** `navigation-compose` is *not* approved. Build navigation with a
  small sealed `Screen` type held in the ViewModel (or with `rememberSaveable`), and handle
  system back with `BackHandler` from `activity-compose`. If you think a library is truly
  needed, stop and ask instead of adding it.
- **Engine untouched.** Don't change anything in `engine/`. Scheduling output must stay
  identical.
- **Feature parity.** Every item in the BRIEF.md parity checklist must still be reachable.
  At the end, list each item and the screen it now lives on.
- **Persistence compatibility.** `AppData.view` is saved in `plans.json`. Either keep its
  meaning or migrate it safely. Old files and the web page's localStorage export must still
  load. If the schema changes, bump `JsonCodec.VERSION` and keep reading old versions.
- **Settings storage** stays out of the undo stack. If you store settings in a new file or
  in SharedPreferences, update `data_extraction_rules.xml` so it's backed up.
- **Undo and saving.** All plan mutations still go through `edit`, `editPlan`,
  `editGroup` and `editBook`. Undo stays reachable from every plan screen, and
  `MainActivity.onStop` → `vm.flush()` still works.
- Compose and Material 3 only, `Theme.Pacer`, no icon library (use text or Unicode as the
  current code does), `minSdk 26`. Reuse `Components.kt` (`Section`, `CommitField`,
  `DateField`, `ListItemRow`, …) and the `Pacer.colors` tokens instead of inventing new
  styles.
- Navigation state survives rotation and process death where that's reasonable.

## How to work
1. Read the `ui/` package, `model/Model.kt` and `data/JsonCodec.kt`. Then write a short plan:
   the screen map (a tree of destinations and how each is reached), where navigation state
   lives, how settings are stored, and any schema impact. Share the plan before writing
   much code.
2. Implement in small, reviewable commits: navigation scaffold → home → split setup
   screens → group detail → settings.
3. Delete code that no longer has a use, such as the tab row. Don't leave it commented out.

## Done when
- `./gradlew testDebugUnitTest assembleRelease lint` passes. Add unit tests for any new
  non-UI logic, such as navigation reduction, settings defaults or the `view` migration.
- The version is bumped per `CHANGELOG.md`: this is a MINOR change, so `1.1.0` and
  `versionCode` 3, with a matching changelog entry and compare link.
- Your final message includes the screen map, the parity checklist mapping, anything you
  chose not to do and why, and any open questions (for example, settings you considered
  and rejected).
