# Bronsåldern (plan, utkast)

> Status: **planering**. Inget av detta är byggt än. Beslut och öppna frågor står sist.

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

## Öppna frågor

- Exakta temperaturer (t.ex. lera 600°, koppar 1085°, brons ~950°) och hur snabbt bälgen höjer.
- Hur många gjutningar en Clay Mold klarar.
- Om Whetstone/kallhamring behövs efter gjutning, eller om gjutna huvuden är färdiga direkt.
