# Coursebook Pacer for Android

[![Android](https://github.com/tsampolion/course_planner/actions/workflows/android.yml/badge.svg)](https://github.com/tsampolion/course_planner/actions/workflows/android.yml)

A native Android version of the Coursebook Pacer page, for teachers who have to get through one or more coursebooks by the end of term. For each group, it counts the lessons left, removes pages that are skipped or already set for homework, and splits the rest so every book lands on the final lesson.

The full brief is in [docs/BRIEF.md](docs/BRIEF.md). Release history is in [CHANGELOG.md](CHANGELOG.md).

## Features

- **Home**: every group at a glance, with lessons and pages left, the pace needed, the verdict and the next lesson. Plan problems sit at the top and each links to the screen that fixes it. From here, open a group or any part of the plan.
- **Plans**: keep several plans and switch between them from the title on Home. Rename, create or delete a plan (deleting asks first). Undo goes back up to 60 steps.
- **Term**: start and end dates, lesson length, and the pace in pages per lesson you can sustain.
- **Groups**: name, teaching weekdays and next-lesson date. Reset every group's position in one step.
- **Days off**: for all groups or one group. One tap adds the Greek public holidays, which are worked out from Orthodox Easter.
- **Tests and worksheets**: a test can take the whole lesson, which moves the pages forward, or run alongside it. The app warns when a test date isn't a lesson day.
- **Books**: first and last page, skipped pages (such as `8, 24-27`), and an optional finish-by date that re-paces only that book.
- **Homework already set**: those pages drop out of the lessons for that group.
- **Group view**: a summary, a verdict (lessons to spare or the shortfall compared with your sustainable pace), and for each book the resume position, pages left and pace. Also lessons per month, and the lesson-by-lesson schedule grouped by month, with **Mark next lesson done**.
- **Output**: copy the schedule as tab-separated text, share it, or print or save every group's schedule as a PDF.
- **Error list**: missing or reversed term dates, groups with no days, bad page ranges, and finish-by dates that do nothing or leave no lesson.
- **Settings**: light, dark or system theme, the lesson length and pace that new plans start with, and the app version.

Plans and settings are saved on the device and included in Android backup and device-to-device transfer.

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
│       │   ├── model/                # Plan, Group, Book, Closure, Homework, TestEvent, Settings; sample plan
│       │   ├── engine/               # pacing rules, Greek holidays, formatting (plain Kotlin)
│       │   ├── data/                 # org.json codec, atomic file storage, settings in SharedPreferences
│       │   └── ui/                   # PacerViewModel, navigation, Compose screens, print to PDF, theme
│       ├── main/res/                 # theme, colors, launcher icon, backup rules
│       └── test/java/.../            # engine, navigation and settings unit tests
├── docs/BRIEF.md                     # product brief and parity checklist
├── docs/UI_BRIEF.md                  # brief for the home, plan and settings screens
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
