package com.soleapixel.bmcmod;

import net.neoforged.neoforge.common.ModConfigSpec;

/**
 * Gameplay tunables (file {@code bmcmod-server.toml} / world {@code serverconfig}).
 * <p>
 * Applies only on the logical server. Restart the world or dedicated server after edits so values reload.
 * On multiplayer, only the host's server files affect gameplay; remote clients do not push settings to the server.
 */
public final class ServerGameplayConfig {
    private static final ModConfigSpec.Builder BUILDER = new ModConfigSpec.Builder();

    private static final ModConfigSpec.DoubleValue END_HIGH_TIER_WEIGHT_MULTIPLIER;
    private static final ModConfigSpec.IntValue BOSS_WEIGHT_COMMON;
    private static final ModConfigSpec.IntValue BOSS_WEIGHT_UNCOMMON;
    private static final ModConfigSpec.IntValue BOSS_WEIGHT_RARE;
    private static final ModConfigSpec.IntValue BOSS_WEIGHT_EPIC;
    private static final ModConfigSpec.IntValue BOSS_WEIGHT_LEGENDARY;
    private static final ModConfigSpec.IntValue BOSS_WEIGHT_MYTHIC;
    private static final ModConfigSpec.DoubleValue BOSS_NATURAL_SPAWN_CHANCE;
    private static final ModConfigSpec.IntValue BOSS_NATURAL_CHECK_INTERVAL_TICKS;
    private static final ModConfigSpec.IntValue QUEST_TRADER_TICK_INTERVAL;
    private static final ModConfigSpec.DoubleValue QUEST_TRADER_NEAR_VILLAGE;
    private static final ModConfigSpec.DoubleValue QUEST_TRADER_WILD;
    private static final ModConfigSpec.DoubleValue QUEST_TRADER_QUEST_VARIANT_CHANCE;
    private static final ModConfigSpec.DoubleValue UNDEAD_INVASION_THUNDER_TRIGGER_CHANCE;
    private static final ModConfigSpec.IntValue UNDEAD_INVASION_RANDOM_TRIGGER_COOLDOWN_TICKS;
    private static final ModConfigSpec.DoubleValue UNDEAD_INVASION_SECRET_WAVE_CHANCE_LEVEL_6;
    private static final ModConfigSpec.BooleanValue SV_PATROL_ENABLED;
    private static final ModConfigSpec.IntValue SV_PATROL_TICK_INTERVAL;
    private static final ModConfigSpec.DoubleValue SV_PATROL_SPAWN_CHANCE_PER_PLAYER;
    private static final ModConfigSpec.IntValue SV_PATROL_GROUP_SIZE_MIN;
    private static final ModConfigSpec.IntValue SV_PATROL_GROUP_SIZE_MAX;
    private static final ModConfigSpec.DoubleValue SV_PATROL_FULL_WIPE_POTION_CHANCE;

    public static final ModConfigSpec SPEC;

    static {
        BUILDER.comment(
                "Server-side gameplay. Restart the world or dedicated server after changing values.",
                "On multiplayer, gameplay uses the server's config files only."
        );

        BUILDER.push("miniboss_rarity");
        END_HIGH_TIER_WEIGHT_MULTIPLIER = BUILDER
                .comment("Multiplier applied to Rare/Epic/Legendary/Mythic weights when the anchor player is in the End.")
                .defineInRange("endHighTierWeightMultiplier", 1.3D, 0.1D, 10.0D);
        BOSS_WEIGHT_COMMON = BUILDER.defineInRange("weightCommon", 38, 0, 100_000);
        BOSS_WEIGHT_UNCOMMON = BUILDER.defineInRange("weightUncommon", 16, 0, 100_000);
        BOSS_WEIGHT_RARE = BUILDER.defineInRange("weightRare", 24, 0, 100_000);
        BOSS_WEIGHT_EPIC = BUILDER.defineInRange("weightEpic", 15, 0, 100_000);
        BOSS_WEIGHT_LEGENDARY = BUILDER.defineInRange("weightLegendary", 7, 0, 100_000);
        BOSS_WEIGHT_MYTHIC = BUILDER.defineInRange("weightMythic", 5, 0, 100_000);
        BUILDER.pop();

        BUILDER.push("miniboss_spawn");
        BOSS_NATURAL_SPAWN_CHANCE = BUILDER
                .comment("Each check interval, probability (0-1) that a natural miniboss attempt runs for a random eligible player.")
                .defineInRange("naturalSpawnChancePerAttempt", 0.085D, 0.0D, 1.0D);
        BOSS_NATURAL_CHECK_INTERVAL_TICKS = BUILDER
                .comment("Ticks between natural miniboss spawn checks (20 ticks = 1 second).")
                .defineInRange("naturalSpawnCheckIntervalTicks", 1800, 20, 600_000);
        BUILDER.pop();

        BUILDER.push("quest_trader");
        QUEST_TRADER_TICK_INTERVAL = BUILDER
                .comment("Ticks between Quest / Wandering trader spawn attempts per overworld player.")
                .defineInRange("tickInterval", 1200, 20, 600_000);
        QUEST_TRADER_NEAR_VILLAGE = BUILDER
                .comment("Per-attempt chance (0-1) when near a village (2+ villagers in range).")
                .defineInRange("spawnChanceNearVillage", 0.14D, 0.0D, 1.0D);
        QUEST_TRADER_WILD = BUILDER
                .comment("Per-attempt chance (0-1) when not near a village.")
                .defineInRange("spawnChanceWild", 0.035D, 0.0D, 1.0D);
        QUEST_TRADER_QUEST_VARIANT_CHANCE = BUILDER
                .comment("When a trader spawns, probability (0-1) that it is the Quest Trader variant.")
                .defineInRange("questVariantChance", 0.5D, 0.0D, 1.0D);
        BUILDER.pop();

        BUILDER.push("undead_invasion");
        UNDEAD_INVASION_THUNDER_TRIGGER_CHANCE = BUILDER
                .comment("Per eligible tick (thunder + sky visible + off cooldown), chance to start random invasion prep.")
                .defineInRange("thunderTriggerChance", 0.00055D, 0.0D, 1.0D);
        UNDEAD_INVASION_RANDOM_TRIGGER_COOLDOWN_TICKS = BUILDER
                .comment("Ticks after a random thunder trigger before it can roll again for that player.")
                .defineInRange("randomTriggerCooldownTicks", 20 * 60 * 15, 0, 2_000_000);
        UNDEAD_INVASION_SECRET_WAVE_CHANCE_LEVEL_6 = BUILDER
                .comment("During invasion level 6, per-wave chance for the secret wave variant.")
                .defineInRange("secretWaveChanceLevel6", 0.075D, 0.0D, 1.0D);
        BUILDER.pop();

        BUILDER.push("skeleton_villager_patrol");
        SV_PATROL_ENABLED = BUILDER
                .comment("Spawn occasional Skeleton Villager patrols in the Overworld (survival players).")
                .define("enabled", true);
        SV_PATROL_TICK_INTERVAL = BUILDER
                .comment("Server ticks between patrol spawn attempts (one roll per survival Overworld player).")
                .defineInRange("tickInterval", 900, 40, 600_000);
        SV_PATROL_SPAWN_CHANCE_PER_PLAYER = BUILDER
                .comment("Per-attempt chance (0-1) for each eligible player to receive a patrol spawn try.")
                .defineInRange("spawnChancePerPlayer", 0.014D, 0.0D, 1.0D);
        SV_PATROL_GROUP_SIZE_MIN = BUILDER
                .defineInRange("groupSizeMin", 3, 1, 16);
        SV_PATROL_GROUP_SIZE_MAX = BUILDER
                .defineInRange("groupSizeMax", 4, 1, 16);
        SV_PATROL_FULL_WIPE_POTION_CHANCE = BUILDER
                .comment("When a player kills the last member of a patrol, chance (0-1) to drop one Undead Invasion potion (tier 1-3).")
                .defineInRange("fullWipeUndeadPotionChance", 0.12D, 0.0D, 1.0D);
        BUILDER.pop();

        SPEC = BUILDER.build();
    }

    private ServerGameplayConfig() {
    }

    public static double endHighTierWeightMultiplier() {
        return END_HIGH_TIER_WEIGHT_MULTIPLIER.get();
    }

    public static int bossWeightCommon() {
        return BOSS_WEIGHT_COMMON.get();
    }

    public static int bossWeightUncommon() {
        return BOSS_WEIGHT_UNCOMMON.get();
    }

    public static int bossWeightRare() {
        return BOSS_WEIGHT_RARE.get();
    }

    public static int bossWeightEpic() {
        return BOSS_WEIGHT_EPIC.get();
    }

    public static int bossWeightLegendary() {
        return BOSS_WEIGHT_LEGENDARY.get();
    }

    public static int bossWeightMythic() {
        return BOSS_WEIGHT_MYTHIC.get();
    }

    public static float bossNaturalSpawnChance() {
        return BOSS_NATURAL_SPAWN_CHANCE.get().floatValue();
    }

    public static int bossNaturalCheckIntervalTicks() {
        return BOSS_NATURAL_CHECK_INTERVAL_TICKS.get();
    }

    public static int questTraderTickInterval() {
        return QUEST_TRADER_TICK_INTERVAL.get();
    }

    public static float questTraderSpawnChanceNearVillage() {
        return QUEST_TRADER_NEAR_VILLAGE.get().floatValue();
    }

    public static float questTraderSpawnChanceWild() {
        return QUEST_TRADER_WILD.get().floatValue();
    }

    public static float questTraderQuestVariantChance() {
        return QUEST_TRADER_QUEST_VARIANT_CHANCE.get().floatValue();
    }

    public static float undeadInvasionThunderTriggerChance() {
        return UNDEAD_INVASION_THUNDER_TRIGGER_CHANCE.get().floatValue();
    }

    public static int undeadInvasionRandomTriggerCooldownTicks() {
        return UNDEAD_INVASION_RANDOM_TRIGGER_COOLDOWN_TICKS.get();
    }

    public static float undeadInvasionSecretWaveChanceLevel6() {
        return UNDEAD_INVASION_SECRET_WAVE_CHANCE_LEVEL_6.get().floatValue();
    }

    public static boolean svPatrolEnabled() {
        return SV_PATROL_ENABLED.get();
    }

    public static int svPatrolTickInterval() {
        return SV_PATROL_TICK_INTERVAL.get();
    }

    public static float svPatrolSpawnChancePerPlayer() {
        return SV_PATROL_SPAWN_CHANCE_PER_PLAYER.get().floatValue();
    }

    public static int svPatrolGroupSizeMin() {
        return SV_PATROL_GROUP_SIZE_MIN.get();
    }

    public static int svPatrolGroupSizeMax() {
        return SV_PATROL_GROUP_SIZE_MAX.get();
    }

    public static float svPatrolFullWipePotionChance() {
        return SV_PATROL_FULL_WIPE_POTION_CHANCE.get().floatValue();
    }
}
