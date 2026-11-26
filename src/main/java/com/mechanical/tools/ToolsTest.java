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
        h.assertFalse(ToolParts.allowed(Bronze.BRONZE_PICKAXE_HEAD.get(), Items.LEATHER, Smithing.STEEL_TOOL_ROD.get()), "steel rod (4) on a bronze head (2)");
        h.assertTrue(ToolParts.allowed(Bronze.BRONZE_PICKAXE_HEAD.get(), Items.LEATHER, Smithing.BRONZE_TOOL_ROD.get()), "bronze rod on a bronze head");
        h.assertFalse(ToolParts.allowed(Mechanical.STONE_PICKAXE_HEAD.get(), Mechanical.TWINE.get(), Smithing.BONE_TOOL_ROD.get()), "stone takes only wood");
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
