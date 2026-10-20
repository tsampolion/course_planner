# Supplementary books (2.10.0): implementation plan

**Spec:** [2026-10-20-supplementary-books-design.md](../specs/2026-10-20-supplementary-books-design.md). **Brief:** [SUPPLEMENTARY_BOOKS_BRIEF.md](../../SUPPLEMENTARY_BOOKS_BRIEF.md). JSON version 8. Version 2.10.0 / versionCode 32. Approved by the owner on 2026-10-20.

- [x] Confirm all nine decisions with the owner, including the teacher-selected replacement dialog, individual pages/ranges, and Keep/Skip confirmation for 0 or unchanged core ranges.
- [x] Read repository rules, existing briefs/specs/plans, affected code and tests.
- [x] Write and self-review the brief, spec and plan. Link all three and list the pending brief in the docs index. No shipped banner or completed-build claim yet.
- [x] Owner approves the brief, spec and plan.
- [x] Baseline: main 2.9.0, versionCode 31, JSON 7; baseline unit checks green.
- [x] Test-first engine work (red phase: 79 compile errors before the engine existed; then 15 engine tests green):
  - Add `SupplementaryBookTest.kt` parity, proposed exclusion, on-top isolation, teacher-selected replacement worked example, restoration and protected-lesson cases. Add the small required supplementary regression to PacerTest without altering existing cases or expectations.
  - Add preservation tests for re-pace/catch-up/fold, touched/overflow lessons, tidy/fit, group independence, modes, counting, tick/untick, copied/merged lessons, strict include validation and invalid atomic saves.
  - Run targeted tests and observe failures caused by the missing feature; record that red phase before implementation.
- [x] Model (`model/Model.kt`): append defaulted Book supplementary/include/countInLesson and PlannedLesson inLesson/replacementBase. Add coreBooks/supplementaryBooks helpers. Keep DoneLesson unchanged.
- [x] Engine (`engine/Supplementary.kt`):
  - Strict bounded include parser, derived first/last, included/unplaced/available-page helpers and per-group statistics.
  - Pure full-draft validation and batch placement operation with explicit final core maps, shared saved baseline, multi-placement transitions and restoration review.
  - Resolve generated lesson ids from date/index once when creating the base; activate on Save only. No automatic proportional deduction or page-count equality rule.
  - Write the target as touched, then re-pace from the following index once; only an explicit Save changes core allocation. Keep coverage problems if no untouched future capacity exists.
- [x] Existing engine hooks:
  - Pacer: core-only allocation/totals/banked/peak/problems; supplementary remainingPages empty; preserve plan.books indexing with empty proposed cells and zero stats placeholders.
  - Lead: supplementary books cannot lead/follow or enter unit checks.
  - Custom: core-only coverage and redistribution, retain supplementary maps/modes/baselines, protect touched lessons, preserve overflow placements in re-pace and fit, copy supplementary content with per-group filtering, handle merge baselines safely.
  - Warnings: core-only progress/reference/backlog and catch-up allocation; keep supplementary contents and inLesson, including untouched/overflow lessons. Leave allocateWeighted unchanged.
- [x] Data (`JsonCodec.kt`, `Sanitise.kt`): JSON 8 with optional book fields, inLesson and nullable replacementBase (empty object distinct from absent); keep v1–7/web reading and lagLessons. Update KDoc. Sanitise ids, bounds, baseline keys and invalid leads without re-pacing. Check codec symmetry; report reasoning or a temporary uncommitted real-org.json test accurately, with no dependency added.
- [x] ViewModel (`PacerViewModel.kt`): supplementary creation, tagged strict include edits, default mode, one untagged batch lesson Save via editLessons, single-placement wrapper, generated-target resolution and active custom-plan handling. Clean inLesson/baselines on book removal. Preserve actual-page recording. Exclude supplementary homework at mutation boundary.
- [x] Class UI (`ClassScreens.kt`): supplementary switch, strict include input/error, derived page count, supplementary tag and default-mode control; hide skip/due/units, core-only lead and homework pickers. Use shared haptics.
- [x] Lesson UI (`CustomPlanUi.kt`, `GroupScreen.kt`):
  - Supplementary fields, availability hints, saved/default modes and focused row-menu entry, including groups without custom plans.
  - All-books replacement dialog edits pages to teach, supports ranges/singles/0, queues Keep/Skip/Cancel decisions for unchanged non-empty sets, and stages its result until outer Save. Preserve draft on rotation.
  - Multiple in-lesson books share one core allocation/baseline; removal and switching review restoration. No new undo entries on dialog Apply or cancellation.
  - PagesTaughtDialog validates included pages; done rows display facts without counting tags. Planned supplementary rows carry mode tags. Quiet unplaced counts appear even without a custom plan.
- [x] Stats and output (`GroupStats.kt`, `HomeModel.kt`, Home UI, `Format.kt`, `PrintSheet.kt`): supplementary placed/total/taught without pace; correct named pages in both hero surfaces; output columns only when used per output table; preserve original book indices, notes, blocked events and colspans.
- [x] UI copy/motion: strings.xml resources and plurals, accessible fields/actions, existing SegmentedToggle and shared haptics/Motion/Space, snap with animations off. No new dependencies or icons.
- [x] Integration tests: strict include problem → Class in NavTest; Home, TSV and Print alignment/conditional columns; sanitised modes/baselines. Test pure Keep/Skip/Cancel draft decisions, multi-book batching and restoration using existing JVM facilities. Keep PacerTest, CustomPlanTest, LeadBookTest and WarningsTest existing expectations intact.
- [x] Docs and version: 2.10.0 / 32, dated CHANGELOG and compare/Unreleased links; one README Features bullet; CLAUDE.md JSON 8, fourth extension and noSupplementaryChangesNothing; docs index; shipped brief banner; any PLANNED_FEATURES entry marked shipped. Cross-check docs against final implementation.
- [x] `./gradlew testDebugUnitTest lint assembleRelease` green before every push; no new lint warnings in changed files. Record actual test counts and JSON-check method. Review the diff for scope and unchanged protected files.
- [ ] Open one PR to main: `feat(class): supplementary books (2.10.0)`. Sections: What teachers get, How it works, Docs, Checks. Include phone checklist and “not checked on a device” until verified. Drive CI to green; do not merge or tag automatically.
- [ ] Checked on a device by the owner.
