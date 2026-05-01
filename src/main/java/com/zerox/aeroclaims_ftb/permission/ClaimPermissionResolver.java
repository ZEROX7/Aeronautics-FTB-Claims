package com.zerox.aeroclaims_ftb.permission;

import com.zerox.aeroclaims_ftb.claim.Claim;
import net.minecraft.server.level.ServerPlayer;

public interface ClaimPermissionResolver {
    boolean canAccess(ServerPlayer player, Claim claim);
}