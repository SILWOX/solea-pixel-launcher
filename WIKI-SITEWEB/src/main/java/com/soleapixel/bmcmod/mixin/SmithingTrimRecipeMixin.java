package com.soleapixel.bmcmod.mixin;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.SmithingRecipeInput;
import net.minecraft.world.item.crafting.SmithingTrimRecipe;
import net.minecraft.world.level.Level;

import com.soleapixel.bmcmod.gameplay.NonTrimmableHeadEquipment;

@Mixin(SmithingTrimRecipe.class)
public class SmithingTrimRecipeMixin {
    @Inject(method = "matches(Lnet/minecraft/world/item/crafting/SmithingRecipeInput;Lnet/minecraft/world/level/Level;)Z", at = @At("HEAD"), cancellable = true)
    private void bmcmod$blockTrimOnCosmeticHelmets(SmithingRecipeInput input, Level level, CallbackInfoReturnable<Boolean> cir) {
        ItemStack base = input.base();
        if (NonTrimmableHeadEquipment.blocksSmithingTrim(base)) {
            cir.setReturnValue(false);
        }
    }
}
