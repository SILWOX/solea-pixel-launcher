package com.soleapixel.bmcmod.morph;

import java.util.List;
import java.util.Optional;
import java.util.stream.Stream;

import net.minecraft.core.Holder;
import net.minecraft.core.Registry;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.Tag;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.attributes.Attribute;
import net.minecraft.world.entity.ai.attributes.AttributeInstance;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.player.Player;

/**
 * Copie temporairement les attributs de base du mob (PV max, vitesse, taille, dégâts…) sur le joueur
 * pendant la métamorphose, avec sauvegarde pour restauration au dé-morph.
 */
public final class MorphBodyAttributes {
    public static final String TAG_ATTR_BACKUP = "BmcModMorphAttrBackup";

    private static final String HEALTH_FRAC = "_health_frac";

    /** Attributs copiés depuis le template ; {@link Attributes#SCALE} n’est pas copié (hitbox via {@link MorphHitboxEvents}). */
    private static final List<Holder<Attribute>> TO_COPY = List.of(
            Attributes.MAX_HEALTH,
            Attributes.MOVEMENT_SPEED,
            Attributes.FLYING_SPEED,
            Attributes.ATTACK_DAMAGE,
            Attributes.ATTACK_SPEED,
            Attributes.ATTACK_KNOCKBACK,
            Attributes.ARMOR,
            Attributes.ARMOR_TOUGHNESS,
            Attributes.KNOCKBACK_RESISTANCE,
            Attributes.STEP_HEIGHT,
            Attributes.BLOCK_INTERACTION_RANGE,
            Attributes.ENTITY_INTERACTION_RANGE,
            Attributes.GRAVITY);

    /** Sauvegarde inclut {@link Attributes#SCALE} (retiré de la copie directe depuis le template). */
    private static final List<Holder<Attribute>> TO_BACKUP =
            Stream.concat(TO_COPY.stream(), Stream.of(Attributes.SCALE)).toList();

    private MorphBodyAttributes() {
    }

    public static boolean hasBackup(Player player) {
        CompoundTag root = player.getPersistentData();
        return root.contains(TAG_ATTR_BACKUP, Tag.TAG_COMPOUND) && !root.getCompound(TAG_ATTR_BACKUP).isEmpty();
    }

    /** Restaure les bases d’attributs si une sauvegarde existe (ne modifie pas l’état morph du joueur). */
    public static void restore(Player player) {
        CompoundTag root = player.getPersistentData();
        if (!root.contains(TAG_ATTR_BACKUP, Tag.TAG_COMPOUND)) {
            return;
        }
        CompoundTag backup = root.getCompound(TAG_ATTR_BACKUP);
        float healthFrac = backup.contains(HEALTH_FRAC, Tag.TAG_FLOAT) ? backup.getFloat(HEALTH_FRAC) : 1.0F;
        for (String key : backup.getAllKeys()) {
            if (HEALTH_FRAC.equals(key) || !backup.contains(key, Tag.TAG_DOUBLE)) {
                continue;
            }
            ResourceLocation id = ResourceLocation.tryParse(key);
            if (id == null) {
                continue;
            }
            Registry<Attribute> reg = BuiltInRegistries.ATTRIBUTE;
            Attribute attr = reg.get(id);
            if (attr == null) {
                continue;
            }
            AttributeInstance inst = player.getAttribute(reg.wrapAsHolder(attr));
            if (inst != null) {
                inst.setBaseValue(backup.getDouble(key));
            }
        }
        root.remove(TAG_ATTR_BACKUP);
        player.refreshDimensions();
        float max = player.getMaxHealth();
        player.setHealth(Mth.clamp(max * healthFrac, 1.0E-4F, max));
    }

    public static void applyFromSoul(ServerPlayer player, CompoundTag soul) {
        if (!MorphPlayerState.isMorphed(player) || hasBackup(player)) {
            return;
        }
        CompoundTag cleaned = MorphSoulSanitizer.sanitize(soul.copy());
        Optional<Entity> opt = EntityType.create(cleaned, player.level());
        if (opt.isEmpty() || !(opt.get() instanceof LivingEntity template)) {
            return;
        }
        try {
            CompoundTag backup = new CompoundTag();
            for (Holder<Attribute> holder : TO_BACKUP) {
                Attribute attr = holder.value();
                AttributeInstance pInst = player.getAttribute(holder);
                if (pInst == null) {
                    continue;
                }
                ResourceLocation key = BuiltInRegistries.ATTRIBUTE.getKey(attr);
                if (key == null) {
                    continue;
                }
                backup.putDouble(key.toString(), pInst.getBaseValue());
            }
            float frac = player.getMaxHealth() > 0 ? player.getHealth() / player.getMaxHealth() : 1.0F;
            backup.putFloat(HEALTH_FRAC, frac);

            for (Holder<Attribute> holder : TO_COPY) {
                Attribute attr = holder.value();
                AttributeInstance tInst = template.getAttribute(holder);
                AttributeInstance pInst = player.getAttribute(holder);
                if (tInst == null || pInst == null) {
                    continue;
                }
                pInst.setBaseValue(tInst.getBaseValue());
            }

            AttributeInstance scaleAttr = player.getAttribute(Attributes.SCALE);
            if (scaleAttr != null) {
                scaleAttr.setBaseValue(1.0);
            }
            ResourceLocation morphEntityId = MorphAppearanceIds.soulEntityId(cleaned);
            clampMorphMovementSpeedExtremes(player, morphEntityId);

            player.getPersistentData().put(TAG_ATTR_BACKUP, backup);
            player.refreshDimensions();
            float max = player.getMaxHealth();
            player.setHealth(Mth.clamp(max * frac, 0.5F, max));
        } finally {
            template.remove(Entity.RemovalReason.DISCARDED);
        }
    }

    private static void clampMorphMovementSpeedExtremes(ServerPlayer player, ResourceLocation morphEntityId) {
        double perMobLand = MorphAppearanceIds.morphLandSpeedBaseCap(morphEntityId);
        // Keep morph run speed near vanilla player speed (prevents sprint-speed bug during metamorph).
        double landCap = Math.min(0.10D, perMobLand);
        AttributeInstance move = player.getAttribute(Attributes.MOVEMENT_SPEED);
        if (move != null) {
            double v = move.getBaseValue();
            move.setBaseValue(Math.min(v, landCap));
        }
        AttributeInstance fly = player.getAttribute(Attributes.FLYING_SPEED);
        if (fly != null) {
            double fv = fly.getBaseValue();
            if (fv > MorphAppearanceIds.DEFAULT_MORPH_MOVEMENT_CAP) {
                fly.setBaseValue(MorphAppearanceIds.DEFAULT_MORPH_MOVEMENT_CAP);
            }
        }
    }

    /** Connexion : orphelin backup, ou morph sans backup (anciennes sauvegardes). */
    public static void handleLogin(ServerPlayer player) {
        if (!MorphPlayerState.isMorphed(player) && hasBackup(player)) {
            restore(player);
        } else if (MorphPlayerState.isMorphed(player) && !hasBackup(player)) {
            applyFromSoul(player, MorphPlayerState.getMorphSoul(player));
        }
    }
}
