package com.example.customtamingframework.network;

import com.example.customtamingframework.CustomTamingFramework;
import com.example.customtamingframework.config.CtfUiSettings;
import com.example.customtamingframework.config.CtfSnbtConfig;
import com.example.customtamingframework.manual.ManualUnlockState;
import net.minecraft.ChatFormatting;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraftforge.network.NetworkEvent;
import net.minecraftforge.network.PacketDistributor;

import java.util.function.Supplier;

public record C2SRequestOpenGeneralConfigPacket() {
    public static void encode(C2SRequestOpenGeneralConfigPacket packet, FriendlyByteBuf buffer) {
    }

    public static C2SRequestOpenGeneralConfigPacket decode(FriendlyByteBuf buffer) {
        return new C2SRequestOpenGeneralConfigPacket();
    }

    public static void handle(C2SRequestOpenGeneralConfigPacket packet, Supplier<NetworkEvent.Context> contextSupplier) {
        NetworkEvent.Context context = contextSupplier.get();
        context.enqueueWork(() -> {
            ServerPlayer player = context.getSender();
            if (player == null) {
                return;
            }
            boolean isOp = player.hasPermissions(2);
            ManualUnlockState.checkUserManualTrigger(player);
            boolean generalReadable = CtfUiSettings.visibleForNonOp(CtfSnbtConfig.uiSettings(), CtfUiSettings.SHOW_NON_OP_GENERAL_CONFIG_SCREEN);
            if (!isOp && !generalReadable) {
                CustomTamingFramework.LOGGER.info("[CTF_SELFTEST] general_config.blocked_for_non_op=PASS player={}", player.getGameProfile().getName());
                return;
            }
            if (!isOp && !ManualUnlockState.hasUnlockedUserManual(player)) {
                player.displayClientMessage(Component.literal("好像遗忘了某些记忆……").withStyle(ChatFormatting.GRAY), true);
                return;
            }
            CtfNetwork.CHANNEL.send(PacketDistributor.PLAYER.with(() -> player),
                    new S2COpenGeneralConfigScreenPacket(CtfSnbtConfig.generalGui().getAsString(), isOp,
                            CtfSnbtConfig.uiSettings()));
            CustomTamingFramework.LOGGER.info("[CTF_SELFTEST] general_config.snbt_sent_to_client=PASS");
        });
        context.setPacketHandled(true);
    }
}
