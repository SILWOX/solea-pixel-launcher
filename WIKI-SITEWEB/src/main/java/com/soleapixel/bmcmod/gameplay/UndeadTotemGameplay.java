package com.soleapixel.bmcmod.gameplay;

import com.soleapixel.bmcmod.BmcMod;
import com.soleapixel.bmcmod.entity.CloneEntity;
import com.soleapixel.bmcmod.item.UndeadTotemItem;
import com.soleapixel.bmcmod.registry.ModEntities;
import com.soleapixel.bmcmod.registry.ModItems;
import java.util.ArrayList;
import java.util.List;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.EntityEvent;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.phys.AABB;
import net.neoforged.bus.api.EventPriority;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.entity.living.LivingIncomingDamageEvent;

@EventBusSubscriber(modid = BmcMod.MODID)
public final class UndeadTotemGameplay {
    private static final String TAG_OWNER_CLONE = "bmcmod_clone_owner";

    private UndeadTotemGameplay() {
    }

    @SubscribeEvent(priority = EventPriority.HIGHEST)
    public static void onLivingIncomingDamage(LivingIncomingDamageEvent event) {
        if (!(event.getEntity() instanceof ServerPlayer player) || player.level().isClientSide()) {
            return;
        }
        float incoming = event.getAmount();
        if (incoming < player.getHealth()) {
            return;
        }
        InteractionHand hand = findUndeadTotemHand(player);
        if (hand == null) {
            return;
        }
        ItemStack totem = player.getItemInHand(hand);
        if (totem.getMaxDamage() - totem.getDamageValue() < UndeadTotemItem.REVIVE_DAMAGE_COST) {
            return;
        }
        event.setAmount(0.0F);
        player.setHealth(1.0F);
        player.removeAllEffects();
        player.addEffect(new MobEffectInstance(MobEffects.REGENERATION, 900, 1));
        player.addEffect(new MobEffectInstance(MobEffects.ABSORPTION, 100, 1));
        player.addEffect(new MobEffectInstance(MobEffects.FIRE_RESISTANCE, 800, 0));
        totem.hurtAndBreak(UndeadTotemItem.REVIVE_DAMAGE_COST, player, LivingEntity.getSlotForHand(hand));
        if (player.level() instanceof ServerLevel sl) {
            sl.broadcastEntityEvent(player, EntityEvent.TALISMAN_ACTIVATE);
            sl.sendParticles(ParticleTypes.TOTEM_OF_UNDYING, player.getX(), player.getY() + 1.0D, player.getZ(), 40, 0.45D, 0.7D, 0.45D, 0.1D);
            sl.playSound(null, player.getX(), player.getY(), player.getZ(), SoundEvents.TOTEM_USE, SoundSource.PLAYERS, 1.0F, 1.0F);
        }
    }

    public static void handleSummonPacket(ServerPlayer player, boolean slimModel) {
        if (player == null || player.level().isClientSide() || !(player.level() instanceof ServerLevel level)) {
            return;
        }
        InteractionHand hand = findUndeadTotemHand(player);
        if (hand == null) {
            return;
        }
        ItemStack totem = player.getItemInHand(hand);
        if (totem.getMaxDamage() - totem.getDamageValue() < UndeadTotemItem.CLONE_DAMAGE_COST) {
            return;
        }
        removeOwnedClones(level, player);
        for (int i = 0; i < 3; i++) {
            CloneEntity clone = ModEntities.CLONE.get().create(level);
            if (clone == null) {
                continue;
            }
            double angle = (Math.PI * 2.0D / 3.0D) * i;
            double x = player.getX() + Math.cos(angle) * 1.3D;
            double z = player.getZ() + Math.sin(angle) * 1.3D;
            clone.moveTo(x, player.getY(), z, player.getYRot(), player.getXRot());
            clone.initializeFromOwner(player, slimModel);
            clone.getPersistentData().putUUID(TAG_OWNER_CLONE, player.getUUID());
            level.addFreshEntity(clone);
        }
        totem.hurtAndBreak(UndeadTotemItem.CLONE_DAMAGE_COST, player, LivingEntity.getSlotForHand(hand));
        level.playSound(null, player.getX(), player.getY(), player.getZ(), SoundEvents.ILLUSIONER_PREPARE_MIRROR, SoundSource.PLAYERS, 0.9F, 1.0F);
        level.sendParticles(ParticleTypes.SMOKE, player.getX(), player.getY() + 1.0D, player.getZ(), 28, 0.7D, 0.6D, 0.7D, 0.01D);
    }

    private static void removeOwnedClones(ServerLevel level, ServerPlayer owner) {
        for (CloneEntity clone : findOwnedClones(level, owner)) {
            clone.discard();
        }
    }

    public static List<CloneEntity> findOwnedClones(ServerLevel level, ServerPlayer owner) {
        AABB box = owner.getBoundingBox().inflate(128.0D);
        return new ArrayList<>(level.getEntitiesOfClass(CloneEntity.class, box,
                c -> owner.getUUID().equals(c.getOwnerUuid())
                        || (c.getPersistentData().hasUUID(TAG_OWNER_CLONE)
                                && owner.getUUID().equals(c.getPersistentData().getUUID(TAG_OWNER_CLONE)))));
    }

    private static InteractionHand findUndeadTotemHand(ServerPlayer player) {
        if (player.getMainHandItem().is(ModItems.UNDEAD_TOTEM.get())) {
            return InteractionHand.MAIN_HAND;
        }
        if (player.getOffhandItem().is(ModItems.UNDEAD_TOTEM.get())) {
            return InteractionHand.OFF_HAND;
        }
        return null;
    }
}
