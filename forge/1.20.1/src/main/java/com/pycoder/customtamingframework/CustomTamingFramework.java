package com.pycoder.customtamingframework;

import com.pycoder.customtamingframework.command.CtfCommands;
import com.pycoder.customtamingframework.config.CtfSnbtConfig;
import com.pycoder.customtamingframework.network.CtfNetwork;
import com.pycoder.customtamingframework.registry.CtfItems;
import com.pycoder.customtamingframework.registry.CtfMenus;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.event.RegisterCommandsEvent;
import net.minecraftforge.eventbus.api.IEventBus;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.event.lifecycle.FMLLoadCompleteEvent;
import net.minecraftforge.fml.event.lifecycle.FMLCommonSetupEvent;
import net.minecraftforge.fml.javafmlmod.FMLJavaModLoadingContext;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@Mod(CustomTamingFramework.MOD_ID)
public final class CustomTamingFramework {
    public static final String MOD_ID = "custom_taming_framework";
    public static final Logger LOGGER = LoggerFactory.getLogger(MOD_ID);

    public CustomTamingFramework() {
        IEventBus modBus = FMLJavaModLoadingContext.get().getModEventBus();
        CtfItems.register(modBus);
        CtfMenus.register(modBus);
        modBus.addListener(this::commonSetup);
        modBus.addListener(this::loadComplete);
        CtfNetwork.register();

        MinecraftForge.EVENT_BUS.addListener(this::registerCommands);
        MinecraftForge.EVENT_BUS.register(new CtfForgeEvents());
        LOGGER.info("Custom Taming Framework initialized");
    }

    private void commonSetup(FMLCommonSetupEvent event) {
        event.enqueueWork(CtfSnbtConfig::init);
    }

    private void loadComplete(FMLLoadCompleteEvent event) {
        CtfSnbtConfig.finalizeLoadedDefaults();
    }

    private void registerCommands(RegisterCommandsEvent event) {
        CtfCommands.register(event.getDispatcher());
    }
}
