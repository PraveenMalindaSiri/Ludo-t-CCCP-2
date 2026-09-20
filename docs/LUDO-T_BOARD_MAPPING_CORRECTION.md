# LUDO-T Assignment 1 board mapping correction

The board renderer and game-core configuration now use the same global 52-cell path. Cell 0 is the Yellow X cell and numbering proceeds clockwise.

| Player | Base quadrant | X/start cell | Approach circle | Home straight |
|---|---|---:|---:|---|
| Yellow | top-right | 0 | 50 | top arm |
| Blue | bottom-right | 13 | 11 | right arm |
| Red | bottom-left | 26 | 24 | bottom arm |
| Green | top-left | 39 | 37 | left arm |

Rule T-11 numbers its destinations from the Yellow approach circle as local cell 0. Translating those local offsets to the application's global path gives:

| Effect | T-11 local offset | Global cell |
|---|---:|---:|
| Alpha | 8 | 6 |
| Beta | 26 | 24 |
| Gamma | 45 | 43 |

Beta therefore occupies the same square as the Red approach circle. The client renders both indicators on that square.

The correction is intentionally limited to board configuration, deterministic Swing geometry, visual markers, tests, and documentation. It does not change the network protocol, persistence schema, server lifecycle, or concurrency design.
