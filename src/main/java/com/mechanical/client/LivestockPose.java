package com.mechanical.client;

import java.util.List;

import com.google.common.reflect.TypeToken;
import com.mechanical.Husbandry;
import com.mechanical.Mechanical;

import net.minecraft.client.color.block.BlockTintSources;
import net.minecraft.client.model.geom.ModelPart;
import net.minecraft.client.renderer.entity.LivingEntityRenderer;
import net.minecraft.client.renderer.entity.state.EntityRenderState;
import net.minecraft.client.renderer.entity.state.EquineRenderState;
import net.minecraft.client.renderer.entity.state.LivingEntityRenderState;
import net.minecraft.resources.Identifier;
import net.minecraft.util.context.ContextKey;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.animal.Animal;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.RegisterColorHandlersEvent;
import net.neoforged.neoforge.client.renderstate.RegisterRenderStateModifiersEvent;

/**
 * Livestock poses from the synced needs ({@link Husbandry#NEEDS}): head down while eating or drinking (horses use their own
 * grazing pose), head hanging low while tired. Applied to the model parts by QuadrupedModelMixin, SheepModelMixin and
 * AbstractEquineModelMixin. Also tints the Feeding Trough's water.
 */
@EventBusSubscriber(modid = Mechanical.MODID, value = Dist.CLIENT)
public final class LivestockPose {
    private static final ContextKey<Boolean> EATING = new ContextKey<>(Identifier.fromNamespaceAndPath(Mechanical.MODID, "livestock_eating"));
    private static final ContextKey<Boolean> TIRED = new ContextKey<>(Identifier.fromNamespaceAndPath(Mechanical.MODID, "livestock_tired"));

    @SubscribeEvent
    static void renderStateModifiers(RegisterRenderStateModifiersEvent event) {
        event.registerEntityModifier(new TypeToken<LivingEntityRenderer<LivingEntity, LivingEntityRenderState, ?>>() {}, (entity, state) -> {
            if (!(entity instanceof Animal a) || !Husbandry.livestock(a)) return;
            Husbandry.Needs n = a.getData(Husbandry.NEEDS);
            long now = a.level().getGameTime();
            if (n.eating(now)) {
                if (state instanceof EquineRenderState equine) equine.eatAnimation = 1.0F;
                else state.setRenderData(EATING, true);
            } else if (n.tired(now)) {
                state.setRenderData(TIRED, true);
            }
        });
    }

    @SubscribeEvent
    static void registerTints(RegisterColorHandlersEvent.BlockTintSources event) {
        event.register(List.of(BlockTintSources.water()), Husbandry.FEEDING_TROUGH.get());
    }

    /** Four-legged models (cow, pig, sheep, goat): the head down to the ground or the trough, or hanging low. */
    public static void head(EntityRenderState state, ModelPart head) {
        if (Boolean.TRUE.equals(state.getRenderData(EATING))) {
            head.xRot = 1.1F;
            head.y += 4.0F;
        } else if (Boolean.TRUE.equals(state.getRenderData(TIRED))) {
            head.xRot = 0.85F;
            head.y += 2.5F;
        }
    }

    /** Horses: a tired horse hangs its head (eating uses the vanilla grazing pose). */
    public static void equineHead(EntityRenderState state, ModelPart headParts) {
        if (Boolean.TRUE.equals(state.getRenderData(TIRED))) headParts.xRot += 0.7F;
    }

    private LivestockPose() {}
}
