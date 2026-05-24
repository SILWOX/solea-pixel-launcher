package com.soleapixel.bmcmod.command;

import com.soleapixel.bmcmod.gameplay.BossEventManager;
import com.soleapixel.bmcmod.gameplay.BossEventRarity;

import java.util.Locale;

import com.mojang.brigadier.builder.LiteralArgumentBuilder;

import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;

/** Littéraux anglais {@code common} … {@code mythic} uniquement : plus de doublon FR/EN dans le TAB. */
public final class BossEventCommand {
    private BossEventCommand() {
    }

    public static LiteralArgumentBuilder<CommandSourceStack> build() {
        LiteralArgumentBuilder<CommandSourceStack> spawn = Commands.literal("spawn");
        for (BossEventRarity r : BossEventRarity.values()) {
            String lit = r.name().toLowerCase(Locale.ROOT);
            spawn.then(Commands.literal(lit).executes(ctx -> runSpawn(ctx.getSource(), r)));
        }
        return Commands.literal("bossevent")
                .requires(s -> s.hasPermission(2))
                .then(spawn);
    }

    private static int runSpawn(CommandSourceStack src, BossEventRarity rarity) {
        ServerPlayer player = src.getPlayer();
        if (player == null) {
            src.sendFailure(Component.translatable("bmcmod.commands.bossevent.spawn.need_player"));
            return 0;
        }
        ServerLevel level = player.serverLevel();
        BossEventManager.spawnBossForCommand(level, player, rarity, level.getRandom());
        src.sendSuccess(() -> Component.translatable(
                "bmcmod.commands.bossevent.spawn.success",
                Component.literal(rarity.name())), true);
        return 1;
    }
}
