# Wagon: plan

En fyrhjulig järnvagn för järnåldern: kärrans storebror. Kärran ([CartEntity](../src/main/java/com/mechanical/CartEntity.java))
är för en person och terräng, vagnen är logistik på väg. Båda finns kvar. Byggs fas för fas, som
[underground-plan.md](underground-plan.md).

> Status: **faserna 1–3 byggda** (2026-11-21). Se *Vagnen (Wagon)* i [ages/03-iron-age.md](ages/03-iron-age.md).
> Avvikelser från planen: sätet är bara för spelare än så länge (inget stamfolk), moduler tas ur genom att slå på dem
> (inte sneak + högerklick), och spannet sparas inte över en omladdning (som kärrans sele).

## Beslutat
- **Fyra hjul med järnringar** (`iron_rimmed_wheel`) och en framaxel som svänger. Entity-id `wagon`.
- **Järn, inte stål**: kommer tidigt i järnåldern.
- **Två hästar i par vid en dragstång.** Med en häst kryper den fram när den är tom och står still när den är lastad.
  Spelare kan inte dra den.
- **Modulärt flak med 4 platser.** Varje plats är tom eller har en modul:
  - **tom**: 3 rum kadaver. Helt tomt flak = **12 rum** (4 stora / 6 medel / 12 små; kärran har 6).
  - **Chest**: 27 slots.
  - **Hay Bale**: foder till spannet på långa resor.
  - **säte**: en passagerare åker med.
- Moduler sätts i genom att högerklicka en plats med blocket och tas ur med sneak + högerklick med tom hand
  (som kärrans Chest idag).
- **Uroxar drar inte** förrän de kan tämjas. Det är en egen uppgift som kommer senare.
- **Trader Caravans behåller kärran** tills vidare.

## 1. Vagnen och spannet (byggt)
- `WagonEntity`. Ärver från `TravoisEntity` om dragrepet går att återanvända, annars en egen entity med samma
  uppför-/fall-/fastnar-logik.
- Spannet: du rider eller leder den ena hästen, och den andra låses vid dess sida (fast offset, följer med i svängar).
  Båda hästarna sparas med UUID, som karavanhästen.
- En häst: kryper fram när den är tom (t.ex. 25 % fart) och står still när den är lastad. Kopplas en häst loss
  (död, avstånd) går vagnen över till enhästläge.
- Framaxeln svänger mot dragstången och bakaxeln följer efter. Vagnen fastnar i tät skog: det är meningen.
- Slå sönder den för att plocka upp den: kadaver, varor och moduler faller ur.
- **Byggt:** `WagonEntity` ärver från `TravoisEntity`. Andra hästen hålls bredvid den första (1,45 block isär) och släpper
  ur oket om den hålls kvar mer än 4 block. En häst: vagnen sluter 30 % av avståndet tom och hästen går 75 % långsammare;
  lastad rör den sig inte. Ett par: upp till 15 % långsammare full. **Avvikelse:** spannet sparas inte med UUID, utan
  försvinner vid omladdning som kärrans sele.

## 2. Modulerna (byggt)
- 4 platser som synkas till klienten (renderaren ritar Chest, Hay Bale och säte på rätt plats).
- Kadaverlast räknas per tom plats: `capacity() = 3 × tomma platser`. En kropp som inte får plats hindrar att en modul
  sätts i.
- Chest: en `SimpleContainer` med 27 slots per plats. Sneak + högerklick på en kistplats öppnar just den.
- Hay Bale: hästarna äter av den när de är hungriga (kopplas till `Carting.HORSE_FEED`), och en bal räcker länge.
- Säte: en passagerare (spelare eller tribesperson).
- **Byggt:** modulerna synkas (2 bitar per plats). Hay Bale: 9 tuggor, en skadad häst i spannet äter en var 5:e s och
  läker 2 hälsa (inte via `Carting.HORSE_FEED`). Säte: valfri trä-slab, kommer tillbaka som Oak Slab. **Avvikelser:**
  sätet är bara för spelare (inget stamfolk än), och moduler tas ur genom att slå på dem (en påbörjad bal ger sin
  återstående Wheat); smyg + högerklick på en tom plats tar i stället av den sista kroppen.

## 3. Modell, recept, guide (byggt)
- `WagonModel` och `WagonRenderer`: längre flak, fyra hjul med järnringar, dragstång och svängbar framaxel.
- `iron_rimmed_wheel`: Wooden Wheel + järn på städet.
- Vagnen: 4 Iron-Rimmed Wheel + plankor + järnbeslag.
- Advancements i järnålderns flik: bygg en vagn, spänn för två hästar, kör en full vagn (12 rum), vagn med 4 kistor.
- `docs/ages/iron_age.md`, registry.html och progression.json uppdateras.
- Gametest: kapacitet per modul, enhästläge, att moduler och last faller ur när den slås sönder.
- **Byggt:** `WagonModel`/`WagonRenderer`, `scripts/gen_wagon.py`, gametest `WagonTest`. Iron-Rimmed Wheel är ett formlöst recept (Wooden
  Wheel + 2 Iron Plate), inte städet. Wagon: `WRW`/`PIP`/`WPW` (4 Iron-Rimmed Wheel, 3 Planks, Iron Plate, Iron Tool Rod).
  Advancements: Iron Shod, Wagoner, A Pair in the Yoke, Heavy Haul, Freight. Docs: 03-iron-age.md, registry_notes,
  progression.json.

## Senare
- Uroxar som dragdjur (kräver tämjning).
- Karavaner med vagn.
- Fler moduler (Barrel för vätskor, tak av canvas).
