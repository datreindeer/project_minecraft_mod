package com.mechanical;

import static com.mechanical.Mechanical.ITEMS;
import static com.mechanical.Mechanical.MODID;

import java.util.List;
import java.util.function.Consumer;

import net.minecraft.core.component.DataComponents;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.gametest.framework.FunctionGameTestInstance;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.gametest.framework.TestData;
import net.minecraft.gametest.framework.TestEnvironmentDefinition;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import net.minecraft.resources.ResourceKey;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.TransientCraftingContainer;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.item.component.TooltipDisplay;
import net.minecraft.world.item.component.WritableBookContent;
import net.minecraft.world.item.crafting.RecipeType;
import net.minecraft.world.level.GameType;
import net.minecraft.world.level.Level;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.RegisterGameTestsEvent;
import net.neoforged.neoforge.registries.DeferredItem;
import net.neoforged.neoforge.registries.DeferredRegister;

/**
 * World-materials phase 9B (docs/world-materials-plan.md "Fas 9B: resultat"): stone chemistry, writing and plaster.
 * Alum Shale roasted in the Clay Kiln and leached with water gives Alum, a mordant (more dye per pigment) and a tawing
 * salt (two hides per Alum). Gypsum crushed on the Stone Anvil and burnt at low heat is Plaster of Paris, a binder for
 * Lime Plaster without the bellows. The Slate Tablet is a one-page writing surface, written with Chalk and wiped by hand.
 * Kiln recipes: {@link KilnBlockEntity#RECIPES}; crushing: {@link #ground} via {@link Pigments#ground}; data: scripts/gen_chemistry.py.
 */
@EventBusSubscriber(modid = MODID)
public final class Chemistry {
    /** Alum shale roasts on Charcoal's own heat (no bellows); gypsum gives up its water on any fire. */
    public static final int ALUM_ROAST_TEMP = 800, PLASTER_TEMP = 300;

    public static final DeferredItem<Item> ROASTED_ALUM_SHALE = ITEMS.registerSimpleItem("roasted_alum_shale");
    public static final DeferredItem<Item> ALUM = ITEMS.registerSimpleItem("alum");
    public static final DeferredItem<Item> GYPSUM_POWDER = ITEMS.registerSimpleItem("gypsum_powder");
    public static final DeferredItem<Item> PLASTER_OF_PARIS = ITEMS.registerSimpleItem("plaster_of_paris");
    public static final DeferredItem<SlateTabletItem> SLATE_TABLET = ITEMS.registerItem("slate_tablet", SlateTabletItem::new,
            p -> p.stacksTo(1).component(DataComponents.WRITABLE_BOOK_CONTENT, WritableBookContent.EMPTY));

    /** Stone Anvil: gypsum to powder, and the chalk / ochre blocks straight to pigment (2 per item, as {@link Pigments#ground}). */
    static ItemStack ground(ItemStack stack) {
        if (stack.is(StoneFamilies.block("gypsum").asItem())) return new ItemStack(GYPSUM_POWDER.get(), 2);
        if (stack.is(StoneFamilies.block("chalk_block").asItem())) return new ItemStack(Pigments.WHITE_PIGMENT.get(), 8);
        if (stack.is(StoneFamilies.block("red_ochre_block").asItem())) return new ItemStack(Pigments.RED_PIGMENT.get(), 8);
        if (stack.is(StoneFamilies.block("yellow_ochre_block").asItem())) return new ItemStack(Pigments.YELLOW_PIGMENT.get(), 8);
        return ItemStack.EMPTY;
    }

    /**
     * A slate in a wooden frame: use it with Chalk in the other hand (one Chalk per writing) to write one page in the book
     * screen; sneak-use wipes it clean. Signing only saves the page (ServerGamePacketListenerImplMixin), it never turns into
     * a Written Book.
     */
    public static class SlateTabletItem extends Item {
        public static final int PAGES = 1;

        public SlateTabletItem(Properties properties) {
            super(properties);
        }

        @Override
        public InteractionResult use(Level level, Player player, InteractionHand hand) {
            ItemStack tablet = player.getItemInHand(hand);
            if (player.isShiftKeyDown()) {
                if (tablet.getOrDefault(DataComponents.WRITABLE_BOOK_CONTENT, WritableBookContent.EMPTY).pages().isEmpty()) return InteractionResult.PASS;
                tablet.set(DataComponents.WRITABLE_BOOK_CONTENT, WritableBookContent.EMPTY);
                level.playSound(player, player.blockPosition(), SoundEvents.BRUSH_GENERIC, SoundSource.PLAYERS, 1.0F, 1.2F);
                return InteractionResult.SUCCESS;
            }
            ItemStack chalk = player.getItemInHand(hand == InteractionHand.MAIN_HAND ? InteractionHand.OFF_HAND : InteractionHand.MAIN_HAND);
            if (!chalk.is(Pigments.CHALK.get())) {
                if (!level.isClientSide()) player.sendOverlayMessage(Component.translatable("message.mechanical.slate_tablet.no_chalk"));
                return InteractionResult.FAIL;
            }
            chalk.consume(1, player);
            player.openItemGui(tablet, hand);
            return InteractionResult.SUCCESS;
        }

        @Override
        public void appendHoverText(ItemStack stack, Item.TooltipContext context, TooltipDisplay display, Consumer<Component> builder, TooltipFlag flag) {
            var pages = stack.getOrDefault(DataComponents.WRITABLE_BOOK_CONTENT, WritableBookContent.EMPTY).pages();
            if (pages.isEmpty() || pages.get(0).raw().isBlank()) {
                builder.accept(Component.translatable("tooltip.mechanical.slate_tablet.blank").withStyle(net.minecraft.ChatFormatting.GRAY));
                return;
            }
            // the page as written, line by line, so it reads without opening the screen (and without spending Chalk)
            pages.get(0).raw().lines().limit(6).forEach(line -> builder.accept(Component.literal(line).withStyle(net.minecraft.ChatFormatting.WHITE)));
        }
    }

    // ---------------------------------------------------------------- game test

    private static final DeferredRegister<Consumer<GameTestHelper>> TEST_FUNCTIONS = DeferredRegister.create(Registries.TEST_FUNCTION, MODID);
    private static final ResourceKey<Consumer<GameTestHelper>> TEST = ResourceKey.create(Registries.TEST_FUNCTION, Identifier.fromNamespaceAndPath(MODID, "chemistry"));

    static {
        TEST_FUNCTIONS.register("chemistry", () -> Chemistry::test);
    }

    public static void init(IEventBus modEventBus) {
        TEST_FUNCTIONS.register(modEventBus);
    }

    @SubscribeEvent
    static void registerTests(RegisterGameTestsEvent event) {
        var env = event.registerEnvironment(Identifier.fromNamespaceAndPath(MODID, "chemistry"), new TestEnvironmentDefinition.AllOf(List.of()));
        event.registerTest(Identifier.fromNamespaceAndPath(MODID, "chemistry"),
                new FunctionGameTestInstance(TEST, new TestData<>(env, Identifier.withDefaultNamespace("empty"), 40, 0, true)));
    }

    private static TransientCraftingContainer grid(ItemStack... items) {
        var grid = new TransientCraftingContainer(new AbstractContainerMenu(null, 0) {
            @Override
            public ItemStack quickMoveStack(Player player, int slot) {
                return ItemStack.EMPTY;
            }

            @Override
            public boolean stillValid(Player player) {
                return true;
            }
        }, 3, 3);
        for (int i = 0; i < items.length; i++) grid.setItem(i, items[i]);
        return grid;
    }

    private static ItemStack craft(GameTestHelper h, String what, ItemStack... items) {
        var input = grid(items).asCraftInput();
        return h.getLevel().recipeAccess().getRecipeFor(RecipeType.CRAFTING, input, h.getLevel())
                .orElseThrow(() -> h.assertionException("no recipe for " + what)).value().assemble(input);
    }

    private static Item dye(String colour) { // 26.x keeps the dyes in Items.DYE, a ColorCollection
        return BuiltInRegistries.ITEM.getValue(Identifier.withDefaultNamespace(colour + "_dye"));
    }

    private static ItemStack of(Item item, int count) {
        return new ItemStack(item, count);
    }

    private static void test(GameTestHelper h) {
        var level = h.getLevel();
        int natural = KilnBlockEntity.FUELS.getLast().maxTemp(), firewood = KilnBlockEntity.FUELS.get(1).maxTemp();

        // alum: Alum Shale roasted at 800° (Charcoal, no bellows), leached with a bucket of water (it comes back) -> 2 Alum
        KilnBlockEntity.Bake roast = KilnBlockEntity.bakeFor(new ItemStack(StoneFamilies.block("alum_shale")));
        h.assertTrue(roast != null && roast.result.is(ROASTED_ALUM_SHALE.get()) && roast.temp == ALUM_ROAST_TEMP && roast.temp <= natural,
                "Alum Shale roasts at " + ALUM_ROAST_TEMP + "° without the bellows");
        var leachGrid = grid(new ItemStack(ROASTED_ALUM_SHALE.get()), new ItemStack(Items.WATER_BUCKET)).asCraftInput();
        var leach = level.recipeAccess().getRecipeFor(RecipeType.CRAFTING, leachGrid, level).orElseThrow(() -> h.assertionException("no leaching recipe"));
        ItemStack alum = leach.value().assemble(leachGrid);
        h.assertTrue(alum.is(ALUM.get()) && alum.getCount() == 2, "Roasted Alum Shale + Water Bucket = 2 Alum (got " + alum + ")");
        h.assertTrue(leach.value().getRemainingItems(leachGrid).stream().anyMatch(s -> s.is(Items.BUCKET)), "the bucket comes back");
        // mordant: 2 pigment + Alum = 4 dye, twice what a pigment alone gives
        h.assertTrue(craft(h, "Red Pigment", new ItemStack(Pigments.RED_PIGMENT.get())).getCount() == 1, "a pigment alone is 1 dye");
        for (Item[] pd : new Item[][] {{Pigments.RED_PIGMENT.get(), dye("red")}, {Pigments.YELLOW_PIGMENT.get(), dye("yellow")},
                {Pigments.WHITE_PIGMENT.get(), dye("white")}, {Pigments.BLACK_PIGMENT.get(), dye("black")}}) {
            ItemStack dye = craft(h, "2 " + pd[0] + " + Alum", new ItemStack(pd[0]), new ItemStack(pd[0]), new ItemStack(ALUM.get()));
            h.assertTrue(dye.is(pd[1]) && dye.getCount() == 4, "2 " + pd[0] + " + Alum = 4 " + pd[1] + " (got " + dye + ")");
        }
        // tawing: 2 Scraped Hides + Alum = 2 Leather (salt and slaked lime do one hide each)
        ItemStack leather = craft(h, "2 Scraped Hide + Alum", new ItemStack(Mechanical.SCRAPED_HIDE.get()), new ItemStack(Mechanical.SCRAPED_HIDE.get()),
                new ItemStack(ALUM.get()));
        h.assertTrue(leather.is(Items.LEATHER) && leather.getCount() == 2, "2 Scraped Hides + Alum = 2 Leather (got " + leather + ")");

        // plaster: Gypsum crushed into 2 Gypsum Powder, burnt at 300° (a firewood fire) to Plaster of Paris, which binds Lime Plaster
        ItemStack powder = BronzeOres.crushed(new ItemStack(StoneFamilies.block("gypsum")));
        h.assertTrue(powder.is(GYPSUM_POWDER.get()) && powder.getCount() == 2, "Gypsum crushes into 2 Gypsum Powder (got " + powder + ")");
        KilnBlockEntity.Bake plaster = KilnBlockEntity.bakeFor(powder);
        h.assertTrue(plaster != null && plaster.result.is(PLASTER_OF_PARIS.get()) && plaster.temp == PLASTER_TEMP && plaster.temp <= firewood,
                "Gypsum Powder burns to Plaster of Paris at " + PLASTER_TEMP + "° on firewood");
        ItemStack lime = craft(h, "Plaster of Paris + 2 Sand + Straw", new ItemStack(PLASTER_OF_PARIS.get()), new ItemStack(Items.SAND),
                new ItemStack(Items.SAND), new ItemStack(Farming.STRAW.get()));
        h.assertTrue(lime.is(BuiltInRegistries.ITEM.getValue(Identifier.fromNamespaceAndPath(MODID, "lime_plaster"))) && lime.getCount() == 4,
                "Plaster of Paris + 2 Sand + Straw = 4 Lime Plaster (got " + lime + ")");

        // the chalk and ochre blocks crush straight into pigment, 2 per item they are made of
        h.assertTrue(BronzeOres.crushed(new ItemStack(StoneFamilies.block("chalk_block"))).is(Pigments.WHITE_PIGMENT.get()), "a Chalk Block crushes into White Pigment");
        ItemStack red = BronzeOres.crushed(new ItemStack(StoneFamilies.block("red_ochre_block")));
        h.assertTrue(red.is(Pigments.RED_PIGMENT.get()) && red.getCount() == 8, "a Red Ochre Block crushes into 8 Red Pigment");
        h.assertTrue(BronzeOres.crushed(new ItemStack(StoneFamilies.block("yellow_ochre_block"))).is(Pigments.YELLOW_PIGMENT.get()),
                "a Yellow Ochre Block crushes into Yellow Pigment");
        h.assertTrue(BronzeOres.crushed(new ItemStack(Pigments.CHALK.get())).getCount() == 2, "Chalk still grinds as before");

        // the Slate Tablet: Polished Slate in a frame of 2 Sticks; written with Chalk in the other hand, wiped by sneak-use
        ItemStack tablet = craft(h, "Polished Slate + 2 Sticks", new ItemStack(StoneFamilies.block("polished_slate")), new ItemStack(Items.STICK),
                new ItemStack(Items.STICK));
        h.assertTrue(tablet.is(SLATE_TABLET.get()) && tablet.has(DataComponents.WRITABLE_BOOK_CONTENT), "Polished Slate + 2 Sticks = a writable Slate Tablet");
        Player player = h.makeMockPlayer(GameType.SURVIVAL);
        player.setItemInHand(InteractionHand.MAIN_HAND, tablet);
        h.assertTrue(tablet.use(level, player, InteractionHand.MAIN_HAND) == InteractionResult.FAIL, "no Chalk, no writing");
        player.setItemInHand(InteractionHand.OFF_HAND, of(Pigments.CHALK.get(), 2));
        InteractionResult wrote = tablet.use(level, player, InteractionHand.MAIN_HAND);
        h.assertTrue(wrote.consumesAction() && player.getOffhandItem().getCount() == 1,
                "writing uses up one Chalk (got " + wrote + ", " + player.getOffhandItem() + ")");
        tablet.set(DataComponents.WRITABLE_BOOK_CONTENT, new WritableBookContent(List.of(net.minecraft.server.network.Filterable.passThrough("tally: 3 sheep"))));
        player.setShiftKeyDown(true);
        h.assertTrue(tablet.use(level, player, InteractionHand.MAIN_HAND).consumesAction()
                && tablet.get(DataComponents.WRITABLE_BOOK_CONTENT).pages().isEmpty(), "sneak-use wipes the tablet");
        h.assertTrue(player.getOffhandItem().getCount() == 1, "wiping costs no Chalk");
        // loading the packet listener applies ServerGamePacketListenerImplMixin (the tablet's one page, no signing): fails here if it can't
        h.assertTrue(java.util.Arrays.stream(net.minecraft.server.network.ServerGamePacketListenerImpl.class.getDeclaredMethods())
                .anyMatch(m -> m.getName().endsWith("tabletSign")), "the Slate Tablet mixin is applied");
        h.succeed();
    }

    private Chemistry() {}
}
