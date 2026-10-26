# Task: Export and import school years

Status: shipped in 1.6.0; the size limit and round-trip contract were reworked in 0.4.6.2. [`ARCHITECTURE.md`](ARCHITECTURE.md) describes the current app.

This is the follow-up agreed while settling the signing key (`docs/SIGNING_BRIEF.md`), with the answers to its questions folded in. Version 1.6.0 was built from it.

## Context

Read `CLAUDE.md` first; its rules are binding. School years live in `plans.json` in the app's private storage. Android backup covers it, but a restore can't be relied on, and it doesn't help across a change of signing key. Uninstalling deletes the file. Until now the app had no way to get the data out or back in, and `docs/BRIEF.md` already expected the web page's data to be importable one day.

This feature can't save the data on phones that run a release from before 1.5.3, the first signed one: installing any newer build there needs an uninstall first (see the signing brief). From 1.5.3 on, releases install as updates, so 1.6.0 arrives as an update and the export is there before any later reinstall or change of phone.

## Goal

A teacher can save every school year to a file of their choice and read that file back, on the same phone or another one, without losing anything already on the phone unless they ask for that.

## Decisions (from the questions)

1. **Contents:** every school year, in exactly the format of `plans.json` (`JsonCodec.encode`, the current version). Settings (theme, new-class defaults, your name, the tips already seen) stay out; they belong to the phone.
2. **Import asks each time.** After the file is read, a dialog says how many years it holds and names them, then offers:
   - **Add**: the file's years go beside the ones on the phone. A year whose id is already on the phone gets a new id. A year whose name is already taken gets " (imported)", or " (imported 2)" and so on, after its name. Nothing on the phone changes.
   - **Replace all**: the file's years become the only ones on the phone, and no year stays open.
   - **Cancel.**
   Either choice is one undo step, offered by the snackbar ("Undo"), since Settings has no undo button.
3. **Placement:** a "Back up and restore" section in Settings, above About, with Export… and Import… buttons and a line on when to use them.
4. **Export goes through the system "Save to" picker** (`ActivityResultContracts.CreateDocument`), so the file can go to Downloads, Google Drive or anything else the phone offers. The suggested name is `course-pacer-YYYY-MM-DD.json`. Import uses the system file picker (`OpenDocument`). No permission, manifest entry or dependency is added.

## Rules for reading a file

- The file must be a Course Pacer export or `plans.json` of any version (1 to 4), or the web page's export (the flat `plans` list, which goes through `migratePlans` as on launch).
- A file whose `version` is newer than the app's is refused ("comes from a newer version of Course Pacer; update the app first"), because re-saving it here would drop what this version doesn't know.
- A file with no school years is refused.
- A file that isn't JSON, or isn't in one of those shapes, is refused with a message.
- **Round trip.** Anything this version exports, it imports. The supported scale was set from synthetic data (3 school years of 8 classes of 15 groups, each group with a 120-lesson custom plan: about 8 MB, above the old 5 MiB limit), which a platform test encodes with the real codec and reads back with nothing missing.
- **Size bound.** The file is streamed in chunks and refused once it passes 50 MiB (`MAX_IMPORT_BYTES`), or at once when the picker reports a larger size, so a huge or endless file never fills memory. An oversized file gets its own message ("This backup is too large to open (N MB; the limit is M MB)."), not "unreadable". The limit is about six times the synthetic scale above.
- Refusing never changes anything on the phone.

## What to build

1. **`data/Backup.kt`** (plain Kotlin, unit-tested on the JVM):
   - `backupFileName(date)`: `course-pacer-YYYY-MM-DD.json`.
   - `addYears(current, incoming)`: the Add rule above. Ids only need to be unique among years, because every class, group, book and day off is looked up inside its own year, so only the year's id changes.
   - `ImportResult`: `Years(list)`, `NoYears`, `Newer`, `Unreadable`.
2. **`data/JsonCodec.kt`:** `VERSION` becomes public, and `readImport(text)` returns an `ImportResult` by the rules above.
3. **`ui/PacerViewModel.kt`:**
   - `exportText()`: the file to write, from the data in memory.
   - `addImportedYears(years)`: one undo step; the open year stays open.
   - `replaceAllYears(years)`: one undo step; no year stays open, so screens for the old year close.
4. **`ui/SettingsScreen.kt`:**
   - The section, both pickers, and the Add / Replace all / Cancel dialog. With no years on the phone, the dialog offers only Add, and Export is off.
   - Import accepts any file type, because phones label JSON files inconsistently; a wrong pick is refused with a message.
   - Reading and writing happen off the main thread. Writing truncates the target file (`"wt"`), so overwriting an older export leaves no tail of it behind.
   - Messages: "Saved N school years to <file>", "Couldn't save the file", the refusals above, and after an import "Added N school years" or "Replaced your school years with N from the file", with Undo.
5. **Tests:**
   - `BackupTest` covers the file name and the Add rule: distinct years; a clashing id; a clashing name; importing the same file twice; and the existing years left untouched.
   - `JsonCodec.readImport` needs the real `org.json`, which the JVM unit tests don't have, and adding it as a test dependency needs the owner's approval. It was checked in the build session with a temporary, uncommitted `org.json`: a round trip of the sample year, the web page's flat export, an empty list, a newer version and junk.
6. **Docs and version:** README (features and the storage line), `CLAUDE.md` (`data/`), `docs/BRIEF.md` (import of the web page's data is now possible), `CHANGELOG.md`, and 1.6.0 (MINOR), `versionCode` 14.

## Out of scope

- Exporting settings, or a single year.
- Sharing the file through the share sheet (it would need a `FileProvider`).
- Automatic or scheduled backups.
- Merging a year from the file into a year on the phone class by class. Add keeps both copies side by side; delete the one you don't want.
