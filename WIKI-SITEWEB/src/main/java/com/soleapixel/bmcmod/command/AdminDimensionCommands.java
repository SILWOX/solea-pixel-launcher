package com.soleapixel.bmcmod.command;

import com.mojang.brigadier.CommandDispatcher;

import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.level.Level;

import com.soleapixel.bmcmod.BmcMod;

import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.RegisterCommandsEvent;

/**
 * Commandes admin (perm. 4) : téléportation vers un autre monde en conservant la position et la rotation.
 */
@EventBusSubscriber(modid = BmcMod.MODID)
public final class AdminDimensionCommands {
    private AdminDimensionCommands() {
    }

    @SubscribeEvent
    public static void register(RegisterCommandsEvent event) {
        CommandDispatcher<CommandSourceStack> dispatcher = event.getDispatcher();
        dispatcher.register(Commands.literal("nether")
                .requires(src -> src.hasPermission(4))
                .executes(ctx -> teleportKeepingPosition(ctx.getSource(), Level.NETHER)));
        dispatcher.register(Commands.literal("end")
                .requires(src -> src.hasPermission(4))
                .executes(ctx -> teleportKeepingPosition(ctx.getSource(), Level.END)));
        dispatcher.register(Commands.literal("overworld")
                .requires(src -> src.hasPermission(4))
                .executes(ctx -> teleportKeepingPosition(ctx.getSource(), Level.OVERWORLD)));
    }

    private static int teleportKeepingPosition(CommandSourceStack source, ResourceKey<Level> dimension) {
        if (!(source.getEntity() instanceof ServerPlayer player)) {
            source.sendFailure(Component.translatable("bmcmod.commands.dimension.players_only"));
            return 0;
        }
        ServerLevel target = source.getServer().getLevel(dimension);
        if (target == null) {
            source.sendFailure(Component.translatable("bmcmod.commands.dimension.unavailable"));
            return 0;
        }
        if (player.level() == target) {
            source.sendSuccess(() -> Component.translatable("bmcmod.commands.dimension.already_here"), true);
            return 1;
        }
        double x = player.getX();
        double y = player.getY();
        double z = player.getZ();
        float yaw = player.getYRot();
        float pitch = player.getXRot();
        player.teleportTo(target, x, y, z, yaw, pitch);
        source.sendSuccess(
                () -> Component.translatable("bmcmod.commands.dimension.teleported", target.dimension().location().toString()),
                true);
        return 1;
    }
}
