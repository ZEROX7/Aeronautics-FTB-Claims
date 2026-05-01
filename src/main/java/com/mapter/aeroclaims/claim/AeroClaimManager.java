package com.mapter.aeroclaims.claim;

import com.mapter.aeroclaims.config.AeroClaimsConfig;
import dev.ftb.mods.ftbchunks.api.ChunkTeamData;
import dev.ftb.mods.ftbchunks.api.ClaimResult;
import dev.ftb.mods.ftbchunks.api.ClaimedChunk;
import dev.ftb.mods.ftbchunks.api.FTBChunksAPI;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.UUID;

public class AeroClaimManager {

    private static final Logger LOGGER = LoggerFactory.getLogger(AeroClaimManager.class);

    public enum TransferResult {
        SUCCESS,
        FTB_NOT_LOADED,
        NOT_ENOUGH_FREE,
        API_ERROR
    }

    public static TransferResult transferFromFtb(ServerPlayer player, int amount) {
        if (amount <= 0) return TransferResult.API_ERROR;

        try {
            var chunksApi = FTBChunksAPI.api();
            if (!chunksApi.isManagerLoaded()) return TransferResult.FTB_NOT_LOADED;

            var manager = chunksApi.getManager();
            ChunkTeamData teamData = manager.getOrCreateData(player);
            if (teamData == null) return TransferResult.API_ERROR;

            var claimedChunks = new ArrayList<>(teamData.getClaimedChunks());

            if (claimedChunks.size() < amount) {
                return TransferResult.NOT_ENOUGH_FREE;
            }

            int transferred = 0;

            for (ClaimedChunk chunk : claimedChunks) {
                if (transferred >= amount) break;

                ClaimResult result = teamData.unclaim(
                        player.createCommandSourceStack(),
                        chunk.getPos(),
                        false
                );

                if (!result.isSuccess()) {
                    LOGGER.warn("[AeroClaims] Failed to unclaim FTB chunk {} for {}", chunk.getPos(), player.getGameProfile().getName());
                    continue;
                }

                transferred++;
            }

            if (transferred < amount) {
                return TransferResult.API_ERROR;
            }

            AeroClaimSavedData.get(player.serverLevel())
                    .addMigratedSlots(player.getUUID(), amount);

            return TransferResult.SUCCESS;
        } catch (Exception e) {
            LOGGER.error("[AeroClaims] transferFromFtb failed", e);
            return TransferResult.API_ERROR;
        }
    }

    public static TransferResult transferToFtb(ServerPlayer player, int amount) {
        if (amount <= 0) return TransferResult.API_ERROR;

        try {
            var chunksApi = FTBChunksAPI.api();
            if (!chunksApi.isManagerLoaded()) return TransferResult.FTB_NOT_LOADED;

            AeroClaimSavedData data = AeroClaimSavedData.get(player.serverLevel());
            UUID playerId = player.getUUID();

            if (data.getFreeSlots(playerId) < amount) {
                return TransferResult.NOT_ENOUGH_FREE;
            }

            int previousMigrated = data.getMigratedSlots(playerId);
            data.setMigratedSlots(playerId, previousMigrated - amount);

            ChunkTeamData teamData = chunksApi.getManager().getOrCreateData(player);
            if (teamData == null) {
                data.setMigratedSlots(playerId, previousMigrated);
                return TransferResult.API_ERROR;
            }

            teamData.setExtraClaimChunks(teamData.getExtraClaimChunks() + amount);

            return TransferResult.SUCCESS;
        } catch (Exception e) {
            LOGGER.error("[AeroClaims] transferToFtb failed", e);
            return TransferResult.API_ERROR;
        }
    }

    public static int getBlockLimit(ServerLevel level, BlockPos pos) {
        return AeroClaimSavedData.get(level).getClaimsForBlock(pos)
                * AeroClaimsConfig.BLOCKS_PER_CLAIM.get();
    }

    public static boolean adjustClaimsForBlock(ServerLevel level, UUID owner, BlockPos pos, int delta) {
        AeroClaimSavedData data = AeroClaimSavedData.get(level);
        int newCount = data.getClaimsForBlock(pos) + delta;

        if (newCount < 0) return false;
        if (delta > 0 && data.getFreeSlots(owner) < delta) return false;

        data.setClaimsForBlock(pos, owner, newCount);
        return true;
    }

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

    public static int getFreeFtbClaims(ServerPlayer player) {
        try {
            var chunksApi = FTBChunksAPI.api();
            if (!chunksApi.isManagerLoaded()) return -1;

            ChunkTeamData teamData = chunksApi.getManager().getOrCreateData(player);
            if (teamData == null) return -1;

            return Math.max(
                    0,
                    teamData.getMaxClaimChunks() - teamData.getClaimedChunks().size()
            );
        } catch (Exception e) {
            LOGGER.error("[AeroClaims] getFreeFtbClaims failed", e);
            return -1;
        }
    }
}
