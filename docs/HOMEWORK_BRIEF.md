# Homework in the lesson flow (Part B)

Status: implemented for 4.0.0 (48), schema 13; automated checks pass, device QA pending.
Native Operate surface; extend the
existing ledger, TalkBack custom actions, assignment editor and Bone/Saffron tokens.

Lessons offer Add homework and one Edit/Delete action per homework set on or due
that date. The editor starts with that lesson's set-on date. Ordinary, slim and
hero lesson rows name homework, its pages/content and due date in words.

Homework content can be pages, a worksheet or a test. After saving, an optional
dialog offers a class lesson for worksheet/test homework, or homework before a
dated worksheet/test. Skip/Back keeps the original save. Defaults use available
unblocked lessons, respecting closures and group overrides: first remaining
lesson on/after set-on before due, or the previous lesson before the class work.
The teacher can choose another eligible lesson. Accepting creates the counterpart
and links both records in one undo step; the original save remains a separate
step. Existing linked/matching scheduled work prevents duplicate suggestions.
Deleting either side keeps its counterpart and clears the dangling link. Imported
links survive only as reciprocal homework/class pairs; ambiguous links are cleared.
Menus use content, dates and an ordinal to distinguish identical homework titles.
Notes are included in the shared visible/spoken summaries.

Homework banking begins on due, falling back to set-on, inclusive of today.
Undated homework retains immediate banking. Legacy in-memory homework fixtures
retain their old semantics; files migrate through Part C. No homework means no
scheduling change. Existing custom content is preserved; no implicit re-pace.

Schema 13 adds optional contentType and linkedId to assignments, with old defaults.
Version 4.0.0 (48): scheduling changes intentionally, per CHANGELOG version rules.
No new dependencies or signing/application-id/minSdk changes.

Verify named suggestion and parity regressions, menu availability and homework
row data, roundtrip/lenient codec checks, then the full unit/lint/release/platform
test build. Device captures and real platform codec execution require a device.

Final checks: 303 unit tests, zero failures/errors/skips; lint zero errors and
63 warnings; release and Android test APKs build. The 15 platform codec checks
compile but were not executed. Fresh source review findings were fixed with
observed RED→GREEN regressions. Native screenshots, TalkBack/recreation/Back,
large text, tablet and motion remain unverified without a device.

Deferred minor: changing linked class work between Worksheet and Test does not
automatically update the homework counterpart's content type. Either record can
still be edited separately.
