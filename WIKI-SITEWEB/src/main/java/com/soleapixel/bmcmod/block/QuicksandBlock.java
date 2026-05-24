package com.soleapixel.bmcmod.block;

import com.soleapixel.bmcmod.registry.ModBlocks;

import net.minecraft.core.BlockPos;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.Shapes;
import net.minecraft.world.phys.shapes.VoxelShape;
import net.minecraft.world.phys.Vec3;

/**
 * Comportement type {@linkplain net.minecraft.world.level.block.PowderSnowBlock neige poudreuse} : pas de collision
 * « solide » sur les entités — elles occupent le volume du bloc et s’enfoncent. Le contour / la sélection reste un
 * cube plein 16³ ; seule la collision est désactivée (comme la poudreuse), sinon un cube plein repousse le joueur et
 * empêche l’aspiration.
 */
public final class QuicksandBlock extends Block {
    /** Même ordre de grandeur que la poudreuse pour {@link Entity#makeStuckInBlock}. */
    private static final Vec3 STICK = new Vec3(0.9D, 1.5D, 0.9D);

    public QuicksandBlock(BlockBehaviour.Properties properties) {
        super(properties);
    }

    @Override
    protected VoxelShape getShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext ctx) {
        return Shapes.block();
    }

    @Override
    protected VoxelShape getCollisionShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext ctx) {
        return Shapes.empty();
    }

    @Override
    public void entityInside(BlockState state, Level level, BlockPos pos, Entity entity) {
        if (!entity.isSpectator() && !entity.isDescending()) {
            entity.makeStuckInBlock(state, STICK);
        }
    }

    @Override
    public void stepOn(Level level, BlockPos pos, BlockState state, Entity entity) {
        if (entity.isSpectator()) {
            return;
        }
        if (entity instanceof Player p && p.getAbilities().flying) {
            return;
        }
        Vec3 v = entity.getDeltaMovement();
        entity.setDeltaMovement(v.x * 0.9D, v.y - 0.14D, v.z * 0.9D);
    }

    public static boolean isQuicksand(BlockState state) {
        return state.is(ModBlocks.QUICKSAND.get());
    }
}
