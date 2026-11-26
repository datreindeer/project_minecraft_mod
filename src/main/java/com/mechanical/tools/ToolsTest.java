package com.mechanical.tools;

import static com.mechanical.Mechanical.MODID;

import java.util.List;
import java.util.function.Consumer;

import com.mechanical.Bronze;
import com.mechanical.Mechanical;
import com.mechanical.Smithing;

import net.minecraft.core.component.DataComponents;
import net.minecraft.core.registries.Registries;
import net.minecraft.gametest.framework.FunctionGameTestInstance;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.gametest.framework.TestEnvironmentDefinition;
import net.minecraft.resources.Identifier;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.crafting.CraftingInput;
import net.minecraft.world.item.crafting.RecipeType;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.RegisterGameTestsEvent;
import net.neoforged.neoforge.registries.DeferredRegister;

/** Game tests for the dynamic tools. */
@EventBusSubscriber(modid = MODID)
public final class ToolsTest {
    private static final DeferredRegister<Consumer<GameTestHelper>> TEST_FUNCTIONS = DeferredRegister.create(Registries.TEST_FUNCTION, MODID);
    private static final ResourceKey<Consumer<GameTestHelper>> TEST = ResourceKey.create(Registries.TEST_FUNCTION, Identifier.fromNamespaceAndPath(MODID, "tools"));

    static {
        TEST_FUNCTIONS.register("tools", () -> ToolsTest::test);
    }

    public static void init(IEventBus modEventBus) {
        TEST_FUNCTIONS.register(modEventBus);
    }

    @SubscribeEvent
    static void registerTests(RegisterGameTestsEvent event) {
        var env = event.registerEnvironment(Identifier.fromNamespaceAndPath(MODID, "tools"), new TestEnvironmentDefinition.AllOf(List.of()));
        event.registerTest(Identifier.fromNamespaceAndPath(MODID, "tools"),
                new FunctionGameTestInstance(TEST, com.mechanical.TestArea.of(env, 20, true)));
    }

    private static void test(GameTestHelper h) {
        try {
            rodsExist(h);
            defaultsUnchanged(h);
            steelRodLonger(h);
            smithingBonus(h);
            poorOnce(h);
            station(h);
            h.succeed();
        } catch (RuntimeException e) {
            e.printStackTrace();
            throw h.assertionException(e + " at " + java.util.Arrays.toString(java.util.Arrays.copyOf(e.getStackTrace(), 6)));
        }
    }

    /** Each rod is registered and made: bone, copper and bronze by a crafting recipe, steel by the anvil's Tool Rod pattern. */
    private static void rodsExist(GameTestHelper h) {
        var level = h.getLevel();
        Item bronzeIngot = net.minecraft.core.registries.BuiltInRegistries.ITEM.getValue(Identifier.fromNamespaceAndPath(MODID, "bronze_ingot"));
        for (var c : List.of(
                new Object[] {Smithing.BONE_TOOL_ROD.get(), List.of(com.mechanical.Mechanical.IRON_KNIFE.get(), Items.BONE, Items.BONE)},
                new Object[] {Smithing.COPPER_TOOL_ROD.get(), List.of(Items.COPPER_INGOT, Items.COPPER_INGOT)},
                new Object[] {Smithing.BRONZE_TOOL_ROD.get(), List.of(bronzeIngot, bronzeIngot)})) {
            Item rod = (Item) c[0];
            h.assertTrue(net.minecraft.core.registries.BuiltInRegistries.ITEM.getKey(rod).getNamespace().equals(MODID), rod + " registered");
            @SuppressWarnings("unchecked")
            List<Item> in = (List<Item>) c[1];
            var input = CraftingInput.of(1, in.size(), in.stream().map(ItemStack::new).toList());
            var recipe = level.recipeAccess().getRecipeFor(RecipeType.CRAFTING, input, level).orElseThrow(() -> h.assertionException("no recipe for " + rod));
            h.assertTrue(recipe.value().assemble(input).is(rod), "recipe makes " + rod);
            if (in.size() == 3) h.assertTrue(recipe.value().getRemainingItems(input).get(0).is(com.mechanical.Mechanical.IRON_KNIFE.get()), "the knife stays");
        }
        h.assertTrue(Smithing.Pattern.TOOL_ROD.result(new ItemStack(Smithing.HOT_STEEL_INGOT.get())).is(Smithing.STEEL_TOOL_ROD.get()), "a hot Steel Ingot forges a Steel Tool Rod");
    }

    /** Every recipe built from its own parts (smithing 0) keeps the fresh stack's durability, mining speed and attack damage. */
    private static void defaultsUnchanged(GameTestHelper h) {
        for (var r : com.mechanical.ToolStationRecipes.ALL) {
            if (ToolParts.binding(r.binding().get()) == null || ToolParts.rod(r.rod().get()) == null) continue; // the Yew Longbow's stave
            ItemStack fresh = new ItemStack(r.result().get()), built = fresh.copy();
            ToolParts.apply(built, r.binding().get(), r.rod().get(), new ToolParts.Parts(r.binding().get(), r.rod().get(), 0));
            for (var type : List.of(DataComponents.MAX_DAMAGE, DataComponents.TOOL, DataComponents.ATTRIBUTE_MODIFIERS))
                h.assertTrue(java.util.Objects.equals(fresh.get(type), built.get(type)), r.id() + " keeps " + type + ": " + fresh.get(type) + " vs " + built.get(type));
            h.assertTrue(built.has(ToolParts.TOOL_PARTS.get()), r.id() + " gets tool_parts");
        }
    }

    /** A rod above the head's tier is refused; a Bronze Tool Rod and Leather on a bronze pickaxe give x1.35 x1.1 over Cordage on a Tool Rod. */
    private static void steelRodLonger(GameTestHelper h) {
        Item toolRod = Mechanical.TOOL_ROD.get();
        h.assertFalse(ToolParts.allowed(Bronze.BRONZE_PICKAXE_HEAD.get(), toolRod, Items.LEATHER, Smithing.STEEL_TOOL_ROD.get()), "steel rod (4) on a bronze head (2)");
        h.assertTrue(ToolParts.allowed(Bronze.BRONZE_PICKAXE_HEAD.get(), toolRod, Items.LEATHER, Smithing.BRONZE_TOOL_ROD.get()), "bronze rod on a bronze head");
        h.assertFalse(ToolParts.allowed(Mechanical.STONE_PICKAXE_HEAD.get(), Items.STICK, Mechanical.TWINE.get(), Smithing.BONE_TOOL_ROD.get()), "stone takes only wood");
        h.assertFalse(ToolParts.allowed(Bronze.BRONZE_PICKAXE_HEAD.get(), toolRod, Mechanical.CORDAGE.get(), Items.LEATHER), "no Leather rod on a haft");
        h.assertTrue(ToolParts.allowed(Bronze.BRONZE_SWORD_BLADE.get(), Items.LEATHER, Mechanical.CORDAGE.get(), Smithing.BRONZE_TOOL_ROD.get()), "a Bronze Tool Rod grip on a sword");
        h.assertTrue(ToolParts.allowed(Bronze.BRONZE_SWORD_BLADE.get(), Items.LEATHER, Mechanical.CORDAGE.get(), Items.LEATHER), "the sword's own Leather grip");
        for (Item head : List.of(Items.FLINT, Mechanical.STONE_PICKAXE_HEAD.get()))
            h.assertTrue(java.util.Set.copyOf(ToolParts.rods(head, toolRod)).equals(java.util.Set.of(Items.STICK, toolRod)), "wood only on " + head + ": " + ToolParts.rods(head, toolRod));
        ItemStack pick = bronzePick();
        int expected = Math.round(460 * 1.35F * 1.1F);
        h.assertTrue(Math.abs(pick.getMaxDamage() - expected) <= 1, "bronze rod durability " + pick.getMaxDamage() + " vs " + expected);
        double damage = pick.get(DataComponents.ATTRIBUTE_MODIFIERS).compute(Attributes.ATTACK_DAMAGE, 0, EquipmentSlot.MAINHAND);
        double base = new ItemStack(Bronze.BRONZE_PICKAXE.get()).get(DataComponents.ATTRIBUTE_MODIFIERS).compute(Attributes.ATTACK_DAMAGE, 0, EquipmentSlot.MAINHAND);
        h.assertTrue(Math.abs(damage - base - 0.5) < 1e-6, "bronze rod +0.5 damage: " + base + " -> " + damage);
        var stone = net.minecraft.world.level.block.Blocks.STONE.defaultBlockState();
        var baseTool = new ItemStack(Bronze.BRONZE_PICKAXE.get()).get(DataComponents.TOOL);
        float speed = pick.get(DataComponents.TOOL).getMiningSpeed(stone), baseSpeed = baseTool.getMiningSpeed(stone);
        h.assertTrue(Math.abs(speed - baseSpeed * 1.03F * 0.98F) < 1e-4, "stone speed x1.03 x0.98: " + baseSpeed + " -> " + speed);
        h.assertTrue(pick.get(DataComponents.TOOL).defaultMiningSpeed() == baseTool.defaultMiningSpeed(), "bare-hand default speed unchanged");
        try {
            ToolParts.apply(pick.copy(), Mechanical.CORDAGE.get(), Mechanical.TOOL_ROD.get(), new ToolParts.Parts(Items.DIRT, Mechanical.TOOL_ROD.get(), 0));
            throw h.assertionException("dirt binding accepted");
        } catch (IllegalArgumentException refused) {}
    }

    /** Smithing 40 gives x1.2 durability. */
    private static void smithingBonus(GameTestHelper h) {
        ItemStack pick = new ItemStack(Bronze.BRONZE_PICKAXE.get());
        ToolParts.apply(pick, Mechanical.CORDAGE.get(), Mechanical.TOOL_ROD.get(), new ToolParts.Parts(Mechanical.CORDAGE.get(), Mechanical.TOOL_ROD.get(), 40));
        h.assertTrue(pick.getMaxDamage() == 552, "smithing 40: " + pick.getMaxDamage());
    }

    /** Poor Bronze takes 70 % once, whether marked before apply or after (as Bronze.inheritPoor does). */
    private static void poorOnce(GameTestHelper h) {
        int good = bronzePick().getMaxDamage(), expected = Math.round(good * Bronze.POOR_DURABILITY);
        ItemStack before = new ItemStack(Bronze.BRONZE_PICKAXE.get());
        Bronze.markPoor(before);
        applyBronzeRod(before);
        Bronze.markPoor(before);
        ItemStack after = bronzePick();
        Bronze.markPoor(after);
        h.assertTrue(before.getMaxDamage() == expected && after.getMaxDamage() == expected, "poor: " + before.getMaxDamage() + " / " + after.getMaxDamage() + " vs " + expected);
    }

    /** The Tool Station builds from any allowed binding and rod, refuses a rod above the head's tier, and never stacks different builds. */
    private static void station(GameTestHelper h) {
        var p = net.neoforged.neoforge.common.util.FakePlayerFactory.get(h.getLevel(), new com.mojang.authlib.GameProfile(java.util.UUID.randomUUID(), "tools"));
        p.setData(Mechanical.UNLOCKED_TOOLS, com.mechanical.ToolStationRecipes.ALL.stream().map(com.mechanical.ToolStationRecipes.Recipe::id).toList());
        ItemStack bronze = result(p, Bronze.BRONZE_PICKAXE_HEAD.get(), Items.LEATHER, Smithing.BRONZE_TOOL_ROD.get());
        var parts = bronze.get(ToolParts.TOOL_PARTS.get());
        h.assertTrue(bronze.is(Bronze.BRONZE_PICKAXE.get()) && parts != null && parts.binding() == Items.LEATHER && parts.rod() == Smithing.BRONZE_TOOL_ROD.get(),
                "anyAllowedBinding: " + bronze + " " + parts);
        h.assertTrue(result(p, Items.FLINT, Mechanical.CORDAGE.get(), Smithing.BONE_TOOL_ROD.get()).isEmpty(), "tierBlocks: bone rod on flint (tier 1 > 0)");
        ItemStack plain = result(p, Bronze.BRONZE_PICKAXE_HEAD.get(), Mechanical.CORDAGE.get(), Mechanical.TOOL_ROD.get());
        h.assertFalse(ItemStack.isSameItemSameComponents(plain, bronze), "noMerge");
        Item cord = Mechanical.CORDAGE.get(), twine = Mechanical.TWINE.get(), rod = Mechanical.TOOL_ROD.get(), plate = Bronze.BRONZE_PLATE.get();
        Item hammer = com.mechanical.Ironworking.BRONZE_HAMMER_HEAD.get();
        shares(h, p, -1, Items.FLINT, twine, Items.STICK, com.mechanical.Farming.FLINT_SICKLE.get());
        shares(h, p, -1, Items.FLINT, cord, rod, Mechanical.FLINT_SPEAR.get());
        shares(h, p, -1, Items.FLINT, twine, rod, Mechanical.CRUDE_FISHING_ROD.get());
        shares(h, p, -1, hammer, plate, rod, com.mechanical.BronzeTools.BRONZE_QUARRY_HAMMER.get());
        shares(h, p, -1, hammer, cord, rod, com.mechanical.Ironworking.BRONZE_SMITHING_HAMMER.get());
        shares(h, p, -1, Bronze.BRONZE_HOE_HEAD.get(), plate, rod, com.mechanical.FarmTools.BRONZE_ARD.get());
        shares(h, p, Items.IRON_PICKAXE, Smithing.IRON_PICKAXE_HEAD.get(), Items.LEATHER, rod, Items.IRON_PICKAXE);
        shares(h, p, Bronze.BRONZE_PICKAXE.get(), Smithing.IRON_PICKAXE_HEAD.get(), Items.LEATHER, rod, Items.IRON_PICKAXE);
        shares(h, p, Smithing.STEEL_SMITHING_HAMMER.get(), Smithing.STEEL_HAMMER_HEAD.get(), Smithing.IRON_TOOL_ROD.get(), rod, Smithing.STEEL_SMITHING_HAMMER.get());
        shares(h, p, Mechanical.CRUDE_FISHING_ROD.get(), Items.FLINT, cord, rod, Mechanical.FLINT_SPEAR.get()); // selection no longer fits: best fit
        shares(h, p, Smithing.STEEL_QUARRY_HAMMER.get(), Smithing.STEEL_HAMMER_HEAD.get(), Smithing.IRON_TOOL_ROD.get(), rod, Smithing.STEEL_QUARRY_HAMMER.get());
        h.assertTrue(ToolParts.bindings(Items.FLINT).get(0) == Mechanical.CORDAGE.get(), "fixed order: tier, then id");
    }

    private static void shares(GameTestHelper h, net.minecraft.server.level.ServerPlayer p, int ignored, Item head, Item binding, Item rod, Item expected) {
        shares(h, p, (Item) null, head, binding, rod, expected);
    }

    /** selected: the recipe clicked in the screen (null = nothing selected); expected null = no result. */
    private static void shares(GameTestHelper h, net.minecraft.server.level.ServerPlayer p, Item selected, Item head, Item binding, Item rod, Item expected) {
        var menu = new com.mechanical.ToolStationMenu(1, p.getInventory());
        if (selected != null) {
            int i = 0;
            while (com.mechanical.ToolStationRecipes.ALL.get(i).result().get() != selected) i++;
            menu.clickMenuButton(p, i);
        }
        menu.slots.get(0).set(new ItemStack(head));
        menu.slots.get(1).set(new ItemStack(binding));
        menu.slots.get(2).set(new ItemStack(rod));
        ItemStack got = menu.slots.get(3).getItem();
        h.assertTrue(expected == null ? got.isEmpty() : got.is(expected), "shared head: " + net.minecraft.core.registries.BuiltInRegistries.ITEM.getKey(head) + " + "
                + net.minecraft.core.registries.BuiltInRegistries.ITEM.getKey(binding) + " gave " + got + " not " + expected);
    }

    private static ItemStack result(net.minecraft.server.level.ServerPlayer p, Item head, Item binding, Item rod) {
        var menu = new com.mechanical.ToolStationMenu(1, p.getInventory());
        menu.slots.get(0).set(new ItemStack(head));
        menu.slots.get(1).set(new ItemStack(binding));
        menu.slots.get(2).set(new ItemStack(rod));
        return menu.slots.get(3).getItem().copy();
    }

    private static ItemStack bronzePick() {
        ItemStack pick = new ItemStack(Bronze.BRONZE_PICKAXE.get());
        applyBronzeRod(pick);
        return pick;
    }

    private static void applyBronzeRod(ItemStack pick) {
        ToolParts.apply(pick, Mechanical.CORDAGE.get(), Mechanical.TOOL_ROD.get(), new ToolParts.Parts(Items.LEATHER, Smithing.BRONZE_TOOL_ROD.get(), 0));
    }

    private ToolsTest() {}
}
