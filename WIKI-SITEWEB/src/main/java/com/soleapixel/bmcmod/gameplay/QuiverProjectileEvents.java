package com.soleapixel.bmcmod.gameplay;

import net.minecraft.core.registries.Registries;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.BowItem;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.ProjectileWeaponItem;
import net.minecraft.world.item.enchantment.EnchantmentHelper;
import net.minecraft.world.item.enchantment.Enchantments;

import com.soleapixel.bmcmod.BmcMod;
import com.soleapixel.bmcmod.item.QuiverContents;
import com.soleapixel.bmcmod.item.QuiverHelper;
import com.soleapixel.bmcmod.registry.ModItems;

import net.neoforged.bus.api.EventPriority;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.entity.living.LivingGetProjectileEvent;
import net.neoforged.neoforge.event.entity.player.ArrowLooseEvent;

/**
 * Munitions : priorité au carquois actif (slot minimal 0–32) ; consommation au relâchement (arc / arbalète).
 */
@EventBusSubscriber(modid = BmcMod.MODID)
public final class QuiverProjectileEvents {
    /** Marque un tir dont la munition affichée vient du carquois actif (copie → consommation manuelle). */
    private static final String TAG_QUIVER_AMMO = BmcMod.MODID + ":quiver_active_shot";

    private QuiverProjectileEvents() {
    }

    @SubscribeEvent(priority = EventPriority.HIGH)
    public static void onLivingGetProjectile(LivingGetProjectileEvent event) {
        if (!(event.getEntity() instanceof Player player)) {
            return;
        }
        ItemStack weapon = event.getProjectileWeaponItemStack();
        if (!(weapon.getItem() instanceof ProjectileWeaponItem pwi)) {
            return;
        }

        int slot = QuiverHelper.findPrimaryQuiverSlot(player, ModItems.QUIVER.get());
        if (slot < 0) {
            if (player instanceof ServerPlayer sp) {
                sp.getPersistentData().remove(TAG_QUIVER_AMMO);
            }
            return;
        }

        ItemStack quiverStack = player.getInventory().getItem(slot);
        QuiverContents qc = QuiverHelper.normalize(QuiverHelper.get(quiverStack));
        int sel = qc.selectedIndex();
        ItemStack arrow = qc.getChannel(sel);
        if (arrow.isEmpty()) {
            int fi = qc.firstNonEmpty();
            if (fi >= 0) {
                arrow = qc.getChannel(fi);
            }
        }
        if (arrow.isEmpty()) {
            if (player instanceof ServerPlayer sp) {
                sp.getPersistentData().remove(TAG_QUIVER_AMMO);
            }
            return;
        }

        if (!pwi.getSupportedHeldProjectiles(weapon).test(arrow)) {
            if (player instanceof ServerPlayer sp) {
                sp.getPersistentData().remove(TAG_QUIVER_AMMO);
            }
            return;
        }

        event.setProjectileItemStack(arrow.copyWithCount(1));
        if (player instanceof ServerPlayer sp) {
            sp.getPersistentData().putBoolean(TAG_QUIVER_AMMO, true);
        }
    }

    @SubscribeEvent(priority = EventPriority.NORMAL)
    public static void onArrowLoose(ArrowLooseEvent event) {
        if (!(event.getEntity() instanceof ServerPlayer player)) {
            return;
        }
        if (!event.hasAmmo()) {
            player.getPersistentData().remove(TAG_QUIVER_AMMO);
            return;
        }
        if (!player.getPersistentData().getBoolean(TAG_QUIVER_AMMO)) {
            return;
        }
        player.getPersistentData().remove(TAG_QUIVER_AMMO);

        ItemStack weapon = event.getBow();
        if (weapon.getItem() instanceof BowItem) {
            var reg = player.registryAccess().lookupOrThrow(Registries.ENCHANTMENT);
            int infinity = reg.get(Enchantments.INFINITY)
                    .map(h -> EnchantmentHelper.getItemEnchantmentLevel(h, weapon))
                    .orElse(0);
            if (infinity > 0) {
                return;
            }
        }

        int slot = QuiverHelper.findPrimaryQuiverSlot(player, ModItems.QUIVER.get());
        if (slot < 0) {
            return;
        }
        ItemStack quiverStack = player.getInventory().getItem(slot);
        QuiverContents qc = QuiverHelper.normalize(QuiverHelper.get(quiverStack));
        ItemStack ref = qc.getChannel(qc.selectedIndex());
        if (ref.isEmpty()) {
            int fi = qc.firstNonEmpty();
            if (fi < 0) {
                return;
            }
            ref = qc.getChannel(fi);
        }
        if (ref.isEmpty()) {
            return;
        }

        int need = QuiverHelper.consumptionAmountForWeapon(player, weapon);
        QuiverHelper.consumeMatching(quiverStack, ref, need, false);
    }
}
