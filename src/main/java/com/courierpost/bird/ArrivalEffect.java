package com.courierpost.bird;

import net.minecraft.core.particles.DustParticleOptions;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.Mth;

import java.util.Random;

public final class ArrivalEffect {
    private static final int DUST_COUNT = 8;
    private static final int SPARK_COUNT = 6;
    private static final float DUST_SCALE = 0.8F;
    private static final double SPREAD = 0.25D;
    private static final float VOLUME = 0.6F;

    private ArrivalEffect() {
    }

    public static void play(ServerLevel level, double x, double y, double z, Random random) {
        int color = Mth.hsvToRgb(random.nextFloat(), 0.85F, 1.0F);
        level.sendParticles(new DustParticleOptions(color, DUST_SCALE), x, y, z, DUST_COUNT, SPREAD, SPREAD, SPREAD, 0.0D);
        level.sendParticles(ParticleTypes.FIREWORK, x, y, z, SPARK_COUNT, SPREAD, SPREAD, SPREAD, 0.02D);
        level.playSound(null, x, y, z, SoundEvents.AMETHYST_BLOCK_CHIME, SoundSource.PLAYERS,
                VOLUME, 1.2F + random.nextFloat() * 0.3F);
    }
}
