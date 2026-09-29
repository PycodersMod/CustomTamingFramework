package com.pycoder.customtamingframework.pet;

import com.pycoder.customtamingframework.network.PetBroadcasts;
import net.minecraft.advancements.Advancement;
import net.minecraft.advancements.AdvancementProgress;
import net.minecraft.core.Holder;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.stats.Stats;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.entity.raid.Raider;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.biome.Biome;
import net.minecraft.world.level.levelgen.structure.StructureStart;
import net.minecraftforge.registries.ForgeRegistries;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

public final class PetProgressionTracker {
    private static final ResourceLocation ADV_KILL_A_MOB = ResourceLocation.fromNamespaceAndPath("minecraft", "adventure/kill_a_mob");
    private static final ResourceLocation ADV_KILL_ALL_MOBS = ResourceLocation.fromNamespaceAndPath("minecraft", "adventure/kill_all_mobs");
    private static final ResourceLocation ADV_VOLUNTARY_EXILE = ResourceLocation.fromNamespaceAndPath("minecraft", "adventure/voluntary_exile");
    private static final ResourceLocation ADV_KILL_DRAGON = ResourceLocation.fromNamespaceAndPath("minecraft", "end/kill_dragon");
    private static final ResourceLocation ADV_UNEASY_ALLIANCE = ResourceLocation.fromNamespaceAndPath("minecraft", "nether/uneasy_alliance");
    private static final ResourceLocation ADV_ADVENTURING_TIME = ResourceLocation.fromNamespaceAndPath("minecraft", "adventure/adventuring_time");
    private static final ResourceLocation ADV_EXPLORE_NETHER = ResourceLocation.fromNamespaceAndPath("minecraft", "nether/explore_nether");
    private static final ResourceLocation ADV_FIND_BASTION = ResourceLocation.fromNamespaceAndPath("minecraft", "nether/find_bastion");
    private static final ResourceLocation ADV_FIND_FORTRESS = ResourceLocation.fromNamespaceAndPath("minecraft", "nether/find_fortress");
    private static final ResourceLocation ADV_FIND_END_CITY = ResourceLocation.fromNamespaceAndPath("minecraft", "end/find_end_city");
    private static final ResourceLocation ADV_FOLLOW_ENDER_EYE = ResourceLocation.fromNamespaceAndPath("minecraft", "story/follow_ender_eye");
    private static final ResourceLocation ADV_ENTER_THE_END = ResourceLocation.fromNamespaceAndPath("minecraft", "story/enter_the_end");
    private static final ResourceLocation ADV_ENTER_THE_NETHER = ResourceLocation.fromNamespaceAndPath("minecraft", "story/enter_the_nether");

    private PetProgressionTracker() {
    }

    public static void handlePetKillProgress(LivingEntity victim, DamageSource source) {
        if (victim == null || source == null || victim.level().isClientSide()) {
            return;
        }

        LivingEntity pet = resolveAttackingPet(source);
        if (pet == null || pet == victim) {
            return;
        }

        ServerPlayer owner = resolveOnlineOwner(pet);
        if (owner == null) {
            return;
        }

        ServerLevel serverLevel = victim.level() instanceof ServerLevel level ? level : null;
        if (serverLevel == null) {
            return;
        }

        owner.killedEntity(serverLevel, victim);
        if (victim instanceof Player) {
            owner.awardStat(Stats.PLAYER_KILLS);
        } else {
            owner.awardStat(Stats.MOB_KILLS);
        }

        boolean announce = owner.getServer() != null
                && owner.getServer().getGameRules().getBoolean(net.minecraft.world.level.GameRules.RULE_ANNOUNCE_ADVANCEMENTS);

        List<Advancement> completed = new ArrayList<>();
        if (awardCriterion(owner, ADV_KILL_A_MOB, entityCriterion(victim), announce)) {
            completed.add(resolveAdvancement(owner, ADV_KILL_A_MOB));
        }
        if (awardCriterion(owner, ADV_KILL_ALL_MOBS, entityCriterion(victim), announce)) {
            completed.add(resolveAdvancement(owner, ADV_KILL_ALL_MOBS));
        }
        if (victim instanceof Raider raider && raider.isPatrolLeader()) {
            if (awardCriterion(owner, ADV_VOLUNTARY_EXILE, "voluntary_exile", announce)) {
                completed.add(resolveAdvancement(owner, ADV_VOLUNTARY_EXILE));
            }
        }
        if (victim.getType() == net.minecraft.world.entity.EntityType.ENDER_DRAGON) {
            if (awardCriterion(owner, ADV_KILL_DRAGON, "killed_dragon", announce)) {
                completed.add(resolveAdvancement(owner, ADV_KILL_DRAGON));
            }
        }
        if (victim.getType() == net.minecraft.world.entity.EntityType.GHAST
                && victim.level().dimension() == Level.OVERWORLD) {
            if (awardCriterion(owner, ADV_UNEASY_ALLIANCE, "killed_ghast", announce)) {
                completed.add(resolveAdvancement(owner, ADV_UNEASY_ALLIANCE));
            }
        }

        if (announce) {
            for (Advancement advancement : completed) {
                if (advancement != null) {
                    PetBroadcasts.broadcastPetAssistedAdvancement(owner.getServer(), owner, pet, advancement);
                }
            }
        }
    }

    public static void handlePetLocationProgress(LivingEntity pet) {
        if (pet == null || pet.level().isClientSide() || !PetProgress.isEnabled(pet)) {
            return;
        }

        ServerPlayer owner = resolveOnlineOwner(pet);
        if (owner == null || owner.getServer() == null || !(pet.level() instanceof ServerLevel serverLevel)) {
            return;
        }

        boolean announce = owner.getServer().getGameRules().getBoolean(net.minecraft.world.level.GameRules.RULE_ANNOUNCE_ADVANCEMENTS);
        List<Advancement> completed = new ArrayList<>();

        ResourceLocation biomeId = currentBiomeId(serverLevel, pet.blockPosition());
        if (biomeId != null) {
            if (awardCriterion(owner, ADV_ADVENTURING_TIME, biomeId.toString(), announce)) {
                completed.add(resolveAdvancement(owner, ADV_ADVENTURING_TIME));
            }
            if (awardCriterion(owner, ADV_EXPLORE_NETHER, biomeId.toString(), announce)) {
                completed.add(resolveAdvancement(owner, ADV_EXPLORE_NETHER));
            }
        }

        awardStructureCriterion(owner, serverLevel, pet, ADV_FIND_BASTION, "bastion", announce, completed);
        awardStructureCriterion(owner, serverLevel, pet, ADV_FIND_FORTRESS, "fortress", announce, completed);
        awardStructureCriterion(owner, serverLevel, pet, ADV_FIND_END_CITY, "in_city", announce, completed);
        awardStructureCriterion(owner, serverLevel, pet, ADV_FOLLOW_ENDER_EYE, "in_stronghold", announce, completed);

        if (announce) {
            for (Advancement advancement : completed) {
                if (advancement != null) {
                    PetBroadcasts.broadcastPetAssistedAdvancement(owner.getServer(), owner, pet, advancement);
                }
            }
        }
    }

    public static void handlePetDimensionTravel(LivingEntity pet, ResourceKey<Level> destination) {
        if (pet == null || destination == null || pet.level().isClientSide() || !PetProgress.isEnabled(pet)) {
            return;
        }

        ServerPlayer owner = resolveOnlineOwner(pet);
        if (owner == null || owner.getServer() == null) {
            return;
        }

        boolean announce = owner.getServer().getGameRules().getBoolean(net.minecraft.world.level.GameRules.RULE_ANNOUNCE_ADVANCEMENTS);
        List<Advancement> completed = new ArrayList<>();

        if (destination.equals(Level.END)) {
            if (awardCriterion(owner, ADV_ENTER_THE_END, "entered_end", announce)) {
                completed.add(resolveAdvancement(owner, ADV_ENTER_THE_END));
            }
        } else if (destination.equals(Level.NETHER)) {
            if (awardCriterion(owner, ADV_ENTER_THE_NETHER, "entered_nether", announce)) {
                completed.add(resolveAdvancement(owner, ADV_ENTER_THE_NETHER));
            }
        }

        if (announce) {
            for (Advancement advancement : completed) {
                if (advancement != null) {
                    PetBroadcasts.broadcastPetAssistedAdvancement(owner.getServer(), owner, pet, advancement);
                }
            }
        }
    }

    private static void awardStructureCriterion(ServerPlayer owner, ServerLevel level, LivingEntity pet,
                                                ResourceLocation advancementId, String criterion,
                                                boolean announce, List<Advancement> completed) {
        if (owner == null || level == null || pet == null) {
            return;
        }
        Advancement advancement = resolveAdvancement(owner, advancementId);
        if (advancement == null || !advancement.getCriteria().containsKey(criterion)) {
            return;
        }
        StructureStart start = switch (criterion) {
            case "bastion" -> level.structureManager().getStructureWithPieceAt(pet.blockPosition(), ResourceKey.create(Registries.STRUCTURE, ResourceLocation.fromNamespaceAndPath("minecraft", "bastion_remnant")));
            case "fortress" -> level.structureManager().getStructureWithPieceAt(pet.blockPosition(), ResourceKey.create(Registries.STRUCTURE, ResourceLocation.fromNamespaceAndPath("minecraft", "fortress")));
            case "in_city" -> level.structureManager().getStructureWithPieceAt(pet.blockPosition(), ResourceKey.create(Registries.STRUCTURE, ResourceLocation.fromNamespaceAndPath("minecraft", "end_city")));
            case "in_stronghold" -> level.structureManager().getStructureWithPieceAt(pet.blockPosition(), ResourceKey.create(Registries.STRUCTURE, ResourceLocation.fromNamespaceAndPath("minecraft", "stronghold")));
            default -> StructureStart.INVALID_START;
        };
        if (!start.isValid()) {
            return;
        }
        if (awardCriterion(owner, advancementId, criterion, announce)) {
            completed.add(advancement);
        }
    }

    private static boolean awardCriterion(ServerPlayer owner, ResourceLocation advancementId, String criterion, boolean announce) {
        if (owner == null || owner.getServer() == null || advancementId == null || criterion == null || criterion.isBlank()) {
            return false;
        }
        Advancement advancement = resolveAdvancement(owner, advancementId);
        if (advancement == null || !advancement.getCriteria().containsKey(criterion)) {
            return false;
        }

        AdvancementProgress progress = owner.getAdvancements().getOrStartProgress(advancement);
        boolean before = progress.isDone();
        if (announce) {
            var rule = owner.getServer().getGameRules().getRule(net.minecraft.world.level.GameRules.RULE_ANNOUNCE_ADVANCEMENTS);
            boolean previous = rule.get();
            if (previous) {
                rule.set(false, owner.getServer());
            }
            try {
                owner.getAdvancements().award(advancement, criterion);
            } finally {
                if (previous) {
                    rule.set(true, owner.getServer());
                }
            }
        } else {
            owner.getAdvancements().award(advancement, criterion);
        }
        boolean after = owner.getAdvancements().getOrStartProgress(advancement).isDone();
        return !before && after;
    }

    private static Advancement resolveAdvancement(ServerPlayer owner, ResourceLocation advancementId) {
        if (owner == null || owner.serverLevel() == null || owner.serverLevel().getServer() == null || advancementId == null) {
            return null;
        }
        return owner.serverLevel().getServer().getAdvancements().getAdvancement(advancementId);
    }

    private static ResourceLocation currentBiomeId(ServerLevel level, net.minecraft.core.BlockPos pos) {
        Holder<Biome> biome = level.getBiome(pos);
        return biome.unwrapKey().map(ResourceKey::location).orElse(null);
    }

    private static String entityCriterion(LivingEntity entity) {
        if (entity == null) {
            return "";
        }
        ResourceLocation id = ForgeRegistries.ENTITY_TYPES.getKey(entity.getType());
        return id == null ? "" : id.toString();
    }

    private static ServerPlayer resolveOnlineOwner(LivingEntity pet) {
        if (pet == null || !PetProgress.isEnabled(pet)) {
            return null;
        }
        UUID ownerUuid = ownerUuid(PetProgress.get(pet).ownerPlayerUuid());
        if (ownerUuid == null) {
            return null;
        }
        MinecraftServer server = pet.level().getServer();
        if (server == null) {
            return null;
        }
        return server.getPlayerList().getPlayer(ownerUuid);
    }

    private static UUID ownerUuid(String raw) {
        if (raw == null || raw.isBlank()) {
            return null;
        }
        try {
            return UUID.fromString(raw.trim());
        } catch (IllegalArgumentException ignored) {
            return null;
        }
    }

    private static LivingEntity resolveAttackingPet(DamageSource source) {
        if (source == null) {
            return null;
        }
        Entity attacker = source.getEntity();
        if (attacker instanceof LivingEntity living && PetProgress.isEnabled(living)) {
            return living;
        }
        Entity direct = source.getDirectEntity();
        if (direct instanceof LivingEntity living && PetProgress.isEnabled(living)) {
            return living;
        }
        return null;
    }
}
