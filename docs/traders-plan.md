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
- **Ledaren** (`TRADER`): gammal, framåtböjd, med Walking Staff i ena handen och en **Coin Purse** (myntpung, stor och
  synlig) hållen framför sig i den andra. Egen gång (`TribeIdlePoses.stoop`): korta, långsamma steg, kroppen sjunker lite
  i varje steg, staven sätts i takt med motsatt fot, pungen svänger nästan inte. Strövar runt djurhållaren inom **6 block**, aldrig längre bort. Dödas han tappar han det som är
  kvar i kassan (som mynt).
- **Djurhållaren** (`HANDLER`, ny roll): håller alla dragdjurens rep och **står still** när karavanen är framme;
  djuren **står kvar och betar** (huvudet ned då och då), vagnarna står.
- **2–3 vakter** (`GUARD`): **metallrustning** (brons- eller järnfjäll / lameller målade på skinnet med axelskydd),
  ingen läderrustning, ingen hjälm. Går **långsamt i en större ring (8–11 block)** runt ledaren, stannar och spanar utåt.
- **Försvar:** slår du någon i gruppen, ett dragdjur eller en vagn blir hela gruppen arg (som när en stamperson slås) och
  vakterna går till anfall. Vagnar och djur kan inte tas eller öppnas.
- **Skins:** egna per sort och roll (4 per roll: män 0/2, kvinnor 1/3) i `textures/entity/tribesperson/caravan/<sort>/`.
  **Ingen stamfärg** (inget gemensamt pannband): varje sort har egna färger och en egen siluett som syns på 15 block,
  med veck och skuggor i tyget och olika ansikten (fräknar, ärr, kraftiga eller tunna ögonbryn):
  - **timmer:** rutigt ylle (rött eller grönt), läderförkläde ned över låren, en yxa över ryggen;
  - **gruva:** sotigt läderförkläde, vadderad mössa, en lykta vid bältet;
  - **smycken:** fina färgade rockar (blå, purpur, karmosin) med guldkant och ädelstenar, turban;
  - **jakt:** pälskantad huva och pälsmantel med cape, koger på ryggen;
  - **örter:** grön eller ockra rock, en väska med örter som sticker upp, torkade knippen i bältet;
  - **samlande:** jordfärgat lapptäcke av tyg, en flätad korg full av bär och nötter på ryggen.

## Storlek
- **Liten** (vanligast, 3 av 4): en **Wheeled Cart** och **ett** djur. Vanliga varor.
- **Stor** (1 av 4): Iron Age **Wagon** med **två** djur i spann. Har även de **sällsynta varorna**, men allt kostar
  **25 % mer**; kassan och lagret är större.
- Både kärra och vagn har ett **välvt kapell** av kanvas på bågar (`client/TiltModel`), randigt i sortens färger
  (`scripts/gen_cart_canopy_textures.py`), öppet fram och bak.
- Lasten på flaket visar vad de säljer (`cargo`), packad i rader: block står i rutor och staplas, annat ligger platt
  ovanpå, så det syns genom kapellets öppna ände. Jägarkaravanens kärra bär ibland ett storvilt i stället.

## Ankomst och avfärd
- Vanillas tidtabell för Wandering Trader är kvar. Målet är **lägrets kant** (16 block från elden) om en spelares läger
  (`CampAge`) finns inom 80 block, annars där handlaren skulle ha kommit.
- Karavanen **startar 32 block bort** (helst på en stig eller väg: trampad stig, dirt path, grus, packad lera) och **går in
  som en kolonn**: djurhållaren först med djuren (han väntar om de hamnar efter), kärran eller vagnen efter dem, ledaren
  bakom flaket och vakterna på flankerna och sist. Framme stannar de och gör som ovan. Når de inte fram på 2 minuter
  stannar de där de är.
- På vägen **håller kolonnen sig till stigar och vägar** (`people/CaravanNavigation`): varje steg utanför stiggrund
  (trampad stig, sliten gräsmatta, dirt path, grus, packad lera, grovjord, och filerna på vägar som en stam byggt) kostar
  mer i vägsökningen, så djurhållaren följer en stig som går åt rätt håll, också runt en krök. Över öppen mark varierar
  kostnaden i mjuka fläckar, så de vandrar lite längs den lättare marken i stället för att gå i en linjalrak linje.
- Efter 2 speldagar **går de därifrån** samma väg, 48 block bort från närmaste spelare, och försvinner när ingen spelare
  är inom 40 block (eller när de är framme).

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
- Flankvakterna och ledaren går på sina platser bredvid och bakom djurhållaren, inte på stigen själv.
- Rabatt efter rykte (som stammarnas byteshandel) gäller inte i den nya rutan; handel höjer ryktet som förut.
