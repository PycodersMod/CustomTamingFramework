package com.example.customtamingframework.network;

import com.example.customtamingframework.CustomTamingFramework;
import com.example.customtamingframework.manual.ManualUnlockState;
import com.example.customtamingframework.pet.PetProgress;
import net.minecraft.ChatFormatting;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.TamableAnimal;
import net.minecraft.world.entity.animal.horse.AbstractHorse;
import net.minecraftforge.network.NetworkEvent;
import net.minecraftforge.network.PacketDistributor;

import java.util.UUID;
import java.util.function.Supplier;

public record C2STransferPetOwnerPacket(int entityId, String targetPlayerUuid, boolean broadcast) {
    public static void encode(C2STransferPetOwnerPacket packet, FriendlyByteBuf buffer) {
        buffer.writeVarInt(packet.entityId());
        buffer.writeUtf(packet.targetPlayerUuid() != null ? packet.targetPlayerUuid() : "");
        buffer.writeBoolean(packet.broadcast());
    }

    public static C2STransferPetOwnerPacket decode(FriendlyByteBuf buffer) {
        return new C2STransferPetOwnerPacket(buffer.readVarInt(), buffer.readUtf(), buffer.readBoolean());
    }

    public static void handle(C2STransferPetOwnerPacket packet, Supplier<NetworkEvent.Context> contextSupplier) {
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

            PetProgress progress = PetProgress.get(target);
            PetProgress.AccessMode viewerPermission = progress.permissionFor(player.getUUID());
            if (!isOp && viewerPermission != PetProgress.AccessMode.OWNER) {
                player.displayClientMessage(Component.literal("你不是它的主人").withStyle(ChatFormatting.GRAY), true);
                return;
            }

            UUID targetUuid = parseUuid(packet.targetPlayerUuid());
            if (targetUuid == null) {
                player.displayClientMessage(Component.literal("转赠目标无效。").withStyle(ChatFormatting.RED), true);
                return;
            }

            PetProgress updated = progress.transferOwner(targetUuid);
            PetProgress.save(target, updated);
            syncVanillaOwner(target, targetUuid);
            PetBroadcasts.notifyTransfer(player, target, progress, updated, targetUuid, packet.broadcast());

            CtfNetwork.CHANNEL.send(PacketDistributor.PLAYER.with(() -> player),
                    S2CPetPermissionSnapshotPacket.build(player, target, false));
            CustomTamingFramework.LOGGER.info("[CTF_SELFTEST] pet_permission.transfer_owner=PASS entity={} target={}",
                    target.getType(), targetUuid);
        });
        context.setPacketHandled(true);
    }

    private static void syncVanillaOwner(LivingEntity target, UUID ownerUuid) {
        if (target instanceof TamableAnimal tamable) {
            tamable.setTame(true);
            tamable.setOwnerUUID(ownerUuid);
        } else if (target instanceof AbstractHorse horse) {
            horse.setTamed(true);
            horse.setOwnerUUID(ownerUuid);
        }
    }

    private static UUID parseUuid(String value) {
        if (value == null || value.isBlank()) {
            return null;
        }
        try {
            return UUID.fromString(value.trim());
        } catch (IllegalArgumentException ignored) {
            return null;
        }
    }
}
