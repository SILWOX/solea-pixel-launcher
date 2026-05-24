package com.soleapixel.bmcmod.item;

import java.util.List;

import com.soleapixel.bmcmod.registry.ModItems;

import net.minecraft.ChatFormatting;
import net.minecraft.core.component.DataComponents;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.Mth;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResultHolder;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Rarity;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.item.UseAnim;
import net.minecraft.world.level.Level;
import net.minecraft.world.item.component.CustomData;
import net.minecraft.world.item.Item.TooltipContext;

/**
 * Fiole d'expérience scellée (même look que l'orbe d'exp en bouteille) : la boire donne toute l'XP stockée.
 */
public class SealedExperienceBottleItem extends Item {
    public static final String XP_KEY = "SealedXp";

    public SealedExperienceBottleItem(Properties properties) {
        super(properties.rarity(Rarity.RARE).stacksTo(16));
    }

    public static int getStoredXp(ItemStack stack) {
        return stack.getOrDefault(DataComponents.CUSTOM_DATA, CustomData.EMPTY).copyTag().getInt(XP_KEY);
    }

    public static void setStoredXp(ItemStack stack, int xp) {
        CompoundTag tag = stack.getOrDefault(DataComponents.CUSTOM_DATA, CustomData.EMPTY).copyTag();
        tag.putInt(XP_KEY, Mth.clamp(xp, 0, Integer.MAX_VALUE));
        stack.set(DataComponents.CUSTOM_DATA, CustomData.of(tag));
    }

    public static ItemStack newStackWithXp(int xp) {
        ItemStack s = new ItemStack(ModItems.SEALED_EXPERIENCE_BOTTLE.get());
        setStoredXp(s, xp);
        return s;
    }

    @Override
    public int getUseDuration(ItemStack stack, LivingEntity user) {
        return 32;
    }

    @Override
    public UseAnim getUseAnimation(ItemStack stack) {
        return UseAnim.DRINK;
    }

    @Override
    public ItemStack finishUsingItem(ItemStack stack, Level level, LivingEntity user) {
        if (user instanceof Player player) {
            int xp = getStoredXp(stack);
            if (xp > 0) {
                player.giveExperiencePoints(xp);
            }
            if (!level.isClientSide()) {
                level.playSound(
                        null,
                        player.getX(), player.getY(), player.getZ(),
                        SoundEvents.EXPERIENCE_ORB_PICKUP,
                        SoundSource.PLAYERS,
                        0.4F, 0.3F);
            }
            if (!player.getAbilities().instabuild) {
                stack.shrink(1);
                ItemStack glass = new ItemStack(net.minecraft.world.item.Items.GLASS_BOTTLE, 1);
                if (!player.getInventory().add(glass)) {
                    player.drop(glass, false);
                }
            }
        }
        return stack;
    }

    @Override
    public InteractionResultHolder<ItemStack> use(Level level, Player player, InteractionHand hand) {
        player.startUsingItem(hand);
        return InteractionResultHolder.sidedSuccess(player.getItemInHand(hand), level.isClientSide());
    }

    @Override
    public void appendHoverText(ItemStack stack, TooltipContext context, List<Component> tooltip, TooltipFlag flag) {
        int xp = getStoredXp(stack);
        tooltip.add(Component.translatable("item.bmcmod.sealed_experience_bottle.tip", xp)
                .withStyle(ChatFormatting.GREEN));
    }
}
