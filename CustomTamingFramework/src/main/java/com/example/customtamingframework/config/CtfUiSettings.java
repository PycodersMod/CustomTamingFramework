package com.example.customtamingframework.config;

import net.minecraft.nbt.CompoundTag;

public final class CtfUiSettings {
    public static final String SHOW_NON_OP_GENERAL_CONFIG_SCREEN = "show_non_op_general_config_screen";
    public static final String SHOW_NON_OP_PET_LIST_BUTTON = "show_non_op_pet_list_button";
    public static final String SHOW_NON_OP_FOOD_LIST_BUTTON = "show_non_op_food_list_button";
    public static final String SHOW_NON_OP_MORE_FOOD_BUTTON = "show_non_op_more_food_button";
    public static final String SHOW_NON_OP_TAMING_CORE_BUTTON = "show_non_op_taming_core_button";
    public static final String SHOW_NON_OP_DATA_LIST_BUTTON = "show_non_op_data_list_button";
    public static final String SHOW_NON_OP_TOTAL_SETTINGS_BUTTON = "show_non_op_total_settings_button";
    public static final String SHOW_NON_OP_MORE_FOOD_CHILD_BUTTON = SHOW_NON_OP_MORE_FOOD_BUTTON;
    public static final String SHOW_NON_OP_TOTAL_SETTINGS_CHILD_BUTTON = SHOW_NON_OP_TOTAL_SETTINGS_BUTTON;

    private CtfUiSettings() {
    }

    public static CompoundTag defaults() {
        CompoundTag tag = new CompoundTag();
        tag.putBoolean(SHOW_NON_OP_GENERAL_CONFIG_SCREEN, false);
        tag.putBoolean(SHOW_NON_OP_PET_LIST_BUTTON, true);
        tag.putBoolean(SHOW_NON_OP_FOOD_LIST_BUTTON, true);
        tag.putBoolean(SHOW_NON_OP_MORE_FOOD_BUTTON, true);
        tag.putBoolean(SHOW_NON_OP_TAMING_CORE_BUTTON, true);
        tag.putBoolean(SHOW_NON_OP_DATA_LIST_BUTTON, true);
        tag.putBoolean(SHOW_NON_OP_TOTAL_SETTINGS_BUTTON, true);
        return tag;
    }

    public static CompoundTag sanitize(CompoundTag raw) {
        CompoundTag tag = defaults();
        if (raw == null) {
            return tag;
        }
        for (String key : tag.getAllKeys()) {
            if (raw.contains(key)) {
                tag.putBoolean(key, raw.getBoolean(key));
            }
        }
        return tag;
    }

    public static boolean visibleForNonOp(CompoundTag settings, String key) {
        return sanitize(settings).getBoolean(key);
    }

    public static CompoundTag set(CompoundTag settings, String key, boolean value) {
        CompoundTag tag = sanitize(settings);
        if (tag.contains(key)) {
            tag.putBoolean(key, value);
        }
        return tag;
    }
}
