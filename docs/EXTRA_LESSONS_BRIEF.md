# Extra lessons

Status: shipped in 3.7.0. Approved on 2026-10-22.

An extra lesson is a date and pinned core-book pages, shared by Proposed and My plan. Tap a day-off row, choose a date in Term weeks, or use the group’s ⋮ → Add extra lesson. The editor starts with the proposed share for that date, accepts strict page ranges and shows the verdict after saving. Other lessons re-pace when saved; typing changes only the preview. Existing extras have Edit and Delete actions in the lesson menu and TalkBack. The row and today’s box say Extra; Print and TSV include it too.

`Group.extras` stores `ExtraLesson(id, date, pages)` in JSON schema 10 (schema 9 already stores the chosen catch-up boost). Older files without extras read unchanged. Import refuses versions above 10. Sanitising keeps one pin per date and unique ids, drops unknown and supplementary books, and removes pages outside 0..MAX_PAGE. Removing a book removes its pages from pins.

Only extras within the group’s remaining date range, not already taught and not blocked by a whole-lesson test, reserve pages. They bypass weekdays and days off. A pin on an ordinary lesson date replaces that ordinary slot; it never adds a second row on the same day. A blocked or out-of-range pin stays saved but returns its pages to the regular pool and produces a warning. A group with no selected weekdays can still teach extras.

Pinned pages leave `remainingPages`. `teachingDates` contains ordinary, unblocked slots only, so the proposed rounding, finish-by dates, lead-book allocation, catch-up and custom-plan slot indices retain their rules. Extra rows merge in date order with `Row.extraId`. The verdict measures the remaining ordinary work against the ordinary slots; when none remain there is no verdict to measure.

Add, save and delete each run through one untagged `editGroup`, including re-pacing an active custom plan from zero. Touched lessons stay intact and stale pages stay flagged. A saved catch-up boost is preserved; its remaining duration recalculates as extra pages leave or return to the pool. Removing a pin that occupied an ordinary slot restores capacity for an untouched custom lesson. An inactive custom plan stays unchanged and may need re-pacing when activated.

Ticking an extra records its pinned pages through the usual done-lesson path and keeps its pin. It never consumes a custom slot. Unticking restores the extra without inserting a custom lesson. Editing taught pages uses Record what I taught; the editor asks the teacher to untick before changing a taught pin. Deleting a pin keeps its taught history.

Supplementary books are excluded in this first version. No new dependencies.

Verification covers engine parity, days off, non-teaching weekdays, empty weekdays, consuming tests, date bounds, done/undone, custom slots, proposal pre-fill, re-pacing and removal, sanitising, menus, warnings, Home, Print and TSV. Android codec tests cover round-trip, v8 fallback, future-version refusal and malformed records. Phone, large-text and tablet checks and Android codec execution require a device.

Run the host suite and build checks with `./gradlew testDebugUnitTest lint assembleRelease`. The device-only codec checks use a platform `Instrumentation` runner with standard test-status reports and no extra libraries. Compile/package them with `./gradlew assembleDebugAndroidTest`; with a connected device, run `./gradlew connectedDebugAndroidTest`.
