# Istidens jättar

Fyra stora djur från den kalla stäppen: **Woolly Mammoth**, **Woolly Rhinoceros**, **Steppe Bison** och **Musk Ox**. Alla
lämnar en kadaver som styckas med kniv (se jakten i `docs/ages/01-stone-age.md`), och alla ger eget kött, en egen stor
hud och egna delar.

Kod: `com.mechanical.wildlife.giants` (`GiantsFauna` registrerar allt, ett djur per klass, `PeggedHideBlock`,
`IvoryFigurineBlock`), klient `com.mechanical.client.wildlife.giants` (modeller, `GiantRenderer`, `GiantsClient`).
Data: `scripts/gen_fauna_giants.py` (körs efter `gen_roots.py`, före `gen_block_items.py`). Föremålens
texturer: `scripts/gen_fauna_giants_textures.py`. Djurens modeller och skinn (vuxna, kalvar, flådda kadaver) görs ur
en tabell per djur av `scripts/gen_pred_models.py` (`GiantsGeometry.java` skrivs av skriptet; mammuten byggs i halv storlek
med två texlar per pixel).

## Stora hudar: spänns ut på marken

Jättarnas råa hudar är för stora för Drying Rack. **Raw Mammoth/Rhino/Bison/Musk Ox Hide** placeras i stället direkt på
marken (högerklick), där huden spikas ut med köttsidan upp och fyra pinnar i hörnen (*Pegged … Hide*). Den torkar i
solen: dagsljus, fri himmel och inget regn. Natt och regn pausar bara. Klar hud byter färg från rött kött till ljus
gul-brun råhud; tom hand tar av den färdiga huden. Bryts den innan den är klar får man tillbaka den råa huden.

| Hud | Tid i sol (ungefär) | Färdig hud | Läder (hud + kniv) |
|---|---|---|---|
| Raw Mammoth Hide | ~13 min | Mammoth Hide | 4 Leather |
| Raw Rhino Hide | ~9 min | Rhino Hide | 3 Leather |
| Raw Bison Hide | ~8 min | Bison Hide | 3 Leather |
| Raw Musk Ox Hide | ~7 min | Musk Ox Hide | 2 Leather |

## Woolly Mammoth

**Var:** Snowy Plains, Snowy Taiga, Grove och Ice Spikes (där Mammoth Bone Huts står). Sällsynt: vikt 1, flockar om 2–4.
30 % av flockens övriga djur är kalvar.

**Beteende:** Flockens första djur är **matriarken** (herd leader): 20 % större (scale 1,2), 220 hp och en vit bossbar.
De andra följer henne. Mammutar flyr inte för spelare. Matriarken trumpetar en varning (huvud och snabel höjs) när en
spelare kommer inom ~14 block (smygande ~8), och anfaller om man fortsätter närmare än 8 (smygande 4). Skadas något djur
i flocken vänder sig alla vuxna mot angriparen. Anfallet är ett **svep med snabel och betar** som träffar allt i en bred båge
framför (~160°, räckvidd 4,5 block × storlek): full skada på målet, 70 % på resten, och kastar långt. Som Giant Deer
blöder och viker den inte förrän den är nere på en femtedel av hälsan; då flyr den.

| Stat | Värde |
|---|---|
| Hälsa | 150 (matriarken 220) |
| Fart | 0,24 |
| Skada | 12 (svep), knockback 2,5 |
| Rustning | 4, knockback-motstånd 0,95 |
| Storlek | 2,4 × 3,4 block (matriarken 2,88 × 4,08). **Huge**: kan inte hängas eller bäras, styckas på platsen |

**Styckning** (flintkniv; bättre knivar ger mer):

| Pass | Del | Chans | Antal |
|---|---|---|---|
| 1 | Raw Mammoth Hide | 100 % | 1 |
| 1 | Mammoth Wool | 90 % | 3–5 |
| 2 | Raw Mammoth Meat | – | 8–12 |
| 2 | Mammoth Ivory | 100 % | 2 (alltid) |
| 2 | Mammoth Fat | 90 % | 2–4 |
| 2 | Sinew | 80 % | 2–3 |
| 2 | Bone | 90 % | 3–6 |

**Produkter:**
- **Raw/Cooked Mammoth Meat** (2 / 4 hunger): Campfire 20 s eller smoker; förskäms (rå 1 dag, tillagad 3), kan rökas och torkas.
- **Mammoth Ivory** (stack 16): **Ivory Knife** (Ivory över Cordage över Leather) – en flåkniv i Tusk Daggers klass
  (klarar björn), lite snabbare och med större utbyte (×1,2, hud ×1,6), 200 hållbarhet. **Ivory Figurine** (Ivory + kniv):
  en liten snidad mammut att ställa på ett bord.
- **Mammoth Wool:** 1 → 2 Twine; 4 → Brown Wool.
- **Mammoth Fat:** smälts till **3 Tallow** på Campfire (ljus, Fire Arrow, färg).
- **Mammoth Hide:** **Mammoth Rug** (Hide + Mammoth Wool) eller 4 Leather med kniv.
- Mammoth Bone Huts kan nu ha Mammoth Ivory och Mammoth Wool i sin kruka.

## Woolly Rhinoceros

**Var:** Snowy Plains, Windswept Hills, Windswept Gravelly Hills, Snowy Taiga, Snowy Slopes. Vikt 2, ensam eller par (ko och
kalv; 40 % kalvchans). Spawnar också på snö, sten och grus.

**Beteende:** Betar ensam (följer ingen flock). Ser dåligt: märker en spelare först på halva det vanliga avståndet. Skadas den
**anfaller** den: skrapar marken med huvudet nere i en sekund (dammoln), sedan stormar den rakt fram mot där angriparen stod
(fart ×2,6, upp till 1,5 s). Den kan inte svänga: kliv åt sidan så rusar den förbi. Träffen gör 9 skada och slänger målet
upp och bakåt. Springer den in i en vägg eller ett träd blir den **omtöcknad** i 2,5 s. Mellan anfallen stångas den på nära
håll. Ger upp och flyr när den blöder (halv hälsa). En skadad kalv skickar modern efter angriparen.

| Stat | Värde |
|---|---|
| Hälsa | 60 |
| Fart | 0,23 (anfall ×2,6) |
| Skada | 7 (stångning), 9 (anfall) |
| Rustning | 6, knockback-motstånd 0,8 |
| Storlek | 1,5 × 1,6 block (large: dras eller läggs på travois, kan hängas) |

| Pass | Del | Chans | Antal |
|---|---|---|---|
| 1 | Raw Rhino Hide | 100 % | 1 |
| 2 | Raw Rhino Meat | – | 4–6 |
| 2 | Rhino Horn | 100 % | 1 |
| 2 | Sinew | 60 % | 1–2 |
| 2 | Bone | 60 % | 1–3 |

**Produkter:**
- **Raw/Cooked Rhino Meat** (1 / 3 hunger), som övrigt kött.
- **Rhino Horn:** **Rhino Horn Club** (Horn över Cordage över Tool Rod) – tung och långsam klubba, 240 hållbarhet; eller
  2 Bone Needle med kniv.
- **Rhino Hide:** **Shield** (Planks–Hide–Planks / 3 Planks / Planks), eller 3 Leather med kniv.

## Steppe Bison

**Var:** Plains, Sunflower Plains, Savanna, Savanna Plateau, Snowy Plains, Meadow, Hazel Meadow, Pine Heath. Vikt 6, hjordar
om 4–8, 30 % kalvar.

**Beteende:** Som uroxen: **tjurar** (30 %, högre puckel och grova horn) vänder och anfaller när de skadas och kommer en skadad
kalv till hjälp; kor och kalvar flyr. En skrämd hjord **stampedar**: bisonar som springer för livet trampar ner det som står
i vägen (4 skada och en knuff, inte på varandra).

| Stat | Värde |
|---|---|
| Hälsa | 45 |
| Fart | 0,25 |
| Skada | 7 (tjurar), 4 (trampning) |
| Knockback-motstånd | 0,6 |
| Storlek | 1,4 × 1,8 block (large) |

| Pass | Del | Chans | Antal |
|---|---|---|---|
| 1 | Raw Bison Hide | 100 % | 1 |
| 2 | Raw Bison Meat | – | 3–6 |
| 2 | Bison Tongue | 80 % | 1 |
| 2 | Horn | 50 % | 1–2 |
| 2 | Sinew | 60 % | 1–2 |

**Produkter:**
- **Raw/Cooked Bison Meat** (1 / 3 hunger, hög mättnad 0,9).
- **Bison Tongue → Roast Bison Tongue** (4 hunger, mättnad 1,1): jägarnas finaste bit.
- **Bison Hide:** **Bison Rug** (Hide + Horn) eller 3 Leather med kniv. Horn som från uroxen (Bone Needle).

## Musk Ox

**Var:** Snowy Plains, Snowy Slopes, Grove, Ice Spikes, Frozen Peaks, Snowy Taiga. Vikt 5, hjordar om 4–8, 30 % kalvar.
Spawnar också på snö och sten.

**Beteende:** Flyr inte. När något hotar – en spelare inom 12 block (smygande 6) som syns, en Grey Wolf eller Cave Lion inom
18, eller att en i hjorden skadas – sluter hela hjorden (inom 20 block) upp i en **ring**: de vuxna ställer sig jämnt utspridda
runt kalvarna (eller hjordens mitt), vända utåt med huvudet nere och hornen fram; kalvarna trängs i mitten. Oxen närmast
hotet gör ett kort utfall när det kommer inom ~4 block från ringen, och backar sedan tillbaka till sin plats (2,5 s mellan
utfallen). Ringen löses upp när hotet är borta (över 28 block bort, eller efter 12 s utan hot). En ox som blöder (halv
hälsa) bryter sig ur och flyr.

| Stat | Värde |
|---|---|
| Hälsa | 36 |
| Fart | 0,23 |
| Skada | 6 (utfall), knockback |
| Rustning | 3, knockback-motstånd 0,7 |
| Storlek | 1,2 × 1,4 block (large) |

| Pass | Del | Chans | Antal |
|---|---|---|---|
| 1 | Qiviut | 90 % | 2–4 |
| 1 | Raw Musk Ox Hide | 100 % | 1 |
| 2 | Raw Musk Ox Meat | – | 2–4 |
| 2 | Horn | 60 % | 1–2 |
| 2 | Sinew | 50 % | 1–2 |

**Produkter:**
- **Raw/Cooked Musk Ox Meat** (1 / 3 hunger).
- **Qiviut** (fin underull): 1 → 2 Wool Thread (syr läderkläder på Armor Station); 4 → Light Gray Wool.
- **Musk Ox Hide:** **Musk Ox Rug** (Hide + Qiviut) eller 2 Leather med kniv.

## Achievements

Wilderness-fliken (under *Butcher*): Hunter's Share (Bison Tongue), Pegged Out (en torkad stor hud), Hearthside Hide (en
jätte-rya), Ring of Horns (Qiviut), Charging Horn (Rhino Horn) → Hide Shield, Horn Club, Mammoth Hunter (Mammoth Ivory,
utmaning) → Ivory Edge, Ivory Carver, Fat of the Land. Food-fliken (under *Cooked Meal*): Ice Age Feast → Delicacy.

## Ljud

Mammut: Ravager-ljud nedstämda (trumpeten: Ravager Roar uppstämd). Noshörning: Hoglin-ljud nedstämda, Ravager-steg i
anfallet. Bison: kons "moody"-ljud djupt nedstämda. Myskoxe: kons ljud och Hoglin-fnysningar när ringen sluts.
