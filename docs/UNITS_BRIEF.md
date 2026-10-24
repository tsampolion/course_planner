# Book units

Approved engine extension from Part A of the owner's handoff, delivered with the separately approved update checker in PR50.

Book units are optional. The class master switch defaults off. A group can disable units or follow the class; a stored group choice to enable units is inert while the class switch is off. Unit metadata remains saved when units are disabled, and the old allocator, lead following, custom plans and catch-up retain their behavior.

When enabled, units require a lead book. Starts are strict: ascending distinct integers, within first–last, beginning on the book's first page. Unit indices in stored revisions and engine APIs are zero-based. Books connected to the lead through book links must have matching valid unit counts. Linking is optional. Unlinked books use existing lead/proportional behavior.

Each linked unit has a separate lesson window. All its remaining pages finish before a later unit begins anywhere in that chain. Pools respect group book membership, skips, positions, taught pages, active pinned extras and homework. A unit with no remaining pages needs no new lesson. If there are fewer available lessons than nonempty units, the engine preserves boundaries and retains the unscheduled pages in its unit windows, with an explicit warning and page count. Deadlines that cannot coexist with the shared windows warn instead of silently breaking the unit rule.

Revisions belong to each group. Completing a unit offers an optional Test, Worksheet, Pages of a book, or No revision. No revision records a decision, so the prompt does not recur. The first three create an ordinary whole-lesson group assignment; Pages uses a worksheet assignment carrying the selected pages. Taught history is never rewritten. No suitable future date leaves the revision visibly unscheduled. Removing or changing a revision only removes its owned assignment and clears any paired homework reference.

Manual custom lessons, touched content, supplementary placements and pinned extras remain teacher-owned. Automatic re-pacing and catch-up apply shared windows to generated content, preserve those manual choices and expose boundary violations as warnings. Progress and revision prompts are derived from remaining pages, not from invented historical allocations.

Persistence is schema 14. Missing metadata defaults to units off, no links, no override and no revisions. Malformed revision records are isolated. Unknown linked books are removed on sanitising, and invalid starts stay visible for correction instead of being silently sorted.

Validation includes units-off rows/custom/catch-up parity, linked boundaries, count mismatch, strict starts, zero-page units, group master-switch precedence, per-group revisions, homework and optional membership, impossible windows and deadlines, touched custom plans, catch-up, and platform JSON round-trip/default/malformed-record checks. Device execution remains separate from JVM verification.
