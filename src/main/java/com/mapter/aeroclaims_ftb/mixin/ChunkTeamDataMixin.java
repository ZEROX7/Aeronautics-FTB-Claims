package com.mapter.aeroclaims_ftb.mixin;

import com.mapter.aeroclaims_ftb.claim.aeroclaims_ftbavedData;
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

@Mixin(targets = "dev.ftb.mods.ftbchunks.data.ChunkTeamDataImpl", remap = false)
public abstract class ChunkTeamDataMixin {

    @Unique
    private UUID aeroclaims_ftb$getTeamIdReflective() {
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
    private int aeroclaims_ftb$getMigratedSlots() {
        UUID teamId = aeroclaims_ftb$getTeamIdReflective();
        if (teamId == null) return 0;

        MinecraftServer server = ServerLifecycleHooks.getCurrentServer();
        if (server == null) return 0;

        ServerLevel overworld = server.overworld();
        return aeroclaims_ftbavedData.get(overworld).getMigratedSlots(teamId);
    }

    @Inject(method = "getMaxClaimChunks", at = @At("RETURN"), cancellable = true)
    private void aeroclaims_ftb$reduceMaxClaimChunks(CallbackInfoReturnable<Integer> cir) {
        int original = cir.getReturnValue();
        int migrated = aeroclaims_ftb$getMigratedSlots();

        cir.setReturnValue(Math.max(0, original - migrated));
    }
}
