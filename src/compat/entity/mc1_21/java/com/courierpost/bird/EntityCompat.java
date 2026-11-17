package com.courierpost.bird;

import net.minecraft.core.Holder;
import net.minecraft.core.Registry;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntitySpawnReason;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.animal.chicken.Chicken;
import net.minecraft.world.entity.animal.chicken.ChickenVariant;
import net.minecraft.world.entity.animal.chicken.ChickenVariants;

import java.util.List;

public final class EntityCompat {
    private static final List<ResourceKey<ChickenVariant>> BABY_VARIANTS = List.of(
            ChickenVariants.TEMPERATE,
            ChickenVariants.WARM,
            ChickenVariants.COLD
    );

    private EntityCompat() {
    }

    public static Mob createBird(ServerLevel level) {
        return EntityType.CHICKEN.create(level, EntitySpawnReason.COMMAND);
    }

    public static boolean isBird(Entity entity) {
        return entity instanceof Chicken;
    }

    public static void makeInvulnerable(Entity entity) {
        entity.setInvulnerable(true);
    }

    public static boolean hasTag(Entity entity, String tag) {
        return entity.getTags().contains(tag);
    }

    public static int variantCount() {
        return BABY_VARIANTS.size();
    }

    public static void applyVariant(ServerLevel level, Mob bird, boolean baby, int variantIndex) {
        Chicken chicken = (Chicken) bird;
        chicken.setBaby(baby);
        if (!baby) {
            return;
        }
        Registry<ChickenVariant> registry = level.registryAccess().lookupOrThrow(Registries.CHICKEN_VARIANT);
        ResourceKey<ChickenVariant> key = BABY_VARIANTS.get(Math.floorMod(variantIndex, BABY_VARIANTS.size()));
        Holder<ChickenVariant> holder = registry.getOptional(key)
                .map(registry::wrapAsHolder)
                .orElse(null);
        if (holder != null) {
            chicken.setVariant(holder);
        }
    }
}
