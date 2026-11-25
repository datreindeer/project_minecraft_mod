# Server Dashboard, Live Map and Tribe Territories Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Tribe territories (Fire Pit 5x5, big Fire Pit = its biome capped at ±16 chunks, never overlapping, used by `Lands`), and an admin-only web dashboard inside the mod with live stats, players, tribes and a self-rendered live map showing each tribe's area.

**Architecture:** Territory is a saved chunk set per `Clans.Clan`, computed by a pure `clan/Territory` helper. A new `dashboard` package runs the JDK `HttpServer` on `127.0.0.1`, serving a JSON snapshot rebuilt once a second on the server thread, PNG map tiles rendered from chunks as players load them, and a static HTML/JS page from mod resources.

**Tech Stack:** Java 25 / NeoForge 26.x, `com.sun.net.httpserver`, `javax.imageio`, plain HTML/CSS/JS (canvas). No new dependencies.

**Spec:** `docs/superpowers/specs/2026-11-25-server-dashboard-design.md` (Swedish). Read it before any task.

## Global Constraints

- Fire Pit territory and load radius: 2 chunks each way (5x5). `Clans.LOAD_RADIUS = 2`. Big pit loads 5x5 too.
- Big pit territory: 5x5 **plus** a 4-neighbour flood fill over chunks whose surface biome equals the biome at the pit block, within ±`TERRITORY_MAX = 16` chunks of the pit's chunk.
- A chunk belongs to at most one tribe, ever. Fills stop at other tribes' chunks and at NPC village/camp land. Freed chunks are not taken by neighbours automatically.
- Campfires: no territory, no loading.
- Dashboard: dedicated server only, bound to `127.0.0.1`, port from server config (default `8100`), on/off flag (default on). Never started in game tests or singleplayer.
- HTTP threads never touch the world; they serve the latest snapshot built on the server thread once per second.
- Map: overworld only, starts empty, tiles 512x512 blocks at 1 px/block in `<world>/mechanical_map/<rx>.<rz>.png`. No reading of old region files.
- Page text in English. Page polls every 2 s.
- Project habits: bump `mod_version` in `gradle.properties` on every commit (Task 1 → `0.210.0`, later tasks patch steps); lang lines come from `scripts/gen_*.py` (clan messages: `scripts/gen_clans.py`), never hand-edited JSON; game tests use `TestArea.of`; run game tests in a worktree with `./gradlew runGameTestServer --no-configuration-cache`, the full suite once at the end (not after every task); never touch main `run/` (live play server).

## Review Focus

1. **Old saves:** AmandaTest tribes have no `territory` field and may already sit closer than 5 chunks to each other. Expected: load without error; territories computed on first `keepPitsLoaded`, older tribe (save order) keeps contested chunks; nobody loses their pit. Test in Task 1.
2. **Big pit next to a river/beach or another tribe:** expected territory is never smaller than the 5x5 minus chunks other tribes already hold. Test in Task 1.
3. **Port 8100 taken** (second server in a worktree): expected one warning in the log, server keeps running, no crash. Test in Task 4.
4. **Fast travel / elytra / many chunks loading:** expected TPS unaffected; tile work is capped per tick and PNG writing happens off-thread. Test in Task 5 (queue cap).
5. **Disband / move vote / broken pit:** expected the territory is cleared at once, map and `Lands` stop showing it. Test in Task 1.

---

### Task 1: Territory data and 5x5 loading

**Files:**
- Create: `src/main/java/com/mechanical/clan/Territory.java`
- Modify: `src/main/java/com/mechanical/clan/Clans.java` (`LOAD_RADIUS`, `Clan` fields + codec, `loadRadius`, `keepPitsLoaded`, `found`, `claimPit`, vote handling at `VOTE_DISBAND`/`VOTE_MOVE`, `pitMoved`)
- Modify: `src/main/java/com/mechanical/FirePit.java:80` (drop `BIG_LOAD_RADIUS` or set to 2; fix its callers)
- Modify: `src/main/java/com/mechanical/FirePitBlock.java` (`upgrade` → recompute territory)
- Create: `src/main/java/com/mechanical/clan/TerritoryTest.java` (game test, pattern of `ClanTest`)
- Modify: `src/main/java/com/mechanical/clan/ClanTest.java` (its force-loading assertions expect 3x3; change to 5x5)

**Interfaces:**
- Produces:
  - `Territory.square(ChunkPos centre, int r) -> Set<Long>` (packed `ChunkPos.pack`)
  - `Territory.fill(ChunkPos start, int max, Predicate<ChunkPos> sameBiome, Predicate<ChunkPos> blocked) -> Set<Long>`: BFS, 4-neighbours, inside ±max of start, never enters `blocked`, always contains start unless blocked.
  - `Territory.surfaceBiome(ServerLevel level, ChunkPos c) -> Holder<Biome>`: chunk centre at surface; uses `level.getBiome` only when the chunk is loaded (`level.hasChunk`), otherwise the generator's biome source + `randomState().sampler()` at `generator.getBaseHeight(...)`, so no chunk is ever loaded or generated.
  - `Clans.Clan.territory` (`Set<Long>`, saved as `optionalFieldOf("territory", List.of())` of `Codec.LONG`), public getter `territory()`.
  - `Clans.TERRITORY_MAX = 16`
  - `void Clans.recomputeTerritory(MinecraftServer server, Clan clan)`: empty if no pit; else 5x5 minus other tribes' chunks and NPC land; if big pit, add `fill(...)` with `blocked` = other tribes' chunks or NPC land (`Lands.npcLandAt(level, chunk)`, Task 3 — until then a stub returning false), `sameBiome` = `surfaceBiome(c) == biome at pit block`.
  - `static Clan Clans.ownerOf(ServerLevel level, ChunkPos c)`: the tribe whose territory holds `c` in that dimension, or null.

- [ ] **Step 1: Write `TerritoryTest` with these checks** (each a `check(h, cond, msg)` like `ClanTest`):
  - `square(new ChunkPos(0,0), 2).size() == 25`
  - fill on a fake 40x40 biome grid (predicate over a lambda grid) with a 3-wide strip of same biome: result only contains strip chunks, all connected, none farther than 16 from start.
  - fill with `blocked` covering half the strip: no blocked chunk in result.
  - recompute for a regular pit tribe: `territory.size() == 25`.
  - two tribes with pits 3 chunks apart, recomputed in save order: no chunk in both; the first keeps all 25.
  - big pit (`FirePit.isBig`) with tribe B's territory 3 chunks east: result contains every chunk of its own 5x5 that B does not hold, no chunk of B, and nothing beyond ±16.
  - Clan decoded from a codec map without `territory` → empty set, no exception; after `keepPitsLoaded` → 25.
  - after `VOTE_MOVE` passes / disband: territory empty and `ownerOf` null for the old pit chunk.
  - force-loading: a regular pit's chunk at offset (2,2) is force-loaded, (3,3) is not.
- [ ] **Step 2: Run** the game tests in a worktree; expect `TerritoryTest` to fail to compile/fail.
- [ ] **Step 3: Implement** `Territory`, the `Clan` field + codec, `TERRITORY_MAX`, `LOAD_RADIUS = 2`, `recomputeTerritory`, `ownerOf`. Call `recomputeTerritory` after a pit is set (`found`, `claimPit`), in `pitMoved` and `FirePitBlock.upgrade`, clear on disband / move vote / missing pit in `keepPitsLoaded`, and in `keepPitsLoaded` compute it for any tribe with a pit but an empty territory (old saves; iterate `clans` in list order so the oldest wins). `setDirty()` after each change.
- [ ] **Step 4: Run** game tests in the worktree: `TerritoryTest` and `ClanTest` pass.
- [ ] **Step 5: Commit**, version `0.210.0`.

### Task 2: No overlapping tribes

**Files:**
- Modify: `src/main/java/com/mechanical/clan/Clans.java` (`found`, `claimPit`)
- Modify: `scripts/gen_clans.py` (new lang line), run it
- Modify: `src/main/java/com/mechanical/clan/TerritoryTest.java`

**Interfaces:**
- Consumes: `Territory.square`, `Clans.ownerOf`
- Produces: `Clan Clans.blockingTribe(GlobalPos pit)`: the first other tribe owning any chunk of `square(pit chunk, 2)`, or null.

- [ ] **Step 1: Add checks:** tribe A at chunk (0,0); `blockingTribe` for a pit at chunk (4,0) returns A; at chunk (5,0) returns null; `found` by a player next to a pit at chunk (4,0) returns false and creates no tribe.
- [ ] **Step 2: Run**, expect fail.
- [ ] **Step 3: Implement.** `found` and `claimPit` call `blockingTribe` after `freePitNear`; if non-null, `player.sendSystemMessage(Component.translatable("message.mechanical.clan.too_close", other.name, direction))` and return false. `direction` = compass word (8 directions) from the pit towards the other tribe's pit, as a translatable `message.mechanical.clan.dir.<n|ne|e|...>`. English text in `gen_clans.py`: `"Too close to %s's land (to the %s). Tribes' lands may not overlap: find a Fire Pit farther away."` `freePitNear` stays unchanged, so the tribe page button still works and the message reaches the player.
- [ ] **Step 4: Run** `python scripts/gen_clans.py`, then game tests: pass.
- [ ] **Step 5: Commit** (patch bump).

### Task 3: `Lands` follows the territory

**Files:**
- Modify: `src/main/java/com/mechanical/clan/Lands.java` (`landAt`, remove `PIT_CHUNKS`, add `npcLandAt`)
- Modify: `src/main/java/com/mechanical/clan/Clans.java` (replace Task 1 stub with `Lands.npcLandAt`)
- Modify: `src/main/java/com/mechanical/clan/ClanTest.java` (land checks)

**Interfaces:**
- Consumes: `Clans.ownerOf`
- Produces: `static boolean Lands.npcLandAt(ServerLevel level, ChunkPos c)`: the chunk centre is inside an NPC village (`TribeVillageStructure.villageAt` within `WALL_R + 2`) or within `CAMP_LAND` of a loaded NPC camp, using the same tests `landAt` uses today.

- [ ] **Step 1: Change `ClanTest` land checks:** a member at chunk offset (2,2) from the pit gets `HOME`; at (3,0) gets null (wild); a non-member at (1,1) gets `CLAN`.
- [ ] **Step 2: Run**, expect fail at (2,2).
- [ ] **Step 3: Implement:** the player-tribe branch of `landAt` becomes `Clans.ownerOf(level, chunk)`; extract the village/camp tests into `npcLandAt`; delete `PIT_CHUNKS` and fix its references (grep).
- [ ] **Step 4: Run** game tests: pass.
- [ ] **Step 5: Commit** (patch bump).

### Task 4: Dashboard server, config and stats JSON

**Files:**
- Create: `src/main/java/com/mechanical/ServerConfig.java` (pattern of `ClientConfig`; register as `ModConfig.Type.SERVER` next to `Mechanical.java:431`)
- Create: `src/main/java/com/mechanical/dashboard/Dashboard.java` (lifecycle + `HttpServer`)
- Create: `src/main/java/com/mechanical/dashboard/Snapshot.java` (builds JSON strings on the server thread)
- Create: `src/main/java/com/mechanical/dashboard/DashboardTest.java`
- Create: `src/main/resources/assets/mechanical/dashboard/index.html` (placeholder "ok", real page in Task 7)

**Interfaces:**
- Produces:
  - `ServerConfig.DASHBOARD` (bool, default true), `ServerConfig.DASHBOARD_PORT` (int 1024..65535, default 8100)
  - `Dashboard.start(MinecraftServer)` on `ServerStartedEvent` only if `server.isDedicatedServer()` and not a game test server and enabled; `Dashboard.stop()` on `ServerStoppingEvent`. Port in use → `LOGGER.warn` once, no throw.
  - `volatile Snapshot.Json latest` with fields `String status, players, tribes, map` rebuilt every 20 ticks in `ServerTickEvent.Post`.
  - `static String Snapshot.status(MinecraftServer)`, `players(MinecraftServer)`, `tribes(MinecraftServer)` (JSON via Gson `JsonObject`, already on the classpath).
  - Routes: `/` and `/<file>` → classpath `assets/mechanical/dashboard/<file>` (reject `..`, 404 otherwise); `/api/status|players|tribes|map` → `latest.*`, `Content-Type: application/json`, `Cache-Control: no-store`.
- JSON fields (exact names, frontend relies on them):
  - status: `name, version, uptimeSec, tps, mspt, online, max, memUsedMb, memMaxMb, loadedChunks, entities, day, time, season, month, dayOfMonth, year, worldAge`
  - players: array of `uuid, name, online, x, y, z, yaw, dim, health, food, ping, tribe` (online) / `uuid, name, online:false, lastSeenDay, tribe` (offline, from tribe members and the server's profile cache)
  - tribes: array of `id, name, colour` (hex `#rrggbb` from `DyeColor.byId(colour).getTextureDiffuseColor()`), `age, pit:{x,y,z,dim}|null, big, chunks, members:[{name, rank, online}], reputation:{tribeName: value}`

- [ ] **Step 1: Write `DashboardTest`:** `Snapshot.status(server)` parses as JSON and has every status key; `tribes(server)` with one test tribe has `chunks == 25` and colour matching `^#[0-9a-f]{6}$`; `Dashboard.start` on an occupied port (open a `ServerSocket` on 127.0.0.1 first) does not throw (call the internal `bind(int port)` directly, since the test server is not dedicated).
- [ ] **Step 2: Run**, expect fail.
- [ ] **Step 3: Implement** config, `Snapshot`, `Dashboard` (executor: `Executors.newFixedThreadPool(2)`; the server is a daemon).
- [ ] **Step 4: Run** game tests: pass. Then manually: `./gradlew runServer` in the worktree, `curl -s http://127.0.0.1:8100/api/status` returns JSON, and `curl` from another machine's IP is refused.
- [ ] **Step 5: Commit** (patch bump).

### Task 5: Map tiles

**Files:**
- Create: `src/main/java/com/mechanical/dashboard/MapTiles.java`
- Modify: `src/main/java/com/mechanical/dashboard/Dashboard.java` (route `/tiles/<rx>.<rz>.png`, 404 if missing, `Cache-Control: no-cache`)
- Modify: `src/main/java/com/mechanical/dashboard/DashboardTest.java`

**Interfaces:**
- Produces:
  - `static int[] MapTiles.colours(LevelChunk chunk)`: 256 ARGB values, index `z * 16 + x`. Top block from `Heightmap.Types.WORLD_SURFACE`; colour = `state.getMapColor(level, pos)` with brightness by comparing height with the block to the north (lower `LOW`, equal `NORMAL`, higher `HIGH`, like vanilla maps); water uses depth to the first non-water block for brightness.
  - Chunks queued on `ChunkEvent.Load` (overworld, only when the dashboard runs) and on `ChunkEvent.Unload`; at most `PER_TICK = 8` chunks read per server tick; the colours go to one worker thread which merges them into the region image (keep the open `BufferedImage`s in an LRU of 16, write PNG with `ImageIO` on change, at most once per region per 5 s, and all on stop).
  - Folder: `server.getWorldPath(LevelResource.ROOT).resolve("mechanical_map")`.
- [ ] **Step 1: Add checks:** place grass over a flat area and water 3 deep in the test area; `colours(chunk)` for those columns gives the grass map colour and a darker water colour for deeper water; a queue with 100 chunks drains only `PER_TICK` per tick.
- [ ] **Step 2: Run**, expect fail.
- [ ] **Step 3: Implement.**
- [ ] **Step 4: Run** game tests: pass. Manually in the worktree server: join with a test client (muted, see memory), walk; a PNG appears under `mechanical_map/` and loads at `/tiles/0.0.png`.
- [ ] **Step 5: Commit** (patch bump).

### Task 6: Map data (`/api/map`) with tribe outlines

**Files:**
- Create: `src/main/java/com/mechanical/dashboard/Outline.java`
- Modify: `src/main/java/com/mechanical/dashboard/Snapshot.java` (`map(MinecraftServer)`)
- Modify: `src/main/java/com/mechanical/dashboard/DashboardTest.java`

**Interfaces:**
- Consumes: `Clans.Clan.territory()`, `CampAge` camp centres (fires the mod already tracks; no world scan)
- Produces:
  - `static List<int[]> Outline.of(Set<Long> chunks)`: closed polygons in block coordinates along chunk edges (outer rings and holes), each `int[]` = `x0,z0,x1,z1,...`. Approach: collect every chunk edge whose neighbour is not in the set, directed so the inside is on the left, then chain edges into rings; drop collinear points.
  - map JSON: `players:[{name, x, z, yaw, tribe}]` (overworld), `fires:[{x, z, kind:"fire_pit"|"big_fire_pit"|"campfire", tribe|null}]`, `tribes:[{id, name, colour, rings:[[x0,z0,...],...], label:{x,z}}]` where label = centroid of the territory chunks snapped to the nearest chunk in the set.
- [ ] **Step 1: Add checks:** `Outline.of(square(0,0,0))` = one ring with 4 corners `(0,0) (16,0) (16,16) (0,16)` in some rotation; a 3x3 with the middle missing = 2 rings; an L shape = one ring with 6 corners; `map(server)` with one tribe has one ring and its label inside the territory.
- [ ] **Step 2: Run**, expect fail.
- [ ] **Step 3: Implement.**
- [ ] **Step 4: Run** game tests: pass.
- [ ] **Step 5: Commit** (patch bump).

### Task 7: The dashboard page

**Files:**
- Create/replace: `src/main/resources/assets/mechanical/dashboard/index.html`, `style.css`, `app.js`

**Interfaces:**
- Consumes: JSON fields from Tasks 4 and 6, `/tiles/<rx>.<rz>.png`.

- [ ] **Step 1: Load the `frontend-design` skill**, and follow the project's quality bar (memory: quality-bar). Header with mod name and version, tabs Overview / Players / Tribes / Map, polling every 2 s.
  - Overview: tiles for TPS, MSPT, players, memory, loaded chunks, entities, day/season/date, world age, uptime; sparklines of TPS and players for the last hour (kept in page memory).
  - Players: table (name, online, tribe, position, health, food, ping, last seen); click → Map tab centred on the player.
  - Tribes: card per tribe in its colour (members with rank, age, pit, big/regular, territory chunks); click → Map tab fitted to the territory.
  - Map: canvas, drag to pan, wheel to zoom (0.25x to 8x, smooth), layers drawn in order tiles → territories (fill tribe colour at ~25 % alpha, 3 px stroke in the tribe colour, name label at `label`) → fires (Fire Pit icon larger than Campfire, big pit larger still) → players (dot + name + heading). Hover readout: block x/z and the owning tribe. Tiles cached per URL and reloaded at most every 10 s.
- [ ] **Step 2: Run** the worktree server with a test client and two test tribes (one big pit); open `http://localhost:8100`, take a screenshot of each tab, check that territories are easy to tell apart and nothing overlaps or floats.
- [ ] **Step 3: Commit** (patch bump).

### Task 8: Docs, full test run, push

**Files:**
- Modify: `docs/seasons-and-tribes.md` (tribe land: 5x5 / biome / no overlap / dashboard section)
- Modify: memory `play-server.md` (dashboard at `http://localhost:8100` on the play server)

- [ ] **Step 1:** Update the docs.
- [ ] **Step 2:** Run the full game test suite once in the worktree: `./gradlew runGameTestServer --no-configuration-cache`. Expected: all required tests pass (known flakes per memory: elkwight_world, living_world_blocks; rerun those alone if they fail).
- [ ] **Step 3:** Merge to main, commit, push. Restart the play server only when the user says so.
