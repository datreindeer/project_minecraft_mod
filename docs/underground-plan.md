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

## 3. Mörkrets väsen (nordisk folktro)
Kommer bara vid ljusnivå 0, och farligare ju djupare. Ljus är försvaret.

| Djup | Väsen |
|---|---|
| y 40 till 0 | Cave Hyena-flockar, Cave Bear (Cave Lion finns redan) |
| y 0 till −30 | Giant Centipede i taket; **Vittra/underjordiska** som jagar efter ljud (springa och hacka lockar dem) |
| under −30 | **Troll**: starka, skyr ljus, blir sten i dagsljus. Sällsynt **Lindworm** riktigt djupt (boss) |

**Alla** djur och väsen här nere lämnar ett eget **Carcass** som styckas (Butchery) och har egna produkter: hud/päls,
ben, kött och övrigt (klor, tänder, gift, horn osv.), med användningar, som de andra faunagrupperna i [fauna/](fauna/).

**Lindworm:** snabb och svår, en riktig boss. Bär på värdefulla saker: utöver eget Carcass (fjäll, ben, kött) har den
skatter på sig (guld, ädelstenar, sällsynta saker) och fjällen blir till något värdefullt.

Öppet: brinner facklor ut? Om inte är mörkret lätt att hålla borta.
