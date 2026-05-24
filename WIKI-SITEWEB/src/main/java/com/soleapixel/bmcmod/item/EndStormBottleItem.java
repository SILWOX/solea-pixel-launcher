package com.soleapixel.bmcmod.item;

import com.soleapixel.bmcmod.gameplay.EndStormGameRules;
import com.soleapixel.bmcmod.registry.ModMobEffects;
import net.minecraft.network.chat.Component;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.stats.Stats;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResultHolder;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.UseAnim;
import net.minecraft.world.level.Level;

public final class EndStormBottleItem extends Item {
    private final int level;

    public EndStormBottleItem(Properties properties, int level) {
        super(properties);
        this.level = Math.max(1, Math.min(4, level));
    }

    @Override
    public ItemStack finishUsingItem(ItemStack stack, Level levelObj, LivingEntity entity) {
        ItemStack result = super.finishUsingItem(stack, levelObj, entity);
        if (!levelObj.isClientSide) {
            int amplifier = this.level - 1;
            entity.addEffect(new MobEffectInstance(ModMobEffects.END_STORM, 20 * 60 * 10, amplifier, false, true));
        }
        if (entity instanceof Player player) {
            player.awardStat(Stats.ITEM_USED.get(this));
            if (!player.getAbilities().instabuild) {
                return new ItemStack(Items.GLASS_BOTTLE);
            }
        }
        return result;
    }

    @Override
    public int getUseDuration(ItemStack stack, LivingEntity entity) {
        return 32;
    }

    @Override
    public UseAnim getUseAnimation(ItemStack stack) {
        return UseAnim.DRINK;
    }

    @Override
    public SoundEvent getDrinkingSound() {
        return SoundEvents.GENERIC_DRINK;
    }

    @Override
    public SoundEvent getEatingSound() {
        return SoundEvents.GENERIC_DRINK;
    }

    @Override
    public InteractionResultHolder<ItemStack> use(Level level, Player player, InteractionHand usedHand) {
        if (EndStormGameRules.isRestrictedToEnd(level) && level.dimension() != Level.END) {
            if (!level.isClientSide) {
                player.displayClientMessage(Component.translatable("message.bmcmod.end_storm.only_end"), true);
            }
            return InteractionResultHolder.fail(player.getItemInHand(usedHand));
        }
        return net.minecraft.world.item.ItemUtils.startUsingInstantly(level, player, usedHand);
    }
}

