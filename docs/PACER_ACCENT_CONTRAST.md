# Course Pacer accent contrast

`docs/EDUSUITE_STYLE_GUIDE.md` §2.5 lists Course Invoicer's accent pairs and says Course Pacer's are
listed in its own copy. The guide's shared sections must stay byte-identical in both repositories, so
Course Pacer's pairs live here instead.

Course Pacer's accent (§2.4): `#245E60` teal (light), `#6FB3AE` (dark); on accent `#FFFFFF` / `#0F2E2F`;
accentSoft `#D5EAE8` / `#163C3C`; on accentSoft `#0F2E2F` / `#D5EAE8`.

Ratios are WCAG 2.x, against the v3 neutrals (paper `#F6F5FA` / `#121118`, surface `#FFFFFF` / `#1C1B22`).

| Pair | Light | Dark |
|------|-------|------|
| Pacer accent on paper | 6.80 | 7.81 |
| Pacer accent on surface | 7.38 | 7.11 |
| Pacer accent on accentSoft | 5.89 | 5.00 |
| on accent on Pacer accent | 7.38 | 6.02 |
| on accentSoft on Pacer accentSoft | 11.54 | 9.60 |
| Icon paper `#F6F5FA` on Pacer tile `#245E60` | 6.80 | (same tile) |

Every pair meets 4.5:1. If the accent, the neutrals or the guide's contrast rules change, compute
these again and update this file.
