# Group settings and collections implementation plan

> **For agentic workers:** Use superpowers:executing-plans. Steps use checkbox syntax for tracking.

**Goal:** Implement handoff phases E and F together in one PR from merged PR48.
**Architecture:** Immutable model additions, lenient JSON schema 11, pure Kotlin engine/report and section helpers, Compose settings and collection forms. Existing edit helpers own undo and save.
**Tech Stack:** Existing Kotlin/Compose/Material 3/JUnit/org.json dependencies.
**Spec:** `docs/GROUP_SETTINGS_BRIEF.md` and user-provided handoff phases E1–E3/F1–F3. User confirmed one shared optional book definition across chosen groups.

## Global constraints

Engine parity with no overrides; touched custom lessons preserved. Existing schema 10 backups remain unchanged. No new dependencies. Copy and accessibility text in string resources; theme tokens and 48dp targets. One feature release 3.10.0 (46), one schema bump to 11. No direct main push or merge. At most two helpers, gpt-6.1-sol/low. Run Gradle serially.

## Review focus

- Effective group end also blocks out-of-range pinned extras and respects earlier year end.
- Group skip additions leave other groups and supplementary semantics unchanged.
- Optional books cannot leak into another group's pools, row indices, lead allocation or print/TSV.
- Collections preserve unique membership and discard dangling references after imports/deletes.
- Group-settings navigation, delete/undo and tablet detail panes retain a valid group/class stack.

### Task 1: Model, persistence and collection mutations

Files: `model/Model.kt`, `data/JsonCodec.kt`, `data/Sanitise.kt`, `data/Backup.kt`, model/data tests.
- [x] Write tests for missing override defaults, unique collection membership, dangling references and backup id collisions; run red.
- [x] Add Group.end/skip/extraBooks and ClassCollection(id,name,classIds), SchoolYear.collections. Implement schema 11 lenient optional fields, sanitising and collision remapping.
- [x] Add pure collection mutations and tests for move/update/remove, preserving class records. Wire VM editYear without tags.
- [x] Add codec instrumentation round-trip/old schema/newer refusal checks; compile later.

### Task 2: Group engine overrides

Files: `engine/Pacer.kt`, `engine/Extras.kt`, `model/Model.kt`, engine tests.
- [x] Red tests: noGroupOverridesChangesNothing, groupEndStopsLessons, groupSkipAddsToBookSkip, extraBeyondGroupEndReturnsPages, optionalBookOnlyAffectsSelectedGroup, effectiveBookIndicesMatchTsvAndLead.
- [x] Implement min end, additive skips and approved book storage/effective plan. Audit direct Plan calls and array indexing by id at report boundaries.
- [x] Run engine parity and integration tests green.

### Task 3: Collections sections, sorting and UI

Files: `ui/ScreensModel.kt`, `ui/YearScreens.kt`, `ui/CollectionsUi.kt`, tests, class-order Settings/store wiring, string resources.
- [x] Red tests for collection order/uncollected section, unknown ids, duplicate ownership, all three stable sorts/ties and no-collection parity.
- [x] Implement model.ClassOrder YOUR_ORDER/NAME/MOST_BEHIND, sortClasses, classSections. Persist class order outside undo.
- [x] Add New collection sheet with checklist, edit/rename and delete keeping classes, undo notices and sort selector. Existing new-class flow retained.

### Task 4: Group settings screen and navigation

Files: `ui/GroupSettingsScreen.kt`, `ui/Nav.kt`, `ui/PacerApp.kt`, `ui/ClassScreens.kt`, VM methods, NavTest.
- [x] Red navigation tests for open/back/prune/year/class switches and tablet group settings detail.
- [x] Add screen/menu/link; display class defaults beside date, closure, skip and book overrides; group-scoped Greek holidays; Delete group plus undo.
- [x] Keep effective report/book ids aligned across Home/Stats/Plan/print and callbacks.

### Task 5: Integrate, review and release

- [x] Run full `testDebugUnitTest`, lint, assembleRelease and assembleDebugAndroidTest serially.
- [x] Review final diff and resolve important findings. Report absent device checks honestly.
- [x] Update feature docs, README/index, CLAUDE, CHANGELOG and version once.
- [x] Prepare verified tree and PR description for one feature PR targeting main. Push/open and inspect remote status as the final delivery step; do not merge.

## Verification

Final source tree: `testDebugUnitTest lint assembleRelease assembleDebugAndroidTest` succeeded in 9m43s. 262 unit tests, no failures/errors/skips. Lint: 0 errors, 29 warnings, 1 hint. Release and instrumentation APKs built. No emulator/device attached: instrumentation execution and visual/accessibility/tablet checks remain pending. Two helper reviews completed; important findings fixed.
