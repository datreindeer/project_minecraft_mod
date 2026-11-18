# Stenåldern

Första åldern. Man börjar med bara händerna. Målet är att ta sig från flinta och fiber till stenverktyg, eld och en
Clay Kiln, och att få fram Crucible som behövs i bronsåldern.

## Progression i korthet

1. Slå på gräs → **Plant Fibre** → **Twine**. Leta **Flint** (lös flinta på marken, grus) och **rock chunks** (lösa stenar).
2. Slå på löv → **Sticks**.
3. **Flint Knife** och **Flint Axe** (+ övriga flintverktyg).
4. Med yxan: hugg träd → **Firewood**. Med kniven: knacka **Stone Pickaxe Head** / **Stone Axe Head** av rock chunks.
5. **Tool Station** → **Crude Stone Pickaxe** och **Crude Stone Axe**.
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
| Stone / Granite / Diorite / Andesite Chunk | Loose rocks på marken (se Block) | Knacka stenhuvuden, Clay Kiln. Alla fyra räknas som `#mechanical:rock_chunks` och kan blandas |
| Firewood (10 sorter: Oak, Spruce, Birch, Jungle, Acacia, Dark Oak, Mangrove, Cherry, Pale Oak, Poplar) | Alla logs släpper **2 Firewood** av sitt träslag i stället för sig själva (även stripped logs och wood-block). Nether-stammar och bamboo släpper sig själva | Campfire, bränsle och Charcoal i Clay Kiln, 4 → 1 Log |
| Tool Rod | Flint Knife ovanför valfri Log (kniven slits 1) | Skaft för Stone Pickaxe / Stone Axe |
| Clay Ball | Vanilla (lera i vatten) | Clay Kiln, Brick, Unfired Crucible |
| Raw Hide | Styckning av djur (se Mat) | Scraped Hide |
| Scraped Hide | Raw Hide + Flint Knife (shapeless, kniven slits 1) | Läggs på Drying Rack → Leather |
| Leather | Torkad Scraped Hide på Drying Rack (djur släpper inte Leather längre) | Leather Armor |
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

### Stenhuvuden (crafting table, med Flint Knife)

| Item | Recept (R = rock chunk, K = Flint Knife) |
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

Crude-verktygen lagas med rock chunks.

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
Högerklicka för att öppna (smyg + högerklick för att placera block mot den). Tre platser + resultat:

| Plats 1 | Plats 2 | Plats 3 | Ger |
|---|---|---|---|
| Flint | Stick | Feather | 4 Arrow (vanilla-receptet borttaget) |
| Fish Bones eller Bone | Stick | Feather | 4 **Bone Arrow** (1,5 skada, vanlig pil 2) |
| Bear Claw | Stick | Feather | 4 **Barbed Arrow** (3 skada) |
| Tallow | Arrow | Twine | 1 **Fire Arrow** (sätter målet i brand) |
| Sinew | Crude Bow | Cordage | **Sinew Bow** (full pilskada, 200 i hållbarhet) |

### Fur-rustning och Bear Rug
- **Fur Hood / Coat / Leggings / Boots:** läderdelen + Bear Pelt (1 / 2 / 2 / 1). Skydd 1/3/2/1, lagas med Bear Pelt.
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
Recept: `[Flint][Twine][Flint]` / `[Planks][Crafting Table][Planks]`.

GUI: lista till vänster med upplåsta verktyg, i mitten tre platser (huvud, bindning, skaft) och en resultatruta.
- Ett verktyg låses upp första gången man har dess material i inventoryt (stenverktyg: valfri rock chunk). Upplåsningen
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
När ett träd fälls blir en stubbe kvar där man högg (10 träslag, samma som Firewood). Ger 1–2 Sticks, bryts snabbast
med yxa. Har inget item. Fungerar som **huggkubbe** för Rough Logs (se Bearbeta stockar).

## Övriga items

| Item | Hur | Används till |
|---|---|---|
| Unfired Crucible | `[Clay Ball][ ][Clay Ball]` / `[Clay Ball][ ][Clay Ball]` / `[Clay Ball][Clay Ball][Clay Ball]` | Bränns till Crucible i Clay Kiln |
| Crucible | Clay Kiln | Bronsåldern (smälta koppar + tenn). Ingen användning än |
| Log (från Firewood) | `[Firewood][Firewood]` / `[Firewood][Firewood]` av samma träslag → 1 Log | Planks, Tool Rod, bränsle |

## Mekaniker

### Stockar och trädfällning
- **Logs kan inte brytas utan yxa** (minst Flint Axe).
- Hugger man en log i ett **naturligt träd** (logs med naturliga löv) med yxa fälls hela trädet ovanför huggstället:
  - Hugg-tiden delas med antalet logs (stort träd = lång tid).
  - Yxan slits 1 per log. Räcker den inte går den sönder, men trädet faller ändå.
  - Trädet faller som en animation bort från spelaren och **blir liggande där det landar** (gravitation, staplas):
    stammen blir **Fallen Logs** som ligger längs fallriktningen. Löven släpper saplings, sticks och äpplen som innan och
    blir **Leaf Litter** på marken där de landar (flera löv på samma ställe ger tjockare lager).
  - Bara löven som hör till just det trädet tas med (grannträd lämnas).
  - En Stump blir kvar.
- **Smyg** för att hugga en enda log (ger 2 Firewood). Hus av logs fälls aldrig (placerade löv räknas inte som naturliga).

### Bearbeta stockar (håll in högerklick med yxa)
Ett tungt hugg var 0,6:e sekund (långsam svingning, sprickor i blocket). Varje steg kräver ett antal hugg beroende på yxan
(Flint Axe / Crude Stone Axe 4, Stone Axe 3, Iron Axe 2) och sliter yxan 1.

1. **Fallen Log:** barken av (ger **1–2 Bark**) → hugget smalare → ännu smalare → klar. Sista steget ger en **Rough Log**
   om det lyckas, annars bara 2–3 Sticks.
2. **Högerklicka en Rough Log på en Stump:** den står upp på stubben (tom hand tar tillbaka den).
3. **Hugg loggen på stubben:** lyckas → **2–3 Firewood** av det träslaget (+1 med Stone/Copper Axe, +2 med Iron Axe,
   +3 med Diamond Axe), misslyckas → 1 Firewood + 1–2 Sticks.

Chans att lyckas: Flint Axe 60 %, Crude Stone Axe 65 %, Stone Axe 75 %, Iron Axe 95 %.
Bryter man en Fallen Log med vanlig brytning blir det bara 1–2 Sticks.

Nya items: **Rough Log** (10 träslag, stackar 16), **Bark** (tändmaterial, bast, korg; senare garvning).

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
- Djur blöder när de träffas (blodpartiklar, fler vid hårda slag).
- **Under 50 % hälsa blöder de:** droppar blod, lämnar **bloddroppar på marken** när de rör sig (går att spåra) och
  tar en halv hjärta var 5:e sekund. Blödningen slutar på 1 hjärta, den dödar aldrig.
- Styckning sprutar blod och lämnar blodfläckar runt kroppen (en pöl runt Bone Pile).
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
- **Zombiehorder:** ungefär var tredje natt kommer 5–8 zombies samtidigt 32–48 block från en spelare. De håller ihop och
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
  mark 8–20 block från klockan, med 3 djur. Finns ingen plats används klockan som "hem".
- Var 30:e sekund fyller byn på med ett djur (Cow, Sheep, Pig eller Chicken) i hagen om det finns färre än 5 farm-djur
  inom 32 block. Byarnas egna djur (vanilla-hagar, hästar) finns kvar.

### Bär och svamp (plocka)
Sällsynta, i små grupper. Kan **inte planteras** än (bären och svamparna är inte frön). Allt får plats i Bark Basket.

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
- **Skadade djur får panik** i 10 s och springer fort bort från spelaren.
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
| Farm-djur i naturen | **Borttagna** → spawnar i byar (hage + påfyllning) |
| Knockback på monster | **Borttaget** |
| Djur-loot | Ersatt av **Carcass** + styckning (ingen Leather/kött/ull direkt) |
| Furnace | **Borttagen** (ersätts av Clay Kiln, uppgraderas senare) |
| Smoker, Blast Furnace | **Borttagna** (senare åldrar) |
| Stone, Granite, Diorite, Andesite | Släpper **2–4 rock chunks** i stället för cobblestone (Silk Touch ger blocket). 4 rock chunks → 1 Cobblestone |
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
- Fletching Table: `FletchingRecipes.java`, `FletchingMenu.java`, `client/FletchingScreen.java`. Pilar: `SpecialArrowItem.java`. Benkrok: `BoneHookRodItem.java`. Bear Rug: `BearRugBlock.java`.
- Register: `docs/registry.html` genereras av `scripts/gen_registry.py` (+ `docs/registry_notes.json`), följer med varje release och laddas ned av `update-mechanical.cmd`.
- Verktygstips: `client/StatTooltips.java`.
- Tändmaterial: `Tinder.java` (används av `FireDrillItem.java`).
- Campfire: `Campfires.java` + mixins `CampfireBlockMixin`, `CampfireBlockEntityMixin`. Vilda djur: paketet `wildlife/` (entities), `client/wildlife/` (modeller).
- Trädfällning: `TreeFelling.java`, `FallingTreeEntity.java` (landning), `TreeChopping.java` (hugga), `FallenLogBlock.java`, `StumpBlock.java`.
- Natten: `NightDangers.java` (eldregister, monster, horder, facklor, extra spawns, smygare, pathfinding, knockback), `client/NightAmbience.java` (ljud). Byar: `VillageAnimals.java`. Fisk: rensningsrecept i `gen_data.py`, Fish Bones i `StoneAgeEvents.java`. Kyla: `Temperature.java`, `client/TemperatureHud.java`. Mörker och dynamiskt ljus: mixins `DarkNightsMixin`, `LightCoordsMixin`, `EntityLightMixin`, `client/DynamicLights.java`.
- Blod: `Blood.java`, `BloodBlock.java`, `client/BloodDropParticle.java`. Rädsla: `AnimalFear.java`.
- Styckning: `Butchery.java` (utbytestabell), `CarcassEntity.java`, `KnifeItem.java`, `BonePileBlock.java`. Hängande ben och flådda texturer: mixins i `mixin/` (benen bara för fyrbenta djur och höns). Texturskript: `scripts/gen_skinned_textures.py`, `scripts/gen_bone_pile_textures.py`. Drying Rack: `DryingRackBlock.java`.
- Tool Station-recept: `ToolStationRecipes.java`. Clay Kiln-recept och bränsle: `KilnBlockEntity.java`. Båda är
  skrivna i Java (inte datapack).
- Övriga recept, loot tables, modeller och taggar genereras av `scripts/gen_data.py` (kör `python scripts/gen_data.py src/main/resources`). Texturskript: `scripts/gen_firewood.py`, `scripts/gen_textures.py`.
- Moddet laddas **efter NeoForge** (`neoforge.mods.toml`), annars skriver NeoForges kopior av vanilla-recept över våra.
