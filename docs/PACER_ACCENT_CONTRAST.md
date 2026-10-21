# Course Pacer contrast

`docs/EDUSUITE_STYLE_GUIDE.md` §2.5 lists the shared contrast pairs and says Course Pacer's are listed in its own copy.
The guide's shared sections must stay byte-identical in both repositories, so Course Pacer's pairs live here instead:
its accent (§2.4), its own dark set (`docs/PACER_DESIGN.md` D1) and its lesson-kind edges (D8). Since 3.1.0
`ContrastTest` checks every ratio below; the shared light neutrals, semantic colours and the tiles stay checked against
the guide.

Ratios are WCAG 2.x, computed from `ui/theme/Tokens.kt`. Light uses the guide's neutrals (paper `#F6F5FA`, surface
`#FFFFFF`); dark uses Course Pacer's (paper `#0E0E12`, surface `#1A1A21`). The token values themselves are in
`docs/PACER_DESIGN.md`.

## Accent

| Pair | Light | Dark |
|------|-------|------|
| accent on paper | 6.80 | 10.33 |
| accent on surface | 7.38 | 9.28 |
| accent on accentSoft | 5.89 | 6.45 |
| on accent on accent | 7.38 | 8.23 |
| on accentSoft on accentSoft | 11.54 | 10.40 |
| Icon paper `#F6F5FA` on the launcher tile `#245E60` | 6.80 | (same tile) |

## Dark set

| Pair | Dark |
|------|------|
| ink on paper | 17.45 |
| ink on surface | 15.67 |
| ink on surfaceContainer | 13.72 |
| paper on ink (selected) | 17.45 |
| muted on paper | 9.68 |
| muted on surface | 8.69 |
| muted on surfaceContainer | 7.61 |
| outline on paper | 5.87 |
| outline on surface | 5.27 |
| warn on paper | 8.62 |
| warn on surface | 7.74 |
| ok on paper | 12.56 |
| warn on warnSoft | 6.71 |
| ok on okSoft | 8.73 |
| on warn on warn | 7.40 |
| on ok on ok | 9.48 |
| on warnSoft on warnSoft (the pale status pill) | 10.89 |
| on okSoft on okSoft | 10.83 |

The dark tiles, okSoft and onOkSoft keep the guide's values.

## Lesson-kind edges, on surface

| Edge | Light | Dark |
|------|-------|------|
| edgeTest | 6.06 | 9.71 |
| edgeDayOff | 4.41 | 5.27 |
| edgeEdited | 7.38 | 9.28 |
| edgeHand | 5.67 | 10.36 |
| edgeFree | 5.22 | 10.84 |
| edgeSupp | 5.55 | 8.93 |

Every text pair meets 4.5:1 and every edge 3:1. If a token, the neutrals or the guide's contrast rules change, compute
these again and update this file and `ContrastTest`.
