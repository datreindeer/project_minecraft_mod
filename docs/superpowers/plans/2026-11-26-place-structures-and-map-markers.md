# Place Structures and Map Markers Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** `/structure tribe_village|ancient_oak <x> <z>`, and Ancient Oaks, waystones and tribe villages as markers on the web map.

**Architecture:** The command calls `Structure.generate` (as `worldgen/Trails.java:191` does), places the start chunk by chunk, then saves the start and its references in the chunks. A new `SavedData`, `dashboard/Landmarks`, built like `dashboard/Villages`, remembers the places and feeds `/api/map`.

**Tech Stack:** Java, NeoForge 26.x, plain JS canvas.

**Spec:** `docs/superpowers/specs/2026-11-26-place-structures-and-map-markers-design.md`

## Global Constraints
- Command: `Commands.LEVEL_GAMEMASTERS`, Overworld only, messages in English.
- Only explored land is shown. No reading of region files.
- Dashboard code on chunk events runs inside `Dashboard.guard`.
- No new dependencies. Stage files by name (shared checkout), never `git add -A`.

## Review Focus
- Part of the footprint was never generated: those chunks are generated first, nothing is cut off.
- A player build or another structure in the footprint: refused, no block changed.
- Server restart after placing: `villageAt` still finds the village, the markers are still there.
- A waystone chunk older than waystones (no stone built): no marker until the stone is built.
- Command run from the Nether or the server console: Nether refused, console works.

---

### Task 1: The command

**Files:**
- Create: `src/main/java/com/mechanical/worldgen/PlaceStructure.java`, `PlaceStructureTest.java` (registered like `dashboard/DashboardTest.java`, area via `com.mechanical.TestArea.of`)
- Modify: `src/main/java/com/mechanical/people/AgeRebuild.java` (it only learns of a village on chunk load, so a placed one must be handed to it)

**Interfaces:**
- Produces: `public static String PlaceStructure.place(ServerLevel level, String kind, int x, int z)`: `null` on success, otherwise the refusal text. `kind` is `tribe_village` or `ancient_oak`.
- Produces: `public static void AgeRebuild.found(TribeVillageStructure.Piece piece)`, used by both the chunk-load handler and the command.

- [ ] **Step 1: Failing gametest.** Place a village well away from the test area. Assert: `place(...) == null`; `TribeVillageStructure.villageAt(level, centre) != null`; a second `place` at the same spot is not null and `level.getBlockState` at the campfire is unchanged. Place an oak elsewhere: result null, `AncientOak.ALTAR` at the piece's `foot()`. With a crafting table in a fresh footprint: refused, the table still there, nothing placed.
- [ ] **Step 2:** `./gradlew runGameTestServer`, expect the new test to fail (compile error).
- [ ] **Step 3: Implement.** The structure snaps to the centre of the chunk holding x,z. Order:
  1. `generate(...)` with predicate `b -> true`; invalid start: "Can't build there: water, or the ground is too uneven".
  2. `level.getChunk` for every chunk the bounding box touches (this generates them).
  3. Refuse if any of them has a reference to `tribe_village`, `ancient_oak` or a `StructureTags.VILLAGE` structure whose box intersects, or fails `Retrogen.noPlayerBlocks`.
  4. `start.placeInChunk` per chunk, box = that chunk over the full world height.
  5. `setStartForStructure` on the start chunk, `addReferenceForStructure` and `markUnsaved` on all.
  6. A village: `AgeRebuild.found(piece)`.

  The success message gives the real centre x y z.
- [ ] **Step 4:** Rerun; `place_structure` passes.
- [ ] **Step 5:** Commit: `Add /structure to place tribe villages and ancient oaks`.

### Task 2: Map markers

**Files:**
- Create: `src/main/java/com/mechanical/dashboard/Landmarks.java`
- Modify: `dashboard/Snapshot.java` (`map`), `dashboard/DashboardTest.java`, `worldgen/PlaceStructure.java`, `worldgen/Waystones.java` (`teleport`, when it builds), `worldgen/Regen.java` (`waystone`, when it builds)
- Modify: `src/main/resources/assets/mechanical/dashboard/app.js`, `style.css`
- Modify: `gradle.properties` (`mod_version`, minor step), `docs/seasons-and-tribes.md` (one short section on the command and markers)

**Interfaces:**
- Consumes: `PlaceStructure.place` from Task 1.
- Produces: `public static void Landmarks.add(ServerLevel level, String kind, int x, int z)`, kinds `tribe_village`, `ancient_oak`, `waystone`; the same kind at the same x,z is stored once.
- Produces: `/api/map` gets `"landmarks": [{"kind", "x", "z"}]`.

- [ ] **Step 1: Failing test** in `DashboardTest`: two identical `Landmarks.add(level, "waystone", 8, 8)` give exactly one entry with those values in `Snapshot.map(server)`.
- [ ] **Step 2:** Run the gametests, expect failure.
- [ ] **Step 3: Implement.** On overworld chunk load: queue starts of the two structures (read as `Villages.onChunkLoad` does) and chunks where `Waystones.partner` is not null; drain on the level tick, recording a waystone only if `Waystones.findStone` finds it. Call `add` from the three places that build in already loaded chunks.
- [ ] **Step 4: Page.** Three canvas icons in the style of `fires()` (oak crown, standing stone with a rune glow, hut), drawn under tribes' fires and players; legend rows for each kind present; the readout names the marker under the pointer.
- [ ] **Step 5:** Run the full gametests once; all pass.
- [ ] **Step 6:** Start a test server from a worktree `run/`, place both structures, screenshot the map, check the icons closely.
- [ ] **Step 7:** Commit: `Show ancient oaks, waystones and tribe villages on the web map`.
