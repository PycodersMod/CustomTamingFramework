package com.example.customtamingframework.network;

import com.example.customtamingframework.pet.PetPermissionEntry;
import com.example.customtamingframework.pet.PetProgress;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.LivingEntity;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.fml.DistExecutor;
import net.minecraftforge.network.NetworkEvent;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.LinkedHashSet;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.function.Supplier;

public record S2CPetPermissionSnapshotPacket(boolean openGui, int entityId, String ownerPlayerUuid,
                                             String ownerPlayerName, String viewerPermission,
                                             List<PetPermissionEntry> entries) {
    public static void encode(S2CPetPermissionSnapshotPacket packet, FriendlyByteBuf buffer) {
        buffer.writeBoolean(packet.openGui());
        buffer.writeVarInt(packet.entityId());
        buffer.writeUtf(packet.ownerPlayerUuid() != null ? packet.ownerPlayerUuid() : "");
        buffer.writeUtf(packet.ownerPlayerName() != null ? packet.ownerPlayerName() : "");
        buffer.writeUtf(packet.viewerPermission() != null ? packet.viewerPermission() : "");
        buffer.writeVarInt(packet.entries().size());
        for (PetPermissionEntry entry : packet.entries()) {
            buffer.writeUtf(entry.playerUuid() != null ? entry.playerUuid() : "");
            buffer.writeUtf(entry.playerName() != null ? entry.playerName() : "");
            buffer.writeBoolean(entry.online());
            buffer.writeUtf(entry.permission() != null ? entry.permission().snbtKey() : PetProgress.AccessMode.SECRET.snbtKey());
            buffer.writeBoolean(entry.owner());
        }
    }

    public static S2CPetPermissionSnapshotPacket decode(FriendlyByteBuf buffer) {
        boolean openGui = buffer.readBoolean();
        int entityId = buffer.readVarInt();
        String ownerPlayerUuid = buffer.readUtf();
        String ownerPlayerName = buffer.readUtf();
        String viewerPermission = buffer.readUtf();
        int size = buffer.readVarInt();
        List<PetPermissionEntry> entries = new ArrayList<>();
        for (int i = 0; i < size; i++) {
            String playerUuid = buffer.readUtf();
            String playerName = buffer.readUtf();
            boolean online = buffer.readBoolean();
            PetProgress.AccessMode permission = PetProgress.AccessMode.fromString(buffer.readUtf());
            boolean owner = buffer.readBoolean();
            entries.add(new PetPermissionEntry(playerUuid, playerName, online, permission, owner));
        }
        return new S2CPetPermissionSnapshotPacket(openGui, entityId, ownerPlayerUuid, ownerPlayerName,
                viewerPermission, List.copyOf(entries));
    }

    public static S2CPetPermissionSnapshotPacket build(ServerPlayer viewer, LivingEntity target, boolean openGui) {
        PetProgress progress = PetProgress.get(target);
        PetProgress updated = ensureKnownPlayers(viewer, progress);
        if (updated != progress) {
            PetProgress.save(target, updated);
            progress = updated;
        }
        String ownerPlayerUuid = progress.ownerPlayerUuid();
        String ownerPlayerName = PetDisplayNames.resolvePlayerName(viewer, ownerPlayerUuid);
        String viewerPermission = progress.permissionFor(viewer.getUUID()).snbtKey();
        List<PetPermissionEntry> entries = buildEntries(viewer, progress);
        return new S2CPetPermissionSnapshotPacket(openGui, target.getId(), ownerPlayerUuid, ownerPlayerName,
                viewerPermission, entries);
    }

    private static PetProgress ensureKnownPlayers(ServerPlayer viewer, PetProgress progress) {
        if (viewer == null || viewer.getServer() == null) {
            return progress;
        }
        List<UUID> uuids = new ArrayList<>();
        if (progress.ownerPlayerUuid() != null && !progress.ownerPlayerUuid().isBlank()) {
            UUID ownerId = safeParseUuid(progress.ownerPlayerUuid());
            if (ownerId != null) {
                uuids.add(ownerId);
            }
        }
        for (String key : progress.permissions().keySet()) {
            UUID playerUuid = safeParseUuid(key);
            if (playerUuid != null) {
                uuids.add(playerUuid);
            }
        }
        viewer.getServer().getPlayerList().getPlayers().forEach(player -> uuids.add(player.getUUID()));
        return progress.ensureKnownPlayers(uuids);
    }

    private static List<PetPermissionEntry> buildEntries(ServerPlayer viewer, PetProgress progress) {
        Map<String, Boolean> onlineMap = collectOnlineState(viewer);
        String ownerUuid = progress.ownerPlayerUuid();
        Set<String> allUuids = new LinkedHashSet<>();
        if (!ownerUuid.isBlank()) {
            allUuids.add(ownerUuid);
        }
        allUuids.addAll(progress.permissions().keySet());
        allUuids.addAll(onlineMap.keySet());

        PetPermissionEntry ownerEntry = null;
        List<PetPermissionEntry> onlineEntries = new ArrayList<>();
        List<PetPermissionEntry> offlineEntries = new ArrayList<>();

        for (String uuid : allUuids) {
            if (uuid.isBlank()) {
                continue;
            }
            String name = PetDisplayNames.resolvePlayerName(viewer, uuid);
            boolean owner = !ownerUuid.isBlank() && ownerUuid.equals(uuid);
            boolean online = onlineMap.getOrDefault(uuid, false);
            PetPermissionEntry row = new PetPermissionEntry(uuid, name, online, progress.permissionFor(safeParseUuid(uuid)), owner);
            if (owner) {
                ownerEntry = row;
            } else if (online) {
                onlineEntries.add(row);
            } else {
                offlineEntries.add(row);
            }
        }

        Comparator<PetPermissionEntry> byName = Comparator.comparing(
                entry -> entry.playerName() == null ? "" : entry.playerName().toLowerCase(Locale.ROOT));
        onlineEntries.sort(byName);
        offlineEntries.sort(byName);

        List<PetPermissionEntry> result = new ArrayList<>();
        if (ownerEntry != null) {
            result.add(ownerEntry);
        }
        result.addAll(onlineEntries);
        result.addAll(offlineEntries);
        return List.copyOf(result);
    }

    private static Map<String, Boolean> collectOnlineState(ServerPlayer viewer) {
        Map<String, Boolean> online = new LinkedHashMap<>();
        if (viewer == null || viewer.getServer() == null) {
            return online;
        }
        viewer.getServer().getPlayerList().getPlayers().forEach(player ->
                online.put(player.getUUID().toString(), true));
        return online;
    }

    private static UUID safeParseUuid(String value) {
        if (value == null || value.isBlank()) {
            return null;
        }
        try {
            return UUID.fromString(value.trim());
        } catch (IllegalArgumentException ignored) {
            return null;
        }
    }

    public static void handle(S2CPetPermissionSnapshotPacket packet, Supplier<NetworkEvent.Context> contextSupplier) {
        NetworkEvent.Context context = contextSupplier.get();
        DistExecutor.unsafeRunWhenOn(Dist.CLIENT, () -> () -> ClientPacketHandler.syncPetPermissionSnapshot(packet));
        context.setPacketHandled(true);
    }
}
