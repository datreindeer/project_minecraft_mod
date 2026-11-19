# Bronsåldern

> Status: **fas 1 (malm), fas 2 (värme), fas 3 för koppar och fas 3b brons (legering, bronsverktyg, Bronze Saw, Bronze
> Helmet + Greaves, byhandel nivå 3) och odlingens fas 3 (Quern, bröd, kompost, Manure, Scarecrow) är byggda**, se
> avsnitten sist. Kvar: tvättat koncentrat, spjut/svärd, mot järnet.
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
- **Inte nu:** modulära verktyg (huvud/skaft/binding som egna delar med stats), tämja hästar. (Jordbruket kom till
  slut med: se Odling nedan.)

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
gen_block_items → gen_unlocks → gen_registry.

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
  Native Copper = 2** (nästan ren metall), Crushed Tin Ore = 1 och Bronze Ingot = 2 (se Fas 3b). Malachite och Raw
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
  Två formar till för brons (Saw Blade, Plate), se Fas 3b. Fel metall: "Copper can't be cast in this mold".

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

Bara brons kan gjutas i dem. De andra formarna gjuter brons som koppar: Bronze Ingot, Rough Cast Bronze Pickaxe/Axe/
Shovel/Hoe Head och Knife Blade. Allt Rough Cast slipas på Whetstone (4 drag) till Bronze ... Head / Blade / Plate.

### Bronsverktyg

Monteras i **Tool Station**, låses upp när man bär det slipade huvudet.

| Verktyg | Huvud | Binding | Skaft |
|---|---|---|---|
| Bronze Pickaxe / Axe / Shovel / Hoe | Bronze ... Head | Cordage | Tool Rod |
| Bronze Knife | Bronze Knife Blade | Twine | Stick |
| Bronze Saw | Bronze Saw Blade | Twine | Stick |

- **Bronsmaterial:** 230 hållbarhet (koppar 190, järn 250), hastighet 6 och +2 skada som järn, förtrollbarhet 12.
  Attackvärden som vanillas järnverktyg. Lagas med Bronze Ingot.
- **Brytnivå:** `mechanical:incorrect_for_bronze_tool` = `#minecraft:needs_iron_tool` + `#minecraft:needs_diamond_tool`.
  Bronze Pickaxe bryter alltså **Iron, Coal och Lapis Ore** (`needs_bronze_tool`) och Tin Ore, men inte guld, diamant,
  redstone eller smaragd (de väntar på järnet). Fungerar som hammarsten på Stone Anvil.
- **Bronze Knife:** 350 hållbarhet, styckar **1,75×** så snabbt och ger **1,35×** så mycket som Flint Knife, **klarar
  Brown Bear** och Giant Deer (knivnivå 2 som Tusk Dagger). Räknas som kniv och svärd.
- Spjut och svärd i brons är inte gjorda (valfria).

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

### Byar: handelsnivå 3

| Yrke | Köper | Säljer |
|---|---|---|
| Armorer | 3 Bronze Ingot → 1 Emerald | 9 Emerald → Bronze Helmet |
| Toolsmith | 6 Crushed Tin Ore → 1 Emerald | 7 Emerald → Bronze Pickaxe, 5 Emerald → Bronze Saw |
| Weaponsmith | 3 Bronze Ingot → 1 Emerald | 5 Emerald → Bronze Knife |

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
