package com.soleapixel.bmcmod.entity;

import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.goal.Goal;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.phys.Vec3;

/**
 * Ruée courte vers le joueur pour le pousser fort (complète les petits pushs en cercle).
 */
public class EndlingDashGoal extends Goal {
    private static final int DASH_TICKS = 18;
    private static final int COOLDOWN_MIN = 55;
    private static final int COOLDOWN_MAX = 95;

    private final Endling endling;
    private int dashTicks;
    private int cooldown;

    public EndlingDashGoal(Endling endling) {
        this.endling = endling;
    }

    @Override
    public boolean canUse() {
        if (this.cooldown > 0) {
            this.cooldown--;
            return false;
        }
        LivingEntity target = this.endling.getTarget();
        if (!(target instanceof Player player) || !this.endling.getSensing().hasLineOfSight(player)) {
            return false;
        }
        double d = this.endling.distanceTo(player);
        if (d < 2.8 || d > 14.0) {
            return false;
        }
        return this.endling.getRandom().nextInt(32) == 0;
    }

    @Override
    public void start() {
        this.dashTicks = DASH_TICKS;
    }

    @Override
    public boolean canContinueToUse() {
        return this.dashTicks > 0 && this.endling.getTarget() instanceof Player;
    }

    @Override
    public void tick() {
        this.dashTicks--;
        LivingEntity target = this.endling.getTarget();
        if (!(target instanceof Player player)) {
            this.dashTicks = 0;
            return;
        }
        Vec3 self = this.endling.position();
        Vec3 tpos = target.position();
        Vec3 hor = new Vec3(tpos.x - self.x, 0.0, tpos.z - self.z);
        double len = hor.length();
        if (len < 1.0E-4) {
            return;
        }
        Vec3 dir = hor.scale(1.0 / len);
        double speed = 0.72;
        this.endling.setDeltaMovement(dir.x * speed, this.endling.getDeltaMovement().y * 0.6, dir.z * speed);
        this.endling.getLookControl().setLookAt(player, 30.0F, 30.0F);

        if (this.endling.distanceTo(player) < 2.4) {
            Vec3 push = dir.scale(1.35);
            player.push(push.x, 0.42, push.z);
            player.hurtMarked = true;
            if (this.endling.level() instanceof ServerLevel server) {
                EndGolem.onPlayerShovedByEndling(server, player, this.endling, push);
            }
            this.dashTicks = 0;
            this.cooldown = COOLDOWN_MIN + this.endling.getRandom().nextInt(COOLDOWN_MAX - COOLDOWN_MIN);
        }
    }

    @Override
    public void stop() {
        if (this.dashTicks > 0) {
            this.cooldown = COOLDOWN_MIN + this.endling.getRandom().nextInt(25);
        }
    }

    @Override
    public boolean requiresUpdateEveryTick() {
        return true;
    }
}
