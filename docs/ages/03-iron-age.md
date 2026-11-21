# Järnåldern

> Status: **byggs** (2026-11-21). Byggt: **Bog Iron** (v0.80.0), **rostning och flux**, **smide, järnverktyg och järnrustning**, **kolmila**, **stål**,
> **de nya malmerna** Galena, guld, Limestone och Rock Salt, **vagnen** (Wagon). Resten är förslag.

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

### Järnbälg och Smithing Hearth (byggd)

Två block som gör smidet mindre stressigt. Kod: `BellowsBlock` (iron), `KilnBlockEntity` / `BloomeryBlockEntity` /
`FurnaceTapBlockEntity` (pump och värmehållning), `SmithingHearthBlock`, `SmithingHearthBlockEntity`,
client/`SmithingHearthRenderer`. Data: `scripts/gen_iron_age.py`. Test: `IronAgeTest`.

| Block | Recept | Vad det gör |
|---|---|---|
| **Iron Bellows** | Bellows + 2 Iron Plate + Iron Tool Rod (formlöst) | Pumpar **dubbelt så hårt** (+80° mot +40°) i Clay Kiln, Bloomery och Smelting Furnace, och **håller värmen** över ugnens naturliga max i **60 s** efter varje pump (annars sjunker den 2°/s). Ungefär en pump i minuten räcker. |
| **Smithing Hearth** | 7 Fire Brick + 1 Iron Plate (B B / BPB / BBB) | En liten härd vid städet. Högerklicka med **Charcoal** (högst 16 i förråd, 1 min per Charcoal), tänd med Fire Drill eller Flint and Steel. Fyra platser i glöden: kalla luppar och tackor av järn eller stål blir **heta på 10 s**, heta **hålls heta** så länge den brinner. Tom hand tar ut den senast ilagda. |

- Med härden bredvid städet hinner man lägga tre heta tackor på städet till ett hackhuvud utan att springa till Bloomery.
- Advancements: **Iron Lungs** (Iron Bellows), **Smithing Hearth**.

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
| Hammer Head | 2 | 6 | Iron Hammer Head | Steel Hammer Head |
| Saw Blade | 1 | 6 | Iron Saw Blade | Steel Saw Blade |
| Bolt Heads | 1 | 4 | 4 Iron Bolt Head | – |
| Sickle Blade | 1 | 5 | Iron Sickle Blade | Steel Sickle Blade |
| Chisel Blade | 1 | 4 | Iron Chisel Blade | Steel Chisel Blade |
| Tong Arms | 1 | 5 | Iron Tong Arms | – |
| Needles | 1 | 3 | 4 Iron Needle | – |
| Fish Hooks | 1 | 3 | 4 Iron Fish Hook | – |

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
| Iron Saw / Steel Saw | Tool Station: Iron / Steel Saw Blade + Leather + Stick |
| Crossbow | `[Iron Plate][Iron Tool Rod][Iron Plate]` / `[Sinew][Tool Rod][Sinew]` / `[ ][Tool Rod][ ]` |
| Iron Bolt ×4 | Fletching Table: 4 Iron Bolt Head + Stick + Feather |

- Smitt järn håller längre än brons: järnverktygen har **600** i hållbarhet (vanilla 250, brons 460) och Iron Pickaxe
  bryter guld, redstone, diamant och smaragd. Full järnrustning, bröstplåten som bronsåldern saknade.
- Fler vanilla-järnsaker av smidda delar: **Iron Bars** (6 Iron Tool Rod → 16), **Iron Chain** (3 Iron Tool Rod i rad
  → 3), **Cauldron** (7 Iron Plate), **Iron Door** (6 Iron Plate → 3), **Iron Trapdoor** (4 Iron Plate). Rail, Hopper,
  Anvil och andra redstone- och järnvägssaker kräver fortfarande Iron Ingot (ångåldern).
- Advancements: **Forged**, **Iron Hammer**, **Ironsmith**, **Iron Clad** (Iron Age-fliken).

**Smidda sågar (byggda, v0.129.0).** Bladet smids på städet (mönstret Saw Blade, 1 tacka, 6 slag) och sätts ihop i Tool
Station som andra verktyg. De gör allt Bronze Saw gör (Log + Saw → 4 Planks, såga fallna stockar, Wooden Wheel, Potter's
Wheel), bara längre och fortare:

| Såg | Hållbarhet | Ett drag | En stock (8 drag) |
|---|---|---|---|
| Bronze Saw | 480 | 10 ticks | 4 s |
| Iron Saw | 900 | 8 ticks | 3,2 s |
| Steel Saw | 1600 | 6 ticks | 2,4 s |

Recepten som ville ha Bronze Saw tar nu vilken såg som helst (taggen `mechanical:saws`: Sawn Planks, Wooden Wheel,
Potter's Wheel). Advancement **Sharper Teeth** (efter *Forged*).

**Armborst och Iron Bolts (byggda, v0.129.0).** Vanillas **Crossbow** görs av smidda delar i crafting grid: två Iron
Plate till bågen, en Iron Tool Rod som avtryckare och stigbygel, två Sinew som sträng och två Tool Rod som stock (vanillas
recept med Iron Ingot och Tripwire Hook går inte att göra). Det skjuter vanliga pilar och **Iron Bolts**: korta pilar med
fyrkantig bodkinspets. En het tacka smids till **4 Iron Bolt Head** (mönstret Bolt Heads, 4 slag), och 4 spetsar + Stick +
Feather blir 4 Iron Bolts på Fletching Table. En bolt gör **4** i grundskada (Bronze Arrow 3, vanlig pil 2) och går igenom
**40 %** av rustningens skydd. Bara armborst kan skjuta dem (de är inte `#minecraft:arrows` utan `mechanical:bolts`; bågar
tar dem inte), de går i Quiver och plockas upp dit, och de går sällan sönder i ett byte (10 %). I luften ritas de som en
vanlig pil. Monster med armborst (plundrare) tappar det först när världen nått järnåldern (`LaterAgeGear`). Kod:
`Smithing` (items, mönster, rustningsgenomslaget), `mixin/CrossbowItemMixin` (armborstet laddar bolts),
`FletchingRecipes`. Advancements **Crossbowman** (mål, efter *Forged*) och **Bodkin** (efter *Crossbowman*).

**Smidda versioner av bronsverktygen (byggda, v0.130.0).** Varje bronsverktyg har nu sin järn- (och ofta stål-)version.
Delarna smids på städet (mönstren ovan) och sätts ihop i Tool Station; de gör allt bronsverktyget gör, och lite till:

| Verktyg | Tool Station | Hållbarhet | Vad det gör |
|---|---|---|---|
| Iron Sickle | Iron Sickle Blade + Leather + Stick | 900 | Skär **3×3** mogna grödor eller gräs (smyg: ett), **90 %** chans på extra kärve |
| Steel Sickle | Steel Sickle Blade + Leather + Stick | 1600 | Skär **5×5** (smyg: ett), **100 %**: alltid en extra kärve |
| Iron Chisel | Iron Chisel Blade + Leather + Stick | 700 | Som Bronze Chisel (sten → slät → tegel → mönstrad; på Whetstone 2 slipdrag per drag) |
| Steel Chisel | Steel Chisel Blade + Leather + Stick | 1400 | Som ovan, men **3** slipdrag per drag på Whetstone |
| Iron Tongs | Iron Tong Arms + Iron Tool Rod + Leather | 700 | Som Bronze Tongs: het metall och smälta degler bränner inte |
| Iron Needle | direkt från städet (4 per tacka) | 600 | Syr som Bone/Bronze Needle (`mechanical:needles`) |
| Iron Hook Fishing Rod | Iron Fish Hook + Twine + Tool Rod | 256 | Napp 15 s tidigare (som Lure III) och Luck of the Sea II (brons: 10 s, I) |
| Iron Quarry Hammer | Iron Hammer Head + Iron Plate + Tool Rod | 900 | 3×3 sten; malmer en järnhacka bryter smulas (guld, redstone, diamant) |
| Steel Smithing Hammer | Steel Hammer Head + Iron Tool Rod + Tool Rod | 1200 | Smider stål, nöts 1 per slag, slår var 7:e tick (andra hamrar var 10:e) |

Verktygen känns igen via taggar och klasser i stället för bronsitemet: `mechanical:tongs` (håller het metall, båda tängerna),
`mechanical:sickles`, `mechanical:needles`, alla `ChiselItem` på Whetstone och quarry hammers på sin egen nivå
(`isCorrectToolForDrops`). Fiskespöet är ett eget mod-item som bronsspöet (vanillas Fishing Rod förblir avstängt).
Kod: `Smithing` (items, mönster), `BronzeTools.sweepRadius` (skärans svep), `Farming.sickleBonus`.
Advancements **Iron Harvest**, **Firm Grip**, **Fine Ironwork**, **Iron Breaker** (efter *Forged*), **Reaper** (mål) och
**Steel Hammer** (efter *Steel*).

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

Cementering (kol och sot vandrar in i järnet). Kod: `Smithing`, `BloomeryBlockEntity` (receptet; Clay Kiln bränner bara krukan).
Data: `scripts/gen_iron_age.py`. Test: `IronAgeTest`.

| Steg | Hur |
|---|---|
| **Unfired Cementation Pot** | 5 Fire Clay (F F / FFF) |
| **Cementation Pot** | Bränd i Clay Kiln vid 1100° |
| **Packed Cementation Pot** | Cementation Pot + 3 Wrought Iron Ingot + 2 Charcoal + 1 Soot (formlöst) |
| **Cemented Pot** | Packed pot **bara i Bloomery** vid **1000°** i 6 min (snabbare hetare). Bloomery sjunker mot 800° utan bälg, så pumpa (Iron Bellows: en pump i minuten) |
| **Blister Steel** | Cemented Pot (formlöst) → **3 Blister Steel** (krukan går sönder) |
| **Steel Ingot** | Blister Steel värmd till 1100° (Hot Blister Steel), **6 slag** på städet med **Iron Smithing Hammer** |

- Heta Steel Ingots smids efter städets mönster till **Steel Plate** och stålets huvuden och blad (se *Smide*; ingen
  Tool Rod eller Hammer Head av stål).
- **Stålverktyg** (Tool Station): smitt huvud med skafthål på ett träskaft, låst med en **Iron Tool Rod** som kil och
  beslag, ingen surrning: huvud + Iron Tool Rod + Tool Rod → Steel Pickaxe / Axe / Shovel / Hoe; Steel Spear (Spear Head
  + Iron Tool Rod + Tool Rod); Steel Sword (Sword Blade + Iron Tool Rod som parerstång och tånge + Leather som grepp);
  Steel Knife (Knife Blade + Leather + Stick). Materialet: **2200** i hållbarhet, fart 7,5, +2,5 skada (järn 6 / +2),
  bryter som diamant (obsidian). Texturerna är vanillas järnverktyg med blått stål.
- **Flint and Steel:** Flint + Steel Plate (vanillas recept ville ha Iron Ingot). Tänder varje gång.
- **Stål kräver Iron Smithing Hammer:** Bronze Smithing Hammer biter inte på stål (Blister Steel eller Steel Ingot).
- **Steel Quarry Hammer** (Tool Station): Steel Hammer Head + Steel Plate + Iron Tool Rod. Bryter 3×3 som Bronze Quarry
  Hammer, men varje malm en stålhacka bryter smulas (guld, diamant m.m.); 1800 i hållbarhet.
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

**Byggblock av Limestone** (som Tuff i vanilla, samma recept och former):

| Block | Hur |
|---|---|
| **Polished Limestone** | 2×2 Limestone → 4. Eller **Bronze Chisel** på Limestone. |
| **Limestone Bricks** | 2×2 Polished Limestone → 4. Eller Bronze Chisel på Polished Limestone. |
| **Chiseled Limestone Bricks** | 2 Polished Limestone Slab på varandra. Eller Bronze Chisel på Limestone Bricks. |
| **Chiseled Limestone** | 2 Limestone Slab på varandra. |
| **Slab / Stairs / Wall** | Av Limestone, Polished Limestone och Limestone Bricks (Limestone Brick Slab/Stairs/Wall), vanliga vanilla-mönster: 3 i rad → 6 slabs, trappmönster av 6 → 4 stairs, 2×3 → 6 walls. |

Stonecutter-recepten finns också, men Stonecutter är låst tills en senare ålder. Advancement: *Limestone Mason* (efter Flux).
Familjen kommer från `scripts/gen_stone_families.py` / `StoneFamilies.java` (en rad per sten, samma som stenålderns
bergarter) och ligger i creative-fliken **Building Blocks**.

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

### 9. Kalk och murbruk (byggd, world-materials-plan fas 2)

Kalksten får en riktig användning: bränd i kilnen blir den bränd kalk, släckt med vatten blir den släckt kalk, och av
den blir murbruk och puts. Kod: `IronOres` (items, putsblocken, gametest `iron_ores`), kilnrecepten i
`KilnBlockEntity.RECIPES`. Data: `scripts/gen_lime.py` (direkt efter `gen_stone_families.py`); de tre nya bergarterna är
rader i `gen_stone_families.py` / `StoneFamilies.java`, och murbruksrecepten för tegel skrivs av `gen_stone_families.py`.

**Nya bergarter** (vilken hacka som helst, ger sig själva, familjerna i creative-fliken Building Blocks):

| Block | Var | Familj |
|---|---|---|
| **Dolomite** | Klumpar (storlek 48) i hela världen på y 0–60, och 2 extra per chunk på y 56–160 där Limestone har sina klippor (`mechanical:has_dolomite` = `#mechanical:has_limestone_cliffs`). Krossas på **Stone Anvil** till **2 Flux** som Limestone. | Tuff: Polished Dolomite, Dolomite Bricks, Chiseled Dolomite (Bricks) + slab/stairs/wall |
| **Travertine** | Honungsfärgad källkalk, y 40–90 under River, Swamp, Mangrove Swamp, Plains och Sunflower Plains (`mechanical:has_travertine`; Limestone Karst läggs till senare). | Sandstone: Cut, Smooth, Chiseled Travertine + slab/stairs/wall |
| **Shell Bank** | Snäckskalssten (coquina) med rosa skalränder, y 40–70 under stränder, Stony Shore, Ocean, Lukewarm och Warm Ocean (`mechanical:has_shell_bank`). | Sandstone: Cut, Smooth, Chiseled Shell Bank + slab/stairs/wall |

**Kalkkedjan:**

| Vad | Hur |
|---|---|
| **Quicklime** | **Limestone, Dolomite, Chalk Block, Travertine eller Shell Bank** (1 block) eller **4 Chalk** i **Clay Kiln vid 900°**, 20 s → 1 Quicklime. 900° är över Charcoals 800°, så det krävs **Bellows**. |
| **Slaked Lime** | Quicklime + Water Bucket (eller Clay/Bronze Water Bucket), formlöst → 1. Hinken kommer tillbaka. |
| **Lime Mortar** | Slaked Lime + Sand + Gravel, formlöst → **4**. |
| **Tegel med murbruk** | 4 av det teglet läggs av + 1 Lime Mortar, formlöst → **dubbelt så många**: 4 Polished Limestone + Mortar → **8** Limestone Bricks, likadant för varje stenfamiljs Bricks (Dolomite, Slate, Gneiss, Soapstone, Shale, Conglomerate ...) och vanillas Stone Bricks, Deepslate Bricks och Mud Bricks; 4 Brick + Mortar → 2 Bricks. Murbruket **krävs inte**, de vanliga recepten finns kvar. |
| **Lime Plaster** | Slaked Lime + 2 Sand + Straw, formlöst → **4** (block, gräddvitt). |
| **Färgad puts** | 8 Lime Plaster runt ett pigment (3×3) → 8: **White** (White Pigment), **Black**, **Red**, **Yellow Lime Plaster**. |
| **Kalkad hud** | Scraped Hide + Slaked Lime, formlöst → **Leather**, som saltad hud. |

Putsen bryts snabbast med hacka men vad som helst ger blocket. Bara block, inga slab/stairs/wall för putsen.

**Advancements** (Iron Age-fliken, under *Limestone Mason*): *Dolomite*, *Travertine*, *Shell Bank*, *Quicklime* →
*Lime Mortar* och *Lime Plaster*.

### 10. Alun (byggd, world-materials-plan fas 9B)

Alum Shale (svart skiffer, Galena Seams och kalla biomer) ger alun: ett betningsmedel som får färgen att fästa och ett
garvsalt. Kod: `Chemistry` (gametest `chemistry`), kilnreceptet i `KilnBlockEntity.RECIPES`. Data: `scripts/gen_chemistry.py`.

| Vad | Hur |
|---|---|
| **Roasted Alum Shale** | 1 **Alum Shale** (block) i **Clay Kiln vid 800°**, 20 s → 1. 800° är Charcoals egen värme, ingen Bellows. |
| **Alum** | Roasted Alum Shale + Water Bucket (eller Clay/Bronze Water Bucket), formlöst → **2**. Hinken kommer tillbaka (lakning). |
| **Färg med alun** | 2 pigment (Black, Red, Yellow eller White Pigment) + Alum, formlöst → **4 Dye**. Utan alun ger 1 pigment 1 dye, så alunet dubblar färgen. |
| **Alungarvning** | 2 Scraped Hide + Alum, formlöst → **2 Leather**. Salt och Slaked Lime garvar en hud var. |

**Advancements** (Iron Age): *Alum* → *Mordant* och *Tawed*.

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
| Iron Saw Blade, Steel Saw Blade, Iron Saw, Steel Saw | Items | **Byggd.** Smitt blad, Tool Station |
| Iron Bolt Head, Iron Bolt | Items | **Byggd.** 4 spetsar per tacka, armborstets ammunition |
| Crossbow (vanilla) | Item | **Byggd.** Iron Plate, Iron Tool Rod, Sinew, Tool Rod |
| Steel Ingot | Item | Wrought Iron + Charcoal, cementering |
| Galena Ore, Deepslate Galena Ore | Block | **Byggd.** Bronze Pickaxe, ger Raw Galena |
| Raw Galena, Lead Ingot | Items | **Byggd.** Kiln 800° |
| Leaded Cupel, Silver Nugget, Silver Ingot | Items | **Byggd.** Kupellering vid 1000°, 9 nuggets = 1 tacka |
| Limestone, Flux | Block/Item | **Byggd.** Stone Anvil: 1 → 2 Flux, taggen `mechanical:flux` |
| Polished Limestone, Limestone Bricks, Chiseled Limestone (Bricks), slabs/stairs/walls | Block | **Byggd.** Vanilla Tuff-recept, Bronze Chisel |
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

## Jaspis som tändsten (byggt, fas 9A av världsmaterialplanen)

Jaspis slår gnistor mot stål som flinta: **Rough Jasper eller Polished Jasper + Steel Plate** (formlöst) ger en
Flint and Steel, precis som Flint + Steel Plate. Advancement *Red Spark* under *Strike a Light*.

## Omfogning med kalkbruk (byggt, fas 10 av världsmaterialplanen)

Tegel som spruckit av frost (Cracked Bricks av alla bergarter, se bronsålderns "Åldrande tegel") lagas med **Lime Mortar**:
högerklicka det spruckna blocket, så blir det helt igen och ett murbruk går åt (inget i creative). Gäller också vanillas
Cracked Stone Bricks, Cracked Deepslate Bricks/Tiles och de andra spruckna vanilla-blocken. Advancement *Repointed* (efter
*Lime Mortar*).

## Linen Seed Bag (byggd, v0.131.0)

Bronsålderns Seed Bag i större format (docs/farming.md "Fältredskap"): sår **5×5** tom farmland per högerklick, ett frö
per block, samma 3×3-meny för utsäde.

```
[Linen Thread][Iron Plate  ][Linen Thread]
[Linen Thread][            ][Linen Thread]
[Linen Thread][  Leather   ][Linen Thread]
```
Görs från grunden och inte av en Seed Bag (fröna i den hade annars försvunnit i receptet). Advancement **Wide Sower**
(Iron Age-fliken, efter *Forged*). Den dragna plogen är ett eget bygge.

## Vagnen (Wagon) (byggd)

Järnålderns transport: kärrans storebror, logistik på väg. Fyra hjul med järnringar, en lång plankbädd och en dragstång
för **ett par tama hästar**. Bronsålderns Wheeled Cart finns kvar oförändrad. Kod: `WagonEntity` (bygger på
`TravoisEntity`), `Carting` (registrering), klient: `client/WagonModel`, `client/WagonRenderer`. Data:
`scripts/gen_wagon.py` (efter gen_iron_age.py, före gen_block_items.py), texturer: `scripts/gen_wagon_textures.py`. Test: `WagonTest`.
Plan: [wagon-plan.md](../wagon-plan.md).

### Items och recept

| Sak | Recept | Låses upp av |
|---|---|---|
| **Iron-Rimmed Wheel** | formlöst: Wooden Wheel + 2 Iron Plate (ringen krymps på het runt fälgen) | Iron Plate |
| **Wagon** | `[Iron-Rimmed Wheel][Iron Tool Rod][Iron-Rimmed Wheel]` / `[Planks][Iron Plate][Planks]` / `[Iron-Rimmed Wheel][Planks][Iron-Rimmed Wheel]` | Iron-Rimmed Wheel |

### Spannet

- **Spelare kan inte dra vagnen.** Den dras av tama, vuxna hästar (inte föl).
- **Spänn för:** rid en tam häst och led en annan i koppel (inom 10 block), och högerklicka vagnen med tom hand: båda
  hamnar i oket. Eller spänn för en häst (rid eller led den) och högerklicka igen med en andra. Högerklicka igen för att
  spänna från ("Unhitched").
- **Andra hästen** hålls vid sidan om den första (paret 1,45 block isär, mitten 2,8 block framför vagnen) och följer med i
  svängar. Hålls den kvar mer än **4 block** från den första släpper den ur oket ("The second horse was held back and came
  out of the yoke") och vagnen går över till enhästläge.
- **En häst:** med tom vagn kryper den fram (vagnen sluter 30 % av avståndet, hästen går 75 % långsammare). Lastad (kroppar
  eller något i kistorna) rör den sig inte alls.
- **Ett par:** drar vagnen i full fart, upp till **15 %** långsammare när den är full (efter hur full den är).
- Framaxeln svänger mot dragstången och hjulen rullar med farten. Fastnar vagnen lossnar spannet ("The wagon is stuck. The
  team came loose"), som på kärran. Vagnen fastnar i tät skog: det är meningen.
- Spannet sparas inte när världen laddas om (som kärrans sele): spänn för igen.

### Flaket: fyra platser

Bädden har **4 platser** framifrån och bakåt. Varje plats är tom eller har en modul:

| Plats | Vad den gör |
|---|---|
| tom | **3 rum** kroppar. Helt tomt flak = **12 rum**: 4 stora, 6 mellanstora eller 12 lätta (kärran 6). Jättar går aldrig. |
| **Chest** | **27 platser**. Smyg + högerklicka den platsen för att öppna just den kistan. Fyra kistor = 108 platser. |
| **Hay Bale** | **9 tuggor**. En skadad häst i spannet äter en tugga var 5:e sekund och läker 2 hälsa. Uppäten försvinner balen. |
| **säte** | valfri trä-slab (taggen `#minecraft:wooden_slabs`). Högerklicka sätet med tom hand för att sitta (när du inte rider eller leder en tam häst som kan spännas för). Kommer tillbaka som en Oak Slab. |

- **Sätt i en modul:** högerklicka en plats med blocket (Chest, Hay Bale eller trä-slab). Platsen måste vara ledig ("That
  place on the bed is taken"), och lasten måste få plats på de tomma platser som blir kvar ("Unload a carcass first").
- **Ta ur en modul:** slå på den. Kistan och allt innehåll faller ut; en påbörjad Hay Bale ger tillbaka sin återstående
  Wheat (en orörd ger en Hay Bale).
- **Plocka upp vagnen:** slå på vagnen där det inte sitter någon modul. Allt faller ut: kroppar, moduler och kistornas innehåll.
- **Last:** som på kärran: högerklicka vagnen med kroppen du bär eller släpar. **Ta av** den sista kroppen: smyg +
  högerklicka en tom plats.
- Säte för spelare än så länge; stamfolk åker inte med.

### Advancements (Iron Age-fliken, efter *Forged*)

- **Iron Shod**: gör en Iron-Rimmed Wheel.
- **Wagoner** (mål): bygg en Wagon.
- **A Pair in the Yoke**: spänn för två hästar.
- **Heavy Haul** (mål): 12 rum kroppar på en vagn med alla fyra platser tomma.
- **Freight**: en Chest på alla fyra platserna.
