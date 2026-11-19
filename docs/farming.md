# Odling

> Status: **byggd 2026-11-19** (spec beslutad samma dag). Stenålderns del är spelbar; bronsålderns del (Quern, bröd,
> kompost, gödsel, fågelskrämma) finns fullt fungerande i creative men saknar survival-recept tills bronsåldern fas 3.
> Spelarens beskrivning av stenålderns del finns i `docs/ages/01-stone-age.md` (Odling). Senare åldrar ger bättre
> verktyg (bronsskära, plog).

## Beslut

- **Säd = utsäde.** Mogen säd ger en **kärve** (Sheaf); tröskning ger **Grain** och **Straw**. Samma Grain sås, mals eller
  äts: varje skörd är ett val mellan att äta och att odla. Inga separata "seeds" som rinner över.
- **System i första versionen:** temperatur och frost, dagsljus, vatten och torka/rotröta, bördighet, kråkor.
- **Grödor:** vanilla-grödorna byggs om (wheat, carrot, potato, beetroot, melon, pumpkin) och nya läggs till:
  vild säd (Wild Einkorn, Wild Emmer), korn (Barley) och en baljväxt (Wild Pea / Peas).
- **Åldersuppdelning (beslutad av användaren):**
  - **Stenåldern (går att göra nu):** vild säd och Wild Pea i naturen, så Grain på farmland (Flint Hoe), Flint Sickle,
    Flail och tröskning, Porridge som sädens mat, alla växtregler (temperatur, frost, dagsljus, fukt, torka, rotröta,
    vattning med Clay Water Bucket), bördighet som sjunker vid skörd och återhämtar sig i träda och med ärtor, och kråkor
    som äter unga grödor.
  - **Bronsåldern (byggt, men inget survival-recept än):** Quern → Flour → Dough → Bread (Baking Stone / Clay Kiln),
    Compost Heap + Compost, Manure, Scarecrow. Recepten kräver en koppar-/bronsdel och läggs till i bronsåldern fas 3
    (förslag längst ned).
  - Vanillas brödrecept (3 Wheat) är borttaget. Bröd går alltså inte att få i survival förrän bronsåldern.

## Vete i det nya systemet (vanilla-mappning)

| Vanilla | Nu | Hur |
|---|---|---|
| `minecraft:wheat` (item) | **Wheat Sheaf** | Omdöpt i lang, samma textur. Mogen vete ger 1 (inte 1 + frön). 9 → Hay Bale och tillbaka som vanligt. Djur som äter vete (kor, får, getter) äter kärvar |
| `minecraft:wheat_seeds` | **Wheat Grain** | Omdöpt. Sås som förut (vanilla-blocket `minecraft:wheat`). Byarnas kistor, bondens handel och höns fungerar som förut |
| Bread-recept (3 Wheat) | **Borttaget** | Bröd = Flour → Dough → Baking Stone / Clay Kiln |
| Cookie, Cake | Flour i stället för Wheat | `minecraft:recipe/cookie.json`, `cake.json` |
| Melon Seeds-recept (skiva → frö) | **Borttaget** | Melonen släpper 0–1 frö när den bryts |
| Pumpkin Seeds-recept | 1 frö (vanilla 4) | |
| Byarnas fält | Fortfarande döende (75 % borta), men 15 % av vetet som är kvar är moget, och 25 % av morötterna/potatisen | `gen_data.py` (farm-processorerna) |
| Hay Bale i byar | Oförändrat: ruttet hö → Plant Fibre, 25 % en Wheat Sheaf | |

## Skörd och utsäde (byggt)

- **Omogen** gröda ger ingenting när den bryts. Undantag: odlat Einkorn och Emmer (fortfarande vild typ) ger 0–1 Grain.
- **Mogen Wheat/Barley/Einkorn/Emmer** → 1 Sheaf av sorten. Med **Flint Sickle**: +50 % chans på en till, och 1 Straw.
  Skäran slits 1 per mogen gröda (grödor bryts direkt, så verktyg slits annars inte).
- **Tröskning:** Flail i handen, kärvar i andra handen, högerklicka på en hård yta (taggen `mechanical:threshing_floors`:
  sten, kullersten, mossig kullersten, smooth stone, stenkross, cobbled deepslate, granit/diorit/andesit och polerade,
  packed mud, mud bricks, Stone Anvil). Varje slag (cooldown 0,4 s) har 1/3 chans att bli klart: 1–3 Grain
  (1: 25 %, 2: 50 %, 3: 25 %) + 1 Straw, flailen slits 1. Utan flail: kärven i craftingrutan → 1 Grain.
- **Rotfrukter:** mogen Carrot/Potato ger 1–2 (vanilla 2–5); Beetroot: 1 Beetroot + 0–1 Beetroot Seeds.
- **Peas:** mogna ärtor ger 1–3.
- **Melon/Pumpkin:** stjälkarna ger ingenting; melonen 3–7 skivor + 0–1 frö (Silk Touch: melonen). Pumpa → 1 frö.
- **Porridge:** 2 Grain (valfri sort, taggen `mechanical:grains`) + Clay Water Bucket + Bowl → Raw Porridge (hinken blir
  kvar tom, slits 1); kokas på Campfire 20 s → **Porridge** (5 hunger, 0,6 mättnad, skålen kommer tillbaka).
- **Bröd (bronsåldern):** Quern mal 1 Grain → 1 Flour (4 klick, överstenen vrids ett kvarts varv per klick). 2 Flour +
  Clay Water Bucket → Dough. Dough på Baking Stone vid brinnande Campfire: 30 s → Bread, 45 s till → Burnt Food. Clay
  Kiln: Dough → Bread på 20 s.

## Var utsädet kommer ifrån (byggt)

Små sällsynta bestånd (placerade som bärbuskarna: chans per chunk, antal försök, spridning ±3).

| Växt | Biomer | Chans per chunk | Ger |
|---|---|---|---|
| Wild Einkorn | Plains, Sunflower Plains, Meadow, Savanna | 1/10, 6 försök | 1 Einkorn Grain med kniv/skära, 50 % för hand |
| Wild Emmer | Plains, Sunflower Plains | 1/12, 6 försök | 1 Emmer Grain med kniv/skära, 50 % för hand |
| Wild Emmer | Forest, Flower Forest | 1/24, 4 försök | som ovan |
| Wild Pea | Forest, Flower Forest, Birch Forest, Meadow | 1/12, 5 försök | 1–2 Peas |

Uppmätt i en ny värld (193×193 block): slätt 42 Wild Einkorn + 21 Wild Emmer, savann 45 Wild Einkorn, meadow 16 Wild
Einkorn + 13 Wild Pea, flower forest 14 Wild Emmer, forest 4 Wild Pea, öken 0.

Barley har ingen vild källa än (bara creative); kommer med handel/byar senare.

## Växtvillkor (byggt)

Alla grödor (även vanilla) går genom NeoForges `CropGrowEvent`. Vanillas egen tillväxtchans gäller fortfarande; vår
faktor multipliceras på: **temperaturfaktor × fukt × bördighet × dagsljus**. Temperaturen är `Temperature.ambient`
(0–20; slätt ~12,4, taiga 8, snö 2–6, savann/öken 22, natt −5 under bar himmel, regn −2).

| Gröda | Växer (temp) | Bäst | Frost dödar under | Övrigt |
|---|---|---|---|---|
| Wheat / Einkorn / Emmer | 7–17 | 10–14 | 2 | |
| Barley | 5–16 | 8–12 | 0 | tålig, växer (långsamt) i snöbiomer |
| Carrot | 4–15 | 7–12 | −1 | tål kyla, rotfrukt |
| Potato | 6–16 | 9–13 | 2 | rotfrukt |
| Beetroot | 5–16 | 8–13 | 1 | rotfrukt |
| Peas | 6–16 | 9–13 | 2 | skörden höjer bördigheten |
| Melon / Pumpkin (stjälk) | 12–**25** | 15–20 | 6 | maxgräns 25 i stället för 22: savann och öken ligger på exakt 22 och hade annars fått noll |

- **Temperaturfaktor:** 1 i bästa spannet, linjärt ner till 0 vid kanterna.
- **Dagsljus:** växer bara på dagen med himmelsljus ≥ 9. Facklor räcker inte, och ingenting växer på natten.
- **Fukt:** fukt 0 → ingen tillväxt.
- **Frost:** på natten, under bar himmel, när luften är under grödans frostgräns: 10 % chans per random tick att en ung
  planta dör → **Withered Crop** (ger 50 % Straw, annars Plant Fibre). Mogna plantor dör inte.
- **Torka:** unga plantor på torr farmland (fukt 0): 5 % chans per random tick att vissna.
- **Rotröta:** rotfrukter på helt blöt farmland (fukt 7) i träsk eller i regn: 1 % chans per random tick.
- **Smyg + högerklicka med tom hand** på farmland eller en gröda: "Fertility 2/3, moisture 7/7", och för en gröda
  dess temperatur, spann och varför den inte växer (too cold / too hot / too dry / needs daylight / ripe).

Uppmätt (random_tick_speed 100, 1200 ticks): slätt vete växer på dagen, inte alls på natten; snöslätt på dagen: vete
står still, korn och morötter växer långsamt; snöslätt på natten: allt vete fryser ihjäl, korn och morötter klarar sig;
savann: vete och morötter står still (för varmt), melonstjälkar växer.

## Vatten och torka (byggt)

- Farmland har vanillas fukt 0–7: regn och vatten inom 4 block håller den på 7, annars sjunker den 1 per random tick.
- **Torka:** när luften är över 15 och det inte regnar, och inget vatten finns nära, sjunker fukten ett extra steg
  hälften av gångerna.
- **Clay Water Bucket:** högerklicka på farmland (eller grödan på den): den och de 8 runt omkring får fukt 7, hinken
  töms och slits 1 (som när man häller ut den). Spec sa en farmland; 3×3 eftersom det är en hel hink vatten.

## Bördighet (byggt)

- Farmland har **bördighet 0–3** (blockstate `fertility`, lagt på vanilla-farmland med en mixin). Ny åker = 2;
  **Podzol** kan nu plöjas och blir farmland med bördighet 3 (`block_transformer/hoe.json`).
- **Skörd** av en mogen gröda: −1 (Peas: +1).
- Bördighet 0: halv tillväxt och halv skörd (varje stack halveras, udda avrundas slumpvis). 1: 75 % tillväxt.
  2: normal. 3: efter varje tillväxtsteg 30 % chans på ett steg till (~1,3× fart), och 30 % chans på en extra vid skörd.
  Uppmätt: 24 mogna vete gav 11 / 24 / 29 kärvar på bördighet 0 / 2 / 3.
- **Återställa:**
  - **Träda:** farmland utan något ovanpå får +1 med chansen 1/18 per random tick (~1 per Minecraft-dag).
  - **Compost** (bronsåldern): Compost Heap fylls med 7 komposterbara saker (taggen `mechanical:compostable`: Leaf Litter,
    Straw, Plant Fibre, löv, saplings, gräs, ormbunke, ruttet kött, Burnt Food, Bark, Fish Bones, melonskivor, rötter,
    äpplen), ruttnar 2 min, ger 2 Compost. Högerklick på farmland: +1.
  - **Manure** (bronsåldern): +2. Har ingen källa än (se nedan).
  - **Växelbruk:** Peas höjer.

## Kråkor och fågelskrämma (byggt)

- När en Hooded Crow-flocks ledare ska landa letar den efter en ung gröda inom 24 block (varannan kolumn) och landar på
  den om ingen Scarecrow står inom 12 block. På marken pickar varje kråka var 4:e sekund i snitt på en ung gröda inom 2
  block: ett växtsteg försvinner, en planta på steg 0 äts upp helt. De flyr som vanligt när en spelare kommer nära.
- **Scarecrow** (bronsåldern): 2 block hög (stolpe, hudtunika stoppad med halm, armar på en tvärslå, pumpahuvud),
  vänd mot den som sätter ut den. Inom 12 block landar och äter inga kråkor.
- Uppmätt: 5 kråkor vid en oskyddad åker (24 unga vetestrån) åt upp 22 på 6000 ticks; åkern med fågelskrämma var orörd.

## Verktyg och block

| Sak | Recept | Användning |
|---|---|---|
| Flint Hoe (finns) | | Gör farmland |
| Flint Sickle | Tool Station: Flint + Twine + Stick (låses upp av Flint) | Extra kärve + Straw, skär gräs/grödor/vild säd snabbt. Hållbarhet 48 |
| Flail | `[ ][Stick]` / `[Twine][Stick]` / `[Stick][ ]` | Tröska. Hållbarhet 64 |
| Raw Porridge | 2 Grain + Clay Water Bucket + Bowl | Kokas till Porridge |
| Quern | bronsåldern | Mal Grain → Flour |
| Baking Stone | bronsåldern | Baka Dough → Bread |
| Compost Heap | bronsåldern | Kompostera |
| Scarecrow | bronsåldern | Håller kråkor borta |
| Straw | tröskning, skära | Kompost, fågelskrämma |

## Recept som läggs till i bronsåldern fas 3

Förslag (C = Copper Ingot eller Bronze Ingot, R = rock chunk):

| Sak | Förslag |
|---|---|
| Quern | `[ ][C][ ]` / `[Cobblestone][Stick][Cobblestone]` / `[Cobblestone][Cobblestone][Cobblestone]` (metallaxel genom stenarna) |
| Baking Stone | `[R][C][R]` (rock chunks kring en kopparplåt) |
| Compost Heap | `[Stick][ ][Stick]` / `[Stick][Leaf Litter][Stick]` / `[Stick][C][Stick]` (kopparspik håller ihop flätverket) |
| Scarecrow | `[C][Carved Pumpkin][ ]` / `[Straw][Raw Hide][Straw]` / `[ ][Stick][ ]` |
| Dough | finns redan (2 Flour + Clay Water Bucket); blir nåbart när Quern får recept |
| Manure | ingen crafting: tamdjur (ko, får, gris, häst) i fållor släpper ibland Manure (förslag ~1 per djur var 10:e minut) |
| Compost | ingen crafting: från Compost Heap |

## Teknik

- `Farming.java` (registrering, växtregler i `CropGrowEvent`, skörd i `BlockDropsEvent`, kråkor, fältinfo),
  `Fertility.java` + `mixin/FarmlandBlockMixin.java` (bördighet, träda, torka), `FarmCropBlock`, `WildPlantBlock`,
  `FlailItem`, `FertilizerItem`, `QuernBlock`, `BakingStoneBlock`, `CompostHeapBlock`, `ScarecrowBlock`.
  Vattning och hinkens rest i recept: `ClayBucketItem`. Dough i ugnen: `KilnBlockEntity.RECIPES`. Kråkor: `wildlife/FlockBird`.
- Data: `scripts/gen_farming.py` (efter gen_bronze_ores, före gen_block_items); texturer: `scripts/gen_farming_textures.py`.

## Att bestämma senare

- Säsonger (vår/höst) finns inte; temperaturen räcker tills vidare.
- Bronsålderns bronsskära och plog (snabbare åker, större skörd).
- Källa för Barley (handel, byar) och för Manure (djurhållning).
- Halmtak och halmbädd av Straw. Scarecrow som bär en Leather Helmet (bara utseende).
- Spelarbyggda Hay Bales ger ruttet hö när de bryts (byarnas regel gäller alla hay bales).
