package com.mechanical.equipment;

import static com.mechanical.Mechanical.MODID;

import java.util.List;
import java.util.function.Consumer;

import com.mechanical.Mechanical;
import com.mechanical.Temperature;
import com.mechanical.wildlife.river.RiverFauna;

import net.minecraft.core.registries.Registries;
import net.minecraft.gametest.framework.FunctionGameTestInstance;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.gametest.framework.TestData;
import net.minecraft.gametest.framework.TestEnvironmentDefinition;
import net.minecraft.resources.Identifier;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.InventoryMenu;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.BowItem;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.GameType;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.RegisterGameTestsEvent;
import net.neoforged.neoforge.registries.DeferredRegister;

/**
 * Game test for the accessory slots: the inventory menu gets them (InventoryMenuMixin), they take only their own items,
 * shift-click fills them, a worn cloak gives its armour and warmth, a worn quiver takes arrows in and a bow shoots from it
 * (ProjectileWeaponItemMixin), and everything worn drops on death.
 */
@EventBusSubscriber(modid = MODID)
public final class EquipmentTest {
    private static final DeferredRegister<Consumer<GameTestHelper>> TEST_FUNCTIONS = DeferredRegister.create(Registries.TEST_FUNCTION, MODID);
    private static final ResourceKey<Consumer<GameTestHelper>> TEST = ResourceKey.create(Registries.TEST_FUNCTION, Identifier.fromNamespaceAndPath(MODID, "equipment"));

    static {
        TEST_FUNCTIONS.register("equipment", () -> EquipmentTest::test);
    }

    public static void init(IEventBus modEventBus) {
        TEST_FUNCTIONS.register(modEventBus);
    }

    @SubscribeEvent
    static void registerTests(RegisterGameTestsEvent event) {
        var env = event.registerEnvironment(Identifier.fromNamespaceAndPath(MODID, "equipment"), new TestEnvironmentDefinition.AllOf(List.of()));
        event.registerTest(Identifier.fromNamespaceAndPath(MODID, "equipment"),
                new FunctionGameTestInstance(TEST, new TestData<>(env, Identifier.withDefaultNamespace("empty"), 20, 0, true)));
    }

    /** Reaches ProjectileWeaponItem's protected draw (what a bow does on release). */
    private static final class Bow extends BowItem {
        private Bow() {
            super(new Properties());
        }

        static List<ItemStack> shoot(ItemStack bow, ItemStack arrows, LivingEntity shooter) {
            return draw(bow, arrows, shooter);
        }
    }

    private static void test(GameTestHelper h) {
        try {
            run(h);
        } catch (RuntimeException e) {
            e.printStackTrace();
            throw h.assertionException(e + " at " + java.util.Arrays.toString(java.util.Arrays.copyOf(e.getStackTrace(), 6)));
        }
    }

    private static void run(GameTestHelper h) {
        Player p = h.makeMockPlayer(GameType.SURVIVAL);
        net.minecraft.world.phys.Vec3 at = h.absoluteVec(new net.minecraft.world.phys.Vec3(1.5, 2, 1.5));
        p.setPos(at.x, at.y, at.z);
        InventoryMenu menu = p.inventoryMenu;

        // the slots: after the vanilla ones, the main inventory moved down for the row under the player model
        h.assertTrue(menu.slots.size() == Equipment.MENU_START + Accessory.COUNT, "inventory menu has the accessory slots (" + menu.slots.size() + ")");
        Slot cloakSlot = menu.slots.get(Equipment.MENU_START + Accessory.CLOAK.ordinal());
        h.assertTrue(cloakSlot instanceof AccessorySlot a && a.accessory == Accessory.CLOAK && cloakSlot.x == 77 && cloakSlot.y == 8, "cloak slot in place");
        h.assertTrue(menu.slots.get(InventoryMenu.INV_SLOT_START).y == 84 + Equipment.SCREEN_EXTRA_HEIGHT, "main inventory moved down");
        ItemStack cape = new ItemStack(RiverFauna.OTTER_FUR_CAPE.get());
        ItemStack satchel = new ItemStack(Mechanical.LEATHER_SATCHEL.get());
        h.assertTrue(cloakSlot.mayPlace(cape) && !cloakSlot.mayPlace(satchel), "the cloak slot takes cloaks only");
        h.assertTrue(menu.slots.get(Equipment.MENU_START + Accessory.BACK.ordinal()).mayPlace(satchel), "the back slot takes the satchel");

        // shift-click from the inventory into the free slot, and back out
        p.getInventory().setItem(9, cape.copy());
        menu.quickMoveStack(p, InventoryMenu.INV_SLOT_START);
        h.assertTrue(Equipment.get(p, Accessory.CLOAK).is(RiverFauna.OTTER_FUR_CAPE.get()) && p.getInventory().getItem(9).isEmpty(), "shift-click wears the cape");

        // worn: armour from the cloak, counted for warmth
        Equipment.update(p);
        h.assertTrue(p.getAttributeValue(Attributes.ARMOR) == 2.0, "the Otter Fur Cape gives 2 armour from the cloak slot (" + p.getAttributeValue(Attributes.ARMOR) + ")");
        h.assertTrue(Temperature.wornForWarmth(p).stream().anyMatch(s -> s.is(RiverFauna.OTTER_FUR_CAPE.get())), "the cloak keeps the cold out");
        h.assertTrue(RiverFauna.waterproofing(p) > 0, "the otter cape keeps the wet out from the cloak slot");
        menu.quickMoveStack(p, Equipment.MENU_START + Accessory.CLOAK.ordinal());
        Equipment.update(p);
        h.assertTrue(Equipment.get(p, Accessory.CLOAK).isEmpty() && p.getAttributeValue(Attributes.ARMOR) == 0.0, "taken off: no armour left");

        // the quiver takes arrows in, the bow shoots from it
        Equipment.of(p).set(Accessory.HIP, new ItemStack(Mechanical.QUIVER.get()));
        ItemStack arrows = new ItemStack(Items.ARROW, 70);
        h.assertTrue(Quiver.insert(p, arrows) && arrows.isEmpty(), "70 arrows go into the quiver");
        List<ItemStack> inQuiver = Quiver.contents(Quiver.worn(p));
        h.assertTrue(inQuiver.get(0).getCount() == 64 && inQuiver.get(1).getCount() == 6, "as a full stack and 6");
        ItemStack bow = new ItemStack(Items.BOW);
        p.setItemInHand(InteractionHand.MAIN_HAND, bow);
        ItemStack ammo = p.getProjectile(bow);
        h.assertTrue(ammo.is(Items.ARROW), "the bow finds the quiver's arrows");
        h.assertTrue(Bow.shoot(bow, ammo, p).size() == 1, "one arrow drawn");
        h.assertTrue(Quiver.contents(Quiver.worn(p)).get(0).getCount() == 63, "the shot arrow left the quiver");

        // death: everything worn drops (keepInventory off)
        Equipment.of(p).set(Accessory.BACK, satchel.copy());
        Equipment.dropOnDeath(p, h.getLevel());
        h.assertTrue(Equipment.of(p).isEmpty(), "nothing stays worn after death");
        List<ItemEntity> drops = h.getLevel().getEntitiesOfClass(ItemEntity.class, p.getBoundingBox().inflate(4));
        h.assertTrue(drops.stream().anyMatch(e -> e.getItem().is(Mechanical.QUIVER.get())) && drops.stream().anyMatch(e -> e.getItem().is(Mechanical.LEATHER_SATCHEL.get())),
                "the quiver and the satchel lie on the ground");
        drops.forEach(ItemEntity::discard);
        h.succeed();
    }

    private EquipmentTest() {}
}
