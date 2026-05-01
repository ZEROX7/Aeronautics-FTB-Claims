package com.mapter.aeroclaims.permission;

import com.mapter.aeroclaims.claim.Claim;
import dev.ftb.mods.ftbchunks.api.FTBChunksAPI;
import dev.ftb.mods.ftbteams.api.FTBTeamsAPI;
import dev.ftb.mods.ftbteams.api.Team;
import dev.ftb.mods.ftbteams.api.TeamManager;
import net.minecraft.server.level.ServerPlayer;

import java.util.Optional;
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

        Optional<Team> playerTeam = teams.getTeamForPlayerID(playerUuid);
        Optional<Team> ownerTeam = teams.getTeamForPlayerID(ownerUuid);

        if (playerTeam.isEmpty() || ownerTeam.isEmpty()) return false;

        Team p = playerTeam.get();
        Team o = ownerTeam.get();

        boolean sameTeam = p.getId().equals(o.getId());

        if (claim.isAllowParty() && sameTeam) return true;

        // FTB Teams API version you use does not expose ally lookup here,
        // so treat allies as same-team for now.
        return claim.isAllowAllies() && sameTeam;
    }
}
