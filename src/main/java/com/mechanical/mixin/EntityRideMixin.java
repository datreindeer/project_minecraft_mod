package com.mechanical.mixin;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;

import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;

/** Players can't be ridden (their type isn't saved); a downed player on a mate's shoulders may (clan.Downed#mayRide). */
@Mixin(Entity.class)
public abstract class EntityRideMixin {
    @Redirect(method = "startRiding(Lnet/minecraft/world/entity/Entity;ZZ)Z",
            at = @At(value = "INVOKE", target = "Lnet/minecraft/world/entity/EntityType;canSerialize()Z"))
    private boolean mechanical$carryDowned(EntityType<?> type, Entity vehicle, boolean force, boolean sendEventAndTriggers) {
        return type.canSerialize() || com.mechanical.clan.Downed.mayRide((Entity) (Object) this, vehicle);
    }
}
