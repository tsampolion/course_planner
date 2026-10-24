# Part C implementation ledger

- Baseline: clean branch work, app 3.9.0 (45), schema 10.
- Ruling: stage migration explicitly until engine and editors switch together —
  migrating on load now would allow old editors to leave stale assignments —
  cost: C1 activation remains part of the C2/C4/C6 cutover.
- Ruling: keep the managed checkout in place — it is the user-provided workspace
  and clean — cost: no separate worktree; implementation now uses branch feat/group-assignments.
- Initial Gradle sandbox run blocked by local socket restrictions; retrying with
  elevated execution using the cached JDK, SDK and Gradle home.

- Red: clean targeted Gradle build reached compileDebugUnitTestKotlin and failed
  for the missing Assignment/AssignmentType/Group.assignments/migrateAssignments
  API, as expected. Log: /tmp/assignments-red.log.
- Build recovery: overlapping initial builds collided in Kotlin incremental
  caches. Stopped both daemons and their orphan compiler children; clean serial
  in-process compilation succeeded for production code before the expected red.
- C1 foundation implemented: model, schema 11 read/write, explicit migration,
  sanitising; legacy fields retained and conversion not activated.
- Device status: adb devices reports no attached device. Platform codec tests
  will be compiled; execution and visual checks remain device-dependent.
- Full unit/lint/release/Android-test compilation running serially.
- Whole-diff review: fresh reviewer found no actionable issues in the staged
  foundation. Engine and UI remain unchanged. No deferred minor findings.
- Verification complete: `testDebugUnitTest lint assembleRelease
  assembleDebugAndroidTest --offline --max-workers=2
  -Pkotlin.compiler.execution.strategy=in-process` succeeded (12m 11s).
  249 unit tests, zero failures/errors. Lint, release APK and platform test APK
  passed. Platform codec execution remains unverified without a device.
- No PR or release-version bump yet: this is an intermediate Part C foundation.
  Continue with C2/C4/C6 coordinated cutover; do not treat Part C as complete.

## Continuation: C2–C6

- Continue on feat/group-assignments; prior foundation and verification retained.
- Ruling: keep TestEvent as a compatibility projection for lesson display/export
  and legacy in-memory Plan fixtures while reading group assignments internally —
  keeps old parity tests meaningful and limits unrelated UI churn — cost: the
  projection remains until a later cleanup; it is never persisted as new work.
- Ruling: represent group pages with Screen.Group(id, page), preserving group
  identity in tablets and saved launch state — avoids duplicating group routing —
  cost: navigation rules must distinguish same-group subpages from group switches.
- C2/C3/Nav red: targeted tests failed on missing Row.assignments, dueSoon,
  workSections and GroupPage; fixed a test fixture API typo before proceeding.
- UI direction: extend current native ledger language, Outfit and Bone/Saffron
  tokens. Group home order is hero → Progress → Due soon → Plan/Work/Stats.
  The committed redesign board is stale in palette; current Tokens.kt/D13 win.
- Device capability unchanged: SDK has no emulator/system image, adb had no
  device. Native screenshot inspection cannot be claimed from source builds.
- C2/C3/Nav green: full unit suite passed after normalising obsolete TestEvent.who
  metadata in the migration parity assertion (assignment-owned tests necessarily
  target their group). Dates/names/blocking/pages/verdict/custom rows stay checked.
- C4/C5 implementation: Work filters/sections, sheet, visible edit/delete menu,
  swipe and existing Undo; group home/Plan/Stats subpages and tablet Plan detail.
- Coordinated cutover implemented: old class editors removed; class and legacy
  routes link to group Work; JSON converts on load/save and omits legacy lists.
- Validation/motion tests added before their helpers; pending expected red.

- Validation/motion red observed: assignmentPages/assignmentDatesValid/MotionSpec
  and worksheet row typing missing; helpers and consumers now implemented.
- Fixed one compile error from adding the optional tablet-link parameter; no
  runtime workaround. Validation green suite running.
- Ruling: Undo's 5,000 ms is a notification lifetime, separate from transitions
  constrained to <= 1,200 ms — resolves the addendum's duration conflict — cost:
  MotionTest bounds transition timings only, while Undo retains five seconds.
- Ruling: keep legacy class work destinations as redirects for one release —
  the plan explicitly permits a moved link — cost: remove these markers later.
- Release preparation: 3.10.0 (46); README, CLAUDE, design and CHANGELOG updated.
- Whole-Part-C fresh review found phone inset loss, tablet sheet gating, missing
  motion consumers and incomplete Home pace comparison. Home source mismatch
  was independently confirmed. No further migration or editor data-loss defect.
- Re-graded the pace summary omission as important: a lagging book needs its
  existing planned/lead comparison and behind count on Home. Reused PaceTrack.
- Review regressions observed RED for groupInsets, logical tablet groupPage and
  once-per-open entranceDelay; implementations now consume these helpers.
- Review fix pass GREEN: full unit suite (263 tests, no failures/errors) and
  Android test packaging passed before the final minor motion/accessibility
  corrections. Final full checks are running.
- Fixed phone inset loss by consuming Scaffold top inset once in the shared
  group frame; kept bottom inset in child pages. Tablet rendering now preserves
  Screen.Group identity and maps only the rendered page, so toolbar sheets work.
- Fixed active home schedule vs inactive custom preview via tested page helpers.
- Motion consumers added: 14px once-per-open row rise with bounded 70ms stagger,
  approved check-glyph reveal, long-press ring without changing system gesture
  timing, native menu content reveal, 24px calendar crossfade, pace fills and
  rolling lesson count. Reduced motion renders finished states.
- Ruling: assignment sheets use a modal Dialog with existing Material styling,
  Close, system Back and outside-tap dismissal — Material 3 1.3.2 exposes no
  sheet timing controls, and adding/upgrading dependencies is forbidden — cost:
  this sheet does not inherit Material's drag-to-dismiss gesture; device QA of
  focus, keyboard insets and motion remains required. Other sheets stay native.
- No deferred minor findings: the one minor pace-comparison finding was upgraded
  and fixed because of its user impact. No native visual approval is claimed.

## Main advanced during implementation

- Remote main ec0138e (PR49) now includes group settings, optional books and class
  collections, release 3.10.0 (46), schema 11. Initial checkout was PR48.
- Stopped the superseded final verification build before updating source.
- Ruling: integrate PR49 before opening Part C, then use 3.11.0 (47) and schema 12
  — prevents version collision and preserves new group/book behaviour — cost:
  re-run integration/parity and full checks on the combined tree.
- Preserve the entire Part C working tree in a named stash, fast-forward the
  feature branch to origin/main, then apply the stash without dropping it until
  conflict resolution and verification are complete.
- Conflicts resolved with both feature sets retained: group overrides, effective
  optional-book indices, collections and the GroupSettings navigation identity.
- Fresh integration review found no additional actionable regressions. It
  confirmed the optional-book editor risk addressed by the following regression.
- Integration red: full unit compilation failed for the missing
  `assignmentEditedPages` helper, as expected (/tmp/assignment-integration-red.log).
- Assignment editing now changes only fields for effective books, preserving
  pages from deselected optional books; a visible hint explains how to edit them.
  Regression covers preserving hidden pages and clearing a visible book's pages.
- Added migrated-schedule parity with optional books and group calendar overrides,
  plus a platform schema-11 roundtrip retaining collections and group settings.
- Full integrated unit/lint/release/platform-test compilation is running serially
  against main ec0138e. Earlier green runs do not certify this integrated tree.
- Final narrow review of optional-book preservation: no findings; visible fields
  can clear pages while hidden books remain intact. No deferred minor findings.
- The execution environment restarted during verification on 2026-10-24; the
  process and temporary log disappeared. Source and cached SDK/JDK survived.
  Restarted the full serial command with a persistent log at
  /workspace/assignment-final-integrated.log; do not claim the interrupted run.
- Final integrated verification: `testDebugUnitTest lint assembleRelease
  assembleDebugAndroidTest --offline --max-workers=2
  -Pkotlin.compiler.execution.strategy=in-process` exited 0. Gradle accepted the
  restored outputs as up to date (104 tasks, 1 executed). XML reports: 283 tests,
  zero failures/errors/skips; lint zero errors, 63 warnings. Release and platform
  test APKs exist. Persistent command log: /workspace/assignment-final-integrated.log.
- Main rechecked before delivery: ec0138e remains current. Version 3.11.0 (47),
  schema 12; release notes dated 2026-10-24. Device QA and actual execution of
  the 14 platform codec checks remain unverified; deliver as a draft PR.
