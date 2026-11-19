# Vattenfåglar (waterfowl)

Fyra vilda fåglar vid vatten: **Mallard**, **Mute Swan**, **Grey Heron** och **Common Crane**. Alla bygger på
flockfåglarnas system (`FlockBird`): de märker spelare på avstånd (smyg för att komma närmare), lyfter och flyger iväg
när de skräms, sover på natten, och **en pil dödar**. Alla fyra är ätbara fåglar: de lämnar en **Carcass** som plockas
med kniv (första passet), sedan styckas (andra passet) eller tas hel som **Plucked**-fågel med tom hand. Hängda på
Hanging Rack ger de en extra köttbit när kroppen åldrats. De häckar i **Bird Nest** (vilda bon vid vattnet, eller bon du
bygger nära dem) och lägger egna ägg.

Kod: `com.mechanical.wildlife.waterfowl` (`WaterfowlFauna`, `Mallard`, `MuteSwan`, `WadingBird`, `GreyHeron`,
`CommonCrane`, `BoneFluteItem`), klient `com.mechanical.client.wildlife.waterfowl`. Data `scripts/gen_fauna_waterfowl.py`,
texturer och modeller `scripts/gen_fauna_waterfowl_textures.py`.

## Mallard (gräsand)

- **Var:** floder, träsk, Willow Fen (vikt 10, 3–6), mangroveträsk (4, 2–4); i slätter, skogar, ängar, Deciduous Forest
  och Hazel Meadow (2, 2–4) på sjöar och vattendrag. Spawnar på vattnet, efter världsgenereringen även på en gräs- eller
  sandstrand högst 2 block från vatten.
- **Beteende:** simmar i småflockar, tippar upp och bökar (stjärten i vädret, huvudet under vattnet), vilar på stranden.
  Skygg (räckvidd ×1,1): hela flocken lyfter rakt upp från vattnet och flyger iväg. Hanne (grönt huvud, vit halsring,
  kastanjebrunt bröst) eller hona (spräckligt brun), båda med blå vingspegel. Liten nog att fångas i **Snare**.
- **Värden:** 5 HP, 0,5 × 0,6 block. Ljud: kvackande (groda, hög tonhöjd).

| Pass | Utbyte |
|---|---|
| Plocka | 1–2 Feather, 60 % 1 Down |
| Stycka | 1 Raw Mallard |
| Hel | Plucked Mallard |

## Mute Swan (knölsvan)

- **Var:** floder och Willow Fen (vikt 3), träsk (2), slätter (1); alltid i par, bara på öppet vatten.
- **Beteende:** lugn (märker dig på 0,6 av normalt avstånd), glider på vattnet, flyger i rad med långsamma vingslag.
  **Försvarar boet:** kommer en spelare inom **6 block** från ett Bird Nest som svanen tagit, väser den, reser vingarna
  över ryggen och anfaller (3 skada, lite knuff) tills spelaren är **12 block** från boet. Slagen inom 16 block från
  boet vänder den sig mot angriparen i stället för att flyga (partnern flyger). Utan bo beter den sig som andra fåglar.
- **Värden:** 16 HP, 0,9 × 1,25 block, attack 3.

| Pass | Utbyte |
|---|---|
| Plocka | 3–5 Feather, 2–3 Down, 80 % 1–2 Swan Quill |
| Stycka | 3–4 Raw Swan, 60 % 1–2 Hollow Bird Bone |
| Hel | Plucked Swan |

## Grey Heron (gråhäger)

- **Var:** floder, träsk, mangroveträsk (vikt 3, 1 st), Willow Fen (4, 1–2), stränder (1). Strand, gräs, lera eller grunt
  vatten.
- **Beteende:** ensam. Vadar (står på botten i vatten ett block djupt, flyter aldrig; flyger om det blir för djupt).
  Om dagen går den från stranden ut i grunt vatten och **fiskar**: står stilla och lutar sig ut över vattnet; när en fisk
  är inom ~2 block hugger den blixtsnabbt (fisken försvinner, hägern får 1 HP). Ibland hugger den efter något för litet
  för att synas. Flyger med långsamma, djupa slag, halsen indragen och benen utsträckta bakåt; kan landa i trädtoppar.
- **Värden:** 8 HP, 0,6 × 1,5 block. Ljud: hes rävskrik, låg tonhöjd.

| Pass | Utbyte |
|---|---|
| Plocka | 2–3 Feather, 90 % 1–2 Heron Plume |
| Stycka | 1–2 Raw Heron, 80 % 1 Heron Bill |
| Hel | Plucked Heron |

## Common Crane (trana)

- **Var:** Willow Fen (vikt 8, 3–6), träsk och Pine Heath (4, 2–4), slätter, ängar och Hazel Meadow (2, 3–5). Gräs, lera
  eller grunt vatten.
- **Beteende:** flockar som betar på marken och vadar i grunt vatten. Mycket skygg (×1,3). Flyger högt (16–30 block) i
  V-formation och **flyttar**: när en lugn flock lyfter flyttar ledaren ibland (1 på 3) flockens hem 64–128 block bort,
  hela tiden åt ungefär samma håll. **Dans:** om dagen börjar två tranor på marken ibland dansa i 6 s, vända mot
  varandra, med halvöppna vingar, bugningar, hopp och trumpetrop.
- **Värden:** 12 HP, 0,7 × 1,75 block. Ljud: trumpetande (get, hög tonhöjd).

| Pass | Utbyte |
|---|---|
| Plocka | 3–4 Feather |
| Stycka | 2–3 Raw Crane, 80 % 1–2 Hollow Bird Bone |
| Hel | Plucked Crane |

## Mat

| Föremål | Hunger / mättnad | Tillagning |
|---|---|---|
| Raw Mallard / Swan / Heron / Crane | 2 / 3 / 2 / 3 (chans på Hunger rå) | Campfire 20 s, Smoker; rökas och torkas (`smokable_meat`) |
| Cooked Mallard | 6 / 0,7 | |
| Cooked Swan | 8 / 0,8 | |
| Cooked Heron | 5 / 0,6 | |
| Cooked Crane | 7 / 0,8 | |
| Plucked … | – | hel på Campfire 30 s (svan 3, trana 2, övriga 1 kokt), + kniv → rått kött, kokas till Broth i Bronze Kettle |

Rått kött och plockade fåglar förskäms på 1 dag, kokt på 3 (ruttnar till Rotten Flesh).

**Ägg** (i Bird Nest, max 3; töm inte boet, annars överger fåglarna det): Mallard Egg (grönaktigt), Swan Egg (stort,
kritvitt – svanen vaktar), Heron Egg (blågrönt), Crane Egg (olivbrunt, fläckigt). Campfire 15 s → Boiled Egg: 1 / 3 / 1 / 2.
Alla ingår i `#mechanical:bird_eggs`.

**Vilda bon** (världsgenerering, redan tagna av fågeln, 0–2 ägg): Mallard vid floder, träsk och Willow Fen (vanligast);
Mute Swan och Grey Heron där också (ovanliga); Common Crane i Willow Fen, träsk och Pine Heath. Alltid på gräs, jord,
lera, sand eller mossa intill vatten.

## Delar och vad de används till

| Föremål | Från | Användning |
|---|---|---|
| Down | Mallard, Mute Swan | **Fnöske**: högerklicka en osläckt Campfire, Fire Pit eller kiln (som Leaf Litter/Bark); Plume Headdress |
| Swan Quill | Mute Swan | + kniv → 4 Feather (pilar vid Fletching Table); Plume Headdress |
| Heron Plume | Grey Heron | Plume Headdress, lagar den |
| Heron Bill | Grey Heron | + kniv → 2 Bone Fish Hook |
| Hollow Bird Bone | Mute Swan, Common Crane | + kniv → Bone Flute; + Flint → 2 Bone Needle |
| Bone Flute | Hollow Bird Bone | Högerklick spelar en fras på femtonsskala (3 s nedkylning); alla spelare inom 12 block får +3 sanity, upp till 75 |
| Plume Headdress | 2 Heron Plume + Swan Quill (eller plym) / Leather + Down + Leather | Hjälm, 1 rustning; **Grey Heron och Common Crane märker bäraren först inom 4 block** (om de inte redan skräms) |

## Framsteg

- Wilderness: *Swan Song* (plocka en svan), *Light as Down* (Down), *Patient Fisher* (Heron Bill), *Among the Reeds*
  (Plume Headdress), *First Music* (Bone Flute).
- Food: *Swan Feast* (laga vattenfågel), *Swan's Egg* (svanägg).

## Ändringar utanför paketet (små, för sammanslagning)

- `BirdNestBlock.Bird`: fyra nya värden (MALLARD, MUTE_SWAN, GREY_HERON, COMMON_CRANE).
- `Birds`: `registerPlucked(...)` och `isPlucked(stack)` (fåglar registreras från egna klasser); `Kettle.isPlucked` använder den.
- `Butchery.dropInner`: fåglar släpper nu sina inre delar (`inner`; tom för de gamla fåglarna).
- `FlockBird.swim()` är `protected` (vadarna står i stället för att flyta).
- `Mechanical`: `WaterfowlFauna.init()`.
