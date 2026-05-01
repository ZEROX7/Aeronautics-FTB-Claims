package com.mapter.aeroclaims.mixin;

import com.mapter.aeroclaims.claim.AeroClaimSavedData;
import dev.ftb.mods.ftbchunks.api.ChunkTeamData;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.neoforged.neoforge.server.ServerLifecycleHooks;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import java.lang.reflect.Method;
import java.util.UUID;

@Mixin(value = ChunkTeamData.class, remap = false)
public abstract class ChunkTeamDataMixin {

    @Unique
    private UUID aeroclaims$getTeamIdReflective() {
        try {
            Method method = this.getClass().getMethod("getTeam");
            Object team = method.invoke(this);
            if (team == null) return null;
    
            Method idMethod = team.getClass().getMethod("getTeamId");
            Object result = idMethod.invoke(team);
    
            return result instanceof UUID uuid ? uuid : null;
        } catch (Exception e) {
            return null;
        }
    }

    @Unique
    private int aeroclaims$getMigratedSlots() {
        UUID teamId = aeroclaims$getTeamIdReflective();
        if (teamId == null) return 0;

        MinecraftServer server = ServerLifecycleHooks.getCurrentServer();
        if (server == null) return 0;

        ServerLevel overworld = server.overworld();
        return AeroClaimSavedData.get(overworld).getMigratedSlots(teamId);
    }

    @Inject(method = "getMaxClaimChunks", at = @At("RETURN"), cancellable = true)
    private void aeroclaims$reduceMaxClaimChunks(CallbackInfoReturnable<Integer> cir) {
        int original = cir.getReturnValue();
        int migrated = aeroclaims$getMigratedSlots();

        cir.setReturnValue(Math.max(0, original - migrated));
    }
}
