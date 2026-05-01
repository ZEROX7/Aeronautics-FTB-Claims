package com.mapter.aeroclaims.permission;

import com.mapter.aeroclaims.claim.Claim;
import dev.ftb.mods.ftbchunks.api.FTBChunksAPI;
import dev.ftb.mods.ftbteams.api.FTBTeamsAPI;
import dev.ftb.mods.ftbteams.api.TeamManager;
import net.minecraft.server.level.ServerPlayer;

import java.util.UUID;

public class FtbPermissionResolver implements ClaimPermissionResolver {

    @Override
    public boolean canAccess(ServerPlayer player, Claim claim) {
        UUID playerUuid = player.getUUID();
        UUID ownerUuid = claim.getOwner();

        if (playerUuid.equals(ownerUuid)) return true;
        if (claim.isAllowOthers()) return true;

        try {
            var chunks = FTBChunksAPI.api();
            if (chunks.isManagerLoaded() && chunks.getManager().getBypassProtection(playerUuid)) {
                return true;
            }
        } catch (Exception ignored) {
        }

        if (!FTBTeamsAPI.api().isManagerLoaded()) return false;

        TeamManager teams = FTBTeamsAPI.api().getManager();

        boolean sameTeam = teams.arePlayersInSameTeam(playerUuid, ownerUuid);

        if (sameTeam) {
            return claim.isAllowParty();
        }

        return false;
    }
}
