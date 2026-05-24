package com.soleapixel.bmcmod.gameplay;

import java.util.List;
import java.util.UUID;

import net.minecraft.core.BlockPos;
import net.minecraft.core.component.DataComponents;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.tags.ItemTags;
import net.minecraft.world.entity.animal.Wolf;
import net.minecraft.world.entity.npc.Villager;
import net.minecraft.world.entity.npc.WanderingTrader;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.component.CustomData;
import net.minecraft.world.level.levelgen.Heightmap;
import net.minecraft.world.phys.AABB;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.entity.living.LivingDeathEvent;
import net.neoforged.neoforge.event.entity.living.LivingIncomingDamageEvent;
import net.neoforged.neoforge.event.level.BlockEvent;
import net.neoforged.neoforge.event.tick.ServerTickEvent;
import net.neoforged.neoforge.event.entity.player.ItemFishedEvent;
import net.neoforged.neoforge.event.entity.player.PlayerEvent;
import net.neoforged.neoforge.event.entity.player.TradeWithVillagerEvent;

import com.soleapixel.bmcmod.BmcMod;
import com.soleapixel.bmcmod.ServerGameplayConfig;
import com.soleapixel.bmcmod.entity.QuestTrader;
import com.soleapixel.bmcmod.quest.QuestGenerator;
import com.soleapixel.bmcmod.quest.QuestKind;
import com.soleapixel.bmcmod.quest.QuestLogData;
import com.soleapixel.bmcmod.quest.QuestLogData.QuestSubTask;
import com.soleapixel.bmcmod.item.QuestLogItem;
import com.soleapixel.bmcmod.registry.ModEntities;
import com.soleapixel.bmcmod.registry.ModItems;

@EventBusSubscriber(modid = BmcMod.MODID)
public final class QuestGameplayEvents {
    private QuestGameplayEvents() {
    }

    @SubscribeEvent
    public static void onTraderHurt(LivingIncomingDamageEvent event) {
        if (!(event.getEntity() instanceof QuestTrader qt) || !(event.getEntity().level() instanceof ServerLevel sl)) {
            return;
        }
        if (!(event.getSource().getEntity() instanceof LivingEntity attacker)) {
            return;
        }
        if (attacker instanceof Player p && (p.isCreative() || p.isSpectator())) {
            return;
        }
        for (Wolf w : sl.getEntitiesOfClass(Wolf.class, qt.getBoundingBox().inflate(32.0D))) {
            if (!w.getPersistentData().hasUUID(QuestTrader.QUEST_TRADER_GUARD_TAG)) {
                continue;
            }
            if (qt.getUUID().equals(w.getPersistentData().getUUID(QuestTrader.QUEST_TRADER_GUARD_TAG))) {
                w.setOrderedToSit(false);
                w.setInSittingPose(false);
                w.setTarget(attacker);
            }
        }
    }

    @SubscribeEvent
    public static void onMobDeath(LivingDeathEvent event) {
        if (event.getEntity().level().isClientSide) {
            return;
        }
        if (event.getEntity() instanceof ServerPlayer dead) {
            QuestLogItem.cancelBountyContractsOnPlayerDeath(dead);
        }
        if (!(event.getSource().getEntity() instanceof ServerPlayer player)) {
            return;
        }
        LivingEntity killed = event.getEntity();
        String killedId = BuiltInRegistries.ENTITY_TYPE.getKey(killed.getType()).toString();
        forEachQuestStack(player, stack -> tryProgressKill(stack, killedId, player, killed));
    }

    @SubscribeEvent
    public static void onBlockBreak(BlockEvent.BreakEvent event) {
        if (!(event.getPlayer() instanceof ServerPlayer player) || player.level().isClientSide) {
            return;
        }
        String blockId = BuiltInRegistries.BLOCK.getKey(event.getState().getBlock()).toString();
        forEachQuestStack(player, stack -> tryProgressMine(stack, blockId, player));
    }

    @SubscribeEvent
    public static void onTradeWithVillager(TradeWithVillagerEvent event) {
        if (!(event.getEntity() instanceof ServerPlayer player)) {
            return;
        }
        if (!(event.getAbstractVillager() instanceof Villager)) {
            return;
        }
        forEachQuestStack(player, stack -> tryProgressTrade(stack, player));
    }

    @SubscribeEvent
    public static void onItemFished(ItemFishedEvent event) {
        if (event.isCanceled() || !(event.getEntity() instanceof ServerPlayer player)) {
            return;
        }
        forEachQuestStack(player, stack -> tryProgressFish(stack, event.getDrops(), player));
    }

    @SubscribeEvent
    public static void onItemCrafted(PlayerEvent.ItemCraftedEvent event) {
        if (!(event.getEntity() instanceof ServerPlayer player)) {
            return;
        }
        ItemStack crafted = event.getCrafting();
        if (crafted.isEmpty()) {
            return;
        }
        forEachQuestStack(player, stack -> tryProgressCraft(stack, crafted, player));
    }

    @SubscribeEvent
    public static void onServerTick(ServerTickEvent.Post event) {
        int interval = ServerGameplayConfig.questTraderTickInterval();
        if (interval <= 0 || event.getServer().getTickCount() % interval != 0) {
            return;
        }
        ServerLevel overworld = event.getServer().overworld();
        for (ServerPlayer player : event.getServer().getPlayerList().getPlayers()) {
            if (!(player.level() instanceof ServerLevel sl) || sl != overworld) {
                continue;
            }
            trySpawnQuestTrader(sl, player);
        }
    }

    private static void trySpawnQuestTrader(ServerLevel level, ServerPlayer player) {
        if (!level.getEntitiesOfClass(WanderingTrader.class, player.getBoundingBox().inflate(96.0D), w -> w.isAlive()).isEmpty()) {
            return;
        }
        boolean nearVillage = nearVillage(level, player.blockPosition());
        float spawnChance = nearVillage
                ? ServerGameplayConfig.questTraderSpawnChanceNearVillage()
                : ServerGameplayConfig.questTraderSpawnChanceWild();
        if (level.random.nextFloat() > spawnChance) {
            return;
        }

        double minDist = nearVillage ? 10.0D : 18.0D;
        double maxDist = nearVillage ? 24.0D : 42.0D;
        double ang = level.random.nextDouble() * Math.PI * 2;
        double dist = minDist + level.random.nextDouble() * (maxDist - minDist);
        int x = Mth.floor(player.getX() + Math.cos(ang) * dist);
        int z = Mth.floor(player.getZ() + Math.sin(ang) * dist);
        // MOTION_BLOCKING* = Y du bloc solide le plus haut ; les pieds du mob doivent être au-dessus (air), sinon pathfinding bloque.
        int groundY = level.getHeight(Heightmap.Types.MOTION_BLOCKING_NO_LEAVES, x, z);
        double spawnY = groundY + 1.0D;
        BlockPos pos = BlockPos.containing(x, spawnY, z);
        if (!level.getWorldBorder().isWithinBounds(pos)) {
            return;
        }
        boolean questVariant = level.random.nextFloat() < ServerGameplayConfig.questTraderQuestVariantChance();
        WanderingTrader trader = questVariant
                ? ModEntities.QUEST_TRADER.get().create(level)
                : EntityType.WANDERING_TRADER.create(level);
        if (trader == null) {
            return;
        }
        trader.moveTo(x + 0.5D, spawnY, z + 0.5D, level.random.nextFloat() * 360.0F, 0.0F);
        level.addFreshEntity(trader);
    }

    private static boolean nearVillage(ServerLevel level, BlockPos pos) {
        return level.getEntitiesOfClass(Villager.class, new AABB(pos).inflate(40.0D), v -> true).size() >= 2;
    }

    private static void forEachQuestStack(ServerPlayer player, java.util.function.Consumer<ItemStack> action) {
        for (int i = 0; i < player.getInventory().getContainerSize(); i++) {
            ItemStack s = player.getInventory().getItem(i);
            if (s.is(ModItems.QUEST_LOG.get())) {
                action.accept(s);
            }
        }
    }

    private static void tryProgressKill(ItemStack stack, String entityId, ServerPlayer player, LivingEntity killed) {
        CustomData cd = stack.get(DataComponents.CUSTOM_DATA);
        if (cd == null) {
            return;
        }
        CompoundTag tag = cd.copyTag();
        QuestLogData d = QuestLogData.read(tag);
        if (d == null || !d.started) {
            return;
        }
        if (player.level().getGameTime() > d.deadlineTick) {
            return;
        }
        d.ensureTasks();
        boolean hit = false;
        for (QuestSubTask t : d.tasks) {
            if (t.kind != QuestKind.KILL || !entityId.equals(t.target)) {
                continue;
            }
            if (QuestGenerator.BOUNTY_ENTITY_ID.equals(t.target)) {
                UUID expected = d.bountyTargetAsUuid();
                if (expected == null || !expected.equals(killed.getUUID())) {
                    continue;
                }
            }
            t.progress = Math.min(t.goal, t.progress + 1);
            hit = true;
        }
        if (!hit) {
            return;
        }
        d.syncLegacyFromFirst();
        d.write(tag);
        stack.set(DataComponents.CUSTOM_DATA, CustomData.of(tag));
    }

    private static void tryProgressMine(ItemStack stack, String blockId, ServerPlayer player) {
        CustomData cd = stack.get(DataComponents.CUSTOM_DATA);
        if (cd == null) {
            return;
        }
        CompoundTag tag = cd.copyTag();
        QuestLogData d = QuestLogData.read(tag);
        if (d == null || !d.started) {
            return;
        }
        if (player.level().getGameTime() > d.deadlineTick) {
            return;
        }
        d.ensureTasks();
        boolean hit = false;
        for (QuestSubTask t : d.tasks) {
            if (t.kind == QuestKind.MINE && blockId.equals(t.target)) {
                t.progress = Math.min(t.goal, t.progress + 1);
                hit = true;
            }
        }
        if (!hit) {
            return;
        }
        d.syncLegacyFromFirst();
        d.write(tag);
        stack.set(DataComponents.CUSTOM_DATA, CustomData.of(tag));
    }

    private static void tryProgressTrade(ItemStack stack, ServerPlayer player) {
        CustomData cd = stack.get(DataComponents.CUSTOM_DATA);
        if (cd == null) {
            return;
        }
        CompoundTag tag = cd.copyTag();
        QuestLogData d = QuestLogData.read(tag);
        if (d == null || !d.started) {
            return;
        }
        if (player.level().getGameTime() > d.deadlineTick) {
            return;
        }
        d.ensureTasks();
        boolean hit = false;
        for (QuestSubTask t : d.tasks) {
            if (t.kind == QuestKind.VILLAGER_TRADE && t.progress < t.goal) {
                t.progress = Math.min(t.goal, t.progress + 1);
                hit = true;
                break;
            }
        }
        if (!hit) {
            return;
        }
        d.syncLegacyFromFirst();
        d.write(tag);
        stack.set(DataComponents.CUSTOM_DATA, CustomData.of(tag));
    }

    private static void tryProgressFish(ItemStack stack, List<ItemStack> drops, ServerPlayer player) {
        CustomData cd = stack.get(DataComponents.CUSTOM_DATA);
        if (cd == null) {
            return;
        }
        CompoundTag tag = cd.copyTag();
        QuestLogData d = QuestLogData.read(tag);
        if (d == null || !d.started) {
            return;
        }
        if (player.level().getGameTime() > d.deadlineTick) {
            return;
        }
        d.ensureTasks();
        boolean hit = false;
        for (QuestSubTask t : d.tasks) {
            if (t.kind != QuestKind.FISH || t.progress >= t.goal) {
                continue;
            }
            boolean ok;
            if (QuestGenerator.ANY_FISH_TARGET.equals(t.target)) {
                ok = drops.stream().anyMatch(s -> !s.isEmpty() && s.is(ItemTags.FISHES));
            } else {
                ok = drops.stream().anyMatch(s -> !s.isEmpty() && BuiltInRegistries.ITEM.getKey(s.getItem()).toString().equals(t.target));
            }
            if (ok) {
                t.progress = Math.min(t.goal, t.progress + 1);
                hit = true;
                break;
            }
        }
        if (!hit) {
            return;
        }
        d.syncLegacyFromFirst();
        d.write(tag);
        stack.set(DataComponents.CUSTOM_DATA, CustomData.of(tag));
    }

    private static void tryProgressCraft(ItemStack stack, ItemStack crafted, ServerPlayer player) {
        CustomData cd = stack.get(DataComponents.CUSTOM_DATA);
        if (cd == null) {
            return;
        }
        CompoundTag tag = cd.copyTag();
        QuestLogData d = QuestLogData.read(tag);
        if (d == null || !d.started) {
            return;
        }
        if (player.level().getGameTime() > d.deadlineTick) {
            return;
        }
        String craftedId = BuiltInRegistries.ITEM.getKey(crafted.getItem()).toString();
        d.ensureTasks();
        boolean hit = false;
        for (QuestSubTask t : d.tasks) {
            if (t.kind != QuestKind.CRAFT || !craftedId.equals(t.target)) {
                continue;
            }
            int add = Math.min(crafted.getCount(), t.goal - t.progress);
            if (add <= 0) {
                continue;
            }
            t.progress = Math.min(t.goal, t.progress + add);
            hit = true;
        }
        if (!hit) {
            return;
        }
        d.syncLegacyFromFirst();
        d.write(tag);
        stack.set(DataComponents.CUSTOM_DATA, CustomData.of(tag));
    }
}
