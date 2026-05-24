package com.soleapixel.bmcmod.item;

import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.stats.Stats;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResultHolder;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;

import com.soleapixel.bmcmod.entity.projectile.VoidShardProjectile;
import com.soleapixel.bmcmod.registry.ModEntities;
import com.soleapixel.bmcmod.rarity.BmcModRarity;
import com.soleapixel.bmcmod.rarity.RarityStickItem;

public class VoidShardItem extends RarityStickItem {
    public VoidShardItem(Properties properties) {
        super(properties, BmcModRarity.MYTHIC);
    }

    @Override
    public InteractionResultHolder<ItemStack> use(Level level, Player player, InteractionHand hand) {
        ItemStack stack = player.getItemInHand(hand);
        level.playSound(null, player.getX(), player.getY(), player.getZ(), SoundEvents.SNOWBALL_THROW, SoundSource.NEUTRAL, 0.5F, 0.4F / (level.getRandom().nextFloat() * 0.4F + 0.8F));
        if (!level.isClientSide()) {
            VoidShardProjectile shard = new VoidShardProjectile(ModEntities.VOID_SHARD_PROJECTILE.get(), level);
            shard.setOwner(player);
            shard.setPos(player.getX(), player.getEyeY() - 0.1, player.getZ());
            shard.shootWithSpread(player, player.getXRot(), player.getYRot(), 0.0F, 1.5F, 0.8F);
            level.addFreshEntity(shard);
        }
        player.awardStat(Stats.ITEM_USED.get(this));
        if (!player.getAbilities().instabuild) {
            stack.shrink(1);
        }
        return InteractionResultHolder.sidedSuccess(stack, level.isClientSide());
    }
}
