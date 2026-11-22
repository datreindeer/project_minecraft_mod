# Flodfauna: Beaver, Otter, Mosquito Swarm

Kod: `wildlife/river/` (`RiverFauna`, `Beaver`, `BeaverWorks`, `Otter`, `MosquitoSwarm`, `PeltHoopBlock`, `SemiAquatic`),
klient: `client/wildlife/river/`. Data: `scripts/gen_fauna_river.py`, texturer: `scripts/gen_fauna_river_textures.py`.

Beaver och Otter är halvakvatiska: de går i land och simmar under ytan (axolotlns navigering), håller andan i 4 minuter
och kliver upp på strandbanken när vägen leder dit.

## Beaver

**Var:** sällsynt vid vatten. River, Swamp och Willow Fen (vikt 3, 1–2 st) och skogar (Forest, Birch Forest, Old
Growth Birch Forest, Dark Forest, Taiga-varianterna, Deciduous Forest, Pine Heath; vikt 2, 1–2 st). Den föds bara på
fast mark (gräs, jord, sand, grus, lera, sten) med vatten inom 3 block. Ungar: 35 % av flocken efter den första.

**Beteende:** skygg (flyr på längre håll än andra, ×1,2). När den flyr i vattnet smäller den svansen i ytan
(plask). Den sover på natten som andra vilda djur. Hemmet sätts vid närmaste vatten (inom 6 block) och den håller sig
inom 20 block därifrån.

**Bygget** (bara med gamerule `mobGriefing` på):

1. Fäller ett litet naturligt träd (högst 16 stockar) inom 10 block från hemmet, högst ett per dag (24 000–36 000 tick
   mellan). Den gnager i 10 s (spån och skrapljud) och trädet faller mot vattnet precis som när en spelare fäller det:
   fallna stockar och lövförna.
2. Gnager de fallna stockarna (bara de inom 7 block från sitt eget träd, inte spelarens stockar på annat håll):
   varje stock blir 3 byggmaterial (högst 12 åt gången).
3. Bär material i munnen (en pinne syns) till **dammen**: den smalaste raka linjen över öppet vatten inom 12 block
   från hemmet, 2–7 block bred med fast strand i båda ändar. Två varv Beaver Dam: ett i vattenytan, ett ovanpå.
4. När dammen är hel stiger vattnet ett block bakom den, men bara om dammen är instängd: en översvämning på högst 64
   block som aldrig rinner över en kant. En flod som rinner vidare får bara dammen.
5. Sedan en **hydda**: 3×3 ring i vattnet (en våt kammare i mitten), 3×3 tak och en topp, 18 block, 3–8 block från
   hemmet och minst 4 block från dammen.

Allt är begränsat och sparas: dammlinje, hyddans mitt, material, senaste träd och nästa fällning sparas i bävern;
den letar arbete ungefär en gång per sekund och söker nya platser högst en gång i minuten. Dammen och hyddan är runt
30 block; sedan lagar den bara det som tas bort.

**Värden:** 14 hälsa, fart 0,2 (i vattnet ×0,12 av farten via simkontrollen), 2 skada, 20 % knuffmotstånd.
Storlek 0,6 × 0,55 (lätt kropp: kan bäras och hängas).

| Styckning (flintkniv) | Antal | Chans |
|---|---|---|
| Raw Beaver Pelt (första omgången) | 1 | 100 % |
| Raw Beaver | 1–2 | – |
| Beaver Tail | 1 | 100 % |
| Castoreum | 1–2 | 60 % |
| Beaver Tooth | 1–2 | 50 % |
| Sinew | 1 | 30 % |

På Hanging Rack: köttet blir Haunch/Ribs/Loin som vanligt, Sinew och ben kommer alltid.

**Produkter:**

| Sak | Hur | Användning |
|---|---|---|
| Raw Beaver → Cooked Beaver | Campfire 20 s / Smoker | 2 → 6 hunger. Förskäms 1 / 3 dagar, kan torkas och rökas |
| Beaver Tail → Roasted Beaver Tail | Campfire 20 s / Smoker | 1 → 7 hunger, mättnad 1,0: en fet delikatess |
| Raw Beaver Pelt → Beaver Pelt | Stretching Hoop, utomhus på dagen utan regn (~4 min) | Beaver Fur Hat |
| Beaver Fur Hat | 2 Beaver Pelt + Sinew (crafting) | Hjälm 1 rustning, isolerar 20 % mot kyla, vattentät |
| Castoreum | – | Bete i Snare (`#mechanical:snare_bait`), Castoreum Salve |
| Castoreum Salve | Castoreum + Tallow + Bark → 2 | Smörjs in: Regeneration I 10 s, tar bort Poison |
| Beaver Tooth Knife | `[B][ ]` / `[T][S]`: Beaver Tooth, Twine, Stick | Kniv (alla knivrecept, styckning): 1,2× fart, 96 hållbarhet |
| Beaver Dam (block) | Bara från bävrar | Fast block som håller undan vatten. Ger 1–3 Stick och 0–2 Clay Ball (leran) |

## Otter

**Var:** River, Frozen River, Swamp, Mangrove Swamp, Stony Shore, Beach och Willow Fen (vikt 4, 1–2 st), på stranden
med vatten inom 3 block. Ungar: 40 %.

**Beteende:** snabb simmare (×0,2 av farten i vattnet, nästan dubbelt så snabb som bävern). Skygg. Lekfull: två
uttrar nära varandra jagar ibland varandra en stund. Fiskar: i vattnet dyker den ibland mot botten och kommer upp med
en fisk i munnen, simmar i land, sätter sig upp och äter (läker 1 hjärta). **Tjuv, men rättvis:** när ingen spelare är
inom 12 block, och högst en gång per dag, tar den en fisk som ligger på marken (`#minecraft:fishes`) eller en fisk som
torkar på en Drying Rack (färsk eller torkad) inom 8 block.

**Värden:** 10 hälsa, fart 0,25, storlek 0,6 × 0,45.

| Styckning | Antal | Chans |
|---|---|---|
| Raw Otter Pelt (första omgången) | 1 | 100 % |
| Raw Otter | 1 | – |
| Sinew | 1 | 30 % |

| Sak | Hur | Användning |
|---|---|---|
| Raw Otter → Cooked Otter | Campfire 20 s / Smoker | 2 → 5 hunger |
| Raw Otter Pelt → Otter Pelt | Stretching Hoop | Otter Fur Cape |
| Otter Fur Cape | 3 Otter Pelt + 2 Sinew | Cloak-slot (över rustningen), 2 rustning, isolerar 20 %, vattentät |

## Mosquito Swarm

**Var:** Swamp, Mangrove Swamp och Willow Fen (ambient som fladdermöss, vikt 12, 1–2 st), i skymningen (tid
11 500–14 500) och ibland på natten, i varma biomer (temperatur ≥ 0,5), under öppen himmel med vatten inom 4 block.

**Beteende:** en svävande moln av knott (28 små prickar, surrande ljud). Den letar upp närmaste spelare inom 12 block
och dansar runt huvudet: var 3:e sekund ett stick som tar 1 sanity (äter upp en stor del av dagsljusets återhämtning)
och var tredje gång ½ hjärta. **Rök och eld driver bort den:** en brinnande Campfire/Fire Pit inom 6 block från
svärmen eller spelaren, eller ett rökigt rum (Smoke): den drar sig undan och skingras efter några sekunder. Den dör
av ett slag (2 hälsa) eller eld och skingras när solen går upp. Ingen kropp (`Butchery.neverCarcass`), inget byte.

## I vattnet

Båda simmar gärna (`WaterHabit.SWIMMER`): vägar genom vatten kostar inget, de simmar under ytan med egen fart (otter
fortare än bäver), dyker och håller andan i minuter, och tar sig upp på en strandkant på ett block. Egen simrörelse:
bävern ligger platt med bakfötterna sparkande och svansen som roder, uttern böljar från huvudet ner genom svansen.

## Stretching Hoop

4 Stick runt en Twine (`[ S ]` / `[STS]` / `[ S ]`). En böjd ring på två fötter. Högerklicka med Raw Beaver Pelt
eller Raw Otter Pelt för att snöra in den; den torkar utomhus (skugga går bra) på dagen utan regn, ungefär 4 minuter
(1/4 per slumpvis tick). Tom hand tar av pälsen, rå eller torkad. Sönderslagen ger den bågen och pälsen.
(Små pälsar har en egen båge i stället för Drying Rack, så rackets tillståndsantal inte växer.)

## Vattentät päls

Beaver Fur Hat och Otter Fur Cape (`#mechanical:waterproof_fur`) isolerar 20 % var, och varje del tar bort hälften av
kylan från vatten och regn (båda: ingen kyla från väta). Räknas ihop med Sealskin.

## Avancemang

Wilderness: Busy Beaver, Dam Builders, Stretched Thin, Water off a Beaver's Back (mål), Chisel Tooth, Old Remedy,
Sleek Swimmer, Bloodsuckers. Food: Trapper's Treat. Hunting-fliken får Beaver och Otter automatiskt.
