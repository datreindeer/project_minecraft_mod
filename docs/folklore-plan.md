# Folktro: plan

Små, sällsynta saker som gör världen levande. Fokus på **väsen (entities)** och **platser (structures/features)**.
Byggs ett steg i taget, som [underground-plan.md](underground-plan.md). Mörkrets väsen under jord står där, inte här.

**Regel:** magin syns lite, händer sällan och förklaras aldrig. Ger aldrig stor fördel, bara en liten gåva eller stämning.
Inget magiskt väsen lämnar Carcass och inget kan jagas för mat. De flesta går inte att skada alls, de försvinner.

## 0. Tro och Second Sight (grunden) — klart (v0.110.0)
> Byggt: `folklore/Faith.java` (attachments `faith` och `folk`, `sight()`, trösklar `STUMP_SIGHT` 10 / `WATCHER_SIGHT` 40,
> `/faith`), effekten `mechanical:second_sight` och Boiled Fly Agaric i `folklore/Folklore.java` (campfire 15 s, rå 20 s /
> kokt 3 min Second Sight), vinjett och gungning i `client/folklore/FolkloreClient.java`. Test: `FolkloreTest` + `Faith.selfCheck`.
Bara den som **tror** ser väsendena. Allt magiskt syns bara för spelare med tillräcklig *syn*.
- **Faith (tro)**: 0–100 per spelare, sparas, synkas till klienten, **behålls vid död**. Börjar på 0.
  Ökar: gåva vid en Glimmering Stump (+3, en gång per dag), första gången man ser ett nytt väsen (+5).
  Minskar: hugga ner en Glimmering Stump (−25).
- **Second Sight** (egen effekt): +50 syn medan den varar. Man blir "lite hög": svag färgad vinjett och lätt gungning.
- **Syn = Faith + 50 om Second Sight.** Varje väsen/glimmer har en tröskel: stubbens glimmer 10, Mountain Watcher 40.
- **Fly Agaric**: rå som nu (gift + illamående) plus 20 s Second Sight. **Boiled Fly Agaric** (kokas): inget gift, kort
  illamående, 3 min Second Sight. Svampen öppnar ögonen, gåvorna bygger tron så man till slut ser utan svamp.
- Den som inte ser: väsendet ritas inte, låter inte, kan inte träffas. Det finns ändå på servern.
- Advancement-flik **Folklore**, rot: ät Fly Agaric.

## 1. Vättestubbe + ekorrar — klart (v0.110.0)
> Byggt: `folklore/GlimmeringStumpBlock.java`, feature `worldgen/GlimmeringStumpFeature.java` (1 på 40 chunks i Dark Forest,
> Old Growth Birch/Pine/Spruce, Pale Garden, Deciduous Forest, plus en i varje Ancient Oak-glänta), gåvor i
> `Faith.offer`/`Faith.answerGift` (taggen `stump_offerings`, loot `gameplay/stump_gift`), `Squirrel.GatherAtStumpGoal`
> (skymning 17:00–19:30, 16 block). Glimret är partiklar på klienten, inget riktigt ljus.
- **Block + feature `glimmering_stump`**: sällsynt mossig stubbe i gamla/mörka skogar och nära Ancient Oak. Syns för alla som
  en stubbe; glimret (partiklar, svagt sken på natten) bara för den med syn ≥ 10.
- **Ekorrar** (`Squirrel`) inom ~16 block söker sig dit i skymningen och sitter runt den. Det ser alla.
- Gåva: högerklick med bär eller nötter → nästa morgon ligger något litet ovanpå (frö, fjäder, vacker sten). En per spelare
  och dag, ger +3 Faith.
- Hugger man ner den: −25 Faith.
- Advancement *Small Folk* (lämna en gåva).

## 2. Mountain Watcher (fågel) — klart (v0.110.0)
> Byggt: `folklore/MountainWatcher.java` (ärver `SoaringBird`, tyst, osårbar, ej träffbar), golden eaglens modell 1,4× i
> `FolkloreClient`, spawn y ≥ 150 / klart / dag i bergsbiomer (vikt 2). Fjädern: inom 24 block, en per kalenderår och spelare.
> Den flyger fortfarande undan som andra fåglar (FlockBird, ~7–12 block), oavsett om man ser den.
- **Entity `mountain_watcher`**: stor, ljus rovfågel med svag glans. Spawnar bara över **y 150**, i klart väder, dagtid.
  Kretsar kring bergstoppar (ärver `SoaringBird` som `GoldenEagle`). Flyger iväg om spelaren kommer inom ~12 block.
- Syns bara med syn ≥ 40. Går inte att skada. Inget Carcass.
- En gång per år (kalendern) per spelare tappar den en **Watcher Feather** när man kommer nära. Trofé.
- Advancement *Above the Clouds* (se den), *Gift of the Peaks* (få fjädern).

## 3. Lyktgubbar
- **Entity `will_o_wisp`**: litet ljus, ingen modell, bara partiklar + ljus. Över myr (Peat, Bog Oak från fas 3) på höstnätter.
- Flyttar sig bort när man följer, alltid mot en **Bog Grave** (se nedan). Går inte att fånga.
- **Feature `bog_grave`**: liten sjunken grav i myren: Bog Oak-plankor, ben, en kista (`chests/bog_grave`) med gamla saker.
- Advancement *Follow the Light*.

## 4. Skogsrået
- **Entity `forest_maiden`**: kvinnofigur i bark och mossa, syns bara på 30–60 blocks håll mellan träden i skymning.
  Tonas bort när spelaren tittar på henne länge eller går närmare (renderer med alfa). Ingen AI utöver att stå still.
- Mycket sällsynt. Rent stämning. Advancement *Was Someone There?*

## 5. Älvdansen
- **Feature `fairy_ring`**: ring av svamp/vita blommor på äng, 7×7. Dimma (partiklar) över den i skymningen,
  starkast midsommarveckan.
- Sover man i ringen vaknar man en bit bort, eller ett dygn senare. Inget farligt, bara konstigt.
- Advancement *Taken by the Dance*.

## 6. Näcken
- **Feature/entity `nacken`**: vid forsar och strömmande vatten på natten hörs fiol (eget ljud). Krusningar på vattnet.
  Ingen synlig figur. Går man ut i vattnet där tystnar musiken.
- Advancement *Heard the Fiddle*.

## 7. Offerstenen
- **Feature `offering_stone`**: stor sten med skålgropar, ofta nära `StandingStones`/`BurialMound`/`RuneStone`.
- Lägg en gåva (kött, mjöd, bärnstensbit) → mindre risk för Blood Moon den månaden (`BloodMoon`). En gåva per månad.
- Advancement *An Offering*.

## 8. Tomten (senare)
- **Entity `tomte`**: liten gubbe i grå kläder, syns bara på avstånd vid stammens hus/ladugård på vinternätter.
- Gröt i en Bowl på julafton (kalendern) → djuren i byn mår bra (snabbare växt) vintern ut. Glöms gröten: små sabotage
  (en dörr öppen, ett djur utsläppt).

## Beslutat
- y 150 för Mountain Watcher.
- Väsen syns per spelare, efter syn (Faith + Second Sight).
- Textures och ljud görs av oss (genererade).
