package com.courierpost.mixin;

import com.courierpost.bird.BirdManager;
import com.courierpost.bird.EntityCompat;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.animal.chicken.Chicken;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(Chicken.class)
public abstract class ChickenMixin {
    @Shadow
    public int eggTime;

    @Inject(method = "aiStep", at = @At("HEAD"))
    private void courierpost$suppressEggs(CallbackInfo info) {
        Entity self = (Entity) (Object) this;
        if (EntityCompat.hasTag(self, BirdManager.BIRD_TAG)) {
            eggTime = Integer.MAX_VALUE;
        }
    }
}
