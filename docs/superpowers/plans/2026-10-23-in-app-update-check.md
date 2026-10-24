# In-app Update Check Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking. If those skills are not available (for example in Codex), execute the tasks yourself, strictly in order, ticking each checkbox as you go, and do not start a task until the previous task's tests pass.

**Goal:** Course Pacer checks GitHub once a day for a newer signed release, shows a quiet badge, and lets the teacher download the APK in the browser so Android's installer updates the app in place.

**Architecture:** A plain-Kotlin `update/` package holds every decision (version parsing, release validation, throttling, note trimming, settings transitions) and is unit-tested on the JVM. A thin Android shim (`GitHubReleases`) does the one HTTP GET and copies fields out of `org.json`. `PacerViewModel` runs the check on launch and on demand and stores the result in `Settings` (outside undo, in `settings.xml`). Compose shows a dot on ☰, an "Update" tag on the drawer's Settings row and an Updates section in Settings.

**Tech Stack:** Kotlin 2.1, Jetpack Compose Material 3 (Compose BOM 2025.05.01), `lifecycle-viewmodel-ktx` 2.9.0 (brings coroutines), `java.net.HttpURLConnection`, `org.json` (platform), JUnit 4.13.2.

**Spec:** `docs/superpowers/specs/2026-10-23-in-app-update-check-design.md`. Read it before Task 1. When this plan and the spec disagree, this plan wins (it was written later, against the code), and you note the difference in the PR.

**Repository:** `tsampolion/course_planner`, local checkout root `course_planner/`. All paths below are relative to that root. Kotlin sources live in `app/src/main/java/gr/edusuite/pacer/`, abbreviated `<main>/` below; JVM tests live in `app/src/test/java/gr/edusuite/pacer/`, abbreviated `<test>/`.

**Branch:** work on `claude/dreamy-carson-ms25q9`. It already holds the spec and this plan, and has an open PR, `tsampolion/course_planner#50`. Before Task 1:

```bash
git fetch origin claude/dreamy-carson-ms25q9
git checkout claude/dreamy-carson-ms25q9
git pull origin claude/dreamy-carson-ms25q9
./gradlew testDebugUnitTest   # must pass before you change anything
```

## Global Constraints

- **No new dependency of any kind**, including test dependencies. In particular, do not add `org.json` to `testImplementation`, and do not add OkHttp, Retrofit, kotlinx-serialization, WorkManager, Robolectric or Mockito. Versions live only in `gradle/libs.versions.toml`, and that file does not change in this plan.
- **One new permission only:** `android.permission.INTERNET`. Do not add `REQUEST_INSTALL_PACKAGES`, `ACCESS_NETWORK_STATE`, a FileProvider or a receiver.
- **Endpoint (exact):** `https://api.github.com/repos/tsampolion/course_planner/releases/latest`. Headers: `Accept: application/vnd.github+json` and `User-Agent: CoursePacer/<installed versionName>`. No token, no query string, no body.
- **Timeouts:** connect 5000 ms, read 10000 ms.
- **Asset name (exact):** `course-pacer-X.Y.Z.apk`, where `X.Y.Z` is the tag without its leading `v`. The tag format is `vX.Y.Z`.
- **Trusted URL prefixes (exact):** the APK URL must start with `https://github.com/tsampolion/course_planner/releases/download/`, and the page URL with `https://github.com/tsampolion/course_planner/releases/`. Anything else is rejected, both when parsing a release and when loading stored settings.
- **Throttle:** an automatic check runs only when `now - lastUpdateCheck >= 86_400_000` ms, or `lastUpdateCheck == 0`, or `now < lastUpdateCheck` (clock moved backwards). `lastUpdateCheck` changes only after a successful request.
- **Debuggable builds never check.** That means `(applicationInfo.flags and ApplicationInfo.FLAG_DEBUGGABLE) != 0`, which covers `gr.edusuite.pacer.debug`. They show no badge and no Updates section.
- **UI copy goes only in `app/src/main/res/values/strings.xml`.** Escape apostrophes as `\'` and use `…` (U+2026). `NoHardcodedTextTest` fails on any letter-bearing string literal in a top-level file of `<main>/ui/`, unless that literal is listed for that file in the test's `dataStrings` map.
- **Colours only from `Pacer.colors`** (tokens in `<main>/ui/theme/Tokens.kt`). No `Color(0x…)` literals, no new tokens.
- **No reflection.** `app/proguard-rules.pro` stays empty, and `./gradlew assembleRelease` must pass.
- **Engine untouched:** no file under `<main>/engine/` or `<main>/data/JsonCodec.kt` changes, and `JsonCodec.VERSION` stays where it is (settings are not in `plans.json`).
- **Never change** `applicationId`, `minSdk 26`, the signing config, the `keyAlias`, or CI secret and variable names.
- **Version:** `versionName = "4.1.0"`, `versionCode = 49`, plus a `CHANGELOG.md` section `## [4.1.0] - <today's date, YYYY-MM-DD>` with a compare link `[4.1.0]: https://github.com/tsampolion/course_planner/compare/v4.0.0...v4.1.0`. (`main` is at 4.0.0 / 48 after lesson homework. If `main` has moved again by the time you finish, take the next MINOR above it.)
- **Test style:** JUnit 4 (`org.junit.Test`, `org.junit.Assert.*`), camelCase test names, no backtick names, as in `<test>/ui/SettingsTest.kt`.
- **Commits:** one per task, with conventional prefixes (`feat:`, `test:`, `docs:`). End every commit message with a trailer naming the agent, for example `Co-Authored-By: Codex <noreply@openai.com>`. Do not put model identifiers anywhere in code, docs or PR text.
- **The validation gate after every task** is `./gradlew testDebugUnitTest` → `BUILD SUCCESSFUL`. Tasks 5 and 7 also run `./gradlew assembleRelease` → `BUILD SUCCESSFUL`.

## Review Focus

These are inputs and failure modes the spec implies but nothing else pins down. Each one has a test in the task named.

1. **A release whose asset or page URL points somewhere other than this repository's releases** (a tampered or proxied response, or a hand-edited `settings.xml`). Reject it, so the app never opens an arbitrary URL. Task 1 (`toUpdateRejectsForeignUrls`) and Task 2 (`storedUpdateRejectsForeignUrls`).
2. **An edit made while a check is in flight** (for example, the teacher flips the auto-check switch or changes the theme during the 10 s read). The check result must merge into the settings as they are when it lands, not overwrite them with a stale copy. Task 3 (`appliedKeepsOtherSettings`).
3. **Settings restored to a new phone by Android backup**, carrying an `availableUpdate` for a version that's already installed or older. Show no badge and clear it at launch. Task 3 (`launchClearsCaughtUpUpdate`).
4. **A release body with Windows line endings, nested Markdown or images.** `shortNotes` must still return at most 3 clean lines with no `\r`, `![`, `**` or `](`. Task 1 (`shortNotesHandlesCrLfAndImages`).
5. **Version parts that are large or have leading zeros** (`v3.010.0`, `v10.0.0`, `v3.10.2147483648`). Compare them as numbers, and reject parts that overflow `Int` instead of crashing. Task 1 (`parseVersionNumericEdgeCases`).

---

## File Structure

| File | Status | Responsibility |
|---|---|---|
| `<main>/update/UpdateModel.kt` | Create | Pure functions and data types: `Version`, `parseVersion`, `isNewer`, `Update`, `Release`, `pickApk`, `toUpdate`, `isDue`, `shortNotes`, `pendingUpdate`, `storedUpdate`, URL prefix constants. No Android imports. |
| `<main>/update/UpdateChecker.kt` | Create | `ReleaseSource` interface, `Fetched` result type, `UpdateChecker` class: launch cleanup, the auto-check decision, fetch, and applying a result to `Settings`. No Android imports. |
| `<main>/update/GitHubReleases.kt` | Create | Android shim: `HttpURLConnection` GET and `readRelease(json): Release?` with `org.json`. Not unit-tested (Android stubs on the JVM). |
| `<main>/model/Settings.kt` | Modify | Three new fields and sanitising of `availableUpdate`. |
| `<main>/data/SettingsStore.kt` | Modify | Six new SharedPreferences keys. |
| `<main>/PacerApplication.kt` | Modify | Builds the `UpdateChecker` (installed version, debuggable flag, `GitHubReleases`). |
| `<main>/MainActivity.kt` | Modify | Passes the checker to `PacerViewModel.Factory`. |
| `<main>/ui/PacerViewModel.kt` | Modify | Takes an `UpdateChecker`; exposes `update`, `updateCheck`, `updatesEnabled`, `installedVersion`; `checkForUpdates()`, `setAutoUpdateCheck()`; runs the launch check. |
| `<main>/ui/NavUi.kt` | Modify | New `MenuButton(onMenu, badge)`; "Update" tag on the drawer's Settings row. |
| `<main>/ui/PacerApp.kt` | Modify | Collects `vm.update`; uses `MenuButton`; passes the badge flag to the drawer and Home. |
| `<main>/ui/HomeScreen.kt` | Modify | `Header` uses `MenuButton`; `HomeScreen` takes `menuBadge`. |
| `<main>/ui/UpdatesUi.kt` | Create | `UpdatesSection` composable and `openLink` helper. |
| `<main>/ui/ClassScreens.kt` | Modify | `SwitchRow` from `private` to `internal` (reused). |
| `<main>/ui/SettingsScreen.kt` | Modify | Updates section first; version line moves into it when updates are enabled. |
| `app/src/main/AndroidManifest.xml` | Modify | `INTERNET` permission. |
| `app/src/main/res/values/strings.xml` | Modify | New strings. |
| `<test>/update/UpdateModelTest.kt` | Create | Tests for `UpdateModel.kt`. |
| `<test>/update/UpdateCheckerTest.kt` | Create | Tests for `UpdateChecker.kt` with a fake source and clock. |
| `<test>/ui/SettingsTest.kt` | Modify | Tests for the new `Settings` fields. |
| `<test>/ui/NoHardcodedTextTest.kt` | Modify | Adds `"updates"` to `SettingsScreen.kt`'s allowed keys. |
| `.github/workflows/release.yml`, `README.md`, `CLAUDE.md`, `docs/README.md`, `docs/UPDATES_BRIEF.md` (create), `CHANGELOG.md`, `app/build.gradle.kts` | Modify | Docs, contract comment, version. |

---

### Task 1: Pure update model

**Files:**
- Create: `<main>/update/UpdateModel.kt`
- Test: `<test>/update/UpdateModelTest.kt`

**Interfaces:**
- Consumes: nothing.
- Produces (package `gr.edusuite.pacer.update`):
  ```kotlin
  const val RELEASES_API = "https://api.github.com/repos/tsampolion/course_planner/releases/latest"
  const val APK_URL_PREFIX = "https://github.com/tsampolion/course_planner/releases/download/"
  const val PAGE_URL_PREFIX = "https://github.com/tsampolion/course_planner/releases/"
  const val CHECK_INTERVAL_MS = 86_400_000L
  const val NOTE_LINES = 3

  data class Version(val major: Int, val minor: Int, val patch: Int) : Comparable<Version>
  fun parseVersion(s: String?): Version?
  fun isNewer(latest: Version, installed: Version): Boolean
  data class Update(val version: String, val notes: String, val apkUrl: String, val pageUrl: String)
  data class Release(val tag: String?, val body: String?, val pageUrl: String?, val assets: List<Pair<String, String>>)
  fun pickApk(assets: List<Pair<String, String>>, version: String): String?
  fun toUpdate(release: Release): Update?
  fun isDue(lastCheck: Long, now: Long): Boolean
  fun shortNotes(markdown: String?): String
  fun pendingUpdate(stored: Update?, installed: String): Update?
  fun storedUpdate(version: String?, notes: String?, apkUrl: String?, pageUrl: String?): Update?
  ```
  `Update.version` is always the bare `X.Y.Z` with no `v`.

- [x] **Step 1: Write the failing tests** in `<test>/update/UpdateModelTest.kt`

```kotlin
package gr.edusuite.pacer.update

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class UpdateModelTest {
    private val apk = "https://github.com/tsampolion/course_planner/releases/download/v3.11.0/course-pacer-3.11.0.apk"
    private val page = "https://github.com/tsampolion/course_planner/releases/tag/v3.11.0"
    private fun release(
        tag: String? = "v3.11.0",
        body: String? = "### Added\n\n- Update check in Settings.\n- A dot on the menu.",
        pageUrl: String? = page,
        assets: List<Pair<String, String>> = listOf(
            "course-pacer-3.11.0.apk" to apk,
            "notes.txt" to "https://github.com/tsampolion/course_planner/releases/download/v3.11.0/notes.txt",
        ),
    ) = Release(tag, body, pageUrl, assets)

    @Test fun parseVersionAcceptsTagsAndBareVersions() {
        assertEquals(Version(3, 10, 0), parseVersion("v3.10.0"))
        assertEquals(Version(3, 10, 0), parseVersion("3.10.0"))
        assertEquals(Version(3, 10, 0), parseVersion(" v3.10.0 "))
    }

    @Test fun parseVersionRejectsOtherShapes() {
        listOf(null, "", "v3.10", "3.10.0-beta", "3.10.0.1", "vx.y.z", "V3.10.0", "v-1.0.0", "3..0").forEach {
            assertNull(it, parseVersion(it))
        }
    }

    @Test fun parseVersionNumericEdgeCases() {
        assertEquals(Version(3, 10, 0), parseVersion("v3.010.0"))
        assertEquals(Version(10, 0, 0), parseVersion("v10.0.0"))
        assertNull(parseVersion("v3.10.2147483648"))
    }

    @Test fun isNewerComparesNumerically() {
        assertTrue(isNewer(Version(3, 10, 0), Version(3, 9, 0)))
        assertTrue(isNewer(Version(3, 10, 1), Version(3, 10, 0)))
        assertTrue(isNewer(Version(4, 0, 0), Version(3, 99, 99)))
        assertFalse(isNewer(Version(3, 10, 0), Version(3, 10, 0)))
        assertFalse(isNewer(Version(3, 9, 9), Version(3, 10, 0)))
    }

    @Test fun pickApkFindsExactNameOnly() {
        assertEquals(apk, pickApk(release().assets, "3.11.0"))
        assertNull(pickApk(release().assets, "3.12.0"))
        assertNull(pickApk(listOf("course-pacer-3.11.0.apk.sha256" to apk), "3.11.0"))
        assertNull(pickApk(emptyList(), "3.11.0"))
    }

    @Test fun toUpdateBuildsFromAGoodRelease() {
        assertEquals(
            Update("3.11.0", "Added\nUpdate check in Settings.\nA dot on the menu.", apk, page),
            toUpdate(release()),
        )
    }

    @Test fun toUpdateRejectsIncompleteReleases() {
        assertNull(toUpdate(release(tag = null)))
        assertNull(toUpdate(release(tag = "latest")))
        assertNull(toUpdate(release(assets = listOf("course-pacer-3.10.0.apk" to apk))))
        assertNull(toUpdate(release(pageUrl = null)))
        assertEquals("", toUpdate(release(body = null))!!.notes)
    }

    @Test fun toUpdateRejectsForeignUrls() {
        assertNull(toUpdate(release(assets = listOf("course-pacer-3.11.0.apk" to "https://example.com/course-pacer-3.11.0.apk"))))
        assertNull(toUpdate(release(assets = listOf("course-pacer-3.11.0.apk" to "http://github.com/tsampolion/course_planner/releases/download/v3.11.0/course-pacer-3.11.0.apk"))))
        assertNull(toUpdate(release(pageUrl = "https://github.com/someone/else/releases/tag/v3.11.0")))
    }

    @Test fun isDueAfterADay() {
        val day = CHECK_INTERVAL_MS
        assertTrue(isDue(0, 1_000))
        assertFalse(isDue(10 * day, 10 * day + day - 1))
        assertTrue(isDue(10 * day, 11 * day))
        assertTrue(isDue(10 * day, 9 * day))
    }

    @Test fun shortNotesStripsMarkdownAndKeepsThreeLines() {
        val md = "### Added\n\n- **Updates**: check in [Settings](https://x.y/z).\n* Second\n1. Third\n- Fourth"
        assertEquals("Added\nUpdates: check in Settings.\nSecond", shortNotes(md))
        assertEquals("", shortNotes(""))
        assertEquals("", shortNotes(null))
    }

    @Test fun shortNotesHandlesCrLfAndImages() {
        val md = "### Added\r\n\r\n- ![shot](https://x.y/a.png) New *badge*\r\n- _Quiet_ `notice`\r\n"
        val out = shortNotes(md)
        assertEquals("Added\nNew badge\nQuiet notice", out)
        listOf("\r", "![", "**", "](", "`").forEach { assertFalse(it, out.contains(it)) }
    }

    @Test fun pendingUpdateKeepsOnlyNewer() {
        val u = Update("3.11.0", "", apk, page)
        assertEquals(u, pendingUpdate(u, "3.10.0"))
        assertNull(pendingUpdate(u, "3.11.0"))
        assertNull(pendingUpdate(u, "3.12.0"))
        assertNull(pendingUpdate(null, "3.10.0"))
        assertNull(pendingUpdate(u.copy(version = "junk"), "3.10.0"))
        assertNull(pendingUpdate(u, "3.11.0-debug"))
    }

    @Test fun storedUpdateNeedsEveryPart() {
        assertEquals(Update("3.11.0", "n", apk, page), storedUpdate("3.11.0", "n", apk, page))
        assertEquals(Update("3.11.0", "", apk, page), storedUpdate("3.11.0", null, apk, page))
        assertNull(storedUpdate(null, "n", apk, page))
        assertNull(storedUpdate("3.11.0", "n", null, page))
        assertNull(storedUpdate("3.11.0", "n", apk, null))
        assertNull(storedUpdate("v3.11", "n", apk, page))
    }

    @Test fun storedUpdateRejectsForeignUrls() {
        assertNull(storedUpdate("3.11.0", "n", "https://example.com/a.apk", page))
        assertNull(storedUpdate("3.11.0", "n", apk, "https://example.com/"))
    }
}
```

- [x] **Step 2: Run the tests to see them fail**

Run: `./gradlew testDebugUnitTest --tests 'gr.edusuite.pacer.update.UpdateModelTest'`
Expected: compilation FAILS with `Unresolved reference: parseVersion` (and others).

- [x] **Step 3: Implement `<main>/update/UpdateModel.kt`**

Rules the tests pin, with the choices they leave open made for you:
- `parseVersion`: trim, then match `^v?(\d+)\.(\d+)\.(\d+)$` (lowercase `v` only). Each part goes through `toIntOrNull()`; any null gives `null`.
- `Version.compareTo` compares major, then minor, then patch. `isNewer(latest, installed) = latest > installed`.
- `pickApk(assets, version)`: the URL of the first asset whose name equals `"course-pacer-$version.apk"` exactly.
- `toUpdate(release)`: `parseVersion(tag)`, otherwise null. `version = "${v.major}.${v.minor}.${v.patch}"`. `apkUrl = pickApk(...)`, which must start with `APK_URL_PREFIX`. `pageUrl` must be non-null and start with `PAGE_URL_PREFIX`. `notes = shortNotes(body)`.
- `isDue(last, now) = last <= 0 || now < last || now - last >= CHECK_INTERVAL_MS`.
- `shortNotes(md)`: the transformations, in this order:
  1. `null` gives `""`. Replace `"\r\n"` and `"\r"` with `"\n"`.
  2. For each line: remove images `!\[[^\]]*]\([^)]*\)`, then turn links `\[([^\]]*)]\([^)]*\)` into `$1`, then remove the leading `#+\s*`, the leading list marker `^\s*([-*+]|\d+\.)\s+`, then the characters `*`, `_` and `` ` ``, then collapse runs of whitespace to one space and trim.
  3. Drop empty lines, `take(NOTE_LINES)`, `joinToString("\n")`.
- `pendingUpdate(stored, installed)`: null unless both versions parse and `isNewer(storedVersion, installedVersion)`. (An installed version like `3.11.0-debug` doesn't parse, so the result is null. Debug builds are switched off anyway.)
- `storedUpdate(...)`: null unless `version` parses as bare `X.Y.Z` (normalise the same way as `toUpdate`), `apkUrl` starts with `APK_URL_PREFIX` and `pageUrl` starts with `PAGE_URL_PREFIX`. A null `notes` becomes `""`.

Put a one-line KDoc on each public declaration, in the house style (see `<main>/ui/HomeModel.kt`).

- [x] **Step 4: Run the tests to see them pass**

Run: `./gradlew testDebugUnitTest --tests 'gr.edusuite.pacer.update.UpdateModelTest'`
Expected: `BUILD SUCCESSFUL`, 14 tests passed.

- [x] **Step 5: Run the whole suite**

Run: `./gradlew testDebugUnitTest`
Expected: `BUILD SUCCESSFUL`.

- [x] **Step 6: Commit**

```bash
git add app/src/main/java/gr/edusuite/pacer/update/UpdateModel.kt app/src/test/java/gr/edusuite/pacer/update/UpdateModelTest.kt
git commit -m "feat: update model for the GitHub release check"
```

---

### Task 2: Settings fields and their storage

**Files:**
- Modify: `<main>/model/Settings.kt` (the `data class Settings` and `sanitised()`, around lines 37-59)
- Modify: `<main>/data/SettingsStore.kt` (all of it, about 60 lines)
- Test: `<test>/ui/SettingsTest.kt` (append tests)

**Interfaces:**
- Consumes: `Update`, `storedUpdate`, `parseVersion` from Task 1.
- Produces, as new `Settings` fields appended after `classOrder`:
  ```kotlin
  /** Check GitHub for a newer release once a day on launch. */
  val autoUpdateCheck: Boolean = true,
  /** Epoch millis of the last successful update check; 0 for never. */
  val lastUpdateCheck: Long = 0L,
  /** The newest release found and not yet installed, or null. */
  val availableUpdate: Update? = null,
  ```
  `sanitised()` additionally does: `lastUpdateCheck = lastUpdateCheck.coerceAtLeast(0)` and `availableUpdate = availableUpdate?.let { storedUpdate(it.version, it.notes, it.apkUrl, it.pageUrl) }`.

- [x] **Step 1: Write the failing tests** (append inside `class SettingsTest` in `<test>/ui/SettingsTest.kt`, and add `import gr.edusuite.pacer.update.Update`)

```kotlin
    @Test fun updateSettingsDefaults() {
        val s = Settings()
        assertTrue(s.autoUpdateCheck)
        assertEquals(0L, s.lastUpdateCheck)
        assertNull(s.availableUpdate)
    }

    @Test fun sanitisedCleansUpdateFields() {
        val good = Update("3.11.0", "n", "https://github.com/tsampolion/course_planner/releases/download/v3.11.0/course-pacer-3.11.0.apk", "https://github.com/tsampolion/course_planner/releases/tag/v3.11.0")
        assertEquals(good, Settings(availableUpdate = good).sanitised().availableUpdate)
        assertNull(Settings(availableUpdate = good.copy(apkUrl = "https://example.com/x.apk")).sanitised().availableUpdate)
        assertNull(Settings(availableUpdate = good.copy(version = "3.11")).sanitised().availableUpdate)
        assertEquals(0L, Settings(lastUpdateCheck = -5).sanitised().lastUpdateCheck)
    }
```

- [x] **Step 2: Run them to see them fail**

Run: `./gradlew testDebugUnitTest --tests 'gr.edusuite.pacer.ui.SettingsTest'`
Expected: compilation FAILS with `No parameter with name 'availableUpdate'`.

- [x] **Step 3: Add the three fields and the sanitising to `Settings`** as specified in Interfaces. `model` importing `gr.edusuite.pacer.update.Update` is intended (both are plain Kotlin).

- [x] **Step 4: Store them in `SettingsStore`**

Add these constants to the private companion, each with a short comment, as the existing ones have:
```kotlin
const val AUTO_UPDATE = "autoUpdateCheck"
const val LAST_UPDATE_CHECK = "lastUpdateCheck"
const val UPDATE_VERSION = "updateVersion"
const val UPDATE_NOTES = "updateNotes"
const val UPDATE_APK = "updateApk"
const val UPDATE_PAGE = "updatePage"
```
- `load()`: `autoUpdateCheck = prefs.getBoolean(AUTO_UPDATE, true)` and `lastUpdateCheck = runCatching { prefs.getLong(LAST_UPDATE_CHECK, 0L) }.getOrDefault(0L)`. For `availableUpdate`, use `storedUpdate(...)` with each of the four strings read through `runCatching { prefs.getString(KEY, null) }.getOrNull()`.
- `save(s)`: `putBoolean(AUTO_UPDATE, …)` and `putLong(LAST_UPDATE_CHECK, …)`. If `s.availableUpdate` is non-null, `putString` all four keys; if it is null, `remove` all four.
- `settings.xml` is already backed up whole by `data_extraction_rules.xml`, so don't change that file.

- [x] **Step 5: Run the tests to see them pass**

Run: `./gradlew testDebugUnitTest --tests 'gr.edusuite.pacer.ui.SettingsTest'`
Expected: `BUILD SUCCESSFUL`.

- [x] **Step 6: Run the whole suite**

Run: `./gradlew testDebugUnitTest`
Expected: `BUILD SUCCESSFUL`.

- [x] **Step 7: Commit**

```bash
git add app/src/main/java/gr/edusuite/pacer/model/Settings.kt app/src/main/java/gr/edusuite/pacer/data/SettingsStore.kt app/src/test/java/gr/edusuite/pacer/ui/SettingsTest.kt
git commit -m "feat: store the update check's settings"
```

---

### Task 3: UpdateChecker (the flow, testable without Android)

**Files:**
- Create: `<main>/update/UpdateChecker.kt`
- Test: `<test>/update/UpdateCheckerTest.kt`

**Interfaces:**
- Consumes: Task 1 (`Release`, `toUpdate`, `isDue`, `pendingUpdate`, `parseVersion`, `isNewer`, `Update`) and Task 2 (`Settings` fields).
- Produces (package `gr.edusuite.pacer.update`):
  ```kotlin
  /** Where the latest release comes from. Null means the request failed for any reason. */
  fun interface ReleaseSource { fun latest(): Release? }

  /** One check's outcome. */
  sealed interface Fetched {
      data class Found(val update: Update) : Fetched
      data object UpToDate : Fetched
      data object Failed : Fetched
  }

  class UpdateChecker(
      private val source: ReleaseSource,
      val installed: String,            // the installed versionName, e.g. "3.11.0"
      val enabled: Boolean,             // false for debuggable builds
      private val now: () -> Long = System::currentTimeMillis,
  ) {
      /** Settings with a stale availableUpdate dropped (installed caught up, or checks disabled). */
      fun atLaunch(settings: Settings): Settings
      /** Whether launch should run a check now. */
      fun shouldAutoCheck(settings: Settings): Boolean
      /** The update to show, or null. Always null when disabled. */
      fun pending(settings: Settings): Update?
      /** Blocking; call on Dispatchers.IO. Never throws. Failed when disabled. */
      fun fetch(): Fetched
      /** Folds a result into the settings current when it lands. Failed changes nothing. */
      fun applied(settings: Settings, fetched: Fetched): Settings
  }
  ```

  Semantics:
  - `atLaunch`: if `!enabled` or `pendingUpdate(settings.availableUpdate, installed) == null`, return a copy with `availableUpdate = null`. Otherwise return `settings` unchanged (the same instance).
  - `shouldAutoCheck = enabled && settings.autoUpdateCheck && isDue(settings.lastUpdateCheck, now())`.
  - `pending = if (enabled) pendingUpdate(settings.availableUpdate, installed) else null`.
  - `fetch`: if not enabled, `Failed`. Otherwise call `runCatching { source.latest() }.getOrNull()`. A null result is `Failed`. If `toUpdate(release)` is non-null and newer than `installed`, `Found`. Anything else (older, equal, no matching APK, unparseable tag) is `UpToDate`.
  - `applied`: `Found` gives `settings.copy(availableUpdate = update, lastUpdateCheck = now())`. `UpToDate` gives `settings.copy(availableUpdate = null, lastUpdateCheck = now())`. `Failed` returns `settings` unchanged.

- [ ] **Step 1: Write the failing tests** in `<test>/update/UpdateCheckerTest.kt`

```kotlin
package gr.edusuite.pacer.update

import gr.edusuite.pacer.model.Settings
import gr.edusuite.pacer.model.ThemeMode
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertSame
import org.junit.Assert.assertTrue
import org.junit.Test

class UpdateCheckerTest {
    private val day = CHECK_INTERVAL_MS
    private var clock = 100 * day
    private var calls = 0
    private fun rel(v: String) = Release(
        "v$v", "- Notes", "https://github.com/tsampolion/course_planner/releases/tag/v$v",
        listOf("course-pacer-$v.apk" to "https://github.com/tsampolion/course_planner/releases/download/v$v/course-pacer-$v.apk"),
    )
    private fun checker(result: Release?, installed: String = "3.10.0", enabled: Boolean = true) =
        UpdateChecker({ calls++; result }, installed, enabled) { clock }
    private val found = toUpdate(rel("3.11.0"))!!

    @Test fun fetchFindsNewer() = assertEquals(Fetched.Found(found), checker(rel("3.11.0")).fetch())
    @Test fun fetchSameOrOlderIsUpToDate() {
        assertEquals(Fetched.UpToDate, checker(rel("3.10.0")).fetch())
        assertEquals(Fetched.UpToDate, checker(rel("3.9.0")).fetch())
    }
    @Test fun fetchWithoutApkIsUpToDate() = assertEquals(Fetched.UpToDate, checker(rel("3.11.0").copy(assets = emptyList())).fetch())
    @Test fun fetchFailureIsFailed() = assertEquals(Fetched.Failed, checker(null).fetch())
    @Test fun fetchSwallowsExceptions() =
        assertEquals(Fetched.Failed, UpdateChecker({ error("boom") }, "3.10.0", true) { clock }.fetch())
    @Test fun disabledNeverFetches() {
        assertEquals(Fetched.Failed, checker(rel("3.11.0"), enabled = false).fetch())
        assertEquals(0, calls)
    }

    @Test fun shouldAutoCheckRules() {
        val c = checker(null)
        assertTrue(c.shouldAutoCheck(Settings()))
        assertFalse(c.shouldAutoCheck(Settings(autoUpdateCheck = false)))
        assertFalse(c.shouldAutoCheck(Settings(lastUpdateCheck = clock - day + 1)))
        assertTrue(c.shouldAutoCheck(Settings(lastUpdateCheck = clock - day)))
        assertFalse(checker(null, enabled = false).shouldAutoCheck(Settings()))
    }

    @Test fun appliedFoundStoresAndStamps() {
        val s = checker(null).applied(Settings(), Fetched.Found(found))
        assertEquals(found, s.availableUpdate)
        assertEquals(clock, s.lastUpdateCheck)
    }
    @Test fun appliedUpToDateClearsAndStamps() {
        val s = checker(null).applied(Settings(availableUpdate = found), Fetched.UpToDate)
        assertNull(s.availableUpdate)
        assertEquals(clock, s.lastUpdateCheck)
    }
    @Test fun appliedFailedChangesNothing() {
        val before = Settings(availableUpdate = found, lastUpdateCheck = 7)
        assertSame(before, checker(null).applied(before, Fetched.Failed))
    }
    @Test fun appliedKeepsOtherSettings() {
        // The teacher changed the theme and turned auto-check off while the request was in flight.
        val current = Settings(theme = ThemeMode.DARK, autoUpdateCheck = false, name = "Anna")
        val s = checker(null).applied(current, Fetched.Found(found))
        assertEquals(ThemeMode.DARK, s.theme)
        assertFalse(s.autoUpdateCheck)
        assertEquals("Anna", s.name)
    }

    @Test fun launchClearsCaughtUpUpdate() {
        assertNull(checker(null, installed = "3.11.0").atLaunch(Settings(availableUpdate = found)).availableUpdate)
        assertNull(checker(null, installed = "3.12.0").atLaunch(Settings(availableUpdate = found)).availableUpdate)
        val keep = Settings(availableUpdate = found)
        assertSame(keep, checker(null, installed = "3.10.0").atLaunch(keep))
        assertNull(checker(null, enabled = false).atLaunch(keep).availableUpdate)
    }

    @Test fun pendingHonoursEnabled() {
        val s = Settings(availableUpdate = found)
        assertEquals(found, checker(null).pending(s))
        assertNull(checker(null, enabled = false).pending(s))
        assertNull(checker(null, installed = "3.11.0").pending(s))
    }
}
```

- [ ] **Step 2: Run them to see them fail**

Run: `./gradlew testDebugUnitTest --tests 'gr.edusuite.pacer.update.UpdateCheckerTest'`
Expected: compilation FAILS with `Unresolved reference: UpdateChecker`.

- [ ] **Step 3: Implement `<main>/update/UpdateChecker.kt`** as specified in Interfaces. No Android imports. A one-line KDoc on each public declaration.

- [ ] **Step 4: Run the tests to see them pass**

Run: `./gradlew testDebugUnitTest --tests 'gr.edusuite.pacer.update.UpdateCheckerTest'`
Expected: `BUILD SUCCESSFUL`, 13 tests passed.

- [ ] **Step 5: Run the whole suite**

Run: `./gradlew testDebugUnitTest`
Expected: `BUILD SUCCESSFUL`.

- [ ] **Step 6: Commit**

```bash
git add app/src/main/java/gr/edusuite/pacer/update/UpdateChecker.kt app/src/test/java/gr/edusuite/pacer/update/UpdateCheckerTest.kt
git commit -m "feat: update checker flow with a pluggable release source"
```

---

### Task 4: GitHub source, permission and ViewModel wiring

**Files:**
- Create: `<main>/update/GitHubReleases.kt`
- Modify: `app/src/main/AndroidManifest.xml`
- Modify: `<main>/PacerApplication.kt`
- Modify: `<main>/MainActivity.kt` (the `viewModels { … }` block, lines 20-23)
- Modify: `<main>/ui/PacerViewModel.kt` (the constructor at line 88, the settings section around lines 290-297, and `Factory` around line 708)

**Interfaces:**
- Consumes: `ReleaseSource`, `Release`, `UpdateChecker`, `Fetched`, `RELEASES_API` (Tasks 1 and 3).
- Produces:
  ```kotlin
  // GitHubReleases.kt
  class GitHubReleases(private val installed: String) : ReleaseSource   // latest() does the GET
  internal fun readRelease(json: String): Release?                      // org.json copy-out; null on any exception

  // PacerViewModel
  enum class UpdateCheckState { IDLE, CHECKING, UP_TO_DATE, FAILED }    // top level in PacerViewModel.kt
  class PacerViewModel(store: PlanStore, settingsStore: SettingsStore, private val updates: UpdateChecker)
  val update: StateFlow<Update?>                 // updates.pending(settings), derived
  val updateCheck: StateFlow<UpdateCheckState>
  val updatesEnabled: Boolean                    // = updates.enabled
  val installedVersion: String                   // = updates.installed
  fun checkForUpdates()                          // Check now
  fun setAutoUpdateCheck(on: Boolean)
  class Factory(store: PlanStore, settings: SettingsStore, updates: UpdateChecker)

  // PacerApplication
  val updates: UpdateChecker
  ```

- [ ] **Step 1: Add the permission.** In `AndroidManifest.xml`, directly inside `<manifest …>` and before the comment above `<application>`:

```xml
    <!-- Only for the once-a-day update check against this app's GitHub releases (4.1.0). -->
    <uses-permission android:name="android.permission.INTERNET" />
```

- [ ] **Step 2: Implement `GitHubReleases.kt`**
- `latest()`: open `URL(RELEASES_API).openConnection() as HttpURLConnection`. Set `connectTimeout = 5000`, `readTimeout = 10000`, `requestMethod = "GET"`, `instanceFollowRedirects = true`, and the headers `Accept: application/vnd.github+json` and `User-Agent: CoursePacer/$installed`. If `responseCode != 200`, return `null`. Read `inputStream` as UTF-8 with a cap of 512 KiB (stop and return `null` past it), then `readRelease(text)`. Wrap the whole thing in `try { … } catch (e: Exception) { null } finally { disconnect() }`.
- `readRelease(json)`: `JSONObject(json)`. Use `optString` with a `null` fallback for `tag_name`, `body` and `html_url` (treat `JSONObject.NULL` and empty strings as `null`). `optJSONArray("assets")` maps each object to `name` paired with `browser_download_url`, skipping entries missing either. The whole function goes inside `runCatching { … }.getOrNull()`.
- This file is not unit-tested, because `org.json` is stubbed on the JVM. Keep it free of any decision that Task 1 or Task 3 owns.

- [ ] **Step 3: Build the checker in `PacerApplication`**

```kotlin
val updates: UpdateChecker by lazy {
    val installed = runCatching { packageManager.getPackageInfo(packageName, 0).versionName }.getOrNull().orEmpty()
    val debuggable = (applicationInfo.flags and ApplicationInfo.FLAG_DEBUGGABLE) != 0
    UpdateChecker(GitHubReleases(installed), installed, enabled = !debuggable && installed.isNotEmpty())
}
```

- [ ] **Step 4: Wire `MainActivity`.** `PacerViewModel.Factory(app.store, app.settings, app.updates)`.

- [ ] **Step 5: Change `PacerViewModel`**
- Constructor: `class PacerViewModel(private val store: PlanStore, private val settingsStore: SettingsStore, private val updates: UpdateChecker)`. Update `Factory` to match.
- Initialise `_settings` with `MutableStateFlow(updates.atLaunch(settingsStore.load()))`. If `atLaunch` returned a different instance, call `settingsStore.save(...)` once.
- `val update: StateFlow<Update?> = settings.map(updates::pending).stateIn(viewModelScope, SharingStarted.Eagerly, updates.pending(_settings.value))`. Declare it **after** `_settings` and `settings`.
- `private val _updateCheck = MutableStateFlow(UpdateCheckState.IDLE)`, exposed as `val updateCheck = _updateCheck.asStateFlow()`.
- In the settings section, next to `setClassOrder`:

```kotlin
fun setAutoUpdateCheck(on: Boolean) = editSettings { it.copy(autoUpdateCheck = on) }

/** Settings → Check now: ignores the daily throttle and the switch. */
fun checkForUpdates() {
    if (!updates.enabled || _updateCheck.value == UpdateCheckState.CHECKING) return
    _updateCheck.value = UpdateCheckState.CHECKING
    viewModelScope.launch {
        val fetched = withContext(Dispatchers.IO) { updates.fetch() }
        editSettings { updates.applied(it, fetched) }
        _updateCheck.value = when (fetched) {
            is Fetched.Found -> UpdateCheckState.IDLE
            Fetched.UpToDate -> UpdateCheckState.UP_TO_DATE
            Fetched.Failed -> UpdateCheckState.FAILED
        }
    }
}
```

- The launch check goes in an `init { }` block placed **after** every property it reads (`_settings`, `_updateCheck`). It must not touch `_updateCheck`, so it stays silent:

```kotlin
init {
    if (updates.shouldAutoCheck(_settings.value)) viewModelScope.launch {
        val fetched = withContext(Dispatchers.IO) { updates.fetch() }
        editSettings { updates.applied(it, fetched) }
    }
}
```

  `editSettings` already sanitises, skips no-op changes and saves at once. `applied` reads the settings current at that moment, which gives the in-flight safety that `appliedKeepsOtherSettings` pins.
- Add the imports you need: `kotlinx.coroutines.flow.map`, `kotlinx.coroutines.flow.stateIn`, `kotlinx.coroutines.flow.SharingStarted`, `kotlinx.coroutines.withContext`, `gr.edusuite.pacer.update.*`. `Dispatchers` and `launch` are already imported.
- `PacerViewModel.kt` is excluded from `NoHardcodedTextTest`. Still, add no user-visible copy here.

- [ ] **Step 6: Compile and run the whole suite**

Run: `./gradlew testDebugUnitTest`
Expected: `BUILD SUCCESSFUL`. No test constructs `PacerViewModel` today. If one does, pass `UpdateChecker({ null }, "0.0.0", enabled = false)`.

- [ ] **Step 7: Commit**

```bash
git add app/src/main/AndroidManifest.xml app/src/main/java/gr/edusuite/pacer/update/GitHubReleases.kt app/src/main/java/gr/edusuite/pacer/PacerApplication.kt app/src/main/java/gr/edusuite/pacer/MainActivity.kt app/src/main/java/gr/edusuite/pacer/ui/PacerViewModel.kt
git commit -m "feat: check GitHub releases once a day on launch"
```

---

### Task 5: The UI (☰ dot, drawer tag, Settings → Updates)

**Files:**
- Modify: `app/src/main/res/values/strings.xml` (append a block after the existing `settings_*` strings)
- Modify: `<main>/ui/NavUi.kt` (new `MenuButton`; the `Screen.Settings ->` branch of `AppDrawer`, about line 166; the `AppDrawer` signature, about line 120)
- Modify: `<main>/ui/PacerApp.kt` (collect at about line 123; `ScreenBar`'s ☰ at about line 483; the `AppDrawer(...)` calls at about lines 317 and 320; `ScreenContent`'s `HomeScreen(...)` call at about line 366)
- Modify: `<main>/ui/HomeScreen.kt` (`HomeScreen` params at about line 101; `Header` at about line 159)
- Modify: `<main>/ui/ClassScreens.kt:372` (`private fun SwitchRow` becomes `internal fun SwitchRow`)
- Create: `<main>/ui/UpdatesUi.kt`
- Modify: `<main>/ui/SettingsScreen.kt` (the first `item` and the `about` item)
- Modify: `<test>/ui/NoHardcodedTextTest.kt` (the `"SettingsScreen.kt"` entry of `dataStrings`)

**Interfaces:**
- Consumes: `vm.update`, `vm.updateCheck`, `vm.updatesEnabled`, `vm.installedVersion`, `vm.checkForUpdates()`, `vm.setAutoUpdateCheck()` and `UpdateCheckState` from Task 4. `Update` comes from Task 1.
- Produces:
  ```kotlin
  // NavUi.kt
  @Composable fun MenuButton(onMenu: () -> Unit, badge: Boolean)
  @Composable fun AppDrawer(data: AppData, screen: Screen, vm: PacerViewModel, onClose: () -> Unit, permanent: Boolean = false, updateBadge: Boolean = false)
  // UpdatesUi.kt
  @Composable fun UpdatesSection(vm: PacerViewModel, toast: (String) -> Unit, modifier: Modifier = Modifier)
  fun openLink(context: Context, url: String, toast: (String) -> Unit)
  // HomeScreen.kt: new parameter on HomeScreen, after onMenu
  menuBadge: Boolean = false
  ```

- [ ] **Step 1: Add the strings** (exact copy):

```xml
    <!-- Updates (4.1.0) -->
    <string name="nav_menu_update">Menu, update available</string>
    <string name="nav_update">Update</string>
    <string name="nav_update_description">Update available</string>
    <string name="settings_updates">Updates</string>
    <string name="updates_available">Version %1$s is available</string>
    <string name="updates_download">Download</string>
    <string name="updates_first_time">The first time, Android may ask you to allow installs from your browser.</string>
    <string name="updates_see_changes">See all changes</string>
    <string name="updates_auto">Check for updates automatically</string>
    <string name="updates_auto_hint">Once a day, Course Pacer asks GitHub for the latest version. Nothing about you or your school years is sent.</string>
    <string name="updates_check_now">Check now</string>
    <string name="updates_checking">Checking…</string>
    <string name="updates_up_to_date">Course Pacer is up to date.</string>
    <string name="updates_failed">Couldn\'t check for updates. Check your connection and try again.</string>
    <string name="updates_installed">Installed version %1$s</string>
    <string name="updates_no_browser">No browser found. The download link was copied.</string>
    <string name="updates_link_label">Course Pacer download link</string>
```

- [ ] **Step 2: Add `MenuButton` to `NavUi.kt`**, next to `WarningButton`, following its pattern:

```kotlin
/** ☰, with a small accent dot while an update is available. TalkBack reads "Menu, update available". */
@Composable
fun MenuButton(onMenu: () -> Unit, badge: Boolean) {
    val c = Pacer.colors
    PillIconButton(stringResource(if (badge) R.string.nav_menu_update else R.string.nav_menu), onMenu) {
        BadgedBox(
            badge = { if (badge) Badge(containerColor = c.accent) },
            modifier = Modifier.clearAndSetSemantics {},
        ) { AppIcon(AppIcons.Menu) }
    }
}
```

  `Pacer.colors.accent` is a `Color` (`ui/theme/Color.kt`). The dot is a fill, which D13 allows on every palette. Don't add a token.

- [ ] **Step 3: Replace both ☰ buttons with `MenuButton`**
  - `PacerApp.kt` `ScreenBar`: `if (showMenu) MenuButton(onMenu, menuBadge)`. Add `menuBadge: Boolean` to `ScreenBar`'s parameters, and pass it from the `bar` lambda.
  - `HomeScreen.kt` `Header`: `MenuButton(onMenu, menuBadge)`. Thread `menuBadge` from `HomeScreen`'s new parameter into `Header`.
  - `PacerApp.kt`: after `val settings by vm.settings.collectAsState()`, add `val update by vm.update.collectAsState()`. Pass `menuBadge = update != null` to `ScreenBar` and, through `ScreenContent` (add a `menuBadge: Boolean = false` parameter), to `HomeScreen`. Pass `updateBadge = update != null` to both `AppDrawer(...)` calls.

- [ ] **Step 4: Add the drawer tag.** In `AppDrawer`, the `Screen.Settings ->` branch becomes:

```kotlin
Screen.Settings -> {
    val description = stringResource(R.string.nav_update_description)
    DrawerRow(stringResource(R.string.app_settings), AppIcons.Settings, screen == s, { go { vm.goTo(s) } }) {
        if (updateBadge) Tag(stringResource(R.string.nav_update), Modifier.semantics { contentDescription = description })
    }
}
```

- [ ] **Step 5: Make `SwitchRow` reusable.** In `ClassScreens.kt`, change `private fun SwitchRow` to `internal fun SwitchRow`. Nothing else changes.

- [ ] **Step 6: Create `UpdatesUi.kt`.** It holds no string literals with letters, so everything goes through `stringResource` or `context.getString`. Layout, top to bottom, inside the existing `Section(title, modifier) { … }` helper from `Components.kt`, titled `R.string.settings_updates`:
  1. If `vm.update` is non-null:
     - `Text(stringResource(R.string.updates_available, u.version), style = MaterialTheme.typography.titleMedium)`
     - if `u.notes` is not blank: `Text(u.notes, style = MaterialTheme.typography.bodyMedium)`
     - `PrimaryButton(stringResource(R.string.updates_download), { openLink(context, u.apkUrl, toast) }, Modifier.fillMaxWidth())`
     - `Hint(stringResource(R.string.updates_first_time))`
     - `TertiaryButton(stringResource(R.string.updates_see_changes), { openLink(context, u.pageUrl, toast) })`
  2. `SwitchRow(stringResource(R.string.updates_auto), settings.autoUpdateCheck, vm::setAutoUpdateCheck)`, then `Hint(stringResource(R.string.updates_auto_hint))`. Read `settings` with `vm.settings.collectAsState()`.
  3. `SecondaryButton(stringResource(R.string.updates_check_now), vm::checkForUpdates, enabled = check != UpdateCheckState.CHECKING)`. Under it, by state: `CHECKING` shows `Hint(updates_checking)`, `UP_TO_DATE` shows `Hint(updates_up_to_date)`, `FAILED` shows `ErrorBox(stringResource(R.string.updates_failed))` (`Components.kt:465`, already contrast-checked), and `IDLE` shows nothing. Wrap the result line in a `Box(Modifier.semantics { liveRegion = LiveRegionMode.Polite })` so TalkBack announces it (imports: `androidx.compose.ui.semantics.liveRegion` and `androidx.compose.ui.semantics.LiveRegionMode`).
  4. `Text(stringResource(R.string.updates_installed, vm.installedVersion), style = MaterialTheme.typography.bodyMedium.merge(Figures))`.

  `openLink(context, url, toast)`:
  ```kotlin
  /** Opens [url] in the browser; with no browser, copies it and says so. */
  fun openLink(context: Context, url: String, toast: (String) -> Unit) {
      try {
          context.startActivity(Intent(Intent.ACTION_VIEW, Uri.parse(url)).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK))
      } catch (e: ActivityNotFoundException) {
          val clipboard = context.getSystemService(ClipboardManager::class.java)
          clipboard?.setPrimaryClip(ClipData.newPlainText(context.getString(R.string.updates_link_label), url))
          toast(context.getString(R.string.updates_no_browser))
      }
  }
  ```

  Large text: `PrimaryButton` and `SecondaryButton` already reflow. Don't add `largeText` branches.

- [ ] **Step 7: Put the section in `SettingsScreen.kt`**
  - As the first item of the `LazyColumn`, before `item(key = "name")`:
    ```kotlin
    if (vm.updatesEnabled) item(key = "updates") { UpdatesSection(vm, toast, Modifier.animateItem()) }
    ```
  - In the `about` item, show the version line only when updates are off (debug builds), because otherwise it now lives in Updates: `if (!vm.updatesEnabled) Text(version, …)`.
  - In `NoHardcodedTextTest.kt`, add `"updates"` to the `"SettingsScreen.kt"` set in `dataStrings`.

- [ ] **Step 8: Run the whole suite and the release build**

Run: `./gradlew testDebugUnitTest`
Expected: `BUILD SUCCESSFUL` (this includes `NoHardcodedTextTest` and `ContrastTest`).

Run: `./gradlew assembleRelease`
Expected: `BUILD SUCCESSFUL`. The APK is debug-signed locally, which is expected.

Run: `./gradlew lint`
Expected: no new errors. A new warning about `HttpURLConnection` on the main thread must not appear; if it does, the call path is wrong, so fix it.

- [ ] **Step 9: Commit**

```bash
git add app/src/main/res/values/strings.xml app/src/main/java/gr/edusuite/pacer/ui/ app/src/test/java/gr/edusuite/pacer/ui/NoHardcodedTextTest.kt
git commit -m "feat: quiet update notice on the menu and in Settings"
```

---

### Task 6: Manual check on a device or emulator

No code, unless a step fails. In that case, fix it in the file that owns the behaviour, re-run Task 5 Step 8, and commit as `fix: …`.

- [ ] **Step 1:** In `app/build.gradle.kts`, temporarily set `versionName = "3.9.0"` (**do not commit this**). The latest published release is 4.0.0 at the time of writing (check the Releases page), so 3.9.0 sees an update.
- [ ] **Step 2:** `./gradlew assembleRelease`, then `adb install -r app/build/outputs/apk/release/app-release.apk` on an emulator or phone **without** a store-signed Course Pacer (a local build can't install over a release). Expected: the install succeeds.
- [ ] **Step 3:** Launch with the network on. Expected, within about 10 s: a dot on ☰; in the menu, Settings shows an "Update" tag; Settings → Updates reads "Version 4.0.0 is available" (or whichever release is latest) followed by notes. Check with TalkBack on: ☰ reads "Menu, update available".
- [ ] **Step 4:** Tap **Download**. Expected: the browser starts downloading `course-pacer-4.0.0.apk`. (The install will fail here because the signatures differ. That is expected, and it's the debug-build caveat the docs describe.)
- [ ] **Step 5:** Turn on airplane mode, then tap **Check now**. Expected: "Checking…", then "Couldn't check for updates. Check your connection and try again." The badge stays.
- [ ] **Step 6:** Kill and relaunch the app (still offline). Expected: the badge still shows, with no network needed.
- [ ] **Step 7:** Restore the committed `versionName` (`git checkout app/build.gradle.kts`) and rebuild. Turn airplane mode off and tap **Check now**. Expected: "Course Pacer is up to date." and no badge.
- [ ] **Step 8:** `./gradlew installDebug`, then launch "Course Pacer (debug)". Expected: no dot, no Updates section, and the version shows in About.
- [ ] **Step 9:** `git status` shows `app/build.gradle.kts` unchanged (`git checkout app/build.gradle.kts` if it isn't). Note the results of steps 3-8 for the PR description.

---

### Task 7: Docs, release contract and version

**Files:**
- Create: `docs/UPDATES_BRIEF.md`
- Modify: `.github/workflows/release.yml` (at the `cp "$f" "course-pacer-$VERSION.apk"` line)
- Modify: `README.md`, `CLAUDE.md`, `docs/README.md`, `CHANGELOG.md`, `app/build.gradle.kts`

- [ ] **Step 1: Write `docs/UPDATES_BRIEF.md`.** Start it with the banner line `Built in 4.1.0. Spec: superpowers/specs/2026-10-23-in-app-update-check-design.md.` Sections:
  - **Why**: not on the Play Store; the owner sets up a few colleagues' phones once, and after that they update themselves.
  - **What the app relies on** (a contract with every installed copy): the public repo `tsampolion/course_planner`; `releases/latest`; the tag `vX.Y.Z`; exactly one asset `course-pacer-X.Y.Z.apk` signed with the permanent key; the release body is the changelog section. Renaming the asset, changing the tag format or making the repo private silently stops in-app updates for everyone.
  - **Behaviour**: once a day on launch while the switch is on; Check now; the ☰ dot, the drawer tag and Settings → Updates; Download opens the browser; nothing to dismiss; debug builds never check.
  - **Privacy**: one GET to `api.github.com` with the app version in the User-Agent; no other data; turn it off in Settings. The app's only permission is `INTERNET`.
  - **Setting up a colleague's phone** (numbered steps): open the Releases page in the phone's browser, download the latest `course-pacer-X.Y.Z.apk`, allow installs from the browser when asked, install, and open Course Pacer. After that, updates show up in Settings.
  - **A phone running a locally built copy**: "App not installed" means it is debug-signed. Export the school years (Settings → Back up), uninstall, install the release, then import.
  - **Not built**: in-app download and install, notifications, background checks, pre-release channels, skipping a version.
- [ ] **Step 2: Add the contract comment to `release.yml`.** Directly above the `cp "$f" "course-pacer-$VERSION.apk"` line, keeping its indentation:

```yaml
            # Installed apps look for exactly this asset name on the vX.Y.Z release (docs/UPDATES_BRIEF.md); don't rename it.
```

- [ ] **Step 3: Update `README.md`**
  - Under **Features**, after the **Settings** bullet: `- **Updates**: Course Pacer checks GitHub for a new version once a day and marks ☰ and Settings when one is out. Settings → Updates shows what changed; **Download** fetches it in your browser and Android updates the app, keeping your school years. Turn the daily check off, or check now, in Settings.`
  - After the paragraph that starts "School years and settings are saved on the device", add: `The app's only network use is the update check: one request to api.github.com, once a day while automatic checks are on or when you tap Check now. It sends no data about you or your school years.`
  - Add a section `## Installing on a phone` before `## Getting started`, with the colleague setup steps from the brief (numbered, 5 steps at most) and a link to `docs/UPDATES_BRIEF.md`.
- [ ] **Step 4: Update `CLAUDE.md`**
  - In **Architecture**, add a bullet after `engine/`: `` - `update/`: the in-app update check (4.1.0, `docs/UPDATES_BRIEF.md`). `UpdateModel.kt` and `UpdateChecker.kt` are plain Kotlin and unit-tested (version parsing, release validation against this repo's release URLs, the once-a-day throttle, note trimming, folding a result into `Settings`); `GitHubReleases.kt` is the only network code (`HttpURLConnection`, `org.json`, one GET to `releases/latest`). `PacerViewModel` checks on launch and on Check now; `Settings` holds `autoUpdateCheck`, `lastUpdateCheck` and `availableUpdate`. Debuggable builds never check. The ☰ dot is `MenuButton` (`NavUi.kt`); Settings → Updates is `UpdatesSection` (`UpdatesUi.kt`). ``
  - In **Rules**, add: `` - **The release is a contract with installed apps** (4.1.0). Installed copies look for tag `vX.Y.Z` and the asset `course-pacer-X.Y.Z.apk` on `releases/latest` of this public repository. Don't rename the asset, change the tag format or make the repository private without planning how installed apps will update. The app's only permission is `INTERNET`, used only for that check. ``
- [ ] **Step 5: Update `docs/README.md`.** Under **Decision records**, after the `BACKUP_BRIEF.md` line: `` - [UPDATES_BRIEF.md](UPDATES_BRIEF.md): the in-app update check against GitHub releases, its release contract and colleague setup (4.1.0). ``
- [ ] **Step 6: Bump the version.** In `app/build.gradle.kts`, set `versionCode = 49` and `versionName = "4.1.0"`.
- [ ] **Step 7: Add the changelog entry.** In `CHANGELOG.md`, insert the section directly under `## [Unreleased]`, leaving one blank line after that heading:

```markdown
## [4.1.0] - YYYY-MM-DD

### Added

- Update check: once a day, Course Pacer asks GitHub for the latest release. When a newer one is out, ☰ shows a dot, Settings in the menu shows an "Update" tag, and Settings → Updates shows the version and what changed. **Download** fetches the APK in your browser and Android updates the app in place, keeping your school years. Turn the daily check off, or check now, in Settings. Debug builds never check.

### Changed

- The app now has the `INTERNET` permission, used only for the update check. It sends nothing but the request itself.
```

  Replace `YYYY-MM-DD` with today's date. At the bottom, above the `[4.0.0]: …` line, add `[4.1.0]: https://github.com/tsampolion/course_planner/compare/v4.0.0...v4.1.0`.
- [ ] **Step 8: Run the final verification**

Run: `./gradlew testDebugUnitTest && ./gradlew assembleRelease`
Expected: `BUILD SUCCESSFUL` for both.

Run: `grep -n 'versionName\|versionCode' app/build.gradle.kts`
Expected: `versionCode = 49` and `versionName = "4.1.0"`.

Run: `grep -n '^## \[4.1.0\]\|^\[4.1.0\]:' CHANGELOG.md`
Expected: two matching lines.

- [ ] **Step 9: Commit and push**

```bash
git add docs/UPDATES_BRIEF.md .github/workflows/release.yml README.md CLAUDE.md docs/README.md CHANGELOG.md app/build.gradle.kts
git commit -m "docs: update check brief, release contract and 4.1.0"
git push origin claude/dreamy-carson-ms25q9
```

  Then update the description of `tsampolion/course_planner#50`: drop the "spec only, don't merge" status, summarise what was built, list the manual-check results from Task 6, and keep the existing footer lines.

---

## Done when

- `./gradlew testDebugUnitTest` and `./gradlew assembleRelease` pass on the branch head, and CI (`Android` workflow) is green on PR #50.
- The manual check in Task 6 passed and its results are in the PR description.
- `git diff origin/main --stat` touches only the files listed in **File Structure**, plus the spec and this plan.
