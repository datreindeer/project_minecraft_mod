# Handelskaravaner och myntpriser: plan

Två saker hör ihop: **all handel med bybor går i mynt**, och **handelskaravanerna** (som ersätter vanillas Wandering
Trader, [Caravans](../src/main/java/com/mechanical/people/Caravans.java)) blir olika **sorter** med egna varor, eget folk,
egna vagnar och en egen handelsruta. Bakgrund: [seasons-and-tribes.md](seasons-and-tribes.md),
[ages/01-stone-age.md](ages/01-stone-age.md) (*Handelskaravaner*), myntet i `Coins.java`.

> Status: **byggt** (2026-11-23, v0.187.0). Avvikelser står under *Kvar / inte gjort* längst ned.

## A. Bybornas handel: bara mynt
Tidigare var bybornas nivå 1–2 (stenåldern) ren byteshandel och nivå 3–4 kopparmynt. Nu är **varje byte mynt mot vara**
(bybon köper något för mynt, eller säljer något för mynt). Inga smaragder, ingen byteshandel.
- **Copper Coin** för billiga saker: mat, råvaror, flintverktyg, pilar (1–9 koppar).
- **Silver Coin** (= 10 koppar) för lite dyrare: bronsverktyg och -vapen, rustning, sängar, namnskyltar (1–6 silver).
- **Gold Coin** (= 10 silver) för värdefulla saker: nivå 5 (Master), järnålderns verktyg och sällsynta varor (1–2 guld).
- Stenåldersbyn köper ändå råvaror för koppar (hudar, bär, flinta, ben), så en spelare utan myntpräglingsverktyg kan
  tjäna sina första mynt hos byn. Mynten präglas annars på Bronze Anvil (`Coins.java`).
- Data: `scripts/gen_data.py` (nivå 1–2), `scripts/gen_bronze.py` (nivå 3–5), `scripts/gen_thread.py` (linfrö).

## B. Karavansorter (datadrivna)
Varje sort är en JSON-fil `data/mechanical/trader_kind/<sort>.json` (skrivs av `scripts/gen_traders.py` från en tabell),
läses in av [TraderKinds](../src/main/java/com/mechanical/people/TraderKinds.java) vid varje datapack-laddning. En ny sort
eller nya varor = en rad i tabellen (eller en egen JSON i ett datapack) + skins i `scripts/gen_tribe_skins.py`
(`KIND_LOOKS`). Den kommande östliga kryddhandlaren läggs till så.

Fält:
- `animal`: `horse` eller `camel` (dragdjuren; ökenhandlare har kameler).
- `cargo`: item-id:n som syns på flaket (block ritas som små block, andra saker ligger platt).
- `purse`: `[min, max]` koppar i kassan för en liten karavan (en stor har 2,5 gånger mer).
- `sells`: `{item, count, price, stock, age, rare}`: vad de säljer, pris i **koppar** (visas som guld/silver/koppar),
  hur många gånger (lagret), lägsta ålder (0 sten, 1 brons, 2 järn; varor upp till **en ålder över** stammens visas) och
  om det är en **sällsynt** vara (bara i stora karavaner).
- `buys`: samma form; vad de köper av dig. `item` kan vara en tagg (`#mechanical:gemstones`): då blir varje sten i taggen
  ett eget köp.

Sorterna:
| Sort | Visas som | Djur | Säljer (urval) | Köper |
| --- | --- | --- | --- | --- |
| `woodsman` | Timber & Tools | häst | plantor, yxor, sågar, mejsel, kåda, bark | rep, kåda, lim |
| `mining` | Mining Equipment | häst | hackor, facklor, kol, malakit, kassiterit, tenn, järnmalm | råkoppar, råjärn, blyglans |
| `jewelry` | Jewelry & Gemstones | kamel | guldringar, -kedjor, -halsband, slipade stenar | alla ädelstenar, diamant, smaragd, ametist |
| `hunting` | Hunting Gear & Furs | häst | bågar, pilar, spjut, snaror, skinn (+ ofta ett storvilt på vagnen) | råa skinn, horn, klor, senor |
| `herbs` | Herbs & Remedies | häst | salvor, omslag, enbär, honung, torkad svamp, frö | murklor, ostronskivling, bävergäll, fläder |
| `foraging` | Forager's Goods | häst | barkkorgar, fröpåsar, nötter, torkade bär, rep | bär och nötter |

## C. Folket
En karavan är **4–5 personer** av en handelsstam (temperament *Traders*), alla klädda för sin sort:
- **Ledaren** (`TRADER`): gammal, går **långsamt och framåtböjd** med Walking Staff i ena handen och en **Coin Purse**
  (myntpung) i den andra. Strövar runt djurhållaren inom **6 block**, aldrig längre bort. Dödas han tappar han det som är
  kvar i kassan (som mynt).
- **Djurhållaren** (`HANDLER`, ny roll): håller alla dragdjurens rep och **står still** när karavanen är framme;
  djuren **står kvar och betar** (huvudet ned då och då), vagnarna står.
- **2–3 vakter** (`GUARD`): **metallrustning** (brons- eller järnfjäll / lameller målade på skinnet med axelskydd),
  ingen läderrustning, ingen hjälm. Går **långsamt i en större ring (8–11 block)** runt ledaren, stannar och spanar utåt.
- **Försvar:** slår du någon i gruppen, ett dragdjur eller en vagn blir hela gruppen arg (som när en stamperson slås) och
  vakterna går till anfall. Vagnar och djur kan inte tas eller öppnas.
- **Skins:** egna per sort och roll (4 per roll: män 0/2, kvinnor 1/3) i `textures/entity/tribesperson/caravan/<sort>/`,
  stammens färg i masken som förut. Gruvfolk sotiga i läder och järnlameller, skogsfolk i grönt och brunt med sågspån,
  juvelerare i djupt blått/purpur med guld och stenar, jägare med päls och tänder, örtfolk i ljust linne med
  örtknippen, samlare i halm och vävt med korgband.

## Storlek
- **Liten** (vanligast, 3 av 4): en **Wheeled Cart** med kapell och **ett** djur. Vanliga varor.
- **Stor** (1 av 4): Iron Age **Wagon** med **två** djur i spann. Har även de **sällsynta varorna**, men allt kostar
  **25 % mer**; kassan och lagret är större.
- Lasten på flaket visar vad de säljer (`cargo`). Jägarkaravanens kärra bär ibland ett storvilt i stället.

## Ankomst och avfärd
- Vanillas tidtabell för Wandering Trader är kvar. Finns en spelares läger (`CampAge`) inom 80 block ställer sig
  karavanen **vid lägrets kant** (16 block från elden, på den sida de kom ifrån), annars där handlaren skulle ha kommit.
- De stannar 2 speldagar och försvinner sedan med djur och vagnar (som förut).

## D. Handelsrutan
Högerklick på ledaren: pratrutan (som förut) med **Trade**, som öppnar karavanens egen ruta (`CaravanTrade`,
`client/people/CaravanTradeScreen`):
- Överst: ledarens namn, stammen, och vad de handlar med (t.ex. *Mining Equipment*, *stor karavan*).
- **Kassan**: hur mycket mynt de har kvar, som guld/silver/koppar.
- Två kolumner: **Till salu** (vara, antal kvar, pris) och **De köper** (vara, hur många till, pris, hur många du har).
- Nederst: dina mynt. Klick köper/säljer en gång; det som inte går (för dyrt, slut, kassan tom, du har inte varan) är
  gråat. Du betalar med vilka mynt som helst och får växel tillbaka (10 koppar = 1 silver, 10 silver = 1 guld).
- **Begränsat:** varje vara har ett lager som tar slut (fylls inte på), och de köper bara så länge kassan räcker. Det du
  köper av dem fyller kassan.

## Avancemang (Wilderness)
- *The Caravan Has Come* (finns): prata med en karavans ledare.
- *Treasures for Coin* (finns): sälj en ädelsten till en karavan.
- *Every Road Leads Here*: träffa alla sorters karavaner (ett kriterium per sort).
- *The Rich Caravan*: träffa en stor karavan med vagn.
- *Bought Out*: köp slut på en vara hos en karavan.

## Tester
`people/CaravanTest.java`: varje sort (folk, roller, djur, rustning), djurhållaren och djuren står still, vakterna slår
tillbaka när ett djur skadas, kassan stoppar försäljning, lagret tar slut. Den gamla testen i `TribeTest` (caravan) gäller
fortfarande (vanillas handlare ersätts, storvilt på vagnen).

## Kvar / inte gjort
- Karavanen **dyker upp** på platsen (vid lägrets kant); den vandrar inte in från fjärran. Avfärden är också att den
  försvinner.
- Vagnen (Wagon) har inget kapell.
- Rabatt efter rykte (som stammarnas byteshandel) gäller inte i den nya rutan; handel höjer ryktet som förut.
