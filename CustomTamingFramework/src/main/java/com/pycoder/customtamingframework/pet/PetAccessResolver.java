package com.pycoder.customtamingframework.pet;

import com.pycoder.customtamingframework.config.CtfSnbtConfig;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.TamableAnimal;
import net.minecraft.world.entity.animal.horse.AbstractHorse;
import net.minecraftforge.registries.ForgeRegistries;

public final class PetAccessResolver {
    private PetAccessResolver() {
    }

    public static boolean canActivate(ServerPlayer player, LivingEntity entity) {
        if (AllowedPetRegistry.isDirectPet(entity)) {
            return true;
        }
        // General entities with allowCTF=true can be directly activated (no taming required)
        if (isGeneralEntityWithAllowCtf(entity)) {
            return true;
        }
        if (!AllowedPetRegistry.requiresTaming(entity)) {
            return false;
        }
        if (entity instanceof TamableAnimal tamable) {
            return tamable.isTame() && tamable.getOwnerUUID() != null && player.getUUID().equals(tamable.getOwnerUUID());
        }
        if (entity instanceof AbstractHorse horse) {
            return horse.isTamed() && horse.getOwnerUUID() != null && player.getUUID().equals(horse.getOwnerUUID());
        }
        return false;
    }

    public static boolean canOpenPetScreen(ServerPlayer player, LivingEntity entity) {
        if (player == null || entity == null) {
            return false;
        }
        if (!PetProgress.isEnabled(entity)) {
            return false;
        }
        if (player.hasPermissions(2)) {
            return true;
        }
        PetProgress progress = PetProgress.get(entity);
        if (progress.permissionFor(player.getUUID()) == PetProgress.AccessMode.SECRET) {
            return false;
        }
        return true;
    }

    public static boolean isOwnerMismatch(ServerPlayer player, LivingEntity entity) {
        if (player == null || entity == null) {
            return false;
        }
        if (entity instanceof TamableAnimal tamable) {
            return tamable.isTame() && tamable.getOwnerUUID() != null && !player.getUUID().equals(tamable.getOwnerUUID());
        }
        if (entity instanceof AbstractHorse horse) {
            return horse.isTamed() && horse.getOwnerUUID() != null && !player.getUUID().equals(horse.getOwnerUUID());
        }
        return false;
    }

    /** Check if entity is a general (non-tamable/non-rideable) entity with allowCTF=true. */
    private static boolean isGeneralEntityWithAllowCtf(LivingEntity entity) {
        ResourceLocation id = ForgeRegistries.ENTITY_TYPES.getKey(entity.getType());
        if (id == null) return false;
        if (!CtfSnbtConfig.generalEntityIds().contains(id.toString())) return false;
        var setting = CtfSnbtConfig.petSetting(id.toString());
        return setting != null && setting.allowCtf();
    }

    public static boolean canUsePet(ServerPlayer player, LivingEntity entity) {
        return canOpenPetScreen(player, entity);
    }
}
