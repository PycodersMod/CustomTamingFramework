package com.example.customtamingframework.network;

import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.fml.DistExecutor;
import net.minecraftforge.network.NetworkEvent;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Supplier;

public record S2COpenPetScreenPacket(int entityId, String entityTypeString, int evolutionPoints,
                                     List<String> unlockedSkills, String equipmentSummary, boolean isOp,
                                     String ownerPlayerUuid, String ownerPlayerName, String accessMode,
                                     String guiSnbt, CompoundTag dataVariables, CompoundTag moduleInventories, CompoundTag uiSettings,
                                     String nickname, String baseName, float currentHealth, float maxHealth, float armor) {
    public static void encode(S2COpenPetScreenPacket packet, FriendlyByteBuf buffer) {
        buffer.writeVarInt(packet.entityId());
        buffer.writeUtf(packet.entityTypeString());
        buffer.writeVarInt(packet.evolutionPoints());
        buffer.writeVarInt(packet.unlockedSkills().size());
        for (String skill : packet.unlockedSkills()) {
            buffer.writeUtf(skill);
        }
        buffer.writeUtf(packet.equipmentSummary());
        buffer.writeBoolean(packet.isOp());
        buffer.writeUtf(packet.ownerPlayerUuid() != null ? packet.ownerPlayerUuid() : "");
        buffer.writeUtf(packet.ownerPlayerName() != null ? packet.ownerPlayerName() : "");
        buffer.writeUtf(packet.accessMode() != null ? packet.accessMode() : "");
        buffer.writeUtf(packet.guiSnbt() != null ? packet.guiSnbt() : "");
        buffer.writeNbt(packet.dataVariables() != null ? packet.dataVariables() : new CompoundTag());
        buffer.writeNbt(packet.moduleInventories() != null ? packet.moduleInventories() : new CompoundTag());
        buffer.writeNbt(packet.uiSettings() != null ? packet.uiSettings() : new CompoundTag());
        buffer.writeUtf(packet.nickname() != null ? packet.nickname() : "");
        buffer.writeUtf(packet.baseName() != null ? packet.baseName() : "");
        buffer.writeFloat(packet.currentHealth());
        buffer.writeFloat(packet.maxHealth());
        buffer.writeFloat(packet.armor());
    }

    public static S2COpenPetScreenPacket decode(FriendlyByteBuf buffer) {
        int entityId = buffer.readVarInt();
        String entityTypeStr = buffer.readUtf();
        int evolutionPoints = buffer.readVarInt();
        int skillCount = buffer.readVarInt();
        List<String> skills = new ArrayList<>();
        for (int i = 0; i < skillCount; i++) {
                skills.add(buffer.readUtf());
        }
        return new S2COpenPetScreenPacket(entityId, entityTypeStr, evolutionPoints, List.copyOf(skills),
                buffer.readUtf(), buffer.readBoolean(), buffer.readUtf(), buffer.readUtf(), buffer.readUtf(), buffer.readUtf(), buffer.readNbt(), buffer.readNbt(), buffer.readNbt(),
                buffer.readUtf(), buffer.readUtf(), buffer.readFloat(), buffer.readFloat(), buffer.readFloat());
    }

    public static void handle(S2COpenPetScreenPacket packet, Supplier<NetworkEvent.Context> contextSupplier) {
        NetworkEvent.Context context = contextSupplier.get();
        DistExecutor.unsafeRunWhenOn(Dist.CLIENT, () -> () -> ClientPacketHandler.openPetScreen(packet));
        context.setPacketHandled(true);
    }
}
