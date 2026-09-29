package com.example.customtamingframework.pet;

public record PetPermissionEntry(String playerUuid, String playerName, boolean online,
                                 PetProgress.AccessMode permission, boolean owner) {
}
