# Print that fits the page

Status: built in 0.4.6.1. Pacing is unchanged; only the printed sheet and what Print offers changed.

## Why

The sheet put every cell on one line and gave each book a column, so many books or a long note ran off an A4 portrait page. Print also let a teacher pick a scope with nothing to print (a finished group, a year with nothing left) and opened the system print dialog on a blank page.

## Decisions

- **Nothing to print is not offered.** The dialog lists only the scopes that have lessons. If none has, the group menu hides Print. The owner chose hiding over a disabled entry or a "No lessons left" page.
- **One check.** `PrintScopes` (`ui/PrintScopes.kt`, plain Kotlin) says a group is printable when its `GroupReport` has rows, which is what `PrintSheet` renders. A schedule with only tests has rows, so it stays printable. The Group, Class and Year scopes, and the later Print entry points on Class and School year (streamlining M3), all go through it.
- **Wrapping and widths.** Notes and event labels wrap. `table-layout:fixed` with a `colgroup` reserves the number, date and note columns; the books share the rest.
- **Many books.** More than four book columns in any printed group (`PrintSheet.MAX_PORTRAIT_BOOKS`) turns the whole job landscape (`@page{size:landscape}` and A4 landscape in the print attributes). The teacher can still change it in the system dialog.

## Phone checks

Print preview (Save as PDF) for 1, 4 and 8 books at the default margins; a finished group's menu still prints its class; a year with nothing left has no Print; a cancelled print does nothing.
