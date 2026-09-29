package com.pycoder.customtamingframework.client;

import com.pycoder.customtamingframework.CustomTamingFramework;
import com.pycoder.customtamingframework.client.screen.ModuleInventoryScreen;
import com.pycoder.customtamingframework.registry.CtfMenus;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraft.client.gui.screens.MenuScreens;
import net.minecraftforge.fml.event.lifecycle.FMLClientSetupEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

@Mod.EventBusSubscriber(modid = CustomTamingFramework.MOD_ID, bus = Mod.EventBusSubscriber.Bus.MOD, value = Dist.CLIENT)
public final class ClientModEvents {
    private ClientModEvents() {
    }

    @SubscribeEvent
    public static void registerScreens(FMLClientSetupEvent event) {
        event.enqueueWork(() -> MenuScreens.register(CtfMenus.MODULE_INVENTORY.get(), ModuleInventoryScreen::new));
    }
}
