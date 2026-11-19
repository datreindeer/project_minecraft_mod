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
- **Inte nu:** modulära verktyg (huvud/skaft/binding som egna delar med stats), jordbruk, tämja hästar.

- **Temperatur:** realistiska grader. Lera bränns vid 600°, brons smälter vid ~950°, koppar vid 1085°. Varje bälgpump
  höjer en bit; utan pump sjunker temperaturen långsamt (pumpa några gånger per minut under smältning).
- **Clay Mold:** håller ungefär 5 gjutningar, sedan spricker den.
- **Efter gjutning:** huvuden kommer ut som **Rough Cast ...** och slipas på en **Whetstone** (några högerklick) innan de
  kan monteras i Tool Station.

## Faser

1. **Malm** (fas 1): ytfynd, vaskning, krossning. Se spec nedan.
2. **Värme:** kilnens temperatur, Bellows, mätare i GUI.
3. **Gjutning:** smält metall i Crucible, Clay Molds, ingots, legering, tvättat koncentrat ger mer metall.
4. **Verktyg och rustning:** koppar- och bronsverktyg, Whetstone, Saw, Bronze Helmet + Greaves, byhandel nivå 3.

## Fas 1: malm (spec)

**Ytfynd (worldgen).** En feature i vegetationssteget provar några kolumner per chunk och letar nedåt (inom chunken) efter
Copper Ore / Deepslate Copper Ore upp till ~64 block under ytan. Hittas malm läggs 1–3 ytfynd på marken nära kolumnen:
**Malachite** (grön sten, vanligast) och ibland **Native Copper** (~15 %). Samma sak över Tin Ore med mörka
**Cassiterite**-stenar. Ytfynden är lösa block som plockas för hand (som Loose Rocks) och ger sitt item.

**Malm och drops.**
- Copper Ore: vanilla (Raw Copper), kräver minst Stone Pickaxe.
- Tin Ore: ger 1–2 **Cassiterite** i stället för blocket.
- Native Copper: ren koppar, smälts direkt i fas 3 (krossas inte).

**Pan (vaskning).** Pan (Tool Station eller crafting av trä/bark, 64 användningar). Högerklick på grus eller sand →
blocket tas och pannan blir fylld (Pan with Gravel / Sand). Använd sedan i vatten (stå i eller sikta på vatten), ~3 s
"använd"-animation → resultat från loot table: Flint, små stenar, **Cassiterite** (vanligare vid floder, stränder och
stenig strand), sällan **Native Copper**, ofta inget.

**Stone Anvil (krossning).** Ett platt stenblock. Lägg malm på det (högerklick, syns ovanpå), slå med hammarsten
(Flint Pickaxe eller Stone Pickaxe) några gånger → krossad malm hoppar av; hammaren slits.
- Raw Copper → 2 **Crushed Copper Ore**
- Malachite → 1 Crushed Copper Ore
- Cassiterite → 1 **Crushed Tin Ore**

Krossad malm används i fas 3 (smälts, eller tvättas i Pan först för mer metall).

## Fas 2: värme (spec)

**Temperatur i Clay Kiln.** Kilnen får en temperatur i grader (sparas, syns i GUI). Kall kiln = omgivningen (~20°,
kallare i snöbiomer via Temperature-systemet).
- **Tänd:** temperaturen stiger mot bränslets **naturliga max** med ~5°/s (20° → 800° på ~2,5 min).
- **Släckt:** sjunker mot omgivningen med ~3°/s. Regn och kyla kyler snabbare.
- **Bälgpump:** varje pump ger **+40°**, upp till bränslets **max med bälg**. Över det naturliga maxet sjunker den
  ~2°/s, så man behöver pumpa ungefär 3 gånger i minuten för att hålla värmen.

| Bränsle | Naturligt max | Max med bälg |
|---|---|---|
| Stick, Sapling, Planks | 500° | 700° |
| Firewood, Log | 650° | 900° |
| Charcoal, Coal | 800° | 1300° |

Slutsats: brons (950°) och koppar (1085°) kräver **Charcoal och bälg**. Trä räcker för lera.

**Temperaturkrav på kilnens recept** (beslutat: gäller alla recept, även stenålderns). Receptet bakar bara när kilnen
är tillräckligt varm (pausar annars):
- Charcoal (av Log/Firewood): 400°
- Brick, Clay Bucket: 600°
- Crucible: 800° (kräver Charcoal-bränsle eller bälg, ett första mål)
- Första bränningen av en ny kiln: räknas bara medan den är minst 500°.
- Fas 3: smältning i Crucible när temperaturen når metallens smältpunkt.

**Bellows.** Ett block som ställs bredvid kilnens eldstad (nedre halvan) med munstycket mot den. Högerklick = en pump
(kort cooldown ~0,5 s): blåsbälgen trycks ihop (animerad modell), luftljud, gnistor och flammor ur kilnens öppning.
Fungerar bara när den pekar på en tänd kiln.
- **Clay Tuyere** (lerrör/munstycke): formas av lera och bränns i kilnen (600°).
- **Bellows:** `[Planks][Scraped Hide][Planks]` / `[Planks][Scraped Hide][Planks]` / `[Twine][Clay Tuyere][Twine]`.

**Mätare.** Kilnens GUI får en lodrät termometer med markeringar vid 600° (lera), 950° (brons) och 1085° (koppar),
färg från mörkröd till vitgul, och exakt temperatur när man håller musen över den. Kilnen syns hetare: mer och ljusare
eld ur öppningen, glödande öppning över ~900°.

**Överhettning (beslutat: kilnen kan spricka).** Taket är 1300°. Pumpar man när kilnen redan är vid taket byggs en
**påfrestning** upp (syns som en varningsstapel i GUI:t och knastrande ljud, sprickor i texturen i 2 steg); den sjunker
långsamt igen när man slutar. Når påfrestningen max **spricker kilnen**: elden slocknar, innehållet i input-slotten
förstörs om det var smält metall (fas 3), och kilnen är **Cracked**. En sprucken kiln blir inte varmare än 600° tills
den lagas: högerklicka med 4 Clay Ball (en i taget), sedan måste lagningen brännas in (som första bränningen, kortare).

**Beslutat:** Bellows är ett eget block bredvid eldstaden; Clay Tuyere krävs.
