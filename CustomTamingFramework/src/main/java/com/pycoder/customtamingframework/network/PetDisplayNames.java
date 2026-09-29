package com.pycoder.customtamingframework.network;

import net.minecraft.server.level.ServerPlayer;

import java.util.Optional;
import java.util.UUID;

final class PetDisplayNames {
    private PetDisplayNames() {
    }

    static String resolvePlayerName(ServerPlayer viewer, String playerUuid) {
        if (playerUuid == null || playerUuid.isBlank()) {
            return "";
        }
        try {
            UUID uuid = UUID.fromString(playerUuid.trim());
            if (viewer != null) {
                if (viewer.getUUID().equals(uuid)) {
                    String localName = viewer.getGameProfile().getName();
                    if (localName != null && !localName.isBlank()) {
                        return localName;
                    }
                }
                if (viewer.getServer() != null) {
                    Optional<String> cached = viewer.getServer().getProfileCache().get(uuid)
                            .map(profile -> profile.getName() == null ? "" : profile.getName());
                    if (cached.isPresent() && !cached.get().isBlank()) {
                        return cached.get();
                    }
                    ServerPlayer online = viewer.getServer().getPlayerList().getPlayer(uuid);
                    if (online != null) {
                        String name = online.getGameProfile().getName();
                        if (name != null && !name.isBlank()) {
                            return name;
                        }
                    }
                }
            }
        } catch (IllegalArgumentException ignored) {
            return playerUuid;
        }
        return playerUuid;
    }

    static String resolveOwnerName(ServerPlayer viewer, String ownerUuid) {
        return resolvePlayerName(viewer, ownerUuid);
    }
}
