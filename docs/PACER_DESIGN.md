# Course Pacer design language

> **Status: approved, being built.** Agreed with the owner on 2026-10-21. It ships in 3.1.0 to 3.7.0 following
> `docs/superpowers/plans/2026-10-21-app-redesign.md`. Built so far: the navigation (3.2.0: the ☰ menu, the group
> switcher, the Warnings sheet and the Pacing overview name; D5 and D7, phones only). Until each other part lands, the
> app still follows `docs/EDUSUITE_STYLE_GUIDE.md` there. The full design is
> `docs/superpowers/specs/2026-10-21-app-redesign-design.md`.

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
| D1 | §2.1/§2.2/§10: the dark set is deep, low-light versions | Its own bright dark palette (below) |
| D2 | §1/§3: figures in monospace | Outfit with tabular figures |
| D3 | §6 Home header: "Hello" with a Settings button | "Hello, name" with the date and year. ☰ opens the menu. Settings is in the menu. |
| D4 | §6 Tile grid (bento) on Home | Next-lesson bar, Classes card and Pacing overview board |
| D5 | §15: a back arrow on every screen; no drawer | ☰ menu on every screen, a permanent drawer on tablets. System Back still works. |
| D6 | §6 List row: a leading icon | Ledger rows with no icon. Edge colours mark the kind. |
| D7 | §6 Warning banner on the screen | A warning button with a count, and the Warnings sheet |
| D8 | §2: no kind colours | Six lesson-kind edge tokens |
| D9 | §6 GuideCard or dialog for the welcome | A full first-run page |
| D10 | §6 segmented control | A filled segmented switch with an ink selected side, 48dp |
| D11 | §2.2 tiles carry status (mint for done) | On-pace is neutral and only behind is coloured. Pills are pale tints. |
| D12 | §1 Flat, colour blocks only | A light-mode background pattern of school icons, under solid sheets |

## Tokens

Light values are the guide's. Dark values and the edge tokens are Course Pacer's own.

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

Edge tokens are at least 3:1 against `surface` in both modes. Text pairs are at least 4.5:1. `ContrastTest` will check
both when the tokens land (3.1.0), against the table in `docs/PACER_ACCENT_CONTRAST.md`.

## Rules

- **Figures:**
  - Lesson counts are whole lessons rounded up, and anything under 1 shows as 1. One verdict feeds every label, so a
    group reads the same everywhere.
  - Pages per lesson keep two decimals, and only on Stats.
- **Status:** one mark per row. On pace is neutral. Behind is rust (a filled cell on the board, a pale pill in lists).
- **Lesson kinds:** a 4dp edge in the kind's token, and the words still say the kind.
- **Undo:** in the top bar only where school-year data changes. Timed snackbars show an emptying bar, with no words.
- **Background pattern:**
  - a 324dp tile of tilted Material Symbols Rounded school icons, `ink` at 14%;
  - light mode only, and not on the welcome page;
  - text never sits on it, because lists and header text sit on a solid paper sheet with 28dp top corners.
- **Large text** (font scale 1.5 and up):
  - layouts reflow;
  - screens show less and abbreviate more;
  - everything hidden is one tap away.
- **Touch targets** are 48dp or more, text links included.
- **Tablets:** a permanent drawer, a list pane and a detail pane, with one set of actions per window.

## Pictures

Before/after boards of the approved design are in `docs/images/redesign/`. Their classes and figures are sample data.

![Home](images/redesign/home.png)
![Group](images/redesign/group.png)
![Stats](images/redesign/stats.png)
![Classes, Class, School year](images/redesign/classes.png)
![Pacing overview, Tests, Homework](images/redesign/overview.png)
![Settings, welcome, sheets](images/redesign/settings.png)
![Large text](images/redesign/large.png)
![Tablet](images/redesign/tablet.png)
