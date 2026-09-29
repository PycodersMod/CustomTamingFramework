package com.pycoder.customtamingframework.client;

import com.pycoder.customtamingframework.client.screen.ManualBookScreen;
import net.minecraft.client.Minecraft;

public final class ManualClient {
    private ManualClient() {
    }

    public static void open(String manualId) {
        Minecraft minecraft = Minecraft.getInstance();
        minecraft.setScreen(new ManualBookScreen(minecraft.screen, manualId));
    }
}
