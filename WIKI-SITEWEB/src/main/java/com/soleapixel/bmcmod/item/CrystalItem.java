package com.soleapixel.bmcmod.item;

import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Objects;
import java.util.TreeMap;
import java.util.ArrayList;

import net.minecraft.ChatFormatting;
import net.minecraft.core.component.DataComponents;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.item.component.CustomData;
import net.minecraft.world.item.Item.TooltipContext;

import com.soleapixel.bmcmod.rarity.BmcModRarity;

import net.minecraft.core.registries.BuiltInRegistries;

/**
 * Cristal stockant les âmes (total agrégé ; le détail par type est conservé en NBT pour
 * l'infusion). Infobulle : seul le total est affiché.
 */
public class CrystalItem extends Item {
    public static final String SOUL_MAP_KEY = "BmcModCrystalSouls";
    /**
     * Âmes ajoutées par {@code /bmc crystal} ; comptées dans le total comme les entrées vanilla
     * {@code minecraft:zombie}, etc., pour les coûts d’infusion.
     */
    public static final String COMMAND_BUCKET_SOUL = "bmcmod:command_bucket";

    public CrystalItem(Properties properties) {
        super(properties);
    }

    public BmcModRarity getBmcModRarity() {
        return BmcModRarity.EXOTIC;
    }

    @Override
    public Component getName(ItemStack stack) {
        return BmcModRarity.EXOTIC.styleItemName(Component.translatable(getDescriptionId(stack)));
    }

    @Override
    public void appendHoverText(
            ItemStack stack, TooltipContext context, List<Component> tooltip, TooltipFlag flag) {
        super.appendHoverText(stack, context, tooltip, flag);
        int total = getTotalSoulCount(stack);
        if (total <= 0) {
            tooltip.add(Component.translatable("item.bmcmod.crystal.empty").withStyle(ChatFormatting.DARK_GRAY));
            return;
        }
        tooltip.add(
                Component.translatable("item.bmcmod.crystal.soul_count", total)
                        .withStyle(ChatFormatting.GRAY));
    }

    public static Map<String, Integer> getSoulCounts(ItemStack stack) {
        Map<String, Integer> out = new TreeMap<>();
        CompoundTag root = stack.getOrDefault(DataComponents.CUSTOM_DATA, CustomData.EMPTY).copyTag();
        if (!root.contains(SOUL_MAP_KEY, net.minecraft.nbt.Tag.TAG_COMPOUND)) {
            return out;
        }
        CompoundTag map = root.getCompound(SOUL_MAP_KEY);
        for (String key : map.getAllKeys()) {
            if (map.contains(key, net.minecraft.nbt.Tag.TAG_INT)) {
                out.put(key, map.getInt(key));
            }
        }
        return out;
    }

    public static int getTotalSoulCount(ItemStack stack) {
        return getSoulCounts(stack).values().stream().mapToInt(Integer::intValue).sum();
    }

    /**
     * Retire jusqu'à {@code toRemove} âmes (somme des comptes par entité) du cristal.
     *
     * @return le nombre d'âmes retirées (0 si l'inventaire d'âmes est insuffisant)
     */
    public static int removeSouls(ItemStack stack, int toRemove) {
        if (toRemove <= 0) {
            return 0;
        }
        if (getTotalSoulCount(stack) < toRemove) {
            return 0;
        }
        Map<String, Integer> souls = new TreeMap<>();
        for (var e : getSoulCounts(stack).entrySet()) {
            souls.put(e.getKey(), e.getValue());
        }
        int still = toRemove;
        for (String key : new ArrayList<>(souls.keySet())) {
            if (still == 0) {
                break;
            }
            int n = Objects.requireNonNull(souls.get(key));
            int take = Math.min(n, still);
            if (take <= 0) {
                continue;
            }
            n -= take;
            still -= take;
            if (n <= 0) {
                souls.remove(key);
            } else {
                souls.put(key, n);
            }
        }
        CompoundTag root = stack.getOrDefault(DataComponents.CUSTOM_DATA, CustomData.EMPTY).copyTag();
        CompoundTag soulMap = new CompoundTag();
        for (Entry<String, Integer> e : souls.entrySet()) {
            soulMap.putInt(e.getKey(), e.getValue());
        }
        root.put(SOUL_MAP_KEY, soulMap);
        stack.set(DataComponents.CUSTOM_DATA, CustomData.of(root));
        return toRemove - still;
    }

    public static void addSoul(ItemStack stack, EntityType<?> type) {
        ResourceLocation key = BuiltInRegistries.ENTITY_TYPE.getKey(type);
        if (key == null) {
            return;
        }
        addSoul(stack, key);
    }

    public static void addSoul(ItemStack stack, ResourceLocation typeId) {
        String id = typeId.toString();
        CompoundTag root = stack.getOrDefault(DataComponents.CUSTOM_DATA, CustomData.EMPTY).copyTag();
        CompoundTag soulMap = root.contains(SOUL_MAP_KEY, net.minecraft.nbt.Tag.TAG_COMPOUND)
                ? root.getCompound(SOUL_MAP_KEY)
                : new CompoundTag();
        int n = soulMap.getInt(id);
        soulMap.putInt(id, n + 1);
        root.put(SOUL_MAP_KEY, soulMap);
        stack.set(DataComponents.CUSTOM_DATA, CustomData.of(root));
    }

    /** Vide toutes les âmes stockées dans le cristal. */
    public static void clearAllSouls(ItemStack stack) {
        CompoundTag root = stack.getOrDefault(DataComponents.CUSTOM_DATA, CustomData.EMPTY).copyTag();
        CompoundTag soulMap = new CompoundTag();
        root.put(SOUL_MAP_KEY, soulMap);
        stack.set(DataComponents.CUSTOM_DATA, CustomData.of(root));
    }

    /** Fixe le total d’âmes (remplace la répartition par un seul compartiment commande). */
    public static void setTotalSoulCountAbsolute(ItemStack stack, int total) {
        CompoundTag root = stack.getOrDefault(DataComponents.CUSTOM_DATA, CustomData.EMPTY).copyTag();
        CompoundTag soulMap = new CompoundTag();
        if (total > 0) {
            soulMap.putInt(COMMAND_BUCKET_SOUL, total);
        }
        root.put(SOUL_MAP_KEY, soulMap);
        stack.set(DataComponents.CUSTOM_DATA, CustomData.of(root));
    }

    /** Ajoute ce nombre d’âmes dans le compartiment commande (sans retirer les autres types déjà présents). */
    public static void addSoulsCommandBucket(ItemStack stack, int amount) {
        if (amount <= 0) {
            return;
        }
        CompoundTag root = stack.getOrDefault(DataComponents.CUSTOM_DATA, CustomData.EMPTY).copyTag();
        CompoundTag soulMap = root.contains(SOUL_MAP_KEY, net.minecraft.nbt.Tag.TAG_COMPOUND)
                ? root.getCompound(SOUL_MAP_KEY).copy()
                : new CompoundTag();
        long sum = (long) soulMap.getInt(COMMAND_BUCKET_SOUL) + amount;
        int capped = sum > Integer.MAX_VALUE ? Integer.MAX_VALUE : (int) sum;
        soulMap.putInt(COMMAND_BUCKET_SOUL, capped);
        root.put(SOUL_MAP_KEY, soulMap);
        stack.set(DataComponents.CUSTOM_DATA, CustomData.of(root));
    }
}
