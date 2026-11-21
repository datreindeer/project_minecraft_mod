# Plan: byggmaterial och nya biomer (ovan och under jord)

Status: **fas 1 byggd** (stengeneratorn + sex Stone Age-bergarter, se "Fas 1: resultat" sist), **fas 6 byggd** (övriga bergarter utom Kaolin och Ochre, se "Fas 6: resultat"), resten plan. Skriven 2026-11-21.

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

---

## 6. Risker

- **Surface rules:** vanilla har inget API för att lägga till regler. Mixin på `SurfaceRuleData` kan krocka med andra moddar. Fas 0 visar om det fungerar.
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
