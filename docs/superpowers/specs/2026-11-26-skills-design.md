# Skills – design

Datum: 2026-11-26. Projekt 2 av 3. Användarens beslut: `docs/skills-and-tools-plan.md`. Användaren bad om att arbetet
inte skulle stanna, så allt märkt **(Claude)** är mitt förslag och kan rättas i efterhand.

## Varför

Spelaren ska märka att den blir bättre på det den gör. Levels begränsar ingenting, de ger förmåner. Ringar och halsband
kan höja en skill.

## Mål

- 12 skills: Smithing, Mining, Farming, Fishing, Slaughter, Woodcutting, Gold Smithing, Cooking, Hunting, Masonry,
  Leatherworking, Fletching.
- Level 1–100, XP genom att göra saken, inget tappas vid död, ingen skillcape, vanillas XP-mätare orörd.
- Förmåner per skill som låses upp vid vissa levels och växer med level.
- Smycken (de som redan finns: guld/silver/electrum ring och halsband med ädelsten) ger +levels i en skill.
- Skill-sida på **K** i quest-skärmens stil, och en liten popup när man får XP eller går upp en level.

## Icke-mål

- Ingen begränsning: inget recept eller verktyg kräver en level.
- Inga nya smyckesitem: de befintliga får skill-bonusen.
- Inga XP-boost-föremål, ingen XP-delning i stammen.

## Anti-mål

- Grind som känns som ett jobb (en skill ska nå 50 på en vanlig spelsäsong, 100 på mycket lång tid).
- En förmån som gör en tidig ålder trivial (t.ex. dubbla malmer vid level 10).

## Hur det byggs

### Kärna
- `Skill`-enum (12 värden, lang-nyckel `skill.mechanical.<namn>`, en ikon = ett befintligt item).
- **XP-kurva (Claude):** total XP för level L = `25·(L−1)² + 75·(L−1)`. Level 2 = 100 XP, 10 = 2 700, 50 = 63 700,
  100 = 252 450.
- Attachment `skills` (map skill → XP), sparas, `copyOnDeath`, synkas bara till ägaren.
- `Skills.addXp(ServerPlayer, Skill, int)`: lägger till, och vid ny level skickar en popup + ett ljud.
- **Effektiv level** = egen level + smyckesbonus, högst 120. Förmåner räknas på den effektiva leveln.

### XP per handling (Claude)
| Skill | Hook | XP |
|---|---|---|
| Mining | malm bruten (`#c:ores`, `BlockDropsEvent`) / sten bruten | 10 / 1 |
| Woodcutting | träd fällt (`TreeFelling.onBreak`), per stock | 3 per stock |
| Farming | mogen gröda skördad (`Farming.onDrops`, staked) | 7 (ingen XP för planterad) |
| Fishing | fångst (`ItemFishedEvent`) / fiskfälla tömd, per fisk | 15 / 5 |
| Slaughter | styckning (`Butchery.awardButchered`) | 20 |
| Hunting | vilt dödat (`LivingDeathEvent`, `Butchery.leavesCarcass`) | 25 |
| Smithing | smitt på städ (`BronzeAnvilBlockEntity.forge`, `strike`, `mint`, `smithGrate`) / Tool Station-uttag | 20 / 10 |
| Gold Smithing | smycke gjort (`JewelryTableBlockEntity.use`) / sten slipad (`LapidaryWheelBlock`) | 40 / 10 (flawless 30) |
| Cooking | mat tas ut ur cookware, Kiln, Fire Pit, Baking Stone | 5 per portion |
| Masonry | skrubbning/omfogning (`Masonry`), krossning (`StoneCrafts.crushedBy`) | 5 |
| Leatherworking | uttag ur Leatherworking-/Armor Station | 15 |
| Fletching | uttag ur Fletching Table | 8 |

### Förmåner (Claude)
En förmån har en upplåsningslevel och en effekt som växer med effektiv level. Alla chanser ska rullas på servern.
| Skill | Förmån |
|---|---|
| Mining | L10: chans till en extra malm = 0,4 %/level (40 % vid 100). L30: +0,3 %/level brytfart på sten och malm. L60: 25 % chans att pickaxen inte slits. |
| Woodcutting | L10: chans till en extra stock per fällt träd = 0,4 %/level. L30: +0,3 %/level brytfart på stockar. L60: 25 % chans att yxan inte slits. |
| Farming | L10: chans till en extra skörd = 0,4 %/level. L40: chans 0,2 %/level att en planterad gröda växer ett steg direkt. |
| Fishing | L10: chans till en extra fisk = 0,3 %/level. L50: fiskfällor ger en extra fisk med samma chans. |
| Slaughter | L10: chans till extra kött = 0,4 %/level. L50: chans till en extra hud = 0,2 %/level. |
| Hunting | L10: +0,2 %/level skada mot vilt. L40: 0,3 %/level chans att en avskjuten pil kan plockas upp igen i stället för att gå sönder (bara om pilar går sönder i modden; annars utgår den). |
| Smithing | L10: 0,3 %/level chans att hammaren inte slits. L50: 0,1 %/level chans att få tillbaka en ingot vid smide. |
| Gold Smithing | L10: +0,2 %/level chans till flawless vid slipning. L30: −0,3 %/level chans att stenen spricker. |
| Cooking | L10: chans till en extra portion = 0,3 %/level. |
| Masonry | L10: chans till ett extra stycke vid krossning = 0,4 %/level. |
| Leatherworking | L10: 0,3 %/level chans att en ingrediens sparas. |
| Fletching | L10: chans till extra pilar (+1) = 0,4 %/level. |

### Smycken (Claude)
- Varje ädelsten får en skill (tabellen nedan). Bonusen: guld +3, electrum +2, silver +1 levels, flawless dubbelt.
  Ringar och halsband staplar (två ringar + halsband). Den gamla attribut-bonusen finns kvar.
- Sten → skill: topaz, amethyst, fluorite → Mining; diamond → Smithing; jade, moonstone → Woodcutting;
  peridot, chalcedony → Farming; aquamarine, turquoise → Fishing; bloodstone, ruby → Slaughter;
  jasper, tourmaline, carnelian → Hunting; smoky_quartz → Cooking; agate, sapphire → Masonry;
  clear_quartz, opal, emerald → Gold Smithing; rose_quartz → Leatherworking; celestine → Fletching.
  Övriga stenar i `Jewelry.BONUSES` får byggaren lägga efter tema. Hela tabellen ligger på ett ställe.
- Tooltip på smycket: "+3 Mining".

### UI
- **K** öppnar skill-sidan: 12 rader (ikon, namn, level, XP-stapel, "+3" i guld om smycke höjer), till höger
  förmånerna för vald skill (upplåsta tända, låsta dämpade med "Level 30").
- Popup ovanför hotbaren: "+10 Mining" som tonar ut; vid ny level "Mining 11!" och ett ljud.

### Annat i samma gren
- Bone Needle-recepten ger 1 (en nål har durability och kan inte stapla, så "2" eller "3" gav ingenting).
- Quests i `assets/mechanical/quests.json`: första level-upp, level 50 i någon skill, level 100 i någon skill.
- Dokument: `docs/ages`-filen för järnåldern (smyckestabellen), `registry_notes` för smyckena, `progression.json`.

## Lämnat åt byggaren
Exakta layouter, popupens tid, ikonval, hur förmåner rullas i varje hook.
