package com.zerox.aeroclaims_ftb.mixin;

import com.zerox.aeroclaims_ftb.protect.CreateProtectionHelper;
import com.simibubi.create.content.contraptions.glue.SuperGlueRemovalPacket;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.Entity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;


@Mixin(SuperGlueRemovalPacket.class)
public class MixinSuperGlueRemovalPacket {

    @Shadow
    private int entityId;

    @Inject(method = "handle", remap = false, at = @At("HEAD"), cancellable = true)
    public void aeroclaims_ftb$onHandle(ServerPlayer player, CallbackInfo ci) {
        if (player == null) return;
        Entity entity = player.level().getEntity(entityId);
        if (entity == null) return;
        if (!CreateProtectionHelper.isBlockAccessAllowed(entity.blockPosition(), player))
            ci.cancel();
    }
}
