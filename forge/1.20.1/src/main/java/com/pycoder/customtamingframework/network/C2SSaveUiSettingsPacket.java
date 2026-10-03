package com.pycoder.customtamingframework.network;

import com.pycoder.customtamingframework.CustomTamingFramework;
import com.pycoder.customtamingframework.config.CtfSnbtConfig;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraftforge.network.NetworkEvent;

import java.util.function.Supplier;

public record C2SSaveUiSettingsPacket(CompoundTag settings) {
    public static void encode(C2SSaveUiSettingsPacket packet, FriendlyByteBuf buffer) {
        buffer.writeNbt(packet.settings() != null ? packet.settings() : new CompoundTag());
    }

    public static C2SSaveUiSettingsPacket decode(FriendlyByteBuf buffer) {
        return new C2SSaveUiSettingsPacket(buffer.readNbt());
    }

    public static void handle(C2SSaveUiSettingsPacket packet, Supplier<NetworkEvent.Context> contextSupplier) {
        NetworkEvent.Context context = contextSupplier.get();
        context.enqueueWork(() -> {
            ServerPlayer player = context.getSender();
            if (player == null || !player.hasPermissions(2)) {
                if (player != null) {
                    player.displayClientMessage(Component.literal("你没有 OP 权限，无法保存用户设置。"), true);
                }
                return;
            }
            try {
                CtfSnbtConfig.saveUiSettings(packet.settings());
                player.displayClientMessage(Component.literal("用户设置已保存。"), true);
                CustomTamingFramework.LOGGER.info("[CTF_SELFTEST] config.snbt.user_settings_saved=PASS");
            } catch (Exception e) {
                player.displayClientMessage(Component.literal("用户设置保存失败。"), true);
                CustomTamingFramework.LOGGER.error("[CTF] Failed to save ui settings", e);
            }
        });
        context.setPacketHandled(true);
    }
}
