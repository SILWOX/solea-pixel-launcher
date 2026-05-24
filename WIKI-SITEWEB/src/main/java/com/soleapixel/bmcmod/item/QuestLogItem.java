package com.soleapixel.bmcmod.item;

import java.util.List;

import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.core.component.DataComponents;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.network.chat.Style;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.util.Mth;
import net.minecraft.world.Difficulty;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.MobSpawnType;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.item.component.CustomData;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.levelgen.Heightmap;

import com.soleapixel.bmcmod.quest.QuestDifficulty;
import com.soleapixel.bmcmod.quest.QuestKind;
import com.soleapixel.bmcmod.quest.QuestLogData;
import com.soleapixel.bmcmod.quest.QuestLogData.QuestSubTask;
import com.soleapixel.bmcmod.entity.BountyHunterEntity;
import com.soleapixel.bmcmod.quest.QuestGenerator;
import com.soleapixel.bmcmod.quest.QuestRewards;
import com.soleapixel.bmcmod.registry.ModEntities;
import com.soleapixel.bmcmod.registry.ModItems;

import net.minecraft.world.item.Item.TooltipContext;

public class QuestLogItem extends Item {
    public QuestLogItem(Properties properties) {
        super(properties.stacksTo(1));
    }

    /** Mort du porteur : annule tout contrat Bounty Hunter actif et retire la cible invoquée. */
    public static void cancelBountyContractsOnPlayerDeath(ServerPlayer player) {
        ServerLevel sl = player.serverLevel();
        boolean any = false;
        for (int i = 0; i < player.getInventory().getContainerSize(); i++) {
            ItemStack stack = player.getInventory().getItem(i);
            if (!stack.is(ModItems.QUEST_LOG.get())) {
                continue;
            }
            CustomData cd = stack.get(DataComponents.CUSTOM_DATA);
            if (cd == null) {
                continue;
            }
            CompoundTag tag = cd.copyTag();
            QuestLogData d = QuestLogData.read(tag);
            if (d == null || !d.isBountyContractActive()) {
                continue;
            }
            discardBountyHunter(sl, d);
            any = true;
            clearStackInSlot(player, i, stack);
        }
        if (any) {
            player.displayClientMessage(Component.translatable("quest.bmcmod.bounty_contract_lost"), true);
        }
    }

    private static void discardBountyHunter(ServerLevel sl, QuestLogData d) {
        java.util.UUID id = d.bountyTargetAsUuid();
        if (id != null && d.bountySpawned) {
            if (sl.getEntity(id) instanceof BountyHunterEntity h) {
                h.discard();
            }
        }
    }

    @Override
    public void appendHoverText(ItemStack stack, TooltipContext context, List<Component> tooltip, TooltipFlag flag) {
        CustomData cd = stack.get(DataComponents.CUSTOM_DATA);
        if (cd == null) {
            tooltip.add(Component.translatable("quest.bmcmod.empty").withStyle(ChatFormatting.DARK_GRAY));
            return;
        }
        QuestLogData d = QuestLogData.read(cd.copyTag());
        if (d == null) {
            tooltip.add(Component.translatable("quest.bmcmod.empty").withStyle(ChatFormatting.DARK_GRAY));
            return;
        }

        tooltip.add(Component.translatable(d.difficulty.translationKey())
                .withStyle(Style.EMPTY.withBold(true).withUnderlined(true).withColor(d.difficulty.labelColor())));

        d.ensureTasks();
        for (QuestSubTask t : d.tasks) {
            String targetText = describeTargetForTooltip(d, t);
            tooltip.add(objectiveLine(t, d.started, targetText).withStyle(ChatFormatting.GRAY));
            tooltip.add(Component.translatable("quest.bmcmod.tooltip.task_progress", t.progress, t.goal).withStyle(ChatFormatting.GRAY));
        }

        int totalMin = Math.max(1, Mth.ceil(d.durationTicks / 1200.0D));
        tooltip.add(Component.translatable("quest.bmcmod.tooltip.time_budget_line", totalMin).withStyle(ChatFormatting.GRAY));

        Level lvl = context.level();
        if (d.started && lvl != null) {
            if (d.difficulty == QuestDifficulty.BOUNTY_HUNTER && !d.bountySpawned && d.bountySpawnGameTime > 0) {
                long untilSpawn = d.bountySpawnGameTime - lvl.getGameTime();
                if (untilSpawn > 0) {
                    int sec = (int) (untilSpawn / 20);
                    int min = sec / 60;
                    sec = sec % 60;
                    tooltip.add(
                            Component.translatable("quest.bmcmod.tooltip.bounty_spawn_in", min, sec).withStyle(ChatFormatting.GOLD));
                } else {
                    tooltip.add(Component.translatable("quest.bmcmod.tooltip.bounty_spawn_imminent").withStyle(ChatFormatting.RED));
                }
            }
            long left = d.deadlineTick - lvl.getGameTime();
            if (left > 0) {
                int sec = (int) (left / 20);
                int min = sec / 60;
                sec = sec % 60;
                tooltip.add(Component.translatable("quest.bmcmod.tooltip.time_left_line", min, sec).withStyle(ChatFormatting.AQUA));
            } else {
                tooltip.add(Component.translatable("quest.bmcmod.time_expired").withStyle(ChatFormatting.RED));
            }
        } else {
            tooltip.add(Component.translatable("quest.bmcmod.tooltip.time_starts_line").withStyle(ChatFormatting.DARK_AQUA));
        }

        if (flag.hasShiftDown()) {
            for (QuestSubTask t : d.tasks) {
                tooltip.add(Component.translatable("quest.bmcmod.shift.kind." + t.kind.id()).withStyle(ChatFormatting.DARK_GRAY));
            }
        } else {
            tooltip.add(Component.translatable("quest.bmcmod.shift_hint").withStyle(ChatFormatting.DARK_PURPLE));
        }
    }

    private static MutableComponent objectiveLine(QuestSubTask t, boolean started, String targetText) {
        if (t.kind == QuestKind.TREASURE || t.kind == QuestKind.EXPLORE) {
            if (started) {
                return Component.translatable("quest.bmcmod.tooltip.objective.explore", t.objectiveX, t.objectiveZ);
            }
            return Component.translatable("quest.bmcmod.tooltip.objective.coords_hidden");
        }
        if (t.kind == QuestKind.KILL && QuestGenerator.BOUNTY_ENTITY_ID.equals(t.target) && !started) {
            return Component.translatable("quest.bmcmod.tooltip.objective.bounty_pending");
        }
        return switch (t.kind) {
            case KILL -> Component.translatable("quest.bmcmod.tooltip.objective.kill", targetText);
            case COLLECT -> Component.translatable("quest.bmcmod.tooltip.objective.collect", targetText);
            case MINE -> Component.translatable("quest.bmcmod.tooltip.objective.mine", targetText);
            case VILLAGER_TRADE -> Component.translatable("quest.bmcmod.tooltip.objective.trade");
            case FISH -> Component.translatable("quest.bmcmod.tooltip.objective.fish", targetText);
            case CRAFT -> Component.translatable("quest.bmcmod.tooltip.objective.craft", targetText);
            default -> Component.translatable("quest.bmcmod.tooltip.objective.generic", targetText);
        };
    }

    @Override
    public void inventoryTick(ItemStack stack, Level level, Entity entity, int slotId, boolean selected) {
        super.inventoryTick(stack, level, entity, slotId, selected);
        if (level.isClientSide || !(entity instanceof ServerPlayer player) || !(level instanceof ServerLevel sl)) {
            return;
        }
        CustomData cd = stack.get(DataComponents.CUSTOM_DATA);
        if (cd == null) {
            return;
        }
        CompoundTag tag = cd.copyTag();
        QuestLogData d = QuestLogData.read(tag);
        if (d == null) {
            return;
        }

        if (!d.started) {
            long now = sl.getGameTime();
            d.started = true;
            d.deadlineTick = now + d.durationTicks;
            if (d.difficulty == QuestDifficulty.BOUNTY_HUNTER) {
                int minIdle = 5 * 60 * 20;
                int fightBuffer = 5 * 60 * 20;
                long earliest = now + minIdle;
                long latest = d.deadlineTick - fightBuffer;
                if (latest <= earliest) {
                    latest = earliest + 20 * 20;
                }
                int span = (int) Math.min(latest - earliest, Integer.MAX_VALUE - 2);
                if (span < 1) {
                    span = 1;
                }
                d.bountySpawnGameTime = earliest + sl.random.nextInt(span);
            }
            d.write(tag);
            stack.set(DataComponents.CUSTOM_DATA, CustomData.of(tag));
        }

        if (d.difficulty == QuestDifficulty.BOUNTY_HUNTER
                && d.started
                && !d.bountySpawned
                && d.bountySpawnGameTime > 0
                && sl.getGameTime() >= d.bountySpawnGameTime) {
            if (sl.getDifficulty() == Difficulty.PEACEFUL) {
                player.displayClientMessage(Component.translatable("quest.bmcmod.bounty_peaceful"), true);
                clearStackInSlot(player, slotId, stack);
                return;
            }
            if (spawnBountyHunter(sl, player, d)) {
                d.bountySpawned = true;
                d.syncLegacyFromFirst();
                d.write(tag);
                stack.set(DataComponents.CUSTOM_DATA, CustomData.of(tag));
            }
        }

        if (sl.getGameTime() > d.deadlineTick) {
            player.displayClientMessage(
                    Component.translatable("quest.bmcmod.expired", QuestGenerator.titleComponentForQuest(d)),
                    true);
            clearStackInSlot(player, slotId, stack);
            return;
        }

        d.ensureTasks();
        boolean dirty = syncCollectProgressFromInventory(player, d);
        for (QuestSubTask t : d.tasks) {
            if ((t.kind == QuestKind.TREASURE || t.kind == QuestKind.EXPLORE) && hasReachedObjective(player, t)) {
                if (t.progress < t.goal) {
                    t.progress = t.goal;
                    dirty = true;
                }
            }
        }
        if (dirty) {
            d.syncLegacyFromFirst();
            d.write(tag);
            stack.set(DataComponents.CUSTOM_DATA, CustomData.of(tag));
        }

        CompoundTag freshTag = stack.getOrDefault(DataComponents.CUSTOM_DATA, CustomData.EMPTY).copyTag();
        QuestLogData fresh = QuestLogData.read(freshTag);
        if (fresh != null && fresh.isComplete()) {
            finishQuest(player, slotId, stack, fresh, sl);
        }
    }

    private static boolean syncCollectProgressFromInventory(ServerPlayer player, QuestLogData d) {
        boolean changed = false;
        for (QuestSubTask t : d.tasks) {
            if (t.kind != QuestKind.COLLECT) {
                continue;
            }
            var item = BuiltInRegistries.ITEM.get(t.targetRl());
            if (item == Items.AIR) {
                continue;
            }
            int have = player.getInventory().countItem(item);
            int np = Math.min(t.goal, have);
            if (np != t.progress) {
                t.progress = np;
                changed = true;
            }
        }
        return changed;
    }

    private static void finishQuest(ServerPlayer player, int slotId, ItemStack stack, QuestLogData d, ServerLevel sl) {
        QuestRewards.grant(player, d, sl.random);
        player.displayClientMessage(
                Component.translatable("quest.bmcmod.complete", QuestGenerator.titleComponentForQuest(d)),
                true);
        clearStackInSlot(player, slotId, stack);
    }

    private static void clearStackInSlot(ServerPlayer player, int slotId, ItemStack stack) {
        if (slotId >= 0 && slotId < player.getInventory().getContainerSize()) {
            ItemStack inSlot = player.getInventory().getItem(slotId);
            if (ItemStack.isSameItemSameComponents(inSlot, stack)) {
                player.getInventory().setItem(slotId, ItemStack.EMPTY);
                return;
            }
        }
        stack.setCount(0);
    }

    private static boolean spawnBountyHunter(ServerLevel sl, ServerPlayer player, QuestLogData d) {
        double ang = sl.random.nextDouble() * Math.PI * 2.0D;
        double dist = 28.0D + sl.random.nextDouble() * 14.0D;
        int x = Mth.floor(player.getX() + Math.cos(ang) * dist);
        int z = Mth.floor(player.getZ() + Math.sin(ang) * dist);
        int y = sl.getHeight(Heightmap.Types.MOTION_BLOCKING_NO_LEAVES, x, z);
        BlockPos pos = new BlockPos(x, y, z);
        if (!sl.getWorldBorder().isWithinBounds(pos)) {
            return false;
        }
        BountyHunterEntity hunter = ModEntities.BOUNTY_HUNTER.get().create(sl);
        if (hunter == null) {
            return false;
        }
        hunter.moveTo(x + 0.5D, y, z + 0.5D, sl.random.nextFloat() * 360.0F, 0.0F);
        hunter.setContractOwner(player.getUUID());
        hunter.finalizeSpawn(sl, sl.getCurrentDifficultyAt(pos), MobSpawnType.EVENT, null);
        hunter.setPersistenceRequired();
        sl.addFreshEntityWithPassengers(hunter);
        d.bountyTargetUuid = hunter.getUUID().toString();
        return true;
    }

    private static boolean hasReachedObjective(ServerPlayer player, QuestSubTask t) {
        int dx = player.blockPosition().getX() - t.objectiveX;
        int dz = player.blockPosition().getZ() - t.objectiveZ;
        return dx * dx + dz * dz <= 14 * 14;
    }

    private static String describeTargetForTooltip(QuestLogData d, QuestSubTask t) {
        if ((t.kind == QuestKind.TREASURE || t.kind == QuestKind.EXPLORE) && !d.started) {
            return Component.translatable("quest.bmcmod.coords_placeholder").getString();
        }
        if (t.kind == QuestKind.KILL && QuestGenerator.BOUNTY_ENTITY_ID.equals(t.target)) {
            return Component.translatable("quest.bmcmod.bounty_target_name").getString();
        }
        return QuestGenerator.describeSubtask(t);
    }
}
