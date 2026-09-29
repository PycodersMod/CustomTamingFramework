package com.example.customtamingframework.config;

import com.example.customtamingframework.CustomTamingFramework;
import com.example.customtamingframework.pet.PetEquipmentState;
import com.example.customtamingframework.pet.PetProgress;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.StringTag;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraftforge.fml.loading.FMLPaths;
import net.minecraftforge.registries.ForgeRegistries;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

/**
 * Server-side helper that builds config screen data for the 4 sub-screens
 * (PetList, FoodList, TamingCore, SpecialFood) and applies save payloads.
 */
public final class CtFConfigHelper {
    private CtFConfigHelper() {
    }

    /**
     * Build screen data from current configuration.
     * screenType: 0=PetList, 1=FoodList, 2=TamingCore, 3=SpecialFood
     */
    public static CompoundTag buildScreenData(int screenType) {
        CompoundTag result = new CompoundTag();
        switch (screenType) {
            case 0 -> buildPetListData(result);
            case 1 -> buildFoodListData(result);
            case 2 -> buildTamingCoreData(result);
            case 3 -> buildSpecialItemData(result);
        }
        return result;
    }

    /**
     * Apply screen data from client save request.
     * screenType: 0=PetList, 1=FoodList, 2=TamingCore, 3=SpecialFood
     */
    public static void applyScreenData(int screenType, CompoundTag data, ServerLevel level) {
        switch (screenType) {
            case 0 -> applyPetListData(data, level);
            case 1 -> applyFoodListData(data);
            case 2 -> applyTamingCoreData(data);
            case 3 -> applySpecialItemData(data);
        }
    }

    // ── PetList ──

    private static void buildPetListData(CompoundTag result) {
        // Collect all pet IDs, preserving order
        Set<String> allIds = new LinkedHashSet<>();
        allIds.addAll(CtfSnbtConfig.requiresTamingEntityIds());
        allIds.addAll(CtfSnbtConfig.directEntityIds());
        allIds.addAll(CtfSnbtConfig.generalEntityIds());
        ListTag pets = new ListTag();
        for (String id : allIds) {
            CompoundTag entry = new CompoundTag();
            entry.putString("id", id);
            var setting = CtfSnbtConfig.petSetting(id);
            boolean isGeneral = CtfSnbtConfig.generalEntityIds().contains(id);
            // Tamable/rideable: default true; General: default false
            entry.putBoolean("allowCTF", setting != null ? setting.allowCtf() : !isGeneral);
            entry.putString("nickname", setting != null ? setting.nickname() : "");
            String transKey = "entity." + id.replace(':', '.');
            entry.putString("displayName", Component.translatable(transKey).getString());
            pets.add(entry);
        }
        result.put("pets", pets);
    }

    private static void applyPetListData(CompoundTag data, ServerLevel level) {
        ListTag pets = data.getList("pets", 10);
        List<String> newlyDisabledIds = new ArrayList<>();
        for (int i = 0; i < pets.size(); i++) {
            CompoundTag entry = pets.getCompound(i);
            String id = entry.getString("id");
            boolean allowCTF = entry.getBoolean("allowCTF");
            var existing = CtfSnbtConfig.petSetting(id);
            // Track newly disabled species
            boolean wasAllowed = existing == null || existing.allowCtf();
            if (wasAllowed && !allowCTF) {
                newlyDisabledIds.add(id);
            }
            if (existing != null) {
                CtfSnbtConfig.savePetSetting(id, existing.nickname(), existing.guiTabs(), allowCTF);
            } else {
                String nickname = entry.getString("nickname");
                if (nickname.isBlank()) {
                    nickname = id.contains(":") ? "[CTF" + id.substring(id.indexOf(':') + 1) + "]" : "[CTF驯兽]";
                }
                CtfSnbtConfig.savePetSetting(id, nickname, new ListTag(), allowCTF);
            }
        }
        // Remove CTF state from all existing converted pets of newly disabled species
        if (!newlyDisabledIds.isEmpty() && level != null) {
            removeCtfFromDisabledPets(newlyDisabledIds, level);
        }
    }

    /** Remove CTF state from all loaded entities matching the given entity type IDs. */
    private static void removeCtfFromDisabledPets(List<String> disabledEntityTypeIds, ServerLevel level) {
        for (String entityTypeId : disabledEntityTypeIds) {
            ResourceLocation typeId = ResourceLocation.tryParse(entityTypeId);
            if (typeId == null) continue;
            var entityType = ForgeRegistries.ENTITY_TYPES.getValue(typeId);
            if (entityType == null) continue;
            int removedCount = 0;
            // Iterate all entities in the level
            for (Entity entity : level.getEntities().getAll()) {
                if (entity.getType() != entityType) continue;
                if (!(entity instanceof LivingEntity living)) continue;
                if (!PetProgress.isEnabled(living)) continue;
                // Drop equipped module items
                PetEquipmentState.remove(living);
                // Clear CTF persistent data and display name
                PetProgress.remove(living);
                removedCount++;
            }
            if (removedCount > 0) {
                CustomTamingFramework.LOGGER.info("[CTF] Removed CTF state from {} entities of type {}", removedCount, entityTypeId);
            }
        }
    }

    // ── FoodList ──

    private static void buildFoodListData(CompoundTag result) {
        ListTag foods = new ListTag();
        Set<String> allKnown = new LinkedHashSet<>();
        for (String id : CtfSnbtConfig.normalFoodItemIds()) {
            if (allKnown.add(id)) {
                CompoundTag entry = new CompoundTag();
                entry.putString("id", id);
                entry.putInt("type", 0); // normal
                foods.add(entry);
            }
        }
        for (String id : CtfSnbtConfig.specialFoodItemIds()) {
            if (allKnown.add(id)) {
                CompoundTag entry = new CompoundTag();
                entry.putString("id", id);
                entry.putInt("type", 1); // functional
                foods.add(entry);
            }
        }
        for (String id : CtfSnbtConfig.disabledFoodItemIds()) {
            if (allKnown.add(id)) {
                CompoundTag entry = new CompoundTag();
                entry.putString("id", id);
                entry.putInt("type", 2); // disabled
                foods.add(entry);
            }
        }
        result.put("foods", foods);
    }

    private static void applyFoodListData(CompoundTag data) {
        ListTag foods = data.getList("foods", 10);
        List<String> normalIds = new ArrayList<>();
        List<String> specialIds = new ArrayList<>();
        List<String> disabledIds = new ArrayList<>();
        for (int i = 0; i < foods.size(); i++) {
            CompoundTag entry = foods.getCompound(i);
            String id = entry.getString("id");
            int type = entry.getInt("type");
            if (type == 0) {
                normalIds.add(id);
            } else if (type == 1) {
                specialIds.add(id);
            } else if (type == 2) {
                disabledIds.add(id);
            }
        }
        // Update in-memory lists
        CtfSnbtConfig.replaceNormalFoodIds(normalIds);
        CtfSnbtConfig.replaceSpecialFoodIds(specialIds);
        CtfSnbtConfig.replaceDisabledFoodIds(disabledIds);
        // Persist to basic.snbt
        saveBasicFile();
    }

    // ── TamingCore ──

    private static void buildTamingCoreData(CompoundTag result) {
        result.putString("recipe", CtfSnbtConfig.tamingCoreRecipe());
        result.putString("displayName", CtfSnbtConfig.tamingCoreDisplayName());
        result.putBoolean("consumeOnSuccess", CtfSnbtConfig.tamingCoreConsumeOnSuccess());
    }

    private static void applyTamingCoreData(CompoundTag data) {
        String recipe = data.getString("recipe");
        String displayName = data.getString("displayName");
        boolean consumeOnSuccess = data.getBoolean("consumeOnSuccess");
        CtfSnbtConfig.updateTamingCore(displayName, recipe, consumeOnSuccess);
        saveBasicFile();
    }

    // ── Special Food Items ──

    private static void buildSpecialItemData(CompoundTag result) {
        // Collect all food list IDs for client-side conflict checking
        java.util.Set<String> usedIds = new java.util.LinkedHashSet<>();
        usedIds.addAll(CtfSnbtConfig.normalFoodItemIds());
        usedIds.addAll(CtfSnbtConfig.specialFoodItemIds());
        usedIds.addAll(CtfSnbtConfig.disabledFoodItemIds());
        ListTag usedList = new ListTag();
        for (String id : usedIds) usedList.add(StringTag.valueOf(id));
        result.put("used_ids", usedList);

        ListTag items = new ListTag();
        for (String id : CtfSnbtConfig.specialItemIds()) {
            CompoundTag entry = new CompoundTag();
            entry.putString("id", id);
            String displayName = getItemDisplayName(id);
            entry.putString("displayName", displayName != null ? displayName : id);
            items.add(entry);
        }
        result.put("items", items);
    }

    private static void applySpecialItemData(CompoundTag data) {
        ListTag items = data.getList("items", 10);
        List<String> ids = new ArrayList<>();
        for (int i = 0; i < items.size(); i++) {
            ids.add(items.getCompound(i).getString("id"));
        }
        CtfSnbtConfig.replaceSpecialItemIds(ids);
        saveBasicFile();
    }

    /** Resolve an item's display name from its registry ID, or null if not found. */
    private static String getItemDisplayName(String itemId) {
        ResourceLocation rl = ResourceLocation.tryParse(itemId);
        if (rl == null || !ForgeRegistries.ITEMS.containsKey(rl)) return null;
        net.minecraft.world.item.Item item = ForgeRegistries.ITEMS.getValue(rl);
        return item != null ? Component.translatable(item.getDescriptionId()).getString() : null;
    }

    // ── Basic file persistence ──

    private static void saveBasicFile() {
        try {
            Path basicPath = FMLPaths.GAMEDIR.get().resolve("CTF_Settings").resolve("basic.snbt");
            if (Files.notExists(basicPath.getParent())) {
                Files.createDirectories(basicPath.getParent());
            }
            // Build a CompoundTag matching basic.snbt format
            CompoundTag root = new CompoundTag();
            CompoundTag core = new CompoundTag();
            core.putString("display_name", CtfSnbtConfig.tamingCoreDisplayName());
            core.putString("recipe", CtfSnbtConfig.tamingCoreRecipe());
            core.putBoolean("consume_on_success", CtfSnbtConfig.tamingCoreConsumeOnSuccess());
            root.put("taming_core", core);

            CompoundTag pets = new CompoundTag();
            pets.putString("requires_taming_mode", CtfSnbtConfig.requiresTamingMode());
            ListTag reqList = new ListTag();
            for (String id : CtfSnbtConfig.requiresTamingEntityIds()) reqList.add(StringTag.valueOf(id));
            pets.put("requires_taming_entity_ids", reqList);
            ListTag dirList = new ListTag();
            for (String id : CtfSnbtConfig.directEntityIds()) dirList.add(StringTag.valueOf(id));
            pets.put("direct_entity_ids", dirList);
            ListTag genList = new ListTag();
            for (String id : CtfSnbtConfig.generalEntityIds()) genList.add(StringTag.valueOf(id));
            pets.put("general_entity_ids", genList);
            root.put("pets", pets);

            CompoundTag foods = new CompoundTag();
            foods.putString("normal_foods_mode", CtfSnbtConfig.normalFoodsMode());
            ListTag normList = new ListTag();
            for (String id : CtfSnbtConfig.normalFoodItemIds()) normList.add(StringTag.valueOf(id));
            foods.put("normal_food_item_ids", normList);
            ListTag specList = new ListTag();
            for (String id : CtfSnbtConfig.specialFoodItemIds()) specList.add(StringTag.valueOf(id));
            foods.put("special_food_item_ids", specList);
            ListTag disList = new ListTag();
            for (String id : CtfSnbtConfig.disabledFoodItemIds()) disList.add(StringTag.valueOf(id));
            foods.put("disabled_food_item_ids", disList);
            ListTag specItemList = new ListTag();
            for (String id : CtfSnbtConfig.specialItemIds()) specItemList.add(StringTag.valueOf(id));
            foods.put("special_item_ids", specItemList);
            foods.putBoolean("consume_on_success", CtfSnbtConfig.usableItemsConsumeOnSuccess());
            root.put("pet_foods", foods);
            root.put("settings", CtfUiSettings.sanitize(CtfSnbtConfig.uiSettings()));

            Files.writeString(basicPath, CtfSnbtConfig.prettyPrintTag(root), StandardCharsets.UTF_8);
            CustomTamingFramework.LOGGER.info("[CTF] basic.snbt saved via config screen");
        } catch (IOException e) {
            CustomTamingFramework.LOGGER.error("Failed to save basic.snbt from config screen", e);
        }
    }
}
