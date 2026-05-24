package com.soleapixel.bmcmod.morph;

import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.phys.Vec3;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.entity.living.LivingEvent;

import com.soleapixel.bmcmod.BmcMod;

/**
 * Impulsion « slime » au moment du saut (évite le champ {@code jumping} protégé côté serveur).
 */
@EventBusSubscriber(modid = BmcMod.MODID)
public final class MorphSlimeJumpEvents {
    private MorphSlimeJumpEvents() {
    }

    @SubscribeEvent
    public static void onLivingJump(LivingEvent.LivingJumpEvent event) {
        if (!(event.getEntity() instanceof ServerPlayer sp) || sp.level().isClientSide()) {
            return;
        }
        if (!MorphPlayerState.isMorphed(sp)) {
            return;
        }
        var id = MorphAppearanceIds.soulEntityId(MorphPlayerState.getMorphSoul(sp));
        if (id == null || !MorphAppearanceIds.morphIsSlimeLike(id)) {
            return;
        }
        if (MorphMovementRules.allowsCreativeLikeFlight(sp)) {
            return;
        }
        if (sp.getAbilities().mayfly && sp.getAbilities().flying) {
            return;
        }
        Vec3 look = sp.getLookAngle();
        sp.setDeltaMovement(look.x * 0.38, 0.52, look.z * 0.38);
    }
}
