package com.soleapixel.bmcmod.gameplay;

import com.soleapixel.bmcmod.BmcMod;
import com.soleapixel.bmcmod.entity.EndGolem;
import com.soleapixel.bmcmod.network.EndStormPackets;
import com.soleapixel.bmcmod.registry.ModEntities;
import com.soleapixel.bmcmod.registry.ModMobEffects;
import com.soleapixel.bmcmod.registry.ModItems;
import com.soleapixel.bmcmod.registry.ModParticles;
import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.core.HolderGetter;
import net.minecraft.core.HolderLookup;
import net.minecraft.core.component.DataComponents;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceKey;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.Tag;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerBossEvent;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.Mth;
import net.minecraft.util.RandomSource;
import net.minecraft.world.BossEvent;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.boss.enderdragon.EnderDragon;
import net.minecraft.world.entity.monster.EnderMan;
import net.minecraft.world.entity.monster.Endermite;
import net.minecraft.world.entity.monster.Evoker;
import net.minecraft.world.entity.monster.Phantom;
import net.minecraft.world.entity.monster.Shulker;
import net.minecraft.world.entity.monster.Vindicator;
import net.minecraft.world.entity.monster.WitherSkeleton;
import net.minecraft.world.entity.monster.Witch;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.DyeColor;
import net.minecraft.world.item.armortrim.ArmorTrim;
import net.minecraft.world.item.armortrim.TrimMaterials;
import net.minecraft.world.item.armortrim.TrimPatterns;
import net.minecraft.world.item.component.CustomData;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.entity.BannerPattern;
import net.minecraft.world.level.block.entity.BannerPatternLayers;
import net.minecraft.world.level.block.entity.ChestBlockEntity;
import net.minecraft.world.phys.Vec3;
import net.minecraft.world.scores.PlayerTeam;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.entity.living.LivingDeathEvent;
import net.neoforged.neoforge.event.entity.player.PlayerEvent;
import net.neoforged.neoforge.event.tick.LevelTickEvent;
import net.neoforged.neoforge.network.PacketDistributor;

import javax.annotation.Nullable;
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

@EventBusSubscriber(modid = BmcMod.MODID)
public final class EndStormManager {
    private static final String TAG_OWNER = "bmcmod:end_storm_owner";
    private static final String PERSIST_KEY = "bmcmod:end_storm_resume_v1";
    private static final String TAG_END_STORM_BANNER = "EndStormBanner";
    private static final String TEAM_END_STORM_GLOW = "bmcmod_end_storm_glow";

    private static final int PREP_DURATION = 20 * 60 * 10;
    private static final int CHARGE_TICKS = 72;
    private static final int INTER_WAVE_TICKS = 52;
    /** Entre chaque salve : réessai quand le plafond « en combat » bloque. */
    private static final int SPAWN_CAP_RETRY_TICKS = 4;

    private static final Map<UUID, PrepState> PREP = new HashMap<>();
    private static final Map<UUID, StormState> ACTIVE = new HashMap<>();

    public static final List<String> DEBUG_SPAWN_KINDS = List.of(
            "enderman", "endermite", "shulker", "phantom", "endling", "blink", "end_golem",
            "wither_knight", "witch", "evoker", "vindicator");

    private EndStormManager() {
    }

    @SubscribeEvent
    public static void onLevelTick(LevelTickEvent.Post event) {
        if (!(event.getLevel() instanceof ServerLevel level) || level.isClientSide()) {
            return;
        }
        long gameTime = level.getGameTime();
        for (ServerPlayer player : level.players()) {
            if (!player.isAlive()) {
                clearPersisted(player);
                PREP.remove(player.getUUID());
                removeActiveStorm(player.getUUID());
                continue;
            }
            tickPreparation(level, player, gameTime);
            tickActive(level, player, gameTime);
        }
    }

    @SubscribeEvent
    public static void onLivingDeath(LivingDeathEvent event) {
        if (event.getEntity() instanceof ServerPlayer player) {
            if (PREP.containsKey(player.getUUID()) || ACTIVE.containsKey(player.getUUID()) || player.hasEffect(ModMobEffects.END_STORM)) {
                player.displayClientMessage(Component.translatable("message.bmcmod.end_storm.failed"), false);
                stopForced(player);
            }
            return;
        }
        if (!(event.getEntity() instanceof Mob mob) || mob.level().isClientSide()) {
            return;
        }
        UUID owner = ownerOf(mob);
        if (owner == null) {
            return;
        }
        StormState st = ACTIVE.get(owner);
        if (st != null) {
            st.aliveInvaders.remove(mob.getUUID());
        }
    }

    @SubscribeEvent
    public static void onPlayerLogout(PlayerEvent.PlayerLoggedOutEvent event) {
        if (event.getEntity() instanceof ServerPlayer sp) {
            writePersisted(sp);
        }
    }

    @SubscribeEvent
    public static void onPlayerLogin(PlayerEvent.PlayerLoggedInEvent event) {
        if (!(event.getEntity() instanceof ServerPlayer sp)) {
            return;
        }
        if (ACTIVE.containsKey(sp.getUUID())) {
            ACTIVE.get(sp.getUUID()).bossEvent.addPlayer(sp);
            sp.getPersistentData().remove(PERSIST_KEY);
            return;
        }
        if (sp.level() instanceof ServerLevel sl) {
            restorePersisted(sp, sl);
        }
    }

    public static boolean startForced(ServerPlayer player, int level) {
        if (EndStormGameRules.isRestrictedToEnd(player.level()) && player.level().dimension() != Level.END) {
            player.displayClientMessage(Component.translatable("message.bmcmod.end_storm.only_end"), true);
            return false;
        }
        int clamped = Mth.clamp(level, 1, 4);
        if (PREP.containsKey(player.getUUID()) || ACTIVE.containsKey(player.getUUID())) {
            return false;
        }
        player.addEffect(new MobEffectInstance(ModMobEffects.END_STORM, PREP_DURATION, clamped - 1, false, true));
        return true;
    }

    public static boolean startImmediate(ServerPlayer player, int level) {
        if (EndStormGameRules.isRestrictedToEnd(player.level()) && player.level().dimension() != Level.END) {
            player.displayClientMessage(Component.translatable("message.bmcmod.end_storm.only_end"), true);
            return false;
        }
        int clamped = Mth.clamp(level, 1, 4);
        PREP.remove(player.getUUID());
        player.removeEffect(ModMobEffects.END_STORM);
        return startInvasion((ServerLevel) player.level(), player, clamped);
    }

    public static boolean stopForced(ServerPlayer player) {
        UUID id = player.getUUID();
        boolean had = PREP.remove(id) != null || removeActiveStorm(id) != null || player.hasEffect(ModMobEffects.END_STORM);
        player.removeEffect(ModMobEffects.END_STORM);
        clearPersisted(player);
        return had;
    }

    public static int getCurrentLevelAround(ServerPlayer player, int range) {
        int best = 0;
        double r2 = (double) range * range;
        for (StormState st : ACTIVE.values()) {
            ServerPlayer owner = player.server.getPlayerList().getPlayer(st.owner);
            if (owner == null || owner.level() != player.level()) {
                continue;
            }
            if (owner.distanceToSqr(player) <= r2) {
                best = Math.max(best, st.level);
            }
        }
        return best;
    }

    public static boolean forceEndCurrentWave(ServerPlayer player) {
        StormState st = ACTIVE.get(player.getUUID());
        if (st == null || st.phase != Phase.WAVE) {
            return false;
        }
        st.pendingSpawns.clear();
        clearInvadersFor((ServerLevel) player.level(), player.getUUID());
        st.aliveInvaders.clear();
        return true;
    }

    public static boolean forceBackOneWave(ServerPlayer player) {
        StormState st = ACTIVE.get(player.getUUID());
        if (st == null || st.wave <= 0) {
            return false;
        }
        st.wave = Math.max(0, st.wave - 1);
        st.phase = Phase.INTER_WAVE;
        st.nextTick = ((ServerLevel) player.level()).getGameTime() + 20L;
        st.pendingSpawns.clear();
        st.aliveInvaders.clear();
        return true;
    }

    public static int getStormWaveCount(int level) {
        return switch (Mth.clamp(level, 1, 4)) {
            case 1 -> 5;
            case 2 -> 6;
            case 3 -> 7;
            default -> 8;
        };
    }

    public static boolean spawnDebugStormMob(ServerLevel level, ServerPlayer player, String kind, int stormLevel, int wave) {
        String k = kind.toLowerCase(Locale.ROOT);
        RandomSource r = level.random;
        return switch (k) {
            case "enderman" -> spawnTagged(level, player, EntityType.ENDERMAN, r, stormLevel, wave);
            case "endermite" -> spawnTagged(level, player, EntityType.ENDERMITE, r, stormLevel, wave);
            case "shulker" -> spawnTagged(level, player, EntityType.SHULKER, r, stormLevel, wave);
            case "phantom" -> spawnTagged(level, player, EntityType.PHANTOM, r, stormLevel, wave);
            case "endling" -> spawnTagged(level, player, ModEntities.ENDLING.get(), r, stormLevel, wave);
            case "blink" -> spawnTagged(level, player, ModEntities.BLINK.get(), r, stormLevel, wave);
            case "end_golem" -> spawnEndGolem(level, player, null, r, stormLevel, wave);
            case "wither_knight" -> spawnWitherKnight(level, player, null, r, stormLevel, wave);
            case "witch" -> spawnRareWitch(level, player, null, r, stormLevel, wave);
            case "evoker" -> spawnRareEvoker(level, player, null, r, stormLevel, wave);
            case "vindicator" -> spawnRareVindicator(level, player, null, r, stormLevel, wave);
            default -> false;
        };
    }

    public static void spawnVictoryRewardChestForCommand(ServerLevel level, ServerPlayer player, int stormLevel) {
        spawnRewardChest(level, player, Mth.clamp(stormLevel, 1, 4));
    }

    public static ItemStack createEndStormBannerStack(HolderLookup.Provider registries) {
        ItemStack banner = new ItemStack(ModItems.END_STORM_BANNER.get());
        banner.set(DataComponents.CUSTOM_NAME, Component.translatable("item.bmcmod.end_storm_banner").withStyle(ChatFormatting.LIGHT_PURPLE));
        CompoundTag marker = new CompoundTag();
        marker.putBoolean(TAG_END_STORM_BANNER, true);
        banner.set(DataComponents.CUSTOM_DATA, CustomData.of(marker));
        HolderGetter<BannerPattern> patternRegistry = registries.lookupOrThrow(Registries.BANNER_PATTERN);
        ResourceKey<BannerPattern> stormKey = ResourceKey.create(Registries.BANNER_PATTERN, BmcMod.loc("end_storm"));
        patternRegistry.get(stormKey).ifPresent(holder -> {
            banner.set(DataComponents.BASE_COLOR, DyeColor.BLACK);
            banner.set(
                    DataComponents.BANNER_PATTERNS,
                    new BannerPatternLayers(List.of(new BannerPatternLayers.Layer(holder, DyeColor.MAGENTA))));
        });
        return banner;
    }

    private static void tickPreparation(ServerLevel level, ServerPlayer player, long gameTime) {
        UUID id = player.getUUID();
        MobEffectInstance effect = player.getEffect(ModMobEffects.END_STORM);
        if (effect == null) {
            PREP.remove(id);
            return;
        }
        if (EndStormGameRules.isRestrictedToEnd(player.level()) && player.level().dimension() != Level.END) {
            PREP.remove(id);
            player.removeEffect(ModMobEffects.END_STORM);
            player.displayClientMessage(Component.translatable("message.bmcmod.end_storm.only_end"), true);
            return;
        }
        PrepState prep = PREP.computeIfAbsent(id, ignored ->
                new PrepState(Mth.clamp(effect.getAmplifier() + 1, 1, 4), gameTime + PREP_DURATION));
        prep.level = Mth.clamp(effect.getAmplifier() + 1, 1, 4);
        if (gameTime >= prep.startAt) {
            PREP.remove(id);
            player.removeEffect(ModMobEffects.END_STORM);
            startInvasion(level, player, prep.level);
        }
    }

    private static boolean startInvasion(ServerLevel level, ServerPlayer player, int levelValue) {
        UUID id = player.getUUID();
        if (ACTIVE.containsKey(id)) {
            return false;
        }
        int totalWaves = getStormWaveCount(levelValue);
        ServerBossEvent boss = new ServerBossEvent(
                Component.translatable("event.bmcmod.end_storm.wave", levelValue, 0, totalWaves),
                BossEvent.BossBarColor.PINK,
                BossEvent.BossBarOverlay.NOTCHED_10);
        boss.addPlayer(player);
        StormState st = new StormState(id, levelValue, totalWaves, level.getGameTime() + CHARGE_TICKS, Phase.CHARGING, boss);
        ACTIVE.put(id, st);
        PacketDistributor.sendToPlayer(player, new EndStormPackets.EndStormPopPayload(levelValue));
        player.displayClientMessage(Component.translatable("message.bmcmod.end_storm.start", levelValue), false);
        level.playSound(null, player.getX(), player.getY(), player.getZ(), SoundEvents.ENDER_DRAGON_GROWL, SoundSource.PLAYERS, 0.55F, 1.25F);
        return true;
    }

    private static void tickActive(ServerLevel level, ServerPlayer player, long gameTime) {
        StormState st = ACTIVE.get(player.getUUID());
        if (st == null) {
            return;
        }
        st.bossEvent.addPlayer(player);
        st.bossEvent.setName(Component.translatable("event.bmcmod.end_storm.wave", st.level, st.wave, st.totalWaves));

        if (st.phase == Phase.CHARGING) {
            float p = Mth.clamp((CHARGE_TICKS - (st.nextTick - gameTime)) / (float) CHARGE_TICKS, 0.0F, 1.0F);
            st.bossEvent.setProgress(p);
            if (gameTime >= st.nextTick) {
                st.wave++;
                spawnWave(level, player, st);
                st.phase = Phase.WAVE;
                st.wavePhaseStartGameTime = gameTime;
                st.nextSpawnGameTime = gameTime + firstWaveSpawnDelayTicks(st.level);
            }
            return;
        }

        if (st.phase == Phase.WAVE) {
            st.aliveInvaders.removeIf(uuid -> {
                Entity e = level.getEntity(uuid);
                return !(e instanceof LivingEntity le) || !le.isAlive();
            });
            // Mini-vagues : ne spawn que tant qu'il reste de la place sous le plafond "en combat".
            int cap = Math.max(1, st.combatCap);
            boolean roomForMore = st.aliveInvaders.size() < cap;
            if (!st.pendingSpawns.isEmpty() && roomForMore && gameTime >= st.nextSpawnGameTime) {
                int room = cap - st.aliveInvaders.size();
                int pulse = Math.min(Math.min(Math.max(1, st.miniWaveSize), room), st.pendingSpawns.size());
                for (int i = 0; i < pulse && !st.pendingSpawns.isEmpty(); i++) {
                    Runnable task = st.pendingSpawns.poll();
                    if (task != null) {
                        task.run();
                    }
                }
                st.nextSpawnGameTime = gameTime + spawnPulseIntervalTicks(st.level, st.wave);
            } else if (!st.pendingSpawns.isEmpty() && !roomForMore) {
                // Le plafond est atteint : réessaie bientôt pour enchaîner dès qu'il y a des morts.
                st.nextSpawnGameTime = Math.min(st.nextSpawnGameTime, gameTime + SPAWN_CAP_RETRY_TICKS);
            }
            if (gameTime - st.wavePhaseStartGameTime > 20L * 150) {
                for (UUID id : st.aliveInvaders) {
                    Entity e = level.getEntity(id);
                    if (e instanceof LivingEntity le) {
                        le.addEffect(new MobEffectInstance(MobEffects.GLOWING, 60, 0, false, false, false));
                    }
                }
            }
            int expected = st.waveExpectedTotal > 0 ? st.waveExpectedTotal : Math.max(1, st.waveSpawnedCount);
            float progress = RaidWaveBossBar.linearRemainingProgress(
                    st.aliveInvaders.size(), st.pendingSpawns.size(), expected);
            st.bossEvent.setProgress(progress);
            if (st.aliveInvaders.isEmpty() && st.pendingSpawns.isEmpty()) {
                if (st.wave >= st.totalWaves) {
                    finishInvasion(level, player, st);
                    clearPersisted(player);
                    removeActiveStorm(player.getUUID());
                } else {
                    st.phase = Phase.INTER_WAVE;
                    st.nextTick = gameTime + INTER_WAVE_TICKS;
                    st.bossEvent.setProgress(0.0F);
                }
            }
            return;
        }

        if (st.phase == Phase.INTER_WAVE && gameTime >= st.nextTick) {
            st.phase = Phase.CHARGING;
            st.nextTick = gameTime + CHARGE_TICKS;
        }
    }

    private static void spawnWave(ServerLevel level, ServerPlayer player, StormState st) {
        st.waveSpawnedCount = 0;
        st.aliveInvaders.clear();
        st.pendingSpawns.clear();
        st.waveExpectedTotal = 0;
        int lvl = st.level;
        int w = st.wave;
        RandomSource r = level.random;

        int enderman = 1 + lvl + w / 3;
        int endermite = 1 + lvl + w / 2;
        int shulker = (w >= 2) ? Math.max(1, lvl + w / 2) : 0;
        int phantom = (w >= 2 && level.isNight()) ? Math.max(1, (lvl + w - 1) / 3) : 0;
        int endling = (w >= 1) ? Math.max(1, (lvl + w) / 2) : 0;
        int blink = (w >= 2) ? Math.max(1, (lvl + w - 1) / 3) : 0;
        int witherKnights = (lvl >= 2 && w >= 3) ? Math.max(0, (lvl + w - 2) / 5) : 0;
        int rareWitches = (lvl >= 3 && w >= 4) ? Math.max(0, (lvl + w - 4) / 8) : 0;
        int rareEvokers = (lvl >= 3 && w >= 4) ? Math.max(0, (lvl + w - 4) / 9) : 0;
        int rareVindicators = (lvl >= 2 && w >= 3) ? Math.max(0, (lvl + w - 3) / 6) : 0;

        float highNerf = (lvl >= 3 && w >= st.totalWaves - 1) ? 0.82F : 0.92F;
        enderman = applyWaveNerf(enderman, highNerf);
        endermite = applyWaveNerf(endermite, highNerf);
        shulker = applyWaveNerf(shulker, highNerf);
        phantom = applyWaveNerf(phantom, highNerf);
        endling = applyWaveNerf(endling, highNerf);
        blink = applyWaveNerf(blink, highNerf);
        witherKnights = applyWaveNerf(witherKnights, highNerf);
        rareWitches = applyWaveNerf(rareWitches, highNerf);
        rareEvokers = applyWaveNerf(rareEvokers, highNerf);
        rareVindicators = applyWaveNerf(rareVindicators, highNerf);

        int planned = enderman + endermite + shulker + phantom + endling + blink
                + witherKnights + rareWitches + rareEvokers + rareVindicators;
        int cap = 14 + lvl * 3 + w;
        if (planned > cap) {
            float scale = cap / (float) planned;
            enderman = Math.max(1, Mth.ceil(enderman * scale));
            endermite = Math.max(1, Mth.ceil(endermite * scale));
            shulker = Math.max(0, Mth.ceil(shulker * scale));
            phantom = Math.max(0, Mth.ceil(phantom * scale));
            endling = Math.max(1, Mth.ceil(endling * scale));
            blink = Math.max(1, Mth.ceil(blink * scale));
            witherKnights = Math.max(0, Mth.ceil(witherKnights * scale));
            rareWitches = Math.max(0, Mth.ceil(rareWitches * scale));
            rareEvokers = Math.max(0, Mth.ceil(rareEvokers * scale));
            rareVindicators = Math.max(0, Mth.ceil(rareVindicators * scale));
        }

        boolean finalWave = w >= st.totalWaves;
        boolean replaceByBossFinal = finalWave && lvl >= 3 && (lvl >= 4 || r.nextFloat() < 0.50F);

        List<Runnable> tasks = new ArrayList<>();
        for (int i = 0; i < enderman; i++) {
            tasks.add(() -> spawnTagged(level, player, EntityType.ENDERMAN, level.random, lvl, w));
        }
        for (int i = 0; i < endermite; i++) {
            tasks.add(() -> spawnTagged(level, player, EntityType.ENDERMITE, level.random, lvl, w));
        }
        for (int i = 0; i < shulker; i++) {
            tasks.add(() -> spawnTagged(level, player, EntityType.SHULKER, level.random, lvl, w));
        }
        for (int i = 0; i < phantom; i++) {
            tasks.add(() -> spawnTagged(level, player, EntityType.PHANTOM, level.random, lvl, w));
        }
        for (int i = 0; i < endling; i++) {
            tasks.add(() -> spawnTagged(level, player, ModEntities.ENDLING.get(), level.random, lvl, w));
        }
        for (int i = 0; i < blink; i++) {
            tasks.add(() -> spawnTagged(level, player, ModEntities.BLINK.get(), level.random, lvl, w));
        }
        for (int i = 0; i < witherKnights; i++) {
            tasks.add(() -> spawnWitherKnight(level, player, st, level.random, lvl, w));
        }
        for (int i = 0; i < rareWitches; i++) {
            tasks.add(() -> spawnRareWitch(level, player, st, level.random, lvl, w));
        }
        for (int i = 0; i < rareEvokers; i++) {
            tasks.add(() -> spawnRareEvoker(level, player, st, level.random, lvl, w));
        }
        for (int i = 0; i < rareVindicators; i++) {
            tasks.add(() -> spawnRareVindicator(level, player, st, level.random, lvl, w));
        }
        if (replaceByBossFinal) {
            tasks.clear();
            tasks.add(() -> spawnEndGolem(level, player, st, level.random, lvl, w));
        }

        for (Runnable t : tasks) {
            st.pendingSpawns.addLast(t);
        }
        st.waveExpectedTotal = st.pendingSpawns.size();
        configureWaveCombatPacing(st, lvl);
        RaidWaveBossBar.applyWaveStyle(st.bossEvent, st.waveExpectedTotal, st.miniWaveSize);

        level.sendParticles(ModParticles.END_STORM.get(), player.getX(), player.getY() + 0.6D, player.getZ(), 35 + lvl * 8, 1.3D, 0.8D, 1.3D, 0.01D);
        level.playSound(null, player.getX(), player.getY(), player.getZ(), SoundEvents.END_PORTAL_SPAWN, SoundSource.PLAYERS, 0.65F, 0.9F + (lvl * 0.08F));
        player.displayClientMessage(Component.translatable("message.bmcmod.end_storm.wave_start", waveName(lvl), w, st.totalWaves), true);
    }

    private static int applyWaveNerf(int base, float factor) {
        if (base <= 0) {
            return 0;
        }
        return Math.max(1, Mth.ceil(base * factor));
    }

    /**
     * Total prévu en file vs créatures simultanées : plusieurs mini-vagues (ex. 20 prévus, max 5 au combat,
     * salves de 5 → plusieurs avalanches sans tout sortir d'un coup).
     */
    private static void configureWaveCombatPacing(StormState st, int lvl) {
        int plannedTotal = st.pendingSpawns.size();
        if (plannedTotal <= 0) {
            st.miniWaveSize = 1;
            st.combatCap = 1;
            return;
        }
        int preferChunk = Mth.clamp(5 + lvl / 2, 5, 8);
        st.miniWaveSize = Math.min(plannedTotal, preferChunk);
        int bonusCap = Mth.clamp(1 + lvl / 2, 1, 4);
        st.combatCap = Math.min(plannedTotal, Math.max(st.miniWaveSize, st.miniWaveSize + bonusCap));
    }

    private static int spawnPulseIntervalTicks(int lvl, int waveIndex) {
        return 8 + lvl * 4 + waveIndex * 2;
    }

    private static long firstWaveSpawnDelayTicks(int lvl) {
        return 8L + (long) lvl * 3L;
    }

    private static String waveName(int level) {
        return switch (Mth.clamp(level, 1, 4)) {
            case 1 -> "End Rift";
            case 2 -> "End Surge";
            default -> "End Storm";
        };
    }

    private static boolean spawnEndGolem(ServerLevel level, ServerPlayer player, @Nullable StormState st, RandomSource r, int lvl, int wave) {
        EndGolem golem = ModEntities.END_GOLEM.get().create(level);
        if (golem == null) {
            return false;
        }
        Vec3 pos = findSpawnPos(level, player, r);
        golem.moveTo(pos.x, pos.y, pos.z, r.nextFloat() * 360.0F, 0.0F);
        golem.setTarget(player);
        golem.getPersistentData().putUUID(TAG_OWNER, player.getUUID());
        golem.setGlowingTag(true);
        applyPurpleGlowingTeam(level, golem);
        if (level.addFreshEntity(golem)) {
            if (st != null) {
                st.aliveInvaders.add(golem.getUUID());
                st.waveSpawnedCount++;
            }
            level.playSound(null, pos.x, pos.y, pos.z, SoundEvents.ENDER_DRAGON_AMBIENT, SoundSource.PLAYERS, 0.5F, 0.7F);
            level.sendParticles(ModParticles.END_STORM.get(), pos.x, pos.y + 1.0D, pos.z, 54, 1.0D, 1.2D, 1.0D, 0.02D);
            return true;
        }
        return false;
    }

    private static boolean spawnTagged(ServerLevel level, ServerPlayer player, EntityType<? extends Mob> type, RandomSource r, int lvl, int wave) {
        Mob mob = type.create(level);
        if (mob == null) {
            return false;
        }
        Vec3 pos = findSpawnPos(level, player, r);
        mob.moveTo(pos.x, pos.y, pos.z, r.nextFloat() * 360.0F, 0.0F);
        mob.setTarget(player);
        mob.getPersistentData().putUUID(TAG_OWNER, player.getUUID());
        mob.setGlowingTag(true);
        applyPurpleGlowingTeam(level, mob);
        if (mob instanceof Phantom ph) {
            ph.setPhantomSize(Mth.clamp((lvl + wave) / 2, 0, 6));
        }
        if (mob instanceof EnderMan enderMan && r.nextFloat() < 0.18F) {
            enderMan.setItemInHand(net.minecraft.world.InteractionHand.MAIN_HAND, new ItemStack(Blocks.END_STONE));
        }
        boolean ok = level.addFreshEntity(mob);
        if (ok) {
            StormState st = ACTIVE.get(player.getUUID());
            if (st != null) {
                st.aliveInvaders.add(mob.getUUID());
                st.waveSpawnedCount++;
            }
        }
        return ok;
    }

    private static boolean spawnWitherKnight(ServerLevel level, ServerPlayer player, @Nullable StormState st, RandomSource r, int lvl, int wave) {
        WitherSkeleton knight = EntityType.WITHER_SKELETON.create(level);
        if (knight == null) {
            return false;
        }
        Vec3 pos = findSpawnPos(level, player, r);
        knight.moveTo(pos.x, pos.y, pos.z, r.nextFloat() * 360.0F, 0.0F);
        knight.setTarget(player);
        knight.getPersistentData().putUUID(TAG_OWNER, player.getUUID());
        knight.setGlowingTag(true);
        applyPurpleGlowingTeam(level, knight);
        gearWitherKnight(knight, level, lvl, wave, r);
        boolean ok = level.addFreshEntity(knight);
        if (ok && st != null) {
            st.aliveInvaders.add(knight.getUUID());
            st.waveSpawnedCount++;
        }
        return ok;
    }

    private static boolean spawnRareWitch(ServerLevel level, ServerPlayer player, @Nullable StormState st, RandomSource r, int lvl, int wave) {
        Witch witch = EntityType.WITCH.create(level);
        if (witch == null) {
            return false;
        }
        Vec3 pos = findSpawnPos(level, player, r);
        witch.moveTo(pos.x, pos.y, pos.z, r.nextFloat() * 360.0F, 0.0F);
        witch.setTarget(player);
        witch.getPersistentData().putUUID(TAG_OWNER, player.getUUID());
        witch.setGlowingTag(true);
        applyPurpleGlowingTeam(level, witch);
        boolean ok = level.addFreshEntity(witch);
        if (ok && st != null) {
            st.aliveInvaders.add(witch.getUUID());
            st.waveSpawnedCount++;
        }
        return ok;
    }

    private static boolean spawnRareEvoker(ServerLevel level, ServerPlayer player, @Nullable StormState st, RandomSource r, int lvl, int wave) {
        Evoker evoker = EntityType.EVOKER.create(level);
        if (evoker == null) {
            return false;
        }
        Vec3 pos = findSpawnPos(level, player, r);
        evoker.moveTo(pos.x, pos.y, pos.z, r.nextFloat() * 360.0F, 0.0F);
        evoker.setTarget(player);
        evoker.getPersistentData().putUUID(TAG_OWNER, player.getUUID());
        evoker.setGlowingTag(true);
        applyPurpleGlowingTeam(level, evoker);
        boolean ok = level.addFreshEntity(evoker);
        if (ok && st != null) {
            st.aliveInvaders.add(evoker.getUUID());
            st.waveSpawnedCount++;
        }
        return ok;
    }

    private static boolean spawnRareVindicator(ServerLevel level, ServerPlayer player, @Nullable StormState st, RandomSource r, int lvl, int wave) {
        Vindicator vindicator = EntityType.VINDICATOR.create(level);
        if (vindicator == null) {
            return false;
        }
        Vec3 pos = findSpawnPos(level, player, r);
        vindicator.moveTo(pos.x, pos.y, pos.z, r.nextFloat() * 360.0F, 0.0F);
        vindicator.setTarget(player);
        vindicator.getPersistentData().putUUID(TAG_OWNER, player.getUUID());
        vindicator.setGlowingTag(true);
        applyPurpleGlowingTeam(level, vindicator);
        vindicator.setItemSlot(EquipmentSlot.MAINHAND, new ItemStack(Items.IRON_AXE));
        if (lvl >= 4 && wave >= 4) {
            vindicator.setItemSlot(EquipmentSlot.HEAD, new ItemStack(Items.DIAMOND_HELMET));
        }
        boolean ok = level.addFreshEntity(vindicator);
        if (ok && st != null) {
            st.aliveInvaders.add(vindicator.getUUID());
            st.waveSpawnedCount++;
        }
        return ok;
    }

    private static void gearWitherKnight(WitherSkeleton knight, ServerLevel level, int lvl, int wave, RandomSource r) {
        knight.setItemSlot(EquipmentSlot.MAINHAND, new ItemStack(r.nextFloat() < 0.55F ? Items.NETHERITE_SWORD : Items.DIAMOND_SWORD));
        knight.setItemSlot(EquipmentSlot.HEAD, new ItemStack(Items.NETHERITE_HELMET));
        knight.setItemSlot(EquipmentSlot.CHEST, new ItemStack(Items.NETHERITE_CHESTPLATE));
        knight.setItemSlot(EquipmentSlot.LEGS, new ItemStack(Items.NETHERITE_LEGGINGS));
        knight.setItemSlot(EquipmentSlot.FEET, new ItemStack(Items.NETHERITE_BOOTS));
        applyNebrithStyleTrim(knight, level);
        if (lvl >= 4 && wave >= 5) {
            knight.addEffect(new MobEffectInstance(MobEffects.DAMAGE_BOOST, 20 * 90, 0, false, true, true));
        }
    }

    private static void applyNebrithStyleTrim(Mob mob, ServerLevel level) {
        var mats = level.registryAccess().lookupOrThrow(Registries.TRIM_MATERIAL);
        var pats = level.registryAccess().lookupOrThrow(Registries.TRIM_PATTERN);
        ArmorTrim trim = new ArmorTrim(
                mats.getOrThrow(TrimMaterials.AMETHYST),
                pats.getOrThrow(TrimPatterns.SPIRE));
        for (EquipmentSlot slot : List.of(EquipmentSlot.HEAD, EquipmentSlot.CHEST, EquipmentSlot.LEGS, EquipmentSlot.FEET)) {
            ItemStack armor = mob.getItemBySlot(slot);
            if (!armor.isEmpty()) {
                armor.set(DataComponents.TRIM, trim);
                mob.setItemSlot(slot, armor);
            }
        }
    }

    private static void applyPurpleGlowingTeam(ServerLevel level, Mob mob) {
        var board = level.getScoreboard();
        PlayerTeam team = board.getPlayerTeam(TEAM_END_STORM_GLOW);
        if (team == null) {
            team = board.addPlayerTeam(TEAM_END_STORM_GLOW);
            team.setColor(ChatFormatting.LIGHT_PURPLE);
        }
        board.addPlayerToTeam(mob.getStringUUID(), team);
    }

    private static Vec3 findSpawnPos(ServerLevel level, ServerPlayer player, RandomSource r) {
        double ang = r.nextDouble() * Math.PI * 2.0D;
        double dist = 4.0D + r.nextDouble() * 6.0D;
        int x = Mth.floor(player.getX() + Math.cos(ang) * dist);
        int z = Mth.floor(player.getZ() + Math.sin(ang) * dist);
        int y = level.getHeight(net.minecraft.world.level.levelgen.Heightmap.Types.MOTION_BLOCKING_NO_LEAVES, x, z) + 1;
        return new Vec3(x + 0.5D, y, z + 0.5D);
    }

    private static void finishInvasion(ServerLevel level, ServerPlayer player, StormState st) {
        level.playSound(null, player.getX(), player.getY(), player.getZ(), SoundEvents.UI_TOAST_CHALLENGE_COMPLETE, SoundSource.PLAYERS, 1.0F, 1.0F);
        level.sendParticles(ModParticles.END_STORM.get(), player.getX(), player.getY() + 0.9D, player.getZ(), 84, 1.6D, 1.0D, 1.6D, 0.02D);
        spawnRewardChest(level, player, st.level);
        clearInvadersFor(level, player.getUUID());
        player.displayClientMessage(Component.translatable("message.bmcmod.end_storm.complete", st.level), false);
    }

    private static void spawnRewardChest(ServerLevel level, ServerPlayer player, int levelValue) {
        BlockPos base = player.blockPosition().offset(2, 0, 2);
        BlockPos top = level.getHeightmapPos(net.minecraft.world.level.levelgen.Heightmap.Types.MOTION_BLOCKING_NO_LEAVES, base);
        if (!level.getBlockState(top).canBeReplaced()) {
            top = top.above();
        }
        level.setBlock(top, Blocks.CHEST.defaultBlockState(), 3);
        if (!(level.getBlockEntity(top) instanceof ChestBlockEntity chest)) {
            return;
        }
        List<ItemStack> rolls = new ArrayList<>();
        rolls.add(new ItemStack(Items.ENDER_PEARL, 6 + level.random.nextInt(8)));
        rolls.add(new ItemStack(Items.CHORUS_FRUIT, 6 + level.random.nextInt(8)));
        rolls.add(new ItemStack(Items.END_STONE, 16 + level.random.nextInt(24)));
        rolls.add(new ItemStack(Items.SHULKER_SHELL, 1 + level.random.nextInt(2 + Math.max(0, levelValue - 2))));
        if (level.random.nextFloat() < 0.65F) {
            rolls.add(new ItemStack(Items.ENDER_EYE, 2 + level.random.nextInt(4)));
        }
        if (level.random.nextFloat() < 0.40F) {
            rolls.add(new ItemStack(Items.PURPUR_BLOCK, 8 + level.random.nextInt(16)));
        }
        if (level.random.nextFloat() < 0.45F) {
            rolls.add(new ItemStack(Items.EXPERIENCE_BOTTLE, 6 + level.random.nextInt(8 + levelValue * 2)));
        }
        if (level.random.nextFloat() < 0.75F) {
            rolls.add(new ItemStack(ModItems.END_STORM_BOTTLE_1.get()));
        }
        if (levelValue >= 2 && level.random.nextFloat() < 0.60F) {
            rolls.add(new ItemStack(ModItems.END_STORM_BOTTLE_2.get()));
        }
        if (levelValue >= 3 && level.random.nextFloat() < 0.35F) {
            rolls.add(new ItemStack(ModItems.END_STORM_BOTTLE_3.get()));
        }
        if (levelValue >= 4 && level.random.nextFloat() < 0.20F) {
            rolls.add(new ItemStack(ModItems.END_STORM_BOTTLE_4.get()));
        }
        if (levelValue >= 2 && level.random.nextFloat() < 0.45F) {
            rolls.add(new ItemStack(Items.DIAMOND, 1 + level.random.nextInt(2)));
        }
        if (levelValue >= 3 && level.random.nextFloat() < 0.38F) {
            rolls.add(new ItemStack(ModItems.ENDERITE_SCRAP.get(), 1 + level.random.nextInt(2)));
        }
        if (levelValue >= 4 && level.random.nextFloat() < 0.22F) {
            rolls.add(new ItemStack(ModItems.ENDERITE_INGOT.get()));
        }
        if (levelValue >= 3 && level.random.nextFloat() < 0.45F) {
            rolls.add(new ItemStack(ModItems.END_GOLEM_HEART.get()));
        }
        if (levelValue >= 4 && level.random.nextFloat() < 0.28F) {
            rolls.add(new ItemStack(Items.ENCHANTED_GOLDEN_APPLE));
        }
        if (level.random.nextFloat() < 0.30F) {
            rolls.add(createEndStormBannerStack(level.registryAccess()));
        }
        for (int i = 0; i < rolls.size() && i < chest.getContainerSize(); i++) {
            chest.setItem(i, rolls.get(i));
        }
        player.displayClientMessage(Component.translatable("message.bmcmod.end_storm.reward_chest", top.getX(), top.getY(), top.getZ()).withStyle(ChatFormatting.LIGHT_PURPLE), false);
    }

    private static void clearInvadersFor(ServerLevel level, UUID owner) {
        for (Mob mob : level.getEntitiesOfClass(Mob.class, playerWideBox())) {
            UUID tagged = ownerOf(mob);
            if (tagged != null && tagged.equals(owner)) {
                mob.discard();
            }
        }
    }

    private static net.minecraft.world.phys.AABB playerWideBox() {
        return new net.minecraft.world.phys.AABB(-3.0E7, -1024.0, -3.0E7, 3.0E7, 2048.0, 3.0E7);
    }

    @Nullable
    private static UUID ownerOf(Mob mob) {
        return mob.getPersistentData().hasUUID(TAG_OWNER) ? mob.getPersistentData().getUUID(TAG_OWNER) : null;
    }

    private static void writePersisted(ServerPlayer sp) {
        StormState st = ACTIVE.get(sp.getUUID());
        if (st == null) {
            clearPersisted(sp);
            return;
        }
        CompoundTag t = new CompoundTag();
        t.putString("dimension", sp.serverLevel().dimension().location().toString());
        t.putInt("level", st.level);
        t.putInt("waves", st.totalWaves);
        t.putInt("wave", st.wave);
        t.putLong("next", st.nextTick);
        t.putLong("next_spawn", st.nextSpawnGameTime);
        t.putInt("phase", st.phase.ordinal());
        t.putInt("combat_cap", st.combatCap);
        t.putInt("mini_wave", st.miniWaveSize);
        t.putInt("expected", st.waveExpectedTotal);
        sp.getPersistentData().put(PERSIST_KEY, t);
    }

    private static void restorePersisted(ServerPlayer sp, ServerLevel sl) {
        if (!sp.getPersistentData().contains(PERSIST_KEY, Tag.TAG_COMPOUND)) {
            return;
        }
        CompoundTag t = sp.getPersistentData().getCompound(PERSIST_KEY);
        if (!sl.dimension().location().toString().equals(t.getString("dimension"))) {
            clearPersisted(sp);
            return;
        }
        int levelValue = Mth.clamp(t.getInt("level"), 1, 4);
        int waves = Mth.clamp(t.getInt("waves"), 1, 16);
        ServerBossEvent boss = new ServerBossEvent(
                Component.translatable("event.bmcmod.end_storm.wave", levelValue, 0, waves),
                BossEvent.BossBarColor.PINK,
                BossEvent.BossBarOverlay.NOTCHED_10);
        boss.addPlayer(sp);
        StormState st = new StormState(sp.getUUID(), levelValue, waves, t.getLong("next"), Phase.CHARGING, boss);
        st.wave = Math.max(0, t.getInt("wave"));
        int phaseOrd = Mth.clamp(t.getInt("phase"), 0, Phase.values().length - 1);
        st.phase = Phase.values()[phaseOrd];
        st.nextSpawnGameTime = t.getLong("next_spawn");
        if (t.contains("combat_cap")) {
            st.combatCap = Mth.clamp(t.getInt("combat_cap"), 1, 256);
        }
        if (t.contains("mini_wave")) {
            st.miniWaveSize = Mth.clamp(t.getInt("mini_wave"), 1, 256);
        }
        if (t.contains("expected")) {
            st.waveExpectedTotal = Mth.clamp(t.getInt("expected"), 0, 4096);
        }
        if (st.waveExpectedTotal > 0) {
            RaidWaveBossBar.applyWaveStyle(st.bossEvent, st.waveExpectedTotal, st.miniWaveSize);
        }
        ACTIVE.put(sp.getUUID(), st);
        clearPersisted(sp);
    }

    private static void clearPersisted(ServerPlayer sp) {
        sp.getPersistentData().remove(PERSIST_KEY);
    }

    /** Removes active storm state and notifies clients to hide the boss bar. */
    @Nullable
    private static StormState removeActiveStorm(UUID owner) {
        StormState removed = ACTIVE.remove(owner);
        if (removed != null) {
            removed.bossEvent.removeAllPlayers();
        }
        return removed;
    }

    private static final class PrepState {
        int level;
        long startAt;

        PrepState(int level, long startAt) {
            this.level = level;
            this.startAt = startAt;
        }
    }

    private enum Phase {
        CHARGING, WAVE, INTER_WAVE
    }

    private static final class StormState {
        final UUID owner;
        final int level;
        final int totalWaves;
        final ServerBossEvent bossEvent;
        final Set<UUID> aliveInvaders = new HashSet<>();
        final ArrayDeque<Runnable> pendingSpawns = new ArrayDeque<>();
        Phase phase;
        int wave;
        long nextTick;
        long wavePhaseStartGameTime;
        long nextSpawnGameTime;
        int waveSpawnedCount;
        int waveExpectedTotal;
        /** Créatures hostiles max à la fois pour cette vague (souvent = taille mini-vague). */
        int combatCap = 6;
        /** Mobs déployés par salve tant qu'il y a de la place sous {@link #combatCap}. */
        int miniWaveSize = 5;

        StormState(UUID owner, int level, int totalWaves, long nextTick, Phase phase, ServerBossEvent bossEvent) {
            this.owner = owner;
            this.level = level;
            this.totalWaves = totalWaves;
            this.nextTick = nextTick;
            this.phase = phase;
            this.bossEvent = bossEvent;
        }
    }
}

