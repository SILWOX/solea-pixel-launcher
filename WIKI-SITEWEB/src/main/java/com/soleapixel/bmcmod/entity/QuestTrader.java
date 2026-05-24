package com.soleapixel.bmcmod.entity;

import net.minecraft.nbt.CompoundTag;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.Mth;
import net.minecraft.util.RandomSource;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.ai.attributes.AttributeSupplier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.animal.Wolf;
import net.minecraft.world.entity.npc.AbstractVillager;
import net.minecraft.world.entity.npc.WanderingTrader;
import net.minecraft.world.entity.animal.horse.TraderLlama;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.trading.ItemCost;
import net.minecraft.world.item.trading.MerchantOffer;
import net.minecraft.world.item.trading.MerchantOffers;
import net.minecraft.world.level.Level;

import com.soleapixel.bmcmod.quest.QuestGenerator;
import com.soleapixel.bmcmod.quest.QuestDifficulty;
import com.soleapixel.bmcmod.registry.ModItems;

import java.util.Optional;

public class QuestTrader extends WanderingTrader {
    /** UUID du marchand sur les loups garde ; réutilisé par {@link com.soleapixel.bmcmod.gameplay.QuestGameplayEvents}. */
    public static final String QUEST_TRADER_GUARD_TAG = "bmcmod_quest_trader";
    private static final String TAG_DOG_SPAWN_COUNT = "bmcmod_quest_trader_dogs_spawned";

    private static final double GUARD_DOG_TELEPORT_DIST_SQ = 12.0 * 12.0;
    private static final double GUARD_DOG_NAVIGATE_DIST_SQ = 2.5 * 2.5;

    private int dogSpawnDelay;
    /** Nombre de chiens déjà créés par ce marchand (max 2 au total, jamais re-spawn après mort). */
    private int guardDogsEverSpawned;

    @SuppressWarnings("unchecked")
    public QuestTrader(EntityType<? extends QuestTrader> type, Level level) {
        super((EntityType<? extends WanderingTrader>) (EntityType<?>) type, level);
    }

    public static AttributeSupplier.Builder createAttributes() {
        // Marchand ambulant : pas de createAttributes() public ; on reprend la base villageois + vitesse vanilla (~0,5).
        return AbstractVillager.createLivingAttributes()
                .add(Attributes.MOVEMENT_SPEED, 0.5D)
                .add(Attributes.FOLLOW_RANGE, 48.0D);
    }

    @Override
    public void addAdditionalSaveData(CompoundTag tag) {
        super.addAdditionalSaveData(tag);
        tag.putInt(TAG_DOG_SPAWN_COUNT, this.guardDogsEverSpawned);
    }

    @Override
    public void readAdditionalSaveData(CompoundTag tag) {
        super.readAdditionalSaveData(tag);
        this.guardDogsEverSpawned = Mth.clamp(tag.getInt(TAG_DOG_SPAWN_COUNT), 0, 2);
    }

    @Override
    protected void updateTrades() {
        MerchantOffers offers = this.getOffers();
        offers.clear();
        RandomSource r = this.random;
        int questOfferSlots = 7 + r.nextInt(9);
        for (int i = 0; i < questOfferSlots; i++) {
            QuestDifficulty diff = QuestDifficulty.roll(r);
            ItemStack quest = QuestGenerator.createQuestLog(r, diff, diff.rollDurationTicks(r), this.blockPosition());
            ItemCost primary = switch (diff) {
                case EASY -> new ItemCost(Items.EMERALD, 1 + r.nextInt(2));
                case NORMAL -> new ItemCost(Items.EMERALD, 2 + r.nextInt(3));
                case HARD -> new ItemCost(Items.EMERALD, 4 + r.nextInt(4));
                case EXTREME -> new ItemCost(Items.EMERALD, 7 + r.nextInt(5));
                case SPECIAL -> new ItemCost(Items.EMERALD, 12 + r.nextInt(7));
                case BOUNTY_HUNTER -> new ItemCost(Items.EMERALD, 22 + r.nextInt(10));
            };
            Optional<ItemCost> secondary = switch (diff) {
                case EASY -> Optional.of(new ItemCost(Items.PAPER, 4 + r.nextInt(8)));
                case NORMAL -> Optional.of(new ItemCost(Items.PAPER, 8 + r.nextInt(10)));
                case HARD -> Optional.of(new ItemCost(Items.GOLD_INGOT, 1 + r.nextInt(2)));
                case EXTREME -> Optional.of(new ItemCost(Items.DIAMOND, 1));
                case SPECIAL -> Optional.of(new ItemCost(Items.DIAMOND, 2 + r.nextInt(2)));
                case BOUNTY_HUNTER -> Optional.of(new ItemCost(Items.DIAMOND, 4 + r.nextInt(2)));
            };
            // One purchase per quest offer (vanilla-style maxUses); each slot is a distinct generated contract.
            offers.add(new MerchantOffer(primary, secondary, quest, 1, 4, 0.06F));
        }
        // Always offer one dedicated treasure contract.
        ItemStack specialQuest = QuestGenerator.createQuestLog(r, QuestDifficulty.SPECIAL, QuestDifficulty.SPECIAL.rollDurationTicks(r), this.blockPosition());
        offers.add(new MerchantOffer(
                new ItemCost(Items.EMERALD, 18 + r.nextInt(8)),
                Optional.of(new ItemCost(Items.DIAMOND, 3)),
                specialQuest,
                1,
                8,
                0.08F));

        ItemStack bountyQuest = QuestGenerator.createQuestLog(
                r, QuestDifficulty.BOUNTY_HUNTER, QuestDifficulty.BOUNTY_HUNTER.rollDurationTicks(r), this.blockPosition());
        offers.add(new MerchantOffer(
                new ItemCost(Items.EMERALD, 28 + r.nextInt(10)),
                Optional.of(new ItemCost(Items.DIAMOND, 5)),
                bountyQuest,
                1,
                2,
                0.05F));

        // Also sell Undead Potions (progressive rarity/cost).
        int tierA = 1 + r.nextInt(3); // 1..3
        int tierB = 2 + r.nextInt(3); // 2..4
        int tierC = 3 + r.nextInt(4); // 3..6
        offers.add(new MerchantOffer(
                new ItemCost(Items.EMERALD, 8 + tierA * 2 + r.nextInt(3)),
                Optional.of(new ItemCost(Items.GLASS_BOTTLE, 1)),
                new ItemStack(undeadBottleByTier(tierA)),
                3,
                2,
                0.08F));
        offers.add(new MerchantOffer(
                new ItemCost(Items.EMERALD, 12 + tierB * 2 + r.nextInt(4)),
                Optional.of(new ItemCost(Items.GOLD_INGOT, 1 + r.nextInt(2))),
                new ItemStack(undeadBottleByTier(tierB)),
                2,
                3,
                0.1F));
        offers.add(new MerchantOffer(
                new ItemCost(Items.EMERALD, 16 + tierC * 3 + r.nextInt(5)),
                Optional.of(new ItemCost(Items.DIAMOND, 1 + r.nextInt(2))),
                new ItemStack(undeadBottleByTier(tierC)),
                1,
                5,
                0.12F));
    }

    private static Item undeadBottleByTier(int tier) {
        return switch (Mth.clamp(tier, 1, 6)) {
            case 1 -> ModItems.UNDEAD_BOTTLE_1.get();
            case 2 -> ModItems.UNDEAD_BOTTLE_2.get();
            case 3 -> ModItems.UNDEAD_BOTTLE_3.get();
            case 4 -> ModItems.UNDEAD_BOTTLE_4.get();
            case 5 -> ModItems.UNDEAD_BOTTLE_5.get();
            default -> ModItems.UNDEAD_BOTTLE_6.get();
        };
    }

    @Override
    public void tick() {
        super.tick();
        if (!(this.level() instanceof ServerLevel sl) || sl.isClientSide) {
            return;
        }
        if (dogSpawnDelay > 0) {
            dogSpawnDelay--;
        }
        if (dogSpawnDelay == 0 && tickCount > 5 && tickCount % 20 == 0) {
            removeLeashedLlamas(sl);
            ensureGuardDogs(sl);
            refreshGuardDogs(sl);
            dogSpawnDelay = 20;
        }
    }

    private void removeLeashedLlamas(ServerLevel sl) {
        for (TraderLlama llama : sl.getEntitiesOfClass(TraderLlama.class, getBoundingBox().inflate(12.0D))) {
            if (llama.getLeashHolder() == this) {
                llama.discard();
            }
        }
    }

    private void ensureGuardDogs(ServerLevel sl) {
        if (this.guardDogsEverSpawned >= 2) {
            return;
        }
        while (this.guardDogsEverSpawned < 2) {
            Wolf wolf = EntityType.WOLF.create(sl);
            if (wolf == null) {
                break;
            }
            double ox = (random.nextDouble() - 0.5D) * 2.0D;
            double oz = (random.nextDouble() - 0.5D) * 2.0D;
            wolf.moveTo(getX() + ox, getY(), getZ() + oz, random.nextFloat() * 360.0F, 0.0F);
            wolf.getPersistentData().putUUID(QUEST_TRADER_GUARD_TAG, getUUID());
            wolf.setCustomName(net.minecraft.network.chat.Component.literal(randomDogName(random)));
            wolf.setCustomNameVisible(true);
            // Keep guards as regular wolves (not tamed) to avoid vanilla sitting state sticking.
            wolf.setTame(false, false);
            wolf.setOwnerUUID(null);
            wolf.setPersistenceRequired();
            wolf.setOrderedToSit(false);
            wolf.setInSittingPose(false);
            sl.addFreshEntity(wolf);
            sl.getServer().execute(() -> {
                if (wolf.isAlive()) {
                    wolf.setOrderedToSit(false);
                    wolf.setInSittingPose(false);
                }
            });
            this.guardDogsEverSpawned++;
        }
    }

    private void refreshGuardDogs(ServerLevel sl) {
        for (Wolf w : sl.getEntitiesOfClass(Wolf.class, getBoundingBox().inflate(48.0D))) {
            if (!w.getPersistentData().hasUUID(QUEST_TRADER_GUARD_TAG)
                    || !getUUID().equals(w.getPersistentData().getUUID(QUEST_TRADER_GUARD_TAG))) {
                continue;
            }
            w.setOrderedToSit(false);
            w.setInSittingPose(false);
            double dSq = w.distanceToSqr(this);
            if (dSq > GUARD_DOG_TELEPORT_DIST_SQ) {
                w.teleportTo(getX() + (random.nextDouble() - 0.5D) * 2.0D, getY(), getZ() + (random.nextDouble() - 0.5D) * 2.0D);
            } else if (dSq > GUARD_DOG_NAVIGATE_DIST_SQ) {
                w.getNavigation().moveTo(this, 1.12D);
            }
        }
    }

    private static String randomDogName(RandomSource r) {
        String[] a = new String[] {
                "Pixel", "Oslo", "Muffin", "Cromwell", "Nimbus", "Truffle", "Biscuit", "Solea",
                "Gravette", "Ender", "Ruby", "Topaze", "Brume", "Chutney", "Falafel", "Wasabi"
        };
        String[] b = new String[] {
                "I", "II", "Jr", "le Brave", "l’Astucieux", "le Fidèle", "MVP", "★", "du Désert", "de Minuit"
        };
        return a[r.nextInt(a.length)] + " " + b[r.nextInt(b.length)];
    }

}
