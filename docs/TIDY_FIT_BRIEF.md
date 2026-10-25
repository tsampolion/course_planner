# Tidy and Fit to term keep the teacher's lessons

Status: built in 0.4.4.7. Engine behaviour change, approved by the owner. No change to the data format.

## Why

CLAUDE.md says the app never rewrites the teacher's work: re-pacing, tidy, fit to term, catch-up and fixes never change a `touched` lesson. Two repairs broke that rule:

- **Tidy** filtered the core pages of every lesson, touched or not, so a page the teacher repeated on purpose was removed. It also removed hand-placed pages of a book the group had dropped, which `coverage()` promises to list and never remove.
- **Fit to term** dropped a touched lesson past the term, with its id and note, and merged the overflow's pages into the last lesson when every lesson in the term was touched.

## Decisions (the owner's answers)

1. **Preservation wins.** When a touched lesson blocks a repair, the warning stays and says why. There is no per-lesson override.
2. **The Stale and Doubled warnings open the lesson** when Tidy can't fix them. There is no "Remove these pages" action. Tidy stays the fix when an untouched lesson holds the pages.
3. **No preview dialog.** The Overflow warning's hint says how many edited lessons stay. Every fix is one Undo.

## Rules

- A touched lesson (pages, note, `inLesson`, `replacementBase`, id) is equal before and after Tidy, Fit to term, and Fit plus re-pace, even for an overflow that is entirely touched.
- **Tidy** reserves every touched lesson's pages first. It then removes repeats and pages that need no teaching from untouched lessons only, so an untouched duplicate of a page a touched lesson holds goes. Pages of a removed book are never removed, from any lesson.
- **Fit to term** re-spreads only untouched overflow lessons, over the untouched lessons after the last touched one. Touched lessons past the term stay past it, unchanged. Fit never merges pages into a touched lesson. With no untouched slot in the term, the untouched overflow lessons stay past it, so every core page is still accounted for once.
- Leftover warnings are accurate: Overflow counts the lessons still past the term and says how many of them the teacher edited. Doubled and Stale offer Tidy only when Tidy would reduce them; otherwise their button opens the first lesson that holds the pages.

## Tests

`CustomPlanTest`: `tidyKeepsTouchedLessons`, `tidyTakesOutPagesThatNeedNoTeaching` (untouched lessons), `tidyKeepsRemovedBookPages`, `fitKeepsTouchedOverflow`, `fitNeverMergesIntoTouched`, `fitWithNoSafeSlotKeepsTheWarning`, `fitThenRepaceKeepsTouchedLessons`. The parity tests are unchanged.
