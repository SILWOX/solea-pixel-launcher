package com.soleapixel.bmcmod.client;

import org.lwjgl.glfw.GLFW;

import com.mojang.blaze3d.platform.InputConstants;
import com.soleapixel.bmcmod.BmcMod;

import net.minecraft.client.KeyMapping;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.RegisterKeyMappingsEvent;

@EventBusSubscriber(modid = BmcMod.MODID, value = Dist.CLIENT)
public final class BmcModKeyMappings {
    public static final String CATEGORY_BMCMOD = "key.categories.bmcmod";
    public static KeyMapping modeModifierKey;
    public static KeyMapping undeadTotemCloneKey;
    /** Cycle du type de flèche du carquois actif. */
    public static KeyMapping quiverCycleTypeKey;

    private BmcModKeyMappings() {
    }

    @SubscribeEvent
    public static void onRegisterKeys(RegisterKeyMappingsEvent event) {
        modeModifierKey = new KeyMapping(
                "key.bmcmod.mode_modifier",
                InputConstants.Type.KEYSYM,
                GLFW.GLFW_KEY_LEFT_CONTROL,
                CATEGORY_BMCMOD);
        event.register(modeModifierKey);
        undeadTotemCloneKey = new KeyMapping(
                "key.bmcmod.undead_totem_clone",
                InputConstants.Type.KEYSYM,
                GLFW.GLFW_KEY_Y,
                CATEGORY_BMCMOD);
        event.register(undeadTotemCloneKey);
        quiverCycleTypeKey = new KeyMapping(
                "key.bmcmod.quiver_cycle_type",
                InputConstants.Type.KEYSYM,
                GLFW.GLFW_KEY_G,
                CATEGORY_BMCMOD);
        event.register(quiverCycleTypeKey);
    }
}
