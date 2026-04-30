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

        var chunks = FTBChunksAPI.api();
        if (!chunks.isManagerLoaded()) return false;

        if (chunks.getManager().getBypassProtection(playerUuid)) return true;

        if (!FTBTeamsAPI.api().isManagerLoaded()) return false;  
        TeamManager teams = FTBTeamsAPI.api().getManager();

        Optional<Team> playerTeam = teams.getTeamForPlayerID(playerUuid);
        Optional<Team> ownerTeam = teams.getTeamForPlayerID(ownerUuid);

        if (playerTeam.isEmpty() || ownerTeam.isEmpty()) return false;

        Team p = playerTeam.get();
        Team o = ownerTeam.get();

        if (claim.isAllowParty() && p.getId().equals(o.getId())) return true;

        // FTB Teams API has same-team built in; “allies” are not a direct OpenPAC concept.
        // Treat allies as same team unless you add a custom FTB Teams property/rank relation.
        return claim.isAllowAllies() && teams.arePlayersInSameTeam(playerUuid, ownerUuid);
    }
}
