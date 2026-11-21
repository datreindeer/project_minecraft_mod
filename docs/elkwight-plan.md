# Elkwight: plan

En svår boss i en nedbrunnen skog långt från spawn: en minotaurlik jätte med **älghuvud och en enorm hornkrona**, som en förvriden
skogens kung. Gröna runor och lysande gröna ögon ger den nordiska tonen. Hans yxa i okänd metall är en ledtråd som går att
undersöka senare. Byggs fas för fas, som
[underground-plan.md](underground-plan.md).

## Beslutat
- **Älghuvud**, inte tjur. Namnet är **Elkwight** (entity-id `elkwight`).
- **Får aldrig fastna**: en boss som fastnar blir lätt att döda (se 3, "Fastnar aldrig").
- **En Elkwight per ruin.** Han kommer inte tillbaka när han har dödats. Ruinerna är sällsynta, så den som vill slåss igen
  får resa vidare.
- **Minst 2500 block från 0,0** (både biomet och ruinerna). Ingen fast punkt: platsen beror på seed.
- **Skalas med spelare**: mer liv (och lite mer skada) för varje spelare inom 32 block när striden börjar.
- **Yxan är bara en trofé nu**: ett tungt föremål som dras och kan ställas ut. Att undersöka den kommer senare.
- Syns för alla (inte bunden till Faith).
- **Gröna runor och ögon**: ögonen lyser ljusgrönt i mörkret (emissivt lager, som AngryEyesLayer). Runor i samma gröna
  sken är ristade både på yxan och i ruinens stenar runt om.
- **Despawnar aldrig** (persistent), inte heller när ingen är i närheten eller när chunken laddas ur.
- **Läker bara genom att äta**: ingen egen återhämtning. Dör en spelare nära honom äter han av den döda och får tillbaka liv.

## 1. Biomet: Scorched Barrens (nedbrunnen skog)
- Nytt ytbiom, bara längre än 2500 block från 0,0. En skog som brunnit: svartbrända stammar (stående, fallna, stubbar),
  aska (eget askblock, Coarse Dirt, Gravel), hällar av Blackstone och Basalt, inget gräs. Kalla, mörka färger på himmel
  och dimma.
- Inga djur utom asätare (korpar) och möjligen enstaka mörkrets väsen på natten.
- Block reagerar på klimatet som övriga block (frost och snö på vintern).
- Gametest: biomet finns inte inom 2500 block.

## 2. Ruinen
- Struktur (jigsaw eller NBT): stor raserad arena **med breda öppningar och högt i tak** (hornkronan är ~4 block bred), med pelare, trappor och murar av **Blackstone av olika slag** (polished,
  bricks, cracked bricks, chiseled, gilded), Basalt och Magma. **Runstenar och ristade block med lysande gröna runor** (eget block, emissiv textur, svagt ljus) i murar, pelare
  och kring arenan. Eldar som brinner (Soul Campfire, brinnande block i skålar).
- Sällsynt inne i biomet (stor spacing/separation), alltid minst 2500 block från 0,0.
- **Dimma när man kliver in**: tät och mörk dimma på klienten inom ruinens gränser, som tunnas ut när bossen är död.
- Elkwighten väntar i mitten och vaknar när en spelare kommer in i arenan. Bossbar.
- Kistor med lite guld och bronsföremål. Ingen kista med det bästa: belöningen är kroppen och yxan.

## 3. Elkwight (entity `elkwight`)
- Människokropp med **älghuvud och stor hornkrona** (skovelhorn, ca 4 block brett), **2× spelarens storlek** (ca 1,2 × 3,6
  block). Hornkronan är bara utseende: hitboxen är kroppen, så kronan aldrig hakar i väggar och tak. Bär den dubbelbladiga yxan
  med båda händerna.
- Mycket liv, hög rustning, immun mot knockback och fallskada. Skalas per spelare (se Beslutat).
- **Despawnar aldrig**: persistent från start, sparas med ruinen. Lämnar man striden ligger skadan kvar tills man kommer tillbaka.
- **Äter de döda**: dör en spelare inom ~16 block går han till platsen, sätter sig på knä och äter i ~3 sekunder. Han får
  tillbaka **25 % av sitt maxliv** per död spelare. Under måltiden står han still och tar extra skada, och avbryts han
  (tillräcklig skada medan han äter) får han bara den del han hunnit äta. Spelarens föremål ligger kvar på marken.
- Varje attack förvarnas tydligt (ljud + pose) så att man kan lära sig mönstret.
- **Fastnar aldrig**: kan han inte nå spelaren (hål, torn av block, bakom murar, i vatten) gör han något annat i stället:
  hoppar upp eller ner (språng på ~6 block), slår sig igenom naturliga block, löv och brända stammar, eller använder
  yxkast och markvåg. Står han still utan väg i några sekunder teleporteras han inte, utan går över till avståndsattacker.
  Gametest: han når en spelare som står på ett torn och en som gömmer sig i ett hål.

**På nära håll**
- **Snurr**: snurrar med yxan och slår alla runt om sig (radie ~4). Förvarning: drar yxan bakåt och stampar.
- **Hornsvep**: sänker hornkronan, skrapar med hoven och rusar fram; kronan träffar brett (~3 block). Rusar han in i en
  vägg vacklar han i 1 sekund (ett kort läge att slå, inte ett fast läge).

**På avstånd** (man ska inte kunna hålla sig undan)
- **Yxkast**: kastar yxan som snurrar mot spelaren och kommer tillbaka (eller går och hämtar den: fönster att slå till).
- **Markvåg**: slår yxan i marken; en våg av sprickor rusar mot spelaren, kastar upp den och skadar. Naturliga block i
  vågens väg spricker (sten blir grus/cobble, träd fälls med FallingTree), men aldrig byggda block, så ingen bas förstörs.
- **Mot den som flyr långt** (> 24 block): stöter mer och kastar oftare.

## 4. Kroppen och yxan
- Han lämnar en **Carcass** som är ännu tyngre än de stora djuren: ny nivå "kolossal". 1 spelare kan inte rubba den,
  2 drar den extremt långsamt, 3–4 i normal takt. Går att lägga på vagn eller travois. Slaktas som andra (hud, kött, horn,
  ben) med egna produkter.
- **Yxan** är ristad med samma gröna runor (glöder svagt även som trofé). Den landar bredvid som ett eget tungt föremål (entity, drar som en carcass): minst 2 spelare för att rubba den, går
  att lägga på vagn eller travois. Kan ställas ut som ett block (trofé). Senare: **undersöka** den (metallen, dess ursprung).
- Systemet för tunga föremål byggs så att fler saker än carcasser kan dras på samma sätt.

## 5. Advancements
- *Into the Ashes* (gå in i Scorched Barrens), *The Smoke Clears* (döda Elkwighten), *Heavy Burden* (dra hem yxan),
  *Many Hands* (bär kroppen tillsammans med 3 andra).

## Fasordning
1. Biomet Scorched Barrens + gräns 2500 block.
2. Ruinen + dimman.
3. Elkwighten: modell, rörelse, närattacker, bossbar, skalning.
4. Avståndsattacker: yxkast och markvåg.
5. Kolossal carcass + yxan som tungt föremål + trofé.
6. Advancements, registry och dokumentation.
