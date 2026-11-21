# Under jord: plan

Grottor ska kännas farligare och kallare ju djupare man går. Byggs i steg, ett i taget.

## 1. Kyla med djupet (klart, v0.103.0)
- `Temperature.ambient`: under y 50 (`CAVE_TOP`) utan himmel räknas bara djupet (`Temperature.cave(y)`):
  10 vid y 50, 0 vid y −64. Fryser under ungefär y −30 utan kläder.
- Eld värmer som vanligt och drar mer bränsle i kylan (`fireDrain`). Ugnar och odling under y 50 följer också grotttemperaturen.
- Advancement The Cold Deep (gå under y 0).
- Senare: Ember Pot (glöd i lerkruka, värmer lite i handen), arbetsvärme när man hackar, Ice Caves kallare och Magma Vents varmare
  (grottbiomerna i [world-materials-plan.md](world-materials-plan.md)).

## 2. Gruvarbetaren (sällsynt NPC)
- Framåtlutad av en stor säck med malm på ryggen (uppskalad `WornSatchelModel`), pickaxe och en fackla som lyser (DynamicLights).
- Sällsynt under y 30, ofta vid ett litet läger: campfire, Straw Bed och kista.
- Byteshandel i stenåldern, mynt senare (`WorldAge.npcAge` som Caravans). Köper raw ores och sten, säljer malm som saknas
  i trakten (tin), facklor, mat och ibland en karta till en ådra i närheten.
- Flyr från monster.

## 3. Mörkrets väsen (nordisk folktro) (klart)
Kommer bara vid ljusnivå 0, och farligare ju djupare. Ljus är försvaret.

**Första halvan klar (djuren):** mörkerregeln och spawnern (`Underground.canSpawn` / `Underground.addBand`), Cave Hyena och
Cave Bear spawnar nu i grottorna (y 40 till 0, samma arter som i bergsfaunan), ny **Giant Centipede** (y 0 till −30) med
eget kadaver och egna produkter (Centipede Chitin → Chitin Vest, Centipede Fang, Centipede Venom Sac, kött). Rovdjur räknar
grottan som natt. Se [fauna/underground.md](fauna/underground.md).

**Andra halvan klar (folktrons väsen):** **Vittra** (y 0 till −30) jagar efter ljud (hackande, springande, steg; Vittra
Hood dämpar), **Troll** (under −30) starka och långsamma, blir Troll Stone i dagsljus, och sällsynt **Lindworm** (under
−45, boss med bett, språng, ringling och svansslag, bär en skatt av guld och ädelstenar). Vittra och troll släpper den som
håller en fackla eller står i facklans sken (blockljus 7+) och backar undan; lindormen bryr sig inte om ljus men spawnar
bara i mörker. Alla tre har eget kadaver och egna produkter (Vittra Hood, Trollhide Coat, Troll Tusk Knife, Lindworm
Scale-rustning, Lindworm Fang Blade, Lindworm Heart m.m.).

| Djup | Väsen |
|---|---|
| y 40 till 0 | Cave Hyena-flockar, Cave Bear (Cave Lion finns redan) – **klart** |
| y 0 till −30 | Giant Centipede i taket, **Vittra** som jagar efter ljud (springa och hacka lockar dem) – **klart** |
| under −30 | **Troll**: starka, skyr ljus, blir sten i dagsljus. Sällsynt **Lindworm** under −45 (boss) – **klart** |

**Alla** djur och väsen här nere lämnar ett eget **Carcass** som styckas (Butchery) och har egna produkter: hud/päls,
ben, kött och övrigt (klor, tänder, gift, horn osv.), med användningar, som de andra faunagrupperna i [fauna/](fauna/).

**Lindworm:** snabb och svår, en riktig boss. Bär på värdefulla saker: utöver eget Carcass (fjäll, ben, kött) har den
skatter på sig (guld, ädelstenar, sällsynta saker) och fjällen blir till något värdefullt.

Öppet: brinner facklor ut? Om inte är mörkret lätt att hålla borta.
