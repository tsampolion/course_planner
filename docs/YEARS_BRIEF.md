# Task: Rename to Course Pacer and organise the app as School years → Classes → Groups

This is the improved version of the request "change the app's name to Course Pacer; the home screen shows the logo, settings and a school-year picker; a year leads to Classes (new naming for Groups) and School year settings; a class holds its books, its groups and their dates". The answers to the clarifying questions are folded in. Version 1.3.0 was built from it.

## Context

Read `CLAUDE.md` and `docs/BRIEF.md` first; their rules are binding. Before this change `AppData` held a flat list of `Plan`s, each with its own term, days off, books, groups, homework and tests, and Home was a grid of ten buttons. Teachers think in school years: one calendar per year, and several classes each taught from their own coursebooks.

## What to build

1. **Rename.** The user-visible name becomes "Course Pacer" (`app_name`, Home, the logo's description, About, the release title, README, BRIEF, CLAUDE.md). Keep the package and `applicationId` `com.coursebook.pacer`; changing them would install a second app and strand saved plans.
2. **Data model.**
   - `SchoolYear { id, name, start, end, closures, classes }`: the calendar every class shares.
   - `SchoolClass { id, name, mins, pace, groups, books, homework, events }`: what a plan was, minus the term and days off. Lesson length and pace are **per class**.
   - `Group` is unchanged. Groups in a class share its books and keep their own days, next lesson and positions.
   - `AppData { years, activeYear, activeClass, view }`.
   - A day off applies to every class (`ALL_GROUPS`), one class (its id) or one group (its id).
   - **Engine untouched.** `SchoolYear.planFor(class)` builds the `Plan` the engine already takes.
3. **Persistence.** `JsonCodec.VERSION` becomes 2. Version-1 files and the web page's export still load: **each plan becomes its own school year** with one class, so nothing is merged or lost. Test that migrated plans schedule row for row as before.
4. **Screens.**
   - **Home**: logo top-centre with the app name; a school-year picker in the middle (tap a year to open it, or "New school year" with a suggested name and dates and an option to add Greek holidays); ⚙ Settings bottom-right.
   - **Year**: buttons for Classes, School year settings, **Overview** and **Print or save PDF**.
   - **School year settings**: name, dates, days off, Greek holidays, delete the year.
   - **Classes**: list with a status line each, plus "New class".
   - **Class**: name, lesson length and pace, books, groups (weekdays, next lesson, open schedule, reset positions), links to Tests & worksheets and Homework, delete the class.
   - **Overview**: every group of every class with its verdict, and the year's problems linked to their fixes.
   - **Group**: the existing group screen, with copy, share and "Mark next lesson done".
   - **Settings**: theme, new-class defaults, About.

## Constraints

- No new dependencies. Edits go through the `edit*` helpers; undo is reachable from every screen inside a year; `onStop → flush()` still saves.
- The back stack stays in plain Kotlin in `Nav.kt`, with unit tests.
- Delete code that has no use any more (Plans, Schedules, the Home grid).
- Every parity item in `BRIEF.md` stays reachable.

## Done when

- `testDebugUnitTest` and `assembleRelease` pass; new non-UI logic (navigation, `planFor`, migration, year suggestion) has unit tests.
- The version goes to 1.3.0 (MINOR: existing plans keep working), `versionCode` 6, with a changelog entry and compare link.

## Where the parity items live now

| Parity item | Screen |
| --- | --- |
| Plans: switch, rename, create, delete, undo | Home (switch, create), School year settings (rename, delete), Undo in every bar inside a year and on Home after a delete |
| Term: start, end | School year settings |
| Lesson length, sustainable pace | Class |
| Groups: name, weekdays, next lesson, add, remove, reset positions | Class |
| Days off: list, add, remove, Greek holidays | School year settings |
| Tests and worksheets | Class → Tests & worksheets |
| Books | Class |
| Homework already assigned | Class → Homework |
| Error list | Overview (whole year), Class and School year settings (their own) |
| Group tab: summary, verdict, positions, mark done, months, schedule | Group (from Class or Overview) |
| Copy, share | Group |
| Print or save PDF | Year |
