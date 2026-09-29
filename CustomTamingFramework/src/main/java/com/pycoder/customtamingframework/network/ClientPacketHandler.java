package com.pycoder.customtamingframework.network;

import com.pycoder.customtamingframework.client.screen.GeneralConfigScreen;
import com.pycoder.customtamingframework.client.screen.PetScreen;
import com.pycoder.customtamingframework.client.screen.PetPermissionScreen;
import com.pycoder.customtamingframework.pet.DataVariables;
import net.minecraft.client.Minecraft;

public final class ClientPacketHandler {
    private ClientPacketHandler() {
    }

    public static void openPetScreen(S2COpenPetScreenPacket packet) {
        Minecraft.getInstance().setScreen(new PetScreen(packet.entityId(), packet.entityTypeString(),
                packet.evolutionPoints(), packet.unlockedSkills(), packet.equipmentSummary(), packet.isOp(),
                packet.ownerPlayerUuid(), packet.ownerPlayerName(), packet.accessMode(),
                packet.guiSnbt(), DataVariables.fromCompoundStates(packet.dataVariables()), packet.moduleInventories(), packet.uiSettings(),
                packet.nickname(), packet.baseName(), packet.currentHealth(), packet.maxHealth(), packet.armor()));
    }

    public static void openGeneralConfigScreen(S2COpenGeneralConfigScreenPacket packet) {
        Minecraft.getInstance().setScreen(new GeneralConfigScreen(packet.guiSnbt(), packet.isOp(), packet.uiSettings()));
    }

    public static void syncPetPermissionSnapshot(S2CPetPermissionSnapshotPacket packet) {
        Minecraft minecraft = Minecraft.getInstance();
        if (minecraft.screen instanceof PetPermissionScreen permissionScreen && permissionScreen.matchesEntity(packet.entityId())) {
            permissionScreen.applySnapshot(packet);
            return;
        }
        if (minecraft.screen instanceof PetScreen petScreen && petScreen.matchesEntity(packet.entityId())) {
            if (packet.openGui()) {
                minecraft.setScreen(new PetPermissionScreen(petScreen, petScreen.isOpPlayer(), packet));
            } else {
                petScreen.syncAccessState(packet.ownerPlayerUuid(), packet.ownerPlayerName(), packet.viewerPermission());
                petScreen.refreshLayout();
            }
        }
    }
}
