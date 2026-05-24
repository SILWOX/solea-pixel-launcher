package com.soleapixel.bmcmod.gameplay;

import com.soleapixel.bmcmod.BmcMod;
import com.soleapixel.bmcmod.ServerGameplayConfig;
import com.soleapixel.bmcmod.network.UndeadInvasionPackets;
import com.soleapixel.bmcmod.registry.ModEntities;
import com.soleapixel.bmcmod.registry.ModMobEffects;
import com.soleapixel.bmcmod.registry.ModItems;
import com.soleapixel.bmcmod.registry.ModParticles;

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
import net.minecraft.util.Mth;
import net.minecraft.util.RandomSource;
import net.minecraft.world.BossEvent;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.decoration.ArmorStand;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.monster.AbstractSkeleton;
import net.minecraft.world.entity.monster.CaveSpider;
import net.minecraft.world.entity.monster.Husk;
import net.minecraft.world.entity.monster.Drowned;
import net.minecraft.world.entity.monster.Phantom;
import net.minecraft.world.entity.monster.Skeleton;
import net.minecraft.world.entity.monster.Witch;
import net.minecraft.world.entity.monster.Zombie;
import net.minecraft.world.entity.monster.ZombieVillager;
import net.minecraft.world.entity.monster.WitherSkeleton;
import net.minecraft.world.entity.monster.Stray;
import net.minecraft.world.entity.monster.Vindicator;
import com.soleapixel.bmcmod.entity.SkeletonVillager;
import net.minecraft.world.entity.animal.horse.SkeletonHorse;
import net.minecraft.world.entity.animal.horse.ZombieHorse;
import net.minecraft.world.item.ArmorItem;
import net.minecraft.world.item.DyeColor;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.armortrim.ArmorTrim;
import net.minecraft.world.item.armortrim.TrimMaterial;
import net.minecraft.world.item.armortrim.TrimMaterials;
import net.minecraft.world.item.armortrim.TrimPattern;
import net.minecraft.world.item.armortrim.TrimPatterns;
import net.minecraft.world.item.component.CustomData;
import net.minecraft.world.level.block.entity.BannerPattern;
import net.minecraft.world.level.block.entity.BannerPatternLayers;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.entity.ChestBlockEntity;
import net.minecraft.world.scores.PlayerTeam;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.ChatFormatting;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.phys.Vec3;
import net.minecraft.world.phys.AABB;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.network.PacketDistributor;
import net.neoforged.neoforge.event.entity.living.LivingDeathEvent;
import net.neoforged.neoforge.event.entity.player.PlayerEvent;
import net.neoforged.neoforge.event.tick.LevelTickEvent;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Random;
import java.util.Set;
import java.util.UUID;

import javax.annotation.Nullable;

@EventBusSubscriber(modid = BmcMod.MODID)
public final class UndeadInvasionManager {
    private static final String TAG_INVADER_OWNER = "bmcmod:undead_invasion_owner";
    /** Reprise après déconnexion / redémarrage du monde (solo ou serveur). */
    private static final String PERSIST_ACTIVE_INVASION_KEY = "bmcmod:undead_invasion_resume_v1";
    private static final AABB LEVEL_WIDE_BOX = new AABB(-3.0E7, -1024.0, -3.0E7, 3.0E7, 2048.0, 3.0E7);
    private static final int PREP_DURATION = 20 * 60 * 10;
    private static final int CHARGE_TICKS = 88;
    private static final int INTER_WAVE_TICKS = 58;
    private static final int MAX_ACTIVE_INVADERS_PER_PLAYER = 80;
    private static final double PLAYER_SCALING_RANGE = 72.0D;
    /** Anneau pour les spawns au sol : distance min + tirage aléatoire (blocs). Réduit = ennemis plus près du joueur. */
    private static final double SPAWN_RING_MIN_BLOCKS = 4.0D;
    private static final double SPAWN_RING_RANDOM_EXTRA = 5.5D;
    /** Fantômes : même logique que l’ancien *22 mais resserré (±7 blocs au lieu de ±11). */
    private static final double PHANTOM_XZ_SPREAD = 14.0D;
    private static final String TAG_REWARD_MARKER = "bmcmod:undead_invasion_reward_marker";
    /** Game time (tick monde) au-delà duquel le porte-armure repère du coffre est retiré. */
    private static final String TAG_REWARD_MARKER_EXPIRE = "bmcmod:undead_invasion_reward_marker_expire";
    private static final int REWARD_MARKER_LIFETIME_TICKS = 20 * 180;
    private static final String TAG_UNDEAD_CROWN_BOSS = "bmcmod:undead_crown_boss";
    private static final String TAG_UNDEAD_SECRET_BOSS = "bmcmod:undead_secret_boss";
    private static final String TEAM_UNDEAD_CROWN_GLOW = "bmcmod_undead_crown_glow";
    private static final String TEAM_UNDEAD_SECRET_GLOW = "bmcmod_undead_secret_glow";
    /** Mob qui porte la bannière d’invasion : doit la lâcher à la mort ({@link Mob#setDropChance}). */
    private static final float UNDEAD_INVASION_BANNER_HEAD_DROP_CHANCE = 1.0F;
    /** Après ce délai (ticks) sans fin de vague : outline spectrale ({@link MobEffects#GLOWING}) pour retrouver les envahisseurs. */
    private static final long UNDEAD_INVASION_WAVE_LONG_HINT_GLOW_AFTER_TICKS = 20L * 60 * 10;
    private static final int UNDEAD_INVASION_WAVE_HINT_GLOW_REFRESH_TICKS = 50;
    /** Comme End Storm : réessai rapide quand le plafond de mobs en combat est atteint. */
    private static final int UNDEAD_SPAWN_CAP_RETRY_TICKS = 4;

    private static final Map<UUID, PrepState> PREP = new HashMap<>();
    private static final Map<UUID, InvasionState> ACTIVE = new HashMap<>();
    private static final Map<UUID, Long> RANDOM_TRIGGER_NEXT = new HashMap<>();

    private UndeadInvasionManager() {
    }

    @SubscribeEvent
    public static void onLevelTick(LevelTickEvent.Post event) {
        if (!(event.getLevel() instanceof ServerLevel level) || level.isClientSide()) {
            return;
        }
        long gameTime = level.getGameTime();

        for (ServerPlayer player : level.players()) {
            if (!player.isAlive()) {
                clearPersistedInvasionResume(player);
                purgeInvasionFromMemory(player.getUUID());
                continue;
            }
            tickRandomThunderTrigger(level, player, gameTime);
            tickPreparation(level, player, gameTime);
            tickActiveInvasion(level, player, gameTime);
        }
    }

    @SubscribeEvent
    public static void onLivingDeath(LivingDeathEvent event) {
        if (event.getEntity() instanceof ServerPlayer player) {
            if (PREP.containsKey(player.getUUID()) || ACTIVE.containsKey(player.getUUID()) || player.hasEffect(ModMobEffects.UNDEAD_INVASION)) {
                if (player.level() instanceof ServerLevel sl) {
                    sl.playSound(null, player.getX(), player.getY(), player.getZ(), SoundEvents.WITHER_DEATH, SoundSource.PLAYERS, 0.7F, 0.8F);
                }
                player.displayClientMessage(Component.translatable("message.bmcmod.undead_invasion.failed"), false);
                stopForced(player);
            }
            return;
        }
        if (!(event.getEntity() instanceof Mob mob) || mob.level().isClientSide()) {
            return;
        }
        UUID owner = getOwnerTag(mob);
        if (owner == null) {
            return;
        }
        InvasionState st = ACTIVE.get(owner);
        if (st != null) {
            if (mob.getPersistentData().getBoolean(TAG_UNDEAD_SECRET_BOSS)) {
                st.secretBossDefeated = true;
            }
            st.aliveInvaders.remove(mob.getUUID());
            st.waveSpawnedCount = Math.max(st.aliveInvaders.size(), st.waveSpawnedCount);
        }
        if (mob.getPersistentData().getBoolean(TAG_UNDEAD_CROWN_BOSS) || mob.getPersistentData().getBoolean(TAG_UNDEAD_SECRET_BOSS)) {
            mob.spawnAtLocation(new ItemStack(ModItems.UNDEAD_TOTEM.get()));
        }
    }

    @SubscribeEvent
    public static void onPlayerLogout(PlayerEvent.PlayerLoggedOutEvent event) {
        if (event.getEntity() instanceof ServerPlayer sp) {
            // Ne pas supprimer les envahisseurs ni l’état en mémoire : reprise multi-session / solo après reload.
            writeInvasionResumeToPlayer(sp);
        }
    }

    @SubscribeEvent
    public static void onPlayerLogin(PlayerEvent.PlayerLoggedInEvent event) {
        if (!(event.getEntity() instanceof ServerPlayer sp)) {
            return;
        }
        if (ACTIVE.containsKey(sp.getUUID())) {
            // Serveur toujours actif : l’invasion est déjà en RAM ; éviter un tag de reprise obsolète.
            sp.getPersistentData().remove(PERSIST_ACTIVE_INVASION_KEY);
            InvasionState st = ACTIVE.get(sp.getUUID());
            st.bossEvent.addPlayer(sp);
            if (st.secretBossEvent != null) {
                st.secretBossEvent.addPlayer(sp);
            }
            return;
        }
        if (sp.level() instanceof ServerLevel sl) {
            tryRestoreInvasionFromPlayer(sp, sl);
        }
    }

    public static ItemStack createUndeadInvasionBannerStack(HolderLookup.Provider registries) {
        ItemStack banner = new ItemStack(ModItems.UNDEAD_INVASION_BANNER.get());
        banner.set(DataComponents.CUSTOM_NAME, Component.translatable("item.bmcmod.undead_invasion_banner").withStyle(net.minecraft.ChatFormatting.GOLD));
        CompoundTag marker = new CompoundTag();
        marker.putBoolean("UndeadInvasionBanner", true);
        banner.set(DataComponents.CUSTOM_DATA, CustomData.of(marker));
        HolderGetter<BannerPattern> patternRegistry = registries.lookupOrThrow(Registries.BANNER_PATTERN);
        ResourceKey<BannerPattern> invasionKey = ResourceKey.create(Registries.BANNER_PATTERN, BmcMod.loc("undead_invasion"));
        patternRegistry.get(invasionKey).ifPresent(holder -> {
            banner.set(DataComponents.BASE_COLOR, DyeColor.BLACK);
            banner.set(
                    DataComponents.BANNER_PATTERNS,
                    new BannerPatternLayers(List.of(new BannerPatternLayers.Layer(holder, DyeColor.WHITE))));
        });
        return banner;
    }

    /** Après {@link #gearMob} si besoin : la tête ne doit pas être réécrasée ensuite. */
    private static void equipInvasionBannerOnHead(Mob mob, HolderLookup.Provider registries) {
        mob.setItemSlot(EquipmentSlot.HEAD, createUndeadInvasionBannerStack(registries));
        mob.setDropChance(EquipmentSlot.HEAD, UNDEAD_INVASION_BANNER_HEAD_DROP_CHANCE);
    }

    private static void tickRandomThunderTrigger(ServerLevel level, ServerPlayer player, long gameTime) {
        if (player.isCreative() || player.isSpectator()) {
            return;
        }
        if (player.hasEffect(ModMobEffects.UNDEAD_INVASION)) {
            return;
        }
        long next = RANDOM_TRIGGER_NEXT.getOrDefault(player.getUUID(), 0L);
        if (gameTime < next) {
            return;
        }
        if (!level.isThundering() || !level.canSeeSky(player.blockPosition())) {
            return;
        }
        if (level.random.nextFloat() >= ServerGameplayConfig.undeadInvasionThunderTriggerChance()) {
            return;
        }
        int amp = level.random.nextInt(3); // level 1..3 only for random trigger
        player.addEffect(new net.minecraft.world.effect.MobEffectInstance(ModMobEffects.UNDEAD_INVASION, PREP_DURATION, amp, false, true));
        RANDOM_TRIGGER_NEXT.put(player.getUUID(), gameTime + ServerGameplayConfig.undeadInvasionRandomTriggerCooldownTicks());
        player.displayClientMessage(Component.translatable("message.bmcmod.undead_invasion.prep_start", amp + 1), true);
    }

    private static void tickPreparation(ServerLevel level, ServerPlayer player, long gameTime) {
        UUID id = player.getUUID();
        var effect = player.getEffect(ModMobEffects.UNDEAD_INVASION);
        if (effect == null) {
            PREP.remove(id);
            return;
        }
        int levelValue = Mth.clamp(effect.getAmplifier() + 1, 1, 6);
        PrepState prep = PREP.computeIfAbsent(id, k -> new PrepState(levelValue, gameTime + effect.getDuration()));
        prep.level = levelValue;
        prep.effectEndTick = gameTime + effect.getDuration();

        if (effect.getDuration() <= 1 && !ACTIVE.containsKey(id)) {
            startInvasion(level, player, levelValue, gameTime, false);
            PREP.remove(id);
        }
    }

    private static void startInvasion(ServerLevel level, ServerPlayer player, int levelValue, long gameTime, boolean forceSecretWave) {
        ServerBossEvent boss = new ServerBossEvent(
                Component.translatable("event.bmcmod.undead_invasion.title", levelValue),
                BossEvent.BossBarColor.YELLOW,
                BossEvent.BossBarOverlay.NOTCHED_10);
        boss.addPlayer(player);
        boss.setProgress(0.0F);
        boolean secretWave = levelValue >= 6 && (forceSecretWave || level.random.nextFloat() < ServerGameplayConfig.undeadInvasionSecretWaveChanceLevel6());
        int waves = totalWaves(levelValue) + (secretWave ? 1 : 0);
        InvasionState state = new InvasionState(levelValue, waves, secretWave, gameTime + CHARGE_TICKS, Phase.CHARGING, boss);
        ACTIVE.put(player.getUUID(), state);
        player.displayClientMessage(Component.translatable("message.bmcmod.undead_invasion.beginning"), true);
        PacketDistributor.sendToPlayer(player, new UndeadInvasionPackets.InvasionPopPayload(levelValue));
        level.playSound(null, player.getX(), player.getY(), player.getZ(), SoundEvents.RAID_HORN, SoundSource.PLAYERS, 1.0F, 0.85F);
        level.playSound(null, player.getX(), player.getY(), player.getZ(), SoundEvents.WITHER_SPAWN, SoundSource.PLAYERS, 0.6F, 1.2F);
        level.sendParticles(ParticleTypes.TOTEM_OF_UNDYING, player.getX(), player.getY() + 1.0D, player.getZ(), 60, 0.7D, 0.9D, 0.7D, 0.02D);
        level.sendParticles(ParticleTypes.SOUL_FIRE_FLAME, player.getX(), player.getY() + 0.2D, player.getZ(), 45, 1.2D, 0.5D, 1.2D, 0.015D);
        if (secretWave) {
            player.displayClientMessage(Component.translatable("message.bmcmod.undead_invasion.secret_wave_hint"), false);
        }
    }

    public static boolean startForced(ServerPlayer player, int levelValue) {
        if (!(player.level() instanceof ServerLevel level)) {
            return false;
        }
        int clamped = Mth.clamp(levelValue, 1, 6);
        player.addEffect(new net.minecraft.world.effect.MobEffectInstance(ModMobEffects.UNDEAD_INVASION, PREP_DURATION, clamped - 1, false, true));
        PREP.put(player.getUUID(), new PrepState(clamped, level.getGameTime() + PREP_DURATION));
        RANDOM_TRIGGER_NEXT.put(player.getUUID(), level.getGameTime() + ServerGameplayConfig.undeadInvasionRandomTriggerCooldownTicks());
        return true;
    }

    public static boolean startImmediate(ServerPlayer player, int levelValue) {
        return startImmediate(player, levelValue, false);
    }

    public static boolean startImmediate(ServerPlayer player, int levelValue, boolean forceSecretWave) {
        if (!(player.level() instanceof ServerLevel level)) {
            return false;
        }
        int clamped = Mth.clamp(levelValue, 1, 6);
        player.removeEffect(ModMobEffects.UNDEAD_INVASION);
        PREP.remove(player.getUUID());
        clearPersistedInvasionResume(player);
        InvasionState old = ACTIVE.remove(player.getUUID());
        if (old != null) {
            old.bossEvent.removeAllPlayers();
            if (old.secretBossEvent != null) {
                old.secretBossEvent.removeAllPlayers();
            }
        }
        startInvasion(level, player, clamped, level.getGameTime(), forceSecretWave && clamped >= 6);
        RANDOM_TRIGGER_NEXT.put(player.getUUID(), level.getGameTime() + ServerGameplayConfig.undeadInvasionRandomTriggerCooldownTicks());
        return true;
    }

    public static boolean stopForced(ServerPlayer player) {
        UUID id = player.getUUID();
        boolean had = PREP.containsKey(id) || ACTIVE.containsKey(id) || player.hasEffect(ModMobEffects.UNDEAD_INVASION);
        player.removeEffect(ModMobEffects.UNDEAD_INVASION);
        clearPersistedInvasionResume(player);
        if (player.level() instanceof ServerLevel level) {
            clearInvadersFor(level, id);
        }
        purgeInvasionFromMemory(id);
        return had;
    }

    public static int getCurrentLevelAround(ServerPlayer player, double range) {
        UUID id = player.getUUID();
        InvasionState self = ACTIVE.get(id);
        if (self != null) {
            return self.level;
        }
        PrepState selfPrep = PREP.get(id);
        if (selfPrep != null) {
            return selfPrep.level;
        }

        double r2 = range * range;
        int best = 0;
        for (Map.Entry<UUID, InvasionState> entry : ACTIVE.entrySet()) {
            ServerPlayer other = player.server.getPlayerList().getPlayer(entry.getKey());
            if (other == null || other.level() != player.level()) {
                continue;
            }
            if (other.distanceToSqr(player) <= r2) {
                best = Math.max(best, entry.getValue().level);
            }
        }
        for (Map.Entry<UUID, PrepState> entry : PREP.entrySet()) {
            ServerPlayer other = player.server.getPlayerList().getPlayer(entry.getKey());
            if (other == null || other.level() != player.level()) {
                continue;
            }
            if (other.distanceToSqr(player) <= r2) {
                best = Math.max(best, entry.getValue().level);
            }
        }
        return best;
    }

    public static boolean forceEndCurrentWave(ServerPlayer player) {
        InvasionState st = ACTIVE.get(player.getUUID());
        if (st == null || st.phase != Phase.WAVE) {
            return false;
        }
        st.aliveInvaders.clear();
        st.waveSpawnedCount = 0;
        st.pendingSpawns.clear();
        return true;
    }

    public static boolean forceBackOneWave(ServerPlayer player) {
        InvasionState st = ACTIVE.get(player.getUUID());
        if (st == null) {
            return false;
        }
        if (player.level() instanceof ServerLevel level) {
            clearInvadersFor(level, player.getUUID());
        }
        st.aliveInvaders.clear();
        st.waveSpawnedCount = 0;
        st.pendingSpawns.clear();
        st.wave = Math.max(0, st.wave - 1);
        st.phase = Phase.CHARGING;
        st.nextTick = player.serverLevel().getGameTime() + 20L;
        st.bossEvent.setProgress(0.0F);
        return true;
    }

    private static void tickActiveInvasion(ServerLevel level, ServerPlayer player, long gameTime) {
        InvasionState state = ACTIVE.get(player.getUUID());
        if (state == null) {
            return;
        }
        state.bossEvent.setName(Component.translatable("event.bmcmod.undead_invasion.wave", state.level, state.wave, state.totalWaves));
        state.bossEvent.addPlayer(player);
        if (state.secretBossEvent != null) {
            state.secretBossEvent.addPlayer(player);
        }

        if (state.phase == Phase.CHARGING) {
            float p = Mth.clamp((CHARGE_TICKS - (state.nextTick - gameTime)) / (float) CHARGE_TICKS, 0.0F, 1.0F);
            state.bossEvent.setProgress(p);
            if (gameTime >= state.nextTick) {
                state.wave++;
                spawnWave(level, player, state);
                state.phase = Phase.WAVE;
                state.wavePhaseStartGameTime = gameTime;
                state.nextSpawnGameTime = gameTime + undeadFirstWaveSpawnDelayTicks(state.level);
            }
            return;
        }

        if (state.phase == Phase.WAVE) {
            state.aliveInvaders.removeIf(uuid -> {
                var e = level.getEntity(uuid);
                return !(e instanceof LivingEntity le) || !le.isAlive();
            });
            int cap = Math.max(1, state.combatCap);
            boolean roomForMore = state.aliveInvaders.size() < cap;
            if (!state.pendingSpawns.isEmpty() && roomForMore && gameTime >= state.nextSpawnGameTime) {
                int room = cap - state.aliveInvaders.size();
                int pulse = Math.min(Math.min(Math.max(1, state.miniWaveSize), room), state.pendingSpawns.size());
                for (int i = 0; i < pulse && !state.pendingSpawns.isEmpty(); i++) {
                    Runnable task = state.pendingSpawns.poll();
                    if (task != null) {
                        task.run();
                    }
                }
                state.nextSpawnGameTime = gameTime + undeadSpawnPulseIntervalTicks(state.level, state.wave);
            } else if (!state.pendingSpawns.isEmpty() && !roomForMore) {
                state.nextSpawnGameTime = Math.min(state.nextSpawnGameTime, gameTime + UNDEAD_SPAWN_CAP_RETRY_TICKS);
            }
            applyLongWaveHintGlow(level, state, gameTime);
            int expected = state.waveExpectedTotal > 0 ? state.waveExpectedTotal : Math.max(1, state.waveSpawnedCount);
            float progress = RaidWaveBossBar.linearRemainingProgress(
                    state.aliveInvaders.size(), state.pendingSpawns.size(), expected);
            state.bossEvent.setProgress(progress);
            tickSecretBossPowers(level, player, state, gameTime);
            if (state.secretBossEvent != null) {
                LivingEntity secretBoss = findSecretBoss(level, state);
                if (secretBoss != null && secretBoss.isAlive()) {
                    state.secretBossEvent.setProgress(Mth.clamp(secretBoss.getHealth() / Math.max(1.0F, secretBoss.getMaxHealth()), 0.0F, 1.0F));
                } else {
                    state.secretBossEvent.removeAllPlayers();
                    state.secretBossEvent = null;
                }
            }
            if (state.aliveInvaders.isEmpty() && state.pendingSpawns.isEmpty()) {
                if (state.wave >= state.totalWaves) {
                    finishInvasion(player, state);
                    clearPersistedInvasionResume(player);
                    purgeInvasionFromMemory(player.getUUID());
                } else {
                    state.phase = Phase.INTER_WAVE;
                    state.nextTick = gameTime + INTER_WAVE_TICKS;
                    state.bossEvent.setProgress(0.0F);
                }
            }
            return;
        }

        if (state.phase == Phase.INTER_WAVE && gameTime >= state.nextTick) {
            state.phase = Phase.CHARGING;
            state.nextTick = gameTime + CHARGE_TICKS;
        }
    }

    /** Vague trop longue : effet Vanilla {@link MobEffects#GLOWING} (visible à travers les murs). */
    private static void applyLongWaveHintGlow(ServerLevel level, InvasionState state, long gameTime) {
        if (state.wavePhaseStartGameTime == 0L) {
            return;
        }
        if (gameTime - state.wavePhaseStartGameTime < UNDEAD_INVASION_WAVE_LONG_HINT_GLOW_AFTER_TICKS) {
            return;
        }
        for (UUID uuid : state.aliveInvaders) {
            Entity entity = level.getEntity(uuid);
            if (entity instanceof LivingEntity living && living.isAlive()) {
                living.addEffect(
                        new MobEffectInstance(MobEffects.GLOWING, UNDEAD_INVASION_WAVE_HINT_GLOW_REFRESH_TICKS, 0, false, false, false));
            }
        }
    }

    private static void finishInvasion(ServerPlayer player, InvasionState state) {
        if (player.level() instanceof ServerLevel level) {
            level.playSound(null, player.getX(), player.getY(), player.getZ(), SoundEvents.UI_TOAST_CHALLENGE_COMPLETE, SoundSource.PLAYERS, 1.0F, 1.0F);
            level.playSound(null, player.getX(), player.getY(), player.getZ(), SoundEvents.PLAYER_LEVELUP, SoundSource.PLAYERS, 0.8F, 1.2F);
            level.sendParticles(ParticleTypes.FIREWORK, player.getX(), player.getY() + 1.0D, player.getZ(), 70, 1.1D, 0.9D, 1.1D, 0.02D);
            spawnRewardChest(level, player, state.level, state.secretBossDefeated);
            clearInvadersFor(level, player.getUUID());
        }
        player.displayClientMessage(Component.translatable("message.bmcmod.undead_invasion.complete", state.level), false);
        if (state.level >= 6) {
            player.addItem(createUndeadInvasionBannerStack(player.level().registryAccess()));
        }
    }

    private static void spawnWave(ServerLevel level, ServerPlayer player, InvasionState state) {
        state.waveSpawnedCount = 0;
        state.aliveInvaders.clear();
        state.pendingSpawns.clear();
        state.waveExpectedTotal = 0;
        int lvl = state.level;
        int w = state.wave;
        RandomSource r = level.random;
        if (state.secretTenthWave && w >= 10) {
            state.pendingSpawns.addLast(() -> spawnUndeadSecretBoss(level, player, state, level.random, lvl, w));
            state.waveExpectedTotal = state.pendingSpawns.size();
            configureUndeadWaveCombatPacing(state, lvl);
            RaidWaveBossBar.applyWaveStyle(state.bossEvent, state.waveExpectedTotal, state.miniWaveSize);
            player.displayClientMessage(Component.translatable("message.bmcmod.undead_invasion.secret_wave_start"), false);
            playWaveStartAmbience(level, player, lvl, w, state.totalWaves);
            return;
        }
        int participants = countParticipants(level, player, PLAYER_SCALING_RANGE);
        float scale = playerScaleMultiplier(participants);

        int skVill = 1 + lvl + w / 4;
        // Au moins 1 variante "âme" par vague pour qu'on les voie régulièrement.
        int soulSkVill = Math.max(1, (lvl + w + 2) / 4);
        int riders = 1 + w / 3;
        int zombieRiders = (w >= 4) ? Math.max(0, (lvl + w) / 4) : 0;
        int zombies = 1 + lvl + (w / 2);
        int zvill = Math.max(0, w / 2);
        int husks = (w >= 5) ? (lvl + w / 2) : 0;
        int witches = (w >= 5) ? Math.max(1, lvl / 2) : 0;
        int strays = (w >= 3) ? Math.max(0, (lvl + w - 1) / 3) : 0;
        int drownedTridents = (w >= 4) ? Math.max(0, (lvl + w - 2) / 5) : 0;
        int caveSpiders = (w >= 4) ? Math.max(0, (lvl + w) / 4) : 0;
        int vindicators = (w >= 5) ? Math.max(0, (lvl + w - 2) / 6) : 0;
        int phantoms = (w >= 6 && level.isNight()) ? Math.max(0, (lvl + w - 3) / 5) : 0;
        int undeadIllagers = 0;

        if (lvl >= 4) {
            skVill += Math.max(1, lvl / 2);
            soulSkVill += Math.max(1, (lvl - 2) / 2);
            riders += Math.max(0, lvl / 3);
            zombieRiders += Math.max(1, lvl / 3);
            zombies += Math.max(1, lvl / 2);
            zvill += Math.max(0, lvl / 3);
            husks += Math.max(0, lvl / 2);
            witches += lvl >= 5 ? 1 : 0;
            strays += Math.max(0, lvl / 3);
            drownedTridents += Math.max(0, (lvl - 3) / 2);
            caveSpiders += Math.max(0, lvl / 3);
            vindicators += Math.max(0, (lvl - 3) / 2);
            phantoms += (lvl >= 6 && level.isNight()) ? 1 : 0;
            // handled below with level-based target range for better consistency
        }

        skVill = scaledCount(skVill, scale);
        soulSkVill = scaledCount(soulSkVill, scale);
        riders = scaledCount(riders, scale);
        zombieRiders = scaledCount(zombieRiders, scale);
        zombies = scaledCount(zombies, scale);
        zvill = scaledCount(zvill, scale);
        husks = scaledCount(husks, scale);
        witches = scaledCount(witches, scale);
        strays = scaledCount(strays, scale);
        drownedTridents = scaledCount(drownedTridents, scale);
        caveSpiders = scaledCount(caveSpiders, scale);
        vindicators = scaledCount(vindicators, scale);
        phantoms = scaledCount(phantoms, scale);
        int undeadIllagerMin;
        int undeadIllagerMax;
        if (lvl <= 2) {
            undeadIllagerMin = 0;
            undeadIllagerMax = 1;
        } else if (lvl <= 4) {
            undeadIllagerMin = 1;
            undeadIllagerMax = 2;
        } else {
            undeadIllagerMin = 1;
            undeadIllagerMax = 2;
        }
        if (w >= Math.max(4, state.totalWaves - 1)) {
            undeadIllagerMax = Math.min(3, undeadIllagerMax + 1);
        }
        undeadIllagers = Mth.nextInt(r, undeadIllagerMin, undeadIllagerMax);
        float presenceScale = Math.max(0.72F, scale * 0.78F);
        undeadIllagers = (int) Mth.floor(undeadIllagers * presenceScale);
        undeadIllagers = Mth.clamp(undeadIllagers, 0, 3);
        // Soft nerf global de densité pour réduire la surcharge de mobs.
        skVill = softWaveCount(skVill);
        soulSkVill = softWaveCount(soulSkVill);
        riders = softWaveCount(riders);
        zombieRiders = softWaveCount(zombieRiders);
        zombies = softWaveCount(zombies);
        zvill = softWaveCount(zvill);
        husks = softWaveCount(husks);
        witches = softWaveCount(witches);
        strays = softWaveCount(strays);
        drownedTridents = softWaveCount(drownedTridents);
        caveSpiders = softWaveCount(caveSpiders);
        vindicators = softWaveCount(vindicators);
        phantoms = softWaveCount(phantoms);
        // Additional nerf on late high-tier waves: fewer burst/special units.
        float highTierNerf = highTierWaveNerf(lvl, w, state.totalWaves);
        riders = applyWaveNerf(riders, highTierNerf);
        zombieRiders = applyWaveNerf(zombieRiders, highTierNerf);
        husks = applyWaveNerf(husks, highTierNerf);
        witches = applyWaveNerf(witches, highTierNerf);
        strays = applyWaveNerf(strays, highTierNerf);
        drownedTridents = applyWaveNerf(drownedTridents, highTierNerf);
        caveSpiders = applyWaveNerf(caveSpiders, highTierNerf);
        vindicators = applyWaveNerf(vindicators, highTierNerf);
        phantoms = applyWaveNerf(phantoms, highTierNerf);
        undeadIllagers = applyWaveNerf(undeadIllagers, Math.min(0.78F, highTierNerf - 0.02F));

        int planned = skVill + soulSkVill + riders + zombieRiders + zombies + zvill + husks + witches
                + strays + drownedTridents + caveSpiders + vindicators + phantoms + undeadIllagers;
        int cap = waveMobCap(lvl, w, state.totalWaves, participants);
        if (planned > cap) {
            float capScale = cap / (float) planned;
            skVill = applyCappedCount(skVill, capScale, 1);
            soulSkVill = applyCappedCount(soulSkVill, capScale, 1);
            riders = applyCappedCount(riders, capScale, 0);
            zombieRiders = applyCappedCount(zombieRiders, capScale, 0);
            zombies = applyCappedCount(zombies, capScale, 1);
            zvill = applyCappedCount(zvill, capScale, 0);
            husks = applyCappedCount(husks, capScale, 0);
            witches = applyCappedCount(witches, capScale, 0);
            strays = applyCappedCount(strays, capScale, 0);
            drownedTridents = applyCappedCount(drownedTridents, capScale, 0);
            caveSpiders = applyCappedCount(caveSpiders, capScale, 0);
            vindicators = applyCappedCount(vindicators, capScale, 0);
            phantoms = applyCappedCount(phantoms, capScale, 0);
            undeadIllagers = applyCappedCount(undeadIllagers, capScale, 1);
        }

        List<Runnable> waveTasks = new ArrayList<>();
        for (int i = 0; i < skVill; i++) {
            waveTasks.add(() -> spawnSkeletonVillager(level, player, state, level.random, lvl, w));
        }
        for (int i = 0; i < soulSkVill; i++) {
            waveTasks.add(() -> spawnSoulSkeletonVillager(level, player, state, level.random, lvl, w));
        }
        for (int i = 0; i < riders; i++) {
            boolean elite = lvl >= 6 && w == state.totalWaves;
            boolean el = elite;
            waveTasks.add(() -> spawnSkeletonRider(level, player, state, level.random, lvl, w, el));
        }
        for (int i = 0; i < zombieRiders; i++) {
            boolean elite = lvl >= 5 && w >= state.totalWaves - 1;
            boolean el = elite;
            waveTasks.add(() -> spawnZombieRider(level, player, state, level.random, lvl, w, el));
        }
        for (int i = 0; i < zombies; i++) {
            waveTasks.add(() -> spawnZombie(level, player, state, level.random, lvl, w, false));
        }
        for (int i = 0; i < zvill; i++) {
            waveTasks.add(() -> spawnZombie(level, player, state, level.random, lvl, w, true));
        }
        for (int i = 0; i < husks; i++) {
            waveTasks.add(() -> spawnHusk(level, player, state, level.random, lvl, w));
        }
        for (int i = 0; i < witches; i++) {
            waveTasks.add(() -> spawnWitch(level, player, state, level.random));
        }
        for (int i = 0; i < strays; i++) {
            waveTasks.add(() -> spawnStrayArcher(level, player, state, level.random, lvl, w));
        }
        for (int i = 0; i < drownedTridents; i++) {
            waveTasks.add(() -> spawnDrownedTrident(level, player, state, level.random, lvl, w));
        }
        for (int i = 0; i < caveSpiders; i++) {
            waveTasks.add(() -> spawnCaveSpider(level, player, state, level.random, lvl, w));
        }
        for (int i = 0; i < vindicators; i++) {
            waveTasks.add(() -> spawnCorruptedVindicator(level, player, state, level.random, lvl, w));
        }
        for (int i = 0; i < phantoms; i++) {
            waveTasks.add(() -> spawnPhantomHarasser(level, player, state, level.random, lvl, w));
        }
        for (int i = 0; i < undeadIllagers; i++) {
            waveTasks.add(() -> spawnUndeadIllager(level, player, state, level.random, lvl, w));
        }
        if (lvl >= 5 && w >= Math.max(4, state.totalWaves - 2) && r.nextFloat() < 0.55F) {
            waveTasks.add(() -> spawnUndeadBrute(level, player, state, level.random, lvl, w));
        }
        if (lvl >= 4 && w >= 4 && level.isNight() && r.nextFloat() < 0.40F) {
            waveTasks.add(() -> spawnBoneCaptain(level, player, state, level.random, lvl, w));
        }

        Collections.shuffle(waveTasks, new Random(r.nextLong()));
        for (Runnable t : waveTasks) {
            state.pendingSpawns.addLast(t);
        }
        if (w >= state.totalWaves) {
            state.pendingSpawns.addLast(() -> spawnUndeadCrownBoss(level, player, state, level.random, lvl, w));
        }
        state.waveExpectedTotal = state.pendingSpawns.size();
        configureUndeadWaveCombatPacing(state, lvl);
        RaidWaveBossBar.applyWaveStyle(state.bossEvent, state.waveExpectedTotal, state.miniWaveSize);

        player.displayClientMessage(Component.translatable("message.bmcmod.undead_invasion.wave_start", w, state.totalWaves), true);
        playWaveStartAmbience(level, player, lvl, w, state.totalWaves);
    }

    private static void spawnUndeadCrownBoss(ServerLevel level, ServerPlayer player, InvasionState state, RandomSource r, int lvl, int wave) {
        Mob boss = switch (r.nextInt(7)) {
            case 0 -> EntityType.WITHER_SKELETON.create(level);
            case 1 -> EntityType.STRAY.create(level);
            case 2 -> EntityType.HUSK.create(level);
            case 3 -> EntityType.ZOMBIE.create(level);
            case 4 -> EntityType.SKELETON.create(level);
            case 5 -> EntityType.ZOMBIE_VILLAGER.create(level);
            default -> ModEntities.SKELETON_VILLAGER.get().create(level);
        };
        if (boss == null) {
            return;
        }
        Vec3 pos = findSpawnPos(level, player, r);
        boss.moveTo(pos.x, pos.y, pos.z, r.nextFloat() * 360.0F, 0.0F);
        boss.setTarget(player);
        boolean preferRanged = boss instanceof AbstractSkeleton || boss instanceof SkeletonVillager;
        gearMob(boss, lvl + 3, wave + 3, r, preferRanged, lvl);
        if (boss instanceof SkeletonVillager sv) {
            if (r.nextFloat() < 0.65F) {
                sv.setItemSlot(EquipmentSlot.MAINHAND, new ItemStack(Items.CROSSBOW));
            } else {
                sv.setItemSlot(EquipmentSlot.MAINHAND, new ItemStack(Items.BOW));
            }
            int tier = Mth.clamp((lvl + 3 + wave + 3) / 3, 0, 3);
            if (tier >= 2 && r.nextFloat() < 0.22F) {
                sv.setItemSlot(EquipmentSlot.OFFHAND, new ItemStack(ModItems.DIAMOND_SHIELD.get()));
            }
            if (r.nextFloat() < 0.35F) {
                sv.setSoulVariant(true);
            }
        }
        boss.setItemSlot(EquipmentSlot.HEAD, new ItemStack(ModItems.UNDEAD_CROWN.get()));
        boss.setDropChance(EquipmentSlot.HEAD, 1.0F);
        boss.getPersistentData().putBoolean(TAG_UNDEAD_CROWN_BOSS, true);
        boss.setCustomName(Component.literal("Undead Crown Boss").withStyle(ChatFormatting.GOLD, ChatFormatting.BOLD));
        boss.setCustomNameVisible(true);
        boss.setGlowingTag(true);
        applyGoldGlowingTeam(level, boss);
        if (boss.getAttribute(Attributes.SCALE) != null) {
            boss.getAttribute(Attributes.SCALE).setBaseValue(1.30D);
        }
        if (boss.getAttribute(Attributes.MAX_HEALTH) != null) {
            boss.getAttribute(Attributes.MAX_HEALTH).setBaseValue(80.0D + lvl * 8.0D);
            boss.setHealth(boss.getMaxHealth());
        }
        if (boss.getAttribute(Attributes.ARMOR) != null) {
            boss.getAttribute(Attributes.ARMOR).setBaseValue(10.0D + lvl);
        }
        if (boss.getAttribute(Attributes.ATTACK_DAMAGE) != null) {
            boss.getAttribute(Attributes.ATTACK_DAMAGE).setBaseValue(8.0D + lvl * 1.6D);
        }
        if (boss.getAttribute(Attributes.MOVEMENT_SPEED) != null) {
            boss.getAttribute(Attributes.MOVEMENT_SPEED).setBaseValue(0.30D);
        }

        if (level.addFreshEntity(boss)) {
            trackInvader(state, boss, player);
            level.playSound(null, pos.x, pos.y, pos.z, SoundEvents.WITHER_SPAWN, SoundSource.PLAYERS, 0.6F, 1.35F);
            level.sendParticles(ParticleTypes.TOTEM_OF_UNDYING, pos.x, pos.y + 1.0D, pos.z, 35, 0.6D, 0.9D, 0.6D, 0.02D);
            level.sendParticles(ParticleTypes.GLOW, pos.x, pos.y + 1.2D, pos.z, 24, 0.4D, 0.8D, 0.4D, 0.01D);
        }
    }

    private static void spawnUndeadSecretBoss(ServerLevel level, ServerPlayer player, @Nullable InvasionState state, RandomSource r, int lvl, int wave) {
        WitherSkeleton boss = EntityType.WITHER_SKELETON.create(level);
        if (boss == null) {
            return;
        }
        Vec3 pos = findSpawnPos(level, player, r);
        boss.moveTo(pos.x, pos.y, pos.z, r.nextFloat() * 360.0F, 0.0F);
        boss.setTarget(player);
        gearMob(boss, lvl + 5, wave + 5, r, false, lvl);
        boss.setItemSlot(EquipmentSlot.HEAD, new ItemStack(ModItems.UNDEAD_CROWN.get()));
        boss.setDropChance(EquipmentSlot.HEAD, 1.0F);
        boss.getPersistentData().putBoolean(TAG_UNDEAD_CROWN_BOSS, true);
        boss.getPersistentData().putBoolean(TAG_UNDEAD_SECRET_BOSS, true);
        boss.setCustomName(Component.literal("The Undead Boss").withStyle(ChatFormatting.RED, ChatFormatting.BOLD));
        boss.setCustomNameVisible(true);
        boss.setGlowingTag(true);
        applyRedGlowingTeam(level, boss);
        if (boss.getAttribute(Attributes.SCALE) != null) {
            boss.getAttribute(Attributes.SCALE).setBaseValue(1.9D);
        }
        if (boss.getAttribute(Attributes.MAX_HEALTH) != null) {
            boss.getAttribute(Attributes.MAX_HEALTH).setBaseValue(280.0D);
            boss.setHealth(boss.getMaxHealth());
        }
        if (boss.getAttribute(Attributes.ARMOR) != null) {
            boss.getAttribute(Attributes.ARMOR).setBaseValue(24.0D);
        }
        if (boss.getAttribute(Attributes.ATTACK_DAMAGE) != null) {
            boss.getAttribute(Attributes.ATTACK_DAMAGE).setBaseValue(18.0D);
        }
        if (boss.getAttribute(Attributes.MOVEMENT_SPEED) != null) {
            boss.getAttribute(Attributes.MOVEMENT_SPEED).setBaseValue(0.34D);
        }
        boss.addEffect(new net.minecraft.world.effect.MobEffectInstance(net.minecraft.world.effect.MobEffects.DAMAGE_RESISTANCE, 20 * 3600, 1, false, false));
        boss.addEffect(new net.minecraft.world.effect.MobEffectInstance(net.minecraft.world.effect.MobEffects.FIRE_RESISTANCE, 20 * 3600, 0, false, false));
        if (level.addFreshEntity(boss)) {
            trackInvader(state, boss, player);
            if (state != null) {
                state.secretBossId = boss.getUUID();
                ServerBossEvent redBoss = new ServerBossEvent(
                        Component.literal("The Undead Boss").withStyle(ChatFormatting.RED, ChatFormatting.BOLD),
                        BossEvent.BossBarColor.RED,
                        BossEvent.BossBarOverlay.PROGRESS);
                redBoss.addPlayer(player);
                redBoss.setProgress(1.0F);
                state.secretBossEvent = redBoss;
            }
            level.playSound(null, pos.x, pos.y, pos.z, SoundEvents.WITHER_SPAWN, SoundSource.PLAYERS, 0.95F, 0.8F);
            level.sendParticles(ParticleTypes.SOUL_FIRE_FLAME, pos.x, pos.y + 1.1D, pos.z, 70, 1.0D, 1.3D, 1.0D, 0.03D);
            level.sendParticles(ParticleTypes.FLAME, pos.x, pos.y + 1.1D, pos.z, 28, 0.6D, 1.2D, 0.6D, 0.01D);
        }
    }

    private static void applyGoldGlowingTeam(ServerLevel level, Mob mob) {
        var board = level.getScoreboard();
        PlayerTeam team = board.getPlayerTeam(TEAM_UNDEAD_CROWN_GLOW);
        if (team == null) {
            team = board.addPlayerTeam(TEAM_UNDEAD_CROWN_GLOW);
            team.setColor(ChatFormatting.GOLD);
        }
        board.addPlayerToTeam(mob.getStringUUID(), team);
    }

    private static void applyRedGlowingTeam(ServerLevel level, Mob mob) {
        var board = level.getScoreboard();
        PlayerTeam team = board.getPlayerTeam(TEAM_UNDEAD_SECRET_GLOW);
        if (team == null) {
            team = board.addPlayerTeam(TEAM_UNDEAD_SECRET_GLOW);
            team.setColor(ChatFormatting.RED);
        }
        board.addPlayerToTeam(mob.getStringUUID(), team);
    }

    private static void spawnUndeadBrute(ServerLevel level, ServerPlayer player, InvasionState state, RandomSource r, int lvl, int wave) {
        Mob brute = switch (r.nextInt(5)) {
            case 0 -> EntityType.HUSK.create(level);
            case 1 -> EntityType.ZOMBIE.create(level);
            case 2 -> EntityType.VINDICATOR.create(level);
            case 3 -> EntityType.WITHER_SKELETON.create(level);
            default -> EntityType.ZOMBIE_VILLAGER.create(level);
        };
        if (brute == null) {
            return;
        }
        Vec3 pos = findSpawnPos(level, player, r);
        brute.moveTo(pos.x, pos.y, pos.z, r.nextFloat() * 360.0F, 0.0F);
        brute.setTarget(player);
        gearMob(brute, lvl + 2, wave + 2, r, brute instanceof AbstractSkeleton, lvl);
        brute.setItemSlot(EquipmentSlot.MAINHAND, new ItemStack(Items.IRON_AXE));
        brute.setCustomName(Component.literal("Undead Brute").withStyle(ChatFormatting.DARK_RED, ChatFormatting.BOLD));
        brute.setCustomNameVisible(true);
        if (brute.getAttribute(Attributes.SCALE) != null) {
            double sc = brute instanceof Vindicator ? 1.12D : (brute instanceof WitherSkeleton ? 1.14D : 1.18D);
            brute.getAttribute(Attributes.SCALE).setBaseValue(sc);
        }
        if (brute.getAttribute(Attributes.MAX_HEALTH) != null) {
            brute.getAttribute(Attributes.MAX_HEALTH).setBaseValue(42.0D + lvl * 4.0D);
            brute.setHealth(brute.getMaxHealth());
        }
        if (level.addFreshEntity(brute)) {
            trackInvader(state, brute, player);
            level.sendParticles(ParticleTypes.SMOKE, pos.x, pos.y + 1.0D, pos.z, 18, 0.4D, 0.5D, 0.4D, 0.01D);
        }
    }

    private static void spawnBoneCaptain(ServerLevel level, ServerPlayer player, InvasionState state, RandomSource r, int lvl, int wave) {
        Stray captain = EntityType.STRAY.create(level);
        if (captain == null) {
            return;
        }
        Vec3 pos = findSpawnPos(level, player, r);
        captain.moveTo(pos.x, pos.y, pos.z, r.nextFloat() * 360.0F, 0.0F);
        captain.setTarget(player);
        gearMob(captain, lvl + 2, wave + 1, r, true, lvl);
        equipInvasionBannerOnHead(captain, level.registryAccess());
        captain.setCustomName(Component.literal("Bone Captain").withStyle(ChatFormatting.AQUA, ChatFormatting.BOLD));
        captain.setCustomNameVisible(true);
        if (captain.getAttribute(Attributes.MAX_HEALTH) != null) {
            captain.getAttribute(Attributes.MAX_HEALTH).setBaseValue(36.0D + lvl * 3.0D);
            captain.setHealth(captain.getMaxHealth());
        }
        if (level.addFreshEntity(captain)) {
            trackInvader(state, captain, player);
            level.sendParticles(ParticleTypes.SOUL_FIRE_FLAME, pos.x, pos.y + 1.0D, pos.z, 14, 0.3D, 0.6D, 0.3D, 0.008D);
        }
    }

    private static void spawnSkeletonVillager(ServerLevel level, ServerPlayer player, InvasionState state, RandomSource r, int lvl, int wave) {
        var e = ModEntities.SKELETON_VILLAGER.get().create(level);
        if (e == null) {
            return;
        }
        Vec3 pos = findSpawnPos(level, player, r);
        e.moveTo(pos.x, pos.y, pos.z, r.nextFloat() * 360.0F, 0.0F);
        e.setTarget(player);
        if (level.addFreshEntity(e)) {
            trackInvader(state, e, player);
            gearMob(e, lvl, wave, r, true, lvl);
            if (lvl >= 5 && r.nextFloat() < 0.12F) {
                equipInvasionBannerOnHead(e, level.registryAccess());
            }
        }
    }

    private static void spawnSoulSkeletonVillager(ServerLevel level, ServerPlayer player, InvasionState state, RandomSource r, int lvl, int wave) {
        var e = ModEntities.SKELETON_VILLAGER.get().create(level);
        if (e == null) {
            return;
        }
        Vec3 pos = findSpawnPos(level, player, r);
        e.moveTo(pos.x, pos.y, pos.z, r.nextFloat() * 360.0F, 0.0F);
        e.setTarget(player);
        if (e instanceof SkeletonVillager sv) {
            sv.setSoulVariant(true);
        }
        if (level.addFreshEntity(e)) {
            trackInvader(state, e, player);
            // Soul variants spawn slightly stronger than regular ones.
            gearMob(e, lvl + 1, wave + 1, r, true, lvl);
            if (lvl >= 5 && r.nextFloat() < 0.18F) {
                equipInvasionBannerOnHead(e, level.registryAccess());
            }
        }
    }

    private static void spawnSkeletonRider(ServerLevel level, ServerPlayer player, InvasionState state, RandomSource r, int lvl, int wave, boolean elite) {
        SkeletonHorse horse = EntityType.SKELETON_HORSE.create(level);
        Skeleton rider = EntityType.SKELETON.create(level);
        if (horse == null || rider == null) {
            return;
        }
        Vec3 pos = findSpawnPos(level, player, r);
        horse.moveTo(pos.x, pos.y, pos.z, r.nextFloat() * 360.0F, 0.0F);
        rider.moveTo(pos.x, pos.y, pos.z, r.nextFloat() * 360.0F, 0.0F);
        horse.setTamed(true);
        rider.setTarget(player);

        gearMob(rider, lvl + (elite ? 2 : 0), wave + (elite ? 2 : 0), r, false, lvl);
        if (elite) {
            equipInvasionBannerOnHead(rider, level.registryAccess());
        }
        if (level.addFreshEntity(horse) && level.addFreshEntity(rider)) {
            rider.startRiding(horse, true);
            trackInvader(state, horse, player);
            trackInvader(state, rider, player);
        }
    }

    private static void spawnZombie(ServerLevel level, ServerPlayer player, InvasionState state, RandomSource r, int lvl, int wave, boolean villager) {
        Zombie z = villager ? EntityType.ZOMBIE_VILLAGER.create(level) : EntityType.ZOMBIE.create(level);
        if (z == null) {
            return;
        }
        Vec3 pos = findSpawnPos(level, player, r);
        z.moveTo(pos.x, pos.y, pos.z, r.nextFloat() * 360.0F, 0.0F);
        z.setTarget(player);
        gearMob(z, lvl, wave, r, false, lvl);
        if (level.addFreshEntity(z)) {
            trackInvader(state, z, player);
            if (lvl >= 5 && !villager && r.nextFloat() < 0.08F) {
                equipInvasionBannerOnHead(z, level.registryAccess());
            }
        }
    }

    private static void spawnZombieRider(ServerLevel level, ServerPlayer player, InvasionState state, RandomSource r, int lvl, int wave, boolean elite) {
        ZombieHorse horse = EntityType.ZOMBIE_HORSE.create(level);
        Zombie rider = EntityType.ZOMBIE.create(level);
        if (horse == null || rider == null) {
            return;
        }
        Vec3 pos = findSpawnPos(level, player, r);
        horse.moveTo(pos.x, pos.y, pos.z, r.nextFloat() * 360.0F, 0.0F);
        rider.moveTo(pos.x, pos.y, pos.z, r.nextFloat() * 360.0F, 0.0F);
        horse.setTamed(true);
        rider.setTarget(player);
        gearMob(rider, lvl + (elite ? 1 : 0), wave + (elite ? 1 : 0), r, false, lvl);
        if (elite || r.nextFloat() < 0.2F) {
            equipInvasionBannerOnHead(rider, level.registryAccess());
        }
        if (level.addFreshEntity(horse) && level.addFreshEntity(rider)) {
            rider.startRiding(horse, true);
            trackInvader(state, horse, player);
            trackInvader(state, rider, player);
        }
    }

    private static void spawnHusk(ServerLevel level, ServerPlayer player, InvasionState state, RandomSource r, int lvl, int wave) {
        Husk h = EntityType.HUSK.create(level);
        if (h == null) {
            return;
        }
        Vec3 pos = findSpawnPos(level, player, r);
        h.moveTo(pos.x, pos.y, pos.z, r.nextFloat() * 360.0F, 0.0F);
        h.setTarget(player);
        gearMob(h, lvl + 1, wave + 1, r, false, lvl);
        if (level.addFreshEntity(h)) {
            trackInvader(state, h, player);
        }
    }

    private static void spawnWitch(ServerLevel level, ServerPlayer player, InvasionState state, RandomSource r) {
        Witch w = EntityType.WITCH.create(level);
        if (w == null) {
            return;
        }
        Vec3 pos = findSpawnPos(level, player, r);
        w.moveTo(pos.x, pos.y, pos.z, r.nextFloat() * 360.0F, 0.0F);
        w.setTarget(player);
        if (level.addFreshEntity(w)) {
            trackInvader(state, w, player);
        }
    }

    private static void spawnStrayArcher(ServerLevel level, ServerPlayer player, InvasionState state, RandomSource r, int lvl, int wave) {
        Stray stray = EntityType.STRAY.create(level);
        if (stray == null) {
            return;
        }
        Vec3 pos = findSpawnPos(level, player, r);
        stray.moveTo(pos.x, pos.y, pos.z, r.nextFloat() * 360.0F, 0.0F);
        stray.setTarget(player);
        gearMob(stray, lvl + 1, wave, r, false, lvl);
        if (level.addFreshEntity(stray)) {
            trackInvader(state, stray, player);
        }
    }

    private static void spawnDrownedTrident(ServerLevel level, ServerPlayer player, InvasionState state, RandomSource r, int lvl, int wave) {
        Drowned drowned = EntityType.DROWNED.create(level);
        if (drowned == null) {
            return;
        }
        Vec3 pos = findSpawnPos(level, player, r);
        drowned.moveTo(pos.x, pos.y, pos.z, r.nextFloat() * 360.0F, 0.0F);
        drowned.setTarget(player);
        gearMob(drowned, lvl + 1, wave + 1, r, false, lvl);
        drowned.setItemSlot(EquipmentSlot.MAINHAND, new ItemStack(Items.TRIDENT));
        drowned.setDropChance(EquipmentSlot.MAINHAND, 0.02F);
        if (level.addFreshEntity(drowned)) {
            trackInvader(state, drowned, player);
        }
    }

    private static void spawnCaveSpider(ServerLevel level, ServerPlayer player, InvasionState state, RandomSource r, int lvl, int wave) {
        CaveSpider spider = EntityType.CAVE_SPIDER.create(level);
        if (spider == null) {
            return;
        }
        Vec3 pos = findSpawnPos(level, player, r);
        spider.moveTo(pos.x, pos.y, pos.z, r.nextFloat() * 360.0F, 0.0F);
        spider.setTarget(player);
        if (level.addFreshEntity(spider)) {
            trackInvader(state, spider, player);
        }
    }

    private static void spawnCorruptedVindicator(ServerLevel level, ServerPlayer player, InvasionState state, RandomSource r, int lvl, int wave) {
        Vindicator vindicator = EntityType.VINDICATOR.create(level);
        if (vindicator == null) {
            return;
        }
        Vec3 pos = findSpawnPos(level, player, r);
        vindicator.moveTo(pos.x, pos.y, pos.z, r.nextFloat() * 360.0F, 0.0F);
        vindicator.setTarget(player);
        gearMob(vindicator, lvl + 1, wave + 1, r, false, lvl);
        vindicator.setItemSlot(EquipmentSlot.MAINHAND, new ItemStack(Items.IRON_AXE));
        if (r.nextFloat() < 0.4F) {
            equipInvasionBannerOnHead(vindicator, level.registryAccess());
        }
        if (level.addFreshEntity(vindicator)) {
            trackInvader(state, vindicator, player);
        }
    }

    private static void spawnPhantomHarasser(ServerLevel level, ServerPlayer player, InvasionState state, RandomSource r, int lvl, int wave) {
        Phantom phantom = EntityType.PHANTOM.create(level);
        if (phantom == null) {
            return;
        }
        Vec3 base = player.position();
        double x = base.x + (r.nextDouble() - 0.5D) * PHANTOM_XZ_SPREAD;
        double z = base.z + (r.nextDouble() - 0.5D) * PHANTOM_XZ_SPREAD;
        double y = base.y + 15.0D + r.nextDouble() * 10.0D;
        phantom.moveTo(x, y, z, r.nextFloat() * 360.0F, 0.0F);
        phantom.setPhantomSize(Mth.clamp((lvl + wave) / 4, 0, 5));
        phantom.setTarget(player);
        if (level.addFreshEntity(phantom)) {
            trackInvader(state, phantom, player);
        }
    }

    private static void spawnUndeadIllager(ServerLevel level, ServerPlayer player, InvasionState state, RandomSource r, int lvl, int wave) {
        var illager = ModEntities.UNDEAD_ILLAGER.get().create(level);
        if (illager == null) {
            return;
        }
        Vec3 pos = findSpawnPos(level, player, r);
        illager.moveTo(pos.x, pos.y, pos.z, r.nextFloat() * 360.0F, 0.0F);
        illager.setTarget(player);
        if (illager.getAttribute(Attributes.MAX_HEALTH) != null) {
            double hp = 36.0D + (lvl * 3.5D) + Math.max(0, wave - 2) * 1.25D;
            illager.getAttribute(Attributes.MAX_HEALTH).setBaseValue(hp);
            illager.setHealth(illager.getMaxHealth());
        }
        if (illager.getAttribute(Attributes.ARMOR) != null) {
            illager.getAttribute(Attributes.ARMOR).setBaseValue(8.0D + lvl * 1.15D);
        }
        // Captain integration: some Undead Illagers can lead a wave with the invasion banner.
        float captainChance = 0.12F + (lvl * 0.035F) + (wave >= Math.max(4, getInvasionWaveCount(lvl) - 1) ? 0.08F : 0.0F);
        if (r.nextFloat() < Mth.clamp(captainChance, 0.12F, 0.42F)) {
            equipInvasionBannerOnHead(illager, level.registryAccess());
            illager.setCustomName(Component.literal("Undead Captain").withStyle(ChatFormatting.DARK_AQUA, ChatFormatting.BOLD));
            illager.setCustomNameVisible(r.nextFloat() < 0.35F);
        }
        if (level.addFreshEntity(illager)) {
            trackInvader(state, illager, player);
            level.sendParticles(ModParticles.UNDEAD_INVASION.get(), pos.x, pos.y + 1.0D, pos.z, 40, 0.7D, 0.9D, 0.7D, 0.015D);
            level.playSound(null, pos.x, pos.y, pos.z, SoundEvents.EVOKER_PREPARE_SUMMON, SoundSource.HOSTILE, 1.0F, 0.8F);
        }
    }

    /**
     * 0 = cuir, 1 = mailles, 2 = cuivre BMC, 3 = fer, 4 = émeraude BMC, 5 = diamant.
     */
    private static int pickInvasionOutfit(int level, int wave, int invasionTier, RandomSource r) {
        int inv = Mth.clamp(invasionTier, 1, 6);
        int score = Mth.clamp(level + wave, 0, 40);
        int base = Mth.clamp(score / 2 + (inv - 1) / 2, 0, 7);
        int jitter = r.nextInt(5) - 2;
        int outfit = Mth.clamp(base + jitter, 0, 5);
        if (inv >= 5 && r.nextFloat() < 0.32F) {
            outfit = Math.min(5, outfit + 1);
        }
        if (r.nextFloat() < 0.16F) {
            outfit = Mth.clamp(outfit + 1, 0, 5);
        } else if (r.nextFloat() < 0.12F) {
            outfit = Mth.clamp(outfit - 1, 0, 5);
        }
        return outfit;
    }

    private static ItemStack invasionArmorPiece(EquipmentSlot slot, int outfit) {
        int o = Mth.clamp(outfit, 0, 5);
        return switch (o) {
            case 0 -> switch (slot) {
                case HEAD -> new ItemStack(Items.LEATHER_HELMET);
                case CHEST -> new ItemStack(Items.LEATHER_CHESTPLATE);
                case LEGS -> new ItemStack(Items.LEATHER_LEGGINGS);
                case FEET -> new ItemStack(Items.LEATHER_BOOTS);
                default -> ItemStack.EMPTY;
            };
            case 1 -> switch (slot) {
                case HEAD -> new ItemStack(Items.CHAINMAIL_HELMET);
                case CHEST -> new ItemStack(Items.CHAINMAIL_CHESTPLATE);
                case LEGS -> new ItemStack(Items.CHAINMAIL_LEGGINGS);
                case FEET -> new ItemStack(Items.CHAINMAIL_BOOTS);
                default -> ItemStack.EMPTY;
            };
            case 2 -> switch (slot) {
                case HEAD -> new ItemStack(ModItems.COPPER_HELMET.get());
                case CHEST -> new ItemStack(ModItems.COPPER_CHESTPLATE.get());
                case LEGS -> new ItemStack(ModItems.COPPER_LEGGINGS.get());
                case FEET -> new ItemStack(ModItems.COPPER_BOOTS.get());
                default -> ItemStack.EMPTY;
            };
            case 3 -> switch (slot) {
                case HEAD -> new ItemStack(Items.IRON_HELMET);
                case CHEST -> new ItemStack(Items.IRON_CHESTPLATE);
                case LEGS -> new ItemStack(Items.IRON_LEGGINGS);
                case FEET -> new ItemStack(Items.IRON_BOOTS);
                default -> ItemStack.EMPTY;
            };
            case 4 -> switch (slot) {
                case HEAD -> new ItemStack(ModItems.EMERALD_HELMET.get());
                case CHEST -> new ItemStack(ModItems.EMERALD_CHESTPLATE.get());
                case LEGS -> new ItemStack(ModItems.EMERALD_LEGGINGS.get());
                case FEET -> new ItemStack(ModItems.EMERALD_BOOTS.get());
                default -> ItemStack.EMPTY;
            };
            default -> switch (slot) {
                case HEAD -> new ItemStack(Items.DIAMOND_HELMET);
                case CHEST -> new ItemStack(Items.DIAMOND_CHESTPLATE);
                case LEGS -> new ItemStack(Items.DIAMOND_LEGGINGS);
                case FEET -> new ItemStack(Items.DIAMOND_BOOTS);
                default -> ItemStack.EMPTY;
            };
        };
    }

    private static ArmorTrim invasionPrimaryTrim(
            int invasionTier,
            RandomSource r,
            HolderGetter<TrimMaterial> materials,
            HolderGetter<TrimPattern> patterns) {
        int t = Mth.clamp(invasionTier, 1, 6);
        return switch (t) {
            case 1 -> new ArmorTrim(materials.getOrThrow(TrimMaterials.QUARTZ), patterns.getOrThrow(TrimPatterns.COAST));
            case 2 -> new ArmorTrim(materials.getOrThrow(TrimMaterials.COPPER), patterns.getOrThrow(TrimPatterns.WARD));
            case 3 -> r.nextBoolean()
                    ? new ArmorTrim(materials.getOrThrow(TrimMaterials.LAPIS), patterns.getOrThrow(TrimPatterns.HOST))
                    : new ArmorTrim(materials.getOrThrow(TrimMaterials.LAPIS), patterns.getOrThrow(TrimPatterns.TIDE));
            case 4 -> r.nextBoolean()
                    ? new ArmorTrim(materials.getOrThrow(TrimMaterials.AMETHYST), patterns.getOrThrow(TrimPatterns.RIB))
                    : new ArmorTrim(materials.getOrThrow(TrimMaterials.REDSTONE), patterns.getOrThrow(TrimPatterns.SPIRE));
            case 5 -> r.nextBoolean()
                    ? new ArmorTrim(materials.getOrThrow(TrimMaterials.EMERALD), patterns.getOrThrow(TrimPatterns.EYE))
                    : new ArmorTrim(materials.getOrThrow(TrimMaterials.GOLD), patterns.getOrThrow(TrimPatterns.SENTRY));
            default -> r.nextBoolean()
                    ? new ArmorTrim(materials.getOrThrow(TrimMaterials.DIAMOND), patterns.getOrThrow(TrimPatterns.SILENCE))
                    : new ArmorTrim(materials.getOrThrow(TrimMaterials.NETHERITE), patterns.getOrThrow(TrimPatterns.BOLT));
        };
    }

    /** Accent sur bottes : motifs récents (1.21) + matériaux contrastés. */
    private static ArmorTrim invasionAccentTrim(
            int invasionTier,
            RandomSource r,
            HolderGetter<TrimMaterial> materials,
            HolderGetter<TrimPattern> patterns) {
        int t = Mth.clamp(invasionTier, 1, 6);
        if (t >= 5) {
            return r.nextBoolean()
                    ? new ArmorTrim(materials.getOrThrow(TrimMaterials.REDSTONE), patterns.getOrThrow(TrimPatterns.FLOW))
                    : new ArmorTrim(materials.getOrThrow(TrimMaterials.COPPER), patterns.getOrThrow(TrimPatterns.BOLT));
        }
        if (t >= 3) {
            return new ArmorTrim(materials.getOrThrow(TrimMaterials.IRON), patterns.getOrThrow(TrimPatterns.WILD));
        }
        return new ArmorTrim(materials.getOrThrow(TrimMaterials.QUARTZ), patterns.getOrThrow(TrimPatterns.DUNE));
    }

    private static void applyInvasionArmorTrim(Mob mob, int invasionTier, RandomSource r) {
        var reg = mob.registryAccess();
        var materials = reg.lookupOrThrow(Registries.TRIM_MATERIAL);
        var patterns = reg.lookupOrThrow(Registries.TRIM_PATTERN);
        ArmorTrim primary = invasionPrimaryTrim(invasionTier, r, materials, patterns);
        ArmorTrim accent = invasionAccentTrim(invasionTier, r, materials, patterns);
        boolean accentFeet = invasionTier >= 3 && r.nextFloat() < 0.34F;
        for (EquipmentSlot slot : EquipmentSlot.values()) {
            if (!slot.isArmor()) {
                continue;
            }
            ItemStack stack = mob.getItemBySlot(slot);
            if (stack.isEmpty() || !(stack.getItem() instanceof ArmorItem)) {
                continue;
            }
            stack.set(DataComponents.TRIM, slot == EquipmentSlot.FEET && accentFeet ? accent : primary);
        }
    }

    private static void gearMob(Mob mob, int level, int wave, RandomSource r, boolean preferCrossbow, int invasionTier) {
        int outfit = pickInvasionOutfit(level, wave, invasionTier, r);
        mob.setItemSlot(EquipmentSlot.HEAD, invasionArmorPiece(EquipmentSlot.HEAD, outfit));
        mob.setItemSlot(EquipmentSlot.CHEST, invasionArmorPiece(EquipmentSlot.CHEST, outfit));
        mob.setItemSlot(EquipmentSlot.LEGS, invasionArmorPiece(EquipmentSlot.LEGS, outfit));
        mob.setItemSlot(EquipmentSlot.FEET, invasionArmorPiece(EquipmentSlot.FEET, outfit));
        applyInvasionArmorTrim(mob, invasionTier, r);

        if (mob instanceof AbstractSkeleton sk) {
            if (preferCrossbow || r.nextFloat() < 0.6F) {
                ItemStack cb = new ItemStack(Items.CROSSBOW);
                if (r.nextFloat() < 0.5F) {
                    cb.enchant(mob.level().registryAccess().lookupOrThrow(Registries.ENCHANTMENT)
                            .getOrThrow(net.minecraft.world.item.enchantment.Enchantments.QUICK_CHARGE), 1 + r.nextInt(2));
                }
                sk.setItemSlot(EquipmentSlot.MAINHAND, cb);
            } else {
                sk.setItemSlot(EquipmentSlot.MAINHAND, new ItemStack(Items.BOW));
            }
            if (outfit >= 3 && r.nextFloat() < 0.22F) {
                sk.setItemSlot(EquipmentSlot.OFFHAND, new ItemStack(ModItems.DIAMOND_SHIELD.get()));
            }
        } else if (mob instanceof Zombie || mob instanceof Husk || mob instanceof ZombieVillager || mob instanceof WitherSkeleton) {
            if (r.nextFloat() < 0.35F) {
                mob.setItemSlot(EquipmentSlot.MAINHAND, new ItemStack(Items.STONE_SHOVEL));
            } else if (outfit >= 3 && r.nextFloat() < 0.24F) {
                mob.setItemSlot(EquipmentSlot.MAINHAND, new ItemStack(Items.IRON_SWORD));
            }
        }

        for (EquipmentSlot slot : EquipmentSlot.values()) {
            if (slot == EquipmentSlot.MAINHAND || slot == EquipmentSlot.OFFHAND || slot.isArmor()) {
                mob.setDropChance(slot, 0.0F);
            }
        }
    }

    private static Vec3 findSpawnPos(ServerLevel level, ServerPlayer player, RandomSource r) {
        Vec3 base = player.position();
        for (int i = 0; i < 16; i++) {
            double angle = r.nextDouble() * Mth.TWO_PI;
            double radius = SPAWN_RING_MIN_BLOCKS + r.nextDouble() * SPAWN_RING_RANDOM_EXTRA;
            int x = Mth.floor(base.x + Math.cos(angle) * radius);
            int z = Mth.floor(base.z + Math.sin(angle) * radius);
            int y = level.getHeight(net.minecraft.world.level.levelgen.Heightmap.Types.MOTION_BLOCKING_NO_LEAVES, x, z);
            BlockPos pos = new BlockPos(x, y, z);
            if (!level.getBlockState(pos.below()).isAir() && level.getBlockState(pos).getCollisionShape(level, pos).isEmpty()) {
                return new Vec3(x + 0.5, y + 0.1, z + 0.5);
            }
        }
        return player.position().add(4.0, 0.1, 0.0);
    }

    @Nullable
    private static LivingEntity findSecretBoss(ServerLevel level, InvasionState state) {
        if (state.secretBossId == null) {
            return null;
        }
        var entity = level.getEntity(state.secretBossId);
        return entity instanceof LivingEntity le ? le : null;
    }

    private static void tickSecretBossPowers(ServerLevel level, ServerPlayer owner, InvasionState state, long gameTime) {
        LivingEntity boss = findSecretBoss(level, state);
        if (boss == null || !boss.isAlive() || !boss.getPersistentData().getBoolean(TAG_UNDEAD_SECRET_BOSS)) {
            return;
        }
        if (gameTime % 70L == 0L) {
            castSecretBossNova(level, boss);
        }
        if (gameTime % 100L == 0L && owner.isAlive() && owner.distanceToSqr(boss) <= (24.0D * 24.0D)) {
            castSecretBossDrain(level, boss, owner);
        }
    }

    private static void castSecretBossNova(ServerLevel level, LivingEntity boss) {
        AABB area = boss.getBoundingBox().inflate(8.0D, 3.0D, 8.0D);
        level.sendParticles(ParticleTypes.SOUL, boss.getX(), boss.getY() + 1.1D, boss.getZ(), 55, 1.2D, 0.5D, 1.2D, 0.02D);
        level.playSound(null, boss.getX(), boss.getY(), boss.getZ(), SoundEvents.WARDEN_SONIC_BOOM, SoundSource.HOSTILE, 0.55F, 0.9F);
        for (ServerPlayer p : level.getEntitiesOfClass(ServerPlayer.class, area)) {
            if (p.isSpectator() || p.isCreative()) {
                continue;
            }
            p.hurt(level.damageSources().magic(), 6.0F);
            p.addEffect(new net.minecraft.world.effect.MobEffectInstance(net.minecraft.world.effect.MobEffects.MOVEMENT_SLOWDOWN, 80, 1, false, true));
            p.addEffect(new net.minecraft.world.effect.MobEffectInstance(net.minecraft.world.effect.MobEffects.WEAKNESS, 60, 0, false, true));
        }
    }

    private static void castSecretBossDrain(ServerLevel level, LivingEntity boss, ServerPlayer target) {
        level.sendParticles(ParticleTypes.SCULK_SOUL, target.getX(), target.getY() + 1.0D, target.getZ(), 28, 0.4D, 0.5D, 0.4D, 0.015D);
        level.sendParticles(ParticleTypes.SCULK_SOUL, boss.getX(), boss.getY() + 1.0D, boss.getZ(), 28, 0.4D, 0.6D, 0.4D, 0.015D);
        target.hurt(level.damageSources().magic(), 5.0F);
        target.addEffect(new net.minecraft.world.effect.MobEffectInstance(net.minecraft.world.effect.MobEffects.WITHER, 80, 0, false, true));
        boss.heal(6.0F);
        Vec3 pull = boss.position().subtract(target.position()).normalize().scale(0.55D);
        target.push(pull.x, 0.12D, pull.z);
        level.playSound(null, boss.getX(), boss.getY(), boss.getZ(), SoundEvents.WITHER_SHOOT, SoundSource.HOSTILE, 0.35F, 0.75F);
    }

    private static void trackInvader(@Nullable InvasionState state, net.minecraft.world.entity.Entity entity, ServerPlayer owner) {
        if (state == null) {
            if (entity instanceof Mob mob) {
                mob.setPersistenceRequired();
            }
            return;
        }
        int participantCount = countParticipants(owner.serverLevel(), owner, PLAYER_SCALING_RANGE);
        int dynamicCap = MAX_ACTIVE_INVADERS_PER_PLAYER + Math.max(0, participantCount - 1) * 35;
        boolean priorityBoss = (entity instanceof Mob mobEntity) && mobEntity.getPersistentData().getBoolean(TAG_UNDEAD_CROWN_BOSS);
        if (!priorityBoss && state.aliveInvaders.size() >= dynamicCap) {
            entity.discard();
            return;
        }
        if (entity instanceof Mob mob) {
            mob.setPersistenceRequired();
            mob.getPersistentData().putUUID(TAG_INVADER_OWNER, owner.getUUID());
        }
        state.aliveInvaders.add(entity.getUUID());
        state.waveSpawnedCount++;
    }

    /** Nombre total de vagues pour un niveau d'invasion (1→6). */
    public static int getInvasionWaveCount(int invasionLevel) {
        return totalWaves(Mth.clamp(invasionLevel, 1, 6));
    }

    /** Types reconnus par {@link #spawnDebugInvasionMob} (commande /bmc undeadinvasion spawn). */
    public static final List<String> DEBUG_SPAWN_KINDS = List.of(
            "boss",
            "secret_boss",
            "brute",
            "captain",
            "skeleton_villager",
            "soul_skeleton_villager",
            "skeleton_rider",
            "skeleton_rider_elite",
            "zombie_rider",
            "zombie_rider_elite",
            "zombie",
            "zombie_villager",
            "husk",
            "witch",
            "stray",
            "drowned",
            "cave_spider",
            "vindicator",
            "phantom",
            "undead_illager",
            "grunt_random");

    /**
     * Fait apparaître un mob d'invasion près du joueur sans le lier à une invasion active (tests / commande).
     *
     * @param invasionLevel niveau d'invasion (1–6), utilisé pour l'équipement et les stats
     * @param wave          vague simulée (1 … {@link #getInvasionWaveCount(int)})
     */
    public static boolean spawnDebugInvasionMob(ServerLevel level, ServerPlayer player, String kind, int invasionLevel, int wave) {
        int lvl = Mth.clamp(invasionLevel, 1, 6);
        int tw = getInvasionWaveCount(lvl);
        int w = Mth.clamp(wave, 1, tw);
        RandomSource r = level.random;
        String k = kind.toLowerCase(Locale.ROOT);
        switch (k) {
            case "boss" -> spawnUndeadCrownBoss(level, player, null, r, lvl, w);
            case "secret_boss" -> spawnUndeadSecretBoss(level, player, null, r, lvl, w);
            case "brute" -> spawnUndeadBrute(level, player, null, r, lvl, w);
            case "captain" -> spawnBoneCaptain(level, player, null, r, lvl, w);
            case "skeleton_villager" -> spawnSkeletonVillager(level, player, null, r, lvl, w);
            case "soul_skeleton_villager" -> spawnSoulSkeletonVillager(level, player, null, r, lvl, w);
            case "skeleton_rider" -> spawnSkeletonRider(level, player, null, r, lvl, w, false);
            case "skeleton_rider_elite" -> spawnSkeletonRider(level, player, null, r, lvl, w, true);
            case "zombie_rider" -> spawnZombieRider(level, player, null, r, lvl, w, false);
            case "zombie_rider_elite" -> spawnZombieRider(level, player, null, r, lvl, w, true);
            case "zombie" -> spawnZombie(level, player, null, r, lvl, w, false);
            case "zombie_villager" -> spawnZombie(level, player, null, r, lvl, w, true);
            case "husk" -> spawnHusk(level, player, null, r, lvl, w);
            case "witch" -> spawnWitch(level, player, null, r);
            case "stray" -> spawnStrayArcher(level, player, null, r, lvl, w);
            case "drowned" -> spawnDrownedTrident(level, player, null, r, lvl, w);
            case "cave_spider" -> spawnCaveSpider(level, player, null, r, lvl, w);
            case "vindicator" -> spawnCorruptedVindicator(level, player, null, r, lvl, w);
            case "phantom" -> spawnPhantomHarasser(level, player, null, r, lvl, w);
            case "undead_illager" -> spawnUndeadIllager(level, player, null, r, lvl, w);
            case "grunt_random" -> {
                int pick = r.nextInt(6);
                if (pick == 0) {
                    spawnZombie(level, player, null, r, lvl, w, false);
                } else if (pick == 1) {
                    spawnHusk(level, player, null, r, lvl, w);
                } else if (pick == 2) {
                    spawnStrayArcher(level, player, null, r, lvl, w);
                } else if (pick == 3) {
                    spawnSkeletonVillager(level, player, null, r, lvl, w);
                } else if (pick == 4) {
                    spawnSoulSkeletonVillager(level, player, null, r, lvl, w);
                } else {
                    spawnUndeadIllager(level, player, null, r, lvl, w);
                }
            }
            default -> {
                return false;
            }
        }
        return true;
    }

    private static int countParticipants(ServerLevel level, ServerPlayer center, double range) {
        int count = 0;
        double r2 = range * range;
        for (ServerPlayer player : level.players()) {
            if (!player.isAlive() || player.isSpectator()) {
                continue;
            }
            if (player.distanceToSqr(center) <= r2) {
                count++;
            }
        }
        return Math.max(1, count);
    }

    private static float playerScaleMultiplier(int participants) {
        // +35% per extra nearby player, capped to keep performance under control.
        float scale = 1.0F + (Math.max(0, participants - 1) * 0.35F);
        return Mth.clamp(scale, 1.0F, 2.1F);
    }

    private static int scaledCount(int base, float scale) {
        if (base <= 0) {
            return 0;
        }
        return Math.max(1, Mth.ceil(base * scale));
    }

    private static int softWaveCount(int base) {
        if (base <= 0) {
            return 0;
        }
        return Math.max(1, Mth.ceil(base * 0.70F));
    }

    private static float highTierWaveNerf(int invasionLevel, int wave, int totalWaves) {
        if (invasionLevel <= 4) {
            return 1.0F;
        }
        if (wave < Math.max(4, totalWaves - 2)) {
            return 0.90F;
        }
        return invasionLevel >= 6 ? 0.72F : 0.80F;
    }

    private static int applyWaveNerf(int count, float multiplier) {
        if (count <= 0) {
            return 0;
        }
        return Math.max(1, Mth.ceil(count * multiplier));
    }

    private static int applyCappedCount(int count, float scale, int minIfPresent) {
        if (count <= 0) {
            return 0;
        }
        return Math.max(minIfPresent, Mth.ceil(count * scale));
    }

    private static int waveMobCap(int invasionLevel, int wave, int totalWaves, int participants) {
        int base = 14 + invasionLevel * 3 + (wave / 2);
        if (wave >= totalWaves) {
            base += 2;
        }
        int multiplayerBonus = Math.max(0, participants - 1) * 3;
        return base + multiplayerBonus;
    }

    private static void configureUndeadWaveCombatPacing(InvasionState state, int lvl) {
        int plannedTotal = state.pendingSpawns.size();
        if (plannedTotal <= 0) {
            state.miniWaveSize = 1;
            state.combatCap = 1;
            return;
        }
        int preferChunk = Mth.clamp(5 + lvl / 2, 5, 8);
        state.miniWaveSize = Math.min(plannedTotal, preferChunk);
        int bonusCap = Mth.clamp(1 + lvl / 2, 1, 4);
        state.combatCap = Math.min(plannedTotal, Math.max(state.miniWaveSize, state.miniWaveSize + bonusCap));
    }

    private static int undeadSpawnPulseIntervalTicks(int lvl, int waveIndex) {
        return 8 + lvl * 4 + waveIndex * 2;
    }

    private static long undeadFirstWaveSpawnDelayTicks(int lvl) {
        return 8L + (long) lvl * 3L;
    }

    private static void playWaveStartAmbience(ServerLevel level, ServerPlayer player, int levelValue, int wave, int totalWaves) {
        RandomSource r = level.random;
        float lateWaveBoost = (wave >= totalWaves) ? 1.15F : 1.0F;
        float highLevelBoost = (levelValue >= 5) ? 1.15F : 1.0F;
        float volume = 0.65F * lateWaveBoost * highLevelBoost;

        // Layered sounds to make each wave arrival feel impactful.
        level.playSound(null, player.getX(), player.getY(), player.getZ(),
                SoundEvents.RAID_HORN, SoundSource.PLAYERS, 0.55F * highLevelBoost, 1.45F - (levelValue * 0.08F));
        level.playSound(null, player.getX(), player.getY(), player.getZ(),
                SoundEvents.ZOMBIE_VILLAGER_CONVERTED, SoundSource.PLAYERS, volume, 0.65F + r.nextFloat() * 0.18F);
        level.playSound(null, player.getX(), player.getY(), player.getZ(),
                SoundEvents.WITHER_AMBIENT, SoundSource.PLAYERS, 0.35F * highLevelBoost, 1.55F - (wave * 0.05F));
        level.playSound(null, player.getX(), player.getY(), player.getZ(),
                SoundEvents.SOUL_ESCAPE.value(), SoundSource.PLAYERS, 0.55F * lateWaveBoost, 0.9F + r.nextFloat() * 0.15F);
        level.playSound(null, player.getX(), player.getY(), player.getZ(),
                SoundEvents.EVOKER_PREPARE_SUMMON, SoundSource.PLAYERS, 0.3F * highLevelBoost, 0.8F + r.nextFloat() * 0.1F);
        if (wave >= Math.max(3, totalWaves - 1)) {
            level.playSound(null, player.getX(), player.getY(), player.getZ(),
                    SoundEvents.WARDEN_NEARBY_CLOSE, SoundSource.PLAYERS, 0.35F * lateWaveBoost, 0.85F);
        }

        // Multi-layer particle burst around player.
        level.sendParticles(ParticleTypes.SOUL, player.getX(), player.getY() + 0.4D, player.getZ(),
                24 + levelValue * 4, 1.5D, 0.7D, 1.5D, 0.012D);
        level.sendParticles(ParticleTypes.SOUL_FIRE_FLAME, player.getX(), player.getY() + 0.3D, player.getZ(),
                12 + levelValue * 3, 1.2D, 0.5D, 1.2D, 0.01D);
        level.sendParticles(ParticleTypes.SMOKE, player.getX(), player.getY() + 0.15D, player.getZ(),
                30 + wave * 3, 1.6D, 0.25D, 1.6D, 0.01D);
        level.sendParticles(ParticleTypes.ENCHANT, player.getX(), player.getY() + 1.0D, player.getZ(),
                14 + levelValue * 2, 1.0D, 0.6D, 1.0D, 0.01D);
        level.sendParticles(ParticleTypes.SCULK_SOUL, player.getX(), player.getY() + 0.6D, player.getZ(),
                12 + levelValue * 2, 1.1D, 0.7D, 1.1D, 0.008D);
        level.sendParticles(ModParticles.UNDEAD_INVASION.get(), player.getX(), player.getY() + 0.7D, player.getZ(),
                16 + levelValue * 2, 0.95D, 0.6D, 0.95D, 0.01D);
        if (wave >= Math.max(3, totalWaves - 1)) {
            level.sendParticles(ParticleTypes.REVERSE_PORTAL, player.getX(), player.getY() + 0.9D, player.getZ(),
                    18 + levelValue * 2, 1.0D, 0.8D, 1.0D, 0.015D);
        }
    }

    private static void clearInvadersFor(ServerLevel level, UUID owner) {
        for (Mob mob : level.getEntitiesOfClass(Mob.class, LEVEL_WIDE_BOX)) {
            UUID tagged = getOwnerTag(mob);
            if (tagged != null && tagged.equals(owner)) {
                mob.discard();
            }
        }
        for (ArmorStand stand : level.getEntitiesOfClass(ArmorStand.class, LEVEL_WIDE_BOX)) {
            if (stand.getPersistentData().hasUUID(TAG_REWARD_MARKER) && owner.equals(stand.getPersistentData().getUUID(TAG_REWARD_MARKER))) {
                stand.discard();
            }
        }
    }

    /**
     * Coffre de victoire d’invasion (même contenu aléatoire que la fin normale), pour tests / commande admin.
     *
     * @param invasionLevel palier 1–6 (quantité et qualité du loot)
     */
    public static void spawnVictoryRewardChestForCommand(ServerLevel level, ServerPlayer player, int invasionLevel) {
        spawnRewardChest(level, player, Mth.clamp(invasionLevel, 1, 6), false);
    }

    private static void spawnRewardChest(ServerLevel level, ServerPlayer player, int invasionLevel, boolean secretWaveWon) {
        BlockPos origin = player.blockPosition();
        BlockPos chestPos = origin;
        for (int i = 0; i < 20; i++) {
            double angle = level.random.nextDouble() * Mth.TWO_PI;
            int x = Mth.floor(origin.getX() + Math.cos(angle) * (4 + level.random.nextInt(8)));
            int z = Mth.floor(origin.getZ() + Math.sin(angle) * (4 + level.random.nextInt(8)));
            int y = level.getHeight(net.minecraft.world.level.levelgen.Heightmap.Types.MOTION_BLOCKING_NO_LEAVES, x, z);
            BlockPos candidate = new BlockPos(x, y, z);
            if (!level.getBlockState(candidate.below()).isAir() && level.getBlockState(candidate).canBeReplaced()) {
                chestPos = candidate;
                break;
            }
        }

        level.setBlockAndUpdate(chestPos, Blocks.CHEST.defaultBlockState());
        if (level.getBlockEntity(chestPos) instanceof ChestBlockEntity chest) {
            fillRewardChest(chest, invasionLevel, level.random, secretWaveWon);
            chest.setChanged();
        }

        ArmorStand marker = EntityType.ARMOR_STAND.create(level);
        if (marker != null) {
            marker.moveTo(chestPos.getX() + 0.5D, chestPos.getY() + 0.1D, chestPos.getZ() + 0.5D, 0.0F, 0.0F);
            marker.setInvisible(true);
            marker.setNoGravity(true);
            marker.setGlowingTag(true);
            marker.setCustomNameVisible(true);
            marker.setCustomName(Component.translatable("message.bmcmod.undead_invasion.reward_marker"));
            marker.getPersistentData().putUUID(TAG_REWARD_MARKER, player.getUUID());
            marker.getPersistentData().putLong(TAG_REWARD_MARKER_EXPIRE, level.getGameTime() + REWARD_MARKER_LIFETIME_TICKS);
            level.addFreshEntity(marker);
        }

        level.sendParticles(ParticleTypes.HAPPY_VILLAGER, chestPos.getX() + 0.5D, chestPos.getY() + 1.0D, chestPos.getZ() + 0.5D, 45, 0.45D, 0.65D, 0.45D, 0.03D);
        level.playSound(null, chestPos, SoundEvents.CHEST_OPEN, SoundSource.PLAYERS, 0.9F, 1.2F);
        player.displayClientMessage(Component.translatable(
                "message.bmcmod.undead_invasion.reward_chest_coords",
                chestPos.getX(), chestPos.getY(), chestPos.getZ()), false);
    }

    private static void fillRewardChest(ChestBlockEntity chest, int invasionLevel, RandomSource random, boolean secretWaveWon) {
        chest.clearContent();
        if (!(chest.getLevel() instanceof ServerLevel)) {
            return;
        }
        int L = Mth.clamp(invasionLevel, 1, 6);
        List<ItemStack> loot = new ArrayList<>();

        addUndeadInvasionResourceBundle(loot, L, random);
        addUndeadInvasionThematicLoot(loot, L, random);
        addUndeadInvasionModLoot(loot, L, random);
        addVictoryUndeadPotionLoot(loot, L, random);
        if (random.nextFloat() < 0.06F) {
            pushLoot(loot, ModItems.UNDEAD_TOTEM.get(), 1);
        }

        int extraRolls = 5 + L * 2 + random.nextInt(4 + L);
        for (int i = 0; i < extraRolls; i++) {
            rollUndeadInvasionMiscReward(loot, L, random);
        }

        addUndeadInvasionGuaranteedHighTier(loot, L, random);
        if (secretWaveWon) {
            addSecretWaveRewardBonus(loot, random);
        }

        List<ItemStack> compacted = compactRewardStacks(loot);
        if (compacted.size() > 27) {
            compacted = new ArrayList<>(compacted.subList(0, 27));
        }
        shuffleIntoChestSlots(chest, compacted, random);
    }

    private static void addSecretWaveRewardBonus(List<ItemStack> loot, RandomSource r) {
        pushLoot(loot, Items.NETHER_STAR, 1 + r.nextInt(2));
        pushLoot(loot, Items.ENCHANTED_GOLDEN_APPLE, 1 + r.nextInt(2));
        pushLoot(loot, Items.DIAMOND_BLOCK, 1 + r.nextInt(2));
        pushLoot(loot, ModItems.ENDERITE_SCRAP.get(), 2 + r.nextInt(3));
        pushLoot(loot, ModItems.BOREAL_FRAGMENT.get(), 3 + r.nextInt(4));
        if (r.nextFloat() < 0.45F) {
            pushLoot(loot, Items.TOTEM_OF_UNDYING, 1);
        }
        int tier = Mth.clamp(5 + r.nextInt(2), 1, 6);
        pushLoot(loot, undeadBottleForTier(tier), 1 + (r.nextFloat() < 0.35F ? 1 : 0));
    }

    private static void addVictoryUndeadPotionLoot(List<ItemStack> loot, int invasionLevel, RandomSource r) {
        int minTier = Mth.clamp(invasionLevel, 1, 6);
        int maxTier = Mth.clamp(invasionLevel + 2, 1, 6);
        int count = 1 + (r.nextFloat() < (0.24F + invasionLevel * 0.08F) ? 1 : 0);
        if (invasionLevel >= 5 && r.nextFloat() < 0.28F) {
            count++;
        }
        for (int i = 0; i < count; i++) {
            int tier = minTier + r.nextInt(maxTier - minTier + 1);
            if (tier > invasionLevel && r.nextFloat() < 0.45F) {
                tier = Math.max(minTier, tier - 1);
            }
            pushLoot(loot, undeadBottleForTier(tier), 1);
        }
    }

    /** Ressources précieuses : sous-ensemble et quantités qui montent nettement avec le niveau. */
    private static void addUndeadInvasionResourceBundle(List<ItemStack> loot, int L, RandomSource r) {
        int picks = 3 + Math.min(4, L) + r.nextInt(2);
        List<Runnable> pool = new ArrayList<>();
        pool.add(() -> pushLoot(loot, Items.DIAMOND, 1 + L + r.nextInt(2 + L * 2)));
        pool.add(() -> pushLoot(loot, Items.EMERALD, 3 + L * 2 + r.nextInt(3 + L * 2)));
        pool.add(() -> pushLoot(loot, Items.GOLD_INGOT, 4 + L * 2 + r.nextInt(5 + L)));
        pool.add(() -> pushLoot(loot, Items.IRON_INGOT, 6 + L * 4 + r.nextInt(8 + L * 3)));
        pool.add(() -> pushLoot(loot, Items.LAPIS_LAZULI, 4 + L * 3 + r.nextInt(6 + L * 2)));
        pool.add(() -> pushLoot(loot, Items.REDSTONE, 6 + L * 4 + r.nextInt(10 + L * 2)));
        pool.add(() -> pushLoot(loot, Items.COAL, 10 + L * 8 + r.nextInt(16 + L * 4)));
        pool.add(() -> pushLoot(loot, Items.RAW_COPPER, 8 + L * 5 + r.nextInt(12)));
        pool.add(() -> pushLoot(loot, Items.COPPER_INGOT, 4 + L * 2 + r.nextInt(6 + L)));
        pool.add(() -> pushLoot(loot, ModItems.COPPER_NUGGET.get(), 10 + L * 6 + r.nextInt(14 + L * 4)));
        pool.add(() -> pushLoot(loot, Items.EXPERIENCE_BOTTLE, 4 + L * 3 + r.nextInt(6 + L * 4)));
        pool.add(() -> pushLoot(loot, Items.GOLDEN_CARROT, 2 + L + r.nextInt(4 + L)));
        pool.add(() -> pushLoot(loot, Items.GLOWSTONE_DUST, 4 + L * 2 + r.nextInt(6)));
        if (L >= 3) {
            pool.add(() -> pushLoot(loot, Items.DIAMOND_BLOCK, r.nextInt(Math.min(2, 1 + L / 3)) + (L >= 5 && r.nextFloat() < 0.35F ? 1 : 0)));
        }
        if (L >= 4) {
            pool.add(() -> pushLoot(loot, Items.EMERALD_BLOCK, r.nextInt(2) + (r.nextFloat() < 0.25F ? 1 : 0)));
        }
        Collections.shuffle(pool, new Random(r.nextLong()));
        for (int i = 0; i < picks && i < pool.size(); i++) {
            pool.get(i).run();
        }
    }

    private static void addUndeadInvasionThematicLoot(List<ItemStack> loot, int L, RandomSource r) {
        pushLoot(loot, Items.ROTTEN_FLESH, 6 + L * 5 + r.nextInt(12 + L * 4));
        pushLoot(loot, Items.BONE, 5 + L * 3 + r.nextInt(10 + L * 2));
        pushLoot(loot, Items.ARROW, 8 + L * 6 + r.nextInt(16));
        if (L >= 2 && r.nextFloat() < 0.65F + L * 0.05F) {
            pushLoot(loot, Items.SPECTRAL_ARROW, 4 + L * 2 + r.nextInt(8 + L * 2));
        }
        if (L >= 2 && r.nextFloat() < 0.5F) {
            pushLoot(loot, Items.SKELETON_SKULL, 1 + r.nextInt(Math.min(3, 1 + L / 2)));
        }
        if (L >= 3) {
            pushLoot(loot, Items.SOUL_SAND, 4 + L * 2 + r.nextInt(8));
            if (r.nextFloat() < 0.55F) {
                pushLoot(loot, Items.SOUL_SOIL, 3 + L + r.nextInt(8));
            }
            if (r.nextFloat() < 0.35F + L * 0.06F) {
                pushSingle(loot, new ItemStack(Items.SOUL_LANTERN, 1 + r.nextInt(Math.min(3, L / 2))));
            }
        }
        if (L >= 3 && r.nextFloat() < 0.4F + L * 0.08F) {
            pushLoot(loot, Items.WITHER_ROSE, 1 + r.nextInt(Math.min(4, L)));
        }
        if (L >= 3 && r.nextFloat() < 0.35F + L * 0.07F) {
            pushLoot(loot, Items.PHANTOM_MEMBRANE, 1 + r.nextInt(L));
        }
        if (L >= 4 && r.nextFloat() < 0.25F + L * 0.06F) {
            pushLoot(loot, Items.ECHO_SHARD, 1 + r.nextInt(Math.min(3, L - 2)));
        }
        if (L >= 4) {
            int skulls = Math.max(1, L - 3 + (r.nextFloat() < 0.45F ? 1 : 0));
            pushLoot(loot, Items.WITHER_SKELETON_SKULL, skulls);
        }
        if (L >= 2 && r.nextFloat() < 0.3F + L * 0.08F) {
            pushLoot(loot, Items.GUNPOWDER, 3 + L * 2 + r.nextInt(8));
        }
        if (r.nextFloat() < 0.25F + L * 0.06F) {
            pushLoot(loot, Items.SPIDER_EYE, 2 + r.nextInt(4 + L));
        }
        if (r.nextFloat() < 0.2F + L * 0.05F) {
            pushLoot(loot, Items.STRING, 4 + L * 3 + r.nextInt(12));
        }
    }

    private static void addUndeadInvasionModLoot(List<ItemStack> loot, int L, RandomSource r) {
        pushLoot(loot, ModItems.RUBY.get(), Math.max(1, L / 2) + r.nextInt(2 + L) + (L >= 5 ? r.nextInt(3) : 0));

        if (r.nextFloat() < 0.35F + L * 0.08F) {
            pushLoot(loot, ModItems.TOPAZ_SHARD.get(), 1 + r.nextInt(2 + L));
        }
        if (r.nextFloat() < 0.3F + L * 0.07F) {
            pushLoot(loot, ModItems.BERYL_SHARD.get(), 1 + r.nextInt(2 + L));
        }
        if (r.nextFloat() < 0.25F + L * 0.06F) {
            pushLoot(loot, ModItems.OPAL_SHARD.get(), 1 + r.nextInt(1 + L));
        }

        if (L >= 2 && r.nextFloat() < 0.45F + L * 0.06F) {
            int maxBottle = Math.min(6, L + r.nextInt(2));
            int tier = 1 + r.nextInt(maxBottle);
            Item bottle = switch (tier) {
                case 1 -> ModItems.UNDEAD_BOTTLE_1.get();
                case 2 -> ModItems.UNDEAD_BOTTLE_2.get();
                case 3 -> ModItems.UNDEAD_BOTTLE_3.get();
                case 4 -> ModItems.UNDEAD_BOTTLE_4.get();
                case 5 -> ModItems.UNDEAD_BOTTLE_5.get();
                default -> ModItems.UNDEAD_BOTTLE_6.get();
            };
            pushLoot(loot, bottle, 1 + (r.nextFloat() < 0.2F + L * 0.04F ? 1 : 0));
        }

        if (L >= 2 && r.nextFloat() < 0.22F + L * 0.06F) {
            pushSingle(loot, randomCopperGear(r));
        }
        if (L >= 3 && r.nextFloat() < 0.18F + L * 0.05F) {
            pushSingle(loot, randomEmeraldGear(r));
        }
        if (L >= 4 && r.nextFloat() < 0.12F + L * 0.04F) {
            pushLoot(loot, ModItems.ENDERITE_SCRAP.get(), 1 + r.nextInt(Math.min(3, L - 2)));
        }
        if (L >= 5 && r.nextFloat() < 0.18F + L * 0.05F) {
            pushLoot(loot, ModItems.BOREAL_FRAGMENT.get(), 1 + r.nextInt(2 + (L >= 6 ? 2 : 0)));
        }
        if (L >= 5 && r.nextFloat() < 0.08F + L * 0.03F) {
            pushSingle(loot, new ItemStack(ModItems.UNDEAD_CROWN.get()));
        }
        if (L >= 4 && r.nextFloat() < 0.12F) {
            pushLoot(loot, ModItems.SKELETON_VILLAGER_SKULL_ITEM.get(), 1);
        }
    }

    private static void addUndeadInvasionGuaranteedHighTier(List<ItemStack> loot, int L, RandomSource r) {
        if (L >= 4 && r.nextFloat() < 0.35F + (L - 4) * 0.2F) {
            pushLoot(loot, Items.GOLDEN_APPLE, 1 + r.nextInt(Math.min(3, L - 2)));
        }
        if (L >= 5) {
            pushLoot(loot, Items.ENCHANTED_GOLDEN_APPLE, L >= 6 || r.nextFloat() < 0.55F ? 1 : 0);
        }
        if (L >= 6) {
            pushLoot(loot, Items.NETHER_STAR, 1);
        }
        if (L >= 5 && r.nextFloat() < 0.18F + (L - 5) * 0.12F) {
            pushLoot(loot, Items.TOTEM_OF_UNDYING, 1);
        }
        if (L >= 3 && r.nextFloat() < 0.15F + L * 0.04F) {
            pushLoot(loot, Items.HONEY_BOTTLE, 1 + r.nextInt(2));
        }
        if (r.nextFloat() < 0.12F + L * 0.03F) {
            pushLoot(loot, Items.GLOW_BERRIES, 4 + r.nextInt(8 + L * 2));
        }
    }

    private static void rollUndeadInvasionMiscReward(List<ItemStack> loot, int L, RandomSource r) {
        int roll = r.nextInt(32);
        if (roll == 0) {
            pushLoot(loot, Items.AMETHYST_SHARD, 2 + r.nextInt(6 + L * 2));
        } else if (roll == 1) {
            pushLoot(loot, Items.QUARTZ, 4 + L * 2 + r.nextInt(10));
        } else if (roll == 2) {
            pushLoot(loot, Items.LEATHER, 4 + L * 3 + r.nextInt(8));
        } else if (roll == 3) {
            pushLoot(loot, Items.FLINT, 3 + r.nextInt(6 + L * 2));
        } else if (roll == 4) {
            pushLoot(loot, Items.CLAY_BALL, 6 + r.nextInt(12 + L * 2));
        } else if (roll == 5) {
            pushLoot(loot, Items.BRICK, 4 + r.nextInt(8 + L));
        } else if (roll == 6) {
            pushLoot(loot, Items.OBSIDIAN, 1 + r.nextInt(Math.min(4, 1 + L / 2)));
        } else if (roll == 7 && L >= 3) {
            pushLoot(loot, Items.CRYING_OBSIDIAN, 1 + r.nextInt(Math.min(4, L - 1)));
        } else if (roll == 8) {
            pushLoot(loot, Items.MOSS_BLOCK, 2 + r.nextInt(4 + L));
        } else if (roll == 9) {
            pushLoot(loot, Items.TORCH, 8 + L * 6 + r.nextInt(24));
        } else if (roll == 10) {
            pushLoot(loot, Items.SOUL_TORCH, 4 + L * 3 + r.nextInt(12));
        } else if (roll == 11) {
            pushLoot(loot, Items.COOKED_BEEF, 4 + L * 2 + r.nextInt(8));
        } else if (roll == 12) {
            pushLoot(loot, Items.COOKED_PORKCHOP, 4 + L * 2 + r.nextInt(8));
        } else if (roll == 13) {
            pushLoot(loot, Items.BREAD, 4 + L * 2 + r.nextInt(8));
        } else if (roll == 14) {
            pushLoot(loot, Items.PUMPKIN_PIE, 2 + r.nextInt(4 + L));
        } else if (roll == 15) {
            pushLoot(loot, Items.FIRE_CHARGE, 2 + r.nextInt(3 + L));
        } else if (roll == 16 && L >= 2) {
            pushLoot(loot, Items.TNT, 1 + r.nextInt(Math.min(4, L)));
        } else if (roll == 17) {
            pushLoot(loot, Items.FEATHER, 4 + r.nextInt(8 + L * 2));
        } else if (roll == 18) {
            pushLoot(loot, Items.SLIME_BALL, 2 + r.nextInt(4 + L));
        } else if (roll == 19 && L >= 3) {
            pushLoot(loot, Items.ENDER_PEARL, 1 + r.nextInt(Math.min(4, L - 1)));
        } else if (roll == 20) {
            pushLoot(loot, Items.BONE_MEAL, 6 + L * 4 + r.nextInt(16));
        } else if (roll == 21) {
            pushLoot(loot, Items.SUGAR, 4 + r.nextInt(8 + L * 2));
        } else if (roll == 22) {
            pushLoot(loot, Items.GLASS_BOTTLE, 2 + r.nextInt(4 + L));
        } else if (roll == 23) {
            pushLoot(loot, Items.MELON_SLICE, 4 + r.nextInt(8 + L * 2));
        } else if (roll == 24 && L >= 4) {
            pushLoot(loot, Items.DRIED_KELP_BLOCK, 1 + r.nextInt(3));
        } else if (roll == 25) {
            pushLoot(loot, Items.CANDLE, 2 + r.nextInt(4 + L));
        } else if (roll == 26) {
            pushLoot(loot, Items.IRON_NUGGET, 6 + L * 4 + r.nextInt(16));
        } else if (roll == 27) {
            pushLoot(loot, Items.GOLD_NUGGET, 6 + L * 3 + r.nextInt(14));
        } else if (roll == 28 && L >= 5) {
            pushLoot(loot, Items.NETHERITE_SCRAP, r.nextInt(2));
        } else if (roll == 29) {
            pushLoot(loot, Items.MUD, 8 + r.nextInt(16 + L * 4));
        } else {
            pushLoot(loot, Items.ROTTEN_FLESH, 3 + r.nextInt(6 + L * 2));
        }
    }

    private static ItemStack randomCopperGear(RandomSource r) {
        Item[] opts = new Item[] {
                ModItems.COPPER_SWORD.get(),
                ModItems.COPPER_PICKAXE.get(),
                ModItems.COPPER_AXE.get(),
                ModItems.COPPER_SHOVEL.get(),
                ModItems.COPPER_HOE.get(),
                ModItems.COPPER_HELMET.get(),
                ModItems.COPPER_CHESTPLATE.get(),
                ModItems.COPPER_LEGGINGS.get(),
                ModItems.COPPER_BOOTS.get()
        };
        return new ItemStack(opts[r.nextInt(opts.length)]);
    }

    private static ItemStack randomEmeraldGear(RandomSource r) {
        Item[] opts = new Item[] {
                ModItems.EMERALD_SWORD.get(),
                ModItems.EMERALD_PICKAXE.get(),
                ModItems.EMERALD_AXE.get(),
                ModItems.EMERALD_SHOVEL.get(),
                ModItems.EMERALD_HOE.get(),
                ModItems.EMERALD_HELMET.get(),
                ModItems.EMERALD_CHESTPLATE.get(),
                ModItems.EMERALD_LEGGINGS.get(),
                ModItems.EMERALD_BOOTS.get()
        };
        return new ItemStack(opts[r.nextInt(opts.length)]);
    }

    private static Item undeadBottleForTier(int tier) {
        return switch (Mth.clamp(tier, 1, 6)) {
            case 1 -> ModItems.UNDEAD_BOTTLE_1.get();
            case 2 -> ModItems.UNDEAD_BOTTLE_2.get();
            case 3 -> ModItems.UNDEAD_BOTTLE_3.get();
            case 4 -> ModItems.UNDEAD_BOTTLE_4.get();
            case 5 -> ModItems.UNDEAD_BOTTLE_5.get();
            default -> ModItems.UNDEAD_BOTTLE_6.get();
        };
    }

    private static void pushLoot(List<ItemStack> loot, Item item, int count) {
        if (count <= 0 || item == null) {
            return;
        }
        int max = item.getDefaultMaxStackSize();
        int left = count;
        while (left > 0) {
            int n = Math.min(left, max);
            loot.add(new ItemStack(item, n));
            left -= n;
        }
    }

    private static void pushSingle(List<ItemStack> loot, ItemStack stack) {
        if (stack.isEmpty()) {
            return;
        }
        loot.add(stack);
    }

    private static List<ItemStack> compactRewardStacks(List<ItemStack> loot) {
        List<ItemStack> out = new ArrayList<>();
        for (ItemStack incoming : loot) {
            if (incoming.isEmpty()) {
                continue;
            }
            ItemStack remainder = incoming.copy();
            for (ItemStack pile : out) {
                if (remainder.isEmpty()) {
                    break;
                }
                if (ItemStack.isSameItemSameComponents(pile, remainder)) {
                    int space = pile.getMaxStackSize() - pile.getCount();
                    if (space > 0) {
                        int add = Math.min(space, remainder.getCount());
                        pile.grow(add);
                        remainder.shrink(add);
                    }
                }
            }
            if (!remainder.isEmpty()) {
                out.add(remainder);
            }
        }
        return out;
    }

    private static void shuffleIntoChestSlots(ChestBlockEntity chest, List<ItemStack> stacks, RandomSource random) {
        List<Integer> slots = new ArrayList<>();
        for (int i = 0; i < 27; i++) {
            slots.add(i);
        }
        Collections.shuffle(slots, new Random(random.nextLong()));
        for (int i = 0; i < stacks.size() && i < slots.size(); i++) {
            chest.setItem(slots.get(i), stacks.get(i));
        }
    }

    private static UUID getOwnerTag(Mob mob) {
        var tag = mob.getPersistentData();
        return tag.hasUUID(TAG_INVADER_OWNER) ? tag.getUUID(TAG_INVADER_OWNER) : null;
    }

    private static int totalWaves(int level) {
        return level + 3; // 1->4, 2->5, 3->6, 4->7, 5->8, 6->9
    }

    private static void clearPersistedInvasionResume(ServerPlayer player) {
        player.getPersistentData().remove(PERSIST_ACTIVE_INVASION_KEY);
    }

    /** Vide la file de spawns retardés avant sérialisation (évite une vague gelée après reprise). */
    private static void flushPendingInvasionSpawns(InvasionState st, ServerLevel level, ServerPlayer player) {
        while (!st.pendingSpawns.isEmpty()) {
            Runnable r = st.pendingSpawns.poll();
            if (r != null) {
                try {
                    r.run();
                } catch (RuntimeException ex) {
                    BmcMod.LOGGER.warn("Undead invasion: pending spawn flush failed during resume save", ex);
                }
            }
        }
    }

    /** Écrit {@link #PERSIST_ACTIVE_INVASION_KEY} sur le joueur (sauvé avec le monde). */
    private static void writeInvasionResumeToPlayer(ServerPlayer sp) {
        InvasionState st = ACTIVE.get(sp.getUUID());
        if (st == null) {
            clearPersistedInvasionResume(sp);
            return;
        }
        if (sp.level() instanceof ServerLevel sl) {
            flushPendingInvasionSpawns(st, sl, sp);
        }
        sp.getPersistentData().put(PERSIST_ACTIVE_INVASION_KEY, encodeInvasionResume(st, sp));
    }

    private static CompoundTag encodeInvasionResume(InvasionState st, ServerPlayer sp) {
        CompoundTag t = new CompoundTag();
        t.putString("dimension", sp.serverLevel().dimension().location().toString());
        t.putInt("invasion_tier", st.level);
        t.putInt("total_waves", st.totalWaves);
        t.putBoolean("secret_10", st.secretTenthWave);
        t.putByte("phase", (byte) st.phase.ordinal());
        t.putInt("wave", st.wave);
        t.putLong("next_tick", st.nextTick);
        t.putLong("wave_phase_start", st.wavePhaseStartGameTime);
        t.putLong("next_spawn", st.nextSpawnGameTime);
        t.putInt("spawned", st.waveSpawnedCount);
        t.putInt("expected", st.waveExpectedTotal);
        t.putInt("combat_cap", st.combatCap);
        t.putInt("mini_wave", st.miniWaveSize);
        t.putBoolean("sec_defeated", st.secretBossDefeated);
        if (st.secretBossId != null) {
            t.putUUID("secret_boss", st.secretBossId);
        }
        return t;
    }

    private static void tryRestoreInvasionFromPlayer(ServerPlayer sp, ServerLevel sl) {
        if (!sp.getPersistentData().contains(PERSIST_ACTIVE_INVASION_KEY, Tag.TAG_COMPOUND)) {
            return;
        }
        CompoundTag t = sp.getPersistentData().getCompound(PERSIST_ACTIVE_INVASION_KEY);
        String dim = t.getString("dimension");
        if (!sl.dimension().location().toString().equals(dim)) {
            clearPersistedInvasionResume(sp);
            return;
        }
        int tier = Mth.clamp(t.getInt("invasion_tier"), 1, 6);
        int totalWaves = Mth.clamp(t.getInt("total_waves"), 1, 32);
        boolean secret10 = t.getBoolean("secret_10");
        int phaseOrd = Mth.clamp(t.getByte("phase"), 0, Phase.values().length - 1);
        Phase phase = Phase.values()[phaseOrd];
        long nextTick = t.getLong("next_tick");
        ServerBossEvent boss = new ServerBossEvent(
                Component.translatable("event.bmcmod.undead_invasion.title", tier),
                BossEvent.BossBarColor.YELLOW,
                BossEvent.BossBarOverlay.NOTCHED_10);
        boss.addPlayer(sp);
        InvasionState st = new InvasionState(tier, totalWaves, secret10, nextTick, phase, boss);
        st.wave = Math.max(0, t.getInt("wave"));
        st.wavePhaseStartGameTime = t.getLong("wave_phase_start");
        st.nextSpawnGameTime = t.getLong("next_spawn");
        st.waveSpawnedCount = t.getInt("spawned");
        st.waveExpectedTotal = t.getInt("expected");
        if (t.contains("combat_cap")) {
            st.combatCap = Mth.clamp(t.getInt("combat_cap"), 1, 256);
        }
        if (t.contains("mini_wave")) {
            st.miniWaveSize = Mth.clamp(t.getInt("mini_wave"), 1, 256);
        }
        if (st.waveExpectedTotal > 0) {
            RaidWaveBossBar.applyWaveStyle(st.bossEvent, st.waveExpectedTotal, st.miniWaveSize);
        }
        st.secretBossDefeated = t.getBoolean("sec_defeated");
        if (t.hasUUID("secret_boss")) {
            UUID sid = t.getUUID("secret_boss");
            Entity ent = sl.getEntity(sid);
            if (ent instanceof LivingEntity living && living.isAlive()) {
                st.secretBossId = sid;
                ServerBossEvent redBoss = new ServerBossEvent(
                        Component.literal("The Undead Boss").withStyle(ChatFormatting.RED, ChatFormatting.BOLD),
                        BossEvent.BossBarColor.RED,
                        BossEvent.BossBarOverlay.PROGRESS);
                redBoss.addPlayer(sp);
                redBoss.setProgress(Mth.clamp(living.getHealth() / Math.max(1.0F, living.getMaxHealth()), 0.0F, 1.0F));
                st.secretBossEvent = redBoss;
            }
        }
        rebuildInvaderSetFromWorld(sl, sp.getUUID(), st);
        ACTIVE.put(sp.getUUID(), st);
        clearPersistedInvasionResume(sp);
        sp.displayClientMessage(Component.translatable("message.bmcmod.undead_invasion.resumed"), true);
    }

    private static void rebuildInvaderSetFromWorld(ServerLevel level, UUID owner, InvasionState state) {
        state.aliveInvaders.clear();
        for (LivingEntity liv : level.getEntitiesOfClass(LivingEntity.class, LEVEL_WIDE_BOX)) {
            if (!liv.isAlive()) {
                continue;
            }
            CompoundTag pd = liv.getPersistentData();
            if (!pd.hasUUID(TAG_INVADER_OWNER)) {
                continue;
            }
            if (owner.equals(pd.getUUID(TAG_INVADER_OWNER))) {
                state.aliveInvaders.add(liv.getUUID());
            }
        }
    }

    private static void purgeInvasionFromMemory(UUID id) {
        PREP.remove(id);
        InvasionState st = ACTIVE.remove(id);
        if (st != null) {
            st.bossEvent.removeAllPlayers();
            if (st.secretBossEvent != null) {
                st.secretBossEvent.removeAllPlayers();
            }
        }
    }

    private enum Phase {
        CHARGING,
        WAVE,
        INTER_WAVE
    }

    private static final class PrepState {
        int level;
        long effectEndTick;

        PrepState(int level, long effectEndTick) {
            this.level = level;
            this.effectEndTick = effectEndTick;
        }
    }

    private static final class InvasionState {
        final int level;
        final int totalWaves;
        final boolean secretTenthWave;
        final ServerBossEvent bossEvent;
        @Nullable
        ServerBossEvent secretBossEvent;
        @Nullable
        UUID secretBossId;
        boolean secretBossDefeated;
        final Set<UUID> aliveInvaders = new HashSet<>();
        final ArrayDeque<Runnable> pendingSpawns = new ArrayDeque<>();
        Phase phase;
        int wave = 0;
        long nextTick;
        /** Tick monde où la vague actuelle ({@link Phase#WAVE}) a commencé ; sert au hint glow après retard. */
        long wavePhaseStartGameTime;
        long nextSpawnGameTime;
        int waveSpawnedCount;
        int waveExpectedTotal;
        int combatCap = 6;
        int miniWaveSize = 5;

        InvasionState(int level, int totalWaves, boolean secretTenthWave, long nextTick, Phase phase, ServerBossEvent bossEvent) {
            this.level = level;
            this.totalWaves = totalWaves;
            this.secretTenthWave = secretTenthWave;
            this.nextTick = nextTick;
            this.phase = phase;
            this.bossEvent = bossEvent;
        }
    }
}

