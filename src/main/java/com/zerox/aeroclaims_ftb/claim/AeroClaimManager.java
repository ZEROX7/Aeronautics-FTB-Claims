package com.mapter.aeroclaims_ftb.claim;

import com.mapter.aeroclaims_ftb.config.aeroclaims_ftbConfig;
import dev.ftb.mods.ftbchunks.api.ChunkTeamData;
import dev.ftb.mods.ftbchunks.api.FTBChunksAPI;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

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

            ChunkTeamData teamData = chunksApi.getManager().getOrCreateData(player);
            if (teamData == null) return TransferResult.API_ERROR;

            UUID ownerId = player.getUUID();

            int freeFtbClaims = Math.max(
                    0,
                    teamData.getMaxClaimChunks() - teamData.getClaimedChunks().size()
            );

            if (freeFtbClaims < amount) {
                return TransferResult.NOT_ENOUGH_FREE;
            }

            aeroclaims_ftbavedData.get(player.serverLevel()).addMigratedSlots(ownerId, amount);

            return TransferResult.SUCCESS;
        } catch (Exception e) {
            LOGGER.error("[aeroclaims_ftb] transferFromFtb failed", e);
            return TransferResult.API_ERROR;
        }
    }

    public static TransferResult transferToFtb(ServerPlayer player, int amount) {
        if (amount <= 0) return TransferResult.API_ERROR;

        try {
            var chunksApi = FTBChunksAPI.api();
            if (!chunksApi.isManagerLoaded()) return TransferResult.FTB_NOT_LOADED;

            ChunkTeamData teamData = chunksApi.getManager().getOrCreateData(player);
            if (teamData == null) return TransferResult.API_ERROR;

            UUID ownerId = player.getUUID();

            aeroclaims_ftbavedData data = aeroclaims_ftbavedData.get(player.serverLevel());

            if (data.getFreeSlots(ownerId) < amount) {
                return TransferResult.NOT_ENOUGH_FREE;
            }

            int previousMigrated = data.getMigratedSlots(ownerId);
            data.setMigratedSlots(ownerId, previousMigrated - amount);

            return TransferResult.SUCCESS;
        } catch (Exception e) {
            LOGGER.error("[aeroclaims_ftb] transferToFtb failed", e);
            return TransferResult.API_ERROR;
        }
    }

    public static int getBlockLimit(ServerLevel level, BlockPos pos) {
        return aeroclaims_ftbavedData.get(level).getClaimsForBlock(pos)
                * aeroclaims_ftbConfig.BLOCKS_PER_CLAIM.get();
    }

    public static boolean adjustClaimsForBlock(ServerLevel level, UUID owner, BlockPos pos, int delta) {
        aeroclaims_ftbavedData data = aeroclaims_ftbavedData.get(level);
        int newCount = data.getClaimsForBlock(pos) + delta;

        if (newCount < 0) return false;
        if (delta > 0 && data.getFreeSlots(owner) < delta) return false;

        data.setClaimsForBlock(pos, owner, newCount);
        return true;
    }

    public static void releaseAllClaimsForBlock(ServerLevel level, UUID owner, BlockPos pos) {
        aeroclaims_ftbavedData.get(level).removeClaimsForBlock(pos, owner);
    }

    public static int getMigratedSlots(ServerLevel level, UUID playerId) {
        return aeroclaims_ftbavedData.get(level).getMigratedSlots(playerId);
    }

    public static int getUsedSlots(ServerLevel level, UUID playerId) {
        return aeroclaims_ftbavedData.get(level).getUsedSlots(playerId);
    }

    public static int getFreeSlots(ServerLevel level, UUID playerId) {
        return aeroclaims_ftbavedData.get(level).getFreeSlots(playerId);
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
            LOGGER.error("[aeroclaims_ftb] getFreeFtbClaims failed", e);
            return -1;
        }
    }
}
