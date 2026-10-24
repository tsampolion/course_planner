# In-app update check: design

Agreed with the owner on 2026-10-23 in a brainstorming session. It is classed as **architectural**: the app's first
network access, a new permission, and a dependency on the shape of `release.yml`'s output. No engine change. The
target version is **3.12.0** (3.11.0 shipped group assignments). Implementation is handed to another agent, so this spec names files and existing helpers.

## Context

Course Pacer is not on the Play Store. The owner installs it on their own phone and on a few colleagues' phones, and
can help each colleague once in person; after that, colleagues must be able to update on their own, without knowing
GitHub. Every merge to `main` already publishes a GitHub release on the public repository `tsampolion/course_planner`:

- tag `vX.Y.Z` (from `versionName`),
- one asset named exactly `course-pacer-X.Y.Z.apk`, checked against the permanent release certificate,
- the matching `CHANGELOG.md` section as the release body (Markdown).

Because every release is signed with the same permanent key, Android installs a downloaded release over the existing
app and keeps its data. What is missing is the app telling the teacher that a new version exists and handing them the
file.

## Decisions

| # | Decision |
|---|---|
| 1 | **Check only; the browser downloads.** The app checks GitHub and, on tap, opens the APK's direct download URL with `ACTION_VIEW`. The system installer installs it. No in-app download, no `REQUEST_INSTALL_PACKAGES`, no FileProvider. Obtainium and a full in-app installer were considered and rejected. |
| 2 | **Automatic, at most once a day, on by default.** On launch, if auto-check is on and the last successful check is 24 h or more ago. Settings has a switch to turn it off and a **Check now** button. |
| 3 | **Quiet notice.** No card, dialog or snackbar. A dot on the ☰ button, an "Update" tag on the drawer's Settings row, and an Updates section at the top of Settings. Nothing to dismiss: the badge stays until the update is installed. |
| 4 | **Teachers never see GitHub's pages** in the normal path. Download goes to the asset URL, not the release page. "See all changes" (optional) opens the release page. |
| 5 | **Debug builds never check** (a release cannot install over `gr.edusuite.pacer.debug`). |
| 6 | **No new dependency.** `HttpURLConnection`, `org.json`, coroutines already brought in by `lifecycle-viewmodel-ktx`. |

## What stays hard

- Engine parity and every approved extension: untouched.
- Approved dependencies only; Compose only; tokens, not literals (`ContrastTest`); UI copy in `strings.xml`
  (`NoHardcodedTextTest`); icons only as path data in `AppIcons.kt` (none expected to be needed).
- `applicationId`, the signing key, `minSdk 26` never change. No reflection, so `proguard-rules.pro` stays empty.

## Architecture

### Network

- Manifest: add `<uses-permission android:name="android.permission.INTERNET" />`. No other permission.
- One request: `GET https://api.github.com/repos/tsampolion/course_planner/releases/latest`, headers
  `Accept: application/vnd.github+json` and `User-Agent: CoursePacer/<versionName>`. No token, no body, no other data.
- `HttpURLConnection`, connect timeout 5 s, read timeout 10 s. Any non-200 response, exception or unreadable body is a
  failure (`null`). `/releases/latest` already excludes drafts and pre-releases.
- Behind a small interface so tests use a fake:

```kotlin
fun interface ReleaseSource { fun latestJson(): String? }   // null = failure
```

  The real implementation lives in `update/GitHubReleases.kt`, runs only on `Dispatchers.IO`.

### Plain Kotlin model (`update/UpdateModel.kt`, no Android imports, unit-tested)

```kotlin
data class Version(val major: Int, val minor: Int, val patch: Int) : Comparable<Version>
fun parseVersion(s: String): Version?          // "v3.10.0" or "3.10.0"; rejects "3.10", "3.10.0-beta", junk
fun isNewer(latest: Version, installed: Version): Boolean

data class Update(val version: String, val notes: String, val apkUrl: String, val pageUrl: String)

/** The fields read from a releases/latest body. Filled by the thin org.json shim in GitHubReleases.kt. */
data class Release(val tag: String?, val body: String?, val pageUrl: String?, val assets: List<Pair<String, String>>)

/** Null when anything needed is missing or there is no asset named exactly "course-pacer-X.Y.Z.apk" for its tag. */
fun toUpdate(release: Release): Update?
fun pickApk(assets: List<Pair<String, String>>, version: String): String?   // (name, url) -> url

/** Due when never checked, when 24 h or more have passed, or when the clock moved backwards. */
fun isDue(lastCheck: Long, now: Long): Boolean

/** Plain text for the Updates section: the first 3 non-empty lines of the Markdown body, with list markers
 *  ("- ", "* ", "1. "), heading hashes, bold/italic markers and link syntax ([text](url) -> text) removed. */
fun shortNotes(markdown: String): String

/** The update to show, or null: [stored] only if it parses and is newer than [installed]. */
fun pendingUpdate(stored: Update?, installed: String): Update?
```

No JVM unit test in this repo uses `org.json` (Android's stub throws there), and adding `org.json` as a test
dependency needs the owner's approval. So `GitHubReleases.kt` holds a thin, untested `readRelease(json): Release?`
that only copies `tag_name`, `body`, `html_url` and `assets[].name` / `browser_download_url` out of `org.json`
(any exception gives `null`); every decision lives in `toUpdate` and the functions above, which take plain data.

### State

`Settings` (`model/Settings.kt`) gains, all outside undo and saved by `SettingsStore` in `settings.xml` (already in
`data_extraction_rules.xml`, so no rules change):

| Field | Default | Meaning |
|---|---|---|
| `autoUpdateCheck: Boolean` | `true` | Check once a day on launch. |
| `lastUpdateCheck: Long` | `0` | Epoch millis of the last **successful** check. |
| `availableUpdate: Update?` | `null` | Last update found and not yet installed. Stored as four string keys (version, notes, apk URL, page URL); missing or partial keys read as `null`. |

New `SettingsStore` keys follow the existing pattern (`runCatching` around reads, as for `CLASS_ORDER`).

`UiState` gets `update: Update?` (derived: `pendingUpdate(settings.availableUpdate, installedVersion)`) and
`updateCheck: CheckState` for Check now's feedback (`Idle`, `Checking`, `UpToDate`, `Failed`), session only.

### Flow (`PacerViewModel`)

1. **Launch** (once per ViewModel creation): read the installed `versionName` from `PackageManager` (as
   `SettingsScreen.kt` already does; `buildConfig` stays off). If the app is debuggable
   (`ApplicationInfo.FLAG_DEBUGGABLE`), stop: no check, no badge, and Settings hides the Updates section.
   If `availableUpdate` is no longer newer than the installed version, clear it.
2. If `autoUpdateCheck` and `isDue(lastUpdateCheck, now)`: run one check on `viewModelScope` + `Dispatchers.IO`.
   - Success with an update newer than installed: store it in `availableUpdate`, set `lastUpdateCheck = now`.
   - Success with nothing newer (or no matching APK): clear `availableUpdate`, set `lastUpdateCheck = now`.
   - Failure: change nothing, show nothing. At most one attempt per launch, no retry loop.
3. **Check now** (Settings): same check, ignoring `isDue` and `autoUpdateCheck`; sets `updateCheck` to `Checking`,
   then `UpToDate`, `Failed`, or `Idle` with the update showing.

The ViewModel takes a `ReleaseSource` and a clock through `PacerViewModel.Factory` (manual DI, as today) so the flow
is testable with fakes.

## UI

All copy in `strings.xml`. Colours from existing tokens only.

- **☰ dot.** Where the ☰ `PillIconButton` is drawn (`ui/PacerApp.kt` top bars and `ui/HomeScreen.kt`), wrap the icon
  in Material 3 `BadgedBox` with a small `Badge()` when `update != null`. Its content description becomes
  "Menu, update available". On tablets at 840dp and up (`AppDrawer(permanent = true)`, no ☰) there is no dot; the
  Settings row tag below is the notice.
- **Drawer Settings row** (`ui/NavUi.kt`, the menu-row helper's `badge` slot): the existing `Tag(...)` from
  `ui/Components.kt` (not `warn`) with the text "Update", described as "Update available".
- **Settings → Updates**, the first section of `SettingsScreen.kt`, hidden in debug builds:
  - With an update: "Version X.Y.Z is available", the `shortNotes` text, a primary **Download** button, the line
    "The first time, Android may ask you to allow installs from your browser.", and a text button
    **See all changes** (opens `pageUrl`).
  - Always: a switch **Check for updates automatically** (`autoUpdateCheck`), a **Check now** button with its result
    line ("Checking…", "Course Pacer is up to date.", "Couldn't check for updates. Check your connection and try
    again."), and "Installed version X.Y.Z" (move the existing version line here rather than showing it twice).
  - Large text (`largeText(fontScale)`): the section reflows like the rest of Settings; nothing is hidden.
- **Download / See all changes** start `Intent(ACTION_VIEW, Uri.parse(url))`. On `ActivityNotFoundException`, copy the
  URL to the clipboard and show "No browser found. The download link was copied."

## Error handling and edge cases

- Offline, timeout, HTTP error, rate limit (60 requests/hour per IP unauthenticated), bad JSON: silent for the
  automatic check; one "Couldn't check" line for Check now. `lastUpdateCheck` moves only on success.
- Tag newer but no `course-pacer-X.Y.Z.apk` asset (a release published with `allow_no_apk`): no update.
- Installed version equal to or newer than latest (local builds): no update; a stored one is cleared.
- Phone running a debug-signed `assembleRelease` (built locally): the system installer refuses with "App not
  installed". Not handled in-app; `docs/UPDATES_BRIEF.md` documents export, uninstall, install release, import.
- Process death during a check: nothing is written until success, so nothing is lost.

## Testing

JVM unit tests in `app/src/test/java/gr/edusuite/pacer/update/UpdateModelTest.kt` (and a ViewModel-flow test if the
existing tests construct the ViewModel; otherwise extract the flow into a plain `UpdateChecker` class and test that):

- `parseVersion`: `v3.10.0`, `3.10.0` accepted; `v3.10`, `3.10.0-beta`, `""`, `vx.y.z` rejected.
- `isNewer`: 3.10.0 > 3.9.0; equal is not newer; older is not newer; 4.0.0 > 3.99.99; 3.10.1 > 3.10.0.
- `pickApk`: exact name found among other assets; missing; asset for a different version ignored.
- `toUpdate`: a `Release` built from a real `releases/latest` response (tag, body, URL, several assets); null tag;
  unparseable tag; no matching asset; null body (gives empty notes); null page URL.
- `isDue`: `0` (never), 23 h, exactly 24 h, clock moved backwards.
- `shortNotes`: bullets, links and bold stripped; at most 3 lines; empty body gives empty text.
- `pendingUpdate`: newer kept, equal/older dropped, unparseable stored version dropped.
- Flow with a fake `ReleaseSource` and clock: not due → no fetch; due + newer → stored and timestamp set;
  due + failure → nothing changes; auto-check off → no fetch; Check now ignores both; debuggable → no fetch.
- `SettingsStore` keys are not unit-testable on the JVM if they need `SharedPreferences`; keep the parse logic for
  `availableUpdate` in plain Kotlin (four nullable strings → `Update?`) and test that.

Existing gates must pass: `./gradlew testDebugUnitTest` (includes `NoHardcodedTextTest`, `ContrastTest`) and
`./gradlew assembleRelease`.

**Manual check** before merging. A local or PR build is debug-signed, so it cannot install over a release; use an
emulator or a phone without Course Pacer. Build with `versionName` temporarily set below the latest release (do not
commit that), install it, launch, and confirm the ☰ dot, the drawer's "Update" tag and Settings → Updates appear with
the real latest release; tap Download and confirm the browser downloads `course-pacer-X.Y.Z.apk`; turn on airplane
mode and confirm Check now shows the failure line. Restore the committed `versionName`. The full upgrade
(3.12.0 → the next release) gets its first real run after 3.12.0 ships.

## Docs and version

- New `docs/UPDATES_BRIEF.md`: what the app relies on (`vX.Y.Z` tag, `course-pacer-X.Y.Z.apk` asset, changelog body,
  public repo), the once-a-day rule, the privacy statement, the debug-build caveat, and colleague setup steps.
- `.github/workflows/release.yml`: a comment next to the asset rename (`course-pacer-$VERSION.apk`) saying the app's
  update check depends on this exact name.
- `README.md`: a feature bullet; a privacy line ("connects only to `api.github.com`, once a day while automatic
  checks are on or when you tap Check now, and sends no data; turn it off in Settings"); an "Installing and updating"
  section for colleagues (download the APK from Releases once, allow installs from the browser, then use Settings →
  Updates).
- `CLAUDE.md`: an architecture line for `update/`, the `INTERNET` permission and the new `Settings` fields; a rule
  that the release asset name and tag format are now a contract with installed apps.
- `docs/README.md`: index entry for `UPDATES_BRIEF.md`.
- Version: `versionName = "3.12.0"`, `versionCode = 48`, `CHANGELOG.md` `## [3.12.0] - <date>` with its compare link.

## Out of scope

In-app download and install, background or scheduled checks (WorkManager is not an approved dependency), update
notifications, pre-release channels, skipping a specific version, and Obtainium documentation.
