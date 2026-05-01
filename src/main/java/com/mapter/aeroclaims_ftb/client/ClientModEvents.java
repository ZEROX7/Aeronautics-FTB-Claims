package com.mapter.aeroclaims_ftb.client;

import com.mapter.aeroclaims_ftb.aeroclaims_ftb;
import com.mapter.aeroclaims_ftb.registry.ModMenus;
import com.mapter.aeroclaims_ftb.screen.ClaimSettingsScreen;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.RegisterMenuScreensEvent;

@EventBusSubscriber(modid = aeroclaims_ftb.MODID, bus = EventBusSubscriber.Bus.MOD, value = Dist.CLIENT)
public class ClientModEvents {

    @SubscribeEvent
    public static void onRegisterScreens(RegisterMenuScreensEvent event) {
        event.register(ModMenus.CLAIM_SETTINGS_MENU.get(), ClaimSettingsScreen::new);
    }
}
