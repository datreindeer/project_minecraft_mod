# Wagon: plan

En fyrhjulig järnvagn för järnåldern: kärrans storebror. Kärran ([CartEntity](../src/main/java/com/mechanical/CartEntity.java))
är för en person och terräng, vagnen är logistik på väg. Båda finns kvar. Byggs fas för fas, som
[underground-plan.md](underground-plan.md).

> Status: **faserna 1–3 byggda** (2026-11-21). Se *Vagnen (Wagon)* i [ages/03-iron-age.md](ages/03-iron-age.md).
> Avvikelser från planen: sätet är bara för spelare än så länge (inget stamfolk) och moduler tas ur genom att slå på dem
> (inte sneak + högerklick). **Uppdaterat 2026-11-22 (living-world-plan D1–D4):** spannet sitter fast, sparas med UUID,
> spänns från med smyg + högerklick på hästen, och hästarna har ork (`HorseWork`).

## Beslutat
- **Fyra hjul med järnringar** (`iron_rimmed_wheel`) och en framaxel som svänger. Entity-id `wagon`.
- **Järn, inte stål**: kommer tidigt i järnåldern.
- **Två hästar i par vid en dragstång.** Med en häst kryper den fram när den är tom och står still när den är lastad.
  Spelare kan inte dra den.
- **Modulärt flak: ett rutnät om 6 celler, 2 breda x 3 djupa** (cell = rad*2 + sida, 0 = främre vänster). Varje cell är tom eller har en modul:
  - **tom**: 2 rum kadaver (stort djur 3 rum = 1,5 cell, medel 2, litet 1). Helt tomt flak = **12 rum** (4 stora / 6 medel / 12 små; kärran har 6).
  - **Chest**: 27 slots.
  - **Hay Bale**: foder till spannet på långa resor.
  - **säte**: en passagerare åker med.
- Moduler sätts i genom att högerklicka vagnen med blocket: en Chest tar första lediga cellen framifrån (vänster, höger, sedan
  raden bakom), Hay Bale och säte tar sista lediga cellen bakifrån. De tas ur genom att slå på dem.
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

- **Ändrat (D1–D2):** inget ok som släpper. Andra hästen sätts exakt bredvid den första varje tick, vagnen dras upp bakom
  spannet om den kommer för långt efter, och den släpper aldrig när den fastnar. Greppnyckeln (H, titta på vagnen) spänner bara för (den
  ridna hästen leder alltid); smyg + högerklick på en spänd häst spänner från den. Spannet sparas med UUID (`lead_horse`,
  `partner_horse`). Ett par har ingen egen fartsänkning längre: lasten sänker farten och tömmer orken genom `HorseWork` (D3).

## 2. Modulerna (byggt)
- 6 celler (2x3) som synkas till klienten (renderaren ritar Chest, Hay Bale och säte i sin cell).
- Kadaverlast räknas per tom cell: `capacity() = 2 × tomma celler` (12 rum när allt är tomt). En kropp som inte får plats hindrar att en modul
  sätts i.
- Chest: en `SimpleContainer` med 27 slots per kista, upp till 6 kistor (162 slots). **Två kistor i samma rad (cell 2r och 2r+1) är en dubbelkista**: ritas som vaniljens dubbelkista (en sammanhängande kropp, ett lås i mitten mot bakgaveln, via vaniljens chest-modeller i `WagonRenderer`) och öppnas som en (smyg + högerklick, 54 slots, vänstra cellens kista först; `ChestMenu.sixRows` över en `CompoundContainer`). En ensam kista öppnar sina 27 slots. Skärmen stängs om vagnen försvinner, är mer än 8 block bort eller kistan tas ur. Sparfilen är oförändrad (`modules` har
  2 bitar per cell, `hay` och `chestN` per cell; gamla vagnar med 4 platser läses som cell 0-3).
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

## Stockar (byggt, living-world-plan F1)
- En fälld stock (`FelledLogEntity`) på högst 4 block kan lastas på ett tomt flak: högerklicka vagnen medan du lyfter
  stocken (det klicket går före att spänna för). Bakgaveln tas bort i modellen, vagnen räknas som full. Smyg + högerklick med tom hand rullar av den.

## Buggrunda (v0.186.0)
- **Andra hästen och vagnen i takt:** dragningen görs en gång till efter att alla entities rört sig (LevelTickEvent.Post,
  båda sidor), så vagnen aldrig ligger en tick efter en häst som tickar senare. På ryttarens klient styr klienten vagnen
  (som hästen; serverns äldre positioner används bara om den hamnat mer än 3 block fel) och den andra hästen placeras
  där, med samma kroppsvridning och samma steg som den första. Mätt i klienten: 0,000 block fel i galopp och sväng.
- **Stel dragstång:** dragstången går från parets mitt rakt bakåt längs hästarna till framaxeln (1,8 block), flaket följer
  framaxeln som ett släp, högst 55° styrutslag (sedan dras flaket runt). Backar hästarna skjuts vagnen bakåt.
  Modellens framaxel visar exakt samma vinkel.
- **Spänd häst står still:** en häst i en vagns, kärras eller plogs sele som ingen rider eller leder får sina gå- och
  tittmål avstängda (HorseWork#standInTraces). Den andra hästen vreds tidigare till förstahästens kurs men med en
  annan kroppsvridning (skakade när ryttaren klev av): nu kopieras varje vridning för sig.
- **Stockar:** ett klick på en stock som redan ligger på flaket räknas som ett klick på vagnen (lastar nästa stock),
  avlastning kräver smyg + tom hand.
- **Övrigt:** sidan för andra hästen sparas och synkas (byter inte sida när ryttaren byter häst), den andra hästen går
  inte att sitta upp på medan någon kör, sittande kliver av vid sidan av flaket, smyg + högerklick på hästen spänner från
  kärra och plog också, kärrans häst (och vagnen den dras efter) sparas med UUID, kärrans skaklar skjuts bakåt.

## Lutning i backar (v0.195.0)
- Flaket vilar på hjulen: klienten letar marken under varje hjul (mitten och två punkter fram och bak på fälgen, så
  hjulet rullar upp på kanten av en slab eller ett trappsteg, gräs-slabs och vägarnas path-slabs med sin riktiga höjd).
  Vagnen lutar fram/bak efter främre mot bakre hjulparet och åt sidan efter höger mot vänster, på medelhöjden av de fyra;
  kärran lutar åt sidan efter sina två hjul och vilar axeln på deras medelhöjd (fram/bak som förut efter skaklarna).
  Allt mjukas ut (0,4 per tick) och marken söks 2,5 ticks av rörelsen framåt så att flaket inte släpar efter i en backe.
  Mätt i galopp uppför en väg med en halv slab per block: hjulen högst ~0,3 block från marken, still 0,05.
- Lasten följer med: moduler, karavanens last och tält ritas i flakets lutning, stockar och kroppar placeras på det lutande
  flaket (stockarna lutar med), sittande likaså. Dragstången håller sig vid hästarnas bogar: den vrids efter hur högt
  paret står över framaxeln, oavsett flakets lutning. Bara klienten lutar (servern räknar som förut), så den stela
  dragningen och hästarna i takt från buggrundan är orörda.
- **Utelämnat:** framhjulens utslag räknas inte in (marken söks där hjulen står rakt fram), kroppar på flaket lutar inte själva.
- Dev: `TiltShots` (tiltshots.flag i run/) bygger en backe och tar bilder.

## Flerspelarkontroll (v0.196.0)
- Provat med en dedikerad server (runServer i worktreens run/, offline-läge) och en separat dev-klient som ansluter och
  spelar igenom allt med riktiga klick och tangenter (`MpShots`, mpshots.flag; servern bygger scenerna med `/mptest`,
  bara registrerat med mptest.flag i serverns mapp, och svarar med serverns positioner så att klienten kan jämföra).
- **Vagnen:** leda andra hästen med lead, sitta upp, klicka vagnen (spänner för båda), galopp och sväng: andra hästen och
  vagnen 0,000 block från det stela spannets platser på klienten, vagn och hästar på exakt samma position som servern
  både med ryttare och 5 s efter avstigning.
- **Stock:** lyfta, dra, lägga ner: klient och server inom 1,5 cm. **Vattenhjul:** båda kroppsentiteterna finns på klienten
  med rätt form, hjulet ritas och snurrar. **Kittel och lerkruka:** tripod och kruka ställs på elden, vatten och mat i,
  innehållet syns direkt på klienten, efter 6000 ticks klart på båda sidor och en skål ger Stew. **Blodkittel:** kitteln på
  slaktställningen fylls (blockstate) och en hink ger Blood Bucket. **Oak-uppdragen:** skärmen öppnas.
- **Fel hittat och rättat (även enspelare):** en karavans ledare räknas som ledare och fick en bosatt ledares dialog
  ("välkommen till vårt läger", ingen Trade-knapp), så karavanens handelsskärm gick inte att nå genom att klicka på honom.
  Nu får han karavandialogen med Trade, och handelsskärmen öppnas och visar varorna över nätet.
- Inga undantag i server- eller klientloggen. Bara en klient provades (ingen andra spelare som tittar på).

## Senare
- Uroxar som dragdjur (kräver tämjning).
- Karavaner med vagn.
- Fler moduler (Barrel för vätskor, tak av canvas).
