package com.courierpost.bird;

import net.minecraft.resources.ResourceKey;
import net.minecraft.world.entity.animal.chicken.Chicken;
import net.minecraft.world.level.Level;

import java.util.UUID;

public final class CourierBird {
    public UUID id;
    public UUID senderId;
    public String senderName;
    public UUID recipientId;
    public MessageKind kind;
    public String content;
    public String teamName;

    public ResourceKey<Level> dimension;
    public double x;
    public double y;
    public double z;
    public double yaw;

    public double speedFactor;
    public int delayTicks;
    public int elapsedTicks;
    public int deathTick = -1;
    public int lostTick = -1;
    public boolean baby;
    public int variantIndex;

    public transient Chicken entity;
    public transient double lastSpeed;
    public transient double remainingDistance;
    public transient int landingTicks;

    public int elapsedSeconds() {
        return elapsedTicks / 20;
    }
}
