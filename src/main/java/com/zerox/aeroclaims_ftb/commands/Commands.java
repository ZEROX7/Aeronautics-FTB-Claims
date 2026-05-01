package com.zerox.aeroclaims_ftb.commands;

import com.zerox.aeroclaims_ftb.aeroclaims_ftb;
import com.mojang.brigadier.CommandDispatcher;
import net.minecraft.commands.CommandSourceStack;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.RegisterCommandsEvent;

@EventBusSubscriber(modid = aeroclaims_ftb.MODID)
public class Commands {

    @SubscribeEvent
    public static void onRegisterCommands(RegisterCommandsEvent event) {
        CommandDispatcher<CommandSourceStack> dispatcher = event.getDispatcher();
        PlayerCommands.register(dispatcher);
        AdminCommands.register(dispatcher);
    }
}
