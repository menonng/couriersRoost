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
        List<ClientboundUpdateAdvancementsPacket.PositionedAdvancement> entries = new ArrayList<>(packet.added());
        boolean modified = false;

        for (int index = 0; index < entries.size(); index++) {
            ClientboundUpdateAdvancementsPacket.PositionedAdvancement entry = entries.get(index);
            AdvancementHolder holder = entry.advancement();
            if (!holder.id().equals(AdvancementLocalizer.ID)) {
                continue;
            }
            Optional<DisplayInfo> display = holder.value().display();
            if (display.isEmpty()) {
                continue;
            }
            AdvancementHolder localized = new AdvancementHolder(holder.id(),
                    AdvancementLocalizer.rebuild(holder.value(), translate(display.get(), player)));
            entries.set(index, new ClientboundUpdateAdvancementsPacket.PositionedAdvancement(
                    localized, entry.x(), entry.y()));
            modified = true;
        }

        if (!modified) {
            return packet;
        }
        return new ClientboundUpdateAdvancementsPacket(
                packet.shouldReset(),
                entries,
                packet.removed(),
                packet.progress(),
                packet.showAdvancements());
    }

    private static DisplayInfo translate(DisplayInfo source, ServerPlayer player) {
        return new DisplayInfo(
                source.icon(),
                AdvancementLocalizer.title(player),
                AdvancementLocalizer.description(player),
                source.background(),
                source.type(),
                source.showToast(),
                source.announceToChat(),
                source.hidden());
    }
}
