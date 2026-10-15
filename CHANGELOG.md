# Changelog

All notable changes to this project are documented in this file.

The format is based on [Keep a Changelog](https://keepachangelog.com/en/1.1.0/),
and this project adheres to [Semantic Versioning](https://semver.org/spec/v2.0.0.html).

Versioning for this app:

- **MAJOR**: a change that breaks existing saved plans or the import from the web page, or that changes scheduling results on purpose
- **MINOR**: a new feature that keeps existing plans working
- **PATCH**: bug fixes, performance or size improvements, and changes to documentation, licensing, build or CI that leave the app working as before

Every pull request bumps the version:

1. Choose MAJOR, MINOR or PATCH using the rules above.
2. Set `versionName` in `app/build.gradle.kts` to `X.Y.Z` (always three parts) and increase `versionCode` by one.
3. Add a `## [X.Y.Z] - YYYY-MM-DD` section below with that PR's changes, plus its compare link at the bottom.
4. After the merge, the `Release` workflow tags the merge commit `vX.Y.Z` and publishes a GitHub release with that section as notes and the APK attached. It only runs once CI passes.

Keep `[Unreleased]` empty between pull requests.

## [Unreleased]

## [1.5.0] - 2026-10-15

Record the pages a lesson actually covered when it didn't go to plan.

### Added

- Long-press a lesson and choose "Edit pages taught…" to type the pages it covered in each book, such as `5-8` or `5-8, 10`, or leave a book empty if you taught none of it. Each field starts with the lesson's planned pages (or the ones already recorded) and shows the plan underneath. Pages outside the book are refused.
- Saving marks the lesson done with those pages. Pages it missed go back into the schedule, normally into the next lesson, and pages taught beyond the plan come off later lessons. Each book's remaining pages re-spread over the lessons left, by the same rule as ticking.
- A lesson recorded with pages other than the plan's shows a small "edited" tag. Putting the planned pages back removes it.

### Changed

- The saved file is version 4: a done lesson can carry an `edited` flag. Version 1 to 3 files and the web page's export still load.
- Ticking lessons that went to plan works as before, and schedules with no edited lessons are unchanged.

## [1.4.2] - 2026-10-15

### Fixed

- Deleting a school year was hard to find: its button sat at the bottom of School year settings, below every day off. Each year on Home now has a ⋮ button (or long-press the year) with Rename and Delete, and the button in School year settings sits under the year's name and dates. Deleting still asks first, and Home offers Undo.

## [1.4.1] - 2026-10-15

### Fixed

- The app opts in to Android's predictive back. Back inside the app works as before; on Android 15 and later, the back gesture from Home previews the launcher before the app closes. The "OnBackInvokedCallback is not enabled" warning no longer appears in the log on every back.

## [1.4.0] - 2026-10-15

Lessons can be ticked as done, schedules can show one month at a time, and printing lets you choose classes and margins.

### Added

- Tick a lesson in the schedule once it's taught. Done lessons stay in the list, dimmed, with the pages they covered. Untick to put a lesson back.
- Long-press a lesson for "Done up to here" and "Not done from here".
- Ticking a lesson out of order leaves earlier lessons that aren't done in place, and the pages still to teach re-spread over the lessons left. Ticking in order gives exactly the schedule the web page's "Mark next lesson done" gives.
- Schedules have an All months / By month toggle. By month shows one tab per month: swipe or tap a tab to move. It opens on the current month, and the choice is remembered.
- Print or save PDF opens a dialog to choose which classes to print (all by default) and the margins: Narrow (8 mm), Normal (15 mm) or Wide (25 mm).

### Changed

- The year screen stacks its four buttons in portrait, full width, and puts them in one row in landscape.
- "Mark next lesson done" now ticks the next lesson, which stays visible as done. The summary shows the group's next lesson and how many are done.
- "Reset all positions" also clears done lessons.
- The saved file is version 3: each group stores its done lessons. Version 1 and 2 files and the web page's export still load.
- Copy, Share and Print cover the lessons still to come.

## [1.3.0] - 2026-10-10

The app is now called Course Pacer, and plans are organised as school years that hold classes. Existing plans move over on first launch and schedule exactly as before.

### Added

- School years. Each holds the term dates, the days off and its classes. Home lists them in a picker in the middle of the screen; "New school year" suggests the next year's name and dates and can add the Greek public holidays.
- Classes (previously groups were listed straight under a plan). A class is a set of books taught to one or more groups, with its own lesson length and sustainable pace. The Classes screen lists them with a status line each.
- Year screen with buttons for Classes, School year settings, Overview, and Print or save PDF.
- School year settings: name, start and end dates, days off and Greek holidays, and deleting the year. A day off can apply to every class, one class, or one group.
- Overview: every group in every class with its verdict, and the year's problems, each linked to the screen that fixes it.
- Deleting a school year offers Undo on Home.

### Changed

- Renamed the app from Coursebook Pacer to Course Pacer.
- Home shows the logo at the top, the school-year picker in the middle and a Settings button at the bottom right.
- The class screen holds what were the Term (lesson length and pace), Groups and Books screens, with links to Tests & worksheets and Homework.
- Print or save PDF covers every group in every class of the open year.
- Undo works within the open school year; opening another year starts a fresh history.
- Settings: "New plans" defaults are now "New classes" defaults.
- The saved file is version 2. Version-1 files and the web page's export still load: each plan becomes a school year with the same name, dates and days off, holding one class ("Main class") with its books, groups, tests and homework.

### Removed

- The Plans and Schedules screens, and the Home button grid. Their features moved to the screens above.

## [1.2.0] - 2026-10-10

Home is now a minimal launcher: the logo, the app's name, the open plan, and a button for each part of the app. Scheduling results and the saved file format don't change.

### Added

- Schedules screen with the group cards and plan problems that were on Home.
- Plans screen to switch, create, rename and delete plans. Rename and delete act on the open plan, and undo is in its bar.

### Changed

- Home shows the app's logo and name, the open plan's name, and ten buttons (Schedules, Plans, Term, Groups, Days off, Tests & worksheets, Books, Homework, Print or save PDF, Settings), each with a one-line summary. Schedules turns rust-coloured and shows the count when the plan has problems.
- Home has no top bar. The plan switcher and the overflow menu are gone; their actions moved to Plans, and Print or save PDF and Settings are buttons on Home.
- Switching plans keeps you on the screen you were on, closing only an open group. A group restored on launch reopens over Schedules, so back returns there.

## [1.1.0] - 2026-10-10

The app now opens on a home screen, each part of a plan has its own screen, and there is a settings screen. Scheduling results and the saved file format don't change.

### Added

- Home screen: one card per group with lessons and pages left, the pace needed, the verdict and the next lesson's pages; plan problems at the top, each linking to the screen that fixes it; links to every part of the plan and to Settings.
- Separate screens for Term, Groups, Days off, Tests & worksheets, Books and Homework, replacing the single Setup list. Each shows the plan problems it can fix.
- Settings screen: light, dark or system theme; lesson length and pace for new plans; app version. Settings are stored in SharedPreferences, outside undo, and included in backups.
- The system back button and a back arrow move up through screens. Each screen keeps its scroll position while it's on the back stack.
- On a group's screen, the title switches between groups. On Groups, each group links to its schedule.
- Unit tests for navigation and settings.
- `docs/UI_BRIEF.md`, the brief for this change.

### Changed

- The group tab row is gone. Groups open from Home, and the open group is still restored on launch through the existing `view` field.
- New plans take their lesson length and pace from Settings. The defaults match the sample plan (75 minutes, 4 pages), so nothing changes until you edit them.
- "Add Greek public holidays" is disabled until the term has both dates.

## [1.0.2] - 2026-10-10

### Added

- `Release` GitHub Actions workflow. When the Android workflow passes on `main`, it reads `versionName`, creates the `vX.Y.Z` tag and a GitHub release if they don't exist yet, uses this file's section for that version as the release notes, and attaches the release APK. It can also be run manually with a commit SHA to tag an older merge.

## [1.0.1] - 2026-10-10

Changes since the `v1.0.0` tag: documentation, licensing and the version format. The app itself doesn't change.

### Added

- `CHANGELOG.md` (this file).
- `CLAUDE.md` with build commands, architecture, rules and gotchas for AI coding agents.
- `LICENSE`: proprietary, all rights reserved, with Dimitrios Tsampallas as the copyright holder. Section 3 forbids AI systems from changing, copying, distributing or training on the code unless the owner has explicitly authorised them.
- README sections on features, tech stack, prerequisites, CI, project structure, development workflow and license, plus a CI status badge.
- `.gitignore` entries for `.kotlin/`, `.navigation/`, `*.apks`, `/app/release/`, logs, heap dumps, `Thumbs.db` and `.claude/settings.local.json`, grouped under comments.

### Changed

- `versionName` follows SemVer exactly: `1.0` became `1.0.0`, and is now `1.0.1` (`versionCode` 2).
- The README now says that CI runs on every push and on manual dispatch, as well as on pull requests.

## [1.0.0] - 2026-10-10

### Added

- Native Android app (Kotlin, Jetpack Compose, Material 3) ported from the Coursebook Pacer web page.
- Pacing engine in plain Kotlin that matches the web page's schedule row for row. It covers lessons per group from its weekdays minus days off, skipped and homework pages, even allocation so every book lands on the final lesson, finish-by dates, tests that take the whole lesson, and the pace verdict.
- Greek public holidays generated from Orthodox Easter.
- Plans you can create, rename, switch and delete, with up to 60 undo steps.
- Setup screen for the term, groups, days off, tests and worksheets, books, and homework, with a list of plan errors.
- Group screen with a summary, verdict, per-book progress, lessons per month, the lesson-by-lesson schedule, and "Mark next lesson done".
- Copy as tab-separated text, share, and print or save as PDF.
- Plans stored as JSON in app storage with atomic, debounced writes, a JSON format compatible with the web page, and backup and device-transfer rules.
- Unit tests for the engine.
- GitHub Actions workflow that runs the tests and builds the release APK on every push and pull request, and uploads the APK as a build artifact.

[Unreleased]: https://github.com/tsampolion/course_planner/compare/v1.5.0...HEAD
[1.5.0]: https://github.com/tsampolion/course_planner/compare/v1.4.2...v1.5.0
[1.4.2]: https://github.com/tsampolion/course_planner/compare/v1.4.1...v1.4.2
[1.4.1]: https://github.com/tsampolion/course_planner/compare/v1.4.0...v1.4.1
[1.4.0]: https://github.com/tsampolion/course_planner/compare/v1.3.0...v1.4.0
[1.3.0]: https://github.com/tsampolion/course_planner/compare/v1.2.0...v1.3.0
[1.2.0]: https://github.com/tsampolion/course_planner/compare/v1.1.0...v1.2.0
[1.1.0]: https://github.com/tsampolion/course_planner/compare/v1.0.2...v1.1.0
[1.0.2]: https://github.com/tsampolion/course_planner/compare/v1.0.1...v1.0.2
[1.0.1]: https://github.com/tsampolion/course_planner/compare/v1.0.0...v1.0.1
[1.0.0]: https://github.com/tsampolion/course_planner/releases/tag/v1.0.0
