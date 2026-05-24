package com.soleapixel.bmcmod.item;

import java.util.List;

import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.core.component.DataComponents;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.item.context.UseOnContext;
import net.minecraft.world.item.component.CustomData;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.Vec3;

import com.soleapixel.bmcmod.item.builderwand.BuilderWandMaterialPicker;
import com.soleapixel.bmcmod.item.builderwand.BuilderWandPlacement;
import com.soleapixel.bmcmod.item.builderwand.BuilderWandTier;
import com.soleapixel.bmcmod.item.builderwand.BuilderWandTier.WandModeSpec;

import net.minecraft.world.item.Item.TooltipContext;

public final class BuilderWandItem extends Item {
    public static final String MODE_KEY = "BuilderWandMode";
    public static final String BLOCK_KEY = "BuilderWandBlock";
    /** Quart de tour (0–3) dans le plan de la face cliquée — molette + Ctrl. */
    public static final String PLACEMENT_TURN_KEY = "BuilderWandPlacementTurn";

    private final BuilderWandTier tier;

    public BuilderWandItem(BuilderWandTier tier, Properties properties) {
        super(properties.stacksTo(1).durability(tier.durability()));
        this.tier = tier;
    }

    public BuilderWandTier tier() {
        return tier;
    }

    public static WandModeSpec currentSpec(ItemStack stack) {
        if (!(stack.getItem() instanceof BuilderWandItem bw)) {
            return null;
        }
        return bw.tier.mode(getMode(stack));
    }

    public static int getMode(ItemStack stack) {
        if (!(stack.getItem() instanceof BuilderWandItem bw)) {
            return 0;
        }
        int m = stack.getOrDefault(DataComponents.CUSTOM_DATA, CustomData.EMPTY).copyTag().getInt(MODE_KEY);
        return Math.floorMod(Math.max(0, m), bw.tier.modeCount());
    }

    public static void setMode(ItemStack stack, int mode) {
        CompoundTag tag = stack.getOrDefault(DataComponents.CUSTOM_DATA, CustomData.EMPTY).copyTag();
        tag.putInt(MODE_KEY, mode);
        stack.set(DataComponents.CUSTOM_DATA, CustomData.of(tag));
    }

    public static void cycleMode(ItemStack stack) {
        if (!(stack.getItem() instanceof BuilderWandItem bw)) {
            return;
        }
        int next = (getMode(stack) + 1) % bw.tier.modeCount();
        setMode(stack, next);
    }

    public static int getPlacementTurn(ItemStack stack) {
        if (!(stack.getItem() instanceof BuilderWandItem)) {
            return 0;
        }
        int t = stack.getOrDefault(DataComponents.CUSTOM_DATA, CustomData.EMPTY).copyTag().getInt(PLACEMENT_TURN_KEY);
        return Math.floorMod(t, 4);
    }

    public static void setPlacementTurn(ItemStack stack, int turn) {
        CompoundTag tag = stack.getOrDefault(DataComponents.CUSTOM_DATA, CustomData.EMPTY).copyTag();
        tag.putInt(PLACEMENT_TURN_KEY, Math.floorMod(turn, 4));
        stack.set(DataComponents.CUSTOM_DATA, CustomData.of(tag));
    }

    /** Incrémente le quart de tour de placement (typiquement ±1 depuis la molette). */
    public static void adjustPlacementTurn(ItemStack stack, int delta) {
        if (!(stack.getItem() instanceof BuilderWandItem) || delta == 0) {
            return;
        }
        setPlacementTurn(stack, getPlacementTurn(stack) + delta);
    }

    /** Libellé orientation de placement (1–4) pour l’action bar. */
    public static MutableComponent placementTurnDescription(ItemStack stack) {
        int q = getPlacementTurn(stack) + 1;
        return Component.translatable("message.bmcmod.builder_wand.placement_turn", q);
    }

    /** Libellé du mode courant (tooltip + message action bar), avec numéro pour s’y retrouver. */
    public static Component modeDescription(ItemStack stack) {
        if (!(stack.getItem() instanceof BuilderWandItem bw)) {
            return Component.empty();
        }
        int idx = getMode(stack);
        WandModeSpec spec = bw.tier.mode(idx);
        int total = bw.tier.modeCount();
        return Component.translatable("message.bmcmod.builder_wand.mode_index", idx + 1, total)
                .append(Component.literal(" · "))
                .append(modeLabel(spec));
    }

    public static void setStoredBlock(ItemStack stack, Block block) {
        CompoundTag tag = stack.getOrDefault(DataComponents.CUSTOM_DATA, CustomData.EMPTY).copyTag();
        tag.putString(BLOCK_KEY, BuiltInRegistries.BLOCK.getKey(block).toString());
        stack.set(DataComponents.CUSTOM_DATA, CustomData.of(tag));
    }

    public static Block getStoredBlock(ItemStack stack) {
        String s = stack.getOrDefault(DataComponents.CUSTOM_DATA, CustomData.EMPTY).copyTag().getString(BLOCK_KEY);
        if (s.isEmpty()) {
            return null;
        }
        ResourceLocation id = ResourceLocation.tryParse(s);
        if (id == null) {
            return null;
        }
        return BuiltInRegistries.BLOCK.getOptional(id).filter(b -> !b.defaultBlockState().isAir()).orElse(null);
    }

    @Override
    public InteractionResult useOn(UseOnContext ctx) {
        Level level = ctx.getLevel();
        Player player = ctx.getPlayer();
        ItemStack stack = ctx.getItemInHand();
        if (player == null) {
            return InteractionResult.PASS;
        }
        if (level.isClientSide) {
            return InteractionResult.SUCCESS;
        }
        if (!(player instanceof ServerPlayer sp) || !(level instanceof ServerLevel sl)) {
            return InteractionResult.PASS;
        }
        if (stack.getDamageValue() >= stack.getMaxDamage()) {
            return InteractionResult.FAIL;
        }

        Block block = getStoredBlock(stack);
        if (block == null || block.defaultBlockState().isAir()) {
            List<Block> invBlocks = BuilderWandMaterialPicker.sortedPlaceableBlocks(sp.getInventory());
            if (!invBlocks.isEmpty()) {
                block = invBlocks.get(0);
                setStoredBlock(stack, block);
            }
        }
        if (block == null || block.defaultBlockState().isAir()) {
            sp.displayClientMessage(Component.translatable("message.bmcmod.builder_wand.no_block"), true);
            sp.playSound(SoundEvents.VILLAGER_NO, 0.4F, 1.2F);
            return InteractionResult.FAIL;
        }
        BlockState toPlace = block.defaultBlockState();
        WandModeSpec spec = tier.mode(getMode(stack));
        Vec3 look = sp.getLookAngle();
        int turn = getPlacementTurn(stack);
        List<BlockPos> positions = BuilderWandPlacement.computePositions(ctx.getClickedPos(), ctx.getClickedFace(), spec, look, turn);
        EquipmentSlot slot = ctx.getHand() == InteractionHand.MAIN_HAND ? EquipmentSlot.MAINHAND : EquipmentSlot.OFFHAND;
        int placed = BuilderWandPlacement.placeAll(sp, sl, positions, toPlace, stack, slot);
        if (placed == 0) {
            sp.displayClientMessage(Component.translatable("message.bmcmod.builder_wand.nothing_placed"), true);
            return InteractionResult.FAIL;
        }
        sp.playSound(SoundEvents.AMETHYST_BLOCK_CHIME, 0.25F, 1.1F + sp.getRandom().nextFloat() * 0.2F);
        return InteractionResult.SUCCESS;
    }

    @Override
    public void appendHoverText(ItemStack stack, TooltipContext context, List<Component> tooltip, TooltipFlag flag) {
        super.appendHoverText(stack, context, tooltip, flag);
        int idx = getMode(stack);
        WandModeSpec spec = tier.mode(idx);
        tooltip.add(Component.translatable("item.bmcmod.builder_wand.tier." + tier.id()).withStyle(ChatFormatting.GOLD));
        tooltip.add(
                Component.translatable("item.bmcmod.builder_wand.mode_progress", idx + 1, tier.modeCount())
                        .append(Component.literal(" "))
                        .append(modeLabelCompact(spec))
                        .withStyle(ChatFormatting.DARK_AQUA));
        Block b = getStoredBlock(stack);
        if (b != null) {
            tooltip.add(Component.translatable("item.bmcmod.builder_wand.block_line", b.getName()).withStyle(ChatFormatting.GRAY));
        } else {
            tooltip.add(Component.translatable("item.bmcmod.builder_wand.block_none").withStyle(ChatFormatting.DARK_RED));
        }
        if (!flag.hasShiftDown()) {
            tooltip.add(Component.translatable("item.bmcmod.staff.hold_shift").withStyle(ChatFormatting.DARK_GRAY));
            return;
        }
        tooltip.add(modeLabel(spec).withStyle(ChatFormatting.AQUA));
        tooltip.add(Component.translatable("item.bmcmod.builder_wand.hint_face").withStyle(ChatFormatting.GRAY));
        tooltip.add(Component.translatable("item.bmcmod.builder_wand.hint_look").withStyle(ChatFormatting.GRAY));
        tooltip.add(Component.translatable("item.bmcmod.builder_wand.hint_placement_turn").withStyle(ChatFormatting.GRAY));
        tooltip.add(Component.translatable("item.bmcmod.builder_wand.controls").withStyle(ChatFormatting.YELLOW));
        tooltip.add(Component.translatable("item.bmcmod.builder_wand.usage").withStyle(ChatFormatting.DARK_GRAY));
    }

    private static MutableComponent modeLabelCompact(WandModeSpec spec) {
        return switch (spec.kind()) {
            case LINE -> Component.translatable("item.bmcmod.builder_wand.mode.line.short", spec.a());
            case PLANE -> Component.translatable("item.bmcmod.builder_wand.mode.plane.short", spec.a(), spec.b());
            case HOLLOW_PLANE -> Component.translatable("item.bmcmod.builder_wand.mode.hollow_plane.short", spec.a(), spec.b());
            case CROSS -> Component.translatable("item.bmcmod.builder_wand.mode.cross.short", spec.a(), spec.b());
            case CORNER -> Component.translatable("item.bmcmod.builder_wand.mode.corner.short", spec.a(), spec.b());
            case DIAGONAL -> Component.translatable("item.bmcmod.builder_wand.mode.diagonal.short", spec.a());
            case WALL -> Component.translatable("item.bmcmod.builder_wand.mode.wall.short", spec.a(), spec.b());
            case HOLLOW_WALL -> Component.translatable("item.bmcmod.builder_wand.mode.hollow_wall.short", spec.a(), spec.b());
        };
    }

    private static MutableComponent modeLabel(WandModeSpec spec) {
        return switch (spec.kind()) {
            case LINE -> Component.translatable("item.bmcmod.builder_wand.mode.line", spec.a());
            case PLANE -> Component.translatable("item.bmcmod.builder_wand.mode.plane", spec.a(), spec.b());
            case HOLLOW_PLANE -> Component.translatable("item.bmcmod.builder_wand.mode.hollow_plane", spec.a(), spec.b());
            case CROSS -> Component.translatable("item.bmcmod.builder_wand.mode.cross", spec.a(), spec.b());
            case CORNER -> Component.translatable("item.bmcmod.builder_wand.mode.corner", spec.a(), spec.b());
            case DIAGONAL -> Component.translatable("item.bmcmod.builder_wand.mode.diagonal", spec.a());
            case WALL -> Component.translatable("item.bmcmod.builder_wand.mode.wall", spec.a(), spec.b());
            case HOLLOW_WALL -> Component.translatable("item.bmcmod.builder_wand.mode.hollow_wall", spec.a(), spec.b());
        };
    }
}
