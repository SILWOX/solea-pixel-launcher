package com.soleapixel.bmcmod.item;

import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResultHolder;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;

/**
 * Ouvre l’écran client {@link com.soleapixel.bmcmod.client.EnchantmentCodexScreen} (hook enregistré dans {@link com.soleapixel.bmcmod.BmcModClient}).
 */
public final class EnchantmentCodexItem extends Item {
    private static Runnable clientOpenAction = () -> {};

    public EnchantmentCodexItem(Properties properties) {
        super(properties);
    }

    /** Appelé au démarrage client uniquement. */
    public static void setClientOpenAction(Runnable action) {
        clientOpenAction = action;
    }

    @Override
    public InteractionResultHolder<ItemStack> use(Level level, Player player, InteractionHand hand) {
        ItemStack stack = player.getItemInHand(hand);
        if (level.isClientSide()) {
            clientOpenAction.run();
        }
        return InteractionResultHolder.success(stack);
    }

    public static Item.Properties defaultProperties() {
        return new Item.Properties().stacksTo(1);
    }
}
