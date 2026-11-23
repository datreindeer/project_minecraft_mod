# Djungel och mangrove (jungle)

Nio djur från djungeln och mangroveträsket. **Djungel / bamboo jungle:** **Tiger**, **Python**, **Orangutan**,
**Monitor Lizard** och **Poison Dart Frog**. **Mangrove swamp:** **Saltwater Crocodile**, **Mudskipper**, **Fiddler Crab**
och **Proboscis Monkey**. De stora lämnar ett kadaver som styckas med kniv (Butchery); groda, slamkrypare och krabba är för
små och tappar sina saker direkt. Leopard och Tapir finns redan i [varma marker](warm.md).
Kod: `wildlife/jungle/` (JungleFauna, Tiger, Python, Orangutan, MonitorLizard, DartFrog, Crocodile, Mudskipper, FiddlerCrab,
ProboscisMonkey, Grip, BlowpipeItem, JungleTest), klient `client/wildlife/jungle/` (JungleFaunaClient, JungleRenderer,
TigerModel, PythonModel, MonitorModel, CrocodileModel, PrimateModel, SmallJungleModels, JungleGeometry, JungleShots).
Data: `scripts/gen_fauna_jungle.py`. Modeller och skinn: `scripts/gen_jungle_models.py` skriver varje djurs delar och
lådor (`JungleGeometry.java`, genererad) och målar texturen och den flådda kadavertexturen ur samma tabell. Övriga
texturer: `scripts/gen_fauna_jungle_textures.py` (kör `gen_jungle_models.py` och `gen_fauna_jungle_tex_items.py`).
Bilder: `JungleShots` (flaggfilen `run/jungleshots.flag`) tar `docs/shots/fauna/jungle_<djur>.png` och närbilder
framifrån, från sidan och snett framifrån (`_front`, `_side`, `_q`) samt kadavret (`_carcass`).

## Gemensamt

- Allt kött: Campfire 20 s eller smoker 5 s (slamkrypare och krabba 15 s); rått förskäms på 1 dag, stekt på 3 dagar,
  ruttnar till Rotten Flesh. Däggdjurs- och reptilköttet kan torkas och rökas (`smokable_meat`).
- Tigerpälsen och krokodilhuden **fettgarvas** i crafting som leopardpälsen: rå + kniv + fett (Tallow, Lard eller Bear Fat,
  `mechanical:tanning_fats`). De passar inte på Drying Rack (fjällig hud och tjock päls garvas med fett här, som i
  [warm](warm.md)).
- Spawnar i data (biome modifiers `add_<djur>.json`); WildlifeRestock fyller på av sig själv.
- **Grepp** (`Grip`, python och krokodil): offret hålls fast vid en punkt. En spelare som går iväg sätts tillbaka, får
  Slowness och kan inte hoppa bort. Offret tar skada varje sekund. **Ett slag från någon annan** slår loss djuret direkt.
  Offrets egna slag räknas som att det kämpar: efter tillräckligt många kommer det loss. Greppet släpper också när offret
  dör, kommer längre bort än 6 block eller när tiden är ute.

## Tiger

| | |
|---|---|
| Var | jungle, bamboo jungle (vikt 2, 1) |
| Liv / fart | 36 / 0,31, skada 6 (språng som grottlejonet), knuffmotstånd 0,5, steg 1, inga ungar |
| Utseende | egen modell: en tung kattkropp med djup bröstkorg och utstickande skulderblad, brett runt huvud med vita polisonger och kort bred nos, små runda öron (svarta baktill med en vit fläck), grova ben på stora tassar, lång ringad svans. Orange över, krämvit lägre ner, vit buk och vita insidor på benen; smala svarta ränder ner längs sidorna och som vinklar över ryggen. Sover liggande på magen med framtassarna framför sig. |
| Beteende | jagar som grottlejonet: kryper hukad, fryser när bytet tittar, **gör språng**, ryter när den blir upptäckt, rädd för eld och facklor. **Osynlig i snåret:** när den kryper genom bambu, ormbunkar, högt gräs, löv, rankor eller plantor (minst 3 sådana block runt fötter och axlar) **syns den inte alls** förrän den är inom 6 block från sitt byte eller gör språnget. I snår jagar den **även på dagen**. **Simmar** gärna och bra. Byten: tapir, vattenbuffel, djungelhöna, påfågel, orangutang, näsapa, varan, vildsvin, rådjur, människor. |
| Ljud | grottlejonets (rytande, morrande), något ljusare |

| Styckning | Antal | Chans |
|---|---|---|
| Tiger Meat | 2–4 | – |
| Raw Tiger Pelt | 1 | alltid (yttre) |
| Sinew | 1–2 | 60 % |
| Bone | 1–2 | 70 % |

- Tjock päls: kräver Tusk Dagger eller bättre för att styckas.
- **Raw Tiger Pelt** + kniv + fett → **Tiger Pelt**.
- 2 Tiger Pelt + Bone → **Tiger Rug** (platt matta, Bear Rug-modellen): troféen.
- 2 Tiger Pelt + 2 Sinew → **Tiger Pelt Mantle**: en mantel för Cloak-platsen (3 rustning som bröstdel, som Otter Fur Cape),
  randig cape över ryggen. Lagas med Tiger Pelt.

## Python (pytonorm)

| | |
|---|---|
| Var | jungle, sparse jungle, bamboo jungle, mangrove swamp (3, 1). Spawnar också **uppe på trädkronorna** (löv räknas som mark för den). |
| Liv / fart | 20 / 0,16, knuffmotstånd 0,6, inga ungar |
| Utseende | egen modell, knappt 4 block lång: brett fyrkantigt huvud med underkäke som kan gapa, värmegropar längs läppen, kluven tunga, tio kroppsled som är tjockast på mitten och en smal svans. Guldbrun med ett svart nät som bildar sadlar över ryggen och ljusa ögonfläckar längs sidorna, svart linje över hjässan, mörkt streck bakåt från ögat, krämfärgad buk. Hoprullad ligger den i en hög spiral med huvudet överst. |
| Beteende | ligger stilla och hoprullad länge, på en gren eller vid vattenkanten. Kommer en spelare eller ett mindre djur (inte rovdjur) inom 1,8 block **slår den till och lindar sig runt offret** (Grip). Från en gren **släpper den sig ner** på byten upp till 6 block under och 3 block åt sidan. Håller fast och **kramar**: ½ hjärta i sekunden. **Någon annan slår loss den med ett slag**; offret kommer loss på egen hand efter **5 egna slag**; den ger upp efter 15 s. Smyger man förbi utanför 1,2 block märker den en inte. Sedan glider den undan och vilar 30 s innan den kan slå till igen. Inget gift. |
| Ljud | väser när den kramar |

| Styckning | Antal | Chans |
|---|---|---|
| Python Meat | 2–3 | – |
| Python Skin | 1 | alltid (yttre) |

- **Python Skin** + kniv → 3 Snakeskin (till Snakeskin Boots, se [meadow](meadow.md)).
- **Python Meat** steks till Cooked Python Meat (4 mat).

## Orangutan

| | |
|---|---|
| Var | jungle (4, 1–2) |
| Liv / fart | 30 / 0,22, steg 1; ungar (30 %) |
| Utseende | egen modell: en stor lurvig roströd hane som går framåtlutad på knytnävarna, mycket långa armar, korta ben, en kjol av långt hår under buken. Litet ansikte med mörka ögon och bred mun inne i stora runda kindvalkar, hängande strupsäck. |
| Beteende | inte skygg (0,4) men vill inte bli trängd: den som kommer inom **7 block** får **frukt och pinnar kastade på sig** (äpplen, söta bär, pinnar; ingen skada, en knuff), var 2,5–4 s. Den är en **tjuv**: saker som ligger på marken inom 10 block **går den och tar** och bär i handen. Slår man den **tappar den bytet**; dör den faller bytet ut. |
| Ljud | pandans, mörkare |

| Styckning | Antal | Chans |
|---|---|---|
| Orangutan Meat | 1–3 | – |
| Orangutan Hair | 1–3 | 90 % (yttre) |
| Raw Hide | 1 | 30 % (yttre) |
| Sinew | 1 | 40 % |

- 2 **Orangutan Hair** → String.

## Monitor Lizard (varan)

| | |
|---|---|
| Var | jungle, sparse jungle, mangrove swamp (4, 1–2) |
| Liv / fart | 18 / 0,24, skada 3, steg 1; ungar (20 %) |
| Utseende | egen modell (vattenvaran): lång låg kropp på utåtställda ben med krökta klor, lång hals, smalt huvud med lång nos och näsborrar nära spetsen, rosa kluven tunga som slickar ut, lång sidoplattad svans. Mörkt olivgrå med band av ljusgula ögonfläckar, krämfärgade läppar och strupe med mörka streck, ljusgul buk. |
| Beteende | **asätare**: känner lukten av ett kadaver inom 24 block, går dit och **äter upp det på 10 s** (ett Bone Pile blir kvar). Ett byte man lämnar liggande kan alltså gå förlorat. Lämnar människor ifred tills de skadar den; då **biter den med gift** (Poison I och Hunger i 8 s). **Simmar** bra. |
| Ljud | silverfiskens, mycket mörkare; väser när den biter |

| Styckning | Antal | Chans |
|---|---|---|
| Monitor Meat | 1–2 | – |
| Monitor Skin | 1 | 80 % (yttre) |
| Venom Gland | 1 | 50 % |

- 2 **Monitor Skin** → Leather. **Venom Gland** som från huggormen (giftpilar, Snakebite Poultice, Poison Darts).

## Poison Dart Frog (pilgiftsgroda)

| | |
|---|---|
| Var | jungle, bamboo jungle (6, 2–4) |
| Liv / fart | 3 / 0,2; inga ungar |
| Utseende | sitter upp på raka framben med bakbenen vikta, puckelrygg, brett huvud med stora svarta utstående ögon, tår med runda fästdynor. Två sorter efter UUID: **blå** (azurblå med svarta prickar) och **gul** (gul med svarta band över ögonen, mitt på ryggen och baktill). Halsen pulserar; när den hoppar sparkar bakbenen ut. |
| Beteende | gömmer sig inte: färgerna varnar. **Huden är giftig:** den som slår grodan med handen eller ett handvapen blir **förgiftad** (Poison I, 5 s); pilar och dart går bra. Hoppar undan när den blir skrämd. För liten för kadaver. |
| Ljud | grodans, mycket ljusare |

| Tappar | Antal |
|---|---|
| Frog Poison | 1 |

### Blåsrör och pilar

- **Blowpipe**: 3 Bamboo i en rad. Högerklick blåser iväg en pil från förrådet (Poison Dart först, annars Dart). Tyst,
  snabb (0,75 s mellan skotten), svag (1 skada) och kort räckvidd. 160 i hållbarhet.
- **Dart**: Bamboo + Feather → 4.
- **Poison Dart**: 4 Dart + Frog Poison → 4, eller 4 Dart + Venom Gland → 4. Förgiftar det den träffar (Poison II, 10 s).
- En pil kan plockas upp igen som vanlig Dart (giftet är förbrukat).

## Saltwater Crocodile (saltvattenskrokodil)

| | |
|---|---|
| Var | mangrove swamp (4, 1–2) |
| Liv / fart | 40 / 0,18 på land, snabb i vatten, rustning 6, skada 6, knuffmotstånd 0,8, steg 1, inga ungar |
| Utseende | egen modell, ungefär 5 block lång och låg: bred kropp med rader av upphöjda ryggsköldar, brett skalltak med ögonknölar, lång tung nos med näsknopp, ojämna tänder synliga ovanför och under den stängda munnen, underkäke som gapar, utåtställda ben med simhud och klor, lång svans med dubbel kam av sköldar som blir enkel. Mörkt oliv över med svarta band på svansen, ljusare fläckiga sidor, krämvit buk. |
| Beteende | **ligger stilla i vattnet** med bara ögon och näsborrar över ytan (lurar). Kommer något inom **5 block** (dricker vid kanten, vadar, simmar) **gör den ett utfall** och **griper det** i käftarna (inom 1,9 block). Sedan **drar den ner bytet under vattnet och rullar** (dödsrullen): 3 skada i sekunden, under vatten. **Någon annan slår loss den med ett slag**; offret sliter sig loss efter **6 egna slag**; den släpper efter 20 s. På land är den långsam och går tillbaka till vatten (2 block djupt) inom 16 block. Andas under vatten. Byten: alla djur som inte är rovdjur (bredd under 2), och människor. |
| Ljud | ravagerns, mycket mörkare; tyst när den lurar; plask vid utfallet |

| Styckning | Antal | Chans |
|---|---|---|
| Crocodile Meat | 3–5 | – |
| Raw Crocodile Hide | 1 | alltid (yttre) |
| Sinew | 1–2 | 50 % |
| Bone | 1–2 | 60 % |

- Tjock hud: kräver Tusk Dagger eller bättre.
- **Raw Crocodile Hide** + kniv + fett → **Crocodile Hide**: hårt fjälligt läder.
- **Crocodile Hide-rustning** sys på Leatherworking Bench med nål och Sinew (låses upp av att bära Crocodile Hide):
  Helmet (2 hud, 1 Sinew), Cuirass (4, 3), Leggings (3, 2), Boots (2, 1). Skydd 2/5/4/2, seghet 1, knuffmotstånd 0,05 per
  del, hållbarhet som ett material på 16. Ingen värme. Lagas med Crocodile Hide.

## Mudskipper (slamkrypare)

| | |
|---|---|
| Var | mangrove swamp (8, 2–5) |
| Liv / fart | 3 / 0,18; inga ungar |
| Utseende | står uppstöttad på bröstfenorna: trubbigt huvud med bred mun, guldringade ögon tätt ihop uppe på huvudet, en hög rundad ryggfena med blå kant och vita prickar och en lång andra ryggfena, rund stjärtfena. Olivbrun med mörka sadlar och ljusblå prickar, ljus buk. |
| Beteende | en liten fisk med utstående ögon som **går på leran** på fenorna och andas både i och ur vatten. Skrämd **hoppar den undan** i långa kast över leran. För liten för kadaver. |
| Ljud | torskens; små klafs på leran |

| Tappar | Antal |
|---|---|
| Mudskipper | 1 |

- **Mudskipper**: äts rå som en liten fisk (1 mat) eller steks till Cooked Mudskipper (3). Är **bete** (`mechanical:snare_bait`) i snaror.

## Fiddler Crab (vinkarkrabba)

| | |
|---|---|
| Var | mangrove swamp (8, 2–4) |
| Liv / fart | 4 / 0,2; inga ungar |
| Utseende | platt fyrkantigt turkost skal, bredare framtill, med ljusa fläckar; ögon på långa stjälkar; fyra par ledade gångben. **Hanen** (efter UUID) har en jättelik orange klo som den håller framför sig: en tung handflata med ett fast finger under och ett rörligt över, vita spetsar. **Honan** har två små klor. |
| Beteende | en liten lerkrabba; hanen vinkar med den stora klon och smäller med fingret. Bor vid sitt hål i leran: kommer en spelare inom **5 block** (smygande 2) eller skadas den **dyker den ner i hålet** (bara ögonen syns) och stannar nere 10 s, så länge någon står nära. Nere i hålet **kan den inte skadas**. Hål finns bara i lera (Mud, mangroverötter i lera, Clay): ute på annan mark kan man fånga den. För liten för kadaver. |

| Tappar | Antal |
|---|---|
| Raw Crab Meat | 1 |

- **Raw Crab Meat** steks till Cooked Crab Meat (4 mat). Rå ger risk för Hunger.

## Proboscis Monkey (näsapa)

| | |
|---|---|
| Var | mangrove swamp (5, 2–4) |
| Liv / fart | 14 / 0,27, steg 1; ungar (30 %) |
| Utseende | egen modell: en markattsapa på alla fyra med stor rund kalasmage, rödbrun kalott och rygg, orange sidor, gråvita armar och ben, vit rumpa och lång tjock vit svans som hänger ner. Litet rosa ansikte; **hanarna har en stor hängande näsa**, honor och ungar en liten uppnäsa. |
| Beteende | lever i flock och är **aldrig tyst**: tutar och grymtar hela dagen. Ser den en jägare komma (en spelare inom 14 block som inte smyger, eller närmare än 5) ger den **larmtutet**, och **alla vilda djur inom 20 block blir rädda och springer** (flockfåglar flyger upp). En dålig granne för den som smyger på byte. Larmar högst var 15:e sekund. **Simmar** över kanalerna. |
| Ljud | getens skrik och pandans grymtningar, högt |

| Styckning | Antal | Chans |
|---|---|---|
| Monkey Meat | 1–2 | – |
| Raw Hide | 1 | 30 % (yttre) |

## Avancemang

- Vildmark: **Stripes in the Bamboo** (utmaning: Raw Tiger Pelt), **Tiger Skin Rug** (Tiger Rug), **Tiger Mantle** (Tiger Pelt
  Mantle), **Coils** (Python Skin), **Thief of the Canopy** (Orangutan Hair), **Death Roll** (utmaning: Raw Crocodile Hide),
  **Scale Armour** (en Crocodile Hide-rustningsdel), **Silent Darts** (Blowpipe), **Painted Poison** (Poison Dart),
  **Mud Dwellers** (Mudskipper eller Raw Crab Meat).
- Mat: **Mangrove Fare** (stekt kött från de nya djuren).
- Jaktfliken: tiger, krokodil under Predators; python, varan, orangutang, näsapa under More Game/Small Game (se HUNT_GROUPS).

## Kända begränsningar

- Orangutangens byte syns inte i handen (inget hand-lager på modellen).
- Tigerns osynlighet använder spelets vanliga osynlighetsflagga (som en dryck): den syns inte heller för andra djur.
- Pythonen klättrar inte själv upp i träd; den spawnar där och stannar.
