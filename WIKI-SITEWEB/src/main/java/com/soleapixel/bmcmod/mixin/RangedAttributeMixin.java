package com.soleapixel.bmcmod.mixin;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import net.minecraft.util.Mth;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.ai.attributes.RangedAttribute;

/**
 * Raises player armor attribute cap above vanilla 30.
 */
@Mixin(RangedAttribute.class)
public abstract class RangedAttributeMixin {
    private static final double BMCMOD_ARMOR_MAX = 80.0D;

    @Inject(method = "sanitizeValue", at = @At("HEAD"), cancellable = true)
    private void bmcmod$raiseArmorCap(double value, CallbackInfoReturnable<Double> cir) {
        if ((Object) this == Attributes.ARMOR.value()) {
            cir.setReturnValue(Mth.clamp(value, 0.0D, BMCMOD_ARMOR_MAX));
        }
    }
}
