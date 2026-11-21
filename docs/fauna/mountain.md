# Bergsfauna (mountain)

Sex djur från bergen och stäppen: **Ibex**, **Chamois**, **Marmot**, **Saiga**, **Cave Bear** och **Cave Hyena**. Alla lämnar ett kadaver som styckas med kniv (Butchery), har eget kött, egen hud/päls och egna delar med användning.
Kod: `wildlife/mountain/` (MountainFauna, Ibex, Chamois, Saiga, MountainBovid, Marmot, CaveBear, CaveHyena), klient `wildlife/mountain/client/`. Data: `scripts/gen_fauna_mountain.py`, texturer: `scripts/gen_fauna_mountain_textures.py`.

**Ibex ersätter vanilla-geten** i snowy slopes, jagged peaks, frozen peaks och stony peaks (`remove_vanilla_goats.json`).

## Gemensamt

- **Fettgarvning** (nytt): rå päls/hud + kniv + fett (Tallow, Lard, Bear Fat eller Marmot Fat, taggen `mechanical:tanning_fats`) i crafting → garvad päls. Kniven stannar kvar. Gäller Ibex, Saiga, Cave Hyena (päls) och Chamois (Chamois Leather).
- Allt kött: Campfire 20 s eller smoker 5 s; råköttet förskäms på 1 dag, stekt på 3 dagar, ruttnar till Rotten Flesh, kan torkas och rökas (`smokable_meat`). Kitteln tar det som all mat.
- Klätterdjuren (Ibex, Chamois) **går rakt uppför naturliga klippväggar** (sten, jord, grus, snö, is, terrakotta – aldrig plank- eller stockväggar) och **flyr uppåt** när en spelare kommer för nära (samma avstånd som AnimalFear, gånger skyggheten). Lång säker fallhöjd.

## Ibex (stenbock)

| | |
|---|---|
| Var | stony peaks (vikt 8, 2–5), jagged peaks och snowy slopes (6, 2–4), frozen peaks, windswept hills, windswept gravelly hills (4, 2–4) |
| Liv / fart | 18 / 0,28, säker fallhöjd 8, steg 1 |
| Kön | 45 % bockar: väldiga knöliga sabelhorn bakåt och skägg; getter korta tunna horn; killingar (30 %) |
| Beteende | hjord, klättrar och flyr uppåt. **Bockar stångas**: sällan, på dagen, går två fria bockar mot varandra, reser sig och slår ihop hornen (smällen hörs långt), slungas bakåt (ingen skada), upp till 4 gånger. Panik bryter det. |
| Ljud | getens, djupare |

| Styckning | Antal | Chans |
|---|---|---|
| Ibex Meat | 2–3 | – |
| Raw Ibex Pelt | 1 | 60 % (yttre) |
| Ibex Horn | 1–2 | 50 % |
| Sinew | 1–2 | 50 % |

- **Raw Ibex Pelt** + kniv + fett → **Ibex Pelt**. 2 Ibex Pelt + Bone → **Ibex Rug**; Ibex Pelt + kniv → 2 Leather.
- **Ibex Horn**: Fletching Table: 2 Sinew + Sinew Bow + 2 Ibex Horn → **Horn Bow** (kompositbåge: pilarna flyger 20 % snabbare = mer skada och räckvidd, säkrare; 384 hållbarhet). Ibex Horn + kniv → Bone Needle.

## Chamois (gems)

| | |
|---|---|
| Var | meadow, cherry grove, grove, windswept forest (4, 2–4); windswept hills, snowy slopes, stony peaks (3, 2–3) |
| Liv / fart | 12 / 0,32, säker fallhöjd 10 |
| Utseende | rödbrun, mörk ryggstrimma, svarta ben, vitt ansikte med svart band genom ögat, korta svarta krokhorn hos båda könen |
| Beteende | mycket skygg (×1,6), flyr uppför klipporna |

| Styckning | Antal | Chans |
|---|---|---|
| Chamois Meat | 1–2 | – |
| Raw Chamois Hide | 1 | 60 % |
| Chamois Horn | 1–2 | 70 % |
| Sinew | 1 | 40 % |

- **Raw Chamois Hide** + kniv + fett → **Chamois Leather** (mjukt sämskskinn).
- Armor Station: 3 Chamois Leather + Sinew → **Chamois Boots** (rustning 1, **+3 säker fallhöjd**).
- **Chamois Horn** + kniv → 2 Bone Fish Hook.

## Marmot (murmeldjur)

| | |
|---|---|
| Var | meadow (8, 3–6), cherry grove (5, 3–5), windswept (gravelly) hills (3, 2–4) |
| Liv / fart | 8 / 0,27; liten (fångas i Snare) |
| Beteende | koloni, var och en har sin **lya** (där den föddes, sparas). En vakt **sätter sig upp** när den ser en spelare (14 block, 5 smygande) eller ett rovdjur (16), och **visslar** efter ~1 s. Visslingen (hörs långt): **alla djur inom 24 block får panik** (AnimalFear; flockfåglar lyfter; jagande rovdjur bryr sig inte), och hela kolonin springer till lyorna och **dyker ner**: osynliga, kan inte träffas, tills inget hotat på 10 s. Visslar också när den skadas. Sover i lyan på natten. |

| Styckning | Antal | Chans |
|---|---|---|
| Marmot Meat | 1 | – |
| Marmot Pelt | 1 | 70 % |
| Marmot Fat | 1–2 | 80 % |

- **Marmot Pelt** (används direkt): Armor Station 3 + Sinew → Fur Hood eller Fur Boots.
- **Marmot Fat**: Campfire → Tallow (ljus, eldpilar …); garvar skinn; 2 + Juniper Berries → 2 **Marmot Salve** (smörj in: Regeneration I 15 s).

## Saiga

| | |
|---|---|
| Var | savanna, savanna plateau (5, 4–8); plains, sunflower plains (3, 4–7); snowy plains, Pine Heath (3, 3–6) |
| Liv / fart | 12 / 0,36 (snabbast), panik dubblar |
| Beteende | hjord, **skyggast av allt vilt** (×2,2: märker en gående jägare på ~22 block), springer ifrån allt till fots – kräver båge (Horn Bow!) eller snaror och tålamod |
| Utseende | kanelsand, vit buk, stor hängande nos; bockar (35 %) bärnstensgula ringade horn |

| Styckning | Antal | Chans |
|---|---|---|
| Saiga Meat | 1–3 | – |
| Raw Saiga Pelt | 1 | 55 % |
| Saiga Horn | 2 | 35 % (bara bockar har horn – chansen står för könet) |
| Sinew | 1 | 40 % |

- **Raw Saiga Pelt** + kniv + fett → **Saiga Pelt**: 2 + Bone → **Saiga Rug**; + kniv → 2 Leather.
- **Saiga Horn**: 4 horn + 5 Stick (ram och kryss) → 4 **Horn Window**: genomskinligt fönsterblock av tunt skrapat horn, bärnstensljus långt före glaset. Saiga Horn + kniv → Bone Needle.

## Cave Bear (grottbjörn)

| | |
|---|---|
| Var | under bergen: windswept hills/forest, stony/jagged/frozen peaks, snowy slopes, grove, meadow, dripstone caves (vikt 2, 1–2: hona med unge). **Spawnar bara i mörker under sten utan himmel** (en lya), aldrig två inom 48 block, inte vid världsgenerering. Spawnar också djupt i grottorna, y 40 till 0, i totalt mörker ([underground.md](underground.md)). |
| Liv / skada | 90 / 11, rustningsmotstånd 0,85, fart 0,28 – större och farligare än Brown Bear |
| Beteende | allt brunbjörnens (reser sig och slår, rusar i korta sprintar, flyr aldrig, honan försvarar ungarna). **Ligger i dvala** i lyan dag och natt (utomhus sover den på natten) tills någon kommer nära – väcks den, ryter den och anfaller. Smygande kommer man närmare. En grottbjörn ute i det fria på vintern letar upp en mörk plats under sten och går i ide som Brown Bear (väcker man den på vintern: **Rude Awakening**). |
| Modell | brunbjörnens kropp 20 % större, med grottbjörnens höga välvda panna |

| Styckning (kräver Tusk Dagger eller bättre) | Antal | Chans |
|---|---|---|
| Cave Bear Meat | 5–8 | – |
| Raw Cave Bear Pelt | 1 | 100 % |
| Raw Hide | 1 | 40 % |
| **Cave Bear Skull** | 1 | alltid |
| Bear Fat | 2–3 | 90 % |
| Bear Claw | 1–3 | 60 % |
| Sinew | 1–2 | 70 % |
| Bone | 2–4 | 70 % |

- **Raw Cave Bear Pelt** torkas på en **hög Drying Rack**, tar hela ställningen (som björnpälsen, värde 22/23) → **Cave Bear Pelt**: Armor Station 2 → Fur Coat, 1 → Fur Leggings; + Bear Claw → **Cave Bear Rug**.
- **Cave Bear Skull**: trofé, ställs på marken med nosen mot den som placerar den.
- Stor kropp (bredd 1,6): kan inte bäras på axlarna, dras eller läggs på travois.

## Cave Hyena (grotthyena)

| | |
|---|---|
| Var | taggen `has_cave_hyenas`: savanna, savanna plateau, windswept savanna, plains, sunflower plains, badlands (alla tre), Hazel Meadow. Flockar 3–5 (vikt 2) vid världsgenerering; på natten var 30:e s 3,5 % chans per spelare (dubbelt vid blodmåne eller med ett kadaver inom 16 block) att en flock kommer 48–64 block bort. Flockar spawnar också i grottorna, y 40 till 0, i totalt mörker, där det alltid är natt för dem ([underground.md](underground.md)) |
| Liv / skada | 20 / 4, fart 0,32 |
| Beteende | nattrovdjur (Predator): sover på dagen, rädd för eld och facklor (utom blodmåne/svält), följer lukten av kadaver. **Asätare, dag som natt**: springer till varje obevakat kadaver inom 24 block (inte hängt, buret eller på travois, ingen eld intill, ingen spelare inom 3 block) – även medan du styckar något annat – och **släpar bort det 20 block** från närmaste spelare; sedan **vaktar flocken** det (spelare inom 7 block jagas bort) och äter det på natten. **Trakasserar** på natten: en flock på minst 3 tar sig an en spelare, cirklar på 5–6 block och fnissar, och en i taget **rusar in och biter** (oftare när du tittar bort), backar sedan ut. Ensam eller två ger de sig inte på människor. Stora flockar (3+) ger sig på ett **ensamt Cave Lion** (gammal rivalitet). Svårt skadad (< 30 %) eller flocken under två → flyr. |
| Ljud | fnissande skratt (häxans kackel, ljusare) |

| Styckning | Antal | Chans |
|---|---|---|
| Hyena Meat | 1–2 | – |
| Raw Hyena Pelt | 1 | 80 % |
| Hyena Fang | 1–2 | 60 % |
| Bone | 1–2 | 60 % |
| Sinew | 1 | 40 % |

- **Raw Hyena Pelt** + kniv + fett → **Hyena Pelt** (fläckig): 2 + Bone → **Hyena Rug**; + kniv → 2 Leather.
- **Hyena Fang**: Fletching Table + Stick + Feather → 4 Barbed Arrow.
- Hyena Meat är segt och mättar mindre (stekt 2 / 0,5).

## Achievements

Wilderness: Lord of the Peaks, Composite Bow, Fat Tanning, Sure-Footed, Sentinel, Steppe Runner, Amber Light, Cave Bear (utmaning), Ice Age Fur, Laughing Thieves. Food: Mountain Fare, Marmot Grease. Hunting-fliken får alla sex automatiskt (More Game).
