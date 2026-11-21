# Järnåldern

> Status: **planering**. Byggt: **Bog Iron** (v0.80.0), **de nya malmerna** Galena, guld, Limestone och Rock Salt
> (2026-11-21). Resten är förslag (2026-11-20).

Tredje åldern. Bronsåldern lämnar över: **Bloomery** (Fire Brick, 1×1×3-schakt, 1350° med Bellows), **Bronze Anvil**,
**Bronze Smithing Hammer**, **Wrought Iron Ingot**, Bronze Pickaxe (bryter Iron, Coal och Lapis Ore) och Charcoal.
Se *Bloomery* i [02-bronze-age.md](02-bronze-age.md).

Fortfarande spärrat i början av järnåldern: järnverktyg och järnrustning, vanillas Iron Ingot, och Gold, Redstone,
Diamond och Emerald Ore (taggen `#minecraft:needs_iron_tool`).

Åldern slutar med **Steel**: de bästa verktygen före ångåldern, och det material som ångålderns maskiner byggs av.

## Starkt rekommenderat

### 1. Bog Iron (myrmalm) (byggd, v0.80.0)

Den nordiska järnålderns huvudsakliga järnkälla, och järnålderns huvudväg till järn. Iron Ore (Bronze Pickaxe) är den
rikare uppgraderingen. Kod: `Ironworking` (block, item, gametest), `BloomeryBlockEntity` (receptet). Data:
`scripts/gen_bog_iron.py` (efter gen_bloomery.py), texturer: `scripts/gen_bog_iron_textures.py`.

| Vad | Hur |
|---|---|
| **Bog Iron Ore** | Block. Rostbruna klumpar i mörk lera. Hårdhet 0,8, ljud som Mud. **Grävs med vilken spade som helst** (Flint Shovel räcker); hacka eller händer ger inget (taggen `minecraft:mineable/shovel` + requiresCorrectToolForDrops). Silk Touch ger blocket. |
| **Raw Bog Iron** | 1 st per block (Fortune hjälper, vanillas ore_drops). Smälts **bara i Bloomery**: **6 Raw Bog Iron → Hot Iron Bloom** vid 1350° (Raw Iron: 4). Går inte i Clay Kiln, Crucible eller vanillaugnar. |

**Var den finns** (feature `mechanical:bog_iron`, en `minecraft:disk` med radie 1–2 och half_height 1):

- Centrum 2 block under botten (heightmap `OCEAN_FLOOR_WG`), så skivan täcker de **översta 3 lagren**, och bara där
  **vatten står ovanför**. Den ersätter Mud, Clay, Dirt, Grass Block, Coarse Dirt, Sand och Gravel. Syns alltså som
  rostfläckar i botten av gölar.
- **Willow Fen, Swamp, Mangrove Swamp** (taggen `mechanical:has_bog_iron`): 2 försök per chunk (testat: ~300–450 block på 81×81) (`bog_iron_fen`).
- **River** (taggen `mechanical:has_river_bog_iron`): 2 försök i var 6:e chunk (`bog_iron_river`).
- Läggs i steget `underground_ores`, efter Willow Fens lerfläckar (`willow_fen_mud`), så den kan ligga i leran.

**Advancement:** *Bog Iron* (Bronze Age-fliken, efter *Toward Iron*): "Dig rusty Bog Iron Ore with a shovel from the
floors of fen and swamp pools". *Iron Bloom* nämner nu "4 Raw Iron or 6 Raw Bog Iron".

### 2. Rostning före Bloomery

- Raw Iron och Raw Bog Iron rostas på en **rostbädd** (vedbädd eller eldgrop) → **Roasted Iron Ore**.
- Bara rostad malm ger en full lupp. Orostad malm ger en mindre lupp eller mest slagg (beslut kvar).
- Historiskt riktigt: rostningen driver ut vatten och svavel.

### 3. Kolmila (charcoal pile)

- Bloomery bränner 2 minuter per Charcoal och slukar kol, så kilnens kol räcker inte.
- **Kolmila:** staplad ved (vedtravarna finns redan) täckt med jord eller torv, tänds och pyr i flera minuter → mycket
  Charcoal. Går den sönder eller får för mycket luft brinner veden upp till aska.
- Passar ihop med trädfällningen och vedtravarna.

### 4. Steel (åldersslutet)

- **Wrought Iron + Charcoal** i en degel eller ett härdningsrör (cementering) → **Steel**.
- Steel smids på städ till de bästa verktygen och full plåtrustning (bröstplåten kommer med järnet, se bronsålderns
  rustning).
- Texturer finns redan i texturbiblioteket: `crude_steel_ingot`, `crude_steel_ingot_hot`, `steel_ingot`,
  `steel_bloom`, `steel_plate`, `steel_dust`.

## Nya malmer (byggda, 2026-11-21)

Alla fyra hör till järnåldern. Kod: `IronOres` (block, items, gametest `iron_ores`), kilnrecepten i
`KilnBlockEntity.RECIPES`, prägling i `Coins.minted`, krossning av kalksten via `BronzeOres.crushed`. Data:
`scripts/gen_iron_ores.py` (efter gen_gemstones.py, före gen_block_items.py), texturer:
`scripts/gen_iron_ores_textures.py` (Galena-, bly- och silvertexturerna är silvret ur texturbiblioteket, omfärgat).

**Advancements** (Iron Age-fliken, under *Iron Age*): *Galena* → *Lead* → *Cupellation* → *Sterling* (Silver Coin);
*Gold* → *Treasury* (Gold Coin); *Flux*; *Salt of the Earth* → *Salt Cured*.

### 5. Galena (blyglans): bly och silver

| Vad | Hur |
|---|---|
| **Galena Ore**, **Deepslate Galena Ore** | Ådror (storlek 8) i sten och deepslate i hela Overworld: 8 försök per chunk, y −48 till 80 (trapets, flest kring y 16). Lite ovanligare än järn. **Kräver Bronze Pickaxe** (`mechanical:needs_bronze_tool`, som Iron Ore). Hårdhet 3 / 4,5. |
| **Raw Galena** | 1 per block (Fortune hjälper), blocket med Silk Touch. |
| **Lead Ingot** | Raw Galena i **Clay Kiln vid 800°**, 20 s. 800° är Charcoals naturliga max, så bly går **utan Bellows**. |
| **Leaded Cupel** | Lead Ingot + Bone Meal (formlöst): en kupell av benaska med blyet i. |
| **Silver Nugget** | Leaded Cupel i **Clay Kiln vid 1000°** (Charcoal + Bellows), 30 s → 1 Silver Nugget. **Kupellering**: blyet sugs upp av benaskan och går åt. |
| **Silver Ingot** | 9 Silver Nugget (3×3), och tillbaka till 9. |

Ett block Galena ger alltså ett bly *eller* en niondels silvertacka: blyet sparas till ång- och elåldern eller offras
för silver. Inte i Bloomery (den kod-delen lämnades orörd).

### 6. Guld

| Vad | Hur |
|---|---|
| **Gold Ore** | Oförändrad: kräver fortfarande järnhacka (`#minecraft:needs_iron_tool`). Ger vanillas Raw Gold. |
| **Vaskning** | Gold Nugget i Pan-tabellerna (`gameplay/panning/gravel` och `sand`): vikt 1 i grundpoolen (ungefär 1 på 85 vaskningar i grus, 1 på 75 i sand) och vikt 1 i extrapoolen vid River, Beach, Stony Shore m.fl. (ungefär 1 på 13 där). |
| **Gold Ingot** | **Raw Gold** eller **9 Gold Nugget** i **Clay Kiln vid 1100°** (Charcoal + Bellows), 20 s. |
| **Avstängt** | Vanillas guldsmältning i ugn och masugn (Raw Gold, Gold Ore, Deepslate och Nether Gold Ore) och receptet 9 Gold Nugget → Gold Ingot. Gold Ingot → 9 Nugget finns kvar. |

### 7. Limestone (kalksten) och Flux

| Vad | Hur |
|---|---|
| **Limestone** | Stenblock, krämvit. Stora klumpar (storlek 64) som granit: 2 per chunk på y 0–60, en i var 6:e chunk på y 64–128 (syns i sluttningar), och 2 extra per chunk på y 56–160 i Stony Shore, Windswept Hills/Gravelly Hills/Forest, Meadow och Savanna Plateau (`mechanical:has_limestone_cliffs`). **Vilken hacka som helst**, ger sig själv. Ligger i `base_stone_overworld` och `stone_ore_replaceables`, så grottor skär igenom den och malm (även Galena) kan ligga i den. |
| **Flux** | Limestone krossad på **Stone Anvil** → **2 Flux**. Item-taggen **`mechanical:flux`**. |

**Inte gjort här:** Flux gör ännu ingenting i Bloomery. Tanken är mer järn och mindre slagg; Bloomery kopplas in
separat (läs taggen `mechanical:flux`).

### 8. Salt

| Vad | Hur |
|---|---|
| **Rock Salt** | Block, rosavitt. Linser (storlek 24) i berget under torra biomer: Desert, Badlands, Eroded och Wooded Badlands, Savanna, Savanna Plateau och Windswept Savanna (`mechanical:has_rock_salt`), 4 per chunk, y 0–72. **Kräver Bronze Pickaxe** (mjuk sten, men så hamnar saltet i järnåldern). Ger **2–3 Salt** (Fortune hjälper), blocket med Silk Touch. |
| **Salted Meat** | Rått kött (taggen `mechanical:smokable_meat`) + Salt, formlöst → 1. **Ruttnar aldrig** (ingen `perishes_*`-tagg). 3 mat, mättnad 0,6 (rökt kött 3 / 0,8 men 30 dagar). |
| **Salted Fish** | Rå fisk (`mechanical:smokable_fish`) + Salt → 1. Ruttnar aldrig. 2 mat, mättnad 0,6. |
| **Saltad hud** | Scraped Hide + Salt, formlöst → **Leather** direkt, utan Drying Rack eller Bronze Kettle. |

Valt så: saltet är en snabbväg som kostar en resurs (ett salt per kött eller hud), medan rökning och torkning är
gratis men tar tid och plats. Stora styckningsdelar ger bara 1 Salted Meat; skär dem till strimlor först. Saltsjöar
och saltskorpa vid stränder byggdes inte (bara bergsalt).

## Vänta med

- **Zinc** (mässing), **Nickel**, **Redstone**, **Sulfur**: ång- och elåldern.
- **Coal → Coked Coal**: ångåldern (texturer `coked_coal`, `coal_dust` finns).

## Mynt: Silver Coin och Gold Coin

Pengarna finns redan (`Coins.java`): **Copper Coin**, **Silver Coin** och **Gold Coin**, 1 Silver = 10 Copper,
1 Gold = 10 Silver. I bronsåldern går bara Copper Coin att prägla (Copper Ingot, 4 slag på Bronze Anvil → 4 Copper Coin).

- **Silver Coin (byggd):** en kall **Silver Ingot**, 4 slag på Bronze Anvil → 4 Silver Coin.
- **Gold Coin (byggd):** en kall **Gold Ingot**, 4 slag → 4 Gold Coin.
- Samma regler som kopparmyntet (`Coins.MINT_HITS`, `Coins.COINS_PER_INGOT`); vilket mynt en tacka ger avgör
  `Coins.minted`. `BronzeAnvilBlockEntity.mint` släpper nu `Coins.minted(item)` i stället för fasta Copper Coin.
- Handelsnivå 5 (tom nu) kan använda Silver Coin.

## Beslut (2026-11-20)

1. **Bog Iron är huvudvägen** till järn i början av åldern. Iron Ore (Bronze Pickaxe) är uppgraderingen: rikare men
   djupare och svårare.
2. **Inget görs direkt av en järntacka.** Som med bronset ska allt gå genom egna steg. Wrought Iron Ingot förblir
   ett eget item, och vanillas Iron Ingot används inte i några recept.
   - **Järn smids, det gjuts inte.** Brons gjuts i lerformar, men järn går inte att smälta med den här tekniken.
     Därför värms tackan till smideshetta (1100°, Clay Kiln eller Bloomery) och hamras på **Bronze Anvil** med
     Bronze Smithing Hammer medan den är het, på samma sätt som luppen.
   - Hamrad till **delar**: Iron Plate, Iron Tool Rod, verktygshuvuden (texturer finns: `iron_plate`,
     `crude_iron_plate`, `iron_tool_rod`). Delarna sätts sedan ihop i Tool Station och Armor Station.
   - Vanillas järnsaker (hink, sax, städ, kittel, räls m.m.) får egna recept av dessa delar, och vanillas
     Iron Ingot-recept stängs av.

## Öppna beslut

1. Rostning: blir orostad malm helt oanvändbar, eller bara sämre?
2. ~~Vilka av de nya malmerna hör till järnåldern?~~ Beslutat 2026-11-21: alla fyra (byggda, se *Nya malmer*).
3. Kallnar en het tacka mitt i smidet (som luppen), och hur många slag kräver en plåt eller ett huvud?

## Items och block (planerade)

| Namn | Typ | Kommentar |
|---|---|---|
| Bog Iron Ore | Block | **Byggd.** Grävs med spade i Willow Fen, Swamp, vid åar |
| Raw Bog Iron | Item | **Byggd.** 6 per lupp i Bloomery |
| Roasted Iron Ore | Item | Rostad Raw Iron / Raw Bog Iron, går i Bloomery |
| Charcoal Pile (kolmila) | Struktur/block | Ved + jord → mycket Charcoal |
| Hot Wrought Iron Ingot | Item | Tackan vid smideshetta, hamras på Bronze Anvil |
| Iron Plate, Iron Tool Rod, järnhuvuden | Items | Smidda delar till verktyg, rustning och vanillas järnsaker |
| Steel Ingot | Item | Wrought Iron + Charcoal, cementering |
| Galena Ore, Deepslate Galena Ore | Block | **Byggd.** Bronze Pickaxe, ger Raw Galena |
| Raw Galena, Lead Ingot | Items | **Byggd.** Kiln 800° |
| Leaded Cupel, Silver Nugget, Silver Ingot | Items | **Byggd.** Kupellering vid 1000°, 9 nuggets = 1 tacka |
| Limestone, Flux | Block/Item | **Byggd.** Stone Anvil: 1 → 2 Flux, taggen `mechanical:flux` |
| Rock Salt, Salt | Block/Item | **Byggd.** Bronze Pickaxe, 2–3 Salt |
| Salted Meat, Salted Fish | Items | **Byggd.** Ruttnar aldrig |

## Borttaget eller spärrat från vanilla

Vanillas guldsmältning i ugn och masugn och receptet 9 Gold Nugget → Gold Ingot (guld smälts i Clay Kiln vid 1100°).
Spärrat sedan tidigare: järnverktyg, järnrustning, vanillas järnugnsrecept (Raw Iron smälts bara i
Bloomery), Gold/Redstone/Diamond/Emerald Ore (kräver järnhacka).

## NPC-byar i järnåldern (byggd)

Järnålderns material är för tunna för en egen byggstil, så NPC-byar i järnåldern är bronsålderns med små uppgraderingar
(`people/AgeRebuild.java`, `TribeVillageStructure` med ålder 2): långhuset en kurs till (två skift sten i foten),
palissaden en stock högre, och en smedja med Bronze Anvil och Furnace Bricks bredvid Clay Kiln och Quern. Lägren vid
spelarläger är desamma som i bronsåldern. Advancement **Iron Age Neighbours** (i bronsålderns flik, där Wrought Iron ligger).

