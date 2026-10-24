# Combined review packet

Review the completed primary branch against origin/main, including both features
and their interaction. The user requested Part A and update checks in one PR;
PR50's closure must be preserved. Root opens one new combined PR on the same
authorized implementation branch. Do not merge or publish from review.

Specs: docs/UNITS_BRIEF.md; original units/work handoff Part A + approved addendum;
docs/superpowers/specs/2026-10-23-in-app-update-check-design.md and its 7-task plan.
Rulings and tests: this directory's ledger, docs/work/units/, update plan ledger.

Priorities:

1. Units disabled/default/override-off preserves existing rows, custom/catch-up,
   homework timing, optional books and taught history. Missing/invalid starts and
   impossible windows must be visible and recoverable without losing pages.
2. Every valid linked chain finishes unit k before entering k+1, including a
   finished lead with a lagging follower, homework/skips, optional membership,
   pinned extras, deadlines, touched custom lessons and re-pacing/catch-up.
3. Revision identity: same-ID edits preserve teacher fields and homework pairs;
   remove touches only owned work. Editing a revision can retain its own valid
   reserved future date, while rejecting other blockers, taught/past dates and
   protected custom/pinned/supplementary content. No-slot and NONE states work.
   PAGES revisions support ordinary or supplementary book pages as specified,
   retain and expose those pages where the lesson is taught, and validate strict
   supplementary includes. Chosen revision dates keep the expected unit order
   when window lengths are recomputed, or produce an explicit correctable issue.
4. Global Done/revision routing covers a non-active class from Home, queues and
   dismisses safely, preserves active navigation, and gives proper Undo. Unit
   position/end actions and warnings remain available for taught/straddled units.
5. Update throttling, debug exclusion, cancellation/failure and live-settings
   result merge must match the pure checker contract in actual VM wiring.
   Trusted URL validation applies to network and stored values; only INTERNET.
6. Combined Settings/nav/drawer/Home UI keeps both additions, all route cases,
   tablet behavior, installed-version deduplication, accessibility labels and
   resource/token boundaries. Signing/dependencies/applicationId/minSdk stay fixed.

Native scope: no connected device/emulator/KVM. No screenshots or native detector
evidence; source review cannot approve runtime Back/recreation/IME/TalkBack,
large text, tablet clipping, gestures or animation quality. Record these as
unverified, not a visual pass. Manual update Task6 remains not run with checklist.

Return actionable findings graded by user impact with exact file/line evidence,
separate minor deferrals and declined runtime judgments. Root applies one tested
material fix pass and runs final integrated gates before publishing.
