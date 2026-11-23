# Savannen (savanna)

Savannen fick rovdjur som syns på dagen, och bytesdjur och grävare till dem: **Spotted Hyena**, **Cheetah**, **Gazelle**,
**Ostrich**, **Aardvark** och blocket **Termite Mound**. Dessutom vilar **Cave Lion** nu i skuggan på dagen och jagar i
skymningen. Alla djur lämnar ett kadaver som styckas med kniv (Butchery) och har egna produkter.
Kod: `wildlife/savanna/` (SavannaFauna, SpottedHyena, Cheetah, Gazelle, Ostrich, Aardvark, TermiteMoundBlock,
SignalHornItem, SavannaTest), klient `client/wildlife/savanna/` (SavannaFaunaClient, SpottedHyenaModel, CheetahModel,
GazelleModel, OstrichModel, AardvarkModel, SavannaAnimalRenderer och den genererade SavannaGeometry). Lejonets dag finns i
`wildlife/CaveLion.java`. Data: `scripts/gen_fauna_savanna.py`, föremål och block: `scripts/gen_fauna_savanna_textures.py`.
**Modeller och skinn**: `scripts/gen_savanna_models.py` (Cave Lion har en egen modell i `scripts/gen_pred_models.py`), som bygger varje djur ur en tabell (delar,
lådor, hudens färger) och skriver både `SavannaGeometry.java` och texturerna, så att modell och textur alltid stämmer.

## Gemensamt

- Allt kött: Campfire 20 s eller smoker 5 s; rått förskäms på 1 dag, stekt på 3 dagar, ruttnar till Rotten Flesh, kan
  torkas och rökas (`smokable_meat`). Hyenan ger grotthyenans **Hyena Meat**.
- Råa hudar skrapas med kniv i crafting till **Scraped Hide** (gasell 1, hyena, struts och jordsvin 2) och torkas sedan
  på Drying Rack till Leather som vanligt.
- Spawnar bara i savannbiomerna (savanna, savanna plateau, windswept savanna) via biome modifiers `add_<djur>.json`;
  WildlifeRestock fyller på av sig själv. Testet `savanna_spawns` kontrollerar att inget av djuren spawnar utanför savannen.

## Spotted Hyena (fläckig hyena)

| | |
|---|---|
| Var | savann (vikt 3, flock 3–5) |
| Liv / fart | 20 / 0,32, skada 4 |
| Utseende | egen modell: höga, tunga framkroppar och en rygg som sluttar ned mot en låg bakdel, en mörk borstig kam över nacke och skuldror, kraftig hals, stort huvud med breda käkmuskler och en trubbig mörk nos, runda öron med mörk kant, kort svans med svart tofs; sandfärgad med runda mörkbruna fläckar på sidor och ben |
| Beteende | grotthyenan på dagen: vaken när det är ljust och sover på natten. **Stjäl byten:** en kropp som ligger obevakad inom 24 block (ingen spelare inom 3 block) springer den fram till, tar i käftarna och **släpar 20 block bort** från närmaste spelare; sedan vaktar flocken den och äter upp den. **Följer sårade djur:** allt som är under 40 % hälsa eller blöder är byte (och gaseller, jordsvin, strutsungar, onagerföl). En frisk spelare lämnar den ifred, utom om han slår en hyena, kommer nära flockens byte (14 block) eller själv är svårt skadad: då cirklar flocken och biter bakifrån som grotthyenan. Fnittrar. Rädd för eld och facklor. |

| Styckning | Antal | Chans |
|---|---|---|
| Hyena Meat | 1–3 | – |
| Hyena Hide | 1 | 85 % (yttre) |
| Hyena Fang | 1–2 | 30 % |
| Sinew | 1 | 40 % |
| Bone | 1–2 | 50 % |

- **Hyena Hide** + kniv → 2 Scraped Hide.

## Cheetah (gepard)

| | |
|---|---|
| Var | savann (2, 1–2) |
| Liv / fart | 20 / 0,3, skada 4, inga ungar |
| Utseende | smal, djup bröstkorg och getingmidja, mycket långa ben, litet runt huvud, lång svans; gyllene med täta svarta prickar, vit buk, svarta "tårstrimmor" från ögonen till munnen, svansringar och vit svansspets |
| Beteende | dagjägare. Jagar Gazelle, Wild Horse och Onager (och strutsungar). **Smyger hukad** fram till 16 block, sedan **rusar den snabbast av alla** (2,6× fart). Rusningen varar **5 sekunder (100 tick)**: hinner den inte ta bytet **ger den upp**, flämtar och jagar inte igen på 30 s. Ett bett i full fart fäller bytet (Slowness). **Skygg:** håller sig undan från spelare som andra vilda djur; bara en svårt skadad spelare (3 hjärtan eller mindre) kan den jaga. Flyr under 40 % hälsa. Sover på natten. |
| Ljud | oceloten, lägre |

| Styckning | Antal | Chans |
|---|---|---|
| Cheetah Meat | 1–2 | – |
| Raw Cheetah Pelt | 1 | alltid (yttre) |
| Sinew | 1–2 | 60 % |
| Bone | 1 | 50 % |

- **Raw Cheetah Pelt** + kniv + fett (Tallow, Lard eller Bear Fat) → **Cheetah Pelt**.
- **Cheetah Cloak**: `PSP / L` (2 Cheetah Pelt, Sinew, Leather). En jägartrofé i Cloak-platsen: pälsen över axlarna och den
  ringade svansen ner längs ryggen (rustning 1).
- Cheetah Pelt + kniv → 2 Leather.

## Gazelle (gasell)

| | |
|---|---|
| Var | savann (8, hjord 4–7) |
| Liv / fart | 12 / 0,34, steg 1; kid (25 %) |
| Utseende | rådjurets byggnad men lättare; kanelbrun rygg, **svart flankstrimma** över vit buk, vit bakdel och svart svans, vit ring runt ögat och mörk strimma ner över ansiktet; båda könen har **ringade lyrformade horn** (inte kiden) |
| Beteende | **mycket skygg** (märker dig på dubbla avståndet, skygghet 2,0) och snabb. När hjorden flyr gör var och en ibland ett högt stelbent hopp rakt upp (**stotting**). Byte för gepard, hyena och lejon. |
| Ljud | getens, ljusare; tyst annars |

| Styckning | Antal | Chans |
|---|---|---|
| Gazelle Meat | 1–3 | – |
| Raw Gazelle Hide | 1 | 60 % (yttre) |
| Gazelle Horn | 1–2 | 90 % (yttre) |
| Sinew | 1–2 | 50 % |

- **Signal Horn** (blåshorn): Gazelle Horn + kniv + Sinew. Blås i det (högerklick, som getens horn): ett långt, djupt
  läte som hörs **256 block** bort. Varje annan spelare inom räckhåll får veta åt vilket håll och hur långt bort
  (*"A Signal Horn sounds to the north-east, 120 blocks away"*), och en **stamfrände** (samma stam, Clans) får också veta
  vem som blåste. 5 s mellan varje signal; hornet nöts (120 användningar).

## Ostrich (struts)

| | |
|---|---|
| Var | savann (4, flock 2–4) |
| Liv / fart | 24 / 0,36, steg 1; ungar (30 %), 40 % tuppar |
| Utseende | drygt 2 block hög; äggformad fjäderkropp (lager av lådor), hopfällda vingar med vita plymer som hänger över bakre flanken, svansplym, en fluffig krage där den bara halsen börjar, lång hals i två delar, platt huvud med stora ögon, ögonfransar och bred näbb, kala lår, fjällade skenben och tvåtåiga fötter med klo på stortån. Tuppen svart med vita plymer och skär hals och ben (röda fjäll fram på skenbenen), hönan gråbrun, ungen har en egen modell: en dunig boll med kort tjock hals, stort huvud och långa ben, gulbrun med mörka strimmor. |
| Beteende | springer mycket fort och är skygg (1,5). **Sparkar när den blir trängd:** om en spelare är inom 2,5 block när den är i panik eller skadas vänder den sig och sparkar framåt med ena benet (6 skada och en stor knuff, var 1,5 s). Hönor värper ett **Ostrich Egg** på marken på dagen (var 10–20:e minut). Tuppen bölar dovt ibland. |
| Ljud | papegojans, mycket mörkare |

| Styckning | Antal | Chans |
|---|---|---|
| Ostrich Meat | 3–5 | – |
| Ostrich Feather | 2–4 | alltid (yttre) |
| Raw Ostrich Hide | 1 | 50 % (yttre) |
| Sinew | 1–2 | 60 % |
| Bone | 1–2 | 50 % |

- **Ostrich Egg** (staplas 4): rostas helt på Campfire (45 s) till **Roasted Ostrich Egg**: 12 hunger, en hel måltid.
  Förskäms på 3 dagar när det är rostat.
- **Ostrich Feather** + kniv → 4 Feather (fjädring till pilar).
- **Raw Ostrich Hide** + kniv → 2 Scraped Hide.

## Aardvark (jordsvin)

| | |
|---|---|
| Var | savann (2, 1) |
| Liv / fart | 18 / 0,24, steg 1; ungar (20 %) |
| Utseende | grisstor, rygg som välver sig högst över länden, gles gulgrå hud med strån, mörkbruna korta tjocka ben med ljusa grävklor, lång rörformad nos med rynkor och en skär grisnos-skiva, höga rörformade öron (skära inuti), tjock avsmalnande svans som hänger ned mot marken |
| Beteende | **nattaktiv**: sover på dagen. På natten går den till en Termite Mound inom 24 block, **gräver vid foten** i 4 s (huvudet nere, jord sprutar) och **lämnar ett hål** i marken bredvid stacken (ett block jord eller sand blir luft). Skygg (1,4). |
| Ljud | minigrisens, mörkare; tyst annars |

| Styckning | Antal | Chans |
|---|---|---|
| Aardvark Meat | 2–3 | – |
| Raw Aardvark Hide | 1 | 70 % (yttre) |
| Aardvark Claw | 1–2 | 60 % |
| Lard | 1 | 40 % |

- **Raw Aardvark Hide** + kniv → 2 Scraped Hide.
- **Aardvark Claw** + kniv → 2 Bone Needle.

## Termite Mound (termitstack)

- Block av bränd röd jord full av gångar, med regnfårade räfflor. Världen bygger **spiror 2–4 block höga** på savannen
  (feature `mechanical:termite_mound`, ungefär var tredje chunk, på jord eller gräs). Det översta blocket i en spira
  (`tip=true`, inget termitstacksblock ovanpå) smalnar av till en trappstegsformad, knölig topp; staplar man ett nytt block
  ovanpå blir det under helt igen. Bryts med spade och ger sig själv.
- **Peta i den med en Stick** (högerklick): 35 % chans att pinnen kommer ut full av **Termites** (½ s mellan försöken).
- **Termites**: kan ätas (1 hunger). 4 Termites → **Chitin Paste**.
- **Chitin Paste** är ett lim som gör samma jobb som Hide Glue: Bellows (`PHP / PHP / GUG` med Chitin Paste i stället för Hide Glue).
- Jordsvinet gräver vid stackarna på natten.

## Cave Lion på dagen (lejonflockar)

- **Utseende** (ny modell, samma beteende): djup bröstkorg och tunga skuldror, en svag krage ned längs strupen, lös bukhud,
  brett huvud med kraftig panna, breda käkar och vit nos, små runda öron med mörka baksidor, tjocka ben på breda tassar,
  svaga spökränder på sidorna och en lång svans med mörk tofs.
- **Vilar i skuggan:** på dagen går lejonet till närmaste plats under en trädkrona (akacior på savannen) inom 16 block och
  lägger sig där (en kolumn där löven ligger 2–9 block över marken). Finns ingen skugga söker det igen efter 30 s. Det går
  inte iväg om en spelare är inom 14 block.
- **Jagar i skymningen:** jakttiden börjar redan i skymningen (klockan 11 500–13 500) och varar hela natten.
- **Flockar på savannen:** utöver de vanliga ensamma lejonen och paren finns nu flockar om 3–4 på savannen
  (`add_cave_lion_prides.json`).
- Leoparden (som ärver lejonets jakt) klättrar fortfarande upp i träd på dagen i stället.

## Avancemang

- Vildmark: **Bone Thief** (släpp ett byte nära en hyenaflock: få en Hyena Hide), **Five Seconds** (utmaning: Raw Cheetah
  Pelt), **Spotted Cloak** (Cheetah Cloak), **Signal Horn** (gör ett blåshorn), **Big Bird** (Ostrich Feather eller Ostrich
  Egg), **Termite Fishing** (Termites), **Glue from Grubs** (Chitin Paste).
- Mat: **Breakfast for Six** (ät ett Roasted Ostrich Egg).
- Jaktfliken: gasell, struts, jordsvin under Big Game; gepard och fläckig hyena under Predators.

## Testat (SavannaTest)

`savanna_spawns` (bara savannbiomer), `savanna_hyena_drags` (en hyena släpar bort en kropp minst 4 block),
`savanna_cheetah_gives_up` (en gepard efter ett byte den inte når rusar 100 tick och ger upp, trött),
`savanna_gazelle_flees` (en gasell som träffas av en spelare springer mer än 8 block bort från honom),
`savanna_lion_shade` (lejonet hittar skugga under en akaciakrona, skymningstiden, Signal Horns väderstreck).

## Bilder

Tagna med `SavannaShots` (klienten, bara med `run/savannashots.flag`), i `docs/shots/fauna/`. Varje djur fotograferas
snett framifrån (`savanna_<djur>.png`), rakt framifrån (`_front`), från sidan (`_side`) och huvudet på nära håll (`_face`),
med kameran i djurets ögonhöjd: `spotted_hyena`, `cheetah`, `gazelle`, `gazelle_fawn`, `ostrich_male`, `ostrich_female`,
`ostrich_chick`, `aardvark` och `cave_lion`. Dessutom `savanna_termite_mound.png` (spira med topp, jordsvin och Termites),
`savanna_lion_in_shade.png` (ett lejon som släpptes i solen 6 block bort har gått in under akaciakronan och sover där) och
`savanna_carcasses.png` (alla fem savanndjuren dödade i rad: kadaverställning och hud).
