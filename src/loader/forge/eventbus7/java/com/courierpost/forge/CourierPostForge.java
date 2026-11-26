package com.courierpost.forge;

import com.courierpost.CourierPost;
import com.courierpost.command.CourierCommands;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.event.RegisterCommandsEvent;
import net.minecraftforge.event.ServerChatEvent;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.event.entity.player.PlayerEvent;
import net.minecraftforge.event.server.ServerStartedEvent;
import net.minecraftforge.event.server.ServerStoppingEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.loading.FMLEnvironment;
import net.minecraftforge.fml.loading.FMLPaths;

@Mod(CourierPost.MOD_ID)
public final class CourierPostForge {
    public CourierPostForge() {
        if (FMLEnvironment.dist != Dist.DEDICATED_SERVER) {
            return;
        }
        CourierPost.initialize(FMLPaths.CONFIGDIR.get());

        RegisterCommandsEvent.BUS.addListener(event -> CourierCommands.register(event.getDispatcher()));
        ServerStartedEvent.BUS.addListener(event -> CourierPost.serverStarted(event.getServer()));
        ServerStoppingEvent.BUS.addListener(event -> CourierPost.serverStopping(event.getServer()));
        TickEvent.ServerTickEvent.Post.BUS.addListener(event -> CourierPost.serverTick());
        PlayerEvent.PlayerLoggedOutEvent.BUS.addListener(event -> CourierPost.playerLeft(event.getEntity().getUUID()));
        ServerChatEvent.BUS.addListener(event -> {
            CourierPost.chat(event.getPlayer(), event.getRawText());
            return true;
        });
    }
}
