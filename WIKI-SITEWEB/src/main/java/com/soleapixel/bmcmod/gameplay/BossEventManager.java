package com.soleapixel.bmcmod.gameplay;

import java.util.List;
import java.util.Locale;

import com.soleapixel.bmcmod.BmcMod;
import com.soleapixel.bmcmod.ServerGameplayConfig;

import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.core.HolderGetter;
import net.minecraft.core.component.DataComponents;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.server.MinecraftServer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.util.Mth;
import net.minecraft.util.RandomSource;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.monster.AbstractSkeleton;
import net.minecraft.world.entity.monster.Pillager;
import net.minecraft.world.entity.monster.Zombie;
import net.minecraft.world.entity.monster.ZombifiedPiglin;
import net.minecraft.world.item.ArmorItem;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.armortrim.ArmorTrim;
import net.minecraft.world.item.armortrim.TrimMaterial;
import net.minecraft.world.item.armortrim.TrimMaterials;
import net.minecraft.world.item.armortrim.TrimPattern;
import net.minecraft.world.item.armortrim.TrimPatterns;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.storage.loot.LootParams;
import net.minecraft.world.level.storage.loot.LootTable;
import net.minecraft.world.level.storage.loot.parameters.LootContextParamSets;
import net.minecraft.world.level.storage.loot.parameters.LootContextParams;
import net.minecraft.world.phys.Vec3;
import net.minecraft.world.scores.PlayerTeam;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.tick.ServerTickEvent;
import net.neoforged.neoforge.event.entity.living.LivingDropsEvent;

/**
 * Miniboss « Boss Event » : spawn aléatoire (Overworld / Nether / End), raretés, équipe glow, loot tables.
 */
@EventBusSubscriber(modid = BmcMod.MODID)
public final class BossEventManager {
    public static final String TAG_BOSS_EVENT = "bmcmod_boss_event";
    /** Joueur cible + autres joueurs dans ce rayon voient entendent la même apparition (un seul boss). */
    private static final double APPEAR_NOTICE_RADIUS_SQ = 100.0 * 100.0;

    private static final String[] NAME_KEYS = new String[24];

    static {
        for (int i = 0; i < NAME_KEYS.length; i++) {
            NAME_KEYS[i] = "bmcmod.bossevent.name." + i;
        }
    }

    private BossEventManager() {}

    /**
     * Spawn public pour la commande admin : même logique que le spawn naturel (distance, stats, annonce).
     */
    public static void spawnBossForCommand(ServerLevel level, ServerPlayer anchor, BossEventRarity rarity, RandomSource random) {
        spawnBossEventMob(level, anchor, rarity, random, true);
    }

    @SubscribeEvent
    public static void onServerTick(ServerTickEvent.Post event) {
        var server = event.getServer();
        int interval = ServerGameplayConfig.bossNaturalCheckIntervalTicks();
        if (interval <= 0 || server.getTickCount() % interval != 0) {
            return;
        }
        RandomSource random = server.overworld().getRandom();
        if (random.nextFloat() > ServerGameplayConfig.bossNaturalSpawnChance()) {
            return;
        }
        List<ServerPlayer> candidates = server.getPlayerList().getPlayers().stream()
                .filter(p -> !p.isSpectator())
                .filter(BossEventManager::dimensionAllowed)
                .toList();
        if (candidates.isEmpty()) {
            return;
        }
        ServerPlayer target = candidates.get(random.nextInt(candidates.size()));
        ServerLevel level = target.serverLevel();
        if (!BossEventGameRules.allowsBossSpawns(level)) {
            return;
        }
        boolean inEnd = level.dimension() == Level.END;
        BossEventRarity rarity = BossEventRarity.roll(random, inEnd);
        spawnBossEventMob(level, target, rarity, random, true);
    }

    private static boolean dimensionAllowed(ServerPlayer p) {
        ResourceKey<Level> d = p.level().dimension();
        return d == Level.OVERWORLD || d == Level.NETHER || d == Level.END;
    }

    private static void spawnBossEventMob(ServerLevel level, ServerPlayer player, BossEventRarity rarity, RandomSource random, boolean announce) {
        EntityType<? extends Mob> type = pickMobType(level, random);
        Mob mob = type.create(level);
        if (mob == null) {
            return;
        }
        Vec3 pos = findBossSpawnPos(level, player, random);
        mob.moveTo(pos.x, pos.y, pos.z, random.nextFloat() * 360.0F, 0.0F);
        mob.setPersistenceRequired();
        mob.setCanPickUpLoot(false);

        double mul = rarity.statMultiplier();
        if (mob.getAttribute(Attributes.MAX_HEALTH) != null) {
            mob.getAttribute(Attributes.MAX_HEALTH).setBaseValue(28.0D * mul);
            mob.setHealth(mob.getMaxHealth());
        }
        if (mob.getAttribute(Attributes.ATTACK_DAMAGE) != null) {
            mob.getAttribute(Attributes.ATTACK_DAMAGE).setBaseValue(3.5D * mul);
        }
        if (mob.getAttribute(Attributes.MOVEMENT_SPEED) != null) {
            mob.getAttribute(Attributes.MOVEMENT_SPEED).setBaseValue(0.235D + mul * 0.048D);
        }
        boolean armored = tryApplyBossArmor(mob, rarity, random);
        if (mob.getAttribute(Attributes.ARMOR) != null) {
            if (armored) {
                mob.getAttribute(Attributes.ARMOR).setBaseValue(0.0D);
            } else {
                mob.getAttribute(Attributes.ARMOR).setBaseValue(4.0D + (mul - 1.0D) * 8.5D);
            }
        }

        mob.getPersistentData().putString(TAG_BOSS_EVENT, rarity.name());

        Component name = Component.translatable(NAME_KEYS[random.nextInt(NAME_KEYS.length)])
                .withStyle(rarity.formatting(), ChatFormatting.BOLD);
        mob.setCustomName(name);
        mob.setCustomNameVisible(true);

        mob.setGlowingTag(true);
        applyGlowTeam(level, mob, rarity);

        mob.setTarget(player);

        if (!level.addFreshEntity(mob)) {
            return;
        }

        if (announce) {
            int x = BlockPos.containing(pos).getX();
            int y = BlockPos.containing(pos).getY();
            int z = BlockPos.containing(pos).getZ();
            Component msg = Component.literal("[").append(name).append(Component.literal("] ")).append(
                    Component.translatable("bmcmod.bossevent.appeared", x, y, z));
            sendBossAppearNoticeToNearbyAnchored(level, player, random, msg);
        }
    }

    /** Message + son : joueur dont le miniboss prend la trace, plus coéquipiers à ≤100 blocs (même dimension). */
    private static void sendBossAppearNoticeToNearbyAnchored(ServerLevel level, ServerPlayer anchor, RandomSource random, Component msg) {
        float pitch = 0.82F + random.nextFloat() * 0.12F;
        ResourceKey<Level> dim = level.dimension();
        MinecraftServer server = level.getServer();
        for (ServerPlayer p : server.getPlayerList().getPlayers()) {
            if (p.level().dimension() != dim) {
                continue;
            }
            if (p != anchor && anchor.distanceToSqr(p) > APPEAR_NOTICE_RADIUS_SQ) {
                continue;
            }
            p.sendSystemMessage(msg);
            p.playSound(SoundEvents.END_PORTAL_SPAWN, 0.92F, pitch);
        }
    }

    private static EntityType<? extends Mob> pickMobType(ServerLevel level, RandomSource r) {
        ResourceKey<Level> dim = level.dimension();
        if (dim == Level.NETHER) {
            return switch (r.nextInt(5)) {
                case 0 -> EntityType.WITHER_SKELETON;
                case 1 -> EntityType.BLAZE;
                case 2 -> EntityType.PIGLIN_BRUTE;
                case 3 -> EntityType.HOGLIN;
                default -> EntityType.ZOMBIFIED_PIGLIN;
            };
        }
        if (dim == Level.END) {
            return r.nextBoolean() ? EntityType.ENDERMAN : EntityType.ENDERMITE;
        }
        return switch (r.nextInt(8)) {
            case 0 -> EntityType.ZOMBIE;
            case 1 -> EntityType.SKELETON;
            case 2 -> EntityType.SPIDER;
            case 3 -> EntityType.CREEPER;
            case 4 -> EntityType.HUSK;
            case 5 -> EntityType.STRAY;
            case 6 -> EntityType.PILLAGER;
            default -> EntityType.WITCH;
        };
    }

    private static Vec3 findBossSpawnPos(ServerLevel level, ServerPlayer player, RandomSource r) {
        Vec3 base = player.position();
        for (int i = 0; i < 28; i++) {
            double angle = r.nextDouble() * Mth.TWO_PI;
            double radius = 50.0D + r.nextDouble() * 200.0D;
            int x = Mth.floor(base.x + Math.cos(angle) * radius);
            int z = Mth.floor(base.z + Math.sin(angle) * radius);
            int y = level.getHeight(net.minecraft.world.level.levelgen.Heightmap.Types.MOTION_BLOCKING_NO_LEAVES, x, z);
            BlockPos pos = new BlockPos(x, y, z);
            if (!level.getBlockState(pos.below()).isAir() && level.getBlockState(pos).getCollisionShape(level, pos).isEmpty()) {
                return new Vec3(x + 0.5, y + 0.1, z + 0.5);
            }
        }
        return player.position().add(32.0, 1.0, 0.0);
    }

    private static void applyGlowTeam(ServerLevel level, Mob mob, BossEventRarity rarity) {
        String teamId = "bmcmod_boss_" + rarity.name().toLowerCase(Locale.ROOT);
        var board = level.getScoreboard();
        PlayerTeam team = board.getPlayerTeam(teamId);
        if (team == null) {
            team = board.addPlayerTeam(teamId);
        }
        team.setColor(rarity.teamGlowColor());
        board.addPlayerToTeam(mob.getStringUUID(), team);
    }

    @SubscribeEvent
    public static void onLivingDrops(LivingDropsEvent event) {
        LivingEntity entity = event.getEntity();
        if (!(entity instanceof Mob mob) || mob.level().isClientSide()) {
            return;
        }
        if (!(mob.level() instanceof ServerLevel sl)) {
            return;
        }
        String tag = mob.getPersistentData().getString(TAG_BOSS_EVENT);
        if (tag.isEmpty()) {
            return;
        }
        BossEventRarity rarity = parseRarityTag(tag);
        if (rarity == null) {
            return;
        }
        ResourceKey<LootTable> lootKey = ResourceKey.create(
                Registries.LOOT_TABLE,
                BmcMod.loc("entities/boss_event/" + rarity.name().toLowerCase(Locale.ROOT)));
        LootTable table = sl.getServer().reloadableRegistries().getLootTable(lootKey);
        DamageSource damageSource = event.getSource() != null ? event.getSource() : mob.damageSources().generic();
        LootParams.Builder builder = new LootParams.Builder(sl)
                .withParameter(LootContextParams.THIS_ENTITY, mob)
                .withParameter(LootContextParams.ORIGIN, mob.position())
                .withParameter(LootContextParams.DAMAGE_SOURCE, damageSource);
        Entity killer = damageSource.getEntity();
        if (killer != null) {
            builder.withOptionalParameter(LootContextParams.ATTACKING_ENTITY, killer);
        }
        if (killer instanceof Player p) {
            builder.withOptionalParameter(LootContextParams.LAST_DAMAGE_PLAYER, p);
        }
        LootParams params = builder.create(LootContextParamSets.ENTITY);
        for (ItemStack stack : table.getRandomItems(params)) {
            if (!stack.isEmpty()) {
                net.minecraft.world.entity.item.ItemEntity drop = new net.minecraft.world.entity.item.ItemEntity(
                        sl, mob.getX(), mob.getY(), mob.getZ(), stack);
                event.getDrops().add(drop);
            }
        }
    }

    private static BossEventRarity parseRarityTag(String tag) {
        try {
            return BossEventRarity.valueOf(tag);
        } catch (IllegalArgumentException e) {
            return null;
        }
    }

    /** Zombie, squelettes, piglin zombifié, pillager — pas les brutes (déjà équipées) ni araignées / blaze / etc. */
    private static boolean canWearBossArmor(Mob mob) {
        if (mob.getType() == EntityType.PIGLIN_BRUTE) {
            return false;
        }
        return mob instanceof Zombie
                || mob instanceof AbstractSkeleton
                || mob instanceof ZombifiedPiglin
                || mob instanceof Pillager;
    }

    /**
     * Équipement visuel + armure vanilla ; l’attribut {@link Attributes#ARMOR} de base est mis à 0 pour éviter le double compte.
     *
     * @return {@code true} si un set a été appliqué
     */
    private static boolean tryApplyBossArmor(Mob mob, BossEventRarity rarity, RandomSource random) {
        if (!canWearBossArmor(mob)) {
            return false;
        }
        Item[] set = switch (rarity) {
            case COMMON -> new Item[] {
                    Items.LEATHER_HELMET, Items.LEATHER_CHESTPLATE, Items.LEATHER_LEGGINGS, Items.LEATHER_BOOTS
            };
            case UNCOMMON -> new Item[] {
                    Items.CHAINMAIL_HELMET, Items.CHAINMAIL_CHESTPLATE, Items.CHAINMAIL_LEGGINGS, Items.CHAINMAIL_BOOTS
            };
            case RARE -> new Item[] { Items.IRON_HELMET, Items.IRON_CHESTPLATE, Items.IRON_LEGGINGS, Items.IRON_BOOTS };
            case EPIC, LEGENDARY -> new Item[] {
                    Items.DIAMOND_HELMET, Items.DIAMOND_CHESTPLATE, Items.DIAMOND_LEGGINGS, Items.DIAMOND_BOOTS
            };
            case MYTHIC -> new Item[] {
                    Items.NETHERITE_HELMET, Items.NETHERITE_CHESTPLATE, Items.NETHERITE_LEGGINGS, Items.NETHERITE_BOOTS
            };
        };
        mob.setItemSlot(EquipmentSlot.HEAD, new ItemStack(set[0]));
        mob.setItemSlot(EquipmentSlot.CHEST, new ItemStack(set[1]));
        mob.setItemSlot(EquipmentSlot.LEGS, new ItemStack(set[2]));
        mob.setItemSlot(EquipmentSlot.FEET, new ItemStack(set[3]));
        float chance = 0.11F;
        mob.setDropChance(EquipmentSlot.HEAD, chance);
        mob.setDropChance(EquipmentSlot.CHEST, chance);
        mob.setDropChance(EquipmentSlot.LEGS, chance);
        mob.setDropChance(EquipmentSlot.FEET, chance);
        maybeApplyBossArmorTrim(mob, rarity, random);
        return true;
    }

    private static float bossTrimChancePerPiece(BossEventRarity rarity) {
        return switch (rarity) {
            case COMMON -> 0.32F;
            case UNCOMMON -> 0.42F;
            case RARE -> 0.52F;
            case EPIC -> 0.62F;
            case LEGENDARY -> 0.72F;
            case MYTHIC -> 0.82F;
        };
    }

    private static ArmorTrim randomBossArmorTrim(RandomSource r, HolderGetter<TrimMaterial> mats, HolderGetter<TrimPattern> pats) {
        ResourceKey<TrimMaterial> mk = switch (r.nextInt(10)) {
            case 0 -> TrimMaterials.QUARTZ;
            case 1 -> TrimMaterials.IRON;
            case 2 -> TrimMaterials.COPPER;
            case 3 -> TrimMaterials.GOLD;
            case 4 -> TrimMaterials.LAPIS;
            case 5 -> TrimMaterials.REDSTONE;
            case 6 -> TrimMaterials.AMETHYST;
            case 7 -> TrimMaterials.EMERALD;
            case 8 -> TrimMaterials.DIAMOND;
            default -> TrimMaterials.NETHERITE;
        };
        ResourceKey<TrimPattern> pk = switch (r.nextInt(14)) {
            case 0 -> TrimPatterns.COAST;
            case 1 -> TrimPatterns.SENTRY;
            case 2 -> TrimPatterns.DUNE;
            case 3 -> TrimPatterns.WILD;
            case 4 -> TrimPatterns.WARD;
            case 5 -> TrimPatterns.EYE;
            case 6 -> TrimPatterns.TIDE;
            case 7 -> TrimPatterns.RIB;
            case 8 -> TrimPatterns.SPIRE;
            case 9 -> TrimPatterns.HOST;
            case 10 -> TrimPatterns.FLOW;
            case 11 -> TrimPatterns.BOLT;
            case 12 -> TrimPatterns.SILENCE;
            default -> TrimPatterns.WAYFINDER;
        };
        return new ArmorTrim(mats.getOrThrow(mk), pats.getOrThrow(pk));
    }

    private static void maybeApplyBossArmorTrim(Mob mob, BossEventRarity rarity, RandomSource r) {
        if (!canWearBossArmor(mob)) {
            return;
        }
        float p = bossTrimChancePerPiece(rarity);
        var mats = mob.registryAccess().lookupOrThrow(Registries.TRIM_MATERIAL);
        var pats = mob.registryAccess().lookupOrThrow(Registries.TRIM_PATTERN);
        for (EquipmentSlot slot : EquipmentSlot.values()) {
            if (!slot.isArmor()) {
                continue;
            }
            ItemStack stack = mob.getItemBySlot(slot);
            if (stack.isEmpty() || !(stack.getItem() instanceof ArmorItem)) {
                continue;
            }
            if (r.nextFloat() >= p) {
                continue;
            }
            stack.set(DataComponents.TRIM, randomBossArmorTrim(r, mats, pats));
        }
    }
}
