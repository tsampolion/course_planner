# Documentation Cleanup Implementation Plan

Shipped as 2.6.1 (versionCode 28): `main` moved to 2.6.0 during the work.

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Trim, correct and index the public documentation, remove owner-only material, and add the "docs stay current" rule (2.5.1).

**Architecture:** Markdown-only changes plus the version bump. No code, workflow, `LICENSE`, `third_party/` or style-guide change. Each task ends with a `grep` check that stands in for a test.

**Tech Stack:** Markdown, Gradle (only to run the workflow's checks).

**Spec:** `docs/superpowers/specs/2026-10-20-docs-cleanup-design.md`

## Global Constraints

- Delete, don't hide. Never add key-creation steps, backup locations, environment settings or other repositories' internals.
- Never edit `docs/EDUSUITE_STYLE_GUIDE.md`, `LICENSE`, `third_party/`, `.github/workflows/*`, or `CHANGELOG.md` entries before 2.5.1.
- Keep the files that code or workflows link to at the same paths: `SIGNING_BRIEF.md`, `PACER_ACCENT_CONTRAST.md`, `BRIEF.md`, `BACKUP_BRIEF.md`, `LESSON_PAGES_BRIEF.md`, `YEARS_BRIEF.md`.
- Follow the repo's writing style: short plain sentences, British spelling, no marketing tone.
- Version 2.5.1, `versionCode` 27.

## Review Focus

1. A link to a deleted file left anywhere: Task 4 greps every removed name across the repo.
2. A README feature claim that no longer matches 2.5.0: Task 3 checks each bullet against `strings.xml`/screens.
3. The signing rewrite loses a rule `CLAUDE.md` relies on (main-only, certificate checked twice, never change key/alias/secret names): Task 1 step lists them.
4. The style guide changed by accident: Task 4 runs `git diff --stat origin/main -- docs/EDUSUITE_STYLE_GUIDE.md` (expect empty).
5. The build broke: Task 4 runs the workflow's commands.

---

### Task 1: Remove owner-only and finished material

**Files:** Delete `docs/superpowers/plans/2026-10-18-edusuite-home-settings-privacy.md`, `docs/superpowers/specs/2026-10-18-edusuite-home-settings-privacy-design.md`, `docs/FEEDBACK_1.4.md`, `docs/UI_BRIEF.md`. Modify `docs/PLANNED_FEATURES.md`, rewrite `docs/SIGNING_BRIEF.md`.

- [ ] **Step 1:** `git rm` the four files.
- [ ] **Step 2:** In `PLANNED_FEATURES.md`, replace entry 2 with a heading and one line: shipped in 1.5.3, see `SIGNING_BRIEF.md`.
- [ ] **Step 3:** Rewrite `SIGNING_BRIEF.md` as "Release signing", under 350 words, with the decision-record banner. It must state: one permanent key; only `main` builds get the secrets (the `release` environment); other branches and PRs use the debug key and never publish; CI checks the certificate against `SIGNING_CERT_SHA256` and `release.yml` checks it again and puts it in the notes; debug builds are `gr.edusuite.pacer.debug`; never change the key, `keyAlias`, secret names or variable; leaks mean v3 rotation as its own task. Drop everything else.
- [ ] **Step 4: Check.** `grep -rnE "keytool|gh secret set|cloud session|required reviewers|2a1a02fc" docs/` prints nothing.
- [ ] **Step 5: Commit** `docs: remove owner-only and finished material`.

### Task 2: Docs index, banners, BRIEF refresh

**Files:** Create `docs/README.md`. Modify `docs/YEARS_BRIEF.md`, `docs/LESSON_PAGES_BRIEF.md`, `docs/BACKUP_BRIEF.md`, `docs/BRIEF.md`.

- [ ] **Step 1:** Write `docs/README.md` with three groups: *Rules and reference* (`BRIEF.md`, `EDUSUITE_STYLE_GUIDE.md`, `PACER_ACCENT_CONTRAST.md`), *Decision records* (`YEARS_BRIEF.md` 1.3.0, `LESSON_PAGES_BRIEF.md` 1.5.0, `SIGNING_BRIEF.md` 1.5.3, `BACKUP_BRIEF.md` 1.6.0, `superpowers/`), *Not yet approved* (`PLANNED_FEATURES.md`), one line each.
- [ ] **Step 2:** Add the banner under each brief's title: `> Decision record: built in X.Y.Z. The code and CLAUDE.md describe the current app.`
- [ ] **Step 3:** Read `BRIEF.md` against the current app (Home, years, `CLAUDE.md` architecture); fix wording that describes removed screens.
- [ ] **Step 4: Check.** Every `docs/*.md` except `README.md` is linked from `docs/README.md`.
- [ ] **Step 5: Commit** `docs: add docs index and decision-record banners`.

### Task 3: README

**Files:** Modify `README.md`.

- [ ] **Step 1:** Restructure to: intro + docs index link; Features (one or two lines each, checked against 2.5.0); Getting started (without the owner-only signing snippet); CI and releases (one paragraph plus the secrets table, no Settings click-paths, link `SIGNING_BRIEF.md`); Project structure (`docs/` as one line → index); Tech stack; Footprint; Development workflow (add the "docs stay current" step); License. Remove "Moving your data to 2.0.0".
- [ ] **Step 2: Check.** `wc -w README.md` is about 1,000; `grep -n "UI_BRIEF\|Moving your data\|read -s" README.md` prints nothing.
- [ ] **Step 3: Commit** `docs(readme): shorten and point at the docs index`.

### Task 4: CLAUDE.md, version, verification

**Files:** Modify `CLAUDE.md`, `app/build.gradle.kts`, `CHANGELOG.md`.

- [ ] **Step 1:** `CLAUDE.md`: add "Docs index: `docs/README.md`" near the top; shorten the signing bullets to rules (drop "Anyone with write access…" operational sentence only if it adds no rule; keep "write access stays with the owner"); add the two Workflow rules from the spec verbatim in substance (**Docs stay current**, **Nothing risky in the public docs**).
- [ ] **Step 2:** `versionName = "2.5.1"`, `versionCode = 27`; `CHANGELOG.md` `## [2.5.1] - 2026-10-20` under `### Changed`, and the `[2.5.1]` compare link `v2.5.0...v2.5.1`.
- [ ] **Step 3: Check links.** `grep -rn "FEEDBACK_1.4\|UI_BRIEF\|edusuite-home-settings-privacy" --exclude-dir=.git . | grep -v "^./CHANGELOG.md"` prints nothing (old changelog entries may keep historical mentions).
- [ ] **Step 4: Check style guide.** `git diff --stat origin/main -- docs/EDUSUITE_STYLE_GUIDE.md` is empty.
- [ ] **Step 5: Build.** `./gradlew testDebugUnitTest assembleRelease` → BUILD SUCCESSFUL (if the environment can't reach Google's Maven, say so in the PR; CI runs it).
- [ ] **Step 6: Commit** `docs: docs-stay-current rule; 2.5.1`, push, open the PR.
