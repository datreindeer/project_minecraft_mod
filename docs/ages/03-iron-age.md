# Järnåldern

> Status: **planering**. Byggt: **Bog Iron** (v0.79.0). Resten är förslag (2026-11-20).

Tredje åldern. Bronsåldern lämnar över: **Bloomery** (Fire Brick, 1×1×3-schakt, 1350° med Bellows), **Bronze Anvil**,
**Bronze Smithing Hammer**, **Wrought Iron Ingot**, Bronze Pickaxe (bryter Iron, Coal och Lapis Ore) och Charcoal.
Se *Bloomery* i [02-bronze-age.md](02-bronze-age.md).

Fortfarande spärrat i början av järnåldern: järnverktyg och järnrustning, vanillas Iron Ingot, och Gold, Redstone,
Diamond och Emerald Ore (taggen `#minecraft:needs_iron_tool`).

Åldern slutar med **Steel**: de bästa verktygen före ångåldern, och det material som ångålderns maskiner byggs av.

## Starkt rekommenderat

### 1. Bog Iron (myrmalm) (byggd, v0.79.0)

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

## Nya malmer att överväga

| Malm | Ger | Varför |
|---|---|---|
| **Galena** (blyglans) | Lead + lite Silver | Silver till ringar och halsband (utrustningsplatserna). Bly till rör och batterier i ång- och elåldern. Texturer: `raw_silver_ore`, `silver_bloom`, `silver_ingot`, `silver_plate`, `silver_dust`. |
| **Gold**: vaskat guld och Gold Ore | Gold Nugget | Redan spärrat bakom järnhacka. Vaskning finns (`PanItem`), så det räcker med en ny drop-pool. Smycken och offergåvor. Texturer: `gold_bloom`, `gold_plate`, `gold_dust`. |
| **Limestone** | Flux | Kalksten i Bloomery ger mer järn och mindre slagg. Behövs senare till masugnen i ångåldern. |
| **Salt** (saltsjöar eller strandbäddar) | Salt | Saltar kött och garvar skinn: kopplar till Butchery och läder. |

## Vänta med

- **Zinc** (mässing), **Nickel**, **Redstone**, **Sulfur**: ång- och elåldern.
- **Coal → Coked Coal**: ångåldern (texturer `coked_coal`, `coal_dust` finns).

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
2. Vilka av de nya malmerna (Galena, Gold, Limestone, Salt) hör till järnåldern, och vilka väntar?
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
| Galena, Lead Ingot, Silver Ingot | Items | Om Galena kommer med |
| Limestone, Flux | Block/Item | Om Limestone kommer med |
| Salt | Item | Om Salt kommer med |

## Borttaget eller spärrat från vanilla

Inget nytt i järnåldern ännu. Spärrat sedan tidigare: järnverktyg, järnrustning, vanillas järnugnsrecept (Raw Iron smälts bara i
Bloomery), Gold/Redstone/Diamond/Emerald Ore (kräver järnhacka).
