# Pace and left-behind warnings with catch-up (2.9.0): implementation plan

**Spec:** `docs/superpowers/specs/2026-10-20-custom-plans-roadmap-design.md`, PR4. **Brief:** `docs/CUSTOM_PLANS_BRIEF.md`, decisions 8 and 9. JSON version 7.

- [x] Engine (`engine/Warnings.kt`):
  - `allocateWeighted(pages, weights)` ends lesson *i* at `(2·W_i·n + W) / (2·W)` and equals `allocatePages` when the weights are equal.
  - `catchUp(boost)` works from `Verdict.Short.cutPages`: lessons = ⌈behind / boost⌉, feasible when they fit the term, and `until` is the day the group is back on pace.
  - `catchUpPlan(boost)` weights the first lessons at pace + boost and the rest at pace, over the untouched lessons in the term.
  - `bookLags(reference, threshold)` measures (f_ref − f) × pages / rate against the lead book, or else the median of the other books.
  - `foldBacklog(bookId, over)` front-loads one book's backlog.
  - Touched lessons never change, and the proposed plan is never written.
- [x] Model and data: `SchoolClass.lagLessons` (default 2, kept between 1 and 20) and `Plan.lagLessons`. `JsonCodec.VERSION` = 7, writing the threshold only when it isn't the default.
- [x] `PacerViewModel`: `applyCatchUp` and `foldBacklog` (each one undo step, making a custom plan if there is none) and `setLagLessons`.
- [x] UI:
  - `ui/WarningSheets.kt`:
    - `WarningBanner`;
    - `Warnings` on the Plan tab (behind the pace, and each book left behind);
    - `CatchUpSheet`: +1/+2/+3 toggle, a live lessons figure and back-on-pace date, a warning with a bigger step when even that isn't enough, and a preview of the first lessons;
    - `FoldSheet`: over 2, 4 or 6 lessons, with a preview of that book's pages.
  - The banners stand down once an active custom plan fits every page into the term. The threshold field sits in the class screen's Lessons sheet.
- [x] Tests (`WarningsTest`):
  - equal weights match `allocatePages`, and unequal weights;
  - catch-up figures for +1/+2/+3, and an infeasible catch-up;
  - catch-up plans front-load and keep touched lessons;
  - lags against a reference book and against the median;
  - fold moves pages earlier.
- [x] 2.9.0 / versionCode 30, CHANGELOG, README, CLAUDE.md, CUSTOM_PLANS_BRIEF, PLANNED_FEATURES §5.
- [x] `./gradlew testDebugUnitTest lint assembleRelease`.
- [ ] Checked on a device by the owner (`installDebug`).
