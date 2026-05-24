package com.soleapixel.bmcmod.item;

import net.minecraft.core.BlockPos;
import net.minecraft.util.Mth;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.item.context.UseOnContext;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;

import com.soleapixel.bmcmod.entity.Dummy;
import com.soleapixel.bmcmod.registry.ModEntities;

/**
 * Places a Dummy entity in the world.
 */
public class DummyItem extends Item {
    public DummyItem(Properties properties) {
        super(properties);
    }

    @Override
    public InteractionResult useOn(UseOnContext context) {
        Level level = context.getLevel();
        if (context.getClickedFace() == net.minecraft.core.Direction.DOWN) {
            return InteractionResult.FAIL;
        }
        Player player = context.getPlayer();
        ItemStack stack = context.getItemInHand();
        BlockPlaceContext placeContext = new BlockPlaceContext(context);
        BlockPos clickedPos = placeContext.getClickedPos();
        BlockState clickedState = level.getBlockState(clickedPos);
        BlockPos placePos = clickedState.canBeReplaced(placeContext) ? clickedPos : clickedPos.relative(context.getClickedFace());
        Vec3 placeCenter = Vec3.atBottomCenterOf(placePos);
        AABB aabb = ModEntities.DUMMY.get().getDimensions().makeBoundingBox(placeCenter.x(), placeCenter.y(), placeCenter.z());
        if (!level.noCollision(null, aabb) || !level.getEntities(null, aabb).isEmpty()) {
            return InteractionResult.FAIL;
        }
        if (level.isClientSide) {
            return InteractionResult.SUCCESS;
        }
        if (!(level instanceof net.minecraft.server.level.ServerLevel serverLevel)) {
            return InteractionResult.FAIL;
        }
        Dummy dummy = new Dummy(ModEntities.DUMMY.get(), serverLevel);
        float baseYaw = player != null ? player.getYRot() : 0.0F;
        float yaw = Mth.floor((Mth.wrapDegrees(baseYaw - 180.0F) + 22.5F) / 45.0F) * 45.0F;
        dummy.moveTo(placeCenter.x(), placeCenter.y(), placeCenter.z(), yaw, 0.0F);
        dummy.setYRot(yaw);
        dummy.yRotO = yaw;
        dummy.setYHeadRot(yaw);
        dummy.yHeadRotO = yaw;
        dummy.yBodyRot = yaw;
        dummy.yBodyRotO = yaw;
        serverLevel.addFreshEntityWithPassengers(dummy);
        if (player == null || !player.getAbilities().instabuild) {
            stack.shrink(1);
        }
        return InteractionResult.sidedSuccess(level.isClientSide);
    }
}
