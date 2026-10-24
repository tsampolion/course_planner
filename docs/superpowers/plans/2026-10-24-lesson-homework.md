# Lesson homework implementation plan

> **For agentic workers:** Use superpowers:executing-plans inline. TDD one task at a time; fresh whole-branch review at the end.

**Goal:** implement Part B of the attached handoff after Part C.
**Architecture:** pure assignment linking/date helpers; shared assignment editor
flow for Work and lesson menus; existing native ledger and TalkBack actions.
**Tech Stack:** Kotlin, Compose Material 3, platform org.json, JUnit.
**Spec:** docs/HOMEWORK_BRIEF.md and the attached handoff Part B.

## Constraints and review focus

No dependencies, signing/applicationId/minSdk changes. Resource copy and existing
tokens. No feature means unchanged scheduling. Focus on duplicate suggestions,
closures/blocked lessons, multiple homeworks per date, optional books, undone
pairing/deleted counterpart, saved custom content, large text/TalkBack and Back.

## Tasks

- [x] B1/B3: write AssignmentLinksTest for worksheetHomeworkSuggestsAClassLesson,
  testOnDateSuggestsHomeworkOnPreviousLesson, noSuggestionWhenAlreadyLinked,
  suggestionSkipsDaysOff, blocked/boundary dates, pairing and delete cleanup;
  HomeworkPacingTest for due/set-on/undated/no-feature parity. Run expected RED.
  Implement Assignment contentType/linkedId, AssignmentLinks.kt and date gate.
  Run targeted GREEN before UI.
- [x] B2/B4: test lesson menu homework actions and row content helpers RED, then
  implement shared editor/prompt, content selector and lesson menu/row wiring.
  Preserve TalkBack custom actions and native components. Run full unit suite.
- [x] B5: schema 13 read/write and platform roundtrip regression, version 4.0.0
  (48), CHANGELOG, README/CLAUDE/design/index/brief. Run testDebugUnitTest lint
  assembleRelease assembleDebugAndroidTest serially; expect success.
- [x] Fresh whole-branch review, one tested material fix pass, record rulings and
  pending device QA.

Delivery: commit, push and open a separate draft PR against main.
