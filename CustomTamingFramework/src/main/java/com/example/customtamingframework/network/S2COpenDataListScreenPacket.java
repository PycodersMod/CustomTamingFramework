package com.example.customtamingframework.network;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.fml.DistExecutor;
import net.minecraftforge.network.NetworkEvent;

import com.example.customtamingframework.pet.DataVariables;

import java.util.Map;
import java.util.function.Supplier;

public record S2COpenDataListScreenPacket(int entityId, CompoundTag data) {
    public static void encode(S2COpenDataListScreenPacket packet, FriendlyByteBuf buf) {
        buf.writeVarInt(packet.entityId());
        buf.writeNbt(packet.data());
    }

    public static S2COpenDataListScreenPacket decode(FriendlyByteBuf buf) {
        return new S2COpenDataListScreenPacket(buf.readVarInt(), buf.readNbt());
    }

    public static void handle(S2COpenDataListScreenPacket packet, Supplier<NetworkEvent.Context> contextSupplier) {
        NetworkEvent.Context context = contextSupplier.get();
        DistExecutor.unsafeRunWhenOn(Dist.CLIENT, () -> () -> {
            Map<String, DataVariables.VariableState> map = packet.data() != null
                    ? new java.util.TreeMap<>(DataVariables.fromCompoundStates(packet.data()))
                    : new java.util.TreeMap<>();
            Screen previous = Minecraft.getInstance().screen;
            java.util.function.Consumer<Map<String, DataVariables.VariableState>> saveCallback = saved -> {
                CompoundTag tag = DataVariables.toCompound(saved);
                CtfNetwork.CHANNEL.sendToServer(new C2SSaveDataVariablesPacket(packet.entityId(), tag));
                if (previous instanceof com.example.customtamingframework.client.screen.PetScreen petScreen) {
                    petScreen.syncDataVariables(saved);
                }
            };
            Minecraft.getInstance().setScreen(
                    new com.example.customtamingframework.client.screen.DataListScreen(map, saveCallback, previous));
        });
        context.setPacketHandled(true);
    }
}
