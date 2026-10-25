# One today

Status: built in 0.4.4.10. Scheduling on any given date is unchanged; the engine's page pools follow the app's shared date.

## Why

0.4.3.0 gave the screens one "today": `PacerViewModel.today`, kept fresh on resume, at midnight and when the system changes the date or time zone. The engine still had its own. `Plan.today` and `SchoolYear.planFor` defaulted to `LocalDate.now()` and no caller passed a date, so homework banking and unit-revision dates read the clock whenever a plan was built. Around midnight the revision sheet could offer a date that `saveUnitRevision` (which used the view model's date) refused, and cached reports kept the old date until something else changed.

## Decisions

- **Banking and revision dates follow the shared today**, not the clock. The owner signed this off as the only behaviour change: on a fixed date nothing moves.
- **One source.** The view model's `today` flow, fed by the existing `TodayProvider`. No second timer or listener.

## Rules

- `Plan.today` and `SchoolYear.planFor(class, today)` have no default. `AppData.plan` became `AppData.plan(today)`. The compiler finds every caller; screen-level models (`nextLesson`, `pacingBoard`, `pacingOverview`, `classSummaries`, `yearStatus`, `groupsBehind`, `Nav.problems(year, today)`, `markNextLessonDone`) take the date too.
- Composables read it with `rememberToday()`, the view model with `today.value`. A report or list cached with `remember` is keyed on it so it rebuilds at midnight. `NewYearDialog` suggests a year from it.
- `UnitRevisionSheet` offers dates from `report.plan.today`; `saveUnitRevision` builds its plan and checks the date with the same `today.value`, so the two cannot disagree.
- `NO_TODAY` (`model/Model.kt`) is for code that reads only a plan's lesson dates and never a page pool: `sanitiseYears` (`settleNextLesson`) and the version-1 plan reader.
- Nothing is rewritten: a new date only changes what is shown and which pages are banked.

## Tests

- `PacerTest.planForReadsTheGivenTodayNotTheClock`: the same class read the day before and on the day homework is due banks its pages only on the due day, whatever the clock says.
- `PacerViewModelTest.midnightMovesPlansBankingAndRevisionDatesTogether`: with a fake clock, one midnight moves the plan's date, `bankedPages`/`remainingPages` and the revision dates together, and `saveUnitRevision` agrees with them.
- Engine parity tests are unchanged. Tests that build a `Plan` pass `TEST_TODAY` (1 September 2026, before the sample term).

## Not covered

- A device run across the Europe/Athens daylight-saving change and around midnight with the debug build (a Compose test is entry 10).
