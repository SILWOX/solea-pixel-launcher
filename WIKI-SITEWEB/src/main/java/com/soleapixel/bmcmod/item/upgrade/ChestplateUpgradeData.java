package com.soleapixel.bmcmod.item.upgrade;

import java.util.ArrayList;
import java.util.List;

import com.soleapixel.bmcmod.BmcMod;

import net.minecraft.core.component.DataComponents;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.StringTag;
import net.minecraft.nbt.Tag;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.component.CustomData;

public final class ChestplateUpgradeData {
    public static final String APPLIED_UPGRADES_KEY = "bmcmod:applied_upgrades";
    public static final int MAX_UPGRADES = 3;
    public static final ResourceLocation ARMOR_UPGRADE_ID = BmcMod.loc("armor_upgrade");
    public static final ResourceLocation DISCRETION_UPGRADE_ID = BmcMod.loc("discretion_upgrade");
    public static final ResourceLocation FROST_WALK_UPGRADE_ID = BmcMod.loc("frost_walk_upgrade");
    public static final ResourceLocation HEALTH_UPGRADE_ID = BmcMod.loc("health_upgrade");
    public static final ResourceLocation LUCK_UPGRADE_ID = BmcMod.loc("luck_upgrade");
    public static final ResourceLocation RANGE_UPGRADE_ID = BmcMod.loc("range_upgrade");
    public static final ResourceLocation SPEED_UPGRADE_ID = BmcMod.loc("speed_upgrade");
    public static final ResourceLocation STEP_UPGRADE_ID = BmcMod.loc("step_upgrade");
    public static final ResourceLocation STRENGHT_UPGRADE_ID = BmcMod.loc("strenght_upgrade");
    public static final ResourceLocation DASH_UPGRADE_ID = BmcMod.loc("dash_upgrade");
    public static final ResourceLocation RAGE_UPGRADE_ID = BmcMod.loc("rage_upgrade");
    public static final ResourceLocation HEAL_UPGRADE_ID = BmcMod.loc("heal_upgrade");
    public static final ResourceLocation CRITICAL_UPGRADE_ID = BmcMod.loc("critical_upgrade");
    public static final ResourceLocation SWIM_UPGRADE_ID = BmcMod.loc("swim_upgrade");
    public static final ResourceLocation CAMOUFLAGE_UPGRADE_ID = BmcMod.loc("camouflage_upgrade");

    private ChestplateUpgradeData() {
    }

    public static List<ResourceLocation> read(ItemStack stack) {
        CompoundTag root = stack.getOrDefault(DataComponents.CUSTOM_DATA, CustomData.EMPTY).copyTag();
        ListTag list = root.getList(APPLIED_UPGRADES_KEY, Tag.TAG_STRING);
        List<ResourceLocation> upgrades = new ArrayList<>();
        for (int i = 0; i < list.size(); i++) {
            ResourceLocation id = ResourceLocation.tryParse(list.getString(i));
            if (id != null) {
                upgrades.add(id);
            }
        }
        return upgrades;
    }

    public static int size(ItemStack stack) {
        return read(stack).size();
    }

    public static boolean canAdd(ItemStack stack) {
        return size(stack) < MAX_UPGRADES;
    }

    public static int count(ItemStack stack, ResourceLocation upgradeId) {
        int c = 0;
        for (ResourceLocation id : read(stack)) {
            if (upgradeId.equals(id)) {
                c++;
            }
        }
        return c;
    }

    public static boolean canAdd(ItemStack stack, ResourceLocation upgradeId) {
        if (size(stack) >= MAX_UPGRADES) {
            return false;
        }
        return count(stack, upgradeId) < maxCopies(upgradeId);
    }

    public static ItemStack withAppendedUpgrade(ItemStack chestplate, ResourceLocation upgradeId) {
        ItemStack out = chestplate.copy();
        if (!canAdd(out, upgradeId)) {
            return out;
        }
        CompoundTag root = out.getOrDefault(DataComponents.CUSTOM_DATA, CustomData.EMPTY).copyTag();
        ListTag list = root.getList(APPLIED_UPGRADES_KEY, Tag.TAG_STRING);
        list.add(StringTag.valueOf(upgradeId.toString()));
        root.put(APPLIED_UPGRADES_KEY, list);
        out.set(DataComponents.CUSTOM_DATA, CustomData.of(root));
        return out;
    }

    public static int maxCopies(ResourceLocation id) {
        if (DISCRETION_UPGRADE_ID.equals(id)
                || CAMOUFLAGE_UPGRADE_ID.equals(id)
                || FROST_WALK_UPGRADE_ID.equals(id)
                || DASH_UPGRADE_ID.equals(id)
                || HEAL_UPGRADE_ID.equals(id)) {
            return 1;
        }
        return MAX_UPGRADES;
    }

    public static String translationKey(ResourceLocation id) {
        return "upgrade.bmcmod." + id.getPath() + ".name";
    }

    public static ResourceLocation upgradePlateId() {
        return BmcMod.loc("upgrade_plate");
    }
}
