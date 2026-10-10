# Changelog

All notable changes to this project are documented in this file.

The format is based on [Keep a Changelog](https://keepachangelog.com/en/1.1.0/),
and this project adheres to [Semantic Versioning](https://semver.org/spec/v2.0.0.html).

Versioning for this app:

- **MAJOR**: a change that breaks existing saved plans or the import from the web page, or that changes scheduling results on purpose
- **MINOR**: a new feature that keeps existing plans working
- **PATCH**: bug fixes, performance or size improvements, and changes to documentation, licensing, build or CI that leave the app working as before

Every pull request bumps the version:

1. Choose MAJOR, MINOR or PATCH using the rules above.
2. Set `versionName` in `app/build.gradle.kts` to `X.Y.Z` (always three parts) and increase `versionCode` by one.
3. Add a `## [X.Y.Z] - YYYY-MM-DD` section below with that PR's changes, plus its compare link at the bottom.
4. After merging, tag the merge commit `vX.Y.Z`.

Keep `[Unreleased]` empty between pull requests.

## [Unreleased]

## [1.0.1] - 2026-10-10

Changes since the `v1.0.0` tag: documentation, licensing and the version format. The app itself doesn't change.

### Added

- `CHANGELOG.md` (this file).
- `CLAUDE.md` with build commands, architecture, rules and gotchas for AI coding agents.
- `LICENSE`: proprietary, all rights reserved, with Dimitrios Tsampallas as the copyright holder. Section 3 forbids AI systems from changing, copying, distributing or training on the code unless the owner has explicitly authorised them.
- README sections on features, tech stack, prerequisites, CI, project structure, development workflow and license, plus a CI status badge.
- `.gitignore` entries for `.kotlin/`, `.navigation/`, `*.apks`, `/app/release/`, logs, heap dumps, `Thumbs.db` and `.claude/settings.local.json`, grouped under comments.

### Changed

- `versionName` follows SemVer exactly: `1.0` became `1.0.0`, and is now `1.0.1` (`versionCode` 2).
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

[Unreleased]: https://github.com/tsampolion/course_planner/compare/v1.0.1...HEAD
[1.0.1]: https://github.com/tsampolion/course_planner/compare/v1.0.0...v1.0.1
[1.0.0]: https://github.com/tsampolion/course_planner/releases/tag/v1.0.0
