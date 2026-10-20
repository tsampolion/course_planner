# Documentation cleanup: design

Agreed with the owner on 2026-10-20. Version 2.5.1 (PATCH: documentation only).

## Why

The repository is public and stays public (making it private would drop the `release` environment on GitHub Free). The docs had grown to about 30,000 words. Some of it is owner-only or risky to publish, some is out of date, and nothing says which document is current.

## Decisions

- **Remove, don't hide.** Owner-only material is deleted, not moved. Nothing removed is a secret (no key, password or base64 was ever committed), so git history is not rewritten.
- **Nothing changes in the app, the workflows, `LICENSE`, `third_party/`, the style guide (it must stay identical to Course Invoicer's copy) or past `CHANGELOG` entries.**

## 1. Removed

- `docs/superpowers/plans/2026-10-18-edusuite-home-settings-privacy.md` and `docs/superpowers/specs/2026-10-18-edusuite-home-settings-privacy-design.md`: finished, and mostly about another repository's internals.
- `docs/PLANNED_FEATURES.md` entry 2 (signing): shipped in 1.5.3. Replaced by one line pointing at `docs/SIGNING_BRIEF.md`.
- `docs/SIGNING_BRIEF.md` is rewritten as a short "how release signing works": one permanent key, only `main` builds use it, the certificate is checked in CI and again before publishing, never change it. Operational detail goes (step tables, key file name, backups, environment settings, session anecdotes, old certificate fingerprints).
- README: the owner-only "sign a local build with the release key" snippet and the Settings click-paths. Secret names stay; `android.yml` shows them anyway.
- `docs/FEEDBACK_1.4.md` (a finished checklist) and `docs/UI_BRIEF.md` (the 1.2.0 screens, self-declared outdated).

## 2. Structure

- New `docs/README.md`: an index in three groups (rules and reference, decision records, not yet approved), one line per document.
- `YEARS_BRIEF.md`, `LESSON_PAGES_BRIEF.md`, `BACKUP_BRIEF.md` and `SIGNING_BRIEF.md` stay where they are (links in `CLAUDE.md`, code and workflows point at them) with a banner: "Decision record: built in X.Y.Z. The code and `CLAUDE.md` describe the current app."
- `docs/BRIEF.md` is checked against the current app and its stale wording fixed.
- `docs/superpowers/` keeps only Course Pacer specs and plans.

## 3. README, CLAUDE.md, version

- README: about half its length. What the app is and the docs index; the feature list tightened and checked against 2.5.0; "Moving your data to 2.0.0" removed (the changelog keeps it); build and run; CI and releases in one paragraph linking `SIGNING_BRIEF.md`; the project tree shows `docs/` as one line; tech stack, footprint, workflow and licence with duplicates merged.
- `CLAUDE.md`: references updated; signing rules kept as rules without operational detail; a pointer to `docs/README.md`; two new rules:
  - **Docs stay current.** Every pull request updates the docs its change makes out of date (README features, `CLAUDE.md`, the docs index, a brief's banner, a `PLANNED_FEATURES.md` status) and deletes any doc it makes obsolete.
  - **Nothing risky in the public docs.** Don't commit plans, specs or notes that describe other repositories, security weaknesses, or how keys and secrets are created, stored or handled.
- The README's development workflow gets the "docs stay current" rule too.
- `versionName` 2.5.1, `versionCode` 27, a `CHANGELOG.md` 2.5.1 section with its compare link.

## Done when

- No removed file is still linked from anywhere (`grep` for each name).
- `./gradlew testDebugUnitTest assembleRelease` pass (nothing in the build changes, but the workflow rule asks for it).
- One pull request, version 2.5.1.
