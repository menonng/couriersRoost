package com.courierpost.forge;

import com.courierpost.CourierPost;
import com.courierpost.command.CourierCommands;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.event.RegisterCommandsEvent;
import net.minecraftforge.event.ServerChatEvent;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.event.entity.player.PlayerEvent;
import net.minecraftforge.event.server.ServerStartedEvent;
import net.minecraftforge.event.server.ServerStoppingEvent;
import net.minecraftforge.eventbus.api.EventPriority;
import net.minecraftforge.eventbus.api.IEventBus;
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

        IEventBus bus = MinecraftForge.EVENT_BUS;
        bus.addListener(EventPriority.NORMAL, false, RegisterCommandsEvent.class,
                event -> CourierCommands.register(event.getDispatcher()));
        bus.addListener(EventPriority.NORMAL, false, ServerStartedEvent.class,
                event -> CourierPost.serverStarted(event.getServer()));
        bus.addListener(EventPriority.NORMAL, false, ServerStoppingEvent.class,
                event -> CourierPost.serverStopping(event.getServer()));
        bus.addListener(EventPriority.NORMAL, false, TickEvent.ServerTickEvent.class, event -> {
            if (event.phase == TickEvent.Phase.END) {
                CourierPost.serverTick();
            }
        });
        bus.addListener(EventPriority.NORMAL, false, PlayerEvent.PlayerLoggedOutEvent.class,
                event -> CourierPost.playerLeft(event.getEntity().getUUID()));
        bus.addListener(EventPriority.NORMAL, false, ServerChatEvent.class, event -> {
            CourierPost.chat(event.getPlayer(), event.getRawText());
            event.setCanceled(true);
        });
    }
}
