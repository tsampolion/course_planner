# Save status

Status: built in 0.4.4.5. No change to the data format or the engine.

## Why

`PlanStore.save` used to swallow a failed write, and the ViewModel never retried it. Messages such as "Assignment saved" described the in-memory edit, not the disk. After a read failure every edit was silently never written. A teacher could lose acknowledged work without being told.

## Decisions

- **The notice is a lasting snackbar**, not a mark in the top bar. It is not part of the toast queue, so an ordinary toast can't hide it, and it stays until a write succeeds.
- **A locked read offers "Try reading again"** in the app, as well as Export a copy.
- No dialog on ordinary edits. The notice appears only when something is wrong.

## Rules

- `PlanStore.save` returns `Saved`, `Failed` or `Locked`.
- The ViewModel counts edits (a revision). A write clears only the revision it wrote, so an older success never clears a newer edit.
- A failed write is retried on the next edit, when the app goes to the background and on Retry. `flush()` writes whenever something is unsaved, whatever the state of the debounced job.
- While reading is locked, nothing is written (the A4 rule). The notice says edits aren't being saved.
- Try reading again reads without unlocking. If the file reads and nothing was edited since, its data replaces the empty start and saving resumes. If edits were made meanwhile, the file stays locked and the notice says so: replacing it would overwrite either the file or the new edits, so the teacher exports a copy first and reopens the app.
- Export a copy writes the in-memory state through the same system picker as Settings.

