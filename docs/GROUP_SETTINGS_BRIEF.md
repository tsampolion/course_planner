# Group settings and group-only books

Status: implemented for 3.10.0 alongside class collections. Owner confirmed the shared optional-book design in this session on 2026-10-23.

## Intent

A dedicated Group settings screen collects Next lesson, an optional earlier end date, group days off, additional skipped pages, optional books and Delete group. Class defaults remain visible. Clearing an override restores the class/year default. Group end dates cannot extend the school year. Skip ranges add to class book skips. Every mutation follows existing undo/save helpers.

## Book approaches

1. **Shared class library (recommended).** Keep one book definition in `SchoolClass.books`. Existing books remain class defaults. Add `Book.groupOnly = false`; new optional books set it true. `Group.extraBooks` selects optional book ids for that group. This supports class books x/y and group-only z, without duplicating book definitions. Other groups may opt into z explicitly.
2. **Independent group book copies.** Store complete book definitions inside each group. Editing a shared optional book would require editing each copy, and homework/lead/output handling would need a second book catalogue. This departs from the handoff's id-based `extraBooks` contract.

## Engine and output flow

At the `GroupReport` boundary, derive the effective plan for that group: default books plus selected optional books, in catalogue order. All row arrays, `bookStats`, `coreBooks`, lead-following allocations and output columns use that same effective book list. Reconstructing a report must be idempotent. Optional books never leak into another group's page pool, verdict or exports. If a class lead is not available in a group, that report has no lead. Existing supplementary rules apply independently of optional membership.

Direct plan functions also honour group membership. Group skip ranges merge with class skips by book id. An effective group end is the earlier of its override and the school-year end; ordinary and extra lessons obey it. Print/TSV use each report's own book columns and indices. Code outside the report must look up book data by id rather than assume class and report indices match. Removing a book drops group skip/membership references; stale optional references are dropped on import.

Changing a calendar or membership override does not silently rewrite a custom plan. Existing missing/stale/overflow warnings and explicit re-pace/fit actions remain available. Touched content is preserved.

## Collections

Each class belongs to at most one collection. Selecting it elsewhere moves it; deleting a collection keeps its classes. Collections retain their own order, followed by uncollected classes. Sort within sections by Your order, Name or Most behind, preserving original order for equal measured values; unmeasured classes follow measured classes when both have zero groups behind. This final tie policy is an implementation choice, not an additional owner request. The optional Next lesson sort is outside the requested three-sort scope.

## Compatibility and delivery

One feature release, 3.10.0 (46), from merged PR48. One schema bump, 10 to 11, stores both group overrides and class collections. Version 10 backups retain current behavior with default values; builds predating this schema refuse new exports. Persist the class sort in Settings outside undo. No dependencies added.
