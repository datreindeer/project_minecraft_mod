# Recipe Viewer (R/U) Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Press R over any item in an inventory (or in a searchable item list beside it) to see how it is made, U to see what it is used for, showing only recipes the player has unlocked, in the mod's hide-and-bark style.

**Architecture:** A common recipe model (`ViewerRecipe`) covers every source: the JSON recipes (server `RecipeManager`, shipped to the client in one payload per datapack sync) and the Java station lists (identical on both sides, built locally). A synced player attachment `seen_items`, plus the existing synced `UNLOCKED_TOOLS`, lets the client decide unlocks itself, so no delta packets are needed. The client adds key handling, a recipe screen and a search panel; items made in the world get a text page from `registry_notes_en.json`, which is copied into resources at build time.

**Tech Stack:** NeoForge 26.3.0.95, MC 26.3, Java; attachments with `.sync`, `CustomPacketPayload`, GameTest (`TestArea.of`), `client/ui/HideUi`, `client/ui/GuiShots`.

**Spec:** `docs/superpowers/specs/2026-11-26-recipe-viewer-design.md`. Note: the spec says the server sends the *unlocked* recipes. This plan sends the whole JSON catalog once and lets the client filter with the synced `seen_items`. The player sees the same thing, with no delta protocol.

## Global Constraints

- New code lives in package `com.mechanical.viewer` (common) and `com.mechanical.client.viewer` (client only). Common code must never touch client classes, because the gametest server never loads client code.
- Unlock rules (spec, decision 5):
  - Tool Station / `StationRecipe`: unlocked iff `unlockedBy == null`, or `UNLOCKED_TOOLS` contains the recipe's `id()`. This is exactly what the station screens use.
  - JSON recipes: unlocked iff **every** input slot has at least one option in `seen_items`.
  - Kiln / Bloomery: unlocked iff any item matching the input predicate is in `seen_items`.
  - Vanilla's recipe book never counts.
- Note text: links to items not in `seen_items` render as `???` with no icon. A note page is visible iff its item, or any item it links to, is seen.
- The search panel lists only items that are seen or are outputs of unlocked recipes.
- Keys: `key.mechanical.recipe` = R, `key.mechanical.usage` = U, category INVENTORY, `KeyConflictContext.GUI`. They must not fire while a text box has focus.
- Style: `client/ui/HideUi` (`panel`, `inset`, `slot`, `row`, colours `NAME/LABEL/TEXT/DIM/GOLD`).
- Work in a worktree `feat/recipe-viewer`. Copy only `build/moddev/artifacts` from main. Run gametests with `./gradlew runGameTestServer --no-configuration-cache` and check "Game directory" in the log points to the worktree.
- Bump `mod_version` in `gradle.properties` from 0.222.0 to **0.223.0** at merge. Add lang keys to `assets/mechanical/lang/en_us.json`.
- The full gametest suite runs once at the end (Task 7), not after every task. Per-task test runs may use the full run if no filter is available, but only once per task.

## Review Focus

1. A tag ingredient that resolves to zero items. The recipe is dropped from the catalog, no crash. (Task 3 test: `emptyTagSkipped`.)
2. An item with neither a recipe nor a note. R shows "No known recipes" instead of nothing or a crash. (Task 5: shot `recipes_none`.)
3. Pressing R/U while typing (search box, anvil rename, sign, chat). The letter is typed and the viewer does not open. (Task 6: check in `onScreenKey`, verified by shot `search_typing`.)
4. Huge lists: U on Stick gives hundreds of recipes. Paging must work and the page count must be right. (Task 5: shot `recipes_stick_usage` on page 2.)
5. A slot with several options, only one of them seen. The slot counts as seen. (Task 2 test: `altOptionCountsAsSeen`.)

---

### Task 1: `seen_items` attachment, scanning and backfill

**Files:**
- Create: `src/main/java/com/mechanical/viewer/SeenItems.java`
- Create: `src/main/java/com/mechanical/viewer/ViewerTest.java` (gametest, grows in Tasks 2–4)
- Modify: `src/main/java/com/mechanical/Mechanical.java` (init line ~438: `com.mechanical.viewer.SeenItems.init(); com.mechanical.viewer.ViewerTest.init(modEventBus);`)

**Interfaces:**
- Produces:
  - `SeenItems.SEEN: Supplier<AttachmentType<List<String>>>`: item ids. Registered on `Mechanical.ATTACHMENTS`, serialized as `Codec.STRING.listOf().fieldOf("items")`, `.sync(ByteBufCodecs.STRING_UTF8.apply(ByteBufCodecs.list()))`, `.copyOnDeath()`. Same shape as `UNLOCKED_TOOLS`.
  - `SeenItems.BACKFILLED: Supplier<AttachmentType<Boolean>>`: default false, serialized, not synced, copyOnDeath.
  - `static Set<String> seen(Player p)`: an unmodifiable view.
  - `static void scan(Player p)`: adds every non-empty item in `getNonEquipmentItems()` plus equipment and accessory slots. Calls `setData` only if something was added.
  - `static void backfill(ServerPlayer p)`: once (guarded by `BACKFILLED`). Runs `scan`, adds the Ender Chest contents, and for each id in `UNLOCKED_TOOLS` adds the key part: Tool Station recipe → `head`; `StationRecipe` → the first part's item. Never the result.

- [ ] **Step 1: Write the failing test** in `ViewerTest` (pattern: `equipment/EquipmentTest`, `TestArea.of(env, 20, true)`), test id `mechanical:viewer`. Body `seenItems(h)`:
  - mock player; give `Items.FLINT`; `SeenItems.scan(p)` → `seen(p)` contains `"minecraft:flint"`, does not contain `"minecraft:iron_ingot"`.
  - set `UNLOCKED_TOOLS` = `["mechanical:bronze_pickaxe"]`, put a Diamond in the Ender Chest, `backfill(p)` → seen contains `mechanical:bronze_pickaxe_head` and `minecraft:diamond`, and does **not** contain `mechanical:bronze_pickaxe`.
  - a second `backfill` after clearing the Ender Chest leaves seen unchanged (the guard works).
- [ ] **Step 2: Run** `./gradlew runGameTestServer --no-configuration-cache`. Expected: compile failure / `viewer` fails.
- [ ] **Step 3: Implement** `SeenItems`. Add a `PlayerTickEvent.Post` server handler at `tickCount % 20 == 5` that calls `scan`, and a `PlayerLoggedInEvent` handler that calls `backfill`.
- [ ] **Step 4: Run again.** Expected: `viewer` passes.
- [ ] **Step 5: Commit** `SeenItems.java ViewerTest.java Mechanical.java` with the message "Recipe viewer: seen items per player".

### Task 2: Common recipe model, Java station sources, unlock rule

**Files:**
- Create: `src/main/java/com/mechanical/viewer/ViewerRecipe.java`
- Create: `src/main/java/com/mechanical/viewer/StationSources.java`
- Modify: `ViewerTest.java`

**Interfaces:**
- Consumes: `SeenItems.seen`, `Mechanical.UNLOCKED_TOOLS`.
- Produces:
  - `enum Station { CRAFTING_SHAPED, CRAFTING_SHAPELESS, STONECUTTING, CAMPFIRE, SMOKING, SMELTING, TOOL_STATION, FLETCHING, ARMOR_STATION, LEATHERWORKING, MOLD_BENCH, PATTERN_BENCH, KILN, BLOOMERY }`. Each has `ItemStack icon()` (the station's block item; crafting = Crafting Table) and a lang key `viewer.mechanical.station.<lowercase>`.
  - `record Slot(List<Item> options, int count, boolean tool)`
  - `enum UnlockKind { ALWAYS, RECIPE_ID, ALL_SEEN, ANY_SEEN }`
  - `record ViewerRecipe(Station station, List<Slot> inputs, ItemStack output, int width, UnlockKind unlock, String unlockId)`. `width` is the shaped grid width (0 otherwise). `unlockId` is used by `RECIPE_ID`, otherwise `""`. Includes `static final StreamCodec<RegistryFriendlyByteBuf, ViewerRecipe> CODEC`.
  - `boolean unlocked(Set<String> seen, List<String> unlockedTools)`:
    - ALWAYS → true
    - RECIPE_ID → `unlockedTools.contains(unlockId)`
    - ALL_SEEN → every non-empty slot has an option in `seen`
    - ANY_SEEN → any option of `inputs.get(0)` is in `seen`
  - `boolean makes(Item i)`, `boolean uses(Item i)`.
  - `StationSources.all(): List<ViewerRecipe>`, built lazily once. Sources:
    - `ToolStationRecipes.ALL`: slots head/binding/rod, with Hide Glue as a second option on the binding when `gluable()`. Unlock RECIPE_ID with `id()`.
    - `FletchingRecipes.ALL`, `ArmorStationRecipes.ALL`, `LeatherworkingRecipes.ALL`, `MoldBench.ALL`, `PatternBenchBlock.RECIPES`: a `Part` becomes a slot whose options are the tag's items, or `item` + `alt`. `tool` is carried over. Unlock = `unlockedBy == null ? ALWAYS : RECIPE_ID(id())`.
    - `KilnBlockEntity.RECIPES`, `BloomeryBlockEntity.RECIPES`: input options = every item in `BuiltInRegistries.ITEM` whose default stack passes `input()`, with the recipe's `count`. Unlock ANY_SEEN. A recipe with zero matching items is dropped.

- [ ] **Step 1: Write the failing tests** in `ViewerTest`:
  - `stationSources`: `StationSources.all()` has at least one recipe for each of TOOL_STATION, FLETCHING, ARMOR_STATION, LEATHERWORKING, MOLD_BENCH, PATTERN_BENCH, KILN, BLOOMERY.
  - `toolStationKeyPart`: take the bronze pickaxe recipe. With `seen = {cordage, tool_rod}` and `unlockedTools = []` it is not unlocked. With `unlockedTools = ["mechanical:bronze_pickaxe"]` it is.
  - `altOptionCountsAsSeen`: an ALL_SEEN recipe with slot options `[A, B]`, with only B seen, is unlocked.
  - `codecRoundTrip`: encode and decode a `ViewerRecipe` from the Tool Station through `CODEC` (using a `RegistryFriendlyByteBuf` over `h.getLevel().registryAccess()`). The result is equal.
- [ ] **Step 2: Run.** Expected: fails (classes missing).
- [ ] **Step 3: Implement** `ViewerRecipe`, `Station` and `StationSources` as above.
- [ ] **Step 4: Run.** Expected: `viewer` passes.
- [ ] **Step 5: Commit** with the message "Recipe viewer: one recipe model for every station".

### Task 3: JSON recipes → catalog payload → client store

**Files:**
- Create: `src/main/java/com/mechanical/viewer/JsonSources.java`
- Create: `src/main/java/com/mechanical/viewer/CatalogPayload.java`
- Create: `src/main/java/com/mechanical/client/viewer/ClientCatalog.java`
- Modify: `ViewerTest.java`

**Interfaces:**
- Consumes: `ViewerRecipe`, `Station`, `Slot`, `StationSources.all()`.
- Produces:
  - `JsonSources.from(RecipeManager rm, HolderLookup.Provider regs): List<ViewerRecipe>`. Covers the six types: shaped (with `width` and empty slots kept as `Slot(List.of(),0,false)`), shapeless, stonecutting, campfire_cooking, smoking, smelting. Each `Ingredient` → its item options. A recipe with a non-empty ingredient that has zero items is skipped. Unlock ALL_SEEN.
  - `record CatalogPayload(List<ViewerRecipe> recipes)`, sent playToClient, registered via `RegisterPayloadHandlersEvent` (pattern: `equipment/OpenPackPayload`, registrar `"1"`). Sent from `OnDatapackSyncEvent`, to the joining player or to all players on reload.
  - `ClientCatalog`, client only:
    - `static void set(List<ViewerRecipe> json)`
    - `static List<ViewerRecipe> recipesFor(Item i)`
    - `static List<ViewerRecipe> usagesOf(Item i)`
    Both lists are filtered by `unlocked(SeenItems.seen(player), player.getData(UNLOCKED_TOOLS))`, sorted by station order, then output name. Each draws from json + `StationSources.all()`.
    - `static List<Item> searchable()`: seen items ∪ outputs of unlocked recipes, in registry order.

- [ ] **Step 1: Write the failing tests:**
  - `jsonSources`: on `h.getLevel().getServer().getRecipeManager()`, `JsonSources.from` contains a CRAFTING_SHAPELESS recipe whose output is `mechanical:twine`, with three slots that each have `mechanical:plant_fibre` as an option.
  - `emptyTagSkipped`: build a shapeless recipe with an empty `Ingredient` from an empty tag and pass it through the converter (`JsonSources.convert(RecipeHolder<?>)`, package-private). The result is empty, no exception.
- [ ] **Step 2: Run.** Expected: fails.
- [ ] **Step 3: Implement** `JsonSources` (with `convert` exposed package-private for the test), `CatalogPayload` and `ClientCatalog`.
- [ ] **Step 4: Run.** Expected: passes. Also run `./gradlew runClient` once, join a world, and check the log has no payload errors. Log the catalog size once at debug level.
- [ ] **Step 5: Commit** with the message "Recipe viewer: JSON recipes sent to the client".

### Task 4: Notes as a resource and the note parser

**Files:**
- Modify: `build.gradle`. In `processResources`: `from('docs/registry_notes_en.json') { into 'assets/mechanical'; rename { 'recipe_notes.json' } }`.
- Create: `src/main/java/com/mechanical/viewer/Note.java` (common, pure parsing)
- Create: `src/main/java/com/mechanical/client/viewer/ClientNotes.java` (loads the resource via a reload listener)
- Modify: `ViewerTest.java`

**Interfaces:**
- Produces:
  - `record Note.Piece(String text, String itemId, boolean bold)`. Exactly one of `text` / `itemId` is set.
  - `static List<Note.Piece> Note.parse(String raw)`: `[[id]]` → item piece, where an id without a namespace gets `mechanical:`. `**x**` → bold text. `\n` is kept in text.
  - `static Set<String> Note.links(String raw)`.
  - `ClientNotes.get(Item i): Optional<String>`, which skips keys starting with `_`. `ClientNotes.visible(Item i, Set<String> seen)` follows the spec rule.

- [ ] **Step 1: Write the failing test** `noteParse`: `Note.parse("3 [[plant_fibre]] (**shapeless**)")` gives exactly the pieces `text "3 "`, `item "mechanical:plant_fibre"`, `text " ("`, `bold "shapeless"`, `text ")"`. `links` of `"[[a]] and [[minecraft:stick]]"` = `{mechanical:a, minecraft:stick}`.
- [ ] **Step 2: Run.** Expected: fails.
- [ ] **Step 3: Implement** `Note`, the gradle copy and `ClientNotes`.
- [ ] **Step 4: Run.** Expected: passes. `./gradlew processResources` shows `build/resources/main/assets/mechanical/recipe_notes.json`.
- [ ] **Step 5: Commit** with the message "Recipe viewer: item notes as a resource".

### Task 5: Recipe screen

**Files:**
- Create: `src/main/java/com/mechanical/client/viewer/RecipeViewScreen.java`
- Modify: `src/main/resources/assets/mechanical/lang/en_us.json`
- Modify: `src/main/java/com/mechanical/client/ui/GuiShots.java`

**Interfaces:**
- Consumes: `ClientCatalog`, `ClientNotes`, `Note`, `HideUi`, `Station.icon()`.
- Produces: `RecipeViewScreen.open(Item item, boolean usage)`. The screen opens over the current screen and keeps it as `parent`. History is a stack of `(item, usage, tab, page)`.

Behaviour:
- Tabs across the top, one per station present in the list, with the station icon and a tooltip naming it.
- One recipe per page, laid out by station. Grid stations use 3×3 (shaped respects `width`). Tool Station / StationRecipe stations use 3 labelled slots (Head / Binding / Rod, or the part names). One-to-one stations use input → output (Kiln/Bloomery show `ticks` and `temp`).
- A slot with several options cycles every 20 ticks. A `tool` slot shows "(tool, not used up)".
- On R, if a note exists and is visible, a final tab "How to get it" shows the parsed note. Item pieces are drawn as icon + name, or `???` if unseen, and are clickable.
- Click an item: left = R, right = U, and push the history. Backspace = back one step. Esc = back, or close to `parent` when the history is empty. Arrow keys and buttons page through, with "page x / y".
- Empty result: the text `viewer.mechanical.none` = "No known recipes".
- Lang keys: `viewer.mechanical.recipes` "Recipes: %s", `viewer.mechanical.usages` "Used in: %s", `viewer.mechanical.how_to_get` "How to get it", `viewer.mechanical.none`, `viewer.mechanical.tool` "tool, not used up", `viewer.mechanical.page` "%s / %s", and one `viewer.mechanical.station.*` per station.

- [ ] **Step 1: Add GuiShots entries** (`inWorld`, which gives the items to the server player first so they count as seen):
  - `recipes_twine` (R on Twine)
  - `recipes_bronze_pickaxe` (R, with the head held so it is unlocked)
  - `recipes_stick_usage` (U on Stick, paged to 2)
  - `recipes_note` (R on Plant Fibre: the note tab)
  - `recipes_none` (R on an item without recipe or note, e.g. `minecraft:bedrock`)
- [ ] **Step 2: Implement** `RecipeViewScreen`.
- [ ] **Step 3: Run the shots.** Create `run/guishots.flag` with an output folder and the prefix `recipes_`, then run `./gradlew runClient --no-configuration-cache`. Expected: five PNGs. Inspect each closely: slots aligned, nothing clipped, the HideUi look, the page number right on the Stick shot, `???` on unseen links in the note shot.
- [ ] **Step 4: Commit** with the message "Recipe viewer: the recipe screen".

### Task 6: Keys, hover and the search panel

**Files:**
- Create: `src/main/java/com/mechanical/client/viewer/RecipeViewerClient.java`
- Create: `src/main/java/com/mechanical/client/viewer/SearchPanel.java`
- Modify: `en_us.json` (`key.mechanical.recipe` "Show Recipes", `key.mechanical.usage` "Show Uses", `viewer.mechanical.search` "Search…")
- Modify: `GuiShots.java`

**Interfaces:**
- Consumes: `RecipeViewScreen.open`, `ClientCatalog.searchable()`.
- Produces: `RecipeViewerClient.RECIPE` and `RecipeViewerClient.USAGE` (KeyMappings, `KeyConflictContext.GUI`), registered in `RegisterKeyMappingsEvent`.

Behaviour:
- `ScreenEvent.KeyPressed.Pre`. Do nothing if the screen's focused widget is an `EditBox` (including the search box). On an `AbstractContainerScreen`, use the hovered slot's item if any, else the search panel's hovered item. On a `RecipeViewScreen`, use the hovered recipe item. Open, then cancel the event.
- Search panel on every `AbstractContainerScreen` except `CreativeModeInventoryScreen`, to the right of the GUI. It is added in `ScreenEvent.Init.Post`, drawn in `Render.Post`, and gets clicks in `MouseButtonPressed.Pre`. It has an EditBox at the top, a grid of 18 px cells filling the free height, and page arrows. It filters by display name, case-insensitive. Left click = R, right click = U. It is not shown when the free width is less than 4 cells. The search text is kept between screens for the session.

- [ ] **Step 1: Add GuiShots entries:**
  - `search_panel`: the inventory open with the panel and a search text "bronze".
  - `search_typing`: the search box focused, with the "r" key sent. The box then contains "r" and no RecipeViewScreen is open.
  - `recipes_hover`: R over an inventory slot holding Twine, which opens the Twine recipe.
- [ ] **Step 2: Implement** `RecipeViewerClient` and `SearchPanel`.
- [ ] **Step 3: Run the shots** (prefixes `search_,recipes_hover`). Expected: three PNGs that match. Also check by hand in `runClient` that R in the world still only does the downed give-up (no viewer), and that the panel does not overlap the vanilla recipe-book button in the inventory (`InventoryScreenMixin`).
- [ ] **Step 4: Commit** with the message "Recipe viewer: R/U keys and the search panel".

### Task 7: Finish: docs, version, full suite, merge

**Files:**
- Modify: `gradle.properties` (`mod_version=0.223.0`)
- Modify: `docs/progression.json`, the Stone Age intro (sv + en): one sentence that R/U over an item shows its recipes and uses.

- [ ] **Step 1:** Run the full suite once: `./gradlew runGameTestServer --no-configuration-cache`. Expected: all tests pass, including `viewer`. Known flaky tests (`elkwight_world`, `living_world_blocks`) are re-run once before treating them as failures.
- [ ] **Step 2:** Bump the version and update `progression.json`.
- [ ] **Step 3:** Commit the remaining files. Merge `feat/recipe-viewer` into main, staging files by name and never with `git add -A` (other sessions share main). Push.
- [ ] **Step 4:** Send the shots from Tasks 5–6 to the user with SendUserFile.
