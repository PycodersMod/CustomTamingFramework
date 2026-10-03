package com.pycoder.customtamingframework.manual;

import com.pycoder.customtamingframework.CustomTamingFramework;
import com.pycoder.customtamingframework.registry.CtfItems;
import net.minecraft.advancements.Advancement;
import net.minecraft.advancements.AdvancementProgress;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.item.ItemStack;

public final class ManualUnlockState {
    private static final ResourceLocation USER_MANUAL_ADVANCEMENT_ID =
            ResourceLocation.fromNamespaceAndPath(CustomTamingFramework.MOD_ID, "manual/obtain_user_manual");
    private static final ResourceLocation USER_MANUAL_VISIBLE_ADVANCEMENT_ID =
            ResourceLocation.fromNamespaceAndPath(CustomTamingFramework.MOD_ID, "manual/obtain_user_manual_visible");
    private static final String USER_MANUAL_CRITERION = "obtain_user_manual";

    private ManualUnlockState() {
    }

    public static void checkUserManualTrigger(ServerPlayer player) {
        if (player == null || !hasUserManual(player)) {
            return;
        }

        Advancement advancement = resolveUserManualAdvancement(player);
        Advancement visibleAdvancement = resolveVisibleUserManualAdvancement(player);
        boolean awardedInternal = awardIfPresent(player, advancement);
        boolean awardedVisible = awardIfPresent(player, visibleAdvancement);
        if (awardedInternal || awardedVisible) {
            CustomTamingFramework.LOGGER.info("[CTF_SELFTEST] manual.user_manual_advancement_awarded=PASS player={}", player.getGameProfile().getName());
        }
    }

    public static boolean hasUnlockedUserManual(ServerPlayer player) {
        if (player == null) {
            return false;
        }
        Advancement advancement = resolveVisibleUserManualAdvancement(player);
        if (advancement == null) {
            return false;
        }
        AdvancementProgress progress = player.getAdvancements().getOrStartProgress(advancement);
        return progress.isDone();
    }

    private static Advancement resolveUserManualAdvancement(ServerPlayer player) {
        if (player.serverLevel() == null || player.serverLevel().getServer() == null) {
            return null;
        }
        return player.serverLevel().getServer().getAdvancements().getAdvancement(USER_MANUAL_ADVANCEMENT_ID);
    }

    private static Advancement resolveVisibleUserManualAdvancement(ServerPlayer player) {
        if (player.serverLevel() == null || player.serverLevel().getServer() == null) {
            return null;
        }
        return player.serverLevel().getServer().getAdvancements().getAdvancement(USER_MANUAL_VISIBLE_ADVANCEMENT_ID);
    }

    private static boolean awardIfPresent(ServerPlayer player, Advancement advancement) {
        if (advancement == null) {
            return false;
        }
        return player.getAdvancements().award(advancement, USER_MANUAL_CRITERION);
    }

    private static boolean hasUserManual(ServerPlayer player) {
        ItemStack target = new ItemStack(CtfItems.USER_MANUAL.get());
        return player.getInventory().contains(target);
    }
}
