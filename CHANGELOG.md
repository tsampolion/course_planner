# Changelog

All notable changes to this project are documented in this file.

The format is based on [Keep a Changelog](https://keepachangelog.com/en/1.1.0/),
and this project adheres to [Semantic Versioning](https://semver.org/spec/v2.0.0.html).

Versioning for this app:

- **MAJOR**: a change that breaks existing saved plans or the import from the web page, or that changes scheduling results on purpose
- **MINOR**: a new feature that keeps existing plans working
- **PATCH**: bug fixes, and performance or size improvements

When you cut a release, rename `[Unreleased]` to the new version and date. Set `versionName` in `app/build.gradle.kts` to the same version, increase `versionCode` by one, and tag the commit `vX.Y.Z`.

## [Unreleased]

### Added

- `CHANGELOG.md` (this file).
- `CLAUDE.md` with build commands, architecture, rules and gotchas for AI coding agents.
- README sections on features, tech stack, prerequisites, CI, project structure, contributing and license, plus a CI status badge.
- `.gitignore` entries for `.kotlin/`, `.navigation/`, `*.apks`, `/app/release/`, logs, heap dumps, `Thumbs.db` and `.claude/settings.local.json`, grouped under comments.

### Changed

- The README now says that CI runs on every push and on manual dispatch, as well as on pull requests.

## [1.0.0] - 2026-10-10

### Added

- Native Android app (Kotlin, Jetpack Compose, Material 3) ported from the Coursebook Pacer web page.
- Pacing engine in plain Kotlin that matches the web page's schedule row for row. It covers lessons per group from its weekdays minus days off, skipped and homework pages, even allocation so every book lands on the final lesson, finish-by dates, tests that take the whole lesson, and the pace verdict.
- Greek public holidays generated from Orthodox Easter.
- Plans you can create, rename, switch and delete, with up to 60 undo steps.
- Setup screen for the term, groups, days off, tests and worksheets, books, and homework, with a list of plan errors.
- Group screen with a summary, verdict, per-book progress, lessons per month, the lesson-by-lesson schedule, and "Mark next lesson done".
- Copy as tab-separated text, share, and print or save as PDF.
- Plans stored as JSON in app storage with atomic, debounced writes, a JSON format compatible with the web page, and backup and device-transfer rules.
- Unit tests for the engine.
- GitHub Actions workflow that runs the tests and builds the release APK on every push and pull request, and uploads the APK as a build artifact.

[Unreleased]: https://github.com/tsampolion/course_planner/compare/v1.0.0...HEAD
[1.0.0]: https://github.com/tsampolion/course_planner/releases/tag/v1.0.0
<!-- Both links work once the v1.0.0 tag is pushed: git tag v1.0.0 5a466df && git push origin v1.0.0 -->
