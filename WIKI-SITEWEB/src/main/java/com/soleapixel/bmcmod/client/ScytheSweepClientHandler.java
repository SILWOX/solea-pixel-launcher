package com.soleapixel.bmcmod.client;

import net.minecraft.client.Minecraft;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.entity.projectile.ProjectileUtil;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.EntityHitResult;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.ClientTickEvent;
import net.neoforged.neoforge.event.entity.player.PlayerInteractEvent;
import net.neoforged.neoforge.network.PacketDistributor;

import com.soleapixel.bmcmod.BmcMod;
import com.soleapixel.bmcmod.item.ScytheItem;
import com.soleapixel.bmcmod.network.ScythePackets;
import com.soleapixel.bmcmod.registry.ModItems;

/**
 * Tourbillon : maintien ~2 s clic droit pour armer, puis clic gauche (cible requise sans enchant ; avec Tourbillon, air suffit).
 * Un clic gauche avant la fin du chargement annule la progression.
 */
@EventBusSubscriber(modid = BmcMod.MODID, value = Dist.CLIENT)
public final class ScytheSweepClientHandler {
    /** Progression 0..{@link ScytheItem#SWEEP_ARM_HOLD_TICKS} tant que clic droit maintenu et pas encore armé. */
    private static int rightHoldTicks;
    /** {@code true} après maintien complet ; reste jusqu’au coup ou reset. */
    private static boolean sweepArmed;
    private static int fpSweepAnimTicks;
    /** Durée initiale de l’anim 1ʳᵉ personne pour ce tour (×2 avec Tourbillon). */
    private static int fpSweepAnimInitialTicks;
    /** max(anim FP, spin) pour la phase de la caméra. */
    private static int sweepCameraPhaseTotalTicks;
    private static int clientCooldownTicks;
    private static int suppressAttackTicksAfterSweep;
    /** Évite paquets redondants pour {@link ScythePackets.ScytheWhirlwindChargingPayload}. */
    private static boolean lastSentWhirlwindCharging;

    private ScytheSweepClientHandler() {
    }

    /** 0..1 pour prédicat modèle (balayage 1ʳᵉ personne). */
    public static float getSweepModelPull() {
        if (fpSweepAnimTicks <= 0) {
            return 0.0F;
        }
        int denom = fpSweepAnimInitialTicks > 0 ? fpSweepAnimInitialTicks : ScytheItem.SWEEP_VISUAL_FP_TICKS;
        int elapsed = denom - fpSweepAnimTicks;
        return Math.min(1.0F, elapsed / (float) denom);
    }

    public static int getFpSweepAnimInitialTicks() {
        return fpSweepAnimInitialTicks;
    }

    public static int getSweepCameraPhaseTotalTicks() {
        return sweepCameraPhaseTotalTicks;
    }

    public static int getFirstPersonSweepAnimTicks() {
        return fpSweepAnimTicks;
    }

    public static boolean isSweepArmed() {
        return sweepArmed;
    }

    /** 0..1 progression du maintien droit (affichage HUD) ; 1 si déjà armé. */
    public static float getWhirlwindChargeRatio() {
        if (sweepArmed) {
            return 1.0F;
        }
        return Math.min(1.0F, rightHoldTicks / (float) ScytheItem.SWEEP_ARM_HOLD_TICKS);
    }

    public static boolean shouldDisplayChargeHud(Minecraft mc) {
        if (mc.player == null || mc.options.hideGui) {
            return false;
        }
        if (!(mc.player.getMainHandItem().getItem() instanceof ScytheItem)) {
            return false;
        }
        if (clientCooldownTicks > 0) {
            return false;
        }
        return rightHoldTicks > 0 || sweepArmed;
    }

    /** Appelé depuis l’input : frappe avant la fin du chargement → annule la progression. */
    public static void onPrematureAttackWhileCharging() {
        if (sweepArmed) {
            return;
        }
        if (rightHoldTicks > 0 && rightHoldTicks < ScytheItem.SWEEP_ARM_HOLD_TICKS) {
            rightHoldTicks = 0;
            syncWhirlwindChargingToServer(false);
        }
    }

    /**
     * Bloque un coup vanilla juste après l’envoi du tourbillon (évite un hit en double).
     */
    public static boolean shouldSuppressAttackInteraction() {
        return suppressAttackTicksAfterSweep > 0;
    }

    /** Déclenche le tourbillon (paquet + FX) ; annule l’état armé. */
    public static void fireSweepAttack(int primaryEntityId) {
        Minecraft mc = Minecraft.getInstance();
        if (mc.player == null || mc.level == null) {
            return;
        }
        if (clientCooldownTicks > 0 || !sweepArmed) {
            return;
        }

        sweepArmed = false;
        rightHoldTicks = 0;
        syncWhirlwindChargingToServer(false);
        clientCooldownTicks = ScytheItem.SWEEP_COOLDOWN_TICKS;

        var stack = mc.player.getMainHandItem();
        var reg = mc.player.level().registryAccess();
        int fpDur = ScytheItem.sweepVisualFpTicks(stack, reg);
        int totalSpin = ScytheItem.sweepVisualTotalSpinTicks(stack, reg);
        int rotations = ScytheItem.sweepVisualFullRotations(stack, reg);

        fpSweepAnimInitialTicks = fpDur;
        fpSweepAnimTicks = fpDur;
        sweepCameraPhaseTotalTicks = Math.max(fpDur, totalSpin);
        suppressAttackTicksAfterSweep = 2;

        ScytheSpinVisualTracker.beginSpin(mc.player.getId(), totalSpin, rotations);
        PacketDistributor.sendToServer(new ScythePackets.ScytheSweepAttackPayload(primaryEntityId));
        mc.player.swing(InteractionHand.MAIN_HAND, true);
    }

    private static void clearScytheState() {
        rightHoldTicks = 0;
        sweepArmed = false;
        fpSweepAnimInitialTicks = 0;
        sweepCameraPhaseTotalTicks = 0;
        syncWhirlwindChargingToServer(false);
    }

    private static void syncWhirlwindChargingToServer(boolean charging) {
        if (charging == lastSentWhirlwindCharging) {
            return;
        }
        lastSentWhirlwindCharging = charging;
        PacketDistributor.sendToServer(new ScythePackets.ScytheWhirlwindChargingPayload(charging));
    }

    @SubscribeEvent
    public static void onClientTick(ClientTickEvent.Post event) {
        Minecraft mc = Minecraft.getInstance();
        if (mc.player == null || mc.level == null) {
            clearScytheState();
            return;
        }

        if (suppressAttackTicksAfterSweep > 0) {
            suppressAttackTicksAfterSweep--;
        }

        if (clientCooldownTicks > 0) {
            clientCooldownTicks--;
        }

        if (fpSweepAnimTicks > 0) {
            fpSweepAnimTicks--;
            if (fpSweepAnimTicks <= 0) {
                fpSweepAnimInitialTicks = 0;
            }
        }

        ScytheSpinVisualTracker.tick();
        if (ScytheSpinVisualTracker.getRemainingTicks(mc.player.getId()) <= 0 && fpSweepAnimTicks <= 0) {
            sweepCameraPhaseTotalTicks = 0;
        }

        var stack = mc.player.getMainHandItem();
        if (!(stack.getItem() instanceof ScytheItem)) {
            clearScytheState();
            return;
        }

        if (clientCooldownTicks > 0) {
            return;
        }

        if (mc.options.keyUse.isDown()) {
            if (!sweepArmed) {
                HitResult hit = mc.hitResult;
                boolean aimingBlock = hit != null && hit.getType() == HitResult.Type.BLOCK;
                // Bloque le début de charge uniquement si on visait déjà un bloc ; une fois la charge entamée (air / entité),
                // regarder un bloc ne réinitialise pas et ne déclenche pas le labour (voir paquet + useOn serveur).
                boolean chargeAlreadyStarted = rightHoldTicks > 0;
                if (aimingBlock && !chargeAlreadyStarted) {
                    rightHoldTicks = 0;
                } else {
                    rightHoldTicks++;
                    if (rightHoldTicks >= ScytheItem.SWEEP_ARM_HOLD_TICKS) {
                        sweepArmed = true;
                        rightHoldTicks = ScytheItem.SWEEP_ARM_HOLD_TICKS;
                    }
                }
            }
        } else {
            if (!sweepArmed) {
                rightHoldTicks = 0;
            }
        }

        syncWhirlwindChargingToServer(rightHoldTicks > 0 || sweepArmed);
    }

    /** Pas de clic bloc (labour) tant que la charge tourbillon est active — cohérent avec le serveur. */
    @SubscribeEvent
    public static void onRightClickBlockWhileWhirlwindCharging(PlayerInteractEvent.RightClickBlock event) {
        if (!event.getLevel().isClientSide()) {
            return;
        }
        Minecraft mc = Minecraft.getInstance();
        if (mc.player == null || event.getEntity() != mc.player) {
            return;
        }
        if (!(event.getItemStack().getItem() instanceof ScytheItem)) {
            return;
        }
        if (!mc.options.keyUse.isDown()) {
            return;
        }
        if (rightHoldTicks <= 0 && !sweepArmed) {
            return;
        }
        event.setCanceled(true);
    }

    /**
     * Entité vivante sous le réticule (comme le combat).
     */
    public static LivingEntity findLivingUnderCrosshair(Minecraft mc) {
        if (mc.player == null || mc.level == null) {
            return null;
        }
        ItemStack main = mc.player.getMainHandItem();
        double reach = mc.player.entityInteractionRange();
        if (main.getItem() instanceof ScytheItem) {
            reach = Math.max(reach, ScytheItem.sweepEffectiveRange(main, mc.level.registryAccess()));
        }
        Vec3 from = mc.player.getEyePosition(1.0F);
        Vec3 look = mc.player.getViewVector(1.0F);
        Vec3 to = from.add(look.scale(reach));
        AABB search = mc.player.getBoundingBox().expandTowards(look.scale(reach)).inflate(1.0D, 1.0D, 1.0D);
        EntityHitResult hit = ProjectileUtil.getEntityHitResult(
                mc.player, from, to, search, ScytheSweepClientHandler::isValidSweepTarget, reach * reach);
        if (hit != null && hit.getEntity() instanceof LivingEntity le) {
            if (mc.player.distanceToSqr(le) > reach * reach) {
                return null;
            }
            return le;
        }
        return null;
    }

    private static boolean isValidSweepTarget(Entity e) {
        Minecraft mc = Minecraft.getInstance();
        if (mc.player == null || !(e instanceof LivingEntity le) || !le.isAlive()) {
            return false;
        }
        if (!e.isPickable() || e.isSpectator()) {
            return false;
        }
        return e != mc.player;
    }

    static boolean isHoldingAnyScythe(net.minecraft.world.item.ItemStack stack) {
        return stack.is(ModItems.IRON_SCYTHE.get())
                || stack.is(ModItems.GOLDEN_SCYTHE.get())
                || stack.is(ModItems.DIAMOND_SCYTHE.get())
                || stack.is(ModItems.EMERALD_SCYTHE.get())
                || stack.is(ModItems.NETHERITE_SCYTHE.get())
                || stack.is(ModItems.ENDERITE_SCYTHE.get())
                || stack.is(ModItems.BOREAL_SCYTHE.get());
    }
}
