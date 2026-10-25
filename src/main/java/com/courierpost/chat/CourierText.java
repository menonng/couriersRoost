package com.courierpost.chat;

import com.courierpost.bird.CourierBird;
import com.courierpost.bird.MessageKind;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;

public final class CourierText {
    private CourierText() {
    }

    public static Component incoming(CourierBird bird) {
        Component sender = Component.literal(bird.senderName);
        Component body = Component.literal(bird.content);
        return switch (bird.kind) {
            case GLOBAL -> Component.translatable("chat.type.text", sender, body);
            case WHISPER -> Component.translatable("commands.message.display.incoming", sender, body)
                    .withStyle(ChatFormatting.GRAY, ChatFormatting.ITALIC);
            case TEAM -> Component.translatable("chat.type.team.text", Component.literal(bird.teamName), sender, body);
        };
    }

    public static Component outgoing(MessageKind kind, String senderName, String targetName, String teamName, String content) {
        Component body = Component.literal(content);
        return switch (kind) {
            case GLOBAL -> Component.translatable("chat.type.text", Component.literal(senderName), body);
            case WHISPER -> Component.translatable("commands.message.display.outgoing", Component.literal(targetName), body)
                    .withStyle(ChatFormatting.GRAY, ChatFormatting.ITALIC);
            case TEAM -> Component.translatable("chat.type.team.sent", Component.literal(teamName), Component.literal(senderName), body);
        };
    }

    public static Component dispatched(ServerPlayer player) {
        return Localization.text(player, "courierpost.feedback.dispatched").withStyle(ChatFormatting.GRAY);
    }

    public static Component died(ServerPlayer player) {
        return Localization.text(player, "courierpost.feedback.bird_died").withStyle(ChatFormatting.RED);
    }

    public static Component cooldown(ServerPlayer player, int seconds) {
        return Localization.text(player, "courierpost.cooldown", seconds).withStyle(ChatFormatting.RED);
    }
}
