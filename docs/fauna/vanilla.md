# Vanilla-djuren (vanilla)

Genomgång av de vanilla-varelser som spelaren fortfarande möter, och nya modeller för dem som stack ut mest bredvid
moddens egna djur. Beteendet är vanilla: bara utseendet är nytt.

## Nya modeller: gårdsdjuren

**Cow**, **Pig**, **Sheep** och **Chicken** spawnar inte vilt längre men lever i byarnas fållor (VillageAnimals) och
föds upp av spelaren, så de syns mest av alla vanilla-djur. De har nu egna modeller och hudar i samma stil som moddens
djur (Aurochs, Wild Boar, Mouflon, Jungle Fowl), men går att känna igen som samma djur och behåller vanilla-varianterna:

- **Cow:** en lantraskor, mindre och finare än uroxen: djup buk, höftknölar, juver, hals med dröglapp, långt ansikte med
  bred, fuktig mule, öron åt sidan under korta horn, lång svans med tofs.
  - temperate: mörkbrun med vita fläckar och vitt ansikte (som vanillakon);
  - warm: röd zebu med puckel över bogen, stor dröglapp, hängande öron och lyrformade horn;
  - cold: raggig rödbrun höglandsko med lugg över ögonen, kjol av långt hår under buken och breda horn.
- **Pig:** en gammal lantras med längre ben och tryne än dagens grisar: borstkam längs ryggen, tunga käkar, trynskiva,
  öron som hänger fram över ögonen och knorrsvans. temperate skär, warm rödbrun med mörk sadel, cold en krullig ullgris
  (egen modell, pälsen står ut).
- **Sheep:** ett hornlöst lantrasfår (tunna ben, romersk nos, öron åt sidan) med en egen fleece-modell som färgas med
  ullfärgen. Ullen är målad nästan vit med lockar, så färgad ull blir ren. Klippt visar fåret sin korta stubb.
- **Chicken:** en lantrashöna: äggformad kropp, uppåtriktad stjärt, halsfjädrar, röd kam och slör, gula fjälliga ben
  med tår. temperate vit, warm röd-guld rapphönsfärgad, cold en fluffigare spräcklig grå höna med fjäderklädda ben.
- Varje unge har en egen modell (kalv, kultingar, lamm med egen fleece, kyckling) i variantens färger.

Delarna heter som i vanilla-modellerna (head, body, benen, vingarna), och modellklasserna ärver vanillas, så vanillas
rörelser, betesställningen vid Feeding Trough (LivestockPose) och kadaverställningarna fungerar som förut. Svansen
svänger när djuret går, och när halsen sänks långt (bete, tråg) böjs ansiktet bakåt så att mulen når marken.
Kadavren får en styckad hud (`textures/entity/skinned/vanilla/`), hönan en plockad. Grisens vanilla-sadel ritas som
förut på sin egen vanilla-modell.

Kod: `client/vanilla/FarmAnimalRenderers.java` (egna renderare för vanillas EntityTypes, med vanillas render states).
Modeller och hudar: `scripts/gen_vanilla_models.py` skriver `client/vanilla/VanillaGeometry.java` (genererad) och
`textures/entity/vanilla/<djur>/<vanillatexturens namn>.png`, en per vanilla-varianttextur.

## Andra omgången: vilda djur och sällskapsdjur

**Rabbit**, **Fox**, **Wolf**, **Cat**, **Ocelot** och **Camel** har också fått egna modeller och hudar. De behåller
vanilla-skelettet (samma delnamn, pivotpunkter och rotationer) med egna lådor, så vanillas modellklasser driver dem som
förut: kaninens hopp och huvudlutning, rävens sittande, sovande, smygande och språng och bäret i munnen, vargens
sittande, skakning och svans, kattens sittande och liggande, kamelens gång, sittande och rusning. Kroppsdelar som vanilla
lägger roterade har ett uppvänt barn ("shape") som bär lådorna, så de byggs och målas i vanligt modellrum.

- **Rabbit:** rund bakdel med tjocka lår, kortare öron med skär insida, runda kinder, vit tofs; alla vanilla-varianter
  (brun agouti, vit och röd-ögd Caerbannog, svart, fläckig, guld, salt och peppar, Toast som holländsk kanin).
- **Fox:** smal nos med vit underkäke och kinder, svartbaksade spetsiga öron, svarta strumpor, yvig svans med vit spets,
  bärnstensgula ögon; Snow-räven vit. Sovande hud med slutna ögon.
- **Wolf:** kraftig manke och nackkrage, smalare midja, buskig svans med mörk spets, mörk sadel över ryggen. Alla nio
  varianter, och för var och en vild, tam och arg hud (arg: röda ögon, rynkad panna, blottade tänder). Halsbandet är ett
  band runt den nya halsen i halsbandets färg. **Vargrustningen ritas fortfarande på vanillas vargform.**
- **Cat och Ocelot:** smal kropp med hög bakdel, kinder, spetsiga öron, tunn svans; alla elva kattvarianter
  (strimmig tabby med M i pannan, tuxedo, röd, siames och ragdoll med mörka spetsar, british shorthair, kalikå, perser,
  vit, jellie, helsvart) och ozeloten med rosetter. Katten är som i vanilla 0,8 gånger ozelotens storlek.
- **Camel:** djupt bröst med knäskydd, en stor rundad puckel, S-formad hals (framåt och upp), långt huvud med hängläpp,
  långa ben med hårda knäskydd och breda fotsulor, tofs på svansen. **Sadeln ritas på vanillas kamelform.**
- **Ungarna** (kanin, räv, valp, kattunge, ozelotunge, kamelföl) har kvar vanillas egna ungmodeller och hudar.
- **Pig:** kroppen har nu djup bog över ett smalare bröst, en bred hängande buk, en rundad skinka och en välvd länd.

Kod: `client/vanilla/WildVanillaRenderers.java`; modellerna är vanillas egna klasser (AdultRabbitModel, AdultFoxModel,
AdultWolfModel, AdultCatModel, AdultOcelotModel, AdultCamelModel) med `VanillaGeometry` som skelett, så de generiska
kadaverställningarna fungerar också.

## Genomgång av resten

Vanilla-djur som inte finns kvar: Creeper (ersatt av Gloson och Mushroom Man), Polar Bear (moddens egen), Wandering
Trader och Trader Llama (karavaner), vilda Horse, Donkey och Llama (moddens Wild Horse), Goat (borttagen i bergen).
Zombie och Skeleton har redan egna hudar (UndeadRenderers).

Bedömning av de övriga (bra = får vara kvar, behöver arbete = ser blockig ut bredvid moddens djur):

- **Omgjorda i andra omgången:** Wolf, Rabbit, Fox, Cat, Ocelot, Camel (se ovan).
- **Bra nog / stiliserade med flit:** Spider och Cave Spider, Bat, Villager och Witch, Iron Golem, Enderman, Slime, Husk,
  Drowned, Stray, Panda, Parrot, Turtle, Frog, Armadillo, Bee, Squid, Dolphin, fiskarna, Mooshroom och Nether-varelserna
  (Piglin, Hoglin, Strider, Ghast, Blaze).

## Bilder

Tagna med `VanillaShots` (klienten, bara med `run/vanillashots.flag`; andra raden kan lista vilka djur som ska tas), i
`docs/shots/fauna/`: `vanilla_<djur>.png` bredvid moddens motsvarighet (kon vid uroxen, grisen vid vildsvinet, fåret
vid mufflonfåret, hönan vid djungelhönan), närbilder i ögonhöjd `_front`, `_side`, `_q`, och för gårdsdjuren det
styckade kadavret `_carcass`. Före-bilderna heter `vanilla_before_<djur>.png`. Ställningar (andra omgången):
`_walk` (gång), `rabbit_hop`, `fox_sit`, `fox_sleep`, `fox_crouch`, `fox_pounce`, `fox_item` (bär i munnen),
`wolf_angry`, `wolf_tame` och `wolf_sit` (halsband), `cat_sit` (halsband), `cat_lie`, `camel_sit`; före-bilder av
ställningarna som `vanilla_before_<namn>.png`.
