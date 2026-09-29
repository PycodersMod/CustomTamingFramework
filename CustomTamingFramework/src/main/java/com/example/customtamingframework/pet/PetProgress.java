package com.example.customtamingframework.pet;

import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.StringTag;
import net.minecraft.world.entity.LivingEntity;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;
import java.util.UUID;

public record PetProgress(boolean ctfEnabled, String ownerPlayerUuid, AccessMode defaultPermission,
                          Map<String, AccessMode> permissions, List<String> unlockedSkills) {
    private static final String ROOT = "CustomTamingFramework";
    private static final String ENABLED = "CtfEnabled";
    private static final String OWNER_UUID = "OwnerPlayerUuid";
    private static final String LEGACY_OWNER_UUID = "ActivatedByPlayerUuid";
    private static final String ACCESS_MODE = "PetAccessMode";
    private static final String PERMISSIONS = "PetPermissions";
    private static final String SKILLS = "UnlockedSkills";
    private static final String SPECIAL_CONSUMPTION = "SpecialFoodConsumption";

    public enum AccessMode {
        OWNER("owner", "主人"),
        READ_ONLY("readonly", "只读"),
        EDITABLE("editable", "可编辑"),
        SECRET("secret", "保密");

        private final String snbtKey;
        private final String displayName;

        AccessMode(String snbtKey, String displayName) {
            this.snbtKey = snbtKey;
            this.displayName = displayName;
        }

        public String snbtKey() {
            return snbtKey;
        }

        public String displayName() {
            return displayName;
        }

        public static AccessMode fromString(String value) {
            if (value != null) {
                for (AccessMode mode : values()) {
                    if (mode.snbtKey.equalsIgnoreCase(value)
                            || mode.name().equalsIgnoreCase(value)
                            || mode.displayName.equalsIgnoreCase(value)) {
                        return mode;
                    }
                }
            }
            return READ_ONLY;
        }
    }

    public PetProgress {
        ownerPlayerUuid = ownerPlayerUuid == null ? "" : ownerPlayerUuid.trim();
        defaultPermission = defaultPermission == null ? AccessMode.SECRET : defaultPermission;
        Map<String, AccessMode> nextPermissions = new TreeMap<>();
        if (permissions != null) {
            for (Map.Entry<String, AccessMode> entry : permissions.entrySet()) {
                String key = entry.getKey() == null ? "" : entry.getKey().trim();
                AccessMode mode = entry.getValue() == null ? AccessMode.SECRET : entry.getValue();
                if (!key.isBlank()) {
                    nextPermissions.put(key, mode);
                }
            }
        }
        if (!ownerPlayerUuid.isBlank()) {
            nextPermissions.put(ownerPlayerUuid, AccessMode.OWNER);
        }
        permissions = Collections.unmodifiableMap(nextPermissions);
        unlockedSkills = unlockedSkills == null ? List.of() : List.copyOf(unlockedSkills);
    }

    public static PetProgress enabled(UUID playerUuid) {
        String owner = playerUuid == null ? "" : playerUuid.toString();
        Map<String, AccessMode> permissions = new TreeMap<>();
        if (!owner.isBlank()) {
            permissions.put(owner, AccessMode.OWNER);
        }
        return new PetProgress(true, owner, AccessMode.SECRET, permissions, List.of());
    }

    public static boolean isEnabled(LivingEntity entity) {
        return get(entity).ctfEnabled();
    }

    public static PetProgress get(LivingEntity entity) {
        CompoundTag root = entity.getPersistentData().getCompound(ROOT);
        List<String> skills = new ArrayList<>();
        ListTag list = root.getList(SKILLS, 8);
        for (int i = 0; i < list.size(); i++) {
            skills.add(list.getString(i));
        }
        String owner = root.contains(OWNER_UUID) ? root.getString(OWNER_UUID) : root.getString(LEGACY_OWNER_UUID);
        AccessMode defaultPermission = AccessMode.fromString(root.getString(ACCESS_MODE));
        Map<String, AccessMode> permissions = new TreeMap<>();
        CompoundTag permissionTag = root.getCompound(PERMISSIONS);
        for (String key : permissionTag.getAllKeys()) {
            permissions.put(key, AccessMode.fromString(permissionTag.getString(key)));
        }
        return new PetProgress(root.getBoolean(ENABLED), owner, defaultPermission, permissions, List.copyOf(skills));
    }

    public static void save(LivingEntity entity, PetProgress progress) {
        CompoundTag root = new CompoundTag();
        root.putBoolean(ENABLED, progress.ctfEnabled());
        root.putString(OWNER_UUID, progress.ownerPlayerUuid());
        root.putString(LEGACY_OWNER_UUID, progress.ownerPlayerUuid());
        root.putString(ACCESS_MODE, progress.defaultPermission().snbtKey());
        CompoundTag permissions = new CompoundTag();
        for (Map.Entry<String, AccessMode> entry : progress.permissions().entrySet()) {
            permissions.putString(entry.getKey(), entry.getValue().snbtKey());
        }
        root.put(PERMISSIONS, permissions);
        ListTag skills = new ListTag();
        for (String skill : progress.unlockedSkills()) {
            skills.add(StringTag.valueOf(skill));
        }
        root.put(SKILLS, skills);
        entity.getPersistentData().put(ROOT, root);
    }

    public AccessMode accessMode() {
        return defaultPermission;
    }

    public AccessMode permissionFor(UUID playerUuid) {
        if (playerUuid == null) {
            return defaultPermission;
        }
        String key = playerUuid.toString();
        AccessMode explicit = permissions.get(key);
        if (explicit != null) {
            return explicit;
        }
        if (!ownerPlayerUuid.isBlank() && ownerPlayerUuid.equals(key)) {
            return AccessMode.OWNER;
        }
        return defaultPermission;
    }

    public PetProgress unlockSkill(String skillId) {
        if (unlockedSkills.contains(skillId)) {
            return this;
        }
        List<String> next = new ArrayList<>(unlockedSkills);
        next.add(skillId);
        return new PetProgress(ctfEnabled, ownerPlayerUuid, defaultPermission, permissions, List.copyOf(next));
    }

    public PetProgress withDefaultPermission(AccessMode nextDefaultPermission) {
        return new PetProgress(ctfEnabled, ownerPlayerUuid, nextDefaultPermission, permissions, unlockedSkills);
    }

    public PetProgress withPermission(UUID playerUuid, AccessMode permission) {
        if (playerUuid == null || permission == null) {
            return this;
        }
        String key = playerUuid.toString();
        Map<String, AccessMode> next = new TreeMap<>(permissions);
        if (owns(playerUuid)) {
            next.put(key, AccessMode.OWNER);
        } else if (permission == AccessMode.OWNER) {
            next.put(key, AccessMode.SECRET);
        } else {
            next.put(key, permission);
        }
        return new PetProgress(ctfEnabled, ownerPlayerUuid, defaultPermission, next, unlockedSkills);
    }

    public PetProgress ensureKnownPlayer(UUID playerUuid) {
        if (playerUuid == null) {
            return this;
        }
        String key = playerUuid.toString();
        if (key.isBlank() || key.equals(ownerPlayerUuid) || permissions.containsKey(key)) {
            return this;
        }
        Map<String, AccessMode> next = new TreeMap<>(permissions);
        next.put(key, AccessMode.SECRET);
        return new PetProgress(ctfEnabled, ownerPlayerUuid, defaultPermission, next, unlockedSkills);
    }

    public PetProgress ensureKnownPlayers(Collection<UUID> playerUuids) {
        if (playerUuids == null || playerUuids.isEmpty()) {
            return this;
        }
        Map<String, AccessMode> next = new TreeMap<>(permissions);
        boolean changed = false;
        for (UUID playerUuid : playerUuids) {
            if (playerUuid == null) {
                continue;
            }
            String key = playerUuid.toString();
            if (key.isBlank() || key.equals(ownerPlayerUuid) || next.containsKey(key)) {
                continue;
            }
            next.put(key, AccessMode.SECRET);
            changed = true;
        }
        if (!changed) {
            return this;
        }
        return new PetProgress(ctfEnabled, ownerPlayerUuid, defaultPermission, next, unlockedSkills);
    }

    public PetProgress transferOwner(UUID newOwnerUuid) {
        if (newOwnerUuid == null) {
            return this;
        }
        String newOwner = newOwnerUuid.toString();
        if (newOwner.isBlank() || newOwner.equals(ownerPlayerUuid)) {
            return this;
        }
        Map<String, AccessMode> next = new TreeMap<>(permissions);
        if (!ownerPlayerUuid.isBlank()) {
            next.put(ownerPlayerUuid, AccessMode.EDITABLE);
        }
        next.put(newOwner, AccessMode.OWNER);
        return new PetProgress(ctfEnabled, newOwner, defaultPermission, next, unlockedSkills);
    }

    public boolean owns(UUID playerUuid) {
        if (playerUuid == null || ownerPlayerUuid == null || ownerPlayerUuid.isBlank()) {
            return false;
        }
        return ownerPlayerUuid.equals(playerUuid.toString());
    }

    // ── Special item/food consumption tracking ──

    /** Record consumption of a special item/food. Increments count if already tracked. */
    public static void recordConsumption(LivingEntity entity, String itemId) {
        CompoundTag root = entity.getPersistentData().getCompound(ROOT);
        ListTag consumption = root.getList(SPECIAL_CONSUMPTION, 10);
        // Check if this item already has a consumption entry
        CompoundTag entry = null;
        for (int i = 0; i < consumption.size(); i++) {
            CompoundTag e = consumption.getCompound(i);
            if (e.getString("id").equals(itemId)) {
                entry = e;
                break;
            }
        }
        if (entry != null) {
            entry.putInt("count", entry.getInt("count") + 1);
        } else {
            CompoundTag newEntry = new CompoundTag();
            newEntry.putString("id", itemId);
            newEntry.putInt("count", 1);
            consumption.add(newEntry);
        }
        // Re-apply to root
        CompoundTag newRoot = root.copy();
        newRoot.put(SPECIAL_CONSUMPTION, consumption);
        entity.getPersistentData().put(ROOT, newRoot);
    }

    /** Remove all CTF state from this entity. Resets custom display name to vanilla. */
    public static void remove(LivingEntity entity) {
        entity.getPersistentData().remove(ROOT);
        if (entity.hasCustomName()) {
            entity.setCustomName(null);
            entity.setCustomNameVisible(false);
        }
    }
}
