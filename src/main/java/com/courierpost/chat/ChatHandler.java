package com.courierpost.chat;

import com.courierpost.CourierPost;
import com.courierpost.advancement.AdvancementLocalizer;
import com.courierpost.bird.BirdManager;
import com.courierpost.bird.MessageKind;
import net.minecraft.advancements.AdvancementHolder;
import net.minecraft.commands.Commands;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.PlayerAdvancements;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.level.gamerules.GameRules;
import net.minecraft.world.scores.PlayerTeam;

import java.util.Collection;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

public final class ChatHandler {
    private static final Map<UUID, Long> COOLDOWNS = new HashMap<>();

    private ChatHandler() {
    }

    public static void clearCooldown(UUID playerId) {
        COOLDOWNS.remove(playerId);
    }

    public static boolean handleGlobal(ServerPlayer sender, String content) {
        int cooldown = CourierPost.config().globalChatCooldown;
        if (cooldown > 0 && !Commands.LEVEL_GAMEMASTERS.check(sender.permissions())) {
            long now = System.currentTimeMillis();
            long last = COOLDOWNS.getOrDefault(sender.getUUID(), 0L);
            long remaining = cooldown * 1000L - (now - last);
            if (remaining > 0L) {
                sender.sendSystemMessage(CourierText.cooldown(sender, (int) Math.ceil(remaining / 1000.0D)));
                return false;
            }
            COOLDOWNS.put(sender.getUUID(), now);
        }

        BirdManager manager = CourierPost.manager();
        MinecraftServer server = sender.level().getServer();
        sender.sendSystemMessage(CourierText.outgoing(MessageKind.GLOBAL, sender.getGameProfile().name(), null, null, content));
        CourierPost.LOGGER.info("<{}> {}", sender.getGameProfile().name(), content);

        for (ServerPlayer recipient : server.getPlayerList().getPlayers()) {
            if (recipient.getUUID().equals(sender.getUUID())) {
                continue;
            }
            manager.dispatch(sender, recipient, MessageKind.GLOBAL, content, null);
        }
        sender.sendSystemMessage(CourierText.dispatched(sender));
        grantAdvancement(sender);
        return true;
    }

    public static void handleWhisper(ServerPlayer sender, Collection<ServerPlayer> targets, String content) {
        BirdManager manager = CourierPost.manager();
        for (ServerPlayer target : targets) {
            sender.sendSystemMessage(CourierText.outgoing(MessageKind.WHISPER, sender.getGameProfile().name(),
                    target.getGameProfile().name(), null, content));
            if (!target.getUUID().equals(sender.getUUID())) {
                manager.dispatch(sender, target, MessageKind.WHISPER, content, null);
            }
        }
        sender.sendSystemMessage(CourierText.dispatched(sender));
        grantAdvancement(sender);
    }

    public static void handleTeam(ServerPlayer sender, PlayerTeam team, Collection<ServerPlayer> targets, String content) {
        String teamName = team.getDisplayName().getString();
        BirdManager manager = CourierPost.manager();
        sender.sendSystemMessage(CourierText.outgoing(MessageKind.TEAM, sender.getGameProfile().name(), null, teamName, content));

        for (ServerPlayer recipient : targets) {
            if (recipient.getUUID().equals(sender.getUUID())) {
                continue;
            }
            manager.dispatch(sender, recipient, MessageKind.TEAM, content, teamName);
        }
        sender.sendSystemMessage(CourierText.dispatched(sender));
        grantAdvancement(sender);
    }

    private static void grantAdvancement(ServerPlayer player) {
        MinecraftServer server = player.level().getServer();
        if (server == null) {
            return;
        }
        AdvancementHolder holder = server.getAdvancements().get(AdvancementLocalizer.ID);
        if (holder == null) {
            return;
        }
        PlayerAdvancements advancements = player.getAdvancements();
        if (advancements.getOrStartProgress(holder).isDone()) {
            return;
        }
        if (!advancements.award(holder, "sent")) {
            return;
        }
        if (!server.getGameRules().get(GameRules.SHOW_ADVANCEMENT_MESSAGES)) {
            return;
        }
        for (ServerPlayer viewer : server.getPlayerList().getPlayers()) {
            viewer.sendSystemMessage(AdvancementLocalizer.announcement(viewer, player));
        }
    }
}
