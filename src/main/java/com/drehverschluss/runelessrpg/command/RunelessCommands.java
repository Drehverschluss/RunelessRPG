package com.drehverschluss.runelessrpg.command;

import com.drehverschluss.runelessrpg.api.ManaChangeEvent;
import com.drehverschluss.runelessrpg.mana.ManaManager;
import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.arguments.DoubleArgumentType;
import com.mojang.brigadier.context.CommandContext;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.commands.arguments.EntityArgument;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.neoforged.neoforge.event.RegisterCommandsEvent;

import java.util.Collection;
import java.util.Locale;

public final class RunelessCommands {
    private RunelessCommands() {}

    public static void onRegisterCommands(RegisterCommandsEvent event) {
        register(event.getDispatcher());
    }

    private static void register(CommandDispatcher<CommandSourceStack> dispatcher) {
        dispatcher.register(Commands.literal("runelessrpg")
                .requires(source -> source.hasPermission(2))
                .then(Commands.literal("mana")
                        .then(Commands.argument("targets", EntityArgument.players())
                                .then(Commands.literal("get").executes(RunelessCommands::get))
                                .then(Commands.literal("set")
                                        .then(Commands.argument("amount", DoubleArgumentType.doubleArg(0.0))
                                                .executes(ctx -> change(ctx, false))))
                                .then(Commands.literal("add")
                                        .then(Commands.argument("amount", DoubleArgumentType.doubleArg())
                                                .executes(ctx -> change(ctx, true)))))));
    }

    private static int get(CommandContext<CommandSourceStack> ctx) throws CommandSyntaxException {
        Collection<ServerPlayer> targets = EntityArgument.getPlayers(ctx, "targets");
        for (ServerPlayer player : targets) {
            ctx.getSource().sendSuccess(() -> Component.translatable("commands.runelessrpg.mana.get",
                    player.getDisplayName(), format(ManaManager.getMana(player)), format(ManaManager.getMaxMana(player)),
                    format(ManaManager.getManaRegen(player))), false);
        }
        return targets.size();
    }

    private static int change(CommandContext<CommandSourceStack> ctx, boolean add) throws CommandSyntaxException {
        Collection<ServerPlayer> targets = EntityArgument.getPlayers(ctx, "targets");
        double amount = DoubleArgumentType.getDouble(ctx, "amount");
        for (ServerPlayer player : targets) {
            if (add) {
                ManaManager.addMana(player, amount, ManaChangeEvent.Reason.COMMAND);
            } else {
                ManaManager.setMana(player, amount, ManaChangeEvent.Reason.COMMAND);
            }
            ctx.getSource().sendSuccess(() -> Component.translatable("commands.runelessrpg.mana.set",
                    player.getDisplayName(), format(ManaManager.getMana(player)), format(ManaManager.getMaxMana(player))), true);
        }
        return targets.size();
    }

    private static String format(double value) {
        return String.format(Locale.ROOT, "%.1f", value);
    }
}
