# Serverpanel, karta och stamområden

> Status: godkänd 2026-11-25. Beslut från samtal med användaren; "mitt val" = Claudes förslag som användaren kan ändra.

## Varför
Admin (användaren) vill se servern i realtid: hur den mår, vilka spelare som är var, vilka stammar som finns och vilket område varje stam äger, på en karta i webbläsaren. Samtidigt görs stamområdena om: Campfire ger inget, Fire Pit ger 5x5 chunks, och den stora Fire Pit tar hela biomen den står i (med ett tak), med plats för att området ska kunna växa i framtiden.

## Mål
1. **Stamområden** (spelet):
   - Campfire: inget område, ingen laddning (redan så idag, ingen ändring).
   - Fire Pit (vanlig): området = 5x5 chunks (2 åt varje håll från eldens chunk). Laddas också 5x5 (idag 3x3, `Clans.LOAD_RADIUS = 1` → 2).
   - Stora Fire Pit: området = alla chunks i **samma biom** som eldens chunk som hänger ihop med den (flood fill), högst `TERRITORY_MAX = 16` chunks åt varje håll (33x33). Laddning: bara 5x5 runt elden (*mitt val*: samma som vanlig Fire Pit; skillnaden mellan dem ligger i området. `FirePit.BIG_LOAD_RADIUS` blir 2 eller tas bort).
   - Taket är ett värde att testa sig fram med ("vi får testa oss fram sen").
2. **Serverpanel** (webbsida, bara admin, bara lokalt): flikar **Översikt**, **Spelare**, **Stammar**, **Karta**.
3. **Kartan**: realtid, visar spelare, stammarnas eldar (Fire Pit och Campfire) och varje stams område så att det syns tydligt.

## Inte nu (non-goals)
- Ingen publik åtkomst, ingen inloggning, ingen Caddy-post. Panelen lyssnar bara på `127.0.0.1`.
- Inga nya spelregler kopplade till området (byggskydd, intrång osv.) utöver det som står här. Området används av kartan, laddningen och `clan/Lands` (se nedan); `CampAge.RADIUS` (24 block, lägret: vakter, besök, lägrets ålder) är oförändrad.
- Ingen expansion av området än; bara att datan tillåter det (se nedan).
- Bara Overworld på kartan.
- Ingen 3D-vy, inga extra bibliotek (varken Java eller JS).

## Anti-mål (misslyckat även om det fungerar)
- Servern laggar av panelen eller kartritningen (märkbart lägre TPS).
- Hela biomer hålls laddade.
- Panelen nås från nätet.
- Stammarnas områden är svåra att skilja åt på kartan.

## Vägar vi övervägde
- **A. Allt i modden (vald).** JDK:s inbyggda `com.sun.net.httpserver.HttpServer` i modden; servern ritar kartbilder själv från blocken; en statisk HTML/JS-sida hämtar JSON. Helt eget, inga beroenden, kan visa moddens egna saker (stamområden, säsong, världsålder). Kostnad: egen kartritare, enklare än BlueMap.
- B. BlueMap/squaremap + egen panel: snygg karta gratis men inte eget, två separata saker, och sådana moddar finns sällan för 26.x NeoForge.
- C. Separat program som läser världsfiler/RCON: ingen riktig realtid. Struken.

## Stamområde: data
- Varje `Clans.Clan` får ett sparat fält `territory`: en mängd chunk-positioner (long, `ChunkPos.pack`) i Pitens dimension. Sparas i codec som `optionalFieldOf("territory", List.of())` så gamla sparfiler fungerar.
- Området räknas om när: stammen får en pit, piten flyttar, piten byggs om till stor (`FirePitBlock#upgrade`) eller går sönder. För gamla stammar utan sparat område räknas det fram vid första tillfälle (t.ex. i `keepPitsLoaded`).
- **Vanlig Fire Pit:** 5x5 runt eldens chunk.
- **Stor:** 5x5 runt elden **plus** flood fill (4-grannar) från eldens chunk över chunks vars biom = eldens biom, inom ±16 chunks (*mitt val*: en stor pit får aldrig mindre än en vanlig, även vid en flod eller strand). "Eldens biom" = biomen vid pit-blocket. Biomen för övriga chunks avläses i chunkens mitt, på ytan, med samma ytbiomregler som modden använder (se `worldgen.BiomeMap`, "after the mod's rules"). Det måste fungera för chunks som inte är laddade/genererade (biomkällan + klimatsamplern, utan att generera chunks).
- Eldens chunk ingår alltid.
- **Inga överlapp, aldrig** (användarens beslut): en chunk tillhör högst en stam.
  - Att grunda en stam vid en Fire Pit eller flytta stammens pit nekas om pitens 5x5 skulle röra en annan stams område. Spelaren får ett meddelande som säger varför (vilken stam, ungefär åt vilket håll), som andra nekanden i `Clans`.
  - En stor pits flood fill stannar vid andra stammars chunks (och NPC-byars/lägers land i `Lands`). Uppgraderingen tillåts alltid.
  - Frigjorda chunks (pit flyttad, sönder, stam upplöst) blir lediga; grannar växer inte in av sig själva (*mitt val*, de får dem vid nästa egna omräkning).
  - Framtida expansion följer samma regel.
- **Expansion senare:** eftersom området är en sparad mängd chunks kan framtida funktioner lägga till chunks utan att formatet ändras.
- **`clan/Lands` följer området** (användarens beslut): `Lands.landAt` frågar `Clans.ownerOf` i stället för `PIT_CHUNKS = 1`, så titeln "You are now entering X's land", hemma/stam-uppdelningen och avancemanget `wilderness/tribe_land` gäller hela området (5x5 eller biomen). `PIT_CHUNKS` tas bort.
- Ett anrop `Clans.territoryOf(clan)` / `Clans.ownerOf(level, chunkPos)` för kartan och framtida regler.

## Panelen: hur den körs
- Startar på `ServerStartedEvent`, stängs på `ServerStoppingEvent`. Bara dedikerad server (*mitt val*: inte i singleplayer/dev-klient eller i gametest-körningar).
- Port i en server-konfig (NeoForge `ModConfigSpec`), standard **8100**, och en av/på-flagga (standard på). Binds till `127.0.0.1`.
- HTTP-trådar rör aldrig världen. Servertråden bygger en **ögonblicksbild** (oföränderlig, färdig JSON-sträng) en gång per sekund; HTTP-trådarna lämnar ut den senaste.
- Sidan uppdaterar sig genom att fråga varannan sekund (*mitt val*; ingen WebSocket).
- Språk på sidan: engelska som resten av moddens text (*mitt val*).

### Endpoints
- `GET /` och statiska filer: sidan (HTML/CSS/JS) från moddens resurser, t.ex. `assets/mechanical/dashboard/`.
- `GET /api/status`: modnamn och version, upptid, TPS och MSPT (snitt), spelare online/max, minne använt/max, laddade chunks, antal entiteter, dag och tid, säsong och datum (`Calendar`), världsålder (`WorldAge`).
- `GET /api/players`: alla kända spelare: namn, UUID, online, position och dimension (online), hälsa, hunger, ping, stam; offline-spelare med senast sedd (från stamdatan/usercache).
- `GET /api/tribes`: namn, färg (DyeColor → hex), medlemmar med rang, ålder, pit-position, stor/vanlig, områdets storlek i chunks, delat rykte med NPC-stammar.
- `GET /api/map`: kartdata för överlägget: spelare (namn, x, z, riktning), eldar (stammens Fire Pit, och alla Campfires/Fire Pits i laddade chunks som modden redan håller koll på, med ägarstam om någon), och varje stams område som **konturer** (polygoner längs chunkkanter, räknade på servern) plus färg och namn.
- `GET /tiles/<x>.<z>.png`: kartbild för en region (512x512 block, 1 pixel per block).

## Kartbilder
- Ritas av servern från block som finns i världen: översta blocket (`Heightmap.WORLD_SURFACE`), dess `MapColor` med höjdskuggning mot grannen i norr som vanliga kartor, vatten med djupskuggning.
- En chunk ritas när den laddas och när den sparas/lastas ur (då har den ändrats). Läsningen av blocken sker på servertråden (256 färger per chunk, billigt); PNG skrivs på en egen arbetstråd.
- Bilderna sparas i världsmappen, `<värld>/mechanical_map/<x>.<z>.png`, så de överlever omstart. Kartan börjar tom (även i AmandaTest, som spelas vidare; ingen ny värld) och fylls i där spelarna rör sig: varje chunk som laddas runt en spelare ritas in. Ingen läsning av gamla regionfiler.
- Gräns för hur mycket arbete per tick (en kö), så att utforskning i hög fart inte sänker TPS.

## Sidan
```
┌ Jackt – Ages of Man v0.x ─ Overview | Players | Tribes | Map ┐
│ TPS 20.0  MSPT 12  Players 2/20  Mem 3.1/8 GB  Day 41 Spring │
```
- **Översikt:** rutor med siffrorna från `/api/status`, och en liten kurva för TPS och spelare senaste timmen (sidan håller historiken i minnet).
- **Spelare:** tabell; klick på en spelare visar den på kartan.
- **Stammar:** kort per stam i stamfärgen: medlemmar, ålder, pit, områdets storlek; klick visar området på kartan.
- **Karta:** canvas med panorering och zoom. Lager: kartbilder, stamområden, eldar, spelare. **Stamområden:** halvgenomskinlig fyllning i stamfärgen, tjock kantlinje i samma färg, stammens namn i mitten. Eldar som ikoner (Fire Pit större än Campfire). Spelare som prickar med namn och riktning. Muspekaren visar koordinater och vilken stam som äger chunken.

## Tester
- Gametest för området: vanlig pit ger 25 chunks; stor pit ger bara chunks i samma biom, inom taket, ihopkopplade; två stammar överlappar inte; gamla sparfiler utan `territory` laddas; grunda/flytta nära en annan stam nekas med meddelande; `Lands.landAt` ger stammens land i hela området.
- Gametest för laddningen: vanlig pit laddar 5x5.
- En enkel kontroll av konturerna (chunkmängd → polygon) och av kartritningen (en chunk → rätt färger).
- Panelen provas på riktigt: starta servern, öppna `http://localhost:8100`, skärmbild.

## Övrigt som följer projektets vanor
- Versionsbump, `docs/registry.html` påverkas inte (inga nya föremål), uppdatera `docs/seasons-and-tribes.md` om stamområden.
- Inga nya avancemang behövs för panelen. Ett för "stor Fire Pit tar biomen" kan läggas till om det känns rätt (*builders val*).

## Lämnas till den som bygger
- Exakt hur biomen för ej laddade chunks avläses (helst samma kod som `BiomeMap`).
- Konturalgoritm, PNG-skrivning, ikoner, exakt utseende inom ramarna ovan (följ `frontend-design` och projektets kvalitetskrav).
- Hur Campfires utanför stammar listas (bara de modden redan spårar, t.ex. via `CampAge`/`Campfires`; ingen ny världsskanning).
