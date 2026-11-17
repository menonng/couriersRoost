package com.courierpost.bird;

import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.animal.Chicken;

public final class EntityCompat {
    private EntityCompat() {
    }

    public static Mob createBird(ServerLevel level) {
        return EntityType.CHICKEN.create(level);
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
        return 1;
    }

    public static void applyVariant(ServerLevel level, Mob bird, boolean baby, int variantIndex) {
        ((Chicken) bird).setBaby(baby);
    }
}
