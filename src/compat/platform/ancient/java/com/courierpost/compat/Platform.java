package com.courierpost.compat;

import net.minecraft.commands.CommandSourceStack;
import net.minecraft.core.particles.DustParticleOptions;
import net.minecraft.core.particles.ParticleOptions;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.level.GameRules;
import net.minecraft.world.level.Level;
import org.joml.Vector3f;

import java.util.function.Predicate;

public final class Platform {
    private static final int OPERATOR_LEVEL = 2;
    private static final int ADMIN_LEVEL = 4;

    private Platform() {
    }

    public static String playerName(ServerPlayer player) {
        return player.getGameProfile().getName();
    }

    public static String language(ServerPlayer player) {
        return ((LanguageHolder) player).courierpost$language();
    }

    public static ServerLevel levelOf(ServerPlayer player) {
        return player.serverLevel();
    }

    public static boolean isOperator(ServerPlayer player) {
        return player.hasPermissions(OPERATOR_LEVEL);
    }

    public static Predicate<CommandSourceStack> adminOnly() {
        return source -> source.hasPermission(ADMIN_LEVEL);
    }

    public static boolean announceAdvancements(ServerLevel level) {
        return level.getGameRules().getBoolean(GameRules.RULE_ANNOUNCE_ADVANCEMENTS);
    }

    public static ParticleOptions dust(int color, float scale) {
        Vector3f rgb = new Vector3f(
                ((color >> 16) & 0xFF) / 255.0F,
                ((color >> 8) & 0xFF) / 255.0F,
                (color & 0xFF) / 255.0F);
        return new DustParticleOptions(rgb, scale);
    }

    public static String dimensionId(ResourceKey<Level> dimension) {
        return dimension.location().toString();
    }

    public static ResourceKey<Level> dimensionKey(String id) {
        return ResourceKey.create(Registries.DIMENSION, new ResourceLocation(id));
    }

    public static void place(Entity entity, double x, double y, double z, float yaw) {
        entity.moveTo(x, y, z, yaw, 0.0F);
    }
}
