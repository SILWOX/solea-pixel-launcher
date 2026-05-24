package com.soleapixel.bmcmod.mixin;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.AbstractFurnaceBlockEntity;

import com.soleapixel.bmcmod.block.endstonefurnace.EndstoneFurnaceBlockEntity;

/**
 * Cuisson plus rapide pour {@link EndstoneFurnaceBlockEntity} (l’XP bonus est gérée dans l’entité via Accessor/Invoker).
 */
@Mixin(AbstractFurnaceBlockEntity.class)
public abstract class AbstractFurnaceBlockEntityEndstoneMixin {

    /** ~2× plus rapide (temps de cuisson divisé par 2, minimum 1 tick). */
    @Inject(
            method = "getTotalCookTime(Lnet/minecraft/world/level/Level;Lnet/minecraft/world/level/block/entity/AbstractFurnaceBlockEntity;)I",
            at = @At("RETURN"),
            cancellable = true)
    private static void bmcmod$fasterEndstoneFurnace(
            Level level, AbstractFurnaceBlockEntity furnace, CallbackInfoReturnable<Integer> cir) {
        if (furnace instanceof EndstoneFurnaceBlockEntity) {
            int v = cir.getReturnValue();
            cir.setReturnValue(Math.max(1, v / 2));
        }
    }
}
