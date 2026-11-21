# Järnåldern

> Status: **byggs** (2026-11-21). Byggt: **Bog Iron** (v0.80.0), **rostning och flux**, **smide, järnverktyg och järnrustning**, **kolmila**, **stål**,
> **de nya malmerna** Galena, guld, Limestone och Rock Salt. Resten är förslag.

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

### 2. Rostning före Bloomery (byggd)

Rostningen driver ut vatten och svavel ur malmen. Kod: `Ironworking`, `BloomeryBlockEntity`, `BronzeAnvilBlockEntity`.
Data: `scripts/gen_iron_age.py`. Test: `IronAgeTest`.

| Vad | Hur |
|---|---|
| **Roasted Iron Ore** | Raw Iron rostad på **Campfire eller Fire Pit** (campfire-recept, 30 s). |
| **Roasted Bog Iron** | Raw Bog Iron rostad likadant. |

- En lupp är **3 delar** järn och slagg. **Rostad malm** (4 Roasted Iron Ore eller 6 Roasted Bog Iron) ger en full lupp:
  **2 Wrought Iron + 1 Slag**. **Orostad malm** fungerar fortfarande men ger en fattig lupp: **1 Wrought Iron + 2 Slag**
  (beslut 2026-11-21: sämre, inte oanvändbar). Luppens tooltip visar vad den kommer att ge.
- **Flux:** högerklicka på Bloomery Hearth med flux (item-taggen `mechanical:flux`, krossad Limestone) för att lägga i
  en laddning, högst **4**. Varje lupp som smälts av malm tar en laddning och ger **en Wrought Iron mer och en Slag
  mindre** (rostad malm + flux = 3 Wrought Iron, ingen slagg).
- Luppar från före rostningen räknas som rostade (2 + 1).
- Advancement **Roasted** (Iron Age-fliken).

### Smide av järn (byggd)

Järn smids, det gjuts inte (beslut 2026-11-20). Kod: `Smithing` (items, mönster, stålverktyg), `HotMetalItem` (het
metall), `BronzeAnvilBlockEntity` (smidet), `BloomeryBlockEntity` / `KilnBlockEntity` (värmning),
`ToolStationRecipes`, `ArmorStationRecipes`. Data: `scripts/gen_iron_age.py`. Test: `IronAgeTest`.

- **Värm:** en Wrought Iron Ingot i **Bloomery eller Clay Kiln** vid **1100°** (10 s) blir en **Hot Wrought Iron Ingot**.
  Den håller hettan **30 s** (stapeln visar hur mycket som är kvar; den hålls varm så länge den ligger kvar i en het
  ugn), bränner en bar hand som en het lupp (Bronze Tongs i andra handen) och blir sedan en kall tacka igen. Slagen som
  hunnits sitter kvar, så den kan värmas igen och smidas klart.
- **Välj mönster:** smyg och högerklicka städet med hammaren. Mönstret står kvar på städet och visas när en tacka läggs på.
- **Smid:** hamra den heta tackan på **Bronze Anvil** (vänster- eller högerklick med Bronze eller Iron Smithing Hammer):

| Mönster | Tackor | Slag | Av järn | Av stål |
|---|---|---|---|---|
| Plate | 1 | 4 | Iron Plate | Steel Plate |
| Tool Rod | 1 | 3 | Iron Tool Rod | – |
| Pickaxe / Axe Head | 3 | 6 | Iron Pickaxe / Axe Head | Steel Pickaxe / Axe Head |
| Shovel Head | 1 | 5 | Iron Shovel Head | Steel Shovel Head |
| Hoe Head | 2 | 5 | Iron Hoe Head | Steel Hoe Head |
| Knife Blade | 1 | 5 | Iron Knife Blade | Steel Knife Blade |
| Sword Blade | 2 | 7 | Iron Sword Blade | Steel Sword Blade |
| Spear Head | 1 | 6 | Iron Spear Head | Steel Spear Head |
| Hammer Head | 2 | 6 | Iron Hammer Head | – |

- **Fler tackor:** stora delar kräver flera heta tackor av samma metall. Lägg den första på städet och högerklicka med
  fler heta tackor (högst 3); högen ritas tacka för tacka. Den första håller hettan och slagen. För få tackor: inga slag,
  meddelandet säger hur många som behövs. Blir det tackor över ligger de kvar på städet, heta och obearbetade; tom hand
  tar en i taget. (En järnhacka kostar därmed 3 tackor = 6 rostad Raw Iron, mot bronshackans 6 enheter ≈ 3 Raw Copper.)
- **Det syns på städet** (allt som görs där, även krossning, lupp, mynt och galler): varje slag trycker ihop biten som
  sedan fjädrar tillbaka, och ju längre arbetet kommit desto plattare och bredare ligger den. Bronze Smithing Hammer nöts 2 per slag på het metall, **Iron Smithing Hammer** (Iron Hammer Head +
  Leather + Tool Rod, 500 i hållbarhet) bara 1.

**Järnverktyg och rustning** (vanillas items, deras Iron Ingot-recept går inte att göra):

| Vad | Hur |
|---|---|
| Iron Pickaxe / Axe / Shovel / Hoe | Tool Station: huvud + Leather + Tool Rod |
| Iron Knife | Tool Station: Iron Knife Blade + Leather + Stick |
| Iron Sword | Tool Station: Iron Sword Blade + Cordage + Leather |
| Iron Spear | Tool Station: Iron Spear Head + Cordage + Tool Rod |
| Iron Helmet / Chestplate / Leggings / Boots | Armor Station: 3 / 6 / 5 / 2 Iron Plate + 2 / 4 / 3 / 2 Leather + hammare |
| Bucket | 3 Iron Plate (V) |
| Shears | 2 Iron Plate (diagonalt) |

- Smitt järn håller längre än brons: järnverktygen har **600** i hållbarhet (vanilla 250, brons 460) och Iron Pickaxe
  bryter guld, redstone, diamant och smaragd. Full järnrustning, bröstplåten som bronsåldern saknade.
- Fler vanilla-järnsaker av smidda delar: **Iron Bars** (6 Iron Tool Rod → 16), **Iron Chain** (3 Iron Tool Rod i rad
  → 3), **Cauldron** (7 Iron Plate), **Iron Door** (6 Iron Plate → 3), **Iron Trapdoor** (4 Iron Plate). Rail, Hopper,
  Anvil och andra redstone- och järnvägssaker kräver fortfarande Iron Ingot (ångåldern).
- Advancements: **Forged**, **Iron Hammer**, **Ironsmith**, **Iron Clad** (Iron Age-fliken).

### 3. Kolmila (charcoal clamp) (byggd)

Bloomery slukar kol, och Clay Kiln ger bara 1 Charcoal per 2 Firewood. Kolmilan ger dubbelt så mycket. Kod:
`CharcoalClamp` (milan, sparas i världsdata), `CharcoalHeapBlock`, `FireDrillItem`. Data: `scripts/gen_iron_age.py`.

- **Bygg:** stapla vedtravar (Firewood Pile och/eller Rough Log Pile, högst 64 block som rör varandra) och
  **täck dem** på alla sidor och ovanpå med jord (taggen `mechanical:clamp_cover`: all dirt, gräs och lera, sand,
  grus, Clay, Packed Mud) eller fast block som inte brinner (sten). Underlaget ska vara fast. **En sida lämnas öppen:
  rökhålet.**
- **Tänd:** högerklicka rökhålet (den öppna sidan av en trave) med **Fire Drill**. Den tar som en Campfire utan tinder
  (en gång på tre) och nöts 1 per försök. Är milan inte täckt säger den hur många sidor som är öppna.
- **Pyr i 5 minuter**, med rök ur rökhålet. Öppnas en sida (jorden grävs bort) brinner den traven upp på några
  sekunder och är borta. Milan väntar om chunken laddas ur, och den finns kvar efter omstart.
- **Kolhögar:** varje trave blir en **Charcoal Heap** i travens form (förkolnade vedträn): **1 Charcoal per Firewood,
  2 per Rough Log** (en full trave = 8). Bryt den med spade (släpper allt) eller ta en i taget med tom hand.
- Advancement **Charcoal Burner** (Iron Age-fliken). Test: `IronAgeTest`.

### 4. Steel (åldersslutet) (byggd)

Cementering (kol och sot vandrar in i järnet). Kod: `Smithing`, `BloomeryBlockEntity` / `KilnBlockEntity` (recepten).
Data: `scripts/gen_iron_age.py`. Test: `IronAgeTest`.

| Steg | Hur |
|---|---|
| **Unfired Cementation Pot** | 5 Fire Clay (F F / FFF) |
| **Cementation Pot** | Bränd i Clay Kiln vid 1100° |
| **Packed Cementation Pot** | Cementation Pot + 2 Wrought Iron Ingot + 2 Charcoal + 1 Soot (formlöst) |
| **Cemented Pot** | Packed pot i **Bloomery eller Clay Kiln** vid **1000°** i 3 min (snabbare hetare). Bloomery sjunker mot 800° utan bälg, så pumpa |
| **Blister Steel** | Cemented Pot (formlöst) → **2 Blister Steel** (krukan går sönder) |
| **Steel Ingot** | Blister Steel värmd till 1100° (Hot Blister Steel), **6 slag** på städet |

- Heta Steel Ingots smids efter städets mönster till **Steel Plate** och stålets huvuden och blad (se *Smide*; ingen
  Tool Rod eller Hammer Head av stål).
- **Stålverktyg** (Tool Station): smitt huvud med skafthål på ett träskaft, låst med en **Iron Tool Rod** som kil och
  beslag, ingen surrning: huvud + Iron Tool Rod + Tool Rod → Steel Pickaxe / Axe / Shovel / Hoe; Steel Spear (Spear Head
  + Iron Tool Rod + Tool Rod); Steel Sword (Sword Blade + Iron Tool Rod som parerstång och tånge + Leather som grepp);
  Steel Knife (Knife Blade + Leather + Stick). Materialet: **1100** i hållbarhet, fart 7,5, +2,5 skada (järn 6 / +2),
  bryter som diamant (obsidian). Texturerna är vanillas järnverktyg med blått stål.
- **Flint and Steel:** Flint + Steel Plate (vanillas recept ville ha Iron Ingot). Tänder varje gång.
- **Stålrustning** (Armor Station): 3 / 6 / 5 / 2 Steel Plate + 2 / 4 / 3 / 2 Leather + hammare → Steel Helmet /
  Chestplate / Leggings / Boots. Skydd 3 / 7 / 6 / 2 och 1 i seghet (järn 2 / 6 / 5 / 2, diamant 3 / 8 / 6 / 3),
  hållbarhet ×25 (järn 15, diamant 33).
- Advancements: **Cementation**, **Steel**, **Steelsmith**, **Strike a Light**, **Steel Clad**.

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

**I Bloomery:** högerklicka på Bloomery Hearth med Flux (högst 4 laddningar): nästa lupp ger en Wrought Iron mer och en
Slag mindre (se *Rostning*).

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

Inga öppna beslut just nu: rostning (sämre, inte oanvändbar), malmerna (alla fyra) och smidet (kallnar, få slag) beslutades 2026-11-21.

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

