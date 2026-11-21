# Nattens väsen (night)

Två nattmonster som tar **creeperns plats**: **Gloson** och **Mushroom Man**. Creepern passade inte i stenåldern och
folktron och förstörde bara bygget; inget av de nya väsendena förstör block.
Kod: `wildlife/night/` (NightFauna, Gloson, MushroomMan, NightTest), klient: `client/wildlife/night/`.
Data: `scripts/gen_fauna_night.py`, texturer: `scripts/gen_fauna_night_textures.py`.

## Ingen creeper

- Varje creeper stoppas när den kommer in i världen (EntityJoinLevelEvent), **även creepers i gamla sparfiler** (de
  försvinner bara). En nyspawnad creeper (vanillas egen spawn i alla biom, spawner, spawn egg) ersätts direkt på samma
  plats av en Gloson eller en Mushroom Man (`NightFauna.create`):
  - **ingen himmel ovanför** (grottor, under tak): alltid Mushroom Man (suggan håller sig på ytan);
  - **skog, taiga, djungel, träsk och blöta biom** (`#minecraft:is_forest`, `is_taiga`, `is_jungle`, `#c:is_swamp`,
    `#c:is_wet/overworld`): 70 % Mushroom Man, 30 % Gloson;
  - **annars** (öppet, torrt land): 60 % Gloson, 40 % Mushroom Man.
- **Extra nattspawns** (NightDangers): creeperns 12 % av blandningen går nu till samma val: Zombie 50 %, Skeleton 22 %,
  Spider 16 %, Gloson/Mushroom Man 12 %.
- Dessutom spawnar Mushroom Man av sig själv i skog och träsk (vikt 40, 1–2, som ett vanligt monster i mörker).
- Utan creepers kommer Gunpowder bara från häxor och ghasts (TNT och fyrverkerier hör ändå till ångåldern).

## Gloson

Gloson (gloso, glosugga): i svensk folktro en spöklik grå sugga med glödande ögon och en rygg vass som en rakkniv, som
rusar mellan benen på folk om natten och skär dem. Här ett **riktigt djur**: det går att döda och lämnar ett kadaver.

- **Var:** där creepern skulle ha spawnat (se ovan), mest i öppet land. Ensam. Inga ungar.
- **Beteende:** ett nattdjur (Predator): sover på dagen och smiter undan folk då. På natten (och under blodmåne, vid svält
  och i grottor) väljer den en spelare inom **32 block** och **cirklar runt honom 10–16 block bort i 20–50 s**. I mörkret
  syns bara de **glödande ögonen** (ett eget lager med full ljusstyrka). Sedan **anfaller** den i full fart, direkt om
  man slår den eller går närmare än **4 block**. Träffen ger 4 skada, **hård knockback** och ett sår som blöder:
  **Bleeding** i 10 s (ett halvt hjärta var 2:a sekund med blodstänk, aldrig under ett hjärta). Efter träffen springer
  den förbi och bort i mörkret (4 s) och börjar cirkla igen.
- **Rädd för eld:** håller sig undan brinnande eldar (Campfire, Fire Pit, Clay Kiln), går inte på den som sitter vid
  elden (den smyger runt elden i stället) och anfaller inte den som håller en fackla (`#mechanical:scares_predators`).
  Under blodmåne och när den svälter hjälper varken eld eller fackla. Kommer också till blodiga kadaver som andra rovdjur.
- **Försvinner** när ingen spelare är i närheten, som ett monster.
- **Stats:** 18 liv, 4 skada, fart 0,3, knockback 1,5, knockback-motstånd 0,4.
- **Utseende:** vildsvinets modell (med borstkam längs ryggen) i askgrått, med bärnstensgula ögon.
- **Ljud:** hoglin-läten i hög ton (en tunn suggskrik), ett argt skri när den anfaller.

| Omgång | Vad | Chans | Antal |
|---|---|---|---|
| 1 | Gloson Bristles | 100 % | 1–2 |
| 1 | Raw Hide | 40 % | 1 |
| 2 | Raw Gloson | – | 1–3 |
| 2 | Razor Ridge | alltid | 1 |
| 2 | Sinew | 30 % | 1 |
| 2 | Bone | 50 % | 1–2 |

**Produkter:**
- Raw Gloson (hunger 2) → Campfire 20 s / rökugn → **Cooked Gloson** (hunger 7, mättnad 0,7). Förskäms på 1 resp. 3
  dagar, kan rökas.
- **Gloson Bristles** + Stick → **Paintbrush** (en borstpensel, samma som av Plant Fibre).
- **Razor Ridge** + Stick + Sinew → **Razor Knife**: en kniv på flintnivå, 1,3× snabbare och 1,15× mer kött och delar vid
  styckning, 120 hållbarhet. Lagas med Razor Ridge.
- Raw Hide och Bone som vanligt.

## Mushroom Man (svampgubbe)

En vandrande svamp: en blek fot-stjälk till kropp på två korta fötter, tunna armar och en bred brun hatt med ljusa
prickar. Creeperns smyg-och-smäll, fast naturligt: den spricker i ett sporoln.

- **Var:** vanligast i skog, träsk och fuktiga eller mörka ställen, alltid i grottor (se ovan). Spawnar i mörker som ett
  monster.
- **Beteende:** smyger fram mot spelaren. Inom **3 block** stannar den, **fräser och sväller** (1,5 s, blinkar vitt) och
  **spricker**: ett sporoln (radie 3, krymper på 6 s) med **Blindness 5 s** och **Nausea 10 s**, **−15 sanity** för
  spelare inom 4 block, **1 hjärta** i skada inom 2,5 block, och monster i molnet tappar sitt mål. **Inga block går
  sönder.** Går man mer än 7 block bort, eller ur sikte, krymper den tillbaka. Som monster är den rädd för eld, slår
  sönder facklor och jagar stamfolk som andra monster (NightDangers, MonstersHuntPeople). Blöder inte (`mechanical:bloodless`).
- **Stats:** 16 liv, fart 0,23.
- **Ljud:** mossa när den går, slemljud när den skadas, ett fräs när den sväller.
- **Fynd** (bara om den dödas innan den spricker; spricker den blir inget kvar): **Mushroom Flesh** 1–2, **Spore Sac** 50 %
  (Looting ger fler).

**Produkter:**
- **Mushroom Flesh:** torrt, korkigt svampkött. **Tändmaterial** som Bark och Leaf Litter: högerklicka på en otänd
  Campfire, Fire Pit eller Clay Kiln.
- **Spore Sac** + **Puffball** → **Spore Bomb**: kastas som en snöboll och spricker där den landar i samma sporoln
  (Blindness, Nausea, −15 sanity, monster tappar sitt mål), men utan skada. Staplas till 16.

## Stamfolk

Alla stamfolk backar från en **svällande Mushroom Man** (inom 6 block); jägare med båge skjuter Mushroom Men på håll,
andra beväpnade går inte i närstrid mot dem. Mushroom Men jagar stamfolk som andra monster. Glosons jagar bara spelare.

## Sanity

En Mushroom Mans sporer ger −15 sanity. De inbillade monstren under 15 sanity är zombie, skeleton eller **Mushroom Man**;
det falska fräset bakom ryggen under 40 är en svällande Mushroom Man.

## Advancements

- Hunting: **Gloson** under Predators (alla delar från kadavret).
- Wilderness: Razorback (Razor Ridge), Razor's Edge (Razor Knife), Bristle Brush (pensel av Gloson Bristles), Before It
  Bursts (Spore Sac), Spore Bomb, Fungal Tinder (Mushroom Flesh). Food: Ghost Pork (Cooked Gloson).

## Test

`NightTest`: en creeper kommer aldrig in i världen och ersätts av en Gloson eller Mushroom Man (alltid Mushroom Man under
tak); en Mushroom Man spricker utan att ett enda block går sönder och ger Blindness och Nausea; Glosonens styckning;
(valfritt, behöver AI) en Gloson går bort från en brinnande eld.
