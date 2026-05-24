package com.soleapixel.bmcmod.registry;

import com.soleapixel.bmcmod.BmcMod;
import com.soleapixel.bmcmod.entity.BountyHunterEntity;
import com.soleapixel.bmcmod.entity.Blink;
import com.soleapixel.bmcmod.entity.CloneEntity;
import com.soleapixel.bmcmod.entity.DiamondGolem;
import com.soleapixel.bmcmod.entity.Dummy;
import com.soleapixel.bmcmod.entity.EndGolem;
import com.soleapixel.bmcmod.entity.Endling;
import com.soleapixel.bmcmod.entity.MimicChest;
import com.soleapixel.bmcmod.entity.QuestTrader;
import com.soleapixel.bmcmod.entity.RadiantSlime;
import com.soleapixel.bmcmod.entity.SkeletonVillager;
import com.soleapixel.bmcmod.entity.UndeadIllager;
import com.soleapixel.bmcmod.entity.Vlinx;
import com.soleapixel.bmcmod.entity.projectile.ScytheTornadoProjectile;
import com.soleapixel.bmcmod.entity.projectile.UnstablePearlProjectile;
import com.soleapixel.bmcmod.entity.projectile.VoidShardProjectile;
import com.soleapixel.bmcmod.entity.vehicle.SunwoodBoat;
import com.soleapixel.bmcmod.entity.vehicle.SunwoodChestBoat;

import net.minecraft.core.registries.Registries;
import net.minecraft.world.entity.EntityDimensions;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.MobCategory;

import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredRegister;

public final class ModEntities {
    public static final DeferredRegister<EntityType<?>> ENTITY_TYPES = DeferredRegister.create(Registries.ENTITY_TYPE, BmcMod.MODID);

    public static final DeferredHolder<EntityType<?>, EntityType<SunwoodBoat>> SUNWOOD_BOAT = ENTITY_TYPES.register("sunwood_boat",
            () -> EntityType.Builder.<SunwoodBoat>of(SunwoodBoat::new, MobCategory.MISC)
                    .sized(1.375F, 0.5625F)
                    .clientTrackingRange(10)
                    .build("sunwood_boat"));
    public static final DeferredHolder<EntityType<?>, EntityType<SunwoodChestBoat>> SUNWOOD_CHEST_BOAT = ENTITY_TYPES.register("sunwood_chest_boat",
            () -> EntityType.Builder.<SunwoodChestBoat>of(SunwoodChestBoat::new, MobCategory.MISC)
                    .sized(1.375F, 0.5625F)
                    .clientTrackingRange(10)
                    .build("sunwood_chest_boat"));

    public static final DeferredHolder<EntityType<?>, EntityType<DiamondGolem>> DIAMOND_GOLEM = ENTITY_TYPES.register("diamond_golem",
            () -> EntityType.Builder.of(DiamondGolem::new, MobCategory.CREATURE)
                    .sized(1.4F, 2.7F)
                    .clientTrackingRange(10)
                    .build("diamond_golem"));

    public static final DeferredHolder<EntityType<?>, EntityType<VoidShardProjectile>> VOID_SHARD_PROJECTILE = ENTITY_TYPES.register("void_shard_projectile",
            () -> EntityType.Builder.<VoidShardProjectile>of(VoidShardProjectile::new, MobCategory.MISC)
                    .sized(0.35F, 0.35F)
                    .clientTrackingRange(4)
                    .updateInterval(10)
                    .build("void_shard_projectile"));

    public static final DeferredHolder<EntityType<?>, EntityType<UnstablePearlProjectile>> UNSTABLE_PEARL_PROJECTILE = ENTITY_TYPES.register("unstable_pearl_projectile",
            () -> EntityType.Builder.<UnstablePearlProjectile>of(UnstablePearlProjectile::new, MobCategory.MISC)
                    .sized(0.35F, 0.35F)
                    .clientTrackingRange(6)
                    .updateInterval(10)
                    .build("unstable_pearl_projectile"));

    public static final DeferredHolder<EntityType<?>, EntityType<ScytheTornadoProjectile>> SCYTHE_TORNADO = ENTITY_TYPES.register("scythe_tornado",
            () -> EntityType.Builder.<ScytheTornadoProjectile>of(ScytheTornadoProjectile::new, MobCategory.MISC)
                    .sized(0.6F, 1.4F)
                    .clientTrackingRange(8)
                    .updateInterval(1)
                    .build("scythe_tornado"));

    public static final DeferredHolder<EntityType<?>, EntityType<Blink>> BLINK = ENTITY_TYPES.register("blink",
            () -> EntityType.Builder.of(Blink::new, MobCategory.MONSTER)
                    // Anneau ~10.5 px + demi-tête 4 px (échelle 1/16) → largeur ~1.8 ; hauteur pour tête + bâtons.
                    .sized(1.82F, 1.48F)
                    .eyeHeight(1.05F)
                    .clientTrackingRange(8)
                    .build("blink"));

    public static final DeferredHolder<EntityType<?>, EntityType<Endling>> ENDLING = ENTITY_TYPES.register("endling",
            () -> EntityType.Builder.of(Endling::new, MobCategory.MONSTER)
                    .sized(0.45F, 0.95F)
                    .clientTrackingRange(8)
                    .build("endling"));

    public static final DeferredHolder<EntityType<?>, EntityType<MimicChest>> MIMIC_CHEST = ENTITY_TYPES.register("mimic_chest",
            () -> EntityType.Builder.of(MimicChest::new, MobCategory.MONSTER)
                    .sized(1.0F, 0.95F)
                    .eyeHeight(0.52F)
                    .clientTrackingRange(8)
                    .build("mimic_chest"));

    public static final DeferredHolder<EntityType<?>, EntityType<BountyHunterEntity>> BOUNTY_HUNTER = ENTITY_TYPES.register("bounty_hunter",
            () -> EntityType.Builder.of(BountyHunterEntity::new, MobCategory.MONSTER)
                    .sized(0.6F, 1.95F)
                    .eyeHeight(1.74F)
                    .clientTrackingRange(10)
                    .build("bounty_hunter"));

    public static final DeferredHolder<EntityType<?>, EntityType<EndGolem>> END_GOLEM = ENTITY_TYPES.register("end_golem",
            () -> EntityType.Builder.of(EndGolem::new, MobCategory.MONSTER)
                    .sized(2.1F, 4.05F)
                    .clientTrackingRange(10)
                    .fireImmune()
                    .build("end_golem"));

    /** Base identique au slime vanilla ; {@link net.minecraft.world.entity.monster.Slime} multiplie déjà par {@code getSize()}. */
    public static final DeferredHolder<EntityType<?>, EntityType<RadiantSlime>> RADIANT_SLIME = ENTITY_TYPES.register("radiant_slime",
            () -> EntityType.Builder.of(RadiantSlime::new, MobCategory.MONSTER)
                    .sized(0.52F, 0.52F)
                    .eyeHeight(0.325F)
                    .clientTrackingRange(10)
                    .build("radiant_slime"));

    public static final DeferredHolder<EntityType<?>, EntityType<SkeletonVillager>> SKELETON_VILLAGER = ENTITY_TYPES.register("skeleton_villager",
            () -> EntityType.Builder.of(SkeletonVillager::new, MobCategory.MONSTER)
                    .sized(0.6F, 1.99F)
                    .eyeHeight(1.74F)
                    .clientTrackingRange(8)
                    .build("skeleton_villager"));

    public static final DeferredHolder<EntityType<?>, EntityType<UndeadIllager>> UNDEAD_ILLAGER = ENTITY_TYPES.register("undead_illager",
            () -> EntityType.Builder.of(UndeadIllager::new, MobCategory.MONSTER)
                    .sized(0.6F, 1.99F)
                    .eyeHeight(1.74F)
                    .clientTrackingRange(10)
                    .build("undead_illager"));

    public static final DeferredHolder<EntityType<?>, EntityType<Vlinx>> VLINX = ENTITY_TYPES.register("vlinx",
            () -> EntityType.Builder.of(Vlinx::new, MobCategory.MONSTER)
                    .sized(0.5F, 0.9F)
                    .eyeHeight(0.45F)
                    .clientTrackingRange(8)
                    .build("vlinx"));

    public static final DeferredHolder<EntityType<?>, EntityType<QuestTrader>> QUEST_TRADER = ENTITY_TYPES.register("quest_trader",
            () -> EntityType.Builder.of(QuestTrader::new, MobCategory.CREATURE)
                    .sized(0.6F, 1.95F)
                    .eyeHeight(1.74F)
                    .clientTrackingRange(10)
                    .build("quest_trader"));

    public static final DeferredHolder<EntityType<?>, EntityType<Dummy>> DUMMY = ENTITY_TYPES.register("dummy",
            () -> EntityType.Builder.<Dummy>of(Dummy::new, MobCategory.MISC)
                    .sized(0.5F, 1.975F)
                    .clientTrackingRange(8)
                    .build("dummy"));
    public static final DeferredHolder<EntityType<?>, EntityType<CloneEntity>> CLONE = ENTITY_TYPES.register("clone",
            () -> {
                EntityDimensions playerDims = EntityType.PLAYER.getDimensions();
                return EntityType.Builder.<CloneEntity>of(CloneEntity::new, MobCategory.CREATURE)
                        .sized(playerDims.width(), playerDims.height())
                        .eyeHeight(playerDims.eyeHeight())
                        .clientTrackingRange(10)
                        .build("clone");
            });

    private ModEntities() {
    }
}
