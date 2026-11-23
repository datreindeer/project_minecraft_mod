# Plan: ny terräng och en natur som förändras (2026-11-23)

> Status: plan. Byggs fas för fas, och varje fas förs in i `docs/biomes.md` och åldersdokumenten. Beslut som Claude har
> tagit står under *Beslut* längst ned och kan ändras.

Det här vill du ha:
- högre berg (200+) och djupa dalar;
- plattare mark i biomer som inte ska vara bergiga;
- mjuka sluttningar med slabs och trappor;
- massor av små saker i landskapet;
- en natur som förändras med tiden även när ingen är där.

## Utgångsläge

- **Terrängformen** är vanillas (`noise_settings` och density functions). Modden bestämmer bara var varje biom hamnar
  (`ModBiomes.SURFACE`, `ScorchedBarrens`) och vilka block marken har (material rules).
- **Vanillas höjder:** höga toppar når bara ungefär y 200–260 i de högsta bergen, och de är sällsynta. Det mesta landet
  ligger på y 64–110, fullt av småkullar även i biomer som borde vara platta. Floddalar är grunda.
- **Det som redan finns:**
  - Dirt Slab, Grass Slab och Dirt Path Slab (`DirtSlabBlock`, `gen_dirt_slab.py`). Gräset kryper upp på dem.
  - Fallna träd (`fallen_beech_tree`, `fallen_pine_tree`) och `grub_log_snag`.
  - `mossy_boulder`, `sandstone_boulder`, `glacial_erratic`, `balanced_rock` och `cairn`.
  - Charred Log och Wood Ash (Scorched Barrens).
  - `TreeFelling` och `FallingTreeEntity`, som fäller träd med fysik.
  - `Retrogen`, som lägger till saker i redan genererade chunks.
  - Kalender och årstider, och block som reagerar på klimatet.
  - `BiomeMap`, verktyget som ritar biomkartor.

## 1. Terrängens form

**Teknik:** vanillas density functions skrivs över i datapacket:
- `data/minecraft/worldgen/density_function/overworld/offset.json` (höjden);
- `.../factor.json` (hur kuperat det är);
- `.../jaggedness.json` (taggiga toppar).

Det är splines över continentalness, erosion och ridges. Ett nytt skript, `scripts/gen_terrain.py`, läser vanillas filer
ur Minecraft-jarren, ändrar punkterna i tabeller och skriver ut våra versioner, så att ändringen syns som tal och inte som
handskriven JSON. Biomernas klimatplatser rörs inte.

| Mål | Hur |
|---|---|
| **Bergskedjor 200–280** | Offset höjs där erosionen är låg och ridges är "peaks". Höga berg blir vanligare, och toppar som i dag stannar på 160–200 når 220–280. Jaggedness höjs lite på de högsta topparna. Byggtaket ligger kvar på 320, så det finns plats att bygga. |
| **Djupa dalar** | Ridges-"valley" sänks mellan bergen. Floddalar blir 30–60 block djupa med branta sidor, och floden ligger i botten. Lägre än havet blir dalen en sjö eller en fjord. |
| **Platta biomer** | Factor höjs där erosionen är hög (slätter, savann, öken, Mammoth Steppe, Peat Bog, Chalk Downs, Gypsum Flats), så marken blir flack med långa mjuka sluttningar i stället för småkullar. |
| **Inget ändras under jord** | Grottor, akviferer och malmhöjder lämnas som de är. Kontrolleras med en jämförelse före och efter. |

**Mäts med verktyg och bilder:**
- `BiomeMap` får en höjdkarta med skuggad relief och en lutningskarta.
- Statistik på tre seeds: andel land över y 150 och y 200, högsta topp, andel platt mark (lutning ≤ 1 över 4 block) per
  biom, och dalarnas djup.
- Dessutom skärmbilder från luften och från marken (`TerrainShots`).
- Före- och efterbilder läggs i `docs/shots/terrain/`.

**Risker:**
- **Strukturer:** byar, Tribe Village, Ancient Oak och Elkwight-ruinen behöver plan mark. Antalet strukturer räknas före
  och efter.
- **Spelservern:** den gamla världen får sin nya terräng bara i nya chunks, och vid gränsen blir det en vägg eller ett
  stup. Se fråga 1.

**Byggt (fas 0 och 1, v0.178.0):** `gen_terrain.py` skriver om `offset`, `factor`, `jaggedness`, `final_density` och
`preliminary_surface_level` och en egen `mechanical:flatness`. Ett kontrastlandskap: toppar till y 280–295 (taket
flyttat till 272–296), 3–8 gånger mer land över y 150 och 8–25 gånger mer över y 200, dalar med plana bottnar och
branta sidor mellan bergen, platta platåer med en brant kant, och ett lugnt lågland där slätterna är 8–15 procentenheter
plattare och skogarna 3–6 plattare än i vanilla. Tribe Village och Ancient Oak har fått lägre `frequency` så att de
blir lika många som förut. Mätverktyget är `BiomeMap`s terrängläge (`TerrainMap`), bilderna tas med `TerrainShots`.
Siffror och bilder: `docs/biomes.md` *Terrängens form*, `docs/shots/terrain/fas1_*.png`.
Kvar att avgöra: fråga 1 (spelserverns värld får den nya terrängen bara i nya chunks).

## 2. Mjuk mark med slabs och trappor

Ett worldgen-steg körs efter att ytan är lagd (`top_layer_modification`, en feature per chunk). Det läser höjdkartan och
mjukar upp varje steg på exakt 1 block:

- **Slab:** den lägre rutan får en slab av samma mark som den högre (Grass, Dirt, Sand, Snow och så vidare), så att
  steget blir två halva. Steg på 2 block eller mer är klippor och lämnas.
- **Trappor:** på raka kanter av sten i berg får den lägre rutan en trappa av bergarten i stället: Stone, Slate, Granite,
  Limestone, med flera. Stenfamiljerna har redan slab och trappor.
- **Hörn:** där två steg möts används trappornas hörnformer.
- **Snö:** på snötäckta sluttningar läggs snöskikt i olika tjocklek (1–8 lager, vanilla) i stället för slabs. Det ger
  den mjukaste kurvan och smälter som vanlig snö.
- **Bara naturlig mark:** inte under vatten, inte i strukturer eller på stigar (stigarna har egna trappor) och inte
  under trädrötter.

**Nya block** (som Dirt Slab: bryts till sin "rena" slab, och gräs kryper på och av som på vanlig mark):

| Slab | Trappa | Kommentar |
|---|---|---|
| Grass Slab (finns) | Grass Stairs | färgas efter biom och årstid, får frost, snöig topp och vinterfärg som Grass Block |
| Dirt Slab (finns) | Dirt Stairs | |
| Coarse Dirt Slab | Coarse Dirt Stairs | |
| Podzol Slab | | tallhed och taiga |
| Sand Slab, Red Sand Slab | Sand Stairs, Red Sand Stairs | stränder, öken, Ochre Badlands; faller inte (bundna av rötter) |
| Gravel Slab | | moräner och floder |
| Moss Slab | | lövskog, kärr |
| Mud Slab | | Reed Delta, kärr |
| Snowy Grass Slab | | ett läge på Grass Slab (som `snowy` på Grass Block) |
| Peat Slab, Loess Slab, Chalk Slab, Wood Ash Slab | | biomernas egna marker |

Sten (Stone, Slate, Granite, Limestone, Chalk Block, Sandstone och så vidare) använder slabs och trappor som redan finns.

**Bonus:** djur, stammarnas folk och vagnen går upp för mjuka sluttningar utan att hoppa.

**Den gamla världen:** steget kan köras på redan genererade chunks av `Retrogen`, som lägger slabs bara på orörd naturlig
mark. Spelservern får alltså mjuk mark även där terrängen är gammal.

**Byggt (v0.176.0):** `worldgen/SmoothGround` (alla överjordsbiomer, sist i `top_layer_modification`, och via `Retrogen`
på orörda gamla chunks, inte nära läger, Fire Pits eller vägar). Nya block: Grass Stairs, Dirt Stairs, Coarse Dirt
Slab/Stairs, Podzol Slab, Sand och Red Sand Slab/Stairs (faller inte), Gravel, Moss, Mud, Peat, Loess och Wood Ash Slab
(`gen_dirt_slab.py`). Grass Slab hade redan sitt snöiga läge, och Grass Stairs fick samma. Nedre slabs visar nu gräs- och
snökanten överst, och marken under en gräsig slab blir Dirt (annars syns gräskanten dubbelt). Snöskikt bara där snön ligger året runt, annars snöig slab. Mätning och bilder i `docs/biomes.md`.

## 3. Små saker i landskapet

Detta är enkla features och mallar som läggs ofta och varieras slumpvis. Varje sak listar de biomer den hör till. Det
som redan finns får fler varianter och placeras oftare.

**Skog:**
- Fallna stockar i alla träslag, med mossa och svamp, några halvt begravda.
- Stockar som ligger tvärs över bäckar och smala åar, som broar från strand till strand.
- Döda stående träd (snags) med hackspettshål, och stubbar.
- Uppryckta träd: rotkaka av Dirt och Rooted Dirt, och en grop bredvid.
- Mossiga stenar, Moss Carpet på stenblock och ormbunkstuvor.

**Vatten:**
- Bäverdammar: stockar och lera tvärs över en bäck, med en göl ovanför.
- Ilandflutet drivved på stränderna.
- Vass- och kaveldunsbälten.
- Grusbankar i flodkrökar.
- Källor i sluttningar som blir en liten bäck.

**Berg:**
- Talus (rasbranter av Gravel, Cobblestone och lösa stenar) vid bergets fot.
- Stora nedrasade block.
- Naturliga valv och klippfönster.
- Fågelklippor.
- Små vattenfall från dalsidor.
- Snöfält som ligger kvar på sommaren högt upp.

**Öppen mark:**
- Flyttblock och hällar.
- Benrester (Bone Block, skallar), en djurkadaver-plats.
- Myrstackar.
- Enbuskar och ensamma gamla träd (vårdträd).
- Stenmurar från en övergiven gård, bara på slätter.
- Små jordkällare och gamla eldstäder.

**Torrt och varmt:**
- Döda buskar och torra träd.
- Termitstackar (finns).
- Skelett i sanden.
- Klipputsprång.
- Uttorkade bäckfåror.

**Kallt:**
- Isblock i snöslätten.
- Frostsprängda stenar.
- Mammutbetar som sticker upp ur Loess.

Varje grupp får en advancement i Wilderness-fliken där den passar, till exempel *Gå över en stockbro*, *Stå på en topp
över y 250* och *Hitta en bäverdamm*.

**Byggt (fas 3, skog och vatten, v0.177.0):** `worldgen/ForestDecor` (Fallen Log i närmaste träds träslag, Snag och
stubbar, Root Plate med grop, Mossy Stones) och `worldgen/WaterDecor` (Log Bridge, Beaver Dam med göl, hydda och bävrar,
Driftwood, Gravel Bar, Reed Belt, Hillside Spring), placerade av `scripts/gen_nature_decor.py`, tätare i gammelskog. Allt
utom bäverdammar och källor läggs även i gamla chunks (`Retrogen`). Advancement *Over the Creek*, och *Dam Builders* räknar
nu att stå på dammen. Kaveldun saknas (inget block än), vass finns. Detaljer, täthet och hur fas 5 hittar fallna stockar:
`docs/biomes.md`, "Små saker i skog och vid vatten". Bilder: `docs/shots/terrain/decor_*.png`.

**Byggt (fas 4, berg, öppen mark, torrt och kallt, v0.180.0):** på är Valley Waterfall (`worldgen/MountainDecor`),
Bone Remains, Lone Tree och Fire Ring (`worldgen/OpenDecor`), Sand Skeleton och Mammoth Tusks (`worldgen/DryColdDecor`), placerade av `scripts/gen_land_decor.py`. Två nya block av tunna ben,
Rib Cage och Mammoth Tusk (`worldgen/OldBonesBlock`). Advancement *Old Bones*. Byggda men
avstängda tills de håller måttet i närbild: Rock Arch, Dead Tree, Dry Creek, Bird Cliff (för kantiga eller svåra att se vid granskningen), Talus, Fallen Rock, Snow Patch, Anthill, Ice Blocks, Rock Overhang, Rock
Outcrop, Frost Split, Farm Walls och enar på slätten. Jordkällare är inte byggda. Detaljer:
`docs/biomes.md`, "Små saker i berg, på öppen mark, i torrt och kallt land". Bilder: `docs/shots/terrain/fas4_*.png`.

**Byggt (putsning efter speltest, v0.184.0):** Snag är omgjord och på igen: stam av nytt block **Deadwood** (silvergrått,
sprucket, utan bark) med bark bara vid foten, taggig topp (**Broken Deadwood**), tunna grenstumpar (**Deadwood Branch**),
**Shelf Fungus**, Grub Log ommålad som Deadwood med hackspettshål, och **Fallen Bark** (en per träslag) vid foten. Marble har fått egna texturer (`scripts/gen_marble_textures.py`): naturligt berg med mjuka ådror och fyra
slumpvis vridna varianter i stället för kvartsens kantade rutor; bricks, pillar, chiseled och smooth är nymålade. Bilder:
`docs/shots/terrain/polish_*.png`.

## 3b. Kanter som flyter ihop (önskemål från användaren)

Vägar och mark ska se mjukare ut: där två besläktade block möts på ovansidan lägger sig det starkare några pixlar in
över det svagare ("connected textures"/"Blend"), i grupper så att bara rätt block möts (mark med mark och gräs med det
mesta, sten med sten, plankor med plankor; aldrig stone bricks med plankor).

**Byggt (v0.179.0):** `client/EdgeBlend` (lägger kantbitar på ovansidan när chunken byggs; matchar block på toppens
textur, så slabs och slumpvridna block följer med) och `scripts/gen_blend_edges.py` (styrketabellen, grupperna och ett ark
kantbitar per block som kan lägga sig över andra). Fyra kantstilar efter material: trasig prickig kant för mark, hela
stenar för kullersten och rå sten, tandade bricks som följer fogarna och plankor som går in i varandra bräda för bräda.
Gräsets färg (årstid, frost) följer med. Av/på: `blendEdges` i `mechanical-client.toml`. Styrketabell och detaljer:
`docs/biomes.md`, "Kanter som flyter ihop". Bilder: `docs/shots/terrain/blend_*.png`.

## 4. En natur som förändras med tiden

**Grundidé, naturens klocka:** världen räknar inte ticks i varje chunk. Varje chunk sparar i stället den dag den senast
"levde" (kalenderns dag). När chunken laddas, eller tickar för att någon är nära, räknas det ut vad som borde ha hänt
sedan dess, och det görs i ett svep, högst ett visst antal ändringar per chunk.
- Slumpen sås med chunkens position och dagen, så resultatet blir detsamma oavsett när man kommer tillbaka.
- Förändringar sker alltså även när ingen är där, men kostar bara något när chunken laddas.
- Det kan byggas på `Retrogen`s mekanik för chunk-data.

**Regler för allt nedan:**
- Rör aldrig block som spelare har satt, och inget inom ett lägers zon, en Fire Pit, en väg eller en struktur.
  Vägbyggets kontroll av spelarbyggen återanvänds.
- Inget sker där en spelare tittar, utom stormfällningar, som är tänkta att synas.

| Händelse | Vad som händer |
|---|---|
| **Stormfällning** | Vid åska fäller vinden ibland ett högt träd nära spelare (`FallingTreeEntity`, med ljud och fall). Utom synhåll fälls fler. Kvar blir en fallen stock. |
| **Stocken ruttnar** | En fallen stock går genom stegen: färsk → mossa på toppen → svampar och ormbunkar bredvid → plantor gror vid stocken → stocken sjunker ner och blir Moss Block och Podzol. Det tar några speltimmar per steg. |
| **Skogen vandrar** | Plantor dyker upp i skogsbrynet och kryper ut på ängen, långsamt. Där djur betar (living-world A10) håller ängen sig öppen. Gamla träd dör och blir snags. |
| **Brand och återväxt** | Blixt i en torr sommar kan tända gräs och skog (vanillas eld). Brandfältet blir Charred Log och Wood Ash, sedan Coarse Dirt, sedan gräs, blommor och plantor, och efter några årstider är det ung skog. |
| **Ras** | Vid tö och regn kan ett block lossna högt i en bergssida och rulla ned (fallande block som studsar). Vid foten blir talus kvar. |
| **Vårflod** | Vid snösmältningen på våren stiger åar och bäckar 1 block i några dagar. Strandängen svämmar över, och efteråt ligger lera, grus och drivved kvar. Vattnet som läggs sparas och tas bort exakt. |
| **Snödrivor** | På vintern lägger blåst snö i drivor bakom stenar och träd. De smälter på våren. Detta bygger vidare på blocken som redan reagerar på klimatet. |
| **Bävrar** | Bävern (finns som djur) fäller träd vid vatten och bygger dammar som höjer en bäck (living-world A10). |

**Prestanda:** samma budget som living-world A9. Det mäts med `/tick query` och MSPT innan något släpps, och det finns ett
tak för antalet ändringar per chunk och sekund.

## 5. Faser

| Fas | Innehåll | Klart när |
|---|---|---|
| **0** | Mätverktyg: höjd- och lutningskarta i `BiomeMap`, statistik, `TerrainShots` (bilder från luften och marken). Bilder före. | Kartor och bilder före finns för tre seeds. |
| **1** | Terrängens form (`gen_terrain.py`): höga berg, djupa dalar, platta biomer. | Höjdstatistiken når målen, strukturerna placeras, alla tester går igenom, bilder efter. |
| **2** | Mjuk mark: nya slabs och trappor, utjämningssteget, snöskikt, `Retrogen` för gamla chunks, klimat- och årstidsfärger. | Bilder på slätt, berg och snö; djur och folk går upp för backarna. |
| **3** | Små saker, del 1: skog och vatten (stockar, stockbroar, snags, rotkakor, bäverdammar, drivved, källor). | Varje sak syns på bild i sin biom. |
| **4** | Små saker, del 2: berg, öppen mark, torrt och kallt (talus, valv, vattenfall, ben, stenmurar och så vidare). | Som fas 3. |
| **5** | Naturens klocka plus stormfällning och stockar som ruttnar. | En stock som lämnas i 10 speldagar har ruttnat bort; gametest. |
| **6** | Brand och återväxt, skogen som vandrar, gamla träd som dör. | Gametest för brandfält → ung skog. |
| **7** | Ras, vårflod och snödrivor. | Gametest och bilder per årstid. |

Fas 1, 2 och 3 rör olika filer och byggs samtidigt av var sin agent i var sin worktree. Fas 5 bygger på fas 3:s stockar.

**Varje fas, som alltid:**
- versionshöjning och commit;
- `docs/biomes.md` och åldersdokumentet uppdateras;
- registry-anteckningar (sv/en) och `registry.html`;
- advancements;
- gametests (`runGameTestServer`);
- worldgen testas i en worktree-`run/`, aldrig i huvud-`run/`;
- bilder skickas.

## Beslut (Claude, kan ändras)

1. **Byggtaket** ligger kvar på y 320. Toppar på 220–280 lämnar plats att bygga, och ett högre tak hade krävt att den
   gamla världen konverterades.
2. **Klimatet** rörs inte, så biomernas placering och övergångarna från förra omgången behålls. Bara höjden ändras.
3. **Stormfällning** får synas, det är häftigt. Allt annat sker utom synhåll.
4. **Slabs** läggs bara vid steg på 1 block. Branter på 2+ block förblir klippor, så berg fortfarande ser ut som berg.

## Öppen fråga

1. **Spelserverns värld (AmandaTest):** den nya terrängen från fas 1 kommer bara i chunks som inte har genererats ännu.
   Där den gamla världen tar slut blir det väggar och stup. Alternativ:
   - (a) Ny värld på servern när fas 1 är klar (rekommenderas).
   - (b) Behåll världen och acceptera kanterna.
   - (c) Behåll världen, och den nya terrängen gäller bara längre än ett visst avstånd från spawn, med en mjuk övergång
     emellan. Det är mer jobb.

   Fas 2–7 fungerar i den gamla världen oavsett svar.
