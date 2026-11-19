# Odling (plan / spec)

> Status: **spec, beslutad 2026-11-19, inte byggd.** Odlingen börjar i stenåldern (yngre stenålder: vild säd, flintskära).
> Senare åldrar ger bättre verktyg (bronsskära, plog). Den här filen är levande dokumentation; när delar byggs förs de in
> i `docs/ages/01-stone-age.md` och markeras här.

## Beslut

- **Säd = utsäde.** Mogen säd ger en **kärve**; tröskning ger **Grain** och **Straw**. Samma Grain sås, mals eller äts:
  varje skörd är ett val mellan att äta och att odla. Inga separata "seeds" som rinner över.
- **System i första versionen:** temperatur och frost, vatten och torka, bördighet och kompost, kråkor och fågelskrämma.
- **Grödor:** vanilla-grödorna byggs om (wheat, carrot, potato, beetroot, melon, pumpkin) och några nya läggs till:
  vild säd (Wild Einkorn, Wild Emmer), korn (Barley) och en baljväxt (Wild Pea).

## Skörd och utsäde

- **Omogen** gröda ger ingenting när den bryts (utom vild säd: 0–1 Grain).
- **Mogen vete/korn/einkorn/emmer** → 1 **Sheaf** av sorten (+ ingenting annat). Med **Flint Sickle**: 1 Sheaf + 50 %
  chans på en till, och 1 Straw.
- **Tröskning:** lägg Sheaf på en hård yta (sten, kullersten, Stone Anvil) och slå med **Flail** några gånger →
  1–3 **Grain** (snitt ~2) + 1 **Straw**. Utan flail: i händerna (crafting) ger 1 Grain.
- **Rotfrukter:** mogen Carrot/Potato ger 1–2 (vanilla 2–5); en av dem behövs för att plantera igen, så nettot är litet.
  Beetroot: 1 Beetroot + 0–1 Beetroot Seeds.
- **Melon/Pumpkin:** som vanilla, men frukten ger bara 0–1 frö när den bryts/delas.
- **Grain** är mat i sig (kokt **Porridge** i Clay Bucket/vatten vid elden), mals till **Flour** i en **Quern**.
- **Bröd:** Flour + Clay Water Bucket → **Dough**; bakas på en **Baking Stone** vid lägerelden eller i Clay Kiln.

## Var utsädet kommer ifrån

- **Wild Einkorn** (slätt, meadow, savann) och **Wild Emmer** (slätt, sparsamt i skog): vilda grässtrån i små bestånd,
  sällsynta. Skördas med kniv (100 %) eller för hand (50 %) → 1 Grain av sorten. Kan sås: blir odlad säd.
- **Wild Pea** (skogsbryn, meadow): klängväxt, ger 1–2 Peas (mat, utsäde).
- Byar: döende fält som idag ger lite. Vete där räknas som vanlig odlad säd.

## Växtvillkor (per växtsteg, slumpas i randomTick)

Varje gröda har ett **temperaturspann** på samma skala som Temperature-systemet (0–20; slätt ~12, taiga ~8, snö 2–6,
öken 20+, natt −5). Utanför spannet växer den inte. Under **frostgränsen** kan plantan dö.

| Gröda | Växer (temp) | Bäst | Frost dödar under | Övrigt |
|---|---|---|---|---|
| Wheat / Einkorn / Emmer | 7–17 | 10–14 | 2 (unga steg) | |
| Barley | 5–16 | 8–12 | 0 | tålig, odlas i taiga |
| Carrot | 4–15 | 7–12 | −1 | tål kyla |
| Potato | 6–16 | 9–13 | 2 | |
| Beetroot | 5–16 | 8–13 | 1 | |
| Wild Pea | 6–16 | 9–13 | 2 | ger bördighet |
| Melon / Pumpkin | 12–22 | 15–20 | 6 | vill ha värme |

- **Tillväxt:** chans per randomTick = bas × temperaturfaktor (1 i bästa spannet, sjunker linjärt mot 0 vid kanterna) ×
  vatten × bördighet × ljus. Vanilla-grödor får samma klass.
- **Frost:** på natten, om luften (ambient) är under grödans frostgräns och plantan står under bar himmel, liten chans per
  randomTick att den dör (blir **Withered Crop**, en död planta som ger 1 Straw/Plant Fibre). Mogna plantor dör inte, de
  bara slutar.
- **Ljus:** kräver dagsljus (sky light ≥ 9 dagtid). Facklor räcker inte.

## Vatten och torka

- Farmland har vanillas MOISTURE 0–7. Regn sätter den till 7. Vatten inom 4 block håller den uppe som vanilla.
- **Torka:** i varma biomer (temp > 15) utan regn sjunker fukten snabbare. Fukt 0 → tillväxten står still; länge torr →
  unga plantor vissnar (Withered Crop).
- **Rotröta:** i träsk och om farmland står i vatten/under regn länge kan rotfrukter ruttna (liten chans).
- En **Clay Water Bucket** kan vattna en farmland (högerklick: fukt 7, sliter hinken).

## Bördighet

- Varje farmland får **bördighet 0–3** (blockstate). Ny åker från gräs/jord = 2; podzol/mull = 3.
- **Skörd** av en mogen gröda sänker bördigheten 1 (inte Wild Pea, som höjer 1).
- Bördighet 0: halv tillväxt och halv skörd. 1: något sänkt. 2: normal. 3: snabbare och chans på extra.
- **Återställa:**
  - **Träda:** farmland utan gröda får +1 ungefär varje Minecraft-dag (ett par timmar som mest).
  - **Compost:** Leaf Litter, växtrester, Straw, ruttet kött → **Compost Heap** (block) → **Compost** (item). Högerklick
    på farmland: +1. Leaf Litter får äntligen en användning.
  - **Manure:** djur i byar/fållor släpper ibland Manure. +2.
  - **Växelbruk:** Wild Pea höjer.

## Kråkor och fågelskrämma

- **Kråkflockar** (Hooded Crow, finns redan) som ser nysådd/ung gröda inom ~24 block kan landa på åkern och äta den
  (plantan försvinner, ett steg i taget). De flyr som vanligt när en spelare kommer nära.
- **Scarecrow:** byggs av sticks, Straw och en pumpa/hö-huvud (2 block högt, kläder av hide). Inom 12 block landar inga
  kråkor på åkern. Kan bära en Leather Helmet/hatt för utseendet.

## Verktyg och block

| Sak | Recept (förslag) | Användning |
|---|---|---|
| Flint Hoe (finns) | | Gör farmland (vanilla) |
| Flint Sickle | Tool Station: flinta + twine + stick | Skörd: extra Sheaf + Straw; klipper gräs/vild säd snabbt |
| Flail | 2 sticks + twine + stick | Tröska Sheaf |
| Quern | 2 sten + stick | Mal Grain → Flour |
| Baking Stone | platt sten vid lägereld | Baka Dough → Bread |
| Compost Heap | sticks + leaf litter | Kompostera |
| Scarecrow | sticks + straw + huvud | Håller kråkor borta |
| Straw | från tröskning | Halmtak, bädd, kompost, scarecrow |

## Att bestämma senare

- Säsonger (vår/höst) finns inte; temperaturen räcker tills vidare.
- Bronsålderns bronsskära och plog (snabbare åker, större skörd).
- Djurhållning (gödsel från fållor) är bara antydd här.
