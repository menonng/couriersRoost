package com.courierpost.advancement;

import com.courierpost.chat.Localization;
import net.minecraft.advancements.Advancement;
import net.minecraft.advancements.FrameType;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.ComponentUtils;
import net.minecraft.network.chat.HoverEvent;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.PlayerAdvancements;
import net.minecraft.server.level.ServerPlayer;

public final class AdvancementLocalizer {
    public static final ResourceLocation ID = new ResourceLocation("courierpost", "courier/first_message");

    private static final String TITLE_KEY = "advancements.courierpost.first_message.title";
    private static final String DESCRIPTION_KEY = "advancements.courierpost.first_message.description";

    private AdvancementLocalizer() {
    }

    public static boolean grant(ServerPlayer player, MinecraftServer server) {
        Advancement advancement = server.getAdvancements().getAdvancement(ID);
        if (advancement == null) {
            return false;
        }
        PlayerAdvancements advancements = player.getAdvancements();
        if (advancements.getOrStartProgress(advancement).isDone()) {
            return false;
        }
        return advancements.award(advancement, "sent");
    }

    public static MutableComponent title(ServerPlayer player) {
        return Localization.text(player, TITLE_KEY);
    }

    public static MutableComponent description(ServerPlayer player) {
        return Localization.text(player, DESCRIPTION_KEY);
    }

    public static Component announcement(ServerPlayer viewer, ServerPlayer earner) {
        MutableComponent title = title(viewer);
        MutableComponent hover = title(viewer).append(Component.literal("\n")).append(description(viewer));
        MutableComponent bracketed = ComponentUtils.wrapInSquareBrackets(title)
                .withStyle(FrameType.TASK.getChatColor())
                .withStyle(style -> style.withHoverEvent(new HoverEvent(HoverEvent.Action.SHOW_TEXT, hover)));
        return Component.translatable("chat.type.advancement.task", earner.getDisplayName(), bracketed);
    }
}
