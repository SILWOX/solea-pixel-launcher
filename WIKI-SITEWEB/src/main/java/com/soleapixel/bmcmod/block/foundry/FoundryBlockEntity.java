package com.soleapixel.bmcmod.block.foundry;

import javax.annotation.Nullable;

import org.jetbrains.annotations.NotNull;

import com.soleapixel.bmcmod.menu.FoundryMenu;
import com.soleapixel.bmcmod.registry.ModBlockEntityTypes;
import com.soleapixel.bmcmod.registry.ModItems;
import com.soleapixel.bmcmod.registry.ModMenus;

import net.minecraft.core.BlockPos;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.world.MenuProvider;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.ContainerData;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.neoforged.neoforge.items.ItemStackHandler;

public class FoundryBlockEntity extends BlockEntity implements MenuProvider {
    public static final int SLOT_INPUT = 0;
    public static final int SLOT_FUEL = 1;
    public static final int SLOT_OUTPUT = 2;
    public static final int SLOT_COUNT = 3;

    private int litTime;
    private int litDuration;
    private int cookTime;
    private int cookTotalTime = FoundryMeltRules.defaultCookTime();

    private final ItemStackHandler items = new ItemStackHandler(SLOT_COUNT) {
        @Override
        protected void onContentsChanged(int slot) {
            setChanged();
        }

        @Override
        public boolean isItemValid(int slot, @NotNull ItemStack stack) {
            if (slot == SLOT_OUTPUT) {
                return false;
            }
            if (slot == SLOT_FUEL) {
                return isFuel(stack);
            }
            return true;
        }
    };

    public final ContainerData dataAccess = new ContainerData() {
        @Override
        public int get(int index) {
            return switch (index) {
                case 0 -> litTime;
                case 1 -> litDuration;
                case 2 -> cookTime;
                case 3 -> cookTotalTime;
                default -> 0;
            };
        }

        @Override
        public void set(int index, int value) {
            switch (index) {
                case 0 -> litTime = value;
                case 1 -> litDuration = value;
                case 2 -> cookTime = value;
                case 3 -> cookTotalTime = value;
                default -> {
                }
            }
        }

        @Override
        public int getCount() {
            return 4;
        }
    };

    public FoundryBlockEntity(BlockPos pos, BlockState state) {
        super(ModBlockEntityTypes.FOUNDRY.get(), pos, state);
    }

    public static void serverTick(net.minecraft.world.level.Level level, BlockPos pos, BlockState state, FoundryBlockEntity be) {
        boolean wasLit = be.isLit();
        if (be.litTime > 0) {
            be.litTime--;
        }

        ItemStack input = be.items.getStackInSlot(SLOT_INPUT);
        FoundryMeltRules.MeltResult result = FoundryMeltRules.meltResult(input);
        be.cookTotalTime = result.cookTime();
        boolean canSmelt = be.canOutput(result.output());
        if (!be.isLit() && canSmelt) {
            ItemStack fuel = be.items.getStackInSlot(SLOT_FUEL);
            int fuelValue = be.getFuelValue(fuel);
            if (fuelValue > 0) {
                be.litTime = fuelValue;
                be.litDuration = fuelValue;
                fuel.shrink(1);
                if (fuel.isEmpty()) {
                    be.items.setStackInSlot(SLOT_FUEL, ItemStack.EMPTY);
                }
                be.setChanged();
            }
        }

        if (be.isLit() && canSmelt) {
            be.cookTime++;
            if (be.cookTime >= be.cookTotalTime) {
                be.cookTime = 0;
                be.smeltOne(result.output());
            }
        } else {
            be.cookTime = 0;
        }

        if (wasLit != be.isLit()) {
            level.setBlock(pos, state.setValue(FoundryBlock.LIT, be.isLit()), 3);
        }
        if (wasLit != be.isLit() || be.cookTime > 0) {
            be.setChanged();
        }
    }

    private boolean canOutput(ItemStack out) {
        if (out.isEmpty()) {
            return false;
        }
        ItemStack current = this.items.getStackInSlot(SLOT_OUTPUT);
        if (current.isEmpty()) {
            return true;
        }
        if (!ItemStack.isSameItemSameComponents(current, out)) {
            return false;
        }
        return current.getCount() + out.getCount() <= current.getMaxStackSize();
    }

    private void smeltOne(ItemStack out) {
        if (out.isEmpty()) {
            return;
        }
        ItemStack input = this.items.getStackInSlot(SLOT_INPUT);
        if (input.isEmpty()) {
            return;
        }
        ItemStack current = this.items.getStackInSlot(SLOT_OUTPUT);
        if (current.isEmpty()) {
            this.items.setStackInSlot(SLOT_OUTPUT, out.copy());
        } else {
            current.grow(out.getCount());
            this.items.setStackInSlot(SLOT_OUTPUT, current);
        }
        input.shrink(1);
        if (input.isEmpty()) {
            this.items.setStackInSlot(SLOT_INPUT, ItemStack.EMPTY);
        }
        this.setChanged();
    }

    public ItemStackHandler getItems() {
        return this.items;
    }

    public boolean isLit() {
        return this.litTime > 0;
    }

    public int getLitTime() {
        return this.litTime;
    }

    public int getLitDuration() {
        return this.litDuration;
    }

    public int getCookTime() {
        return this.cookTime;
    }

    public int getCookTotalTime() {
        return this.cookTotalTime;
    }

    public static boolean isFuel(ItemStack stack) {
        return stack.is(ModItems.NEBRITH_SHARD.get())
                || stack.is(ModItems.TOPAZ_SHARD.get())
                || stack.is(ModItems.BERYL_SHARD.get())
                || stack.is(ModItems.OPAL_SHARD.get());
    }

    private int getFuelValue(ItemStack stack) {
        if (stack.isEmpty()) {
            return 0;
        }
        if (stack.is(ModItems.NEBRITH_SHARD.get())) {
            return 900;
        }
        if (stack.is(ModItems.TOPAZ_SHARD.get())) {
            return 820;
        }
        if (stack.is(ModItems.BERYL_SHARD.get())) {
            return 860;
        }
        if (stack.is(ModItems.OPAL_SHARD.get())) {
            return 780;
        }
        return 0;
    }

    @Override
    public Component getDisplayName() {
        return Component.translatable("container.bmcmod.foundry");
    }

    @Nullable
    @Override
    public AbstractContainerMenu createMenu(int id, Inventory inventory, Player player) {
        return new FoundryMenu(ModMenus.FOUNDRY.get(), id, inventory, this);
    }

    @Override
    protected void saveAdditional(CompoundTag tag, HolderLookup.Provider registries) {
        super.saveAdditional(tag, registries);
        tag.put("items", this.items.serializeNBT(registries));
        tag.putInt("litTime", this.litTime);
        tag.putInt("litDuration", this.litDuration);
        tag.putInt("cookTime", this.cookTime);
        tag.putInt("cookTotalTime", this.cookTotalTime);
    }

    @Override
    protected void loadAdditional(CompoundTag tag, HolderLookup.Provider registries) {
        super.loadAdditional(tag, registries);
        this.items.deserializeNBT(registries, tag.getCompound("items"));
        this.litTime = tag.getInt("litTime");
        this.litDuration = tag.getInt("litDuration");
        this.cookTime = tag.getInt("cookTime");
        int loadedCookTotal = tag.getInt("cookTotalTime");
        this.cookTotalTime = loadedCookTotal > 0 ? loadedCookTotal : FoundryMeltRules.defaultCookTime();
    }
}
