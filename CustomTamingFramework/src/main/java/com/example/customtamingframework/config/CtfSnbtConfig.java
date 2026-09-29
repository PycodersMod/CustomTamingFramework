package com.example.customtamingframework.config;

import com.example.customtamingframework.CustomTamingFramework;
import com.example.customtamingframework.pet.PetEquipment;
import com.example.customtamingframework.manual.ManualBookConfig;
import net.minecraft.nbt.ByteTag;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.DoubleTag;
import net.minecraft.nbt.FloatTag;
import net.minecraft.nbt.IntTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.LongTag;
import net.minecraft.nbt.ShortTag;
import net.minecraft.nbt.StringTag;
import net.minecraft.nbt.Tag;
import net.minecraft.nbt.TagParser;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.decoration.ArmorStand;
import net.minecraft.world.entity.Saddleable;
import net.minecraft.world.entity.TamableAnimal;
import net.minecraft.world.entity.animal.Pig;
import net.minecraft.world.entity.animal.camel.Camel;
import net.minecraft.world.entity.animal.horse.AbstractHorse;
import net.minecraft.world.entity.animal.horse.SkeletonHorse;
import net.minecraft.world.entity.animal.horse.ZombieHorse;
import net.minecraft.world.entity.monster.Strider;
import net.minecraft.world.item.Item;
import net.minecraft.world.entity.player.Player;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraftforge.fml.loading.FMLPaths;
import net.minecraftforge.registries.ForgeRegistries;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;
import java.util.stream.Stream;

public final class CtfSnbtConfig {
    private static final String SETTINGS_DIR = "CTF_Settings";
    private static final String BASIC_FILE = "basic.snbt";
    private static final String GENERAL_GUI_FILE = "GeneralGUI.snbt";
    private static final String PET_SETTINGS_DIR = "PetSettings";
    private static final String RESOURCES_DIR = "resources";
    private static final List<String> DEFAULT_REQUIRES_TAMING_PETS = List.of("minecraft:wolf", "minecraft:cat", "minecraft:parrot", "minecraft:horse", "minecraft:donkey", "minecraft:mule", "minecraft:llama", "minecraft:trader_llama");
    private static final List<String> DEFAULT_DIRECT_PETS = List.of("minecraft:skeleton_horse", "minecraft:zombie_horse", "minecraft:pig", "minecraft:strider", "minecraft:camel");
    private static final List<String> DEFAULT_SPECIAL_FOODS = List.of("minecraft:golden_apple", "minecraft:enchanted_golden_apple", "minecraft:golden_carrot");

    private static Path rootDirectory;
    private static Path settingsDirectory;
    private static String tamingCoreDisplayName = "驯兽核心";
    private static String tamingCoreRecipe = "minecraft:redstone,minecraft:redstone,minecraft:redstone,minecraft:redstone,minecraft:diamond,minecraft:redstone,minecraft:redstone,minecraft:redstone,minecraft:redstone";
    private static boolean tamingCoreConsumeOnSuccess = true;
    private static String requiresTamingMode = "AUTO_TAMEABLE_PLUS_LIST";
    private static String normalFoodsMode = "AUTO_PLAYER_FOOD_PLUS_LIST";
    private static boolean usableItemsConsumeOnSuccess = true;
    private static CompoundTag generalGui = defaultGeneralGui();
    private static CompoundTag uiSettings = CtfUiSettings.defaults();
    private static boolean basicSettingsNeedsRepair;

    private static final List<String> requiresTamingEntityIds = new ArrayList<>();
    private static final List<String> directEntityIds = new ArrayList<>();
    private static final List<String> normalFoodItemIds = new ArrayList<>();
    private static final List<String> specialFoodItemIds = new ArrayList<>();
    private static final List<String> disabledFoodItemIds = new ArrayList<>();
    private static final List<String> specialItemIds = new ArrayList<>();
    private static final List<String> generalEntityIds = new ArrayList<>();
    private static final Map<String, PetSetting> petSettings = new TreeMap<>();

    private CtfSnbtConfig() {
    }

    public static void init() {
        rootDirectory = FMLPaths.GAMEDIR.get();
        settingsDirectory = rootDirectory.resolve(SETTINGS_DIR);
        if (Files.notExists(settingsDirectory)) {
            generateDefaultSettings();
        }
        loadSettings();
        ManualBookConfig.init();
        try {
            Files.createDirectories(settingsDirectory.resolve(RESOURCES_DIR));
        } catch (IOException e) {
            CustomTamingFramework.LOGGER.error("Failed to create CTF_Settings/resources/", e);
        }
        CustomTamingFramework.LOGGER.info("[CTF_SELFTEST] config.snbt.root={} result=PASS", settingsDirectory);
    }

    /**
     * Re-run the default repair pass after registries are fully ready.
     * This catches entity types that were not available during early config init.
     */
    public static synchronized void finalizeLoadedDefaults() {
        if (settingsDirectory == null) {
            return;
        }
        populateGeneralEntities();
        repairMissingDefaults();
    }

    public static Path settingsDirectory() {
        return settingsDirectory;
    }

    public static Path resourcesDirectory() {
        return settingsDirectory.resolve(RESOURCES_DIR);
    }

    public static String tamingCoreDisplayName() {
        return tamingCoreDisplayName;
    }

    public static String tamingCoreRecipe() {
        return tamingCoreRecipe;
    }

    public static boolean tamingCoreConsumeOnSuccess() {
        return tamingCoreConsumeOnSuccess;
    }

    public static String requiresTamingMode() {
        return requiresTamingMode;
    }

    public static List<String> requiresTamingEntityIds() {
        return Collections.unmodifiableList(requiresTamingEntityIds);
    }

    public static List<String> directEntityIds() {
        return Collections.unmodifiableList(directEntityIds);
    }

    public static String normalFoodsMode() {
        return normalFoodsMode;
    }

    public static List<String> normalFoodItemIds() {
        return Collections.unmodifiableList(normalFoodItemIds);
    }

    public static List<String> specialFoodItemIds() {
        return Collections.unmodifiableList(specialFoodItemIds);
    }

    public static List<String> disabledFoodItemIds() {
        return Collections.unmodifiableList(disabledFoodItemIds);
    }

    public static List<String> specialItemIds() {
        return Collections.unmodifiableList(specialItemIds);
    }

    public static List<String> generalEntityIds() {
        return Collections.unmodifiableList(generalEntityIds);
    }

    public static boolean usableItemsConsumeOnSuccess() {
        return usableItemsConsumeOnSuccess;
    }

    public static Map<String, PetSetting> petSettings() {
        return Collections.unmodifiableMap(petSettings);
    }

    public static PetSetting petSetting(String entityId) {
        return petSettings.get(entityId);
    }

    public static CompoundTag generalGui() {
        return generalGui.copy();
    }

    public static CompoundTag uiSettings() {
        return uiSettings.copy();
    }

    public static void saveUiSettings(CompoundTag tag) {
        uiSettings = CtfUiSettings.sanitize(tag);
        try {
            writeSnbt(settingsDirectory.resolve(BASIC_FILE), createBasicFromLoadedValues());
            ManualBookConfig.syncUserManualCover(CtfUiSettings.visibleForNonOp(uiSettings, CtfUiSettings.SHOW_NON_OP_GENERAL_CONFIG_SCREEN));
            CustomTamingFramework.LOGGER.info("[CTF_SELFTEST] config.snbt.ui_settings_saved=PASS");
        } catch (IOException e) {
            CustomTamingFramework.LOGGER.error("Failed to save basic.snbt ui settings", e);
        }
    }

    public static boolean hasGeneralGuiTabs() {
        // New format: has any CompoundTag key (excluding "tabs" which is an old-format array)
        for (String key : generalGui.getAllKeys()) {
            if (generalGui.contains(key, 10)) return true;
        }
        // Old format fallback: check tabs array
        if (generalGui.contains("tabs", 9)) {
            return !generalGui.getList("tabs", 10).isEmpty();
        }
        return false;
    }

    public static void saveGeneralGui(CompoundTag tag) {
        generalGui = tag.copy();
        try {
            writeSnbt(settingsDirectory.resolve(GENERAL_GUI_FILE), tag);
            CustomTamingFramework.LOGGER.info("[CTF_SELFTEST] config.snbt.general_gui_saved=PASS");
        } catch (IOException e) {
            CustomTamingFramework.LOGGER.error("Failed to save GeneralGUI.snbt", e);
        }
    }

    public static void savePetSetting(String entityId, String nickname, ListTag guiTabs, boolean allowCtf) {
        CompoundTag dataVariables = defaultDataVariables();
        PetSetting existing = petSettings.get(entityId);
        if (existing != null && existing.dataVariables() != null && !existing.dataVariables().isEmpty()) {
            dataVariables = existing.dataVariables().copy();
        }
        savePetSetting(entityId, nickname, guiTabs, dataVariables, allowCtf);
    }

    public static void savePetSetting(String entityId, String nickname, ListTag guiTabs, CompoundTag dataVariables) {
        boolean allowCtf = true;
        PetSetting existing = petSettings.get(entityId);
        if (existing != null) {
            allowCtf = existing.allowCtf();
        }
        savePetSetting(entityId, nickname, guiTabs, dataVariables, allowCtf);
    }

    public static void savePetSetting(String entityId, String nickname, ListTag guiTabs,
                                      CompoundTag dataVariables, boolean allowCtf) {
        try {
            CompoundTag safeDataVariables = dataVariables != null && !dataVariables.isEmpty()
                    ? dataVariables.copy() : defaultDataVariables();
            Path path = settingsDirectory.resolve(PET_SETTINGS_DIR).resolve(toPetFileName(entityId));
            Files.createDirectories(path.getParent());
            Files.writeString(path, petSettingToSnbt(entityId, nickname, guiTabs, safeDataVariables, allowCtf), StandardCharsets.UTF_8);
            petSettings.put(entityId, new PetSetting(entityId, nickname,
                    guiTabs != null ? guiTabs.copy() : new ListTag(), safeDataVariables, allowCtf));
            CustomTamingFramework.LOGGER.info("[CTF_SELFTEST] config.snbt.pet_setting_saved=PASS entity={}", entityId);
        } catch (IOException e) {
            CustomTamingFramework.LOGGER.error("Failed to save pet setting: {}", entityId, e);
        }
    }

    /** 3-arg overload: preserves existing allowCtf value. */
    public static void savePetSetting(String entityId, String nickname, ListTag guiTabs) {
        boolean allowCtf = true;
        PetSetting existing = petSettings.get(entityId);
        if (existing != null) allowCtf = existing.allowCtf();
        savePetSetting(entityId, nickname, guiTabs, allowCtf);
    }

    // ── Entity display name helpers ──

    /** Update a living entity's custom name based on its CTF nickname (from pet setting) and CtfBaseName (from persistent data). */
    public static void updateEntityDisplayName(LivingEntity entity) {
        String entityType = ForgeRegistries.ENTITY_TYPES.getKey(entity.getType()).toString();
        PetSetting setting = petSettings.get(entityType);
        String nickname = (setting != null && !setting.nickname().isBlank()) ? setting.nickname() : "";
        MutableComponent nickComponent;
        if (nickname.startsWith("[CTFid:")) {
            String storedId = nickname.substring(7, nickname.length() - 1);
            String key = "entity." + storedId.replace(':', '.');
            nickComponent = Component.literal("[CTF")
                    .append(Component.translatable(key))
                    .append(Component.literal("]"));
        } else if (nickname.isBlank()) {
            nickComponent = Component.literal("[CTF")
                    .append(Component.translatable(entity.getType().getDescriptionId()))
                    .append(Component.literal("]"));
        } else {
            nickComponent = Component.literal(nickname);
        }
        String baseName = getCtfBaseName(entity);
        MutableComponent displayName = Component.empty()
                .append(nickComponent.withStyle(ChatFormatting.AQUA, ChatFormatting.ITALIC));
        if (!baseName.isBlank()) {
            displayName.append(Component.literal(" " + baseName));
        }
        entity.setCustomName(displayName);
        entity.setCustomNameVisible(true);
    }

    /** Read the stored base name (from a previous nametag use) from entity persistent data. */
    public static String getCtfBaseName(LivingEntity entity) {
        CompoundTag root = entity.getPersistentData().getCompound("CustomTamingFramework");
        return root.getString("CtfBaseName");
    }

    /** Store a base name (e.g. from nametag) in entity persistent data, appending to existing CTF data. */
    public static void setCtfBaseName(LivingEntity entity, String baseName) {
        CompoundTag root = entity.getPersistentData().getCompound("CustomTamingFramework");
        CompoundTag newRoot = root.copy();
        newRoot.putString("CtfBaseName", baseName);
        entity.getPersistentData().put("CustomTamingFramework", newRoot);
    }

    private static void generateDefaultSettings() {
        try {
            Files.createDirectories(settingsDirectory.resolve(PET_SETTINGS_DIR));
            ScanResult scan = scanRegistries();
            CompoundTag basic = createBasic(scan);
            writeSnbt(settingsDirectory.resolve(BASIC_FILE), basic);
            writeSnbt(settingsDirectory.resolve(GENERAL_GUI_FILE), defaultGeneralGui());
            for (String entityId : scan.allPets()) {
                Files.writeString(settingsDirectory.resolve(PET_SETTINGS_DIR).resolve(toPetFileName(entityId)),
                        petSettingToSnbt(entityId, "[CTFid:" + entityId + "]", null, true), StandardCharsets.UTF_8);
            }
            CustomTamingFramework.LOGGER.info("[CTF_SELFTEST] config.snbt.defaults_generated=PASS pets={} general={} normal_foods={} special_foods={}", scan.allPets().size(), generalEntityIds.size(), scan.normalFoods().size(), scan.specialFoods().size());
        } catch (IOException exception) {
            CustomTamingFramework.LOGGER.error("Failed to generate CTF_Settings defaults", exception);
        }
    }

    private static void loadSettings() {
        resetToDefaults();
        loadBasic();
        populateGeneralEntities();
        loadPetSettings();
        loadGeneralGui();
        repairMissingDefaults();
    }

    private static void loadBasic() {
        Path basic = settingsDirectory.resolve(BASIC_FILE);
        if (Files.notExists(basic)) {
            CustomTamingFramework.LOGGER.warn("CTF_Settings exists but basic.snbt is missing; using in-memory defaults without regenerating files");
            return;
        }
        try {
            CompoundTag root = readSnbt(basic);
            CompoundTag core = root.getCompound("taming_core");
            if (core.contains("display_name")) {
                tamingCoreDisplayName = core.getString("display_name");
            }
            if (core.contains("recipe")) {
                tamingCoreRecipe = core.getString("recipe");
            }
            if (core.contains("consume_on_success")) {
                tamingCoreConsumeOnSuccess = core.getBoolean("consume_on_success");
            }

            CompoundTag pets = root.getCompound("pets");
            if (pets.contains("requires_taming_mode")) {
                requiresTamingMode = pets.getString("requires_taming_mode");
            }
            loadStringList(pets.getList("requires_taming_entity_ids", 8), requiresTamingEntityIds);
            loadStringList(pets.getList("direct_entity_ids", 8), directEntityIds);
            loadStringList(pets.getList("general_entity_ids", 8), generalEntityIds);

            CompoundTag foods = root.getCompound("pet_foods");
            if (foods.contains("normal_foods_mode")) {
                normalFoodsMode = foods.getString("normal_foods_mode");
            }
            loadStringList(foods.getList("normal_food_item_ids", 8), normalFoodItemIds);
            loadStringList(foods.getList("special_food_item_ids", 8), specialFoodItemIds);
            loadStringList(foods.getList("disabled_food_item_ids", 8), disabledFoodItemIds);
            loadStringList(foods.getList("special_item_ids", 8), specialItemIds);
            if (foods.contains("consume_on_success")) {
                usableItemsConsumeOnSuccess = foods.getBoolean("consume_on_success");
            }
            CompoundTag rawSettings = root.contains("settings", 10) ? root.getCompound("settings") : new CompoundTag();
            uiSettings = CtfUiSettings.sanitize(rawSettings);
            basicSettingsNeedsRepair = !root.contains("settings", 10)
                    || !rawSettings.getAllKeys().containsAll(CtfUiSettings.defaults().getAllKeys());
            CustomTamingFramework.LOGGER.info("[CTF_SELFTEST] config.snbt.basic_loaded=PASS");
        } catch (Exception exception) {
            CustomTamingFramework.LOGGER.error("Failed to load CTF_Settings/basic.snbt; using in-memory defaults", exception);
        }
    }

    private static void loadPetSettings() {
        Path petDirectory = settingsDirectory.resolve(PET_SETTINGS_DIR);
        if (Files.notExists(petDirectory)) {
            CustomTamingFramework.LOGGER.warn("CTF_Settings/PetSettings is missing; no per-pet SNBT files loaded");
            return;
        }
        try (Stream<Path> files = Files.list(petDirectory)) {
            files.filter(path -> path.getFileName().toString().endsWith(".snbt"))
                    .forEach(CtfSnbtConfig::loadPetSetting);
            CustomTamingFramework.LOGGER.info("[CTF_SELFTEST] config.snbt.pet_settings_loaded=PASS count={}", petSettings.size());
        } catch (IOException exception) {
            CustomTamingFramework.LOGGER.error("Failed to load CTF_Settings/PetSettings", exception);
        }
    }

    private static void loadPetSetting(Path path) {
        try {
            CompoundTag tag = readSnbt(path);
            String id = tag.getString("id");
            if (!id.isBlank()) {
                ListTag guiTabs = new ListTag();
                CompoundTag dataVariables = tag.contains("data_variables", 10)
                        ? tag.getCompound("data_variables").copy()
                        : defaultDataVariables();
                for (String key : tag.getAllKeys()) {
                    if (!key.equals("id") && !key.equals("nickname") && !key.equals("allowCTF")
                            && !key.equals("data_variables") && tag.contains(key, 10)) {
                        CompoundTag tabData = tag.getCompound(key).copy();
                        tabData.putString("id", key);
                        if (!tabData.contains("title")) tabData.putString("title", key);
                        if (!tabData.contains("enabled")) tabData.putBoolean("enabled", true);
                        if (!tabData.contains("summary")) tabData.putString("summary", "");
                        if (!tabData.contains("order")) tabData.putInt("order", 999);
                        guiTabs.add(tabData);
                    }
                }
                boolean allowCtf = !tag.contains("allowCTF") || tag.getBoolean("allowCTF");
                petSettings.put(id, new PetSetting(id, tag.getString("nickname"), guiTabs, dataVariables, allowCtf));
            }
        } catch (Exception exception) {
            CustomTamingFramework.LOGGER.error("Failed to load pet setting {}", path, exception);
        }
    }

    private static void loadGeneralGui() {
        Path path = settingsDirectory.resolve(GENERAL_GUI_FILE);
        if (Files.notExists(path)) {
            CustomTamingFramework.LOGGER.warn("CTF_Settings/GeneralGUI.snbt is missing; creating default file from template");
            try {
                Files.createDirectories(path.getParent());
                writeSnbt(path, defaultGeneralGui());
            } catch (IOException e) {
                CustomTamingFramework.LOGGER.error("Failed to create default GeneralGUI.snbt", e);
            }
            return;
        }
        try {
            generalGui = readSnbt(path);
            // Auto-convert old {tabs: [...]} format to new {tab_id: {...}} format
            if (generalGui.contains("tabs", 9) && !generalGui.getAllKeys().stream().anyMatch(k -> generalGui.contains(k, 10) && !k.equals("tabs"))) {
                CompoundTag converted = new CompoundTag();
                ListTag oldTabs = generalGui.getList("tabs", 10);
                for (int i = 0; i < oldTabs.size(); i++) {
                    CompoundTag tab = oldTabs.getCompound(i);
                    String id = tab.getString("id");
                    if (!id.isBlank()) {
                        CompoundTag tabData = tab.copy();
                        tabData.remove("id");
                        converted.put(id, tabData);
                    }
                }
                generalGui = converted;
                writeSnbt(path, generalGui);
                CustomTamingFramework.LOGGER.info("Auto-converted GeneralGUI.snbt from old tabs-array format to new per-key format");
            }
            CustomTamingFramework.LOGGER.info("[CTF_SELFTEST] config.snbt.general_gui_loaded=PASS");
        } catch (Exception exception) {
            CustomTamingFramework.LOGGER.error("Failed to load CTF_Settings/GeneralGUI.snbt; using in-memory default", exception);
        }
    }

    /**
     * Populate generalEntityIds with mob-like entity types from the registry
     * that are not already in requiresTamingEntityIds or directEntityIds.
     * Uses BuiltInRegistries which is fully populated at this stage.
     */
    private static void populateGeneralEntities() {
        for (ResourceLocation id : BuiltInRegistries.ENTITY_TYPE.keySet()) {
            String idStr = id.toString();
            if (requiresTamingEntityIds.contains(idStr) || directEntityIds.contains(idStr) || generalEntityIds.contains(idStr))
                continue;
            EntityType<?> type = BuiltInRegistries.ENTITY_TYPE.get(id);
            if (type == null) continue;
            Class<? extends Entity> entityClass = type.getBaseClass();
            if (entityClass == null) continue;
            // Keep all creature-like living entities from vanilla/mod registries,
            // including bosses and summoned golems, while excluding players/armor stands.
            if (!LivingEntity.class.isAssignableFrom(entityClass)) continue;
            if (Player.class.isAssignableFrom(entityClass) || ArmorStand.class.isAssignableFrom(entityClass)) continue;
            generalEntityIds.add(idStr);
            // Create SNBT file for each general entity with allowCtf=false by default
            if (settingsDirectory != null) {
                try {
                    Path petFile = settingsDirectory.resolve(PET_SETTINGS_DIR).resolve(toPetFileName(idStr));
                    if (Files.notExists(petFile)) {
                        Files.createDirectories(petFile.getParent());
                        Files.writeString(petFile, petSettingToSnbt(idStr, "", null, false), StandardCharsets.UTF_8);
                    }
                } catch (IOException e) {
                    CustomTamingFramework.LOGGER.warn("Failed to create SNBT for general entity: {}", idStr, e);
                }
            }
        }
        // basic.snbt may be missing general entities (from old version); update it if already non-empty
        Path basicPath = settingsDirectory != null ? settingsDirectory.resolve(BASIC_FILE) : null;
        if (basicPath != null && Files.exists(basicPath) && !generalEntityIds.isEmpty()) {
            try {
                writeSnbt(basicPath, createBasicFromLoadedValues());
            } catch (IOException e) {
                CustomTamingFramework.LOGGER.warn("Failed to update basic.snbt with general entities", e);
            }
        }
        CustomTamingFramework.LOGGER.info("[CTF] Populated {} general entity types from BuiltInRegistries", generalEntityIds.size());
    }

    private static ScanResult scanRegistries() {
        Set<String> requiresTaming = new LinkedHashSet<>();
        Set<String> directPets = new LinkedHashSet<>();
        for (ResourceLocation id : ForgeRegistries.ENTITY_TYPES.getKeys()) {
            EntityType<?> entityType = ForgeRegistries.ENTITY_TYPES.getValue(id);
            if (entityType == null) continue;
            classifyEntity(id.toString(), entityType, requiresTaming, directPets);
        }
        requiresTaming.addAll(DEFAULT_REQUIRES_TAMING_PETS);
        directPets.addAll(DEFAULT_DIRECT_PETS);
        directPets.removeAll(requiresTaming);

        Set<String> normalFoods = new LinkedHashSet<>();
        Set<String> specialFoods = new LinkedHashSet<>();
        for (ResourceLocation id : ForgeRegistries.ITEMS.getKeys()) {
            Item item = ForgeRegistries.ITEMS.getValue(id);
            if (item == null || !item.isEdible()) {
                continue;
            }
            String itemId = id.toString();
            if (isDefaultSpecialFood(itemId)) {
                specialFoods.add(itemId);
            } else {
                normalFoods.add(itemId);
            }
        }
        specialFoods.addAll(DEFAULT_SPECIAL_FOODS);
        normalFoods.removeAll(specialFoods);
        return new ScanResult(List.copyOf(requiresTaming), List.copyOf(directPets), List.of(), List.copyOf(normalFoods), List.copyOf(specialFoods));
    }

    private static void repairMissingDefaults() {
        boolean changed = false;
        changed |= basicSettingsNeedsRepair;
        changed |= addMissing(requiresTamingEntityIds, DEFAULT_REQUIRES_TAMING_PETS);
        changed |= addMissing(directEntityIds, DEFAULT_DIRECT_PETS);
        changed |= addMissing(specialFoodItemIds, DEFAULT_SPECIAL_FOODS);
        // Auto-populate normal foods from registry if the list is empty
        if (normalFoodItemIds.isEmpty() && settingsDirectory != null) {
            for (ResourceLocation id : ForgeRegistries.ITEMS.getKeys()) {
                Item item = ForgeRegistries.ITEMS.getValue(id);
                if (item != null && item.isEdible()) {
                    String itemId = id.toString();
                    if (!isDefaultSpecialFood(itemId) && !specialFoodItemIds.contains(itemId)) {
                        normalFoodItemIds.add(itemId);
                    }
                }
            }
            changed = true;
            CustomTamingFramework.LOGGER.info("[CTF] Auto-populated {} normal food items from registry", normalFoodItemIds.size());
        }
        directEntityIds.removeAll(requiresTamingEntityIds);
        // Create missing pet SNBT files for all known entities
        boolean createdFiles = false;
        for (String entityId : allLoadedPetIds()) {
            Path petFile = settingsDirectory.resolve(PET_SETTINGS_DIR).resolve(toPetFileName(entityId));
            boolean isGeneral = generalEntityIds.contains(entityId);
            boolean allowCtf = !isGeneral;
            if (Files.notExists(petFile)) {
                try {
                    Files.writeString(petFile, petSettingToSnbt(entityId, "[CTFid:" + entityId + "]", null, allowCtf), StandardCharsets.UTF_8);
                    createdFiles = true;
                } catch (IOException e) {
                    CustomTamingFramework.LOGGER.error("Failed to create missing pet SNBT: {}", entityId, e);
                }
            }
            if (!petSettings.containsKey(entityId)) {
                petSettings.put(entityId, new PetSetting(entityId, "[CTFid:" + entityId + "]", new ListTag(),
                        defaultDataVariables(), allowCtf));
            }
        }
        // Repair missing nicknames in existing pet settings (upgrade from older versions)
        for (Map.Entry<String, PetSetting> entry : petSettings.entrySet()) {
            PetSetting ps = entry.getValue();
            if (ps.nickname().isBlank()) {
                savePetSetting(entry.getKey(), "[CTFid:" + entry.getKey() + "]", ps.guiTabs(), ps.dataVariables(), ps.allowCtf());
                changed = true;
            }
        }

        if (changed || createdFiles) {
            try {
                writeSnbt(settingsDirectory.resolve(BASIC_FILE), createBasicFromLoadedValues());
                CustomTamingFramework.LOGGER.info("[CTF_SELFTEST] config.snbt.defaults_repaired=PASS");
            } catch (IOException exception) {
                CustomTamingFramework.LOGGER.error("Failed to repair CTF_Settings defaults", exception);
            }
        }
    }

    private static boolean addMissing(List<String> target, List<String> defaults) {
        boolean changed = false;
        for (String value : defaults) {
            if (!target.contains(value)) {
                target.add(value);
                changed = true;
            }
        }
        return changed;
    }

    private static List<String> allLoadedPetIds() {
        List<String> ids = new ArrayList<>(requiresTamingEntityIds);
        ids.addAll(directEntityIds);
        ids.addAll(generalEntityIds);
        return List.copyOf(new LinkedHashSet<>(ids));
    }

    private static void classifyEntity(String id, EntityType<?> entityType, Set<String> requiresTaming, Set<String> directPets) {
        Class<? extends Entity> entityClass = entityType.getBaseClass();
        if (entityClass == null) {
            return;
        }
            if (SkeletonHorse.class.isAssignableFrom(entityClass) || ZombieHorse.class.isAssignableFrom(entityClass)) {
            directPets.add(id);
            return;
        }
        if (TamableAnimal.class.isAssignableFrom(entityClass) || AbstractHorse.class.isAssignableFrom(entityClass) || Camel.class.isAssignableFrom(entityClass)) {
            requiresTaming.add(id);
            return;
        }
        if (Saddleable.class.isAssignableFrom(entityClass) || Pig.class.isAssignableFrom(entityClass) || Strider.class.isAssignableFrom(entityClass)) {
            directPets.add(id);
        }
    }

    private static boolean isDefaultSpecialFood(String itemId) {
        return itemId.equals("minecraft:golden_apple") || itemId.equals("minecraft:enchanted_golden_apple") || itemId.equals("minecraft:golden_carrot");
    }

    private static CompoundTag createBasic(ScanResult scan) {
        CompoundTag root = new CompoundTag();
        CompoundTag tamingCore = new CompoundTag();
        tamingCore.putString("display_name", "驯兽核心");
        tamingCore.putString("recipe", "minecraft:redstone,minecraft:redstone,minecraft:redstone,minecraft:redstone,minecraft:diamond,minecraft:redstone,minecraft:redstone,minecraft:redstone,minecraft:redstone");
        tamingCore.putBoolean("consume_on_success", true);
        root.put("taming_core", tamingCore);

        CompoundTag pets = new CompoundTag();
        pets.putString("requires_taming_mode", "AUTO_TAMEABLE_PLUS_LIST");
        pets.put("requires_taming_entity_ids", toListTag(scan.requiresTamingPets()));
        pets.put("direct_entity_ids", toListTag(scan.directPets()));
        pets.put("general_entity_ids", toListTag(scan.generalPets()));
        root.put("pets", pets);

        CompoundTag foods = new CompoundTag();
        foods.putString("normal_foods_mode", "AUTO_PLAYER_FOOD_PLUS_LIST");
        foods.put("normal_food_item_ids", toListTag(scan.normalFoods()));
        foods.put("special_food_item_ids", toListTag(scan.specialFoods()));
        foods.put("disabled_food_item_ids", new ListTag());
        foods.put("special_item_ids", new ListTag());
        foods.putBoolean("consume_on_success", true);
        root.put("pet_foods", foods);
        root.put("settings", CtfUiSettings.defaults());
        return root;
    }

    private static CompoundTag createBasicFromLoadedValues() {
        CompoundTag root = new CompoundTag();
        CompoundTag tamingCore = new CompoundTag();
        tamingCore.putString("display_name", tamingCoreDisplayName);
        tamingCore.putString("recipe", tamingCoreRecipe);
        tamingCore.putBoolean("consume_on_success", tamingCoreConsumeOnSuccess);
        root.put("taming_core", tamingCore);

        CompoundTag pets = new CompoundTag();
        pets.putString("requires_taming_mode", requiresTamingMode);
        pets.put("requires_taming_entity_ids", toListTag(requiresTamingEntityIds));
        pets.put("direct_entity_ids", toListTag(directEntityIds));
        pets.put("general_entity_ids", toListTag(generalEntityIds));
        root.put("pets", pets);

        CompoundTag foods = new CompoundTag();
        foods.putString("normal_foods_mode", normalFoodsMode);
        foods.put("normal_food_item_ids", toListTag(normalFoodItemIds));
        foods.put("special_food_item_ids", toListTag(specialFoodItemIds));
        foods.put("disabled_food_item_ids", toListTag(disabledFoodItemIds));
        foods.put("special_item_ids", toListTag(specialItemIds));
        foods.putBoolean("consume_on_success", usableItemsConsumeOnSuccess);
        root.put("pet_foods", foods);
        root.put("settings", CtfUiSettings.sanitize(uiSettings));
        return root;
    }

    private static CompoundTag defaultDataVariables() {
        CompoundTag dataVars = new CompoundTag();
        CompoundTag evolutionPoints = new CompoundTag();
        evolutionPoints.putString("current", "0");
        evolutionPoints.putString("total", "0");
        evolutionPoints.putString("total_mode", "cumulative");
        evolutionPoints.put("stages", new ListTag());
        dataVars.put("EvolutionPoints", evolutionPoints);
        return dataVars;
    }

    private static CompoundTag defaultGeneralGui() {
        CompoundTag root = new CompoundTag();
        CompoundTag dataVars = defaultDataVariables();
        root.put("data_variables", dataVars);
        CompoundTag petEvolution = new CompoundTag();
        petEvolution.putString("title", "宠物本体进化路线");
        petEvolution.putBoolean("enabled", true);
        petEvolution.putInt("order", 0);
        petEvolution.putString("summary", "玩家通过普通食物或特殊食物获得进化点，用于强化基础数值并解锁技能。");
        root.put("pet_evolution", petEvolution);

        CompoundTag equipmentEvolution = new CompoundTag();
        equipmentEvolution.putString("title", "装备进化路线");
        equipmentEvolution.putBoolean("enabled", true);
        equipmentEvolution.putInt("order", 1);
        equipmentEvolution.put("slots", toListTag(List.of(PetEquipment.HEAD.name(), PetEquipment.BODY.name(), PetEquipment.FORE_LIMB.name(), PetEquipment.HIND_LIMB.name(), PetEquipment.TAIL.name())));
        equipmentEvolution.putString("summary", "预设装备部位：头部、躯干、前肢、后肢、尾部。");
        root.put("equipment_evolution", equipmentEvolution);

        return root;
    }

    public static ListTag generalGuiTabList() {
        ListTag tabs = new ListTag();
        // Collect keys with their order values, sort by order
        List<String> sortedKeys = new ArrayList<>();
        java.util.Map<String, Integer> orderMap = new java.util.HashMap<>();
        for (String key : generalGui.getAllKeys()) {
            if (generalGui.contains(key, 10) && !key.equals("data_variables")) {
                sortedKeys.add(key);
                CompoundTag data = generalGui.getCompound(key);
                orderMap.put(key, data.contains("order") ? data.getInt("order") : 999);
            }
        }
        sortedKeys.sort(java.util.Comparator.comparingInt(k -> orderMap.getOrDefault(k, 999)));
        for (String key : sortedKeys) {
            if (generalGui.contains(key, 10)) {
                CompoundTag tabData = generalGui.getCompound(key).copy();
                tabData.putString("id", key);
                if (!tabData.contains("title")) tabData.putString("title", key);
                if (!tabData.contains("enabled")) tabData.putBoolean("enabled", true);
                if (!tabData.contains("summary")) tabData.putString("summary", "");
                if (!tabData.contains("order")) tabData.putInt("order", 999);
                tabs.add(tabData);
            }
        }
        return tabs;
    }

    public static ListTag importGeneralToPet(String entityId) {
        ListTag generalTabs = generalGuiTabList();
        ListTag imported = generalTabs.copy();
        CompoundTag importedDataVariables = generalGui.contains("data_variables", 10)
                ? generalGui.getCompound("data_variables").copy()
                : defaultDataVariables();
        // Preserve existing nickname — import overwrites tabs and data-variable definitions.
        String nickname = "";
        PetSetting existing = petSettings.get(entityId);
        if (existing != null) {
            nickname = existing.nickname();
        }
        if (nickname.isBlank()) {
            nickname = "[CTFid:" + entityId + "]";
        }
        savePetSetting(entityId, nickname, imported, importedDataVariables);
        return imported;
    }

    // ── Package-access helpers for CtFConfigHelper ──

    public static String prettyPrintTag(CompoundTag tag) {
        return prettySnbt(tag);
    }

    public static void replaceNormalFoodIds(List<String> ids) {
        normalFoodItemIds.clear();
        normalFoodItemIds.addAll(ids);
    }

    public static void replaceSpecialFoodIds(List<String> ids) {
        specialFoodItemIds.clear();
        specialFoodItemIds.addAll(ids);
    }

    public static void replaceDisabledFoodIds(List<String> ids) {
        disabledFoodItemIds.clear();
        disabledFoodItemIds.addAll(ids);
    }

    public static void replaceGeneralEntityIds(List<String> ids) {
        generalEntityIds.clear();
        generalEntityIds.addAll(ids);
    }

    public static void replaceSpecialItemIds(List<String> ids) {
        specialItemIds.clear();
        specialItemIds.addAll(ids);
    }

    public static void updateTamingCore(String displayName, String recipe, boolean consumeOnSuccess) {
        tamingCoreDisplayName = displayName;
        tamingCoreRecipe = recipe;
        tamingCoreConsumeOnSuccess = consumeOnSuccess;
    }

    private static void resetToDefaults() {
        tamingCoreDisplayName = "驯兽核心";
        tamingCoreRecipe = "minecraft:redstone,minecraft:redstone,minecraft:redstone,minecraft:redstone,minecraft:diamond,minecraft:redstone,minecraft:redstone,minecraft:redstone,minecraft:redstone";
        tamingCoreConsumeOnSuccess = true;
        requiresTamingMode = "AUTO_TAMEABLE_PLUS_LIST";
        normalFoodsMode = "AUTO_PLAYER_FOOD_PLUS_LIST";
        usableItemsConsumeOnSuccess = true;
        requiresTamingEntityIds.clear();
        directEntityIds.clear();
        normalFoodItemIds.clear();
        specialFoodItemIds.clear();
        disabledFoodItemIds.clear();
        specialItemIds.clear();
        generalEntityIds.clear();
        petSettings.clear();
        generalGui = defaultGeneralGui();
        uiSettings = CtfUiSettings.defaults();
        basicSettingsNeedsRepair = false;
    }

    private static ListTag toListTag(List<String> values) {
        ListTag list = new ListTag();
        for (String value : values) {
            list.add(StringTag.valueOf(value));
        }
        return list;
    }

    private static void loadStringList(ListTag source, List<String> target) {
        target.clear();
        for (int index = 0; index < source.size(); index++) {
            target.add(source.getString(index));
        }
    }

    private static CompoundTag readSnbt(Path path) throws IOException {
        try {
            return TagParser.parseTag(Files.readString(path, StandardCharsets.UTF_8));
        } catch (Exception exception) {
            throw new IOException("Invalid SNBT file: " + path, exception);
        }
    }

    private static void writeSnbt(Path path, CompoundTag tag) throws IOException {
        Files.createDirectories(path.getParent());
        Files.writeString(path, prettySnbt(tag), StandardCharsets.UTF_8);
    }

    /** Build SNBT string with id/nickname/allowCTF first, then tabs — never relies on HashMap ordering. */
    private static String petSettingToSnbt(String entityId, String nickname, ListTag guiTabs, boolean allowCtf) {
        return petSettingToSnbt(entityId, nickname, guiTabs, defaultDataVariables(), allowCtf);
    }

    private static String petSettingToSnbt(String entityId, String nickname, ListTag guiTabs,
                                          CompoundTag dataVariables, boolean allowCtf) {
        StringBuilder sb = new StringBuilder();
        sb.append("{\n    id:\"").append(snbtEscape(entityId)).append("\",\n");
        sb.append("    nickname:\"").append(snbtEscape(nickname)).append("\",\n");
        sb.append("    allowCTF:").append(allowCtf);
        CompoundTag safeDataVariables = dataVariables != null && !dataVariables.isEmpty()
                ? dataVariables.copy() : defaultDataVariables();
        sb.append(",\n    data_variables:").append(prettySnbt(safeDataVariables));
        if (guiTabs != null) {
            for (int i = 0; i < guiTabs.size(); i++) {
                CompoundTag tab = guiTabs.getCompound(i);
                String tabId = tab.getString("id");
                if (!tabId.isBlank()) {
                    CompoundTag tabData = tab.copy();
                    tabData.remove("id");
                    sb.append(",\n    ").append(tabId).append(":").append(prettySnbt(tabData));
                }
            }
        }
        sb.append("\n}");
        return sb.toString();
    }

    private static String snbtEscape(String value) {
        return value.replace("\\", "\\\\").replace("\"", "\\\"");
    }

    // ── Pretty SNBT printer (multi-line with indentation) ──

    private static String prettySnbt(CompoundTag tag) {
        StringBuilder sb = new StringBuilder();
        writePretty(sb, tag, 0);
        return sb.toString();
    }

    private static void writePretty(StringBuilder sb, Tag tag, int indent) {
        String indentUnit = "    ";
        String nlIndent = "\n" + indentUnit.repeat(indent + 1);
        String closeIndent = indentUnit.repeat(indent);
        if (tag instanceof CompoundTag compound) {
            sb.append("{");
            boolean first = true;
            for (String key : compound.getAllKeys()) {
                if (!first) sb.append(",");
                first = false;
                sb.append(nlIndent).append(key).append(":");
                writePretty(sb, compound.get(key), indent + 1);
            }
            if (!first) sb.append("\n").append(closeIndent);
            sb.append("}");
        } else if (tag instanceof ListTag list) {
            if (list.isEmpty()) {
                sb.append("[]");
                return;
            }
            boolean allSimple = true;
            for (int i = 0; i < list.size() && allSimple; i++) {
                Tag e = list.get(i);
                if (e instanceof CompoundTag || e instanceof ListTag) allSimple = false;
            }
            if (allSimple) {
                sb.append("[");
                for (int i = 0; i < list.size(); i++) {
                    if (i > 0) sb.append(",");
                    writePretty(sb, list.get(i), indent);
                }
                sb.append("]");
            } else {
                sb.append("[");
                for (int i = 0; i < list.size(); i++) {
                    if (i > 0) sb.append(",");
                    sb.append(nlIndent);
                    writePretty(sb, list.get(i), indent + 1);
                }
                sb.append("\n").append(closeIndent).append("]");
            }
        } else if (tag instanceof StringTag str) {
            sb.append("\"").append(snbtEscape(str.getAsString())).append("\"");
        } else if (tag instanceof ByteTag bt) {
            sb.append(bt.getAsByte()).append("b");
        } else if (tag instanceof ShortTag st) {
            sb.append(st.getAsShort()).append("s");
        } else if (tag instanceof IntTag it) {
            sb.append(it.getAsInt());
        } else if (tag instanceof LongTag lt) {
            sb.append(lt.getAsLong()).append("L");
        } else if (tag instanceof FloatTag ft) {
            sb.append(ft.getAsFloat()).append("f");
        } else if (tag instanceof DoubleTag dt) {
            sb.append(dt.getAsDouble()).append("d");
        } else {
            sb.append(tag.getAsString());
        }
    }

    private static String toPetFileName(String entityId) {
        return entityId.replace(':', '_') + ".snbt";
    }

    public record PetSetting(String id, String nickname, ListTag guiTabs, CompoundTag dataVariables, boolean allowCtf) {
        public PetSetting(String id, String nickname, ListTag guiTabs, boolean allowCtf) {
            this(id, nickname, guiTabs, defaultDataVariables(), allowCtf);
        }

        public PetSetting {
            guiTabs = guiTabs != null ? guiTabs.copy() : new ListTag();
            dataVariables = dataVariables != null && !dataVariables.isEmpty()
                    ? dataVariables.copy() : defaultDataVariables();
        }
    }

    private record ScanResult(List<String> requiresTamingPets, List<String> directPets, List<String> generalPets, List<String> normalFoods, List<String> specialFoods) {
        private List<String> allPets() {
            List<String> all = new ArrayList<>(requiresTamingPets);
            all.addAll(directPets);
            return List.copyOf(new LinkedHashSet<>(all));
        }
    }
}
