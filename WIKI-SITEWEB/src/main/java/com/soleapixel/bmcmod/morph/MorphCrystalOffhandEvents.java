package com.soleapixel.bmcmod.morph;

import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.phys.HitResult;
import net.minecraft.server.level.ServerPlayer;
import net.neoforged.bus.api.EventPriority;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;

import com.soleapixel.bmcmod.BmcMod;
import com.soleapixel.bmcmod.item.MorphCrystalItem;

import net.neoforged.neoforge.event.entity.player.PlayerInteractEvent;

/**
 * Avec une épée / objet en main principale qui renvoie {@link InteractionResult#FAIL} sur {@code Item#use},
 * vanilla n’essaie pas la main secondaire — le cristal en off-hand ne se lançait jamais (pas de particules).
 * On laisse passer la main principale (PASS) quand le réticule est dans le vide et que le morph off-hand est utilisable.
 */
@EventBusSubscriber(modid = BmcMod.MODID)
public final class MorphCrystalOffhandEvents {
    private MorphCrystalOffhandEvents() {
    }

    @SubscribeEvent(priority = EventPriority.HIGH)
    public static void yieldMainHandToOffHandMorph(PlayerInteractEvent.RightClickItem event) {
        if (event.getLevel().isClientSide()) {
            return;
        }
        if (!(event.getEntity() instanceof ServerPlayer sp)) {
            return;
        }
        if (event.getHand() != InteractionHand.MAIN_HAND) {
            return;
        }
        ItemStack off = sp.getOffhandItem();
        if (!(off.getItem() instanceof MorphCrystalItem)) {
            return;
        }
        if (!crosshairMiss(sp)) {
            return;
        }
        if (!canBeginMorphChannel(sp, off)) {
            return;
        }
        event.setCanceled(true);
        event.setCancellationResult(InteractionResult.PASS);
    }

    @SubscribeEvent(priority = EventPriority.LOWEST)
    public static void afterEmptyClickTryOffHandMorph(PlayerInteractEvent.RightClickEmpty event) {
        if (event.getLevel().isClientSide()) {
            return;
        }
        if (!(event.getEntity() instanceof ServerPlayer sp)) {
            return;
        }
        tryBeginOffHandMorphUse(sp);
    }

    private static boolean crosshairMiss(Player player) {
        double reach = Math.max(player.blockInteractionRange(), player.entityInteractionRange());
        HitResult hit = player.pick(reach, 1.0F, false);
        return hit.getType() == HitResult.Type.MISS;
    }

    private static boolean canBeginMorphChannel(ServerPlayer sp, ItemStack off) {
        if (MorphCrystalServer.isMorphed(sp)) {
            return true;
        }
        return MorphCrystalSoul.hasSoul(off);
    }

    private static void tryBeginOffHandMorphUse(ServerPlayer sp) {
        if (sp.isUsingItem()) {
            return;
        }
        ItemStack off = sp.getOffhandItem();
        if (!(off.getItem() instanceof MorphCrystalItem)) {
            return;
        }
        if (!canBeginMorphChannel(sp, off)) {
            return;
        }
        off.use(sp.level(), sp, InteractionHand.OFF_HAND);
    }
}
