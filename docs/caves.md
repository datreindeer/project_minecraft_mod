# Grottbiomer

Tolv grottbiomer under jord, byggda kring modens stenar och jordarter (plan: `docs/world-materials-plan.md`, faserna 5 och 8).
En grottbiom ändrar inte grottornas form, bara vad de är gjorda av och vad som finns i dem. Vilken grottbiom du hittar
beror på klimatet ovanför (samma temperatur, fuktighet och kontinentalitet som ytbiomen) och på hur djupt du är. Djupen
nedan är uppmätta i en riktig värld och ungefärliga: under berg ligger allt djupare, under havet grundare. Dripstone Caves, Lush Caves och Deep Dark
finns kvar som förut.

Varje grottbiom har en advancement i fliken **Wilderness & Hunting**. Grottbjörnar (Cave Bear) kan finnas i alla.

Teknik: biomerna är rader i `scripts/gen_world_biomes.py` (`BIOMES`, `MATERIALS`), deras features i `scripts/gen_caves.py`
och klimatplatserna i `ModBiomes.CAVES`.

---

## Grunda grottor (mest y 20–45)

### Ochre Caves
**Under:** varmt och torrt land (savann, badlands, öken).
**Väggar:** Chalk Block, med klumpar av Red Ochre Block och Yellow Ochre Block. Golvet är Red Ochre Block.
**Hittar du:** Loose Red Ochre och Loose Yellow Ochre på golvet, alltså pigment direkt. På väggarna finns gamla
**grottmålningar** (Cave Painting) i rött, gult och svart: djur, jägare, solar, spiraler och handavtryck. De är lagda av
världen och går att skrapa bort med en kniv som dina egna.
**Faror:** inga särskilda.

### Bone Caves
**Under:** svalt till tempererat, torrt land.
**Väggar:** Limestone, golv av Coarse Dirt, korta kalkstalaktiter i taket.
**Hittar du:** Bone Pile på golvet och Bone Block i väggarna.
**Faror:** det här är rovdjurens lyor. Cave Bear och Cave Hyena hör hemma här (Cave Lion är också listat, men lejonen
vill ha ljus för att dyka upp). Ta med spjut och fackla.

### Glowworm Caves
**Under:** fuktig skog i tempererat eller varmt klimat.
**Väggar:** vanlig sten; bara golvet (Moss Block) och taket (Rooted Dirt) ändras. Det är biomen som visar att golv- och
takreglerna fungerar utan `wall`.
**Hittar du:** **Glowworm** i taket: mörka trådar med blåa lysande pärlor. De ger svagt ljus (nivå 6), man går rakt
igenom dem och de ger inget när de går sönder. Dessutom Hanging Roots, mossa, gräs och azalea på golvet, och små lerpölar.
Det är det enda naturliga ljuset under jord i Stone Age.
**Faror:** få; ljuset räcker inte för att hålla monster borta.

### Ice Caves
**Under:** de frusna biomerna (snötäckt land, isslätter).
**Väggar:** Slate, golv och tak av Packed Ice. Blue Ice i klumpar i berget.
**Hittar du:** istappar (Ice) i taket, frusna pölar och snödrivor på golvet. Snöflingor i luften.
**Faror:** golvet är halt. Kylan djupt ned gäller som vanligt (fryst kött och kylrum kommer i fas 9).

### Peat Hollows
**Under:** svalt och blött land (myrar, kalla träsk, taiga).
**Väggar:** vanlig sten; golv och tak av Peat.
**Hittar du:** Bog Oak-rötter (stockar som hänger ned från taket), Hanging Roots, lera i berget och torvbruna vattenpölar.
Mörkt och fuktigt, med mycelsporer i luften.
**Faror:** inga särskilda.

---

## Under kust och hav

### Flooded Caves
**Under:** kuster och grunt hav, mest kring y −20 till 0.
**Väggar:** vanlig sten; golv av Gravel och tak av Clay.
**Hittar du:** små sjöar med lerkant, vattenpölar överallt, lera i berget. Mörkt grönblått vatten.
Pike kan komma upp där vattnet ligger nära havsytan.
**Faror:** dykning: långa vattenfyllda gångar.

---

## Mellandjup (mest y −20 till 10)

### Limestone Caverns
**Under:** tempererat land en bit från kusten.
**Väggar:** Limestone, golv av Travertine.
**Hittar du:** travertinterrasser med klart vatten, tunna kalkstalaktiter och stalagmiter (Limestone Wall) och fler
Travertine-klumpar. Droppande ljud.
**Faror:** inga särskilda.

### Salt Caves
**Under:** hett och torrt land.
**Väggar:** Gypsum, golv och tak av Rock Salt.
**Hittar du:** saltdroppsten (Rock Salt) från tak och golv, gipsnålar (Gypsum Wall), Rock Salt- och Alabaster-klumpar i
väggarna. Ljust och vitt, med vitt damm i luften.
**Faror:** inga särskilda.

### Soapstone Grottos
**Under:** kallt och svalt land, mest i bergstrakter.
**Väggar:** Soapstone, golv av Slate och fler Slate-klumpar.
**Hittar du:** Glow Lichen och ibland ett **verktygsgömsle**: en kista med Stone Age-verktyg (flintverktyg, enkla
stenverktyg, Bone Needle) och flinta, stenbitar och Soapstone som någon lämnat.
**Faror:** inga särskilda.

---

## Djupt (y −20 och nedåt, mest kring y −40)

### Galena Seams
**Under:** kallt till tempererat land.
**Väggar:** Shale, tak av Alum Shale och stora Alum Shale-klumpar.
**Hittar du:** mycket Galena Ore (bly och lite silver i Iron Age).
**Faror:** mörkt och långt ned.

### Magma Vents
**Under:** varmt och hett land, djupt och ned till botten.
**Väggar:** Coastal Basalt, golv av Scoria.
**Hittar du:** Scoria, Pumice och Obsidian i väggarna, basaltpelare, Magma Block i golvet och lavasjöar. Aska i luften och
basaltdeltats ljud.
**Faror:** Magma Block bränner fötterna och lavan är överallt. (Värmeskada utan skydd kommer i fas 9.)

### Crystal Geodes
**Under:** tempererat land, sällsynt, djupt och ned till botten.
**Väggar:** Quartzite.
**Hittar du:** stora geoder av Quartzite och Calcite med kristaller av Clear Quartz, Rose Quartz eller Smoky Quartz inuti,
ibland en vanlig ametistgeod, och kristaller av Clear Quartz, Rose Quartz, Smoky Quartz, Citrine och Fluorite på golv och tak.
Klingande ljud.
**Faror:** djupt, nära Deep Dark.
