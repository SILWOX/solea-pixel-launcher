package com.soleapixel.bmcmod.item;

import java.util.List;

import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.InteractionResultHolder;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.UseAnim;
import net.minecraft.world.level.Level;
import net.minecraft.core.component.DataComponents;
import net.minecraft.world.item.enchantment.ItemEnchantments;
import net.minecraft.world.item.Item.TooltipContext;
import net.minecraft.world.item.TooltipFlag;

import com.soleapixel.bmcmod.morph.MorphCrystalServer;
import com.soleapixel.bmcmod.morph.MorphCrystalSoul;
import com.soleapixel.bmcmod.morph.MorphPlayerState;
import com.soleapixel.bmcmod.rarity.BmcModRarity;

import net.minecraft.network.chat.Component;

public class MorphCrystalItem extends Item {
    public static final int CHARGE_TICKS = 5 * 20;
    /** Tolérance réseau / survie : un canal presque complet compte comme réussi. */
    private static final int CHANNEL_GRACE_TICKS = 12;

    public MorphCrystalItem(Properties properties) {
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
    public boolean isEnchantable(ItemStack stack) {
        return false;
    }

    @Override
    public void inventoryTick(ItemStack stack, Level level, net.minecraft.world.entity.Entity entity, int slotId, boolean isSelected) {
        if (!level.isClientSide() && !stack.getOrDefault(DataComponents.ENCHANTMENTS, ItemEnchantments.EMPTY).isEmpty()) {
            stack.set(DataComponents.ENCHANTMENTS, ItemEnchantments.EMPTY);
        }
    }

    @Override
    public InteractionResultHolder<ItemStack> use(Level level, Player player, InteractionHand hand) {
        ItemStack stack = player.getItemInHand(hand);
        if (level.isClientSide()) {
            player.startUsingItem(hand);
            return InteractionResultHolder.consume(stack);
        }
        if (!(player instanceof ServerPlayer sp)) {
            return InteractionResultHolder.pass(stack);
        }
        if (MorphCrystalServer.isMorphed(sp)) {
            player.startUsingItem(hand);
            return InteractionResultHolder.consume(stack);
        }
        // PASS (pas FAIL) : un FAIL en main principale empêche vanilla d’essayer la main secondaire (cristal off-hand).
        if (!MorphCrystalSoul.hasSoul(stack)) {
            return InteractionResultHolder.pass(stack);
        }
        player.startUsingItem(hand);
        return InteractionResultHolder.consume(stack);
    }

    /**
     * Clic sur une entité avec le cristal : même canal que {@link #use} quand morph ou âme présente (voir
     * {@link com.soleapixel.bmcmod.morph.CrystalEntityInteractEvents}).
     */
    public static InteractionResult tryBeginMorphChannelOnEntity(
            Player player, InteractionHand hand, ItemStack stack, Entity ignoredTarget) {
        if (!(stack.getItem() instanceof MorphCrystalItem)) {
            return InteractionResult.PASS;
        }
        if (!MorphPlayerState.isMorphed(player) && !MorphCrystalSoul.hasSoul(stack)) {
            return InteractionResult.PASS;
        }
        Level level = player.level();
        if (level.isClientSide()) {
            player.startUsingItem(hand);
            return InteractionResult.SUCCESS;
        }
        if (!(player instanceof ServerPlayer sp)) {
            return InteractionResult.PASS;
        }
        sp.startUsingItem(hand);
        return InteractionResult.SUCCESS;
    }

    @Override
    public int getUseDuration(ItemStack stack, LivingEntity entity) {
        return CHARGE_TICKS;
    }

    @Override
    public UseAnim getUseAnimation(ItemStack stack) {
        return UseAnim.BOW;
    }

    @Override
    public void onUseTick(Level level, LivingEntity entity, ItemStack stack, int remainingUseDuration) {
        if (level.isClientSide() || !(entity instanceof ServerPlayer sp)) {
            return;
        }
        double x = sp.getX();
        double y = sp.getY() + 0.2;
        double z = sp.getZ();
        if (!(level instanceof net.minecraft.server.level.ServerLevel sl)) {
            return;
        }
        if (MorphCrystalServer.isMorphed(sp)) {
            for (int i = 0; i < 6; i++) {
                double ox = (sl.random.nextDouble() - 0.5) * 1.2;
                double oy = sl.random.nextDouble() * 1.4;
                double oz = (sl.random.nextDouble() - 0.5) * 1.2;
                sl.sendParticles(ParticleTypes.REVERSE_PORTAL, x + ox, y + oy, z + oz, 1, 0, 0, 0, 0.06);
            }
            return;
        }
        if (!MorphCrystalSoul.hasSoul(stack)) {
            return;
        }
        for (int i = 0; i < 6; i++) {
            double ox = (sl.random.nextDouble() - 0.5) * 1.2;
            double oy = sl.random.nextDouble() * 1.4;
            double oz = (sl.random.nextDouble() - 0.5) * 1.2;
            sl.sendParticles(ParticleTypes.ENCHANT, x + ox, y + oy, z + oz, 1, 0, 0, 0, 0.02);
        }
    }

    @Override
    public void releaseUsing(ItemStack stack, Level level, LivingEntity livingEntity, int timeLeft) {
        if (level.isClientSide() || !(livingEntity instanceof ServerPlayer sp)) {
            return;
        }
        int used = getUseDuration(stack, livingEntity) - timeLeft;
        if (used + CHANNEL_GRACE_TICKS < CHARGE_TICKS) {
            return;
        }
        applyCompletedChannel(stack, level, sp);
    }

    @Override
    public ItemStack finishUsingItem(ItemStack stack, Level level, LivingEntity livingEntity) {
        if (!level.isClientSide() && livingEntity instanceof ServerPlayer sp) {
            applyCompletedChannel(stack, level, sp);
        }
        return stack;
    }

    private static void applyCompletedChannel(ItemStack stack, Level level, ServerPlayer sp) {
        InteractionHand hand = sp.getUsedItemHand();
        if (MorphCrystalServer.isMorphed(sp)) {
            MorphCrystalServer.tryCompleteDemorph(sp);
            level.playSound(null, sp.getX(), sp.getY(), sp.getZ(), SoundEvents.EVOKER_CAST_SPELL, SoundSource.PLAYERS, 0.55F, 0.75F);
        } else {
            MorphCrystalServer.tryCompleteMorph(sp, stack, hand);
            level.playSound(null, sp.getX(), sp.getY(), sp.getZ(), SoundEvents.EVOKER_CAST_SPELL, SoundSource.PLAYERS, 0.6F, 1.1F);
        }
    }

    @Override
    public void appendHoverText(ItemStack stack, TooltipContext context, List<Component> tooltipComponents, TooltipFlag tooltipFlag) {
        super.appendHoverText(stack, context, tooltipComponents, tooltipFlag);
        if (MorphCrystalSoul.hasSoul(stack)) {
            CompoundTag soul = MorphCrystalSoul.getSoul(stack);
            tooltipComponents.add(
                    Component.translatable("item.bmcmod.morph_crystal.soul_stored", MorphCrystalSoul.getStoredSoulDisplayName(soul))
                            .withStyle(net.minecraft.ChatFormatting.GREEN));
        }
        if (!tooltipFlag.hasShiftDown()) {
            return;
        }
        if (MorphCrystalSoul.hasSoul(stack)) {
            tooltipComponents.add(Component.translatable("item.bmcmod.morph_crystal.has_soul").withStyle(net.minecraft.ChatFormatting.GRAY));
        } else {
            tooltipComponents.add(Component.translatable("item.bmcmod.morph_crystal.empty").withStyle(net.minecraft.ChatFormatting.DARK_GRAY));
        }
        tooltipComponents.add(Component.translatable("item.bmcmod.morph_crystal.demorph_hint").withStyle(net.minecraft.ChatFormatting.DARK_AQUA));
    }
}
