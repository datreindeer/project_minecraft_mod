# Odling

> Status: **byggd 2026-11-19** (spec beslutad samma dag). Stenålderns del är spelbar. Bronsålderns del (Quern, bröd,
> kompost, gödsel, fågelskrämma) är **byggd i bronsåldern fas 3**: recepten kräver en Copper eller Bronze Ingot, och
> boskap släpper Manure (se "Bronsåldern fas 3 (byggd)").
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
  - **Bronsåldern (fas 3, byggd):** Quern → Flour → Dough → Bread (Baking Stone / Clay Kiln),
    Compost Heap + Compost, Manure, Scarecrow. Recepten kräver en Copper eller Bronze Ingot.
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
| Byarnas fält | Fortfarande döende (75 % borta), men 15 % av vetet som är kvar är moget, och 25 % av morötterna. Potatis blir morötter (även i zombiebyar) | `gen_data.py` (farm-processorerna), `gen_roots.py` (zombie-processorerna) |
| Hay Bale i byar | Oförändrat: ruttet hö → Plant Fibre, 25 % en Wheat Sheaf | |

## Skörd och utsäde (byggt)

- **Omogen** gröda ger ingenting när den bryts. Undantag: odlat Einkorn och Emmer (fortfarande vild typ) ger 0–1 Grain.
- **Mogen Wheat/Barley/Einkorn/Emmer** → 1 Sheaf av sorten. Med en skära: 1 Straw och chans på en kärve till, bättre skära ger mer: **Flint Sickle** 50 %, **Bronze Sickle** 80 % (`Farming.sickleBonus`, visas i tooltipen).
  Skäran slits 1 per mogen gröda (grödor bryts direkt, så verktyg slits annars inte).
- **Tröskning:** Flail i handen, kärvar i andra handen, högerklicka på en hård yta (taggen `mechanical:threshing_floors`:
  sten, kullersten, mossig kullersten, smooth stone, stenkross, cobbled deepslate, granit/diorit/andesit och polerade,
  packed mud, mud bricks, Stone Anvil). Varje slag (cooldown 0,4 s) har 1/3 chans att bli klart: 1–3 Grain
  (1: 25 %, 2: 50 %, 3: 25 %) + 1 Straw, flailen slits 1. Utan flail: kärven i craftingrutan → 1 Grain.
- **Rotfrukter:** mogen Carrot/Potato ger 1–2 (vanilla 2–5); Beetroot: 1 Beetroot + 0–1 Beetroot Seeds. Mogna Wild Carrots:
  1–2 Wild Carrot och 25 % chans på en riktig Carrot (se "Rotfrukter").
- **Peas:** mogna ärtor ger 1–3.
- **Melon/Pumpkin:** stjälkarna ger ingenting; melonen 3–7 skivor + 0–1 frö (Silk Touch: melonen). Pumpa → 1 frö.
- **Porridge:** 2 Grain (valfri sort, taggen `mechanical:grains`) + Clay Water Bucket + Bowl → Raw Porridge (hinken blir
  kvar tom, slits 1); kokas på Campfire 20 s → **Porridge** (5 hunger, 1,2 mättnad som en måltid, skålen kommer tillbaka).
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
| Wild Carrot Plant | Plains, Sunflower Plains, Meadow, Flower Forest, Hazel Meadow | 1/12, 5 försök | 1–2 Wild Carrot med kniv/skära, 1 i 50 % för hand |
| Sea Beet | Beach, Stony Shore (på sand, grus, sten eller jord) | 1/2, 10 försök, spridning ±4 | 1–3 Sea Beet Leaves; Beetroot Seeds ~45 % med kniv/skära, 15 % för hand |

Uppmätt i en ny värld (193×193 block): slätt 42 Wild Einkorn + 21 Wild Emmer, savann 45 Wild Einkorn, meadow 16 Wild
Einkorn + 13 Wild Pea, flower forest 14 Wild Emmer, forest 4 Wild Pea, öken 0.

Uppmätt (seed 4242, 9×9 chunks runt biomen): beach 19 Sea Beet, stony shore 40; meadow 9 Wild Carrot Plant, slätt 15,
flower forest 25.

Barley har ingen vild källa än (bara creative); kommer med handel/byar senare.

## Växtvillkor (byggt)

Alla grödor (även vanilla) går genom NeoForges `CropGrowEvent`. Vanillas egen tillväxtchans gäller fortfarande; vår
faktor multipliceras på: **temperaturfaktor × säsongsfaktor × fukt × bördighet × dagsljus**. Temperaturen är `Temperature.ambient`
(0–20; slätt ~12,4, taiga 8, snö 2–6, savann/öken 22, natt −5 under bar himmel, regn −2).
Ingen delning med dagens längd (dagarna är 3× vanillas): tidigare delades chansen med 3 och ett vetefält tog ~5 timmars
spel att mogna. Nu växer det i vanilla-takt per riktig minut, men bara på dagen: runt 1–2 timmar för vete på fuktig jord.

| Gröda | Växer (temp) | Bäst | Frost dödar under | Övrigt |
|---|---|---|---|---|
| Wheat / Einkorn / Emmer | 7–17 | 10–14 | 2 | |
| Barley | 5–16 | 8–12 | 0 | tålig, växer (långsamt) i snöbiomer |
| Carrot / Wild Carrots | 4–15 | 7–12 | −1 | tål kyla, rotfrukt |
| Potato | 6–16 | 9–13 | 2 | rotfrukt |
| Beetroot | 5–16 | 8–13 | 1 | rotfrukt |
| Peas | 6–16 | 9–13 | 2 | skörden höjer bördigheten |
| Flax (bronsåldern) | 5–16 | 8–13 | 0 | som korn; se "Lin" nedan |
| Melon / Pumpkin (stjälk) | 12–**25** | 15–20 | 6 | maxgräns 25 i stället för 22: savann och öken ligger på exakt 22 och hade annars fått noll |

- **Temperaturfaktor:** 1 i bästa spannet, linjärt ner till 0 vid kanterna.
- **Säsongsfaktor:** varje gröda har en profil per säsong (vår, sommar, höst, vinter), utöver temperaturen. Den ligger
  i samma tabell som temperaturen (`Farming.climate`, sista fältet i `Climate`). En ny gröda lägger bara till en rad;
  utan egen profil gäller standarden 1.0 / 1.2 / 0.6 / 0. På vintern växer ingenting (0), och fältinfon säger då
  "resting till spring". Frosten är oförändrad (grödans frostgräns ovan).

| Gröda | Vår | Sommar | Höst | Vinter |
|---|---|---|---|---|
| Wheat / Einkorn / Emmer (standard) | 1.0 | 1.2 | 0.6 | 0 |
| Barley | 1.2 | 1.0 | 0.7 | 0 |
| Carrot / Wild Carrots | 1.0 | 1.0 | 0.8 | 0 |
| Potato | 0.9 | 1.2 | 0.5 | 0 |
| Beetroot | 1.0 | 1.1 | 0.8 | 0 |
| Peas | 1.2 | 1.0 | 0.4 | 0 |
| Flax | 1.1 | 1.2 | 0.4 | 0 |
| Melon / Pumpkin | 0.5 | 1.3 | 0.6 | 0 |

  Advancement **Out of Season** (Food & Farming): smyg-högerklicka en gröda på vintern.
- **Dagsljus:** växer bara på dagen med himmelsljus ≥ 9. Facklor räcker inte, och ingenting växer på natten.
- **Fukt:** fukt 0 → ingen tillväxt.
- **Frost:** på natten, under bar himmel, när luften är under grödans frostgräns: 10 % chans per random tick att en ung
  planta dör → **Withered Crop** (ger 50 % Straw, annars Plant Fibre). Mogna plantor dör inte.
- **Torka:** unga plantor på torr farmland (fukt 0): 5 % chans per random tick att vissna.
- **Rotröta:** rotfrukter på helt blöt farmland (fukt 7) i träsk eller i regn: 1 % chans per random tick.
- **Smyg + högerklicka med tom hand** på farmland eller en gröda: "Fertility 2/3, moisture 7/7", och för en gröda
  dess temperatur, spann och varför den inte växer (too cold / too hot / too dry / needs daylight / resting till
  spring / ripe).

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
- **Mouldboard Plough** (järnåldern, `PloughEntity`): en hästdragen plog som plöjer en 5 block bred remsa till farmland
  med **bördighet 3** (en över handplöjd åker, taket är 3). Bara Grass Block, Dirt, Dirt Path, Podzol och Loess med luft
  ovanför; befintlig åker och grödor lämnas i fred. Se "Plogen" i [ages/03-iron-age.md](ages/03-iron-age.md).
- **Skörd** av en mogen gröda: −1 (Peas: +1).
- Bördighet 0: halv tillväxt och halv skörd (varje stack halveras, udda avrundas slumpvis). 1: 75 % tillväxt.
  2: normal. 3: efter varje tillväxtsteg 30 % chans på ett steg till (~1,3× fart), och 30 % chans på en extra vid skörd.
  Uppmätt: 24 mogna vete gav 11 / 24 / 29 kärvar på bördighet 0 / 2 / 3.
- **Syns på blocket:** 0 = blek och gråaktig, 1 = lite blekare, 2 = som vanilla, 3 = mörk mylla med fläckar av
  kompost (torr och fuktig variant). Texturer + blockstate-override: `scripts/gen_farmland_textures.py`.
- **Återställa:**
  - **Träda:** farmland utan något ovanpå får +1 med chansen 1/18 per random tick (~1 per Minecraft-dag).
  - **Compost** (bronsåldern): Compost Heap fylls med 7 komposterbara saker (taggen `mechanical:compostable`: Leaf Litter,
    Straw, Plant Fibre, löv, saplings, gräs, ormbunke, ruttet kött, Burnt Food, Bark, Fish Bones, melonskivor, rötter,
    äpplen), ruttnar 2 min, ger 2 Compost. Högerklick på farmland: +1.
  - **Manure** (bronsåldern): +2. Släpps av boskap (se "Bronsåldern fas 3").
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
| Bronze Sickle (bronsåldern) | Tool Station: Bronze Sickle Blade (gjuten i Clay Sickle Blade Mold, slipad) + Twine + Stick | Samma bonusar som flintskäran men 80 % chans på extra kärve (flint 50 %), och skär **3×3** mogna grödor eller gräs med ett drag (smyg: ett). Hållbarhet 460. Se docs/ages/02-bronze-age.md "Fler bronsverktyg" |
| Flail | `[ ][Stick]` / `[Twine][Stick]` / `[Stick][ ]` | Tröska. Hållbarhet 64 |
| Crop Stakes | `[Stick][ ][Stick]` / `[Stick][Twine][Stick]` → 2 | På farmland: högerklicka en mogen gröda med tom hand eller skära → skördas och sås om, men varje item i skörden bara 60 % chans. Med skära: dess Straw och chans på extra kärve (flinta 50 %, brons 80 %) efter avdraget, och skäran slits 1. Går sönder och tappas igen när farmlanden bryts, trampas eller torkar |
| Raw Porridge | 2 Grain + Clay Water Bucket + Bowl | Kokas till Porridge |
| Quern | `[ ][C][ ]` / `[Cobblestone][Stick][Cobblestone]` / `[Cobblestone]×3` | Mal Grain → Flour |
| Baking Stone | `[R][C][R]` | Baka Dough → Bread |
| Compost Heap | `[Stick][ ][Stick]` / `[Stick][Leaf Litter][Stick]` / `[Stick][C][Stick]` | Kompostera |
| Scarecrow | `[C][Carved Pumpkin][ ]` / `[Straw][Raw Hide][Straw]` / `[ ][Stick][ ]` | Håller kråkor borta |
| Straw | tröskning, skära | Kompost, fågelskrämma |

## Bronsåldern fas 3 (byggd)

C = **Copper Ingot eller Bronze Ingot** (även Poor Bronze), R = rock chunk (`#mechanical:rock_chunks`). Alla fyra är
vanliga shaped-recept (kategori misc) och visas i receptboken först när man bär en Copper eller Bronze Ingot (advancement
`farming_bronze_age`), inte redan när man bär kullersten eller pinnar.

| Sak | Recept | Varför metallen |
|---|---|---|
| Quern | `[ ][C][ ]` / `[Cobblestone][Stick][Cobblestone]` / `[Cobblestone][Cobblestone][Cobblestone]` | metallaxel genom stenarna |
| Baking Stone | `[R][C][R]` | kopparplåt mellan stenarna |
| Compost Heap | `[Stick][ ][Stick]` / `[Stick][Leaf Litter][Stick]` / `[Stick][C][Stick]` | kopparspik håller ihop flätverket |
| Scarecrow | `[C][Carved Pumpkin][ ]` / `[Straw][Raw Hide][Straw]` / `[ ][Stick][ ]` | en kopparbjällra på armen |
| Dough | 2 Flour + Clay Water Bucket (fanns redan, nu nåbart) | |
| Compost | ingen crafting: från Compost Heap | |
| Manure | ingen crafting: från boskap | |

- **Manure:** vuxna djur i entity-taggen `mechanical:drops_manure` (Cow, Mooshroom, Sheep, Pig, Goat, Horse, Donkey,
  Mule, Aurochs, Wild Horse) släpper då och då en Manure med ett blött ljud: var 200:e tick (förskjutet per djur) 1/60
  chans, alltså i snitt **1 per djur var 10:e minut**. Ungar släpper inget. Gäller överallt, inte bara i fållor (beslut:
  enklast, och sakerna despawnar efter 5 min som annat på marken); vilda uroxar och hästar ger alltså också gödsel.
- **Manure i Compost Heap:** Manure finns i `mechanical:compostable` och fyller högen **2 steg** (allt annat 1), så den
  snabbar på komposten. Spoiled Food (från förskämd mat) finns också i taggen (gen_preserving). Manure direkt på åkern ger
  +2, så att kompostera den lönar sig bara när man har mer gödsel än åker.
- **Hållbarhet:** Flour 30 dagar (torr), Dough 1 dag, Bread 3 dagar (som tillagat kött). Ruttnar till Spoiled Food.
- **Bröd:** Quern mal 1 Grain → 1 Flour (4 klick, överstenen vrids ett kvarts varv per klick, kvarnljud och korn som
  yr). 2 Flour + Clay Water Bucket → Dough. Baking Stone vid brinnande Campfire: 30 s → Bread, 45 s till → Burnt Food.
  Clay Kiln: Dough → Bread på 20 s vid 200°.

## Rotfrukter (byggt)

Morötter, rödbetor och potatis fick egna källor (beslut 2026-11-19), så de inte bara kommer från döende byfält.

- **Wild Carrot Plant** (Daucus carota, vit flockblomma): se tabellen ovan. Ger **Wild Carrot**: tunn, blek rot, mat
  (1 hunger, 0,1 mättnad). Sås på farmland som grödan **Wild Carrots** (samma växtvillkor som Carrot). Mogen: 1–2 Wild Carrot
  + **25 % chans på en Carrot** ("tämjning": det vilda utsädet finns alltid kvar, och de riktiga morötterna kommer en och
  en, sedan odlas de som vanligt). Valt i stället för att Wild Carrot blir vanilla-morötter direkt: det följer
  Einkorn/Emmer (odlad vild typ ger vild skörd) och gör en riktig morot till ett litet framsteg.
- **Sea Beet** (strandbeta): se tabellen ovan. **Sea Beet Leaves** är blad att äta (1 hunger, 0,1 mättnad, håller 1 dag).
  **Beetroot Seeds** gör att rödbetor kan odlas utan byar.
- **Potatis** kommer från Amerika och växer inte vilt här. Borttagen ur byarnas kistor, byarnas fält (blir morötter),
  zombiebyarnas fält, all strukturloot (shipwreck, pillager outpost, trial chambers, ancient city), zombiernas/huskernas
  sällsynta drops (`gen_data.py`, `NOT_NATIVE`). Bondens handel hade redan ingen potatis. **Enda källan:** en stam-Elder
  kan ha ett extra byte, **4 Leather + 1 Antler → 3 Potato** (1 gång per dag): 1 Elder av 8, i en Traders-stam 1 av 3
  (`Tribesperson.POTATO_ODDS`, slumpat per Elder; uppmätt 47–55 av 400). Elders som redan sparat sina byten får det inte.
- **Root Cellar** (stenåldern): `[Planks][Straw][Planks]` / `[Planks][ ][Planks]` / `[Planks]×3`. En plankbinge med halm
  och lock, 9 platser, öppnas som Storage Jar (locket står upp när den är öppen; hoppers och komparator fungerar).
  **Nedgrävd** (himmelsljuset på alla sex sidor av blocket är högst 7: täckt grop, källare i en backe) åldras
  rotfrukter och blad (taggen `mechanical:cellar_produce`: Carrot, Potato, Beetroot, Wild Carrot, Sea Beet Leaves) med
  **1/4** av normal takt, annan mat med 1/2. Ovan jord är den bara en låda ("Too much daylight" när man öppnar den).
  Valt framför en lerkruka: Storage Jar finns redan i bronsåldern (1/4 för allt), källaren gör rotfrukterna lagringsbara
  redan i stenåldern men kräver att man gräver. Takten avgörs när locket lyfts (genväg, se koden).
- **Hållbarhet** (nytt, de förskämdes inte förut): Carrot, Beetroot, Wild Carrot **10 dagar**, Potato **20 dagar**, Sea
  Beet Leaves **1 dag** (taggarna `perishes_10_days` / `perishes_20_days`).

## Lin (byggt, v0.65.0, bronsåldern)

- **Flax Seeds** köps av byns Farmer på handelsnivå 3 (2 Emerald → 4). Ingen vild källa (lin kommer som odlad gröda).
- Sås på farmland som **Flax** (`mechanical:flax_crop`, ålder 0–7) och följer alla regler ovan: dagsljus, fukt,
  bördighet (−1 per skörd), frost, torka, kråkor, skäran slits.
- **Mogen:** 2 Flax Stalks (skära: +50 % en till) och 1–2 Flax Seeds. **Omogen:** ingenting.
- Stjälkarna röts och skäktas till Flax Fiber och spinns till Linen Thread (`docs/ages/02-bronze-age.md`, "Lin").

## Teknik

- `Farming.java` (registrering, växtregler i `CropGrowEvent`, skörd i `BlockDropsEvent`, kråkor, fältinfo),
  `Fertility.java` + `mixin/FarmlandBlockMixin.java` (bördighet, träda, torka), `FarmCropBlock`, `WildPlantBlock`,
  `FlailItem`, `FertilizerItem`, `QuernBlock`, `BakingStoneBlock`, `CompostHeapBlock`, `ScarecrowBlock`.
  Vattning och hinkens rest i recept: `ClayBucketItem`. Dough i ugnen: `KilnBlockEntity.RECIPES`. Kråkor: `wildlife/FlockBird`.
- Rotfrukter: `Roots.java`, `RootCellarBlock`, `RootCellarBlockEntity` (bygger på `StorageJarBlockEntity`), potatisbytet i
  `Tribesperson.getOffers`; data `scripts/gen_roots.py` (efter gen_birds2, före gen_block_items), texturer
  `scripts/gen_roots_textures.py`.
- Data: `scripts/gen_farming.py` (efter gen_age_recipes, före gen_wind); bronsålderns recept, Manure-taggen och
  hållbarheten: `scripts/gen_farming3.py` (efter gen_wolf_pelt, före gen_block_items, eftersom den lägger till i
  gen_preservings perishes-taggar); texturer: `scripts/gen_farming_textures.py`. Manure från djur: `Farming.dropManure`.

## Att bestämma senare

- Marktyper som ändrar säsongsprofilen (växthus, kompost).
- Bronsålderns bronsskära och plog (snabbare åker, större skörd).
- Källa för Barley (handel, byar).
- Manure bara från djur i fållor eller tamdjur (nu släpper alla vuxna i taggen, även vilda).
- Halmtak och halmbädd av Straw. Scarecrow som bär en Leather Helmet (bara utseende).
- Spelarbyggda Hay Bales ger ruttet hö när de bryts (byarnas regel gäller alla hay bales).
