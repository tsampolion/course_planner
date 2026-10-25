# Course Pacer for Android

[![Android](https://github.com/tsampolion/course_planner/actions/workflows/android.yml/badge.svg)](https://github.com/tsampolion/course_planner/actions/workflows/android.yml)

Documentation index: [docs/README.md](docs/README.md)

Course Pacer is part of the **EduSuite** apps, with [Course Invoicer](https://github.com/tsampolion/EduInvoiceApp). Both share one look, set out in [docs/EDUSUITE_STYLE_GUIDE.md](docs/EDUSUITE_STYLE_GUIDE.md); each has its own palette. Course Pacer offers Bone & green (default) and Saffron & ink, each in light and dark.

It started as an Android version of the Coursebook Pacer web page, for teachers who must get through coursebooks by the end of term. For each group, it counts the lessons left, removes skipped pages and homework pages whose due or set-on date has arrived, and splits the rest so every book lands on the final lesson.

Release history is in [CHANGELOG.md](CHANGELOG.md).

## Features

Each line links to where the feature is explained: a decision record in `docs/` or the [architecture](docs/ARCHITECTURE.md).

- **Pacing**: for each group, count the lessons left, remove skipped pages and homework pages already set, and split the rest so every book lands on the last lesson. [Spec](docs/BRIEF.md)
- **School years, classes and groups**: terms, days off with one-tap Greek holidays from Orthodox Easter, classes with books and groups. [Brief](docs/YEARS_BRIEF.md)
- **Home**: next lesson with **Done**, your classes and the Pacing overview board of groups behind. [Home](docs/ARCHITECTURE.md#home)
- **Menu and group switcher**: find any class or group by name from ☰ on every screen. [Navigation](docs/ARCHITECTURE.md#navigation)
- **Group**: Plan (today, week sections, long-press lesson menu), Work and Stats with the verdict and pace track. [Group screens](docs/ARCHITECTURE.md#group-screens)
- **Done lessons and pages taught**: tick lessons, or record the pages you really covered; the rest re-spreads. [Brief](docs/LESSON_PAGES_BRIEF.md)
- **Custom plans**: make the proposed schedule your own, edit lessons, re-pace and switch back. [Brief](docs/CUSTOM_PLANS_BRIEF.md)
- **Lead book**: other books keep step with the one you choose. [Brief](docs/CUSTOM_PLANS_BRIEF.md)
- **Warnings and catch-up**: every warning has a one-tap fix, and re-pacing keeps your catch-up pace. [Brief](docs/CATCH_UP_REPACE_BRIEF.md)
- **Supplementary books**: companions and readers placed by hand in chosen lessons. [Brief](docs/SUPPLEMENTARY_BOOKS_BRIEF.md)
- **Extra lessons**: pin core-book pages to a date, including days off. [Brief](docs/EXTRA_LESSONS_BRIEF.md)
- **Group settings and collections**: per-group overrides, optional books, named collections of classes and class sorting. [Brief](docs/GROUP_SETTINGS_BRIEF.md)
- **Assignments**: Homework, Tests and Worksheets owned by each group. [Brief](docs/ASSIGNMENTS_BRIEF.md)
- **Lesson homework**: set and due homework in the lesson menu, optionally paired with class work. [Brief](docs/HOMEWORK_BRIEF.md)
- **Book units**: linked books finish each unit together, with optional revisions. [Brief](docs/UNITS_BRIEF.md)
- **Output**: copy, share, or print or save as PDF a group, a class or the year. [Print](docs/ARCHITECTURE.md#print)
- **Large text and tablets**: screens show less from font scale 1.5; list and detail panes on tablets. [Layout](docs/ARCHITECTURE.md#large-text-and-tablets)
- **Undo**: up to 60 steps within the open school year. [Edits and undo](docs/ARCHITECTURE.md#view-model-edits-and-undo)
- **Backup**: export every school year to a file, import it or the web page's export. [Brief](docs/BACKUP_BRIEF.md)
- **Two palettes**: Bone & green and Saffron & ink, light and dark. [Design](docs/PACER_DESIGN.md)
- **Updates**: a daily check against GitHub releases, with **Download** in Settings. [Brief](docs/UPDATES_BRIEF.md)

School years and settings are saved on the device and included in Android backup and device-to-device transfer. Uninstalling deletes them, so export first.

The app's only network use is the update check: one request to api.github.com, once a day while automatic checks are on or when you tap Check now. It sends no data about you or your school years.

## Installing on a phone

1. Open [Releases](https://github.com/tsampolion/course_planner/releases/latest) in the phone's browser.
2. Download the latest `course-pacer-0.X.Y.Z.apk`.
3. Allow installs from the browser if Android asks.
4. Install the APK and open Course Pacer.

After this first installation, Settings → Updates shows new versions. Download
and install them to keep your school years while updating. See
[In-app updates](docs/UPDATES_BRIEF.md) for setup and the locally built copy caveat.

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

[`.github/workflows/android.yml`](.github/workflows/android.yml) runs on every push, pull request and manual dispatch. It runs the tests and lint, builds the release APK and checks its signing certificate. When it passes on `main`, [`.github/workflows/release.yml`](.github/workflows/release.yml) tags `v0.X.Y.Z` from `versionName` and publishes a GitHub release with that version's `CHANGELOG.md` section as the notes. Only `main` signs with the release key. Other branches use the debug key and never publish. How release signing works: [docs/SIGNING_BRIEF.md](docs/SIGNING_BRIEF.md).

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
│       │   ├── model/                # SchoolYear, SchoolClass, Group, Book, Closure, Assignment, Plan, Settings
│       │   ├── engine/               # pacing rules, Greek holidays, formatting (plain Kotlin)
│       │   ├── data/                 # org.json codec, migration of older files, atomic file storage, settings
│       │   └── ui/                   # PacerViewModel, navigation, Compose screens, print to PDF, theme
│       ├── main/res/                 # strings, theme, font, launcher icon, backup rules
│       └── test/java/.../            # unit tests
├── docs/                             # architecture, briefs, style guide; start at docs/README.md
├── gradle/libs.versions.toml         # version catalog
├── .github/workflows/android.yml     # CI: tests and release APK
└── .github/workflows/release.yml     # tags v0.X.Y.Z and publishes the release after CI passes on main
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
3. Run `./gradlew testDebugUnitTest lint assembleRelease` before you open a pull request. CI runs the same checks.
4. Bump the version: `versionName` and `versionCode` in `app/build.gradle.kts`, and a matching section in [CHANGELOG.md](CHANGELOG.md). The release is automatic after the merge.
5. Update the docs your change affects and delete the ones it makes obsolete, in the same pull request. The rules are in [CLAUDE.md](CLAUDE.md#docs-stay-current-and-uncluttered).
6. New dependencies need approval first.

## License

Proprietary. Copyright © 2026 Dimitrios Tsampallas. All rights reserved. See [LICENSE](LICENSE).

No one may use, copy, modify or distribute this app or its code without the owner's written permission. AI systems (assistants, agents, bots and automated tools) may not change, copy or train on the code unless the owner has explicitly authorised them for that work.
