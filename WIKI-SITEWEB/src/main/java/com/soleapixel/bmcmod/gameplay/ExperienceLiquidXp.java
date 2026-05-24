package com.soleapixel.bmcmod.gameplay;

import net.minecraft.core.BlockPos;
import net.minecraft.util.Mth;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;

/**
 * Heuristique d’XP quand un stack est dissous dans le liquide d’expérience (valeur / rareté / durabilité / bloc dur).
 */
public final class ExperienceLiquidXp {
    private ExperienceLiquidXp() {
    }

    public static int valueForDissolving(ItemStack stack, Level level, BlockPos at) {
        if (stack.isEmpty()) {
            return 0;
        }
        int count = stack.getCount();
        int rarityMul = 1 + stack.getRarity().ordinal();
        int durabilityPart = stack.isDamageableItem() ? Mth.ceil(stack.getMaxDamage() / 52.0F) : 0;
        float hardnessPart = 0.0F;
        if (stack.getItem() instanceof BlockItem bi) {
            hardnessPart = bi.getBlock().defaultBlockState().getDestroySpeed(level, at) * count * 0.11F;
        }
        int enchantLines = stack.getEnchantments().size() * 2;
        int raw = Mth.ceil(2 * count * rarityMul + durabilityPart + hardnessPart + enchantLines);
        // Moins d’XP qu’avant (division plus forte + plafond plus bas) pour dissoudre dans le lac.
        return Mth.clamp(Math.max(1, raw / 18), 1, 18);
    }
}
