package com.soleapixel.bmcmod.command;

import java.util.Collection;
import java.util.List;
import java.util.Locale;

import javax.annotation.Nullable;

import com.mojang.brigadier.arguments.IntegerArgumentType;
import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import com.mojang.brigadier.context.CommandContext;
import com.mojang.brigadier.exceptions.CommandSyntaxException;

import com.soleapixel.bmcmod.gameplay.UndeadInvasionManager;
import com.soleapixel.bmcmod.quest.QuestDifficulty;
import com.soleapixel.bmcmod.quest.QuestGenerator;

import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.commands.SharedSuggestionProvider;
import net.minecraft.commands.arguments.EntityArgument;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.util.RandomSource;
import net.minecraft.world.item.ItemStack;

/**
 * Sous-arbres {@code undeadinvasion} et {@code quest} rattachés à {@code /bmc} depuis {@link BmcModHelpCommand}
 * (un seul enregistrement Brigadier pour éviter d’écraser l’aide).
 */
public final class UndeadInvasionCommand {
    private UndeadInvasionCommand() {
    }

    public static LiteralArgumentBuilder<CommandSourceStack> buildUndeadInvasion() {
        return Commands.literal("undeadinvasion")
                .requires(src -> src.hasPermission(2))
                .then(Commands.literal("play")
                        .executes(ctx -> runPlayRandom(ctx.getSource(), ctx.getSource().getPlayerOrException()))
                        .then(Commands.argument("level", IntegerArgumentType.integer(1, 6))
                                .executes(ctx -> runPlay(
                                        ctx.getSource(),
                                        ctx.getSource().getPlayerOrException(),
                                        IntegerArgumentType.getInteger(ctx, "level")))
                                .then(Commands.argument("joueurs", EntityArgument.players())
                                        .executes(ctx -> runPlayForTargets(
                                                ctx.getSource(),
                                                EntityArgument.getPlayers(ctx, "joueurs"),
                                                IntegerArgumentType.getInteger(ctx, "level"))))))
                .then(Commands.literal("start")
                        .executes(ctx -> runStartRandom(ctx.getSource(), ctx.getSource().getPlayerOrException()))
                        .then(Commands.argument("level", IntegerArgumentType.integer(1, 6))
                                .executes(ctx -> runStart(
                                        ctx.getSource(),
                                        ctx.getSource().getPlayerOrException(),
                                        IntegerArgumentType.getInteger(ctx, "level"),
                                        false))
                                .then(Commands.literal("secret")
                                        .executes(ctx -> runStart(
                                                ctx.getSource(),
                                                ctx.getSource().getPlayerOrException(),
                                                IntegerArgumentType.getInteger(ctx, "level"),
                                                true))
                                        .then(Commands.argument("joueurs", EntityArgument.players())
                                                .executes(ctx -> runStartForTargets(
                                                        ctx.getSource(),
                                                        EntityArgument.getPlayers(ctx, "joueurs"),
                                                        IntegerArgumentType.getInteger(ctx, "level"),
                                                        true))))
                                .then(Commands.argument("joueurs", EntityArgument.players())
                                        .executes(ctx -> runStartForTargets(
                                                ctx.getSource(),
                                                EntityArgument.getPlayers(ctx, "joueurs"),
                                                IntegerArgumentType.getInteger(ctx, "level"),
                                                false)))))
                .then(Commands.literal("stop")
                        .executes(ctx -> runStop(ctx.getSource(), ctx.getSource().getPlayerOrException()))
                        .then(Commands.argument("joueurs", EntityArgument.players())
                                .executes(ctx -> runStopForTargets(ctx.getSource(), EntityArgument.getPlayers(ctx, "joueurs")))))
                .then(Commands.literal("get")
                        .executes(ctx -> runGet(ctx.getSource(), ctx.getSource().getPlayerOrException(), 64))
                        .then(Commands.argument("range", IntegerArgumentType.integer(8, 512))
                                .executes(ctx -> runGet(
                                        ctx.getSource(),
                                        ctx.getSource().getPlayerOrException(),
                                        IntegerArgumentType.getInteger(ctx, "range")))))
                .then(Commands.literal("status")
                        .executes(ctx -> runGet(ctx.getSource(), ctx.getSource().getPlayerOrException(), 128)))
                .then(Commands.literal("end")
                        .executes(ctx -> runEndWave(ctx.getSource(), ctx.getSource().getPlayerOrException()))
                        .then(Commands.argument("joueurs", EntityArgument.players())
                                .executes(ctx -> runEndWaveForTargets(ctx.getSource(), EntityArgument.getPlayers(ctx, "joueurs")))))
                .then(Commands.literal("back")
                        .executes(ctx -> runBackWave(ctx.getSource(), ctx.getSource().getPlayerOrException()))
                        .then(Commands.argument("joueurs", EntityArgument.players())
                                .executes(ctx -> runBackWaveForTargets(ctx.getSource(), EntityArgument.getPlayers(ctx, "joueurs")))))
                .then(Commands.literal("victoryloot")
                        .executes(ctx -> runVictoryLoot(ctx.getSource(), ctx.getSource().getPlayerOrException(), null))
                        .then(Commands.argument("level", IntegerArgumentType.integer(1, 6))
                                .executes(ctx -> runVictoryLoot(
                                        ctx.getSource(),
                                        ctx.getSource().getPlayerOrException(),
                                        IntegerArgumentType.getInteger(ctx, "level")))))
                .then(Commands.literal("spawn")
                        .then(Commands.argument("kind", StringArgumentType.word())
                                .suggests((ctx, builder) -> SharedSuggestionProvider.suggest(UndeadInvasionManager.DEBUG_SPAWN_KINDS, builder))
                                .then(Commands.argument("level", IntegerArgumentType.integer(1, 6))
                                        .executes(ctx -> runSpawnDebug(
                                                ctx.getSource(),
                                                StringArgumentType.getString(ctx, "kind"),
                                                IntegerArgumentType.getInteger(ctx, "level"),
                                                UndeadInvasionManager.getInvasionWaveCount(IntegerArgumentType.getInteger(ctx, "level"))))
                                        .then(Commands.argument("wave", IntegerArgumentType.integer(1, 32))
                                                .executes(ctx -> runSpawnDebug(
                                                        ctx.getSource(),
                                                        StringArgumentType.getString(ctx, "kind"),
                                                        IntegerArgumentType.getInteger(ctx, "level"),
                                                        IntegerArgumentType.getInteger(ctx, "wave")))))));
    }

    /** Suggestions de difficulté uniquement en anglais (les alias FR restent acceptés à la saisie). */
    public static LiteralArgumentBuilder<CommandSourceStack> buildQuestGive() {
        return Commands.literal("quest")
                .requires(src -> src.hasPermission(2))
                .then(Commands.literal("give")
                        .then(Commands.argument("difficulty", StringArgumentType.word())
                                .suggests((ctx, builder) -> SharedSuggestionProvider.suggest(
                                        List.of("easy", "normal", "hard", "extreme", "special"), builder))
                                .then(Commands.argument("minutes", IntegerArgumentType.integer(1, 480))
                                        .executes(ctx -> runQuestGiveFromSource(ctx))
                                        .then(Commands.argument("targets", EntityArgument.players())
                                                .executes(ctx -> runQuestGiveTargets(
                                                        ctx.getSource(),
                                                        StringArgumentType.getString(ctx, "difficulty"),
                                                        IntegerArgumentType.getInteger(ctx, "minutes"),
                                                        EntityArgument.getPlayers(ctx, "targets")))))));
    }

    private static int runQuestGiveFromSource(CommandContext<CommandSourceStack> ctx) throws CommandSyntaxException {
        CommandSourceStack source = ctx.getSource();
        if (!(source.getEntity() instanceof ServerPlayer self)) {
            source.sendFailure(Component.translatable("bmcmod.commands.quest.give.console_needs_targets"));
            return 0;
        }
        return runQuestGiveTargets(
                source,
                StringArgumentType.getString(ctx, "difficulty"),
                IntegerArgumentType.getInteger(ctx, "minutes"),
                List.of(self));
    }

    private static int runPlayRandom(CommandSourceStack source, ServerPlayer player) {
        int randomLevel = 1 + player.serverLevel().random.nextInt(6);
        return runPlay(source, player, randomLevel);
    }

    private static int runPlay(CommandSourceStack source, ServerPlayer player, int level) {
        boolean ok = UndeadInvasionManager.startForced(player, level);
        if (!ok) {
            source.sendFailure(Component.translatable("bmcmod.commands.undeadinvasion.play_failed"));
            return 0;
        }
        source.sendSuccess(() -> Component.translatable("bmcmod.commands.undeadinvasion.play_success", level, player.getName()), true);
        return 1;
    }

    private static int runPlayForTargets(CommandSourceStack source, Collection<ServerPlayer> players, int level) {
        int count = 0;
        for (ServerPlayer sp : players) {
            if (UndeadInvasionManager.startForced(sp, level)) {
                count++;
            }
        }
        final int finalCount = count;
        final int finalLevel = level;
        source.sendSuccess(() -> Component.translatable("bmcmod.commands.undeadinvasion.play_success_many", finalLevel, finalCount), true);
        return count;
    }

    private static int runStartRandom(CommandSourceStack source, ServerPlayer player) {
        int randomLevel = 1 + player.serverLevel().random.nextInt(6);
        return runStart(source, player, randomLevel, false);
    }

    private static int runStart(CommandSourceStack source, ServerPlayer player, int level, boolean forceSecretWave) {
        boolean ok = UndeadInvasionManager.startImmediate(player, level, forceSecretWave);
        if (!ok) {
            source.sendFailure(Component.translatable("bmcmod.commands.undeadinvasion.play_failed"));
            return 0;
        }
        source.sendSuccess(() -> Component.translatable("bmcmod.commands.undeadinvasion.start_success", level, player.getName()), true);
        return 1;
    }

    private static int runStartForTargets(CommandSourceStack source, Collection<ServerPlayer> players, int level, boolean forceSecretWave) {
        int count = 0;
        for (ServerPlayer sp : players) {
            if (UndeadInvasionManager.startImmediate(sp, level, forceSecretWave)) {
                count++;
            }
        }
        final int finalCount = count;
        final int finalLevel = level;
        source.sendSuccess(() -> Component.translatable("bmcmod.commands.undeadinvasion.start_success_many", finalLevel, finalCount), true);
        return count;
    }

    private static int runStop(CommandSourceStack source, ServerPlayer player) {
        boolean stopped = UndeadInvasionManager.stopForced(player);
        if (!stopped) {
            source.sendFailure(Component.translatable("bmcmod.commands.undeadinvasion.none"));
            return 0;
        }
        source.sendSuccess(() -> Component.translatable("bmcmod.commands.undeadinvasion.stop_success", player.getName()), true);
        return 1;
    }

    private static int runStopForTargets(CommandSourceStack source, Collection<ServerPlayer> players) {
        int count = 0;
        for (ServerPlayer sp : players) {
            if (UndeadInvasionManager.stopForced(sp)) {
                count++;
            }
        }
        final int finalCount = count;
        source.sendSuccess(() -> Component.translatable("bmcmod.commands.undeadinvasion.stop_success_many", finalCount), true);
        return count;
    }

    private static int runGet(CommandSourceStack source, ServerPlayer player, int range) {
        int level = UndeadInvasionManager.getCurrentLevelAround(player, range);
        if (level <= 0) {
            source.sendSuccess(() -> Component.translatable("bmcmod.commands.undeadinvasion.get_none", range), false);
            return 0;
        }
        source.sendSuccess(() -> Component.translatable("bmcmod.commands.undeadinvasion.get_found", level, range), false);
        return level;
    }

    private static int runEndWave(CommandSourceStack source, ServerPlayer player) {
        if (!UndeadInvasionManager.forceEndCurrentWave(player)) {
            source.sendFailure(Component.translatable("bmcmod.commands.undeadinvasion.end_failed"));
            return 0;
        }
        source.sendSuccess(() -> Component.translatable("bmcmod.commands.undeadinvasion.end_success", player.getName()), true);
        return 1;
    }

    private static int runEndWaveForTargets(CommandSourceStack source, Collection<ServerPlayer> players) {
        int count = 0;
        for (ServerPlayer sp : players) {
            if (UndeadInvasionManager.forceEndCurrentWave(sp)) {
                count++;
            }
        }
        if (count == 0) {
            source.sendFailure(Component.translatable("bmcmod.commands.undeadinvasion.end_failed"));
            return 0;
        }
        final int finalCount = count;
        source.sendSuccess(() -> Component.translatable("bmcmod.commands.undeadinvasion.end_success_many", finalCount), true);
        return count;
    }

    private static int runBackWave(CommandSourceStack source, ServerPlayer player) {
        if (!UndeadInvasionManager.forceBackOneWave(player)) {
            source.sendFailure(Component.translatable("bmcmod.commands.undeadinvasion.back_failed"));
            return 0;
        }
        source.sendSuccess(() -> Component.translatable("bmcmod.commands.undeadinvasion.back_success", player.getName()), true);
        return 1;
    }

    private static int runBackWaveForTargets(CommandSourceStack source, Collection<ServerPlayer> players) {
        int count = 0;
        for (ServerPlayer sp : players) {
            if (UndeadInvasionManager.forceBackOneWave(sp)) {
                count++;
            }
        }
        if (count == 0) {
            source.sendFailure(Component.translatable("bmcmod.commands.undeadinvasion.back_failed"));
            return 0;
        }
        final int finalCount = count;
        source.sendSuccess(() -> Component.translatable("bmcmod.commands.undeadinvasion.back_success_many", finalCount), true);
        return count;
    }

    private static int runVictoryLoot(CommandSourceStack source, ServerPlayer player, @Nullable Integer level) {
        if (!(player.level() instanceof ServerLevel sl)) {
            source.sendFailure(Component.translatable("bmcmod.commands.undeadinvasion.spawn_failed"));
            return 0;
        }
        int invasionLevel = level != null ? level : 1 + sl.random.nextInt(6);
        UndeadInvasionManager.spawnVictoryRewardChestForCommand(sl, player, invasionLevel);
        source.sendSuccess(() -> Component.translatable("bmcmod.commands.undeadinvasion.victoryloot_success", invasionLevel), true);
        return 1;
    }

    private static int runSpawnDebug(CommandSourceStack source, String kind, int level, int wave) throws CommandSyntaxException {
        ServerPlayer player = source.getPlayerOrException();
        if (!(player.level() instanceof ServerLevel sl)) {
            source.sendFailure(Component.translatable("bmcmod.commands.undeadinvasion.spawn_failed"));
            return 0;
        }
        String k = kind.toLowerCase(Locale.ROOT);
        if (!UndeadInvasionManager.DEBUG_SPAWN_KINDS.contains(k)) {
            source.sendFailure(Component.translatable("bmcmod.commands.undeadinvasion.spawn_unknown", kind));
            return 0;
        }
        boolean ok = UndeadInvasionManager.spawnDebugInvasionMob(sl, player, k, level, wave);
        if (!ok) {
            source.sendFailure(Component.translatable("bmcmod.commands.undeadinvasion.spawn_unknown", kind));
            return 0;
        }
        source.sendSuccess(() -> Component.translatable("bmcmod.commands.undeadinvasion.spawn_success", kind, level, wave), true);
        return 1;
    }

    private static int runQuestGiveTargets(
            CommandSourceStack source,
            String difficultyRaw,
            int minutes,
            Collection<ServerPlayer> targets) {
        QuestDifficulty diff = QuestDifficulty.tryParseCommand(difficultyRaw);
        if (diff == null) {
            source.sendFailure(Component.translatable("bmcmod.commands.quest.give.bad_difficulty", difficultyRaw));
            return 0;
        }
        int ticks = minutes * 60 * 20;
        int given = 0;
        for (ServerPlayer player : targets) {
            RandomSource r = player.getRandom();
            ItemStack stack = QuestGenerator.createQuestLog(r, diff, ticks, player.blockPosition());
            if (!player.getInventory().add(stack)) {
                player.drop(stack, false);
            }
            given++;
        }
        final int g = given;
        final String d = diff.name();
        source.sendSuccess(() -> Component.translatable("bmcmod.commands.quest.give.success", g, d, minutes), true);
        return given;
    }
}
