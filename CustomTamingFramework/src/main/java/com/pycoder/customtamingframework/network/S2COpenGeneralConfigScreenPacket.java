package com.pycoder.customtamingframework.network;

import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.fml.DistExecutor;
import net.minecraftforge.network.NetworkEvent;

import java.util.function.Supplier;

public record S2COpenGeneralConfigScreenPacket(String guiSnbt, boolean isOp, CompoundTag uiSettings) {
    public static void encode(S2COpenGeneralConfigScreenPacket packet, FriendlyByteBuf buffer) {
        buffer.writeUtf(packet.guiSnbt());
        buffer.writeBoolean(packet.isOp());
        buffer.writeNbt(packet.uiSettings() != null ? packet.uiSettings() : new CompoundTag());
    }

    public static S2COpenGeneralConfigScreenPacket decode(FriendlyByteBuf buffer) {
        return new S2COpenGeneralConfigScreenPacket(buffer.readUtf(), buffer.readBoolean(), buffer.readNbt());
    }

    public static void handle(S2COpenGeneralConfigScreenPacket packet, Supplier<NetworkEvent.Context> contextSupplier) {
        NetworkEvent.Context context = contextSupplier.get();
        DistExecutor.unsafeRunWhenOn(Dist.CLIENT, () -> () -> ClientPacketHandler.openGeneralConfigScreen(packet));
        context.setPacketHandled(true);
    }
}
