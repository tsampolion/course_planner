# Setup checks

Status: implemented in UI PR6, 0.4.12.0.

Book forms accept only `1 ≤ first ≤ last ≤ MAX_PAGE`. Invalid drafts stay open with inline reasons. Supplementary lists reject zero, reversed and out-of-range spans. Optional group books use the same strict range rule. ViewModel mutations independently reject invalid values.

Year and group days off require an end date on or after the start. Existing imported ranges are retained. The separate Home on-pace count change remains unapproved in PLANNED_FEATURES.md.
