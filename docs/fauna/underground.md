# Under jord (underground)

Djuren i grottornas mörker: **Cave Hyena**, **Cave Bear** och **Giant Centipede**. Steg 3 i
[underground-plan.md](../underground-plan.md) (de riktiga djuren; folktrons väsen kommer sedan).
Kod: `wildlife/underground/` (Underground = mörkerregeln och spawnern, UndergroundFauna, GiantCentipede), klient
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

Rovdjur (Predator: Cave Hyena, men också vargar och lejon som går ner) räknar en grotta som **natt**: de sover inte och
jagar dag som natt där nere. Cave Bear ligger i dvala i grottan tills någon kommer nära (som förut).
Grottorna blir kallare med djupet (`Temperature.cave`), se steg 1 i planen.

Folktrons väsen (Vittra, Troll, Lindworm) lägger till sina egna band med `Underground.addBand(typ, minY, maxY, vikt, min, max)`.

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

## Gemensamt

- Rått kött förskäms på 1 dag, tillagat på 3; ruttnar till Rotten Flesh; kan rökas och torkas.
- Prestationer: Wilderness – Look Up, Plated, Venom of the Deep (efter The Cold Deep); Food – Crunchy. Hunting-fliken får
  en ny grupp **The Deep** (Giant Centipede; folktrons väsen hamnar här också).
- Gametests: `underground_spawn_rule` (mörkt rum ja, öppen himmel nej, utanför bandet nej, fackla i rummet nej; banden
  ger hyena/björn vid y 20 och bara centipede vid y −10) och `underground_yields` (allas egna kött, hud och delar).
