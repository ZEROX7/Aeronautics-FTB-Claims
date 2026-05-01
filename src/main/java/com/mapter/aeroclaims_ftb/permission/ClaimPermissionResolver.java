package com.mapter.aeroclaims_ftb.permission;

import com.mapter.aeroclaims_ftb.claim.Claim;
import net.minecraft.server.level.ServerPlayer;

public interface ClaimPermissionResolver {
    boolean canAccess(ServerPlayer player, Claim claim);
}