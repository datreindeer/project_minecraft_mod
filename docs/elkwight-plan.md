# Elkwight: plan

En svår boss i en nedbrunnen skog långt från spawn: en minotaurlik jätte med **älghuvud och en enorm hornkrona**, som en förvriden
skogens kung. Gröna runor och lysande gröna ögon ger den nordiska tonen. Hans yxa i okänd metall är en ledtråd som går att
undersöka senare. Byggs fas för fas, som
[underground-plan.md](underground-plan.md).

## Beslutat
- **Älghuvud**, inte tjur. Namnet är **Elkwight** (entity-id `elkwight`).
- **Får aldrig fastna**: en boss som fastnar blir lätt att döda (se 3, "Fastnar aldrig").
- **En Elkwight per ruin.** Han kommer inte tillbaka när han har dödats. Ruinerna är sällsynta, så den som vill slåss igen
  får resa vidare.
- **Minst 2500 block från 0,0** (både biomet och ruinerna). Ingen fast punkt: platsen beror på seed.
- **Skalas med spelare**: mer liv (och lite mer skada) för varje spelare inom 32 block när striden börjar.
- **Yxan är bara en trofé nu**: ett tungt föremål som dras och kan ställas ut. Att undersöka den kommer senare.
- Syns för alla (inte bunden till Faith).
- **Gröna runor och ögon**: ögonen lyser ljusgrönt i mörkret (emissivt lager, som AngryEyesLayer). Runor i samma gröna
  sken är ristade både på yxan och i ruinens stenar runt om.
- **Despawnar aldrig** (persistent), inte heller när ingen är i närheten eller när chunken laddas ur.
- **Läker bara genom att äta**: ingen egen återhämtning. Dör en spelare nära honom äter han av den döda och får tillbaka liv.
- **Svårighet (2026-11-22)**: ungefär som en Warden, med omkring 400 liv och hårda slag. Det är tänkt för 2–3 spelare, men en ensam
  spelare med stål klarar det med rätt taktik. Skalningen per spelare läggs ovanpå.
- **Biomet är sällsynt** bortom 2500 block, ungefär lika vanligt som en svampö. Ruinerna är sällsynta inne i biomet.
- **Ljud**: vanilla-ljud (Ravager, Warden och Hoglin) med ändrad tonhöjd används som platshållare, så att förvarningarna hörs.
  CC0-länkar från freesound listas så att ljuden kan bytas ut senare.
- **Slaktprodukter**:
  - Hornkronan blir en väggtrofé.
  - Huden blir en **Elkwight Cloak** för mantelplatsen, med värme och lite skydd.
  - Benen blir en **Bone Charm** för charm-platsen.
  - Claude bestämmer detaljerna och dokumenterar dem.
  - **Detaljerna (2026-11-23)**: Elkwight Hide (alltid 2) + 2 Sinew + Cordage blir **Elkwight Cloak**: 4 rustning, 1 seghet,
    varm som päls (25 %, taggen `fur_insulation`), lagas med Elkwight Hide. 3 Elkwight Bone (3–4 per kropp) runt en Gold
    Nugget under Cordage blir **Bone Charm**: +2 hjärtan och 10 % knockback-motstånd i Charm-platsen (det första föremålet
    där). Hornkronan (alltid 1) är själva väggtrofén **Elkwight Antler Crown** (sätts mot en vägg, 3 block bred, så stor en
    blockmodell kan vara). Köttet är vanliga Game Meat Scraps (10–14), plus Sinew.
- **Kniv för huden (2026-11-23)**: kroppen kan bara slaktas med en kniv av **nivå 4**, över järn och stål (Ancient Ritual
  Knife från grenen feat/p10oak). Med sämre knivar står det "This hide is too tough for your knife". Kollen går genom
  `KnifeItem#tier` (`Butchery.requireKnife`), så den nya kniven fungerar så fort den finns.
- **Ingen spårning av byggda block finns i modden**: "naturligt" betyder blocktaggen `mechanical:elkwight_breakable`
  (sten, djupskiffer, jord, sand, grus, snö, lera, basalt, blackstone, magma, löv) plus att löv inte får vara
  spelarplacerade (vanillas PERSISTENT) och stammar bara räknas som del av ett naturligt träd. Kullersten, plankor, tegel
  och allt annat byggt klarar sig. Rå sten eller jord som en spelare byggt med räknas dock som naturligt (genväg, noterad
  i koden). Biomgrenens brända stammar och aska ska läggas i samma tagg.
- **Advancement-namn**: "Many Hands" fanns redan (dra en stor kropp tillsammans), så Elkwightens heter **All Hands**.

## 1. Biomet: Scorched Barrens (nedbrunnen skog)
- Nytt ytbiom, bara längre än 2500 block från 0,0. En skog som brunnit: svartbrända stammar (stående, fallna, stubbar),
  aska (eget askblock, Coarse Dirt, Gravel), hällar av Blackstone och Basalt, inget gräs. Kalla, mörka färger på himmel
  och dimma.
- Inga djur utom asätare (korpar) och möjligen enstaka mörkrets väsen på natten.
- Block reagerar på klimatet som övriga block (frost och snö på vintern).
- Gametest: biomet finns inte inom 2500 block.

## 2. Ruinen
- Struktur (jigsaw eller NBT): stor raserad arena **med breda öppningar och högt i tak** (hornkronan är ~4 block bred), med pelare, trappor och murar av **Blackstone av olika slag** (polished,
  bricks, cracked bricks, chiseled, gilded), Basalt och Magma. **Runstenar och ristade block med lysande gröna runor** (eget block, emissiv textur, svagt ljus) i murar, pelare
  och kring arenan. Eldar som brinner (Soul Campfire, brinnande block i skålar).
- Sällsynt inne i biomet (stor spacing/separation), alltid minst 2500 block från 0,0.
- **Dimma när man kliver in**: tät och mörk dimma på klienten inom ruinens gränser, som tunnas ut när bossen är död.
- Elkwighten väntar i mitten och vaknar när en spelare kommer in i arenan. Bossbar.
- Kistor med lite guld och bronsföremål. Ingen kista med det bästa: belöningen är kroppen och yxan.

## 3. Elkwight (entity `elkwight`) (byggd, 2026-11-23)
- Människokropp med **älghuvud och stor hornkrona** (skovelhorn, ca 4 block brett), **2× spelarens storlek** (ca 1,2 × 3,6
  block). Hornkronan är bara utseende: hitboxen är kroppen, så kronan aldrig hakar i väggar och tak. Bär den dubbelbladiga yxan
  med båda händerna.
- Mycket liv, hög rustning, immun mot knockback och fallskada. Skalas per spelare (se Beslutat).
- **Despawnar aldrig**: persistent från start, sparas med ruinen. Lämnar man striden ligger skadan kvar tills man kommer tillbaka.
- **Äter de döda**: dör en spelare inom ~16 block går han till platsen, sätter sig på knä och äter i ~3 sekunder. Han får
  tillbaka **25 % av sitt maxliv** per död spelare. Under måltiden står han still och tar extra skada, och avbryts han
  (tillräcklig skada medan han äter) får han bara den del han hunnit äta. Spelarens föremål ligger kvar på marken.
- Varje attack förvarnas tydligt (ljud + pose) så att man kan lära sig mönstret.
- **Fastnar aldrig**: kan han inte nå spelaren (hål, torn av block, bakom murar, i vatten) gör han något annat i stället:
  hoppar upp eller ner (språng på ~6 block), slår sig igenom naturliga block, löv och brända stammar, eller använder
  yxkast och markvåg. Står han still utan väg i några sekunder teleporteras han inte, utan går över till avståndsattacker.
  Gametest: han når en spelare som står på ett torn och en som gömmer sig i ett hål.

**På nära håll**
- **Snurr**: snurrar med yxan och slår alla runt om sig (radie ~4). Förvarning: drar yxan bakåt och stampar.
- **Hornsvep**: sänker hornkronan, skrapar med hoven och rusar fram; kronan träffar brett (~3 block). Rusar han in i en
  vägg vacklar han i 1 sekund (ett kort läge att slå, inte ett fast läge).

Byggt så här (`wildlife/elkwight/Elkwight.java`, modell `client/wildlife/ElkwightModel.java`):
- Vaknar när en spelare (inte creative/spectator) kommer inom **20 block i synhåll**, när han blir slagen, eller när ruinen
  anropar `wake()` (`isAwake()` finns också). Är ingen spelare inom 48 block på 30 s somnar han igen och går hem; skadan
  ligger kvar.
- Skalning när han vaknar: **400 liv** +60 % per extra spelare inom 32 block, +10 % skada per extra spelare (andelen liv
  behålls, staplas inte). Rustning 12, seghet 4, knockback-immun, fallskadeimmun, eldimmun, persistent.
- Förvarningar (pose + ljud): **Hugg** (yxan över huvudet, Ravager-attack), **Snurr** (yxan bakåt + stamp med
  Warden-nedslag, sedan två varv: 16 skada inom 4 block), **Hornsvep** (kronan sänks, hoven skrapar med grusljud och
  damm, Hoglin-ilska; rusar ~16 block, 20 skada 3 block brett; vägg = vacklar 1 s, löv och snö ger vika).
- Äter: 60 tick, läker 25 % av maxlivet jämnt över måltiden, tar 1,5x skada, 8 % av maxlivet i skada avbryter (då
  vacklar han). Spelarens föremål ligger kvar.
- Fastnar aldrig: utan väg i 3 s hoppar han (upp till 8 block upp, 12 ner, rakt upp intill ett torn och styr över
  kanten), annars slår han sig igenom naturliga block (rakt ner när han står över målet), annars kommer yxkast och
  markvåg tätare. Ingen teleport.
- Gametests (`ElkwightTest`): når en spelare på ett torn av kullersten (hopp) och en i ett schakt i naturlig sten
  (gräver), skalning, äter och läker 25 % (avbruten: bara en del).

**På avstånd** (man ska inte kunna hålla sig undan) (byggt, 2026-11-23)
- **Yxkast**: kastar yxan som snurrar mot spelaren och kommer tillbaka (eller går och hämtar den: fönster att slå till).
- **Markvåg**: slår yxan i marken; en våg av sprickor rusar mot spelaren, kastar upp den och skadar. Naturliga block i
  vågens väg spricker (sten blir grus/cobble, träd fälls med FallingTree), men aldrig byggda block, så ingen bas förstörs.
- **Mot den som flyr långt** (> 24 block): stöter mer och kastar oftare.
- Byggt: yxkastet (`ThrownElkAxe`) flyger 1,1 block/tick till 4 block bortom målet (14 skada) och vänder; träffar det ett
  block fastnar det och han går dit och drar loss det (12 tick). Försvinner yxan (oladdad) får han tillbaka den. Markvågen
  (`ElkwightTerrain`) går 24 block, 3 block bred, följer marken upp/ner, 12 skada och kastar upp; naturlig sten blir
  kullersten (grus var tredje), naturliga träd fälls med FallingTree bort från honom. Bortom 24 block räknas
  nedkylningen för hornsvep och kast dubbelt så fort.

## 4. Kroppen och yxan (byggd, 2026-11-23)
- Han lämnar en **Carcass** som är ännu tyngre än de stora djuren: ny nivå "kolossal". 1 spelare kan inte rubba den,
  2 drar den extremt långsamt, 3–4 i normal takt. Går att lägga på vagn eller travois. Slaktas som andra (hud, kött, horn,
  ben) med egna produkter.
- **Yxan** är ristad med samma gröna runor (glöder svagt även som trofé). Den landar bredvid som ett eget tungt föremål (entity, drar som en carcass): minst 2 spelare för att rubba den, går
  att lägga på vagn eller travois. Kan ställas ut som ett block (trofé). Senare: **undersöka** den (metallen, dess ursprung).
- Systemet för tunga föremål byggs så att fler saker än carcasser kan dras på samma sätt.
- Byggt: `HeavyLoad` är basklassen för allt som dras (grepp, dragare, travois-platser, `pullersNeeded`/`pullersToBudge`);
  `CarcassEntity` och `ElkwightAxeEntity` ärver den och `CarcassHauling` drar vilken som helst. Kolossal kropp:
  `Butchery.colossal`, 1 spelare rubbar den inte ("It won't budge"), 2 kryper (10 % x 2/3), 3–4 normalt; 3 platser på
  travois/vagn, inte på Hanging Rack. Yxan: 2 rubbar den och drar normalt, 2 platser. Högerklicka ovansidan av ett block
  medan man drar den: den ställs upp som blocket Elkwight's Axe (lyser 4, runorna glöder); bryts det ligger yxan där igen.

## 5. Advancements (Elkwightens byggda, 2026-11-23; *Into the Ashes* hör till biomgrenen)
- *Into the Ashes* (gå in i Scorched Barrens), *The Smoke Clears* (döda Elkwighten), *Heavy Burden* (dra hem yxan),
  *Many Hands* (bär kroppen tillsammans med 3 andra).
- Byggt i Wilderness-fliken under *Wyrmslayer* (flyttas under *Into the Ashes* när biomgrenen slås ihop): **The Smoke
  Clears** (döda honom), **Heavy Burden** (ställ upp yxan som trofé), **All Hands** (dra kroppen med 3 andra), **Mantle of
  the Wight** (Elkwight Cloak), **Bone Charm**, **Crowned** (häng upp hornkronan). Elkwighten finns också i Hunting-fliken
  (Predators, utmaning).

## Ljud
Platshållare nu (vanilla, tonhöjd sänkt): Ravager (röst, hugg, steg, vrål före kast och hopp), Warden (vaknar, stamp och
nedslag, laddning före markvågen, död), Hoglin (ilska före hornsvep, smackande vid måltiden), Trident (kast och yxa som
fastnar). CC0-ljud från freesound att byta till senare (licensen kontrollerad som Creative Commons 0):
- Röst/vrål: [CyclopsMonster av jonccox](https://freesound.org/people/jonccox/sounds/191809/)
- Yxa som snurrar: [Whoosh Deep Heavy av Qvantor](https://freesound.org/people/Qvantor/sounds/877977/),
  [Heavy Whoosh av FALL-E](https://freesound.org/people/FALL-E/sounds/697432/),
  [FX - Swoosh - Low Pitch av bolkmar](https://freesound.org/people/bolkmar/sounds/475135/)
- Fler kandidater (stamp, tuggande, älgbröl, hovskrap): sök med CC0-filtret, t.ex.
  `https://freesound.org/search/?q=moose+bellow&f=license%3A%22Creative+Commons+0%22` (freesound stoppade fler
  sökningar med 429 när listan gjordes).

## Fasordning
1. Biomet Scorched Barrens + gräns 2500 block.
2. Ruinen + dimman.
3. Elkwighten: modell, rörelse, närattacker, bossbar, skalning. **Byggd.**
4. Avståndsattacker: yxkast och markvåg. **Byggd.**
5. Kolossal carcass + yxan som tungt föremål + trofé. **Byggd.**
6. Advancements, registry och dokumentation.

## Bilder (docs/shots/elkwight/, tagna med `BossShots` och flaggfilen run/bossshots.flag)
- `boss_front.png`, `boss_side.png`: vaken, redo med yxan; boss-mätaren.
- `boss_chop.png`, `boss_spin.png`, `boss_sweep.png`, `boss_throw.png`, `boss_wave.png`: förvarningsposerna (hugg, snurr,
  hornsvep, yxkast, markvåg). `boss_eat.png`: på knä över en död.
- `boss_night.png`: de gröna ögonen och runorna i mörker.
- `boss_carcass.png`: den kolossala kroppen. `boss_axe.png`: yxan som tungt föremål. `boss_trophies.png`: yxan uppställd och
  hornkronan på väggen.
