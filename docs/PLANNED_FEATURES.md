# Planned features

Ideas that have been discussed and specified but are not approved or built. Each entry describes the problem, a proposed design that fits the app's rules, a worked example, and the questions to settle before building it.

How to use this file:

- **Nothing here is approved yet.** Build an entry only when the owner asks for it in a session. Several entries change the pacing engine, which `CLAUDE.md` treats as a contract ("Engine parity is the contract"), so they need explicit sign-off.
- When an entry is picked up, ask its open questions first, write a `docs/<NAME>_BRIEF.md` with the answers folded in (as `docs/LESSON_PAGES_BRIEF.md` does), and remove the entry from this file.
- Entry 2 (Tidy and Fit) is built and recorded in [`TIDY_FIT_BRIEF.md`](TIDY_FIT_BRIEF.md).
- Entry 8 (backup) is built and recorded in [`BACKUP_BRIEF.md`](BACKUP_BRIEF.md); its number is not reused.
- Entry 7 (print) is built and recorded in [`PRINT_BRIEF.md`](PRINT_BRIEF.md); its number is not reused.
- Entry 3 (save status) is built and recorded in [`SAVE_STATUS_BRIEF.md`](SAVE_STATUS_BRIEF.md); its number is not reused, so the other entries keep theirs.
- Entry 4 (one today for the engine) is built and recorded in [`ONE_TODAY_BRIEF.md`](ONE_TODAY_BRIEF.md); its number is not reused either.
- Entry 5 (removed books keep their taught history; schema-15 tests) is built and recorded in [`REMOVED_BOOK_HISTORY_BRIEF.md`](REMOVED_BOOK_HISTORY_BRIEF.md); its number is not reused either.
- Entries 2 to 9 come from the usability audit of 25 October 2026. That audit was read-only and covered 0.4.1.3 and 0.4.2.0. Every finding was checked again against 0.4.4.0. Findings keep their audit IDs (F01 to F19) and open decisions keep theirs (I1 to I6), so they can be traced.
- The audit's visual, accessibility and streamlining findings (F09 to F12 and F16 to F18, with I1, I3, I4 and I5) are now in the approved [`UI_AUDIT_BRIEF.md`](UI_AUDIT_BRIEF.md) and [`STREAMLINING_BRIEF.md`](STREAMLINING_BRIEF.md). This file keeps the parts that change data, the engine, storage or output, which those briefs do not cover.
- Most of entries 2 to 8 fix existing behaviour rather than add features. They wait here because they need the owner's go-ahead, and some need an engine decision.
- Entries 11 (tablet layout) and 12 (help) came out of UI PR 4.
- [Order of work](#order-of-work) at the end merges these entries with the briefs' fix pull requests.

Evidence labels used in entries 2 to 9:

- **Source**: the code path has been read and holds, but nothing was run on a device.
- **Hypothesis**: a design choice that may cause friction. Only teachers can confirm it.
- **Not run**: needs a device, TalkBack or a PDF, none of which the build environment has.

## 1. Focus blocks: one book on its own for a run of lessons

**Status:** proposed (discussed while building 1.5.0). Not approved. It's an engine extension. Custom plans and the lead book cover part of it (see the architecture): a custom plan can give a run of lessons to one book by hand.

### The problem

A teacher wants to teach one topic straight through from one book, then go back to all the books. For example: the present perfect tenses take three units of the grammar book (12 pages). The teacher wants to finish those 12 pages first, teaching nothing from the Student's book or the Workbook, and then carry on with all three books at a regular pace.

### What the app does today

- The schedule gives every book pages in every lesson, so it can't plan a run of lessons for one book.
- **Workaround since 1.5.0:** after each focus lesson, long-press it, choose "Edit pages taught…", type the grammar pages and clear the other books. Those grammar pages leave the schedule and the other books' planned pages go back to be taught later. Once the block is over, the schedule is right.
- Limits of the workaround:
  - **No look-ahead.** During the block, the lessons still to come keep showing pages from the other books. The pace, the heaviest lesson and the verdict don't know that more focus lessons are coming.
  - **Typing every lesson.** Each focus lesson takes clearing a field per book and typing the grammar pages.
- **The finish-by date doesn't help.** It applies to the whole book, so the grammar book's remaining pages would all be squeezed in before the date. The other books also keep getting pages during those lessons.

### Proposal

A **focus block** says: "for these lessons, teach only this book, these pages". Everything else waits until the block is over.

A block has:

- a **group** (or every group in the class; see the open questions),
- a **book**,
- a **page range** (e.g. 40–51),
- **when it runs**: a first lesson and a number of lessons (the recommended default) or an end date.

### Pacing rules

The allocation rule doesn't change: within each book, lesson *i* of *m* ends at `round(i × pages / m)` (`allocatePages`). The extension only changes **which lessons each book's pages are spread over**. The engine already gives each book its own lesson count: a finish-by date limits a book to the lessons on or before it (`bookLessons`).

1. **Block lessons.** These are the group's teaching lessons inside the block: lessons from `lessonsFor` that a test doesn't take whole.
2. **Chunk.** These are the focus book's pages left to teach (after skipped pages, homework and pages already taught) that fall in the block's page range.
3. **During the block,** only the focus book is taught. The chunk is spread over the block's lessons by `allocatePages`. Every other book gets nothing in those lessons.
4. **Outside the block,** every other page (the other books, and the focus book's pages outside the chunk) is spread over the teaching lessons outside any block, exactly as today. A finish-by date counts only the lessons outside blocks that fall on or before it.
5. **Page order.** The focus book's pages outside the chunk keep their page order. If the chunk isn't the next pages (the teacher jumps ahead to pages 40–51 while the book's position is 30), pages 30–39 are taught after the block, before 52 onwards (see the open questions).
6. **No blocks, no change.** A plan without blocks must schedule exactly as now, row for row. That's the parity test.
7. **Done lessons.** Ticking a block lesson stores its pages as usual (`markDone`). Recording other pages with "Edit pages taught…" works too. Taught chunk pages leave the chunk, and what's left of the chunk re-spreads over the block's remaining lessons. A block whose lessons are all done no longer affects anything.
8. **Block dates must not slide.** Done lessons drop out of `lessonsFor`, so "3 lessons from 1 March" must not be re-counted from the lessons that remain. Otherwise ticking the first block lesson would push the block one lesson later. The simplest fix is to store the block as dates. The add dialog asks for a number of lessons, works out the last date from the group's calendar at that moment, and stores the first and last dates. A day off added later then shortens the block instead of moving it, and the block screen shows "3 lessons, 1–8 Mar" so this is visible.

### Worked example

30 lessons left, sustainable pace 5, no tests. Books: Student's pp. 1–60, Workbook pp. 1–30, Grammar pp. 40–99 (60 pages). Block: Grammar pp. 40–51, the first 3 lessons.

| Lesson | Student's | Workbook | Grammar | Note |
| --- | --- | --- | --- | --- |
| 1 | — | — | 40–43 | focus |
| 2 | — | — | 44–47 | focus |
| 3 | — | — | 48–51 | focus |
| 4 | 1–2 | 1 | 52–53 | |
| 5 | 3–4 | 2 | 54–55 | |
| 6 | 5–7 | 3 | 56 | |
| … | | | | |
| 30 | 59–60 | 30 | 98–99 | |

- After the block, Student's spreads 60 pages over 27 lessons (2.22 a lesson), Workbook 30 over 27 (1.11) and Grammar 48 over 27 (1.78): 5.11 pages a lesson, against 5.00 without the block. The block lessons carry 4 pages each.
- Without the block, lesson 1 would be Student's 1–2, Workbook 1, Grammar 40–41, and lesson 30 is the same in both.
- The overall required pace is unchanged (150 pages over 30 lessons = 5.00), because the block only moves pages around. The summary should add a line such as "After the focus block: 5.11 a lesson over 27 lessons", so the higher load later is visible before the block starts.

### Where it shows

- **Class screen:** a "Focus blocks" link next to Tests & worksheets and Homework, opening a list of blocks with an add dialog. The dialog takes group, book, pages from–to, first lesson (date picker, defaulting to the group's next lesson) and number of lessons. The number is pre-filled from the class pace (12 pages at a sustainable 4 a lesson = 3 lessons) and can be changed.
- **Group schedule:** block lessons carry a "Focus: Grammar" tag under the date, like tests, and the other books' cells show "—". Long-press could offer "Focus on one book from here…" as a shortcut to the add dialog.
- **Summary card:**
  - a hint such as "Lessons 1–3 focus on Grammar pp. 40–51",
  - the "after the block" pace line above,
  - the Positions table's pace column showing each book's pace outside blocks.
- **Copy, Share, Print:** block lessons show only the focus book, with "[Focus: Grammar]" in the first book column the way test names appear in the TSV.
- **Problems list** (`Plan.problems()`, also in Overview):
  - a block whose dates hold no lesson for its group (all closed, after the term ends, or every lesson taken by a test). The chunk then has nowhere to go; it should land in the first lesson after the block, as a missed finish-by date does today, with a warning.
  - a page range outside the book, or with no pages left to teach in it.
  - a chunk that needs more pages a lesson than the sustainable pace (12 pages over 2 lessons is 6 a lesson against a pace of 4).
  - a block that ends after the focus book's finish-by date.
- **Overlapping blocks** for the same group are refused by the add dialog (see the open questions). Back-to-back blocks are fine: Grammar for 3 lessons, then Workbook for 2.

### Data and code

- **Model** (`model/Model.kt`): `data class FocusBlock(id, groupId, bookId, from: Int, to: Int, start: LocalDate, end: LocalDate)`. `groupId` may be `ALL_GROUPS` if blocks can cover the whole class. `SchoolClass.blocks` and `Plan.blocks`, passed through by `SchoolYear.planFor`.
- **Storage** (`data/JsonCodec.kt`): `JsonCodec.VERSION` = 5 with a `blocks` list on each class; a missing list reads as none. Version 1–4 files and the web page's export keep loading. `data_extraction_rules.xml` needs no change (same file).
- **Engine** (`engine/Pacer.kt`):
  - Generalise the per-book allocation in `GroupReport.rows`. Today each book is spread over the first *m* teaching lessons (*m* from `bookLessons`) and padded with empty lessons. Instead, each book (and each chunk) gets an explicit list of teaching-lesson indices, its pages are spread over that list's length, and the result is mapped back to rows.
  - `BookStats` needs a clear meaning per book. Proposal: pages and lessons outside blocks, with the chunk reported separately, so the Positions pace column still means "pages a lesson from now on".
  - `afterNextLesson` (the web page's rule, used by the parity tests) stays as is.
- **View model** (`ui/PacerViewModel.kt`):
  - `addBlock` and `removeBlock` go through `editClass` with no tag, since they're structural edits.
  - `removeBook` and `removeGroup` also drop their blocks.
  - "Reset all positions" keeps blocks, because they are plans, not progress (open question).
- **UI:** a `Screen.Blocks` entry in `Nav.kt` (and its `NavTest` cases), the list and add dialog (in `ClassScreens.kt` or a new file), tags in `GroupScreen.kt`, and the block note in `Format.kt` (TSV) and `PrintSheet.kt`.
- **Version:** MINOR (1.6.0 or whichever is next). It keeps existing plans working, and schedules without blocks don't change.

### Tests to write (`PacerTest`)

- **`noBlocksMatchesTheWebPage`:** the seed plan and the existing fixtures schedule exactly as before.
- **`blockTeachesOnlyItsBook`:** the worked example above, row for row.
- **`blockRestMatchesSkippingTheBlockLessons`:** the lessons outside the block equal the schedule of a plan whose positions sit after the chunk and whose block dates are closed, as a cross-check.
- **`blockWithATestThatTakesTheLesson`:** the test lesson isn't a block lesson, and the chunk spreads over the rest.
- **`blockAndFinishByDate`:** a finish-by date counts only the lessons outside blocks.
- **`tickingInsideABlock`:** the block doesn't slide, taught chunk pages leave the chunk, and the rest of the chunk re-spreads. Recording other pages in a block lesson (1.5.0's `recordLesson`) behaves the same way.
- **`blockWithNoLessonsWarns`:** the chunk lands in the next lesson after the block, and `problems()` says so.
- **Chunk not next:** pages before the chunk come after the block, in page order.
- **JSON:** a version-5 round trip, and version-4 files reading as no blocks. This needs the same JVM `org.json` workaround as before, or a check on a device.

### Open questions (ask before building)

1. **Block length:** a number of lessons stored as dates (recommended), an end date, or worked out from the sustainable pace?
2. **Scope:** one group, a choice of groups, or the whole class? Per group fits the model best, since each group has its own lessons and positions.
3. **Overlaps:** refuse overlapping blocks for the same group (recommended), or let the later block win?
4. **Chunk not next:** when the chunk isn't the next pages of the book, should the pages before it come after the block (recommended), or count as skipped?
5. **Other books during the block:** nothing at all (recommended; homework already covers a little Workbook practice), or allow a small share?
6. **After the block:** spread the other books evenly over the lessons left, as proposed, or catch up first? Catching up would need a new allocation rule.
7. **"Reset all positions":** keep blocks (recommended) or clear them too?
8. **Display:** is a "Focus: Grammar" tag with "—" in the other columns enough, or should block lessons be shaded like test lessons?
9. **Relation to pinned lessons:** fixing the pages of a single future lesson was left out of 1.5.0 (`docs/LESSON_PAGES_BRIEF.md`, "Out of scope"). A one-lesson focus block covers part of that idea. Is that enough, or is a separate "plan this lesson's pages" still wanted?

## 6. Setup checks that match the rest of the app (F05, F06)

**Status:** proposed (audit P2). No pacing change. Not approved.

### The problem

**Add book (F05)** (`ui/ClassScreens.kt:416-438`):

- Add is enabled when Last is valid. First is ignored.
- A First of 0, blank or above `MAX_PAGE` (`model/Model.kt:13`, 100,000) silently becomes 1 (line 426).
- First 100 / Last 10 is saved reversed. The engine only reports it afterwards ("the last page is before the first", `engine/Pacer.kt:427`).
- The book editor has the same gap (`ClassScreens.kt:339-340`, `PacerViewModel.kt:827-868` only cap the maximum).
- The optional group book already does it right: inline errors and `lastN >= firstN` (`ui/GroupSettingsScreen.kt:235-252`), backed by `addGroupBook`'s `first !in 1..MAX_PAGE || last !in first..MAX_PAGE` (`PacerViewModel.kt:630`).

**Year day off (F06).** Year settings → Add day off → More options accepts From 20 Nov, To 10 Nov.

- Add only needs From (`ui/YearScreens.kt:196-200`). `addClosure` stores it as given (`PacerViewModel.kt:488-490`).
- The list shows "20 Nov – 10 Nov" (`YearScreens.kt:70-71`), while the engine closes the whole span either way round (`engine/Pacer.kt:144-153`).
- The group day-off sheet already refuses this (`GroupSettingsScreen.kt:193`), as does `withClosureEdited` (`model/GroupBooks.kt:33-37`).

### Proposal

- One shared page-range check, `1 ≤ first ≤ last ≤ MAX_PAGE`, used by Add book, the book editor, the supplementary page fields and the optional group book.
- Inline reasons on both fields, and Add disabled until the range is valid. Imported invalid data keeps today's warning.
- The year day-off sheet reuses the group sheet's `to >= from` rule with an inline message. A reversed closure already stored keeps working; its row shows the effective span in order.
- Count only measured groups (`short == 0`) as on pace. Show unmeasured groups as "set-up needed", or use a neutral "See all N groups ›". Update the two tests.

### Alternative

- For ranges: offer an explicit suggested correction ("Swap to 10–100?") that the teacher must accept. Never correct silently.

### Acceptance

- Blank, boundary, invalid and reversed inputs never change data or close the sheet. Accepted values are stored exactly.
- Ordinary and supplementary paths are tested separately.
- Single-day, open-ended and valid ranges still work; a reversed range is disabled.
- Scope, Greek holiday deduplication and Undo are unaffected.
- With null-only data, Home never says "on pace", and mixed null, zero and behind totals are right. Every group stays reachable, and 100% and 200% text agree.
- Parity is unchanged.

### Risk

- **Fits with:** the range and day-off checks fit UI fix PR 6 (set-up screens) in [`UI_AUDIT_BRIEF.md`](UI_AUDIT_BRIEF.md).
- **Ranges:** low to medium.
- **Year closures:** medium, because a closure affects every group under it.
- **Rollback:** two independent reverts, no data change.

## 9. Device and accessibility check pass

**Status:** proposed. Not approved. It is a verification pass, not an app change. `scripts/device-check.sh` (0.4.4.5) already automates the theme, text-size, width and process-death screenshots for whichever screen is open; the palette, TalkBack and midnight checks stay manual. Its results feed the fix pull requests in [`UI_AUDIT_BRIEF.md`](UI_AUDIT_BRIEF.md), whose own phone checks are the short form of this matrix.

### Why

- The audit had no Android SDK, device or emulator. So:
  - no screenshots, semantics tree, touch bounds, TalkBack, 200% rendering or PDF output were checked;
  - nothing on performance or end-to-end runtime was checked.
- Its passing test run was a subset: 137 engine and model tests, compiled by hand against 0.4.1.3, with `CustomPlanTest` excluded. CI on `main` is the real record.
- Every "Source" finding above still needs a reproduction on a device.

### Synthetic data (never a real teacher's records)

- Invented classes such as A Junior, B Senior, FCE and Proficiency: at least 8 classes and 15 groups meeting 2 or 3 times a week.
- Greek names, Greek public holidays and a group snow day.
- Core books and workbooks, plus supplementary and optional books, one lead book and units.
- Paired homework, tests and worksheets, and an extra lesson.
- Done and overdue lessons, and a touched custom plan that overflows the term.

### Matrix

- **Themes:** both palettes, light and dark.
- **Devices:** a compact phone, a large phone and a tablet, in portrait and landscape.
- **Text:** 100%, 150% and 200%.
- **Widths:** 599, 600, 839, 840 and 1024dp, plus split screen.
- **Capture:** real screenshots, merged and unmerged semantics, and touch bounds.
- **Input:** TalkBack, a keyboard where available, switch access and gesture alternatives (every long-press and swipe has a named action), the keyboard up, Remove animations, and system haptics.
- **Lifecycle:**
  - midnight, resume and time-zone changes, including Europe/Athens summer time;
  - an open sheet while the year or group changes, and process death;
  - cancelled and failed file pickers;
  - empty, completed, invalid and large data;
  - print and PDF with 1, 4 and 8 books.
- **Consistency:** after every cross-screen action, compare the class, group, date and plan source shown.

### Coverage gaps to close with tests

- Failed writes and retry ([`SAVE_STATUS_BRIEF.md`](SAVE_STATUS_BRIEF.md)).
- Midnight across screens: the engine side is covered (`PacerViewModelTest.midnightMovesPlansBankingAndRevisionDatesTogether`); the Compose screens are not.
- Shared-control semantics (UI audit X12, X13).
- Process and sheet restoration (streamlining S4).

What the existing tests do not prove:

- **Deep JSON.** The A4 fake-file tests inject a `StackOverflowError` but do not decode really deep JSON.
- **The platform suite** (`app/src/androidTest`) is a codec suite only. It has no Compose UI, semantics or workflow tests.
- **D4's long-press sheets.** The group book options and own-day-off Edit/Delete need a device check that TalkBack and switch access reach them, not only the long press.
- **Gestures and speed.** System Back and predictive back, the system bars and gesture navigation, and a startup or large-data frame benchmark were not run. Don't infer speed from file size.

## 10. Automatic UI tests (Compose robot tests)

**Status:** proposed. Not approved. It adds one test-only library, so it needs the owner's approval (CLAUDE.md, approved dependencies).

### The problem

- Unit tests check the engine and the models. Nothing checks the screens: that Done is there and works, that every control has a TalkBack name, or that large text keeps the important parts on screen.
- So every pull request asks the owner to check by hand on a phone. A screen bug fixed once can come back unnoticed.

### Proposal

- Add `androidx.compose.ui:ui-test-junit4` (version from the existing Compose BOM) to `androidTestImplementation`, and `ui-test-manifest` to `debugImplementation`. Declare them in `gradle/libs.versions.toml`.
- Each test opens one screen with synthetic data, acts as the teacher would (`onNodeWithText("Done").performClick()`), and checks what is shown or what was stored.
- Tests grow with the work: each fix pull request in the Order of work adds tests for the journeys it touches. Start with:
  - tick a lesson and undo;
  - record pages taught;
  - a failed save shows its notice (entry 3);
  - every control on group home has a name;
  - 200% text keeps today's pages.
- **CI:** they run in the `device-tests` job, which already runs the 16 codec tests on a Gradle Managed Device (0.4.4.7). New tests there make that job somewhat longer.

### Alternative

Run the tests only locally (`./gradlew connectedAndroidTest` with the emulator open) and keep CI as it is. This is cheaper, but it relies on someone running them.

### What it does not cover

How screens look. That stays with `scripts/device-check.sh` and the owner's eyes. Screenshot comparison tests would be another library and a separate decision.

### Acceptance

- The tests pass locally against an emulator, and in CI if chosen.
- Each test fails when the behaviour it guards is broken. Prove this once by breaking the behaviour on purpose.
- No production code changes just to make it testable, beyond adding semantics that TalkBack needs anyway.
- CI time is recorded before and after.

### Open questions

1. Approve `ui-test-junit4` and `ui-test-manifest` as test-only dependencies?

## 11. Tablet layout: a redesign of its own

**Status:** proposed (owner's request while building UI PR 4). Not approved.

### The problem

A tablet in landscape has far more room than a phone, but the app still shows phone screens in panes: Pacing overview beside one group, and a 320dp menu. Pane widths shrink when the menu is open (audit X15), most screens stretch to the full pane, and nothing uses the width to show more at once.

### What 0.4.9.0 did

Only the menu: from 840dp it can be pinned open (the default, as before) or unpinned so it collapses behind ☰ and frees its 320dp.

### Proposal

Design the tablet as its own layout, not a wider phone. Ideas to settle with the owner:

- which screens show two or three panes side by side (for example Plan beside Stats, or Work beside the lesson);
- a working width for list screens and forms instead of full-pane stretch;
- whether the pinned menu is a narrower rail when the panes need the room;
- how it works in portrait, on foldables and in split screen.

### Open questions

1. Which tasks matter most on a tablet: planning at a desk, teaching with the screen in front of the class, or both?
2. Should the pinned menu stay at 320dp, shrink to a rail, or follow the panes?

## 12. Help: a place to learn how the app works

**Status:** proposed (owner's request while building UI PR 4). Not approved. Planned for a future release; nothing is built.

### The problem

The app explains itself in small pieces: the first-run guide, the Tips sheet in a group's ⋮ menu, and hints on empty screens. There is no one place a teacher can open to look something up, and the menu has no Help row.

### Proposal

A Help feature, reached from the ☰ menu. Its content and shape are not decided. Ideas to settle with the owner:

- a short list of how-to topics (set up a class, tick lessons, fit to term, back up) written for teachers, not as a manual;
- whether it reuses the first-run guide and the Tips text instead of repeating them (one home per fact);
- whether it also explains the warnings and the pacing verdicts in plain words.
- reusing the What's new `CardDeck` from [`FEATURE_MODULES_BRIEF.md`](FEATURE_MODULES_BRIEF.md) as Help's frame, and its module cards as topics.

### Open questions

1. Where does it sit in the menu: a sixth fixed row, or inside Settings? The five fixed rows are the owner's decision, so a new row needs a new answer.
2. Should it work offline only (the app's only permission is `INTERNET`, used for the update check)? Text inside the app avoids a new network use.
3. Which topics first?

## Order of work

The 4.1.0 audit plan has shipped every part, and its plan and spec were deleted in 0.4.4.1 (git history keeps them):

| Part | Release | Version code |
| --- | --- | --- |
| A, F | — | — |
| D | 0.4.2.0 | 53 |
| B | 0.4.3.0 | 55 |
| C | 0.4.4.0 | 56 |
| E | 0.4.4.1 | 57 |

The UI/UX audit and streamlining briefs were approved in 0.4.4.2 (58). What is left is the briefs' nine fix pull requests and the entries in this file.

The recommended sequence keeps data safety ahead of layout work, and each layout area is touched once. Each step is its own pull request with its own version bump; re-check `main` before each one.

1. ~~Entry 3, save status~~ (0.4.4.6, `SAVE_STATUS_BRIEF.md`).
2. ~~Entry 2, Tidy and Fit preservation~~ (0.4.4.8, `TIDY_FIT_BRIEF.md`).
3. ~~Entry 4, one today for the engine~~ (0.4.4.10, `ONE_TODAY_BRIEF.md`).
4. ~~Entry 5, history and schema-15 tests~~ (0.4.4.11, `REMOVED_BOOK_HISTORY_BRIEF.md`).
5. ~~**UI PR 1**, group home and the hero~~ (0.4.5.0).
6. ~~**UI PR 2**, Plan~~ (0.4.6.0, includes the book-label decision, I5).
7. ~~**UI PR 3**, Home today~~ (0.4.8.0, includes F16 (audit H1) and entry 6's F07 fix; lessons tick themselves from 0.4.7.0).
8. ~~**UI PR 4**, menu and top bar~~ (0.4.9.0).
9. ~~**UI PR 5**, one sheet frame~~ (0.4.10.0, includes F18's draft rule (streamlining S4), the Picker and SwitchRow semantics (audit X12, X13) and Print from Class and School year (M3)).
10. **UI PR 6**, set-up screens. It includes F17 (audit C2, streamlining S2) and entry 6's range and day-off checks.
11. **UI PRs 7 and 8**, Stats and Work; Settings, welcome and empty states.
12. ~~Entry 7, print~~ (0.4.6.1, `PRINT_BRIEF.md`). ~~Entry 8, backup~~ (0.4.6.2, `BACKUP_BRIEF.md`).
13. **UI PR 9**, polish and record. Run entry 9's full matrix first. It includes F12, the tablet panes (audit X15).

Test clean-up rides these steps rather than a separate audit (the suite has no duplicate tests, but some lock in wrong behaviour or check storage instead of what the teacher sees):

- Entry 2 revises `CustomPlanTest.tidyTakesOutPagesThatNeedNoTeaching`.
- ~~Entry 5 extends `GroupOverridesTest.removedBookKeepsDoneHistory`.~~ Done in 0.4.4.11.
- Entry 6 updates `HomeModelTest` (on-pace count) and `ScreensModelTest`.
- Entries 3 to 8 add the missing cases listed in each one.

Entry 10 (UI tests) can start with step 1: once it is approved, each later step adds tests for what it touches. Entry 1 (focus blocks) has no slot; build it only when asked. Do not start a parallel navigation or design system.
