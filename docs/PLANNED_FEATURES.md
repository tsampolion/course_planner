# Planned features

Ideas that have been discussed and specified but are not approved or built. Each entry describes the problem, a proposed design that fits the app's rules, a worked example, and the questions to settle before building it.

How to use this file:

- **Nothing here is approved yet.** Build an entry only when the owner asks for it in a session. Several entries change the pacing engine, which `CLAUDE.md` treats as a contract ("Engine parity is the contract"), so they need explicit sign-off.
- When an entry is picked up, ask its open questions first, write a `docs/<NAME>_BRIEF.md` with the answers folded in (as `docs/LESSON_PAGES_BRIEF.md` does), and remove the entry from this file.

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
