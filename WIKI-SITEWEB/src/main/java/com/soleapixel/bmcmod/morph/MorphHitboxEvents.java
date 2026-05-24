package com.soleapixel.bmcmod.morph;

import java.util.Optional;

import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.Tag;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityDimensions;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Pose;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.entity.EntityEvent;

import com.soleapixel.bmcmod.BmcMod;

/**
 * Serveur : hitbox / œil du joueur métamorphosé = ceux du mob d’origine pour la pose courante.
 *
 * @see com.soleapixel.bmcmod.client.MorphHitboxClientEvents
 */
@EventBusSubscriber(modid = BmcMod.MODID)
public final class MorphHitboxEvents {
    private MorphHitboxEvents() {
    }

    /** Recrée un mob à partir de l’âme (hors monde) et lit ses dimensions pour une pose donnée. */
    public static Optional<EntityDimensions> computeMorphDimensions(Level level, CompoundTag soul, Pose pose) {
        CompoundTag cleaned = MorphSoulSanitizer.sanitize(soul.copy());
        Optional<Entity> opt = EntityType.create(cleaned, level);
        if (opt.isEmpty() || !(opt.get() instanceof LivingEntity template)) {
            return Optional.empty();
        }
        try {
            template.setPose(pose);
            return Optional.of(template.getDimensions(pose));
        } finally {
            template.remove(Entity.RemovalReason.DISCARDED);
        }
    }

    @SubscribeEvent
    public static void onEntitySize(EntityEvent.Size event) {
        if (!(event.getEntity() instanceof ServerPlayer player)) {
            return;
        }
        if (player.level().isClientSide()) {
            return;
        }
        if (!MorphPlayerState.isMorphed(player)) {
            return;
        }
        CompoundTag soul = MorphPlayerState.getMorphSoul(player);
        if (!soul.contains("id", Tag.TAG_STRING)) {
            return;
        }
        computeMorphDimensions(player.level(), soul, event.getPose()).ifPresent(event::setNewSize);
    }
}
