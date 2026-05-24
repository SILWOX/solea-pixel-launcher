package com.soleapixel.bmcmod.client;

import java.util.ArrayList;
import java.util.List;

import com.mojang.blaze3d.vertex.VertexConsumer;
import com.soleapixel.bmcmod.BmcMod;
import com.soleapixel.bmcmod.gameplay.EnchantmentGameplayEvents;
import com.soleapixel.bmcmod.item.BuilderWandItem;
import com.soleapixel.bmcmod.registry.ModEnchantmentKeys;

import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.LevelRenderer;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.PickaxeItem;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.Vec3;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.RenderHighlightEvent;

/** Aperçu visuel des blocs cassés par Excavator (single / 3x3). */
@EventBusSubscriber(modid = BmcMod.MODID, value = Dist.CLIENT)
public final class ExcavatorOutlineClient {
    private ExcavatorOutlineClient() {
    }

    @SubscribeEvent
    public static void onRenderBlockHighlight(RenderHighlightEvent.Block event) {
        if (event.isCanceled()) {
            return;
        }
        Minecraft mc = Minecraft.getInstance();
        if (mc.player == null || mc.level == null || !(event.getTarget() instanceof BlockHitResult hit)) {
            return;
        }
        if (mc.player.getMainHandItem().getItem() instanceof BuilderWandItem || mc.player.getOffhandItem().getItem() instanceof BuilderWandItem) {
            return;
        }
        ItemStack tool = resolveExcavatorTool(mc.player.getMainHandItem(), mc.player.getOffhandItem());
        if (tool.isEmpty()) {
            return;
        }
        int excavator = ModEnchantmentKeys.enchantmentLevel(tool, mc.level.registryAccess(), ModEnchantmentKeys.EXCAVATOR);
        if (excavator <= 0) {
            return;
        }

        boolean mode3x3 = EnchantmentGameplayEvents.isExcavator3x3Enabled(tool);
        List<BlockPos> targets = mode3x3
                ? collect3x3(hit.getBlockPos(), mc.player.getXRot(), mc.player.getDirection())
                : List.of(hit.getBlockPos());

        Vec3 cam = event.getCamera().getPosition();
        VertexConsumer lines = event.getMultiBufferSource().getBuffer(RenderType.lines());
        float r = mode3x3 ? 0.10F : 0.95F;
        float g = mode3x3 ? 0.85F : 0.95F;
        float b = mode3x3 ? 1.00F : 0.25F;
        for (BlockPos pos : targets) {
            renderBox(event, lines, mc.level, pos, cam, r, g, b);
        }
        // Remplace le contour vanilla pour afficher exactement la zone cassée.
        event.setCanceled(true);
    }

    private static ItemStack resolveExcavatorTool(ItemStack main, ItemStack off) {
        if (main.getItem() instanceof PickaxeItem) {
            return main;
        }
        if (off.getItem() instanceof PickaxeItem) {
            return off;
        }
        return ItemStack.EMPTY;
    }

    private static List<BlockPos> collect3x3(BlockPos center, float xRot, Direction facing) {
        List<BlockPos> out = new ArrayList<>(9);
        boolean horizontal = Math.abs(xRot) > 55.0F;
        for (int a = -1; a <= 1; a++) {
            for (int b = -1; b <= 1; b++) {
                BlockPos p;
                if (horizontal) {
                    p = center.offset(a, 0, b);
                } else if (facing.getAxis() == Direction.Axis.X) {
                    p = center.offset(0, a, b);
                } else {
                    p = center.offset(a, b, 0);
                }
                out.add(p);
            }
        }
        return out;
    }

    private static void renderBox(RenderHighlightEvent.Block event, VertexConsumer lines, Level level, BlockPos pos, Vec3 cam, float r, float g, float b) {
        if (level.isEmptyBlock(pos)) {
            return;
        }
        AABB box = level.getBlockState(pos).getShape(level, pos).bounds().move(pos).inflate(0.002D).move(-cam.x, -cam.y, -cam.z);
        LevelRenderer.renderLineBox(event.getPoseStack(), lines, box, r, g, b, 0.95F);
    }
}
