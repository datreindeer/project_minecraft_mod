# Tempererade marker (temperate)

Djur och händelser från slätten, lövskogen, den mörka skogen, kärret och floden/kusten (docs/fauna-expansion-plan.md,
"Tempererade marker"). Nya djur: **Wisent**, **European Pond Turtle**, **Water Vole**, **Giant Stag Beetle**,
**Woodpecker**, **Tawny Owl**, **Skylark**, **Lapwing**, **Bittern**, **White-tailed Sea Eagle** och **Eel**. Utbyggda djur:
**rödräven** (vanillas Fox), **hermelinen/stoaten** (moddens Ermine), **vildsvinsflockar** med en **tusker**, **gråsälskolonier**
och **laxvandringen**. Nya block och mekaniker: **Grub Log**, **Bumblebee Nest**, **Fox Den**, **Fox Tracks**, **blodiglar**,
**ål och kräftor i Fish Trap**.

Kod: `wildlife/temperate/` (TemperateFauna, Wisent, PondTurtle, WaterVole, StagBeetle, Woodpecker, TawnyOwl, Skylark (även
lärkan och viporna), Bittern, SeaEagle, Eel, RedFox, StoatDance, SoundersAndColonies, SalmonRun, Leeches, GrubLogBlock,
BumblebeeNestBlock, FoxDenBlock, FoxTracksBlock, TemperateTest), klient `client/wildlife/temperate/` (TemperateClient,
TemperateGeometry, StagBeetleModel, EelModel). Ändrat utanför paketet: `FishTrapBlock` (ål och kräftor), `BirdNestBlock` (SKYLARK, LAPWING),
`VanillaGame` (rävens slakt flyttad hit), `Owl` (kan ha andra sorter), `ColdhuntersFauna.CURES` (öppen för andra paket).
Data: `scripts/gen_fauna_temperate.py`, texturer: `scripts/gen_fauna_temperate_textures.py` (föremål och block) och
`scripts/gen_temperate_models.py` (djurens modeller och hudar).

## Gemensamt

- Allt kött: Campfire 20 s eller smoker 5 s; råköttet förskäms på 1 dag, stekt på 3 dagar, ruttnar till Rotten Flesh, kan
  torkas och rökas. Fågelkött ger som rå kyckling risk för Hunger.
- Plockade fåglar (Plucked Skylark/Lapwing/Bittern): ta det plockade kadavret med tom hand; grillas hela på Campfire (30 s)
  eller skärs med kniv till rått kött.
- Spawn i data (biome modifiers); WildlifeRestock fyller på av sig själv.
- Alla nya djur har egna modeller och texturer (`scripts/gen_temperate_models.py` skriver `TemperateGeometry.java`, hudarna
  och de flådda/plockade kadaverhudarna). Modellerna behåller delnamnen i den modellklass som animerar dem, så gångarten och
  poserna är släktdjurets: wisenten (hög puckel, ullig framdel, skägg, korta horn) rörs av stäppbisonens BisonModel,
  kärrsköldpaddan (låg, rundad sköld, utspärrade simfötter) av landsköldpaddans, vattensorken av åkersorkens, kattugglan
  och havsörnen av skogsfåglarnas, rördrommen av hägerns och hackspetten, sånglärkan och tofsvipan (med sin långa tofs) av
  flockfåglarnas. Ekoxen (stora käkar, täckvingar, sex leder ben) och ålen har egna modellklasser.
- Bilder: `TemperateShots` (flaggfil `run/temperateshots.flag`: rad 1 en utmapp, rad 2 valfritt vilka djur), tre vinklar per
  djur; sparade i `docs/shots/fauna/temperate_*.png`.

## Slätt, solrosslätt, hasseläng

### Red Fox (rödräv) – vanillas räv, utbyggd

| | |
|---|---|
| Var | nu också plains, sunflower plains, Hazel Meadow (vikt 4, 1–2), utöver vanillas taiga |
| Beteende | **plundrar gårdar på natten**: tar ägg från marken och ur fågelbon (Bird Nest med ägg inom 12 block), och dödar höns (inom 24 block). Med ägget i munnen, eller **släpande på den döda hönan**, går den hem till sin **lya** och lämnar ägget/kadavret där. På vägen trycker den **Fox Tracks** (tassavtryck) i marken vartannat block: följ spåren till lyan. |
| Lya | närmaste **Fox Den** inom ~16 block från där räven föddes; finns ingen gräver räven en där (om mobGriefing). Fox Dens genereras också på slätter och i lövskog. |

| Styckning | Antal | Chans |
|---|---|---|
| Raw Fox Pelt | 1 | 90 % (yttre) |
| Game Meat Scraps | 1 | – |
| Sinew | 1 | 30 % |

- **Raw Fox Pelt** torkas på **Drying Rack** (som mårdskinn) till **Fox Fur**.
- **Fox Fur Hat**: 5 Fox Fur + Sinew (`FFF / F F / S`). Hjälm, skydd 1, varm (`fur_insulation`).
- **Fox Tracks**: platta tassavtryck utan kollision; bleknar bort på ungefär en halvtimme.
- **Fox Den**: en jordhög med ett lyhål, fjädrar och benbitar. Bryts till ett Fox Den-block.

### Stoat (hermelin i sommardräkt) – moddens Ermine, utbyggd

Hermelinen spawnar nu också på slätter och i hasseläng (vikt 3, 1–2), och där har den brun sommarpäls. Den **jagar harar**:
innan den anfaller **dansar** den några block från haren i 3 s (hopp, vridningar och kullerbyttor), och haren **står
som förhäxad** och stirrar tills hermelinen slår till. I snön jagar den vita hermelinen lämlar som förut. Slakt som förut
(Coldhunters).

### Skylark (sånglärka) och Lapwing (tofsvipa)

| | Skylark | Lapwing |
|---|---|---|
| Var | plains, sunflower plains, Hazel Meadow, Chalk Downs (6, 2–4) | plains, sunflower plains, Hazel Meadow, swamp, Reed Delta (5, 2–4) |
| Liv | 3 | 4 |
| Beteende | flock på marken. Lärkan **stiger högt (10–20 block) och sjunger** ofta när den är uppe; på marken är den tyst. Vipan flyger lågt och ropar "pee-wit". | |
| Bo | **markbon i gräset** (Bird Nest med lärk- eller vipägg, 0–2 ägg), genereras på slätten | |

| Styckning | Skylark | Lapwing |
|---|---|---|
| Feather (plockning) | 1 | 1–2 |
| Kött | Raw Skylark | Raw Lapwing |

- **Skylark Egg** och **Lapwing Egg**: kokas på Campfire till Boiled Egg.

### Bumblebee Nest (humlebo)

En mossig tuva över ett gammalt mushål på ängen (genereras på slätter och ängar). **På våren och sommaren** fylls det med
honung (0–3); högerklicka med tom hand för **Wild Honey** (1 per nivå). **Smyg**, annars kan humlorna sticka (50 %: lite
skada och Poison 3 s). På hösten och vintern dör samhället undan och ingen honung görs.

- **Wild Honey**: mat (3 hunger) och lindrar (Regeneration 3 s).

## Lövskog och björkskog

### Wisent (visent)

| | |
|---|---|
| Var | forest, birch forest, old growth birch forest, Deciduous Forest (3, 2–4) |
| Liv / fart | 50 / 0,24, skada 7, knuffmotstånd 0,6, steg 1; kalvar (25 %) |
| Utseende | stäppbisonens kropp, mörkare: nästan svart på huvud och ben; tjurar med större puckel och horn |
| Beteende | lugn i små flockar men **farlig när den blir trängd**: alla vuxna (även kor) **vänder sig mot den som skadar dem**, och en spelare som står inom 3 block i 2 s utan att smyga blir **anfallen**. Flocken skyddar kalvar och kan **trampa ner** sådant i vägen när den får panik (som stäppbisonen). **Tjock hud**: Tusk Dagger eller bättre för att stycka. |

| Styckning | Antal | Chans |
|---|---|---|
| Raw Wisent | 3–6 | – |
| Raw Wisent Hide | 1 | alltid (yttre) |
| Horn | 1–2 | 60 % |
| Sinew | 1–2 | 60 % |
| Lard | 1–2 | 30 % |

- **Raw Wisent Hide** + kniv → 3 Scraped Hide; eller + kniv + fett (`tanning_fats`) + Bone → **Wisent Rug** (platt matta).

### Woodpecker (större hackspett)

| | |
|---|---|
| Var | forest, birch forest, old growth birch, Deciduous Forest, dark forest (4, 1–2) |
| Beteende | håller till vid döda träd: landar på toppen av en **Grub Log** inom 24 block och **trummar** på den (en snabb, hög smattrande trumvirvel som hörs ~50 block bort), på dagen. **Följ trummandet** så hittar du det ruttna trädet och larverna i det. |
| Slakt | inget kadaver: 0–1 Feather och 1–2 **Woodpecker Feather** |

- **Woodpecker Feather**: 2 → Red Dye.

### Grub Log (larvstock)

Mjukt, ruttet trä i en död, stående stam (2–4 block hög), full av skalbaggslarver; genereras i lövskog och mörk skog.
Högerklicka med tom hand för **Grubs** (2); de kommer tillbaka med tiden (ungefär en per 7 min). Bryts till 1–2 Stick (och
en Grub om den var full). Inte en stock för yxan.

- **Grub**: bete (`snare_bait`: lockar smådjur till snaran), och en munsbit (1 hunger).

### Tawny Owl (kattuggla)

Som berguven (Owl): sover på dagen, jagar smådjur på natten på ljudlösa vingar, och hoandet hörs (ljusare och darrigare).
Mindre, rödbrun med mörka streck och **utan örontofsar**. Var: forest, birch, Deciduous Forest, dark forest (3, 1). Ger
0–1 Feather och 1–2 Owl Feather.

## Mörk skog

### Wild Boar Sounder (vildsvinsflock) – vildsvinet utbyggt

I dark forest kommer vildsvinen i **stora flockar** (vikt 6, 5–8) med en **tusker** (gammal galt): det första vuxna svinet
i flocken blir en tusker, 30 % större, +14 liv och +3 skada (sparas som attributmodifierare). Tuskern **väntar inte på att
bli skadad**: en spelare inom 7 block (smygande 2,5) blir anfallen, och flockens vuxna anfaller med den. Slakt som
vildsvinet.

### Giant Stag Beetle (ekoxe)

| | |
|---|---|
| Var | dark forest (6, 1–2); forest och Deciduous Forest (2, 1) |
| Liv / fart | 3 / 0,1; hanar (60 %) med jättelika hornlika käkar, honor med korta |
| Beteende | långsam och ofarlig. **Reser sig** med käkarna lyfta och isär mot den som kommer inom 3 block (hotställning), och står kvar. |
| Slakt | inget kadaver: **Beetle Shell** (1) och 0–1 Grub |

- **Beetle Shell**: ensam → 2 Brown Dye; 2 Beetle Shell + en stenbit (`rock_chunks`) → 2 **Black Pigment** (målarfärg).

## Kärr, Willow Fen, Peat Bog, Reed Delta

### European Pond Turtle (kärrsköldpadda)

| | |
|---|---|
| Var | swamp, Willow Fen, Peat Bog (5, 1–3) |
| Liv / fart | 10 / 0,12, rustning 4; ungar (25 %) |
| Beteende | simmar bra. **Solar sig på stockar** vid vattnet på soliga dagar (klättrar upp på en stock med vatten intill och sitter där 1–2 min). Kommer någon nära (6 block, smygande 2,5) **glider den ner i vattnet**; finns inget vatten nära drar den in sig i skalet som landsköldpaddan (skalet tar 80 % av slagen). |

| Styckning | Antal | Chans |
|---|---|---|
| Pond Turtle Meat | 1 | – |
| Pond Turtle Shell | 1 | alltid (yttre) |

- **Pond Turtle Shell** + kniv → 2 Bowl.

### Water Vole (vattensork)

Mörkbrun, dubbelt så stor som åkersorken. Simmar bra och flyr ner i (och under) vattnet med ett plask. Byte för räv
och vildkatt; går i snara. Var: swamp, Willow Fen, Reed Delta, river (5, 1–3). Litet kadaver: **Raw Water Vole**.

### Bittern (rördrom)

En kraftig, streckad brun häger i vassen. Vadar som hägern men litar på kamouflaget: den låter dig komma nära innan den
flyger. **På våren, och i skymning och gryning, "bölar" hanen**: en djup mistlursliknande ton som hörs ~80 block över
kärret. Var: swamp, Willow Fen, Peat Bog, Reed Delta (3, 1). Kadaver: 2–3 Feather, **Raw Bittern**.

### Leech (blodigel)

Vadar du i vattnet i ett kärr (swamp, mangrove swamp, Willow Fen, Peat Bog, Reed Delta) **kan en blodigel suga sig fast** på
benet (12 % var 5:e s, högst 3). Varje igel dricker lite blod (½ hjärta var 5:e s, aldrig under 2 hjärtan) och **släpper
mätt efter ~50 s**, och landar vid dina fötter som en **Leech**. Att gå upp ur vattnet skakar inte av dem.

- **Leech**: gammal medicin. Använd den (lägg den på huden): drar ut gift (tar bort Poison och Nausea) och ger
  Regeneration 5 s.

## Flod, strand, Basalt Coast, Shingle Beach

### Salmon Run (laxvandring)

Hela **Leaffall** (höstens första månad) fylls floderna av lax från havet: nära en spelare i ett flodbiom dyker stim på 3–5
laxar upp var 10:e s (högst 14 inom 32 block), och laxarna **hoppar ur vattnet** då och då. **Brunbjörnar** (inom 32
block) och **uttrar** (inom 16) kommer ner till floden och **tar lax** ur vattnet (laxen äts upp, djuret läker lite).

### Eel (ål)

| | |
|---|---|
| Var | river, swamp, mangrove swamp, Willow Fen, Peat Bog, Reed Delta (vatten, 6, 1–2) |
| Beteende | ligger stilla i dyn på botten om dagen; **kommer ut på natten** och slingrar runt. |
| Död | en **Whole Eel** (som fisk). Whole Eel + kniv → 2 Raw Eel. |

**Ålkistan är Fish Trap**: på natten i ål-vatten (`has_eels`: flod och kärr) är 60 % av fångsterna ål. Ålarna räknas i
fällan och kommer ut som Whole Eel när den töms.

### Crayfish (kräfta)

I **Reaping** (sommarens sista månad, "augusti") fylls Fish Trap i flod och kärr av **Crayfish** (60 % av fångsten där).
Crayfish kokas/grillas till **Cooked Crayfish** (3 hunger).

### Grey Seal Colony (gråsälskoloni) – sälen utbyggd

På klippiga kuster (stony shore, Basalt Coast, Shingle Beach) ligger sälarna i **stora kolonier** (vikt 8, 4–8).
**Kutar föds bara på våren**: inga kutar kommer till världen andra årstider (en kut som spawnar utanför våren blir en
ettåring), och på våren föder sälarna i en koloni nära en spelare kutar tills ungefär var tredje är en kut.

### Sea Eagle (havsörn)

| | |
|---|---|
| Var | stony shore, Basalt Coast, Shingle Beach, beach, river (2, 1–2) |
| Utseende | kungsörnens kropp, större, mörkbrun med ljust huvud, vit stjärt och stor gul näbb |
| Beteende | kretsar högt (SoaringBird) och spanar på vattnet: en fisk nära ytan (högst 2,5 block ner) inom 32 block **slår den ner på och tar ur vattnet** med ett plask (fisken är borta), sedan stiger den igen. Mätt jagar den inte på en minut. |
| Slakt | inget kadaver: 1–2 Feather, 1–3 Eagle Feather, 0–2 Eagle Talon |

## Avancemang

- Vildmark: **Follow the Tracks** (Fox Den), **Henhouse Thief** (Fox Fur), **Red Hat** (Fox Fur Hat), **Forest Giant**
  (utmaning: Raw Wisent Hide), **Shaggy Rug** (Wisent Rug), **Basking** (Pond Turtle Shell), **Antlers of a Beetle** (Beetle
  Shell), **Follow the Drumming** (Grub), **Red Cap** (Woodpecker Feather), **Bumblebee Honey** (Wild Honey), **Bloodsucker**
  (Leech), **Slippery** (Whole Eel), **Crayfish Party** (Crayfish).
- Mat: **Woodland Fare** (stekt wisent, sköldpadda, vattensork, ål eller kräfta), **Larks' Tongues** (stekt lärka, vipa eller
  rördrom), **Eggs in the Grass** (lärk- eller vipägg).
- Jaktfliken: wisent under Big Game (utmaning); vattensork och kärrsköldpadda under Small Game; lärka, vipa och rördrom under
  Fowl; räven (vanilla) under Small Game med sin nya päls.

## Gametester (TemperateTest)

`temperate_spawns` (spawnlistor per biom, och inte i öken/snö/djungel/badlands), `temperate_yields`, `temperate_fox_carries_home`
(räven bär ett ägg till lyan och lämnar spår), `temperate_stoat_dance`, `temperate_wisent_cornered`, `temperate_turtle_basks`,
`temperate_woodpecker_grub_tree`, `temperate_harvests` (Grub Log, Bumblebee Nest), `temperate_calendar_catches` (ål i Fish Trap,
kräftmånaden, laxvandring, kutar, honung, rördromsbölandet), `temperate_otter_takes_salmon`, `temperate_sea_eagle_snatch`,
`temperate_tusker`, `temperate_seal_pup`, `temperate_leeches`.
