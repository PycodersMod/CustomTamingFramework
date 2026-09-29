package com.pycoder.customtamingframework.network;

import com.pycoder.customtamingframework.CustomTamingFramework;
import com.pycoder.customtamingframework.config.CtfSnbtConfig;
import com.pycoder.customtamingframework.pet.PetEquipmentState;
import com.pycoder.customtamingframework.pet.DataVariables;
import com.pycoder.customtamingframework.pet.ModuleInventoryState;
import com.pycoder.customtamingframework.pet.PetProgress;
import com.pycoder.customtamingframework.manual.ManualUnlockState;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.chat.Component;
import net.minecraft.ChatFormatting;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraftforge.network.NetworkEvent;
import net.minecraftforge.network.PacketDistributor;

import java.util.function.Supplier;

public record C2SRequestOpenPetScreenPacket(int entityId) {
    public static void encode(C2SRequestOpenPetScreenPacket packet, FriendlyByteBuf buffer) {
        buffer.writeVarInt(packet.entityId());
    }

    public static C2SRequestOpenPetScreenPacket decode(FriendlyByteBuf buffer) {
        return new C2SRequestOpenPetScreenPacket(buffer.readVarInt());
    }

    public static void handle(C2SRequestOpenPetScreenPacket packet, Supplier<NetworkEvent.Context> contextSupplier) {
        NetworkEvent.Context context = contextSupplier.get();
        context.enqueueWork(() -> {
            ServerPlayer player = context.getSender();
            if (player == null) {
                return;
            }
            ManualUnlockState.checkUserManualTrigger(player);
            boolean isOp = player.hasPermissions(2);
            if (!isOp && !ManualUnlockState.hasUnlockedUserManual(player)) {
                player.displayClientMessage(Component.literal("好像遗忘了某些记忆……").withStyle(ChatFormatting.GRAY), true);
                return;
            }
            Entity entity = player.level().getEntity(packet.entityId());
            if (!(entity instanceof LivingEntity target) || !PetProgress.isEnabled(target)) {
                player.displayClientMessage(Component.literal("准心目标不是你可操作的 CTF 宠物。"), true);
                return;
            }
            PetProgress progress = PetProgress.get(target);
            PetProgress.AccessMode viewerPermission = progress.permissionFor(player.getUUID());
            if (!isOp && viewerPermission == PetProgress.AccessMode.SECRET) {
                player.displayClientMessage(Component.literal("它已被主人设置为保密").withStyle(ChatFormatting.GRAY), true);
                return;
            }
            String entityTypeStr = net.minecraftforge.registries.ForgeRegistries.ENTITY_TYPES.getKey(target.getType()).toString();
            PetEquipmentState equipment = PetEquipmentState.get(target);

            // Build gui_tabs SNBT string from pet settings (compound format: tab_id -> tab_data)
            String guiSnbt = "";
            String nickname = "";
            CtfSnbtConfig.PetSetting setting = CtfSnbtConfig.petSetting(entityTypeStr);
            if (setting != null) {
                ModuleInventoryState.syncToGuiTabs(target, setting.guiTabs());
                nickname = setting.nickname();
                CompoundTag tabCompound = new CompoundTag();
                net.minecraft.nbt.ListTag guiTabs = setting.guiTabs();
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
            } else {
                // No saved pet setting yet — generate default nickname from translated entity name
                nickname = "[CTF" + Component.translatable(target.getType().getDescriptionId()).getString() + "]";
            }

            String baseName = CtfSnbtConfig.getCtfBaseName(target);
            String ownerName = PetDisplayNames.resolveOwnerName(player, progress.ownerPlayerUuid());

            float currentHealth = target.getHealth();
            float maxHealth = target.getMaxHealth();
            float armor = target.getArmorValue();

            // Read evolution points from data_variables (single source of truth)
            String evoStr = DataVariables.get(target, "EvolutionPoints");
            int evoPoints = DataVariables.parseDecimal(evoStr).intValue();
            CtfNetwork.CHANNEL.send(PacketDistributor.PLAYER.with(() -> player),
                    new S2COpenPetScreenPacket(target.getId(), entityTypeStr, evoPoints,
                            progress.unlockedSkills(), equipment.summary(), isOp,
                            progress.ownerPlayerUuid(), ownerName, viewerPermission.snbtKey(),
                            guiSnbt, DataVariables.toCompound(target), ModuleInventoryState.toCompound(target),
                            CtfSnbtConfig.uiSettings(), nickname, baseName,
                            currentHealth, maxHealth, armor));
            CustomTamingFramework.LOGGER.info("[CTF_SELFTEST] pet_gui.shift_t_opens_enabled_pet=PASS");
        });
        context.setPacketHandled(true);
    }
}
