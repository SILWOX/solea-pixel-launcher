package com.soleapixel.bmcmod.quest;

import java.util.ArrayList;
import java.util.EnumSet;
import java.util.List;
import java.util.UUID;

import javax.annotation.Nullable;

import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.Mth;
import net.minecraft.util.RandomSource;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;

import com.soleapixel.bmcmod.registry.ModItems;

import net.minecraft.core.component.DataComponents;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.world.item.component.CustomData;

public final class QuestGenerator {
    /** Cible spéciale : n'importe quel poisson (tag vanilla). */
    public static final String ANY_FISH_TARGET = "bmcmod:any_fish";

    /** Entité unique du contrat « Bounty Hunter ». */
    public static final String BOUNTY_ENTITY_ID = "bmcmod:bounty_hunter";

    /** Aligné sur {@link QuestDifficulty#QUEST_TIME_MIN_TICKS} / {@link QuestDifficulty#QUEST_TIME_MAX_TICKS} (10–60 min). */
    public static final int MIN_DURATION_TICKS = QuestDifficulty.QUEST_TIME_MIN_TICKS;
    public static final int MAX_DURATION_TICKS = QuestDifficulty.QUEST_TIME_MAX_TICKS;

    private static final String[] KILL_EASY = new String[] {
            "minecraft:zombie", "minecraft:skeleton", "minecraft:spider", "minecraft:zombie_villager"
    };
    private static final String[] KILL_NORMAL = new String[] {
            "minecraft:creeper", "minecraft:husk", "minecraft:stray", "minecraft:cave_spider", "minecraft:drowned"
    };
    private static final String[] KILL_HARD = new String[] {
            "minecraft:witch", "minecraft:blaze", "minecraft:magma_cube", "minecraft:guardian", "minecraft:pillager"
    };
    /** Ravager retiré : objectifs trop lourds ; mélange end/nether dangereux. */
    private static final String[] KILL_EXTREME = new String[] {
            "minecraft:enderman", "minecraft:wither_skeleton", "minecraft:shulker", "minecraft:ghast", "minecraft:hoglin"
    };

    private static final Item[] COLLECT_EASY = new Item[] {
            Items.ROTTEN_FLESH, Items.BONE, Items.STRING, Items.WHEAT, Items.COAL, Items.STICK
    };
    private static final Item[] COLLECT_NORMAL = new Item[] {
            Items.ARROW, Items.FEATHER, Items.LEATHER, Items.COPPER_INGOT, Items.REDSTONE, Items.LAPIS_LAZULI
    };
    private static final Item[] COLLECT_HARD = new Item[] {
            Items.GOLD_INGOT, Items.IRON_INGOT, Items.ENDER_PEARL, Items.GLOWSTONE_DUST, Items.PRISMARINE_SHARD
    };
    private static final Item[] COLLECT_EXTREME = new Item[] {
            Items.DIAMOND, Items.EMERALD, Items.NETHERITE_SCRAP, Items.ECHO_SHARD, Items.SHULKER_SHELL
    };

    private static final Block[] MINE_EASY = new Block[] {
            Blocks.STONE, Blocks.COBBLESTONE, Blocks.DIRT, Blocks.SAND, Blocks.GRAVEL, Blocks.OAK_LOG
    };
    private static final Block[] MINE_NORMAL = new Block[] {
            Blocks.DEEPSLATE, Blocks.COAL_ORE, Blocks.DEEPSLATE_COAL_ORE, Blocks.IRON_ORE, Blocks.COPPER_ORE
    };
    private static final Block[] MINE_HARD = new Block[] {
            Blocks.GOLD_ORE, Blocks.DEEPSLATE_GOLD_ORE, Blocks.REDSTONE_ORE, Blocks.LAPIS_ORE, Blocks.NETHERRACK
    };
    private static final Block[] MINE_EXTREME = new Block[] {
            Blocks.OBSIDIAN, Blocks.ANCIENT_DEBRIS, Blocks.DEEPSLATE_DIAMOND_ORE, Blocks.CRYING_OBSIDIAN
    };

    private static final Item[] FISH_TARGETS = new Item[] {
            Items.COD, Items.SALMON, Items.TROPICAL_FISH, Items.PUFFERFISH
    };

    private static final Item[] CRAFT_EASY = new Item[] {
            Items.STICK, Items.OAK_PLANKS, Items.TORCH, Items.CRAFTING_TABLE, Items.CHEST
    };
    private static final Item[] CRAFT_NORMAL = new Item[] {
            Items.FURNACE, Items.STONE_PICKAXE, Items.IRON_INGOT, Items.BUCKET, Items.SHEARS
    };
    private static final Item[] CRAFT_HARD = new Item[] {
            Items.IRON_PICKAXE, Items.BLAST_FURNACE, Items.ENCHANTING_TABLE, Items.ANVIL, Items.DIAMOND
    };
    /** Pas de beacon / cristal d’End / bloc de netherite en masse. */
    private static final Item[] CRAFT_EXTREME = new Item[] {
            Items.NETHERITE_INGOT, Items.SHULKER_BOX, Items.DIAMOND_BLOCK, Items.IRON_BLOCK, Items.GOLD_BLOCK, Items.BOOKSHELF
    };

    private QuestGenerator() {
    }

    public static ItemStack createQuestLog(RandomSource random) {
        QuestDifficulty diff = QuestDifficulty.roll(random);
        int duration = diff.rollDurationTicks(random);
        return createQuestLog(random, diff, duration, null);
    }

    public static ItemStack createQuestLog(RandomSource random, QuestDifficulty difficulty, int durationTicks) {
        return createQuestLog(random, difficulty, durationTicks, null);
    }

    public static ItemStack createQuestLog(RandomSource random, QuestDifficulty difficulty, int durationTicks, @Nullable BlockPos anchor) {
        int baseDuration = Mth.clamp(durationTicks, MIN_DURATION_TICKS, MAX_DURATION_TICKS);

        QuestLogData data = new QuestLogData();
        data.questId = UUID.randomUUID().toString();
        data.difficulty = difficulty;
        data.started = false;
        data.deadlineTick = 0;

        if (difficulty == QuestDifficulty.SPECIAL) {
            data.tasks.add(buildSubtaskTreasure(random, difficulty, anchor));
        } else if (difficulty == QuestDifficulty.BOUNTY_HUNTER) {
            QuestLogData.QuestSubTask bounty = new QuestLogData.QuestSubTask();
            bounty.kind = QuestKind.KILL;
            bounty.target = BOUNTY_ENTITY_ID;
            bounty.goal = 1;
            bounty.progress = 0;
            bounty.titleKey = "quest.bmcmod.title.bounty_hunter";
            data.tasks.add(bounty);
        } else {
            int subCount = rollSubtaskCount(difficulty, random);
            QuestKind rolled = rollKind(random, difficulty);
            if (rolled == QuestKind.EXPLORE || rolled == QuestKind.TREASURE || subCount <= 1) {
                data.tasks.add(buildSubtask(rolled, difficulty, random, anchor));
            } else {
                EnumSet<QuestKind> used = EnumSet.noneOf(QuestKind.class);
                QuestKind first = rerollIfNonStackable(rolled, random, difficulty);
                data.tasks.add(buildSubtask(first, difficulty, random, anchor));
                used.add(first);
                while (data.tasks.size() < subCount) {
                    QuestKind next = pickAnotherStackableKind(used, random, difficulty);
                    used.add(next);
                    data.tasks.add(buildSubtask(next, difficulty, random, anchor));
                }
            }
        }

        data.syncLegacyFromFirst();
        if (difficulty == QuestDifficulty.BOUNTY_HUNTER) {
            data.durationTicks = Mth.clamp(
                    baseDuration,
                    QuestDifficulty.BOUNTY_TIME_MIN_TICKS,
                    QuestDifficulty.BOUNTY_TIME_MAX_TICKS);
        } else {
            data.durationTicks = finalizeDurationTicksMulti(baseDuration, data.tasks, difficulty, random);
        }

        ItemStack stack = new ItemStack(ModItems.QUEST_LOG.get());
        CompoundTag tag = new CompoundTag();
        data.write(tag);
        stack.set(DataComponents.CUSTOM_DATA, CustomData.of(tag));
        return stack;
    }

    /** Nombre d’objectifs distincts (1–3) ; plus de chances d’en avoir plusieurs en difficulté élevée. */
    private static int rollSubtaskCount(QuestDifficulty d, RandomSource r) {
        float f = r.nextFloat();
        return switch (d) {
            case EASY -> f < 0.88F ? 1 : 2;
            case NORMAL -> f < 0.58F ? 1 : (f < 0.93F ? 2 : 3);
            case HARD -> f < 0.16F ? 1 : (f < 0.62F ? 2 : 3);
            case EXTREME -> f < 0.08F ? 1 : (f < 0.45F ? 2 : 3);
            case BOUNTY_HUNTER -> 1;
            default -> 1;
        };
    }

    private static QuestKind rerollIfNonStackable(QuestKind rolled, RandomSource random, QuestDifficulty difficulty) {
        QuestKind k = rolled;
        int guard = 0;
        while ((k == QuestKind.EXPLORE || k == QuestKind.TREASURE) && guard++ < 16) {
            k = rollKind(random, difficulty);
        }
        if (k == QuestKind.EXPLORE || k == QuestKind.TREASURE) {
            return QuestKind.KILL;
        }
        return k;
    }

    private static final QuestKind[] STACKABLE_KINDS = {
            QuestKind.KILL, QuestKind.COLLECT, QuestKind.MINE,
            QuestKind.VILLAGER_TRADE, QuestKind.FISH, QuestKind.CRAFT
    };

    private static QuestKind pickAnotherStackableKind(EnumSet<QuestKind> used, RandomSource r, QuestDifficulty difficulty) {
        List<QuestKind> pool = new ArrayList<>();
        for (QuestKind k : STACKABLE_KINDS) {
            if (!used.contains(k)) {
                pool.add(k);
            }
        }
        if (pool.isEmpty()) {
            return QuestKind.KILL;
        }
        return pool.get(r.nextInt(pool.size()));
    }

    private static QuestLogData.QuestSubTask buildSubtaskTreasure(RandomSource random, QuestDifficulty difficulty, @Nullable BlockPos anchor) {
        BlockPos destination = rollDestination(anchor, random, difficulty, true);
        QuestLogData.QuestSubTask s = new QuestLogData.QuestSubTask();
        s.kind = QuestKind.TREASURE;
        s.target = "bmcmod:treasure";
        s.goal = 1;
        s.progress = 0;
        s.titleKey = "quest.bmcmod.title.treasure";
        s.objectiveX = destination.getX();
        s.objectiveZ = destination.getZ();
        return s;
    }

    private static QuestLogData.QuestSubTask buildSubtask(QuestKind kind, QuestDifficulty difficulty, RandomSource random, @Nullable BlockPos anchor) {
        QuestLogData.QuestSubTask s = new QuestLogData.QuestSubTask();
        s.kind = kind;
        s.progress = 0;
        switch (kind) {
            case COLLECT -> {
                Item it = pickCollectItem(difficulty, random);
                s.target = BuiltInRegistries.ITEM.getKey(it).toString();
                s.goal = collectGoal(difficulty, it, random);
                s.titleKey = "quest.bmcmod.title.collect";
            }
            case MINE -> {
                Block b = pickMineBlock(difficulty, random);
                s.target = BuiltInRegistries.BLOCK.getKey(b).toString();
                s.goal = mineGoal(difficulty, b, random);
                s.titleKey = "quest.bmcmod.title.mine";
            }
            case VILLAGER_TRADE -> {
                s.target = "minecraft:villager";
                s.goal = switch (difficulty) {
                    case EASY -> 1;
                    case NORMAL -> 2 + random.nextInt(2);
                    case HARD -> 4 + random.nextInt(4);
                    case EXTREME -> 5 + random.nextInt(4);
                    default -> 4 + random.nextInt(3);
                };
                s.titleKey = "quest.bmcmod.title.trade";
            }
            case FISH -> {
                boolean any = switch (difficulty) {
                    case EASY, NORMAL -> true;
                    case HARD -> random.nextFloat() < 0.30F;
                    case EXTREME -> random.nextFloat() < 0.14F;
                    default -> random.nextFloat() < 0.55F;
                };
                if (any) {
                    s.target = ANY_FISH_TARGET;
                } else {
                    Item fish = FISH_TARGETS[random.nextInt(FISH_TARGETS.length)];
                    s.target = BuiltInRegistries.ITEM.getKey(fish).toString();
                }
                s.goal = switch (difficulty) {
                    case EASY -> 2 + random.nextInt(3);
                    case NORMAL -> 4 + random.nextInt(4);
                    case HARD -> 8 + random.nextInt(6);
                    case EXTREME -> 10 + random.nextInt(8);
                    default -> 8 + random.nextInt(6);
                };
                s.titleKey = "quest.bmcmod.title.fish";
            }
            case CRAFT -> {
                Item it = pickCraftResult(difficulty, random);
                s.target = BuiltInRegistries.ITEM.getKey(it).toString();
                s.goal = craftGoal(difficulty, it, random);
                s.titleKey = "quest.bmcmod.title.craft";
            }
            case EXPLORE -> {
                BlockPos destination = rollDestination(anchor, random, difficulty, false);
                s.target = "bmcmod:explore";
                s.goal = 1;
                s.titleKey = "quest.bmcmod.title.explore";
                s.objectiveX = destination.getX();
                s.objectiveZ = destination.getZ();
            }
            case TREASURE -> {
                BlockPos destination = rollDestination(anchor, random, difficulty, true);
                s.target = "bmcmod:treasure";
                s.goal = 1;
                s.titleKey = "quest.bmcmod.title.treasure";
                s.objectiveX = destination.getX();
                s.objectiveZ = destination.getZ();
            }
            default -> {
                s.target = pickKillEntity(difficulty, random);
                s.goal = killGoal(difficulty, s.target, random);
                s.titleKey = "quest.bmcmod.title.kill";
            }
        }
        return s;
    }

    /** Objectifs lourds reçoivent un peu plus de temps, sans dépasser 60 min. */
    private static int finalizeDurationTicksMulti(
            int baseTicks, List<QuestLogData.QuestSubTask> subtasks, QuestDifficulty difficulty, RandomSource random) {
        int load = 0;
        for (QuestLogData.QuestSubTask t : subtasks) {
            if (t.kind == QuestKind.TREASURE || t.kind == QuestKind.EXPLORE) {
                load += 12;
            } else {
                load += t.goal;
            }
        }
        int bonus = Mth.clamp((int) (load * 20L), 0, 8 * 60 * 20);
        if (difficulty == QuestDifficulty.SPECIAL) {
            bonus += 2 * 60 * 20;
        }
        if (subtasks.size() >= 2) {
            bonus += (subtasks.size() - 1) * 90 * 20;
        }
        if (difficulty == QuestDifficulty.HARD && subtasks.size() >= 2) {
            bonus += 3 * 60 * 20;
        }
        if (difficulty == QuestDifficulty.EXTREME && subtasks.size() >= 2) {
            bonus += 5 * 60 * 20;
        }
        bonus += random.nextInt(400);
        return Mth.clamp(baseTicks + bonus, MIN_DURATION_TICKS, MAX_DURATION_TICKS);
    }

    private static int collectGoal(QuestDifficulty d, Item it, RandomSource r) {
        if (it == Items.NETHERITE_SCRAP || it == Items.ECHO_SHARD || it == Items.SHULKER_SHELL) {
            return switch (d) {
                case EASY, NORMAL -> 1 + r.nextInt(2);
                case HARD -> 2 + r.nextInt(3);
                case EXTREME -> 3 + r.nextInt(3);
                default -> 2 + r.nextInt(3);
            };
        }
        if (it == Items.DIAMOND || it == Items.EMERALD) {
            return switch (d) {
                case EASY -> 1 + r.nextInt(2);
                case NORMAL -> 2 + r.nextInt(3);
                case HARD -> 4 + r.nextInt(5);
                case EXTREME -> 5 + r.nextInt(7);
                default -> 4 + r.nextInt(5);
            };
        }
        if (it == Items.GOLD_INGOT || it == Items.IRON_INGOT || it == Items.ENDER_PEARL) {
            return switch (d) {
                case EASY -> 4 + r.nextInt(6);
                case NORMAL -> 8 + r.nextInt(10);
                case HARD -> 14 + r.nextInt(14);
                case EXTREME -> 18 + r.nextInt(16);
                default -> 16 + r.nextInt(14);
            };
        }
        return switch (d) {
            case EASY -> 6 + r.nextInt(8);
            case NORMAL -> 12 + r.nextInt(12);
            case HARD -> 22 + r.nextInt(18);
            case EXTREME -> 28 + r.nextInt(20);
            default -> 24 + r.nextInt(18);
        };
    }

    private static int mineGoal(QuestDifficulty d, Block b, RandomSource r) {
        if (b == Blocks.ANCIENT_DEBRIS) {
            return switch (d) {
                case EASY, NORMAL -> 1 + r.nextInt(2);
                case HARD -> 3 + r.nextInt(3);
                case EXTREME -> 4 + r.nextInt(4);
                default -> 3 + r.nextInt(4);
            };
        }
        if (b == Blocks.OBSIDIAN || b == Blocks.CRYING_OBSIDIAN) {
            return switch (d) {
                case EASY -> 6 + r.nextInt(8);
                case NORMAL -> 12 + r.nextInt(12);
                case HARD -> 20 + r.nextInt(16);
                case EXTREME -> 24 + r.nextInt(20);
                default -> 16 + r.nextInt(16);
            };
        }
        if (b == Blocks.DEEPSLATE_DIAMOND_ORE) {
            return switch (d) {
                case EASY -> 2 + r.nextInt(3);
                case NORMAL -> 4 + r.nextInt(5);
                case HARD -> 7 + r.nextInt(7);
                case EXTREME -> 10 + r.nextInt(10);
                default -> 8 + r.nextInt(8);
            };
        }
        return switch (d) {
            case EASY -> 12 + r.nextInt(16);
            case NORMAL -> 28 + r.nextInt(22);
            case HARD -> 46 + r.nextInt(30);
            case EXTREME -> 52 + r.nextInt(34);
            default -> 32 + r.nextInt(28);
        };
    }

    private static int craftGoal(QuestDifficulty d, Item it, RandomSource r) {
        if (it == Items.NETHERITE_INGOT || it == Items.SHULKER_BOX || it == Items.DIAMOND_BLOCK) {
            return 1 + r.nextInt(2);
        }
        if (it == Items.DIAMOND || it == Items.IRON_BLOCK || it == Items.GOLD_BLOCK) {
            return switch (d) {
                case EASY -> 1 + r.nextInt(2);
                case NORMAL -> 2 + r.nextInt(2);
                case HARD -> 3 + r.nextInt(3);
                case EXTREME -> 3 + r.nextInt(4);
                default -> 2 + r.nextInt(3);
            };
        }
        if (it == Items.BOOKSHELF) {
            return switch (d) {
                case EASY -> 2 + r.nextInt(3);
                case NORMAL -> 3 + r.nextInt(4);
                case HARD -> 5 + r.nextInt(4);
                case EXTREME -> 6 + r.nextInt(5);
                default -> 4 + r.nextInt(4);
            };
        }
        return switch (d) {
            case EASY -> 2 + r.nextInt(5);
            case NORMAL -> 3 + r.nextInt(6);
            case HARD -> 5 + r.nextInt(7);
            case EXTREME -> 6 + r.nextInt(8);
            default -> 4 + r.nextInt(5);
        };
    }

    private static boolean isDangerousKillTarget(String id) {
        return id.contains("ghast")
                || id.contains("shulker")
                || id.contains("wither_skeleton")
                || id.contains("enderman")
                || id.contains("hoglin");
    }

    private static int killGoal(QuestDifficulty d, String entityId, RandomSource r) {
        if (isDangerousKillTarget(entityId)) {
            return switch (d) {
                case EASY -> 1;
                case NORMAL -> 1 + r.nextInt(2);
                case HARD -> 3 + r.nextInt(3);
                case EXTREME -> 4 + r.nextInt(4);
                default -> 2 + r.nextInt(3);
            };
        }
        return switch (d) {
            case EASY -> 3 + r.nextInt(5);
            case NORMAL -> 5 + r.nextInt(6);
            case HARD -> 10 + r.nextInt(9);
            case EXTREME -> 12 + r.nextInt(11);
            default -> 10 + r.nextInt(8);
        };
    }

    private static QuestKind rollKind(RandomSource random, QuestDifficulty difficulty) {
        if (difficulty == QuestDifficulty.SPECIAL) {
            return QuestKind.TREASURE;
        }
        float f = random.nextFloat();
        if (f < 0.20F) {
            return QuestKind.KILL;
        }
        if (f < 0.38F) {
            return QuestKind.COLLECT;
        }
        if (f < 0.55F) {
            return QuestKind.MINE;
        }
        if (f < 0.68F) {
            return QuestKind.VILLAGER_TRADE;
        }
        if (f < 0.82F) {
            return QuestKind.FISH;
        }
        if (f < 0.93F) {
            return QuestKind.CRAFT;
        }
        if (f < 0.985F) {
            return QuestKind.EXPLORE;
        }
        return QuestKind.TREASURE;
    }

    private static Item pickCollectItem(QuestDifficulty d, RandomSource r) {
        return switch (d) {
            case EASY -> COLLECT_EASY[r.nextInt(COLLECT_EASY.length)];
            case NORMAL -> COLLECT_NORMAL[r.nextInt(COLLECT_NORMAL.length)];
            case HARD -> COLLECT_HARD[r.nextInt(COLLECT_HARD.length)];
            default -> COLLECT_EXTREME[r.nextInt(COLLECT_EXTREME.length)];
        };
    }

    private static Block pickMineBlock(QuestDifficulty d, RandomSource r) {
        return switch (d) {
            case EASY -> MINE_EASY[r.nextInt(MINE_EASY.length)];
            case NORMAL -> MINE_NORMAL[r.nextInt(MINE_NORMAL.length)];
            case HARD -> MINE_HARD[r.nextInt(MINE_HARD.length)];
            default -> MINE_EXTREME[r.nextInt(MINE_EXTREME.length)];
        };
    }

    private static Item pickCraftResult(QuestDifficulty d, RandomSource r) {
        return switch (d) {
            case EASY -> CRAFT_EASY[r.nextInt(CRAFT_EASY.length)];
            case NORMAL -> CRAFT_NORMAL[r.nextInt(CRAFT_NORMAL.length)];
            case HARD -> CRAFT_HARD[r.nextInt(CRAFT_HARD.length)];
            default -> CRAFT_EXTREME[r.nextInt(CRAFT_EXTREME.length)];
        };
    }

    private static String pickKillEntity(QuestDifficulty d, RandomSource r) {
        return switch (d) {
            case EASY -> KILL_EASY[r.nextInt(KILL_EASY.length)];
            case NORMAL -> KILL_NORMAL[r.nextInt(KILL_NORMAL.length)];
            case HARD -> KILL_HARD[r.nextInt(KILL_HARD.length)];
            default -> KILL_EXTREME[r.nextInt(KILL_EXTREME.length)];
        };
    }

    public static String describeTarget(QuestLogData d) {
        d.ensureTasks();
        if (d.tasks.size() == 1) {
            return describeSubtask(d.tasks.getFirst());
        }
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < d.tasks.size(); i++) {
            if (i > 0) {
                sb.append(" + ");
            }
            sb.append(describeSubtask(d.tasks.get(i)));
        }
        return sb.toString();
    }

    public static String describeSubtask(QuestLogData.QuestSubTask t) {
        try {
            if (t.kind == QuestKind.TREASURE || t.kind == QuestKind.EXPLORE) {
                return Component.translatable("quest.bmcmod.coords", t.objectiveX, t.objectiveZ).getString();
            }
            if (t.kind == QuestKind.FISH && ANY_FISH_TARGET.equals(t.target)) {
                return Component.translatable("quest.bmcmod.any_fish").getString();
            }
            ResourceLocation rl = t.targetRl();
            return switch (t.kind) {
                case COLLECT -> BuiltInRegistries.ITEM.getOptional(rl)
                        .map(i -> i.getDefaultInstance().getHoverName().getString())
                        .orElse(t.target);
                case MINE -> BuiltInRegistries.BLOCK.getOptional(rl)
                        .map(b -> b.asItem().getDefaultInstance().getHoverName().getString())
                        .orElse(t.target);
                case VILLAGER_TRADE -> Component.translatable("entity.minecraft.villager").getString();
                case FISH -> BuiltInRegistries.ITEM.getOptional(rl)
                        .map(i -> i.getDefaultInstance().getHoverName().getString())
                        .orElse(t.target);
                case CRAFT -> BuiltInRegistries.ITEM.getOptional(rl)
                        .map(i -> i.getDefaultInstance().getHoverName().getString())
                        .orElse(t.target);
                default -> BuiltInRegistries.ENTITY_TYPE.getOptional(rl)
                        .map(e -> e.getDescription().getString())
                        .orElse(t.target);
            };
        } catch (Exception e) {
            return t.target;
        }
    }

    public static MutableComponent titleComponentForQuest(QuestLogData d) {
        d.ensureTasks();
        if (d.tasks.size() == 1) {
            return titleLineForSubtask(d.tasks.getFirst());
        }
        MutableComponent out = Component.empty();
        for (int i = 0; i < d.tasks.size(); i++) {
            if (i > 0) {
                out.append(Component.literal(" · "));
            }
            out.append(titleLineForSubtask(d.tasks.get(i)));
        }
        return out;
    }

    private static MutableComponent titleLineForSubtask(QuestLogData.QuestSubTask t) {
        if (t.kind == QuestKind.KILL && BOUNTY_ENTITY_ID.equals(t.target)) {
            return Component.translatable(t.titleKey);
        }
        String desc = describeSubtask(t);
        return switch (t.kind) {
            case EXPLORE, TREASURE -> Component.translatable(t.titleKey, desc);
            default -> Component.translatable(t.titleKey, desc, String.valueOf(t.goal));
        };
    }

    private static BlockPos rollDestination(@Nullable BlockPos anchor, RandomSource random, QuestDifficulty difficulty, boolean treasure) {
        int baseX = anchor != null ? anchor.getX() : 0;
        int baseZ = anchor != null ? anchor.getZ() : 0;
        int minRadius;
        int maxRadius;
        if (treasure) {
            minRadius = switch (difficulty) {
                case EASY -> 220;
                case NORMAL -> 350;
                case HARD -> 580;
                case EXTREME -> 820;
                case SPECIAL, BOUNTY_HUNTER -> 900;
            };
            maxRadius = minRadius + switch (difficulty) {
                case EASY -> 180;
                case NORMAL -> 260;
                case HARD -> 380;
                case EXTREME -> 520;
                case SPECIAL, BOUNTY_HUNTER -> 700;
            };
        } else {
            minRadius = switch (difficulty) {
                case EASY -> 200;
                case NORMAL -> 240;
                case HARD -> 320;
                case EXTREME -> 400;
                case SPECIAL, BOUNTY_HUNTER -> 360;
            };
            maxRadius = switch (difficulty) {
                case EASY -> 520;
                case NORMAL -> 620;
                case HARD -> 780;
                case EXTREME -> 960;
                case SPECIAL, BOUNTY_HUNTER -> 920;
            };
        }
        double angle = random.nextDouble() * Math.PI * 2.0D;
        int radius = minRadius + random.nextInt(Math.max(1, maxRadius - minRadius + 1));
        int x = baseX + Mth.floor(Math.cos(angle) * radius);
        int z = baseZ + Mth.floor(Math.sin(angle) * radius);
        return new BlockPos(x, 64, z);
    }
}
