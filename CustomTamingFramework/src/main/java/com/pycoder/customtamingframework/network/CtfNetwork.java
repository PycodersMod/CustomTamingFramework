package com.pycoder.customtamingframework.network;

import com.pycoder.customtamingframework.CustomTamingFramework;
import net.minecraft.resources.ResourceLocation;
import net.minecraftforge.network.NetworkRegistry;
import net.minecraftforge.network.simple.SimpleChannel;

public final class CtfNetwork {
    private static final String PROTOCOL_VERSION = "1";
    public static final SimpleChannel CHANNEL = NetworkRegistry.newSimpleChannel(
            new ResourceLocation(CustomTamingFramework.MOD_ID, "main"),
            () -> PROTOCOL_VERSION,
            PROTOCOL_VERSION::equals,
            PROTOCOL_VERSION::equals
    );

    private CtfNetwork() {
    }

    public static void register() {
        int idx = 0;
        CHANNEL.messageBuilder(S2COpenPetScreenPacket.class, idx++)
                .encoder(S2COpenPetScreenPacket::encode)
                .decoder(S2COpenPetScreenPacket::decode)
                .consumerMainThread(S2COpenPetScreenPacket::handle)
                .add();
        CHANNEL.messageBuilder(C2SRequestOpenPetScreenPacket.class, idx++)
                .encoder(C2SRequestOpenPetScreenPacket::encode)
                .decoder(C2SRequestOpenPetScreenPacket::decode)
                .consumerMainThread(C2SRequestOpenPetScreenPacket::handle)
                .add();
        CHANNEL.messageBuilder(C2SRequestOpenGeneralConfigPacket.class, idx++)
                .encoder(C2SRequestOpenGeneralConfigPacket::encode)
                .decoder(C2SRequestOpenGeneralConfigPacket::decode)
                .consumerMainThread(C2SRequestOpenGeneralConfigPacket::handle)
                .add();
        CHANNEL.messageBuilder(S2COpenGeneralConfigScreenPacket.class, idx++)
                .encoder(S2COpenGeneralConfigScreenPacket::encode)
                .decoder(S2COpenGeneralConfigScreenPacket::decode)
                .consumerMainThread(S2COpenGeneralConfigScreenPacket::handle)
                .add();
        CHANNEL.messageBuilder(C2SSaveGeneralConfigPacket.class, idx++)
                .encoder(C2SSaveGeneralConfigPacket::encode)
                .decoder(C2SSaveGeneralConfigPacket::decode)
                .consumerMainThread(C2SSaveGeneralConfigPacket::handle)
                .add();
        CHANNEL.messageBuilder(C2SSavePetSettingPacket.class, idx++)
                .encoder(C2SSavePetSettingPacket::encode)
                .decoder(C2SSavePetSettingPacket::decode)
                .consumerMainThread(C2SSavePetSettingPacket::handle)
                .add();
        CHANNEL.messageBuilder(C2SImportGeneralToPetPacket.class, idx++)
                .encoder(C2SImportGeneralToPetPacket::encode)
                .decoder(C2SImportGeneralToPetPacket::decode)
                .consumerMainThread(C2SImportGeneralToPetPacket::handle)
                .add();
        CHANNEL.messageBuilder(C2SSetEntityBaseNamePacket.class, idx++)
                .encoder(C2SSetEntityBaseNamePacket::encode)
                .decoder(C2SSetEntityBaseNamePacket::decode)
                .consumerMainThread(C2SSetEntityBaseNamePacket::handle)
                .add();
        CHANNEL.messageBuilder(C2SRequestConfigScreenPacket.class, idx++)
                .encoder(C2SRequestConfigScreenPacket::encode)
                .decoder(C2SRequestConfigScreenPacket::decode)
                .consumerMainThread(C2SRequestConfigScreenPacket::handle)
                .add();
        CHANNEL.messageBuilder(S2CConfigScreenDataPacket.class, idx++)
                .encoder(S2CConfigScreenDataPacket::encode)
                .decoder(S2CConfigScreenDataPacket::decode)
                .consumerMainThread(S2CConfigScreenDataPacket::handle)
                .add();
        CHANNEL.messageBuilder(C2SSaveConfigScreenPacket.class, idx++)
                .encoder(C2SSaveConfigScreenPacket::encode)
                .decoder(C2SSaveConfigScreenPacket::decode)
                .consumerMainThread(C2SSaveConfigScreenPacket::handle)
                .add();
        CHANNEL.messageBuilder(C2SSaveUiSettingsPacket.class, idx++)
                .encoder(C2SSaveUiSettingsPacket::encode)
                .decoder(C2SSaveUiSettingsPacket::decode)
                .consumerMainThread(C2SSaveUiSettingsPacket::handle)
                .add();
        // v0.1.4-alpha: Data variables
        CHANNEL.messageBuilder(C2SRequestDataListPacket.class, idx++)
                .encoder(C2SRequestDataListPacket::encode)
                .decoder(C2SRequestDataListPacket::decode)
                .consumerMainThread(C2SRequestDataListPacket::handle)
                .add();
        CHANNEL.messageBuilder(S2COpenDataListScreenPacket.class, idx++)
                .encoder(S2COpenDataListScreenPacket::encode)
                .decoder(S2COpenDataListScreenPacket::decode)
                .consumerMainThread(S2COpenDataListScreenPacket::handle)
                .add();
        CHANNEL.messageBuilder(C2SSaveDataVariablesPacket.class, idx++)
                .encoder(C2SSaveDataVariablesPacket::encode)
                .decoder(C2SSaveDataVariablesPacket::decode)
                .consumerMainThread(C2SSaveDataVariablesPacket::handle)
                .add();
        CHANNEL.messageBuilder(C2SRequestOpenPetPermissionScreenPacket.class, idx++)
                .encoder(C2SRequestOpenPetPermissionScreenPacket::encode)
                .decoder(C2SRequestOpenPetPermissionScreenPacket::decode)
                .consumerMainThread(C2SRequestOpenPetPermissionScreenPacket::handle)
                .add();
        CHANNEL.messageBuilder(S2CPetPermissionSnapshotPacket.class, idx++)
                .encoder(S2CPetPermissionSnapshotPacket::encode)
                .decoder(S2CPetPermissionSnapshotPacket::decode)
                .consumerMainThread(S2CPetPermissionSnapshotPacket::handle)
                .add();
        CHANNEL.messageBuilder(C2SSavePetPermissionsPacket.class, idx++)
                .encoder(C2SSavePetPermissionsPacket::encode)
                .decoder(C2SSavePetPermissionsPacket::decode)
                .consumerMainThread(C2SSavePetPermissionsPacket::handle)
                .add();
        CHANNEL.messageBuilder(C2STransferPetOwnerPacket.class, idx++)
                .encoder(C2STransferPetOwnerPacket::encode)
                .decoder(C2STransferPetOwnerPacket::decode)
                .consumerMainThread(C2STransferPetOwnerPacket::handle)
                .add();
        CHANNEL.messageBuilder(C2SSyncPetInventoryOverlayPacket.class, idx++)
                .encoder(C2SSyncPetInventoryOverlayPacket::encode)
                .decoder(C2SSyncPetInventoryOverlayPacket::decode)
                .consumerMainThread(C2SSyncPetInventoryOverlayPacket::handle)
                .add();
        CHANNEL.messageBuilder(C2SRequestOpenModuleInventoryPacket.class, idx++)
                .encoder(C2SRequestOpenModuleInventoryPacket::encode)
                .decoder(C2SRequestOpenModuleInventoryPacket::decode)
                .consumerMainThread(C2SRequestOpenModuleInventoryPacket::handle)
                .add();
    }
}
