package com.mechanical;

import java.util.ArrayList;
import java.util.List;

import net.minecraft.core.BlockPos;
import net.minecraft.core.HolderLookup;
import net.minecraft.core.NonNullList;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.network.protocol.Packet;
import net.minecraft.network.protocol.game.ClientGamePacketListener;
import net.minecraft.network.protocol.game.ClientboundBlockEntityDataPacket;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.Prediction;
import net.minecraft.util.RandomSource;
import net.minecraft.world.ContainerHelper;
import net.minecraft.world.Containers;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.crafting.CampfireCookingRecipe;
import net.minecraft.world.item.crafting.RecipeHolder;
import net.minecraft.world.item.crafting.RecipeManager;
import net.minecraft.world.item.crafting.RecipeType;
import net.minecraft.world.item.crafting.SingleRecipeInput;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.gameevent.GameEvent;
import net.minecraft.world.level.storage.ValueInput;
import net.minecraft.world.level.storage.ValueOutput;

/**
 * Fire and food of a Fire Pit (on its PART 0 corner). Burns like a campfire (CampfireBlockEntityMixin) at {@link FirePit#DRAIN}
 * times the rate: a fresh fire burns 3-5 campfire-minutes, then the stacked firewood (up to {@link FirePit#STOCK_MAX}, one
 * piece at a time, {@link Campfires#FIREWOOD_TICKS} each), then it sinks to embers ({@link FirePit#EMBER_TICKS}), which
 * Firewood brings back and which die cold if left. Eight food slots on two spits: campfire recipes, cooked food stays
 * on and burns if left, as on the campfire.
 * <p>
 * A big pit (FirePitBlock.BIG, F12) holds {@link FirePit#BIG_STOCK_MAX} firewood, burns at {@link FirePit#BIG_DRAIN} with
 * every piece lasting 3/2 as long, glows twice as long, reaches further and smokes a much bigger column. A skinned carcass
 * hauled to it hangs over the fire ({@link #hangCarcass}) and roasts into a pile of cooked meat ({@link #takeRoast}).
 */
public class FirePitBlockEntity extends BlockEntity {
    /** Food on the spits, in pixels from the corner (the model spans 32 x 32): two spits along x, four places each. */
    public static final float SPIT_Y = 18.5F;
    private static final float[] SPIT_Z = {10.5F, 21.5F};

    private final NonNullList<ItemStack> items = NonNullList.withSize(FirePit.SLOTS, ItemStack.EMPTY);
    private final int[] cookingProgress = new int[FirePit.SLOTS], cookingTime = new int[FirePit.SLOTS];
    private final boolean[] cooked = new boolean[FirePit.SLOTS];
    /** Firewood waiting to burn, one item per piece. */
    private final List<ItemStack> stock = new ArrayList<>();
    /** Campfire-burn ticks left while lit; -1 = not started (a fresh fire is rolled on its first lit tick). */
    private int burnLeft = -1;
    private int embersLeft;
    private final RecipeManager.CachedCheck<SingleRecipeInput, CampfireCookingRecipe> recipes = RecipeManager.createCheck(RecipeType.CAMPFIRE_COOKING);
    /** A whole carcass roasting over a big pit: the raw meat shown while it cooks, the cooked pile it gives, lit ticks left. */
    private ItemStack roastRaw = ItemStack.EMPTY, roastCooked = ItemStack.EMPTY;
    private int roastLeft;

    public FirePitBlockEntity(BlockPos pos, BlockState state) {
        super(FirePit.FIRE_PIT_ENTITY.get(), pos, state);
    }

    public static float slotX(int slot) {
        return 8 + (slot % 4) * 16 / 3F;
    }

    public static float slotZ(int slot) {
        return SPIT_Z[slot / 4];
    }

    /** The big pit's model is the 2x2 one scaled by 1.5 across (1.25 up) round the 3x3's middle (scripts/gen_firepit.py). */
    public static final float BIG_SCALE = 1.5F, BIG_SPIT_Y = SPIT_Y * 1.25F;

    public static float bigX(float x) {
        return 24 + (x - 16) * BIG_SCALE;
    }

    public boolean big() {
        return getBlockState().getValue(FirePitBlock.BIG);
    }

    public int stockMax() {
        return big() ? FirePit.BIG_STOCK_MAX : FirePit.STOCK_MAX;
    }

    /** What hangs roasting over a big pit (raw while it cooks, then the cooked meat), or empty. */
    public ItemStack roast() {
        return roastRaw.isEmpty() ? ItemStack.EMPTY : roastLeft > 0 ? roastRaw : roastCooked;
    }

    public boolean roastDone() {
        return !roastRaw.isEmpty() && roastLeft <= 0;
    }

    public NonNullList<ItemStack> items() {
        return items;
    }

    public int stock() {
        return stock.size();
    }

    // ---- ticking ----

    void serverTick(ServerLevel level, BlockPos pos, BlockState state) {
        if (state.getValue(FirePitBlock.LIT)) {
            tickLit(level, pos, state);
            return;
        }
        boolean changed = false;
        for (int slot = 0; slot < FirePit.SLOTS; slot++) {
            if (cookingProgress[slot] > 0) {
                cookingProgress[slot] = Math.max(0, cookingProgress[slot] - 2); // cools off, like vanilla's campfire
                changed = true;
            }
        }
        if (state.getValue(FirePitBlock.EMBERS) && (embersLeft -= Temperature.fireDrain(level, pos, level.getRandom())) <= 0) {
            setFire(false, false);
            changed = true;
        }
        if (changed) setChanged();
    }

    private void tickLit(ServerLevel level, BlockPos pos, BlockState state) {
        RandomSource random = level.getRandom();
        boolean big = state.getValue(FirePitBlock.BIG);
        NightDangers.markLit(level, pos, big ? FirePit.BIG_REACH : FirePit.REACH);
        if (burnLeft < 0) burnLeft = Campfires.MIN_BURN + random.nextInt(Campfires.MAX_BURN - Campfires.MIN_BURN);
        int drain = big ? FirePit.BIG_DRAIN : state.getValue(FirePitBlock.GRATE) ? FirePit.GRATE_DRAIN : FirePit.DRAIN;
        if ((burnLeft -= drain * Temperature.fireDrain(level, pos, random)) <= 0) {
            // nothing stacked on it: a Firewood Pile beside the pit (from this corner, so the pit's width out) feeds it
            if (stock.isEmpty() && FirewoodPileBlock.takeNear(level, pos, big ? 3 : 2).isEmpty()) {
                sinkToEmbers();
                return;
            }
            boolean charcoal = !stock.isEmpty() && stock.removeLast().is(Items.CHARCOAL); // the next piece of the stack catches
            burnLeft += Campfires.FIREWOOD_TICKS * (charcoal ? FirePit.CHARCOAL_BURN : 1) * (big ? FirePit.BIG_FUEL_HALVES : 2) / 2;
            updateFuel();
        }
        if (roastLeft > 0 && --roastLeft == 0) {
            level.playSound(null, pos, SoundEvents.FIRE_EXTINGUISH, SoundSource.BLOCKS, 0.4F, 1.6F);
            sync();
        }
        boolean changed = false;
        for (int slot = 0; slot < FirePit.SLOTS; slot++) {
            ItemStack stack = items.get(slot);
            if (stack.isEmpty() || stack.is(Mechanical.BURNT_FOOD.get())) continue;
            changed = true;
            if (++cookingProgress[slot] < cookingTime[slot]) continue;
            if (!cooked[slot]) {
                // done: stays on the spit and starts its countdown to burning
                SingleRecipeInput input = new SingleRecipeInput(stack);
                items.set(slot, recipes.getRecipeFor(input, level).map(r -> r.value().assemble(input)).orElse(stack));
                cooked[slot] = true;
            } else {
                items.set(slot, new ItemStack(Mechanical.BURNT_FOOD.get()));
                cooked[slot] = false;
            }
            cookingProgress[slot] = 0;
            cookingTime[slot] = Campfires.MIN_BURN_FOOD + random.nextInt(Campfires.MAX_BURN_FOOD - Campfires.MIN_BURN_FOOD);
            level.sendBlockUpdated(pos, state, state, Block.UPDATE_ALL);
        }
        if (changed) setChanged();
    }

    /** Particles: a tall smoke column over a burning pit (taller with more firewood), flames and sparks, smoke off the food; embers smoulder. */
    static void clientTick(Level level, BlockPos pos, BlockState state, FirePitBlockEntity pit) {
        RandomSource r = level.getRandom();
        boolean big = state.getValue(FirePitBlock.BIG);
        double half = big ? 1.5 : 1, cx = pos.getX() + half, cy = pos.getY(), cz = pos.getZ() + half;
        if (state.getValue(FirePitBlock.LIT)) {
            int fuel = state.getValue(FirePitBlock.FUEL);
            // a big pit sends up a column twice as thick and wide whether grated or not; a small one with a grate burns clean
            // a thin wisp, not a solid pillar: ~1 puff/tick big, ~0.4 small (vanilla signal campfire ~0.3)
            float chance = big ? 0.35F + 0.1F * fuel : (0.2F + 0.07F * fuel) * (state.getValue(FirePitBlock.GRATE) ? FirePit.GRATE_SMOKE : 1);
            if (r.nextFloat() < chance) {
                // signal smoke lives ~300 ticks and barely slows: 0.16-0.22/tick climbs ~50-60 blocks, seen from far off
                for (int i = 0; i < (big ? 2 + r.nextInt(2) : 1 + r.nextInt(2)); i++) {
                    level.addAlwaysVisibleParticle(ParticleTypes.CAMPFIRE_SIGNAL_SMOKE, true, cx + (r.nextDouble() - 0.5) * 1.1 * half, cy + 0.6 + r.nextDouble() * 1.4 * half,
                            cz + (r.nextDouble() - 0.5) * 1.1 * half, 0, (big ? 0.2 : 0.16) + r.nextDouble() * 0.06, 0);
                }
            }
            if (r.nextInt(3) == 0) level.addParticle(ParticleTypes.FLAME, cx + (r.nextDouble() - 0.5) * 0.9, cy + 0.3 + r.nextDouble() * 0.6, cz + (r.nextDouble() - 0.5) * 0.9, 0, 0.02, 0);
            if (r.nextInt(6) == 0) level.addParticle(ParticleTypes.LAVA, cx + (r.nextDouble() - 0.5) * 0.6, cy + 0.4, cz + (r.nextDouble() - 0.5) * 0.6, 0, 0, 0);
            if (r.nextInt(12) == 0) {
                level.playLocalSound(cx, cy + 0.5, cz, SoundEvents.CAMPFIRE_CRACKLE, SoundSource.BLOCKS, 1.0F + r.nextFloat(), 0.5F + r.nextFloat() * 0.6F, false);
            }
            for (int slot = 0; slot < FirePit.SLOTS; slot++) {
                if (pit.items.get(slot).isEmpty() || r.nextFloat() >= 0.2F) continue;
                double x = pos.getX() + (big ? bigX(slotX(slot)) : slotX(slot)) / 16, y = pos.getY() + (big ? BIG_SPIT_Y : SPIT_Y) / 16,
                        z = pos.getZ() + (big ? bigX(slotZ(slot)) : slotZ(slot)) / 16;
                for (int i = 0; i < 3; i++) level.addParticle(ParticleTypes.SMOKE, x, y, z, 0, 5.0E-4, 0);
            }
        } else if (state.getValue(FirePitBlock.EMBERS)) {
            if (r.nextInt(3) == 0) level.addParticle(ParticleTypes.SMOKE, cx + (r.nextDouble() - 0.5) * 1.2, cy + 0.25, cz + (r.nextDouble() - 0.5) * 1.2, 0, 0.02, 0);
            if (r.nextInt(8) == 0) level.addParticle(ParticleTypes.SMALL_FLAME, cx + (r.nextDouble() - 0.5) * 1.0, cy + 0.2, cz + (r.nextDouble() - 0.5) * 1.0, 0, 0.01, 0);
        }
    }

    // ---- fire ----

    private void setFire(boolean lit, boolean embers) {
        if (level != null) FirePitBlock.setAll(level, worldPosition, s -> s.setValue(FirePitBlock.LIT, lit).setValue(FirePitBlock.EMBERS, embers));
        setChanged();
    }

    /** The visible log stack: 0 none, 1 up to 6 pieces (12 on a big pit), 2 up to 12 (24), 3 more. */
    private void updateFuel() {
        int fuel = stock.isEmpty() ? 0 : Math.min(3, 1 + (stock.size() - 1) / (6 * stockMax() / FirePit.STOCK_MAX));
        if (level != null) FirePitBlock.setAll(level, worldPosition, s -> s.setValue(FirePitBlock.FUEL, fuel));
        setChanged();
    }

    private void sinkToEmbers() {
        burnLeft = -1;
        embersLeft = big() ? FirePit.BIG_EMBER_TICKS : FirePit.EMBER_TICKS;
        setFire(false, true);
        if (level != null) level.playSound(null, worldPosition, SoundEvents.FIRE_EXTINGUISH, SoundSource.BLOCKS, 0.6F, 0.5F);
    }

    /** Lit by the Fire Drill. */
    public void light() {
        burnLeft = -1;
        setFire(true, false);
    }

    /** Wind or rain: sinks to embers (Firewood brings it back). */
    public void blowOut() {
        sinkToEmbers();
    }

    /** Shovel or a horde: out cold. */
    public void douse() {
        burnLeft = -1;
        embersLeft = 0;
        setFire(false, false);
        if (level != null) level.playSound(null, worldPosition, SoundEvents.FIRE_EXTINGUISH, SoundSource.BLOCKS, 1.0F, 0.8F);
    }

    /** Firewood (or Charcoal on a grated pit): blows embers back into flame (as on the campfire), else goes on the stack. False if the stack is full. */
    boolean feed(ServerLevel level, Player player, ItemStack stack) {
        if (getBlockState().getValue(FirePitBlock.EMBERS) && !getBlockState().getValue(FirePitBlock.LIT)) {
            light();
            level.playSound(null, worldPosition, SoundEvents.FIRECHARGE_USE, SoundSource.BLOCKS, 0.8F, 1.2F);
        } else if (stock.size() < stockMax()) {
            stock.add(stack.copyWithCount(1));
            updateFuel();
            level.playSound(null, worldPosition, SoundEvents.WOOD_PLACE, SoundSource.BLOCKS, 0.8F, 0.9F);
        } else {
            player.sendOverlayMessage(Component.translatable("message.mechanical.fire_pit.full", stockMax()));
            return false;
        }
        stack.consume(1, player);
        return true;
    }

    /** A whole Rough Log: relights embers like firewood, else goes on the stack as {@link FirePit#LOG_FIREWOOD} pieces (needs that much room). */
    boolean feedLog(ServerLevel level, Player player, ItemStack log, ItemStack firewood) {
        boolean embers = getBlockState().getValue(FirePitBlock.EMBERS) && !getBlockState().getValue(FirePitBlock.LIT);
        if (!embers && stock.size() + FirePit.LOG_FIREWOOD > stockMax()) {
            player.sendOverlayMessage(Component.translatable("message.mechanical.fire_pit.full", stockMax()));
            return false;
        }
        if (!feed(level, player, firewood.copy())) return false;
        for (int i = 1; i < FirePit.LOG_FIREWOOD && !embers; i++) stock.add(firewood.copy());
        updateFuel();
        log.consume(1, player);
        return true;
    }

    // ---- food ----

    boolean placeFood(ServerLevel level, Player player, ItemStack stack) {
        for (int slot = 0; slot < FirePit.SLOTS; slot++) {
            if (!items.get(slot).isEmpty()) continue;
            var recipe = level.recipeAccess().getRecipeFor(RecipeType.CAMPFIRE_COOKING, new SingleRecipeInput(stack), level).map(RecipeHolder::value);
            if (recipe.isEmpty()) return false;
            cookingTime[slot] = recipe.get().cookingTime();
            cookingProgress[slot] = 0;
            cooked[slot] = false;
            items.set(slot, stack.consumeAndReturn(1, player));
            level.gameEvent(GameEvent.BLOCK_CHANGE, worldPosition, GameEvent.Context.of(player, getBlockState()));
            sync();
            return true;
        }
        return false;
    }

    /** Gives the player one item off the spits (cooked or burnt first). False if there is nothing. */
    boolean takeItem(Player player) {
        int slot = -1;
        for (int i = 0; i < FirePit.SLOTS; i++) {
            if (items.get(i).isEmpty()) continue;
            boolean done = cooked[i] || items.get(i).is(Mechanical.BURNT_FOOD.get());
            if (slot < 0 || done) slot = i;
            if (done) break;
        }
        if (slot < 0) return false;
        player.getInventory().placeItemBackInInventory(items.get(slot), Prediction.SERVER_ONLY);
        items.set(slot, ItemStack.EMPTY);
        cookingProgress[slot] = 0;
        cooked[slot] = false;
        sync();
        return true;
    }

    // ---- a whole carcass over a big pit ----

    /**
     * Hangs {@code carcass} (hauled by {@code player}) over this big pit: it must be skinned (or have nothing to skin) and
     * not a caravan's. It roasts {@link FirePit#ROAST_TICKS} lit ticks into {@link FirePit#ROAST_MEAT} times its most meat,
     * cooked (rotten flesh if it had spoiled). The body is used up.
     */
    public boolean hangCarcass(ServerLevel level, Player player, CarcassEntity carcass) {
        String problem = !big() ? "message.mechanical.fire_pit.roast_big" : !roastRaw.isEmpty() ? "message.mechanical.fire_pit.roast_busy"
                : carcass.isCaravanProperty() ? "message.mechanical.carcass.caravan"
                : carcass.stage() == 0 && !Butchery.yieldFor(carcass.animal()).outer().isEmpty() ? "message.mechanical.fire_pit.roast_skin" : null;
        ItemStack raw = ItemStack.EMPTY, cooked = ItemStack.EMPTY;
        if (problem == null) {
            Butchery.Yield yield = Butchery.yieldFor(carcass.animal());
            raw = new ItemStack(yield.meat().get());
            SingleRecipeInput input = new SingleRecipeInput(raw);
            cooked = level.recipeAccess().getRecipeFor(RecipeType.CAMPFIRE_COOKING, input, level).map(r -> r.value().assemble(input)).orElse(ItemStack.EMPTY);
            if (cooked.isEmpty()) problem = "message.mechanical.fire_pit.roast_cannot";
            else cooked.setCount(Math.max(1, FirePit.ROAST_MEAT * yield.maxMeat()));
            if (carcass.spoiled()) cooked = new ItemStack(Items.ROTTEN_FLESH, cooked.getCount());
        }
        if (problem != null) {
            player.sendOverlayMessage(Component.translatable(problem));
            return false;
        }
        roastRaw = raw;
        roastCooked = cooked;
        roastLeft = FirePit.ROAST_TICKS;
        carcass.discard();
        level.playSound(null, worldPosition, SoundEvents.WOOD_HIT, SoundSource.BLOCKS, 1.0F, 0.8F);
        level.playSound(null, worldPosition, SoundEvents.SLIME_SQUISH, SoundSource.BLOCKS, 0.6F, 0.6F);
        sync();
        return true;
    }

    /** Empty hand on a big pit with a done roast: the cooked meat into the inventory. False if there is none ready. */
    boolean takeRoast(Player player) {
        if (!roastDone()) return false;
        player.getInventory().placeItemBackInInventory(roastCooked, Prediction.SERVER_ONLY);
        if (player instanceof net.minecraft.server.level.ServerPlayer sp) FirePit.award(sp, "iron_age/whole_roast");
        roastRaw = roastCooked = ItemStack.EMPTY;
        sync();
        return true;
    }

    /** Moves everything (food, firewood, fire, roast) into {@code to} and leaves this empty: the pit grows (FirePitBlock#upgrade). */
    void moveTo(FirePitBlockEntity to) {
        for (int i = 0; i < FirePit.SLOTS; i++) {
            to.items.set(i, items.get(i));
            items.set(i, ItemStack.EMPTY);
            to.cookingProgress[i] = cookingProgress[i];
            to.cookingTime[i] = cookingTime[i];
            to.cooked[i] = cooked[i];
        }
        to.stock.clear();
        to.stock.addAll(stock);
        stock.clear();
        to.burnLeft = burnLeft;
        to.embersLeft = embersLeft;
        to.roastRaw = roastRaw;
        to.roastCooked = roastCooked;
        to.roastLeft = roastLeft;
        roastRaw = roastCooked = ItemStack.EMPTY;
    }

    /** After a move: the log stack shown and the clients told. */
    void refresh() {
        updateFuel();
        sync();
    }

    private void sync() {
        setChanged();
        if (level != null) level.sendBlockUpdated(worldPosition, getBlockState(), getBlockState(), Block.UPDATE_ALL);
    }

    @Override
    public void preRemoveSideEffects(BlockPos pos, BlockState state) {
        if (level == null) return;
        Containers.dropContents(level, pos, items);
        for (ItemStack wood : stock) Containers.dropItemStack(level, pos.getX() + 1, pos.getY() + 0.3, pos.getZ() + 1, wood);
        // a roast taken down half done is raw meat again
        if (!roastRaw.isEmpty()) Containers.dropItemStack(level, pos.getX() + 1, pos.getY() + 0.3, pos.getZ() + 1, roastDone() ? roastCooked : roastRaw.copyWithCount(roastCooked.getCount()));
    }

    // ---- saving / syncing ----

    @Override
    protected void saveAdditional(ValueOutput output) {
        super.saveAdditional(output);
        ContainerHelper.saveAllItems(output, items, true);
        output.putIntArray("cooking", cookingProgress);
        output.putIntArray("cooking_total", cookingTime);
        int[] done = new int[FirePit.SLOTS];
        for (int i = 0; i < FirePit.SLOTS; i++) done[i] = cooked[i] ? 1 : 0;
        output.putIntArray("cooked", done);
        output.store("fuel", ItemStack.CODEC.listOf(), List.copyOf(stock));
        output.putInt("burn", burnLeft);
        output.putInt("embers", embersLeft);
        if (!roastRaw.isEmpty()) {
            output.store("roast_raw", ItemStack.CODEC, roastRaw);
            output.store("roast_cooked", ItemStack.CODEC, roastCooked);
            output.putInt("roast_left", roastLeft);
        }
    }

    @Override
    protected void loadAdditional(ValueInput input) {
        super.loadAdditional(input);
        items.clear();
        ContainerHelper.loadAllItems(input, items);
        input.getIntArray("cooking").ifPresent(a -> System.arraycopy(a, 0, cookingProgress, 0, Math.min(a.length, FirePit.SLOTS)));
        input.getIntArray("cooking_total").ifPresent(a -> System.arraycopy(a, 0, cookingTime, 0, Math.min(a.length, FirePit.SLOTS)));
        input.getIntArray("cooked").ifPresent(a -> {
            for (int i = 0; i < Math.min(a.length, FirePit.SLOTS); i++) cooked[i] = a[i] != 0;
        });
        stock.clear();
        input.read("fuel", ItemStack.CODEC.listOf()).ifPresent(stock::addAll);
        burnLeft = input.getIntOr("burn", -1);
        embersLeft = input.getIntOr("embers", 0);
        roastRaw = input.read("roast_raw", ItemStack.CODEC).orElse(ItemStack.EMPTY);
        roastCooked = input.read("roast_cooked", ItemStack.CODEC).orElse(ItemStack.EMPTY);
        roastLeft = input.getIntOr("roast_left", 0);
    }

    @Override
    public Packet<ClientGamePacketListener> getUpdatePacket() {
        return ClientboundBlockEntityDataPacket.create(this);
    }

    @Override
    public CompoundTag getUpdateTag(HolderLookup.Provider registries) {
        return saveCustomOnly(registries);
    }
}
