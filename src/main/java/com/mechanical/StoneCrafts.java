package com.mechanical;

import static com.mechanical.Mechanical.BLOCKS;
import static com.mechanical.Mechanical.ITEMS;
import static com.mechanical.Mechanical.MODID;

import java.util.EnumMap;
import java.util.List;
import java.util.Map;
import java.util.function.Consumer;
import java.util.stream.Stream;

import com.mechanical.wildlife.coast.OilLampBlock;

import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.gametest.framework.FunctionGameTestInstance;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.gametest.framework.TestEnvironmentDefinition;
import net.minecraft.resources.Identifier;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.context.UseOnContext;
import net.minecraft.world.item.crafting.CraftingInput;
import net.minecraft.world.item.crafting.RecipeType;
import net.minecraft.world.level.GameType;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.SoundType;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.material.MapColor;
import net.minecraft.world.level.material.PushReaction;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.Vec3;
import net.minecraft.core.Direction;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.RegisterGameTestsEvent;
import net.neoforged.neoforge.registries.DeferredBlock;
import net.neoforged.neoforge.registries.DeferredItem;
import net.neoforged.neoforge.registries.DeferredRegister;

/**
 * World-materials plan phase 9A: the stones' real uses. Soapstone Molds (carved at the Mold Bench, never crack), the
 * Quartzite Whetstone (half the strokes), the Rapakivi Quern (half the clicks), the Basalt Anvil (half the blows),
 * Pumice Chunks (polish rough stone without bronze), the Alabaster Lamp (an Oil Lamp that shines 15), and crushing
 * Conglomerate (Gravel + Flint), Shale (Clay Balls) and Pumice on the Stone Anvil. Jasper strikes a Flint and Steel
 * (recipe only). Data and textures: scripts/gen_stone_crafts.py; player docs: docs/ages/.
 */
@EventBusSubscriber(modid = MODID)
public final class StoneCrafts {
    public static final Map<ClayMoldBlock.Shape, DeferredBlock<ClayMoldBlock>> SOAPSTONE_MOLDS = new EnumMap<>(ClayMoldBlock.Shape.class);

    static {
        for (ClayMoldBlock.Shape s : ClayMoldBlock.Shape.values())
            SOAPSTONE_MOLDS.put(s, BLOCKS.registerBlock("soapstone_" + s.getSerializedName() + "_mold", p -> new ClayMoldBlock(p, s, false), p -> p
                    .mapColor(MapColor.COLOR_LIGHT_GREEN).strength(1.0F).sound(SoundType.TUFF).noOcclusion().pushReaction(PushReaction.POPPED)
                    .lightLevel(st -> st.getValue(ClayMoldBlock.FILL) == ClayMoldBlock.Fill.MOLTEN ? 9 : 0)));
    }

    public static final DeferredBlock<WhetstoneBlock> QUARTZITE_WHETSTONE = BLOCKS.registerBlock("quartzite_whetstone", p -> new WhetstoneBlock(p, 2), p -> p
            .mapColor(MapColor.TERRACOTTA_WHITE).strength(2.0F).sound(SoundType.STONE).noOcclusion());
    public static final DeferredBlock<QuernBlock> RAPAKIVI_QUERN = BLOCKS.registerBlock("rapakivi_quern", p -> new QuernBlock(p, 2), p -> p
            .mapColor(MapColor.TERRACOTTA_ORANGE).strength(2.0F).sound(SoundType.STONE).noOcclusion());
    public static final DeferredBlock<StoneAnvilBlock> BASALT_ANVIL = BLOCKS.registerBlock("basalt_anvil", StoneAnvilBlock::new, p -> p
            .mapColor(MapColor.COLOR_BLACK).strength(3.0F, 9.0F).sound(SoundType.BASALT).noOcclusion());
    public static final DeferredBlock<OilLampBlock> ALABASTER_LAMP = BLOCKS.registerBlock("alabaster_lamp", OilLampBlock::new, p -> p
            .mapColor(MapColor.SAND).strength(1.0F).sound(SoundType.STONE).noOcclusion().randomTicks()
            .lightLevel(s -> s.getValue(OilLampBlock.LIT) ? 15 : 0)); // the flame glows through the thin stone

    public static final int PUMICE_USES = 16;
    public static final DeferredItem<PumiceItem> PUMICE_CHUNK = ITEMS.registerItem("pumice_chunk", PumiceItem::new, p -> p.durability(PUMICE_USES));

    public static void init(IEventBus modEventBus) {
        TEST_FUNCTIONS.register(modEventBus);
    }

    /** One soapstone mold per cast shape, half a Polished Soapstone per metal unit (at least 1). */
    static Stream<StationRecipe> moldRecipes() {
        return SOAPSTONE_MOLDS.entrySet().stream().map(e -> StationRecipe.of(() -> e.getValue().get().asItem(), 1,
                StationRecipe.Part.of(() -> StoneFamilies.block("polished_soapstone").asItem(), Math.max(1, e.getKey().units / 2)), null));
    }

    /** Stone Anvil: Conglomerate -> 2 Gravel (+ a Flint, {@link #crushedExtra}), Shale -> 2 Clay Balls, Pumice -> a Pumice Chunk. */
    static ItemStack crushed(ItemStack stack) {
        if (is(stack, "conglomerate")) return new ItemStack(Items.GRAVEL, 2);
        if (is(stack, "shale")) return new ItemStack(Items.CLAY_BALL, 2);
        if (is(stack, "pumice")) return new ItemStack(PUMICE_CHUNK.get());
        return ItemStack.EMPTY;
    }

    /** The second thing a crushed piece leaves on the anvil, or empty. */
    static ItemStack crushedExtra(ItemStack stack) {
        return is(stack, "conglomerate") ? new ItemStack(Items.FLINT) : ItemStack.EMPTY;
    }

    private static boolean is(ItemStack stack, String stone) {
        return stack.is(StoneFamilies.block(stone).asItem());
    }

    /** What pumice polishes this block into: the Bronze Chisel's step when it leads to a polished or smooth block, else null. */
    public static Block polished(Block block) {
        Block to = BronzeTools.ChiselItem.carved(block);
        if (to == null) return null;
        String path = BuiltInRegistries.BLOCK.getKey(to).getPath();
        return path.startsWith("polished_") || path.startsWith("smooth_") ? to : null;
    }

    /** Pumice Chunk: right-click rough stone to rub it smooth (the Bronze Chisel's first step, no bronze needed); 1 wear per block. */
    public static class PumiceItem extends Item {
        public PumiceItem(Properties properties) {
            super(properties);
        }

        @Override
        public InteractionResult useOn(UseOnContext context) {
            Level level = context.getLevel();
            BlockPos pos = context.getClickedPos();
            BlockState state = level.getBlockState(pos);
            Block to = polished(state.getBlock());
            boolean scrub = to == null;
            if (scrub) to = Masonry.scrubbed(state.getBlock()); // moss scrubbed off weathered bricks (phase 10)
            if (to == null) return InteractionResult.PASS;
            if (level instanceof ServerLevel server) {
                server.levelEvent(2001, pos, Block.getId(state)); // stone dust
                server.setBlock(pos, to.withPropertiesOf(state), Block.UPDATE_ALL);
                server.playSound(null, pos, SoundEvents.SAND_HIT, SoundSource.BLOCKS, 1.0F, 0.8F);
                if (context.getPlayer() != null) context.getItemInHand().hurtAndBreak(1, context.getPlayer(), context.getHand());
                if (scrub) Masonry.scrubbedBy(context.getPlayer());
                else if (context.getPlayer() instanceof ServerPlayer player) Kettle.award(player, Identifier.fromNamespaceAndPath(MODID, "stone_age/pumice_polish"));
            }
            return InteractionResult.SUCCESS;
        }
    }

    /** Called by the Stone Anvil when a piece is crushed, for the "Rubble" advancement. */
    static void crushedBy(Player player, ItemStack piece) {
        if (!(player instanceof ServerPlayer p)) return;
        com.mechanical.skills.Skills.addXp(p, com.mechanical.skills.Skill.MASONRY, 5);
        if (is(piece, "conglomerate") || is(piece, "shale")) Kettle.award(p, Identifier.fromNamespaceAndPath(MODID, "bronze_age/rubble"));
    }

    // ---------------------------------------------------------------- game test

    private static final DeferredRegister<Consumer<GameTestHelper>> TEST_FUNCTIONS = DeferredRegister.create(Registries.TEST_FUNCTION, MODID);
    private static final ResourceKey<Consumer<GameTestHelper>> TEST = ResourceKey.create(Registries.TEST_FUNCTION, Identifier.fromNamespaceAndPath(MODID, "stone_crafts"));

    static {
        TEST_FUNCTIONS.register("stone_crafts", () -> StoneCrafts::test);
    }

    @SubscribeEvent
    static void registerTests(RegisterGameTestsEvent event) {
        var env = event.registerEnvironment(Identifier.fromNamespaceAndPath(MODID, "stone_crafts"), new TestEnvironmentDefinition.AllOf(List.of()));
        event.registerTest(Identifier.fromNamespaceAndPath(MODID, "stone_crafts"),
                new FunctionGameTestInstance(TEST, TestArea.of(env, 40, true)));
    }

    private static void test(GameTestHelper h) {
        try {
            run(h);
        } catch (RuntimeException e) {
            e.printStackTrace();
            throw h.assertionException(e + " at " + java.util.Arrays.toString(java.util.Arrays.copyOf(e.getStackTrace(), 6)));
        }
    }

    private static BlockHitResult hit(GameTestHelper h, BlockPos rel) {
        return new BlockHitResult(Vec3.atCenterOf(h.absolutePos(rel)), Direction.UP, h.absolutePos(rel), false);
    }

    private static ItemStack craft(GameTestHelper h, int w, int ht, ItemStack... items) {
        CraftingInput in = CraftingInput.of(w, ht, List.of(items));
        return h.getLevel().recipeAccess().getRecipeFor(RecipeType.CRAFTING, in, h.getLevel()).map(r -> r.value().assemble(in)).orElse(ItemStack.EMPTY);
    }

    private static void run(GameTestHelper h) {
        ServerLevel level = h.getLevel();
        Player player = h.makeMockPlayer(GameType.SURVIVAL);
        BlockPos floor = new BlockPos(1, 0, 1), at = floor.above();
        h.setBlock(floor, Blocks.STONE);

        // 1. soapstone mold: carved at the Mold Bench, casts more than a clay mold's 5 times and never cracks
        ClayMoldBlock ingotMold = SOAPSTONE_MOLDS.get(ClayMoldBlock.Shape.INGOT).get();
        h.assertTrue(MoldBench.ALL.stream().anyMatch(r -> r.result().get() == ingotMold.asItem()
                && r.parts().get(0).item().get() == StoneFamilies.block("polished_soapstone").asItem()), "Mold Bench: Polished Soapstone -> Soapstone Ingot Mold");
        h.assertTrue(SOAPSTONE_MOLDS.size() == ClayMoldBlock.Shape.values().length, "a soapstone mold for every shape");
        for (int i = 0; i < ClayMoldBlock.MAX_USES + 2; i++) {
            h.setBlock(at, ingotMold.defaultBlockState().setValue(ClayMoldBlock.FILL, ClayMoldBlock.Fill.SOLID).setValue(ClayMoldBlock.METAL, Metal.COPPER));
            BlockState s = h.getBlockState(at);
            s.useWithoutItem(level, player, hit(h, at));
            h.assertTrue(h.getBlockState(at).is(ingotMold) && h.getBlockState(at).getValue(ClayMoldBlock.FILL) == ClayMoldBlock.Fill.EMPTY,
                    "soapstone mold still whole after cast " + (i + 1));
        }
        h.assertTrue(player.getInventory().countItem(Items.COPPER_INGOT) == ClayMoldBlock.MAX_USES + 2, "every cast gave an ingot");

        // 2. quartzite whetstone: 2 strokes instead of 4
        h.setBlock(at, QUARTZITE_WHETSTONE.get());
        ItemStack rough = new ItemStack(Casting.ROUGH_CAST_COPPER_PICKAXE_HEAD.get());
        player.setItemInHand(InteractionHand.MAIN_HAND, rough);
        for (int i = 0; i < 2; i++) {
            for (int t = 0; t < 10; t++) player.getCooldowns().tick();
            h.getBlockState(at).useItemOn(rough, level, player, InteractionHand.MAIN_HAND, hit(h, at));
        }
        h.assertTrue(player.getInventory().countItem(Casting.COPPER_PICKAXE_HEAD.get()) == 1, "Quartzite Whetstone: a head in 2 strokes");

        // 3. rapakivi quern: 2 clicks per Flour instead of 4
        h.setBlock(at, RAPAKIVI_QUERN.get());
        ItemStack grain = new ItemStack(Items.WHEAT_SEEDS, 4);
        player.setItemInHand(InteractionHand.MAIN_HAND, grain);
        for (int i = 0; i < 2; i++) h.getBlockState(at).useItemOn(grain, level, player, InteractionHand.MAIN_HAND, hit(h, at));
        h.assertTrue(grain.getCount() == 3, "Rapakivi Quern: one Grain ground in 2 clicks");

        // 4. pumice: crushed from a Pumice block, polishes Soapstone and vanilla Andesite, not bricks
        h.assertTrue(BronzeOres.crushed(new ItemStack(StoneFamilies.block("pumice"))).is(PUMICE_CHUNK.get()), "Pumice -> Pumice Chunk on the Stone Anvil");
        h.assertTrue(polished(StoneFamilies.block("soapstone")) == StoneFamilies.block("polished_soapstone"), "pumice polishes Soapstone");
        h.assertTrue(polished(Blocks.ANDESITE) == Blocks.POLISHED_ANDESITE && polished(Blocks.STONE_BRICKS) == null, "pumice polishes, it doesn't carve");
        ItemStack pumice = new ItemStack(PUMICE_CHUNK.get());
        player.setItemInHand(InteractionHand.MAIN_HAND, pumice);
        h.setBlock(at, StoneFamilies.block("soapstone"));
        pumice.useOn(new UseOnContext(player, InteractionHand.MAIN_HAND, hit(h, at)));
        h.assertTrue(h.getBlockState(at).is(StoneFamilies.block("polished_soapstone")) && pumice.getDamageValue() == 1, "Pumice Chunk: Soapstone -> Polished Soapstone, 1 wear");

        // 5. crushing on the Stone Anvil
        h.assertTrue(BronzeOres.crushed(new ItemStack(StoneFamilies.block("conglomerate"))).is(Items.GRAVEL)
                && crushedExtra(new ItemStack(StoneFamilies.block("conglomerate"))).is(Items.FLINT), "Conglomerate -> Gravel + Flint");
        h.assertTrue(BronzeOres.crushed(new ItemStack(StoneFamilies.block("shale"))).is(Items.CLAY_BALL), "Shale -> Clay Balls");

        // 6. jasper strikes a light
        h.assertTrue(craft(h, 2, 1, new ItemStack(item("rough_jasper")), new ItemStack(Smithing.STEEL_PLATE.get())).is(Mechanical.FLINT_AND_STEEL.get()),
                "Rough Jasper + Steel Plate: Flint and Steel");
        h.assertTrue(craft(h, 2, 1, new ItemStack(item("jasper")), new ItemStack(Smithing.STEEL_PLATE.get())).is(Mechanical.FLINT_AND_STEEL.get()),
                "Polished Jasper + Steel Plate: Flint and Steel");

        // 7. basalt anvil: crushes in 2 blows
        h.setBlock(at, BASALT_ANVIL.get());
        StoneAnvilBlockEntity anvil = (StoneAnvilBlockEntity) h.getBlockEntity(at, StoneAnvilBlockEntity.class);
        anvil.place(level, new ItemStack(Items.RAW_COPPER));
        ItemStack hammer = new ItemStack(Mechanical.FLINT_PICKAXE.get());
        for (int i = 0; i < StoneAnvilBlockEntity.HITS / 2; i++) anvil.strike(level, player, hammer, InteractionHand.MAIN_HAND);
        h.assertTrue(anvil.item().isEmpty(), "Basalt Anvil: crushed in " + StoneAnvilBlockEntity.HITS / 2 + " blows");

        // 8. alabaster lamp: fill, light, light 15
        h.setBlock(at, ALABASTER_LAMP.get());
        player.setItemInHand(InteractionHand.MAIN_HAND, new ItemStack(Mechanical.TALLOW.get()));
        h.getBlockState(at).useItemOn(player.getMainHandItem(), level, player, InteractionHand.MAIN_HAND, hit(h, at));
        player.setItemInHand(InteractionHand.MAIN_HAND, new ItemStack(Items.TORCH));
        h.getBlockState(at).useItemOn(player.getMainHandItem(), level, player, InteractionHand.MAIN_HAND, hit(h, at));
        h.assertTrue(h.getBlockState(at).getValue(OilLampBlock.LIT) && h.getBlockState(at).getLightEmission() == 15, "Alabaster Lamp lit: light 15");

        h.succeed();
    }

    private static Item item(String name) {
        return BuiltInRegistries.ITEM.getValue(Identifier.fromNamespaceAndPath(MODID, name));
    }

    private StoneCrafts() {}
}
