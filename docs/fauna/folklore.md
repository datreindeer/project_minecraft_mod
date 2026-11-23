# Folktrons väsen (folklore)

Alla folktroposter i [fauna-expansion-plan.md](../fauna-expansion-plan.md): **Skogsrået (Huldra)**, **Lyktgubbe**,
**Jungle Spirit Mask**, **Vätten (Tomte)**, **Näcken**, **Myrmannen (Bog Body)**, **Bergatroll i skogen**,
**Frostjätte (Rimthurs)**, **Bergtagen** och **Dune Wraith**. Byggt i folktrons stil ([folklore-plan.md](../folklore-plan.md)):
magin syns lite, händer sällan och förklaras aldrig.

Kod: `folklore/` (`Beings` = registreringar, `Spirit` = gemensam grund, `Huldra`, `Wisp` (lyktgubbe och mask), `Tomte`,
`Nacken`, `BogBody`, `FrostGiant`, `DuneWraith`, `TrollStone`, `Bergtagen`, test `BeingsTest`), `worldgen/TrollStoneSleeperFeature`,
ändringar i `wildlife/underground/Troll` (skogstroll) och `Vittra` (gäster), `Thaw.melt` (Frost Heart).
Klient: `client/folklore/BeingsClient`. Data: `scripts/gen_folklore.py`, texturer: `scripts/gen_folklore_textures.py`.

## Gemensamt för andeväsendena

Huldra, lyktgubbe, mask, tomte, Näcken och Dune Wraith är **andar** (`Spirit`):
- Syns bara för den vars **syn** (Faith, +50 under Second Sight, +15 med en Spirit Mask på huvudet) når väsendets tröskel.
  Andra ser inget och kan inte klicka på dem. De finns ändå på servern, och det de gör (stjäla vatten, spela fiol) händer ändå.
- Går inte att skada. Ett slag från en spelare får dem att försvinna i en pust (eller det väsendet själv gör, se nedan).
- Ingen Carcass, inga drops, inga ljud utom sina egna.
- Första gången man ser ett slags väsen: **+5 Faith** och advancement.
- De spawnar som monster (biome modifiers, `MobCategory.MONSTER`) med egna, sällsynta regler.

| Väsen | Syn | Var | När |
|---|---|---|---|
| Lyktgubbe | 10 | Swamp, Dark Forest, Peat Bog, Willow Fen, Reed Delta | Natt, öppen himmel; dubbelt så ofta på hösten |
| Tomte | 20 | Där någon odlar (se nedan) | Flyttar in på natten och stannar |
| Dune Wraith | 20 | Desert | Sandstorm (åska över öknen) |
| Huldra | 25 | Forest, Birch Forest, Dark Forest, Old Growth Birch Forest, Flower Forest, Deciduous Forest | Natt, på skogsjord |
| Jungle Spirit Mask | 25 | Jungle, Bamboo Jungle, Sparse Jungle | Natt, öppen himmel |
| Näcken | 30 | River | Månljus natt (mörkt, inget regn), i vattnet vid forsar (strömmande vatten inom 3 block) |

## Skogsrået (Huldra)

En vacker kvinna i skogsbrynet på natten, med långt ljust hår, mossgrön klänning och **ihålig rygg** som ett ruttet träd.
- Håller sig före den som ser henne (inom 24 block): kommer man närmare än 10 block går hon längre in bland träden.
  Hon lockar en bort från stigen.
- Efter 30 s stannar hon och håller fram sin gåva (en kam i handen). **Högerklicka henne** då: man får en **Huldra Comb**
  och **Luck i 20 min**, +3 Faith, och hon försvinner.
- **Tackar man nej** (går mer än 12 block från henne när hon har räckt fram gåvan) eller slår henne: man blir **Lost** i
  3 min och hon försvinner.
- Försvinner när det blir dag.

**Lost** (egen effekt): synen glider sakta åt sidan hela tiden, så att man går i cirklar.

## Lyktgubbe (Will-o'-the-wisp) och Jungle Spirit Mask

Lyktgubben är ett blekt blått ljus (partiklar, ingen modell) över myr och mörk skog. Masken är en snidad, målad trämask
som svävar mellan djungelträden. De beter sig likadant (`Wisp`):
- När de kommer väljer de en plats 24–48 block bort. När någon som ser dem följer efter (närmare än 9 block) glider de
  vidare mot platsen, alltid lite före.
- Vid slutet: oftast **ingenting**. Ljuset slocknar och alla som följde blir **Lost** i 1 min.
- Ibland (lyktgubbe 30 %, mask 20 %) **en skatt**: lyktgubben leder till en sjunken **Bog Grave** (en kista under mossa,
  loot `chests/bog_grave`: alltid en Wisp Ember, plus ben, Copper/Silver Coin, Rough Amber, Raw Bog Iron eller en Bog Torc),
  masken lämnar en **Spirit Mask** på marken.
- Försvinner på dagen, eller om ingen följt dem på en minut.

## Vätten (Tomte)

En liten gubbe i grå vadmal med röd luva och långt vitt skägg. Han bor där man odlar.
- **Flyttar in:** en gång i minuten på natten, för en spelare med minst **8 Farmland** inom 8 block och ingen tomte inom 48
  block, är det 1 på 3 att en tomte kommer. Han stannar vid gården för alltid (inom 12 block) och försvinner aldrig.
- **Julgröten:** högerklicka honom med en **Porridge** under **julmånaden (Deepwinter**, kalenderns månad 10). Han äter, man
  får skålen tillbaka och första gången det året en **Straw Yule Goat** (+3 Faith). Gröt en annan månad rör han inte.
- **Nöjd** (fick gröt senaste julen): var 5:e sekund växer en omogen gröda på gården ett steg.
- **Arg** (en jul gick utan gröt, eller man slog honom: arg i 30 dagar): var 30:e sekund nyps ett av gårdens djur (1 skada).
  Den jul som var innan han kom räknas inte mot någon.

## Näcken

En blek, våt man med långt mörkt hår som sitter i forsen och spelar fiol.
- Spelar en långsam melodi i moll (not-ljud) som **alla** inom 20 block hör. Bara den som ser (syn 30) ser honom.
- **Lyssnar man för länge** (mer än 20 s inom 20 block) dras man sakta mot honom och vattnet. När man står i vattnet bredvid
  honom tystnar musiken och han är borta. Gå därifrån i tid (spelare i creative dras inte).
- **Gåva:** högerklicka honom med en mynt eller kött (taggen `#mechanical:nacken_gifts`: Silver Coin, Gold Coin, allt kött).
  En gång per kalenderår och spelare **lär han ut**: **Fiddler's Blessing** (+2 Luck i 3 dagar), **+30 sanity** och
  en **Näcken's String**. Den han lärt drar hans musik aldrig mer. Det finns inga musikinstrument i modden än, därför blev
  lärdomen en tur- och moralbuff.
- Försvinner på dagen och när det regnar.

## Myrmannen (Bog Body)

En svartbrun, mumifierad kropp med rött torvfärgat hår och ett rep om halsen. **Inte en ande**: alla ser den och den går att döda.
- **Var:** Peat Bog på natten, på Peat (vikt 30, 1–2). Reser sig långsamt ur torven (2 s) med torvstänk.
- Går på spelare som en zombie (armarna framåt). Sjunker tillbaka i torven när solen kommer.
- **Stats:** 26 liv, 4 skada, rustning 2, fart 0,2.
- **Fynd** (`entities/bog_body`): **Peat-Tanned Hide** 1–2 (→ 1 Leather, formlöst), Rotten Flesh 0–2, **Bog Torc** 10 %.

## Bergatroll i skogen

I taigan sover ett troll som en mossig sten: två block **Mossy Troll Stone** på varandra (worldgen `troll_stone_sleeper`,
ungefär 1 på 48 chunks i Taiga, Old Growth Pine/Spruce Taiga, Snowy Taiga och Pine Heath).
- **Hugger man en stock** inom 10 block, eller slår på stenen själv, vaknar det: stenen är borta och ett vanligt **Troll**
  ([underground.md](underground.md#troll)) står där och går på den som väckte det. Samma kadaver och produkter.
- Ett skogstroll blir inte Troll Stone i solen direkt. Det **somnar om** som Mossy Troll Stone där det står när det inte har
  någon att jaga på 30 s, eller i dagsljus efter en minut vaket. Det kan väckas igen.
- Mossy Troll Stone går inte att bryta för den som spelar: man väcker bara trollet.

## Frostjätte (Rimthurs)

En sällsynt **miniboss** med boss-mätare, ungefär 5 block hög, isblå med istappsskägg. Alla ser den.
- **Var:** Snowy Plains, Ice Spikes, Snowy Taiga, Frozen Peaks, Snowy Slopes, Glacial Moraine. **Bara i snöstorm**
  (`Thaw.storm`: åska på vintern), under öppen himmel, aldrig två inom 128 block. När stormen slutar går den tillbaka in i
  snön, så man ser den bara när det stormar.
- Slag: 13 skada och Slowness. Var 8:e sekund **frostande andedräkt** mot den den jagar inom 8 block: Slowness II och man
  fryser (som i puddersnö).
- **Stats:** 160 liv, rustning 8, knockback-motstånd 1, fart 0,25, 50 erfarenhet. Fryser inte, ingen fallskada.
- **Fynd** (`entities/frost_giant`): alltid **Frost Heart**, 4–10 Snowball, 1–3 Packed Ice.
- **Frost Heart** (block, ljus 6): placerat smälter **ingen is inom 8 block** när det blir varmt (årstidernas töväder,
  `Thaw.melt`). Is som smälter av facklor nära (vanilla) skyddar den inte.

## Bergtagen

Sover man **ensam** (ingen annan spelare inom 64 block) **högt i fjället** (ett bergsbiom, `#minecraft:is_mountain`, på
höjd 100 eller mer) kan man en morgon (1 på 4, högst en gång per kalenderår) **vakna inne i berget**:
- en mörk sal 7×7 cirka 18 block under ytan, med **3 Vittra**, deras kista (`chests/bergtagen`: **Vittra Silver**, Vittra
  Hide, Cooked Vittra Meat, Brown Mushroom, Silver Coin) och ett schakt med stege upp;
- man är deras **gäst** (Mountain Guest, 5 min): ingen vittra hör en. Slår man en vittra slår den tillbaka.
- +5 Faith.

## Dune Wraith

En gestalt av sand i ökenns sandstorm (åska över öknen). Alla ser **sanden som virvlar** runt den; själva gestalten syns bara
med syn 20 och bara medan stormen pågår (halvgenomskinlig, trasig).
- Driver fram till närmaste spelare. På armlängds avstånd **stjäl den vatten**: en Water Bottle blir en tom Glass Bottle,
  eller en Water Jug töms. Sedan är den borta. Har man inget vatten stryker den bara förbi.
- **Slår man den** innan den hinner fram faller den sönder i en **Dune Glass**.
- Slutar stormen, försvinner den.

## Items

| Item | Från | Vad |
|---|---|---|
| Huldra Comb | Huldrans gåva | Trofé (turen är effekten) |
| Wisp Ember | Bog Grave (lyktgubbens skatt) | Trofé |
| Spirit Mask | Ibland där en Jungle Spirit Mask stannar | Bärs på huvudet: **+15 syn** |
| Straw Yule Goat | Tomtens tack för julgröten, en gång per år | Trofé |
| Näcken's String | Näckens lärdom | Trofé |
| Peat-Tanned Hide | Bog Body | → 1 Leather |
| Bog Torc | Bog Body (10 %), Bog Grave | Trofé |
| Vittra Silver | Vittrans kista (Bergtagen) | Trofé |
| Dune Glass | Slå en Dune Wraith | Trofé |
| Frost Heart | Frost Giant | Block: is inom 8 block töar inte |
| Mossy Troll Stone | Worldgen (taiga) | Ett sovande troll |

Effekter: **Lost** (synen glider, man går i cirklar), **Mountain Guest** (vittror hör en inte), **Fiddler's Blessing** (+2 Luck).

## Advancements (fliken Folklore)

Fair Is the Forest (se en huldra), Her Gift (ta hennes gåva), A Light in the Bog (se en lyktgubbe), Follow the Light (hitta
en skatt vid slutet), Faces in the Leaves (se en mask), Wear the Face (Spirit Mask), The Farm's Own (se tomten), Porridge for
the Tomte (julgröt), Heard the Fiddle (höra Näcken), The Fiddler in the Rapids (se honom), The Nix's Lesson (hans lärdom),
Out of the Peat (döda en Bog Body), A Troll in the Woods (väcka ett skogstroll), Rimthurs (döda en frostjätte), Heart of
Winter (Frost Heart), Taken by the Mountain (Bergtagen), Shape in the Sand (se en Dune Wraith), Glass from the Storm (Dune Glass).

## Test

`BeingsTest`: `folklore_spawn_biomes` (varje väsen i sina biom, inte i Plains/Mushroom Fields/Ocean eller i de andras),
`folklore_spawn_rules` (natt, torv, storm, sand, forsar, Bergtagens villkor), `folklore_huldra` (gåvan tagen ger kam och tur;
gå därifrån ger Lost; går inte att skada; den som inte tror ser henne inte), `folklore_tomte` (gröt bara i julmånaden, getter
och skål, nöjd tomte låter en gröda växa, arg tomte nyper djuren, gårdens storlek), `folklore_nacken` (dras först efter
20 s, gåvan ger välsignelse och sträng, en gång per år, eleven dras inte), `folklore_forest_troll` (stockhuggning väcker,
somnar om som mossig sten, kan väckas igen), `folklore_frost_heart`, `folklore_real_beings` (Frost Heart och Peat-Tanned
Hide som fynd, frostjätten försvinner utan storm), `folklore_bergtagen` (salen, stegen, kistan, vittrorna hör inte gästen men
alla andra), `folklore_dune_wraith` (flaskan och kannan töms, Dune Glass när den slås), `folklore_wisp` (vilse vid slutet,
Bog Grave, masken lämnar Spirit Mask, masken på huvudet ger syn). Tomtens humör kontrolleras också vid start (`Tomte.selfCheck`).
