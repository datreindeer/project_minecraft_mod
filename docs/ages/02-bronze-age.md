# Bronsåldern

> Status: **hela bronsåldern är byggd**: fas 1 (malm, tvättat koncentrat), fas 2 (värme), fas 3 koppar och fas 3b brons
> (legering, bronsverktyg med spjut, svärd och skära, Bronze Saw, Bronze Helmet + Greaves, byhandel nivå 3–4 med Copper Coin), Smelting
> Furnace, odlingens fas 3 (Quern, bröd, kompost, Manure, Scarecrow, Barley), keramik, Bronze Kettle, fler bronsverktyg,
> vagn och häst, och till sist **Bloomery** (järnlupp, Bronze Anvil, smidesjärn), se avsnitten sist. Kvar (valfritt):
> bronslykta. Beslut står under Beslut. Fältredskapen (Seed Bag, Bronze Ard) och den snabbare växttiden: se *Fältredskap*.

Andra åldern. Målet är att gå från sten till metall: hitta koppar och tenn, få fram tillräcklig värme, smälta och gjuta,
legera brons och smida verktyg och rustning. Åldern slutar när spelaren kan bygga det som behövs för järn
(en Bloomery av eldfast tegel och bälgar, och en bronshacka som klarar järnmalm).

Stenåldern lämnar över: **Clay Kiln**, **Crucible**, **Charcoal**, **Tin Ore** (finns redan i världen), Raw Hide/Leather,
Sinew, Tool Station och Leatherworking Bench.

## Progression i korthet (förslag)

1. **Leta malm.** Ytfynd visar var malmen finns: små gröna **Malachite**-stenar och **Native Copper**-klumpar bland de
   lösa stenarna ovanför kopparådror. **Cassiterite** (tennsten) ligger i grus vid bäckar och floder och måste vaskas fram.
2. **Bryta.** Kopparmalm kräver Stone Pickaxe och ger **Raw Copper Chunks** (små, inte hela tackor). Tenn kommer mest från
   vaskning: **Pan** + grus/sand i vatten → Cassiterite.
3. **Krossa och rensa.** Krossa malmen med hammarsten på en **Stone Anvil** (platt sten) → **Crushed Copper Ore**.
   Vaska → renare koncentrat (mer metall per smältning).
4. **Värme.** Clay Kiln räcker inte för metall. Uppgradera den med **Bellows** (läder + trä) och kör på Charcoal. Kilnen
   får en **temperatur** som stiger med bälgen och sjunker utan den. Koppar smälter först vid hög värme.
5. **Smälta och gjuta.** Lägg malm i en **Crucible** i den heta kilnen → smält metall i deglen. Häll i **Clay Molds**
   (formas av Clay Balls i **Mold Bench**, bränns i kilnen): ingot, yxhuvud, hackhuvud, kniv, spjutspets, sågblad, svärd.
6. **Legera brons.** Ungefär 9 delar koppar och 1 del tenn i samma degel → **Bronze**. Fel blandning ger svagare metall.
7. **Montera.** Gjutna huvuden (kanske slipade på en **Whetstone**, se öppna frågor) sätts ihop i Tool Station med
   skaft och binding, som stenverktygen.
8. **Rustning (bara delvis).** Gjut **Bronze Helmet** och **Bronze Greaves** (benskydd), slipa dem och sätt ihop i
   Armor Station. Ingen bröstplåt: bålen skyddas fortfarande av fur/läder. Full plåtrustning kommer med järnet.
9. **Mot järnet** (byggt, se *Bloomery*). Fire Clay → eldfast tegel → **Bloomery**, som behöver bälgar och ger en
   järnlupp som smids till smidesjärn på ett Bronze Anvil. Järnmalm kräver en bronshacka.

## Nytt innehåll (förslag)

| Sak | Typ | Syfte |
|---|---|---|
| Malachite, Native Copper (ytfynd) | Lösa block | Visar var kopparådror finns. Native Copper kan hamras direkt till lite koppar |
| Cassiterite | Item | Tennmalm från vaskning (och Tin Ore) |
| Pan | Verktyg | Vaska grus och sand i vatten |
| Stone Anvil | Block | Krossa malm, kallhamra |
| Bellows | Block | Höjer kilnens temperatur |
| Mold Bench | Block | Station där alla lerformar och Unfired Crucible formas av Clay Balls (från stenåldern) |
| Clay Molds (7 sorter) | Items | Gjutformar, formade i Mold Bench och brända i kilnen. Kan gå sönder efter några gjutningar |
| Crude Copper / Crude Tin Ingot, Copper Ingot, Bronze Ingot | Items | Användarens texturer finns (workshop/) |
| Tin Nugget, Bronze Nugget | Items | 9 ↔ 1 tacka (Poor Bronze-korn blir en Poor Bronze-tacka och tvärtom). Resten under en tacka när tenn/brons knackas ur ett tråg, 18 ur Iron Nugget Mold, 22 mB i High Furnace. Degeln och Smelting Furnace tar inte korn (22 mB är mindre än en quarter): gör en tacka först (v0.209.0, docs/metals.md avsnitt 15) |
| Copper Bloom, Bronze Dust, Bronze Plate | Items | Mellanprodukter (texturer finns) |
| Copper- och Bronze-verktyg | Verktyg | Yxa, hacka, spade, hacka (hoe), kniv, spjut, svärd, såg |
| Bronze Helmet, Bronze Greaves | Rustning | Gjuts, slipas, Armor Station. Ingen bröstplåt eller boots |
| Basic Smithing Hammer, Basic Binding | Verktyg/item | Texturer finns |
| Whetstone | Block/verktyg | Slipa gjutna huvuden |
| Saw | Verktyg | Planks från logs (den öppna punkten från stenåldern) |
| Bronze Fire Grate | Item | Smids av 3 Bronze Ingot på Bronze Anvil. I Fire Pit och Clay Kiln: längre brinntid, mindre rök och sot, Charcoal i Fire Pit |

## Vad bronsen låser upp (förslag)

- **Saw:** fler planks per log. Utan såg fortsätter det vara 1–2 sticks.
- **Bronze Knife:** snabbare styckning, klarar björn.
- **Byar:** handelsnivå 3 öppnas (bronsålderns varor). Smedjan i byn blir "lite" användbar.
- **Lantern / ljus:** en bronslykta som skydd mot horder (eld utan lägereld)?

## Beslut (2026-11-19)

- **Malm:** ytfynd + vaskning. Malachite/Native Copper på marken över kopparådror, Cassiterite vaskas ur grus vid vatten.
- **Bearbetning:** gjutning i lerformar (Crucible → Clay Mold). Även rustningsplåt gjuts (plåtform); ingen lupp/hamring
  i bronsåldern, det sparas till järnet.
- **Värme:** temperatur + bälg. Kilnen får en temperaturmätare, bälgen höjer den, utan bälg sjunker den. Koppar kräver
  hög värme som måste hållas uppe.
- **Koppar-tier före brons:** ja. Kopparverktyg är ett mjukt mellansteg innan man har tenn. Ingen kopparrustning.
- **Rustning:** bara Bronze Helmet och Bronze Greaves (benskydd). Ingen bröstplåt; full rustning i järnåldern.
- **Bränsle:** Charcoal (från kilnen) är bronsålderns bränsle. Coal Ore kräver Bronze Pickaxe; stenkol hör till
  järnålderns smide/koks. Iron, Coal och Lapis Ore ligger i taggen `mechanical:needs_bronze_tool`.
- **Inte nu:** modulära verktyg (huvud/skaft/binding som egna delar med stats). (Jordbruket kom till slut med: se
  Odling nedan. Att tämja hästar kom också: se Vagn och häst.)

- **Temperatur:** realistiska grader. Lera bränns vid 600°, brons smälter vid ~950°, koppar vid 1085°. Varje bälgpump
  höjer en bit; utan pump sjunker temperaturen långsamt (pumpa några gånger per minut under smältning).
- **Clay Mold:** håller ungefär 5 gjutningar, sedan spricker den.
- **Efter gjutning:** huvuden kommer ut som **Rough Cast ...** och slipas på en **Whetstone** (några högerklick) innan de
  kan monteras i Tool Station.

## Faser

1. **Malm** (fas 1, **byggd**): ytfynd, vaskning, krossning. Se nedan.
2. **Värme** (**byggd**): kilnens temperatur, Bellows, mätare i GUI.
3. **Gjutning:** smält metall i Crucible, Clay Molds, ingots, legering till brons, tvättat koncentrat (**byggt**).
4. **Verktyg och rustning:** kopparverktyg, Whetstone, bronsverktyg, Bronze Saw, Bronze Helmet + Greaves, byhandel nivå 3
   (**byggda**, se Fas 3b).
5. **Smelting Furnace** (sen bronsålder, **byggd**, se Fas 3c): 3×3×3 ugn av Furnace Bricks med Furnace Chute och Furnace Tap,
   Clay Channels till formarna, 40 enheter, +25 % metall per malm.
6. **Bloomery** (slutet av bronsåldern, **byggd**, se *Bloomery*): Fire Brick, 1×1×3-schakt, järnlupp vid 1350°,
   Bronze Anvil och Bronze Smithing Hammer, Wrought Iron Ingot.

## Fas 1: malm (byggd)

Allt nedan finns i spelet. Kod: `BronzeOres.java` (registrering), `PanItem`, `StoneAnvilBlock` + `StoneAnvilBlockEntity`,
`worldgen/OreIndicators`, `client/AnvilRenderer`. Data: `scripts/gen_bronze_ores.py`, texturer: `scripts/gen_bronze_ore_textures.py`.

### Ytfynd (worldgen)

Featuren `mechanical:ore_indicators` körs en gång per chunk i vegetationssteget (efter `underground_ores`, så malmen
finns redan). Den provar **6 slumpade kolumner i sin egen chunk** och letar nedåt från ytan upp till **64 block** efter
Copper Ore / Deepslate Copper Ore (taggen `minecraft:copper_ores`) eller Tin Ore. Den läser och skriver bara inom
chunken. Vid träff läggs **1–3 ytfynd** inom 2 block från kolumnen (klämt till chunken), på fast mark där det är luft
eller lågt gräs (inte vatten, inte halva höga växter). Storlek 1 (75 %) eller 2 (25 %).

| Block | Över | Ger |
|---|---|---|
| Loose Malachite | kopparmalm (85 % av fynden) | Malachite |
| Loose Native Copper | kopparmalm (15 %) | Native Copper |
| Loose Cassiterite | Tin Ore | Cassiterite |

Ytfynden är samma sorts block som Loose Rocks (`RockBlock`): plockas för hand med ett slag, faller om marken under
försvinner. Storlek 2 ger 2 st (storlek 3 finns bara i creative och ger 3).

**Frekvens (worldgen-test, 3 kolumner/chunk):** på land ligger ytfynd i ungefär en tredjedel av chunkarna (8–21 av 25 i olika områden, nästan inget över hav), oftast 1–3 block per fynd. Malachite är vanligast, Cassiterite ungefär hälften så vanligt, Native Copper ovanligt. Kopparmalm finns i vanilla nästan i varje chunk, så fyndet visar främst var malmen ligger **nära ytan**: i testet låg malmen i kolumnerna runt varje kontrollerat fynd.

### Malm och drops

- **Copper Ore:** vanilla, ger Raw Copper. Kräver minst stentier: Crude Stone Pickaxe eller Stone Pickaxe
  (flintverktyg är trätier och räcker inte).
- **Tin Ore:** ger **1–2 Cassiterite** (Fortune hjälper, Silk Touch ger blocket). **Kräver Copper Pickaxe** eller bättre
  (taggen `mechanical:needs_copper_tool`, som ligger i `incorrect_for_wooden_tool` och `incorrect_for_stone_tool`), så
  kopparsteget går inte att hoppa över. Sten- och flinthackor bryter den utan drop.

### Items

| Item | Varifrån | Används till |
|---|---|---|
| Malachite | Loose Malachite | Stone Anvil → 1 Crushed Copper Ore |
| Native Copper | Loose Native Copper, sällan vaskning | Krossas inte. Läggs direkt i en Crucible (2 enheter), se fas 3 |
| Cassiterite | Tin Ore, Loose Cassiterite, vaskning | Stone Anvil → 1 Crushed Tin Ore |
| Crushed Copper Ore | Stone Anvil | Läggs i en Crucible (1 enhet) och smälts, se fas 3 |
| Crushed Tin Ore | Stone Anvil | Läggs i en Crucible med koppar (1 enhet), se Fas 3b |
| Copper Concentrate / Tin Concentrate | Crushed ... Ore tvättad i Pan | Crucible, **2 enheter** (se *Tvätta malm* nedan) |
| Pan / Pan with Gravel / Pan with Sand | Crafting | Vaskning |

### Pan (vaskning)

Recept (crafting table, P = valfria Planks, T = Twine):

```
[P][ ][P]
[T][P][T]
```

- 64 användningar. Slitaget följer med mellan tom och fylld panna (tre items: Pan, Pan with Gravel, Pan with Sand).
- **Fylla:** högerklicka på Gravel → Pan with Gravel, på Sand eller Red Sand → Pan with Sand. Blocket försvinner.
- **Vaska:** håll inne Använd med fylld panna medan du står i vatten eller siktar på vatten. 3 s borst-animation med
  stänk och ljud. Saknas vatten står det "Pan in water: stand in it or aim at it". Släpper man i förtid händer inget.
- **Resultat** (loot tables `data/mechanical/loot_table/gameplay/panning/gravel.json` och `sand.json`), ett drag:

| | Inget | Flint | Stone Chunk | Cassiterite | Native Copper |
|---|---|---|---|---|---|
| Gravel | 48 % | 26,5 % | 19,3 % | 3,6 % | 2,4 % |
| Sand | 75,3 % | 8,2 % | 11 % | 4,1 % | 1,4 % |

  Vid **River, Frozen River, Beach, Snowy Beach och Stony Shore** (biom där spelaren står) en extra pool:
  8,3 % chans till ytterligare en Cassiterite. Cassiterite sänktes till ungefär en tredjedel (förr 9 % / 12,5 % / 25 %)
  när Tin Ore började kräva Copper Pickaxe: vaskning ger en smak av tenn men räcker inte till verktyg.
- Varje vaskning slits pannan 1 (inte i creative). Sedan är den tom igen.

### Tvätta malm (koncentrat)

- Håll en **tom Pan** i ena handen och **Crushed Copper Ore** eller **Crushed Tin Ore** i den andra, och håll inne Använd
  i vatten (samma krav och 3 s borst-animation som vaskning). Då sköljs gråberget bort: en krossad malm blir en
  **Copper Concentrate** / **Tin Concentrate** och pannan slits 1. Siktar man på grus under vattnet tvättar pannan
  ändå (den öser inte upp gruset så länge malm ligger i andra handen).
- Koncentrat räknas som **2 enheter** i degeln (krossad malm 1). Tvättning dubblar alltså utbytet men kostar 3 s och en
  användning per malm (en panna = 64 koncentrat). Fem Copper Concentrate fyller en degel.
- Tänk på blandningen: 4 Copper Concentrate + 1 Tin Concentrate är 20 % tenn (slagg). Bra brons: 4 Copper Concentrate +
  1 Crushed Tin Ore (8:1 = 11,1 %), eller 9 krossad koppar + 1 krossad tenn som förut.
- Kod: `PanItem` (tvättning), `BronzeOres.washed`, `Casting.oreUnits`; självtestet i `CrucibleContents` kollar
  koncentratets enheter. Texturer: `gen_bronze_textures.py` (bibliotekets dammhögar rentvättade, med metallglimt; tenn
  är svart tennsand).

### Stone Anvil (krossning)

Recept (crafting table, R = rock chunk, K = Flint Pickaxe som blir kvar och slits 1, C = Cobblestone):

```
[R][K][R]
[C][C][C]
```

- Platt stenblock (eget model: Smooth Stone ovanpå, Stone på sidorna). Bryts med pickaxe, ger sig själv.
- Högerklicka med krossbar malm → en läggs på stenen och syns ovanpå. Tom hand tar tillbaka den.
- Slå **4 gånger** med en hammarsten (taggen `mechanical:hammerstones`: Flint Pickaxe, Crude Stone Pickaxe,
  Stone Pickaxe), med vänster- eller högerklick. Varje slag: stenljud, sten- och malmpartiklar, hammaren slits 1.
  Så länge malm ligger på stenen bryter hammaren inte sönder städet.
- Resultatet hoppar upp ovanpå:
  - Raw Copper → 2 **Crushed Copper Ore**
  - Malachite → 1 Crushed Copper Ore
  - Cassiterite → 1 **Crushed Tin Ore**
  - Red/Yellow Ochre, Chalk, Charcoal → 2 pigment, Calcite → 4 White Pigment (se *Pigment och grottmålningar* i stenåldern)
  - Chalk Block, Red/Yellow Ochre Block → 8 pigment; Gypsum → 2 **Gypsum Powder** (se *Gips och Slate Tablet* nedan)
- Bryts städet ramlar malmen som ligger på det ut.

## Fas 2: värme (byggd)

Kod: `KilnBlockEntity` (temperatur, påfrestning, lagning), `KilnBlock` (CRACKS, HOT), `BellowsBlock`,
`client/KilnScreen` (termometer), `Casting` (registrering). Data: `scripts/gen_copper.py` och kilnens modeller i
`gen_data.py`; texturer: `scripts/gen_copper_textures.py`. Generatorordning: gen_data.py src/main/resources → gen_tribe_village →
gen_trails → gen_paths → gen_bronze_ores → gen_age_recipes → gen_farming → gen_wind → **gen_copper** → **gen_bronze** → gen_smoke → ... →
gen_block_items → gen_unlocks → gen_advancements → gen_registry.

### Temperatur i Clay Kiln

Kilnen har en temperatur i grader (sparas i blocket, syns i GUI:t). Kall kiln = luften, omräknad från
Temperature-systemet (×1,5 av kroppsskalan: slätt ~18°, snöbiom några grader, natt och regn kallare).

- **Tänd:** stiger **5°/s** mot det brinnande bränslets **naturliga max** (20° → 800° på ~2,5 min).
- **Släckt:** sjunker **3°/s** mot luften. Regn på skorstenen ×2, luft under 5° ×1,5.
- **Över naturligt max** (efter bälgpumpning): sjunker **2°/s** tillbaka mot det.
- Taket är **1300°**. En sprucken kiln blir inte varmare än **600°**.
- Maxet följer bränslet som brinner just nu (byts när nästa bränsle tas). Lägerelden kilnen byggdes på räknas som Firewood.

| Bränsle | Naturligt max | Max med Bellows |
|---|---|---|
| Stick, Sapling, Planks | 500° | 700° |
| Firewood, Log (och lägerelden) | 650° | 900° |
| Peat (3 min, se stenålderns *Myr och jord*) | 600° | 850° |
| Charcoal, Coal | 800° | 1300° |

**Hetare bränsle tar direkt:** lägger man ett bränsle med högre max i bränsleslotten medan ett sämre brinner (t.ex.
Charcoal på ved) tas det in genast. Det som var kvar av det gamla bränslet brinner med (brinntiden läggs ihop), så
inget går förlorat och temperaturen börjar stiga mot det nya maxet direkt.

Brons (950°) och koppar (1085°) kräver alltså **Charcoal och Bellows**. Trä räcker för lera.

### Temperaturkrav på kilnens recept

Gäller alla recept. Kilnen bränner bränsle och värms så länge det finns något att baka, men framstegen står still
(nollställs inte) tills temperaturen räcker. GUI:t visar ett vitt streck vid termometern på den temperatur som behövs
(grönt när den nåtts).

**Hetare går fortare:** tiden i tabellen gäller vid precis den temperatur som krävs. Varje 200° över det ger en hel takt
till, upp till 4× (t.ex. ett recept som kräver 500° går 2× vid 700°, 3× vid 900°). Tooltipen på termometern visar
takten ("Baking speed: 2.0x"). Gäller även smältning i Crucible, inte bränningen av en ny kiln.

| In | Ut | Tid | Kräver |
|---|---|---|---|
| 1 Clay Ball | Brick | 20 s | 600° |
| 1 Log / 1 Rough Log / 2 Firewood | Charcoal | 30 s | 400° |
| Unfired Crucible | Crucible | 40 s | 800° |
| Unfired Clay Bucket | Clay Bucket | 30 s | 600° |
| Dough | Bread | 20 s | 200° |
| Unfired Clay Tuyere | Clay Tuyere | 20 s | 600° |
| Unfired Clay ... Mold (6 sorter) | Clay ... Mold | 20 s | 600° |
| Crucible med malm | Crucible of Molten Copper / Bronze | 15 s | metallens smältpunkt: koppar 1085°, brons 950° |
| Crucible of Solid Copper / Bronze | Crucible of Molten ... | 10 s | 1085° / 950° |
| Unfired Clay Saw Blade / Plate Mold | Clay Saw Blade / Plate Mold | 20 s | 600° |
| Unfired Storage Jar / Unfired Water Jug | Storage Jar / Water Jug | 30 s | 600° |
| Unfired Flower Pot | Flower Pot (vanilla) | 20 s | 600° |
| Unfired Decorated Pot | Decorated Pot (vanilla), med inpressade Pottery Sherds kvar | 30 s | 600° |
| Fire Clay | Fire Brick (se *Bloomery*) | 20 s | 1100° |
| Unfired Clay Hammer Head / Anvil Mold | Clay Hammer Head / Anvil Mold | 20 s / 30 s | 600° |
| Iron Bloom (kall) | Hot Iron Bloom, slagen behålls | 10 s | 1100° |
| Kaolin (blocket) | Kaolin Tiles | 20 s | 1000° |

**Första bränningen** av en ny kiln räknas bara medan den är minst **500°** (2 min). På lägereldens ved tar det ~1,5 min
att komma dit.

### Bellows

Recept (crafting table): `[Planks][Scraped Hide][Planks]` / `[Planks][Scraped Hide][Planks]` / `[Twine][Clay Tuyere][Twine]`.

- Ställs **bredvid kilnens eldstad** (nedre halvan) med munstycket mot den. Munstycket pekar bort från spelaren när
  man ställer ut den: stå bakom bälgens plats och titta mot kilnen.
- **Högerklick = en pump** (+40°, upp till bränslets max med bälg). Bälgen trycks ihop i 0,5 s (eget modellsteg, det
  är också cooldownen). Luftljud och en pust ur munstycket; mot en tänd kiln flammor, gnistor och rök ur kilnen och ett
  fräs från elden.
- Pekar den inte på en kiln: "The bellows must blow into a Clay Kiln's firebox". Släckt kiln: "The kiln is not lit".
- Modell: två plankbrädor med ett veckat lädersäck emellan, handtag på övre brädan, läderrem i gångjärnet och ett
  lerrör (Clay Tuyere) fram. Bryts med yxa, ger sig själv.
- Över naturligt max sjunker temperaturen 2°/s, så **ungefär 3 pumpningar i minuten** håller koppar flytande när man
  väl nått 1085° (från Charcoals 800° krävs ~8 pumpningar för att komma upp).

### Clay Tuyere

Unfired Clay Tuyere: `[ ][ ][Clay Ball]` / `[ ][Clay Ball][ ]` / `[Clay Ball][ ][ ]` (diagonalt). Bränns i kilnen (600°)
till **Clay Tuyere**, som bara används i Bellows.

### Termometer i GUI:t

Lodrät glastermometer till vänster i kilnens GUI (0–1300°) med markeringar vid **600°** (röd, lera), **950°** (orange,
brons) och **1085°** (gul, koppar). Fyllningen går från mörkröd via röd och orange till vitgul, och kulan glöder i
samma färg som temperaturen. Håller man musen över den: exakt temperatur, vad receptet kräver, påfrestning eller att
kilnen är sprucken.

### Hetare kiln

Över **900°** (tänd): eldöppningen och golvet glöder (ett animerat glödlager som lyser själv), ljus 15 i stället för 13,
fler flammor och gnistor (lavapartiklar) ur öppningen.

### Överhettning och sprucken kiln

- Pumpar man när kilnen redan är vid taket (minst 1280°) byggs **påfrestning** upp: **+12 per pump**, knastrande och
  ett dovt ljud från leran. Den sjunker **1 per sekund** när man slutar. Syns som en röd stapel bredvid termometern.
- Leran får **sprickor i 2 steg** (påfrestning 34 % och 67 %).
- Vid **100** spricker kilnen: elden slocknar (påbörjat bränsle finns kvar), ljud av krossad lera, rök och lerbitar.
  Ligger en **Crucible of Molten Copper** i resultatrutan förstörs metallen (degeln blir en tom Crucible). Kilnen är nu
  **Cracked** (egen sprucken textur) och blir inte varmare än 600°.
- **Laga:** högerklicka med **Clay Ball**, en i taget, 4 st ("Patched 1/4" ...). Sedan måste lagningen brännas in som
  första bränningen men kortare: kilnen går tillbaka till sista bränningssteget (40 s vid minst 500°).
- Bryts en sprucken kiln behåller item:et sprickorna (block_state, som bränningen).

## Fas 3: gjutning (koppar, byggd)

Kod: `Casting` (registrering, malm → enheter, slipning), `Metal`, `CrucibleContents` (datakomponent), `CrucibleItem`,
`ClayMoldBlock`, `WhetstoneBlock`.

### Crucible med malm

Crucible (från stenåldern) kan nu fyllas med malm. Den staplas inte längre (en per stack, eftersom den bär sitt innehåll).

- **Fylla:** klicka malm på degeln i inventoryt (vänsterklick: så mycket som får plats, högerklick: en), eller håll
  malmen i andra handen och använd degeln.
- Rymmer **10 enheter** (var 8 före brons; 10 så att 9 koppar + 1 tenn får plats). **Crushed Copper Ore = 1 enhet,
  Native Copper = 2** (nästan ren metall), Crushed Tin Ore = 1, Copper/Tin Concentrate = 2 (tvättad i Pan, se Fas 1)
  och Bronze Ingot = 2 (se Fas 3b). Malachite och Raw
  Copper måste krossas först (Stone Anvil).
- Tooltip: "Copper ore: 6/10 units" och "Melts in a Clay Kiln at 1085°". Malm går inte att ta ut igen.
- Innehållet sparas som enheter per metall (`mechanical:crucible_contents`), så brons kan lägga till tenn och en
  legeringsregel (9:1) utan att ändra lagringen.

### Smälta

Lägg degeln i kilnens **input-ruta**. När kilnen är **minst 1085°** smälter den på 15 s till en **Crucible of Molten
Copper** i resultatrutan (samma antal enheter). Ligger den kvar i resultatrutan medan kilnen är minst 1085° hålls den
flytande.

### Crucible of Molten Copper

- Stelnar efter **60 s** utanför kilnen (en orange stapel visar värmen som är kvar) och blir en **Crucible of Solid
  Copper**, som måste smältas om i kilnen (10 s vid 1085°). Kollas när degeln ligger i spelarens inventory och när man
  häller (har den legat i en kista för länge är den stel när man tar fram den).
- En Crucible of Solid Copper kan **fyllas på med mer malm** på samma sätt som en tom (upp till 8 enheter totalt); allt
  smälter ihop när den smälts om.
- Hälls i en Clay Mold (nedan). Blir degeln tom får man tillbaka en tom Crucible.
- **Tömma degeln:** smyg + använd degeln i luften. Malm kommer tillbaka med lika många enheter: varje par som 1 Copper/Tin
  Concentrate och en udda enhet som 1 Crushed Ore (så tömning och tvättning inte kan fördubbla malmen; brons i den som Bronze Ingots), stelnad metall slås ut som tackor (Copper Ingot / Bronze Ingot, 2 enheter per tacka, en
  udda enhet blir skrot). Smält metall går inte: "Let it cool before you knock it out". Kvar blir en tom Crucible.

### Mold Bench

Alla lerformar (och Unfired Crucible) formas i **Mold Bench**, en låg träbänk med en lerkladdad skiva, en lerplatta,
flintkniv och spatel. Förr var de crafting-recept av Clay Balls, men mönstren krockade hela tiden med varandra.

- **Recept:** `[Flint][Clay][Flint]` / `[Planks][Planks][Planks]` (Clay = lerblocket). Låses upp i receptboken när man
  bär Clay Ball. Bryts med yxa.
- **Användning:** högerklicka. Till vänster en lista med alla formar (ikoner, hovra för kostnaden), en ruta för Clay
  Balls och en resultatruta. Klicka en form: leran hämtas från inventoryt (shift-klick: så många omgångar som får plats
  i en stack). Ta formen ur resultatrutan. Flera formar kostar lika mycket lera; den som valdes i listan gäller.
  Lägger man lera i rutan utan att välja blir det den dyraste form leran räcker till.
- Alla recept är upplåsta från början. Inget verktyg slits.

| Unfired ... | Clay Balls |
|---|---|
| Crucible | 7 |
| Clay Ingot Mold | 5 |
| Clay Pickaxe Head Mold / Axe Head Mold | 7 / 7 |
| Clay Shovel Head Mold / Hoe Head Mold | 5 / 6 |
| Clay Knife Blade Mold | 4 |
| Clay Tool Rod Mold | 4 (koppar eller brons, se "Clay Molds") |
| Clay Saw Blade Mold / Plate Mold | 5 / 8 |
| Clay Spear Head Mold / Sword Blade Mold | 4 / 6 |
| Clay Kettle Mold | 28 (= 7 Clay-block; bänken tar bara Clay Balls) |
| Clay Arrowheads Mold | 3 |
| Clay Bucket / Sickle Blade / Shears Mold | 8 / 5 / 5 |
| Clay Fish Hooks / Needles Mold | 3 / 3 |
| Clay Tongs / Chisel Blade Mold | 6 / 4 (se "Fler bronsverktyg") |
| Clay Hammer Head / Anvil Mold | 5 / 32 (se "Bloomery") |

Kod: `MoldBench` (registrering, recepten, menyn), `MoldBenchBlock`, `RecipeStationMenu` (1 inputruta). Data:
`scripts/gen_moldbench.py`, texturer: `scripts/gen_moldbench_textures.py`.

### Clay Molds

Sju formar. Unfired-formarna formas av Clay Balls i **Mold Bench** (se nedan, inte i crafting table) och bränns i
kilnen (600°, 20 s) till en form som ställs på marken (behöver fast mark under).

| Form | Mold Bench (Clay Balls) | Enheter per gjutning | Ger |
|---|---|---|---|
| Clay Ingot Mold | 5 | 2 | Copper Ingot (vanilla) |
| Clay Pickaxe Head Mold | 7 | 6 | Rough Cast Copper Pickaxe Head |
| Clay Axe Head Mold | 7 | 6 | Rough Cast Copper Axe Head |
| Clay Shovel Head Mold | 5 | 2 | Rough Cast Copper Shovel Head |
| Clay Hoe Head Mold | 6 | 4 | Rough Cast Copper Hoe Head |
| Clay Knife Blade Mold | 4 | 2 | Rough Cast Copper Knife Blade |
| Clay Tool Rod Mold | 4 | 2 | Rough Cast Copper Tool Rod (brons: Rough Cast Bronze Tool Rod, Poor Bronze följer med) → slipas till Copper / Bronze Tool Rod |

- **Hälla:** högerklicka på en tom form med Crucible of Molten Copper. Räcker inte metallen: "This mold takes 6 units
  of metal; the crucible holds 4". Formen fylls med glödande metall (syns i formen, animerad, lyser 9), lavaljud.
- Metallen **stelnar på 5 s** (fräs och rök). Kliver man på flytande metall bränner det.
- **Ta ut:** tom hand på den stelnade formen → gjutningen hamnar i inventoryt.
- Formen tål **5 gjutningar**, sedan spricker den (ljud och lerbitar, "The mold cracked"). Bryts den behåller item:et
  hur många gjutningar den gjort.
- Formens ovansida visar själva håligheten (gjutningens egen siluett i bränd lera), och flytande eller stelnad
  koppar syns i den.
- En full degel (10 enheter) räcker till en hacka eller yxa + 2 ingots, eller 5 ingots. 6 enheter = 3 Raw Copper.
- Formen har block-state `metal` (copper, bronze, poor_bronze) som avgör färgen i håligheten och vad som kommer ut.
  Fyra formar till för brons (Saw Blade, Plate, Spear Head, Sword Blade), se Fas 3b. Fel metall: "Copper can't be cast in this mold".

### Whetstone

Recept: `[ ][Sandstone][ ]` / `[Stick][Planks][Stick]`. Ett sandstensblock på en låg träbänk. Bryts med pickaxe.

Högerklicka med ett **Rough Cast ...** **4 gånger** (kort cooldown mellan dragen): gnistor, slipljud, metallflisor.
Det fjärde draget ger det slipade huvudet (Copper Pickaxe Head osv.). Framstegen sitter i stenen, så ett huvud i taget.

## Kopparverktyg (byggda)

Delar, nivåer och faktorer för skaft och bindningar: se *Delar och nivåer* i 01-stone-age.md. Quest: **A Better Haft** (ta ett verktyg med metallskaft från Tool Station).

Ett mjukt mellansteg mellan sten och brons. Monteras i **Tool Station**; receptet låses upp när man bär på det slipade
huvudet. Hacka, yxa, spade och hacka (hoe) är vanillas kopparverktyg (vanillas egna recept är fortfarande avstängda).

**Rods för dynamiska verktyg:** **Copper Tool Rod** (nivå 1, hållbarhet ×1,2) och **Bronze Tool Rod** (nivå 2, ×1,35,
+0,5 skada) **gjuts** som huvudena: Clay Tool Rod Mold (4 Clay Balls i Mold Bench, bränns i kilnen 600°; täljstensformen
Soapstone Tool Rod Mold spricker aldrig), 2 enheter koppar eller brons → Rough Cast Copper / Bronze Tool Rod, 4 drag på
Whetstone → rod. Poor Bronze följer med till rodden och verktyget (70 % hållbarhet). Inget crafting-recept.
Skript: `scripts/gen_tool_rods.py` (form, gjutningar, texturer), `scripts/gen_metals.py` (gjutningen `tool_rod`). Test: `ToolsTest`.

| Verktyg | Huvud | Binding | Skaft |
|---|---|---|---|
| Copper Pickaxe | Copper Pickaxe Head | Cordage | Tool Rod |
| Copper Axe | Copper Axe Head | Cordage | Tool Rod |
| Copper Shovel | Copper Shovel Head | Cordage | Tool Rod |
| Copper Hoe | Copper Hoe Head | Cordage | Tool Rod |
| Copper Knife | Copper Knife Blade | Twine | Stick |

- **Siffror** (vanillas kopparmaterial): 190 hållbarhet, hastighet 5, +1 skada (Stone Pickaxe: 131 / 4 / +1).
- **Brytnivå som sten:** `minecraft:incorrect_for_copper_tool` innehåller `#mechanical:needs_bronze_tool`, så
  kopparhackan bryter inte Iron, Coal eller Lapis Ore.
- Copper Pickaxe fungerar som hammarsten på Stone Anvil.
- **Copper Knife:** 200 hållbarhet, styckar 1,5× så snabbt och ger 1,2× så mycket som Flint Knife. Klarar **inte**
  Brown Bear (Tusk Dagger är fortfarande jaktkniven tills brons). Räknas som kniv (`mechanical:knives`) och svärd.
- Ingen kopparrustning. Copper Ingot har ingen egen användning i modden än.
- **Vanilla-recept med Copper Ingot:** Copper Block, Cut Copper m.m., Copper Bars, Chain, Door, Trapdoor, Brush och Copper
  Nugget fungerar. **Avstängda** (kommer senare): Spyglass, Lightning Rod och Copper Chest (`gen_age_recipes.py`).

## Fas 3b: brons (byggd)

Kod: `Bronze` (registrering, Poor Bronze-märket, tooltip), `Metal` (COPPER, TIN, BRONZE, POOR_BRONZE), `CrucibleContents`
(legeringsregeln + självtest som körs vid laddning), `CrucibleItem`, `ClayMoldBlock` (METAL-property), `Casting.oreUnits`.
Data: `scripts/gen_bronze.py` (direkt efter gen_copper; formarna skrivs av gen_copper.py med de andra), texturer:
`scripts/gen_bronze_textures.py`. Brons är gyllenbrun (egen palett), koppar laxrosa.

### Legering i degeln

- **Crushed Tin Ore** (1 enhet) läggs i samma Crucible som kopparmalm (Cassiterite krossas först, som Malachite).
- Andelen tenn = tenn / (koppar + tenn):

| Tenn | Blir | Smälter vid | Exempel (10 enheter) |
|---|---|---|---|
| 9–11,5 % | **Bronze** | 950° | 9:1 (10 %), 8:1 (11,1 %) |
| 8–9 % eller 11,5–15 % | **Poor Bronze** | 950° | 7:1 (12,5 %), 6:1 (14,3 %) |
| under 8 % eller över 15 % | **Copper**, tennet går förlorat som slagg | 1085° | 5:1 (16,7 %) → 5 koppar |
| bara tenn | inget (smälter inte) | – | |

- Tooltip på degeln: "Copper ore: 9/10 units", "Tin ore: 1/10 units", sedan **"10.0% tin: Bronze"** (grön), gul för
  Poor Bronze, röd för slagg ("no bronze ... Melts as copper; the tin is lost as slag") och "Melts in a Clay Kiln at 950°".
- Smält brons: "Crucible of Molten Bronze" / "... Poor Bronze" (alla enheterna blir brons). Stelnad: "Crucible of Solid
  Bronze", smälts om vid 950°.
- Brons blandas inte med malm: en degel med brons tar bara emot Bronze Ingot av samma kvalitet, och en degel med malm tar
  inte emot Bronze Ingot. En stelnad kopparsdegel kan fortfarande fyllas på med koppar- eller tennmalm.
- **Bronze Ingot** (gjuten i Clay Ingot Mold, 2 enheter) kan läggas tillbaka i en Crucible (2 enheter); Poor förblir Poor.

### Kvalitet: Poor Bronze

Allt som gjuts av Poor Bronze får märket `mechanical:poor_bronze` (tooltip "Poor Bronze: 70% durability"). Det följer
med: gjutning → slipning på Whetstone → Tool Station / Armor Station. Färdiga verktyg och rustningsdelar får **70 %**
hållbarhet (t.ex. Bronze Pickaxe 322 i stället för 460). Delar av olika kvalitet staplas inte ihop, och stationerna
blandar dem inte när de hämtar delar.

### Formar (nya)

| Form | Mold Bench (Clay Balls) | Enheter | Ger |
|---|---|---|---|
| Clay Saw Blade Mold | 5 | 4 | Rough Cast Bronze Saw Blade |
| Clay Plate Mold | 8 | 4 | Rough Cast Bronze Plate |
| Clay Spear Head Mold | 4 | 4 | Rough Cast Bronze Spear Head |
| Clay Sword Blade Mold | 6 | 6 | Rough Cast Bronze Sword Blade |
| Clay Arrowheads Mold | 3 | 2 | Rough Cast Bronze Arrowheads (4 spetsar på en gjuttapp) |

Bara brons kan gjutas i dem. De andra formarna gjuter brons som koppar: Bronze Ingot, Rough Cast Bronze Pickaxe/Axe/
Shovel/Hoe Head och Knife Blade. Allt Rough Cast slipas på Whetstone (4 drag) till Bronze ... Head / Blade / Plate.

### Bronze Arrowheads och Bronze Arrow

- **Clay Arrowheads Mold:** en flerform med **fyra pilspetsar** på en gjutkanal (3 Clay Balls i Mold Bench, bränns i
  kilnen 600° som de andra formarna). Tar **2 enheter** brons (som ett tackel) och ger **Rough Cast Bronze Arrowheads**.
  Bara brons.
- **Whetstone:** 4 drag på Rough Cast Bronze Arrowheads ger **4 Bronze Arrowhead** (spetsarna slipas loss från tappen).
- **Fletching Table:** **4 Bronze Arrowhead + Stick + Feather → 4 Bronze Arrow**. En hel degel (10 enheter, 5 gjutningar)
  räcker till 40 pilar; formen spricker efter 5 gjutningar som de andra.
- **Bronze Arrow:** **3 skada** (Arrow 2, Eagle Fletched Arrow 2,75, Barbed Arrow 3 men den blöder mer) och går sönder
  bara **10 %** av gångerna när den faller ur ett djur (Arrow 30 %). Står i `minecraft:arrows`, så alla bågar skjuter den.
  Genomborrning (pierce) gick inte utan en access transformer (`AbstractArrow.setPierceLevel` är privat), så den är
  bara starkare och tåligare.
- **Poor Bronze:** följer med från gjutningen till spetsarna och pilarna (Fletching Table ärver märket som Tool
  Station). En pil av Poor Bronze gör **0,5 mindre skada** (2,5); tooltip "Poor Bronze: half a point less damage".
- Advancement **Bronze Tips** (Bronze Age-fliken, efter Bronze!).
- Kod: `Bronze` (items, `ground`), `ClayMoldBlock.Shape.ARROWHEADS`, `Metal`, `MoldBench`, `KilnBlockEntity`,
  `FletchingRecipes`, `SpecialArrowItem`, `ArrowWounds#breakChance`. Data: `scripts/gen_copper.py` (formen),
  `scripts/gen_smallbatch.py`; texturer: `scripts/gen_bronze_textures.py`.

### Bronsverktyg

Monteras i **Tool Station**, låses upp när man bär det slipade huvudet.

| Verktyg | Huvud | Binding | Skaft |
|---|---|---|---|
| Bronze Pickaxe / Axe / Shovel / Hoe | Bronze ... Head | Cordage | Tool Rod |
| Bronze Knife | Bronze Knife Blade | Twine | Stick |
| Bronze Saw | Bronze Saw Blade | Twine | Stick |
| Bronze Spear | Bronze Spear Head | Cordage | Tool Rod |
| Bronze Sword | Bronze Sword Blade (med parerstång) | Cordage | Leather (grepp) |

- **Bronsmaterial:** 460 hållbarhet (koppar 190, järn 250), hastighet 6 och +2 skada som järn, förtrollbarhet 12.
  Attackvärden som vanillas järnverktyg. Lagas med Bronze Ingot.
- **Brytnivå:** `mechanical:incorrect_for_bronze_tool` = `#minecraft:needs_iron_tool` + `#minecraft:needs_diamond_tool`.
  Bronze Pickaxe bryter alltså **Iron, Coal och Lapis Ore** (`needs_bronze_tool`) och Tin Ore, men inte guld, diamant,
  redstone eller smaragd (de väntar på järnet). Fungerar som hammarsten på Stone Anvil.
- **Bronze Knife:** 700 hållbarhet, styckar **1,75×** så snabbt och ger **1,35×** så mycket som Flint Knife, **klarar
  Brown Bear** och Giant Deer (knivnivå 2 som Tusk Dagger). Räknas som kniv och svärd.
- **Bronze Spear:** vanillas spjut (laddad stöt när man håller in Använd, vanligt hugg annars; kastas inte). Flint Spear
  har stone spears värden på ett flintmaterial (100 hållbarhet, +1 skada); Bronze Spear har **iron spears** tider och
  trösklar på bronsmaterialet: 460 hållbarhet, +2 skada, snabbare stöt (0,95 s), mer skada vid laddning (×0,95 mot
  ×0,82) och kortare väntan. Eget långt modell i handen (järnspjutets form i brons, rött band), platt ikon i GUI:t.
  Står i `minecraft:spears`.
- **Bronze Sword:** vanligt vanilla-svärd på bronsmaterialet: **5,5 skada** (Iron Sword 6), attackfart 1,6, **460**
  hållbarhet. Står i `minecraft:swords` (svärdsförtrollningar).
- Poor Bronze följer med till spjut och svärd som till de andra verktygen (70 % hållbarhet: 322).

### Bronze Saw och planks

- 480 hållbarhet. I crafting table: **Log + Bronze Saw → 4 Planks** (alla träslag, recepten `sawn_<trä>_planks`);
  sågen blir kvar och slits 1. Utan såg ger en log fortfarande 2 planks.
- **Fallen Log:** håll in högerklick på stocken med sågen och fortsätt titta på den: man drar sågen fram och tillbaka
  (ett sågtag var halv sekund, 8 sågtag ≈ 4 s) och sista taget kapar hela stocken till 3 Rough Logs (utan slump, färre om
  yxan redan har huggit den tunnare), plus barken om den sitter kvar. Sågen slits 1 per kapad stock. Tittar man bort eller
  släpper avbryts sågningen, men snittet står kvar i stocken (blockstate `sawn` 0–7) och man fortsätter där man slutade.
  Snittet syns som ett mörkt sågspår tvärs över stockens mitt som blir djupare uppifrån och ner, med sågspån vid kanterna.
  Alla ser armarna dra sågen fram och tillbaka (egen ArmPose) och hör raspet; sågspån (blockpartiklar) yr vid varje tag.
- **Felled Log** (fällt träd, entity): samma sak på ett block av stocken: 8 sågtag kapar just det blocket till 3 Rough Logs
  plus barken, och stocken delas eller blir kortare (snittet syns inte, sågtagen räknas tills stocken flyttas eller servern
  startar om).

### Bronze Helmet och Bronze Greaves

Rustningsplåt gjuts (Clay Plate Mold), slipas och nitas på ett läderfoder vid **Armor Station**, smedjan för metallrustning.
Recept: `[Bronze Plate][Bronze Ingot][Bronze Plate]` / `[Planks][Crafting Table][Planks]`, låses upp av Bronze Plate.
Hammaren är vilken pickaxe som helst eller Bronze Smithing Hammer (taggen `#mechanical:hammerstones`); den ligger kvar och slits 1 per del:

| Del | Recept | Rustning | Hållbarhet |
|---|---|---|---|
| Bronze Helmet | 2 Bronze Plate + hammare (slits 1) + 2 Leather | 2 | 110 |
| Bronze Greaves (leggings-platsen) | 3 Bronze Plate + hammare + 3 Leather | 5 | 150 |

- Järnets rustningsvärden, toughness 0, ~70 % av järnets hållbarhet (multiplikator 10 mot 15). Ingen bröstplåt eller
  boots. Egna texturer på kroppen: hjälmen är en bronsfärgad järnhjälm, benskydden täcker bara smalbenen med en läderrem
  under knät.

### Degelns ikon

Crucible of Molten / Solid Metal visar metallen den håller: bronsfärgad (gyllenbrun) för Bronze och Poor Bronze,
kopparfärgad annars. Smält brons glöder gulare och ljusare än koppar, med en mer gyllene glöd på kanten. Item-modellerna
använder villkoret `mechanical:bronze_crucible` (`client/BronzeCrucibleProperty`, registrerat med NeoForges
`RegisterConditionalItemModelPropertyEvent`) som läser degelns innehåll; texturerna `molten_crucible_bronze` och
`solid_crucible_bronze` ritas av `gen_crucible_textures.py`.

### Byar: handelsnivå 3, 4 och 5

Nivå 1–2 är stenålderns (`gen_data.py`), nivå 3–4 bronsålderns och nivå 5 (Master) järnålderns (`gen_bronze.py`). Allt
är **mynt mot vara** ([traders-plan.md](../traders-plan.md) A): **Copper Coin** för billiga saker, **Silver Coin**
(= 10 koppar) för lite dyrare, **Gold Coin** (= 10 silver) för värdefulla. Formatet: köper = vad byborna tar emot (och
betalar för), säljer = vad de ger. Max = hur många gånger innan den måste fyllas på.

| Yrke | Nivå | Köper | Säljer |
|---|---|---|---|
| Armorer | 3 | 3 Bronze Ingot → 4 Copper | 4 Silver → Bronze Helmet (max 3) |
|  | 4 | 6 Copper Ingot → 4 Copper | 5 Silver → Bronze Greaves (max 3) |
| Butcher | 3 | 8 Tallow → 4 Copper | 4 Copper → 3 Smoked Meat |
|  | 4 | 3 Bear Fat → 4 Copper | 8 Copper → 4 Jerky |
| Cartographer | 3 | 8 Black Pigment → 4 Copper | 8 Copper → Item Frame |
|  | 4 | 8 Yellow Pigment → 4 Copper | 1 Silver → Painting |
| Cleric | 3 | 6 White Pigment → 4 Copper | 4 Copper → 4 Red Pigment |
|  | 4 | 6 Honeycomb → 4 Copper | 4 Copper → 4 Dried Mushrooms |
|  | 5 | 8 Glowstone Dust → 2 Silver | 1 Gold → Ender Pearl (max 4) |
| Farmer | 3 | 20 Barley Grain → 4 Copper | 4 Copper → 4 Flour, 8 Copper → 4 Flax Seeds |
|  | 4 | 20 Emmer Grain → 4 Copper | 8 Copper → 6 Dried Berries |
| Fisherman | 3 | 4 Whole Tropical Fish → 4 Copper | 8 Copper → 4 Smoked Fish |
|  | 4 | 3 Whole Pufferfish → 4 Copper | 8 Copper → 4 Dried Fish |
| Fletcher | 3 | 24 Feather → 4 Copper | 8 Copper → 8 Bone Arrow |
|  | 4 | 8 Sinew → 4 Copper | 2 Silver → Sinew Bow |
| Leatherworker | 3 | 4 Scraped Hide → 4 Copper | 2 Silver → Leather Satchel |
|  | 4 | 2 Wolf Pelt → 4 Copper | 2 Silver → Leather Leggings |
|  | 5 | 4 Wolf Pelt → 3 Silver | 1 Gold → 2 Hide Saddle Blanket |
| Librarian | 3 | 4 Book → 4 Copper | 2 Silver → Bookshelf |
|  | 4 | 2 Book and Quill → 4 Copper | 3 Silver → Name Tag |
|  | 5 | 4 Book and Quill → 3 Silver | 1 Gold → Cloth Bed |
| Mason | 3 | 20 Andesite Rock → 4 Copper | 4 Copper → 4 Bricks |
|  | 4 | 12 Terracotta → 4 Copper | 8 Copper → Clay Bucket |
| Shepherd | 3 | 12 Red Dye → 4 Copper | 1 Silver → White Bed |
|  | 4 | 12 Yellow Dye → 4 Copper | 4 Copper → 3 Red Wool |
|  | 5 | 4 Linen Cloth → 2 Silver | 1 Gold → 4 Silk Thread |
| Toolsmith | 3 | 6 Crushed Tin Ore → 4 Copper | 3 Silver → Bronze Pickaxe (max 3), 2 Silver → Bronze Saw (max 3) |
|  | 4 | 8 Copper Concentrate → 4 Copper | 1 Silver → 2 Tin Concentrate, 2 Silver → Bronze Axe (max 3) |
|  | 5 | 4 Wrought Iron Ingot → 3 Silver | 2 Gold → Steel Pickaxe (max 3), 1 Gold → Steel Saw (max 3) |
| Weaponsmith | 3 | 3 Bronze Ingot → 4 Copper | 2 Silver → Bronze Knife (max 3) |
|  | 4 | 12 Crushed Copper Ore → 4 Copper | 3 Silver → Bronze Spear (max 3), 4 Silver → Bronze Sword (max 3) |
|  | 5 | 2 Steel Ingot → 4 Silver | 2 Gold → Steel Spear (max 3) |

- **Myntens ikon växer med stacken** som Firewood: 1–6, 7–16, 17–32, 33–46 och 47–64 mynt ger fem bilder
  (ett mynt upp till tre staplar; texturer från `scripts/gen_coin_textures.py`, modeller från `gen_bronze.py`).
- **Pengarna är mynt** (ersätter vanillas Emerald). Byborna betalar med mynt när de köper och tar mynt när de säljer.
- Byborna handlar i samma handelsruta som stammar och karavaner: porträtt, yrkesnivå med erfarenhetsmätare, de säljer till vänster och köper till höger, hur många gånger kvar, och ändrat pris med det vanliga överstruket (rykte, Hero of the Village, efterfrågan). Klick handlar en gång, Shift så många gånger det går. Vanillas handelsmeny och regler är kvar under ytan (`people/VillagerTrade.java`, `client/people/VillagerTradeScreen.java`). Advancement: **Market Day**.
  Mynt präglas på Bronze Anvil (se Prägla mynt nedan).
- Priser: ett bronsverktyg 2–5 Silver Coin, en Bronze Ingot drygt 1 Copper, mat och råvaror 4–9 Copper. Köpen ger 20 xp (nivå 3)
  och 30 xp (nivå 4), säljen 10 / 15; max 12–16 för köp, 3 för bronsverktyg och rustning.
- Toolsmiths Tin Concentrate är en väg till tenn för den som saknar tennådror (dyrt: 12 Copper Coin för 4 enheter).
- Lerkärl (pottery) läggs till här när det finns.

### Kvar

- Bronslykta (valfritt). Tvättat koncentrat, spjut, svärd, skära och Bloomery är byggda.
- Crucible of Solid Bronze har samma (kopparfärgade) ikon som Solid Copper; namnet visar metallen.

## Fas 3c: Smelting Furnace (byggd)

Sen bronsålder: en fast ugn (lite som Tinkers' Construct, förenklad) som tar över när degeln blir för liten.
Degeln (Crucible + Clay Kiln) finns kvar som den tidiga vägen. Ugnen rymmer fyra deglar, blandar brons direkt, ger
**25 % mer metall per malm** och fyller lerrännor som leder metallen nedåt till formarna.

Kod: `Smelting` (registrering, advancements från koden), `FurnaceMelt` (innehåll, utbyte, legering, intagsgränser,
självtest), `FurnaceTapBlock` + `FurnaceTapBlockEntity` (styrblocket: struktur, intag, värme, smältning, tappning,
munstyckshålen), `FurnaceBricksBlock` (tegel med munstyckshål), `FurnaceChuteBlock` (taktratten), `FurnaceMenu` +
`client/FurnaceScreen` (status-GUI), `ClayChannelBlock` (rännor, lutningar, ösa) + `ClayChannelBlockEntity` (metallen i
en ränna) + `ChannelFlow` (portar, flöde, formar, ösning), `client/MoltenRenderer` (glödande metall i rännor och
kammaren). Ändrat: `BellowsBlock` (pumpar mot ugnsväggar), `FireDrillItem` och `Tinder` (tänder tappen),
`KilnBlockEntity` (två nya kilnrecept, värmesteget utbrutet till `heatStep` så ugnen delar det), `client/KilnScreen`
(termometern utbruten till `thermometer`), `CrucibleContents.alloy(int, int)` (publik). Data: `scripts/gen_smelter.py`,
texturer: `scripts/gen_smelter_textures.py`, registrets 3D-vy: `scripts/registry_models.py`. Generatorordning: ...
gen_pottery → gen_cart → **gen_smelter** → gen_block_items → gen_unlocks → gen_advancements → gen_registry.

### Recept

| Vad | Hur |
|---|---|
| Unfired Furnace Brick ×4 | Clay Ball (C) + Sand (S): `[C][S]` / `[S][C]` (eldfast lera: lera magrad med sand) |
| Furnace Brick | Unfired Furnace Brick i Clay Kiln, **10 s vid 600°** |
| Furnace Bricks | 4 Furnace Brick (2×2) |
| Furnace Chute | 3 Furnace Brick (B): `[B][ ][B]` / `[ ][B][ ]` (en tratt) |
| Unfired Clay Channel ×4 | Clay Ball: `[C][ ][C]` / `[C][ ][C]` |
| Clay Channel | Unfired Clay Channel i Clay Kiln, **20 s vid 600°** |
| Furnace Tap | Furnace Bricks + Clay Channel + Clay Ball + Stick (formlöst) |

En hel ugn är 24 Furnace Bricks + en Furnace Chute = 99 tegel = 50 Clay Ball + 50 Sand (avrundat). Teglet bränns på
halva vanliga tegeltiden så att det inte tar en evighet i kilnen (vid 1300° går det 4× fortare). Bricks, tratten och
tappen bryts med hacka (vilken som helst).

### Bygga

- **3×3×3** av **Furnace Bricks** där **bara mittblocket är tomt** (kammaren, där smältan står). Under kammaren sitter
  ett tegel, ovanpå den en **Furnace Chute** (mitt i översta lagret).
- **Furnace Tap** sitter mitt i **nedersta lagrets framvägg** (i stället för ett tegel), med pipen utåt. Den vänds mot
  spelaren när man sätter den, så sätt den utifrån.
- Strukturen kontrolleras varje sekund och när något bredvid tappen ändras. Är den inte hel gör ugnen ingenting
  (elden slocknar, bränslet ligger kvar) och högerklick på tappen säger vad som saknas: "Not a furnace yet: 5 Furnace
  Bricks missing (one at x y z)", "Put a Furnace Chute on top of the furnace's middle: x y z" eller "The middle of the
  furnace must be empty: clear x y z".
- **Ugnar byggda före 0.58** (schaktet öppet genom alla tre lagren) blir inte hela längre. Tappen säger då "Rebuild
  for 0.58: ... Furnace Bricks into the bottom of the shaft (x y z), a Furnace Chute on top (x y z)". Ingen automatisk
  ombyggnad: smältan och bränslet ligger kvar i tappen tills ugnen är hel igen.
- **När ugnen är hel** visar mitten av nedersta raden på **sidoväggarna och bakväggen** ett **munstyckshål** (tuyère: en
  ring av bränd lera runt ett mörkt hål, sot ovanför) som glöder när ugnen brinner. Det visar var bälgarna hör hemma
  (bälgar fungerar fortfarande mot alla väggar). Det är ett blocktillstånd på teglet som tappen sätter; teglet ger
  alltid vanliga Furnace Bricks när det bryts, och blir vanligt igen när ugnen inte längre är hel eller tappen bryts.
- Ugnen glöder: elden syns i den välvda **ugnsmunnen** ovanför tapphålet (animerade lågor), tapphålet lyser när
  pluggen är ute, munstyckshålen och trattens hål lyser, rök och lågor kommer ur tratten, och smältan syns som en
  glödande yta genom trattens hål (malm som inte smält ännu ligger som ett mörkare lager ovanpå).

### Fylla

Allt kommer in **genom tratten**: kasta eller släpp föremål på Furnace Chute (det som faller i tratten, ner i kammaren
eller ligger på kanten tas in).

- **Malm** i alla former degeln tar: Crushed Copper / Tin Ore (1 enhet), Copper / Tin Concentrate (2), Native Copper (2).
- **Tackor att smälta om:** Copper Ingot, Tin Ingot och Bronze Ingot (2 enheter; Poor Bronze förblir Poor). Tin Ingot sedan v0.208.0.
- **Bränsle:** bara **Charcoal** (och Coal), upp till **16** st.
- Det som inte får plats, inte smälter eller inte går att blanda kastas tillbaka upp från tratten (med ett fräs).
  Blandningsregeln är degelns: koppar och tenn blandas, smält brons tar bara mer av samma brons.

**Kapacitet: 40 enheter** metall (malm som inte smält räknas med), dvs. **fyra deglar**. Det räcker till en hel
uppsättning bronsverktyg (hacka 6 + yxa 6 + spade 2 + hacka/hoe 4 + kniv 2 + svärd 6 = 26 enheter) med tackor över,
och ugnen är ett stort bygge som ska kännas som ett steg upp från degeln.

**Gränser så att brons alltid går att blanda** (ny i 0.58):

- **Tenn högst 4,5 enheter** (18 kvarts, så mycket tenn som en full ugn kan ha och ändå bli bra brons, 11,5 %).
- **Koppar högst 35,5 enheter så länge inget tenn finns i ugnen** (resten av ugnen hålls fri för tennet). Ren koppar
  rymmer alltså 35,5 enheter, inte 40.
- Exempel: en hel stack Crushed Copper Ore tar 28 st, en stack Crushed Tin Ore efter det tar 3 st: 9,7 % tenn = Bronze.
  Tvärtom (tennstacken först) blir det 3 tenn + 29 koppar = 9,4 % = Bronze.

**Buggen före 0.58 (tenn och koppar blandades inte):** första stacken som kastades i fyllde ugnen (32 Crushed Copper Ore
är 160 kvarts) så att tennet kastades tillbaka och ugnen hällde ren koppar; eller tennet kom först, smälte ensamt
(232°) och blev en tennpöl som aldrig hälls. Med räknade mängder (t.ex. 27 + 3) blandades det även förut. Gränserna
ovan löser det. Kvar finns att en ugn som redan är varm nog för koppar (1085°) och har **öppen tapp** häller koppar
så fort den smält, innan tennet hunnit kastas i: **ha tappen pluggad medan du fyller på**.

### Värme

Samma modell och konstanter som Clay Kiln (`KilnBlockEntity.heatStep`):

- **Tänds** med **Fire Drill** på tappen (som kilnen, Tinder hjälper); kräver en hel ugn och Charcoal i den.
- Tänd stiger den **5°/s** mot Charcoals naturliga **800°**; släckt sjunker den 3°/s (×2 i regn på tratten).
- **Bellows** som pekar på vilken ugnsvägg som helst (tappen räknas; munstyckshålen visar var) pumpar **+40°** per
  klick upp till **1300°**. Över 800° sjunker den 2°/s tillbaka. Flera bälgar på olika väggar går fortare.
- Brons kräver **950°**, koppar **1085°**, tenn ensamt 232°. Alltså Charcoal och bälg, som i kilnen.
- Bränslet brinner så länge ugnen är tänd (4 min per Charcoal, kallt väder och regn drar mer); slut på bränsle = släckt.
- **Ingen överhettning:** eldfast tegel tål taket (1300°), ingen påfrestning eller sprickor som kilnen.

### Smälta

Malmen smälter en **kvarts enhet** i taget: **6 tick per kvarts enhet** vid smältpunkten (24 tick per enhet, lite
fortare än degelns 30), och **hetare går fortare** som i kilnen: +1 takt per 200° över, upp till 4×. Smältpunkten är
den för det som allt i ugnen blir (koppar + rätt mängd tenn: 950°, annars 1085°).

Ugnen smälter alltid den metall som ligger mest efter i smältan jämfört med blandningen, så smältan håller
blandningens förhållande medan den smälter. Brons blandas av koppar + tenn med **degelns regler** (9–11,5 % tenn =
Bronze, 8–15 % = Poor Bronze, annars koppar och tennet blir slagg).

**"Satt sig":** smälter ugnen fortfarande till en annan metall än smältan just nu är (t.ex. bara koppar har hunnit
smälta i en tennblandning) står tappen still, så man inte tappar en halvsmält blandning som slagg. När allt smält är
blandningen satt.

### Utbyte (+25 %)

- Ugnen räknar i **kvarts enheter**. **Malm ger 5 kvarts per enhet** (1,25 enheter, degeln 1), **tackor 4** (exakt
  sin metall).
- Exempel: 27 Crushed Copper Ore + 3 Crushed Tin Ore = 30 enheter malm → **37,5 enheter brons → 18 Bronze Ingots**
  (degeln: 15). Det som blir över (här 1,5 enheter) ligger kvar i ugnen tills mer malm fyller på.
- **Varför det inte går att fuska:** metall skapas bara när ett föremål går in (malm ×5, tacka ×4). Efter det flyttas
  den bara i hela kvarts mellan ugnen, rännorna, formarna och deglar, och varje flytt drar av exakt det den lägger
  till: ugnen → ränna (upp till 4 kvarts per steg), ränna → ränna, ugn/rännor → form (formens enheter ×4), rännor →
  degel (hela enheter ×4). Bara slagg (koppar tappad med tenn i) och en ränna som bryts med metall i förstör metall.
  En tacka kostar 8 kvarts att gjuta och ger 8 tillbaka, så smält → tappa → smält om ger aldrig mer. Bonusen sitter på
  malmen, och inget i modden gör metall till malm igen (degelns tömning ger malm tillbaka exakt som den lades i,
  tackor som tackor; en degel med stelnad metall ger tackor, 2 enheter var). Smältan hålls i tusendelar av en kvarts
  så att en hällning tar koppar och tenn i exakt rätt andel (med hela kvartar avrundades det åt samma håll varje gång
  och brons blev Poor efter några tackor).
- Bryts tappen förloras smältan och den osmälta malmen (bränslet ramlar ut), så det finns inget att plocka ut.

### Tappa

- **Smyg + högerklick med tom hand** på tappen drar ur / sätter i lerpluggen (syns: pluggen sitter i hålet med en
  pinne, eller ligger på marken bredvid pipen).
- Med öppen tapp rinner metallen när: ugnen är hel, smältan är minst sin smältpunkt varm, och blandningen har satt sig.
- **Form direkt framför pipen:** fylls efter en stund (0,5 s + 0,25 s per enhet formen tar), sedan nästa när den är
  tömd.
- **Ränna framför pipen:** tappen häller **upp till 1 enhet per kvarts sekund** (4 kvarts per steg, ett steg var
  5:e tick) i den första rännan när den har plats, och metallen rinner vidare genom nätet (se Rännor).
- Formen fylls med **samma regler som från degeln**: rätt metall för formen (koppar bara i de sex kopparformerna),
  formens enheter (tacka 2, hackhuvud 6 ...), stelnar på 5 s, tom hand tar ut gjutningen (Poor Bronze-märkt om
  smältan var Poor), formen spricker efter 5 gjutningar.
- Under hällningen lyser pipen (glödande metall rinner ner för den och droppar över kanten) och tappen ger mer ljus.

### Rännor (Clay Channel)

- Bränd lerränna (golv 2 px, väggar upp till 5 px, 6 px bred ränna). Kopplar ihop sig själv med rännor, Clay Molds och
  en tapps pip bredvid sig: rak, hörn, T och kors (modellen är rännans mitt plus en arm eller en ändvägg per sida).
- **Lutningar som räls:** en ränna som läggs **ett block under** en ränna (eller en tapps pip) bredvid sig, och som
  inte har någon plan ränna åt det hållet, blir en **45°-lutning** som klättrar mot den. Dess låga ände kopplar till det
  som ligger i dess egen höjd på andra sidan, den höga till det ovanför. Flera lutningar i rad går nedför en backe.
  Rännor räknas om när något bredvid eller snett ovanför/under dem ändras.
- **Kopplingsregel (portar):** varje sak har portar, mitten på en blockkant i en viss höjd: en plan ränna sina fyra
  sidor i golvhöjd, en lutning sin låga sida i golvhöjd och sin höga sida ett block upp, en form sina fyra sidor, en
  tapp sin pip. Två saker hänger ihop när de delar en port.
- **Rännor håller metall:** **1 enhet** (4 kvarts) per ränna, av en sorts metall. Metallen syns som en glödande yta i
  metallens färg som stiger med mängden (koppar orange, brons guldgul, Poor Bronze mattare), strömmar medan den rör
  sig och står nästan still annars; en lutning har en tunn ström nedför.
- **Flöde, ett steg var 5:e tick:** varje ränna lämnar upp till 4 kvarts av det den hade när steget började, **först
  nedåt** (en lutning, en lägre ränna), **sedan längs plana rännor bort från tappen** (längre från tappen i nätet). Så
  rinner metallen ränna för ränna, och nätet fylls från sina låga och bortre ändar bakåt mot tappen. Uppför rinner
  inget. Olika metaller blandas inte: en ränna tar bara emot samma metall som den redan har.
- **Formar** vid en rännas sida (eller vid en lutnings låga ände) fylls när rännan bredvid har metall och metallen som
  hänger ihop med den (smält, samma sort, närmast först) plus det den öppna tappen kan hälla räcker till hela
  gjutningen. Högst en form per steg. En form räknas lägre än rännan bredvid, så den fylls före rännorna.
- **Stelnar:** metall som inte rört sig (in eller ut) på **60 s** stelnar (mörk, matt). Stelnad metall rinner inte och
  tar inte emot mer. Rinnande metall i en full ränna som inget tar ifrån står också still, så ett fullt nät som ingen
  form tömmer stelnar efter en minut. Ingen klocka tickar: det räknas fram från när metallen senast rörde sig.
- **Ösa (tömma):** högerklicka en ränna med en **tom Crucible**: den öser upp **hela enheter** (upp till degelns 10) av
  metallen närmast rännan ur hela det sammanhängande nätet, närmast först. Smält metall ger en **Crucible of Molten
  Metal** (60 s att hälla den i en form för hand), stelnad en **Crucible of Solid Metal** (smält om i kilnen). Sorten
  och kvaliteten följer med (Bronze, Poor Bronze, Copper). Under en hel enhet går inte att ösa ("Not a whole unit of
  metal in these channels").
- **Bryta en ränna med metall i:** metallen rinner ut och **förloras** (lava- eller rökpartiklar, fräs). Inget
  återlämnas, så det kan inte dubbleras: ös först.
- Flödet körs av tappen (högst 64 rännor i nätet) och fortsätter med stängd tapp: metallen i rännorna rinner färdigt
  nedåt och in i formar. Bryts tappen står metallen kvar och stelnar.

### Kontroller

| Gör | Hur |
|---|---|
| Status | Högerklick på tappen (tom hand eller med något annat än Fire Drill). Hel ugn: GUI; annars vad som saknas |
| Öppna / stänga tappen | Smyg + högerklick med tom hand |
| Tända | Fire Drill på tappen |
| Fylla på | Kasta malm, tackor och Charcoal på Furnace Chute |
| Värma | Bellows mot en ugnsvägg (helst ett munstyckshål), högerklick = en pump |
| Tömma rännor | Tom Crucible på en ränna |

**GUI:** kilnens termometer (vitt streck vid smältpunkten som behövs, grönt när den nåtts; tooltip med temperatur och
smälttakt, markeringar vid 232°/950°/1085°), en tank med smältan i metallens färg och osmält malm mörkare ovanpå
(streck var 10:e enhet, tooltip "12.25 / 40 units"), under dem lågan, en Charcoal-ikon och antalet ("5/16", tooltip
"Charcoal 5/16" och om den brinner). Till höger rader med ikon: **smältan** som den metall den hälls som, med mängd och
kvalitet ("Molten Bronze" / "37.50 units (Good)"; Poor Bronze "(Poor)", koppar "(pure)", koppar med tenn "(tin lost as
slag)", medan den blandas "(still mixing)", bara tenn "(needs copper)"), sedan **varje sorts osmält metall** för sig
("Unmelted Copper" / "5.00 units", ikon krossad malm eller tacka), blandningen ("Mix: 10.0% tin = Bronze") och vad
tappen gör (öppen/stängd, för kall, blandningen sätter sig, fel tennandel ger slagg).

### Advancements (Bronze Age-fliken, efter Bronze!)

- **Firebrick:** ha Furnace Brick eller Furnace Bricks.
- **Smelting Furnace** (goal): ugnen blir hel (ges från koden till spelare inom 12 block).
- **Tapped** (challenge): en form fylls från en Clay Channel som tappen fyllt (spelare inom 16 block).
- **Skimming** (task, efter Tapped): ös metall ur en ränna med en tom Crucible (spelare inom 6 block). Beskrivningen
  tipsar också om lutningar.

### Beslut

- **Casting Basin: nej.** Modden har inga metallblock värda att gjuta (inget bronsblock; vanillas kopparblock är 9
  tackor i crafting och tillför inget).
- Tappen styr (status-GUI, plugg), allt matas genom tratten; inga platser i GUI:t.
- Bara Charcoal/Coal som bränsle: ugnen ska kräva kol och bälg, och ved räcker ändå inte till brons.
- **Munstyckshål som blocktillstånd**, inte ett eget block: inget extra recept, ugnen visar själv var bälgarna hör
  hemma, och bälgar mot andra väggar fungerar som förut.
- **Rännor:** metall i rännor stelnar efter 60 s stillastående (som en degel), en bruten ränna förlorar sin metall
  (ingen dubblering), ösning bara i hela enheter (degeln räknar hela enheter; en rest under en enhet blir kvar).
- **Inte gjort (valfritt):** att smältan rinner ut när ugnen bryts, överhettning, att stelnad metall i rännor smälter
  igen av ny metall.

## Odling: fas 3 (byggd)

Stenåldern har redan säd, tröskning, Porridge, bördighet och kråkor (`docs/farming.md`). Bronsåldern lägger till bröd,
gödsel och fågelskrämma. Allt kräver en **Copper Ingot eller Bronze Ingot** (C), så det kommer efter första gjutningen.
Recepten syns i receptboken när man bär en sådan tacka. Data: `scripts/gen_farming3.py`.

| Sak | Recept | Användning |
|---|---|---|
| Quern | `[ ][C][ ]` / `[Cobblestone][Stick][Cobblestone]` / `[Cobblestone]×3` | Högerklicka med Grain: 4 kvarts varv → 1 Flour |
| Baking Stone | `[R][C][R]` (R = rock chunk) | Vid brinnande Campfire: Dough → Bread på 30 s (bränns efter 45 s till) |
| Compost Heap | `[Stick][ ][Stick]` / `[Stick][Leaf Litter][Stick]` / `[Stick][C][Stick]` | 7 växtrester / Spoiled Food (Manure räknas dubbelt) → 2 min → 2 Compost |
| Scarecrow | `[C][Carved Pumpkin][ ]` / `[Straw][Raw Hide][Straw]` / `[ ][Stick][ ]` | Inga kråkor landar eller äter inom 12 block |

- **Bröd:** Grain → Quern → Flour; 2 Flour + Clay Water Bucket → Dough; Dough på Baking Stone eller i Clay Kiln
  (20 s vid 200°) → Bread. Vanillas 3-Wheat-recept finns inte.
- **Manure:** vuxna Cow, Mooshroom, Sheep, Pig, Goat, Horse, Donkey, Mule, Aurochs och Wild Horse släpper i snitt en
  Manure var 10:e minut. På farmland: bördighet +2 (Compost +1).
- **Hållbarhet:** Flour 30 dagar, Dough 1 dag, Bread 3 dagar.
- Kvar: plog. (Bronze Sickle och Barley är byggda.)

- Kvar (valfritt): bronslykta, lerkärl i handeln.

## Lin (byggt, v0.65.0)

Lin är bronsålderns trådgröda (stenålderns: brännässla, se `docs/ages/01-stone-age.md`, "Tråd"). Odlingen: `docs/farming.md`.

- **Källa: byns Farmer på handelsnivå 3** (bronsålderns nivåer): **2 Emerald → 4 Flax Seeds** (max 12, 10 xp). Valt för
  att nivå 3 redan är bronsålderns handel och lin kommer utifrån som en odlad gröda; ingen vild lin i världen.
- **Flax** på farmland, ålder 0–7 (5 ritade stadier: groddar, bladiga stjälkar, himmelsblå blommor, gyllene mogna stjälkar
  med fröknoppar). Mogen: **2 Flax Stalks** (skära: 50 % en till) + **1–2 Flax Seeds**; omogen ger ingenting.
- **Rötning och skäktning:** 4 Flax Stalks + Clay Water Bucket (eller vattenhink) + kniv → **6 Flax Fiber**; hinken blir
  tom, kniven slits 1.
- **Flax Fiber + Drop Spindle → Linen Thread** (i taggen `mechanical:threads`: syr läder vid Leatherworking Bench).
- **Advancements (Bronze Age-fliken):** Flax (Flax Seeds) → Linen (Linen Thread).
- Inte gjort: Linen Cloth (inget använder tyg än), linfröolja.

## Keramik (byggd)

Drejskiva och lerkärl: förvaring där maten håller längre, och en kruka för vatten. Kod: `Pottery` (registrering,
`JAR_RATE`), `PottersWheelBlock` + `PottersWheelBlockEntity`, `client/PottersWheelRenderer`, `StorageJarBlock` +
`StorageJarBlockEntity`, `WaterJugBlock` + `WaterJugItem`; bränningen i `KilnBlockEntity.RECIPES`. Data:
`scripts/gen_pottery.py` (efter gen_wolf_pelt.py, före gen_block_items.py) och `scripts/gen_pots.py` (vanillas krukor,
efter gen_thread.py), texturer: `scripts/gen_pottery_textures.py`.

### Potter's Wheel

Recept (crafting table, P = valfria Planks, S = Bronze Saw som blir kvar och slits 1, B = Bronze Ingot, C = Cobblestone):

```
[P][S][P]
[ ][B][ ]
[C][C][C]
```

Sågen skär ut drejskivan, bronstappen är lagret och kullerstenen svänghjulet. Bryts med yxa, ger sig själv.

- Modell: ett runt svänghjul av slät sten nere, en axel av skalad ek och ett runt plankhuvud överst. Huvudet och leran
  ritas av en block entity renderer och snurrar (~1,3 varv/s) medan man drejar.
- **Form:** smyg + högerklick med tom hand (innan man börjat dreja) väljer form: **Storage Jar (4 Clay Balls)** eller
  **Water Jug (3 Clay Balls)**, **Flower Pot (2 Clay Balls)** eller **Decorated Pot (4 Clay Balls)**. Lera som blir över när man byter till en mindre form går tillbaka till spelaren.
- **Lera:** högerklicka med Clay Ball, en i taget, tills formen har sin lera. Leran ligger som en skev klump på huvudet.
- **Dreja:** håll in högerklick med tom hand. Varje upprepning (var 4:e tick) är ett steg; **24 steg** (~5 s) ger en
  färdig råform. Leran går klump → centrerad kupol → uppdragen cylinder → öppnad med mynning → färdig form, med
  lerstänk och ljud. Släpper man fortsätter man där man var.
- **Ta av:** tom hand på den färdiga formen → **Unfired Storage Jar** / **Unfired Water Jug** / **Unfired Flower Pot** /
  **Unfired Decorated Pot** (grå rå lera).
- Bryts skivan: färdig form faller ut som den är (med inpressade sherds), annars Clay Balls tillbaka.

Råformerna bränns i Clay Kiln vid **600°**: krukor och kannor **30 s**, Flower Pot **20 s**.

### Flower Pot och Decorated Pot

Vanillas krukor görs nu på drejskivan. **Vanillas recept är borttagna** (`flower_pot` av 3 Brick, `decorated_pot_simple`
av 4 Brick och `decorated_pot` med sherds; bara ugnen ger krukorna). Brick är kvar som förut.

- **Flower Pot:** 2 Clay Balls på skivan → **Unfired Flower Pot** (vanillas form i grå lera) → kilnen (20 s, 600°) →
  vanillas **Flower Pot**.
- **Decorated Pot:** 4 Clay Balls → **Unfired Decorated Pot** (vanillas form: bred kropp, smal hals, bred kant) →
  kilnen (30 s, 600°) → vanillas **Decorated Pot**. Utan sherds blir den en vanlig slät kruka (staplas med vanillas).
- **Pottery Sherds:** medan den färdiga Unfired Decorated Pot står kvar på skivan, högerklicka med en Pottery Sherd:
  den pressas in i nästa tomma sida i ordningen **fram, vänster, höger, bak** (samma ordning som krukans tooltip), en
  sherd per sida, max 4. Sidor utan sherd blir släta. Råformen bär `pot_decorations` (syns i tooltipen) och kilnen för
  över den till den brända krukan, så mönstren syns på Decorated Pot när den ställs ut. Sherds går inte att pressa in
  efter att formen tagits av skivan (leran har torkat).
- Råformerna är vanliga items (som Unfired Storage Jar), inte block. Mönstren syns inte på råformen, bara i tooltipen.
- Advancements (Bronze Age-fliken): **Potted** (bränn en Flower Pot), **Potter's Pride** (bränn en Decorated Pot),
  **Storyteller** (pressa in en sherd på skivan, ges från koden).

### Storage Jar

Rund kruka med bred buk, smal hals och lock med knopp, bränd terrakotta med en målad sicksackbård. Ställs ut som block
(staplas 16 i inventoryt).

- Högerklick lyfter locket (syns i modellen: mynningen öppen) och öppnar **9 platser** (3×3). Allt får plats.
- **Förseglad håller maten 4 gånger så länge:** när ingen har den öppen åldras mat i den med **1/4** av normal takt
  (Bark Basket 1/3). Samma stämpel som korgen: när locket läggs på får maten tiden (`basket_since`), när det lyfts
  flyttas `spoils_at` fram med 3/4 av tiden den stod förseglad.
- Hoppers fungerar; det som tas ut eller läggs i medan den är förseglad räknas av/stämplas på vägen, så inget får en
  gammal stämpel med sig. Komparator läser fyllnaden.
- Bryts krukan räknas maten av och allt faller ut (inget sparas på item:et).
- Jämför stenålderns **Root Cellar** (`docs/farming.md`, Rotfrukter): samma lock-mekanik, men bara nedgrävd, och
  bara rotfrukter och blad får 1/4 (annan mat 1/2). Krukan fungerar var som helst och för all mat.

### Water Jug

Rund kanna med hals, pip och handtag. **Rymmer 6 hällningar** vatten (block-state `water` 0–6, följer med item:et när
den plockas upp; tooltip "Water: 3/6"). Mynningen visar tom/lite/mycket vatten, både på blocket och i handen. Staplas inte.

- **I handen:** sikta på vatten och högerklicka → full (6). Högerklicka farmland (eller en gröda på den) → den och de 8
  runt om blir blöta, som Clay Water Bucket (1 hällning). Annars ställs den ut.
- **Utställd:** Clay Bucket, Bucket eller Glass Bottle tar 1 hällning (Clay Water Bucket, Water Bucket, Water Bottle);
  Clay Water Bucket eller Water Bucket häller i 1. Så används kannan som förråd för recept som vill ha Clay Water Bucket.
- **Regn:** under öppen himmel fylls den av regnet, ungefär en hällning per minut (50 % per random tick).
- Kannan slits inte (Clay Bucket spricker efter 16).

### Beslut

- Vanillas **Flower Pot** och **Decorated Pot** drejas och bränns (v0.67.0) i stället för att craftas av Brick.
  Sherds pressas in på skivan i stället för i craftingrutnätet. Storage Jar och Water Jug är egna, funktionella kärl.
- Formen väljs med smyg-klick (inte efter antal lerbollar), så man ser vad man drejar innan leran läggs på.
- **Inte gjort (valfritt):** Clay Bowl/fat, Amphora (större förvaring), att kärl går sönder vid fall/explosion, att
  dricka ur kannan (modden har ingen törst), att hälla kannan i en kittel (Bronze Kettle fylls med Water Bucket eller
  Clay Water Bucket, inte med kannan).

## Bronze Kettle och Tripod (byggd)

En gjuten bronskittel som hänger i ett stativ över elden. Den kokar soppa av vad som helst, kokar hela fåglar, smälter
talg, kokar lim och garvar hudar. Kod: `Kettle` (registrering, värden, recept i koden), `TripodBlock`,
`TripodBlockEntity` (kitteln, vattnet, innehållet, värmen), `client/KettleRenderer` (vätskeyta och bitar). Data:
`scripts/gen_kettle.py`, texturer: `scripts/gen_kettle_textures.py`.

### Recept och gjutning

| Item | Recept | Not |
|---|---|---|
| **Tripod** | `[ ][Cordage][ ]` / `[ ][Tool Rod][ ]` / `[Tool Rod][ ][Tool Rod]` | Tre stänger surrade i toppen |
| **Unfired Clay Kettle Mold** | **Mold Bench:** 28 Clay Balls (lika mycket lera som de 7 Clay-block den kostade förr) | Bränns i kilnen (600°) till **Clay Kettle Mold** |
| **Rough Cast Bronze Kettle** | Gjuts i Clay Kettle Mold: **8 enheter brons** | Bara brons (koppar passar inte) |
| **Bronze Kettle** | Rough Cast Bronze Kettle slipas på Whetstone (4 drag) | Som alla gjutna delar |

- **8 enheter** är mest av en full degel (10): hackhuvud och svärdsblad tar 6, plåt 4, tacka 2. En kittel är det största
  man gjuter och kostar ungefär lika mycket brons som en pickaxe och en kniv tillsammans. Formen fylls både ur en
  degel och via Clay Channels från Smelting Furnace (32 kvartsenheter av poolen).
- **Poor Bronze:** kitteln bär märket vidare och är tunnare: rymmer **6** saker i stället för 8 (tooltip).

### Stativet över elden

- Tripod ställs **ovanpå en Campfire eller en Fire Pit** (högerklicka på elden uppifrån). Benen når ner till marken
  runt elden. På en Fire Pit står den på valfri del men ritas **mitt över hela 2×2** (bara ett stativ per grop), och
  kitteln hänger 4 px högre så att den går fri från spetten. Utan eld under går det inte att ställa den, och tas elden
  bort faller stativet (Tripod och kittel kommer tillbaka).
- Högerklicka stativet med en **Bronze Kettle** för att hänga upp den (kedja ner till ett handtag).
- **Värme:** tänd Campfire värmer 1×, tänd **Fire Pit 1,5×** (kokar fortare). Glöd håller den bara varm (högst 60°),
  den kokar inte. Kall eld: den svalnar. Kokpunkt 100°, ungefär 10 s över en Campfire.
- Stativet har ingen kollision och skymmer inte rök eller ljus. Firewood läggs på elden genom att klicka på elden, inte
  på stativet.

### Använda kitteln

1. **Vatten:** Water Bucket (ger tillbaka hinken) eller Clay Water Bucket (slits 1, som vid alla hällningar).
2. **Ingredienser:** högerklicka med en sak i taget, upp till **8** (Poor: 6). **Det första som läggs i bestämmer
   läget**, och saker som inte hör dit tas inte emot (meddelande). Ruttnande mat tas inte emot.
3. **Sjuda:** när vattnet kokar räknas tiden: mat 90 s, rendering 2 min, garvning 3 min över en Campfire
   (Fire Pit: två tredjedelar av tiden). Läggs något nytt i börjar tiden om.
4. **Ta ut:** skålar öses med en **Bowl**; fasta saker (talg, lim, läder, kokta fåglar) tas med **tom hand**. När allt
   är uttaget är kitteln tom igen.

| Kontroll | Vad som händer |
|---|---|
| Tom hand | Tar ut det som är klart, annars status i chatten: läge, antal, ingredienser, värme och tid kvar |
| Smyg + tom hand | Häller ut allt (innehållet går förlorat); är kitteln redan tom tas den ner |
| Riva stativet | Tripod och Bronze Kettle kommer tillbaka, innehållet spills ut |

Läget syns också på vätskans färg: blått vatten, soppa i en brun ton dragen mot ingredienserna (bär rödlila, morot
orange, fisk ljus, svamp gråbrun), mjölkvit fettlake vid rendering, mörkt rödbrun barklake vid garvning. När det kokar
bubblar och ångar det, och upp till fyra av ingredienserna flyter och guppar i ytan.

### Soppa (Stew)

Allt som går att äta utan bieffekter får ligga i (kött, hela plockade fåglar, fisk, grönsaker, svamp, bär, nötter,
ägg), plus grain, Flour, Brown/Red Mushroom och Boiled Egg. Inte: ruttet kött, Pufferfish, Spider Eye, Golden Apple,
rätter i egen skål (Porridge, Mushroom Stew) eller soppa och buljong själva.

- **Värde:** varje ingrediens räknas som det den blir på en Campfire (Raw Beef räknas som Steak, Plucked Goose som
  2 Cooked Goose, ägg som Peeled Egg). Summan av hungern × **1,25 + 0,05 per olik ingrediens** (högst
  **1,5×**), delas på skålar à ungefär 7 hunger (1–6 skålar), avrundat nedåt. En skål är en måltid: mättnad = hunger × 2,4
  (`FoodValues.MEAL`, se 01-stone-age.md "Mättnad").
- **Exempel:** 2 Raw Beef + 2 Carrot + 1 Potato: stekt 8 + 8 + 3 + 3 + 5 = 27 hunger, tre olika ×1,35 →
  36,5 → **5 skålar à 7 hunger, 16,8 mättnad** (35 hunger i stället för 27 stekt).
- **Stew** stackar inte, har sitt värde på sig (FOOD-komponent) och listar ingredienserna i tooltip. Förskäms på
  **1 dag**; skålen kommer tillbaka när man ätit.

### Hela fåglar (Broth)

Bara plockade fåglar (Plucked Goose, Pheasant, Ptarmigan, Pigeon, Chicken) i vattnet: när de kokat kommer fåglarna upp
**kokta** (samma kött som stekta hela på en Campfire, tom hand) och lämnar **2 skålar Broth per fågel** (högst 8).
Broth: 4 hunger, förskäms på 1 dag. Blandas fåglarna med annat blir det vanlig soppa (fågeln räknas som sitt kokta kött).

### Rendering: talg och Hide Glue

| I kitteln | Ger | Jämför |
|---|---|---|
| Lard | 2 Tallow | 1 på Campfire |
| Bear Fat | 3 Tallow | 2 på Campfire |
| 2 Bone | 1 Tallow (benfett) | nytt |
| Raw Hide | 3 **Hide Glue** | |
| Rabbit Hide | 1 Hide Glue | |
| 2 Fish Bones | 1 Hide Glue | |

Udda ben och fiskben blir över och går förlorade. **Hide Glue** binder som Cordage:
- **Tool Station:** alla verktyg med Cordage som bindning (Stone Pickaxe/Axe, Flint Spear, Tusk Dagger, koppar- och
  bronsverktyg utom kniv och såg, Bronze Spear, Bronze Sword) kan skaftas med Hide Glue. Klick i listan tar Cordage om
  man har, annars lim.
- **Sinew Bow:** Hide Glue i stället för Cordage i Fletching Table.
- **Bellows:** `[Planks][Scraped Hide][Planks]` ×2 / `[Hide Glue][Clay Tuyere][Hide Glue]` (i stället för Twine).

### Garvning

Bark och **Scraped Hide** i vattnet: varje hud som får en Bark garvas, och **2 garvade hudar ger 3 Leather**. Hudar
utan bark kommer tillbaka som Scraped Hide. Till skillnad från Drying Rack behövs varken sol eller torrt väder, och
upp till 4 hudar (med 4 Bark) garvas på en gång.

### Blodkittel under Hanging Rack

En **Bronze Kettle** (eller en järn-**Cauldron** i järnåldern) kan också ställas under en Hanging Rack: högerklicka
ställningen med kitteln. Den står på marken under kroken (vriden så att öronen går fria från stolparna) och fångar blodet
från kroppen som hänger där: dropparna från en flådd kropp och blodet när den styckas. Så länge kitteln har plats blir det
**ingen blodpöl** runt ställningen; först när den är full (**1000 mB**, en hink) rinner det över på marken som förut.
Blodytan syns i kitteln och stiger i 20 steg (50 mB styck). Kod: `HangingRackBlock` (`KETTLE`, `KETTLE_BLOOD`,
`catchBlood`), `CarcassEntity.spill`; modeller `rack_kettle_*` i gen_kettle.py, blockstate i gen_carcass_hauling.py.

| Kontroll | Vad som händer |
|---|---|
| Bronze Kettle / Cauldron på ställningen | Ställer kitteln under kroken (Poor Bronze kommer tillbaka som Poor Bronze) |
| Tom hand | Visar hur mycket blod kitteln har (x / 1000 mB) |
| Bucket / Bronze Bucket | Öser upp en **full** kittel: **Blood Bucket** / **Bronze Blood Bucket** (inte full: visar mängden) |
| Smyg + tom hand | Tar ner kitteln; blodet i den spills ut på marken |
| Riva ställningen | Kitteln släpps som item, blodet går förlorat |

En kropp av hjortstorlek som flås och styckas på ställningen ger ungefär en full kittel (fler droppar om den får hänga
flådd en stund innan den styckas).

**Blood Sausage (blodkorv):** Blood Bucket eller Bronze Blood Bucket + 2 Flour + Lard (formlöst) → **4 Raw Blood
Sausage** (hinken kommer tillbaka). Stekt på Campfire (20 s) eller i Smoker → **Blood Sausage**: 6 hunger, mättnad 0,8.
Rå förskäms på 2 dagar, stekt på 10. Går också i soppan som annan mat.

### Säkert mot dubblering

Ingredienserna försvinner när de läggs i och räknas om till resultat **en gång**, när kitteln är klar. Varje skål tar
en Bowl, fasta resultat tas ut en gång, och att riva eller tömma kitteln ger aldrig tillbaka innehållet. Talg, lim,
läder och soppa går inte att lägga i igen.

### Advancements

- **Bronze Age:** *Kettle* (efter Bronze!: ha en Bronze Kettle), *Sticky Business* (Hide Glue), *Tanner* (ta ut garvat
  läder, ges från koden), *Not a Drop Wasted* (efter Kettle: ha en Blood Bucket eller Bronze Blood Bucket).
- **Food & Farming:** *Hearty Stew* (efter Cooked Meal: ös upp en soppa med 4 eller fler ingredienser, ges från koden),
  *Black Pudding* (efter Cooked Meal: ha en Blood Sausage).

### Beslut

- Kitteln färdigställs på Whetstone som alla gjutna delar. Poor Bronze: mindre kittel (6), inget läckage.
- Ett läge per kittelfyllning (första ingrediensen), så resultatet är tydligt.
- **Inte gjort (valfritt):** färgning med pigment i kitteln (ull, läderrustning, banderoller), örter (finns inga än),
  att fylla kitteln med Water Jug.

## Fler bronsverktyg (byggda, v0.66.0)

Hink, skära, sax, fiskekrok, nål, tång och mejsel i brons. Alla görs som de andra bronsverktygen: en lerform formas i
**Mold Bench** och bränns i kilnen (600°), brons hälls i (ur degeln eller via Clay Channels), gjutningen slipas på
**Whetstone** (4 drag) och sätts ihop i **Tool Station**; saxen och nålarna är färdiga direkt från slipstenen. Formarna
tar bara brons och tål 5 gjutningar. Recepten i Tool Station låses upp när man bär den slipade delen, så allt kräver brons.
Kod: `BronzeTools` (registrering, mjölkning, cauldrons, skärans svep, tång och brännskador, mejseln, gametest),
`BronzeBucketItem`, `BoneHookRodItem` (nu med lure/luck per stav), formarna i `ClayMoldBlock.Shape`/`Metal`, bränning i
`KilnBlockEntity`, formrecepten i `MoldBench`, monteringen i `ToolStationRecipes`. Data: `scripts/gen_bronzetools.py`,
texturer: `scripts/gen_bronzetools_textures.py`.

### Formar och gjutning

| Form (Mold Bench) | Clay Balls | Enheter brons | Gjutning → slipas till | Tool Station |
|---|---|---|---|---|
| Clay Bucket Mold | 8 | 6 | Rough Cast Bronze Bucket → **Bronze Bucket Body** | Body + Cordage (hänklet) + Stick → **Bronze Bucket** |
| Clay Sickle Blade Mold | 5 | 4 | Rough Cast Bronze Sickle Blade → **Bronze Sickle Blade** | Blade + Twine + Stick → **Bronze Sickle** |
| Clay Shears Mold | 5 | 4 | Rough Cast Bronze Shears → **Bronze Shears** (färdig) | – |
| Clay Fish Hooks Mold | 3 | 2 | Rough Cast Bronze Fish Hooks (4 krokar på en kanal) → **4 Bronze Fish Hook** | Hook + Twine + Tool Rod → **Bronze Hook Fishing Rod** |
| Clay Needles Mold | 3 | 2 | Rough Cast Bronze Needles (4 nålar på en kanal) → **4 Bronze Needle** (färdiga) | – |
| Clay Tongs Mold | 6 | 4 | Rough Cast Bronze Tongs → **Bronze Tong Arms** | Arms + Twine + Leather (grepp mot värmen) → **Bronze Tongs** |
| Clay Chisel Blade Mold | 4 | 2 | Rough Cast Bronze Chisel Blade → **Bronze Chisel Blade** | Blade + Twine + Stick → **Bronze Chisel** |

- **Poor Bronze** följer med som vanligt: verktygen får 70 % hållbarhet. En Poor Bronze-hink bär vatten och mjölk lika bra.
- Hinkens bindning är Cordage, så Hide Glue duger i stället (som för andra verktyg med Cordage).

### Bronze Bucket

- **Spricker aldrig** (Clay Bucket spricker efter 16 hällningar). Staplas till 16 tomma.
- Öser upp **vatten** (källblock, även ur vattenfyllda block) och häller ut det som vanilla-hinken: **Bronze Water
  Bucket**. **Lava tar den inte** (brons håller inte för det), och inte järnhinken (vanilla-hinken) heller.
- **Mjölkar** kor, mooshrooms och getter → **Bronze Milk Bucket** (drick: tar bort alla effekter, hinken blir kvar).
- **Cauldrons:** häll i vatten, ös upp en full vattencauldron (ingen hink öser ur en lavacauldron).
- Bronze Water Bucket fungerar överallt där Clay Water Bucket gör: **kitteln** (Tripod), **Water Jug** (fyll och töm),
  **recept** (Dough, Raw Porridge, färgernas bindemedel, rötning av lin till Flax Fiber; hinken blir kvar i rutnätet)
  och **farmland** (blöter 3×3). Bronze Milk Bucket går i **Cake** i stället för Milk Bucket.
- Generatorn lägger brons-hinken bredvid Clay Water Bucket / Milk Bucket i varje recept som tar dem, så nya recept får den
  automatiskt.
- **Inte gjort:** dispensrar häller inte bronshinkar (vanilla-hinkens dispenser-beteende gäller bara vanilla-hinkar).
  Mjölk och kaka lämnar en vanlig Bronze Bucket (en Poor Bronze-hink blir hel igen där; litet hål, avsiktligt kvar).

### Bronze Sickle

- Flint Sickles verktyg i brons: **460 hållbarhet** (flinta 48), järnets fart.
- **Svep 3×3:** skär du ett moget sädesstrå (eller gräs, nässlor, vild säd ...) går de 8 runt om på samma höjd med samma
  drag, av samma sort (mogna grödor tar bara mogna grödor, gräs tar gräs). Omogna grödor rörs inte. **Smyg** för att skära
  ett i taget. Varje mogen gröda sliter skäran 1, som förut.
- Alla skärbonusar gäller (taggen `#mechanical:sickles`): extra kärve (80 %, flintskäran 50 %) och Straw, Flax Stalks, vild säd, morot och
  strandbeta med kniv-eller-skära.

### Bronze Shears

- Vanilla-saxen i brons (fjädersax i ett stycke): klipper får, behåller löv, spindelväv, gräs, pumpor, bikupor,
  snubbeltråd. **476 hållbarhet** (järnsaxen 238). Vanillas predikat `minecraft:tool/can_shear` (lövens och gräsets
  loot) tar nu båda saxarna.

### Bronze Hook Fishing Rod

- Napp som med **Lure II** (10 s tidigare; Bone Hook-staven är Lure I) och fångst som med **Luck of the Sea I** (mer
  skatter, mindre skräp), ovanpå förtrollningar. **128 hållbarhet** (benkroken 48). Förtrollningsbar som andra spön.

### Bronze Needle

- Syr vid Leatherworking Bench som Bone Needle men tål **250** (benet 32). Bänkens nålruta tar taggen `#mechanical:needles`
  (Bone Needle, Bronze Needle), liksom receptet för själva Leatherworking Bench.

### Bronze Tongs och brännskador

- **Ny regel:** smält metall bränns. En **Crucible of Molten Metal i handen** (huvudhanden eller offhand) bränner ½ hjärta
  varannan sekund; i resten av inventoryt bränner den inte. En **färsk gjutning** som tas ur en lerform bränner ½
  hjärta. Skadetypen `mechanical:hot_metal` ("was burnt by molten metal") räknas som eld, så Fire Resistance skyddar.
- **Håll Bronze Tongs i någon av händerna:** inga brännskador. Tången slits 1 per gjutning den tar ur en form.
  250 hållbarhet. Med degeln i huvudhanden och tången i offhand häller man som vanligt; med tången i huvudhanden
  högerklickar man formen för att ta ut gjutningen.
- Skadan är liten med avsikt: innan man har brons (de första koppargjutningarna) får man ta några brännskador.

### Bronze Chisel

- Högerklicka sten: den huggs **ett steg** längre, 1 hållbarhet per hugg (320 totalt):
  Stone → Smooth Stone → Stone Bricks → Chiseled Stone Bricks; Sandstone → Cut Sandstone → Chiseled Sandstone (och röd);
  Andesite/Diorite/Granite → Polished; Cobbled Deepslate → Polished Deepslate → Deepslate Bricks → Chiseled Deepslate;
  Tuff → Polished Tuff → Tuff Bricks → Chiseled Tuff Bricks; Blackstone → Polished → Polished Blackstone Bricks →
  Chiseled Polished Blackstone; Nether Bricks, Quartz Block, Resin Bricks → Chiseled.
- **Rensa gjutningar:** med mejseln i andra handen vid Whetstone slår man av gjutskägget först: en gjutning slipas på
  **2 drag** i stället för 4 (mejseln slits 1 per drag). Utan mejsel fungerar slipstenen som förut.

### Vanilla

- **Järnhinken och järnsaxen** är kvar men kräver järntackor, som inte går att få före järnåldern. Bronze Bucket och
  Bronze Shears är bronsålderns hink och sax. Vanilla-hinkar som hittas (de filtreras ur loot) fungerar som förut.
- `minecraft:tool/can_shear` skrivs över (shears + bronze_shears). Cake-receptet tar Bronze Milk Bucket som alternativ.

### Advancements (Bronze Age-fliken, efter Bronze!)

*Bronze Bucket*, *Reaper* (Bronze Sickle), *Shearer* (Bronze Shears), *Bronze
Hook* (Bronze Hook Fishing Rod), *Fine Needlework* (Bronze Needle), *Tongs* (Bronze Tongs), *Stonemason* (Bronze Chisel).

### Test

Gametest `mechanical:bronze_tools`: bronshinken lämnar lava orörd, vatten öses
upp, Dough med Bronze Water Bucket lämnar hinken kvar, Bronze Sickle skär 3×3 mogen vete med ett drag (omogen utanför
står kvar), degeln i handen bränner utan tång men inte med tång i andra handen, mejseln gör Stone Bricks till Chiseled
Stone Bricks och slits 1, nålgjutningen ger 4 nålar.

## Yew Longbow (byggd, v0.128.0)

Bronsålderns tunga båge, av idegran. Horn Bow är fortfarande den snabba bågen, Yew Longbow den tunga.

### Recept

- **Yew Stave** (crafting, shapeless): 1 **Yew Rough Log** + en såg (**Bronze Saw**, i järnåldern även Iron/Steel Saw) → 1 Yew Stave. Sågen stannar kvar i
  rutnätet och tappar 1 hållbarhet (som Sawn Planks). Staplas till 16.
- **Yew Longbow** (Tool Station): **Sinew** (bågsträng) + **Leather** (grepp) + **Yew Stave**. Receptet låses upp när
  man bär en Yew Stave.

### Egenskaper

| | Sinew Bow | Horn Bow | Yew Longbow |
|---|---|---|---|
| Pilfart (skada och räckvidd) | ×1.0 | ×1.2 | **×1.3** |
| Full dragning | 1.0 s (20 ticks) | 1.0 s | **1.5 s (30 ticks)** |
| Hållbarhet | 200 | 384 | **450** |

- Pilens skada följer farten, så pilarna gör ungefär 30 % mer skada och flyger längre än från en vanlig båge, oavsett
  pil. Spridningen är som en vanlig båges (Horn Bow är träffsäkrare).
- Dragkurvan är vanillas, utsträckt över 30 ticks: släpper man efter 1 s har bågen bara ~57 % kraft.
- Förtrollas som en båge (Power, Punch, Flame, Infinity, Unbreaking, Mending).
- Tooltip visar Arrow damage 130 %, Arrow speed ×1.3 och Full draw 1.5 s. Modellens dragsteg följer den längre
  dragningen. (Kamerans zoom vid dragning är vanillas och gäller bara vanilla-bågen.)

### Advancement

*Longbowman* (Bronze Age-fliken, efter *Saw*): såga en Yew Stave och stränga den i Tool Station.

### Test

Gametest `mechanical:bronze_tools`: Yew Rough Log + Bronze Saw ger Yew Stave och sågen stannar kvar, Tool Station-receptet
(Sinew + Leather + Yew Stave) finns, full kraft först efter 30 ticks (20 ticks ger under 0.9), fartfaktorn är större än
Horn Bows, 450 hållbarhet.

## Fältredskap (byggda, v0.131.0)

Odlingen gick för långsamt. Grödor växer nu **dubbelt så fort per tick** och gödslad jord (bördighet 3) 1,5× till
(docs/farming.md "Växtvillkor" och "Bördighet": vete ~35–65 min spel, gödslat ~25–50 min, förut 1–2 h), och bronsåldern
får två redskap som gör en hel ruta åt gången (docs/farming.md "Fältredskap").

### Seed Bag

```
[        ][Cordage ][        ]
[Leather ][   C    ][Leather ]
[        ][Leather ][        ]
```
C = Copper eller Bronze Ingot (ringen som håller öppningen öppen). Högerklicka i luften eller smyg-högerklicka: en 3×3-meny
(som Bark Basket) som bara tar utsäde, allt som sätts som item och blir en gröda eller stjälk: Grain, Wheat Grain, Peas,
Flax Seeds, Wild Carrot, Carrot, Potato, Beetroot Seeds, Melon- och Pumpkin Seeds. Högerklicka farmland eller en gröda
på den: varje tom farmland i **3×3** får ett frö, det första i påsen som kan växa där. Receptboken visar den när man bär
en ingot.

### Bronze Ard

Tool Station: **Bronze Hoe Head + Bronze Plate + Tool Rod** (en årder: hackhuvudet som bill, plåten som sko under den,
bommen). Låses upp när man bär ett Bronze Hoe Head. Plöjer det klickade blocket och de 8 runt omkring på samma höjd,
med hackans regler (gräs, dirt, Podzol, Loess blir farmland bara med luft ovanför, Coarse Dirt blir Dirt); smyg: bara
ett. **Hållbarhet 300**, 1 per plöjt block. Tung: attackfart −3,2. Räknas som hacka (`#minecraft:hoes`, förtrollas som en).
Den dragna plogen kommer i järnåldern.

### Advancements (Bronze Age-fliken, efter *Bronze!*)

- **Broadcast Sowing**: ha en Seed Bag. **Breaking Ground**: ha en Bronze Ard.

### Test

`FarmToolsTest`: faktorerna (2× på ny åker, 1,5× till på bördighet 3, inget på vintern), mognadstiden mätt genom
`randomTick` (11,8 / 9,0 random ticks mot vanillas 21), påsen sår 3×3 och drar ett frö per block, årdern plöjer 3×3 men inte
under ett block och slits 8, smygande ett.

## Vagn och häst (byggd)

Bronsålderns transport: travoisen får hjul, och vildhästen kan tämjas och dra vagnen. Kod: `Carting` (registrering,
taggar, Rope Halter), `CartEntity` (bygger på `TravoisEntity`), `wildlife/WildHorse` (förtroende och tämjning),
`wildlife/TameHorse` (den tama hästen), `AnimalFear` (hästen skyggar inte för den som matar), klient: `client/CartModel`,
`client/CartRenderer`, `client/wildlife/TameHorseRenderer`. Data: `scripts/gen_cart.py` (efter gen_pottery.py, före
gen_block_items.py), texturer: `scripts/gen_cart_textures.py`.

### Recept

| Sak | Recept | Låses upp av |
|---|---|---|
| **Wooden Wheel** | `[Planks][Tool Rod][Planks]` / `[Tool Rod][Bronze Ingot][Tool Rod]` / `[Planks][Bronze Saw][Planks]`, sågen blir kvar och slits 1 | Bronze Saw |
| **Wheeled Cart** | `[Tool Rod][ ][Tool Rod]` / `[Planks][Planks][Planks]` / `[Wooden Wheel][Planks][Wooden Wheel]` | Wooden Wheel |
| **Rope Halter** | `[Cordage][ ][Cordage]` / `[ ][Cordage][ ]` | Bronze Ingot |
| **Hide Saddle Blanket** | `[Leather][Leather][Leather]` / `[Cordage][ ][Cordage]` | Rope Halter |

### Wheeled Cart

Plankbädd med sidobräden, gavlar och hörnstörar på en axel med två ekrade hjul (navet en gjuten bronshatt, tolv
fälgbitar med bronsspik), och två skaklar framåt. Hjulen rullar med farten. Ställs ut och plockas upp som travoisen
(högerklicka på marken, slå på den: last och varor faller ut).

- **Dra själv:** högerklicka med tom hand (igen för att släppa). Den följer **2,6 block** bakom, mellan skaklarnas spetsar.
  Fart **−5 % tom, −3 % per plats last** (travois −10 % + −7 %), alltså −23 % fullastad. Spring går med upp till 3
  platser last (en stor eller en mellanstor + en lätt); mer än så går inte.
- **Kostar mat** när du drar själv, per block du går: **0,01 utmattning tom, upp till 0,08 full** (en vagn med kista: efter
  hur många av kistans platser som är fyllda). 300 block med full vagn kostar ~24, ungefär tre klubbor. Drar en häst
  eller onager betalar du inget.
- **Häst:** titta på kärran och tryck på greppnyckeln (**H**, kan ändras) när du **rider** en tam häst, eller när du **leder** en tam häst i koppel (inom 10
  block): hästen spänns för och vagnen följer **2,0 block** bakom den. Hästen saktas inte ner. Tryck igen, eller
  **smyg + högerklicka hästen**, för att spänna från. Skaklarna är fasta: backar hästen skjuts vagnen bakåt. En spänd häst
  som ingen rider eller leder **står still** (den strövar inte iväg med vagnen). Hästen (eller vagnen den dras efter)
  **sparas** med kärran och hittas igen när världen laddas om. Föl kan inte dra. En tämjd onager (Tame Onager) drar på samma sätt.
- **Ett fordon åt gången:** en häst eller spelare som redan drar en travois, kärra, vagn eller plog kan inte spännas för
  ett till ("That one already pulls something: unhitch it first").
- **Last:** **6 platser** (lätt kropp 1, mellanstor 2, stor 3, jättar går inte; se tabellen i 01-stone-age.md):
  två stora, tre mellanstora, en stor + en mellanstor + en lätt, eller sex lätta. Lasta som på travoisen (högerklicka med
  kroppen du bär eller släpar; det går också att klicka på en kropp som redan ligger på vagnen, den täcker nästan hela
  flaket). Kropparna ligger på längden **sida vid sida** över flaket, var och en så bred som sina platser (0,22 block per
  plats), gruppen centrerad. **Ta av en kropp:** smyg + högerklicka på kroppen (som på travoisen).
- **Tama djur:** led kor, får, grisar eller höns i koppel (entitetstaggen `mechanical:domestic`, inom 10 block) och
  högerklicka vagnen med tom hand: de kliver upp så länge det finns plats. **Höna eller unge 1 plats, gris eller får 2,
  ko 3**, i samma 6 platser som kropparna. Kopplet knyts fast i vagnen och håller dem kvar; klipps eller brister det
  hoppar djuret av. **Ta av ett djur:** smyg + högerklicka på det med tom hand, så leder du det i kopplet igen.
  Travoisen tar inga levande djur. Advancement **Market Day**. Test: `mechanical:livestock_cart` (WagonTest).
- **Förråd:** **högerklicka på vagnen** med tom hand öppnar **18 platser** (2 rader), som en kista. Sparas med
  vagnen.
- **Kärra med kista (storage cart):** högerklicka en **tom** vagn med en vanlig **Chest** så monteras kistan på flaket
  (syns på vagnen). Högerklicka, med vad som helst i handen, öppnar då **54 platser** (6 rader; förrådets 18 ingår). Kistan tar hela flaket:
  inga kroppar ("The chest takes up the whole bed"). Ligger det kroppar på vagnen måste de av först. Kistan går inte att
  ta av; slå sönder vagnen så faller vagnen, kistan och allt innehåll ut. Hästen eller onagern drar den som vanligt.
- Rovdjur: den som drar en lastad vagn (spelare eller häst) blir bytet, som med travoisen.
- **Släpvagn bakom en Wagon (järnåldern):** ställ kärran nära bakom en Wagon och tryck på greppnyckeln (**H**) med blicken på den, så kopplas
  den efter vagnen. Se *Släpvagn* under Vagnen i 03-iron-age.md.
- **Rörelse som travoisen** (se *Bära hem bytet* i 01-stone-age.md): tyngdkraft, ettblockssteg, jämn rörelse i klienten.
  Fastnar vagnen håller den emot dig (eller hästen) vid dragavståndet + 1 block; drar du bort från den i 3 sekunder, eller
  kommer mer än 5 block bort, lossnar den ("The cart is stuck. It came loose from the shafts" / "The cart came loose from
  the shafts").
- **Lutning:** vagnen tippar kring axeln så att skaklarnas spetsar når höften på den som drar (en häst: bogen, 60 % av
  höjden). **Parkerad** vilar den på hjulen med skaklarna på marken (bädden lutar ~16° framåt). Lasten lutar med bädden.

### Tämja vildhästar

Wild Horse är mycket skygg. Tämjningen sker med tålamod, inte genom att rida in den.

1. **Smyg fram med foder i handen** (tagg `mechanical:horse_feed`: Barley/Einkorn/Emmer Grain och Sheaf, Wheat, Hay
   Bale, Apple, Carrot). Hästen flyr inte från någon som smyger med foder framme (de andra i flocken gör det).
2. **Mata ur handen:** högerklicka med fodret. Varje matning är **1 förtroende**, högst en gång per **10 sekunder**
   ("still chewing" annars). Hjärtan visas och förtroendet står i meddelandet (t.ex. 2/6). Står du upp när du försöker
   innan den litar på dig skenar den.
3. **Vid 3 förtroende** flyr den inte längre när du går nära (men om du springer), och tar inget mer foder förrän den
   har grimma: sätt på en **Rope Halter** (högerklicka). Grimman blir ett koppel och hästen följer med. Med mindre än 3
   vägrar den och skenar.
4. **Mata vidare i grimman** till **6 förtroende**: då blir den tam (vanilla-hjärtan) och ägs av dig. Den behåller
   grimman/kopplet, namn och hälsa. (Tas kopplet av blir det en vanlig Lead.)
- Slår du hästen tappar den allt förtroende. Föl kan inte tämjas (de följer sin mor).
- Förtroendet sparas på hästen (`Trust`, `FeedCooldown`).

### Den tama hästen

`mechanical:tame_horse` är en vanilla-häst i grunden (ridning, hopp, inventory, hästrustning, avel) men behåller
vildhästens utseende (vildhästmodellen och -texturen; föl med fölmodellen). Den äter gräs med huvudet nere och stegrar
sig på bakbenen.

- **Hälsa 20–26, fart 0,2–0,3, hopp 0,5–0,75** (en vanilla-häst: 15–30, 0,11–0,34, 0,4–1,0).
- **Rope Halter** på en tam häst/åsna/onager som inte redan har koppel: den leds som med en Lead (i stället för att du sitter upp).
- **Barbacka:** högerklicka för att rida, den styrs **utan sadel** men går i **80 %** av sin fart och hoppar **70 %**
  så högt.
- **Hide Saddle Blanket:** läggs i sadelplatsen (smyg + högerklicka på hästen, eller högerklicka med filten): full fart
  och fulla hopp. Syns som en sydd hudfilt med en gjord av rep över ryggen. Tas av i hästens inventory. Bara den tama hästen
  kan bära den.
- **Avel:** sker av sig själv: två nöjda, helt friska tama hästar vid ett Feeding Trough med vatten och foder (se
  stenåldern, *Naturlig avel*). Fölet blir en **tam** häst med samma ägare som stoet. Mat ur handen startar aldrig avel;
  foder läker 2 hälsa och får föl att växa. Onagrer och kameler avlar på samma sätt.
- Släpper Manure som andra hästar. Flyr inte (tam).
- **Inventory (egen skärm):** smyg + högerklicka (eller E när du rider) öppnar hästens skärm: sadelplats och rustningsplats
  till vänster, sedan vänster satchel, hästen och höger satchel. (Vanillas hästskärm har bara plats för 5 kolumner.)
- **Leather Horse Armor:** sys på **Leatherworking Bench**: 7 Leather + needle + 4 Thread (valfri tråd). Låses upp av
  Leather, som de andra läderföremålen. Läggs i rustningsplatsen och ger vanillas 3 armor. Syns som en ljus läderbarding över rygg,
  flanker, hals, panna och överben, **färgad efter rustningens färg** (färga den som vanlig läderrustning; ofärgad är den
  läderbrun). Vanillas recept på crafting table är fortfarande avstängt. Andra hästrustningar ritas med samma läderbarding
  tills de får egna texturer.
- **Saddlebags:** högerklicka en tam, vuxen häst med en **Leather Satchel**: den första hänger på **vänster** sida och den
  andra på **höger** sida. Högst 2. Varje satchel ger 3×3 platser i hästens skärm, och det som låg i satcheln flyttas in.
  Där går det in samma saker som i en satchel (inga andra väskor eller korgar). **Shears** tar av den senast fastsatta
  satcheln, med innehållet kvar i den. Om hästen dör släpper den satchlarna packade. Advancement **Saddlebags**.
- **Ork (stamina):** varje häst har en ork-mätare, 0–100, som sparas på hästen (gäller alla hästar, även vanillas). Den
  töms per block hästen går: ungefär 1300 block med en ryttare. **Last** tömmer den fortare (×(1 + 3 × last)) och tar
  lite fart (upp till 10 %): ryttare 0,25, kroppar på sidorna 0,15 per rum, en Wheeled Cart 0,2 tom och 0,7 full, en
  Wagon 0,4 + hur full den är (delat på två i ett par), annat som hästen drar 0,3. Under **25** är hästen trött och går
  30 % långsammare. På **0** är den helt slut: den stannar, **kastar av ryttaren** och **lägger sig**, och går inte att
  rida förrän den har vilat upp sig till **30**. Står den still vilar den (0 till full på 50 s). **Vatten** fyller på
  fort: högerklicka hästen med en **Water Bucket** (järn eller brons, +40, hinken blir tom), eller låt den stå i grunt
  vatten där den når botten utan att simma (+5 var halv sekund). Mätaren syns som en stapel ovanför rustningsraden
  medan du rider en häst eller sitter på en vagn (ett hästhjärta före varje stapel; grön, gul under hälften, röd när
  hästen är trött och blinkande mörkröd när den ligger). Advancement **Water Stop**: ge en trött häst vatten ur hink.
- **Förspänd häst:** en häst som drar något (Wheeled Cart, Wagon, plog) **vänder långsamt**: 40° i sekunden stillastående,
  upp till 70° i sekunden i gång, så att ekipaget svänger i sin egen takt (du kan titta runt fritt, hästen kommer efter).
  Riden går den högst ungefär 7 block i sekunden (fart 0,16). Gäller även en förspänd häst som en spelare leder i koppel.
- **Spänn för från sadeln:** rid fram till en kärra, vagn eller plog (högst 4 block, eller titta på den): rutan vid
  hårkorset visar **H Attach horse**, och greppnyckeln spänner för hästen du rider.
- **Kroppar på sidorna:** bär du en kropp på axlarna (upp till ett fårs storlek; tyngre kan bara släpas) och högerklickar en tam, vuxen häst
  hänger den på hästens **vänstra** sida, nästa på den **högra**. Högst en per sida. Den hänger med huvudet nedåt mot
  flanken och följer med hästen (sparas över omladdning). Smyg + högerklicka kroppen med tom hand för att ta den på
  axlarna igen. Dör hästen faller kropparna av. De räknas som last för orken. Advancement **Pack Horse**: en på varje sida.

### Beslut

- Den tama hästen är en egen entitet (vanilla `Horse` som underklass) med vildhästens modell, i stället för att byta till
  en vanilla-häst med annan färg.
- Ingen sadel i bronsåldern: barbacka fungerar, filten gör den bättre. Vanillas Saddle (järn) fungerar också men syns inte
  på modellen.
- Satchlarna lagras i hästens eget inventory medan de sitter på (3 kolumner per satchel), så att vanillas logik för
  inventory, tappade saker och sparning går att använda. Innehållet packas tillbaka i satcheln när den tas av.
- **Inte gjort:** vagnen visar inte varorna i förrådet; kistan kan inte tas av vagnen; ingen tvåhästvagn.

## Bloomery (byggd, v0.76.0)

Bronsålderns sista steg: en liten blästugn av eldfast tegel som reducerar järnmalm till en glödande järnlupp, och ett
gjutet bronsstäd där luppen smids till smidesjärn medan den är het. Kod: `Ironworking` (registrering, värden,
gametest), `BloomeryHearthBlock` + `BloomeryBlockEntity` (struktur, värme, smältning; samma GUI som kilnen via
`KilnMenu.bloomery`), `BloomeryStackBlock`, `IronBloomItem` (het/kall lupp), `BronzeAnvilBlock` + `BronzeAnvilBlockEntity`
(bygger på `StoneAnvilBlockEntity`, ritas av `client/AnvilRenderer`). Ändrat: `KilnBlockEntity` (Fire Brick, formarna,
omvärmning), `KilnMenu`/`client/KilnScreen` (termometer med 1500°-skala), `BellowsBlock`, `FireDrillItem`, `Tinder`,
`BronzeOres.crushed` (Brick → Grog), `ClayMoldBlock.Shape`, `Metal`, `MoldBench`, `ToolStationRecipes`, `BronzeTools`
(heta luppar bränner), `StoneAnvilBlock`. Data: `scripts/gen_bloomery.py` (efter gen_pots.py, före gen_block_items.py),
texturer: `scripts/gen_bloomery_textures.py`.

### Järnmalm och Raw Iron

- **Iron Ore kräver Bronze Pickaxe** (taggen `mechanical:needs_bronze_tool`, som förut): koppar-, sten- och flinthackor
  bryter den utan drop. Ger vanillas **Raw Iron**.
- **Raw Bog Iron** (myrmalm, grävs med spade i gölar) smälts också i Bloomery: 6 st per lupp. Se *Bog Iron* i
  [03-iron-age.md](03-iron-age.md).
- **Raw Iron smälts bara i Bloomery.** Vanillas sex ugnsrecept (`iron_ingot_from_smelting/blasting_raw_iron`, `..._iron_ore`,
  `..._deepslate_iron_ore`) är avstängda, Clay Kiln och Smelting Furnace har inget recept för den, den går inte i en
  Crucible och krossas inte på Stone Anvil. Lägerelden har inget recept heller.
- **Temperaturen spärrar också:** Clay Kiln och Smelting Furnace når högst 1300° med Charcoal och Bellows, och järnet
  reduceras först vid **1350°**, som bara Bloomery når.
- Järnverktyg och järnrustning är fortfarande avstängda, och vanillas järntackor går inte att få: Bloomery ger
  **Wrought Iron Ingot**, ett eget item, så hinkar, saxar, städ m.m. av järn väntar på järnåldern.

### Eldfast tegel

| Vad | Hur |
|---|---|
| Grog ×2 | En Brick på **Stone Anvil**, 4 slag med hammarsten (krossat tegel, chamotte) |
| Fire Clay ×4 | 2 Clay Ball + 2 Grog (formlöst) |
| Fire Brick | Fire Clay i Clay Kiln, **20 s vid 1100°** (kräver Charcoal och Bellows; Furnace Brick tar 600°) |
| Bloomery Hearth | `[F][F][F]` / `[F][T][F]` / `[F][ ][F]` (F = Fire Brick, T = Clay Tuyere) |
| Bloomery Stack | `[F][ ][F]` / `[F][ ][F]` / `[F][ ][F]` |

En hel Bloomery = 19 Fire Brick + 1 Clay Tuyere ≈ 10 Clay Balls + 10 Grog (5 Brick). Allt bryts med hacka.

### Bygga

- **1×1×3:** en **Bloomery Hearth** längst ner och **två Bloomery Stacks** ovanpå. Blocket ovanför det översta får inte
  vara fast (beskickningen läggs i och röken går ut där). En tredje stack gör schaktet för högt.
- Härden vänds mot spelaren när den sätts: fram ett lågt **slaggvalv** av bränd lera (mörkt, slaggdroppar vid tröskeln),
  bak ett **lerrör (tuyère)** som sticker ut ur väggen. Den översta stacken visar den öppna **mynningen**: 4 px tjocka
  sotiga väggar runt ett hål med beskickningen (träkol och rostig malm) en bit ner.
- **Tänd:** valvet och munstycket glöder (animerat, lyser 10), beskickningen i mynningen glöder (lyser 9), rök, lågor
  och gnistor ur toppen, knaster och lavagnistor vid valvet.
- Strukturen kollas varje sekund. Saknas något slocknar elden (bränslet ligger kvar) och högerklick på härden säger
  vad som fattas: "Not a bloomery yet: stack 1 more Bloomery Stack(s) on the hearth", "The shaft is too tall ..." eller
  "The top of the shaft must be open: clear x y z".
- Advancement **Bloomery** ges från koden till spelare inom 12 block när den blir hel.

### Värme

Samma modell som kilnen (`KilnBlockEntity.heatStep`), men det eldfasta schaktet tål mer:

- **Bränsle: bara Charcoal (och Coal).** Brinner **2 min** per st (hälften av kilnens 4 min: en blästugn slukar kol).
- **Tänds** med Fire Drill på härden (Tinder hjälper), bara när den är hel och har bränsle.
- Tänd stiger den **5°/s** mot Charcoals naturliga **800°**; släckt sjunker den 3°/s (×2 i regn på toppen).
- **Bellows** som pekar på härden (vilken sida som helst; munstycket bak visar var den hör hemma): **+40°** per pump upp
  till **1500°**. Över 800° sjunker den **2°/s** tillbaka. Från 800° till 1350° krävs 14 pumpningar; att hålla 1350°
  kräver ungefär 3 pumpningar i minuten.
- Ingen överhettning och inga sprickor (eldfast tegel).

| Temperatur | Vad |
|---|---|
| 800° | Charcoals naturliga max (utan bälg) |
| 1100° | Smideshetta: värmer om en kall lupp (här och i Clay Kiln) |
| 1300° | Taket för Clay Kiln och Smelting Furnace: räcker **inte** till järn |
| **1350°** | Raw Iron reduceras till en lupp (bara Bloomery, alltså bara med bälg) |
| 1500° | Bloomerys tak med bälg |

### Smälta

Högerklick på härden öppnar **kilnens GUI** (samma rutor: in, bränsle, ut), men termometern går till **1500°** och har
markeringar vid **1100°** (smideshetta, orange) och **1350°** (järn, vit). Det vita strecket vid termometern visar vad
det som ligger i härden kräver (grönt när det nåtts), tooltipen temperatur och takt.

| In | Ut | Tid | Kräver |
|---|---|---|---|
| **4 Raw Iron** | **Hot Iron Bloom** | 60 s | 1350° |
| Iron Bloom (kall) | Hot Iron Bloom (slagen behålls) | 10 s | 1100° |

- För kallt: framstegen står still (nollställs inte), som i kilnen. **Hetare går fortare**: +1 takt per 200° över (vid
  1500° 1,75×).
- En lupp i utrutan **hålls het** så länge härden är minst 1100°. Nästa smältning väntar tills luppen tagits ut.
- Clay Kiln värmer också om en kall lupp: **Iron Bloom → Hot Iron Bloom, 10 s vid 1100°** (kilnen når 1300° med bälg),
  och håller den het i sin utruta på samma sätt. Smelting Furnace tar inte luppar (den tar bara det som smälter genom tratten).

### Järnluppen

- **Hot Iron Bloom:** glödande järnsvamp (animerad). Het i **30 s** utanför elden, en orange stapel visar värmen som är
  kvar. **Bränner** i bar hand som en Crucible of Molten Metal (½ hjärta varannan sekund) om man inte håller **Bronze
  Tongs** i någon hand. Svalnar den blir den en **Iron Bloom** (mörk, rostig, med slaggporer).
- Luppen bär hur många slag den fått (`mechanical:bloom_hits`, tooltip "Hammered 3/8"). Slagen följer med när den
  svalnar och värms om, så man fortsätter där man var.

### Bronze Anvil och Bronze Smithing Hammer

| Sak | Hur |
|---|---|
| Unfired Clay Hammer Head Mold | Mold Bench, 5 Clay Balls → kilnen (600°, 20 s) → **Clay Hammer Head Mold** |
| Rough Cast Bronze Hammer Head | 4 enheter brons i formen → Whetstone (4 drag) → **Bronze Hammer Head** |
| **Bronze Smithing Hammer** | Tool Station: Bronze Hammer Head + Cordage (eller Hide Glue) + Tool Rod |
| **Bronze Quarry Hammer** | Tool Station: Bronze Hammer Head + **Bronze Plate** + Tool Rod (se nedan) |
| Unfired Clay Anvil Mold | Mold Bench, **32 Clay Balls** → kilnen (600°, 30 s) → **Clay Anvil Mold** |
| Rough Cast Bronze Anvil | **10 enheter brons**, en hel degel (eller via Clay Channels från Smelting Furnace) → Whetstone → **Bronze Anvil** |

- Båda formarna tar bara brons och tål 5 gjutningar, som de andra. Bronze Anvil är det största som gjuts (Bronze Kettle 8).
- **Bronze Smithing Hammer:** 160 hållbarhet, **2 per slag** på en lupp (järn sliter brons fort: 10 luppar per hammare).
  Den är också en hammarsten på Stone Anvil (1 per slag). Poor Bronze: 70 % hållbarhet. Låses upp i Tool Station när man
  bär huvudet.
- **Bronze Anvil:** ett gjutet bronsstäd (fot, midja, bana och horn; banan blankare). Vänds med hornet åt sidan. Bryts
  med hacka. Poor Bronze-märket följer inte med (blocket bär inget).

### Bronze Quarry Hammer (v0.82.0)

En tung stenhammare som bryter **3×3** i taget: planet mot spelaren, efter vilken sida av blocket man slår på (golv,
tak eller vägg). Görs i Tool Station av samma Bronze Hammer Head som smideshammaren, med en Bronze Plate runt skaftet
som "bindning"; låses upp när man bär huvudet. Poor Bronze: 70 % hållbarhet.

- **Bara sten:** taggen `#mechanical:quarry_hammer_mineable` = `#minecraft:base_stone_overworld` (Stone, Granite, Diorite,
  Andesite, Tuff, Deepslate), `#minecraft:base_stone_nether` (Netherrack, Basalt, Blackstone), Cobblestone, Mossy
  Cobblestone, Cobbled Deepslate, Calcite, Dripstone Block, Smooth Basalt, Sandstone, Red Sandstone, End Stone och malmerna.
  Modden har ingen egen naturlig sten. På allt annat (jord, trä ...) är hammaren som en bar hand.
- Bronsnivå (som Bronze Pickaxe), **halva hackans fart** (3 mot 6), 460 hållbarhet, 1 per block (9 per fullt slag),
  6 skada, långsam (-3.4).
- **Malm den klarar** (`#mechanical:quarry_ores` = `#c:ores` + ädelstensmalmerna) smulas till sin bergart: den ger det
  stenen ger (Stone Chunks, Deepslate, Netherrack, Sandstone), aldrig malmen, och hammaren slits **2 extra** per malm.
  "You need more precision to break ores: they crumble to stone". Bryt malm med en Bronze Pickaxe.
- **Malm över bronsnivån** (Gold, Diamond, Redstone, Emerald, Ruby utom deepslate ...; `#mechanical:incorrect_for_bronze_tool`)
  någonstans i 3×3: **inget** går sönder, hammaren slits **4**, "This ore is too hard for the hammer: the blow rings off".
- Advancement *Quarryman* (efter Smith).

### Smida

1. Högerklicka **Bronze Anvil** med luppen (het eller kall). Den ligger på banan. Tom hand tar tillbaka den.
2. Slå med **Bronze Smithing Hammer** (vänster- eller högerklick), ett slag per halv sekund: städljud, gnistor.
   Med en lupp på städet slår hammaren luppen i stället för att bryta städet.
3. **8 slag** på en het lupp → **2 Wrought Iron Ingot + 1 Slag** hoppar upp. 4 Raw Iron blir alltså 2 tackor.
4. **Kall lupp:** inga slag och inget slitage, "The bloom has cooled: reheat it to 1100° in the Bloomery or a Clay Kiln".
   En het lupp som ligger kvar på städet svalnar där (fräs) när tiden gått ut.

8 slag tar ungefär 4 s, så den som har städet nära härden hinner med en hetta; längre bort får man värma om.
**Stone Anvil** tar inte luppar: "Stone is too soft for iron: hammer the bloom on a Bronze Anvil".

### Prägla mynt (Copper Coin)

Pengar ersätter Emerald (`Coins.java`): **Copper Coin**, **Silver Coin** och **Gold Coin**, 1 Silver = 10 Copper,
1 Gold = 10 Silver. Bara Copper Coin går att prägla i bronsåldern; silver och guld väntar på järnåldern.

1. Högerklicka **Bronze Anvil** med en **Copper Ingot** (kall, ingen värme behövs). Den ligger på banan.
2. **4 slag** med **Bronze Smithing Hammer** (1 slitage per slag) → **4 Copper Coin** hoppar upp.

Det finns ingen kopparplåt, så tackan präglas direkt. Att prägla ger mer än att sälja koppar till Armorer (6 Copper
Ingot → 4 Coin), så smedjan lönar sig. Texturer: `gen_coin_textures.py`; modeller och namn i `gen_bronze.py`.

### Bronze Fire Grate

Ett galler av bronsstänger som läggs i en Fire Pit eller i en Clay Kilns eldstad. Elden får luft underifrån: veden räcker
längre och det ryker och sotar mycket mindre. Inga nackdelar.

**Smida:**

1. Högerklicka **Bronze Anvil** med **Bronze Ingot** (kall, ingen värme). Upp till **3** läggs på banan, vilken kvalitet
   som helst (Poor Bronze går bra, gallret bär ingen kvalitet). Tom hand tar tillbaka dem.
2. **6 slag** med **Bronze Smithing Hammer** (1 slitage per slag) → **1 Bronze Fire Grate** hoppar upp.
3. Med färre än 3 tackor klingar hammaren av: "A Bronze Fire Grate needs 3 Bronze Ingots (2/3)", inget slitage.

**I Fire Pit:** högerklicka vilken del som helst med gallret. Det ligger i gropen (stänger syns i modellen, i alla fyra
delarna). Ett galler per eld.

- Veden brinner i **en Campfires takt** i stället för dubbelt så fort, så varje vedträ räcker **2× så länge** som förut.
- Rök och sot inomhus bara **0,3×** en Campfires (utan galler 3×). Marken bränns som av en Campfire (1×, utan galler 2×).
  Rökpelaren utomhus blir mycket tunnare.
- **Charcoal** läggs på högen som Firewood (räknas mot samma 16, tänder glöd igen). En Charcoal brinner som **2 Firewood**.
  Utan galler: "Charcoal needs a grate to draw air: lay a Bronze Fire Grate in the pit", och inget händer.
- Rivs Fire Pit kommer gallret tillbaka.

**I Clay Kiln:** högerklicka någon av halvorna med gallret. Det läggs i eldstaden (stänger syns i eldöppningen).

- Allt bränsle (även lägerelden ugnen byggdes på) räcker **1,5×** så länge.
- Rök och sot **0,3×** en Campfires (utan galler 1×).
- Temperaturerna ändras inte: Bellows behövs fortfarande för koppar och brons.
- Bryts ugnen kommer gallret tillbaka.

Kod: `FirePit.java` (`FIRE_GRATE` och konstanterna), `FirePitBlock.java`/`FirePitBlockEntity.java`,
`KilnBlock.java`/`KilnBlockEntity.java` (`GRATE_SKIP`), `BronzeAnvilBlockEntity.smithGrate`, `Smoke.smokeFactor`. Data
`scripts/gen_firepit.py` (gallrets modell, item, namn), `scripts/gen_data.py` (kilnens gallermodell, multipart-blockstate),
texturer `scripts/gen_fire_grate_textures.py`. Advancement *Clean Burn* (efter Bronze Anvil).

### Slag

1 per lupp. **4 Slag → 1 Gravel** (formlöst, vägfyllning). Ingen annan användning än.

### Advancements (Bronze Age-fliken)

*Fire Brick* (efter Toward Iron), *Bloomery* (goal, från koden), *Iron Bloom* (goal), *Wrought Iron* (challenge),
*Bronze Anvil* (goal, efter Bronze!), *Mint* (första Copper Coin, efter Bronze Anvil), *Clean Burn* (Bronze Fire Grate,
efter Bronze Anvil), *Smith* (Bronze Smithing Hammer, efter Bronze!),
*Quarryman* (Bronze Quarry Hammer, efter Smith).

### Test

Gametest `mechanical:bloomery`: härd + en stack, härd + tre stackar och ett lock på schaktet är ingen Bloomery, härd + två
stackar med öppen topp är det (den övre stacken är toppen); 4 Raw Iron smälter inte vid 1300° men blir en Hot Iron Bloom
vid 1500°; Raw Iron tas inte av Clay Kiln, Crucible, Stone Anvil eller vanillas smelting/blasting/campfire/smoking-recept;
Copper och Stone Pickaxe bryter inte Iron Ore men Bronze Pickaxe gör; en kall lupp tar inga slag på Bronze Anvil, en het ger
Wrought Iron Ingot och Slag efter 8 slag och hammaren slits 16; kilnen värmer om en lupp vid 1100° och den behåller sina slag; en Copper Ingot på Bronze Anvil ger Copper Coin först efter 4 slag.

### Beslut

- **Wrought Iron Ingot** i stället för vanillas Iron Ingot, så att allt av vanilla-järn (hink, sax, städ, räls ...) fortsatt
  väntar på järnåldern utan att varje recept måste stängas av.
- **Temperaturspärr:** järn kräver 1350°, över kilnens och Smelting Furnaces 1300°-tak, så Bloomery och bälgen behövs
  även om man skulle få in Raw Iron någon annanstans.
- **Kilnens GUI återanvänds** (samma rutor och data) med en hetare termometer, i stället för ett eget gränssnitt.
- Omvärmning i Bloomery och Clay Kiln; Smelting Furnace tar bara emot det som smälter.
- Fire Clay av lera + Grog (krossat tegel), bränd vid 1100°: Furnace Brick (lera + sand, 600°) är det enklare teglet,
  Fire Brick kräver redan bälg och kol.
- **Inte gjort (valfritt):** att beskicka genom toppen (allt läggs i GUI:t), slaggtappning ur valvet medan den brinner,
  att städet krossar malm, en Poor Bronze-variant av städet.

### Planerat: härdning och stål (inte byggt)
Wrought Iron Ingot leder inte vidare ännu. Två steg med sot och kol är bestämda till senare (2026-11-21):
- **Cementation → Blister Steel:** Wrought Iron Ingot packas med Charcoal och Soot i en lerlåda och bränns länge vid hög
  värme; kolet vandrar in i järnet och det blir Blister Steel, materialet för senare stålverktyg.
- **Ythärdning av järnverktyg:** när järnverktyg finns packas ett verktyg i kol och sot och glödgas, så att ytan blir
  hårdare: mer hållbarhet och snabbare brytning ("Case-Hardened" på verktyget).

## Skrift (byggd, v0.98.1)
Skriften kommer tillbaka i bronsåldern, med bläck av sot (`scripts/gen_writing.py`).
- **Soot Ink:** 2 Soot + Tallow eller Lard (bindemedlet) → 2 Soot Ink (formlöst). En svart bläckkaka som rivs med vatten.
- **Paper:** 3 Sugar Cane + **Bronze Knife** (eller Iron Knife) → 3 Paper (formlöst; kniven stannar i rutnätet och nöts
  som vid andra kniv-recept). Vanillas recept (3 Sugar Cane) är ersatt; kniven är det som gör det till bronsålder.
- **Book:** vanillas recept igen (3 Paper + Leather).
- **Book and Quill:** Book + **Soot Ink** + Feather (formlöst), i stället för Ink Sac.
- Bookshelf, Chiseled Bookshelf och Lectern är fortfarande avstängda (gen_age_recipes.py).
- Advancement **The Written Word** (Bronze Age, efter Bronzesmith). Test: FertilizerTest (Paper kräver kniven, kniven
  stannar, Book and Quill med Soot Ink).

## Gips och Slate Tablet (byggd, world-materials-plan fas 9B)
Kod: `Chemistry` (items, Slate Tablet, gametest `chemistry`), kilnrecepten i `KilnBlockEntity.RECIPES`, mixin
`ServerGamePacketListenerImplMixin`. Data och texturer: `scripts/gen_chemistry.py` (efter `gen_lime.py` och `gen_pigments.py`).

| Vad | Hur |
|---|---|
| **Gypsum Powder** | 1 **Gypsum** krossas på **Stone Anvil** → 2. |
| **Plaster of Paris** | 1 Gypsum Powder i **Clay Kiln vid 300°** (vilket bränsle som helst), 15 s → 1. |
| **Lime Plaster av gips** | Plaster of Paris + 2 Sand + Straw, formlöst → **4 Lime Plaster**, samma block som med Slaked Lime. Gipsen gör att puts (och de fyra färgade putsblocken) går att göra redan i bronsåldern, utan Bellows och 900°. |
| **Slate Tablet** | 1 **Polished Slate** + 2 Stick, formlöst → 1 (stackas inte). |

**Slate Tablet:** en skrivtavla av skiffer i träram. Håll **Chalk** i andra handen och använd tavlan: den öppnar
bokskärmen (som Book and Quill) och kostar **1 Chalk** per gång. Tavlan har **en sida**; fler sidor sparas inte, och
**Sign** sparar bara sidan (den blir aldrig en Written Book). Texten syns i tooltipen, så den går att läsa utan krita.
**Sneak-use** torkar av tavlan (gratis). Tänkt för anteckningar, räkning och skyltar man bär med sig; Paper och böcker är
fortfarande det som håller.

**Advancements** (Bronze Age): *Plaster of Paris* (efter Stone Anvil), *Slate and Chalk* (efter Bronzesmith).

## NPC-byar i bronsåldern (byggd)

När en NPC-stam når bronsåldern (`WorldAge.npcAge`) river den sina gamla byggnader och bygger nya (`people/AgeRebuild.java`,
detaljer i `docs/seasons-and-tribes.md`, *Ombyggnad av NPC-byar*). Stambyar: långhuset står en kurs högre på stenfot
(Cobblestone), korsvirke med Stripped Log och Packed Mud, Horn Window och halmtak (Hay Block) över barkfoten. Rundhyddorna
får stenfot, lerväggar och halmtak, mammutbenhyddorna blir rundhyddor och tältplatserna blir inhägnader av staket med
grind. Vid samlarna står en Clay Kiln och en Quern. Varmt: husen en kurs högre och verandor med tak av Packed Mud.
Djungel: plankväggar i pålhusen. Lägren stammarna bygger vid spelarläger byter hudtälten mot små lerhyddor och får Clay
Kiln och Quern. Ingen spelare nära: byn byts på en gång. Spelare nära: en stammedlem bygger om block för block.
Spelarblock rörs inte. Advancement **Bronze Age Neighbours**.

## Hårda bergarter (byggt, fas 6 av världsmaterialplanen)

Sju hårda bergarter i berget. Flinta biter inte på dem: de kräver **Bronze Pickaxe** och ger sig själva. Varje sten har en
hel familj byggblock från en vanilla-familj och ligger i creative-fliken **Building Blocks**. Klumparna finns i hela världen
men är vanligare i "sin" biom (mängderna ligger under vanillas granit och diorit). De mjuka stenarna från samma fas
(Pumice, Scoria, Jasper Stone, Gypsum, Alum Shale) står i stenålderns dokument.

| Sten | Mall | Var | Familj | Bronze Chisel |
|---|---|---|---|---|
| **Marble** | quartz | Djupt, y −20–40 (1 klump i var 8:e chunk), 1 extra per chunk under berg, Savanna Plateau, Windswept Savanna och badlands | Marble Bricks (2×2), **Marble Pillar** (2 på varandra, vrids som en stock), Chiseled Marble, Smooth Marble (smält), slab/stairs av Marble och Smooth | Marble → Bricks → Chiseled, Smooth → Pillar |
| **Quartzite** | tuff | y 0–120 (var 8:e chunk), 1 extra under berg | Polished, Bricks, Chiseled, Chiseled Bricks + slab/stairs/wall | Quartzite → Polished → Bricks → Chiseled Bricks |
| **Red Porphyry** | tuff | Sällsynta små klumpar y −16–80 (var 12:e chunk), 1 extra under badlands och Windswept Savanna | Som Quartzite | Som Quartzite |
| **Green Porphyry** | tuff | Som Red Porphyry, extra under Windswept Hills, Gravelly Hills och Forest | Som Quartzite | Som Quartzite |
| **Rapakivi Granite** | stone | y 0–80 (var 8:e chunk), 1 extra under taiga, Forest, Birch Forest, Old Growth Birch Forest och Snowy Plains (ersätter granit där) | Som Gneiss: Smooth, Bricks, Cracked, Mossy, Chiseled Bricks + slab/stairs/wall | Rapakivi Granite → Smooth → Bricks → Chiseled Bricks |
| **Coastal Basalt** | basalt | **Pelare** på 2–6 block på Stony Shore (även ute i vattnet), klumpar y 30–80 (var 10:e chunk), 2 extra under Stony Shore och stränder | Polished Coastal Basalt (2×2, vrids), Smooth Coastal Basalt (smält) | Coastal Basalt → Polished |
| **Serpentine** | tuff | Sällsynt, y −16–64 (var 12:e chunk), 1 extra på y 0–100 under Windswept Hills, Gravelly Hills och Stony Peaks | Som Quartzite | Som Quartzite |
| **Alabaster** | quartz | Små klumpar y 40–90 (var 16:e chunk), 1 extra under öken och badlands, där Gypsum ligger | Som Marble | Alabaster → Bricks → Chiseled, Smooth → Pillar |

Namnet **Coastal Basalt** valdes för att inte krocka med vanillas Basalt, Polished Basalt och Smooth Basalt (Nether), som
annars skulle ha samma namn i creative-fliken och JEI. Det passar också den planerade biomen Basalt Coast. Pelarna kommer
från `worldgen/feature/coastal_basalt_columns.json` (vanilla `block_column`).

Advancements i Bronze Age-fliken (under Bronze Pickaxe): *Marble*, *Quartzite*, *Porphyry*, *Rapakivi*, *Basalt Columns*,
*Serpentine*, *Alabaster*. Senare användning (planen fas 9): statyer, slipstenar, kvarnstenar av Rapakivi, lyxsten,
inläggningar, lampor av Alabaster.

## Kaolin Tiles (byggt, fas 3 av världsmaterialplanen)

**Kaolin** är en vit lera i sällsynta fläckar i jorden (y 50–75) i skog, Plains, Meadow och djungel
(`#mechanical:has_kaolin`). Grävs med spade och smular sönder till **4 Kaolin Clay** som vanlig lera (Silk Touch: blocket).
2×2 Kaolin Clay → 1 Kaolin. Det är inte samma sak som Fire Clay.

Ett Kaolin-block bränt i **Clay Kiln vid 1000°** (Charcoal + Bellows) blir **Kaolin Tiles**, vitt kakel med slab, stairs och
wall (vanilla-bricks-mönster: 3 i rad → 6 slabs, trappmönster av 6 → 4 stairs, 2×3 → 6 walls). Advancement *White Tiles*
i Bronze Age-fliken (under Bellows). Senare: vit glasyr och porslin.

## Stenhantverk (byggt, fas 9A av världsmaterialplanen)

Bergarterna från fas 1 och 6 har fått riktiga användningar. Allt finns i `StoneCrafts.java`, data i `scripts/gen_stone_crafts.py`.

| Block | Recept | Vad det gör |
|---|---|---|
| **Soapstone ... Mold** (en för varje lerform) | Mold Bench: Polished Soapstone, en halv per metallenhet (minst 1) | Som lerformen, men behöver ingen bränning och **spricker aldrig** |
| **Quartzite Whetstone** | `[ ][Quartzite][ ]` / `[Stick][Planks][Stick]` | Slipar på **2 drag** i stället för 4 (1 med Bronze Chisel i andra handen) |
| **Rapakivi Quern** | `[ ][C][ ]` / `[R][Stick][R]` / `[R]×3` (C = Copper/Bronze Ingot, R = Rapakivi Granite) | Ett halvt varv per klick: **2 klick** per Flour i stället för 4 |
| **Basalt Anvil** | `[ ][Stone Anvil][ ]` / `[Coastal Basalt]×3` | Tungt städ: krossar på **2 slag** i stället för 4 |
| **Alabaster Lamp** | `[A][Plant Fibre][A]` / `[A][A][A]` (A = Alabaster) | En Oil Lamp av alabaster: samma bränsle och tändning, men **ljus 15** (Oil Lamp 14) |

Kostnaden för täljstensformar: Ingot, Shovel Head, Knife Blade, Arrowheads, Fish Hooks, Needles och Chisel Blade 1 Polished
Soapstone; Hoe Head, Saw Blade, Plate, Spear Head, Sickle Blade, Shears, Tongs och Hammer Head 2; Pickaxe Head, Axe Head,
Sword Blade och Bucket 3; Kettle 4; Anvil 5. De fungerar överallt där lerformar gör (degel, Smelting Furnace-tappen, rännor).

Advancements i Bronze Age-fliken: *Stone That Remembers* (under Mold Maker), *Heavy Stone* (Coastal Basalt), *Hard Grit*
(Quartzite), *Millstone* (Rapakivi) och *Glowing Stone* (Alabaster).

Inte gjort: vind- och vattenkvarn (modden har ingen, så Rapakivi blev handkvarnens stenar), statyer och piedestaler av
marmor (kräver egna modeller), inläggningar av Porphyry och Serpentine (ring- och charm-platserna i utrustningen har inga
smycken än att lägga in dem i).

## Åldrande tegel, mosaik och tegeltak (byggt, fas 10 av världsmaterialplanen)

Java i `Masonry.java`, data i `scripts/gen_masonry.py` och de vittrade raderna i `scripts/gen_stone_families.py`.

**Åldrande.** Alla bergarters Bricks (Limestone, Slate, Gneiss, Soapstone, Shale, Conglomerate, Dolomite, Marble, Quartzite,
Red/Green Porphyry, Rapakivi Granite, Pumice, Serpentine, Jasper, Alabaster, Alum Shale) har nu **Cracked** och **Mossy**
Bricks (+ Mossy slab, stairs, wall), och tegelblocket åldras av sig självt när det står ute:

- **Mossy:** toppen eller en sida mot bar himmel, och vatten, mossa, Vine, mossigt tegel eller regn intill. I snitt efter ett år
  (360 dagar). Mossan sprider sig alltså längs en mur.
- **Cracked:** samma öppna läge och minusgrader där (luften under 3, vinternätter i tempererat klimat, nästan alltid i kalla
  biomer). Ungefär två årstider med frost, alltså några vintrar i tempererat klimat.
- **Inomhus**, under tak, händer ingenting. Bara de hela Bricks-blocken åldras, inte slab, stairs eller wall, och inte puts
  (Lime Plaster), Adobe, Peat Bricks eller vanillas Stone Bricks.

En **Pumice Chunk** skrubbar bort mossan (1 slitage): Mossy blir vanligt tegel igen, också vanillas Mossy Stone Bricks och
Mossy Cobblestone. Det går också att göra varianterna som i vanilla: Bricks + Vine eller Moss Block → Mossy, Bricks i ugnen →
Cracked. Advancement *Weathered* (efter *Rubble*).

**Mosaic Tiles.** Fyra olika polerade stenar (formlöst) → **4 Mosaic Tiles**, + slab, stairs och wall:

| Mosaik | Stenar |
|---|---|
| **Warm Mosaic Tiles** | Polished Jasper, Polished Quartzite, Cut Travertine, Yellow Ochre Bricks |
| **Cool Mosaic Tiles** | Polished Slate, Polished Serpentine, Smooth Marble, Polished Soapstone |
| **Checkered Mosaic Tiles** | Smooth Marble, Smooth Alabaster, Polished Alum Shale, Polished Coastal Basalt |

Ingen mejsel i receptet: de polerade stenarna kräver redan Bronze Chisel eller Pumice. Advancement *Mosaic* (efter *Marble*).

**Fired Roof Tiles.** 2 Clay Ball i rad → **Unfired Roof Tile**, bränd i Clay Kiln vid **800°** (Charcoal eller bellows) →
**Fired Roof Tile**. 2×2 → **Roof Tiles** (block + slab, stairs, wall); 3 i trappform → **4 Tiled Roof**, ett tak som sluttar
som barktaket. **8 Roof Tiles eller Tiled Roof runt ett pigment → 8** i färg: Red, Black, Yellow, White. Tegeltak brinner inte
och räknas som sten för husets värme (inte som torv- och halmtaken). Advancement *Tiled Roof* (efter *White Tiles*).

## Köket i bronsåldern (flora och mat, fas 4, byggt)

- **Bronze Griddle:** Bronze Plate + 3 Sticks (`[S][P][S]` / `[ ][S][ ]`). En stekhäll på tre ben som ställs **över en
  Campfire**. Lägg på upp till 4: **Dough → 2 Flatbread**, **Raw Honey Cake → Honey Cake** (Flour + honung + ägg → 2 Raw Honey
  Cake). 30 s över tänd eld (glöd räcker inte); tom hand tar av, och lämnas det kvar 60 s till bränns det till Burnt Food.
- **Bronze Ladle:** Bronze Ingot + Stick. Ös ur en gryta med sleven (en Bowl i förrådet): första gången skrapar den grytan ren
  och ger **en skål till**. Slits 1 per skål. Fungerar på Clay Cooking Pot, Bronze Kettle och Iron Cauldron.
- **Rätter i Bronze Kettle:** en känd rätt går före vanlig stuvning när ingredienserna stämmer exakt (dubbla ger dubbelt, högst
  6 skålar). Kitteln kokar också alla krukans rätter.

| Rätt | Ingredienser | Ger |
|---|---|---|
| **Pea Soup** | 3 Peas + en lök | 4 skålar à 6 |
| **Onion and Garlic Stew** | 2 Onion + Garlic + rått kött | 4 skålar à 8 |
| **Spiced Porridge** | 2 säd + Cinnamon Bark (östra karavanen) + honung | 3 skålar à 7 |
| **Tea** | Tea Leaves (östra karavanen) | 3 skålar |
| **Elderberry Syrup** | 3 Elderberries + honung | 2 (håller 30 dagar) |
| **Sloe Jam** | 3 Sloes + honung | 2 (håller 30 dagar) |
| **Bitter Orange Marmalade** (flora3) | 3 Bitter Oranges + honung | 2 à 4 (håller 30 dagar) |
| **Quince Paste** (fruktträdgården) | 3 Quinces + honung | 2 à 5 (håller 30 dagar) |
| **Cherry Jam** (fruktträdgården) | 3 Sweet Cherries + honung | 2 à 4 (håller 30 dagar) |

- **Bybornas varor:** byns Farmer säljer för kopparmynt det som kom med jordbruket och inte växer vilt: nivå 2 **Barley Grain**,
  **Onion**, **Garlic**, **Dill Seeds**, **Mustard Seeds**; nivå 3 **Cabbage**, **Broad Beans**; nivå 4 **Wild Apple Sapling** (6 Copper Coin) och **Apple Scion** (2 för 8 Copper Coin, att ympa på den). Dill och Mustard sås på farmland
  (dill ger Dill och frön, senap ger frön), Broad Beans likaså.
- **Den östra karavanen** (*Eastern Caravan*, sällsynt: vikt 1 mot de andras 4; kameler, sidenrockar i ikatränder, höga
  broderade mössor, indigo- och saffransrandigt kapell): säljer Black Pepper, Cinnamon Bark, Ginger Root, Rice, Sesame Seeds,
  Dried Dates, Dried Figs, Indigo, Lapis (brons), Saffron Crocus Corms, Tea Leaves, Silk Thread, **Bitter Orange Sapling** (3 Silver
  Coin, växer bara i varmt land), Lemon, Clementine och Pomegranate Sapling (3 Silver Coin, bara i varmt land) och Almond Sapling
  (2 Silver Coin) (järn) och sällsynt Saffron;
  köper ädelstenar, Jujubes, Sea Buckthorn Berries och ametist. Ginger, Saffron och Rice kan sås i varmt klimat.
- Advancements (Bronze Age): *Griddle*, *Proper Dishes* och *Every Last Drop* (efter *Kettle*). *Paste and Jam* (efter *Marmalade*): kvittenpasta eller körsbärssylt. Wilderness: *The Spice Road*.

## Magma Block (byggt 2026-11-25)

Fås från ruinerade portaler och havsbotten (kontrollerat i en genererad värld).

- **Värme:** upp till +8 kroppsgrader intill, räckvidd 5 block (campfire +10), värmer aldrig över behaglig nivå (12),
  väggar ger upp till halva till (som för en eld). Hjälper mot vinter, snö och djupets kyla. Flera block staplas inte.
- **Snö:** ingen ny snö och ingen kvarliggande snö/is inom 2 block åt sidan och upp till 2 block ovanför.
- **Under Baking Stone:** bakar utan eld, i halv fart.
- **Under Clay Kiln** (blocket under eldstaden) och **mitt under golvet i en Smelting Furnace:** håller 600° för alltid,
  utan Fire Drill och utan att bränna bränsle. Bälgen kräver riktig eld. Räcker till tegel, keramik, bröd, pajer och
  träkol; inte till koppar, brons, guld eller fire brick (i Smelting Furnace smälter bara tenn, men den är förvärmd).
- **Crushed Magma:** se 04-steel-age.md, avsnitt 7.
- Advancements **Captive Heat** och **Cinder Grit** (Bronze Age-fliken).
