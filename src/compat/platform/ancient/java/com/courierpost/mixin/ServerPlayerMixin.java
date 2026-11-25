package com.courierpost.mixin;

import com.courierpost.compat.LanguageHolder;
import net.minecraft.network.protocol.game.ServerboundClientInformationPacket;
import net.minecraft.server.level.ServerPlayer;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(ServerPlayer.class)
public abstract class ServerPlayerMixin implements LanguageHolder {
    @Unique
    private String courierpost$language = "en_us";

    @Inject(method = "updateOptions", at = @At("HEAD"))
    private void courierpost$captureLanguage(ServerboundClientInformationPacket packet, CallbackInfo info) {
        courierpost$language = packet.language();
    }

    @Override
    public String courierpost$language() {
        return courierpost$language;
    }
}
