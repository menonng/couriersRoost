package com.courierpost;

import com.courierpost.bird.BirdManager;
import com.courierpost.bird.BirdStorage;
import com.courierpost.bird.CourierBird;
import com.courierpost.chat.ChatHandler;
import com.courierpost.command.CourierCommands;
import com.courierpost.config.CourierConfig;
import net.fabricmc.api.DedicatedServerModInitializer;
import net.fabricmc.fabric.api.command.v2.CommandRegistrationCallback;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerLifecycleEvents;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerTickEvents;
import net.fabricmc.fabric.api.message.v1.ServerMessageEvents;
import net.fabricmc.fabric.api.networking.v1.ServerPlayConnectionEvents;
import net.fabricmc.loader.api.FabricLoader;
import net.minecraft.server.MinecraftServer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.nio.file.Path;
import java.util.List;

public final class CourierPost implements DedicatedServerModInitializer {
    public static final String MOD_ID = "courierpost";
    public static final Logger LOGGER = LoggerFactory.getLogger("The Courier's Roost");

    private static final Path CONFIG_PATH =
            FabricLoader.getInstance().getConfigDir().resolve(MOD_ID + ".json");

    private static CourierConfig config = new CourierConfig();
    private static BirdManager manager;

    public static CourierConfig config() {
        return config;
    }

    public static BirdManager manager() {
        return manager;
    }

    public static void reloadConfig() {
        config.copyFrom(CourierConfig.load(CONFIG_PATH));
    }

    public static void saveConfig() {
        config.save(CONFIG_PATH);
    }

    @Override
    public void onInitializeServer() {
        config = CourierConfig.load(CONFIG_PATH);

        CommandRegistrationCallback.EVENT.register((dispatcher, access, environment) ->
                CourierCommands.register(dispatcher));

        ServerLifecycleEvents.SERVER_STARTED.register(server -> {
            manager = new BirdManager(server);
            if (config.saveBirds) {
                List<CourierBird> restored = BirdStorage.load(server);
                manager.addAll(restored);
                LOGGER.info("Restored {} courier birds", restored.size());
            }
        });

        ServerTickEvents.END_SERVER_TICK.register(server -> {
            if (manager != null) {
                manager.tick();
            }
        });

        ServerLifecycleEvents.SERVER_STOPPING.register(this::shutdown);

        ServerPlayConnectionEvents.DISCONNECT.register((handler, server) ->
                ChatHandler.clearCooldown(handler.player.getUUID()));

        ServerMessageEvents.ALLOW_CHAT_MESSAGE.register((message, sender, params) -> {
            ChatHandler.handleGlobal(sender, message.signedContent());
            return false;
        });
    }

    private void shutdown(MinecraftServer server) {
        if (manager == null) {
            return;
        }
        if (config.saveBirds) {
            BirdStorage.save(server, manager.birds());
        }
        manager.despawnAll();
        manager = null;
    }
}
