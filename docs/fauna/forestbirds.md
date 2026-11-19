# Skogsfåglar (forestbirds)

Sex fåglar för skog, berg, slätt och torra marker: **Capercaillie**, **Great Bustard**, **Eagle-Owl**, **Raven**, **Golden Eagle** och **Griffon Vulture**. Alla är flockfåglar (`FlockBird`): de går, pickar och sover på marken, lyfter tillsammans och flyger iväg när en spelare kommer nära (smyg för att komma närmare). Varje pil fäller en fågel direkt.

**Vilka lämnar kadaver?** Bara de två ätliga markfåglarna, Capercaillie och Great Bustard: de plockas med kniv, styckas till eget kött eller tas hela som plockad fågel (som gås, fasan och ripa). Uggla, korp, örn och gam lämnar inget kadaver utan släpper fjädrar och sina egna delar direkt (som kråkan): ingen jagade dem för köttet, och en asätares kött är inte mat. Inga ungar: liksom de andra flockfåglarna förökar de sig inte i spelet, de ätliga lägger ägg i bon i stället.

Kod: `com.mechanical.wildlife.forestbirds` (`ForestBirdsFauna`), klient: `client.wildlife.forestbirds`. Data: `scripts/gen_fauna_forestbirds.py`, texturer och modeller: `scripts/gen_fauna_forestbirds_textures.py`.

## Capercaillie (tjäder)

- **Var:** taiga, snöig taiga, gammal tall- och granskog, blåsig skog, Pine Heath. Vikt 7, grupper om 1–3.
- **Beteende:** går i små sällskap på skogsmarken, sover i tallarna (landar på löv). Flyger upp med ett högt brak, lågt (2–6 block) och kort. Tuppen är skiffergrå med grönt bröst, vit skulderfläck, röd ögonbrynskam och ljus näbb; hönan (hälften) är mindre och rostbrun med svarta band.
- **Spel i gryningen** (klockan 22500–3000): en tupp på marken börjar spela då och då i 24 s: stjärten reses i en solfjäder, halsen sträcks, vingarna hänger. Varje sång är knäppar som tätnar, en "klunk" och sedan **slipningen**, då han inte hör någonting: han märker bara en spelare inom 2 block. Smyg fram under slipningen och stå still mellan sångerna, så som tjäderjakt gick till.
- **Stats:** 8 hp, fart 0,22, bredd 0,7 × 0,9 (lätt att bära, hängs i fötterna).
- **Bo:** Bird Nest på gräs/podsol/mossa i tallskog (1 per ~14 chunks), [[capercaillie_egg]].

| Omgång | Vad | Chans | Antal |
|---|---|---|---|
| 1 (plockning) | Feather | 100 % | 2–4 |
| 1 | Capercaillie Tail Feather | 60 % | 1–2 |
| 2 | Raw Capercaillie | – | 1–2 |

## Great Bustard (stortrapp)

- **Var:** slätt, solrosslätt, savann, Hazel Meadow. Vikt 5, grupper om 2–4.
- **Beteende:** den skyggaste fågeln (rädsla 1,4). Märker man på långt håll och **springer** undan (fart ×1,9) i stället för att flyga; den lyfter bara (tungt, 3–7 block högt och kort) när man är inom 6 block, när den träffas eller när flocken beskjuts. Ingefärsfärgad, svartbandad rygg, blågrått huvud och hals, kastanjebrunt bröstband, vita "mustascher" och stora vita vingfält.
- **Stats:** 10 hp, fart 0,26, bredd 0,8 × 1,1.
- **Bo:** Bird Nest i gräset på slätten (1 per ~18 chunks), [[bustard_egg]].

| Omgång | Vad | Chans | Antal |
|---|---|---|---|
| 1 (plockning) | Feather | 100 % | 3–5 |
| 2 | Raw Bustard | – | 2–3 |

## Eagle-Owl (berguv)

- **Var:** skog, mörk skog, björkskog, taiga, gammal tall-/granskog, Deciduous Forest, Pine Heath, Willow Fen. Vikt 3, ensam.
- **Beteende:** omvänd dygnsrytm: sover sittande på en gren eller stubbe om dagen (en spelare som kommer nära väcker och skrämmer upp den) och tappar ibland en [[owl_pellet]] under kvisten. På natten flyger den lågt (3–8 block) och **helt ljudlöst** (inget vingljud alls) och hoar dovt två gånger ("hoo-hoo", flöjt i låg tonhöjd). Hör man den i mörkret när man redan är orolig (Sanity under 70) tappar man 1 Sanity. Den slår småvilt i taggen `mechanical:owl_prey` (ekorre, hare, igelkott, ripa, kanin) inom 20 block: glider in och störtar de sista blocken, dödar bytet och sätter sig vid det en stund. Bytet blir ett vanligt kadaver som spelaren kan ta.
- **Stats:** 6 hp, bredd 0,5 × 0,8. Sitter upprätt (modellen lutar 0,9 rad), stora framåtriktade orange ögon, örontofsar.
- **Drop:** Feather 0–1, [[owl_feather]] 1–3.

## Raven (korp)

- **Var:** taiga, snöig taiga, gammal talltaiga, blåsiga berg och skog, grove, meadow, stony peaks, Pine Heath. Vikt 4, 2–3. Spawnar på sten, grus och snö också.
- **Beteende:** klok och skygg (rädsla 1,3). Följer en **jägare** (spelare med pilbåge, armborst eller spjut i handen inom 40 block) och kretsar högt (11–14 block) i en ring runt honom och väntar på bytet. Ett kadaver inom 48 block (inte buret eller upphängt) drar ner flocken som sätter sig runt det och pickar. En köttbit (`mechanical:raven_scraps`: #minecraft:meat och rökbart kött) på marken inom 2 block **snor korpen** och flyger iväg med; när den landar igen är biten uppäten. Skjuter man tjuven tappar den biten.
- **Stats:** 5 hp, bredd 0,5 × 0,6.
- **Drop:** Feather 0–1, [[raven_feather]] 1–3.

## Golden Eagle (kungsörn)

- **Var:** blåsiga berg (även grus och skog), stony peaks, meadow, grove, snowy slopes. Vikt 2, 1–2.
- **Beteende:** kretsar högt (16–30 block) på stilla vingar. Ser den ett byte i taggen `mechanical:eagle_prey` (hare, ökenhare, ekorre, ripa, klippduva, tjäder, kanin, räv) på öppen mark inom 36 block störtdyker den, dödar det och landar vid kadavret och äter i 30 s, sedan är det borta. **Snor jägarens småvilt:** ett lätt kadaver som ligger fritt (ingen spelare inom 4 block) griper den i klorna (kadavret rider under örnen) och flyger bort från jägaren för att äta i lugn och ro. Skjut ner den så faller bytet; skrämmer man den från måltiden ligger kadavret kvar.
- **Stats:** 10 hp, bredd 0,7 × 0,9. Krokig näbb med gul vaxhud, gyllene nacke.
- **Drop:** Feather 1–2, [[eagle_feather]] 1–3, [[eagle_talon]] 0–2.

## Griffon Vulture (gåsgam)

- **Var:** savann, savannplatå, blåsig savann, badlands (alla tre), blåsiga berg. Vikt 4, 2–4. Spawnar på terrakotta och sten.
- **Beteende:** kretsar mycket högt (18–34 block). Ett kadaver inom 80 block får **hela gruppen att cirkla över det** i en tät pelare, ett tecken på kött som syns på långt håll. Nattrovdjur (varg, grottlejon) inom 96 block som inte har något för sig går mot kadavret (deras doftmål tar över på nära håll). Efter ~30 s cirklande landar de och äter tillsammans; 2 min ätande sammanlagt så återstår bara en Bone Pile.
- **Stats:** 10 hp, bredd 0,8 × 1,0. Kal vitdunig hals och huvud, vit krage.
- **Drop:** Feather 2–4, [[vulture_wing_bone]] 1.

## Produkter och recept

| Sak | Hur | Användning |
|---|---|---|
| Raw Capercaillie / Raw Bustard | styckning, eller plockad fågel + kniv (2 / 3) | Campfire 20 s / rökare → Cooked (7 / 8 hunger). Förskäms 1 dag, kan torkas och rökas, Kettle |
| Plucked Capercaillie / Bustard | plocka kadavret, tom hand | kniv i griden → kött, hel på Campfire → 2 / 3 Cooked |
| Capercaillie Egg / Bustard Egg | ur bon | Campfire → 1 / 2 Boiled Egg |
| Capercaillie Tail Feather | tjäderkadaver | Fletching Table: Flint + Stick + Tail Feather → **6** Arrow |
| Owl Feather | uggla | Fletching Table: Flint + Stick + Owl Feather → 4 **Silent Arrow** (2 skada; en miss skrämmer inga djur) |
| Owl Pellet | under ugglans dagkvist | griden → 2 Bone Meal |
| Raven Feather | korp | 3 st + Twine + Bone → **Raven Talisman**: i inventoryt kommer 40 % av den Sanity mörkret tar tillbaka |
| Eagle Feather | örn | Fletching Table: Flint + Stick + Eagle Feather → 4 **Eagle-Fletched Arrow** (2,75 skada) |
| Eagle Talon | örn | ersätter Bear Claw i Barbed Arrow |
| Vulture Wing Bone | gam | + kniv → **Bone Flute**: spelar en melodi, +4 Sanity för alla spelare inom 12 block, 10 s cooldown |

## Prestationer

Wilderness: Lek Hunter, Wariest of All, Silent Wings, Whisper, King of the Mountains, Flies True, Ravens' Watch, First Music. Food: Feast of the Forest, Ground Nest.
