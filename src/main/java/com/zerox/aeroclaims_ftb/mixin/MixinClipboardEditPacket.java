package com.zerox.aeroclaims_ftb.mixin;

import com.zerox.aeroclaims_ftb.protect.CreateProtectionHelper;
import com.simibubi.create.content.equipment.clipboard.ClipboardEditPacket;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerPlayer;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;


@Mixin(ClipboardEditPacket.class)
public class MixinClipboardEditPacket {

    @Shadow
    private BlockPos targetedBlock;

    @Inject(method = "handle", remap = false, at = @At("HEAD"), cancellable = true)
    public void aeroclaims_ftb$onHandle(ServerPlayer player, CallbackInfo ci) {
        if (player == null) return;
        if (!CreateProtectionHelper.isBlockAccessAllowed(targetedBlock, player))
            ci.cancel();
    }
}
