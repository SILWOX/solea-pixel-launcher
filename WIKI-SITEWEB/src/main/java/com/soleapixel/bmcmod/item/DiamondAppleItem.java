package com.soleapixel.bmcmod.item;

import net.minecraft.core.component.DataComponents;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.food.FoodProperties;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Rarity;
import net.minecraft.world.level.Level;

import com.soleapixel.bmcmod.registry.ModMobEffects;

/**
 * Pomme diamant (variante classique / enchantée) : Prisme, résistance, vitesse, régénération.
 */
public final class DiamondAppleItem extends Item {
    private final int prismTicks;
    private final int prismAmp;
    private final int resTicks;
    private final int resAmp;
    private final int spTicks;
    private final int spAmp;
    private final int regenTicks;
    private final int regenAmp;

    public DiamondAppleItem(
            Rarity rarity,
            int prismTicks, int prismAmp,
            int resTicks, int resAmp,
            int spTicks, int spAmp,
            int regenTicks, int regenAmp,
            boolean glint) {
        super(baseProps(rarity, glint));
        this.prismTicks = prismTicks;
        this.prismAmp = prismAmp;
        this.resTicks = resTicks;
        this.resAmp = resAmp;
        this.spTicks = spTicks;
        this.spAmp = spAmp;
        this.regenTicks = regenTicks;
        this.regenAmp = regenAmp;
    }

    private static Item.Properties baseProps(Rarity rarity, boolean glint) {
        Item.Properties p = new Item.Properties().rarity(rarity).food(appleFood());
        if (glint) {
            p = p.component(DataComponents.ENCHANTMENT_GLINT_OVERRIDE, Boolean.TRUE);
        }
        return p;
    }

    private static FoodProperties appleFood() {
        return new FoodProperties.Builder()
                .nutrition(4)
                .saturationModifier(1.2f)
                .alwaysEdible()
                .build();
    }

    @Override
    public ItemStack finishUsingItem(ItemStack stack, Level level, LivingEntity livingEntity) {
        if (!level.isClientSide() && livingEntity instanceof ServerPlayer p) {
            p.addEffect(new MobEffectInstance(ModMobEffects.PRISM, prismTicks, prismAmp, false, true, true));
            p.addEffect(new MobEffectInstance(MobEffects.DAMAGE_RESISTANCE, resTicks, resAmp, false, true, true));
            p.addEffect(new MobEffectInstance(MobEffects.MOVEMENT_SPEED, spTicks, spAmp, false, true, true));
            p.addEffect(new MobEffectInstance(MobEffects.REGENERATION, regenTicks, regenAmp, false, true, true));
        }
        return super.finishUsingItem(stack, level, livingEntity);
    }
}
