# Metaller och smältor

Genomgång av alla metaller och smältor i moddet: smältpunkter, densitet och lagerordning, färg, vilka ugnar som smälter
vad, malm och utbyte, legeringar, gjutning, smide, verktyg, mynt och var varje värde står i koden. Föremålsnamn står på
engelska. Avsnitt 1–10 är granskningen som den såg ut i v0.207.0; avsnitt 11 listar inkonsekvenserna och förslagen.

Sökvägar: `java/` = `src/main/java/com/mechanical/`, `res/` = `src/main/resources/`.

## 1. Metalltabell (v0.207.0)

Spelets smältpunkt är den temperatur ugnen måste nå för att smälta (och hålla smältan flytande nog att hälla).
Lagerordningen i High Furnace är deklarationsordningen i `Molten` (tyngst längst ned), inte en densitet.

| Smälta | I spelet | Verklig | Verklig densitet (g/cm³) | Lager i HF (nedifrån) | Färg `Molten.color` | Färg `FurnaceScreen.colour` | Definieras |
|---|---|---|---|---|---|---|---|
| Gold | 1100° | 1064° | 19,3 | 1 | `FFC21E` | – | `java/Molten.java:13`, `IronOres.GOLD_TEMP` 1100 (`java/IronOres.java:66`) |
| Lead | 330° | 327° | 11,3 | 2 | `8C93A8` | – | `java/Molten.java:14` |
| Silver | 1000° | 962° | 10,5 | 3 | `F4F6FF` | – | `java/Molten.java:15`, `IronOres.CUPEL_TEMP` 1000 |
| Copper | 1085° | 1085° | 8,96 | 4 | `FF7A2A` | `FF8A48` | `java/Metal.java:18` och `java/Molten.java:16` (två gånger) |
| Bronze | 950° | ≈950° (10 % tenn) | 8,8 | 5 | `FFB13B` | `FFC85A` | `java/Metal.java:26` och `java/Molten.java:17` |
| Poor Bronze | 950° | 900–1000° | 8,8 | 6 | `E09A44` | `E8A850` | `java/Metal.java:27` och `java/Molten.java:18` |
| Pig Iron | 1200° | 1150–1200° | ≈7,1 | 7 | `FFA33A` | – | `java/Molten.java:19` |
| Steel | 1500° | 1425–1540° | 7,85 | 8 | `FFCE4A` | – | `java/Molten.java:20` |
| Wrought Iron | 1550° | ≈1510–1540° | 7,75 | 9 | `FFC040` | – | `java/Molten.java:21` |
| Tin | 232° | 232° | 7,29 | 10 | `E8ECF2` | `E4E8F0` | `java/Metal.java:25` och `java/Molten.java:22` |
| Glass | 1400° | ≈1000–1400° (bearbetning) | 2,5 | 11 | `B8F0A0` | – | `java/Molten.java:23` |
| Basalt | 1250° | 1100–1250° | 2,9–3,0 | 12 | `C0402A` | – | `java/Molten.java:24` |
| Obsidian | 1700° | ≈1000–1200° | 2,4 | 13 | `6A2AA8` | – | `java/Molten.java:25` (spelvärde: ska kräva toppvärme) |
| Slag | 1200° | 1200–1400° | 2,8–3,3 | 14 (alltid överst) | `3A4A2A` | – | `java/Molten.java:26` |

Former som inte är egna smältor:

- **Bog Iron** (Raw/Roasted Bog Iron) är malm, ger Pig Iron i HF och en bloom i Bloomery.
- **Iron Bloom** är järn + slagg i 3 delar (`Ironworking.BLOOM_PARTS`, `java/Ironworking.java:70`), smids till Wrought Iron Ingot.
- **Blister Steel** (cementering) smälter i HF som Steel, 200 mB (`java/HighFurnaceMelting.java:62`).
- **Cast Iron** är Pig Iron gjutet i Molding Flask (Cast Iron Gear, Cast Iron Pot, järnformarna).
- **Iron** (vanilla Iron Ingot) görs inte av moddets järn; Iron Nugget används som nugget för alla tre järnen.
- Zink, mässing, arsenikbrons och nickel finns inte. Zink och mässing är planerade till ångåldern
  (`docs/ages/03-iron-age.md:347`, `docs/ages/04-steel-age.md:16`).

## 2. Ugnar och temperaturer

| Ugn | Naturligt max | Med blåsbälg/blästring | Tak | Definieras |
|---|---|---|---|---|
| Clay Kiln | sticks 500, firewood/logs 650, peat 600, charcoal/coal 800 | 700 / 900 / 850 / **1300** | 1300 (`CEILING`) | `java/KilnBlockEntity.java:51,158-165` |
| Smelting Furnace (Furnace Tap) | 800 (bara charcoal) | 1300 | kilnens 1300 | `java/FurnaceTapBlockEntity.java:55,214-237` |
| Bloomery | 800 | 1500 | 1500; smälter malm vid 1350, smideshetta 1100, cementering 1000 | `java/BloomeryBlockEntity.java:35`, `java/Smithing.java:52` |
| Finery Hearth | 800 + 50/blåspoäng | – | färskar vid 1300 | `java/FineryHearthBlock.java:41,47` |
| High Furnace | charcoal 800, coke 950 | +50 per blåspoäng (max 16), Hot Blast +100 | charcoal 1600, coke 1750, med Hot Blast 1700/1800 (`MAX_HEAT`) | `java/HighFurnaceBlockEntity.java:55,62,286` |
| Lera (Clay Mold, Clay Channel, Pouring Spout → lerform) | – | – | tål smälta upp till 1100 (`CLAY_MAX`) | `java/PouringSpoutBlockEntity.java:31` |

Vilken metall smälter var (v0.207.0):

| Smälta | Crucible i Clay Kiln | Smelting Furnace | Bloomery | High Furnace |
|---|---|---|---|---|
| Copper | ja, 1085° | ja, 1085° | – | ja |
| Tin | smälter (232°) men kan inte hällas ensam | smälter, hälls inte ensam | – | ja, 232° |
| Bronze / Poor Bronze | ja, 950° (legeras i degeln) | ja, 950° | – | ja |
| Lead | Raw Galena → Lead Ingot vid 800° (reduktion) | – | – | ja, 330° |
| Silver | Leaded Cupel → Silver Nugget vid 1000° | – | – | ja (bara omsmältning) |
| Gold | Raw Gold / 9 Gold Nugget → Gold Ingot vid 1100° | – | – | ja |
| Iron (malm) | – | – | bloom vid 1350° | Pig Iron vid 1450° |
| Pig Iron | – | – | – | ja, 1200° |
| Steel / Wrought Iron | värms till 1100° för smide | – | värms till 1100° | ja, 1500° / 1550° |
| Glass, Basalt, Obsidian | – | – | – | ja |

## 3. Malm och utbyte per ugn

Enheter: degeln räknar *units* (1 unit = 1 Crushed Copper Ore, 2 units = 1 tacka), Smelting Furnace *quarters*
(4 = 1 unit), High Furnace mB (200 = 1 tacka, 22 = 1 nugget, 1800 = 1 block).

| Malm / föremål | Hur | Crucible (Clay Kiln) | Smelting Furnace | High Furnace | Bloomery |
|---|---|---|---|---|---|
| Raw Copper (vanilla copper ore) | krossas till 2 Crushed Copper Ore (`java/BronzeOres.java:68`) | – | – | 400 mB Copper (2 tackor) | – |
| Malachite (lösa fynd) | krossas till 1 Crushed Copper Ore (`:69`) | – | – | – | – |
| Crushed Copper Ore | | 1 unit (½ tacka) | 5 quarters (+25 %) | 200 mB (1 tacka) | – |
| Copper Concentrate (vaskad) | 1 Crushed → 1 Concentrate (`:85`) | 2 units | 10 quarters | 400 mB | – |
| Native Copper | lösa fynd, vaskning | 2 units | 10 quarters | 400 mB | – |
| Cassiterite (tin_ore 1–2 st) | krossas 1:1 till Crushed Tin Ore | – | – | – | – |
| Crushed Tin Ore / Tin Concentrate | | 1 / 2 units | 5 / 10 quarters | 200 / 400 mB Tin | – |
| Raw Galena | | Clay Kiln: 1 Lead Ingot vid 800° | – | 300 mB Lead (1,5 tackor) | – |
| Leaded Cupel (Lead Ingot + Bone Meal) | | Clay Kiln: 1 Silver Nugget vid 1000° | – | – | – |
| Raw Gold | | Clay Kiln: 1 Gold Ingot vid 1100° | – | 300 mB Gold | – |
| Raw Iron | | – | – | 100 mB Pig Iron (200 med flux) vid 1450° | 4 per bloom → 1 Wrought Iron Ingot (+1 med flux) |
| Roasted Iron Ore (campfire) | | – | – | 200 (300 med flux) | 4 per bloom → 2 (+1) |
| Raw Bog Iron | grävs med spade | – | – | 65 (130) | 6 per bloom → 1 (+1) |
| Roasted Bog Iron | | – | – | 130 (200) | 6 per bloom → 2 (+1) |
| Tackor | | Bronze Ingot 2 units; Copper Ingot **tas inte** | Copper/Bronze Ingot 8 quarters; Tin Ingot **tas inte** | 200 mB | – |
| Nuggets | | – | – | Gold/Silver Nugget 22 mB; Copper och Iron Nugget **smälter inte** | – |
| Block | | – | – | Gold/Copper Block 1800 mB | – |

Järnmalm i HF lämnar 75 mB Slag (25 med flux) och bränner ½ Charcoal (`java/HighFurnaceMelting.java:15`).
Definieras: `Casting.oreUnits` (`java/Casting.java:104-113`), `FurnaceMelt.quarters` (`java/FurnaceMelt.java:42-47`),
`HighFurnaceMelting.of` (`java/HighFurnaceMelting.java:36-64`), `BloomeryBlockEntity.RECIPES` (`java/BloomeryBlockEntity.java:48-51`).

## 4. Legeringar

| Legering | Var | Recept | Regel | Definieras |
|---|---|---|---|---|
| Bronze | Crucible, Smelting Furnace | Copper + Tin | 9–11,5 % tenn → Bronze; 8–15 % → Poor Bronze (70 % hållbarhet); annars Copper och tennet blir slagg | `java/CrucibleContents.java:78-83`, `java/Bronze.java:44` |
| Bronze | High Furnace | 9 Copper + 1 Tin (mB) | exakt, vid 950°; överskott blir kvar som eget lager; aldrig Poor Bronze | `java/MeltPool.java:26-28` |
| Steel | High Furnace | 1 Pig Iron + 2 Wrought Iron | vid 1500° | `java/MeltPool.java:28` |
| Steel (Blister Steel) | Bloomery | 3 Wrought Iron + 2 Charcoal + Soot i Cementation Pot | 1000° i 7200 ticks → 3 Blister Steel, viks till Steel Ingot (6 slag) | `java/Smithing.java:44,52` |
| Wrought Iron | Finery Hearth | Pig Iron | färskas vid 1300° | `java/FineryHearthBlock.java:47` |

## 5. Gjutning: vad blir vad

| Form | Tar | Metaller | Definieras |
|---|---|---|---|
| Clay Mold (22 former, 2–10 units, spricker efter 5) / Soapstone Mold (spricker aldrig) | Crucible eller Pouring Spout (≤1100°) | Copper: ingot + 5 verktygshuvuden; Bronze/Poor Bronze: alla 22; Tin: ingenting | `java/Metal.java:18-65`, `java/ClayMoldBlock.java:51-54` |
| Molding Flask | ⅔ av lerformens mB | samma som lerformen men färdiga föremål; Pig Iron: Iron Ingot/Plate/Nugget Mold, Cast Iron Gear, Cast Iron Pot | `java/MoldingFlaskBlock.java:61-107` |
| Iron Ingot Mold | 800 mB → 4 tackor | alla med tacka (Gold, Lead, Silver, Copper, Bronze, Poor Bronze, Tin, Pig Iron, Steel, Wrought Iron) | `java/IngotMoldBlock.java:155-167` |
| Iron Plate Mold | 400 mB | Bronze, Wrought Iron, Steel (**inte Poor Bronze**) | `java/IngotMoldBlock.java:96-101` |
| Iron Nugget Mold | 400 mB → 18 | Copper (specialfall), Gold, Silver, järnen (Iron Nugget) | `java/IngotMoldBlock.java:107-111` |
| Pig Mold | 400 mB → 2 tackor | alla med tacka | `java/PigMoldBlock.java` |
| Foundry Basin | 1800 mB (sten 250) | Copper, Gold, Lead, Bronze, **Poor Bronze → Bronze Block**, Steel, Slag, Basalt, Glass, Obsidian; **inte Tin** (fast Tin Block finns), Silver, Pig/Wrought Iron | `java/FoundryBasinBlock.java:78-95` |
| Loam Mold | 1600 / 1400 mB | Bell (Bronze), Cauldron (Pig Iron) | `java/FoundryBasinBlock.java:99` |
| Refractory Trough / Sand Runner (knackas ur) | allt | tackor + nuggets (22 mB); resten förloras; Copper **har ingen nugget här** | `java/RefractoryTroughBlock.java:74-88` |
| Steel Ladle | 1000 mB | allt | `java/SteelLadleItem.java:23` |

## 6. Smide

Wrought Iron och Steel gjuts inte till verktyg; de smids varma (1100°) på städet efter mönster i `Smithing.Pattern`
(`java/Smithing.java:175-194`). Iron Bloom: 8 slag (`java/Ironworking.java:65`). Blister Steel: 6 slag till Steel Ingot.
Steel kräver Iron/Steel Smithing Hammer (`java/Smithing.java:281`). Tackor och plåtar av Bronze/Wrought Iron/Steel kan
också gjutas i järnformarna.

## 7. Verktyg och rustning

| Material | Verktyg | Rustning | Bryter | Definieras |
|---|---|---|---|---|
| Copper | vanilla copper tools (Tool Station) + Copper Knife 200 | – | som sten + Tin Ore | `java/Casting.java:92`, `res/data/minecraft/tags/block/incorrect_for_copper_tool.json` |
| Bronze | 460 hållbarhet, fart 6, +2, ench. 12 | ×10, 2/6/5/2 (bara hjälm och benskydd) | järn, kol, galena, ädelstenar | `java/Bronze.java:52-57` |
| Poor Bronze | 70 % hållbarhet | 70 % | som Bronze | `java/Bronze.java:44,161` |
| Iron | vanilla, 600 hållbarhet | vanilla | vanilla | `java/Smithing.java:172,336` |
| Steel | 2200, 7,5, +2,5, ench. 12 | ×25, 3/7/6/2, toughness 1 | som diamant | `java/Smithing.java:126,141` |
| Gold | – (Jewelry: ring, chain, necklace) | – | – | `java/Jewelry.java:109` |

## 8. Mynt

Copper, Silver och Gold Ingot präglas kallt på Bronze Anvil: 4 slag → 4 mynt. 1 Silver Coin = 10 Copper, 1 Gold = 10
Silver. Tackorna står hårdkodade i `Coins.minted` (`java/Coins.java:36-37`).

## 9. Tackor, nuggets och block

| Metall | Ingot | Nugget | Block |
|---|---|---|---|
| Copper | vanilla | vanilla | vanilla |
| Tin | `tin_ingot` | **saknas** | `tin_block` |
| Bronze / Poor Bronze | `bronze_ingot` (Poor = komponent) | **saknas** | `bronze_block` |
| Lead | `lead_ingot` | **saknas** | `lead_block` |
| Silver | `silver_ingot` | `silver_nugget` | **saknas** |
| Gold | vanilla | vanilla | vanilla |
| Pig Iron | `pig_iron_ingot` | Iron Nugget (delad) | **saknas** |
| Wrought Iron | `wrought_iron_ingot` | Iron Nugget (delad) | **saknas** |
| Steel | `steel_ingot` | Iron Nugget (delad) | `steel_block` |

## 10. Var allt står (v0.207.0)

Samma metall beskrivs på många ställen:

- `Metal` (degel, Smelting Furnace, Clay Mold-statet `metal`): smältpunkt och gjutformer.
- `Molten` (High Furnace): smältpunkt igen, färg, lagerordning.
- `MeltPool.ALLOYS`, `CrucibleContents.alloy`: legeringar.
- `HighFurnaceMelting.of`/`nugget`, `IngotMoldBlock.ingot`/`result`, `FoundryBasinBlock.block`, `MoldingFlaskBlock.result`:
  föremål per smälta som `switch`-satser.
- `FurnaceScreen.colour`: en andra färg per metall.
- `IronOres.GOLD_TEMP`/`CUPEL_TEMP`, `HighFurnaceScreen` (1450 som literal), `Coins.minted`.
- Generatorerna: tupeln `("copper", "bronze", "poor_bronze")` för Clay Mold-varianter i sex skript (`gen_copper.py:28`,
  `gen_bloomery.py:141`, `gen_bronzetools.py:70`, `gen_high_furnace.py:370`, `gen_kettle.py:225`, och utseendet i
  `gen_copper.py`), smältornas namn i `gen_high_furnace.py:896`, metallnamnen i `gen_copper.py` och `gen_bronze.py`.

## 11. Inkonsekvenser och förslag

1. **Två smältpunktskällor.** `Metal.melt` och `Molten.melt` upprepar samma tal för Copper, Tin, Bronze och Poor Bronze.
   *Förslag:* en tabell; båda läser den.
2. **Två färger per metall.** `FurnaceScreen.colour` (Smelting Furnace och Clay Channel-strängen) skiljer sig från
   `Molten.color` (alla HF-renderare) för Copper, Tin, Bronze och Poor Bronze. *Förslag:* en färg i tabellen.
3. **Lagerordningen är inte densitet.** Pig Iron (≈7,1) ligger under Steel (7,85), Wrought Iron (7,75) och Tin (7,29);
   Glass (2,5) under Basalt (2,9). *Förslag:* densitet i tabellen, ordningen räknas fram: … Poor Bronze, Steel, Wrought
   Iron, Tin, Pig Iron, Basalt, Glass, Obsidian, Slag (Slag får spelvärdet 2,3 så att den alltid flyter överst).
4. **HF-poolen sparas med enum-ordningstal** (`MeltPool.save`). En ny smälta någonstans utom sist, eller en ändrad
   ordning, blandar ihop sparade pooler. *Förslag:* spara per id, läs gamla världar via den frysta gamla ordningen.
5. **Smelting Furnace tömmer sig** om antalet `Metal` ändras (`FurnaceMelt.load` kräver exakt längd). *Förslag:* läs
   så mycket som finns.
6. **Copper Nugget.** Iron Nugget Mold ger Copper Nugget som specialfall, men en tråg med koppar som knackas ur förlorar
   resten (`HighFurnaceMelting.nugget` har ingen koppar), och Copper/Iron Nugget smälter inte i HF.
   *Förslag:* nugget per metall i tabellen; alla nuggets smälter till 22 mB; Iron Nugget (delad av tre järn) smälter
   som Pig Iron (skrot tar upp kol i ugnen).
7. **Poor Bronze tvättas till Bronze.** Foundry Basin gör Bronze Block av Poor Bronze, och blocket smälter om som god
   Bronze. *Förslag:* Poor Bronze gjuter inget block.
8. **Poor Bronze i Iron Plate Mold** ger inget, fast lerformen för plåt tar Poor Bronze. *Förslag:* Bronze Plate,
   märkt Poor.
9. **Tin i Foundry Basin** ger inget, fast Tin Block finns. *Förslag:* Tin → Tin Block.
10. **Smelting Furnace tar Copper Ingot men inte Tin Ingot.** *Förslag:* varje degelmetalls tacka (2 units). Degeln
    behåller sin regel att bara ta malm och legeringstackor: en tömd degel ger malmmetall tillbaka som malm, och malm
    ger dubbelt i HF, så Copper Ingot → degel → Copper Concentrate → HF vore en fördubblingsloop.
11. **Saknade föremål:** nuggets för Tin, Lead och Bronze, block för Silver, Pig Iron och Wrought Iron. Rester under en
    tacka förloras när Tin/Lead/Bronze knackas ur en tråg. *Förslag:* låt vara tills vidare (nya föremål kräver
    texturer och registry-noter); listat som lucka.
12. **Hårdkodade dubbletter av smältpunkter:** `IronOres.GOLD_TEMP` 1100 (= Gold), `IronOres.CUPEL_TEMP` 1000
    (= Silver), Jewelry-bordet, `HighFurnaceScreen` 1450, `HighFurnaceMelting.IRON_ORE_HEAT`. *Förslag:* läs ur tabellen.
13. **Mynt** har sina tre tackor hårdkodade. *Förslag:* `coin` i tabellen.
14. **Gjutformerna och föremålen** står som `switch` i fem klasser. *Förslag:* `ingot`, `nugget`, `block`, `plate`,
    `casts` (lerformer) och `flask` i tabellen.
15. **Generatorerna** har Clay Mold-metallerna på sex ställen och smältornas namn i ett. *Förslag:* läs ur tabellen.
16. **Dokumentfel:** `docs/ages/02-bronze-age.md:526` säger att Poor Bronze-spjut/svärd har "70 %: 161" – 460 × 0,7 =
    **322**.
17. **Vanilla-ugn:** `tin_ingot_from_smelting_tin_ore` smälter silk-touchad Tin Ore direkt till Tin Ingot förbi
    bronskedjan. Kräver Silk Touch (sent), så bara noterat.
18. **Texturpaletter** skiljer sig mellan tacka och block för Lead och Bronze, och Slag har tre paletter. Det är
    utseende, inte regler; noterat.
19. **Smältpunkter mot verkligheten:** Silver 1000 (verklig 962) och Gold 1100 (1064) är avrundade till spelsteg
    (kupellering 1000°, Gold precis på lerans gräns 1100°). Obsidian 1700 är ett spelvärde (ska kräva toppvärme).
    Bloomery 1350 mot HF 1450 är två processer (fast reduktion mot smält järn). Inga ändringar föreslås.
