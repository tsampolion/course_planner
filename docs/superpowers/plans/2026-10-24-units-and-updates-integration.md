# Units and update checks integration plan

> Use Superpowers TDD and independent implementation domains, then one fresh
> combined review. Publish only the authorized update-check implementation branch.

**Goal:** deliver Part A and the supplied update-check plan together without
changing unrelated behavior or losing any prior group/homework feature.
**Specs:** original units/work handoff Part A and approved addendum;
`2026-10-23-in-app-update-check-design.md` and its supplied 7-task plan.

- [x] Read requirements and reconcile engine/schema/branch/version constraints.
- [x] Merge/check current main and run baseline suite before implementation.
- [x] A1: units model, schema 14, old-file defaults, sanitising and parity tests.
- [x] A2/A3: strict linked-unit windows, revisions, progress and warnings across
  proposed/custom/re-pace/catch-up, preserving taught/touched content.
- [x] A4–A6: class/creation settings, unit starts and optional links, group override,
  revision prompt/edit/remove, lesson position/end rows, stats/home unit progress.
- [x] Update tasks 1–7: pure model, settings, checker, badges, Settings updates,
  manual-device checklist (Task6 NOT RUN), docs/release contract; per-task tests and commits retained.
- [x] Integrate shared VM/UI/resource/docs changes; version 4.1.0 (49), schema 14.
- [x] Finish final unit, lint, release and platform-test builds after the tested
  material fix pass. Fresh reviewer stopped before its consolidated report; root
  integration checks completed. Native checks declined without a device.
- [x] Prepare one combined draft PR preserving owner-closed PR50, attribution,
  deviations and pending-device results. Publish only the authorized branch and
  attach/link the created PR in the task handoff. Do not merge or tag.

Review focus: disabled-unit parity and existing homework timing; unit-boundary
feasibility with skips/taught/custom/optional books; revision identity and future
lesson eligibility; update throttling/debug exclusion/trusted URLs/concurrent
settings edits; navigation/tablet/large text/accessibility; unchanged signing and
only INTERNET permission. Source compilation does not certify native visuals.
