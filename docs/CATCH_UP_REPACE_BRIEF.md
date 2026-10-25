# Catch-up survives re-pacing

Status: shipped in 3.6.2. Owner chose preservation on 2026-10-22.

## Problem and behavior

Catch-up front-loads pages into untouched custom lessons. Re-pacing previously treated them like ordinary proposed-plan lessons and replaced the catch-up distribution. Because the resulting plan still covered all pages, the warning disappeared and the teacher had to switch to Proposed to get back to catch-up.

A custom plan now records the chosen extra pages per lesson as optional `catchUpBoost`. Re-pacing uses that boost while the group remains short, recalculating the catch-up duration from the remaining pages and teaching days. Earlier lessons and touched lessons stay fixed. Once the group is caught up, re-pacing uses ordinary allocation. Proposed pacing is unchanged.

My plan has an Adjust catch-up action whenever the group is short, independent of whether its warning is suppressed. The sheet starts with the saved boost and explains that re-pacing preserves it. Applying an unchanged choice does not create an Undo notification.

## Storage and old plans

Schema 9 adds `custom.catchUpBoost`; absent means ordinary allocation. Invalid saved boosts are ignored. The choice survives copying, ticking, manual lesson edits and supplementary-page saves through the existing immutable custom-plan copies.

Earlier builds saved only lesson pages, so old catch-up intent cannot be identified reliably. Use Adjust catch-up once on an existing plan to record its boost; no switch to Proposed is needed. No guess is made from its page distribution.

This bug fix consumes schema 9. The later roadmap phases must allocate their schema versions from 10 onward rather than reuse 9 for extra lessons. Extra lessons and group overrides remain outside this change.

## Validation

Regression tests cover repeated re-pacing, partial re-pacing, touched lessons, taught lessons, completed catch-up, inactive plans, copying, supplementary-page saves, sanitising saved choices and parity when the choice is absent. A standalone check with Android org.json also verifies real JSON round-trips, re-pacing after reopening, v8 files with and without custom plans, invalid boosts, and future-version refusal. No app or test dependency was added. The release build and lint are required; on-device checks of the sheet and persisted choice remain necessary.
