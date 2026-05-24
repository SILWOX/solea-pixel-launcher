package com.soleapixel.bmcmod.block.feeder;

import java.util.Comparator;
import java.util.List;

import org.jetbrains.annotations.Nullable;
import org.joml.Vector3f;

import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.DustParticleOptions;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.RandomSource;
import net.minecraft.world.entity.animal.Animal;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;

/**
 * Fenêtres matin / midi / soir, rayon 20, plus proche mangeoire compatible, reproduction, particules.
 * Auto-repas : chaque animal ne peut manger qu’au plus une fois toutes les 6–10 min (temps jeu), tirage au hasard.
 * Liaison stricte : un mob n’est lié qu’à une seule mangeoire tant que le lien est valide (pas de bascule vers une autre,
 * même avec plusieurs types de nourriture à portée). Dissociation : trop loin, mangeoire vide, ou bloc absent.
 */
public final class FeederAutoFeed {
    private static final String FEEDER_BIND_POS = "bmcmod:feeder_bind_pos";
    /** Prochain tick jeu où l’animal peut à nouveau manger à la mangeoire (auto). */
    private static final String FEEDER_NEXT_EAT_TICK = "bmcmod:feeder_next_eat_tick";
    /** Entre deux repas automatiques : 6 à 10 minutes (temps jeu). */
    private static final int EAT_COOLDOWN_MIN_TICKS = 6 * 60 * 20;
    private static final int EAT_COOLDOWN_MAX_TICKS = 10 * 60 * 20;
    private static final int LINK_SCAN_INTERVAL = 15;
    /** Fenêtres approximatives sur {@code dayTime % 24000}. */
    private static final int MORNING_START = 1000;
    private static final int MORNING_END = 3500;
    private static final int NOON_START = 5500;
    private static final int NOON_END = 7500;
    private static final int EVENING_START = 11000;
    private static final int EVENING_END = 13000;

    private static final double RADIUS = 20.0;
    private static final double RADIUS_SQ = RADIUS * RADIUS;
    /** Marge au-delà du rayon pour encore trouver le mob qui vient de sortir de la zone. */
    private static final double LOST_SCAN_EXTRA = 6.0;
    private static final DustParticleOptions LOST_LINK_DUST =
            new DustParticleOptions(new Vector3f(1.0F, 0.2F, 0.18F), 0.72F);

    private FeederAutoFeed() {
    }

    public static boolean isFeedingWindow(Level level) {
        long t = level.getDayTime() % 24000L;
        return inRange(t, MORNING_START, MORNING_END)
                || inRange(t, NOON_START, NOON_END)
                || inRange(t, EVENING_START, EVENING_END);
    }

    private static boolean inRange(long t, int a, int b) {
        return t >= a && t <= b;
    }

    /**
     * Particules rouges lorsque le mob n’est plus « détecté » par ce feeder alors qu’il était lié
     * (trop loin, mangeoire vide, ou nourriture refusée par le mob).
     */
    public static void tickLostLinkParticles(FeederBlockEntity feeder) {
        Level level = feeder.getLevel();
        if (!(level instanceof ServerLevel sl) || level.isClientSide()) {
            return;
        }
        if (sl.getGameTime() % LINK_SCAN_INTERVAL != 0L) {
            return;
        }
        BlockPos origin = feeder.getBlockPos();
        long packed = origin.asLong();
        ItemStack food = feeder.isEmpty() ? ItemStack.EMPTY : feeder.getRepresentativeStack();
        AABB box = new AABB(origin).inflate(RADIUS + LOST_SCAN_EXTRA);
        List<Animal> animals = level.getEntitiesOfClass(Animal.class, box, Animal::isAlive);
        for (Animal animal : animals) {
            if (animal.getPersistentData().getLong(FEEDER_BIND_POS) != packed) {
                continue;
            }
            if (!isLostDetection(feeder, animal, origin, food)) {
                continue;
            }
            spawnLostLinkParticles(sl, origin, animal);
            animal.getPersistentData().remove(FEEDER_BIND_POS);
        }
    }

    private static boolean isLostDetection(
            FeederBlockEntity feeder,
            Animal animal,
            BlockPos feederPos,
            ItemStack food) {
        Vec3 center = Vec3.atCenterOf(feederPos);
        if (animal.position().distanceToSqr(center) > RADIUS_SQ) {
            return true;
        }
        if (feeder.isEmpty() || food.isEmpty()) {
            return true;
        }
        if (!animal.isFood(food)) {
            return true;
        }
        return false;
    }

    private static void spawnLostLinkParticles(ServerLevel sl, BlockPos feederPos, Animal animal) {
        Vec3 feederTop = Vec3.atCenterOf(feederPos).add(0.0, 0.45, 0.0);
        Vec3 mouth = animal.position().add(0.0, animal.getBbHeight() * 0.88, 0.0);
        sl.sendParticles(LOST_LINK_DUST, feederTop.x, feederTop.y, feederTop.z, 12, 0.18, 0.1, 0.18, 0.015);
        sl.sendParticles(LOST_LINK_DUST, mouth.x, mouth.y, mouth.z, 10, 0.2, 0.12, 0.2, 0.015);
    }

    /**
     * Particules quand un mob entre dans la logique « ce feeder est mon mangeoire liée »
     * (nourriture acceptée + ce feeder est le plus proche avec cette nourriture).
     */
    public static void tickLinkParticles(FeederBlockEntity feeder) {
        Level level = feeder.getLevel();
        if (!(level instanceof ServerLevel sl) || level.isClientSide()) {
            return;
        }
        if (feeder.isEmpty()) {
            return;
        }
        if (sl.getGameTime() % LINK_SCAN_INTERVAL != 0L) {
            return;
        }
        ItemStack food = feeder.getRepresentativeStack();
        if (food.isEmpty()) {
            return;
        }
        BlockPos origin = feeder.getBlockPos();
        AABB box = new AABB(origin).inflate(RADIUS);
        List<Animal> animals = level.getEntitiesOfClass(Animal.class, box, a -> a.isAlive() && a.isFood(food));
        for (Animal animal : animals) {
            long target = origin.asLong();
            long prev = animal.getPersistentData().getLong(FEEDER_BIND_POS);
            if (prev != 0L && prev != target) {
                continue;
            }
            if (prev == target) {
                continue;
            }
            if (!isClosestCompatibleFeeder(sl, animal, feeder)) {
                continue;
            }
            animal.getPersistentData().putLong(FEEDER_BIND_POS, target);
            spawnFeederLinkParticles(sl, origin, animal);
        }
    }

    private static void spawnFeederLinkParticles(ServerLevel sl, BlockPos feederPos, Animal animal) {
        Vec3 feederTop = Vec3.atCenterOf(feederPos).add(0.0, 0.45, 0.0);
        Vec3 mouth = animal.position().add(0.0, animal.getBbHeight() * 0.88, 0.0);
        sl.sendParticles(ParticleTypes.COMPOSTER, feederTop.x, feederTop.y, feederTop.z, 14, 0.2, 0.12, 0.2, 0.02);
        sl.sendParticles(ParticleTypes.HAPPY_VILLAGER, mouth.x, mouth.y, mouth.z, 12, 0.25, 0.15, 0.25, 0.02);
        sl.sendParticles(ParticleTypes.HEART, feederTop.x, feederTop.y + 0.15, feederTop.z, 3, 0.15, 0.08, 0.15, 0.0);
    }

    public static void tickServer(FeederBlockEntity feeder) {
        Level level = feeder.getLevel();
        if (!(level instanceof ServerLevel sl) || level.isClientSide()) {
            return;
        }
        feedCycle(sl, feeder, false, false);
    }

    /**
     * Debug / commande : une passe de nourrissage pour tous les feeders enregistrés dans cette dimension,
     * sans fenêtre jour/ni espacement 45 t.
     *
     * @return nombre d’animaux effectivement nourris (items retirés du feeder).
     */
    public static int runDebugEatCycle(ServerLevel sl) {
        int total = 0;
        for (BlockPos fp : FeederNetwork.snapshot(sl.dimension())) {
            if (sl.getBlockEntity(fp) instanceof FeederBlockEntity feeder) {
                total += feedCycle(sl, feeder, true, true);
            }
        }
        return total;
    }

    /**
     * @param ignoreScheduling ignore la fenêtre horaire et le modulo 45 t (pour debug).
     * @param ignoreCooldown ignore le délai 6–10 min entre deux repas (pour {@link #runDebugEatCycle}).
     * @return nombre d’animaux nourris pendant cet appel.
     */
    private static int feedCycle(ServerLevel sl, FeederBlockEntity feeder, boolean ignoreScheduling, boolean ignoreCooldown) {
        if (!ignoreScheduling) {
            if (!isFeedingWindow(sl)) {
                return 0;
            }
            if (sl.getGameTime() % 45L != 0L) {
                return 0;
            }
        }
        if (feeder.isEmpty()) {
            return 0;
        }
        ItemStack food = feeder.getRepresentativeStack();
        if (food.isEmpty()) {
            return 0;
        }
        BlockPos origin = feeder.getBlockPos();
        AABB box = new AABB(origin).inflate(RADIUS);
        List<Animal> animals = sl.getEntitiesOfClass(Animal.class, box, a -> a.isFood(food));
        animals.sort(Comparator.comparingDouble(a -> a.distanceToSqr(Vec3.atCenterOf(origin))));
        RandomSource rand = sl.random;
        int budget = ignoreScheduling ? Integer.MAX_VALUE : 3 + rand.nextInt(3);
        int fed = 0;
        for (Animal animal : animals) {
            if (budget <= 0) {
                break;
            }
            long bind = animal.getPersistentData().getLong(FEEDER_BIND_POS);
            long here = origin.asLong();
            if (bind != 0L && bind != here) {
                continue;
            }
            if (!isClosestCompatibleFeeder(sl, animal, feeder)) {
                continue;
            }
            if (!ignoreCooldown && !canEatFromFeederYet(sl, animal)) {
                continue;
            }
            if (!consumeOne(feeder)) {
                break;
            }
            if (animal.getPersistentData().getLong(FEEDER_BIND_POS) == 0L) {
                animal.getPersistentData().putLong(FEEDER_BIND_POS, here);
            }
            if (!ignoreCooldown) {
                scheduleNextFeederMeal(sl, animal);
            }
            Vec3 mouth = animal.position().add(0.0, animal.getBbHeight() * 0.85, 0.0);
            sl.sendParticles(ParticleTypes.HAPPY_VILLAGER, mouth.x, mouth.y, mouth.z, 8, 0.2, 0.12, 0.2, 0.02);
            tryLove(animal);
            fed++;
            budget--;
        }
        return fed;
    }

    private static boolean canEatFromFeederYet(ServerLevel sl, Animal animal) {
        long next = animal.getPersistentData().getLong(FEEDER_NEXT_EAT_TICK);
        return next == 0L || sl.getGameTime() >= next;
    }

    private static void scheduleNextFeederMeal(ServerLevel sl, Animal animal) {
        int span = EAT_COOLDOWN_MAX_TICKS - EAT_COOLDOWN_MIN_TICKS + 1;
        long delay = EAT_COOLDOWN_MIN_TICKS + (span > 0 ? sl.random.nextInt(span) : 0);
        animal.getPersistentData().putLong(FEEDER_NEXT_EAT_TICK, sl.getGameTime() + delay);
    }

    private static boolean consumeOne(FeederBlockEntity feeder) {
        if (feeder.isEmpty()) {
            return false;
        }
        ItemStack one = feeder.extractItems(1);
        return !one.isEmpty();
    }

    private static void tryLove(Animal animal) {
        if (animal.getAge() != 0) {
            return;
        }
        if (animal.canFallInLove()) {
            animal.setInLove(null);
        }
    }

    /**
     * Plus proche mangeoire non vide dont la nourriture est acceptée par l’animal (patate vs carotte : un seul « meilleur »).
     */
    private static boolean isClosestCompatibleFeeder(ServerLevel level, Animal animal, FeederBlockEntity candidate) {
        FeederBlockEntity best = findClosestCompatibleFeeder(level, animal);
        return best != null && best.getBlockPos().equals(candidate.getBlockPos());
    }

    @Nullable
    private static FeederBlockEntity findClosestCompatibleFeeder(ServerLevel level, Animal animal) {
        Vec3 ac = animal.position();
        double bestDistSq = Double.MAX_VALUE;
        FeederBlockEntity best = null;
        for (BlockPos fp : FeederNetwork.snapshot(level.dimension())) {
            if (fp.distSqr(animal.blockPosition()) > RADIUS_SQ) {
                continue;
            }
            if (!(level.getBlockEntity(fp) instanceof FeederBlockEntity f)) {
                continue;
            }
            if (f.isEmpty()) {
                continue;
            }
            ItemStack fFood = f.getRepresentativeStack();
            if (fFood.isEmpty() || !animal.isFood(fFood)) {
                continue;
            }
            double d = Vec3.atCenterOf(fp).distanceToSqr(ac);
            if (d < bestDistSq) {
                bestDistSq = d;
                best = f;
            }
        }
        return best;
    }
}
