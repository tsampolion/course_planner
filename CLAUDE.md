# CLAUDE.md

Coursebook Pacer: a single-module Kotlin + Jetpack Compose Android app (`com.coursebook.pacer`). It ports a web page that paces coursebook pages over the lessons left in a term. `docs/BRIEF.md` is the spec and feature-parity checklist.

## Commands

```sh
./gradlew testDebugUnitTest                      # all unit tests (engine only)
./gradlew testDebugUnitTest --tests 'com.coursebook.pacer.engine.PacerTest.parsesRanges'   # one test
./gradlew assembleRelease                        # R8-minified, debug-signed APK
./gradlew installDebug                           # needs a device or emulator
./gradlew lint
```

CI (`.github/workflows/android.yml`) runs `testDebugUnitTest` then `assembleRelease` on JDK 17. Any change has to pass both.

## Architecture

- `model/`: immutable data classes. `AppData` holds every plan, `activePlan`, and `view` (`"setup"` or a group id). `ALL_GROUPS = "all"` stands in for a group id on closures and tests. `Seed.kt` builds the sample plan and `newId()`.
- `engine/`: pure Kotlin with no Android imports, so it's unit-testable on the JVM. `Pacer.kt` holds the rules: `lessonsFor`, `remainingPages`, `allocatePages`, `GroupReport`, `verdict()`, `problems()`. `Holidays.kt` covers Orthodox Easter and Greek holidays. `Format.kt` handles display and TSV.
- `data/`: `JsonCodec` maps to `org.json` by hand. `PlanStore` writes `plans.json` in `filesDir` through `AtomicFile`. If the file can't be read, it's copied to `plans.json.bad` and the app falls back to the seed plan.
- `ui/`: one `PacerViewModel` exposes `StateFlow<UiState>`, and Compose reads it with `collectAsState`. Every mutation goes through `edit`, `editPlan`, `editGroup` or `editBook`, which push undo (at most 60 steps; edits with the same tag within 1.2 s merge) and schedule a debounced save (400 ms). `MainActivity.onStop` calls `vm.flush()`. `PrintSheet` renders HTML into a `WebView` for `PrintManager`.
- DI is manual: `PacerApplication.store` goes to `PacerViewModel.Factory`.

## Rules

- **Engine parity is the contract.** The schedule must match the original web page row for row. Lesson *i* ends at `round(i × pages / lessons)`, with halves rounding up in integer arithmetic. Don't "improve" the rules. Any engine change needs a test in `app/src/test/.../engine/PacerTest.kt`.
- **Approved dependencies only**: the Compose BOM with Material 3, `activity-compose`, `lifecycle-viewmodel-ktx`, and JUnit. Ask before adding anything, including kotlinx-serialization, Hilt, AppCompat or an icon library. Use the platform instead (`org.json`, `AtomicFile`, `WebView`, the clipboard, the share sheet).
- Declare versions in `gradle/libs.versions.toml`, not inline.
- Compose only, with no XML layouts. Use the framework theme (`Theme.Pacer`), not AppCompat.
- Keep `minSdk 26` (it lets `java.time` run without desugaring) and `resourceConfigurations += "en"`.

## Gotchas

- The JSON format has to stay readable from the web page's localStorage export. Weekdays are numbered **0 = Sunday**, unlike `java.time.DayOfWeek`, and dates are ISO strings. If you change the schema, bump `JsonCodec.VERSION` and keep reading old files.
- Don't give the `edit` helpers a tag for structural edits (add or remove). Tags are for per-field typing, so it merges into one undo step.
- `proguard-rules.pro` is empty because nothing uses reflection. If you add reflection or JNI, add keep rules and check `assembleRelease`.
- The release build is signed with the debug key on purpose. Don't commit keystores (`*.jks` and `*.keystore` are gitignored).
- `data_extraction_rules.xml` backs up only `plans.json`. If you add another stored file, update it.

## Workflow

- Run `./gradlew testDebugUnitTest` (and `assembleRelease` if you touched the build or dependencies) before committing.
- Record every user-facing change under `## [Unreleased]` in `CHANGELOG.md` (Keep a Changelog, SemVer). When you release, bump `versionName` and `versionCode` in `app/build.gradle.kts` to match.
