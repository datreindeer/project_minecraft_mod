# Djupet (deep): grottsjöar, grottmynningar och trånga gångar

Resten av livet under jord ur [fauna-expansion-plan.md](../fauna-expansion-plan.md) ("Under jord"): **Cave Salamander**
(grottolm), **Blind Cave Fish**, **Bat Swarm** och moddens egen grottspindel, **Cavern Spider**, med sitt stora nät **Cave
Web**. De kompletterar [underground.md](underground.md) (Cave Hyena, Cave Bear, Giant Centipede, Vittra, Troll, Lindworm).

Kod: `wildlife/deep/` (DeepFauna = registreringar och de två spawnarna, CaveSalamander, BlindCaveFish, BatSwarm,
CavernSpider, CaveWebBlock), klient `client/wildlife/deep/` (DeepModels, DeepClient; olmen och fisken ritas med kustfaunans
`CoastClient.FishRenderer`). Data: `scripts/gen_fauna_deep.py`, texturer: `scripts/gen_fauna_deep_textures.py`.
Gametester: `DeepTest` (`deep_*`, `steppe_biome`). Bilder: `docs/shots/fauna/deep_*.png`.

**Inget av dem lyser.** Grottorna är kolsvarta ([underground-plan.md](../underground-plan.md) steg 4): olmen, fisken,
fladdermössen och spindeln (vars ögon är små och matta, inte röda och lysande som vanillaspindelns) syns bara i ljuset man
själv har med sig, eller med Night Vision.

## Var de finns

| Art | Var | Spawn |
|---|---|---|
| Cave Salamander | mörka grottsjöar | DeepFauna: var 10:e s har en spelare i en grotta 30 % chans; 8–32 block bort letar den nedåt efter vatten under y 50 utan himmels- och blockljus där minst 5 av de 9 blocken runt är vatten (en sjö, inte en droppe). 1 av 3 gånger 1–2 olmar (högst 3 inom 48 block) |
| Blind Cave Fish | samma sjöar | 2 av 3 gånger ett stim på 3–5 (högst 8 inom 48 block). Fångas också på kroken, se nedan |
| Bat Swarm | grottmynningar i skymningen | i skymningen (klockan 11 800–13 800) har en spelare ovan jord var 10:e s 25 % chans; 12–48 block bort söks en grottmynning nedåt från ytan: luft (2 block) utan fri himmel men med lite dagsljus som läcker in (himmelsljus 1–12) och naturlig sten 2–6 block ovanför. Högst 1 svärm inom 48 block |
| Cavern Spider | grunda grottor, y 40 till 0 | Undergrounds mörkerregel (ljusnivå 0, ingen himmel), vikt 2 i samma band som Cave Hyena (3) och Cave Bear (1), alltid ensam |

Ingen spawnar inom 8 block från en spelare. Alla kan försvinna när ingen är i närheten.

## Cave Salamander (grottolm)

- **Utseende:** blekrosa, fingerlång och slank, platt nos, **inga ögon**, röda fjäderlika gälar på huvudets sidor, fyra små
  ben och en stjärt med fena. 32×32-textur.
- **Beteende:** helt vattenlevande (en fisk i koden): kryper och driver långsamt, torkar ut på land. **Blind**: ljus och en
  smygande spelare bryr den sig inte om. Den **känner när vattnet rör sig**: simmar något förbi inom 3 block slinker den
  iväg åt andra hållet. Flyr när den blir slagen. Tyst; axolotlns ljud när den skadas.
- **Stats:** 4 liv. Ingen hink (den glider ur handen): döda den.
- **Tappar:** sig själv, en **Cave Salamander** (inget kadaver).

**Produkter:**
- **Cave Salamander** (mat 1, mättnad 0,1; förskäms på 1 dag).
- **Dark-Sight Broth:** Cave Salamander + Glow Lichen + Bowl (formlöst) → en skål. Äts alltid: hunger 3, mättnad 0,4 och
  **Night Vision i 3 minuter**. Skålen blir kvar. Det gamla botemedlet mot mörkret (Vittra Eye ger en riktig dryck, Night
  Eye Salve av Bat Wing 90 s).

## Blind Cave Fish (blind grottfisk)

- **Utseende:** liten, blekrosa och ögonlös, rosa gälspringa, ryggfena och stjärtfena.
- **Beteende:** stim på 3–5 som följer en ledare. **Blind**: flyr inte från spelare (vanillafiskarnas flykt är borttagen),
  bara när den blir slagen. Torskens ljud.
- **Fiske:** i fiskeloot (`gameplay/fishing/fish`, som gen_data.py skriver) finns Blind Cave Fish med vikt 150 när
  **blocket ovanför flötet** inte ser himlen och ligger under y 50, alltså i en grottsjö. Där är den den vanligaste fångsten
  (ungefär 3 av 5 fångster); under öppen himmel nappar den aldrig. (Vattnet självt dämpar himmelsljuset ett steg, därför testas
  blocket ovanför.)
- **Tappar:** **Blind Cave Fish** (och 5 % benmjöl).

**Produkter:** Blind Cave Fish (rå 1 hunger) → Campfire 15 s / smoker → **Cooked Cave Fish** (hunger 4, mättnad 0,6). Kan
rökas. Rå förskäms på 1 dag, stekt på 3.

## Bat Swarm (fladdermussvärm)

- **Utseende:** 18 små fladdermöss som virvlar i ett moln (1,2 block brett), vingarna slår i otakt.
- **Beteende:** i skymningen väller svärmen ut ur en grottmynning och **virvlar runt mynningen i 15 s**, stiger sakta ut
  ur den med ett väldigt pipande, och **drar sedan iväg** över landet och uppåt. Efter 45 s är den borta. **Där
  fladdermössen kommer ut finns en grotta**: det är så man hittar grottorna. Ofarlig, går rakt igenom berget (ett moln).
- **Stats:** 4 liv.
- **Tappar:** 1–3 **Bat Wing** och 1–2 **Guano** (samma som vanliga fladdermöss ger, se `VanillaGame`); inget kadaver.

**Produkter:** Bat Wing → Night Eye Salve (2 + Soot + Lard; Night Vision 90 s). Guano: starkaste gödslet.

## Cavern Spider (grottspindel) och Cave Web

- **Utseende:** grå, handflatsbred, liten framkropp, huvud med två svarta huggtänder och små matta ögon, stor bakkropp som
  pekar snett uppåt med bleka vinklar längs ryggen, åtta långa ben som går i två omväxlande uppsättningar. 64×32-textur.
- **Beteende:** vandrar inte. Var 30:e s, om den inte jagar och det inte finns något nät inom 8 block, **spinner den ett nät
  tvärs över närmaste trånga gång** inom 4 block: en luftruta där väggarna på ena ledden står högst 3 block isär, taket är
  högst 3 block upp och gången är öppen åt andra hållet (en gång, inte en nisch). Hela tvärsnittet, vägg till vägg och golv
  till tak, blir **Cave Web**. Sedan väntar den intill. Den går på spelare inom 6 block. **Bettet** ger **Slowness II i 3 s**.
  Klättrar på väggar och tar ingen fallskada. Skyr ljus som de andra mörkrets väsen: den släpper den som håller en fackla
  eller står i facklans sken (`Underground.inLight`) och backar undan. Flyr inte annars, sover aldrig.
- **Stats:** 14 liv, 3 skada, fart 0,3.

**Cave Web:**
- Den som går in i nätet **hålls fast mycket hårdare än av spindelväv** (en tiondel av farten, nästan inget hopp) och har
  **Slowness II** en stund efter. När något sprattlar i nätet kommer **varje grottspindel inom 16 block**. Spindlarna själva
  går rakt igenom.
- Bryts med **kniv eller sax** (eller Silk Touch): nätet kommer loss helt och kan **sättas ut igen som fälla** (för djur,
  monster eller andra spelare). Annars blir det bara String. Det tar en stund att skära sig loss: ungefär 4 s med kniv
  eller svärd, 6 s med bara händerna.

| Omgång | Vad | Chans | Antal |
|---|---|---|---|
| 1 | Silk Gland | 90 % | 1–2 |
| 2 | Spider Leg | – | 2–4 |
| 2 | Cavern Spider Fang | 70 % | 1–2 |
| 2 | Spider Eye | 50 % | 1–2 |

**Produkter:**
- **Spider Leg** (rå 2 hunger) → Campfire 15 s / smoker → **Roasted Spider Leg** (hunger 5, mättnad 0,6). Kan rökas.
- **Silk Gland** + kniv → **3 String** (kniven blir kvar).
- **Cavern Spider Fang:** 8 Arrow runt huggtanden → **8 Arrow of Slowness** (spindelns bedövande bett).
- **Cave Web** som fälla (ovan).

## Advancements

Wilderness (efter *The Cold Deep* om inget annat sägs): **Pale Dweller** (Cave Salamander), **Eyes for the Dark**
(Dark-Sight Broth), **Fishing in the Dark** (Blind Cave Fish), **Out of the Hill** (döda en Bat Swarm; från fliken
Wilderness rot), **Caught in the Web** (Silk Gland eller Cavern Spider Fang), **Spinner's Trap** (en hel Cave Web). Food:
**Food from the Deep** (Cooked Cave Fish eller Roasted Spider Leg). Hunting: grottspindeln ligger i gruppen *The Deep*.

## Mammoth Steppe

Den nya ytbiomen som hör till samma paket finns i [biomes.md](../biomes.md#mammoth-steppe): mammutar, ullhårig noshörning,
stäppbison, myskoxe, saiga, vildhästar, grottlejon och vargar på torrt gräs över loess.

## Bilder

`docs/shots/fauna/`: `deep_cave_salamander.png`, `deep_blind_cave_fish.png`, `deep_bat_swarm.png`, `deep_cavern_spider.png`,
`deep_cave_web.png`, `steppe_mammoth_steppe.png`.
