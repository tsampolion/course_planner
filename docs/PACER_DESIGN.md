# Course Pacer design language

> **Status: built.** Agreed with the owner on 2026-10-21 and shipped in 3.1.0 to 3.9.0 (see `CHANGELOG.md`). The background pattern (D12) was dropped before it was built. D13 adds the selectable Bone and Saffron palettes. The UI/UX audit ([`UI_AUDIT_BRIEF.md`](UI_AUDIT_BRIEF.md)) proposes changes to it, each to be approved on its own.

`docs/EDUSUITE_STYLE_GUIDE.md` stays the shared EduSuite guide and a byte-for-byte copy of the one in Course Invoicer.
This file records where Course Pacer departs from it. Every departure is Pacer-only for now. Together they are
candidates for the next suite guide, which Course Invoicer may adopt later, one decision at a time.

## The idea

A plain ledger with one focal point per screen. Today's lesson always comes first. Lists are one line per item on
hairline rows, with few boxes. Colour is for what needs attention: groups behind and the kind of each lesson. Figures
are whole lessons, rounded up. The look is bright and friendly, and precise like an instrument.

## Departures from the shared guide

| # | Guide | Course Pacer |
|---|---|---|
| D1 | §2.1/§2.2/§10: the dark set is deep, low-light versions | Deep green-tinted grounds and soft ink in both Pacer palettes (D13) |
| D2 | §1/§3: figures in monospace | Outfit with tabular figures |
| D3 | §6 Home header: "Hello" with a Settings button | "Hello, name" with the date and year. ☰ opens the menu. Settings is in the menu. |
| D4 | §6 Tile grid (bento) on Home | Next-lesson bar, Classes card and Pacing overview board |
| D5 | §15: a back arrow on every screen; no drawer | ☰ menu on every screen, a permanent drawer on tablets. System Back still works. |
| D6 | §6 List row: a leading icon | Ledger rows with no icon. Edge colours mark the kind. |
| D7 | §6 Warning banner on the screen | A warning button with a count, and the Warnings sheet |
| D8 | §2: no kind colours | Six non-violet lesson-kind edge tokens, with words retained (D13) |
| D9 | §6 GuideCard or dialog for the welcome | A full first-run page |
| D10 | §6 segmented control | A filled segmented switch with an ink selected side, 48dp |
| D11 | §2.2 tiles carry status (mint for done) | On-pace is neutral; behind keeps its word and pale pill, with rust/red distinct from the accent. |
| D12 | §1 Flat, colour blocks only | ~~A light-mode background pattern of school icons, under solid sheets~~ Dropped by the owner (3.6.0): the app stays flat, with no pattern |
| D13 | §2 shared light neutrals, tiles and accent | Pacer-owned Bone & green (default) and Saffron & ink, each light/dark. No dynamic colour. The suite guide still shows the old palette; other suite apps may adopt these later. |

## Tokens

The palette is saved outside undo and changes the whole Compose theme immediately. Surface steps sit close to their ground. Saffron is a fill, never text on pale paper; foreground controls use accentText, derived from onAccentSoft. Legacy tile names remain for source compatibility, with new non-violet values. Values were tuned for text and edge contrast, edge separation and warning/accent hue separation. The Saffron warnings use a redder rust to reach the required 40° hue separation while keeping the seed accent.

| Token | Bone light | Bone dark | Saffron light | Saffron dark |
|---|---|---|---|---|
| paper | `#E9E6DD` | `#161C1A` | `#F6F4EC` | `#0E1A14` |
| surface | `#F4F2EC` | `#1F2725` | `#FFFFFF` | `#16251D` |
| surfaceContainer | `#E1DED4` | `#2A3330` | `#E9E9DF` | `#24352B` |
| line | `#CDCABF` | `#3B4540` | `#D5D9CE` | `#35493B` |
| outline | `#7D766C` | `#89908D` | `#738078` | `#828A85` |
| muted | `#6B5B51` | `#9AA097` | `#5A6A60` | `#9DB0A4` |
| ink | `#2A1D17` | `#E3E2DA` | `#0D2A1C` | `#EEF0E8` |
| tileLilac | `#D7DFCA` | `#303E31` | `#F4E7AA` | `#454025` |
| tileMint | `#E0DCD1` | `#3A3C34` | `#DCE7D9` | `#2D3F30` |
| tileSky | `#D1DEE0` | `#2C3E42` | `#DCE7E5` | `#293D39` |
| tileApricot | `#E5D9CB` | `#45382D` | `#EBE3CE` | `#413B2A` |
| tilePink | `#DDE3BF` | `#3B4128` | `#E1E2D9` | `#363C32` |
| onTile | `#2A1D17` | `#E3E2DA` | `#0D2A1C` | `#EEF0E8` |
| onTileMuted | `#574D42` | `#C6C9BE` | `#42574A` | `#C8D4C8` |
| warn | `#91421F` | `#FF8F6B` | `#9A2F24` | `#FF7C70` |
| onWarn | `#F4F2EC` | `#3A1206` | `#FFFFFF` | `#3A1206` |
| warnSoft | `#E9DCD0` | `#432B21` | `#F0DED4` | `#43291D` |
| onWarnSoft | `#542811` | `#F8CBB6` | `#5F2416` | `#F8CBB6` |
| ok | `#37622F` | `#9FC98E` | `#315F40` | `#A1CAA3` |
| onOk | `#F4F2EC` | `#0F1A0C` | `#FFFFFF` | `#102719` |
| okSoft | `#D9E1CF` | `#2C3F29` | `#DEE8D9` | `#283F2D` |
| onOkSoft | `#233C1E` | `#CEE7C0` | `#1D4028` | `#D7EACF` |
| accent | `#3F6B34` | `#9FC98E` | `#F0BF1A` | `#F2C94C` |
| onAccent | `#F4F2EC` | `#0F1A0C` | `#0D2A1C` | `#0E1A14` |
| accentSoft | `#D9E3D0` | `#2D4129` | `#F4E7AE` | `#423C1E` |
| onAccentSoft | `#253F1F` | `#D4E8C8` | `#344522` | `#F1E2A8` |
| edgeTest | `#994B39` | `#D77D60` | `#9A2F24` | `#D77D60` |
| edgeDayOff | `#7D766C` | `#89908D` | `#738078` | `#828A85` |
| edgeEdited | `#3F6B34` | `#9FC98E` | `#315F40` | `#F2C94C` |
| edgeHand | `#2D6C84` | `#77B2C2` | `#2D6C84` | `#77B2C2` |
| edgeFree | `#896900` | `#D8C26A` | `#896900` | `#9FC98E` |
| edgeSupp | `#414700` | `#E0DDD0` | `#414700` | `#E0DDD0` |

The launcher icon and first-frame window background are static and always Bone (the background follows light/dark). Saffron users see that neutral Bone background only until Compose draws; it contains no launch text. The in-app logo follows the chosen palette, using accent and onAccent. Today's lesson is the accent fill wherever it is the screen's focal point (group home and Plan; Home's today block follows with its redesign).

Text is at least 4.5:1 and outlines/edges at least 3:1; pairwise edge RGB distance is at least 60. Every set excludes violet/indigo. `docs/PACER_ACCENT_CONTRAST.md` lists all measured pairs. ![Palette swatches, not app screenshots](images/palettes.svg)

## Rules

- **Figures:**
  - Lesson counts are whole lessons rounded up, and anything under 1 shows as 1. One verdict feeds every label, so a
    group reads the same everywhere.
  - Pages per lesson keep two decimals, and only on Stats.
- **Status:** one mark per row. On pace is neutral. Behind is rust (a filled cell on the board, a pale pill in lists).
- **Lesson kinds:** a 4dp edge in the kind's token, and the words still say the kind.
- **Undo:** in the top bar only where school-year data changes. Timed snackbars show an emptying bar, with no words.
- **Large text** (font scale 1.5 and up, `largeText`):
  - layouts reflow: buttons go full width under their text, and a row's date sits over its pages;
  - screens show less: Home and Pacing overview list only the groups behind, with "N groups on pace ›", "N on pace ›" and "N classes all on pace ›"; Stats puts the months and details behind "More stats ›"; the custom-plan actions and Own pace are available in ⋮;
  - everything hidden is one tap away, and a top bar's title is cut short with "…".
- **Touch targets** are 48dp or more, text links included.
- **Tablets** (the window's width in dp, no window-size library):
  - from 600dp, Pacing overview and the open group share the window as a list pane and a detail pane, with one set of actions in the detail pane's bar and the open group highlighted in the list;
  - from 840dp the menu is a permanent drawer, so there is no ☰;
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
Use the existing Material components, Outfit, Bone/Saffron roles and large-text
layout. Native runtime/screenshot checks remain pending without a device.

## Plan redesign (0.4.6.0)

- **One hue per kind in every mode** (proposal 4): each lesson kind keeps its hue family between light and dark, a worksheet has an ochre edge of its own, and a test is never the warning colour. `ContrastTest` checks the families, the distinctness of the seven edges and the 3:1 contrast.
- **Day-number gutters:** each lesson row starts with a gutter of one width: the day number in figures over the weekday. Rows keep their height; the kind edge is the only colour.
- **Tests and worksheets are full lessons:** as tall and as large as any lesson, with the kind word in bold. A day off stays slim.
- **Hero buttons:** Done is the strong (dark) button and Homework the light one in every mode. Both are 40dp so the box keeps its size. The hero's pages go two books to a line.
- **Week and month controls** use round chevron pills, in one row with a label that opens the month picker.

## Group home redesign (0.4.5.0)

Owner-approved proposals from [`UI_AUDIT_BRIEF.md`](UI_AUDIT_BRIEF.md):

- **Today's lesson is the accent fill** wherever it is the focal point (proposal 1).
- **Marks on light paper use `mark`** (proposal 3): progress fills and ticks never use the fill accent where it falls under 3:1. `mark` is the accent in Bone and in both dark sets, and `accentText` in Saffron light; `ContrastTest` checks it against paper, surface and surfaceContainer.
- **Pills have a visible edge** (proposal 6): a transparent pill with a 1px `outline` stroke, which reaches 3:1 against the ground.
