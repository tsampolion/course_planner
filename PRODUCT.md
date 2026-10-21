# Product

<!-- impeccable:product-schema 1 -->

## Platform

android

## Users

Teachers at Greek private language schools (frontistiria): the owner and their colleagues. Each teacher runs a
school year of about 8 or more classes by level (A Junior … Proficiency, exam classes such as FCE), split into about
15 or more groups that meet 2–3 times a week. Colleagues use the app without the owner explaining it, so every screen
has to make sense on its own.

They use it in two moments, equally often:

- **In class, at a glance.** The phone is in one hand between activities: which pages today, and ticking the lesson
  done.
- **At the desk, planning.** Before term and week by week: building plans, checking which groups and books are behind,
  and fixing them.

They start from three directions: a day (today, this week), a group, or a class.

## Product Purpose

The app paces coursebook pages over the lessons left in a term, so that every book lands on the last lesson. For each
group it counts the lessons left, takes out skipped pages and pages already set as homework, and spreads the rest.
Success means a teacher always knows what to teach next, sees early when a group or a book falls behind, and can fix it
in a few taps.

## Positioning

A pacing engine with exact, published rules: lesson *i* ends at `round(i × pages / lessons)`, row for row the same as
the original Coursebook Pacer web page. The proposed plan is a template. The teacher's own custom plan, the lead book,
catch-up and supplementary books build on it without breaking that parity.

## Operating Context

- Phones (portrait, one-handed) and tablets, both first-class.
- Greek school calendar: term dates, Greek public holidays from Orthodox Easter, snow days and other days off per group
  or for everyone.
- Printed or PDF schedules, and tab-separated text copied or shared to other apps.
- Data stays on the device. Export and import move school years between phones.
- Part of the EduSuite apps with Course Invoicer. Course Pacer leads the next suite design language, which Invoicer may
  adopt later.

## Capabilities and Constraints

- Capabilities: school years, classes, books (lead and supplementary), groups, days off, tests, homework, the proposed
  plan, custom plans, done lessons and pages taught, warnings with catch-up, Overview, print/PDF, undo (60 steps),
  export and import.
- Engine parity and its four approved extensions are a contract. A change to the engine needs the owner's approval and
  a test.
- Kotlin + Jetpack Compose with Material 3 only. Approved dependencies only. No icon library: Material Symbols are kept
  as path data. No XML layouts.
- Every visible string lives in `strings.xml`. Colours, type, shape, spacing and motion are tokens.
- `applicationId` (`gr.edusuite.pacer`), the release signing key and `minSdk 26` never change.
- UI language: English only. Class and book names may be in Greek.
- Tests and Homework will get their own overhaul later. Until then, the redesign keeps their structure.

## Brand Commitments

- The name is Course Pacer, part of EduSuite. The accent is teal. The typeface is the bundled Outfit. Wallpaper
  (dynamic) colour stays off, so the accent identifies the app.
- Voice: plain, short and warm. Sentence case. No exclamation marks, no blame.
- The launcher icon is an open book with three pacing lines.

## Evidence on Hand

- No real user data, testimonials or usage figures. Mock-ups use invented sample classes and must say so.
- The existing app and its strings (`app/src/main/res/values/strings.xml`) are the source of copy.

## Product Principles

1. Today's lesson is never more than one tap from opening the app.
2. The proposed plan stays trustworthy. Anything the teacher changes is visible as theirs.
3. Warn early, with a way out. Every warning offers a fix, and nothing changes by itself.
4. One fact, one place. No figure is counted twice on a screen.
5. Works for a colleague on day one: features are found from Home, not learned by hearsay.

## Accessibility & Inclusion

Text contrast of at least 4.5:1, meaningful UI parts at least 3:1, and touch targets of at least 48dp. TalkBack reaches
every action, including the lesson actions. Layouts survive 200% font size and landscape. When the system's animations
are off, motion snaps to its end state.
