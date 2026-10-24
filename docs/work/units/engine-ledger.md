# Units engine ledger

## A1 — schema and model

- Model: `SchoolClass` / `Plan.unitsEnabled` and `bookLinks`; `Group.unitsOverride` and `unitRevisions`; zero-based `UnitRevision` and `RevisionKind`.
- Schema 14: defaults preserve old files; per-record lenient revision parsing; unknown links are removed without changing legacy `Book.units` text.
- RED: `/workspace/units-a1-red.log`, missing model references in the new sanitising guard.
- GREEN: `/workspace/units-a1-green.log`, full JVM gate (`BUILD SUCCESSFUL`, 2m 43s).
- Platform codec coverage added to the existing dependency-free instrumentation harness; actual device execution reported separately.

## A2/A3 — scheduling, revisions and warnings

- `Units.kt` exposes zero-based unit windows/progress, completion prompts, revision mutations and typed issues. Connected books share windows using the union of their remaining unit pages, including when the lead has already finished that unit.
- Proposed allocation, custom re-pacing, catch-up and backlog folding use the shared windows. Unlinked books retain their prior allocation; touched/manual content is preserved and boundary conflicts are reported.
- Impossible windows retain unscheduled pages in `UnitPlan` and report page counts; invalid starts/counts, conflicting deadlines, held-back units and unscheduled revisions have explicit issues surfaced by `Plan.problems`.
- Regression guards cover units-off parity, linked boundaries, unit validation/counts, empty units, overrides, revisions, membership/skips/homework/taught pages, impossible windows/deadlines, touched custom content and catch-up.
- Initial RED: `/workspace/units-a2-red.log` (missing units APIs); initial GREEN: `/workspace/units-a2-green.log` (`BUILD SUCCESSFUL`, 2m 35s).
- Part B integration RED: `/workspace/units-a2-pair-red.log`, same-ID revision editing lost the reciprocal homework link. Same-ID edits now update the owned assignment in place; removal/replacement releases only its owned assignment and clears the surviving homework link.
- Final GREEN: `/workspace/units-a2-final-green.log`, full `testDebugUnitTest` gate (`BUILD SUCCESSFUL`, 1m 47s), including the same-ID homework-pair regression.

## Rulings

- Owner's review focus makes the class switch authoritative; stored group enable overrides are inert when it is off.
- Revisions are per group; NONE records Skip; Pages reserves a lesson through a worksheet assignment.
- Strict starts apply only to the enabled units feature; legacy parsing stays untouched.
- No future window: retain all remaining pages in `UnitPlan`, schedule no later unit across an unfinished boundary, report page counts and the remedy.
- No push; engine commits are cherry-picked into the single combined PR50 branch.
