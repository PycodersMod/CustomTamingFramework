package com.pycoder.customtamingframework.network;

import com.pycoder.customtamingframework.CustomTamingFramework;
import com.pycoder.customtamingframework.config.CtfSnbtConfig;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.TagParser;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraftforge.network.NetworkEvent;

import java.util.function.Supplier;

public record C2SSaveGeneralConfigPacket(String guiSnbt) {
    public static void encode(C2SSaveGeneralConfigPacket packet, FriendlyByteBuf buffer) {
        buffer.writeUtf(packet.guiSnbt());
    }

    public static C2SSaveGeneralConfigPacket decode(FriendlyByteBuf buffer) {
        return new C2SSaveGeneralConfigPacket(buffer.readUtf());
    }

    public static void handle(C2SSaveGeneralConfigPacket packet, Supplier<NetworkEvent.Context> contextSupplier) {
        NetworkEvent.Context context = contextSupplier.get();
        context.enqueueWork(() -> {
            ServerPlayer player = context.getSender();
            if (player == null || !player.hasPermissions(2)) {
                if (player != null) {
                    player.displayClientMessage(Component.literal("你没有 OP 权限，无法保存通用配置。"), true);
                    CustomTamingFramework.LOGGER.info("[CTF_SELFTEST] general_config.save_rejected_non_op=PASS");
                }
                return;
            }
            try {
                CompoundTag tag = TagParser.parseTag(packet.guiSnbt());
                CtfSnbtConfig.saveGeneralGui(tag);
                player.displayClientMessage(Component.literal("通用配置已保存到 CTF_Settings/GeneralGUI.snbt。"), true);
                CustomTamingFramework.LOGGER.info("[CTF_SELFTEST] general_config.save_success=PASS");
            } catch (Exception e) {
                player.displayClientMessage(Component.literal("通用配置保存失败：无效的 SNBT 数据。"), true);
                CustomTamingFramework.LOGGER.error("[CTF_SELFTEST] general_config.save_failed=FAIL error={}", e.getMessage());
            }
        });
        context.setPacketHandled(true);
    }
}
