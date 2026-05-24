package com.soleapixel.bmcmod.mixin;

import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.Difficulty;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.monster.Illusioner;
import net.minecraft.world.entity.raid.Raid;

/**
 * Fait parfois apparaître un illusionniste (entité vanilla inutilisée) en plus des pillards d’une vague de raid.
 */
@Mixin(Raid.class)
public abstract class RaidMixin {
    @Shadow
    @Final
    private ServerLevel level;

    @Inject(method = "spawnGroup", at = @At("TAIL"))
    private void bmcmod$spawnBonusIllusioner(BlockPos pos, CallbackInfo ci) {
        Raid raid = (Raid) (Object) this;
        if (this.level.getDifficulty() == Difficulty.PEACEFUL) {
            return;
        }
        int wave = raid.getGroupsSpawned();
        if (wave < 2) {
            return;
        }
        float chance =
                switch (this.level.getDifficulty()) {
                    case EASY -> wave >= 5 ? 0.12F : 0.0F;
                    case NORMAL -> 0.18F;
                    case HARD -> 0.28F;
                    default -> 0.15F;
                };
        if (chance <= 0.0F || this.level.getRandom().nextFloat() > chance) {
            return;
        }
        Illusioner illusioner = new Illusioner(EntityType.ILLUSIONER, this.level);
        illusioner.setPos(pos.getX() + 0.5, pos.getY() + 0.1, pos.getZ() + 0.5);
        illusioner.setYRot(this.level.getRandom().nextFloat() * 360.0F);
        raid.joinRaid(wave, illusioner, pos, false);
    }
}
