package com.mapter.aeroclaims.permission;

import dev.ftb.mods.ftbteams.api.FTBTeamsAPI;
import dev.ftb.mods.ftbteams.api.Team;
import dev.ftb.mods.ftbteams.api.TeamManager;

import java.util.Optional;
import java.util.UUID;

public class FtbPermissionResolver implements ClaimPermissionResolver {

    @Override
    public boolean isSameParty(UUID playerUuid, UUID ownerUuid) {
        if (!FTBTeamsAPI.api().isManagerLoaded()) return false;

        TeamManager teams = FTBTeamsAPI.api().getManager();

        Optional<Team> playerTeam = teams.getTeamForPlayerID(playerUuid);
        Optional<Team> ownerTeam = teams.getTeamForPlayerID(ownerUuid);

        return playerTeam.isPresent()
                && ownerTeam.isPresent()
                && playerTeam.get().getId().equals(ownerTeam.get().getId());
    }

    @Override
    public boolean isAlly(UUID playerUuid, UUID ownerUuid) {
        if (!FTBTeamsAPI.api().isManagerLoaded()) return false;

        TeamManager teams = FTBTeamsAPI.api().getManager();

        Optional<Team> playerTeam = teams.getTeamForPlayerID(playerUuid);
        Optional<Team> ownerTeam = teams.getTeamForPlayerID(ownerUuid);

        if (playerTeam.isEmpty() || ownerTeam.isEmpty()) return false;

        Team p = playerTeam.get();
        Team o = ownerTeam.get();

        // FIX: correct ally detection
        return p.isMember(ownerUuid) // same team shortcut
                || p.getAllyTeams().contains(o.getId());
    }
}
