package com.soleapixel.bmcmod.registry;

import net.minecraft.world.effect.MobEffect;
import net.minecraft.world.effect.MobEffectCategory;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraft.world.entity.ai.attributes.Attributes;

import com.soleapixel.bmcmod.BmcMod;

import net.minecraft.core.registries.Registries;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredRegister;

public final class ModMobEffects {
    public static final DeferredRegister<MobEffect> MOB_EFFECTS = DeferredRegister.create(Registries.MOB_EFFECT, BmcMod.MODID);

    public static final DeferredHolder<MobEffect, MobEffect> SHRINK = MOB_EFFECTS.register("shrink", ShrinkEffect::new);
    public static final DeferredHolder<MobEffect, MobEffect> GROW = MOB_EFFECTS.register("grow", GrowEffect::new);
    public static final DeferredHolder<MobEffect, MobEffect> VEIN_WHISPER = MOB_EFFECTS.register("vein_whisper", VeinWhisperEffect::new);
    public static final DeferredHolder<MobEffect, MobEffect> BLEED = MOB_EFFECTS.register("bleed", BleedEffect::new);
    public static final DeferredHolder<MobEffect, MobEffect> PRISM = MOB_EFFECTS.register("prism", PrismEffect::new);
    public static final DeferredHolder<MobEffect, MobEffect> UNDEAD_INVASION = MOB_EFFECTS.register("undead_invasion", UndeadInvasionEffect::new);
    public static final DeferredHolder<MobEffect, MobEffect> END_STORM = MOB_EFFECTS.register("end_storm", EndStormEffect::new);

    private ModMobEffects() {
    }

    private static final class ShrinkEffect extends MobEffect {
        ShrinkEffect() {
            super(MobEffectCategory.NEUTRAL, 0x2D8B74);
            this.addAttributeModifier(
                    Attributes.SCALE,
                    BmcMod.loc("effect.shrink"),
                    AttributeModifier.Operation.ADD_VALUE,
                    amplifier -> -0.55);
        }
    }

    private static final class GrowEffect extends MobEffect {
        GrowEffect() {
            super(MobEffectCategory.NEUTRAL, 0xFF9933);
            this.addAttributeModifier(
                    Attributes.SCALE,
                    BmcMod.loc("effect.grow"),
                    AttributeModifier.Operation.ADD_VALUE,
                    amplifier -> 0.55);
        }
    }

    private static final class VeinWhisperEffect extends MobEffect {
        VeinWhisperEffect() {
            super(MobEffectCategory.BENEFICIAL, 0x7FDFFF);
        }
    }

    private static final class BleedEffect extends MobEffect {
        BleedEffect() {
            super(MobEffectCategory.HARMFUL, 0x8B0000);
        }

        @Override
        public boolean applyEffectTick(LivingEntity entity, int amplifier) {
            if (entity.getHealth() > 0.5F) {
                entity.hurt(entity.damageSources().magic(), amplifier + 1);
            }
            return true;
        }

        @Override
        public boolean shouldApplyEffectTickThisTick(int duration, int amplifier) {
            return duration % 20 == 0;
        }
    }

    private static final class PrismEffect extends MobEffect {
        PrismEffect() {
            super(MobEffectCategory.BENEFICIAL, 0x3CF3FF);
        }
    }

    private static final class UndeadInvasionEffect extends MobEffect {
        /**
         * Effet sans référence à {@link ModParticles} : évite tout chaînage registry au chargement de la classe
         * (cause typique d’{@code ExceptionInInitializerError} si l’ordre des registres ou l’API NeoForge diffère).
         * La particule custom reste utilisable via {@code ModParticles.UNDEAD_INVASION} pour les FX serveur.
         */
        UndeadInvasionEffect() {
            super(MobEffectCategory.NEUTRAL, 0xE1B52E);
        }
    }

    private static final class EndStormEffect extends MobEffect {
        EndStormEffect() {
            super(MobEffectCategory.NEUTRAL, 0xE047FF);
        }
    }
}
