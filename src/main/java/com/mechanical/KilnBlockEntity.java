package com.mechanical;

import java.util.List;
import java.util.Optional;
import java.util.function.Predicate;
import java.util.function.Supplier;

import net.minecraft.core.BlockPos;
import net.minecraft.core.NonNullList;
import net.minecraft.core.particles.BlockParticleOption;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.core.component.DataComponents;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.tags.ItemTags;
import net.minecraft.world.ContainerHelper;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.ContainerData;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.entity.BaseContainerBlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.storage.ValueInput;
import net.minecraft.world.level.storage.ValueOutput;

/**
 * Clay kiln (lives on the LOWER half). Slots: 0 input, 1 fuel, 2 output.
 * Once lit (fire drill, or placed on a burning campfire) the fire burns fuel continuously, baking or not; when the
 * fuel runs out it goes out and must be lit again. Baking progress pauses (doesn't reset) while the fire is out.
 * <p>Heat (Bronze Age phase 2): the kiln has a temperature in degrees. Lit, it climbs {@link #RISE_PER_SEC}°/s toward
 * the burning fuel's natural max; out, it falls {@link #COOL_PER_SEC}°/s (faster in rain and cold). Each pump of a
 * {@link BellowsBlock} adds {@link #PUMP}° up to the fuel's bellows max; above the natural max it sinks
 * {@link #OVER_FALL_PER_SEC}°/s. Every recipe needs a temperature (hotter bakes faster, see {@link #speed}), and a new kiln's clay only fires at
 * {@link #FIRING_TEMP}° or more. Pumping at the {@link #CEILING} strains the clay; full strain cracks the kiln
 * (CRACKS 3), which then stays under {@link #CRACKED_MAX}° until patched with {@link #REPAIR_CLAY} Clay Balls and fired in again.
 */
public class KilnBlockEntity extends BaseContainerBlockEntity {
    public static final int INPUT = 0, FUEL = 1, OUTPUT = 2;
    /** The firewood of the campfire the kiln was built on: three firewood's worth. */
    public static final int CAMPFIRE_BURN_TICKS = 3600;
    /** With a Bronze Fire Grate the fuel skips burning one tick in this many (3: it lasts 1.5x as long). */
    public static final int GRATE_SKIP = 3;
    /** A new kiln's clay must burn this long (2 min, at {@link #FIRING_TEMP}° or more) before it can bake; it fires in 3 visible steps. */
    public static final int FIRE_TICKS = 2400;
    public static final int FIRING_TEMP = 500, CEILING = 1300, CRACKED_MAX = 600, HOT = 900, PUMP = 40;
    public static final float RISE_PER_SEC = 5, COOL_PER_SEC = 3, OVER_FALL_PER_SEC = 2;
    /** Strain from one pump at the ceiling, and how much strain cracks the kiln (it eases 1 per second). */
    public static final int STRAIN_PER_PUMP = 12, STRAIN_MAX = 100, REPAIR_CLAY = 4;
    /** Melting a crucible of ore, or re-melting one that set. */
    public static final int MELT_TICKS = 300, REMELT_TICKS = 200;
    /** Baking speeds up with heat: one extra speed per this many degrees over what the recipe needs, up to {@link #MAX_SPEED}. */
    public static final float DEGREES_PER_SPEED = 200, MAX_SPEED = 4;
    /** An Iron Bellows pumps {@link #PUMP} times this, and holds the heat over the natural max this long after a pump. */
    public static final int IRON_PUMP_FACTOR = 2, IRON_HOLD_TICKS = 1200;
    /** Game time until which an Iron Bellows holds the heat. */
    private long heldUntil;

    /** {@code count} input items are used up per result; bakes only at {@code temp}° or hotter. */
    public record Recipe(Predicate<ItemStack> input, int count, Supplier<Item> result, int ticks, int temp) {}
    /** Burns {@code ticks}; heats the kiln to {@code maxTemp}° by itself, {@code bellowsTemp}° with a bellows. */
    public record Fuel(Predicate<ItemStack> item, int ticks, int maxTemp, int bellowsTemp) {}

    /** One bake: what the input slot holds right now turns into {@code result}. */
    public static final class Bake {
        final int count, ticks, temp;
        final ItemStack result;

        Bake(int count, int ticks, int temp, ItemStack result) {
            this.count = count;
            this.ticks = ticks;
            this.temp = temp;
            this.result = result;
        }
    }

    // shortcut: hardcoded like the Tool Station recipes; move to datapack recipes when modpacks need to edit them.
    private static final net.minecraft.tags.TagKey<Item> ROUGH_LOGS = net.minecraft.tags.TagKey.create(net.minecraft.core.registries.Registries.ITEM,
            net.minecraft.resources.Identifier.fromNamespaceAndPath(Mechanical.MODID, "rough_logs"));
    public static final List<Recipe> RECIPES = List.of(
            new Recipe(s -> s.is(Items.CLAY_BALL), 1, () -> Items.BRICK, 400, 600),
            new Recipe(s -> s.is(EarthBlocks.KAOLIN.get().asItem()), 1, () -> StoneFamilies.block("kaolin_tiles").asItem(), 400, EarthBlocks.KAOLIN_TEMP),
            new Recipe(s -> s.is(ItemTags.LOGS_THAT_BURN), 1, () -> Items.CHARCOAL, 600, 400),
            new Recipe(s -> s.is(Mechanical.FIREWOOD_TAG), 2, () -> Items.CHARCOAL, 600, 400),
            new Recipe(s -> s.is(ROUGH_LOGS), 1, () -> Items.CHARCOAL, 600, 400),
            new Recipe(s -> s.is(Mechanical.UNFIRED_CRUCIBLE.get()), 1, () -> Mechanical.CRUCIBLE.get(), 800, 800),
            new Recipe(s -> s.is(Mechanical.UNFIRED_CLAY_BUCKET.get()), 1, () -> Mechanical.CLAY_BUCKET.get(), 600, 600),
            new Recipe(s -> s.is(Farming.DOUGH.get()), 1, () -> Items.BREAD, 400, 200),
            new Recipe(s -> s.is(Casting.UNFIRED_CLAY_TUYERE.get()), 1, () -> Casting.CLAY_TUYERE.get(), 400, 600),
            new Recipe(s -> s.is(Casting.UNFIRED_CLAY_INGOT_MOLD.get()), 1, () -> Casting.CLAY_INGOT_MOLD_ITEM.get(), 400, 600),
            new Recipe(s -> s.is(Casting.UNFIRED_CLAY_PICKAXE_HEAD_MOLD.get()), 1, () -> Casting.CLAY_PICKAXE_HEAD_MOLD_ITEM.get(), 400, 600),
            new Recipe(s -> s.is(Casting.UNFIRED_CLAY_AXE_HEAD_MOLD.get()), 1, () -> Casting.CLAY_AXE_HEAD_MOLD_ITEM.get(), 400, 600),
            new Recipe(s -> s.is(Casting.UNFIRED_CLAY_SHOVEL_HEAD_MOLD.get()), 1, () -> Casting.CLAY_SHOVEL_HEAD_MOLD_ITEM.get(), 400, 600),
            new Recipe(s -> s.is(Casting.UNFIRED_CLAY_HOE_HEAD_MOLD.get()), 1, () -> Casting.CLAY_HOE_HEAD_MOLD_ITEM.get(), 400, 600),
            new Recipe(s -> s.is(Casting.UNFIRED_CLAY_KNIFE_BLADE_MOLD.get()), 1, () -> Casting.CLAY_KNIFE_BLADE_MOLD_ITEM.get(), 400, 600),
            new Recipe(s -> s.is(Bronze.UNFIRED_CLAY_SAW_BLADE_MOLD.get()), 1, () -> Bronze.CLAY_SAW_BLADE_MOLD_ITEM.get(), 400, 600),
            new Recipe(s -> s.is(Bronze.UNFIRED_CLAY_PLATE_MOLD.get()), 1, () -> Bronze.CLAY_PLATE_MOLD_ITEM.get(), 400, 600),
            new Recipe(s -> s.is(Bronze.UNFIRED_CLAY_SPEAR_HEAD_MOLD.get()), 1, () -> Bronze.CLAY_SPEAR_HEAD_MOLD_ITEM.get(), 400, 600),
            new Recipe(s -> s.is(Bronze.UNFIRED_CLAY_SWORD_BLADE_MOLD.get()), 1, () -> Bronze.CLAY_SWORD_BLADE_MOLD_ITEM.get(), 400, 600),
            new Recipe(s -> s.is(Bronze.UNFIRED_CLAY_KETTLE_MOLD.get()), 1, () -> Bronze.CLAY_KETTLE_MOLD_ITEM.get(), 400, 600),
            new Recipe(s -> s.is(Bronze.UNFIRED_CLAY_ARROWHEADS_MOLD.get()), 1, () -> Bronze.CLAY_ARROWHEADS_MOLD_ITEM.get(), 400, 600),
            new Recipe(s -> s.is(BronzeTools.UNFIRED_CLAY_BUCKET_MOLD.get()), 1, () -> BronzeTools.CLAY_BUCKET_MOLD_ITEM.get(), 400, 600),
            new Recipe(s -> s.is(BronzeTools.UNFIRED_CLAY_SICKLE_BLADE_MOLD.get()), 1, () -> BronzeTools.CLAY_SICKLE_BLADE_MOLD_ITEM.get(), 400, 600),
            new Recipe(s -> s.is(BronzeTools.UNFIRED_CLAY_SHEARS_MOLD.get()), 1, () -> BronzeTools.CLAY_SHEARS_MOLD_ITEM.get(), 400, 600),
            new Recipe(s -> s.is(BronzeTools.UNFIRED_CLAY_FISH_HOOKS_MOLD.get()), 1, () -> BronzeTools.CLAY_FISH_HOOKS_MOLD_ITEM.get(), 400, 600),
            new Recipe(s -> s.is(BronzeTools.UNFIRED_CLAY_NEEDLES_MOLD.get()), 1, () -> BronzeTools.CLAY_NEEDLES_MOLD_ITEM.get(), 400, 600),
            new Recipe(s -> s.is(BronzeTools.UNFIRED_CLAY_TONGS_MOLD.get()), 1, () -> BronzeTools.CLAY_TONGS_MOLD_ITEM.get(), 400, 600),
            new Recipe(s -> s.is(BronzeTools.UNFIRED_CLAY_CHISEL_BLADE_MOLD.get()), 1, () -> BronzeTools.CLAY_CHISEL_BLADE_MOLD_ITEM.get(), 400, 600),
            new Recipe(s -> s.is(Pottery.UNFIRED_STORAGE_JAR.get()), 1, () -> Pottery.STORAGE_JAR_ITEM.get(), 600, 600),
            new Recipe(s -> s.is(Pottery.UNFIRED_WATER_JUG.get()), 1, () -> Pottery.WATER_JUG_ITEM.get(), 600, 600),
            new Recipe(s -> s.is(Pottery.UNFIRED_FLOWER_POT.get()), 1, () -> Items.FLOWER_POT, 400, 600),
            new Recipe(s -> s.is(Pottery.UNFIRED_DECORATED_POT.get()), 1, () -> Items.DECORATED_POT, 600, 600),
            new Recipe(s -> s.is(Smelting.UNFIRED_FURNACE_BRICK.get()), 1, () -> Smelting.FURNACE_BRICK.get(), 200, 600),
            new Recipe(s -> s.is(Smelting.UNFIRED_CLAY_CHANNEL.get()), 1, () -> Smelting.CLAY_CHANNEL_ITEM.get(), 400, 600),
            new Recipe(s -> s.is(Ironworking.FIRE_CLAY.get()), 1, () -> Ironworking.FIRE_BRICK.get(), 400, 1100),
            // stone chemistry (Chemistry): alum shale roasted for leaching, gypsum burnt to Plaster of Paris
            new Recipe(s -> s.is(StoneFamilies.block("alum_shale").asItem()), 1, () -> Chemistry.ROASTED_ALUM_SHALE.get(), 400, Chemistry.ALUM_ROAST_TEMP),
            new Recipe(s -> s.is(Chemistry.GYPSUM_POWDER.get()), 1, () -> Chemistry.PLASTER_OF_PARIS.get(), 300, Chemistry.PLASTER_TEMP),
            // roof tiles (Masonry): fired hotter than a brick
            new Recipe(s -> s.is(Masonry.UNFIRED_ROOF_TILE.get()), 1, () -> Masonry.FIRED_ROOF_TILE.get(), 400, Masonry.ROOF_TILE_TEMP),
            // Iron Age ores (IronOres): lead, cupellation (silver), gold
            new Recipe(s -> s.is(IronOres.RAW_GALENA.get()), 1, () -> IronOres.LEAD_INGOT.get(), 400, IronOres.LEAD_TEMP),
            new Recipe(s -> s.is(IronOres.LEADED_CUPEL.get()), 1, () -> IronOres.SILVER_NUGGET.get(), 600, IronOres.CUPEL_TEMP),
            new Recipe(s -> s.is(Items.RAW_GOLD), 1, () -> Items.GOLD_INGOT, 400, IronOres.GOLD_TEMP),
            new Recipe(s -> s.is(Items.GOLD_NUGGET), 9, () -> Items.GOLD_INGOT, 400, IronOres.GOLD_TEMP),
            new Recipe(s -> s.is(Ironworking.UNFIRED_CLAY_HAMMER_HEAD_MOLD.get()), 1, () -> Ironworking.CLAY_HAMMER_HEAD_MOLD_ITEM.get(), 400, 600),
            new Recipe(s -> s.is(Ironworking.UNFIRED_CLAY_ANVIL_MOLD.get()), 1, () -> Ironworking.CLAY_ANVIL_MOLD_ITEM.get(), 600, 600),
            // a cooled Iron Bloom back to forging heat (BloomeryBlockEntity.FORGE_TEMP); it keeps its blows
            new Recipe(s -> s.is(Ironworking.IRON_BLOOM.get()), 1, () -> Ironworking.HOT_IRON_BLOOM.get(), 200, 1100),
            // iron and steel to forging heat for the anvil (Smithing)
            new Recipe(s -> s.is(Ironworking.WROUGHT_IRON_INGOT.get()), 1, () -> Smithing.HOT_WROUGHT_IRON_INGOT.get(), 200, 1100),
            new Recipe(s -> s.is(Smithing.BLISTER_STEEL.get()), 1, () -> Smithing.HOT_BLISTER_STEEL.get(), 200, 1100),
            new Recipe(s -> s.is(Smithing.STEEL_INGOT.get()), 1, () -> Smithing.HOT_STEEL_INGOT.get(), 200, 1100),
            // cementation (Smithing): the fire-clay pot fired, then a packed pot held at 1000°
            new Recipe(s -> s.is(Smithing.UNFIRED_CEMENTATION_POT.get()), 1, () -> Smithing.CEMENTATION_POT.get(), 400, 1100),
            // lime burning (IronOres): a lime stone block, or 4 Chalk, -> Quicklime
            new Recipe(IronOres::limeStone, 1, () -> IronOres.QUICKLIME.get(), 400, IronOres.LIME_TEMP),
            new Recipe(s -> s.is(Pigments.CHALK.get()), 4, () -> IronOres.QUICKLIME.get(), 400, IronOres.LIME_TEMP));

    // Our own fuel list (vanilla's fuel component needs a loot context); controls what burns in the stone age.
    public static final List<Fuel> FUELS = List.of(
            new Fuel(s -> s.is(Items.STICK) || s.is(ItemTags.SAPLINGS), 200, 500, 700),       // 10 s
            new Fuel(s -> s.is(Mechanical.FIREWOOD_TAG), 1200, 650, 900),                       // 1 min
            new Fuel(s -> s.is(ItemTags.PLANKS), 600, 500, 700),                                // 30 s
            new Fuel(s -> s.is(ItemTags.LOGS_THAT_BURN), 2400, 650, 900),                       // 2 min, as its 2 firewood
            new Fuel(EarthBlocks::isPeat, 3600, 600, 850),                                     // 3 min, slow and low (Peat)
            new Fuel(s -> s.is(EarthBlocks.DRIED_PEAT.get()), 1800, 650, 900),                 // 1.5 min, dried: hot as firewood (4 per Peat)
            new Fuel(s -> s.is(Items.CHARCOAL) || s.is(Items.COAL), 4800, 800, 1300));          // 4 min

    private NonNullList<ItemStack> items = NonNullList.withSize(3, ItemStack.EMPTY);
    private int progress, total, burn, burnTotal, fireTicks;
    /** Fraction of a progress tick carried over when baking faster than 1x. */
    private float partial;
    /** Degrees; NaN until the first tick sets it to the air temperature. */
    private float temp = Float.NaN, ambient = 20;
    private int maxTemp = 650, bellowsTemp = 900, strain, repair, needTemp;

    /** Synced to the menu: 0 progress, 1 total, 2 burn left, 3 burn total, 4 temperature, 5 strain, 6 cracked, 7 temperature the bake needs. */
    final ContainerData data = new ContainerData() {
        @Override
        public int get(int id) {
            return switch (id) {
                // while the clay is still firing the arrow shows the firing instead
                case 0 -> isFired() ? progress : fireTicks;
                case 1 -> isFired() ? total : FIRE_TICKS;
                case 2 -> burn;
                case 3 -> isLit() ? burnTotal : 0; // the screen shows the flame only while lit
                case 4 -> Math.round(temperature());
                case 5 -> strain;
                case 6 -> isCracked() ? 1 : 0;
                default -> isFired() ? needTemp : FIRING_TEMP;
            };
        }

        @Override
        public void set(int id, int value) {}

        @Override
        public int getCount() {
            return 8;
        }
    };

    public KilnBlockEntity(BlockPos pos, BlockState state) {
        super(Mechanical.KILN_ENTITY.get(), pos, state);
    }

    public static Optional<Recipe> recipeFor(ItemStack stack) {
        return RECIPES.stream().filter(r -> r.input().test(stack)).findFirst();
    }

    /** What the input stack bakes into: a Recipe, or a Crucible of ore / Solid Metal melting (at the metal's melting point). */
    public static Bake bakeFor(ItemStack stack) {
        if (stack.getItem() instanceof CrucibleItem crucible && crucible.kind() != CrucibleItem.Kind.MOLTEN) {
            CrucibleContents contents = CrucibleItem.contents(stack);
            Metal metal = contents.alloy();
            if (metal == null) return null;
            return new Bake(1, crucible.kind() == CrucibleItem.Kind.ORE ? MELT_TICKS : REMELT_TICKS, metal.melt, CrucibleItem.molten(contents.melted(), 0));
        }
        return recipeFor(stack).map(r -> {
            ItemStack out = new ItemStack(r.result().get());
            // sherds pressed into an Unfired Decorated Pot stay on the fired pot
            if (stack.has(DataComponents.POT_DECORATIONS)) out.set(DataComponents.POT_DECORATIONS, stack.get(DataComponents.POT_DECORATIONS));
            if (stack.has(Ironworking.BLOOM_HITS.get())) out.set(Ironworking.BLOOM_HITS.get(), stack.get(Ironworking.BLOOM_HITS.get()));
            return new Bake(r.count(), r.ticks(), r.temp(), out);
        }).orElse(null);
    }

    public static boolean canBake(ItemStack stack) {
        return bakeFor(stack) != null;
    }

    public static int burnTicks(ItemStack stack) {
        return fuelFor(stack).map(Fuel::ticks).orElse(0);
    }

    static Optional<Fuel> fuelFor(ItemStack stack) {
        return FUELS.stream().filter(f -> f.item().test(stack)).findFirst();
    }

    public boolean isFired() {
        return getBlockState().getValue(KilnBlock.FIRING) >= KilnBlock.FIRED;
    }

    public boolean isLit() {
        return getBlockState().getValue(KilnBlock.LIT);
    }

    public boolean isCracked() {
        return getBlockState().getValue(KilnBlock.CRACKS) >= KilnBlock.CRACKED;
    }

    public float temperature() {
        return Float.isNaN(temp) ? ambient : temp;
    }

    /** The campfire's own firewood. */
    public void startBurning(int ticks) {
        burn = burnTotal = ticks;
        maxTemp = 650;
        bellowsTemp = 900;
        setLit(true);
        setChanged();
    }

    private void takeFuel(ItemStack fuel) {
        Fuel f = fuelFor(fuel).orElseThrow();
        burn = burnTotal = f.ticks();
        maxTemp = f.maxTemp();
        bellowsTemp = f.bellowsTemp();
        fuel.shrink(1);
    }

    /** There is something to catch: fuel left from before it went out, or fuel in the slot. */
    public boolean canLight() {
        return burn > 0 || burnTicks(items.get(FUEL)) > 0;
    }

    /** Fire drill: lights the kiln, finishing any partly burnt fuel first. Returns false if there's nothing to burn. */
    public boolean tryLight() {
        if (isLit()) return true;
        if (burn > 0) {
            setLit(true);
            return true;
        }
        if (burnTicks(items.get(FUEL)) == 0) return false;
        takeFuel(items.get(FUEL));
        setLit(true);
        setChanged();
        return true;
    }

    /** Put out (a zombie horde smothering it). The fuel left stays for the next lighting. */
    public void extinguish() {
        setLit(false);
    }

    /**
     * One pump of the bellows: +{@link #PUMP}° up to the fuel's bellows max. Pumping when it is already at the
     * {@link #CEILING} strains the clay, and full strain cracks it. False if the fire is out (nothing happens).
     */
    public boolean pump(ServerLevel level) {
        return pump(level, false);
    }

    /** One pump of a bellows; an Iron Bellows ({@code iron}) pumps twice as hard and holds the heat. */
    public boolean pump(ServerLevel level, boolean iron) {
        if (!isLit()) return false;
        if (iron) heldUntil = level.getGameTime() + IRON_HOLD_TICKS;
        float t = temperature();
        if (t + PUMP / 2.0F >= CEILING) {
            strain = Math.min(STRAIN_MAX, strain + STRAIN_PER_PUMP);
            level.playSound(null, worldPosition, SoundEvents.DECORATED_POT_HIT, SoundSource.BLOCKS, 0.9F, 0.5F);
            level.playSound(null, worldPosition, SoundEvents.CAMPFIRE_CRACKLE, SoundSource.BLOCKS, 1.5F, 0.6F);
            if (strain >= STRAIN_MAX) {
                crack(level);
                return true;
            }
        }
        float max = Math.min(bellowsTemp, isCracked() ? CRACKED_MAX : CEILING);
        if (t < max) temp = Math.min(max, t + PUMP * (iron ? IRON_PUMP_FACTOR : 1));
        updateBlock();
        setChanged();
        return true;
    }

    /** Overheated: the fire dies, molten metal in the kiln is lost (the crucible survives), the kiln is Cracked. */
    private void crack(ServerLevel level) {
        strain = 0;
        repair = 0;
        if (items.get(OUTPUT).is(Casting.MOLTEN_CRUCIBLE.get())) items.set(OUTPUT, new ItemStack(Mechanical.CRUCIBLE.get()));
        double x = worldPosition.getX() + 0.5, y = worldPosition.getY() + 1.0, z = worldPosition.getZ() + 0.5;
        level.playSound(null, worldPosition, SoundEvents.DECORATED_POT_SHATTER, SoundSource.BLOCKS, 1.2F, 0.6F);
        level.playSound(null, worldPosition, SoundEvents.FIRE_EXTINGUISH, SoundSource.BLOCKS, 1.0F, 0.8F);
        level.sendParticles(new BlockParticleOption(ParticleTypes.BLOCK, getBlockState()), x, y, z, 30, 0.4, 0.6, 0.4, 0.0);
        level.sendParticles(ParticleTypes.LARGE_SMOKE, x, y + 0.2, z, 12, 0.3, 0.5, 0.3, 0.02);
        level.setBlock(worldPosition, getBlockState().setValue(KilnBlock.CRACKS, KilnBlock.CRACKED).setValue(KilnBlock.LIT, false)
                .setValue(KilnBlock.ACTIVE, false).setValue(KilnBlock.HOT, false), Block.UPDATE_ALL);
        setChanged();
    }

    /**
     * A Clay Ball pressed into a cracked kiln. The {@link #REPAIR_CLAY}th seals it; the patch then has to be fired in
     * like a new kiln's clay, from the last firing step. Returns how many balls are in so far.
     */
    public int patch() {
        if (++repair < REPAIR_CLAY) {
            setChanged();
            return repair;
        }
        repair = 0;
        fireTicks = 0;
        BlockState state = getBlockState();
        if (level != null) level.setBlock(worldPosition, state.setValue(KilnBlock.CRACKS, 0)
                .setValue(KilnBlock.FIRING, Math.min(state.getValue(KilnBlock.FIRING), KilnBlock.FIRED - 1)), Block.UPDATE_ALL);
        setChanged();
        return REPAIR_CLAY;
    }

    private void setLit(boolean lit) {
        setState(lit, lit && getBlockState().getValue(KilnBlock.ACTIVE));
    }

    private void updateBlock() {
        setState(isLit(), getBlockState().getValue(KilnBlock.ACTIVE));
    }

    /** LIT, ACTIVE, HOT and CRACKS live on the block state (model, light, particles); the upper half follows through updateShape. */
    private void setState(boolean lit, boolean active) {
        Level level = getLevel();
        BlockState state = getBlockState();
        boolean hot = lit && temperature() >= HOT;
        int cracks = isCracked() ? KilnBlock.CRACKED : Math.min(KilnBlock.CRACKED - 1, strain * KilnBlock.CRACKED / STRAIN_MAX);
        if (level == null || (state.getValue(KilnBlock.LIT) == lit && state.getValue(KilnBlock.ACTIVE) == active
                && state.getValue(KilnBlock.HOT) == hot && state.getValue(KilnBlock.CRACKS) == cracks)) return;
        level.setBlock(getBlockPos(), state.setValue(KilnBlock.LIT, lit).setValue(KilnBlock.ACTIVE, active).setValue(KilnBlock.HOT, hot)
                .setValue(KilnBlock.CRACKS, cracks), Block.UPDATE_ALL);
    }

    /** One tick of heating or cooling. */
    private void heat(Level level) {
        temp = Math.min(heatStep(temperature(), isLit(), maxTemp, ambient, level.isRainingAt(worldPosition.above(2)), level.getGameTime() < heldUntil), isCracked() ? CRACKED_MAX : CEILING);
    }

    /**
     * One tick of heat, also for the Smelting Furnace: lit, toward the fuel's natural max ({@link #RISE_PER_SEC} up,
     * {@link #OVER_FALL_PER_SEC} down from above it); out, {@link #COOL_PER_SEC} toward the air (x2 in rain, x1.5 when cold).
     */
    static float heatStep(float t, boolean lit, int maxTemp, float ambient, boolean rain) {
        return heatStep(t, lit, maxTemp, ambient, rain, false);
    }

    /** As above; {@code held} (an Iron Bellows pumped lately, {@link #IRON_HOLD_TICKS}): heat over the natural max doesn't sink. */
    static float heatStep(float t, boolean lit, int maxTemp, float ambient, boolean rain, boolean held) {
        float sec = 1 / 20.0F;
        if (lit) {
            float natural = Math.max(ambient, maxTemp);
            if (t < natural) return Math.min(natural, t + RISE_PER_SEC * sec);
            return t > natural && !held ? Math.max(natural, t - OVER_FALL_PER_SEC * sec) : t;
        }
        if (t > ambient) {
            float rate = COOL_PER_SEC;
            if (rain) rate *= 2;
            if (ambient < 5) rate *= 1.5F;
            return Math.max(ambient, t - rate * sec);
        }
        return Math.min(ambient, t + sec);
    }

    public static void serverTick(ServerLevel level, BlockPos pos, BlockState state, KilnBlockEntity kiln) {
        if (Float.isNaN(kiln.temp) || level.getGameTime() % 20 == 0) {
            kiln.ambient = Temperature.ambient(level, pos) * 1.5F; // body scale (plains ~12) -> roughly degrees
            if (Float.isNaN(kiln.temp)) kiln.temp = kiln.ambient;
            if (kiln.strain > 0) kiln.strain--;
        }
        ItemStack input = kiln.items.get(INPUT);
        Bake bake = bakeFor(input);
        boolean firing = !kiln.isFired();
        boolean canBake = !firing && bake != null && input.getCount() >= bake.count && kiln.canOutput(bake.result);
        boolean working = canBake || firing; // firing the clay burns fuel like baking does; a kiln still too cold keeps heating
        kiln.needTemp = canBake ? bake.temp : 0;

        // A lit kiln burns its fuel whether or not anything bakes (so it can be heated up first, and keeps molten metal
        // hot in the output); it goes out only when the fuel runs out.
        if (kiln.isLit()) NightDangers.markLit(level, pos);
        if (kiln.isLit()) {
            // hotter fuel put in catches at once: it joins the fire (what's left of the old fuel keeps burning with it)
            Fuel next = fuelFor(kiln.items.get(FUEL)).orElse(null);
            if (kiln.burn > 0 && next != null && next.maxTemp() > kiln.maxTemp) {
                int left = kiln.burn;
                kiln.takeFuel(kiln.items.get(FUEL));
                kiln.burn = kiln.burnTotal = kiln.burn + left;
            }
            if (kiln.burn <= 0) {
                if (burnTicks(kiln.items.get(FUEL)) > 0) {
                    kiln.takeFuel(kiln.items.get(FUEL));
                } else {
                    kiln.burnTotal = 0;
                    kiln.setLit(false);
                }
            }
            // a Bronze Fire Grate skips every GRATE_SKIP-th tick: fuel lasts 1.5x as long
            boolean saved = state.getValue(KilnBlock.GRATE) && level.getGameTime() % GRATE_SKIP == 0;
            if (kiln.burn > 0 && !saved) kiln.burn = Math.max(0, kiln.burn - Temperature.fireDrain(level, pos, level.getRandom()));
        }
        kiln.heat(level);
        kiln.keepMoltenHot(level);
        kiln.setChanged();
        if (firing && kiln.isLit() && kiln.temp >= FIRING_TEMP) kiln.fire(state);
        kiln.setState(kiln.isLit(), kiln.isLit() && working);

        if (!canBake) {
            kiln.progress = 0;
            kiln.total = 0;
            return;
        }
        kiln.total = bake.ticks;
        if (!kiln.isLit() || kiln.temp < bake.temp) return; // too cold: progress waits
        kiln.partial += speed(kiln.temp, bake.temp);
        int steps = (int) kiln.partial;
        kiln.partial -= steps;
        kiln.progress += steps;
        if (kiln.progress >= kiln.total) {
            kiln.progress = 0;
            kiln.partial = 0;
            ItemStack was = input.copyWithCount(1);
            input.shrink(bake.count);
            ItemStack out = kiln.items.get(OUTPUT);
            ItemStack result = bake.result;
            if (result.has(Casting.COOLS_AT.get())) result.set(Casting.COOLS_AT.get(), level.getGameTime() + CrucibleItem.COOL_SECONDS * 20L);
            if (Ironworking.hotItem(result)) result = Ironworking.heated(was, result.getItem(), level.getGameTime()); // keeps its blows
            if (out.isEmpty()) kiln.items.set(OUTPUT, result);
            else out.grow(1);
        }
    }

    /** How many times faster than its base time a bake goes at {@code temp}° when it needs {@code need}°. */
    public static float speed(float temp, int need) {
        return Math.clamp(1 + (temp - need) / DEGREES_PER_SPEED, 1, MAX_SPEED);
    }

    /** Molten metal in the output slot stays molten while the kiln is at its melting point (a hot bloom at forging heat). */
    private void keepMoltenHot(ServerLevel level) {
        ItemStack out = items.get(OUTPUT);
        if (Ironworking.hotItem(out) && temp >= BloomeryBlockEntity.FORGE_TEMP && level.getGameTime() % 20 == 0) {
            out.set(Casting.COOLS_AT.get(), level.getGameTime() + Ironworking.HOT_SECONDS * 20L);
            out.setDamageValue(0);
        }
        if (!out.is(Casting.MOLTEN_CRUCIBLE.get()) || level.getGameTime() % 20 != 0) return;
        Metal metal = CrucibleItem.contents(out).alloy();
        if (metal != null && temp >= metal.melt) {
            out.set(Casting.COOLS_AT.get(), level.getGameTime() + CrucibleItem.COOL_SECONDS * 20L);
            out.setDamageValue(0);
        }
    }

    /** One tick of firing the raw clay; the block's FIRING step follows (the upper half copies it). */
    private void fire(BlockState state) {
        int stage = state.getValue(KilnBlock.FIRING);
        fireTicks = Math.max(fireTicks, stage * FIRE_TICKS / KilnBlock.FIRED) + 1; // a part-fired kiln re-placed from its item
        int now = Math.min(KilnBlock.FIRED, fireTicks * KilnBlock.FIRED / FIRE_TICKS);
        if (now > stage && level != null) level.setBlock(worldPosition, getBlockState().setValue(KilnBlock.FIRING, now), Block.UPDATE_ALL);
        setChanged();
    }

    private boolean canOutput(ItemStack result) {
        ItemStack out = items.get(OUTPUT);
        return out.isEmpty() || (ItemStack.isSameItemSameComponents(out, result) && out.getCount() < out.getMaxStackSize());
    }

    @Override
    public boolean canPlaceItem(int slot, ItemStack stack) {
        return switch (slot) {
            case INPUT -> canBake(stack);
            case FUEL -> burnTicks(stack) > 0;
            default -> false;
        };
    }

    @Override
    protected void loadAdditional(ValueInput input) {
        super.loadAdditional(input);
        items = NonNullList.withSize(getContainerSize(), ItemStack.EMPTY);
        ContainerHelper.loadAllItems(input, items);
        progress = input.getIntOr("progress", 0);
        burn = input.getIntOr("burn", 0);
        burnTotal = input.getIntOr("burn_total", 0);
        fireTicks = input.getIntOr("fire_ticks", 0);
        temp = input.getFloatOr("temp", Float.NaN);
        maxTemp = input.getIntOr("max_temp", 650);
        bellowsTemp = input.getIntOr("bellows_temp", 900);
        strain = input.getIntOr("strain", 0);
        repair = input.getIntOr("repair", 0);
    }

    @Override
    protected void saveAdditional(ValueOutput output) {
        super.saveAdditional(output);
        ContainerHelper.saveAllItems(output, items);
        output.putInt("progress", progress);
        output.putInt("burn", burn);
        output.putInt("burn_total", burnTotal);
        output.putInt("fire_ticks", fireTicks);
        if (!Float.isNaN(temp)) output.putFloat("temp", temp);
        output.putInt("max_temp", maxTemp);
        output.putInt("bellows_temp", bellowsTemp);
        output.putInt("strain", strain);
        output.putInt("repair", repair);
    }

    @Override
    public int getContainerSize() {
        return 3;
    }

    @Override
    protected NonNullList<ItemStack> getItems() {
        return items;
    }

    @Override
    protected void setItems(NonNullList<ItemStack> items) {
        this.items = items;
    }

    @Override
    protected Component getDefaultName() {
        return Component.translatable("container.mechanical.kiln");
    }

    @Override
    protected AbstractContainerMenu createMenu(int containerId, Inventory inventory) {
        return new KilnMenu(containerId, inventory, this, data);
    }
}
