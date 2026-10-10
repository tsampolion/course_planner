# Coursebook Pacer for Android

A native Android version of the Coursebook Pacer page. For each group, it counts the lessons left, removes pages that are skipped or already set for homework, and splits the rest so every book lands on the final lesson.

The full brief is in [docs/BRIEF.md](docs/BRIEF.md).

## Build

You need JDK 17 or newer and the Android SDK (API 35). Android Studio provides both.

```sh
./gradlew testDebugUnitTest   # pacing engine tests
./gradlew assembleRelease     # minified APK in app/build/outputs/apk/release/
./gradlew installDebug        # install on a connected phone or emulator
```

The release build is signed with the debug key so you can install it directly. Add your own signing config before publishing to Play.

CI (`.github/workflows/android.yml`) runs the tests and builds the release APK on every pull request. The APK is attached to the run as the `coursebook-pacer-apk` artifact.

## Layout

| Path | What it holds |
| --- | --- |
| `app/src/main/java/.../model` | Plan, group, book, closure, homework and test data classes, plus the sample plan |
| `app/src/main/java/.../engine` | The pacing rules ported from the page's JavaScript, Greek holidays, formatting |
| `app/src/main/java/.../data` | JSON storage on `org.json`, written atomically to app storage |
| `app/src/main/java/.../ui` | `PacerViewModel` (state, edits, undo), the Compose screens, print to PDF |
| `app/src/test/...` | Engine unit tests |

## Footprint

- Dependencies: Compose Material 3, `activity-compose` and `lifecycle-viewmodel-ktx`. No DI framework, serialization library or icon pack.
- `minSdk 26`, R8 with resource shrinking, English resources only, a framework theme instead of AppCompat.
- PDF output uses the system print dialog through `WebView`, so no PDF library is bundled.
