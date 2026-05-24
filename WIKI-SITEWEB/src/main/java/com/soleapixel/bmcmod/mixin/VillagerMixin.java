package com.soleapixel.bmcmod.mixin;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.ModifyVariable;

import net.minecraft.world.entity.npc.Villager;
import net.minecraft.world.entity.npc.VillagerData;
import net.minecraft.world.entity.npc.VillagerProfession;

import com.soleapixel.bmcmod.villager.ModProfessions;
import com.soleapixel.bmcmod.villager.WitchCuringEvents;

/**
 * Évite d’enregistrer un autre métier (NONE, fermier, etc.) pour un villageoi guéri : c’est
 * l’appel fréquent à setVillagerData qui nullait offers et recréait l’inventaire de trade, fermait
 * la GUI et mélangeait l’aléa des offres.
 */
@Mixin(Villager.class)
public abstract class VillagerMixin {
    @ModifyVariable(method = "setVillagerData", at = @At("HEAD"), argsOnly = true)
    private VillagerData bmcmod$forceCuredWitchData(VillagerData value) {
        Villager self = (Villager) (Object) this;
        if (!self.getPersistentData().getBoolean(WitchCuringEvents.TAG_CURED_WITCH_LOCK)) {
            return value;
        }
        VillagerProfession c = ModProfessions.CURED_WITCH.get();
        if (c.equals(value.getProfession()) || c == value.getProfession()) {
            return value;
        }
        return new VillagerData(value.getType(), c, value.getLevel());
    }
}
