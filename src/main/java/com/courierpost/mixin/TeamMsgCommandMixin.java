package com.courierpost.mixin;

import com.courierpost.chat.ChatHandler;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.network.chat.PlayerChatMessage;
import net.minecraft.server.commands.TeamMsgCommand;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.scores.PlayerTeam;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import java.util.List;

@Mixin(TeamMsgCommand.class)
public abstract class TeamMsgCommandMixin {
    @Inject(
            method = "sendMessage(Lnet/minecraft/commands/CommandSourceStack;Lnet/minecraft/world/entity/Entity;Lnet/minecraft/world/scores/PlayerTeam;Ljava/util/List;Lnet/minecraft/network/chat/PlayerChatMessage;)V",
            at = @At("HEAD"),
            cancellable = true
    )
    private static void courierpost$intercept(CommandSourceStack source, Entity self, PlayerTeam team, List<ServerPlayer> targets, PlayerChatMessage message, CallbackInfo info) {
        ServerPlayer sender = source.getPlayer();
        if (sender == null) {
            return;
        }
        info.cancel();
        ChatHandler.handleTeam(sender, team, targets, message.signedContent());
    }
}
