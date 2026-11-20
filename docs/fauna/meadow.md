# Ängens djur (meadow)

Fem djur för ängar, slätter, skogsbryn, hedar och fuktmark: **Wild Cat**, **Mole**, **Field Vole**, **Adder** och **Grass Snake**. Alla är vilda djur (`WildAnimal`): de lämnar en kadaver som styckas med kniv (`Butchery`), räds spelare enligt `AnimalFear` och fyller på sig själva genom `WildlifeRestock` (de står i biomernas djurlistor). Alla fem är så små att en **Snare** kan fånga dem.

Kod: `com.mechanical.wildlife.meadow` (`MeadowFauna` registrerar allt och lägger till styckningsutbytet), klient: `com.mechanical.client.wildlife.meadow`. Data: `scripts/gen_fauna_meadow.py`, texturer: `scripts/gen_fauna_meadow_textures.py`.

Ormarna är inga däggdjur men lever som de andra småvilten här (jagas, snaras, styckas, sover och räds som de), därför samma basklass; de har inga ungar och går alltid ensamma.

## Wild Cat

**Var:** Forest, Flower Forest, Birch Forest, Meadow, Deciduous Forest och Hazel Meadow (vikt 2, 1–2 st: en ensam katt eller en hona med kattunge).

**Beteende:** Grå­brun tabby med mörk ryggstrimma, brutna tvärränder på sidorna, fyra linjer över pannan, krämvit strupe och den tjocka, trubbiga svansen med tre svarta ringar och svart spets. **Skygg** (märker spelare på 1,6 gånger vanligt avstånd och smyger undan) och **nattaktiv**: sover hoprullad om dagen och jagar i skymning och mörker. Byten: Field Vole, Mole (när den är uppe), Squirrel, Rabbit, Pheasant, Chicken och Rock Dove. Den smyger lågt och långsamt de sista tio blocken och gör ett språng när den är nära. Hotas den i ett hörn fräser den. Kattungar jagar inte.

**Värden:** 10 hälsa, fart 0,3, 3 skada. Kadaver: lätt (bärs i full fart).

| Styckning | Vad | Chans | Antal |
|---|---|---|---|
| 1:a omgången | Raw Wildcat Pelt | 90 % | 1 |
| 2:a omgången | Raw Wildcat Meat | – | 1–2 |
| 2:a omgången | Sinew | 30 % | 1 |

**Produkter:** Raw Wildcat Pelt + kniv i rutnätet (kniven blir kvar) → **Wildcat Pelt**. 2 Wildcat Pelt + Sinew → **Wildcat Fur Cap** (hjälm, skydd 1, lagas med Wildcat Pelt). Raw Wildcat Meat steks på Campfire (20 s) eller i smoker → Roast Wildcat Meat (hunger 5, mättnad 0,7).

*Tämjning:* inte gjord. Kroken finns: `WildCat.isPrey` är vad den jagar; en tam katt skulle behålla den och släppa rädslan. Redan nu jagar vanliga (även tama) Cats sorkar, och sorkar flyr katter, så en katt på gården skyddar säden.

## Mole

**Var:** Plains, Sunflower Plains, Meadow, Hazel Meadow, Forest, Flower Forest och Deciduous Forest (vikt 4, 1 st).

**Beteende:** Sammetssvart med grå lyster, rosa spetsig nos och de stora rosa grävhänderna utåtvända. Den **lever under jord**: nedgrävd är den osynlig och kan varken träffas av slag eller pilar. Den tunnlar långsamt fram och trycker då och då upp en **Molehill** (ungefär varannan gång var tionde sekund, aldrig närmare än 2 block från en annan):

- på **åkermark** rycks grödan upp (den hoppar av som föremål) och jorden blir Dirt,
- på **stigar** (Dirt Path, upptrampade stigar) bryts stigen upp till Dirt,
- på gräs och jord blir det bara en hög.

Mullvadshögar går att gå över och sjunker ihop av sig själva efter ungefär en halvtimme. Mullvaden kommer upp ibland för att leta mat, mest nattetid, och stannar ute 10–30 s. Märker den en spelare (som inte smyger) inom 6 block, eller blir skrämd, gräver den ner sig igen (1,5 s med jord som yr). Kommer den ut på sten, sand eller vatten måste den upp.

**Jakt:** smyg dig fram när den är uppe, **gräv upp en färsk mullvadshög** (en nedgrävd mullvad inom 6 block skräms upp och flyr), eller sätt en **Snare** över gångarna – den fångar mullvaden även under jord. Följer mobGriefing.

**Värden:** 4 hälsa, fart 0,16 (under jord 70 % av det). Kadaver: lätt.

| Styckning | Vad | Chans | Antal |
|---|---|---|---|
| 1:a omgången | Raw Mole Pelt | 85 % | 1 |
| 2:a omgången | Raw Mole | – | 1 |

**Produkter:** Raw Mole Pelt + kniv → **Moleskin**, den fina sammetspälsen. 3 Moleskin + Twine (` T ` / `M M` / ` M `) → en **Bundle**. Raw Mole → Roast Mole (hunger 3, mättnad 0,6). Molehill ger ibland en Dirt.

## Field Vole

**Var:** Plains, Sunflower Plains, Meadow, Hazel Meadow och Savanna (vikt 8, 2–4 st).

**Beteende:** Liten, trubbnosig och korthårig, gråbrun med grå buk, öronen gömda i pälsen. Lever i lösa kolonier, är vaken i skymning och natt och vilar om dagen. **Ett skadedjur på åkrar och förråd**, men bara nattetid och bara nära fält:

- **Grödor:** den letar upp en gröda inom 8 block och gnager på den: en växande gröda backar 2 steg, en nysådd planta (steg 0) äts upp helt. Smulor och ätljud syns och hörs.
- **Förråd:** finns åkermark inom 12 block tar den sig in i en **Chest eller Barrel** inom 8 block och äter 1–2 korn ur den (taggen `mechanical:vole_food`: säd, kärvar, Bread, betfrön). Gnagljud och smulor vid kistan. Högst en räd per sork var andra minut. **Storage Jar, Root Cellar och Bark Basket** håller den ute.
- **Förökning:** var tredje måltid får den 1–2 ungar, så länge det är färre än 6 sorkar inom 16 block. Sorkar som får äta i fred blir fler.

Sorken flyr Wild Cat, Fox, Cat och Ocelot (8 block), och de jagar den (rävar och katter fick ett mål för sorkar och uppkomna mullvadar). Fångas lätt i Snare nära fälten. Följer mobGriefing.

**Värden:** 3 hälsa, fart 0,28. Kadaver: ett litet (lätt).

| Styckning | Vad | Chans | Antal |
|---|---|---|---|
| 2:a omgången | Raw Vole | – | 1 |

Ingen päls värd att ta. **Produkter:** Raw Vole → Roast Vole (hunger 2, mättnad 0,5).

## Adder

**Var:** Pine Heath (vikt 6, 1–2 st), och glesare på Meadow, Windswept Hills, Windswept Gravelly Hills, Plains och Hazel Meadow (vikt 3, 1 st). Kan födas på gräs eller bar sten i dagsljus (soliga klippor).

**Beteende:** Grov kropp, hanen grå och honan brun (bestäms av UUID, kadavern behåller utseendet), båda med svart sicksack längs ryggen, ett mörkt V på huvudet och kopparröda ögon. Den **flyr inte**: den litar på kamouflaget, solar sig hoprullad på klara dagar och ligger lågt i gräset. **Den som går rakt fram till den (inom 1,6 block) utan att smyga blir huggen**: 2 skada och **Poison I i 6 s**. En väsning varnar när någon är inom 4 block. **Snakeskin Boots** stoppar bettet (stövlarna tar 1 slitage i stället). Efter ett hugg, eller när den skadas, glider den undan. Sover hoprullad nattetid.

**Värden:** 6 hälsa, fart 0,2, 2 skada. Kadaver: lätt.

| Styckning | Vad | Chans | Antal |
|---|---|---|---|
| 1:a omgången | Snakeskin | 85 % | 1 |
| 2:a omgången | Raw Snake Meat | – | 1 |
| 2:a omgången | Venom Gland | 75 % | 1 |

**Produkter:**
- 8 Arrow runt en Venom Gland → 8 **Arrow of Poison**.
- Venom Gland + Lard + Plant Fibre → 2 **Snakebite Poultice**: äts på 1 s och tar bort Poison (huggormsbett, flugsvamp).
- Leather Boots + 2 Snakeskin + Sinew → **Snakeskin Boots** (skydd 1, lagas med Snakeskin; huggormar biter inte igenom).
- Raw Snake Meat → Roast Snake Meat (hunger 4, mättnad 0,6).

## Grass Snake

**Var:** Swamp, Willow Fen och River (vikt 5, 1 st), och glesare i Meadow, Hazel Meadow och Flower Forest (vikt 2, 1 st).

**Beteende:** Lång och smal, olivgrå-grön med svarta tvärband på sidorna, en gul och svart halskrage och schackrutig buk. Ofarlig. Håller sig vid vatten, simmar gärna och glider undan från spelare. **Spelar död:** skadad men inte dödad rullar den ofta (65 %) över på rygg, slapp med tungan ute, i 6–11 s, och flyr sedan. Solar sig också hoprullad.

**Värden:** 8 hälsa, fart 0,24. Kadaver: lätt.

| Styckning | Vad | Chans | Antal |
|---|---|---|---|
| 1:a omgången | Snakeskin | 85 % | 1 |
| 2:a omgången | Raw Snake Meat | – | 1–2 |

**Produkter:** som huggormens, utom giftkörteln.

## Gemensamt

- **Kött:** alla råa sorter förskäms på 1 dag, stekta på 3, och ruttnar till Rotten Flesh. Det råa köttet kan rökas på Smoking Rack och torkas på Drying Rack (`smokable_meat`). Kitteln tar allt som är mat.
- **Skinn:** de små skinnen (vildkatt, mullvad) behöver ingen torkställning: de skrapas med kniv i rutnätet, som Raw Hide. Ormskinn är färdigt direkt.
- **Hängning:** alla kan hängas på Hanging Rack (ormarna hänger raka i stjärten).
- **Ljud:** vanilla med ändrad tonhöjd (katt, kanin, silverfisk; ormarnas väsning är kattens).

## Framsteg

Wilderness-fliken: *Wildcat*, *Tabby Cap*, *Velvet* (Moleskin), *Pest Control* (Raw Vole), *Shed Skin* (Snakeskin), *Watch Your Step* (Snakeskin Boots), *Fangs* (Venom Gland), *Venom Tips* (Arrow of Poison), *Antidote* (Snakebite Poultice). Food-fliken: *Small Game* (stekt vildkatt, mullvad, sork eller orm).
