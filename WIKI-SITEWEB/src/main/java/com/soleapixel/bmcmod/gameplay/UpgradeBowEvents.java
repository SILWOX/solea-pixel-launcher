package com.soleapixel.bmcmod.gameplay;

import com.soleapixel.bmcmod.BmcMod;
import com.soleapixel.bmcmod.item.UpgradeBowItem;

import net.minecraft.server.level.ServerPlayer;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.projectile.AbstractArrow;
import net.minecraft.world.item.ItemStack;
import net.neoforged.bus.api.EventPriority;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.entity.EntityJoinLevelEvent;
import net.neoforged.neoforge.event.entity.player.ArrowLooseEvent;

/** Bonus des arcs upgradés (portée + dégâts) via tir à l'arc vanilla. */
@EventBusSubscriber(modid = BmcMod.MODID)
public final class UpgradeBowEvents {
    private static final String PENDING_UPGRADE_BOW_ARROW = BmcMod.MODID + ":pending_upgrade_bow_arrow";
    private static final String PENDING_UPGRADE_BOW_VELOCITY = BmcMod.MODID + ":pending_upgrade_bow_velocity";
    private static final String PENDING_UPGRADE_BOW_DAMAGE = BmcMod.MODID + ":pending_upgrade_bow_damage";

    private UpgradeBowEvents() {
    }

    @SubscribeEvent(priority = EventPriority.HIGH)
    public static void onArrowLoose(ArrowLooseEvent event) {
        ItemStack bow = event.getBow();
        if (!(bow.getItem() instanceof UpgradeBowItem upgradeBow)) {
            return;
        }
        if (event.getEntity().level().isClientSide() || !(event.getEntity() instanceof ServerPlayer player)) {
            return;
        }
        player.getPersistentData().putInt(PENDING_UPGRADE_BOW_ARROW, 1);
        player.getPersistentData().putFloat(PENDING_UPGRADE_BOW_VELOCITY, upgradeBow.velocityScale());
        player.getPersistentData().putFloat(PENDING_UPGRADE_BOW_DAMAGE, upgradeBow.damageScale());
        int charge = event.getCharge();
        int tuned = Mth.clamp((int) (charge * upgradeBow.drawScale()), 1, 40);
        event.setCharge(tuned);
    }

    @SubscribeEvent
    public static void onArrowJoin(EntityJoinLevelEvent event) {
        if (event.getLevel().isClientSide() || !(event.getEntity() instanceof AbstractArrow arrow)) {
            return;
        }
        if (!(arrow.getOwner() instanceof ServerPlayer player)) {
            return;
        }
        int pending = player.getPersistentData().getInt(PENDING_UPGRADE_BOW_ARROW);
        if (pending <= 0) {
            return;
        }
        player.getPersistentData().putInt(PENDING_UPGRADE_BOW_ARROW, pending - 1);
        float velocityScale = player.getPersistentData().getFloat(PENDING_UPGRADE_BOW_VELOCITY);
        float damageScale = player.getPersistentData().getFloat(PENDING_UPGRADE_BOW_DAMAGE);
        arrow.setDeltaMovement(arrow.getDeltaMovement().scale(Math.max(1.0F, velocityScale)));
        // Slight spread to avoid laser-like precision on upgraded bows.
        double spread = Math.max(0.0F, velocityScale - 1.0F) * 0.06D;
        if (spread > 0.0D) {
            arrow.setDeltaMovement(arrow.getDeltaMovement().add(
                    player.getRandom().nextGaussian() * spread,
                    player.getRandom().nextGaussian() * (spread * 0.35D),
                    player.getRandom().nextGaussian() * spread));
        }
        arrow.setBaseDamage(arrow.getBaseDamage() * Math.max(1.0F, damageScale));
    }
}
