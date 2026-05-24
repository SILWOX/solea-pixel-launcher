package com.soleapixel.bmcmod.copper;

import java.util.List;
import java.util.Map;

import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.world.item.ArmorItem;
import net.minecraft.world.item.ArmorMaterial;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.Tier;
import net.minecraft.world.item.crafting.Ingredient;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.tick.PlayerTickEvent;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredRegister;

import com.soleapixel.bmcmod.BmcMod;
import com.soleapixel.bmcmod.registry.ModItems;

import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import com.soleapixel.bmcmod.equipment.InterpolatedTier;
import net.minecraft.util.RandomSource;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.LightningBolt;
import net.minecraft.world.level.levelgen.Heightmap;
import net.minecraft.util.Mth;

@EventBusSubscriber(modid = BmcMod.MODID)
public final class CopperEquipment {
    public static final DeferredRegister<ArmorMaterial> ARMOR_MATERIALS = DeferredRegister.create(Registries.ARMOR_MATERIAL, BmcMod.MODID);

    /** Légèrement sous le fer total (13 vs 15). */
    public static final DeferredHolder<ArmorMaterial, ArmorMaterial> COPPER_ARMOR_MATERIAL = ARMOR_MATERIALS.register("copper",
            () -> new ArmorMaterial(
                    Map.of(
                            ArmorItem.Type.BOOTS, 1,
                            ArmorItem.Type.LEGGINGS, 4,
                            ArmorItem.Type.CHESTPLATE, 5,
                            ArmorItem.Type.HELMET, 2),
                    Mth.floor(Mth.lerp(0.5F, 12, 9)),
                    SoundEvents.ARMOR_EQUIP_IRON,
                    () -> Ingredient.of(Items.COPPER_INGOT),
                    List.of(new ArmorMaterial.Layer(ResourceLocation.fromNamespaceAndPath(BmcMod.MODID, "copper/copper"))),
                    0.0F,
                    0.0F));

    /** Entre pierre et fer (vanilla), réparation au lingot de cuivre. */
    public static final Tier COPPER_TIER = InterpolatedTier.COPPER;

    private CopperEquipment() {
    }

    @SubscribeEvent
    public static void onPlayerTick(PlayerTickEvent.Post event) {
        if (!(event.getEntity() instanceof ServerPlayer player)) {
            return;
        }
        ServerLevel level = player.serverLevel();
        if (level.isClientSide()) {
            return;
        }
        if (!level.isThundering() || !level.isRaining()) {
            return;
        }
        if (!level.canSeeSky(player.blockPosition())) {
            return;
        }
        if (!player.getItemBySlot(EquipmentSlot.HEAD).is(ModItems.COPPER_HELMET.get())) {
            return;
        }
        if (player.tickCount % 55 != 0) {
            return;
        }
        RandomSource random = level.random;
        if (random.nextFloat() > 0.028F) {
            return;
        }
        BlockPos base = player.blockPosition().offset(random.nextInt(17) - 8, 0, random.nextInt(17) - 8);
        BlockPos strikePos = level.getHeightmapPos(Heightmap.Types.MOTION_BLOCKING, base);
        LightningBolt bolt = EntityType.LIGHTNING_BOLT.create(level);
        if (bolt == null) {
            return;
        }
        bolt.moveTo(strikePos.getX() + 0.5, strikePos.getY(), strikePos.getZ() + 0.5);
        level.addFreshEntity(bolt);
    }
}
