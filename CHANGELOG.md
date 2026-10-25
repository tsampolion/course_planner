# Changelog

All notable changes to this project are documented in this file.

The format is based on [Keep a Changelog](https://keepachangelog.com/en/1.1.0/),
and this project adheres to [Semantic Versioning](https://semver.org/spec/v2.0.0.html).

Version numbers were `MAJOR.MINOR.PATCH` up to 4.1.0. From 0.4.1.1 they have four parts, `0.MAJOR.MINOR.PATCH`, and 4.1.0 is written 0.4.1.0 in this file. The git tags and GitHub releases before 0.4.1.1 keep their old names (`v4.1.0`, `v4.0.0`, …); the compare links below still point at them.

Versioning for this app (format `0.A.B.C`):

- **A** (the second number): a change that breaks old saved files or changes scheduling results on purpose. Rare.
- **B**: a new feature, including redesign work.
- **C**: fixes, documentation, copy changes, build or CI changes and polish that leave the app working as before.
- The leading `0` becomes `1` only when the owner says the app is ready for others.

Every pull request bumps the version:

1. Choose A, B or C using the rules above.
2. Set `versionName` in `app/build.gradle.kts` to `0.A.B.C` (always four parts, starting `0.`) and increase `versionCode` by one.
3. Add a `## [0.A.B.C] - YYYY-MM-DD` section below with that PR's changes, plus its compare link at the bottom.
4. After the merge, the `Release` workflow tags the merge commit `v0.A.B.C` and publishes a GitHub release with that section as notes and the APK attached. It only runs once CI passes.

Keep `[Unreleased]` empty between pull requests.

## [Unreleased]

## [0.4.1.3] - 2026-10-25

### Changed

- The app always opens on Home when you start it. It no longer reopens the group that was open when you closed it. The open school year and class are still remembered.

## [0.4.1.2] - 2026-10-25

### Changed

- `CLAUDE.md` is now the short list of contributor rules. How the app works moved to the new `docs/ARCHITECTURE.md`, which describes only the current app and holds the schema version table.
- The README feature list is one line per feature, linking to its brief or architecture section. The README, `PRODUCT.md` and `docs/PACER_DESIGN.md` name the real palettes (Bone & green, Saffron & ink) instead of teal, and the product record no longer lists stale items.
- Every brief starts with a `Status:` line. The docs index has Reference, Decision records and Not yet approved sections; shipped features are removed from `docs/PLANNED_FEATURES.md`.

### Added

- Docs rules in `CLAUDE.md`, a pull request checklist, and `DocsTest`, which checks the docs index, relative links, brief status lines, the size of `CLAUDE.md` and that the top changelog section matches `versionName`.

### Removed

- Working logs (`docs/work/`), finished plans and specs, and the redesign screenshots. Git history keeps them.

## [0.4.1.1] - 2026-10-25

### Added

- The update check reads four-part versions (`0.X.Y.Z`) as well as the old three-part ones. A build is never offered an older release, and every old three-part version counts as newer than any `0.x.y.z`.
- CI runs lint on every build.
- `docs/STRING_INVENTORY.md` lists every word the app shows, says or prints, grouped by screen, with a blank column for proposed wording. Planned features gain the front-end redesign audit and the streamlining pass.

### Changed

- Versions are now written `0.MAJOR.MINOR.PATCH`, and the release workflow accepts only that form. Past entries below are relabelled to match (4.1.0 is now 0.4.1.0); the old tags and releases keep their names.
- The bump rules are stricter and are recorded at the top of this file and in `CLAUDE.md`.

### Fixed

- A read error on the saved plans file (for example a momentary storage fault) no longer moves it aside as if it were corrupt. The app leaves the file alone, does not save over it, and says so once; closing and reopening the app tries again. Only a file whose content cannot be parsed is set aside.

**Installing:** installed copies of 4.1.0 and earlier can't read four-part versions, so they will not offer this build. Install 0.4.1.1 by hand once. From the next release on, updates arrive in the app as before.


## [0.4.1.0] - 2026-10-24

### Added

- Optional book units with a lead book, numbered start pages, evenly spaced
  boundaries, optional linked books and per-group overrides. Linked books finish
  each unit before starting the next; disabled units preserve existing pacing.
- Unit progress and lesson positions, boundary warnings, and per-group Test,
  Worksheet or book-page revisions with Add/Edit/Remove and Undo. Taught history
  and manually edited plans remain intact; impossible windows stay visible.
- Schema 14 saves units, links, group overrides and revision choices, with older
  backups retaining their defaults. Update preferences remain outside backups of
  school years and outside lesson Undo.
- Update check: once a day, Course Pacer asks GitHub for the latest release. When a newer one is out, ☰ shows a dot, Settings in the menu shows an "Update" tag, and Settings → Updates shows the version and what changed. **Download** fetches the APK in your browser and Android updates the app in place, keeping your school years. Turn the daily check off, or check now, in Settings. Debug builds never check.

### Changed

- The app now has the `INTERNET` permission, used only for the update check. It sends nothing but the request itself.

## [0.4.0.0] - 2026-10-24

### Added

- Lesson and TalkBack actions to add homework or edit/delete each item set on or
  due that lesson. Homework details wrap beneath ordinary, slim and hero rows.
- Worksheet/test content for homework, with optional prompts to schedule it in
  class or set class work as homework. Skip keeps the original save; accepting
  links both items in one additional undo step.
- Schema 13 stores optional homework content and pair links, keeps old defaults,
  and clears invalid links on import, deletion and incompatible type changes.

### Changed

- Homework pages leave the pacing pool on their due date, falling back to set-on,
  inclusive of today. Future homework remains paced until then. Undated homework
  keeps immediate banking. This intentional scheduling change requires a major
  version; existing manually planned lesson content remains intact.

## [0.3.11.0] - 2026-10-24

### Added

- Group-owned Homework, Tests and Worksheets with type filters, dated sections,
  pages from multiple books, notes, Add/Edit and swipe or menu removal with Undo.
- Group home with the lesson hero, Progress, Due soon and Plan/Work/Stats pages;
  tablets retain Plan in the detail pane.
- Pure assignment validation, group-home ordering and reduced-motion timing tests.

### Changed

- Schema 12 migrates old homework and class-wide or targeted tests into their
  groups. Old files remain readable; new files omit class-level work lists.
- Class links to each group's Work page. Existing class work routes redirect
  there for one release. Scheduling parity covers proposed and custom plans.
- Work transitions, press feedback and segmented selection use motion tokens
  with reduced-motion support.

## [0.3.10.0] - 2026-10-23

### Added

- Group settings opens from the group menu or class group editor. It collects Next lesson, an optional earlier end date, group days off and Greek holidays, additional skipped pages, optional books and Delete group with Undo.
- Optional books have one shared definition in the class library. A group chooses which optional books to use; other groups retain their defaults. All pacing, lead-book allocation, Home, Plan, Stats, print and TSV use the group's effective books and calendar.
- Collections organise Classes with editable names and membership. Moving a class removes its previous collection membership; deleting a collection keeps every class. Classes can be sorted within sections by Your order, Name or Most behind, with the choice remembered outside undo.
- Schema 11 stores group overrides, optional-book membership and class collections, reads older backups with unchanged defaults, and refuses future formats. Import sanitising removes unknown references; adding backups remaps clashing collection ids.

### Changed

- Combines handoff phases E and F in one release. Group settings remains in the tablet detail pane, and large-text controls retain their existing reflow and touch targets.

## [0.3.9.0] - 2026-10-23

### Added

- Stats follow My plan or Proposed, with planned lesson count, pace and completion versus term end. Own pace offers an editable rate and a live preview before saving one undoable plan change.
- Shortfall coverage shows how much of the sustainable-pace gap a custom plan covers within the term. Warnings retain Catch up and offer Re-pace to fit while the shortfall remains.
- Pace tracks add an outlined proposed-position line when actual progress is behind, with a combined accessible description.
- The Plan tab remembers Weeks or Months, offers month navigation and selection, and collapses earlier lessons into one expandable row.

### Changed

- The next-lesson panel uses a compact two-row layout with equal action space while previewing either plan, plus full-width actions at large text sizes.
- My plan actions move to the group menu: re-pace untouched lessons, adjust catch-up, copy and delete. The source switch labels the plan currently in use.
- Combines handoff phases C and D in one release. Existing schema 10 files, pinned extra lessons, palettes and fixed menu corners remain supported.

## [0.3.8.1] - 2026-10-23

### Fixed

- Dropdown popups use fixed 12dp corners rather than a percentage-rounded pill, keeping the first and last option labels visible in book selectors and action menus, including at large text sizes.

## [0.3.8.0] - 2026-10-22

### Added

- Selectable Bone & green and Saffron & ink palettes, each with light and dark mode. Settings saves the choice outside undo, applies it immediately and falls back to Bone for unknown or corrupt stored choices.
- Contrast checks across all four token sets, including readable foreground controls for Saffron, distinct non-violet lesson edges and warning/accent hue separation.

### Changed

- Home’s next-lesson and Group’s today panels use neutral surfaces to limit accent coverage. Links, date-picker controls, field focus and text buttons use readable foreground colours.
- The in-app logo follows the selected palette. Static launcher artwork and the first-frame background follow Bone. The shared suite guide remains unchanged and shows the previous palette; other suite apps may adopt these later.


## [0.3.7.0] - 2026-10-22

### Added

- Extra lessons with pinned core-book pages on any date in the group’s remaining term, including days off. Add one from a day-off row, Term weeks or the group menu; edit or delete it from the lesson menu, with Undo.
- The editor pre-fills the proposed share and previews the remaining shortfall. Extra lessons work in Proposed and My plan, re-pacing untouched custom lessons in the same edit and keeping taught history.
- Extra labels in the ledger, Print and TSV, and warnings when a whole-lesson test or the date range excludes a saved extra and returns its pages to the remaining plan.
- JSON schema 10 stores extras while continuing to read older backups.

## [0.3.6.2] - 2026-10-22

### Fixed

- Re-pacing a catch-up plan preserves its chosen boost instead of silently replacing it with proposed pacing. The choice is saved, survives copying and lesson edits, and recalculates as lessons are taught.
- My plan offers "Adjust catch-up…" directly, even when the warning is hidden. The sheet starts with the saved choice. On older plans, use this once to record the boost that previous versions did not save.
- Applying an unchanged catch-up choice says "Already up to date" rather than offering Undo for an unrelated edit.

### Data

- Saved-data version 9 adds the optional catch-up boost to custom plans. Older files still open; older builds refuse imports of the new format.

### Tests

- Catch-up preservation through re-pacing, taught lessons, manual edits, copying and supplementary-page saves, with parity when no boost is saved. Real JSON round-trips and re-pacing after reopening were checked separately.

## [0.3.6.1] - 2026-10-22

### Fixed

- Undo notifications keep their countdown and dismiss themselves after repeated re-pacing. Each notification has its own lifetime, and its timeout starts when it appears. A fix that changes nothing says "Already up to date".
- Applying a fix in the Warnings sheet closes the sheet so its notification is visible.
- A lesson's long-press menu and TalkBack actions offer "Change planned pages…" for future custom lessons and "Record what I taught…" for today and past lessons, removing the duplicate pages action.

### Tests

- Custom plans preserve proposed dates and pages when created with Greek holidays, and still list their days off. Repeated re-pacing preserves lesson dates and holiday rows after modern and legacy imports.

## [0.3.6.0] - 2026-10-21

The redesign's large text and tablet layout (`docs/superpowers/plans/2026-10-21-app-redesign.md`, PR6 and PR7 together). The background pattern was dropped at the owner's request, so there is none.

### Added

- **Large text** (font scale 1.5 and up): screens show less, and the rest is one tap away.
  - Home lists only the groups behind, with "N groups on pace ›", and its Classes card becomes one "Classes ›" row.
  - Pacing overview lists only the groups behind in each class, with "N on pace ›" on a class and "N classes all on pace ›" for the classes with nothing behind. The sort pill goes under the counts.
  - Stats puts lessons by month and the details behind "More stats ›".
  - The custom-plan line moves into the group's ⋮ menu as "Make a custom plan".
- **Tablets**: from 600dp wide, Pacing overview and the open group share the window as a list pane and a detail pane. The group's actions are in the detail pane's bar, the open group is highlighted in the list, and Back closes the group first. From 840dp the menu stays open beside the screens, so there is no ☰.

### Changed

- With large text, the "Done" and "Pages taught" buttons on today's lesson go full width under it, and each lesson row, day off and ledger row puts its date over its pages instead of beside them.

## [0.3.5.0] - 2026-10-21

The redesign's other screens (`docs/superpowers/plans/2026-10-21-app-redesign.md`, PR5).

### Added

- **Pacing overview** lists every group class by class, one line per group with its next lesson, lessons left and one status mark ("Short by 2 lessons" on a pale pill, or a muted "On pace"). Each class says "All on pace" or how many groups are behind. A sort ("Groups: most behind first", by name, by next lesson) reorders groups only inside each class. The year's problems moved off the page into a sheet behind the warning button in the top bar, each with its Fix.
- **Print** offers three choices as radio buttons: this group, every group in its class, or every group this year, then the margins as a segmented switch.

### Changed

- **Classes** is a list: a row per class with its groups, how many books, and one status mark (groups behind, problems, or "On pace"), with a New class button at the bottom end.
- **Class** puts the lesson length and pace on one line with Edit, then the groups with their next lesson and status (tap to open, the pencil to edit days and next lesson), the books with the lead book and supplementary books marked (tap to edit), Tests & worksheets (with the next upcoming one) and Homework, and Reset all positions and Delete this class at the end. The class's problems moved off the page into a sheet behind a warning button with a count in the top bar.
- **School year** switches years with chips at the top and + New year. The term is one line with its weeks and days off, the days off are dated rows with the day-off edge (swipe or long-press to remove), and Rename and Delete this school year sit at the end.
- **Tests & worksheets** and **Homework** are restyled and titled with the class ("FCE · Tests & worksheets", "FCE · Homework"): dated rows (tests with the test edge), ⋮ with Remove on each row beside the swipe and long-press, a "2 past tests · Show" line, and on Homework a section for every group with its own "+ Add" and rows such as "Workbook 12–13 · Set Fri 9 Oct".
- **Welcome** is a full first-run page in place of the dialog: the logo, the same words, the four steps as a numbered list, "Create your school year" and "Not now". Settings → Getting started opens it too.

### Removed

- The tile grids on Classes, Class and Pacing overview, and the year picker's radio list with its per-year ⋮ (rename and delete now act on the open year).

## [0.3.4.0] - 2026-10-21

The redesign's group screen, Plan and Stats (`docs/superpowers/plans/2026-10-21-app-redesign.md`, PR4).

### Added

- **Today's box** first on Plan: the next lesson's date and pages, with **Done** and **Pages taught** (which opens the same pages-taught sheet as before). A long-press opens the lesson's menu.
- **Lessons in week sections**: "This week", "Next week", then the week's dates. Done lessons sit above today's box and the list opens on it, so what was taught is a scroll up.
- **One-line lesson rows**: the date, the books' initials with their pages ("SB 38–40 · WB 20–21") and a round tick, with one edge colour by kind (a test, a day off, pages taught as edited, planned by hand, a free lesson, supplementary pages) and a line naming the kind. Days off and tests that take the whole lesson are slim rows. TalkBack reads each row as one item with the books' full names, and offers every action in its menu (Edit pages taught, Done up to here, Not done from here, and the custom plan's) as custom actions; the drag handle and Move earlier/later stay.
- **⋮ menu** on a group: Copy, Share, Print (which starts with just this group chosen), Term weeks (the day strip, in a sheet) and Tips.
- **Stats**: the verdict as a heading in whole lessons ("Short by 2 lessons"), the same figure as everywhere else, with a one-sentence fix; one figure strip (Lessons left, Pages left, Pace needed to two decimals); a **pace track** with a bar per book and a tick where it would be in step with the lead book (the existing lead-book rule; no new figure), supplementary books as placed of their pages; lessons by month as columns; and a plain details list. Tapping a book still opens its position.
- Show-once tips on Plan (the lesson menu) and Stats (the pace track), remembered with "Show tips again" in Settings.

### Changed

- The custom-plan card is now one line ("Start from this schedule and make it yours." with Create). Once a plan exists, the My plan | Proposed switch takes its place, with Re-pace, Copy to… and Delete under it; its coverage problems and their fixes are in the Warnings sheet.
- Undo in the top bar is a symbol, so the bar has room for the title, the warnings and ⋮. TalkBack still reads how many steps it takes back.

### Removed

- The All months | By month switch: the schedule is one list in week sections. Its stored setting goes too, and older installs drop it on the next settings save.
- Print on the School year screen: it is in a group's ⋮ menu, which starts with that group and can print whole classes instead.
- The Schedule heading with Copy and Share (now in ⋮), the Term weeks and Tips sections on the page, and the custom plan's coverage bars.

## [0.3.3.0] - 2026-10-21

The redesign's Home (`docs/superpowers/plans/2026-10-21-app-redesign.md`, PR3).

### Added

- **Hello, *name***: Home greets you by the name set in Settings ("Hello" alone while it's empty), with today's date and the open school year under it.
- **Next-lesson bar** with Done: the year's next lesson, labelled "today" with how many of today's lessons are left, its date, or overdue.
- **Classes card**: a scrolling row of class chips, each with its group count, and "All N ›" to Classes.
- **Pacing overview board**: one cell per group, class by class. On-pace cells are neutral and behind cells are rust with "−N" whole lessons. A cell opens its group, TalkBack reads it as one item ("FCE Wed–Fri, short by 2 lessons"), and "See all ›" opens Pacing overview.
- **Year switcher on School year**: every school year at the top of the screen, with + New school year and each year's Rename and Delete.
- **Settings**: "Your name", and "Show tips again", which brings back the show-once tips (kept for the whole app, outside undo).

### Changed

- The theme in Settings is a 48dp segmented switch (System, Light, Dark).
- Print or save PDF moves to the School year screen until the group's ⋮ menu offers it (3.4.0).

### Removed

- Home's tile grid and its year chip. Tests and Homework open from the Class screen, Days off and the year from School year.

## [0.3.2.0] - 2026-10-21

The redesign's navigation (`docs/superpowers/plans/2026-10-21-app-redesign.md`, PR2).

### Added

- **☰ menu** on every screen, Home included: "Find a class or group", Home, Pacing overview with how many groups are behind, Classes (an expandable list of the year's classes), School year and Settings. System Back closes it first, then pops screens as before.
- **Group switcher**: tap a group's title for a sheet with search, the groups opened recently this session and the class's groups, each with its status ("On pace" or "Short by 2 lessons").
- **Warnings sheet**: a warning button with a count in the group's top bar ("2 warnings" to TalkBack) lists each warning with its fix: Catch up, Bring in step, and the custom plan's Re-pace, Fit to term and Tidy.
- Home, Search and Menu symbols (Material Symbols Rounded, Apache 2.0) in `AppIcons.kt`.

### Changed

- ☰ replaces the back arrow at the start of every top bar, and Settings moves from Home's header into the menu.
- Undo sits in the top bar only on Group, Class, Classes, School year, Tests and Homework; Pacing overview no longer shows it.
- Overview is now called **Pacing overview**.
- Lesson counts in status labels are whole lessons, rounded up, through one helper (`wholeLessons`).

### Removed

- The warning banners and the status chip on a group's Plan tab, and the fix chips on the custom-plan card; their fixes are in the Warnings sheet.
- The group title's dropdown, replaced by the group switcher.

## [0.3.1.0] - 2026-10-21

The first part of the approved redesign (`docs/PACER_DESIGN.md`): new colours, figures and touch targets. Screens keep
their layouts; navigation, Home and the group screen change in later releases.

### Changed

- **A brighter dark mode.** Course Pacer has its own dark palette: deeper paper, lighter text, a brighter teal accent and clearer warning and success colours. Light mode is unchanged.
- **Figures in Outfit.** Dates, page numbers and counts use the app's own font with figures of one width, in place of the system monospace font, so columns and rolling numbers still line up.
- **Bigger switches and links.** The segmented switches (Plan/Stats, catch-up choices and others) are 48dp tall with a solid ink selected side, and text buttons such as *Open schedule* and *Add Greek holidays* have 48dp targets.
- **Status pill.** On pace reads in a neutral pill; only a group that is behind takes colour, as a pale rust pill.
- **Done lessons stay readable.** Ticked lessons are no longer faded; their text is muted at full contrast.

### Internal

- Six lesson-kind edge tokens (`edgeTest`, `edgeDayOff`, `edgeEdited`, `edgeHand`, `edgeFree`, `edgeSupp`), light and dark, ready for the lesson list redesign.
- `ContrastTest` checks the shared light values against the style guide and the accent, the dark set and the edges against the Course Pacer contrast table (`docs/PACER_ACCENT_CONTRAST.md`).

## [0.2.10.1] - 2026-10-21

### Documentation

- The approved app redesign: its design (`docs/superpowers/specs/2026-10-21-app-redesign-design.md`), its plan in seven releases (`docs/superpowers/plans/2026-10-21-app-redesign.md`), Course Pacer's own design language (`docs/PACER_DESIGN.md`), before/after pictures (`docs/images/redesign/`) and the product record (`PRODUCT.md`). The app itself is unchanged.

## [0.2.10.0] - 2026-10-20

### Added

- **Supplementary books.** Add a companion, a reader or a test booklet to a class with *Supplementary book* on, and list only the pages you'll use (*4-7, 12, 30-33*). It is never paced: the schedule, pages left, the pace, the verdict, the lead book and the warnings leave it out.
- **Place its pages by hand.** Long-press a lesson for *Add <book> pages…*, or record them in *Edit pages taught…*. A group with no custom plan gets one made from the proposed plan, in the same step. Each book shows what it hasn't placed yet.
- **On top or in the lesson.** *On top* adds the pages and changes nothing else. *In the lesson* opens *Pages to teach this lesson* with every book. You say what each core book teaches, as pages or ranges, blank for none, or 0 to leave it out. A book typed as 0, or left as it was, asks *Keep these pages* or *Don't use this lesson*. The pages the lesson gives up move to the next lessons, all in one undo step. Removing the last in-lesson book, or switching it to on top, brings the lesson's core pages back.
- **Where it shows.** Its pages show by name in the lesson, on the next-lesson tiles, and in Copy, Share and Print, with a column only where it's used. Stats shows *x of y pages placed · z taught*. Each group places and tracks its own pages.

### Changed

- The saved file is version 8: books can be supplementary, and custom lessons remember which books count in them and the core pages to restore. Older files still load.

## [0.2.9.0] - 2026-10-20

### Added

- **Catch up.** When a group is behind its sustainable pace, its Plan shows *Behind the pace by n pages · Catch up*. The sheet lets you pick **+1, +2 or +3 pages a lesson** and shows live how many lessons that takes and the day you're back on pace, with the first lessons it would change. If even that isn't enough for the term, it says so. *Apply catch-up* writes the extra pages into your custom plan, making one from the proposed plan if you have none.
- **Bring a book back in step.** A book more than 2 lessons behind the lead book (or the other books) gets its own warning. The sheet spreads its backlog over the next **2, 4 or 6 lessons**, previews that book's pages, and applies them to your custom plan. The number of lessons before the warning appears is set on the class screen, under Lessons.
- Lessons you planned by hand never change, and the proposed plan stays as it is. Once your custom plan fits every page into the term, the warnings stand down; the status chip still gives the verdict against your sustainable pace.
- Implementation plan in `docs/superpowers/plans/2026-10-20-pace-warnings.md`, linked from the design spec.

### Changed

- The saved file is version 7: a class may store its own left-behind threshold. Older files still load, with a threshold of 2 lessons.

## [0.2.8.0] - 2026-10-20

### Added

- **Lead book.** A class with two or more books can pick a *Lead book* on the class screen. The other books keep step with it: lesson by lesson, each one is as far through its pages as the lead book is through its own. A book with its own finish-by date keeps its own pace. The lead book's tile, and its row in a group's Stats, carry a *Leads* tag.
- **Unit start pages** for each book (*1, 13, 25, 37*). When a book has the same number of units as the lead book, it keeps step unit by unit: it starts its unit 3 when the lead book starts its unit 3. If the counts differ, it keeps step page by page, and the class's problems say so.
- Re-pacing a custom plan follows the lead book too.
- Implementation plan in `docs/superpowers/plans/2026-10-20-lead-book.md`, linked from the design spec.

### Changed

- The saved file is version 6: classes may name a lead book, and books may list unit start pages. Older files still load, with no lead book.

## [0.2.7.0] - 2026-10-20

### Added

- **Custom plans.** *Create your custom plan* on a group's Plan copies the proposed schedule into the group's own plan, saved beside the proposed one, which never changes. Your plan then drives the schedule, ticking, Home's next lesson, Copy, Share and Print.
  - **Plan a lesson** (long-press → *Plan this lesson…*): the pages from each book and a note (revision, video, speaking activity). The note shows on the lesson, on the next-lesson tiles, and in Copy, Share and Print.
  - **Add a lesson before** any lesson, **remove** one (its pages join the next lesson), **move** one earlier or later, or drag it by its handle: it lifts, the other lessons glide out of its way, and each place it passes ticks.
  - **Re-pace from here** spreads what's left over your untouched lessons by the usual rule. Lessons you planned by hand keep an accent edge and are never rewritten.
  - **Coverage card:** a bar per book showing how many of the pages left have a lesson, plus a one-tap fix for each problem: pages left out (*Re-pace*), pages taught twice or already covered (*Tidy*), lessons after the term ends (*Fit to term*).
  - **My plan | Proposed** shows either plan; lessons that differ say what the proposed plan teaches that day. *Use this plan* switches which one you teach from, and your plan stays either way.
  - A day off or a test moves your content to the next lesson instead of re-spreading it.
  - Done lessons are shared by both plans. Ticking takes the lesson on that day, unticking puts it back, and recording other pages leaves the rest of your plan as it is, flagging the pages left out.
  - *Copy to…* gives another group the same lessons on its own days; *Delete my plan* goes back to the proposed one (with Undo).
- Overview lists a custom plan that leaves pages out or runs past the term end, and opens its group.

### Changed

- The saved file is version 5: each group may carry its custom plan. Older files and the web page's export still load, as groups without one.
- Implementation plan for custom plans in `docs/superpowers/plans/2026-10-20-custom-plans.md`, linked from the design spec.

## [0.2.6.1] - 2026-10-20

### Changed

- Documentation only. `docs/SIGNING_BRIEF.md` is now a short "Release signing" page that keeps the rules and drops the owner-only steps. `README.md` is about 40% shorter and points to the new docs index, `docs/README.md`.
- `CLAUDE.md` links the docs index, states the signing rules more tightly, and gains two rules: docs stay current with each pull request, and the public docs carry no plans or notes about other repositories, security weaknesses, or how keys and secrets are handled.

### Removed

- Finished feedback notes, UI brief, plans and checklists from `docs/`.

## [0.2.6.0] - 2026-10-20

### Changed

- A group opens on its **Plan**: the next lesson with Mark done and Edit pages taught, a status chip, and the lesson-by-lesson schedule. A **Plan | Stats** toggle at the top switches views. Its ink pill slides between the two choices and the views slide sideways; with animations removed they snap. The plan keeps its scroll position when you come back.
- **Stats** holds the figures that used to sit above the schedule: lessons left, pages left and pace needed, the verdict, the details (heaviest lesson, contact hours, banked homework, blocked lessons), lessons by month, and the books' positions.
- The status chip under the next lesson reads *On track* or *Short by n lessons* in warning colours, and opens Stats. So a warning is never hidden behind the toggle.
- The term weeks strip and the schedule tips fold away behind their own toggles, so the schedule starts higher up.

### Added

- Design and brief for the next features: custom plans built from the proposed one, a lead book, and pace and left-behind warnings with catch-up (`docs/CUSTOM_PLANS_BRIEF.md`, `docs/superpowers/specs/2026-10-20-custom-plans-roadmap-design.md`, `docs/PLANNED_FEATURES.md` §3–5).

## [0.2.5.0] - 2026-10-18

### Changed

- Home's tiles fill the screen: the grid has as many 160dp columns as the width fits (two on a phone held upright, four in landscape, up to six on a tablet), each tile dropping into the shortest column. The 480dp cap is gone; the next-lesson tile stays at most 560dp wide so its text stays readable. At 200% font or under 360dp the grid is one column, as before.
- Turning the phone no longer restarts the screen, so the tiles glide to their new places (`Motion.settle`) when the column count changes. With animations removed they jump there.

## [0.2.4.1] - 2026-10-18

### Changed

- Home no longer ends with a floating *part of EduSuite apps* line; Settings → About already says the app is part of the EduSuite apps.
- The EduSuite style guide describes a Home tile grid whose column count follows the screen width, with tiles gliding to new places when it changes, and keeps the suite line in Settings → About. The grid itself follows in 2.5.0.
- Design spec and implementation plan for the Home and Settings rework in `docs/superpowers/`.

## [0.2.4.0] - 2026-10-17

### Changed

- All screen copy, action messages and accessibility labels now use Android string resources; counts use plural resources. Engine messages, problem-routing messages, printed HTML and stored defaults remain in Kotlin.
- Material Symbols Rounded outlined vectors replace text-glyph controls and provide leading row and tile icons. Back and directional chevrons mirror with layout direction. The Google artwork is credited under Apache 2.0 without adding an icon library.
- Buttons and tiles spring to 96% while pressed, changing figures roll, list items animate, and expandable sections resize with a rotating chevron. The day strip's selected background slides between dates.
- Toggles and selections give system haptic feedback. Invalid number and pace fields show their error, reject invalid persistence and shake once with reject feedback.
- Screens slide and fade on the shared horizontal axis. Undo snackbars show a remaining-time bar while preserving the accessibility-adjusted timeout and the exact history-step check.
- Motion observes the system animator setting live and snaps to the final state when animations are removed. Saving sheets briefly show a check before closing when animations are enabled.

### Added

- A JVM guard against hardcoded UI copy, including checks that catch interpolated prose and accessibility labels while permitting documented internal keys and data formats.
- Material Symbols source attribution and the Apache 2.0 licence in `third_party/material-symbols`.

## [0.2.3.0] - 2026-10-17

### Changed

- Redesigned the remaining screens around EduSuite tiles, grouped settings rows and bottom sheets. The pacing engine, saved data and ViewModel operations are unchanged.
- Group view starts with the next lesson's book pages and Mark done with Undo. A weekly day strip shows today, lesson dates and the selected day. Lessons, pages and pace have responsive stat tiles; More details holds contact hours, the heaviest lesson, banked homework and month bars.
- Book positions open in sheets. Schedule rows retain lesson ticks, edited/test status, long-press commands and TalkBack actions, with layouts that stack at large text sizes.
- Classes, books and groups use tiles; class and school-year settings open in sheets. Reset and delete actions live under More options and still offer Undo.
- Overview uses mint for on track, apricot for a shortfall and neutral tiles when no verdict is available, with problem banners first.
- Tests and homework use swipe rows and add sheets. Test dates start from the selected group's next lesson, homework starts at its current book position, and past tests are collapsed.
- Settings, the welcome guide and the unreadable-file notice follow the suite look. About includes the font and AndroidX credits.

## [0.2.2.0] - 2026-10-17

The EduSuite look, part two: a new Home that shows the open school year at a glance, and deletes that happen at once with Undo instead of asking first. Plans and schedules are unchanged.

### Changed

- **Home is the open school year's dashboard.** At the top: "Hello", today's date and Settings. Under it, a chip with the year's name: tap it to switch to another year, add one, or rename or delete one.
- **Next lesson on Home.** A teal tile shows the next lesson across every class and group: its date, class and group, and the pages for each book (or the test that takes it), marked "Overdue" when its date has passed. **Mark done** ticks it there and then, with Undo, by the same rule as ticking it in the schedule. Tapping the tile opens that group.
- **Tiles for the rest of the year:** Classes, Overview, Tests, Homework, Days off & dates, and Print or save PDF, each with a live figure ("3 classes · 7 groups", "Next: Mon 19 Oct · Unit 1 test", "2 problems"). A section with nothing in it yet shows its tile faded, and it still opens. At very large text sizes or on narrow screens the tiles stack in one column. The school year screen they replace is gone.
- **Home always shows a year when there is one**: after an import, or when the open year is deleted, it shows the one that starts last. Undoing the delete opens the deleted year again.
- **Deletes happen at once with Undo.** Deleting a school year, a class, a group or a book, and resetting every group's positions, no longer ask first: a message offers Undo for 5 seconds (longer if the phone's accessibility settings ask for more time). Import's Replace all still asks first.
- **Swipe to remove.** Days off, tests and homework rows can be swiped away (from the right), with Undo. A long press and TalkBack's actions offer the same, and the × stays.
- **Short adds open from the bottom of the screen**: a new class, a book, and a day off, whose To date and "Applies to" sit under More options.

## [0.2.1.0] - 2026-10-17

The EduSuite look, part one: Course Pacer's colours, type and shapes now follow the EduSuite style guide, the same base as Course Invoicer's, with teal as Course Pacer's own colour. The screens keep their layout; Home and the other screens are redesigned in the next releases. Plans and schedules are unchanged.

### Changed

- **Colours**: a soft lavender-grey paper, white panels, teal for buttons and highlights, rust for warnings and moss green for "fits", in light and dark. The year screen's buttons are now pastel tiles: sky for Classes, lilac for School year settings, mint for Overview (it turns to a warning tint when there are problems).
- **Wallpaper colours are off** on Android 12 and later too (they came in with 1.8.0). Every phone shows the same colours, and teal tells Course Pacer apart from Course Invoicer.
- **Type**: the Outfit typeface for all text, with page numbers, dates and counts still in a monospace font so they line up. Screen titles are 22sp.
- **Shapes**: panels and list groups have 16dp corners and no outline, tiles 24dp, dialogs 28dp; buttons, chips and tags are pills, and "Add" buttons are full-width pill buttons. Labels above groups and tags are in sentence case instead of capitals.
- All text keeps at least 4.5:1 contrast with its background (WCAG AA), and field outlines 3:1, in both themes. A unit test checks every pair the style guide lists.

### Added

- The Outfit typeface (SIL Open Font License 1.1), bundled as one 110 KB variable font. Its licence is in `third_party/outfit/OFL.txt` and is named in `LICENSE`.

## [0.2.0.0] - 2026-10-17

Course Pacer joins the **EduSuite** apps, with Course Invoicer. This release changes the app's id, so it installs as a new app beside the old one: move your school years over with Export and Import (below). The schedules, the saved file's format and the rest of the app are unchanged; the new EduSuite look follows in the next releases.

### Changed

- **New app id**, `gr.edusuite.pacer` (it was `com.coursebook.pacer`). Android treats it as a different app, so 2.0.0 installs beside the old Course Pacer instead of over it, and starts empty. To move your data:
  1. In the old Course Pacer, open Settings → Export… and save the file.
  2. Install this version.
  3. Open Settings → Import…, pick that file and choose Add. The welcome guide shows first, because the new app has no school years yet.
  4. Once your years are there, uninstall the old Course Pacer.

  The theme and the defaults for new classes aren't in the export, so set them again in Settings. The debug build is now `gr.edusuite.pacer.debug`. The release signing key is the same.
- **New launcher icon**, the EduSuite open book: Course Invoicer's pages with three pacing lines cut out of the right page, on a teal tile in both light and dark mode. Android 13 and later can tint it to the wallpaper with themed icons, and launchers that ask for a round icon get one. The logo on Home uses the same tile.
- The release download is now `course-pacer-X.Y.Z.apk`, and the CI artifact is `course-pacer-apk`. The CI summary also shows the APK's package name and version.

### Added

- **EduSuite style guide** (`docs/EDUSUITE_STYLE_GUIDE.md`), a copy of the canonical guide shared with Course Invoicer, and Course Pacer's accent contrast ratios (`docs/PACER_ACCENT_CONTRAST.md`). The README links Course Invoicer.

## [0.1.8.0] - 2026-10-16

A new look: the app now uses EduInvoiceApp's colours, so the two apps look alike. Plans, schedules and settings are unchanged.

### Changed

- On Android 12 and later, the colours come from the wallpaper, as in EduInvoiceApp, including the screen shown while the app opens.
- On older phones, the palette is EduInvoiceApp's navy blue, slate and green, with Material's red for warnings, in place of the web page's teal, sage and rust, in both the light and dark themes. EduInvoiceApp leaves some colours to Material's defaults, which are purple and pink; Course Pacer fills those from the same blue and green instead, and sets every colour itself, so a library update can't change them.
- On every phone, warnings stay red and "Fits" stays green whatever the wallpaper, and the launcher icon and the logo on Home are EduInvoiceApp's blue. Android 13 and later can still tint the icon to the wallpaper with themed icons.
- All text keeps at least 4.5:1 contrast with its background (WCAG AA), in both themes, with the app's own colours or the wallpaper's. "Fits" uses a slightly darker green than EduInvoiceApp's so it stays readable on panels. Where a wallpaper's tinted background would make tags, the next-step card and days off hard to read, they use a neutral one.

## [0.1.7.3] - 2026-10-16

CI only. The app is the same as 1.7.2.

### Changed

- The release signing secrets now sit in a GitHub Environment, `release`, that only `main` may use. Builds of other branches and pull requests no longer get them, so a branch's own build files can't reach the signing key; they're signed with the debug key and never publish, as pull requests from forks already were.
- The Android workflow's token is read-only (`permissions: contents: read`).

## [0.1.7.2] - 2026-10-16

The smaller fixes from the 1.6.0 audit.

### Fixed

- One damaged record, such as a group or book without an id, no longer makes the whole saved file, or an imported one, unreadable. The record gets a new id, or is left out if it can't be used (homework that names no group or book), and everything else loads.
- The **Undo** on Home's and Settings' snackbar now only undoes its own change: the year you deleted or the import you made. If you make another change first, or open a school year, the snackbar goes away instead of undoing something else or nothing.
- Importing a deliberately malformed file, deeply nested past what the reader handles, now says it can't be read instead of closing the app.
- A plan that fits exactly no longer says "Short by 0.00 lessons… 0 fewer pages", a rounding slip in pages-per-lesson arithmetic (21 pages at 1.4 a lesson came out as 15.000000000000002 lessons).
- Running the Release workflow by hand fetches the APK from that commit's Android run. If there isn't one, it stops instead of publishing a release without its APK, which a later run couldn't fix because the tag would exist. Tick **allow_no_apk** to publish without one on purpose.

## [0.1.7.1] - 2026-10-15

Fixes from a full audit of 1.6.0: crashes and freezes from unusual numbers, number fields that saved the wrong value, one-tap deletions, and past lessons coming back after a calendar change.

### Fixed

- A page number typed by mistake, such as 16000000, or one in an imported file, no longer freezes or crashes the app. Pages go up to 100,000; the page fields don't accept more, and files are capped when read.
- A pace of "Infinity" or "1e400", typed or imported, no longer crashes the app when it saves. The pace fields only accept a real number, and a save that fails is logged instead of closing the app.
- Editing a number could save a different one: changing a pace of 1.5 to 1.25 saved 12, because deleting the "5" reset the field to "1". Number fields now keep what you type, and fields with a lowest or highest value (a book's resume page, the lesson length) wait until the number is in range.
- "Reset all positions", and the × on a group or a book, now ask first. They clear ticked lessons and the pages recorded for them.
- Removing a group also removes the tests aimed only at it.
- If the saved school years can't be read, the app now says so the first time it opens, instead of silently starting empty. **Save a copy…** puts the unreadable file somewhere you can reach, such as Drive. An earlier unreadable file is never overwritten.
- Lessons ticked in order stay in the past. Adding a teaching day or removing a day off used to bring back earlier dates as lessons still to teach, with pages on them. Ticking now moves the group's next lesson past the lessons taught, as the web page did; unticking moves it back. Groups saved by 1.4.0 to 1.7.0 are fixed when the app opens. The class screen's "Next lesson" shows the first lesson still to teach.
- A file with repeated ids, or a lesson ticked twice on one date, no longer crashes the screens that list them. Repeats get new ids when the file is read, and **Replace all** no longer keeps two years with one id.

### Changed

- The build uses `androidResources.localeFilters` in place of the deprecated `resourceConfigurations`. The APK still ships English only.

## [0.1.7.0] - 2026-10-15

A fresh install now starts empty, with a short guide, instead of a made-up sample year.

### Changed

- **No more sample year.** A new install opens on an empty Home screen instead of a ready-made 2026–27 year with a "Sample class", so there's nothing fake to find and delete. School years already on a phone are untouched.
- If the saved file can't be read, the app also starts empty (the unreadable file is still kept as `plans.json.bad`) instead of loading the sample year.

### Added

- **Welcome guide.** The first launch shows what the app does and the four steps to a first schedule: school year, class, books and groups, then the schedule. "Create a school year" starts the first step. Settings → Help → **Getting started** shows it again.
- **"Next step" cards** on empty screens say what to do next: create a school year on Home, add a class on the year and Classes screens, add books (or a group) on the class screen, and add books before a group's schedule can show pages. Each card goes away once its step is done.
- **A note on every group's schedule** saying the schedule is plain arithmetic (each book's pages spread evenly over the lessons left), that it can't know how the class is really doing, and that teachers should go by what they actually teach and record it. It also appears under each group's heading on the printed sheet (Print or save PDF), but not in Copy or Share.

## [0.1.6.0] - 2026-10-15

Back up your school years to a file and bring them back. Uninstalling the app deletes its school years, so export before you uninstall or change phones.

### Added

- Settings has a "Back up and restore" section. **Export…** saves every school year to a file you choose, such as Downloads or Google Drive, named like `course-pacer-2026-10-15.json`. Settings aren't included.
- **Import…** reads that file on this phone or another, and also reads the Coursebook Pacer web page's export. It shows how many school years the file holds and asks:
  - **Add** puts them beside yours. A year whose name is already taken gets "(imported)" after it.
  - **Replace all** keeps only the file's years.
  - Either way, the snackbar offers Undo.
- Import refuses a file it can't read, one with no school years, and one from a newer version of the app, and says why. Nothing changes on the phone.
- `docs/BACKUP_BRIEF.md` records the decisions.

## [0.1.5.3] - 2026-10-15

**The first release signed with the permanent key. Installing it needs one last uninstall, which deletes the school years on that phone. Every release after it installs as an update and keeps them.** Before you uninstall, note each year's dates, classes, books and positions so you can type them in again. 1.5.2 below describes the change; it was never published.

### Fixed

- CI read no certificate from the release APK, so 1.5.2 stopped at the certificate check and wasn't published. The check took the newest `apksigner` on GitHub's runners, which prints certificates in another format. Both workflows now use build-tools 35.0.0, the version the build uses, and accept either format. If `apksigner` prints no certificate, the check now fails on every branch and shows its output, instead of passing a debug-signed build without one.

## [0.1.5.2] - 2026-10-15 (not published)

**Installing this release needs one last uninstall, which deletes the school years on that phone. Every release after it installs as an update and keeps them.** Before you uninstall, note each year's dates, classes, books and positions so you can type them in again.

### Changed

- Releases are signed with one permanent key. Until now each release had a different signing certificate, so Android refused to install it over the one before ("App not installed"), and the only way past that was to uninstall. From this release on, each new release installs as an update.
- CI fails on `main` if the key is missing or the APK's certificate isn't the release certificate, so no release can go out signed with another key. The release workflow checks the certificate again and names it in the release notes, so you can check any APK against it.
- Debug builds from Android Studio install as a separate app, "Course Pacer (debug)", with their own data. They no longer clash with an installed release.

### Added

- `docs/SIGNING_BRIEF.md`: the decisions for the release key, and who does each step.

## [0.1.5.1] - 2026-10-15

### Added

- `docs/PLANNED_FEATURES.md` gains a second entry: signing every release with one permanent key, so each new release installs over the last and keeps your school years. Today every release is signed with a different key, so updating means uninstalling, which deletes the saved years. The entry sets out the process step by step, from creating and backing up the key on the owner's computer to the checks before and after the first signed release.

## [0.1.5.0] - 2026-10-15

Record the pages a lesson actually covered when it didn't go to plan.

### Added

- Long-press a lesson and choose "Edit pages taught…" to type the pages it covered in each book, such as `5-8` or `5-8, 10`, or leave a book empty if you taught none of it. Each field starts with the lesson's planned pages (or the ones already recorded) and shows the plan underneath. Pages outside the book are refused.
- Saving marks the lesson done with those pages. Pages it missed go back into the schedule, normally into the next lesson, and pages taught beyond the plan come off later lessons. Each book's remaining pages re-spread over the lessons left, by the same rule as ticking.
- A lesson recorded with pages other than the plan's shows a small "edited" tag. Putting the planned pages back removes it.
- `docs/PLANNED_FEATURES.md` describes features planned for later. The first is focus blocks: teaching one book on its own for a run of lessons (such as three grammar units), then going back to all the books.

### Changed

- The saved file is version 4: a done lesson can carry an `edited` flag. Version 1 to 3 files and the web page's export still load.
- Ticking lessons that went to plan works as before, and schedules with no edited lessons are unchanged.

## [0.1.4.2] - 2026-10-15

### Fixed

- Deleting a school year was hard to find: its button sat at the bottom of School year settings, below every day off. Each year on Home now has a ⋮ button (or long-press the year) with Rename and Delete, and the button in School year settings sits under the year's name and dates. Deleting still asks first, and Home offers Undo.

## [0.1.4.1] - 2026-10-15

### Fixed

- The app opts in to Android's predictive back. Back inside the app works as before; on Android 15 and later, the back gesture from Home previews the launcher before the app closes. The "OnBackInvokedCallback is not enabled" warning no longer appears in the log on every back.

## [0.1.4.0] - 2026-10-15

Lessons can be ticked as done, schedules can show one month at a time, and printing lets you choose classes and margins.

### Added

- Tick a lesson in the schedule once it's taught. Done lessons stay in the list, dimmed, with the pages they covered. Untick to put a lesson back.
- Long-press a lesson for "Done up to here" and "Not done from here".
- Ticking a lesson out of order leaves earlier lessons that aren't done in place, and the pages still to teach re-spread over the lessons left. Ticking in order gives exactly the schedule the web page's "Mark next lesson done" gives.
- Schedules have an All months / By month toggle. By month shows one tab per month: swipe or tap a tab to move. It opens on the current month, and the choice is remembered.
- Print or save PDF opens a dialog to choose which classes to print (all by default) and the margins: Narrow (8 mm), Normal (15 mm) or Wide (25 mm).

### Changed

- The year screen stacks its four buttons in portrait, full width, and puts them in one row in landscape.
- "Mark next lesson done" now ticks the next lesson, which stays visible as done. The summary shows the group's next lesson and how many are done.
- "Reset all positions" also clears done lessons.
- The saved file is version 3: each group stores its done lessons. Version 1 and 2 files and the web page's export still load.
- Copy, Share and Print cover the lessons still to come.

## [0.1.3.0] - 2026-10-10

The app is now called Course Pacer, and plans are organised as school years that hold classes. Existing plans move over on first launch and schedule exactly as before.

### Added

- School years. Each holds the term dates, the days off and its classes. Home lists them in a picker in the middle of the screen; "New school year" suggests the next year's name and dates and can add the Greek public holidays.
- Classes (previously groups were listed straight under a plan). A class is a set of books taught to one or more groups, with its own lesson length and sustainable pace. The Classes screen lists them with a status line each.
- Year screen with buttons for Classes, School year settings, Overview, and Print or save PDF.
- School year settings: name, start and end dates, days off and Greek holidays, and deleting the year. A day off can apply to every class, one class, or one group.
- Overview: every group in every class with its verdict, and the year's problems, each linked to the screen that fixes it.
- Deleting a school year offers Undo on Home.

### Changed

- Renamed the app from Coursebook Pacer to Course Pacer.
- Home shows the logo at the top, the school-year picker in the middle and a Settings button at the bottom right.
- The class screen holds what were the Term (lesson length and pace), Groups and Books screens, with links to Tests & worksheets and Homework.
- Print or save PDF covers every group in every class of the open year.
- Undo works within the open school year; opening another year starts a fresh history.
- Settings: "New plans" defaults are now "New classes" defaults.
- The saved file is version 2. Version-1 files and the web page's export still load: each plan becomes a school year with the same name, dates and days off, holding one class ("Main class") with its books, groups, tests and homework.

### Removed

- The Plans and Schedules screens, and the Home button grid. Their features moved to the screens above.

## [0.1.2.0] - 2026-10-10

Home is now a minimal launcher: the logo, the app's name, the open plan, and a button for each part of the app. Scheduling results and the saved file format don't change.

### Added

- Schedules screen with the group cards and plan problems that were on Home.
- Plans screen to switch, create, rename and delete plans. Rename and delete act on the open plan, and undo is in its bar.

### Changed

- Home shows the app's logo and name, the open plan's name, and ten buttons (Schedules, Plans, Term, Groups, Days off, Tests & worksheets, Books, Homework, Print or save PDF, Settings), each with a one-line summary. Schedules turns rust-coloured and shows the count when the plan has problems.
- Home has no top bar. The plan switcher and the overflow menu are gone; their actions moved to Plans, and Print or save PDF and Settings are buttons on Home.
- Switching plans keeps you on the screen you were on, closing only an open group. A group restored on launch reopens over Schedules, so back returns there.

## [0.1.1.0] - 2026-10-10

The app now opens on a home screen, each part of a plan has its own screen, and there is a settings screen. Scheduling results and the saved file format don't change.

### Added

- Home screen: one card per group with lessons and pages left, the pace needed, the verdict and the next lesson's pages; plan problems at the top, each linking to the screen that fixes it; links to every part of the plan and to Settings.
- Separate screens for Term, Groups, Days off, Tests & worksheets, Books and Homework, replacing the single Setup list. Each shows the plan problems it can fix.
- Settings screen: light, dark or system theme; lesson length and pace for new plans; app version. Settings are stored in SharedPreferences, outside undo, and included in backups.
- The system back button and a back arrow move up through screens. Each screen keeps its scroll position while it's on the back stack.
- On a group's screen, the title switches between groups. On Groups, each group links to its schedule.
- Unit tests for navigation and settings.
- `docs/UI_BRIEF.md`, the brief for this change.

### Changed

- The group tab row is gone. Groups open from Home, and the open group is still restored on launch through the existing `view` field.
- New plans take their lesson length and pace from Settings. The defaults match the sample plan (75 minutes, 4 pages), so nothing changes until you edit them.
- "Add Greek public holidays" is disabled until the term has both dates.

## [0.1.0.2] - 2026-10-10

### Added

- `Release` GitHub Actions workflow. When the Android workflow passes on `main`, it reads `versionName`, creates the `vX.Y.Z` tag and a GitHub release if they don't exist yet, uses this file's section for that version as the release notes, and attaches the release APK. It can also be run manually with a commit SHA to tag an older merge.

## [0.1.0.1] - 2026-10-10

Changes since the `v1.0.0` tag: documentation, licensing and the version format. The app itself doesn't change.

### Added

- `CHANGELOG.md` (this file).
- `CLAUDE.md` with build commands, architecture, rules and gotchas for AI coding agents.
- `LICENSE`: proprietary, all rights reserved, with Dimitrios Tsampallas as the copyright holder. Section 3 forbids AI systems from changing, copying, distributing or training on the code unless the owner has explicitly authorised them.
- README sections on features, tech stack, prerequisites, CI, project structure, development workflow and license, plus a CI status badge.
- `.gitignore` entries for `.kotlin/`, `.navigation/`, `*.apks`, `/app/release/`, logs, heap dumps, `Thumbs.db` and `.claude/settings.local.json`, grouped under comments.

### Changed

- `versionName` follows SemVer exactly: `1.0` became `1.0.0`, and is now `1.0.1` (`versionCode` 2).
- The README now says that CI runs on every push and on manual dispatch, as well as on pull requests.

## [0.1.0.0] - 2026-10-10

### Added

- Native Android app (Kotlin, Jetpack Compose, Material 3) ported from the Coursebook Pacer web page.
- Pacing engine in plain Kotlin that matches the web page's schedule row for row. It covers lessons per group from its weekdays minus days off, skipped and homework pages, even allocation so every book lands on the final lesson, finish-by dates, tests that take the whole lesson, and the pace verdict.
- Greek public holidays generated from Orthodox Easter.
- Plans you can create, rename, switch and delete, with up to 60 undo steps.
- Setup screen for the term, groups, days off, tests and worksheets, books, and homework, with a list of plan errors.
- Group screen with a summary, verdict, per-book progress, lessons per month, the lesson-by-lesson schedule, and "Mark next lesson done".
- Copy as tab-separated text, share, and print or save as PDF.
- Plans stored as JSON in app storage with atomic, debounced writes, a JSON format compatible with the web page, and backup and device-transfer rules.
- Unit tests for the engine.
- GitHub Actions workflow that runs the tests and builds the release APK on every push and pull request, and uploads the APK as a build artifact.

[Unreleased]: https://github.com/tsampolion/course_planner/compare/v0.4.1.3...HEAD
[0.4.1.3]: https://github.com/tsampolion/course_planner/compare/v0.4.1.2...v0.4.1.3
[0.4.1.2]: https://github.com/tsampolion/course_planner/compare/v0.4.1.1...v0.4.1.2
[0.4.1.1]: https://github.com/tsampolion/course_planner/compare/v4.1.0...v0.4.1.1
[0.4.1.0]: https://github.com/tsampolion/course_planner/compare/v4.0.0...v4.1.0
[0.4.0.0]: https://github.com/tsampolion/course_planner/compare/v3.11.0...v4.0.0
[0.3.11.0]: https://github.com/tsampolion/course_planner/compare/v3.10.0...v3.11.0
[0.3.10.0]: https://github.com/tsampolion/course_planner/compare/v3.9.0...v3.10.0
[0.3.9.0]: https://github.com/tsampolion/course_planner/compare/v3.8.1...v3.9.0
[0.3.8.1]: https://github.com/tsampolion/course_planner/compare/v3.8.0...v3.8.1
[0.3.8.0]: https://github.com/tsampolion/course_planner/compare/v3.7.0...v3.8.0
[0.3.7.0]: https://github.com/tsampolion/course_planner/compare/v3.6.2...v3.7.0
[0.3.6.2]: https://github.com/tsampolion/course_planner/compare/v3.6.1...v3.6.2
[0.3.6.1]: https://github.com/tsampolion/course_planner/compare/v3.6.0...v3.6.1
[0.3.6.0]: https://github.com/tsampolion/course_planner/compare/v3.5.0...v3.6.0
[0.3.5.0]: https://github.com/tsampolion/course_planner/compare/v3.4.0...v3.5.0
[0.3.4.0]: https://github.com/tsampolion/course_planner/compare/v3.3.0...v3.4.0
[0.3.3.0]: https://github.com/tsampolion/course_planner/compare/v3.2.0...v3.3.0
[0.3.2.0]: https://github.com/tsampolion/course_planner/compare/v3.1.0...v3.2.0
[0.3.1.0]: https://github.com/tsampolion/course_planner/compare/v2.10.1...v3.1.0
[0.2.10.1]: https://github.com/tsampolion/course_planner/compare/v2.10.0...v2.10.1
[0.2.10.0]: https://github.com/tsampolion/course_planner/compare/v2.9.0...v2.10.0
[0.2.9.0]: https://github.com/tsampolion/course_planner/compare/v2.8.0...v2.9.0
[0.2.8.0]: https://github.com/tsampolion/course_planner/compare/v2.7.0...v2.8.0
[0.2.7.0]: https://github.com/tsampolion/course_planner/compare/v2.6.1...v2.7.0
[0.2.6.1]: https://github.com/tsampolion/course_planner/compare/v2.6.0...v2.6.1
[0.2.6.0]: https://github.com/tsampolion/course_planner/compare/v2.5.0...v2.6.0
[0.2.5.0]: https://github.com/tsampolion/course_planner/compare/v2.4.1...v2.5.0
[0.2.4.1]: https://github.com/tsampolion/course_planner/compare/v2.4.0...v2.4.1
[0.2.4.0]: https://github.com/tsampolion/course_planner/compare/v2.3.0...v2.4.0
[0.2.3.0]: https://github.com/tsampolion/course_planner/compare/v2.2.0...v2.3.0
[0.2.2.0]: https://github.com/tsampolion/course_planner/compare/v2.1.0...v2.2.0
[0.2.1.0]: https://github.com/tsampolion/course_planner/compare/v2.0.0...v2.1.0
[0.2.0.0]: https://github.com/tsampolion/course_planner/compare/v1.8.0...v2.0.0
[0.1.8.0]: https://github.com/tsampolion/course_planner/compare/v1.7.3...v1.8.0
[0.1.7.3]: https://github.com/tsampolion/course_planner/compare/v1.7.2...v1.7.3
[0.1.7.2]: https://github.com/tsampolion/course_planner/compare/v1.7.1...v1.7.2
[0.1.7.1]: https://github.com/tsampolion/course_planner/compare/v1.7.0...v1.7.1
[0.1.7.0]: https://github.com/tsampolion/course_planner/compare/v1.6.0...v1.7.0
[0.1.6.0]: https://github.com/tsampolion/course_planner/compare/v1.5.3...v1.6.0
[0.1.5.3]: https://github.com/tsampolion/course_planner/compare/v1.5.1...v1.5.3
[0.1.5.2]: https://github.com/tsampolion/course_planner/pull/12
[0.1.5.1]: https://github.com/tsampolion/course_planner/compare/v1.5.0...v1.5.1
[0.1.5.0]: https://github.com/tsampolion/course_planner/compare/v1.4.2...v1.5.0
[0.1.4.2]: https://github.com/tsampolion/course_planner/compare/v1.4.1...v1.4.2
[0.1.4.1]: https://github.com/tsampolion/course_planner/compare/v1.4.0...v1.4.1
[0.1.4.0]: https://github.com/tsampolion/course_planner/compare/v1.3.0...v1.4.0
[0.1.3.0]: https://github.com/tsampolion/course_planner/compare/v1.2.0...v1.3.0
[0.1.2.0]: https://github.com/tsampolion/course_planner/compare/v1.1.0...v1.2.0
[0.1.1.0]: https://github.com/tsampolion/course_planner/compare/v1.0.2...v1.1.0
[0.1.0.2]: https://github.com/tsampolion/course_planner/compare/v1.0.1...v1.0.2
[0.1.0.1]: https://github.com/tsampolion/course_planner/compare/v1.0.0...v1.0.1
[0.1.0.0]: https://github.com/tsampolion/course_planner/releases/tag/v1.0.0
