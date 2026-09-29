package com.example.customtamingframework.pet;

import com.example.customtamingframework.config.CtfSnbtConfig;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.ItemStack;
import net.minecraftforge.registries.ForgeRegistries;

public final class UsablePetItemRegistry {
    private UsablePetItemRegistry() {
    }

    public static boolean isUsable(ItemStack stack) {
        if (stack.isEmpty()) {
            return false;
        }
        ResourceLocation id = ForgeRegistries.ITEMS.getKey(stack.getItem());
        String itemId = id != null ? id.toString() : "";
        if (CtfSnbtConfig.disabledFoodItemIds().contains(itemId)) {
            return false;
        }
        return isNormalFood(stack) || isSpecialFood(stack) || isSpecialItem(stack);
    }

    public static boolean isSpecialItem(ItemStack stack) {
        if (stack.isEmpty()) {
            return false;
        }
        ResourceLocation id = ForgeRegistries.ITEMS.getKey(stack.getItem());
        String itemId = id != null ? id.toString() : "";
        if (CtfSnbtConfig.disabledFoodItemIds().contains(itemId)) {
            return false;
        }
        return id != null && CtfSnbtConfig.specialItemIds().contains(itemId);
    }

    public static boolean isNormalFood(ItemStack stack) {
        if (stack.isEmpty()) {
            return false;
        }
        ResourceLocation id = ForgeRegistries.ITEMS.getKey(stack.getItem());
        String itemId = id != null ? id.toString() : "";
        if (CtfSnbtConfig.disabledFoodItemIds().contains(itemId)) {
            return false;
        }
        if (id != null && CtfSnbtConfig.specialFoodItemIds().contains(itemId)) {
            return false;
        }
        if (stack.isEdible()) {
            return true;
        }
        return id != null && CtfSnbtConfig.normalFoodItemIds().contains(itemId);
    }

    public static boolean isSpecialFood(ItemStack stack) {
        if (stack.isEmpty()) {
            return false;
        }
        ResourceLocation id = ForgeRegistries.ITEMS.getKey(stack.getItem());
        String itemId = id != null ? id.toString() : "";
        if (CtfSnbtConfig.disabledFoodItemIds().contains(itemId)) {
            return false;
        }
        return id != null && CtfSnbtConfig.specialFoodItemIds().contains(itemId);
    }

    public static int normalFoodPoints(ItemStack stack) {
        if (stack.getFoodProperties(null) == null) {
            return 1;
        }
        return Math.max(1, stack.getFoodProperties(null).getNutrition());
    }

    public static SpecialFoodEffect specialFoodEffect(ItemStack stack) {
        // Alpha keeps a built-in example special-food effect here.
        // Later OP-configured GUI/template values will replace this hook.
        return new SpecialFoodEffect(5, "sample_attribute_boost");
    }

    public record SpecialFoodEffect(int points, String unlockSkill) {
    }
}
