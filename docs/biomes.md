# Moddens biomer

Alla biomer som modden lägger till i Overworld. Terrängens form är moddens egen sedan terrängplanens fas 1 (se
*Terrängens form* nedan: högre berg, djupa dalar, plattare slätter). Biomerna placeras som förut; modden ändrar bara vilken biom som
hamnar var (varje biom tar en del av en vanilla-biomens klimatzon, se `docs/world-materials-plan.md` "Fas 4+7"),
och i de nya ytbiomerna också vilka block marken består av. Varje biom ärver sin vanilla-malls djur, strukturer och
moddens tillägg (foder, vilt, landmärken) utom det som inte passar. Att hitta en ny ytbiom ger en advancement i
Wilderness-fliken.

## De fyra första (gen_biomes.py)

- **Deciduous Forest** (Lövskog): fuktiga halvan av tempererad skog. Bok, lind, ask, hassel och ek, tjock lövströ,
  svamp. Rådjur, grävling, ekorre. Den stora ihåliga eken (Ancient Oak) står här.
- **Hazel Meadow** (Hasselbacke): fuktiga delen av svala och tempererade slätter. Hasselsnår, enstaka ask och lind,
  ängsblommor. Rådjur, hare, fasan, vildhäst, uroxe.
- **Willow Fen** (Pilkärr): svala halvan av Swamp och den fuktiga delen av den tempererade. Pil och al, ormbunkar,
  vass (sugar cane), lera i dammarna, Bog Iron på dammbottnar. Vildsvin, älg, tranor, gäss.
- **Pine Heath** (Tallhed): torra svala slätter. Glesa tallar, ljung, lingon och blåbär, podzol och sand. Hare,
  kronhjort.

## Världsmaterialens ytbiomer (gen_world_biomes.py)

### Chalk Downs
Kustnära, torra delen av tempererade slätter. Öppna gräskullar på vit **Chalk Block** (krita syns i varje
sluttning), flinta (Loose Flint) och lös krita i gräset. Inga träd. Får, harar, trappar, tranor. Stäppdjuren
(saiga, bison, onager, vildhäst, uroxe) finns inte här.

### Peat Bog
Den våtare halvan av sval taiga, på flack mark, bredvid Willow Fen. Marken är **Peat** under tunt gräs, med fläckar av
öppen torv och mossa, mörka små gölar med torvbotten, tuvor och glesa tallar. Under marken: begravd **Bog Oak**, och
**Bog Iron** i gölarnas botten. Dimmigt, brunt vatten. Älg, tranor, gäss, hjortron.

### Slate Highlands
Svala Windswept Hills (halva). **Slate** i alla sluttningar och hällar, **Gneiss** under den. Ljung, stenrösen
(Cairn), små slatehögar. Mufflon och kungsörn. Kall, grå himmel.

### Soapstone Valley
Liten och sällsynt: en del av tempererad Windswept Forest. Grågrön **Soapstone** ligger i dagen i hällar och
sluttningar, mjuk nog för en Flint Pickaxe. Skog som i Windswept Forest.

### Limestone Karst
Tempererad Meadow (den fuktigare delen). Kalkhällar (alvar) av **Limestone** med tunt gräs direkt på berget, torra
slukhål, ängsblommor (orkidé, blåklint, allium). **Travertine** på botten av gölar och bäckar. Mer Dolomite under.

### Marble Gorge
Varma Savanna Plateau (den fuktigare halvan). Vita klippor av **Marble** med **Limestone** i väggarna, grov jord och
torrt gräs, nästan inga träd. Marble kräver en Bronze Pickaxe. Mufflon, onager.

### Ochre Badlands
En del av Badlands. Randiga klippor av **Red Ochre Block**, **Yellow Ochre Block**, **Gypsum**, **Mudstone** och
terrakotta, röd sand på slätterna, lös ockra på marken. Gamar, ökenhare, ödlor.

### Basalt Coast
Stony Shore i fuktigt klimat. Svart **Coastal Basalt** med **Beach Pebbles** emellan, basaltpelare som står ute i
vattnet. Säl och måsar.

### Shingle Beach
Svala stränder (den torrare delen). **Beach Pebbles** över grus, lösa block av **Fieldstone** och stora flyttblock
(**Glacial Boulder**: Gneiss, Granite, Rapakivi Granite). Säl och måsar.

### Reed Delta
Tempererad Swamp nära kusten. Täta vassbälten (**Reed**), lerbankar av Mud, **Clay** och Mud under vattnet, **Loess**
och **Mudstone** under marken, enstaka pilar. Gräsänder, svanar, tranor, gäss.

### Glacial Moraine
Kall taiga på kuperad mark. Grus och grov jord, **Conglomerate** under, lösa **Fieldstone** och stora flyttblock av
Gneiss, Granite och Rapakivi Granite. Glesa granar. Ren och älg.

### Gypsum Flats
Den torraste delen av öknen, på flack mark. En vit skorpa av **Gypsum** med saltfläckar (**Rock Salt**) och sand,
**Alabaster** i gipsen under. Bländande ljus himmel.

### Mammoth Steppe
Den torraste delen av de frusna snöslätterna (den kallaste temperaturzonen, torraste fuktighetsbandet; Ice Spikes behåller
sin plats). Kall och torr gräsmark, istidens stäpp: varm nog för gräs i stället för snötäcke (0,3, som taigan), gulgrått torrt gräs med fläckar av
grov jord, tjock **Loess** (vindburen jord) under grässvålen och i varje slänt, enstaka flyttblock (**Glacial Boulder**).
Inga träd och inga bär. Hit hör snöslätternas istidsdjur, **Woolly Mammoth**, **Woolly Rhinoceros**, **Steppe Bison**,
**Musk Ox** och **Saiga**, och dessutom **vildhästar**, **grottlejon** och **vargar** som jagar dem. Inga isbjörnar. Mammutbenshyddor
och jaktlag som på snöslätterna, men inga igloor eller snöbyar. Advancement **Mammoth Steppe** i Wilderness. Klimatplatsen:
`ModBiomes.SURFACE`; data: `gen_world_biomes.py` (ärver från `snowy_plains`; `no_creatures` tar bort isbjörnen, `skip_tags`
igloo, snöby och snörävar/vita kaniner). Gametest `steppe_biome`, bild `docs/shots/fauna/steppe_mammoth_steppe.png`.

### Scorched Barrens
Nedbrunnen skog, **bara längre än 2500 block från 0,0** och sällsynt (ungefär 0,2 % av marken där, i samma storleksordning
som Mushroom Fields). Svartbrända stammar av **Charred Log** (stående, fallna och stubbar), mark av **Wood Ash**, Coarse
Dirt och Gravel, hällar av Blackstone och Basalt i sluttningarna, inget gräs. Grå, kall himmel och dimma, aska i luften.
Inga djur utom korpar (Raven); monster på natten som vanligt. Snö och frost på vintern som i andra svala biomer. Här står
de sällsynta **Elkwight-ruinerna** (`docs/elkwight-plan.md`), alltid mitt i biomet. Biomen tar en del av sval Forest
(`ModBiomes.SURFACE`); närmare 0,0 än 2500 block blir den platsen Forest igen (`mixin/MultiNoiseBiomeSourceMixin`).
**Alltid omgiven av levande skog:** en plats blir bara Scorched Barrens om allt inom 24 block är skog (Forest, Birch,
Dark Forest, Taiga, Deciduous Forest med flera) eller barrens, annars blir den Forest (`ScorchedBarrens.stays`). Den rör
alltså aldrig slätter, vatten, berg eller öken.

## Övergångar mellan kallt och varmt

Vanilla ger tempererade berg snöiga sluttningar och toppar (Snowy Slopes, Grove, Frozen Peaks, Jagged Peaks) ända fram
till det varma bandet, så snö kunde ligga direkt bredvid savann, djungel och Marble Gorge. I den varma delen av det
tempererade bandet (temperatur 0,05..0,2) är bergen nu snöfria i stället: Snowy Slopes blir Meadow, Grove blir Taiga och
topparna blir Stony Peaks, som vanilla gör i det varma bandet (`ModBiomes.SURFACE`, raderna med `WARM_EDGE`). Övriga kalla
biomer (snöslätter, snötaiga, Mammoth Steppe, Glacial Moraine) ligger i det frusna bandet och har alltid hela det svala och
det tempererade bandet mellan sig och värmen. Mätt på tre seeds, 16 384 × 16 384 block (16 block per pixel), kalla
pixlar (16 × 16 block) nära en varm, i antal:

| | inom 32 block | inom 64 | inom 128 | inom 256 |
|---|---|---|---|---|
| Förut (seed 1 / 2 / 4242) | 125 / 80 / 64 | 389 / 289 / 246 | 867 / 749 / 649 | 1638 / 1665 / 1429 |
| Nu | 0 / 0 / 0 | 0 / 0 / 0 | 0 / 0 / 0 | 44 / 130 / 32 |

Kalla pixlar totalt: ungefär 47 000–71 000 per karta (6 % färre än förut, nästan bara snöiga berg i det varma
tempererade bandet). Kartorna: `docs/shots/biomes/before_seed*.png` och `after_seed*.png`, kall pixel inom 64 block från
en varm är röd.

**Kartverktyget** (`worldgen/BiomeMap`, bara för utveckling): lägg en fil `biomemap.flag` i spelmappen (`run/`) med rad 1
utmapp, rad 2 seeds, rad 3 kartans sida i block, rad 4 filnamnsprefix, och starta gametest-servern. Den ritar ytbiomerna
(efter moddens regler) runt 0,0 för varje seed, med färgförklaring, plus en närbild av den första Scorched Barrens, och
loggar mätningen ovan och andelen Scorched Barrens bortom 2600 block. Utan filen gör den ingenting.

## Terrängens form (gen_terrain.py)

Terrängplanens fas 1 (`docs/terrain-plan.md`). Modden skriver över vanillas density functions för Overworld:
`data/minecraft/worldgen/density_function/overworld/` `offset.json` (höjden), `factor.json` (hur kuperat),
`jaggedness.json` (taggiga toppar), `final_density.json` och `preliminary_surface_level.json` (taket), plus en egen
`mechanical:flatness`. Allt skrivs av `scripts/gen_terrain.py`, som läser vanillas filer ur jarren och byter ut
punkterna från en tabell (`OFFSET`), så ändringarna står som tal. Klimatbruset är detsamma, så **varje biom ligger kvar
där den låg**: kartorna över kallt och varmt ovan är oförändrade. Kuster (kontinentalitet upp till -0,1) och träsk
(erosion 0,7) har vanillas höjder kvar, och grottor, akviferer och malmhöjder är orörda.

- **Bergskedjor:** långt inne i landet (kontinentalitet 0,25 och 1) ligger bergen högre och oftare. Där erosionen är
  låg reser sig kammarna till y 220–260, och de högsta topparna med sina taggiga spiror når y 280–295 (vanilla stannade
  på 255, där taket plattade till dem). Taket som pressar bort terrängen flyttades från y 240–256 till y 272–296, så det
  finns över 20 block kvar att bygga på även på den högsta toppen. Platåerna (Savanna Plateau, Marble Gorge) ligger
  lite högre, och kullarna i det kuperade låglandet blir bergsryggar som löper genom landskapet.
- **Djupa dalar:** floden ligger alltid i botten av en dal. Låglandet ligger högre än förut (y 75–90 i stället för
  64–67), och flodbankarna stiger brant från vattnet och planar ut en bit upp, så floder i låglandet går i dalar 15–25
  block djupa. Mellan bergen är dalarna 30–60 block och mer, med floden längst ned. Vattnet ligger kvar i havets nivå.
- **Platta biomer:** `mechanical:flatness` (0..1) är 1 där marken ska vara platt: torrt (fuktighet under -0,15:
  Plains, Savanna, Chalk Downs, Pine Heath, Mammoth Steppe), hett (temperatur över 0,6: Desert, Gypsum Flats) och kallt
  och torrt (Snowy Plains, Ice Spikes), och 0,4 på jämn mark i övrigt (Peat Bog). Den tonas bort mot bergen (erosion
  -0,3 .. -0,45), så toppar och bergssidor behåller sin form. Där väger höjden över till en lugn kopia av höjdkurvan,
  där låglandets kullar bara har 12 % kvar av sin höjd och flodbankarna är mjukare, och 3D-bruset dämpas fem gånger
  (`factor`). Resultatet är långa jämna sluttningar i stället för småkullar. Skogar, taiga och ängar är i stället lite
  mer kuperade än förut.

**Mätt** med kartverktyget (nedan) på tre seeds, 6144 × 6144 block runt 0,0, före (vanilla) → efter:

| | seed 1 | seed 2 | seed 4242 |
|---|---|---|---|
| Land över y 150 | 1,6 % → 8,5 % | 2,7 % → 10,1 % | 0,1 % → 3,6 % |
| Land över y 200 | 0,06 % → 1,4 % | 0,20 % → 1,9 % | 0 → 0,21 % |
| Högsta topp | y 255 → 287 | y 255 → 295 | y 181 → 240 |
| Floddalens djup, median / 90:e percentil (alla floder) | 6 / 35 → 12 / 49 | 7 / 31 → 12 / 42 | 7 / 26 → 11 / 39 |
| Floder med mark över y 130 inom 96 block: antal pixlar, djup median | 2393, 38 → 6228, 45 | 3212, 23 → 7656, 30 | 1418, 35 → 8558, 33 |
| Höjdskillnad i rutor om 128 block, median | 53 → 66 | 58 → 74 | 49 → 60 |
| Strukturer: byar / Tribe Village / Ancient Oak / Elkwight-ruin (12 288 × 12 288 block) | 105 / 41 / 87 / 3 → 103 / 39 / 78 / 3 | 104 / 37 / 49 / 1 → 104 / 27 / 46 / 1 | 120 / 34 / 53 / 1 → 118 / 35 / 49 / 2 |

Platt mark (lutning högst 1 block på 4), alla tre seeds: Mammoth Steppe 57 → 69 %, Desert 72 → 80 %, Sunflower Plains
72 → 78 %, Chalk Downs 75 → 79 %, Pine Heath 76 → 79 %, Ice Spikes 68 → 70 %, Savanna 71 → 71 %, Plains 70 → 66 %,
Snowy Plains 65 → 62 %, Peat Bog 73 → 71 %; skogarna 65–70 → 54–61 %, Taiga 63 → 52 %, Meadow 51 → 38 %. Plains och
Snowy Plains ligger ofta i kuperade erosionsband vid bergen och vid flodbankar, där höjdkurvan och inte bruset ger
lutningen, så andelen ändras lite; småkullarna mitt på slätten är borta (höjdprofiler i 1-blockssteg följer nu höjdkurvan
nästan exakt). Strukturerna: Tribe Village och Ancient Oak placeras 10 % och 8 % mer sällan, eftersom skogarna och
dalsidorna har fått mer lutning; byarna och Elkwight-ruinerna lika ofta. På en riktig dev-server (seed 2) nådde toppen
vid 2128, 488 y 295 och den vid -1468, -1576 y 284, och floderna (bland annat i en bergsdal vid 2418, -368) har vatten.

Bilder, före till vänster och efter till höger: `docs/shots/terrain/fas1_relief_seed1.png`, `_seed2`, `_seed4242`
(skuggad relief), `fas1_slope_seed2.png` (lutning: grönt platt, gult, orange, rött, lila brant) och skärmbilder från
seed 2: `fas1_range_air.png`, `fas1_highland_air.png`, `fas1_range_ground.png`, `fas1_valley_air.png`,
`fas1_valley_ground.png`, `fas1_plains_air.png`, `fas1_plains_ground.png`.

**Advancement:** *Roof of the World* (Wilderness, under *White Downs*): klättra upp över y 250.

**Verktygen** (bara för utveckling):
- `BiomeMap` med rad 5 `terrain` i `biomemap.flag` (eller `terrain-only`, utan biomkartor; rad 6 `exit` stänger
  servern efteråt i stället för att köra testerna) kör `worldgen/TerrainMap`: höjden direkt ur chunk-generatorn (inga
  chunks genereras), 8 block per pixel, som skuggad reliefkarta (`<prefix>_seed<seed>_relief.png`), lutningskarta
  (`_slope.png`), statistik (`_terrain.txt`: tabellen ovan, platt mark per biom och per erosionsband, flatness per biom,
  strukturer som klarar sina egna placeringskrav, bra platser att fotografera) och råa höjder (`_heights.bin`).
- `TerrainShots` (klienten): `terrainshots.flag` med rad 1 utmapp, rad 2 seed (plus `fresh` för att skapa världen på
  nytt, `world=Namn` för en annan värld), rad 3 filprefix och sedan en kameraplats per rad:
  `namn kameraX kameraZ målX målZ höjd [lutning]` (höjd över marken, eller `=Y`). Den skapar världen, väntar tills
  terrängen har ritats, tar bilderna mitt på dagen utan moln och HUD och avslutar spelet.

## Grottbiomer

**Limestone Caverns** (tempererat, mitt inne i landet, mellandjup): golv, tak och väggar av Limestone. Fler grottbiomer
kommer i fas 5 och 8.
