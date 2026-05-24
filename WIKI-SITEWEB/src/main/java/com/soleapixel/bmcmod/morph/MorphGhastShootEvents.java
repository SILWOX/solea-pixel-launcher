package com.soleapixel.bmcmod.morph;

import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.projectile.LargeFireball;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;
import net.neoforged.bus.api.EventPriority;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.entity.player.PlayerInteractEvent;

import com.soleapixel.bmcmod.BmcMod;
import com.soleapixel.bmcmod.item.MorphCrystalItem;

/**
 * Clic droit dans le vide en morph ghast : envoie une boule de feu (style ghast), avec un court cooldown.
 */
@EventBusSubscriber(modid = BmcMod.MODID)
public final class MorphGhastShootEvents {
    private static final String TAG_COOLDOWN = "BmcModGhastMorphShootCd";

    private MorphGhastShootEvents() {
    }

    @SubscribeEvent(priority = EventPriority.HIGH)
    public static void onRightClickEmpty(PlayerInteractEvent.RightClickEmpty event) {
        if (event.getLevel().isClientSide()) {
            return;
        }
        if (!(event.getEntity() instanceof ServerPlayer sp)) {
            return;
        }
        if (!MorphPlayerState.isMorphed(sp)) {
            return;
        }
        var id = MorphAppearanceIds.soulEntityId(MorphPlayerState.getMorphSoul(sp));
        if (id == null || !"ghast".equals(id.getPath())) {
            return;
        }
        if (event.getHand() != InteractionHand.MAIN_HAND) {
            return;
        }
        ItemStack off = sp.getOffhandItem();
        if (off.getItem() instanceof MorphCrystalItem) {
            return;
        }
        ItemStack main = sp.getMainHandItem();
        if (!main.isEmpty()) {
            return;
        }
        if (!crosshairMiss(sp)) {
            return;
        }
        if (sp.isCreative()) {
            shoot(sp);
            return;
        }
        int cd = sp.getPersistentData().getInt(TAG_COOLDOWN);
        if (cd > 0) {
            return;
        }
        shoot(sp);
        sp.getPersistentData().putInt(TAG_COOLDOWN, 40);
    }

    @SubscribeEvent
    public static void onPlayerTick(net.neoforged.neoforge.event.tick.PlayerTickEvent.Post event) {
        if (!(event.getEntity() instanceof ServerPlayer sp) || sp.level().isClientSide()) {
            return;
        }
        if (!sp.getPersistentData().contains(TAG_COOLDOWN)) {
            return;
        }
        int cd = sp.getPersistentData().getInt(TAG_COOLDOWN);
        if (cd <= 0) {
            sp.getPersistentData().remove(TAG_COOLDOWN);
            return;
        }
        sp.getPersistentData().putInt(TAG_COOLDOWN, cd - 1);
    }

    private static boolean crosshairMiss(ServerPlayer sp) {
        double reach = Math.max(sp.blockInteractionRange(), sp.entityInteractionRange());
        HitResult hit = sp.pick(reach, 1.0F, false);
        return hit.getType() == HitResult.Type.MISS;
    }

    private static void shoot(ServerPlayer sp) {
        if (!(sp.level() instanceof ServerLevel server)) {
            return;
        }
        Vec3 eye = sp.getEyePosition(1.0F);
        Vec3 look = sp.getLookAngle();
        Vec3 move = look.scale(0.18);
        LargeFireball ball = new LargeFireball(server, sp, move, 2);
        ball.setPos(eye);
        server.addFreshEntity(ball);
        sp.swing(InteractionHand.MAIN_HAND, true);
    }
}
