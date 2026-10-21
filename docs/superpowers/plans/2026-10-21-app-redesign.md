# App redesign (3.1.0 to 3.7.0): implementation plan

**Spec:** `docs/superpowers/specs/2026-10-21-app-redesign-design.md`. **Product record:** `PRODUCT.md`. There is no
engine change and no JSON schema change in any PR (`PacerTest` and the extension tests stay unchanged and green).

Each PR has its own version, CHANGELOG section with compare link, docs updates and owner device check. They ship in
order, and each one leaves the app working. After each merge, restart the branch from `main`.

Every PR runs `./gradlew testDebugUnitTest lint assembleRelease` before pushing, and keeps `NoHardcodedTextTest`
and `ContrastTest` green.

## PR0: design docs (2.10.1, PATCH)

- [ ] Commit the spec, this plan, `PRODUCT.md` and the before/after boards under `docs/images/redesign/`.
- [ ] Add `docs/PACER_DESIGN.md`: the Pacer-only design language, departures D1–D12, and the token table. Mark them as
  candidates for the next suite guide.
- [ ] Update `docs/README.md` (new docs), `CLAUDE.md` (a pointer to `PACER_DESIGN.md` next to the style guide rule),
  and the CHANGELOG.
- [ ] Keep `.impeccable/` out of git.

## PR1: foundation tokens and type (3.1.0)

- [x] **`Tokens.kt`:**
  - the new dark set;
  - `warnSoft`/`onWarnSoft` dark;
  - the six edge tokens, light and dark.
- [x] **`ContrastTest`:**
  - the shared light rows stay checked against the guide;
  - a Pacer table checks the new dark values and the edge tokens against `docs/PACER_ACCENT_CONTRAST.md` (renamed in
    the docs to Course Pacer contrast);
  - `everyPairMeetsItsTarget` also covers edges at 3:1.
- [x] **`Type.kt`:** `Figures` (Outfit, `tnum`) replaces `Mono`, and all about 36 uses move to it. Check that digits
  line up in `AnimatedFigure`.
- [x] **Components** (`Components.kt`, `DashboardComponents.kt`):
  - 48dp segmented switch with an ink selected side;
  - 48dp buttons and text-link targets;
  - pale status pill.
  - Lesson rows lose `alpha(0.6f)`.
- [x] **Check:** screens keep their layouts in this PR; only colours, figures and target sizes change. Before/after
  screenshots in the PR (or a note there when no emulator was available).

## PR2: navigation (3.2.0)

- [x] `AppDrawer`: a `ModalNavigationDrawer` with search over classes and groups, Home, Pacing overview (count of
  groups behind), Classes (an expandable list), School year and Settings. New symbols (home, search, menu) go into
  `AppIcons.kt` as path data, with attribution unchanged.
- [x] **`ScreenBar`:** ☰ replaces the back arrow, and System Back keeps popping (`BackHandler` unchanged).
  `showUndo` is set only on Group, Class, Classes, School year, Tests and Homework.
- [x] **`GroupSwitcherSheet`:**
  - It replaces the title dropdown, with search, recent groups (kept in memory per session) and the class's groups,
    each with its status.
- [x] **`WarningsSheet`:**
  - A warning button with a count, with the TalkBack label "N warnings", lists each warning with its fix.
  - On-page `WarningBanner`s and `StatusChip` are removed from the Plan tab.
- [x] **Overview renamed Pacing overview** everywhere it's visible, through strings only (`Screen.Overview` stays).
- [x] **Tests:** `NavTest` covers drawer destinations and Back. A unit-tested `searchGroups(year, query)` helper.

## PR3: Home (3.3.0)

- [x] **Header:** "Hello, *name*" (or "Hello" when no name is set), with the date and the year. ☰ is on the left.
- [x] **Settings:** `Settings.name` field, the theme as a segmented switch, "Show tips again" and `seenTips`, kept in
  `SettingsStore`, outside undo. `data_extraction_rules.xml` already covers `settings.xml`.
- [x] **Next-lesson bar** with Done.
- [x] **Classes card:** a `LazyRow` of class chips and "All N ›".
- [x] **`PacingBoard`:**
  - one cell per group, grouped by class. On-pace cells are neutral and behind cells are rust with "−N".
  - Tapping a cell opens that group. Legend and "See all ›".
  - TalkBack reads each cell as "FCE Wed–Fri, short by 2 lessons".
- [x] **Removed from Home:** the tiles, `YearChip` and `HomeGrid`. Year switching moves into this PR: the School year
  screen gets the year switcher at its top (with + New year, rename and delete from the old picker), so no release
  lacks it. PR5 restyles the rest of that screen.
- [x] **`wholeLessons()` helper** (round up, at least 1), unit-tested. Used by the board, the switcher, Class,
  Pacing overview, Warnings and, in PR4, Stats. It comes from the existing `verdict()`.

## PR4: group Plan and Stats (3.4.0)

- [ ] **`TodayBox`:** the date, pages, Done and **Pages taught** (opens the existing `PagesTaughtDialog`/
  `LessonPlanSheet`).
- [ ] **Custom-plan line** (two sentences and Create). Once a plan exists, the My plan | Proposed switch takes its
  place. `CoverageCard` fixes move into `WarningsSheet`.
- [ ] **`LessonLedger`:**
  - `weekSections(rows, today)` (unit-tested), with week headers.
  - `LessonRow` shows the date, book initials with pages and the tick, with one edge colour by kind.
  - `SlimRow` for days off and whole-lesson tests.
  - Done lessons sit above today, and the list opens at today.
- [ ] **Row behaviour:**
  - The row is one merged TalkBack node with its custom actions (Edit pages, Done up to here, Not done from here,
    custom-plan actions).
  - Long-press opens the same menu.
  - The drag grip and Move earlier/later are kept.
- [ ] **`bookInitials(name)`**, unit-tested; full names in TalkBack.
- [ ] **⋮ menu:** Copy, Share, Print (opens print with this group chosen), Term weeks (the day strip in a sheet) and
  Tips.
- [ ] **Stats:**
  - the verdict heading via `wholeLessons`, and a one-sentence fix;
  - `FigureStrip`;
  - `PaceTrack` (a bar per core book, a tick at the `followLead` target, supplementary books as placed of total);
  - lessons-by-month columns;
  - the details list.
  - Positions still open the existing sheet.
- [ ] **Show-once hints** use `seenTips`.

## PR5: other screens (3.5.0)

- [ ] **Classes:** a row per class (groups, books, status) and a New class button.
- [ ] **Class:**
  - groups first, with their status;
  - books (lead book and supplementary marked);
  - Tests & worksheets and Homework rows;
  - Reset all positions and Delete this class at the end.
- [ ] **School year:**
  - the term in one line;
  - days off as dated rows with the day-off edge;
  - Add Greek public holidays.
- [ ] **Pacing overview:**
  - class by class, with `sortGroups(order)` (unit-tested: most behind first, by name, by next lesson) inside each
    class only;
  - one pale status pill per row;
  - the year's problems in the warning sheet.
- [ ] **Tests and Homework, restyle only:**
  - dated rows with the test edge;
  - ⋮ with Remove on each row, beside the existing swipe and long-press;
  - the past tests line.
- [ ] **Welcome:** a full first-run page in place of `WelcomeDialog`. Settings → Getting started opens it.
- [ ] **Print sheet:** radio buttons (this group, the class, the year) and margins.

## PR6: large text and the background pattern (3.6.0)

- [ ] **`largeText(fontScale)`** (≥ 1.5), unit-tested. It drives:
  - **Reflow:** full-width buttons under text, and stacked rows.
  - **Hiding with a one-tap path:**
    - Home shows only the groups behind, with "N groups on pace ›" and "Classes ›";
    - Pacing overview shows only the groups behind, with "N classes all on pace ›";
    - Stats moves months and details behind "More stats ›";
    - the custom-plan line moves to ⋮.
  - **Titles** cut short with "…".
- [ ] **`PatternBackground`** (light mode only):
  - a 324dp tile drawn once into an `ImageBitmap` from `AppIcons` paths (school symbols added as path data) and
    repeated with an `ImageShader`;
  - `ink` at 14%, with no animation;
  - absent in dark mode and on the welcome page.
- [ ] **`SolidSheet`:** lists and header text sit on paper with 28dp top corners.
- [ ] **Checks:**
  - a unit test that the tile's seed and layout are stable;
  - a contrast note in `PACER_DESIGN.md`;
  - screenshots at 100% and 200% on the emulator.

## PR7: tablet (3.7.0)

- [ ] **Layout:** at `screenWidthDp` ≥ 840, a `PermanentNavigationDrawer`, a list pane (Pacing overview or Classes)
  and a detail pane (Group). Between 600 and 839, a two-pane layout with the modal drawer.
- [ ] **Panes:** one set of actions per window, in the detail pane. The list pane has no ☰ or Undo. The chosen row is
  highlighted.
- [ ] **Behaviour:** Back closes the detail pane first, following `Nav.kt` rules with new tests. The Activity's own
  rotation handling stays.

## After each PR

- [ ] **Owner device check** (`installDebug`): light and dark, 100% and 200% font size, TalkBack on lesson rows,
  tablet or emulator for PR7.
- [ ] **Docs:** README features, `CLAUDE.md` architecture notes (Home, navigation, Pacing overview), and
  `PACER_DESIGN.md`.
