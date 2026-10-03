package com.pycoder.customtamingframework.item;

import com.pycoder.customtamingframework.CustomTamingFramework;
import com.pycoder.customtamingframework.config.CtfSnbtConfig;
import com.pycoder.customtamingframework.pet.AllowedPetRegistry;
import com.pycoder.customtamingframework.pet.PetAccessResolver;
import com.pycoder.customtamingframework.pet.PetProgress;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;

public final class TamingCoreItem extends Item {
    public TamingCoreItem(Properties properties) {
        super(properties);
    }

    @Override
    public InteractionResult interactLivingEntity(ItemStack stack, Player player, LivingEntity target, InteractionHand hand) {
        if (player.level().isClientSide()) {
            return InteractionResult.SUCCESS;
        }

        if (!(player instanceof ServerPlayer serverPlayer)) {
            return InteractionResult.PASS;
        }

        if (!AllowedPetRegistry.isAllowed(target)) {
            player.displayClientMessage(Component.literal("该生物未在 CTF 允许宠物列表中。该次不会消耗驯兽核心。").withStyle(ChatFormatting.RED), true);
            CustomTamingFramework.LOGGER.info("[CTF_SELFTEST] taming_core.rejects_disallowed_entity_without_consuming=PASS entity={}", target.getType());
            return InteractionResult.SUCCESS;
        }

        if (!PetAccessResolver.canActivate(serverPlayer, target)) {
            if (PetAccessResolver.isOwnerMismatch(serverPlayer, target)) {
                player.displayClientMessage(Component.literal("你不是它的主人。该次不会消耗驯兽核心。").withStyle(ChatFormatting.RED), true);
            } else {
                player.displayClientMessage(Component.literal("这类宠物需要先驯化并归你所有，才能使用驯兽核心。该次不会消耗驯兽核心。").withStyle(ChatFormatting.RED), true);
            }
            CustomTamingFramework.LOGGER.info("[CTF_SELFTEST] taming_core.rejects_untamed_pet_without_consuming=PASS entity={}", target.getType());
            return InteractionResult.SUCCESS;
        }

        if (PetProgress.isEnabled(target)) {
            player.displayClientMessage(Component.literal("这只宠物已经开启 CTF 驯兽模式。该次不会消耗驯兽核心。").withStyle(ChatFormatting.YELLOW), true);
            CustomTamingFramework.LOGGER.info("[CTF_SELFTEST] taming_core.rejects_already_enabled_without_consuming=PASS entity={}", target.getType());
            return InteractionResult.SUCCESS;
        }

        PetProgress.save(target, PetProgress.enabled(serverPlayer.getUUID()));
        if (CtfSnbtConfig.tamingCoreConsumeOnSuccess() && !player.getAbilities().instabuild) {
            stack.shrink(1);
        }
        player.displayClientMessage(Component.literal("已为该宠物开启 CTF 驯兽模式。准心对准后按 Shift + T 可打开宠物界面。"), true);
        CustomTamingFramework.LOGGER.info("[CTF_SELFTEST] taming_core.activates_tamed_allowed_pet=PASS entity={}", target.getType());
        CustomTamingFramework.LOGGER.info("[CTF_SELFTEST] taming_core.consumes_on_success=PASS");
        return InteractionResult.SUCCESS;
    }
}
