package com.courierpost.bird;

import com.courierpost.CourierPost;
import com.courierpost.chat.CourierText;
import com.courierpost.config.CourierConfig;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.animal.chicken.Chicken;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Iterator;
import java.util.List;
import java.util.Random;
import java.util.UUID;

public final class BirdManager {
    public static final String BIRD_TAG = "courierpost_bird";

    private static final double ARRIVAL_RADIUS = 1.5D;
    private static final double CRUISE_OFFSET = 10.0D;
    private static final double CLIMB_RATIO = 0.5D;
    private static final int LANDING_TICKS = 5;
    private static final int AUTOSAVE_INTERVAL = 600;

    private final MinecraftServer server;
    private final List<CourierBird> birds = new ArrayList<>();
    private final Random random = new Random();
    private boolean orphansCleared;
    private int autosaveTimer = AUTOSAVE_INTERVAL;

    public BirdManager(MinecraftServer server) {
        this.server = server;
    }

    public MinecraftServer server() {
        return server;
    }

    public List<CourierBird> birds() {
        return birds;
    }

    public void addAll(Collection<CourierBird> restored) {
        birds.addAll(restored);
    }

    public void dispatch(ServerPlayer sender, ServerPlayer recipient, MessageKind kind, String content, String teamName) {
        CourierConfig config = CourierPost.config();
        CourierBird bird = new CourierBird();
        bird.id = UUID.randomUUID();
        bird.senderId = sender.getUUID();
        bird.senderName = sender.getGameProfile().name();
        bird.recipientId = recipient.getUUID();
        bird.kind = kind;
        bird.content = content;
        bird.teamName = teamName == null ? "" : teamName;
        bird.dimension = sender.level().dimension();
        bird.x = sender.getX();
        bird.y = sender.getY() + 1.5D;
        bird.z = sender.getZ();
        bird.yaw = sender.getYRot();
        bird.delayTicks = config.baseDimensionDelay * 20;
        bird.speedFactor = 1.0D + (random.nextDouble() * 2.0D - 1.0D) * config.speedVariation;
        bird.baby = random.nextDouble() < 0.1D;
        bird.variantIndex = random.nextInt(BirdVariants.count());

        int estimated = estimateTicks(bird, recipient, config);
        if (random.nextDouble() < config.lostChance) {
            bird.lostTick = bird.delayTicks + random.nextInt(Math.max(1, estimated));
        } else if (random.nextDouble() < config.birdDeathChance) {
            bird.deathTick = bird.delayTicks + random.nextInt(Math.max(1, estimated));
        }

        birds.add(bird);
        spawnEntity(bird);
    }

    private int estimateTicks(CourierBird bird, ServerPlayer recipient, CourierConfig config) {
        double distance;
        if (bird.dimension.equals(recipient.level().dimension())) {
            distance = Math.sqrt(squaredHorizontal(bird.x - recipient.getX(), bird.z - recipient.getZ()));
        } else {
            distance = Math.sqrt(squaredHorizontal(recipient.getX(), recipient.getZ()));
        }
        double speed = Math.max(0.1D, config.birdSpeed * bird.speedFactor);
        return Math.max(1, (int) (distance / speed * 20.0D));
    }

    public void tick() {
        if (!orphansCleared) {
            clearOrphans();
            orphansCleared = true;
        }

        CourierConfig config = CourierPost.config();
        int lifetimeTicks = config.birdMaxLifetime * 20;
        Iterator<CourierBird> iterator = birds.iterator();

        while (iterator.hasNext()) {
            CourierBird bird = iterator.next();
            bird.elapsedTicks++;

            if (bird.elapsedTicks > lifetimeTicks) {
                despawnEntity(bird);
                iterator.remove();
                continue;
            }
            if (bird.lostTick >= 0 && bird.elapsedTicks >= bird.lostTick) {
                despawnEntity(bird);
                iterator.remove();
                continue;
            }
            if (bird.deathTick >= 0 && bird.elapsedTicks >= bird.deathTick) {
                notifyDeath(bird);
                despawnEntity(bird);
                iterator.remove();
                continue;
            }

            ServerPlayer recipient = server.getPlayerList().getPlayer(bird.recipientId);
            if (recipient == null) {
                if (!config.deliverOfflineMessages) {
                    despawnEntity(bird);
                    iterator.remove();
                }
                continue;
            }

            if (bird.delayTicks > 0) {
                bird.delayTicks--;
                syncEntity(bird);
                continue;
            }

            ResourceKey<Level> targetDimension = recipient.level().dimension();
            if (!bird.dimension.equals(targetDimension)) {
                enterDimension(bird, recipient, config);
            }

            ServerLevel level = server.getLevel(bird.dimension);
            if (level == null) {
                despawnEntity(bird);
                iterator.remove();
                continue;
            }

            double perTick = effectiveSpeed(bird, level, config) / 20.0D;
            bird.lastSpeed = perTick * 20.0D;

            double dx = recipient.getX() - bird.x;
            double dz = recipient.getZ() - bird.z;
            double horizontal = Math.sqrt(dx * dx + dz * dz);
            bird.remainingDistance = horizontal;

            if (horizontal > ARRIVAL_RADIUS) {
                double step = Math.min(perTick, horizontal);
                bird.x += dx / horizontal * step;
                bird.z += dz / horizontal * step;
                double cruise = recipient.getY() + CRUISE_OFFSET;
                double dy = cruise - bird.y;
                bird.y += Math.signum(dy) * Math.min(Math.abs(dy), perTick * CLIMB_RATIO);
                bird.yaw = Mth.wrapDegrees(Math.toDegrees(Math.atan2(dz, dx)) - 90.0D);
                syncEntity(bird);
                continue;
            }

            double targetY = recipient.getY();
            double dy = targetY - bird.y;
            double distance = Math.sqrt(dx * dx + dy * dy + dz * dz);
            bird.remainingDistance = distance;

            if (distance > perTick) {
                bird.x += dx / distance * perTick;
                bird.y += dy / distance * perTick;
                bird.z += dz / distance * perTick;
                bird.yaw = Mth.wrapDegrees(Math.toDegrees(Math.atan2(dz, dx)) - 90.0D);
                syncEntity(bird);
                continue;
            }

            bird.x = recipient.getX();
            bird.y = targetY;
            bird.z = recipient.getZ();
            bird.remainingDistance = 0.0D;
            syncEntity(bird);

            if (bird.landingTicks < LANDING_TICKS) {
                bird.landingTicks++;
                continue;
            }

            deliver(bird, recipient);
            despawnEntity(bird);
            iterator.remove();
        }

        if (config.saveBirds && --autosaveTimer <= 0) {
            autosaveTimer = AUTOSAVE_INTERVAL;
            BirdStorage.save(server, birds);
        }
    }

    private void enterDimension(CourierBird bird, ServerPlayer recipient, CourierConfig config) {
        ServerLevel source = server.getLevel(bird.dimension);
        ServerLevel destination = recipient.level();
        double newX = 0.0D;
        double newZ = 0.0D;
        if (config.useCoordinateConversion && source != null) {
            double scale = source.dimensionType().coordinateScale() / destination.dimensionType().coordinateScale();
            newX = bird.x * scale;
            newZ = bird.z * scale;
        }
        despawnEntity(bird);
        bird.dimension = destination.dimension();
        bird.x = newX;
        bird.z = newZ;
        bird.y = recipient.getY() + CRUISE_OFFSET;
        spawnEntity(bird);
    }

    private double effectiveSpeed(CourierBird bird, ServerLevel level, CourierConfig config) {
        double base = bird.dimension.equals(Level.NETHER) ? config.netherSpeed : config.birdSpeed;
        double speed = base * bird.speedFactor;
        if (bird.dimension.equals(Level.OVERWORLD)) {
            if (level.isThundering()) {
                speed *= config.thunderMultiplier;
            } else if (level.isRaining()) {
                speed *= config.rainMultiplier;
            }
        }
        return Math.max(0.1D, speed);
    }

    private void deliver(CourierBird bird, ServerPlayer recipient) {
        recipient.sendSystemMessage(CourierText.incoming(bird));
        ArrivalEffect.play(recipient.level(), recipient.getX(),
                recipient.getY() + 0.3D, recipient.getZ(), random);
    }

    private void notifyDeath(CourierBird bird) {
        ServerPlayer sender = server.getPlayerList().getPlayer(bird.senderId);
        if (sender != null) {
            sender.sendSystemMessage(CourierText.died(sender));
        }
    }

    private void spawnEntity(CourierBird bird) {
        if (!CourierPost.config().showBird) {
            return;
        }
        ServerLevel level = server.getLevel(bird.dimension);
        if (level == null) {
            return;
        }
        Chicken entity = EntityCompat.createBird(level);
        if (entity == null) {
            return;
        }
        entity.snapTo(bird.x, bird.y, bird.z, (float) bird.yaw, 0.0F);
        entity.setNoAi(true);
        EntityCompat.makeInvulnerable(entity);
        entity.setSilent(true);
        entity.setNoGravity(true);
        entity.setPersistenceRequired();
        entity.noPhysics = true;
        entity.addTag(BIRD_TAG);
        BirdVariants.apply(level, entity, bird.baby, bird.variantIndex);
        level.addFreshEntity(entity);
        bird.entity = entity;
    }

    private void syncEntity(CourierBird bird) {
        CourierConfig config = CourierPost.config();
        if (!config.showBird) {
            if (bird.entity != null) {
                despawnEntity(bird);
            }
            return;
        }
        if (bird.entity == null || bird.entity.isRemoved()) {
            spawnEntity(bird);
            return;
        }
        Chicken entity = bird.entity;
        entity.noPhysics = true;
        entity.setDeltaMovement(Vec3.ZERO);
        entity.snapTo(bird.x, bird.y, bird.z, (float) bird.yaw, 0.0F);
        entity.setYHeadRot((float) bird.yaw);
        entity.setYBodyRot((float) bird.yaw);
    }

    private void despawnEntity(CourierBird bird) {
        if (bird.entity != null) {
            bird.entity.discard();
            bird.entity = null;
        }
    }

    public void despawnAll() {
        for (CourierBird bird : birds) {
            despawnEntity(bird);
        }
    }

    private void clearOrphans() {
        List<Entity> orphans = new ArrayList<>();
        for (ServerLevel level : server.getAllLevels()) {
            for (Entity entity : level.getAllEntities()) {
                if (entity instanceof Chicken && EntityCompat.hasTag(entity, BIRD_TAG)) {
                    orphans.add(entity);
                }
            }
        }
        for (Entity entity : orphans) {
            entity.discard();
        }
        for (CourierBird bird : birds) {
            bird.entity = null;
        }
    }

    private static double squaredHorizontal(double dx, double dz) {
        return dx * dx + dz * dz;
    }
}
