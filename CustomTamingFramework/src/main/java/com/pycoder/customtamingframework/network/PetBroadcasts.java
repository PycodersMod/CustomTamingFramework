package com.pycoder.customtamingframework.network;

import com.pycoder.customtamingframework.CustomTamingFramework;
import com.pycoder.customtamingframework.pet.PetProgress;
import net.minecraft.advancements.Advancement;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.GameRules;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.UUID;

public final class PetBroadcasts {
    private static final Set<MinecraftServer> RESTORE_DEATH_RULE = new HashSet<>();

    private PetBroadcasts() {
    }

    public static void flushPendingMessages(ServerPlayer player) {
        if (player == null || player.getServer() == null) {
            return;
        }
        List<Component> messages = PetMessageQueueData.get(player.getServer()).drain(player.getUUID());
        for (Component message : messages) {
            player.sendSystemMessage(message);
        }
    }

    public static void notifyPermissionChange(ServerPlayer actor, LivingEntity target, PetProgress before, PetProgress after, UUID changedPlayerUuid) {
        if (actor == null || target == null || before == null || after == null || changedPlayerUuid == null) {
            return;
        }
        MinecraftServer server = actor.getServer();
        if (server == null) {
            return;
        }
        String actorName = safeName(actor.getGameProfile().getName(), "未知操作者");
        String ownerName = safeName(resolvePlayerName(server, after.ownerPlayerUuid()), "未知主人");
        String petName = plainEntityName(target);
        String permissionName = after.permissionFor(changedPlayerUuid).displayName();

        String text = actorName + "已将你对于" + ownerName + "的" + petName + "权限设置为" + permissionName;
        deliverOrQueue(server, changedPlayerUuid, net.minecraft.network.chat.Component.literal(text).withStyle(ChatFormatting.WHITE));
    }

    public static void notifyTransfer(ServerPlayer actor, LivingEntity target, PetProgress before, PetProgress after, UUID newOwnerUuid, boolean broadcast) {
        if (actor == null || target == null || before == null || after == null || newOwnerUuid == null) {
            return;
        }
        MinecraftServer server = actor.getServer();
        if (server == null) {
            return;
        }
        UUID originalOwnerUuid = parseUuid(before.ownerPlayerUuid());
        String originalOwnerName = safeName(resolvePlayerName(server, before.ownerPlayerUuid()), "原主人");
        String newOwnerName = safeName(resolvePlayerName(server, newOwnerUuid.toString()), "新主人");
        String petName = plainEntityName(target);

        Component privateMessage = Component.literal(originalOwnerName + "已将" + petName + "赠与你")
                .withStyle(ChatFormatting.WHITE);
        deliverOrQueue(server, newOwnerUuid, privateMessage);

        if (!broadcast) {
            return;
        }

        Component publicMessage = Component.literal(originalOwnerName + "已将" + petName + "赠与" + newOwnerName)
                .withStyle(ChatFormatting.WHITE);
        for (ServerPlayer online : server.getPlayerList().getPlayers()) {
            UUID onlineUuid = online.getUUID();
            if ((originalOwnerUuid != null && onlineUuid.equals(originalOwnerUuid)) || onlineUuid.equals(newOwnerUuid)) {
                continue;
            }
            online.sendSystemMessage(publicMessage);
        }
    }

    public static void broadcastPetAssistedAdvancement(MinecraftServer server, ServerPlayer owner, LivingEntity pet, Advancement advancement) {
        if (server == null || owner == null || pet == null || advancement == null || advancement.getDisplay() == null) {
            return;
        }
        if (!advancement.getDisplay().shouldAnnounceChat()) {
            return;
        }

        String frameName = advancement.getDisplay().getFrame().getName();
        String actionText = switch (frameName) {
            case "goal" -> "达成了目标";
            case "challenge" -> "完成了挑战";
            default -> "取得了进度";
        };
        String petName = plainEntityName(pet);

        Component message = Component.literal("在")
                .append(Component.literal(petName))
                .append(Component.literal("的帮助下，"))
                .append(owner.getDisplayName())
                .append(Component.literal(" "))
                .append(Component.literal(actionText))
                .append(Component.literal(" "))
                .append(advancement.getChatComponent());

        for (ServerPlayer online : server.getPlayerList().getPlayers()) {
            online.sendSystemMessage(message);
        }
    }

    public static void handlePetDeath(LivingEntity victim, DamageSource source) {
        if (victim == null || source == null || victim.level().isClientSide()) {
            return;
        }
        MinecraftServer server = victim.level().getServer();
        if (server == null) {
            return;
        }
        if (!isRelevantDeath(victim, source)) {
            return;
        }
        suppressVanillaDeathMessages(server);

        List<ServerPlayer> onlinePlayers = server.getPlayerList().getPlayers();
        for (ServerPlayer viewer : onlinePlayers) {
            viewer.sendSystemMessage(buildDeathMessage(viewer.getUUID(), server, victim, source));
        }

        if (PetProgress.isEnabled(victim)) {
            PetProgress victimProgress = PetProgress.get(victim);
            UUID ownerUuid = parseUuid(victimProgress.ownerPlayerUuid());
            if (ownerUuid != null && server.getPlayerList().getPlayer(ownerUuid) == null) {
                Component queued = buildDeathMessage(ownerUuid, server, victim, source);
                PetMessageQueueData.get(server).enqueue(ownerUuid, queued);
            }
        }
    }

    public static void restoreVanillaDeathMessages() {
        if (RESTORE_DEATH_RULE.isEmpty()) {
            return;
        }
        List<MinecraftServer> restore = new ArrayList<>(RESTORE_DEATH_RULE);
        RESTORE_DEATH_RULE.clear();
        for (MinecraftServer server : restore) {
            if (server == null) {
                continue;
            }
            try {
                server.getGameRules().getRule(GameRules.RULE_SHOWDEATHMESSAGES).set(true, server);
            } catch (RuntimeException exception) {
                CustomTamingFramework.LOGGER.warn("Failed to restore death message rule", exception);
            }
        }
    }

    private static void suppressVanillaDeathMessages(MinecraftServer server) {
        if (server == null) {
            return;
        }
        try {
            if (server.getGameRules().getBoolean(GameRules.RULE_SHOWDEATHMESSAGES)) {
                server.getGameRules().getRule(GameRules.RULE_SHOWDEATHMESSAGES).set(false, server);
                RESTORE_DEATH_RULE.add(server);
            }
        } catch (RuntimeException exception) {
            CustomTamingFramework.LOGGER.warn("Failed to suppress vanilla death messages", exception);
        }
    }

    private static boolean isRelevantDeath(LivingEntity victim, DamageSource source) {
        if (PetProgress.isEnabled(victim)) {
            return true;
        }
        if (!(victim instanceof Player)) {
            return false;
        }
        Entity attacker = source.getEntity();
        if (attacker instanceof LivingEntity living && PetProgress.isEnabled(living)) {
            return true;
        }
        Entity direct = source.getDirectEntity();
        return direct instanceof LivingEntity living && PetProgress.isEnabled(living);
    }

    private static Component buildDeathMessage(UUID viewerUuid, MinecraftServer server,
                                               LivingEntity victim, DamageSource source) {
        String message = stripFormatting(source.getLocalizedDeathMessage(victim).getString());
        List<String[]> replacements = new ArrayList<>();

        addReplacement(replacements, victim, viewerUuid, server);
        Entity attacker = source.getEntity();
        if (attacker != null) {
            addReplacement(replacements, attacker, viewerUuid, server);
        }
        Entity direct = source.getDirectEntity();
        if (direct != null && direct != attacker) {
            addReplacement(replacements, direct, viewerUuid, server);
        }

        replacements.sort((left, right) -> Integer.compare(right[0].length(), left[0].length()));
        List<String[]> tokenReplacements = new ArrayList<>();
        for (int i = 0; i < replacements.size(); i++) {
            String[] pair = replacements.get(i);
            String token = "__CTF_DEATH_TOKEN_" + i + "__";
            message = replaceNext(message, pair[0], token);
            tokenReplacements.add(new String[]{token, pair[1]});
        }
        for (String[] pair : tokenReplacements) {
            message = replaceNext(message, pair[0], pair[1]);
        }
        message = stripFormatting(message);
        return Component.literal(message).withStyle(ChatFormatting.WHITE);
    }

    private static void addReplacement(List<String[]> replacements, Entity entity, UUID viewerUuid, MinecraftServer server) {
        if (!(entity instanceof LivingEntity living) || !PetProgress.isEnabled(living)) {
            return;
        }
        String actual = plainEntityName(living);
        String replacement = viewerVisibleName(server, viewerUuid, living);
        if (replacement.isBlank()) {
            return;
        }
        addReplacementToken(replacements, actual, replacement);
        addReplacementToken(replacements, compactEntityName(actual), replacement);
        addReplacementToken(replacements, stripFormatting(safeName(living.getDisplayName().getString(), "")), replacement);
        addReplacementToken(replacements, compactEntityName(stripFormatting(safeName(living.getDisplayName().getString(), ""))), replacement);
        addReplacementToken(replacements, stripFormatting(safeName(living.getName().getString(), "")), replacement);
        addReplacementToken(replacements, compactEntityName(stripFormatting(safeName(living.getName().getString(), ""))), replacement);
        if (living.getCustomName() != null) {
            addReplacementToken(replacements, stripFormatting(living.getCustomName().getString()), replacement);
            addReplacementToken(replacements, compactEntityName(stripFormatting(living.getCustomName().getString())), replacement);
        }
        String titleOnly = plainEntityTitleOnly(actual);
        if (!titleOnly.isBlank()) {
            addReplacementToken(replacements, titleOnly, replacement);
        }
    }

    private static void addReplacementToken(List<String[]> replacements, String actual, String replacement) {
        if (actual == null || actual.isBlank() || replacement == null || replacement.isBlank()) {
            return;
        }
        replacements.add(new String[]{actual, replacement});
    }

    private static String viewerVisibleName(MinecraftServer server, UUID viewerUuid, LivingEntity entity) {
        if (entity == null) {
            return "";
        }
        String actual = plainEntityName(entity);
        if (actual.isBlank()) {
            return "";
        }
        if (!PetProgress.isEnabled(entity)) {
            return actual;
        }
        PetProgress progress = PetProgress.get(entity);
        if (viewerUuid != null && progress.owns(viewerUuid)) {
            return "你的" + actual;
        }
        String ownerName = resolvePlayerName(server, progress.ownerPlayerUuid());
        if (ownerName.isBlank()) {
            ownerName = "主人";
        }
        return ownerName + "的" + actual;
    }

    private static String resolvePlayerName(MinecraftServer server, String playerUuid) {
        if (playerUuid == null || playerUuid.isBlank()) {
            return "";
        }
        try {
            UUID uuid = UUID.fromString(playerUuid.trim());
            if (server != null) {
                for (ServerPlayer online : server.getPlayerList().getPlayers()) {
                    if (online != null && uuid.equals(online.getUUID())) {
                        String name = online.getGameProfile().getName();
                        if (name != null && !name.isBlank()) {
                            return name;
                        }
                    }
                }
                String cached = server.getProfileCache().get(uuid)
                        .map(profile -> safeName(profile.getName(), ""))
                        .orElse("");
                if (!cached.isBlank()) {
                    return cached;
                }
            }
        } catch (IllegalArgumentException ignored) {
            return playerUuid;
        }
        return "";
    }

    private static String plainEntityName(LivingEntity entity) {
        if (entity == null) {
            return "";
        }
        String actual = safeName(entity.getDisplayName().getString(),
                Component.translatable(entity.getType().getDescriptionId()).getString());
        return stripFormatting(actual);
    }

    private static String plainEntityTitleOnly(String actual) {
        if (actual == null || actual.isBlank()) {
            return "";
        }
        int closingBracket = actual.indexOf(']');
        if (actual.startsWith("[") && closingBracket > 0) {
            return actual.substring(0, closingBracket + 1).trim();
        }
        int firstSpace = actual.indexOf(' ');
        if (firstSpace > 0) {
            return actual.substring(0, firstSpace).trim();
        }
        return actual;
    }

    private static String compactEntityName(String value) {
        if (value == null || value.isBlank()) {
            return "";
        }
        return value.replace(" ", "");
    }

    private static String stripFormatting(String value) {
        if (value == null || value.isBlank()) {
            return "";
        }
        StringBuilder sb = new StringBuilder(value.length());
        for (int i = 0; i < value.length(); i++) {
            char c = value.charAt(i);
            if (c == '§' && i + 1 < value.length()) {
                i++;
                continue;
            }
            sb.append(c);
        }
        return sb.toString();
    }

    private static void deliverOrQueue(MinecraftServer server, UUID recipient, Component message) {
        if (server == null || recipient == null || message == null) {
            return;
        }
        ServerPlayer online = server.getPlayerList().getPlayer(recipient);
        if (online != null) {
            online.sendSystemMessage(message);
            return;
        }
        PetMessageQueueData.get(server).enqueue(recipient, message);
    }

    private static String replaceNext(String text, String target, String replacement) {
        if (text == null || text.isBlank() || target == null || target.isBlank() || replacement == null) {
            return text;
        }
        int index = text.indexOf(target);
        if (index < 0) {
            return text;
        }
        return text.substring(0, index) + replacement + text.substring(index + target.length());
    }

    private static String safeName(String value, String fallback) {
        if (value == null || value.isBlank()) {
            return fallback == null ? "" : fallback;
        }
        return value;
    }

    private static UUID parseUuid(String value) {
        if (value == null || value.isBlank()) {
            return null;
        }
        try {
            return UUID.fromString(value.trim());
        } catch (IllegalArgumentException ignored) {
            return null;
        }
    }
}
