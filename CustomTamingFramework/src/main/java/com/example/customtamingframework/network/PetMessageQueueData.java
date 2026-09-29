package com.example.customtamingframework.network;

import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.StringTag;
import net.minecraft.network.chat.Component;
import net.minecraft.server.MinecraftServer;
import net.minecraft.world.level.saveddata.SavedData;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

final class PetMessageQueueData extends SavedData {
    private static final String NAME = "ctf_pet_message_queue";

    private final Map<String, List<String>> pendingMessages = new HashMap<>();

    static PetMessageQueueData get(MinecraftServer server) {
        return server.overworld().getDataStorage().computeIfAbsent(
                PetMessageQueueData::load,
                PetMessageQueueData::new,
                NAME
        );
    }

    static PetMessageQueueData load(CompoundTag tag) {
        PetMessageQueueData data = new PetMessageQueueData();
        if (tag == null) {
            return data;
        }
        for (String key : tag.getAllKeys()) {
            if (key == null || key.isBlank() || !tag.contains(key, 9)) {
                continue;
            }
            List<String> values = new ArrayList<>();
            ListTag list = tag.getList(key, 8);
            for (int i = 0; i < list.size(); i++) {
                String raw = list.getString(i);
                if (raw != null && !raw.isBlank()) {
                    values.add(raw);
                }
            }
            if (!values.isEmpty()) {
                data.pendingMessages.put(key, values);
            }
        }
        return data;
    }

    void enqueue(UUID playerUuid, Component message) {
        if (playerUuid == null || message == null) {
            return;
        }
        String key = playerUuid.toString();
        if (key.isBlank()) {
            return;
        }
        pendingMessages.computeIfAbsent(key, ignored -> new ArrayList<>())
                .add(Component.Serializer.toJson(message));
        setDirty();
    }

    List<Component> drain(UUID playerUuid) {
        if (playerUuid == null) {
            return List.of();
        }
        String key = playerUuid.toString();
        List<String> raw = pendingMessages.remove(key);
        if (raw == null || raw.isEmpty()) {
            return List.of();
        }
        List<Component> result = new ArrayList<>();
        for (String entry : raw) {
            if (entry == null || entry.isBlank()) {
                continue;
            }
            try {
                Component component = Component.Serializer.fromJson(entry);
                if (component != null) {
                    result.add(component);
                }
            } catch (RuntimeException ignored) {
                result.add(Component.literal(entry));
            }
        }
        setDirty();
        return List.copyOf(result);
    }

    @Override
    public CompoundTag save(CompoundTag tag) {
        for (Map.Entry<String, List<String>> entry : pendingMessages.entrySet()) {
            ListTag list = new ListTag();
            for (String raw : entry.getValue()) {
                list.add(StringTag.valueOf(raw));
            }
            tag.put(entry.getKey(), list);
        }
        return tag;
    }
}
