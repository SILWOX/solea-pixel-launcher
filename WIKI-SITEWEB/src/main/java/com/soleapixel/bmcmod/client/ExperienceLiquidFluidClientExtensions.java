package com.soleapixel.bmcmod.client;

import org.joml.Vector3f;

import com.soleapixel.bmcmod.BmcMod;

import net.minecraft.client.Camera;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.resources.ResourceLocation;
import net.neoforged.neoforge.client.extensions.common.IClientFluidTypeExtensions;

public enum ExperienceLiquidFluidClientExtensions implements IClientFluidTypeExtensions {
    INSTANCE;

    private static final ResourceLocation STILL = BmcMod.loc("block/fluid/experience_still");
    private static final ResourceLocation FLOW = BmcMod.loc("block/fluid/experience_flow");

    /** Brouillard vert (canaux 0–1) quand la caméra est dans le liquide d’XP. */
    private static final Vector3f FOG_COLOR = new Vector3f(0.08F, 0.72F, 0.18F);

    @Override
    public ResourceLocation getStillTexture() {
        return STILL;
    }

    @Override
    public ResourceLocation getFlowingTexture() {
        return FLOW;
    }

    @Override
    public int getTintColor() {
        return 0xFFFFFFFF;
    }

    @Override
    public Vector3f modifyFogColor(
            Camera camera,
            float partialTick,
            ClientLevel level,
            int renderDistance,
            float darkenWorldAmount,
            Vector3f fluidFogColor) {
        return FOG_COLOR;
    }
}
