# Architecture

How Course Pacer works today. Contributor rules are in [`CLAUDE.md`](../CLAUDE.md); why a feature exists is in its brief (see the [docs index](README.md)); what changed is in [`CHANGELOG.md`](../CHANGELOG.md).

## Overview

Course Pacer is a single-module Kotlin + Jetpack Compose app (`gr.edusuite.pacer`). It ports a web page that paces coursebook pages over the lessons left in a term, and `BRIEF.md` is that spec and its feature-parity checklist.

Data flows one way. `PlanStore` loads `plans.json` into `AppData`. `PacerViewModel` holds it in `StateFlow<UiState>`, and Compose reads it with `collectAsState`. Every mutation goes through the view model's edit helpers, which push undo and schedule a save. The engine is pure Kotlin: it takes a `Plan` (one class over one term, built by `SchoolYear.planFor(class, today)`) and returns the schedule.

Dependency injection is manual: `PacerApplication.store` goes to `PacerViewModel.Factory`. The only permission is `INTERNET`, used only for the update check.

## Model

`model/` holds immutable data classes.

- `AppData`: every `SchoolYear`, `activeYear`, `activeClass`, and `view` (the open group's id, or `"setup"` when none is open).
- `SchoolYear`: the term dates, the days off (`Closure.who` is `ALL_GROUPS`, a class id or a group id), its `SchoolClass`es and its `collections`.
- `SchoolClass`: lesson length, pace, books and groups.
- `Group`: owns its assignments, done lessons, custom plan, extras and its overrides of the class's books (`removedBooks`, `bookDue`).
- `Book` has a `BookType` (`BookType.kt`): SB, WB, GB, WrB, SkB, R, Supp or unclassified. The Supplementary type and `Book.supplementary` are one setting, kept equal by `withType`, the sanitiser and the codec.
- `Settings`: app preferences (theme, palette, new-class defaults, whether the welcome guide was seen, the teacher's `name`, `classOrder`, `planGrouping`, and `seenTips`, the show-once hints already seen). Read and mark hints through `Hints` by stable id.
- `ALL_GROUPS = "all"` stands in for a group id on closures and tests.
- `Drafts.kt` holds `newId()` and `suggestYear()`.
- A fresh install starts with no school years. The old sample year lives on only as a test fixture (`seedYear()`/`seedPlan()` in `app/src/test/.../model/SampleYear.kt`).

## Engine

`engine/` has no Android imports, so it is unit-testable on the JVM.

- `Pacer.kt` holds the rules: `lessonsFor`, `remainingPages`, `allocatePages`, `GroupReport`, `verdict()`, `problems()`.
- `Holidays.kt` covers Orthodox Easter and Greek holidays.
- `Format.kt` handles display and TSV.

### Parity (the contract)

The schedule must match the original web page row for row. Lesson *i* ends at `round(i × pages / lessons)`, halves rounding up in integer arithmetic. Any engine change needs a test in `app/src/test/.../engine/PacerTest.kt`. Each extension below is guarded by a test that shows that, with the feature unused, nothing changes.

### Done lessons

`Group.done` holds lessons marked done. A done lesson's date leaves `lessonsFor` and its pages leave `remainingPages`.

- Ticking lessons in order equals the web page's `afterNextLesson` repeated (`markingDoneInOrderMatchesTheOldRule`). `markDoneUpTo` ticks one lesson at a time for that reason.
- Ticking also moves `Group.from` to the day after the last lesson ticked in order (`settleNextLesson`), and unticking moves it back, so a later calendar edit can't bring past dates back as lessons.
- Lessons tick themselves when their day has passed (`settlePast`, `AutoTick.kt`, [`AUTO_TICK_BRIEF.md`](AUTO_TICK_BRIEF.md)). `PacerViewModel` applies it on launch, resume, midnight and every published edit, outside the undo stack, from `Settings.autoTickFrom`. A lesson recorded with no pages (`recordNotTaught`, `DoneLesson.notTaught`) is the one that didn't happen.
- A done lesson can hold the pages actually taught instead of the planned ones (`recordLesson`, `DoneLesson.edited`, `LESSON_PAGES_BRIEF.md`). The same two rules apply to them, and the allocation rule doesn't change.

### Custom plans

`engine/Custom.kt`, `CUSTOM_PLANS_BRIEF.md`. `Group.custom` holds the teacher's own lessons in order, with no dates: lesson *k* takes the group's *k*-th teaching day.

- `GroupReport(plan, group, source)` lays out the active plan by default (`Source.ACTIVE`), or `PROPOSED` or `CUSTOM` on purpose. A group with no custom plan never leaves the proposed path (`CustomPlanTest.noCustomPlanChangesNothing`).
- Done lessons are shared by both plans. Ticking takes the custom lesson on that day (`takeSlots`); unticking puts it back (`unmarkLesson`, `unmarkLessonsFrom`). Use these rather than `Group.unmarkDone` in the app.
- Nothing re-paces a custom plan by itself. `repaceFrom`, `tidy` and `fitToTerm` run only when the teacher taps them, and never rewrite a `touched` lesson. `repaceFrom(0)` on an untouched plan without a saved catch-up boost equals the proposed plan.
- `tidy` reserves every touched lesson's pages first, then removes repeats and pages that need no teaching from untouched lessons only. Pages of a removed book are never removed. `fitToTerm` re-spreads only untouched overflow lessons, over the untouched lessons after the last touched one; touched lessons past the term stay there, and with no free untouched lesson nothing changes. `repaceFrom` with no untouched lesson to fill returns the plan as it is. `Coverage.overflowEdited` counts the touched overflow lessons. The Overflow, Doubled and Stale warnings offer a fix only while it can change something; otherwise Overflow says why and Doubled and Stale open the lesson ([`TIDY_FIT_BRIEF.md`](TIDY_FIT_BRIEF.md)).
- `shortfallCoverage` counts only remaining core pages placed within ordinary in-term slots, so content coverage with overflow never silences Behind.
- `customAtRate` creates a chosen-rate plan while preserving touched lessons and supplementary placements. `applyOwnPace` creates and activates it in one undo step.

### Lead book

`engine/Lead.kt`. With `SchoolClass.lead` set, the lead book is allocated as before. Every other book without a finish-by date follows it (`followLead`): follower lesson *i* ends at `round(c_i × n_B / n_L)`, halves up in integer arithmetic, where c_i is the lead's cumulative pages. When both books have the same number of `units`, the follower keeps step unit by unit instead. With no lead book, `followLead` returns the allocation untouched (`LeadBookTest.noLeadChangesNothing`). Re-pacing a custom plan follows the lead too.

### Warnings and catch-up

`engine/Warnings.kt`, `CATCH_UP_REPACE_BRIEF.md`. Warnings never change the proposed plan. `catchUpPlan` and `foldBacklog` write into the custom plan, making one if needed, and use `allocateWeighted`, which equals `allocatePages` when all weights are equal (`WarningsTest.equalWeightsMatchAllocatePages`). Touched lessons are kept.

A catch-up plan saves its chosen `catchUpBoost`. Re-pacing keeps that policy while the group is short, including partial re-pacing, and returns to normal allocation once caught up. My plan offers Adjust catch-up directly. Old custom plans have no saved boost: use Adjust catch-up once rather than infer intent from their pages.

### Supplementary books

`engine/Supplementary.kt`, `SUPPLEMENTARY_BOOKS_BRIEF.md`. A `Book.supplementary` book is never paced.

- `remainingPages` and `bankedPages` have none for it; `bookStats` keeps a zero placeholder so indices still match `plan.books`; every pacing path (coverage, lead, lags, catch-up, fold) works on `coreBooks`.
- Its pages live in custom lessons and done lessons like any book's. Re-pace, tidy, fit to term, catch-up and copy keep supplementary pages and `inLesson`.
- `saveLesson` is the one way to plan one: whole or nothing, it keeps the core pages before the first in-lesson placement in `replacementBase` and re-paces from the next lesson once.
- Output columns come from `outputColumns()`, so a supplementary book shows only where used.
- Guarded by `SupplementaryBookTest.noSupplementaryChangesNothing`.

### Extra lessons

`engine/Extras.kt`, `EXTRA_LESSONS_BRIEF.md`. `Group.extras` pins core-book pages to dates in both plans.

- Active pins bypass weekdays and closures, leave ordinary `teachingDates` and reserve their pages outside `remainingPages`. Out-of-range, taught or test-blocked pins reserve nothing.
- Ticking and unticking never consume or restore a custom slot.
- Add, edit and delete run one untagged group edit, re-pacing an active custom plan without changing touched lessons.
- Extras obey the group end.

### Group overrides and optional books

`GROUP_SETTINGS_BRIEF.md`. `GroupReport` derives `plan.forGroup(group)` before building any arrays: the earlier group/year end, class skips plus group skips, and default books plus selected `groupOnly` entries.

- All book indices belong to `report.plan`, never a raw class catalogue.
- Direct remaining and banked pages also honour membership and additive skips.
- A default book can be left out of one group (`Group.removedBooks`, `withBookRemoved`: never the last paced book). The group keeps its taught pages and positions, so adding the book back resumes where it was; hand-placed pages of it stay in their custom lessons and count as stale in the warnings. Only default books are removed; optional ones are selected. Done lessons still show pages taught from a book the group dropped: `lessonsOf` puts them in `Lesson.history`, not in `cells`, so pacing and the book indices never see them ([`REMOVED_BOOK_HISTORY_BRIEF.md`](REMOVED_BOOK_HISTORY_BRIEF.md)).
- A group can set its own finish-by date for a book (`Group.bookDue`). `forGroup` makes it the book's `due`, replacing the class's, and clamps it to the group's end.
- Unselected or removed leads fall back to no lead for that group.
- With no overrides, pacing is unchanged.
- Override edits preserve custom content and expose existing missing, stale and overflow fixes rather than silently re-pacing.

### Collections

`SchoolYear.collections` contains `ClassCollection(id,name,classIds)`. `withCollection` moves selected classes out of other collections; removing a collection keeps classes. `classSections` keeps collection order then uncollected classes and applies `ClassOrder` within each. Name sorting uses the ROOT collator; Most behind puts unmeasured after measured zero counts, retaining original order for remaining ties. `Settings.classOrder` persists outside undo. Sanitisers drop missing references and duplicate membership. Backup Add remaps clashing collection ids.

### Assignments

`ASSIGNMENTS_BRIEF.md`. `Group.assignments` owns Homework, Test and Worksheet records.

- `migrateAssignments` converts legacy class homework and test lists on load, import and save, including one copy of each class-wide test per group. New JSON writes assignments only.
- `Plan.homeworkPages` reads homework assignment pages. Tests and worksheets project to existing lesson and export events, and only their `takesLesson` blocks dates.
- Homework pages leave pacing as soon as the assignment exists, whatever its dates or Done state (`HomeworkPacingTest` guards this and no-homework parity). Worksheets and tests carry no book pages in the editor.
- Legacy `Plan` lists remain supported for parity fixtures.
- `AssignmentTest.migratedAssignmentsChangeNothing` guards scheduling, labels, figures and custom rows.

### Lesson homework

`HOMEWORK_BRIEF.md`. Assignments store an optional `completed` flag, defaulting to false for older data. Completion has no scheduling effect. Homework content type (worksheet or test) is no longer chosen in the editor; saved values keep their label and links. `AssignmentLinks.kt` suggests eligible class and homework dates, skipping blocked dates and closures, and prevents repeated links. `withLinkedAssignment` changes both records in one edit; deleting a counterpart clears surviving links. `AssignmentEditor` saves first, then offers an optional native pairing dialog with Skip/Back. The lesson menu and TalkBack share named actions for every homework set or due that date; ordinary, slim and hero rows show HW content and due dates. Manual custom-plan content is preserved without automatic re-pacing.

### Book units

`Units.kt`, `UNITS_BRIEF.md`. Lesson windows are shared by the lead and linked books. Strict starts cover the full book, group overrides are inert while the class switch is off, and disabled-unit parity includes custom and catch-up paths. Taught, touched and manual content stays intact; insufficient windows and boundary violations are warnings, not silent page loss. Revisions are per-group ordinary assignments plus metadata: same-ID edits preserve homework pairs and teacher fields, and removal clears surviving pair links.

## Data and storage

`data/`: `JsonCodec` maps to `org.json` by hand. Weekdays are numbered **0 = Sunday**, unlike `java.time.DayOfWeek`; dates are ISO strings. The format stays readable from the web page's localStorage export through `migratePlans` (`Migration.kt`), which makes each plan a year with one class. If the schema changes, bump `JsonCodec.VERSION` and keep reading old files.

Reading is lenient per record: one it can't read is left out and one without an id gets a new one, so a single bad record doesn't cost the whole file. A book with no stored `type` (a file before schema 15) gets one from its name (`guessBookType`) or is unclassified (`readBookType`); a stored type is never guessed again.

| Version | Adds | Release |
| --- | --- | --- |
| 1 | the web page's flat `plans` list, read through `migratePlans` | — |
| 2 | years | 1.3.0 |
| 3 | each group's done lessons (pages as range strings) | 1.4.0 |
| 4 | `edited`: done lessons recorded with other pages | 1.5.0 |
| 5 | a group's custom plan (`custom`) | 2.7.0 |
| 6 | a class's lead book (`lead`) and books' unit start pages (`units`) | 2.8.0 |
| 7 | the class's left-behind threshold (`lagLessons`) | 2.9.0 |
| 8 | supplementary books (`supplementary`, `include`, `countInLesson`); on custom lessons `inLesson` and `replacementBase` | 2.10.0 |
| 9 | the optional custom-plan `catchUpBoost` | 3.6.2 |
| 10 | date-pinned extra lessons (`Group.extras`) | 3.7.0 |
| 11 | group end, skip and optional books; class collections | 3.10.0 |
| 12 | `Group.assignments`; legacy class homework/tests convert on load/import/save and are no longer written | 3.11.0 |
| 13 | optional homework `contentType` and `linkedId` | 4.0.0 |
| 14 | class units and book links; per-group overrides and revisions | 4.1.0 |
| 15 | `Book.type`; `Group.removedBooks` and `Group.bookDue` | 0.4.2.0 |
| 16 | `Assignment.completed` (homework marked done); written since 0.4.12.0, versioned in 0.4.12.1 | 0.4.12.1 |

Releases up to 4.1.0 used three-part numbers; from 0.4.1.1 they have four (`CHANGELOG.md` explains the mapping).

`PlanStore` writes `plans.json` in `filesDir` through `AtomicFile`, behind the small `PlanFile` seam so the load rules are unit-tested on the JVM.

- If the file is missing, the app starts with no school years.
- `save` returns a `SaveResult`: `Saved`, `Failed` or `Locked`. Only `Saved` means the data is on disk.
- A read error (`IOException`) leaves the file alone: `Loaded.readFailed` is set and `save` returns `Locked` until a later `load` succeeds or `unlock` is called.
- The ViewModel counts edits (`revision`) and remembers the newest one written (`savedRevision`). `saveNow` writes when they differ, from the debounced job, `flush()` (on stop) and `retrySave()`. A success records only the revision it wrote, so it never clears a newer edit.
- `saveProblem` (`NotSaved`, `ReadLocked`, `LockedWithEdits`) drives `SaveNotice`, a lasting snackbar outside the toast queue with Retry or Try reading again, and Export a copy. `retryRead` reads without unlocking; it replaces the data in memory and unlocks only if nothing was edited since launch (`SAVE_STATUS_BRIEF.md`).
- Only content that can't be parsed (including nesting deep enough to overflow the stack) is moved to the first free `plans.json.bad`, `plans.json.bad-2`, … (never overwriting one). The app starts empty too, with a one-time notice (`SetAsideDialog`) that offers to save a copy where the teacher can reach it.

`SettingsStore` keeps `Settings` in SharedPreferences (`settings.xml`), outside undo. Export and import (`BACKUP_BRIEF.md`) write and read the same JSON through the system file pickers: `JsonCodec.readImport` refuses newer versions and junk, `readBoundedText` in `Backup.kt` streams the file and refuses one over `MAX_IMPORT_BYTES` (50 MiB, its own "too large" message) so a huge file cannot fill memory, and `addYears` in `Backup.kt` (plain Kotlin, unit-tested) gives clashing years a new id or name.

`data_extraction_rules.xml` backs up `plans.json` and `settings.xml`. A new stored file must be added there.

## Update check

`update/`, `UPDATES_BRIEF.md`. `UpdateModel.kt` and `UpdateChecker.kt` are plain Kotlin and unit-tested:

- Version parsing of three-part and four-part versions. A three-part `X.Y.Z` compares as `[X, Y, Z, 0]` and a four-part `0.X.Y.Z` as itself, so every old version counts as newer than any `0.x.y.z`.
- Release validation against this repository's release URLs.
- The once-a-day throttle, note trimming (`shortNotes` keeps up to four plain bullets of the changelog section), and folding a result into `Settings`.

`GitHubReleases.kt` is the only network code (`HttpURLConnection`, `org.json`, one GET to `releases/latest`). `PacerViewModel` checks on launch and on Check now. `Settings` holds `autoUpdateCheck`, `lastUpdateCheck` and `availableUpdate`. Debuggable builds never check. The ☰ dot is `MenuButton` (`NavUi.kt`); Settings → Updates is `UpdatesSection` (`UpdatesUi.kt`).

## UI

### View model, edits and undo

One `PacerViewModel` exposes `StateFlow<UiState>`. Every mutation goes through `edit`, `editYear`, `editClass`, `editGroup` or `editBook`, which push undo (at most 60 steps; edits with the same tag within 1.2 s merge) and schedule a debounced save (400 ms). The edit helpers return whether state changed; custom-plan fixes show Undo only for a change. `UndoGate` gives each notification a unique token, and its display starts the timeout independently of the history step. `MainActivity.onStop` calls `vm.flush()`. The view model takes its settings through `SettingsPort`, and its scope, IO dispatcher and clock as parameters, so `PacerViewModelTest` runs on the JVM with fakes and a manual dispatcher.

Opening another school year clears the undo history. Deleting a year lands on Home, whose snackbar offers undo (which opens that year again). Deletes happen at once with a 5 s Undo snackbar; only Import's Replace all still asks first. Undo shows in the top bar only where `Screen.showsUndo` (Group, Class, Classes, School year, Tests, Homework).

Only the open year, class and group persist (`activeYear`, `activeClass`, `view`), but a fresh start always opens Home (`Nav.launch`, 0.4.1.3): the saved group is written and never reopened. With no year open, Home shows the one that starts last (`defaultYearId`).

### Feature modules

`model/Features.kt` holds the bricks and modules of [`FEATURE_MODULES_BRIEF.md`](FEATURE_MODULES_BRIEF.md): each `Brick` names its `Module` (null for the core) and its `needs`, and `Brick.inUse` reads which saved data uses it. `Settings.modules` stores the teacher's modules (null means every one, so older installs keep every control); `turnOn` only adds and `chooseModules` applies once. `featuresOf` adds the open year's bricks in use, and `PacerApp` provides the result as `LocalFeatures`. Each menu control names its brick (`GroupMenuItem.brick()`, `MenuItemKind.brick()`, `GroupDestination.bricks()`), and the menu models filter by `Features.has`, so saved data always shows. Import asks to turn on what a file needs (`modulesNeeded`), and a plan written only by fixes gets the slim `FixPlanBar` while Planning tools are off (`planBarMode`).

### Navigation

A back stack of `Screen`s in `UiState`, with rules in `Nav.kt` (plain Kotlin, unit-tested). There is no navigation library. Every top bar starts with ☰, which opens `AppDrawer` (`NavUi.kt`, a `ModalNavigationDrawer`): search over the year's groups, then five items in a fixed order: Home, Pacing overview with its count of groups behind, Classes & Groups, School year (its name shortened by `yearShort`), Settings. The menu, its search and the title switcher (`GroupSwitcherSheet`) open screens through `Nav.goTo`, which goes back to a screen already on the stack instead of stacking it again. System Back still pops, closing the menu first.

Classes & Groups is the "You are here" list. `menuLevelFor` gives the level that holds the open screen (classes, one class, or one group) and `menuRows` the rows of a level: the classes, a class's Class settings and groups under a back row, or a group's five `GroupPlace`s with the current one marked. A level hides the ones above it; its back row changes the list, not the screen. The list starts at the teacher's place each time the menu opens. A class or group deleted while its level is open falls back to the level above. `MenuState.followScreen` moves the list with the teacher inside it and leaves it alone elsewhere; on a tablet `groupPlaces` leaves Group home out because it is shown as Plan. The title switcher (`switcherPages`, `switcherGroups`) is one sheet: the open group's places, then the class's other groups.

What the menu lists comes from `NavModel.kt` (`searchGroups`, `yearStatus`, `groupWarnings`, and `wholeLessons`, the one rounding helper for status labels), plain Kotlin and unit-tested. There is no recent-groups list.

The top bar keeps the title's room: `foldBarActions` estimates from the title's length, the bar's width and the text size whether the title and buttons fit, and if not Undo and the warning button fold into ⋮ (`FoldedActions`), which carries the warning count. The ⋮ menu's sections come from `groupMenuSections`, so a heading never stands over nothing and Work leaves out the lesson sections. The update dot on ☰ uses the `mark` token.

Route graph: Home is the root. From it: Classes → Class → Group home → Plan / Work / Stats (Class also links directly to group Work), School year, Pacing overview (`Screen.Overview`) and Settings. Home's tests card opens its group's Work page.

### Home

The open year's dashboard (`PACER_DESIGN.md` D3, D4): "Hello, *name*" (`Settings.name`) over today's date and the year, the `PacingBoard` (one cell per group by class, groups most behind first like Pacing overview, rust "−N" only for groups behind, a blinking dot and "Today" when the group has a lesson today, each cell one TalkBack item) and the card of the next tests and worksheets. Its figures come from `HomeModel.kt` (`pacingBoard`, `behindOnly`, `upcoming`), plain Kotlin and unit-tested. Large text keeps only the groups behind; groups with nothing to measure are "set-up needed", never "on pace". Home has no Done: lessons tick themselves ([`AUTO_TICK_BRIEF.md`](AUTO_TICK_BRIEF.md)).

### Group screens

Group home (`Screen.Group(id, page)`) has the accent-filled hero, a Progress strip (`progressLine`; `showsFix` adds a round Fix that opens Warnings), the Pace track, Due soon and four outlined pills (`GroupDestination`: Plan, Work, Stats, Settings; `homePills` picks 2 × 2, one row or a column). The hero (`TodayBox`) stacks its available actions on the right; Homework follows the resolved Homework brick. On a tablet it opens Plan in the detail pane, whose links reach Work, Stats and Settings.

- **Hero** (`TodayBox.kt`): tapping the planned pages opens the taught-pages edit, and saving always marks the lesson done (`recordLesson`), even when the pages equal the plan. `heroActions` says when each is offered.
- **Today**: `PacerViewModel.today` is re-read on resume, at midnight and when the system changes the date or time zone (`TodayProvider`, `rememberToday`). It is the app's only date. Every `Plan` is built with it (`AppData.plan(today)`, `SchoolYear.planFor(class, today)`; neither has a default), so homework banking (`remainingPages`, `bankedPages`), unit-revision dates and the hero all read it, and screens that cache plans or reports key them on it. Code that reads only a plan's calendar passes `NO_TODAY`. See [`ONE_TODAY_BRIEF.md`](ONE_TODAY_BRIEF.md).
- **Title switcher**: the group's name opens one sheet from every group page: the group's places (the current one ticked), then the class's other groups.

- **Plan** (`GroupScreen.kt`, `LessonLedger.kt`): today's `TodayBox`, the custom-plan line (`CustomPlanLine`, or the My plan | Proposed switch with an In use label), then the view controls, then `weekSections` of `LessonRow`s and `SlimRow`s (days off, whole-lesson tests), with earlier and done lessons collapsed above today and the list opening on it. The pinned Weekly view | Monthly view switch remembers `Settings.planGrouping`. Under the My plan | Proposed switch, Weekly view shows the term-week `DayStrip`: one row of round week chevrons and a label that opens the month picker, the days, and a + pill that adds an extra lesson on the selected day. The selected day starts on the lesson after the one in the box, and picking a day scrolls the list to its row (`planRowIndex`) and highlights it. Monthly view shows the month chevrons (month text centred, with a picker); Monthly view filters upcoming entries before partitioning, and preserves earlier entries across months. In a custom plan, `differingBooks` (`PlanDiff.kt`) marks the core books whose pages differ from the proposed lesson that day; `LessonRow` colours only those pages with `warn` and TalkBack adds "differs from the proposed plan" (done lessons, tests, extras, free days and supplementary pages are never marked). The compact hero reserves action space for previews. My plan actions live in the group menu.
- **Rows**: each row is one TalkBack node whose custom actions are its long-press menu (`lessonMenu`, chosen by `lessonMenuItems` in `LessonMenuModel.kt`: planned pages for future lessons, recording taught pages for today and earlier). Its edge colour comes from `Lesson.kind`, and a `DateGutter` (day number over weekday) replaces the date text. Whole-lesson tests and worksheets are as tall as lessons with the kind in bold; a day off is slim and ignores taps. With a custom plan the menu is two parts (`MenuSection`): This lesson, and My plan behind one item. Book labels (`classBookLabels`) are worked out over the class's books, so a book keeps its label in every group. Finishing a unit does not open the revision sheet; the unit's revision row asks.
- **Stats** (`GroupStats.kt`, `StatsModel.kt`) follows the selected active/custom or proposed source and reports planned count, rate and end. It is the verdict heading with in-term shortfall coverage, `FigureStrip`, `PaceTrack` (its tick is `inStepPages`, the engine's `followLead` over the whole book, so no new figure), an outlined proposed-position line for books behind, month columns and details. Own pace is offered on long-press of Pace needed (also in the large-text group menu).
- **`GroupModel.kt`** holds `bookLabels` (a type's abbreviation, numbered when a class has two of a type; unclassified books use `bookInitials`/`shortBookNames`), `weekSections` and `inStepPages`, plain Kotlin and unit-tested.
- **Warnings**: `WarningsSheet`, opened from the warning button in the group's top bar. Applying a fix closes it so its notification is visible.
- **Group menu** (`GroupMenu.kt`): sections Share & print (Copy, Share, Print with `PrintDialog(only = …)`, this group chosen), Plan (My plan actions, Own pace, Adjust catch-up), Lessons (Add extra lesson) and Help (Tips) (`GroupSheet`).
- **Group settings**: reached from the Settings pill on group home. `Screen.GroupSettings(id)` keeps the group on the navigation stack and in the tablet detail pane, shows Undo, and is pruned after deletion. Its inputs use the raw class plan to show defaults; its reports use effective plans. Group Greek holiday additions are scoped and deduplicate against applicable inherited closures. Group name and lesson days live here; Class’s pencil and missing-days fixes open this screen. Optional definitions are edited once from the class book editor. Each book’s visible options button or long-press opens its options (leave it out, the group's own finish-by date with "Use class date"); long-pressing one of the group's own days off offers Edit and Delete (`withClosureEdited`), while inherited days off stay read-only.
- **Work**: saves and deletes each run one group edit and the existing Undo.

### List screens

The list screens use the ledger rows in `Rows.kt` (`SectionHead`, `RowGroup`, `LedgerRow` with an optional edge and date column, `RemoveMenu`, `FootActions`). What Classes, School year and Pacing overview list comes from `ScreensModel.kt` (`pacingOverview`, `sortGroups`, `classSummaries`, `termSummary`), plain Kotlin and unit-tested. School year has year chips and + New year at its top, a Print row (`PrintRow`), and Rename and Delete at its foot. Class has the same Print row, on its class. Pacing overview's warning button opens `YearProblemsSheet`, and Class's opens `ClassProblemsSheet` (`screenProblems`). The first-run guide is the full-page `WelcomePage` (`Guide.kt`), which Settings → Getting started reopens. Show-once hints are `ShowOnceHint` (`Guide.kt`) with an id from `Hints` (`model/Settings.kt`), kept in `Settings.seenTips`.

### Sheets and shared controls

Every sheet is `PacerSheet` (`PacerSheet.kt`): a Dialog drawn as a bottom sheet so its motion uses `MotionSpec` and snaps when animations are off. It has a handle, the title, ✕, the content, and a filled `SheetAction` only when there is something to save. ✕, Back, a scrim tap and a swipe down go through one close request; `sheetCloseDecision` (`SheetClose.kt`, plain Kotlin, unit-tested) asks Keep editing / Discard when the sheet is `dirty`. Staged sheets work that out with `rememberDraftChanged(fields…)`, which compares a `draftSignature` saved when the sheet opened. Drafts are not restored after process death. `Picker` is one TalkBack node (label, choice, dropdown role) and ticks the choice in its menu; `SwitchRow` is one toggleable row.

### Large text and tablets

`Layout.kt`: `largeText(fontScale)` from 1.5 reflows rows and buttons and shows less, with the rest one tap away (`behindOnly` in `HomeModel.kt`, `largeOverview` in `ScreensModel.kt`, Stats' "More stats", the custom-plan line in the group's ⋮). On a tablet (`windowLayout(widthDp)`: 600dp and 840dp) `Nav.panes` puts Pacing overview in a list pane beside the open group's detail pane. `AppDrawer(permanent = true)` stays open from 840dp (no ☰) while `Settings.drawerPinned` is on (the default); its pin turns it into the usual sliding menu behind ☰. Back pops the stack as usual, so it closes the detail pane first. There is no background pattern.

### Print

`PrintSheet` renders HTML into a `WebView` for `PrintManager`: fixed table layout with reserved number, date and note columns, wrapping text, and landscape when a group prints more than four book columns. `PrintDialog` chooses this group, its class or the year, offering only the scopes `PrintScopes` finds printable (a group is printable when its report has rows); the group menu hides Print and `PrintRow` hides itself when none is. See `PRINT_BRIEF.md`.

## Theme and tokens

Colours live only in `ui/theme/Tokens.kt`, checked by `ContrastTest` across the six Pacer-owned sets (three palettes, light and dark), their two primaries, class colours, kind tints and lesson-kind edges (measured pairs: `PACER_ACCENT_CONTRAST.md`). Type is in `Type.kt`: the bundled Outfit variable font (`res/font/outfit.ttf`, OFL); numbers use `Figures`, Outfit with tabular figures, merged into a style (`bodyMedium.merge(Figures)`). Corners, `Space` and `Motion` are in `Shape.kt`. Vertical rhythm on list and settings screens is `Rhythm`: `Rhythm.section` (`Space.l`) between a screen's blocks (`ScreenList`'s default gap) and `Rhythm.inSection` (`Space.s`) between the parts of one block (`SectionColumn`: heading, rows, hint, button). `SpacingTokensTest` fails on a literal `NN.dp` padding or gap in those screens. Screens read `MaterialTheme` and `Pacer.colors`.

- Palettes: Freetown (default, stored as `BONE`), Arusha (`SAFFRON`) and Oxford (`OXFORD`), each light and dark (`PACER_DESIGN.md` D13). An unknown stored name falls back to Freetown. Each palette has two primaries (`accent`, `second`), five class colours (`classTile` picks one by the class's position in the list) and kind tints (`kindTint`), as D14 describes. `Settings.palette` is saved immediately, outside undo. Wallpaper (dynamic) colour stays off. Arusha's saffron is fill-only on light paper: links, text buttons, cursors and focus use `accentText`. `ReadablePicker` handles Material date-picker internals.
- Dropdowns use `PacerDropdownMenu`, whose fixed 12dp `PacerMenuShape` avoids the theme's percentage-rounded `extraSmall`, which clips the first and last rows. Pill controls keep their shape.
- Icons are Material Symbols Rounded outlined path data in `AppIcons.kt`.
- Visible copy lives in `res/values/strings.xml`; `NoHardcodedTextTest` guards the UI sources. Engine strings, `Nav.problems`, PrintSheet's HTML and printed `PRINT_NOTE`, and stored model and view-model defaults stay in Kotlin. The two `NO_BOOKS`/`NO_GROUPS` literals in ClassScreens are matching keys for `Nav.problems`, not rendered UI.
