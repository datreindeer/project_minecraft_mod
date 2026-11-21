package com.mechanical;

import java.util.List;
import java.util.Optional;
import java.util.function.Predicate;
import java.util.function.Supplier;

import net.minecraft.core.BlockPos;
import net.minecraft.core.NonNullList;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
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
 * The Bloomery, on its hearth. Slots as the Clay Kiln's (0 input, 1 fuel, 2 output) and the same screen ({@link KilnMenu#bloomery}).
 * Heat works like the kiln's ({@link KilnBlockEntity#heatStep}, bellows +{@link KilnBlockEntity#PUMP}° a pump), but the
 * refractory shaft takes Charcoal with bellows up to {@link #CEILING}°; Raw Iron only reduces to a bloom at
 * {@link #SMELT_TEMP}°, above anything the kiln or the Smelting Furnace reach. Progress pauses (doesn't reset) while it is
 * too cold. The structure (hearth + two stacks + open top) is checked every second; a broken one goes out.
 */
public class BloomeryBlockEntity extends BaseContainerBlockEntity {
    public static final int INPUT = 0, FUEL = 1, OUTPUT = 2;
    public static final int CEILING = 1500, NATURAL_MAX = 800, SMELT_TEMP = 1350, FORGE_TEMP = 1100, SMELT_TICKS = 1200;
    /** Raw Iron per bloom. */
    public static final int ORE_PER_BLOOM = 4;
    /** Raw Bog Iron per bloom: a poorer ore than Raw Iron. */
    public static final int BOG_ORE_PER_BLOOM = 6;

    /** {@code count} input items make one result; only at {@code temp}° or hotter (hotter goes faster, {@link KilnBlockEntity#speed}); an ore bloom gives {@code yield} ingots (0: not an ore). */
    public record Recipe(Predicate<ItemStack> input, int count, Supplier<Item> result, int ticks, int temp, int yield) {}
    /** Burns {@code ticks}; heats to {@code maxTemp}° by itself, {@code bellowsTemp}° with bellows. */
    public record Fuel(Predicate<ItemStack> item, int ticks, int maxTemp, int bellowsTemp) {}

    // shortcut: hardcoded like the kiln's; move to datapack recipes when modpacks need to edit them.
    public static final List<Recipe> RECIPES = List.of(
            new Recipe(s -> s.is(Ironworking.ROASTED_IRON_ORE.get()), ORE_PER_BLOOM, () -> Ironworking.HOT_IRON_BLOOM.get(), SMELT_TICKS, SMELT_TEMP, Ironworking.ROASTED_YIELD),
            new Recipe(s -> s.is(Ironworking.ROASTED_BOG_IRON.get()), BOG_ORE_PER_BLOOM, () -> Ironworking.HOT_IRON_BLOOM.get(), SMELT_TICKS, SMELT_TEMP, Ironworking.ROASTED_YIELD),
            new Recipe(s -> s.is(Items.RAW_IRON), ORE_PER_BLOOM, () -> Ironworking.HOT_IRON_BLOOM.get(), SMELT_TICKS, SMELT_TEMP, Ironworking.RAW_YIELD),
            new Recipe(s -> s.is(Ironworking.RAW_BOG_IRON.get()), BOG_ORE_PER_BLOOM, () -> Ironworking.HOT_IRON_BLOOM.get(), SMELT_TICKS, SMELT_TEMP, Ironworking.RAW_YIELD),
            new Recipe(s -> s.is(Ironworking.IRON_BLOOM.get()), 1, () -> Ironworking.HOT_IRON_BLOOM.get(), 200, FORGE_TEMP, 0),
            // iron and steel to forging heat for the anvil (Smithing)
            new Recipe(s -> s.is(Ironworking.WROUGHT_IRON_INGOT.get()), 1, () -> Smithing.HOT_WROUGHT_IRON_INGOT.get(), 200, FORGE_TEMP, 0),
            new Recipe(s -> s.is(Smithing.BLISTER_STEEL.get()), 1, () -> Smithing.HOT_BLISTER_STEEL.get(), 200, FORGE_TEMP, 0),
            new Recipe(s -> s.is(Smithing.STEEL_INGOT.get()), 1, () -> Smithing.HOT_STEEL_INGOT.get(), 200, FORGE_TEMP, 0));
    // a bloomery eats charcoal: half as long a burn as in the kiln
    public static final List<Fuel> FUELS = List.of(
            new Fuel(s -> s.is(Items.CHARCOAL) || s.is(Items.COAL), 2400, NATURAL_MAX, CEILING));

    private NonNullList<ItemStack> items = NonNullList.withSize(3, ItemStack.EMPTY);
    private int progress, total, burn, burnTotal, needTemp;
    /** Charges of flux laid in the hearth: each makes the next bloom one ingot richer. */
    private int flux;
    private float partial, temp = Float.NaN, ambient = 20;
    /** Why it isn't a bloomery (null when whole); refreshed every second. */
    private Component problem;
    private boolean formed;

    /** Synced to the menu like the kiln's: 0 progress, 1 total, 2 burn left, 3 burn total, 4 temperature, 5-6 unused, 7 needed. */
    final ContainerData data = new ContainerData() {
        @Override
        public int get(int id) {
            return switch (id) {
                case 0 -> progress;
                case 1 -> total;
                case 2 -> burn;
                case 3 -> isLit() ? burnTotal : 0;
                case 4 -> Math.round(temperature());
                case 7 -> needTemp;
                default -> 0;
            };
        }

        @Override
        public void set(int id, int value) {}

        @Override
        public int getCount() {
            return 8;
        }
    };

    public BloomeryBlockEntity(BlockPos pos, BlockState state) {
        super(Ironworking.BLOOMERY_ENTITY.get(), pos, state);
    }

    static Optional<Recipe> recipeFor(ItemStack stack) {
        return RECIPES.stream().filter(r -> r.input().test(stack)).findFirst();
    }

    public static boolean canSmelt(ItemStack stack) {
        return recipeFor(stack).isPresent();
    }

    static Optional<Fuel> fuelFor(ItemStack stack) {
        return FUELS.stream().filter(f -> f.item().test(stack)).findFirst();
    }

    public static int burnTicks(ItemStack stack) {
        return fuelFor(stack).map(Fuel::ticks).orElse(0);
    }

    public boolean isLit() {
        return getBlockState().getValue(BloomeryHearthBlock.LIT);
    }

    public float temperature() {
        return Float.isNaN(temp) ? ambient : temp;
    }

    public int progress() {
        return progress;
    }

    /** Test hook: set the heat directly. */
    void forceTemperature(float degrees) {
        temp = degrees;
    }

    public Component problem() {
        return problem;
    }

    /**
     * Why this hearth isn't a bloomery, or null: two Bloomery Stacks on it, and nothing solid on top (the charge goes
     * in and the smoke comes out there; a third stack makes the shaft too tall).
     */
    public Component check(Level level) {
        BlockPos p = worldPosition;
        int stacks = 0;
        while (stacks < 3 && level.getBlockState(p.above(stacks + 1)).is(Ironworking.BLOOMERY_STACK.get())) stacks++;
        if (stacks < 2) return Component.translatable("message.mechanical.bloomery.stacks", 2 - stacks);
        if (stacks > 2) return Component.translatable("message.mechanical.bloomery.too_tall");
        BlockPos top = p.above(3);
        if (!level.getBlockState(top).getCollisionShape(level, top).isEmpty())
            return Component.translatable("message.mechanical.bloomery.closed", top.getX(), top.getY(), top.getZ());
        return null;
    }

    public boolean canLight() {
        return problem == null && (burn > 0 || burnTicks(items.get(FUEL)) > 0);
    }

    /** Fire Drill: lights it (a whole bloomery with fuel left or in the slot). */
    public boolean tryLight(ServerLevel level) {
        problem = check(level);
        if (isLit()) return true;
        if (!canLight()) return false;
        if (burn <= 0) takeFuel();
        setLit(level, true);
        setChanged();
        return true;
    }

    /** One pump of the bellows into the hearth: +{@link KilnBlockEntity#PUMP}° up to {@link #CEILING}. False if the fire is out. */
    public boolean pump() {
        if (!isLit()) return false;
        float t = temperature();
        if (t < CEILING) temp = Math.min(CEILING, t + KilnBlockEntity.PUMP);
        setChanged();
        return true;
    }

    private void takeFuel() {
        Fuel f = fuelFor(items.get(FUEL)).orElseThrow();
        burn = burnTotal = f.ticks();
        items.get(FUEL).shrink(1);
    }

    /** LIT on the hearth and on both stacks above it (the top one glows). */
    private void setLit(Level level, boolean lit) {
        if (getBlockState().getValue(BloomeryHearthBlock.LIT) != lit)
            level.setBlock(worldPosition, getBlockState().setValue(BloomeryHearthBlock.LIT, lit), Block.UPDATE_ALL);
        for (int i = 1; i <= 2; i++) {
            BlockPos at = worldPosition.above(i);
            BlockState s = level.getBlockState(at);
            if (s.is(Ironworking.BLOOMERY_STACK.get()) && s.getValue(BloomeryStackBlock.LIT) != lit)
                level.setBlock(at, s.setValue(BloomeryStackBlock.LIT, lit), Block.UPDATE_ALL);
        }
    }

    public static void serverTick(ServerLevel level, BloomeryBlockEntity b) {
        BlockPos pos = b.worldPosition;
        if (Float.isNaN(b.temp) || level.getGameTime() % 20 == 0) {
            b.ambient = Temperature.ambient(level, pos) * 1.5F; // as the kiln: body scale -> roughly degrees
            if (Float.isNaN(b.temp)) b.temp = b.ambient;
            b.problem = b.check(level);
            if (b.problem == null && !b.formed) Smelting.award(level, pos, Ironworking.BLOOMERY_ADVANCEMENT, 12);
            b.formed = b.problem == null;
        }
        boolean lit = b.isLit();
        if (lit && b.problem != null) lit = false; // a broken shaft goes out; the fuel left stays
        if (lit) {
            NightDangers.markLit(level, pos);
            if (b.burn <= 0) {
                if (burnTicks(b.items.get(FUEL)) > 0) b.takeFuel();
                else {
                    b.burnTotal = 0;
                    lit = false;
                }
            }
            if (b.burn > 0) b.burn = Math.max(0, b.burn - Temperature.fireDrain(level, pos, level.getRandom()));
        }
        // natural max from Charcoal; over it (after pumping) it sinks back, out it cools to the air
        b.temp = Math.min(KilnBlockEntity.heatStep(b.temperature(), lit, NATURAL_MAX, b.ambient, level.isRainingAt(pos.above(3))), CEILING);
        b.setLit(level, lit);

        // the bloom waiting in the output stays hot while the bloomery has forging heat
        ItemStack out = b.items.get(OUTPUT);
        if (Ironworking.hotItem(out) && b.temp >= FORGE_TEMP && level.getGameTime() % 20 == 0) {
            out.set(Casting.COOLS_AT.get(), level.getGameTime() + Ironworking.HOT_SECONDS * 20L);
            out.setDamageValue(0);
        }
        b.setChanged();

        ItemStack input = b.items.get(INPUT);
        Recipe r = recipeFor(input).orElse(null);
        boolean canSmelt = r != null && input.getCount() >= r.count() && out.isEmpty();
        b.needTemp = canSmelt ? r.temp() : 0;
        if (!canSmelt) {
            b.progress = 0;
            b.total = 0;
            return;
        }
        b.total = r.ticks();
        if (!lit || b.temp < r.temp()) return; // too cold: progress waits
        b.partial += KilnBlockEntity.speed(b.temp, r.temp());
        int steps = (int) b.partial;
        b.partial -= steps;
        b.progress += steps;
        if (b.progress < b.total) return;
        b.progress = 0;
        b.partial = 0;
        ItemStack bloom = Ironworking.heated(input, r.result().get(), level.getGameTime()); // a reheated bloom or ingot keeps its blows
        if (r.yield() > 0) {
            int extra = b.flux > 0 ? 1 : 0;
            b.flux -= extra;
            bloom.set(Ironworking.BLOOM_YIELD.get(), Math.min(Ironworking.BLOOM_PARTS, r.yield() + extra));
        }
        b.items.set(OUTPUT, bloom);
        input.shrink(r.count());
        level.playSound(null, pos, net.minecraft.sounds.SoundEvents.LAVA_POP, net.minecraft.sounds.SoundSource.BLOCKS, 1.0F, 0.7F);
    }

    @Override
    public boolean canPlaceItem(int slot, ItemStack stack) {
        return switch (slot) {
            case INPUT -> canSmelt(stack);
            case FUEL -> burnTicks(stack) > 0;
            default -> false;
        };
    }

    public int flux() {
        return flux;
    }

    /** One charge of flux into the hearth; false when it holds {@link Ironworking#MAX_FLUX} already. */
    public boolean addFlux() {
        if (flux >= Ironworking.MAX_FLUX) return false;
        flux++;
        setChanged();
        return true;
    }

    @Override
    protected void loadAdditional(ValueInput input) {
        super.loadAdditional(input);
        items = NonNullList.withSize(getContainerSize(), ItemStack.EMPTY);
        ContainerHelper.loadAllItems(input, items);
        progress = input.getIntOr("progress", 0);
        burn = input.getIntOr("burn", 0);
        burnTotal = input.getIntOr("burn_total", 0);
        flux = input.getIntOr("flux", 0);
        temp = input.getFloatOr("temp", Float.NaN);
    }

    @Override
    protected void saveAdditional(ValueOutput output) {
        super.saveAdditional(output);
        ContainerHelper.saveAllItems(output, items);
        output.putInt("progress", progress);
        output.putInt("burn", burn);
        output.putInt("burn_total", burnTotal);
        if (flux > 0) output.putInt("flux", flux);
        if (!Float.isNaN(temp)) output.putFloat("temp", temp);
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
        return Component.translatable("container.mechanical.bloomery");
    }

    @Override
    protected AbstractContainerMenu createMenu(int containerId, Inventory inventory) {
        return KilnMenu.bloomery(containerId, inventory, this, data);
    }
}
