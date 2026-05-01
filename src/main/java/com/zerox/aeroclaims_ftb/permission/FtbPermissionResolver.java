package com.zerox.aeroclaims_ftb.permission;

import com.zerox.aeroclaims_ftb.claim.Claim;
import dev.ftb.mods.ftbchunks.api.FTBChunksAPI;
import dev.ftb.mods.ftbteams.api.FTBTeamsAPI;
import dev.ftb.mods.ftbteams.api.TeamManager;
import dev.ftb.mods.ftbteams.api.TeamRank;
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

        if (teams.arePlayersInSameTeam(playerUuid, ownerUuid)) {
            return claim.isAllowParty();
        }

        boolean ally = teams.getTeamForPlayerID(ownerUuid)
            .map(ownerTeam -> ownerTeam.getRankForPlayer(playerUuid) == TeamRank.ALLY)
            .orElse(false);

        if (ally) {
            return claim.isAllowAllies(); // or whatever your Claim field is called
        }

        return false;
    }
}
