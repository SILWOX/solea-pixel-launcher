package com.soleapixel.bmcmod.entity;

import net.minecraft.advancements.CriteriaTriggers;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.CarvedPumpkinBlock;
import net.minecraft.world.level.block.state.pattern.BlockInWorld;
import net.minecraft.world.level.block.state.pattern.BlockPattern;
import net.minecraft.world.level.block.state.pattern.BlockPatternBuilder;
import net.minecraft.world.level.block.state.predicate.BlockStatePredicate;

import com.soleapixel.bmcmod.registry.ModEntities;

public final class DiamondGolemFormation {
    private static BlockPattern diamondGolemFull;

    private DiamondGolemFormation() {
    }

    public static BlockPattern getOrCreateDiamondGolemFull() {
        if (diamondGolemFull == null) {
            diamondGolemFull = BlockPatternBuilder.start()
                    .aisle("~^~", "###", "~#~")
                    .where('^', BlockInWorld.hasState(s -> s.is(Blocks.CARVED_PUMPKIN) || s.is(Blocks.JACK_O_LANTERN)))
                    .where('#', BlockInWorld.hasState(BlockStatePredicate.forBlock(Blocks.DIAMOND_BLOCK)))
                    .where('~', BlockInWorld.hasState(s -> s.isAir()))
                    .build();
        }
        return diamondGolemFull;
    }

    public static void trySpawnDiamondGolem(ServerLevel level, BlockPos pumpkinPos) {
        BlockPattern.BlockPatternMatch match = getOrCreateDiamondGolemFull().find(level, pumpkinPos);
        if (match == null) {
            return;
        }
        DiamondGolem golem = ModEntities.DIAMOND_GOLEM.get().create(level);
        if (golem == null) {
            return;
        }
        golem.setPlayerCreated(true);
        BlockPos spawnPos = match.getBlock(1, 2, 0).getPos();
        CarvedPumpkinBlock.clearPatternBlocks(level, match);
        golem.moveTo(spawnPos.getX() + 0.5D, spawnPos.getY() + 0.05D, spawnPos.getZ() + 0.5D, 0.0F, 0.0F);
        level.addFreshEntity(golem);
        for (ServerPlayer player : level.getEntitiesOfClass(ServerPlayer.class, golem.getBoundingBox().inflate(5.0D))) {
            CriteriaTriggers.SUMMONED_ENTITY.trigger(player, golem);
        }
        CarvedPumpkinBlock.updatePatternBlocks(level, match);
    }

    public static boolean isPumpkinHead(net.minecraft.world.level.block.state.BlockState state) {
        return state.is(Blocks.CARVED_PUMPKIN) || state.is(Blocks.JACK_O_LANTERN);
    }
}
