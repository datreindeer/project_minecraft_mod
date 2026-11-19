package com.mechanical.client;

import com.mechanical.Mechanical;
import com.mechanical.Undead;
import com.mojang.blaze3d.vertex.PoseStack;

import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.entity.SkeletonRenderer;
import net.minecraft.client.renderer.entity.ZombieRenderer;
import net.minecraft.client.renderer.entity.state.SkeletonRenderState;
import net.minecraft.client.renderer.entity.state.ZombieRenderState;
import net.minecraft.resources.Identifier;
import net.minecraft.world.entity.EntityTypes;
import net.minecraft.world.entity.monster.skeleton.Skeleton;
import net.minecraft.world.entity.monster.zombie.Zombie;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.EntityRenderersEvent;

/** Replaces the vanilla zombie and skeleton renderers with subclasses that pick the skin from {@link Undead}'s look. */
@EventBusSubscriber(modid = Mechanical.MODID, value = Dist.CLIENT)
public final class UndeadRenderers {
    private static final Identifier[] ZOMBIE_SKINS = skins("zombie", Undead.ZombieLook.values());
    private static final Identifier[] SKELETON_SKINS = skins("skeleton", Undead.SkeletonLook.values());

    private static Identifier[] skins(String kind, Enum<?>[] looks) {
        Identifier[] out = new Identifier[looks.length];
        for (Enum<?> look : looks) out[look.ordinal()] = Identifier.fromNamespaceAndPath(Mechanical.MODID,
                "textures/entity/undead/" + kind + "_" + look.name().toLowerCase(java.util.Locale.ROOT) + ".png");
        return out;
    }

    @SubscribeEvent
    static void registerRenderers(EntityRenderersEvent.RegisterRenderers event) {
        event.registerEntityRenderer(EntityTypes.ZOMBIE, Zombies::new);
        event.registerEntityRenderer(EntityTypes.SKELETON, Skeletons::new);
    }

    static final class ZombieState extends ZombieRenderState {
        Undead.ZombieLook look = Undead.ZombieLook.HIDE_MAN;
    }

    static final class Zombies extends ZombieRenderer {
        Zombies(EntityRendererProvider.Context context) {
            super(context);
        }

        @Override
        public ZombieRenderState createRenderState() {
            return new ZombieState();
        }

        @Override
        public void extractRenderState(Zombie zombie, ZombieRenderState state, float partialTicks) {
            super.extractRenderState(zombie, state, partialTicks);
            ((ZombieState) state).look = Undead.zombieLook(zombie);
        }

        @Override
        public Identifier getTextureLocation(ZombieRenderState state) {
            // babies keep the vanilla baby skin (own model and UV layout)
            return state.isBaby ? super.getTextureLocation(state) : ZOMBIE_SKINS[((ZombieState) state).look.ordinal()];
        }

        @Override
        protected void scale(ZombieRenderState state, PoseStack poseStack) {
            if (!state.isBaby && ((ZombieState) state).look == Undead.ZombieLook.GAUNT) poseStack.scale(0.82F, 1.0F, 0.82F);
        }
    }

    static final class SkeletonState extends SkeletonRenderState {
        Undead.SkeletonLook look = Undead.SkeletonLook.BONE;
    }

    static final class Skeletons extends SkeletonRenderer {
        Skeletons(EntityRendererProvider.Context context) {
            super(context);
        }

        @Override
        public SkeletonRenderState createRenderState() {
            return new SkeletonState();
        }

        @Override
        public void extractRenderState(Skeleton skeleton, SkeletonRenderState state, float partialTicks) {
            super.extractRenderState(skeleton, state, partialTicks);
            ((SkeletonState) state).look = Undead.skeletonLook(skeleton);
        }

        @Override
        public Identifier getTextureLocation(SkeletonRenderState state) {
            Undead.SkeletonLook look = ((SkeletonState) state).look;
            return look == Undead.SkeletonLook.BONE ? super.getTextureLocation(state) : SKELETON_SKINS[look.ordinal()];
        }
    }

    private UndeadRenderers() {}
}
