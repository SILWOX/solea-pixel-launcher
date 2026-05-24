package com.soleapixel.bmcmod.morph;

import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.network.protocol.game.ClientboundSetActionBarTextPacket;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.TamableAnimal;
import net.minecraft.world.entity.decoration.ArmorStand;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.entity.projectile.Projectile;
import net.minecraft.world.item.ItemStack;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.entity.living.LivingDeathEvent;

import com.soleapixel.bmcmod.BmcMod;
import com.soleapixel.bmcmod.registry.ModItems;

import org.jetbrains.annotations.Nullable;

@EventBusSubscriber(modid = BmcMod.MODID)
public final class MorphCrystalCaptureEvents {
    private MorphCrystalCaptureEvents() {
    }

    @SubscribeEvent
    public static void onLivingDeath(LivingDeathEvent event) {
        LivingEntity victim = event.getEntity();
        if (victim.level().isClientSide() || victim instanceof Player) {
            return;
        }
        if (victim instanceof ArmorStand) {
            return;
        }
        if (MorphBossBlacklist.isBoss(victim)) {
            return;
        }

        ServerPlayer killer = resolveKiller(victim, event.getSource());
        if (killer == null) {
            return;
        }

        ItemStack off = killer.getOffhandItem();
        if (!off.is(ModItems.MORPH_CRYSTAL.get())) {
            return;
        }

        String encodeId = victim.getEncodeId();
        if (encodeId == null) {
            return;
        }
        CompoundTag soul = new CompoundTag();
        soul.putString("id", encodeId);
        victim.saveWithoutId(soul);
        MorphCrystalSoul.setSoul(off, MorphSoulSanitizer.sanitize(soul));
        killer.setItemInHand(net.minecraft.world.InteractionHand.OFF_HAND, off);

        ServerLevel level = (ServerLevel) victim.level();
        double x = victim.getX();
        double y = victim.getY() + victim.getBbHeight() * 0.5;
        double z = victim.getZ();
        for (int i = 0; i < 24; i++) {
            double ox = (level.random.nextDouble() - 0.5) * 0.8;
            double oy = (level.random.nextDouble() - 0.5) * 0.5;
            double oz = (level.random.nextDouble() - 0.5) * 0.8;
            level.sendParticles(net.minecraft.core.particles.ParticleTypes.SOUL, x + ox, y + oy, z + oz, 1, 0, 0.04, 0, 0.04);
        }

        Component mobName = victim.hasCustomName() && victim.getCustomName() != null
                ? victim.getCustomName().copy()
                : Component.translatable(victim.getType().getDescriptionId());
        killer.connection.send(
                new ClientboundSetActionBarTextPacket(Component.translatable("message.bmcmod.morph_crystal.captured", mobName)));
        level.playSound(null, killer.getX(), killer.getEyeY(), killer.getZ(), SoundEvents.SOUL_ESCAPE, SoundSource.PLAYERS, 0.9F, 1.0F);
    }

    @Nullable
    private static ServerPlayer resolveKiller(LivingEntity victim, DamageSource source) {
        Entity e = source.getEntity();
        if (e instanceof ServerPlayer sp) {
            return sp;
        }
        if (e instanceof Projectile p && p.getOwner() instanceof ServerPlayer sp) {
            return sp;
        }
        if (e instanceof TamableAnimal t && t.getOwner() instanceof ServerPlayer sp) {
            return sp;
        }
        return null;
    }
}
