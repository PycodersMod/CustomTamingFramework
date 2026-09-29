package com.pycoder.customtamingframework.network;

import com.pycoder.customtamingframework.CustomTamingFramework;
import com.pycoder.customtamingframework.config.CtFConfigHelper;
import com.pycoder.customtamingframework.config.CtfSnbtConfig;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.server.level.ServerPlayer;
import net.minecraftforge.network.NetworkEvent;
import net.minecraftforge.network.PacketDistributor;

import java.util.function.Supplier;

/**
 * Client requests config data for one of the 3 config sub-screens.
 * screenType: 0=PetList, 1=FoodList, 2=TamingCore
 */
public record C2SRequestConfigScreenPacket(int screenType) {
    public static void encode(C2SRequestConfigScreenPacket packet, FriendlyByteBuf buffer) {
        buffer.writeVarInt(packet.screenType());
    }

    public static C2SRequestConfigScreenPacket decode(FriendlyByteBuf buffer) {
        return new C2SRequestConfigScreenPacket(buffer.readVarInt());
    }

    public static void handle(C2SRequestConfigScreenPacket packet, Supplier<NetworkEvent.Context> contextSupplier) {
        NetworkEvent.Context context = contextSupplier.get();
        context.enqueueWork(() -> {
            ServerPlayer player = context.getSender();
            if (player == null) return;

            boolean isOp = player.hasPermissions(2);
            var data = CtFConfigHelper.buildScreenData(packet.screenType());
            String parentGuiSnbt = CtfSnbtConfig.generalGui().getAsString();
            CtfNetwork.CHANNEL.send(PacketDistributor.PLAYER.with(() -> player),
                    new S2CConfigScreenDataPacket(packet.screenType(), data, parentGuiSnbt, isOp,
                            CtfSnbtConfig.uiSettings()));
            CustomTamingFramework.LOGGER.debug("[CTF] Config screen data sent type={}", packet.screenType());
        });
        context.setPacketHandled(true);
    }
}
