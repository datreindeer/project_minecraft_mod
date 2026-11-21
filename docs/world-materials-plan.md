# Plan: byggmaterial och nya biomer (ovan och under jord)

Status: **faser 0–9 byggda** (material, 24 biomer och användningarna 9A–9C, se "Fas N: resultat"); kvar: fas 10 (åldrande). Skriven 2026-11-21.

Målet: ett 40-tal nya material i världen som först och främst är till för att bygga med, men som alla också har minst
en annan användning. Plus 12 nya biomer på ytan och 12 under jord, byggda kring materialen. Varje material ska gå att
hitta, ha en tydlig plats i en ålder och synas i guiden, registret och advancement-trädet.

---

## 1. Hur det byggs (teknik)

### 1.1 Stenfamiljer: en generator för alla
`scripts/gen_limestone_blocks.py` blev **`scripts/gen_stone_families.py`**: en tabell med en rad per sten. Varje rad säger
vilken vanilla-familj den kopierar (blockstates, modeller, loot, recept och taggar från jarfilen, med namnet utbytt) och
vilken palett texturerna färgas om med. Java-sidan får ett `StoneFamilies.java` med samma tabell, så att ett nytt
material är en rad på varje sida.

Vanilla-mallar att kopiera:

| Mall | Ger | Passar för |
|---|---|---|
| **tuff** | polished, bricks, chiseled, chiseled bricks + slab/stairs/wall ×3 (13 block) | de flesta bergarterna |
| **stone** | smooth, bricks, cracked, mossy, chiseled + slab/stairs | Gneiss, Rapakivi Granite |
| **deepslate** | cobbled, polished, bricks, tiles, cracked, chiseled + slab/stairs/wall | Slate (tiles = takplattor) |
| **sandstone** | smooth, cut, chiseled + slab/stairs/wall | Chalk, Gypsum, Travertine |
| **quartz** | bricks, pillar, chiseled, smooth + slab/stairs | Marble, Alabaster |
| **basalt** | polished, smooth (pelare som kan roteras) | Basalt (ytvarianten), Scoria |
| **mud / packed mud** | packed, mud bricks + slab/stairs/wall | Mudstone/Adobe, Loess, Rammed Earth |
| **bricks** | bricks + slab/stairs/wall | Peat Bricks, Fired Roof Tiles |

Bronze Chisel-kedjan (`BronzeTools.ChiselItem.carved`) får en rad per familj, som Limestone redan har.

### 1.2 Bergarter i berget
Som Limestone: klumpar via `ore`-features i `base_stone_overworld`, plus att de läggs i `base_stone_overworld` och
`stone_ore_replaceables`. **Varje bergart finns i hela världen men är mycket vanligare i "sin" biom**, så att den går att
hitta utan att just den biomen ligger nära.

### 1.3 Ytbiomer
Samma väg som de fyra biomerna i `gen_biomes.py`: varje biom tar en del av en vanilla-biomens klimatzon via
`OverworldBiomeBuilderMixin.addSurfaceBiome`. Terrängens form ändras inte, bara biomkartan.

**Ytblock (vit krita, torv, kalkhäll ...) kräver surface rules.** Vanilla bestämmer ytblocken i `SurfaceRuleData.overworld()`.
Vi behöver en mixin som lägger till våra regler först i listan (per biom: topplager, några lager under, sluttningar).
Det här är det största tekniska frågetecknet och testas först (fas 0).

### 1.4 Grottbiomer
Vanilla lägger Dripstone och Lush Caves i `addUndergroundBiome` (djup 0.2–0.9) och Deep Dark i `addBottomBiome`.
Grottor som inte ligger i någon av dem får ytbiomen ovanför. Vi lägger till en ny `@Inject` på `addUndergroundBiome`
som delar upp det lediga klimatutrymmet (temperatur, fuktighet, kontinentalitet) mellan våra grottbiomer, så att
Dripstone och Lush Caves får ha kvar sina zoner.

En grottbiom ändrar inte heller grottornas form. Den bestämmer golv, tak och väggar (surface rules med `stone_depth`
uppåt och nedåt), features (droppsten, pooler, kristaller, rötter), mobs, ljud, partiklar och dimfärg.

### 1.5 Regler som gäller varje fas (från minnet)
- Bumpa `mod_version` (minor per fas).
- Uppdatera åldersdokumentet (`docs/ages/`), `registry_notes.json` + `_en`, `progression.json` om progressionen ändras, och kör `gen_registry.py`.
- Advancements för allt nytt, i rätt åldersflik.
- Lägg in nya generatorer i generatorkedjan (minnet `generator-chain`).
- `runGameTestServer` efter sista dataändringen. Worldgen testas på en testserver i en worktree, aldrig i huvudmappens `run/`.
- Skicka bild när det är klart (texturer och skärmdumpar från världen).

---

## 2. Material: alla block

Kolumnen **Finns** visar vad modden redan har som materialet ska bygga vidare på, inte göra en dubblett av.

### 2.1 Bergarter

| Material | Mall | Finns i världen | Ålder | Annan användning | Finns |
|---|---|---|---|---|---|
| **Slate** | deepslate | Lager i kalla berg, mest i Slate Highlands | Stone | Slate Tiles som tak som inte brinner. Skrivtavla för skrivsystemet | – |
| **Soapstone** | tuff | Ådror i bergen, Soapstone Valley/Grottos | Stone (flinta biter) | Gjutformar för brons som tål flera gjutningar. Grytor, oljelampor | – |
| **Marble** | quartz | Djupt nära Limestone, Marble Gorge | Bronze (chisel) | Statyer och altare, huggs med Bronze Chisel | – |
| **Chalk** | sandstone | Vita klippor vid kusten, Chalk Downs | Stone | Vitt pigment, kalkmålning, flinta-nodulärer, källa till kalk | `chalk`, `loose_chalk`, `white_pigment` |
| **Shale** | tuff | Lager i sedimentbiomer | Stone | Krossas till lera. Senare: Oil Shale som bränsle | – |
| **Alum Shale** | tuff | Svarta lager i kalla biomer, Galena Seams | Iron | Alun: betningsmedel vid färgning, snabbare garvning | – |
| **Quartzite** | tuff | Bergskedjor, Crystal Geodes | Bronze | Slipstenar (Whetstone), skär som flinta | – |
| **Gneiss** | stone | Gamla berg överallt i norr | Stone | Ren byggsten | – |
| **Porphyry** | tuff (röd + grön) | Sällsynta klumpar | Iron | Lyxsten: gravstenar, troner, pelare | – |
| **Rapakivi Granite** | stone | Ersätter granit i vissa biomer | Bronze | Kvarnstenar till vind- och vattenkvarnen | – |
| **Dolomite** | tuff | Tillsammans med Limestone | Iron | Alternativt Flux, kalk | – |
| **Basalt** (ytvarianten) | basalt | Pelare vid kusten, Basalt Coast, Magma Vents | Stone | Tunga vikter, ett städ i sten | – |
| **Pumice** | tuff | Vulkaniskt, Magma Vents | Bronze | Slipmedel: putsar rustning och polerar sten. Flyter | – |
| **Scoria** | basalt | Vulkaniskt | Iron | Filter för vatten och bränsle | – |
| **Serpentine** | tuff | Sällsynt, grönt | Bronze | Inläggningar i smycken (ädelstenssystemet) | – |
| **Jasper** | tuff | Små klumpar | Stone | Tändsten (gnista mot stål) | `jasper`-ädelstenen, `loose_jasper` |
| **Conglomerate** | tuff | Flodbankar, Glacial Moraine | Stone | Krossas till grus och sand | – |
| **Travertine** | sandstone | Vid källor, Limestone Karst/Caverns | Iron | Kalkkälla, terrasser | – |
| **Gypsum** | sandstone | Torra biomer, Gypsum Flats, Salt Caves | Bronze | Gips och puts | – |
| **Alabaster** | quartz | Inuti Gypsum | Iron | Genomskinliga lampor, statyer | – |

### 2.2 Jord, lera och ytmaterial

| Material | Mall | Finns i världen | Ålder | Annan användning | Finns |
|---|---|---|---|---|---|
| **Peat** | bricks (Peat Bricks) | Myrar, Peat Bog, Peat Hollows | Stone | Bränsle som brinner länge. Torvrök för att röka mat | – |
| **Turf / Sod** | eget block + slab/stairs | Gräsytor, skärs loss med spade | Stone | Torvtak och torvväggar som håller värme (årstider) | – |
| **Kaolin** | bricks (vitt kakel) | Sällsynt i jord | Iron | Vit glasyr, porslin längre fram | `fire_clay` (annan lera) |
| **Red / Yellow Ochre** | mud (färgad puts) | Lager i lerbankar, Ochre Badlands/Caves | Stone | Pigment, grottmålningar | `red_ochre`, `yellow_ochre`, `loose_*`, pigment |
| **Loess** | mud | Stora slätter | Stone | Bra åkerjord (farming) | – |
| **Mudstone / Adobe** | mud (Mud Bricks) | Flodbankar i torra biomer | Stone | Soltorkat tegel, det enklaste byggmaterialet | – |
| **Beach Pebbles** | eget block + slab | Stenstränder, Shingle Beach | Stone | Kullerstensgolv, slungstenar, kokstenar | – |
| **Fieldstone** | stone (+ Dry Stone Wall) | Lösa stenar på ytan | Stone | Kallmur utan murbruk, gränsmurar för klanmark | – |
| **Glacial Boulder** | feature (stora block) | Glacial Moraine, Shingle Beach | – | Megaliter, egna runstenar | runstenar i worldgen |
| **Shell Bank / Coquina** | sandstone | Kuster | Stone | Kalk av brända snäckskal | – |

### 2.3 Organiskt

| Material | Mall | Finns i världen | Ålder | Annan användning | Finns |
|---|---|---|---|---|---|
| **Bog Oak** | hel träfamilj (som Trees.java) | Nedgrävda stockar i Peat Bog | Stone | Svart lyxträ till möbler och vapenskaft | – |
| **Birch Bark** (näverplattor) | slab/stairs | – | Stone | Vattentätt tak, behållare, tjära | `birch_bark`, `birch_bark_roof` |
| **Reed** + **Reed Thatch** | växt + takblock | Vid vatten, Reed Delta | Stone | Halmtak (brinner fort, isolerar) | – |
| **Wattle** | panelblock | Tillverkas av käppar | Stone | Bas för Wattle & Daub | – |

### 2.4 Tillverkade material

| Material | Gjort av | Ålder | Användning |
|---|---|---|---|
| **Quicklime → Slaked Lime** | Limestone, Chalk, Dolomite eller snäckskal bränt i Kiln, sedan släckt med vatten | Iron | Kalkbruk, garvning, whitewash |
| **Lime Mortar** | Slaked Lime + sand | Iron | Krävs för att mura riktiga stenhus (se fråga 3) |
| **Lime Plaster** | Lime + sand + hår/halm | Iron | Vita putsade väggar som går att färga med pigment |
| **Wattle & Daub** | Wattle + lera + halm + dynga | Stone/Bronze | Korsvirkeshus |
| **Fired Roof Tiles** | Bränd lera i Kiln | Bronze | Tegeltak som inte brinner, i flera färger |
| **Rammed Earth** | Jord/Loess pressad i en träform | Stone | Billiga, tjocka väggar |
| **Mosaic Tiles** | Bitar av flera stenarter | Bronze | Golvmönster |
| **Mossy / Cracked / Weathered** | Väder och tid (world age) | – | Byggnader som åldras, ruiner (egen mekanik, sista fasen) |

**Ungefär antal block:** cirka 20 stenfamiljer × 8–13 + jord och organiskt ≈ **250–300 nya block**. Det är mycket i
creative-fliken och registret, se fråga 4.

---

## 3. Ytbiomer (12)

| Biom | Tar del av | Yta / block | Features | Djur och känsla |
|---|---|---|---|---|
| **Chalk Downs** | Kustnära tempererade slätter | Chalk under tunt gräs, vita klippor | Flinta-nodulärer, loose chalk | Öppet och blåsigt, får, harar |
| **Peat Bog** | Kall del av Swamp | Peat, gungfly, vattenhål | Bog Oak nergrävd, Bog Iron, tuvor, glesa tallar, hjortron | Tranor, dimma |
| **Slate Highlands** | Kalla Windswept Hills | Slate i sluttningar, Gneiss | Ljung (finns från Pine Heath), stenrösen | Mouflon, örnar |
| **Soapstone Valley** | Tempererade bergsskogar (liten) | Soapstone-ådror som syns i dagen | Gamla huggspår, rester av stenbrott | Lugnt, sällsynt |
| **Limestone Karst** | Tempererade kullar | Limestone-hällar (alvar), tunt gräs | Slukhål ned till Limestone Caverns, Travertine vid källor | Orkidéer, fjärilar |
| **Marble Gorge** | Varma berg | Marble, Limestone | Vita raviner, övergivna stenbrott (struktur) | Torrt, ekande |
| **Ochre Badlands** | Varm, torr del av Badlands | Randiga Ochre-lager, Gypsum | Loose ochre | Värme, gamar |
| **Basalt Coast** | Steniga stränder | Basaltpelare, Beach Pebbles | Pelarformationer ut i havet | Säl, valross (finns redan) |
| **Shingle Beach** | Kalla stränder | Beach Pebbles, Fieldstone | Glacial Boulders, drivved | Måsar |
| **Reed Delta** | Flodmynningar / River-kanter | Mudstone, Loess, lera | Täta vassbälten | Fåglar, fisk |
| **Glacial Moraine** | Kall taiga | Grus, Conglomerate | Stora flyttblock, ställen för runstenar | Ren, älg |
| **Gypsum Flats** | Torr öken | Gypsum, Alabaster, Rock Salt | Saltskorpa, kristaller | Hett, bländande |

---

## 4. Grottbiomer (12)

| Biom | Djup / läge | Väggar och golv | Features | Mobs och känsla |
|---|---|---|---|---|
| **Limestone Caverns** | Mellandjup, tempererat | Limestone, Travertine-terrasser | Kalkdroppsten, klara sjöar | Blindfisk, droppande ljud |
| **Salt Caves** | Under torra biomer | Rock Salt, Gypsum | Saltkristaller som droppsten | Glittrande vitt, ljust |
| **Ochre Caves** | Grunt, nära ytan | Ochre, Chalk | Grottmålningar (struktur), ritualplatser | Mystiskt, ekande |
| **Soapstone Grottos** | Mellandjup i berg | Soapstone, Slate | Huggspår, övergivna verktyg (loot) | Lugnt |
| **Glowworm Caves** | Fuktigt, under skog | Mossa, lera, rötter | Lysmaskar i taket (svagt blått ljus), hängande rötter | Naturligt ljus i Stone Age |
| **Ice Caves** | Under kalla biomer | Packed/Blue Ice, Slate | Frusna sjöar, fryst kött (loot) | Kylrum: mat ruttnar långsammare |
| **Peat Hollows** | Precis under myrar | Peat, lera | Bog Oak-rötter, bevarade fynd (gamla vapen) | Fuktigt, mörkt |
| **Bone Caves** | Grunt, i Limestone | Limestone, Bone Pile (finns) | Benrester, tänder | Grottbjörn, Cave Lion (finns). Farligt |
| **Galena Seams** | Djupt | Shale, Alum Shale | Galena-ådror (finns) | Mörkt, gnistrar |
| **Magma Vents** | Djupast, nära lava | Basalt, Scoria, Pumice, Obsidian | Heta källor, svavel | Hettan skadar utan skydd |
| **Crystal Geodes** | Djupt, sällsynt | Quartzite | Stora geoder med ädelstenar (gemstone-systemet) | Klingande ljud |
| **Flooded Caves** | Under kust och sjöar | Lera, grus, Beach Pebbles | Vattenfyllda gångar | Pike, Sturgeon (finns). Dykning |

Nya mobs (blindfisk, tranor, måsar, gamar, lysmaskar) är **inte med** i den här planen. De listas här bara som
idéer och får en egen plan om du vill ha dem.

---

## 5. Faser

Varje fas är en egen version och ska fungera för sig.

| Fas | Innehåll | Varför i den ordningen |
|---|---|---|
| **0. Spik (teknik)** | Surface rules-mixin och grottbiom-mixin med en enda testbiom var. Ingen ny gameplay | Det är det tekniskt osäkra. Om det inte fungerar ändras resten av planen |
| **1. Stengeneratorn** | `gen_stone_families.py` + `StoneFamilies.java`, Limestone flyttas in. Slate, Gneiss, Chalk, Soapstone, Shale, Conglomerate | Mallen allt annat bygger på. Stone Age-stenarna först |
| **2. Kalk och murbruk** | Dolomite, Travertine, Quicklime/Slaked Lime, Lime Mortar, Lime Plaster, Shell Bank | Ger Limestone och Chalk en riktig användning i Iron Age |
| **3. Myr och jord** | Peat, Turf, Bog Oak, Reed + Thatch, Mudstone/Adobe, Loess, Rammed Earth, Wattle & Daub | Stone Age-byggande |
| **4. Ytbiomer del 1** | Peat Bog, Chalk Downs, Limestone Karst, Reed Delta | Använder fas 1–3 |
| **5. Grottbiomer del 1** | Limestone Caverns, Salt Caves, Bone Caves, Glowworm Caves | Använder det som finns + fas 2 |
| **6. Övriga bergarter** | Marble, Quartzite, Porphyry, Rapakivi, Basalt, Pumice, Scoria, Serpentine, Jasper, Gypsum, Alabaster, Alum Shale, Kaolin, Ochre-block | Bronze/Iron Age-material |
| **7. Ytbiomer del 2** | Slate Highlands, Soapstone Valley, Marble Gorge, Ochre Badlands, Basalt Coast, Shingle Beach, Glacial Moraine, Gypsum Flats | Behöver fas 6 |
| **8. Grottbiomer del 2** | Ochre Caves, Soapstone Grottos, Ice Caves, Peat Hollows, Galena Seams, Magma Vents, Crystal Geodes, Flooded Caves | Behöver fas 6 |
| **9. Användningar** | Soapstone-formar, kvarnstenar, slipstenar, Pumice-polering, alun vid färgning, Peat som bränsle, kylrum i Ice Caves, värmeskada i Magma Vents | Varje användning är egen gameplay och testas för sig |
| **10. Åldrande** | Mossy/Cracked/Weathered via world age, Mosaic Tiles, Fired Roof Tiles | Kräver världsålderns mekanik från säsongsplanen |

Delar av fas 1, 3 och 6 kan agenter göra parallellt i worktrees när generatorn från fas 1 finns.

### Fas 0: resultat

Spiken fungerar, och **ingen mixin behövs för ytblocken**. I 26.3 är surface rules data: registret
`worldgen/material_rule`, med `minecraft:overworld` som rot (vanillas `SurfaceRuleData` finns inte längre). Vi skriver
egna regler i `mechanical:overworld` och ersätter vanillas rotfil med samma sekvens plus en rad `"mechanical:overworld"`
direkt efter malmådrorna. Våra regler vinner då över vanillas yta och deepslate, men bara inne i våra biomer
(villkoret `minecraft:biome`). Allt genereras av **`scripts/gen_world_biomes.py`**, som läser vanillas rotfil ur jarfilen.

Testbiomer: **Chalk Downs** (torra halvan av tempererade Plains i kustbandet, kontinentalitet under 0.03) med
grass_block, ett lager dirt och sedan `CHALK_STONE` (tills vidare `minecraft:calcite`, byts mot Chalk i fas 4), calcite
i branta sluttningar. **Limestone Caverns** (tempererat, mitt inne i landet, mellandjup) med golv, tak och väggar av
Limestone.

**Ny ytbiom** (tre ställen):
1. `gen_world_biomes.py`, `BIOMES`: `"namn": ("vanilla-mall", "English Name", [features att ta bort], {fält att ändra})`.
   Biomen kopierar mallen och går med i alla biomtaggar och biome modifiers som listar mallen.
2. `gen_world_biomes.py`, `MATERIALS`: `"namn": {"top": ..., "under": ..., "stone": ..., "slope": ...}` (alla valfria).
3. Klimatplats i `OverworldBiomeBuilderMixin.mechanical$shareSlot`: dela en vanilla-zon som de andra biomerna.

**Ny grottbiom** (tre ställen):
1. `BIOMES` som ovan (mallen är oftast `dripstone_caves` utan dess droppstens-features).
2. `MATERIALS`: `"namn": {"floor": ..., "ceiling": ..., "wall": ...}`. `wall` gör all sten i biomen till det blocket
   (väggar och berget runt grottan, som vanillas Sulfur Caves); utan `wall` får bara golv och tak blocket.
3. En rad i `ModBiomes.CAVES`: `new CaveSlot(NYCKEL, temperatur, fuktighet, kontinentalitet, erosion, weirdness, djup, offset)`.
   Mixinen lägger till raderna efter vanillas grottbiomer.

**Lärdom om grottbiomernas storlek:** biomvalet tar den närmaste klimatpunkten, och ytbiomerna ligger på djup 0 och 1.
En grottbiom "läcker" därför långt utanför sina intervall: utan offset tog Limestone Caverns över halva grottvärlden på
y 0–30. `offset` (straff) styr det: 0.35 ger ungefär en tredjedel av rutnätspunkterna på y 0 och en tiondel på y ±20,
0.4 nästan bara ett band kring y 0. När fler grottbiomer kommer delar de på samma utrymme, så offset blir en
sällsynthetsratt per rad. Dripstone och Lush Caves behåller sina zoner (där vinner de alltid).

Generatorkedjan: `gen_world_biomes.py` körs efter `gen_limestone_blocks.py` (och efter `gen_biomes.py`), före
`gen_block_items.py`. Den är idempotent.

---

## 6. Risker

- **Surface rules:** löst i fas 0 med data, men vi ersätter vanillas `material_rule/overworld.json`. En annan mod eller ett datapack som ersätter samma fil vinner eller förlorar mot oss beroende på laddordning.
- **Biomer blir sällsynta:** 12 nya ytbiomer delar på vanillas klimatzoner. Både nya och gamla blir ovanligare. Testa i en riktig värld efter fas 4 och fas 7.
- **Feature-ordning:** `FeatureSorter` kraschar om biomer har features i olika ordning. `gen_biomes.py` löser det med unika feature-namn. Samma regel gäller här.
- **Grottbiomer mot vanilla:** de får inte ta Dripstone/Lush Caves zoner. Grotta under ytbiomen och ovan Deep Dark måste ses över.
- **Antal block:** 250–300 nya block gör creative-fliken och registret stora (fråga 4).
- **Texturer:** omfärgade vanilla-texturer kan se lika ut. De viktigaste stenarna kan behöva egna texturer (texturbiblioteket i `Documents/Minecraft Modding/item` kollas först).
- **Prestanda:** fler ore-features per chunk gör worldgen långsammare. Mät efter fas 6.

---

## 7. Öppna frågor till dig

1. **Klimatzoner:** ska nya ytbiomer få ersätta delar av vanilla-biomer (som de fyra nuvarande), eller bara ta del av modens egna biomer?
2. **Stone Age-verktyg:** ska Slate, Soapstone, Chalk och liknande mjuka stenar kunna brytas med Flint Pickaxe, och de hårda (Quartzite, Porphyry, Marble) bara med brons?
3. **Kalkbruk som krav:** ska stenbricks av *alla* sorter kräva Lime Mortar för att craftas i Iron Age, eller ska murbruket bara ge starkare och billigare recept?
4. **Creative-fliken:** en egen flik "Building Blocks" för alla stenfamiljer, eller ha kvar allt i den vanliga fliken?
5. **Nya djur och fåglar** till biomerna: egen plan senare, eller med redan här?

**Standardval tills du säger annat (2026-11-21):** 1) ja, som de fyra nuvarande. 2) ja: mjuka stenar (Slate,
Soapstone, Chalk, Shale, Gypsum, Peat, Pumice, Conglomerate, Mudstone) med Flint Pickaxe, hårda (Quartzite, Porphyry,
Marble, Basalt, Rapakivi, Gneiss, Serpentine, Alabaster) kräver Bronze Pickaxe. 3) murbruk krävs inte; Lime Mortar ger
billigare recept (8 bricks i stället för 4). 4) egen creative-flik "Building Blocks" för stenfamiljer och byggblock.
5) egen plan senare.

---

## Fas 1: resultat (byggt)

`scripts/gen_stone_families.py` (körs direkt efter `gen_iron_ores.py`) och `StoneFamilies.java`. Limestone-familjen
flyttades in med samma id och ger exakt samma filer. Nya: **Slate**, **Gneiss**, **Chalk Block** (+ familj med `chalk_`),
**Soapstone**, **Shale**, **Conglomerate**, alla i creative-fliken **Building Blocks** (`mechanical:building_blocks_tab`).

**En ny sten = en rad på varje sida:**

```python
# scripts/gen_stone_families.py, STONES
Stone(name, template, palette, hard, chains, ore, base=None, drop="self", bands=None, own_base=True)
#  name      "slate": blocknamnen blir mallens mönster med "{}" = name
#  template  "tuff" | "deepslate" | "stone" | "sandstone" (testade) | "quartz" | "basalt" | "mud" | "bricks" (bara listade)
#  palette   5 färger mörk -> ljus; mallens texturer färgas om efter ljushet
#  hard      True = #mechanical:needs_bronze_tool (Bronze Pickaxe), False = vilken hacka som helst (Flint Pickaxe)
#  chains    [["slate", "polished_slate", "slate_bricks", ...], ...]  Bronze Chisel-stegen
#  ore       Ore(size, chance, lo, hi, count, blo, bhi, biomes): 1 klump per `chance` chunk på y lo..hi överallt,
#            `count` extra per chunk på y blo..bhi i biomes (vanilla-id eller #taggar); None = worldgen någon annanstans
#  base      basblockets id om det inte är name (Chalk: "chalk_block")
#  drop      "self" eller (item, antal): Silk Touch ger blocket; ett item utanför familjen kan pressas 2x2 tillbaka
#  bands     andra palett i ränder (Gneiss)
#  own_base  False = basblocket skrivs av ett annat skript (Limestone: gen_iron_ores.py)
Stone("soapstone", "tuff", [(74, 88, 80), ..., (160, 170, 156)], False,
      [["soapstone", "polished_soapstone", "soapstone_bricks", "chiseled_soapstone_bricks"]],
      Ore(40, 6, 0, 90, 1, 30, 160, ["#minecraft:is_mountain"])),
```

```java
// StoneFamilies.java, STONES (samma namn, mall och kedjor; MapColor, hårdhet och ljud för basblocket)
Stone.of("soapstone", "soapstone", Template.TUFF, false, MapColor.COLOR_LIGHT_GREEN, 1.2F, SoundType.TUFF,
        List.of(List.of("soapstone", "polished_soapstone", "soapstone_bricks", "chiseled_soapstone_bricks"))),
```

En ny **mall** = en `TEMPLATES`-rad i skriptet (`family(vanilla_bas, "vanilla-block ...", word=..., oregelbundna="{}_...")`)
och samma mönster i `StoneFamilies.Template`. Skriptet kollar att Java har alla mönster, och gametesten `iron_ores`
kollar att varje block Java registrerar har en loot table, rätt verktyg, rätt drop och mejselkedjan. Quartz/basalt
behöver `_pillar`/axel-hantering (Java gör `RotatedPillarBlock` för namn som slutar på `_pillar`; basalts polished/smooth
måste läggas till när den mallen används). Efter skriptet: `gen_block_items.py`, `gen_unlocks.py`, `gen_advancements.py`,
`gen_loot_format.py`, och lägg till lang (skriptet gör blocknamnen), advancement, `docs/ages/` och registry notes.

## Fas 2: resultat (byggt)

Tre nya rader i `STONES` (skriptet och `StoneFamilies.java`): **Dolomite** (tuff, med Limestone och mest i dess klippor,
krossas till Flux), **Travertine** (sandstone, under åar, träsk och slätter, taggen `mechanical:has_travertine` som
Limestone Karst ska läggas till i) och **Shell Bank** (sandstone med rosa skalränder, under stränder och hav,
`mechanical:has_shell_bank`). Kalkkedjan (`IronOres`, data `scripts/gen_lime.py` direkt efter `gen_stone_families.py`):
lime stones eller 4 Chalk i Clay Kiln vid 900° → **Quicklime**, + vattenhink → **Slaked Lime**, + Sand + Gravel → 4
**Lime Mortar**, + 2 Sand + Straw → 4 **Lime Plaster** (+ White/Black/Red/Yellow med pigment). Slaked Lime kalkar
också en Scraped Hide till Leather. Murbruket krävs inte (standardval 3): `gen_stone_families.py` skriver
`<bricks>_with_mortar` (4 + 1 Lime Mortar → dubbelt) för varje familjs 2×2-Bricks-recept och för vanillas Stone,
Deepslate, Mud Bricks och Bricks, så nya stenar får det av sig själva. Inte byggt: slab/stairs/wall för putsen, murbruk
för sandstone-familjernas Cut-block (de har inga Bricks), att Quicklime fräter i regn.
---

## Fas 6: resultat (byggt)

Tolv nya stenfamiljer (147 block), alla som en rad i `STONES` på varje sida. Mjuka (Flint Pickaxe, Stone Age-fliken):
**Pumice**, **Scoria**, **Jasper Stone**, **Gypsum**, **Alum Shale**. Hårda (Bronze Pickaxe, Bronze Age-fliken): **Marble**,
**Quartzite**, **Red Porphyry**, **Green Porphyry**, **Rapakivi Granite**, **Coastal Basalt**, **Serpentine**, **Alabaster**.
Kaolin och Ochre-blocken flyttades till fas 3 (mud/bricks-mallarna). Var de ligger och vad de ger: `docs/ages/01-stone-age.md`
och `02-bronze-age.md`.

**Namn:** `coastal_basalt` (inte `basalt`, som skulle få samma namn som vanillas Nether-basalt i creative och JEI), och
`jasper_stone` som basblock med familjen `jasper_slab`, `polished_jasper`, `jasper_bricks` ... (som Chalk: raden heter
`jasper`, `base="jasper_stone"`), eftersom `mechanical:jasper` är ädelstenen.

**Två nya mallar:**

```python
# TEMPLATES (gen_stone_families.py)
"quartz": family("quartz_block", "quartz_slab quartz_stairs quartz_bricks quartz_pillar chiseled_quartz_block smooth_quartz "
                 "smooth_quartz_slab smooth_quartz_stairs", word="quartz", chiseled_quartz_block="chiseled_{}"),
"basalt": family("basalt", "polished_basalt smooth_basalt"),
Stone("marble", "quartz", [...], True, [["marble", "marble_bricks", "chiseled_marble"], ["smooth_marble", "marble_pillar"]], Ore(...)),
Stone("coastal_basalt", "basalt", [...], True, [["coastal_basalt", "polished_coastal_basalt"]], Ore(...)),
```

```java
// StoneFamilies.Template
QUARTZ(false, "{}_slab", "{}_stairs", "{}_bricks", "{}_pillar", "chiseled_{}", "smooth_{}", "smooth_{}_slab", "smooth_{}_stairs"),
BASALT(true, "polished_{}", "smooth_{}");   // basen och polished_{} blir RotatedPillarBlock, smooth_{} ett vanligt block
```

Quartz-mallens `{}_pillar` blir `RotatedPillarBlock` (fanns redan), basalt-mallens bas (`pillarBase`) och `polished_{}` också.
Gametesten `iron_ores` kollar nu även att pelarna har `axis`.

**Texturer:** `ACCENTS` i skriptet målar mönster ovanpå omfärgningen, en rad per sten: ådror (Marble, Alabaster), korn
(Porphyry), runda fläckar med mörk kant (Rapakivi), porer (Pumice, Scoria), fläckar (Serpentine). Frö = texturens namn,
så filerna blir likadana vid varje körning.

**Worldgen:** samma `Ore`-rader som fas 1, men färre och mindre klumpar (storlek 24–56, 1 extra per chunk i biomen, Gypsum
och Coastal Basalt 2), under vanillas granit/diorit. Biomtaggarna `mechanical:has_<sten>` (t.ex. `has_marble` med varma berg,
`has_gypsum`/`has_alabaster` med öken och badlands) är tänkta att återanvändas av fas 7–8 (Marble Gorge, Gypsum Flats, Salt
Caves). Coastal Basalt har dessutom pelare på 2–6 block på Stony Shore: `worldgen/feature/coastal_basalt_columns.json`
(vanilla `block_column`, luft eller vatten), placerad i `local_modifications` via `add_coastal_basalt_columns.json`.
Alabaster och Gypsum ligger båda i `base_stone_overworld`, så den som placeras sist kan skriva över den andra; Alabaster
finns därför också utanför gipsen.

---

## Fas 3: resultat (byggt)

Java: `EarthBlocks.java` (block utan familj), `worldgen/BogOakLogs.java` (feature `mechanical:bog_oak_logs`), sju nya rader
i `StoneFamilies.java`. Data: `scripts/gen_earth.py` (körs direkt efter `gen_stone_families.py`) och raderna i
`gen_stone_families.py`. Loess plöjs i `gen_farming.py`. Spelardokumentation: `docs/ages/01-stone-age.md` "Myr och jord",
Kaolin Tiles i `02-bronze-age.md`.

**Nytt:** Peat (bränsle: Campfire 2 min, Clay Kiln 3 min 600°/850°) + Peat Bricks, Turf + slab/stairs,
Bog Oak (hel träfamilj utan träd, begravda stammar y 55–62), Reed (växer som sugar cane) + Reed Thatch + slab/stairs +
Reed Thatch Roof (BarkRoofBlock), Mudstone + Adobe Bricks, Loess (plöjs till bördighet 3) + Loess Bricks, Rammed Earth,
Wattle (panel), Wattle and Daub + Framed, Kaolin + Kaolin Clay + Kaolin Tiles (kiln 1000°), Red/Yellow Ochre Block +
Bricks. Biomtaggar: `has_peat` (Swamp, taiga; Peat Bog läggs till i fas 4), `has_reed`, `has_mudstone`, `has_loess`,
`has_kaolin`, `has_ochre` (Badlands med, för Ochre Badlands).

**De två nya mallarna** (en rad i `STONES` på varje sida, som förut):

```python
# mud: basblocket = vanilla packed mud, plus "{}_bricks" och "{}_brick_slab/_stairs/_wall" (mud bricks)
Stone("adobe", "mud", palette, False, [], None, base="mudstone")              # mudstone, adobe_bricks, adobe_brick_slab ...
# bricks: basblocket = vanilla bricks med namnet i base, plus "{}_slab/_stairs/_wall"; name = stammen utan "s"
Stone("peat_brick", "bricks", palette, False, [], None, base="peat_bricks")   # peat_bricks, peat_brick_slab ...
Stone("rammed_earth", "bricks", palette, False, [], None)                     # rammed_earth, rammed_earth_slab ...
```

```java
Stone.of("adobe", "mudstone", Template.MUD, false, MapColor.TERRACOTTA_ORANGE, 1.0F, SoundType.PACKED_MUD, List.of()),
Stone.of("peat_brick", "peat_bricks", Template.BRICKS, false, MapColor.COLOR_BROWN, 1.5F, SoundType.MUD_BRICKS, List.of()),
```

Ändring i generatorn: basblocket läggs bara i `base_stone_overworld`/`stone_ore_replaceables` när raden har `ore`
(tegel och mjuka block ska inte vara berggrund). Gametesten `iron_ores` hoppar över hackkontrollen för baser som inte
kräver rätt verktyg (Loess) och kollar Ochre-blockens 4 ockra; nya kontroller för spade, Peat som bränsle, Kaolin → Kaolin
Tiles och Turf-receptet.

**Inte gjort:** Turf som isolering (modden har ingen byggnadsisolering), dynga är ett krav i Wattle and Daub (Manure
finns). Worldgen är inte provad i en riktig värld än.

**Efteråt:** Drying Rack torkar nu också Wet Adobe Brick → Adobe Brick, Cut Peat → Dried Peat och Reed → Dried Reed
(som Reed Thatch görs av), se `DryingRackBlockEntity.TABLE` och stenålderns dokument under Drying Rack.

---

## Fas 4+7: resultat (byggt)

Alla 12 ytbiomer finns: **Chalk Downs** (nu på riktig Chalk Block), **Peat Bog**, **Slate Highlands**, **Soapstone
Valley**, **Limestone Karst**, **Marble Gorge**, **Ochre Badlands**, **Basalt Coast**, **Shingle Beach**, **Reed Delta**,
**Glacial Moraine**, **Gypsum Flats**. Spelarbeskrivning av alla biomer (också de fyra gamla): `docs/biomes.md`. Nya block:
**Beach Pebbles** + slab, **Fieldstone** + slab, stairs och **Dry Stone Wall** (`EarthBlocks.java`, `gen_earth.py`), och
featuren **Glacial Boulder** (`Boulder.Kind.GLACIAL`, radie 2–4, Gneiss/Granite/Rapakivi Granite).

**En ny ytbiom är nu fyra rader** (ingen ny kod):
1. `ModBiomes.SURFACE` (Java): `new SurfaceShare(vanilla-biom, key("namn"), temperatur, fuktighet, kontinentalitet, erosion, weirdness)`,
   null = hela axeln. Mixinen (`mechanical$share`) skär ut överlappet ur varje klimatruta som vanilla ger den biomen
   (låda minus låda); resten går tillbaka till vanilla-biomen och kan skäras igen av en senare rad.
2. `gen_world_biomes.py`, `BIOMES` (mall, namn, features att ta bort, fält som effects).
3. `MATERIALS`: blocknamn eller hela regler. Nya nycklar: `wet` (botten under vatten), `all` (allt ovanför den
   preliminära ytan, som badlands-ränderna). `pick((tröskel, block), ..., default=)` ger fläckar efter ytbruset
   (`minecraft:surface`), `bands(block)` ger lager efter höjd.
4. `EXTRA`: egna features per steg (unika namn), djur (`creatures`), `attributes` (himmel, dimma, musik), `join`/`skip`
   (moddens biome modifiers att gå med i eller lämna, utan "add_"; skip tar bort det som ärvts från mallen) och `has`
   (`mechanical:has_<material>`-taggarna, så att materialets klumpar blir vanligare där).

### Klimatplatser

Ingen biom tar en hel vanilla-zon. Rutorna är vanillas (temperatur 0 frusen .. 4 het, fuktighet 0 torrast .. 4 våtast).
Avstånd: `locate biome` från spawn i en testvärld (frö 12345), testserver i worktreen.

| Biom | Mall | Tar från | Del | Närmast från spawn |
|---|---|---|---|---|
| Chalk Downs | plains | Plains, tempererad | torra fuktighetshalvan (-0.35..-0.2), kustbandet (kontinentalitet under 0.03) | 566 |
| Peat Bog | swamp | Taiga, sval (temp 1) | fuktighet 0.2..0.3 (den våtare halvan), flack mark (erosion över 0.05) | 2547 |
| Slate Highlands | windswept_hills | Windswept Hills, sval (temp 1) | weirdness under 0 (halva) | 2572 |
| Soapstone Valley | windswept_forest | Windswept Forest, tempererad | fuktighet 0.1..0.3 och weirdness över 0 | 2670 |
| Limestone Karst | meadow | Meadow, tempererad | fuktighet -0.1..0.3 (den torrare tempererade och all sval Meadow kvar) | 1092 |
| Marble Gorge | savanna_plateau | Savanna Plateau, varm (temp 3) | fuktighet -0.35..-0.1 (en av två) | 960 |
| Ochre Badlands | badlands | Badlands | fuktighet -0.35..-0.1 (torraste och mellersta bandet kvar; Eroded/Wooded orörda) | 5747 |
| Basalt Coast | stony_shore | Stony Shore | fuktighet över 0.1 | 115 |
| Shingle Beach | beach | Beach, sval (temp 1) | fuktighet under 0.1 | 1718 |
| Reed Delta | swamp | Swamp, tempererad (det Willow Fen lämnat) | nära kusten (kontinentalitet -0.11..0.03) | 3958 |
| Glacial Moraine | taiga | Taiga, frusen (temp 0) | kuperad mark (erosion under 0.05) | 607 |
| Gypsum Flats | desert | Desert | torraste fuktighetsbandet (under -0.35) på flack mark (erosion över 0.05) | 6449 |

Vanilla i samma värld: plains 611, forest 497, taiga 595, meadow 692, stony_shore 430, windswept_hills 1825,
beach 1984, desert 2400, swamp 2905, badlands 3345, savanna_plateau 4724. De varma biomerna (Ochre Badlands, Gypsum
Flats, Marble Gorge) ligger långt bort för att fröets spawn är kall; de ligger ungefär lika långt bort som sina
vanilla-mallar. Willow Fen, Hazel Meadow, Pine Heath och Deciduous Forest har kvar sina rutor (raderna ovan rör dem inte).

**Hur sällsynta:** Basalt Coast, Chalk Downs och Glacial Moraine är vanliga (inom 600 block); Limestone Karst och Marble
Gorge medel; Peat Bog, Slate Highlands, Soapstone Valley, Shingle Beach och Reed Delta är ovanliga (2–4 km); Ochre
Badlands och Gypsum Flats finns bara där det är hett och torrt, som Badlands och Desert.

### Ytblocken (räknade i en riktig värld)

Block räknade i en 48×48-ruta (y 40–200) i varje biom med `fill ... air replace` över RCON:

| Biom | Block |
|---|---|
| Chalk Downs | Chalk Block 3385–7563, Grass Block ~1000, Loose Flint 61 |
| Peat Bog | Peat 2948–3938, Moss Block 621, vatten 1259, Pine Log 58 |
| Slate Highlands | Slate 4642–8105, Gneiss 3901–10528, Heather 4–13 |
| Soapstone Valley | Soapstone 12019 |
| Limestone Karst | Limestone 13948, Travertine 219, Grass Block 1063 |
| Marble Gorge | Marble 3014, Limestone 7303, Coarse Dirt 423 |
| Ochre Badlands | Red Ochre Block 12012, Yellow Ochre Block 3147, Gypsum 3878, Mudstone 1130 |
| Basalt Coast | Coastal Basalt 5476, Beach Pebbles 1157 |
| Shingle Beach | Beach Pebbles 427–865 (smal strand), Gravel 1637–9390, Fieldstone 38 och flyttblock (Granite, Gneiss) i 96×96 |
| Reed Delta | Mud 3274, Clay 266, Loess 2679, Mudstone 2090, Reed 45 (96×96) |
| Glacial Moraine | Gravel 3961, Conglomerate 8923, Fieldstone 62, Spruce Log 162, Gneiss/Granite/Rapakivi (flyttblock) |
| Gypsum Flats | Gypsum 8539, Rock Salt 284, Alabaster 494, Sand 1233 |

**Inte gjort / begränsningar:** slukhålen i Limestone Karst är vanillas lake-feature med luft (gropar ca 4 djupa, inga
schakt ned till grottorna). Peat Bog har Reed bara där vassen överlever (inte på torvkanter). Inga drivved, inga gamla
huggspår i Soapstone Valley och inga övergivna stenbrott i Marble Gorge (strukturer). Inga nya djur (tranor, måsar,
gamar finns redan som mobs och används). Alla nya biomer ärver sin malls biomtaggar, så vanilla-strukturer (byar,
utposter, swamp huts) kan hamna i dem.

---

## Fas 5+8: resultat (byggt)

Alla tolv grottbiomer. Spelartext per biom: **`docs/caves.md`**. Java: `CaveSlot`-rader i `ModBiomes.CAVES` (Magma Vents
och Crystal Geodes har två rader var), blocket **Glowworm** (`EarthBlocks.GLOWWORM`, en `HangingRootsBlock` med ljus 6,
ingen kollision, ingen drop; gametest i `iron_ores`) och featuren `worldgen/CaveFinds` (`mechanical:cave_paintings` i Ochre
Caves, `mechanical:tool_cache` i Soapstone Grottos, loot `chests/tool_cache`). Data: `BIOMES`/`MATERIALS`-rader i
`gen_world_biomes.py` och nya **`scripts/gen_caves.py`** (körs direkt efter `gen_world_biomes.py`), som skriver biomernas
placed features (namn `<biom>_<sak>`, konfigurerad feature inline), Glowworm-blocket och biomtaggarna (`has_<material>`,
`has_cave_hyenas`/`has_cave_lions` för Bone Caves, Pike i Flooded Caves). En advancement per biom i Wilderness-fliken
(`cave_<biom>`; de djupa efter The Cold Deep).

**Ny grottbiom nu** (tillägg till fas 0-receptet): `BIOMES`-raden använder `cave(temperatur, fuktighet, dimfärg,
feats=..., sound=..., particle=..., water=...)`, som slår ihop attributen med mallens (spawns och musik blir kvar) och
lägger till biomens features (`gen_caves.FEATURES[biom]`, `{steg: [...]}`). Glowworm Caves, Peat Hollows och Flooded Caves
saknar `wall`: där ändras bara golv och tak, och det fungerar (räknat nedan).

**Lärdomar om features i grottor:**
- Prydnader läggs som `vegetation_patch` (som vanillas Lush Caves): en lyckad träff klär en hel fläck golv eller tak.
  Enstaka block med `environment_scan` från en slumpad höjd träffar sällan (de flesta försök börjar i berget eller utanför
  biomen), därför `SCAN = 12` gånger fler försök.
- Växtdelen i en patch är en `block_column` med höjd 1, inte `simple_block`. En kolumn under ett tak måste peka **nedåt**:
  kolumnen kollar blocket efter startpunkten, så en uppåtriktad placerar inget.
- Geodernas kristaller kräver `placements_require_layer0_alternate: false` när det inte finns något budding-block.
- Räkna prydnader **före** golvet och taket med `fill ... replace`: annars ramlar de av när underlaget tas bort.

### Klimatplatser

Djup = `depth`-parametern. Uppmätt i en riktig värld är den ungefär 0.5 + (30 − y) / 128 på land (lägre under havet, högre
under berg): 0.3–0.45 ≈ y 35–55, 0.5–0.75 ≈ y 0–30, 0.8–0.92 ≈ y −20 till −5, 1.06+ ≈ under y −40. Runt depth 1 (y ≈ −30)
vinner alltid ytbiomen (den har en punkt på depth 1), och vid botten finns bara Deep Dark (låg erosion) kvar. Ingen rad går in
i Dripstones zon (kontinentalitet över 0.45 lämnas), Lush (fuktighet över 0.35) eller Deep Dark (erosion under −0.3 längst ned).
Offset är raritetsratten; lika offset betyder att lådorna ensamma delar upp utrymmet.

| Biom | Temperatur | Fuktighet | Kontinentalitet | Erosion | Weirdness | Djup | Offset |
|---|---|---|---|---|---|---|---|
| Ice Caves | −1 – −0.5 | −1 – 0.3 | −0.11 – 0.45 | alla | alla | 0.3–0.45 | 0.12 |
| Peat Hollows | −0.45 – −0.15 | 0.1 – 0.35 | −0.11 – 0.45 | alla | alla | 0.3–0.45 | 0.1 |
| Glowworm Caves | −0.1 – 0.5 | 0.1 – 0.35 | 0.03 – 0.45 | alla | alla | 0.3–0.45 | 0.1 |
| Bone Caves | −0.4 – 0.15 | −0.6 – 0 | 0.03 – 0.45 | −0.2 – 0.6 | alla | 0.3–0.45 | 0.12 |
| Ochre Caves | 0.3 – 1 | −1 – −0.1 | 0.03 – 0.45 | alla | alla | 0.3–0.45 | 0.1 |
| Flooded Caves | −0.45 – 0.55 | −0.35 – 0.3 | −0.4 – −0.11 | −0.375 – 0.45 | −0.3 – 0.3 | 0.35–0.55 | 0.3 |
| Limestone Caverns | −0.1 – 0.3 | −0.2 – 0.2 | 0.03 – 0.45 | alla | alla | 0.5–0.75 | 0.13 |
| Salt Caves | 0.35 – 1 | −1 – −0.3 | −0.11 – 0.45 | alla | alla | 0.5–0.75 | 0.1 |
| Soapstone Grottos | −1 – −0.3 | −1 – 0.3 | 0 – 0.45 | −1 – 0 | alla | 0.5–0.75 | 0.1 |
| Galena Seams | −0.45 – 0.1 | −0.35 – 0.3 | −0.11 – 0.45 | −0.3 – 0.45 | −0.45 – 0.45 | 0.8–0.92 | 0.1 |
| Magma Vents | 0.35 – 1 | −0.6 – 0.3 | −0.11 – 0.45 | −0.3 – 1 | alla | 0.8–0.92 och 1.06–1.5 | 0.1 |
| Crystal Geodes | −0.2 – 0.35 | −1 – 0.35 | 0.03 – 0.45 | −0.3 – 1 | 0.6 – 1 | 0.8–0.92 och 1.06–1.5 | 0.12 |

### Täckning (riktig värld, seed 12345)

Rutnät 33×33 punkter med 256 blocks mellanrum (8192×8192 block), biomen på sju höjder (`execute if biome`), andel av
punkterna i procent. "Övrigt" = ytbiomen under jord (som i vanilla). Vanilla = samma värld utan våra rader.

| y | Vanilla: Dripstone / Lush / Deep Dark / övrigt | Med grottbiomerna |
|---|---|---|
| 55 | 5.6 / 1.7 / 0 / 92.7 | övrigt 77.9, Glowworm 5.5, Dripstone 3.7, Ice 3.1, Ochre 2.6, Bone 2.4, Soapstone 1.6, Peat 1.5 |
| 40 | 10.0 / 3.3 / 0.1 / 86.6 | övrigt 44.4, Glowworm 11.7, Ice 10.5, Bone 10.0, Dripstone 6.5, Ochre 5.5, Peat 4.2, Soapstone 2.8, Limestone 2.7, Lush 1.5 |
| 20 | 12.9 / 6.2 / 0.5 / 80.5 | övrigt 27.7, Ice 13.9, Bone 12.2, Glowworm 12.0, Peat 6.7, Dripstone 6.1, Limestone 6.0, Ochre 5.4, Soapstone 5.1, Lush 2.4 |
| 0 | 14.5 / 10.4 / 1.6 / 73.6 | Limestone 13.8, Soapstone 12.3, övrigt 11.0, Ice 10.8, Glowworm 10.5, Flooded 8.4, Peat 8.2, Bone 5.9, Dripstone 5.3, Salt 3.7, Lush 3.0, Ochre 2.8 |
| −20 | 8.7 / 8.3 / 3.0 / 80.0 | Limestone 14.8, Soapstone 13.3, Flooded 12.9, övrigt 12.5, Galena 10.0, Ice 7.4, Magma 5.6, Dripstone 4.8, Salt 4.4, Peat 4.0, Lush 3.5, Deep Dark 3.0, Geodes 1.3 |
| −40 | 3.6 / 3.0 / 7.0 / 86.4 | övrigt 38.0, Galena 25.9, Magma 9.6, Soapstone 6.5, Deep Dark 6.3, Flooded 3.6, Dripstone 3.1, Lush 2.5 |
| −55 | 1.1 / 1.2 / 11.3 / 86.4 | övrigt 74.9, Deep Dark 9.4, Galena 6.5, Magma 4.7, Geodes 1.2, Lush 1.2, Dripstone 1.1 |

Medel över de sju höjderna: Dripstone i vanilla 8.1 %. Våra: Ice 6.6, Galena 6.3, Soapstone 6.0, Glowworm 5.9, Limestone 5.7,
Bone 4.4, Flooded 3.6, Peat 3.5, Magma 3.1, Ochre 2.4, Salt 1.6, Crystal Geodes 0.5: alla mindre än Dripstone. Topparna på en
höjd ligger kring Dripstones topp (14.5 %), utom **Galena Seams på y −40 (26 %)**, där den delar botten bara med Magma Vents och
ytbiomerna. Dripstone, Lush och Deep Dark har kvar sina kärnzoner, men **Dripstone och Lush tappar mycket yta** (y 0: 14.5 → 5.3
och 10.4 → 3.0), eftersom deras läckage utanför zonerna nu går till våra biomer; Deep Dark är nästan oförändrad. Ytbiomernas
grottor (övrigt) går från 74–93 % till 11–78 %. Vill man ha kvar mer vanilla: höj offset på raderna.

`locate biome` från 0,0 (alla hittas): Limestone 426, Salt 2831, Ochre 636, Soapstone 559, Glowworm 321, Ice 891, Peat 300,
Bone 730, Galena 540, Magma 740, Crystal Geodes 214, Flooded 2130; Dripstone 602, Lush 64, Deep Dark 192.

**Block i en biom** (48×48 block, ±40 höjd runt en punkt i biomen, `fill ... air replace`): Limestone Caverns 66 145
Limestone, 2 474 Travertine, 457 Limestone Wall (droppsten), 5 390 vatten; Salt Caves 35 134 Gypsum, 1 827 Rock Salt, 24
Alabaster; Ochre Caves 56 159 Chalk Block, 1 884 Red och 794 Yellow Ochre Block, 105 Loose Ochre, 8 Cave Painting; Soapstone
Grottos 79 485 Soapstone, 16 522 Slate, 2 kistor; Glowworm Caves 1 452 Moss Block, 162 Rooted Dirt, 39 Glowworm, 50 532 vanlig
sten (ingen `wall`); Ice Caves 41 783 Slate, 1 030 Packed Ice, 544 Blue Ice, 805 Ice, 399 Snow Block; Peat Hollows 5 238 Peat,
970 Bog Oak Log, 713 Hanging Roots (81×81 block); Bone Caves 77 101 Limestone, 441 Coarse Dirt, 119 Bone Pile, 88 Bone Block;
Galena Seams 46 526 Shale, 4 279 Alum Shale, 594 Galena Ore; Magma Vents 26 543 Coastal Basalt, 889 Scoria, 283 Pumice, 69
Obsidian, 358 Magma Block, 194 lava; Crystal Geodes 19 306 Quartzite, 342 Calcite, 127 Polished Quartzite, 171 Amethyst Block,
68 kristaller av fem ädelstenar; Flooded Caves 1 895 Gravel, 4 510 Clay, 14 139 vatten.

**Inte gjort:** fas 9-effekterna (kyla i Ice Caves, hetta i Magma Vents). Cave Lion står i Bone Caves taggar men dess
spawnregel kräver ljus, så lejon dyker i praktiken inte upp i mörka grottor. Pike i Flooded Caves bara nära havsytan
(vattendjurens regel). Cave Bear finns i alla grottbiomer, eftersom mallen dripstone_caves står i `add_cave_bear`. Beach
Pebbles i Flooded Caves (ytagenten gör blocket parallellt; Gravel och Clay används nu). Dev-servern fick OutOfMemory eller
watchdog när 1 089 tvångsladdade chunks lastades ur efter täckningsmätningen (mätverktyget, inte modden).

## Fas 9C: resultat (byggt)

Miljöeffekterna från grottbiomerna, plus torvrök. Gametest **`environment`** (`EnvironmentTest.java`) testar varje regel.

**Grottluft** (`Temperature.cave(biom, y)`, används av `ambient`): under y 50 utan himmel gäller djupet som förut, men
**Ice Caves = 2** (nära fryspunkten, på alla djup) och **Magma Vents = 30** (MAX är 20). Eftersom `ambient` också styr
grödor, Fertility-torka, ugnarnas omgivningsvärme och kylrummen följer de med.

**Hetta** (`Temperature`): `OVERHEAT = 17`, `SCORCHING = 19`. Över 17 food exhaustion (0,5 × (kropp − 17) / 3 per sekund,
som kylans hunger), över 19 en skada varannan sekund (`onFire`, så Fire Resistance skyddar). `heatCap`: bara luft på minst
30 (Magma Vents) kan värma kroppen över 17; sol, Nether och eldar stannar vid 17, så öknar och lägereldar blir inte farliga
(förut kunde kroppen nå 20 där utan effekt). **Skydd: vatten eller regn** håller kroppen vid 17. Kläder spelar ingen roll i
hettan (isoleringen gäller bara kyla), för att hålla det enkelt. HUD: röda termometrar (`hot_full`/`hot_half`) över 17.

**Kylrum** (`Preserving.coldRate`): luft under 8 → mat åldras i halv fart, under 3 eller med is (`#minecraft:ice`) direkt
bredvid behållaren → en fjärdedel. Gäller stängda **kistor och tunnor** (stämplas med `BASKET_SINCE` när de stängs i kylan och
räknas om när de öppnas, som Bark Basket), **Storage Jar** och **Root Cellar** (deras egen takt × kylan, `sealedRate`) och
**Bark Basket** (kylan där den öppnas). Kylan bedöms när behållaren öppnas (samma genväg som Root Cellar). En stämpel som en
hopper drar ut ur en kall kista tas bort i spelarens inventory utan kylbonus.

**Torvrök** (`SmokingRackBlock.peatSmoke`): Peat eller Dried Peat som matas till en brinnande Campfire ger den torvrök så
länge torven brinner (`mechanical$addPeat`, nytt fält `mechanical_peat` i campfire-mixinen, räknas ned med elden, nollas vid
glöd). En Smoking Rack ovanför (1–2) eller bredvid röker då **dubbelt så fort** (`PEAT_SPEED = 2`: 2 min i stället för 4).

**Advancements** (från Java): **Cold Store** (Wilderness, efter Ice Caves: öppna en kista/tunna vars mat hållits i
fjärdedelsfart), **Hot Spring** (Wilderness, efter Magma Vents: stå i vatten i Magma Vents), **Peat Smoke** (Food, efter
Smoked).

**Inte gjort:** Glowworm Caves som "upplyst" för nattens faror: NightDangers räknar bara eldar (inget ljusvärde), så det var
inte trivialt. Ingen egen skadetyp för värmeslag (vanilla `on_fire`, dödsmeddelandet "burned to death").

---

## Fas 9B: resultat (byggt)

Kemi, skrift och puts. Java: **`Chemistry.java`** (items, `SlateTabletItem`, krossningen i `Chemistry.ground` som
`Pigments.ground` anropar, gametest `chemistry`), två rader i `KilnBlockEntity.RECIPES` och två injects i
`ServerGamePacketListenerImplMixin`. Data och texturer: **`scripts/gen_chemistry.py`** (efter `gen_lime.py` och
`gen_pigments.py`). Spelartext: `docs/ages/03-iron-age.md` "10. Alun" och `02-bronze-age.md` "Gips och Slate Tablet".

| Användning | Hur | Ålder |
|---|---|---|
| **Alun** | Alum Shale i Clay Kiln vid 800° (Charcoal, ingen Bellows) → Roasted Alum Shale, + vattenhink (kommer tillbaka) → **2 Alum** | Iron |
| **Betningsmedel** | 2 pigment + Alum → **4 dye** (Black, Red, Yellow, White; utan alun 1 pigment → 1 dye). Modden har ingen egen färgning av ull/läder, bara vanillas dye-recept, så alunet ger mer färg per pigment | Iron |
| **Alungarvning** | 2 Scraped Hide + Alum → **2 Leather** (Salt och Slaked Lime: en hud var) | Iron |
| **Gips** | Gypsum på Stone Anvil → 2 Gypsum Powder, Clay Kiln 300° → **Plaster of Paris**, + 2 Sand + Straw → **4 Lime Plaster** (samma block och färger som kalkputsen) | Bronze |
| **Slate Tablet** | Polished Slate + 2 Stick → Slate Tablet: bokskärmen med **en sida**, kräver Chalk i andra handen (1 per gång), sneak-use torkar av, texten i tooltipen, Sign sparar bara sidan (mixin) | Bronze |
| **Pigment av block** | Chalk Block, Red/Yellow Ochre Block på Stone Anvil → 8 pigment (2 per ockra/krita blocket görs av) | Bronze |

**Nya id:** `roasted_alum_shale`, `alum`, `gypsum_powder`, `plaster_of_paris`, `slate_tablet`. Recept: `alum`,
`<färg>_dye_from_<färg>_pigment_with_alum` (4), `leather_from_alum`, `lime_plaster_from_plaster_of_paris`, `slate_tablet`.
Advancements: Iron Age *Alum* → *Mordant*, *Tawed*; Bronze Age *Plaster of Paris*, *Slate and Chalk*.

**Inte gjort:** Scoria-filter (modden har inget smutsigt vatten, ingen törst och ingen rening; Kettle kokar mat, inte
vatten), eget Gypsum Plaster-block (gipset blir i stället en väg till den befintliga putsen), gipsformar för gjutning
(del A har formarna), grönt pigment av Serpentine och svart av Alum Shale (det finns inget grönt pigment eller grön
grottfärg att mata, och svart finns redan av kol och sot). Slate Tablet går inte att sätta upp som skylt.

---

## Fas 9A: resultat (byggt)

Stenhantverket: Java i **`StoneCrafts.java`** (block, Pumice Chunk, gametest `stone_crafts`), data och texturer i
**`scripts/gen_stone_crafts.py`** (körs efter `gen_stone_families.py`, `gen_copper.py`, `gen_bronze*.py`, `gen_fauna_coast.py`
och `gen_moldbench.py`, eftersom den kopierar deras modeller). Spelartext: `docs/ages/01-stone-age.md` "Pumice och krossad sten",
`02-bronze-age.md` "Stenhantverk", `03-iron-age.md` "Jaspis som tändsten".

| Sten | Användning | Hur |
|---|---|---|
| Soapstone | 21 gjutformar `soapstone_<form>_mold` som aldrig spricker | `ClayMoldBlock` med `cracks = false`; skärs i Mold Bench av Polished Soapstone (`StoneCrafts.moldRecipes`, efter lerformarna i `MoldBench.ALL`). Modellerna är lerformens med täljstenssidor och omfärgad ovansida; metallfyllningens texturer delas |
| Quartzite | `quartzite_whetstone`: 2 drag i stället för 4 | `WhetstoneBlock(p, bite = 2)` |
| Rapakivi Granite | `rapakivi_quern`: 2 klick per Flour | `QuernBlock(p, step = 2)` (halvt varv per klick) |
| Coastal Basalt | `basalt_anvil`: krossar på 2 slag | Samma `StoneAnvilBlock` och block entity-typ som Stone Anvil; `StoneAnvilBlockEntity.hitsNeeded()` |
| Pumice | `pumice_chunk` (krossat Pumice, 16 användningar) polerar grov sten | Bronze Chisels steg om det leder till ett `polished_`/`smooth_`-block (`StoneCrafts.polished`) |
| Conglomerate, Shale | Stone Anvil: 2 Gravel + 1 Flint, 2 Clay Ball | `StoneCrafts.crushed`/`crushedExtra` (från `BronzeOres.crushed` och anvilens slag) |
| Jasper | Rough/Polished Jasper + Steel Plate → Flint and Steel | recept `flint_and_steel_from_jasper` |
| Alabaster | `alabaster_lamp`, ljus 15 | `OilLampBlock` med alabastertextur och eget ljus |

Advancements: *Rubbed Smooth* (Stone Age), *Stone That Remembers*, *Rubble*, *Heavy Stone*, *Hard Grit*, *Millstone*, *Glowing Stone*
(Bronze Age), *Red Spark* (Iron Age). *Rubbed Smooth* och *Rubble* ges från Java.

**Inte gjort:** vind- och vattenkvarn (finns inte i modden; Rapakivi blev handkvarnens stenar). Pumice som putsar rustning (ingen
mekanik att haka i utan att hitta på reparation). Marble-statyer och piedestaler (egna modeller, inte billigt). Porphyry och
Serpentine som inläggningar (ring- och charm-platserna har inga smycken än). Täljstensgrytor och -oljelampor. Täljstensformarna
syns inte i registrets receptlista (gen_registry.py läser bara `StationRecipe.of`-anrop rad för rad; formarna görs i en loop),
men registry notes beskriver dem under `soapstone_ingot_mold`.
