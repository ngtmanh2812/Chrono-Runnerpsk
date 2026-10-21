# Chapter 2 — Post-apocalyptic arena

## Artwork and transform

- Original: `Shadowed Ruins Over a Sunlit Canal.png`, PNG RGB, **1913 × 822**.
- Runtime copy: `assets/backgrounds/chapter_2.png`; identical bytes to the original.
- Renderer: existing `ArenaBackdrop`, nearest-neighbor, uniform scale **720 / 822 = 120 / 137**.
- World size: **1675.620438 × 720**. Logical viewport remains **1280 × 720**.
- Map origin: **(0, 0)**. Image, collision, actors and debug share the same camera transform.
- Bounded camera x: **0 … 395.620438**; y: **0**. Existing optional screen shake is unchanged.
- Actor movement bounds: **0 ≤ x ≤ worldWidth − actorWidth**.
- No parallax skyline or generated stone platforms are drawn over this image: the existing image-arena rendering path handles both automatically.
- Cars, containers, fences, ladders, flames and broken scenery below the road have no separate collision. The right upper cap stops before its container.
- The source image has a warm sun/reflection in the distant canal. The road itself stays dark and neutral; no color wash, blur or image filtering was added.

## Geometry

Coordinates were measured on the actual source PNG, not copied from Chapter 1. Multiply every source coordinate by **120/137** to obtain world coordinates. All elevated caps are one-way and **12 source pixels / 10.510949 world units** thick.

| Surface | Source x | Source y | Source width | World x | World y | World width |
|---|---:|---:|---:|---:|---:|---:|
| Left top scaffold | 0 | 155 | 241 | 0.00 | 135.77 | 211.09 |
| Left upper scaffold | 255 | 243 | 185 | 223.36 | 212.85 | 162.04 |
| Left middle scaffold | 0 | 319 | 278 | 0.00 | 279.42 | 243.50 |
| Left lower scaffold | 142 | 415 | 249 | 124.38 | 363.50 | 218.10 |
| Left suspended slab | 611 | 397 | 242 | 535.18 | 347.74 | 211.97 |
| Right upper clear cap | 1533 | 243 | 77 | 1342.77 | 212.85 | 67.45 |
| Right middle scaffold | 1671 | 319 | 242 | 1463.65 | 279.42 | 211.97 |
| Right lower scaffold | 1588 | 431 | 178 | 1390.95 | 377.52 | 155.91 |
| Right suspended slab | 1090 | 397 | 226 | 954.74 | 347.74 | 197.96 |

The uninterrupted main road top is at **source y=568**, or **world y=497.518248**. Its collision rectangle is **(0, 497.518248, 1675.620438, 302.481752)**, including the existing 80-unit safety depth below the world. There are no independently playable surfaces below this road.

## Spawns and boss behavior

- P1: source x=650 → world **(569.343066, 435.518248)** for the existing 44×62 body; feet at road y=497.518248.
- P2: source x=730 → world **(639.416058, 435.518248)**.
- Void Core: source **(1260,170)** → world body top-left **(1103.649635,148.905109)**, existing 116×140 body, initially airborne.
- Both P1 and the complete idle boss sprite fit the initial viewport. The boss starts above the right suspended slab without intersecting it or the HUD.
- Existing centered sprite pivot, gravity, movement, teleport/dive, volleys, radial burst, summons, damage, phases and sprites are untouched. The current boss is visually floating but already uses shared gravity; this map change does not add sustained flight or replace that behavior.

## Files

- Modified `src/chronorunner/world/LevelFactory.java`: only `future()` was replaced; `past()` is byte-for-byte unchanged.
- Added `assets/backgrounds/chapter_2.png`.
- Added `tools/ChapterTwoMapCheck.java`.
- Updated `tools/ChapterOneMapCheck.java` to share its verification helpers and remove its obsolete assertion that Chapter 2 must have four platforms. Chapter 1 gameplay was not edited.
- Backup: `backups/chapter2-before-post-apocalyptic/LevelFactory.java.bak`.
- This report: `docs/chapter-2-arena.md`.

The backed-up Chapter 2 used world 1920×720, ground y=656, caps (420,480,200,24), (820,390,220,24), (1240,460,220,24), (1580,340,190,24), player x=360/520 and Void Core at (1640,500).

## Verification

1. Existing `build.bat`: **BUILD OK**.
2. Chapter 2: **11,573 assertions passed**, including each platform's upward crossing, landing, side passage, fast fall, continuous floor and arena limits.
3. All three playable characters traverse the entire road in both directions and can reach all nine elevated caps using their existing jump/double-jump settings.
4. Existing Void Core AI ran for 20 simulated seconds, exercising projectiles, melee, radial burst, teleport/dive and summons. Health and phases 2/3 passed.
5. Actual `Game` / `PlayScreen` loading and rendering were exercised. Left/right camera bounds, HUD, pause/resume without rebuilding the level, victory and game-over detection passed. Verification stops before result timers complete, so it does not write progression saves.
6. Chapter 1 regression: **756 assertions passed**. Its factory method and background, plus every other production source file, matched the pre-edit snapshots.
7. Debug was enabled during per-cap and camera checks, then disabled. Normal left/right renders were generated afterward. The normal and debug engine renders were visually inspected; there was no manual keyboard playthrough in a desktop window for this change.
8. Original image and runtime copy matched SHA-256. No source image, player/boss sprite, AI, combat, menu or HUD code changed.

Verification images are under `out/chapter2-check/` (`left-normal.png`, `right-normal.png`, camera debug renders, per-platform debug renders, pause and victory). These are development outputs, not in-game overlays or asset replacements.

Reproduce in PowerShell:

```powershell
.\build.bat
javac -encoding UTF-8 -cp out -d out tools/ChapterOneMapCheck.java tools/ChapterTwoMapCheck.java
java '-Djava.awt.headless=true' -cp out ChapterTwoMapCheck
java '-Djava.awt.headless=true' -cp out ChapterOneMapCheck
```

Normal launch uses `run.bat` with debug disabled. Optional manual Chapter 2 inspection can use `java -cp out ChapterTwoMapCheck --live` after compiling the verification helper; this does not unlock or alter chapter progression.
