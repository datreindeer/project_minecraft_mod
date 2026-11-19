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
9. Maten förskäms (rått kött på 1 dag). **Smoking Rack** över en Campfire → Smoked Meat/Fish, eller torka på Drying Rack →
   Jerky, Dried Fish, Dried Berries: håller 30 dagar.

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
| Firewood (19 sorter: Oak, Spruce, Birch, Jungle, Acacia, Dark Oak, Mangrove, Cherry, Pale Oak, Poplar, Aspen, Alder, Beech, Linden, Willow, Ash, Hazel, Pine, Yew) | Alla logs släpper **2 Firewood** av sitt träslag i stället för sig själva (även stripped logs och wood-block). Nether-stammar och bamboo släpper sig själva | Campfire, bränsle och Charcoal i Clay Kiln, 4 → 1 Log |
| Tool Rod | Flint Knife ovanför valfri Log (kniven slits 1) | Skaft för Stone Pickaxe / Stone Axe |
| Clay Ball | Vanilla (lera i vatten) | Clay Kiln, Brick, Unfired Crucible |
| Raw Hide | Styckning av djur (se Mat) | Scraped Hide |
| Scraped Hide | Raw Hide + Flint Knife (shapeless, kniven slits 1) | Läggs på Drying Rack → Leather |
| Leather | Torkad Scraped Hide på Drying Rack (djur släpper inte Leather längre) | Leather Armor-recept | Sys i **Armor Station** med Bone Needle + Wool Thread |
| Leather Armor |
| Raw Phantom Hide | Styckning av Phantom, omgång 1 (60 %) | Scraped Phantom Hide |
| Scraped Phantom Hide | Raw Phantom Hide + kniv (shapeless, kniven slits 1) | Läggs på Drying Rack **i dagsljus** → Phantom Leather |
| Phantom Leather | Torkad Scraped Phantom Hide på Drying Rack | Glider Cloak, lagar Glider Cloak |
| Glider Frame | Crafting: `[Tool Rod][Sinew][Tool Rod]` / `[Bone][Cordage][Bone]` / `[Tool Rod][ ][Tool Rod]` | Glider Cloak |
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
fjädrar): högre chans och fler, t.ex. Raw Bear Pelt 1–2 i stället för 1.

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
**String** (från spindlar) fungerar överallt där receptet vill ha Wool Thread (samma antal, en sort per ruta).
Passar ingredienserna flera recept (Leather Boots och Leather Satchel har samma delar) gäller receptet man senast klickade på i listan; annars det som använder mest (5 Leather blir en hjälm, inte stövlar).

| Rustning | Material | Verktyg | Tråd | Låses upp av |
|---|---|---|---|---|
| Leather Helmet / Chestplate / Leggings / Boots | 5 / 8 / 7 / 4 Leather | Bone Needle | 2 / 4 / 3 / 2 Wool Thread | Leather |
| **Leather Satchel** | 4 Leather | Bone Needle | 2 Wool Thread | Leather |
| Fur Hood / Coat / Leggings / Boots | 1 / 3 / 2 / 1 Bear Pelt | Bone Needle | 1 / 3 / 2 / 1 Sinew | Bear Pelt |
| **Wolf Hood** | 2 Wolf Pelt | Bone Needle | 1 Sinew | Wolf Pelt |
| Wolf Fur Coat / Leggings / Boots | 4 / 3 / 2 Wolf Pelt | Bone Needle | 3 / 2 / 1 Sinew | Wolf Pelt |
| **Glider Cloak** | 6 Phantom Leather | Bone Needle | 1 Glider Frame | Phantom Leather |

Vanilla-recepten för läderrustning är borttagna.

### Leather Satchel
Sys i Armor Station (se ovan). Högerklicka för att öppna ett förråd med **9 platser (3x3)** för vad som
helst, utom andra förråd (Leather Satchel, Bark Basket, shulker boxes, bundles). Innehållet sparas på väskan. Staplas inte.

### Fur-rustning och Bear Rug
- **Fur Hood / Coat / Leggings / Boots:** sys i Armor Station (se ovan). Skydd 1/3/2/1, lagas med Bear Pelt.
  **Isolerar 25 % per del mot kyla** (läder 15 %).
- **Bear Rug:** Bear Pelt + Bear Claw. Platt dekoration (som en matta) med huvudet bort från den som placerar den.

### Wolf Pelt: Wolf Hood, Wolf Fur och Wolf Rug
- **Wolf Hood:** jägarhuva gjord av en vargs huvud: överkäken med huggtänderna över pannan, öronen på toppen, ansiktet
  öppet och pälsen över axlarna och nedför ryggen till svansen (egen 3D-modell). Sys i Armor Station av 2 Wolf Pelt +
  1 Sinew. Skydd 1, **isolerar 20 %**. **Vargarna tar dig för en i flocken:** en Grey Wolf känner bara vittringen av dig
  på halva avståndet och anfaller inte alls om du inte är **illa skadad (under 30 % liv)** eller **slår en varg först**
  (då anfaller flocken som vanligt). Under **blodmåne** eller när vargarna **svälter** hjälper huvan inte.
- **Wolf Fur Coat / Leggings / Boots:** sys i Armor Station av 4 / 3 / 2 Wolf Pelt + 3 / 2 / 1 Sinew. Skydd 2/2/1
  (med huvan 6 mot björnpälsens 7), lite lägre hållbarhet än Fur. **Isolerar 20 % per del** (björnpäls 25 %, läder 15 %).
  Lagas med Wolf Pelt.
- **Wolf Rug:** 2 Wolf Pelt + Bone (formlöst). Platt dekoration som Bear Rug, med vargens huvud bort från den som placerar den.

### Glider Cloak
En sen stenåldersbedrift: Phantoms finns bara när en spelare inte har sovit på 3+ nätter, varje Phantom ger Raw Phantom
Hide bara i 60 % av fallen, och huden måste torka i solen utan att ruttna. Totalt: 6 Phantom Leather (~10 Phantoms) +
**Glider Frame** (4 Tool Rod, 2 Bone, Sinew, Cordage) sys med Bone Needle i Armor Station.
- Bröstplats, **skydd 1**, 96 i hållbarhet, lagas med Phantom Leather. Ser ut som en mantel av hinna (krage med benspänne,
  axelslag) med hopvikta fladdermusvingar på ryggen (elytra-modellen med egen textur).
- **Glidflygning:** faller du (mer än 2 block) och **håller inne hopp** fångar manteln luften. Fallet bromsas till
  ~2,4 m/s och du driver framåt i blickriktningen, högst ~6 m/s (elytra ~30). Det går aldrig uppåt. Släpp hopp eller
  smyg för att släppa. Ingen fallskada när du landar från en glidning. Kostar 1 hållbarhet per sekund i luften.
  Fungerar inte i vatten, på stegar, när man rider eller flyger.

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
- Klick på ett recept hämtar en sats delar ur inventoryt; **shift-klick hämtar så många satser du har** (upp till en
  stack per plats). Gäller även Fletching Table och Armor Station (verktyg som Bone Needle läggs alltid bara en).
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
  medan ugnen är **minst 500°** (lägereldens ved eller bränsle i ugnen; bränsle går åt under tiden, och den slocknar
  inte av att stå tom). På lägereldens ved tar det ~1,5 min att nå 500°, så en ny ugn är bränd efter ungefär 3,5 min. Leran
  mörknar i 3 steg nerifrån och upp till bränd rödbrun lera; pilen i GUI:t visar hur långt det kommit. Slocknar elden
  behålls det som redan bränts. En bränd ugn förblir bränd även om man bryter och sätter ut den igen (item:et visar det).
  Står den på en tänd Campfire när man bygger den bränns den på lägereldens ved.
- Var Campfire tänd fortsätter ugnen brinna på lägereldens egen ved (3 min bakning).
- Annars: lägg bränsle och tänd med Fire Drill.
- En tänd ugn **bränner bränsle hela tiden**, även när inget bakas, så man kan värma upp den i förväg och den håller
  smält metall varm i output-slotten. Den slocknar först när bränslet tar slut. Påbörjat bränsle sparas till nästa tändning.
- Syns: flammor ur eldöppningen (mer när den bakar), rök ur skorstenen, lyser (ljus 13).
- Bryter man någon halva försvinner ugnen och man får tillbaka Clay Kiln + 1–3 Stick (eldstaden går sönder) + innehåll.
- GUI som en Furnace: input, bränsle, resultat, plus en **termometer** till vänster.
- **Temperatur** (bronsålderns fas 2, se [02-bronze-age.md](02-bronze-age.md)): ugnen har en temperatur i grader som
  stiger ~5°/s mot bränslets max när den brinner och sjunker ~3°/s när den är släckt. Varje recept kräver en viss
  temperatur och väntar (utan att börja om) tills ugnen är varm nog. Trä räcker för allt i stenåldern; Crucible (800°)
  kräver Charcoal eller en Bellows.

**Recept i Clay Kiln**

| In | Ut | Tid | Kräver |
|---|---|---|---|
| 1 Clay Ball | Brick | 20 s | 600° |
| 1 Log (alla som brinner) | Charcoal | 30 s | 400° |
| 2 Firewood | Charcoal | 30 s | 400° |
| 1 Unfired Crucible | Crucible | 40 s | 800° |
| 1 Unfired Clay Bucket | Clay Bucket | 30 s | 600° |
| 1 Dough (bronsåldern) | Bread | 20 s | 200° |

Bronsålderns recept (Clay Tuyere, Clay Molds, smältning i Crucible) står i 02-bronze-age.md.

**Bränsle**

| Bränsle | Brinntid | Max temperatur | Max med Bellows |
|---|---|---|---|
| Stick, Sapling | 10 s | 500° | 700° |
| Planks | 30 s | 500° | 700° |
| Firewood (och lägerelden ugnen byggdes på) | 1 min | 650° | 900° |
| Log | 2 min | 650° | 900° |
| Charcoal, Coal | 4 min | 800° | 1300° |

### Drying Rack
Recept: `[Stick][Twine][Stick]` / `[Stick][ ][Stick]`.
- Högerklicka med **Scraped Hide** för att spänna upp den. Efter några minuter (slumpmässigt, ~3 min i snitt) är den torr.
- Högerklicka på en torr hud → **Leather**.
- **Scraped Phantom Hide** torkar bara i **dagsljus** (dag, inte regn, och fri himmel ovanför ställningen): lika fort
  som en vanlig hud (~3 min solljus i snitt) → **Phantom Leather**. Varje slumpad tick i mörker (natt, regn, inomhus)
  räknas, och efter 8 sådana (~9 min, ungefär en hel natt; räknas ihop, nollställs inte av sol) **ruttnar** den. En
  ruttnad hud ger **Rotten Flesh** när man högerklickar. Häng den alltså ute på morgonen och ta in läder före natten.
- Bryts den släpper den sig själv + hud/Leather som satt på.
- **Mat torkar också på den** (se "Förskämd mat, rökning och torkning"): en remsa rått kött (vilket rått kött som helst,
  scraps eller vanilla) → **Jerky**, en rå fiskfilé (Raw Cod/Salmon) → **Dried Fish**, eller en näve med **4 bär** (alla
  vilda bär + Sweet Berries) → **4 Dried Berries**. Torkar **utomhus på dagen utan regn**, även i skugga (t.ex. under ett träd,
  skylight minst 10): ~7 min i snitt i full sol ute i vinden (fri himmel, inte inbyggd på tre sidor), ~14 min i skugga
  eller lä. Inne i ett hus med tak torkar inget. Natt och regn **pausar** bara, inget ruttnar på ställningen. Högerklicka med tom
  hand när den är klar. Bryts ställningen släpps färdig torkad mat, men mat som håller på att torka går förlorad.
- **Svamp torkar också:** en näve med **4 svampar** (Chanterelle, Porcini, Morel, Oyster Mushroom, Puffball, gärna
  blandat) → **4 Dried Mushrooms**, på samma sätt som bär. Fly Agaric och vanilla-svamp går inte (Fly Agaric är giftig,
  och vanilla-svamp är inte mat här).
- **Rotting-mat** (under 20 % kvar, se "Förskämd mat") går inte att hänga upp, varken här eller på Smoking Rack:
  "This is too far gone to keep". Fresh och Stale går bra.
- **Hög ställning:** högerklicka på en Drying Rack med en till Drying Rack så växer den uppåt till **2 block** (rutan
  ovanför måste vara tom; högst 2 block, föremålet går åt). En hög ställning har **4 platser**, två bredvid varandra i
  varje block (nedre raden hänger från en tvärslå mitt på, övre raden från toppslån; stolparna har fötter). Varje plats
  rymmer en hud eller en sats mat och torkar för sig, med samma regler som ovan. Det som hängde på den låga ställningen
  hamnar på nedre vänstra platsen.
  - Högerklick med något att hänga: platsen man pekar på (vänster/höger halva av blocket) om den är ledig, annars nästa
    lediga i samma block, sedan i den andra halvan.
  - Tom hand tar ner **allt som är klart** på hela ställningen.
  - Bryts ena halvan går båda (i creative utan drops). Varje halva släpper en Drying Rack + det som hänger på den.
    Kolvar knuffar inte ställningen, den lossnar som föremål.
  - Solen kollas ovanför övre halvan, så den nedre torkar lika fort. Två Phantom Hides i samma block delar mörkerräkningen
    (den nya ärver den gamlas räkning); i olika block räknas de var för sig.
  - En låg ställning har kvar **1 plats** och fungerar som förut. Gamla ställningar (från före den höga) behåller det som
    hänger på dem: tillståndet heter fortfarande `hide`, och de nya egenskaperna `part`/`hide2` får sina standardvärden
    (`single`/0) när världen laddas.
- **Stora pälsar (bara på hög ställning):** styckning ger **Raw Bear Pelt** (Brown Bear) och **Raw Wolf Pelt** (Grey Wolf),
  som måste torkas på en **hög** Drying Rack innan de blir **Bear Pelt** / **Wolf Pelt** (som används i alla recept som
  förut). På en låg ställning: "This pelt needs a bigger rack".
  - **Raw Bear Pelt tar hela ställningen** (alla 4 platser, båda blocken måste vara tomma). Björnen spänns ut över hela
    ramen med huvudet upp mot toppslån, frambenen surrade med Twine upp mot slån och bakbenen ner mot bottenslån.
  - **Raw Wolf Pelt tar en halva** (båda platserna i ett block): blocket man pekar på om det är tomt, annars det andra.
    Två vargpälsar ryms alltså på en hög ställning, eller en vargpäls och två andra saker.
  - Är det fullt: "There is no room for the pelt on this rack".
  - Torkar **utomhus på dagen utan regn**, även i skugga under träd (kollas ovanför övre halvan): **~6 min** i snitt (en vanlig hud ~3 min). Natt
    och regn **pausar** bara, pälsen ruttnar inte. Pälssidan vänds framåt, köttsidan (rå och röd med fett, torkad
    gulbrun) syns bakifrån.
  - Tom hand tar ner den färdiga pälsen (en Bear Pelt för hela björnen). Bryts ställningen släpps pälsen **en gång**
    (rå eller färdig), inte en gång per plats eller block.

### Smoking Rack
Recept: `[Stick][Cordage][Stick]` / `[Stick][ ][Stick]` / `[Stick][ ][Stick]` (låses upp när man får rått kött eller rå fisk).
- En hög träram med en tvärslå. Högerklicka med **rått kött eller rå fisk** (samma sorter som på Drying Rack) för att hänga
  upp en remsa; upp till **4 remsor**, kött och fisk blandas inte. Remsorna syns på ramen (rå, rökt eller ruttna).
- **Rök:** ställ den **ovanför** (1–2 block upp) eller **bredvid** en brinnande eller glödande Campfire, eller i ett rökigt rum
  (rökhalt ≥ 0,3, se "Rök inomhus"). Efter **4 minuter i rök** blir alla remsor **Smoked Meat** / **Smoked Fish**. Hänger man
  upp en ny remsa börjar röktiden om. En lägereld brinner 3–5 min och glöder 2 min till, så en eld räcker oftast; lägg på Firewood för säkerhets skull.
- **Utan rök händer ingenting**, och remsorna förskäms på sin vanliga tid (1 dag). Den första remsan som ruttnar tar alla med sig → Rotten Flesh.
- Tom hand tar ner allt (råa remsor behåller sin färskhet). Bryts den släpps ramen och det som hängde på.

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
| Crucible | Clay Kiln (800°) | Bronsåldern: fylls med malm och smälts i Clay Kiln (se 02-bronze-age.md). Staplas inte (1 per stack) |
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
    stammen blir **Fallen Logs** som ligger längs fallriktningen. Gräs, blommor, plantor, buskar och svamp där trädet
    landar krossas (och släpper sina vanliga drops). Löven släpper saplings, sticks och äpplen som innan och
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
- **Nio nya träslag** med full träuppsättning (Log, Wood, Stripped Log/Wood, Planks, Slab, Stairs, Fence, Fence Gate,
  Door, Trapdoor, Button, Pressure Plate, Leaves, Sapling, Potted Sapling; inga skyltar eller båtar än). De fälls som
  andra träd och har egna Stump, Fallen Log, Rough Log och Firewood. 1 Log → 2 Planks som alla träslag. Löven ger
  Sapling och Sticks men inga äpplen.
  - **Aspen:** hög, smal och rak stam (9–13 block), liten krona. Forest, Flower Forest, glest i Meadow och Taiga.
  - **Alder:** medelstort rakt träd (6–9 block). River-stränder och Swamp, ibland i Forest och Flower Forest.
  - **Beech:** stort träd, rak hög stam (8–12 block), bred och tät krona. Forest och Flower Forest.
  - **Linden:** stort grenat träd med bred krona. Forest och Flower Forest, sällsynt på Plains.
  - **Willow** (pil): kort grenad stam, varje gren bär ett platt lövparaply med hängande lövridåer (upp till 4 block).
    Grå fårad bark, ljusa planks, smala ljusgröna löv. Växer vid vatten: River och Swamp, och i Forest, Flower Forest,
    Birch Forest, Plains och Meadow bara intill vatten (sjöar, bäckar).
  - **Ash** (ask): hög rak stam (10–15 block), hög och gles oval krona. Ljusgrå bark med fint rutnät, nästan vita
    planks. Forest och Flower Forest, sällsynt på Plains och Meadow.
  - **Beech:** löven ger ibland (6 %) **Beechnuts** (bokollon), inte med Shears/Silk Touch.
  - **Hazel** (hassel, trädet): 3–5 tunna stammar från samma rot, 4–6 block högt och buskigt. Blank kopparbrun bark,
    rosaljusa planks, runda löv med hängen. Löven ger ibland (6 %) **Hazelnuts**. Forest, Flower Forest, Birch Forest,
    ibland på Plains. (Den låga Hazel Shrub finns kvar.)
  - **Pine** (tall): hög kal stam (11–15 block) som ibland grenar sig i toppen, platta kronor högst upp. Orange flagnande
    bark, gulaktiga planks, blågröna barr. Taiga, Snowy Taiga, Old Growth Pine Taiga, Windswept Forest och Windswept Hills.
    **Högerklicka en Pine Log med kniv:** barken skärs upp → 1 **Pine Resin**, och loggen blir Stripped Pine Log (en
    gång per log, kniven slits 1).
  - **Yew** (idegran): kort tjock stam (2×2) med sidogrenar, låg, bred och tät, mörka barr med röda frön. Rödbrun
    flagnande bark, rödorange planks. Sällsynt i Old Growth Spruce/Pine Taiga och Windswept Forest, mycket sällsynt i
    Taiga och Dark Forest.
  - Nya saker: **Beechnuts** (1 mat, rostas på Campfire till **Roasted Beechnuts**, 3 mat), **Pine Resin**
    (kåda: 1 Pine Resin över 1 Stick → 2 Torches, en kådfackla innan man har kol; används mer senare, t.ex. som lim).
    Nötter och kåda får plats i Bark Basket.
  - Framtida idé: **Yew Bow**, en bättre båge av idegran i Tool Station. Idegranens löv kan bli giftiga för djur.
  - För biomer: varje träd har `mechanical:<träd>` (trädet, som saplings växer) och `mechanical:<träd>_checked`
    (bara sapling-kontrollen) att använda i egna biomer.
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

### Biomer
Fyra nya biomer i overworld. De tar en del av vanilla-biomernas klimat (vanilla-biomerna finns kvar, bara mindre),
terrängen är densamma som förut. Bara **nya chunks** får dem; gamla chunks behåller sina biomer, så gränsen mot gammal
värld kan bli tvär.

| Biom | Tar plats från | Hur vanlig |
|---|---|---|
| **Deciduous Forest** (lövskog) | Den fuktigare halvan av tempererad Forest | Vanlig, ungefär lika vanlig som Forest |
| **Hazel Meadow** (hasselbacke) | Den fuktigaste delen av sval och tempererad Plains, mot skogen | Vanlig, ligger mellan slätt och skog |
| **Pine Heath** (tallhed) | All sval och torr Plains (inte Dappled Forest) | Ganska vanlig i svalt klimat |
| **Willow Fen** (pilkärr) | Sval Swamp och den fuktigaste delen av tempererad Swamp | Ovanlig, ungefär lika sällsynt som Swamp (bara platt låglänt mark) |

- **Deciduous Forest:** tät lövskog av **Beech** (ca 40 %), Oak (20 %), **Linden** (15 %), **Ash** (15 %), Hazel och
  ibland en omkullfallen bok med svamp på. Tjockt lager löv (Leaf Litter, en del ännu bok-, lind- och ekfärgat), mycket
  Brown/Red Mushroom, Porcini, Chanterelle, Morel och Oyster Mushroom, Hazel Shrub, Bramble, Undergrowth, Sapling
  Thicket, Raspberry och Elderberry. Djur: rådjur, kronhjort, vildsvin, hare, fasan (som all skog), Badger, Squirrel,
  Hedgehog, Crow, Brown Bear, Giant Deer, vargar. Mörkare grönt gräs och löv. Räknas som skog (`#minecraft:is_forest`),
  så Tribe Camps, Hunting Bands, Bear Dens, Hermit's Hut, Ancient Oak och Tribe Villages kan dyka upp.
- **Hazel Meadow:** öppen äng med **klungor av Hazel** (2–4 hasselträd tätt ihop), enstaka Ash, Oak och Linden, mycket
  blommor (Oxeye Daisy, Cornflower, Poppy, Dandelion, Azure Bluet, Allium, Lily of the Valley) och Wildflowers. Hazel
  Shrub, Wild Strawberry, Raspberry, Elderberry, Bramble, vilt Einkorn och Emmer, Wild Pea, Puffball. Djur: rådjur,
  hare, fasan, kanin, Aurochs, Wild Horse, Hedgehog, Crow, Giant Deer. Ljust gräs. Plains-byar (vanilla) och Tribe
  Villages, Standing Stones, Burial Mound, Stone Head m.fl.
- **Willow Fen:** blött låglänt kärr med grunda gölar, tätt med **Willow** (ca 55 %) och **Alder**, som också står i
  grunt vatten. **Mud** på gölarnas botten och upp på stränderna, Lily Pads, Sugar Cane, Fern, Blue Orchid, Cloudberry,
  Elderberry och Oyster Mushroom. Grumligt grönbrunt vatten, disig luft (dimma närmare än vanligt) och kort sikt under
  vatten. Swamp-musik. Djur: grodor, vildsvin, Goose, Moose; slimes och Witch Huts som i Swamp.
- **Pine Heath:** torr, sval hed med glest stående **Pine** och ibland en omkullfallen tall. Fläckar av **Podzol**,
  **Coarse Dirt** och **Sand** i gräset, mycket **Heather** (ljung), Sweet Berries, Lingonberry, Blueberry, Juniper,
  Chanterelle, Porcini och Fly Agaric. Gulgrönt torrt gräs. Old Growth Taiga-musik. Djur: hare, kronhjort, kanin, räv,
  varg, Badger, Squirrel, Giant Deer, Brown Bear. Räknas som taiga (`#minecraft:is_taiga`).
- **Heather** (ljung): lågt rosalila ris på Pine Heath. Ingen kollision, går att gå igenom och ersätts när man bygger.
  Bryts: hälften av gångerna en Plant Fibre; med Shears blocket självt. Kan komposteras.
- Teknik: `mixin/OverworldBiomeBuilderMixin` delar vanillas klimatrutor (fuktighet eller temperatur) och ger ena
  halvan till vår biom; data i `scripts/gen_biomes.py`. Biomerna är också med i `c:`-taggar (is_swamp, is_plains,
  is_tree/deciduous m.fl.).

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
**Bronze Saw** (bronsåldern) på en Fallen Log: håll in högerklick, 8 sågtag (~3,5 s, sågen slits 1) så kapas hela stocken på en gång
till **3 Rough Logs**, alltid, utan slump, plus barken (1–2 Bark) om den satt kvar. Har yxan redan huggit den smalare blir det
färre (2 respektive 1).

Nya items: **Rough Log** (14 träslag, stackar 16), **Bark** (tändmaterial, bast, korg; senare garvning).

**Bark används till:**
- **Bast:** Bark + kniv (crafting, kniven slits 1) → **2 Plant Fibre**.
- **Bark Basket:** `[ ][Cordage][ ]` / `[Bark][ ][Bark]` / `[Bark][Bark][Bark]`. Högerklicka för att öppna (3×3). Rymmer bara
  frukt, bär och hudar: Apple, Golden Apple, Sweet Berries, Glow Berries, Melon Slice, Chorus Fruit, Leather, Rabbit Hide,
  Raw Hide, Scraped Hide, Raw Phantom Hide, Scraped Phantom Hide. Innehållet sparas i korgen (stackar inte, 1 per plats i inventoryt).
  **Mat håller längre i korgen:** medan korgen är stängd åldras bär, svamp och annan mat i den bara en tredjedel så fort
  (barken är sval och luftig). Öppen korg = vanlig takt. Gäller inte Leather Satchel.
**Leaf Litter** (från fällda träds löv) används som tändmaterial och blir kompost i Compost Heap (bronsåldern).

### Flotte och kanot (i stället för båtar)
Vanilla-båtarna går inte att crafta i stenåldern (se "Recept från senare åldrar"). I stället finns två farkoster. Båda är
vanilla-båtar i grunden (flyter, går att leda med Lead, knuffar in djur, släpper sitt item när de slås sönder), men
toppfarten hålls nere.

| | Log Raft | Dugout Canoe | Vanilla Boat (jämförelse) |
|---|---|---|---|
| Platser | 1 | 2 | 2 |
| Toppfart på vatten | ~3,4 block/s (47 %) | ~6,3 block/s (87 %) med **Paddle** i handen, ~3,4 block/s utan | ~7,2 block/s |
| Hur | Crafting table | Hålas ur i världen | — |

Fart utan styrning (drift, strömmar) är som en vanilla-båt; bromsen gäller bara när någon sitter i och styr. Flotten
gungar på vattnet och har inga väggar; kanoten håller vattnet ute ur skrovet.

**Log Raft:** `[Log][Cordage][Log]` × 3 rader (6 stockar + 3 Cordage). Stockarna kan vara **Rough Log** av valfritt träslag
eller valfri Log som brinner (`#mechanical:raft_logs`). Låses upp av Cordage. Modell: fem barkklädda stockar sida vid sida,
två skalade tvärslanor surrade med rep, enkla paddlar.

**Paddle:** valfri Rough Log + kniv (formlöst, kniven slits 1). Låses upp av en Rough Log. Håll den i valfri hand när du
sitter i kanoten; då syns paddlarna och kanoten går nästan dubbelt så fort.

**Dugout Canoe** (urholkad stock), görs i världen:
1. **Högerklicka med en kniv** på en **Fallen Log** (med eller utan bark, inte hugget smalare) eller en **Log som ligger
   ned** (placerad på sidan, alla stockar som brinner: `#mechanical:dugout_logs`). Kanotens kontur ritsas in i ovansidan och
   blocket blir en **Dugout Blank** (kniven slits 1).
2. **Hugg ur** med yxa (håll högerklick, som Fallen Logs men dubbelt så många hugg: Flint Axe 6 hugg ≈ 2,7 s, Stone Axe 4).
   Spånen flyger (1–2 Sticks), yxan slits 1.
3. **Bränn:** tänd i urholkningen med **Torch** (tänder alltid, förbrukas inte), **Fire Drill** (75 %, spånen är bra
   tändmaterial; slits 1 per försök) eller **Flint and Steel** (slits 1). Det brinner i **30 s** (lågor, rök, ljus 11).
   Regnar det när elden slocknar blir det inget kol, och man får tända igen.
4. **Skrapa ur kolet** med yxa (som steg 2).
5. **Bränn igen** (som steg 3).
6. **Hugg ur** sista gången med yxa → urholkningen är djup, väggarna tunna.
7. **Jämna till med kniv** (håll högerklick, 4 drag, kniven slits 1) → blocket försvinner och ger en **Dugout Canoe**.

Fel verktyg i fel steg ger ett meddelande om vad som behövs. Medan den brinner går den inte att bearbeta. Bryts blanken
blir det 1–3 Sticks. Blanken har ett eget utseende (huggen ek) oavsett träslag; kanoten likaså. Kanotens modell: lång
smal urholkad stock med rundad botten, solida avsmalnande ändar och mörkbränd, skrapad insida.

Båtar med plankor (vanilla Boat, Chest Boat, Bamboo Raft) kommer tillbaka i **bronsåldern**.

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
| **Phantom** (odöd, inget kött) | Raw Phantom Hide 60 % | 1–2 Rotten Flesh, Phantom Membrane 50 % |

**Phantom:** dödar en spelare en Phantom faller kroppen till marken och blir en Carcass (platt på magen, vingarna
utbredda, ögonen slocknade). Efter omgång 1 ser den flådd ut (grått odött kött, trasiga vingmembran). Den släpper ingen
vanilla-loot direkt (Phantom Membrane kommer i omgång 2). Phantoms som brinner upp i solen försvinner utan kropp.

### Bära hem bytet (Carcass, Travois, Hanging Rack)
En Carcass som ligger på marken försvinner efter 5 minuter. Vill man ha ut mer av djuret bär man hem det.

- **Bära på axlarna:** smyg + högerklicka på kroppen med **tom hand**. Små djur (hare, kanin, fåglar, ekorre, igelkott;
  hitbox-bredd under 0,6) och mellanstora (rådjur, kronhjort, ren, får, gris, vildsvin, mufflon, varg, grävling, ko …)
  läggs över axlarna och syns där (i tredje person; i första person döljs den). Mellanstora gör dig **25 % långsammare**
  och du **kan inte springa**; små väger ingenting.
- **Släpa:** stora djur (bredd 1,1 eller mer: uroxe, häst, björn, älg, grottlejon, jättehjort) kan inte bäras. Samma grepp
  gör att du **släpar** kroppen efter dig, huvudet först, en kroppslängd bakom (den tar sig upp för steg). **50 % långsammare**,
  ingen sprint. Högerklicka på den igen för att släppa.
- **Båda händerna är upptagna:** inget i någon hand kan användas (äta, dricka, kasta, båge, Shield …), inga block kan
  placeras och inga items användas på djur (klippa, koppla, mata). Bara Travois (lasta) och kroppen själv (släppa) svarar
  på högerklick. Föremålen i händerna syns inte medan du bär (i första person syns inte heller händerna). Ett slag, att bryta
  ett block eller en **träff på 4 skada eller mer** tappar kroppen. Högerklicka på ett block för att lägga ner den där. Dör
  eller loggar du ut, eller kommer mer än 12 block ifrån den, ligger den kvar.
- **Armarna håller kroppen**, och alla spelare ser det: bär du på axlarna är båda armarna uppe och håller kroppen, släpar
  du sträcker båda armarna sig bakåt mot den.
- **Blodspår:** en färsk kropp blöder på vägen, glest men lätt att följa. En släpad kropp lämnar en blodfläck på marken
  ungefär **var 2:a block**, en buren droppar då och då och lämnar en fläck ungefär **var 6:e block**. Står du still blir
  det inga fläckar. Har kroppen åldrats på en Hanging Rack, börjat bli dålig eller förskämts blöder den inte längre.
  Spåret, liksom den blodiga kroppen, kan locka rovdjur på natten.
- Burna, släpade, lastade och upphängda kroppar försvinner inte, men köttet förskäms: **ett dygn (20 min) efter att djuret
  dog** om kroppen bärs eller ligger på Travois/vagn, **tre dygn (1 h)** medan den hänger på en Hanging Rack (svalt och
  luftigt). Då ger styckningen Rotten Flesh i stället för kött (hud och resten som vanligt). Tas den ner från ställningen
  gäller ett dygn igen. Vid **75 % av tiden** börjar kroppen bli dålig: den får en grönaktig ton och flugor surrar runt den.
  **Högerklicka en hängande kropp med tom hand** för att se hur länge den hängt och hur länge den håller. Läggs kroppen på marken igen
  börjar 5-minutersklockan gå.

**Travois** (släpsläde): recept `[Tool Rod][ ][Tool Rod]` / `[Cordage][Scraped Hide][Cordage]` / `[Tool Rod][ ][Tool Rod]`
(låses upp med en Scraped Hide). Två långa stänger som korsas framtill och är surrade där, tre tvärslåar och en spänd hud.
- Högerklicka på marken för att ställa ner den. **Högerklicka med tom hand** för att spänna dig för den (och igen för att
  släppa). Den följer 2,4 block bakom dig, med bakändarna släpande på marken, och klättrar upp för steg (1 block).
- **Följer marken:** släden har tyngdkraft (faller 0,08 block/tick², högst 1,5 block/tick), glider längs marken och tar
  sig upp för ettblockssteg. Stängerna lutar från bakändarna på marken upp mot **höften** på den som drar (halva
  kroppshöjden). Rörelsen räknas ut både på servern och i klienten (som för föremål på marken) och klienten jämkar in
  serverns läge över 3 tick, så släden glider jämnt efter dig i stället för att hacka eller sväva.
- **Fastnar den** (en vägg, ett träd, en för smal öppning) håller den emot som ett rep: du kommer inte längre än
  **1 block** utöver dragavståndet (3,4 block för travoisen) från den. Fortsätter du dra **bort** från den i **3 sekunder**
  släpper du taget ("The travois is stuck. You let go of the poles"). Hamnar du ändå mer än **5 block** bort (teleport,
  fall, knuff) glider stängerna ur händerna ("The poles slipped out of your hands"). Gå tillbaka och lirka runt hindret.
- **Släppt** ligger den på marken: bakändarna och de korsade framändarna vilar på marken, framänden en aning upphöjd
  (~4°), och den lutar efter marken framför (upp på ett block, ner i en grop). Lasten ligger på huden och lutar med den.
- **Last:** en stor eller två mellanstora/små kroppar. Högerklicka på släden med kroppen du bär eller släpar för att lasta
  (jättehjorten är för stor). **Smyg + högerklicka** med tom hand lastar av den sista. Lasten ligger på huden och syns.
- Fart: −10 % tom, −10 % till per lastenhet (en stor = 2), alltså −30 % med en uroxe mot −50 % om du släpar den. Lastad
  går det inte att springa. Slå på släden för att plocka upp den (lasten hamnar på marken).
- I bronsåldern kommer **Wheeled Cart** (se 02-bronze-age.md, *Vagn och häst*): dubbelt så mycket last, ett förråd, och en tam häst kan dra den.

**Hanging Rack** (slaktställning): recept `[Tool Rod][Cordage][Tool Rod]` / `[Tool Rod][ ][Tool Rod]` /
`[Tool Rod][ ][Tool Rod]` (låses upp med en Raw Hide). Två stolpar **tre block** höga, en tvärbjälke och en gambrel (krokpinne)
som hänger i ett snöre, så att kroppen hänger fritt över marken. Byggd av tre block (nedre, mitten, övre) som var och en
har sin egen träffyta (bara stolparna och bjälken, man kan slå mellan stolparna). De två blocken ovanför måste vara fria.
Bryts en del försvinner hela ställningen (den släpps som ett item) och kroppen faller ner.
- **Högerklicka på ställningen** med kroppen du bär eller släpar: den hänger med huvudet nedåt i bakbenen, buken mot
  framsidan (mot den som ställde ut ställningen). En kropp per ställning, jättehjorten är för stor. Smyg + tom hand tar ner
  den igen (på axlarna eller för att släpa). Bryts ställningen faller kroppen ner. Det droppar blod under den.
- **Benen hänger naturligt:** bakbenen pekar rakt upp mot gambreln (lite isär), frambenen/vingarna dinglar ned mot
  marken, och hals och huvud hänger slappt i linje med kroppen med öron och svans nedhängande.
- **Mognad syns:** när kroppen har hängt **3 minuter** (×1,25 kött, se nedan) har den torkat: pälsen/huden blir mörkare,
  brunare och mattare, och en flådd kropp får mörkt, torkat kött med en skorpa i kanterna, intorkade blodstänk, fläckar av
  glans och gulnat fett (egna texturer under `textures/entity/skinned_aged/`, `scripts/gen_aged_textures.py`). Blodet slutar
  droppa och en och annan fluga surrar runt den. Utseendet följer med om man tar ner den, tills den hängs upp igen.
- **Styckning på ställningen (med kniv som vanligt) ger rejält mer:**
  - Omgång 1: hela huden/pälsen/ullen/fjädrarna kommer av, **ingen slump** (alla delar i tabellen faller, med knivens
    multiplikator på antalet).
  - Omgång 2: **dubbelt så mycket kött** som scraps-utbytet (samma slag × knivens multiplikator × 2), styckat i stora bitar:
    **Raw Haunch** (lår, 3 enheter), **Raw Ribs** (revben, 2) och **Raw Loin** (filé, 2), högst 4 Haunch, 2 Ribs och 2 Loin
    per kropp; det som blir över faller som vanliga scraps. Har kroppen hängt **3 minuter** har den mognat: **×1,25** kött.
    Sinew, Lard, Bear Fat och Bone faller **alltid, en extra**; djur utan ben i tabellen ger 1–3 Bone (fler för stora).
    Ingen Bone Pile blir kvar (benen kom med).
  - Exempel med Flint Knife: kronhjort (2–4 scraps på marken) → 4–8 enheter, t.ex. 1 Haunch + 1 Ribs + 1 Loin och 1 scrap;
    uroxe (3–5) → 6–10 enheter.
- 1 enhet = 1 scrap = 1 Smoked Meat. Bitarna förskäms (rå: 2 dagar, tillagade: 3 dagar) och blir Rotten Flesh.

| Bit | Hunger | Mättnad | Tillagas | Smoking Rack |
|---|---|---|---|---|
| Raw Haunch | 3 | 0,2 | Campfire 30 s → **Cooked Haunch** (9 / 0,8) | 3 Smoked Meat |
| Raw Ribs | 2 | 0,2 | Campfire 25 s → **Cooked Ribs** (6 / 0,8) | 2 Smoked Meat |
| Raw Loin | 2 | 0,2 | Campfire 25 s → **Cooked Loin** (6 / 1,0) | 2 Smoked Meat |

En hel bit hängs på Smoking Rack som en remsa och röks till en Smoked Meat per enhet. (Drying Rack tar dem också men ger
bara 1 Jerky per bit, så det lönar sig inte.)

**Rovdjuren känner lukten** (se *Rovdjur på natten*): blodiga kroppar drar till sig Grey Wolves och Cave Lions inom 40 block
när de jagar (natt, blodmåne eller svält). Bär, släpar eller drar du en kropp på en travois blir **du** bytet (även nära
lägret, men elden skrämmer dem som vanligt). En kropp som ligger obevakad eller hänger på en ställning går de fram till och
**äter upp på 10 sekunder** (Bone Pile blir kvar på marken), om det inte brinner en eld inom 8 block från kroppen (utom
under blodmåne eller vid svält). **En lägereld bredvid slaktställningen skyddar den.** Rovdjuren äter inte sin egen sort.
Har en spelare en kropp inom 16 block är chansen **dubbelt så stor** att en flock eller ett lejon kommer på natten.

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

### Viltet kommer tillbaka
Djur placeras när en chunk skapas, så ett område man jagat tomt skulle annars förbli tomt. Var 30:e sekund tittar spelet
på en plats 48–96 block från varje spelare. Om platsens område (128×128 block) inte har fyllts på på **3 dagar** och det
finns färre än 3 djur inom 48 block där, vandrar **en flock** av något som biomen har (samma lista som när världen skapas)
in där, utom synhåll (aldrig närmare än 32 block från en spelare) och inte nära en brinnande eld. Ett område man ser för
första gången räknas som påfyllt just då. Tiderna sparas med världen (`WildlifeRestock.java`).

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
| Desert Hare | 8 | Som haren men blek sandfärgad; öken och badlands (1–2) | Rabbit Hide 35 % | 1 Rabbit Scraps, Rabbit's Foot 8 % |
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
- En björn djupt inne i ett ide (eller annan grotta) går ibland ut i dagsljuset.
- **Kräver Tusk Dagger eller Iron Knife för att styckas** (Flint Knife: "This hide is too tough for your knife").
  Omgång 1: **Raw Bear Pelt** (torkas till Bear Pelt på en hög Drying Rack, se Drying Rack) + Raw Hide 40 %. Omgång 2: 4–6 Game Meat Scraps, **Bear Fat** 80 % (1–3), **Bear Claw** 60 % (1–3), Sinew.

### Småvilt i skogen
| Djur | Var | Beteende | Styckning |
|---|---|---|---|
| **Squirrel** | Skog, björkskog, dark forest, taiga (1–2) | Mycket skygg. Skrämd springer den till närmaste träd, **klättrar upp** i kronan, sitter där en stund och klättrar ner med huvudet först | 1 Game Meat Scraps, **Squirrel Pelt** 70 % |
| **Hedgehog** | Skog, blomsterskog; ibland slätt och meadow | Nattaktiv. Flyr aldrig utan **rullar ihop sig** när någon kommer nära (sover hoprullad på dagen). Slår du den med bar hand sticker du dig (1 skada) | 0–1 scraps |
| **Badger** | Skog, dark forest, taiga, i sluttningar (1–2) | Håller sig undan, men **biter hårt** (3) om den skadas eller du kommer alldeles inpå utan att smyga | 1–2 scraps, Raw Hide 35 %, Lard 40 % |

**4 Squirrel Pelt + Twine → Raw Hide** (shapeless), som sedan skrapas och torkas som vanligt.

### Fåglar i flock
Lever i flockar som går och pickar på marken, sover på natten och då och då lyfter tillsammans (ledaren flyger upp, de
andra följer efter ett och ett), flyger runt en stund och landar igen. Kommer en spelare nära (smyg för att komma
närmare) eller blir någon i flocken skadad lyfter **hela flocken** på en gång och flyger bort från dig. **En pil dödar
alltid.** Lämnar ingen kadaver: fjädrarna (och lite kött) faller direkt — ett sätt att få fjädrar till pilar.

| Fågel | Var | Beteende | Drops |
|---|---|---|---|
| **Hooded Crow** (gråkråka) | Slätt, skog, björkskog, dark forest, savann (3–6) | Lös, föränderlig flock; landar på marken eller i trädtopparna | 1–2 Feather, 0–1 Game Meat Scraps |
| **Greylag Goose** (grågås) | Floder, träsk, frusna floder (4–7) | Flyger högt i **V-formation**, landar helst på vatten och simmar omkring | 2–4 Feather, 1–2 Chicken Scraps |
| **Rock Dove** (klippduva) | Meadow, windswept hills, stony peaks, klippstränder (4–7) | Snabb flock, landar på marken | 1–2 Feather, 0–1 Chicken Scraps |
| **Willow Ptarmigan** (dalripa) | Snöiga slätter/taiga, grove, taiga (3–5) | Trycker (låter dig komma nära), flyger lågt och kort. **Vit** där det snöar, annars brun med vita vingar | 1–3 Feather, 1 Chicken Scraps |
| **Seagull** (fiskmås) | Stränder, klippstränder (2–4); ibland floder och hav | Seglar i cirklar över vattnet och skriker, **dyker mot ytan där det finns fisk**, vilar på stranden eller flyter på vattnet | 1–3 Feather |

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

### Rovdjur på natten (Grey Wolf, Cave Lion)
Natten ute utan eld är farlig på riktigt. Rovdjuren **sover på dagen** och håller sig då undan från spelare som andra vilda
djur, men **jagar på natten** (och dygnet runt under blodmåne eller när de svälter, 2 dagar utan byte). Samma system som
övriga vilda djur: egna modeller och animationer (trav/galopp, nosen i marken, smygande, språng, ylande/vrål, hoprullad
sömn), kroppar, flådda texturer, styckning och blod.

| Djur | Biomer (grupp) | Liv / skada | Omgång 1 | Omgång 2 |
|---|---|---|---|---|
| **Grey Wolf** (gråvarg) | Alla taigor, skog, björkskog, dark forest, plains, snowy plains, grove, snowy slopes (flock 3–6) | 14 liv, 4 skada, snabb (fart 0,32, rusar med 1,55×) | **Raw Wolf Pelt** (torkas till Wolf Pelt på en hög Drying Rack) | 1–2 Game Meat Scraps, Sinew 50 %, 1–2 Bone 60 % |
| **Cave Lion** (grottlejon) | Savanna, savanna plateau, windswept savanna, sunflower plains, windswept hills, windswept gravelly hills, stony peaks, meadow (1–2) | 30 liv, språng 8 skada, slag 5, svår att knuffa | Raw Hide 70 % + Raw Hide 40 % | 3–5 Game Meat Scraps, Sinew 60 % (1–2), 1–3 Bone 70 % |

**Grey Wolf:**
- **Ylar** (hörs ~100 block bort, en tidig varning): ledaren ylar när flocken får upp ett spår och ibland av sig själv på
  natten (~varannan minut). De andra svarar en efter en i olika tonhöjd. En flock som kommer på natten ylar några sekunder
  efter att den dykt upp. (26.3 har inget vargylande längre, så ylet är vanilla-vargens gnäll nedstämt.)
- **Spårar lukten** utan att behöva se dig: 32 block, **48 om du är skadad** (under halva hälsan), 14 om du smyger. Annars
  tar de byten inom 24 block: Roe Deer, Red Deer (hindar), Reindeer, Hare, Mouflon, Sheep och **stenåldersmänniskor**.
- **Följer på avstånd och omringar:** på över 20 block travar de efter dig (hukande när de kommer närmare). Sedan tar varje
  varg en egen plats på en ring 5,5 block runt dig: **bakom dig och på sidorna**. De rusar in och biter när läget är rätt:
  vargen står bakom dig, eller två andra står på plats, eller efter ett tag ändå (~12 s). **Mycket snabbare** (efter 1 s) om
  du är skadad, ensam i mörkret (blockljus under 5 och ingen annan spelare eller människa inom 10 block) eller har slagit
  dem. Efter ett bett faller vargen tillbaka till ringen (1,5–4 s) och nästa rusar in.
- **Drar sig undan** när en varg har under 35 % hälsa, eller när flocken har halverats (då flyr de i 30 s och kan försöka
  igen senare). På morgonen ger de upp jakten, om du inte slogs med dem.
- **Rädd för eld:** håller sig 12 block från en brinnande Campfire eller tänd Clay Kiln och går inte på någon som står inom
  8 block från elden: de **smyger runt elden ~16 block bort**, i mörkret vid kanten av eldskenet. **Håller du en fackla**
  (Torch, Soul Torch, Copper Torch, Lantern; taggen `mechanical:scares_predators`) står de kvar runt dig på 6,5 blocks
  avstånd utan att bita, och hoppar undan om du kommer närmare än 4,5 block. **Under blodmåne, eller om de svälter, hjälper elden inte.**
- Vanilla-vargen (den tämjbara) spawnar inte längre där gråvargen lever. Den finns kvar i savannplatåer, badlands och
  djungelkanter.

**Cave Lion:**
- **Bakhållsjägare:** ser spelare inom 24 block (10 om du smyger). Smyger fram **hukad** (långsamt och tyst, inga fotsteg),
  **fryser** när du tittar åt dess håll och arbetar sig runt bakom dig. Från 2,5–7 block, när du inte tittar: ett **språng**
  som gör 8 skada. Sedan slåss den med slag (5) och kan hoppa igen (var 5:e sekund).
- Ser du den på nära håll (inom 12 block) i 3 s, eller slår den, slutar den smyga: ett **djupt vrål** (hörs långt) och
  anfall i det öppna. Flyr under 25 % hälsa.
- Jagar också Roe Deer, Red Deer, Reindeer, Wild Horse, Aurochs (kor), Mouflon, Wild Boar och stenåldersmänniskor.
  **Släpar bort bytet:** en färsk, orörd Carcass inom 4 block bärs i käftarna upp till ~20 block bort från spelare.
- Rädd för eld och facklor som vargen (morrar och backar undan till ~7 block), utom under blodmåne eller vid svält.

**Båda dras till blodiga kroppar** inom 40 block (se *Bära hem bytet*): den som bär eller drar en kropp blir jagad, och
obevakade eller upphängda kroppar äts upp om ingen eld brinner inom 8 block.

**Var och hur ofta:** några flockar och lejon finns redan när världen skapas (de sover på dagen). Dessutom har varje
spelare i varglandet (biomtaggen `mechanical:has_grey_wolves`) **var 30:e sekund på natten 6 % chans** att en flock på
3–6 kommer 48–64 block bort, om det inte redan finns gråvargar inom 96 block. Hälften av flockarna jagar och har din lukt
från början, resten strövar (och kan få upp spåret senare). En natt i taigan ger alltså oftast ett ylande och ibland en
flock som anfaller (~70 % chans för minst en flock under en hel natt). I lejonlandet (`mechanical:has_cave_lions`) är det
2,5 % för ett lejon (1/3 chans att det är ett par). Dubbelt så ofta under blodmåne. Aldrig inom 28 block från en brinnande
eld, och bara på mörk, torr mark. Djur som kommer på natten försvinner när ingen spelare är i närheten (de från
världsskapandet stannar). Testa med `/nightpredators wolf` eller `/nightpredators lion` (op): en ankomst runt dig direkt.

Nytt item: **Wolf Pelt**: Wolf Hood, Wolf Fur-rustning, Wolf Rug och fäll på Straw Bed (se "Wolf Pelt: Wolf Hood, Wolf Fur och Wolf Rug" och "Straw Bed (vila)"). Spawn eggs för båda.

### Natten
- **Vargar och grottlejon** jagar på natten (se *Rovdjur på natten*): ylande på långt håll och flockar som omringar dig.
  Eld och facklor håller dem borta.
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
  spelare inom 6 block från elden (skeletons når inte dit) och spawnar inte inom 18 block. Glöd och släckt eld skyddar inte.
  **I kanten av ljuset:** monster utan mål nära en eld (inom 40 block) samlas på en ring 21–25 block ut, smyger runt den,
  stannar och stirrar på den som sitter vid elden och låter då och då. Elden håller dem borta, men inte ur sikte.
- **Blodmåne:** ungefär var sjunde natt (bestämt av världens seed, så en omstart ändrar inget) stiger månen full och
  blodröd vid skymningen och står kvar till gryningen. Himlen, dimman och månskenet blir röda, stjärnorna bleknar och natten
  blir lite ljusare i rött. Vid skymningen kommer ett meddelande ("The moon rises red...") och ett mörkt hornljud. Under
  blodmånen spawnar dubbelt så många extra monster (upp till 20 inom 48 block i stället för 12) och zombiehorderna kommer.
  Monstren är rasande: de **slår sönder facklor** inom räckhåll även medan de jagar dig (och söker upp facklor i
  närheten oftare), och alla närstridsmonster som går på marken (inte creepers, skelett eller endermen) **slår in
  trädörrar**, som zombier gör på Hard. **Elden skyddar inte under blodmånen:** monstren håller sig inte undan från
  campfires och kilns, anfaller spelare som står vid elden och kan spawna ända in till 10 block från den (annars 28).
  Testa med `/bloodmoon` (op), som gör kvällens natt till blodmåne.
- **Blåsiga nätter:** ungefär var femte natt (bestämt av världens seed, oberoende av blodmånen) blåser det från
  skymning till gryning. Ett meddelande vid skymningen, vindbyar hörs och aska driver förbi. **Lägereldar och facklor
  under bar himmel kan blåsa ut:** en Campfire sjunker till glöd (mata med Firewood), en fackla blir en **Unlit Torch**
  (tänds igen med en fackla, Fire Drill eller Flint and Steel; bryts den får man facklan). I snitt håller en fackla ute
  2–3 min och en lägereld ~3 min i vinden. **Tak över, eller väggar på tre sidor, skyddar.** Testa med `/windynight` (op).
- **Regn släcker:** när det regnar på en Campfire eller fackla under bar himmel kan den slockna, när som helst på dygnet
  (samma sätt som vinden: glöd respektive Unlit Torch). Lite långsammare än vinden: en fackla håller i snitt 4–5 min och en
  lägereld ~6 min i regnet, **hälften så länge vid åska**. Tak skyddar (då regnar det inte på den). I snöbiomer regnar det
  inte, där gäller bara vinden. Regnet gör också att en eld bränner sitt bränsle dubbelt så fort.
- **Zombiehorder:** bara under blodmåne, i två vågor (en tidigt på natten och en efter midnatt), med 8–12 zombies per våg
  32–48 block från en spelare. De håller ihop och
  går **först på ljuset**: de söker upp närmaste brinnande Campfire eller Clay Kiln (inom 48 block) och släcker den helt
  (ingen glöd kvar), och slår sönder facklor och lanterns inom 10 block. Först när det inte finns ljus kvar i närheten går
  de mot spelarna. Elden skrämmer inte bort dem.
- **Facklor** skyddar inte och värmer inte. Monster utan mål kan dras till en fackla i närheten (inom 8 block) och slå sönder den.
  Lanterns rörs inte.
- **Dynamiskt ljus** (klient): en fackla, lantern eller annat lysande block i handen, eller på marken, lyser upp runt
  omkring för alla spelare. Ljuset sprids som vanligt blockljus: väggar stoppar det, men det går runt hörn.

### Mörkerrädsla (sanity)
Står man länge i totalt mörker börjar man bli tokig. **Inget väsen anfaller dig**, det är bara galenskapen själv: den
lurar dig med ljud och syner och döljer på så sätt de riktiga farorna (monster och rovdjur). Varje spelare har ett dolt
värde **Sanity 0–100** (sparas, synkas till klienten, nollställs till 100 vid död). Bara i världar med himmel
(overworld); Nether och End påverkar det inte. Creative och spectator påverkas inte.
- **Mörker:** ljuset vid ögonen är högst 3 och det är inte dagsljus. Ljuset är blockljus, eller ljus som någon inom 8
  block håller i handen (fackla, lantern o.s.v., samma regel som dynamiskt ljus, minus 1 per blocks avstånd). Håller
  man själv en fackla räknas det alltså som ljust. 4–7 är skymning (ingen ändring), 8+ är upplyst.
- **Tappar (per sekund i mörker):** 0,10 i grunden, ×1,25 ute på natten, ×1,5 under jord (ingen himmel och under
  havsnivån), ×0,5 med sällskap (annan spelare eller vänlig tribesperson inom 12 block), ×2 under blodmåne. Ensam ute
  i mörkret på natten: 0,125/s, alltså 100 → 0 på ~13 minuter (100 → 70 på 4 min). Under blodmånen tappar man också
  0,05/s i skymningsljus.
- **Kommer tillbaka (per sekund):** vid brinnande eld (Campfire eller tänd Clay Kiln inom 6 block) +0,5, alltid, även
  under blodmåne; i dagsljus +0,3; på upplysta ställen +0,05; sällskap +0,1 extra. Under blodmånen halveras allt
  utom elden. **Vila i Straw Bed** ger +20 (+35 i en Furred Straw Bed; bara +8 om man är under 15, "man vänder och vrider sig") och hjälper bara
  om det gått minst 2 minuter sedan förra vilan. **Varm mat** (cooked/roasted, stuvningar, soppor, bakad potatis)
  ger +3.
- **Under 70:** "You feel watched..." Viskningar bakom dig (grottljud och själar, lågt tonade), fotsteg som följer
  efter och stannar, en kvist som knäcks bakom dig; var 45–65:e sekund vid 70, oftare ju lägre. Svag vinjett och
  färgerna börjar blekna (himmel, dimma och ljus dras mot grått, upp till 60 % vid 0). Ett öga visas till höger om
  hotbaren, värre för varje steg.
- **Under 40:** "The dark presses in." Mörka gestalter i ögonvrån (i kanten av synfältet) som försvinner när man
  tittar på dem, hjärtslag (snabbare och starkare ju lägre), en långsam gungning i bilden och falska monsterljud: ett
  zombiestön, en creeper som fräser precis bakom dig, skelett, spindlar. Undertexterna visar som vanligt, så de går
  inte att skilja från riktiga.
- **Under 15:** "Your mind is slipping." Kraftig vinjett, falska stötar (skadetilt och skadeljud utan skada) och ett
  **inbillat monster** (zombie, skeleton eller creeper) som står 9–14 block bort och stirrar. Det löses upp i rök när
  man slår det, går närmare än 4 block, efter en minut eller när sanity stiger över 20; en inbillad creeper fräser när
  man är inom 7 block och försvinner sedan. **Riktiga effekter:** Mining Fatigue I och Weakness I så länge man är under 15.
- **Vid 0:** dessutom anfall av illamående (Nausea i 8 s, i snitt var 30:e sekund). Ingen direkt skada.
- Testa med `/sanity` (visar värdet), `/sanity <0–100>` eller `/sanity <värde> <spelare>` (op).

### Kyla
- Kroppstemperatur 0–20 (12 = lagom), visas som 10 termometrar ovanför matmätaren (varm / kall / iskall).
- Rör sig sakta mot en måltemperatur: biomen (snöbiomer är kalla även på dagen, öken varm), natten (−5 ute, −2,5
  under tak), regn (−2), i vatten (−5), höjd över y 100. **Brinnande eld inom 6 block värmer** (upp till +10).
- **Leather Armor** isolerar mot kyla: 15 % per del (60 % med full rustning). **Fur-rustning** 25 % per del (100 % = ingen kyla). **Wolf Fur** och Wolf Hood 20 % per del. En vila i en Furred Straw Bed ger +30 % i 5 minuter (högst 100 % totalt).
- **Under 8:** maten går åt fortare. **Under 3:** man fryser (frost på skärmen, långsam, tar frysskada).

### Rök inomhus
En eld i ett stängt rum fyller det med rök. **Källor:** tänd Campfire och Soul Campfire, och Clay Kiln när den brinner
(inte facklor eller ljus).
- **Rummet:** luften fylls ut från blocket ovanför elden (Clay Kiln: ovanför skorstenen), högst 500 block. Det räknas som
  stängt om fyllningen inte når ut. Väggar är alla "solida" block: sten, plank, glas, glasrutor, staket, **stängda** dörrar
  och fallluckor. Öppna dörrar/fallluckor och öppna grindar släpper ut röken. Rummet räknas om var 5:e sekund.
- **Så ventilerar man:**
  - **Rökhål/skorsten rakt ovanför elden:** en pelare av luft (eller andra icke-solida block) från elden upp till bar
    himmel. Då blir det ingen rök alls, hur stängt rummet än är. En öppen fallucka i taket fungerar som stängbart rökhål.
  - **En öppning i väggen** (dörröppning, öppen dörr, fönsterhål) släpper ut röken.
  - **Ett ensamt hål i taket som inte är över elden** läcker bara: röken byggs upp hälften så fort. Två eller fler hål
    ventilerar.
  - Ett rum större än 500 block (stor grotta) blir inte rökigt.
- **Förlopp** (litet rum, ~100 block luft): röken når fullt på **~50 s** (30 s + 1 s per 5 block luft; ett större rum
  tar längre tid). Grå dimma och rök i luften på klienten som tätnar med röken (sikten ner till ~4 block vid full rök).
  Från **halv rök:** hosta och Slowness. Från **90 %:** **½ hjärta varannan sekund** (rustning hjälper inte,
  dödsmeddelande "choked on smoke"). Djur, monster och Tribespeople i rummet tar också skadan.
- **Trä tar eld:** efter **3 minuter på full rök** kan brandbara block i rummet (plank, stockar, ull, trädetaljer …)
  fatta eld: varje sekund 1 på 20 att eld uppstår i luften bredvid något brandbart. I testet tog det ~3,5 min på full rök.
- **Röken skingras** långsamt när elden slocknar eller rummet öppnas: från full rök till ingenting på 1,5 minut.
- Testa med `/smoke <pos>` (op): röknivån vid positionen och alla eldar som följs (stängt, rumsstorlek, läckor).
  Eldar hittas inom 3 chunks från spelare (och runt positionen i `/smoke`).

### Sot
Samma eldar som ger rök (tänd Campfire, Soul Campfire, brinnande Clay Kiln) sotar ner ytorna runt sig, både ute och inne.
- **Block: Soot.** Ett tunt svart lager på blockytor (som Glow Lichen): ingen kollision, går att ersätta, kan sitta på
  flera sidor av samma luftblock. Tre stadier: **1** ljus fläck, **2** sotigt, **3** tjockt svart sot (gäller alla sidor
  i blocket). Regn tvättar inte bort det, det sitter kvar tills man skrapar bort det. Har inget eget item.
- **Var:** 2 av 3 gånger i taket ovanför elden (upp till 6 block upp, utspritt ±2 block, mest rakt ovanför), annars
  på en vägg upp till 3 block åt sidan (från eldens höjd till 2 block ovanför röken). Utan tak blir det inget sot i
  taket. Bara på hela blocksidor (inte på staket, facklor o.d.).
- **Takt:** inget sot de första **2 minuterna** en eld brinner (räknas om när elden slocknar). Därefter 1 chans på 15
  varje sekund att sot läggs eller att ett befintligt sotlager blir ett stadium mörkare; i ett rökigt rum upp till
  **dubbelt så ofta** (× 1 + rummets röknivå, se Rök inomhus). En eld som får gå ut av sig själv (3–5 min) hinner
  bara ge några fläckar; det krävs en eld som hålls vid liv med Firewood.
- **Testat (20 min brinnande eld, tak 4 block över elden):** stängt 7×7-rum (full rök): 50 sotblock, 24 i taket och
  26 på väggarna, 16 av dem stadium 3. Öppet skjul (bara tak och en vägg): 21 sotblock, 19 i taket, 10 stadium 3.
  Första fläckarna kom strax efter 2 minuter.
- **Skrapa bort:** bryts för hand (lite segt), **4× snabbare med en kniv**. Ger item **Soot**: stadium 1 50 % chans på
  1, stadium 2 1 st, stadium 3 2 st; med kniv (Flint Knife, Iron Knife) **+1**.
- **Item: Soot**, ett svart pigment: 1 Soot → 1 Black Dye (formlöst).
- **Härden nöts in:** efter **1 minut** brinnande eld (samma räkning som sotet) ändras marken runt elden, 1 chans på 20
  varje sekund, en plats i taget inom 2 block: gräs, blommor och plantor brinner bort; Grass Block, Podzol, Mycelium och
  Moss Block blir Dirt, Dirt blir Coarse Dirt, och sedan (eller direkt på sten, grus, sand osv.) läggs ett sot- och
  asklager på marken som blir mörkare i 3 stadier. En eld som hålls vid liv i ungefär en timme har bränt en mörk ring
  runt sig. Gäller Campfire, Soul Campfire och Clay Kiln.

### Pigment och grottmålningar
Jordfärger, krita och sot mals till pigment, blandas till färg i en skål och målas på berget med en pensel.
Kod: `Pigments` (registrering, malning på städet), `CavePaintingBlock`, `PaintItem`, `PaintbrushItem`.
Data och alla texturer: `scripts/gen_pigments.py`.
- **Ytfynd (worldgen):** lösa klumpar som Loose Rocks (`RockBlock`, storlek 1–2, plockas för hand med ett slag:
  liten 1, mellan 2). Ligger i små samlingar på naturlig mark, sand, grus, sten, lera och terrakotta, aldrig i vatten.

| Block | Biom | Ger |
|---|---|---|
| Loose Red Ochre | Badlands, Eroded/Wooded Badlands, Savanna, Savanna Plateau, Windswept Savanna | Red Ochre |
| Loose Yellow Ochre | Savanna (alla tre), Desert, River (flodbankar) | Yellow Ochre |
| Loose Chalk | Stony Shore, Beach, Meadow, Windswept Hills, Windswept Gravelly Hills, Stony Peaks | Chalk |

  **Testat (5×5 chunks per biom):** Badlands 19–21 Red Ochre, Savanna 36 Red + 11 Yellow, Savanna Plateau 37 Red +
  38 Yellow, Desert 19 Yellow, Meadow 22 Chalk, Windswept Hills 23 Chalk, Stony Shore 13 Chalk, Beach 2 Chalk,
  Plains 0. Ungefär ett fynd per chunk i rätt biom.
- **Mala till pigment:** för hand med **Flint Pickaxe** i crafting (hackan ligger kvar och slits 1) → **1** pigment;
  på **Stone Anvil** (lägg på, 4 slag med hammarsten) → **2**.

| Råvara | Pigment | För hand | Stone Anvil |
|---|---|---|---|
| Red Ochre | Red Pigment | 1 | 2 |
| Yellow Ochre | Yellow Pigment | 1 | 2 |
| Chalk | White Pigment | 1 | 2 |
| Calcite (block) | White Pigment | – | 4 |
| Charcoal | Black Pigment | 1 | 2 |

  **Soot** fungerar direkt som svart pigment (behöver inte malas).
- **Färg (formlöst):** 2 pigment (svart: Black Pigment eller Soot) + bindemedel (**Tallow**, Clay Water Bucket eller
  Water Bucket; hinken blir kvar) + **Bowl** → **Black / Red / Yellow / White Paint**. **16 användningar**, sedan
  blir skålen kvar.
- **Paintbrush:** Stick + 2 Plant Fibre (formlöst), 64 användningar. Håll penseln i ena handen och färgen i den
  andra, högerklicka på en hel blocksida (vägg, tak eller golv) → en **Cave Painting** i färgens kulör. Varje block
  tar 1 färg och 1 av penselns slitage. **Smyg + högerklick** byter motiv (visas över hotbaren och i tooltip).
  Motiv: Aurochs, Horse (den runda "kinesiska hästen" från Lascaux), Stag, Cave Bear, Mammoth, Ibex, Hunter, Sun,
  Spiral, Dots, och två **scener två block breda**: **The Hunt** (två jägare med spjut mot en uroxe med spjut i
  ryggen) och **Running Herd** (fem hästar i galopp). Scener behöver två väggytor bredvid varandra (inte tak/golv)
  och tar 2 färg.
- **Handavtryck:** högerklicka med färgen utan pensel i andra handen → **Hand Print**.
- **Block: Cave Painting.** Tunt halvgenomskinligt lager (berget syns igenom), ingen kollision, går att ersätta.
  4 färger × 15 motivdelar × 6 riktningar. Måla på en befintlig målning för att byta motiv/färg. Försvinner om
  blocket bakom tas bort. Bryts lätt och ger inget, **4× snabbare med kniv**. **Regn:** under öppen himmel tvättas
  den bort (1 på 6 per random tick när det regnar, ungefär 7 minuters regn); i grottor och under tak sitter den kvar.
  Inget eget item (inte i creative heller), placeras med `/setblock ~ ~ ~ mechanical:cave_painting[facing=south,motif=aurochs,colour=red]`.
- **Pigment → färgämne:** Black / Red / Yellow / White Pigment → 1 Black / Red / Yellow / White Dye (formlöst).
- Recepten låses upp när man får Red Ochre, Yellow Ochre, Chalk, Soot eller något pigment.

### Fisk
- Döda fiskar och fångst släpper **Whole Cod / Whole Salmon / Whole Tropical Fish / Whole Pufferfish** (gäller både
  döda fiskar och fiskespö). De går inte att äta eller laga.
- **Rensa:** Whole fish + Flint Knife (crafting, kniven slits 1) → vanilla rå fisk (filén) + **Fish Bones**.
- Fish Bones → 1 Bone Meal.

### Fällor och snaror
Mat utan att jaga själv. Båda recepten låses upp när man får Twine. Koden: `Traps.java`, `SnareBlock.java`,
`FishTrapBlock.java`; data `scripts/gen_traps.py`, texturer `scripts/gen_trap_textures.py`.

**Snare** (crafting table): `[Stick][Cordage]` / `[Stick][ ]` → 1 Snare. Ett lågt block på marken: två pinnar med en
tvärslå och en snara av snöre som hänger tvärs över stigen (vänd mot den som sätter ut den).
- **Fångst:** ett litet vilt djur (vuxet, vilt, lämnar kropp, hitbox högst 0,75 block: Hare, Desert Hare, Rabbit,
  Squirrel, Pheasant, Badger, Hedgehog, Chicken, Fox …) som går in i en gillrad snara fastnar med **80 %** chans: det
  dör där och blir en vanlig **Carcass** som styckas som vanligt. Annars slinker det ur, får panik och snaran är utlöst tom.
  Flockfåglar fastnar inte. Kroppen ligger kvar i 5 min som vanligt.
- **Snaran går sönder** vid **15 %** av fångsterna: blocket försvinner och en Stick blir kvar.
- **Lockar djur:** var 10:e sekund kan en gillrad snara få ett litet djur i närheten att gå fram till den
  (ett slumpvis djur i området, det ger upp efter 30 s). Rädsla går före: ser djuret en spelare springer det i stället.
  - **Betad** (högerklicka med bär, frön, säd, Peas, Carrot eller Apple; taggen `mechanical:snare_bait`): inom **8 block**, **50 %** chans per 10 s.
  - **Obetad:** inom 6 block, **4 %** per 10 s; **×2** på en stig (Dirt Path, Worn Grass, Trodden Path, Coarse Dirt) eller nära bärbuskar (3 block),
    **×2,5** från skymning till strax efter gryning (dagtid 12000–1000).
  - Djur som vandrar förbi kan också gå rakt in i snaran av sig själva.
- **Utlöst** snara (nedfallen tvärslå, snaran åtdragen på marken) fångar inget. **Högerklicka** för att gillra den igen (betet är borta).
- **Spelare** som går över en gillrad snara löser ut den och snubblar (**Slowness II i 2 s**), ingen skada. Smyger man kliver man över.
  Stora djur (rådjur, vildsvin …) trampar sönder den, alltså löser ut den tom.
- Testat på dev-server: 4 betade snaror i en inhägnad med 6 småvilt: 3 fångster på 2 minuter (Squirrel, Pheasant, Badger), en snara gick sönder.

**Fish Trap** (crafting table): `[Stick][Twine][Stick]` / `[Stick][ ][Stick]` / `[Stick][Twine][Stick]` → 1 Fish Trap.
En flätad korg av pinnar med en tratt i mynningen, som läggs i vatten (stilla eller rinnande; den blir vattenfylld).
- Vid varje random tick (~68 s i snitt) kan den fånga en fisk, upp till **3**. Fiskarna syns genom flätverket.
  **Högerklicka** för att tömma (Whole fish som rensas med kniv). Bryter man fällan faller fisken ut.
- Kräver **minst 12 vattenblock** inom 3 block åt sidorna och 2 nedåt: i en liten damm fångar den inget. På land fångar den inget.
- Chans per random tick: **20 %** i sjö/damm, **×2 i flod** (River-biom), **×1,25** i hav/strand, **×1,5** om rinnande vatten
  ligger bredvid; högst 60 %. Ungefär en fisk per 5,7 min i en sjö, 2,8 min i en flod.
- Art efter biom (bestäms när man tömmer): flod 70 % Whole Salmon / 30 % Whole Cod; varmt och ljummet hav 50 % Whole
  Tropical Fish, 10 % Whole Pufferfish, 40 % Whole Cod; annars 70 % Whole Cod / 30 % Whole Salmon.
- Testat: två fällor i en flod fylldes till 3 med random_tick_speed 30; en fälla i en vattenpöl på 2 block fick inget.

### Förskämd mat, rökning och torkning
Färsk mat **förskäms** med tiden. Tiden räknas i speltid (ticks, inte `/time set`) från när maten först hamnar i ett
inventory. I tooltipen står **Fresh** (mer än halva tiden kvar), **Stale** (20–50 %) eller **Rotting** (under 20 %), och en
stapel under ikonen (som hållbarhet) går från grönt till rött. När tiden är slut blir maten **Rotten Flesh** (kött och fisk)
eller **Spoiled Food** (allt annat). Spoiled Food duger bara till Compost Heap. I en stängd **Bark Basket** åldras maten bara en tredjedel så fort. **Samma mat stackar igen:** högar av samma mat som bara skiljer sig i ålder slås ihop automatiskt (i inventoryt och i öppna kistor, en gång i sekunden), och den sammanslagna högen får den **äldsta** tiden.

| Mat | Håller |
|---|---|
| Rått kött (alla scraps, vanilla rått kött), rå fisk (filéer och Whole fish) | **1 dag** (20 min) |
| Bär (vilda, Sweet Berries, Glow Berries), vild svamp, Raw Porridge, Porridge, Peeled Egg | **2 dagar** |
| Tillagat kött och fisk (scraps och vanilla), Roasted Mushrooms, Boiled Egg | **3 dagar** |
| **Smoked Meat, Smoked Fish, Jerky, Dried Fish, Dried Berries, Dried Mushrooms** | **30 dagar** |

- Allt annat (bröd, äpplen, morötter, vanilla-soppor …) förskäms inte.
- Maten kontrolleras en gång i sekunden i spelarens inventory och i den behållare man har öppen. Mat som ligger i en kista
  förskäms **när kistan öppnas** nästa gång (tiden har gått ändå). Mat på marken räknas inte förrän den plockas upp.
- Mat som stämplas inom samma 1/16 av sin hållbarhet staplas ihop (rått kött: inom 75 s); annars blir det två högar.
- **Äta Rotting-mat:** 50 % chans på Hunger i 30 s. Stale går bra att äta.
- Porridge-skålen försvinner om gröten förskäms.
- Kyla bromsar inte förskämningen (än).

**Konserverad mat** (görs på Smoking Rack och Drying Rack, se Block):

| Mat | Från | Hunger | Mättnad |
|---|---|---|---|
| Smoked Meat | rått kött, 4 min rök | 3 | 0,8 |
| Smoked Fish | Raw Cod/Salmon, 4 min rök | 3 | 0,7 |
| Jerky | rått kött, torkat i solen (~7–14 min) | 2 | 0,8 |
| Dried Fish | Raw Cod/Salmon, torkad i solen | 2 | 0,7 |
| Dried Berries | 4 bär → 4, torkade i solen | 1 | 0,5 |
| Dried Mushrooms | 4 ätliga svampar → 4, torkade i solen | 1 | 0,6 |

Mat som redan är **Rotting** går inte att röka eller torka ("This is too far gone to keep").

Rökning går fortare och ger lite mer mat; torkning kräver ingen eld men bara fungerar i solsken.

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
| Bear Den | Taiga, skog, dark forest | 120 | Grotta i en sluttning med mossiga stenblock vid öppningen, eller en grop under rotkakan på ett omfallet träd. Bädd av mossa och löv, hängande rötter, ben och en kruka; utanför en trampad stig, ett träd med klösmärken och rester av ett mål. Öppningen är minst 3 block bred, och en **Brown Bear** bor där och går ut på dagen |
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

### Stigar mellan platser (trails)
Upptrampade **stigar** går mellan närliggande större platser: byar, Tribe Villages och Ancient Oak.
Varje plats får stig till sina närmaste grannar inom ca **640 block**, men aldrig förbi en tredje plats som ligger
närmare båda (så det blir ett glest nät, inte ett spindelnät), och aldrig över hav. Stigen slingrar sig, följer
terrängen och är 1–3 block bred med trasiga, gräsiga kanter: **Trodden Path** i mitten med fläckar av Coarse Dirt, Gravel
och Packed Mud, och **Worn Grass** längs kanterna (se *Upptrampade stigar* nedan; stigarnas block växer aldrig igen). Den går
runt trädstammar och lämnar hus, åkrar, sand och berg i fred, och slutar några block från platsens mitt.
- **Vatten:** över djupt vatten (3+ block) en **spång** av plankslabs, med stockstolpar ner till botten och ett
  staketräcke var fjärde block (ek eller gran). I grunt vatten (1–2 block) **trampstenar** av kullersten och mossig
  kullersten, i nivå med ytan.
- **Rösen:** ungefär var 96:e block står ett litet stenröse (kullersten med en kullerstensmur ovanpå) bredvid stigen.

Vilka strukturer som får stigar styrs av taggen `mechanical:trail_connected` (bara strukturer med random_spread-placering).
Byggs bara i nya chunks.

### Upptrampade stigar (Worn Grass, Trodden Path)
Gräs som man går på om och om igen trampas ner till en stig, och en stig som ingen använder växer igen. Koden:
`TrampledPaths.java` (slitage, block), `TrampledPathBlock.java` (anslutningar, återväxt). Data: `scripts/gen_paths.py`,
texturer: `scripts/gen_paths_textures.py`.

- **Stadier:** Grass Block → **Worn Grass** (fläckigt gräs med bara jordfläckar) → **Trodden Path** (mest bar,
  hårdtrampad jord med småsten). Dirt och Coarse Dirt blir Trodden Path direkt. Båda är hela block (16 px, samma nivå
  som gräset, inte nedsänkta som vanilla Dirt Path).
- **Vad som räknas:** ett steg upp på ett *nytt* block på marken. Att stå still räknas inte, och inte heller att
  **smyga** (så man kan smyga över gräsmattan utan att slita på den). Spelare till fots, hästen man rider, och en
  Travois eller Wheeled Cart i sele (medarna och hjulen sliter också).
- **Takt:** varje steg på Grass Block ger **5 %** chans till Worn Grass, varje steg på Worn Grass (eller Dirt/Coarse Dirt)
  **10 %** chans till Trodden Path. En stig man går **ett par dussin gånger** (i snitt ~30) blir Trodden Path; en enda
  promenad lämnar bara någon enstaka sliten fläck. Blir det Trodden Path trampas Short Grass, Fern och liknande ovanpå bort.
- **Fart:** **Worn Grass +10 %**, **Trodden Path +20 %** gångfart, för spelare, djur och hästar. Vanillas
  `speedFactor` på blocket (1,075 och 1,14): faktorn multipliceras in i farten varje tick före markfriktionen
  (0,6 × 0,91), så den förstärker sig själv till (1 − 0,546) / (1 − 0,546 × f), därför inte 1,1 och 1,2. Gäller även
  sprint och när man hoppar från stigen.
- **Återväxt:** ett trampat block är "nyss använt" och växer inte. Det släpper efter i snitt **ett dygn** (1/18 per
  random tick). Sedan går det **ett stadium tillbaka** med **4 %** per random tick (~1,4 dygn) om det har ljus (9+)
  ovanför: Trodden Path → Worn Grass → Grass Block. En övergiven stig är gräs igen efter **ungefär 4 dygn**. Täcks
  blocket av ett tätt block eller vatten blir det Dirt, som gräs.
- **Övergång till gräset:** varje block vet om grannen åt norr, öster, söder och väster är stigmark (taggen
  `mechanical:path_connects`: Worn Grass, Trodden Path, Dirt, Coarse Dirt, Rooted Dirt, Dirt Path, Farmland, Gravel,
  Packed Mud, Mud). Ovansidan har en textur per kombination (16 per stadium): jorden löper ut till kanten mot
  stiggrannar (lika bred vid kanten, så två block möts sömlöst) och fransar upp sig i gräset åt andra hållen, så en
  slingrande stig ser sammanhängande ut. Gräset under jorden är vanillas Grass Block-ovansida i biomets gräsfärg, så det
  smälter ihop med gräset runt omkring. Varje kombination finns i 4 vridningar som väljs slumpvis. Sidorna ser ut som en
  Grass Block; med snö ovanpå blir blocket som snötäckt gräs.
- **Som gräs annars:** bryts med spade, ger Dirt (Silk Touch: blocket självt). **Hacka** gör Farmland, **spade** gör
  vanilla Dirt Path. Blommor, plantor, sockerrör m.m. kan växa på dem (taggen `minecraft:dirt`), och djur spawnar på dem.
- **Världens stigar** (*Stigar mellan platser* ovan) är byggda av samma block men växer aldrig igen.
- Snaror: Worn Grass och Trodden Path räknas som stig (×2 lockchans).

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
  Woodcutter (Flint Axe), Elder (stav). Var och en har ett eget namn och sin stam, t.ex. "Aruk the Hunter of the Raven Tribe".
- **De sköter sin omgivning:** en Woodcutter planterar en planta bredvid stubben efter varje träd han fäller, och när det
  finns färre än 4 träd inom 14 block från elden planterar han plantor runt lägret (sorten från närmaste träd, annars en
  som passar biomet). En Gatherer planterar unga vilda bärbuskar som passar klimatet när det finns färre än 3. Aldrig
  närmare elden än 5 block och med minst 4 blocks mellanrum, så ett läger på kal mark växer långsamt upp med skog och bär.
- **Läger** (ca 1 på 150 chunks i skog, taiga, slätt, meadow och vid floder; plan mark, aldrig nära byar): brinnande
  Campfire, 2–3 hudtält, Drying Rack och en sittstock. 3–5 personer: en Elder plus Gatherers, Woodcutters och Fishers
  (bara nära vatten).
- **Jaktlag** (ca 1 på 200 chunks i skog och taiga): 2–3 Hunters som följer en ledare.
- **Tribe Village** (sällsynt, högst en per 36x36 chunks, i slätt, meadow, skog, björkskog, taiga, snöig taiga/slätt och savann;
  `/locate structure mechanical:tribe_village`): en rund **palissad** (ca 43 block bred) av stockar med **Sharpened Logs**
  (spetsade stockar) överst och en port med två höga stolpar med stammens banderoll. Inne: ett Longhouse för Elder (stockväggar,
  tak av halm eller hudar), runda hyddor och hudtält runt en stor lägereld med stenar och sittstockar, en flaggstång med
  banderollen, Drying Racks, förråd (Decorated Pots och tunna med mat, hudar och flint), vedtrave och huggkubbe,
  Fletching Table och Bone Pile för jägarna, kompost för samlarna. Marken jämnas ut. 10–12 personer av alla roller, alla
  med lägerelden som hem; stammens humör bestämmer rollerna, träslaget, hudfärgerna och taken. Ingen metall.
- **Stammar ("lag"):** varje läger, jaktlag och by är en egen stam med namn (ett totemdjur, t.ex. Raven Tribe), egen
  **banderoll** (färg och mönster) på en stång i lägret/byn, och Elder respektive jaktlagets ledare bär banderollen på huvudet.
  - **Rykte per spelare** (-100 till 100, sparas på spelaren): slå en medlem -8, döda en → fientlig för alltid, bryt
    lägerelden -15, öppna/bryt deras kistor, tunnor eller krukor medan någon ser dig -10 ("Thief!"). Byteshandel +2 per byte,
    **gåvor** (högerklicka med mat; till en Elder smygande) +2. Ryktet visas i action bar när det ändras.
  - **Alla i stammen minns**, även de du aldrig träffat: vid -40 eller lägre (Hostile) anfaller de direkt. Vid 30+ (Friendly)
    hälsar de som en vän och **Elder säljer billigare** (upp till halva priset), under 0 dyrare.
  - **Humör:** *Peaceful* (samlare och fiskare, börjar vänliga), *Wary* (många jägare, handlar bara vid rykte 10+),
    *Raiders* (nästan bara jägare och vedhuggare, börjar ovänliga, handlar först vid 30, **försvarar sitt land**: kommer du
    nära utan Friendly-rykte varnar de och anfaller om du stannar; dödskallar vid porten), *Traders* (börjar vänliga, 9
    byten i stället för 6, större rabatt, två Elders och extra förråd).
- **Sharpened Oak/Spruce Log:** stock + flint (formlöst, building). Spetsig topp till palissader; faller man på den gör
  fallet dubbel skada.
- **Vardag:** Hunters jagar vilda djur (inte björnar, tama djur eller bydjur) och styckar kroppen (en Bone Pile blir kvar).
  Gatherers plockar mogna bär och svamp (bara i större svampfält). Fishers fiskar vid stranden. Woodcutters **fäller riktiga
  träd** nära lägret (trädet faller och blir Fallen Logs som vanligt), hugger upp Fallen Logs och bär veden till elden. Elder håller elden vid liv.
- **Natt:** alla samlas runt lägerelden men går aldrig in i den. Jaktlaget håller ihop där det är.
- **Ny lägereld:** försvinner lägrets eld samlas gruppen efter ca 2 minuter, väljer en plan, torr plats nära den gamla
  (helst samma), går dit tillsammans och gör en ny Campfire med en ring av Loose Stones; den blir deras nya hem. Ett
  jaktlag som strövat en hel dag (och inte är mitt i en jakt) slår sig ner på samma sätt.
- **Försvar:** bara **beväpnade** (Hunters, och den som håller yxa, svärd, spjut, båge e.d.) anfaller monster och hjälper
  andra i gruppen. Obeväpnade (fiskare med spö, samlare, Elder) **flyr** från monster mot elden eller en beväpnad, och slår
  bara tillbaka om de är instängda. Alla backar från en fräsande creeper; jägare med båge skjuter creepers på håll.
  Monster jagar dem också, som zombier jagar bybor (även creepers; inte endermen eller piglins).
- **Sår:** de blöder som djur (blod när de träffas, droppar under halv hälsa) och **pilar fastnar** i dem. Pilsår blöder
  dem ner till ett hjärta men aldrig ihjäl; utan strid läker de sakta och drar ut en pil i taget.
- **Humör:** de hälsar ibland i chatten. **Slår du en blir hela gruppen arg i ungefär en speldag. Dödar du en är gruppen
  fientlig för alltid** (och hela stammen, se Stammar). **Bryter du lägrets Campfire blir de också arga** (en speldag). Verktyget faller med 10 % chans när en spelare dödar dem.
- **Byteshandel (Elder):** 6 av 12 byten, t.ex. 6 Raw Hide → Bone Needle, 10 Flint → Flint Knife, 12 Raspberries → 4
  Cooked Game Meat Scraps, 6 Cordage → 8 Bone Arrows, 12 Bark → 4 Twine, 3 Scraped Hide → 2 Leather, 4 Sinew + 2 Cordage →
  Crude Bow, 3 Tusk + 2 Cordage → Tusk Dagger, 2 Antler → 3 Bone Fish Hooks. Fylls på varje morgon. Ingen handel när de är arga.

### Lägerålder och räder
Ju längre du håller eld vid liv på samma plats, desto mer syns lägret, och till slut kommer fientliga stammar på natten.
- **Lägerålder ("hearth time"):** ett läger är området inom **24 block** från platsen där dess första eld brann. All tid som
  en **brinnande Campfire eller tänd Kiln** står inom området räknas till samma läger (tiden räknas, inte antalet eldar: två
  eldar går inte dubbelt så fort). En ny eld bredvid den gamla fortsätter alltså räkningen, och en eld långt bort startar ett
  nytt läger. Bara medan elden brinner (inte glöd) och bara i laddade chunks (där du är). En stams egen lägereld räknas inte.
  Sparas i världen.
- **Se den:** **smyg och högerklicka en Campfire eller Kiln med tom hand**: "This camp has burned for 3 days 4 h" i action bar.
  Från 5 dagar står det också "Its smoke is seen far and wide..." (då kan räder komma). Op: `/camp` visar lägret där du står.
- **Räder:** när ett läger har brunnit i **5 speldagar** (120 000 ticks) kan det plundras. Vid skymningen (dagtid 12500–13500)
  slås det en gång per natt för varje sådant läger med en spelare inom 48 block. Högst en räd per natt, **aldrig en blodmånenatt**.
  - **Vilken stam:** den sämst inställda stam som spelarna vid lägret har rykte hos eller som bor inom 160 block. Friendly
    (30+) kommer aldrig, neutrala bara om de är *Wary* eller *Raiders*, och *Peaceful*/*Traders* bara om ryktet är under 0.
    Finns ingen sådan stam kan ett **vandrande band** (Wandering Band, en egen Raiders-stam) komma, men sällan.
  - **Chans per natt:** bas 5 % + 2 % per dag över 5 dagar (högst 25 %). Hostile (-40 eller lägre) ×3, ovänlig (under 0)
    ×1,5, neutral ×0,75; *Raiders*-stammar dessutom ×1,5; högst 70 %. Vandrande band: bas ×0,3. Exempel: ett 5 dagar gammalt
    läger och en fientlig Raiders-stam ger 22 %, 15 dagar 70 %. Bara band: 1,5 % → 7,5 %.
  - **Varning:** "You hear drums in the dark, from the north..." och **krigstrummor** från det hållet i 1–3 minuter. Sedan en
    stridslur och "War cries! Raiders of the Raven Tribe come out of the dark from the north, with torches!".
  - **Rädarna:** 3–7 (fler mot fientliga stammar och äldre läger, +1 per 10 dagar; band 3–4), 40 block bort. En ledare (Hunter
    med stammens banderoll på huvudet), resten Hunters och Woodcutters. **Facklor** i andra handen (lyser), Flint Spear (var
    fjärde Crude Bow) eller Flint Axe, 24 hälsa. Hunters och ledaren anfaller spelare och lägrets tama djur och bondgårdsdjur
    (inte vilt). Woodcutters (tjuvar och mordbrännare) slåss bara om du kommer inom 6 block eller slår dem.
  - **Vad de gör:** går till lägret och **stjäl mat** ur kistor, tunnor m.m. inom 16 block (upp till 2 stackar var; "Aruk is
    carrying off your food!") och springer sedan hem med den. Dödar du tjuven får du tillbaka maten. De **tänder eld** på trä
    och annat brännbart (inte löv) inom 12 block: 1 eld var, ledaren 2, och bara med gamerule `mob_griefing` på. De **slår
    sönder trädörrar** (långsamt) men kan inte öppna grindar (fence gates).
  - **Reträtt:** vid gryningen, när hälften av dem har fallit eller efter ca 12 minuter. Långt från lägret och utom synhåll är
    de borta (med det de stal).
  - **Försvar:** eld, vapen och palissad. Den som pressar sig mot en palissad med **Sharpened Logs** (inom 3 block upp) tar
    skada och blir långsam. Faller hälften kommer "The raiders flee!" och **+5 rykte** hos stammen (de respekterar dig). Att slå
    eller döda rädare sänker inte ryktet. Fallna rädare tappar vapen och facklor (50 %) och ledaren sin banderoll (alltid).
  - Op: `/raid` startar en räd mot lägret där du står (trummor, sedan rädare efter 5 s).

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
- **Fälten är döende:** 75 % av grödorna är borta, resten är mest unga (ger ingenting); 15 % av vetet och 25 % av morötterna/potatisen som är kvar är mogna. Jorden är torr.
- **Iron Golem** släpper inget järn (bara 0–2 Poppy).
- **Block från senare åldrar är låsta:** Furnace, Smoker, Blast Furnace, Anvil, Smithing Table, Stonecutter, Grindstone
  och Brewing Stand går inte att använda ("You don't know how to use this yet"). Bryts de blir det bara Rocks eller
  Sticks (Anvil inget). Cauldron, Bell och Lantern går att använda men släpper inget när de bryts (inget järn/guld).
  Listan är taggen `mechanical:too_advanced`, så senare åldrar kan låsa upp dem.
- **Sängarna går inte att sova i** (sängar kan inte tillverkas i stenåldern). Bryts de ger de bara 1–2 Sticks (ramen); tyget är mal-ätet och ger ingen ull.
- **Abandoned Camp** (vanilla): tältens **White Wool** ger 1–3 Wool Thread när den bryts (gammalt tyg rivs upp). Det gäller all
  White Wool, även egen. Campens **Straw Beds** ger 2–4 Plant Fibre (inte sängen).

### Straw Bed (vila)
Stenålderns säng. Man kan inte sova bort natten, men man kan **vila**: högerklicka på en Straw Bed så sätts din
respawnpunkt där och tiden sedan du vilade nollställs, så **phantoms** inte kommer (de kommer annars efter 3 nätter utan vila).
Natten fortsätter som vanligt. Vilan ger också tillbaka sanity (se Mörkerrädsla).
- Recept (crafting table): `[Straw][Straw][Straw]` / `[Planks][Planks][Planks]` → 1 Straw Bed. Straw får man när man skördar säd med Sickle.
- Bryts den ger den 2–4 Plant Fibre, inte sängen (samma för de gamla i Abandoned Camps).
- **Furred Straw Bed (fäll):** högerklicka på en Straw Bed med en **Wolf Pelt** i handen så breds pälsen ut över sängen
  (pälsen går åt; vargens huvud ligger på kudden och svansen hänger över fotänden). Vilan där ger **+35 sanity** i stället för
  +20, **läker 2 hjärtan** och håller dig **varm i 5 minuter** (+30 % isolering mot kyla). Respawn och phantoms som en vanlig
  Straw Bed. Bryts den ger den 2–4 Plant Fibre + Wolf Pelt tillbaka. Har inget item (görs bara på plats).
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

### Odling
Hela systemet (siffror, vanilla-mappning, bronsålderns del) står i `docs/farming.md`.

- **Säd = utsäde.** Gräs och ormbunkar släpper inga frön. Första utsädet hittar man vilt: **Wild Einkorn** (slätt,
  sunflower plains, meadow, savann) och **Wild Emmer** (slätt, sparsamt i skog) i små sällsynta bestånd: med kniv eller
  Flint Sickle 1 Grain, för hand 50 %. **Wild Pea** (skog, blomsterskog, björkskog, meadow) ger 1–2 **Peas** (mat och
  utsäde). Byarnas kistor och bonden har **Wheat Grain** (vanillas Wheat Seeds heter så nu).
- **Så:** Flint Hoe gör farmland (Podzol blir extra bördig), Grain/Peas sås på den. Grödor: Wheat, **Einkorn**, **Emmer**,
  **Barley** (bara creative än), **Peas**, Carrot, Potato, Beetroot, Melon, Pumpkin.
- **Skörd:** omogna grödor ger **ingenting** (odlat Einkorn/Emmer 0–1 Grain). Mogen säd ger **1 Sheaf** (vanillas Wheat
  heter nu **Wheat Sheaf**); med **Flint Sickle** +50 % chans på en till och 1 **Straw**. Carrot/Potato 1–2, Beetroot
  1 + 0–1 frö, Peas 1–3. Melonen ger 0–1 frö, en pumpa → 1 frö.
- **Tröska:** **Flail** i handen, kärvar i andra handen, högerklicka på sten/kullersten/Stone Anvil: var tredje slag i
  snitt → 1–3 Grain + 1 Straw. Utan flail: kärven i craftingrutan → 1 Grain.
- **Mat:** 2 Grain + Clay Water Bucket + Bowl → **Raw Porridge**, kokas på Campfire (20 s) → **Porridge** (5 hunger).
  **Bröd finns inte i stenåldern:** 3-Wheat-receptet är borttaget, bröd bakas av Flour/Dough i bronsåldern.
- **Växtvillkor:** varje gröda har ett temperaturspann (t.ex. vete 7–17, korn 5–16, morot 4–15, melon 12–25; slätt
  ~12, taiga 8, snö 2–6, savann/öken 22). Växer bara **på dagen** under öppen himmel (facklor räcker inte) och på fuktig
  farmland. **Frost** på natten dödar unga plantor under bar himmel (vete under 2, korn under 0, morot under −1) → de blir
  **Withered Crop** (Straw eller Plant Fibre). **Torka** (fukt 0) stoppar tillväxten och vissnar unga plantor; i värme
  torkar jorden fortare. **Rotröta:** rotfrukter på dyblöt jord i träsk eller regn kan ruttna.
- **Vattna:** högerklicka farmland med **Clay Water Bucket** → den och de 8 runt om blir blöta (hinken töms).
- **Bördighet 0–3:** ny åker 2. Varje skörd tar 1 (ärtor ger 1 tillbaka). 0 = halv fart och halv skörd, 3 = snabbare
  och ibland extra. Lämnas jorden i **träda** (tom) återhämtar den sig ~1 per dag.
- **Kråkor:** Hooded Crow-flockar landar på åkrar med unga grödor och äter dem steg för steg. Skrämma bort dem eller
  vakta; Scarecrow kommer i bronsåldern.
- **Smyg + högerklicka med tom hand** på farmland/en gröda: bördighet, fukt, temperatur och varför den inte växer.

| Item | Recept | Användning |
|---|---|---|
| Flint Sickle | Tool Station: Flint + Twine + Stick | Skörd: extra kärve + Straw, vild säd alltid. 48 i hållbarhet |
| Flail | `[ ][Stick]` / `[Twine][Stick]` / `[Stick][ ]` | Tröska kärvar. 64 i hållbarhet |
| Straw | Tröskning, skära | Kompost (bronsåldern) |
| Raw Porridge → Porridge | 2 Grain + Clay Water Bucket + Bowl, sedan Campfire | Mat |

### Rädsla och jakt
- Vilda djur håller sig undan spelare. De märker dig på **16 block om du springer, 10 om du går, 4 om du smyger**
  (längre än 4 block bara med fri sikt). Smyg för att komma nära, en Crude Bow rekommenderas.
- **Skadade djur får panik** i 10 s och springer fort bort från spelaren. De byter väg om spelaren springer runt
  dem eller om vägen leder tillbaka mot spelaren, och springer rakt bort om de inte hittar någon väg.
- **Blödande djur slutar slåss**: även Wild Boar, Aurochs, Moose och hjortar flyr när de blöder (björnen slåss alltid).
- **Panik smittar:** djur som ser ett djur i panik eller ett blödande djur får också panik.
- **Bomskott skrämmer:** en pil som missar och slår ner i marken eller ett träd ger vilda djur inom 12 block panik
  (bort från skytten om han är inom 32 block, annars bort från nedslaget) och får fågelflockar att lyfta. Djur som
  redan anfaller något bryr sig inte. En träffad eller skjuten fågel får hela flocken att lyfta.
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
| Rått kött eller Raw Cod/Salmon | Smoking Rack |
| Wool Thread | White Wool (från tråd) |
| Stone Axe Head | Double Stone Axe Head |
| Twine | Flail |
| Sheaf (valfri) | Tröska för hand (Sheaf → Grain) |
| Grain (valfri) | Raw Porridge |
| Cordage | Log Raft |
| Rough Log (valfri) | Paddle |

| Red/Yellow Ochre, Chalk, Soot eller pigment | Malning, Paint (4), Paintbrush, Pigment → Dye |

Verktygen i Tool Station låses upp separat (se Tool Station).

### Advancements (framsteg)
Vanillas flikar (Minecraft, Nether, The End, Adventure, Husbandry) är dolda. I stället finns fyra egna flikar som visar vägen
framåt. Varje framsteg har föremålet som ikon och en beskrivning som säger hur man gör det, och föräldern är steget som
behövs först, så trädet läses som progressionen:

- **Stone Age**: lösa stenar och flinta → fiber, Twine, flintverktyg → Tool Station → stenverktyg; eld (Fire Drill,
  Campfire) → Clay Kiln → Brick, Charcoal, Clay Bucket, Crucible; flotte, paddel och kanot; pigment och grottmålning; upptrampad stig (Beaten Path).
- **Wilderness & Hunting**: kniv → styckning → hudar, Drying Rack, Leather, Armor Station och rustning; Travois, Hanging
  Rack, snaror, fiske, spjut, bågar och pilar, Tallow, Tusk Dagger, björn, varg, Phantom och Glider Cloak, Ancient Oak och
  stambyar.
- **Food & Farming**: bär och svamp → lagad, rökt och torkad mat, förskämning; vild säd → skära, sådd, skörd, tröskning,
  Straw Bed, gröt, Manure, Compost, Scarecrow, mjöl, deg och bröd.
- **Bronze Age**: första kopparfyndet → Raw Copper, vaskning, Stone Anvil, koncentrat, Tuyere, Bellows, formar, smält
  koppar, gjutning, Whetstone, kopparverktyg → tenn → brons → bronsverktyg, vapen, rustning, såg, drejskiva, krukor, vagn
  och häst, och Raw Iron mot järnåldern.

Genereras av `scripts/gen_advancements.py` (tabellen `TABS`; körs efter gen_unlocks.py, före gen_registry.py). Vanillas
framsteg skrivs över med `neoforge:never` (bara `recipes/**` behålls). **Varje ny sak i moddet ska få sina rader i
tabellen**, så att man lär sig hur den görs.

## Borttaget och ersatt (vanilla)

| Vanilla | Status |
|---|---|
| Wooden Pickaxe/Axe/Shovel/Hoe/Sword/Spear | **Borttagna** |
| Stone Pickaxe, Stone Axe | Receptet borttaget → görs i **Tool Station** |
| Stone Shovel, Stone Hoe, Stone Sword | **Borttagna** |
| Copper-, Iron-, Diamond-verktyg (inkl. spears) | **Borttagna** (kommer tillbaka i senare åldrar) |
| Stone Spear | **Borttagen** → **Flint Spear** i Tool Station |
| Iron-, Golden-, Diamond-, Copper-armor, Turtle Helmet | **Borttagna** (Armor Station kommer senare). Leather Armor finns kvar |
| Alla Beds (alla färger, omfärgning) | **Borttagna** (kommer senare, kräver Shears). Straw Bed finns, se "Straw Bed (vila)" |
| Bow, Fishing Rod | Recepten **borttagna** → **Crude Bow / Crude Fishing Rod** i Tool Station |
| Fisk-loot (döda fiskar, fiske) | Rå fisk → **Whole fish** som måste rensas |
| Farm-djur i naturen | **Borttagna** → spawnar en gång i byar (hage, 4 djur) |
| Byarnas kistor, fält, handel, Iron Golem-järn | **Primitiv loot, döende fält, stenåldershandel, inget järn** |
| Knockback på monster | **Borttaget** |
| Djur-loot | Ersatt av **Carcass** + styckning (ingen Leather/kött/ull direkt) |
| Furnace | **Borttagen** (ersätts av Clay Kiln, uppgraderas senare) |
| Smoker, Blast Furnace | **Borttagna** (senare åldrar) |
| Stone, Granite, Diorite, Andesite | Släpper **2–4 rocks** i stället för cobblestone (Silk Touch ger blocket). 4 Rock → 1 Cobblestone, 4 Granite/Diorite/Andesite Rock → 1 Granite/Diorite/Andesite |
| Arrow-recept | Flyttat till **Fletching Table** |
| Candle-recept | Honeycomb + String → **Tallow + Twine** |
| Fletching Table | Har ett GUI för pilar och pilbågsuppgraderingar |
| Planks | 1 Log → **2** Planks (vanilla 4). Sågen kommer senare |
| Campfire-recept | Logs + Coal → **Firewood + Sticks** |
| Campfire placering | Placeras **släckt** |
| Campfire drop | Charcoal → **1–3 Stick** |
| Log drops | Log → **2 Firewood** (en enskild log). Fällda träd → **Fallen Logs** som måste huggas |
| Löv från fällda träd | Blir **Leaf Litter** på marken |
| Wheat Seeds från gräs | **Borttaget** (vild säd, byar) |
| Hay Bale | Ger inte sig själv (= 9 Wheat) när den bryts: gammalt, ruttet hö → 1–3 Plant Fibre, 25 % chans på 1 Wheat Sheaf |
| Wheat / Wheat Seeds | Heter **Wheat Sheaf** / **Wheat Grain**. Mogen vete ger 1 kärve, omogen ingenting |
| Bread-recept (3 Wheat) | **Borttaget** → Flour/Dough (bronsåldern) |
| Cookie, Cake | Flour i stället för Wheat |
| Grödors drops | Omogna ger ingenting; Carrot/Potato 1–2; stjälkar inget; melon 0–1 frö |
| Melon Seeds-recept | **Borttaget**; Pumpkin Seeds 1 per pumpa (vanilla 4) |
| Grödors tillväxt | Temperatur, dagsljus, fukt, bördighet, frost; kråkor äter unga grödor |
| Facklor | Skyddar inte mot monster, kan slås sönder |
| Logs | Kräver yxa |
| Iron Ore, Coal Ore, Lapis Ore (+ deepslate, Block of Raw Iron, Lapis Block) | Ger inget med trä-/stenhacka. Kräver Bronze Pickaxe (bronsåldern). Stenhackan bryter bara sten, Copper Ore och Tin Ore |
| Leaves | Stick-chans höjd |
| Boats, Chest Boats, Bamboo Raft | Recepten **avstängda** → **Log Raft** och **Dugout Canoe** (se "Flotte och kanot") |

Netherite-verktyg går inte heller att nå, eftersom diamantverktygen är borttagna.

### Recept från senare åldrar (avstängda)
Vanilla-recept som går att göra av saker man hittar i stenåldern (creepers, häxor, fiske, strukturer) men som hör till
en senare ålder. Avstängda i `scripts/gen_age_recipes.py` (tabellen där säger också varför); de kommer tillbaka i sin ålder.

| Recept | Varför det gick | Tillbaka i |
|---|---|---|
| TNT | Gunpowder (creeper) + Sand | Ångåldern |
| Firework Rocket (båda recepten), Firework Star, Firework Star (fade) | Gunpowder + Paper / Dye | Ångåldern |
| Leather Horse Armor | 7 Leather | Bronsåldern |
| Wolf Armor | Armadillo Scute (bältdjur fäller dem själva) | Bronsåldern |
| Spectral Arrow | Glowstone Dust (häxor) + Arrow | Järnåldern |
| Conduit | Nautilus Shell (fiske) + Heart of the Sea (begravd skatt) | Elåldern |
| Respawn Anchor | Crying Obsidian (Ruined Portal-kistor) + Glowstone | Järnåldern |
| Wind Charge, Mace | Breeze Rod och Heavy Core (Trial Chambers) | Järnåldern |
| Cartography Table | Paper + Planks (kartor och kompass kommer senare) | Järnåldern |
| Fermented Spider Eye | Används bara till bryggning | Järnåldern |
| Copper Ingot från Block of Copper (även vaxad) | Kopparblock i Trial Chambers bryts med stenhacka → 9 tackor var | Bronsåldern |
| Bundle (alla färger) | Leather + String; Leather Satchel är stenålderns väska | Järnåldern |
| Paper, Book, Writable Book, Bookshelf, Chiseled Bookshelf, Lectern | Skrift kommer med papyrus | Bronsåldern |
| Concrete Powder (alla färger) | Betong hör till en senare ålder | Järnåldern |
| Lever | En redstone-del | Järnåldern |
| Alla Boat och Chest Boat (10 träslag), Bamboo Raft, Bamboo Chest Raft | Planks; ersatta av Log Raft och Dugout Canoe | Bronsåldern |

Ändrat: **Lead** görs av 5 Cordage (i stället för String). **Redstone-tråd** i tempel, Trial Chambers och herrgårdar ger
ingenting. Loot-filtret tar också bort **Saddle**, **Fire Charge**, vanillas sten- och träverktyg (de görs i Tool Station)
och Weighted Pressure Plates (guld/järn) ur all struktur-, fiske- och mob-loot.

## Öppna frågor / att göra

- **Tin Ore / Tin Ingot / Block of Tin** finns sedan tidigare. Tin Ingot kan inte smältas fram nu (Furnace borttagen) och
  hör till bronsåldern.
- Såg för planks.
- Flera texturer är tillfälliga (Fire Drill, Crucible, stenhuvuden, Firewood, Crude-verktyg, Clay Kiln-sot, meat scraps,
  hides, Wool Thread, Flint Spear, Tool Station, Drying Rack).
- Carcass ritas med djurets standardutseende (vita får, standardkor osv.).
- Upphängd eller buren Carcass ritas med stela ben (benen pekar ut från kroppen i stället för att hänga ner längs den), och hängande kroppar längre än ~1,6 block (älg) når ner i marken.
- Kistor och byinvånare kan fortfarande ge järn/järnverktyg.
- Odlingens bronsåldersdel (Quern, bröd, Compost Heap, Manure, Scarecrow) saknar recept tills bronsåldern fas 3 (`docs/farming.md`).

## Teknik (för utveckling)

- Bär och svamp: `Foraging.java` (registrering), `WildBerryBushBlock.java`, `WildMushroomBlock.java`, björnens `wildlife/ForageBerriesGoal.java`; worldgen i `gen_data.py`.
- Stationer med receptlista (Fletching Table, Armor Station): `StationRecipe.java`, `RecipeStationMenu.java`, `client/RecipeStationScreen.java`; recept i `FletchingRecipes.java`, `ArmorStationRecipes.java`. Pilar: `SpecialArrowItem.java`. Benkrok: `BoneHookRodItem.java`. Bear Rug och Wolf Rug: `BearRugBlock.java`. Wolf Pelt-saker: `client/WolfHoodModel.java` (huvans modell), `StrawBedRest.java` (fäll), data `scripts/gen_wolf_pelt.py` (körs efter gen_biomes.py, före gen_block_items.py), texturer `scripts/gen_wolf_pelt_textures.py`.
- Advancements: `scripts/gen_advancements.py` (alla flikar och texter; vanillas flikar döljs). Ordning: ... gen_block_items → gen_unlocks → gen_advancements → gen_registry. Tämjd häst ger `minecraft:tame_animal` (`WildHorse.tame`).
- Register: `docs/registry.html` genereras av `scripts/gen_registry.py` (+ `docs/registry_notes.json`), följer med varje release och laddas ned av `update-mechanical.cmd`.
- Verktygstips: `client/StatTooltips.java`.
- Odling: se Teknik i `docs/farming.md`.
- Tändmaterial: `Tinder.java` (används av `FireDrillItem.java`).
- Förskämning, rökning, torkning: `Preserving.java` (komponenten `mechanical:spoils_at`, kontroller, taggar), `SmokingRackBlock.java`, `SmokingRackBlockEntity.java`, `DryingRackBlock.java` (hide 6–11), `client/PreservingClient.java` (tooltip, stapel). Data `scripts/gen_preserving.py`, texturer `scripts/gen_preserving_textures.py`.
- Campfire: `Campfires.java` + mixins `CampfireBlockMixin`, `CampfireBlockEntityMixin`. Vilda djur: paketet `wildlife/` (entities), `client/wildlife/` (modeller).
- Flotte och kanot: `Watercraft.java` (register, fartbroms, knivritsning), `DugoutBlankBlock.java`, `client/WatercraftModel.java`, `client/WatercraftRenderer.java`; data `scripts/gen_watercraft.py`, texturer `scripts/gen_watercraft_textures.py`.
- Trädfällning: `TreeFelling.java`, `FallingTreeEntity.java` (landning), `TreeChopping.java` (hugga), `FallenLogBlock.java`, `StumpBlock.java`.
- Natten: `NightDangers.java` (eldregister, monster, horder, facklor, extra spawns, smygare, pathfinding, knockback), `client/NightAmbience.java` (ljud). Byar: `VillageAnimals.java`. Fisk: rensningsrecept i `gen_data.py`, Fish Bones i `StoneAgeEvents.java`. Kyla: `Temperature.java`, `client/TemperatureHud.java`. Vind: `Wind.java`, `UnlitTorchBlock.java`, `client/WindClient.java`, mixin `TorchBlockMixin`. Blodmåne: `BloodMoon.java`, `client/BloodMoonClient.java`, mixins `BloodMoonSkyMixin`, `BloodMoonMoonMixin`. Mörkerrädsla: `Sanity.java` (värde, tappa/återhämta, effekter, `/sanity`), `client/SanityClient.java` (ljud, gestalter, inbillat monster, vinjett, öga, gråa färger via `BloodMoonSkyMixin`); data `scripts/gen_sanity.py`, texturer `scripts/gen_sanity_textures.py`. Mörker och dynamiskt ljus: mixins `DarkNightsMixin`, `LightCoordsMixin`, `EntityLightMixin`, `client/DynamicLights.java`.

- Natten: `NightDangers.java` (eldregister, monster, horder, facklor, extra spawns, smygare, pathfinding, knockback), `client/NightAmbience.java` (ljud). Rovdjur: `wildlife/Predator.java` (eldrädsla, sömn på dagen, svält), `GreyWolf.java`, `CaveLion.java`, `Predators.java` (nattankomster, ylljud, `/nightpredators`), `client/wildlife/PredatorModel.java` (animationer), `scripts/gen_predators.py`, `scripts/gen_predator_textures.py`. Byar: `VillageAnimals.java`. Fisk: rensningsrecept i `gen_data.py`, Fish Bones i `StoneAgeEvents.java`. Kyla: `Temperature.java`, `client/TemperatureHud.java`. Vind: `Wind.java`, `UnlitTorchBlock.java`, `client/WindClient.java`, mixin `TorchBlockMixin`. Blodmåne: `BloodMoon.java`, `client/BloodMoonClient.java`, mixins `BloodMoonSkyMixin`, `BloodMoonMoonMixin`. Mörker och dynamiskt ljus: mixins `DarkNightsMixin`, `LightCoordsMixin`, `EntityLightMixin`, `client/DynamicLights.java`.
- Blod: `Blood.java`, `BloodBlock.java`, `client/BloodDropParticle.java`. Rädsla: `AnimalFear.java`.
- Styckning: `Butchery.java` (utbytestabell), `CarcassEntity.java`, `KnifeItem.java`, `BonePileBlock.java`. Hängande ben och flådda texturer: mixins i `mixin/` (benen bara för fyrbenta djur och höns). Texturskript: `scripts/gen_skinned_textures.py`, `scripts/gen_bone_pile_textures.py`. Drying Rack: `DryingRackBlock.java`.
- Bära, släpa, Travois, Hanging Rack: `CarcassHauling.java` (registrering, händelser, fart), `TravoisEntity.java`, `HangingRackBlock.java`, `client/TravoisModel.java`, `client/TravoisRenderer.java`, poser i `client/CarcassRenderer.java`, armarna och dolda föremål: `client/HaulingClient.java` + `mixin/HumanoidModelMixin.java`, blodspår: `CarcassHauling#bleed`, ingen sprint: `mixin/PlayerMixin.java`, rovdjurens lukt: `Predator.CarcassScentGoal`. Data: `scripts/gen_carcass_hauling.py`, texturer: `scripts/gen_carcass_hauling_textures.py`.
- Tool Station-recept: `ToolStationRecipes.java`. Clay Kiln-recept och bränsle: `KilnBlockEntity.java`. Båda är
  skrivna i Java (inte datapack).
- Övriga recept, loot tables, modeller och taggar genereras av `scripts/gen_data.py` (kör `python scripts/gen_data.py src/main/resources`). Texturskript: `scripts/gen_firewood.py`, `scripts/gen_textures.py`.
- Moddet laddas **efter NeoForge** (`neoforge.mods.toml`), annars skriver NeoForges kopior av vanilla-recept över våra.
