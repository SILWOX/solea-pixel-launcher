package com.soleapixel.bmcmod.registry;

import net.minecraft.core.registries.Registries;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.item.alchemy.Potion;

import com.soleapixel.bmcmod.BmcMod;

import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredRegister;

public final class ModPotions {
    public static final DeferredRegister<Potion> POTIONS = DeferredRegister.create(Registries.POTION, BmcMod.MODID);

    public static final DeferredHolder<Potion, Potion> SHRINK = POTIONS.register("shrink",
            () -> new Potion("shrink", new MobEffectInstance(ModMobEffects.SHRINK, 3600, 0)));

    public static final DeferredHolder<Potion, Potion> GROW = POTIONS.register("grow",
            () -> new Potion("grow", new MobEffectInstance(ModMobEffects.GROW, 3600, 0)));

    public static final DeferredHolder<Potion, Potion> VEIN_WHISPER = POTIONS.register("vein_whisper",
            () -> new Potion("vein_whisper", new MobEffectInstance(ModMobEffects.VEIN_WHISPER, 3600, 0)));
    public static final DeferredHolder<Potion, Potion> STRONG_VEIN_WHISPER = POTIONS.register("strong_vein_whisper",
            () -> new Potion("strong_vein_whisper", new MobEffectInstance(ModMobEffects.VEIN_WHISPER, 1800, 1)));
    public static final DeferredHolder<Potion, Potion> UNDEAD_INVASION_1 = POTIONS.register("undead_invasion_1",
            () -> new Potion("undead_invasion_1", new MobEffectInstance(ModMobEffects.UNDEAD_INVASION, 20 * 60 * 10, 0)));
    public static final DeferredHolder<Potion, Potion> UNDEAD_INVASION_2 = POTIONS.register("undead_invasion_2",
            () -> new Potion("undead_invasion_2", new MobEffectInstance(ModMobEffects.UNDEAD_INVASION, 20 * 60 * 10, 1)));
    public static final DeferredHolder<Potion, Potion> UNDEAD_INVASION_3 = POTIONS.register("undead_invasion_3",
            () -> new Potion("undead_invasion_3", new MobEffectInstance(ModMobEffects.UNDEAD_INVASION, 20 * 60 * 10, 2)));
    public static final DeferredHolder<Potion, Potion> UNDEAD_INVASION_4 = POTIONS.register("undead_invasion_4",
            () -> new Potion("undead_invasion_4", new MobEffectInstance(ModMobEffects.UNDEAD_INVASION, 20 * 60 * 10, 3)));
    public static final DeferredHolder<Potion, Potion> UNDEAD_INVASION_5 = POTIONS.register("undead_invasion_5",
            () -> new Potion("undead_invasion_5", new MobEffectInstance(ModMobEffects.UNDEAD_INVASION, 20 * 60 * 10, 4)));
    public static final DeferredHolder<Potion, Potion> UNDEAD_INVASION_6 = POTIONS.register("undead_invasion_6",
            () -> new Potion("undead_invasion_6", new MobEffectInstance(ModMobEffects.UNDEAD_INVASION, 20 * 60 * 10, 5)));
    public static final DeferredHolder<Potion, Potion> END_STORM_1 = POTIONS.register("end_storm_1",
            () -> new Potion("end_storm_1", new MobEffectInstance(ModMobEffects.END_STORM, 20 * 60 * 10, 0)));
    public static final DeferredHolder<Potion, Potion> END_STORM_2 = POTIONS.register("end_storm_2",
            () -> new Potion("end_storm_2", new MobEffectInstance(ModMobEffects.END_STORM, 20 * 60 * 10, 1)));
    public static final DeferredHolder<Potion, Potion> END_STORM_3 = POTIONS.register("end_storm_3",
            () -> new Potion("end_storm_3", new MobEffectInstance(ModMobEffects.END_STORM, 20 * 60 * 10, 2)));
    public static final DeferredHolder<Potion, Potion> END_STORM_4 = POTIONS.register("end_storm_4",
            () -> new Potion("end_storm_4", new MobEffectInstance(ModMobEffects.END_STORM, 20 * 60 * 10, 3)));

    private ModPotions() {
    }
}
