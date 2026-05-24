package com.soleapixel.bmcmod.morph;

import net.minecraft.nbt.CompoundTag;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.ai.attributes.AttributeInstance;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.phys.Vec3;

import com.soleapixel.bmcmod.BmcMod;

/**
 * Règles de déplacement spécifiques au mob (ex. slime : quasi pas de glisse au sol ; l’impulsion au saut est dans
 * {@link MorphSlimeJumpEvents}).
 */
public final class MorphMovementRules {
    private static final ResourceLocation SLIME_GROUND_ID =
            ResourceLocation.fromNamespaceAndPath(BmcMod.MODID, "morph_slime_ground");
    private static final AttributeModifier SLIME_GROUND_LOCK =
            new AttributeModifier(SLIME_GROUND_ID, -0.985, AttributeModifier.Operation.ADD_MULTIPLIED_TOTAL);

    private MorphMovementRules() {
    }

    public static void clear(ServerPlayer player) {
        AttributeInstance move = player.getAttribute(Attributes.MOVEMENT_SPEED);
        if (move != null) {
            move.removeModifier(SLIME_GROUND_ID);
        }
    }

    public static boolean allowsCreativeLikeFlight(ServerPlayer player) {
        return player.isSpectator() || (player.isCreative() && player.getAbilities().flying);
    }

    public static void tick(ServerPlayer player) {
        CompoundTag soul = MorphPlayerState.getMorphSoul(player);
        var entityId = MorphAppearanceIds.soulEntityId(soul);
        if (entityId == null) {
            clear(player);
            return;
        }
        if (!MorphAppearanceIds.morphIsSlimeLike(entityId)) {
            clear(player);
            return;
        }
        if (allowsCreativeLikeFlight(player)) {
            clear(player);
            return;
        }
        if (player.getAbilities().mayfly && player.getAbilities().flying) {
            clear(player);
            return;
        }

        Vec3 v = player.getDeltaMovement();
        AttributeInstance move = player.getAttribute(Attributes.MOVEMENT_SPEED);
        if (player.onGround()) {
            if (move != null && !move.hasModifier(SLIME_GROUND_ID)) {
                move.addTransientModifier(SLIME_GROUND_LOCK);
            }
            player.setDeltaMovement(0.0, v.y, 0.0);
        } else {
            if (move != null) {
                move.removeModifier(SLIME_GROUND_ID);
            }
        }
    }
}
