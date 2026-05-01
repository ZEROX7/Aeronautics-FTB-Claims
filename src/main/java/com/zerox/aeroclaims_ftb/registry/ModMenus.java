package com.zerox.aeroclaims_ftb.registry;

import com.zerox.aeroclaims_ftb.screen.ClaimSettingsMenu;
import com.zerox.aeroclaims_ftb.aeroclaims_ftb;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.world.inventory.MenuType;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.common.extensions.IMenuTypeExtension;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredRegister;

public class ModMenus {

    public static final DeferredRegister<MenuType<?>> MENUS =
            DeferredRegister.create(BuiltInRegistries.MENU, aeroclaims_ftb.MODID);

    public static final DeferredHolder<MenuType<?>, MenuType<ClaimSettingsMenu>> CLAIM_SETTINGS_MENU =
            MENUS.register("claim_settings",
                    () -> IMenuTypeExtension.create(ClaimSettingsMenu::new));

    public static void register(IEventBus bus) {
        MENUS.register(bus);
    }
}