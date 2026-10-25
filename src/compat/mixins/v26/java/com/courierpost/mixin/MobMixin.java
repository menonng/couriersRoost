package com.courierpost.mixin;

import com.courierpost.bird.BirdManager;
import com.courierpost.bird.EntityCompat;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Mob;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(Mob.class)
public abstract class MobMixin {
    @Inject(method = "canAttack(Lnet/minecraft/world/entity/LivingEntity;)Z", at = @At("HEAD"), cancellable = true)
    private void courierpost$ignoreCouriers(LivingEntity target, CallbackInfoReturnable<Boolean> info) {
        if (EntityCompat.hasTag(target, BirdManager.BIRD_TAG)) {
            info.setReturnValue(false);
        }
    }
}
