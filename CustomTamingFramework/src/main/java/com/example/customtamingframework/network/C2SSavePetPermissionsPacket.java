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
import net.minecraftforge.network.NetworkEvent;
import net.minecraftforge.network.PacketDistributor;

import java.util.Map;
import java.util.TreeMap;
import java.util.UUID;
import java.util.function.Supplier;

public record C2SSavePetPermissionsPacket(int entityId, Map<String, String> permissions) {
    public static void encode(C2SSavePetPermissionsPacket packet, FriendlyByteBuf buffer) {
        buffer.writeVarInt(packet.entityId());
        buffer.writeVarInt(packet.permissions() != null ? packet.permissions().size() : 0);
        if (packet.permissions() != null) {
            for (Map.Entry<String, String> entry : packet.permissions().entrySet()) {
                buffer.writeUtf(entry.getKey() != null ? entry.getKey() : "");
                buffer.writeUtf(entry.getValue() != null ? entry.getValue() : "");
            }
        }
    }

    public static C2SSavePetPermissionsPacket decode(FriendlyByteBuf buffer) {
        int entityId = buffer.readVarInt();
        int size = buffer.readVarInt();
        Map<String, String> permissions = new TreeMap<>();
        for (int i = 0; i < size; i++) {
            permissions.put(buffer.readUtf(), buffer.readUtf());
        }
        return new C2SSavePetPermissionsPacket(entityId, permissions);
    }

    public static void handle(C2SSavePetPermissionsPacket packet, Supplier<NetworkEvent.Context> contextSupplier) {
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
            if (!isOp && viewerPermission == PetProgress.AccessMode.SECRET) {
                player.displayClientMessage(Component.literal("它已被主人设置为保密").withStyle(ChatFormatting.GRAY), true);
                return;
            }

            Map<String, PetProgress.AccessMode> next = new TreeMap<>(progress.permissions());
            for (Map.Entry<String, String> entry : packet.permissions().entrySet()) {
                UUID rowUuid = parseUuid(entry.getKey());
                if (rowUuid == null) {
                    continue;
                }
                String key = rowUuid.toString();
                if (progress.owns(rowUuid)) {
                    next.put(key, PetProgress.AccessMode.OWNER);
                    continue;
                }
                PetProgress.AccessMode current = progress.permissionFor(rowUuid);
                PetProgress.AccessMode desired = PetProgress.AccessMode.fromString(entry.getValue());
                if (isOp || viewerPermission == PetProgress.AccessMode.OWNER) {
                    if (desired != PetProgress.AccessMode.OWNER) {
                        next.put(key, desired);
                    }
                    continue;
                }
                if (viewerPermission == PetProgress.AccessMode.EDITABLE) {
                    if (current == PetProgress.AccessMode.OWNER || current == PetProgress.AccessMode.EDITABLE) {
                        continue;
                    }
                    if (desired == PetProgress.AccessMode.READ_ONLY || desired == PetProgress.AccessMode.SECRET) {
                        next.put(key, desired);
                    }
                }
            }

            PetProgress updated = new PetProgress(progress.ctfEnabled(), progress.ownerPlayerUuid(), progress.accessMode(), next, progress.unlockedSkills());
            PetProgress.save(target, updated);
            broadcastPermissionChanges(player, target, progress, updated);

            CtfNetwork.CHANNEL.send(PacketDistributor.PLAYER.with(() -> player),
                    S2CPetPermissionSnapshotPacket.build(player, target, false));
            CustomTamingFramework.LOGGER.info("[CTF_SELFTEST] pet_permission.save=PASS entity={} rows={}",
                    target.getType(), packet.permissions().size());
        });
        context.setPacketHandled(true);
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

    private static void broadcastPermissionChanges(ServerPlayer actor, LivingEntity target, PetProgress before, PetProgress after) {
        if (actor == null || target == null || before == null || after == null) {
            return;
        }
        var server = actor.getServer();
        if (server == null) {
            return;
        }
        for (String key : after.permissions().keySet()) {
            UUID rowUuid = parseUuid(key);
            if (rowUuid == null || after.owns(rowUuid)) {
                continue;
            }
            PetProgress.AccessMode beforeMode = before.permissionFor(rowUuid);
            PetProgress.AccessMode afterMode = after.permissionFor(rowUuid);
            if (beforeMode == afterMode) {
                continue;
            }
            PetBroadcasts.notifyPermissionChange(actor, target, before, after, rowUuid);
        }
    }
}
