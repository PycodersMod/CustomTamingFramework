package com.pycoder.customtamingframework.network;

import com.pycoder.customtamingframework.client.screen.FoodListScreen;
import com.pycoder.customtamingframework.client.screen.PetListScreen;
import com.pycoder.customtamingframework.client.screen.SpecialFoodScreen;
import com.pycoder.customtamingframework.client.screen.TamingCoreScreen;
import net.minecraft.client.Minecraft;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraftforge.network.NetworkEvent;

import java.util.function.Supplier;

/**
 * Server responds with config data for one of the 4 config sub-screens.
 * screenType: 0=PetList, 1=FoodList, 2=TamingCore, 3=SpecialFood
 */
public record S2CConfigScreenDataPacket(int screenType, CompoundTag data, String parentGuiSnbt, boolean isOp,
                                        CompoundTag uiSettings) {

    public S2CConfigScreenDataPacket(FriendlyByteBuf buf) {
        this(buf.readVarInt(), buf.readNbt(), buf.readUtf(), buf.readBoolean(), buf.readNbt());
    }

    public void encode(FriendlyByteBuf buf) {
        buf.writeVarInt(screenType);
        buf.writeNbt(data);
        buf.writeUtf(parentGuiSnbt);
        buf.writeBoolean(isOp);
        buf.writeNbt(uiSettings != null ? uiSettings : new CompoundTag());
    }

    public static S2CConfigScreenDataPacket decode(FriendlyByteBuf buffer) {
        return new S2CConfigScreenDataPacket(buffer.readVarInt(), buffer.readNbt(), buffer.readUtf(), buffer.readBoolean(),
                buffer.readNbt());
    }

    public static void handle(S2CConfigScreenDataPacket packet, Supplier<NetworkEvent.Context> contextSupplier) {
        NetworkEvent.Context context = contextSupplier.get();
        context.enqueueWork(() -> {
            CompoundTag data = packet.data();
            String parentGuiSnbt = packet.parentGuiSnbt();
            boolean isOp = packet.isOp();
            CompoundTag uiSettings = packet.uiSettings();
            switch (packet.screenType()) {
                case 0 -> Minecraft.getInstance().setScreen(new PetListScreen(data, parentGuiSnbt, isOp, uiSettings));
                case 1 -> Minecraft.getInstance().setScreen(new FoodListScreen(data, parentGuiSnbt, isOp, uiSettings));
                case 2 -> Minecraft.getInstance().setScreen(new TamingCoreScreen(data, parentGuiSnbt, isOp, uiSettings));
                case 3 -> Minecraft.getInstance().setScreen(new SpecialFoodScreen(data, parentGuiSnbt, isOp, uiSettings));
            }
        });
        context.setPacketHandled(true);
    }
}
