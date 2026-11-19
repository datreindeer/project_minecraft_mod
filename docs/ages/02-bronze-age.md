# Bronsåldern

> Status: **fas 1 (malm), fas 2 (värme), fas 3 för koppar och fas 3b brons (legering, bronsverktyg, Bronze Saw, Bronze
> Helmet + Greaves, byhandel nivå 3) och odlingens fas 3 (Quern, bröd, kompost, Manure, Scarecrow) är byggda**, se
> avsnitten sist. Kvar: tvättat koncentrat, spjut/svärd, mot järnet.

> Helmet + Greaves, byhandel nivå 3) och keramik (Potter's Wheel, Storage Jar, Water Jug) är byggda**, se avsnitten sist. Kvar: tvättat koncentrat, spjut/svärd, mot järnet.
> Beslut står under Beslut.

Andra åldern. Målet är att gå från sten till metall: hitta koppar och tenn, få fram tillräcklig värme, smälta och gjuta,
legera brons och smida verktyg och rustning. Åldern slutar när spelaren kan bygga det som behövs för järn
(en Bloomery av eldfast tegel och bälgar, och en bronshacka som klarar järnmalm).

Stenåldern lämnar över: **Clay Kiln**, **Crucible**, **Charcoal**, **Tin Ore** (finns redan i världen), Raw Hide/Leather,
Sinew, Tool Station och Armor Station.

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
   (bränns i kilnen): ingot, yxhuvud, hackhuvud, kniv, spjutspets, sågblad, svärd.
6. **Legera brons.** Ungefär 9 delar koppar och 1 del tenn i samma degel → **Bronze**. Fel blandning ger svagare metall.
7. **Montera.** Gjutna huvuden (kanske slipade på en **Whetstone**, se öppna frågor) sätts ihop i Tool Station med
   skaft och binding, som stenverktygen.
8. **Rustning (bara delvis).** Gjut **Bronze Helmet** och **Bronze Greaves** (benskydd), slipa dem och sätt ihop i
   Armor Station. Ingen bröstplåt: bålen skyddas fortfarande av fur/läder. Full plåtrustning kommer med järnet.
9. **Mot järnet.** Fire Clay → eldfast tegel → **Bloomery**, som behöver bälgar och ger en järnlupp. Järnmalm kräver
   en bronshacka.

## Nytt innehåll (förslag)

| Sak | Typ | Syfte |
|---|---|---|
| Malachite, Native Copper (ytfynd) | Lösa block | Visar var kopparådror finns. Native Copper kan hamras direkt till lite koppar |
| Cassiterite | Item | Tennmalm från vaskning (och Tin Ore) |
| Pan | Verktyg | Vaska grus och sand i vatten |
| Stone Anvil | Block | Krossa malm, kallhamra |
| Bellows | Block | Höjer kilnens temperatur |
| Clay Molds (7 sorter) | Items | Gjutformar, brända i kilnen. Kan gå sönder efter några gjutningar |
| Crude Copper / Crude Tin Ingot, Copper Ingot, Bronze Ingot | Items | Användarens texturer finns (workshop/) |
| Copper Bloom, Bronze Dust, Bronze Plate | Items | Mellanprodukter (texturer finns) |
| Copper- och Bronze-verktyg | Verktyg | Yxa, hacka, spade, hacka (hoe), kniv, spjut, svärd, såg |
| Bronze Helmet, Bronze Greaves | Rustning | Gjuts, slipas, Armor Station. Ingen bröstplåt eller boots |
| Basic Smithing Hammer, Basic Binding | Verktyg/item | Texturer finns |
| Whetstone | Block/verktyg | Slipa gjutna huvuden |
| Saw | Verktyg | Planks från logs (den öppna punkten från stenåldern) |

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
3. **Gjutning:** smält metall i Crucible, Clay Molds, ingots, legering till brons (**byggt**); tvättat koncentrat återstår.
4. **Verktyg och rustning:** kopparverktyg, Whetstone, bronsverktyg, Bronze Saw, Bronze Helmet + Greaves, byhandel nivå 3
   (**byggda**, se Fas 3b).

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
- Bryts städet ramlar malmen som ligger på det ut.

## Fas 2: värme (byggd)

Kod: `KilnBlockEntity` (temperatur, påfrestning, lagning), `KilnBlock` (CRACKS, HOT), `BellowsBlock`,
`client/KilnScreen` (termometer), `Casting` (registrering). Data: `scripts/gen_copper.py` och kilnens modeller i
`gen_data.py`; texturer: `scripts/gen_copper_textures.py`. Generatorordning: gen_data.py src/main/resources → gen_tribe_village →
gen_trails → gen_bronze_ores → gen_age_recipes → gen_farming → gen_wind → **gen_copper** → **gen_bronze** → gen_smoke → ... →
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
| Charcoal, Coal | 800° | 1300° |

**Hetare bränsle tar direkt:** lägger man ett bränsle med högre max i bränsleslotten medan ett sämre brinner (t.ex.
Charcoal på ved) tas det in genast. Det som var kvar av det gamla bränslet brinner med (brinntiden läggs ihop), så
inget går förlorat och temperaturen börjar stiga mot det nya maxet direkt.

Brons (950°) och koppar (1085°) kräver alltså **Charcoal och Bellows**. Trä räcker för lera.

### Temperaturkrav på kilnens recept

Gäller alla recept. Kilnen bränner bränsle och värms så länge det finns något att baka, men framstegen står still
(nollställs inte) tills temperaturen räcker. GUI:t visar ett vitt streck vid termometern på den temperatur som behövs
(grönt när den nåtts).

| In | Ut | Tid | Kräver |
|---|---|---|---|
| 1 Clay Ball | Brick | 20 s | 600° |
| 1 Log / 2 Firewood | Charcoal | 30 s | 400° |
| Unfired Crucible | Crucible | 40 s | 800° |
| Unfired Clay Bucket | Clay Bucket | 30 s | 600° |
| Dough | Bread | 20 s | 200° |
| Unfired Clay Tuyere | Clay Tuyere | 20 s | 600° |
| Unfired Clay ... Mold (6 sorter) | Clay ... Mold | 20 s | 600° |
| Crucible med malm | Crucible of Molten Copper / Bronze | 15 s | metallens smältpunkt: koppar 1085°, brons 950° |
| Crucible of Solid Copper / Bronze | Crucible of Molten ... | 10 s | 1085° / 950° |
| Unfired Clay Saw Blade / Plate Mold | Clay Saw Blade / Plate Mold | 20 s | 600° |
| Unfired Storage Jar / Unfired Water Jug | Storage Jar / Water Jug | 30 s | 600° |

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
- **Tömma degeln:** smyg + använd degeln i luften. Malm kommer tillbaka som krossad malm (1 Crushed Copper/Tin Ore per enhet;
  brons i den som Bronze Ingots), stelnad metall slås ut som tackor (Copper Ingot / Bronze Ingot, 2 enheter per tacka, en
  udda enhet blir skrot). Smält metall går inte: "Let it cool before you knock it out". Kvar blir en tom Crucible.

### Clay Molds

Sex formar. Unfired-formarna formas av Clay Ball i crafting table och bränns i kilnen (600°, 20 s) till en form som
ställs på marken (behöver fast mark under).

| Form | Recept (C = Clay Ball) | Enheter per gjutning | Ger |
|---|---|---|---|
| Clay Ingot Mold | `[C][ ][C]` / `[C][C][C]` | 2 | Copper Ingot (vanilla) |
| Clay Pickaxe Head Mold | `[C][C][C]` / `[C][ ][C]` / `[C][ ][C]` | 6 | Rough Cast Copper Pickaxe Head |
| Clay Axe Head Mold | `[C][C][C]` / `[C][ ][C]` / `[C][C][ ]` | 6 | Rough Cast Copper Axe Head |
| Clay Shovel Head Mold | `[C][ ][C]` / `[C][ ][C]` / `[ ][C][ ]` | 2 | Rough Cast Copper Shovel Head |
| Clay Hoe Head Mold | `[C][C][ ]` / `[C][ ][ ]` / `[C][C][C]` | 4 | Rough Cast Copper Hoe Head |
| Clay Knife Blade Mold | `[C][ ][C]` / `[ ][C][ ]` / `[ ][C][ ]` | 2 | Rough Cast Copper Knife Blade |

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

Ett mjukt mellansteg mellan sten och brons. Monteras i **Tool Station**; receptet låses upp när man bär på det slipade
huvudet. Hacka, yxa, spade och hacka (hoe) är vanillas kopparverktyg (vanillas egna recept är fortfarande avstängda).

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
hållbarhet (t.ex. Bronze Pickaxe 161 i stället för 230). Delar av olika kvalitet staplas inte ihop, och stationerna
blandar dem inte när de hämtar delar.

### Formar (nya)

| Form | Recept (C = Clay Ball) | Enheter | Ger |
|---|---|---|---|
| Clay Saw Blade Mold | `[C][C][C]` / `[C][ ][C]` | 4 | Rough Cast Bronze Saw Blade |
| Clay Plate Mold | `[C][C][C]` / `[C][ ][C]` / `[C][C][C]` | 4 | Rough Cast Bronze Plate |
| Clay Spear Head Mold | `[ ][C][ ]` / `[C][ ][C]` / `[ ][C][ ]` | 4 | Rough Cast Bronze Spear Head |
| Clay Sword Blade Mold | `[C][ ][C]` / `[C][ ][C]` / `[C][ ][C]` | 6 | Rough Cast Bronze Sword Blade |

Bara brons kan gjutas i dem. De andra formarna gjuter brons som koppar: Bronze Ingot, Rough Cast Bronze Pickaxe/Axe/
Shovel/Hoe Head och Knife Blade. Allt Rough Cast slipas på Whetstone (4 drag) till Bronze ... Head / Blade / Plate.

### Bronsverktyg

Monteras i **Tool Station**, låses upp när man bär det slipade huvudet.

| Verktyg | Huvud | Binding | Skaft |
|---|---|---|---|
| Bronze Pickaxe / Axe / Shovel / Hoe | Bronze ... Head | Cordage | Tool Rod |
| Bronze Knife | Bronze Knife Blade | Twine | Stick |
| Bronze Saw | Bronze Saw Blade | Twine | Stick |
| Bronze Spear | Bronze Spear Head | Cordage | Tool Rod |
| Bronze Sword | Bronze Sword Blade (med parerstång) | Cordage | Leather (grepp) |

- **Bronsmaterial:** 230 hållbarhet (koppar 190, järn 250), hastighet 6 och +2 skada som järn, förtrollbarhet 12.
  Attackvärden som vanillas järnverktyg. Lagas med Bronze Ingot.
- **Brytnivå:** `mechanical:incorrect_for_bronze_tool` = `#minecraft:needs_iron_tool` + `#minecraft:needs_diamond_tool`.
  Bronze Pickaxe bryter alltså **Iron, Coal och Lapis Ore** (`needs_bronze_tool`) och Tin Ore, men inte guld, diamant,
  redstone eller smaragd (de väntar på järnet). Fungerar som hammarsten på Stone Anvil.
- **Bronze Knife:** 350 hållbarhet, styckar **1,75×** så snabbt och ger **1,35×** så mycket som Flint Knife, **klarar
  Brown Bear** och Giant Deer (knivnivå 2 som Tusk Dagger). Räknas som kniv och svärd.
- **Bronze Spear:** vanillas spjut (laddad stöt när man håller in Använd, vanligt hugg annars; kastas inte). Flint Spear
  har stone spears värden på ett flintmaterial (100 hållbarhet, +1 skada); Bronze Spear har **iron spears** tider och
  trösklar på bronsmaterialet: 230 hållbarhet, +2 skada, snabbare stöt (0,95 s), mer skada vid laddning (×0,95 mot
  ×0,82) och kortare väntan. Eget långt modell i handen (järnspjutets form i brons, rött band), platt ikon i GUI:t.
  Står i `minecraft:spears`.
- **Bronze Sword:** vanligt vanilla-svärd på bronsmaterialet: **5,5 skada** (Iron Sword 6), attackfart 1,6, **230**
  hållbarhet. Står i `minecraft:swords` (svärdsförtrollningar).
- Poor Bronze följer med till spjut och svärd som till de andra verktygen (70 % hållbarhet: 161).

### Bronze Saw och planks

- 240 hållbarhet. I crafting table: **Log + Bronze Saw → 4 Planks** (alla träslag, recepten `sawn_<trä>_planks`);
  sågen blir kvar och slits 1. Utan såg ger en log fortfarande 2 planks.
- **Fallen Log:** håll in högerklick med sågen: 8 sågtag kapar hela stocken till 3 Rough Logs (utan slump), plus barken.

### Bronze Helmet och Bronze Greaves

Rustningsplåt gjuts (Clay Plate Mold), slipas och sys på ett läderfoder i **Armor Station** (låses upp av Bronze Plate):

| Del | Recept | Rustning | Hållbarhet |
|---|---|---|---|
| Bronze Helmet | 2 Bronze Plate + Bone Needle (slits 1) + 2 Leather | 2 | 110 |
| Bronze Greaves (leggings-platsen) | 3 Bronze Plate + Bone Needle + 3 Leather | 5 | 150 |

- Järnets rustningsvärden, toughness 0, ~70 % av järnets hållbarhet (multiplikator 10 mot 15). Ingen bröstplåt eller
  boots. Egna texturer på kroppen: hjälmen är en bronsfärgad järnhjälm, benskydden täcker bara smalbenen med en läderrem
  under knät.

### Degelns ikon

Crucible of Molten / Solid Metal visar metallen den håller: bronsfärgad (gyllenbrun) för Bronze och Poor Bronze,
kopparfärgad annars. Smält brons glöder gulare och ljusare än koppar, med en mer gyllene glöd på kanten. Item-modellerna
använder villkoret `mechanical:bronze_crucible` (`client/BronzeCrucibleProperty`, registrerat med NeoForges
`RegisterConditionalItemModelPropertyEvent`) som läser degelns innehåll; texturerna `molten_crucible_bronze` och
`solid_crucible_bronze` ritas av `gen_crucible_textures.py`.

### Byar: handelsnivå 3 och 4

Nivå 1–2 är stenålderns (`gen_data.py`), nivå 3–4 bronsålderns (`gen_bronze.py`), nivå 5 är tom tills järnet. Inget av
järn, guld eller diamant. Formatet: köper = vad byborna tar emot, säljer = vad de ger. Max = hur många gånger innan den
måste fyllas på.

| Yrke | Nivå | Köper (→ 1 Emerald) | Säljer |
|---|---|---|---|
| Armorer | 3 | 3 Bronze Ingot | 9 Emerald → Bronze Helmet (max 3) |
| | 4 | 6 Copper Ingot | 12 Emerald → Bronze Greaves (max 3) |
| Butcher | 3 | 8 Tallow | 1 Emerald → 3 Smoked Meat |
| | 4 | 3 Bear Fat | 2 Emerald → 4 Jerky |
| Cartographer | 3 | 8 Black Pigment | 2 Emerald → Item Frame |
| | 4 | 8 Yellow Pigment | 3 Emerald → Painting |
| Cleric | 3 | 6 White Pigment | 1 Emerald → 4 Red Pigment |
| | 4 | 6 Honeycomb | 1 Emerald → 4 Dried Mushrooms |
| Farmer | 3 | 20 Barley Grain | 1 Emerald → 4 Flour |
| | 4 | 20 Emmer Grain | 2 Emerald → 6 Dried Berries |
| Fisherman | 3 | 4 Whole Tropical Fish | 2 Emerald → 4 Smoked Fish |
| | 4 | 3 Whole Pufferfish | 2 Emerald → 4 Dried Fish |
| Fletcher | 3 | 24 Feather | 2 Emerald → 8 Bone Arrow |
| | 4 | 8 Sinew | 6 Emerald → Sinew Bow |
| Leatherworker | 3 | 4 Scraped Hide | 6 Emerald → Leather Satchel |
| | 4 | 2 Wolf Pelt | 5 Emerald → Leather Leggings |
| Librarian | 3 | 4 Book | 6 Emerald → Bookshelf |
| | 4 | 2 Book and Quill | 8 Emerald → Name Tag |
| Mason | 3 | 20 Andesite Rock | 1 Emerald → 4 Bricks |
| | 4 | 12 Terracotta | 2 Emerald → Clay Bucket |
| Shepherd | 3 | 12 Red Dye | 3 Emerald → White Bed |
| | 4 | 12 Yellow Dye | 1 Emerald → 3 Red Wool |
| Toolsmith | 3 | 6 Crushed Tin Ore | 7 Emerald → Bronze Pickaxe, 5 Emerald → Bronze Saw (max 3) |
| | 4 | 8 Copper Concentrate | 3 Emerald → 2 Tin Concentrate (max 8), 6 Emerald → Bronze Axe (max 3) |
| Weaponsmith | 3 | 3 Bronze Ingot | 5 Emerald → Bronze Knife (max 3) |
| | 4 | 12 Crushed Copper Ore | 8 Emerald → Bronze Spear, 9 Emerald → Bronze Sword (max 3) |

- Priser: ett bronsverktyg 5–12 Emerald, en Bronze Ingot ~1/3 Emerald, mat och råvaror 1–3. Köpen ger 20 xp (nivå 3)
  och 30 xp (nivå 4), säljen 10 / 15; max 12–16 för köp, 3 för bronsverktyg och rustning.
- Toolsmiths Tin Concentrate är en väg till tenn för den som saknar tennådror (dyrt: 3 Emerald för 4 enheter).
- Lerkärl (pottery) läggs till här när det finns.

### Kvar

- Tvättat koncentrat (mer metall per smältning), spjut och svärd i brons, bronslykta, Bloomery mot järnet.
- Crucible of Solid Bronze har samma (kopparfärgade) ikon som Solid Copper; namnet visar metallen.

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
- Kvar: bronsskära och plog, källa för Barley.

- Bronslykta, Bloomery mot järnet, lerkärl i handeln.

## Keramik (byggd)

Drejskiva och lerkärl: förvaring där maten håller längre, och en kruka för vatten. Kod: `Pottery` (registrering,
`JAR_RATE`), `PottersWheelBlock` + `PottersWheelBlockEntity`, `client/PottersWheelRenderer`, `StorageJarBlock` +
`StorageJarBlockEntity`, `WaterJugBlock` + `WaterJugItem`; bränningen i `KilnBlockEntity.RECIPES`. Data:
`scripts/gen_pottery.py` (efter gen_wolf_pelt.py, före gen_block_items.py), texturer: `scripts/gen_pottery_textures.py`.

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
  **Water Jug (3 Clay Balls)**. Lera som blir över när man byter till en mindre form går tillbaka till spelaren.
- **Lera:** högerklicka med Clay Ball, en i taget, tills formen har sin lera. Leran ligger som en skev klump på huvudet.
- **Dreja:** håll in högerklick med tom hand. Varje upprepning (var 4:e tick) är ett steg; **24 steg** (~5 s) ger en
  färdig råform. Leran går klump → centrerad kupol → uppdragen cylinder → öppnad med mynning → färdig form, med
  lerstänk och ljud. Släpper man fortsätter man där man var.
- **Ta av:** tom hand på den färdiga formen → **Unfired Storage Jar** / **Unfired Water Jug** (grå rå lera).
- Bryts skivan: färdig form faller ut som den är, annars Clay Balls tillbaka.

Råformerna bränns i Clay Kiln: **30 s vid 600°**.

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

- Vanillas **Decorated Pot** (4 Brick, receptet är kvar) får vara som den är: dekoration och behållare i strukturerna.
  Krukorna här är egna, funktionella kärl.
- Formen väljs med smyg-klick (inte efter antal lerbollar), så man ser vad man drejar innan leran läggs på.
- **Inte gjort (valfritt):** Clay Bowl/fat, Amphora (större förvaring), att kärl går sönder vid fall/explosion, att
  dricka ur kannan (modden har ingen törst), att hälla kannan i en kittel (Clay Bucket fyller inte heller kittlar).

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
  Fart **−5 % tom, −4 % per lastenhet** (travois −10 % + −10 %), alltså −21 % fullastad. Spring går med upp till 2
  lastenheter (en stor eller två mellanstora); mer än så går inte.
- **Häst:** högerklicka på vagnen när du **rider** en tam häst, eller när du **leder** en tam häst i koppel (inom 10
  block): hästen spänns för och vagnen följer **2,0 block** bakom den. Hästen saktas inte ner. Högerklicka igen för att
  spänna från. Föl kan inte dra.
- **Last:** **4 lastenheter**, två stora eller fyra mellanstora/små kroppar (jättehjorten är för stor). Lasta som på
  travoisen (högerklicka med kroppen du bär eller släpar). Kropparna ligger i två halvor, fram och bak: en stor tar en
  halva, mellanstora ligger två i bredd. **Ta av en kropp:** smyg + högerklicka på kroppen (som på travoisen).
- **Förråd:** **smyg + högerklicka på vagnen** med tom hand öppnar **18 platser** (2 rader), som en kista. Sparas med
  vagnen.
- Rovdjur: den som drar en lastad vagn (spelare eller häst) blir bytet, som med travoisen.
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
- **Barbacka:** högerklicka för att rida, den styrs **utan sadel** men går i **80 %** av sin fart och hoppar **70 %**
  så högt.
- **Hide Saddle Blanket:** läggs i sadelplatsen (smyg + högerklicka på hästen, eller högerklicka med filten): full fart
  och fulla hopp. Syns som en sydd hudfilt med en gjord av rep över ryggen. Tas av i hästens inventory. Bara den tama hästen
  kan bära den.
- **Avel:** mata två tama, vuxna hästar med **Sheaf eller Hay Bale** (tagg `mechanical:horse_breeding_feed`; vanillas
  guldmat fungerar också). Fölet blir en **tam** häst, ägd av den som matade. Med en vanilla-häst eller åsna gäller
  vanillas regler (fölet blir en vanilla-häst eller mula). Annat foder läker 2 hälsa och får föl att växa.
- Släpper Manure som andra hästar. Flyr inte (tam).

### Beslut

- Den tama hästen är en egen entitet (vanilla `Horse` som underklass) med vildhästens modell, i stället för att byta till
  en vanilla-häst med annan färg.
- Ingen sadel i bronsåldern: barbacka fungerar, filten gör den bättre. Vanillas Saddle (järn) fungerar också men syns inte
  på modellen.
- **Inte gjort:** hästrustning syns inte på vildhästmodellen; vagnen visar inte varorna i förrådet; ingen tvåhästvagn.
