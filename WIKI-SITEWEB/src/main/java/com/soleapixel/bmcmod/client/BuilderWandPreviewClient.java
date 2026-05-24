package com.soleapixel.bmcmod.client;

import java.util.List;

import com.mojang.blaze3d.vertex.VertexConsumer;

import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.LevelRenderer;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.Vec3;
import net.minecraft.world.phys.HitResult;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.EventPriority;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;

import com.soleapixel.bmcmod.BmcMod;
import com.soleapixel.bmcmod.item.BuilderWandItem;
import com.soleapixel.bmcmod.item.builderwand.BuilderWandMaterialPicker;
import com.soleapixel.bmcmod.item.builderwand.BuilderWandPlacement;
import com.soleapixel.bmcmod.item.builderwand.BuilderWandTier.WandModeSpec;

import net.neoforged.neoforge.client.event.RenderHighlightEvent;

/** Contours verts / rouges des emplacements que la baguette poserait (aperçu client). */
@EventBusSubscriber(modid = BmcMod.MODID, value = Dist.CLIENT)
public final class BuilderWandPreviewClient {
    private BuilderWandPreviewClient() {
    }

    @SubscribeEvent(priority = EventPriority.HIGH)
    public static void onRenderBlockHighlight(RenderHighlightEvent.Block event) {
        if (event.isCanceled()) {
            return;
        }
        Minecraft mc = Minecraft.getInstance();
        if (mc.player == null || mc.level == null || !(event.getTarget() instanceof BlockHitResult hit) || hit.getType() != HitResult.Type.BLOCK) {
            return;
        }
        ItemStack wand = findHeldWand(mc);
        if (wand.isEmpty()) {
            return;
        }
        Block block = BuilderWandItem.getStoredBlock(wand);
        if (block == null || block.defaultBlockState().isAir()) {
            List<Block> opts = BuilderWandMaterialPicker.sortedPlaceableBlocks(mc.player.getInventory());
            if (opts.isEmpty()) {
                return;
            }
            block = opts.get(0);
        }
        BlockState toPlace = block.defaultBlockState();
        WandModeSpec spec = BuilderWandItem.currentSpec(wand);
        if (spec == null) {
            return;
        }
        Direction face = hit.getDirection();
        Vec3 look = mc.player.getLookAngle();
        int turn = BuilderWandItem.getPlacementTurn(wand);
        List<BlockPos> positions = BuilderWandPlacement.computePositions(hit.getBlockPos(), face, spec, look, turn);
        Level level = mc.level;
        Vec3 cam = event.getCamera().getPosition();
        VertexConsumer lines = event.getMultiBufferSource().getBuffer(RenderType.lines());
        for (BlockPos pos : positions) {
            BlockState existing = level.getBlockState(pos);
            boolean ok = (existing.isAir() || existing.canBeReplaced()) && toPlace.canSurvive(level, pos);
            float r = ok ? 0.2F : 1.0F;
            float g = ok ? 0.95F : 0.35F;
            float b = ok ? 0.35F : 0.2F;
            renderBox(lines, event, pos, cam, r, g, b);
        }
        event.setCanceled(true);
    }

    private static ItemStack findHeldWand(Minecraft mc) {
        for (InteractionHand hand : InteractionHand.values()) {
            ItemStack s = mc.player.getItemInHand(hand);
            if (s.getItem() instanceof BuilderWandItem) {
                return s;
            }
        }
        return ItemStack.EMPTY;
    }

    private static void renderBox(VertexConsumer lines, RenderHighlightEvent.Block event, BlockPos pos, Vec3 cam, float r, float g, float b) {
        AABB box = new AABB(pos).inflate(0.002D).move(-cam.x, -cam.y, -cam.z);
        LevelRenderer.renderLineBox(event.getPoseStack(), lines, box, r, g, b, 0.9F);
    }
}
