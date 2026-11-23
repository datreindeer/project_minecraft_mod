# Kust och flod: Seal, Walrus, Pike, Sturgeon

Fyra djur för kalla kuster och floder (paket `com.mechanical.wildlife.coast`, data i `scripts/gen_fauna_coast.py`,
texturer i `scripts/gen_fauna_coast_textures.py`; djurens modeller och skinn i `scripts/gen_owarm_coast_models.py`). Säl och valross ligger uppe på land och is men flyr ut i vattnet;
gädda och stör är fiskar som simmar, kan spjutas och nappar på kroken.

## Seal (säl)

**Var:** snöiga stränder, klippstränder (stony shore), isflak på frusna hav (frozen / deep frozen ocean), vikt 10,
grupper om 2–5. Några på vanliga stränder (vikt 3, 2–3). Föds bara på torr strand: sand, grus, sten, snö eller is,
med fri himmel ovanför.

**Beteende:** ligger i grupp på stranden och solar sig (lyfter huvud och bakfenor i en "banan"), sover på land om
natten och går i vattnet då och då för att fiska. På land kryper den långsamt (0,12), i vattnet simmar den 3,2 gånger
så fort, dyker och håller andan i 5 minuter, och kommer upp för att andas. **När någon kommer nära glider den ut i
vattnet** (märker dig på 13 block, smygande 5); i vattnet tar den vanliga flykten vid (`AnimalFear`). Smyg för att
komma nära. Ungarna är vita (25 % av en grupp).

**Utseende:** en knubbig spolformad kropp som smalnar av mot höfterna, ett runt huvud utan öron med stora mörka ögon, kort nos med morrhår och V-formade näsborrar, framfenor med klor och två solfjäderformade bakfenor. Pälsen är en knubbsäls: mörkgrå rygg med ljusa ringar, fläckiga grå sidor och ljus buk; ungen är vit.

| | |
|---|---|
| Liv | 16 |
| Storlek | 1,0 × 0,6 (vanlig kropp: kan bäras på axlarna och hängas) |
| Ljud | isbjörnsunge (högre), isbjörnens skadeljud |

**Kadaver (Butchery):**

| Runda | Vad | Chans | Antal |
|---|---|---|---|
| 1 (skinn) | [Raw Sealskin](#produkter) | alltid | 1 |
| 2 (kött) | Raw Seal Meat | – | 2–3 |
| 2 | Blubber | 0,9 | 1–3 |
| 2 | Sinew | 0,4 | 1 |

## Walrus (valross)

**Var:** frusna hav (isflak) och snöiga stränder, vikt 6, flockar om 2–4.

**Beteende:** nästan inte rädd för människor (räckvidd ×0,4). Den som kommer inom 5 block utan att smyga får en
varning: valrossen bölar och kastar upp huvudet med betarna. **Slår tillbaka när den skadas** (8 skada, 80 %
knockback-motstånd) och hela flocken går på den som skadar en unge. Först när den blöder svårt (under 30 % liv) flyr
den ut i vattnet. Simmar tre gånger så fort som den lufsar på land. Ungarna (20 %) saknar betar.

**Utseende:** en väldig, hängande säck till kropp med veck längs sidorna, tjocka halsveck, ett litet runt huvud med små ögon, den breda borstiga mustaschdynan och långa elfenbensbetar, stora nakna fenor. Huden är vårtig och rynkig, rosabrun och mörkare på ryggen; ungen är gråbrun.

| | |
|---|---|
| Liv | 60 |
| Skada | 8 |
| Storlek | 2,0 × 1,2 (**enorm**: kan inte hängas eller läggas på travois, bara släpas) |
| Kniv | **tjock hud**: kräver Tusk Dagger, Ivory Knife eller bättre (`Butchery.toughHide`) |

**Kadaver:**

| Runda | Vad | Chans | Antal |
|---|---|---|---|
| 1 | Raw Walrus Hide | alltid | 1 |
| 2 | Raw Walrus Meat | – | 4–6 |
| 2 | Walrus Ivory | alltid | 2 |
| 2 | Blubber | alltid | 2–4 |
| 2 | Sinew | 0,6 | 1–2 |
| 2 | Bone | 0,7 | 1–3 |

## Pike (gädda)

**Var:** floder, frusna floder, träsk och Willow Fen (vikt 8, 1–2); sällsynt i skogssjöar (forest, birch forest,
taiga, Deciduous Forest, Pine Heath: vikt 2). Vattenvarelse (`WATER_AMBIENT`), försvinner på avstånd som vanlig fisk.

**Beteende:** bakhållsjägare. Står nästan stilla bland vattnet (simmar sällan), fläktar med bröstfenorna, och rusar
sedan mot småfisk (torsk, lax, tropisk fisk) och **sväljer den hel**. Mätt vilar den två minuter. Flyr från spelare
inom 6 block, **utom den som smyger**, så smyg fram och spjuta den. Kan inte tas i hink.

**Utseende:** lång torpedkropp med platt andnäbbsnos och utskjutande underkäke, gyllene öga, rygg- och analfenan långt bak vid den kluvna stjärtfenan, orange bröst- och bukfenor. Olivgrön med rader av ljusa bönformade fläckar och vit buk; fenorna bärnstensfärgade med mörka fläckar.

| | |
|---|---|
| Liv | 8 |
| Skada | 2 (bara mot fisk) |
| Död | [Whole Pike](#produkter) (som vanlig fisk: rensas med kniv) |
| Fiske | Whole Pike i floder, frusna floder, träsk och Willow Fen (vikt 30 mot torskens 60) |

## Sturgeon (stör)

**Var:** floder, sällsynt (vikt 2, alltid ensam). `WATER_CREATURE`.

**Beteende:** en jättestor, bepansrad fisk. Simmar långsamt längs botten, är svår att skrämma (flyr först inom
4 block) och hoppar ibland högt ur vattnet. Lämnar ett **kadaver** (`Butchery.alwaysCarcass`) i stället för ett
föremål. På Hanging Rack blir köttet inte styckdelar (`Butchery.wholeMeat`), bara en extra bit när den hängt färdigt.

**Utseende:** spetsig, platt skovelnos med fyra skäggtömmar framför den rörformade munnen, rader av benplåtar längs rygg, sidor och buk, ryggfenan långt bak och en hajlik stjärt där den övre loben är mycket längre. Gråbrun ovan, ljus under.

| | |
|---|---|
| Liv | 30 |
| Storlek | 1,3 × 0,6 (stor: släpas eller läggs på travois, kan hängas) |
| Fiske | Whole Sturgeon (en ung stör) i floder, vikt 4 |

**Kadaver:**

| Runda | Vad | Chans | Antal |
|---|---|---|---|
| 1 | Raw Hide (störläder) | 0,3 | 1 |
| 2 | Raw Sturgeon | – | 4–6 |
| 2 | Sturgeon Roe | 0,6 | 1–3 |
| 2 | Isinglass | alltid | 1 |
| 2 | Fish Bones | 0,8 | 1–2 |

## Produkter

| Föremål | Från | Användning |
|---|---|---|
| Raw / Cooked Seal Meat | säl | 3 / 7 hunger. Campfire 20 s eller rökare. Röks och torkas (`smokable_meat`). Rått 1 dag, stekt 3 dagar |
| Raw / Cooked Walrus Meat | valross | 3 / 8 hunger. Som ovan |
| Blubber | säl, valross | bränsle i Oil Lamp (`#mechanical:lamp_fuel`) |
| Raw Sealskin → Sealskin | säl | hög Drying Rack, ena halvan (~6 min sol). Sealskin Parka, Sealskin Boots |
| Sealskin Parka | Armor Station: 4 Sealskin, Bone Needle, 3 Sinew | 3 rustning, 20 % värme, **35 % mindre kyla i vatten** |
| Sealskin Boots | Armor Station: 2 Sealskin, Bone Needle, 1 Sinew | 1 rustning, 20 % värme, 35 % mindre kyla i vatten |
| Walrus Ivory | valross | Ivory Knife; med kniv → 2 Bone Needle eller 3 Bone Fish Hook |
| Ivory Knife | Crafting: Ivory, Twine, Stick (som Flint Knife) | kniv i tusk-klass: 1,3× fart, 1,1× kött, 1,3× skinn, 180 hållbarhet, klarar björn och valross |
| Raw Walrus Hide → Walrus Hide | valross | hög Drying Rack, hela ställningen. Med kniv → **6 Cordage**. Walrus Hide Cuirass |
| Walrus Hide Cuirass | Armor Station: 2 Walrus Hide, Bone Needle, 4 Sinew | 5 rustning, 1 seghet, ingen värme |
| Whole Pike → Raw / Cooked Pike | gädda, fiske | rensa med kniv: 2 Raw Pike + Fish Bones. 2 / 6 hunger. Röks och torkas som fisk |
| Whole Sturgeon → Raw / Cooked Sturgeon | fiske (ung stör), kadaver | rensa: 2 Raw Sturgeon + Fish Bones + Isinglass, 40 % Sturgeon Roe. 2 / 7 hunger |
| Sturgeon Roe | stör | kaviar: 3 hunger, 1,2 mättnad, äts snabbt. 1 dag |
| Isinglass | stör | Crafting: 1 → 2 Hide Glue (lim utan kittel) |
| Oil Lamp | Crafting: 5 rock chunks + Plant Fibre | stenlampa: fyll med Blubber (eller Tallow, Lard, Bear Fat) upp till 4, tänd med Fire Drill / Flint and Steel / fackla, tom hand släcker. Ljus 14, ~7 minuter per fyllning |

## Advancements

Vildmark: Seal Hunter → Sealskin → Dry and Warm; Lamp Oil (Oil Lamp); Tusks and Blubber (valross, efter Hunter's
Knife) → Ivory Blade, Walrus Hide → Hide Armour; Water Wolf (gädda, efter Gone Fishing) → King of the River (stör).
Mat: Sea Meat (stekt säl/valross), Caviar.

## Tekniskt

- Säl och valross simmar gärna (`WaterHabit.SWIMMER`, egen simstyrning och egna simrörelser: sälen slår med
  höfter och bakfenor, valrossen sculler med bakfenorna och håller huvudet uppe). Från vattnet tar de sig upp på en
  strandkant på ett block (gemensamt för alla vilda djur, `WildAnimal`).
- `Pinniped`: gemensam bas för säl och valross (amfibisk navigering, egen simstyrning, `EscapeToWaterGoal`,
  `BreatheGoal`, `ChangeElementGoal` för att gå upp på land och ner i vattnet).
- Drying Rack: värden 18/19 (sälskinn, ett block) och 20/21 (valrosshud, båda blocken) i `DryingRackBlock`;
  blockstate och loot läggs till av `gen_fauna_coast.py` efter `gen_preserving.py`.
- Nya Butchery-anrop: `toughHide` (kräver tusk-kniv) och `wholeMeat` (inga styckdelar på Hanging Rack).
- `Temperature`: sälskinnets värme och vattentäthet.
