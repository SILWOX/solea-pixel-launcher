package com.soleapixel.bmcmod.registry;

import com.soleapixel.bmcmod.BmcMod;

import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.damagesource.DamageType;

public final class ModDamageTypes {
    public static final ResourceKey<DamageType> QUICKSAND = ResourceKey.create(Registries.DAMAGE_TYPE, BmcMod.loc("quicksand"));

    private ModDamageTypes() {
    }
}
