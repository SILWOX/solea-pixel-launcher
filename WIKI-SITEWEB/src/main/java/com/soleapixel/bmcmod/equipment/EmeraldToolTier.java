package com.soleapixel.bmcmod.equipment;

import net.minecraft.tags.TagKey;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.Tier;
import net.minecraft.world.item.Tiers;
import net.minecraft.world.item.crafting.Ingredient;
import net.minecraft.world.level.block.Block;
import net.minecraft.util.Mth;

/**
 * Tier légèrement au-dessus du diamant (interpolation diamant → Netherite), réparation à l'émeraude.
 * Récolte des blocs au niveau diamant : la pioche émeraude ne suffit pas pour les blocs
 * {@code neoforge:needs_netherite_tool} (ex. débris fossil), qui exigent netherite ou mieux.
 */
public enum EmeraldToolTier implements Tier {
    INSTANCE;

    private static final Tier FROM = Tiers.DIAMOND;
    private static final Tier TO = Tiers.NETHERITE;
    private static final float T = 0.22F;

    @Override
    public int getUses() {
        return Mth.floor(Mth.lerp(T, FROM.getUses(), TO.getUses()));
    }

    @Override
    public float getSpeed() {
        return Mth.lerp(T, FROM.getSpeed(), TO.getSpeed());
    }

    @Override
    public float getAttackDamageBonus() {
        return Mth.lerp(T, FROM.getAttackDamageBonus(), TO.getAttackDamageBonus());
    }

    @Override
    public TagKey<Block> getIncorrectBlocksForDrops() {
        return Tiers.DIAMOND.getIncorrectBlocksForDrops();
    }

    @Override
    public int getEnchantmentValue() {
        return Mth.floor(Mth.lerp(T, FROM.getEnchantmentValue(), TO.getEnchantmentValue()));
    }

    @Override
    public Ingredient getRepairIngredient() {
        return Ingredient.of(Items.EMERALD);
    }
}
