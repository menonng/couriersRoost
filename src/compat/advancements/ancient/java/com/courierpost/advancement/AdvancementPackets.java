package com.courierpost.advancement;

import com.courierpost.compat.Platform;
import net.minecraft.advancements.Advancement;
import net.minecraft.advancements.DisplayInfo;
import net.minecraft.network.protocol.game.ClientboundUpdateAdvancementsPacket;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;

public final class AdvancementPackets {
    private AdvancementPackets() {
    }

    public static ClientboundUpdateAdvancementsPacket localize(ClientboundUpdateAdvancementsPacket packet, ServerPlayer player) {
        Advancement.Builder builder = packet.getAdded().get(AdvancementLocalizer.ID);
        if (builder == null) {
            return packet;
        }
        MinecraftServer server = Platform.levelOf(player).getServer();
        Advancement source = server.getAdvancements().getAdvancement(AdvancementLocalizer.ID);
        if (source == null || source.getDisplay() == null) {
            return packet;
        }
        builder.display(translate(source.getDisplay(), player));
        return packet;
    }

    private static DisplayInfo translate(DisplayInfo source, ServerPlayer player) {
        DisplayInfo display = new DisplayInfo(
                source.getIcon(),
                AdvancementLocalizer.title(player),
                AdvancementLocalizer.description(player),
                source.getBackground(),
                source.getFrame(),
                source.shouldShowToast(),
                source.shouldAnnounceChat(),
                source.isHidden());
        display.setLocation(source.getX(), source.getY());
        return display;
    }
}
