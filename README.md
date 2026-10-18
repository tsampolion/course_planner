# Course Pacer for Android

[![Android](https://github.com/tsampolion/course_planner/actions/workflows/android.yml/badge.svg)](https://github.com/tsampolion/course_planner/actions/workflows/android.yml)

Course Pacer is part of the **EduSuite** apps, with [Course Invoicer](https://github.com/tsampolion/EduInvoiceApp). Both share one look, set out in [docs/EDUSUITE_STYLE_GUIDE.md](docs/EDUSUITE_STYLE_GUIDE.md); each has its own accent colour, teal for Course Pacer.

It started as a native Android version of the Coursebook Pacer web page, for teachers who have to get through one or more coursebooks by the end of term. For each group, it counts the lessons left, removes pages that are skipped or already set for homework, and splits the rest so every book lands on the final lesson.

The full brief is in [docs/BRIEF.md](docs/BRIEF.md). Release history is in [CHANGELOG.md](CHANGELOG.md), and features specified for later are in [docs/PLANNED_FEATURES.md](docs/PLANNED_FEATURES.md).

## Features

- **Home**: the open school year at a glance. A greeting with today's date and Settings at the top; a chip to switch, add, rename or delete school years; the **next lesson** across every class and group, with its pages and a **Mark done** button; and tiles for Classes, Overview, Tests, Homework, Days off & dates, and Print or save PDF, each with a live figure. A section with nothing in it yet shows its tile faded, and the tile still opens it.
- **School year settings**: rows open sheets for the year's name and dates; Delete lives under More options and offers Undo. Days off support swipe, long-press and TalkBack removal. One tap adds the Greek public holidays, which are worked out from Orthodox Easter.
- **Classes**: tiles show lessons left and problem chips. A class is a set of books taught to one or more groups; settings, books and groups open sheets for editing.
- **Class**: lesson length and the pace in pages per lesson you can sustain; books (first and last page, skipped pages such as `8, 24-27`, and an optional finish-by date that re-paces only that book); groups (name, teaching weekdays, next-lesson date, and a reset for every group's position); and links to tests and homework.
- **Tests and worksheets**: swipe rows, collapsed past tests and an add sheet defaulting to the group's next lesson. A test can take the whole lesson, which moves the pages forward, or run alongside it. The app warns when a test date isn't a lesson day.
- **Homework already set**: swipe rows and an add sheet starting at the group's current book position. Those pages drop out of the lessons for that group.
- **Overview**: every group in every class at a glance, with lessons and pages left, the pace needed, the verdict and the next lesson. The year's problems sit at the top and each links to the screen that fixes it.
- **Group view**: a next-lesson hero with Mark done and Edit pages taught, a weekly day strip, responsive lessons/pages/pace tiles and a verdict. Book positions open sheets; More details holds hours, peak pace, banked homework and month bars. The lesson-by-lesson schedule shows all months or one month per tab (swipe between them).
- **Done lessons**: tick a lesson once it's taught; it stays in the schedule, dimmed, with its pages. Long-press for "Done up to here" or "Not done from here". **Mark next lesson done** ticks the next one. Ticking out of order re-spreads the pages still to teach.
- **Pages taught**: when a lesson didn't go to plan, long-press it and choose "Edit pages taught…" to type the pages it covered in each book (`5-8`, or nothing). The lesson is marked done with a small "edited" tag; pages it missed go back into the schedule, extra pages come off later lessons, and the rest re-spread.
- **Output**: copy a group's remaining schedule as tab-separated text, share it, or print or save schedules as a PDF, choosing the classes and the margins.
- **Error list**: missing or reversed term dates, classes with no groups or books, groups with no days, bad page ranges, and finish-by dates that do nothing or leave no lesson.
- **Undo**: up to 60 steps within the open school year, from every screen inside it.
- **Settings**: light, dark or system theme, the lesson length and pace that new classes start with, and the app version.
- **Back up and restore** (in Settings): Export saves every school year to a file of your choice (Downloads, Google Drive and so on). Import reads that file, or the Coursebook Pacer web page's export, and asks whether to add its years beside yours or replace them; either can be undone.

School years and settings are saved on the device and included in Android backup and device-to-device transfer. Uninstalling deletes them, so export first.

### Moving your data to 2.0.0

Since 2.0.0 the app's id is `gr.edusuite.pacer` (it was `com.coursebook.pacer`), so it installs beside the old Course Pacer instead of over it, and starts empty. To move your school years:

1. In the old Course Pacer, open Settings → **Export…** and save the file.
2. Install Course Pacer 2.0.0 or later.
3. In the new app, open Settings → **Import…**, pick that file and choose **Add**.
4. Check your years are there, then uninstall the old Course Pacer.

Settings (theme and the defaults for new classes) aren't in the export, so set them again.

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

Screen copy and accessibility labels are Android string resources, with plurals for counts. Buttons and tiles answer presses with a small spring, figures roll when they change, and lists and expandable sections animate into place. Selections give the system's haptic feedback. Invalid fields show their error and shake once. Screen transitions slide and fade; Undo shows its remaining time. With system animations removed, transitions snap to their end state.

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

A local `assembleRelease` is signed with your debug key, so you can install it directly. Releases from CI are signed with the permanent release key instead (see below), so a local build can't install over a release, and a release can't install over it. `installDebug` installs a separate app, "Course Pacer (debug)" (`gr.edusuite.pacer.debug`), with its own data, so it never touches the installed release.

To sign a local build with the release key (owner only), set the keystore path and password first, without putting the password in the shell history:

```sh
read -s SIGNING_PASSWORD; export SIGNING_PASSWORD SIGNING_KEYSTORE=/path/to/course-pacer-release.p12
./gradlew assembleRelease
unset SIGNING_PASSWORD SIGNING_KEYSTORE
```

## Continuous integration

[`.github/workflows/android.yml`](.github/workflows/android.yml) runs on every push, every pull request and on manual dispatch. It sets up JDK 17, runs `testDebugUnitTest`, builds `assembleRelease`, checks the APK's signing certificate, writes the certificate and the APK size to the job summary, and uploads the APK as the `course-pacer-apk` artifact.

Release signing uses two secrets in the `release` environment (Settings → Environments → `release`, limited to the `main` branch) and one repository variable (Settings → Secrets and variables → Actions). The brief is [docs/SIGNING_BRIEF.md](docs/SIGNING_BRIEF.md).

| Name | Kind | Holds |
| --- | --- | --- |
| `SIGNING_KEYSTORE_B64` | `release` environment secret | the release keystore (`.p12`), base64-encoded |
| `SIGNING_PASSWORD` | `release` environment secret | its password |
| `SIGNING_CERT_SHA256` | variable | the certificate's SHA-256 fingerprint, lowercase without colons |

- On `main`, a missing secret, a missing variable, or a certificate that isn't `SIGNING_CERT_SHA256` fails the build, so nothing is published.
- Only builds of `main` use the `release` environment, so only they get the secrets. Other branches and every pull request are signed with the debug key and never publish, whatever their own build files do. Each `main` build shows on the repository page as a `release` deployment.

When that workflow passes on `main`, [`.github/workflows/release.yml`](.github/workflows/release.yml) reads `versionName` from `app/build.gradle.kts`. If the tag `vX.Y.Z` doesn't exist yet, it checks the APK's certificate against `SIGNING_CERT_SHA256` again, then creates the tag and a GitHub release, with that version's `CHANGELOG.md` section as the notes, a line naming the signing certificate, and the APK attached. To tag an older merge, run it manually from the Actions tab with the commit SHA.

## Project structure

```text
.
├── app/
│   ├── build.gradle.kts              # SDK levels, R8, signing, dependencies
│   ├── proguard-rules.pro            # empty on purpose: R8 defaults are enough
│   └── src/
│       ├── main/java/gr/edusuite/pacer/
│       │   ├── MainActivity.kt       # hosts Compose, flushes saves in onStop
│       │   ├── PacerApplication.kt   # composition root: creates the PlanStore and SettingsStore
│       │   ├── model/                # SchoolYear, SchoolClass, Group, Book, Closure, Homework, TestEvent, Plan, Settings; ids and new-year defaults
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
