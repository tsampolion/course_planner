# Combined units and update-check delivery ledger

- User request: implement the final units phase and attached update-check plan in
  the same PR. The authorized publication branch is claude/dreamy-carson-ms25q9; PR50 was subsequently closed (see ruling below).
- Baseline: e68532d already incorporates merged Part B/main a7c7083. Required
  baseline `testDebugUnitTest` passed (303 tests; cached outputs accepted).
- Ruling: use isolated local worktrees for units engine and update checks, then
  integrate on the existing PR branch — independent source/build state prevents
  clashes — cost: shared UI/docs need explicit integration; only the authorized branch is pushed.
- Ruling: scope the update plan's no-engine/no-schema restriction to update checks
  — the user's combined request also authorizes the units engine/schema extension
  — cost: the combined PR must verify both data contracts and engine parity.
- Ruling: units revisions are per group, taught history is immutable, and Done on
  a completed unit offers revision — settles the old handoff questions using its
  approved addendum — cost: revision decisions are independent across groups.
- Ruling: class units off leaves group overrides stored but inert — follows the
  handoff's explicit review focus — cost: group On cannot enable a disabled class.
- Ruling: combined release 4.1.0 (49), units schema 14; update state stays outside
  plans.json — both features are optional and disabled-unit parity is required —
  cost: future-format backups require this release, while old files keep defaults.
- Baseline tests /workspace/combined-baseline.log. Build execution is serialized
  across worktrees with /workspace/course-pacer-gradle.lock. No parallel Gradle
  builds, signing/dependency changes, or extra permission beyond INTERNET.
- Implementation scopes: units_engine owns model/codec/engine/tests; units_ui owns
  native UI/navigation/VM additions; updates_impl follows its 7 supplied tasks in
  isolation. Root integrates, resolves shared files, runs final review and gates.
- Impeccable launcher unavailable; existing PRODUCT/PACER_DESIGN and native rules
  used directly. Current Bone/Saffron tokens win historical teal references.
- A1 model/codec gate passed (304 tests), committed e05bce6 and integrated as
  3446c57. Update pure model and settings gates passed, integrated as 87c2705 and
  d325340. Checker flow gate passed including 13 new tests; integrated next.
- Session resumed from saved work: completed commits and RED/GREEN logs retained;
  continuing only unfinished tasks. No implementation was restarted.
- Ruling: PR50 was closed unmerged during the pause; preserve its closure and open
  one new combined PR from the authorized implementation branch — honors the
  current combined request without reopening an owner-closed PR — cost: a new
  review URL; prior spec attribution/footer is retained.
- Confirmed no connected device, no installed emulator and no KVM. Update Task6
  will be marked not run with the original owner checklist; native visuals and
  real platform codec execution remain pending.
- Cross-feature regression: same-ID revision edits cleared reciprocal homework
  pairing. Observed RED and corrected in-place update; full units gate pending.
- Units A2/A3 full gate passed: 321 tests, zero failures/errors/skips; commit
  4708d87 integrated as 63cc4f6. Includes linked barrier, empty-lead backlog,
  impossible-window issues, custom/catch-up integration and revision pair fix.
- Update checker integrated as d85b92d after its 13 flow tests and full gate.
  Units UI now compiles against the actual combined model/engine/settings APIs.
- Update HTTP/JSON/VM wiring gate passed; task4 commit 798e0e9 awaits integration
  after the units UI commit because PacerViewModel is shared. Task5 owns the
  serialized suite/release/lint build before UI integration.
- Units UI focused tests passed. First full gate had one foreground-source guard
  failure on a progress fill; kept guard unchanged and named the fill role.
- A new unit-label regression must count actual taught dates rather than reset
  after Done. Current queued UI test intentionally retains the old calculation
  for RED; fixed helper saved at /workspace/units-ui-history-label-fixed.kt.
  Restore it after the observed failure, then require the bare full GREEN gate
  before committing/integrating overlapping update UI.

- Units UI final gate passed: 362 tests; actual-history label regression observed
  RED then GREEN. Integrated UI commit ab09b67 and update Tasks4/5/7 through
  4a60e7e. Resolved VM/resource conflicts retaining both implementations.
- Fully integrated unit suite passed: 362 tests, zero failures/errors/skips;
  /workspace/combined-integrated-unit.log. Final packaging follows review fixes.
- Fresh combined review identified five material findings: PAGES excluded ordinary
  books; reserved PAGES rows erased selected content; editing an owned date could
  move it because temporary removal shifted custom lessons; recomputed windows
  could teach the next unit before the chosen revision; catch-up weights were
  discarded within linked units. One regression-led material fix pass underway.
- Reviewer stopped at its usage limit before a consolidated report. Findings
  remain actionable; this is not a clean full-review result. Root separately
  inspects update wiring and integration, with native runtime judgments declined.
- Remote main rechecked: a7c7083 (4.0.0/48), unchanged; combined 4.1.0/49 remains
  the next appropriate minor release. No version or signing drift.
- Root integration review confirmed update DI/debug exclusion, current-settings
  merge, trusted cached/network links and permission/build boundaries. No further
  material update finding from that source inspection; native checks stay pending.
- Root found an interaction in the actual-date unit fix: Stats uses two virtual
  progress slices, not calendar lessons. Add a units-enabled linked-Stats guard
  and an explicit virtual-progress contract to prevent indexing calendar slots.
- Anchor fix also needs early/out-of-order revision guards: a missing window for
  unfinished unit pages must never release later automatic units across it.
- Regression evidence: engine initial RED 21 tests/5 expected failures. After
  initial fixes those five passed; four added interaction guards then failed as
  expected (early/reversed anchors, manual order conflict, assignment-page edits).
  UI model RED 10 tests/3 expected failures; Home RED14 tests/1 expected failure.
- Revision pages use the owned assignment as current content; Work edits remain
  authoritative. The revision sheet preserves untouched book maps and never
  flattens different books into the selected book's range.
- Stats RED observed: enabled linked-unit inStepPages raised IndexOutOfBounds
  with more than two calendar dates. Explicit calendarUnits=false retains the
  established whole-book taught/rest reference calculation.
- Export RED observed: selected revision ranges disappeared from TSV; ordinary
  blank blocked cells also needed preservation after Row.cells changed. Both
  received narrow fixes. Print RED3 tests/1 expected failure reproduced missing
  revision ranges; title+book cells now render while ordinary colspan stays.
- Fold backlog shares weighted unit constraints. Preserve the selected book's
  front-load weights within windows, other book columns, full coverage and
  touched lessons; duration regression precedes the fix. No native polish loop.
- Material fix pass settled. Combined focused GREEN: 70 tests, zero failures or
  errors (Units28, GroupModel15, HomeModel14, UnitRevisionModel10, PrintSheet3),
  /workspace/review-focused-green.log, BUILD SUCCESSFUL2m25s. All new material
  regression cases were observed RED before their fix. No source edits follow.
- Full final command running under the shared lock: testDebugUnitTest lint
  assembleRelease assembleDebugAndroidTest, offline/two workers/in-process,
  warning-mode all. Durable log /workspace/combined-final-verification.log.
- Authorized remote branch rechecked: e68532d, unchanged, ancestor of local work.
  Only claude/dreamy-carson-ms25q9 will be pushed; preserve PR50 closure.
- Full-suite integration guard RED: 380 tests, one failure in
  NoHardcodedTextTest.noHardcodedUiCopy. Nested interpolation with an inner empty
  string in SlimRow confused its lexical scanner. Kept the guard unchanged and
  extracted the book name before interpolation; output is unchanged. Rerun the
  full command, serialized after the first process finishes outstanding lint.
  Final authoritative log is /workspace/combined-final-green.log.
- Full gate rerun: all380 tests passed. Lint then found one real correctness
  error: BookFields read vm.state.value during composition and could miss a
  class units-toggle update. Observed lint RED; now collectAsState supplies the
  current UiState. No lint suppression or test weakening. Report also lists71
  warnings/one hint; final counts follow the successful gate.
- Rerun the complete command after the reactive-state fix, under the same lock.
  Authoritative final log: /workspace/combined-final-complete.log. Earlier
  full-command logs are superseded and must not be cited as successful builds.
- Final complete gate succeeded: exit 0, BUILD SUCCESSFUL in 11m4s; all 380 tests
  passed with zero failures/errors/skips, lint zero errors/71 warnings/one hint,
  release and Android test APKs built. Signature verifies, one signer; package
  gr.edusuite.pacer version 4.1.0/code 49. Main rechecked unchanged a7c7083.
  Final evidence: /workspace/combined-final-complete.log; verification.md.
- All material findings fixed, plus mandatory source-copy and reactive Compose
  integration guards. No lint suppression, dependency/signing/id/minSdk change.
  Native Task 6 remains NOT RUN. Prepare/publish one draft PR, never merge/tag.
