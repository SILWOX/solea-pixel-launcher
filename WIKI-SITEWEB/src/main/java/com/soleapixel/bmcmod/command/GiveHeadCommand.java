package com.soleapixel.bmcmod.command;

import java.util.Collection;
import java.util.List;

import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.arguments.StringArgumentType;

import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.commands.arguments.EntityArgument;
import net.minecraft.network.chat.Component;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.item.ItemStack;

import com.soleapixel.bmcmod.BmcMod;
import com.soleapixel.bmcmod.util.PlayerHeadProfileUtil;

import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.RegisterCommandsEvent;

/**
 * {@code /givehead <pseudo> [@cibles]} — tête de joueur avec skin Mojang (session + cache, pseudo hors-ligne résolu via API).
 */
@EventBusSubscriber(modid = BmcMod.MODID)
public final class GiveHeadCommand {
    private GiveHeadCommand() {
    }

    @SubscribeEvent
    public static void register(RegisterCommandsEvent event) {
        CommandDispatcher<CommandSourceStack> d = event.getDispatcher();
        d.register(Commands.literal("givehead")
                .requires(src -> src.hasPermission(2))
                .then(Commands.argument("name", StringArgumentType.word())
                        .executes(ctx -> {
                            CommandSourceStack src = ctx.getSource();
                            if (!(src.getEntity() instanceof ServerPlayer self)) {
                                src.sendFailure(Component.translatable("bmcmod.commands.givehead.console_needs_targets"));
                                return 0;
                            }
                            String name = StringArgumentType.getString(ctx, "name");
                            return giveHead(src, name, List.of(self));
                        })
                        .then(Commands.argument("targets", EntityArgument.players())
                                .executes(ctx -> {
                                    String name = StringArgumentType.getString(ctx, "name");
                                    Collection<ServerPlayer> targets = EntityArgument.getPlayers(ctx, "targets");
                                    return giveHead(ctx.getSource(), name, targets);
                                }))));
    }

    private static int giveHead(CommandSourceStack source, String skinName, Collection<ServerPlayer> targets) {
        MinecraftServer server = source.getServer();
        ItemStack template = PlayerHeadProfileUtil.createPlayerHead(skinName, server);
        int count = 0;
        for (ServerPlayer target : targets) {
            ItemStack copy = template.copy();
            if (!target.getInventory().add(copy)) {
                target.spawnAtLocation(copy);
            }
            count++;
        }
        int finalCount = count;
        String name = skinName;
        source.sendSuccess(
                () -> Component.translatable("bmcmod.commands.givehead.success", name, finalCount),
                true);
        return count;
    }
}
