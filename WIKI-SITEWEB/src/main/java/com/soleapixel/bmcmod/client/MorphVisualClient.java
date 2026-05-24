package com.soleapixel.bmcmod.client;

import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Pose;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;

import com.soleapixel.bmcmod.morph.MorphAppearanceIds;
import com.soleapixel.bmcmod.morph.MorphSoulSanitizer;
import com.soleapixel.bmcmod.util.WalkAnimationStateCopy;

public final class MorphVisualClient {
    private static final Map<UUID, CompoundTag> SOULS = new ConcurrentHashMap<>();
    private static final Map<UUID, LivingEntity> PROXY_BY_PLAYER = new ConcurrentHashMap<>();
    private static final Map<UUID, String> PROXY_SIGNATURE = new ConcurrentHashMap<>();
    private static int cooldownTicksLeft;

    private MorphVisualClient() {
    }

    public static void setMorphVisual(UUID playerId, boolean active, CompoundTag soul) {
        releaseProxy(playerId);
        if (active && soul != null && !soul.isEmpty()) {
            SOULS.put(playerId, MorphSoulSanitizer.sanitize(soul));
        } else {
            SOULS.remove(playerId);
        }
    }

    public static boolean isMorphed(UUID playerId) {
        return SOULS.containsKey(playerId);
    }

    public static CompoundTag getSoul(UUID playerId) {
        return SOULS.getOrDefault(playerId, new CompoundTag());
    }

    public static void setCooldownHud(int ticks) {
        cooldownTicksLeft = ticks;
    }

    public static int getCooldownTicksLeft() {
        return cooldownTicksLeft;
    }

    /** Libère les proxies (changement de monde, déconnexion). */
    public static void clearAllProxies() {
        PROXY_BY_PLAYER.clear();
        PROXY_SIGNATURE.clear();
    }

    public static void clearProxy() {
        clearAllProxies();
    }

    private static void releaseProxy(UUID playerId) {
        PROXY_BY_PLAYER.remove(playerId);
        PROXY_SIGNATURE.remove(playerId);
    }

    private static String proxySignature(CompoundTag soul) {
        return soul.getString("id") + "|" + soul.getBoolean("IsBaby");
    }

    public static LivingEntity getOrCreateMorphProxy(ClientLevel level, Player player, CompoundTag soul) {
        CompoundTag cleaned = MorphSoulSanitizer.sanitize(soul);
        UUID pid = player.getUUID();
        String sig = proxySignature(cleaned);
        LivingEntity cached = PROXY_BY_PLAYER.get(pid);
        if (cached != null && sig.equals(PROXY_SIGNATURE.get(pid))) {
            syncProxyFromPlayer(cached, player, cleaned);
            return cached;
        }
        releaseProxy(pid);
        Entity created = EntityType.create(cleaned, level).orElse(null);
        if (!(created instanceof LivingEntity le)) {
            return null;
        }
        le.setHealth(le.getMaxHealth());
        le.setAbsorptionAmount(0);
        le.setRemainingFireTicks(0);
        le.setTicksFrozen(0);
        if (le.getPose() == Pose.DYING) {
            le.setPose(Pose.STANDING);
        }
        PROXY_BY_PLAYER.put(pid, le);
        PROXY_SIGNATURE.put(pid, sig);
        syncProxyFromPlayer(le, player, cleaned);
        return le;
    }

    private static void syncProxyFromPlayer(LivingEntity le, Player player, CompoundTag soulForSit) {
        le.moveTo(player.getX(), player.getY(), player.getZ(), player.getYRot(), player.getXRot());
        le.setYHeadRot(player.getYHeadRot());
        le.yHeadRotO = player.yHeadRotO;
        le.yBodyRot = player.yBodyRot;
        le.yBodyRotO = player.yBodyRotO;
        le.setXRot(player.getXRot());
        le.xRotO = player.xRotO;
        le.setYRot(player.getYRot());
        le.yRotO = player.yRotO;
        le.walkDist = player.walkDist;
        le.walkDistO = player.walkDistO;
        WalkAnimationStateCopy.copyFromTo(player.walkAnimation, le.walkAnimation);
        le.tickCount = player.tickCount;
        le.setShiftKeyDown(player.isShiftKeyDown());
        le.setSprinting(player.isSprinting());
        le.setSwimming(player.isSwimming());
        le.setInvisible(false);

        var morphId = MorphAppearanceIds.soulEntityId(soulForSit);
        if (morphId != null && MorphAppearanceIds.morphSupportsSitPose(morphId) && player.isShiftKeyDown()) {
            le.setPose(Pose.SITTING);
        } else {
            le.setPose(player.getPose());
        }

        le.attackAnim = player.attackAnim;
        le.oAttackAnim = player.oAttackAnim;
        le.swinging = player.swinging;
        le.swingTime = player.swingTime;
        le.swingingArm = player.swingingArm;
        le.hurtTime = player.hurtTime;
        le.hurtDuration = player.hurtDuration;

        for (EquipmentSlot slot : EquipmentSlot.values()) {
            le.setItemSlot(slot, player.getItemBySlot(slot).copy());
        }
        ItemStack main = player.getMainHandItem();
        ItemStack off = player.getOffhandItem();
        le.setItemInHand(InteractionHand.MAIN_HAND, main.copy());
        le.setItemInHand(InteractionHand.OFF_HAND, off.copy());

        if (player.isUsingItem()) {
            le.startUsingItem(player.getUsedItemHand());
        } else {
            le.stopUsingItem();
        }
    }
}
