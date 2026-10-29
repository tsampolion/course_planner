# Feature Modules Foundation Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Build step 2 of the modules order, the invisible foundation: a module registry, stored modules, gates in the pure menu models, the import question and the slim catch-up bar, with every module on for every install until the Welcome presets ship (UI audit PR 8).

**Architecture:** A pure `model/Features.kt` holds the bricks (brief, "Bricks"): each names its module, its needs and the data that means it is in use. `Settings.modules` stores the teacher's set; `PacerViewModel.features` combines it with the data and Compose reads it through `LocalFeatures`. Every control names its brick through an exhaustive `when`, and the pure `*Model.kt` files filter by `features.has(brick)`, so gates are unit-tested and no screen tests a module directly.

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
6. A brick whose module is on but whose need is off stays hidden: unit revisions with Work on and Book tools off (Task 1 test `needsAreHonoured`).

---

### Task 1: Bricks and modules

**Files:**
- Create: `app/src/main/java/gr/edusuite/pacer/model/Features.kt`
- Test: `app/src/test/java/gr/edusuite/pacer/model/FeaturesTest.kt`

**Interfaces:**
- Produces:
  - `enum class Module { WORK, BOOK_TOOLS, PLANNING, OUTPUT }` with `companion object { fun parse(name: String?): Module? }`
  - `enum class Brick(val module: Module?, val needs: Set<Brick> = emptySet())` (`module == null` is core), entries exactly as the brief's Bricks table: `PACING`, `PLAN_BASE`, `CATCH_UP(needs PLAN_BASE)`, `HOMEWORK`, `TESTS`, `UNIT_REVISIONS(needs UNITS)`, `LEAD_BOOK`, `UNITS`, `OPTIONAL_BOOKS`, `GROUP_BOOK_RULES`, `PLAN_EDITOR(needs PLAN_BASE)`, `OWN_PACE(needs PLAN_BASE)`, `EXTRA_LESSONS`, `SUPPLEMENTARY(needs PLAN_EDITOR)`, `PRINT`, `COPY_SHARE`. Declare `needs` as a function or lazy property so forward references compile.
  - `fun Brick.inUse(years: List<SchoolYear>): Boolean`
  - `fun modulesInUse(years: List<SchoolYear>): Set<Module>` = the modules of the bricks in use
  - `fun CustomPlan.handPlanned(): Boolean` = any lesson `touched`
  - `data class Features(val on: Set<Module>, val inUse: Set<Brick> = emptySet()) { fun has(b: Brick): Boolean; companion object { val ALL: Features } }`. `has` is true when `b` is in `inUse`, or when its module is null or in `on` and every brick in `needs` `has` too.

`Brick.inUse` reads existing fields only:
- HOMEWORK / TESTS: a `Group.assignments` entry of that type (WORKSHEET counts as TESTS); UNIT_REVISIONS: any `Group.unitRevisions`.
- LEAD_BOOK: a class `lead`; UNITS: `unitsEnabled`, a book with `units`, or `unitsOverride != null`; OPTIONAL_BOOKS: any `Group.extraBooks`; GROUP_BOOK_RULES: `removedBooks`, a non-blank `Group.skip` value or `bookDue`.
- PLAN_EDITOR: a `Group.custom` whose `handPlanned()` is true; SUPPLEMENTARY: a book with `supplementary`; EXTRA_LESSONS: any `Group.extras`.
- Core bricks, OWN_PACE and the OUTPUT bricks: never (they hold no data of their own).
Class-level `Book.skip`, `Book.due` and `Group.end` are core and never count.

- [ ] **Step 1: Write the failing tests** in `FeaturesTest` using `model/SampleYear.kt` fixtures:
  - `emptyYearUsesNothing`: `modulesInUse(listOf(sampleYear))` == `emptySet()`
  - `assignmentMeansWork`, `unitRevisionMeansWork`, `leadBookMeansBookTools`, `groupSkipMeansBookTools`, `classSkipAndDueAreCore` (expects `emptySet()`), `supplementaryBookMeansPlanning`, `extraLessonMeansPlanning`
  - `catchUpPlanIsNotPlanning`: a group whose `custom` comes from `GroupReport.catchUpPlan(2)` → `!Brick.PLAN_EDITOR.inUse(...)`
  - `oneTouchedLessonIsPlanning`: the same plan with one lesson `touched = true` → `Brick.PLAN_EDITOR.inUse(...)`
  - `coreBricksAlwaysOn`: at `Features(emptySet())`, `has(PACING)`, `has(PLAN_BASE)` and `has(CATCH_UP)`
  - `needsAreHonoured`: `Features(setOf(Module.WORK)).has(Brick.UNIT_REVISIONS)` is false; with `BOOK_TOOLS` added it is true
  - `everyNeedIsAcyclic`: walking `needs` from every brick never returns to it
  - `inUseRevealsBrick`: `Features(emptySet(), inUse = setOf(Brick.HOMEWORK)).has(Brick.HOMEWORK)`
  - `allHasEveryBrick`: `Brick.entries.all { Features.ALL.has(it) }`
  - `parseIgnoresUnknown`: `Module.parse("NOPE") == null`
- [ ] **Step 2: Run** `./gradlew testDebugUnitTest --tests 'gr.edusuite.pacer.model.FeaturesTest'`; expect compile failure.
- [ ] **Step 3: Implement** `Features.kt` with the interfaces above. KDoc on `Brick` points to the brief's table instead of repeating it.
- [ ] **Step 4: Run the same command;** expect PASS.
- [ ] **Step 5: Commit** `git commit -m "Bricks, modules and modulesInUse"`.

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

- [ ] **Step 1: Write the failing tests:** `missingModulesMeanEverything` (`Settings().features() == Features.ALL`), `storedModulesAreKept` (`Settings(modules = setOf(Module.WORK)).features().on == setOf(Module.WORK)`, so `has(Brick.HOMEWORK)` and not `has(Brick.PLAN_EDITOR)`), `sanitisedKeepsModules`.
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
  - `val features: StateFlow<Features>` from `settings` and the open year's data: `Features(settings.features().on, Brick.entries.filter { it.inUse(listOfNotNull(data.year)) }.toSet())`

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
- Consumes: `Brick`, `Features.has` (Task 1), `vm.features` (Task 3)
- Produces:
  - `fun GroupMenuItem.brick(): Brick`, `fun MenuItemKind.brick(): Brick`, `fun GroupDestination.brick(): Brick`, each an exhaustive `when` beside the enum (as `GroupDestination.titleRes()` is), so a new item does not compile until it names its brick
  - a trailing `features: Features = Features.ALL` parameter on `groupMenuItems`, `lessonMenuItems`, and the Home and group-home builders, which end with `.filter { features.has(it.brick()) }`; every existing call and test is unchanged

Brick per control (from the brief):
- `GroupMenuItem`: MY_PLAN, REPACE, COPY_PLAN, DELETE_PLAN, CREATE_PLAN → PLAN_EDITOR; OWN_PACE → OWN_PACE; EXTRA → EXTRA_LESSONS; COPY, SHARE → COPY_SHARE; PRINT → PRINT; CATCH_UP → CATCH_UP; TIPS → PACING.
- `MenuItemKind`: ADD/EDIT/DELETE_HOMEWORK → HOMEWORK; ADD_SUPPLEMENTARY → SUPPLEMENTARY; PLAN_LESSON, ADD_BEFORE, MOVE_EARLIER, MOVE_LATER, REPACE, REMOVE → PLAN_EDITOR; EDIT_EXTRA, DELETE_EXTRA → EXTRA_LESSONS; RECORD_TAUGHT, NOT_TAUGHT, DONE_UP_TO, NOT_DONE_FROM → PACING.
- `GroupDestination`: WORK → HOMEWORK if the group has homework, otherwise TESTS (shown when either `has`); PLAN, STATS, SETTINGS → PACING.
- Home's upcoming card → TESTS.

Existing homework and extra lessons stay editable because `vm.features` marks their bricks in use.

- [ ] **Step 1: Write the failing tests**, one per brick above at `Features(emptySet())`, plus `justPacingMenuKeepsCatchUpAndTips` (`groupMenuItems(hasRows = true, hasCustom = false, otherGroups = 1, large = false, catchUp = true, features = Features(emptySet()))` == `listOf(CATCH_UP, TIPS)`) and `existingHomeworkStaysEditable` (`Features(emptySet(), inUse = setOf(Brick.HOMEWORK))` keeps EDIT_HOMEWORK).
- [ ] **Step 2: Run** the four test classes; expect FAIL.
- [ ] **Step 3: Implement** the `brick()` mappings and the generic filter; wire `LocalFeatures.current` at the pass-through call sites. Compose-only fields call `LocalFeatures.current.has(Brick.X)` in the later PRs that redesign them.
- [ ] **Step 4: Run** `./gradlew testDebugUnitTest`; expect PASS with no existing test changed.
- [ ] **Step 5: Commit** `git commit -m "Every menu control names its brick"`.

### Task 6: The slim catch-up bar

**Files:**
- Modify: `app/src/main/java/gr/edusuite/pacer/ui/CustomPlanUi.kt` (`PlanSourceBar`)
- Modify: `app/src/main/res/values/strings.xml`
- Test: `app/src/test/java/gr/edusuite/pacer/ui/GroupModelTest.kt`

**Interfaces:**
- Consumes: `CustomPlan.handPlanned()` (Task 1), `LocalFeatures`
- Produces: `fun planBarMode(custom: CustomPlan?, features: Features): PlanBarMode` with `enum class PlanBarMode { NONE, CATCH_UP_ONLY, EDITOR }`

Rule: no custom → NONE; `features.has(Brick.PLAN_EDITOR)` (true when Planning tools are on or `handPlanned()` marks it in use) → EDITOR; else CATCH_UP_ONLY. Copy for CATCH_UP_ONLY: "Catch-up: {n} more pages a lesson until {date}." and a text button "Undo catch-up" that calls the existing `vm.deleteCustomPlan(groupId)` (`PacerViewModel.kt:813`).

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

## Built in 0.4.11.0, and what is left

Tasks 1 to 7 are built. Carried to the later steps:

- Home's tests card has no gate: it lists only tests that exist, so data in use already decides it.
- Compose-only gates (the book form, Group settings, the hero's Homework button, the Units revision step) land with audit PRs 6 and 7; the Day strip's +, a day off's Add extra lesson and Plan's Create line are gated here.
- "Turn on and import" keeps the modules on if the Add or Replace question is then cancelled; acceptable while modules only turn on.

