package com.pycoder.customtamingframework.network;

import com.pycoder.customtamingframework.CustomTamingFramework;
import com.pycoder.customtamingframework.manual.ManualUnlockState;
import com.pycoder.customtamingframework.pet.PetAccessResolver;
import com.pycoder.customtamingframework.pet.PetProgress;
import net.minecraft.ChatFormatting;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraftforge.network.NetworkEvent;
import net.minecraftforge.network.PacketDistributor;

import java.util.function.Supplier;

public record C2SRequestOpenPetPermissionScreenPacket(int entityId) {
    public static void encode(C2SRequestOpenPetPermissionScreenPacket packet, FriendlyByteBuf buffer) {
        buffer.writeVarInt(packet.entityId());
    }

    public static C2SRequestOpenPetPermissionScreenPacket decode(FriendlyByteBuf buffer) {
        return new C2SRequestOpenPetPermissionScreenPacket(buffer.readVarInt());
    }

    public static void handle(C2SRequestOpenPetPermissionScreenPacket packet, Supplier<NetworkEvent.Context> contextSupplier) {
        NetworkEvent.Context context = contextSupplier.get();
        context.enqueueWork(() -> {
            ServerPlayer player = context.getSender();
            if (player == null) {
                return;
            }
            ManualUnlockState.checkUserManualTrigger(player);
            boolean isOp = player.hasPermissions(2);
            if (!isOp && !ManualUnlockState.hasUnlockedUserManual(player)) {
                player.displayClientMessage(Component.literal("好像遗忘了某些记忆……").withStyle(ChatFormatting.GRAY), true);
                return;
            }
            Entity entity = player.level().getEntity(packet.entityId());
            if (!(entity instanceof LivingEntity target) || !PetProgress.isEnabled(target)) {
                player.displayClientMessage(Component.literal("准心目标不是你可操作的 CTF 宠物。").withStyle(ChatFormatting.GRAY), true);
                return;
            }
            if (!PetAccessResolver.canOpenPetScreen(player, target)) {
                player.displayClientMessage(Component.literal("它已被主人设置为保密").withStyle(ChatFormatting.GRAY), true);
                return;
            }

            CtfNetwork.CHANNEL.send(PacketDistributor.PLAYER.with(() -> player),
                    S2CPetPermissionSnapshotPacket.build(player, target, true));
            CustomTamingFramework.LOGGER.info("[CTF_SELFTEST] pet_permission.open_screen=PASS entity={}", target.getType());
        });
        context.setPacketHandled(true);
    }
}
