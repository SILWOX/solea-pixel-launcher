package com.soleapixel.bmcmod.quest;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.util.Mth;
import net.minecraft.util.RandomSource;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.entity.ChestBlockEntity;

import com.soleapixel.bmcmod.registry.ModItems;

public final class QuestRewards {
    private QuestRewards() {
    }

    /** Conservé pour compat / debug ; plus utilisé par le tooltip Quest Log. */
    public static MutableComponent rewardSummary(QuestDifficulty diff) {
        return Component.translatable("quest.bmcmod.reward." + diff.name().toLowerCase(Locale.ROOT));
    }

    public static void grant(ServerPlayer player, QuestLogData data, RandomSource random) {
        data.ensureTasks();
        QuestDifficulty diff = data.difficulty;
        QuestKind kind = data.tasks.getFirst().kind;
        int extra = Math.max(0, data.tasks.size() - 1);
        grantWithRollBonus(player, diff, kind, random, extra);
    }

    public static void grant(ServerPlayer player, QuestDifficulty diff, QuestKind kind, RandomSource random) {
        grantWithRollBonus(player, diff, kind, random, 0);
    }

    private static void grantWithRollBonus(ServerPlayer player, QuestDifficulty diff, QuestKind kind, RandomSource random, int extraRolls) {
        Level level = player.level();
        int rolls = rewardRolls(diff, kind, random) + extraRolls;
        rolls = Mth.clamp(
                rolls,
                1,
                diff == QuestDifficulty.SPECIAL && kind == QuestKind.TREASURE
                        ? 8
                        : (diff == QuestDifficulty.BOUNTY_HUNTER ? 8 : 7));
        List<ItemStack> rewards = new ArrayList<>();
        for (int i = 0; i < rolls; i++) {
            ItemStack reward = pickReward(diff, kind, random);
            if (!reward.isEmpty()) {
                rewards.add(reward);
            }
        }
        if (diff == QuestDifficulty.SPECIAL
                && kind == QuestKind.TREASURE
                && level instanceof ServerLevel serverLevel
                && spawnTreasureRewardChest(serverLevel, player, rewards)) {
            return;
        }
        for (ItemStack reward : rewards) {
            if (!player.getInventory().add(reward.copy())) {
                level.addFreshEntity(new ItemEntity(level, player.getX(), player.getY(), player.getZ(), reward.copy()));
            }
        }
    }

    private static int rewardRolls(QuestDifficulty diff, QuestKind kind, RandomSource r) {
        int base = switch (diff) {
            case EASY -> 1 + r.nextInt(2);
            case NORMAL -> 2;
            case HARD -> 2 + r.nextInt(2);
            case EXTREME -> 3 + r.nextInt(2);
            case SPECIAL -> 4 + r.nextInt(2);
            case BOUNTY_HUNTER -> 5 + r.nextInt(3);
        };
        int kindAdj = switch (kind) {
            case TREASURE, EXPLORE -> 1;
            case KILL, MINE -> 1;
            case CRAFT, COLLECT -> 0;
            case FISH, VILLAGER_TRADE -> -1;
        };
        return Mth.clamp(
                base + kindAdj,
                1,
                diff == QuestDifficulty.SPECIAL && kind == QuestKind.TREASURE
                        ? 6
                        : (diff == QuestDifficulty.BOUNTY_HUNTER ? 7 : 5));
    }

    private static int stackMult(QuestDifficulty diff, RandomSource r) {
        return switch (diff) {
            case EASY -> 1;
            case NORMAL -> 1 + r.nextInt(2);
            case HARD -> 2;
            case EXTREME -> 2 + r.nextInt(2);
            case SPECIAL -> 3 + r.nextInt(2);
            case BOUNTY_HUNTER -> 4 + r.nextInt(3);
        };
    }

    private static ItemStack pickReward(QuestDifficulty diff, QuestKind kind, RandomSource r) {
        int mult = stackMult(diff, r);
        float f = r.nextFloat();

        return switch (kind) {
            case MINE -> pickMineFlavor(diff, mult, f, r);
            case FISH -> pickFishFlavor(diff, mult, f, r);
            case KILL -> pickKillFlavor(diff, mult, f, r);
            case COLLECT, CRAFT -> pickGenericFlavor(diff, mult, f, r);
            case VILLAGER_TRADE -> pickTradeFlavor(diff, mult, f, r);
            case EXPLORE, TREASURE -> pickExploreFlavor(diff, mult, f, r);
        };
    }

    private static ItemStack pickMineFlavor(QuestDifficulty diff, int mult, float f, RandomSource r) {
        if (f < 0.35F) {
            return new ItemStack(Items.IRON_INGOT, capStack(mult * 2 + r.nextInt(5), diff));
        }
        if (f < 0.58F) {
            return new ItemStack(Items.COAL, capStack(mult * 3 + r.nextInt(8), diff));
        }
        if (f < 0.78F) {
            return new ItemStack(Items.RAW_IRON, capStack(mult + r.nextInt(4), diff));
        }
        if (f < 0.9F && diff.ordinal() >= QuestDifficulty.HARD.ordinal()) {
            return new ItemStack(Items.DIAMOND, capStack(1 + (diff == QuestDifficulty.EXTREME ? r.nextInt(2) : 0), diff));
        }
        return new ItemStack(Items.GOLD_NUGGET, capStack(3 + mult * 2 + r.nextInt(6), diff));
    }

    private static ItemStack pickFishFlavor(QuestDifficulty diff, int mult, float f, RandomSource r) {
        if (f < 0.4F) {
            return new ItemStack(Items.PRISMARINE_SHARD, capStack(mult + r.nextInt(4), diff));
        }
        if (f < 0.65F) {
            return new ItemStack(Items.EXPERIENCE_BOTTLE, capStack(mult + r.nextInt(4), diff));
        }
        if (f < 0.82F) {
            return new ItemStack(Items.COD, capStack(2 + r.nextInt(4), diff));
        }
        if (f < 0.92F) {
            return new ItemStack(Items.ENDER_PEARL, capStack(1 + (diff.ordinal() >= QuestDifficulty.NORMAL.ordinal() ? r.nextInt(2) : 0), diff));
        }
        return new ItemStack(Items.LAPIS_LAZULI, capStack(mult * 2 + r.nextInt(5), diff));
    }

    private static ItemStack pickKillFlavor(QuestDifficulty diff, int mult, float f, RandomSource r) {
        if (f < 0.3F) {
            return new ItemStack(Items.ROTTEN_FLESH, capStack(4 + mult * 2 + r.nextInt(6), diff));
        }
        if (f < 0.52F) {
            return new ItemStack(Items.BONE, capStack(3 + mult + r.nextInt(5), diff));
        }
        if (f < 0.72F) {
            return new ItemStack(Items.GUNPOWDER, capStack(mult + r.nextInt(4), diff));
        }
        if (f < 0.88F) {
            return new ItemStack(Items.EXPERIENCE_BOTTLE, capStack(mult + r.nextInt(3), diff));
        }
        if (diff.ordinal() >= QuestDifficulty.HARD.ordinal() && f < 0.95F) {
            return new ItemStack(Items.GOLD_INGOT, capStack(1 + r.nextInt(3), diff));
        }
        return new ItemStack(Items.ARROW, capStack(4 + mult * 2 + r.nextInt(8), diff));
    }

    private static ItemStack pickTradeFlavor(QuestDifficulty diff, int mult, float f, RandomSource r) {
        if (f < 0.45F) {
            return new ItemStack(Items.EMERALD, capStack(mult + r.nextInt(4), diff));
        }
        if (f < 0.72F) {
            return new ItemStack(Items.GOLD_INGOT, capStack(mult + r.nextInt(3), diff));
        }
        if (f < 0.88F) {
            return new ItemStack(Items.EXPERIENCE_BOTTLE, capStack(mult + r.nextInt(3), diff));
        }
        if (diff.ordinal() >= QuestDifficulty.HARD.ordinal()) {
            return new ItemStack(ModItems.RUBY.get(), capStack(1 + r.nextInt(Math.min(3, mult)), diff));
        }
        return new ItemStack(Items.BREAD, capStack(2 + r.nextInt(4), diff));
    }

    private static ItemStack pickExploreFlavor(QuestDifficulty diff, int mult, float f, RandomSource r) {
        if (f < 0.38F) {
            return new ItemStack(Items.COMPASS, 1);
        }
        if (f < 0.62F) {
            return new ItemStack(Items.MAP, 1 + r.nextInt(2));
        }
        if (f < 0.82F) {
            return new ItemStack(Items.TORCH, capStack(8 + mult * 4 + r.nextInt(12), diff));
        }
        if (f < 0.92F) {
            return new ItemStack(Items.GOLD_INGOT, capStack(mult + r.nextInt(2), diff));
        }
        return new ItemStack(Items.COOKED_BEEF, capStack(4 + r.nextInt(6), diff));
    }

    private static ItemStack pickGenericFlavor(QuestDifficulty diff, int mult, float f, RandomSource r) {
        if (f < 0.22F) {
            return new ItemStack(Items.EMERALD, capStack(mult + r.nextInt(3), diff));
        }
        if (f < 0.42F) {
            return new ItemStack(Items.GOLD_INGOT, capStack(mult + r.nextInt(3), diff));
        }
        if (f < 0.62F) {
            return new ItemStack(Items.IRON_INGOT, capStack(mult * 2 + r.nextInt(5), diff));
        }
        if (f < 0.78F) {
            return new ItemStack(Items.EXPERIENCE_BOTTLE, capStack(mult + r.nextInt(4), diff));
        }
        if (f < 0.88F && diff.ordinal() >= QuestDifficulty.HARD.ordinal()) {
            return new ItemStack(Items.DIAMOND, capStack(1 + (diff == QuestDifficulty.EXTREME ? r.nextInt(2) : 0), diff));
        }
        if (f < 0.94F && diff.ordinal() >= QuestDifficulty.NORMAL.ordinal()) {
            return new ItemStack(ModItems.RUBY.get(), capStack(1 + r.nextInt(Math.min(3, mult)), diff));
        }
        if (diff == QuestDifficulty.SPECIAL && r.nextFloat() < 0.18F) {
            return new ItemStack(Items.ENCHANTED_GOLDEN_APPLE, 1);
        }
        if (diff == QuestDifficulty.BOUNTY_HUNTER && r.nextFloat() < 0.22F) {
            return new ItemStack(Items.ENCHANTED_GOLDEN_APPLE, 1);
        }
        if (diff == QuestDifficulty.EXTREME && r.nextFloat() < 0.12F) {
            return new ItemStack(Items.GOLDEN_APPLE, 1);
        }
        return new ItemStack(Items.GOLD_NUGGET, capStack(2 + mult * 2 + r.nextInt(6), diff));
    }

    private static int capStack(int count, QuestDifficulty diff) {
        int cap = switch (diff) {
            case EASY -> 24;
            case NORMAL -> 32;
            case HARD -> 40;
            case EXTREME -> 48;
            case SPECIAL -> 56;
            case BOUNTY_HUNTER -> 64;
        };
        return Mth.clamp(count, 1, cap);
    }

    private static boolean spawnTreasureRewardChest(ServerLevel level, ServerPlayer player, List<ItemStack> rewards) {
        var basePos = player.blockPosition().relative(player.getDirection(), 2);
        int y = level.getHeight(net.minecraft.world.level.levelgen.Heightmap.Types.MOTION_BLOCKING_NO_LEAVES, basePos.getX(), basePos.getZ());
        var chestPos = new net.minecraft.core.BlockPos(basePos.getX(), y, basePos.getZ());
        if (!level.getWorldBorder().isWithinBounds(chestPos)) {
            return false;
        }
        var existing = level.getBlockState(chestPos);
        if (!existing.canBeReplaced()) {
            chestPos = chestPos.above();
        }
        if (!level.getBlockState(chestPos).canBeReplaced()) {
            return false;
        }
        level.setBlock(chestPos, Blocks.CHEST.defaultBlockState(), 3);
        if (!(level.getBlockEntity(chestPos) instanceof ChestBlockEntity chest)) {
            return false;
        }
        int slot = 0;
        for (ItemStack reward : rewards) {
            if (slot >= chest.getContainerSize()) {
                level.addFreshEntity(new ItemEntity(level, chestPos.getX() + 0.5, chestPos.getY() + 1.0, chestPos.getZ() + 0.5, reward.copy()));
                continue;
            }
            chest.setItem(slot++, reward.copy());
        }
        chest.setChanged();
        return true;
    }
}
