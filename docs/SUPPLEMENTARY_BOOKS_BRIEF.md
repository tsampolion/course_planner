# Task: Supplementary books in chosen lessons

> Approved by the owner on 2026-10-20; being built for 2.10.0.

The owner's request, clarified on 2026-10-20:

> Plans are built from the core books used every lesson. Sometimes I also use other material, such as the course's companion, a reader or a test booklet. I want to add such a book to a class without it joining the regular pacing, and use it only in lessons I choose. Instead of listing pages to skip, I list the pages I'll use. When I put some of its pages in a lesson, I choose whether they count in that lesson or come on top.
>
> When replacing core material, open a window with all the group's books and let me enter the pages each book will teach: individual pages or ranges. Entering 0 or keeping the same core pages should ask whether to keep those pages or not use that book this lesson.

## Context

Read `CLAUDE.md` and [the docs index](README.md) first. The existing custom plans, lead book and warnings remain the basis of the feature. The [design spec](superpowers/specs/2026-10-20-supplementary-books-design.md) defines the engine and UI; the [implementation plan](superpowers/plans/2026-10-20-supplementary-books.md) tracks delivery.

The baseline is main at 2.9.0, versionCode 31, JSON version 7. This is one feature and one PR to main.

## Decisions (from the clarifying questions)

1. **Name:** “Supplementary book” in the UI; `Book.supplementary` in code.
2. **Pages:** a strict include list, for example `4–7, 12, 30–33`, with inline errors. First and last are derived from its minimum and maximum. Skip, finish-by and units do not apply.
3. **Where:** plan future pages in “Plan this lesson…” and record actual pages in “Edit pages taught…”. A future placement creates a custom plan from the proposed plan if needed, in the same undo step. Recording actual pages uses the existing done-lesson path.
4. **Placement mode and replacement:** choose “In the lesson” or “On top” in the UI, remembered per lesson, with a per-book default initially “On top”. On top keeps core pages unchanged. In the lesson opens a dialog containing all class books for this group, prefilled with this lesson's pages. Inputs describe what will be taught, not what will be removed. The teacher chooses individual pages and ranges from any core books, including omitting a book entirely. For each core book submitted as `0` or the unchanged non-empty range, ask “Keep [Book] on pages [range], or skip it this lesson?” with Keep these pages, Don't use this lesson, and Cancel. No automatic proportional deductions or equal-page-count requirement. Displaced core pages re-pace over later untouched lessons in the same undo step. Removing the last in-lesson placement or changing it to on top restores the saved core allocation, then re-paces later untouched lessons. Multiple in-lesson books share the lesson's explicitly chosen core allocation; the spec defines editing and restoration.
5. **Pacing:** supplementary pages never enter the proposed allocation, remaining core pages, required pace, verdict, banked homework, core coverage warnings, lead-book following, lags, catch-up or heaviest lesson. Hours stay unchanged. Explicit changes to core lesson contents still affect the core schedule.
6. **Display:** supplementary pages appear by book name in lesson rows, Home's next lesson, the group hero, Copy/Share (TSV) and Print. Output columns appear only when that supplementary book is used in the output. Stats → Books lists “x of y pages placed · z taught”, without pace. Not-yet-placed pages are quiet information, not warnings.
7. **Groups:** books belong to the class; placements and taught pages are independent for each group.
8. **Homework:** leave supplementary books out of the homework picker in this release. The owner intends a separate homework overhaul; this feature does not implement it.
9. **Release:** 2.10.0, versionCode 32, JSON version 8. Continue reading versions 1–7 and the web export. No new dependencies.

## Parity

`SupplementaryBookTest.noSupplementaryChangesNothing` compares core rows, figures, verdict, coverage, lags and catch-up with and without an unused supplementary book, including when it is between core books. `PacerTest`, `CustomPlanTest`, `LeadBookTest` and `WarningsTest` keep their existing expectations unchanged. The web allocation rule and core `remainingPages` meaning stay unchanged. Re-pacing never rewrites other touched lessons.

## Done when

- [x] Owner approves this brief, the spec and the implementation plan before product code changes.
- [ ] Strict supplementary-book creation/editing and both lesson entry points work.
- [ ] Replacement dialog edits the pages to teach, handles ranges, individual pages and `0`, and resolves unchanged ranges explicitly.
- [ ] Placement, replacement, restoration and any custom-plan creation are one undo step per Save; cancellation changes nothing.
- [ ] Core pages remain accounted for; automatic fixes keep supplementary placements and counting choices.
- [ ] Ticking, unticking, per-group tracking, Home, Stats, TSV and Print work.
- [ ] Version-8 persistence includes the restoration baseline; older exports still load.
- [ ] Engine work is test-first; all unit tests, lint and release assembly pass locally and CI is green.
- [ ] Version, CHANGELOG, README, CLAUDE.md and docs index are current. On shipping, replace this brief's pending banner with `> Decision record: built in 2.10.0. The code and CLAUDE.md describe the current app.`
- [ ] One PR: `feat(class): supplementary books (2.10.0)`, with checks and phone checklist.
- [ ] Checked on a device by the owner.
