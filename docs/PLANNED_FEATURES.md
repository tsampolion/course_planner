# Planned features

Features that have been discussed and specified but not built yet. Each entry describes the problem, a proposed design that fits the app's rules, a worked example, and the questions to settle before building it.

How to use this file:

- **Nothing here is approved yet.** Build an entry only when the owner asks for it in a session. Several entries change the pacing engine, which `CLAUDE.md` treats as a contract ("Engine parity is the contract"), so they need explicit sign-off.
- When an entry is picked up, ask its open questions first, write a `docs/<NAME>_BRIEF.md` with the answers folded in (as `docs/LESSON_PAGES_BRIEF.md` does), and change the entry's status here to point to that brief and the version that shipped it.

## 1. Focus blocks: one book on its own for a run of lessons

**Status:** proposed (discussed while building 1.5.0). Not approved. It's an engine extension.

### The problem

A teacher wants to teach one topic straight through from one book, then go back to all the books. For example: the present perfect tenses take three units of the grammar book (12 pages). The teacher wants to finish those 12 pages first, teaching nothing from the Student's book or the Workbook, and then carry on with all three books at a regular pace.

### What the app does today

- The schedule gives every book pages in every lesson, so it can't plan a run of lessons for one book.
- **Workaround since 1.5.0:** after each focus lesson, long-press it, choose "Edit pages taught…", type the grammar pages and clear the other books. Those grammar pages leave the schedule and the other books' planned pages go back to be taught later. Once the block is over, the schedule is right.
- Limits of the workaround:
  - **No look-ahead.** During the block, the lessons still to come keep showing pages from the other books. The pace, the heaviest lesson and the verdict don't know that more focus lessons are coming.
  - **Typing every lesson.** Each focus lesson takes clearing a field per book and typing the grammar pages.
- **The finish-by date doesn't help.** It applies to the whole book, so the grammar book's remaining pages would all be squeezed in before the date. The other books also keep getting pages during those lessons.

### Proposal

A **focus block** says: "for these lessons, teach only this book, these pages". Everything else waits until the block is over.

A block has:

- a **group** (or every group in the class; see the open questions),
- a **book**,
- a **page range** (e.g. 40–51),
- **when it runs**: a first lesson and a number of lessons (the recommended default) or an end date.

### Pacing rules

The allocation rule doesn't change: within each book, lesson *i* of *m* ends at `round(i × pages / m)` (`allocatePages`). The extension only changes **which lessons each book's pages are spread over**. The engine already gives each book its own lesson count: a finish-by date limits a book to the lessons on or before it (`bookLessons`).

1. **Block lessons.** These are the group's teaching lessons inside the block: lessons from `lessonsFor` that a test doesn't take whole.
2. **Chunk.** These are the focus book's pages left to teach (after skipped pages, homework and pages already taught) that fall in the block's page range.
3. **During the block,** only the focus book is taught. The chunk is spread over the block's lessons by `allocatePages`. Every other book gets nothing in those lessons.
4. **Outside the block,** every other page (the other books, and the focus book's pages outside the chunk) is spread over the teaching lessons outside any block, exactly as today. A finish-by date counts only the lessons outside blocks that fall on or before it.
5. **Page order.** The focus book's pages outside the chunk keep their page order. If the chunk isn't the next pages (the teacher jumps ahead to pages 40–51 while the book's position is 30), pages 30–39 are taught after the block, before 52 onwards (see the open questions).
6. **No blocks, no change.** A plan without blocks must schedule exactly as now, row for row. That's the parity test.
7. **Done lessons.** Ticking a block lesson stores its pages as usual (`markDone`). Recording other pages with "Edit pages taught…" works too. Taught chunk pages leave the chunk, and what's left of the chunk re-spreads over the block's remaining lessons. A block whose lessons are all done no longer affects anything.
8. **Block dates must not slide.** Done lessons drop out of `lessonsFor`, so "3 lessons from 1 March" must not be re-counted from the lessons that remain. Otherwise ticking the first block lesson would push the block one lesson later. The simplest fix is to store the block as dates. The add dialog asks for a number of lessons, works out the last date from the group's calendar at that moment, and stores the first and last dates. A day off added later then shortens the block instead of moving it, and the block screen shows "3 lessons, 1–8 Mar" so this is visible.

### Worked example

30 lessons left, sustainable pace 5, no tests. Books: Student's pp. 1–60, Workbook pp. 1–30, Grammar pp. 40–99 (60 pages). Block: Grammar pp. 40–51, the first 3 lessons.

| Lesson | Student's | Workbook | Grammar | Note |
| --- | --- | --- | --- | --- |
| 1 | — | — | 40–43 | focus |
| 2 | — | — | 44–47 | focus |
| 3 | — | — | 48–51 | focus |
| 4 | 1–2 | 1 | 52–53 | |
| 5 | 3–4 | 2 | 54–55 | |
| 6 | 5–7 | 3 | 56 | |
| … | | | | |
| 30 | 59–60 | 30 | 98–99 | |

- After the block, Student's spreads 60 pages over 27 lessons (2.22 a lesson), Workbook 30 over 27 (1.11) and Grammar 48 over 27 (1.78): 5.11 pages a lesson, against 5.00 without the block. The block lessons carry 4 pages each.
- Without the block, lesson 1 would be Student's 1–2, Workbook 1, Grammar 40–41, and lesson 30 is the same in both.
- The overall required pace is unchanged (150 pages over 30 lessons = 5.00), because the block only moves pages around. The summary should add a line such as "After the focus block: 5.11 a lesson over 27 lessons", so the higher load later is visible before the block starts.

### Where it shows

- **Class screen:** a "Focus blocks" link next to Tests & worksheets and Homework, opening a list of blocks with an add dialog. The dialog takes group, book, pages from–to, first lesson (date picker, defaulting to the group's next lesson) and number of lessons. The number is pre-filled from the class pace (12 pages at a sustainable 4 a lesson = 3 lessons) and can be changed.
- **Group schedule:** block lessons carry a "Focus: Grammar" tag under the date, like tests, and the other books' cells show "—". Long-press could offer "Focus on one book from here…" as a shortcut to the add dialog.
- **Summary card:**
  - a hint such as "Lessons 1–3 focus on Grammar pp. 40–51",
  - the "after the block" pace line above,
  - the Positions table's pace column showing each book's pace outside blocks.
- **Copy, Share, Print:** block lessons show only the focus book, with "[Focus: Grammar]" in the first book column the way test names appear in the TSV.
- **Problems list** (`Plan.problems()`, also in Overview):
  - a block whose dates hold no lesson for its group (all closed, after the term ends, or every lesson taken by a test). The chunk then has nowhere to go; it should land in the first lesson after the block, as a missed finish-by date does today, with a warning.
  - a page range outside the book, or with no pages left to teach in it.
  - a chunk that needs more pages a lesson than the sustainable pace (12 pages over 2 lessons is 6 a lesson against a pace of 4).
  - a block that ends after the focus book's finish-by date.
- **Overlapping blocks** for the same group are refused by the add dialog (see the open questions). Back-to-back blocks are fine: Grammar for 3 lessons, then Workbook for 2.

### Data and code

- **Model** (`model/Model.kt`): `data class FocusBlock(id, groupId, bookId, from: Int, to: Int, start: LocalDate, end: LocalDate)`. `groupId` may be `ALL_GROUPS` if blocks can cover the whole class. `SchoolClass.blocks` and `Plan.blocks`, passed through by `SchoolYear.planFor`.
- **Storage** (`data/JsonCodec.kt`): `JsonCodec.VERSION` = 5 with a `blocks` list on each class; a missing list reads as none. Version 1–4 files and the web page's export keep loading. `data_extraction_rules.xml` needs no change (same file).
- **Engine** (`engine/Pacer.kt`):
  - Generalise the per-book allocation in `GroupReport.rows`. Today each book is spread over the first *m* teaching lessons (*m* from `bookLessons`) and padded with empty lessons. Instead, each book (and each chunk) gets an explicit list of teaching-lesson indices, its pages are spread over that list's length, and the result is mapped back to rows.
  - `BookStats` needs a clear meaning per book. Proposal: pages and lessons outside blocks, with the chunk reported separately, so the Positions pace column still means "pages a lesson from now on".
  - `afterNextLesson` (the web page's rule, used by the parity tests) stays as is.
- **View model** (`ui/PacerViewModel.kt`):
  - `addBlock` and `removeBlock` go through `editClass` with no tag, since they're structural edits.
  - `removeBook` and `removeGroup` also drop their blocks.
  - "Reset all positions" keeps blocks, because they are plans, not progress (open question).
- **UI:** a `Screen.Blocks` entry in `Nav.kt` (and its `NavTest` cases), the list and add dialog (in `ClassScreens.kt` or a new file), tags in `GroupScreen.kt`, and the block note in `Format.kt` (TSV) and `PrintSheet.kt`.
- **Version:** MINOR (1.6.0 or whichever is next). It keeps existing plans working, and schedules without blocks don't change.

### Tests to write (`PacerTest`)

- **`noBlocksMatchesTheWebPage`:** the seed plan and the existing fixtures schedule exactly as before.
- **`blockTeachesOnlyItsBook`:** the worked example above, row for row.
- **`blockRestMatchesSkippingTheBlockLessons`:** the lessons outside the block equal the schedule of a plan whose positions sit after the chunk and whose block dates are closed, as a cross-check.
- **`blockWithATestThatTakesTheLesson`:** the test lesson isn't a block lesson, and the chunk spreads over the rest.
- **`blockAndFinishByDate`:** a finish-by date counts only the lessons outside blocks.
- **`tickingInsideABlock`:** the block doesn't slide, taught chunk pages leave the chunk, and the rest of the chunk re-spreads. Recording other pages in a block lesson (1.5.0's `recordLesson`) behaves the same way.
- **`blockWithNoLessonsWarns`:** the chunk lands in the next lesson after the block, and `problems()` says so.
- **Chunk not next:** pages before the chunk come after the block, in page order.
- **JSON:** a version-5 round trip, and version-4 files reading as no blocks. This needs the same JVM `org.json` workaround as before, or a check on a device.

### Open questions (ask before building)

1. **Block length:** a number of lessons stored as dates (recommended), an end date, or worked out from the sustainable pace?
2. **Scope:** one group, a choice of groups, or the whole class? Per group fits the model best, since each group has its own lessons and positions.
3. **Overlaps:** refuse overlapping blocks for the same group (recommended), or let the later block win?
4. **Chunk not next:** when the chunk isn't the next pages of the book, should the pages before it come after the block (recommended), or count as skipped?
5. **Other books during the block:** nothing at all (recommended; homework already covers a little Workbook practice), or allow a small share?
6. **After the block:** spread the other books evenly over the lessons left, as proposed, or catch up first? Catching up would need a new allocation rule.
7. **"Reset all positions":** keep blocks (recommended) or clear them too?
8. **Display:** is a "Focus: Grammar" tag with "—" in the other columns enough, or should block lessons be shaded like test lessons?
9. **Relation to pinned lessons:** fixing the pages of a single future lesson was left out of 1.5.0 (`docs/LESSON_PAGES_BRIEF.md`, "Out of scope"). A one-lesson focus block covers part of that idea. Is that enough, or is a separate "plan this lesson's pages" still wanted?

## 2. Permanent release signing key: every release installs over the last

**Status:** in progress. The open questions were answered on 2026-10-15 and folded into [`docs/SIGNING_BRIEF.md`](SIGNING_BRIEF.md), which also says who does each step. The code change (Phase 2) is in 1.5.2. The key, the secrets and the checks that need the key (Phases 1 and 3 to 6) are the owner's, on the laptop. It's a build and CI change, with no change to the app's code or the engine. Mark it shipped only after Phase 6.

### The problem

Android installs an update only if it's signed with the same certificate as the version already on the phone. Otherwise it refuses with "App not installed" or "package conflicts with an existing package". The only way past that is to uninstall first, and **uninstalling deletes the app's data**: every school year in `plans.json`, and the settings. The app has no export, and Android backup can't be relied on to bring the data back, because restores are tied to the signing certificate.

### What the app does today

- `app/build.gradle.kts` signs the release build with `signingConfigs.getByName("debug")`.
- CI (`.github/workflows/android.yml`) runs on a fresh GitHub runner every time. Each runner creates its own debug keystore, so **every release is signed with a different certificate**. This was checked on the published release APKs (SHA-256 of the v2 signing certificate, first 16 hex digits):

  | Release | Signing certificate |
  | --- | --- |
  | 1.3.0 | `2a1a02fcb4b46f68…` |
  | 1.4.0 | `dcaaba0168c031c4…` |
  | 1.4.1 | `ad4c687600047414…` |
  | 1.4.2 | `aade3225e8055aa4…` |
  | 1.5.0 | `7b5b0adbee3ed0bd…` |

- So no release can be installed over an earlier one. Each one needs an uninstall, which deletes the data.
- Builds from Android Studio (`installDebug`, or `assembleRelease` on the laptop) are signed with the laptop's own debug key, which differs again. They have the same `applicationId`, so they clash with installed releases too.

### Goal

- Every release APK that CI builds on `main` is signed with **one permanent key**. Each new release installs as an update and keeps the school years.
- CI can never publish a release signed with any other key. A missing or wrong key fails the build on `main`.
- Local builds keep working without the key and fall back to the debug key, as now.
- The key exists only on the owner's laptop, in the owner's backups and in the repository's Actions secrets. It is never committed, and never pasted into a chat, an AI session, an issue or a pull request.

### Ground rules for the session

- **Only the owner handles the key and its password.** The AI session writes the Gradle and CI changes that *read* the key from environment variables, and checks the result. It never sees, generates, stores or transmits the key or the password. (A cloud session already refused to generate the key, which is the right outcome.)
- Run the session in Claude Code **on the laptop**, where the Android SDK, `apksigner` and `adb` are available. The cloud environment blocks `dl.google.com`, so it can't run Gradle without its network settings being changed.
- No corners cut: every phase ends with its check, and a check that fails stops the process until it's understood.

### The process

#### Phase 0: decide (at the start of the session)

Answer the open questions below. The answers go into `docs/SIGNING_BRIEF.md`.

#### Phase 1: the owner creates and secures the key (laptop, about 15 minutes)

1. **Find `keytool`.** It comes with any JDK 17 or newer. Android Studio bundles one:
   - macOS: `/Applications/Android Studio.app/Contents/jbr/Contents/Home/bin/keytool`
   - Windows: `C:\Program Files\Android\Android Studio\jbr\bin\keytool.exe`
   - Linux: `<android-studio>/jbr/bin/keytool`
2. **Create the key** in a folder **outside the repository**:

   ```sh
   keytool -genkeypair -v \
     -keystore course-pacer-release.p12 -storetype PKCS12 \
     -alias release -keyalg RSA -keysize 4096 -sigalg SHA256withRSA \
     -validity 10000 -dname "CN=Course Pacer"
   ```

   - Type the password when asked; don't pass `-storepass` on the command line, where it would land in the shell history.
   - Use a long random password from a password manager. A PKCS12 file uses the same password for the store and the key.
   - 10,000 days runs to about 2053. Google Play would require a certificate valid past 22 October 2033, so this also covers a later move to Play.
3. **Record the certificate fingerprint.** It isn't secret.

   ```sh
   keytool -list -v -keystore course-pacer-release.p12 -alias release | grep SHA256
   ```

   Write it down in two forms: as printed (`AB:CD:…`), and in lowercase without colons (`abcd…`), which is how `apksigner` prints it and how the CI check compares it.
4. **Back up the key in two places**, for example the password manager (the `.p12` file as an attachment, plus the password) and an encrypted USB stick kept offline.
   - **Test the backup**: copy the `.p12` back from each place and run the `keytool -list` command above with the password.
   - **Losing the key means every phone has to uninstall again.** A leaked key would let someone else sign an "update" to the app.
5. **Add the key to GitHub.** The GitHub CLI avoids the clipboard:

   ```sh
   gh auth status
   base64 < course-pacer-release.p12 | tr -d '\n' | gh secret set SIGNING_KEYSTORE_B64 --repo tsampolion/course_planner
   gh secret set SIGNING_PASSWORD --repo tsampolion/course_planner      # prompts; paste the password
   gh variable set SIGNING_CERT_SHA256 --repo tsampolion/course_planner --body "<fingerprint, lowercase, no colons>"
   gh secret list --repo tsampolion/course_planner
   gh variable list --repo tsampolion/course_planner
   ```

   - On Windows, run the first line in PowerShell as `[Convert]::ToBase64String([IO.File]::ReadAllBytes((Resolve-Path course-pacer-release.p12))) | gh secret set SIGNING_KEYSTORE_B64 --repo tsampolion/course_planner`.
   - Without the CLI, go to the repository's Settings → Secrets and variables → Actions. Add the two secrets under **Secrets** and the fingerprint under **Variables**, then clear the clipboard.
   - The fingerprint is a *variable*, not a secret, so it shows in logs and can be compared openly.
6. **Check:** `gh secret list` shows `SIGNING_KEYSTORE_B64` and `SIGNING_PASSWORD`, and `gh variable list` shows `SIGNING_CERT_SHA256`. The `.p12` file is not inside the repository folder (`git status` stays clean).

#### Phase 2: the code change (AI session, one pull request)

1. **`app/build.gradle.kts`:**
   - Create a `release` signing config only when the `SIGNING_KEYSTORE` environment variable is set.
   - Read it with `providers.environmentVariable(…)`, which the configuration cache (on in `gradle.properties`) tracks correctly.
   - `storeFile = file(<that path>)`, `keyAlias = "release"`, and `storePassword` and `keyPassword` from `SIGNING_PASSWORD`. Use `.get()`, so a keystore without a password fails loudly instead of signing with nothing.
   - The release build type uses `signingConfigs.findByName("release") ?: signingConfigs.getByName("debug")`.
   - Update the comment above it: CI signs with the permanent key, and local builds use the debug key.
2. **`.github/workflows/android.yml`, in the "Release APK" step:**
   - Pass `SIGNING_KEYSTORE_B64` and `SIGNING_PASSWORD` from `secrets` as step environment variables.
   - If the keystore secret is present, decode it into `$RUNNER_TEMP/release.p12` and export `SIGNING_KEYSTORE`. Strip spaces and newlines first (`tr -d ' \r\n' | base64 -d`), so a pasted value with line breaks still decodes.
   - If it's missing and `GITHUB_REF` is `refs/heads/main`, fail with an `::error::` that names the missing secret. A debug-signed APK must never reach a release.
   - Pull requests from forks get no secrets. They fall back to the debug key and never publish.
3. **`.github/workflows/android.yml`, a new step "Check the signing certificate" after the build:**
   - Run `apksigner verify --print-certs` on the APK, using the newest `$ANDROID_HOME/build-tools/*/apksigner` on the runner.
   - Write `Signer #1 certificate SHA-256 digest` to the job summary.
   - **Fail** if the key was used (on `main` always, and on any other branch that had the secret) and the digest isn't `vars.SIGNING_CERT_SHA256`. This catches a wrong file or wrong secret before anything is published.
4. **`.github/workflows/release.yml`:** no change is needed. It already attaches the APK that the Android workflow built for the same commit. If open question 5 is answered yes, append "Signed with certificate SHA-256 `…`" to the release notes.
5. **`.gitignore`:** add `*.p12` next to `*.jks` and `*.keystore`.
6. **Docs:**
   - `CLAUDE.md`: the `assembleRelease` comment under Commands, and the Gotchas line "The release build is signed with the debug key on purpose".
   - `README.md`: the signing paragraph under "Build and run", and "Continuous integration", which gains the two secrets, the variable and the check.
   - This entry's status points to the brief and the version.
   - `CHANGELOG.md`: a section whose first line says that installing this release needs **one last uninstall**, which deletes the school years on that phone, and that every release after it installs as an update.
7. **Version:** PATCH under the `CHANGELOG.md` rules (a build and CI change that leaves the app working as before), unless open question 2 decides otherwise.

#### Phase 3: verify before merging

1. On the laptop, **without** the variables: run `./gradlew testDebugUnitTest assembleRelease lint`. All must pass, and `apksigner verify --print-certs app/build/outputs/apk/release/*.apk` shows the laptop's debug certificate. This proves the fallback.
2. On the laptop, **with** the key:
   - Run `read -s SIGNING_PASSWORD; export SIGNING_PASSWORD SIGNING_KEYSTORE=/path/to/course-pacer-release.p12`, then `./gradlew assembleRelease`.
   - `apksigner` must show the recorded fingerprint.
   - Afterwards, `unset SIGNING_PASSWORD SIGNING_KEYSTORE`.
3. **Test the `main` guard's failure path**: run the step's shell logic locally with `GITHUB_REF=refs/heads/main` and an empty `SIGNING_KEYSTORE_B64`. It must exit non-zero with the error message. With a branch ref, it must fall through to the debug key.
4. On the pull request, CI must be green, and the job summary must show the recorded fingerprint. The branch is in the same repository, so the secrets are available.
5. Download the PR's `coursebook-pacer-apk` artifact and check it with `apksigner verify --print-certs`. It must show the same fingerprint.
6. Install that APK on an emulator, uninstalling first, and check that the app opens and keeps a school year across a restart.

#### Phase 4: merge and check the release

1. Merge. CI on `main` signs and checks the APK, then `release.yml` publishes `vX.Y.Z`.
2. Download the release asset and confirm the fingerprint with `apksigner`, matching the job summary.

#### Phase 5: move each phone over (once)

1. **Before uninstalling**, decide what happens to the data on that phone (open question 4). Uninstalling deletes it, and Android backup won't restore it across the change of key.
2. Uninstall Course Pacer (Settings → Apps → Course Pacer → Uninstall). Install the new release from <https://github.com/tsampolion/course_planner/releases/latest> (Assets → `coursebook-pacer-X.Y.Z.apk`), allowing the browser to install unknown apps.
3. Do the same on the emulator if it should run releases. Android Studio builds still clash with releases unless open question 3 changes that.

#### Phase 6: prove the update path (at the next release)

1. When the next version is released, install it **over** the current one without uninstalling. Android must offer **Update**, and every school year must still be there.
2. Only then mark this entry shipped.

### Rules after it ships (go into `CLAUDE.md` with the change)

- **Never regenerate, replace or "fix" the signing key**, and never change `keyAlias`, the secret names or the variable without the owner. A new key means every phone uninstalls again.
- Proper key rotation needs APK Signature Scheme v3 rotation (`apksigner rotate` and a signing lineage), which AGP doesn't do out of the box. If the key ever leaks, plan that as its own task.
- Never paste the keystore, its base64 or the password into a chat, an AI session, an issue, a pull request or a commit. If one is exposed, treat the key as leaked.
- Anyone with write access to the repository can read the secrets through a workflow, so keep write access to the owner.
- Builds made without the key (Android Studio, `installDebug`, a local `assembleRelease`) can't install over a release, and a release can't install over them.

### Done when

- [ ] The key was created on the owner's laptop, backed up in two places, and both backups were tested with `keytool -list`.
- [ ] `SIGNING_KEYSTORE_B64` and `SIGNING_PASSWORD` (secrets) and `SIGNING_CERT_SHA256` (variable) are set.
- [ ] The pull request (Gradle, CI, `.gitignore`, docs, version) is merged after every Phase 3 check passed.
- [ ] The first release from it is signed with the recorded fingerprint, confirmed in the job summary and with `apksigner` on the downloaded asset.
- [ ] Every phone has been moved over once.
- [ ] The next release installed as an update over it, with the data intact.

### Open questions (ask before starting)

1. **Key type:** RSA 4096 (recommended: the widest tool support, and fine as a Play upload key later) or EC P-256 (a smaller key, equally secure)?
2. **Version:** PATCH (recommended; the `CHANGELOG.md` rules call build and CI changes PATCH) or MINOR, to make the one-time reinstall stand out?
3. **Debug builds beside the release:** add `applicationIdSuffix = ".debug"` and a "Course Pacer (debug)" label to the debug build type? Then Android Studio installs a separate app with its own data, and never clashes with or touches the real one. Recommended, but it changes how testing on the phone works.
4. **Data on phones before the switch:** re-enter it after reinstalling, or build an export and import of `plans.json` first, through the share sheet and the system file picker using `JsonCodec`? That would be a separate planned feature. `docs/BRIEF.md` already expects importing the web page's data later.
5. **Fingerprint in release notes:** append "Signed with certificate SHA-256 …" to each release's notes (recommended, so any APK can be checked against it)?
6. **Google Play later?** If so, use Play App Signing with this key as the upload key. Nothing needs deciding now.
