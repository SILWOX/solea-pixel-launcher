package com.soleapixel.bmcmod.item;

import java.util.List;

import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResultHolder;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Rarity;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.item.UseAnim;
import net.minecraft.world.level.Level;
import net.minecraft.world.item.Item.TooltipContext;

/**
 * Totem de retour : maintien clic droit (~1 s) puis téléportation au point de spawn (lit ou monde).
 * {@code maxDamage = 3} ⇒ trois utilisations.
 */
public final class BackTotemItem extends Item {

    public static final int USE_DURATION_TICKS = 24;

    public BackTotemItem(Properties properties) {
        super(properties);
    }

    @Override
    public InteractionResultHolder<ItemStack> use(Level level, Player player, InteractionHand hand) {
        player.startUsingItem(hand);
        return InteractionResultHolder.sidedSuccess(player.getItemInHand(hand), level.isClientSide());
    }

    @Override
    public int getUseDuration(ItemStack stack, LivingEntity entity) {
        return USE_DURATION_TICKS;
    }

    @Override
    public UseAnim getUseAnimation(ItemStack stack) {
        return UseAnim.BOW;
    }

    @Override
    public ItemStack finishUsingItem(ItemStack stack, Level level, LivingEntity entity) {
        if (entity instanceof ServerPlayer sp && !level.isClientSide()) {
            BackTotemTeleport.teleportToSpawn(sp);
            level.playSound(null, sp.getX(), sp.getY(), sp.getZ(), SoundEvents.CHORUS_FRUIT_TELEPORT, SoundSource.PLAYERS, 1.0F, 1.0F);
            if (level instanceof ServerLevel sl) {
                sl.sendParticles(ParticleTypes.PORTAL, sp.getX(), sp.getY() + 1.0, sp.getZ(), 32, 0.45, 0.5, 0.45, 0.08);
            }
            if (!sp.getAbilities().instabuild) {
                InteractionHand hand = sp.getUsedItemHand();
                stack.hurtAndBreak(1, sp, LivingEntity.getSlotForHand(hand));
            }
        }
        return stack;
    }

    @Override
    public boolean isBarVisible(ItemStack stack) {
        return true;
    }

    @Override
    public void appendHoverText(ItemStack stack, TooltipContext context, List<Component> tooltip, TooltipFlag flag) {
        super.appendHoverText(stack, context, tooltip, flag);
        tooltip.add(Component.translatable("item.bmcmod.back_totem.tip").withStyle(ChatFormatting.DARK_AQUA));
        int left = stack.getMaxDamage() - stack.getDamageValue();
        tooltip.add(Component.translatable("item.bmcmod.back_totem.charges", left, stack.getMaxDamage()).withStyle(ChatFormatting.GRAY));
    }
}
