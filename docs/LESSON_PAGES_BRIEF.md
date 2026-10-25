# Task: Record the pages a lesson actually covered

Status: shipped in 1.5.0. Since 3.6.1, the menu action is "Record what I taught…" on today and earlier lessons; future custom lessons offer "Change planned pages…". [`ARCHITECTURE.md`](ARCHITECTURE.md) describes the current app.

This is the improved version of the request "let the teacher add custom page coverage in a lesson: Lesson 5 might plan pages 5–6 from the Student's book, 6–7 from the grammar and 8 from the workbook, but the teacher does 5–8 from the Student's, 6–8 from the grammar and leaves the workbook for the next lesson". The answers to the clarifying questions are folded in. Version 1.5.0 was built from it.

## Context

Read `CLAUDE.md` and `docs/BRIEF.md` first; their rules are binding. Since 1.4.0 a teacher can tick a lesson as done. A tick stores a `DoneLesson` with the pages the schedule planned for each book. The engine then drops that date from `lessonsFor` and those pages from `remainingPages`, and the pages still to teach re-spread over the lessons left. Real lessons rarely follow the plan, so the record and the rest of the schedule drift from what was taught.

## Goal

Let the teacher say, book by book, which pages a lesson actually covered, and re-pace the rest of the schedule from that.

## Acceptance example

Lesson 5 plans Student's 5–6, Grammar 6–7 and Workbook 8. The teacher records Student's 5–8, Grammar 6–8 and no Workbook. Then:

- Lesson 5 is done, shows 5–8 · 6–8 · —, and carries an "edited" tag.
- Student's 7–8 and Grammar 8 don't appear in any later lesson.
- Workbook 8 goes back to the pages still to teach and comes first in the next Workbook lesson.
- Pages left, the required pace, the heaviest lesson and the verdict all follow the recorded pages.

## Decisions (from the clarifying questions)

1. **Which lessons**: taught lessons only. Saving the editor marks the lesson done; lessons still to come stay automatic. No "pinned" future lessons.
2. **Re-pacing**: the existing rule. Pages a lesson missed stay in the queue (so they normally land in the next lesson), pages beyond the plan leave it, and every book's remaining pages re-spread so it still ends on the final lesson. No new allocation rule.
3. **Entry point**: the long-press menu only ("Edit pages taught…", also an accessibility action). The ✓ stays a one-tap "taught as planned", and rows don't react to a plain tap.
4. **Marking**: a small "edited" tag under the date of a lesson recorded with pages other than the plan's. It's stored on the done lesson, so the file becomes version 4.

## What to build

1. **Model.** `DoneLesson.edited: Boolean = false`.
2. **Engine** (`engine/Pacer.kt`, with tests in `PacerTest`):
   - `lessonPages(text, book)`: strict reading of what the teacher typed (`5-8, 10`, en dashes too, blank for none). Null when a part isn't a page or a range, or reaches outside the book's first–last pages.
   - `GroupReport.plannedPages(date)`: what the schedule gives that lesson, or for a done lesson what it would give if it were put back.
   - `GroupReport.recordLesson(date, pages)`: replaces or adds the done lesson with those pages. `edited` is true when they differ from `plannedPages`; saving a done lesson unchanged keeps its flag. Recording the planned pages equals a plain tick. Unknown books are dropped. Null for a day that isn't one of the group's lessons.
   - No change to `allocatePages`, `lessonsFor` or `remainingPages`. Ticking in order with no edits must still match the web page (`markingDoneInOrderMatchesTheOldRule`).
3. **Data.** `JsonCodec.VERSION` = 4; write `"edited": true` only when set; a missing flag reads as false. Version 1–3 files and the web page's export still load.
4. **UI** (`ui/GroupScreen.kt`): a "Lesson n · Day d Mon" dialog with one field per book, prefilled with the recorded pages (done lesson) or the planned ones, a "Planned: …" line under each and an inline error for unreadable or out-of-book pages (Save stays off). Saving calls `PacerViewModel.recordLesson`, one undo step. The tick hint mentions the new long-press item.
5. **Ship.** 1.5.0 (MINOR), `versionCode` 10, CHANGELOG, README, BRIEF and CLAUDE.md.

## Out of scope

- Planning a future lesson's pages in advance.
- Changing later done lessons when an earlier one is edited: a done lesson is a record of what was taught.
- The per-book resume positions, and the Copy, Share and Print output (they list the lessons still to come).

## Done when

- [x] Engine tests: the acceptance example (also checked against moving the positions by hand), recording the plan equals a tick, re-editing and the flag, a test day that ends up teaching pages, and strict page input.
- [x] Every existing unit test still passes.
- [ ] CI green: `testDebugUnitTest` and `assembleRelease`.
