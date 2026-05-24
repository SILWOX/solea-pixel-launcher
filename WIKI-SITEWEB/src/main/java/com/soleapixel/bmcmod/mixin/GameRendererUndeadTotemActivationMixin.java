package com.soleapixel.bmcmod.mixin;

import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.GameRenderer;
import net.minecraft.world.item.ItemStack;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.ModifyVariable;

import com.soleapixel.bmcmod.client.UndeadTotemActivationFx;

/**
 * Substitution de la pile utilisée pour l’HUD d’activation totem sans toucher au bytecode de {@code Player}.
 */
@Mixin(GameRenderer.class)
public class GameRendererUndeadTotemActivationMixin {

    @ModifyVariable(method = "displayItemActivation", at = @At("HEAD"), ordinal = 0, argsOnly = true)
    private ItemStack bmcmod$substituteTotemActivationItem(ItemStack stack) {
        Minecraft mc = Minecraft.getInstance();
        if (mc.player == null) {
            return stack;
        }
        return UndeadTotemActivationFx.swapActivationStackIfUndeadTotem(mc.player, stack);
    }
}
