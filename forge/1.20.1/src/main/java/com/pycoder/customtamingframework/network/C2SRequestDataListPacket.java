package com.pycoder.customtamingframework.network;

import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraftforge.network.NetworkEvent;

import java.util.function.Supplier;

public record C2SRequestDataListPacket(int entityId) {
    public static void encode(C2SRequestDataListPacket packet, FriendlyByteBuf buf) {
        buf.writeVarInt(packet.entityId());
    }

    public static C2SRequestDataListPacket decode(FriendlyByteBuf buf) {
        return new C2SRequestDataListPacket(buf.readVarInt());
    }

    public static void handle(C2SRequestDataListPacket packet, Supplier<NetworkEvent.Context> contextSupplier) {
        NetworkEvent.Context context = contextSupplier.get();
        context.enqueueWork(() -> {
            ServerPlayer player = context.getSender();
            if (player == null) return;
            player.displayClientMessage(Component.literal("实例数据变量暂不支持直接编辑。"), true);
        });
        context.setPacketHandled(true);
    }
}
