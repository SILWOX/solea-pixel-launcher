package com.soleapixel.bmcmod.command;

import java.util.Optional;

import com.mojang.brigadier.arguments.IntegerArgumentType;
import com.mojang.brigadier.builder.LiteralArgumentBuilder;

import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;

import com.soleapixel.bmcmod.registry.ModItems;
import com.soleapixel.bmcmod.item.CrystalItem;

/**
 * {@code /bmc crystal set|add|remove|clear} — modifie les âmes du cristal ({@link ModItems#CRYSTAL}) tenu en main principale ou
 * en main secondaire (permission 2).
 */
public final class BmcCrystalCommand {
    /** Plafond Brigade : valeurs negatives interdites. */
    static final int MAX_ARG = 1_000_000_000;

    private BmcCrystalCommand() {
    }

    static LiteralArgumentBuilder<CommandSourceStack> buildCrystalBranch() {
        return Commands.literal("crystal")
                .requires(src -> src.hasPermission(2))
                .then(Commands.literal("clear").executes(ctx -> runClear(ctx.getSource())))
                .then(Commands.literal("set")
                        .then(Commands.argument("count", IntegerArgumentType.integer(0, MAX_ARG))
                                .executes(ctx -> runSet(ctx.getSource(), IntegerArgumentType.getInteger(ctx, "count")))))
                .then(Commands.literal("add")
                        .then(Commands.argument("count", IntegerArgumentType.integer(0, MAX_ARG))
                                .executes(ctx -> runAdd(ctx.getSource(), IntegerArgumentType.getInteger(ctx, "count")))))
                .then(Commands.literal("remove")
                        .then(Commands.argument("count", IntegerArgumentType.integer(0, MAX_ARG))
                                .executes(ctx -> runRemove(
                                        ctx.getSource(), IntegerArgumentType.getInteger(ctx, "count")))));
    }

    private static int runClear(CommandSourceStack source) {
        return mutateHeldCrystal(source, stack -> {
            CrystalItem.clearAllSouls(stack);
            return CrystalItem.getTotalSoulCount(stack);
        }, "bmcmod.commands.crystal.success_clear");
    }

    private static int runSet(CommandSourceStack source, int count) {
        return mutateHeldCrystal(source, stack -> {
            CrystalItem.setTotalSoulCountAbsolute(stack, count);
            return CrystalItem.getTotalSoulCount(stack);
        }, "bmcmod.commands.crystal.success_set", Integer.valueOf(count));
    }

    private static int runAdd(CommandSourceStack source, int count) {
        return mutateHeldCrystal(source, stack -> {
            CrystalItem.addSoulsCommandBucket(stack, count);
            return CrystalItem.getTotalSoulCount(stack);
        }, "bmcmod.commands.crystal.success_add", Integer.valueOf(count));
    }

    private static int runRemove(CommandSourceStack source, int count) {
        return mutateHeldCrystal(source, stack -> {
            CrystalItem.removeSouls(stack, count);
            return CrystalItem.getTotalSoulCount(stack);
        }, "bmcmod.commands.crystal.success_remove", Integer.valueOf(count));
    }

    @FunctionalInterface
    private interface CrystalMutator {
        /** Applique la modification au stack déjà résolu ; retourne le total d’âmes après action. */
        int apply(ItemStack stack);
    }

    private static int mutateHeldCrystal(CommandSourceStack source, CrystalMutator mutator, String successKey) {
        return mutateHeldCrystal(source, mutator, successKey, null);
    }

    /**
     * @param deltaPourMessage si non null, premier argument du message ({@code remove}/{@code add}/{@code set}) avant le total.
     */
    private static int mutateHeldCrystal(
            CommandSourceStack source,
            CrystalMutator mutator,
            String successKey,
            Integer deltaPourMessage) {
        if (!(source.getEntity() instanceof ServerPlayer player)) {
            source.sendFailure(Component.translatable("bmcmod.commands.crystal.players_only"));
            return 0;
        }
        Optional<HeldCrystalSlot> slot = crystalInHands(player);
        if (slot.isEmpty()) {
            source.sendFailure(Component.translatable("bmcmod.commands.crystal.no_crystal"));
            return 0;
        }
        HeldCrystalSlot h = slot.get();
        ItemStack stack = player.getItemInHand(h.hand);
        int after = mutator.apply(stack);
        player.setItemInHand(h.hand, stack);
        if (deltaPourMessage != null) {
            int d = deltaPourMessage;
            source.sendSuccess(() -> Component.translatable(successKey, d, after), true);
        } else {
            source.sendSuccess(() -> Component.translatable(successKey, after), true);
        }
        return 1;
    }

    private record HeldCrystalSlot(InteractionHand hand) {}

    private static Optional<HeldCrystalSlot> crystalInHands(Player player) {
        ItemStack main = player.getMainHandItem();
        if (isInfusionCrystal(main)) {
            return Optional.of(new HeldCrystalSlot(InteractionHand.MAIN_HAND));
        }
        ItemStack off = player.getOffhandItem();
        if (isInfusionCrystal(off)) {
            return Optional.of(new HeldCrystalSlot(InteractionHand.OFF_HAND));
        }
        return Optional.empty();
    }

    private static boolean isInfusionCrystal(ItemStack stack) {
        return !stack.isEmpty() && stack.is(ModItems.CRYSTAL.get());
    }
}
