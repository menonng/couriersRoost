package com.courierpost.advancement;

import net.minecraft.advancements.AdvancementHolder;
import net.minecraft.advancements.DisplayInfo;
import net.minecraft.network.protocol.game.ClientboundUpdateAdvancementsPacket;
import net.minecraft.server.level.ServerPlayer;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

public final class AdvancementPackets {
    private AdvancementPackets() {
    }

    public static ClientboundUpdateAdvancementsPacket localize(ClientboundUpdateAdvancementsPacket packet, ServerPlayer player) {
        List<AdvancementHolder> holders = new ArrayList<>(packet.getAdded());
        boolean modified = false;

        for (int index = 0; index < holders.size(); index++) {
            AdvancementHolder holder = holders.get(index);
            if (!holder.id().equals(AdvancementLocalizer.ID)) {
                continue;
            }
            Optional<DisplayInfo> display = holder.value().display();
            if (display.isEmpty()) {
                continue;
            }
            holders.set(index, new AdvancementHolder(holder.id(),
                    AdvancementLocalizer.rebuild(holder.value(), translate(display.get(), player))));
            modified = true;
        }

        if (!modified) {
            return packet;
        }
        return new ClientboundUpdateAdvancementsPacket(
                packet.shouldReset(),
                holders,
                packet.getRemoved(),
                packet.getProgress(),
                packet.shouldShowAdvancements());
    }

    private static DisplayInfo translate(DisplayInfo source, ServerPlayer player) {
        DisplayInfo display = new DisplayInfo(
                source.getIcon(),
                AdvancementLocalizer.title(player),
                AdvancementLocalizer.description(player),
                source.getBackground(),
                source.getType(),
                source.shouldShowToast(),
                source.shouldAnnounceChat(),
                source.isHidden());
        display.setLocation(source.getX(), source.getY());
        return display;
    }
}
