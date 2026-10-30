# Course Pacer docs

The root [`README.md`](../README.md) lists the features and [`CLAUDE.md`](../CLAUDE.md) holds the rules for contributors. This folder holds the architecture, reference documents, decision records and ideas not yet approved. Where one fact lives is set out in the docs rules in `CLAUDE.md`.

Briefs give versions in the old three-part form up to 4.1.0 (4.1.0 is `0.4.1.0` in the four-part format; see `CHANGELOG.md`).

## Reference

- [ARCHITECTURE.md](ARCHITECTURE.md): how the app works today: model, engine and its extensions, storage and schema versions, update check, UI, theme.
- [NAVIGATION.md](NAVIGATION.md): every control on every screen, sheet, menu and dialog: its gesture, when it shows and what it opens or changes.
- [BRIEF.md](BRIEF.md): the original port brief and the feature-parity checklist.
- [EDUSUITE_STYLE_GUIDE.md](EDUSUITE_STYLE_GUIDE.md): the shared EduSuite look. A copy of the canonical guide.
- [PACER_DESIGN.md](PACER_DESIGN.md): Course Pacer's own design language and its departures from the shared guide.
- [PACER_ACCENT_CONTRAST.md](PACER_ACCENT_CONTRAST.md): measured contrast pairs for all four Bone/Saffron sets.
- [STRING_INVENTORY.md](STRING_INVENTORY.md): every string the app shows, says or prints, grouped by screen, with a blank column for the owner's proposed wording.
- [../PRODUCT.md](../PRODUCT.md): the product record (users, purpose, constraints, principles).

- [SETUP_CHECKS_BRIEF.md](SETUP_CHECKS_BRIEF.md): strict book ranges and day-off dates.

## Decision records

Each one records why a feature was built, and starts with a `Status:` line.

- [YEARS_BRIEF.md](YEARS_BRIEF.md): school years, classes and groups.
- [LESSON_PAGES_BRIEF.md](LESSON_PAGES_BRIEF.md): recording the pages a lesson actually covered.
- [SIGNING_BRIEF.md](SIGNING_BRIEF.md): the permanent release signing key.
- [BACKUP_BRIEF.md](BACKUP_BRIEF.md): export and import of school years.
- [CUSTOM_PLANS_BRIEF.md](CUSTOM_PLANS_BRIEF.md): custom plans, a lead book and pace warnings.
- [CATCH_UP_REPACE_BRIEF.md](CATCH_UP_REPACE_BRIEF.md): preserving the chosen catch-up pace when re-pacing.
- [SUPPLEMENTARY_BOOKS_BRIEF.md](SUPPLEMENTARY_BOOKS_BRIEF.md): supplementary books placed by hand in chosen lessons.
- [EXTRA_LESSONS_BRIEF.md](EXTRA_LESSONS_BRIEF.md): date-pinned core-book extra lessons.
- [GROUP_SETTINGS_BRIEF.md](GROUP_SETTINGS_BRIEF.md): group overrides, optional books, collections and sorting.
- [ASSIGNMENTS_BRIEF.md](ASSIGNMENTS_BRIEF.md): group-owned Homework, Tests and Worksheets.
- [HOMEWORK_BRIEF.md](HOMEWORK_BRIEF.md): lesson homework actions, pairing and date-based banking.
- [UNITS_BRIEF.md](UNITS_BRIEF.md): optional linked book units, group overrides and revisions.
- [UPDATES_BRIEF.md](UPDATES_BRIEF.md): the in-app update check against GitHub releases and its release contract.
- [SAVE_STATUS_BRIEF.md](SAVE_STATUS_BRIEF.md): the save result, the lasting "not saved" notice, Retry, Export a copy and Try reading again.
- [TIDY_FIT_BRIEF.md](TIDY_FIT_BRIEF.md): Tidy and Fit to term leave the teacher's hand-edited lessons and removed-book pages alone.
- [AUTO_TICK_BRIEF.md](AUTO_TICK_BRIEF.md): lessons count as taught when their day has passed, "Lesson didn't happen" and the first-launch question.
- [ONE_TODAY_BRIEF.md](ONE_TODAY_BRIEF.md): every plan the app builds reads the view model's one "today", so banking and revision dates follow midnight with the screens.
- [REMOVED_BOOK_HISTORY_BRIEF.md](REMOVED_BOOK_HISTORY_BRIEF.md): done lessons still show pages taught from a book the group dropped, and the schema-15 codec tests.
- [PRINT_BRIEF.md](PRINT_BRIEF.md): the printed sheet wraps and turns landscape for many books, and Print offers only scopes with lessons.
- [UI_AUDIT_BRIEF.md](UI_AUDIT_BRIEF.md): the UI/UX audit: findings per screen, proposed design-rule changes and the order of the fix pull requests.
- [STREAMLINING_BRIEF.md](STREAMLINING_BRIEF.md): each screen's start, purpose, next step and end, and what to remove, merge or move.
- [FEATURE_MODULES_BRIEF.md](FEATURE_MODULES_BRIEF.md): start with pacing alone and turn on Work, Book tools, Planning tools and Output when needed; woven into the UI audit's order of work.

## Not yet approved

- [PLANNED_FEATURES.md](PLANNED_FEATURES.md): specified ideas that wait for the owner's go-ahead.
