package com.soleapixel.bmcmod.item;

import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.InteractionResultHolder;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.context.UseOnContext;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;

import com.soleapixel.bmcmod.gameplay.SkeletonVillagerSpawnEggEvents;
import com.soleapixel.bmcmod.registry.ModEntities;

import net.neoforged.neoforge.common.DeferredSpawnEggItem;

public final class SkeletonVillagerSpawnEggItem extends DeferredSpawnEggItem {
    private final boolean soulVariant;

    public SkeletonVillagerSpawnEggItem(boolean soulVariant, int baseColor, int spotsColor, Properties properties) {
        super(ModEntities.SKELETON_VILLAGER, baseColor, spotsColor, properties);
        this.soulVariant = soulVariant;
    }

    @Override
    public InteractionResultHolder<ItemStack> use(Level level, Player player, InteractionHand hand) {
        if (!level.isClientSide()) {
            SkeletonVillagerSpawnEggEvents.beginSpawn(this.soulVariant);
            try {
                return super.use(level, player, hand);
            } finally {
                SkeletonVillagerSpawnEggEvents.endSpawn();
            }
        }
        return super.use(level, player, hand);
    }

    @Override
    public InteractionResult useOn(UseOnContext context) {
        if (!context.getLevel().isClientSide()) {
            SkeletonVillagerSpawnEggEvents.beginSpawn(this.soulVariant);
            try {
                return super.useOn(context);
            } finally {
                SkeletonVillagerSpawnEggEvents.endSpawn();
            }
        }
        return super.useOn(context);
    }
}
