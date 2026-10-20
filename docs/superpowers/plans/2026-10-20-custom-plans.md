# Custom plans (2.7.0): implementation plan

**Spec:** `docs/superpowers/specs/2026-10-20-custom-plans-roadmap-design.md`, PR2 (see its "As built" note). **Brief:** `docs/CUSTOM_PLANS_BRIEF.md`. JSON version 5.

- [x] Model: `Group.custom: CustomPlan?`, `CustomPlan(active, lessons)` and `PlannedLesson(id, pages, note, touched)` in `model/Model.kt`.
- [x] Engine:
  - `GroupReport(plan, group, source)` with `Source.ACTIVE` (the default), `PROPOSED` and `CUSTOM`. `Row.note` and `Row.lessonId` have defaults. With a custom plan, `rows` come from `customRows`; without one, the proposed path is unchanged.
  - `engine/Custom.kt`: `toCustomPlan`, `coverage` (missing, doubled, stale, overflow), `takeSlots` (ticking takes the lesson on that day), `unmarkLesson` and `unmarkLessonsFrom` (unticking puts it back), and `repaceFrom`, `tidy` and `fitToTerm`, which never rewrite a touched lesson.
  - It also has the edits `editLesson`, `insertLesson`, `removeLesson` (pages join the next lesson), `moveLesson` and `copyCustomPlan`.
  - `markDone` and `recordLesson` call `takeSlots`. `problems()` names a custom plan that leaves pages out or runs past the term end.
- [x] Data: `JsonCodec.VERSION` = 5 (`custom` is written only when set; older files read as none). `Sanitise.kt` drops pages for unknown books and keeps lesson ids unique.
- [x] `PacerViewModel`:
  - `createCustomPlan`, `setCustomActive` and `deleteCustomPlan`;
  - `savePlannedLesson` (an edit, or a new lesson, in one undo step), `removePlannedLesson` and `movePlannedLesson`;
  - `repaceFrom`, `tidyCustomPlan`, `fitCustomPlanToTerm` and `copyCustomPlan`;
  - `toggleLesson` and `unmarkDoneFrom` use the slot-restoring unmark, and `removeBook` clears the book's pages from custom plans.
- [x] UI:
  - `ui/CustomPlanUi.kt`:
    - `CreatePlanCard`;
    - `PlanSourceBar` (My plan | Proposed with a preview banner);
    - `CoverageCard` (a bar per book plus one-tap `FixChip`s, re-pace, copy to another group, delete);
    - `PlannedLessonSheet`;
    - `DraggableLesson` (the lesson lifts, the others glide aside, and each place passed ticks) and `DragGrip`;
    - the staggered reveal when a plan is made.
  - `ui/GroupScreen.kt`:
    - the source switch and the coverage card under the status chip;
    - a lesson menu and TalkBack actions for planning, adding, moving, re-pacing and removing;
    - the accent edge for touched lessons, and the "Proposed: …" ghost line;
    - notes on lessons and the hero.
  - Notes also show on Home (`HomeModel.NextLesson.note`), in Copy/Share (`asTsv` adds a Note column only when there are notes) and in Print (`PrintSheet`).
- [x] Nav and Overview: "<group>'s plan" problems open that group (`Nav.fixFor`, `OverviewScreen`).
- [x] Strings in `strings.xml`. `NoHardcodedTextTest` knows the new lazy and state keys.
- [x] Tests:
  - `CustomPlanTest`, 20 tests. It checks that no custom plan changes nothing, and that a new plan is the proposed one. Further cases: a day off or a test shifts the content; ticking and unticking, in order and out of order; recording other pages; re-pace parity; touched lessons kept; edits; fit to term; tidy; copying; problems; the TSV note column.
  - `NavTest` case. `PacerTest` is unchanged.
- [x] JSON round trip checked with a temporary test against real `org.json` (not committed: adding org.json to the tests needs owner approval).
- [x] 2.7.0 / versionCode 28, CHANGELOG, README, BRIEF, CLAUDE.md (the second approved extension), CUSTOM_PLANS_BRIEF, PLANNED_FEATURES §3.
- [x] `./gradlew testDebugUnitTest lint assembleRelease`.
- [ ] Checked on a device by the owner (`installDebug`): create, drag, coverage fixes, the source switch, TalkBack actions.
