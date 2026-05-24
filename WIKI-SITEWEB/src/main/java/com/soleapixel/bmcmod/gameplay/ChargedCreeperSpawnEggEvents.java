package com.soleapixel.bmcmod.gameplay;

import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LightningBolt;
import net.minecraft.world.entity.MobSpawnType;
import net.minecraft.world.entity.monster.Creeper;

import com.soleapixel.bmcmod.BmcMod;

import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.entity.EntityJoinLevelEvent;

/**
 * Marque les creepers issus de l’œuf « chargé » (clic joueur) comme {@link Creeper#isPowered()}.
 */
@EventBusSubscriber(modid = BmcMod.MODID)
public final class ChargedCreeperSpawnEggEvents {
    /** Profondeur de récursion pour {@link com.soleapixel.bmcmod.item.ChargedCreeperSpawnEggItem#use}. */
    private static final ThreadLocal<Integer> CHARGED_SPAWN_DEPTH = ThreadLocal.withInitial(() -> 0);

    private ChargedCreeperSpawnEggEvents() {
    }

    public static void beginChargedSpawn() {
        CHARGED_SPAWN_DEPTH.set(CHARGED_SPAWN_DEPTH.get() + 1);
    }

    public static void endChargedSpawn() {
        CHARGED_SPAWN_DEPTH.set(Math.max(0, CHARGED_SPAWN_DEPTH.get() - 1));
    }

    private static boolean isChargedSpawnPending() {
        return CHARGED_SPAWN_DEPTH.get() > 0;
    }

    @SubscribeEvent
    public static void onEntityJoin(EntityJoinLevelEvent event) {
        if (event.getLevel().isClientSide() || !(event.getEntity() instanceof Creeper creeper)) {
            return;
        }
        if (creeper.getSpawnType() != MobSpawnType.SPAWN_EGG) {
            return;
        }
        if (!isChargedSpawnPending()) {
            return;
        }
        applyCharge(creeper);
    }

    /**
     * {@link Creeper#DATA_IS_POWERED} est privé : on réutilise la voie vanilla (éclair « décoratif » + {@link Creeper#thunderHit}).
     */
    public static void applyCharge(Creeper creeper) {
        if (!(creeper.level() instanceof ServerLevel sl) || creeper.isPowered()) {
            return;
        }
        LightningBolt bolt = EntityType.LIGHTNING_BOLT.create(sl);
        if (bolt == null) {
            return;
        }
        bolt.moveTo(creeper.getX(), creeper.getY(), creeper.getZ(), 0.0F, 0.0F);
        bolt.setVisualOnly(true);
        sl.addFreshEntity(bolt);
        creeper.thunderHit(sl, bolt);
    }
}
