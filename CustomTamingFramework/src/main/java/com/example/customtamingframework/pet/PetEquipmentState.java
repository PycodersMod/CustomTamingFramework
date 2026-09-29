package com.example.customtamingframework.pet;

import net.minecraft.nbt.CompoundTag;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.ItemStack;
import net.minecraftforge.registries.ForgeRegistries;

public record PetEquipmentState(String head, String body, String foreLimb, String hindLimb, String tail) {
    private static final String ROOT = "CustomTamingFrameworkEquipment";

    public static PetEquipmentState empty() {
        return new PetEquipmentState("", "", "", "", "");
    }

    public static PetEquipmentState get(LivingEntity entity) {
        CompoundTag root = entity.getPersistentData().getCompound(ROOT);
        return new PetEquipmentState(root.getString("Head"), root.getString("Body"), root.getString("ForeLimb"), root.getString("HindLimb"), root.getString("Tail"));
    }

    public static void save(LivingEntity entity, PetEquipmentState state) {
        CompoundTag root = new CompoundTag();
        root.putString("Head", state.head());
        root.putString("Body", state.body());
        root.putString("ForeLimb", state.foreLimb());
        root.putString("HindLimb", state.hindLimb());
        root.putString("Tail", state.tail());
        entity.getPersistentData().put(ROOT, root);
    }

    /**
     * Remove all equipment state from this entity.
     * Drops item stacks for each non-empty equipped slot at the entity's position.
     */
    public static void remove(LivingEntity entity) {
        PetEquipmentState state = get(entity);
        dropIfPresent(entity, state.head());
        dropIfPresent(entity, state.body());
        dropIfPresent(entity, state.foreLimb());
        dropIfPresent(entity, state.hindLimb());
        dropIfPresent(entity, state.tail());
        entity.getPersistentData().remove(ROOT);
    }

    private static void dropIfPresent(LivingEntity entity, String itemId) {
        if (itemId == null || itemId.isBlank()) return;
        ResourceLocation id = ResourceLocation.tryParse(itemId);
        if (id == null) return;
        var item = ForgeRegistries.ITEMS.getValue(id);
        if (item == null) return;
        entity.spawnAtLocation(new ItemStack(item));
    }

    public PetEquipmentState equipHeadModule(String itemId) {
        return new PetEquipmentState(itemId, body, foreLimb, hindLimb, tail);
    }

    public String summary() {
        return "HEAD=" + label(head) + ", BODY=" + label(body) + ", FORE=" + label(foreLimb) + ", HIND=" + label(hindLimb) + ", TAIL=" + label(tail);
    }

    private static String label(String value) {
        return value == null || value.isBlank() ? "empty" : value;
    }
}
