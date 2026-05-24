package com.soleapixel.bmcmod.client;

import net.minecraft.ChatFormatting;
import net.minecraft.core.component.DataComponents;
import net.minecraft.network.chat.Component;
import net.minecraft.tags.EnchantmentTags;
import net.minecraft.util.Mth;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.Rarity;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.entity.player.ItemTooltipEvent;
import net.neoforged.neoforge.client.event.RenderTooltipEvent;

import com.soleapixel.bmcmod.BmcMod;
import com.soleapixel.bmcmod.item.BackpackItem;
import com.soleapixel.bmcmod.item.CaptureCrystalItem;
import com.soleapixel.bmcmod.item.CrystalItem;
import com.soleapixel.bmcmod.item.MorphCrystalItem;
import com.soleapixel.bmcmod.rarity.BmcModRarity;
import com.soleapixel.bmcmod.rarity.RarityStickItem;
import com.soleapixel.bmcmod.registry.ModItems;

@EventBusSubscriber(modid = BmcMod.MODID, value = Dist.CLIENT)
public final class RarityTooltipHandler {
    private RarityTooltipHandler() {
    }

    @SubscribeEvent
    public static void onItemTooltip(ItemTooltipEvent event) {
        ItemStack stack = event.getItemStack();
        if (event.getToolTip().isEmpty()) {
            return;
        }

        long millis = net.minecraft.Util.getMillis();
        if (hasCurseEnchantment(stack)) {
            event.getToolTip().set(0, BmcModRarity.CURSE.styleTooltipTitle(stack.getHoverName().copy(), millis));
            appendCurseTrackingLine(event);
            return;
        }

        if (stack.getItem() instanceof CrystalItem) {
            Component title = Component.translatable(stack.getDescriptionId());
            event.getToolTip().set(0, BmcModRarity.EXOTIC.styleTooltipTitle(title, millis));
        } else if (stack.getItem() instanceof MorphCrystalItem) {
            Component title = Component.translatable(stack.getDescriptionId());
            event.getToolTip().set(0, BmcModRarity.EXOTIC.styleTooltipTitle(title, millis));
        } else if (stack.getItem() instanceof CaptureCrystalItem) {
            Component title = Component.translatable(stack.getDescriptionId());
            event.getToolTip().set(0, BmcModRarity.EXOTIC.styleTooltipTitle(title, millis));
        } else if (stack.getItem() instanceof RarityStickItem stick) {
            BmcModRarity rarity = stick.getBmcModRarity();
            if (rarity.hasAnimatedTooltip()) {
                Component title = Component.translatable(stack.getDescriptionId());
                event.getToolTip().set(0, rarity.styleTooltipTitle(title, millis));
            }
            // Bâtons sans animation : la première ligne reste celle de {@link RarityStickItem#getName}.
        } else if (stack.getItem() == ModItems.RUBY.get() || stack.getItem() == ModItems.RUBY_BLOCK_ITEM.get()) {
            Component title = Component.translatable(stack.getDescriptionId());
            event.getToolTip().set(0, BmcModRarity.EPIC.styleTooltipTitle(title, millis));
        } else if (stack.getItem() == ModItems.RUBY_ORE_ITEM.get()
                || stack.getItem() == ModItems.DEEPSLATE_RUBY_ORE_ITEM.get()
                || stack.getItem() == ModItems.ENDSTONE_FURNACE_ITEM.get()) {
            Component title = Component.translatable(stack.getDescriptionId());
            event.getToolTip().set(0, BmcModRarity.RARE.styleTooltipTitle(title, millis));
        } else if (stack.getItem() == ModItems.FOSSIL_DEBRIS_ITEM.get()) {
            Component title = Component.translatable(stack.getDescriptionId());
            event.getToolTip().set(0, BmcModRarity.EXOTIC.styleTooltipTitle(title, millis));
        } else if (stack.getItem() == ModItems.LIGHTNING_BOW.get()) {
            Component title = Component.translatable(stack.getDescriptionId());
            event.getToolTip().set(0, BmcModRarity.EXOTIC.styleTooltipTitle(title, millis));
        } else if (stack.getItem() == ModItems.SKY_BOOTS.get()) {
            Component title = Component.translatable(stack.getDescriptionId());
            event.getToolTip().set(0, BmcModRarity.EXOTIC.styleTooltipTitle(title, millis));
        } else if (stack.getItem() == ModItems.BOREAL_UPGRADE_SMITHING_TEMPLATE.get()) {
            Component title = Component.translatable(stack.getDescriptionId());
            event.getToolTip().set(0, BmcModRarity.LEGENDARY.styleTooltipTitle(title, millis));
        } else if (isEnderiteLegendary(stack.getItem())) {
            Component title = Component.translatable(stack.getDescriptionId());
            event.getToolTip().set(0, BmcModRarity.LEGENDARY.styleTooltipTitle(title, millis));
        } else if (isBorealMythic(stack.getItem())) {
            Component title = Component.translatable(stack.getDescriptionId());
            event.getToolTip().set(0, BmcModRarity.MYTHIC.styleTooltipTitle(title, millis));
        } else if (stack.getItem() == ModItems.ENDERITE_SCRAP.get()) {
            Component title = Component.translatable(stack.getDescriptionId());
            event.getToolTip().set(0, BmcModRarity.EPIC.styleTooltipTitle(title, millis));
        } else if (stack.getItem() == ModItems.ENDERITE_UPGRADE_SMITHING_TEMPLATE.get()) {
            Component title = Component.translatable(stack.getDescriptionId());
            event.getToolTip().set(0, BmcModRarity.RARE.styleTooltipTitle(title, millis));
        } else if (stack.getItem() == ModItems.STAFF_UPGRADE_SMITHING_TEMPLATE.get()) {
            Component title = Component.translatable(stack.getDescriptionId());
            event.getToolTip().set(0, BmcModRarity.RARE.styleTooltipTitle(title, millis));
        } else if (isVanillaMobOrPlayerHead(stack)) {
            event.getToolTip().set(0, BmcModRarity.EXOTIC.styleTooltipTitle(stack.getHoverName().copy(), millis));
        } else if (isUndeadInvasionBanner(stack) || isEndStormBanner(stack)) {
            event.getToolTip().set(0, BmcModRarity.MYTHIC.styleTooltipTitle(stack.getHoverName().copy(), millis));
        } else if (stack.getItem() == ModItems.UNDEAD_CROWN.get()) {
            event.getToolTip().set(0, BmcModRarity.MYTHIC.styleTooltipTitle(stack.getHoverName().copy(), millis));
        } else if (stack.getItem() instanceof BackpackItem && stack.getRarity() == Rarity.COMMON) {
            Component base;
            if (stack.has(DataComponents.CUSTOM_NAME)) {
                base = stack.get(DataComponents.CUSTOM_NAME).copy();
            } else if (stack.has(DataComponents.ITEM_NAME)) {
                base = stack.get(DataComponents.ITEM_NAME).copy();
            } else {
                base = Component.translatable(stack.getDescriptionId());
            }
            event.getToolTip().set(0, base.copy().withStyle(ChatFormatting.YELLOW));
        } else {
            event.getToolTip().set(0, VanillaRarityStyle.retitle(stack));
        }

        appendRarityTrackingLine(event, stack);
    }

    /**
     * Ligne du bas « rarity : … » pour tout objet sauf rareté vanilla Uncommon (et bâton Uncommon BmcMod).
     */
    private static void appendRarityTrackingLine(ItemTooltipEvent event, ItemStack stack) {
        if (stack.getItem() instanceof RarityStickItem stick) {
            if (stick.getBmcModRarity() == BmcModRarity.UNCOMMON) {
                return;
            }
            Component label = Component.translatable("bmcmod.rarity_label." + stick.getBmcModRarity().name().toLowerCase());
            event.getToolTip().add(Component.translatable("bmcmod.tooltip.rarity_line", label).withStyle(ChatFormatting.DARK_GRAY));
            return;
        }
        if (stack.getItem() instanceof CrystalItem) {
            Component label = Component.translatable("bmcmod.rarity_label.exotic");
            event.getToolTip().add(Component.translatable("bmcmod.tooltip.rarity_line", label).withStyle(ChatFormatting.DARK_GRAY));
            return;
        }
        if (stack.getItem() instanceof MorphCrystalItem) {
            Component label = Component.translatable("bmcmod.rarity_label.exotic");
            event.getToolTip().add(Component.translatable("bmcmod.tooltip.rarity_line", label).withStyle(ChatFormatting.DARK_GRAY));
            return;
        }
        if (stack.getItem() instanceof CaptureCrystalItem) {
            Component label = Component.translatable("bmcmod.rarity_label.exotic");
            event.getToolTip().add(Component.translatable("bmcmod.tooltip.rarity_line", label).withStyle(ChatFormatting.DARK_GRAY));
            return;
        }
        if (stack.getItem() == ModItems.FOSSIL_DEBRIS_ITEM.get()) {
            Component label = Component.translatable("bmcmod.rarity_label.exotic");
            event.getToolTip().add(Component.translatable("bmcmod.tooltip.rarity_line", label).withStyle(ChatFormatting.DARK_GRAY));
            return;
        }
        if (stack.getItem() == ModItems.LIGHTNING_BOW.get()) {
            Component label = Component.translatable("bmcmod.rarity_label.exotic");
            event.getToolTip().add(Component.translatable("bmcmod.tooltip.rarity_line", label).withStyle(ChatFormatting.DARK_GRAY));
            return;
        }
        if (stack.getItem() == ModItems.SKY_BOOTS.get()) {
            Component label = Component.translatable("bmcmod.rarity_label.exotic");
            event.getToolTip().add(Component.translatable("bmcmod.tooltip.rarity_line", label).withStyle(ChatFormatting.DARK_GRAY));
            return;
        }
        if (stack.getItem() == ModItems.RUBY_ORE_ITEM.get()
                || stack.getItem() == ModItems.DEEPSLATE_RUBY_ORE_ITEM.get()
                || stack.getItem() == ModItems.ENDSTONE_FURNACE_ITEM.get()) {
            Component label = Component.translatable("bmcmod.rarity_label.rare");
            event.getToolTip().add(Component.translatable("bmcmod.tooltip.rarity_line", label).withStyle(ChatFormatting.DARK_GRAY));
            return;
        }
        if (stack.getItem() == ModItems.RUBY.get() || stack.getItem() == ModItems.RUBY_BLOCK_ITEM.get()) {
            Component label = Component.translatable("bmcmod.rarity_label.epic");
            event.getToolTip().add(Component.translatable("bmcmod.tooltip.rarity_line", label).withStyle(ChatFormatting.DARK_GRAY));
            return;
        }
        if (stack.getItem() == ModItems.BOREAL_UPGRADE_SMITHING_TEMPLATE.get()) {
            Component label = Component.translatable("bmcmod.rarity_label.legendary");
            event.getToolTip().add(Component.translatable("bmcmod.tooltip.rarity_line", label).withStyle(ChatFormatting.DARK_GRAY));
            return;
        }
        if (isEnderiteLegendary(stack.getItem())) {
            Component label = Component.translatable("bmcmod.rarity_label.legendary");
            event.getToolTip().add(Component.translatable("bmcmod.tooltip.rarity_line", label).withStyle(ChatFormatting.DARK_GRAY));
            return;
        }
        if (isBorealMythic(stack.getItem())) {
            Component label = Component.translatable("bmcmod.rarity_label.mythic");
            event.getToolTip().add(Component.translatable("bmcmod.tooltip.rarity_line", label).withStyle(ChatFormatting.DARK_GRAY));
            return;
        }
        if (stack.getItem() == ModItems.ENDERITE_SCRAP.get()) {
            Component label = Component.translatable("bmcmod.rarity_label.epic");
            event.getToolTip().add(Component.translatable("bmcmod.tooltip.rarity_line", label).withStyle(ChatFormatting.DARK_GRAY));
            return;
        }
        if (stack.getItem() == ModItems.ENDERITE_UPGRADE_SMITHING_TEMPLATE.get()) {
            Component label = Component.translatable("bmcmod.rarity_label.rare");
            event.getToolTip().add(Component.translatable("bmcmod.tooltip.rarity_line", label).withStyle(ChatFormatting.DARK_GRAY));
            return;
        }
        if (stack.getItem() == ModItems.STAFF_UPGRADE_SMITHING_TEMPLATE.get()) {
            Component label = Component.translatable("bmcmod.rarity_label.rare");
            event.getToolTip().add(Component.translatable("bmcmod.tooltip.rarity_line", label).withStyle(ChatFormatting.DARK_GRAY));
            return;
        }
        if (isVanillaMobOrPlayerHead(stack)) {
            Component label = Component.translatable("bmcmod.rarity_label.exotic");
            event.getToolTip().add(Component.translatable("bmcmod.tooltip.rarity_line", label).withStyle(ChatFormatting.DARK_GRAY));
            return;
        }
        if (isUndeadInvasionBanner(stack) || isEndStormBanner(stack)) {
            Component label = Component.translatable("bmcmod.rarity_label.mythic");
            event.getToolTip().add(Component.translatable("bmcmod.tooltip.rarity_line", label).withStyle(ChatFormatting.DARK_GRAY));
            return;
        }
        if (stack.getItem() == ModItems.UNDEAD_CROWN.get()) {
            Component label = Component.translatable("bmcmod.rarity_label.mythic");
            event.getToolTip().add(Component.translatable("bmcmod.tooltip.rarity_line", label).withStyle(ChatFormatting.DARK_GRAY));
            return;
        }
        Rarity rarity = stack.getRarity();
        if (rarity == Rarity.UNCOMMON) {
            return;
        }
        Component label = switch (rarity) {
            case COMMON -> Component.translatable("bmcmod.rarity_label.common");
            case RARE -> Component.translatable("bmcmod.rarity_label.rare");
            case EPIC -> Component.translatable("bmcmod.rarity_label.epic");
            default -> Component.translatable("bmcmod.rarity_label.common");
        };
        event.getToolTip().add(Component.translatable("bmcmod.tooltip.rarity_line", label).withStyle(ChatFormatting.DARK_GRAY));
    }

    private static void appendCurseTrackingLine(ItemTooltipEvent event) {
        Component label = Component.translatable("bmcmod.rarity_label.curse");
        event.getToolTip().add(Component.translatable("bmcmod.tooltip.rarity_line", label).withStyle(ChatFormatting.DARK_GRAY));
    }

    @SubscribeEvent
    public static void onTooltipColor(RenderTooltipEvent.Color event) {
        ItemStack stack = event.getItemStack();
        long t = net.minecraft.Util.getMillis();
        if (hasCurseEnchantment(stack)) {
            applyBmcModStickBorder(event, BmcModRarity.CURSE.tooltipAccent(), t);
            return;
        }

        if (stack.getItem() instanceof CrystalItem) {
            applyBmcModStickBorder(event, BmcModRarity.EXOTIC.tooltipAccent(), t);
            return;
        }
        if (stack.getItem() instanceof MorphCrystalItem) {
            applyBmcModStickBorder(event, BmcModRarity.EXOTIC.tooltipAccent(), t);
            return;
        }
        if (stack.getItem() instanceof CaptureCrystalItem) {
            applyBmcModStickBorder(event, BmcModRarity.EXOTIC.tooltipAccent(), t);
            return;
        }
        if (stack.getItem() instanceof RarityStickItem stick) {
            applyBmcModStickBorder(event, stick.getBmcModRarity().tooltipAccent(), t);
            return;
        }
        if (stack.getItem() == ModItems.RUBY.get() || stack.getItem() == ModItems.RUBY_BLOCK_ITEM.get()) {
            applyBmcModStickBorder(event, BmcModRarity.EPIC.tooltipAccent(), t);
            return;
        }
        if (stack.getItem() == ModItems.FOSSIL_DEBRIS_ITEM.get()) {
            applyBmcModStickBorder(event, BmcModRarity.EXOTIC.tooltipAccent(), t);
            return;
        }
        if (stack.getItem() == ModItems.LIGHTNING_BOW.get()) {
            applyBmcModStickBorder(event, BmcModRarity.EXOTIC.tooltipAccent(), t);
            return;
        }
        if (stack.getItem() == ModItems.SKY_BOOTS.get()) {
            applyBmcModStickBorder(event, BmcModRarity.EXOTIC.tooltipAccent(), t);
            return;
        }
        if (stack.getItem() == ModItems.BOREAL_UPGRADE_SMITHING_TEMPLATE.get()) {
            applyBmcModStickBorder(event, BmcModRarity.LEGENDARY.tooltipAccent(), t);
            return;
        }
        if (isEnderiteLegendary(stack.getItem())) {
            applyBmcModStickBorder(event, BmcModRarity.LEGENDARY.tooltipAccent(), t);
            return;
        }
        if (isBorealMythic(stack.getItem())) {
            applyBmcModStickBorder(event, BmcModRarity.MYTHIC.tooltipAccent(), t);
            return;
        }
        if (stack.getItem() == ModItems.ENDERITE_SCRAP.get()) {
            applyBmcModStickBorder(event, BmcModRarity.EPIC.tooltipAccent(), t);
            return;
        }
        if (stack.getItem() == ModItems.ENDERITE_UPGRADE_SMITHING_TEMPLATE.get()) {
            applyVanillaRarityBorder(event, Rarity.RARE, t);
            return;
        }
        if (stack.getItem() == ModItems.STAFF_UPGRADE_SMITHING_TEMPLATE.get()) {
            applyVanillaRarityBorder(event, Rarity.RARE, t);
            return;
        }
        if (isVanillaMobOrPlayerHead(stack)) {
            applyBmcModStickBorder(event, BmcModRarity.EXOTIC.tooltipAccent(), t);
            return;
        }
        if (isUndeadInvasionBanner(stack) || isEndStormBanner(stack)) {
            applyBmcModStickBorder(event, BmcModRarity.MYTHIC.tooltipAccent(), t);
            return;
        }
        if (stack.getItem() == ModItems.UNDEAD_CROWN.get()) {
            applyBmcModStickBorder(event, BmcModRarity.MYTHIC.tooltipAccent(), t);
            return;
        }

        applyVanillaRarityBorder(event, stack.getRarity(), t);
    }

    private static void applyBmcModStickBorder(RenderTooltipEvent.Color event, BmcModRarity.TooltipAccent accent, long t) {
        switch (accent) {
            case NONE -> {
            }
            case PURPLE -> {
                int pulse = (int) (20 * Math.sin(t / 200.0));
                event.setBorderStart(0xFFAA66FF + (pulse << 16));
                event.setBorderEnd(0xFF6633CC - (pulse << 8));
            }
            case GOLD -> {
                float w = (float) (0.5 + 0.5 * Math.sin(t / 260.0));
                int hi = lerpRgb(0xFFE45C, 0xFFD700, w);
                int lo = lerpRgb(0xE6B422, 0xB8860B, w);
                event.setBorderStart(0xFF000000 | hi);
                event.setBorderEnd(0xFF000000 | lo);
                event.setBackgroundStart(0xF018140A);
                event.setBackgroundEnd(0xF0261E0C);
            }
            case SKY -> {
                // Exotic : bordure periwinkle ↔ cyan (style référence), fond bleu nuit assombri.
                float w = (float) (0.5 + 0.5 * Math.sin(t / 230.0));
                int hi = lerpRgb(0xA8B8E8, 0x6AFFEC, w);
                float w2 = (float) (0.5 + 0.5 * Math.sin(t / 230.0 + 0.9f));
                int lo = lerpRgb(0x5C6FA0, 0x42D8C2, w2);
                event.setBorderStart(0xFF000000 | hi);
                event.setBorderEnd(0xFF000000 | lo);
                event.setBackgroundStart(0xF0141C34);
                event.setBackgroundEnd(0xF00E2434);
            }
            case MYTHIC_GOLD -> {
                // Or chaud + veines violettes (le légendaire reste or / ambre sans cette teinte).
                float w = (float) (0.5 + 0.5 * Math.sin(t / 250.0));
                int goldHi = lerpRgb(0xF8E6A8, 0xD4B24A, w);
                float w2 = (float) (0.5 + 0.5 * Math.cos(t / 300.0));
                int goldLo = lerpRgb(0xC9A03C, 0x9A6E2E, w2);
                int vein = lerpRgb(0x7B48C8, 0x4E6BE0, (float) (0.5 + 0.5 * Math.sin(t / 210.0)));
                int hi = lerpRgb(goldHi, vein, 0.22F);
                int lo = lerpRgb(goldLo, vein, 0.18F);
                event.setBorderStart(0xFF000000 | hi);
                event.setBorderEnd(0xFF000000 | lo);
                event.setBackgroundStart(0xF01A1428);
                event.setBackgroundEnd(0xF0261C38);
            }
            case CURSE_RED -> {
                float w = (float) (0.5 + 0.5 * Math.sin(t / 170.0));
                int hi = lerpRgb(0xFF5A5A, 0xC10F0F, w);
                int lo = lerpRgb(0x7A0C0C, 0x400707, (float) (0.5 + 0.5 * Math.cos(t / 190.0)));
                event.setBorderStart(0xFF000000 | hi);
                event.setBorderEnd(0xFF000000 | lo);
                event.setBackgroundStart(0xF01C0606);
                event.setBackgroundEnd(0xF0280A0A);
            }
            case GLITCH -> {
                float w = (float) (0.5 + 0.5 * Math.sin(t / 175.0));
                int a = lerpRgb(0xFF3D9E, 0x8A4DFF, w);
                int b = lerpRgb(0x40F5FF, 0xFF2D6E, (float) (0.5 + 0.5 * Math.cos(t / 195f)));
                event.setBorderStart(0xFF000000 | a);
                event.setBorderEnd(0xFF000000 | b);
                event.setBackgroundStart(0xF00C1024);
                event.setBackgroundEnd(0xF0141834);
            }
        }
    }

    private static void applyVanillaRarityBorder(RenderTooltipEvent.Color event, Rarity rarity, long t) {
        switch (rarity) {
            case COMMON, UNCOMMON -> {
            }
            case RARE -> {
                int w = (int) (10 * Math.sin(t / 280.0));
                event.setBorderStart(0xFF4488FF + (w << 16));
                event.setBorderEnd(0xFF2266CC - (w << 8));
            }
            case EPIC -> {
                int pulse = (int) (18 * Math.sin(t / 210.0));
                event.setBorderStart(0xFFAA66FF + (pulse << 16));
                event.setBorderEnd(0xFF7733CC - (pulse << 8));
            }
        }
    }

    private static boolean isVanillaMobOrPlayerHead(ItemStack stack) {
        return stack.is(Items.PLAYER_HEAD)
                || stack.is(Items.SKELETON_SKULL)
                || stack.is(Items.WITHER_SKELETON_SKULL)
                || stack.is(Items.ZOMBIE_HEAD)
                || stack.is(Items.CREEPER_HEAD)
                || stack.is(Items.DRAGON_HEAD)
                || stack.is(Items.PIGLIN_HEAD)
                || stack.is(ModItems.SKELETON_VILLAGER_SKULL_ITEM.get());
    }

    private static boolean isUndeadInvasionBanner(ItemStack stack) {
        if (stack.is(ModItems.UNDEAD_INVASION_BANNER.get())) {
            return true;
        }
        if (!stack.is(Items.BLACK_BANNER)) {
            return false;
        }
        var data = stack.get(DataComponents.CUSTOM_DATA);
        return data != null && data.copyTag().getBoolean("UndeadInvasionBanner");
    }

    private static boolean isEndStormBanner(ItemStack stack) {
        if (!stack.is(ModItems.END_STORM_BANNER.get()) && !stack.is(Items.BLACK_BANNER)) {
            return false;
        }
        var data = stack.get(DataComponents.CUSTOM_DATA);
        return data != null && data.copyTag().getBoolean("EndStormBanner");
    }

    private static boolean hasCurseEnchantment(ItemStack stack) {
        var active = stack.get(DataComponents.ENCHANTMENTS);
        if (active != null) {
            for (var e : active.entrySet()) {
                if (e.getIntValue() > 0 && e.getKey().is(EnchantmentTags.CURSE)) {
                    return true;
                }
            }
        }
        var stored = stack.get(DataComponents.STORED_ENCHANTMENTS);
        if (stored != null) {
            for (var e : stored.entrySet()) {
                if (e.getIntValue() > 0 && e.getKey().is(EnchantmentTags.CURSE)) {
                    return true;
                }
            }
        }
        return false;
    }

    private static boolean isEnderiteLegendary(Item item) {
        return item == ModItems.ENDERITE_INGOT.get()
                || item == ModItems.ENDERITE_BLOCK_ITEM.get()
                || item == ModItems.ENDERITE_SWORD.get()
                || item == ModItems.ENDERITE_PICKAXE.get()
                || item == ModItems.ENDERITE_AXE.get()
                || item == ModItems.ENDERITE_SHOVEL.get()
                || item == ModItems.ENDERITE_SCYTHE.get()
                || item == ModItems.ENDERITE_HOE.get()
                || item == ModItems.ENDERITE_HELMET.get()
                || item == ModItems.ENDERITE_CHESTPLATE.get()
                || item == ModItems.ENDERITE_LEGGINGS.get()
                || item == ModItems.ENDERITE_BOOTS.get();
    }

    private static boolean isBorealMythic(Item item) {
        return item == ModItems.BOREAL_SWORD.get()
                || item == ModItems.BOREAL_PICKAXE.get()
                || item == ModItems.BOREAL_AXE.get()
                || item == ModItems.BOREAL_SHOVEL.get()
                || item == ModItems.BOREAL_SCYTHE.get()
                || item == ModItems.BOREAL_HELMET.get()
                || item == ModItems.BOREAL_CHESTPLATE.get()
                || item == ModItems.BOREAL_LEGGINGS.get()
                || item == ModItems.BOREAL_BOOTS.get();
    }

    private static int lerpRgb(int from, int to, float blend) {
        blend = Mth.clamp(blend, 0, 1);
        int r = Mth.clamp((int) Mth.lerp(blend, (from >> 16) & 0xFF, (to >> 16) & 0xFF), 0, 255);
        int g = Mth.clamp((int) Mth.lerp(blend, (from >> 8) & 0xFF, (to >> 8) & 0xFF), 0, 255);
        int b = Mth.clamp((int) Mth.lerp(blend, from & 0xFF, to & 0xFF), 0, 255);
        return (r << 16) | (g << 8) | b;
    }
}
