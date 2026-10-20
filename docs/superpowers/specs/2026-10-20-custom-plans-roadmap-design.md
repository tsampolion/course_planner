# Custom plans, lead book, pace warnings and the Plan | Stats split: design

Agreed with the owner on 2026-10-20 in a brainstorming session. The brief for teachers' words is `docs/CUSTOM_PLANS_BRIEF.md`; the PR1 implementation plan is `docs/superpowers/plans/2026-10-20-plan-stats-split.md`. Later PRs get their own plan when they start.

## Context

The owner, a teacher, wants the engine's proposed schedule to be a **template**: they build **their own plan** from it, saved alongside the proposed one without changing it, and the whole toolkit works on it. Follow-up notes add three things: pace warnings that come with a catch-up proposal, a **lead book** that drives the other books' pacing (proportional, with optional unit links), and moving group statistics off the main plan view. This is four features, so it ships as **4 PRs in order**, each with its own version, tests and CHANGELOG. Engine parity is the contract: with no custom plan, no lead book and no catch-up applied, every schedule stays row for row as today (`PacerTest` unchanged).

## Decisions agreed in brainstorming

| # | Decision |
|---|---|
| 1 | A custom plan is an **ordered list of lesson contents**. Dates are never stored; they come from the live calendar, so a snow day shifts the content and keeps it. |
| 2 | Edits: lesson pages, insert/remove lessons, drag to reorder, per-lesson notes. |
| 3 | When a group has a custom plan it is **active**. A "My plan \| Proposed" switch shows either, and "Use proposed plan" makes the proposed one active without deleting the custom one. |
| 4 | Automation: **watch plus one-tap fixes**. A coverage strip shows problems, nothing changes by itself, and every fix is one undo step. |
| 5 | **One custom plan per group**, plus "Copy to another group". |
| 6 | Done lessons are shared by both plans (a fact, not a plan). |
| 7 | Lead book: **proportional follow, with optional unit links** as anchors. |
| 8 | Catch-up speed is **picked in the warning** with a stepper (+1/+2/+3 pages a lesson), and the lessons needed update live. |
| 9 | A book is **left behind** when it lags by more than **2 lessons** of its own pace (editable per class). |
| 10 | Order: PR1 Stats split → PR2 custom plans → PR3 lead book → PR4 warnings and catch-up. |

## Step 0: docs (first commit, before any code)

- Write `docs/superpowers/specs/2026-10-20-custom-plans-roadmap-design.md` with this plan's decisions and the four-PR design.
- Write `docs/CUSTOM_PLANS_BRIEF.md`, and add §3 Custom plans, §4 Lead book and §5 Pace warnings to `docs/PLANNED_FEATURES.md` with status "approved, see brief".
- Add a note to Focus blocks (§1) saying the lead book and custom plans cover part of it, and that it stays unapproved.
- Branch `claude/youthful-knuth-mz8116`, one PR per sub-project. After each one merges, restart the branch from `main` as the workflow rules require.

---

## PR1 — Plan | Stats on the group screen (2.6.0, versionCode 27, no schema change)

File: `app/src/main/java/gr/edusuite/pacer/ui/GroupScreen.kt` (split into `GroupScreen.kt` and a new `GroupStats.kt` so neither file grows).

- A `SingleChoiceSegmentedButtonRow` (Material 3, already approved) under the top bar: **Plan | Stats**. The choice is kept per group with `rememberSaveable(g.id)`, and switching uses `rememberSegmentHaptic()` (`ui/Motion.kt`).
- **Plan** (the default):
  - the hero next-lesson tile (from today's `SummaryCard`: Mark done, Edit pages),
  - a compact **status chip**: "On track" or "Short by 1.5 lessons" in warn colours; tapping it opens Stats, so warnings stay visible,
  - the schedule header (Copy and Share), the All months / By month chips, and the lessons,
  - the day strip ("Term weeks") and the hints, each behind a collapsible `MoreOptions` toggle (`ui/DashboardComponents.kt`). The open state is a per-viewer convenience held in `rememberSaveable`.
- **Stats**: the three `StatTile`s, `VerdictBox`, the Details stats, `MonthBars` and `Positions`, moved unchanged as composables.
- Motion: `AnimatedContent` between the two tabs, with a horizontal slide and fade from the `Motion` tokens in `ui/theme/Shape.kt`. It snaps when `animationsEnabled()` is false.
- Strings go in `res/values/strings.xml` (`group_tab_plan`, `group_tab_stats`, `group_status_on_track`, `group_status_short`, `group_term_weeks_toggle`).
- Accessibility: the tabs carry selected semantics, and the chip has a click label ("Open stats").
- Updates: CHANGELOG `## [2.6.0]` with its compare link, the README section if it describes the group screen, and `docs/UI_BRIEF.md` if it lists the layout.

## PR2 — Custom plans (2.7.0, `JsonCodec.VERSION` = 5)

### Model (`model/Model.kt`)
- `Group.custom: CustomPlan? = null`
- `data class CustomPlan(val active: Boolean, val lessons: List<PlannedLesson>)`
- `data class PlannedLesson(val id: String, val pages: Map<String, List<Int>>, val note: String = "", val touched: Boolean = false)`

### Engine (new `engine/Custom.kt`; `Pacer.kt` untouched apart from an interface)
- `interface Schedule { plan; group; rows: List<Row>; lessons; teaching; pages; peak; rate; bookStats }`. `GroupReport` implements it unchanged. `Row` gains an optional `note: String = ""` and `planned: PlannedLesson? = null`, with defaults so existing callers compile.
- `fun GroupReport.toCustomPlan(): CustomPlan` copies the teaching rows into untouched lessons (`newId()` from `model/Drafts.kt`).
- `class CustomReport(plan, group) : Schedule`:
  - it pairs lesson *k* with the *k*-th date of `GroupReport.teachingDates`, and keeps test-blocked lessons as `blocked` rows in date order;
  - `coverage`: `missing` (`remainingPages` not in any lesson), `doubled`, `stale` (in a lesson but not in `remainingPages`), and `overflow` (lessons past the last teaching date);
  - `peak`, `rate` and `verdict()` reuse `Verdict`.
- `fun Plan.scheduleFor(g): Schedule` returns `CustomReport` when `g.custom?.active == true`, and `GroupReport` otherwise.
- Ticking: `CustomReport.markDone(dates)` stores `DoneLesson`s from the lessons' pages and removes those lessons, then calls `settleNextLesson`. `unmarkDone` puts a `PlannedLesson` back at the index of its slot (pages from the `DoneLesson`, `touched = true`). `recordLesson` keeps today's behaviour, and pages left out show up as `missing`.
- `fun CustomPlan.repaceFrom(report, k): CustomPlan`:
  - pool = missing pages plus the pages of untouched lessons from *k*;
  - untouched lessons past term end are dropped first;
  - each book's pool is spread with `allocatePages` over the untouched lessons from *k* that fall inside the term, honouring `bookLessons` finish-by dates;
  - touched lessons and lessons before *k* never change.
- Tidy (`dropStale`): removes stale pages and untouched lessons that end up empty.
- `copyTo(target: Group)`: copies the lessons as untouched, drops pages the target has already taught, then runs repace from 0.

### Data
- `JsonCodec`: write `custom` only when it is set. Version 1–4 files read as no custom plan. Add a round trip to `PlanStoreTest`.
- `Sanitise.kt`: clamp pages to the book's range and drop lessons for unknown books.
- `Backup.addYears`: no change, since ids are per group.
- `PacerViewModel`: `createCustomPlan`, `setCustomActive`, `deleteCustomPlan` (5 s Undo snackbar), `editPlannedLesson` (tagged per field so typing merges), and `insertLesson`, `removeLesson`, `moveLesson`, `repaceFrom`, `tidy`, `copyCustomTo` (all untagged, because they are structural). `removeBook` drops that book's pages from the lessons.

### UI (new `ui/CustomPlanScreens.kt`, plus the group screen)
- Plan tab with no custom plan: a GuideCard, **"Create your custom plan"** ("Start from this schedule. The proposed plan stays as your fallback."). On tap, the rows lift in a staggered sequence (a 30 ms step via `Motion`) and gain an accent edge, with a confirm haptic.
- With a custom plan:
  - a segmented **My plan | Proposed** switch. Rows `animateItem()` between the two. On Proposed, a "Use proposed plan" / "Make my plan active" action, and rows that differ get a small accent dot plus a "Proposed: SB 20–22" ghost line in My plan;
  - lesson cards with a drag handle. Long-press drag is built by hand with `detectDragGesturesAfterLongPress` on the `LazyColumn` item offsets (no new dependency): a tick haptic at every slot crossed, and the dates stay with their slots;
  - a "+" between cards on long-press opens "Insert lesson" (a revision or a note); swipe or menu removes a lesson and its pages merge into the next one;
  - the editor sheet: one field per book (`lessonPages` from `Pacer.kt`, inline errors as in `PagesTaughtDialog`), a note field, and "Planned: …";
  - the coverage strip: a thin bar per book with segments for covered, missing, doubled and stale, each animating to new widths. Each problem chip opens its fix ("Re-pace from lesson …", "Tidy", "Fit to term"), and every fix is one undo step with a snackbar;
  - notes show on the rows, on Home's hero tile (`HomeModel.kt` reads `scheduleFor`), in Print (`PrintSheet.kt`) and in the TSV (`Format.kt`, as a note column only when one is present).
- `HomeModel`, `OverviewScreen`, `PrintSheet`, `Format` and `Plan.problems()` all switch from `GroupReport(plan, g)` to `plan.scheduleFor(g)`. `Nav.fixFor` gains "My plan:" problems → `Screen.Group`.

### Tests (new `CustomPlanTest`, plus additions to `NavTest`, `PlanStoreTest` and `HomeModelTest`)
- Creating a plan equals the proposed rows; a snow day shifts content by one slot; a consuming test shifts it the same way.
- Tick and untick round trip; ticking out of order.
- Repace keeps touched lessons; repace from 0 on an untouched plan equals the proposed plan.
- Coverage: missing, doubled, stale and overflow.
- Copying to a group with other weekdays.
- Parity: `PacerTest` untouched and green.

## PR3 — Lead book (2.8.0, VERSION = 6)

- Model: `SchoolClass.lead: String? = null` (a book id) and `Book.units: String = ""`, unit start pages such as "1, 13, 25, 37", read with `parseRanges`.
- Engine (`engine/Lead.kt`):
  - The lead book is allocated as today. For a follower B with `n_B` remaining pages, lesson *i* ends at `round(c_i × n_B / n_L)` in integer half-up arithmetic, where `c_i` is the lead book's cumulative pages after lesson *i*.
  - **Unit links:** when both books have the same number of units, the mapping is piecewise. The lead book's position inside its unit *u* (as a fraction) maps to the same fraction of B's unit *u*. Otherwise it falls back to proportional, with a problem: "Units of Workbook don't match Student's book (6 vs 8)".
  - A follower with a finish-by date is paced on its own (the due date wins).
  - In a custom plan, editing the lead book's pages in a touched lesson re-flows the followers' pages in untouched lessons (`repaceFollowers`).
  - No lead book → exactly today's path (a parity test).
- UI: on the class screen, a "Lead book" choice (radio list) and a "Units" field per book; on the group screen, a "Leads" tag on that book's column; a hint explaining the follow rule.
- Tests (`LeadBookTest`): the proportional worked example, the unit-mapped example, a unit mismatch falling back, a due date overriding, and the parity case.

## PR4 — Pace and left-behind warnings with catch-up (2.9.0, VERSION = 7 for `SchoolClass.lagLessons: Int = 2`)

- Engine (`engine/Warnings.kt`):
  - `paceWarning(schedule)`: when `verdict()` is `Short`, deficit `d = cutPages`. For a boost `Δ` ∈ {1, 2, 3}, `lessons = ceil(d / Δ)`, feasible when `lessons ≤ teaching`, and the result is the date of lesson `lessons` (for example "back on pace by 12 Dec").
  - `bookLag(schedule)`: per book, `f = taught / total` (taught = done pages plus pages before the position, minus skips). The reference is the lead book's `f`, or else the median of the other books. `lagLessons = (f_ref − f_B) × total_B / rate_B`, and the book is flagged when that is above `lagLessons`.
  - The new allocation helper `allocateWeighted(pages, weights)` makes lesson *i* end at `round(W_i × n / W)`. With equal weights it equals `allocatePages` (a property test), and the catch-up uses weights `s+Δ` for the first *k* lessons and `s` afterwards.
- UI (`ui/WarningSheets.kt`):
  - Tapping the status chip from PR1, or a warning banner on Plan, opens a full-height sheet showing the deficit, a **+1/+2/+3 stepper** that recomputes the lessons and date live (`AnimatedFigure`), and a preview of the first affected lessons.
  - **Apply catch-up** writes the result into the custom plan, creating one first with a confirm step if the group has none. It is one undo step.
  - The left-behind sheet says "Workbook is 3.2 lessons behind", previews folding its backlog into the next N lessons, and applies it to the custom plan.
  - The threshold goes in the class settings.
- Tests (`WarningsTest`): the deficit and lessons for each Δ, an infeasible case, the lag against the lead book and against the median, the `allocateWeighted` equality, and that applying a catch-up leaves the proposed plan unchanged.

## Verification (every PR)

- `./gradlew testDebugUnitTest` (all tests, including `NoHardcodedTextTest`, `ContrastTest` and the parity tests in `PacerTest`), `./gradlew lint` and `./gradlew assembleRelease`, all green before pushing.
- Reproduce on JVM tests each worked example from the spec.
- This container has no emulator, so the owner checks each PR on a device with `installDebug`: the drag haptics, motion snapping with animations off, and TalkBack labels on the tabs and the chip.
- Open the PR as ready for review, subscribe to its activity, and drive CI to green. Each PR bumps `versionName`/`versionCode` and adds its CHANGELOG section with the compare link.
