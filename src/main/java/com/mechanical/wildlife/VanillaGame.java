package com.mechanical.wildlife;

import static com.mechanical.Mechanical.BLOCKS;
import static com.mechanical.Mechanical.ITEMS;
import static com.mechanical.Mechanical.MODID;

import java.util.List;
import java.util.function.Supplier;

import com.mechanical.Butchery;
import com.mechanical.FertilizerItem;
import com.mechanical.Mechanical;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.component.DataComponents;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.EntityTypes;
import net.minecraft.world.entity.ambient.Bat;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemUseAnimation;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.component.Consumable;
import net.minecraft.world.item.consume_effects.ApplyStatusEffectsConsumeEffect;
import net.minecraft.world.level.block.SoundType;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.material.MapColor;
import net.minecraft.world.level.material.PushReaction;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.tick.EntityTickEvent;
import net.neoforged.neoforge.registries.DeferredBlock;
import net.neoforged.neoforge.registries.DeferredItem;

/**
 * Vanilla creatures as game: what each one that still spawns gives when butchered (wild ones become carcasses like the
 * mod's own animals), which ones never leave a carcass (pets, the undead, nether and insect life), and the bat: a tiny
 * carcass with a Bat Wing (ground with Soot and Lard into Night-Eye Salve), and Guano that roosting bats drop on the cave
 * floor below them, a pile that grows ({@link GuanoPileBlock}). Guano is the strongest fertiliser: poor farmland to rich
 * in one go. Data: scripts/gen_wildtweaks.py, textures: scripts/gen_wildtweaks_textures.py.
 */
@EventBusSubscriber(modid = MODID)
public final class VanillaGame {
    /** The thin wing membrane, light and tough. */
    public static final DeferredItem<Item> BAT_WING = ITEMS.registerSimpleItem("bat_wing");
    public static final DeferredItem<FertilizerItem> GUANO = ITEMS.registerItem("guano", p -> new FertilizerItem(3, p));
    /** Bat wing ground with soot and fat, smeared under the eyes: see in the dark (Night Vision, 90 s). */
    public static final DeferredItem<Item> NIGHT_EYE_SALVE = ITEMS.registerSimpleItem("night_eye_salve", p -> p.stacksTo(16)
            .component(DataComponents.CONSUMABLE, Consumable.builder().consumeSeconds(2.0F).animation(ItemUseAnimation.BRUSH)
                    .sound(BuiltInRegistries.SOUND_EVENT.wrapAsHolder(SoundEvents.HONEYCOMB_WAX_ON)).hasConsumeParticles(false)
                    .onConsume(new ApplyStatusEffectsConsumeEffect(new MobEffectInstance(MobEffects.NIGHT_VISION, 1800, 0))).build()));
    public static final DeferredBlock<GuanoPileBlock> GUANO_PILE = BLOCKS.registerBlock("guano_pile", GuanoPileBlock::new, p -> p
            .mapColor(MapColor.TERRACOTTA_WHITE).strength(0.2F).noOcclusion().sound(SoundType.MUD).pushReaction(PushReaction.POPPED));

    /** Ticks between guano rolls for a roosting bat (spread by entity id), the chance per roll, and how far down the floor may be. */
    static final int GUANO_CHECK = 400, GUANO_CHANCE = 6, GUANO_DROP = 24;

    private static Butchery.Extra x(Supplier<Item> item, float chance, int min, int max) {
        return new Butchery.Extra(item, chance, min, max);
    }

    private static Butchery.Extra hide(float chance) {
        return x(Mechanical.RAW_HIDE, chance, 1, 1);
    }

    /** Registers the yields. Call from the mod constructor. Counts and chances are for a flint knife. */
    public static void init() {
        // bat: not an Animal, but a tiny carcass all the same; the wings come off first
        Butchery.alwaysCarcass(() -> EntityTypes.BAT);
        Butchery.wholeMeat(() -> EntityTypes.BAT);
        Butchery.register(() -> EntityTypes.BAT, new Butchery.Yield(Mechanical.GAME_MEAT_SCRAPS, 0, 1, List.of(x(BAT_WING, 0.85F, 1, 2)), List.of()));
        // small game and fur animals
        // (the fox: wildlife.temperate.TemperateFauna, with its Raw Fox Pelt)
        Butchery.register(() -> EntityTypes.OCELOT, new Butchery.Yield(Mechanical.GAME_MEAT_SCRAPS, 1, 2, List.of(hide(0.4F)), List.of(x(Wildlife.SINEW, 0.3F, 1, 1))));
        Butchery.register(() -> EntityTypes.ARMADILLO, new Butchery.Yield(Mechanical.GAME_MEAT_SCRAPS, 1, 1, List.of(x(() -> Items.ARMADILLO_SCUTE, 1.0F, 1, 2)), List.of()));
        Butchery.register(() -> EntityTypes.FROG, new Butchery.Yield(Mechanical.GAME_MEAT_SCRAPS, 1, 1, List.of(), List.of()));
        Butchery.wholeMeat(() -> EntityTypes.FROG);
        // the turtle's shell plates come off first; no big cuts under a shell
        Butchery.register(() -> EntityTypes.TURTLE, new Butchery.Yield(Mechanical.GAME_MEAT_SCRAPS, 2, 3, List.of(x(() -> Items.TURTLE_SCUTE, 1.0F, 1, 2)), List.of(x(() -> Items.BONE, 0.4F, 1, 1))));
        Butchery.wholeMeat(() -> EntityTypes.TURTLE);
        // squid: not Animals either; the ink sacs come off first, then the meat in one piece
        Butchery.alwaysCarcass(() -> EntityTypes.SQUID);
        Butchery.wholeMeat(() -> EntityTypes.SQUID);
        Butchery.register(() -> EntityTypes.SQUID, new Butchery.Yield(Mechanical.GAME_MEAT_SCRAPS, 1, 2, List.of(x(() -> Items.INK_SAC, 1.0F, 1, 3)), List.of()));
        Butchery.alwaysCarcass(() -> EntityTypes.GLOW_SQUID);
        Butchery.wholeMeat(() -> EntityTypes.GLOW_SQUID);
        Butchery.register(() -> EntityTypes.GLOW_SQUID, new Butchery.Yield(Mechanical.GAME_MEAT_SCRAPS, 1, 2, List.of(x(() -> Items.GLOW_INK_SAC, 1.0F, 1, 3)), List.of()));
        // bears and wolves: like the brown bear and the grey wolf
        Butchery.register(() -> EntityTypes.POLAR_BEAR, new Butchery.Yield(Mechanical.GAME_MEAT_SCRAPS, 4, 6, List.of(x(Wildlife.RAW_BEAR_PELT, 1.0F, 1, 1), hide(0.4F)),
                List.of(x(Wildlife.BEAR_FAT, 0.9F, 2, 3), x(Wildlife.BEAR_CLAW, 0.6F, 1, 3), x(Wildlife.SINEW, 0.6F, 1, 2))));
        Butchery.toughHide(() -> EntityTypes.POLAR_BEAR);
        Butchery.register(() -> EntityTypes.PANDA, new Butchery.Yield(Mechanical.GAME_MEAT_SCRAPS, 3, 5, List.of(x(Wildlife.RAW_BEAR_PELT, 1.0F, 1, 1)),
                List.of(x(Wildlife.BEAR_FAT, 0.7F, 1, 2), x(Wildlife.SINEW, 0.5F, 1, 1))));
        Butchery.toughHide(() -> EntityTypes.PANDA);
        Butchery.register(() -> EntityTypes.WOLF, new Butchery.Yield(Mechanical.GAME_MEAT_SCRAPS, 1, 2, List.of(x(Wildlife.RAW_WOLF_PELT, 1.0F, 1, 1)),
                List.of(x(Wildlife.SINEW, 0.5F, 1, 1), x(() -> Items.BONE, 0.6F, 1, 2))));
        // big game: desert-village camels, wandering traders' llamas, mountain goats, nether hoglins
        Butchery.register(() -> EntityTypes.CAMEL, new Butchery.Yield(Mechanical.GAME_MEAT_SCRAPS, 4, 6, List.of(hide(0.7F), hide(0.3F)), List.of(x(Wildlife.SINEW, 0.6F, 1, 2))));
        Butchery.register(() -> EntityTypes.LLAMA, new Butchery.Yield(Mechanical.GAME_MEAT_SCRAPS, 2, 3, List.of(x(Mechanical.WOOL_THREAD, 0.4F, 1, 2)),
                List.of(hide(0.4F), x(Wildlife.SINEW, 0.4F, 1, 1))));
        Butchery.register(() -> EntityTypes.TRADER_LLAMA, new Butchery.Yield(Mechanical.GAME_MEAT_SCRAPS, 2, 3, List.of(x(Mechanical.WOOL_THREAD, 0.4F, 1, 2)),
                List.of(hide(0.4F), x(Wildlife.SINEW, 0.4F, 1, 1))));
        Butchery.register(() -> EntityTypes.GOAT, new Butchery.Yield(Mechanical.MUTTON_SCRAPS, 1, 2, List.of(hide(0.3F)), List.of(x(Wildlife.HORN, 0.4F, 1, 2))));
        Butchery.register(() -> EntityTypes.HOGLIN, new Butchery.Yield(Mechanical.PORKCHOP_SCRAPS, 3, 5, List.of(hide(0.5F)),
                List.of(x(Wildlife.TUSK, 0.5F, 1, 2), x(Wildlife.LARD, 0.5F, 1, 2))));
        // keep vanilla behaviour: pets and tame-bred animals, the undead, insects, nether striders, sea creatures
        for (EntityType<?> type : List.of(EntityTypes.CAT, EntityTypes.AXOLOTL, EntityTypes.BEE, EntityTypes.STRIDER, EntityTypes.SNIFFER,
                EntityTypes.HAPPY_GHAST, EntityTypes.CAMEL_HUSK, EntityTypes.NAUTILUS, EntityTypes.ZOMBIE_NAUTILUS)) {
            Butchery.neverCarcass(() -> type);
        }
    }

    /** A roosting bat now and then drops Guano onto the floor below it: a pile that grows to 4 layers. */
    @SubscribeEvent
    static void dropGuano(EntityTickEvent.Post event) {
        if (!(event.getEntity() instanceof Bat bat) || (bat.tickCount + bat.getId()) % GUANO_CHECK != 0 || !bat.isResting()
                || !(bat.level() instanceof ServerLevel level) || level.getRandom().nextInt(GUANO_CHANCE) != 0) return;
        BlockPos.MutableBlockPos pos = bat.blockPosition().mutable();
        for (int i = 0; i < GUANO_DROP; i++) {
            pos.move(Direction.DOWN);
            BlockState below = level.getBlockState(pos);
            if (below.isAir()) continue;
            if (below.is(GUANO_PILE.get())) {
                int layers = below.getValue(GuanoPileBlock.LAYERS);
                if (layers < GuanoPileBlock.MAX_LAYERS) level.setBlock(pos, below.setValue(GuanoPileBlock.LAYERS, layers + 1), 3);
            } else {
                BlockPos at = pos.above();
                BlockState pile = GUANO_PILE.get().defaultBlockState();
                if (level.getBlockState(at).isAir() && pile.canSurvive(level, at)) level.setBlock(at, pile, 3);
            }
            level.playSound(null, pos, SoundEvents.MUD_PLACE, SoundSource.NEUTRAL, 0.3F, 1.2F);
            return;
        }
    }

    private VanillaGame() {}
}
