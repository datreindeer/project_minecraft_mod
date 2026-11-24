# Stålåldern

> Status: **byggs** (2026-11-24). Byggt: **stålverktyg och stålrustning**, **stor Fire Pit, väskor och smycken**, **sågverket**,
> **stålets kök**. Nästa stora mål: **High Furnace** (planen i [steel-furnace-plan.md](../steel-furnace-plan.md)), fas för fas.

Fjärde åldern (användarens beslut 2026-11-24: stålet blir en egen ålder). Järnåldern lämnar över: **Steel Ingot** (Blister Steel
från Cementation Pot, vikt med 6 slag av **Iron Smithing Hammer**, se *4. Steel* i [03-iron-age.md](03-iron-age.md)),
**Smithing Hearth**, **Iron Bellows**, och vattenkraften (**Water Wheel**, **Wooden Shaft**, **Gearbox**). Heta Steel Ingots
smids på städet efter samma mönster som järnet (tabellen under *Smide av järn* i 03): **Steel Plate** och stålets huvuden
och blad (ingen Tool Rod av stål).

Advancements: egen flik, **Steel Age** (rot: *Steel Age*, en Steel Ingot). Stegen som fanns innan fliken (Steelsmith, Steel
Clad, Goldsmith, Up and Down, Ground Fine m.fl.) har kvar sina id:n (`mechanical:iron_age/...`), så den som redan tagit dem
behåller dem; de hänger nu under stålålderns rot.

Åldern slutar med ångåldern (Zinc och mässing, Coked Coal i stor skala, maskiner av stål).

## 1. Stålverktyg och stålrustning (byggd)

Kod: `Smithing`. Data: `scripts/gen_iron_age.py`. Test: `IronAgeTest`.

- **Stålverktyg** (Tool Station): smitt huvud med skafthål på ett träskaft, låst med en **Iron Tool Rod** som kil och
  beslag, ingen surrning: huvud + Iron Tool Rod + Tool Rod → Steel Pickaxe / Axe / Shovel / Hoe; Steel Spear (Spear Head
  + Iron Tool Rod + Tool Rod); Steel Sword (Sword Blade + Iron Tool Rod som parerstång och tånge + Leather som grepp);
  Steel Knife (Knife Blade + Leather + Stick). Materialet: **2200** i hållbarhet, fart 7,5, +2,5 skada (järn 6 / +2),
  bryter som diamant (obsidian). Texturerna är vanillas järnverktyg med blått stål.
- **Flint and Steel:** Flint + Steel Plate (vanillas recept ville ha Iron Ingot). Tänder varje gång.
- **Steel Quarry Hammer** (Tool Station): Steel Hammer Head + Steel Plate + Iron Tool Rod. Bryter 3×3 som Bronze Quarry
  Hammer, men varje malm en stålhacka bryter smulas (guld, diamant m.m.); 1800 i hållbarhet.
- **Stålrustning** (Armor Station): 3 / 6 / 5 / 2 Steel Plate + 2 / 4 / 3 / 2 Leather + hammare → Steel Helmet /
  Chestplate / Leggings / Boots. Skydd 3 / 7 / 6 / 2 och 1 i seghet (järn 2 / 6 / 5 / 2, diamant 3 / 8 / 6 / 3),
  hållbarhet ×25 (järn 15, diamant 33).
- **Smidda stålversioner** (Tool Station, se tabellerna i 03): **Steel Saw** (Steel Saw Blade + Leather + Stick, 1600, ett
  drag 6 ticks), **Steel Sickle** (Steel Sickle Blade + Leather + Stick, 1600, skär **5×5** och ger alltid en extra kärve),
  **Steel Chisel** (Steel Chisel Blade + Leather + Stick, 1400, 3 slipdrag per drag på Whetstone), **Steel Smithing Hammer**
  (Steel Hammer Head + Iron Tool Rod + Tool Rod, 1200, nöts 1 per slag och slår var 7:e tick).
- Advancements (Steel Age-fliken): **Steelsmith**, **Strike a Light** → **Red Spark**, **Mountain Breaker**, **Steel Clad**,
  **Reaper**, **Steel Hammer**. (Steel Saw och Steel Chisel räknas i järnålderns *Sharper Teeth* och *Fine Ironwork*.)

## 2. Efter stålet: stor Fire Pit, väskor och smycken (byggd, living-world-plan F12, F9, F10)

Allt tre låses upp med Steel (recepten med Steel Plate). Kod: `FirePit`, `FirePitBlock#upgrade`, `FirePitBlockEntity`
(steken), `Satchels`, `LeatherSatchelItem`, `equipment.PackMenu`, `Jewelry`, `JewelryTableBlock(Entity)`.
Data: `scripts/gen_firepit.py`, `scripts/gen_kettle.py`, `scripts/gen_equipment.py`, `scripts/gen_p8smith.py`; texturer
`scripts/gen_p8_textures.py`, `scripts/gen_equipment_textures.py`. Test: `P8SmithTest` (p8_big_fire_pit, p8_roast,
p8_satchel, p8_jewelry).

**Stor Fire Pit (3×3)**

| Vad | Hur |
|---|---|
| **Steel Hearth Ring** | 4 Steel Plate i hörnen, 4 rock chunks, en Firewood i mitten (`PRP`/`RFR`/`PRP`) |
| **Uppgradering** | Högerklicka med ringen på en Fire Pit. Av de fyra 3×3 som rymmer gropen väljs den första vars fem nya block är lediga (ersättbara, inget vatten, fast mark under), helst bort från spelaren. Är alla blockerade säger den vad som står i vägen och var (block, eller luften under). Maten, veden, elden och en stek flyttar med; stammens centrum följer med (`Clans.pitMoved`); en Tripod som inte längre står rätt faller |
| **Elden** | Brinner som med galler (takt 1) **med eller utan galler**, varje vedträ räcker 1,5× så länge, rymmer **32** Firewood, glöden räcker 6 min. Ljus, värme och avstånd för monster 2× en Campfires (liten grop 1,5×). Rökpelaren är ungefär dubbelt så tät och bred och stiger fortare. Vedhög intill matar den upp till 3 block bort |
| **Modeller** | Hela gropen, gallret, Tripod och Bronze Kettle har större modeller (1,5×). Tripoden ställs bara över mittblocket |
| **Hel stek** | Bär (haul) en **flådd** kropp till en stor grop och högerklicka på den. Den hänger över elden och steks på **2 min** brinnande eld till **2 × djurets mesta kött**, stekt (ett ruttet kadaver blir Rotten Flesh). Tom hand tar steken. Rivs gropen under tiden släpps köttet rått |
| **Stamfördelar** | En stams stora Fire Pit forceladdar **5×5** chunks (`Clan.loadRadius`), **två band** kan besöka lägret samtidigt (`TribeVisits.visitCap`, aldrig samma stam två gånger), och `Clan.isBigPit()` finns för senare stamfunktioner |
| **Rivs** | Fire Pit + Steel Hearth Ring (+ galler) tillbaka |

**Väskor**

| Vad | Hur |
|---|---|
| **Tool Loops** | Leatherworking Bench: 2 Leather + 1 Steel Plate + nål. Släpp den på en Leather Satchel i inventariet: **3 platser som bara tar verktyg** (taggen `mechanical:satchel_tools`: knivar, sågar, tänger, nålar, mejslar, skäror, smides- och stenhammare, Fire Drill, Shears, Flint and Steel). Shift-klick lägger verktyg där först |
| **Steel-Frame Pack** | Leatherworking Bench: 10 Leather + 3 Steel Plate + nål. **5×5** med 3 verktygsöglor inbyggda, bärs i Back-platsen (B). Skärmen blir två rader högre |
| **Häst** | Bara en vanlig Leather Satchel utan öglor kan hängas som sadelväska (de nio sadelplatserna skulle tappa resten) |

**Guld och smycken**

| Vad | Hur |
|---|---|
| **Smälta** | Som förut: Raw Gold eller 9 Gold Nugget i Clay Kiln vid 1100° → Gold Ingot |
| **Steel Casting Table** | 3 Steel Plate över Fire Brick och en Cementation Pot (`PPP`/`BCB`/`B B`). Gjut-multiblocket: bordet **intill en Clay Kiln** (någon halva) på **minst 1100°**. Lägg Gold Ingots på bordet (högst 8). En i taget smälter av ugnens hetta och stelnar på 10 s: **2 Gold Ring Blank** eller **1 Gold Chain**. Tom hand tar det gjutna, annars byter den form; smyg + tom hand tar tillbaka tackorna. Utan het ugn: inget händer (meddelande när man lägger på guld) |
| **Jeweler's Bench** | Steel Plate + nål över plankor (`SC `/`WWW`/`W W`). Lägg ett ämne och en **slipad eller Flawless** gemsten på bänken (inte Rough eller Flawed), högerklicka med en mejsel (Bronze/Iron/Steel Chisel, slits 1) |
| **Gold Ring** | Ring Blank + sten. Bärs i de två ringplatserna |
| **Gold Necklace** | Gold Chain + sten. Bärs i halsplatsen |

Bonusar per sten (Claude bestämde 2026-11-22, nattkörningen; kan ändras i `Jewelry.BONUSES`). Vanlig sten = liten,
Flawless = ungefär fyra gånger så mycket, glänser och har aqua namn. Två ringar och ett halsband staplas. "Värme" är
kylskydd som en rustningsdel (Temperature#insulation, läder 15 %).

| Sten | Bonus | Vanlig | Flawless |
|---|---|---|---|
| Diamond | Armor Toughness | +0,5 | +2 |
| Emerald | Luck | +0,5 | +2 |
| Amethyst | Bryter block fortare | +5 % | +20 % |
| Garnet | Värme | 5 % | 15 % |
| Ruby | Max Health | +1 (½ hjärta) | +4 (2 hjärtan) |
| Sapphire | Armor | +0,5 | +2 |
| Topaz | Mining Efficiency | +1 | +4 |
| Jade | Knockback Resistance | +2 % | +10 % |
| Opal | Räckvidd mot entiteter | +0,25 | +1 block |
| Turquoise | Säker fallhöjd | +0,5 | +2 block |
| Peridot | Andas under vatten (Oxygen Bonus) | +0,5 | +2 |
| Amber | Värme | 5 % | 15 % |
| Clear Quartz | Räckvidd mot block | +0,25 | +1 block |
| Rose Quartz | Smygfart | +0,05 | +0,2 |
| Citrine | Värme | 5 % | 15 % |
| Smoky Quartz | Brinner kortare | −10 % | −40 % |
| Aquamarine | Fart i vatten | +0,1 | +0,4 |
| Fluorite | Bryter fortare under vatten | +0,2 | +0,8 |
| Celestine | Hopp | +0,02 | +0,08 |
| Moonstone | Stegar upp | +0,1 | +0,5 block |
| Tourmaline | Attack Speed | +0,1 | +0,4 |
| Agate | Tål tryckvåg från explosioner | +5 % | +25 % |
| Jasper | Attack Damage | +0,25 | +1 |
| Carnelian | Gångfart | +2 % | +10 % |
| Chalcedony | Går bättre i lera och snö (Movement Efficiency) | +0,05 | +0,25 |
| Bloodstone | Max Health | +0,5 | +2 |

- Advancements (Steel Age-fliken): **Hearth of the Tribe** → **Whole Roast**; **A Place for Every Tool** →
  **Pack Mule**; **Goldsmith** → **Jeweler** → **Without Flaw**.
- Inte gjort än: fler guldföremål att gjuta (bara ringämne och kedja), egen modell för packen på kroppen (den visas som satcheln),
  egen modell för den hängande steken (tre stora köttbitar som vänds på ett spett).

## 3. Jaspis som tändsten (byggt, fas 9A av världsmaterialplanen)

Jaspis slår gnistor mot stål som flinta: **Rough Jasper eller Polished Jasper + Steel Plate** (formlöst) ger en
Flint and Steel, precis som Flint + Steel Plate. Advancement *Red Spark* under *Strike a Light*.

## 4. Sågverket (byggt, v0.191.0, flora-and-food-plan fas 5b)

Kod: `Sawmill`, `LogFlumeBlock`, `SawBenchBlock(Entity)`, klient `SawBenchRenderer`. Data: `scripts/gen_waterpower.py`.
Test: `WaterPowerTest` (sawmill_barked, sawmill_stripped, sawmill_no_power, sawmill_wrong_flow).

| Vad | Hur |
|---|---|
| **Bygge** | En **Saw Bench** (2 Steel Plate, 2 plankor, Wooden Shaft, 3 stockar) mellan **3 Log Flumes** (5 plankor → 4) i linje på varje sida. Bänkens insida är den som vette mot spelaren när den sattes |
| **Vatten** | Rännorna har inget riktigt vatten. Vatten rinner från den ände som har ett vattenblock intill (eller en ränna i linje som rinner in) mot den andra, och står stilla om båda ändar har vatten. Det måste rinna **mot sågen** (in vid insidans ände) |
| **Kraft** | En axel från ett vattenhjul till bänkens vänstra eller högra sida, minst **8 kraft**. Svänghjulet (ett ljust ekerhjul med järnring) sitter på den sidan och snurrar 9× hjulets fart, vevstaken driver sågramen upp och ner |
| **Blad** | Ett **Steel Saw Blade** (smids på städet som förut) räcker 512 stockar. Ett nytt blad byter ut det gamla, som kommer tillbaka med sina återstående stockar på sig |
| **Stockar** | Högerklicka rännan på insidan (eller bänken) med en stock: högst 3, ett block isär. Den flyter fram (1 block/s) och matas genom bladet på 4 s; man ser stocken gå in och brädorna komma ut (fyra tunna brädor på högkant som glider isär). Stockar och Rough Logs av moddens 19 träslag |
| **Utbyte** | Med bark: **2 Rough Logs + 2 Firewood + 2 Bark + 2 Sawdust**. Barkad stock eller Rough Log: **6 plankor + 2 Sticks + 1 Sawdust**. Det sågade flyter bort i rännan på utsidan och faller av vid änden |
| **Fel** | Vatten åt fel håll: stocken står still. Ingen kraft eller inget blad: stocken väntar vid bladet. Tom hand på bänken säger vad som saknas (rännor, vatten, riktning, blad, kraft) |
| **Sawdust** | Kort ugnsbränsle (50 ticks), komposterbart. 4 Sawdust + Clay Ball → 2 **Sawdust Briquette**, som en Clay Kiln bränner till Charcoal |

- Kraften kommer från ett vattenhjul (se *Vattenkraft och sågverk* i [03-iron-age.md](03-iron-age.md)); hjulet, axlarna och
  Gearbox hör till järnåldern. Planen: [flora-and-food-plan.md](../flora-and-food-plan.md) avsnitt 7.
- Advancement (Steel Age-fliken): **Up and Down** (första stocken sågad).
- Inte gjort än: sågspån i Root Cellar, strö i stallar, papper och spånplattor; stycken av en Felled Log i rännan.
- Maskiner på samma axel **delar kraften** (v0.200.0, beslut 2026-11-24): sågen och en Great Bellows på samma hjul får
  hälften var, och sågen behöver 8. Ge sågen en egen axel eller fler hjul.

## 5. Stålets kök (flora och mat, fas 6, byggt v0.193.0)

Stålet ger köket finare redskap, och den **östra karavanens kryddor** (Black Pepper, Cinnamon Bark, Ginger Root, Saffron) mals till
blandningar som stålrätterna vill ha.

| Redskap | Recept | Används |
|---|---|---|
| **Steel Pan** | `[I][P][I]` / `[I][ ][I]` (P Steel Plate, I Steel Ingot) | Bred grund stålpanna med ringhandtag på en trefot, över en Campfire: **Saffron Rice** (2 Rice + Golden Spice + lök → 2 à 10), **Peppered Steak** (2 rått kött + Kitchen Pepper → 2 à 10), och allt stekpannan och hällen kan |
| **Mortar and Pestle** | `[ ][ ][S]` / `[M][ ][M]` / `[ ][M][ ]` (M Marble, S Steel Ingot) | Marmormortel med stålstöt. Lägg i, klicka med tom hand: varje klick vrider stöten ett kvarts varv, 8 klick → blandningen |
| **Sieve** | `[S][P][S]` / `[W][ ][W]` / `[W][W][W]` (S pinne, P Steel Plate, W plankor) | Böjd askring med vävt ståltrådsnät på ett ämbar. Tom hand skakar, 6 skak: Flour → **Fine Flour**; 3 Sea Buckthorn Berries + honung → 2 **Sea Buckthorn Juice** (Regeneration I 5 s) |
| **Steel Knife Set** | Steel Knife + 2 Steel Knife Blade + Leather + Stick | Knivrulle: styckar 2× (stålkniven 1,6×) och snabbare, klipper örter som en kniv |

**Kryddblandningar (morteln):** **Kitchen Pepper** (pepper + ingefära + kanel → 3), **Sweet Spice** (2 kanel + ingefära → 3),
**Golden Spice** (saffran + peppar + senapsfrön → 3), **Pickling Spice** (senapsfrön + dill + peppar → 3), **Herb Salt** (salt + 2
köksörter → 2, den enda utan karavanen). Alla räknas som kryddor i stuvning.

**Rätter:** **Spiced Pilaf** i Iron Cauldron (2 Rice + Kitchen Pepper + rått kött + lök → 5 skålar à 11), **Pickled Vegetables** i
Fermenting Barrel (3 grönsaker + Pickling Spice + Salt → 5, håller 30 dagar), **Gingerbread** (2 Fine Flour + honung + Sweet Spice +
ägg → 4 Raw Gingerbread, bakas i Clay Kiln). Advancements (Steel Age-fliken): *Ground Fine*, *Sifted*, *Spice Road*, *Pilaf*,
*Gingerbread*, *In Brine*, *A Cut Above*.


## 6. High Furnace (stålålderns stora mål)

Plan och beslut: [steel-furnace-plan.md](../steel-furnace-plan.md). Byggs fas för fas; varje fas förs in här.

- Ett stort multiblock av stålålderns block: väggar av **Banded Furnace Casing** (Silica Brick i stålband av **Steel Hoop**),
  golv av **Silica Bricks**, en **High Furnace Front** med titthucka, **Firebox**-eldstäder och ett **Taphole**. Inre 2–3 ×
  2–3 × 5–8 block, 400 mB smälta per inre block.
- Värmen kommer av Charcoal i eldstäderna och **Great Bellows** som drivs av vattenhjul: upp mot **1600°** (1800° med Coke
  och Hot Blast Stove i sista fasen). Maskiner på samma axel delar kraften, så större ugnar behöver fler hjul.
- Smälter det mesta som bronsugnen gör, i lager efter densitet, med **2× utbyte på malm**, legerar brons (9:1) och stål (1:2)
  av sig själv, och gör järnmalm till **Pig Iron** och slagg. Tackjärn färskas till smidesjärn i en **Finery Hearth**.
- Metallen tappas genom en **Pouring Spout** i **Refractory Troughs** till en ny gjutnivå: **Pig Bed**, **Molding Flask** med
  Wooden Patterns, **Foundry Table** och **Foundry Basin**, järnformar för tackor och plåt. Verktyg av järn och stål smids
  fortfarande på städet.
- Senare faser: glas (Glass Batch vid 1400°), Bell och Cauldron i Loam Mold, Steel Block och Anvil, och valfritt **Coke Oven**.

### 6a. Byggt: grund, ugnen och värmen (fas 0–2, v0.200.0)

**Byggblock och recept**

| Item / block | Hur | Anteckning |
|---|---|---|
| **Crushed Quartzite** ×2 | Quartzite på Stone Anvil | – |
| **Unfired Silica Brick** ×4 | 2 Crushed Quartzite + Fire Clay + Quicklime (formlöst) | – |
| **Silica Brick** | Unfired Silica Brick i Clay Kiln, 30 s vid **1250°** (Charcoal + bälg) | – |
| **Silica Bricks** | 4 Silica Brick (2×2) | ugnens golv |
| **Steel Hoop** ×8 | het Steel Ingot på städet, mönstret **Hoops**, 3 slag | bara stål |
| **Banded Furnace Casing** | 4 Silica Brick + Steel Hoop (formlöst) | väggarna; fogarna i nedersta ringen glöder i 3 steg |
| **Clay Tuyere Mold** | 5 Clay Ball på Mold Bench, bränd 600° | gjuter **Bronze Tuyere Nozzle** av 4 enheter brons |
| **Firebox** | Casing + Bronze Tuyere Nozzle + Iron Plate (formlöst) | eldstad, gallret glöder |
| **High Furnace Front** | Casing + 2 Steel Plate + Iron Plate (formlöst) | styrblocket: ståldörr med titthucka |
| **Taphole** | Casing + Iron Tool Rod + Fire Clay (formlöst) | hällning kommer i fas 4 |
| **Great Bellows** | `PLP` / `LGL` / `PTP` (Planks, Leather, Gearbox, Bronze Tuyere Nozzle) | vattenhjulsdriven bälg |

**Formen:** ett tomt inre 2–3 block brett och djupt, Silica Bricks under hela det inre, och en ring av Banded Furnace Casing
runt det inre i varje lager, **5–8 lager**. Hörnen ingår inte och toppen är öppen. I nedersta ringen: exakt en High Furnace
Front, **en Firebox per påbörjat 24-tal inre block** (högst 4) och högst 2 Tapholes. Fronten letar upp formen bakom sig var
2:a sekund och när ett väggblock ändras, och säger vad som är fel vid högerklick (för smal, för bred, för låg, för hög,
saknade väggblock med plats, block i det inre, fel golv, för få eldstäder). En del i en olastad chunk pausar ugnen.

**Värmen:** högerklicka eldstäderna med Charcoal eller Coal (högst 32 var) och tänd med Fire Drill eller Flint and Steel (alla
eldstäder med bränsle tänds). En Great Bellows med munstycket mot en eldstads lucka och en axel från ett vattenhjul in i sidan
ger **blåst = kraften den får** (maskiner på samma axel delar). Mål = **800° + 50° per blåstpoäng**, där blåsten räknas per
volym (`B × 20 / inre block`, högst 16), högst **1600°** med Charcoal. Stiger 6°/s till 800° och 4°/s över, sjunker 2°/s mot
målet (1°/s släckt), gånger `√(20 / inre block)`. En kolbit brinner 60 s utan blåst, `60 / (1 + b/8)` s med blåst b. En ny
ugn torkar ut sina första 3 minuter tänd (högst 600°, ånga ur fogarna). Regn i toppen: −100°. Minsta ugnen med en full
bälg når 1600°, den största behöver fyra.

**Utseende:** fogarna i nedersta ringen, eldstädernas galler och titthuckan glöder i fyra steg; ur toppen stiger rök, över
1000° slår lågor upp, över 1300° flyger gnistor och över 1400° spottar det lava; ner i schaktet glöder bädden och väggarna.
Bälgarnas lock lyfts och fälls i hjulets takt. Skärmen: termometer till 1800° med märken vid 950°, 1450°, 1500° och 1600°,
blåst, varje eldstads bränsle och blåst, uttorkningen och pölens storlek.

Advancements (Steel Age-fliken): **Silica Brick**, **Hooped**, **High Furnace** (goal), **Wind of the River** (challenge, 16
blåst), **White Heat** (1450°). Test: `high_furnace_shape`, `high_furnace_heat`, `waterpower_shared(_bellows)`.

### 6b. Byggt: smältning (fas 3, v0.200.0)

- **Platser:** en per inre block (20–72), en sak per plats. Kasta in malm och tackor genom den öppna toppen; det som inte
  smälter i ugnen kastas tillbaka ut med ett fräs, det som inte får plats ligger kvar på bädden tills en plats blir ledig.
  Alla platser smälter samtidigt: **40 tick per 100 mB** vid smältpunkten, snabbare när det är hetare (som kilnen). För kallt
  eller full pöl: platsen väntar (röd stapel i skärmen, "Needs 1085°").
- **Utbyte:** Crushed Copper/Tin Ore 200 mB, Copper/Tin Concentrate, Native Copper och Raw Copper 400, Raw Gold 300, Raw
  Galena 300 bly, tackor 200, nuggets 22 (9 nuggets blir 198 mB, aldrig en tacka), Copper Block och Gold Block 1800.
- **Pölen:** 400 mB per inre block. Varje smälta är ett lager, efter densitet (guld underst, slagg överst). Koppar och tenn
  blandar sig själva **exakt 9:1** till brons vid 950° (20 mB per tick); det som blir över ligger kvar som eget lager. Ingen
  ny Poor Bronze; omsmälta Poor Bronze-tackor blir ett eget lager.
- **Skärmen:** lagerkolumnen i varje smältas färg (tooltip: mB, tackor, smältpunkt). **Klick på ett lager** väljer vad
  tapphålen ska hälla (ram runt bandet), klick igen går tillbaka till bottenlagret. Platserna visas med föremål och stapel.
- **Utseende:** pölens lager syns som band på innerväggarna och ytan glöder i översta lagrets färg; föremål som smälter
  flyter på ytan (högst 16).
- Advancement **Layer Cake** (tre smältor i pölen samtidigt). Test `high_furnace_melt`.
- Inte gjort: lägga föremål i platserna från skärmen (bara genom toppen); en kall pöl ritas inte.

### 6c. Byggt: hällning, kokill och järn (kärnan av fas 4, 4b och 5, v0.200.0)

- **Pouring Spout** (`P P` / ` P `, Steel Plate) på utsidan av ett Taphole. Högerklick öppnar/stänger. Öppen häller den
  det valda lagret rakt ner i det som står under läppen, **40 mB per tick**, med en glödande stråle i smältans färg,
  gnistor och fräs där den slår ner. Metallen samlas i pipen tills gjutningen är hel; tas formen bort rinner den tillbaka
  i pölen (inget skapas eller försvinner).
- Tar emot: **Clay Mold** (bara koppar och brons, en hel gjutning; över 1100°: "Too hot for clay") och **Iron Ingot Mold**
  (gjutjärnskokill, spricker aldrig: 800 mB av vilken metall som helst → 4 tackor efter 5 s). Kokillen görs tills vidare
  av 5 Iron Plate (`P P` / `PPP`), inte i formflaska.
- **Järn:** Raw Iron, Roasted Iron Ore, Raw/Roasted Bog Iron reduceras vid **1450°** till **Pig Iron** (2× Bloomery) plus
  **Slag** som flyter överst (75 mB, 25 med flux). **Flux** som kastas in läggs på en räknare (1 flux räcker till 4 malmer)
  och ger mer järn och mindre slagg. Varje malm bränner en halv Charcoal ur eldstäderna.
- **Stål:** Pig Iron Ingot, Wrought Iron Ingot (1550°), Steel Ingot och Blister Steel smälter (200 mB). 1 Pig Iron + 2 Wrought
  Iron blandar sig själva till 3 Steel i pölen. Hälls i en Iron Ingot Mold → vanliga Steel Ingots som smids på städet.
- Advancements: **Pour**, **Pig Iron**, **Cast Steel** (challenge). Test `high_furnace_pour`, `high_furnace_iron`.
- **Inte byggt än:** Refractory Trough, Trough Gate, rättvis delning och värmeförlust i rännor, lerrännor som spricker,
  pip i rännände; Pig Bed, Molding Flask, Wooden Patterns, Pattern Bench, Foundry Table/Basin, Iron Plate/Nugget Mold;
  Finery Hearth, Slag Stone; hela fas 6 (glas, Sight Glass, basalt, block, Loam Mold, Salamander, brännskador) och fas 7
  (Coke Oven, Hot Blast Stove, Obsidian, Steel Ladle).

### 6d. Byggt: rännor (resten av fas 4)

- **Refractory Trough** (`B B` / `PBP`: 3 Silica Brick + 2 Iron Plate → 4): samma form, portar och lutningar som en Clay
  Channel, men 200 mB av vilken smälta som helst. Var 5:e tick: först en hel gjutning in i en form bredvid (Clay Mold,
  Iron Ingot Mold ...), sedan nedför delat lika mellan alla lägre rännor, sedan till plana rännor med mindre i (rättvis
  delning: en T fyller båda grenarna). Varje flytt tar exakt det den ger.
- **Räckvidd:** smältan når 8 + 1 per 25° över smältpunkten rännor från tappen (stål tappat vid 1550°: 10); rännan efter
  det fryser direkt.
- **Trough Gate** (Trough + Iron Plate + Iron Tool Rod): rak, högerklick lyfter/sänker luckan; stängd har den inga portar.
- **Lerrännor och hett järn:** en Clay Channel som fogas till en ränna med smälta över 1100° spricker och går sönder,
  metallen förloras.
- **Pouring Spout** häller i en ränna under sig (strålen landar på metallytan), och en pip i en rännas ände häller
  rännans metall nedåt (kortare fläns, ingen tapphålsnisch bakom).
- Stelnad metall slås ut med hacka: tackor och nuggets (22 mB), resten förloras. Ösa med Steel Ladle kommer i fas 7.
- Test `trough_flow`, `trough_pour`. Inte gjort: Sand Runner (Pig Bed, fas 4b), sprickor i två synliga steg (lerrännan
  går sönder direkt), cachat nät (varje ränna flyttar lokalt, ingen nätsökning behövs).
