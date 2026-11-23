# Kyla (cold)

Djur för snölandet, isen, taigan och de högsta topparna: **Polar Bear** (ersätter vanilla-isbjörnen), **Arctic Fox**,
**Snow Leopard**, **Siberian Tiger**, **Snowy Owl**, **Black Grouse**, **Lammergeier** och **Pika** – och brunbjörnens
**honor med ungar på våren**, som är extra farliga.
Kod: `wildlife/cold/` (ColdFauna, ett djur per klass, ColdTest), klient: `client/wildlife/cold/`.
Data: `scripts/gen_fauna_cold.py`, texturer: `scripts/gen_fauna_cold_textures.py` (föremål, block), som till sist kör
`scripts/gen_cold_models.py`: varje djurs egen modell (`ColdGeometry.java`, genererad) och dess hud målade ur samma
tabell, plus höstackens texturer. Rörelserna är kroppstypens: björnens (BrownBearModel/BearCubModel), hunddjurens
(PredatorModel), leopardens (LeopardModel) och skogsfåglarnas (ForestBirdModel); pikan har en egen PikaModel, byggd
dubbelt så stor och ritad i halv storlek så att detaljerna syns.

Alla lämnar ett Carcass som styckas med kniv (Butchery) och kan hängas på en Hanging Rack. Mängder nedan gäller
flintkniv; på Hanging Rack faller allt i första omgången utan slumpslag och köttet blir Haunch/Ribs/Loin.

## Pälsar på torkställningen

Råa pälsar (Raw Polar Bear/Arctic Fox/Snow Leopard Pelt) högerklickas på en vanlig **Drying Rack**-plats och torkar som
en Scraped Hide (~3 min). Det är samma system som för de kalla jägarnas pälsar (`ColdhuntersFauna.CURES`, PeltCuring):
tar man ner den torra pälsen med tom hand får man den färdiga pälsen, slås ställningen sönder får man pälsen tillbaka.

## Polar Bear (isbjörn)

- **Var:** snowy plains, ice spikes, snowy beach, Glacial Moraine, frozen ocean och deep frozen ocean (på isen). Vikt 1,
  1–2 (den andra är en unge). **Vanilla-isbjörnen finns inte längre:** den är borttagen ur alla biomers spawnlistor
  (`remove_vanilla_polar_bears.json`), och en vanilla-isbjörn som ändå dyker upp (gamla världar, spawnägg, kommandon)
  byts direkt mot moddens (en unge mot en unge).
- **Beteende:** brunbjörnens kropp och strid (reser sig och slår, flyr aldrig, en hona försvarar sin unge), men en
  **jägare**. Den väljer ett byte inom **24 block**: helst en **Seal**, annars en kalv av Walrus, Reindeer eller Musk Ox.
  Finns inget byte tar den en spelare inom **20 block** (8 om han smyger) som den ser. Den **smyger** in långsamt med
  huvudet lågt tills den är 5 block ifrån, sedan anfaller den. Simmar bra (följer sälen ut i vattnet). Vaken dag som natt
  och går aldrig i ide. När den fällt ett byte **äter den i 10 s** och lämnar resten (kadavret märks som rester, den går
  inte tillbaka) – det lever fjällräven på.
- **Stats:** 70 liv, 9 skada, fart 0,3, knockback-motstånd 0,7. Tjock hud: kräver Tusk Dagger eller bättre.
- **Ljud:** isbjörnens egna läten.

| Omgång | Vad | Chans | Antal |
|---|---|---|---|
| 1 | Raw Polar Bear Pelt | 100 % | 1 |
| 2 | Raw Polar Bear | – | 4–6 |
| 2 | Bear Fat | 90 % | 2–4 |
| 2 | Bear Claw | 60 % | 1–3 |
| 2 | Polar Bear Liver | 100 % | 1 |
| 2 | Sinew | 60 % | 1–2 |

**Produkter:**
- Raw Polar Bear (hunger 3) → Campfire 20 s / rökugn → **Cooked Polar Bear** (hunger 8, mättnad 0,8). Torkas till Game Jerky.
- **Polar Bear Liver:** giftig av A-vitamin. Äter man den får man Nausea (20 s), Poison II (15 s) och Hunger II (30 s).
- Raw Polar Bear Pelt → torkställning → **Polar Bear Pelt**.
- **Polar Bear Cloak:** 2 Polar Bear Pelt + Leather + Sinew (crafting). Bärs i Cloak-slotten, rustning 3, isolerar 25 %
  mot kyla (`#mechanical:fur_insulation`), med en vit björnfäll som cape.
- **Polar Bear Rug:** Polar Bear Pelt + Bone – platt dekoration som Bear Rug.

## Arctic Fox (fjällräv)

- **Var:** snowy plains, ice spikes, snowy beach och Glacial Moraine. Vikt 4, 1–2.
- **Beteende:** liten och rund med korta ben, små runda öron och tjock svans. **Följer isbjörnar:** finns en vuxen
  isbjörn inom 32 block håller räven sig inom ungefär 10 block från den. **Äter rester:** ett kadaver inom 24 block som
  ingen vaktar (ingen spelare inom 10 block, ingen isbjörn precis vid det, inte buret, släpat eller upphängt) äter den
  upp på 8 s, och en Bone Pile blir kvar. Jagar också lämlar. Skygg för spelare.
  **Pälsen följer kalendern:** snövit från sista månaden på hösten till slutet av vårens första månad (dag 240–29 på
  året), gråbrun ovan och ljus under resten av året. Bytet sker för levande rävar inom 5 s.
- **Stats:** 8 liv, 2 skada, fart 0,32.

| Omgång | Vad | Chans | Antal |
|---|---|---|---|
| 1 | Raw Arctic Fox Pelt | 90 % | 1 |
| 2 | Raw Arctic Fox | – | 1 |

**Produkter:** Raw Arctic Fox (1) → **Cooked Arctic Fox** (3 / 0,5); torkas till Small Game Jerky. Raw Arctic Fox Pelt
→ **Arctic Fox Fur**. **Arctic Fox Hat:** 3 Arctic Fox Fur + Sinew: hjälm, rustning 1, isolerar 25 %.

## Snow Leopard (snöleopard)

- **Var:** frozen peaks, jagged peaks och stony peaks. Vikt 1, ensam – syns sällan.
- **Beteende:** jagar som Cave Lion (smyger hukande, fryser när bytet tittar, kastar sig fram), men **när som helst på
  dygnet**: Ibex framför allt, Chamois, Mouflon, Marmot och harar. En spelare är byte bara **på natten**; på dagen håller
  den sig undan folk på dubbelt så långt avstånd som annat vilt. Rädd för eld och facklor (Predator). Landar på fötterna:
  de första 12 blocken av ett fall gör inte ont. Släpar bort ett färskt byte.
- **Utseende:** mindre och lägre än leoparden (ca 0,7 block i manken), tjock rökgrå päls med öppna mörka rosetter,
  små runda öron, kraftiga tassar och en lång, tjock svans med mörk spets.
- **Stats:** 26 liv, 5 skada, fart 0,32.
- **Ljud:** ozelotens läten, låga.

| Omgång | Vad | Chans | Antal |
|---|---|---|---|
| 1 | Raw Snow Leopard Pelt | 100 % | 1 |
| 2 | Raw Snow Leopard | – | 1–3 |
| 2 | Sinew | 50 % | 1–2 |
| 2 | Bone | 50 % | 1–2 |

**Produkter:** Raw Snow Leopard (2) → **Cooked Snow Leopard** (6 / 0,7); torkas till Game Jerky. Raw Snow Leopard Pelt →
**Snow Leopard Pelt**.
- **Snow Leopard Boots:** Leather Boots + 2 Snow Leopard Pelt + Sinew. Rustning 1, isolerar 25 %, och mjuka sulor: de
  första **4 blocken** av ett fall gör inte ont.
- **Snow Leopard Rug:** 2 Snow Leopard Pelt + Bone.

## Siberian Tiger (sibirisk tiger)

- **Var:** bara snowy taiga. Vikt 1, ensam – taigans sällsynta spetsrovdjur.
- **Beteende:** jagar som Cave Lion (smyger hukande, fryser när bytet tittar, kastar sig fram och släpar bort bytet)
  i skymningen och på natten och sover på dagen: Red Deer, Reindeer, Wild Boar, Roe Deer, älgkalvar, stamfolk – och
  spelare. Rädd för eld och facklor (Predator). Egen modell: tung framdel, stort huvud med vita kinder och vita fläckar över ögonen, ljusare orange än
  djungeltigern, svarta ränder som löper runt kroppen, ringad svans med svart spets.
- **Stats:** 50 liv, 8 skada (språnget 8), fart 0,31, knockback-motstånd 0,6.

| Omgång | Vad | Chans | Antal |
|---|---|---|---|
| 1 | Raw Tiger Pelt | 100 % | 1 |
| 2 | Tiger Meat | – | 3–5 |
| 2 | Sinew | 60 % | 1–2 |
| 2 | Bone | 60 % | 1–3 |

Tjock hud (Tusk Dagger eller bättre). **Produkterna är djungeltigerns** ([jungle.md](jungle.md)): samma Tiger Meat och
Raw Tiger Pelt, som fettgarvas till Tiger Pelt och blir Tiger Pelt Mantle och Tiger Rug där.

## Snowy Owl (fjälluggla)

- **Var:** snowy plains, ice spikes och Glacial Moraine. Vikt 2, ensam.
- **Beteende:** berguvens sätt (ljudlös flykt, hoande, sitter vid bytet en stund efteråt, se forestbirds.md) men den
  **jagar på dagen** – den arktiska sommaren har ingen natt – lågt över snön, och slår **lämlar** framför allt, ripor
  och harungar. Natten sitter den ute på marken eller en sten, inte i träd.
- **Drop (inget kadaver):** Feather 0–1, **Snowy Owl Feather** 1–3. Snowy Owl Feather fungerar som Owl Feather på
  Fletching Table: Flint + Stick + fjädern → 4 **Silent Arrow**.

## Black Grouse (orre)

- **Var:** taiga, old growth pine/spruce taiga, Pine Heath och Willow Fen (myrarna). Vikt 5, 2–4.
- **Beteende:** tjäderns sätt (Capercaillie: korta flygturer, deaf medan han "slipar"), men tuppen **spelar bara på
  våren**, i gryningen på den öppna myren, och oftare. Tuppen är blåsvart med vitt vingband, röda ögonbryn och
  lyrformad stjärt; hönan är brunspräcklig. Mindre än tjädern.
- **Kadaver:** plockas hel (Plucked Black Grouse: Campfire → 2 Cooked Black Grouse, eller kniv → 2 Raw Black Grouse)
  eller styckas: Raw Black Grouse 1–2; första omgången Feather 1–3 och **Black Grouse Lyre Feather** 60 % (1–2).
  Lyre Feather fungerar som Capercaillie Tail Feather: Flint + Stick + fjädern → **6 Arrow**.
- Raw Black Grouse (2) → **Cooked Black Grouse** (6 / 0,8); torkas till Dried Fowl.

## Lammergeier (lammgam)

- **Var:** frozen, jagged och stony peaks, windswept hills och Slate Highlands. Vikt 1, ensam.
- **Beteende:** kretsar högt (SoaringBird). Ser den ett **Bone** på marken eller en **Bone Pile** (det rovdjuren lämnar
  av ett kadaver) inom 24 block dyker den ner och tar det i klorna – en Bone Pile ger två ben och försvinner. Sedan
  stiger den minst 12 block över marken och **släpper benet**: det spricker mot marken och lämnar **Bone Marrow** och
  1–2 Bone Meal (ett ben per släpp).
- **Drop (inget kadaver):** Feather 1–2, **Lammergeier Feather** 1–3. Lammergeier Feather fungerar som Eagle Feather:
  Flint + Stick + fjädern → 4 **Eagle-Fletched Arrow**.
- **Bone Marrow:** hunger 4, mättnad 1,0 – fet och mättande. Förskäms på 1 dag.

## Pika (piphare)

- **Var:** stony peaks, meadow, windswept hills och Slate Highlands. Vikt 5, 2–3. Lämmelns kropp, gråbrun.
- **Beteende:** liten och rund, piper skarpt när någon kommer nära, skygg. **Gör hö** på sommaren och hösten: då och då
  bär den en tugga gräs till sin **Pika Hay Pile** som växer ett lager (högst 4), eller börjar en ny bredvid – aldrig
  fler än 3 högar inom 8 block.
- **Pika Hay Pile:** en liten hög av slaget gräs och torkade blommor, i avsatser med strån som sticker upp, högre för
  varje lager. Slås sönder och ger 1 **Pika Hay** per lager. Pika Hay är foder (Feeding Trough tar det: det är
  ko-, får-, get- och hästfoder) och 9 st → Hay Bale.
- Styckas: Raw Pika (1) → **Cooked Pika** (2 / 0,4); torkas till Small Game Jerky. 3 liv.

## Brown Bear: honor med ungar på våren

En vuxen brunbjörn (och Cave Bear, Polar Bear) med en unge av sitt slag inom 16 block är på **våren** en vaktande hona:
den märker och anfaller spelare på **dubbla avståndet** (24 block, 10 om man smyger) och **tröttnar aldrig** på jakten
(annars saktar en björn in efter 5 s). Resten av året är hon som vanligt.

## Gemensamt

- Rått kött förskäms på 1 dag, tillagat på 3; allt ruttnar till Rotten Flesh; rått kött kan rökas och torkas.
- Prestationer: Wilderness – White Bear, Ice Walker, White Rug, Too Much of a Good Thing, Camp Follower, Snow-White Hat,
  Grey Ghost, Soft Landing, Grey Rosettes, White Wings, Lyre Tail,
  Bone Breaker, Bearded Vulture, Haymaker; Food – Arctic Fare, Marrow Feast; Hunting – Polar Bear, Snow Leopard och
  Siberian Tiger (Predators), Arctic Fox och Pika (Small Game), Black Grouse (Fowl).
- Test (`ColdTest`): spawnlistor per biom och att vanilla-isbjörnen byts ut; isbjörnen väljer en säl vid iskanten som
  byte; fjällräven tar sig fram till en isbjörn och äter dess rester; pälsens färg efter kalendern; snöleoparden väljer
  en stenbock som byte; pikans höhögar (säsong, lager, högst tre); lammgamen tar en Bone Pile och släpper två ben som
  blir Bone Marrow; björnhonan vaktar bara på våren.
- Bilder: `docs/shots/fauna/cold_*.png` (ColdShots, en klientharness som bara körs med `coldshots.flag` i run/), med
  närbilder i ögonhöjd `cold_<namn>_q.png` (tre kvart) och `cold_<namn>_front.png`, och styckade kroppar
  (`cold_polar_bear_carcass.png`, `cold_siberian_tiger_carcass.png`).
  [polar_bear](../shots/fauna/cold_polar_bear.png), [polar_bear_cub](../shots/fauna/cold_polar_bear_cub.png), [arctic_fox_winter](../shots/fauna/cold_arctic_fox_winter.png), [arctic_fox_summer](../shots/fauna/cold_arctic_fox_summer.png), [snow_leopard](../shots/fauna/cold_snow_leopard.png), [siberian_tiger](../shots/fauna/cold_siberian_tiger.png), [snowy_owl](../shots/fauna/cold_snowy_owl.png), [black_grouse](../shots/fauna/cold_black_grouse.png), [black_grouse_hen](../shots/fauna/cold_black_grouse_hen.png), [lammergeier](../shots/fauna/cold_lammergeier.png), [pika](../shots/fauna/cold_pika.png).
