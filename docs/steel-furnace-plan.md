# Plan: High Furnace, stålålderns stora smältugn (2026-11-24)

> Status: **fas 0–2 byggda (v0.200.0)**, se *Byggt* längst ned och `docs/ages/04-steel-age.md` 6a. Beslut märkta "Beslut (Claude, kan ändras)" är mina förslag; säg till så ändras de.
> Item- och blocknamn står på engelska. Byggs fas för fas (se *Faser*), och varje fas förs in i åldersdokumentet.

Det här vill du ha (översatt): **ett nytt, stort multiblock av stålålderns block som når mycket höga temperaturer, där man
kan smälta det mesta som i bronsugnen, med rinnande metall och allt.** Förebilden för känslan är **Tinkers' Constructs
foundry**: väggar i valfri storlek, ett styrblock med fönster mot smältan, en lista med varje smälta i lager och mB,
föremål som smälter ett och ett med en mätare, legeringar som blandar sig själva och pipar som häller glödande strålar i
gjutbord och gjutgropar. Vi tar **känslan**, inte deras block, namn eller texturer. Allt har egna namn, eget utseende och
följer moddens åldrar: eldfast tegel av våra stenar, stålband, vattenhjulsdrivna bälgar och träkol.

Namn: **High Furnace** (masugn, *Hochofen*). Vanillas "Blast Furnace" är borttagen och namnet hade förvirrat.

---

## 1. Utgångsläge

### Vad som finns i dag

| Steg | Var | Värme | Rymmer | Utbyte | Hur metallen kommer ut |
|---|---|---|---|---|---|
| **Crucible** (sten/brons) | Clay Kiln | 800° med Charcoal, **1300°** med Bellows (+40°/pump, taket spräcker leran) | 10 enheter | malm 1× | hälls för hand i en Clay Mold (60 s innan den stelnar) |
| **Smelting Furnace** (sen brons) | 3×3×3 Furnace Bricks, Furnace Chute, Furnace Tap | samma modell, tak **1300°**, inga sprickor | **40 enheter** (160 kvarts) | malm **1,25×** | tappen fyller en form framför pipen, eller Clay Channels (1 enhet var, flöde var 5:e tick, högst 64 rännor) till formar |
| **Bloomery** (brons→järn) | 1×1×3, Fire Brick | tak **1500°**, järn reduceras vid **1350°** | 1 lupp | 4 Raw Iron → 1 tacka; rostad 2; rostad + flux **3** | het lupp som smids på städet |
| **Cementation** (järn→stål) | Bloomery, 1000° i 6 min | – | 3 tackor | 3 Wrought Iron → 3 Blister Steel → 6 slag var → Steel Ingot | smide |
| **Steel Casting Table** | bredvid en Clay Kiln ≥1100° | – | 8 Gold Ingots | – | ringämnen och kedjor av guld |

- **Smelting Furnace** (`FurnaceMelt`, `FurnaceTapBlockEntity`): allt kastas in i tratten, laddningen (charge) smälter en
  kvarts enhet i taget till en pöl (pool), koppar + tenn blandas till brons med degelns procentregler (9–11,5 % Bronze,
  8–15 % Poor Bronze, annars slagg). Metallen räknas i kvarts enheter och flyttas bara, så inget kan dubbleras.
- **Rännorna** (`ChannelFlow`, `ClayChannelBlockEntity`): portar på blockkanterna, nedför först, en metall per ränna,
  stelnar efter 60 s stilla, ösning med tom Crucible.
- **Formarna** (`ClayMoldBlock`): 21 former, högst 10 enheter (Anvil), spricker efter 5 gjutningar (soapstone aldrig).
  Blocktillståndet `METAL` har bara koppar, brons och poor bronze.
- **Värmen** (`KilnBlockEntity.heatStep`, `speed`): stiger 5°/s mot bränslets naturliga max, sjunker 2°/s över det, +1
  takt per 200° över det som krävs (högst 4×). Bälgar pumpas för hand.
- **Vattenkraft** (`WaterPower`): kraft 0–16 per axellinje, sågen behöver 8. **Kraften delas (byggt v0.200.0):** maskinerna på en
  linje delar linjens kraft lika (`WaterPower.share`, test `waterpower_shared`).

### Gränser i dag (det som den nya ugnen löser)

1. **Taket är 1300°** i allt som smälter. Järn och stål kan inte smälta alls. Det var ett medvetet beslut (2026-11-20:
   *järn smids, det gjuts inte*), och det gäller fortfarande fram till stålåldern.
2. **Bara koppar, tenn och brons** finns som smälta (`Metal` har 4 värden). Guld, silver och bly smälts i kilnen till tackor och
   går aldrig att hälla.
3. **En smälta i taget.** Den som inte går att blanda studsar ut. Ingen överblick över vad som smälter: laddningen är
   en klump utan mätare per föremål.
4. **Fast storlek** (3×3×3, 40 enheter).
5. **Lerrännor och lerformar** klarar högst ~1100°. De största gjutningarna är 10 enheter, det finns inga gjutna block
   och inga strålar som faller: metallen syns bara i rännor och formar.
6. **Bälgarna pumpas för hand.** Vattenhjulet driver bara sågen.

---

## 2. Tinkers-känslan → vårt system

Det här gör en sådan foundry rolig, och så här blir det hos oss:

| Det roliga | Hos oss | Återanvänds | Nytt |
|---|---|---|---|
| **Valfri storlek**, större = rymmer mer | Golv av **Silica Bricks** och väggar av **Banded Furnace Casing** runt ett tomt inre på 2×2 till 3×3 och 5–8 lager, alltså 6–9 m höga som riktiga masugnar. 400 mB per inre block | `FurnaceTapBlockEntity.check`/`problem`/`decorate` (mönstret: kolla varje sekund och säga vad som saknas) | Storleksökning åt alla håll från styrblocket (som Tinkers), rektangulärt inre |
| **Styrblock med fönster** mot smältan | **High Furnace Front**: en ståldörr med en glödande titthucka som visar översta lagrets färg och nivå. **Sight Glass** i väggarna visar hela smältan | `client/MoltenRenderer` (glödande yta, beacon-rendertyp) | En renderare som ritar smältans lager i hela det inre |
| **GUI med varje smälta** i lager och mB | Smältkolumn med band i varje smältas färg, mB och tackor i tooltipen, klick väljer vad som hälls | `client/FurnaceScreen` (tank, termometer från `KilnScreen.thermometer`) | `HighFurnaceScreen` med lagerlista, rullande platser och eldstadslista |
| **Föremål smälter ett och ett** med mätare | En plats per inre block (20–72). Kastas in genom den öppna toppen eller läggs i GUI:t. Varje plats har sin egen stapel | `FurnaceTapBlockEntity.intake` (plocka föremål ur luften) | Platser i stället för en klump; parallell smältning |
| **Malm ger mer** som belöning | Crucible 1× < Smelting Furnace 1,25× < **High Furnace 2×** (järn: 2× Bloomery) | `FurnaceMelt.quarters`, `Casting.oreUnits` | Tabellen i avsnitt 5 |
| **Legeringar blandar sig själva** | Recept med fasta förhållanden: 9 Copper + 1 Tin → 10 Bronze, 1 Pig Iron + 2 Wrought Iron → 3 Steel. Det som blir över ligger kvar som eget lager | konserveringen i `FurnaceMelt` (tusendelar av en kvarts) | `MeltPool` med lager och legeringsrecept |
| **Drains + pipar** som häller en synlig, glödande stråle | **Taphole** i nedersta väggraden, **Pouring Spout** utanpå. Strålen faller på ett Foundry Table, i en Foundry Basin, Molding Flask, Clay Mold eller ränna | `ChannelFlow.port`/`across` (tappens pip blir en port), `FurnaceTapBlockEntity.pour` | Stråle-renderare, hällhastighet i mB/tick |
| **Gjutbord** (tackor, plattor, nuggets, verktygshuvuden) | **Foundry Table** som håller en **Molding Flask** (sandgjutning med **Wooden Pattern**) eller en permanent **Iron Ingot/Plate/Nugget Mold**; för bulk en **Pig Bed** i sand framför tappen | `ClayMoldBlock.Shape` och `Metal.cast` (vad en form ger), `StationRecipe` (Pattern Bench) | Bordet, flaskan, kokillerna, tackbädden, Pattern Bench |
| **Gjutbassäng** (block, stora gjutningar) | **Foundry Basin**: block; med en **Loam Mold** Bell, Cauldron, Great Kettle | – | Bassängen, lerformen |
| **Bränsletankar/värmare** | **Firebox** i väggen: Charcoal (senare Coke) + luft från en **Great Bellows** på vattenkraft | `KilnBlockEntity.FUELS`, `heatStep`, `speed`; `WaterPower.walk` | Blåst → temperatur, delad kraft |
| **Lager**, välj vad som hälls | Lagren ligger efter **densitet** (tyngst underst, slagg överst). Som standard hälls bottenlagret; klick i GUI:t väljer ett annat | – | Lagerordning, val sparas i styrblocket |
| **Hög värme låser upp mer** | 1450° järnmalm, 1500° stål, 1550° smidesjärn, 1400° glas, 1250° gjuten basalt, 1650° stora stålgjutningar, 1700° obsidian | – | Avsnitt 4 |

**Beslut (Claude, kan ändras): värme från fast bränsle och vattenhjulsdrivna bälgar, inte flytande bränsle.** Det passar
åldrarna bättre:
- Träkol (kolmilan) och vattenhjulet är järn- och stålålderns två stora system. Ugnen knyter ihop dem: en kolmila och ett
  hjul per eldstad.
- Lava som bränsle hoppar över hela kedjan, och modden har inget åldersriktigt sätt att flytta lava i rör.
- Flytande bränsle kommer senare som en uppgradering av samma ugn: en **Gas Burner** (kolgas från Coke Oven) i
  ångåldern och en ljusbåge i elåldern. Firebox-platsen i väggen är förberedd för det.

---

## 3. Multiblocket

### Byggblocken (stålålderns palett)

| Block / item | Recept | Utseende |
|---|---|---|
| **Crushed Quartzite** ×2 | Quartzite (block eller kullersten) på Stone Anvil, 4 slag (eller Steel Quarry Hammer) | Vitt krossat glittrigt grus |
| **Unfired Silica Brick** ×4 | 2 Crushed Quartzite + 1 Fire Clay + 1 Quicklime (formlöst). Kalken binder, som i riktiga **silikategel** (Dinas) | Ljust, grynigt |
| **Silica Brick** | Unfired Silica Brick i Clay Kiln, **30 s vid 1250°** (Charcoal + Bellows, Iron Bellows gör det lätt) | Gräddvitt tegel med gula prickar |
| **Silica Bricks** | 4 Silica Brick (2×2). Golvet | Gräddvita, tunna fogar, sot mot kanterna när de använts |
| **Steel Hoop** ×8 | En het Steel Ingot, nytt mönster **Hoop** på städet, 3 slag (Iron Smithing Hammer) | Blåsvart stålband med nitar |
| **Banded Furnace Casing** | 4 Silica Brick + 1 Steel Hoop (formlöst). Väggarna | Silikategel i två stålband med nitar; sotränder uppåt; glöder i fogarna vid hetta |
| **Bronze Tuyere Nozzle** | Gjuts i en ny Clay Mold **TUYERE** (4 enheter brons). Riktiga formor var vattenkylda kopparmunstycken | Tjockt bronsrör med fläns |
| **Firebox** | Banded Furnace Casing + Bronze Tuyere Nozzle + Iron Plate (lucka) | Gjutjärnslucka med galler, munstycket i sidan, glöder genom gallret |
| **High Furnace Front** (styrblocket) | Banded Furnace Casing + 2 Steel Plate + Iron Plate (formlöst) | Ståldörr med gångjärn och en rund titthucka (ett öppet hål med en svängbar järnlucka) där smältan glöder |
| **Taphole** | Banded Furnace Casing + Iron Tool Rod + Fire Clay | Ett lerpluggat hål i en stålram |
| **Pouring Spout** | 3 Steel Plate (`P P` / ` P `) | Kort stålränna med spak; sätts på utsidan av ett Taphole eller i änden av en ränna |
| **Sight Glass** (fas 6) | Banded Furnace Casing + 2 Glass Pane | Stålramat fönster; smältan syns igenom |
| **Great Bellows** | `PLP` / `LGL` / `PTP` (P Planks, L Leather, G Gearbox, T Bronze Tuyere Nozzle) | Två kilformade läderbälgar som kammar på en axel lyfter i takt: den svenska hyttans *blåsbälg* |

Allt bryts med hacka. Den minsta ugnen (inre 2×2×5) kostar 4 Silica Bricks + 40 väggblock ≈ 176 Silica Brick och
**5 Steel Ingots** (hoops). Den största (inre 3×3×8) kostar 9 + 96 väggblock ≈ 420 Silica Brick och **12 Steel Ingots**.

### Storlek: variabel, men med ett verkligt tak

**1 block = 1 meter.** Medeltida och tidigmoderna masugnar, som bergslagens hyttor, var ungefär **6–9 m höga**. Deras
**ställ** (härden längst ner, där smältan samlas) var **1–2 m** brett, och deras **rast** (den bredaste delen av schaktet)
var upp till **cirka 3 m**. Ugnen följer de måtten:

| | Min | Max | Varför |
|---|---|---|---|
| Inre bredd och djup | **2** | **3** | Ställ 1–2 m, rast högst ~3 m. Ett 1 block brett inre är för trångt för att smälta och hälla (ugnen blir en Bloomery), och ett inre över 3 m hade ingen hytta med vattenhjulsbälgar kunnat blåsa varm |
| Väggar (lager) | **5** | **8** | Med golvet blir ugnen **6–9 m** hög. Ett högt schakt värmer beskickningen på vägen ner, och det var därför masugnar var höga och smala |
| Utvändigt | 4×4 × 6 | **5×5 × 9** | Väggarna är 1 m tjocka (hörnen valfria) |

Tillåtna inre: 2×2, 2×3, 3×2 och 3×3, med 5–8 lager. **Förenkling:** det inre är lika brett hela vägen upp. En riktig
masugn är smalast i stället, bredast i rasten och smalnar igen mot gichten (toppen). Det ritas in i väggtexturen men
kontrolleras inte.

| Inre | Block | Rymmer | Platser | Fireboxes (min) | Blåst till 1450° | Vattenhjul (fulla, 16) |
|---|---|---|---|---|---|---|
| 2×2×5 (minsta) | 20 | 8 000 mB (40 tackor) | 20 | 1 | 13 | 1 |
| 2×3×6 | 36 | 14 400 mB (72 tackor) | 36 | 2 | 24 | 2 |
| 3×3×5 | 45 | 18 000 mB (90 tackor) | 45 | 2 | 29 | 2 |
| 3×3×8 (största) | 72 | 28 800 mB (144 tackor) | 72 | 3 | 47 | 3 |

Smelting Furnace rymmer 4 000 mB. Även den minsta High Furnace rymmer dubbelt så mycket, och den största 7 gånger så mycket.

**Gichtbryggan:** toppen ligger 6–9 m upp, och där kastas malm, flux och glas in. Riktiga hyttor byggdes mot en slänt med
en bro till gichten. Bygg en ramp, trappa eller bro dit. Ugnen kräver det inte, men det är så man matar den.

### Formen

```
Sett ovanifrån, största ugnen (inre 3×3), nedersta lagret:     Från sidan (inre 3×3×8):

   ·  C  C  C  ·          · = hörn, valfritt (inte del av ugnen)     C C C C C   <- lager 8 (gichten, öppen topp)
   C  _  _  _  F          C = Banded Furnace Casing                  C _ _ _ C      ... lager 2–7: Casing / Sight Glass
   F  _  _  _  T          _ = inre (luft; smältan står här)          C _ _ _ T   <- lager 1: Front, Fireboxes, Tapholes
   C  _  _  _  F          F = Firebox, T = Taphole                   S S S S S   <- golv: Silica Bricks under det inre
   ·  C  H  C  ·          H = High Furnace Front
```

- **Inre:** ett rektangulärt tomrum, **2–3 block** brett och djupt, **5–8 lager** högt. Det får inte innehålla block
  (föremål och smältan är inte block).
- **Golv:** Silica Bricks under hela det inre.
- **Väggar:** en ring runt det inre i varje lager. Varje väggruta är Banded Furnace Casing eller ett av specialblocken.
  **Hörnen ingår inte** (bygg pelare där om du vill).
  - **High Furnace Front:** exakt 1, i nedersta lagret.
  - **Firebox:** i nedersta lagret (formnivån), minst **1 per påbörjat 24-tal inre block** (20–24: 1, 25–48: 2, 49–72: 3),
    högst 4. Riktiga hyttor hade 1–2 formor, de största 3–4.
  - **Taphole:** i nedersta lagret, 0–2 (ugnen formas utan, men kan inte hällas).
  - **Sight Glass:** var som helst i väggarna ovanför nedersta lagret.
- **Toppen är öppen** (gichten). Föremål kastas in ovanifrån. Regn ner i ugnen kyler den.
- **Hitta formen:** när Front sätts, eller när man högerklickar den, letar den bakåt efter det inre (luften bakom den, med
  Silica Bricks under). Sedan går den åt sidorna tills den når väggen (högst 3), och uppåt så länge ringen är hel (högst
  8 lager).
- **Kollas** var 40:e tick och när ett väggblock ändras (väggblocken säger till styrblocket). Delar i en chunk som inte är
  laddad **pausar** ugnen, de bryter den inte.
- **Felmeddelanden** (högerklick på Front), som Smelting Furnace: "Not a High Furnace yet: 3 wall blocks missing (one at x y z)",
  "The furnace needs 2 Fireboxes for its size (has 1)", "The inside must be empty: clear x y z", "The floor under the
  inside must be Silica Bricks: x y z", "Too wide: a real furnace's bosh is at most 3 blocks", "Too narrow: the hearth
  needs at least 2 blocks", "Too low: at least 5 wall layers (6 m with the floor)", "Too tall: at most 8 wall layers".
- **Bryts ugnen** slocknar den, men **smältan ligger kvar i styrblocket** (stelnad om den hinner svalna) och smälter igen när ugnen är hel
  och varm. **Bryts själva Front** tappar den en **Furnace Salamander** (en tung klump, det riktiga ordet för järnklumpen som blir kvar i en
  nedlagd masugn). Den bär **hälften** av smältan som den var, och smälter tillbaka i en High Furnace. Så går smältan
  inte att dubblera, men allt är inte heller förlorat.

### Utseende

- **Front:** titthuckan glöder i översta lagrets färg, lyser 13. Vid start ånga, sedan värmedallring.
- **Fireboxes:** gallret glöder i fyra steg (kallt, rött, orange, vitgult). Munstycket pulserar i bälgens takt.
- **Väggarna:** nedersta ringens fogar glöder i fyra steg (blocktillståndet `HEAT`, sätts bara när steget byts).
- **Öppna toppen:** smältans yta syns i lager med glödande skorpa som driver. Över 1200° slår låga upp i kanten, över
  1400° flyger gnistor. Föremål som ännu inte smält ligger och flyter på ytan (högst 16 syns).
- **Strålen:** när en Pouring Spout är öppen faller en tjock, glödande stråle (animerad, rullande textur, vitgul för järn och stål,
  orange för brons, grön för glas), fräser och stänker när den träffar. Pipen lyser 15.
- **Ljud:** blåstens dova brus i loop (starkare med mer blåst), bälgarnas knarr i hjulets takt, lavabubbel ur toppen, fräs när en stråle
  träffar.

---

## 4. Värme

### Bränsle och luft

- **Fireboxes tar bränslet:** högerklicka med Charcoal eller Coal (högst **32** var). Föremål som kastas in i toppen
  hamnar aldrig i en eldstad.
- **Great Bellows** sätts med munstycket mot en Firebox och en axel från ett vattenhjul in i sidan (som sågbänken). Den ger
  **blåst b = kraften den får (0–16)**.
- **Beslut (Claude, kan ändras): kraften delas mellan maskinerna på en axellinje.** Ett hjul som driver två bälgar ger 8
  var. Annars hade ett hjul och en Gearbox drivit åtta eldstäder. Sågen ensam på en linje märker ingen skillnad.
- **Handbälgar** (Bellows, Iron Bellows) gör inget här. Ugnen är för stor för en människas lungor.
- **Tänds** med Fire Drill eller Flint and Steel på en Firebox. Alla eldstäder med bränsle tänds.

### Temperatur

Ugnens mål räknas på **blåsten per volym**. En stor ugn behöver mer luft för samma hetta:

```
B      = summan av blåsten från alla bälgar på tända eldstäder med bränsle
B_eff  = min(16, B × 20 / inre block)          (den minsta ugnen, 20 block, får B rakt av)
mål    = bränslets naturliga max + 50° × B_eff, högst bränslets tak
```

Har eldstäderna olika bränsle gäller det sämsta (lägst naturligt max och tak).

| Bränsle | Naturligt | Tak | Brinner per eldstad (b = 0) | Vid full blåst |
|---|---|---|---|---|
| Charcoal / Coal | 800° | **1600°** | 60 s | 20 s (`60 / (1 + b/8)`) |
| Coke (fas 7) | 950° | **1750°** | 120 s | 40 s |
| + Hot Blast Stove (fas 7) | | +100° på mål och tak, **högst 1800°** | | 25 % längre |

| B_eff (Charcoal) | Mål | Vad det betyder |
|---|---|---|
| 0 | 800° | Brons (950°) går inte ens |
| 7 | 1150° | Brons, koppar, guld, silver, bly |
| 10 | 1300° | + tackjärn smälter om, gjuten basalt |
| 13 | **1450°** | + **järnmalm** blir tackjärn |
| 14 | 1500° | + **stål** |
| 15 | 1550° | + smidesjärn |
| 16 | 1600° | Charcoals tak |

**Fler vattenhjul.** En axellinje bär högst 16 kraft. Flera hjul på samma axel lägger ihop sin kraft (mech2), så två
undershot-hjul (7 + 7) eller ett overshot-hjul på en sida och ett undershot-hjul (10 + 7, högst 16) driver en bälg nästan fullt. En
större ugn behöver **fler linjer**: en bälg per eldstad, och varje bälg på en egen axel med eget hjul eller egna hjul.

- Minsta ugnen (20 block): en bälg på 13 räcker till järn, till exempel två hjul på samma axel.
- 3×3×5 (45 block): 29 blåst, två eldstäder med var sin nästan full linje.
- Största (72 block): 47 blåst till järn, alltså **tre fulla linjer**. Charcoals tak (1600°) kräver 58, så **fyra eldstäder** med
  fyra linjer. Med tre når den knappt 1470°: järn, men inte stål.

**Bränsle och luft skalar med storleken:** varje eldstad bränner sitt eget kol (tabellen ovan), och en stor ugn har fler
eldstäder. Största ugnen med fyra eldstäder på full blåst äter **12 Charcoal per minut** plus reduktionskolet, och den minsta 3.

- **Stiger** 6°/s upp till 800° och 4°/s över, gånger en storleksfaktor `√(20 / inre block)` (största: 0,53). Kall →
  1450°: minsta **cirka 5 min**, största **cirka 9,5 min**.
- **Sjunker** 2°/s mot målet när den är över det (hjulet stannar: ner mot 800°), 1°/s släckt, gånger samma storleksfaktor.
  En stor ugn håller värmen länge: släckt tappar den största ungefär 30° i minuten.
- **Regn** i den öppna toppen: målet −100°.
- **Uttorkning:** en nybyggd ugn (ny Front) håller högst 600° de första **3 minuterna** tänd, och ånga pyser ur fogarna
  (samma mönster som kilnens första bränning, `FIRE_TICKS`).
- **Ingen överhettning och inga sprickor.** Silikateglet tål taket.

### Vad varje temperatur låser upp

| ° | Smälter / händer |
|---|---|
| 232 | Tin |
| 330 | Lead |
| 950 | Bronze (och legering 9:1) |
| 1000 | Silver |
| 1085 | Copper |
| 1100 | Gold |
| 1200 | Pig Iron (smälter om, kan hällas) |
| 1250 | Basalt → Molten Basalt |
| 1400 | Glass Batch → Molten Glass |
| **1450** | Järnmalm → Pig Iron + Slag |
| **1500** | Steel (smälter, legeras 1:2) |
| 1550 | Wrought Iron |
| 1600 | Charcoals tak |
| 1650 | Stora stålgjutningar: Steel Block och Anvil (de stelnar annars i pipen) |
| 1700 | Obsidian → Molten Obsidian (kräver Coke) |
| 1750 / 1800 | Coke / Coke + Hot Blast: allt går upp till 4× fortare |

**Takt:** som i kilnen, `KilnBlockEntity.speed`: +1 per 200° över det som krävs, högst 4×.

---

## 5. Vad den smälter

### Enheter

Modden räknar redan i kvarts enheter. Ugnen visar **mB**: **1 enhet = 100 mB, 1 kvarts = 25 mB, 1 tacka = 200 mB,
1 block = 1800 mB.** Inuti hålls smältan i tusendelar av en kvarts, som i `FurnaceMelt`, så att inget avrundas fram och tillbaka.
En nugget som smälts ger **22 mB** (avrundat nedåt: 9 nuggets = 198 mB < 1 tacka, så det går inte att fuska). Nuggets gjuts 9 åt gången
av 200 mB.

**Rymmer:** **400 mB per inre block.** 2×2×5 = 20 block = **8 000 mB** (40 tackor; Smelting Furnace: 4 000). 3×3×5 =
18 000 mB. 3×3×8 = **28 800 mB** (144 tackor). Slagg räknas med, så den måste tappas bort.

**Platser:** en per inre block (20–72). En plats tar **ett** föremål (malm, tacka, block). Alla platser smälter samtidigt:
**40 tick per 100 mB** vid smältpunkten, snabbare när det är hetare. En Crushed Copper Ore (200 mB här) tar alltså 4 s.
Det som inte smälter vid nuvarande värme väntar (röd stapel, tooltip "Needs 1450°"). Det som inte får plats i pölen väntar
också. Föremål som aldrig smälter studsar ut ur toppen med ett fräs.

### Utbyte (malm är belöningen)

| In | Crucible / Kiln | Smelting Furnace | Bloomery | **High Furnace** |
|---|---|---|---|---|
| Crushed Copper / Tin Ore | 100 mB | 125 | – | **200** |
| Copper / Tin Concentrate, Native Copper | 200 | 250 | – | **400** |
| Raw Iron | – | – | 50 (100 med flux) | **100** (200 med flux) |
| Roasted Iron Ore | – | – | 100 (150 med flux) | **200** (300 med flux) |
| Raw / Roasted Bog Iron | – | – | ⅔ av ovan | **⅔ av ovan** (65 / 130 / 130 / 200) |
| Raw Gold | 200 (Kiln) | – | – | **300** |
| Raw Galena | 200 bly (Kiln) | – | – | **300 bly** (silvret kupelleras som förut) |
| Tackor (alla) | – | 200 | – | **200** (exakt, ingen vinst) |
| Blister Steel | – | – | – | 200 Steel (stålet slipper 6 slag) |
| Metallblock (Copper, Gold, Bronze, Steel) | – | – | – | 1800 |

- Basmetaller ger **2×** degeln (järn 2× Bloomery). Ädelmetaller ger 1,5×, så att myntekonomin inte flyter över.
- **Flux** (`mechanical:flux`: krossad Limestone eller Dolomite) kastas in och läggs på en räknare. En flux räcker till 4
  järnmalmer.
- **Reduktionskol:** varje järnmalm tar dessutom en halv Charcoal ur eldstäderna. Masugnen äter kol.
- **Slagg:** järnmalm ger **25 mB Molten Slag med flux, 75 mB utan**. Andra malmer ger ingen slagg.

### Familjer, lager och legering

- Allt får ligga i pölen samtidigt (det är det som gör det roligt). Varje smälta är ett **lager**. Lagren ligger efter
  densitet: Gold, Lead, Silver, Copper, Bronze, Pig Iron, Steel, Wrought Iron, Tin, Glass, Basalt, Obsidian och **Slag överst**.
- **Legeringar blandar sig själva** när båda delarna finns och ugnen är varm nog. Det går 20 mB per tick och recept. Det som blir över
  ligger kvar som eget lager:

| Recept | Förhållande | Kräver |
|---|---|---|
| **Bronze** | 9 Copper + 1 Tin → 10 Bronze | 950° |
| **Steel** | 1 Pig Iron + 2 Wrought Iron → 3 Steel | 1500° |

- **Beslut (Claude, kan ändras): ingen Poor Bronze i High Furnace.** Ugnen blandar exakt. Ingen ny Poor Bronze skapas, och
  slaggregeln för fel tennhalt gäller inte här. Det är en belöning för ugnen. Poor Bronze-tackor som smälts om förblir ett
  eget lager Poor Bronze. Smelting Furnace behåller sina procentregler.
- **Tackjärn → smidesjärn:** i en **Finery Hearth** (fas 5), ett eget litet block bredvid ugnen: en härd med en Great
  Bellows. Pig Iron Ingot + Charcoal → Wrought Iron Ingot, 20 s vid 1300°. Det är **vallonsmidet** från Uppland. Så slipper man
  Bloomery och luppens alla slag, och 1 malm ger upp till 1,5 smidesjärn mot Bloomerys 0,75.
- **Glas:** **Glass Batch** ×4 = 4 Sand + 1 Wood Ash (formlöst). Det är skogsglas med pottaska som flussmedel. 250 mB var vid
  1400°.
- **Gjuten basalt:** Basalt (alla basaltblock) → 250 mB vid 1250°.
- **Obsidian:** Obsidian → 250 mB vid 1700° (Coke).
- **Inte i ugnen:** trä, mat, sten som inte står ovan.

### Välja vad som hälls

- Som standard häller alla Tapholes **bottenlagret**.
- **Klick på ett lager i GUI:t väljer det** (en ram runt bandet, och titthuckan visar det lagret). Tapholes häller det valda
  tills det är slut, sedan bottenlagret igen. Valet sparas.
- Slaggen tappar man genom att välja den och hälla i en Foundry Basin (→ Slag Stone) eller i en ränna som rinner bort.

---

## 6. Rinnande metall: tappa, hälla och gjuta

### Taphole och Pouring Spout

- En **Pouring Spout** sätts på utsidan av ett Taphole. **Högerklick öppnar och stänger** (spaken fälls).
  Öppen häller den det valda lagret när ugnen är hel och minst så varm som lagrets smältpunkt.
- **Häller rakt ner** i blocket under pipen med en synlig stråle, **40 mB per tick** (en tacka på 5 tick, ett block på 45 tick, samma takt som en Refractory Trough).
  Det fyller det som står där: Foundry Table, Foundry Basin, Molding Flask, Iron Ingot Mold, Pig Mold, Clay Mold (bara smältor ≤ 1100°). Står det inget där
  stängs flödet (ingen metall på marken).
- En **ränna** vid pipens nivå (som vid dagens Furnace Tap) fylls via `ChannelFlow`. Pipen är en port.
- En Pouring Spout kan också sitta **i änden av en ränna**. Då häller den rännans metall nedåt i det som står under. Så blir varje
  rännsystem en kaskad av strålar.

### Rännor: Refractory Trough (stålålderns rännor)

Metallen ska fortfarande **rinna i rännor** från tappen till bord, bassänger, formflaskor och tackbädden, som lerrännorna i
bronsåldern men en nivå upp. All logik i `ChannelFlow` står kvar: portar på blockkanterna, lutningar som räls, nedför först,
en smälta per ränna, formar som sitter lägre än rännan och fylls först, stelning efter tid och ösning. Det som ändras är att
**värdena blir per ränntyp** i stället för konstanter.

| | Clay Channel (brons) | **Refractory Trough** (ny) | **Sand Runner** (tackbädden) |
|---|---|---|---|
| Recept | Clay Ball, bränd 600° | 3 Silica Brick + 2 Iron Plate (`B B` / `PBP`) → 4. En järnränna med silikateglets foder | En spade på **Casting Sand** som ligger på marken: den gräver ut en ränna i sanden |
| Tål | ≤ 1100° | allt (till 1800°) | allt |
| Rymmer | 100 mB (4 kvarts) | **200 mB** | 200 mB |
| Flöde per steg (5 tick) | 100 mB (`RATE` 4) | **200 mB** (`RATE` 8) | 200 mB |
| Stelnar efter | 60 s stilla | 60 s stilla | 30 s (sanden drar värme) |
| Slits | nej | nej | sanden blir bränd efter varje tappning: dra spaden igen |
| Värmeförlust | nej (som i dag) | ja, se nedan | ja, dubbelt så mycket |

- **Grenar och delning:** T-, hörn- och korsrännor fungerar som i dag. Ett nytt steg i flödet gör delningen rättvis: en ränna som
  har flera vägar åt samma håll (nedför, eller i plan bort från tappen) **delar lika** mellan dem (resten roterar mellan
  vägarna steg för steg), i stället för att fylla den första grenen först. Så fylls en rad formar längs en huvudränna
  ungefär samtidigt. Formarna vid rännan fylls fortfarande en per steg, nu **en per gren och steg**.
- **Trough Gate (slussucka):** ett eget block: en Refractory Trough med en järnlucka (Refractory Trough + Iron Plate +
  Iron Tool Rod). Högerklick fäller luckan upp och ner. **Stängd har den inga portar åt luckans håll**, så att
  `ChannelFlow.ports` släpper inget förbi: metallen står och väntar (och stelnar efter 60 s). Med en lucka i varje gren styr
  man vart tappningen går: först tackbädden, sedan bordet, sedan bassängen. Luckan syns stängd och öppen, och metallen
  pressar glödande mot den.
- **Värmeförlust, så att avståndet spelar roll:** nätet räknar redan hur långt varje ränna ligger från tappen
  (`Net.depth`). En smälta kommer högst
  `räckvidd = 8 + (temperatur vid tappen − smältpunkt) / 25` rännor (Sand Runner: hälften) innan den **fryser i rännan**.
  Rännan längst bort stelnar direkt, mörk med en ljus front. Exempel:
  - tackjärn (1200°) tappat vid 1450°: 18 rännor;
  - stål (1500°) vid 1550°: 10, och vid 1600°: 12;
  - brons (950°) vid 1300° i en Refractory Trough: 22.

  Lerrännor räknar inte värmeförlust, så bronsålderns nät beter sig som i dag. En het ugn når alltså längre, och långa
  rännor till en avlägsen bassäng kräver mer blåst.
- **Stelnad metall** i en Refractory Trough slås ut med hacka. Den ger tillbaka det den rymde som tackor och nuggets
  (22 mB var), och det som blir över under en nugget går förlorat. Ösa med Crucible fungerar bara för smältor ≤ 1100°. En
  **Steel Ladle** (fas 7, 1000 mB) öser allt.
- **Pouring Spout i änden av en ränna** häller rännans metall nedåt (avsnittet ovan). Så kan en ränna löpa över en rad
  bord och hälla i dem genom sina pipar.
- **Lerrännor och järnets hetta:** smälta över 1100° **rinner in i en Clay Channel men spräcker den**. Leran glöder vitt,
  spricker synligt i två steg (1 s var) och **går sönder**, och metallen i den rinner ut och förloras (fräs, eld, rök), som när
  en ränna med metall i bryts i dag. Ett lernät klarar alltså ingen järntappning. Det ska synas att det är fel material.
  Clay Molds tar inte emot smälta över 1100° alls (pipen säger "Too hot for clay").

**Utseende i rännorna:** metallen glöder i sin färg (avsnittet nedan) och strömmar i flödets riktning med rullande yta,
som dagens rännor. **Fronten** av ett flöde som är på väg är mörkare och skorpig. Metall som står svalnar under
stelningstiden: vit → gul → orange → mörkröd → grå skorpa. Gnistor stänker där en ränna fyller en form, och luckan
glöder när metall står mot den.

### Gjutning i stålåldern: en ny nivå

Lerformarna finns kvar för brons och koppar, men de är inte ugnens väg. Stålålderns gjuteri bygger på hur riktiga gjuterier
arbetade: **tackbädd** för bulk, **sandgjutning i formflaska** för allt formgjutet, **permanenta gjutjärnskokiller** för
standardformer, **bord och bassäng** att ställa formarna i, och **lerformar (loam)** för de allra största engångsgjutningarna.

#### Clay Mold mot den nya nivån

| | Clay Mold (brons) | Pig Bed | Molding Flask | Iron Ingot / Plate / Nugget Mold | Loam Mold |
|---|---|---|---|---|---|
| Tål | ≤ 1100° | allt | allt | allt | allt |
| Hållbarhet | 5 gjutningar (soapstone ∞) | ∞ (spaden drar om sanden) | ramen ∞, mönstret 128 tryck, **3 av 4 Casting Sand kommer tillbaka** | **∞, spricker aldrig** | 1 gjutning (slås sönder) |
| Största gjutning | 1000 mB (Anvil) | hela tappningen | **2400 mB** | 800 mB | **4000 mB** |
| Föremål per hällning | 1 | sugga + upp till 8 tackgrisar | 1 (nuggets 9, pilspetsar som förut) | **4 tackor** / 1 platta / **18 nuggets** | 1 |
| Metall per verktygshuvud | Clay Mold-enheterna (hackhuvud 600 mB) | – | **⅔ av det** (hackhuvud 400 mB): tunnare gods i fin sand | – | – |
| Yta | Rough Cast, slipas på Whetstone | grov | **färdig**, inget slipsteg | färdig | färdig |
| Stelnar | 5 s | 30 s (sanden) | 5–15 s efter storlek | 5 s, **2,5 s på ett Foundry Table** | 30 s |

#### Pig Bed (tackbädd, "sow and pigs")

- Billig, stor och grov. **Casting Sand** läggs som block på marken framför tapphålet. **Spade** på sanden gräver en **Sand
  Runner** (suggan, huvudrännan, en `ChannelFlow`-ränna). **Pig Pattern** på sanden bredvid trycker en **Pig Mold** (en
  tackgrop som rännan fyller).
- En Pig Mold tar **400 mB** och ger **2 tackor** av metallen (Pig Iron Ingot för tackjärn; annars metallens tacka).
  Metall som stelnar i suggan grävs ut med spade som tackor och nuggets.
- Efter varje tappning är sanden bränd: dra spaden och tryck mönstret igen. Det är arbetet som är priset.
- Det är så man tar emot en hel masugnstappning på 40–144 tackor på en gång.

#### Molding Flask (sandgjutning)

- **Casting Sand** (block, ligger som sand): 4 Sand + 1 Clay Ball + 1 **Coal Dust** → 4. Kolstoftet hindrar metallen från att
  bränna fast. **Coal Dust** ×2: Coal eller Charcoal på Stone Anvil (texturen `coal_dust` finns).
- **Molding Flask:** 4 Iron Plate + 2 Iron Tool Rod (`P P` / `R R` / `P P`), en järnram i två halvor, överlåda och
  underlåda (cope och drag). Ställs på marken eller på ett Foundry Table.
- **Så gör man:** högerklicka flaskan med **4 Casting Sand**, sedan med ett **Wooden Pattern**. Mönstret trycks i, flaskan stängs
  och ett ingjutshål syns överst. Flaskan har portar som en form, så den fylls från en pip ovanifrån eller från en ränna
  bredvid. När metallen stelnat **slår man ur** flaskan (högerklick med hacka eller hammare): gjutningen och **3 Burnt
  Casting Sand** faller ut, och ramen står tom kvar. 4 Burnt Casting Sand + 1 Clay Ball + 1 Coal Dust → 4 Casting Sand.
- **Det här gör flaskan:**
  - **Verktygshuvuden** av koppar och brons, alla Clay Mold-former, ⅔ av metallen och färdiga utan slipning.
  - **Plattor**.
  - **Cast Iron Gear** (200 mB tackjärn, till ångålderns maskiner).
  - **Cast Iron Pot** (800 mB, kök: rymmer som Iron Cauldron men står direkt på en Campfire).
  - **Anvil** (stål 2400 mB, ≥ 1650°, vanillas Anvil).
  - **Iron Ingot Mold, Iron Plate Mold och Iron Nugget Mold** (1000 / 1000 / 600 mB tackjärn): sandgjutningen gör de
    permanenta formarna.
- **Wooden Patterns** täljs på en ny **Pattern Bench** (snickarbänk: 4 Planks + 2 Logs + Bronze Chisel). Den är byggd som Leatherworking
  Bench och Mold Bench (`StationRecipe` och `RecipeStationMenu`): plankor in, välj mönster i listan. Mönster: Ingot Mold,
  Plate Mold, Nugget Mold, Pig, Gear, Pot, Anvil, Pane och alla Clay Mold-former. **Strickle Board** (Bell och Cauldron)
  används till lerformar.

#### Iron Ingot Mold, Iron Plate Mold, Iron Nugget Mold (permanenta kokiller)

- Gjutjärnsformar (se ovan). **Går aldrig sönder.** Det är det snabba sättet för standardformer, framför allt på ett
  Foundry Table.
- **Iron Ingot Mold:** 4 fack, 800 mB → **4 tackor** av vilken metall som helst.
- **Iron Plate Mold:** 400 mB → 1 platta: Bronze Plate, **Iron Plate** (smidesjärn) eller **Steel Plate** (se Öppna
  frågor: plattor är halvfabrikat, inte verktyg).
- **Iron Nugget Mold:** 400 mB → 18 nuggets (guld, silver, koppar, järn).
- Kan också stå på marken under en pip, men stelnar då i vanlig takt.

#### Foundry Table och Foundry Basin

Stålålderns gjutbord och gjutbassäng. De heter inte "Casting Table/Basin", eftersom Steel Casting Table (guldet) redan finns och
namnen inte ska vara Tinkers.

- **Foundry Table:** stålskiva på ben med eldfast foder och en vattenkylning, 3 Steel Plate + 2 Silica Bricks + Bronze Bucket.
  **Håller en form:** en Iron Ingot/Plate/Nugget Mold eller en Molding Flask (lägg på med högerklick, ta av med smyg). Formen fylls från en
  pip ovanför eller från en ränna (bordet har portar på fyra sidor). Kylningen gör att det gjutna **stelnar på halva tiden**. Tom hand tar
  ut det gjutna, och formen ligger kvar.
- **Foundry Basin:** djup ståltunna med silikatfoder, 5 Steel Plate + 4 Silica Bricks (`P P` / `PBP` / `BPB`). Den tar emot
  pip och ränna.
  - **Tom** gjuter den **block** (1800 mB metall, 250 mB för resten): Copper Block, Gold Block, Lead Block, **Bronze
    Block** och **Steel Block** (nya; bronsålderns beslut "Casting Basin: nej" gällde för att ingen metall var värd ett
    block då. Steel Block kräver ≥ 1650°), **Glass**, **Slag Stone**, **Cast Basalt** och **Obsidian**.
  - **Med en Loam Mold** i: de stora engångsgjutningarna.
  - Fyllnaden syns stiga. Stelnar på 10 s per 1000 mB.
- **Slag Stone** (slaggsten, bruksorternas byggsten i Bergslagen) får Slab, Stairs, Wall och Bricks. **Cast Basalt** blir
  mörka blanka plattor (Tiles).

#### Loam Mold (valfri, för de största gjutningarna)

- **Loam** ×4: 2 Clay Ball + 1 Sand + 1 Wheat (halm i leran, som klockgjutarna gjorde). En **Strickle Board** (Bell eller
  Cauldron, från Pattern Bench) på Loam vid Pattern Bench → **Unfired Loam Mold** → bränd i Clay Kiln vid 600° → **Loam
  Mold**.
- Läggs i en Foundry Basin. Den tar en gjutning och slås sedan sönder när det gjutna tas ut.
  - **Bell:** brons, 1600 mB, vanillas Bell.
  - **Cauldron:** tackjärn, 1400 mB, en gjuten Cauldron i stället för 7 Iron Plate.
  - **Great Kettle:** brons, 4000 mB, dekor och kök.

#### Vad som kräver den nya nivån

- **Allt av tackjärn, smidesjärn och stål:** tackor, plattor, Gear, Pot, Anvil, Cauldron, Steel Block och kokillerna.
- **Allt över 1000 mB:** block, Bell, Great Kettle.
- **Glas, Slag Stone, Cast Basalt, Obsidian.**
- **Guld och silver som gjutna nuggets och block.**
- **Brons och koppar** gjuts fortfarande i Clay Molds (bronsålderns väg). Flaskan gör samma huvuden billigare (⅔) och färdiga.

#### I koden: återanvänt och nytt

| Del | Återanvänds | Nytt |
|---|---|---|
| `ClayMoldBlock` | oförändrad för brons. `METAL` får inga nya värden (tillståndsexplosion) | vägrar smälta > 1100° |
| `ClayMoldBlock.Shape` | blir mönsterlistan för flaskan (former är en konstruktorparameter, inte ett tillstånd, så fler värden är ofarliga) | PIG, GEAR, POT, PANE, INGOT_MOLD, PLATE_MOLD, NUGGET_MOLD, ANVIL_STEEL |
| `Metal.cast(shape)` | "vad blir det" för alla nya former | kartor för de nya smältorna |
| `ChannelFlow` | portar, `across`, `explore`, `elevation`, flöde, `fillMold`, `scoop` | ränntyper (kapacitet, `RATE`, högsta temperatur), rättvis delning, luckor utan port, räckvidd från `Net.depth`, ett `CastSink`-gränssnitt i stället för `instanceof ClayMoldBlock` (Clay Mold, Pig Mold, Flask, Table, Basin) |
| `ClayChannelBlockEntity` | lagringen (smälta, mängd, `movedAt`) | sprickor vid > 1100°; Refractory Trough och Sand Runner delar BE:n |
| `Casting` | registrering, `SOLID_CRUCIBLE`, `CRUCIBLE_CONTENTS` | nya items och block |
| `CrucibleContents` / `CrucibleItem` | oförändrade (degeln är lera: ≤ 1100°) | – (Steel Ladle i fas 7) |
| `MoldBench` | mönstret för en receptbänk | **Pattern Bench** med samma `StationRecipe`/`RecipeStationMenu` |
| `MoltenRenderer` | ytan i rännor och formar | färger per smälta, svalningsfärg, front, strålen |

### Hur smältan ser ut

- **Färger:** Tin silvervit, Lead blågrå, Copper orange, Bronze guldgul, Gold djupgul, Silver vit, Pig Iron gulorange, Steel och
  Wrought Iron vitgul, Glass grönaktig, Slag svartgrön med ljusa slingor, Basalt mörkröd, Obsidian djuplila.
- **Svalnar synligt:** vitt → gult → orange → mörkrött → grått skal under stelningstiden, i rännor, formar, bord och bassänger.
- **Rinner:** ytan rullar i flödesriktningen (som dagens rännor), strålen faller med rullande textur och stänker gnistor
  där den slår ner.
- **Bränner:** den som rör en ränna, form, bord eller bassäng med smälta i, eller står i en stråle, tar eld (som lava, 4 skada). Bronze
  Tongs och stålrustningen skyddar inte.

---

## 7. Progression och koppling

- **Plats:** efter **Steel** (cementering). Man behöver 5 Steel Ingots till den minsta ugnen, Iron Bellows-värme till Silica
  Brick och ett vattenhjul. Det är stålålderns stora mål och bron till ångåldern: Coke, gjutjärn och stora gjutningar är
  ångålderns material.
- **Det den ersätter eller gör snabbare:**

| I dag | Med High Furnace |
|---|---|
| Bloomery: 4 malm → upp till 3 tackor, varje lupp hamras | 1 malm → 300 mB tackjärn → Finery → 1,5 smidesjärn |
| Cementation: 3 tackor på 6 min + 6 slag var | 1 Pig + 2 Wrought → 3 Steel direkt i pölen, tackorna är färdigt stål |
| Smelting Furnace: 40 enheter brons, 1,25× | 80–288 enheter, 2×, ingen Poor Bronze |
| Guld/silver/bly som tackor ur kilnen | Gjutbara, i block och nuggets |
| Clay Molds: 5 gjutningar, högst 1000 mB, Rough Cast | Kokiller som aldrig spricker (4 tackor per hällning), sandgjutna färdiga huvuden för ⅔ av metallen, gjutningar upp till 4000 mB |
| – | Glas, block, Bell, Cauldron, Anvil, Gear, Pot, Slag Stone, Cast Basalt |

- Smelting Furnace, Bloomery och cementering finns kvar som vägen fram hit. Clay Kiln, Smithing Hearth och Steel Casting
  Table ändras inte.
- **Advancements** (fliken: se Öppna frågor), från koden till spelare inom 16 block där det gäller ugnen:
  - *Silica Brick*;
  - *High Furnace* (goal, ugnen formas);
  - *Wind of the River* (en eldstad får 16 blåst);
  - *White Heat* (1450°);
  - *Layer Cake* (3 olika smältor i pölen samtidigt);
  - *Pour* (första strålen i en Molding Flask eller på ett Foundry Table);
  - *Sow and Pigs* (en hel tappning i en Pig Bed);
  - *Never Cracks* (första Iron Ingot Mold);
  - *Pig Iron*;
  - *Walloon Forge* (Finery Hearth);
  - *Cast Steel* (challenge, Steel ur pölen);
  - *Glassmaker*;
  - *Slag Stone*;
  - *Bell Founder* (challenge);
  - *Salamander* (hidden);
  - *Coke* och *Hot Blast* (challenge, 1800°).
- **Byar och stammar:** inget i de första faserna. Senare, valfritt: järnålderns smed köper Pig Iron och säljer Wooden Patterns på nivå 5,
  och en ruin, **Abandoned Bruk** (en raserad masugn med slaggvarp), visar i världen att det här finns.

---

## 8. Faser

| Fas | Innehåll | Klart när |
|---|---|---|
| **0** | Grund i koden: `Metal` får de nya smältorna (melt, densitet, färg, flagga för icke-metall); `FurnaceMelt` bryts ut till en `MeltPool` med lager och legeringsrecept (Smelting Furnace använder den gamla regeln); `ChannelFlow` tar flera ränntyper (kapacitet, högsta temperatur) och nya portar; `MoltenRenderer` får färger per smälta och svalningsfärger. | Smelting Furnace-testerna (bygga, smälta, tappa, rännor, ösa) går igenom oförändrade; `MeltPool.selfCheck` (lager, legering 9:1 och 1:2, inget skapas, rester blir kvar). |
| **1** | Block och struktur: Crushed Quartzite, Silica Brick(s), Steel Hoop, Banded Furnace Casing, Bronze Tuyere Nozzle-formen, Firebox, High Furnace Front, Taphole. Storlekssökning, felmeddelanden, `HEAT`-glöd. | Gametest `high_furnace_shape`: 2×2×5, 2×3×6 och 3×3×8 formas, 1 brett, 4 brett, 4 lager och 9 lager inte, ett saknat väggblock, ett block i det inre, fel golv och för få eldstäder ger rätt meddelande, hörn behövs inte, en olastad chunk pausar. Bild: liten och stor ugn, dag och natt. |
| **2** | Värme: Great Bellows i `WaterPower.walk`, **delad kraft**, eldstadsbränsle, målformeln, uppvärmning/storleksfaktor, uttorkning, regn. Ugnens GUI: termometer till 1800°, eldstadslista. | Gametest `high_furnace_heat`: minsta ugnen med B = 16 → 1600° och B = 13 → 1450°; största med tre bälgar på 16 → 1467° och fyra → 1600°; två hjul på samma axel räknas ihop till högst 16; hjulet stoppas → sjunker, två bälgar på ett hjul får 8 var, sågen ensam får 16 (regression `sawmill_*`), uttorkning håller 600°. Bild: lågan i toppen på natten, bälgarna i rörelse. |
| **3** | Smältning: platser med mätare, intag genom toppen och GUI, mB-lagren, Copper/Tin/Bronze/Gold/Silver/Lead, 2× malm, auto-Bronze, val av lager, GUI-lagerkolumnen. | Gametest `high_furnace_melt`: 9 + 1 Crushed Ore → 2000 mB Bronze, 10 Copper + 1 Tin lämnar ett Copper-lager, platserna väntar när det är för kallt, nuggets ger 22 mB, kapacitet 400 mB per block. Bild: GUI med fyra lager. |
| **4** | Rännor och hällning: Pouring Spout (stråle, på Taphole och i rännände), Refractory Trough, Trough Gate, rättvis delning, värmeförlust (räckvidd), lerrännor som spricker av > 1100°, Clay Mold under pipen. | Gametest `high_furnace_pour` och `trough_flow`: inget mB försvinner eller skapas längs pip → ränna → form; en T delar lika; en stängd lucka stoppar; stål fryser efter räckvidden; en Clay Channel med stål går sönder och metallen förloras; brons i lerrännor beter sig som förut. Bild: strålen och en kaskad av rännor med luckor. |
| **4b** | Gjutningens nya nivå: Coal Dust, Casting Sand, Pattern Bench och Wooden Patterns, Molding Flask (slå ur, bränd sand tillbaka), Pig Bed (Sand Runner, Pig Mold), Foundry Table, Foundry Basin (block), Iron Ingot/Plate/Nugget Mold. | Gametest `foundry_casting`: Iron Ingot Mold ger 4 tackor av 800 mB och spricker aldrig; flaskan ger ett färdigt Bronze Pickaxe Head av 400 mB och 3 Burnt Casting Sand; Pig Mold 400 mB → 2 tackor; bordet halverar stelningen; ett block är exakt 1800 mB. Bild: en Pig Bed som fylls och ett bord med flaska. |
| **5** | Järn och stål: järnmalm → Pig Iron + Slag, flux, reduktionskol, Finery Hearth, Pig + Wrought → Steel, Blister Steel → Steel, Slag Stone. | Gametest `high_furnace_iron`: 4 rostade malm + 1 flux → 1200 mB Pig Iron + 100 mB Slag, utan flux 800 + 300, 1 Pig + 2 Wrought → 3 Steel-tackor som smids som vanliga. Bild: tappning av tackjärn i en Pig Bed. |
| **6** | Stora gjutningar och höga värmen: Glass Batch → Glass/Pane, Sight Glass, Cast Basalt, Bronze/Steel Block, Loam Mold och Strickle Board (Bell, Cauldron, Great Kettle), Anvil och Cast Iron Gear/Pot i flaskan, Furnace Salamander, brännskador. | Gametest: glas, bell och salamander (hälften av smältan, smälter tillbaka). Bild: ugnen med Sight Glass och synliga lager, en gjuten Bell. |
| **7 (valfri)** | Coke Oven (bikupeugn av Silica Bricks: 16 Coal → 12 Coke på 10 min, utan bränsle), Coke i eldstäderna, Hot Blast Stove (bredvid en Great Bellows, +100°), Obsidian, Steel Ladle. | Gametest: 1800° med Coke + Hot Blast. Bild: koksugnen glöder. |

Fas 1 och 0 kan byggas samtidigt av två agenter i var sin worktree (olika filer). Övriga faser bygger på varandra.

**Varje fas, som alltid:** versionshöjning; `docs/ages/` uppdateras med alla items, recept och vad som ändrats; registry-anteckningar
(sv/en) och `registry.html`; advancements; `progression.json` när progressionen ändras; hela generatorkedjan (ny
`gen_high_furnace.py` efter `gen_smelter.py`, texturer i `gen_high_furnace_textures.py`); `runGameTestServer` en gång efter
sista dataändringen; bilder skickas.

### Prestanda

- **En tickande BE per ugn** (Front). Väggar, golv och eldstäder är vanliga block. Eldstäderna har bara blocktillstånd för
  bränsle och glöd som Front sätter (bränsle och blåst sparas i Front). Bälgarna läser kraften från axeln som sågen gör.
- **Strukturkoll** var 40:e tick: högst 9 + 96 + 72 ≈ 180 blockläsningar, och vid väggändring nästa tick (inte direkt, så att
  en bruten vägg inte ger hundra kontroller).
- **Platser:** upp till 72 stackar i en loop per tick. Det är billigt. Klienten synkas bara när något ändrats, högst var 10:e tick (som
  Furnace Tap).
- **Rännor:** `ChannelFlow.explore` byggs om varje steg i dag. Med större nät **cachas nätet** och byggs om bara när ett block
  i det ändras. `MAX_CHANNELS` höjs till 96. Räckvidden (värmeförlusten) håller ändå de flesta nät korta.
- **Rendering:** ett BER på Front ritar hela pölen (ett par plan per lager, render-boxen täcker det inre), högst 16
  flytande föremål; strålar ritas av pipens BER; partiklar bara inom 32 block och begränsade per tick. Ingen ljuskälla i
  det inre (smältan är fullbright men lyser inte upp världen); Front, pipar och eldstäder lyser via blocktillstånd.

### Risker

- **Delad kraft ändrar `WaterPower`.** Den som i dag har flera maskiner på en linje (bara sågen finns) märker det. Testas
  med `sawmill_*`.
- **`Metal` och blocktillstånden:** `ClayMoldBlock.METAL` får **inte** de nya smältorna (antalet tillstånd växer med
  former × fyllnad × metall × slitage). Flaska, bord, bassäng och Pig Mold sparar smältan i en BE i stället.
- **Konserveringen** är det som gör systemet tryggt (inget kan dubbleras). Varje flytt pip → ränna → form → bord → bassäng
  måste dra av exakt det den lägger till. Självtest och gametest i varje fas.
- **Kolåtgång:** största ugnen med fyra eldstäder på full blåst äter ca 12 Charcoal per minut plus reduktionskolet. Om kolmilan inte hinner med
  sänks förbrukningen i fas 5, inte värmen.
- **Myntekonomin:** 1,5× guld och silver. Följs upp när karavanerna handlar.
- **Omfång:** fas 6 och 7 är många småsaker. De kan skjutas utan att ugnen blir sämre.

---

## Beslut (Claude, kan ändras)

1. **Namnet High Furnace**, inte Blast Furnace (vanillas borttagna block). Gjutbordet heter **Foundry Table** och
   bassängen **Foundry Basin**, eftersom "Steel Casting Table" redan finns och Tinkers namn inte ska kopieras.
2. **Fast bränsle + vattenhjulsdrivna bälgar**, inget flytande bränsle i den här åldern (avsnitt 2). Handbälgar gör inget.
3. **Kraften på en axellinje delas** mellan maskinerna.
4. **Variabel storlek med verkligt tak:** inre 2–3 × 2–3 × 5–8 (ugnen 6–9 m hög, rast högst 3 m), 400 mB per inre block, en
   eldstad per 24 inre block (högst 4), blåsten räknas per volym, hörn valfria, öppen topp.
5. **Malm 2×** (ädelmetall 1,5×): Crucible < Smelting Furnace < High Furnace, och 2× Bloomery för järn.
6. **Exakta legeringsrecept** (9:1 brons, 1:2 stål) och ingen ny Poor Bronze i den här ugnen.
7. **Lagren efter densitet**, slagg överst; standard är bottenlagret, klick i GUI:t väljer.
8. **Järn och stål gjuts bara till tackor och plattor** (och Steel Block och Anvil). Verktyg av järn och stål smids som förut.
13. **Ny gjutnivå:** Pig Bed, Molding Flask med Wooden Patterns, permanenta gjutjärnskokiller, Foundry Table/Basin och
    Loam Molds. Clay Molds blir kvar för brons men tar inte smälta över 1100°.
14. **Rännor:** Refractory Trough (200 mB, all hetta, värmeförlust efter avstånd), Trough Gate och rättvis delning;
    lerrännor spricker och förlorar metallen med järn och stål. All logik i `ChannelFlow` behålls.
9. **Tackjärn → smidesjärn i Finery Hearth**; ingen Bessemer-konverter (ångåldern).
10. **Smältan sparas i Front** när ugnen bryts; bryts Front blir hälften en Furnace Salamander.
11. **Glas och vanillas Glass** får sitt enda recept här (Glass Batch vid 1400°); ingen glashytta just nu.
12. **Coke** som valfri sista fas här, trots att järnålderns dokument sa "ångåldern" (texturerna `coked_coal` och `coal_dust`
    finns redan). Utan Coke når ugnen ändå allt utom Obsidian.

## Användarens beslut (2026-11-24)

1. **Stålet blir en egen ålder**, Steel Age, med en egen advancement-flik och `docs/ages/04-steel-age.md`. High Furnace,
   sågverket och stålköket hör dit.
2. **Smide står kvar:** verktygshuvuden av järn och stål smids på städet. Järn- och stålplåtar får gjutas i en järnform.
3. **Maskiner på samma axel delar kraften**, så större ugnar behöver fler hjul (sågverket påverkas också).
4. **Koks byggs nu** som fas 7 (Coke Oven och Hot Blast Stove), inte först i ångåldern.

Kvar att kontrollera vid bygget: om vanillas Bell och Cauldron är låsta i dag, om Bronze Bucket kan bära lava, och
vanillas Anvil (järnålderns dokument säger ångåldern; gjuts inte från stål utan att användaren tillfrågas).

## Byggt

- **Fas 0 (v0.200.0):** `Molten` (egen enum för ugnens smältor, i densitetsordning; `Metal` och lerformarnas tillstånd är
  orörda, avvikelse från planen som sa "Metal får de nya smältorna"), `MeltPool` (lager i mB, legering 9:1 och 1:2 med 20 mB
  per tick, rester ligger kvar, `selfCheck` vid start). `ChannelFlow` och `MoltenRenderer` ändras först i fas 4 där de behövs.
- **Fas 1 (v0.200.0):** alla byggblock i tabellen utom Sight Glass och Pouring Spout; `HighFurnaceShape.find` med
  felmeddelandena; `HEAT`-glöd på nedersta ringen. Test `high_furnace_shape`.
- **Fas 2 (v0.200.0):** Great Bellows (`WaterPower.Driven`), delad kraft (`WaterPower.share`), eldstadsbränsle, målformeln,
  storleksfaktor, uttorkning, regn, skärmen (utan lagerkolumn). Lågor/gnistor/rök från toppen och glöden i schaktet
  (`client/HighFurnaceRenderer`). Test `high_furnace_heat`, `waterpower_shared`, `waterpower_shared_bellows`.
