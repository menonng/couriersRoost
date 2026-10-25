package com.courierpost.bird;

import net.minecraft.core.Holder;
import net.minecraft.core.Registry;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.animal.chicken.Chicken;
import net.minecraft.world.entity.animal.chicken.ChickenVariant;
import net.minecraft.world.entity.animal.chicken.ChickenVariants;

import java.util.List;

public final class BirdVariants {
    private static final List<ResourceKey<ChickenVariant>> BABY_VARIANTS = List.of(
            ChickenVariants.TEMPERATE,
            ChickenVariants.WARM,
            ChickenVariants.COLD
    );

    private BirdVariants() {
    }

    public static int count() {
        return BABY_VARIANTS.size();
    }

    public static void apply(ServerLevel level, Chicken bird, boolean baby, int variantIndex) {
        bird.setBaby(baby);
        if (!baby) {
            return;
        }
        Registry<ChickenVariant> registry = level.registryAccess().lookupOrThrow(Registries.CHICKEN_VARIANT);
        ResourceKey<ChickenVariant> key = BABY_VARIANTS.get(Math.floorMod(variantIndex, BABY_VARIANTS.size()));
        Holder<ChickenVariant> holder = registry.getOptional(key)
                .map(registry::wrapAsHolder)
                .orElse(null);
        if (holder != null) {
            bird.setVariant(holder);
        }
    }
}
