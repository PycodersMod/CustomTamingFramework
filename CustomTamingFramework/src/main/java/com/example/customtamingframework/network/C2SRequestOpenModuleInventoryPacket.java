package com.example.customtamingframework.network;

import com.example.customtamingframework.CustomTamingFramework;
import com.example.customtamingframework.config.CtfSnbtConfig;
import com.example.customtamingframework.menu.ModuleInventoryMenu;
import com.example.customtamingframework.manual.ManualUnlockState;
import com.example.customtamingframework.pet.ModuleInventoryState;
import com.example.customtamingframework.pet.PetAccessResolver;
import com.example.customtamingframework.pet.PetProgress;
import net.minecraft.ChatFormatting;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.MenuProvider;
import net.minecraft.world.item.ItemStack;
import net.minecraftforge.network.NetworkEvent;
import net.minecraft.world.SimpleMenuProvider;
import net.minecraftforge.network.NetworkHooks;

import java.util.List;
import java.util.function.Supplier;

public record C2SRequestOpenModuleInventoryPacket(int entityId, String moduleId) {
    public static void encode(C2SRequestOpenModuleInventoryPacket packet, FriendlyByteBuf buffer) {
        buffer.writeVarInt(packet.entityId());
        buffer.writeUtf(packet.moduleId() != null ? packet.moduleId() : "");
    }

    public static C2SRequestOpenModuleInventoryPacket decode(FriendlyByteBuf buffer) {
        return new C2SRequestOpenModuleInventoryPacket(buffer.readVarInt(), buffer.readUtf());
    }

    public static void handle(C2SRequestOpenModuleInventoryPacket packet, Supplier<NetworkEvent.Context> contextSupplier) {
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
                player.displayClientMessage(Component.literal("准心目标不是你可操作的 CTF 宠物。").withStyle(ChatFormatting.GRAY), true);
                return;
            }
            if (!PetAccessResolver.canOpenPetScreen(player, target)) {
                player.displayClientMessage(Component.literal("它已被主人设置为保密").withStyle(ChatFormatting.GRAY), true);
                return;
            }

            String entityTypeId = net.minecraftforge.registries.ForgeRegistries.ENTITY_TYPES.getKey(target.getType()).toString();
            CtfSnbtConfig.PetSetting setting = CtfSnbtConfig.petSetting(entityTypeId);
            if (setting == null) {
                player.displayClientMessage(Component.literal("该宠物没有可用的物品栏模块。").withStyle(ChatFormatting.GRAY), true);
                return;
            }
            ModuleInventoryState.syncToGuiTabs(target, setting.guiTabs());

            net.minecraft.nbt.ListTag guiTabs = setting.guiTabs();
            net.minecraft.nbt.CompoundTag moduleConfig = null;
            for (int i = 0; i < guiTabs.size(); i++) {
                net.minecraft.nbt.CompoundTag tab = guiTabs.getCompound(i);
                if (!tab.contains("modules", net.minecraft.nbt.Tag.TAG_LIST)) {
                    continue;
                }
                net.minecraft.nbt.ListTag modules = tab.getList("modules", net.minecraft.nbt.Tag.TAG_COMPOUND);
                for (int m = 0; m < modules.size(); m++) {
                    net.minecraft.nbt.CompoundTag module = modules.getCompound(m);
                    if (!"inventory".equalsIgnoreCase(module.getString("type"))) {
                        continue;
                    }
                    net.minecraft.nbt.CompoundTag config = module.getCompound("config");
                    String id = config.getString("module_id");
                    if (packet.moduleId().equals(id)) {
                        moduleConfig = config.copy();
                        break;
                    }
                }
                if (moduleConfig != null) {
                    break;
                }
            }
            if (moduleConfig == null) {
                player.displayClientMessage(Component.literal("这个物品栏模块不存在或已经被删除。").withStyle(ChatFormatting.GRAY), true);
                return;
            }

            final net.minecraft.nbt.CompoundTag openConfig = moduleConfig.copy();
            final List<ItemStack> stacks = ModuleInventoryState.get(target, packet.moduleId(), ModuleInventoryState.slotCount(openConfig));
            PetProgress progress = PetProgress.get(target);
            PetProgress.AccessMode viewerPermission = progress.permissionFor(player.getUUID());
            final boolean editable = isOp || viewerPermission != PetProgress.AccessMode.READ_ONLY;
            MenuProvider provider = new SimpleMenuProvider((windowId, inv, p) ->
                    new ModuleInventoryMenu(windowId, inv, target, openConfig, stacks, editable),
                    Component.literal(ModuleInventoryState.displayName(openConfig)));
            NetworkHooks.openScreen(player, provider, buf -> {
                buf.writeVarInt(target.getId());
                buf.writeNbt(openConfig);
                buf.writeVarInt(stacks.size());
                for (ItemStack stack : stacks) {
                    buf.writeBoolean(stack != null && !stack.isEmpty());
                    if (stack != null && !stack.isEmpty()) {
                        buf.writeNbt(stack.save(new net.minecraft.nbt.CompoundTag()));
                    }
                }
                buf.writeBoolean(editable);
            });
            CustomTamingFramework.LOGGER.info("[CTF_SELFTEST] module_inventory.open_screen=PASS entity={} module={}", packet.entityId(), packet.moduleId());
        });
        context.setPacketHandled(true);
    }
}
