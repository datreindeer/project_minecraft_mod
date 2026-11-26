# Dynamic Tools Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** A tool's binding and rod change its durability, speed and damage and show on its sprite, built in a rebuilt Tool Station.

**Architecture:** Tool item types stay as they are (head material = item). A `tool_parts` data component (binding, rod, smithing level) is written when the tool is taken from the Tool Station, together with recomputed vanilla components (`MAX_DAMAGE`, tool speed, attack damage), so existing code needs no change. Parts and their stats live in one table (`tools/ToolParts.java`). Looks come from generated layered sprites and `minecraft:composite` item models.

**Tech Stack:** NeoForge 26.3.0.95, MC 26.3, Java; data components; 26.3 item model definitions (composite/select on custom_model_data); Python generators in scripts/; GameTest; HideUi; GuiShots.

**Spec:** `docs/superpowers/specs/2026-11-26-dynamic-tools-design.md`

## Global Constraints

- Worktree `C:/Users/Yngve/Documents/mm_wt/dyntools`, branch `feat/dynamic-tools`. Stage by name. Never touch the main checkout.
- Tiers, rod and binding tables, the stat formula (relative to each recipe's current default parts, so a tool built with the old parts keeps exactly its old stats), Smithing +0.5 %/level, Poor Bronze 70 % on top: exactly as the spec.
- A stack without `tool_parts` = default parts; nothing about it changes on load.
- Dynamic tool types: pickaxe, axe, shovel, hoe, sickle, knife, saw, chisel, smithing hammer, quarry hammer, spear, sword, tongs, ard. Bows, fishing rods, broom, bucket keep fixed recipes.
- Package `com.mechanical.tools` (common), `com.mechanical.client.tools` (client).
- Gametests `./gradlew runGameTestServer --no-configuration-cache` (long timeout, ≤2 per task). Generators: run the one you change; generated files committed.
- Commit trailer `Co-Authored-By: Claude Opus 5.5 <noreply@anthropic.com>`. Version at the end 0.224.1 → **0.225.0**.

## Review Focus

1. An old tool (no component) in a player's inventory: identical durability/speed/damage before and after this change. (Task 1 test `defaultsUnchanged`.)
2. Rod tier above head tier (steel rod on flint head) never produces a result. (Task 3 test.)
3. A damaged-then-rebuilt or Poor Bronze tool: max damage recomputed without losing current damage proportion incorrectly — Poor 70 % applies once. (Task 1 test `poorOnce`.)
4. Two stacks of the same tool with different parts never merge (components differ → vanilla keeps them apart) — checked in Task 3 test.
5. Recipe viewer still lists Tool Station recipes and only shows seen parts. (Task 6.)

---

### Task 1: Parts table and stats

**Files:** Create `tools/ToolParts.java` (component type `TOOL_PARTS` registered like `Bronze.POOR` via `Casting.COMPONENTS` or the mod's component register; record `Parts(Item binding, Item rod, int smithing)`; part tables with tier/dur/speed/damage; `static int tier(Item head)`; `static void apply(ItemStack tool, Item defaultBinding, Item defaultRod, Parts parts)` writing `TOOL_PARTS`, `MAX_DAMAGE`, tool speed and attack damage relative to the defaults), `tools/ToolsTest.java` (test id `mechanical:tools`). Modify `Mechanical.java` init line.

- [ ] Tests: `defaultsUnchanged` (for every ToolStationRecipes recipe: `apply` with the recipe's own binding/rod and smithing 0 leaves MAX_DAMAGE, attack damage and mining speed equal to a fresh stack), `steelRodLonger` (bronze pickaxe + Leather + Steel Tool Rod … steel rod is tier 4 > bronze 2 → `allowed` false; with Bronze Tool Rod: durability = base × 1.35×1.1 / (1.0×1.0) ± 1), `smithingBonus` (level 40 → ×1.2), `poorOnce` (Poor Bronze stack → 70 % of the computed value, applied once).
- [ ] Run → implement → run → commit "Dynamic tools: parts and stats".

### Task 2: New rods

**Files:** Register `bone_tool_rod`, `copper_tool_rod`, `bronze_tool_rod`, `steel_tool_rod` (where Iron Tool Rod is registered or the metal's own class); recipes mirroring how Iron Tool Rod is made today (find it) per metal; Bone Tool Rod = Bone + `#mechanical:knives` shapeless (knife wears like other knife recipes); textures by a generator in the style of the existing Tool Rod / Iron Tool Rod sprites (palette per metal); lang; `docs/registry_notes*.json` entries; the age docs where each metal's items are listed.
- [ ] Test `rodsExist` (each item registered, each has a recipe in the RecipeManager or the station lists).
- [ ] Commit "Dynamic tools: bone, copper, bronze and steel tool rods".

### Task 3: Tool Station logic

**Files:** `ToolStationRecipes.java` (Recipe keeps `head`, adds `List<Supplier<Item>> bindings()`/`rods()` derived from the tables filtered by the head's tier, keeps the old binding/rod as the defaults; `matches` accepts any allowed binding/rod; Hide Glue already in the binding table), `ToolStationMenu.java` (result = `ToolParts.apply(new ItemStack(result), defaults, chosen parts with the taker's Smithing level)` then `Bronze.inheritPoor`; computed in `slotsChanged` for display and again on take with the real player), keep unlocks and shift-fill working.
- [ ] Tests: `anyAllowedBinding` (bronze pickaxe head + Leather + Bronze Tool Rod → result with those parts), `tierBlocks` (flint spear with Bone Tool Rod → no result), `noMerge` (two results with different rods do not stack).
- [ ] Commit "Dynamic tools: the Tool Station takes any allowed binding and rod".

### Task 4: Tool Station screen

**Files:** `client/ToolStationScreen.java` (rebuild): left a list of tool types (one icon per type: pickaxe, axe, …) filtered to unlocked recipes; picking a type shows the head/binding/rod slots with ghost icons (the expected part, faded) and a hint of allowed parts; right the result preview and a stats box: durability, speed, damage, and per part its contribution ("Rod: Bronze Tool Rod ×1.35 durability"), plus "Smithing 40: +20 %". Menu layout may grow (adjust the texture or draw with HideUi). Lang keys. GuiShots `toolstation_empty`, `toolstation_bronze_pickaxe`.
- [ ] Shoot, inspect closely, re-shoot until good. Commit "Dynamic tools: the new Tool Station screen".

### Task 5: Looks

**Files:** new generator `scripts/gen_tool_parts.py`: for every dynamic tool sprite, split head / haft (wood palette pixels) / binding (haft pixels touching the head, ~2–4 px); write head layer, one haft layer per rod material (recolour with the material's palette, keep the shading ramp), one binding layer per binding; write `assets/mechanical/items/<tool>.json` as `minecraft:composite` [head, `select` on `custom_model_data` strings index 0 (rod), index 1 (binding), fallback = default part]. Vanilla tool items used by the mod (Items.STONE_PICKAXE, COPPER_*, IRON_*): override their item model under `assets/minecraft/items/` the same way, from the vanilla sprites. `ToolParts.apply` writes the two strings into `CUSTOM_MODEL_DATA`. Spears keep their 3D in-hand model (`// shortcut:` note).
- [ ] GuiShots `tool_variants`: one pickaxe with each rod and a few bindings side by side in an inventory, plus first-person held. Inspect closely against the quality bar (shading kept, no stray pixels). Commit "Dynamic tools: tools look like their parts".

### Task 6: Tooltip and recipe viewer

**Files:** tooltip line (where tool tooltips are added, e.g. `StatTooltips`): "Rod: …  Binding: …" and the smithing bonus; `viewer/StationSources.java` Tool Station recipes: binding and rod slots list all allowed parts; `client/viewer/RecipeViewScreen.java`: on Tool Station pages the rod slot only cycles rods allowed for the head shown, and the page shows the same stats box as the Tool Station (share the code with Task 4).
- [ ] Extend `ViewerTest`: the bronze pickaxe recipe's rod slot options contain Bronze Tool Rod and not Steel Tool Rod. Shot `recipes_bronze_pickaxe` updated. Commit "Dynamic tools: tooltip and recipe viewer".

### Task 7: Quest, docs, version, full suite

**Files:** one advancement/quest via `scripts/gen_advancements.py` ("A Better Haft": take a tool with a metal rod from the Tool Station; awarded from code), docs (`docs/ages/*` tool sections: parts and tiers table; registry notes for the Tool Station; `progression.json` one sentence sv+en), `docs/skills-and-tools-plan.md` (mark approach A, replace the old "one item per tool type" viewer notes), `gradle.properties` 0.225.0. Full suite once. Commit "Dynamic tools: quest, docs, v0.225.0".

### Task 8: Copper and bronze rods are cast (user answer)

Replace the 2-ingot crafting recipes from Task 2: a rod mold (unfired clay rod mold in Mold Bench → fired in the kiln, like `Casting.UNFIRED_CLAY_*_MOLD`), cast with copper or bronze the way heads are cast (`Casting`, `PouringSpout`, molding flask or whatever the copper heads use), a rough cast rod ground on the Whetstone like copper heads, Poor Bronze inherited. Generator for mold/rough-rod textures in the style of the existing molds. Update registry notes, age docs, the recipe viewer sees the new station recipes automatically. Test: cast a copper rod end to end in a gametest (or the closest existing casting test pattern). Commit "Dynamic tools: copper and bronze rods are cast in a mold".

### Task 9: Dynamic bows and fishing rods (user answer)

Extend `ToolParts` with bow parts (string, grip, stave) and fishing-rod parts (hook = head, line, rod) per the spec addendum; stats: durability + draw power/time for bows (`CrudeBowItem`, `YewLongbowItem`, `SINEW_BOW` if it gets a station recipe), durability + lure time for rods (`BoneHookRodItem`, crude rod). Defaults reproduce today's stats. Tool Station screen gets the two types; looks via the same layered generator where the sprite allows (string/line recolour). Tests like Task 1/3. Commit "Dynamic tools: bows and fishing rods".

### Task 10: Persist the placed-block memory (skills follow-up)

`SkillHooks` PLACED → per-level SavedData (bounded as today), so player-placed ore/stone and fluid-made stone stay XP-free across restarts. Test: save/load round trip of the set. Commit "Skills: remember player-placed blocks across restarts".
