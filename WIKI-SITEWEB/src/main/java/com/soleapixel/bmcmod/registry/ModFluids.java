package com.soleapixel.bmcmod.registry;

import com.soleapixel.bmcmod.BmcMod;

import net.minecraft.core.registries.Registries;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.world.level.block.LiquidBlock;
import net.minecraft.world.level.material.FlowingFluid;
import net.minecraft.world.level.material.Fluid;
import net.neoforged.neoforge.common.SoundActions;
import net.neoforged.neoforge.fluids.BaseFlowingFluid;
import net.neoforged.neoforge.fluids.FluidType;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredRegister;
import net.neoforged.neoforge.registries.NeoForgeRegistries;

/**
 * Fluides du mod : liquide d’expérience (écoulement plus lent / moins étalé que l’eau, pas de source infinie).
 */
public final class ModFluids {
    public static final DeferredRegister<FluidType> FLUID_TYPES = DeferredRegister.create(NeoForgeRegistries.Keys.FLUID_TYPES, BmcMod.MODID);
    public static final DeferredRegister<Fluid> FLUIDS = DeferredRegister.create(Registries.FLUID, BmcMod.MODID);

    public static final DeferredHolder<FluidType, FluidType> EXPERIENCE_FLUID_TYPE = FLUID_TYPES.register("experience_liquid", () -> new FluidType(
            FluidType.Properties.create()
                    .descriptionId("block." + BmcMod.MODID + ".experience_liquid")
                    // motionScale par défaut (~0.014) : une valeur proche de 1.0 multiplie énormément le courant et éjecte joueurs / items.
                    .canPushEntity(false)
                    .canSwim(false)
                    .canDrown(true)
                    .canExtinguish(false)
                    .canConvertToSource(false)
                    .supportsBoating(false)
                    .canHydrate(false)
                    .density(1028)
                    .viscosity(1100)
                    .temperature(300)
                    .lightLevel(9)
                    .sound(SoundActions.BUCKET_FILL, SoundEvents.BUCKET_FILL)
                    .sound(SoundActions.BUCKET_EMPTY, SoundEvents.BUCKET_EMPTY)));

    public static final DeferredHolder<Fluid, Fluid> EXPERIENCE_STILL = FLUIDS.register("experience_liquid", () -> new BaseFlowingFluid.Source(experienceFluidProperties()));
    public static final DeferredHolder<Fluid, Fluid> EXPERIENCE_FLOWING = FLUIDS.register("flowing_experience_liquid", () -> new BaseFlowingFluid.Flowing(experienceFluidProperties()));

    private static BaseFlowingFluid.Properties experienceFluidProperties() {
        return new BaseFlowingFluid.Properties(
                () -> EXPERIENCE_FLUID_TYPE.get(),
                () -> EXPERIENCE_STILL.get(),
                () -> EXPERIENCE_FLOWING.get())
                .block(() -> (LiquidBlock) ModBlocks.EXPERIENCE_LIQUID.get())
                .bucket(() -> ModItems.EXPERIENCE_LIQUID_BUCKET.get())
                .tickRate(11)
                .slopeFindDistance(2)
                .levelDecreasePerBlock(2)
                .explosionResistance(100.0F);
    }

    private ModFluids() {
    }
}
