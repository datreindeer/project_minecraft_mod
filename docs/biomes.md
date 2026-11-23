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

## Små saker i skog och vid vatten (gen_nature_decor.py)

Terrängplanens fas 3 (`docs/terrain-plan.md`): enkla saker som gör landskapet levande. Java-features i
`worldgen/ForestDecor` och `worldgen/WaterDecor`, placering (biomer, försök per chunk) i `scripts/gen_nature_decor.py`.
Allt läggs bara på naturlig mark, aldrig i en chunk som en by, Tribe Village eller annan ytstruktur når in i, och snö
lägger sig ovanpå som vanligt. Stockar tar **träslaget från närmaste stående träd** (alla moddens och vanillas), så
bokskogen får bokstockar och tallheden tallstockar. Löv som läggs (fallna kronor) är persistenta. Gamla och nya chunks:
allt utom bäverdammar och källor läggs även i gamla chunks av `Retrogen`.

**Skog** (Forest, Birch Forest, Taiga, Jungle, Swamp, Willow Fen, Pine Heath med flera; tätare i gammelskog: Old Growth
Birch/Pine/Spruce, Dark Forest, Pale Garden, Deciduous Forest):
- **Fallen Log:** 3–9 block lång, ibland halvt nedsjunken (i nivå med marken där den är plan) eller utan bark. Moss Carpet
  och svamp på, ormbunkar, mossa, Porcini och Chanterelle längs sidorna, ibland en stubbe en bit från rotänden.
- **Snag:** dött stående träd av samma slag som skogen runt, en barklös stam (Stripped Log, bark kvar vid foten och i fläckar), 4–8 högt,
  korta grenstumpar och borrhål (Grub Log). Var tredje är bara en stubbe med mossa eller svamp.
- **Root plate:** upprycket träd. Rotkakan står på högkant (Rooted Dirt, Dirt, Coarse Dirt, Hanging Roots under), stammen
  ligger ut från den och bakom är gropen, ibland med vatten och Mud i botten. Ibland ligger kronans löv kvar.
- **Mossy stones:** 2–5 små stenar (Mossy Cobblestone, Stone, Andesite, Tuff), halvt nedsjunkna med Moss Block och Moss
  Carpet på, ormbunkar runt. Även vid åar och på heden (Meadow, Plains, Chalk Downs, Slate Highlands). Komplement till de
  större `mossy_boulder`.

**Vatten:**
- **Log bridge:** ett träd fallet tvärs över en bäck eller smal å (2–6 block vatten mellan två bankar 1–3 block över
  vattnet), vilande på båda bankarna. Ibland rotkakan på ena sidan och kronans löv på den andra. Skog och River.
- **Beaver dam:** Beaver Dam-block, Mud och någon stock i två lager över en bäck med bankar minst 2 block höga. Är vattnet
  ovanför instängt (högst 96 block) stiger gölen ett block; finns en 3x3 öppen vattenyta står en bäverhydda där. En eller
  två bävrar som bor där och lagar dammen (`Beaver.settle`).
- **Driftwood:** 1–3 blekta barklösa stockar (Stripped Birch, Stripped Pale Oak, Stripped Oak, Stripped Spruce) platt på
  stränder och åbankar, högst 3 block från vattnet.
- **Gravel bar:** grusbank av Gravel och lite Sand i grunt flodvatten (högst 2 djupt) intill en bank, mitten precis över
  ytan. River.
- **Reed belt:** Reed i ett långt bälte längs sjö- och bäckstränder i skog och på slätt, där vass inte redan växer (River,
  Swamp och Mangrove har sin egen), några Lily Pad utanför.
- **Hillside spring:** en vattenkälla i en sluttning och en fåra, ett block djup, som trappar ner högst ett block per steg
  med fast mark på båda sidor, tills marken planar ut. Varje ruta är en källa och överflödet faller bara ner i nästa, så
  vattnet sprids aldrig. Mossa och ormbunkar runt källan.

**Hitta fallna stockar (för fas 5, stockar som ruttnar):** en fallen stock är vanliga stockblock (`#minecraft:logs`) med
`axis` x eller z som ligger på `#minecraft:substrate_overworld` (eller på en annan sådan stock, eller över en bäck), med
bara luft, Moss Carpet, svamp eller snö ovanpå. Stockbroar och drivved är samma sak; fallna kronor är persistenta löv.

**Advancements** (Wilderness): *Over the Creek* (gå över en stockbro: stå på en stock med vatten 2–3 block under fötterna)
och *Dam Builders* (står man på en Beaver Dam räknas den nu också, inte bara att ha en i inventariet).

**Gametests:** `log_bridge`, `worldgen_beaver_dam`, `hillside_spring` (`worldgen/NatureDecorTest`).

**Täthet** (seed 20261123, antal per 256 × 256 block kring där biomen hittades, området blandar in grannbiomer):

| Område | Fallen Log | Snag | Root Plate | Mossy Stones | Log Bridge | Beaver Dam | Reed Belt | Driftwood | Gravel Bar | Spring |
|---|---|---|---|---|---|---|---|---|---|---|
| Forest | 28 | 10 | 0 | 7 | 2 | 0 | 11 | 0 | 0 | 0 |
| Deciduous Forest | 145 | 69 | 8 | 51 | 0 | 0 | 7 | 0 | 2 | 4 |
| Birch Forest | 133 | 67 | 8 | 30 | 6 | 3 | 6 | 0 | 3 | 0 |
| Old Growth Spruce Taiga | 82 | 33 | 2 | 17 | 5 | 0 | 5 | 7 | 2 | 0 |
| Taiga | 50 | 12 | 2 | 14 | 4 | 1 | 16 | 1 | 0 | 0 |
| Pine Heath | 21 | 5 | 2 | 15 | 0 | 0 | 2 | 0 | 0 | 1 |
| Swamp | 3 | 2 | 0 | 6 | 0 | 1 | 3 | 7 | 0 | 0 |

Ungefär en fallen stock per två chunks i gammelskog, en per fem i vanlig skog (vanillas egna fallna träd kommer till).
Bäverdammar och stockbroar är sällsynta: de kräver en riktig bäck.

**Mätverktyg:** med en fil `decorlog.flag` i spelmappen loggas varje placerad sak (`decor <sort> at x y z`).

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

## Grottbiomer

**Limestone Caverns** (tempererat, mitt inne i landet, mellandjup): golv, tak och väggar av Limestone. Fler grottbiomer
kommer i fas 5 och 8.
