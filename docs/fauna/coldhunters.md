# Kalla jägare (coldhunters)

Fem djur för snölandet, taigan och skogarna: **Wolverine**, **Lynx**, **Ermine**, **Lemming** och **Pine Marten**.
Kod: `wildlife/coldhunters/` (ColdhuntersFauna, ett djur per klass, PeltCuring), klient: `client/wildlife/coldhunters/`.
Data: `scripts/gen_fauna_coldhunters.py`, texturer: `scripts/gen_fauna_coldhunters_textures.py`.

Alla lämnar ett Carcass som styckas med kniv (Butchery) och kan hängas på en Hanging Rack. Alla spawnar på rävens mark
(gräs, snö, podzol, coarse dirt) i dagsljus. Mängder nedan gäller flintkniv; på Hanging Rack faller allt i första omgången
utan slumpslag och köttet blir Haunch/Ribs/Loin.

## Pälsar på torkställningen

Råa pälsar (Raw Wolverine/Lynx/Ermine/Marten Pelt) högerklickas på en vanlig **Drying Rack**-plats och torkar som en
Scraped Hide (~3 min, väder spelar ingen roll). Ingen av dem behöver den höga ställningen – de är mindre än en vargfäll.
Tar man ner den torra pälsen med tom hand får man den färdiga pälsen (inte Leather); slås ställningen sönder får man
pälsen tillbaka (rå om den var våt). Vilken päls som hänger var sparas per värld (PeltCuring); ställningen ritar den som
en uppspänd hud. Lemming Pelt är för liten att torka: 4 st + Twine → Raw Hide (som ekorrskinnet).

## Wolverine (järv)

- **Var:** snowy plains, snowy taiga, grove, snowy slopes. Vikt 2, ensam (1).
- **Beteende:** ensam och låg. Låter en komma närmare än andra djur (skygghet 0,45) men viker inte: kommer en spelare
  närmare än **4 block** utan att smyga anfaller den, och den som kommer inom **8 block** medan den äter blir anfallen
  oavsett. Morrar först. Sover mitt på dagen (4000–8000), annars vaken.
  **Tjuv:** dag som natt och utan rädsla för eld letar den upp ett obevakat kadaver (ingen spelare inom 10 block, inte
  buret eller släpat) inom **32 block** – på marken, vid en snara eller hängt på en Hanging Rack – och äter upp det på 8 s:
  på marken blir en Bone Pile kvar, från ställningen faller 1–2 Bone. Den plundrar också **betade snaror** inom 12 block:
  äter betet och utlöser snaran (den är för stor för att fastna).
- **Stats:** 22 liv, 5 skada, fart 0,27, knockback-motstånd 0,5, blöder under 35 %.
- **Ljud:** arg vargmorrning (ljus), rävskrik i låg ton.

| Omgång | Vad | Chans | Antal |
|---|---|---|---|
| 1 | Raw Wolverine Pelt | 100 % | 1 |
| 2 | Raw Wolverine | – | 1–3 |
| 2 | Wolverine Claw | 60 % | 1–2 |
| 2 | Sinew | 50 % | 1 |
| 2 | Bone | 50 % | 1–2 |

**Produkter:**
- Raw Wolverine (hunger 2) → Campfire 20 s / rökugn → **Cooked Wolverine** (hunger 6, mättnad 0,6).
- Raw Wolverine Pelt → torkställning → **Wolverine Fur**.
- **Wolverine-Trimmed Hood:** Fur Hood + 2 Wolverine Fur + Sinew (crafting). Hjälm, rustning 1, isolerar 25 % mot kyla och
  är **frostsäker** (i `minecraft:freeze_immune_wearables`: ingen frysskada, varken från Powder Snow eller iskyla).
- **Claw Flesher:** 2 Wolverine Claw + Stick + Sinew. En flåkniv på flintnivå (1,1× fart, 1,4× päls/hud i första
  omgången, 90 hållbarhet); fungerar i alla knivrecept.

## Lynx (lodjur)

- **Var:** taiga, old growth spruce/pine taiga, snowy taiga, dark forest, Pine Heath. Vikt 2, 1–2 (den andra är oftast
  en unge, 60 %).
- **Beteende:** ett nattdjur (Predator): sover på dagen och håller sig undan folk, rädd för eld och facklor (utom vid
  blodmåne eller svält), söker sig till lukten av kadaver. På natten jagar den rådjur, harar, kaniner, ekorrar, ripor,
  fasaner, lämlar och unga renar/kronhjortar. **En ensam spelare** (ingen annan spelare inom 24 block) inom 16 block (6 om
  han smyger) är också byte. Smyger hukande, står still när bytet tittar åt dess håll, och kastar sig fram från 2–6 block
  (6 skada), sedan klor. Under 40 % liv drar den sig tillbaka. Ungen följer modern.
- **Stats:** 20 liv, 4 skada, fart 0,3, ljudlösa steg.
- **Ljud:** katt (låg ton): parningsläte på natten, fräsning vid språnget.

| Omgång | Vad | Chans | Antal |
|---|---|---|---|
| 1 | Raw Lynx Pelt | 100 % | 1 |
| 2 | Raw Lynx | – | 2–3 |
| 2 | Sinew | 50 % | 1–2 |
| 2 | Bone | 60 % | 1–2 |

**Produkter:** Raw Lynx (hunger 2) → **Cooked Lynx** (6 / 0,7). Raw Lynx Pelt → **Lynx Pelt** (fläckig).
**Lynx Rug:** 2 Lynx Pelt + Bone – platt dekoration som Bear/Wolf Rug.

## Ermine (hermelin)

- **Var:** snölandet (snowy plains, snowy taiga, grove, snowy slopes) och taigan (taiga, old growth spruce/pine, Pine
  Heath). Vikt 4, 1–2.
- **Beteende:** liten och snabb, studsar fram i bågar, mycket skygg. **Vit** om den föds i ett snöbiom (samma regel som
  ripan), annars brun ovan och gräddvit under; svart svanstipp alltid. Jagar **lämlar** och äter upp kroppen (3 s).
  Fångas i snaror. Ingen fallskada.
- **Stats:** 6 liv, 2 skada, fart 0,33.

| Omgång | Vad | Chans | Antal |
|---|---|---|---|
| 1 | Raw Ermine Pelt | 80 % | 1 |
| 2 | Raw Ermine | – | 1 |

**Produkter:** Raw Ermine (1) → **Cooked Ermine** (3 / 0,5). Raw Ermine Pelt → **Ermine Fur** (fin vit lyxpäls; vita och
bruna hermeliner ger samma skinn). **Ermine Mantle:** 6 Ermine Fur + Leather + Sinew (`E E` / `ELE` / `ESE`):
bärs i Cloak-slotten över rustningen, rustning 2, isolerar 25 %, förtrollbarhet 22 – en hövdingamantel av vit hermelin med svarta svanstippar.

## Lemming (fjällämmel)

- **Var:** snowy plains, snowy taiga, grove, snowy slopes, ice spikes. Vikt 8, 3–6.
- **Beteende:** liten och rund, piper, låter en komma nära innan den springer. Sitter upp och gnager när den betar.
  **Lämmelår:** var 8:e dag är det boom i två dagar (dag 7–8 i cykeln); då får varje vuxen lämmel ungefär varannan minut
  en unge (30 % var 30:e s) tills det är 10 lämlar inom 12 block. Byte för rävar (vanilla-rävar jagar dem), lodjur och
  hermeliner. Fångas i snaror (smådjur).
- **Stats:** 3 liv, fart 0,28.

| Omgång | Vad | Chans | Antal |
|---|---|---|---|
| 1 | Lemming Pelt | 60 % | 1 |
| 2 | Raw Lemming | – | 1 |

**Produkter:** Raw Lemming (1) → **Cooked Lemming** (2 / 0,4). 4 Lemming Pelt + Twine → Raw Hide.

## Pine Marten (mård)

- **Var:** forest, birch forest, old growth birch, dark forest, Deciduous Forest, taiga, old growth spruce/pine, Pine
  Heath, snowy taiga. Vikt 4, ensam.
- **Beteende:** springer upp i närmaste träd när den skräms (eller ibland bara så) och väntar under kronan, kommer ner
  med huvudet först. Jagar **ekorrar**. **Plundrar fågelbon** (Bird Nest med ägg) inom 16 block: äter ett ägg, lämnar
  ett Eggshell, fåglarna runt boet flyger upp, och boet räknas som rånat precis som när en spelare tar äggen (oroliga
  fåglar, övergivet bo efter tre gånger). Ingen fallskada. Fångas i snaror.
- **Stats:** 10 liv, 3 skada, fart 0,32.

| Omgång | Vad | Chans | Antal |
|---|---|---|---|
| 1 | Raw Marten Pelt | 90 % | 1 |
| 2 | Raw Marten | – | 1 |

**Produkter:** Raw Marten (1) → **Cooked Marten** (3 / 0,6). Raw Marten Pelt → **Marten Fur** (tät och blank).
**Marten Fur Cap:** 5 Marten Fur + Sinew (`MMM` / `M M` / ` S `): hjälm, rustning 1, isolerar 25 %.

## Gemensamt

- Rått kött förskäms på 1 dag, tillagat på 3; allt ruttnar till Rotten Flesh; rått kött kan rökas och torkas.
- De tre plaggen ligger i `#mechanical:fur_insulation` (Temperature: 25 % per del, som Fur-rustning).
- Prestationer: Wilderness – Lemming Year, Winter White, Chieftain's Mantle, Tree Weasel, Marten Fur Cap, Glutton,
  Frost-Proof, Claw Flesher, Night Stalker, Spotted Rug; Food – Taiga Fare.
