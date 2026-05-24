package com.soleapixel.bmcmod.client;

import com.mojang.blaze3d.systems.RenderSystem;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.core.BlockPos;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.MenuProvider;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.GameType;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.EntityHitResult;
import net.minecraft.world.phys.HitResult;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.RenderGuiLayerEvent;
import net.neoforged.neoforge.client.gui.VanillaGuiLayers;

import com.soleapixel.bmcmod.BmcMod;
import com.soleapixel.bmcmod.ClientConfig;

/**
 * Remplace le viseur vanilla par la texture {@code assets/bmcmod/textures/gui/hud/red_crosshair.png} lorsque le
 * joueur vise une créature vivante (hors lui-même), si l'option client est activée.
 */
@EventBusSubscriber(modid = BmcMod.MODID, value = Dist.CLIENT)
public final class CrosshairTintHandler {
    private static final ResourceLocation RED_CROSSHAIR = BmcMod.loc("textures/gui/hud/red_crosshair.png");
    private static final int CROSSHAIR_SIZE = 15;

    private CrosshairTintHandler() {
    }

    @SubscribeEvent
    public static void onCrosshairPre(RenderGuiLayerEvent.Pre event) {
        if (!event.getName().equals(VanillaGuiLayers.CROSSHAIR)) {
            return;
        }
        if (!ClientConfig.CROSSHAIR_RED_ON_ENTITY.get()) {
            return;
        }
        Minecraft mc = Minecraft.getInstance();
        if (mc.player == null || mc.level == null) {
            return;
        }
        if (mc.options.hideGui) {
            return;
        }
        if (!mc.options.getCameraType().isFirstPerson()) {
            return;
        }
        if (mc.gameMode.getPlayerMode() == GameType.SPECTATOR && !canRenderCrosshairForSpectator(mc, mc.hitResult)) {
            return;
        }
        if (mc.gui.getDebugOverlay().showDebugScreen()
                && !mc.player.isReducedDebugInfo()
                && !mc.options.reducedDebugInfo().get()) {
            return;
        }
        if (!livingUnderCrosshair(mc)) {
            return;
        }
        event.setCanceled(true);
        GuiGraphics gg = event.getGuiGraphics();
        RenderSystem.enableBlend();
        RenderSystem.defaultBlendFunc();
        RenderSystem.setShaderColor(1.0F, 1.0F, 1.0F, 1.0F);
        int gw = gg.guiWidth();
        int gh = gg.guiHeight();
        int x = (gw - CROSSHAIR_SIZE) / 2;
        int y = (gh - CROSSHAIR_SIZE) / 2;
        gg.blit(RED_CROSSHAIR, x, y, 0, 0, CROSSHAIR_SIZE, CROSSHAIR_SIZE, CROSSHAIR_SIZE, CROSSHAIR_SIZE);
        RenderSystem.disableBlend();
    }

    private static boolean canRenderCrosshairForSpectator(Minecraft mc, HitResult rayTrace) {
        if (rayTrace == null) {
            return false;
        }
        return switch (rayTrace.getType()) {
            case ENTITY -> ((EntityHitResult) rayTrace).getEntity() instanceof MenuProvider;
            case BLOCK -> {
                BlockPos pos = ((BlockHitResult) rayTrace).getBlockPos();
                Level level = mc.level;
                yield level.getBlockState(pos).getMenuProvider(level, pos) != null;
            }
            default -> false;
        };
    }

    private static boolean livingUnderCrosshair(Minecraft mc) {
        Player self = mc.player;
        Entity pick = mc.crosshairPickEntity;
        if (pick instanceof LivingEntity living && living != self) {
            return true;
        }
        HitResult hit = mc.hitResult;
        if (hit instanceof EntityHitResult ehr) {
            Entity e = ehr.getEntity();
            return e instanceof LivingEntity && e != self;
        }
        return false;
    }
}
