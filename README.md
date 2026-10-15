# Course Pacer for Android

[![Android](https://github.com/tsampolion/course_planner/actions/workflows/android.yml/badge.svg)](https://github.com/tsampolion/course_planner/actions/workflows/android.yml)

A native Android version of the Coursebook Pacer web page, for teachers who have to get through one or more coursebooks by the end of term. For each group, it counts the lessons left, removes pages that are skipped or already set for homework, and splits the rest so every book lands on the final lesson.

The full brief is in [docs/BRIEF.md](docs/BRIEF.md). Release history is in [CHANGELOG.md](CHANGELOG.md), and features specified for later are in [docs/PLANNED_FEATURES.md](docs/PLANNED_FEATURES.md).

## Features

- **Home**: the logo, a picker for the school year, and Settings. Tap a year to open it, or add a new one (its name and dates are suggested, and the Greek public holidays can come as days off). Each year's ⋮ button (or a long-press) renames or deletes it.
- **School year**: buttons for Classes, School year settings, Overview, and Print or save PDF, each with a one-line summary. They stack in portrait and sit in one row in landscape.
- **School year settings**: the year's name, start and end dates, and a Delete button (it asks first, and Home offers undo); then days off for every class, one class or one group. One tap adds the Greek public holidays, which are worked out from Orthodox Easter.
- **Classes**: a class is a set of books taught to one or more groups. Add, open, rename or delete classes.
- **Class**: lesson length and the pace in pages per lesson you can sustain; books (first and last page, skipped pages such as `8, 24-27`, and an optional finish-by date that re-paces only that book); groups (name, teaching weekdays, next-lesson date, and a reset for every group's position); and links to tests and homework.
- **Tests and worksheets**: a test can take the whole lesson, which moves the pages forward, or run alongside it. The app warns when a test date isn't a lesson day.
- **Homework already set**: those pages drop out of the lessons for that group.
- **Overview**: every group in every class at a glance, with lessons and pages left, the pace needed, the verdict and the next lesson. The year's problems sit at the top and each links to the screen that fixes it.
- **Group view**: a summary, a verdict (lessons to spare or the shortfall compared with your sustainable pace), and for each book the resume position, pages left and pace. Also lessons per month, and the lesson-by-lesson schedule, either all months in one list or one month per tab (swipe between them).
- **Done lessons**: tick a lesson once it's taught; it stays in the schedule, dimmed, with its pages. Long-press for "Done up to here" or "Not done from here". **Mark next lesson done** ticks the next one. Ticking out of order re-spreads the pages still to teach.
- **Pages taught**: when a lesson didn't go to plan, long-press it and choose "Edit pages taught…" to type the pages it covered in each book (`5-8`, or nothing). The lesson is marked done with a small "edited" tag; pages it missed go back into the schedule, extra pages come off later lessons, and the rest re-spread.
- **Output**: copy a group's remaining schedule as tab-separated text, share it, or print or save schedules as a PDF, choosing the classes and the margins.
- **Error list**: missing or reversed term dates, classes with no groups or books, groups with no days, bad page ranges, and finish-by dates that do nothing or leave no lesson.
- **Undo**: up to 60 steps within the open school year, from every screen inside it.
- **Settings**: light, dark or system theme, the lesson length and pace that new classes start with, and the app version.

School years and settings are saved on the device and included in Android backup and device-to-device transfer.

## Tech stack

| | |
| --- | --- |
| Language | Kotlin 2.1.21, JVM target 17 |
| UI | Jetpack Compose (BOM 2025.05.01), Material 3, no XML layouts |
| Build | Gradle 8.14.3 (wrapper), Android Gradle Plugin 8.10.1, Kotlin DSL, version catalog in `gradle/libs.versions.toml` |
| SDK | `minSdk 26`, `targetSdk 35`, `compileSdk 35` |
| Libraries | `activity-compose`, `lifecycle-viewmodel-ktx`, and JUnit 4 for tests. Nothing else. |

## Getting started

### Prerequisites

- JDK 17 or newer
- Android SDK with platform API 35

Android Studio provides both. If you build from the command line, point `ANDROID_HOME` (or `sdk.dir` in `local.properties`) at your SDK.

### Build and run

```sh
git clone https://github.com/tsampolion/course_planner.git
cd course_planner

./gradlew testDebugUnitTest   # pacing engine unit tests
./gradlew installDebug        # install on a connected phone or emulator
./gradlew assembleRelease     # minified APK in app/build/outputs/apk/release/
./gradlew lint                # Android lint (report in app/build/reports/)
```

To build in Android Studio, open the project folder and run the `app` configuration.

The release build is signed with the debug key, so you can install it directly. Add your own signing config in `app/build.gradle.kts` before you publish to Play.

## Continuous integration

[`.github/workflows/android.yml`](.github/workflows/android.yml) runs on every push, every pull request and on manual dispatch. It sets up JDK 17, runs `testDebugUnitTest`, builds `assembleRelease`, writes the APK size to the job summary, and uploads the APK as the `coursebook-pacer-apk` artifact.

When that workflow passes on `main`, [`.github/workflows/release.yml`](.github/workflows/release.yml) reads `versionName` from `app/build.gradle.kts`. If the tag `vX.Y.Z` doesn't exist yet, it creates the tag and a GitHub release, with that version's `CHANGELOG.md` section as the notes and the APK attached. To tag an older merge, run it manually from the Actions tab with the commit SHA.

## Project structure

```text
.
├── app/
│   ├── build.gradle.kts              # SDK levels, R8, signing, dependencies
│   ├── proguard-rules.pro            # empty on purpose: R8 defaults are enough
│   └── src/
│       ├── main/java/com/coursebook/pacer/
│       │   ├── MainActivity.kt       # hosts Compose, flushes saves in onStop
│       │   ├── PacerApplication.kt   # composition root: creates the PlanStore and SettingsStore
│       │   ├── model/                # SchoolYear, SchoolClass, Group, Book, Closure, Homework, TestEvent, Plan, Settings; sample year
│       │   ├── engine/               # pacing rules, Greek holidays, formatting (plain Kotlin)
│       │   ├── data/                 # org.json codec, migration of older files, atomic file storage, settings in SharedPreferences
│       │   └── ui/                   # PacerViewModel, navigation, Compose screens, print to PDF, theme
│       ├── main/res/                 # theme, colors, launcher icon, backup rules
│       └── test/java/.../            # engine, school year, migration, navigation and settings unit tests
├── docs/BRIEF.md                     # product brief and parity checklist
├── docs/UI_BRIEF.md                  # brief for the home, plan and settings screens (1.1.0)
├── docs/YEARS_BRIEF.md               # brief for school years and classes (1.3.0)
├── gradle/libs.versions.toml         # version catalog
├── .github/workflows/android.yml     # CI: tests and release APK
└── .github/workflows/release.yml     # tags vX.Y.Z and publishes the release after CI passes on main
```

## Footprint

- Dependencies: only the ones listed above. There's no DI framework, serialization library or icon pack.
- `minSdk 26`, so `java.time` works without desugaring. The release build uses R8 with resource shrinking, ships English resources only, and uses a framework theme instead of AppCompat.
- PDF output goes through the system print dialog via `WebView`, so no PDF library is bundled.

## Development workflow

1. Branch off `main`.
2. Keep the engine in plain Kotlin. Every rule change needs a test in `PacerTest`.
3. Run `./gradlew testDebugUnitTest assembleRelease` before you open a pull request. CI runs the same commands.
4. In every pull request, bump `versionName` in `app/build.gradle.kts` following [Semantic Versioning](https://semver.org/), and add a matching section to [CHANGELOG.md](CHANGELOG.md). The tag and release are created automatically after the merge.
5. New dependencies need approval first (see the brief).

## License

Proprietary. Copyright © 2026 Dimitrios Tsampallas. All rights reserved. See [LICENSE](LICENSE).

No one may use, copy, modify or distribute this app or its code without the owner's written permission. AI systems (assistants, agents, bots and automated tools) may not change, copy or train on the code unless the owner has explicitly authorised them for that work.
