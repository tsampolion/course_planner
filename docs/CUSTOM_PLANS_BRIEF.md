# Task: Custom plans, a lead book, pace warnings and a calmer group screen

Status: shipped in 2.6.0 (the Plan | Stats split), 2.7.0 (custom plans), 2.8.0 (the lead book) and 2.9.0 (pace and left-behind warnings). Since 3.6.2, catch-up choices survive re-pacing (see `CATCH_UP_REPACE_BRIEF.md`). Since 3.6.1, future lessons offer "Change planned pages…", and fixes show a timed Undo only when they change the plan. Since 3.9.0, Stats follows the selected plan, Own pace previews a chosen rate, shortfall coverage requires in-term pages, and plan actions live in the group menu. Weeks/Months and collapsed earlier lessons refine the Plan tab. [`ARCHITECTURE.md`](ARCHITECTURE.md) describes the current app.

This is the improved version of two requests from the owner (2026-10-20). The answers to the clarifying questions are folded in.

1. "Take the proposed plan as a template upon which I create my own course plan, so I have the template as a fall-back if things go haywire and still get the whole toolkit. A button like *Create your custom plan*, saved alongside ours in a non-destructive way."
2. "a) Warn when a group falls out of the desired pace, with how many lessons at an increased speed it takes to catch up, and warn when one book has been left behind, proposing to integrate it in the plan. b) A focused book that directs the pacing of the rest. c) Group statistics in their own view, so the main screen is the plan."

## Context

Read `CLAUDE.md` and `docs/BRIEF.md` first; their rules are binding. Today the schedule is computed live from positions, the calendar, tests and done lessons. Only done lessons are stored, and a teacher can't plan a future lesson's pages (out of scope in `docs/LESSON_PAGES_BRIEF.md`).

## Decisions (from the clarifying questions)

1. **What a custom plan is:** an ordered list of lesson contents (pages per book, a note). Dates come from the live calendar, so a snow day or a new test shifts the content one lesson later and keeps it.
2. **Edits:** a lesson's pages, insert and remove lessons, drag to reorder, per-lesson notes.
3. **Which plan drives the app:** the custom plan, once created. A "My plan | Proposed" switch shows either, and "Use proposed plan" makes the proposed one active again without deleting the custom one.
4. **Automation:** the app watches coverage (pages missing, doubled, already taught, lessons past term end) and offers one-tap fixes ("Re-pace from here", "Tidy", "Fit to term"). Nothing changes by itself, and every fix is one undo step. Re-pacing never rewrites a lesson the teacher edited by hand.
5. **Scope:** one custom plan per group, plus "Copy to another group".
6. **Done lessons** are shared by both plans: what was taught is a fact.
7. **Lead book:** the other books follow the lead book proportionally. Optional unit start pages per book turn that into unit-by-unit links. A finish-by date still wins for its book.
8. **Catch-up:** the warning sheet has a +1/+2/+3 pages-a-lesson stepper and shows live how many lessons that takes and by when. "Apply" writes it into the custom plan.
9. **Left behind:** a book lagging the lead book (or the others) by more than 2 lessons of its own pace; the threshold is a class setting. The sheet previews folding its backlog into the next lessons.
10. **Group screen:** Plan | Stats. The plan is the main view; tiles, verdict, details, month bars and book positions move to Stats, and a status chip on Plan keeps warnings visible.
11. **Delivery:** four PRs in order (Plan | Stats 2.6.0, custom plans 2.7.0, lead book 2.8.0, warnings 2.9.0).

## Parity

With no custom plan, no lead book and no catch-up applied, every schedule stays row for row as today, and `PacerTest` keeps passing unchanged.

## Done when

- [x] 2.6.0 Plan | Stats split
- [x] 2.7.0 Custom plans
- [x] 2.8.0 Lead book
- [x] 2.9.0 Pace and left-behind warnings with catch-up
