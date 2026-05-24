package com.soleapixel.bmcmod.gameplay;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.stats.Stats;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;

import com.soleapixel.bmcmod.block.feeder.FeederBlock;
import com.soleapixel.bmcmod.block.feeder.FeederBlockEntity;
import com.soleapixel.bmcmod.registry.ModBlocks;

import net.neoforged.bus.api.EventPriority;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.neoforge.event.entity.player.PlayerInteractEvent;

/**
 * Clic gauche sur la face avant du feeder : retire la nourriture sans casser le bloc.
 * Les autres faces : cassage normal (y compris plein → contenu droppé par {@link FeederBlock}).
 * Sneak + clic droit : remplissage massif (voir aussi {@link FeederBlock#trySneakBulkFill}, event prioritaire).
 */
public final class FeederInteractionEvents {
    private FeederInteractionEvents() {
    }

    /**
     * Traite le sneak + clic droit avant l’interaction item/bloc vanilla : évite l’ordre des mains et les retours
     * {@code CONSUME} qui bloquent le second passage.
     */
    @SubscribeEvent(priority = EventPriority.HIGH)
    public static void onRightClickBlockSneakBulkFill(PlayerInteractEvent.RightClickBlock event) {
        Level level = event.getLevel();
        if (level.isClientSide()) {
            return;
        }
        if (!event.getEntity().isShiftKeyDown()) {
            return;
        }
        if (event.getHand() != InteractionHand.MAIN_HAND) {
            return;
        }
        BlockPos pos = event.getPos();
        if (!level.getBlockState(pos).is(ModBlocks.FEEDER.get())) {
            return;
        }
        if (!(level.getBlockEntity(pos) instanceof FeederBlockEntity be)) {
            return;
        }
        if (!(event.getEntity() instanceof ServerPlayer sp) || !(level instanceof ServerLevel sl)) {
            return;
        }
        int n = FeederBlock.trySneakBulkFill(sl, sp, be);
        if (n > 0) {
            event.setCanceled(true);
        }
    }

    @SubscribeEvent
    public static void onLeftClickBlock(PlayerInteractEvent.LeftClickBlock event) {
        Level level = event.getLevel();
        BlockPos pos = event.getPos();
        if (!(level.getBlockEntity(pos) instanceof FeederBlockEntity be)) {
            return;
        }
        if (!level.getBlockState(pos).is(ModBlocks.FEEDER.get())) {
            return;
        }
        if (be.isEmpty()) {
            return;
        }
        Direction hitFace = event.getFace();
        Direction front = level.getBlockState(pos).getValue(BlockStateProperties.HORIZONTAL_FACING);
        if (hitFace != front) {
            return;
        }

        event.setCanceled(true);

        if (!(level instanceof ServerLevel sl) || !(event.getEntity() instanceof ServerPlayer player)) {
            return;
        }

        int maxTake;
        if (player.isShiftKeyDown()) {
            maxTake = new ItemStack(be.getRepresentativeStack().getItem()).getMaxStackSize();
        } else {
            maxTake = 1;
        }

        net.minecraft.world.item.Item statItem = be.getRepresentativeStack().getItem();
        ItemStack extracted = be.extractItems(maxTake);
        if (extracted.isEmpty()) {
            return;
        }
        InteractionHand hand = InteractionHand.MAIN_HAND;
        ItemStack held = player.getItemInHand(hand);
        if (!held.isEmpty()) {
            if (!ItemStack.isSameItemSameComponents(held, extracted)) {
                player.drop(extracted, false);
            } else {
                int fit = Math.min(extracted.getCount(), held.getMaxStackSize() - held.getCount());
                if (fit <= 0) {
                    player.drop(extracted, false);
                } else {
                    held.grow(fit);
                    extracted.shrink(fit);
                    if (!extracted.isEmpty()) {
                        player.drop(extracted, false);
                    }
                }
            }
        } else {
            player.setItemInHand(hand, extracted);
        }
        player.awardStat(Stats.ITEM_USED.get(statItem));
        sl.playSound(null, pos, SoundEvents.ITEM_PICKUP, SoundSource.BLOCKS, 0.25F, 1.1F);
    }
}
