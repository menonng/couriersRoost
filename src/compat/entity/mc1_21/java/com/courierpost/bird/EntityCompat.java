package com.courierpost.bird;

import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntitySpawnReason;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.animal.chicken.Chicken;

public final class EntityCompat {
    private EntityCompat() {
    }

    public static Chicken createBird(ServerLevel level) {
        return EntityType.CHICKEN.create(level, EntitySpawnReason.COMMAND);
    }

    public static void makeInvulnerable(Entity entity) {
        entity.setInvulnerable(true);
    }

    public static boolean hasTag(Entity entity, String tag) {
        return entity.getTags().contains(tag);
    }
}
