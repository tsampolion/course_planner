# Group assignments (Part C)

Status: implemented for 3.11.0, schema 12; automated checks pass, device QA pending.
Part B is specified in HOMEWORK_BRIEF.md; Part A (book units) follows separately.

## Behaviour

Each group owns assignments of type Homework, Test or Worksheet, with a title,
optional set-on and due dates, pages by book, a note and a takes-lesson flag.
Work has type filters, dated sections, an undated section, Add/Edit and Delete
through swipe or the visible menu. Saves and deletes each use one undo step;
deletes use the existing five-second Undo notification.

Group home replaces Plan | Stats: lesson hero, Progress, Due soon, then separate
Plan, Work and Stats entry rows. The hero names homework due today and work set
today. Plan keeps its existing source, week/month and lesson controls. On tablets,
Plan stays in the group detail pane, with links to the group's other pages.
Large text uses stacked ledger dates and reflows the existing hero actions.

## Migration and engine contract

Legacy homework becomes HOMEWORK with inclusive pages and its set-on date.
Legacy tests become TEST with due = event date. Class-wide tests copy to every
group; targeted tests and homework reach only their own group. Loading, importing
and saving convert legacy class lists, and new JSON never writes those lists.
Migration is idempotent; unknown book references, duplicate ids and invalid page
numbers are sanitised. The original staged foundation is superseded by this
coordinated engine/editor/persistence cutover.

Group settings, calendar overrides, optional books and class collections from
schema 11 remain intact. Editors show the group's effective books and preserve
saved pages for deselected optional books; selecting the book again makes its
pages editable.

Non-homework work blocks a lesson only when takesLesson is true. In 3.11.0 homework
pages left the pool immediately; Part B (4.0.0) changes that to due/set-on timing.
The engine keeps a TestEvent projection for existing lesson
labels, exports and legacy in-memory fixtures. No new TestEvent is persisted.
The parity test normalises only the obsolete class-wide targeting metadata;
dates, names, blocked rows, pages, figures and custom-plan contents must agree.

## Design and validation

Use current Bone/Saffron tokens and strings.xml; no dependencies, signing or
application-id changes. The supplied mock files are absent; the written design
addendum supplies section order and interactions. Committed redesign boards use
an older palette, so current Tokens.kt and PACER_DESIGN.md D13 take precedence.

Verified: 283 unit tests, zero failures/errors; lint passes with zero errors;
release APK and Android test APK build. A connected
phone/tablet is required to execute real org.json codec tests and inspect phone,
large-text, dark-theme and tablet captures; source compilation is not visual QA.
