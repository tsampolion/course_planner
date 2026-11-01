# Course Pacer palette contrast

Pacer owns three palettes (D13, D14 in `PACER_DESIGN.md`), each light and dark: Freetown (stored as `BONE`), Arusha (`SAFFRON`) and Oxford (`OXFORD`). The shared suite guide remains unchanged and still shows the previous palette.

Ratios are WCAG 2.x, rounded to two decimals, computed from `ui/theme/Tokens.kt`. Text pairs reach 4.5:1; outlines, lesson-kind edges, progress marks and the today mark reach 3:1. `ContrastTest` checks every pair below across all six sets. Rows marked *tint* are a lesson kind's row ground: its edge over surface at 12% (`kindTint`).

Arusha light's accent (saffron) is a **fill only**: its contrast on paper is below 3:1. Links, icons, field focus and other foreground controls use `accentText` (`onAccentSoft`). Snackbar actions use the other mode's `accentText`.


## Freetown light

| Foreground | Background | Ratio |
|---|---|---|
| ink | paper | 13.75 |
| ink | surface | 15.48 |
| ink | surfaceContainer | 12.62 |
| muted | paper | 5.77 |
| muted | surface | 6.50 |
| muted | surfaceContainer | 5.30 |
| warn | paper | 6.19 |
| warn | surface | 6.98 |
| warn | surfaceContainer | 5.69 |
| ok | paper | 6.55 |
| ok | surface | 7.38 |
| ok | surfaceContainer | 6.01 |
| accentText | paper | 10.37 |
| accentText | surface | 11.68 |
| accentText | surfaceContainer | 9.52 |
| second | paper | 5.78 |
| second | surface | 6.51 |
| second | surfaceContainer | 5.30 |
| onSecondSoft | paper | 10.23 |
| onSecondSoft | surface | 11.52 |
| onSecondSoft | surfaceContainer | 9.39 |
| accent | paper | 6.55 |
| accent | surface | 7.38 |
| accent | surfaceContainer | 6.01 |
| onAccent | accent | 7.38 |
| onAccentSoft | accentSoft | 9.27 |
| ink | accentSoft | 12.29 |
| onSecond | second | 6.51 |
| onSecondSoft | secondSoft | 9.66 |
| second | secondSoft | 5.46 |
| ink | secondSoft | 12.98 |
| onWarn | warn | 6.98 |
| onWarnSoft | warnSoft | 10.31 |
| warn | warnSoft | 5.68 |
| onOk | ok | 7.38 |
| onOkSoft | okSoft | 9.27 |
| ok | okSoft | 5.85 |
| onTile | tile1 | 12.18 |
| onTileMuted | tile1 | 6.10 |
| muted | tile1 | 5.12 |
| second (today dot) | tile1 | 5.12 |
| onTile | tile2 | 12.14 |
| onTileMuted | tile2 | 6.08 |
| muted | tile2 | 5.10 |
| second (today dot) | tile2 | 5.10 |
| onTile | tile3 | 12.23 |
| onTileMuted | tile3 | 6.13 |
| muted | tile3 | 5.14 |
| second (today dot) | tile3 | 5.14 |
| onTile | tile4 | 11.72 |
| onTileMuted | tile4 | 5.87 |
| muted | tile4 | 4.92 |
| second (today dot) | tile4 | 4.93 |
| onTile | tile5 | 12.11 |
| onTileMuted | tile5 | 6.07 |
| muted | tile5 | 5.08 |
| second (today dot) | tile5 | 5.09 |
| outline | paper | 3.81 |
| outline | surface | 4.29 |
| mark | paper | 5.78 |
| mark | surface | 6.51 |
| mark | surfaceContainer | 5.30 |
| mark | secondSoft | 5.46 |
| edgeTest | surface | 5.88 |
| ink | edgeTest tint | 12.91 |
| muted | edgeTest tint | 5.42 |
| edgeWorksheet | surface | 4.01 |
| ink | edgeWorksheet tint | 13.42 |
| muted | edgeWorksheet tint | 5.63 |
| edgeHand | surface | 5.40 |
| ink | edgeHand tint | 13.10 |
| muted | edgeHand tint | 5.50 |
| edgeEdited | surface | 7.38 |
| ink | edgeEdited tint | 12.86 |
| muted | edgeEdited tint | 5.40 |
| edgeFree | surface | 4.75 |
| ink | edgeFree tint | 13.30 |
| muted | edgeFree tint | 5.58 |
| edgeSupp | surface | 7.46 |
| ink | edgeSupp tint | 12.92 |
| muted | edgeSupp tint | 5.42 |
| edgeDayOff | surface | 4.29 |
| ink | edgeDayOff tint | 13.51 |
| muted | edgeDayOff tint | 5.67 |
| oppositeMode.accentText (snackbar action) | ink | 12.51 |

## Freetown dark

| Foreground | Background | Ratio |
|---|---|---|
| ink | paper | 14.32 |
| ink | surface | 12.85 |
| ink | surfaceContainer | 11.07 |
| muted | paper | 7.72 |
| muted | surface | 6.92 |
| muted | surfaceContainer | 5.97 |
| warn | paper | 7.44 |
| warn | surface | 6.67 |
| warn | surfaceContainer | 5.75 |
| ok | paper | 8.67 |
| ok | surface | 7.77 |
| ok | surfaceContainer | 6.70 |
| accentText | paper | 13.31 |
| accentText | surface | 11.94 |
| accentText | surfaceContainer | 10.29 |
| second | paper | 8.29 |
| second | surface | 7.43 |
| second | surfaceContainer | 6.40 |
| onSecondSoft | paper | 13.37 |
| onSecondSoft | surface | 11.99 |
| onSecondSoft | surfaceContainer | 10.33 |
| accent | paper | 8.67 |
| accent | surface | 7.77 |
| accent | surfaceContainer | 6.70 |
| onAccent | accent | 8.49 |
| onAccentSoft | accentSoft | 9.42 |
| ink | accentSoft | 10.14 |
| onSecond | second | 8.02 |
| onSecondSoft | secondSoft | 10.36 |
| second | secondSoft | 6.42 |
| ink | secondSoft | 11.11 |
| onWarn | warn | 7.72 |
| onWarnSoft | warnSoft | 9.93 |
| warn | warnSoft | 6.00 |
| onOk | ok | 8.49 |
| onOkSoft | okSoft | 9.42 |
| ok | okSoft | 6.13 |
| onTile | tile1 | 9.31 |
| onTileMuted | tile1 | 6.88 |
| muted | tile1 | 5.02 |
| second (today dot) | tile1 | 5.38 |
| onTile | tile2 | 10.44 |
| onTileMuted | tile2 | 7.71 |
| muted | tile2 | 5.63 |
| second (today dot) | tile2 | 6.04 |
| onTile | tile3 | 9.19 |
| onTileMuted | tile3 | 6.79 |
| muted | tile3 | 4.95 |
| second (today dot) | tile3 | 5.32 |
| onTile | tile4 | 10.58 |
| onTileMuted | tile4 | 7.81 |
| muted | tile4 | 5.70 |
| second (today dot) | tile4 | 6.12 |
| onTile | tile5 | 9.39 |
| onTileMuted | tile5 | 6.93 |
| muted | tile5 | 5.06 |
| second (today dot) | tile5 | 5.43 |
| outline | paper | 5.49 |
| outline | surface | 4.93 |
| mark | paper | 8.29 |
| mark | surface | 7.43 |
| mark | surfaceContainer | 6.40 |
| mark | secondSoft | 6.42 |
| edgeTest | surface | 5.82 |
| ink | edgeTest tint | 10.64 |
| muted | edgeTest tint | 5.73 |
| edgeWorksheet | surface | 5.79 |
| ink | edgeWorksheet tint | 10.71 |
| muted | edgeWorksheet tint | 5.77 |
| edgeHand | surface | 6.83 |
| ink | edgeHand tint | 10.32 |
| muted | edgeHand tint | 5.56 |
| edgeEdited | surface | 7.77 |
| ink | edgeEdited tint | 10.11 |
| muted | edgeEdited tint | 5.45 |
| edgeFree | surface | 8.43 |
| ink | edgeFree tint | 10.02 |
| muted | edgeFree tint | 5.40 |
| edgeSupp | surface | 6.52 |
| ink | edgeSupp tint | 10.43 |
| muted | edgeSupp tint | 5.62 |
| edgeDayOff | surface | 4.60 |
| ink | edgeDayOff tint | 10.92 |
| muted | edgeDayOff tint | 5.89 |
| oppositeMode.accentText (snackbar action) | ink | 10.15 |

## Arusha light

| Foreground | Background | Ratio |
|---|---|---|
| ink | paper | 16.00 |
| ink | surface | 18.11 |
| ink | surfaceContainer | 14.46 |
| muted | paper | 6.51 |
| muted | surface | 7.37 |
| muted | surfaceContainer | 5.89 |
| warn | paper | 5.94 |
| warn | surface | 6.72 |
| warn | surfaceContainer | 5.37 |
| ok | paper | 5.74 |
| ok | surface | 6.50 |
| ok | surfaceContainer | 5.19 |
| accentText | paper | 13.02 |
| accentText | surface | 14.74 |
| accentText | surfaceContainer | 11.77 |
| second | paper | 5.67 |
| second | surface | 6.41 |
| second | surfaceContainer | 5.12 |
| onSecondSoft | paper | 10.34 |
| onSecondSoft | surface | 11.70 |
| onSecondSoft | surfaceContainer | 9.35 |
| onAccent | accent | 10.12 |
| onAccentSoft | accentSoft | 12.28 |
| ink | accentSoft | 15.09 |
| onSecond | second | 6.41 |
| onSecondSoft | secondSoft | 9.23 |
| second | secondSoft | 5.06 |
| ink | secondSoft | 14.28 |
| onWarn | warn | 6.72 |
| onWarnSoft | warnSoft | 10.59 |
| warn | warnSoft | 5.29 |
| onOk | ok | 6.50 |
| onOkSoft | okSoft | 9.98 |
| ok | okSoft | 5.26 |
| onTile | tile1 | 15.01 |
| onTileMuted | tile1 | 7.38 |
| muted | tile1 | 6.11 |
| second (today dot) | tile1 | 5.31 |
| onTile | tile2 | 14.28 |
| onTileMuted | tile2 | 7.02 |
| muted | tile2 | 5.81 |
| second (today dot) | tile2 | 5.06 |
| onTile | tile3 | 13.82 |
| onTileMuted | tile3 | 6.79 |
| muted | tile3 | 5.63 |
| second (today dot) | tile3 | 4.89 |
| onTile | tile4 | 13.99 |
| onTileMuted | tile4 | 6.88 |
| muted | tile4 | 5.69 |
| second (today dot) | tile4 | 4.95 |
| onTile | tile5 | 14.19 |
| onTileMuted | tile5 | 6.97 |
| muted | tile5 | 5.78 |
| second (today dot) | tile5 | 5.02 |
| outline | paper | 3.92 |
| outline | surface | 4.43 |
| mark | paper | 5.67 |
| mark | surface | 6.41 |
| mark | surfaceContainer | 5.12 |
| mark | secondSoft | 5.06 |
| edgeTest | surface | 6.27 |
| ink | edgeTest tint | 15.12 |
| muted | edgeTest tint | 6.16 |
| edgeWorksheet | surface | 4.28 |
| ink | edgeWorksheet tint | 15.61 |
| muted | edgeWorksheet tint | 6.35 |
| edgeHand | surface | 6.23 |
| ink | edgeHand tint | 15.26 |
| muted | edgeHand tint | 6.21 |
| edgeEdited | surface | 6.25 |
| ink | edgeEdited tint | 15.20 |
| muted | edgeEdited tint | 6.19 |
| edgeFree | surface | 11.42 |
| ink | edgeFree tint | 14.64 |
| muted | edgeFree tint | 5.96 |
| edgeSupp | surface | 5.24 |
| ink | edgeSupp tint | 15.44 |
| muted | edgeSupp tint | 6.29 |
| edgeDayOff | surface | 4.43 |
| ink | edgeDayOff tint | 15.74 |
| muted | edgeDayOff tint | 6.41 |
| oppositeMode.accentText (snackbar action) | ink | 14.19 |

Fill-only accent on paper: **1.58:1** (excluded from the text pairs).

## Arusha dark

| Foreground | Background | Ratio |
|---|---|---|
| ink | paper | 15.42 |
| ink | surface | 14.06 |
| ink | surfaceContainer | 12.26 |
| muted | paper | 7.67 |
| muted | surface | 6.99 |
| muted | surfaceContainer | 6.09 |
| warn | paper | 7.21 |
| warn | surface | 6.58 |
| warn | surfaceContainer | 5.73 |
| ok | paper | 9.20 |
| ok | surface | 8.39 |
| ok | surfaceContainer | 7.32 |
| accentText | paper | 14.19 |
| accentText | surface | 12.94 |
| accentText | surfaceContainer | 11.28 |
| second | paper | 8.75 |
| second | surface | 7.98 |
| second | surfaceContainer | 6.96 |
| onSecondSoft | paper | 14.51 |
| onSecondSoft | surface | 13.23 |
| onSecondSoft | surfaceContainer | 11.53 |
| accent | paper | 11.20 |
| accent | surface | 10.21 |
| accent | surfaceContainer | 8.90 |
| onAccent | accent | 11.20 |
| onAccentSoft | accentSoft | 9.67 |
| ink | accentSoft | 10.51 |
| onSecond | second | 7.72 |
| onSecondSoft | secondSoft | 9.94 |
| second | secondSoft | 6.00 |
| ink | secondSoft | 10.56 |
| onWarn | warn | 7.22 |
| onWarnSoft | warnSoft | 9.96 |
| warn | warnSoft | 5.60 |
| onOk | ok | 8.62 |
| onOkSoft | okSoft | 9.70 |
| ok | okSoft | 6.40 |
| onTile | tile1 | 10.51 |
| onTileMuted | tile1 | 7.42 |
| muted | tile1 | 5.23 |
| second (today dot) | tile1 | 5.97 |
| onTile | tile2 | 10.56 |
| onTileMuted | tile2 | 7.46 |
| muted | tile2 | 5.25 |
| second (today dot) | tile2 | 6.00 |
| onTile | tile3 | 10.66 |
| onTileMuted | tile3 | 7.53 |
| muted | tile3 | 5.30 |
| second (today dot) | tile3 | 6.05 |
| onTile | tile4 | 12.03 |
| onTileMuted | tile4 | 8.49 |
| muted | tile4 | 5.98 |
| second (today dot) | tile4 | 6.83 |
| onTile | tile5 | 11.07 |
| onTileMuted | tile5 | 7.82 |
| muted | tile5 | 5.51 |
| second (today dot) | tile5 | 6.29 |
| outline | paper | 5.43 |
| outline | surface | 4.95 |
| mark | paper | 8.75 |
| mark | surface | 7.98 |
| mark | surfaceContainer | 6.96 |
| mark | secondSoft | 6.00 |
| edgeTest | surface | 6.09 |
| ink | edgeTest tint | 11.71 |
| muted | edgeTest tint | 5.82 |
| edgeWorksheet | surface | 6.06 |
| ink | edgeWorksheet tint | 11.65 |
| muted | edgeWorksheet tint | 5.79 |
| edgeHand | surface | 6.98 |
| ink | edgeHand tint | 11.44 |
| muted | edgeHand tint | 5.69 |
| edgeEdited | surface | 9.28 |
| ink | edgeEdited tint | 10.93 |
| muted | edgeEdited tint | 5.43 |
| edgeFree | surface | 9.93 |
| ink | edgeFree tint | 10.78 |
| muted | edgeFree tint | 5.36 |
| edgeSupp | surface | 8.83 |
| ink | edgeSupp tint | 10.92 |
| muted | edgeSupp tint | 5.43 |
| edgeDayOff | surface | 4.95 |
| ink | edgeDayOff tint | 11.91 |
| muted | edgeDayOff tint | 5.92 |
| oppositeMode.accentText (snackbar action) | ink | 12.55 |

## Oxford light

| Foreground | Background | Ratio |
|---|---|---|
| ink | paper | 15.92 |
| ink | surface | 17.36 |
| ink | surfaceContainer | 14.13 |
| muted | paper | 6.49 |
| muted | surface | 7.07 |
| muted | surfaceContainer | 5.76 |
| warn | paper | 5.65 |
| warn | surface | 6.16 |
| warn | surfaceContainer | 5.01 |
| ok | paper | 5.91 |
| ok | surface | 6.45 |
| ok | surfaceContainer | 5.25 |
| accentText | paper | 12.39 |
| accentText | surface | 13.50 |
| accentText | surfaceContainer | 11.00 |
| second | paper | 5.40 |
| second | surface | 5.88 |
| second | surfaceContainer | 4.79 |
| onSecondSoft | paper | 10.20 |
| onSecondSoft | surface | 11.12 |
| onSecondSoft | surfaceContainer | 9.06 |
| accent | paper | 13.54 |
| accent | surface | 14.76 |
| accent | surfaceContainer | 12.02 |
| onAccent | accent | 14.76 |
| onAccentSoft | accentSoft | 10.50 |
| ink | accentSoft | 13.50 |
| onSecond | second | 5.88 |
| onSecondSoft | secondSoft | 8.66 |
| second | secondSoft | 4.58 |
| ink | secondSoft | 13.51 |
| onWarn | warn | 6.16 |
| onWarnSoft | warnSoft | 10.22 |
| warn | warnSoft | 5.04 |
| onOk | ok | 6.45 |
| onOkSoft | okSoft | 9.82 |
| ok | okSoft | 5.19 |
| onTile | tile1 | 13.50 |
| onTileMuted | tile1 | 6.29 |
| muted | tile1 | 5.50 |
| second (today dot) | tile1 | 4.58 |
| onTile | tile2 | 13.51 |
| onTileMuted | tile2 | 6.30 |
| muted | tile2 | 5.50 |
| second (today dot) | tile2 | 4.58 |
| onTile | tile3 | 13.23 |
| onTileMuted | tile3 | 6.16 |
| muted | tile3 | 5.39 |
| second (today dot) | tile3 | 4.48 |
| onTile | tile4 | 13.85 |
| onTileMuted | tile4 | 6.46 |
| muted | tile4 | 5.64 |
| second (today dot) | tile4 | 4.70 |
| onTile | tile5 | 13.74 |
| onTileMuted | tile5 | 6.40 |
| muted | tile5 | 5.60 |
| second (today dot) | tile5 | 4.66 |
| outline | paper | 4.14 |
| outline | surface | 4.51 |
| mark | paper | 13.54 |
| mark | surface | 14.76 |
| mark | surfaceContainer | 12.02 |
| mark | accentSoft | 11.48 |
| edgeTest | surface | 5.97 |
| ink | edgeTest tint | 14.64 |
| muted | edgeTest tint | 5.96 |
| edgeWorksheet | surface | 4.28 |
| ink | edgeWorksheet tint | 14.96 |
| muted | edgeWorksheet tint | 6.09 |
| edgeHand | surface | 5.01 |
| ink | edgeHand tint | 14.84 |
| muted | edgeHand tint | 6.04 |
| edgeEdited | surface | 14.76 |
| ink | edgeEdited tint | 13.66 |
| muted | edgeEdited tint | 5.56 |
| edgeFree | surface | 5.07 |
| ink | edgeFree tint | 14.82 |
| muted | edgeFree tint | 6.04 |
| edgeSupp | surface | 6.23 |
| ink | edgeSupp tint | 14.62 |
| muted | edgeSupp tint | 5.96 |
| edgeDayOff | surface | 4.51 |
| ink | edgeDayOff tint | 15.09 |
| muted | edgeDayOff tint | 6.15 |
| oppositeMode.accentText (snackbar action) | ink | 12.98 |

## Oxford dark

| Foreground | Background | Ratio |
|---|---|---|
| ink | paper | 15.32 |
| ink | surface | 13.96 |
| ink | surfaceContainer | 12.17 |
| muted | paper | 8.21 |
| muted | surface | 7.48 |
| muted | surfaceContainer | 6.52 |
| warn | paper | 9.22 |
| warn | surface | 8.41 |
| warn | surfaceContainer | 7.33 |
| ok | paper | 8.91 |
| ok | surface | 8.12 |
| ok | surfaceContainer | 7.08 |
| accentText | paper | 13.56 |
| accentText | surface | 12.36 |
| accentText | surfaceContainer | 10.77 |
| second | paper | 6.76 |
| second | surface | 6.16 |
| second | surfaceContainer | 5.37 |
| onSecondSoft | paper | 13.43 |
| onSecondSoft | surface | 12.24 |
| onSecondSoft | surfaceContainer | 10.68 |
| accent | paper | 8.19 |
| accent | surface | 7.46 |
| accent | surfaceContainer | 6.51 |
| onAccent | accent | 8.21 |
| onAccentSoft | accentSoft | 10.37 |
| ink | accentSoft | 11.72 |
| onSecond | second | 6.62 |
| onSecondSoft | secondSoft | 10.49 |
| second | secondSoft | 5.28 |
| ink | secondSoft | 11.97 |
| onWarn | warn | 8.36 |
| onWarnSoft | warnSoft | 10.12 |
| warn | warnSoft | 6.76 |
| onOk | ok | 8.20 |
| onOkSoft | okSoft | 10.06 |
| ok | okSoft | 6.45 |
| onTile | tile1 | 11.72 |
| onTileMuted | tile1 | 8.34 |
| muted | tile1 | 6.28 |
| second (today dot) | tile1 | 5.17 |
| onTile | tile2 | 11.97 |
| onTileMuted | tile2 | 8.52 |
| muted | tile2 | 6.41 |
| second (today dot) | tile2 | 5.28 |
| onTile | tile3 | 9.96 |
| onTileMuted | tile3 | 7.09 |
| muted | tile3 | 5.34 |
| second (today dot) | tile3 | 4.40 |
| onTile | tile4 | 11.52 |
| onTileMuted | tile4 | 8.20 |
| muted | tile4 | 6.17 |
| second (today dot) | tile4 | 5.08 |
| onTile | tile5 | 10.78 |
| onTileMuted | tile5 | 7.67 |
| muted | tile5 | 5.78 |
| second (today dot) | tile5 | 4.76 |
| outline | paper | 5.85 |
| outline | surface | 5.34 |
| mark | paper | 8.19 |
| mark | surface | 7.46 |
| mark | surfaceContainer | 6.51 |
| mark | accentSoft | 6.26 |
| edgeTest | surface | 7.65 |
| ink | edgeTest tint | 11.22 |
| muted | edgeTest tint | 6.01 |
| edgeWorksheet | surface | 6.06 |
| ink | edgeWorksheet tint | 11.64 |
| muted | edgeWorksheet tint | 6.24 |
| edgeHand | surface | 4.58 |
| ink | edgeHand tint | 12.02 |
| muted | edgeHand tint | 6.44 |
| edgeEdited | surface | 7.46 |
| ink | edgeEdited tint | 11.17 |
| muted | edgeEdited tint | 5.99 |
| edgeFree | surface | 8.83 |
| ink | edgeFree tint | 10.89 |
| muted | edgeFree tint | 5.84 |
| edgeSupp | surface | 6.83 |
| ink | edgeSupp tint | 11.32 |
| muted | edgeSupp tint | 6.07 |
| edgeDayOff | surface | 5.34 |
| ink | edgeDayOff tint | 11.69 |
| muted | edgeDayOff tint | 6.27 |
| oppositeMode.accentText (snackbar action) | ink | 11.41 |

The static launcher uses Freetown light paper on Freetown light accent in both modes; the window background uses the matching Freetown mode before Compose draws.
