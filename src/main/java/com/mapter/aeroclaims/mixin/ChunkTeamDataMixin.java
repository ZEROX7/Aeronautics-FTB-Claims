package com.mapter.aeroclaims.mixin;

import com.mapter.aeroclaims.claim.AeroClaimSavedData;
import dev.ftb.mods.ftbchunks.api.ChunkTeamData;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.neoforged.neoforge.server.ServerLifecycleHooks;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.ModifyReturnValue;

import java.util.UUID;

@Mixin(value = ChunkTeamData.class, remap = false)
public abstract class ChunkTeamDataMixin {

    @Shadow
    public abstract UUID getTeamId();

    @Unique
    private int aeroclaims$getMigratedSlots() {
        MinecraftServer server = ServerLifecycleHooks.getCurrentServer();
        if (server == null) return 0;

        ServerLevel overworld = server.overworld();
        return AeroClaimSavedData.get(overworld).getMigratedSlots(getTeamId());
    }

    @ModifyReturnValue(method = "getMaxClaimChunks", at = @At("RETURN"))
    private int aeroclaims$reduceMaxClaimChunks(int original) {
        int migrated = aeroclaims$getMigratedSlots();
        return Math.max(0, original - migrated);
    }
}
