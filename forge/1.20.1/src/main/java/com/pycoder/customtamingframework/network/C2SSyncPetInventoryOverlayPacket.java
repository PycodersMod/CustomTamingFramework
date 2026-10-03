package com.pycoder.customtamingframework.network;

import com.pycoder.customtamingframework.CustomTamingFramework;
import com.pycoder.customtamingframework.pet.ModuleInventoryState;
import com.pycoder.customtamingframework.pet.PetAccessResolver;
import com.pycoder.customtamingframework.pet.PetProgress;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.ItemStack;
import net.minecraftforge.network.NetworkEvent;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Supplier;

public record C2SSyncPetInventoryOverlayPacket(int entityId,
                                               List<ItemStack> playerMain,
                                               List<ItemStack> armor,
                                               List<ItemStack> offhand,
                                               List<ItemStack> enderChest,
                                               Map<String, List<ItemStack>> moduleInventories,
                                               ItemStack carried) {
    public static void encode(C2SSyncPetInventoryOverlayPacket packet, FriendlyByteBuf buffer) {
        buffer.writeVarInt(packet.entityId());
        writeStacks(buffer, packet.playerMain());
        writeStacks(buffer, packet.armor());
        writeStacks(buffer, packet.offhand());
        writeStacks(buffer, packet.enderChest());
        buffer.writeVarInt(packet.moduleInventories() != null ? packet.moduleInventories().size() : 0);
        if (packet.moduleInventories() != null) {
            for (Map.Entry<String, List<ItemStack>> entry : packet.moduleInventories().entrySet()) {
                buffer.writeUtf(entry.getKey() != null ? entry.getKey() : "");
                writeStacks(buffer, entry.getValue());
            }
        }
        buffer.writeBoolean(packet.carried() != null && !packet.carried().isEmpty());
        if (packet.carried() != null && !packet.carried().isEmpty()) {
            buffer.writeNbt(packet.carried().save(new CompoundTag()));
        }
    }

    public static C2SSyncPetInventoryOverlayPacket decode(FriendlyByteBuf buffer) {
        int entityId = buffer.readVarInt();
        List<ItemStack> playerMain = readStacks(buffer);
        List<ItemStack> armor = readStacks(buffer);
        List<ItemStack> offhand = readStacks(buffer);
        List<ItemStack> enderChest = readStacks(buffer);
        int moduleCount = buffer.readVarInt();
        Map<String, List<ItemStack>> moduleInventories = new LinkedHashMap<>();
        for (int i = 0; i < moduleCount; i++) {
            String moduleId = buffer.readUtf();
            moduleInventories.put(moduleId, readStacks(buffer));
        }
        ItemStack carried = buffer.readBoolean() ? ItemStack.of(buffer.readNbt()) : ItemStack.EMPTY;
        return new C2SSyncPetInventoryOverlayPacket(entityId, playerMain, armor, offhand, enderChest, moduleInventories, carried);
    }

    public static void handle(C2SSyncPetInventoryOverlayPacket packet, Supplier<NetworkEvent.Context> contextSupplier) {
        NetworkEvent.Context context = contextSupplier.get();
        context.enqueueWork(() -> {
            ServerPlayer player = context.getSender();
            if (player == null) {
                return;
            }
            Entity entity = player.level().getEntity(packet.entityId());
            if (!(entity instanceof LivingEntity target) || !PetProgress.isEnabled(target)) {
                return;
            }
            if (!player.hasPermissions(2) && !PetAccessResolver.canOpenPetScreen(player, target)) {
                return;
            }

            applyInventory(player.getInventory().items, packet.playerMain());
            applyInventory(player.getInventory().armor, packet.armor());
            applyInventory(player.getInventory().offhand, packet.offhand());
            try {
                net.minecraft.world.Container enderChest = player.getEnderChestInventory();
                applyInventory(enderChest, packet.enderChest());
            } catch (Throwable ignored) {
            }
            ModuleInventoryState.saveAll(target, packet.moduleInventories());

            if (packet.carried() != null && !packet.carried().isEmpty()) {
                player.drop(packet.carried().copy(), false);
            }

            try {
                player.inventoryMenu.broadcastChanges();
            } catch (Exception ignored) {
                try {
                    player.containerMenu.broadcastChanges();
                } catch (Exception ignoredToo) {
                }
            }
            CustomTamingFramework.LOGGER.info("[CTF_SELFTEST] inventory_overlay.sync=PASS entity={}", packet.entityId());
        });
        context.setPacketHandled(true);
    }

    private static void writeStacks(FriendlyByteBuf buffer, List<ItemStack> stacks) {
        List<ItemStack> safe = stacks == null ? List.of() : stacks;
        buffer.writeVarInt(safe.size());
        for (ItemStack stack : safe) {
            boolean present = stack != null && !stack.isEmpty();
            buffer.writeBoolean(present);
            if (present) {
                buffer.writeNbt(stack.save(new CompoundTag()));
            }
        }
    }

    private static List<ItemStack> readStacks(FriendlyByteBuf buffer) {
        int size = buffer.readVarInt();
        List<ItemStack> stacks = new ArrayList<>(size);
        for (int i = 0; i < size; i++) {
            boolean present = buffer.readBoolean();
            stacks.add(present ? ItemStack.of(buffer.readNbt()) : ItemStack.EMPTY);
        }
        return stacks;
    }

    private static void applyInventory(List<ItemStack> target, List<ItemStack> source) {
        if (target == null || source == null) {
            return;
        }
        int max = Math.min(target.size(), source.size());
        for (int i = 0; i < max; i++) {
            ItemStack stack = source.get(i);
            target.set(i, stack == null ? ItemStack.EMPTY : stack.copy());
        }
    }

    private static void applyInventory(net.minecraft.world.Container target, List<ItemStack> source) {
        if (target == null || source == null) {
            return;
        }
        int max = Math.min(target.getContainerSize(), source.size());
        for (int i = 0; i < max; i++) {
            ItemStack stack = source.get(i);
            target.setItem(i, stack == null ? ItemStack.EMPTY : stack.copy());
        }
        target.setChanged();
    }

}
