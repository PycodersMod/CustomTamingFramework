package com.pycoder.customtamingframework.pet;

import com.pycoder.customtamingframework.config.CtfSnbtConfig;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.TamableAnimal;
import net.minecraft.world.entity.animal.camel.Camel;
import net.minecraft.world.entity.animal.horse.AbstractHorse;
import net.minecraft.world.entity.animal.horse.SkeletonHorse;
import net.minecraft.world.entity.animal.horse.ZombieHorse;
import net.minecraft.world.entity.animal.Pig;
import net.minecraft.world.entity.monster.Strider;
import net.minecraftforge.registries.ForgeRegistries;

public final class AllowedPetRegistry {
    private AllowedPetRegistry() {
    }

    public static boolean isAllowed(LivingEntity entity) {
        ResourceLocation id = ForgeRegistries.ENTITY_TYPES.getKey(entity.getType());
        if (id == null) return false;
        String idStr = id.toString();

        // Must be a known entity type in our lists
        boolean known = isDirectPet(entity) || requiresTaming(entity) || CtfSnbtConfig.generalEntityIds().contains(idStr);
        if (!known) return false;

        // Check per-pet allowCTF setting
        var setting = CtfSnbtConfig.petSetting(idStr);

        // No setting: allow for tamable/direct pets, deny for general entities
        if (setting == null) {
            return !CtfSnbtConfig.generalEntityIds().contains(idStr);
        }

        // Has setting: respect its value
        return setting.allowCtf();
    }

    public static boolean isDirectPet(LivingEntity entity) {
        if (entity instanceof SkeletonHorse || entity instanceof ZombieHorse || entity instanceof Pig || entity instanceof Strider || entity instanceof Camel) {
            return true;
        }
        ResourceLocation id = ForgeRegistries.ENTITY_TYPES.getKey(entity.getType());
        return id != null && CtfSnbtConfig.directEntityIds().contains(id.toString());
    }

    public static boolean requiresTaming(LivingEntity entity) {
        if (entity instanceof TamableAnimal || entity instanceof AbstractHorse) {
            return true;
        }
        ResourceLocation id = ForgeRegistries.ENTITY_TYPES.getKey(entity.getType());
        return id != null && CtfSnbtConfig.requiresTamingEntityIds().contains(id.toString());
    }
}
