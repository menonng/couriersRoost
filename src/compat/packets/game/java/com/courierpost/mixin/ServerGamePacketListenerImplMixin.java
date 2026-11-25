package com.courierpost.mixin;

import com.courierpost.advancement.AdvancementPackets;
import net.minecraft.network.protocol.Packet;
import net.minecraft.network.protocol.game.ClientboundUpdateAdvancementsPacket;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.server.network.ServerGamePacketListenerImpl;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.ModifyVariable;

@Mixin(ServerGamePacketListenerImpl.class)
public abstract class ServerGamePacketListenerImplMixin {
    @Shadow
    public ServerPlayer player;

    @ModifyVariable(method = "send(Lnet/minecraft/network/protocol/Packet;)V", at = @At("HEAD"), argsOnly = true)
    private Packet<?> courierpost$localizeAdvancements(Packet<?> packet) {
        if (!(packet instanceof ClientboundUpdateAdvancementsPacket update) || player == null) {
            return packet;
        }
        return AdvancementPackets.localize(update, player);
    }
}
