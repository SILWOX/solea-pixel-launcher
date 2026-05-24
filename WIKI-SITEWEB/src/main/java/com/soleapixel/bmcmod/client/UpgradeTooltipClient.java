package com.soleapixel.bmcmod.client;

import java.util.List;
import java.util.ArrayList;

import com.soleapixel.bmcmod.BmcMod;
import com.soleapixel.bmcmod.item.upgrade.ChestplateUpgradeData;
import com.soleapixel.bmcmod.registry.ModItems;

import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.CommonComponents;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.ArmorItem;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.entity.player.ItemTooltipEvent;

@EventBusSubscriber(modid = BmcMod.MODID, value = Dist.CLIENT)
public final class UpgradeTooltipClient {
    private UpgradeTooltipClient() {
    }

    @SubscribeEvent
    public static void onItemTooltip(ItemTooltipEvent event) {
        ItemStack stack = event.getItemStack();
        if (isUpgradeItem(stack.getItem())) {
            appendUpgradeItemTooltip(event);
        }

        if (!(stack.getItem() instanceof ArmorItem armorItem) || armorItem.getType() != ArmorItem.Type.CHESTPLATE) {
            return;
        }

        List<ResourceLocation> upgrades = ChestplateUpgradeData.read(stack);
        if (upgrades.isEmpty()) {
            return;
        }

        List<Component> section = new ArrayList<>();
        section.add(Component.empty());
        section.add(Component.translatable("tooltip.bmcmod.upgrade.header_colon").withStyle(ChatFormatting.GRAY));
        for (ResourceLocation id : upgrades) {
            section.add(Component.literal("- ").withStyle(ChatFormatting.DARK_GRAY)
                    .append(Component.translatable(shortTranslationKey(id)).withStyle(colorForUpgrade(id))));
        }
        event.getToolTip().addAll(1, section);
    }

    private static void appendUpgradeItemTooltip(ItemTooltipEvent event) {
        ItemStack stack = event.getItemStack();
        ResourceLocation id = net.minecraft.core.registries.BuiltInRegistries.ITEM.getKey(stack.getItem());
        if (id == null) {
            return;
        }
        event.getToolTip().add(Component.empty());
        event.getToolTip().add(Component.translatable("tooltip.bmcmod.upgrade.item.applies_to").withStyle(ChatFormatting.GRAY));
        event.getToolTip().add(CommonComponents.space().append(Component.translatable("tooltip.bmcmod.upgrade.item.chestplate_only")).withStyle(ChatFormatting.BLUE));
        event.getToolTip().add(Component.translatable("tooltip.bmcmod.upgrade.item.effect").withStyle(ChatFormatting.GRAY));
        event.getToolTip().add(CommonComponents.space().append(Component.translatable("tooltip.bmcmod.upgrade.effect." + id.getPath())).withStyle(ChatFormatting.BLUE));
        int maxCopies = ChestplateUpgradeData.maxCopies(id);
        String combineKey = maxCopies <= 1
                ? "tooltip.bmcmod.upgrade.combine.single"
                : "tooltip.bmcmod.upgrade.combine.multi";
        event.getToolTip().add(Component.translatable(combineKey, maxCopies).withStyle(ChatFormatting.DARK_GRAY));
    }

    private static boolean isUpgradeItem(Item item) {
        return item == ModItems.ARMOR_UPGRADE.get()
                || item == ModItems.DISCRETION_UPGRADE.get()
                || item == ModItems.FROST_WALK_UPGRADE.get()
                || item == ModItems.HEALTH_UPGRADE.get()
                || item == ModItems.LUCK_UPGRADE.get()
                || item == ModItems.RANGE_UPGRADE.get()
                || item == ModItems.SPEED_UPGRADE.get()
                || item == ModItems.STEP_UPGRADE.get()
                || item == ModItems.STRENGHT_UPGRADE.get()
                || item == ModItems.DASH_UPGRADE.get()
                || item == ModItems.RAGE_UPGRADE.get()
                || item == ModItems.HEAL_UPGRADE.get()
                || item == ModItems.CRITICAL_UPGRADE.get()
                || item == ModItems.SWIM_UPGRADE.get()
                || item == ModItems.CAMOUFLAGE_UPGRADE.get();
    }

    private static String shortTranslationKey(ResourceLocation id) {
        return "tooltip.bmcmod.upgrade.short." + id.getPath();
    }

    private static ChatFormatting colorForUpgrade(ResourceLocation id) {
        if (ChestplateUpgradeData.HEALTH_UPGRADE_ID.equals(id)) return ChatFormatting.RED;
        if (ChestplateUpgradeData.LUCK_UPGRADE_ID.equals(id)) return ChatFormatting.GREEN;
        if (ChestplateUpgradeData.RANGE_UPGRADE_ID.equals(id)) return ChatFormatting.BLUE;
        if (ChestplateUpgradeData.SPEED_UPGRADE_ID.equals(id)) return ChatFormatting.AQUA;
        if (ChestplateUpgradeData.STEP_UPGRADE_ID.equals(id)) return ChatFormatting.YELLOW;
        if (ChestplateUpgradeData.STRENGHT_UPGRADE_ID.equals(id)) return ChatFormatting.DARK_RED;
        if (ChestplateUpgradeData.ARMOR_UPGRADE_ID.equals(id)) return ChatFormatting.GOLD;
        if (ChestplateUpgradeData.FROST_WALK_UPGRADE_ID.equals(id)) return ChatFormatting.DARK_AQUA;
        if (ChestplateUpgradeData.DISCRETION_UPGRADE_ID.equals(id)) return ChatFormatting.GRAY;
        if (ChestplateUpgradeData.DASH_UPGRADE_ID.equals(id)) return ChatFormatting.YELLOW;
        if (ChestplateUpgradeData.RAGE_UPGRADE_ID.equals(id)) return ChatFormatting.RED;
        if (ChestplateUpgradeData.HEAL_UPGRADE_ID.equals(id)) return ChatFormatting.DARK_GREEN;
        if (ChestplateUpgradeData.CRITICAL_UPGRADE_ID.equals(id)) return ChatFormatting.LIGHT_PURPLE;
        if (ChestplateUpgradeData.SWIM_UPGRADE_ID.equals(id)) return ChatFormatting.AQUA;
        if (ChestplateUpgradeData.CAMOUFLAGE_UPGRADE_ID.equals(id)) return ChatFormatting.DARK_GREEN;
        return ChatFormatting.BLUE;
    }
}
