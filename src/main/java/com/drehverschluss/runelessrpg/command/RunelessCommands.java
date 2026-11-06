package com.drehverschluss.runelessrpg.command;

import com.drehverschluss.runelessrpg.api.ManaChangeEvent;
import com.drehverschluss.runelessrpg.attribute.RunelessAttributes;
import com.drehverschluss.runelessrpg.config.RunelessServerConfig;
import com.drehverschluss.runelessrpg.cost.CostSettings;
import com.drehverschluss.runelessrpg.cost.ManaCosts;
import com.drehverschluss.runelessrpg.cost.SpellCostInfo;
import com.drehverschluss.runelessrpg.cost.SpellCostReport;
import com.drehverschluss.runelessrpg.mana.ManaManager;
import com.drehverschluss.runelessrpg.potion.BorrowedPower;
import com.drehverschluss.runelessrpg.util.CostFormat;
import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.arguments.DoubleArgumentType;
import com.mojang.brigadier.context.CommandContext;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import com.mojang.brigadier.exceptions.DynamicCommandExceptionType;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.commands.SharedSuggestionProvider;
import net.minecraft.commands.arguments.EntityArgument;
import net.minecraft.commands.arguments.ResourceLocationArgument;
import net.minecraft.core.Holder;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.neoforged.neoforge.event.RegisterCommandsEvent;
import net.spell_engine.api.spell.Spell;
import net.spell_engine.api.spell.registry.SpellRegistry;

import java.util.Collection;
import java.util.Locale;

public final class RunelessCommands {
    private static final DynamicCommandExceptionType UNKNOWN_SPELL =
            new DynamicCommandExceptionType(id -> Component.translatable("commands.runelessrpg.cost.unknown", id));

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
                                                .executes(ctx -> change(ctx, true))))))
                .then(Commands.literal("cost")
                        .then(Commands.argument("spell", ResourceLocationArgument.id())
                                .suggests((ctx, builder) -> SharedSuggestionProvider.suggestResource(
                                        SpellRegistry.from(ctx.getSource().getLevel()).keySet(), builder))
                                .executes(ctx -> cost(ctx, null))
                                .then(Commands.argument("player", EntityArgument.player())
                                        .executes(ctx -> cost(ctx, EntityArgument.getPlayer(ctx, "player"))))))
                .then(Commands.literal("report").executes(RunelessCommands::report)));
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

    private static int cost(CommandContext<CommandSourceStack> ctx, ServerPlayer target) throws CommandSyntaxException {
        CommandSourceStack source = ctx.getSource();
        ResourceLocation id = ResourceLocationArgument.getId(ctx, "spell");
        Holder<Spell> holder = SpellRegistry.from(source.getLevel()).getHolder(id).orElseThrow(() -> UNKNOWN_SPELL.create(id.toString()));
        ServerPlayer player = target != null ? target : source.getPlayer();
        SpellCostInfo info = ManaCosts.resolve(source.getLevel(), holder);
        Spell spell = holder.value();

        source.sendSuccess(() -> Component.translatable("commands.runelessrpg.cost.header", id.toString(),
                info.category().name(), info.source().name(), spell.tier), false);
        if (!info.costsMana()) {
            source.sendSuccess(() -> Component.translatable("commands.runelessrpg.cost.none"), false);
            return 1;
        }
        source.sendSuccess(() -> Component.translatable("commands.runelessrpg.cost.base", CostFormat.format(info.baseCost())), false);
        if (player == null) {
            return 1;
        }
        CostSettings settings = CostSettings.server();
        double manaCost = player.getAttributeValue(RunelessAttributes.MANA_COST);
        double factor = ManaCosts.manaCostFactor(player, holder);
        double overcharge = player.getAttributeValue(RunelessAttributes.OVERCHARGE);
        double total = ManaCosts.cost(player, holder, info);
        source.sendSuccess(() -> Component.translatable("commands.runelessrpg.cost.factors", player.getDisplayName(),
                percent(manaCost), percent(settings.minManaCostPercent() / 100.0), percent(factor), percent(overcharge)), false);
        source.sendSuccess(() -> Component.translatable("commands.runelessrpg.cost.result", CostFormat.format(total)), false);
        boolean infinity = ManaCosts.hasSpellInfinity(player, settings);
        source.sendSuccess(() -> Component.translatable("commands.runelessrpg.cost.infinity",
                Component.translatable(infinity ? "gui.yes" : "gui.no"), settings.spellInfinityMode().name(),
                percent(ManaCosts.manaCostFloorPercent(player) / 100.0)), false);
        double bonus = ManaCosts.overchargeBonus(player, holder) - 1.0;
        source.sendSuccess(() -> Component.translatable("commands.runelessrpg.cost.overcharge",
                CostFormat.format(bonus * 100.0), CostFormat.format(settings.overchargeDamageFactor()),
                Component.translatable(settings.overchargeAffectsHealing() ? "gui.yes" : "gui.no")), false);
        if (info.channeled()) {
            int ticks = ManaCosts.channelTicks(player, holder);
            source.sendSuccess(() -> Component.translatable("commands.runelessrpg.cost.channel", ticks,
                    CostFormat.format(total / ticks), settings.channelCostMode().name(), CostFormat.format(settings.channelStartPercent())), false);
        }
        double debtLimit = ManaCosts.debtLimit(player, settings, total);
        double borrowedLimit = BorrowedPower.debtLimit(player, settings);
        source.sendSuccess(() -> Component.translatable("commands.runelessrpg.cost.debt", settings.manaMode().name(),
                settings.manaMode() == RunelessServerConfig.ManaMode.DEBT ? "-" : CostFormat.format(-debtLimit),
                Component.translatable(BorrowedPower.isActive(player) ? "gui.yes" : "gui.no"), CostFormat.format(-borrowedLimit)), false);
        return 1;
    }

    private static int report(CommandContext<CommandSourceStack> ctx) {
        SpellCostReport.Result result = SpellCostReport.write(ctx.getSource().getServer());
        if (result.spells() < 0) {
            ctx.getSource().sendFailure(Component.translatable("commands.runelessrpg.report.failed", result.file().toString()));
            return 0;
        }
        ctx.getSource().sendSuccess(() -> Component.translatable("commands.runelessrpg.report.done", result.spells(),
                result.file().toAbsolutePath().normalize().toString()), true);
        return result.spells();
    }

    private static String format(double value) {
        return String.format(Locale.ROOT, "%.1f", value);
    }

    private static String percent(double value) {
        return CostFormat.format(value * 100.0) + "%";
    }
}
