package com.pycoder.customtamingframework.network;

import com.pycoder.customtamingframework.CustomTamingFramework;
import com.pycoder.customtamingframework.config.CtFConfigHelper;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraftforge.network.NetworkEvent;

import java.util.function.Supplier;

/**
 * Client saves config data for one of the 3 config sub-screens.
 * screenType: 0=PetList, 1=FoodList, 2=TamingCore
 */
public record C2SSaveConfigScreenPacket(int screenType, CompoundTag data) {
    public static void encode(C2SSaveConfigScreenPacket packet, FriendlyByteBuf buffer) {
        buffer.writeVarInt(packet.screenType());
        buffer.writeNbt(packet.data());
    }

    public static C2SSaveConfigScreenPacket decode(FriendlyByteBuf buffer) {
        return new C2SSaveConfigScreenPacket(buffer.readVarInt(), buffer.readNbt());
    }

    public static void handle(C2SSaveConfigScreenPacket packet, Supplier<NetworkEvent.Context> contextSupplier) {
        NetworkEvent.Context context = contextSupplier.get();
        context.enqueueWork(() -> {
            ServerPlayer player = context.getSender();
            if (player == null || !player.hasPermissions(2)) {
                if (player != null) {
                    player.displayClientMessage(Component.literal("你没有 OP 权限，无法保存配置。"), true);
                }
                return;
            }
            try {
                ServerLevel level = player.serverLevel();
                CtFConfigHelper.applyScreenData(packet.screenType(), packet.data(), level);
                player.displayClientMessage(Component.literal("配置已保存。"), true);
                CustomTamingFramework.LOGGER.info("[CTF] Config screen saved type={}", packet.screenType());
            } catch (Exception e) {
                player.displayClientMessage(Component.literal("配置保存失败。"), true);
                CustomTamingFramework.LOGGER.error("[CTF] Config screen save failed type={}", packet.screenType(), e);
            }
        });
        context.setPacketHandled(true);
    }
}
