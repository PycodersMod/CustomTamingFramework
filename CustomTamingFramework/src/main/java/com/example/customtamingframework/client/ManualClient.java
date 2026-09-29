package com.example.customtamingframework.client;

import com.example.customtamingframework.client.screen.ManualBookScreen;
import net.minecraft.client.Minecraft;

public final class ManualClient {
    private ManualClient() {
    }

    public static void open(String manualId) {
        Minecraft minecraft = Minecraft.getInstance();
        minecraft.setScreen(new ManualBookScreen(minecraft.screen, manualId));
    }
}
