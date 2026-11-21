# Under jord (underground)

Djuren och väsendena i grottornas mörker: **Cave Hyena**, **Cave Bear**, **Giant Centipede** och folktrons **Vittra**,
**Troll** och **Lindworm**. Steg 3 i [underground-plan.md](../underground-plan.md).
Kod: `wildlife/underground/` (Underground = mörkerregeln och spawnern, UndergroundFauna, GiantCentipede, Vittra, Troll,
Lindworm), klient
`client/wildlife/underground/`. Data: `scripts/gen_fauna_underground.py`, texturer: `scripts/gen_fauna_underground_textures.py`.

Cave Hyena och Cave Bear är **samma arter** som i bergsfaunan ([mountain.md](mountain.md)): samma kadaver, produkter och
recept, men de spawnar nu också djupt nere i grottorna. Giant Centipede är ny.

## Mörkerregeln (gemensam)

`Underground.canSpawn(level, pos, minY, maxY)`: en plats duger om
- `minY <= y < maxY` (djupbandet),
- **ljusnivå 0**: inget blockljus och inget himmelsljus,
- två block luft utan vätska och ett fast golv.

**Ljus är försvaret:** en fackla, lykta eller lägereld som lyser upp platsen stoppar alla spawns där.

Spawnern: var 10:e sekund har varje spelare som är i en grotta (under y 50 utan himmelsljus) 30 % chans att en grupp
kommer 16–40 block bort (upp till 12 block upp eller ner), inte inom 12 block från någon spelare. Djupet på platsen väljer
djur (viktat bland banden som täcker det djupet). Är redan bandets maxantal av djuret inom 64 block kommer inga fler.
Djur från spawnern kan försvinna när ingen spelare är i närheten.

| Djup | Djur | Vikt | Grupp |
|---|---|---|---|
| y 40 till 0 | Cave Hyena | 3 | 3–5 |
| y 40 till 0 | Cave Bear | 1 | 1 (aldrig fler inom 64 block) |
| y 0 till −30 | Giant Centipede | 4 | 1–2 |
| y 0 till −30 | Vittra | 3 | 2–3 |
| y −30 till −64 | Troll | 20 | 1–2 |
| y −45 till −64 | Lindworm | 1 | 1 (aldrig fler inom 64 block) |

Rovdjur (Predator: Cave Hyena, men också vargar och lejon som går ner) räknar en grotta som **natt**: de sover inte och
jagar dag som natt där nere. Cave Bear ligger i dvala i grottan tills någon kommer nära (som förut).
Grottorna blir kallare med djupet (`Temperature.cave`), se steg 1 i planen.

Fler väsen läggs till med `Underground.addBand(typ, minY, maxY, vikt, min, max)`.

## Cave Hyena (grotthyena)

Allt som i [mountain.md](mountain.md#cave-hyena-grotthyena). Under jord: flockar på 3–5 i y 40 till 0, och eftersom det
alltid är natt i grottan trakasserar de spelaren (flock på 3+) när som helst. Rädda för facklor som på ytan.

| Omgång | Vad | Chans | Antal |
|---|---|---|---|
| 1 | Raw Hyena Pelt | 80 % | 1 |
| 2 | Hyena Meat | – | 1–2 |
| 2 | Hyena Fang | 60 % | 1–2 |
| 2 | Bone | 60 % | 1–2 |
| 2 | Sinew | 40 % | 1 |

**Produkter:** Raw Hyena Pelt + kniv + fett → Hyena Pelt (2 + Bone → Hyena Rug; + kniv → 2 Leather). Hyena Fang → 4 Barbed
Arrow på Fletching Table. Hyena Meat → Cooked Hyena Meat.

## Cave Bear (grottbjörn)

Allt som i [mountain.md](mountain.md#cave-bear-grottbjörn): 90 liv, 11 skada, ligger i dvala tills den väcks. Under jord:
ensam i y 40 till 0. Nytt: kadavret ger nu också **Bone**.

| Omgång (Tusk Dagger eller bättre) | Vad | Chans | Antal |
|---|---|---|---|
| 1 | Raw Cave Bear Pelt | 100 % | 1 |
| 1 | Raw Hide | 40 % | 1 |
| 2 | Cave Bear Meat | – | 5–8 |
| 2 | Cave Bear Skull | alltid | 1 |
| 2 | Bear Fat | 90 % | 2–3 |
| 2 | Bear Claw | 60 % | 1–3 |
| 2 | Sinew | 70 % | 1–2 |
| 2 | Bone | 70 % | 2–4 |

**Produkter:** pälsen torkas på hög Drying Rack → Cave Bear Pelt (Fur Coat, Fur Leggings, Cave Bear Rug); skallen är en
trofé; Bear Fat garvar och blir Tallow; Bear Claw → Barbed Arrow; Bone → Bone Arrow, benmjöl.

## Giant Centipede (jättegrottfoting)

- **Var:** y 0 till −30, bara i totalt mörker. 1–2 åt gången.
- **Beteende:** klättrar på väggar som en spindel. Utan byte kryper den ibland **rakt upp för en vägg till grottaket**
  (2–6 block upp) och **hänger där upp och ner** och väntar. Går en spelare in under den (inom 1,5 block i sidled) släpper
  den och faller över honom, sedan biter den. Jagar spelare den ser inom 12 block (5 om man smyger). Bettet ger
  **Poison I i 5 s**. **Hatar ljus:** den går inte på den som håller en fackla (allt i `mechanical:scares_predators`),
  backar undan om den redan jagade honom, och släpper inte från taket över honom. Flyr inte annars. Ingen fallskada, sover
  aldrig. Släpper taket när den skadas, eller efter en lång stund.
- **Stats:** 16 liv, 4 skada, rustning 4 (kitinet), fart 0,3. Blöder bara under 30 %.
- **Modell:** platt huvud med långa antenner och två krokiga giftklor, 8 plåtade segment med ett benpar vardera (benen
  böljar framifrån och bakåt), stjärt med två cerci. Mörkt rödbrun med orange ben.
- **Ljud:** silverfiskens prassel, spindelns skrik när den skadas.

| Omgång | Vad | Chans | Antal |
|---|---|---|---|
| 1 | Centipede Chitin | 100 % | 2–4 |
| 2 | Raw Centipede | – | 1–2 |
| 2 | Centipede Fang | 70 % | 1–2 |
| 2 | Centipede Venom Sac | 50 % | 1 |

En tusenfoting har inga ben (skelett): kitinet är både hud och skelett. På Hanging Rack faller ändå 2–3 Bone, som för
alla kroppar.

**Produkter:**
- Raw Centipede (hunger 2) → Campfire 20 s / smoker → **Cooked Centipede** (hunger 5, mättnad 0,6).
- **Chitin Vest:** 7 Centipede Chitin + Sinew (`C C` / `CSC` / `CCC`). Bröstharnesk, rustning 4, hållbarhet 12×,
  förtrollbarhet 10, lagas med Centipede Chitin. Den första riktiga rustningen före brons för den som vågar sig ner.
- **Centipede Fang:** Fletching Table + Stick + Feather → 4 Barbed Arrow.
- **Centipede Venom Sac:** 8 Arrow runt säcken → 8 Arrow of Poison; + Lard + Plant Fibre → 2 Snakebite Poultice (tar bort
  Poison, också centipedens eget gift).

## Folktrons väsen: Vittra, Troll, Lindworm

Andra halvan av steg 3. Kod: `Vittra`, `Troll`, `Lindworm` i `wildlife/underground/`, klient `FolkModel` (vittra och troll)
och `LindwormModel`. Data och texturer i samma generatorer som centipeden (`gen_fauna_underground.py`,
`gen_fauna_underground_textures.py`), andra halvan av filerna.

**Ljus är försvaret.** Vittra och troll släpper den som håller en fackla (allt i `mechanical:scares_predators`) eller står i
facklans sken (blockljus 7 eller mer, ungefär 7 block från en fackla: `Underground.inLight`) och backar undan. Står de själva
i sådant ljus går de därifrån. Ett upplyst läger eller en upplyst gruvgång är alltså trygg. I mörkret däremot är de farliga, och
djupare ner farligare. Lindormen bryr sig inte om ljus, men den spawnar som alla andra bara i totalt mörker. Ingen av de tre
flyr när den skadas (`Underground.Fearless`). De slåss eller backar efter egna regler.

### Vittra (de underjordiska)

- **Var:** y 0 till −30, 2–3 åt gången, bara i totalt mörker. Delar bandet med Giant Centipede (vikt 3 mot 4).
- **Beteende:** nästan blind, **jagar efter ljud**. Den som **hackar** ett block inom 24 block, **springer** inom 20 eller
  **går** (utan att smyga) inom 6 block blir hörd, och vittran kommer. Smyga och gå långsamt förbi fungerar, och en **Vittra
  Hood** dämpar allt. Beröringen ger **Mining Fatigue I i 10 s** (vittrans förbannelse över gruvarbetaren). Skyr ljus (se
  ovan). Sover aldrig och slåss när den skadas.
- **Stats:** 14 liv, 3 skada, fart 0,32 (snabb), ingen rustning.
- **Modell:** smal och grå, stort huvud med långa spetsiga öron och stora svarta ögon, en trasig svart man, ländskynke av
  mossbrunt. Ungefär 1,5 block hög. Ljud: allayns röst, mycket mörkare.

| Omgång | Vad | Chans | Antal |
|---|---|---|---|
| 1 | Vittra Hide | 90 % | 1–2 |
| 2 | Vittra Meat | – | 1–2 |
| 2 | Vittra Eye | 60 % | 1–2 |
| 2 | Bone | 60 % | 1–2 |

**Produkter:**
- **Vittra Hood:** 5 Vittra Hide (`HHH` / `H H`). Hjälm, rustning 1, lagas med Vittra Hide. **Ingen vittra hör den som bär
  den.**
- **Vittra Eye** + Glass Bottle → **Potion of Night Vision** (att se i grottorna).
- Vittra Meat (hunger 2) → Campfire 20 s / smoker → **Cooked Vittra Meat** (hunger 5, mättnad 0,6).
- Bone: som alla ben (benmjöl, Bone Arrow osv.).

### Troll

- **Var:** under y −30 (till −64), 1–2, bara i totalt mörker (vikt 20, tar nästan alla spawns där nere).
- **Beteende:** ser spelare inom 16 block (6 om man smyger) och går på dem. Långsam men slår **mycket hårt med stor
  knockback**. Skyr ljus (se ovan). **Blir sten i dagsljus:** står det under öppen himmel medan solen är uppe blir det två
  block **Troll Stone** där det stod, med ett stendamm och ett dovt ljud. Inget kadaver och inget annat blir kvar. Den som
  lockar upp ett troll i solen slipper slåss men får inga produkter.
- **Stats:** 80 liv, 12 skada, knockback 1,5, rustning 6, fart 0,24, knockback-motstånd 0,7. Blöder bara under 35 %.
- **Tjock hud:** styckas bara med Tusk Dagger, Troll Tusk Knife eller bättre.
- **Modell:** hopsjunket och massivt (ungefär 2,7 block), mossig grågrön vårtig hud, lurvig rygg, långa armar ner till
  knäna, stor näsa, betar, små gula ögon och kosvansen från sagorna. Ljud: ravagerns, lite mörkare.

| Omgång (Tusk-kniv eller bättre) | Vad | Chans | Antal |
|---|---|---|---|
| 1 | Troll Hide | alltid | 3–5 |
| 1 | Troll Hair | 80 % | 1–3 |
| 2 | Troll Meat | – | 4–6 |
| 2 | Troll Tusk | 70 % | 1–2 |
| 2 | Bone | 90 % | 3–5 |
| 2 | Sinew | 70 % | 1–3 |

**Produkter:**
- **Trollhide Coat:** 7 Troll Hide + Sinew (`H H` / `HSH` / `HHH`). Bröstharnesk, **rustning 6**, seghet 1, hållbarhet 20×,
  förtrollbarhet 9, lagas med Troll Hide.
- **Troll Tusk Knife:** Troll Tusk + Twine + Stick (`T ` / `WS`). Styckkniv i tusk-klass (som Tusk Dagger, ger mer hud): skär
  troll, lindorm och björnar. 220 användningar.
- **Troll Hair:** 2 → 3 Twine.
- Troll Meat (hunger 3) → **Cooked Troll Meat** (hunger 8, mättnad 0,8).
- **Troll Stone** (blocket ett troll blir i solen): bryts med hacka och ger sig själv. Dekoration och trofé.

### Lindworm (lindorm, boss)

- **Var:** under y −45, mycket sällsynt (vikt 1 mot trollens 20, aldrig mer än en inom 64 block). Bara i totalt mörker.
- **Beteende:** snabb och svår, en riktig boss med **boss-mätare**. Jagar spelare inom 32 block och bryr sig inte om facklor.
  Fyra attacker:
  - **Bett** (närstrid): 14 skada, **Poison II i 6 s**, kastar bakåt.
  - **Språng:** från 4–14 block kastar den sig mot bytet och slår in i det (10 skada, kastas långt). Var 4:e sekund.
  - **Ringling:** nära (inom 3 block) lindar den sig runt bytet i 3 s. Bytet hålls fast (Slowness V) och kläms med 4 skada i
    sekunden. Var 11:e sekund.
  - **Svansslag:** alla inom 4,5 block får 8 skada och kastas iväg. Var 7:e sekund.
  - **Raseri:** under halva livet blir den snabbare (0,4) och attackerar dubbelt så ofta.
- **Stats:** 260 liv, 14 skada, rustning 14, seghet 4, fart 0,32, knockback-motstånd 1, ingen fallskada. Blöder bara under
  25 %. 100 erfarenhet.
- **Skatten:** lindormen ligger på en skatt. När den dör sprids `mechanical:entities/lindworm_hoard` ut där den dog, förutom
  kadavret:

| Del | Vad |
|---|---|
| alltid | 3–8 Gold Ingot, 4–12 Gold Coin, 6–18 Gold Nugget |
| 3–5 ädelstenar | Garnet, Ruby, Sapphire, Topaz, Amber, Emerald, Diamond, Moonstone, ibland Flawless Ruby/Sapphire/Diamond/Emerald |
| ofta något sällsynt | Golden Apple, Gold Block, Golden Horse Armor, Name Tag, ibland Enchanted Golden Apple eller skivan Otherside |

- **Modell:** en vinglös drake med två framben, omkring 6,5 block lång: hornat huvud med långa käftar, 7 tjocka kroppsled
  med ryggkam, 4 smalare svansled och en piskspets. Mörkgröna fjäll med ljusare kanter, gyllene buk, gula ögon. Den ringlar
  i en våg när den rör sig, reser huvudet och gapar när den jagar, sträcker ut sig rak i språnget och lindar sig i en ring
  när den klämmer.
- **Tjock hud:** Tusk-kniv eller bättre.

| Omgång (Tusk-kniv eller bättre) | Vad | Chans | Antal |
|---|---|---|---|
| 1 | Lindworm Scale | alltid | 8–12 |
| 2 | Lindworm Meat | – | 6–10 |
| 2 | Lindworm Fang | alltid | 2 |
| 2 | Lindworm Venom Gland | 80 % | 1 |
| 2 | Lindworm Heart | alltid | 1 |
| 2 | Bone | alltid | 4–8 |

**Produkter:**
- **Lindworm Scale-rustning** (bättre än järn): Helmet 5 fjäll (`SSS` / `S S`), Chestplate 7 fjäll + Leather
  (`S S` / `SLS` / `SSS`), Leggings 7 (`SSS` / `S S` / `S S`), Boots 4 (`S S` / `S S`). Rustning 3/8/6/3, seghet 3,
  knockback-motstånd 0,1, hållbarhet 35×, förtrollbarhet 15. Lagas med Lindworm Scale. En hel uppsättning tar 24 fjäll,
  alltså två eller tre lindormar.
- **Lindworm Fang Blade:** 2 Lindworm Fang + Stick (lodrätt). Svärd, **7 skada**, snabbare än järnsvärd (−2,2), 1200
  användningar, förtrollbarhet 15. Lagas med Lindworm Fang.
- **Lindworm Venom Gland:** 8 Arrow runt körteln → 8 Arrow of Poison II.
- **Lindworm Heart:** som Sigurds drakhjärta. Äts (alltid) och ger **Strength II och Resistance I i 3 min**, Regeneration
  II i 20 s. Hunger 6.
- Lindworm Meat (hunger 3) → **Cooked Lindworm Meat** (hunger 10, mättnad 1,0).

## Gemensamt

- Rått kött förskäms på 1 dag, tillagat på 3; ruttnar till Rotten Flesh; kan rökas och torkas.
- Prestationer: Wilderness – Look Up, Plated, Venom of the Deep (efter The Cold Deep); Hush, Unheard, Eyes of the Dark
  (vittra); Troll Slayer, Sunrise (utmaning: ett troll blir sten), Thick Skinned, Tusk and Twine (troll); **Wyrmslayer**
  (utmaning: döda en lindorm), Dragon Scale, Fang Blade, Heart of the Wyrm (lindorm). Food – Crunchy, Strange Meat.
  Hunting-fliken har gruppen **The Deep**: Giant Centipede, Vittra, Troll och Lindworm (troll och lindorm är utmaningar).
- Gametests: `underground_spawn_rule` (mörkt rum ja, öppen himmel nej, utanför bandet nej, fackla i rummet nej; banden
  ger hyena/björn vid y 20, centipede/vittra vid y −10, bara troll vid y −40, troll och sällan lindorm vid y −55, inget
  under −64), `underground_yields` (allas egna kött, hud och delar; troll och lindorm kräver tusk-kniv),
  `underground_light_repels` (troll och vittra släpper en spelare med fackla i handen eller i facklans sken, men inte i
  mörkret), `underground_troll_daylight` (ett troll under öppen himmel mitt på dagen blir två Troll Stone) och
  `underground_lindworm_hoard` (guld och 3+ ädelstenar i varje skatt; en dödad lindorm sprider guld och mynt).
