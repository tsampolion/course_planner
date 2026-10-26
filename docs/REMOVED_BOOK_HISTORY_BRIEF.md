# Removed books keep their taught history

Status: built in 0.4.4.11. Pacing is unchanged; only the done rows of the Plan and TalkBack's reading of them change, and only for a group that dropped a book. The greyed-in-the-row layout awaits the owner's yes (see Decisions).

## Why

When a group drops a book (`Group.removedBooks`), `forGroup` leaves it out of the report, so done rows showed only the books still in the plan. The pages taught from the dropped book stayed stored in `DoneLesson.pages`, but nothing on screen showed them, and a teacher could think the work had vanished. Adding the book back showed it again.

Schema 15 (book `type`, group `removedBooks` and `bookDue`) also had no test against the real platform `org.json`.

## Decisions

- **Greyed, labelled pages in the done row**, after the books the group still uses: "SB 38–40 · Workbook 12 (not in plan)". Open question for the owner: a greyed segment in the row (built), or a second line under it ("Also taught: Workbook 12")? The change is one place, `LessonRow` in `ui/LessonLedger.kt`.
- **Not a column.** The book is not added to `Lesson.cells`, `LessonActions` or the plan's book list, so `differs`/`differingBooks` indices, Today, Print and every index-based consumer stay aligned and cannot see it. It sits in `Lesson.history`.
- **The JVM stays free of a JSON dependency.** The codec tests are platform instrumentation checks in `ExtraLessonCodecTest`.

## Rules

- `lessonsOf` fills `Lesson.history` only on done lessons: each class book the group no longer uses (removed default book, or optional book not selected) that has pages in that lesson's `DoneLesson.pages`, with its full name and collapsed pages.
- Future rows, pacing, Print and the Today box never see a dropped book. It cannot re-enter future pacing: `history` is display-only.
- TalkBack reads "Workbook: 12, no longer in the group's plan" after the other books' pages. The row keeps its single node and its actions.
- "Add back" makes the book part of `plan.books` again; it then shows as a normal cell, not as history. Undo restores the group as before. A group with no dropped books has an empty `history` on every row.

## Tests

- `GroupOverridesTest.removedBookKeepsDoneHistory`: the done row's cells exclude the dropped book, `history` holds it with its name and pages, future rows have none, added back it is a cell and not history, and a group that dropped nothing has no history anywhere.
- `ExtraLessonCodecTest` (device tests job): schema 14 to 15 migration with guessed types and no overrides; a schema-15 round trip with `type`, `removedBooks`, `bookDue`, touched and done lessons, book pairs and units; an unknown `type`; malformed `removedBooks` and `bookDue` entries; a file newer than `JsonCodec.VERSION` refused.

## Not covered

- The spoken string and the grey span are composition-only; there is no Compose test yet (entry 10).
