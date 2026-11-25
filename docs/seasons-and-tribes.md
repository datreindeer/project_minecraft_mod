# Stammar, världsålder och säsonger

> Status: **fas 1 byggd 2026-11-20 (v0.81.0)**: kalender, 60-minutersdygn, HUD och `/calendar`. Resten är plan (beslutad 2026-11-20). Tre delar som hänger ihop: spelarstammar med gemensam ålder,
> en världsålder som följer den mest avancerade stammen, och ett år med säsonger, längre dygn och kalender i HUD.

## Beslut i korthet

| Fråga | Beslut |
|---|---|
| Stam | Bildas vid en **Fire Pit**, som blir stammens centrum. Den som bildar stammen är ledare. En stam har exakt en Fire Pit. Den är oförstörbar och kan inte tas bort så länge stammen finns. |
| Flytta eller upplösa | Görs med en **röstning** i stammen: flytta Fire Pit:en eller upplösa stammen. |
| Ledaren lämnar | Ledaren måste först ge ledarrollen till någon annan. |
| Stam-GUI | En egen sida som öppnas med `G`: medlemmar, ranker, inbjudningar och röstningar. |
| Stamfärg | Ledaren väljer en av de 16 dye-färgerna på stamsidan (vimpelknappen uppe till höger). Syns på Tab-listan. |
| Tab-listan | Grupperad per stam: en vimpelrad i stammens färg med namn, online/alla och ålder, sedan medlemmarna med rank, ålder och ping. Spelare utan stam hamnar sist som **Wanderers**. |
| Ranker | Leader, Elder (högre rank), Member (vanlig) och Newcomer (ny medlem). Ledare och Elder kan bjuda in. |
| Advancements | Delas av hela stammen. Vid inträde slås spelarens och stammens advancements ihop **åt båda hållen**. Den som lämnar behåller allt. |
| Stammens ålder | Samma för alla medlemmar: den högsta ålder stammen nått. |
| Max antal medlemmar | Inget. |
| Skada inom stammen | Nej (ingen friendly fire). |
| Stamchatt, gemensamma kistor, hemplats | Väntar. |
| Världsålder | Den högsta åldern bland alla stammar. Sjunker aldrig. |
| NPC-stammar | Följer världsåldern, men 0–1 ålder efter. När en NPC-stam går upp en ålder river den sina gamla byggnader, bygger nya och byter utrustning. |
| Handel | **Byggd (2026-11-23).** All byteshandel med bybor går i **mynt** (koppar billigt, silver dyrare, guld värdefullt). **Handelskaravaner** kommer i sorter (trä, gruva, smycken, jakt, örter, samlande) med 4–5 personer, egen last och egen handelsruta med kassa och lager. Stammarnas Elders, Miner och besökande ledare byter i **samma ruta** (bytesvaror som ikoner, ingen kassa, v0.211.0). Se [traders-plan.md](traders-plan.md). |
| Dygn | 60 min: i snitt 30 min dag + 30 min natt. Längden på dag och natt varierar med säsongen. |
| Kalender | 30 dagar/månad, 3 månader/säsong, 4 säsonger/år (360 dagar). En säsong = 90 h, ett år = 360 h. |
| Start | Mitt i våren (dag 15 i vårens andra månad). |
| Sova bort natten | **Byggd.** När **alla** spelare ligger i en Straw Bed spolas natten fram. Himlen snurrar synligt, och natten är över på cirka 23 s. |
| Hunger | **Byggd.** Bättre och större mat ger mer mättnad som räcker längre (`FoodValues`). |
| Tid när servern är tom | **Byggt (ändrat 2026-11-22):** går vidare i **halv hastighet** (ett speldygn = 2 h), se `living-world-plan.md` A1. Tidigare: står still. |
| Temperatur | Sänks globalt på vintern. Kalla biomes kan skada spelare som saknar skydd. |
| Väder | Följer säsongen. |
| Lövträd | Blir kala på vintern (bara grenar). Barrträd behåller barren. |
| Bär | Vissa klarar vintern. De andra får ett fruset stadium och växer vidare när det blir varmt. |
| Grödor | Varje gröda och marktyp har en egen säsongsprofil. Profilerna är data, så nya grödor bara lägger till en rad. |
| Sjöar | Fryser på vintern och tinar på våren. |
| Djur | Mindre aktiva på vintern. Björnar går i ide och flyttfåglar flyttar söderut. Båda är med från början. |
| Mat | Det gäller att bunkra inför vintern. |

---

## 1. Spelarstammar

- **Bildas** vid en Fire Pit, t.ex. med sneak + högerklick med tom hand eller med ett kommando medan man står vid den.
  Fire Pit:en blir stammens centrum och den som bildar stammen blir ledare. Mer om stammar kommer senare.
- **En stam har exakt en Fire Pit.** Den är oförstörbar: tål hackor, explosioner, eld och raider. Inte ens ledaren
  kan ta bort den så länge stammen finns.
- **Röstning:** en medlem startar en röstning i GUI:t om att antingen **flytta Fire Pit:en** eller **upplösa
  stammen**. Går flytten igenom kan den gamla Fire Pit:en tas bort, och stammens centrum flyttas till nästa Fire Pit
  som placeras. Går upplösningen igenom upplöses stammen. Alla behåller sina advancements, och Fire Pit:en blir en
  vanlig Fire Pit igen. En stam med bara en medlem röstar direkt igenom.
- **Röstningsregler:** en röstning pågår i **3 speldygn** och går igenom med **2/3** av stammens medlemmar. Vid stora
  frågor (att upplösa stammen eller flytta Fire Pit:en) räknas medlemmar som inte röstat, även de som är offline,
  som **nej**. Vid val av ny ledare räknas den frånvarande ledaren inte alls. Pågående röstningar visas i GUI:t,
  och medlemmar får ett meddelande när de loggar in.
- **Ledaren lämnar:** ledaren måste först ge ledarrollen till en annan medlem.
- **Ledaren frånvarande:** har ledaren inte loggat in på **10 speldygn** kan vilken medlem som helst starta en
  röstning om ny ledare.
- **Stam-GUI** som öppnas med `G`. Sidan visar stammens namn och ålder, medlemmar med rank och om de är online,
  knappar för att bjuda in, befordra, degradera och kicka, samt pågående röstningar. Ledaren väljer stammens
  **färg** (en av de 16 dye-färgerna) med vimpelknappen uppe till höger; klick ger nästa färg. Gamla stammar får
  en färg utifrån sitt id.
- **Relationsfliken** (byggd): flikknapparna **Tribe | Relations** ovanför stamsidan. Relations listar varje NPC-stam
  spelaren (eller hens stam) har en relation med (`Tribe.standings`), bäst först: vimpel i stammens färg, namn, temperament (Peaceful,
  Wary, Raiders, Traders), en stapel från -100 till 100 med streck vid 0 fylld i nivåns färg, och nivåord + tal
  ("Friendly 45"; nivåer som `Tribe.standing`: Hostile ≤ -40, Unfriendly < 0, Neutral < 30, Friendly < 70, Honoured).
  Sju rader per sida med ▲▼. Bara spelarens relation till NPC-stammar (inga NPC↔NPC- eller stam↔stam-relationer).
  Fungerar även utan egen stam. Data: `ClanNetwork.View.relations`.
- **Relation per stam** (byggd, A4): alla i en spelarstam delar relationen med varje NPC-stam (sparas i stammen,
  `Clans.Clan.reputation`). Spelare utan stam har sin egen (`Tribe.REPUTATION`). När man grundar eller går med läggs ens
  egen relation in i stammens som ett **medelvärde** över medlemmarna (en NPC-stam som saknas räknas som dess startvärde);
  gamla stammar slås ihop när varje medlem loggar in. Den som lämnar, kickas eller är med när stammen upplöses **behåller
  stammens värde** som sitt eget (offline: vid nästa inloggning). Alla läsningar och ändringar går via `Tribe.standings`
  och `Tribe.addReputation` (handel, dialog, besök, räder, gåvor, zontiteln, relationsfliken).
- **Zontitel** (byggd): när man går in på en stams mark visas en barkpanel en fjärdedel ned på skärmen i 5 s (tonar in
  och ut): "You are now entering X's land" med vimpel och namn i stammens färg, och under det relationen ("Friendly 45")
  för NPC-stammar, "Home" på den egna stammens mark och inget mer på en annan spelarstams. Mark: en spelarstams territorium (se *Stammens mark* nedan: 5x5 chunks runt Fire Pit:en, för en stor Fire Pit även dess biome), en NPC-by innanför palissaden (radie 23 från mitten),
  och ett NPC-läger runt elden dess folk kallar hem (palissadens radie + 2, minst 10 block; gäller även bosättningar).
  Servern kollar var 10:e tick och skickar `Lands.Entered` en gång per inträde (inte igen förrän man gått ut eller in på
  annan mark). Första gången på en annan stams mark ger framsteget **Their Land** (wilderness). Kod: `clan/Lands`,
  `client/clan/LandTitle`.
- **Tab-listan** (spelarlistan) ersätter vanilla och syns bara medan Tab hålls nere, så den tar stor plats: upp till 88 %
  av skärmbredden (en ensam kolumn högst 420 GUI-pixlar), rader på 14 px med 12 px ansikten, rankord bredvid namnet,
  ålder som ikon och namn och ping i ms bredvid staplarna. En ram i stamsidans barkstil med serverns namn, världsåldern och
  datumet överst. Varje stam med någon online har en vimpelrad i sin färg (namn, online/alla medlemmar, åldern som
  en ikon), och under den medlemmarna efter rank och namn: ansikte, rankmärke (★ Leader, ✦ Elder,
  • Member, ◦ Newcomer), namn, åldersikon och ping. Ikonerna (9x9, `scripts/gen_tab_age_textures.py`): Stone en
  knackad flinta, Bronze en bronsyxa, Iron en järnyxa (Steam ett kugghjul och Electric en blixt, för senare). Den egna stammen står först, sedan efter storlek och namn. Spelare
  utan stam står sist som **Wanderers** med sin egen ålder. Långa listor delas i jämna kolumner (en stam som delas
  upprepar sin vimpelrad) och ramen krymper om den inte får plats. Serverns egen header/footer visas i ramen.
  Servern skickar listan (`ClanNetwork.Roster`) vid inloggning, utloggning, stamändringar, färgbyte och när någon
  når en ny ålder. Utan den (eller när servern visar en scoreboard i listan) visas vanillas lista. Till skillnad
  från vanilla syns den även i singleplayer.
- **Multiplayer-sidan:** en server med moddet blir ett enda kort i Tab-listans barkstil i serverlistan. Hela raden
  (ikon, namn, MOTD, spelare, ping) ligger inne i ramen, och under en avdelare: världsåldern med ikon till vänster och
  serverns datum i årstidens färg till höger, antalet stammar, och sedan en ruta per stam i två kolumner (vimpel i
  stammens färg, namn, online/alla och åldersikon), **Wanderers** med antal och "+N more" om det finns fler än 6.
  Servern lägger datan som ett extra fält `"mechanical"` i sitt status-svar (`ServerListInfo`, uppdateras var 5:e
  sekund); vanilla-klienter hoppar över fältet, och MOTD:n är orörd. En server som svarat utan fältet behåller
  vanillas rad, men dämpad och märkt "Not running <modnamn>". En server som inte svarat än eller är offline ser ut som
  vanligt.
  - **Servernamn:** raden `server-name=AmandaTest` i `server.properties` (ingen vanilla-nyckel; vanilla behåller den när
    filen sparas om) blir kortets titel i stället för namnet spelaren skrev in när servern lades till. Saknas raden
    visas det inskrivna namnet. Läses en gång när servern startat (ändring kräver omstart), bara på dedikerade
    servrar, högst 64 tecken. Moddet skriver inte in raden själv: vanilla sparar om filen från sin egen kopia och
    skulle kunna tappa en rad som lagts till under körning, så lägg in den för hand.
- **Stammens mark** (byggd): en stams territorium sparas i stammen och räknas om när Fire Pit:en ändras eller byggs ut.
  - **5x5 chunks** (`Clans.LOAD_RADIUS` = 2 åt varje håll) runt Fire Pit:en, liten som stor. Hela rutan är forceloadad, så
    grödor och djur går vidare även när ingen är där.
  - **Stor Fire Pit:** stammens mark växer också över sin Fire Pit:s **biome**, så långt biomen hänger ihop med Fire Pit:en,
    högst 16 chunks åt varje håll (`Clans.TERRITORY_MAX`). Aldrig mindre än en liten Fire Pit:s 5x5. Det är marken som
    visas som Lands (zontiteln) och på kartan; forceloaden är fortfarande bara 5x5.
  - **Stammar överlappar aldrig.** Chunks en annan stam har, och NPC-land, tas inte. Att grunda en stam eller flytta
    Fire Pit:en så att dess 5x5 skulle ta någon annans mark vägras med meddelandet "Too close to X's land (to the <väderstreck>).
    Tribes' lands may not overlap: find a Fire Pit farther away." Chunks som frigörs när marken krymper delas inte ut
    till grannar.
  - Clan/Lands följer territoriet (inte längre fasta 3x3).
- **Adminpanel** (byggd, bara dedikerade servrar): en webbpanel inne i moddet på `http://localhost:8100`. Bara för
  admin: begäran måste ha `Host` `localhost` eller `127.0.0.1`. Konfigureras i `config/mechanical-local.toml` (en LOCAL config med en
  av/på-flagga och port). Flikar: **Overview**, **Players**, **Tribes** och **Map**. Kartan ritas live av de chunks
  spelare laddar, med stammarnas territorier ovanpå; kartplattorna sparas i `<värld>/mechanical_map/`.
- **Ranker:** **Leader** (en), **Elder** (högre rank, kan bjuda in), **Member** (vanlig) och **Newcomer** (ny medlem).
  Ledaren får göra allt: bjuda in, kicka, befordra och degradera. En Elder får bjuda in och kicka en Newcomer. En
  Newcomer blir Member automatiskt efter **7 speldygn**. Speldygnen räknas även när servern är tom, för då går tiden
  vidare i halv hastighet (se *Tom server* nedan).
- **Inbjudan** skickas från GUI:t av ledaren eller en Elder. Den inbjudna får ett meddelande och accepterar, antingen
  i sitt eget GUI eller genom att klicka i chatten.
- **Data:** en `SavedData` per värld, likt `CampAge`. Varje stam sparar namn, ledare, medlemmar (UUID), Fire Pit:ens
  position och alla advancements den låst upp.
- **Delade advancements:** när en medlem får en advancement (`AdvancementEarnEvent`) får alla andra medlemmar samma.
  Den sparas också i stammens lista, så att offline-medlemmar får den när de loggar in. Recept som låses upp av
  advancements följer med av sig själva.
- **Inträde:** spelarens och stammens advancements slås ihop åt båda hållen.
- **Utträde:** spelaren behåller allt.
- **Ingen friendly fire** mellan medlemmar.
- **Locator bar:** prickarna för andra spelare visas bara för medlemmar i samma stam (spectators ser alla). Spelare utan stam syns inte för någon.
- **Ålder:** räknas ut från advancements, alltså den högsta ålderns rot-advancement. Inget eget fält som kan hamna ur fas.
- **Spelare utan stam** räknas som en stam med en medlem när ålder räknas ut.
- **I koden:** `Tribe` används redan för NPC-stammar. Spelarstammarna får ett eget namn i koden, t.ex. `Clan`, men
  heter "stam"/"Tribe" i spelet.
- **Advancements:** "Form a tribe" och "Grow the tribe" (minst 2 medlemmar) i stenålderns flik.

## 2. Världsålder

- `världsålder = max(ålder för alla stammar)`. Sparas som högsta värde och sjunker aldrig, inte ens om stammar upplöses.
- Offline-stammar räknas, eftersom de är sparade.
- När världen går in i en ny ålder visas ett meddelande i chatten för alla, t.ex. "The world has entered the Iron Age".
- **NPC-stammar** ligger 0–1 ålder efter. Förslag: varje NPC-stam får vid skapandet en fast fördröjning på 0 eller 1, så
  att världen blir blandad. Deras utrustning (`LaterAgeGear`), handel och raider följer stammens ålder.
- **När en NPC-stam går upp en ålder** river den sina gamla byggnader och bygger nya i den nya ålderns stil. Stammen
  byter också ut sin utrustning. **Byggt** (`people/AgeRebuild.java`), se *Ombyggnad av NPC-byar* nedan.
- Ombyggnaden sker **mest när ingen spelare är i närheten**, för att minska lagg: byn byts då ut på en gång. Är en
  spelare nära byggs det synligt men långsamt, block för block.

### Ombyggnad av NPC-byar (byggd)

**Vilka bosättningar finns och vad händer med dem:**

| Bosättning | Hur den vet sin stam | Vid ny ålder |
|---|---|---|
| Stambyar från worldgen (`TribeVillageStructure`) | Stammens id följer av byns frö (`tribeOf(seed)`) | Rivs och byggs om |
| Läger som stammar bygger vid ett spelarläger (`Settlements`, sparade i `Neighbours`) | `Settlement.tribe` | Hyddorna (och ugn och kvarn) rivs och byggs om |
| Läger från worldgen (`TribeCamp`) | Bara folkets `group` | Byggs inte om: blocken är inte kända (slumpade, inte sparade) |
| Fiskeläger (`FishingCamp`) | Ingen stam, inget folk | Byggs inte om |
| Jaktgängens eldar (`SetUpCampGoal`) | Folkets `group` | Bara eld och stenring, inget att bygga om |

Utrustningen byts för alla (redan gjort i fas 3).

**Minnet:** en by minns åldern den står i. Stambyar noteras när chunken där byn började laddas (`ChunkEvent.Load`) i
`SavedData` `mechanical:village_ages` (centrum, frö, klimat, ålder, kvarvarande jobb). De genereras alltid i stenåldern.
Ett läger har fälten `age` och `rebuild` i `Settlement` (gamla sparfiler läses som ålder 0; nya läger får stammens ålder
direkt när de grundas). När stammens ålder (`WorldAge.npcAge`) är högre än byns planeras ombyggnaden **en gång** och åldern
sätts direkt. Jobben sparas, så en omstart mitt i fortsätter där den var och planerar inte om. Hoppar världen två åldrar
byggs byn om direkt till den senaste.

**Planen:** en lista jobb (`Neighbours.Job`: plats, stammens gamla block, nytt block). För en stamby är det skillnaden
mellan byns layout i den gamla och den nya åldern (layouten följer helt av frö, centrum, klimat och ålder): först rivs det
som försvinner (uppifrån och ned, marken under golvet lämnas), sedan byggs det nya i byggordning. För ett läger rivs
hyddorna i de etapper som är klara (deras 3x3-ytor, plus ugn och kvarn) och byggs upp igen på samma platser. Eld,
ställningar, förråd, banér och palissad står kvar. Ett läger byggs bara om när första etappen står och det inte är mitt i
en etapp, och nästa etapp planeras om i den nya stilen.

**Spelarblock rörs inte:** ett jobb görs bara om platsen fortfarande har stammens gamla block (samma blocktyp) eller är
fri (luft, växter, snö), eller är vanlig mark under golvet. Står ett spelarblock där hoppas jobbet över. För läger räknas
bara block lägret själv byggt (`Settlement.built`; ett block en spelare bryter tas bort därifrån), och en hydda vars plats
en spelare byggt i hoppas över helt.

**Nära och långt borta:** en plan görs först när hela byn är laddad (alla chunks inom 26 block för en stamby). Finns ingen
spelare inom 64 block görs alla jobb direkt, så som worldgen lägger block (inga drops, inga grannuppdateringar). Är en
spelare nära tar en medlem av stammen (inte jägare, på dagen) jobben ett i taget med `BuildCampGoal`: går dit, svingar en
halv sekund, river (med brytpartiklar) eller sätter blocket med ljud, och fortsätter med nästa. Går spelaren därifrån byts
resten ut på en gång. Ombyggnaden kostar lägret inget ur förrådet.

**Ritningar per ålder** (stambyar; samma plan och platser i alla åldrar, så porten och muren hittas som förut):

| | Stenåldern | Bronsåldern | Järnåldern |
|---|---|---|---|
| Långhus (tempererat, kallt) | Timmerväggar 2 högt, barktak, torv för försiktiga stammar och i kylan | En kurs högre på stenfot (Cobblestone, Mossy Cobblestone), korsvirke: Stripped Log-stolpar med lerklining (Packed Mud) emellan, Horn Window, gavlar av lera, halmtak (Hay Block) över barkfoten; torv i kylan och för försiktiga stammar | Som brons med en kurs till, två skift sten |
| Rundhyddor | Lera eller hudar, barktak; i kylan stenvägg under torv | En kurs högre på stenfot, lerväggar, halmtak (Hay Block) över barkfoten med halmtopp (raiders behåller rökstolpen); i kylan sten under torv | Som brons |
| Mammutbenhyddor (kallt) | Benbågar, hudar, snö | Rundhyddor (mammutarna är borta) | Som brons |
| Tältplatser | Hudtält, hudskydd (varmt), lövskjul (djungel) | Inhägnader 5x5 av staket med grind mot elden, Hay Block inuti i tempererat land | Som brons |
| Varmt | Lertegelhus, platta tak, hudskydd | En kurs högre, veranda med tak av Packed Mud, handlarnas hudtak blir lera | Som brons |
| Djungel | Pålhus av bambu, lövtak | Väggar av Jungle Planks i stället för bambu | Som brons |
| Arbetsplatser | Vedtrave, Fletching Table, Composter | + Clay Kiln och Quern vid samlarna | + Bronze Anvil och Furnace Bricks (smedja) |
| Palissad | Spetsade stockar 3–4 högt | Som sten | En stock högre |

Järnålderns material är tunna i modden (ingen järnflik, inga järnverktyg än), så järnåldern återanvänder brons med små
uppgraderingar: högre långhus och palissad och en smedja.

**Läger** (`Settlements`): stenålderns hudtält (hudskydd där det är varmt, lövskjul i djungeln) blir i brons- och
järnåldern små 3x3-hyddor: hörnstolpar av Stripped Log (bambu i djungeln), väggar av Packed Mud (Mud Bricks där det är
varmt, bambu i djungeln) med dörr, tak av Hay Block (platt Bark Roof i kylan, Packed Mud där det är varmt, löv i
djungeln). Etapp 2 får en Clay Kiln och en Quern vid elden. Palissaden är densamma i alla åldrar.

**Advancements** (bronsålderns flik): **Bronze Age Neighbours** (kom inom 40 block från en stamby eller ett läger som står
ombyggt i bronsåldern) och **Iron Age Neighbours** (järnåldern).

**Gametest** `tribe_rebuild`: layouter i alla klimat och 16 frön i brons- och järnåldern (porten öppen, ugn och kvarn,
inga benhyddor, smedja i järnåldern), en stenåldersby som byts till bronsålderns layout på en gång och exakt en gång med
två spelarblock orörda, och ett läger i tre etapper vars tält blir hyddor med ugn, en gång, med ett spelarblock orört.

**Inte gjort / gränser:** worldgen-läger (`TribeCamp`) och fiskeläger byggs inte om. En by genereras alltid i stenåldern
och byts sedan (först när hela byn är laddad), den ritas inte direkt i sin ålder. Den långsamma ombyggnaden har en byggare
per stam, så en spelare som bor precis vid en by kan få vänta länge (resten byts när hen går mer än 64 block bort).
Ombyggnaden nära spelare är inte gametestad (gametest har inga spelare). Snö på ett tak som rivs i det stora steget kan
bli hängande i luften.
- Senare kan världsåldern styra vilka strukturer som spawnar, och stammar som ligger efter kan få hjälp att komma ikapp
  via NPC-handel.

## 3. Tid och kalender

### Längre dygn
- Ett vanilla-dygn är 24000 ticks (20 min). Vi saktar ner klockan (`dayTime`) så att ett dygn tar 60 min, i snitt en
  tredjedel så fort. Himmel, sol och HUD följer med av sig själva eftersom klienten får tiden från servern.
- **Dag och natt varierar med säsongen**, i en mjuk kurva över året. Klockan går olika fort under dag och natt, så
  himlen ser rätt ut utan att vi rör renderingen:

  | Säsong | Dag | Natt |
  |---|---|---|
  | Sommar (mitt i) | 40 min | 20 min |
  | Vår / höst | 30 min | 30 min |
  | Vinter (mitt i) | 20 min | 40 min |

- **Viktigt:** bara klockan saktar ner. Allt som räknas i ticks (grödor, ugnar, uppväxt, hunger) går lika fort som förut
  och hinner därför 3 gånger mer per dygn. Det som ska kännas "per dag" måste räknas om:
  - Grödor: grundtillväxten skalas ner, sedan gäller säsongsfaktorn.
  - Matförstörelse (`Preserving`): räknas i dygn.
  - Kod som antar 24000 ticks per dygn finns i cirka 69 filer, bland andra `CampAge`, `Raids`, `BloodMoon`,
    `NightDangers`, `Tribesperson`, `TribeVisits`, `WildlifeRestock` och djuren. Alla gås igenom. En gemensam hjälpklass
    (`Calendar`) får ge dygn, timme, dag/natt, säsong och år.
- **Sova:** när alla spelare ligger i en Straw Bed (eller Furred Straw Bed) **spolas natten fram**. Klockan går då
  många gånger snabbare, så att natten tar cirka 20–30 s och himlen snurrar synligt. Om någon kliver ur går klockan i
  vanlig takt igen. Vila, sanity och värme från Furred Straw Bed finns kvar. **Byggt:** `StrawBedRest.nightRunsFast`
  avgör, och `Calendar.onLevelTick` sätter då klockans takt till `StrawBedRest.SPEED` (24 klockticks per tick, en natt på
  cirka 23 s) i stället för dygnets vanliga takt. Vanillas nattskip är fortfarande avstängt för halmsängar
  (`mixin/PlayerMixin`). Advancement **Night Spins By**. Matförstörelse och annat som räknas i speltid går som vanligt
  under en framspolad natt (bara ~23 s), så maten åldras knappt över natten.
- **Tom server:** **byggt (ändrat 2026-11-22, `living-world-plan.md` A1).** En dedikerad server pausar inte när den är tom
  (vanillas `pause-when-empty-seconds` sätts till 0 när servern startar, och raden i server.properties skrivs om), utan kör
  på **10 TPS i stället för 20** så länge ingen är online (`EmptyServer`). Hela världen går då i halv hastighet: klockan och
  därmed kalendern, säsongerna och världsåldern, och allt i de chunks som stammarnas Fire Pits håller laddade (grödor,
  ugnar, förskämning). Ett speldygn tar 2 h. Så fort någon loggar in går servern i 20 TPS igen. En takt som en op själv
  satt med `/tick rate` lämnas i fred (utom att 20 halveras när servern är tom). Singleplayer pausar som vanligt.

### Kalender
- 30 dagar per månad, 3 månader per säsong, 12 månader per år.
- Ny värld: dag 15 i vårens andra månad, år 1.
- **Månadsnamn**, förslag (engelska i spelet):

  | Säsong | Månader |
  |---|---|
  | Vår | Thaw, Sowing, Blossom |
  | Sommar | Midsun, Haymonth, Reaping |
  | Höst | Leaffall, Hunting, Frostfall |
  | Vinter | Deepwinter, Wolfmoon, Snowmelt |

### HUD
- Uppe till vänster: tid, dag, månad, år och en ikon för säsongen, t.ex. `☀ 14:30 · 15 Sowing · Year 1`.
- Måste samsas med `TemperatureHud`.
- Kommandon för op/test: `/calendar` (visa), `/calendar set <season|month|day>`.

## 4. Säsonger

### Temperatur (kärnan)
- En mixin på biomens temperatur lägger till en säsongsförskjutning som följer en mjuk kurva över året. Vintern sänker
  temperaturen globalt, men mest i tempererade och kalla biomes. Startvärden att finjustera:

  | Klimat (`ClimateZone`) | Vinter | Sommar |
  |---|---|---|
  | Kallt / tempererat (bas under 0.9) | −0.95 | +0.1 |
  | Varmt (bas 0.9–1.5: djungel, svampöar) | −0.3 | +0.1 |
  | Torrt (öken) | −0.2 | +0.1 |

  Vanilla snöar under 0.15. Med de här värdena (byggda, se fas 4) är skog och slätt vita hela vintern, men varma biomes
  får ingen snö.
- **Det här får vi nästan gratis från vanilla:** snö i stället för regn, snölager och isade sjöar (vanilla fryser
  ytvatten när det är tillräckligt kallt).
- **Spelarens temperatur** (`Temperature`) följer med. Kalla biomes på vintern skadar den som saknar skydd, och päls,
  eld och furred bed blir viktiga.

**Hus håller värmen (byggt):** väggar och tak runt spelaren isolerar som kläder, upp till 60 %. Från huvudet går
nio strålar, en uppåt och åtta åt sidorna (också snett), högst 8 block långt. Utan tak ger huset inget skydd. Annars
blir skyddet medelvärdet av vad strålarna träffar (en öppning eller öppen dörr räknas som 0):

| Värme | Block (taggar `mechanical:insulation/high`, `/medium`, `/poor`) |
|---|---|
| **Bra** (1.0) | Turf, Peat, Peat Bricks, Adobe Bricks, Mudstone, Rammed Earth, Wattle & Daub, Reed Thatch, alla Bark Roofs, Snow Block, Packed Mud, Mud Bricks, Hay Bale, Wool |
| **Ganska bra** (0.7) | stockar, plankor, trä-slabs, trä-stairs, trädörrar och trä-trapdoors, Loess och Loess Bricks |
| **Sten** (0.4) | alla andra solida block (sten, tegel, alla stenfamiljer) |
| **Dålig** (0.1) | glas, löv, is, Iron Bars, Wattle utan lera |

En eld inne i ett tätt hus värmer upp till hälften så mycket till. Exempel: en hydda helt av Turf ger 60 % isolering och
tillsammans med en päls (25 %) är vinternatten uthärdlig. Ett stenhus ger 24 %. Tooltippen på block säger "Keeps warmth in:
well / fairly / poorly". Kod: `Temperature.shelter`, taggarna från `scripts/gen_insulation.py`.

**Grottornas luft och hetta (byggt, fas 9C):** under y 50 utan himmel räknas grottluften efter djupet (`Temperature.cave`),
men två grottbiomer har egen luft: **Ice Caves** 2 (nära fryspunkten, på alla djup: utan päls fryser man) och **Magma
Vents** 30, långt över MAX 20. Kroppen kan nu bli **för varm**: över **17** tröttnar man (food exhaustion, upp till 0,5 per
sekund vid 20, som kylans hunger), över **19** brinner man (1 skada varannan sekund, typ `on_fire`, så Fire Resistance
skyddar). Bara Magma Vents kan värma kroppen över 17: sol (öken), Nether och eldar stannar vid 17, så inget som fanns förut
blir farligt. **Skydd:** att stå i vatten (eller regn) håller kroppen vid 17 även i Magma Vents. Kläder varken hjälper eller
stjälper i hettan (isoleringen gäller bara kyla), för att hålla det enkelt. Från 12 till 19 tar det knappt 30 s, så man hinner
in och ut. HUD:en visar röda termometrar över 17. Kylrum: kall luft bromsar förskämningen (`Preserving.coldRate`, se
01-stone-age.md "Förskämd mat"). Kod: `Temperature.cave(biom, y)`, `heatCap`, `OVERHEAT`/`SCORCHING`; gametest `environment`.

### Upptining (eget arbete)
- Vanilla smälter aldrig snö eller is. En egen tick tar bort snölager och is i laddade chunks när det blir varmt.
- **Chunks som inte är laddade:** chunken sparar sin senaste säsong och kommer ikapp när den laddas. Samma lösning
  används för kala träd och frysta bär. Det här är den största risken för prestanda.

### Väder
- Vinter: oftare nederbörd och snöstormar. Höst: mer regn. Sommar: mest klart, ibland åska. Vi styr vanillas
  vädertimers.

### Lövträd
- Sent på hösten tappar lövträden gradvis löven och blir kala grenar. På våren kommer knoppar och sedan löv igen.
  Barrträd (gran, tall, en) ändras inte.
- **Vinterväxter:** gräset frostas på vintern (blekt gråvitt, nästan inget grönt mitt i vintern). Buskar, bärbuskar som
  inte fryser och blommor (Hazel Shrub, Juniper, Bramble, Undergrowth, Sapling Thicket, Wild Carrot, Sea Beet,
  Stinging Nettle, bärbuskarna, Sweet Berry Bush och vanillas blommor) blir gråare och mattare från sen höst till tidig
  vår: ett filter över deras egna färger (`SeasonLook.frost`, `SeasonalFoliage.WINTER_PLANTS`, modellerna tintas av
  `scripts/gen_winter_plants.py`). Bärbuskar som fryser visar sin frusna modell som förut.
- **Snöstorm:** ett åskväder på vintern blir en snöstorm där det är kallt nog för snö. Snön driver in på fyra
  kolumner till per snötick och chunk, upp till 3 lager på öppen mark och 2 under träd (på marken under löven, som
  vanilla aldrig snöar på), men inte under tak (`Thaw.storm/drift`). Advancement: Whiteout.
- Gräs och löv byter färg efter säsong: grönt, sedan gult, orange och rött, och blekt på vintern. Färgerna ändras i
  ungefär 3 steg per säsong så att chunkarna bara behöver renderas om vid stegen.
- **Byggt (fas 7):** bara på klienten och bara i utseendet. Inga block byts och inget sparas, så det behövs ingen
  ikappkörning. Matematiken ligger i `SeasonLook.java` (testas i `CalendarTest`), ritandet i
  `client/SeasonalFoliage.java`.
  - **Färger, ett steg per månad (3 per säsong):** biomens egen färg dras mot månadens färg. Thaw: blekt (gräs) och
    ljust nytt grönt (löv). Sowing och Blossom: friskt grönt som klingar av. Midsun och Haymonth: biomens vanliga
    färg. Reaping: lite torrt. Leaffall: gult. Hunting: orange (gräset gulbrunt). Frostfall: rött (gräset brunt).
    Deepwinter till Snowmelt: blekt brunt gräs. Lövträd blir antingen gula (Birch, Aspen, Alder, Linden, Willow, Ash,
    Hazel) eller orange och sedan röda (Oak, Dark Oak, Cherry, Beech). Egna färgresolvers för gräs, Oak/Dark Oak/Vine
    och Birch, så färgerna blandas över biomgränser och cachas som vanillas. Gräs: Grass Block, Short/Tall Grass,
    Fern, Bush, Pink Petals, Wildflowers, Worn Grass, Trodden Path och Turf (block, slab, stairs: vanillas grå
    gräs-texturer med tintad topp och kantöverlägg). Reed (grå textur) är ljusgrön på våren, grön på sommaren, guld på
    hösten och halmfärgad på vintern; Heather mattas och brunnar från sen höst till tidig vår (`SeasonLook.reed/heather`). Löv utan tint (Cherry och våra träslag) ritas på
    hösten med en grå kopia av sin textur som tintas med höstfärgen.
  - **Kala träd:** lövblockens modeller lindas in (`ModelEvent.ModifyBakingResult`) och ritar kala grenar eller knoppar
    i stället för löv. Varje block har en egen tröskel (hash av positionen), så ett träd glesnar under 10 dagar från
    6 Frostfall och är helt kalt från 15 Frostfall. Det står kalt hela vintern. 11–20 Thaw kommer knoppar (rosa blom på
    Cherry) och 21–30 Thaw löven. Grentexturerna ritas av `scripts/gen_seasonal_leaves_textures.py` (fristående
    texturskript, ingen plats i kedjan) i trädets barkfärger.
  - **Lövträd:** Oak, Dark Oak, Birch, Cherry, Red/Orange/Yellow Poplar (kala men ingen färgändring, de är redan
    höstfärgade), Aspen, Alder, Beech, Linden, Willow, Ash och Hazel. **Ändras inte:** Spruce, Pine, Yew, Jungle,
    Acacia, Mangrove, Azalea, Pale Oak och Ancient Oak.
  - **Klimat:** i biomer med bastemperatur från 0.95 (djungel, savann, öken, badlands) ändras varken färg eller löv.
    I biomer som är snöiga hela året (under 0.1) ändras inte färgerna, men lövträd blir ändå kala. Nether och End
    ändras inte.
  - **Omritning:** när månaden byts, eller en dag medan löven faller eller slår ut, töms tint-cachen och sektionerna
    runt spelaren byggs om på plats (`LevelExtractor.setSectionRangeDirty`, ingen tom värld som vid `allChanged`).
    Det händer högst runt 30 gånger per år.
  - **Begränsningar:** kala löv är fortfarande lövblock. Kollision, drops, lövförfall och fallande lövpartiklar är
    oförändrade, och partiklarna när man slår sönder ett kalt block ser ut som löv. Med Cutout Leaves avstängt (snabb
    grafik) ritar vanilla alla löv ogenomskinliga, så kala träd syns då som mörka block. Ingen advancement, eftersom
    det bara är utseende.

### Grödor och mark
- Varje gröda har en **säsongsprofil som data**: en tillväxtfaktor per säsong och eventuellt frostkänslighet (dör
  under en viss temperatur). Exempel: vår 1.0, sommar 1.2, höst 0.6, vinter 0.
- Marktyp kan ändra värdena (växthus, kompost och liknande längre fram).
- Nya grödor och marktyper behöver bara en egen rad i datan.
- Uppdatera `docs/farming.md` när det byggs.
- **Byggt:** säsongsprofilen ligger i grödans rad i `Farming.climate` (`Climate.seasons`, vår/sommar/höst/vinter;
  standard 1.0/1.2/0.6/0) och multipliceras på tillväxtchansen ovanpå temperaturen. Frostkänsligheten är den befintliga
  frostgränsen. Tabellen per gröda står i farming.md. Fältinfon visar "resting till spring" på vintern. Advancement
  **Out of Season**. Marktyper är inte byggda.

### Bär
- **Vintertåliga** (förslag): Lingonberry och Juniper Berries. De står kvar och kan plockas på vintern.
- **Övriga** (Raspberry, Blueberry, Wild Strawberry, Elderberry, Cloudberry, Blackberry): får ett **fruset** stadium
  på vintern. Busken minns sitt stadium, kan inte plockas, och växer vidare från samma stadium när det blir varmt.
- **Byggt:** blockstate `frozen` på `WildBerryBushBlock`. En buske är frusen när det är vinter **och** biomens temperatur
  (med säsong) där är under 0.2 (`Thaw.MELT_AT`, där snön inte smälter). Säsongen ensam skulle frysa buskar i öknen;
  temperaturen ensam skulle hålla buskar i snöbiomer frusna hela året. Kollas på random tick (även mogna buskar tickar
  nu) och när någon plockar, så en buske i en chunk som sovit hela vintern ger inget. Vintertåliga: `Foraging.WINTER_HARDY`
  (Lingonberry); Juniper är en egen buske som aldrig fryser. Frostiga texturer från `gen_berry_textures.py`. Björnar och
  samlare plockar inte frusna buskar. Advancement **Winter Larder**.

### Djur
- På vintern rör de sig långsammare, vandrar mindre och parar sig inte.
- **Björnar går i ide** över vintern i en håla eller grotta. De sover och vaknar om man stör dem (då är de farliga).
- **Flyttfåglar** (tranor, hägrar och andra) flyger söderut på hösten, försvinner över vintern och kommer tillbaka på
  våren.
- **Byggt** (`WinterAnimals.java`): vilda djur (`WildAnimal`) går 20 % långsammare på vintern och strövar en tredjedel
  så ofta. Inga djur parar sig på vintern (vanilla, byns och tama djur som Tame Horse): paret tappar kärleken och den
  som matade dem får "Too cold: animals don't mate in winter". **Brown Bear och Cave Bear går i ide:** på vintern letar
  en björn utanför ett ide upp en mörk plats under sten i närheten (himmelsljus högst 4) och sover där tills våren.
  Hittar den inget efter ett tag sover den där den är. Väcks den anfaller den (Cave Bear ryter först). På våren vaknar
  Brown Bear och går ut igen; Cave Bear ligger i sin lya året runt som förut. **Flyttfåglar:** Common Crane, Grey
  Heron, Greylag Goose och Mute Swan. Från sista höstmånaden (Frostfall) till vårens början lyfter flockarna på dagen
  och flyger söderut, och försvinner när ingen spelare är nära (eller efter en minut). Under tiden spawnar de inte och
  fylls inte på. På våren kommer de tillbaka med djurpåfyllningen. Mallard och övriga fåglar stannar. **Djurpåfyllningen
  halveras på vintern** (hälften av områdena får inget). Advancements i Wilderness: **Rude Awakening** (väck en björn
  ur vintersömnen, under First Frost) och **The Cranes Return** (se flyttfåglarna komma tillbaka på våren, under
  Through the Winter). `WinterAnimalsTest` testar flyttiden, vilka arter som flyttar och att ingen kalv föds på vintern.
- NPC-stammar samlar förråd inför vintern.

### Mat och bunkring
- Färre bär och mindre vilt på vintern gör att man måste bunkra. Rökning och torkning (`Preserving`) och Storage Jar
  blir viktiga.
- **Hunger med längre dygn:** man äter fler gånger per dygn än förut, men bättre och större mat ger mer mättnad
  (saturation) som räcker längre. Råa bär och små bitar ger lite. Tillagat kött ger mer. Måltider som gröt, stuvning
  från Kettle och bröd ger mest. Det gör matlagning värd mödan. **Byggt:** tabell i `FoodValues.java` (rått 0,1,
  tillagat/konserverat 0,8, delikatess 1,0, måltid 1,2 i mättnadsmodifierare; hungern är oförändrad), se
  01-stone-age.md "Mättnad". Hungern sjunker lika fort som i vanilla. Advancement **Square Meal**.

### Advancements
- Advancements som "First Frost", "Survive your first winter" och "A full year", i en lämplig ålders flik.

---

## Ordning att bygga

1. **Kalender + längre dygn + HUD + `/calendar`** (byggd, v0.81.0). `Calendar.java` sätter världsklockans takt
   (NeoForge `setRate`) två gånger per dygn efter säsongens dag- och nattlängd. Kalendern läses av klockan (inget
   sparas), så en befintlig värld får ett datum direkt. `Calendar.CLOCK_DAY` (24000 klockticks) används för tid på
   dygnet och dagnummer. `Calendar.DAY` (72000 ticks) används för varaktigheter: matförstörelse, kadaver, Camp Age,
   NPC-besök och bosättning, ilska, lokalbefolkningens band, uttern, ödlans svans, bävern och djurpåfyllning. Grödornas
   tillväxtchans är delad med 3, så att de växer lika mycket per dygn som förut. Det som läser klockan (natt, Blood
   Moon, vind, nattfaror, raider, djurens dygnsrytm) blev rätt utan ändring. Vanilla-djurens uppväxt och
   korta timers (sekunder till minuter) är oförändrade. `CalendarTest` testar matematiken. Lang:
   `scripts/gen_calendar.py`.
2. **Spelarstammar** (byggd, v0.85.0): oförstörbar Fire Pit, ledare, ranker, stam-GUI med inbjudningar och röstningar,
   delade advancements, stammens ålder, ingen friendly fire. Kod: `clan/Clans.java` (data, regler, händelser),
   `clan/ClanNetwork.java` (paket och vy), `client/clan/ClanScreen.java` (sidan, G). `ClanTest` testar röstreglerna och
   åldern. Lang: `scripts/gen_clans.py`. Advancements: Hearth of the Tribe och Kin (Stenåldern, under Bonfire).
   Förenklingar: inbjudningar sparas inte över en omstart; sidan visar 7 medlemmar per sida.
   Straw Bed som spolar fram natten och ny mättnad per mattyp är också byggda (v0.85.0).
3. **Världsålder + NPC-stammars fördröjning** (byggd, v0.86.0). `WorldAge.java` sparar den högsta ålder någon stam eller
   ensam spelare nått och sjunker aldrig. En ålder nås med samma markörer som karavanerna redan använde: Bronze Age när
   man gjort Bronze, Iron Age när man gjort Wrought Iron (stammens ålder räknas nu likadant, inte längre från flikens
   första advancement). Alla får ett meddelande när världen går in i en ny ålder. Varje NPC-stam ligger 0 eller 1 ålder
   efter (`Tribe.ageLag()`, fast utifrån stammens id). Efter stammens ålder: verktyg och vapen för jägare, vakter,
   samlare, vedhuggare och raiders (bronsspjut, bronskniv, bronsyxa; järnverktyg finns inte än) och karavanernas vakter
   och varor (tidigare efter spelarna i närheten). Monster får behålla kopparutrustning från bronsåldern och järn och
   ringbrynja från järnåldern (`LaterAgeGear`). `/worldage` och `/worldage set <0-2>` för op.
   NPC-byar och lägren stammarna bygger rivs och byggs om i den nya ålderns stil (byggd, `AgeRebuild`, se *Ombyggnad av
   NPC-byar* under 2. Världsålder). Worldgen-läger och fiskeläger byggs inte om.
4. **Säsongstemperatur** → snö, isade sjöar, spelarkyla, väder (byggd, v0.87.0). `Seasons.java` + `mixin/BiomeSeasonMixin`
   förskjuter varje bioms temperatur längs en mjuk kurva över året: +0.1 mitt i sommaren, mitt i vintern −0.95 i
   tempererade och kalla biomes (bas under 0.9), −0.3 i varma (under 1.5, t.ex. djungel 0.95) och −0.2 i torra. Vanilla
   ger då snö i stället för regn, snötäcke och isade sjöar. **Ändrat v0.186.0** (var −0.8 och bas under 1.0): med −0.8
   snöade det på slätten (0.8) bara ~20 % av året och i skogen (0.7) ~26 %, inte hela vintern. Vintern är kvartalet
   runt midvintern där kurvan är under −0.707, och 0.8 − 0.95 × 0.707 = 0.13 < 0.15: allt tempererat upp till slätten
   ligger under snögränsen hela vintern (slätten från ungefär två dagar före vintern till två dagar in i våren) och
   smälter inte (under 0.2). Djungeln räknas nu som varm, annars hade den fått snö mitt i vintern. Kroppstemperaturen
   blir 1.2 grader kallare mitt i vintern på slätten (dag 4.8 i stället för 6, natt under fryspunkten som förut); vårens
   första månad blir upp till 0.85 grader kallare, så vete sått före Sådd kan frysa i skogen en natt; i Sådd (dag 30)
   är det som förut (skog dag 9.6, natt 4.6, över frostgränsen 2). Kroppstemperaturen (`Temperature.ambient`) följer samma förskjutning, så
   vinternätter blir farliga utan päls eller eld, och grödornas temperaturgränser (frost) följer med. Vädret: varje ny
   regn- eller uppehållsperiod som vanilla slumpar skalas efter säsong (regn ×1.0/0.7/1.3/1.2, uppehåll
   ×0.9/1.4/0.6/0.8 för vår/sommar/höst/vinter). Advancements i Wilderness-fliken: First Frost, Through the Winter,
   A Full Year. **Snö och is smälter inte än:** det är fas 5.
5. **Upptining** + ikappkörning när chunks laddas (byggd, v0.89.0). `Thaw.java` + `mixin/ServerLevelThawMixin`: vanillas
   snö- och istick (en slumpad kolumn per chunk då och då) smälter också. Där temperaturen (med säsong) är minst 0.2
   går ett snölager ner ett steg och is vid ytan blir vatten, så upptiningen kryper fram under några minuter. Chunks som
   laddas en ny kalenderdag körs ikapp när deras grannar är laddade (4 per tick): varmt ger att all snö och is smälter
   direkt, snöväder ger ett tunt snötäcke, så en skog man besöker först mitt i vintern är redan vit. Varje chunk minns
   dagen (`Thaw.UPDATED`). Hav fryser bara där de är kalla hela året (inga säsongsisar på havet); sjöar och älvar fryser.
6. **Grödor och bär** (profiler, frysta stadier) och **djur** (vinterlugn, ingen parning, björnar i ide, flyttfåglar): byggda, v0.90.0. `WinterAnimals.java`, säsongsprofiler i `Farming.climate()`, frysta buskar i `WildBerryBushBlock`.
7. **Färger + kala träd** (byggd, fas 7). Bara på klienten: färgresolvers för gräs och löv med ett steg per månad, och
   inlindade lövmodeller som ritar kala grenar eller knoppar efter dag och position. Se "Lövträd" under 4. Säsonger.

Varje fas: advancements, `mod_version`-bump, registry och anteckningar, ålders-dokument, och ett laddtest
(`runGameTestServer`) innan push. Kalendermatematiken får ett eget gametest.

## Öppna frågor

Inga just nu. Alla frågor besvarades 2026-11-20.
