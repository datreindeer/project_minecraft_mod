# Moddens biomer

Alla biomer som modden lägger till i Overworld. Terrängen formas som i vanilla; modden ändrar bara vilken biom som
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

## Mjuk mark: slabs och trappor på 1-blockssteg (terrängplanen fas 2)

Varje naturligt steg på exakt 1 block får ett halvsteg (`worldgen/SmoothGround`, en feature per chunk i alla
överjordsbiomer, sist i `top_layer_modification`, efter stigarna och snön):

- **Slab:** den lägre rutan får en nedre slab av den högre grannens mark: Grass Slab, Dirt Slab, Coarse Dirt Slab, Podzol
  Slab, Sand Slab, Red Sand Slab, Gravel Slab, Moss Slab, Mud Slab, Peat Slab, Loess Slab, Wood Ash Slab, och för sten
  stenfamiljens slab (Stone Slab, Granite Slab, Chalk Slab, Cobbled Slate Slab och så vidare). Gäller även hörn där bara
  den diagonala grannen är högre. Gräset under en nedre slab eller trappa förblir gräs (`mixin/SpreadingSnowyBlockMixin`),
  så sluttningens sidor behåller gräskanten.
- **Trappor:** där den högre marken är berg och kanten är rak (rutorna på båda sidor längs kanten ligger på samma höjd)
  blir det en trappa av bergarten mot steget, med inre och yttre hörnformer där två kanter möts. Formen räknas fram ur
  höjderna, så den stämmer över chunkgränser.
- **Snö:** där snön ligger kvar året runt (`Thaw.coldAllYear`, den smälter inte ens vid midsommar) eller den högre marken
  är snö läggs snöskikt i stället: 5 lager på ett rakt steg (ett halvt block att stå på, så djur går upp), 3 under ett
  hörn och 6 i en vrå. Säsongssnö får en snöig Grass Slab, som finns kvar efter tövädret.
- **Steg på 2 block eller mer** lämnas som klippor.
- **Aldrig** under vatten, på eller bredvid en stig (stigarna har egna trappor), inuti en strukturs ruta (plus 2 block),
  under ett träd, en hög växt eller något block som inte är en liten växt. Små växter (gräs, blommor) ersätts av slaben.
- **Gamla chunks:** `Retrogen` lägger halvstegen i redan genererade chunks som ingen har rört (som för övriga features),
  men aldrig nära ett läger (CampAge), en stams Fire Pit eller en stamväg.

Djur och folk går upp för backarna utan att hoppa (gametest `smooth_ground_walk_up`: en ko på terrasser).

**Mätt** (seed 20261123, 80 × 80 block per plats, före = samma seed med steget avstängt via `MECHANICAL_NOSMOOTH`):

| Plats | 1-blockssteg | med halvsteg före | med halvsteg efter | vanligast |
|---|---|---|---|---|
| Slätt (Plains, delvis snöslätt) | 2 445 | 0 | 2 124 (87 %) | Grass Slab 1 159, snö ×5 595 |
| Snöiga sluttningar | 1 936 | 0 | 1 795 (93 %) | snö ×5 1 117, snö ×6 613, Stone Stairs 47 |
| Kullar (Windswept Hills) | 2 911 | 0 | 2 706 (93 %) | snö ×5 979, Grass Slab 904, Stone Stairs 192 |
| Berg (Jagged Peaks-kanten) | 3 427 | 0 | 2 817 (82 %) | Grass Slab 1 428, snö ×5 784 |
| Meadow | 1 472 | 0 | 1 331 (90 %) | Grass Slab 1 306 |
| Strand | 380 | 0 | 330 (87 %) | Grass Slab 276, Sand Slab 52 |

Klippkolumnerna (steg på 2+ block) är lika många före och efter. Resten av stegen saknar halvsteg för att något står
på den lägre rutan (träd, höga växter, blommor som inte fick plats högre upp) eller för att marken där inte har någon slab
(Terracotta, Clay, Mycelium).

Bilder: `docs/shots/terrain/smooth_*_before.png` och `smooth_*_after.png` (gräskulle, Meadow, stenbrant med trappor,
snöig sluttning, strand).

Små växter på rutan där ett halvsteg läggs flyttas upp till en högre granne om det finns plats; blommor som inte får plats
står kvar (och rutan behåller sitt steg), grästuvor försvinner. På branta gräsbackar, där varje ruta får en slab, blir det
alltså färre grästuvor.

## Grottbiomer

**Limestone Caverns** (tempererat, mitt inne i landet, mellandjup): golv, tak och väggar av Limestone. Fler grottbiomer
kommer i fas 5 och 8.
