package com.soleapixel.bmcmod.entity.ai.clone;

import java.util.EnumSet;

import com.soleapixel.bmcmod.entity.CloneEntity;

import net.minecraft.core.BlockPos;
import net.minecraft.world.entity.ai.goal.Goal;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;

/**
 * Quitte vite lave ou eau où le clone peut se noyer / brûler.
 */
public final class CloneDangerEscapeGoal extends Goal {
    private final CloneEntity clone;

    public CloneDangerEscapeGoal(CloneEntity clone) {
        this.clone = clone;
        this.setFlags(EnumSet.of(Flag.MOVE));
    }

    @Override
    public boolean canUse() {
        BlockPos feet = clone.blockPosition();
        BlockState stateFeet = clone.level().getBlockState(feet);
        BlockState stateHead = clone.level().getBlockState(feet.above());
        boolean lavaFeetOrHead =
                stateFeet.is(Blocks.LAVA)
                        || clone.isInLava()
                        || stateHead.is(Blocks.LAVA)
                        || stateFeet.is(Blocks.FIRE)
                        || clone.isOnFire();
        boolean waterTooLong =
                clone.isInWaterOrBubble() && clone.getAirSupply() < clone.getMaxAirSupply() - 40;
        return lavaFeetOrHead || waterTooLong;
    }

    @Override
    public void start() {
        var owner = clone.getOwnerPlayer();
        if (owner != null && owner.level() == clone.level()) {
            clone.getNavigation().moveTo(owner, 1.6D);
        } else if (clone.getRandom().nextBoolean()) {
            clone.getJumpControl().jump();
        }
    }

    @Override
    public void tick() {
        var owner = clone.getOwnerPlayer();
        if (owner != null && owner.level() == clone.level()) {
            double dy = owner.getEyeY() - clone.getEyeY();
            clone.getNavigation().moveTo(owner, Math.min(1.75D, 1.35D + Math.abs(dy) * 0.05));
            if (clone.getDeltaMovement().y < 0.15 && (clone.level().getFluidState(clone.blockPosition()).isEmpty() || clone.isInWater())) {
                clone.getJumpControl().jump();
            }
        }
    }

    @Override
    public boolean canContinueToUse() {
        return canUse();
    }

    @Override
    public void stop() {
        clone.getNavigation().stop();
    }
}
