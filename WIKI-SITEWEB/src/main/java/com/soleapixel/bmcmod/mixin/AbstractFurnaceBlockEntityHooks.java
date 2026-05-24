package com.soleapixel.bmcmod.mixin;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;
import org.spongepowered.asm.mixin.gen.Invoker;

import it.unimi.dsi.fastutil.objects.Object2IntOpenHashMap;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.block.entity.AbstractFurnaceBlockEntity;
import net.minecraft.world.phys.Vec3;

/**
 * Accès à {@code recipesUsed} et au helper static {@code createExperience} pour surcharger le comportement dans
 * {@link com.soleapixel.bmcmod.block.endstonefurnace.EndstoneFurnaceBlockEntity}.
 */
@Mixin(AbstractFurnaceBlockEntity.class)
public interface AbstractFurnaceBlockEntityHooks {
    @Accessor("recipesUsed")
    Object2IntOpenHashMap<ResourceLocation> bmcmod$recipesUsed();

    @Invoker("createExperience")
    static void bmcmod$createExperience(ServerLevel level, Vec3 pos, int count, float xpPerUnit) {
        throw new AssertionError();
    }
}
