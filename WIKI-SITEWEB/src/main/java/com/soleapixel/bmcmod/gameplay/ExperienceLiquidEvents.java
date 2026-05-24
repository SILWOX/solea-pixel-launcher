package com.soleapixel.bmcmod.gameplay;

import com.soleapixel.bmcmod.registry.ModDataComponents;
import com.soleapixel.bmcmod.registry.ModFluids;
import com.soleapixel.bmcmod.registry.ModItems;
import com.soleapixel.bmcmod.item.UnknownBookItem;

import net.minecraft.core.component.DataComponents;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.ExperienceOrb;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.enchantment.ItemEnchantments;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.neoforge.event.tick.EntityTickEvent;

/**
 * Liquide d’expérience : dégâts magiques + absorption d’XP pour les joueurs ; dissolution des {@link ItemEntity} en orbes (peu d’XP).
 */
public final class ExperienceLiquidEvents {

    @SubscribeEvent
    static void onEntityTickPost(EntityTickEvent.Post event) {
        Entity entity = event.getEntity();
        if (entity.level().isClientSide() || !(entity.level() instanceof ServerLevel sl)) {
            return;
        }
        var expType = ModFluids.EXPERIENCE_FLUID_TYPE.get();
        if (entity instanceof ServerPlayer player && !player.isDeadOrDying()) {
            if (player.isCreative() || player.isSpectator()) {
                return;
            }
            if (player.getFluidTypeHeight(expType) > 0.18) {
                if (player.tickCount % 40 == 0) {
                    player.hurt(player.damageSources().magic(), 1.25F);
                    int drain = 2 + sl.random.nextInt(7);
                    player.giveExperiencePoints(-drain);
                }
            }
        } else if (entity instanceof ItemEntity ie && !ie.getItem().isEmpty()) {
            if (ie.getFluidTypeHeight(expType) > 0.12) {
                ItemStack stack = ie.getItem();
                // Livre enchanté révélé (ou jeté) : ne pas recycler en orbes d’XP dans le lac.
                if (stack.is(Items.ENCHANTED_BOOK)) {
                    return;
                }
                if (stack.is(ModItems.UNKNOWN_BOOK.get())) {
                    if (sl.random.nextFloat() < 0.7F) {
                        ItemStack revealed;
                        if (UnknownBookItem.hasLatentEnchantments(stack)) {
                            ItemEnchantments latent = stack.get(ModDataComponents.LATENT_STORED_ENCHANTMENTS.get());
                            revealed = new ItemStack(Items.ENCHANTED_BOOK);
                            revealed.set(DataComponents.STORED_ENCHANTMENTS, latent);
                        } else {
                            revealed = UnknownBookLogic.createRevealedEnchantedBook(sl.random, sl.registryAccess());
                        }
                        ItemEntity drop = new ItemEntity(sl, ie.getX(), ie.getY(), ie.getZ(), revealed);
                        drop.setPickUpDelay(10);
                        sl.addFreshEntity(drop);
                    } else {
                        int xp = ExperienceLiquidXp.valueForDissolving(stack, sl, ie.blockPosition());
                        ExperienceOrb.award(sl, ie.position(), xp);
                    }
                    ExperienceLiquidDailyDissolveTracker.recordDissolve(sl);
                    RadiantSlimeSpawnLogic.onItemDissolvedInChunk(sl, ie.chunkPosition(), ie.blockPosition());
                    ie.discard();
                } else {
                    int xp = ExperienceLiquidXp.valueForDissolving(stack, ie.level(), ie.blockPosition());
                    ExperienceOrb.award(sl, ie.position(), xp);
                    ExperienceLiquidDailyDissolveTracker.recordDissolve(sl);
                    RadiantSlimeSpawnLogic.onItemDissolvedInChunk(sl, ie.chunkPosition(), ie.blockPosition());
                    ie.discard();
                }
            }
        }
    }
}
