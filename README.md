# Course Pacer for Android

[![Android](https://github.com/tsampolion/course_planner/actions/workflows/android.yml/badge.svg)](https://github.com/tsampolion/course_planner/actions/workflows/android.yml)

Documentation index: [docs/README.md](docs/README.md)

Course Pacer is part of the **EduSuite** apps, with [Course Invoicer](https://github.com/tsampolion/EduInvoiceApp). Both share one look, set out in [docs/EDUSUITE_STYLE_GUIDE.md](docs/EDUSUITE_STYLE_GUIDE.md); each has its own accent colour, teal for Course Pacer.

It started as an Android version of the Coursebook Pacer web page, for teachers who must get through coursebooks by the end of term. For each group, it counts the lessons left, removes skipped pages and homework pages whose due or set-on date has arrived, and splits the rest so every book lands on the final lesson.

Release history is in [CHANGELOG.md](CHANGELOG.md).

## Features

- **Two palettes**: Bone & green (default) and Saffron & ink, each in light and dark mode. Choose in Settings; the choice applies immediately and is remembered. Text and lesson edges have tested contrast.

- **Menu**: ☰ on every screen opens the menu: find a class or group by name, Home, Pacing overview with how many groups are behind, the year's classes, School year and Settings. System Back still steps back.
- **Home**: "Hello" with your name, today's date and the open school year. Under it, the next lesson across every class and group with a **Done** button, a card of your classes, and the **Pacing overview** board: one cell per group, where only groups that are behind are coloured, with how many lessons short. Tap a cell to open its group.
- **School year**: switch years with the chips at the top or add one, then the term on one line and the days off as dated rows. One tap adds the Greek public holidays, worked out from Orthodox Easter. Rename and delete the open year at the end.
- **Classes, books and groups**: Classes lists each class with its groups, books and status. A class has a lesson length, a pace, books and groups, and its screen lists the groups with their status, the books (lead and supplementary marked), and links to each group’s Work page. A book has its first and last page, skipped pages such as `8, 24-27`, and an optional finish-by date. A group has teaching weekdays and a next-lesson date.
- **Lead book**: pick a class's lead book and the other books keep step with it, a third of the way through it meaning a third of the way through them. Unit start pages keep books in step unit by unit. A book with its own finish-by date keeps its own pace.
- **Group**: a home with today’s lesson, Progress, Due soon and entry rows for Plan, Work and Stats. Plan opens on today's lesson, with **Done** and **Pages taught**, then the lessons to come in week sections ("This week", "Next week", then dates), one line each: the date, the books' initials with their pages, and a tick, with an edge colour for each kind of lesson. Earlier and done lessons sit behind one expandable count. Weeks or Months is remembered, with month arrows and a month picker. Days off and tests that take a whole lesson are slim rows. TalkBack reads each lesson as one item with every action from its long-press menu. Its ⋮ menu has Copy, Share, Print (starting with this group), Term weeks, Extra lesson and Tips, plus My plan actions to re-pace untouched lessons, adjust catch-up, copy or delete. Tap the group's name to switch to another group: search, recent groups and the class's groups, each with its status. Stats leads with the verdict in whole lessons ("Short by 2 lessons") and how to fix it, then figures for the selected My plan or Proposed source, including planned lessons, pace and completion versus term end. Long-press Pace needed for Own pace and a live preview (also in ⋮ at large text). A pace track per book has a tick where it would be in step with the lead book and an outlined proposed-position line when behind, lessons by month and the details; tap a book to change its position.
- **Extra lessons**: add a lesson on a day off or another date from the group menu or Term weeks. Its core-book pages stay pinned in both plans, while untouched custom lessons re-pace. Preview the shortfall, edit or delete the pin, and undo the whole change.
- **Group settings**: open the group's ⋮ menu or the class group editor to override Next lesson, finish earlier, add group days off or Greek holidays, skip additional pages, select optional books, or delete the group with Undo. Class defaults stay visible. Optional books are defined once in the class library and affect only groups selecting them.
- **Collections**: organise Classes into named, editable collections; each class belongs to at most one. Moving a class changes its collection, and deleting a collection keeps its classes. Sort within sections by Your order, Name or Most behind; the app remembers the choice.
- **Custom plans**: *Start from this schedule and make it yours* (Create) turns the proposed schedule into the group's own plan, kept beside it. Long-press a future lesson for *Change planned pages…* to set its pages and a note, add, remove or drag lessons, and re-pace from any lesson; lessons you change by hand are never rewritten. Pages left out, taught twice or past the term end show up as warnings, each with a one-tap fix, and *My plan | Proposed* switches back at any time, labelling the source in use. A custom plan only handles a shortfall when its pages fit inside the term; overflow keeps the warning and its fixes.
- **Supplementary books**: add a companion or reader with just the pages you'll use. It is never paced: place its pages by hand in the lessons you choose, on top of the lesson or counting in it, where you say what each core book teaches that day and the rest moves on.
- **Warnings with a way out**: a warning button with a count in the group's top bar opens every warning with its fix. A group behind its sustainable pace gets *Catch up*: pick +1, +2 or +3 pages a lesson, see how many lessons it takes and when you are back on pace, and apply it to your custom plan. Re-pacing keeps your chosen catch-up boost, and My plan → *Adjust catch-up…* changes it without switching plans. A book that falls more than 2 lessons behind the others (a class setting) gets *Bring in step*.
- **Done lessons**: tick a lesson once it is taught. Long-press for "Done up to here" or "Not done from here". Ticking out of order re-spreads the pages still to teach.
- **Pages taught**: tap *Pages taught* on today's lesson, or long-press a lesson dated today or earlier and choose "Record what I taught…", to record the pages it really covered. Missed pages go back into the schedule and the rest re-spread.
- **Assignments**: each group owns its Homework, Tests and Worksheets on Work. Filter by type, add or edit dates, pages and notes, and remove through swipe or ⋮ with Undo. Tests and worksheets can take a lesson or run alongside it. Lesson menus and TalkBack offer Add/Edit/Delete homework, with wrapping HW details on lesson rows. Worksheet/test homework can optionally be scheduled in class, and class work can optionally be set as homework. Homework leaves the classroom sequence on its due date, falling back to set-on; undated work keeps immediate banking. Older class-wide tests copy into each group without changing the schedule.
- **Pacing overview**: every group class by class, one line each with its next lesson and status, sorted inside each class by most behind, name or next lesson. The year's problems are behind the warning button, each with its fix.
- **Output**: from a group's ⋮ menu, copy its schedule as tab-separated text, share it, or print or save it as a PDF (that group, its class, or the whole year).
- **Large text and tablets**: from font scale 1.5 the screens show less, with the rest one tap away (groups on pace, more stats), and buttons go full width under their text. On a tablet, Pacing overview and the open group sit side by side, and from 840dp the menu stays open.
- **Undo**: up to 60 steps within the open school year. Notifications show a countdown from when they appear; repeating a fix that changes nothing says "Already up to date".
- **Settings**: your name for Home's greeting, theme, defaults for new classes, "Show tips again", and back up and restore. Export saves every school year to a file you choose. Import reads that file, or the web page's export, and adds its years beside yours or replaces them.

School years and settings are saved on the device and included in Android backup and device-to-device transfer. Uninstalling deletes them, so export first.

## Getting started

### Prerequisites

- JDK 17 or newer
- Android SDK with platform API 35

Android Studio provides both. From the command line, point `ANDROID_HOME` (or `sdk.dir` in `local.properties`) at your SDK.

### Build and run

```sh
git clone https://github.com/tsampolion/course_planner.git
cd course_planner

./gradlew testDebugUnitTest   # pacing engine unit tests
./gradlew installDebug        # install on a connected phone or emulator
./gradlew assembleRelease     # minified APK in app/build/outputs/apk/release/
./gradlew lint                # Android lint (report in app/build/reports/)
```

A local `assembleRelease` is signed with your debug key, so you can install it directly. Releases from CI are signed with the permanent release key, so a local build can't install over a release, and a release can't install over it. `installDebug` installs a separate app, "Course Pacer (debug)" (`gr.edusuite.pacer.debug`), with its own data.

## CI and releases

[`.github/workflows/android.yml`](.github/workflows/android.yml) runs on every push, pull request and manual dispatch. It runs the tests, builds the release APK and checks its signing certificate. When it passes on `main`, [`.github/workflows/release.yml`](.github/workflows/release.yml) tags `vX.Y.Z` from `versionName` and publishes a GitHub release with that version's `CHANGELOG.md` section as the notes. Only `main` signs with the release key. Other branches use the debug key and never publish. How release signing works: [docs/SIGNING_BRIEF.md](docs/SIGNING_BRIEF.md).

| Name | Kind | Holds |
| --- | --- | --- |
| `SIGNING_KEYSTORE_B64` | `release` environment secret | the release keystore, base64-encoded |
| `SIGNING_PASSWORD` | `release` environment secret | its password |
| `SIGNING_CERT_SHA256` | variable | the certificate's SHA-256 fingerprint, lowercase without colons |

## Project structure

```text
.
├── app/
│   ├── build.gradle.kts              # SDK levels, R8, signing, dependencies
│   └── src/
│       ├── main/java/gr/edusuite/pacer/
│       │   ├── MainActivity.kt       # hosts Compose, flushes saves in onStop
│       │   ├── model/                # SchoolYear, SchoolClass, Group, Book, Closure, Homework, TestEvent, Plan, Settings
│       │   ├── engine/               # pacing rules, Greek holidays, formatting (plain Kotlin)
│       │   ├── data/                 # org.json codec, migration of older files, atomic file storage, settings
│       │   └── ui/                   # PacerViewModel, navigation, Compose screens, print to PDF, theme
│       ├── main/res/                 # strings, theme, font, launcher icon, backup rules
│       └── test/java/.../            # unit tests
├── docs/                             # briefs, style guide and decision records; start at docs/README.md
├── gradle/libs.versions.toml         # version catalog
├── .github/workflows/android.yml     # CI: tests and release APK
└── .github/workflows/release.yml     # tags vX.Y.Z and publishes the release after CI passes on main
```

## Tech stack

| | |
| --- | --- |
| Language | Kotlin 2.1.21, JVM target 17 |
| UI | Jetpack Compose (BOM 2025.05.01), Material 3, no XML layouts |
| Build | Gradle 8.14.3 (wrapper), Android Gradle Plugin 8.10.1, Kotlin DSL, version catalog in `gradle/libs.versions.toml` |
| SDK | `minSdk 26`, `targetSdk 35`, `compileSdk 35` |
| Libraries | `activity-compose`, `lifecycle-viewmodel-ktx`, and JUnit 4 for tests. Nothing else. |
| Font | [Outfit](https://github.com/Outfitio/Outfit-Fonts) (SIL Open Font License 1.1, [`third_party/outfit/OFL.txt`](third_party/outfit/OFL.txt)), bundled as one variable font |
| Icons | Material Symbols Rounded, outlined, Copyright Google, Apache 2.0; path data in `ui/AppIcons.kt`, attribution in [`third_party/material-symbols`](third_party/material-symbols/) |

## Footprint

- Only the dependencies listed above: no DI framework, serialization library or icon pack.
- `minSdk 26`, so `java.time` works without desugaring. R8 shrinks code and resources, and only English resources ship.
- PDF output uses the system print dialog via `WebView`, so no PDF library is bundled.

## Development workflow

1. Branch off `main`.
2. Keep the engine in plain Kotlin. Every rule change needs a test in `PacerTest`.
3. Run `./gradlew testDebugUnitTest assembleRelease` before you open a pull request. CI runs the same commands.
4. In every pull request, bump `versionName` in `app/build.gradle.kts` following [Semantic Versioning](https://semver.org/), and add a matching section to [CHANGELOG.md](CHANGELOG.md). The release is automatic after the merge.
5. Update every doc your change makes out of date (this README, CLAUDE.md, docs/README.md, a brief's banner, a PLANNED_FEATURES.md status), and delete any doc it makes obsolete, in the same pull request.
6. New dependencies need approval first (see the brief).

## License

Proprietary. Copyright © 2026 Dimitrios Tsampallas. All rights reserved. See [LICENSE](LICENSE).

No one may use, copy, modify or distribute this app or its code without the owner's written permission. AI systems (assistants, agents, bots and automated tools) may not change, copy or train on the code unless the owner has explicitly authorised them for that work.
