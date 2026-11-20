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
| Ranker | Leader, Elder (högre rank), Member (vanlig) och Newcomer (ny medlem). Ledare och Elder kan bjuda in. |
| Advancements | Delas av hela stammen. Vid inträde slås spelarens och stammens advancements ihop **åt båda hållen**. Den som lämnar behåller allt. |
| Stammens ålder | Samma för alla medlemmar: den högsta ålder stammen nått. |
| Max antal medlemmar | Inget. |
| Skada inom stammen | Nej (ingen friendly fire). |
| Stamchatt, gemensamma kistor, hemplats | Väntar. |
| Världsålder | Den högsta åldern bland alla stammar. Sjunker aldrig. |
| NPC-stammar | Följer världsåldern, men 0–1 ålder efter. När en NPC-stam går upp en ålder river den sina gamla byggnader, bygger nya och byter utrustning. |
| Dygn | 60 min: i snitt 30 min dag + 30 min natt. Längden på dag och natt varierar med säsongen. |
| Kalender | 30 dagar/månad, 3 månader/säsong, 4 säsonger/år (360 dagar). En säsong = 90 h, ett år = 360 h. |
| Start | Mitt i våren (dag 15 i vårens andra månad). |
| Sova bort natten | När **alla** spelare ligger i en Straw Bed spolas natten fram. Himlen snurrar synligt, och natten är över på cirka 20–30 s. |
| Hunger | Bättre och större mat ger mer mättnad som räcker längre. |
| Tid när servern är tom | Står still (vanilla-standard). Ingen missar vintern, och mat ruttnar inte medan man är borta. |
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
  knappar för att bjuda in, befordra, degradera och kicka, samt pågående röstningar.
- **Ranker:** **Leader** (en), **Elder** (högre rank, kan bjuda in), **Member** (vanlig) och **Newcomer** (ny medlem).
  Ledaren får göra allt: bjuda in, kicka, befordra och degradera. En Elder får bjuda in och kicka en Newcomer. En
  Newcomer blir Member automatiskt efter **7 speldygn**. Speldygn räknas bara när servern är igång, eftersom tiden
  står still när den är tom.
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
- **När en NPC-stam går upp en ålder** river den sina gamla byggnader och bygger nya i den nya ålderns stil. Det sker
  med samma byggbeteende som redan finns (`BuildCampGoal`, `SetUpCampGoal`). Stammen byter också ut sin utrustning.
  Varje ålder behöver alltså egna byggnadsritningar för NPC-byar.
- Ombyggnaden sker **mest när ingen spelare är i närheten**, för att minska lagg: byn byts då ut i större steg, t.ex.
  när chunken laddas. Är en spelare nära byggs det synligt men långsamt, block för block.
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
  vanlig takt igen. Det ersätter dagens beteende i `StrawBedRest`, där natten fortsätter som vanligt. Vila, sanity och
  värme från Furred Straw Bed finns kvar. Matförstörelse räknas i dygn, så maten åldras även över en framspolad natt.
- **Tom server:** tiden står still.

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
  | Kallt / tempererat | −0.7 | +0.1 |
  | Varmt | −0.3 | +0.1 |
  | Torrt (öken) | −0.2 | +0.1 |

  Vanilla snöar under 0.15. Med de här värdena snöar det på vintern i skog och slätt, men inte i varma biomes.
- **Det här får vi nästan gratis från vanilla:** snö i stället för regn, snölager och isade sjöar (vanilla fryser
  ytvatten när det är tillräckligt kallt).
- **Spelarens temperatur** (`Temperature`) följer med. Kalla biomes på vintern skadar den som saknar skydd, och päls,
  eld och furred bed blir viktiga.

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
- **Teknik som behöver provas:** antingen en egen blockstate "bare" på lövblocken (mixin på vanilla-löv) eller att
  löven byts mot egna "bare branches"-block som minns trädtyp och avstånd. Förändringen drivs av random ticks och
  ikappkörning när chunken laddas.
- Gräs och löv byter färg efter säsong: grönt, sedan gult, orange och rött, och blekt på vintern. Färgerna ändras i
  ungefär 3 steg per säsong så att chunkarna bara behöver renderas om vid stegen.

### Grödor och mark
- Varje gröda har en **säsongsprofil som data**: en tillväxtfaktor per säsong och eventuellt frostkänslighet (dör
  under en viss temperatur). Exempel: vår 1.0, sommar 1.2, höst 0.6, vinter 0.
- Marktyp kan ändra värdena (växthus, kompost och liknande längre fram).
- Nya grödor och marktyper behöver bara en egen rad i datan.
- Uppdatera `docs/farming.md` när det byggs.

### Bär
- **Vintertåliga** (förslag): Lingonberry och Juniper Berries. De står kvar och kan plockas på vintern.
- **Övriga** (Raspberry, Blueberry, Wild Strawberry, Elderberry, Cloudberry, Blackberry): får ett **fruset** stadium
  på vintern. Busken minns sitt stadium, kan inte plockas, och växer vidare från samma stadium när det blir varmt.

### Djur
- På vintern rör de sig långsammare, vandrar mindre och parar sig inte.
- **Björnar går i ide** över vintern i en håla eller grotta. De sover och vaknar om man stör dem (då är de farliga).
- **Flyttfåglar** (tranor, hägrar och andra) flyger söderut på hösten, försvinner över vintern och kommer tillbaka på
  våren.
- NPC-stammar samlar förråd inför vintern.

### Mat och bunkring
- Färre bär och mindre vilt på vintern gör att man måste bunkra. Rökning och torkning (`Preserving`) och Storage Jar
  blir viktiga.
- **Hunger med längre dygn:** man äter fler gånger per dygn än förut, men bättre och större mat ger mer mättnad
  (saturation) som räcker längre. Råa bär och små bitar ger lite. Tillagat kött ger mer. Måltider som gröt, stuvning
  från Kettle och bröd ger mest. Det gör matlagning värd mödan.

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
   Förenklingar: inbjudningar sparas inte över en omstart; sidan visar 7 medlemmar per sida. Straw Bed som spolar fram natten och ny mättnad per mattyp passar också här.
3. **Världsålder + NPC-stammars fördröjning.** NPC-byar som byggs om är ett eget, större steg, eftersom varje ålder
   behöver egna byggnadsritningar.
4. **Säsongstemperatur** → snö, isade sjöar, spelarkyla, väder.
5. **Upptining** + ikappkörning när chunks laddas.
6. **Grödor och bär** (profiler, frysta stadier) och **djur**.
7. **Färger + kala träd.**

Varje fas: advancements, `mod_version`-bump, registry och anteckningar, ålders-dokument, och ett laddtest
(`runGameTestServer`) innan push. Kalendermatematiken får ett eget gametest.

## Öppna frågor

Inga just nu. Alla frågor besvarades 2026-11-20.
