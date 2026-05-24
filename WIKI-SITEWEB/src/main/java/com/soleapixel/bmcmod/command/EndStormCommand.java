package com.soleapixel.bmcmod.command;

import com.mojang.brigadier.arguments.IntegerArgumentType;
import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import com.soleapixel.bmcmod.gameplay.EndStormManager;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.commands.SharedSuggestionProvider;
import net.minecraft.commands.arguments.EntityArgument;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;

import java.util.Collection;
import java.util.Locale;

public final class EndStormCommand {
    private EndStormCommand() {
    }

    public static LiteralArgumentBuilder<CommandSourceStack> buildEndStorm() {
        return Commands.literal("endstorm")
                .requires(src -> src.hasPermission(2))
                .then(Commands.literal("play")
                        .executes(ctx -> runPlay(ctx.getSource(), ctx.getSource().getPlayerOrException(), 1 + ctx.getSource().getLevel().random.nextInt(4)))
                        .then(Commands.argument("level", IntegerArgumentType.integer(1, 4))
                                .executes(ctx -> runPlay(ctx.getSource(), ctx.getSource().getPlayerOrException(), IntegerArgumentType.getInteger(ctx, "level")))
                                .then(Commands.argument("joueurs", EntityArgument.players())
                                        .executes(ctx -> runPlayForTargets(ctx.getSource(), EntityArgument.getPlayers(ctx, "joueurs"), IntegerArgumentType.getInteger(ctx, "level"))))))
                .then(Commands.literal("start")
                        .executes(ctx -> runStart(ctx.getSource(), ctx.getSource().getPlayerOrException(), 1 + ctx.getSource().getLevel().random.nextInt(4)))
                        .then(Commands.argument("level", IntegerArgumentType.integer(1, 4))
                                .executes(ctx -> runStart(ctx.getSource(), ctx.getSource().getPlayerOrException(), IntegerArgumentType.getInteger(ctx, "level")))
                                .then(Commands.argument("joueurs", EntityArgument.players())
                                        .executes(ctx -> runStartForTargets(ctx.getSource(), EntityArgument.getPlayers(ctx, "joueurs"), IntegerArgumentType.getInteger(ctx, "level"))))))
                .then(Commands.literal("stop")
                        .executes(ctx -> runStop(ctx.getSource(), ctx.getSource().getPlayerOrException()))
                        .then(Commands.argument("joueurs", EntityArgument.players())
                                .executes(ctx -> runStopForTargets(ctx.getSource(), EntityArgument.getPlayers(ctx, "joueurs")))))
                .then(Commands.literal("get")
                        .executes(ctx -> runGet(ctx.getSource(), ctx.getSource().getPlayerOrException(), 64))
                        .then(Commands.argument("range", IntegerArgumentType.integer(8, 512))
                                .executes(ctx -> runGet(ctx.getSource(), ctx.getSource().getPlayerOrException(), IntegerArgumentType.getInteger(ctx, "range")))))
                .then(Commands.literal("status")
                        .executes(ctx -> runGet(ctx.getSource(), ctx.getSource().getPlayerOrException(), 128)))
                .then(Commands.literal("end")
                        .executes(ctx -> runEnd(ctx.getSource(), ctx.getSource().getPlayerOrException()))
                        .then(Commands.argument("joueurs", EntityArgument.players())
                                .executes(ctx -> runEndForTargets(ctx.getSource(), EntityArgument.getPlayers(ctx, "joueurs")))))
                .then(Commands.literal("back")
                        .executes(ctx -> runBack(ctx.getSource(), ctx.getSource().getPlayerOrException()))
                        .then(Commands.argument("joueurs", EntityArgument.players())
                                .executes(ctx -> runBackForTargets(ctx.getSource(), EntityArgument.getPlayers(ctx, "joueurs")))))
                .then(Commands.literal("victoryloot")
                        .executes(ctx -> runVictoryLoot(ctx.getSource(), ctx.getSource().getPlayerOrException(), 4))
                        .then(Commands.argument("level", IntegerArgumentType.integer(1, 4))
                                .executes(ctx -> runVictoryLoot(ctx.getSource(), ctx.getSource().getPlayerOrException(), IntegerArgumentType.getInteger(ctx, "level")))))
                .then(Commands.literal("spawn")
                        .then(Commands.argument("kind", StringArgumentType.word())
                                .suggests((ctx, builder) -> SharedSuggestionProvider.suggest(EndStormManager.DEBUG_SPAWN_KINDS, builder))
                                .then(Commands.argument("level", IntegerArgumentType.integer(1, 4))
                                        .executes(ctx -> runSpawn(ctx.getSource(), StringArgumentType.getString(ctx, "kind"), IntegerArgumentType.getInteger(ctx, "level"), EndStormManager.getStormWaveCount(IntegerArgumentType.getInteger(ctx, "level"))))
                                        .then(Commands.argument("wave", IntegerArgumentType.integer(1, 24))
                                                .executes(ctx -> runSpawn(ctx.getSource(), StringArgumentType.getString(ctx, "kind"), IntegerArgumentType.getInteger(ctx, "level"), IntegerArgumentType.getInteger(ctx, "wave")))))));
    }

    private static int runPlay(CommandSourceStack source, ServerPlayer player, int level) {
        if (!EndStormManager.startForced(player, level)) {
            source.sendFailure(Component.translatable("bmcmod.commands.endstorm.play_failed"));
            return 0;
        }
        source.sendSuccess(() -> Component.translatable("bmcmod.commands.endstorm.play_success", level, player.getName()), true);
        return 1;
    }

    private static int runPlayForTargets(CommandSourceStack source, Collection<ServerPlayer> players, int level) {
        int count = 0;
        for (ServerPlayer sp : players) {
            if (EndStormManager.startForced(sp, level)) {
                count++;
            }
        }
        int c = count;
        source.sendSuccess(() -> Component.translatable("bmcmod.commands.endstorm.play_success_many", level, c), true);
        return count;
    }

    private static int runStart(CommandSourceStack source, ServerPlayer player, int level) {
        if (!EndStormManager.startImmediate(player, level)) {
            source.sendFailure(Component.translatable("bmcmod.commands.endstorm.play_failed"));
            return 0;
        }
        source.sendSuccess(() -> Component.translatable("bmcmod.commands.endstorm.start_success", level, player.getName()), true);
        return 1;
    }

    private static int runStartForTargets(CommandSourceStack source, Collection<ServerPlayer> players, int level) {
        int count = 0;
        for (ServerPlayer sp : players) {
            if (EndStormManager.startImmediate(sp, level)) {
                count++;
            }
        }
        int c = count;
        source.sendSuccess(() -> Component.translatable("bmcmod.commands.endstorm.start_success_many", level, c), true);
        return count;
    }

    private static int runStop(CommandSourceStack source, ServerPlayer player) {
        if (!EndStormManager.stopForced(player)) {
            source.sendFailure(Component.translatable("bmcmod.commands.endstorm.none"));
            return 0;
        }
        source.sendSuccess(() -> Component.translatable("bmcmod.commands.endstorm.stop_success", player.getName()), true);
        return 1;
    }

    private static int runStopForTargets(CommandSourceStack source, Collection<ServerPlayer> players) {
        int count = 0;
        for (ServerPlayer sp : players) {
            if (EndStormManager.stopForced(sp)) {
                count++;
            }
        }
        int c = count;
        source.sendSuccess(() -> Component.translatable("bmcmod.commands.endstorm.stop_success_many", c), true);
        return count;
    }

    private static int runGet(CommandSourceStack source, ServerPlayer player, int range) {
        int lvl = EndStormManager.getCurrentLevelAround(player, range);
        if (lvl <= 0) {
            source.sendSuccess(() -> Component.translatable("bmcmod.commands.endstorm.get_none", range), false);
            return 0;
        }
        source.sendSuccess(() -> Component.translatable("bmcmod.commands.endstorm.get_found", lvl, range), false);
        return lvl;
    }

    private static int runEnd(CommandSourceStack source, ServerPlayer player) {
        if (!EndStormManager.forceEndCurrentWave(player)) {
            source.sendFailure(Component.translatable("bmcmod.commands.endstorm.end_failed"));
            return 0;
        }
        source.sendSuccess(() -> Component.translatable("bmcmod.commands.endstorm.end_success", player.getName()), true);
        return 1;
    }

    private static int runEndForTargets(CommandSourceStack source, Collection<ServerPlayer> players) {
        int count = 0;
        for (ServerPlayer sp : players) {
            if (EndStormManager.forceEndCurrentWave(sp)) {
                count++;
            }
        }
        if (count <= 0) {
            source.sendFailure(Component.translatable("bmcmod.commands.endstorm.end_failed"));
            return 0;
        }
        int c = count;
        source.sendSuccess(() -> Component.translatable("bmcmod.commands.endstorm.end_success_many", c), true);
        return count;
    }

    private static int runBack(CommandSourceStack source, ServerPlayer player) {
        if (!EndStormManager.forceBackOneWave(player)) {
            source.sendFailure(Component.translatable("bmcmod.commands.endstorm.back_failed"));
            return 0;
        }
        source.sendSuccess(() -> Component.translatable("bmcmod.commands.endstorm.back_success", player.getName()), true);
        return 1;
    }

    private static int runBackForTargets(CommandSourceStack source, Collection<ServerPlayer> players) {
        int count = 0;
        for (ServerPlayer sp : players) {
            if (EndStormManager.forceBackOneWave(sp)) {
                count++;
            }
        }
        if (count <= 0) {
            source.sendFailure(Component.translatable("bmcmod.commands.endstorm.back_failed"));
            return 0;
        }
        int c = count;
        source.sendSuccess(() -> Component.translatable("bmcmod.commands.endstorm.back_success_many", c), true);
        return count;
    }

    private static int runVictoryLoot(CommandSourceStack source, ServerPlayer player, int level) {
        if (!(player.level() instanceof ServerLevel sl)) {
            source.sendFailure(Component.translatable("bmcmod.commands.endstorm.spawn_failed"));
            return 0;
        }
        EndStormManager.spawnVictoryRewardChestForCommand(sl, player, level);
        source.sendSuccess(() -> Component.translatable("bmcmod.commands.endstorm.victoryloot_success", level), true);
        return 1;
    }

    private static int runSpawn(CommandSourceStack source, String kind, int level, int wave) throws com.mojang.brigadier.exceptions.CommandSyntaxException {
        ServerPlayer player = source.getPlayerOrException();
        if (!(player.level() instanceof ServerLevel sl)) {
            source.sendFailure(Component.translatable("bmcmod.commands.endstorm.spawn_failed"));
            return 0;
        }
        String k = kind.toLowerCase(Locale.ROOT);
        if (!EndStormManager.DEBUG_SPAWN_KINDS.contains(k)) {
            source.sendFailure(Component.translatable("bmcmod.commands.endstorm.spawn_unknown", kind));
            return 0;
        }
        if (!EndStormManager.spawnDebugStormMob(sl, player, k, level, wave)) {
            source.sendFailure(Component.translatable("bmcmod.commands.endstorm.spawn_unknown", kind));
            return 0;
        }
        source.sendSuccess(() -> Component.translatable("bmcmod.commands.endstorm.spawn_success", kind, level, wave), true);
        return 1;
    }
}

