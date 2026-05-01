package com.zerox.aeroclaims_ftb.network;

import com.zerox.aeroclaims_ftb.aeroclaims_ftb;
import com.zerox.aeroclaims_ftb.claim.AeroClaimManager;
import com.zerox.aeroclaims_ftb.claim.aeroclaims_ftbavedData;
import com.zerox.aeroclaims_ftb.claim.Claim;
import com.zerox.aeroclaims_ftb.claim.ClaimManager;
import com.zerox.aeroclaims_ftb.claim.ClaimSavedData;
import com.zerox.aeroclaims_ftb.config.aeroclaims_ftbConfig;
import com.zerox.aeroclaims_ftb.sublevel.SableShipUtils;
import dev.ryanhcode.sable.sublevel.SubLevel;
import net.minecraft.core.BlockPos;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.chat.Component;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.neoforged.neoforge.network.PacketDistributor;
import net.neoforged.neoforge.network.handling.IPayloadContext;

public record RefreshClaimPacket(BlockPos center) implements CustomPacketPayload {

    public static final Type<RefreshClaimPacket> TYPE =
            new Type<>(ResourceLocation.fromNamespaceAndPath(aeroclaims_ftb.MODID, "refresh_claim"));

    public static final StreamCodec<RegistryFriendlyByteBuf, RefreshClaimPacket> STREAM_CODEC =
            StreamCodec.composite(
                    BlockPos.STREAM_CODEC, RefreshClaimPacket::center,
                    RefreshClaimPacket::new
            );

    @Override
    public Type<RefreshClaimPacket> type() { return TYPE; }

    public static void handle(RefreshClaimPacket msg, IPayloadContext context) {
        context.enqueueWork(() -> {
            if (!(context.player() instanceof ServerPlayer player)) return;

            ServerLevel level = player.serverLevel();
            Claim claim = ClaimManager.getClaimByCenter(level, msg.center);
            if (claim == null || !player.getUUID().equals(claim.getOwner())) return;

            if (!SableShipUtils.isOnShip(level, msg.center)) {
                player.sendSystemMessage(Component.translatable("message.aeroclaims_ftb.not_on_subclaim"));
                return;
            }

            if (hasDuplicateClaimBlock(level, msg.center)) {
                player.sendSystemMessage(Component.translatable("message.aeroclaims_ftb.duplicate_claim_block"));
                return;
            }

            int maxSize = AeroClaimManager.getBlockLimit(level, msg.center);
            boolean hasClaims = maxSize > 0;

            boolean deactivateOnOverflow = aeroclaims_ftbConfig.DEACTIVATE_ON_OVERFLOW.get();
            int blockCount;

            if (hasClaims) {
                blockCount = ClaimManager.recountShipBlocks(level, msg.center, deactivateOnOverflow);
            } else {
                blockCount = ClaimManager.countShipBlocksExact(level, msg.center);
                player.sendSystemMessage(Component.translatable("message.aeroclaims_ftb.no_claims_allocated"));
            }

            if (blockCount < 0) {
                player.sendSystemMessage(Component.translatable("message.aeroclaims_ftb.refresh_failed"));
                sync(player, msg.center, claim, level, SyncClaimStatePacket.SHIP_BLOCK_COUNT_UNKNOWN);
                return;
            }

            if (hasClaims && blockCount > maxSize) {
                String msgKey = deactivateOnOverflow
                        ? "message.aeroclaims_ftb.ship_too_large_deactivated"
                        : "message.aeroclaims_ftb.ship_too_large";
                player.sendSystemMessage(Component.translatable(msgKey, blockCount, maxSize));
            } else if (hasClaims) {
                player.sendSystemMessage(Component.translatable("message.aeroclaims_ftb.claim_recounted"));
            }

            cacheShipStructure(level, msg.center, blockCount);

            Claim updated = ClaimManager.getClaimByCenter(level, msg.center);
            sync(player, msg.center, updated != null ? updated : claim, level, blockCount);
        });
    }

    private static boolean hasDuplicateClaimBlock(ServerLevel level, BlockPos center) {
        SubLevel ship = SableShipUtils.getShipAt(level, center);
        String shipId = SableShipUtils.getShipId(ship);
        if (shipId == null) return false;

        for (Claim other : ClaimSavedData.get(level).getClaims()) {
            if (other.getCenter().equals(center)) continue;
            String otherId = SableShipUtils.getShipId(SableShipUtils.getShipAt(level, other.getCenter()));
            if (shipId.equals(otherId)) return true;
        }
        return false;
    }

    private static void cacheShipStructure(ServerLevel level, BlockPos center, int blockCount) {
        SubLevel ship = SableShipUtils.getShipAt(level, center);
        String shipId = SableShipUtils.getShipId(ship);

        AeroClaimSavedData data = AeroClaimSavedData.get(level);
        data.cacheShipBlockCount(center, blockCount);
        if (shipId != null) {
            data.cacheShipId(center, shipId);
        }
    }

    private static void sync(ServerPlayer player, BlockPos center, Claim claim,
                              ServerLevel level, int shipBlockCount) {
        AeroClaimSavedData data = AeroClaimSavedData.get(level);
        PacketDistributor.sendToPlayer(player, new SyncClaimStatePacket(
                center,
                claim.isActive(),
                claim.isAllowParty(),
                claim.isAllowAllies(),
                claim.isAllowOthers(),
                data.getClaimsForBlock(center),
                data.getFreeSlots(player.getUUID()),
                AeroClaimConfig.BLOCKS_PER_CLAIM.get(),
                shipBlockCount
        ));
    }
}
