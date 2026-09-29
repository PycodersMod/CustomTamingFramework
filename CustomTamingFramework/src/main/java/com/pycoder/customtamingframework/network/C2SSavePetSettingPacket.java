package com.pycoder.customtamingframework.network;

import com.pycoder.customtamingframework.CustomTamingFramework;
import com.pycoder.customtamingframework.config.CtfSnbtConfig;
import com.pycoder.customtamingframework.pet.ModuleInventoryState;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.TagParser;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraftforge.network.NetworkEvent;

import java.util.function.Supplier;

public record C2SSavePetSettingPacket(String entityId, String nickname, String guiSnbt, int entityNetworkId) {
    public static void encode(C2SSavePetSettingPacket packet, FriendlyByteBuf buffer) {
        buffer.writeUtf(packet.entityId());
        buffer.writeUtf(packet.nickname());
        buffer.writeUtf(packet.guiSnbt() != null ? packet.guiSnbt() : "");
        buffer.writeVarInt(packet.entityNetworkId());
    }

    public static C2SSavePetSettingPacket decode(FriendlyByteBuf buffer) {
        return new C2SSavePetSettingPacket(buffer.readUtf(), buffer.readUtf(), buffer.readUtf(), buffer.readVarInt());
    }

    public static void handle(C2SSavePetSettingPacket packet, Supplier<NetworkEvent.Context> contextSupplier) {
        NetworkEvent.Context context = contextSupplier.get();
        context.enqueueWork(() -> {
            ServerPlayer player = context.getSender();
            if (player == null || !player.hasPermissions(2)) {
                if (player != null) {
                    player.displayClientMessage(Component.literal("你没有 OP 权限，无法保存宠物配置。"), true);
                }
                return;
            }
            try {
                ListTag guiTabs = new ListTag();
                CompoundTag dataVariables = new CompoundTag();
                String snbt = packet.guiSnbt();
                if (snbt != null && !snbt.isBlank()) {
                    CompoundTag parsed = TagParser.parseTag(snbt);
                    for (String key : parsed.getAllKeys()) {
                        if (key.equals("data_variables")) {
                            if (parsed.contains(key, 10)) {
                                dataVariables = parsed.getCompound(key).copy();
                            }
                            continue;
                        }
                        if (parsed.contains(key, 10)) {
                            CompoundTag tab = parsed.getCompound(key).copy();
                            tab.putString("id", key);
                            guiTabs.add(tab);
                        }
                    }
                }
                CtfSnbtConfig.savePetSetting(packet.entityId(), packet.nickname(), guiTabs, dataVariables);

                // Update the entity's display name with the new nickname
                Entity target = player.level().getEntity(packet.entityNetworkId());
                if (target instanceof LivingEntity living) {
                    ModuleInventoryState.syncToGuiTabs(living, guiTabs);
                    CtfSnbtConfig.updateEntityDisplayName(living);
                }

                player.displayClientMessage(Component.literal("宠物配置已保存到 PetSettings/" + packet.entityId().replace(':', '_') + ".snbt"), true);
                CustomTamingFramework.LOGGER.info("[CTF_SELFTEST] pet_setting.save_success=PASS entity={}", packet.entityId());
            } catch (Exception e) {
                player.displayClientMessage(Component.literal("宠物配置保存失败。"), true);
                CustomTamingFramework.LOGGER.error("[CTF_SELFTEST] pet_setting.save_failed=FAIL entity={} error={}", packet.entityId(), e.getMessage());
            }
        });
        context.setPacketHandled(true);
    }
}
