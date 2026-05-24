package com.soleapixel.bmcmod.entity.projectile;

import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.Mth;
import net.minecraft.util.RandomSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.entity.projectile.ItemSupplier;
import net.minecraft.world.entity.projectile.ThrowableItemProjectile;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.chunk.status.ChunkStatus;
import net.minecraft.world.level.levelgen.Heightmap;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.EntityHitResult;
import net.minecraft.world.phys.Vec3;

import com.soleapixel.bmcmod.registry.ModItems;

/**
 * Perle instable : attire une cible au lanceur / revient au sol / chaos si elle touche le lanceur.
 */
public final class UnstablePearlProjectile extends ThrowableItemProjectile implements ItemSupplier {

    public UnstablePearlProjectile(net.minecraft.world.entity.EntityType<? extends UnstablePearlProjectile> type, Level level) {
        super(type, level);
    }

    @Override
    protected Item getDefaultItem() {
        return ModItems.UNSTABLE_PEARL.get();
    }

    @Override
    protected void onHitEntity(EntityHitResult result) {
        if (!(this.level() instanceof ServerLevel sl)) {
            return;
        }
        Entity victim = result.getEntity();
        Entity shooter = getOwner();
        if (!(shooter instanceof Player ownerPlayer)) {
            dropPearlAt(sl, this.position());
            this.discard();
            return;
        }
        if (victim.getUUID().equals(ownerPlayer.getUUID())) {
            if (ownerPlayer instanceof ServerPlayer sp) {
                chaosTeleportSelf(sl, sp);
            }
            this.discard();
            return;
        }
        if (victim instanceof LivingEntity living && victim != shooter) {
            teleportVictimNearOwner(sl, ownerPlayer, living);
            returnPearlTo(ownerPlayer);
            sl.playSound(null, getX(), getY(), getZ(), SoundEvents.ENDERMAN_TELEPORT, SoundSource.PLAYERS, 0.9F, 1.15F);
            this.discard();
            return;
        }
        returnPearlTo(ownerPlayer);
        this.discard();
    }

    @Override
    protected void onHitBlock(BlockHitResult result) {
        if (!(this.level() instanceof ServerLevel sl)) {
            return;
        }
        Entity shooter = getOwner();
        if (shooter instanceof Player ownerPlayer) {
            returnPearlTo(ownerPlayer);
            sl.playSound(null, getX(), getY(), getZ(), SoundEvents.ENDER_EYE_DEATH, SoundSource.NEUTRAL, 0.35F, 1.4F);
        } else {
            dropPearlAt(sl, this.position());
        }
        this.discard();
    }

    private static void returnPearlTo(Player owner) {
        ItemStack give = new ItemStack(ModItems.UNSTABLE_PEARL.get(), 1);
        if (!owner.getInventory().add(give)) {
            owner.drop(give, false);
        }
    }

    private static void dropPearlAt(ServerLevel level, Vec3 pos) {
        ItemStack stack = new ItemStack(ModItems.UNSTABLE_PEARL.get());
        net.minecraft.world.entity.item.ItemEntity ie =
                new net.minecraft.world.entity.item.ItemEntity(level, pos.x, pos.y, pos.z, stack);
        ie.setDeltaMovement(Vec3.ZERO);
        level.addFreshEntity(ie);
    }

    private static void teleportVictimNearOwner(ServerLevel level, Player owner, LivingEntity victim) {
        Vec3 look = owner.getLookAngle();
        Vec3 horiz = new Vec3(look.x, 0.0, look.z);
        if (horiz.lengthSqr() < 1.0E-6) {
            horiz = new Vec3(0.0, 0.0, -1.0);
        } else {
            horiz = horiz.normalize();
        }
        double dist = 2.85;
        double tx = owner.getX() + horiz.x * dist;
        double tz = owner.getZ() + horiz.z * dist;
        int ix = Mth.floor(tx);
        int iz = Mth.floor(tz);
        double ty = level.getHeight(Heightmap.Types.MOTION_BLOCKING_NO_LEAVES, ix, iz)
                + (victim instanceof Player ? 0.01 : victim.getBbHeight() * 0.02);
        teleportLiving(level, victim, tx, ty, tz, victim.getYRot(), victim.getXRot());
        victim.resetFallDistance();
    }

    private static void teleportLiving(ServerLevel level, LivingEntity entity, double x, double y, double z, float yRot, float xRot) {
        if (entity instanceof ServerPlayer sp) {
            sp.teleportTo(level, x, y, z, yRot, xRot);
        } else {
            entity.teleportTo(x, y, z);
            entity.setYRot(yRot);
            entity.setXRot(xRot);
        }
    }

    private static void chaosTeleportSelf(ServerLevel level, ServerPlayer player) {
        double ox = player.getX();
        double oz = player.getZ();
        spawnChaosParticles(level, player);

        boolean ok = tryChaosLongTeleport(player, level, ox, oz);
        if (!ok) {
            player.displayClientMessage(net.minecraft.network.chat.Component.translatable("message.bmcmod.unstable_pearl.chaos_failed"), true);
            level.playSound(null, player.getX(), player.getY(), player.getZ(), SoundEvents.END_PORTAL_FRAME_FILL, SoundSource.PLAYERS, 0.5F, 0.6F);
            if (!player.getAbilities().instabuild) {
                ItemStack refund = new ItemStack(ModItems.UNSTABLE_PEARL.get(), 1);
                if (!player.getInventory().add(refund)) {
                    player.drop(refund, false);
                }
            }
            return;
        }
        spawnChaosParticles(level, player);
        level.playSound(null, player.getX(), player.getY(), player.getZ(), SoundEvents.ENDERMAN_TELEPORT, SoundSource.PLAYERS, 1.2F, 0.65F);
        level.playSound(null, player.getX(), player.getY(), player.getZ(), SoundEvents.UI_TOAST_CHALLENGE_COMPLETE, SoundSource.PLAYERS, 0.25F, 1.8F);
    }

    private static void spawnChaosParticles(ServerLevel level, LivingEntity around) {
        Vec3 p = around.position().add(0.0, around.getBbHeight() * 0.5, 0.0);
        for (int i = 0; i < 140; i++) {
            double ox = (level.random.nextDouble() - 0.5) * 2.4;
            double oy = (level.random.nextDouble() - 0.5) * 2.2;
            double oz = (level.random.nextDouble() - 0.5) * 2.4;
            var type = switch (level.random.nextInt(5)) {
                case 0 -> ParticleTypes.PORTAL;
                case 1 -> ParticleTypes.REVERSE_PORTAL;
                case 2 -> ParticleTypes.DRAGON_BREATH;
                case 3 -> ParticleTypes.END_ROD;
                default -> ParticleTypes.SOUL_FIRE_FLAME;
            };
            level.sendParticles(type, p.x + ox, p.y + oy, p.z + oz, 1, 0.02, 0.06, 0.02, 0.02);
        }
    }

    /**
     * TP horizontal aléatoire entre ~1000 et ~10000 blocs. Charge le chunk de destination (sinon
     * {@code hasChunk} reste faux et aucun essai ne réussit loin du joueur).
     */
    private static boolean tryChaosLongTeleport(ServerPlayer player, ServerLevel level, double originX, double originZ) {
        RandomSource rnd = level.random;
        for (int attempt = 0; attempt < 72; attempt++) {
            double angle = rnd.nextDouble() * Math.PI * 2;
            int dist = 1000 + rnd.nextInt(9001);
            double tx = originX + Math.cos(angle) * dist;
            double tz = originZ + Math.sin(angle) * dist;
            int ix = Mth.floor(tx);
            int iz = Mth.floor(tz);

            ChunkPos chunkPos = new ChunkPos(ix >> 4, iz >> 4);
            level.getChunkSource().getChunk(chunkPos.x, chunkPos.z, ChunkStatus.FULL, true);

            double ty = level.getHeight(Heightmap.Types.MOTION_BLOCKING_NO_LEAVES, ix, iz) + 0.01;
            double prevX = player.getX();
            double prevY = player.getY();
            double prevZ = player.getZ();
            float prevYaw = player.getYRot();
            float prevPitch = player.getXRot();

            player.teleportTo(level, tx, ty, tz, prevYaw, prevPitch);

            double dx = player.getX() - originX;
            double dz = player.getZ() - originZ;
            double horiz = Math.sqrt(dx * dx + dz * dz);
            if (horiz >= 850 && horiz <= 10100) {
                player.resetFallDistance();
                return true;
            }
            player.teleportTo(level, prevX, prevY, prevZ, prevYaw, prevPitch);
        }
        return false;
    }
}
