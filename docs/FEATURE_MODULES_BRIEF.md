# Feature modules

Status: approved 2026-10-29 (design and order); the foundation (step 2) is built in 0.4.11.0, and the launch (UI audit PR 8) is built in 0.4.15.0: Welcome presets, Settings › Features, two hints, What's new and the release name. Deck wording and the hint placement wait for the owner's check on a phone. The build is planned in `docs/superpowers/plans/2026-10-29-feature-modules.md` and rides the remaining UI audit pull requests (see [Order of work](#order-of-work)).

A new teacher meets every tool at once. This feature lets them start with pacing alone and turn on more as they need it, and lets a teacher who only paces keep a lean app for good. Modules only ever turn on.

## Why modules, not levels

The first idea was a ladder (1 plan, 2 homework, 3 tests …). The code says otherwise: features feed each other.

- Every warning fix writes a custom plan: catch-up and fold (`PacerViewModel.applyCatchUp`, `foldBacklog`), fit to term, own pace.
- Unit revisions are ordinary tests and worksheets (`UnitRevisionModel.kt`).
- Supplementary books live only in custom lessons (`saveLesson`).
- Homework can point at a test (`Assignment.linkedId`, `contentType`).

Teachers also differ: an exam class needs tests on day one, another group only homework. So the design is **presets over modules**: a preset is a starting set, and each module turns on by itself.

## Core and modules

**Core, always on:** school years and days off, classes, books (name, type, pages, skip pages, finish-by), groups (lesson days, days off, end date), the schedule, ticking and recording pages, auto-tick, Stats, backup, warnings and their fixes.

| Module | What it adds |
|---|---|
| **Work** | homework, tests and worksheets; the Work page, Due soon, Home's tests card; unit revisions |
| **Book tools** | lead book, units, optional books, a group's removed books and its own skips and finish-by dates |
| **Planning tools** | my plan and its tools (re-pace, tidy, fit, copy), own pace, extra lessons, supplementary books |
| **Output** | print, copy, share |

Nothing here changes pacing. Each engine extension already leaves the schedule unchanged when unused (its parity test), so a module that is off hides only ways to add things that do not exist yet. The engine is already built of separate pieces; the bricks below make the screens match it.

## Bricks

Each feature is a **brick**: it belongs to a module (or to the core), may need other bricks, and knows which saved data means it is in use. Every button, menu item, pill and form field names its brick, and a screen shows only the controls whose brick is on. Nothing on a screen tests a module directly.

| Brick | Module | Needs |
|---|---|---|
| Pacing | core | |
| Plan base (a group's own plan, as fixes write it) | core | |
| Catch-up and other warning fixes | core | Plan base |
| Homework | Work | |
| Tests and worksheets | Work | |
| Unit revisions | Work | Units |
| Lead book | Book tools | |
| Units | Book tools | |
| Optional books | Book tools | |
| Group book rules (removed books, own skips and finish-by) | Book tools | |
| Plan editor | Planning tools | Plan base |
| Own pace | Planning tools | Plan base |
| Extra lessons | Planning tools | |
| Supplementary books | Planning tools | Plan editor |
| Print | Output | |
| Copy and share | Output | |

- **A brick is on** when its module is on (core always is) and every brick it needs is on. Unit revisions therefore show only with both Work and Book tools; the Units screen hides that step otherwise.
- **Two baseplates** limit where bricks can go. The plan base carries the fixes, the plan editor, own pace and supplementary books. One assignment record carries homework, tests, worksheets and revisions, which is why those share Work.
- **Data in use always shows.** A brick whose data exists counts as on for showing that data, whatever its module.
- **Presets and modules are groupings of bricks.** Regrouping later (into levels, or more modules) changes the table above, not the screens. A new control cannot be built without naming its brick.

## Presets

Welcome asks once, on a fresh install, before the first school year:

- **Just pacing** (selected): the core.
- **Pacing and work**: core and Work.
- **Everything**: all four modules.
- **Choose tools…** opens the four modules as a checklist.

An install that already has data, or has seen Welcome, starts with every module on. Nobody loses a control on update.

## Turning a module on

- **Settings › Features** is one row. Its sheet lists each module that is off with what it adds and a **Turn on** button; modules that are on read "On", with no switch. There is no way to turn one off.
- **At the moment of need**, a one-line hint appears once, in place, under the control the teacher just used. Turning the module on there shows the new control in that spot, so the hint, the switch and the first use are two taps. Candidate triggers (open decision 1):
  - pages recorded into a lesson that are already ahead of the plan → "Set these as homework? Turn on Work";
  - a lesson moved by hand twice through "Edit pages taught" → "Plan lessons yourself? Turn on Planning tools".
- The sheet shows what each module produces: a small, real piece of the app (the Homework button, the ⋮ items), not an icon.

## Imports

Backup › Import reads the file first. If it uses a module that is off (a pure `inUse(data)` over existing fields), the teacher sees one question before anything changes:

> This backup uses tests and planning tools. Turn them on to bring it in?
> **Turn on and import** · **Cancel**

Cancel imports nothing. The same words serve the teacher's own backup on a new phone. Android's own restore brings `settings.xml` back with `plans.json`, so the two stay in step; a restored file with no stored modules counts as an existing install.

## Warning fixes at every preset

A fix that writes a custom plan works without Planning tools. While no lesson is planned by hand, Plan shows one slim bar instead of the plan editor: "Catch-up: 2 more pages a lesson until the group is back on pace" (or "A fix has adjusted this group's plan"), with **Back to the proposed plan**, which deletes the fix's plan with Undo. The editor appears once Planning tools are on, or once any group in the open year has a lesson planned by hand (data in use always shows). The launch adds a way to turn Planning tools on from the bar.

## What's new

Teachers who update see a short card deck once. It is a reusable `CardDeck`, so Help (planned feature 12) can use the same frame.

1. "Hi {name}. Course Pacer can now start simple." Then: "Turn on only the tools you use. Swipe to see them." (No name: "Hi.")
2. One card per module: its name, one line on what it is for, and a real fragment of the screen it adds to. A module that needs more gets a second card.
3. "Choose in Settings › Features", with a button that opens the sheet and then returns to where the teacher was.

The deck can be skipped from the first card and reopened from Settings › Help.

## Release names

From the release that launches this feature, A and B releases (`0.A.0.0`, `0.x.B.0`) carry a title from English literature, classic or contemporary; a C release keeps the name of the release it fixes and never adds or changes one.

- The name follows the date in the changelog heading: `## [0.4.13.0] - 2026-12-01 - Great Expectations`.
- Names start early, with 0.4.14.0 “To the Lighthouse” (the palette release), before this feature launches. `release.yml` already puts a name in the GitHub release title (“Course Pacer 0.4.14.0 · To the Lighthouse”; a heading without one keeps “Course Pacer <version>”); the update notice and the What's new deck show it once this feature ships.
- Installed apps read only the tag and the APK name, `release.yml` finds notes by the `## [version]` prefix and `DocsTest` reads the bracket, so the update contract does not change.
- Prefer titles that are not brands or series trademarks.

## Leaner, and every journey shorter

**Fewer controls.** At Just pacing:

| Place | Today | Just pacing |
|---|---|---|
| Group ⋮ (no plan of one's own) | Extra lesson, Copy, Share, Print, Tips (+ Catch up) | Tips (+ Catch up) |
| Group ⋮ (with a plan) | up to 10 | Tips (+ Catch up) |
| Lesson long-press, a future lesson | Add homework, Add supplementary, Done up to here, Not done from here | Done up to here, Not done from here |
| Group home pills | Plan, Work, Stats, Settings | Plan, Stats, Settings |
| Add book | name, type, supplementary switch, pages | name, type, pages |
| Home | board, Tests & worksheets card | board |

No new chrome at any preset: no badges, no locked or greyed items, no "upgrade" rows. Hidden means absent.

**Shorter journeys.** Each UI pull request counts taps before and after for the journeys it touches, and none may get longer. The journeys and their counts live in [`STREAMLINING_BRIEF.md`](STREAMLINING_BRIEF.md), which gains a preset column:

1. First run, install to first schedule: the preset costs one tap; at Just pacing the forms ask only what pacing needs.
2. Daily use: no step longer at any preset; a long-press action the preset keeps gets a visible route.
3. Weekly planning: a warning's fix ends on the slim bar, not in the plan editor.
4. Term set-up: core fields only; a module's fields appear where they are used.
5. Discovering a tool: hint to first use in two taps, in place.
6. Import or restore: one extra tap, only when a module is off.
7. Updating: the deck is skippable and returns the teacher where they were.

## Motion and micro-interactions

One focal moment: **turning a module on**. The sheet or hint closes and the new controls arrive where they will be used (they expand into the ⋮ menu or onto the hero), carry an accent wash that fades over about 800ms, like the revision diamond in [`PACER_DESIGN.md`](PACER_DESIGN.md), and a confirm haptic fires. TalkBack says what appeared and where.

Supporting states stay small:

- preset cards press with `pressScale`; the check draws in within 150–300ms;
- the deck's pages move with the pager; its indicator stretches; content enters in reading order, 50–100ms apart, capped;
- hints slide in under their control and leave faster than they came;
- the import question is a plain dialog, because it is a decision.

Durations come from `Motion` and `MotionSpec`; everything snaps when the system's animations are off (`ui/Motion.kt`).

## Design rules for the build

- impeccable (native Android, Operate mode): `shape` per screen, `onboard`'s principles (time to value, help in context, skippable, replayable), `animate` for the motion, then `critique` and `audit.native`.
- taste-skill v2 app screens: colour on data, not chrome; no icon in a circle; show the real product; the teacher's words; no generated phrasing (`references/copy.md` §6).
- House rules: tokens only, `PacerDropdownMenu`, `AppIcons.kt`, strings in `strings.xml`, `PacerSheet`, the one sheet frame (UI audit PR 5).

## Order of work

The remaining UI audit pull requests redesign the screens modules touch, so the two are woven together rather than built twice. The order is in the fix table of [`UI_AUDIT_BRIEF.md`](UI_AUDIT_BRIEF.md#fix-pull-requests-in-order); each step still needs the owner's yes.

1. Audit PR 5, one sheet frame (built, 0.4.10.0). Every new sheet uses its `PacerSheet`.
2. **Modules foundation**: the registry (`model/Features.kt`), the stored modules and their migration, `LocalFeatures`, gates in the pure menu models, the import question, the slim catch-up bar. Nothing changes for existing installs.
3. Audit PR 6, set-up screens, with their gates.
4. Audit PR 7, Stats and Work, with Work's gates.
5. Audit PR 8, Settings, welcome and empty states: presets, Settings › Features, hints, What's new and the first release name. This is the launch.
6. Audit PR 9, polish: dark and tablet passes over the module screens; `PACER_DESIGN.md` records the deck and the motion.

No engine change, no schema change, no new dependency. Each step takes the version its pull request earns; the launch is a B release and carries the first release name.

## Benefits and costs

**Benefits.**

- A calmer first week where the clutter actually is: the lesson menu, the group ⋮ menu, the book form, Group settings and Work.
- A lasting lean app for teachers who only pace.
- Safe by construction: unused extensions change nothing, and modules only grow.
- One feature registry that every future control reads, which a rebuild would otherwise be the reason for.
- Regrouping is a change to the brick table, not to every screen.
- The deck and the module map give Help its shape.

**Costs and risks.**

- The gain is smaller than it looks: units, the lead book, custom plans and supplementary books already hide until used.
- A hidden tool cannot be found by exploring; hints and the Features sheet must carry discovery.
- A teacher who turned on too much cannot tidy back down (the owner's choice, to keep it simple).
- Every future feature names its brick, and its NAVIGATION.md rows gain a "When" condition.
- The two baseplates (the plan base and the assignment record) limit how freely bricks can be regrouped.
- The brick registry is a little more work in the foundation than per-screen conditions.
- The modules are stored per phone; two phones can differ until the teacher turns the same ones on.
- The Compose-only gates have no automated UI tests until planned feature 10.

**Rebuild or evolve.** Evolve. The engine, model and data are pure Kotlin behind 536 tests that guard parity, schemas 1–15 and the web export; modules are a presentation concern that fits the existing seams. A rebuild would be right only for a platform move, a broken data format or an engine tangled with the UI.

## Fringe cases

| Case | Answer |
|---|---|
| A catch-up applied at Just pacing | Works; the slim bar shows it; Planning tools stay off |
| Units on in a class, Work off | Units work; the revision step stays hidden until Work is on |
| Homework "about a test" | Both are Work, so it never crosses |
| A backup with modules that are off | The import question; Cancel imports nothing |
| Android restore without stored modules | Treated as an existing install: everything on |
| Undo after turning a module on | Undo covers data, not settings, so the module stays on |
| Large text | Large text keeps hiding what it hides; modules hide independently; nothing hides twice |
| Tips, empty states and warning texts | Never name a control the preset hides; checked per pull request |
| Copy, share or print | Output; at Just pacing the group ⋮ keeps Tips only |
| TalkBack | Turning on announces what appeared; hidden controls have no actions |

## Open decisions

Settled with the owner for the launch: both hints ship, each once; the deck has one card per tool plus a greeting and a closing card, no exclamation mark, and no Skip or close (Continue on every card, Back to step back); releases are named from 0.4.14.0, shown without a last number of 0; at Just pacing the group ⋮ is absent and Tips live in Settings › How pacing works; the deck is remembered with the other show-once hints.

Still open: the deck's final wording and card count after a phone check, and whether the hints should sit directly under the lesson row instead of under the week.
