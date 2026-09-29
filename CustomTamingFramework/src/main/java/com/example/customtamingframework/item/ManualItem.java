package com.example.customtamingframework.item;

import com.example.customtamingframework.client.ManualClient;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResultHolder;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.fml.DistExecutor;

public final class ManualItem extends Item {
    private final String manualId;

    public ManualItem(String manualId, Properties properties) {
        super(properties);
        this.manualId = manualId;
    }

    @Override
    public InteractionResultHolder<ItemStack> use(Level level, Player player, InteractionHand hand) {
        ItemStack stack = player.getItemInHand(hand);
        if (level.isClientSide()) {
            DistExecutor.unsafeRunWhenOn(Dist.CLIENT, () -> () -> ManualClient.open(manualId));
        }
        return InteractionResultHolder.sidedSuccess(stack, level.isClientSide());
    }
}
