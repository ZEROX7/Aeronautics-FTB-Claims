package com.zerox.aeroclaims_ftb;

import com.zerox.aeroclaims_ftb.claim.ClaimManager;
import com.zerox.aeroclaims_ftb.config.aeroclaims_ftbConfig;
import com.zerox.aeroclaims_ftb.registry.ModBlocks;
import com.zerox.aeroclaims_ftb.registry.ModMenus;
import com.zerox.aeroclaims_ftb.sublevel.SableSubLevelEventHandler;
import net.minecraft.world.item.CreativeModeTabs;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.fml.ModContainer;
import net.neoforged.fml.ModList;
import net.neoforged.fml.common.Mod;
import net.neoforged.fml.config.ModConfig;
import net.neoforged.fml.event.lifecycle.FMLCommonSetupEvent;
import net.neoforged.neoforge.event.BuildCreativeModeTabContentsEvent;

@Mod(aeroclaims_ftb.MODID)
public class AeroClaims {
    public static final String MODID = "aeroclaims_ftb";

    public aeroclaims_ftb(IEventBus modBus, ModContainer modContainer) {
    
        ModBlocks.register(modBus);
        ModMenus.register(modBus);
    
        modBus.addListener(aeroclaims_ftb::addCreative);
        modBus.addListener(aeroclaims_ftb::onCommonSetup);
    
        modContainer.registerConfig(ModConfig.Type.SERVER, aeroclaims_ftbConfig.SPEC);
    
        boolean ftbLoaded =
                ModList.get().isLoaded("ftbchunks") &&
                ModList.get().isLoaded("ftbteams");
    
        ClaimManager.init(ftbLoaded);
    }

    private static void onCommonSetup(FMLCommonSetupEvent event) {
        SableSubLevelEventHandler.register();
    }

    private static void addCreative(BuildCreativeModeTabContentsEvent event) {
        if (event.getTabKey() == CreativeModeTabs.FUNCTIONAL_BLOCKS) {
            event.accept(ModBlocks.CLAIM_BLOCK_ITEM.get());
        }
    }
}
