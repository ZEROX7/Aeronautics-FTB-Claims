package com.zerox.aeroclaims_ftb.network;

import com.zerox.aeroclaims_ftb.aeroclaims_ftb;
import com.zerox.aeroclaims_ftb.claim.AeroClaimManager;
import com.zerox.aeroclaims_ftb.claim.aeroclaims_ftbavedData;
import com.zerox.aeroclaims_ftb.claim.Claim;
import com.zerox.aeroclaims_ftb.claim.ClaimManager;
import com.zerox.aeroclaims_ftb.config.aeroclaims_ftbConfig;
import net.minecraft.core.BlockPos;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.chat.Component;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.neoforged.neoforge.network.PacketDistributor;
import net.neoforged.neoforge.network.handling.IPayloadContext;

public record AdjustBlockClaimsPacket(BlockPos center, int delta) implements CustomPacketPayload {

    public static final Type<AdjustBlockClaimsPacket> TYPE =
            new Type<>(ResourceLocation.fromNamespaceAndPath(aeroclaims_ftb.MODID, "adjust_block_claims"));

    public static final StreamCodec<RegistryFriendlyByteBuf, AdjustBlockClaimsPacket> STREAM_CODEC =
            StreamCodec.composite(
                    BlockPos.STREAM_CODEC, AdjustBlockClaimsPacket::center,
                    ByteBufCodecs.INT,      AdjustBlockClaimsPacket::delta,
                    AdjustBlockClaimsPacket::new
            );

    @Override
    public Type<AdjustBlockClaimsPacket> type() { return TYPE; }

    public static void handle(AdjustBlockClaimsPacket msg, IPayloadContext context) {
        context.enqueueWork(() -> {
            if (!(context.player() instanceof ServerPlayer player)) return;

            ServerLevel level = player.serverLevel();
            Claim claim = ClaimManager.getClaimByCenter(level, msg.center);
            if (claim == null) return;

            if (!player.getUUID().equals(claim.getOwner())) {
                player.sendSystemMessage(Component.translatable("message.aeroclaims_ftb.only_owner_can_configure"));
                return;
            }

            AeroClaimSavedData data = AeroClaimSavedData.get(level);

            if (msg.delta < 0 && claim.isActive()) {
                int newLimit = (data.getClaimsForBlock(msg.center) + msg.delta)
                        * AeroClaimConfig.BLOCKS_PER_CLAIM.get();
                if (newLimit >= 0 && ClaimManager.countShipBlocks(level, msg.center, newLimit + 1) > newLimit) {
                    player.sendSystemMessage(Component.translatable("message.aeroclaims_ftb.cannot_reduce_claims_ship_too_large"));
                    sync(player, msg.center, claim, data);
                    return;
                }
            }

            if (!AeroClaimManager.adjustClaimsForBlock(level, player.getUUID(), msg.center, msg.delta)) {
                player.sendSystemMessage(Component.translatable("message.aeroclaims_ftb.no_ship_slots"));
            }

            sync(player, msg.center, claim, data);
        });
    }

    private static void sync(ServerPlayer player, BlockPos center, Claim claim, AeroClaimSavedData data) {
        PacketDistributor.sendToPlayer(player, new SyncClaimStatePacket(
                center,
                claim.isActive(),
                claim.isAllowParty(),
                claim.isAllowAllies(),
                claim.isAllowOthers(),
                data.getClaimsForBlock(center),
                data.getFreeSlots(player.getUUID()),
                AeroClaimConfig.BLOCKS_PER_CLAIM.get(),
                SyncClaimStatePacket.SHIP_BLOCK_COUNT_UNKNOWN
        ));
    }
}
