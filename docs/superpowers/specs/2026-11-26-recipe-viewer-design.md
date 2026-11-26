# Receptvisaren (R/U) – design

Datum: 2026-11-26. Första av tre projekt (receptvisaren, skills, dynamiska verktyg). Besluten för de två senare står i
`docs/skills-and-tools-plan.md`.

## Varför

Modden har tusentals recept spridda över crafting, en rad stationer och processer ute i världen. Spelaren ska kunna peka
på ett item och få veta hur det görs (R) och vad det används till (U), i moddens eget utseende, utan spoilers från
senare åldrar.

## Mål

- **R** över ett item i vilket inventory som helst: visa recepten som *ger* itemet.
- **U** över ett item: visa recepten som *använder* itemet.
- En **sökbar itemlista** bredvid inventoryt (som JEI), där R/U fungerar likadant och vänsterklick = R, högerklick = U.
- Alla recepttyper visas, ritade som sin station: crafting (shaped/shapeless), stonecutting, campfire/smoking/smelting,
  Tool Station, Fletching, Armor Station, Leatherworking, Mold Bench, Pattern Bench, Kiln, Bloomery.
- Items som görs ute i världen (Drying Rack, anvils, Quern, kniv på gräs, styckning …) får en **textsida**
  "Så får du tag i det" med texten från `registry_notes_en.json`, där `[[id]]` blir klickbara item-ikoner.
- Bara **upplåsta** recept visas.
- Hide-and-bark-stilen (`client/ui/HideUi`).

## Icke-mål

- Ingen JEI/REI/EMI-integration, inget beroende.
- Inget "fyll i receptet åt mig" (flytta items till craftinggriden). Kan komma senare.
- Inga favoriter/bokmärken.
- Recepten flyttas inte till datapacks; Java-listorna förblir sanningen.

## Anti-mål

- Visar recept från en ålder spelaren inte nått → spoiler, räknas som fel.
- Ser ut som en vanilla-/JEI-skärm mitt i moddens stil.
- R i världen (ge upp när nedslagen, `DownedClient`) får inte störas.

## Begränsningar

- MC 26.3 skickar inte receptlistan till klienten. **Servern** måste bygga och skicka spelarens upplåsta recept.
- Stationsrecepten är hårdkodade i Java: `ToolStationRecipes.ALL`, `StationRecipe`-listor (`FletchingRecipes`,
  `ArmorStationRecipes`, `LeatherworkingRecipes`, `MoldBench`, `PatternBenchBlock`), `KilnBlockEntity.RECIPES`,
  `BloomeryBlockEntity`.
- `registry_notes_en.json` ligger i `docs/`, inte i resources.
- R är redan bunden (`key.mechanical.downed`).

## Byggsätt

| | Vad | För/emot |
|---|---|---|
| **A. Allt + text (valt)** | Servern skickar alla upplåsta recept (receptfiler + Java-stationer); världsprocesser som textsidor | Flest items får ett svar. Mest arbete. |
| B. Recept + stationer | Som A utan textsidor | Plant Fibre, kött, torkat m.m. visar "inget recept". |
| C. Bara receptfiler | Bara JSON-crafting | Minst, men Tool Station, Kiln m.fl. saknas helt. |

Valt av användaren: **A**.

## Det användaren bestämt

1. Egen visare i moddens stil, inte JEI.
2. Bara upplåsta recept.
3. R/U på hover i alla inventories + sökbar itemlista bredvid.
4. Allt + textsidor (byggsätt A).
5. **Upplåst** (ändrat efter granskningen, eftersom "en ingrediens" lät en Stick låsa upp järnålderns recept):
   - **Java-stationerna** behåller sin egen regel: `unlockedBy` i `ToolStationRecipes`/`StationRecipe` (nyckeldelen,
     t.ex. Bronze Pickaxe Head), `UNLOCKED_TOOLS` som redan räknas ut, och `unlockedBy == null` = alltid upplåst.
     Kiln och Bloomery: upplåsta när spelaren sett något item som receptets ingångs-predikat matchar.
   - **JSON-recept** (crafting, stonecutting, matlagning): upplåsta när spelaren har sett **varje** ingrediens
     (en slot med flera möjliga items räknas som sedd om något av dem är sett).
   - **Vanillas receptbok räknas inte.**

## Hur det byggs

### Server
- **Sedda items:** ny attachment `seen_items` (mängd av item-id, sparas med spelaren). Fylls av samma sorts
  `PlayerTickEvent`-skanning som `ToolStationRecipes`/`StationRecipe` redan gör (med samma glesa intervall).
  Vid första inloggningen efter uppdateringen fylls den från inventoryt, Ender Chesten och utrustningen, och för
  varje recept i `UNLOCKED_TOOLS` läggs receptets nyckeldel till (inte resultatet), så att gamla spelare i AmandaTest
  inte börjar från noll utan att få mer än de redan låst upp.
- **Katalogformat:** varje recept bär sitt eget upplåsningsvillkor (inte bara sina ingångar), enligt beslut 5.
- **Receptkatalog:** en server-klass samlar alla recept i ett gemensamt, enkelt format:
  `typ (station), ingångar (lista av slots, varje slot = en eller flera items, antal, ev. "verktyg"), utgång(ar)`.
  Källor: `RecipeManager` (de sex JSON-typerna ovan) och Java-listorna. Byggs en gång per datapack-laddning.
- **Upplåsta recept:** de i katalogen där någon ingång finns i `seen_items` (eller receptboken har det).
  Skickas till klienten vid inloggning; när `seen_items` växer skickas bara de nya.

### Klient
- **Tangenter:** två egna KeyMappings, `R` och `U`, med GUI-konfliktläge, så de inte krockar med `downed` i världen och
  kan bindas om i Controls.
- **Receptskärm:** öppnas ovanpå inventoryt. En sida per recept, flikar per station (stationens ikon), bläddring. Ingångar
  med flera möjliga items (taggar, `alt`) växlar runt. Klick på ett item i receptet = R/U på det (vänster/höger),
  med "tillbaka" (Backspace/Esc går ett steg bakåt).
- **Textsida:** fliken "Så får du tag i det" visas på R när itemet har en not (även bredvid recept, eftersom noten
  ofta berättar om världskällor som recepten saknar). Visas när spelaren har sett itemet eller något item noten länkar
  till. **Länkar till osedda items visas som "???"** utan ikon, så att noter som säger "Used for [[x]]" inte avslöjar
  senare åldrar. Kopieringen till resources förblir en ren kopia; maskningen görs vid ritning.
- **Söklista:** till höger om inventoryt i `AbstractContainerScreen`-skärmar, en sökruta + ett rutnät + sidor. Visar
  sedda items + utgångar från upplåsta recept. Döljs när fönstret är för smalt. Krockar inte med vanillas receptbok-knapp
  (`InventoryScreenMixin` flyttar den redan).

### Data
- `registry_notes_en.json` kopieras till `assets/mechanical/recipe_notes.json` av generatorkedjan (ett steg sist i
  `gen_registry.py` eller ett eget litet skript), så att klienten kan läsa det som resurs.

### Test och kontroll
- Gametest: `seen_items` fylls, upplåsningsregeln (ett JSON-recept är upplåst först när alla ingredienser är sedda; ett Tool Station-recept
  låses upp av nyckeldelen, inte av Cordage),
  katalogen innehåller minst ett recept av varje stationstyp.
- Skärmbild av receptskärmen, textsidan och söklistan via `client/ui/GuiShots`.

## Lämnat åt byggaren

- Exakt layout per stationstyp (följ stationernas egna skärmar, t.ex. `ToolStationScreen`).
- Paketformat och hur deltan skickas.
- Sortering i söklistan (förslag: registerordning, sökning på visat namn).

## Mina beslut (rätta gärna)

- Söklistan visar bara sådant spelaren sett eller kan göra, inte allt (följer spoilerregeln).
- Textsidor syns när itemet eller ett länkat item är sett; osedda länkar blir "???".
- Gamla spelare: nyckeldelen från `UNLOCKED_TOOLS` räknas som sedd, plus allt i inventory, Ender Chest och utrustning.
- Kiln/Bloomery: upplåsta när ett item som matchar ingången är sett.
- Ingen advancement för receptvisaren (det är ett verktyg, inte en upptäckt).
