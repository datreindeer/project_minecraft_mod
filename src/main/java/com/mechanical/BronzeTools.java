package com.mechanical;

import static com.mechanical.Mechanical.ITEMS;
import static com.mechanical.Mechanical.MODID;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.function.Consumer;
import java.util.function.Supplier;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.component.DataComponents;
import net.minecraft.core.cauldron.CauldronInteractions;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.gametest.framework.FunctionGameTestInstance;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.gametest.framework.TestEnvironmentDefinition;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.stats.Stats;
import net.minecraft.tags.TagKey;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.damagesource.DamageType;
import net.minecraft.world.entity.AgeableMob;
import net.minecraft.world.entity.animal.cow.AbstractCow;
import net.minecraft.world.entity.animal.goat.Goat;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.ItemUtils;
import net.minecraft.world.item.ShearsItem;
import net.minecraft.world.item.ToolMaterial;
import net.minecraft.world.item.component.Consumables;
import net.minecraft.world.item.context.UseOnContext;
import net.minecraft.world.item.crafting.CraftingInput;
import net.minecraft.world.item.crafting.RecipeType;
import net.minecraft.world.level.GameType;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.CropBlock;
import net.minecraft.world.level.block.LayeredCauldronBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.gameevent.GameEvent;
import net.minecraft.world.level.material.Fluids;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.Vec3;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.common.util.FakePlayerFactory;
import net.neoforged.neoforge.event.RegisterCauldronInteractionEvent;
import net.neoforged.neoforge.event.RegisterGameTestsEvent;
import net.neoforged.neoforge.event.entity.player.PlayerInteractEvent;
import net.neoforged.neoforge.event.level.block.BreakBlockEvent;
import net.neoforged.neoforge.event.tick.PlayerTickEvent;
import net.neoforged.neoforge.registries.DeferredBlock;
import net.neoforged.neoforge.registries.DeferredItem;
import net.neoforged.neoforge.registries.DeferredRegister;

/**
 * More bronze tools, made like the others: a clay mold shaped at the Mold Bench and fired, bronze poured in, the casting
 * ground on the Whetstone, then (most of them) assembled at the Tool Station. Bronze Bucket (water, lava, milk; never
 * cracks), Bronze Sickle (reaps 3x3), Bronze Shears, Bronze Hook Fishing Rod, Bronze Needle, Bronze Tongs (handle molten
 * metal without burns, see {@link #HOT_METAL}), Bronze Chisel (carves stone) and Bronze Quarry Hammer (breaks stone 3x3,
 * {@link #quarry}). Data: scripts/gen_bronzetools.py,
 * textures: scripts/gen_bronzetools_textures.py, docs/ages/02-bronze-age.md "Fler bronsverktyg".
 */
@EventBusSubscriber(modid = MODID)
public final class BronzeTools {
    /** Bone Needle, Bronze Needle: what the Armor Station sews with. */
    public static final TagKey<Item> NEEDLES = TagKey.create(Registries.ITEM, Identifier.fromNamespaceAndPath(MODID, "needles"));
    /** Bronze Tongs, Iron Tongs: what keeps molten and hot metal from burning the hand ({@link #holdsTongs}). */
    public static final TagKey<Item> TONGS = TagKey.create(Registries.ITEM, Identifier.fromNamespaceAndPath(MODID, "tongs"));
    /** Burns from molten metal handled without Bronze Tongs. */
    public static final ResourceKey<DamageType> HOT_METAL = ResourceKey.create(Registries.DAMAGE_TYPE, Identifier.fromNamespaceAndPath(MODID, "hot_metal"));
    public static final float BURN_DAMAGE = 1.0F;
    /** A Molten Crucible in hand burns every this many ticks without tongs. */
    public static final int BURN_INTERVAL = 40;
    /** What a Quarry Hammer breaks: stone of every kind, and the ores in it ({@link #QUARRY_ORES}). */
    public static final TagKey<Block> QUARRY_MINEABLE = TagKey.create(Registries.BLOCK, Identifier.fromNamespaceAndPath(MODID, "quarry_hammer_mineable"));
    /** Ores a Quarry Hammer crumbles to their rock (#c:ores plus the gemstone ores). */
    public static final TagKey<Block> QUARRY_ORES = TagKey.create(Registries.BLOCK, Identifier.fromNamespaceAndPath(MODID, "quarry_ores"));
    /** Bronze's tier and durability at half its speed: a 3x3 face of stone a blow is slow, heavy work. */
    public static final ToolMaterial QUARRY_BRONZE = new ToolMaterial(Bronze.INCORRECT_FOR_BRONZE_TOOL, 460, 3.0F, 2.0F, 12, Bronze.BRONZE_TOOL_MATERIALS);
    /** Extra wear for each ore the hammer crumbles, and for a blow that rings off an ore too hard for it. */
    public static final int ORE_WEAR = 2, TOO_HARD_WEAR = 4;

    // --- molds (Clay Balls at the Mold Bench -> kiln 600°) ---
    public static final DeferredItem<Item> UNFIRED_CLAY_BUCKET_MOLD = ITEMS.registerSimpleItem("unfired_clay_bucket_mold");
    public static final DeferredItem<Item> UNFIRED_CLAY_SICKLE_BLADE_MOLD = ITEMS.registerSimpleItem("unfired_clay_sickle_blade_mold");
    public static final DeferredItem<Item> UNFIRED_CLAY_SHEARS_MOLD = ITEMS.registerSimpleItem("unfired_clay_shears_mold");
    public static final DeferredItem<Item> UNFIRED_CLAY_FISH_HOOKS_MOLD = ITEMS.registerSimpleItem("unfired_clay_fish_hooks_mold");
    public static final DeferredItem<Item> UNFIRED_CLAY_NEEDLES_MOLD = ITEMS.registerSimpleItem("unfired_clay_needles_mold");
    public static final DeferredItem<Item> UNFIRED_CLAY_TONGS_MOLD = ITEMS.registerSimpleItem("unfired_clay_tongs_mold");
    public static final DeferredItem<Item> UNFIRED_CLAY_CHISEL_BLADE_MOLD = ITEMS.registerSimpleItem("unfired_clay_chisel_blade_mold");
    public static final DeferredBlock<ClayMoldBlock> CLAY_BUCKET_MOLD = Casting.mold("clay_bucket_mold", ClayMoldBlock.Shape.BUCKET);
    public static final DeferredBlock<ClayMoldBlock> CLAY_SICKLE_BLADE_MOLD = Casting.mold("clay_sickle_blade_mold", ClayMoldBlock.Shape.SICKLE_BLADE);
    public static final DeferredBlock<ClayMoldBlock> CLAY_SHEARS_MOLD = Casting.mold("clay_shears_mold", ClayMoldBlock.Shape.SHEARS);
    public static final DeferredBlock<ClayMoldBlock> CLAY_FISH_HOOKS_MOLD = Casting.mold("clay_fish_hooks_mold", ClayMoldBlock.Shape.FISH_HOOKS);
    public static final DeferredBlock<ClayMoldBlock> CLAY_NEEDLES_MOLD = Casting.mold("clay_needles_mold", ClayMoldBlock.Shape.NEEDLES);
    public static final DeferredBlock<ClayMoldBlock> CLAY_TONGS_MOLD = Casting.mold("clay_tongs_mold", ClayMoldBlock.Shape.TONGS);
    public static final DeferredBlock<ClayMoldBlock> CLAY_CHISEL_BLADE_MOLD = Casting.mold("clay_chisel_blade_mold", ClayMoldBlock.Shape.CHISEL_BLADE);
    public static final DeferredItem<BlockItem> CLAY_BUCKET_MOLD_ITEM = ITEMS.registerSimpleBlockItem("clay_bucket_mold", CLAY_BUCKET_MOLD);
    public static final DeferredItem<BlockItem> CLAY_SICKLE_BLADE_MOLD_ITEM = ITEMS.registerSimpleBlockItem("clay_sickle_blade_mold", CLAY_SICKLE_BLADE_MOLD);
    public static final DeferredItem<BlockItem> CLAY_SHEARS_MOLD_ITEM = ITEMS.registerSimpleBlockItem("clay_shears_mold", CLAY_SHEARS_MOLD);
    public static final DeferredItem<BlockItem> CLAY_FISH_HOOKS_MOLD_ITEM = ITEMS.registerSimpleBlockItem("clay_fish_hooks_mold", CLAY_FISH_HOOKS_MOLD);
    public static final DeferredItem<BlockItem> CLAY_NEEDLES_MOLD_ITEM = ITEMS.registerSimpleBlockItem("clay_needles_mold", CLAY_NEEDLES_MOLD);
    public static final DeferredItem<BlockItem> CLAY_TONGS_MOLD_ITEM = ITEMS.registerSimpleBlockItem("clay_tongs_mold", CLAY_TONGS_MOLD);
    public static final DeferredItem<BlockItem> CLAY_CHISEL_BLADE_MOLD_ITEM = ITEMS.registerSimpleBlockItem("clay_chisel_blade_mold", CLAY_CHISEL_BLADE_MOLD);
    /** The gang molds (fish hooks, needles) cast this many on one sprue, ground apart on the Whetstone. */
    public static final int PER_GANG_CAST = 4;

    // --- castings, rough and ground ---
    public static final DeferredItem<Item> ROUGH_CAST_BRONZE_BUCKET = ITEMS.registerSimpleItem("rough_cast_bronze_bucket");
    public static final DeferredItem<Item> ROUGH_CAST_BRONZE_SICKLE_BLADE = ITEMS.registerSimpleItem("rough_cast_bronze_sickle_blade");
    public static final DeferredItem<Item> ROUGH_CAST_BRONZE_SHEARS = ITEMS.registerSimpleItem("rough_cast_bronze_shears");
    public static final DeferredItem<Item> ROUGH_CAST_BRONZE_FISH_HOOKS = ITEMS.registerSimpleItem("rough_cast_bronze_fish_hooks");
    public static final DeferredItem<Item> ROUGH_CAST_BRONZE_NEEDLES = ITEMS.registerSimpleItem("rough_cast_bronze_needles");
    public static final DeferredItem<Item> ROUGH_CAST_BRONZE_TONGS = ITEMS.registerSimpleItem("rough_cast_bronze_tongs");
    public static final DeferredItem<Item> ROUGH_CAST_BRONZE_CHISEL_BLADE = ITEMS.registerSimpleItem("rough_cast_bronze_chisel_blade");
    public static final DeferredItem<Item> BRONZE_BUCKET_BODY = ITEMS.registerSimpleItem("bronze_bucket_body");
    public static final DeferredItem<Item> BRONZE_SICKLE_BLADE = ITEMS.registerSimpleItem("bronze_sickle_blade");
    public static final DeferredItem<Item> BRONZE_FISH_HOOK = ITEMS.registerSimpleItem("bronze_fish_hook");
    public static final DeferredItem<Item> BRONZE_TONG_ARMS = ITEMS.registerSimpleItem("bronze_tong_arms");
    public static final DeferredItem<Item> BRONZE_CHISEL_BLADE = ITEMS.registerSimpleItem("bronze_chisel_blade");

    // --- tools ---
    public static final DeferredItem<BronzeBucketItem> BRONZE_BUCKET = ITEMS.registerItem("bronze_bucket",
            p -> new BronzeBucketItem(Fluids.EMPTY, p), p -> p.stacksTo(16));
    public static final DeferredItem<BronzeBucketItem> BRONZE_WATER_BUCKET = ITEMS.registerItem("bronze_water_bucket",
            p -> new BronzeBucketItem(Fluids.WATER, p), p -> p.stacksTo(1));
    // shortcut: drinking or baking with milk hands back a fresh Bronze Bucket, so a Poor Bronze one comes back sound; track it if that matters
    public static final DeferredItem<Item> BRONZE_MILK_BUCKET = ITEMS.registerSimpleItem("bronze_milk_bucket", p -> p.stacksTo(1)
            .craftRemainder(BRONZE_BUCKET.get()).component(DataComponents.CONSUMABLE, Consumables.MILK_BUCKET).usingConvertsTo(BRONZE_BUCKET.get()));
    /** A kettle-full of blood under a Hanging Rack scooped with a Bronze Bucket ({@link HangingRackBlock}); goes into Blood Sausage. */
    // shortcut: crafting with it hands back a fresh Bronze Bucket, so a Poor Bronze one comes back sound (as the milk bucket); track it if that matters
    public static final DeferredItem<Item> BRONZE_BLOOD_BUCKET = ITEMS.registerSimpleItem("bronze_blood_bucket", p -> p.stacksTo(1).craftRemainder(BRONZE_BUCKET.get()));
    /** Flint Sickle's tool on bronze (460 uses, iron speed), and it reaps a 3x3 swath of ripe crops or grass ({@link #onBreak}). */
    public static final DeferredItem<Item> BRONZE_SICKLE = ITEMS.registerSimpleItem("bronze_sickle",
            p -> p.tool(Bronze.BRONZE, Farming.SICKLE_MINEABLE, 1.5F, -2.2F, 0.0F));
    /** Vanilla shears (sheep, leaves, cobwebs, pumpkins, beehives, tripwire) in bronze: twice iron shears' 238 uses. */
    public static final DeferredItem<ShearsItem> BRONZE_SHEARS = ITEMS.registerItem("bronze_shears", ShearsItem::new,
            p -> p.durability(476).component(DataComponents.TOOL, ShearsItem.createToolProperties()));
    /** Bites come as with Lure II (10 s sooner) plus Luck of the Sea I; the Bone Hook rod is Lure I. */
    public static final DeferredItem<BoneHookRodItem> BRONZE_HOOK_FISHING_ROD = ITEMS.registerItem("bronze_hook_fishing_rod",
            p -> new BoneHookRodItem(p, 200, 1), p -> p.durability(128).enchantable(1));
    /** Sews at the Armor Station (#mechanical:needles) and lasts about 8 times as long as a Bone Needle. */
    public static final DeferredItem<Item> BRONZE_NEEDLE = ITEMS.registerSimpleItem("bronze_needle", p -> p.durability(250));
    /** Held in either hand: molten crucibles and fresh castings don't burn ({@link #scald}, {@link #takeHotCasting}). */
    public static final DeferredItem<Item> BRONZE_TONGS = ITEMS.registerSimpleItem("bronze_tongs", p -> p.durability(250));
    public static final DeferredItem<ChiselItem> BRONZE_CHISEL = ITEMS.registerItem("bronze_chisel", p -> new ChiselItem(p, 2), p -> p.durability(320));
    /** Breaks the 3x3 face of stone it hits ({@link #quarry}); on anything else it is a bare hand. */
    public static final DeferredItem<Item> BRONZE_QUARRY_HAMMER = ITEMS.registerSimpleItem("bronze_quarry_hammer",
            p -> p.tool(QUARRY_BRONZE, QUARRY_MINEABLE, 6.0F, -3.4F, 0.0F));
    /** Sawn from a Yew Rough Log with the Bronze Saw (crafting grid); the Tool Station strings it into a Yew Longbow. */
    public static final DeferredItem<Item> YEW_STAVE = ITEMS.registerSimpleItem("yew_stave", p -> p.stacksTo(16));
    public static final DeferredItem<YewLongbowItem> YEW_LONGBOW = ITEMS.registerItem("yew_longbow", YewLongbowItem::new, p -> p.durability(450).enchantable(1));

    /** Rough cast -> what the Whetstone grinds it into (fish hooks and needles: {@link #PER_GANG_CAST} of them). */
    private static final Map<Supplier<Item>, Supplier<? extends Item>> GROUND = Map.of(
            ROUGH_CAST_BRONZE_BUCKET, BRONZE_BUCKET_BODY, ROUGH_CAST_BRONZE_SICKLE_BLADE, BRONZE_SICKLE_BLADE,
            ROUGH_CAST_BRONZE_SHEARS, BRONZE_SHEARS, ROUGH_CAST_BRONZE_FISH_HOOKS, BRONZE_FISH_HOOK,
            ROUGH_CAST_BRONZE_NEEDLES, BRONZE_NEEDLE, ROUGH_CAST_BRONZE_TONGS, BRONZE_TONG_ARMS,
            ROUGH_CAST_BRONZE_CHISEL_BLADE, BRONZE_CHISEL_BLADE);

    /** The ground piece for one of these rough castings (Poor Bronze stays poor), or empty. */
    static ItemStack ground(ItemStack rough) {
        for (var e : GROUND.entrySet()) {
            if (!rough.is(e.getKey().get())) continue;
            boolean gang = rough.is(ROUGH_CAST_BRONZE_FISH_HOOKS.get()) || rough.is(ROUGH_CAST_BRONZE_NEEDLES.get());
            ItemStack out = new ItemStack(e.getValue().get(), gang ? PER_GANG_CAST : 1);
            if (Bronze.isPoor(rough)) Bronze.markPoor(out);
            return out;
        }
        return Ironworking.ground(rough);
    }

    public static boolean isBucket(ItemStack s) {
        return s.is(BRONZE_BUCKET.get()) || s.is(BRONZE_WATER_BUCKET.get()) || s.is(BRONZE_MILK_BUCKET.get())
                || s.is(BRONZE_BUCKET_BODY.get()) || s.is(ROUGH_CAST_BRONZE_BUCKET.get());
    }

    // ---------------------------------------------------------------- bucket: milk and cauldrons

    /** Milking a cow (mooshroom too) or goat into a Bronze Bucket, as vanilla does with its bucket. */
    @SubscribeEvent
    static void onEntityInteract(PlayerInteractEvent.EntityInteract event) {
        ItemStack stack = event.getItemStack();
        if (!stack.is(BRONZE_BUCKET.get()) || !(event.getTarget() instanceof AgeableMob mob) || mob.isBaby()
                || !(mob instanceof AbstractCow || mob instanceof Goat)) return;
        Player player = event.getEntity();
        player.playSound(mob instanceof Goat ? SoundEvents.GOAT_MILK : SoundEvents.COW_MILK, 1.0F, 1.0F);
        player.setItemInHand(event.getHand(), ItemUtils.createFilledResult(stack, player, stack.transmuteCopy(BRONZE_MILK_BUCKET.get(), 1)));
        event.setCanceled(true);
        event.setCancellationResult(InteractionResult.SUCCESS);
    }

    @SubscribeEvent
    static void onCauldrons(RegisterCauldronInteractionEvent.Interaction event) {
        event.registerToAll(BRONZE_WATER_BUCKET.get(), (state, level, pos, player, hand, stack) -> cauldron(level, pos, player, hand, stack,
                BRONZE_BUCKET.get(), Blocks.WATER_CAULDRON.defaultBlockState().setValue(LayeredCauldronBlock.LEVEL, 3), SoundEvents.BUCKET_EMPTY));
        CauldronInteractions.WATER.put(BRONZE_BUCKET.get(), (state, level, pos, player, hand, stack) -> state.getValue(LayeredCauldronBlock.LEVEL) < 3
                ? InteractionResult.TRY_WITH_EMPTY_HAND
                : cauldron(level, pos, player, hand, stack, BRONZE_WATER_BUCKET.get(), Blocks.CAULDRON.defaultBlockState(), SoundEvents.BUCKET_FILL));
        // no bucket of ours carries lava: the iron (vanilla) bucket can't scoop it out of a cauldron either
        CauldronInteractions.LAVA.put(net.minecraft.world.item.Items.BUCKET, (state, level, pos, player, hand, stack) -> InteractionResult.TRY_WITH_EMPTY_HAND);
    }

    /** The iron (vanilla) bucket won't scoop lava: aimed at lava, the click does nothing. */
    @SubscribeEvent
    static void onBucketUse(PlayerInteractEvent.RightClickItem event) {
        if (!event.getItemStack().is(net.minecraft.world.item.Items.BUCKET)) return;
        Player player = event.getEntity();
        if (player.pick(player.blockInteractionRange(), 1.0F, true) instanceof BlockHitResult hit
                && player.level().getFluidState(hit.getBlockPos()).is(net.minecraft.tags.FluidTags.LAVA)) {
            event.setCanceled(true);
            event.setCancellationResult(InteractionResult.FAIL);
        }
    }

    /** Pours a bronze bucket into a cauldron or scoops one out: the bucket becomes {@code into}, the cauldron {@code cauldron}. */
    private static InteractionResult cauldron(Level level, BlockPos pos, Player player, InteractionHand hand, ItemStack stack, Item into,
            BlockState cauldron, SoundEvent sound) {
        if (!level.isClientSide()) {
            boolean fills = into == BRONZE_BUCKET.get();
            player.setItemInHand(hand, ItemUtils.createFilledResult(stack, player, stack.transmuteCopy(into, 1)));
            player.awardStat(fills ? Stats.FILL_CAULDRON : Stats.USE_CAULDRON);
            level.setBlockAndUpdate(pos, cauldron);
            level.playSound(null, pos, sound, SoundSource.BLOCKS, 1.0F, 1.0F);
            level.gameEvent(null, fills ? GameEvent.FLUID_PLACE : GameEvent.FLUID_PICKUP, pos);
        }
        return InteractionResult.SUCCESS;
    }

    // ---------------------------------------------------------------- sickle: a 3x3 swath

    private static boolean reaping;

    /** Something a sickle cuts in one stroke: a ripe crop, or grass and other sickle plants that aren't crops. */
    static boolean reapable(BlockState state) {
        if (state.getBlock() instanceof CropBlock crop) return crop.isMaxAge(state);
        return state.is(Farming.SICKLE_MINEABLE);
    }

    /** How far round the cut block a sickle reaps: 1 (3x3) for bronze and iron, 2 (5x5) for steel, 0 for flint. */
    public static int sweepRadius(ItemStack tool) {
        if (tool.is(Smithing.STEEL_SICKLE.get())) return 2;
        return tool.is(BRONZE_SICKLE.get()) || tool.is(Smithing.IRON_SICKLE.get()) ? 1 : 0;
    }

    /** The other blocks within {@code radius} of {@code center} (same level) a sickle takes along: the same kind (crop or not) and reapable. */
    static List<BlockPos> swath(Level level, BlockPos center, BlockState cut, int radius) {
        List<BlockPos> out = new ArrayList<>();
        if (!reapable(cut)) return out;
        boolean crop = cut.getBlock() instanceof CropBlock;
        for (BlockPos p : BlockPos.betweenClosed(center.offset(-radius, 0, -radius), center.offset(radius, 0, radius))) {
            if (p.equals(center)) continue;
            BlockState s = level.getBlockState(p);
            if (reapable(s) && (s.getBlock() instanceof CropBlock) == crop) out.add(p.immutable());
        }
        return out;
    }

    /** Cutting with a metal sickle (not sneaking) reaps the swath around the block too; each ripe crop wears it as usual. */
    @SubscribeEvent
    static void onBreak(BreakBlockEvent event) {
        if (reaping || !(event.getPlayer() instanceof ServerPlayer player) || !(event.getLevel() instanceof ServerLevel level)
                || player.isShiftKeyDown()) return;
        Item sickle = player.getMainHandItem().getItem();
        int radius = sweepRadius(player.getMainHandItem());
        if (radius == 0) return;
        reaping = true;
        try {
            for (BlockPos p : swath(level, event.getPos(), event.getState(), radius)) {
                if (!player.getMainHandItem().is(sickle)) break; // worn out mid-swath
                player.gameMode.destroyBlock(p);
            }
        } finally {
            reaping = false;
        }
    }

    // ---------------------------------------------------------------- quarry hammer: a 3x3 face of stone

    /** The Bronze, Iron or Steel Quarry Hammer (Smithing): each takes the ores a pickaxe of its metal mines. */
    public static boolean isQuarryHammer(ItemStack stack) {
        return stack.is(BRONZE_QUARRY_HAMMER.get()) || stack.is(Smithing.IRON_QUARRY_HAMMER.get()) || stack.is(Smithing.STEEL_QUARRY_HAMMER.get());
    }

    private static boolean quarrying;

    /** The 9 blocks around {@code center} across {@code axis}: the face the player hit. */
    static List<BlockPos> face(BlockPos center, Direction.Axis axis) {
        List<BlockPos> out = new ArrayList<>();
        for (int a = -1; a <= 1; a++) for (int b = -1; b <= 1; b++) out.add(switch (axis) {
            case X -> center.offset(0, a, b);
            case Y -> center.offset(a, 0, b);
            case Z -> center.offset(a, b, 0);
        });
        return out;
    }

    /** The rock an ore sits in, told by its name (deepslate_, nether_, sandstone_ ores; stone otherwise). */
    static BlockState host(BlockState ore) {
        String id = BuiltInRegistries.BLOCK.getKey(ore.getBlock()).getPath();
        return (id.startsWith("deepslate_") ? Blocks.DEEPSLATE : id.startsWith("nether_") ? Blocks.NETHERRACK
                : id.startsWith("sandstone_") ? Blocks.SANDSTONE : Blocks.STONE).defaultBlockState();
    }

    /** Hitting stone with the Quarry Hammer breaks the face the player looks at instead (the clicked block too). */
    @SubscribeEvent
    static void onQuarry(BreakBlockEvent event) {
        if (quarrying || !(event.getPlayer() instanceof ServerPlayer player) || !(event.getLevel() instanceof ServerLevel level)
                || !isQuarryHammer(player.getMainHandItem()) || !event.getState().is(QUARRY_MINEABLE)) return;
        event.setCanceled(true);
        quarry(level, player, event.getPos(), ((BlockHitResult) player.pick(player.blockInteractionRange(), 1.0F, false)).getDirection().getAxis());
    }

    /**
     * Breaks the stone and ores of the face; an ore the hammer's tier could mine crumbles to its rock and wears the hammer
     * {@link #ORE_WEAR} more, one beyond its tier stops the whole blow ({@link #TOO_HARD_WEAR}). False if nothing broke.
     */
    static boolean quarry(ServerLevel level, ServerPlayer player, BlockPos center, Direction.Axis axis) {
        List<BlockPos> face = face(center, axis);
        ItemStack hammer = player.getMainHandItem();
        // the tool's own rules: an ore its tier can't take isn't "correct for drops"
        if (face.stream().map(level::getBlockState).anyMatch(s -> s.is(QUARRY_ORES) && !hammer.isCorrectToolForDrops(s))) {
            player.getMainHandItem().hurtAndBreak(TOO_HARD_WEAR, player, InteractionHand.MAIN_HAND);
            player.sendOverlayMessage(Component.translatable("message.mechanical.quarry_hammer.too_hard"));
            return false;
        }
        boolean ore = false;
        quarrying = true;
        try {
            for (BlockPos p : face) {
                if (!isQuarryHammer(player.getMainHandItem())) break; // worn out mid-blow
                BlockState s = level.getBlockState(p);
                if (!s.is(QUARRY_MINEABLE)) continue;
                boolean crumbles = s.is(QUARRY_ORES);
                if (crumbles) level.setBlock(p, host(s), Block.UPDATE_CLIENTS);
                player.gameMode.destroyBlock(p);
                if (crumbles) player.getMainHandItem().hurtAndBreak(ORE_WEAR, player, InteractionHand.MAIN_HAND);
                ore |= crumbles;
            }
        } finally {
            quarrying = false;
        }
        if (ore) player.sendOverlayMessage(Component.translatable("message.mechanical.quarry_hammer.ore"));
        return true;
    }

    // ---------------------------------------------------------------- tongs and burns

    public static boolean holdsTongs(Player player) {
        return player.getMainHandItem().is(TONGS) || player.getOffhandItem().is(TONGS);
    }

    public static boolean hot(ItemStack stack, long now) {
        return stack.is(Casting.MOLTEN_CRUCIBLE.get()) && !CrucibleItem.isCold(stack, now) || Ironworking.isHot(stack, now);
    }

    /** A Molten Crucible held in a hand burns unless Bronze Tongs are in a hand too; true if it burnt. */
    static boolean scald(ServerLevel level, Player player) {
        long now = level.getGameTime();
        if (player.isCreative() || player.isSpectator() || holdsTongs(player)) return false;
        if (!hot(player.getMainHandItem(), now) && !hot(player.getOffhandItem(), now)) return false;
        player.sendOverlayMessage(Component.translatable("message.mechanical.hot_metal.crucible"));
        return player.hurtServer(level, level.damageSources().source(HOT_METAL), BURN_DAMAGE);
    }

    @SubscribeEvent
    static void onPlayerTick(PlayerTickEvent.Post event) {
        if (event.getEntity().tickCount % BURN_INTERVAL == 0 && event.getEntity().level() instanceof ServerLevel level) scald(level, event.getEntity());
    }

    /** Taking a fresh casting out of a Clay Mold: tongs lose 1, a bare hand gets burnt. */
    static void takeHotCasting(ServerLevel level, Player player) {
        if (player.isCreative()) return;
        if (holdsTongs(player)) {
            InteractionHand hand = player.getMainHandItem().is(TONGS) ? InteractionHand.MAIN_HAND : InteractionHand.OFF_HAND;
            player.getItemInHand(hand).hurtAndBreak(1, player, hand);
            return;
        }
        player.sendOverlayMessage(Component.translatable("message.mechanical.hot_metal.casting"));
        player.hurtServer(level, level.damageSources().source(HOT_METAL), BURN_DAMAGE);
    }

    // ---------------------------------------------------------------- chisel

    /** Right-click stone with it: one step more worked (stone -> smooth -> bricks -> chiseled ...), 1 durability each. */
    public static class ChiselItem extends Item {
        private static Map<Block, Block> carved;
        /** Grinding strokes one Whetstone stroke counts for with this chisel in the other hand (1 without). */
        public final int whetStrokes;

        public ChiselItem(Properties properties, int whetStrokes) {
            super(properties);
            this.whetStrokes = whetStrokes;
        }

        /** What the chisel turns this block into, or null. */
        public static Block carved(Block block) {
            if (carved == null) {
                var map = new java.util.HashMap<>(Map.ofEntries(
                    Map.entry(Blocks.STONE, Blocks.SMOOTH_STONE), Map.entry(Blocks.SMOOTH_STONE, Blocks.STONE_BRICKS),
                    Map.entry(Blocks.STONE_BRICKS, Blocks.CHISELED_STONE_BRICKS),
                    Map.entry(Blocks.ANDESITE, Blocks.POLISHED_ANDESITE), Map.entry(Blocks.DIORITE, Blocks.POLISHED_DIORITE),
                    Map.entry(Blocks.GRANITE, Blocks.POLISHED_GRANITE),
                    Map.entry(Blocks.SANDSTONE, Blocks.CUT_SANDSTONE), Map.entry(Blocks.CUT_SANDSTONE, Blocks.CHISELED_SANDSTONE),
                    Map.entry(Blocks.RED_SANDSTONE, Blocks.CUT_RED_SANDSTONE), Map.entry(Blocks.CUT_RED_SANDSTONE, Blocks.CHISELED_RED_SANDSTONE),
                    Map.entry(Blocks.COBBLED_DEEPSLATE, Blocks.POLISHED_DEEPSLATE), Map.entry(Blocks.POLISHED_DEEPSLATE, Blocks.DEEPSLATE_BRICKS),
                    Map.entry(Blocks.DEEPSLATE_BRICKS, Blocks.CHISELED_DEEPSLATE),
                    Map.entry(Blocks.TUFF, Blocks.POLISHED_TUFF), Map.entry(Blocks.POLISHED_TUFF, Blocks.TUFF_BRICKS),
                    Map.entry(Blocks.TUFF_BRICKS, Blocks.CHISELED_TUFF_BRICKS),
                    Map.entry(Blocks.BLACKSTONE, Blocks.POLISHED_BLACKSTONE), Map.entry(Blocks.POLISHED_BLACKSTONE, Blocks.POLISHED_BLACKSTONE_BRICKS),
                    Map.entry(Blocks.POLISHED_BLACKSTONE_BRICKS, Blocks.CHISELED_POLISHED_BLACKSTONE),
                    Map.entry(Blocks.NETHER_BRICKS, Blocks.CHISELED_NETHER_BRICKS), Map.entry(Blocks.QUARTZ_BLOCK, Blocks.CHISELED_QUARTZ_BLOCK),
                    Map.entry(Blocks.RESIN_BRICKS, Blocks.CHISELED_RESIN_BRICKS)));
                map.putAll(StoneFamilies.chisel()); // Limestone, Slate, Gneiss ...
                carved = map;
            }
            return carved.get(block);
        }

        @Override
        public InteractionResult useOn(UseOnContext context) {
            Level level = context.getLevel();
            BlockPos pos = context.getClickedPos();
            BlockState state = level.getBlockState(pos);
            Block to = carved(state.getBlock());
            if (to == null) return InteractionResult.PASS;
            if (level instanceof ServerLevel server) {
                server.levelEvent(2001, pos, Block.getId(state)); // stone chips fly
                server.setBlock(pos, to.withPropertiesOf(state), Block.UPDATE_ALL);
                server.playSound(null, pos, SoundEvents.STONE_HIT, SoundSource.BLOCKS, 1.0F, 1.3F + server.getRandom().nextFloat() * 0.2F);
                if (context.getPlayer() != null) context.getItemInHand().hurtAndBreak(1, context.getPlayer(), context.getHand());
            }
            return InteractionResult.SUCCESS;
        }
    }

    // ---------------------------------------------------------------- game test

    private static final DeferredRegister<Consumer<GameTestHelper>> TEST_FUNCTIONS = DeferredRegister.create(Registries.TEST_FUNCTION, MODID);
    private static final ResourceKey<Consumer<GameTestHelper>> TEST = ResourceKey.create(Registries.TEST_FUNCTION, Identifier.fromNamespaceAndPath(MODID, "bronze_tools"));

    static {
        TEST_FUNCTIONS.register("bronze_tools", () -> BronzeTools::test);
    }

    public static void init(IEventBus modEventBus) {
        TEST_FUNCTIONS.register(modEventBus);
    }

    @SubscribeEvent
    static void registerTests(RegisterGameTestsEvent event) {
        var env = event.registerEnvironment(Identifier.fromNamespaceAndPath(MODID, "bronze_tools"), new TestEnvironmentDefinition.AllOf(List.of()));
        event.registerTest(Identifier.fromNamespaceAndPath(MODID, "bronze_tools"),
                new FunctionGameTestInstance(TEST, TestArea.of(env, 40, true)));
    }

    private static void test(GameTestHelper h) {
        ServerLevel level = h.getLevel();
        // bucket: lava stays put (no bucket of ours carries it), water scoops up
        BlockPos fluid = h.absolutePos(new BlockPos(1, 1, 1));
        level.setBlockAndUpdate(fluid, Blocks.LAVA.defaultBlockState());
        h.assertTrue(BronzeBucketItem.scoop(level, fluid, null, new ItemStack(BRONZE_BUCKET.get())).isEmpty()
                && level.getFluidState(fluid).is(net.minecraft.tags.FluidTags.LAVA), "the Bronze Bucket leaves lava alone");
        level.setBlockAndUpdate(fluid, Blocks.WATER.defaultBlockState());
        h.assertTrue(BronzeBucketItem.scoop(level, fluid, null, new ItemStack(BRONZE_BUCKET.get())).is(BRONZE_WATER_BUCKET.get()), "and water");
        level.setBlockAndUpdate(fluid, Blocks.AIR.defaultBlockState());
        // as water in a recipe it comes back empty
        CraftingInput in = CraftingInput.of(3, 1, List.of(new ItemStack(Farming.FLOUR.get()), new ItemStack(Farming.FLOUR.get()), new ItemStack(BRONZE_WATER_BUCKET.get())));
        var dough = level.recipeAccess().getRecipeFor(RecipeType.CRAFTING, in, level).orElseThrow(() -> h.assertionException("no Dough from a Bronze Water Bucket"));
        h.assertTrue(dough.value().getRemainingItems(in).get(2).is(BRONZE_BUCKET.get()), "the Bronze Bucket stays in the grid");

        // sickle: a 3x3 of ripe wheat on farmland goes in one cut
        BlockState ripe = ((CropBlock) Blocks.WHEAT).getStateForAge(((CropBlock) Blocks.WHEAT).getMaxAge());
        for (int x = 3; x <= 5; x++) for (int z = 0; z <= 2; z++) {
            h.setBlock(new BlockPos(x, 0, z), Blocks.FARMLAND);
            h.setBlock(new BlockPos(x, 1, z), ripe);
        }
        h.setBlock(new BlockPos(6, 0, 1), Blocks.FARMLAND);
        h.setBlock(new BlockPos(6, 1, 1), Blocks.WHEAT.defaultBlockState()); // young: outside the swath anyway
        ServerPlayer reaper = FakePlayerFactory.getMinecraft(level);
        reaper.setGameMode(GameType.SURVIVAL);
        reaper.setItemInHand(InteractionHand.MAIN_HAND, new ItemStack(BRONZE_SICKLE.get()));
        h.assertTrue(swath(level, h.absolutePos(new BlockPos(4, 1, 1)), ripe, sweepRadius(new ItemStack(BRONZE_SICKLE.get()))).size() == 8, "the swath is the 8 around");
        reaper.gameMode.destroyBlock(h.absolutePos(new BlockPos(4, 1, 1)));
        for (int x = 3; x <= 5; x++) for (int z = 0; z <= 2; z++) h.assertBlockNotPresent(Blocks.WHEAT, new BlockPos(x, 1, z));
        h.assertBlockPresent(Blocks.WHEAT, new BlockPos(6, 1, 1));
        reaper.setItemInHand(InteractionHand.MAIN_HAND, ItemStack.EMPTY);

        // tongs: a molten crucible in hand burns, unless tongs are in the other hand
        Player smith = h.makeMockPlayer(GameType.SURVIVAL);
        smith.setItemInHand(InteractionHand.MAIN_HAND, CrucibleItem.molten(CrucibleContents.EMPTY.add(Metal.BRONZE, 4), level.getGameTime()));
        float before = smith.getHealth();
        h.assertTrue(scald(level, smith) && smith.getHealth() < before, "molten metal in a bare hand burns");
        Player careful = h.makeMockPlayer(GameType.SURVIVAL);
        careful.setItemInHand(InteractionHand.MAIN_HAND, CrucibleItem.molten(CrucibleContents.EMPTY.add(Metal.BRONZE, 4), level.getGameTime()));
        careful.setItemInHand(InteractionHand.OFF_HAND, new ItemStack(BRONZE_TONGS.get()));
        h.assertFalse(scald(level, careful), "Bronze Tongs in the other hand: no burn");

        // chisel: stone bricks -> chiseled stone bricks, 1 durability
        BlockPos stone = new BlockPos(1, 1, 4);
        h.setBlock(stone, Blocks.STONE_BRICKS);
        Player mason = h.makeMockPlayer(GameType.SURVIVAL);
        ItemStack chisel = new ItemStack(BRONZE_CHISEL.get());
        mason.setItemInHand(InteractionHand.MAIN_HAND, chisel);
        BlockPos at = h.absolutePos(stone);
        chisel.useOn(new UseOnContext(level, mason, InteractionHand.MAIN_HAND, chisel, new BlockHitResult(Vec3.atCenterOf(at), net.minecraft.core.Direction.UP, at, false)));
        h.assertBlockPresent(Blocks.CHISELED_STONE_BRICKS, stone);
        h.assertTrue(mason.getMainHandItem().getDamageValue() == 1, "the chisel loses 1 durability");

        // every new rough casting grinds into its piece
        h.assertTrue(Casting.ground(new ItemStack(ROUGH_CAST_BRONZE_NEEDLES.get())).getCount() == PER_GANG_CAST, "4 needles per sprue");
        h.assertTrue(Casting.ground(new ItemStack(ROUGH_CAST_BRONZE_SHEARS.get())).is(BRONZE_SHEARS.get()), "rough shears grind into Bronze Shears");
        h.assertTrue(new ItemStack(BRONZE_NEEDLE.get()).is(NEEDLES) && new ItemStack(Mechanical.BONE_NEEDLE.get()).is(NEEDLES), "both needles sew");

        // quarry hammer: a 3x3 wall (x 8..10, y 1..3, z 4) with coal ore in the middle: all of it breaks, the ore as stone; gold ore stops it
        for (int x = 8; x <= 10; x++) for (int y = 1; y <= 3; y++) h.setBlock(new BlockPos(x, y, 4), Blocks.STONE);
        h.setBlock(new BlockPos(9, 2, 4), Blocks.COAL_ORE);
        h.setBlock(new BlockPos(9, 2, 3), Blocks.DIRT); // in front: not part of the face
        ServerPlayer quarrier = FakePlayerFactory.getMinecraft(level);
        quarrier.setGameMode(GameType.SURVIVAL);
        quarrier.setItemInHand(InteractionHand.MAIN_HAND, new ItemStack(BRONZE_QUARRY_HAMMER.get()));
        h.assertTrue(quarry(level, quarrier, h.absolutePos(new BlockPos(9, 2, 4)), Direction.Axis.Z), "the hammer strikes");
        for (int x = 8; x <= 10; x++) for (int y = 1; y <= 3; y++) h.assertBlockPresent(Blocks.AIR, new BlockPos(x, y, 4));
        h.assertBlockPresent(Blocks.DIRT, new BlockPos(9, 2, 3));
        h.assertTrue(quarrier.getMainHandItem().getDamageValue() == 9 + ORE_WEAR, "9 blocks and an ore's extra wear");
        h.assertFalse(new ItemStack(BRONZE_QUARRY_HAMMER.get()).isCorrectToolForDrops(Blocks.DIRT.defaultBlockState()), "no better than a hand on dirt");
        for (int x = 8; x <= 10; x++) for (int y = 1; y <= 3; y++) h.setBlock(new BlockPos(x, y, 4), Blocks.STONE);
        h.setBlock(new BlockPos(10, 3, 4), Blocks.GOLD_ORE);
        h.assertFalse(quarry(level, quarrier, h.absolutePos(new BlockPos(9, 2, 4)), Direction.Axis.Z), "gold is beyond bronze");
        h.assertBlockPresent(Blocks.STONE, new BlockPos(9, 2, 4));
        quarrier.setItemInHand(InteractionHand.MAIN_HAND, ItemStack.EMPTY);

        // Yew Longbow: the stave is sawn from a Yew Rough Log (the saw stays), the bow strung at the Tool Station
        CraftingInput staveIn = CraftingInput.of(2, 1, List.of(new ItemStack(Mechanical.ROUGH_LOGS.get("yew").get()), new ItemStack(Bronze.BRONZE_SAW.get())));
        var stave = level.recipeAccess().getRecipeFor(RecipeType.CRAFTING, staveIn, level).orElseThrow(() -> h.assertionException("no Yew Stave recipe"));
        h.assertTrue(stave.value().assemble(staveIn).is(YEW_STAVE.get()) && stave.value().getRemainingItems(staveIn).get(1).is(Bronze.BRONZE_SAW.get()),
                "Yew Rough Log + Bronze Saw -> Yew Stave, the saw stays");
        h.assertTrue(ToolStationRecipes.ALL.stream().anyMatch(r -> r.result().get() == YEW_LONGBOW.get()
                && r.matches(new ItemStack(com.mechanical.wildlife.Wildlife.SINEW.get()), new ItemStack(net.minecraft.world.item.Items.LEATHER), new ItemStack(YEW_STAVE.get()))),
                "Tool Station: Sinew + Leather + Yew Stave -> Yew Longbow");
        h.assertTrue(YewLongbowItem.powerForTime(20) < 0.9F && YewLongbowItem.powerForTime(YewLongbowItem.DRAW_TICKS) == 1.0F
                && YewLongbowItem.powerForTime(YewLongbowItem.DRAW_TICKS - 1) < 1.0F,
                "the longbow needs 30 ticks for a full draw");
        h.assertTrue(YewLongbowItem.SPEED_FACTOR > com.mechanical.wildlife.mountain.HornBowItem.SPEED_FACTOR, "heavier than the Horn Bow");
        h.assertTrue(new ItemStack(YEW_LONGBOW.get()).getMaxDamage() == 450, "450 uses");
        h.succeed();
    }

    private BronzeTools() {}
}
