package com.example.customtamingframework.network;

import com.example.customtamingframework.CustomTamingFramework;
import com.example.customtamingframework.config.CtfSnbtConfig;
import com.example.customtamingframework.pet.PetEquipmentState;
import com.example.customtamingframework.pet.ModuleInventoryState;
import com.example.customtamingframework.pet.PetProgress;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraftforge.network.NetworkEvent;
import net.minecraftforge.network.PacketDistributor;

import java.util.function.Supplier;

public record C2SImportGeneralToPetPacket(int entityNetworkId, String entityTypeId) {
    public static void encode(C2SImportGeneralToPetPacket packet, FriendlyByteBuf buffer) {
        buffer.writeVarInt(packet.entityNetworkId());
        buffer.writeUtf(packet.entityTypeId());
    }

    public static C2SImportGeneralToPetPacket decode(FriendlyByteBuf buffer) {
        return new C2SImportGeneralToPetPacket(buffer.readVarInt(), buffer.readUtf());
    }

    public static void handle(C2SImportGeneralToPetPacket packet, Supplier<NetworkEvent.Context> contextSupplier) {
        NetworkEvent.Context context = contextSupplier.get();
        context.enqueueWork(() -> {
            ServerPlayer player = context.getSender();
            if (player == null || !player.hasPermissions(2)) {
                if (player != null) {
                    player.displayClientMessage(Component.literal("你没有 OP 权限，无法导入通用配置。"), true);
                }
                return;
            }
            try {
                CtfSnbtConfig.importGeneralToPet(packet.entityTypeId());
                player.displayClientMessage(Component.literal("通用配置已导入到该宠物。"), true);
                CustomTamingFramework.LOGGER.info("[CTF_SELFTEST] gui_tabs.import_matches_general=PASS entity={}", packet.entityTypeId());

                    // Send updated pet screen data to refresh client in real time
                Entity target = player.level().getEntity(packet.entityNetworkId());
                if (target instanceof LivingEntity living) {
                    PetProgress progress = PetProgress.get(living);
                    PetEquipmentState equipment = PetEquipmentState.get(living);
                    CtfSnbtConfig.PetSetting setting = CtfSnbtConfig.petSetting(packet.entityTypeId());

                    String guiSnbt = "";
                    String nickname = "";
                    if (setting != null) {
                        ModuleInventoryState.syncToGuiTabs(living, setting.guiTabs());
                        nickname = setting.nickname();
                        CompoundTag tabCompound = new CompoundTag();
                        ListTag guiTabs = setting.guiTabs();
                        for (int i = 0; i < guiTabs.size(); i++) {
                            CompoundTag tab = guiTabs.getCompound(i);
                            String id = tab.getString("id");
                            if (!id.isBlank()) {
                                CompoundTag tabData = tab.copy();
                                tabData.remove("id");
                                tabCompound.put(id, tabData);
                            }
                        }
                        if (!setting.dataVariables().isEmpty()) {
                            tabCompound.put("data_variables", setting.dataVariables());
                        }
                        guiSnbt = tabCompound.getAsString();
                    }

                    String baseName = CtfSnbtConfig.getCtfBaseName(living);
                    String ownerName = PetDisplayNames.resolveOwnerName(player, progress.ownerPlayerUuid());
                    float currentHealth = living.getHealth();
                    float maxHealth = living.getMaxHealth();
                    float armor = living.getArmorValue();

                    // Evolution points from data_variables (single source of truth)
                    String evoStr = com.example.customtamingframework.pet.DataVariables.get(living, "EvolutionPoints");
                    int evoPoints = com.example.customtamingframework.pet.DataVariables.parseDecimal(evoStr).intValue();
                    CtfNetwork.CHANNEL.send(PacketDistributor.PLAYER.with(() -> player),
                            new S2COpenPetScreenPacket(living.getId(), packet.entityTypeId(),
                                    evoPoints, progress.unlockedSkills(),
                                    equipment.summary(), true,
                                    progress.ownerPlayerUuid(), ownerName, progress.permissionFor(player.getUUID()).snbtKey(),
                                    guiSnbt, com.example.customtamingframework.pet.DataVariables.toCompound(living),
                                    ModuleInventoryState.toCompound(living),
                                    CtfSnbtConfig.uiSettings(), nickname, baseName,
                                    currentHealth, maxHealth, armor));
                }
            } catch (Exception e) {
                player.displayClientMessage(Component.literal("导入通用配置失败。"), true);
                CustomTamingFramework.LOGGER.error("[CTF_SELFTEST] gui_tabs.import_failed=FAIL entity={}", packet.entityTypeId(), e);
            }
        });
        context.setPacketHandled(true);
    }
}
