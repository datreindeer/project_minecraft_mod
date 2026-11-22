# Öken och badlands (desert)

Åtta djur från öknen (Desert, Gypsum Flats) och badlands (Badlands, Eroded/Wooded Badlands, Ochre Badlands): **Desert Scorpion**, **Fennec Fox**, **Wild Camel** och **Sand Viper** i öknen; **Puma**, **Bighorn Sheep**, **Rattlesnake** och **Condor** i badlands. Alla utom kondoren lämnar ett kadaver som styckas med kniv (Butchery) och har eget kött, egen hud/päls och egna delar med användning. Kondoren ger fjädrar som gamen (loot).
Kod: `wildlife/desert/` (DesertFauna, DesertScorpion, FennecFox, TameFennec, WildCamel, TameCamel, SandViper, Rattlesnake, Puma, BighornSheep, Condor, DesertFaunaTest), klient `client/wildlife/desert/` (DesertFaunaClient, ScorpionModel, FennecModel, CamelModel, BighornModel; pumans modell är leopardens, ormarnas SnakeModel, kondorens gamens). Data: `scripts/gen_fauna_desert.py`, texturer: `scripts/gen_fauna_desert_textures.py` (kör `gen_fauna_desert_tex_items.py`). Ökenväsendet Dune Wraith hör till folktron och byggs inte här.

## Gemensamt

- Allt kött: Campfire 20 s eller smoker 5 s; råköttet förskäms på 1 dag, stekt på 3 dagar, ruttnar till Rotten Flesh, kan torkas och rökas (`smokable_meat`).
- Råa hudar skrapas med kniv i crafting till **Scraped Hide** (kamel 3, bighorn 2) och torkas sedan på Drying Rack till Leather som vanligt.
- Pumapälsen **fettgarvas** i crafting som leopardens: Raw Puma Pelt + kniv + fett (`mechanical:tanning_fats`, där **Camel Hump Fat** nu också ingår) → Puma Pelt.
- Ormarna (Sand Viper, Rattlesnake) ger samma Snakeskin, Raw Snake och Venom Gland som huggormen (se [äng och hed](meadow.md)); Snakeskin Boots stoppar alla ormbett.
- Spawnar i data (biome modifiers `add_<djur>_desert.json`); WildlifeRestock fyller på av sig själv. Nattdjuren (skorpion, fennek, puma, skallerorm) spawnar i alla ljus, dagdjuren bara i dagsljus. Marken: sand, röd sand, terrakotta, sandsten, grovjord, gräs eller sten.

## Desert Scorpion (ökenskorpion) ★

| | |
|---|---|
| Var | desert, Gypsum Flats, alla badlands, Ochre Badlands (vikt 6, 1–2) |
| Liv / fart | 6 / 0,24, skada 2, rustning 3; inga ungar |
| Utseende | halmgul med mörkare bandade ryggplåtar, två klor, åtta tunna ben, svansen krökt upp över ryggen till en mörk gadd |
| Beteende | **gömmer sig på dagen** (ligger platt och still, sover) och är ute på natten. På natten sticker den den som går rakt fram till den (inom 1,8 block, att smyga hjälper inte); den som väcker eller skadar den blir också stucken. **Giftsticket** ger Weakness i 10 s och Poison i 3 s. Efter ett stick drar den sig undan. Flyr aldrig för spelare. |
| Ljud | silverfiskens och spindelns, ljusa |

| Styckning | Antal | Chans |
|---|---|---|
| Raw Scorpion | 1 | – |
| Scorpion Venom | 1 | 80 % (inre) |

- **Scorpion Venom**: 8 Arrow runt giftet → 8 Arrow of Poison (giftpilar).
- **Raw Scorpion** steks till Roast Scorpion (3 mat).

## Fennec Fox (ökenräv)

| | |
|---|---|
| Var | desert, Gypsum Flats (4, 1–2) |
| Liv / fart | 8 / 0,33; ungar (30 %) |
| Utseende | liten krämgul räv med vit buk och vitt ansikte, **enorma öron** (rosa inuti), buskig svans med svart spets |
| Beteende | **nattaktiv**: sover hoprullad i sin grop på dagen och strövar på natten. Mycket skygg (1,6). |
| Ljud | rävens, ljusare |

**Tämjning:** smyg fram med rått kött i handen (vanilla-kött eller moddens vilt) – då sätter den sig och tittar på dig – och handmata den (högerklick) **3 gånger**, en gång per 5 s. Står du upp när du matar den flyr den; skadar du den försvinner förtroendet. Tämj den på natten: på dagen sover den och flyr när den väcks.

**Tame Fennec** (tämjd fennek): följer sin ägare, sätter sig vid högerklick (igen för att resa sig) och äter kött ur handen för att läka (4 liv). **Varnar för fiender på natten:** när det är mörkt ute och något fientligt (zombie, skelett, spindel, creeper …) kommer inom 20 block **gläfser den**, vänder sig mot det och **markerar det** (Glowing i 6 s), och ägaren får ett meddelande om den är inom 48 block. Varnar inte två gånger för samma varelse inom 30 s. Slåss inte. Inget kadaver.

| Styckning | Antal | Chans |
|---|---|---|
| Raw Fennec | 1 | – |
| Raw Fennec Pelt | 1 | 90 % (yttre) |
| Sinew | 1 | 30 % |

- **Raw Fennec Pelt** + kniv → **Fennec Pelt**; 3 Fennec Pelt + Twine (` T / F F / F `) → **Leather Pouch**.

## Wild Camel (vild dromedar)

| | |
|---|---|
| Var | desert, Gypsum Flats (3, 2–4) |
| Liv / fart | 30 / 0,25, steg 1; kalvar (25 %) |
| Utseende | hög, långbent sandbrun dromedar med **en puckel**, lång hals som går fram och ner från bröstet och sedan upp, lång mule, små öron, tunn svans med tofs, breda gråbruna trampdynor |
| Beteende | hjord, skygg (1,2), slåss aldrig. Passgång (benen på samma sida tillsammans), nickar med halsen. |
| Ljud | vanilla-kamelens |

**Tämjning** exakt som onagern: smyg fram med hästfoder (`mechanical:horse_feed`), handmata (en gång per 10 s), **Rope Halter** vid förtroende 3, tam vid 7. Skadar du den försvinner förtroendet.

**Tame Camel** (tämjd kamel, vanilla-åsna under ytan som den tämjda onagern): rids barbacka, 28–34 liv, fart 0,17–0,22, hopp 0,4–0,5. **Pack Baskets** ger en packning med 15 platser (remmen går över puckeln). Drar **Cart**. Två tämjda kameler som får kärvar eller hö får en tam kalv (inte med onager eller häst). **Klarar öknen utan vatten:**
- arbetsorken (HorseWork) töms **hälften så fort** per gånget block som hästens (`CAMEL_DRAIN` 0,5): kamelen går dubbelt så långt innan den blir trött;
- en drink från tråget (Husbandry) räcker **fyra gånger så länge** (`CAMEL_WATER` 4: 8 dagar i stället för 2).
- Vanilla-kameler (från ökenbyarna) får samma fördelar.

| Styckning | Antal | Chans |
|---|---|---|
| Camel Meat | 3–5 | – |
| Raw Camel Hide | 1 | 80 % (yttre) |
| Camel Hump Fat | 1–2 | 70 % |
| Sinew | 1–2 | 60 % |

- **Raw Camel Hide** + kniv → 3 Scraped Hide.
- **Camel Hump Fat**: garvfett (`mechanical:tanning_fats`, för leopard- och pumapälsar); + Twine → Candle.

## Sand Viper (sandorm, hornhuggorm)

| | |
|---|---|
| Var | desert, Gypsum Flats (5, 1) |
| Liv / fart | 6 / 0,18, skada 2; inga ungar |
| Utseende | kort, tjock, ljust sandfärgad med svaga mörkare sadlar, mörkt streck bakom ögat och ett litet "horn" över varje öga |
| Beteende | på sand **gräver den ner sig** (bara ringlarna och huvudet syns, ingen skugga) och ligger så i flera minuter. Nergrävd **biter den den som kliver inom 1,7 block** – att smyga hjälper inte, den känner stegen. Ovan sanden är den en huggorm: biter den som går rakt fram utan att smyga, giftbett (Poison 6 s), Snakeskin Boots stoppar bettet; efter ett bett glider den undan. |
| Ljud | huggormens |

| Styckning | Antal | Chans |
|---|---|---|
| Raw Snake | 1 | – |
| Snakeskin | 1 | 85 % (yttre) |
| Venom Gland | 1 | 75 % |
| Viper Fang | 1–2 | 60 % |

- **Viper Fang**: på Fletching Table → 4 Barbed Arrow (som björnklo och hyenatand).

## Puma (bergslejon) ★

| | |
|---|---|
| Var | alla badlands, Ochre Badlands (1, 1) |
| Liv / fart | 26 / 0,33, skada 4,5 (språng 8), steg 1, inga ungar |
| Utseende | slät gulbrun katt utan fläckar (leopardens byggnad, något större), ljus buk och mule, mörka mulsidor, örbaksidor och svansspets |
| Beteende | ensam jägare som ärver grottlejonets jakt: kryper hukad, fryser när den blir sedd, **språng** (8 skada), rädd för eld och facklor. **På dagen ligger den uppe på en klipphylla** – en plats med minst 4 blocks fall bredvid – inom 14 block. **Från en hylla hoppar den ner över bytet**: ett byte minst 2,5 block nedanför och högst 10 block bort som den ser. Tar ingen fallskada av de första 12 blocken. Byten: bighorn, ökenhare, onager, mufflon, vildsvin, ökenödla, fennek, kamelkalvar, människor. |
| Ljud | ozelotens, mycket mörkare; rävens skrik som jaktläte |

| Styckning | Antal | Chans |
|---|---|---|
| Puma Meat | 1–3 | – |
| Raw Puma Pelt | 1 | alltid (yttre) |
| Sinew | 1 | 50 % |
| Bone | 1–2 | 60 % |

- **Raw Puma Pelt** + kniv + fett → **Puma Pelt**. 2 Puma Pelt + Bone → **Puma Rug** (platt matta, Bear Rug-modellen); Puma Pelt + kniv → 2 Leather.

## Bighorn Sheep (tjockhornsfår)

| | |
|---|---|
| Var | alla badlands, Ochre Badlands (4, 2–4) |
| Liv / fart | 20 / 0,28, steg 1, säkert fall 8; 40 % baggar, lamm (30 %) |
| Utseende | kraftigt brunt vildfår med vit bakdel, buk och mule; **baggen har väldiga horn som böjer sig i en hel cirkel** bakåt, ner bakom örat och fram förbi kinden; tackan korta, lätt böjda horn |
| Beteende | klättrar uppför klippväggar och flyr uppför som stenbocken (som den ärver av). Baggar drabbar samman i brunsten. **Stångar:** en bagge som skadas av någon inom 10 block sänker hornen och **anfaller**: stöten gör 5 skada och kastar dig bakåt, sedan springer den uppför. |
| Ljud | getens |

| Styckning | Antal | Chans |
|---|---|---|
| Bighorn Mutton | 2–3 | – |
| Raw Bighorn Hide | 1 | 70 % (yttre) |
| Bighorn Horn | 1–2 | 60 % |
| Sinew | 1 | 40 % |

- **Raw Bighorn Hide** + kniv → 2 Scraped Hide.
- **Bighorn Horn**: på Fletching Table räcker **ett** horn (i stället för två stenbockshorn) till en **Horn Bow** (2 Sinew + Sinew Bow + Bighorn Horn); + kniv → 3 Bone Needle.

## Rattlesnake (skallerorm)

| | |
|---|---|
| Var | alla badlands, Ochre Badlands (5, 1) |
| Liv / fart | 8 / 0,2, skada 3; inga ungar |
| Utseende | tung, gråbrun orm med mörka ljuskantade romber längs ryggen, en ljus linje från ögat till käken, svartvitt ringad svans som slutar i en gulbeige skallra |
| Beteende | **varnar alltid innan den biter:** kommer en spelare inom 5 block reser den sig i ett S, lyfter svansen och **rasslar** med skallran. Bara den som efter 1,5 s varning fortfarande är inom 2,2 block blir biten (att smyga lugnar den inte). Det räcker att backa. Giftbett, Snakeskin Boots stoppar bettet. |
| Ljud | skallran (ett snabbt surrande rassel); ingen väsning |

| Styckning | Antal | Chans |
|---|---|---|
| Raw Snake | 1–2 | – |
| Snakeskin | 1 | 85 % (yttre) |
| Rattlesnake Rattle | 1 | alltid (yttre) |
| Venom Gland | 1 | 75 % |

- **Rattlesnake Rattle** + Twine + Stick (` R / T / S `) → **Shell Rattle** (håller rovdjur borta som en fackla när den hålls i handen).

## Condor (kondor)

| | |
|---|---|
| Var | alla badlands, Ochre Badlands (2, 1–3) |
| Liv | 14 |
| Utseende | jättestor svart gam (gamens modell, 1,4 gånger större) med vit krage, vita fält på vingarnas ovansida och bart rödgrått huvud |
| Beteende | en gam i större skala (Vulture): seglar högst av alla fåglar (24–44 block upp) i vida cirklar och **ser kadaver på 128 blocks håll**. Då **cirklar kondorerna över kadavret** i en långsam pelare som syns långt bort och visar var det finns kadaver, lockar dit nattens jägare och landar till sist och äter tills bara ett benhög är kvar. |
| Ljud | gamens väsning, mörkare |

| Loot | Antal |
|---|---|
| Feather | 3–5 |
| Condor Feather | 1–3 |

- **Condor Feather**: på Fletching Table med Flint och Stick → **6** Eagle-Fletched Arrow (örnfjädern ger 4).

## Avancemang

- Bronsåldern: **Ship of the Desert** (tämj en kamel).
- Vildmark: **Sting in the Tail** (Scorpion Venom), **Big Ears** (tämj en fennek), **Under the Sand** (Viper Fang), **Fair Warning** (Rattlesnake Rattle), **Cliff Cat** (utmaning: pumapäls), **Mountain Lion** (Puma Rug), **Full Curl** (Bighorn Horn), **Giant of the Sky** (Condor Feather).
- Mat: **Desert Fare** (stekt kött från de nya djuren).
- Jaktfliken: skorpion, fennek, sandorm, skallerorm under Small Game; kamel och bighorn under Big Game; puma under Predators.

## Test

`DesertFaunaTest`: spawnbiomer (varje art bara i sina biomer), styckning, skorpionens stick och dagsömn, fenneken som tämjs och markerar en zombie, kamelens orkdränage och drickstid, sandormen som gräver ner sig, skallerormen som rasslar innan den biter, pumans hopp från hyllan och fallskydd, bighornbaggens anfall, kondorens syn.
