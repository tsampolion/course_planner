# Brief: Course Pacer for Android

This is the improved version of the original request ("turn the Coursebook Pacer page into a lightweight, fast Android app"). The app in this repository was built from it. The app is now called Course Pacer; since 1.3.0 plans are organised as school years and classes (see `docs/YEARS_BRIEF.md`).

## Goal

Port the Coursebook Pacer web page (a single HTML file with inline JavaScript) to a native Android app. The app must do everything the page does, with exactly the same scheduling results. It must start fast and keep a small APK.

## Source of truth

The pacing rules live in the page's JavaScript. Port them to plain Kotlin and give them parity tests: the same plan must produce the same lesson-by-lesson schedule, row for row.

- Lessons are each group's weekdays from its next lesson to the term end, minus days off. A day off can apply to all groups or to one group.
- Pages left in a book are the pages from the group's resume position to the last page, minus skipped pages (all groups) and pages set as homework (one group).
- Each book is paced on its own. Lesson *i* ends at `round(i × pages / lessons)`, so every book lands on the final lesson.
- A test that takes the whole lesson removes that lesson from teaching. A test that runs alongside the lesson is only labelled.
- A book with a finish-by date is paced over the lessons up to that date. If no lesson is left before it, all its pages land in the next lesson, and a warning is shown.
- The verdict compares the required pace with the pace the teacher says they can sustain, and reports lessons to spare or the shortfall.
- Greek public holidays are generated from Orthodox Easter for every year the term touches.

## Feature parity checklist

- Plans (now school years, each holding classes): switch, rename, create, delete (with confirmation), undo (60 steps; edits to one field within 1.2 s count as one step).
- Term (the school year's dates; lesson length and pace per class): start, end, lesson length, sustainable pace.
- Groups: name, teaching weekdays, next-lesson date, add, remove, reset all positions.
- Days off: list, add (reason, from, to, applies to), remove, add Greek holidays.
- Tests and worksheets: list (warn when the date is not a lesson day), add, remove.
- Books: title, first and last page, skipped pages, finish-by date, count of teachable pages, add, remove.
- Homework already assigned: list, add, remove.
- Error list: missing term dates, a term that ends before it starts, groups with no days, finished groups, books with bad page ranges or finish-by dates that do nothing or leave no lesson.
- Group tab: summary card, verdict, per-book resume position, pages left and pace, "Mark next lesson done" (since 1.4.0 also per-lesson ticks), lessons per month, and the lesson-by-lesson schedule grouped by month.
- Output: copy the schedule as tab-separated text, share it, and print or save every group's schedule as PDF.

## Technical rules

- Kotlin 2.x, Jetpack Compose with Material 3, no XML layouts.
- State: one `ViewModel` exposing a `StateFlow`; Compose reads it with `collectAsState`.
- Gradle Kotlin DSL with a version catalog.
- No dependency injection framework. The `Application` creates the store, and the `ViewModel` gets it through its constructor and a small factory.
- Approved dependencies only: Compose BOM + Material 3, `activity-compose`, `lifecycle-viewmodel-ktx`, and JUnit for tests. Anything else needs approval first.
- Use the platform where it can do the job: `org.json` for storage, `AtomicFile` for safe writes, `WebView` + `PrintManager` for PDF, the system clipboard and share sheet.
- Size and speed: `minSdk 26` (so `java.time` needs no desugaring), R8 minification and resource shrinking, English resources only, framework theme rather than AppCompat, no icon library.
- Data: save to one JSON file in app storage, debounced, plus a final save when the app goes to the background. Read the page's localStorage format too, so existing plans can be imported later.

## Definition of done

- The engine's unit tests pass, and its output matches the web page's for the same plan.
- `./gradlew testDebugUnitTest assembleRelease` succeeds in CI and the release APK is uploaded as a build artifact.
- The README explains how to build, install and run the app.
