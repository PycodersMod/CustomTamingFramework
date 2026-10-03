package com.pycoder.customtamingframework.network;

import com.pycoder.customtamingframework.config.CtfSnbtConfig;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraftforge.network.NetworkEvent;

import java.util.function.Supplier;

public record C2SSetEntityBaseNamePacket(int entityNetworkId, String name) {
    public static void encode(C2SSetEntityBaseNamePacket packet, FriendlyByteBuf buffer) {
        buffer.writeVarInt(packet.entityNetworkId());
        buffer.writeUtf(packet.name());
    }

    public static C2SSetEntityBaseNamePacket decode(FriendlyByteBuf buffer) {
        return new C2SSetEntityBaseNamePacket(buffer.readVarInt(), buffer.readUtf());
    }

    public static void handle(C2SSetEntityBaseNamePacket packet, Supplier<NetworkEvent.Context> contextSupplier) {
        NetworkEvent.Context context = contextSupplier.get();
        context.enqueueWork(() -> {
            ServerPlayer player = context.getSender();
            if (player == null) return;
            Entity entity = player.level().getEntity(packet.entityNetworkId());
            if (entity instanceof LivingEntity living) {
                CtfSnbtConfig.setCtfBaseName(living, packet.name());
                CtfSnbtConfig.updateEntityDisplayName(living);
            }
        });
        context.setPacketHandled(true);
    }
}
