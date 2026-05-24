package com.soleapixel.bmcmod.block.feeder;

import org.jetbrains.annotations.Nullable;

import net.minecraft.core.Direction;
import net.minecraft.world.item.ItemStack;
import net.neoforged.neoforge.capabilities.Capabilities;
import net.neoforged.neoforge.capabilities.RegisterCapabilitiesEvent;
import net.neoforged.neoforge.items.IItemHandler;

import com.soleapixel.bmcmod.registry.ModBlockEntityTypes;

/**
 * Hopper / automation : dessus et côtés = entrée, dessous = sortie uniquement.
 * L’orientation décorative ({@code HORIZONTAL_FACING}) ne s’applique pas aux tubes : un entonnoir au-dessus ou sur le côté doit toujours pouvoir remplir.
 */
public final class FeederCapabilities {
    private FeederCapabilities() {
    }

    public static void register(RegisterCapabilitiesEvent event) {
        event.registerBlockEntity(
                Capabilities.ItemHandler.BLOCK,
                ModBlockEntityTypes.FEEDER.get(),
                (be, side) -> {
                    if (!(be instanceof FeederBlockEntity feeder)) {
                        return NOOP;
                    }
                    return forSide(feeder, side);
                });
    }

    private static IItemHandler forSide(FeederBlockEntity feeder, @Nullable Direction side) {
        if (side == null) {
            // Insert + extract : pas d’aperçu du slot pour ne pas casser l’entrée en foin après vue « blé ».
            return new FeederItemHandler(feeder, true, true, false);
        }
        if (side == Direction.UP) {
            return new FeederItemHandler(feeder, true, false, false);
        }
        if (side == Direction.DOWN) {
            return new FeederItemHandler(feeder, false, true, true);
        }
        return new FeederItemHandler(feeder, true, false, false);
    }

    private static final IItemHandler NOOP = new IItemHandler() {
        @Override
        public int getSlots() {
            return 0;
        }

        @Override
        public ItemStack getStackInSlot(int slot) {
            return ItemStack.EMPTY;
        }

        @Override
        public ItemStack insertItem(int slot, ItemStack stack, boolean simulate) {
            return stack;
        }

        @Override
        public ItemStack extractItem(int slot, int amount, boolean simulate) {
            return ItemStack.EMPTY;
        }

        @Override
        public int getSlotLimit(int slot) {
            return 0;
        }

        @Override
        public boolean isItemValid(int slot, ItemStack stack) {
            return false;
        }
    };
}
