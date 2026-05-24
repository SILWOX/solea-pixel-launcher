package com.soleapixel.bmcmod.entity;

import net.minecraft.world.entity.ai.goal.Goal;

public final class DiamondGolemFollowOwnerGoal extends Goal {
    private final DiamondGolem golem;
    private final double speed;

    public DiamondGolemFollowOwnerGoal(DiamondGolem golem) {
        this.golem = golem;
        this.speed = 1.0D;
    }

    @Override
    public boolean canUse() {
        if (!golem.isCompanion() || golem.getTarget() != null) {
            return false;
        }
        return golem.getOwner().map(o -> golem.distanceToSqr(o) > 144.0).orElse(false);
    }

    @Override
    public boolean canContinueToUse() {
        if (!golem.isCompanion()) {
            return false;
        }
        if (golem.getTarget() != null) {
            return false;
        }
        return golem.getOwner().map(o -> golem.distanceToSqr(o) > 64.0).orElse(false);
    }

    @Override
    public void tick() {
        golem.getOwner().ifPresent(owner -> golem.getNavigation().moveTo(owner, speed));
    }

    @Override
    public void stop() {
        golem.getNavigation().stop();
    }

    @Override
    public void start() {
        golem.getOwner().ifPresent(owner -> golem.getNavigation().moveTo(owner, speed));
    }
}
