package com.soleapixel.bmcmod.client;

import java.util.List;

import org.jetbrains.annotations.Nullable;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.Style;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.HitResult;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.RenderGuiEvent;

import com.soleapixel.bmcmod.BmcMod;
import com.soleapixel.bmcmod.block.chest.EnchantedChestBlockEntity;
import com.soleapixel.bmcmod.registry.ModBlocks;

/**
 * Panneau style tooltip vanilla (fond/9-slice d’un item) lorsque le joueur vise le coffre enchanté.
 * Couleurs inspirées de l’XP (vert / or). Pas besoin d’accroupissement.
 */
@EventBusSubscriber(modid = BmcMod.MODID, value = Dist.CLIENT)
public final class EnchantedChestHud {
    /** Titre: or / jaune-vert type barre d’expérience. */
    private static final int COLOR_TITLE = 0xFFFCFC40;
    /** Ligne améliorations. */
    private static final int COLOR_BONUS = 0xFF80FF4D;
    /** Ligne stockée. */
    private static final int COLOR_XP = 0xFFAAFF7A;
    /** Ligne période (secondaire, vert atténué). */
    private static final int COLOR_MUTED = 0xFF9FD89F;

    private static final int MARGIN = 10;
    private static final ItemStack TOOLTIP_ITEM_CONTEXT = ItemStack.EMPTY;

    private EnchantedChestHud() {
    }

    @Nullable
    private static EnchantedChestBlockEntity getCoreBe(BlockPos pos, ClientLevel level) {
        if (!(level.getBlockEntity(pos) instanceof EnchantedChestBlockEntity be)) {
            return null;
        }
        return be.getCore().orElse(be);
    }

    @SubscribeEvent
    public static void onRenderGui(RenderGuiEvent.Post event) {
        Minecraft mc = Minecraft.getInstance();
        Player pl = mc.player;
        if (pl == null || mc.screen != null || mc.options.hideGui || !(mc.level instanceof ClientLevel cl)) {
            return;
        }
        HitResult hit = mc.hitResult;
        if (!(hit instanceof BlockHitResult br) || br.getType() != HitResult.Type.BLOCK) {
            return;
        }
        BlockPos pos = br.getBlockPos();
        BlockState st = cl.getBlockState(pos);
        if (!st.is(ModBlocks.ENCHANTED_CHEST.get())) {
            return;
        }
        EnchantedChestBlockEntity core = getCoreBe(pos, cl);
        if (core == null) {
            return;
        }
        long tStored = core.getClientStoredTenths();
        long tLast = core.getClientLastPeriodTenths();
        int up = core.getVisibleUpgradeCount();
        double mult = EnchantedChestBlockEntity.getXpRewardMultiplierForUpgradeCount(up);
        String multStr = String.format(java.util.Locale.ROOT, "%.2f", mult);
        Font font = mc.font;
        Component title = Component.translatable("hud.bmcmod.enchanted_chest_panel_title")
                .withStyle(Style.EMPTY.withColor(COLOR_TITLE).withBold(true));
        Component line0 = Component.translatable("hud.bmcmod.enchanted_chest_upgrades", up, multStr)
                .withStyle(Style.EMPTY.withColor(COLOR_BONUS));
        Component line1 = Component.translatable("hud.bmcmod.enchanted_chest_stored", formatTenths(tStored))
                .withStyle(Style.EMPTY.withColor(COLOR_XP));
        Component line2 = Component.translatable("hud.bmcmod.enchanted_chest_last", formatTenths(tLast))
                .withStyle(Style.EMPTY.withColor(COLOR_MUTED).withItalic(true));
        // Lignes aérées : séparateur vide + marge visuelle (identique à un gros paragraphe de tooltip).
        List<Component> lines = List.of(
                title,
                line0,
                Component.empty(),
                line1,
                line2
        );
        GuiGraphics g = event.getGuiGraphics();
        int sh = g.guiHeight();
        // Ancre en bas à gauche : le positionneur de tooltip ajuste pour ne pas couper l’écran.
        g.renderComponentTooltip(
                font,
                lines,
                MARGIN,
                sh - MARGIN,
                TOOLTIP_ITEM_CONTEXT);
    }

    /** 10 = 1,0 ; affichage à une décimale. */
    public static String formatTenths(long tenths) {
        long a = Math.abs(tenths);
        long intPart = a / 10L;
        long dec = a % 10L;
        if (tenths < 0) {
            return "-" + intPart + "," + dec;
        }
        return intPart + "," + dec;
    }
}
