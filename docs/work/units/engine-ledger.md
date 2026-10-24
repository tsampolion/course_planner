# Units engine ledger

## A1 — schema and model

- Model: `SchoolClass` / `Plan.unitsEnabled` and `bookLinks`; `Group.unitsOverride` and `unitRevisions`; zero-based `UnitRevision` and `RevisionKind`.
- Schema 14: defaults preserve old files; per-record lenient revision parsing; unknown links are removed without changing legacy `Book.units` text.
- RED: `/workspace/units-a1-red.log`, missing model references in the new sanitising guard.
- GREEN: `/workspace/units-a1-green.log`, full JVM gate (record result before commit).
- Platform codec coverage added to the existing dependency-free instrumentation harness; actual device execution reported separately.

## Rulings

- Owner's review focus makes the class switch authoritative; stored group enable overrides are inert when it is off.
- Revisions are per group; NONE records Skip; Pages reserves a lesson through a worksheet assignment.
- Strict starts apply only to the enabled units feature; legacy parsing stays untouched.
- No future window: retain all remaining pages in `UnitPlan`, schedule no later unit across an unfinished boundary, report page counts and the remedy.
- No push; engine commits are cherry-picked into the single combined PR50 branch.
