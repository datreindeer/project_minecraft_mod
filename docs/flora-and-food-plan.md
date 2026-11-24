# Plan: undervegetation, kryddor, mat och sågverk

> Status: **plan skriven 2026-11-23, fas 1 byggd samma dag; fas 2 och 3 samt fas 4, 4b och 5 (köket) byggda 2026-11-24**
> (se "Fas 1: resultat", "Fas 2 och 3: resultat" och "Fas 4, 4b och 5: resultat" sist). Beslut märkta
> "Beslut (Claude, kan ändras)" är mina förslag; säg till så ändras de. Item- och blocknamn står på engelska.

Användarens önskelista (översatt och sorterad):

1. **Undervegetation:** fler blommor, stora och små, vanliga och magiska (ultrasällsynta). Bärbuskar i flera storlekar
   som växer ihop till stora snår. Bärbuskarna ska följa klimat och temperatur så att de inte alla växer på samma
   ställe. Fler växter i biomer som har lite. Bär som är giftiga råa. Bär från träd (nya trädsorter?).
2. **Kryddor, örter, grönsaker och frukt:** vilda sorter som kan "tämjas" (förädlas) till odlade. Vissa säljs bara av
   bybor, vissa bara av en karavan "från fjärran öster". Rätter som passar åldern man är i, med köksredskap av brons,
   järn och stål.
3. **Sågverk** när vattenkraften kommer: ett multiblock som sågar stockar i mitten. Stock med bark → Rough Logs, Firewood,
   Bark, Sawdust. Barkad stock → Planks, Sticks, Sawdust. Ger mycket mer än såg/yxa men kräver ett vattenhjul med nog kraft
   och en ränna där vatten flyter åt rätt håll och för stocken fram till sågen.

---

## 1. Vad som finns i dag

| Sort | Finns | Var det står |
|---|---|---|
| Bärbuskar (1 block, korsmodell, 4 stadier, fryser på vintern) | Raspberry, Blueberry, Lingonberry (vintertålig), Wild Strawberry, Elderberry, Cloudberry, Blackberry | `Foraging`, `WildBerryBushBlock`, `gen_data.py`, `gen_berry_textures.py` |
| Buskar i skogen | Hazel Shrub (hasselnötter), Juniper (enbär), Bramble, Undergrowth, Sapling Thicket, Heather | `ForestUndergrowth` |
| Svamp | Chanterelle, Porcini, Morel, Oyster Mushroom, Fly Agaric (giftig), Puffball | `Foraging` |
| Vilda grödor | Wild Einkorn, Wild Emmer, Wild Pea, Wild Carrot Plant, Sea Beet, Stinging Nettle | `docs/farming.md` |
| Blommor | Vanillas blommor, Fireweed (Scorched Barrens), Spirit Bloom (Ancient Oak, magisk) | |
| Matlagning | Campfire, Baking Stone, Clay Kiln, Quern (brons), Bronze Kettle + Tripod (stuvning, buljong), Fire Pit, stor Fire Pit (stål) | åldersdokumenten |

**Glest i dag:** Chalk Downs, Limestone Karst, Mammoth Steppe, Slate Highlands, Glacial Moraine, Shingle Beach, Basalt
Coast, Gypsum Flats, Ochre Badlands, Marble Gorge, Pine Heath (bara ljung och lingon) och vanillas öken, savann, badlands
och snöslätt. Ingen bärbuske växer i varmt och torrt land, och ingen på kalla stäpper eller stränder.

## 2. Klimatet styr var växter växer

Varje vild växt får ett **klimatfönster**: biomens grundtemperatur (vanilla-skalan: snö −0,5…0, taiga 0,25, slätt 0,8,
öken 2,0) och fuktighet (downfall 0…1). Generatorn läser temperatur och fuktighet för **alla** överjordsbiomer (vanillas
ur jar-filen, moddens ur deras json) och lägger växten i de biomer som ligger inom fönstret. En ny biom får alltså rätt
växter av sig själv, och två växter med olika fönster hamnar aldrig på samma ställen.

- **Beslut (Claude, kan ändras):** klimatet väljs i generatorn (data), inte med en egen placeringsregel i Java. Det blir
  samma biomlistor som förut, bara uträknade. Ett fåtal växter är markspecialister (orkidén på kalk, strandtrift på
  kusten): de har en fast biomlista i stället.
- **Beslut (Claude, kan ändras):** i spelet (inte bara i worldgen) sprider sig och växer snåren bara där biomens
  grundtemperatur ligger inom fönstret (±0,15). En buske som flyttats till fel klimat lever men breder inte ut sig.
- Säsongen styr blomning och mognad (avsnitt 3 och 4); vinterfrosten som förut (`Thaw`, `SeasonLook.frost`).
- **Fas 2:** de sju gamla bärbuskarna flyttas också till klimatfönster (i dag fasta biomlistor). Förslag:

| Buske | Temp | Fukt | Mognar |
|---|---|---|---|
| Raspberry | 0,5–0,8 | 0,5–0,9 | sommar |
| Blueberry | 0,2–0,7 | 0,6–1,0 | sommar–höst |
| Lingonberry | −0,5–0,4 | 0,4–0,9 | höst (vintertålig) |
| Wild Strawberry | 0,5–0,9 | 0,3–0,8 | vår–sommar |
| Elderberry | 0,5–0,9 | 0,5–1,0 | sensommar–höst |
| Cloudberry | −0,5–0,3 | 0,6–1,0 (myr) | sommar |
| Blackberry | 0,2–0,9 | 0,4–0,9 | sensommar–höst |

## 3. Blommor

Alla blommor har en **blomningssäsong**. Utanför den står plantan kvar som gröna blad (egen bild utan blommor), och på
vintern blir bladen grå och frostiga (`gen_winter_plants`, samma frostfärg som buskarna). De går att plocka och plantera
som vanillas blommor (blocket är sitt eget item).

### Vanliga, små (fas 1)

| Blomma | Biomer | Temp / fukt | Blommar | Utseende |
|---|---|---|---|---|
| **Pasque Flower** (backsippa) | Chalk Downs, Limestone Karst, Mammoth Steppe, Pine Heath, slätter | 0,25–0,8 / 0–0,5 | vår | lila klockor med gula ståndare, silverluddiga stjälkar |
| **Harebell** (liten blåklocka) | Chalk Downs, Slate Highlands, Glacial Moraine, Pine Heath, Shingle Beach | 0,1–0,75 / 0,3–0,7 | sommar, höst | ljusblå hängande klockor på trådsmala stjälkar |
| **Lady's Slipper** (guckusko) | Limestone Karst, Chalk Downs (kalk) | fast lista | vår, sommar | gul "sko" med vinröda vridna kronblad |
| **Sea Thrift** (strandtrift) | Shingle Beach, Basalt Coast, Stony Shore, Beach | fast lista | vår, sommar | rosa bollar på en tät grön tuva |
| **Mountain Avens** (fjällsippa) | Glacial Moraine, Slate Highlands, Snowy Plains, Grove, Windswept Hills | −0,5–0,4 / 0,2–0,7 | sommar | vit med åtta kronblad och gul mitt, lågt mattliknande |
| **Edelweiss** | Slate Highlands, Mammoth Steppe, Windswept Hills | fast lista | sommar | ullvita stjärnor, grågröna blad |
| **Sand Verbena** | Desert, Gypsum Flats, Badlands | ≥1,5 / ≤0,1 | vår, sommar | rosa kupor av små blommor, krypande stjälk, växer på sand |
| **Desert Marigold** | Savanna, Badlands, Ochre Badlands, Marble Gorge | ≥1,0 / ≤0,2 | vår–höst | gul prästkrage på grå ludna blad, sand eller jord |

### Vanliga, stora (två block, fas 1)

| Blomma | Biomer | Temp / fukt | Blommar | Utseende |
|---|---|---|---|---|
| **Great Mullein** (kungsljus) | Chalk Downs, Mammoth Steppe, Pine Heath, Limestone Karst, Marble Gorge | 0,3–1,2 / 0–0,5 | sommar | grå ullig bladrosett, hög spira med gula blommor |
| **Desert Candle** (stäppljus, Eremurus) | Gypsum Flats, Ochre Badlands, Marble Gorge, Savanna Plateau, Mammoth Steppe | 0,3–2,0 / ≤0,1 | vår, sommar | smal 2-blocks spira tät av aprikosfärgade stjärnor |

### Magiska, ultrasällsynta (fas 1)

- **Fern Flower** (ormbunksblomman): folktron säger att ormbunken blommar en enda natt, midsommarnatten, och den som
  hittar blomman får tur. Ser ut som en ormbunke med en hoprullad knopp; **bara på sommarnätter** slår knoppen ut i en
  glödande gyllene blomma (ljus 9, gyllene och gröna gnistor som stiger). Lövskog, skog, björkskog, mörk skog, gammal
  taiga: ~1 per 400 chunks. Plockas den medan den blommar: **Fern Flower** (kan planteras igen och följer samma regel)
  och **Luck i 10 minuter**. Plockad när den inte blommar ger den bara Plant Fibre: man måste hitta den på natten.
- **Frost Lily** (frostlilja): en blekblå, nästan genomskinlig lilja som **bara blommar på vintern**, i snön på kalla
  höjder. Lyser svagt (ljus 6), små isglittrande partiklar. Resten av året är den en låg mörkgrön bladtuss. Slate
  Highlands, Glacial Moraine, Snowy Plains, Snowy Taiga, Grove, Snowy Slopes: ~1 per 250 chunks. Plockad i blom:
  **Frost Lily**, annars ingenting.
- **Fas 3 (idéer):** Will-o'-Wisp Lily i Peat Bog (lyser bara i dimma), Moonwort (låsgräs, folktrons nyckelört som
  öppnar lås) i Limestone Karst. Användning: Fern Flower i Charm-platsen (tur), Frost Lily-te mot kyla (`Temperature`).

### Fas 2, fler blommor

Foxglove (fingerborgsblomma, giftig, skogsbryn), Yellow Adonis (stäpp), Bog Asphodel (Peat Bog), Marsh Marigold (Willow
Fen, tidig vår), Wood Anemone (vitsippa, Deciduous Forest, vår, mattor), Saxifrage (Slate Highlands), Purple Loosestrife
(Reed Delta), Spiny Thrift / Desert Rose (Gypsum Flats), Bluebells (lövskog, vårmattor). Pine Heath får Cat's Foot och
Bearberry (marktäckande bär).

## 4. Bärbuskar och snår

### Storlekar och snår (fas 1)

De nya buskarna är **snårbuskar** (`ThicketBushBlock`, bygger på `WildBerryBushBlock` så att björnar, fåglar, stammar
och Oak Folk kan plocka dem som förut):

1. **Ung buske** (litet block, ~9 px hög): bara blad, ger inga bär. Växer till fullvuxen på ~10 min i rätt säsong och klimat.
2. **Fullvuxen buske** (~15 px, ett rundat bladverk med grenar inuti): blommar, får omogna och mogna bär.
3. **Snår:** två fullvuxna buskar av samma sort **bredvid varandra växer ihop**: bladverket fyller glappet mellan dem
   (sidorna kopplas som ett staket), så en grupp blir en sammanhängande massa.
4. **Stort snår:** en fullvuxen buske med minst två grannar i snåret kan **växa på höjden** (ett andra block ovanpå,
   med rundad topp). Mitten av ett snår blir alltså 2 block hög medan kanterna är låga.
5. **Snåret breder ut sig:** en fullvuxen buske sätter då och då en ung buske på en ledig grästuva bredvid sig, så länge
   det finns färre än 9 av samma sort inom 2 block och klimatet passar.

Alla tre fas 1-sorterna har **taggar**: att gå genom snåret bromsar och sticker (som Bramble), smyger man gör det inte ont.
Bryter man en buske: 1–2 Sticks och 1–2 Plant Fibre; det övre blocket faller om det undre tas bort.

**Beslut (Claude, kan ändras):** buskarnas stadier följer **säsongen** och inte bara tiden: varje sort har ett mål per
säsong (blad, blommor, omogna, mogna) och går ett steg i taget mot det. Plockar man en mogen buske under mognadssäsongen
går den tillbaka till omogen och mognar igen (~6 min per steg); efter säsongen faller bären av. Det betyder att man
inte får slånbär på våren. Säsongerna är långa (90 timmars spel), så varje sort har mogna bär under minst en hel säsong.

### Nya bärbuskar (fas 1)

| Buske | Bär | Temp / fukt | Vår | Sommar | Höst | Vinter | Mat |
|---|---|---|---|---|---|---|---|
| **Blackthorn** (slån) | **Sloes** | 0,5–0,9 / 0,3–0,8 | vita blommor | omogna | mogna | kala grenar med kvarhängande bär | 1 hunger (sträva) |
| **Sea Buckthorn** (havtorn) | **Sea Buckthorn Berries** | 0,0–0,5 / 0–0,6 | blad | omogna | mogna | kala grenar täta av orange bär | 1 hunger |
| **Jujube** (bröstbär) | **Jujubes** | 1,0–2,0 / 0–0,5, även på sand | blad | blommor → omogna | mogna | kal där det fryser, annars blad | 2 hunger |

Blackthorn hamnar i slätter, ängar, Chalk Downs, Hazel Meadow, lövskog och Limestone Karst; Sea Buckthorn på Mammoth
Steppe, Glacial Moraine, Shingle Beach, Basalt Coast, Slate Highlands, Pine Heath, snöslätt och klippstrand; Jujube i
savann, badlands, Ochre Badlands, Gypsum Flats, Marble Gorge och öken (på sand). Exakt lista: körningen av
`gen_flora.py` skriver ut den.

### Giftiga råa bär (fas 1)

- **Elderberries** är nu giftiga råa (som i verkligheten): Poison 4 s och Nausea 10 s. **Cooked Elderberries** (Campfire,
  15 s) är ofarliga och mättar mer (2 hunger, mättnad som lagad mat). Torkade (Drying Rack) är också ofarliga, som förut.
- **Beslut (Claude, kan ändras):** fler giftiga råa kommer i fas 2: Rowan Berries (rönnbär, beska och lite giftiga råa,
  goda kokta till gelé), Yew Arils (idegran, fröet är dödligt, bara fruktköttet ätligt: Poison om man äter dem hela) och
  Bittersweet Nightshade (besksöta, giftig hela vägen, bara för färg och medicin).

### Fler buskar (fas 2)

| Buske | Var | Notis |
|---|---|---|
| Rosehip (nyponros) | Chalk Downs, slätter, kust | snår, mogna nypon höst–vinter, Rosehip Tea |
| Crowberry (kråkbär) | Pine Heath, Glacial Moraine, Slate Highlands | marktäckande, vintertålig |
| Bearberry (mjölon) | Pine Heath, sandiga tallhedar | marktäckande |
| Barberry (berberis) | Marble Gorge, Limestone Karst | snår med taggar, sura bär till kryddor |
| Caper Bush (kapris) | Marble Gorge, kalkklippor | kryddknoppar (avsnitt 5) |
| Saskatoon / Juneberry | Hazel Meadow | snår |
| Wild Currant (vinbär, röda och svarta) | Willow Fen, Reed Delta | fuktigt |
| Prickly Pear | Gypsum Flats, Ochre Badlands | kaktus med frukt (märks taggar) |

### Bär från träd (fas 3: nya träd)

Nya trädsorter, gjorda som de befintliga moddträden (Aspen, Alder ... i `Trees`): stam, löv med frukt som mognar efter
säsong (lövblocket får ett mognadsstadium som hassellöven ger nötter), Fallen Log, Rough Log, Firewood, Bark.

| Träd | Biomer | Frukt |
|---|---|---|
| **Rowan** (rönn) | Deciduous Forest, Birch Forest, Slate Highlands | Rowan Berries (giftiga råa, se ovan), röda klasar på hösten |
| **Bird Cherry** (hägg) | Willow Fen, Reed Delta, flodkanter | vita doftande blommor på våren, svarta bär |
| **Wild Apple** (vildapel) | Hazel Meadow, Deciduous Forest (sällsynt) | Crab Apples, sura; ympas till äpple (avsnitt 5) |
| **Wild Plum / Cherry Plum** | Hazel Meadow, Chalk Downs | plommon, sensommar |
| **Mulberry** | Marble Gorge, savann (sällsynt) | svarta mullbär |
| **Date Palm** | oaser i Desert och Gypsum Flats (sällsynt) | dadlar, torkas |
| **Olive** | Marble Gorge | oliver (bittra råa, läggs in i salt; olja i pressen) |

## 5. Kryddor, örter, grönsaker och frukt

### Vild → tämjd (förädling)

**Beslut (Claude, kan ändras):** samma mönster som Wild Carrots → Carrot (byggt): en vild planta som sås på farmland och
skördas mogen ger sin vilda skörd och en **liten chans (25 %) på den odlade sortens utsäde**. Den odlade ger mer, växer
fortare och tål mer, men måste odlas (ingen vild källa). Bördighet ≥ 2 och rätt klimat höjer chansen (35 %).

| Vild | Var | Tämjd | Ålder | Används |
|---|---|---|---|---|
| Wild Garlic (ramslök) | Deciduous Forest, Willow Fen (vår) | Garlic | sten → brons | krydda, Herb Butter, korv |
| Wild Onion | Plains, Hazel Meadow, Mammoth Steppe | Onion | sten → brons | soppor, stuvning |
| Wild Cabbage | Chalk Downs, Basalt Coast (kalkklippor vid havet) | Cabbage | brons | Sauerkraut (järn, krus) |
| Wild Leek | Hazel Meadow | Leek | brons | soppa |
| Wild Parsnip | Plains, Chalk Downs | Parsnip | sten → brons | rotfrukt |
| Wild Celery (smallage) | Reed Delta | Celery | brons | buljong |
| Wild Turnip / Rapeseed | Plains | Turnip, Rapeseed | brons → järn | rova; olja (pressen, järn) |
| Wild Lettuce | Plains, Meadow | Lettuce | järn | sallad |
| Crab Apple-träd | se avsnitt 4 | Apple (ympning med kniv, järn) | järn | äpple, cider (järn, fat) |

**Örter (vilda, plockas, några odlas i örtagård):** Wild Thyme (Chalk Downs, Limestone Karst), Wild Marjoram/Oregano
(Marble Gorge, Chalk Downs), Juniper (finns), Yarrow (slätter, medicin), Chamomile (slätter, te), Mint (Willow Fen),
Sorrel (ängar), Dill (bybor), Sage och Rosemary (Marble Gorge), Bog Myrtle (Peat Bog, ölkrydda), Meadowsweet (Willow
Fen, mjöd), Hops (Hazel Meadow, klätterväxt, järn: öl), Mustard (Wild Mustard på slätter, frön mals i Quern).

**Salt:** Rock Salt finns (järnåldern); havssalt i saltpannor vid kusten (fas 4).

### Säljs bara av bybor

**Beslut (Claude, kan ändras):** bybornas bönder (nivå 2–4) säljer det som "kom med jordbruket" och inte finns vilt i
världen: **Barley** (har ingen vild källa i dag), **Onion**, **Garlic**, **Cabbage**, **Dill**, **Mustard Seeds**,
**Broad Beans**, och från nivå 4 **Apple Saplings** (ympade). Stenåldern byteshandel, bronsåldern kopparmynt.

### Den östra karavanen ("från fjärran öster")

En annan agent bygger flera sorters handelskaravaner. **Den här karavanen** (förslag: **Eastern Caravan**, en
kryddhandlare med kameler i stället för hästar, sidentäckta vagnar) säljer det som aldrig växer i världen:

| Vara | Pris (förslag) | Ålder | Används |
|---|---|---|---|
| Black Pepper | 3 Copper Coin | brons | krydda i alla rätter (+mättnad) |
| Cinnamon Bark | 4 Copper Coin | brons | Spiced Porridge, mjöd, kakor |
| Ginger Root | 3 Copper Coin | brons | odlingsbar i varma biomer (savann) |
| Saffron Crocus Corms | 2 Silver Coin | järn | odlingsbar (Marble Gorge), dyr färg och krydda |
| Rice | 4 Copper Coin | brons | odlingsbar i Reed Delta och Willow Fen (vattenfylld farmland) |
| Sesame Seeds | 2 Copper Coin | brons | olja, bröd |
| Dried Dates, Dried Figs | 2 Copper Coin | brons | mat som håller länge |
| Tea Leaves | 1 Silver Coin | järn | te (Kettle) |
| Citrus (Bitter Orange) Saplings | 3 Silver Coin | järn | sällsynt fruktträd, bara i varma biomer |
| Silk, Indigo, Lapis | (inte mat) | | för väveriet och färgerna |

Den vill gärna ha **gemstones** (som alla karavaner), Jujubes (fas 1, "smaken hemifrån") och Sea Buckthorn Berries.

## 6. Rätter per ålder och köksredskap

Varje ålder ger **nya redskap**, och redskapen öppnar nya rätter. Kryddor höjer mättnaden (+0,2) eller ger en liten
effekt. Mat som redan finns står kursivt.

| Ålder | Redskap | Rätter |
|---|---|---|
| **Stenålder** | *Campfire, Baking Stone, Clay Kiln, Drying Rack, Smoking Rack*; nytt: **Clay Cooking Pot** (lerkruka i glöden, ingen tripod) | *Porridge, Roasted Mushrooms, Roasted Hazelnuts, torkat och rökt*, **Cooked Elderberries** (fas 1), Berry Mash (bär + nötter), Nettle Soup (Clay Cooking Pot), Herb-Roasted Fish |
| **Bronsålder** | *Bronze Kettle + Tripod, Quern*; nytt: **Bronze Griddle** (häll över elden), **Bronze Ladle**, **Bronze Knife** för örter | *Bread, Stew, Broth*, Flatbread (Griddle), Onion and Garlic Stew, Pea Soup, Spiced Porridge (karavankrydda), Elderberry Syrup (Kettle, håller länge), Sloe Jam, Honey Cakes |
| **Järnålder** | **Iron Skillet**, **Iron Cauldron** (stor gryta, 3x3 Fire Pit), **Spit** (finns: stek över stor Fire Pit), **Wooden Barrel** (lagring/jäsning), **Cheese Press** | Sausages (tarm från styckningen), Sauerkraut (Barrel), Cheese, Cider, Ale (hops), Roast with Herbs, Fried Eggs and Onion, Pies (Kiln) |
| **Stål** | **Steel Knife Set** (fler portioner per styckning), **Steel Pan**, **Mortar and Pestle** (stål/marmor), **Sieve** | Spice Mixes (malda), Pickles i saltlake, Rice dishes (karavanens ris), Sea Buckthorn Juice (pressad, Regeneration I kort), finare bakverk |

**Beslut (Claude, kan ändras):** rätterna görs i stationerna som redan finns (Kettle, Kiln, Fire Pit) där det går; nya
redskap är oftast **verktyg** man håller (Ladle, Skillet, Knife) så att samma eld räcker. En egen kokstation först i
järnåldern (Iron Cauldron).

## 7. Sågverk (vattenkraft)

**Byggt v0.191.0** (se "Fas 5a och 5b: resultat" sist). Sågverket kom i samma fas som **vattenhjulet**, som behövs för både sågverk, kvarn och
(senare) bälgar och hammare. Plan:

### Fas 5a: Water Wheel (järn- eller stålåldern)

- Ett hjul (3×3 eller 5×5 block, egen entity-modell som snurrar) på en axel, i strömmande vatten. Kraften räknas från
  **flödet**: rinnande vatten som passerar hjulets nedre del åt ett håll (undershot) eller faller på det ovanifrån
  (overshot, mer kraft). **Kraft** = antal vattenblock som flyter rätt × fallhöjd; 5×5 overshot ger mest.
- Axel-block för över kraften (raka axlar, kugghjul i 90°). Kraften syns på hjulets fart och i en tooltip ("12 / 16").

### Fas 5b: Sawmill

```
   [ränna][ränna][ränna][SÅG][ränna][ränna][ränna]      <- 7 block lång bädd, sågen i mitten
                          |
                     axel från vattenhjulet
```

- **Multiblock:** 7 block lång bädd. **Mitten** är sågen (Saw Bench med ett ramsågblad av stål som går upp och ner,
  animerat). De tre blocken på var sida är **Log Flume** (en ränna av plankor). Rännan måste ha **vatten som rinner mot
  sågen** på inmatningssidan (vatten sätts i rännans högre ände och rinner nedåt mot mitten); utmatningssidan för bort det
  sågade. Rinner vattnet åt fel håll står stocken still. Sågen drivs av en axel från ett vattenhjul och behöver **minst 8
  kraft**. Byggs som Fire Pit-uppgraderingen: sågen säger vad som saknas ("the flume needs water flowing toward the saw").
- **Inmatning:** man lägger en hel stock (Log, en Rough Log eller ett stycke av en Felled Log) i rännans ände; den flyter
  fram med vattnet (synlig, ~1 block/s) och sågas när den når mitten (~4 s).
- **Utbyte** (jämfört med hand):

| In | Sågverket | För hand i dag |
|---|---|---|
| Log med bark (1 block) | **2 Rough Logs + 2 Firewood + 2 Bark + 2 Sawdust** | yxa: 2 Firewood; såg på Fallen Log: 3 Rough Logs per stock (flera block) |
| Stripped/barkad Log eller Rough Log | **6 Planks + 2 Sticks + 1 Sawdust** | 1 Log → 2 Planks (med yxa) |

- **Sawdust** (nytt): bränsle (kort), strö i djurstallar (gödsel), kompost, Charcoal Briquettes (med lera), packas i
  Root Cellar som isolering, och senare papper (med Kettle) och spånplattor.
- Sågklingan slits (1 per stock, 512 i livslängd), byts mot en ny Steel Saw Blade (smids på städet).
- **Beslut (Claude, kan ändras):** sågverket är **järnålderns sena del eller stålåldern** (sågbladet av stål), samma
  fas som vattenhjulet. Användarens living-world-plan F2 sa "ångåldern"; vattenhjulet kommer före ångan, så sågverket
  flyttas till när vattenhjulet byggs.

## 8. Faser

| Fas | Innehåll | Ålder | Beroende |
|---|---|---|---|
| **1 (byggd)** | 12 blommor (8 små, 2 stora, 2 magiska) i de glesa biomerna; 3 snårbuskar (Blackthorn, Sea Buckthorn, Jujube) i två storlekar som växer ihop och på höjden; klimatfönster i generatorn; Elderberries giftiga råa + Cooked Elderberries | sten | — |
| **2 (byggd)** | De gamla bärbuskarna till klimatfönster; fas 2-blommorna; fler buskar (Rosehip, Crowberry, Barberry, Caper, vinbär); Rowan Berries/Yew Arils | sten | — |
| **3 (byggd)** | Fruktträd (Rowan, Bird Cherry, Wild Apple, Plum, Olive, Date); magiska fas 3-blommor | sten–järn | `Trees` |
| **4 (byggd)** | Vilda grönsaker och örter + förädling; bybornas varor; Clay Cooking Pot; Bronze Griddle/Ladle; rätter brons | sten–brons | Farming |
| **4b (byggd)** | Den östra karavanen (varorna ovan) | brons–järn | handelsagentens karavaner |
| **5 (byggd, köket)** | Järnålderns kök (Skillet, Cauldron, Barrel, ost, korv, surkål); havssalt | järn | — |
| **5a (byggd)** | Water Wheel + axlar | järn | — |
| **5b (byggd)** | Sawmill, Sawdust | stål | 5a |
| 6 | Stålets kök (Knife Set, Mortar, Sieve, kryddblandningar) | stål | — |

---

## Fas 1: resultat

Byggt v0.187.0. Spelarens beskrivning står i `docs/ages/01-stone-age.md` ("Snårbuskar" och "Blommor").

- **Kod:** `Flora` (registrering, klimatfönster för snåren, `dryGround`), `WildFlowerBlock` (blommar efter säsong,
  `Bloom`-regeln, magiska partiklar), `TallWildFlowerBlock`, `ThicketBushBlock`, `SlabPlanting` (bygger på `WildBerryBushBlock`; ung,
  sidor som kopplas, övre block, spridning, säsongsmål, taggar), `worldgen/ThicketFeature` (klumpar på 4–11 buskar,
  unga i kanten, höga i mitten), `Retrogen.FEATURES` (gamla världar får blommorna och snåren), `SeasonalFoliage`
  (frostfärg på vintern). `WildBerryBushBlock.ageAfterPicking` är ny (snåren mognar om efter plockning).
- **Data:** `scripts/gen_flora.py` (efter `gen_writing`, före `gen_winter_plants` i kedjan), texturer
  `scripts/gen_flora_textures.py` (fristående). `gen_winter_plants.py` tonar blommornas modeller.
- **Klimat:** generatorn skriver ut vilka biomer varje växt hamnar i. Uppmätt i en ny värld (seed 4242, 128×128 block):
  Chalk Downs 138 Pasque Flower, 132 Harebell, 82 Lady's Slipper, 42 Great Mullein, 27 Blackthorn (7 toppar);
  Mammoth Steppe 15 Sea Buckthorn, 18 Edelweiss, 38 Pasque Flower, 14 Great Mullein, 10 Desert Candle; savann
  28 Jujube, 49 Desert Marigold, 36 Desert Candle; Shingle Beach 9 Sea Buckthorn, 17 Mountain Avens. Ingen Sea Buckthorn
  på Chalk Downs.
- **Test:** gametest `flora` (`FloraTest`): sidor kopplas, unga kopplas inte, toppen står bara på en fullvuxen och
  faller med den, plockning, säsongsmål, klimat, blomningsregler (Fern Flower bara sommarnätter, Frost Lily bara
  vinter), giftiga råa fläderbär.
- **Halva steg:** `SlabPlanting`: en spelare som planterar en växt på en jordslab (Grass Slab m.fl. från `SmoothGround`)
  gör den först till ett helt block. **Beslut (Claude, kan ändras):** hellre det än att låta växter stå på slabben med
  modellen sänkt ett halvt block (det skulle kräva dubbla modeller för varje växt). I worldgen läggs växterna före
  halvstegen, så där behövs det inte.
- **Svagt / kvar:** snår som sprider sig själva och vilda växter som sås av fåglar sätts inte på slabbar (de kräver
  hela block). Grass Stairs tas inte med. De gamla sju
  bärbuskarna har kvar sina fasta biomlistor (fas 2). Fern Flowers Luck gäller den som bryter blomman, inte den som
  plockar den på annat sätt (t.ex. med vatten eller kolv).

---

## Fas 2 och 3: resultat

Byggt v0.191.0. Spelarens beskrivning står i `docs/ages/01-stone-age.md` ("Fler snårbuskar", "De gamla bärbuskarna följer
klimatet", "Fruktträd", "Fler blommor", magiska blommor).

- **Gamla bärbuskar:** `gen_flora.py` skriver om deras biome modifiers från klimatfönstren i avsnitt 2 (tabellen ovan) och
  skriver ut listorna. `Foraging.RIPE_SEASONS` + `WildBerryBushBlock.ripensIn`: en buske mognar bara i sin säsong och
  tappar mogna bär efteråt (går tillbaka till blommor). **Beslut (Claude, kan ändras):** de sprider sig fortfarande bara
  via fåglar, inte efter klimat.
- **Blommor fas 2:** alla tio (Foxglove och Purple Loosestrife två block). Bluebells och Wood Anemone läggs i stora mattor.
  Desert Rose blev **Spiny Thrift** (en taggig tuva passar Gypsum Flats bättre än en suckulent).
- **Buskar fas 2:** Dog Rose (Rosehips), Crowberry, Bearberry, Barberry, Caper Bush, Red/Black Currant, alla som
  `ThicketBushBlock` med nya flaggor: `thorns` (bara Dog Rose och Barberry, plus fas 1), `low` (Crowberry, Bearberry, Caper:
  ~6 px matta/kulle, växer ihop men aldrig på höjden), `evergreen` (Crowberry, Bearberry: fäller inga löv). Bearberry
  blev buske i stället för blomma. **Inte byggt:** Saskatoon och Prickly Pear (inte med i fas 2-listan), Rosehip Tea
  (kommer med Kettle-rätterna i fas 4), Bittersweet Nightshade.
- **Giftiga råa:** Rowan Berries (`Flora.rawRowanBerries`: Poison 3 s, Nausea; Cooked Rowan Berries på Campfire, torkade
  ofarliga), Yew Arils (från Yew-löv 8 %, Poison II 7 s).
- **Fruktträd fas 3:** Rowan, Bird Cherry, Wild Apple, Plum, Olive, Date Palm som hela träslag (`Trees.WOODS`,
  `Mechanical.WOODS`, alla trä-generatorer). Löven är `FruitLeavesBlock`: `age` 0–3 (blad, blom, omogen, mogen) mot ett
  mål per säsong (`Trees.FRUIT_KINDS`), högerklick plockar 1–2 frukter, mogna löv som bryts ger frukt (loot,
  `gen_trees.py`). De fyra lövfällande sorterna har kala grenar och knoppar på vintern/våren (`SeasonalFoliage`) men
  **ingen höstfärg**: höstmodellen skulle dölja frukten. Dadelpalmen har en egen kronform (`TreeShapes.PalmFoliagePlacer`:
  åtta blad som hänger ut från toppen) och en planta som tar rot i sand. `StumpBlock.LOG` går nu till 25 (en per trä).
  Kronformer: rönnen en upprätt luftig oval (vanillas fancy-stam med grenar), häggen flera lutande stammar med lösa
  hängande kronor (pilens placerare), vildapeln låg och bred med krokiga grenar (körsbärsträdets stam och krona),
  plommonet en liten rund krona, olivträdet en kort kluven stam med små öppna silvriga kronor, dadelpalmen en svagt
  böjd stam (`TreeShapes.CurvedTrunkPlacer`) med en stjärna av tunna hängande blad (bladen är egna modeller: ett plan
  ovanifrån och korsade plan med hängande bladspetsar, var fjärde med blomsträngar eller en dadelklase). Frukt och blom
  sitter i små klasar i 3 texturvarianter per stadium som vrids slumpvis, så kronan inte blir ett rutmönster.
  **Inte byggt:** Mulberry, ympning till Apple, oliver i saltlake och olja (pressen), frukt som faller till marken.
- **Magiska fas 3:** Will-o'-Wisp Lily (Peat Bog, blommar och lyser bara på natten) och Moonwort (Limestone Karst, bara
  fullmånenätter: `WildFlowerBlock.Bloom.fullMoon`). **Beslut (Claude, kan ändras):** "lyser bara i dimma" blev "bara på
  natten", eftersom det inte finns någon dimma i Peat Bog. Användningarna (Fern Flower i Charm, Frost Lily-te, Moonwort
  öppnar lås) är inte byggda.
- **Snårbuskarnas skott:** planen som sticker ut ur buskarna har egna texturer (`<sort>_sprig`, `_sprig_bare`: några
  tydliga bladskott) i stället för utsträckt lövmassa. De låga mattorna och vinbären är målade som mjuka klumpar.
- **Fas 1-putsning:** Jujube-löven målade som mjuka klumpar med ljus och skuggsida; Blackthorns vårblom är nu små
  femkronbladiga blommor i klasar på mörka kvistar.
- **Test:** `FloraTest` utökad (låga och vintergröna buskar, mognadssäsong, fullmåne, fruktplockning, stubbens trästorlek,
  råa rönnbär).

---

## Fas 4, 4b och 5: resultat

Byggt v0.191.0. Spelarens beskrivning: `docs/ages/01-stone-age.md` ("Vilda grönsaker, örter och Clay Cooking Pot"),
`02-bronze-age.md` ("Köket i bronsåldern", östra karavanen), `03-iron-age.md` ("Järnålderns kök"), odlingen i `docs/farming.md`
("Vilda grönsaker och tämjning").

- **Kod:** `Herbs` (grönsaker vilda och odlade, köpta grödor, örter, karavanens varor, klimat, `domesticate`), `HerbBlock`
  (klipps med kniv, växer igen), `Kitchen` (rätter som tabell `DISHES`: redskap, lägsta nivå, ingredienser; redskapen),
  `ClayPotBlock` (krukan i glöden: samma block entity och renderare som kitteln, `TripodBlockEntity` med krukan som kärl),
  `CookwareBlock`/`CookwareBlockEntity`/`client/CookwareRenderer` (häll, stekpanna, tunna, ostpress), `SaltPanBlock`. Kitteln:
  `Kettle.finish` tar kärlet (lera 0, brons 1, järn 2), en känd rätt går före vanlig stuvning, kryddor höjer stuvningen;
  `TripodBlock.IRON` för Iron Cauldron; sleven i `TripodBlockEntity.ladleWith`. Karavaner: `TraderKinds.Kind.weight` +
  `TraderKinds.pick` (den östra har vikt 1).
- **Data:** `scripts/gen_herbs.py` (efter `gen_flora`, före `gen_winter_plants` i kedjan; den kör `scripts/herbs_cookware.py`
  som skriver redskapens modeller och lappar kittelns tripod-blockstate), en rad i `gen_traders.py`, texturer:
  `scripts/gen_herbs_textures.py` (växter, grödor, råvaror) och `scripts/gen_cookware_textures.py` (redskap, rätter),
  karavanens folk och kapell: `gen_tribe_skins.py` (`eastern`) och `gen_cart_canopy_textures.py`.
- **Beslut (Claude, kan ändras):** rätterna är fasta ingredienslistor per redskap (dubbla ingredienser ger dubbelt), så att
  maten följer åldrarna: krukan kan bara sina rätter, kitteln och grytan kan också de lägre. Bladgrönsaker sås med själva
  grönsaken (som vanillas potatis), inga egna frön. Iron Cauldron hänger i tripoden (bara över Fire Pit) i stället för en egen
  station: samma vätska, värme och öslogik. Wooden Barrel heter **Fermenting Barrel** (vanillas Barrel finns).
- **Kvar / inte gjort:** byns Apple Saplings blev **Wild Apple Sapling** (fas 3:s vildapel; bonden nivå 4, 1 för 6 Copper
  Coin, v0.192.0; ympade äpplen finns inte än). Bitter Orange Saplings (karavanen) saknas fortfarande: inget citrusträd. Rapeseed,
  Cider, Ale, Pies och Roast with Herbs är inte gjorda (pressen, ölet och ugnspajer kommer senare); örterna Hops och Bog Myrtle
  har ännu bara kompost och framtida öl. Rice växer på vanlig farmland (inte vattendränkt). Crop Stakes-skörd tämjer inte.
- **Test:** `HerbsTest`: tämjning (~25 % av 400 skördar), rätter per redskap och nivå, kitteln ger Pea Soup, krukan lämnar
  tillbaka det som inte är en rätt, hällen bakar bara över tänd eld, tunnan jäser surkål, örten växer igen.

---

## Fas 5a och 5b: resultat

Byggt v0.191.0. Spelarens beskrivning står i `docs/ages/03-iron-age.md` ("Vattenkraft och sågverk").

- **Vattenhjulet:** 5×5, bara navet är ett block. Kraften räknas per skovelruta (20 rutor): rinnande vatten runt hjulet 1,
  fallande 2, med riktning (vatten åt samma håll överst och nederst tar ut varandra), högst 16. **Beslut (Claude, kan
  ändras):** planens "fallhöjd" blev "fallande vatten räknas dubbelt"; undershot når högst 7, så sågverket (8) behöver
  vatten som faller på hjulet. Rörelsen syns som fart (0,25°/tick per kraft) och i högerklick ("12 / 16"), inte i en
  svävande tooltip.
- **Axlar:** Wooden Shaft (rak) och Gearbox (rakt fram och runt hörnet, åt alla vågräta håll). Inga lodräta axlar.
  Alla delar på en linje visar hjulets egen fas (ingen synk per tick), och stannar 2,5 s efter att hjulet slutat mata dem.
  Två hjul på samma linje adderas inte (det senaste vinner).
- **Sågverket:** som planen: 7 block, sågen i mitten, 3 Log Flumes på var sida, vatten mot sågen, minst 8 kraft, Steel
  Saw Blade 512 stockar, utbytet enligt tabellen. Rännorna har inget riktigt vatten (vattenloggade block kan inte rinna),
  de räknar själva varifrån vattnet kommer och visar rinnande eller stilla vatten. Det sågade flyter ut som föremål.
- **Sawdust:** bränsle, kompost, Sawdust Briquette → Charcoal i Clay Kiln. Root Cellar-isolering, strö, papper och
  spånplattor är inte gjorda.
- **Generator:** `gen_waterpower.py` efter `gen_flora.py`, före `gen_winter_plants.py`; texturer `gen_waterpower_textures.py`
  (fristående).
