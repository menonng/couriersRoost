package com.courierpost.mixin;

import com.courierpost.command.WhisperCommands;
import com.mojang.brigadier.CommandDispatcher;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.server.commands.MsgCommand;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(MsgCommand.class)
public abstract class MsgCommandMixin {
    @Inject(
            method = "register(Lcom/mojang/brigadier/CommandDispatcher;)V",
            at = @At("HEAD"),
            cancellable = true
    )
    private static void courierpost$replace(CommandDispatcher<CommandSourceStack> dispatcher, CallbackInfo info) {
        info.cancel();
        WhisperCommands.register(dispatcher);
    }
}
