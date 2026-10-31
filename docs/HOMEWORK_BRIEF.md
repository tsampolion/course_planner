# Homework in the lesson flow (Part B)

Status: shipped.

Native Operate surface; extend the
existing ledger, TalkBack custom actions, assignment editor and Bone/Saffron tokens.

Homework can be marked done or not done in Work and from lesson menus (including TalkBack actions). Completion is optional assignment metadata, persisted with a false default for old files; it never changes pacing. Editing keeps the completion record. Work states beside page homework that its pages are out of pacing.

Lessons offer Add homework and one Edit/Delete action per homework set on or due
that date. The editor starts with that lesson's set-on date. Ordinary, slim and
hero lesson rows name homework, its pages/content and due date in words.

The editor creates homework as pages or notes; only older saved homework can
carry a worksheet or test content type, and it keeps its label and links. After
saving, an optional dialog offers a class lesson for such homework, or homework
before a dated worksheet/test. Skip/Back keeps the original save. Defaults use available
unblocked lessons, respecting closures and group overrides: first remaining
lesson on/after set-on before due, or the previous lesson before the class work.
The teacher can choose another eligible lesson. Accepting creates the counterpart
and links both records in one undo step; the original save remains a separate
step. Existing linked/matching scheduled work prevents duplicate suggestions.
Deleting either side keeps its counterpart and clears the dangling link. Imported
links survive only as reciprocal homework/class pairs; ambiguous links are cleared.
Menus use content, dates and an ordinal to distinguish identical homework titles.
Notes are included in the shared visible/spoken summaries.

Homework pages leave pacing as soon as the homework is saved, whatever its dates
or Done state. No homework means no scheduling change. Existing custom content
is preserved; no implicit re-pace.

Assignments store optional contentType and linkedId (schema 13), with old defaults.

Deferred minor: changing linked class work between Worksheet and Test does not
automatically update the homework counterpart's content type. Either record can
still be edited separately.
