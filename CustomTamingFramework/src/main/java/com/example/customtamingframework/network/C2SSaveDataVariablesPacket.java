package com.example.customtamingframework.network;

import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraftforge.network.NetworkEvent;

import java.util.function.Supplier;

public record C2SSaveDataVariablesPacket(int entityId, CompoundTag data) {
    public static void encode(C2SSaveDataVariablesPacket packet, FriendlyByteBuf buf) {
        buf.writeVarInt(packet.entityId());
        buf.writeNbt(packet.data());
    }

    public static C2SSaveDataVariablesPacket decode(FriendlyByteBuf buf) {
        return new C2SSaveDataVariablesPacket(buf.readVarInt(), buf.readNbt());
    }

    public static void handle(C2SSaveDataVariablesPacket packet, Supplier<NetworkEvent.Context> contextSupplier) {
        NetworkEvent.Context context = contextSupplier.get();
        context.enqueueWork(() -> {
            ServerPlayer player = context.getSender();
            if (player == null) return;
            player.displayClientMessage(Component.literal("实例数据变量暂不支持直接保存。"), true);
        });
        context.setPacketHandled(true);
    }
}
