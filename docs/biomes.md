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
Sedan v0.184.0 ser Marble ut som naturligt berg: vitgrå och molnig med mjuka grå ådror som slingrar och tonar ut inne i
blocket. Fyra varianter som vrids åt alla håll slumpvis, så en klippvägg aldrig visar samma ruta i ett gitter (samma
block-id som förut). Smooth Marble är nästan vit med svaga ådror, Marble Bricks har skarpa fogar, Pillar är räfflad och
Chiseled har en ram med en upphöjd romb (sidan) och en rosett (ovansidan). Texturerna: `scripts/gen_marble_textures.py`.

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
som Mushroom Fields). Grå, kall himmel och dimma, aska i luften. Inga djur utom korpar (Raven); monster på natten som
vanligt. Snö och frost på vintern som i andra svala biomer. Här står de sällsynta **Elkwight-ruinerna**
(`docs/elkwight-plan.md`), alltid mitt i biomet. Biomen tar en del av sval Forest (`ModBiomes.SURFACE`); närmare 0,0 än
2500 block blir den platsen Forest igen (`mixin/MultiNoiseBiomeSourceMixin`).

Omgjord 2026-11-23 (v0.183.0) efter att den första versionen såg ut som snö med pinnar i:
- **Marken:** mörkgrå **Wood Ash** (tre texturvarianter som väljs och vrids per block, så att rutan aldrig syns som ett
  rutnät) med fläckar av nästan svart **Burnt Soil** (10–20 block stora, och enstaka klumpar i askan). Askan ligger kvar i
  svackorna, på krönen syns den brända jorden. Under ytan Burnt Soil, i branta sluttningar Blackstone och Basalt. Inget
  grus och ingen orange Coarse Dirt längre. Kanterna mellan aska och jord fransar sig (kantblandningen), och halvstegen
  blir Wood Ash Slab och Burnt Soil Slab.
- **Träden** (`ScorchedBarrens.CharredTree`): stubbar (en del med splitter på toppen eller rotben längs marken), döda
  stammar i alla höjder med avbrutna eller avsmalnande toppar och grenstumpar, lutande stammar, ibland ett stort gammalt
  träd med tjock fot, förkolnade grenar som når ut och en kluven topp, och stammar som ligger platt på marken för sig
  själva (aldrig ihopsvetsade med en stubbe; var tredje halvt begravd i aska), ibland med stubben de bröts av en bit bort.
  De smala delarna är **Charred Branch** (8 px) och **Charred Twig** (4 px), som fogar ihop sig som korusplantor.
  Tätheten går i vågor: täta bestånd, glesa partier och gläntor.
- **Kanten mot skogen** (`worldgen/BurntForest`, ungefär 16 block in): ytterst sveden mark, gräs i biomets torra
  olivbruna ton med Short och Tall Dry Grass, Leaf Litter och någon ormbunke; träd av skogens eget slag som har klarat sig
  med förkolnad fot och glesa, döda bruna kronor (biomets lövfärg gör eklöv bruna); längre in Burnt Soil och halvbrända
  träd (mest förkolnade, bark kvar högst upp, inga löv); sedan den helt brända skogen. På skogssidan (de levande
  skogarna runt om, upp till cirka 9 block ut) har trädens fötter förkolnat, kronorna glesnat, gräset torkat och här och
  där syns en fläck bränd jord eller Podzol.
- **Återväxt:** sparsamt inne i biomet: små bestånd av **Fireweed** (rosa, två block hög), ormbunkar i lä av en stam, någon
  enstaka björkplanta och tuvor av torrt gräs. Växter slår rot i Burnt Soil (taggarna `supports_vegetation` och
  `supports_dry_vegetation`), inte i ren aska.

**Mätt** (seed 4242, Scorched Barrens vid 3600, 4920; översta blocket i 40 × 40 kolumner, före = v0.181.0):

| | före | efter |
|---|---|---|
| Inne i biomet: Wood Ash (block + slab) | 50 % | 39 % |
| Inne i biomet: Burnt Soil (block + slab) | – | 29 % |
| Inne i biomet: Coarse Dirt + grus | 17 % | 0 % |
| Inne i biomet: stamblock (Charred Log) i ytan / totalt | 55 / 237 | 29 / 109 (+ 34 grenar och kvistar) |
| Kanten (västra gränsen): gräs, torrt gräs, ormbunkar | 5 % | 12 % |
| Kanten: Burnt Soil | – | 24 % |

Bilder: `docs/shots/terrain/barrens_*.png` (efter) och `barrens_before_*.png`.

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
- **Snag** (omgjord v0.184.0): dött stående träd, 4–9 högt. Bark (skogens eget träslag, Log) bara vid foten, ett block,
  två på ett högt träd. Ovanför silvergrå, sprucken **Deadwood** (eget stockblock utan bark) med ibland en **Grub Log**
  (samma gråa trä, borrhål av skalbaggar och ett hackspettshål). Toppen är **Broken Deadwood**: en avbruten, taggig topp med
  en spjälkad flisa som sticker upp. En eller två korta, tunna grenstumpar (**Deadwood Branch**, 6 px tjocka, ibland två
  block: ut och vidare eller ut och upp). Var tredje har **Shelf Fungus** (tickor) lågt på stammen. Stammen står rak (en
  lutning med ett block åt sidan provades men såg ut som en 7:a på håll). Vid foten flagor av trädets bark (**Fallen Bark**, en per
  träslag, ger trädslagets Bark), en kvist eller två (Twigs), ormbunkar och mossa, och ibland ligger den avbrutna toppen
  (2–3 Deadwood) i gräset bredvid. Var fjärde är bara en stubbe.
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

**Täthet, tredje omgången** (v0.195.0, seed 20261123, 31 områden om 12 × 12 chunks som `locate biome` hittade, 4 464
chunks, `DECOR_TEST` = tätare försök; omräknat till de riktiga försöken per 1 000 chunks över alla områden): Anthill ~54,
Dry Creek 13 chunks med fåra (ungefär var fjortonde chunk i torrt land; samma i riktigt spel), Snow Patch ~2,5, Ice Blocks
~3,5, Root Cellar ~0,5. Avstängda (provvärden): Talus 14, Fallen Rock 4, Bird Cliff 0,4, Rock Overhang 11, Frost Split
27, Farm Walls 8.

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
bara naturlig mark, aldrig i en chunk som en ytstruktur når in i, snön lägger sig ovanpå som vanligt. Allt som är på
utom vattenfall och vårdträd läggs även i gamla chunks av `Retrogen`.

**Bara det som håller måttet i närbild är på.** Varje sak har prövats med skärmbilder i sin biom; det som såg klumpigt
ut eller inte gick att få en bra bild av är avstängt (koden finns kvar och feature-typen är registrerad, men
`gen_land_decor.py` skriver ingen placed feature eller biome modifier för den, listan `OFF`).

**Två nya block** för benen (`worldgen/OldBonesBlock`, modeller av tunna ben i `gen_land_decor.py`, man går igenom dem):
**Rib Cage** (ett par revben som välver sig över ryggraden, även halvt nedsjunken; ger 1–3 Bone) och **Mammoth Tusk**
(en böjd bete i två delar, roten och den framåtböjda övre delen, byggd av korsade smala bitar som smalnar av mot spetsen
så att den ser rund ut; roten ger en Tusk).

**Formade av en täthet i halvblock (`worldgen/Shaper`, v0.187.0).** Valvet, hällen, det frostsprängda blocket,
talus, det nedfallna blocket och jordkällarens kulle byggs inte block för block utan som en mjuk form (en täthet som är positiv inuti, styrd av jämnt brus):
varje cell provar sina åtta åttondelar, nästan allt inuti blir ett helt block, en halva en slab och en halva plus en
eller två fjärdedelar en trappa (rak eller yttre hörn, rättvänd eller uppochned). Slabs och trappor är stenens egna
(`SmoothGround.variant`: Stone Slab, Granite Stairs, Grass Slab ...). En halvbit sätts aldrig i stället för mark (den
skulle göra en grop), och det som hänger fritt fylls ner till marken (`Shaper.seat`). Så blir silhuetten rund och
oregelbunden i stället för kantig.

### På

- **Rock arch:** naturligt valv, resten av en klippfena som nötts igenom: öppningen en båge bredare än den är hög (8–11
  bred, 4–5 hög) med rundad undersida, berget ett band runt den, tunt över krönet (ungefär 2 tjockt, 2–3 djupt) och
  tjockt i benen som vidgar sig ner i marken. Ovankanten en jämn kurva; det ena benet grövre och fortsätter som en låg rygg
  som sluttar ner i marken. Under spannet fylls inget ner till marken (det var det som gjorde förra versionerna till en
  port med pelare). Av stenen under marken med fläckar av en andra sten och mossa i milt land; i badlands Red Sandstone
  med vågräta band av Terracotta genom båda benen och spannet. Grus- eller sandslabs och nedfallna stenar vid fötterna.
  Sällsynt (1 på 40 chunks försök, på plan mark): Stony Peaks, Windswept Hills, Savanna Plateau, Slate Highlands,
  Limestone Karst, Marble Gorge, Badlands, Ochre Badlands. Bilder `fas4b_rock_arch*.png`.
- **Dead tree:** dött, silvergrått träd av **Deadwood** utan bark: stam 3–6 som ibland lutar ett block, rötter av
  Deadwood Branch som löper ut över marken och ner i den. Två eller tre ledare grenar sig ur toppen (första biten
  Deadwood), sprider sig i grenar som går lågt och brett, klättrar ett steg bara efter två ut, gaffrar sig och slutar i
  **Deadwood Twig** (nytt, 4 px); ibland en låg gren ur stammen och en taggig avbruten topp. På savannen flatare krona.
  Coarse Dirt i en trasig fläck vid foten, Dead Bush, torrt gräs och kvistar, ibland en nedfallen gren. Desert, Badlands,
  Gypsum Flats, Ochre Badlands, savann. Bilder `fas4b_dead_tree*.png`.
- **Rock outcrop (häll):** låg ryggformad häll 6–13 lång och 1–2,5 hög, flack åt ena hållet och brantare på läsidan,
  rundad i halvblock och nedsjunken i grässvålen vid kanterna; berget under marken (Granite eller Stone om inget finns
  nära) med ådror av en andra sten, mossa i fläckar, gräs, ormbunkar och blommor vid foten. Inte i snöland (där syns den
  inte). Bild `fas4b_rock_outcrop.png`.
- **Valley waterfall**, **Bone remains**, **Sand skeleton**, **Mammoth tusks**, **Lone tree** och **Fire ring**: som
  förut (bilder `fas4_*.png`).

På sedan v0.195.0 (tredje omgången, granskad i naturligt genererade provvärldar med `DECOR_TEST=1`, se täthet nedan):

- **Dry creek:** helt omgjord. Inte längre en kort fåra per försök utan ett världsomspännande mönster: bäcken går längs
  nollinjen av ett jämnt brus (vickat av ett finare), ett långsamt brus säger var bäckar finns alls, så varje fåra blir en
  lång slingrande sträcka (hundra block och mer) som smalnar av till inget i båda ändar. Varje chunk lägger sin egen bit
  (placeringen körs en gång per chunk), så fårorna går över chunkgränser. Kanalen ett block ner och 1–3 bred, sidorna ett
  halvt steg ner (Gravel Slab nära kanalen, annars markens egen slab), botten som förut (grus, sand/jord i drivor, Packed
  Mud där den är djupast, småsten), döda buskar och torrt gräs bortom kanten, ibland en blekt gren. Aldrig vatten: bara i
  biomer utan regn, där marken lutar högst 2 över 4 block, och aldrig inom 9 block från vatten. Bilder
  `fas4c_dry_creek*.png`.
- **Snow patch:** flyttad ner under den eviga snön, där den syns: från y 115 i svala berg (y 150 i varma som Stony Peaks),
  aldrig där snön ligger året runt (`Thaw.coldAllYear`) eller marken redan är snö. Fläcken av Snow Block nedsänkt i en
  svacka med Coarse Dirt och grus i kanten (inte längre Mud, som blev svarta hål). Bäst på de grå bergssidorna i Stony
  Peaks. Bilder `fas4c_snow_patch*.png`.
- **Anthill:** inte längre en stor kupolmodell. Stacken byggs av `Shaper` som en låg, lite sned, trubbig kon 3–5 bred och
  upp till ett och ett halvt block hög av Podzol och Coarse Dirt i halvblock (slabs, trappor), och bara på krönet sitter
  blocket Anthill, nu en liten rundad topp av barr (modellen `anthill_small`, samma textur genom alla skikt så att inga
  ränder syns). Kjol av Leaf Litter och kvistar runt. Bilder `fas4c_anthill*.png`.
- **Ice blocks:** nya block **Packed Ice Slab** och **Packed Ice Stairs** (`EarthBlocks`, `gen_dirt_slab.py`, halkiga,
  Silk Touch), så `Shaper` kan luta isbitarna. Bitarna är nu tjocka men flata skivor tippade åt alla håll. Läses som
  lösbrutna isflak i snön; ibland syns en liten grop med jord bredvid en bit. Bilder `fas4c_ice_blocks*.png`.
- **Root cellar:** omgjord för sluttningar. Läggs bara där marken stiger stadigt bakom en plan plats (1–3 upp två block in,
  2 fyra in, 3–6 sex in, över tre blocks bredd), så att fronten sitter i en riktig sluttning. Rummet ligger i markplan
  in i backen, jord läggs på i halvblock bara där kullen över rummet är för tunn (den smälter ihop med backen), en
  grästorvskant över överliggaren, och framför en gård skuren i sluttningen mellan vingmurar som trappar ner (3, 2, 1
  högt). Inga trappsteg ner längre. Gårdsmurarna (Farm Walls) lägger inte längre någon jordkällare (de står på plan mark).
  Slätter, ängar, Hazel Meadow, Chalk Downs och Windswept Hills, 1 på 12 chunks försök. Bilder `fas4c_root_cellar*.png`.

### Avstängt (byggt men håller inte måttet)

Prövat en tredje gång i v0.195.0 (placeringen först, med räkning av varför försöken ger upp: `Decor.reject`, loggas
med `decorlog.flag`):

- **Talus** och **Fallen rock:** ny regel att foten ska synas (inga väggar inom 10 block utåt som reser sig mer än 3,
  inget träd inom 5, inte i kallt land där snön begraver dem). Ändå: av 10 000 försök i provvärlden hittade bara ett par
  procent en bergvägg alls, och väggarna som finns är terrasserade sluttningar, vita kalkväggar eller snötäckta. Fläktarna
  blev spridda småbitar, inte en kon; inget gick att få en bra bild av.
- **Bird cliff:** större (upp till 14 avsatser 9 block åt varje håll, längre och bredare vita strimmor, fler duvor) och
  kräver 8 block lodrät vägg. Bara 2 i hela provvärlden (4 464 chunks), och strimmorna drunknar i snön på väggarna.
- **Rock overhang:** klippsökningen tar nu Terracotta som berg (`MountainDecor.cliff(..., clay)`), så den läggs (51 i
  provvärlden), men badlands väggar är låga terrasser av röd sand och terracotta: hålorna blir hack i en trappa, inte ett
  överhäng.
- **Frost split:** sjunken till sin bredaste punkt (inga raka sidor av `seat`), spricka som vidgar sig uppåt, halvorna
  lutar isär. Men blocket läses fortfarande som raka, kantiga klumpar på avstånd; av.
- **Farm walls:** kräver nu plan mark (±1) över hela åkern och inget vatten inom 3 runt den. Gårdarna hamnar nu på slätt
  mark men murarna läses som spridda stenhögar och murstumpar, inte som en inhägnad; av.
- **Juniper in plains:** ingen bild igen (provvärldens slättområden blev snöiga eller vid sjöar, och att sätta en en med
  kommando i DecorShots gav ingen synlig buske); av tills den är sedd.

**Advancement** (Wilderness): *Under the Arch* (stå under ett valv: berg 3–7 över fötterna och 2–6 åt båda sidorna,
fri himmel framför och bakom; under *Roof of the World*, tillbaka med valvet) och *Old Bones* (stå på, i eller bredvid
ett Bone Block, Rib Cage eller Mammoth Tusk; under *White Downs*).

**Gametests:** `valley_waterfall`, `rock_arch` (prövar också `Shaper`:s former) och `dead_tree` (ingen tunn gren pekar
ut i luften) i `worldgen/NatureDecorTest`.

**Täthet** (seed 20261123, antal per 256 × 256 block kring där biomen hittades; grannbiomer blandas in; mätt före
v0.187.0): Badlands: Rock Arch 2–3, Dead Tree 26, Dry Creek 7. Desert: Dead Tree 13, Dry Creek 5, Sand Skeleton 1.
Savanna: Dead Tree 6, Dry Creek 4, Bones 2, Lone Tree 1. Plains: Lone Tree 1, Fire Ring 1, Bones 1. Mammoth Steppe:
Mammoth Tusks 8, Bones 5. Snowy Plains: Bones 2. Dry creek kräver nu plan mark längs hela sträckan och blir färre.

**Mätverktyg:** samma `decorlog.flag` som i fas 3; berg- och klippsakerna loggar också `<sort>_dir_<vinkel>` (sidan att
titta från). `DecorShots` kan köra kommandon före en bild (`@ x y z cmd`, flera med ` ; `), prova nästa plats bara om
inget placerats (`@@`) och sikta på det senast placerade (`namn last avstånd vinkel`, vinkel `dN` = loggad vinkel + N).
Ett kommando i en levande chunk läser nu `OCEAN_FLOOR` i stället för den inaktuella `_WG`-höjdkartan (`StoneAgeFeature.surface`).

## En natur som förändras med tiden (terrängplanen fas 5–7)

**Naturens klocka** (`worldgen/NatureClock`): varje chunk i överjorden sparar kalenderdagen den senast levde. När den
laddas, och varje ny dag medan den är laddad, lever den ikapp dag för dag (högst ett år bakåt), med högst 48 ändrade block
och 8 dagar per svep, och varje chunk högst ett svep i sekunden, högst 2 chunks och 2 ms per tick. Inte på gametest-servern (testerna kör dagar för hand). Varje dags tärningar sås med världens
seed, chunken och dagen, så det blir samma resultat när man än kommer tillbaka.

Regler:
- Varje chunk minns var spelare har satt block (`placed`, upp till 512; fler och chunken räknas som byggd). En chunk som
  fanns innan klockan och där någon har byggt (Retrogens kontroll av spelarblock nära ytan) eller vistats mer än 20 minuter
  räknas också som byggd. Byggda chunks och deras grannar lämnas helt i fred.
- Inget nära ett läger, en Fire Pit eller en väg (samma kontroll som `SmoothGround`), inget i chunks med en ytstruktur.
- Inget sker i en chunk som någon spelare kan se (en synkon på 128° ut till 192 block, eller man står i den). Chunken
  väntar tills man tittar bort. Bara stormfällning får synas.
- `/nature status | pause | run <dagar> [radie] [process] | demo log | fell | burn | strike | do flood|recede|drift|rockfall <radie> | height` (op): läget i chunken, stoppa klockan, köra
  dagar för hand runt sig (regler av, bara inom radien), lägga en färsk stock, fälla ett träd framför sig.

**Stockar som ruttnar** (`worldgen/NatureRot`, fas 5): en fallen stock (se ovan, "Hitta fallna stockar"; också Deadwood
och Charred Log) tar ett steg de flesta dagar (95 %): färsk → Moss Carpet på toppen → ormbunkar, mossa och svamp längs
sidorna → en planta av samma träd gror bredvid → barken faller av → den sjunker ner i två steg från ändarna och blir Moss
Block och Podzol. Ungefär en vecka (speldagar, en speldag är en timme). Nya stockar: i biomer med fallna stockar kommer en
ny i en chunk var 12:e dag (var 5:e i gammelskog), ForestDecors egen stock, så skogen behåller sina lågor.

**Stormfällning** (`worldgen/StormFelling`, fas 5): i åska fäller vinden ett högt träd (stam 6+, högst 48 stockar) 12–48
block från en spelare, ungefär en gång på 50 sekunder per spelare. Ett träd som någon ser hoppas över två gånger av tre, så
de flesta faller utom synhåll. Trädet knakar och faller med vinden (`FallingTreeEntity`), kronan blir lövströ, en stubbe
blir kvar och stammen läggs som en fallen stock av stockblock (högst 12 långa) som sedan ruttnar. Advancement
*Windthrow* (Wilderness) för den som ser ett falla.

**Brand och återväxt** (`worldgen/Wildfire`, fas 6): en torr sommardag (sommar, inget regn) slår torr blixt ned ungefär
en gång på 40 minuter per spelare, 48–110 block bort, utom synhåll, i träd eller gräs (inte i öken, hav, flod, kallt land
eller Scorched Barrens) där alla chunks två chunks runt är fria. Vanillas eld tar (den sprids bara inom 128 block från en
spelare och slocknar i regn). Elden bevakas: när den har slocknat står stammarna som brann upp igen som Charred Log och
marken där elden gick blir Burnt Soil och Wood Ash (mjuka marksteg blir Burnt Soil Slab och Wood Ash Slab). Där ingen är kan
en skogschunk brinna en torr sommardag (1 på 20 000 per chunk och dag, en fläck med radien 7). Sedan växer det tillbaka
(per dag): Wood Ash och Burnt Soil blir Coarse Dirt (1/30), Fireweed kommer upp ur Burnt Soil (1/60), Coarse Dirt blir gräs
(1/40), på bart gräs kommer gräs, Fireweed, blommor eller en planta (1/50; plantor med 3 blocks avstånd, mest björk,
pionjären, annars närmaste levande träds trä), de brända stammarna vittrar ett block i taget (1/40) ned till stubbar. Efter
ungefär en till två årstider står ung skog. När ingen bränd mark är kvar och unga träd står har chunken vuxit igen:
advancement *From the Ashes* för att stå i den. Aldrig i Scorched Barrens. `/nature burn <radie>` bränner en fläck runt
en, `/nature strike` slår ned en torr blixt framför en.

**Skogen vandrar** (`worldgen/ForestCreep`, fas 6): plantor växer upp till träd även på klockan (1/40 per dag), så en chunk
ingen besöker får också sina träd. I skogsbrynet kommer då och då en planta av brynets trä upp ute på ängen, 3–7 block
från en stam och inte under en krona (en ruta per chunk och dag med chansen 1/6). Står betande djur inom 16 block
(boskap, eller vilda gräsätare som hjortar och visenter) håller ängen sig öppen, och hjortar äter plantor ändå
(`WildNature`). I skogar med snags dör ett gammalt träd ibland (1/300 per chunk och dag): kronan försvinner och stammen står
som ForestDecors snag av sitt eget trä. En snag faller med tiden (1/90 per dag): barkfoten blir stubbe och resten ligger som
en fallen stock av Deadwood, som ruttnar som andra stockar.

**Ras** (`worldgen/Rockfall`, fas 7): i tövädret (månaderna Snowmelt och Thaw, chansen 1/6 per bergschunk och dag) och
regniga vår- och höstdagar (1/20) lossnar ett block berg högt i en brant (30+ över havet, ett fall på 4+ inom två block). Det
rullar alltid till den lägsta grannen tills sluttningen planar ut, studsar vidare några block över den plana marken
nedanför (längre ju högre det föll) och blir liggande som sitt eget berg eller Cobblestone, med Gravel och lösa stenar
runt foten. Så växer talus under klipporna med åren. Den som är nära samma dag hör raset.

**Vårflod** (`worldgen/SpringFlood`, fas 7): sent i Snowmelt (dag 335–346, ny start varje år) stiger åar och bäckar ett
block i 3–5 dagar: en vattenkälla läggs ovanpå varje yta av flodvatten och på de plana bankarna (marken i höjd med vattnet)
upp till 5 block ut, i chunkens huvudnivå. Varje vattenblock sparas i chunken och tas bort exakt när floden sjunker, och
rinnande vatten på flodnivån får inte bli nya källor medan floden står. Efteråt ligger Clay och Gravel här och var på
bankarna (12 %) och ibland en drivvedsstock (35 % per chunk). Inte i hav, på stränder eller i träsk. Advancement *High
Water* för att vada i floden.

**Snödrivor** (`worldgen/SnowDrift`, fas 7): en blåsig snödag på vintern (var tredje vinterdag, vinden mest från väster) där
det är kallt nog att snöa lägger sig snön i lä bakom allt som sticker upp: stenblock, stammar, stubbar och liggande stockar.
Drivan är djupast vid foten (två lager per block hindret står upp, högst 6), tunnar ut över fem block i vindens riktning
och breder ut sig lite lägre åt sidorna, ett lager till per blåsig dag. Vårens töväder (`Thaw`) smälter drivorna som all annan snö; de djupa ligger längst.

**Prestanda** (dedikerad testserver, inga spelare, 256 forceladdade chunks plus kanten, 400 i klockan; `/tick query`):
när 400 chunks låg 120–190 dagar efter och levde ikapp samtidigt, mätt växelvis med klockan pausad och igång (12 s
var): P50 6,7–10,4 ms pausad mot 9,4–11,0 ms igång, alltså 0,6–2,8 ms mer per tick. Klockans egen tid var 1,2 ms per svep
(8 dagar i en chunk), och den slutar svepa för tiken när den har använt 2 ms. Ikapp-levandet tog några minuter för alla 400.
När chunkarna är i fas kostar klockan nästan inget: ett svep per laddad chunk och speldag (en timme). Stormfällning,
torr blixt och brandvakten körs en gång i sekunden och är billiga utom själva elden (vanillas).

**Höjdkartor:** i 26.x räknar höjdkartorna bara block i taggen `#minecraft:blocks_motion_in_heightmap`. Alla modens
block som blockerar rörelse (efter den gamla regeln) läggs i taggen av `scripts/gen_heightmap.py`, bland dem Burnt Soil,
Wood Ash, Charred Log, Deadwood, Broken Deadwood och Grub Log; annars läser klockan (och regn, snö och spawning) marken
under dem. Gametesten `heightmap_tags` (`HeightmapTest`) kontrollerar listan mot de riktiga kollisionsformerna.

**Gametests** (`worldgen/NatureClockTest`): `nature_rot` (en stock som lämnas 10 dagar har ruttnat bort i Moss Block och
Podzol, med en ekplanta bredvid), `storm_felling`, `nature_regrowth` (ett brandfält är ung skog efter 300 dagar),
`forest_creep`, `forest_creep_grazed` (inga plantor där kor betar), `old_tree_snag`, `rockfall`, `spring_flood` (vattnet
stiger över bäck och bank och allt är borta efteråt), `snow_drift`.

Bilder: `docs/shots/terrain/nature_rot*.png`, `nature_storm*.png`, `nature_fire*.png` (brandfältet dag 0, 30, 70, 120, 200, 320), `nature_flood*.png`, `nature_drift*.png`, `nature_talus*.png`.

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

## Packad mark (gen_road_ground.py)
Tre nya markblock som både ligger naturligt och är ytan på stammarnas vägar (`people/RoadBuilding`):
- **Packed Sand**: sand som vind, fötter och hjul packat hårt, lite mörkare och tätare än Sand med enstaka småsten (rinner
  inte). Små vindpackade fläckar (radie 2–4, ungefär var femte chunk) i ytsanden i öken och på strand.
- **Packed Red Sand**: samma i badlands (`#minecraft:is_badlands`), i den röda sanden.
- **Packed Snow**: snö som trampats eller blåst hård, gråare och tätare med en fläck jord här och var. Vindskurade fläckar i
  Snow Block på Snowy Slopes, Grove, Frozen Peaks och Jagged Peaks. Smälter med tövädret (`Thaw`) tillbaka till Coarse
  Dirt där det blir varmt nog (slaben till Coarse Dirt Slab).

Alla tre har slabs (3 i rad → 6), rader i kantblandningens tabell (lös sand och snö fransar ut över sin packade sort) och
läggs av `SmoothGround` på steg. Advancement **Hard Ground** i Wilderness. Vägar i sandmark är Packed Sand och Sand med
strödda Coarse Dirt, grus och lite Packed Mud och två brutna hjulspår; i snömark Packed Snow med jord i hjulspåren och
lös snö tillbaka på kanterna.

## Stigarnas broar och vägen in (worldgen/Trails)
De förgenererade stigarna mellan byar, Tribe Villages och Ancient Oaks:
- **Höga gångbroar:** där en å ligger i en skärning med höga stränder går stigen inte längre ner för två stup till en
  plankbana vid vattnet. Bron spänner i stället över skärningen i strändernas höjd: däcket ligger så högt som den lägre
  stranden når (högst 12 block över vattnet) och går rakt från strand till strand, också över sluttningarna ner mot
  vattnet. Däcket är 3 block brett av slabs (Spruce eller Oak, samma som stigens andra broar), med en bjälke av Spruce/Oak
  Wood (bark runt om) och ett Fence-räcke på var sida, och var fjärde block ett par stockstolpar ner till marken eller
  åbotten med en tvärbjälke under däcket. Där stranden når upp till däcket skärs en skåra så däcket går in plant, och man
  går upp på bron med ett halvsteg. Höjden räknas ur brusterrängen längs stigen (inte ur lagda block), så varje chunk
  bygger samma bro och en bro över en chunkgräns ligger i en nivå. Låga stränder (ett däck högst 2 över vattnet), sjöar
  bredare än 32 block och flacka dalar (mindre än ungefär 1 blocks stigning på 2) behåller den låga plankbanan eller
  trampstenarna. Smala bäckar på 1 block kan slinka mellan stickproven och får då den låga bron.
- **Runt palissaden till porten:** en stig till en Tribe Village går aldrig rakt genom byn. Den böjer av runt utsidan av
  palissaden, ungefär 4 block utanför stockarna, och slutar vid porten där byns egen stig börjar. Samma sak vid en
  Ancient Oak: stigen går runt stammen och rotstöttorna till öppningen in i den ihåliga stammen.

Bilder: `docs/shots/terrain/trails2_*.png` (ravin före och efter, bron från sidan och från stranden, byn uppifrån och vid
porten före och efter, en låg övergång som är oförändrad).

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
