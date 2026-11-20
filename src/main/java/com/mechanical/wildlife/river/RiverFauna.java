package com.mechanical.wildlife.river;

import static com.mechanical.Mechanical.BLOCKS;
import static com.mechanical.Mechanical.ENTITY_TYPES;
import static com.mechanical.Mechanical.ITEMS;
import static com.mechanical.Mechanical.MODID;

import java.util.List;
import java.util.Map;

import com.mechanical.Butchery;
import com.mechanical.KnifeItem;
import com.mechanical.wildlife.Wildlife;

import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.Identifier;
import net.minecraft.resources.ResourceKey;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.tags.BlockTags;
import net.minecraft.tags.FluidTags;
import net.minecraft.tags.TagKey;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.EntitySpawnReason;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.MobCategory;
import net.minecraft.world.entity.SpawnPlacementTypes;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.food.FoodProperties;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemUseAnimation;
import net.minecraft.world.item.SpawnEggItem;
import net.minecraft.world.item.ToolMaterial;
import net.minecraft.world.item.component.Consumable;
import net.minecraft.world.item.consume_effects.ApplyStatusEffectsConsumeEffect;
import net.minecraft.world.item.consume_effects.RemoveStatusEffectsConsumeEffect;
import net.minecraft.world.item.equipment.ArmorMaterial;
import net.minecraft.world.item.equipment.ArmorType;
import net.minecraft.world.item.equipment.EquipmentAssets;
import net.minecraft.world.level.LevelAccessor;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.SoundType;
import net.minecraft.world.level.levelgen.Heightmap;
import net.minecraft.world.level.material.MapColor;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.entity.EntityAttributeCreationEvent;
import net.neoforged.neoforge.event.entity.RegisterSpawnPlacementsEvent;
import net.neoforged.neoforge.registries.DeferredBlock;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredItem;

/**
 * River fauna: the Beaver (fells trees, builds a dam and a lodge: {@link Beaver}, {@link BeaverWorks}), the Otter
 * ({@link Otter}) and the Mosquito Swarm ({@link MosquitoSwarm}), with everything they give: meat, pelts cured on the
 * Stretching Hoop ({@link PeltHoopBlock}), castoreum, beaver teeth and what is made from them.
 * Data: scripts/gen_fauna_river.py, textures: scripts/gen_fauna_river_textures.py, docs: docs/fauna/river.md.
 */
@EventBusSubscriber(modid = MODID)
public final class RiverFauna {
    public static final DeferredHolder<EntityType<?>, EntityType<Beaver>> BEAVER = ENTITY_TYPES.registerEntityType(
            "beaver", Beaver::new, MobCategory.CREATURE, b -> b.sized(0.6F, 0.55F).clientTrackingRange(10));
    public static final DeferredHolder<EntityType<?>, EntityType<Otter>> OTTER = ENTITY_TYPES.registerEntityType(
            "otter", Otter::new, MobCategory.CREATURE, b -> b.sized(0.6F, 0.45F).clientTrackingRange(10));
    public static final DeferredHolder<EntityType<?>, EntityType<MosquitoSwarm>> MOSQUITO_SWARM = ENTITY_TYPES.registerEntityType(
            "mosquito_swarm", MosquitoSwarm::new, MobCategory.AMBIENT, b -> b.sized(0.7F, 0.7F).clientTrackingRange(8));

    // ---- what they give (Butchery yields below) ----
    public static final DeferredItem<Item> RAW_BEAVER = food("raw_beaver", 2, 0.3F);
    public static final DeferredItem<Item> COOKED_BEAVER = food("cooked_beaver", 6, 0.7F);
    /** The fatty, scaly tail: a delicacy roasted over the fire. */
    public static final DeferredItem<Item> BEAVER_TAIL = food("beaver_tail", 1, 0.2F);
    public static final DeferredItem<Item> ROASTED_BEAVER_TAIL = food("roasted_beaver_tail", 7, 1.0F);
    public static final DeferredItem<Item> RAW_OTTER = food("raw_otter", 2, 0.2F);
    public static final DeferredItem<Item> COOKED_OTTER = food("cooked_otter", 5, 0.6F);
    /** Fresh pelts: cured on a Stretching Hoop (PeltHoopBlock) into the pelt. */
    public static final DeferredItem<Item> RAW_BEAVER_PELT = ITEMS.registerSimpleItem("raw_beaver_pelt");
    public static final DeferredItem<Item> BEAVER_PELT = ITEMS.registerSimpleItem("beaver_pelt");
    public static final DeferredItem<Item> RAW_OTTER_PELT = ITEMS.registerSimpleItem("raw_otter_pelt");
    public static final DeferredItem<Item> OTTER_PELT = ITEMS.registerSimpleItem("otter_pelt");
    /** Musky scent glands: snare bait (#mechanical:snare_bait) and the Castoreum Salve. */
    public static final DeferredItem<Item> CASTOREUM = ITEMS.registerSimpleItem("castoreum");
    public static final DeferredItem<Item> BEAVER_TOOTH = ITEMS.registerSimpleItem("beaver_tooth");

    /** Rubbed in: heals a little and draws out poison (castoreum carries willow-bark salicin). */
    public static final DeferredItem<Item> CASTOREUM_SALVE = ITEMS.registerSimpleItem("castoreum_salve", p -> p.stacksTo(16)
            .component(net.minecraft.core.component.DataComponents.CONSUMABLE, Consumable.builder().consumeSeconds(1.2F)
                    .animation(ItemUseAnimation.NONE).sound(SoundEvents.HONEY_DRINK).hasConsumeParticles(false)
                    .onConsume(new ApplyStatusEffectsConsumeEffect(new MobEffectInstance(MobEffects.REGENERATION, 200, 0)))
                    .onConsume(new RemoveStatusEffectsConsumeEffect(MobEffects.POISON)).build()));

    /** A beaver's incisor set in a stick: a chisel-sharp little knife (a real stone age woodworking tool). */
    public static final TagKey<Item> BEAVER_TOOTH_REPAIR = TagKey.create(Registries.ITEM, Identifier.fromNamespaceAndPath(MODID, "beaver_tooth_tool_materials"));
    public static final ToolMaterial BEAVER_TOOTH_MATERIAL = new ToolMaterial(BlockTags.INCORRECT_FOR_WOODEN_TOOL, 96, 2.5F, 0.0F, 5, BEAVER_TOOTH_REPAIR);
    public static final DeferredItem<KnifeItem> BEAVER_TOOTH_KNIFE = ITEMS.registerItem("beaver_tooth_knife",
            p -> new KnifeItem(p, 1.2F, 1.0F, KnifeItem.FLINT), p -> p.sword(BEAVER_TOOTH_MATERIAL, 1.0F, -1.8F));

    // ---- waterproof fur: warm like wolf fur, and keeps the chill of water and rain off (Temperature) ----
    public static final TagKey<Item> WATERPROOF_FUR = TagKey.create(Registries.ITEM, Identifier.fromNamespaceAndPath(MODID, "waterproof_fur"));
    public static final float FUR_INSULATION = 0.2F, WATERPROOF_PER_PIECE = 0.5F;
    public static final ArmorMaterial BEAVER_FUR = fur("beaver_fur", 8, 1, 1, 1, 1);
    public static final ArmorMaterial OTTER_FUR = fur("otter_fur", 7, 1, 2, 1, 1);
    public static final DeferredItem<Item> BEAVER_FUR_HAT = ITEMS.registerSimpleItem("beaver_fur_hat", p -> p.humanoidArmor(BEAVER_FUR, ArmorType.HELMET));
    /** A cloak (Cloak slot) of sleek otter fur: warm and waterproof over whatever armour is worn under it. */
    public static final DeferredItem<com.mechanical.equipment.CloakItem> OTTER_FUR_CAPE = ITEMS.registerItem("otter_fur_cape",
            p -> new com.mechanical.equipment.CloakItem(p, OTTER_FUR, true), p -> com.mechanical.equipment.CloakItem.properties(p, OTTER_FUR));

    // ---- blocks ----
    /** Sticks, logs and mud packed by beavers: the dam across a stream and the lodge. Drops sticks. */
    public static final DeferredBlock<Block> BEAVER_DAM = BLOCKS.registerBlock("beaver_dam", Block::new, p -> p
            .mapColor(MapColor.DIRT).strength(1.2F).sound(SoundType.ROOTED_DIRT).ignitedByLava());
    public static final DeferredItem<BlockItem> BEAVER_DAM_ITEM = ITEMS.registerSimpleBlockItem("beaver_dam", BEAVER_DAM);
    public static final DeferredBlock<PeltHoopBlock> PELT_HOOP = BLOCKS.registerBlock("pelt_hoop", PeltHoopBlock::new, p -> p
            .mapColor(MapColor.WOOD).strength(0.8F).sound(SoundType.WOOD).noOcclusion().randomTicks().ignitedByLava()
            .pushReaction(net.minecraft.world.level.material.PushReaction.POPPED));
    public static final DeferredItem<BlockItem> PELT_HOOP_ITEM = ITEMS.registerSimpleBlockItem("pelt_hoop", PELT_HOOP);

    public static final DeferredItem<SpawnEggItem> BEAVER_SPAWN_EGG = egg("beaver_spawn_egg", BEAVER);
    public static final DeferredItem<SpawnEggItem> OTTER_SPAWN_EGG = egg("otter_spawn_egg", OTTER);
    public static final DeferredItem<SpawnEggItem> MOSQUITO_SWARM_SPAWN_EGG = egg("mosquito_swarm_spawn_egg", MOSQUITO_SWARM);

    private static DeferredItem<Item> food(String name, int nutrition, float saturation) {
        return ITEMS.registerSimpleItem(name, p -> p.food(new FoodProperties.Builder().nutrition(nutrition).saturationModifier(saturation).build()));
    }

    private static DeferredItem<SpawnEggItem> egg(String name, DeferredHolder<EntityType<?>, ? extends EntityType<?>> type) {
        return ITEMS.registerItem(name, SpawnEggItem::new, p -> p.spawnEgg(type.get()));
    }

    private static ArmorMaterial fur(String asset, int durability, int helmet, int chest, int legs, int boots) {
        return new ArmorMaterial(durability, Map.of(ArmorType.HELMET, helmet, ArmorType.CHESTPLATE, chest, ArmorType.LEGGINGS, legs,
                ArmorType.BOOTS, boots, ArmorType.BODY, chest), 12, SoundEvents.ARMOR_EQUIP_LEATHER, 0.0F, 0.0F,
                TagKey.create(Registries.ITEM, Identifier.fromNamespaceAndPath(MODID, "repairs_" + asset + "_armor")),
                ResourceKey.create(EquipmentAssets.ROOT_ID, Identifier.fromNamespaceAndPath(MODID, asset)));
    }

    /** Share (0..1) of the chill of water and rain kept off by the waterproof fur this player wears (armour and cloak). */
    public static float waterproofing(Player player) {
        float w = 0;
        for (net.minecraft.world.item.ItemStack worn : com.mechanical.Temperature.wornForWarmth(player)) {
            if (worn.is(WATERPROOF_FUR)) w += WATERPROOF_PER_PIECE;
        }
        return Math.min(w, 1);
    }

    /** Loads this class so its registrations happen; call from the mod constructor. */
    public static void init() {
        // beaver: the pelt first, then meat, the tail, castoreum and the teeth
        Butchery.register(() -> RiverFauna.BEAVER.get(), new Butchery.Yield(RiverFauna.RAW_BEAVER, 1, 2, List.of(new Butchery.Extra(RiverFauna.RAW_BEAVER_PELT, 1.0F, 1, 1)),
                List.of(new Butchery.Extra(RiverFauna.BEAVER_TAIL, 1.0F, 1, 1), new Butchery.Extra(RiverFauna.CASTOREUM, 0.6F, 1, 2),
                        new Butchery.Extra(RiverFauna.BEAVER_TOOTH, 0.5F, 1, 2), new Butchery.Extra(Wildlife.SINEW, 0.3F, 1, 1))));
        Butchery.register(() -> RiverFauna.OTTER.get(), new Butchery.Yield(RiverFauna.RAW_OTTER, 1, 1, List.of(new Butchery.Extra(RiverFauna.RAW_OTTER_PELT, 1.0F, 1, 1)),
                List.of(new Butchery.Extra(Wildlife.SINEW, 0.3F, 1, 1))));
        Butchery.neverCarcass(MOSQUITO_SWARM::get);
    }

    @SubscribeEvent
    static void attributes(EntityAttributeCreationEvent event) {
        event.put(BEAVER.get(), Beaver.createAttributes().build());
        event.put(OTTER.get(), Otter.createAttributes().build());
        event.put(MOSQUITO_SWARM.get(), MosquitoSwarm.createAttributes().build());
    }

    @SubscribeEvent
    static void spawnRules(RegisterSpawnPlacementsEvent event) {
        // on a bank by the water (beavers in forests too, so the water check matters)
        event.register(BEAVER.get(), SpawnPlacementTypes.ON_GROUND, Heightmap.Types.MOTION_BLOCKING_NO_LEAVES,
                (type, level, reason, pos, random) -> shore(level, pos, reason), RegisterSpawnPlacementsEvent.Operation.REPLACE);
        event.register(OTTER.get(), SpawnPlacementTypes.ON_GROUND, Heightmap.Types.MOTION_BLOCKING_NO_LEAVES,
                (type, level, reason, pos, random) -> shore(level, pos, reason), RegisterSpawnPlacementsEvent.Operation.REPLACE);
        event.register(MOSQUITO_SWARM.get(), SpawnPlacementTypes.NO_RESTRICTIONS, Heightmap.Types.MOTION_BLOCKING_NO_LEAVES,
                (type, level, reason, pos, random) -> MosquitoSwarm.canSpawn(level, pos, reason, random), RegisterSpawnPlacementsEvent.Operation.REPLACE);
    }

    /** Solid natural ground (grass, dirt, sand, gravel, mud, stone) with water within 3 blocks. */
    static boolean shore(LevelAccessor level, BlockPos pos, EntitySpawnReason reason) {
        var below = level.getBlockState(pos.below());
        boolean ground = below.is(BlockTags.ANIMALS_SPAWNABLE_ON) || below.is(BlockTags.SAND) || below.is(BlockTags.DIRT)
                || below.is(net.minecraft.world.level.block.Blocks.GRAVEL) || below.is(BlockTags.BASE_STONE_OVERWORLD);
        return ground && level.getBlockState(pos).getFluidState().isEmpty() && nearWater(level, pos, 3, reason == EntitySpawnReason.CHUNK_GENERATION);
    }

    /** Water at or one below {@code pos}'s level within {@code r} blocks; {@code ownChunk}: only look inside pos's chunk (world generation). */
    static boolean nearWater(LevelAccessor level, BlockPos pos, int r, boolean ownChunk) {
        BlockPos.MutableBlockPos at = new BlockPos.MutableBlockPos();
        for (int dx = -r; dx <= r; dx++) {
            for (int dz = -r; dz <= r; dz++) {
                int x = pos.getX() + dx, z = pos.getZ() + dz;
                if (ownChunk && (x >> 4 != pos.getX() >> 4 || z >> 4 != pos.getZ() >> 4)) continue;
                for (int dy = -1; dy <= 0; dy++) {
                    if (level.getFluidState(at.set(x, pos.getY() + dy, z)).is(FluidTags.WATER)) return true;
                }
            }
        }
        return false;
    }

    private RiverFauna() {}
}
