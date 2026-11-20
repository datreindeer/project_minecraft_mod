package com.mechanical;

import static com.mechanical.Mechanical.BLOCKS;
import static com.mechanical.Mechanical.ITEMS;
import static com.mechanical.Mechanical.MODID;

import java.util.List;
import java.util.function.Supplier;

import com.mojang.serialization.Codec;

import net.minecraft.core.component.DataComponentType;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.resources.Identifier;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.tags.TagKey;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.food.FoodProperties;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.ResultSlot;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.block.SoundType;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.material.MapColor;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.entity.living.LivingEntityUseItemEvent;
import net.neoforged.neoforge.event.entity.player.PlayerContainerEvent;
import net.neoforged.neoforge.event.tick.PlayerTickEvent;
import net.neoforged.neoforge.registries.DeferredBlock;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredItem;
import net.neoforged.neoforge.registries.DeferredRegister;

/**
 * Food spoils; smoking and drying preserve it.
 * <p>
 * A perishable food (item tags {@code mechanical:perishes_1_day/2_days/3_days/10_days/20_days/30_days}) carries {@link #SPOILS_AT}, the
 * game time it spoils at, so nothing ticks per item. It is stamped the first time it is seen in a player's inventory
 * or an open container, and checked there once a second: when the time is up the stack turns into Rotten Flesh
 * ({@code mechanical:rots_to_flesh}: meat and fish) or Spoiled Food (everything else). Items lying unseen in a chest
 * spoil when the chest is next opened. The time is rounded up to 1/16 of the shelf life so food stamped close together
 * still stacks. Eating rotting food (last 20 % of its life) may give Hunger.
 * <p>
 * Smoking ({@link SmokingRackBlock}) and drying ({@link DryingRackBlock}) turn raw meat, fish, berries and mushrooms into foods that
 * keep for 30 days. Data: scripts/gen_preserving.py, textures: scripts/gen_preserving_textures.py.
 */
@EventBusSubscriber(modid = MODID)
public final class Preserving {
    public static final long DAY = 24000;
    /** Freshness above this is Fresh, above {@link #ROTTING} Stale, below it Rotting. */
    public static final float STALE = 0.5F, ROTTING = 0.2F;

    public static final DeferredRegister.DataComponents COMPONENTS = DeferredRegister.createDataComponents(Registries.DATA_COMPONENT_TYPE, MODID);
    /** Game time when this food spoils. */
    public static final DeferredHolder<DataComponentType<?>, DataComponentType<Long>> SPOILS_AT = COMPONENTS.registerComponentType(
            "spoils_at", b -> b.persistent(Codec.LONG).networkSynchronized(ByteBufCodecs.VAR_LONG));
    /** Game time this food was shut in a closed Bark Basket or sealed Storage Jar (see {@link #intoBasket}). */
    public static final DeferredHolder<DataComponentType<?>, DataComponentType<Long>> BASKET_SINCE = COMPONENTS.registerComponentType(
            "basket_since", b -> b.persistent(Codec.LONG).networkSynchronized(ByteBufCodecs.VAR_LONG));
    /** In a closed Bark Basket (cool, dry, airy bark) food ages at this share of the normal rate. */
    public static final float BASKET_RATE = 1 / 3F;

    public static final TagKey<Item> PERISHES_1_DAY = tag("perishes_1_day"), PERISHES_2_DAYS = tag("perishes_2_days"),
            PERISHES_3_DAYS = tag("perishes_3_days"), PERISHES_10_DAYS = tag("perishes_10_days"),
            PERISHES_20_DAYS = tag("perishes_20_days"), PERISHES_30_DAYS = tag("perishes_30_days"), ROTS_TO_FLESH = tag("rots_to_flesh"),
            SMOKABLE_MEAT = tag("smokable_meat"), SMOKABLE_FISH = tag("smokable_fish"), DRYABLE_BERRIES = tag("dryable_berries"),
            DRYABLE_MUSHROOMS = tag("dryable_mushrooms");

    public static final DeferredItem<Item> SMOKED_MEAT = food("smoked_meat", 3, 0.8F);
    public static final DeferredItem<Item> SMOKED_FISH = food("smoked_fish", 3, 0.7F);
    public static final DeferredItem<Item> JERKY = food("jerky", 2, 0.8F);
    public static final DeferredItem<Item> DRIED_FISH = food("dried_fish", 2, 0.7F);
    public static final DeferredItem<Item> DRIED_BERRIES = food("dried_berries", 1, 0.5F);
    public static final DeferredItem<Item> DRIED_MUSHROOMS = food("dried_mushrooms", 1, 0.6F);
    // ---- what each meat dries into (DryingRackBlock, item tags mechanical:dryable_meat/<product>); Jerky is what's left:
    // strips cut from a big cut, and any smokable meat not in a family ----
    public static final DeferredItem<Item> VENISON_JERKY = food("venison_jerky", 3, 0.8F);
    public static final DeferredItem<Item> BEEF_JERKY = food("beef_jerky", 3, 0.9F);
    public static final DeferredItem<Item> DRIED_PORK = food("dried_pork", 3, 0.7F);
    public static final DeferredItem<Item> DRIED_MUTTON = food("dried_mutton", 3, 0.8F);
    public static final DeferredItem<Item> GAME_JERKY = food("game_jerky", 2, 0.8F);
    public static final DeferredItem<Item> SMALL_GAME_JERKY = food("small_game_jerky", 2, 0.6F);
    public static final DeferredItem<Item> DRIED_FOWL = food("dried_fowl", 2, 0.7F);
    public static final DeferredItem<Item> DRIED_SEAL_MEAT = food("dried_seal_meat", 3, 0.9F);
    public static final DeferredItem<Item> STOCKFISH = food("stockfish", 2, 0.8F);
    /** Cut from a Raw Haunch / Ribs / Loin with a knife: thin enough to dry (into Jerky), or to smoke. */
    public static final DeferredItem<Item> RAW_MEAT_STRIPS = food("raw_meat_strips", 1, 0.2F);
    /** A big cut smoked whole on the Smoking Rack; a knife slices it into Smoked Meat. */
    public static final DeferredItem<Item> SMOKED_HAUNCH = food("smoked_haunch", 9, 0.8F);
    public static final DeferredItem<Item> SMOKED_RIBS = food("smoked_ribs", 6, 0.8F);
    public static final DeferredItem<Item> SMOKED_LOIN = food("smoked_loin", 6, 0.9F);
    /** Meat that can't go on the Drying Rack: big cuts (too thick) and fat-rich meat (goes rancid in the air). */
    public static final TagKey<Item> NOT_DRYABLE = tag("not_dryable");

    /** A rack look (DryingRackBlock spot value while drying) and product per family; tags {@code mechanical:dryable_meat/<product>}. */
    public record Drying(TagKey<Item> tag, Supplier<Item> product, int look) {}

    public static final List<Drying> DRYING = List.of(
            dries("venison_jerky", VENISON_JERKY, DryingRackBlock.MEAT), dries("beef_jerky", BEEF_JERKY, DryingRackBlock.MEAT),
            dries("dried_pork", DRIED_PORK, DryingRackBlock.MEAT), dries("dried_mutton", DRIED_MUTTON, DryingRackBlock.MEAT),
            dries("game_jerky", GAME_JERKY, DryingRackBlock.MEAT), dries("small_game_jerky", SMALL_GAME_JERKY, DryingRackBlock.MEAT),
            dries("dried_seal_meat", DRIED_SEAL_MEAT, DryingRackBlock.MEAT), dries("dried_fowl", DRIED_FOWL, DryingRackBlock.FOWL),
            dries("stockfish", STOCKFISH, DryingRackBlock.FISH));

    /** What a strip on the Smoking Rack smokes into: a big cut stays whole (Smoked Haunch / Ribs / Loin). */
    public static Item smoked(ItemStack raw) {
        return raw.is(CarcassHauling.RAW_HAUNCH.get()) ? SMOKED_HAUNCH.get() : raw.is(CarcassHauling.RAW_RIBS.get()) ? SMOKED_RIBS.get()
                : raw.is(CarcassHauling.RAW_LOIN.get()) ? SMOKED_LOIN.get() : SMOKED_MEAT.get();
    }

    private static Drying dries(String product, DeferredItem<Item> item, int look) {
        return new Drying(tag("dryable_meat/" + product), item::get, look);
    }

    /** How this raw meat or fish dries: its family, else the plain Jerky / Dried Fish; null if it doesn't go on the rack. */
    public static Drying drying(ItemStack stack) {
        if (stack.is(NOT_DRYABLE)) return null;
        for (Drying d : DRYING) if (stack.is(d.tag())) return d;
        if (stack.is(SMOKABLE_MEAT)) return new Drying(SMOKABLE_MEAT, JERKY::get, DryingRackBlock.MEAT);
        if (stack.is(SMOKABLE_FISH)) return new Drying(SMOKABLE_FISH, DRIED_FISH::get, DryingRackBlock.FISH);
        return null;
    }
    /** What plant food turns into; goes in the Compost Heap. */
    public static final DeferredItem<Item> SPOILED_FOOD = ITEMS.registerSimpleItem("spoiled_food");

    public static final DeferredBlock<SmokingRackBlock> SMOKING_RACK = BLOCKS.registerBlock("smoking_rack", SmokingRackBlock::new, p -> p
            .mapColor(MapColor.WOOD).strength(1.0F).sound(SoundType.WOOD).noOcclusion().ignitedByLava());
    public static final DeferredItem<BlockItem> SMOKING_RACK_ITEM = ITEMS.registerSimpleBlockItem("smoking_rack", SMOKING_RACK);
    public static final Supplier<BlockEntityType<SmokingRackBlockEntity>> SMOKING_RACK_ENTITY = Mechanical.BLOCK_ENTITIES.register("smoking_rack",
            () -> new BlockEntityType<>(SmokingRackBlockEntity::new, SMOKING_RACK.get()));

    private static TagKey<Item> tag(String name) {
        return TagKey.create(Registries.ITEM, Identifier.fromNamespaceAndPath(MODID, name));
    }

    private static DeferredItem<Item> food(String name, int nutrition, float saturation) {
        return ITEMS.registerSimpleItem(name, p -> p.food(new FoodProperties.Builder().nutrition(nutrition).saturationModifier(saturation).build()));
    }

    public static void init(IEventBus modEventBus) {
        COMPONENTS.register(modEventBus);
    }

    /** Shelf life in ticks, or 0 if it keeps. */
    public static long shelfLife(ItemStack stack) {
        if (stack.is(PERISHES_1_DAY)) return DAY;
        if (stack.is(PERISHES_2_DAYS)) return 2 * DAY;
        if (stack.is(PERISHES_3_DAYS)) return 3 * DAY;
        if (stack.is(PERISHES_10_DAYS)) return 10 * DAY;
        if (stack.is(PERISHES_20_DAYS)) return 20 * DAY;
        if (stack.is(PERISHES_30_DAYS)) return 30 * DAY;
        return 0;
    }

    /** Spoil time for food stamped now: rounded up to 1/16 of the shelf life, so food stamped close together stacks. */
    public static long spoilTime(long life, long now) {
        long step = life / 16;
        return Math.ceilDiv(now + life, step) * step;
    }

    /** Freshness left, 1 (fresh) to 0 (spoiled); 1 for food not stamped yet, -1 for food that keeps. */
    public static float freshness(ItemStack stack, long now) {
        long life = shelfLife(stack);
        if (life == 0) return -1;
        Long at = stack.get(SPOILS_AT.get());
        return at == null ? 1 : Math.clamp((at - now) / (float) life, 0, 1);
    }

    /** A copy of a stack as it goes into a closed basket: stamped (if perishable) with the time it went in. */
    public static ItemStack intoBasket(ItemStack stack, long now) {
        if (stack.isEmpty() || shelfLife(stack) == 0) return stack;
        ItemStack out = stack.copy();
        if (!out.has(SPOILS_AT.get())) out.set(SPOILS_AT.get(), spoilTime(shelfLife(out), now));
        out.set(BASKET_SINCE.get(), now);
        return out;
    }

    /** A stack coming out of a closed basket: its spoil time moves later by the share of the basket time that didn't count. */
    public static ItemStack outOfBasket(ItemStack stack, long now) {
        return outOfBasket(stack, now, BASKET_RATE);
    }

    /** As {@link #outOfBasket(ItemStack, long)} for a store where food ages at {@code rate} (a sealed Storage Jar: {@link Pottery#JAR_RATE}). */
    public static ItemStack outOfBasket(ItemStack stack, long now, float rate) {
        Long since = stack.get(BASKET_SINCE.get());
        if (since == null) return stack;
        ItemStack out = stack.copy();
        out.remove(BASKET_SINCE.get());
        Long at = out.get(SPOILS_AT.get());
        if (at != null && now > since) out.set(SPOILS_AT.get(), at + (long) ((now - since) * (1 - rate)));
        return out;
    }

    /** Rotting food can't go on a rack: tells the player so (overlay) and returns true. */
    public static boolean tooFarGone(ItemStack stack, net.minecraft.world.level.Level level, Player player) {
        float fresh = freshness(stack, level.getGameTime());
        if (fresh < 0 || fresh >= ROTTING) return false;
        if (!level.isClientSide()) player.sendOverlayMessage(net.minecraft.network.chat.Component.translatable("message.mechanical.too_far_gone"));
        return true;
    }

    /** What a stack spoils into (same count). */
    public static ItemStack spoiled(ItemStack stack) {
        return new ItemStack(stack.is(ROTS_TO_FLESH) ? Items.ROTTEN_FLESH : SPOILED_FOOD.get(), stack.getCount());
    }

    /**
     * Stamps a perishable that has no spoil time yet (in place, so a stack being eaten stays the same stack), or returns
     * what it spoiled into once its time is up. Returns the stack itself when it changed only in place, null if nothing changed.
     */
    public static ItemStack check(ItemStack stack, long now) {
        if (stack.isEmpty()) return null;
        long life = shelfLife(stack);
        if (life == 0) return null;
        Long at = stack.get(SPOILS_AT.get());
        if (at == null) {
            stack.set(SPOILS_AT.get(), spoilTime(life, now));
            return stack;
        }
        return now >= at ? spoiled(stack) : null;
    }

    private static void checkSlots(List<Slot> slots, long now) {
        for (Slot slot : slots) {
            if (slot instanceof ResultSlot) continue; // a crafting preview, not a real item yet
            ItemStack stack = slot.getItem();
            ItemStack out = check(stack, now);
            if (out == stack) slot.setChanged();
            else if (out != null) slot.set(out);
        }
    }

    /** The player's inventory and whatever container they have open. */
    public static void checkPlayer(Player player) {
        long now = player.level().getGameTime();
        checkSlots(player.inventoryMenu.slots, now);
        mergeAged(player.inventoryMenu.slots, player);
        AbstractContainerMenu open = player.containerMenu;
        if (open != player.inventoryMenu) {
            checkSlots(open.slots, now);
            mergeAged(open.slots, player);
        }
    }

    /** Same food, only the spoil time differs? */
    static boolean sameButAge(ItemStack a, ItemStack b) {
        if (!a.is(b.getItem()) || !a.has(SPOILS_AT.get()) || !b.has(SPOILS_AT.get())) return false;
        ItemStack x = a.copyWithCount(1), y = b.copyWithCount(1);
        x.remove(SPOILS_AT.get());
        y.remove(SPOILS_AT.get());
        return ItemStack.isSameItemSameComponents(x, y);
    }

    /**
     * Food of the same kind caught or picked at different times stacks again: stacks in the same container that differ only
     * in spoil time are combined, and the combined stack takes the earliest time (the oldest food decides). Not in a crafting
     * grid: there each item sits in its own slot on purpose (2 Flour + water is two flour slots).
     */
    static void mergeAged(List<Slot> slots, Player player) {
        for (int i = 0; i < slots.size(); i++) {
            Slot into = slots.get(i);
            ItemStack a = into.getItem();
            if (into instanceof ResultSlot || into.container instanceof net.minecraft.world.inventory.CraftingContainer || a.isEmpty() || !a.has(SPOILS_AT.get()) || a.getCount() >= a.getMaxStackSize()) continue;
            for (int j = i + 1; j < slots.size() && a.getCount() < a.getMaxStackSize(); j++) {
                Slot from = slots.get(j);
                ItemStack b = from.getItem();
                if (from.container != into.container || from instanceof ResultSlot || !sameButAge(a, b) || !from.mayPickup(player)) continue;
                int n = Math.min(b.getCount(), a.getMaxStackSize() - a.getCount());
                a.set(SPOILS_AT.get(), Math.min(a.get(SPOILS_AT.get()), b.get(SPOILS_AT.get())));
                a.grow(n);
                b.shrink(n);
                into.setChanged();
                from.setChanged();
            }
        }
    }

    @SubscribeEvent
    static void onPlayerTick(PlayerTickEvent.Post event) {
        if (event.getEntity() instanceof ServerPlayer player && player.tickCount % 20 == 0) checkPlayer(player);
    }

    /** Food that sat unseen in a chest spoils (or is stamped) the moment the chest is opened. */
    @SubscribeEvent
    static void onContainerOpen(PlayerContainerEvent.Open event) {
        if (event.getEntity() instanceof ServerPlayer player) checkSlots(event.getContainer().slots, player.level().getGameTime());
    }

    /** Rotting food: 1 in 2 chance of Hunger for 30 s. */
    @SubscribeEvent
    static void onEat(LivingEntityUseItemEvent.Finish event) {
        var entity = event.getEntity();
        if (entity.level().isClientSide() || !event.getItem().has(net.minecraft.core.component.DataComponents.FOOD)) return;
        float fresh = freshness(event.getItem(), entity.level().getGameTime());
        if (fresh >= 0 && fresh < ROTTING && entity.getRandom().nextBoolean()) entity.addEffect(new MobEffectInstance(MobEffects.HUNGER, 600, 0));
    }

    private Preserving() {}
}
