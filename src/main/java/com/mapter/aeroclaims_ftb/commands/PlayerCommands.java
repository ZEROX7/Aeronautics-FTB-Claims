package com.mapter.aeroclaims_ftb.commands;

import com.mapter.aeroclaims_ftb.claim.AeroClaimManager;
import com.mapter.aeroclaims_ftb.sublevel.RegisteredSublevelManager;
import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.arguments.IntegerArgumentType;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.network.chat.ClickEvent;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.HoverEvent;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.server.level.ServerPlayer;

import java.util.Map;
import java.util.UUID;

public class PlayerCommands {

    public static void register(CommandDispatcher<CommandSourceStack> dispatcher) {
        dispatcher.register(
            Commands.literal("aeroclaims_ftb")
                .then(Commands.literal("info")
                    .executes(ctx -> executeInfo(ctx.getSource(), null, null)))

                .then(Commands.literal("transfer")
                    .then(Commands.literal("to")
                        .then(Commands.literal("ftb")
                            .then(Commands.argument("amount", IntegerArgumentType.integer(1))
                                .executes(ctx -> executeTransferToFtb(
                                        ctx.getSource(),
                                        IntegerArgumentType.getInteger(ctx, "amount")
                                ))))
                        .then(Commands.literal("aero")
                            .then(Commands.argument("amount", IntegerArgumentType.integer(1))
                                .executes(ctx -> executeTransferFromFtb(
                                        ctx.getSource(),
                                        IntegerArgumentType.getInteger(ctx, "amount")
                                ))))))
        );
    }

    static int executeInfo(CommandSourceStack source, UUID targetUuid, String targetName) {

        if (targetUuid == null) {
            ServerPlayer player = CommandUtils.requirePlayer(source);
            if (player == null) return 0;
            targetUuid  = player.getUUID();
            targetName  = player.getGameProfile().getName();
        }

        Map<String, String> ships = RegisteredSublevelManager.getRegisteredShips(targetUuid);
        int migratedSlots = AeroClaimManager.getMigratedSlots(source.getLevel(), targetUuid);
        int usedSlots     = AeroClaimManager.getUsedSlots(source.getLevel(), targetUuid);

        final UUID   finalUuid = targetUuid;
        final String finalName = targetName;

        source.sendSuccess(() -> Component.translatable("commands.aeroclaims_ftb.info.header", finalName), false);
        source.sendSuccess(() -> Component.translatable("commands.aeroclaims_ftb.info.ship_slots", usedSlots, migratedSlots), false);

        if (source.getEntity() instanceof ServerPlayer caller && caller.getUUID().equals(finalUuid)) {
            int freeFtb = AeroClaimManager.getFreeFtbClaims(caller);
            if (freeFtb >= 0) {
                source.sendSuccess(() -> Component.translatable("commands.aeroclaims_ftb.claim_info.ftb_free", freeFtb), false);
            } else {
                source.sendSuccess(() -> Component.translatable("commands.aeroclaims_ftb.claim_info.ftb_unavailable"), false);
            }
        }

        source.sendSuccess(() -> Component.translatable("commands.aeroclaims_ftb.info.registered_count", ships.size()), false);

        if (ships.isEmpty()) {
            source.sendSuccess(() -> Component.translatable("commands.aeroclaims_ftb.info.empty"), false);
        } else {
            for (Map.Entry<String, String> entry : ships.entrySet()) {
                String shipId   = entry.getKey();
                String shipName = entry.getValue();
                source.sendSuccess(() -> buildShipEntry(shipName, shipId), false);
            }
        }

        return 1;
    }

    private static MutableComponent buildShipEntry(String shipName, String shipId) {
        return Component.translatable("commands.aeroclaims_ftb.info.entry", shipName)
                .withStyle(style -> style
                        .withHoverEvent(new HoverEvent(
                                HoverEvent.Action.SHOW_TEXT,
                                Component.translatable("commands.aeroclaims_ftb.info.entry.hover", shipId)
                        ))
                        .withClickEvent(new ClickEvent(
                                ClickEvent.Action.COPY_TO_CLIPBOARD,
                                shipId
                        ))
                );
    }

    private static int executeTransferFromFtb(CommandSourceStack source, int amount) {
        ServerPlayer player = CommandUtils.requirePlayer(source);
        if (player == null) return 0;

        int freeFtb = AeroClaimManager.getFreeFtbClaims(player);
        AeroClaimManager.TransferResult result = AeroClaimManager.transferFromFtb(player, amount);

        switch (result) {
            case SUCCESS -> {
                UUID id         = player.getUUID();
                int newMigrated = AeroClaimManager.getMigratedSlots(player.serverLevel(), id);
                int newUsed     = AeroClaimManager.getUsedSlots(player.serverLevel(), id);
                source.sendSuccess(() -> Component.translatable(
                        "commands.aeroclaims_ftb.transfer.success", amount, newUsed, newMigrated
                ), false);
            }
            case FTB_NOT_LOADED ->
                    source.sendFailure(Component.translatable("commands.aeroclaims_ftb.transfer.ftb_not_loaded"));
            case NOT_ENOUGH_FREE ->
                    source.sendFailure(Component.translatable(
                            "commands.aeroclaims_ftb.transfer.not_enough", freeFtb, amount
                    ));
            case API_ERROR ->
                    source.sendFailure(Component.translatable("commands.aeroclaims_ftb.transfer.error"));
        }

        return CommandUtils.toResult(result);
    }

    private static int executeTransferToFtb(CommandSourceStack source, int amount) {
        ServerPlayer player = CommandUtils.requirePlayer(source);
        if (player == null) return 0;

        int freeShipClaims = AeroClaimManager.getFreeSlots(player.serverLevel(), player.getUUID());
        AeroClaimManager.TransferResult result = AeroClaimManager.transferToFtb(player, amount);

        switch (result) {
            case SUCCESS -> {
                UUID id         = player.getUUID();
                int newMigrated = AeroClaimManager.getMigratedSlots(player.serverLevel(), id);
                int newUsed     = AeroClaimManager.getUsedSlots(player.serverLevel(), id);
                source.sendSuccess(() -> Component.translatable(
                        "commands.aeroclaims_ftb.transfer_back.success", amount, newUsed, newMigrated
                ), false);
            }
            case FTB_NOT_LOADED ->
                    source.sendFailure(Component.translatable("commands.aeroclaims_ftb.transfer.ftb_not_loaded"));
            case NOT_ENOUGH_FREE ->
                    source.sendFailure(Component.translatable(
                            "commands.aeroclaims_ftb.transfer_back.not_enough", freeShipClaims, amount
                    ));
            case API_ERROR ->
                    source.sendFailure(Component.translatable("commands.aeroclaims_ftb.transfer.error"));
        }

        return CommandUtils.toResult(result);
    }
}
