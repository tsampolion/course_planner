# Supplementary books (2.10.0): design

Decisions agreed with the owner on 2026-10-20, and approved for implementation the same day. **Brief:** [SUPPLEMENTARY_BOOKS_BRIEF.md](../../SUPPLEMENTARY_BOOKS_BRIEF.md). **Plan:** [2026-10-20-supplementary-books.md](../plans/2026-10-20-supplementary-books.md). Read `CLAUDE.md` first.

## As built (2.10.0)

- **One engine save.** `GroupReport.saveLesson(slot, LessonSave)` is the single batch operation. A slot past the plan's last lesson adds a lesson. A lesson may always keep its own core pages, even ones another lesson also holds.
- **Simpler toggle behaviour.**
  - Toggling a book to "In the lesson" opens the window at once.
  - Save stays disabled until the core pages are confirmed, and "Choose pages to teach…" reopens the window.
  - A refused save is reported in a snackbar.
- **Simpler entry points.** "Add <book> pages…" opens the same editor for the lesson; it doesn't focus one field. When a custom plan exists but the Proposed view is shown, supplementary planning is offered only from My plan.
- **Free days.** A free teaching day with no lesson takes the new lesson at the end of the plan, which is the first free day.

## Scope and baseline

Build on main 2.9.0 (versionCode 31, JSON 7), including `engine/Warnings.kt`. Ship one PR as 2.10.0 / 32 / JSON 8. The homework overhaul is separate.

Supplementary books belong to a class, with included pages placed or taught independently in each group. They never join automatic pacing. Teachers can place them before a lesson or record them afterwards. Proposed rows remain an untouched fallback.

## Model and parsing

Append defaulted fields to preserve existing constructor calls:

- `Book.supplementary: Boolean = false`, `include: String = ""`, `countInLesson: Boolean = false`.
- `PlannedLesson.inLesson: Set<String> = emptySet()`: supplementary ids counting in this lesson. Pages remain in the existing book-id-to-pages map.
- `PlannedLesson.replacementBase: Map<String, List<Int>>? = null`: the complete core allocation just before the first in-lesson placement. Non-null empty map means that lesson originally had no core pages; null means no saved replacement. Save it once, not on every dialog edit. This additional field is needed to undo teacher-selected replacement after restarting the app, without guessing the old core allocation.
- `DoneLesson` is unchanged. Counting mode has planning meaning only; unticking restores actual pages as a touched planned lesson, with no `inLesson` ids or replacement baseline.

Add `Plan.coreBooks` and `Plan.supplementaryBooks` helpers in the engine. Keep original `plan.books` order as the column contract. Display core books first and supplementary books underneath only where the layout requires grouping, using original indices to fetch cells.

`Book.includedPages(): List<Int>` uses `lessonPages`' strict syntax over the supported book bounds `0..MAX_PAGE`, sorted and deduplicated. Empty or unreadable lists return empty; creation/edit validation distinguishes errors using the strict parse result. Never use forgiving `parseRanges` for this list. Derive `first` and `last` only from a valid non-empty list. An invalid imported include remains visible as a problem, with bounded first/last fallback; it never starts pacing. Supplementary lesson inputs must be a subset of included pages, including holes between min/max. `0` is a sentinel only in the replacement dialog's core fields; elsewhere it remains subject to existing page-range rules.

Changing the include list does not silently change existing placements or done records. Existing excluded pages stay visible, with an inline validation explanation when that lesson is next edited; a new save must use valid included pages. Statistics intersect with the current include list. Teachers can clear an obsolete placement. No new supplementary coverage-warning category is introduced.

## Replacement dialog: teacher controls what is taught

The approved flow replaces the attachment's automatic proportional deduction rule. There is no largest-remainder split, automatic highest-page removal, chosen single replacement book, or requirement that removed core-page counts equal supplementary-page counts.

1. “Plan this lesson…” lists core inputs and a Supplementary section. Each supplementary field has “In the lesson | On top”, preset from the book default for a new placement and the saved lesson choice for an existing one. Show “Not placed yet: …”, excluding the current lesson's own pages from conflicts. An empty field is no placement, regardless of toggle position.
2. Choosing In the lesson for non-empty supplementary pages opens a dialog listing every class book for this group. Prefill the complete draft lesson contents, not a list of pages to remove. The dialog can scroll, preserves the outer sheet draft and shows each book's type. Supplementary modes remain per book; this dialog edits page contents.
3. Core inputs accept individual pages, ranges and blank (no pages). The special input `0`, when the book had pages at dialog entry, requires confirmation. Saving an unchanged non-empty core page set requires the same confirmation. Compare parsed page sets, so `5, 6` equals `5–6`. A book empty at dialog entry needs no keep-or-skip prompt for blank or `0`.
4. For each unresolved core book, in class order, show “Keep [Book] on pages [range], or skip it this lesson?” with Keep these pages, Don't use this lesson, and Cancel. Keep retains that book's pages from dialog entry; Don't use supplies an empty list. Cancel returns to the dialog with its draft intact. Resolve once per unchanged draft; changing that field invalidates its resolution. Never store the sentinel `0` as a page because of this flow.
5. Dialog Apply updates only the outer sheet's draft. The sheet's final Save applies all books, notes and choices as one action. Closing either editor without final Save writes nothing, creates no plan and adds no undo entry. Preserve drafts across rotation and use accessible labels for prompts and toggles.
6. Supplementary pages must be included and not taught or placed in another lesson of this group. Core edits may keep this lesson's pages or take remaining pages from later untouched in-term lessons. Reject pages already taught, skipped, assigned as homework, held earlier or held in any other touched lesson. This avoids changing a protected lesson to satisfy the edit. The existing ordinary custom-plan editor keeps its existing rules outside this replacement flow.
7. On top alone changes supplementary content without changing any core lesson. If another placement in the same lesson is In the lesson, its shared core draft remains in force. Explicit core changes are permitted in the replacement dialog even if the final core load is not reduced: the teacher's confirmed contents are authoritative.

### Multiple placements, changes and restoration

All in-lesson placements in a lesson share one explicit core allocation and one baseline. The first such placement captures the existing core map; subsequent edits retain that snapshot and prefill the current core allocation. They do not automatically deduct more core pages.

When one in-lesson book is removed or changed to On top while another remains, reopen the replacement dialog so the teacher confirms the resulting shared core allocation. Do not guess a per-book share to restore. When the last in-lesson book is removed or changed to On top, prefill restoration with `replacementBase`, remove the baseline on Save, and re-pace later untouched lessons. The teacher can review the restoration and edit its core contents before saving. Reject restoration conflicts using the same core validation rules; explain which pages are unavailable rather than duplicating pages held in protected lessons. A version-8 imported lesson with `inLesson` but no baseline opens the same review with its current core pages and never invents a snapshot of earlier contents.

The explicit Save may rewrite its target even if touched; all other touched lessons remain unchanged. The target stays touched. To restore core contents, write the reviewed baseline into that target and then re-pace from `index + 1`. This gives the pages back without temporarily making a teacher-edited lesson eligible for automatic rewriting. This is the teacher-controlled equivalent of re-pacing from the edited lesson in the original prompt.

## Engine API and atomic application

New `engine/Supplementary.kt` contains parsing, validation, unplaced counts and the pure placement operation. `GroupReport.unplacedPages(b)` is included pages minus the union of done pages and pages in every stored custom lesson, including inactive and overflow lessons, for this group. Use a separate available-for-lesson helper that adds back the edited lesson's own placed pages and still excludes taught pages. Historical done-page editing validates against includes but permits revisiting that same record; it does not use the future-placement uniqueness restriction.

Retain a convenience `placeSupplementary(lessonId, bookId, pages, inLesson, id)` for on-top saves and in-lesson saves that retain an already confirmed core allocation. It must not perform proportional deduction. The canonical batch API also accepts the full final core map, all supplementary page/mode changes and note. New in-lesson saves require that confirmed core map; fail validation if it is absent. Validate the full draft before changing anything.

Base is `group.custom ?: toCustomPlan(id)`, returned active. Proposed rows have no lesson ids, so the ViewModel addresses that entry point by the teaching date/index, creates the base in memory, then resolves the corresponding generated id. Do not generate a second base or use a UI-generated id. Editing an existing inactive custom plan uses `Source.CUSTOM`; placing directly on Proposed when a stored custom plan exists opens its corresponding teaching slot for editing and clearly labels My plan. Never silently overwrite a stored custom plan with a fresh proposed copy. A missing slot is inserted explicitly at that teaching index in the same Save.

For the batch Save:

1. Resolve the target in the in-memory base and validate pages and modes against the latest group state. If the target vanished, leave state unchanged and ask the UI to reopen it.
2. Capture, preserve or clear `replacementBase` according to the transitions above. Store the teacher's final core map and supplementary maps, trimmed note, and `inLesson` intersected with non-empty supplementary placements. Mark the target touched.
3. If core contents changed, or an in-lesson placement was added, removed or switched, construct a fresh report from that updated group and call `repaceFrom(index + 1)` once. All modifications from one editor Save share this one re-pace. On-top-only saves do not re-pace.
4. Return the complete active custom plan to one untagged `editLessons` mutation. A validation failure changes nothing. Persisting, Undo and activation use the existing mechanisms.

Future untouched lessons supply and receive only core pages. If no later target can hold displaced core pages, keep the explicit lesson edit and expose missing core pages through the existing coverage card. No page is silently discarded or forced into a touched lesson; ordinary coverage fixes remain available. Pages pulled forward are removed from the later pools, so valid plans still cover each remaining core page exactly once.

### Worked replacement example

Before: target lesson SB `5–6`, WB `3`; next untouched lesson SB `7–8`, WB `4`; no other remaining pages. Add Companion `12–13` In the lesson. The dialog keeps SB `5–6` through Keep these pages and sets WB to `0`, resolved as Don't use this lesson. Save yields target SB `5–6`, no WB, Companion `12–13`; next lesson retains SB `7–8` and gets WB `3–4`. Two supplementary pages replace one core page by explicit teacher choice. Switching Companion to On top restores target WB `3`, and next WB returns to `4`. Every core page appears once. This replaces the prompt's proportional worked example.

## Core-only hooks and column alignment

- `Pacer.kt`: proposed allocation, totals, banked pages and peak use core books only. Keep `bookStats` aligned to `plan.books`, with zero-page, zero-lesson placeholder stats for supplementary books; all pace consumers skip those entries. This retains existing indexing in callers and lags. Proposed `Row.pages` contains an empty list at each supplementary index. `remainingPages` returns no paced pages for supplementary books, with core semantics unchanged; supplementary callers use `includedPages`/`unplacedPages`. Do not alter `allocatePages` or `lessonsFor`.
- `problems()`: core-only range, finish-by and lead checks. Supplementary empty/unreadable include yields `<Book>: list the pages you'll use.`; `Nav` maps it to Class. No supplementary missing/doubled/stale warnings. The existing no-books problem continues to describe a class with no books; a class with only supplementary books has zero core pace and permits explicit placements.
- `Lead.kt`: a supplementary id never resolves as lead, and supplementary columns never follow a lead. Keep original column indices and skip supplementary followers and unit-mismatch checks.
- `Custom.kt`: proposed copy contains only core pages. Coverage uses core books. `repaceFrom` clears and allocates only core entries on untouched targets, retaining supplementary maps, `inLesson`, baseline and notes. Preserve overflow lessons containing supplementary pages instead of dropping them; existing overflow coverage still reports the lesson count. `tidy` does not alter touched lessons or supplementary maps/choices; it only removes an untouched empty lesson if it has no supplementary content or note. `fitToTerm` retains overflow lessons with supplementary pages rather than losing or automatically relocating those placements; it may still fit core-only overflow. Leave unresolved overflow visible for explicit teacher action.
- `Warnings.kt`: catch-up clears only core maps, retains supplementary maps/modes and retains overflow placements. Progress, lag references, book counts and backlog targets use core books; stats indexing remains original-order indexing. `foldBacklog` refuses a supplementary target. `allocateWeighted` remains unchanged.
- Copying a custom plan uses each target group's included-but-untaught supplementary pages rather than core `remainingPages`; preserve modes for retained placements and filter saved baselines to valid core books. Copying is the existing explicit replacement of the target custom plan, not cross-group progress sharing. Removing/merging planned lessons filters modes to retained pages; a merged lesson clears its restoration baseline because two prior lesson allocations no longer describe one original slot, and uses the review fallback above.

All pacing hooks receive short comments explaining the core-only rule. Every automatic fix keeps other touched lessons byte-for-byte unchanged, including their supplementary data.

## Persistence and ViewModel

JSON 8 writes book `supplementary`, `include` and `countInLesson` only when true/non-empty, lesson `inLesson` only when non-empty, and `replacementBase` when non-null (including an empty object). Reuse the codec's range-string map representation for the baseline. Missing fields read as their defaults. Keep v7 `lagLessons` and all v1–7/web migration paths. Update the version-history KDoc. No committed JVM codec test depending on real `org.json`: use symmetric inspection or a temporary uncommitted check and accurately report which was used.

`Sanitise.kt` derives valid supplementary bounds; drops unknown/core ids from `inLesson` and ids with no supplementary pages; restricts baseline keys to class core books and bounded pages; clears supplementary leads. It must not silently re-pace imported data. `removeBook` removes page entries, `inLesson` ids and baseline entries as applicable. Removing an in-lesson book is structural and does not secretly re-pace or restore a lesson: its core edit remains, the baseline is cleared when no in-lesson placements survive, and the existing coverage tools remain available.

`PacerViewModel` adds supplementary creation, tagged include editing, and a per-book default setter. Validate the include before committing and derive bounds atomically. Book defaults affect new placements only. One batch save applies all editor changes through one untagged `editLessons`, including optional custom creation and re-pace. A thin single-placement wrapper uses the same transaction. `recordLesson` continues storing actual pages, including supplementary pages, without storing counting metadata. Do not create a custom plan merely to edit a done record; if one exists, its existing slot-taking path still applies. Restrict supplementary books from the homework picker and reject new supplementary homework at the ViewModel boundary.

## Screens and output

- `ClassScreens.kt`: Supplementary book switch with shared toggle haptic in AddBookSheet. Name plus strict Pages you'll use replaces first/last for supplementary creation. Book tiles show Supplementary and an included-page count. BookFields shows include and Count in the lesson by default, hiding skip/due/units and position controls. Lead picker contains core books only. Book type is chosen at creation; converting an existing core book is outside scope.
- `CustomPlanUi.kt`: supplementary section, availability hint, mode toggles and the replacement dialog above. Support focusing a specific book from a row menu. Save all changed books together. A focused menu on a proposed row must work without an existing custom plan.
- `GroupScreen.kt`: “Add <book> pages…” per supplementary book on editable teaching rows. Render non-empty supplementary pages below core books, by name with an in lesson/on top tag on planned rows. Done rows show actual pages without an invented counting tag. PagesTaughtDialog adds include-validated fields; editing a done record remains a factual correction with the existing effects on core pages. The replacement-choice popup belongs to future planning, where counting mode is meaningful.
- Coverage: core bars/chips only; a quiet line per supplementary book saying `<Book>: n pages not placed yet`. Show this information on Plan even before a custom plan exists, without requiring a coverage object.
- `GroupStats.kt`: core entries first, supplementary entries after them. For supplementary books, y is distinct included pages, x is the union of included pages in stored future custom lessons, z is the union taught in done lessons. Counts are distinct within each category, per group; malformed imported overlap may contribute to both x and z. No pace, position editor or finish-by label.
- `HomeModel.kt`, Home UI and group hero: supplementary names and non-empty pages appear, without shifting core cells. Preserve note and test-row handling. Filter empty supplementary cells explicitly; `collapse(emptyList())` is an em dash, not an empty string.
- `Format.kt` / Copy / Share and `PrintSheet.kt`: choose output indices once from core books plus supplementary books with non-empty pages in the output rows. Use those indices for headers, data, test-row colspan and month separators. Preserve class book order and existing notes/events and escaping. Print selects columns per group table; a different group's placement does not add a blank column here. Existing output scope remains unchanged (these functions currently print/export upcoming rows).
- All visible Compose copy and accessibility labels are in `strings.xml`; engine/problem and existing print-string exceptions remain as defined in CLAUDE.md. Reuse SegmentedToggle, shared haptics, Space/Motion tokens and animation snapping. No dependencies, layouts or icon libraries are added.

## Verification and delivery

Write engine tests first and observe meaningful failures before implementation. Keep existing parity suites and their expectations unchanged; add new cases in `SupplementaryBookTest.kt`, and a small supplementary regression in `PacerTest.kt` as required by CLAUDE.md. No expected value in an existing test changes.

Required cases: unused-book parity against seed and other fixtures (including supplementary first/between/last); no proposed pages or metric changes; on-top leaves every core row unchanged; the worked explicit replacement above; ranges and individual inputs; `0` and unchanged-page set resolutions including Keep, Skip and Cancel; multiple replacements share one baseline; restoration after removal/switch/reload; no modification to unrelated touched lessons; no later target reports missing core pages; invalid/double placements reject atomically; two groups stay independent; re-pace, catch-up and fold preserve supplementary data in untouched and overflow lessons; tidy/fit preserve supplementary placements and touched lessons; tick/untick preserves actual supplementary pages and updates unplaced counts; lead exclusion; strict include errors; Nav mapping; conditional TSV/Print columns and event alignment; Home pages; sanitised ids and baselines; single batch application and state restoration on cancellation. Put pure dialog-decision logic under JVM tests so its edge cases do not require new UI-test dependencies.

Run `./gradlew testDebugUnitTest lint assembleRelease` successfully before each push. Record actual JUnit totals, lint results and JSON verification method, never estimates. Add version 2.10.0 / 32, dated CHANGELOG with `v2.9.0...v2.10.0` and updated Unreleased link, a concise README feature, CLAUDE.md's fourth extension/schema/parity rule, and docs index. On shipping add the brief's decision-record banner and update any feature entry to shipped.

Open one PR to main titled `feat(class): supplementary books (2.10.0)` with sections What teachers get, How it works, Docs, Checks. Checks includes counts, JSON method and “not checked on a device” until owner verification. Include the phone checklist: add a supplementary book; plan both modes; choose specific core ranges and 0; Keep/Skip/Cancel; remove/switch and restore; tick/untick; Print and Copy. Drive CI to green. Leave device verification unticked until the owner confirms it. Do not change the application id, dependencies, signing or CI workflows.
