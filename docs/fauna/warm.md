# Varma marker (warm)

Nio djur från öken, badlands, savann, djungel och träsk: **Onager**, **Tortoise**, **Desert Lizard**, **Water Buffalo**, **Tapir**, **Leopard** och tre djungelfåglar: **Hornbill**, **Jungle Fowl** och **Peafowl**. Alla lämnar ett kadaver som styckas med kniv (Butchery), har eget kött, egen hud/päls/fjädrar och egna delar med användning.
Kod: `wildlife/warm/` (WarmFauna, Onager, TameOnager, Tortoise, DesertLizard, WaterBuffalo, Tapir, Leopard, JungleFowl, Peafowl; hornfågeln är en FlockBird), klient `client/wildlife/warm/` (WarmFaunaClient, modellerna, WarmAnimalRenderer, TameOnagerRenderer). Data: `scripts/gen_fauna_warm.py`, texturer: `scripts/gen_fauna_warm_textures.py` (kör `gen_fauna_warm_tex_mammals/dry/birds/items.py`).

## Gemensamt

- Allt kött: Campfire 20 s eller smoker 5 s; råköttet (och plockade fåglar) förskäms på 1 dag, stekt på 3 dagar, ruttnar till Rotten Flesh, kan torkas och rökas (`smokable_meat`). Kitteln tar det som all mat. Fågelkött ger som rå kyckling risk för Hunger.
- Plockade fåglar (Plucked Hornbill/Jungle Fowl/Peafowl): ta det plockade kadavret med tom hand; grillas hela på Campfire (30 s) eller skärs med kniv till rått kött.
- Råa hudar skrapas med kniv i crafting till **Scraped Hide** (onager 1, tapir 2, buffel 3) och torkas sedan på Drying Rack till Leather som vanligt.
- Leopardpälsen **fettgarvas** i crafting: Raw Leopard Pelt + kniv + fett (Tallow, Lard eller Bear Fat, `mechanical:tanning_fats`) → Leopard Pelt.
- Spawnar i data (biome modifiers `add_<djur>.json`); WildlifeRestock fyller på av sig själv. Leoparder kommer dessutom på natten (2 % per 30 s per spelare i `mechanical:has_leopards`, djungel och savann, om ingen leopard finns inom 96 block).

## Onager (vildåsna)

| | |
|---|---|
| Var | savanna, savanna plateau, windswept savanna (vikt 5, 3–5); desert och alla badlands (4, 2–4); plains, sunflower plains (1, 2–3) |
| Liv / fart | 22 / 0,34, steg 1 |
| Utseende | smäcker sandfärgad åsna med vit buk och vita ben, mörk ryggstrimma, långa öron, kort upprätt man, tofsad svans; föl (25 %) |
| Beteende | hjord, extremt skygg (skygghet 2,0), slåss aldrig |
| Ljud | åsnans, något ljusare |

| Styckning | Antal | Chans |
|---|---|---|
| Onager Meat | 2–4 | – |
| Raw Onager Hide | 1 | 60 % (yttre) |
| Sinew | 1–2 | 50 % |

**Tämjning** (som vildhästen): smyg fram med hästfoder (`mechanical:horse_feed`) i handen – då står den still och tittar på dig – och handmata den (högerklick, en gång per 10 s). Står du upp innan den litar på dig flyr den. Vid förtroende 3 tar den en **Rope Halter**, vid 7 blir den en **tämjd onager** (ägd av dig). Skadar du den försvinner allt förtroende.

**Tämjd onager** (vanilla-åsna under ytan): rids barbacka (ingen sadel, 85 % fart), 18–24 liv, fart 0,2–0,28, hopp 0,45–0,65. **Pack Baskets** (högerklick) ger en packning med 15 platser (smyg + högerklick öppnar den); en vanlig kista går inte. Korgarna faller av när den dör. Drar **Cart** som en häst. Två nöjda tämjda onagrer vid ett Feeding Trough får ett tamt föl av sig själva.

- **Pack Baskets**: 2 Bark Basket + Scraped Hide + 2 Cordage (`CHC / B B`).

## Tortoise (landsköldpadda)

| | |
|---|---|
| Var | desert, badlands, savann (5, 1–2) |
| Vatten | undviker vatten och simmar dåligt: paddlar långsamt till närmaste strand (som Desert Lizard) |
| Liv / fart | 16 / 0,08, rustning 6, knuffmotstånd 0,8; ungar (35 %) |
| Beteende | flyr aldrig. Kommer en spelare inom 4 block (smygande 1,5) eller skadas den **drar den in huvud, ben och svans i skalet** i 8 s (så länge någon står intill). Indragen tar skalet **80 % av varje slag** (inte eld, fall eller rustningsgenomträngande skada) och går inte att knuffa. |
| Ljud | sköldpaddans, mörkare |

| Styckning | Antal | Chans |
|---|---|---|
| Tortoise Meat | 1–2 | – |
| Tortoise Shell | 1 | alltid (yttre) |

- **Tortoise Shell**: + kniv → 2 Bowl; `PSP / PHP / P` (Planks, skalet, Scraped Hide) → **Shield**; + Twine + Gravel + Stick → **Shell Rattle**.
- **Shell Rattle**: hållen i någon hand håller den rovdjur borta **som en fackla** (`mechanical:scares_predators`), utan eld.
- Utseende: ett högt välvt skal som trappas upp från en utsvängd kantrad till en ås av upphöjda ryggsköldar; varje sköld har mörk fog, olivbruna tillväxtringar och ljus mitt, och skalet är gulbrunt till olivbrunt med en ljus bukplåt med mörka fogar. Fjällig rynkig skalle med hornnäbb på en läderartad hals, grova elefantlika framben med stora fjäll och klor, och en stubbsvans. Huvud, ben och svans dras in helt i skalet. Modell och skinn: `scripts/gen_owarm_reptiles_models.py` (WarmDryGeometry).

## Desert Lizard (ökenödla)

| | |
|---|---|
| Var | desert, badlands, savann (8, 1–2); Pine Heath (3, 1–2) |
| Liv / fart | 4 / 0,36; ungar (20 %) |
| Beteende | snabb och skygg (1,4). På dagen i full sol **solar den** platt mot marken 20–60 s och märker dig då senare (0,7). Träffas den av ett slag från någon som inte dödar den **tappar den oftast (70 %) svansen**: slaget gör ingen skada, en **Lizard Tail** faller, den får Speed III i 5 s och flyr. Svansen växer ut på en dag. Går i snara. |
| Ljud | silverfiskens, ljusare |

| Styckning | Antal | Chans |
|---|---|---|
| Lizard Tail (köttet) | 1 | – |
| Lizard Skin | 1 | 60 % (yttre) |

- **Lizard Tail** steks till Cooked Lizard Tail (2 mat).
- **Lizard Skin**: 3 → 1 Leather.
- Utseende: låg platt kropp, kilformat huvud med käke, öga i en ljus ring och mörk öronöppning, utåtspretande ben med armbågarna ut och fötter med långa tår, och en lång avsmalnande svans i fyra leder. Sandfärgad med mörka tvärband, ljusa fläckar, ringad svans och ljus buk. Modell och skinn: `scripts/gen_owarm_reptiles_models.py`.

## Water Buffalo (vattenbuffel)

| | |
|---|---|
| Var | jungle, sparse jungle, swamp, mangrove swamp, Willow Fen (6, 3–5) |
| Liv / fart | 48 / 0,23, skada 8, knuffmotstånd 0,7, steg 1; kalvar (30 %) |
| Utseende | en massiv, djup kropp (omkring 1,6 m i manken) med en puckel över manken och hängande buk, en tjock rynkig hals, det långa huvudet buret lågt och framåt med bred våt mule, hängande öron under hornen och hos alla vuxna de väldiga platta, räfflade hornen som sveper ut och bakåt i en bred halvmåne. Skiffergrå till svartaktig gles hud, ljusgrå strumpor, två ljusa vinklar på strupen, lera på benen, tunn svans med tofs. Kalven är brunare och luddigare med stora öron, hornknoppar och långa ben. Modell och skinn: `scripts/gen_owarm_hoofed_models.py` (WarmHoofGeometry). |
| Beteende | inte skygg (0,6). Skadar du en **anfaller alla vuxna buffler inom 16 block**. På dagen går den till grunt vatten eller lera inom 12 block och **vältrar** sig där 20–40 s. Vadar gärna och **simmar gärna** över djupt vatten: lågt som en flodhäst, bara huvud och horn över ytan, nosen högt så att hornen ligger bakåt. **Tjock hud**: kräver Tusk Dagger eller bättre för att styckas. |
| Ljud | kons (moody), mycket mörkare |

| Styckning | Antal | Chans |
|---|---|---|
| Buffalo Meat | 3–6 | – |
| Raw Buffalo Hide | 1 | 80 % (yttre) |
| Buffalo Horn | 1–2 | 60 % |
| Sinew | 1–2 | 50 % |
| Lard | 1–2 | 40 % |

- **Raw Buffalo Hide** + kniv → 3 Scraped Hide.
- **Buffalo Horn**: + kniv → 2 Bone Needle; 2 horn runt en Torch med 2 Cordage (` C / HTH / C `) → **Lantern** (hornlykta).

## Tapir

| | |
|---|---|
| Var | jungle, sparse jungle, bamboo jungle (6, 1–2) |
| Liv / fart | 26 / 0,26, steg 1; randiga ungar (40 %) |
| Utseende | malajisk tapir: en tung, rund päronformad kropp som är högst över länden, en kort rörlig snabel över överläppen, små ögon, runda öron med vit kant, korta kraftiga ben med utspärrade tår och en stump till svans. Svart med en mjukt kantad gräddvit sadel över bakre halvan och buken. Ungen är kastanjebrun med gräddvita ränder och prickar ("vattenmelon"). Modell och skinn: `scripts/gen_owarm_hoofed_models.py` (WarmHoofGeometry). |
| Beteende | skygg (1,6). Skrämd **springer den till närmaste djupa vatten** (2 block) inom 16 block och stannar i vattnet tills paniken är över; annars flyr den som andra. **Simmar gärna** och fort: kroppen nästan under ytan, snabeln uppåt som en snorkel, snabba hundsimtag. |
| Ljud | grisens (stora), mörkare; tyst annars |

| Styckning | Antal | Chans |
|---|---|---|
| Tapir Meat | 2–4 | – |
| Raw Tapir Hide | 1 | 60 % (yttre) |
| Lard | 1–2 | 50 % |
| Sinew | 1 | 30 % |

- **Raw Tapir Hide** + kniv → 2 Scraped Hide.

## Leopard

| | |
|---|---|
| Var | `#mechanical:has_leopards` (djungel och savann) (2, 1); nattankomster (se ovan) |
| Liv / fart | 24 / 0,32, skada 4 (språng 8), steg 1, inga ungar |
| Beteende | ett nattrovdjur som ärver grottlejonets jakt: kryper hukad, fryser när bytet tittar, **språng** från 2,5–7 block, rytande när den blir upptäckt, rädd för eld och facklor (och Shell Rattle). Byten: tapir, onager, djungelhöna, påfågel, rådjur, mufflon, vildsvin, ökenödla, buffelkalvar, människor. **På dagen klättrar den upp i ett träd** inom 14 block (klättrar uppför stammen genom lövverket) och ligger på trädkronan; därifrån kastar den sig ner över byten i skymningen. Ett färskt kadaver (inte stort) **släpar den upp i trädet** och lämnar bland grenarna; utan träd i närheten drar den det bort som lejonet. Tar ingen fallskada av de första 10 blocken. |
| Ljud | ocelotens, mycket mörkare |

| Styckning | Antal | Chans |
|---|---|---|
| Leopard Meat | 1–3 | – |
| Raw Leopard Pelt | 1 | alltid (yttre) |
| Sinew | 1 | 50 % |
| Bone | 1–2 | 60 % |

- **Raw Leopard Pelt** + kniv + fett → **Leopard Pelt**. 2 Leopard Pelt + Bone → **Leopard Rug** (platt matta, Bear Rug-modellen); Leopard Pelt + kniv → 2 Leather.
- Utseende: guldgul rygg som bleknar till gräddvit buk, med riktiga rosetter (mörka ringar brutna i 3–5 fläckar runt en lite mörkare mitt) på rygg och flanker och massiva fläckar på huvud, ben och buk. Runt huvud med kort nos, vita morrhårsdynor med prickar, grönbärnstensgula ögon och små runda öron med svart baksida och vit fläck. Den långa svansens fläckar övergår i ringar och slutar i en mörk spets. Modell och skinn: `scripts/gen_owarm_reptiles_models.py`; mattan och pälsen behåller sina föremålstexturer.

## Hornbill (hornfågel)

| | |
|---|---|
| Var | jungle, sparse jungle, bamboo jungle (6, 2–4) |
| Liv | 8; en pil dödar |
| Beteende | flockfågel (FlockBird): flyger högt (7–16 block), sätter sig i trädkronorna, högt skrän |

| Styckning | Antal | Chans |
|---|---|---|
| Feather | 2–4 | alltid (plockning) |
| Hornbill Casque | 1 | alltid (plockning) |
| Raw Hornbill | 1 | – |

- Utseende: svart kropp med vit buk och vita lår, raggig gräddvit hals och krage, svart ansikte med rött öga, den långa nedåtböjda gula näbben i tre steg under den stora uppåtböjda hjälmen (casque) med svarta fram- och bakkanter, svarta vingar med gräddvitt band och vita spetsar, och en lång vit stjärt med svart band. Modell och skinn: `scripts/gen_owarm_birds_models.py` (WarmBirdGeometry).
- **Hornbill Casque** ("rött elfenben"): + kniv → 2 Bone Needle; ensam → 2 Orange Dye.

## Jungle Fowl (röd djungelhöna)

| | |
|---|---|
| Var | jungle, sparse jungle, bamboo jungle (8, 2–4) |
| Liv / fart | 5 / 0,27; 35 % tuppar, kycklingar (30 %) |
| Beteende | som fasanen: trycker och flyger upp när du kommer nära. Tuppen gal i gryningen. **Hönor värper ett Jungle Fowl Egg** på marken var 5–10:e minut. Går i snara. |
| Ljud | hönans |

| Styckning | Antal | Chans |
|---|---|---|
| Feather | 1–3 | alltid |
| Raw Jungle Fowl | 1 | – |

- Utseende: tuppen har sågtandad röd kam och slör, rött bart ansikte med vit örontofs, gyllene nackfjädrar (hackles) som faller över axlarna, rödbrun sadel, grönsvart bröst, en hög hopfälld stjärt med de glänsande bågformade skärorna (sickle) och grå ben med sporrar. Hönan är spräcklig brun med ljusare bröst och liten kam. Modell och skinn: `scripts/gen_owarm_birds_models.py`; kycklingen behåller PheasantChickModel.
- **Jungle Fowl Egg**: kokas på Campfire till Boiled Egg. **Kastat** kan det kläcka en kyckling som ett vanligt ägg – hönans vilda förfader.

## Peafowl (påfågel)

| | |
|---|---|
| Var | jungle, sparse jungle, bamboo jungle (4, 2–3); savann (2, 2–3) |
| Liv / fart | 8 / 0,25; 40 % tuppar, inga ungar |
| Beteende | som fasanen. Tuppen **reser ibland stjärten till ett solfjäder** i 6 s (skriker och skallrar med den), fäller den när den skräms. |
| Ljud | papegojans (fantom-imitation) som skrik |

| Styckning | Antal | Chans |
|---|---|---|
| Feather | 2–3 | alltid |
| Peacock Feather | 1–3 | 60 % |
| Raw Peafowl | 1 | – |

- Utseende: påfågeltuppen har blå hals och blått bröst, en krona av nakna fjäderskaft med blå tofsar, vita streck över och under ögat, fjällig grön rygg, bandade vingar med kastanjebruna handpennor och ett släp av sju långa paneler med ögonfläckar som reser sig till ett stort solfjäder. Hönan är brun med grön hals, vit buk och krona. Modell och skinn: `scripts/gen_owarm_birds_models.py`.
- **Peacock Feather**: + kniv → 3 Feather (fjädring); 2 → Cyan Dye.

## Avancemang

- Bronsåldern: **Beast of Burden** (tämj en onager), **Pack Animal** (Pack Baskets).
- Vildmark: **Slow and Steady** (Tortoise Shell), **Rattle** (Shell Rattle), **Left Holding the Tail** (Lizard Tail), **Herd Bull** (utmaning: buffelhorn/hud), **Tree Cat** (utmaning: leopardpäls), **Rosettes** (Leopard Rug), **Jungle Plumage** (Peacock Feather eller Hornbill Casque).
- Mat: **Far Country Fare** (stekt kött från de nya djuren), **First Chicken** (Jungle Fowl Egg).
- Jaktfliken: onager, buffel, tapir under Big Game; ökenödla, sköldpadda under Small Game; leopard under Predators; fåglarna under Fowl.
