package com.soleapixel.bmcmod.emerald;

import java.util.List;
import java.util.Map;

import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.item.ArmorItem;
import net.minecraft.world.item.ArmorMaterial;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.Tier;
import net.minecraft.world.item.crafting.Ingredient;
import net.minecraft.server.level.ServerPlayer;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.tick.PlayerTickEvent;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredRegister;

import com.soleapixel.bmcmod.BmcMod;
import com.soleapixel.bmcmod.registry.ModItems;
import com.soleapixel.bmcmod.equipment.EmeraldToolTier;

@EventBusSubscriber(modid = BmcMod.MODID)
public final class EmeraldEquipment {
    public static final DeferredRegister<ArmorMaterial> ARMOR_MATERIALS = DeferredRegister.create(Registries.ARMOR_MATERIAL, BmcMod.MODID);

    /** Un peu plus protecteur que le diamant (20 → 22 points), enchantabilité et résistance légèrement supérieures. */
    public static final DeferredHolder<ArmorMaterial, ArmorMaterial> EMERALD_ARMOR_MATERIAL = ARMOR_MATERIALS.register("emerald",
            () -> new ArmorMaterial(
                    Map.of(
                            ArmorItem.Type.BOOTS, 3,
                            ArmorItem.Type.LEGGINGS, 7,
                            ArmorItem.Type.CHESTPLATE, 9,
                            ArmorItem.Type.HELMET, 3),
                    11,
                    SoundEvents.ARMOR_EQUIP_DIAMOND,
                    () -> Ingredient.of(Items.EMERALD),
                    List.of(new ArmorMaterial.Layer(ResourceLocation.fromNamespaceAndPath(BmcMod.MODID, "emerald/emerald"))),
                    2.25F,
                    0.05F));

    public static final Tier EMERALD_TIER = EmeraldToolTier.INSTANCE;

    private EmeraldEquipment() {
    }

    private static boolean hasFullEmeraldSet(ServerPlayer player) {
        return player.getItemBySlot(EquipmentSlot.HEAD).is(ModItems.EMERALD_HELMET.get())
                && player.getItemBySlot(EquipmentSlot.CHEST).is(ModItems.EMERALD_CHESTPLATE.get())
                && player.getItemBySlot(EquipmentSlot.LEGS).is(ModItems.EMERALD_LEGGINGS.get())
                && player.getItemBySlot(EquipmentSlot.FEET).is(ModItems.EMERALD_BOOTS.get());
    }

    @SubscribeEvent
    public static void onPlayerTick(PlayerTickEvent.Post event) {
        if (!(event.getEntity() instanceof ServerPlayer player)) {
            return;
        }
        if (player.level().isClientSide()) {
            return;
        }

        MobEffectInstance hero = player.getEffect(MobEffects.HERO_OF_THE_VILLAGE);
        boolean full = hasFullEmeraldSet(player);

        if (full) {
            if (hero == null || hero.getAmplifier() != 0 || !hero.isInfiniteDuration()) {
                player.addEffect(new MobEffectInstance(MobEffects.HERO_OF_THE_VILLAGE, MobEffectInstance.INFINITE_DURATION, 0, false, false, true));
            }
        } else {
            if (hero != null && hero.getAmplifier() == 0 && hero.isInfiniteDuration()) {
                player.removeEffect(MobEffects.HERO_OF_THE_VILLAGE);
            }
        }
    }
}
