# Stenåldern

Första åldern. Man börjar med bara händerna. Målet är att ta sig från flinta och fiber till stenverktyg, eld och en
Clay Kiln, och att få fram Crucible som behövs i bronsåldern.

## Progression i korthet

1. Slå på gräs → **Plant Fibre** → **Twine**. Leta **Flint** (lös flinta på marken, grus) och **rocks** (lösa stenar).
2. Slå på löv → **Sticks**.
3. **Flint Knife**, **Flint Axe** och **Flint Pickaxe** (+ Flint Shovel/Hoe). Inget av de tre går att hoppa över.
4. Med yxan: hugg träd → **Firewood** → planks och crafting table. Med **Flint Pickaxe** som hammarsten: knacka
   **Stone Pickaxe Head** / **Stone Axe Head** av rocks.
5. **Tool Station** (byggs av Flint Axe + Flint Pickaxe) → **Crude Stone Pickaxe** och **Crude Stone Axe**.
6. **Cordage** + **Tool Rod** + dubbla huvuden → **Stone Pickaxe** och **Stone Axe**.
7. **Campfire** av Firewood, tänd med **Fire Drill**. Sätt en **Clay Kiln** på den → **Brick**, **Charcoal**, **Crucible**.
8. Mat: döda djur → **Carcass** → stycka med Flint Knife → **meat scraps** (laga på Campfire), **Raw Hide** → **Scraped Hide** →
   **Drying Rack** → **Leather** → Leather Armor.

---

## Material

| Item | Hur man får det | Används till |
|---|---|---|
| Plant Fibre | Slå sönder Short Grass, Tall Grass, Fern, Large Fern: 20 % chans, **60 % med Flint Knife** (kniven slits 1 per gräs) | Twine |
| Twine | 3 Plant Fibre (shapeless) | Flintverktyg, bindning i Tool Station, Fire Drill, Tool Station, Cordage |
| Cordage | 3 Twine (shapeless) | Bindning för Stone Pickaxe / Stone Axe |
| Stick | Vanilla, plus löv släpper Stick oftare (+8 %, totalt ~10 %) | Skaft, flintverktyg, Campfire, Fire Drill |
| Flint | Loose Flint på marken, Gravel (vanilla-chans) | Flintverktyg, Tool Station |
| Stone / Granite / Diorite / Andesite Rock | Loose rocks på marken (se Block) | Knacka stenhuvuden, Clay Kiln. Alla fyra räknas som `#mechanical:rock_chunks` och kan blandas |
| Firewood (14 sorter: Oak, Spruce, Birch, Jungle, Acacia, Dark Oak, Mangrove, Cherry, Pale Oak, Poplar, Aspen, Alder, Beech, Linden) | Alla logs släpper **2 Firewood** av sitt träslag i stället för sig själva (även stripped logs och wood-block). Nether-stammar och bamboo släpper sig själva | Campfire, bränsle och Charcoal i Clay Kiln, 4 → 1 Log |
| Tool Rod | Flint Knife ovanför valfri Log (kniven slits 1) | Skaft för Stone Pickaxe / Stone Axe |
| Clay Ball | Vanilla (lera i vatten) | Clay Kiln, Brick, Unfired Crucible |
| Raw Hide | Styckning av djur (se Mat) | Scraped Hide |
| Scraped Hide | Raw Hide + Flint Knife (shapeless, kniven slits 1) | Läggs på Drying Rack → Leather |
| Leather | Torkad Scraped Hide på Drying Rack (djur släpper inte Leather längre) | Leather Armor-recept | Sys i **Armor Station** med Bone Needle + Wool Thread |
| Leather Armor |
| Wool Thread | Styckning av får (30 % chans med Flint Knife, 1–2 st) | 9 (3×3) → 1 White Wool |
| Feather | Styckning av höns och papegojor, omgång 1 (80 %, 1–2 st) | Arrows |
| Bone, Bone Meal | Bone Pile (låg chans) | |

Texturen för Firewood visar 1 vedträ (1 st), 2 vedträn (2–15 st) eller 3 vedträn (16+ st).

## Verktyg

### Flintverktyg (crafting table)
Bryter som trä (sten → cobblestone, men inte järnmalm). Hållbarhet 48. Lagas med Flint. Svaga vapen
(Flint Axe 4 skada, Flint Knife 2,5).

| Verktyg | Recept |
|---|---|
| Flint Pickaxe | `[Flint][Flint][Flint]` / `[Twine][Stick][ ]` / `[ ][Stick][ ]` |
| Flint Axe | `[Flint][Flint]` / `[Twine][Stick]` (2×2, går i inventoryt: behövs för första stockarna) |
| Flint Shovel | `[Flint]` / `[Twine]` / `[Stick]` |
| Flint Hoe | `[Flint][Flint]` / `[Twine][Stick]` / `[ ][Stick]` |
| Flint Knife | `[Flint][ ]` / `[Twine][Stick]` |

**Flint Knife** är ett verktyg i recept: det ligger kvar i rutan efter craftingen och tappar 1 i hållbarhet. Den ger också
mer Plant Fibre från gräs.

### Stenhuvuden (crafting table, med Flint Pickaxe som hammarsten)
Flint Pickaxe ligger kvar i rutnätet och tappar 1 i hållbarhet per knackning.

| Item | Recept (R = rock, K = Flint Pickaxe) |
|---|---|
| Stone Pickaxe Head (enkelsidigt) | `[R][R][R]` / `[ ][K][ ]` |
| Double Stone Pickaxe Head | `[Stone Pickaxe Head][K][Stone Pickaxe Head]` |
| Stone Axe Head | `[R][R]` / `[R][K]` |
| Double Stone Axe Head | `[Stone Axe Head][K][Stone Axe Head]` |

### Stenverktyg (Tool Station)

| Verktyg | Huvud | Bindning | Skaft | Egenskaper |
|---|---|---|---|---|
| Crude Stone Pickaxe | Stone Pickaxe Head | Twine | Stick | Bryter som sten, långsammare, 80 i hållbarhet |
| Stone Pickaxe (vanilla) | Double Stone Pickaxe Head | Cordage | Tool Rod | Vanilla: 131 i hållbarhet |
| Crude Stone Axe | Stone Axe Head | Twine | Stick | Yxa, 80 i hållbarhet, 5,5 skada |
| Stone Axe (ändrad) | Double Stone Axe Head | Cordage | Tool Rod | **250** i hållbarhet (vanilla 131), **6,5** skada (vanilla 9) |

Crude-verktygen lagas med rocks.

### Flint Spear (Tool Station)
Huvud: **Flint**, bindning: **Cordage**, skaft: **Tool Rod**. Fungerar som vanilla Stone Spear: 2 skada vid stöt (vänsterklick), håll in högerklick och spring för ansats (skadan beror på farten). Hållbarhet 100.
Låses upp i Tool Station när man har Flint. Ersätter Stone Spear (vanilla-receptet borttaget).

### Crude Bow och Crude Fishing Rod (Tool Station)
Vanilla Bow och Fishing Rod går inte att crafta (de riktiga kommer i senare åldrar).

| Verktyg | Huvud | Bindning | Skaft | Låses upp av | Egenskaper |
|---|---|---|---|---|---|
| Crude Bow | Cordage (sträng) | Twine (grepp) | Tool Rod | Cordage | Som en båge, men pilarna gör 70 % skada. Hållbarhet 96 (vanilla 384) |
| Crude Fishing Rod | Flint (krok) | Twine (lina) | Tool Rod | Twine | Som ett fiskespö. Hållbarhet 24 (vanilla 64) |

### Tusk Dagger (Tool Station)
Huvud: **Tusk**, bindning: **Cordage**, skaft: **Leather** (handtag). Låses upp när man har en Tusk. En kniv (fungerar i alla
knivrecept och för styckning): 1,3× snabbare och 1,1× mer kött än Flint Knife, 140 i hållbarhet, 3 skada, attackfart 2,2.
**Jägarkniv:** kan stycka **Brown Bear** (Flint Knife kan inte) och ger **1,6× mer i första omgången** (hudar, päls, ull,
fjädrar): högre chans och fler, t.ex. Bear Pelt 1–2 i stället för 1.

### Bone Hook Fishing Rod (Tool Station)
**Bone Fish Hook** = Fish Bones + kniv (crafting). Spöet: huvud **Bone Fish Hook**, bindning **Twine**, skaft **Tool Rod**.
Napp kommer 5 s snabbare (som Lure I, utöver förtrollningar). Hållbarhet 48.

### Fletching Table (ändrad)
Högerklicka för att öppna (smyg + högerklick för att placera block mot den). Ser ut som Tool Station: **listan till vänster
visar allt man kan göra** (alla recept upplåsta från början), tänd när man har delarna, håll musen över för att se delarna,
klicka för att lägga i dem. Tre platser + resultat:

| Plats 1 | Plats 2 | Plats 3 | Ger |
|---|---|---|---|
| Flint | Stick | Feather | 4 Arrow (vanilla-receptet borttaget) |
| Fish Bones eller Bone | Stick | Feather | 4 **Bone Arrow** (1,5 skada, vanlig pil 2) |
| Bear Claw | Stick | Feather | 4 **Barbed Arrow** (3 skada) |
| Tallow | Arrow | Twine | 1 **Fire Arrow** (sätter målet i brand) |
| Sinew | Crude Bow | Cordage | **Sinew Bow** (full pilskada, 200 i hållbarhet) |

### Armor Station
Recept: `[Leather][Bone Needle][Leather]` / `[Planks][Crafting Table][Planks]`. Fungerar som Tool Station (lista till
vänster, tre platser, resultat). **Bone Needle** = Fish Bones, Bone eller Horn + kniv (crafting), 32 i hållbarhet; den
ligger kvar i stationen och tappar 1 per rustningsdel.

| Rustning | Material | Verktyg | Tråd | Låses upp av |
|---|---|---|---|---|
| Leather Helmet / Chestplate / Leggings / Boots | 5 / 8 / 7 / 4 Leather | Bone Needle | 2 / 4 / 3 / 2 Wool Thread | Leather |
| Fur Hood / Coat / Leggings / Boots | 1 / 3 / 2 / 1 Bear Pelt | Bone Needle | 1 / 3 / 2 / 1 Sinew | Bear Pelt |

Vanilla-recepten för läderrustning är borttagna.

### Fur-rustning och Bear Rug
- **Fur Hood / Coat / Leggings / Boots:** sys i Armor Station (se ovan). Skydd 1/3/2/1, lagas med Bear Pelt.
  **Isolerar 25 % per del mot kyla** (läder 15 %).
- **Bear Rug:** Bear Pelt + Bear Claw. Platt dekoration (som en matta) med huvudet bort från den som placerar den.

### Ägg
- **Boiled Egg:** valfritt ägg (vanligt, blått, brunt) på en Campfire (15 s). Stackar 16. Går inte att äta med skalet på.
- **Skala:** Boiled Egg i craftingrutan → **Peeled Egg** (3 hunger) + **Eggshell**.
- **Eggshell** fungerar som Bone Meal (högerklicka på växter).

### Tallow och ljus
- **Tallow:** Lard (1) eller Bear Fat (2) på en Campfire (20 s).
- **Candle:** `[Twine]` / `[Tallow]` / `[Tallow]` → 2 Candle (vanilla-receptet med Honeycomb borttaget). Tänds med
  **Fire Drill** (60 %). Monster slår inte sönder ljus, men en zombiehord blåser ut dem.

### Fire Drill
Recept: `[Twine][Stick]` / `[Stick][ ]`. Hållbarhet 16.
Högerklicka på en släckt Campfire eller Clay Kiln. Lyckas ungefär var tredje gång (33 %, fräsljud), annars lite rök.
**Tändmaterial:** högerklicka först med **Leaf Litter** eller **Bark** på den släckta elden (1 åt gången, gröna partiklar).
Då lyckas Fire Drill i 75 % av försöken. Tändmaterialet går åt när elden tar sig. Varje
försök kostar 1 i hållbarhet. En Clay Kiln går bara att tända om den har bränsle (eller rest av påbörjat bränsle).

### Verktygstips med stats
Alla verktyg visar sina värden direkt under namnet: hållbarhet (alltid, färgad efter hur sliten), bryt-/hugg-/grävfart,
vad en hacka kan bryta, yxans hugg per steg, chans att lyckas och Firewood-bonus, knivens styckningsfart, köttutbyte,
hud-/pälsutbyte och om den klarar björn, bågens pilskada, pilars skada, fiskespöns napptid och rustningens isolering mot kyla.

## Block

### Loose rocks
Ligger på marken i 5 varianter (Stone, Granite, Diorite, Andesite, Flint) och 3 storlekar.
- Spawnar bara ovanpå Grass Block och Stone (Granite/Diorite/Andesite även på sin egen bergart).
- Liten och mellan: plockas med ett slag → 1 / 2 chunks. Stor: kräver pickaxe → 3–4 chunks.
- Loose Flint ger Flint i stället för chunks.
- Har inget item, finns bara i världen.

### Tool Station
Recept: `[Flint Axe][Twine][Flint Pickaxe]` / `[Planks][Crafting Table][Planks]` (flintverktygen går åt).

GUI: lista till vänster med upplåsta verktyg, i mitten tre platser (huvud, bindning, skaft) och en resultatruta.
- Ett verktyg låses upp första gången man har dess material i inventoryt (stenverktyg: valfri rock). Upplåsningen
  sparas per spelare och finns kvar efter döden.
- Verktyg där man har alla delar är upplysta. Tooltip visar varje del i grönt/rött.
- Klick på ett upplyst verktyg flyttar delarna från inventoryt till platserna.
- Listan visar 9 verktyg åt gången: scrolla med mushjulet eller dra i scrollbaren.
- Egen textur (bänk med verktyg).

### Campfire (ändrad)
Recept: `[ ][Stick][ ]` / `[Stick][ ][Stick]` / `[Firewood][Firewood][Firewood]` (valfritt träslag, kan blandas).
- Placeras **släckt**. Tänds med Fire Drill.
- Går sönder till **1–3 Stick** när den hackas (vanilla: 2 Charcoal). Den kommer inte tillbaka, utan måste byggas om.
- **Brinner i 3–5 minuter**, sedan blir den **glöd** (glödande stockar, inga lågor, gnistor och rök, lyser svagt med ljusnivå 1). Glöden räcker
  i 2 minuter, sedan är den helt slocknad och måste tändas med Fire Drill igen.
- **Kyla och regn gör att elden brinner ut fortare** (gäller även glöden och bränslet i Clay Kiln): vid lagom temperatur
  normal tid, en snönatt upp till 2,5× fortare, regn på elden +1× till.
- **Högerklick med Firewood:** på glöd → brinner igen direkt. På en brinnande eld → +1 minut (max 10 min).
- **Matlagning:** maten ligger kvar på elden när den är klar. Efter 30–60 s till blir den **Burnt Food**.
  Högerklicka med tom hand för att ta av maten (klar eller bränd först).

### Clay Kiln
Recept: `[Clay Ball][Chunk][Clay Ball]` / `[Clay Ball][ ][Clay Ball]` / `[Chunk][Chunk][Chunk]`.

- **Placeras bara ovanpå en Campfire.** Campfire blir ugnens nedre halva (eldstad) och ugnen blir 2 block hög.
- **Måste brännas först:** en ny ugn är av rå lera (grå) och bakar ingenting. En eld måste brinna i den i **2 min**
  (lägereldens ved eller bränsle i ugnen; bränsle går åt under tiden, och den slocknar inte av att stå tom). Leran
  mörknar i 3 steg nerifrån och upp till bränd rödbrun lera; pilen i GUI:t visar hur långt det kommit. Slocknar elden
  behålls det som redan bränts. En bränd ugn förblir bränd även om man bryter och sätter ut den igen (item:et visar det).
  Står den på en tänd Campfire när man bygger den bränns den på lägereldens ved.
- Var Campfire tänd fortsätter ugnen brinna på lägereldens egen ved (3 min bakning).
- Annars: lägg bränsle och tänd med Fire Drill.
- Bränsle går **bara åt medan något bakas**. Står den tänd utan att baka pyr den i 60 s och slocknar sedan. Påbörjat
  bränsle sparas till nästa tändning.
- Slocknar om bränslet tar slut mitt i en bakning.
- Syns: flammor ur eldöppningen (mer när den bakar), rök ur skorstenen, lyser (ljus 13).
- Bryter man någon halva försvinner ugnen och man får tillbaka Clay Kiln + 1–3 Stick (eldstaden går sönder) + innehåll.
- GUI som en Furnace: input, bränsle, resultat.

**Recept i Clay Kiln**

| In | Ut | Tid |
|---|---|---|
| 1 Clay Ball | Brick | 20 s |
| 1 Log (alla som brinner) | Charcoal | 30 s |
| 2 Firewood | Charcoal | 30 s |
| 1 Unfired Crucible | Crucible | 40 s |
| 1 Unfired Clay Bucket | Clay Bucket | 30 s |

**Bränsle**

| Bränsle | Brinntid |
|---|---|
| Stick, Sapling | 10 s |
| Planks | 30 s |
| Firewood | 1 min |
| Log | 2 min |
| Charcoal, Coal | 4 min |

### Drying Rack
Recept: `[Stick][Twine][Stick]` / `[Stick][ ][Stick]`.
- Högerklicka med **Scraped Hide** för att spänna upp den. Efter några minuter (slumpmässigt, ~3 min i snitt) är den torr.
- Högerklicka på en torr hud → **Leather**.
- Bryts den släpper den sig själv + hud/Leather som satt på.

### Stumps
När ett träd fälls blir en stubbe kvar där man högg (14 träslag, samma som Firewood). Ger 1–2 Sticks, bryts snabbast
med yxa. Har inget item. Fungerar som **huggkubbe** för Rough Logs (se Bearbeta stockar).

### Creative
Alla moddens block finns som items i creative-fliken (buskar, svamp, Twigs, Leaf Litter, stubbar, Fallen Logs,
Ancient Oak-block ...), även de som inte går att få i survival.

## Övriga items

| Item | Hur | Används till |
|---|---|---|
| Unfired Crucible | `[Clay Ball][ ][Clay Ball]` / `[Clay Ball][ ][Clay Ball]` / `[Clay Ball][Clay Ball][Clay Ball]` | Bränns till Crucible i Clay Kiln |
| Crucible | Clay Kiln | Bronsåldern (smälta koppar + tenn). Ingen användning än |
| Unfired Clay Bucket | `[Clay Ball][ ][Clay Ball]` / `[ ][Clay Ball][ ]` | Bränns till Clay Bucket i Clay Kiln |
| Clay Bucket / Clay Water Bucket | Clay Kiln | Som en hink men **bara vatten**. Tål 16 hällningar, sedan spricker den. Fyller inte kittlar |
| Log (från Firewood) | `[Firewood][Firewood]` / `[Firewood][Firewood]` av samma träslag → 1 Log | Planks, Tool Rod, bränsle |

## Mekaniker

### Stockar och trädfällning
- **Logs kan inte brytas utan yxa** (minst Flint Axe).
- Hugger man en log i ett **naturligt träd** (logs med naturliga löv) med yxa fälls hela trädet ovanför huggstället:
  - Hugg-tiden delas med antalet logs (stort träd = lång tid).
  - Yxan slits 1 per log. Räcker den inte går den sönder, men trädet faller ändå.
  - Trädet faller som en animation bort från spelaren och **blir liggande där det landar** (gravitation, staplas):
    stammen blir **Fallen Logs** som ligger längs fallriktningen. Löven släpper saplings, sticks och äpplen som innan och
    blir **Leaf Litter** på marken där de landar (flera löv på samma ställe ger tjockare lager). Varje lövsort har sin
    egen **färska leaf litter** i lövens färg (18 sorter, även poppelns gula/orange/röda och azalea); den **torkar** med
    tiden till vanlig brun Leaf Litter. Bryter man den får man vanlig Leaf Litter (tändmaterial).
  - Bara löven som hör till just det trädet tas med (grannträd lämnas).
  - En Stump blir kvar.
- **Planks och Crafting Table kräver också yxa:** bryts de utan yxa splittras de till 1–2 Sticks (ingen genväg via
  byarnas hus).
- **Smyg** för att hugga en enda log (ger 2 Firewood). Hus av logs fälls aldrig (placerade löv räknas inte som naturliga).

### Skogen (ändrad)
- **Oak är stora träd**: grenade ekar med 9–15 block hög stam, kronan börjar högt upp så man ser under den. Gäller alla
  ekar (skog, slätt, saplings).
- **Birch är högre och smalare**: 9–12 block stam (tall birch upp till ~18), kronan högst upp.
- **Fyra nya träslag** med full träuppsättning (Log, Wood, Stripped Log/Wood, Planks, Slab, Stairs, Fence, Fence Gate,
  Door, Trapdoor, Button, Pressure Plate, Leaves, Sapling, Potted Sapling; inga skyltar eller båtar än). De fälls som
  andra träd och har egna Stump, Fallen Log, Rough Log och Firewood. 1 Log → 2 Planks som alla träslag. Löven ger
  Sapling och Sticks men inga äpplen.
  - **Aspen:** hög, smal och rak stam (9–13 block), liten krona. Forest, Flower Forest, glest i Meadow och Taiga.
  - **Alder:** medelstort rakt träd (6–9 block). River-stränder och Swamp, ibland i Forest och Flower Forest.
  - **Beech:** stort träd, rak hög stam (8–12 block), bred och tät krona. Forest och Flower Forest.
  - **Linden:** stort grenat träd med bred krona. Forest och Flower Forest, sällsynt på Plains.
  - Birch Forest och Taiga behåller sina vanliga träd. Vanilla-träden är kvar överallt.
- **Twigs:** små kvistar på marken i skog och taiga. Slå sönder → 1 Stick. Inget block att placera.

### Skogens undervegetation
Skogarna har fått buskar och sly, i grupper. Inget har kollision (man kan gå igenom), inget kan planteras än.

| Block | Var | Ger |
|---|---|---|
| **Hazel Shrub** (hassel, 2 block) | Lövskog, björkskog, blomsterskog | Högerklicka när nötterna syns: 1–2 Hazelnuts (växer tillbaka på ca 23 min i ljus). Bryts: 1–3 Sticks, ibland en nöt |
| **Juniper** (en) | Taiga, snötaiga, gammal taiga, blåsiga berg | Högerklicka: 1–3 Juniper Berries (tillbaka på ca 14 min). Bryts: 1–2 Sticks |
| **Bramble** (snår) | Lövskog, mörk skog, ibland slätt | Saktar ner och sticks (1 skada ibland, inte om man smyger). Plant Fibre, ibland en Stick |
| **Undergrowth** (ris, ormbunkar) | Tätt i alla lövskogar | Halv chans till Plant Fibre |
| **Sapling Thicket** (sly, 2 block) | Lövskog, björkskog | 2–3 Sticks |

Mat: **Hazelnuts** (2), **Roasted Hazelnuts** (4, rostas på Campfire), **Juniper Berries** (1). Nötter och bär får plats i Bark Basket.

### Bearbeta stockar (håll in högerklick med yxa)
Ett tungt hugg var 0,45:e sekund (långsam svingning, sprickor i blocket). Varje steg kräver ett antal hugg beroende på yxan
(Flint Axe / Crude Stone Axe 3, Stone Axe 2, Iron Axe 2) och sliter yxan 1.

1. **Fallen Log:** barken av (ger **1–2 Bark**) → hugget smalare → ännu smalare → klar. Varje gång den blir smalare
   faller **1–2 Sticks** av och ibland en **Rough Log** (halva lyckochansen). Sista steget ger en **Rough Log** om det
   lyckas, annars bara 2–3 Sticks. En stock kan alltså ge upp till 3 Rough Logs.
2. **Högerklicka en Rough Log på en Stump:** den står upp på stubben (tom hand tar tillbaka den).
3. **Hugg loggen på stubben:** lyckas → **2–3 Firewood** av det träslaget (+1 med Stone/Copper Axe, +2 med Iron Axe,
   +3 med Diamond Axe), misslyckas → 1 Firewood + 1–2 Sticks.

Chans att lyckas: Flint Axe 60 %, Crude Stone Axe 65 %, Stone Axe 75 %, Iron Axe 95 %.
Bryter man en Fallen Log med vanlig brytning blir det bara 1–2 Sticks.

Nya items: **Rough Log** (14 träslag, stackar 16), **Bark** (tändmaterial, bast, korg; senare garvning).

**Bark används till:**
- **Bast:** Bark + kniv (crafting, kniven slits 1) → **2 Plant Fibre**.
- **Bark Basket:** `[ ][Cordage][ ]` / `[Bark][ ][Bark]` / `[Bark][Bark][Bark]`. Högerklicka för att öppna (3×3). Rymmer bara
  frukt, bär och hudar: Apple, Golden Apple, Sweet Berries, Glow Berries, Melon Slice, Chorus Fruit, Leather, Rabbit Hide,
  Raw Hide, Scraped Hide. Innehållet sparas i korgen (stackar inte, 1 per plats i inventoryt).
**Leaf Litter** (från fällda träds löv) används som tändmaterial och blir kompost när odlingen kommer.

### Mat och styckning
- **Alla vilda, vuxna passiva djur** (kor, grisar, får, höns, kaniner, getter, hästar, lamor, rävar, vargar, isbjörnar …)
  släpper **ingen loot** när de dör. När dödsanimationen är klar ligger djuret kvar som en **Carcass** där kroppen
  landade, på sidan med benen och huvudet hängande mot marken. Den försvinner efter 5 minuter. Tama djur, ungar och
  zombie/skeleton horses beter sig som vanligt. XP släpps som vanligt.
- **Håll inne högerklick** på kroppen med en kniv → armrörelse, partiklar och ljud. Styckningen sker i **två omgångar**
  (håll kvar för att gå direkt vidare). Varje omgång kostar kniven 1 i hållbarhet. Tittar man bort sparas arbetet.
  1. **Det yttre:** får klipps (Wool Thread), höns och papegojor plockas (Feathers), kaniner flås (Rabbit Hide), övriga
     flås (Raw Hide). Kroppen ser sedan klippt (får) eller flådd ut (egna flådda köttexturer för alla djur, `textures/entity/skinned/`).
  2. **Det inre:** köttet (och Raw Hide för får, Rabbit's Foot för kaniner). Kvar blir en **Bone Pile**.
- Flint Knife: ~2 s per omgång (längre för stora djur). Bättre knivar (senare metaller) är snabbare och ger mer.

**Utbyte med Flint Knife**

| Djur | Omgång 1 | Omgång 2 |
|---|---|---|
| Cow, Mooshroom | Raw Hide 30 % | 1–3 Beef Scraps |
| Pig | Raw Hide 25 % | 1–3 Pork Scraps |
| Sheep | Wool Thread 1–2 (30 %) | 1–2 Mutton Scraps, Raw Hide 25 % |
| Chicken | Feather 1–2 (80 %) | 1 Chicken Scraps |
| Parrot | Feather 1–2 (80 %) | 1 Game Meat Scraps |
| Rabbit | Rabbit Hide 30 % | 1 Rabbit Scraps, Rabbit's Foot 5 % |
| Övriga små djur | Raw Hide 10 % | 1 Game Meat Scraps |
| Övriga mellanstora | Raw Hide 25 % | 1–2 Game Meat Scraps |
| Övriga stora (häst, lama, kamel, isbjörn …) | Raw Hide 40 % | 2–3 Game Meat Scraps |

### Blod
- Djur **och monster** blöder när de träffas (blodpartiklar, fler vid hårda slag). Monster blöder likadant som djur
  (droppar, spår, blödning under 50 %). Undantag utan blod (taggen `mechanical:bloodless`): alla skelett, Slime, Magma
  Cube, Blaze, Breeze, Creeper, Vex, Ghast, Shulker, Wither, golems, Creaking, Endermite och Silverfish.
- **Under 50 % hälsa blöder de:** droppar blod, lämnar **bloddroppar på marken** när de rör sig (går att spåra) och
  tar en halv hjärta var 5:e sekund. Blödningen slutar på 1 hjärta, den dödar aldrig.
- Styckning sprutar blod och lämnar blodfläckar runt kroppen (en pöl runt Bone Pile).
- **Pilar fastnar** i djur och syns i kroppen (på våra vilda djurs modeller). Varje pil får djuret att **blöda tills
  det dör**: det springer i panik, sprutar mer och mer blod och tappar liv allt snabbare (1 hp per pil, var 3:e sekund
  i början och sedan varje sekund; Barbed Arrow blöder dubbelt). Följ blodspåret.
- När djuret dör **faller pilarna ut** bredvid kroppen så du kan plocka upp dem. Dåliga pilar kan ha gått sönder och
  blir då en Feather: Arrow 30 %, Bone Arrow 50 %, Fire Arrow 70 %, Barbed Arrow 15 %.
- **Blod på blod blir en större pöl** (4 storlekar). En full pöl **rinner över** till ett grannblock (eller ner ett steg).
  Pölar torkar in steg för steg.
- Blod på marken (blocket **Blood**) ersätter gräs, Leaf Litter och liknande, bleknar bort efter några minuter, går att gå igenom och har inget item. Blodet syns som droppar som faller (egen partikel).

### Vilda djur (skog)
Spawnar i skogsbiomer (`#minecraft:is_forest`). Förökar sig inte. Alla har egna modeller och texturer, flådda
texturer och fungerar med kroppar, styckning, blod och rädsla.

| Djur | Liv | Beteende | Omgång 1 | Omgång 2 |
|---|---|---|---|---|
| Roe Deer | 16 | Flock 3–5, mycket skygg (1,5× upptäcktsavstånd), snabb, betar | Raw Hide 30 % | 1–2 Venison Scraps, Sinew 40 % |
| Red Deer (hind) | 30 | Flock 2–4, flyr | Raw Hide 45 % | 2–4 Venison Scraps, Sinew 50 % (1–2) |
| Red Deer (stag, 35 %) | 30 | Horn. Skadad → stångar (5 skada) | Raw Hide 45 % | Som hind + **1–2 Antler** |
| Wild Boar | 26 | Grupp 2–4, mindre skygg. Skadad → anfaller (4 skada) med gruppen. I grupp kan den anfalla en spelare som kommer nära (20 % chans) | Raw Hide 35 % | 2–3 Pork Scraps, Lard 50 % (1–2), Tusk 40 % (1–2) |
| Hare | 8 | Ensam, snabb, skuttar och kastar sig i sidled när den flyr | Rabbit Hide 35 % | 1 Rabbit Scraps, Rabbit's Foot 8 % |
| Pheasant | 7 | Trycker i gräset, flyger upp när man kommer inom 5 block (smyg för att komma nära) | 2–3 Feathers | 1 Chicken Scraps |

**Ungar:** spawnar i flocken (den första i en flock är alltid vuxen) och växer upp efter ~20 minuter. De följer de
vuxna, flyr med flocken, anfaller aldrig och lämnar ingen kropp. Skadar man en kulting anfaller de vuxna vildsvinen.
Egna modeller: Fawn (rådjur, prickig), Calf (kronhjort, samma modell större), Piglet (randig), Chick (fasan, dun).
Leveret (hare) är harens modell i mindre storlek med egen textur. Chans per flockmedlem: rådjur 35 %, kronhjort 30 %,
vildsvin 40 %, hare 15 %, fasan 25 %.

Nya items: **Venison Scraps** (1 hunger, tillagad 3), **Sinew**, **Antler**, **Lard**, **Tusk** (material för senare
recept), spawn eggs för alla fem.

### Brown Bear
Skog och alla taigor, sällsynt, och **bara nära vilda bärbuskar** (inom 16 block). Ensam, eller en hona med 1–2 ungar.
Äter gärna mogna bär: går till en mogen buske i närheten och äter upp bären (busken börjar om). 60 liv, 8 skada, svår att knuffa.
- **Mycket aggressiv:** anfaller spelare inom 12 block (5 om man smyger) som den ser, och slår tillbaka när den skadas.
  Ger upp efter 24 block. Snabb i början av jakten, sedan långsammare.
- Reser sig på bakbenen och slår med ramarna. Flyr aldrig och får aldrig panik.
- Skadar man en unge anfaller alla vuxna björnar inom 20 block. Väcker man en sovande björn anfaller den direkt.
- **Kräver Tusk Dagger eller Iron Knife för att styckas** (Flint Knife: "This hide is too tough for your knife").
  Omgång 1: **Bear Pelt** + Raw Hide 40 %. Omgång 2: 4–6 Game Meat Scraps, **Bear Fat** 80 % (1–3), **Bear Claw** 60 % (1–3), Sinew.

### Småvilt i skogen
| Djur | Var | Beteende | Styckning |
|---|---|---|---|
| **Squirrel** | Skog, björkskog, dark forest, taiga (1–2) | Mycket skygg. Skrämd springer den till närmaste träd, **klättrar upp** i kronan, sitter där en stund och klättrar ner med huvudet först | 1 Game Meat Scraps, **Squirrel Pelt** 70 % |
| **Hedgehog** | Skog, blomsterskog; ibland slätt och meadow | Nattaktiv. Flyr aldrig utan **rullar ihop sig** när någon kommer nära (sover hoprullad på dagen). Slår du den med bar hand sticker du dig (1 skada) | 0–1 scraps |
| **Badger** | Skog, dark forest, taiga, i sluttningar (1–2) | Håller sig undan, men **biter hårt** (3) om den skadas eller du kommer alldeles inpå utan att smyga | 1–2 scraps, Raw Hide 35 %, Lard 40 % |

**4 Squirrel Pelt + Twine → Raw Hide** (shapeless), som sedan skrapas och torkas som vanligt.

### Giant Deer (miniboss)
Megaloceros, jättehjorten: sällsynt (vikt 1), alltid ensam, i skog, taiga, slätt och meadow. **Dubbelt så stor som
modellen** (ca 5 block hög med hornen), med enorma skovelhorn. Kliver upp 1,5 block. **120 hp**, slag 10 med kraftig knockback, bossmätare. Anfaller spelare inom 14 block (6 om du
smyger) och får då **röda lysande ögon**. **Blöder inte och flyr inte förrän den har 25 % hp kvar** — då börjar den blöda och flyr i panik. Kräver
minst Tusk Dagger för att styckas: 6–9 Game Meat Scraps, 1–2 Raw Hide, 2–3 Sinew och **alltid en Giant Antler**
(offras vid Offering Altar i Ancient Oak).

**Fäller träd:** när den är arg och springer rakt in i en trädstam fälls hela trädet (som när en spelare fäller det),
åt det håll den springer. Bara riktiga träd med löv, inte timmerväggar, och aldrig Ancient Oak. Följer `mobGriefing`.

**Magisk:** runor i hornen och tagg-spetsarna lyser svagt grönt-guld (syns i mörker), den sprider ett svagt ljus omkring
sig, sporer och glimtar driver runt hornen (en virvel när den laddar), blommor och ormbunkar spirar ibland där den går,
och den blåser ett djupt hornrop när den går till anfall. När den dör: ett grönt-gyllene utbrott och en klingande ton.

### Iron Knife
Järnåldern, **går inte att få tag på än** (finns i creative). Snabbare (2×) och mer utbyte (1,5×) vid styckning än Flint
Knife. Kan stycka Brown Bear. Fungerar i alla knivrecept.

### Vilda djur (slätt, berg, kyla)
Samma system som skogsdjuren (kroppar, styckning, blod, rädsla, ungar, sömn). Egna modeller och texturer.

| Djur | Biomer | Liv / beteende | Omgång 1 | Omgång 2 |
|---|---|---|---|---|
| Aurochs (uroxe) | Plains, Sunflower Plains, Meadow, Savanna. Flock 3–6 | Tjurar (30 %) har stora horn och anfaller när de skadas. Skadar man en kalv anfaller tjurarna | Raw Hide 60 % | 3–5 Beef Scraps, Sinew 50 %, Horn 50 % |
| Wild Horse | Plains, Savanna. Flock 3–6 | Mycket skygg (1,8×) och snabb, slåss aldrig | Raw Hide 50 % | 2–4 Game Meat Scraps, Sinew 50 % |
| Mouflon (vildfår) | Windswept Hills, Meadow, Savanna Plateau, Stony Peaks. Flock 2–5 | Baggar (40 %) med krökta horn. Skygg (1,4×), klättrar | Wool Thread 40 % | 1–3 Mutton Scraps, Raw Hide 30 %, Sinew, Horn 30 % |
| Moose (älg) | Taiga, Old Growth Taiga, Swamp. 1–3 | 50 liv. Tjurar (45 %) med skovelhorn anfaller när de skadas (8 skada) | Raw Hide 70 % | 3–6 Venison Scraps, Sinew 60 % (1–3), tjur: Antler |
| Reindeer (ren) | Snowy Plains, Snowy Taiga, Grove, Snowy Slopes, Ice Spikes. Flock 4–8 | Båda könen har horn. Skygg, slåss aldrig | Raw Hide 45 % | 2–3 Venison Scraps, Sinew 50 %, tjur: Antler |

Nytt item: **Horn** (material för senare recept).

### Natten
- **Sovande djur:** vilda djur lägger sig och sover på natten. Kommer man inom 7 block (2,5 om man smyger) vaknar de och
  flyr i panik. De vaknar också när de skadas och när det blir morgon.
- **Fler monster:** var 5:e sekund är det 45 % chans att ett extra monster spawnar 24–40 block från spelaren (på ytan,
  i mörker, inte nära eld), tills det finns 12 monster inom 48 block. Zombie 50 %, Skeleton 22 %, Spider 16 %, Creeper 12 %.
- **Smygare:** 40 % av alla zombies och creepers smyger först runt spelaren på 12–18 blocks avstånd och tittar i 30–70 s
  innan de anfaller. De anfaller direkt om man slår dem eller går närmare än 5 block.
- **Ingen knockback på monster:** de fortsätter trycka sig fram mot spelaren när man slår dem.
- **Smartare pathfinding:** monster söker längre vägar. Står en med mål stilla i ~2 s försöker den en ny väg, sedan halvvägs
  mot målet, och till sist rakt mot målet (och hoppar när den går in i något).
- **Ljud i mörkret** (klient): ute i mörkret, utan eld nära, hörs ibland något var 15–45:e sekund: fotsteg som kommer
  närmare och stannar, en kvist som knäcks, prassel i buskar, ett avlägset stön, ett skrik, skramlande ben. Bara ljud.
- **Mörkare natt:** på natten ignoreras ljusstyrkeinställningen och himmels- och grundljuset sänks till ~10 %. Utan
  ljuskälla är det nästan svart. Oväder räknas som delvis natt. Blockljus (facklor, eld) påverkas inte.
- **Brinnande eld skyddar:** monster håller sig ~20 block från en brinnande Campfire eller tänd Clay Kiln, siktar inte på
  spelare inom 6 block från elden (skeletons når inte dit) och spawnar inte inom 28 block. Glöd och släckt eld skyddar inte.
- **Blodmåne:** ungefär var sjunde natt (bestämt av världens seed, så en omstart ändrar inget) stiger månen full och
  blodröd vid skymningen och står kvar till gryningen. Himlen, dimman och månskenet blir röda, stjärnorna bleknar och natten
  blir lite ljusare i rött. Vid skymningen kommer ett meddelande ("The moon rises red...") och ett mörkt hornljud. Under
  blodmånen spawnar dubbelt så många extra monster (upp till 20 inom 48 block i stället för 12) och zombiehorderna kommer.
  Testa med `/bloodmoon` (op), som gör kvällens natt till blodmåne.
- **Zombiehorder:** bara under blodmåne, i två vågor (en tidigt på natten och en efter midnatt), med 8–12 zombies per våg
  32–48 block från en spelare. De håller ihop och
  går **först på ljuset**: de söker upp närmaste brinnande Campfire eller Clay Kiln (inom 48 block) och släcker den helt
  (ingen glöd kvar), och slår sönder facklor och lanterns inom 10 block. Först när det inte finns ljus kvar i närheten går
  de mot spelarna. Elden skrämmer inte bort dem.
- **Facklor** skyddar inte och värmer inte. Monster utan mål kan dras till en fackla i närheten (inom 8 block) och slå sönder den.
  Lanterns rörs inte.
- **Dynamiskt ljus** (klient): en fackla, lantern eller annat lysande block i handen, eller på marken, lyser upp runt
  omkring för alla spelare. Ljuset sprids som vanligt blockljus: väggar stoppar det, men det går runt hörn.

### Kyla
- Kroppstemperatur 0–20 (12 = lagom), visas som 10 termometrar ovanför matmätaren (varm / kall / iskall).
- Rör sig sakta mot en måltemperatur: biomen (snöbiomer är kalla även på dagen, öken varm), natten (−5 ute, −2,5
  under tak), regn (−2), i vatten (−5), höjd över y 100. **Brinnande eld inom 6 block värmer** (upp till +10).
- **Leather Armor** isolerar mot kyla: 15 % per del (60 % med full rustning). **Fur-rustning** 25 % per del (100 % = ingen kyla).
- **Under 8:** maten går åt fortare. **Under 3:** man fryser (frost på skärmen, långsam, tar frysskada).

### Fisk
- Döda fiskar och fångst släpper **Whole Cod / Whole Salmon / Whole Tropical Fish / Whole Pufferfish** (gäller både
  döda fiskar och fiskespö). De går inte att äta eller laga.
- **Rensa:** Whole fish + Flint Knife (crafting, kniven slits 1) → vanilla rå fisk (filén) + **Fish Bones**.
- Fish Bones → 1 Bone Meal.

### Byar och tamdjur
- **Cow, Sheep, Pig, Chicken, Horse, Donkey och Llama spawnar inte i naturen.** Goat, Rabbit och Mooshroom finns kvar.
- Första gången en spelare kommer inom 48 block från en bys klocka byggs en **hage** (7×7 Oak Fence med grind) på plan
  mark 8–20 block från klockan, med **4 djur** (Cow, Sheep, Pig eller Chicken). Finns ingen plats hamnar de vid klockan.
- **Det kommer inga fler.** Djuren despawnar aldrig, men vill man ha mer får man föda upp dem själv. Byarnas egna djur
  (vanilla-hagar, hästar) finns kvar.

### Strukturer i världen
Små, sällsynta platser som gör världen levande. Byggs bara i **nya chunks**. Behållarna är **Decorated Pots** med
stenålders-loot (inget järn). Krossa krukan för att få innehållet.

| Struktur | Var | Ungefär 1 på N chunks | Innehåll |
|---|---|---|---|
| Abandoned Campsite | Skog, slätt, meadow | 80 | Släckt lägereld, Drying Rack, Bone Pile; flint, cordage, mat |
| Hunter's Lean-to | Taiga, skog | 120 | Vindskydd, Bear Rug; hudar, pilar, sällan Crude Bow |
| Bear Den | Taiga, skog, dark forest | 120 | Grotta i sluttning (eller stenhög) med ben och en **Brown Bear** |
| Standing Stones | Slätt, savann, meadow | 100 | Stencirkel med altarsten, lite flinta och ben |
| Burial Mound | Slätt, meadow, taiga | 180 | Gravhög med gång in, skalle och ben; bone tools, horn, antler |
| Fishing Camp | Floder, stränder | 40 (kräver vatten) | Torkställningar, brygga; krokar, fisk, sällan Crude Fishing Rod |
| Flint Quarry | Slätt, flod, stony shore | 120 | Grus- och stengrop med Loose Flint; stenhuvuden, sällan Flint Knife |
| Mammoth Bone Hut | Snöiga biomer | 220 | Kupol av benblock; Tusk, ben, sinew, sällan Bear Pelt |
| Hermit's Hut | Dark forest, old growth | 250 | Hydda med säng och en **eremit** (leatherworker) som byter stenålderssaker |

### Småsaker i landskapet (dekorationer)
Små, vanliga saker som gör världen roligare att vandra i, byggs också bara i nya chunks (`scripts/gen_decorations.py`).
Varje sak slumpar storlek, riktning och stensort, så ingen ser likadan ut. Runt de flesta ligger Loose rocks av samma sten.

| Dekoration | Var | Ungefär 1 på N chunks | Utseende / innehåll |
|---|---|---|---|
| Glacial Erratic | Slätt, meadow, snowy plains, savann, windswept hills, stony shore | 24 | Stort rundat flyttblock (granite, diorite, andesite, stone, deepslate, tuff eller calcite), halvt nedsjunket, ibland 2–3 tillsammans |
| Mossy Boulder | Skog, taiga | 24 | Mossig sten (mossy cobblestone) med Moss Block, mossmatta och ormbunkar runt om |
| Sandstone Boulder | Öken, badlands | 24 | Sandstensblock (röd sandsten på röd sand) |
| Cairn | Windswept hills, stony shore, meadow, grove, taiga, slätt | 70 | Uppstaplad stenhög med en mur-sten på toppen; var tredje är ett smalt stentorn med en Loose Stone balanserad överst. Ibland en **Decorated Pot** gömd inne i högen (ben, flinta, antler, bone arrows, sällan stenyxhuvud) |
| Balanced Rock | Öken, badlands, savann, windswept hills | 70 | Bred sten balanserad på en smal stam |
| Rune Stone | Slätt, meadow, skog, taiga | 140 | Platt stående sten med inristade tecken (chiseled stone bricks eller chiseled tuff), blommor och trampad mark framför |
| Stone Head | Slätt, meadow, savann, windswept hills, stony shore | 200 | Jättelikt stenansikte nedsjunket till hakan, med panna, mörka ögon, lång näsa och mun. Var fjärde är dubbelt så stort |
| Animal Carving | Slätt, meadow, savann, skog, taiga | 160 | En hjort, björn eller vildsvin huggen i sten på en låg sockel |
| Shrine Statue | Slätt, meadow, savann, skog, taiga | 200 | Gammal stenfigur på en sockel mitt på ett litet torg av kullersten, grus och trampad jord, med slitna trappsteg, ljus, blommor och en **Decorated Pot** med offergåvor (bär, nötter, tallow, horn, antler, sällan Bear Claw). Ibland har huvudet ramlat av |

### Ancient Oak (uråldrig ek) och Offering Altar
En väldig, förtrollad ek: en egen **struktur** (hitta den med `/locate structure mechanical:ancient_oak`), mycket
sällsynt (en chans per 24×24 chunks, bara i Forest och Dark Forest). Sju väldiga rotben gör stammen ca **22 block bred
vid marken**; den smalnar till ca 9 block vid förgreningen 25–29 block upp. Trädet är ca **48 block högt** och kronan ca
**53 block bred**: 6–7 tjocka huvudgrenar böjer sig utåt och hänger ner i ändarna, delar sig i allt tunnare grenar och
kvistar med lövklungor bara i ändarna, så grenverket syns. Rötterna slingrar sig 15–20 block ut och dyker ner i jorden.
**Hela trädet är förtrollat**: det går inte att bryta, flytta med kolv eller spränga, och löven vissnar aldrig.

Magiskt: **runbark** (Ancient Oak Rune Log, ljus 6) går i spiraler uppför stammen och gröna/gyllene ljusprickar driver
från den; glow berries hänger från kronan, spore blossoms växer under grenarna och glow lichen på stammen. Runt foten en
ring av mossa, podzol, azaleor, firefly bushes och **Glowcap** (liten lysande svamp, ljus 8, går att plocka). Inne i
stammen ett rum på ca 9×9 och 5 högt med bred öppning mellan rotbenen och en skorsten 20 block upp, med lavar, svamp,
spindelväv och ibland en fladdermus. Barken är sömlös med djupa fåror i tre slumpade varianter.

**Gläntan under eken:** ut till en ojämn kant 22–29 block från stammen ligger en frodig glänta: mossa, podzol och rotjord
närmast stammen, gräs med mosslappar längre ut, ormbunkar, högt gräs, Lily of the Valley, Cornflower, Allium, Blue Orchid,
Azure Bluet, Oxeye Daisy, wildflowers och pink petals, moddets buskage, bärbuskar och svamp, och en **älvring** av Glowcap
(ca 7 block bred). Gläntan **lyser svagt om natten**: **Spirit Bloom** (blek cyanvit blomma, ljus 10, kan planteras på
jord, gräs och mossa) växer där det annars vore mörkt, **Glow Moss** (tunn lysande mossmatta, ljus 5) i fläckar,
Glowcaps, firefly bushes, glow lichen på rötterna och glow berries under de låga grenarna. Inga fientliga monster
spawnar i gläntan. Vägen in till håligheten hålls fri.

Mitt i rummet står **Offering Altar**. Lägg vilket föremål som helst på altaret (högerklick), ta tillbaka med tom hand.
Läggs en **Giant Antler** dit startar en ritual (7 s): hornet stiger och snurrar allt fortare, ljuspartiklar och löv
virvlar in, marken pulserar och klockspel ljuder, sedan löses hornet upp i ett utbrott och **ett av fem mytiska föremål**
(slumpvis) ligger på altaret. Under ritualen och 45 s efteråt kommer hjortar och andra fridsamma djur till trädets
öppning och är inte rädda för dig. Altaret kan användas igen.

| Mytiskt föremål | Effekt |
|---|---|
| **Thunderstone** (åskvigg) | Yxa: 3000 hållbarhet, modets bästa huggning (2 hugg, 95 %, +4 ved). Slag mot monster kallar ner blixten 1 gång på 8. Bäraren är immun mot blixtnedslag |
| **Mistletoe Charm** | I snabbraden eller andra handen: 1 hälsa var 4:e sekund och immunitet mot gift |
| **Ancient Amber** | Lyser (ljus 10) i handen; smyger du med den märker vilda djur dig aldrig |
| **Heart of the Oak** | Högerklicka en sapling så växer den direkt (30 s nedkylning). I inventariet: dubbel ved vid huggning |
| **Antler Crown** | Hjälm: mörkerseende utan flimmer, djur märker dig först på 3 blocks håll |

De mytiska föremålens namn är **feta och tonar från skogsgrönt till guld**, och tonen skimrar i tooltipen.

### Stenåldersmänniskor (Tribesperson)
Folk som lever i läger eller strövar i jaktlag. De **despawnar aldrig**, men nya kommer heller aldrig: dör de är de borta.
Spelarmodell med egna skins (pälsar, hudar, flätor, halsband; män och kvinnor, 4 varianter per roll).

- **Roller:** Hunter (Crude Bow eller Flint Spear), Gatherer (Flint Knife och Bark Basket), Fisher (Crude Fishing Rod),
  Woodcutter (Flint Axe), Elder (stav). Var och en har ett eget namn, t.ex. "Aruk the Hunter".
- **Läger** (ca 1 på 150 chunks i skog, taiga, slätt, meadow och vid floder; plan mark, aldrig nära byar): brinnande
  Campfire, 2–3 hudtält, Drying Rack och en sittstock. 3–5 personer: en Elder plus Gatherers, Woodcutters och Fishers
  (bara nära vatten).
- **Jaktlag** (ca 1 på 200 chunks i skog och taiga): 2–3 Hunters som följer en ledare.
- **Vardag:** Hunters jagar vilda djur (inte björnar, tama djur eller bydjur) och styckar kroppen (en Bone Pile blir kvar).
  Gatherers plockar mogna bär och svamp (bara i större svampfält). Fishers fiskar vid stranden. Woodcutters **fäller riktiga
  träd** nära lägret (trädet faller och blir Fallen Logs som vanligt), hugger upp Fallen Logs och bär veden till elden. Elder håller elden vid liv.
- **Natt:** alla samlas runt lägerelden men går aldrig in i den. Jaktlaget håller ihop där det är.
- **Försvar:** de slår tillbaka, hjälper varandra och anfaller monster i närheten (inte creepers). Monster jagar dem också, som zombier jagar bybor (inte creepers, endermen eller piglins).
- **Humör:** de hälsar ibland i chatten. **Slår du en blir hela gruppen arg i ungefär en speldag. Dödar du en är gruppen
  fientlig för alltid.** **Bryter du lägrets Campfire blir de också arga** (en speldag). Verktyget faller med 10 % chans när en spelare dödar dem.
- **Byteshandel (Elder):** 6 av 12 byten, t.ex. 6 Raw Hide → Bone Needle, 10 Flint → Flint Knife, 12 Raspberries → 4
  Cooked Game Meat Scraps, 6 Cordage → 8 Bone Arrows, 12 Bark → 4 Twine, 3 Scraped Hide → 2 Leather, 4 Sinew + 2 Cordage →
  Crude Bow, 3 Tusk + 2 Cordage → Tusk Dagger, 2 Antler → 3 Bone Fish Hooks. Fylls på varje morgon. Ingen handel när de är arga.

### Strukturernas loot (vanilla)
Alla vanilla-kistor och liknande (Mineshaft, Ruined Portal, Shipwreck, Buried Treasure, Desert/Jungle Temple, Dungeon,
Stronghold, Outpost, Mansion, Igloo, Ancient City, Trial Chambers, Nether- och End-strukturer), arkeologi, krukor,
spawners, Hero of the Village-gåvor, fiske (även skräpet: inga spön, bågar eller tripwire hooks) och **mobbars loot**
(t.ex. zombiens järntacka, drownedens koppar, häxans redstone) har **allt från senare åldrar bortfiltrerat**: metaller (järn, guld,
koppar …), ädelstenar, lapis, redstone, förtrollat, smithing templates, kompass/karta, hinkar, räls, crossbow m.m.
Det som är kvar (mat, kol, facklor, ben, frön, skivor, name tags …) ligger kvar, och kistor får också 2–4 primitiva saker
(sticks, flint, plant fibre, cordage, ben, twine, rocks, frön).

### Monstrens utrustning
Monster spawnar **utan rustning av järn, guld, chainmail eller diamant**. Vapen de behöver för att slåss (skelettens
bågar, järnsvärd, crossbows, tridents) har de kvar i handen, men de **släpps aldrig**. Saker ett monster själv plockat
upp släpper det som vanligt.

### Byarna är fattiga (stenåldern)
- **Kistor:** bara primitiva saker: sticks, flint, plant fibre, cordage, bark, twine, bone, seeds, plus lite efter hus
  (smed: flint-verktyg och tool rods, garveri: hudar, slaktare: kött och lard, fiskare: fisk och krokar, hus: bär och mat).
  Inget järn, guld eller diamanter.
- **Fälten är döende:** 75 % av grödorna är borta, resten är unga (ger bara frön, eller en morot/potatis). Jorden är torr.
- **Iron Golem** släpper inget järn (bara 0–2 Poppy).
- **Block från senare åldrar är låsta:** Furnace, Smoker, Blast Furnace, Anvil, Smithing Table, Stonecutter, Grindstone
  och Brewing Stand går inte att använda ("You don't know how to use this yet"). Bryts de blir det bara Rocks eller
  Sticks (Anvil inget). Cauldron, Bell och Lantern går att använda men släpper inget när de bryts (inget järn/guld).
  Listan är taggen `mechanical:too_advanced`, så senare åldrar kan låsa upp dem.
- **Sängarna går inte att sova i** (sängar kan inte tillverkas i stenåldern). Bryts de ger de 1–3 ull av sin färg.
- **Handel växer med åldrarna.** Bybor har bara stenålderssaker på nivå 1–2 (Novice/Apprentice); nivå 3–5 är tomma tills
  senare åldrar låser upp dem. Exempel: Toolsmith köper flint och säljer Flint Axe / Crude Stone Axe, Fletcher säljer
  Arrow och Crude Bow, Fisherman säljer Bone Fish Hook och Crude Fishing Rod, Weaponsmith säljer Flint Spear och Tusk
  Dagger. Wandering Trader säljer inget av metall (inga hinkar eller järnhackor).

### Bär och svamp (plocka)
Växer i **fläckar**: en bärbuske kommer sällan ensam (upp till ~7 buskar ihop), svamp växer i **svampfält** (upp
till ~14 ihop). Kan **inte planteras** än. Allt får plats i Bark Basket. **Svamp (även vanilla) släpper alltid med kniv, bara
hälften av gångerna utan.**

**Bärbuskar:** växer bar → blommor → omogna → mogna (~6 min per steg i ljus). **Högerklicka en mogen buske → 1–3 bär**,
sedan börjar den om från blommor (~12 min tills den är mogen igen). Inga taggar. Hugger man sönder busken: 1–2 Sticks +
1–2 Plant Fibre, och den är borta.

| Buske | Biomer | Bär (1 hunger) |
|---|---|---|
| Raspberry Bush | Forest, Birch Forest, Old Growth Birch, Flower Forest | Raspberries |
| Blueberry Bush | Forest, Dark Forest, Taiga, Old Growth Pine Taiga | Blueberries |
| Lingonberry Bush | Taiga, Snowy Taiga, Old Growth Pine/Spruce Taiga | Lingonberries |
| Wild Strawberry Bush | Plains, Sunflower Plains, Meadow, Flower Forest | Wild Strawberries |
| Elderberry Bush | Forest, Birch Forest, Flower Forest, River | Elderberries |
| Cloudberry Bush | Swamp, Snowy Plains, Snowy Taiga, Grove | Cloudberries |
| Blackberry Bush | Snowy Taiga, Snowy Plains, Grove, Taiga, Old Growth Spruce Taiga, Windswept Forest | Blackberries |

**Svampar:** högerklicka (eller bryt) för att plocka. Sprider sig långsamt till en plats i närheten i skugga (ljus ≤ 12),
om det finns färre än 4 av samma sort runt omkring.

| Svamp | Biomer | Rå |
|---|---|---|
| Chanterelle (kantarell) | Forest, Birch Forest, Taiga, Old Growth Birch/Pine | 1 hunger |
| Porcini (Karl Johan) | Forest, Dark Forest, Taiga, Old Growth Spruce | 1 hunger |
| Morel (murkla) | Forest, Flower Forest, Birch Forest | 1 hunger |
| Oyster Mushroom (ostronskivling) | Dark Forest, Swamp, Old Growth Spruce | 1 hunger |
| Fly Agaric (flugsvamp) | Birch Forest, Taiga, Snowy Taiga, Dark Forest | **Giftig:** Poison 10 s + Nausea 15 s |
| Puffball (röksvamp) | Plains, Meadow, Sunflower Plains | 1 hunger |

**Roasted Mushrooms:** valfri ätlig svamp (även vanilla Brown Mushroom) på Campfire (20 s) → 4 hunger.

### Grödor
- Gräs och ormbunkar släpper **inga frön**. Frön får man från byar (kistor och bönder).

### Rädsla och jakt
- Vilda djur håller sig undan spelare. De märker dig på **16 block om du springer, 10 om du går, 4 om du smyger**
  (längre än 4 block bara med fri sikt). Smyg för att komma nära, en Crude Bow rekommenderas.
- **Skadade djur får panik** i 10 s och springer fort bort från spelaren. De byter väg om spelaren springer runt
  dem eller om vägen leder tillbaka mot spelaren, och springer rakt bort om de inte hittar någon väg.
- **Blödande djur slutar slåss**: även Wild Boar, Aurochs, Moose och hjortar flyr när de blöder (björnen slåss alltid).
- **Panik smittar:** djur som ser ett djur i panik eller ett blödande djur får också panik.
- Tama djur och djur i koppel flyr inte. Mat i handen lugnar inte.
- Gäller djur med vanilla-"goals" (Cow, Pig, Sheep, Chicken, Rabbit, Horse, Llama, Fox, Wolf, Polar Bear, Panda, Parrot …).
  Goat, Camel, Frog, Axolotl, Armadillo och Sniffer (vanilla "brain"-AI) påverkas inte än.

### Bone Pile
Lämnas kvar där ett djur styckats färdigt och **försvinner aldrig**. Man går igenom den. Bryts den: Bone 15 %,
Bone Meal 1–2 (25 %). Har inget item.

**Mat** (laga på Campfire, 20 s; i Smoker längre fram)

| Item | Hunger | Tillagad | Hunger |
|---|---|---|---|
| Beef Scraps | 1 | Cooked Beef Scraps | 3 |
| Pork Scraps | 1 | Cooked Pork Scraps | 3 |
| Mutton Scraps | 1 | Cooked Mutton Scraps | 2 |
| Chicken Scraps | 1 | Cooked Chicken Scraps | 2 |
| Rabbit Scraps | 1 | Cooked Rabbit Scraps | 2 |
| Game Meat Scraps | 1 | Cooked Game Meat Scraps | 3 |

(Vanilla Cooked Beef = 8.) Mat kan bara lagas på Campfire just nu (Furnace borttagen, Smoker kräver Furnace).

### Plant Fibre och kniven
Se Material. Flint Knife slits 1 per gräs man slår sönder med den.

### Receptboken
Recepten dyker upp i receptboken när man får tag på det första materialet:

| När man får | Låses upp |
|---|---|
| Flint | Flint Pickaxe, Flint Axe, Flint Shovel, Flint Hoe, Flint Knife, Tool Station |
| Plant Fibre | Twine |
| Twine | Cordage, Fire Drill |
| Firewood (valfritt) | Tool Rod, Campfire, Firewood → Log (alla träslag) |
| Rock chunk (valfri) | Stone Pickaxe Head, Stone Axe Head, Clay Kiln |
| Clay Ball | Clay Kiln, Unfired Crucible |
| Stone Pickaxe Head | Double Stone Pickaxe Head |
| Whole fish (valfri) | Rensning av alla fyra |
| Fish Bones | Bone Meal |
| Bark | Plant Fibre (bast), Bark Basket |
| Raw Hide | Scraped Hide, Drying Rack |
| Wool Thread | White Wool (från tråd) |
| Stone Axe Head | Double Stone Axe Head |

Verktygen i Tool Station låses upp separat (se Tool Station).

## Borttaget och ersatt (vanilla)

| Vanilla | Status |
|---|---|
| Wooden Pickaxe/Axe/Shovel/Hoe/Sword/Spear | **Borttagna** |
| Stone Pickaxe, Stone Axe | Receptet borttaget → görs i **Tool Station** |
| Stone Shovel, Stone Hoe, Stone Sword | **Borttagna** |
| Copper-, Iron-, Diamond-verktyg (inkl. spears) | **Borttagna** (kommer tillbaka i senare åldrar) |
| Stone Spear | **Borttagen** → **Flint Spear** i Tool Station |
| Iron-, Golden-, Diamond-, Copper-armor, Turtle Helmet | **Borttagna** (Armor Station kommer senare). Leather Armor finns kvar |
| Alla Beds (alla färger, omfärgning, Straw Bed) | **Borttagna** (kommer senare, kräver Shears) |
| Bow, Fishing Rod | Recepten **borttagna** → **Crude Bow / Crude Fishing Rod** i Tool Station |
| Fisk-loot (döda fiskar, fiske) | Rå fisk → **Whole fish** som måste rensas |
| Farm-djur i naturen | **Borttagna** → spawnar en gång i byar (hage, 4 djur) |
| Byarnas kistor, fält, handel, Iron Golem-järn | **Primitiv loot, döende fält, stenåldershandel, inget järn** |
| Knockback på monster | **Borttaget** |
| Djur-loot | Ersatt av **Carcass** + styckning (ingen Leather/kött/ull direkt) |
| Furnace | **Borttagen** (ersätts av Clay Kiln, uppgraderas senare) |
| Smoker, Blast Furnace | **Borttagna** (senare åldrar) |
| Stone, Granite, Diorite, Andesite | Släpper **2–4 rocks** i stället för cobblestone (Silk Touch ger blocket). 4 rocks → 1 Cobblestone |
| Arrow-recept | Flyttat till **Fletching Table** |
| Candle-recept | Honeycomb + String → **Tallow + Twine** |
| Fletching Table | Har ett GUI för pilar och pilbågsuppgraderingar |
| Planks | 1 Log → **2** Planks (vanilla 4). Sågen kommer senare |
| Campfire-recept | Logs + Coal → **Firewood + Sticks** |
| Campfire placering | Placeras **släckt** |
| Campfire drop | Charcoal → **1–3 Stick** |
| Log drops | Log → **2 Firewood** (en enskild log). Fällda träd → **Fallen Logs** som måste huggas |
| Löv från fällda träd | Blir **Leaf Litter** på marken |
| Wheat Seeds från gräs | **Borttaget** (byar) |
| Facklor | Skyddar inte mot monster, kan slås sönder |
| Logs | Kräver yxa |
| Leaves | Stick-chans höjd |

Netherite-verktyg går inte heller att nå, eftersom diamantverktygen är borttagna.

## Öppna frågor / att göra

- **Tin Ore / Tin Ingot / Block of Tin** finns sedan tidigare. Tin Ingot kan inte smältas fram nu (Furnace borttagen) och
  hör till bronsåldern.
- Såg för planks.
- Flera texturer är tillfälliga (Fire Drill, Crucible, stenhuvuden, Firewood, Crude-verktyg, Clay Kiln-sot, meat scraps,
  hides, Wool Thread, Flint Spear, Tool Station, Drying Rack).
- Carcass ritas med djurets standardutseende (vita får, standardkor osv.).
- Kistor och byinvånare kan fortfarande ge järn/järnverktyg.
- Farming (kommer senare). Leaf Litter ska bli kompost/gödsel.

## Teknik (för utveckling)

- Bär och svamp: `Foraging.java` (registrering), `WildBerryBushBlock.java`, `WildMushroomBlock.java`, björnens `wildlife/ForageBerriesGoal.java`; worldgen i `gen_data.py`.
- Stationer med receptlista (Fletching Table, Armor Station): `StationRecipe.java`, `RecipeStationMenu.java`, `client/RecipeStationScreen.java`; recept i `FletchingRecipes.java`, `ArmorStationRecipes.java`. Pilar: `SpecialArrowItem.java`. Benkrok: `BoneHookRodItem.java`. Bear Rug: `BearRugBlock.java`.
- Register: `docs/registry.html` genereras av `scripts/gen_registry.py` (+ `docs/registry_notes.json`), följer med varje release och laddas ned av `update-mechanical.cmd`.
- Verktygstips: `client/StatTooltips.java`.
- Tändmaterial: `Tinder.java` (används av `FireDrillItem.java`).
- Campfire: `Campfires.java` + mixins `CampfireBlockMixin`, `CampfireBlockEntityMixin`. Vilda djur: paketet `wildlife/` (entities), `client/wildlife/` (modeller).
- Trädfällning: `TreeFelling.java`, `FallingTreeEntity.java` (landning), `TreeChopping.java` (hugga), `FallenLogBlock.java`, `StumpBlock.java`.
- Natten: `NightDangers.java` (eldregister, monster, horder, facklor, extra spawns, smygare, pathfinding, knockback), `client/NightAmbience.java` (ljud). Byar: `VillageAnimals.java`. Fisk: rensningsrecept i `gen_data.py`, Fish Bones i `StoneAgeEvents.java`. Kyla: `Temperature.java`, `client/TemperatureHud.java`. Blodmåne: `BloodMoon.java`, `client/BloodMoonClient.java`, mixins `BloodMoonSkyMixin`, `BloodMoonMoonMixin`. Mörker och dynamiskt ljus: mixins `DarkNightsMixin`, `LightCoordsMixin`, `EntityLightMixin`, `client/DynamicLights.java`.
- Blod: `Blood.java`, `BloodBlock.java`, `client/BloodDropParticle.java`. Rädsla: `AnimalFear.java`.
- Styckning: `Butchery.java` (utbytestabell), `CarcassEntity.java`, `KnifeItem.java`, `BonePileBlock.java`. Hängande ben och flådda texturer: mixins i `mixin/` (benen bara för fyrbenta djur och höns). Texturskript: `scripts/gen_skinned_textures.py`, `scripts/gen_bone_pile_textures.py`. Drying Rack: `DryingRackBlock.java`.
- Tool Station-recept: `ToolStationRecipes.java`. Clay Kiln-recept och bränsle: `KilnBlockEntity.java`. Båda är
  skrivna i Java (inte datapack).
- Övriga recept, loot tables, modeller och taggar genereras av `scripts/gen_data.py` (kör `python scripts/gen_data.py src/main/resources`). Texturskript: `scripts/gen_firewood.py`, `scripts/gen_textures.py`.
- Moddet laddas **efter NeoForge** (`neoforge.mods.toml`), annars skriver NeoForges kopior av vanilla-recept över våra.
