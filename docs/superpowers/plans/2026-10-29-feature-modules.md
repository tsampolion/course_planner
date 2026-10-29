# Feature Modules Foundation Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Build step 2 of the modules order, the invisible foundation: a module registry, stored modules, gates in the pure menu models, the import question and the slim catch-up bar, with every module on for every install until the Welcome presets ship (UI audit PR 8).

**Architecture:** A pure `model/Features.kt` decides what each module covers and what data uses it. `Settings.modules` stores the teacher's set; `PacerViewModel.features` combines it with the data and Compose reads it through `LocalFeatures`. Gates live in the pure `*Model.kt` files so they are unit-tested; Compose call sites only pass `features` in.

**Tech Stack:** Kotlin, Jetpack Compose (Material 3), JUnit 4, SharedPreferences. No new dependency.

**Spec:** `docs/FEATURE_MODULES_BRIEF.md`

Later steps (audit PRs 6, 7, 8, 9 with their module work) get their own plan when each starts, after its impeccable and taste design pass.

## Global Constraints

- No engine change, no `JsonCodec.VERSION` change, no new dependency (CLAUDE.md).
- Modules only turn on. There is no API that removes a module.
- Until the Welcome presets ship, a missing stored set means every module on, for fresh and existing installs alike. Nothing visible changes in this PR.
- UI copy in `res/values/strings.xml`; `NoHardcodedTextTest` must pass. Sentence case, no exclamation marks.
- Sheets use `PacerSheet`; motion uses `ui/Motion.kt` helpers and snaps with animations off.
- `versionName` takes the next B step after main's (`0.4.(B+1).0`), `versionCode` + 1, a matching CHANGELOG section; no release name yet (the launch carries the first).
- NAVIGATION.md rows for every gated control gain a "When" condition in this PR.

## Review Focus

1. An existing install updating with no stored modules: every module on, nothing hidden (Task 2 test `missingModulesMeanEverything`).
2. A plan made only by catch-up does not count as Planning tools in use, but one hand-planned lesson does (Task 1 tests `catchUpPlanIsNotPlanning`, `oneTouchedLessonIsPlanning`).
3. Importing a backup that uses tests while Work is off asks first, and Cancel leaves the data exactly as it was (Task 4 tests).
4. Importing into a phone with every module on never asks (Task 4 test `noQuestionWhenEverythingIsOn`).
5. Undo after turning a module on leaves it on, because modules live in Settings, outside undo (Task 3 test `undoKeepsModulesOn`).

---

### Task 1: The module registry

**Files:**
- Create: `app/src/main/java/gr/edusuite/pacer/model/Features.kt`
- Test: `app/src/test/java/gr/edusuite/pacer/model/FeaturesTest.kt`

**Interfaces:**
- Produces:
  - `enum class Module { WORK, BOOK_TOOLS, PLANNING, OUTPUT }` with `companion object { fun parse(name: String?): Module? }`
  - `fun modulesInUse(years: List<SchoolYear>): Set<Module>`
  - `fun CustomPlan.handPlanned(): Boolean` = any lesson `touched`
  - `data class Features(val on: Set<Module>) { fun has(m: Module): Boolean; companion object { val ALL: Features } }`

`modulesInUse` reads existing fields only:
- WORK: any `Group.assignments` or `Group.unitRevisions`.
- BOOK_TOOLS: a class `lead`, `unitsEnabled`, a book with `units`, any `Group.extraBooks`, `removedBooks`, non-blank `Group.skip` value, `bookDue`, `unitsOverride != null`.
- PLANNING: a book with `supplementary`, any `Group.extras`, a `Group.custom` whose `handPlanned()` is true.
- OUTPUT: never.
Class-level `Book.skip`, `Book.due` and `Group.end` are core and never count.

- [ ] **Step 1: Write the failing tests** in `FeaturesTest` using `model/SampleYear.kt` fixtures:
  - `emptyYearUsesNothing`: `modulesInUse(listOf(sampleYear))` == `emptySet()`
  - `assignmentMeansWork`, `unitRevisionMeansWork`
  - `leadBookMeansBookTools`, `groupSkipMeansBookTools`, `classSkipAndDueAreCore` (expects `emptySet()`)
  - `supplementaryBookMeansPlanning`, `extraLessonMeansPlanning`
  - `catchUpPlanIsNotPlanning`: a group whose `custom` comes from `GroupReport.catchUpPlan(2)` → no PLANNING
  - `oneTouchedLessonIsPlanning`: same plan with one lesson `touched = true` → PLANNING
  - `allHasEveryModule`: `Module.entries.all { Features.ALL.has(it) }`
  - `parseIgnoresUnknown`: `Module.parse("NOPE") == null`
- [ ] **Step 2: Run** `./gradlew testDebugUnitTest --tests 'gr.edusuite.pacer.model.FeaturesTest'`; expect compile failure.
- [ ] **Step 3: Implement** `Features.kt` with the interfaces above. KDoc on `Module` names each module's contents as in the brief's table.
- [ ] **Step 4: Run the same command;** expect PASS.
- [ ] **Step 5: Commit** `git commit -m "Module registry and modulesInUse"`.

### Task 2: Stored modules

**Files:**
- Modify: `app/src/main/java/gr/edusuite/pacer/model/Settings.kt` (field and `sanitised`)
- Modify: `app/src/main/java/gr/edusuite/pacer/data/SettingsStore.kt` (key `modules`, a string set of `Module.name`)
- Test: `app/src/test/java/gr/edusuite/pacer/ui/SettingsTest.kt`

**Interfaces:**
- Consumes: `Module`, `Features` (Task 1)
- Produces:
  - `Settings.modules: Set<Module>? = null` (null = never chosen)
  - `fun Settings.features(): Features` = `Features(modules ?: Module.entries.toSet())`

- [ ] **Step 1: Write the failing tests:** `missingModulesMeanEverything` (`Settings().features() == Features.ALL`), `storedModulesAreKept` (`Settings(modules = setOf(Module.WORK)).features().has(Module.WORK)` and not `PLANNING`), `sanitisedKeepsModules`.
- [ ] **Step 2: Run** `--tests 'gr.edusuite.pacer.ui.SettingsTest'`; expect FAIL.
- [ ] **Step 3: Implement** the field, `features()`, and the store: load with `getStringSet("modules", null)?.mapNotNull(Module::parse)?.toSet()`, save with `remove` when null. `data_extraction_rules.xml` needs no change (same file).
- [ ] **Step 4: Run;** expect PASS.
- [ ] **Step 5: Commit** `git commit -m "Store the teacher's modules in Settings"`.

### Task 3: View model

**Files:**
- Modify: `app/src/main/java/gr/edusuite/pacer/ui/PacerViewModel.kt` (beside `setName`)
- Test: `app/src/test/java/gr/edusuite/pacer/ui/PacerViewModelTest.kt` (its `FakeSettings`)

**Interfaces:**
- Consumes: `Settings.features()`, `modulesInUse` (Tasks 1–2)
- Produces:
  - `fun turnOn(vararg modules: Module)` (adds to the stored set, starting from `features().on`; never removes)
  - `val features: StateFlow<Features>` from `settings`

- [ ] **Step 1: Write the failing tests:** `turnOnAddsAndNeverRemoves` (start `setOf(WORK)`, `turnOn(OUTPUT)` → `{WORK, OUTPUT}`, saved to `FakeSettings`), `undoKeepsModulesOn` (turn on, make an edit, `undo()`, module still on).
- [ ] **Step 2: Run** `--tests 'gr.edusuite.pacer.ui.PacerViewModelTest'`; expect FAIL.
- [ ] **Step 3: Implement** through `editSettings`.
- [ ] **Step 4: Run;** expect PASS.
- [ ] **Step 5: Commit** `git commit -m "turnOn and the features flow"`.

### Task 4: The import question

**Files:**
- Modify: `app/src/main/java/gr/edusuite/pacer/data/Backup.kt` (pure helper)
- Modify: `app/src/main/java/gr/edusuite/pacer/ui/SettingsScreen.kt` (`ImportDialog`, before Add or Replace)
- Modify: `app/src/main/res/values/strings.xml`
- Test: `app/src/test/java/gr/edusuite/pacer/data/BackupTest.kt`

**Interfaces:**
- Consumes: `modulesInUse`, `Features` (Task 1), `PacerViewModel.turnOn`, `features` (Task 3)
- Produces: `fun modulesNeeded(incoming: List<SchoolYear>, features: Features): Set<Module>` (in use in the file and off here)

Copy (the brief's wording): title "Turn on {modules}?", body "This backup uses {modules}. Turn them on to bring it in?", buttons "Turn on and import" and "Cancel". Module names: "homework and tests", "book tools", "planning tools" (OUTPUT never appears). Join two with "and", three with commas and "and".

- [ ] **Step 1: Write the failing tests:** `noQuestionWhenEverythingIsOn` (`modulesNeeded(yearWithTests, Features.ALL)` empty), `testsNeedWorkWhenOff` (`== setOf(WORK)`), `onlyMissingModulesAreNamed`.
- [ ] **Step 2: Run** `--tests 'gr.edusuite.pacer.data.BackupTest'`; expect FAIL.
- [ ] **Step 3: Implement** the helper, then the dialog step: when `modulesNeeded` is non-empty, show the question; "Turn on and import" calls `vm.turnOn(*needed)` then the chosen import; Cancel closes with no change. Uses `PacerSheet`-era dialog styling (an `AlertDialog`, as other decisions do).
- [ ] **Step 4: Run** the test, then `./gradlew testDebugUnitTest` (NoHardcodedTextTest); expect PASS.
- [ ] **Step 5: Commit** `git commit -m "Ask before importing data for modules that are off"`.

### Task 5: Gates in the menu models

**Files:**
- Modify: `app/src/main/java/gr/edusuite/pacer/ui/GroupMenuModel.kt`, `LessonMenuModel.kt`, `GroupHomeModel.kt`, `HomeModel.kt`
- Modify (pass-through only): `GroupMenu.kt:150`, `LessonLedger.kt` (its `lessonMenuItems` call), `GroupHome.kt:140,169`, `HomeScreen.kt:155`
- Create: `app/src/main/java/gr/edusuite/pacer/ui/LocalFeatures.kt` (`val LocalFeatures = staticCompositionLocalOf { Features.ALL }`), provided in `PacerApp.kt` from `vm.features`
- Test: `GroupMenuModelTest` (new, `ui/`), `LessonMenuModelTest`, `GroupHomeModelTest`, `HomeModelTest`

**Interfaces:**
- Consumes: `Features` (Task 1), `vm.features` (Task 3)
- Produces: a trailing `features: Features = Features.ALL` parameter on `groupMenuItems`, `lessonMenuItems`, and the Home and group-home builders, so every existing call and test is unchanged.

Gates (from the brief's tables):
- `groupMenuItems`: MY_PLAN, REPACE, COPY_PLAN, DELETE_PLAN, CREATE_PLAN, OWN_PACE and EXTRA need PLANNING; COPY, SHARE, PRINT need OUTPUT; CATCH_UP and TIPS always.
- `lessonMenuItems`: ADD/EDIT/DELETE_HOMEWORK need WORK (EDIT and DELETE also show when the lesson has homework, since data in use always shows); ADD_SUPPLEMENTARY and the MY_PLAN section need PLANNING; EDIT_EXTRA and DELETE_EXTRA show whenever the lesson is an extra.
- Group home: the WORK destination needs WORK or any assignment in the group.
- Home: the upcoming card needs WORK or any upcoming item.

- [ ] **Step 1: Write the failing tests**, one per gate above at `Features(emptySet())`, plus `justPacingMenuKeepsCatchUpAndTips` (`groupMenuItems(hasRows = true, hasCustom = false, otherGroups = 1, large = false, catchUp = true, features = Features(emptySet()))` == `listOf(CATCH_UP, TIPS)`) and `existingHomeworkStaysEditable`.
- [ ] **Step 2: Run** the four test classes; expect FAIL.
- [ ] **Step 3: Implement** the parameters and filters; wire `LocalFeatures.current` at the pass-through call sites.
- [ ] **Step 4: Run** `./gradlew testDebugUnitTest`; expect PASS with no existing test changed.
- [ ] **Step 5: Commit** `git commit -m "Module gates in the menu models"`.

### Task 6: The slim catch-up bar

**Files:**
- Modify: `app/src/main/java/gr/edusuite/pacer/ui/CustomPlanUi.kt` (`PlanSourceBar`)
- Modify: `app/src/main/res/values/strings.xml`
- Test: `app/src/test/java/gr/edusuite/pacer/ui/GroupModelTest.kt`

**Interfaces:**
- Consumes: `CustomPlan.handPlanned()` (Task 1), `LocalFeatures`
- Produces: `fun planBarMode(custom: CustomPlan?, features: Features): PlanBarMode` with `enum class PlanBarMode { NONE, CATCH_UP_ONLY, EDITOR }`

Rule: no custom → NONE; PLANNING on or `handPlanned()` → EDITOR; else CATCH_UP_ONLY. Copy for CATCH_UP_ONLY: "Catch-up: {n} more pages a lesson until {date}." and a text button "Undo catch-up" that calls the existing `vm.deleteCustomPlan(groupId)` (`PacerViewModel.kt:813`).

- [ ] **Step 1: Write the failing tests:** `noPlanNoBar`, `catchUpOnlyAtJustPacing`, `handPlannedShowsEditor`, `planningOnShowsEditor`.
- [ ] **Step 2: Run** `--tests 'gr.edusuite.pacer.ui.GroupModelTest'`; expect FAIL.
- [ ] **Step 3: Implement** `planBarMode` and the CATCH_UP_ONLY branch of `PlanSourceBar` with tokens and `PacerSheet`-era components only.
- [ ] **Step 4: Run** `./gradlew testDebugUnitTest`; expect PASS.
- [ ] **Step 5: Commit** `git commit -m "Slim catch-up bar when planning tools are off"`.

### Task 7: Docs and version

**Files:**
- Modify: `docs/ARCHITECTURE.md` (UI: a short "Modules" paragraph naming `Features.kt`, `Settings.modules`, `LocalFeatures`, the gates), `docs/NAVIGATION.md` ("When" conditions on every gated control), `docs/FEATURE_MODULES_BRIEF.md` (Status: foundation built in this version), `docs/UI_AUDIT_BRIEF.md` (row 5a built), `app/build.gradle.kts`, `CHANGELOG.md`

- [ ] **Step 1:** Update the docs above; one home per fact (CLAUDE.md).
- [ ] **Step 2:** Bump the version per Global Constraints and add the CHANGELOG section ("Added: the groundwork for feature modules; nothing changes on screen yet").
- [ ] **Step 3:** Run `./gradlew testDebugUnitTest lint assembleRelease`; expect BUILD SUCCESSFUL.
- [ ] **Step 4: Commit** `git commit -m "Docs and version for the modules foundation"`.
