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

## [2.2.0] - 2026-10-17

The EduSuite look, part two: a new Home that shows the open school year at a glance, and deletes that happen at once with Undo instead of asking first. Plans and schedules are unchanged.

### Changed

- **Home is the open school year's dashboard.** At the top: "Hello", today's date and Settings. Under it, a chip with the year's name: tap it to switch to another year, add one, or rename or delete one.
- **Next lesson on Home.** A teal tile shows the next lesson across every class and group: its date, class and group, and the pages for each book (or the test that takes it), marked "Overdue" when its date has passed. **Mark done** ticks it there and then, with Undo, by the same rule as ticking it in the schedule. Tapping the tile opens that group.
- **Tiles for the rest of the year:** Classes, Overview, Tests, Homework, Days off & dates, and Print or save PDF, each with a live figure ("3 classes · 7 groups", "Next: Mon 19 Oct · Unit 1 test", "2 problems"). A section with nothing in it yet shows its tile faded, and it still opens. At very large text sizes or on narrow screens the tiles stack in one column. The school year screen they replace is gone.
- **Home always shows a year when there is one**: after an import, or when the open year is deleted, it shows the one that starts last. Undoing the delete opens the deleted year again.
- **Deletes happen at once with Undo.** Deleting a school year, a class, a group or a book, and resetting every group's positions, no longer ask first: a message offers Undo for 5 seconds (longer if the phone's accessibility settings ask for more time). Import's Replace all still asks first.
- **Swipe to remove.** Days off, tests and homework rows can be swiped away (from the right), with Undo. A long press and TalkBack's actions offer the same, and the × stays.
- **Short adds open from the bottom of the screen**: a new class, a book, and a day off, whose To date and "Applies to" sit under More options.

## [2.1.0] - 2026-10-17

The EduSuite look, part one: Course Pacer's colours, type and shapes now follow the EduSuite style guide, the same base as Course Invoicer's, with teal as Course Pacer's own colour. The screens keep their layout; Home and the other screens are redesigned in the next releases. Plans and schedules are unchanged.

### Changed

- **Colours**: a soft lavender-grey paper, white panels, teal for buttons and highlights, rust for warnings and moss green for "fits", in light and dark. The year screen's buttons are now pastel tiles: sky for Classes, lilac for School year settings, mint for Overview (it turns to a warning tint when there are problems).
- **Wallpaper colours are off** on Android 12 and later too (they came in with 1.8.0). Every phone shows the same colours, and teal tells Course Pacer apart from Course Invoicer.
- **Type**: the Outfit typeface for all text, with page numbers, dates and counts still in a monospace font so they line up. Screen titles are 22sp.
- **Shapes**: panels and list groups have 16dp corners and no outline, tiles 24dp, dialogs 28dp; buttons, chips and tags are pills, and "Add" buttons are full-width pill buttons. Labels above groups and tags are in sentence case instead of capitals.
- All text keeps at least 4.5:1 contrast with its background (WCAG AA), and field outlines 3:1, in both themes. A unit test checks every pair the style guide lists.

### Added

- The Outfit typeface (SIL Open Font License 1.1), bundled as one 110 KB variable font. Its licence is in `third_party/outfit/OFL.txt` and is named in `LICENSE`.

## [2.0.0] - 2026-10-17

Course Pacer joins the **EduSuite** apps, with Course Invoicer. This release changes the app's id, so it installs as a new app beside the old one: move your school years over with Export and Import (below). The schedules, the saved file's format and the rest of the app are unchanged; the new EduSuite look follows in the next releases.

### Changed

- **New app id**, `gr.edusuite.pacer` (it was `com.coursebook.pacer`). Android treats it as a different app, so 2.0.0 installs beside the old Course Pacer instead of over it, and starts empty. To move your data:
  1. In the old Course Pacer, open Settings → Export… and save the file.
  2. Install this version.
  3. Open Settings → Import…, pick that file and choose Add. The welcome guide shows first, because the new app has no school years yet.
  4. Once your years are there, uninstall the old Course Pacer.

  The theme and the defaults for new classes aren't in the export, so set them again in Settings. The debug build is now `gr.edusuite.pacer.debug`. The release signing key is the same.
- **New launcher icon**, the EduSuite open book: Course Invoicer's pages with three pacing lines cut out of the right page, on a teal tile in both light and dark mode. Android 13 and later can tint it to the wallpaper with themed icons, and launchers that ask for a round icon get one. The logo on Home uses the same tile.
- The release download is now `course-pacer-X.Y.Z.apk`, and the CI artifact is `course-pacer-apk`. The CI summary also shows the APK's package name and version.

### Added

- **EduSuite style guide** (`docs/EDUSUITE_STYLE_GUIDE.md`), a copy of the canonical guide shared with Course Invoicer, and Course Pacer's accent contrast ratios (`docs/PACER_ACCENT_CONTRAST.md`). The README links Course Invoicer.

## [1.8.0] - 2026-10-16

A new look: the app now uses EduInvoiceApp's colours, so the two apps look alike. Plans, schedules and settings are unchanged.

### Changed

- On Android 12 and later, the colours come from the wallpaper, as in EduInvoiceApp, including the screen shown while the app opens.
- On older phones, the palette is EduInvoiceApp's navy blue, slate and green, with Material's red for warnings, in place of the web page's teal, sage and rust, in both the light and dark themes. EduInvoiceApp leaves some colours to Material's defaults, which are purple and pink; Course Pacer fills those from the same blue and green instead, and sets every colour itself, so a library update can't change them.
- On every phone, warnings stay red and "Fits" stays green whatever the wallpaper, and the launcher icon and the logo on Home are EduInvoiceApp's blue. Android 13 and later can still tint the icon to the wallpaper with themed icons.
- All text keeps at least 4.5:1 contrast with its background (WCAG AA), in both themes, with the app's own colours or the wallpaper's. "Fits" uses a slightly darker green than EduInvoiceApp's so it stays readable on panels. Where a wallpaper's tinted background would make tags, the next-step card and days off hard to read, they use a neutral one.

## [1.7.3] - 2026-10-16

CI only. The app is the same as 1.7.2.

### Changed

- The release signing secrets now sit in a GitHub Environment, `release`, that only `main` may use. Builds of other branches and pull requests no longer get them, so a branch's own build files can't reach the signing key; they're signed with the debug key and never publish, as pull requests from forks already were.
- The Android workflow's token is read-only (`permissions: contents: read`).

## [1.7.2] - 2026-10-16

The smaller fixes from the 1.6.0 audit.

### Fixed

- One damaged record, such as a group or book without an id, no longer makes the whole saved file, or an imported one, unreadable. The record gets a new id, or is left out if it can't be used (homework that names no group or book), and everything else loads.
- The **Undo** on Home's and Settings' snackbar now only undoes its own change: the year you deleted or the import you made. If you make another change first, or open a school year, the snackbar goes away instead of undoing something else or nothing.
- Importing a deliberately malformed file, deeply nested past what the reader handles, now says it can't be read instead of closing the app.
- A plan that fits exactly no longer says "Short by 0.00 lessons… 0 fewer pages", a rounding slip in pages-per-lesson arithmetic (21 pages at 1.4 a lesson came out as 15.000000000000002 lessons).
- Running the Release workflow by hand fetches the APK from that commit's Android run. If there isn't one, it stops instead of publishing a release without its APK, which a later run couldn't fix because the tag would exist. Tick **allow_no_apk** to publish without one on purpose.

## [1.7.1] - 2026-10-15

Fixes from a full audit of 1.6.0: crashes and freezes from unusual numbers, number fields that saved the wrong value, one-tap deletions, and past lessons coming back after a calendar change.

### Fixed

- A page number typed by mistake, such as 16000000, or one in an imported file, no longer freezes or crashes the app. Pages go up to 100,000; the page fields don't accept more, and files are capped when read.
- A pace of "Infinity" or "1e400", typed or imported, no longer crashes the app when it saves. The pace fields only accept a real number, and a save that fails is logged instead of closing the app.
- Editing a number could save a different one: changing a pace of 1.5 to 1.25 saved 12, because deleting the "5" reset the field to "1". Number fields now keep what you type, and fields with a lowest or highest value (a book's resume page, the lesson length) wait until the number is in range.
- "Reset all positions", and the × on a group or a book, now ask first. They clear ticked lessons and the pages recorded for them.
- Removing a group also removes the tests aimed only at it.
- If the saved school years can't be read, the app now says so the first time it opens, instead of silently starting empty. **Save a copy…** puts the unreadable file somewhere you can reach, such as Drive. An earlier unreadable file is never overwritten.
- Lessons ticked in order stay in the past. Adding a teaching day or removing a day off used to bring back earlier dates as lessons still to teach, with pages on them. Ticking now moves the group's next lesson past the lessons taught, as the web page did; unticking moves it back. Groups saved by 1.4.0 to 1.7.0 are fixed when the app opens. The class screen's "Next lesson" shows the first lesson still to teach.
- A file with repeated ids, or a lesson ticked twice on one date, no longer crashes the screens that list them. Repeats get new ids when the file is read, and **Replace all** no longer keeps two years with one id.

### Changed

- The build uses `androidResources.localeFilters` in place of the deprecated `resourceConfigurations`. The APK still ships English only.

## [1.7.0] - 2026-10-15

A fresh install now starts empty, with a short guide, instead of a made-up sample year.

### Changed

- **No more sample year.** A new install opens on an empty Home screen instead of a ready-made 2026–27 year with a "Sample class", so there's nothing fake to find and delete. School years already on a phone are untouched.
- If the saved file can't be read, the app also starts empty (the unreadable file is still kept as `plans.json.bad`) instead of loading the sample year.

### Added

- **Welcome guide.** The first launch shows what the app does and the four steps to a first schedule: school year, class, books and groups, then the schedule. "Create a school year" starts the first step. Settings → Help → **Getting started** shows it again.
- **"Next step" cards** on empty screens say what to do next: create a school year on Home, add a class on the year and Classes screens, add books (or a group) on the class screen, and add books before a group's schedule can show pages. Each card goes away once its step is done.
- **A note on every group's schedule** saying the schedule is plain arithmetic (each book's pages spread evenly over the lessons left), that it can't know how the class is really doing, and that teachers should go by what they actually teach and record it. It also appears under each group's heading on the printed sheet (Print or save PDF), but not in Copy or Share.

## [1.6.0] - 2026-10-15

Back up your school years to a file and bring them back. Uninstalling the app deletes its school years, so export before you uninstall or change phones.

### Added

- Settings has a "Back up and restore" section. **Export…** saves every school year to a file you choose, such as Downloads or Google Drive, named like `course-pacer-2026-10-15.json`. Settings aren't included.
- **Import…** reads that file on this phone or another, and also reads the Coursebook Pacer web page's export. It shows how many school years the file holds and asks:
  - **Add** puts them beside yours. A year whose name is already taken gets "(imported)" after it.
  - **Replace all** keeps only the file's years.
  - Either way, the snackbar offers Undo.
- Import refuses a file it can't read, one with no school years, and one from a newer version of the app, and says why. Nothing changes on the phone.
- `docs/BACKUP_BRIEF.md` records the decisions.

## [1.5.3] - 2026-10-15

**The first release signed with the permanent key. Installing it needs one last uninstall, which deletes the school years on that phone. Every release after it installs as an update and keeps them.** Before you uninstall, note each year's dates, classes, books and positions so you can type them in again. 1.5.2 below describes the change; it was never published.

### Fixed

- CI read no certificate from the release APK, so 1.5.2 stopped at the certificate check and wasn't published. The check took the newest `apksigner` on GitHub's runners, which prints certificates in another format. Both workflows now use build-tools 35.0.0, the version the build uses, and accept either format. If `apksigner` prints no certificate, the check now fails on every branch and shows its output, instead of passing a debug-signed build without one.

## [1.5.2] - 2026-10-15 (not published)

**Installing this release needs one last uninstall, which deletes the school years on that phone. Every release after it installs as an update and keeps them.** Before you uninstall, note each year's dates, classes, books and positions so you can type them in again.

### Changed

- Releases are signed with one permanent key. Until now each release had a different signing certificate, so Android refused to install it over the one before ("App not installed"), and the only way past that was to uninstall. From this release on, each new release installs as an update.
- CI fails on `main` if the key is missing or the APK's certificate isn't the release certificate, so no release can go out signed with another key. The release workflow checks the certificate again and names it in the release notes, so you can check any APK against it.
- Debug builds from Android Studio install as a separate app, "Course Pacer (debug)", with their own data. They no longer clash with an installed release.

### Added

- `docs/SIGNING_BRIEF.md`: the decisions for the release key, and who does each step.

## [1.5.1] - 2026-10-15

### Added

- `docs/PLANNED_FEATURES.md` gains a second entry: signing every release with one permanent key, so each new release installs over the last and keeps your school years. Today every release is signed with a different key, so updating means uninstalling, which deletes the saved years. The entry sets out the process step by step, from creating and backing up the key on the owner's computer to the checks before and after the first signed release.

## [1.5.0] - 2026-10-15

Record the pages a lesson actually covered when it didn't go to plan.

### Added

- Long-press a lesson and choose "Edit pages taught…" to type the pages it covered in each book, such as `5-8` or `5-8, 10`, or leave a book empty if you taught none of it. Each field starts with the lesson's planned pages (or the ones already recorded) and shows the plan underneath. Pages outside the book are refused.
- Saving marks the lesson done with those pages. Pages it missed go back into the schedule, normally into the next lesson, and pages taught beyond the plan come off later lessons. Each book's remaining pages re-spread over the lessons left, by the same rule as ticking.
- A lesson recorded with pages other than the plan's shows a small "edited" tag. Putting the planned pages back removes it.
- `docs/PLANNED_FEATURES.md` describes features planned for later. The first is focus blocks: teaching one book on its own for a run of lessons (such as three grammar units), then going back to all the books.

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

[Unreleased]: https://github.com/tsampolion/course_planner/compare/v2.2.0...HEAD
[2.2.0]: https://github.com/tsampolion/course_planner/compare/v2.1.0...v2.2.0
[2.1.0]: https://github.com/tsampolion/course_planner/compare/v2.0.0...v2.1.0
[2.0.0]: https://github.com/tsampolion/course_planner/compare/v1.8.0...v2.0.0
[1.8.0]: https://github.com/tsampolion/course_planner/compare/v1.7.3...v1.8.0
[1.7.3]: https://github.com/tsampolion/course_planner/compare/v1.7.2...v1.7.3
[1.7.2]: https://github.com/tsampolion/course_planner/compare/v1.7.1...v1.7.2
[1.7.1]: https://github.com/tsampolion/course_planner/compare/v1.7.0...v1.7.1
[1.7.0]: https://github.com/tsampolion/course_planner/compare/v1.6.0...v1.7.0
[1.6.0]: https://github.com/tsampolion/course_planner/compare/v1.5.3...v1.6.0
[1.5.3]: https://github.com/tsampolion/course_planner/compare/v1.5.1...v1.5.3
[1.5.2]: https://github.com/tsampolion/course_planner/pull/12
[1.5.1]: https://github.com/tsampolion/course_planner/compare/v1.5.0...v1.5.1
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
