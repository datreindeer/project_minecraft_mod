# En levande värld: stora önskelistan (2026-11-22)

> Status: **plan, inget byggt.** Alla frågor besvarade 2026-11-22 (se *Beslut* längst ned). Byggs fas för fas;
> det som byggs förs också in i respektive åldersdokument (`docs/ages/*.md`).

Målet: världen ska kännas levande och dynamisk även när ingen är där. Stammar bygger, handlar och lever sina liv,
djur påverkar naturen, och döden blir ett ögonblick där stammen hjälper varandra.

Befintligt som det här bygger på: `Clans` (spelarstammar, Fire Pit som centrum), `CampAge` (lägrets zon, radie 24),
`Tribe.REPUTATION` (NPC-stammars relation, idag **per spelare**, -100..100), `TribeVisits`/`Settlements`/`Raids`,
`Trails` (förgenererade stigar), `WagonEntity`, `FallingTreeEntity`/`TreeFelling`, `CarcassEntity`/`CarcassHauling`,
`Gemstones` (med flawless), `KnifeItem` (tier 1–3), Clay Kiln som redan smälter guld vid 1100°.

---

## A. Servern: världen lever vidare

| # | Sak | Plan |
|---|---|---|
| A1 | Tiden går när servern är tom | Kalendern och dygnet fortsätter. Vanillas `pause-when-empty-seconds` sätts till 0 (eller kringgås), så forceladdade chunks tickar. **Ändrar ett tidigare beslut** (se fråga 1). |
| A2 | Fire Pit forceladdar | En stams Fire Pit forceladdar chunken den står i + de 8 runt om (3x3, täcker zonen på radie 24). Grödor, djur, ugnar osv. rullar på. Släpps när stammen upplöses/flyttar. |
| A3 | Inga besök/raider utan spelare | `TribeVisits` och `Raids` hoppar över ett läger där ingen stammedlem står inom zonen. |
| A4 | Relation per stam | `REPUTATION` flyttas från spelare till spelarstam: alla i stammen delar relationen. Spelare utan stam har sin egen som idag. Gamla värden slås ihop vid uppgradering (se fråga 3). |
| A5 | Vägbygge | Vid **Friendly** eller högre kan man be NPC-stammens ledare (dialog) bygga en väg. Man får en **Road Marker** (banner) och sätter den där vägen ska sluta. Stammens folk går ut, hugger/gräver material, och bygger en stig (1–2 block bred, grus/trampad jord). Undviker att förstöra spelarbyggen (inga block som spelare satt rivs; vägen går runt). Broar bara när det inte går runt. |
| A6 | Handelsväg | Vid högre relation (t.ex. 60+) kan stigen uppgraderas till en bred handelsväg (3 block, sten). Samma process: de hämtar sten och bygger. |
| A7 | Inga stup | Vägbyggaren lägger trappor/slabs där höjden ändras mer än 1 block. Samma regel läggs in i `Trails` (förgenererade stigar). |
| A8 | Forceladdad handelsväg | En NPC-stam med handelsväg till en spelarstam forceladdar sin bas (3x3) och chunksen vägen går över, så bygge och handlare rör sig även när ingen är där. |
| A9 | Prestanda | Max ~10 spelare. Forceladdning bara för: spelarstammars Fire Pits, NPC-stammar med handelsväg, och vägchunks **bara medan någon rör sig/bygger på vägen** (handlare eller byggare). Vägchunks laddas i ett "fönster" runt de NPC:er som är ute, inte hela vägen hela tiden. Tickhastighet för slumpticks/entiteter i tomma forceladdade chunks behålls som vanilla. Mäts med `/tick query`/MSPT innan vi släpper det. |
| A10 | Levande natur | Djur betar gräs (gräs → jord, växer tillbaka), hjortar/älgar äter av unga träd och buskar, **bävrar** (ny djurart) fäller träd vid vatten och bygger dammar, vildsvin bökar upp jord, fåglar sprider bärbuskar. Fler förslag läggs till efter hand. |

## B. Klienten: visa världen

| # | Sak | Plan |
|---|---|---|
| B1 | "Du går nu in på X:s mark" | När man går in i en stams zon (spelarstam eller NPC-stam) visas en titel i stamsidans stil med namn, färg och er relation (t.ex. "Friendly 45"). Visas en gång per inträde. |
| B2 | Relationssida | En ny flik på stamsidan (`G`): alla NPC-stammar man känner till med relation, och andra spelarstammar. Se fråga 5 om NPC↔NPC och spelarstam↔spelarstam. |
| B3 | Squid-carcass hänger fel | Bugg: vänd modellen rätt. |
| B4 | Smithing Hammer-val på anvil | Shift+högerklick öppnar ett **radialt val** (hjul runt hårkorset) med ikon för varje smidesform, i stället för texten under. |
| B5 | Vedhög ramlar fel | Bugg: ved landar på fel sida av toppen. Fixa fallordningen. |
| B6 | Blod i hagen | Bugg: tama djur sprider blod utan att vara skadade. Hitta orsaken (troligen trängsel-/kvävningsskada eller blodsdroppar från hunger). |
| B7 | Multiplayer-sidan | Servrar med moddet visar en detaljerad rad i Tab-stil: säsong och datum, världsålder, antal stammar, spelare online per stam. Data skickas i serverns status-svar. Servrar utan moddet ser ut som vanligt. |
| B8 | Eget ljud för stammar | Inte villager-ljud. Se fråga 11. |

## C. Stammarnas folk (stor AI-uppgradering)

- Går aldrig in i Fire Pit/campfire (pathfinding-straff och stannar vid kanten).
- **Vid besök hos spelare:** alla sätter sig runt elden i stället för att vandra. Finns vatten nära går fiskarna dit och fiskar,
  och några av de andra följer med och sätter sig vid vattnet. Ledaren reser sig ibland och går fram till en spelare i stammen.
- **Idle-animationer:** sitta och tälja, kolla vapnet, rota i väskan, äta, prata med varandra (vända sig mot varandra, gestikulera),
  sträcka på sig, värma händerna vid elden.
- **Natt:** alla utom krigarna lägger ut en Straw Bed på marken nära elden och sover till morgonen. Krigarna vaktar.
- **Dygnsrytm i byn:** morgon (äta, sträcka), dag (arbeta: hugga, samla, fiska, bygga), kväll (samlas vid elden), natt (sova, vakter).
- **Försvar:** besökande stammar hjälper till mot monster och raiders vid ens eld (krigarna slåss, de andra flyr till elden).
- **Carcass som gåva:** lämnar man en carcass hos en stam (Neutral eller högre) ses det som en generös gåva (+relation, mer för större djur). Under Neutral ses det som ett hot (-relation, krigarna blir vaksamma).

## D. Vagn, hästar och djur

| # | Sak | Plan |
|---|---|---|
| D1 | Vagnen som en enhet | Hästarna är fast i vagnen (ingen lead som kan lossna). Häst 2 följer häst 1 exakt, vagnen följer hästarna som en stel enhet, ingen kollision mellan dem. |
| D2 | Spänna fast | Rid häst 1 eller led den med lead till vagnen och klicka på vagnen → låst i första spannet. Samma för häst 2. Shift+högerklick på en spänd häst släpper den. |
| D3 | Tyngd och trötthet | Hästen får en "ork"-mätare. Last (vagn, carcasses, trä, ryttare) sänker farten lite och tömmer orken snabbare. Trött häst går långsammare; helt slut stannar den, kastar av ryttaren och lägger sig. Vatten (hink eller att den går ner till vatten, utan att simma) och vila fyller på. |
| D4 | Små carcasses på hästen | De man bär på axlarna kan hängas på hästens sidor (en per sida). |
| D5 | Tama djur trampar marken | Där många tama djur går: gräs → jord → coarse dirt → mud (när det regnar) → packed mud (när det torkar). Byggs på `TrampledPaths`. |
| D6 | Feeding Trough | Tama djur och hästar behöver mat och vatten. Utan det dör de inte, men går långsamt (ny trött-animation) och vill inte para sig. **Feeding Trough** som man fyller med hink (vatten) eller mat (rätt mat per djur). Flera tråg bredvid varandra blir ett större. Vattnet räcker länge. Ät-/drick-animationer. |

## E. Döden, att bli nedslagen och att vakna

- **Nedslagen (downed):** vid 0 liv lägger sig spelaren på marken (egen modell/pose). Man ser och kan vrida kameran, men synen blir
  sämre och blodigare med tiden. Inga items, ingen hotbar eller inventory. På skärmen: "En stammedlem kan hjälpa dig upp" och
  "Håll [tangent] för att ge upp" (respawn). Efter ~1 minut dör man (se fråga 8).
- **Hjälpa upp:** en stammedlem shift+högerklickar och håller; en animation som tar några sekunder och avbryts av slag.
  Sedan bär hjälparen den nedslagna på axlarna (som en carcass), kan lägga ner den, och på en säker plats hålla en tangent för att
  **återuppliva**. Den nedslagna vaknar med 2 hjärtan och behåller sitt inventory. Bara död/ge upp tappar allt.
- **Vakna utan säng (world spawn):** tredjepersonskamera, man ligger på marken, ögonen öppnas långsamt (fade/blink), reser sig, sedan
  förstaperson och styrning. **Andlig form** i 2 minuter eller tills man slår/interagerar: osynlig för mobs.
- **Vakna i säng:** samma animation fast i sängen, utan andlig form.

## F. Nya saker

| # | Sak | Plan |
|---|---|---|
| F1 | Dra fällda träd | Se fråga 6. Grundidé: ett fällt träd är en **stock-entitet** (block som hör ihop). Man drar i rotänden (rep/lead?), stocken "sugs ihop" till en lång stock. Tyngre ju större; stora träd kräver flera spelare. Kan yxas/sågas av block för block, då delas stocken. Max längd för att få plats i vagnen (lite överhäng, vagnens bakgavel tas bort i modellen). |
| F2 | Sawmill (ångåldern) | Multiblock (lång bädd med sågblad) driven av vattenhjul. Man drar in hela stockar. Snabbare och ger mer plankor och bark än handsågning. |
| F3 | Kolsvarta grottor | Bara lava, kristaller och ljuskällor man själv sätter lyser. Se fråga 7. |
| F4 | Ancient Oak-dyrkare | Ny folkgrupp vid Ancient Oaks, i gröna naturfärgade kläder. Tre sorter: **samlare** (kniv + korg, plockar bär och svamp, planterar nya buskar och svampar i tomrum runt trädet, flera sorter), **bedjare** (sitter och ber till trädet), **ljusbärare** (går runt med tända ljus, ny animation). **Ledaren** har en krona av giant deer-horn, speciella kläder och en magisk stav med gröna partiklar och sken. Högerklick öppnar en ny GUI med en **questlinje** som delas av hela stammen. Uppdrag: samla items m.m., allt svårare. Högre uppdrag ger unika items, bl.a. **Ancient Ritual Knife** (bästa kniven, högsta slaktgraden). Se fråga 9. |
| F5 | Crop Stakes | Grödor med crop stakes växer snabbare (förslag +25 %). |
| F6 | Rock-items | En Rock för varje ny stensort (från world-materials-plan) som saknar en. |
| F7 | Mold Chest | En kista som bara tar emot clay molds. |
| F8 | Spärra fler vanilla-recept | String → Wool tas bort. Gör en genomgång av alla vanilla-recept som kringgår moddets progression och spärrar dem (listan förs in i åldersdokumenten). |
| F9 | Satchel-uppgradering | Se fråga 10. |
| F10 | Guld och smycken | Ett nytt multiblock för **guldgjutning** som byggs med stål. En **Jeweler's Bench** som gör smycken av guld + gemstones, till ring-/halsband-slotsen. Vanliga stenar ger små fördelar (skydd mot kyla osv.), **flawless** ger riktiga fördelar. Se fråga 10. |
| F11 | Dirt Slab | Kan bli grass/path/snötäckt som ett vanligt dirt block. (Mjukare worldgen med slabs väntar, tveksamt.) |
| F12 | Stor Fire Pit (Iron/Steel) | Uppgradering till 3x3. Brinner längre och effektivare, mycket större rökpelare. Uppgraderingen fungerar som ett multiblock: den säger vad som står i vägen och växer åt det håll som är ledigt. Fire Grate, Tripod och Kettle får nya större modeller. Man kan hänga ett helt flått djur över elden → mycket cooked food. Ger också fördelar framåt (se fråga 12). |

---

## Förslag på ordning (faser)

1. **Snabba buggar:** B3 squid, B5 vedhög, B6 blod i hagen, F5 crop stakes, F8 vanilla-recept, F6 rocks, F7 Mold Chest, F11 Dirt Slab.
2. **Servern lever:** A1, A2, A3, A9 (forceladdning och tid).
3. **Vagnen:** D1, D2, sedan D3, D4.
4. **Relationer:** A4, B1, B2, C (gåvor, försvar).
5. **Stammarnas AI:** C (sitta, sova, idle-animationer, dygnsrytm), B8 ljud.
6. **Döden:** E.
7. **Vägar:** A5–A8.
8. **Smide och fire pit:** B4 anvil-hjul, F12 stor Fire Pit, F9 satchel, F10 guld och smycken.
9. **Djur och natur:** D5, D6, A10 (bävrar m.m.).
10. **Ancient Oak-dyrkare:** F4.
11. **Träd och såg:** F1, F2 (F2 när ångåldern finns).
12. **Multiplayer-sidan:** B7. **Grottor:** F3.

## Beslut (2026-11-22)

Gäller före tabellerna ovan där de skiljer sig.

| Fråga | Beslut |
|---|---|
| Tom server (A1) | Tiden går vidare i **halv hastighet** när ingen är online (ett speldygn = 2 h). Ersätter beslutet i `seasons-and-tribes.md` om att tiden står still. |
| Fire Pit forceload (A2) | 3x3 chunks. Stor Fire Pit: 5x5. |
| Relation per stam (A4) | Vid sammanslagning får stammen **medelvärdet** av medlemmarnas relationer. |
| Vägar (A5–A6) | Max **1000 block**. Gratis under 500 block, längre än så vill de ha betalt i silver/guld (Silver Coin/Gold Coin). |
| Relationssidan (B2) | Bara er relation till varje NPC-stam. Inga nya NPC↔NPC- eller spelarstam↔spelarstam-relationer. |
| Dra träd (F1) | **Allt på en gång:** alla fällda träd blir stock-entiteter med gravitation som kan dras och yxas/sågas av (delas). Man drar med **lead i rotänden**; flera spelare kan sätta var sin lead och dragkraften läggs ihop. |
| Grottor (F3) | Spelarens Brightness (gamma) ignoreras under jord. Glow Lichen m.m. lyser som idag. |
| Nedslagen (E) | Dör efter ~1 minut utan hjälp. Mobs ignorerar nedslagna (spelare och raiders kan avsluta). Ingen stammedlem online eller singleplayer → dör direkt. |
| Dyrkarna (F4) | 10–15 uppdrag. Har en relation till stammen som andra NPC-stammar och kan bli fientliga (skada trädet/dem); dålig relation stänger questlinjen. Belöningar: Claude föreslår en lista för godkännande. **Ancient Ritual Knife** ger en **ny slaktnivå**: finare delar (t.ex. sener, organ, troféer) som bara den kniven kan ta. |
| Satchel (F9) | Satchel kan uppgraderas med **fack för verktyg** (de nya, som Saw, Tongs osv.). Sedan en ny större väska, **5x5**, av läder + stål (Steel, slutet av järnåldern). |
| Guld (F10) | Clay Kiln smälter guld som idag. Det nya multiblocket (byggs med stål) behövs för att **gjuta** smyckesämnen och andra guldföremål. |
| Smycken (F10) | Claude föreslår en bonus per gemsort i vanlig och flawless, för godkännande innan bygget. |
| Ljud (B8) | CC0-ljud från freesound.org (.ogg), med källa och licens noterade. |
| Stor Fire Pit (F12) | Kommer vid **Steel**. Ger forceload 5x5, fler besökande band kan stanna samtidigt, och krävs för senare stamfunktioner. |
| Ordning | **Ändrat 2026-11-22:** ny fas 1 = F1 stockar, B7 multiplayer-sidan och GUI-ändringarna (B1 zontitel, B2 relationsflik, B4 anvil-hjul). Sedan faserna ovan i ordning. |
