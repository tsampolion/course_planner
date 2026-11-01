# Course Pacer design language

> **Status: built.** Agreed with the owner on 2026-10-21 and shipped in 3.1.0 to 3.9.0 (see `CHANGELOG.md`). The background pattern (D12) was dropped before it was built. D13 adds the selectable palettes (Freetown, Arusha, Oxford) and D14 carries their colour into the screens (0.4.14.0). The UI/UX audit ([`UI_AUDIT_BRIEF.md`](UI_AUDIT_BRIEF.md)) proposes changes to it, each to be approved on its own.

`docs/EDUSUITE_STYLE_GUIDE.md` stays the shared EduSuite guide and a byte-for-byte copy of the one in Course Invoicer.
This file records where Course Pacer departs from it. Every departure is Pacer-only for now. Together they are
candidates for the next suite guide, which Course Invoicer may adopt later, one decision at a time.

## The idea

A plain ledger with one focal point per screen. Today's lesson always comes first. Lists are one line per item on
hairline rows, with few boxes. Colour is part of the experience, because the app is for teachers of children, teenagers
and adults: every palette has two primaries, every class has a colour and every lesson kind tints its row (D14). Words
still carry every meaning, so colour is never the only signal. Figures are whole lessons, rounded up. The look is bright
and friendly, and precise like an instrument.

## Departures from the shared guide

| # | Guide | Course Pacer |
|---|---|---|
| D1 | §2.1/§2.2/§10: the dark set is deep, low-light versions | Each palette's dark set is the same palette at night: a deep ground tinted toward its own hue (never pure black or navy) and soft ink (D13) |
| D2 | §1/§3: figures in monospace | Outfit with tabular figures |
| D3 | §6 Home header: "Hello" with a Settings button | "Hello, name" with the date and year. ☰ opens the menu. Settings is in the menu. |
| D4 | §6 Tile grid (bento) on Home | The Pacing overview board, with a blinking dot and "Today" on groups that teach today, and a card of the next tests and worksheets |
| D5 | §15: a back arrow on every screen; no drawer | ☰ menu on every screen, no back arrow; System Back still works. The menu keeps five items in a fixed order, and Classes & Groups opens at the teacher's place (class, group, or the group's pages) with the current page marked and a back row up. On tablets 840dp and wider the menu is a drawer the teacher can pin open (the default) or let collapse behind ☰. |
| D6 | §6 List row: a leading icon | Ledger rows with no icon. Edge colours mark the kind. |
| D7 | §6 Warning banner on the screen | A warning button with a count, and the Warnings sheet |
| D8 | §2: no kind colours | Seven non-violet lesson-kind colours: a 4dp edge and a tint across the row, with words retained (D13, D14) |
| D9 | §6 GuideCard or dialog for the welcome | A full first-run page |
| D10 | §6 segmented control | A filled segmented switch with an accent selected side, 48dp |
| D11 | §2.2 tiles carry status (mint for done) | Tiles are class colours, not status. Behind keeps its word and its pill, in a warning colour at least 40° of hue from both primaries; a palette's primary is never moved to make room for a warning. |
| D12 | §1 Flat, colour blocks only | ~~A light-mode background pattern of school icons, under solid sheets~~ Dropped by the owner (3.6.0): the app stays flat, with no pattern |
| D13 | §2 shared light neutrals, tiles and accent | Pacer-owned palettes, each light/dark: Freetown (default, stored as `BONE`), Arusha (`SAFFRON`) and Oxford (`OXFORD`). No dynamic colour. The suite guide still shows the old palette; other suite apps may adopt these later. |
| D14 | §1 colour blocks for status only | Colour across the app: two primaries per palette, class colours, kind tints, accent controls and tinted neutrals (below). |

## Tokens

The palette is saved outside undo and changes the whole Compose theme immediately. Each palette is named after a town whose flag carries its colours:

- **Freetown** (Sierra Leone: green, white, blue): bone paper, chalkboard green and ballpoint blue. Behind is oxblood; a test is raspberry.
- **Arusha** (Tanzania: green, gold, black, blue): graphite neutrals, pencil yellow and deep teal. Behind is crimson.
- **Oxford** (the Union flag: blue, red, white): cool white paper, flag navy and flag red as equal primaries. Red is only the second primary; behind and Remove are amber and the test edge is teal.

## Colour across the app (D14)

- **Two primaries.** The accent is the first: the lesson hero, Done ticks, the selected side of every switch, the selected day and filter, primary buttons and the logo. `second` is the second: today (today's lesson row, the today dot on the Home board and its key, the ring and dot on the week strip), the actions that add (Add assignment, Homework on the hero, the extra-lesson button) and, in Freetown and Arusha, progress bars (`mark`). Oxford keeps progress navy, so red never reads as a score.
- **One meaning per hue.** No lesson edge sits within 25° of hue of `second`, and warnings sit at least 40° from both primaries. A palette's primary is never removed because a warning shares its hue: the warning moves.
- **Class colours.** `tile1` to `tile5` are class colours. A class takes one by its position in the class list (`classTile`), so nothing new is stored. On-pace cells on the Home board wear their class's colour, with a swatch beside the class name; a class's pages (the class, its groups, group settings) wear it in the top bar.
- **Kind tints.** A lesson kind's colour is a 4dp edge and a 12% tint across the row (`kindTint`, `KIND_TINT`), on Plan, Work, Due soon and every ledger row with an edge. Today's lesson row is `secondSoft`.
- **Soft fields.** Home's greeting sits on `accentSoft`; the group page pills are `accentSoft` with an outline; the selected palette card is `accentSoft`. Section labels (Eyebrow) are `accentText`.
- **Tinted neutrals.** Paper, containers and hairlines lean toward each palette's hue; Arusha keeps true graphite neutrals.

Arusha's saffron is a fill, never text on pale paper; foreground controls use accentText, derived from onAccentSoft. The launcher icon and first-frame window background are static and always Freetown (the background follows light/dark); Arusha and Oxford users see that neutral background only until Compose draws. The in-app logo follows the chosen palette, using accent and onAccent.

| Token | Freetown light | Freetown dark | Arusha light | Arusha dark | Oxford light | Oxford dark |
|---|---|---|---|---|---|---|
| paper | `#E9EBE4` | `#161A17` | `#F1F1EE` | `#161615` | `#F4F5F8` | `#141619` |
| surface | `#F7F8F3` | `#1F2420` | `#FFFFFF` | `#1F1F1D` | `#FFFFFF` | `#1C1F24` |
| surfaceContainer | `#DCE3D9` | `#283029` | `#E6E6E1` | `#2A2A27` | `#E4E8F0` | `#252A33` |
| line | `#C8D0C4` | `#374038` | `#D4D4CF` | `#3B3B37` | `#CFD6E2` | `#363D49` |
| outline | `#6F786E` | `#8A928A` | `#787874` | `#8D8D88` | `#6E7787` | `#8A93A3` |
| muted | `#535C54` | `#A6AEA5` | `#565652` | `#A9A9A3` | `#4F596A` | `#A7AFBD` |
| ink | `#17211C` | `#E6E9E1` | `#161615` | `#EDEDE8` | `#121A2C` | `#E9ECF2` |
| tile1 | `#C9E4CF` | `#21402E` | `#FCE9AE` | `#3D3416` | `#DCE3F4` | `#1E2C4A` |
| tile2 | `#D2DDF7` | `#22305A` | `#CFEAE6` | `#173A35` | `#F9DCE0` | `#4A1C24` |
| tile3 | `#F1DCA8` | `#46391A` | `#F8DCC4` | `#52281A` | `#C6E6F2` | `#133D45` |
| tile4 | `#F3D0DF` | `#4A2440` | `#D7E3F7` | `#1E2B45` | `#E6E6E0` | `#2E2E2A` |
| tile5 | `#BDE3EE` | `#163E4A` | `#DDE8C8` | `#2B3420` | `#D2EBD9` | `#1C3828` |
| onTile | `#17211C` | `#E6E9E1` | `#161615` | `#EDEDE8` | `#121A2C` | `#E9ECF2` |
| onTileMuted | `#46514A` | `#C3CBC1` | `#4A4A46` | `#C9C9C3` | `#465065` | `#C3C9D4` |
| warn | `#9E2A2B` | `#EF8E86` | `#B0213A` | `#F2828F` | `#8A5600` | `#E6B04E` |
| onWarn | `#F7F8F3` | `#2A0B0A` | `#FFFFFF` | `#2C0A10` | `#FFFFFF` | `#2B1D00` |
| warnSoft | `#F2DCD8` | `#402322` | `#F8DEE2` | `#42222A` | `#F6E7C8` | `#3A2E14` |
| onWarnSoft | `#5A1515` | `#F6CFCB` | `#5E0F1C` | `#F8CFD5` | `#4A2E00` | `#F5DFA8` |
| ok | `#1E5C43` | `#86C3A3` | `#2B6A3D` | `#8CC69C` | `#1E6B4A` | `#7FC4A2` |
| onOk | `#F7F8F3` | `#0F1E17` | `#FFFFFF` | `#0F2015` | `#FFFFFF` | `#0C2219` |
| okSoft | `#CFE3D6` | `#21392D` | `#DCEBDF` | `#22382A` | `#D8EBE1` | `#1E352B` |
| onOkSoft | `#123B2A` | `#CDE6D7` | `#163C21` | `#CFE8D6` | `#0F3D29` | `#CBE8DA` |
| accent | `#1E5C43` | `#86C3A3` | `#F5B800` | `#F7C531` | `#012169` | `#8EAEF2` |
| onAccent | `#F7F8F3` | `#0F1E17` | `#161615` | `#161615` | `#FFFFFF` | `#0A1430` |
| accentSoft | `#CFE3D6` | `#21392D` | `#FBEAB0` | `#3D3416` | `#DCE3F4` | `#1E2C4A` |
| onAccentSoft | `#123B2A` | `#CDE6D7` | `#2A2822` | `#F6E3A6` | `#12296B` | `#D3DFFA` |
| second | `#2550C4` | `#93B0FF` | `#006B61` | `#4CC7B6` | `#C8102E` | `#FF6F7F` |
| onSecond | `#F7F8F3` | `#0E1A3D` | `#FFFFFF` | `#0B2622` | `#FFFFFF` | `#330810` |
| secondSoft | `#DCE4F8` | `#1F2C52` | `#CFEAE6` | `#173A35` | `#F9DCE0` | `#4A1C24` |
| onSecondSoft | `#142F78` | `#D6E0FF` | `#00403A` | `#C7EEE8` | `#7A0A1C` | `#FFD3D8` |
| mark | `#2550C4` | `#93B0FF` | `#006B61` | `#4CC7B6` | `#012169` | `#8EAEF2` |
| edgeTest | `#A8336A` | `#E17DAE` | `#A8336A` | `#E17DAE` | `#0E6E78` | `#4FC0C9` |
| edgeDayOff | `#6F786E` | `#8E8A84` | `#787874` | `#8D8D88` | `#6E7787` | `#8A93A3` |
| edgeEdited | `#1E5C43` | `#86C3A3` | `#7A5C00` | `#E0C04A` | `#012169` | `#8EAEF2` |
| edgeHand | `#1C6E8A` | `#4FB8D0` | `#2D5DB8` | `#86A8F0` | `#2F6FC4` | `#4F86E0` |
| edgeFree | `#8A6A00` | `#D8BC4E` | `#3A3A36` | `#C9C9C3` | `#8A6A00` | `#D8BC4E` |
| edgeSupp | `#4C5600` | `#8FB33A` | `#3C7A2A` | `#9CCB7A` | `#4C6A00` | `#8FB33A` |
| edgeWorksheet | `#C25E16` | `#EC8034` | `#C25E16` | `#EC8034` | `#C25E16` | `#EC8034` |

Text is at least 4.5:1, on every ground, class colour, kind tint and today's row; outlines, edges and progress marks are at least 3:1. Pairwise edge RGB distance is at least 60, class colours at least 20 apart. Every set excludes violet/indigo. `ContrastTest` checks all of it; `docs/PACER_ACCENT_CONTRAST.md` lists the measured pairs. ![Palette swatches, not app screenshots](images/palettes.svg)

## Rules

- **Figures:**
  - Lesson counts are whole lessons rounded up, and anything under 1 shows as 1. One verdict feeds every label, so a
    group reads the same everywhere.
  - Pages per lesson keep two decimals, and only on Stats.
- **Status:** one mark per row. On pace is the class's colour on the board and plain in lists. Behind is the warning colour (a filled cell on the board, a pale pill in lists).
- **Lesson kinds:** a 4dp edge in the kind's token and a tint of it across the row, and the words still say the kind.
- **Undo:** in the top bar only where school-year data changes. Timed snackbars show an emptying bar, with no words.
- **Large text** (font scale 1.5 and up, `largeText`):
  - layouts reflow: buttons go full width under their text, and a row's date sits over its pages;
  - screens show less: Home and Pacing overview list only the groups behind, with "N groups on pace ›", "N on pace ›" and "N classes all on pace ›"; Stats puts the months and details behind "More stats ›"; the custom-plan actions and Own pace are available in ⋮;
  - everything hidden is one tap away, and a top bar's title is cut short with "…".
- **Touch targets** are 48dp or more, text links included.
- **Tablets** (the window's width in dp, no window-size library):
  - from 600dp, Pacing overview and the open group share the window as a list pane and a detail pane, with one set of actions in the detail pane's bar and the open group highlighted in the list;
  - from 840dp the menu is a permanent drawer while it is pinned, so there is no ☰; unpinned, it collapses behind ☰;
  - Back closes the detail pane first (`Nav.panes`).

## Popup corners (3.8.1)

Dropdown menus use the shared `PacerDropdownMenu` with fixed 12dp corners. They must not inherit the percentage-rounded pill shape: on tall menus it cuts through the first and last option labels. Buttons and chips retain their pill shapes.


## Stats and Plan refinements (3.9.0)

Stats follows the selected My plan or Proposed source. Custom figures show planned lessons, average core pages per lesson, and completion against the term end. Shortfall coverage counts core pages actually placed inside the term; content coverage alone cannot hide overflow. Own pace previews the chosen rate before saving, preserves touched lessons and supplementary placements, and is exposed by long-press plus a TalkBack action on Pace needed and the large-text menu.

The book pace track retains its lead-book tick and adds a thin outlined proposed-position bar when the group is behind the calendar schedule. One accessible description names both positions; the outline carries the distinction beyond colour.

The Plan hero is two compact rows: label/date with Done, then the pages, which open the taught-pages edit. Preview actions reserve the same space. Large text reflows actions below the lesson. The source switch labels the plan in use; re-pace, copy, delete and catch-up adjustment live in the group menu. Weekly or Monthly view is remembered, the week pills or month navigation sit under the source switch, and earlier/done lessons across months expand from one count. Calendar filtering precedes partitioning so list positioning stays aligned.

## Group assignments and group home (3.11.0)

The group opens on a home rather than a Plan | Stats toggle. Its order is the
lesson hero (filled with the accent, including Homework due today and Set today),
a Progress strip, the Pace track, a dated Due soon ledger, then Plan / Work /
Stats / Settings pills. Progress, Pace track and Due soon sit in containers one
`Space.l` apart. The strip is one line ("Lesson 14 of 40 · On pace") over a thin
bar and opens Stats. Pills are 2 × 2 on phones, one row from 600dp and one column
at large text. Plan retains the compact hero, source and calendar controls.
Tablets keep Plan in the detail pane, with Work, Stats and Settings one tap away. Group pages use the normal back stack; only group identity persists.

Work uses one horizontally scrolling filter row (All, Homework, Tests,
Worksheets), This week/Later/No date/Earlier sections and the existing ledger
geometry. Every row names its type; existing edgeHand/edgeTest/edgeSupp tokens
supply its edge. Swipe removes with five-second Undo; the visible menu offers
Edit and Remove. The empty state has one Add action; populated lists have an
extended Add button. The sheet has type, title, set-on/due dates, pages by book,
a note and an optional whole-lesson switch. Invalid dates/pages cannot save.

The class's old editors are replaced by links to each group's Work. Deprecated
class work routes show those links for one release. See ASSIGNMENTS_BRIEF.md for
migration and pacing parity. The older boards above are historical; current
palette tokens take precedence. Device screenshots and motion inspection are
still required before treating this change as visually verified.

## Homework in the lesson flow (4.0.0)

Every lesson menu and its TalkBack custom actions offer Add homework; each item
set on or due that lesson has its own named Edit/Delete actions. This includes
taught, whole-lesson test and preview rows. Actions edit group work independently
of the selected plan. The existing assignment sheet starts on the chosen lesson.
Homework has a Pages or notes / Worksheet / Test content picker.

Ordinary, slim and hero rows show a wrapping `HW:` line below their content,
naming the title, worksheet/test content, effective book pages and due date.
TalkBack includes that same information. The group hero avoids repeating today's
homework in its footer when its lesson already shows the line.

After saving, a native Material dialog may offer Schedule it in class or Also
set it as homework. A lesson picker names eligible dates; class scheduling
explicitly says it reserves a whole lesson. Skip, outside tap and Back retain the
original save. Linking uses a separate undo step. No new visual system or motion;
the current assignment editor and native dialog behavior apply.

## Book units and quiet updates (4.1.0)

Book units extend the native Operate surfaces. Class settings and creation expose
the optional master switch. The class Units screen has three steps: lead book,
numbered starts with a count stepper/Space evenly/end-page notes, then an optional
link checklist with compatibility status and a Don’t link escape. Group settings
offers Follow class/On/Off and explains stored overrides while class units are off.

Group home shows fractional unit segments in place of the pace summary when
units are available. Stats keeps linked book progress together and retains pace
tracks with unit-boundary ticks. Lesson rows name unit/lesson position, including
actual taught dates so ticking Done does not reset the count. Unit-end rows offer
revision Add/Edit/Remove. A native revision sheet offers Test/Worksheet/Pages/No
revision, future lesson selection and unscheduled feedback; changes have Undo.
The revision diamond pulses once for 800ms, snapping with reduced motion.

Updates are quiet: the shared menu button's dot and accessible description, the
drawer Settings tag, and the first Settings section. The section has notes,
Download/See all changes, automatic-check switch, Check now feedback and one
installed-version line. Debug builds retain About's version and hide Updates.
Use the existing Material components, Outfit, palette roles and large-text
layout. Native runtime/screenshot checks remain pending without a device.

## One sheet frame (0.4.10.0)

- **One frame for every sheet** (audit X7, streamlining S4): a handle, the title, ✕, then the fields. A filled Save, Add or Apply sits at the bottom only when the sheet has something to save; sheets that save as you type, pickers and notices have ✕ alone.
- **Every way out is the same:** ✕, Back, a tap outside and a swipe down. An untouched sheet closes at once; a changed draft asks Keep editing / Discard. Drafts are not kept after the app is closed.
- **Motion** uses the sheet and scrim timings from `MotionSpec` and snaps when animations are off.

## Plan redesign (0.4.6.0)

- **One hue per kind in every mode** (proposal 4): each lesson kind keeps its hue family between light and dark, a worksheet has an ochre edge of its own, and a test is never the warning colour. `ContrastTest` checks the families, the distinctness of the seven edges and the 3:1 contrast.
- **Day-number gutters:** each lesson row starts with a gutter of one width: the day number in figures over the weekday. Rows keep their height; the kind edge is the only colour.
- **Tests and worksheets are full lessons:** as tall and as large as any lesson, with the kind word in bold. A day off stays slim.
- **Hero buttons:** Done is the strong (dark) button in every mode and Homework is the second primary (D14). Both are 40dp so the box keeps its size. The hero's pages go two books to a line.
- **Week and month controls** use round chevron pills, in one row with a label that opens the month picker.

## Group home redesign (0.4.5.0)

Owner-approved proposals from [`UI_AUDIT_BRIEF.md`](UI_AUDIT_BRIEF.md):

- **Today's lesson is the accent fill** wherever it is the focal point (proposal 1). Home has none: its board marks the groups that teach today.
- **Marks on light paper use `mark`** (proposal 3): progress fills and ticks never use the fill accent where it falls under 3:1. `mark` is the second primary in Freetown and Arusha and the accent in Oxford; `ContrastTest` checks it against paper, surface, surfaceContainer and its progress track.
- **Pills have a visible edge** (proposal 6): an `accentSoft` pill (D14) with a 1px `outline` stroke, which reaches 3:1 against the ground.

## Visible actions

No action by long-press alone. Every long-press action also has a visible entry point.
