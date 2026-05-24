package com.soleapixel.bmcmod.item;

import com.soleapixel.bmcmod.block.PurpleBerryBushBlock;
import com.soleapixel.bmcmod.registry.ModBlocks;

import net.minecraft.core.BlockPos;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.context.UseOnContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.gameevent.GameEvent;

/**
 * Comme les {@link net.minecraft.world.item.Items#SWEET_BERRIES} vanilla : même pile pour manger (nourriture + effet)
 * et pour planter un buisson d’âge 0 sur un sol valide.
 */
public final class PurpleBerryItem extends Item {
    public PurpleBerryItem(Item.Properties properties) {
        super(properties);
    }

    @Override
    public InteractionResult useOn(UseOnContext context) {
        Level level = context.getLevel();
        BlockPos pos = context.getClickedPos().relative(context.getClickedFace());
        BlockState placeState = level.getBlockState(pos);
        BlockState below = level.getBlockState(pos.below());
        if (placeState.isAir() && PurpleBerryBushBlock.mayPlantOn(below)) {
            if (!level.isClientSide()) {
                BlockState bush = ModBlocks.PURPLE_BERRY_BUSH.get().defaultBlockState().setValue(PurpleBerryBushBlock.AGE, 0);
                level.setBlock(pos, bush, 3);
                level.playSound(null, pos, SoundEvents.HOE_TILL, SoundSource.BLOCKS, 0.75F, 1.1F);
                level.gameEvent(context.getPlayer(), GameEvent.BLOCK_PLACE, pos);
            }
            context.getItemInHand().shrink(1);
            return InteractionResult.sidedSuccess(level.isClientSide());
        }
        return super.useOn(context);
    }
}
