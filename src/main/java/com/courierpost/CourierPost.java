package com.courierpost;

import com.courierpost.bird.BirdManager;
import com.courierpost.bird.BirdStorage;
import com.courierpost.bird.CourierBird;
import com.courierpost.chat.ChatHandler;
import com.courierpost.config.CourierConfig;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.nio.file.Path;
import java.util.List;
import java.util.UUID;

public final class CourierPost {
    public static final String MOD_ID = "courierpost";
    public static final Logger LOGGER = LoggerFactory.getLogger("Courier's Roost");

    private static Path configPath;
    private static CourierConfig config = new CourierConfig();
    private static BirdManager manager;

    private CourierPost() {
    }

    public static CourierConfig config() {
        return config;
    }

    public static BirdManager manager() {
        return manager;
    }

    public static void reloadConfig() {
        config.copyFrom(CourierConfig.load(configPath));
    }

    public static void saveConfig() {
        config.save(configPath);
    }

    public static void initialize(Path configDirectory) {
        configPath = configDirectory.resolve(MOD_ID + ".json");
        config = CourierConfig.load(configPath);
    }

    public static void serverStarted(MinecraftServer server) {
        manager = new BirdManager(server);
        if (config.saveBirds) {
            List<CourierBird> restored = BirdStorage.load(server);
            manager.addAll(restored);
            LOGGER.info("Restored {} courier birds", restored.size());
        }
    }

    public static void serverTick() {
        if (manager != null) {
            manager.tick();
        }
    }

    public static void serverStopping(MinecraftServer server) {
        if (manager == null) {
            return;
        }
        if (config.saveBirds) {
            BirdStorage.save(server, manager.birds());
        }
        manager.despawnAll();
        manager = null;
    }

    public static void playerLeft(UUID playerId) {
        ChatHandler.clearCooldown(playerId);
    }

    public static void chat(ServerPlayer sender, String content) {
        ChatHandler.handleGlobal(sender, content);
    }
}
