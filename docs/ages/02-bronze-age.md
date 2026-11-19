# Bronsåldern

> Status: **fas 1 (malm) är byggd**, se "Fas 1: malm (byggd)" sist. Fas 2–4 är plan. Beslut står under Beslut.

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
8. **Rustning.** Gjut **Bronze Plates** i en plåtform och sätt ihop rustning i Armor Station.
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
| Bronze Armor | Rustning | Armor Station |
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
- **Koppar-tier före brons:** ja. Kopparverktyg är ett mjukt mellansteg innan man har tenn.
- **Inte nu:** modulära verktyg (huvud/skaft/binding som egna delar med stats), jordbruk, tämja hästar.

- **Temperatur:** realistiska grader. Lera bränns vid 600°, brons smälter vid ~950°, koppar vid 1085°. Varje bälgpump
  höjer en bit; utan pump sjunker temperaturen långsamt (pumpa några gånger per minut under smältning).
- **Clay Mold:** håller ungefär 5 gjutningar, sedan spricker den.
- **Efter gjutning:** huvuden kommer ut som **Rough Cast ...** och slipas på en **Whetstone** (några högerklick) innan de
  kan monteras i Tool Station.

## Faser

1. **Malm** (fas 1, **byggd**): ytfynd, vaskning, krossning. Se nedan.
2. **Värme:** kilnens temperatur, Bellows, mätare i GUI.
3. **Gjutning:** smält metall i Crucible, Clay Molds, ingots, legering, tvättat koncentrat ger mer metall.
4. **Verktyg och rustning:** koppar- och bronsverktyg, Whetstone, Saw, Bronze Armor, byhandel nivå 3.

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
- **Tin Ore:** ger **1–2 Cassiterite** (Fortune hjälper, Silk Touch ger blocket). Kräver stentier.

### Items

| Item | Varifrån | Används till |
|---|---|---|
| Malachite | Loose Malachite | Stone Anvil → 1 Crushed Copper Ore |
| Native Copper | Loose Native Copper, sällan vaskning | Krossas inte. Smälts direkt i fas 3 (ingen användning än) |
| Cassiterite | Tin Ore, Loose Cassiterite, vaskning | Stone Anvil → 1 Crushed Tin Ore |
| Crushed Copper Ore | Stone Anvil | **Ingen användning än** (fas 3: smälts, eller tvättas först) |
| Crushed Tin Ore | Stone Anvil | **Ingen användning än** (fas 3) |
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
| Gravel | 45 % | 25 % | 18 % | 9 % | 2 % |
| Sand | 69 % | 7,5 % | 10 % | 12,5 % | 1 % |

  Vid **River, Frozen River, Beach, Snowy Beach och Stony Shore** (biom där spelaren står) en extra pool:
  25 % chans till ytterligare en Cassiterite.
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
- Bryts städet ramlar malmen som ligger på det ut.
