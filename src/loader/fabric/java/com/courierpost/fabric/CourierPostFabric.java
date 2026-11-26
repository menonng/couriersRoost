package com.courierpost.fabric;

import com.courierpost.CourierPost;
import com.courierpost.command.CourierCommands;
import net.fabricmc.api.DedicatedServerModInitializer;
import net.fabricmc.fabric.api.command.v2.CommandRegistrationCallback;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerLifecycleEvents;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerTickEvents;
import net.fabricmc.fabric.api.message.v1.ServerMessageEvents;
import net.fabricmc.fabric.api.networking.v1.ServerPlayConnectionEvents;
import net.fabricmc.loader.api.FabricLoader;

public final class CourierPostFabric implements DedicatedServerModInitializer {
    @Override
    public void onInitializeServer() {
        CourierPost.initialize(FabricLoader.getInstance().getConfigDir());

        CommandRegistrationCallback.EVENT.register((dispatcher, access, environment) ->
                CourierCommands.register(dispatcher));
        ServerLifecycleEvents.SERVER_STARTED.register(CourierPost::serverStarted);
        ServerTickEvents.END_SERVER_TICK.register(server -> CourierPost.serverTick());
        ServerLifecycleEvents.SERVER_STOPPING.register(CourierPost::serverStopping);
        ServerPlayConnectionEvents.DISCONNECT.register((handler, server) ->
                CourierPost.playerLeft(handler.player.getUUID()));
        ServerMessageEvents.ALLOW_CHAT_MESSAGE.register((message, sender, params) -> {
            CourierPost.chat(sender, message.signedContent());
            return false;
        });
    }
}
