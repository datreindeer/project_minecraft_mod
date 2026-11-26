# Dynamiska verktyg – design

Datum: 2026-11-26. Projekt 3 av 3. Användarens beslut: `docs/skills-and-tools-plan.md`. Användaren bad att arbetet inte
skulle stanna; allt märkt **(Claude)** är mitt beslut och kan rättas.

## Varför

Hur man sätter ihop ett verktyg ska spela roll: ett järnhuvud på ett stålskaft med läderlindning håller längre än samma
huvud på en pinne med snöre, och man ska se det på verktyget.

## Byggsätt (Claude)

| | Vad | För/emot |
|---|---|---|
| **A. Behåll item-typerna (valt)** | Huvudets material bestämmer item (Bronze Pickaxe som i dag). Binding och rod sparas på stacken (komponent) och skriver om durability, fart och skada när verktyget görs. | Alla taggar, `KnifeItem`-nivåer, advancements, 57 recept och gamla verktyg fungerar som förut. Ingen världsomvandling. |
| B. Ett item per verktygstyp | "Pickaxe" med alla tre delar i komponenter | Ca 50 kodställen som testar exakta items/klasser måste skrivas om; riskabel omvandling av gamla verktyg. |

A uppfyller användarens beslut: delarna avgör stats, verktyget ser ut som sina delar, gamla verktyg fungerar (de räknas
som byggda med receptets gamla standarddelar, så ingen omvandling behövs).

## Mål

- Verktyg med huvud och skaft blir dynamiska: pickaxe, axe, shovel, hoe, sickle, knife, saw, chisel, smithing hammer,
  quarry hammer, spear, sword, tongs, ard.
- **Inte** dynamiska (Claude): bows, fishing rods, broom, bucket – de har inget huvud på skaft.
- Ny Tool Station-skärm: vänster väljer verktygstyp, mitten tre slots med spökbild av vad som ska dit, höger färdigt
  verktyg med stats och vad varje del bidrar med.
- Verktyget ser ut som sina delar (rod och binding syns på sprite, i GUI och i handen).
- Smithing-level ger +0,5 % durability per level (från skills, `Skills.level(player, SMITHING)`), sparas på verktyget.

## Delar och nivåer (Claude)

**Nivå (tier):** flint/stone 0, copper 1, bronze 2, iron 3, steel 4. Rodens nivå får inte vara högre än huvudets (flint och sten: bara trä – Stick eller Tool Rod – som användaren sa).

**Rods** (durability ×, fart ×, skada +):
| Rod | Nivå | Dur | Fart | Skada | Item |
|---|---|---|---|---|---|
| Stick | 0 | 0,8 | 1,0 | 0 | `minecraft:stick` |
| Tool Rod (trä) | 0 | 1,0 | 1,0 | 0 | finns |
| Bone Tool Rod | 1 | 1,1 | 1,0 | 0 | **ny**: Bone + knife (formlöst, kniven slits) |
| Copper Tool Rod | 1 | 1,2 | 0,98 | 0 | **ny** |
| Bronze Tool Rod | 2 | 1,35 | 0,98 | +0,5 | **ny** |
| Iron Tool Rod | 3 | 1,5 | 0,96 | +0,5 | finns |
| Steel Tool Rod | 4 | 1,75 | 0,96 | +1 | **ny** |
Nya metallrods görs på samma sätt som Iron Tool Rod görs i dag (byggaren hittar och speglar det receptet per metall).
Svärd och kniv har ett grepp i stället för skaft: där gäller samma rod-tabell (greppet är kärnan i fästet).

**Bindings:**
| Binding | Nivå | Dur | Fart |
|---|---|---|---|
| Twine | 0 | 0,85 | 1,0 |
| Cordage | 0 | 1,0 | 1,0 |
| Hide Glue | 0 | 1,05 | 1,0 |
| Leather | 0 | 1,1 | 1,03 |
| Sinew | 0 | 1,1 | 1,0 |
| Bronze/Iron/Steel Plate | 2/3/4 | 1,25 | 0,95 |
| Iron Tool Rod (kil) | 3 | 1,3 | 1,0 |
Plattor och kil kräver huvud av minst sin nivå.

**Uträkning:** verktygets durability = item-typens nuvarande durability ÷ (standarddelarnas dur-produkt) × (valda delarnas
dur-produkt) × (1 + 0,005 × Smithing-level). Standarddelar = de som står i receptet i dag, så ett verktyg byggt med
samma delar som förr får exakt samma stats som förr. Fart och skada räknas på samma sätt relativt standarddelarna.
Poor Bronze (70 %) gäller ovanpå.

## Hur det byggs

- **Komponent** `tool_parts` (binding-id, rod-id, smithing-level). Saknas den: standarddelarna (gamla verktyg).
- När verktyget tas ut ur Tool Station sätts `tool_parts` och de vanliga komponenterna `MAX_DAMAGE`, verktygsfart och
  attackskada beräknas och skrivs på stacken, så vanilla och all befintlig kod räknar rätt utan ändring.
- **Recept:** `ToolStationRecipes.Recipe` får listor: tillåtna bindings och rods (filtrerade på nivå mot huvudet) i
  stället för en enda. Upplåsning som i dag (huvudet).
- **Utseende:** en generator delar varje verktygs sprite i huvud och skaft (skaftets pixlar = trä-färgerna) och lindning
  (pixlarna där skaftet möter huvudet). Den gör ett skaft-lager per rod-material (skaftets pixlar omfärgade med
  materialets palett, skuggning bevarad) och ett lindningslager per binding. Item-modellen blir `minecraft:composite`:
  huvud + `select` på rod + `select` på binding (val via `custom_model_data`-strängar som skrivs vid tillverkning).
  Verktyg med egna 3D-modeller i handen (spjuten): 3D-modellen följer inte delarna i första versionen
  (`shortcut:` i koden), GUI-spriten gör det.
- **Tooltip:** "Rod: Bronze Tool Rod · Binding: Leather" och en rad om smithing-bonusen.
- **Receptvisaren:** Tool Station-recepten visar alla tillåtna bindings/rods (bara sedda); rod-sloten visar bara rods
  som passar huvudet; stats-rutan från Tool Station visas på receptsidan.

## Lämnat åt byggaren
Exakta paletter per rod-material, spökbilderna i slots, layouten i nya Tool Station (följ `HideUi`), hur Iron Tool Rod
görs och därmed de nya metallrods.

## Anti-mål
- Ett gammalt verktyg som byter stats när världen laddas.
- Fula omfärgade skaft (kvalitetsribban: skuggning och kanter ska se handgjorda ut).

## Ändringar efter användarens svar (2026-11-26)

- Byggsätt A bekräftat av användaren.
- **Copper och Bronze Tool Rod gjuts i form:** en ny skaftform (unfired clay → fired, som de andra formerna) görs i
  Mold Bench, gjuts med koppar eller brons och slipas som kopparhuvudena. Crafting-receptet (2 ingots) tas bort.
  Poor Bronze ärvs till skaftet som till andra bronsdelar.
- **Bågar och fiskespön blir dynamiska** (broom och bucket fortfarande inte):
  - Båge: sträng (Cordage/Sinew/String …) + grepp (binding-tabellen) + stav (stick/Tool Rod/Yew Stave …). Delarna
    påverkar durability och dragkraft (pilens fart/skada) och dragtid. Byggaren sätter faktorer i samma stil som
    rod-tabellen, relativt dagens standarddelar (samma delar = samma stats som förr).
  - Fiskespö: krok (bone/bronze/iron hook = huvudet, som i dag) + lina (Twine/String/Sinew …) + spö (stick/Tool Rod/
    bambu om det finns …). Delarna påverkar durability och hur snabbt fisken nappar (lure-tid).
- Skills (samma gren): minnet av spelarutsatta block (ingen Mining-XP) sparas med världen i stället för i minnet.
- Malmbalansen (Mining + Masonry staplas) och skills-tempot behålls som de är.

## Bågar och fiskespön – byggarens val (Task 9, Claude)

Receptet har en uttrycklig sort (`ToolParts.Kind`: TOOL, BOW, FISHING_ROD, FIXED) i stället för namnsökningen; broom och bucket är FIXED.
Varje slot läser sortens egen tabell; en slot utan tabell tar bara receptets egen del. Faktorer relativt receptets standarddelar,
så samma delar som förr = exakt samma stats (testat för alla recept).

**Båge** (slot: sträng, grepp, stav; strängen är det enda "huvud" som är ett val och sparas i `tool_parts.head`):
| Sträng | Dur | Dragfart | Kraft |  | Stav | Dur | Dragfart | Kraft |
|---|---|---|---|---|---|---|---|---|
| Twine | 0,7 | 1,1 | 0,85 |  | Stick | 0,75 | 1,1 | 0,85 |
| Cordage | 1,0 | 1,0 | 1,0 |  | Tool Rod | 1,0 | 1,0 | 1,0 |
| String | 0,9 | 1,05 | 1,05 |  | Yew Stave | 1,0 | 1,0 | 1,0 |
| Sinew | 1,2 | 0,95 | 1,1 |  | | | | |
Grepp = binding-tabellens Twine, Cordage, Leather, Sinew (durability och fart som där; fart = dragfart). Yew Stave går bara på
Yew Longbow och den tar bara Yew Stave (staven avgör vilken båge det blir). Dragtid = bågens egen ÷ produkten av dragfart;
kraft multiplicerar pilens fart (och därmed skadan) i `shoot`. Sinew Bow har inget Tool Station-recept och lämnas.

**Fiskespö** (krok = huvudet som förut; lina, spö): linor Twine 0,85 / Cordage 1,0 / String 0,9 (+2 s tidigare napp) /
Sinew 1,1 (+1 s); spön Stick 0,8 / Tool Rod 1,0 / Bamboo 1,15 (bara durability). Lure läggs på krokens (`BoneHookRodItem.lureTicks(stack)`);
Crude Fishing Rod är nu en `BoneHookRodItem` med 0 egen lure (kastar som vanilla). Inga negativa lure-värden (FishingHook kapar vid 0).

Komponenten `ranged_parts` (kraft, dragtid, lure) skrivs vid tillverkning och läses av item-klasserna; saknas den = dagens beteende.
Utseende: `gen_tool_parts.py` RANGED delar varje bild (även pulling/cast) i zoner efter färg och färgar om sträng/lina, grepp och stav/spö.
`shortcut:` bågens dragbild (use_duration-skalan i item-modellen) följer inte en ändrad dragtid.
