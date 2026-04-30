package com.mapter.aeroclaims.claim;

import com.mapter.aeroclaims.config.AeroClaimsConfig;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.UUID;

// Manages AeroClaims claim slots.
// FTB version: no OPAC slot transfer. Aero slots are stored locally only.
public class AeroClaimManager {

    private static final Logger LOGGER = LoggerFactory.getLogger(AeroClaimManager.class);

    public enum TransferResult {
        SUCCESS,
        OPAC_NOT_LOADED,
        NOT_ENOUGH_FREE,
        API_ERROR
    }

    // OPAC no longer exists in this backend.
    public static TransferResult transferFromOpac(ServerPlayer player, int amount) {
        return TransferResult.OPAC_NOT_LOADED;
    }

    // OPAC no longer exists in this backend.
    public static TransferResult transferToOpac(ServerPlayer player, int amount) {
        return TransferResult.OPAC_NOT_LOADED;
    }

    // Returns maximum allowed ship blocks for this claim block.
    public static int getBlockLimit(ServerLevel level, BlockPos pos) {
        return AeroClaimSavedData.get(level).getClaimsForBlock(pos)
                * AeroClaimsConfig.BLOCKS_PER_CLAIM.get();
    }

    // Changes allocated slots for claim block by delta.
    // Returns false if no free slots or result would go negative.
    public static boolean adjustClaimsForBlock(ServerLevel level, UUID owner, BlockPos pos, int delta) {
        AeroClaimSavedData data = AeroClaimSavedData.get(level);
        int newCount = data.getClaimsForBlock(pos) + delta;

        if (newCount < 0) return false;
        if (delta > 0 && data.getFreeSlots(owner) < delta) return false;

        data.setClaimsForBlock(pos, owner, newCount);
        return true;
    }

    // Releases all slots occupied by this claim block.
    public static void releaseAllClaimsForBlock(ServerLevel level, UUID owner, BlockPos pos) {
        AeroClaimSavedData.get(level).removeClaimsForBlock(pos, owner);
    }

    public static int getMigratedSlots(ServerLevel level, UUID playerId) {
        return AeroClaimSavedData.get(level).getMigratedSlots(playerId);
    }

    public static int getUsedSlots(ServerLevel level, UUID playerId) {
        return AeroClaimSavedData.get(level).getUsedSlots(playerId);
    }

    public static int getFreeSlots(ServerLevel level, UUID playerId) {
        return AeroClaimSavedData.get(level).getFreeSlots(playerId);
    }

    // Old OPAC compatibility method.
    // Returns -1 because OPAC is no longer used.
    public static int getFreeOpacClaims(ServerPlayer player) {
        return -1;
    }
}
