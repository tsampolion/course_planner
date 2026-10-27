# Course Pacer palette contrast

Pacer owns Bone & green and Saffron & ink (D13, 3.8.0), each in light and dark mode. The shared suite guide remains unchanged and still shows the previous palette; other suite apps may adopt these palettes later.

Ratios are WCAG 2.x, rounded to two decimals, computed from `ui/theme/Tokens.kt`. Text pairs reach 4.5:1; outlines and lesson-kind edges reach 3:1. `ContrastTest` checks the rules across all four sets. The legacy tile field names now hold sage, neutral, mist and warm-earth tones rather than lilac or pink.

SaffronLight accent is a **fill only**: its contrast on paper is below 3:1. Links, icons, field focus and other foreground controls use `accentText` (the existing `onAccentSoft` token). Material primary stays the fill accent. Date picker internals use a readable local primary, and snackbar inversePrimary uses the other mode’s accentText. Progress fills, ticks and the update dot on ☰ use `mark` (the accent, or `accentText` in Saffron light), which reaches 3:1 on every ground. Behind keeps its word and pill shape as well as a rust/red tone.

## BoneLight

| Foreground | Background | Ratio |
|---|---|---|
| ink | paper | 13.08 |
| ink | surface | 14.58 |
| ink | surfaceContainer | 12.13 |
| muted | paper | 5.19 |
| muted | surface | 5.79 |
| muted | surfaceContainer | 4.81 |
| warn | paper | 5.60 |
| warn | surface | 6.25 |
| warn | surfaceContainer | 5.20 |
| ok | paper | 5.70 |
| ok | surface | 6.36 |
| ok | surfaceContainer | 5.29 |
| accentText | paper | 9.30 |
| accentText | surface | 10.37 |
| accentText | surfaceContainer | 8.62 |
| accent | paper | 5.00 |
| accent | surface | 5.58 |
| accent | surfaceContainer | 4.64 |
| onAccent | accent | 5.58 |
| onAccentSoft | accentSoft | 8.76 |
| onWarn | warn | 6.25 |
| onWarnSoft | warnSoft | 9.25 |
| onOk | ok | 6.36 |
| onOkSoft | okSoft | 9.00 |
| warn | warnSoft | 5.20 |
| ok | okSoft | 5.29 |
| onTile | tileLilac | 11.89 |
| onTileMuted | tileLilac | 6.01 |
| onTile | tileMint | 11.91 |
| onTileMuted | tileMint | 6.02 |
| onTile | tileSky | 11.84 |
| onTileMuted | tileSky | 5.99 |
| onTile | tileApricot | 11.75 |
| onTileMuted | tileApricot | 5.94 |
| onTile | tilePink | 12.29 |
| onTileMuted | tilePink | 6.21 |
| outline | paper | 3.60 |
| outline | surface | 4.01 |
| edgeTest | surface | 5.49 |
| edgeDayOff | surface | 4.01 |
| edgeEdited | surface | 5.58 |
| edgeHand | surface | 5.23 |
| edgeFree | surface | 4.59 |
| edgeSupp | surface | 8.82 |
| edgeWorksheet | surface | 3.22 |
| oppositeMode.accentText (snackbar action) | ink | 12.57 |

## BoneDark

| Foreground | Background | Ratio |
|---|---|---|
| ink | paper | 13.30 |
| ink | surface | 11.75 |
| ink | surfaceContainer | 10.01 |
| muted | paper | 6.46 |
| muted | surface | 5.71 |
| muted | surfaceContainer | 4.86 |
| warn | paper | 7.73 |
| warn | surface | 6.83 |
| warn | surfaceContainer | 5.82 |
| ok | paper | 9.23 |
| ok | surface | 8.16 |
| ok | surfaceContainer | 6.95 |
| accentText | paper | 13.31 |
| accentText | surface | 11.76 |
| accentText | surfaceContainer | 10.02 |
| accent | paper | 9.23 |
| accent | surface | 8.16 |
| accent | surfaceContainer | 6.95 |
| onAccent | accent | 9.56 |
| onAccentSoft | accentSoft | 8.52 |
| onWarn | warn | 7.40 |
| onWarnSoft | warnSoft | 8.85 |
| onOk | ok | 9.56 |
| onOkSoft | okSoft | 8.55 |
| warn | warnSoft | 5.85 |
| ok | okSoft | 6.06 |
| onTile | tileLilac | 8.69 |
| onTileMuted | tileLilac | 6.72 |
| onTile | tileMint | 8.61 |
| onTileMuted | tileMint | 6.66 |
| onTile | tileSky | 8.62 |
| onTileMuted | tileSky | 6.67 |
| onTile | tileApricot | 8.70 |
| onTileMuted | tileApricot | 6.73 |
| onTile | tilePink | 8.19 |
| onTileMuted | tilePink | 6.34 |
| outline | paper | 5.30 |
| outline | surface | 4.68 |
| edgeTest | surface | 5.08 |
| edgeDayOff | surface | 4.68 |
| edgeEdited | surface | 8.16 |
| edgeHand | surface | 6.49 |
| edgeFree | surface | 8.61 |
| edgeSupp | surface | 5.44 |
| edgeWorksheet | surface | 3.34 |
| oppositeMode.accentText (snackbar action) | ink | 8.93 |

## SaffronLight

| Foreground | Background | Ratio |
|---|---|---|
| ink | paper | 13.97 |
| ink | surface | 15.38 |
| ink | surfaceContainer | 12.59 |
| muted | paper | 5.20 |
| muted | surface | 5.73 |
| muted | surfaceContainer | 4.69 |
| warn | paper | 6.80 |
| warn | surface | 7.48 |
| warn | surfaceContainer | 6.12 |
| ok | paper | 6.71 |
| ok | surface | 7.39 |
| ok | surfaceContainer | 6.05 |
| accentText | paper | 9.44 |
| accentText | surface | 10.39 |
| accentText | surfaceContainer | 8.51 |
| onAccent | accent | 8.92 |
| onAccentSoft | accentSoft | 8.36 |
| onWarn | warn | 7.48 |
| onWarnSoft | warnSoft | 9.20 |
| onOk | ok | 7.39 |
| onOkSoft | okSoft | 9.17 |
| warn | warnSoft | 5.74 |
| ok | okSoft | 5.86 |
| onTile | tileLilac | 12.35 |
| onTileMuted | tileLilac | 6.26 |
| onTile | tileMint | 12.07 |
| onTileMuted | tileMint | 6.12 |
| onTile | tileSky | 12.16 |
| onTileMuted | tileSky | 6.16 |
| onTile | tileApricot | 12.02 |
| onTileMuted | tileApricot | 6.09 |
| onTile | tilePink | 11.78 |
| onTileMuted | tilePink | 5.97 |
| outline | paper | 3.75 |
| outline | surface | 4.13 |
| edgeTest | surface | 5.44 |
| edgeDayOff | surface | 4.13 |
| edgeEdited | surface | 7.39 |
| edgeHand | surface | 5.85 |
| edgeFree | surface | 5.14 |
| edgeSupp | surface | 9.88 |
| edgeWorksheet | surface | 8.56 |
| oppositeMode.accentText (snackbar action) | ink | 11.86 |

Fill-only accent on paper: **1.57:1** (excluded from the text pairs).

## SaffronDark

| Foreground | Background | Ratio |
|---|---|---|
| ink | paper | 15.52 |
| ink | surface | 13.87 |
| ink | surfaceContainer | 11.28 |
| muted | paper | 7.80 |
| muted | surface | 6.97 |
| muted | surfaceContainer | 5.67 |
| warn | paper | 7.11 |
| warn | surface | 6.36 |
| warn | surfaceContainer | 5.17 |
| ok | paper | 9.77 |
| ok | surface | 8.73 |
| ok | surfaceContainer | 7.10 |
| accentText | paper | 13.76 |
| accentText | surface | 12.29 |
| accentText | surfaceContainer | 10.00 |
| accent | paper | 11.25 |
| accent | surface | 10.05 |
| accent | surfaceContainer | 8.17 |
| onAccent | accent | 11.25 |
| onAccentSoft | accentSoft | 8.53 |
| onWarn | warn | 6.59 |
| onWarnSoft | warnSoft | 9.03 |
| onOk | ok | 8.67 |
| onOkSoft | okSoft | 9.00 |
| warn | warnSoft | 5.32 |
| ok | okSoft | 6.25 |
| onTile | tileLilac | 9.07 |
| onTileMuted | tileLilac | 6.81 |
| onTile | tileMint | 9.79 |
| onTileMuted | tileMint | 7.35 |
| onTile | tileSky | 10.03 |
| onTileMuted | tileSky | 7.53 |
| onTile | tileApricot | 9.70 |
| onTileMuted | tileApricot | 7.28 |
| onTile | tilePink | 9.88 |
| onTileMuted | tilePink | 7.41 |
| outline | paper | 5.03 |
| outline | surface | 4.50 |
| edgeTest | surface | 5.31 |
| edgeDayOff | surface | 4.50 |
| edgeEdited | surface | 8.73 |
| edgeHand | surface | 6.78 |
| edgeFree | surface | 9.00 |
| edgeSupp | surface | 5.68 |
| edgeWorksheet | surface | 3.23 |
| oppositeMode.accentText (snackbar action) | ink | 9.04 |

The static launcher uses BoneLight paper on BoneLight accent in both modes; the window background uses the matching Bone mode before Compose draws. Full device, large-text and tablet visual verification remains pending in the cloud environment.
