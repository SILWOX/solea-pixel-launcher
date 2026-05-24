package com.soleapixel.bmcmod.event;

import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.network.protocol.game.ClientboundSetActionBarTextPacket;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.RandomSource;
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
import com.soleapixel.bmcmod.item.CrystalItem;
import com.soleapixel.bmcmod.registry.ModItems;
import com.soleapixel.bmcmod.soul.SoulCaptureTuning;

import org.jetbrains.annotations.Nullable;

@EventBusSubscriber(modid = BmcMod.MODID)
public final class CrystalSoulEvents {
    private CrystalSoulEvents() {
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

        ServerPlayer killer = resolveKiller(victim, event.getSource());
        if (killer == null) {
            return;
        }

        ItemStack off = killer.getOffhandItem();
        if (off.getItem() != ModItems.CRYSTAL.get()) {
            return;
        }

        float chance = SoulCaptureTuning.getCaptureChance(victim);
        RandomSource random = victim.getRandom();
        if (random.nextFloat() > chance) {
            return;
        }

        CrystalItem.addSoul(off, victim.getType());

        ServerLevel level = (ServerLevel) victim.level();
        double x = victim.getX();
        double y = victim.getY() + victim.getBbHeight() * 0.5;
        double z = victim.getZ();
        spawnSoulParticles(level, x, y, z, random);

        Component mobName = victim.hasCustomName() && victim.getCustomName() != null
                ? victim.getCustomName().copy()
                : Component.translatable(victim.getType().getDescriptionId());

        Component captureMsg = Component.translatable("message.bmcmod.crystal.soul_captured", mobName);
        killer.connection.send(new ClientboundSetActionBarTextPacket(captureMsg));

        // Sons audibles côté joueur (l’âme s’échappant + petite confirmation type XP).
        double sx = killer.getX();
        double sy = killer.getEyeY();
        double sz = killer.getZ();
        level.playSound(
                null, sx, sy, sz, SoundEvents.SOUL_ESCAPE, SoundSource.PLAYERS, 0.8F, 0.9F + 0.2F * random.nextFloat());
        level.playSound(
                null, sx, sy, sz, SoundEvents.EXPERIENCE_ORB_PICKUP, SoundSource.PLAYERS, 0.4F, 0.6F + 0.1F * random.nextFloat());
    }

    private static void spawnSoulParticles(ServerLevel level, double x, double y, double z, RandomSource random) {
        for (int i = 0; i < 32; i++) {
            double ox = (random.nextDouble() - 0.5) * 0.8;
            double oy = (random.nextDouble() - 0.5) * 0.5;
            double oz = (random.nextDouble() - 0.5) * 0.8;
            level.sendParticles(ParticleTypes.SOUL, x + ox, y + oy, z + oz, 1, 0, 0.04, 0, 0.04);
        }
        for (int i = 0; i < 12; i++) {
            double ox = (random.nextDouble() - 0.5) * 0.5;
            double oz = (random.nextDouble() - 0.5) * 0.5;
            level.sendParticles(ParticleTypes.SCULK_SOUL, x + ox, y, z + oz, 1, 0, 0.08, 0, 0.04);
        }
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
