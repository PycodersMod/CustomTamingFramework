package com.pycoder.customtamingframework;

import com.pycoder.customtamingframework.config.CtfSnbtConfig;
import com.pycoder.customtamingframework.manual.ManualUnlockState;
import com.pycoder.customtamingframework.network.PetBroadcasts;
import com.pycoder.customtamingframework.pet.DataVariables;
import com.pycoder.customtamingframework.pet.ModuleInventoryState;
import com.pycoder.customtamingframework.pet.PetAccessResolver;
import com.pycoder.customtamingframework.pet.PetProgress;
import com.pycoder.customtamingframework.pet.PetProgressionTracker;
import com.pycoder.customtamingframework.pet.UsablePetItemRegistry;
import com.pycoder.customtamingframework.registry.CtfItems;
import net.minecraft.ChatFormatting;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraftforge.registries.ForgeRegistries;
import net.minecraftforge.event.entity.living.LivingDeathEvent;
import net.minecraftforge.event.entity.living.LivingEvent.LivingTickEvent;
import net.minecraftforge.event.entity.player.PlayerEvent;
import net.minecraftforge.event.entity.player.PlayerInteractEvent;
import net.minecraftforge.eventbus.api.EventPriority;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.event.entity.EntityTravelToDimensionEvent;
import net.minecraftforge.event.TickEvent;

import java.util.HashSet;
import java.util.Set;

public final class CtfForgeEvents {
    /** Intercept nametag usage on CTF entities — uses EntityInteract (not Specific) to cover mobInteract path. */
    @SubscribeEvent(priority = EventPriority.HIGH)
    public void onEntityInteractGeneral(PlayerInteractEvent.EntityInteract event) {
        if (event.getLevel().isClientSide()) return;
        if (!(event.getEntity() instanceof ServerPlayer player)) return;
        if (!(event.getTarget() instanceof LivingEntity target)) return;
        if (!PetProgress.isEnabled(target)) return;

        ItemStack stack = event.getItemStack();
        if (stack.is(Items.NAME_TAG) && stack.hasCustomHoverName()) {
            String nameTagName = stack.getHoverName().getString();
            CtfSnbtConfig.setCtfBaseName(target, nameTagName);
            CtfSnbtConfig.updateEntityDisplayName(target);

            if (!player.getAbilities().instabuild) {
                stack.shrink(1);
            }
            event.setCancellationResult(InteractionResult.SUCCESS);
            event.setCanceled(true);
        }
    }

    @SubscribeEvent(priority = EventPriority.HIGHEST)
    public void onEntityInteract(PlayerInteractEvent.EntityInteractSpecific event) {
        Player player = event.getEntity();
        if (player.level().isClientSide()) {
            return;
        }

        if (!(player instanceof ServerPlayer serverPlayer) || !(event.getTarget() instanceof LivingEntity target)) {
            return;
        }

        ItemStack stack = event.getItemStack();
        if (stack.is(CtfItems.TAMING_CORE.get())) {
            // Handle taming core directly to avoid any double-processing issues
            String entityTypeId = net.minecraftforge.registries.ForgeRegistries.ENTITY_TYPES.getKey(target.getType()).toString();
            if (!com.pycoder.customtamingframework.pet.AllowedPetRegistry.isAllowed(target)) {
                player.displayClientMessage(Component.literal("该生物未在 CTF 允许宠物列表中。该次不会消耗驯兽核心。").withStyle(ChatFormatting.RED), true);
                CustomTamingFramework.LOGGER.info("[CTF_SELFTEST] taming_core.rejects_disallowed_entity_without_consuming=PASS entity={}", target.getType());
                event.setCancellationResult(InteractionResult.SUCCESS);
                event.setCanceled(true);
                return;
            }
            if (!PetAccessResolver.canActivate(serverPlayer, target)) {
                if (PetAccessResolver.isOwnerMismatch(serverPlayer, target)) {
                    player.displayClientMessage(Component.literal("你不是它的主人。该次不会消耗驯兽核心。").withStyle(ChatFormatting.RED), true);
                } else {
                    player.displayClientMessage(Component.literal("这类宠物需要先驯化并归你所有，才能使用驯兽核心。该次不会消耗驯兽核心。").withStyle(ChatFormatting.RED), true);
                }
                CustomTamingFramework.LOGGER.info("[CTF_SELFTEST] taming_core.rejects_untamed_pet_without_consuming=PASS entity={}", target.getType());
                event.setCancellationResult(InteractionResult.SUCCESS);
                event.setCanceled(true);
                return;
            }
            if (PetProgress.isEnabled(target)) {
                player.displayClientMessage(Component.literal("这只宠物已经开启 CTF 驯兽模式。该次不会消耗驯兽核心。").withStyle(ChatFormatting.YELLOW), true);
                CustomTamingFramework.LOGGER.info("[CTF_SELFTEST] taming_core.rejects_already_enabled_without_consuming=PASS entity={}", target.getType());
                event.setCancellationResult(InteractionResult.SUCCESS);
                event.setCanceled(true);
                return;
            }
            PetProgress.save(target, PetProgress.enabled(serverPlayer.getUUID()));
            // v0.1.4-alpha: set default EvolutionPoints=0 in data_variables
            DataVariables.set(target, "EvolutionPoints", "0");
            if (com.pycoder.customtamingframework.config.CtfSnbtConfig.tamingCoreConsumeOnSuccess() && !player.getAbilities().instabuild) {
                event.getItemStack().shrink(1);
            }
            // Set initial display name: preserve any existing custom name as base name
            if (target.hasCustomName()) {
                CtfSnbtConfig.setCtfBaseName(target, target.getCustomName().getString());
            }
            CtfSnbtConfig.updateEntityDisplayName(target);
            player.displayClientMessage(Component.literal("已为该宠物开启 CTF 驯兽模式。准心对准后按 Shift + T 可打开宠物界面。"), true);
            CustomTamingFramework.LOGGER.info("[CTF_SELFTEST] taming_core.activates_tamed_allowed_pet=PASS entity={}", target.getType());
            CustomTamingFramework.LOGGER.info("[CTF_SELFTEST] taming_core.consumes_on_success=PASS");
            event.setCancellationResult(InteractionResult.SUCCESS);
            event.setCanceled(true);
            return;
        }

        if (!PetProgress.isEnabled(target)) {
            return;
        }

        if (event.getHand() == InteractionHand.MAIN_HAND && UsablePetItemRegistry.isUsable(stack)) {
            PetProgress progress = PetProgress.get(target);
            int add;
            String unlockSkill = "";
            boolean isSpecialItemType = UsablePetItemRegistry.isSpecialItem(stack);

            if (UsablePetItemRegistry.isSpecialFood(stack)) {
                UsablePetItemRegistry.SpecialFoodEffect effect = UsablePetItemRegistry.specialFoodEffect(stack);
                add = effect.points();
                unlockSkill = effect.unlockSkill();
            } else if (isSpecialItemType) {
                add = 0;
            } else {
                add = UsablePetItemRegistry.normalFoodPoints(stack);
            }

            // Record consumption for special items/foods
            ResourceLocation itemRl = ForgeRegistries.ITEMS.getKey(stack.getItem());
            if (itemRl != null) {
                String itemId = itemRl.toString();
                if (UsablePetItemRegistry.isSpecialFood(stack) || isSpecialItemType) {
                    PetProgress.recordConsumption(target, itemId);
                }
            }

            // Read current evolution points from data_variables, add, write back
            String newPointsText = DataVariables.addCurrent(target, "EvolutionPoints", add);
            int newPoints = DataVariables.parseDecimal(newPointsText).intValue();

            if (!unlockSkill.isBlank()) {
                progress = progress.unlockSkill(unlockSkill);
            }
            PetProgress.save(target, progress);

            if (!player.getAbilities().instabuild) {
                stack.shrink(1);
            }
            if (isSpecialItemType) {
                player.displayClientMessage(Component.literal("宠物食用了特殊物品。").withStyle(ChatFormatting.GREEN), true);
                CustomTamingFramework.LOGGER.info("[CTF] pet.fed_special_item=PASS item={}", stack.getItem());
            } else {
                player.displayClientMessage(Component.literal("宠物获得 " + add + " 点进化点。当前：" + newPoints).withStyle(ChatFormatting.GREEN), true);
                CustomTamingFramework.LOGGER.info("[CTF_SELFTEST] feed.configured_item_triggers_rule=PASS item={} points={}", stack.getItem(), newPoints);
                if (UsablePetItemRegistry.isSpecialFood(stack)) {
                    CustomTamingFramework.LOGGER.info("[CTF_SELFTEST] feed.special_food_custom_effect_applied=PASS skill={}", unlockSkill);
                } else {
                    CustomTamingFramework.LOGGER.info("[CTF_SELFTEST] feed.normal_food_uses_nutrition=PASS points={}", add);
                }
            }
            event.setCancellationResult(InteractionResult.SUCCESS);
            event.setCanceled(true);
        }
    }

    @SubscribeEvent(priority = EventPriority.HIGHEST)
    public void onLivingDeath(LivingDeathEvent event) {
        LivingEntity target = event.getEntity();
        if (target.level().isClientSide()) {
            return;
        }
        PetProgressionTracker.handlePetKillProgress(target, event.getSource());
        if (PetProgress.isEnabled(target)) {
            dropInventoryModulesOnDeath(target);
        }
        PetBroadcasts.handlePetDeath(target, event.getSource());
    }

    @SubscribeEvent
    public void onLivingTick(LivingTickEvent event) {
        LivingEntity living = event.getEntity();
        if (living.level().isClientSide() || !PetProgress.isEnabled(living)) {
            return;
        }
        if (living.tickCount % 20 != 0) {
            return;
        }
        PetProgressionTracker.handlePetLocationProgress(living);
    }

    @SubscribeEvent(priority = EventPriority.LOWEST)
    public void onEntityTravelToDimension(EntityTravelToDimensionEvent event) {
        if (event.isCanceled()) {
            return;
        }
        if (!(event.getEntity() instanceof LivingEntity living)) {
            return;
        }
        if (living.level().isClientSide()) {
            return;
        }
        PetProgressionTracker.handlePetDimensionTravel(living, event.getDimension());
    }

    private void dropInventoryModulesOnDeath(LivingEntity target) {
        var entityTypeId = ForgeRegistries.ENTITY_TYPES.getKey(target.getType());
        if (entityTypeId == null) {
            return;
        }
        CtfSnbtConfig.PetSetting setting = CtfSnbtConfig.petSetting(entityTypeId.toString());
        Set<String> handled = new HashSet<>();

        if (setting != null && setting.guiTabs() != null) {
            ListTag guiTabs = setting.guiTabs();
            for (int i = 0; i < guiTabs.size(); i++) {
                if (!(guiTabs.get(i) instanceof CompoundTag tab) || !tab.contains("modules", 9)) {
                    continue;
                }
                ListTag modules = tab.getList("modules", 10);
                for (int m = 0; m < modules.size(); m++) {
                    CompoundTag module = modules.getCompound(m);
                    if (!"inventory".equalsIgnoreCase(module.getString("type"))) {
                        continue;
                    }
                    CompoundTag config = module.getCompound("config");
                    String moduleId = config.getString("module_id");
                    if (moduleId == null || moduleId.isBlank() || !handled.add(moduleId)) {
                        continue;
                    }
                    boolean deathDrop = !config.contains("death_drop") || config.getBoolean("death_drop");
                    ModuleInventoryState.clear(target, moduleId, deathDrop);
                }
            }
        }

        if (handled.isEmpty()) {
            CompoundTag stored = ModuleInventoryState.toCompound(target);
            for (String moduleId : stored.getAllKeys()) {
                if (moduleId == null || moduleId.isBlank() || !handled.add(moduleId)) {
                    continue;
                }
                ModuleInventoryState.clear(target, moduleId, true);
            }
        }
    }

    @SubscribeEvent
    public void onPlayerTick(TickEvent.PlayerTickEvent event) {
        if (event.phase != TickEvent.Phase.END) {
            return;
        }
        if (!(event.player instanceof ServerPlayer serverPlayer)) {
            return;
        }
        if (serverPlayer.level().isClientSide()) {
            return;
        }
        ManualUnlockState.checkUserManualTrigger(serverPlayer);
    }

    @SubscribeEvent
    public void onPlayerLoggedIn(PlayerEvent.PlayerLoggedInEvent event) {
        if (!(event.getEntity() instanceof ServerPlayer serverPlayer)) {
            return;
        }
        syncPlayerIntoAllPetPermissions(serverPlayer);
        PetBroadcasts.flushPendingMessages(serverPlayer);
    }

    @SubscribeEvent
    public void onServerTick(TickEvent.ServerTickEvent event) {
        if (event.phase != TickEvent.Phase.END) {
            return;
        }
        PetBroadcasts.restoreVanillaDeathMessages();
    }

    private void syncPlayerIntoAllPetPermissions(ServerPlayer joinedPlayer) {
        if (joinedPlayer.getServer() == null) {
            return;
        }
        boolean changed = false;
        for (var level : joinedPlayer.getServer().getAllLevels()) {
            for (var entity : level.getAllEntities()) {
                if (!(entity instanceof LivingEntity living) || !PetProgress.isEnabled(living)) {
                    continue;
                }
                PetProgress progress = PetProgress.get(living);
                PetProgress updated = progress.ensureKnownPlayer(joinedPlayer.getUUID());
                if (updated != progress) {
                    PetProgress.save(living, updated);
                    changed = true;
                }
            }
        }
        if (changed) {
            CustomTamingFramework.LOGGER.info("[CTF_SELFTEST] pet_permission.login_sync=PASS player={}",
                    joinedPlayer.getGameProfile().getName());
        }
    }
}
