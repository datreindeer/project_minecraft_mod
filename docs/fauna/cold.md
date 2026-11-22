# Kyla (cold)

Djur för snölandet, isen och de högsta topparna: **Polar Bear** (ersätter vanilla-isbjörnen), **Arctic Fox** och
**Snow Leopard**.
Kod: `wildlife/cold/` (ColdFauna, ett djur per klass, ColdTest), klient: `client/wildlife/cold/`.
Data: `scripts/gen_fauna_cold.py`, texturer: `scripts/gen_fauna_cold_textures.py`.

Alla lämnar ett Carcass som styckas med kniv (Butchery) och kan hängas på en Hanging Rack. Mängder nedan gäller
flintkniv; på Hanging Rack faller allt i första omgången utan slumpslag och köttet blir Haunch/Ribs/Loin.

## Pälsar på torkställningen

Råa pälsar (Raw Polar Bear/Arctic Fox/Snow Leopard Pelt) högerklickas på en vanlig **Drying Rack**-plats och torkar som
en Scraped Hide (~3 min). Det är samma system som för de kalla jägarnas pälsar (`ColdhuntersFauna.CURES`, PeltCuring):
tar man ner den torra pälsen med tom hand får man den färdiga pälsen, slås ställningen sönder får man pälsen tillbaka.

## Polar Bear (isbjörn)

- **Var:** snowy plains, ice spikes, snowy beach, Glacial Moraine, frozen ocean och deep frozen ocean (på isen). Vikt 1,
  1–2 (den andra är en unge). **Vanilla-isbjörnen finns inte längre:** den är borttagen ur alla biomers spawnlistor
  (`remove_vanilla_polar_bears.json`), och en vanilla-isbjörn som ändå dyker upp (gamla världar, spawnägg, kommandon)
  byts direkt mot moddens (en unge mot en unge).
- **Beteende:** brunbjörnens kropp och strid (reser sig och slår, flyr aldrig, en hona försvarar sin unge), men en
  **jägare**. Den väljer ett byte inom **24 block**: helst en **Seal**, annars en kalv av Walrus, Reindeer eller Musk Ox.
  Finns inget byte tar den en spelare inom **20 block** (8 om han smyger) som den ser. Den **smyger** in långsamt med
  huvudet lågt tills den är 5 block ifrån, sedan anfaller den. Simmar bra (följer sälen ut i vattnet). Vaken dag som natt
  och går aldrig i ide. När den fällt ett byte **äter den i 10 s** och lämnar resten (kadavret märks som rester, den går
  inte tillbaka) – det lever fjällräven på.
- **Stats:** 70 liv, 9 skada, fart 0,3, knockback-motstånd 0,7. Tjock hud: kräver Tusk Dagger eller bättre.
- **Ljud:** isbjörnens egna läten.

| Omgång | Vad | Chans | Antal |
|---|---|---|---|
| 1 | Raw Polar Bear Pelt | 100 % | 1 |
| 2 | Raw Polar Bear | – | 4–6 |
| 2 | Bear Fat | 90 % | 2–4 |
| 2 | Bear Claw | 60 % | 1–3 |
| 2 | Polar Bear Liver | 100 % | 1 |
| 2 | Sinew | 60 % | 1–2 |

**Produkter:**
- Raw Polar Bear (hunger 3) → Campfire 20 s / rökugn → **Cooked Polar Bear** (hunger 8, mättnad 0,8). Torkas till Game Jerky.
- **Polar Bear Liver:** giftig av A-vitamin. Äter man den får man Nausea (20 s), Poison II (15 s) och Hunger II (30 s).
- Raw Polar Bear Pelt → torkställning → **Polar Bear Pelt**.
- **Polar Bear Cloak:** 2 Polar Bear Pelt + Leather + Sinew (crafting). Bärs i Cloak-slotten, rustning 3, isolerar 25 %
  mot kyla (`#mechanical:fur_insulation`), med en vit björnfäll som cape.
- **Polar Bear Rug:** Polar Bear Pelt + Bone – platt dekoration som Bear Rug.

## Arctic Fox (fjällräv)

- **Var:** snowy plains, ice spikes, snowy beach och Glacial Moraine. Vikt 4, 1–2.
- **Beteende:** liten och rund med korta ben, små runda öron och tjock svans. **Följer isbjörnar:** finns en vuxen
  isbjörn inom 32 block håller räven sig inom ungefär 10 block från den. **Äter rester:** ett kadaver inom 24 block som
  ingen vaktar (ingen spelare inom 10 block, ingen isbjörn precis vid det, inte buret, släpat eller upphängt) äter den
  upp på 8 s, och en Bone Pile blir kvar. Jagar också lämlar. Skygg för spelare.
  **Pälsen följer kalendern:** snövit från sista månaden på hösten till slutet av vårens första månad (dag 240–29 på
  året), gråbrun ovan och ljus under resten av året. Bytet sker för levande rävar inom 5 s.
- **Stats:** 8 liv, 2 skada, fart 0,32.

| Omgång | Vad | Chans | Antal |
|---|---|---|---|
| 1 | Raw Arctic Fox Pelt | 90 % | 1 |
| 2 | Raw Arctic Fox | – | 1 |

**Produkter:** Raw Arctic Fox (1) → **Cooked Arctic Fox** (3 / 0,5); torkas till Small Game Jerky. Raw Arctic Fox Pelt
→ **Arctic Fox Fur**. **Arctic Fox Hat:** 3 Arctic Fox Fur + Sinew: hjälm, rustning 1, isolerar 25 %.

## Snow Leopard (snöleopard)

- **Var:** frozen peaks, jagged peaks och stony peaks. Vikt 1, ensam – syns sällan.
- **Beteende:** jagar som Cave Lion (smyger hukande, fryser när bytet tittar, kastar sig fram), men **när som helst på
  dygnet**: Ibex framför allt, Chamois, Mouflon, Marmot och harar. En spelare är byte bara **på natten**; på dagen håller
  den sig undan folk på dubbelt så långt avstånd som annat vilt. Rädd för eld och facklor (Predator). Landar på fötterna:
  de första 12 blocken av ett fall gör inte ont. Släpar bort ett färskt byte.
- **Stats:** 26 liv, 5 skada, fart 0,32.
- **Ljud:** ozelotens läten, låga.

| Omgång | Vad | Chans | Antal |
|---|---|---|---|
| 1 | Raw Snow Leopard Pelt | 100 % | 1 |
| 2 | Raw Snow Leopard | – | 1–3 |
| 2 | Sinew | 50 % | 1–2 |
| 2 | Bone | 50 % | 1–2 |

**Produkter:** Raw Snow Leopard (2) → **Cooked Snow Leopard** (6 / 0,7); torkas till Game Jerky. Raw Snow Leopard Pelt →
**Snow Leopard Pelt**.
- **Snow Leopard Boots:** Leather Boots + 2 Snow Leopard Pelt + Sinew. Rustning 1, isolerar 25 %, och mjuka sulor: de
  första **4 blocken** av ett fall gör inte ont.
- **Snow Leopard Rug:** 2 Snow Leopard Pelt + Bone.

## Gemensamt

- Rått kött förskäms på 1 dag, tillagat på 3; allt ruttnar till Rotten Flesh; rått kött kan rökas och torkas.
- Prestationer: Wilderness – White Bear, Ice Walker, White Rug, Too Much of a Good Thing, Camp Follower, Snow-White Hat,
  Grey Ghost, Soft Landing, Grey Rosettes; Food – Arctic Fare; Hunting – Polar Bear och Snow Leopard (Predators),
  Arctic Fox (Small Game).
- Test (`ColdTest`): spawnlistor per biom och att vanilla-isbjörnen byts ut; isbjörnen väljer en säl vid iskanten som
  byte; fjällräven tar sig fram till en isbjörn och äter dess rester; pälsens färg efter kalendern; snöleoparden väljer
  en stenbock som byte.
