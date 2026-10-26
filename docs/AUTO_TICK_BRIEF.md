# Lessons tick themselves

Status: built in 0.4.7.0. With the feature unused (no lesson before today) scheduling is unchanged; ticking a lesson by hand is as before.

## Why

The schedule counts a lesson as still to teach until someone ticks it. A teacher who forgets to tick sees a plan that is too optimistic, and has to remember which lessons they ticked. A lesson that took place should count as taught without a tap.

## Decisions (owner)

- **A lesson ticks itself at midnight.** Lessons before today are ticked as taught as planned; today's lesson is not, so it can be corrected all day.
- **Done stays.** Ticking early, and the Done button on group home, work as before.
- **A lesson that didn't happen is not a day off.** It is recorded as taught with no pages (`DoneLesson.pages` empty and `edited`). Its pages go back to the pool and spread over the lessons left; the group's days off are untouched.
- **A first-launch question.** Lessons already left unticked are ticked only if the teacher says yes. Until it is answered nothing is ticked.
- **The Yesterday line on group home** shows how yesterday's lesson was counted, with Change: record the pages taught, or "Lesson didn't happen".

## Rules

- `GroupReport.settlePast(today, from)` marks every lesson before `today` done, one at a time with `markDoneUpTo`, so it equals ticking each in order (`AutoTickTest.settlingEqualsTickingEachPastLessonInOrder`).
- `Settings.autoTickFrom` is null until the first-launch question is answered. "Yes" stores 1970-01-01; "Leave them" stores that day. A year with no lesson left unticked in the past never asks.
- A group whose first lesson to teach is before `autoTickFrom` is left alone until the teacher has ticked those lessons, so auto-ticking never ticks around them.
- `PacerViewModel` settles on launch, resume, midnight and every published edit. It is not an undo step, and undo never brings a passed lesson back.
- Not taught: `recordNotTaught` is `recordLesson` with no pages. The Plan row says "Not taught", the lesson menu offers "Lesson didn't happen" for today and earlier lessons, and a dialog first says how the pages a lesson change.
- Unticking a lesson before today has no lasting effect, so the lesson menu no longer offers "Not done from here" for them.

## Not covered

- Importing an older backup with unticked past lessons ticks them when the app next opens, without asking again.
- A phone left unopened for days ticks all those lessons the next time the app opens.
