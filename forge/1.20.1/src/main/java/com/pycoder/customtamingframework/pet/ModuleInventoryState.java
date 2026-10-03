package com.pycoder.customtamingframework.pet;

import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.Tag;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.ItemStack;
import net.minecraftforge.registries.ForgeRegistries;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

/**
 * Runtime inventory storage for inventory modules.
 * Stored on instantiated pets only, under CustomTamingFramework -> ModuleInventories.
 */
public final class ModuleInventoryState {
    private static final String ROOT = "CustomTamingFramework";
    private static final String KEY = "ModuleInventories";
    private static final String FIELD_SIZE = "size";
    private static final String FIELD_ITEMS = "items";
    private static final String FIELD_SLOT = "slot";
    private static final String FIELD_STACK = "stack";

    private ModuleInventoryState() {
    }

    public static Map<String, List<ItemStack>> getAll(LivingEntity entity) {
        Map<String, List<ItemStack>> result = new LinkedHashMap<>();
        if (entity == null) {
            return result;
        }
        CompoundTag root = entity.getPersistentData().getCompound(ROOT);
        CompoundTag inventories = root.getCompound(KEY);
        for (String moduleId : inventories.getAllKeys()) {
            if (moduleId == null || moduleId.isBlank()) {
                continue;
            }
            CompoundTag invTag = inventories.getCompound(moduleId);
            int size = Math.max(0, invTag.contains(FIELD_SIZE) ? invTag.getInt(FIELD_SIZE) : 0);
            result.put(moduleId, readStacks(invTag, size));
        }
        return result;
    }

    public static List<ItemStack> get(LivingEntity entity, String moduleId, int size) {
        if (entity == null || moduleId == null || moduleId.isBlank()) {
            return empty(size);
        }
        CompoundTag root = entity.getPersistentData().getCompound(ROOT);
        CompoundTag inventories = root.getCompound(KEY);
        if (!inventories.contains(moduleId, Tag.TAG_COMPOUND)) {
            return empty(size);
        }
        return resize(readStacks(inventories.getCompound(moduleId), size), size);
    }

    public static void save(LivingEntity entity, String moduleId, List<ItemStack> stacks) {
        if (entity == null || moduleId == null || moduleId.isBlank()) {
            return;
        }
        CompoundTag root = entity.getPersistentData().getCompound(ROOT).copy();
        CompoundTag inventories = root.getCompound(KEY).copy();
        CompoundTag invTag = new CompoundTag();
        List<ItemStack> safeStacks = stacks == null ? List.of() : stacks;
        invTag.putInt(FIELD_SIZE, safeStacks.size());
        invTag.put(FIELD_ITEMS, writeStacks(safeStacks));
        inventories.put(moduleId, invTag);
        root.put(KEY, inventories);
        entity.getPersistentData().put(ROOT, root);
    }

    public static void saveAll(LivingEntity entity, Map<String, List<ItemStack>> inventoriesMap) {
        if (entity == null || inventoriesMap == null) {
            return;
        }
        CompoundTag root = entity.getPersistentData().getCompound(ROOT).copy();
        CompoundTag inventories = new CompoundTag();
        for (Map.Entry<String, List<ItemStack>> entry : inventoriesMap.entrySet()) {
            String moduleId = entry.getKey();
            if (moduleId == null || moduleId.isBlank()) {
                continue;
            }
            List<ItemStack> stacks = entry.getValue();
            List<ItemStack> safeStacks = stacks == null ? List.of() : stacks;
            CompoundTag invTag = new CompoundTag();
            invTag.putInt(FIELD_SIZE, safeStacks.size());
            invTag.put(FIELD_ITEMS, writeStacks(safeStacks));
            inventories.put(moduleId, invTag);
        }
        root.put(KEY, inventories);
        entity.getPersistentData().put(ROOT, root);
    }

    public static void clear(LivingEntity entity, String moduleId, boolean dropItems) {
        if (entity == null || moduleId == null || moduleId.isBlank()) {
            return;
        }
        CompoundTag root = entity.getPersistentData().getCompound(ROOT).copy();
        CompoundTag inventories = root.getCompound(KEY).copy();
        if (inventories.contains(moduleId, Tag.TAG_COMPOUND)) {
            CompoundTag invTag = inventories.getCompound(moduleId);
            if (dropItems) {
                dropStacks(entity, readStacks(invTag, invTag.contains(FIELD_SIZE) ? invTag.getInt(FIELD_SIZE) : 0));
            }
            inventories.remove(moduleId);
            root.put(KEY, inventories);
            entity.getPersistentData().put(ROOT, root);
        }
    }

    public static CompoundTag toCompound(LivingEntity entity) {
        CompoundTag result = new CompoundTag();
        if (entity == null) {
            return result;
        }
        CompoundTag root = entity.getPersistentData().getCompound(ROOT);
        if (root.contains(KEY, Tag.TAG_COMPOUND)) {
            result = root.getCompound(KEY).copy();
        }
        return result;
    }

    public static Map<String, Integer> collectInventorySizes(ListTag guiTabs) {
        Map<String, Integer> sizes = new HashMap<>();
        if (guiTabs == null) {
            return sizes;
        }
        for (int i = 0; i < guiTabs.size(); i++) {
            if (!(guiTabs.get(i) instanceof CompoundTag tab)) {
                continue;
            }
            if (!tab.contains("modules", Tag.TAG_LIST)) {
                continue;
            }
            ListTag modules = tab.getList("modules", Tag.TAG_COMPOUND);
            for (int m = 0; m < modules.size(); m++) {
                CompoundTag module = modules.getCompound(m);
                if (!"inventory".equalsIgnoreCase(module.getString("type"))) {
                    continue;
                }
                CompoundTag config = module.getCompound("config");
                String moduleId = config.getString("module_id");
                if (moduleId == null || moduleId.isBlank()) {
                    continue;
                }
                int rows = Math.max(1, config.getInt("rows"));
                int cols = Math.max(1, config.getInt("cols"));
                sizes.put(moduleId, rows * cols);
            }
        }
        return sizes;
    }

    public static void syncToGuiTabs(LivingEntity entity, ListTag guiTabs) {
        if (entity == null) {
            return;
        }
        Map<String, Integer> activeSizes = collectInventorySizes(guiTabs);
        CompoundTag root = entity.getPersistentData().getCompound(ROOT).copy();
        CompoundTag inventories = root.getCompound(KEY).copy();
        boolean changed = false;

        List<String> storedIds = new ArrayList<>(inventories.getAllKeys());
        for (String moduleId : storedIds) {
            if (!activeSizes.containsKey(moduleId)) {
                if (inventories.contains(moduleId, Tag.TAG_COMPOUND)) {
                    dropStacks(entity, readStacks(inventories.getCompound(moduleId),
                            inventories.getCompound(moduleId).contains(FIELD_SIZE) ? inventories.getCompound(moduleId).getInt(FIELD_SIZE) : 0));
                    inventories.remove(moduleId);
                    changed = true;
                }
                continue;
            }
            int nextSize = Math.max(0, activeSizes.getOrDefault(moduleId, 0));
            CompoundTag invTag = inventories.getCompound(moduleId);
            int oldSize = invTag.contains(FIELD_SIZE) ? invTag.getInt(FIELD_SIZE) : 0;
            List<ItemStack> stacks = resize(readStacks(invTag, oldSize), nextSize);
            if (oldSize > nextSize) {
                dropStacks(entity, tail(readStacks(invTag, oldSize), nextSize));
            }
            invTag.putInt(FIELD_SIZE, nextSize);
            invTag.put(FIELD_ITEMS, writeStacks(stacks));
            inventories.put(moduleId, invTag);
            changed = true;
        }

        if (changed) {
            root.put(KEY, inventories);
            entity.getPersistentData().put(ROOT, root);
        }
    }

    public static String firstInventoryModuleId(ListTag guiTabs) {
        if (guiTabs == null) {
            return "";
        }
        for (int i = 0; i < guiTabs.size(); i++) {
            if (!(guiTabs.get(i) instanceof CompoundTag tab)) {
                continue;
            }
            if (!tab.contains("modules", Tag.TAG_LIST)) {
                continue;
            }
            ListTag modules = tab.getList("modules", Tag.TAG_COMPOUND);
            for (int m = 0; m < modules.size(); m++) {
                CompoundTag module = modules.getCompound(m);
                if (!"inventory".equalsIgnoreCase(module.getString("type"))) {
                    continue;
                }
                CompoundTag config = module.getCompound("config");
                String moduleId = config.getString("module_id");
                if (moduleId != null && !moduleId.isBlank()) {
                    return moduleId;
                }
            }
        }
        return "";
    }

    public static String moduleId(CompoundTag moduleConfig) {
        if (moduleConfig == null) {
            return "";
        }
        String id = moduleConfig.getString("module_id");
        if (id == null || id.isBlank()) {
            id = UUID.randomUUID().toString();
            moduleConfig.putString("module_id", id);
        }
        return id;
    }

    public static String displayName(CompoundTag moduleConfig) {
        if (moduleConfig == null) {
            return "物品栏模块";
        }
        String name = moduleConfig.getString("name");
        return name == null || name.isBlank() ? "物品栏模块" : name;
    }

    public static int rows(CompoundTag moduleConfig) {
        if (moduleConfig == null) {
            return 1;
        }
        return Math.max(1, moduleConfig.getInt("rows"));
    }

    public static int cols(CompoundTag moduleConfig) {
        if (moduleConfig == null) {
            return 1;
        }
        return Math.max(1, moduleConfig.getInt("cols"));
    }

    public static int slotCount(CompoundTag moduleConfig) {
        return rows(moduleConfig) * cols(moduleConfig);
    }

    public static boolean showName(CompoundTag moduleConfig) {
        return moduleConfig != null && moduleConfig.getBoolean("show_name");
    }

    public static String stackMode(CompoundTag moduleConfig) {
        if (moduleConfig == null) {
            return "vanilla";
        }
        String mode = moduleConfig.getString("stack_mode");
        return mode == null || mode.isBlank() ? "vanilla" : mode;
    }

    public static boolean allItems(CompoundTag moduleConfig) {
        return moduleConfig != null && moduleConfig.getBoolean("all_items");
    }

    public static String filterMode(CompoundTag moduleConfig) {
        if (moduleConfig == null) {
            return "blacklist";
        }
        String mode = moduleConfig.getString("filter_mode");
        return mode == null || mode.isBlank() ? "blacklist" : mode;
    }

    public static List<String> filters(CompoundTag moduleConfig) {
        List<String> filters = new ArrayList<>();
        if (moduleConfig == null || !moduleConfig.contains("filters", Tag.TAG_LIST)) {
            return filters;
        }
        ListTag list = moduleConfig.getList("filters", Tag.TAG_STRING);
        for (int i = 0; i < list.size(); i++) {
            String id = list.getString(i);
            if (id != null && !id.isBlank()) {
                filters.add(id);
            }
        }
        return filters;
    }

    public static boolean allowsItem(CompoundTag moduleConfig, ItemStack stack) {
        if (stack == null || stack.isEmpty()) {
            return false;
        }
        if (allItems(moduleConfig)) {
            return true;
        }
        String itemId = ForgeRegistries.ITEMS.getKey(stack.getItem()) != null
                ? ForgeRegistries.ITEMS.getKey(stack.getItem()).toString()
                : "";
        if (itemId.isBlank()) {
            return false;
        }
        List<String> filters = filters(moduleConfig);
        boolean whiteList = !"blacklist".equalsIgnoreCase(filterMode(moduleConfig));
        if (filters.isEmpty()) {
            return !whiteList;
        }
        boolean match = filters.contains(itemId);
        return whiteList ? match : !match;
    }

    public static int maxStackSize(CompoundTag moduleConfig, ItemStack stack) {
        if (stack == null || stack.isEmpty()) {
            return 1;
        }
        String mode = stackMode(moduleConfig);
        if ("no_stack".equalsIgnoreCase(mode)) {
            return 1;
        }
        if ("infinite".equalsIgnoreCase(mode)) {
            return 9999;
        }
        return Math.max(1, stack.getMaxStackSize());
    }

    private static List<ItemStack> empty(int size) {
        if (size <= 0) {
            return List.of();
        }
        List<ItemStack> stacks = new ArrayList<>(Collections.nCopies(size, ItemStack.EMPTY));
        return stacks;
    }

    private static List<ItemStack> resize(List<ItemStack> stacks, int size) {
        if (size <= 0) {
            return List.of();
        }
        List<ItemStack> result = new ArrayList<>(Collections.nCopies(size, ItemStack.EMPTY));
        if (stacks != null) {
            for (int i = 0; i < Math.min(size, stacks.size()); i++) {
                ItemStack stack = stacks.get(i);
                result.set(i, stack == null ? ItemStack.EMPTY : stack.copy());
            }
        }
        return result;
    }

    private static List<ItemStack> tail(List<ItemStack> stacks, int keepSize) {
        List<ItemStack> dropped = new ArrayList<>();
        if (stacks == null || keepSize < 0) {
            return dropped;
        }
        for (int i = Math.max(keepSize, 0); i < stacks.size(); i++) {
            ItemStack stack = stacks.get(i);
            if (stack != null && !stack.isEmpty()) {
                dropped.add(stack.copy());
            }
        }
        return dropped;
    }

    private static List<ItemStack> readStacks(CompoundTag invTag, int size) {
        List<ItemStack> stacks = empty(size);
        if (invTag == null || !invTag.contains(FIELD_ITEMS, Tag.TAG_LIST)) {
            return stacks;
        }
        ListTag items = invTag.getList(FIELD_ITEMS, Tag.TAG_COMPOUND);
        for (int i = 0; i < items.size(); i++) {
            CompoundTag slotTag = items.getCompound(i);
            int slot = slotTag.getInt(FIELD_SLOT);
            if (slot < 0) {
                continue;
            }
            if (slot >= stacks.size()) {
                continue;
            }
            if (slotTag.contains(FIELD_STACK, Tag.TAG_COMPOUND)) {
                stacks.set(slot, ItemStack.of(slotTag.getCompound(FIELD_STACK)));
            }
        }
        return stacks;
    }

    private static ListTag writeStacks(List<ItemStack> stacks) {
        ListTag items = new ListTag();
        if (stacks == null) {
            return items;
        }
        for (int i = 0; i < stacks.size(); i++) {
            ItemStack stack = stacks.get(i);
            if (stack == null || stack.isEmpty()) {
                continue;
            }
            CompoundTag slotTag = new CompoundTag();
            slotTag.putInt(FIELD_SLOT, i);
            slotTag.put(FIELD_STACK, stack.save(new CompoundTag()));
            items.add(slotTag);
        }
        return items;
    }

    private static void dropStacks(LivingEntity entity, List<ItemStack> stacks) {
        if (entity == null || stacks == null || stacks.isEmpty()) {
            return;
        }
        for (ItemStack stack : stacks) {
            if (stack != null && !stack.isEmpty()) {
                entity.spawnAtLocation(stack);
            }
        }
    }
}
