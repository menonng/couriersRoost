package com.courierpost.advancement;

import com.courierpost.chat.Localization;
import net.minecraft.advancements.Advancement;
import net.minecraft.advancements.AdvancementType;
import net.minecraft.advancements.DisplayInfo;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.ComponentUtils;
import net.minecraft.network.chat.HoverEvent;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.resources.Identifier;
import net.minecraft.server.level.ServerPlayer;

import java.util.Optional;

public final class AdvancementLocalizer {
    public static final Identifier ID = Identifier.fromNamespaceAndPath("courierpost", "courier/first_message");

    private static final String TITLE_KEY = "advancements.courierpost.first_message.title";
    private static final String DESCRIPTION_KEY = "advancements.courierpost.first_message.description";

    private AdvancementLocalizer() {
    }

    public static Advancement rebuild(Advancement source, DisplayInfo display) {
        return new Advancement(
                source.parent(),
                Optional.of(display),
                source.rewards(),
                source.criteria(),
                source.requirements(),
                source.sendsTelemetryEvent());
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
                .withStyle(AdvancementType.TASK.getChatColor())
                .withStyle(style -> style.withHoverEvent(new HoverEvent.ShowText(hover)));
        return Component.translatable("chat.type.advancement.task", earner.getDisplayName(), bracketed);
    }
}
