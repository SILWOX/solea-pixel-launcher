package com.soleapixel.bmcmod.item;

import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.stats.Stats;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResultHolder;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;

import com.soleapixel.bmcmod.entity.projectile.UnstablePearlProjectile;
import com.soleapixel.bmcmod.registry.ModEntities;
import com.soleapixel.bmcmod.rarity.BmcModRarity;
import com.soleapixel.bmcmod.rarity.RarityStickItem;

public final class UnstablePearlItem extends RarityStickItem {

    public UnstablePearlItem(Properties properties) {
        super(properties, BmcModRarity.EXOTIC);
    }

    @Override
    public InteractionResultHolder<ItemStack> use(Level level, Player player, InteractionHand hand) {
        ItemStack stack = player.getItemInHand(hand);
        level.playSound(
                null,
                player.getX(),
                player.getY(),
                player.getZ(),
                SoundEvents.ENDER_PEARL_THROW,
                SoundSource.NEUTRAL,
                0.55F,
                0.65F / (level.getRandom().nextFloat() * 0.4F + 0.8F));
        if (!level.isClientSide()) {
            UnstablePearlProjectile pearl =
                    new UnstablePearlProjectile(ModEntities.UNSTABLE_PEARL_PROJECTILE.get(), level);
            pearl.setOwner(player);
            pearl.setItem(stack.copyWithCount(1));
            pearl.setPos(player.getX(), player.getEyeY() - 0.1, player.getZ());
            pearl.shootFromRotation(player, player.getXRot(), player.getYRot(), 0.0F, 1.45F, 1.0F);
            level.addFreshEntity(pearl);
        }
        player.awardStat(Stats.ITEM_USED.get(this));
        if (!player.getAbilities().instabuild) {
            stack.shrink(1);
        }
        return InteractionResultHolder.sidedSuccess(stack, level.isClientSide());
    }

    public static Item.Properties defaultProperties() {
        return new Item.Properties().stacksTo(16);
    }
}
