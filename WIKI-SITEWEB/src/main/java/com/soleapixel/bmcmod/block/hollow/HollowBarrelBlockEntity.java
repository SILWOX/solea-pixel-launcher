package com.soleapixel.bmcmod.block.hollow;

import net.minecraft.core.BlockPos;
import net.minecraft.core.HolderLookup;
import net.minecraft.core.NonNullList;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.ContainerHelper;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.ChestMenu;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.ContainerOpenersCounter;
import net.minecraft.world.level.block.entity.RandomizableContainerBlockEntity;
import net.minecraft.world.level.block.state.BlockState;

import com.soleapixel.bmcmod.registry.ModBlockEntityTypes;

import org.jetbrains.annotations.NotNull;

/**
 * Comportement proche du {@link net.minecraft.world.level.block.entity.BarrelBlockEntity} : 27 cases,
 * menu 9×3 vanilla, son ouverture/fermeture, animation {@link HollowBarrelBlock#OPEN} via
 * {@link ContainerOpenersCounter}.
 */
public class HollowBarrelBlockEntity extends RandomizableContainerBlockEntity {
    public static final int CONTAINER_SIZE = 27;
    private NonNullList<ItemStack> items = NonNullList.withSize(CONTAINER_SIZE, ItemStack.EMPTY);
    private final ContainerOpenersCounter openersCounter = new ContainerOpenersCounter() {
        @Override
        protected void onOpen(@NotNull Level level, @NotNull BlockPos pos, @NotNull BlockState st) {
            playSound(level, pos, st, SoundEvents.BARREL_OPEN);
            level.setBlock(pos, st.setValue(HollowBarrelBlock.OPEN, true), 3);
        }

        @Override
        protected void onClose(@NotNull Level level, @NotNull BlockPos pos, @NotNull BlockState st) {
            playSound(level, pos, st, SoundEvents.BARREL_CLOSE);
            level.setBlock(pos, st.setValue(HollowBarrelBlock.OPEN, false), 3);
        }

        @Override
        protected void openerCountChanged(
                @NotNull Level level, @NotNull BlockPos pos, @NotNull BlockState st, int count, int open) {
            /* Pas de blockEvent ici : le client n’a pas le handler package-private du compteur vanilla.
             * {@link HollowBarrelBlockEntity#recheckOpen()} est appelé périodiquement par le ticker du bloc. */
        }

        @Override
        protected boolean isOwnContainer(@NotNull Player player) {
            if (player.containerMenu instanceof ChestMenu chestMenu) {
                return chestMenu.getContainer() == HollowBarrelBlockEntity.this;
            }
            return false;
        }
    };

    public HollowBarrelBlockEntity(BlockPos pos, BlockState state) {
        super(ModBlockEntityTypes.HOLLOW_BARREL.get(), pos, state);
    }

    @Override
    protected void saveAdditional(CompoundTag tag, HolderLookup.Provider registries) {
        super.saveAdditional(tag, registries);
        if (!this.trySaveLootTable(tag)) {
            ContainerHelper.saveAllItems(tag, this.items, registries);
        }
    }

    @Override
    protected void loadAdditional(CompoundTag tag, HolderLookup.Provider registries) {
        super.loadAdditional(tag, registries);
        this.items = NonNullList.withSize(CONTAINER_SIZE, ItemStack.EMPTY);
        if (!this.tryLoadLootTable(tag)) {
            ContainerHelper.loadAllItems(tag, this.items, registries);
        }
    }

    @Override
    public int getContainerSize() {
        return CONTAINER_SIZE;
    }

    @Override
    protected @NotNull NonNullList<ItemStack> getItems() {
        return this.items;
    }

    @Override
    protected void setItems(@NotNull NonNullList<ItemStack> list) {
        this.items = list;
    }

    @Override
    protected @NotNull Component getDefaultName() {
        return Component.translatable("container.bmcmod.hollow_barrel");
    }

    @Override
    protected @NotNull AbstractContainerMenu createMenu(int id, @NotNull Inventory inv) {
        return ChestMenu.threeRows(id, inv, this);
    }

    @Override
    public void startOpen(@NotNull Player player) {
        if (!this.remove && !player.isSpectator() && this.level != null) {
            this.openersCounter.incrementOpeners(player, this.level, this.getBlockPos(), this.getBlockState());
        }
    }

    @Override
    public void stopOpen(@NotNull Player player) {
        if (!this.remove && !player.isSpectator() && this.level != null) {
            this.openersCounter.decrementOpeners(player, this.level, this.getBlockPos(), this.getBlockState());
        }
    }

    public void recheckOpen() {
        if (!this.remove && this.level != null) {
            this.openersCounter.recheckOpeners(this.level, this.getBlockPos(), this.getBlockState());
        }
    }

    static void playSound(Level level, BlockPos pos, BlockState state, net.minecraft.sounds.SoundEvent sound) {
        double dx = pos.getX() + 0.5;
        double dy = pos.getY() + 0.5;
        double dz = pos.getZ() + 0.5;
        level.playSound(null, dx, dy, dz, sound, SoundSource.BLOCKS, 0.5F, level.random.nextFloat() * 0.1F + 0.9F);
    }

    /** Appelé périodiquement par le bloc (comme le tonneau vanilla après blockEvent) pour fermetures anormales. */
    public static void serverTick(net.minecraft.world.level.Level level, BlockPos pos, BlockState state, HollowBarrelBlockEntity be) {
        if (level.isClientSide()) {
            return;
        }
        if ((level.getGameTime() & 15) == 0) {
            be.recheckOpen();
        }
    }
}
