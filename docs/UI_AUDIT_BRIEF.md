# UI/UX audit and redesign

Status: approved 2026-10-25; audit done (stage 1, this brief). 0.4.4.3 added the usability audit's UI findings (X12–X15 and the notes under G1, H1 and C2); 0.4.4.7 added the interaction map's (X16–X21, from [NAVIGATION.md](NAVIGATION.md)). Fix PR 1 (Group home and the hero, 0.4.5.0), PR 2 (Plan, 0.4.6.0), PR 3 (Home today, 0.4.8.0), PR 4 (Menu and top bar, 0.4.9.0) and PR 5 (One sheet frame, 0.4.10.0) are built. The others need the owner's go-ahead, one at a time.

This brief records the taste-skill UI/UX audit (`PLANNED_FEATURES.md` §2 until 0.4.4.2), what it found per screen, and the order of the fix pull requests. Its companion is [`STREAMLINING_BRIEF.md`](STREAMLINING_BRIEF.md), which covers each screen's start, purpose, next step and end.

## Decisions settled with the owner

| Question | Answer |
|---|---|
| Which screens first | Group home and Plan |
| May the audit change `PACER_DESIGN.md` | Yes, item by item: each proposal below goes in only when the owner says yes in its fix PR; contradictions get fixed |
| Visual direction | Push further, inside the app's palettes (now Freetown, Arusha and Oxford, D13) and font (Outfit). Token values may be tuned while `ContrastTest` stays green |
| Dark theme | Equal attention: every fix is checked in light and dark, Bone and Saffron |
| Back arrow | Keep ☰ on every screen (D5), but make the menu do more (see N1) |
| Home | Lists all of today's lessons, not only the next one (see H1) |
| Removing features | Only with the owner's yes, item by item (streamlining brief) |
| Phone checks | The owner installs each fix PR's debug build from Android Studio and checks the list in that PR |

Copy is recorded, never rewritten here. Wording waits for the owner's filled-in [`STRING_INVENTORY.md`](STRING_INVENTORY.md).

## How the audit was done

- **Sources read:** every Compose screen under `ui/`, `Tokens.kt`, `Color.kt`, `Shape.kt`, `Type.kt`, `strings.xml`, `PACER_DESIGN.md`, `EDUSUITE_STYLE_GUIDE.md`, `PACER_ACCENT_CONTRAST.md` and `PRODUCT.md`, on `main` at 0.4.4.0. The spacing change in 0.4.4.1 (merged while this brief was written) was read too, and the spacing findings take it into account.
- **Methods:** impeccable `audit` (native Android variant) and `critique` with its Android reference; taste-skill's app-screen rules and checklist (`references/app-screens.md`); the design-critique, accessibility-review, ux-copy and design-system checklists.
- **Not possible here:** the app can't run in the build sandbox (no Android SDK, no emulator), so there are no screenshots. Taste-skill's inspector and impeccable's detector read web pages only, so neither ran.
- **What that means:** every finding is from code. The *Seen* column says **code** when the code alone proves it, and **phone** when how it looks must be checked on a device. Contrast figures were computed from `Tokens.kt` with the WCAG formula.

## Scores

Impeccable native audit: **14/20, Good.**

| Dimension | Score | Key finding |
|---|---|---|
| Accessibility | 3 | TalkBack actions on every lesson, 48dp targets and large text are thorough. Saffron light's progress bar, done tick and update dot fall under 3:1. |
| Performance | 3 | Lazy lists everywhere that matters; nothing measured on a device. |
| Theming | 3 | Strict tokens. Two lesson kinds share one edge, two kinds change hue between light and dark, and the design doc contradicts itself on the hero. |
| Platform conformance | 2 | Three different sheet frames, actions that only a long-press reaches, and no way up except system Back. |
| Adaptivity | 3 | Panes and the permanent drawer from 600 and 840dp; only Home and the welcome page cap their width on tablets. |

Impeccable critique (Nielsen): **26/40.** Weakest: consistency (2), recognition rather than recall (2), minimalist design (2), help (2).

Design specificity: the bones are Course Pacer's own (the ledger rows, kind edges, the pacing board, whole-lesson verdicts). The surfaces around them read as a well-applied Material kit: neutral pills, a greeting header, an icon in a tinted circle for empty states, numbered circles on the welcome page, equal grey buttons. "Push further" is about giving those surfaces the product's face.

## Findings that cross screens

| # | Finding | Seen | Severity |
|---|---|---|---|
| X1 | *(progress bar, 0.4.5.0; done tick with Plan; update dot **done 0.4.9.0**: it uses `mark`, 10.4:1 on the menu and 9.4:1 on the bar)* **Saffron light marks under 3:1.** The progress bar fill (accent on surfaceContainer, 1.41:1), the done tick's fill (accent on surface, 1.73:1) and the update dot on ☰ (1.73:1) carry meaning but fail WCAG 1.4.11. Bone and both dark sets pass. | code | P1 |
| X2 | *(0.4.6.0)* **Two kinds, one edge.** Worksheet lessons and supplementary pages both use `edgeSupp`, so the edge can't tell them apart (D8 promises one edge per kind). | code | P2 |
| X3 | *(0.4.6.0)* **Edges change meaning with the mode.** In Saffron, *edited* is green in light and saffron in dark; *free* is mustard in light and green in dark. Saffron light's test edge equals `warn`, so a test reads as a problem. | code | P2 |
| X4 | **The design doc contradicts itself.** Its Tokens section says the Home next-lesson and group today panels are neutral; its group home section and the owner's decision say the hero is accent-filled. The code follows the decision on group home and Plan, and the neutral rule on Home. | code | P2 |
| X5 | *(0.4.5.0)* **Neutral pills are hard to see.** Group home's pills and Classes chips are `surfaceContainer` on `paper`: 1.08:1 in Bone light, about 1.1–1.4:1 elsewhere. They read as text, not buttons. | phone | P2 |
| X6 | **Hidden actions.** Group settings' book options, the hero's pages, Stats' Pace needed (Own pace) and a book's pace track (set position) work only by long-press or an unmarked tap. Hints have to tell people to long-press. | code | P2 |
| X7 | *(done, 0.4.10.0: one frame, `PacerSheet`)* **Three sheet frames.** `HomeSheet` and `SheetFrame` (Material bottom sheets) and `WorkSheetFrame` (a dialog drawn as a sheet) open, close and end differently: no button, a filled Done that only closes, or ✕ plus Save. | code | P2 |
| X8 | **Leftover components.** `ListItemRow`, `AddButton`, `Heading`, `CardRow` and `RemoveButton` in `Components.kt` are unused; `Section` (Settings) and `SectionHead` + `RowGroup` (list screens) do the same job two ways. | code | P3 |
| X9 | *(week and month controls, 0.4.6.0)* **Same job, different controls.** Previous/next week uses arrow text buttons; previous/next month uses chevron pills. Day-off rows have a ⋮ with Remove only and a long-press menu with Edit and Delete. | code | P3 |
| X10 | **Tablet widths.** Home caps at 640dp and the welcome page at 560dp; list screens, Stats and sheets stretch to the full pane. | phone | P3 |
| X11 | **Generic surfaces.** Taste-skill flags a greeting header with a logo, a tinted circle with an icon for empty states, numbered circles for steps and rows of equal grey buttons as kit habits. Bone light's warm paper with rust also matches its "cream with terracotta" pattern; the brand keeps it, so rust stays for *behind* only. | code | P3 |
| X12 | *(done, 0.4.10.0)* **Picker has an unnamed second node** (usability audit F10). `Picker` (`ui/Components.kt:360-389`) is a labelled read-only field with a separate clickable overlay (`Role.DropdownList`) that has no name or value. TalkBack meets two items, and the menu items show no selected state. Seven consumers, among them the lead book, book type, "Applies to", units and homework content. Fix: one merged named node (label, choice, role), or Material 3's `ExposedDropdownMenuBox`. | code | P2 |
| X13 | *(done, 0.4.10.0)* **Switch labels are separate nodes** (F11). `SwitchRow` (`ui/ClassScreens.kt:402-411`, six consumers) and Work's "Takes a lesson" switch (`WorkScreen.kt:186-190`) have a label that is neither part of the switch's node nor a touch target. H6 is the same pattern on a checkbox. Fix: `toggleable(role = Role.Switch)` on the row, with the child callback null. | code | P2 |
| X14 | *(0.4.6.0)* **Book labels shift between groups and screens** (I5). Lesson rows use type labels (SB, WB1, WB2); Stats and Print use full names. Labels are recomputed per group, so a group that drops WB1 shows its other workbook as WB while sibling groups show WB2. Recommended: one stable class-wide abbreviation in compact views, full names in Print and Stats, and the full name always for TalkBack. | code | P3 |
| X15 | **Wider window, narrower panes** (F12). Layout follows the whole window (`ui/Layout.kt:22-35`). At 840dp the 320dp permanent drawer arrives (`NavUi.kt:176`) on top of the 40/60 split (`PacerApp.kt:293,303`), so the panes drop from about 336/503dp at 839dp to 208/312dp. Proposal: show the permanent drawer only when both panes keep a minimum width (for example 320 and 360dp). Check at 599, 600, 839, 840 and 1024dp. | phone | P2 |
| X16 | **Done (0.4.9.0).** `Screen.Tests` and `Screen.Homework` (`MovedWorkScreen`) are removed. Home's tests card had opened Tests since 0.4.8.0, so its rows now open their group's Work page. | code | P3 |
| X17 | **Deletes with no confirmation.** Delete this school year, Delete this class and Delete group act on one tap; the Undo message is the only way back, and it closes after a later edit. Proposal: one confirm step for the three, or a longer-lived Undo. | code | P2 |
| X18 | **Done (0.4.9.0).** Make a custom plan in ⋮ (large text) gives the same Undo message as Create on Plan. | code | P3 |
| X19 | *(Plan, 0.4.6.0; Home's quiet prompt with PR 3)* **A sheet opens by itself.** Ticking the last lesson of a unit (Plan's tick or Home's Done) opens the unit revision sheet unasked, mid-class. Proposal: a quiet prompt on the row or Home tile that opens it. | code | P2 |
| X20 | **Undo history clears silently.** Creating or switching a school year empties Undo, and the bar's Undo count drops to 0 without a word. | code | P3 |
| X21 | *(Plan unchanged; Group settings and School year with PR 6)* **Long-press means different things on a day off.** On Plan it offers only Add extra lesson…; in Group settings it offers Edit and Delete; on School year it offers Remove (X9's pattern). | code | P3 |

Bone and Saffron dark use green-tinted grounds (D1). Taste-skill prefers neutral darks; the brief keeps D1, as the brand decided.

## Findings per screen

### Group home

| # | Finding | Seen | Severity |
|---|---|---|---|
| G1 | *(0.4.5.0)* **Today's pages aren't the biggest thing.** The hero shows the date in `titleMedium` and the pages in `bodyLarge` on one line that cuts off with "…". With three books ("SB 38–40 · WB 20–21 · GB 40–41") the most important text may be cut on a narrow phone. | phone | P1 |
| G2 | *(0.4.5.0)* **Tapping the pages records what was taught, with no sign that it can.** No icon, underline or chevron. | code | P2 |
| G3 | *(declined by the owner: the pills stay at the bottom)* **The pills sit under Due soon.** With four homework rows the four pills (Plan first) may start below the first screen on a phone. | phone | P2 |
| G4 | *(0.4.5.0)* **"Short by 2 lessons" leads to Stats, not to the fix.** The fix is behind the warning icon in the top bar (see the streamlining brief). | code | P2 |
| G5 | *(0.4.5.0)* Progress strip bar fails 3:1 in Saffron light (X1). | code | P1 |
| G6 | *(0.4.5.0)* In Saffron dark and Bone dark the hero is a large light fill on a dark ground; check it doesn't glare at night. | phone | P3 |

**G1 in detail (usability audit F09).**

- In compact layout, the pages line and the tests/note line use `maxLines = 1` (`ui/TodayBox.kt:184-188`).
- The stacked layout depends on large text only (`TodayBox.kt:164`; `largeText` ignores width).
- The pages column clears its semantics (`:182`), so TalkBack reaches "record pages" only through the lesson menu.

PR 1 should:

- wrap or stack the pages by measured width, not only at large text;
- give the pages a named click action;
- keep Part B's `heroActions` guards.

Acceptance: every range is visible at 320dp, on a 360dp phone and in a 312dp pane, with four or more books and Greek names, at 100%, 150% and 200% text, with the full spoken text kept.

### Plan

| # | Finding | Seen | Severity |
|---|---|---|---|
| P1 | *(0.4.6.0)* **Today's lesson shows twice in Weekly view.** The day strip starts on the group's next lesson (`report.lessons.first`), and the row under the strip repeats the lesson already in the hero (breaks "one fact, one place"). | code | P1 |
| P2 | *(0.4.6.0)* **Three rows of controls before the list:** Weekly/Monthly, My plan/Proposed and the day strip or month arrows, plus Add extra lesson. Taste-skill's rule: the first list row starts in the top 60% of a phone screen. | phone | P2 |
| P3 | *(0.4.6.0)* **Tapping a day off adds an extra lesson.** A slim day-off row's tap opens Add extra lesson, which a teacher won't expect. | code | P2 |
| P4 | *(0.4.6.0)* A whole-lesson test is a slim muted row in `bodySmall`, quieter than an ordinary lesson. | phone | P3 |
| P5 | *(0.4.6.0)* Lesson rows use a 96dp date column, ledger rows 112dp, so dates don't line up between screens. | code | P3 |
| P6 | *(0.4.6.0)* Lesson menus run to 15 items (homework, extras, custom-plan moves, recording). | code | P3 |

### Home

| # | Finding | Seen | Severity |
|---|---|---|---|
| H1 | **Only the next lesson.** On a day with three lessons Home shows one ("1 of 3 today"). Teachers start from a day; the owner chose to list all of today's lessons. **Done (0.4.8.0):** the owner dropped the list of today's lessons. Home marks the groups with a lesson today on the board, and lessons tick themselves ([`AUTO_TICK_BRIEF.md`](AUTO_TICK_BRIEF.md)). | code | P1 |
| H2 | **Today's lesson isn't primary here.** The next-lesson bar is neutral with an accent Done; on group home the same lesson is accent-filled (X4). **Done (0.4.8.0):** Home no longer has a today block, so there is nothing to fill. | code | P2 |
| H3 | The greeting header (logo, "Hello, name", date) takes the first screen's best place (X11). D3 keeps it; the redesign can make it smaller. **Done (0.4.8.0):** the header stays as it was. | phone | P3 |
| H4 | Classes scroll sideways, so classes past the third or fourth are out of sight. **Done (0.4.8.0):** the Classes card is gone; the board's cells and the menu reach every class. | phone | P3 |
| H5 | The board's legend shows an outlined square; on-pace cells have no outline. **Done (0.4.8.0).** | code | P3 |
| H6 | New school year: only the checkbox toggles Greek holidays, not its label. **Done (0.4.8.0).** | code | P3 |

**H1 and the usability audit (F16, I1).** Today's bar takes the earliest pending lesson across all groups (`ui/HomeModel.kt:35-55`). Its Done ticks that lesson even when it is overdue (`markNextLessonDoneIn`). The group screen's hero skips overdue lessons (`GroupScreen.kt:182`). The today list should therefore:

- keep earlier unticked lessons in their own alert that opens them;
- never tick anything automatically;
- give each Done the right group and date.

Lessons now tick themselves, so there is no overdue lesson to tick by mistake, and Home has no Done. The large-text board's "N groups on pace" counted unmeasured groups too; that is fixed in 0.4.8.0 (F07): they show as "set-up needed".

### Navigation, top bar and group menu

| # | Finding | Seen | Severity |
|---|---|---|---|
| N1 | **Done (0.4.9.0).** The menu's Classes & Groups list opens at the teacher's place (class, group or its pages) with the current page marked and a back row up. The owner kept the five main items fixed and chose no link on the context line and no recent groups. | code | P2 |
| N2 | **Done (0.4.9.0).** The group menu shows only the sections that have items, Work leaves out the lesson sections, and every item has an icon. | code | P3 |
| N3 | **Done (0.4.9.0).** When the title and buttons don't fit, Undo and the warnings fold into ⋮, which carries the count. | phone | P3 |

### Stats and Work

| # | Finding | Seen | Severity |
|---|---|---|---|
| S1 | Stats' My plan/Proposed switch is separate from Plan's; changing one doesn't change the other. | code | P2 |
| S2 | The pace track opens Stats on group home but opens "set position" on Stats: same component, different tap. | code | P2 |
| S3 | Pace needed opens Own pace on a plain tap with no affordance (X6). | code | P3 |
| W1 | Work is consistent (chips, sections, swipe, ⋮, Add button). Its empty state uses the tinted-circle pattern (X11). | code | P3 |

### Class, Group settings, School year, Units

| # | Finding | Seen | Severity |
|---|---|---|---|
| C1 | **Leftover links.** Class still has a "Work" section linking each group's Work, kept "for one release" in 3.11.0. | code | P2 |
| C2 | **A group is edited in two places.** The pencil on Class opens a sheet (name, days, next lesson); Group settings has dates, days off, books and units. Lesson days are only in the sheet. | code | P2 |
| C3 | Units is the second row of Class, above Groups and Books, and is also in the class settings sheet. | code | P3 |
| C4 | Group settings: default books have no tap action, only long-press; two hints stack above the list. | code | P2 |
| C5 | Group settings' Dates repeat Class's "Next lesson" field. | code | P3 |
| Y1 | School year and Classes follow the ledger rules well; spacing is fixed by 0.4.4.1. | code | none |

**C2 detail (F17).** `renameGroup` and `toggleDay` have no caller outside the pencil sheet. The "has no teaching days selected" problem routes to Class (`ui/Nav.kt:165`), so it should open Group settings once S2 lands.

### Sheets, dialogs and notices

| # | Finding | Seen | Severity |
|---|---|---|---|
| D1 | **Done (0.4.10.0).** The three sheet frames end differently (X7). | code | P2 |
| D2 | *(done, 0.4.10.0: a Print or save PDF row on Class and School year)* Print for the whole class or year is only reachable from one group's ⋮. | code | P3 |
| D3 | **Done (0.4.9.0).** The update dot uses `mark` (X1); Settings' Updates section was already clear. | code | P2 |
| D4 | The welcome page is a numbered four-step list with accent circles (X11). | phone | P3 |
| D5 | Warnings, Catch up and the group switcher follow the design. | code | none |

## Copy issues recorded (not changed)

- One thing, two names: "custom plan" and "My plan".
- Near-synonyms for fixes: Re-pace, Re-pace untouched lessons, Re-pace to fit, Tidy, Fit to term, Catch up, Adjust catch-up, Bring in step.
- Hints that teach gestures ("Long-press a lesson…", "Long-press a book…") point at hidden UI (X6).
- Several hints are three sentences long under a field; taste-skill's rule is words of the trade, one line.
- "Done" (button) and "Mark done" (TalkBack) for the same act.

## Proposed changes to `PACER_DESIGN.md` (each needs a yes)

1. **Today's lesson is the accent fill wherever it is the screen's focal point** (group home, Plan, Home's today block), replacing the neutral-panel line (X4, H2).
2. **D5, enhanced (approved 0.4.9.0, in a different form):** ☰ on every screen, and the menu's Classes & Groups list opens at the teacher's place with the current page marked and a back row up (N1).
3. **Saffron light marks:** progress fills, ticks and dots use `ink` or `accentText`, never the fill accent, on light paper (X1).
4. **One hue per kind in every mode,** and a worksheet edge of its own (X2, X3). `ContrastTest` gains a check that kinds keep their hue family across modes.
5. **No action by long-press alone:** every long-press item also has a visible way in, as removal already does (X6).
6. **Pills have a visible edge:** a 1px `outline` stroke or a tone step that reaches 3:1 against the ground (X5).
7. **D4, revised:** Home's first block lists today's lessons, each with Done (H1).
8. **Signature elements** (push further): see below.
9. **Tablets:** the permanent drawer appears only when both panes keep a usable width, replacing the plain 840dp rule (X15).

## The bolder direction (push further)

Same palettes and Outfit, flat (D12 stays), used with more conviction. To try in the first fix PRs and keep only what the owner likes on the phone:

- **Pages as the headline.** Today's pages in large tabular figures, one book per line ("SB 38–40"), the date small above them. The hero becomes a bookmark for today.
- **The pacing lines.** The launcher's three pacing lines become the progress mark: a thin line per book, ending where the group is, with the lead book's tick (the Pace track already holds this data).
- **Ledger gutters.** Plan's week sections with a day-number gutter in figures and the kind edge as the only colour, so a week reads like a page of a register.
- **Words of the trade** in empty states instead of an icon in a circle, once the owner's wording is in.
- **Fewer grey buttons.** Group home's pills become one row of outlined pills with the current page underlined on group pages.

## Fix pull requests, in order

Each is its own session and PR, approved by the owner one at a time. Redesign work is a B-level version step (`0.4.B.0`). Sizes: S under a day, M one to two days, L more.

| # | PR | Covers | Size |
|---|---|---|---|
| 1 | **Group home and the hero** | G1–G6 (G1 includes F09), X1 (progress bar), X5 (pills), D-proposals 1, 3, 6, the pages headline and pacing lines | M |
| 2 | **Plan** | P1–P6, X14, X2, X3, X9 (week and month controls), X19, X21, D-proposal 4, ledger gutters | L |
| 3 | **Home today** | H1–H6 (with F16 and F07), D-proposal 7; needs a `HomeModel` change and tests | M |
| 4 | **Menu and top bar** (built, 0.4.9.0) | N1–N3, D-proposal 2, X1 (update dot), X16, X18 | M |
| 5 | **One sheet frame** (built, 0.4.10.0) | X7, D1, D2, X12, X13, F18 (streamlining S4); every sheet ends the same way | L |
| 5a | **Modules foundation** (built, 0.4.11.0) | [`FEATURE_MODULES_BRIEF.md`](FEATURE_MODULES_BRIEF.md): the module registry, stored modules, gates in the menu models, the import question, the slim catch-up bar; no visible change for existing installs | M |
| 6 | **Set-up screens — implemented in 0.4.12.0; device validation pending** | C1–C5 (with F17), X6 (Group settings), X17, X20, D-proposal 5; removals only with the owner's yes; the book form, Class, Group settings and Units module gates | L |
| 7 | **Work module gates — implemented in 0.4.13.0; device validation pending** | Work's assignment types and filters, single-type selector omission, and the hero's Homework button. The owner declined S1–S3 and W1 for this PR; Stats and Work's empty-state design stay unchanged. | S |
| 8 | **Settings, welcome, empty states** | D3, D4, X8, X10, X11; the modules launch: Welcome presets, Settings › Features, hints, What's new, the first release name | M |
| 9 | **Polish and record** | dark-mode and tablet pass over all screens (module screens included), X15 and D-proposal 9, `PACER_DESIGN.md` updated with what was approved, the deck and the module motion | M |

Each PR checks its screens' rows in [NAVIGATION.md](NAVIGATION.md) first and updates them with the change.

The modules work rides PRs 5a to 9 so no screen is redesigned twice ([`FEATURE_MODULES_BRIEF.md`](FEATURE_MODULES_BRIEF.md#order-of-work)). It needs the owner's yes per PR like the rest; each of these PRs also counts its journeys' taps before and after, and none may get longer.

Copy changes wait for the inventory and can ride any PR after it comes back.

The data, engine and output fixes that must come before or alongside these PRs (save status, Tidy and Fit preservation, one today, removed-book history, setup checks, print, backup) are in [`PLANNED_FEATURES.md`](PLANNED_FEATURES.md), with the combined order in its Order of work section.

## What to check on the phone (each PR lists its own)

Install the PR's debug build from Android Studio, then for each changed screen:

- light and dark, Bone and Saffron (Settings → Theme);
- a phone in portrait and, if you have one, a tablet or landscape;
- font size at its largest (the phone's Settings, under Display or Accessibility);
- a group with three books, a long group name, homework due today and a test this week;
- TalkBack on: the lesson row and the hero read their pages and offer Done and the menu actions.

Send screenshots of anything that looks wrong, so the next session sees what you saw.

The full matrix (widths 599 to 1024dp, 150% text, switch access, Remove animations, midnight and time-zone changes, process death, synthetic data) is entry 9 of [`PLANNED_FEATURES.md`](PLANNED_FEATURES.md). Run it before PR 9.

### Already right, keep it

- Lesson rows are one TalkBack node, with actions from the same model as the long-press menu.
- Shared buttons are 48dp and rows 56dp.
- Motion snaps when animations are off.
- Kinds of lesson and work are named in words.
- The Plan rows' "differs from the proposed plan" marker is spoken.
- Plan tools use explicit preview and apply.
- The app opens on Home at launch (an owner decision).
