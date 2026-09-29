package com.example.customtamingframework.client;

import com.example.customtamingframework.CustomTamingFramework;
import com.example.customtamingframework.network.C2SRequestOpenGeneralConfigPacket;
import com.example.customtamingframework.network.C2SRequestOpenPetScreenPacket;
import com.example.customtamingframework.network.CtfNetwork;
import com.mojang.blaze3d.platform.InputConstants;
import net.minecraft.client.KeyMapping;
import net.minecraft.client.Minecraft;
import net.minecraft.world.entity.Entity;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.client.event.RegisterKeyMappingsEvent;
import net.minecraftforge.client.settings.KeyConflictContext;
import net.minecraftforge.client.settings.KeyModifier;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import org.lwjgl.glfw.GLFW;

@Mod.EventBusSubscriber(modid = CustomTamingFramework.MOD_ID, bus = Mod.EventBusSubscriber.Bus.MOD, value = Dist.CLIENT)
public final class CtfClientEvents {
    private static final KeyMapping OPEN_PET_GUI = new KeyMapping(
            "key.custom_taming_framework.open_pet_gui",
            KeyConflictContext.IN_GAME,
            KeyModifier.SHIFT,
            InputConstants.Type.KEYSYM,
            GLFW.GLFW_KEY_T,
            "key.categories.custom_taming_framework"
    );
    private CtfClientEvents() {
    }

    @SubscribeEvent
    public static void registerKeys(RegisterKeyMappingsEvent event) {
        event.register(OPEN_PET_GUI);
    }

    @Mod.EventBusSubscriber(modid = CustomTamingFramework.MOD_ID, bus = Mod.EventBusSubscriber.Bus.FORGE, value = Dist.CLIENT)
    public static final class ForgeBus {
        private ForgeBus() {
        }

        @SubscribeEvent
        public static void clientTick(TickEvent.ClientTickEvent event) {
            if (event.phase != TickEvent.Phase.END) {
                return;
            }
            Minecraft minecraft = Minecraft.getInstance();
            while (OPEN_PET_GUI.consumeClick()) {
                if (minecraft.player == null || minecraft.hitResult == null || !minecraft.player.isShiftKeyDown()) {
                    continue;
                }
                if (minecraft.hitResult instanceof net.minecraft.world.phys.EntityHitResult hit) {
                    Entity entity = hit.getEntity();
                    CtfNetwork.CHANNEL.sendToServer(new C2SRequestOpenPetScreenPacket(entity.getId()));
                } else {
                    // Block/empty target → open general config GUI
                    CtfNetwork.CHANNEL.sendToServer(new C2SRequestOpenGeneralConfigPacket());
                }
            }
        }
    }
}
