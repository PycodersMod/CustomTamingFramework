package com.example.customtamingframework.config;

import net.minecraftforge.common.ForgeConfigSpec;

import java.util.List;

public final class CtfCommonConfig {
    public static final ForgeConfigSpec SPEC;
    public static final ForgeConfigSpec.BooleanValue TAMING_CORE_CONSUME_ON_SUCCESS;
    public static final ForgeConfigSpec.ConfigValue<String> TAMING_CORE_RECIPE_PATTERN;
    public static final ForgeConfigSpec.ConfigValue<String> TAMING_REQUIRED_PETS_MODE;
    public static final ForgeConfigSpec.ConfigValue<List<? extends String>> TAMING_REQUIRED_ENTITY_IDS;
    public static final ForgeConfigSpec.ConfigValue<List<? extends String>> DIRECT_ENTITY_IDS;
    public static final ForgeConfigSpec.ConfigValue<String> NORMAL_FOODS_MODE;
    public static final ForgeConfigSpec.ConfigValue<List<? extends String>> NORMAL_FOOD_ITEM_IDS;
    public static final ForgeConfigSpec.ConfigValue<List<? extends String>> SPECIAL_FOOD_ITEM_IDS;
    public static final ForgeConfigSpec.BooleanValue USABLE_ITEMS_CONSUME_ON_SUCCESS;

    static {
        ForgeConfigSpec.Builder builder = new ForgeConfigSpec.Builder();
        builder.push("taming_core");
        TAMING_CORE_RECIPE_PATTERN = builder.comment("Default recipe as 9 comma-separated item IDs (row-major). Empty entries = no item.").define("recipe", "minecraft:redstone,minecraft:redstone,minecraft:redstone,minecraft:redstone,minecraft:diamond,minecraft:redstone,minecraft:redstone,minecraft:redstone,minecraft:redstone");
        TAMING_CORE_CONSUME_ON_SUCCESS = builder.define("consume_on_success", true);
        builder.pop();

        builder.push("pets");
        TAMING_REQUIRED_PETS_MODE = builder.comment("AUTO_TAMEABLE_PLUS_LIST treats vanilla TamableAnimal as requiring successful tame/owner check before Taming Core can be used.").define("requires_taming_mode", "AUTO_TAMEABLE_PLUS_LIST");
        TAMING_REQUIRED_ENTITY_IDS = builder.comment("Entities in this list must pass the tame/owner check before Taming Core can enable CTF.").defineListAllowEmpty("requires_taming_entity_ids", List.of("minecraft:wolf", "minecraft:cat", "minecraft:parrot", "minecraft:horse", "minecraft:donkey", "minecraft:mule", "minecraft:llama", "minecraft:trader_llama"), value -> value instanceof String);
        DIRECT_ENTITY_IDS = builder.comment("Entities in this list can use Taming Core directly without a tame/owner check. Includes vanilla mounts that do not use the vanilla tame-owner flow.").defineListAllowEmpty("direct_entity_ids", List.of("minecraft:skeleton_horse", "minecraft:zombie_horse", "minecraft:pig", "minecraft:strider", "minecraft:camel"), value -> value instanceof String);
        builder.pop();

        builder.push("pet_foods");
        NORMAL_FOODS_MODE = builder.comment("AUTO_PLAYER_FOOD_PLUS_LIST makes any edible player food a normal pet food. Normal food effects are calculated from FoodProperties.nutrition and are not individually tuned by OP players.").define("normal_foods_mode", "AUTO_PLAYER_FOOD_PLUS_LIST");
        NORMAL_FOOD_ITEM_IDS = builder.comment("Vanilla/player edible normal foods plus extra integration-pack items. Effects still use the normal food formula.").defineListAllowEmpty("normal_food_item_ids", List.of("minecraft:apple", "minecraft:mushroom_stew", "minecraft:bread", "minecraft:porkchop", "minecraft:cooked_porkchop", "minecraft:cod", "minecraft:salmon", "minecraft:tropical_fish", "minecraft:pufferfish", "minecraft:cooked_cod", "minecraft:cooked_salmon", "minecraft:cookie", "minecraft:melon_slice", "minecraft:dried_kelp", "minecraft:beef", "minecraft:cooked_beef", "minecraft:chicken", "minecraft:cooked_chicken", "minecraft:rotten_flesh", "minecraft:spider_eye", "minecraft:carrot", "minecraft:potato", "minecraft:baked_potato", "minecraft:poisonous_potato", "minecraft:pumpkin_pie", "minecraft:rabbit", "minecraft:cooked_rabbit", "minecraft:rabbit_stew", "minecraft:mutton", "minecraft:cooked_mutton", "minecraft:beetroot", "minecraft:beetroot_soup", "minecraft:sweet_berries", "minecraft:glow_berries", "minecraft:honey_bottle", "minecraft:suspicious_stew"), value -> value instanceof String);
        SPECIAL_FOOD_ITEM_IDS = builder.comment("Special foods only decide which items enter the special-rule path. Specific effects are configured later by OP players in the in-game GUI/template.").defineListAllowEmpty("special_food_item_ids", List.of("minecraft:golden_apple", "minecraft:enchanted_golden_apple", "minecraft:golden_carrot"), value -> value instanceof String);
        USABLE_ITEMS_CONSUME_ON_SUCCESS = builder.define("consume_on_success", true);
        builder.pop();
        SPEC = builder.build();
    }

    private CtfCommonConfig() {
    }
}
