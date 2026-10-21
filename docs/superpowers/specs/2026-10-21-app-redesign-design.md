# Course Pacer app redesign: design

Agreed with the owner on 2026-10-21 in a brainstorming session, which included Impeccable `init`, `shape` and an
Android-mode `critique`, and the taste skill's app-screen rules. It is classed as **architectural**: a new visual
language and new navigation across every screen, with no engine change. The implementation plan will be
`docs/superpowers/plans/2026-10-21-app-redesign.md`. The product record is `PRODUCT.md`.

## Context

Course Pacer grew one feature at a time, and on a 360×800 phone the group screen's first lesson started about 1056dp
down. The owner wants a fresh, coherent look and a structure that suits 9 or more classes and 16 or more groups, in
class (a quick glance, ticking the lesson) and at the desk (planning, catching up), on phones and tablets alike.
Course Pacer becomes the launchpad for a future EduSuite guide. For now every departure stays Pacer-only.

## What stays hard

- **Engine parity and its four approved extensions** (custom plans, the lead book, warnings with catch-up,
  supplementary books). The redesign changes no engine code. Rounding figures for display (below) is UI formatting.
- Approved dependencies only. Compose only. Tokens, not literals, checked by `ContrastTest`. UI text in
  `strings.xml`. No icon library: new Material Symbols are added as path data in `AppIcons.kt`.
- `applicationId`, the signing key and `minSdk 26` never change.
- Every PR bumps the version and keeps the docs current. Nothing risky goes in public docs.

## Decisions

| # | Decision |
|---|---|
| 1 | **Direction:** a new Pacer-only visual language. Bright and playful, and precise like an instrument. Teal accent, Outfit, flat, no shadows at rest. |
| 2 | **Figures** (dates, pages, counts) in Outfit with tabular figures (`fontFeatureSettings = "tnum"`), replacing `FontFamily.Monospace` everywhere. |
| 3 | **Navigation:** a ☰ menu, a modal drawer on phones and a permanent drawer on tablets. It holds search ("Find a class or group"), Home, Pacing overview (with its count), Classes (an expandable list of classes), School year and Settings. Every top bar starts with ☰ instead of a back arrow, and the system Back gesture and button still pop the back stack. There is no bottom bar. |
| 4 | **Group switcher:** tapping the group title opens a sheet with search, recent groups and the class's groups, each with its status. |
| 5 | **Home:** "Hello, *name*", with the date and the open year under it. Then the next-lesson bar with Done, the Classes card (scrolling class chips and "All 9 ›"), and the **Pacing overview** board (one cell per group, grouped by class). Home has no other tiles. |
| 6 | **Where things moved:** the year chip goes to the School year screen, which now switches years. Days off live in School year. Tests and Homework are reached from the Class screen. Print is reached from the group's ⋮ menu, starting with that group. |
| 7 | **Overview is renamed Pacing overview** everywhere: the screen title, the menu, the Home board and its "See all ›". It is laid out class by class, with a sort control ("Groups: most behind first", by name, by next lesson) that reorders groups only inside each class. |
| 8 | **Group, Plan tab:** today's lesson box comes first, with Done and **Pages taught**. Under it is the custom-plan line ("Start from this schedule and make it yours." / "The proposed plan stays as your fallback." with Create), shown until a plan exists, where the My plan \| Proposed switch takes its place. Lessons follow in week sections ("This week", "Next week", then date ranges). Rows are one line: date, book initials with pages, and a tick. Each kind has its own edge colour. Days off and whole-lesson tests are slim rows with no lesson number. Done lessons sit above today, and the list opens scrolled to today. |
| 9 | **Warnings** leave the page. A warning button with a count in the top bar opens the Warnings sheet, which lists each warning with its fix (Catch up, Bring in step, coverage fixes). |
| 10 | **Figures as whole lessons:** every lesson count is rounded up, and anything under 1 shows as 1 (3.33 → 4, 0.3 → 1). That includes the Stats verdict (overriding the earlier "Stats keeps the exact figure"). Every status label comes from one verdict and one rounding helper, so a group reads the same on Home, the board, the switcher, Class, Pacing overview, Warnings and Stats. Pages per lesson keep two decimals, and only on Stats. |
| 11 | **Stats:** the verdict as a heading ("Short by 2 lessons"), followed by a one-sentence fix. Then one divided strip (Lessons left, Pages left, Pace needed). Then a **pace track**: a bar per book, plus a tick where it would be in step with the lead book (the engine's existing `followLead` target; no new metric). Then lessons by month as columns and a plain details list. |
| 12 | **Colour for status:** on-pace is neutral (`surfaceContainer` with ink, or grey "On pace" text). Only groups that are behind take colour: rust cells on the board, pale `warnSoft` pills with `onWarnSoft` text. Each row carries one status mark. |
| 13 | **Lesson-kind edge tokens (Pacer-only),** at least 3:1 against `surface` in both modes: `edgeTest`, `edgeDayOff`, `edgeEdited`, `edgeHand`, `edgeFree` and `edgeSupp`. The words still name each kind. |
| 14 | **Dark mode has its own palette** (Pacer-only): a near-black ground with bright accents, so status and actions stand out against the dark background. Values are below. |
| 15 | **Dropped by the owner before it was built (3.6.0):** the background pattern. Original text: **Background pattern** (light mode only): a seamless 324dp tile of tilted Material Symbols Rounded school icons (book, cap, flask, pencil, ruler, compass, bulb, globe, palette, backpack, clock, note, clip, scissors, ball, calculator, microscope, abc, 123, fx, rocket and others), drawn in `ink` at 14%. There's no pattern in dark mode or on the welcome page. Lists and header text sit on a solid paper sheet with rounded top corners, so no text sits on the pattern. |
| 16 | **Large-text mode** from font scale 1.5. Android's non-linear scaling applies. Layouts reflow (buttons go full width under their text, and rows stack the date over the pages). Each screen also shows less and abbreviates more, but anything hidden stays one tap away: "Classes ›", "13 groups on pace ›", "6 classes all on pace ›" and "More stats ›". The custom-plan line moves to ⋮, and a top bar's title is cut short with "…". |
| 17 | **Tablet:** a permanent drawer, a list pane (Pacing overview or Classes) and a detail pane (group). The width breakpoint comes from `LocalConfiguration.screenWidthDp`, since no new window-size library is added. There's one set of actions per window, in the detail pane. The list pane has no ☰ or Undo, and the chosen row is highlighted. |
| 18 | **Undo** sits in the top bar only on screens that change school-year data: Group, Class, Classes, School year, Tests and Homework. Settings, Pacing overview and Home have none. Every timed snackbar shows its countdown as an emptying bar, with no words. |
| 19 | **Settings** gains "Your name" (Home says "Hello" alone while it's empty) and "Show tips again". The theme becomes a segmented switch (System, Light, Dark). "Show once" hints are remembered for the whole app. |
| 20 | **The welcome guide** becomes a full first-run page: the logo, the same words, the four steps as a numbered list, "Create your school year" and "Not now". |
| 21 | **Tests and Homework** are restyled only (dated rows, the test edge, ⋮ with Remove on each row as the visible way to remove one). Their overhaul comes later and isn't specified here. |
| 22 | **Book initials** are worked out in the UI ("Student's Book" → SB) for one-line rows. TalkBack and the lesson sheet use the full names. There's no data change. |
| 23 | **Touch targets** are at least 48dp: segmented switches, buttons, text links (Edit, Show, Reset, Add), board cells and ticks. |
| 24 | **Print** offers three choices as radio buttons: this group, every group in the class, every group this year. Then the margins and Print. |

Kept from earlier decisions: lesson reordering in custom plans (drag grip plus Move earlier/later), the warning colours,
the "Next" badge (now today's box), slim day-off rows, and the pace track.

## Departures from the shared guide (all Pacer-only, by the owner's choice)

`docs/EDUSUITE_STYLE_GUIDE.md` stays byte-identical. The departures are recorded in a new `docs/PACER_DESIGN.md` as
candidates for the next suite guide.

| # | Guide | Course Pacer |
|---|---|---|
| D1 | §2.1/§2.2/§10: the dark set is deep, low-light versions | Its own bright dark palette |
| D2 | §1/§3: figures in monospace | Outfit with tabular figures |
| D3 | §6 Home header: "Hello" with a Settings button | "Hello, name" with the date and year. ☰ opens the menu. Settings is in the menu. |
| D4 | §6 Tile grid (bento) on Home | Next-lesson bar, Classes card and Pacing overview board |
| D5 | §15: a back arrow on every screen; no drawer | ☰ menu on every screen, and a permanent drawer on tablets |
| D6 | §6 List row: a leading icon | Ledger rows with no icon. Edge colours mark the kind. |
| D7 | §6 Warning banner on the screen | A warning button with a count, and the Warnings sheet |
| D8 | §2: no kind colours | Six edge tokens |
| D9 | §6 GuideCard or dialog for the welcome | A full first-run page |
| D10 | §6 segmented control | A filled segmented switch with an ink selected side, 48dp |
| D11 | §2.2 tiles carry status (mint for done) | On-pace is neutral and only behind is coloured. Pills are pale tints. |
| D12 | §1 Flat, colour blocks only | ~~A light-mode background pattern under solid sheets~~ Dropped (3.6.0) |

## Tokens

Light values stay as the guide gives them. New Pacer-only tokens and the new dark set:

| Token | Light | Dark |
|---|---|---|
| paper | `#F6F5FA` | `#0E0E12` |
| surface | `#FFFFFF` | `#1A1A21` |
| surfaceContainer | `#E8E6EF` | `#26252E` |
| line | `#E4E2EA` | `#2C2B35` |
| outline | `#7A7686` | `#8F8C9C` |
| muted | `#5E5A6B` | `#B9B6C4` |
| ink | `#1B1A22` | `#F4F3F8` |
| accent / onAccent | `#245E60` / `#FFFFFF` | `#4FD1C5` / `#062A27` |
| accentSoft / onAccentSoft | `#D5EAE8` / `#0F2E2F` | `#163C3C` / `#D5F5F1` |
| ok / onOk | `#3E6E47` / `#FFFFFF` | `#8BE3AE` / `#06301A` |
| warn / onWarn | `#9E4A24` / `#FFFFFF` | `#FF8F6B` / `#3A1206` |
| warnSoft / onWarnSoft | `#F7E4DA` / `#3E1A0A` | `#3A2018` / `#FFD2C2` |
| edgeTest | `#6E4BC4` | `#C9B8FF` |
| edgeDayOff | `#7A7686` | `#8F8C9C` |
| edgeEdited | `#245E60` | `#4FD1C5` |
| edgeHand | `#1F6F8B` | `#7FD4F0` |
| edgeFree | `#A35A00` | `#FFC266` |
| edgeSupp | `#B03A84` | `#FF9AD5` |
| patternInk (alpha) | ink at 14% | not used |

Contrast was measured on the mock-ups:
- Dark accent on surface: 9.28. On-accent on accent: 8.23. Dark warn on surface: 7.74. Pale pills: 12.58 light and 10.89 dark.
- Grey text over a pattern stroke: 4.62. That is why text never sits on the pattern.
- `ContrastTest` splits in two:
  - the shared light rows stay checked against the guide;
  - the new dark set and the edge tokens are checked against a Pacer table in `docs/PACER_ACCENT_CONTRAST.md`, renamed in the docs to "Course Pacer contrast".

Status rendering is decided in rule 12 above. The tile colours remain only for the welcome page's step numbers and are
otherwise retired from screens.

## Components and code shape

- **`ui/theme/`:**
  - `Tokens.kt` gets the new dark values and the edge tokens.
  - `Type.kt` replaces `Mono` with `Figures` (Outfit + `tnum`), and every use of `Mono` moves to it (about 36 places).
- **`ui/Nav.kt`:** the screens stay the same. The menu pushes `Screen`s through the existing rules, with
  `Screen.Overview` kept and its label renamed. Nav tests cover the menu destinations and the Back behaviour.
- **New composables:**
  - `AppDrawer`, `GroupSwitcherSheet`, `WarningsSheet` and `PacingBoard` (Home).
  - `LessonLedger` with `WeekHeader`, `LessonRow`, `SlimRow` and `TodayBox`.
  - `PaceTrack`, `FigureStrip`, `PatternBackground` and `SolidSheet`.
- **New pure Kotlin helpers, unit-tested:**
  - `wholeLessons(x: Double): Int` (round up, at least 1, and 0 stays 0 only where a count is truly zero).
  - `weekSections(rows, today)`, `bookInitials(name)`, `sortGroups(byClass, order)` and `largeText(fontScale)`.
- **Removed or folded:** `HomeGrid`/`TileGrid` on Home, `StatusChip`, the on-page `WarningBanner`, the `Tips`
  `MoreOptions`, `YearChip` and the old `WelcomeDialog`.
- **`ui/PacerApp.kt`:** the snackbar keeps its bar and loses no behaviour. The top bar takes `showUndo`.
- **Settings model:** adds `name: String = ""` and `seenTips: Set<String> = emptySet()`, stored in SharedPreferences
  (already backed up through `settings.xml`, outside undo). There's no JSON schema change.
- **Accessibility:**
  - Each lesson row is one merged TalkBack node with its custom actions (fixing the `GroupScreen.kt:540` concern).
  - Done lessons are no longer faded with `alpha`; their text is muted at full contrast.
  - The warning button reads "2 warnings".

## States

- **First run:** the welcome page.
- **Empty states:**
  - **No classes:** Home shows "Add your first class".
  - **No books or groups:** the Class screen shows its guides.
  - **No lessons left:** the group shows its empty line.
- **Status:** on pace or behind.
- **Custom plan:** off or on.
- **Modes:** light and dark, large text from 1.5, phone and tablet.

## Mock-ups

The mock-ups are HTML at 1dp = 1px, built from the real tokens, Outfit and strings, and rendered with Playwright at
411×891 (the owner's chosen size) and 360×800, in light and dark, at 200%, and on a tablet at 1280×800. The
before/after boards chosen by the owner are in `docs/images/redesign/`. Their classes and figures are sample data.

## Critique

The two-agent Impeccable critique in Android mode scored **27/40**:
- Assessment A was a design review.
- Assessment B ran Impeccable's detector on the HTML mock-ups, plus contrast and touch-target measurements.

All five priority issues have been fixed in the mock-ups and in this spec: decisions 8, 10, 12, 16, 17, 18 and 23, and
the pattern sheet in 15.

## Out of scope

- Any engine or data-format change.
- The Tests and Homework overhaul.
- Changes to Course Invoicer or to the shared guide.
- Localisation.
- New dependencies, including a window-size-class library.
