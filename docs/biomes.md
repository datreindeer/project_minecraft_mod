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
- **Root plate:** upprycket träd. Rotkakan står på högkant: en ojämn oval, ungefär 5 bred och 4–5 hög, av Rooted Dirt,
  Dirt och Coarse Dirt med några stenar i, tjockare i mitten och med toppen lutad bakåt över stammen. Rotändar (Mangrove
  Roots) sticker ut runt kanten och bakåt, Hanging Roots under. Stammens rotände sitter i mitten av skivan och ligger ett
  block över marken de två första blocken. Bakom är kratern, ungefär 3 × 5 och 1–2 djup, ibland med vatten och Mud i
  botten. Ibland ligger kronans löv kvar.
- **Mossy stones:** 2–5 små stenar (Mossy Cobblestone, Stone, Andesite, Tuff), halvt nedsjunkna med Moss Block och Moss
  Carpet på, ormbunkar runt. Även vid åar och på heden (Meadow, Plains, Chalk Downs, Slate Highlands). Komplement till de
  större `mossy_boulder`.

**Vatten:**
- **Log bridge:** ett träd fallet tvärs över en bäck eller smal å (2–6 block vatten mellan två bankar 1–3 block över
  vattnet), vilande på båda bankarna. Ibland rotkakan på ena sidan och kronans löv på den andra. Skog och River.
- **Beaver dam:** en sammanhängande vall av Beaver Dam-block (kvistar och lera) och lite Mud tvärs över en bäck med bankar
  minst 2 block höga: tre lager, det översta över vattenytan och inkilat i bankarna, mitten utbuktande ett block nedströms,
  någon stock ovanpå. Är vattnet ovanför instängt (högst 96 block) stiger gölen ett block. I gölen en hydda: en låg rundad
  hög 5 bred och 3 hög (3 bred och 2 hög om det är trångt) med en våt kammare i mitten, ofta lutad mot stranden. En eller
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

## Små saker i berg, på öppen mark, i torrt och kallt land (gen_land_decor.py)

Terrängplanens fas 4 (`docs/terrain-plan.md`), fortsättningen på fas 3. Java-features i `worldgen/MountainDecor`,
`worldgen/OpenDecor` och `worldgen/DryColdDecor`, placering i `scripts/gen_land_decor.py`. Samma regler som i fas 3:
bara naturlig mark, aldrig i en chunk som en ytstruktur når in i, och snön lägger sig ovanpå som vanligt (allt läggs i
`vegetal_decoration`, före snön och före `SmoothGround`, som sedan rundar av även talus, hällar och bäckfåror med
halvsteg). Allt utom vattenfall och vårdträd läggs även i gamla chunks av `Retrogen`. Det som redan fanns (flyttblock,
`balanced_rock`, `cairn`, `standing_stones`, `abandoned_campsite`, `juniper`, `termite_mound`) är inte dubblerat; en en
får nu också växa ensam på slätter och ängar (`juniper_in_plains`).

**Två nya block** för benen (`worldgen/OldBonesBlock`, modeller av tunna ben i `gen_land_decor.py`, man går igenom dem):
**Rib Cage** (ett par revben som välver sig över ryggraden, även halvt nedsjunken; ger 1–3 Bone) och **Mammoth Tusk**
(en böjd bete i två delar, roten och den framåtböjda övre delen; roten ger en Tusk).

**Berg** (Stony/Jagged/Frozen Peaks, Snowy Slopes, Grove, Meadow, Windswept Hills/Gravelly Hills/Forest/Savanna,
Savanna Plateau, Slate Highlands, Limestone Karst, Marble Gorge, Soapstone Valley, och dalbottnarnas taiga och skog).
En klippvägg är där marken tar ett steg på minst 2 block och sedan stiger minst 6–10 block inom några block, med en
vägg som till största delen är sten (en jordbank med en sten i räknas inte); allt byggs av väggens egen sten.
- **Talus:** halv kon av Gravel, Cobblestone och väggens sten vid foten, 2–4 block tjock mot väggen, utglesnad till grus
  och lösa stenar i gräset 4–10 block ut.
- **Fallen rock:** stort kantigt block, 3–5 brett och 2–4 högt, hörnen avslagna, nedsjunket ett block och ofta tippat,
  flisor av grus och lösa stenar runt, Moss Carpet på där det inte är kallt.
- **Rock arch:** naturligt valv, resten av en klippvägg som nötts igenom: ett lågt brett spann (5–7 brett och 3–4 högt
  inuti), tunt och 3 djupt vid krönet, fötterna bredare och djupare och övergår i stenknölar, knölig yta (jämnt brus,
  inte enstaka block), grus och lösa stenar vid fötterna. Av stenen under marken; i badlands randig Terracotta (samma
  färg runt om på samma höjd, som i klipporna). Sällsynt (1 på 40 chunks försök, på plan mark).
- **Bird cliff:** lodrät vägg minst 6 hög med små avsatser: Bird Nest (Rock Dove, ibland ägg) eller Guano Pile på varje,
  vita strimmor av Calcite nedför väggen under dem, guano vid foten. Även Stony Shore och Basalt Coast.
- **Valley waterfall:** en eller två vattenkällor i en skåra 3+ block upp i en lodrät klippvägg, vattnet faller rakt ner
  i en göl nedsänkt ett block i marken. Bara där skåran är stängd åt alla håll utom framåt och gölens alla sidor är fast
  mark, så vattnet sprids aldrig (fallande vatten över en källa sprider sig inte åt sidorna). Fallet läggs direkt i
  worldgen, så gölen aldrig fryser till under det. Moss Block runt skåran. Läggs inte i gamla chunks.
- **Snow patch:** snöfält som ligger kvar på sommaren: en oval av Snow Block 4–12 block lång. Från y 150 där snö aldrig
  smälter (`Thaw.coldAllYear`) med en kant av allt tunnare snölager, och från y 200 i övriga berg (y 240 i varma som
  Stony Peaks) utan kant. `Thaw` smälter bara snölager och vanlig is, aldrig Snow Block, så kärnan blir kvar.

**Öppen mark:**
- **Rock outcrop (häll):** berggrund i nivå med grässvålen, en ojämn fläck 5–11 block, ibland en låg puckel, en spricka
  med gräs, fläckar av mossa, kullersten och grus, Coarse Dirt i kanten. Stenen under marken (Granite eller Stone om
  ingen finns nära).
- **Bone remains:** ett djurskelett: ryggrad av Bone Block nedsjunken i marken, Rib Cage över den (något revben saknas),
  en Skeleton Skull vid huvudänden, ett lårben och Bone Pile en bit bort, blommor runt. Slätter, savann, snöslätt,
  Mammoth Steppe.
- **Anthill:** myrstack, en jämn brant kupol av barr över jord 3–5 bred och 1,5–2,5 hög, byggd i halvblock (Podzol och
  Podzol Slab, Coarse Dirt Slab längst ner), Leaf Litter på och runt. Taiga, tallhed och skog, glesare på slätter och
  ängar.
- **Lone tree (vårdträd):** en stor Linden, Ash eller Fancy Oak där inget annat träd står inom 14 block, blommor och en
  sten vid foten. Slätter och ängar.
- **Farm walls:** övergiven gård: låga raserade stenmurar i en grov rektangel 10–16 × 8–13 som följer marken, ett skift
  Fieldstone och Mossy Cobblestone med Dry Stone Wall ovanpå i sträckor, långa partier nedfallna till stenar i gräset,
  utrasade stenar bredvid, en grindöppning, hörnet av en husgrund och brännässlor innanför. Bara Plains och Sunflower
  Plains, sällsynt (1 på 70 chunks försök).
- **Fire ring:** gammal eldstad: stenring nedsjunken i grässvålen runt Wood Ash och en förkolnad stock nedsjunken i askan,
  gräs på väg tillbaka, ibland en stock att sitta på.

**Torrt och varmt** (Desert, Badlands, Gypsum Flats, Ochre Badlands, savann):
- **Dead tree:** torrt träd utan bark, stam 3–6 med rötter som syns i marken, två till fyra kala krokiga grenar av tunt
  trä (staket) som trappar ut och upp, den avbrutna toppen en kvist, Coarse Dirt vid foten, Dead Bush och Short Dry
  Grass runt, ibland en nedfallen gren. Stripped Acacia och Acacia Fence på savannen, blekt Pale Oak eller grå Oak annars.
- **Sand skeleton:** samma skelett halvt begravt i sanden, bara revbenens toppar (Rib Cage nedsjunken) och skallen syns.
  Desert och Gypsum Flats.
- **Rock overhang:** hylla av klippans sten 2–3 ut från en klippvägg, 3 upp, 3–5 bred, en urholkning i väggen under den
  med Coarse Dirt i botten. Badlands, Desert, Savanna Plateau, Windswept Savanna, Ochre Badlands, Marble Gorge.
- **Dry creek:** uttorkad bäckfåra som slingrar sig 14–28 block, 2–3 bred, två block djup i mitten och ett vid kanterna
  (SmoothGround rundar kanterna), botten av Gravel, Packed Mud (sprucken lera) och några kullerstenar, kanterna Coarse
  Dirt och lera, småsten och Dead Bush längs den. Botten trappar högst ett block i taget.

**Kallt:**
- **Ice blocks:** ett isflak av Packed Ice (lite Blue Ice) på högkant som stiger ur marken till 2–3 block, halvt begravt
  i snödrivor, några isbitar runt. Packad is och blåis smälter inte i vårtöet (`Thaw` smälter bara vanlig is). Snowy
  Plains, Ice Spikes, Snowy Beach.
- **Frost split:** ett stort flackt stenblock spräckt mitt itu, halvorna en bit isär, grus i sprickan, flisor och grus
  runt. Snowy Plains, Snowy Taiga, Ice Spikes, Grove, Snowy Slopes, Glacial Moraine, Mammoth Steppe.
- **Mammoth tusks:** två Mammoth Tusk, vardera två block höga, som reser sig ur en fläck bar Loess och böjer sig framåt,
  skallen en halvt begravd benklump mellan rötterna, revbenen (Rib Cage, nedsjunken) i en rad bakom, ibland en bete
  avbruten, Bone Pile runt. Bara Mammoth Steppe.

**Advancements** (Wilderness): *Under the Arch* (stå under ett naturligt valv: sten 3–5 block över huvudet, sten på båda
sidor 2–4 block bort och fri himmel 3 block framför och bakom; under *Roof of the World*) och *Old Bones* (stå på,
i eller bredvid ett Bone Block, Rib Cage eller Mammoth Tusk; under *White Downs*).

**Gametests:** `valley_waterfall` (vattnet stannar i fallet och gölen) och `rock_arch` (valvet står som *Under the Arch*
väntar sig), i `worldgen/NatureDecorTest`.

**Täthet** (seed 20261123, antal per 256 × 256 block kring där biomen hittades; området blandar in grannbiomer, och de
flesta features ger upp där marken inte passar, så siffrorna är långt under försöken):

| Område | Talus | Fallen Rock | Snow Patch | Bird Cliff | Waterfall | Outcrop | Anthill | Övrigt |
|---|---|---|---|---|---|---|---|---|
| Stony Peaks | 34 | 6 | 12 | 1 | 0 | 2 | 2 | |
| Jagged Peaks | 18 | 0 | 0 | 2 | 1 | 6 | 11 | |
| Windswept Hills | 5 | 1 | 0 | 0 | 0 | 7 | 0 | |
| Meadow | 10 | 1 | 0 | 0 | 1 | 1 | 29 | |
| Plains | 15 | 1 | 0 | 0 | 0 | 13 | 2 | Lone Tree 1, Fire Ring 1, Bones 1, Farm Walls 0–1 |
| Taiga | 0 | 0 | 0 | 0 | 0 | 1 | 8 | |
| Savanna | 0 | 0 | 0 | 0 | 0 | 4 | 0 | Dead Tree 6, Dry Creek 4, Bones 2, Lone Tree 1 |
| Savanna Plateau | 25 | 0 | 0 | 1 | 0 | 0 | 0 | Dead Tree 14, Dry Creek 3, Overhang 1 |
| Desert | 0 | 0 | 0 | 0 | 0 | 0 | 0 | Dead Tree 13, Dry Creek 5, Sand Skeleton 1 |
| Badlands | 0 | 0 | 0 | 0 | 0 | 0 | 0 | Dead Tree 26, Dry Creek 7, Rock Arch 3 |
| Snowy Plains | 0 | 0 | 0 | 0 | 0 | 4 | 0 | Ice Blocks 6, Frost Split 1, Bones 2 |
| Mammoth Steppe | 6 | 0 | 0 | 0 | 0 | 10 | 4 | Mammoth Tusks 8, Frost Split 15, Ice Blocks 10, Bones 5 |

Bilder: `docs/shots/terrain/fas4_*.png`. **Mätverktyg:** samma `decorlog.flag` som i fas 3; berg- och klippsakerna
loggar också `<sort>_dir_<vinkel>`, sidan att titta från (för `DecorShots`).

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
  den diagonala grannen är högre. Gräs och podzol under en slab blir Dirt, så varje
  halvsteg visar en gräskant (slabens), inte två.
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

Små växter på rutan där ett halvsteg läggs flyttas till närmaste lediga hela marktopp inom 2 block (de kan inte stå på
en slab). Får de inte plats, eller är de höga (Tall Grass, Sunflower, Rose Bush), tas de bort: en växt som fick stå kvar
lämnade ett hål i sluttningen. Branta gräsbackar där varje ruta får en slab blir därför något kalare.

## Terrängens form (gen_terrain.py)

Terrängplanens fas 1 (`docs/terrain-plan.md`). Modden skriver över vanillas density functions för Overworld:
`data/minecraft/worldgen/density_function/overworld/` `offset.json` (höjden), `factor.json` (hur kuperat),
`jaggedness.json` (taggiga toppar), `final_density.json` och `preliminary_surface_level.json` (taket), plus en egen
`mechanical:flatness`. Allt skrivs av `scripts/gen_terrain.py`, som läser vanillas filer ur jarren och byter ut
punkterna från en tabell (`OFFSET`), så ändringarna står som tal. Klimatbruset är detsamma, så **varje biom ligger kvar
där den låg**: kartorna över kallt och varmt ovan är oförändrade. Kuster (kontinentalitet upp till -0,1) och träsk
(erosion 0,7) har vanillas höjder kvar, och grottor, akviferer och malmhöjder är orörda.

Landskapet bygger på kontrast: lugnt lågland mellan bergskedjorna, och berg och platåer som reser sig brant ur det.

- **Bergskedjor:** långt inne i landet (kontinentalitet 0,25 och 1) ligger bergen högre och oftare. Där erosionen är
  låg reser sig kammarna till y 220–260, och de högsta topparna med sina taggiga spiror når y 280–295 (vanilla stannade
  på 255, där taket plattade till dem). Taket som pressar bort terrängen flyttades från y 240–256 till y 272–296, så det
  finns över 20 block kvar att bygga på även på den högsta toppen. Mellan kammarna har dalarna plana bottnar
  (y 90–100), och bergssidorna stiger brant ur dem.
- **Platåer:** Savanna Plateau, Marble Gorge och de andra platåbiomerna står på platta platåer (y 125–145) som slutar i
  en enda brant kant ned mot låglandet, i stället för att luta över hela erosionsbandet som i vanilla.
- **Lågland och floder:** låglandet ligger lite högre än förut (y 70–85 i stället för 64–67) och är plant upp till de
  högsta åsarna. Floden ligger alltid i en dal med branta bankar som tar lite mark: 15–20 block djup i skogarna, grundare
  (6–10) på slätterna, och 30–60 och mer mellan bergen. Vattnet ligger kvar i havets nivå.
- **Platta biomer:** `mechanical:flatness` (0..1) är 1 där klimatet säger att marken ska vara platt: torrt (fuktighet
  under -0,15: Plains, Chalk Downs, Pine Heath, Mammoth Steppe), varmt och torrt (Savanna), hett (Desert, Gypsum
  Flats) och kallt och inte fuktigt (Snowy Plains, Ice Spikes). Den gäller bara på låglandet och platåerna och på
  dalbottnarna mellan bergen, så toppar och bergssidor behåller sin form. Där går höjden över i en lugn kopia av
  höjdkurvan (kullar med 12 % av sin höjd kvar, lägre land och mjukare flodbankar) och 3D-bruset dämpas fem gånger
  (`factor`). På övrigt lågland (skog, taiga, Peat Bog) gäller den lugna kopian till 35 % och brusdämpningen till 60 %.
  Windswept Hills behåller sin vilda form.

**Mätt** med kartverktyget (nedan) på tre seeds, 6144 × 6144 block runt 0,0, före (vanilla) → efter:

| | seed 1 | seed 2 | seed 4242 |
|---|---|---|---|
| Land över y 150 | 1,6 % → 4,6 % | 2,7 % → 6,5 % | 0,1 % → 1,1 % |
| Land över y 200 | 0,06 % → 1,6 % | 0,20 % → 2,1 % | 0 → 0,26 % |
| Högsta topp | y 255 → 292 | y 255 → 291 | y 181 → 248 |
| Floddalens djup, median / 90:e percentil (alla floder) | 6 / 35 → 7 / 37 | 7 / 31 → 6 / 29 | 7 / 26 → 7 / 21 |
| Floder med mark över y 130 inom 96 block: antal pixlar, djup median / 75:e percentil | 2393, 38 / 54 → 4890, 39 / 66 | 3212, 23 / 46 → 5304, 23 / 54 | 1418, 35 / 48 → 4957, 36 / 59 |
| Platt mark i de platta biomerna | 67 % → 75 % | 66 % → 75 % | 72 % → 82 % |
| Strukturer: byar / Tribe Village / Ancient Oak / Elkwight-ruin (12 288 × 12 288 block) | 105 / 41 / 87 / 3 → 105 / 46 / 85 / 3 | 104 / 37 / 49 / 1 → 103 / 29 / 52 / 0 | 120 / 34 / 53 / 1 → 118 / 34 / 57 / 2 |

Platt mark (lutning högst 1 block på 4), alla tre seeds: Plains 70 → 78 %, Snowy Plains 65 → 74 %, Sunflower Plains
73 → 84 %, Savanna 71 → 79 %, Desert 72 → 86 %, Mammoth Steppe 57 → 71 %, Pine Heath 76 → 86 %, Hazel Meadow 67 → 76 %,
Chalk Downs 75 → 79 %, Peat Bog 73 → 76 %; Forest 65 → 68 %, Birch Forest 67 → 72 %, Taiga 63 → 69 %, Deciduous Forest
70 → 74 %, Dark Forest 68 → 71 %, Meadow 51 → 52 %, Windswept Hills 44 → 42 %. Marble Gorge 58 → 43 %: platåns topp är
platt, men den branta kanten och flodkanjonerna ligger inom biomen. Låglandet (erosionsband E3 och E4) är 84–87 % platt
mot 65–79 % i vanilla; kuperat är det nu nästan bara vid bergen. Eftersom låglandet är plattare klarar fler platser
Tribe Villages och Ancient Oaks krav på plan mark, så deras `frequency` i structure_set sänktes (0,77 och 0,82) för att
de ska bli lika många som förut (summa 112 → 109 och 189 → 194). På en riktig dev-server (seed 2) nådde toppen vid
2132, 484 y 295, och floderna, också i en bergsdal vid 2418, -368 och vid slätten vid 2240, 928, hade vatten.

Bilder, före till vänster och efter till höger: `docs/shots/terrain/fas1_relief_seed1.png`, `_seed2`, `_seed4242`
(skuggad relief), `fas1_slope_seed2.png` (lutning: grönt platt, gult, orange, rött, lila brant) och skärmbilder från
seed 2: `fas1_range_air.png`, `fas1_highland_air.png`, `fas1_range_ground.png`, `fas1_valley_air.png`,
`fas1_valley_ground.png`, `fas1_plains_air.png`, `fas1_plains_low.png` och `fas1_plains_ground.png` (slätten vid
foten av massivet vid 2128, 488), `fas1_forest_air.png`, `fas1_forest_low.png` och `fas1_forest_ground.png` (skog under
ett berg vid 2816, -240) och `fas1_deciduous_ground.png`. Lutningskartan visar kontrasten tydligast: grönt lågland,
rött och lila i bergen.

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

## Kanter som flyter ihop (client, EdgeBlend)

Där två besläktade block möts på ovansidan går gränsen inte längre i en rak söm: det starkare blocket lägger sig några
pixlar in över det svagare, som i "Blend"/"Continuity"-moddarna. Bara utseende (klienten, när chunken byggs); inga block
ändras. Av/på: `blendEdges` i `config/mechanical-client.toml` (också under Mods > Mechanical > Config).

**Grupper.** Bara block i samma grupp flyter ihop: **mark** (Grass Block, Moss Block, Snow Block, Mycelium, Podzol, Sand,
Red Sand, Gravel, Beach Pebbles, Chalk Block, Wood Ash, Loess, Coarse Dirt, Rooted Dirt, Peat, Clay, Kaolin, Mud, Packed
Mud, Dirt, Worn Grass, Trodden Path, Dirt Path), **sten** (alla bricks och tiles, cobblestone, Fieldstone, rå sten och
stenfamiljernas rå sten, polerad och slipad sten) och **plankor** (alla plankor). Gräs, mossa, grus, Cobblestone och
Mossy Cobblestone hör till både mark och sten: gräs växer in över kullersten och berg, grus rinner ut över berget, och
kullerstenens stenar ligger in i jord, grovjord och stigar. Bricks och polerad sten möts aldrig med jord, sten och
plankor möts aldrig, och plankor aldrig med gräs.

**Styrka.** Varje block har en styrka i en enda tabell (`scripts/gen_blend_edges.py`, starkast först), och det starkare
lägger sig alltid över det svagare: Grass Block > Moss Block > Snow Block > Mycelium > Podzol > Sand > Red Sand > mossiga
bricks > bricks > tiles > spruckna bricks > Mossy Cobblestone > Cobblestone > Cobbled Deepslate/Slate > Fieldstone > Gravel
> Beach Pebbles > rå sten (Stone, Andesite, Granite, Diorite, Tuff, Calcite, Deepslate, stenfamiljerna) > polerad/slipad
sten > Chalk Block > Wood Ash > Loess > Coarse Dirt > Rooted Dirt > Peat > Clay > Kaolin > Mud > Packed Mud > Dirt > Worn
Grass > Trodden Path > Dirt Path. Plankor: Dark Oak > Bog Oak > Spruce > Mangrove > Jungle > Acacia > Oak > Birch > Cherry
> Pale Oak > Poplar > Crimson > Warped > Pine > Yew > Alder > Ash > Aspen > Beech > Hazel > Linden > Willow. Polerad sten,
Worn Grass, Trodden Path och Dirt Path tar emot kanter men lägger sig aldrig över något. Moddens stenar (alla
stenfamiljer: rå sten, cobbled, bricks, mossiga och spruckna bricks, tiles, takpannor, mosaik, polerad/slipad/cut),
plankor och markslabbarnas block läses ur `StoneFamilies.java`, taggen `minecraft:planks` och `EarthBlocks.java`, så nya
block kommer med av sig själva, och generatorn stoppar med ett fel om ett block av rätt sort saknar rad.

**Kantens form följer materialet:**
- **Mark:** en trasig, prickig kant 2–5 pixlar djup med lösa korn utanför, också runt hörn (som i referens-gifen).
- **Rå sten:** hela stenbitar. Cobblestone, Mossy Cobblestone, Cobbled Deepslate, Cobbled Slate och Fieldstone skärs
  längs sina egna stenar (texturen delas upp i stenar), rå sten i oregelbundna klumpar.
- **Bricks och tiles:** kanten följer texturens fogar. Vinkelrätt mot skiften når vartannat skift en halv sten in över
  grannen, längs ett skift sticker var tredje sten ut ett skift, med fogbruket runt sig.
- **Plankor:** brädorna går in i varandra. Tvärs över brädorna löper varannan bräda vidare till en skarv (en mörk ände
  målas där den inte är texturens egen), längs brädorna byts en bit av kantbrädan, och den fortsätter in i nästa block.

**Regler:** bara ovansidor i samma höjd (eller 1/16 högre, så gräs växer in över en Dirt Path), slabs med slabs. Kanterna
varieras per position så att en lång kant inte upprepar sig, och ändarna på en kant stämmer med grannblockets. Gräs
som lägger sig över något får exakt gräsets färg på sin plats, med årstid och vinterfrost.

**Prestanda:** grannarna läses bara när en chunk byggs (på chunktrådarna, inte i renderingen), och block vars ovansida
är täckt hoppas över direkt. Mätt på en äng med väg och stig (render distance 12, ~360 sektioner byggda om): alla
sektioner tar 743–777 ms CPU utan och 867–871 ms med (cirka 2,1 mot 2,4 ms per sektion, +12–15 %), varav själva
kantkoden 45–51 ms. FPS påverkas inte, bara hur fort chunks byggs om. Mäts med `people/BlendShots` (`bench`).

**Bilder:** `docs/shots/terrain/blend_*_before.png` / `blend_*_after.png` (mark, sten, plankor, moddens block, stig,
handelsväg), `blend_overview_after.png`, `blend_road_eye_after.png` och `blend_ground_winter.png` (frostat gräs).

## Grottbiomer

**Limestone Caverns** (tempererat, mitt inne i landet, mellandjup): golv, tak och väggar av Limestone. Fler grottbiomer
kommer i fas 5 och 8.
