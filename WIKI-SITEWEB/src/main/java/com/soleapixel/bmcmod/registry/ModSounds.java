package com.soleapixel.bmcmod.registry;

import com.soleapixel.bmcmod.BmcMod;

import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.sounds.SoundEvent;

import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredRegister;

public final class ModSounds {
    public static final DeferredRegister<SoundEvent> SOUND_EVENTS = DeferredRegister.create(Registries.SOUND_EVENT, BmcMod.MODID);

    /**
     * Fichier audio : {@code assets/bmcmod/sounds/music/beyond_the_endermans.ogg} ; dans {@code sounds.json} le champ {@code name}
     * doit être {@code bmcmod:music/...} (sinon Minecraft cherche sous {@code minecraft:}).
     */
    public static final DeferredHolder<SoundEvent, SoundEvent> MUSIC_DISC_BEYOND_THE_ENDERMAN = SOUND_EVENTS.register("music_disc.beyond_the_enderman",
            () -> SoundEvent.createVariableRangeEvent(ResourceLocation.fromNamespaceAndPath(BmcMod.MODID, "music_disc.beyond_the_enderman")));

    /**
     * Bruits d’ambiance en poursuite : grognements zombie + craquements de coffre (fichiers vanilla, voir {@code sounds.json}).
     */
    public static final DeferredHolder<SoundEvent, SoundEvent> ENTITY_MIMIC_CHEST_CHASE = SOUND_EVENTS.register("entity.mimic_chest.chase",
            () -> SoundEvent.createVariableRangeEvent(ResourceLocation.fromNamespaceAndPath(BmcMod.MODID, "entity.mimic_chest.chase")));

    private ModSounds() {
    }
}
