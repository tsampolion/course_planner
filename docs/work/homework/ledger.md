# Part B implementation ledger

- Base: main 1aeaaeb, merged Part C PR51, 3.11.0 (47), schema 12.
- Ruling: implement inline on a separate feature branch in the managed checkout
  — preserves the supplied workspace and serial Gradle caches — cost: no extra
  worktree isolation. Branch feat/lesson-homework.
- Ruling: explicit optional contentType and bidirectional linkedId, with title
  inference for older/manual work — avoids repeated prompts after edits — cost:
  schema 13 and link cleanup when a counterpart is deleted.
- Ruling: due ?: setOn governs banking, inclusive of today; undated stays immediate
  — matches the handoff's date-based intent without losing undated assignments —
  cost: future homework intentionally changes pacing; version 4.0.0 required.
- Ruling: save original work before optional pairing, pair in one later undo step
  — Skip/Back always preserves the entered work — cost: accepting uses two total
  undo steps; undo pairing leaves the original assignment.
- Pre-flight: B1/B2 consume C's Lesson.assignments and AssignmentSheet; B3/B4
  share contentType, linkedId and pure date suggestions; B5 persists both fields.
  Conflicts resolved in HOMEWORK_BRIEF.md before implementation.
- Impeccable launcher unavailable. Read PRODUCT.md, PACER_DESIGN.md and native
  guidance directly. PRODUCT palette/feature notes are stale; current code and
  PACER_DESIGN D13 remain authoritative. No unrelated context repair.
- B1/B3 engine RED: missing contentType/linkedId, suggestion helpers and dated
  homeworkPages API. Implemented those interfaces and group pair/delete helpers.
- Engine GREEN: 11 new targeted tests pass, including due inclusivity, undated
  fallback, no-homework parity, closures, blocking, pairing and title inference.
  Log /workspace/homework-engine-green.log. UI action/row RED now running.
- UI RED observed for ADD/EDIT/DELETE_HOMEWORK and row helpers. Implemented them
  in the shared long-press/TalkBack menu, ordinary/slim/hero rows and editor flow.
- First compile exposed a missing Compose key import; corrected it. Full suite
  then ran 297 tests with one failure: ForegroundBoundaryTest rejected direct
  Material TextButton. Replaced prompt controls with the shared TertiaryButton.
- Added RED regressions for incompatible pair/type edits and imported metadata;
  added a platform roundtrip/schema-12-default regression (device execution pending).
- Device check after elevated adb startup: no connected devices; SDK still has
  no emulator/system images. Native screenshot and platform execution unavailable.
- Metadata RED observed: 300 tests ran, exactly the two new integrity regressions
  failed (invalid imported metadata; incompatible pair after type edit). The UI
  and readable-foreground regressions passed.
- Added shared normaliseAssignmentMetadata for live edits and codec sanitising;
  pair creation updates both sides before validation, rejects same-kind pairs,
  and deletion/type changes clear invalid surviving references. Date choices
  reuse the report's computed blocked flags rather than scanning work per date.
- Schema 13 read/write retains missing-field defaults and lenient unknown content.
  Full unit/lint/release/platform APK checks running serially; fresh reviewer
  examines the complete Part B diff and untracked files while build runs.
- Pre-review full suite: 300 tests, zero failures/errors; lint zero errors.
  Release shrinking/packaging still running; this run predates review fixes.
- Fresh whole-branch/native source review: three Important/P2 findings enter one
  fix pass: duplicate-title action ambiguity, hidden notes in summaries and
  nonreciprocal/conflicting imported links. No scheduling or optional-book issue.
- Final: minor (deferred): Worksheet↔Test edits do not auto-synchronize an existing
  homework counterpart's contentType; both records remain independently editable.
- Review's checkbox/status reminder is completion bookkeeping, handled after
  verification. Native recreation/Back/TalkBack/layout/motion were declined for
  lack of a device; source review is not visual approval.
- Ruling: retain only reciprocal, opposite-kind assignment links — ambiguous
  imported graphs must not suppress a teacher's next scheduling suggestion —
  cost: one-way/corrupt links are cleared rather than guessed or repaired.
- Stopped the superseded pre-review build during release shrinking; its 300-unit
  and lint results stand only for the old tree. Final artifacts will be rebuilt
  after the material fixes. No overlapping Gradle builds.
- Extracted existing summary/action data behavior into shared plain-Kotlin
  helpers without fixing it yet. Added regressions for note visibility,
  duplicate-title/identical-content actions and conflicting/asymmetric imports.
  Review RED running against those unchanged behaviors.
- Final review RED observed: 303 tests ran, exactly the three new regressions
  failed (note visibility, duplicate/identical-title action ambiguity, conflicting
  imported links). Log /workspace/homework-review-red.log.
- One material fix pass implemented: summaries include notes; action names include
  content/due date and a stable ordinal; metadata retains reciprocal opposite-kind
  links only, and suggestion suppression checks that same pair invariant.
  Final full suite/lint/release/platform APK command now running serially.
- Impeccable documentation completion: no material factual mismatch. Existing
  ledger/editor/TalkBack world and tokens are preserved; brief, README, CLAUDE,
  design appendix and release notes agree. Historical PRODUCT/README teal notes
  remain pre-existing drift, not repaired in this task. No visual approval.
- Final: fixed hidden homework notes — notesRemainVisibleWithRequiredTitleAndDueDate
  RED→GREEN, full unit suite 303/303.
- Final: fixed ambiguous duplicate-title homework actions —
  duplicateTitlesHaveDistinctActionsEvenWhenContentMatches RED→GREEN, suite 303/303.
- Final: fixed conflicting imported pair links —
  conflictingImportedLinksKeepOnlyTheReciprocalPair RED→GREEN, suite 303/303.
  Lint/release/platform packaging still running on the same fixed source tree.
- Final full verification succeeded: `testDebugUnitTest lint assembleRelease
  assembleDebugAndroidTest --offline --max-workers=2
  -Pkotlin.compiler.execution.strategy=in-process` exited 0, BUILD SUCCESSFUL in
  9m54s. 303 unit tests, zero failures/errors/skips; lint zero errors, 63 warnings.
  Release and platform test APKs build. Log /workspace/homework-review-green.log.
- B1–B5 and the single material review-fix pass are complete. Platform codec
  execution (15 checks) and native visual/accessibility/recreation QA remain
  pending for a device. Deliver as a draft PR; Part A remains separate.
