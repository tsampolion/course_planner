# Streamlining

Status: approved 2026-10-25; audit done (stage 1, this brief). D1 and D3 are built in 0.4.5.0; the owner declined G3 (D2 waits for PR 3). R3, P3 and D4 are built in 0.4.6.0. The rest ride the fix pull requests in [`UI_AUDIT_BRIEF.md`](UI_AUDIT_BRIEF.md), and every removal needs the owner's yes.

This brief records the streamlining pass (`PLANNED_FEATURES.md` §3 until 0.4.4.2). For every screen it answers four questions: where the teacher arrives from, what the screen is for, what the next step is, and how it ends (done, back, undo). It then lists what to remove, merge or move.

Every control each screen offers today, and where it leads, is in [NAVIGATION.md](NAVIGATION.md). Check each proposal against it, and update it in the pull request that carries the change.

## Decisions settled with the owner

- **Flows:** all four matter (daily use, weekly planning at the desk, term set-up, first run).
- **Removing:** moving and merging is fine; each removal is listed under "Remove" and needs the owner's yes in its PR.
- **Navigation:** ☰ stays on every screen and gets more useful (M1). No back arrow.
- **Home:** lists all of today's lessons.
- **Wording** waits for [`STRING_INVENTORY.md`](STRING_INVENTORY.md). Nothing here touches the engine, the release, `applicationId`, signing or dependencies.

The audit was done from the code; the app could not run here (see the audit brief). Step counts are taps, from the code paths.

## Per screen

| Screen | Arrives from | For | Next step | Ends |
|---|---|---|---|---|
| **Home** | app launch; ☰ | today: the next lesson, classes, who is behind | Done; tap the lesson to open the group; a behind cell | stays; Done has Undo |
| **Group home** | Home's lesson or board; Class; Pacing overview; menu search; the switcher | this group today: pages, progress, homework due | Done or record pages; Plan / Work / Stats / Settings pills | Back; Undo in the bar |
| **Plan** | group home pill; tablet detail pane | the lesson-by-lesson schedule; custom plan edits | tick, record pages, plan a lesson (long-press) | Back |
| **Work** | group home pill or Due soon; Class (old links) | homework, tests and worksheets for the group | Add; edit a row | Back; swipe has Undo |
| **Stats** | group home pill or progress strip | why the group is on pace or short, by book | Own pace (hidden tap); set a book's position (hidden tap) | Back |
| **Group settings** | group home pill | the group's own dates, days off, skipped pages, books, units | edit a field; long-press a book or day off | Back; Undo in the bar |
| **Classes** | ☰; Home's Classes card | list and add classes and collections | open a class; New class | Back |
| **Class** | Classes; Home chip; ☰ | the class's books, groups and settings | add a book or group; open a group | Back; Undo |
| **Units** | Class row; class settings sheet | split books into units and link them | three steps (lead book, starts, links) | Back |
| **School year** | ☰ | term dates, days off, years | edit dates; add days off; Greek holidays | Back; Undo |
| **Pacing overview** | ☰; Home's See all | every group's status, sorted | open a group; warning button for the year's problems | Back |
| **Settings** | ☰ | name, theme, class defaults, backup, help, updates | change a setting | Back |
| **Welcome** | first launch; Settings → Getting started | what the app does, in four steps | Create a school year | Not now / Got it |
| **Warnings sheet** | warning button on a group | each warning with its fix | Fix (opens Catch up, etc.) | closes on fix; swipe down |
| **Group switcher** | the group's title | jump to another group or page | pick one | closes |
| **Assignment editor** | Work Add; a lesson's Add homework | one homework, test or worksheet | Save, then maybe "Schedule it in class" | ✕ or Save; Undo |
| **Print** | a group's ⋮ only | print a group, its class or the year | Print | Cancel |
| **Update notice** | dot on ☰; Settings | a new version | Download | stays in Settings |

### Where the four answers are weak

- **Plan, Stats, Group settings, Units:** they end only with system Back, and nothing on screen names the way up (M1).
- **Group home:** "Short by 2 lessons" is the warning, but its fix sits behind a small icon in the top bar; the strip opens Stats (D1).
- **Stats:** its two next steps are hidden taps (Own pace, set position).
- **Group settings:** book options are long-press only; lesson days are not here at all (S2).
- **Class:** its "Work" section is a dead end that only forwards to each group (R1).
- **Print:** class and year printing hide inside one group's menu.
- **Sheets:** some end with Save, some with a filled Done that only closes, some only by swiping down (audit X7).

## The four flows

### Daily use (in class)

| Task | Today | Proposed |
|---|---|---|
| Tick today's lesson | Home → Done: **1 tap** | same, for each of today's lessons (H1) |
| Record pages taught | Home → lesson → group home → tap pages → sheet → Save: **4 taps + typing** | Home's today block opens the pages sheet directly: **2 taps + typing** (D2) |
| Set homework for today | group home → long-press hero → Add homework, or Work → Add: **3–4 taps, one hidden** | a visible "Homework" action on the hero (D3) |
| See what's due | group home Due soon (4 rows) | same; cap stays at 4 with "All work ›" |

### Weekly planning (at the desk)

| Task | Today | Proposed |
|---|---|---|
| Find who is behind | Home board or Pacing overview | same |
| Fix a group that is short | board cell → group home → warning icon → Warnings → Fix → sheet → Apply: **6 taps** | the progress strip, when short, shows "Fix" that opens Warnings: **5 taps**, and the fix is in sight (D1) |
| Change a plan by hand | Plan → long-press a lesson → menu (up to 15 items) | menu grouped (this lesson / my plan / homework) and the custom-plan actions behind one "Edit my plan" item (D4) |
| Print the week for a class | open any group → ⋮ → Print → choose Class | Print also on Class and School year (M3) |

### Term set-up

| Task | Today | Proposed |
|---|---|---|
| Term dates and holidays | School year → Edit dates → Add Greek holidays | same |
| Add a class with books and groups | Classes → New class → Class → Add book (sheet per book) → Add group → pencil → days | same path, but a new group's days are asked for when it is added (S1) |
| A group's own days, dates and books | Class pencil sheet **and** Group settings | one place: Group settings (S2) |
| Units | Class row, or class settings sheet | one entry: under Books on Class (S3) |

### First run

Welcome → Create school year (dialog) → Home "Add your first class" → Classes → New class → Class "Add the books" → book sheet → Add group → pencil → days → open group: **about 10 steps**, each guided by a card. Proposed:

- The welcome page's four steps link to where each happens, and say which is done (F1).
- After "New class", the class asks for its first book and first group in one sheet (S1).
- The first group opens on its group home with a one-line "This is today's lesson" hint, shown once.

## Remove, merge, move

### Remove (each needs the owner's yes)

| # | What | Why |
|---|---|---|
| R1 | Class's "Work" section with a link per group, and its "moved" hint | kept "for one release" in 3.11.0; Work is on every group home |
| R2 | The old `Screen.Tests` and `Screen.Homework` routes (`MovedWorkScreen`) and their `NavTest` cases | same transition; nothing navigates to them (`ui/Nav.kt:23-24`, `PacerApp.kt:411-412`) and saved state can't restore them |
| R3 | The duplicate lesson row under Plan's day strip | it repeats the hero; the strip can scroll the list to the chosen day instead |
| R4 | The Units switch and link inside the class settings sheet | Units has its own row (S3) |
| R5 | Unused components (`ListItemRow`, `AddButton`, `Heading`, `CardRow`, `RemoveButton`) | dead code; no feature lost |

### Merge

| # | What | Into |
|---|---|---|
| S2 | Class's group pencil sheet (name, days, next lesson) | Group settings, which gains name and lesson days; the pencil opens Group settings |
| S4 | `HomeSheet`, `SheetFrame`, `WorkSheetFrame` | one sheet frame that always ends the same way: a filled action when there is something to save, ✕ otherwise. An untouched sheet closes at once. A changed draft asks Keep editing / Discard (F18: today Back, a scrim tap or ✕ drops it silently, `ui/WorkSheetFrame.kt:51-90`). A draft is never autosaved invalid, or restored into another group after switching or process death (I4: confirm, recommended, or restore). |
| S5 | Stats' and Plan's separate My plan/Proposed switches | one choice per group, shared by both pages |
| S6 | Day-off rows' ⋮ (Remove) and long-press menu (Edit, Delete) | one ⋮ with Edit and Delete |

### Move

| # | What | From → to |
|---|---|---|
| D1 | The way to fix a short group | top-bar icon only → also a "Fix" on the progress strip |
| D2 | Recording today's pages | group home → also Home's today block |
| D3 | Add homework for today | long-press menu → a visible action on the hero |
| D4 | Custom-plan lesson actions | flat long-press menu → a grouped menu |
| S1 | A new group's lesson days | after the fact, in a sheet → asked when the group is added |
| S3 | Units | second row of Class → under Books |
| M3 | Print for a class or the year | one group's ⋮ → also Class and School year |
| P3 | Tapping a day off on Plan | adds an extra lesson → shows the day off; Add extra lesson stays in its menu |
| G3 | Group home pills | under Due soon → directly under the progress strip (owner's call in the first fix PR; the pills stay pills) |

### The menu, enhanced (M1)

The owner keeps ☰ on every screen. To give every screen a visible way up:

- The menu opens with a **"You are here"** block: the open class, the open group with its pages (Home, Plan, Work, Stats, Settings), and the current one marked.
- The small context line above each title ("2026–27 · FCE") becomes a link to its parent (the class, or the year).
- Group pages keep the title switcher; it lists the group's pages first, then other groups, so one sheet serves both (today they are two sheets).
- Recent groups, kept for the session today, appear in the menu under search.

## Rules for every change

- **Progressive disclosure.**
  - Keep the pages needed to teach today visible. Hide detailed figures and rare tools, never what is needed to teach.
  - At large text, hide something only behind a named one-tap route, with an accurate count.
  - A state with no lessons, no books or no safe repair says what to do next. It never shows an enabled action that does nothing.
- **Live or staged.** Forms mix live fields with sheets that save on Save. First check whether teachers can tell which values already apply. Don't add a Save button to every field or a confirmation to every close.
- **Scope in sight.** Show group, class or year scope whenever a change affects other screens (a year day off moves every group).
- **Repairs stay safe.** A more visible "Fix" (D1) must only offer repairs that keep hand-edited lessons ([`TIDY_FIT_BRIEF.md`](TIDY_FIT_BRIEF.md)).
- **Also weigh** (hypotheses):
  - new classes start with Group A on Mon/Wed/Fri (S1's question at creation answers it);
  - some class-level problems have no direct fix link.

## Order

These ride the audit brief's fix PRs: M1 with PR 4 (menu), D1–D3 with PR 1 (group home), R3 and P3 with PR 2 (Plan), H1 and D2 with PR 3 (Home), S4 with PR 5 (sheets), R1–R2, R4, S1–S3 and S6 with PR 6 (set-up), S5 with PR 7 (Stats), R5 and F1 with PR 8. Each PR names the removals it carries so the owner can say yes or no to each.
