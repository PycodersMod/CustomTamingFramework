package com.example.customtamingframework.command;

import com.example.customtamingframework.registry.CtfItems;
import com.example.customtamingframework.selftest.CtfSelfTest;
import com.mojang.brigadier.CommandDispatcher;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.item.ItemStack;
import net.minecraft.commands.CommandSourceStack;

import static net.minecraft.commands.Commands.literal;

public final class CtfCommands {
    private CtfCommands() {
    }

    public static void register(CommandDispatcher<CommandSourceStack> dispatcher) {
        dispatcher.register(literal("ctf")
                .then(literal("selftest")
                        .then(literal("all").executes(context -> CtfSelfTest.runAll(context.getSource())))
                        .then(literal("config").executes(context -> CtfSelfTest.runConfig(context.getSource())))
                        .then(literal("taming_core").executes(context -> CtfSelfTest.runTamingCore(context.getSource())))
                        .then(literal("registry").executes(context -> CtfSelfTest.runRegistry(context.getSource())))
                        .executes(context -> CtfSelfTest.runAll(context.getSource()))));

        dispatcher.register(literal("？！驯兽？！")
                .executes(context -> {
                    CommandSourceStack source = context.getSource();
                    if (!(source.getEntity() instanceof ServerPlayer player)) {
                        source.sendFailure(Component.literal("该指令只能由玩家执行。"));
                        return 0;
                    }

                    ItemStack stack = new ItemStack(CtfItems.USER_MANUAL.get());
                    if (!player.getInventory().add(stack)) {
                        player.drop(stack, false);
                    }

                    source.sendSuccess(() -> Component.literal("已获得 CTF 用户手册。"), false);
                    return 1;
                }));
    }
}
