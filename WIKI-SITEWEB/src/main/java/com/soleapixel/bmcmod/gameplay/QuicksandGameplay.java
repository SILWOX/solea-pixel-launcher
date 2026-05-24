package com.soleapixel.bmcmod.gameplay;

import com.soleapixel.bmcmod.BmcMod;
import com.soleapixel.bmcmod.block.QuicksandBlock;
import com.soleapixel.bmcmod.registry.ModDamageTypes;

import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.tick.EntityTickEvent;
import net.neoforged.neoforge.event.tick.PlayerTickEvent;

/**
 * Sable mouvant : tirage vers le bas ; tête immergée → dégâts {@link ModDamageTypes#QUICKSAND} à intervalle régulier
 * (pas de bulles d’air comme dans l’eau).
 */
@EventBusSubscriber(modid = BmcMod.MODID)
public final class QuicksandGameplay {
    /** Tirage vers le bas chaque tick quand on est dans ou sur le quicksand. */
    private static final double SINK_BOOST = 0.12D;
    /** Dégâts tête immergée : pas chaque tick (plus lent que la noyade instantanée). */
    private static final int HEAD_DAMAGE_INTERVAL_TICKS = 22;
    private static final float HEAD_DAMAGE_AMOUNT = 2.0F;

    private QuicksandGameplay() {
    }

    /** Joueurs : {@link PlayerTickEvent.Post} (fiable côté serveur, comme Sky Boots). */
    @SubscribeEvent
    public static void onPlayerTick(PlayerTickEvent.Post event) {
        if (!(event.getEntity() instanceof ServerPlayer player)) {
            return;
        }
        if (player.level().isClientSide()) {
            return;
        }
        tickQuicksand(player);
    }

    /** Mobs et autres entités vivantes (pas les joueurs, déjà gérés ci-dessus). */
    @SubscribeEvent
    public static void onNonPlayerLivingTick(EntityTickEvent.Post event) {
        if (!(event.getEntity() instanceof LivingEntity entity)) {
            return;
        }
        if (entity instanceof Player) {
            return;
        }
        if (entity.level().isClientSide()) {
            return;
        }
        tickQuicksand(entity);
    }

    private static void tickQuicksand(LivingEntity entity) {
        if (entity instanceof Player p && p.getAbilities().flying) {
            return;
        }
        Level level = entity.level();
        boolean body = feetOrBodyInQuicksand(entity);
        boolean eyes = eyesInQuicksand(entity);

        if (body) {
            Vec3 v = entity.getDeltaMovement();
            entity.setDeltaMovement(v.x * 0.92D, v.y - SINK_BOOST, v.z * 0.92D);
        }

        if (eyes) {
            if (entity instanceof Player player) {
                player.setAirSupply(player.getMaxAirSupply());
            }
            if (entity.tickCount % HEAD_DAMAGE_INTERVAL_TICKS == 0) {
                entity.hurt(level.damageSources().source(ModDamageTypes.QUICKSAND), HEAD_DAMAGE_AMOUNT);
            }
        } else if (entity instanceof Player player && body && player.getAirSupply() < player.getMaxAirSupply()) {
            player.setAirSupply(Math.min(player.getMaxAirSupply(), player.getAirSupply() + 4));
        }
    }

    private static boolean eyesInQuicksand(LivingEntity entity) {
        BlockPos eyePos = BlockPos.containing(entity.getX(), entity.getEyeY(), entity.getZ());
        return QuicksandBlock.isQuicksand(entity.level().getBlockState(eyePos));
    }

    private static boolean feetOrBodyInQuicksand(LivingEntity entity) {
        Level level = entity.level();
        BlockPos bp = entity.blockPosition();
        BlockState here = level.getBlockState(bp);
        if (QuicksandBlock.isQuicksand(here)) {
            return true;
        }
        /*
         * Debout sur un bloc plein : les pieds sont dans la cellule d’AIR au-dessus du quicksand.
         * getOnPos() / intersection AABB sont peu fiables ; blockPosition().below() pointe vers le sable porteur.
         */
        if (QuicksandBlock.isQuicksand(level.getBlockState(bp.below()))) {
            return true;
        }
        /*
         * Sous les pieds (légèrement sous la bbox) pour bords de bloc / positions flottantes.
         */
        double footY = entity.getBoundingBox().minY + 0.01D;
        BlockPos underStep = BlockPos.containing(entity.getX(), footY - 0.25D, entity.getZ());
        if (QuicksandBlock.isQuicksand(level.getBlockState(underStep))) {
            return true;
        }
        if (QuicksandBlock.isQuicksand(level.getBlockState(underStep.below()))) {
            return true;
        }
        AABB box = entity.getBoundingBox();
        int x0 = Mth.floor(box.minX);
        int y0 = Mth.floor(box.minY);
        int z0 = Mth.floor(box.minZ);
        int x1 = Mth.floor(box.maxX);
        int y1 = Mth.floor(box.maxY);
        int z1 = Mth.floor(box.maxZ);
        BlockPos.MutableBlockPos m = new BlockPos.MutableBlockPos();
        for (int x = x0; x <= x1; x++) {
            for (int y = y0; y <= y1; y++) {
                for (int z = z0; z <= z1; z++) {
                    m.set(x, y, z);
                    BlockState st = level.getBlockState(m);
                    if (QuicksandBlock.isQuicksand(st)) {
                        if (box.intersects(new AABB(m))) {
                            return true;
                        }
                    }
                }
            }
        }
        return false;
    }
}
